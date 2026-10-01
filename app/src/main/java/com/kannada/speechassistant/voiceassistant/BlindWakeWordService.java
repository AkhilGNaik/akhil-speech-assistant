package com.kannada.speechassistant.voiceassistant;

import android.Manifest;
import android.app.ActivityOptions;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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

import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.StorageService;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dedicated Android Foreground Service that runs in the background / when app is closed
 * exclusively for Blind Users (RoleManager.ROLE_BLIND_USER).
 *
 * STRICT REQUIREMENT & RESTRICTION:
 * ONLY listens for and executes exactly ONE command when the app is closed or backgrounded:
 * "Hey Assistant, open app" (and variations like "Assistant, open app", "open app", "launch app").
 *
 * When detected, automatically launches and brings up the Blind User Module home screen.
 *
 * All other voice assistant commands (CALL_CAREGIVER, SEND_MESSAGE, EMERGENCY_SOS,
 * RECORD_VOICE_MESSAGE, navigation, etc.) are strictly forbidden in the background
 * and are immediately ignored and discarded if heard.
 */
public class BlindWakeWordService extends Service {

    private static final String TAG = "BlindWakeWordService";

    public static final String ACTION_APP_FOREGROUNDED = "com.kannada.speechassistant.ACTION_APP_FOREGROUNDED";
    public static final String ACTION_APP_BACKGROUNDED = "com.kannada.speechassistant.ACTION_APP_BACKGROUNDED";
    public static final String ACTION_STOP_SERVICE = "com.kannada.speechassistant.ACTION_STOP_SERVICE";

    private static final String CHANNEL_ID = "blind_assistant_channel";
    private static final String CHANNEL_LAUNCH_ID = "blind_assistant_launch_channel";
    private static final int NOTIFICATION_ID = 7007;
    public static final int LAUNCH_NOTIFICATION_ID = 7008;

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private static volatile BlindWakeWordService activeInstance;

    private SessionManager sessionManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Model voskModel = null;
    private Recognizer voskRecognizer = null;
    private final Object recognizerLock = new Object();

    private AudioRecord audioRecord = null;
    private Thread audioThread = null;
    private final AtomicBoolean isRecordingRunning = new AtomicBoolean(false);
    private final AtomicBoolean isAppForeground = new AtomicBoolean(false);

