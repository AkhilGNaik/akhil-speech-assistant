package com.kannada.speechassistant.voiceassistant;

import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.kannada.speechassistant.BlindUserDashboardActivity;
import com.kannada.speechassistant.R;
import com.kannada.speechassistant.RoleManager;
import com.kannada.speechassistant.SessionManager;
import com.kannada.speechassistant.SpeechAssistantApplication;
import com.kannada.speechassistant.SplashActivity;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.StorageService;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dedicated persistent background Foreground Service for Hands-Free App Opening.
 *
 * Runs when the app is in the background, minimized, or closed so that blind users
 * (and any user needing accessibility) can open the app purely by voice:
 * "Hey Assistant, open speech assistant app" / "Hey Assistant, open app"
 * (or Kannada, Hindi, Malayalam equivalent commands).
 *
 * CRITICAL ISOLATION RULE:
 * When the app is not in the foreground, ONLY the "Open App" command is recognized.
 * Any other voice commands (caregiver call, messages, emergency, etc.)
 * are strictly ignored while the app is closed or in the background.
 */
public class BackgroundVoiceLaunchService extends Service {

    private static final String TAG = "VoiceLaunchService";
    private static final String CHANNEL_ID = "voice_launch_service_channel_v2";
    private static final String CHANNEL_TRIGGER_ID = "voice_launch_trigger_channel_v2";
    private static final int NOTIFICATION_ID = 40401;
    private static final int TRIGGER_NOTIFICATION_ID = 40402;

    public static final String ACTION_START = "com.kannada.speechassistant.ACTION_START_VOICE_LAUNCH";
    public static final String ACTION_STOP = "com.kannada.speechassistant.ACTION_STOP_VOICE_LAUNCH";
    public static final String ACTION_APP_FOREGROUNDED = "com.kannada.speechassistant.ACTION_APP_FOREGROUNDED";
    public static final String ACTION_APP_BACKGROUNDED = "com.kannada.speechassistant.ACTION_APP_BACKGROUNDED";

    // Vosk grammar including individual tokens and combined trigger phrases
    private static final String LAUNCHER_VOSK_GRAMMAR = "[\"assistant\", \"hey\", \"ok\", \"okay\", \"open\", \"speech\", \"app\", \"application\", \"launch\", \"start\", \"the\", \"my\", \"please\", \"hey assistant\", \"ok assistant\", \"open app\", \"open the app\", \"open my app\", \"open speech assistant\", \"open speech assistant app\", \"hey assistant open app\", \"hey assistant open speech assistant\", \"hey assistant open speech assistant app\", \"assistant open app\", \"assistant open speech assistant\", \"assistant open speech assistant app\", \"launch app\", \"launch speech assistant\", \"start app\", \"start speech assistant\", \"[unk]\"]";

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private static volatile boolean isAppInForeground = false;
    private static volatile BackgroundVoiceLaunchService instance = null;

    private SessionManager sessionManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Vosk Engine
    private Model voskModel = null;
    private Recognizer voskRecognizer = null;
    private final Object recognizerLock = new Object();
    private volatile boolean isModelReady = false;
    private volatile boolean isModelLoading = false;

    // Audio Capture State
    private AudioRecord audioRecord = null;
    private Thread audioThread = null;
    private final AtomicBoolean isListening = new AtomicBoolean(false);
    private final AtomicBoolean isTriggerProcessing = new AtomicBoolean(false);

    // TextToSpeech for confirmation announcement
    private TextToSpeech tts = null;
    private boolean isTtsReady = false;

    private final Runnable restartCaptureRunnable = this::startAudioCaptureIfEligible;

    public static boolean isAppInForeground() {
        return isAppInForeground;
    }

    public static void onAppForegrounded(@NonNull Context context) {
        isAppInForeground = true;
        Log.d(TAG, "onAppForegrounded: App entered foreground. Background listener yielding microphone.");
        if (instance != null) {
            instance.handleAppForegrounded();
        } else {
            sendActionIntent(context, ACTION_APP_FOREGROUNDED);
        }
    }

