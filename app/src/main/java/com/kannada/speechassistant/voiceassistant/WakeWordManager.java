package com.kannada.speechassistant.voiceassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.kannada.speechassistant.BlindUserDashboardActivity;
import com.kannada.speechassistant.SpeechImpairedDashboardActivity;
import com.kannada.speechassistant.R;
import com.kannada.speechassistant.RoleManager;
import com.kannada.speechassistant.SessionManager;

import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dedicated Wake-Word Manager for hands-free Voice Assistant activation.
 *
 * Exclusively active for Blind Users (RoleManager.ROLE_BLIND_USER).
 * Listens for the wake word: "Assistant" (and variations like "hey assistant").
 *
 * Responsibilities:
 * 1. Offline, on-device keyword spotting using Vosk with grammar mode.
 * 2. Zero network communication; no audio streaming to cloud.
 * 3. Strict microphone conflict protection (yields to WebRTC calls, voice recording, manual STT).
 * 4. Zero duplicate trigger: immediately pauses upon detection and yields mic to existing SpeechRecognizer.
 * 5. Lifecycle bound: active only when BlindUserDashboardActivity is in the foreground.
 * 6. Forwards detected wake word to existing AppVoiceAssistant; never executes commands directly.
 */
public class WakeWordManager {

    private static final String TAG = "WakeWordManager";

    // Centralized Wake Word and In-App Voice Command Configuration for Vosk
    public static final String WAKE_WORD_PRIMARY = "assistant";
    public static final String WAKE_WORD_HEY = "hey assistant";
    static final String VOSK_GRAMMAR = "[\"assistant\", \"hey\", \"ok\", \"okay\", \"open\", \"app\", \"speech\", \"application\", \"profile\", \"caregiver\", \"caretaker\", \"call\", \"emergency\", \"sos\", \"settings\", \"home\", \"message\", \"messages\", \"voice\", \"send\", \"read\", \"stop\", \"back\", \"help\", \"cancel\", \"hey assistant\", \"ok assistant\", \"okay assistant\", \"open app\", \"hey assistant open app\", \"assistant open app\", \"open speech assistant\", \"hey assistant open speech assistant\", \"assistant open speech assistant\", \"open speech assistant app\", \"hey assistant open speech assistant app\", \"assistant open speech assistant app\", \"launch app\", \"hey assistant launch app\", \"start app\", \"hey assistant start app\", \"open the app\", \"open my app\", \"open profile\", \"assistant open profile\", \"hey assistant open profile\", \"show profile\", \"go to profile\", \"profile\", \"profile page\", \"my profile page\", \"open profile page\", \"assistant open profile page\", \"hey assistant open profile page\", \"open my profile page\", \"call caregiver\", \"assistant call caregiver\", \"hey assistant call caregiver\", \"call my caregiver\", \"assistant call my caregiver\", \"hey assistant call my caregiver\", \"call caretaker\", \"assistant call caretaker\", \"call my caretaker\", \"caregiver page\", \"open caregiver page\", \"assistant open caregiver page\", \"hey assistant caregiver page\", \"open caregiver\", \"assistant open caregiver\", \"hey assistant open caregiver\", \"caregiver connection\", \"emergency\", \"assistant emergency\", \"hey assistant emergency\", \"emergency help\", \"assistant emergency help\", \"hey assistant emergency help\", \"send emergency\", \"assistant send emergency\", \"sos\", \"assistant sos\", \"hey assistant sos\", \"send sos\", \"assistant send sos\", \"open settings\", \"assistant open settings\", \"hey assistant open settings\", \"show settings\", \"go to settings\", \"settings\", \"open home\", \"assistant open home\", \"hey assistant open home\", \"go home\", \"home\", \"home page\", \"open home page\", \"send message\", \"assistant send message\", \"hey assistant send message\", \"send a message\", \"assistant send a message\", \"message caregiver\", \"send voice message\", \"assistant send voice message\", \"read messages\", \"assistant read messages\", \"hey assistant read messages\", \"read my messages\", \"assistant read my messages\", \"read caregiver message\", \"read caregiver messages\", \"check messages\", \"count messages\", \"how many messages\", \"repeat message\", \"repeat the message\", \"say that again\", \"go back\", \"assistant go back\", \"hey assistant go back\", \"back\", \"assistant back\", \"return\", \"previous page\", \"stop\", \"assistant stop\", \"hey assistant stop\", \"stop listening\", \"accept call\", \"assistant accept call\", \"receive call\", \"end call\", \"assistant end call\", \"hang up\", \"disconnect call\", \"[unk]\"]";

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public enum State {
        IDLE,
        INITIALIZING,
        READY,
        LISTENING,
        PAUSED,
        TRIGGERED,
        DISABLED,
        ERROR
    }