    /**
     * Helper to start the service safely on all Android versions.
     */
    public static void startService(@NonNull Context context) {
        try {
            SessionManager sm = new SessionManager(context);
            if (!sm.isLoggedIn() || !RoleManager.ROLE_BLIND_USER.equals(sm.getUserRole())) {
                return;
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Intent intent = new Intent(context, BlindWakeWordService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start BlindWakeWordService: " + e.getMessage());
        }
    }

    /**
     * Notifies the service that an app activity has entered the foreground.
     * The background service yields the microphone immediately to the in-app WakeWordManager.
     */
    public static void notifyAppForegrounded() {
        BlindWakeWordService instance = activeInstance;
        if (instance != null) {
            instance.onAppForegroundChanged(true);
        }
    }

    /**
     * Notifies the service that the app has entered the background or minimized.
     * The background service resumes listening for "Hey Assistant, open app".
     */
    public static void notifyAppBackgrounded(@NonNull Context context) {
        startService(context);
        BlindWakeWordService instance = activeInstance;
        if (instance != null) {
            instance.onAppForegroundChanged(false);
        }
    }

    public static void stopService(@NonNull Context context) {
        try {
            Intent intent = new Intent(context, BlindWakeWordService.class);
            intent.setAction(ACTION_STOP_SERVICE);
            context.startService(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop BlindWakeWordService: " + e.getMessage());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        activeInstance = this;
        sessionManager = new SessionManager(this);

        createNotificationChannels();
        startServiceInForeground();
        initVoskModel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP_SERVICE.equals(action)) {
                stopSelf();
                return START_NOT_STICKY;
            } else if (ACTION_APP_FOREGROUNDED.equals(action)) {
                onAppForegroundChanged(true);
                return START_STICKY;
            } else if (ACTION_APP_BACKGROUNDED.equals(action)) {
                onAppForegroundChanged(false);
                return START_STICKY;
            }
        }

        // Verify eligibility: Blind user with microphone permission
        if (!isEligible()) {
            Log.d(TAG, "User not eligible for BlindWakeWordService. Stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Check foreground status
        boolean inForeground = SpeechAssistantApplication.getInstance() != null
                && SpeechAssistantApplication.getInstance().isAppInForeground();
        onAppForegroundChanged(inForeground);

        return START_STICKY;
    }

    private boolean isEligible() {
        if (sessionManager == null) return false;
        boolean loggedIn = sessionManager.isLoggedIn();
        String role = sessionManager.getUserRole();
        boolean isBlind = RoleManager.ROLE_BLIND_USER.equals(role);
        boolean hasMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        return loggedIn && isBlind && hasMic;
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                // Ongoing low-priority accessibility channel for background listening
                NotificationChannel chan = new NotificationChannel(
                        CHANNEL_ID,
                        "Blind Voice Assistant Service",
                        NotificationManager.IMPORTANCE_LOW
                );
                chan.setDescription("Listens for 'Hey Assistant, open app' for blind users");
                chan.setShowBadge(false);
                chan.setSound(null, null);
                nm.createNotificationChannel(chan);

                // High priority channel used for reliable full-screen app launch
                NotificationChannel launchChan = new NotificationChannel(
                        CHANNEL_LAUNCH_ID,
                        "App Launch Trigger",
                        NotificationManager.IMPORTANCE_HIGH
                );
                launchChan.setDescription("Triggers opening the Speech Assistant application");
                launchChan.enableVibration(true);
                launchChan.setVibrationPattern(new long[]{0, 250, 200, 250});
                launchChan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                launchChan.setBypassDnd(true);
                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                android.media.AudioAttributes audioAttr = new android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                launchChan.setSound(soundUri, audioAttr);
                nm.createNotificationChannel(launchChan);
            }
        }
    }

    private void startServiceInForeground() {
        Intent openIntent = new Intent(this, BlindUserDashboardActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openIntent.putExtra("EXTRA_TARGET_TAB", "home");

        PendingIntent pi = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle("Blind Voice Assistant")
                .setContentText("Say \"Hey Assistant, open app\" to launch")
                .setOngoing(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE);

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(NOTIFICATION_ID, builder.build());
        }
    }

    private synchronized void onAppForegroundChanged(boolean inForeground) {
        isAppForeground.set(inForeground);
        Log.d(TAG, "onAppForegroundChanged: inForeground = " + inForeground);

        if (inForeground) {
            // App is active in foreground: yield microphone to in-app WakeWordManager
            stopAudioRecordingInternal();
        } else {
            // App is in background or closed: start background listening
            if (isEligible()) {
                startBackgroundListening();
            }
        }
    }

    private synchronized void initVoskModel() {
        WakeWordManager wwm = WakeWordManager.getInstance(getApplicationContext());
        Model existingModel = wwm.getVoskModel();
        if (existingModel != null) {
            this.voskModel = existingModel;
            if (!isAppForeground.get()) {
                startBackgroundListening();
            }
            return;
        }

        // Unpack if not yet loaded
        executor.execute(() -> {
            try {
                StorageService.unpack(getApplicationContext(), "model-en-us", "model",
                        model -> {
                            synchronized (BlindWakeWordService.this) {
                                voskModel = model;
                                Log.i(TAG, "Vosk model loaded in BlindWakeWordService.");
                                if (!isAppForeground.get()) {
                                    mainHandler.post(BlindWakeWordService.this::startBackgroundListening);
                                }
                            }
                        },
                        exception -> Log.e(TAG, "Failed unpacking Vosk model in service: " + exception.getMessage()));
            } catch (Exception e) {
                Log.e(TAG, "Error initiating Vosk model in service: " + e.getMessage());
            }
        });
    }

    private synchronized void startBackgroundListening() {
        if (isRecordingRunning.get()) return;
        if (isAppForeground.get()) {
            Log.d(TAG, "Skipping background listening: app is currently in foreground.");
            return;
        }
        if (voskModel == null) {
            Log.d(TAG, "Waiting for Vosk model before starting background listening.");
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECORD_AUDIO permission missing for background listening.");
            return;
        }

        stopAudioRecordingInternal();

        try {
            synchronized (recognizerLock) {
                if (voskRecognizer == null) {
                    voskRecognizer = new Recognizer(voskModel, (float) SAMPLE_RATE, WakeWordManager.VOSK_GRAMMAR);
                    voskRecognizer.setWords(false);
                } else {
                    try {
                        voskRecognizer.reset();
                    } catch (Throwable ignored) {
                        try { voskRecognizer.close(); } catch (Throwable ignored2) {}
                        voskRecognizer = new Recognizer(voskModel, (float) SAMPLE_RATE, WakeWordManager.VOSK_GRAMMAR);
                        voskRecognizer.setWords(false);
                    }
                }
            }

            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            int bufferSize = Math.max(minBufferSize, 8192);

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed in BlindWakeWordService.");
                releaseAudioRecord();
                mainHandler.postDelayed(this::startBackgroundListening, 2000);
                return;
            }

            audioRecord.startRecording();
            isRecordingRunning.set(true);
            Log.i(TAG, "BlindWakeWordService: Background listening active. Listening for 'Hey Assistant, open app'...");

            audioThread = new Thread(this::backgroundAudioCaptureLoop, "BlindBackgroundAudioThread");
            audioThread.setPriority(Thread.MAX_PRIORITY);
            audioThread.start();

        } catch (Exception e) {
            Log.e(TAG, "Error starting background audio recording: " + e.getMessage(), e);
            releaseAudioRecord();
            mainHandler.postDelayed(this::startBackgroundListening, 3000);
        }
    }

    private void backgroundAudioCaptureLoop() {
        short[] audioBuffer = new short[2048];
        Log.d(TAG, "Background audio capture loop started.");

        while (isRecordingRunning.get()) {
            if (isAppForeground.get()) {
                Log.d(TAG, "App became foregrounded. Terminating background capture loop.");
                break;
            }

            if (audioRecord == null || audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                break;
            }

            int read = audioRecord.read(audioBuffer, 0, audioBuffer.length);
            if (read > 0) {
                try {
                    boolean accepted = false;
                    String partialJson = null;
                    String resultJson = null;

                    synchronized (recognizerLock) {
                        if (voskRecognizer == null || !isRecordingRunning.get()) {
                            break;
                        }
                        accepted = voskRecognizer.acceptWaveForm(audioBuffer, read);
                        if (accepted) {
                            resultJson = voskRecognizer.getResult();
                        } else {
                            partialJson = voskRecognizer.getPartialResult();
                        }
                    }

                    String jsonToParse = accepted ? resultJson : partialJson;
                    String candidate = extractCandidateText(jsonToParse);

                    if (candidate != null && !candidate.isEmpty()) {
                        // STRICT RESTRICTION: ONLY the command "Hey Assistant, open app" is recognized and executed.
                        // All other commands are ignored and discarded.
                        if (isOpenAppPhrase(candidate)) {
                            Log.i(TAG, "VOICE_COMMAND_RECOGNIZED: 'OPEN_APP' in background! Candidate: '" + candidate + "'");
                            isRecordingRunning.set(false);
                            mainHandler.post(this::onOpenAppCommandDetected);
                            break;
                        } else if (hasWakeWord(candidate)) {
                            // Wake word or other command heard, but not "open app"
                            Log.d(TAG, "Background command discarded (ONLY 'open app' is permitted in background): '" + candidate + "'");
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error in background recognizer loop: " + e.getMessage(), e);
                }
            } else if (read < 0) {
                Log.w(TAG, "AudioRecord read error: " + read);
                break;
            }
        }

        Log.d(TAG, "Background audio capture loop ended.");
    }

    /**
     * Checks if the detected spoken text matches the "open app" command.
     * Package-private static for direct unit testability.
     */
    public static boolean isOpenAppPhrase(@Nullable String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) return false;
        String s = rawText.toLowerCase(Locale.ROOT)
                .replaceAll(VoiceIntentMatcher.PUNCTUATION_REGEX, " ")
                .replaceAll("\\s+", " ").trim();

        // Explicit negative check: ensure it does not refer to other openable screens or other actions
        if (s.contains("profile") || s.contains("setting") || s.contains("caregiver")
                || s.contains("caretaker") || s.contains("message") || s.contains("call")
                || s.contains("emergency") || s.contains("sos") || s.contains("recorder")
                || s.contains("voice") || s.contains("notification") || s.contains("back")
                || s.contains("chat") || s.contains("communication")) {
            return false;
        }

        // Direct matching for "open app", "launch app", "open speech assistant", etc.
        if (s.contains("open app") ||
                s.contains("open speech assistant") ||
                s.contains("open the app") ||
                s.contains("open my app") ||
                s.contains("launch app") ||
                s.contains("start app") ||
                s.contains("open application") ||
                s.contains("launch speech assistant") ||
                s.contains("start speech assistant") ||
                s.contains("open kannada speech assistant") ||
                s.contains("ಆ್ಯಪ್ ತೆರೆ") ||
                s.contains("ಆ್ಯಪ್ ತೆರೆಯಿರಿ") ||
                s.contains("ಆ್ಯಪ್ ಓಪನ್ ಮಾಡು") ||
                s.contains("ಆ್ಯಪ್ ಓಪನ್ ಮಾಡಿ") ||
                s.contains("ಆ್ಯಪ್ ಓಪನ್") ||
                s.contains("ಸ್ಪೀಚ್ ಅಸಿಸ್ಟೆಂಟ್ ತೆರೆ") ||
                s.contains("ಅಪ್ಲಿಕೇಶನ್ ತೆರೆ") ||
                s.contains("ऐप खोलो") ||
                s.contains("ऐप ओपन करो") ||
                s.contains("ऐप ओपन") ||
                s.contains("स्पीच असिस्टेंट खोलो") ||
                s.contains("ആപ്പ് തുറക്കുക") ||
                s.contains("ആപ്പ് തുറക്കൂ") ||
                s.contains("ആപ്പ് ഓപ്പൺ") ||
                s.contains("സ്പീച്ച് അസിസ്റ്റന്റ് തുറക്കൂ")) {
            return true;
        }

        // Also check via VoiceIntentMatcher if wake-word prefix was present
        String cmd = VoiceIntentMatcher.extractCommandText(rawText);
        if (!cmd.isEmpty()) {
            VoiceIntent intent = VoiceIntentMatcher.matchIntent(cmd);
            return intent.getIntentType() == VoiceIntentType.OPEN_APP;
        }

        return false;
    }

    private static boolean hasWakeWord(@NonNull String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("assistant") || lower.contains("hey assistant")
                || VoiceIntentMatcher.hasWakeWord(lower);
    }

    @Nullable
    private static String extractCandidateText(@Nullable String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) return null;
        try {
            String text = "";
            String partial = "";
            try {
                JSONObject obj = new JSONObject(jsonStr);
                text = obj.optString("text", "");
                partial = obj.optString("partial", "");
            } catch (Throwable ignored) {
                text = extractJsonField(jsonStr, "text");
                partial = extractJsonField(jsonStr, "partial");
            }

            String candidate = !text.isEmpty() ? text : partial;
            if (candidate.isEmpty()) return null;

            candidate = candidate.replace("[unk]", "").replaceAll("\\s+", " ").trim();
            return candidate.isEmpty() ? null : candidate;
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractJsonField(@NonNull String json, @NonNull String field) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    /**
     * Executes when "Hey Assistant, open app" is detected in the background.
     * Opens the existing application and navigates directly to the Blind User home screen.
     */
    private synchronized void onOpenAppCommandDetected() {
        Log.i(TAG, "OPEN_APP triggered: Launching BlindUserDashboardActivity from background...");

        // 1. Release hardware microphone immediately so foreground activity can use it
        stopAudioRecordingInternal();

        // 2. Launch existing BlindUserDashboardActivity
        launchBlindUserDashboard(getApplicationContext());
    }

    /**
     * Reliable background launch mechanism for Android:
     * Combines direct startActivity with a high-priority Full-Screen PendingIntent notification.
     */
    public static void launchBlindUserDashboard(@NonNull Context context) {
        Intent intent = new Intent(context, BlindUserDashboardActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("EXTRA_TARGET_TAB", "home");
        intent.putExtra("EXTRA_FROM_VOICE_OPEN_APP", true);

        // Bundle with Background Activity Start privilege (Android 14+ / API 34)
        Bundle optionsBundle = null;
        if (Build.VERSION.SDK_INT >= 34) {
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
            options.setPendingIntentCreatorBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
            optionsBundle = options.toBundle();
        }

        PendingIntent pi = PendingIntent.getActivity(
                context,
                LAUNCH_NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0),
                optionsBundle
        );

        // 1. Direct startActivity invocation with background options
        try {
            if (optionsBundle != null) {
                context.startActivity(intent, optionsBundle);
            } else {
                context.startActivity(intent);
            }
            Log.i(TAG, "Direct startActivity executed successfully.");
        } catch (Exception e) {
            Log.w(TAG, "Direct startActivity error: " + e.getMessage());
        }

        // 2. PendingIntent send invocation
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                pi.send(context, 0, intent, null, null, null, optionsBundle);
            } else {
                pi.send();
            }
            Log.i(TAG, "pi.send executed successfully.");
        } catch (Exception e) {
            Log.w(TAG, "pi.send error: " + e.getMessage());
        }

        // 3. High-Priority Full-Screen Notification (incoming-call priority)
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_LAUNCH_ID)
                        .setSmallIcon(R.drawable.ic_logo)
                        .setContentTitle("Kannada Speech Assistant")
                        .setContentText("Opening Speech Assistant...")
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setFullScreenIntent(pi, true)
                        .setContentIntent(pi)
                        .setSound(soundUri)
                        .setVibrate(new long[]{0, 250, 200, 250})
                        .setAutoCancel(true);

                nm.notify(LAUNCH_NOTIFICATION_ID, builder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error posting launch notification: " + e.getMessage());
        }
    }