    public static void onAppBackgrounded(@NonNull Context context) {
        isAppInForeground = false;
        Log.d(TAG, "onAppBackgrounded: App entered background/closed. Background listener activating.");
        if (instance != null) {
            instance.handleAppBackgrounded();
        } else {
            sendActionIntent(context, ACTION_APP_BACKGROUNDED);
        }
    }

    private static void sendActionIntent(@NonNull Context context, @NonNull String action) {
        try {
            Intent intent = new Intent(context, BackgroundVoiceLaunchService.class);
            intent.setAction(action);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to send action intent " + action + ": " + e.getMessage());
        }
    }

    public static void stopService(@NonNull Context context) {
        Log.d(TAG, "stopService requested.");
        sendActionIntent(context, ACTION_STOP);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        sessionManager = new SessionManager(this);

        createNotificationChannels();
        initTts();
        initVoskModel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            startForegroundNotification();

            String action = (intent != null) ? intent.getAction() : null;
            Log.d(TAG, "onStartCommand action: " + action);

            if (ACTION_STOP.equals(action)) {
                stopAudioCapture();
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            } else if (ACTION_APP_FOREGROUNDED.equals(action)) {
                handleAppForegrounded();
            } else if (ACTION_APP_BACKGROUNDED.equals(action) || ACTION_START.equals(action) || action == null) {
                handleAppBackgrounded();
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error in onStartCommand: " + t.getMessage(), t);
        }

        return START_STICKY;
    }

    private void handleAppForegrounded() {
        isAppInForeground = true;
        mainHandler.removeCallbacks(restartCaptureRunnable);
        stopAudioCapture();
    }