    public interface WakeWordListener {
        void onWakeWordDetected(@NonNull String keyword);
        void onStateChanged(@NonNull State newState);
        void onError(@NonNull String errorMessage);
    }

    private static volatile WakeWordManager instance;

    private final Context appContext;
    private final SessionManager sessionManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    // Vosk Engine Components
    private Model voskModel = null;
    private Recognizer voskRecognizer = null;
    private final Object recognizerLock = new Object();
    private boolean isModelLoaded = false;
    private boolean isModelLoading = false;

    // Audio Capture Thread State
    private AudioRecord audioRecord = null;
    private Thread audioThread = null;
    private final AtomicBoolean isRecordingRunning = new AtomicBoolean(false);
    private final AtomicBoolean isTriggered = new AtomicBoolean(false);
    private State currentState = State.IDLE;

    private WeakReference<Activity> currentActivityRef;
    private WakeWordListener listener;

    private final Runnable retryRunnable = new Runnable() {
        @Override
        public void run() {
            Activity act = (currentActivityRef != null) ? currentActivityRef.get() : null;
            if (act != null && !act.isFinishing() && !act.isDestroyed()
                    && (act instanceof BlindUserDashboardActivity
                    || act instanceof SpeechImpairedDashboardActivity
                    || act instanceof com.kannada.speechassistant.ChatActivity
                    || act instanceof com.kannada.speechassistant.IncomingCallActivity)) {
                Log.d(TAG, "WakeWordManager: Executing scheduled retry for startListening...");
                startListening(act);
            }
        }
    };

    @Nullable
    public Activity getCurrentActivity() {
        return (currentActivityRef != null) ? currentActivityRef.get() : null;
    }

    private void scheduleRetry(@NonNull Activity activity, long delayMs) {
        mainHandler.removeCallbacks(retryRunnable);
        mainHandler.postDelayed(retryRunnable, delayMs);
    }

    private WakeWordManager(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = new SessionManager(appContext);
        if (appContext instanceof android.app.Application) {
            ((android.app.Application) appContext).registerActivityLifecycleCallbacks(new android.app.Application.ActivityLifecycleCallbacks() {
                @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
                @Override public void onActivityStarted(@NonNull Activity activity) {}
                @Override
                public void onActivityResumed(@NonNull Activity activity) {
                    currentActivityRef = new WeakReference<>(activity);
                    Log.d(TAG, "WakeWordManager: Activity resumed: " + activity.getClass().getSimpleName());
                }
                @Override public void onActivityPaused(@NonNull Activity activity) {}
                @Override public void onActivityStopped(@NonNull Activity activity) {}
                @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
                @Override
                public void onActivityDestroyed(@NonNull Activity activity) {
                    if (currentActivityRef != null && currentActivityRef.get() == activity) {
                        currentActivityRef.clear();
                    }
                }
            });
        }
        initVoskModelAsync();
    }

    public synchronized void setForegroundActivity(@NonNull Activity activity) {
        if (!activity.isFinishing() && !activity.isDestroyed()) {
            this.currentActivityRef = new WeakReference<>(activity);
            Log.d(TAG, "WakeWordManager: setForegroundActivity -> " + activity.getClass().getSimpleName());
        }
    }

    public synchronized void pauseListeningForActivity(@NonNull Activity activity) {
        Activity current = (currentActivityRef != null) ? currentActivityRef.get() : null;
        if (current == activity) {
            pauseListening();
        }
    }