    private synchronized void stopAudioRecordingInternal() {
        isRecordingRunning.set(false);
        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
            } catch (Exception ignored) {}
            try {
                audioRecord.release();
            } catch (Exception ignored) {}
            audioRecord = null;
        }

        Thread threadToJoin = audioThread;
        audioThread = null;
        if (threadToJoin != null && threadToJoin != Thread.currentThread()) {
            try {
                threadToJoin.interrupt();
                threadToJoin.join(500);
            } catch (Exception ignored) {}
        }

        synchronized (recognizerLock) {
            if (voskRecognizer != null) {
                try {
                    voskRecognizer.reset();
                } catch (Throwable ignored) {}
            }
        }
    }

    private void releaseAudioRecord() {
        stopAudioRecordingInternal();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.i(TAG, "onTaskRemoved: App task removed from recents.");
        // Keep listening in background if user is logged in as Blind User
        if (isEligible()) {
            mainHandler.postDelayed(this::startBackgroundListening, 1000);
        } else {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "BlindWakeWordService destroyed.");
        stopAudioRecordingInternal();
        synchronized (recognizerLock) {
            if (voskRecognizer != null) {
                try { voskRecognizer.close(); } catch (Throwable ignored) {}
                voskRecognizer = null;
            }
        }
        activeInstance = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