    private void handleAppBackgrounded() {
        isAppInForeground = false;
        mainHandler.removeCallbacks(restartCaptureRunnable);
        // Delay 700ms to allow foreground activity to completely release hardware microphone
        mainHandler.postDelayed(restartCaptureRunnable, 700);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.i(TAG, "onTaskRemoved: Main application task swiped away. Keeping voice launch service alive.");
        isAppInForeground = false;

        try {
            // Schedule service restart through AlarmManager in case OS terminates process
            Intent restartServiceIntent = new Intent(getApplicationContext(), BackgroundVoiceLaunchService.class);
            restartServiceIntent.setAction(ACTION_START);
            PendingIntent restartPendingIntent = PendingIntent.getService(
                    getApplicationContext(),
                    991,
                    restartServiceIntent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                boolean canExact = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        canExact = alarmManager.canScheduleExactAlarms();
                    } catch (Throwable ignored) {}
                }

                try {
                    if (canExact) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 800, restartPendingIntent);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 800, restartPendingIntent);
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 800, restartPendingIntent);
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "Alarm schedule failed in onTaskRemoved: " + t.getMessage());
                    try {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, restartPendingIntent);
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Safe recovery in onTaskRemoved: " + t.getMessage());
        }

        // Restart capture loop for closed state
        mainHandler.postDelayed(restartCaptureRunnable, 1000);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        stopAudioCapture();
        mainHandler.removeCallbacks(restartCaptureRunnable);
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        executor.shutdown();
        Log.i(TAG, "BackgroundVoiceLaunchService destroyed.");
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                // Ongoing silent channel for foreground service
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Speech Assistant Voice Launch",
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Keeps speech assistant listening for voice launch commands when closed");
                channel.setShowBadge(false);
                channel.setSound(null, null);
                channel.enableVibration(false);
                nm.createNotificationChannel(channel);

                // High priority trigger channel for launching activity
                NotificationChannel triggerChannel = new NotificationChannel(
                        CHANNEL_TRIGGER_ID,
                        "Voice Launch Trigger Notification",
                        NotificationManager.IMPORTANCE_HIGH
                );
                triggerChannel.setDescription("Launches Speech Assistant when wake command is spoken");
                triggerChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                nm.createNotificationChannel(triggerChannel);
            }
        }
    }

    private void startForegroundNotification() {
        Intent openAppIntent = new Intent(this, SplashActivity.class);
        openAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle("Speech Assistant: Voice Launch Active")
                .setContentText("Say 'Hey Assistant, open app' to open")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(pi);

        boolean hasAudioPerm = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        Notification notification = builder.build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ (API 34)
            if (hasAudioPerm) {
                try {
                    startForeground(NOTIFICATION_ID, notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE | ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
                    return;
                } catch (Throwable t) {
                    Log.w(TAG, "Microphone FGS start restricted: " + t.getMessage() + ", falling back to specialUse");
                }
            }
            try {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } catch (Throwable t) {
                Log.e(TAG, "Failed to start FGS with specialUse: " + t.getMessage(), t);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 - 13
            try {
                if (hasAudioPerm) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
                } else {
                    startForeground(NOTIFICATION_ID, notification);
                }
            } catch (Throwable t) {
                Log.w(TAG, "startForeground error on Android Q..T: " + t.getMessage());
                try {
                    startForeground(NOTIFICATION_ID, notification);
                } catch (Throwable ignored) {}
            }
        } else {
            // Pre-Android 10
            try {
                startForeground(NOTIFICATION_ID, notification);
            } catch (Throwable t) {
                Log.e(TAG, "Failed startForeground: " + t.getMessage());
            }
        }
    }

    private void initTts() {
        tts = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true;
                String langCode = sessionManager.getLanguage();
                Locale targetLocale = VoiceLanguageConfig.getTtsLocale(langCode);
                try {
                    tts.setLanguage(targetLocale);
                } catch (Exception e) {
                    tts.setLanguage(Locale.US);
                }
            }
        });
    }

    private void initVoskModel() {
        if (isModelReady || isModelLoading) return;
        isModelLoading = true;

        executor.execute(() -> {
            try {
                // Check if model files already exist on disk
                File modelDir = new File(getFilesDir(), "model");
                if (modelDir.exists() && modelDir.isDirectory() && new File(modelDir, "am").exists()) {
                    synchronized (recognizerLock) {
                        voskModel = new Model(modelDir.getAbsolutePath());
                        isModelReady = true;
                        isModelLoading = false;
                        Log.i(TAG, "Vosk model loaded directly from disk in BackgroundVoiceLaunchService.");
                        if (!isAppInForeground) {
                            mainHandler.post(this::startAudioCaptureIfEligible);
                        }
                    }
                    return;
                }

                // Reuse shared model if WakeWordManager has loaded it
                WakeWordManager wwm = WakeWordManager.getInstance(getApplicationContext());
                Model existing = wwm.getVoskModel();
                if (existing != null) {
                    synchronized (recognizerLock) {
                        voskModel = existing;
                        isModelReady = true;
                        isModelLoading = false;
                        Log.i(TAG, "Vosk model successfully reused from WakeWordManager.");
                        if (!isAppInForeground) {
                            mainHandler.post(this::startAudioCaptureIfEligible);
                        }
                    }
                    return;
                }

                // Otherwise unpack model assets
                StorageService.unpack(getApplicationContext(), "model-en-us", "model",
                        model -> {
                            synchronized (recognizerLock) {
                                voskModel = model;
                                isModelReady = true;
                                isModelLoading = false;
                                Log.i(TAG, "Vosk model unpacked successfully in BackgroundVoiceLaunchService.");
                                if (!isAppInForeground) {
                                    mainHandler.post(this::startAudioCaptureIfEligible);
                                }
                            }
                        },
                        exception -> {
                            isModelLoading = false;
                            Log.e(TAG, "Failed to load Vosk model: " + exception.getMessage());
                            mainHandler.postDelayed(this::initVoskModel, 3000);
                        }
                );
            } catch (Exception e) {
                isModelLoading = false;
                Log.e(TAG, "Error initializing Vosk: " + e.getMessage(), e);
                mainHandler.postDelayed(this::initVoskModel, 3000);
            }
        });
    }

    private synchronized void startAudioCaptureIfEligible() {
        // If app is currently visible in foreground, in-app WakeWordManager takes mic precedence
        if (isAppInForeground) {
            Log.d(TAG, "App is currently in foreground. Background capture not needed.");
            return;
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECORD_AUDIO permission not granted. Cannot start background capture.");
            return;
        }

        if (!isModelReady || voskModel == null) {
            Log.d(TAG, "Model not ready yet. Scheduling retry...");
            if (!isModelLoading) {
                initVoskModel();
            }
            mainHandler.removeCallbacks(restartCaptureRunnable);
            mainHandler.postDelayed(restartCaptureRunnable, 1500);
            return;
        }

        if (isListening.get()) {
            return;
        }

        stopAudioCapture();

        try {
            synchronized (recognizerLock) {
                voskRecognizer = new Recognizer(voskModel, (float) SAMPLE_RATE, LAUNCHER_VOSK_GRAMMAR);
                voskRecognizer.setWords(false);
            }

            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            int bufferSize = Math.max(minBufferSize, 8192);

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord init failed in background service. Scheduling retry...");
                releaseAudioRecord();
                mainHandler.removeCallbacks(restartCaptureRunnable);
                mainHandler.postDelayed(restartCaptureRunnable, 1500);
                return;
            }

            audioRecord.startRecording();
            isListening.set(true);
            isTriggerProcessing.set(false);
            Log.i(TAG, "BackgroundVoiceLaunchService: STARTED listening for 'open app' in closed/background state.");

            audioThread = new Thread(this::audioLoop, "BgVoiceLaunchThread");
            audioThread.setPriority(Thread.MAX_PRIORITY);
            audioThread.start();

        } catch (Exception e) {
            Log.e(TAG, "Error starting background audio recording: " + e.getMessage(), e);
            releaseAudioRecord();
            mainHandler.removeCallbacks(restartCaptureRunnable);
            mainHandler.postDelayed(restartCaptureRunnable, 2000);
        }
    }

    private void audioLoop() {
        short[] buffer = new short[2048];
        Log.d(TAG, "Background voice capture loop active.");

        while (isListening.get()) {
            if (isAppInForeground) {
                Log.d(TAG, "App foregrounded, exiting background capture loop.");
                break;
            }
            if (audioRecord == null || audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                break;
            }

            int read = audioRecord.read(buffer, 0, buffer.length);
            if (read > 0) {
                try {
                    boolean accepted = false;
                    String json = null;

                    synchronized (recognizerLock) {
                        if (voskRecognizer == null || !isListening.get()) {
                            break;
                        }
                        accepted = voskRecognizer.acceptWaveForm(buffer, read);
                        if (accepted) {
                            json = voskRecognizer.getResult();
                        } else {
                            json = voskRecognizer.getPartialResult();
                        }
                    }

                    if (json != null && !json.isEmpty()) {
                        String candidate = extractCandidateText(json);
                        if (candidate != null && !candidate.isEmpty()) {
                            checkAndHandleAppLaunchCandidate(candidate);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Background recognizer error: " + e.getMessage());
                }
            } else if (read < 0) {
                Log.w(TAG, "AudioRecord read error in background: " + read);
                break;
            }
        }

        Log.d(TAG, "Background voice capture loop ended.");

        // Automatically schedule retry if the loop terminated unexpectedly while app is closed
        if (!isAppInForeground && isListening.get()) {
            stopAudioCapture();
            mainHandler.removeCallbacks(restartCaptureRunnable);
            mainHandler.postDelayed(restartCaptureRunnable, 1000);
        }
    }

    /**
     * Parses Vosk JSON result to extract candidate spoken text.
     */
    @Nullable
    private String extractCandidateText(@NonNull String jsonStr) {
        try {
            String text = "";
            java.util.regex.Matcher tm = java.util.regex.Pattern.compile("\"text\"\\s*:\\s*\"([^\"]*)\"").matcher(jsonStr);
            if (tm.find()) {
                text = tm.group(1);
            }
            if (text.isEmpty()) {
                java.util.regex.Matcher pm = java.util.regex.Pattern.compile("\"partial\"\\s*:\\s*\"([^\"]*)\"").matcher(jsonStr);
                if (pm.find()) {
                    text = pm.group(1);
                }
            }
            text = text.replace("[unk]", "").replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
            return text.isEmpty() ? null : text;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks if spoken text contains the app-opening command.
     * STRICT ENFORCEMENT: ONLY the open app command is handled!
     * All other commands are strictly ignored while the app is closed or in background.
     */
    private void checkAndHandleAppLaunchCandidate(@NonNull String rawCandidate) {
        if (isTriggerProcessing.get()) return;

        if (isAppOpenCandidate(rawCandidate)) {
            // Confirmed Open App Command!
            if (isTriggerProcessing.compareAndSet(false, true)) {
                Log.i(TAG, "VOICE_TRIGGER: Confirmed Open App command in closed/background state: '" + rawCandidate + "'");
                mainHandler.post(() -> executeOpenAppFlow(rawCandidate));
            }
        }
    }

    /**
     * Accurately determines if the spoken candidate represents an intent to open the app.
     */
    public static boolean isAppOpenCandidate(@NonNull String rawCandidate) {
        String cleaned = rawCandidate.replaceAll(VoiceIntentMatcher.PUNCTUATION_REGEX, " ")
                .replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        if (cleaned.isEmpty()) return false;

        // Disallow commands that belong to other sub-screens (caregiver, call, messages, etc.)
        if (cleaned.contains("home") || cleaned.contains("profile") || cleaned.contains("setting") ||
                cleaned.contains("message") || cleaned.contains("caregiver") || cleaned.contains("connection") ||
                cleaned.contains("emergency") || cleaned.contains("recorder") || cleaned.contains("chat") ||
                cleaned.contains("ಕಾಲ್") || cleaned.contains("ಕರೆ") || cleaned.contains("ಮನೆ") || cleaned.contains("ಸೆಟ್ಟಿಂಗ್") ||
                cleaned.contains("ಪ್ರೊಫೈಲ್") || cleaned.contains("ಸಂದೇಶ") || cleaned.contains("ಕೇರ್") || cleaned.contains("ತುರ್ತು")) {
            return false;
        }

        // Direct matching against phrase sets
        if (cleaned.contains("open speech assistant") || cleaned.contains("open app") ||
                cleaned.contains("open the app") || cleaned.contains("open my app") ||
                cleaned.contains("launch speech assistant") || cleaned.contains("launch app") ||
                cleaned.contains("start speech assistant") || cleaned.contains("start app") ||
                cleaned.contains("speech assistant app") || cleaned.contains("open application") ||
                cleaned.contains("ಸ್ಪೀಚ್ ಅಸಿಸ್ಟೆಂಟ್") || cleaned.contains("ಆ್ಯಪ್ ತೆರೆ") || cleaned.contains("ಆಪ್ ತೆರೆ") ||
                cleaned.contains("ಆ್ಯಪ್ ಓಪನ್") || cleaned.contains("ಆಪ್ ಓಪನ್") ||
                cleaned.contains("ऐप खोलो") || cleaned.contains("ऐप चालू") || cleaned.contains("स्पीच असिस्टेंट") ||
                cleaned.contains("ആപ്പ് തുറക്കുക") || cleaned.contains("ആപ്പ് ഓപ്പൺ") || cleaned.contains("സ്പീച്ച് അസിസ്റ്റന്റ്")) {
            return true;
        }

        Set<String> words = new HashSet<>(Arrays.asList(cleaned.split(" ")));
        String commandOnly = VoiceIntentMatcher.extractCommandText(cleaned);
        Set<String> cmdWords = new HashSet<>(Arrays.asList(commandOnly.split(" ")));

        return VoiceIntentMatcher.matchesOpenApp(cleaned, words) ||
                VoiceIntentMatcher.matchesOpenApp(commandOnly, cmdWords);
    }

    /**
     * Executes the sequence to launch the app into the foreground with voice confirmation.
     */
    private void executeOpenAppFlow(@NonNull String recognizedPhrase) {
        // 1. Stop background audio immediately to release microphone
        stopAudioCapture();

        // 2. Immediate Haptic Feedback so blind user feels the phone heard them
        triggerHapticVibration();

        // 3. Spoken Audio Feedback in user's selected language
        String langCode = sessionManager.getLanguage();
        String announcement = VoiceLanguageConfig.getOpenAppResponse(langCode);
        speakAnnouncement(announcement);

        // 4. Determine target activity
        Intent launchIntent;
        String role = sessionManager.getUserRole();
        if (sessionManager.isLoggedIn() && RoleManager.ROLE_BLIND_USER.equals(role)) {
            launchIntent = new Intent(this, BlindUserDashboardActivity.class);
        } else if (sessionManager.isLoggedIn() && role != null && RoleManager.getDashboardClassForRole(role) != null) {
            launchIntent = new Intent(this, RoleManager.getDashboardClassForRole(role));
        } else {
            launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (launchIntent == null) {
                launchIntent = new Intent(this, SplashActivity.class);
            }
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        // Android 14 (API 34) Background Activity Start Exemption
        Bundle optionsBundle = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                ActivityOptions opts = ActivityOptions.makeBasic();
                opts.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                optionsBundle = opts.toBundle();
            } catch (Throwable ignored) {}
        }

        // 5. Try Direct startActivity
        try {
            if (optionsBundle != null) {
                startActivity(launchIntent, optionsBundle);
            } else {
                startActivity(launchIntent);
            }
            Log.i(TAG, "Direct startActivity executed successfully.");
        } catch (Throwable e) {
            Log.w(TAG, "Direct startActivity restricted: " + e.getMessage());
        }

        // 6. Try Direct PendingIntent send
        try {
            PendingIntent directPi = PendingIntent.getActivity(
                    this,
                    NOTIFICATION_ID + 10,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE,
                    optionsBundle
            );
            directPi.send();
            Log.i(TAG, "Direct PendingIntent.send() executed.");
        } catch (Throwable t) {
            Log.w(TAG, "PendingIntent.send() error: " + t.getMessage());
        }

        // 7. High-Priority Heads-Up / Full-Screen Notification fallback (safely guarded)
        showLaunchFullScreenNotification(launchIntent, announcement, optionsBundle);

        // Reset trigger flag after 3 seconds
        mainHandler.postDelayed(() -> isTriggerProcessing.set(false), 3000);
    }

    private void showLaunchFullScreenNotification(@NonNull Intent launchIntent, @NonNull String announcement, @Nullable Bundle optionsBundle) {
        try {
            PendingIntent fullScreenPi = PendingIntent.getActivity(
                    this,
                    NOTIFICATION_ID + 11,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE,
                    optionsBundle
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_TRIGGER_ID)
                    .setSmallIcon(R.drawable.ic_logo)
                    .setContentTitle("Speech Assistant")
                    .setContentText(announcement)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setContentIntent(fullScreenPi)
                    .setFullScreenIntent(fullScreenPi, true)
                    .setAutoCancel(true);

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(TRIGGER_NOTIFICATION_ID, builder.build());
                // Clear trigger notification after 4 seconds
                mainHandler.postDelayed(() -> {
                    try {
                        nm.cancel(TRIGGER_NOTIFICATION_ID);
                    } catch (Throwable ignored) {}
                }, 4000);
            }
        } catch (Throwable t) {
            Log.w(TAG, "showLaunchFullScreenNotification error: " + t.getMessage());
        }
    }

    private void triggerHapticVibration() {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(400);
                }
            }
        } catch (Exception ignored) {}
    }

    private void speakAnnouncement(@NonNull String text) {
        if (tts != null && isTtsReady) {
            String langCode = sessionManager.getLanguage();
            Locale loc = VoiceLanguageConfig.getTtsLocale(langCode);
            try {
                tts.setLanguage(loc);
            } catch (Exception ignored) {}
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "VoiceLaunchAnnouncement");
        }
    }

    private synchronized void stopAudioCapture() {
        isListening.set(false);
        releaseAudioRecord();
        if (audioThread != null) {
            audioThread.interrupt();
            audioThread = null;
        }
        synchronized (recognizerLock) {
            if (voskRecognizer != null) {
                try {
                    voskRecognizer.close();
                } catch (Throwable ignored) {}
                voskRecognizer = null;
            }
        }
    }

    private synchronized void releaseAudioRecord() {
        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
            } catch (Exception e) {
                Log.w(TAG, "Error releasing AudioRecord: " + e.getMessage());
            } finally {
                audioRecord = null;
            }
        }
    }
}