    @NonNull
    public static WakeWordManager getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (WakeWordManager.class) {
                if (instance == null) {
                    instance = new WakeWordManager(context);
                }
            }
        }
        return instance;
    }

    public synchronized void setWakeWordListener(@Nullable WakeWordListener listener) {
        this.listener = listener;
    }

    private void setState(@NonNull State newState) {
        if (this.currentState != newState) {
            this.currentState = newState;
            Log.d(TAG, "WakeWordManager state changed to: " + newState);
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onStateChanged(newState);
                }
            });
        }
    }

    @NonNull
    public State getCurrentState() {
        return currentState;
    }

    public boolean isListening() {
        return currentState == State.LISTENING && isRecordingRunning.get();
    }

    /**
     * Initializes the Vosk Kaldi speech model asynchronously from assets.
     */
    private synchronized void initVoskModelAsync() {
        if (isModelLoaded || isModelLoading) return;
        isModelLoading = true;
        setState(State.INITIALIZING);

        Log.d(TAG, "WakeWordManager: Starting offline model unpacking/initialization...");

        backgroundExecutor.execute(() -> {
            try {
                StorageService.unpack(appContext, "model-en-us", "model",
                        model -> {
                            synchronized (WakeWordManager.this) {
                                voskModel = model;
                                isModelLoaded = true;
                                isModelLoading = false;
                                Log.i(TAG, "WakeWordManager: Vosk model initialized successfully.");
                                setState(State.READY);

                                // If an activity was waiting to start, resume now
                                Activity currentAct = (currentActivityRef != null) ? currentActivityRef.get() : null;
                                if (currentAct != null && !currentAct.isFinishing() && !currentAct.isDestroyed()) {
                                    mainHandler.post(() -> startListening(currentAct));
                                }
                            }
                        },
                        exception -> {
                            synchronized (WakeWordManager.this) {
                                isModelLoading = false;
                                isModelLoaded = false;
                                Log.e(TAG, "WakeWordManager: Failed to unpack Vosk model: " + exception.getMessage(), exception);
                                setState(State.ERROR);
                                mainHandler.post(() -> {
                                    if (listener != null) {
                                        listener.onError("Failed to load wake word model: " + exception.getMessage());
                                    }
                                });
                            }
                        });
            } catch (Exception e) {
                synchronized (WakeWordManager.this) {
                    isModelLoading = false;
                    isModelLoaded = false;
                    Log.e(TAG, "WakeWordManager: Initialization error: " + e.getMessage(), e);
                    setState(State.ERROR);
                }
            }
        });
    }

    /**
     * Exposes the initialized Vosk Model so background launcher service can share it.
     */
    @Nullable
    public synchronized Model getVoskModel() {
        return voskModel;
    }

    /**
     * Checks whether the current user is eligible for wake-word detection (Blind User only).
     */
    public boolean isBlindUser() {
        String role = sessionManager.getUserRole();
        return RoleManager.ROLE_BLIND_USER.equals(role);
    }

    public boolean isDeafUser() {
        String role = sessionManager.getUserRole();
        if (RoleManager.ROLE_DEAF_USER.equals(role) || RoleManager.ROLE_SPEECH_IMPAIRED.equals(role)) {
            return true;
        }
        Activity act = (currentActivityRef != null) ? currentActivityRef.get() : null;
        return act instanceof SpeechImpairedDashboardActivity;
    }

    public boolean isEligibleForWakeWord() {
        return isBlindUser() || isDeafUser();
    }

    /**
     * Starts listening for the wake word "Assistant".
     * Invoked from BlindUserDashboardActivity or SpeechImpairedDashboardActivity when resumed and permission granted.
     */
    public synchronized void startListening(@NonNull Activity activity) {
        this.currentActivityRef = new WeakReference<>(activity);

        // 1. Role Enforcement: Blind and Deaf Users
        if (!isEligibleForWakeWord()) {
            Log.d(TAG, "WakeWordManager: Skipped start - User is neither Blind nor Deaf User.");
            setState(State.DISABLED);
            return;
        }

        // 2. Permission Check
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "WakeWordManager: permission denied for RECORD_AUDIO.");
            setState(State.DISABLED);
            return;
        }

        // 3. Microphone Conflict Protection: Do not start if mic in use
        if (isMicrophoneConflicted(activity)) {
            Log.w(TAG, "WakeWordManager: microphone conflict detected at start. Scheduling retry...");
            setState(State.PAUSED);
            scheduleRetry(activity, 600);
            return;
        }

        // 4. Check if appropriate Assistant is currently triggered/active
        if (activity instanceof SpeechImpairedDashboardActivity) {
            boolean active = activity.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .getBoolean("pref_deaf_voice_assistant_enabled", true);
            if (!active) {
                Log.d(TAG, "WakeWordManager: Deaf Assistant is disabled by user. Holding wake word.");
                setState(State.PAUSED);
                return;
            }
        }

        AppVoiceAssistant va = AppVoiceAssistant.getInstance(appContext);
        if (isTriggered.get() || va.isListening() || va.getAssistantState() != AppVoiceAssistant.AssistantState.IDLE) {
            Log.d(TAG, "WakeWordManager: Assistant session already in progress (" + va.getAssistantState() + "). Holding wake word.");
            setState(State.PAUSED);
            mainHandler.removeCallbacks(retryRunnable);
            mainHandler.postDelayed(retryRunnable, 1000);
            return;
        }

        // 5. Check if model is ready
        if (!isModelLoaded || voskModel == null) {
            Log.d(TAG, "WakeWordManager: Model not ready yet. Will start once loaded.");
            if (!isModelLoading) {
                initVoskModelAsync();
            }
            return;
        }

        // 6. Stop any previous audio thread before starting new one
        stopAudioRecordingInternal();

        try {
            // Instantiate recognizer with restricted grammar for low CPU & high precision
            synchronized (recognizerLock) {
                if (voskRecognizer == null) {
                    voskRecognizer = new Recognizer(voskModel, (float) SAMPLE_RATE, VOSK_GRAMMAR);
                    voskRecognizer.setWords(false);
                } else {
                    try {
                        voskRecognizer.reset();
                    } catch (Throwable ignored) {
                        try { voskRecognizer.close(); } catch (Throwable ignored2) {}
                        voskRecognizer = new Recognizer(voskModel, (float) SAMPLE_RATE, VOSK_GRAMMAR);
                        voskRecognizer.setWords(false);
                    }
                }
            }

            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            int bufferSize = Math.max(minBufferSize, 8192);

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "WakeWordManager: AudioRecord initialization failed. Scheduling retry...");
                releaseAudioRecord();
                setState(State.PAUSED);
                scheduleRetry(activity, 500);
                return;
            }

            audioRecord.startRecording();
            mainHandler.removeCallbacks(retryRunnable);
            isRecordingRunning.set(true);
            isTriggered.set(false);
            setState(State.LISTENING);
            Log.i(TAG, "VOICE_MIC_OWNER = WAKE_WORD");
            Log.i(TAG, "WakeWordManager: started listening for wake word 'Assistant'.");

            audioThread = new Thread(this::audioCaptureLoop, "WakeWordAudioThread");
            audioThread.setPriority(Thread.MAX_PRIORITY);
            audioThread.start();

        } catch (Exception e) {
            Log.e(TAG, "WakeWordManager: Error starting audio recording: " + e.getMessage(), e);
            releaseAudioRecord();
            setState(State.PAUSED);
            scheduleRetry(activity, 600);
        }
    }

    /**
     * Dedicated background audio capture loop for wake-word detection.
     * Supports both single-utterance commands ("Assistant, open profile")
     * and two-step interaction ("Assistant" -> listening -> command).
     */
    private void audioCaptureLoop() {
        short[] audioBuffer = new short[2048];
        Log.d(TAG, "WakeWordManager: Audio capture loop active.");
        boolean wakeWordSpotted = false;
        long wakeWordSpottedTime = 0;
        String latestPhrase = null;

        while (isRecordingRunning.get()) {
            Activity act = (currentActivityRef != null) ? currentActivityRef.get() : null;

            // Safe Conflict Check on each cycle: If another component requires the mic, yield immediately
            if (act != null && isMicrophoneConflicted(act)) {
                Log.w(TAG, "WakeWordManager: Conflict detected during capture loop. Yielding microphone immediately.");
                mainHandler.post(this::pauseListening);
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
                    String candidate = parseAndExtractCandidate(jsonToParse);
                    if (candidate != null) {
                        latestPhrase = candidate;
                        if (!wakeWordSpotted) {
                            wakeWordSpotted = true;
                            wakeWordSpottedTime = System.currentTimeMillis();
                            Log.i(TAG, "VOICE_ASSISTANT: Wake word detected");
                            Log.i(TAG, "WAKE_WORD: Wake word detected = " + candidate);
                            Log.i(TAG, "WakeWordManager: Wake word spotted in audio: '" + candidate + "'");
                        }
                    }

                    if (wakeWordSpotted) {
                        if (accepted) {
                            // Full utterance endpoint reached by Vosk
                            Log.i(TAG, "WakeWordManager: Utterance completed with phrase: '" + latestPhrase + "'");
                            break;
                        } else {
                            String cmd = VoiceIntentMatcher.extractCommandText(latestPhrase != null ? latestPhrase : "");
                            // If a command is recognized along with the wake word, wait slightly for any final words or endpoint
                            if (!cmd.isEmpty() && (System.currentTimeMillis() - wakeWordSpottedTime > 500)) {
                                Log.i(TAG, "WakeWordManager: Single utterance command captured: '" + latestPhrase + "'");
                                break;
                            }

                            // If user said only "Assistant" and remained silent for 450ms, finish to enter two-step mode
                            if (cmd.isEmpty() && (System.currentTimeMillis() - wakeWordSpottedTime > 450)) {
                                Log.i(TAG, "WakeWordManager: Two-step silence threshold reached. Phrase: '" + latestPhrase + "'");
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "WakeWordManager: Recognizer error: " + e.getMessage(), e);
                }
            } else if (read < 0) {
                Log.w(TAG, "WakeWordManager: AudioRecord.read error: " + read);
                break;
            }
        }

        Log.d(TAG, "WakeWordManager: Audio capture loop terminated.");

        if (wakeWordSpotted && latestPhrase != null && isRecordingRunning.get()) {
            isTriggered.set(true);
            final String phraseToDispatch = latestPhrase;
            Log.i(TAG, "WAKE_WORD: Wake callback triggered for phrase: '" + phraseToDispatch + "'");
            mainHandler.post(() -> onWakeWordTriggered(phraseToDispatch));
        } else if (isRecordingRunning.get()) {
            Log.w(TAG, "WakeWordManager: Audio capture loop exited unexpectedly without wake word. Scheduling restart...");
            stopAudioRecordingInternal();
            mainHandler.post(() -> {
                Activity act = (currentActivityRef != null) ? currentActivityRef.get() : null;
                if (act != null && !act.isFinishing() && !act.isDestroyed()
                        && (act instanceof BlindUserDashboardActivity || act instanceof SpeechImpairedDashboardActivity || act instanceof com.kannada.speechassistant.ChatActivity)) {
                    scheduleRetry(act, 1000);
                }
            });
        }
    }

    /**
     * Parses JSON output from Vosk recognizer to detect wake word and any accompanying command.
     * Package-private static for direct unit testability.
     */
    @Nullable
    public static String parseAndExtractCandidate(@Nullable String jsonStr) {
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

            // Strip Vosk out-of-grammar token [unk]
            candidate = candidate.replace("[unk]", "").replaceAll("\\s+", " ").trim();
            if (candidate.isEmpty()) return null;

            String normalized = candidate.toLowerCase(java.util.Locale.ROOT);
            if (normalized.contains(WAKE_WORD_PRIMARY) || normalized.contains(WAKE_WORD_HEY)
                    || VoiceIntentMatcher.hasWakeWord(normalized)) {
                return normalized;
            }
            return null;
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
     * Invoked when wake word or in-app voice command is confirmed.
     * Yields the microphone and forwards seamlessly to existing AppVoiceAssistant.
     */
    private synchronized void onWakeWordTriggered(@NonNull String detectedPhrase) {
        setState(State.TRIGGERED);

        // 1. Immediately stop wake-word audio recording to release hardware microphone
        stopAudioRecordingInternal();

        Activity activity = (currentActivityRef != null) ? currentActivityRef.get() : null;
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "WakeWordManager: Target activity is no longer active. Aborting wake flow.");
            isTriggered.set(false);
            setState(State.IDLE);
            return;
        }

        if (listener != null) {
            listener.onWakeWordDetected(detectedPhrase);
        }

        // 2. Delegate directly to existing AppVoiceAssistant with candidate phrase
        AppVoiceAssistant voiceAssistant = AppVoiceAssistant.getInstance(appContext);
        Log.i(TAG, "WakeWordManager: Transferring control to AppVoiceAssistant with phrase: '" + detectedPhrase + "'");

        voiceAssistant.executeWakeWordCommand(activity, detectedPhrase, new VoiceAssistantCallback() {
            @Override
            public void onAssistantReady(@NonNull String userRole, @NonNull String languageCode) {}

            @Override
            public void onListeningStarted() {}

            @Override
            public void onListeningStopped() {}

            @Override
            public void onSpeechRecognized(@NonNull String rawText) {}

            @Override
            public void onIntentDetected(@NonNull VoiceIntent intent) {}

            @Override
            public void onCommandResolved(@NonNull VoiceCommand command) {}

            @Override
            public void onResponseSpoken(@NonNull String ttsResponse) {
                isTriggered.set(false);
                Activity act = (currentActivityRef != null) ? currentActivityRef.get() : null;
                if (act != null && !act.isFinishing() && !act.isDestroyed()) {
                    mainHandler.postDelayed(() -> resumeListening(act), 600);
                }
            }

            @Override
            public void onError(@NonNull String errorMessage, int errorCode) {
                isTriggered.set(false);
                Activity act = (currentActivityRef != null) ? currentActivityRef.get() : null;
                if (act != null && !act.isFinishing() && !act.isDestroyed()) {
                    mainHandler.postDelayed(() -> resumeListening(act), 800);
                }
            }
        });
    }

    /**
     * Pauses wake-word listening (e.g., when activity enters background, call starts, or voice recording begins).
     */
    public synchronized void pauseListening() {
        Log.d(TAG, "WakeWordManager: paused");
        mainHandler.removeCallbacks(retryRunnable);
        stopAudioRecordingInternal();
        if (currentState != State.ERROR && currentState != State.DISABLED) {
            setState(State.PAUSED);
        }
    }

    /**
     * Resumes wake-word listening when the conflicting operation has ceased.
     */
    public synchronized void resumeListening(@NonNull Activity activity) {
        Log.d(TAG, "WakeWordManager: resumed");
        isTriggered.set(false);
        startListening(activity);
    }

    /**
     * Completely stops wake-word listening and cleans up audio capture resources.
     */
    public synchronized void stopListening() {
        Log.d(TAG, "WakeWordManager: stopped");
        mainHandler.removeCallbacks(retryRunnable);
        isTriggered.set(false);
        stopAudioRecordingInternal();
        setState(State.IDLE);
    }

    /**
     * Safely terminates audio capture thread and releases AudioRecord instance.
     */
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
                threadToJoin.join(800);
            } catch (Exception ignored) {}
        }

        synchronized (recognizerLock) {
            if (voskRecognizer != null) {
                try {
                    voskRecognizer.reset();
                } catch (Throwable ignored) {}
            }
        }

        Log.i(TAG, "VOICE_MIC_OWNER = NONE");
    }

    private void releaseAudioRecord() {
        stopAudioRecordingInternal();
    }

    /**
     * Comprehensive microphone conflict check:
     * Returns true if a real-time voice call, voice recording, or other mic operation is active.
     */
    public static boolean isMicrophoneConflicted(@Nullable Activity activity) {
        if (activity == null) return false;

        // 1. WebRTC Voice Call Active
        if (AppVoiceAssistant.isVoiceCallActive(activity)) {
            return true;
        }

        // 2. Voice Message Recording Active
        if (AppVoiceAssistant.isVoiceRecordingActive(activity)) {
            return true;
        }

        // 3. Existing Voice Assistant is actively listening
        if (AppVoiceAssistant.getInstance(activity.getApplicationContext()).isListening()) {
            return true;
        }

        // 4. Audio mode in communication or in-call
        try {
            android.media.AudioManager am = (android.media.AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                int mode = am.getMode();
                if (mode == android.media.AudioManager.MODE_IN_COMMUNICATION || mode == android.media.AudioManager.MODE_IN_CALL) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        return false;
    }

    /**
     * Cleans up all resources on application shutdown.
     */
    public synchronized void destroy() {
        stopListening();
        if (voskModel != null) {
            try {
                voskModel.close();
            } catch (Exception ignored) {}
            voskModel = null;
        }
        isModelLoaded = false;
        isModelLoading = false;
        instance = null;
    }
}
