package com.kannada.speechassistant.voiceassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioAttributes;
import android.media.AudioRecordingConfiguration;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import java.io.File;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.kannada.speechassistant.AccessibleMicFeedbackManager;
import com.kannada.speechassistant.CaregiverSoundManager;
import com.kannada.speechassistant.LanguageManager;
import com.kannada.speechassistant.RoleManager;
import com.kannada.speechassistant.SessionManager;
import com.kannada.speechassistant.AdminDashboardActivity;
import com.kannada.speechassistant.BlindUserDashboardActivity;
import com.kannada.speechassistant.ChatActivity;
import com.kannada.speechassistant.MuteUserDashboardActivity;
import com.kannada.speechassistant.SpeechImpairedDashboardActivity;
import com.kannada.speechassistant.VoiceCallManager;

import android.content.SharedPreferences;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kannada.speechassistant.ChatMessage;
import com.kannada.speechassistant.NotificationService;
import com.kannada.speechassistant.LocalConnectionSimulator;
import com.google.firebase.firestore.DocumentSnapshot;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Core Controller for the App-Specific In-App Voice Assistant.
 *
 * Implements the modular architecture pipeline:
 * Speech -> Speech-to-Text -> Command Processing -> Intent Detection -> Existing App Function -> TTS Response.
 *
 * Adheres strictly to system constraints:
 * - Operates only for authenticated, logged-in users.
 * - Detects existing user roles (Mute, Deaf, Blind, Physically Disabled, Admin/Caregiver).
 * - Reuses existing SpeechRecognizer STT system.
 * - Reuses existing TextToSpeech & CaregiverSoundManager TTS system.
 * - Reuses existing LanguageManager for Kannada, Hindi, and Malayalam locale resolution.
 * - Executes commands through existing Activities/Fragments without screen duplication.
 * - Falls back to graceful TTS on unrecognized input: "Sorry, I did not understand that command."
 */
public class AppVoiceAssistant {

    private static final String TAG = "AppVoiceAssistant";

    // Request & Error Codes
    public static final int REQUEST_CODE_VOICE_ASSISTANT_PERMISSION = 909;
    public static final int ERROR_NOT_LOGGED_IN = 1001;
    public static final int ERROR_RECORD_AUDIO_PERMISSION = 1002;
    public static final int ERROR_STT_INITIALIZATION = 1003;
    public static final int ERROR_TTS_INITIALIZATION = 1004;
    public static final int ERROR_STT_RECOGNITION = 1005;
    public static final int ERROR_COMMAND_PROCESSING = 1006;
    public static final int ERROR_CALL_ACTIVE = 1007;
    public static final int ERROR_RECORDING_ACTIVE = 1008;
    public static final int ERROR_MIC_BUSY = 1009;

    private static volatile AppVoiceAssistant instance;

    private final Context appContext;
    private final SessionManager sessionManager;
    private final VoiceCommandProcessor commandProcessor;

    // Existing STT & TTS components
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;
    private boolean isListening = false;
    private WeakReference<Activity> activeActivityRef;
    private WeakReference<DeafAssistantResponseManager> deafResponseManagerRef;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Explicit Voice Assistant State Machine
    public enum AssistantState {
        IDLE,
        WAITING_FOR_COMMAND,
        EXECUTING,
        SPEAKING,
        MESSAGE_COMPOSING,
        VOICE_MESSAGE_RECORDING,
        SENDING_MESSAGE,
        READING_MESSAGE,
        MESSAGE_COUNTING,
        REPEATING_MESSAGE,
        READING_NOTIFICATIONS,
        GOING_BACK,
        WAITING_FOR_CONFIRMATION,
        WAITING_FOR_CLARIFICATION
    }

    public enum ConfirmationType {
        NONE,
        COMMAND,
        VOICE_MESSAGE
    }

    public enum ClarificationContext {
        NONE,
        HELP,
        CALL,
        MESSAGE
    }

    private volatile AssistantState currentState = AssistantState.IDLE;
    private final java.util.concurrent.atomic.AtomicBoolean commandHandled = new java.util.concurrent.atomic.AtomicBoolean(false);

    // In-memory cache of the most recent caregiver message successfully read to the blind user
    private volatile ChatMessage lastReadCaregiverMessage = null;

    @Nullable
    public ChatMessage getLastReadCaregiverMessage() {
        return lastReadCaregiverMessage;
    }

    public void setLastReadCaregiverMessage(@Nullable ChatMessage message) {
        this.lastReadCaregiverMessage = message;
    }

    private ConfirmationType pendingConfirmationType = ConfirmationType.NONE;
    private File pendingRecordedAudioFile = null;
    private int pendingRecordedAudioDuration = 0;
    private boolean isHandsFreeVoiceRecordingActive = false;
    private MediaRecorder internalMediaRecorder = null;
    private MediaPlayer activeVoicePlayer = null;

    public synchronized void stopVoicePlayer() {
        if (activeVoicePlayer != null) {
            try {
                if (activeVoicePlayer.isPlaying()) {
                    activeVoicePlayer.stop();
                }
                activeVoicePlayer.release();
            } catch (Exception ignored) {}
            activeVoicePlayer = null;
        }
    }

    public boolean isHandsFreeVoiceRecordingActive() {
        return isHandsFreeVoiceRecordingActive || currentState == AssistantState.VOICE_MESSAGE_RECORDING;
    }

    public ConfirmationType getPendingConfirmationType() {
        return pendingConfirmationType;
    }

    public void setPendingConfirmationTypeForTesting(ConfirmationType type) {
        this.pendingConfirmationType = type;
    }

    public void setPendingRecordedAudioFileForTesting(File file, int duration) {
        this.pendingRecordedAudioFile = file;
        this.pendingRecordedAudioDuration = duration;
    }

    @Nullable
    public File getPendingRecordedAudioFile() {
        return pendingRecordedAudioFile;
    }

    public int getPendingRecordedAudioDuration() {
        return pendingRecordedAudioDuration;
    }

    public void resetRecordingAndConfirmationStateForTesting() {
        this.currentState = AssistantState.IDLE;
        this.pendingConfirmationType = ConfirmationType.NONE;
        this.pendingRecordedAudioFile = null;
        this.pendingRecordedAudioDuration = 0;
        this.isHandsFreeVoiceRecordingActive = false;
    }

    public void setAssistantState(@NonNull AssistantState newState) {
        if (this.currentState != newState) {
            Log.i(TAG, "STATE: " + newState);
            Log.i(TAG, "VOICE_ASSISTANT_STATE: " + newState);
            this.currentState = newState;
        }
    }

    private synchronized void cleanupSpeechRecognizer() {
        isListening = false;
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
            } catch (Exception ignored) {}
            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {}
            speechRecognizer = null;
        }
        Log.i(TAG, "VOICE_MIC_OWNER = NONE");
    }

    @NonNull
    public AssistantState getAssistantState() {
        return currentState;
    }

    // Confirmation State Handling
    private VoiceCommand pendingConfirmationCommand = null;
    private WeakReference<Activity> pendingConfirmationActivityRef = null;
    private VoiceAssistantCallback pendingConfirmationCallback = null;

    // Clarification State Handling
    private ClarificationContext pendingClarificationContext = ClarificationContext.NONE;
    private WeakReference<Activity> pendingClarificationActivityRef = null;
    private VoiceAssistantCallback pendingClarificationCallback = null;

    private AppVoiceAssistant(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = new SessionManager(appContext);
        this.commandProcessor = new VoiceCommandProcessor(appContext);
        initTextToSpeechEngine();
    }

    @NonNull
    public static AppVoiceAssistant getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (AppVoiceAssistant.class) {
                if (instance == null) {
                    instance = new AppVoiceAssistant(context);
                }
            }
        }
        return instance;
    }

    @NonNull
    public VoiceCommandProcessor getCommandProcessor() {
        return commandProcessor;
    }

    public boolean isUserAuthenticated() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return sessionManager.isLoggedIn() && currentUser != null;
    }

    @Nullable
    public String getCurrentUserRole() {
        if (!isUserAuthenticated()) {
            return null;
        }
        String role = sessionManager.getUserRole();
        if (role != null && !role.trim().isEmpty()) {
            return role.trim();
        }
        Activity active = (activeActivityRef != null) ? activeActivityRef.get() : null;
        if (active instanceof BlindUserDashboardActivity) return RoleManager.ROLE_BLIND_USER;
        if (active instanceof MuteUserDashboardActivity) return RoleManager.ROLE_MUTE_USER;
        if (active instanceof SpeechImpairedDashboardActivity) return RoleManager.ROLE_DEAF_USER;
        if (active instanceof AdminDashboardActivity) return RoleManager.ROLE_ADMIN_CAREGIVER;
        return null;
    }

    public boolean isDeafUser(@Nullable String role) {
        // AppVoiceAssistant is dedicated 100% to Blind Users.
        // Deaf users are handled by the standalone DeafVoiceAssistant class.
        return false;
    }

    public void setDeafAssistantResponseManager(@Nullable DeafAssistantResponseManager manager) {
        // No-op in AppVoiceAssistant; visual cards are managed by DeafVoiceAssistant.
    }

    @Nullable
    public DeafAssistantResponseManager getDeafAssistantResponseManager() {
        return null;
    }

    @NonNull
    public String getCurrentLanguageCode() {
        String lang = sessionManager.getLanguage();
        return LanguageManager.normalizeLanguageCode(lang);
    }

    private void initTextToSpeechEngine() {
        try {
            textToSpeech = new TextToSpeech(appContext, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        android.media.AudioAttributes aa = new android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build();
                        textToSpeech.setAudioAttributes(aa);
                    }
                    applyUserLanguageToTts();
                    Log.i(TAG, "TTS engine initialized successfully with Google TTS in Voice Assistant.");
                } else {
                    isTtsReady = false;
                    Log.e(TAG, "Failed to initialize TTS engine in Voice Assistant: code " + status);
                }
            }, "com.google.android.tts");
        } catch (Exception e) {
            textToSpeech = new TextToSpeech(appContext, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true;
                    applyUserLanguageToTts();
                } else {
                    isTtsReady = false;
                }
            });
        }
    }

    private void applyUserLanguageToTts() {
        applyLanguageToTts(getCurrentLanguageCode());
    }

    private void applyLanguageToTts(@Nullable String langOrCode) {
        if (textToSpeech == null || !isTtsReady) return;
        String langCode = langOrCode != null && !langOrCode.trim().isEmpty()
                ? LanguageManager.normalizeLanguageCode(langOrCode)
                : getCurrentLanguageCode();
        Locale targetLocale = VoiceLanguageConfig.resolveAvailableTtsLocale(textToSpeech, langCode);
        try {
            textToSpeech.setLanguage(targetLocale);
        } catch (Throwable t) {
            Log.e(TAG, "Failed setting TTS locale: " + targetLocale, t);
        }
    }

    public static String detectScriptLanguage(@NonNull String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '\u0C80' && c <= '\u0CFF') return LanguageManager.LANG_KANNADA;
            if (c >= '\u0900' && c <= '\u097F') return LanguageManager.LANG_HINDI;
            if (c >= '\u0D00' && c <= '\u0D7F') return LanguageManager.LANG_MALAYALAM;
        }
        return null;
    }

    public void speakResponse(@NonNull String text, @Nullable Runnable onComplete) {
        speakResponseWithLanguage(text, null, onComplete);
    }

    public void speakResponseWithLanguage(@NonNull String text, @Nullable String targetLanguage, @Nullable Runnable onComplete) {
        String role = getCurrentUserRole();
        if (isDeafUser(role)) {
            Log.i(TAG, "DEAF_ASSISTANT_TTS_RESPONSE: DISABLED (Response mode = TEXT, suppressed: '" + text + "')");
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
            return;
        }

        if (!isTtsReady || textToSpeech == null) {
            Log.w(TAG, "TTS not ready to articulate response.");
            if (onComplete != null) onComplete.run();
            return;
        }

        String scriptLang = detectScriptLanguage(text);
        if (scriptLang != null) {
            applyLanguageToTts(scriptLang);
        } else if (targetLanguage != null && !targetLanguage.trim().isEmpty()) {
            applyLanguageToTts(targetLanguage);
        } else {
            applyUserLanguageToTts();
        }

        String utteranceId = "VoiceAssistantResponse_" + System.currentTimeMillis();

        if (onComplete != null) {
            final java.util.concurrent.atomic.AtomicBoolean hasCompleted = new java.util.concurrent.atomic.AtomicBoolean(false);
            final Runnable safeCompleteAction = () -> {
                if (hasCompleted.compareAndSet(false, true)) {
                    onComplete.run();
                }
            };

            // Estimate duration: ~100ms per char, min 1.5s, max 10s fallback
            long fallbackDelayMs = Math.max(1500L, Math.min(10000L, text.length() * 100L + 1500L));
            final Runnable fallbackRunnable = () -> {
                Log.w(TAG, "TTS safety fallback triggered for: " + utteranceId);
                safeCompleteAction.run();
            };
            mainHandler.postDelayed(fallbackRunnable, fallbackDelayMs);

            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String id) {}

                @Override
                public void onDone(String id) {
                    if (utteranceId.equals(id)) {
                        mainHandler.removeCallbacks(fallbackRunnable);
                        mainHandler.post(safeCompleteAction);
                    }
                }

                @Override
                public void onError(String id) {
                    if (utteranceId.equals(id)) {
                        Log.w(TAG, "TTS onError encountered for utterance: " + id);
                        mainHandler.removeCallbacks(fallbackRunnable);
                        mainHandler.postDelayed(safeCompleteAction, 800);
                    }
                }
            });
        }

        CaregiverSoundManager.speakWithVolume(textToSpeech, text, utteranceId, appContext);
    }

    @Nullable
    public TextToSpeech getEffectiveTextToSpeech(@Nullable Activity activity) {
        if (textToSpeech != null && isTtsReady) {
            return textToSpeech;
        }
        if (activity instanceof BlindUserDashboardActivity) {
            TextToSpeech dashTts = ((BlindUserDashboardActivity) activity).getTts();
            if (dashTts != null) {
                return dashTts;
            }
        }
        return isTtsReady ? textToSpeech : null;
    }

    void speakRecordingPrompt(@NonNull Activity activity,
                              @NonNull String prompt,
                              @NonNull Runnable onDoneAction,
                              @NonNull Runnable onErrorAction) {
        String role = getCurrentUserRole();
        if (isDeafUser(role)) {
            onDoneAction.run();
            return;
        }

        TextToSpeech activeTts = getEffectiveTextToSpeech(activity);
        if (activeTts == null) {
            Log.e(TAG, "TTS not ready to articulate recording prompt.");
            onErrorAction.run();
            return;
        }

        String scriptLang = detectScriptLanguage(prompt);
        String code = scriptLang != null ? scriptLang : getCurrentLanguageCode();
        Locale targetLocale = VoiceLanguageConfig.resolveAvailableTtsLocale(activeTts, code);
        try {
            activeTts.setLanguage(targetLocale);
        } catch (Throwable t) {
            Log.e(TAG, "Failed setting TTS locale for recording prompt: " + targetLocale, t);
        }

        final String utteranceId = "RECORD_PROMPT_" + System.currentTimeMillis();
        final java.util.concurrent.atomic.AtomicBoolean hasCompleted = new java.util.concurrent.atomic.AtomicBoolean(false);

        activeTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String id) {
                Log.d(TAG, "TTS onStart for recording prompt: " + id);
            }

            @Override
            public void onDone(String id) {
                if (utteranceId.equals(id)) {
                    Log.i(TAG, "TTS onDone for recording prompt: " + id);
                    if (hasCompleted.compareAndSet(false, true)) {
                        mainHandler.post(onDoneAction);
                    }
                }
            }

            @Override
            public void onError(String id) {
                if (utteranceId.equals(id)) {
                    Log.w(TAG, "TTS onError for recording prompt: " + id);
                    if (hasCompleted.compareAndSet(false, true)) {
                        mainHandler.post(onErrorAction);
                    }
                }
            }

            @Override
            public void onError(String id, int errorCode) {
                if (utteranceId.equals(id)) {
                    Log.w(TAG, "TTS onError (code " + errorCode + ") for recording prompt: " + id);
                    if (hasCompleted.compareAndSet(false, true)) {
                        mainHandler.post(onErrorAction);
                    }
                }
            }
        });

        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle params = new Bundle();
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f);
            result = activeTts.speak(prompt, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
        } else {
            HashMap<String, String> map = new HashMap<>();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            map.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "1.0");
            result = activeTts.speak(prompt, TextToSpeech.QUEUE_FLUSH, map);
        }

        if (result != TextToSpeech.SUCCESS) {
            Log.e(TAG, "TTS speak() failed to enqueue recording prompt utterance (result=" + result + ").");
            if (hasCompleted.compareAndSet(false, true)) {
                mainHandler.post(onErrorAction);
            }
        }
    }

    public void attachVoiceAssistantButton(@NonNull Activity activity,
                                          @Nullable MaterialButton button,
                                          @Nullable VoiceAssistantCallback customCallback) {
        if (button == null) return;

        button.setOnClickListener(v -> {
            if (isListening) {
                stopListening();
                button.setText("🎤 Voice Assistant");
                return;
            }

            // Safe Conflict Check: Active voice call
            if (isVoiceCallActive(activity)) {
                notifyConflict(activity, VoiceCommandConstants.MESSAGE_CONFLICT_CALL);
                return;
            }

            // Safe Conflict Check: Active voice recording
            if (isVoiceRecordingActive(activity)) {
                notifyConflict(activity, VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING);
                return;
            }

            // Safe Conflict Check: Another microphone operation
            if (isAnotherMicOperationActive(activity)) {
                notifyConflict(activity, VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING);
                return;
            }

            // Permission Check
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                String role = getCurrentUserRole();
                if (RoleManager.ROLE_BLIND_USER.equals(role)) {
                    AccessibleMicFeedbackManager.onPermissionDenied(activity, textToSpeech, isTtsReady, getCurrentLanguageCode(), () -> {
                        ActivityCompat.requestPermissions(activity,
                                new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_VOICE_ASSISTANT_PERMISSION);
                    });
                } else {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_VOICE_ASSISTANT_PERMISSION);
                }
                return;
            }

            startListeningFlow(activity, button, customCallback);
        });
    }

    public void handlePermissionsResult(@NonNull Activity activity,
                                        int requestCode,
                                        @NonNull int[] grantResults,
                                        @Nullable MaterialButton button) {
        if (requestCode == REQUEST_CODE_VOICE_ASSISTANT_PERMISSION) {
            String langCode = getCurrentLanguageCode();
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListeningFlow(activity, button, null);
            } else {
                String role = getCurrentUserRole();
                if (RoleManager.ROLE_BLIND_USER.equals(role)) {
                    AccessibleMicFeedbackManager.onPermissionDenied(activity, textToSpeech, isTtsReady, langCode, null);
                } else {
                    Toast.makeText(activity, AccessibleMicFeedbackManager.getPermissionRequiredText(langCode), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Entry point when wake word is spotted by WakeWordManager.
     * Supports both Style 1 (single utterance "Assistant, open profile") and Style 2 (two-step "Assistant").
     */
    public void executeWakeWordCommand(@NonNull Activity activity,
                                       @NonNull String detectedPhrase,
                                       @Nullable VoiceAssistantCallback customCallback) {
        Log.i(TAG, "WAKE_WORD_DETECTED: '" + detectedPhrase + "'");
        Log.i(TAG, "VOICE_ASSISTANT: Wake word detected");
        setAssistantState(AssistantState.WAITING_FOR_COMMAND);
        commandHandled.set(false);

        String userRole = getCurrentUserRole();
        String langCode = getCurrentLanguageCode();

        if (isDeafUser(userRole)) {
            DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
            if (deafResp != null) {
                deafResp.showWakeWordDetected();
            }
        }

        String commandPortion = VoiceIntentMatcher.extractCommandText(detectedPhrase);

        // RELIABILITY FIX: Always route through Android STT for command recognition.
        // Vosk uses a grammar-restricted offline model which is much less accurate than
        // Android STT (Google Speech Recognition) for command matching. We use Vosk only
        // to detect the wake word trigger. Android STT then captures the actual command.
        //
        // This covers both:
        //   Style 1: User said "Assistant, open profile" in one breath (Vosk captured command)
        //   Style 2: User said "Assistant" alone (Vosk captured only wake word)
        //
        // In both cases, we open Android STT so the user can speak their command clearly.

        boolean isBlindOrDash = RoleManager.ROLE_BLIND_USER.equals(userRole)
                || activity instanceof BlindUserDashboardActivity;

        // For Style 1 where Vosk already captured a clear command, we still go through
        // Android STT but give the user an audio hint of what was heard, allowing correction.
        // For Style 2 (command is empty), just say "Yes?" and open mic.
        String listeningCue = commandPortion.isEmpty()
                ? VoiceLanguageConfig.getListeningReadyCue(langCode)
                : null; // No cue for Style 1 - just open mic immediately

        Runnable openMicAction = () -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                setAssistantState(AssistantState.WAITING_FOR_COMMAND);
                MaterialButton btnAssistant = activity.findViewById(com.kannada.speechassistant.R.id.btnVoiceAssistant);
                startListeningFlow(activity, btnAssistant, customCallback);
            }
        };

        if (isBlindOrDash && listeningCue != null && !listeningCue.isEmpty()) {
            // Speak cue then open mic (Style 2 for blind users)
            setAssistantState(AssistantState.SPEAKING);
            speakResponse(listeningCue, openMicAction);
        } else {
            // Open mic immediately (Style 1, or non-blind Style 2)
            mainHandler.post(openMicAction);
        }
    }


    public void startListeningFlow(@NonNull Activity activity,
                                   @Nullable MaterialButton button,
                                   @Nullable VoiceAssistantCallback customCallback) {
        this.activeActivityRef = new WeakReference<>(activity);

        // 0. Release WakeWordManager AudioRecord first so it doesn't appear as a conflicting mic
        try {
            WakeWordManager.getInstance(appContext).pauseListening();
        } catch (Throwable ignored) {}
        // Also clear any stale isListening state from a previous failed session
        if (isListening && speechRecognizer == null) {
            isListening = false;
        }

        // 1. Safe Microphone Conflict Protection: Active voice call
        if (isVoiceCallActive(activity)) {
            notifyConflict(activity, VoiceCommandConstants.MESSAGE_CONFLICT_CALL);
            if (customCallback != null) {
                customCallback.onError(VoiceCommandConstants.MESSAGE_CONFLICT_CALL, ERROR_CALL_ACTIVE);
            }
            return;
        }

        // 2. Safe Microphone Conflict Protection: Active voice recording (user's voice message mic)
        if (isVoiceRecordingActive(activity)) {
            notifyConflict(activity, VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING);
            if (customCallback != null) {
                customCallback.onError(VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING, ERROR_RECORDING_ACTIVE);
            }
            return;
        }

        // 3. Safe Microphone Conflict Protection: Real competing mic use (voice call audio mode)
        if (isAnotherMicOperationActive(activity)) {
            notifyConflict(activity, VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING);
            if (customCallback != null) {
                customCallback.onError(VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING, ERROR_MIC_BUSY);
            }
            return;
        }

        String role = getCurrentUserRole();
        String langCode = getCurrentLanguageCode();

        setAssistantState(AssistantState.WAITING_FOR_COMMAND);
        commandHandled.set(false);

        VoiceAssistantCallback internalCallback = new VoiceAssistantCallback() {
            @Override
            public void onAssistantReady(@NonNull String userRole, @NonNull String languageCode) {
                if (customCallback != null) customCallback.onAssistantReady(userRole, languageCode);
            }

            @Override
            public void onListeningStarted() {
                if (button != null) {
                    button.setText("🔴 Listening...");
                }
                if (customCallback != null) customCallback.onListeningStarted();
            }

            @Override
            public void onListeningStopped() {
                if (button != null) {
                    button.setText("🎤 Voice Assistant");
                }
                if (customCallback != null) customCallback.onListeningStopped();
            }

            @Override
            public void onSpeechRecognized(@NonNull String rawText) {
                Log.i(TAG, "Voice Assistant recognized speech: " + rawText);
                AccessibleMicFeedbackManager.triggerShortVibration(activity);
                Toast.makeText(activity, "🎤 \"" + rawText + "\"", Toast.LENGTH_SHORT).show();
                if (button != null) button.setText("🎤 Voice Assistant");
                if (customCallback != null) customCallback.onSpeechRecognized(rawText);
            }

            @Override
            public void onIntentDetected(@NonNull VoiceIntent intent) {
                if (customCallback != null) customCallback.onIntentDetected(intent);
            }

            @Override
            public void onCommandResolved(@NonNull VoiceCommand command) {
                if (customCallback != null) customCallback.onCommandResolved(command);
            }

            @Override
            public void onResponseSpoken(@NonNull String ttsResponse) {
                if (customCallback != null) customCallback.onResponseSpoken(ttsResponse);
            }

            @Override
            public void onError(@NonNull String errorMessage, int errorCode) {
                Log.w(TAG, "Voice Assistant error: " + errorMessage + " (code: " + errorCode + ")");
                if (button != null) {
                    button.setText("🎤 Voice Assistant");
                }

                // Tactile feedback for blind users; spoken error is handled once by AppVoiceAssistant.speakResponse
                AccessibleMicFeedbackManager.triggerShortVibration(activity);

                if (!RoleManager.ROLE_BLIND_USER.equals(role) && !(activity instanceof BlindUserDashboardActivity)) {
                    Toast.makeText(activity, "Voice Assistant: " + errorMessage, Toast.LENGTH_SHORT).show();
                }

                if (customCallback != null) customCallback.onError(errorMessage, errorCode);
            }
        };

        // Short non-blocking haptic cue so the microphone is immediately open for user speech
        AccessibleMicFeedbackManager.triggerShortVibration(activity);
        startListening(activity, internalCallback);
    }

    public void notifyConflict(@NonNull Activity activity, @NonNull String message) {
        String role = getCurrentUserRole();
        if (isDeafUser(role)) {
            DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
            if (deafResp != null) {
                deafResp.showMicConflict(message);
            }
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        } else if (RoleManager.ROLE_BLIND_USER.equals(role) || activity instanceof BlindUserDashboardActivity) {
            speakResponse(message, null);
        } else {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        }
    }

    public void startListening(@NonNull Activity activity, @NonNull VoiceAssistantCallback callback) {
        this.activeActivityRef = new WeakReference<>(activity);

        // 0. Release WakeWordManager AudioRecord before any conflict checks
        try {
            WakeWordManager.getInstance(appContext).pauseListening();
        } catch (Throwable ignored) {}
        // Clear stale isListening state if SpeechRecognizer was already destroyed
        if (isListening && speechRecognizer == null) {
            isListening = false;
        }

        // 1. Authenticated User Check
        if (!isUserAuthenticated()) {
            callback.onError("Voice Assistant is available only to logged-in users.", ERROR_NOT_LOGGED_IN);
            return;
        }

        // 2. Safe Microphone Conflict Protection: Voice Call
        if (isVoiceCallActive(activity)) {
            callback.onError(VoiceCommandConstants.MESSAGE_CONFLICT_CALL, ERROR_CALL_ACTIVE);
            return;
        }

        // 3. Safe Microphone Conflict Protection: Voice Recording (user's voice message mic)
        if (isVoiceRecordingActive(activity)) {
            callback.onError(VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING, ERROR_RECORDING_ACTIVE);
            return;
        }

        // 4. Safe Microphone Conflict Protection: Real competing mic use (voice call audio mode)
        if (isAnotherMicOperationActive(activity)) {
            callback.onError(VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING, ERROR_MIC_BUSY);
            return;
        }

        // 5. Role Detection
        String userRole = getCurrentUserRole();
        String langCode = getCurrentLanguageCode();
        callback.onAssistantReady(userRole != null ? userRole : "Unknown", langCode);

        // 6. Audio Permission Check
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            callback.onError("RECORD_AUDIO permission is required for Voice Assistant.", ERROR_RECORD_AUDIO_PERMISSION);
            return;
        }

        // 7. Reuse existing Android SpeechRecognizer STT
        mainHandler.post(() -> launchSpeechRecognizer(activity, langCode, userRole, callback));
    }

    private void launchSpeechRecognizer(@NonNull Activity activity,
                                        @NonNull String langCode,
                                        @Nullable String userRole,
                                        @NonNull VoiceAssistantCallback callback) {
        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {}
            speechRecognizer = null;
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create SpeechRecognizer", e);
            callback.onError("Failed to instantiate speech recognizer.", ERROR_STT_INITIALIZATION);
            return;
        }

        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        String speechLanguageTag = VoiceLanguageConfig.getSpeechRecognizerLanguageTag(langCode);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLanguageTag);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, speechLanguageTag);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                isListening = true;
                Log.i(TAG, "VOICE_ASSISTANT: Command listening started");
                Log.i(TAG, "VOICE_MIC_OWNER = COMMAND_STT");
                Log.i(TAG, "COMMAND_LISTENING_STARTED");
                Log.i(TAG, "COMMAND_STT_STARTED");
                if (isDeafUser(userRole)) {
                    DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                    if (deafResp != null) {
                        deafResp.showListeningState();
                    }
                }
                callback.onListeningStarted();
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                isListening = false;
                Log.i(TAG, "COMMAND_STT_STOPPED");
                if (isDeafUser(userRole)) {
                    DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                    if (deafResp != null) {
                        deafResp.showProcessingState();
                    }
                }
                callback.onListeningStopped();
            }

            @Override
            public void onError(int error) {
                isListening = false;
                callback.onListeningStopped();
                cleanupSpeechRecognizer();

                if (isDeafUser(userRole)) {
                    DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                    if (deafResp != null) {
                        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            deafResp.showPleaseSpeakCommand();
                        } else {
                            deafResp.showError(null);
                        }
                    }
                }

                // If a command was already handled in this session, ignore later error callback
                if (!commandHandled.compareAndSet(false, true)) {
                    return;
                }

                Log.w(TAG, "STT Recognition error: code " + error);
                Log.i(TAG, "VOICE_ERROR_HANDLED_ONCE");

                // If the assistant was actively awaiting a command, speak at most one short notification
                if (currentState == AssistantState.WAITING_FOR_COMMAND) {
                    setAssistantState(AssistantState.SPEAKING);
                    String errorText = RoleManager.ROLE_BLIND_USER.equals(userRole) || activity instanceof BlindUserDashboardActivity
                            ? AccessibleMicFeedbackManager.getNoSpeechErrorText(langCode)
                            : VoiceCommandConstants.RESPONSE_UNKNOWN;
                    Log.i(TAG, "TTS_STARTED: " + errorText);
                    speakResponse(errorText, () -> {
                        Log.i(TAG, "TTS_COMPLETED: " + errorText);
                        callback.onError(errorText, ERROR_STT_RECOGNITION);
                        onAssistantFinished(activity);
                    });
                } else {
                    onAssistantFinished(activity);
                }
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                callback.onListeningStopped();
                cleanupSpeechRecognizer();

                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches == null || matches.isEmpty()) {
                    if (isDeafUser(userRole)) {
                        DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                        if (deafResp != null) {
                            deafResp.showPleaseSpeakCommand();
                        }
                    }
                    if (commandHandled.compareAndSet(false, true)) {
                        Log.i(TAG, "VOICE_ERROR_HANDLED_ONCE");
                        if (currentState == AssistantState.WAITING_FOR_COMMAND) {
                            setAssistantState(AssistantState.SPEAKING);
                            String errorText = RoleManager.ROLE_BLIND_USER.equals(userRole) || activity instanceof BlindUserDashboardActivity
                                    ? AccessibleMicFeedbackManager.getNoSpeechErrorText(langCode)
                                    : VoiceCommandConstants.RESPONSE_UNKNOWN;
                            Log.i(TAG, "TTS_STARTED: " + errorText);
                            speakResponse(errorText, () -> {
                                Log.i(TAG, "TTS_COMPLETED: " + errorText);
                                callback.onError("No speech recognized.", ERROR_STT_RECOGNITION);
                                onAssistantFinished(activity);
                            });
                        } else {
                            onAssistantFinished(activity);
                        }
                    }
                    return;
                }

                String recognizedUtterance = matches.get(0);
                Log.i(TAG, "COMMAND_RECOGNIZED: " + recognizedUtterance);
                Log.i(TAG, "VOICE_ASSISTANT: Recognized command = " + recognizedUtterance);

                // Robust Command Gate:
                // Process if either:
                // 1) The assistant was previously placed in WAITING_FOR_COMMAND (e.g. by wake word detection or mic button)
                // 2) The utterance itself starts with or contains the wake word ("Assistant", "Hey Assistant", "ಹೇ ಅಸಿಸ್ಟೆಂಟ್")
                // 3) Or the utterance itself matches any recognized system command (e.g. "go back", "ಹಿಂದೆ ಹೋಗು")
                boolean hasWake = VoiceIntentMatcher.hasWakeWord(recognizedUtterance);
                boolean wasWaiting = (currentState == AssistantState.WAITING_FOR_COMMAND);
                boolean hasValidIntent = VoiceIntentMatcher.match(recognizedUtterance) != VoiceIntentType.UNKNOWN;

                if (!wasWaiting && !hasWake && !hasValidIntent) {
                    Log.i(TAG, "VOICE_ASSISTANT_STATE: IDLE (Ignored non-activated speech without wake word or valid intent: '" + recognizedUtterance + "')");
                    setAssistantState(AssistantState.IDLE);
                    onAssistantFinished(activity);
                    return;
                }

                // Single Command Latch: Exactly ONE valid command executed per session
                if (!commandHandled.compareAndSet(false, true)) {
                    Log.w(TAG, "Command already handled for this session. Ignoring duplicate callback: " + recognizedUtterance);
                    return;
                }

                callback.onSpeechRecognized(recognizedUtterance);

                String commandPortion = VoiceIntentMatcher.extractCommandText(recognizedUtterance);
                if (commandPortion.isEmpty()) {
                    if (hasWake) {
                        // User said only "Assistant" into STT: Transition to WAITING_FOR_COMMAND and start second step
                        setAssistantState(AssistantState.WAITING_FOR_COMMAND);
                        commandHandled.set(false);
                        if (isDeafUser(userRole)) {
                            DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                            if (deafResp != null) {
                                deafResp.showListeningState();
                            }
                        }
                        AccessibleMicFeedbackManager.triggerShortVibration(activity);
                        startListening(activity, callback);
                        return;
                    }
                    commandPortion = recognizedUtterance;
                } else if (hasValidIntent && VoiceIntentMatcher.match(commandPortion) == VoiceIntentType.UNKNOWN) {
                    // If stripping prefix lost a recognized command, fall back to full utterance
                    commandPortion = recognizedUtterance;
                }

                setAssistantState(AssistantState.EXECUTING);
                Log.i(TAG, "COMMAND_EXECUTION_STARTED: '" + commandPortion + "'");
                if (isDeafUser(userRole)) {
                    DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                    if (deafResp != null) {
                        deafResp.showProcessingState();
                    }
                }

                // Pipeline: STT -> Command Processing -> Intent Detection -> Execution -> TTS
                executeVoiceCommandFlow(activity, commandPortion, userRole, langCode, callback);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(recognizerIntent);
    }

    private void executeVoiceCommandFlow(@NonNull Activity activity,
                                         @NonNull String recognizedUtterance,
                                         @Nullable String userRole,
                                         @NonNull String langCode,
                                         @NonNull VoiceAssistantCallback callback) {
        // 0. Check if voice recording is active
        if (currentState == AssistantState.VOICE_MESSAGE_RECORDING || isHandsFreeVoiceRecordingActive) {
            handleRecordingResponseFlow(activity, recognizedUtterance, userRole, langCode, callback);
            return;
        }

        // 0.5 Check if we are currently awaiting voice message send confirmation
        if (pendingConfirmationType == ConfirmationType.VOICE_MESSAGE) {
            handleVoiceMessageConfirmationFlow(activity, recognizedUtterance, userRole, langCode, callback);
            return;
        }

        // 1. Check if we are currently awaiting confirmation
        if (pendingConfirmationCommand != null) {
            handleConfirmationResponseFlow(activity, recognizedUtterance, userRole, langCode, callback);
            return;
        }

        // 1.5 Check if we are currently awaiting clarification
        if (pendingClarificationContext != ClarificationContext.NONE) {
            handleClarificationResponseFlow(activity, recognizedUtterance, userRole, langCode, callback);
            return;
        }

        // 1.8 Context-aware call termination during an active call
        if (isVoiceCallActive(activity)) {
            String s = VoiceIntentMatcher.normalizeText(recognizedUtterance);
            if (s.equals("disconnect") || s.equals("hang up") || s.equals("end the call") ||
                    s.equals("stop the call") || s.equals("please stop the call") ||
                    s.equals("cut call") || s.equals("drop call")) {
                VoiceCommand endCallCmd = new VoiceCommand(VoiceCommandConstants.CMD_END_CALL,
                        new VoiceIntent(VoiceIntentType.END_CALL, recognizedUtterance),
                        VoiceCommandProcessor.ALL_ROLES, false);
                dispatchResolvedCommand(activity, endCallCmd, userRole, callback);
                return;
            }
        }

        commandProcessor.processSpokenText(recognizedUtterance, userRole, langCode, new VoiceCommandProcessor.ProcessingCallback() {
            @Override
            public void onIntentDetected(@NonNull VoiceIntent intent) {
                Log.i(TAG, "VOICE_ASSISTANT: Intent = " + intent.getIntentType().name());
                callback.onIntentDetected(intent);
            }

            @Override
            public void onCommandResolved(@NonNull VoiceCommand command) {
                callback.onCommandResolved(command);
            }

            @Override
            public void onProcessingComplete(@NonNull VoiceCommand command, @Nullable String proposedResponse) {
                if (activity.isFinishing() || activity.isDestroyed()) {
                    onAssistantFinished(activity);
                    return;
                }

                // Check for ambiguous requests if intent is UNKNOWN
                if (command.getIntent().getIntentType() == VoiceIntentType.UNKNOWN) {
                    if (VoiceIntentMatcher.isAmbiguousHelpRequest(recognizedUtterance)) {
                        initiateClarification(activity, ClarificationContext.HELP, callback);
                        return;
                    }
                    if (VoiceIntentMatcher.isAmbiguousCallRequest(recognizedUtterance)) {
                        initiateClarification(activity, ClarificationContext.CALL, callback);
                        return;
                    }
                    if (VoiceIntentMatcher.isAmbiguousMessageRequest(recognizedUtterance)) {
                        initiateClarification(activity, ClarificationContext.MESSAGE, callback);
                        return;
                    }
                }

                // 2. Check if this command requires confirmation first
                if (command.requiresConfirmation()) {
                    if (!command.isRoleAuthorized(userRole)) {
                        String unauthorizedResponse = RoleManager.ROLE_BLIND_USER.equals(userRole)
                                ? "That function is not available."
                                : VoiceCommandConstants.RESPONSE_ROLE_UNAUTHORIZED;
                        if (isDeafUser(userRole)) {
                            DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                            if (deafResp != null) {
                                deafResp.showFunctionNotAvailable();
                            }
                        }
                        setAssistantState(AssistantState.SPEAKING);
                        speakResponse(unauthorizedResponse, () -> {
                            callback.onResponseSpoken(unauthorizedResponse);
                            onAssistantFinished(activity);
                        });
                        return;
                    }

                    pendingConfirmationCommand = command;
                    pendingConfirmationActivityRef = new WeakReference<>(activity);
                    pendingConfirmationCallback = callback;

                    String confirmationPrompt = getConfirmationPrompt(command.getCommandId());
                    if (isDeafUser(userRole)) {
                        DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                        if (deafResp != null) {
                            deafResp.showAssistantMessage(confirmationPrompt);
                        }
                    }
                    setAssistantState(AssistantState.SPEAKING);
                    speakResponse(confirmationPrompt, () -> {
                        callback.onResponseSpoken(confirmationPrompt);
                        mainHandler.postDelayed(() -> {
                            if (!activity.isFinishing() && !activity.isDestroyed()) {
                                setAssistantState(AssistantState.WAITING_FOR_CONFIRMATION);
                                commandHandled.set(false);
                                startListening(activity, callback);
                            }
                        }, 250);
                    });
                    return;
                }

                // 3. Immediate execution without confirmation (simple navigation, calls, etc.)
                dispatchResolvedCommand(activity, command, userRole, callback);
            }

            @Override
            public void onProcessingFailed(@NonNull String errorMessage) {
                // If processing failed, check if it was ambiguous before reporting error
                if (VoiceIntentMatcher.isAmbiguousHelpRequest(recognizedUtterance)) {
                    initiateClarification(activity, ClarificationContext.HELP, callback);
                    return;
                }
                if (VoiceIntentMatcher.isAmbiguousCallRequest(recognizedUtterance)) {
                    initiateClarification(activity, ClarificationContext.CALL, callback);
                    return;
                }
                if (VoiceIntentMatcher.isAmbiguousMessageRequest(recognizedUtterance)) {
                    initiateClarification(activity, ClarificationContext.MESSAGE, callback);
                    return;
                }

                Log.i(TAG, "VOICE_ERROR_HANDLED_ONCE");
                setAssistantState(AssistantState.SPEAKING);
                if (isDeafUser(userRole)) {
                    DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                    if (deafResp != null) {
                        deafResp.showCommandNotRecognized();
                    }
                }
                String response = RoleManager.ROLE_BLIND_USER.equals(userRole)
                        ? "Sorry, I did not understand."
                        : VoiceCommandConstants.RESPONSE_UNKNOWN;
                Log.i(TAG, "TTS_STARTED: " + response);
                speakResponse(response, () -> {
                    Log.i(TAG, "TTS_COMPLETED: " + response);
                    callback.onError(errorMessage, ERROR_COMMAND_PROCESSING);
                    onAssistantFinished(activity);
                });
            }
        });
    }

    public void initiateClarification(@NonNull Activity activity,
                                      @NonNull ClarificationContext context,
                                      @NonNull VoiceAssistantCallback callback) {
        pendingClarificationContext = context;
        pendingClarificationActivityRef = new WeakReference<>(activity);
        pendingClarificationCallback = callback;

        String prompt = getClarifyPrompt(context, getCurrentLanguageCode());
        if (isDeafUser(getCurrentUserRole())) {
            DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
            if (deafResp != null) {
                deafResp.showAssistantMessage(prompt);
            }
        }

        setAssistantState(AssistantState.SPEAKING);
        speakResponse(prompt, () -> {
            callback.onResponseSpoken(prompt);
            mainHandler.postDelayed(() -> {
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    setAssistantState(AssistantState.WAITING_FOR_CLARIFICATION);
                    commandHandled.set(false);
                    startListening(activity, callback);
                }
            }, 250);
        });
    }

    public void handleClarificationResponseFlow(@NonNull Activity activity,
                                                @NonNull String recognizedUtterance,
                                                @Nullable String userRole,
                                                @NonNull String langCode,
                                                @NonNull VoiceAssistantCallback callback) {
        ClarificationContext context = pendingClarificationContext;
        clearPendingClarification();

        String s = VoiceIntentMatcher.normalizeText(recognizedUtterance);
        Set<String> words = new HashSet<>(Arrays.asList(s.split(" ")));
        VoiceIntent intent = commandProcessor.detectIntent(recognizedUtterance, langCode);

        // 1. Cancellation check
        if (intent.getIntentType() == VoiceIntentType.CONFIRM_NO ||
                s.equals("no") || s.equals("cancel") || s.equals("neither") || s.equals("none") ||
                s.equals("don t") || s.equals("dont") || s.equals("stop") ||
                s.equals("ಇಲ್ಲ") || s.equals("ಬೇಡ") || s.equals("ರದ್ದು") ||
                s.equals("नहीं") || s.equals("रद्द") || s.equals("मत करो") ||
                s.equals("വേണ്ട") || s.equals("ഇല്ല") || s.equals("റദ്ദാക്കുക")) {
            setAssistantState(AssistantState.SPEAKING);
            String cancelResp = VoiceLanguageConfig.getActionCancelledResponse(langCode);
            speakResponse(cancelResp, () -> {
                callback.onResponseSpoken(cancelResp);
                onAssistantFinished(activity);
            });
            return;
        }

        // 2. Context-specific clarification choices
        if (context == ClarificationContext.HELP) {
            boolean wantsCall = words.contains("call") || words.contains("caregiver") || words.contains("helper") ||
                    words.contains("phone") || words.contains("talk") || words.contains("speak") ||
                    VoiceIntentMatcher.containsKannadaCaregiverAny(s) || s.contains("ಕರೆ") || s.contains("ಕಾಲ್") ||
                    VoiceIntentMatcher.containsHindiCaregiverAny(s) || s.contains("कॉल") || s.contains("फोन") ||
                    VoiceIntentMatcher.containsMalayalamCaregiverAny(s) || s.contains("വിളി") || s.contains("കോൾ");

            boolean wantsEmergency = words.contains("emergency") || words.contains("alert") || words.contains("sos") ||
                    s.contains("ತುರ್ತು") || s.contains("ಎಚ್ಚರಿಕೆ") || s.contains("ಆಪತ್ಕಾಲೀನ") ||
                    s.contains("आपातकाल") || s.contains("अलर्ट") || s.contains("इमरजेंसी") ||
                    s.contains("അടിയന്തര") || s.contains("അലർട്ട്") || s.contains("മുന്നറിയിപ്പ്");

            if (wantsCall && !wantsEmergency) {
                VoiceCommand callCmd = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_VOICE_CALL,
                        new VoiceIntent(VoiceIntentType.CALL_CAREGIVER, recognizedUtterance),
                        VoiceCommandProcessor.BLIND_ONLY_ROLES, false);
                dispatchResolvedCommand(activity, callCmd, userRole, callback);
                return;
            } else if (wantsEmergency && !wantsCall) {
                VoiceCommand emergCmd = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_EMERGENCY_ALERT,
                        new VoiceIntent(VoiceIntentType.OPEN_EMERGENCY_ALERT, recognizedUtterance),
                        VoiceCommandProcessor.ALL_ROLES, false);
                dispatchResolvedCommand(activity, emergCmd, userRole, callback);
                return;
            }
        } else if (context == ClarificationContext.CALL) {
            boolean isAffirmative = intent.getIntentType() == VoiceIntentType.CONFIRM_YES ||
                    words.contains("yes") || words.contains("yeah") || words.contains("call") || words.contains("caregiver") ||
                    s.contains("ಹೌದು") || s.contains("ಕರೆ") || s.contains("ಕಾಲ್") ||
                    s.contains("हाँ") || s.contains("कॉल") ||
                    s.contains("അതെ") || s.contains("വിളി");

            if (isAffirmative) {
                VoiceCommand callCmd = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_VOICE_CALL,
                        new VoiceIntent(VoiceIntentType.CALL_CAREGIVER, recognizedUtterance),
                        VoiceCommandProcessor.BLIND_ONLY_ROLES, false);
                dispatchResolvedCommand(activity, callCmd, userRole, callback);
                return;
            }
        } else if (context == ClarificationContext.MESSAGE) {
            boolean isAffirmative = intent.getIntentType() == VoiceIntentType.CONFIRM_YES ||
                    words.contains("yes") || words.contains("send") || words.contains("message") ||
                    s.contains("ಹೌದು") || s.contains("ಸಂದೇಶ") || s.contains("ಕಳುಹಿಸು") ||
                    s.contains("हाँ") || s.contains("संदेश") || s.contains("भेजो") ||
                    s.contains("അതെ") || s.contains("ಸന്ദേശം") || s.contains("അയക്കൂ");

            if (isAffirmative) {
                handleSendMessageFlow(activity, userRole, callback);
                return;
            }
        }

        // Unrecognized response during clarification
        setAssistantState(AssistantState.SPEAKING);
        String unrecResp = RoleManager.ROLE_BLIND_USER.equals(userRole)
                ? "Sorry, I did not understand."
                : VoiceCommandConstants.RESPONSE_UNKNOWN;
        speakResponse(unrecResp, () -> {
            callback.onError(unrecResp, ERROR_COMMAND_PROCESSING);
            onAssistantFinished(activity);
        });
    }

    private void handleConfirmationResponseFlow(@NonNull Activity activity,
                                                 @NonNull String recognizedUtterance,
                                                 @Nullable String userRole,
                                                 @NonNull String langCode,
                                                 @NonNull VoiceAssistantCallback callback) {
        final VoiceCommand pendingCmd = pendingConfirmationCommand;
        final Activity targetActivity = (pendingConfirmationActivityRef != null && pendingConfirmationActivityRef.get() != null)
                ? pendingConfirmationActivityRef.get()
                : activity;

        VoiceIntent confirmIntent = commandProcessor.detectIntent(recognizedUtterance, langCode);
        callback.onIntentDetected(confirmIntent);

        if (confirmIntent.getIntentType() == VoiceIntentType.CONFIRM_YES) {
            clearPendingConfirmation();
            String confirmedSpokenText = getConfirmedExecutionResponse(pendingCmd.getCommandId(), userRole);
            if (isDeafUser(userRole)) {
                DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                if (deafResp != null) {
                    deafResp.showSuccess(confirmedSpokenText);
                }
            }
            setAssistantState(AssistantState.SPEAKING);
            speakResponse(confirmedSpokenText, () -> {
                callback.onResponseSpoken(confirmedSpokenText);
                dispatchResolvedCommand(targetActivity, pendingCmd, userRole, callback);
            });
        } else if (confirmIntent.getIntentType() == VoiceIntentType.CONFIRM_NO ||
                confirmIntent.getIntentType() == VoiceIntentType.STOP_LISTENING) {
            clearPendingConfirmation();
            String cancelResponse = VoiceCommandConstants.RESPONSE_ACTION_CANCELLED;
            if (isDeafUser(userRole)) {
                DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                if (deafResp != null) {
                    deafResp.showAssistantMessage(cancelResponse);
                }
            }
            setAssistantState(AssistantState.SPEAKING);
            speakResponse(cancelResponse, () -> {
                callback.onResponseSpoken(cancelResponse);
                onAssistantFinished(activity);
            });
        } else {
            clearPendingConfirmation();
            String cancelResponse = RoleManager.ROLE_BLIND_USER.equals(userRole)
                    ? "I did not understand the command."
                    : VoiceCommandConstants.RESPONSE_ACTION_CANCELLED;
            if (isDeafUser(userRole)) {
                DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                if (deafResp != null) {
                    deafResp.showCommandNotRecognized();
                }
            }
            setAssistantState(AssistantState.SPEAKING);
            speakResponse(cancelResponse, () -> {
                callback.onResponseSpoken(cancelResponse);
                onAssistantFinished(activity);
            });
        }
    }

    private void dispatchResolvedCommand(@NonNull Activity activity,
                                         @NonNull VoiceCommand command,
                                         @Nullable String userRole,
                                         @NonNull VoiceAssistantCallback callback) {
        if (VoiceCommandConstants.CMD_RECORD_VOICE_MESSAGE.equals(command.getCommandId())
                || (VoiceCommandConstants.CMD_OPEN_VOICE_RECORDER.equals(command.getCommandId())
                && (RoleManager.ROLE_BLIND_USER.equals(userRole) || activity instanceof BlindUserDashboardActivity))) {
            handleVoiceRecordingFlow(activity, userRole, callback);
            return;
        }

        if (VoiceCommandConstants.CMD_SEND_MESSAGE.equals(command.getCommandId())) {
            handleSendMessageFlow(activity, userRole, callback);
            return;
        }

        if (VoiceCommandConstants.CMD_READ_MESSAGES.equals(command.getCommandId())) {
            handleReadMessagesFlow(activity, userRole, callback);
            return;
        }

        if (VoiceCommandConstants.CMD_MESSAGE_COUNT.equals(command.getCommandId())) {
            handleMessageCountFlow(activity, userRole, callback);
            return;
        }

        if (VoiceCommandConstants.CMD_REPEAT_MESSAGE.equals(command.getCommandId())) {
            handleRepeatMessageFlow(activity, userRole, callback);
            return;
        }

        if (VoiceCommandConstants.CMD_READ_NOTIFICATIONS.equals(command.getCommandId())) {
            handleReadNotificationsFlow(activity, userRole, callback);
            return;
        }

        if (VoiceCommandConstants.CMD_GO_BACK.equals(command.getCommandId())) {
            handleGoBackFlow(activity, userRole, callback);
            return;
        }

        VoiceActionDispatcher.executeCommand(activity, command, userRole, new VoiceActionDispatcher.DispatchCallback() {
            @Override
            public void onSuccess(@NonNull String spokenResponse) {
                Log.i(TAG, "COMMAND_EXECUTED_ONCE: " + command.getCommandId());
                String finalResponse = spokenResponse;
                if (RoleManager.ROLE_BLIND_USER.equals(userRole) || activity instanceof BlindUserDashboardActivity) {
                    finalResponse = getBlindUserSpokenResponse(command.getCommandId(), spokenResponse, getCurrentLanguageCode());
                }

                // If outgoing call was started, VoiceCallActivity takes ownership of UI and accessible audio feedback
                if (VoiceCommandConstants.CMD_OPEN_VOICE_CALL.equals(command.getCommandId())) {
                    if (VoiceCommandConstants.RESPONSE_OPEN_VOICE_CALL.equals(spokenResponse)) {
                        Log.i(TAG, "VOICE_CALL_STARTED_BY_ACTIVITY: Yielding microphone and audio control to VoiceCallActivity");
                        cleanupSpeechRecognizer();
                        Log.i(TAG, "VOICE_MIC_OWNER = NONE");
                        WakeWordManager.getInstance(appContext).pauseListening();
                        setAssistantState(AssistantState.IDLE);
                        commandHandled.set(false);
                        callback.onResponseSpoken(finalResponse);
                        return;
                    }
                }

                // If incoming call was accepted, VoiceCallActivity takes ownership of UI and accessible audio feedback
                if (VoiceCommandConstants.CMD_ACCEPT_CALL.equals(command.getCommandId())) {
                    Log.i(TAG, "INCOMING_CALL_ACCEPTED: Yielding microphone and audio control to VoiceCallActivity");
                    cleanupSpeechRecognizer();
                    Log.i(TAG, "VOICE_MIC_OWNER = NONE");
                    WakeWordManager.getInstance(appContext).pauseListening();
                    setAssistantState(AssistantState.IDLE);
                    commandHandled.set(false);
                    callback.onResponseSpoken(finalResponse);
                    return;
                }

                // If emergency was triggered, EmergencyActivity takes ownership of UI and accessible alarm/audio feedback
                if (VoiceCommandConstants.CMD_OPEN_EMERGENCY.equals(command.getCommandId())
                        || VoiceCommandConstants.CMD_OPEN_EMERGENCY_ALERT.equals(command.getCommandId())) {
                    if (!(activity instanceof BlindUserDashboardActivity)) {
                        Log.i(TAG, "EMERGENCY_TRIGGERED: Yielding microphone and audio control to EmergencyActivity");
                        cleanupSpeechRecognizer();
                        WakeWordManager.getInstance(appContext).pauseListening();
                        setAssistantState(AssistantState.IDLE);
                        commandHandled.set(false);
                        callback.onResponseSpoken(finalResponse);
                        return;
                    }
                }

                String finalCalculatedResponse = finalResponse;
                if (isDeafUser(userRole)) {
                    finalCalculatedResponse = VoiceLanguageConfig.getLocalizedResponse(command.getCommandId(), spokenResponse, getCurrentLanguageCode());
                    DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                    if (deafResp != null) {
                        deafResp.showSuccess(finalCalculatedResponse);
                    }
                }
                final String responseToSpeak = finalCalculatedResponse;
                setAssistantState(AssistantState.SPEAKING);
                Log.i(TAG, "TTS_STARTED: " + responseToSpeak);
                speakResponse(responseToSpeak, () -> {
                    Log.i(TAG, "TTS_COMPLETED: " + responseToSpeak);
                    callback.onResponseSpoken(responseToSpeak);
                    onAssistantFinished(activity);
                });
            }

            @Override
            public void onUnauthorized(@NonNull String spokenResponse) {
                String response = RoleManager.ROLE_BLIND_USER.equals(userRole)
                        ? "That function is not available."
                        : VoiceCommandConstants.RESPONSE_ROLE_UNAUTHORIZED;
                if (isDeafUser(userRole)) {
                    DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                    if (deafResp != null) {
                        deafResp.showFunctionNotAvailable();
                    }
                }
                setAssistantState(AssistantState.SPEAKING);
                Log.i(TAG, "TTS_STARTED: " + response);
                speakResponse(response, () -> {
                    Log.i(TAG, "TTS_COMPLETED: " + response);
                    callback.onResponseSpoken(response);
                    onAssistantFinished(activity);
                });
            }

            @Override
            public void onUnrecognized(@NonNull String spokenResponse) {
                String response = RoleManager.ROLE_BLIND_USER.equals(userRole)
                        ? "Sorry, I did not understand."
                        : VoiceCommandConstants.RESPONSE_UNKNOWN;
                if (isDeafUser(userRole)) {
                    DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                    if (deafResp != null) {
                        deafResp.showCommandNotRecognized();
                    }
                }
                setAssistantState(AssistantState.SPEAKING);
                Log.i(TAG, "TTS_STARTED: " + response);
                speakResponse(response, () -> {
                    Log.i(TAG, "TTS_COMPLETED: " + response);
                    callback.onResponseSpoken(response);
                    onAssistantFinished(activity);
                });
            }
        });
    }

    private void handleSendMessageFlow(@NonNull Activity activity,
                                       @Nullable String userRole,
                                       @NonNull VoiceAssistantCallback callback) {
        Log.i(TAG, "COMMAND_EXECUTED_ONCE: " + VoiceCommandConstants.CMD_SEND_MESSAGE);

        // 1. Voice call active check
        if (isVoiceCallActive(activity)) {
            Log.i(TAG, "SEND_MESSAGE rejected: active call");
            setAssistantState(AssistantState.SPEAKING);
            String callResp = VoiceLanguageConfig.getCallActiveResponse(getCurrentLanguageCode());
            if (isDeafUser(userRole)) {
                DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                if (deafResp != null) {
                    deafResp.showMicConflict(callResp);
                }
            }
            Log.i(TAG, "TTS_STARTED: " + callResp);
            speakResponse(callResp, () -> {
                Log.i(TAG, "TTS_COMPLETED: " + callResp);
                callback.onResponseSpoken(callResp);
                onAssistantFinished(activity);
            });
            return;
        }

        // 2. Caregiver connected check
        if (!isCaregiverConnected(activity, appContext)) {
            Log.i(TAG, "SEND_MESSAGE rejected: no caregiver connected");
            setAssistantState(AssistantState.SPEAKING);
            String noCaregiverResp = VoiceLanguageConfig.getNoCaregiverResponse(getCurrentLanguageCode());
            if (isDeafUser(userRole)) {
                DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                if (deafResp != null) {
                    deafResp.showError(noCaregiverResp);
                }
            }
            Log.i(TAG, "TTS_STARTED: " + noCaregiverResp);
            speakResponse(noCaregiverResp, () -> {
                Log.i(TAG, "TTS_COMPLETED: " + noCaregiverResp);
                callback.onResponseSpoken(noCaregiverResp);
                onAssistantFinished(activity);
            });
            return;
        }

        // 3. Prompt user for message content
        setAssistantState(AssistantState.SPEAKING);
        String promptText = VoiceLanguageConfig.getSayMessagePrompt(getCurrentLanguageCode());
        if (isDeafUser(userRole)) {
            DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
            if (deafResp != null) {
                deafResp.showAssistantMessage(promptText);
            }
        }
        Log.i(TAG, "TTS_STARTED: " + promptText);
        speakResponse(promptText, () -> {
            Log.i(TAG, "TTS_COMPLETED: " + promptText);
            callback.onResponseSpoken(promptText);

            // Settle audio and start message listening
            mainHandler.postDelayed(() -> {
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    startMessageCompositionListening(activity, callback);
                } else {
                    onAssistantFinished(activity);
                }
            }, 300);
        });
    }

    private void startMessageCompositionListening(@NonNull Activity activity,
                                                  @NonNull VoiceAssistantCallback callback) {
        setAssistantState(AssistantState.MESSAGE_COMPOSING);
        Log.i(TAG, "VOICE_MIC_OWNER = MESSAGE_STT");
        Log.i(TAG, "MESSAGE_COMPOSING_STARTED");

        cleanupSpeechRecognizer();
        WakeWordManager.getInstance(appContext).pauseListening();

        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            Log.e(TAG, "STT recognition unavailable on device for message composition");
            setAssistantState(AssistantState.SPEAKING);
            String errorResp = VoiceLanguageConfig.getNoMessageHeardResponse(getCurrentLanguageCode());
            if (isDeafUser(getCurrentUserRole())) {
                DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                if (deafResp != null) {
                    deafResp.showError(errorResp);
                }
            }
            speakResponse(errorResp, () -> {
                callback.onError(errorResp, ERROR_STT_INITIALIZATION);
                onAssistantFinished(activity);
            });
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext);
        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        String langCode = getCurrentLanguageCode();
        String resolvedLocale = VoiceLanguageConfig.getSpeechRecognizerLanguageTag(langCode);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, resolvedLocale);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, resolvedLocale);

        final AtomicBoolean messageProcessed = new AtomicBoolean(false);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                isListening = true;
                Log.i(TAG, "VOICE_MIC_OWNER = MESSAGE_STT");
                Log.i(TAG, "MESSAGE_LISTENING_STARTED");
                if (isDeafUser(getCurrentUserRole())) {
                    DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                    if (deafResp != null) {
                        deafResp.showListeningState();
                    }
                }
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                isListening = false;
                Log.i(TAG, "MESSAGE_STT_STOPPED");
                if (isDeafUser(getCurrentUserRole())) {
                    DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                    if (deafResp != null) {
                        deafResp.showProcessingState();
                    }
                }
            }

            @Override
            public void onError(int error) {
                isListening = false;
                cleanupSpeechRecognizer();

                if (!messageProcessed.compareAndSet(false, true)) {
                    return;
                }

                Log.w(TAG, "Message STT Recognition error: code " + error);
                setAssistantState(AssistantState.SPEAKING);
                String errorText = VoiceLanguageConfig.getNoMessageHeardResponse(getCurrentLanguageCode());
                if (isDeafUser(getCurrentUserRole())) {
                    DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                    if (deafResp != null) {
                        deafResp.showError(errorText);
                    }
                }
                Log.i(TAG, "TTS_STARTED: " + errorText);
                speakResponse(errorText, () -> {
                    Log.i(TAG, "TTS_COMPLETED: " + errorText);
                    callback.onError(errorText, ERROR_STT_RECOGNITION);
                    onAssistantFinished(activity);
                });
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                cleanupSpeechRecognizer();

                if (!messageProcessed.compareAndSet(false, true)) {
                    return;
                }

                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches == null || matches.isEmpty() || matches.get(0) == null || matches.get(0).trim().isEmpty()) {
                    setAssistantState(AssistantState.SPEAKING);
                    String errorText = VoiceLanguageConfig.getNoMessageHeardResponse(getCurrentLanguageCode());
                    if (isDeafUser(getCurrentUserRole())) {
                        DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                        if (deafResp != null) {
                            deafResp.showError(errorText);
                        }
                    }
                    Log.i(TAG, "TTS_STARTED: " + errorText);
                    speakResponse(errorText, () -> {
                        Log.i(TAG, "TTS_COMPLETED: " + errorText);
                        callback.onError(errorText, ERROR_STT_RECOGNITION);
                        onAssistantFinished(activity);
                    });
                    return;
                }

                String rawMessage = matches.get(0).trim();
                Log.i(TAG, "MESSAGE_CONTENT_CAPTURED: '" + rawMessage + "'");

                // Check for cancellation during message composition
                if (VoiceIntentMatcher.isCancelMessagePhrase(rawMessage)) {
                    Log.i(TAG, "MESSAGE_COMPOSITION_CANCELLED");
                    setAssistantState(AssistantState.SPEAKING);
                    String cancelResponse = VoiceLanguageConfig.getMessageCancelledResponse(getCurrentLanguageCode());
                    if (isDeafUser(getCurrentUserRole())) {
                        DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                        if (deafResp != null) {
                            deafResp.showAssistantMessage(cancelResponse);
                        }
                    }
                    Log.i(TAG, "TTS_STARTED: " + cancelResponse);
                    speakResponse(cancelResponse, () -> {
                        Log.i(TAG, "TTS_COMPLETED: " + cancelResponse);
                        callback.onResponseSpoken(cancelResponse);
                        onAssistantFinished(activity);
                    });
                    return;
                }

                setAssistantState(AssistantState.SENDING_MESSAGE);
                dispatchCaregiverTextMessage(activity, rawMessage, () -> {
                    setAssistantState(AssistantState.SPEAKING);
                    String confirmation = VoiceLanguageConfig.getMessageSentResponse(getCurrentLanguageCode());
                    if (isDeafUser(getCurrentUserRole())) {
                        DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                        if (deafResp != null) {
                            deafResp.showSuccess(confirmation);
                        }
                    }
                    Log.i(TAG, "TTS_STARTED: " + confirmation);
                    speakResponse(confirmation, () -> {
                        Log.i(TAG, "TTS_COMPLETED: " + confirmation);
                        callback.onResponseSpoken(confirmation);
                        onAssistantFinished(activity);
                    });
                });
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(recognizerIntent);
    }

    private void dispatchCaregiverTextMessage(@NonNull Activity activity,
                                              @NonNull String messageText,
                                              @NonNull Runnable onComplete) {
        if (activity instanceof BlindUserDashboardActivity) {
            ((BlindUserDashboardActivity) activity).sendTextMessageToCaregiver(messageText, onComplete);
        } else if (activity instanceof SpeechImpairedDashboardActivity) {
            ((SpeechImpairedDashboardActivity) activity).sendTextMessageToCaregiver(messageText, onComplete);
        } else {
            sendCaregiverMessageDirectly(messageText, onComplete);
        }
    }

    private void sendCaregiverMessageDirectly(@NonNull String messageText, @NonNull Runnable onComplete) {
        String currentUid = sessionManager.getUserDetails().get(SessionManager.KEY_USER_UID);
        if (currentUid == null || currentUid.isEmpty()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) currentUid = user.getUid();
        }

        SharedPreferences prefs = appContext.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        String caregiverEmail = prefs.getString("caregiverEmail", "");
        String caregiverUid = null;

        if (caregiverEmail == null || caregiverEmail.trim().isEmpty()) {
            SharedPreferences speechPrefs = appContext.getSharedPreferences("SpeechSettings", Context.MODE_PRIVATE);
            caregiverEmail = speechPrefs.getString("caregiverEmail", "");
            caregiverUid = speechPrefs.getString("connectedCaregiverUid", null);
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final String finalUid = currentUid != null ? currentUid : "unknown_user";

        if (caregiverUid != null && !caregiverUid.trim().isEmpty() && !"mock_caregiver_uid".equals(caregiverUid)) {
            writeCaregiverMessageToFirestore(db, finalUid, caregiverUid, caregiverEmail != null ? caregiverEmail : "", messageText, onComplete);
        } else if (caregiverEmail != null && !caregiverEmail.trim().isEmpty()) {
            final String finalCaregiverEmail = caregiverEmail;
            db.collection("users")
                    .whereEqualTo("email", caregiverEmail.toLowerCase().trim())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        String cUid = !queryDocumentSnapshots.isEmpty() ? queryDocumentSnapshots.getDocuments().get(0).getId() : "mock_caregiver_uid";
                        writeCaregiverMessageToFirestore(db, finalUid, cUid, finalCaregiverEmail, messageText, onComplete);
                    })
                    .addOnFailureListener(e -> {
                        writeCaregiverMessageToFirestore(db, finalUid, "mock_caregiver_uid", finalCaregiverEmail, messageText, onComplete);
                    });
        } else {
            writeCaregiverMessageToFirestore(db, finalUid, "mock_caregiver_uid", "", messageText, onComplete);
        }
    }

    private void writeCaregiverMessageToFirestore(FirebaseFirestore db, String senderUid, String receiverUid, String recipientEmail, String messageText, Runnable onComplete) {
        String chatId = senderUid.compareTo(receiverUid) < 0 ? senderUid + "_" + receiverUid : receiverUid + "_" + senderUid;
        Map<String, Object> chatMsgMap = new HashMap<>();
        chatMsgMap.put("chatId", chatId);
        chatMsgMap.put("senderId", senderUid);
        chatMsgMap.put("senderUid", senderUid);
        chatMsgMap.put("receiverId", receiverUid);
        chatMsgMap.put("recipientEmail", recipientEmail != null ? recipientEmail.toLowerCase().trim() : "");
        
        String userRole = getCurrentUserRole();
        String resolvedSenderRole = RoleManager.ROLE_BLIND_USER;
        if (userRole != null && !userRole.trim().isEmpty()) {
            resolvedSenderRole = userRole.trim();
        } else {
            Activity active = (activeActivityRef != null) ? activeActivityRef.get() : null;
            if (active instanceof SpeechImpairedDashboardActivity) {
                resolvedSenderRole = RoleManager.ROLE_SPEECH_IMPAIRED;
            }
        }
        chatMsgMap.put("senderRole", resolvedSenderRole);
        chatMsgMap.put("receiverRole", RoleManager.ROLE_ADMIN_CAREGIVER);
        chatMsgMap.put("message", messageText);
        chatMsgMap.put("messageText", messageText);
        chatMsgMap.put("language", sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE);
        chatMsgMap.put("messageType", "text");
        chatMsgMap.put("type", "text");
        chatMsgMap.put("isVoice", false);
        chatMsgMap.put("status", "sent");
        chatMsgMap.put("readStatus", false);
        chatMsgMap.put("delivered", false);
        chatMsgMap.put("seen", false);
        chatMsgMap.put("timestamp", FieldValue.serverTimestamp());

        db.collection("caregiver_messages")
                .add(chatMsgMap)
                .addOnSuccessListener(ref -> {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        ref.update("status", "delivered", "delivered", true);
                    }, 1000);
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    LocalConnectionSimulator.saveLocalMessage(appContext, chatId, chatMsgMap);
                    if (onComplete != null) onComplete.run();
                });
    }

    public static boolean isCaregiverConnected(@Nullable Activity activity, @NonNull Context context) {
        if (activity instanceof BlindUserDashboardActivity) {
            return ((BlindUserDashboardActivity) activity).isCaregiverConnected();
        }
        if (activity instanceof SpeechImpairedDashboardActivity) {
            return ((SpeechImpairedDashboardActivity) activity).isCaregiverConnected();
        }
        SharedPreferences speechPrefs = context.getSharedPreferences("SpeechSettings", Context.MODE_PRIVATE);
        String deafCaregiverUid = speechPrefs.getString("connectedCaregiverUid", "");
        if (deafCaregiverUid != null && !deafCaregiverUid.trim().isEmpty()) {
            return true;
        }
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        String caregiverEmail = prefs.getString("caregiverEmail", "");
        return caregiverEmail != null && !caregiverEmail.trim().isEmpty();
    }

    public void handleVoiceRecordingFlow(@NonNull Activity activity,
                                         @Nullable String userRole,
                                         @NonNull VoiceAssistantCallback callback) {
        Log.i(TAG, "COMMAND_EXECUTED_ONCE: " + VoiceCommandConstants.CMD_RECORD_VOICE_MESSAGE);
        String langCode = getCurrentLanguageCode();

        // 1. Voice call active check
        if (isVoiceCallActive(activity)) {
            Log.i(TAG, "RECORD_VOICE_MESSAGE rejected: active call");
            setAssistantState(AssistantState.SPEAKING);
            String callResp = VoiceLanguageConfig.getCallActiveResponse(langCode);
            speakResponse(callResp, () -> {
                callback.onResponseSpoken(callResp);
                onAssistantFinished(activity);
            });
            return;
        }

        // 2. Caregiver connected check
        if (!isCaregiverConnected(activity, appContext)) {
            Log.i(TAG, "RECORD_VOICE_MESSAGE rejected: no caregiver connected");
            setAssistantState(AssistantState.SPEAKING);
            String noCaregiverResp = VoiceLanguageConfig.getNoCaregiverResponse(langCode);
            speakResponse(noCaregiverResp, () -> {
                callback.onResponseSpoken(noCaregiverResp);
                onAssistantFinished(activity);
            });
            return;
        }

        // 3. Audio permission check
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECORD_VOICE_MESSAGE rejected: RECORD_AUDIO permission not granted");
            setAssistantState(AssistantState.SPEAKING);
            String permResp = "Microphone permission is required to record a voice message.";
            speakResponse(permResp, () -> {
                callback.onError(permResp, ERROR_RECORD_AUDIO_PERMISSION);
                onAssistantFinished(activity);
            });
            return;
        }

        // 4. Prompt user to speak the voice message
        setAssistantState(AssistantState.SPEAKING);
        String prompt = VoiceLanguageConfig.getSpeakVoiceMessagePrompt(langCode);
        Log.i(TAG, "TTS_STARTED: " + prompt);
        speakRecordingPrompt(activity, prompt, () -> {
            Log.i(TAG, "TTS_COMPLETED: " + prompt);
            callback.onResponseSpoken(prompt);

            // 5. Safe delay (300ms) strictly after TTS engine's onDone() completes
            mainHandler.postDelayed(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) {
                    onAssistantFinished(activity);
                    return;
                }

                // 6. Only then start MediaRecorder
                setAssistantState(AssistantState.VOICE_MESSAGE_RECORDING);
                isHandsFreeVoiceRecordingActive = true;
                Log.i(TAG, "VOICE_MESSAGE_RECORDING_STARTED");

                if (activity instanceof BlindUserDashboardActivity) {
                    ((BlindUserDashboardActivity) activity).startHandsFreeRecording();
                } else {
                    startInternalVoiceRecording(activity);
                }

                // 7. Only then start the recording-control SpeechRecognizer
                startRecordingControlListening(activity, callback);
            }, 300);
        }, () -> {
            Log.e(TAG, "TTS recording prompt failed or encountered an error. Safely recovering without recording.");
            setAssistantState(AssistantState.IDLE);
            callback.onError("Failed to speak recording prompt.", ERROR_TTS_INITIALIZATION);
            onAssistantFinished(activity);
        });
    }

    private void startRecordingControlListening(@NonNull Activity activity,
                                                @NonNull VoiceAssistantCallback callback) {
        cleanupSpeechRecognizer();
        WakeWordManager.getInstance(appContext).pauseListening();

        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            Log.w(TAG, "SpeechRecognizer not available for recording control");
            return;
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext);
            Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            String langCode = getCurrentLanguageCode();
            String resolvedLocale = VoiceLanguageConfig.getSpeechRecognizerLanguageTag(langCode);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, resolvedLocale);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, resolvedLocale);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {}
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}
                @Override
                public void onError(int error) {
                    Log.d(TAG, "Recording control STT error: " + error);
                    // Re-arm speech recognition if recording is still active so stop command can be caught
                    if (isHandsFreeVoiceRecordingActive && currentState == AssistantState.VOICE_MESSAGE_RECORDING) {
                        rearmRecordingControlListening(activity, callback);
                    }
                }
                @Override
                public void onResults(Bundle results) {
                    if (results != null) {
                        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (matches != null && !matches.isEmpty() && matches.get(0) != null) {
                            String heard = matches.get(0).trim();
                            Log.i(TAG, "RECORDING_CONTROL_HEARD: " + heard);
                            handleRecordingResponseFlow(activity, heard, getCurrentUserRole(), getCurrentLanguageCode(), callback);
                        } else if (isHandsFreeVoiceRecordingActive && currentState == AssistantState.VOICE_MESSAGE_RECORDING) {
                            rearmRecordingControlListening(activity, callback);
                        }
                    } else if (isHandsFreeVoiceRecordingActive && currentState == AssistantState.VOICE_MESSAGE_RECORDING) {
                        rearmRecordingControlListening(activity, callback);
                    }
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
            speechRecognizer.startListening(recognizerIntent);
        } catch (Exception e) {
            Log.w(TAG, "Could not start recording control listening: " + e.getMessage());
        }
    }

    void rearmRecordingControlListening(@NonNull Activity activity,
                                        @NonNull VoiceAssistantCallback callback) {
        if (!isHandsFreeVoiceRecordingActive || currentState != AssistantState.VOICE_MESSAGE_RECORDING) {
            return;
        }
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        cleanupSpeechRecognizer();
        mainHandler.postDelayed(() -> {
            if (isHandsFreeVoiceRecordingActive && currentState == AssistantState.VOICE_MESSAGE_RECORDING
                    && !activity.isFinishing() && !activity.isDestroyed()) {
                startRecordingControlListening(activity, callback);
            }
        }, 300);
    }

    public void handleRecordingResponseFlow(@NonNull Activity activity,
                                            @NonNull String recognizedUtterance,
                                            @Nullable String userRole,
                                            @NonNull String langCode,
                                            @NonNull VoiceAssistantCallback callback) {
        // 1. Cancellation check during recording
        if (VoiceIntentMatcher.isRecordingCancelPhrase(recognizedUtterance)) {
            Log.i(TAG, "VOICE_RECORDING_CANCELLED");
            stopAndDiscardHandsFreeRecording(activity);
            setAssistantState(AssistantState.SPEAKING);
            String cancelResp = VoiceLanguageConfig.getVoiceMessageCancelledResponse(langCode);
            speakResponse(cancelResp, () -> {
                callback.onResponseSpoken(cancelResp);
                onAssistantFinished(activity);
            });
            return;
        }

        // 2. Stop recording check
        if (VoiceIntentMatcher.isRecordingStopPhrase(recognizedUtterance)) {
            Log.i(TAG, "VOICE_RECORDING_STOPPED_BY_COMMAND");
            File audioFile = stopHandsFreeRecordingInternal(activity);
            int duration = getHandsFreeRecordingDurationInternal(activity);
            if (audioFile == null || !audioFile.exists() || audioFile.length() <= 0) {
                Log.w(TAG, "Recorded audio file invalid or missing");
                setAssistantState(AssistantState.SPEAKING);
                String failResp = VoiceLanguageConfig.getVoiceMessageFailedResponse(langCode);
                speakResponse(failResp, () -> {
                    callback.onError(failResp, ERROR_COMMAND_PROCESSING);
                    onAssistantFinished(activity);
                });
                return;
            }

            Log.i(TAG, "VOICE_MESSAGE_DIRECT_SEND_STARTED");
            setAssistantState(AssistantState.SENDING_MESSAGE);
            if (activity instanceof BlindUserDashboardActivity) {
                ((BlindUserDashboardActivity) activity).sendHandsFreeVoiceMessage(audioFile, duration);
            }
            return;
        }

        // 3. User spoken message audio content (not a command)
        Log.i(TAG, "VOICE_RECORDING_AUDIO_CONTENT_IGNORED_AS_COMMAND: '" + recognizedUtterance + "'");
        if (isHandsFreeVoiceRecordingActive && currentState == AssistantState.VOICE_MESSAGE_RECORDING) {
            rearmRecordingControlListening(activity, callback);
        }
    }

    public void handleVoiceMessageConfirmationFlow(@NonNull Activity activity,
                                                   @NonNull String recognizedUtterance,
                                                   @Nullable String userRole,
                                                   @NonNull String langCode,
                                                   @NonNull VoiceAssistantCallback callback) {
        VoiceIntent intent = commandProcessor.detectIntent(recognizedUtterance, langCode);
        String s = VoiceIntentMatcher.normalizeText(recognizedUtterance);
        Set<String> words = new HashSet<>(Arrays.asList(s.split(" ")));

        // 1. Positive confirmation: YES, SEND, CONFIRM
        boolean isAffirmative = intent.getIntentType() == VoiceIntentType.CONFIRM_YES ||
                words.contains("yes") || words.contains("send") || words.contains("confirm") ||
                words.contains("proceed") || words.contains("okay") || words.contains("ok") ||
                s.equals("send it") || s.equals("yes send") || s.equals("yes send it") ||
                s.equals("yes please") ||
                s.equals("ಹೌದು") || s.equals("ಸರಿ") || s.equals("ಕಳುಹಿಸು") || s.equals("ಕಳುಹಿಸಿ") ||
                s.equals("ಕಳಿಸು") || s.equals("ಕಳಿಸಿ") || s.equals("ಖಂಡಿತ") ||
                s.equals("हाँ") || s.equals("हा") || s.equals("भेजो") || s.equals("भेजें") ||
                s.equals("ठीक है") || s.equals("ज़रूर") || s.equals("जरूर") ||
                s.equals("അതെ") || s.equals("ശരി") || s.equals("അയക്കൂ") || s.equals("അയക്കുക") ||
                s.equals("തീർച്ചയായും");

        if (isAffirmative) {
            Log.i(TAG, "VOICE_MESSAGE_SEND_CONFIRMED");
            File audioToSend = pendingRecordedAudioFile;
            int durationToSend = pendingRecordedAudioDuration;
            clearVoiceMessageConfirmation();

            setAssistantState(AssistantState.SENDING_MESSAGE);
            if (activity instanceof BlindUserDashboardActivity) {
                ((BlindUserDashboardActivity) activity).sendHandsFreeVoiceMessage(audioToSend, durationToSend);
            }
            setAssistantState(AssistantState.SPEAKING);
            String sentResp = VoiceLanguageConfig.getVoiceMessageSentResponse(langCode);
            speakResponse(sentResp, () -> {
                callback.onResponseSpoken(sentResp);
                onAssistantFinished(activity);
            });
            return;
        }

        // 2. Negative confirmation: NO, CANCEL, DON'T SEND
        boolean isNegative = intent.getIntentType() == VoiceIntentType.CONFIRM_NO ||
                words.contains("no") || words.contains("cancel") || s.contains("don t") ||
                s.contains("dont") || s.contains("do not") || s.equals("never mind") ||
                s.equals("stop") ||
                s.equals("ಬೇಡ") || s.equals("ಇಲ್ಲ") || s.equals("ರದ್ದು") || s.equals("ರದ್ದು ಮಾಡಿ") ||
                s.equals("नहीं") || s.equals("रद्द") || s.equals("मत भेजो") || s.equals("मत करो") ||
                s.equals("വേണ്ട") || s.equals("ഇല്ല") || s.equals("റദ്ദാക്കുക") || s.equals("അയക്കരുത്");

        if (isNegative) {
            Log.i(TAG, "VOICE_MESSAGE_SEND_CANCELLED");
            clearVoiceMessageConfirmation();
            stopAndDiscardHandsFreeRecording(activity);

            setAssistantState(AssistantState.SPEAKING);
            String cancelResp = VoiceLanguageConfig.getVoiceMessageCancelledResponse(langCode);
            speakResponse(cancelResp, () -> {
                callback.onResponseSpoken(cancelResp);
                onAssistantFinished(activity);
            });
            return;
        }

        // 3. Ambiguous/unrecognized response
        Log.i(TAG, "VOICE_MESSAGE_CONFIRMATION_UNRECOGNIZED: '" + recognizedUtterance + "'");
        clearVoiceMessageConfirmation();
        stopAndDiscardHandsFreeRecording(activity);
        setAssistantState(AssistantState.SPEAKING);
        String unrecResp = RoleManager.ROLE_BLIND_USER.equals(userRole)
                ? "Sorry, I did not understand."
                : VoiceCommandConstants.RESPONSE_UNKNOWN;
        speakResponse(unrecResp, () -> {
            callback.onError(unrecResp, ERROR_COMMAND_PROCESSING);
            onAssistantFinished(activity);
        });
    }

    private void startInternalVoiceRecording(@NonNull Activity activity) {
        try {
            if (internalMediaRecorder != null) {
                try { internalMediaRecorder.release(); } catch (Exception ignored) {}
                internalMediaRecorder = null;
            }
            File cacheDir = activity.getExternalCacheDir() != null ? activity.getExternalCacheDir() : activity.getCacheDir();
            pendingRecordedAudioFile = new File(cacheDir, "blind_voice_" + System.currentTimeMillis() + ".m4a");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                internalMediaRecorder = new MediaRecorder(activity);
            } else {
                internalMediaRecorder = new MediaRecorder();
            }
            internalMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            internalMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            internalMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            internalMediaRecorder.setOutputFile(pendingRecordedAudioFile.getAbsolutePath());
            internalMediaRecorder.prepare();
            internalMediaRecorder.start();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start internal MediaRecorder: " + e.getMessage(), e);
        }
    }

    private File stopHandsFreeRecordingInternal(@NonNull Activity activity) {
        cleanupSpeechRecognizer();
        isHandsFreeVoiceRecordingActive = false;
        if (activity instanceof BlindUserDashboardActivity) {
            return ((BlindUserDashboardActivity) activity).stopHandsFreeRecording();
        }
        if (internalMediaRecorder != null) {
            try {
                internalMediaRecorder.stop();
                internalMediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping internal MediaRecorder: " + e.getMessage(), e);
            }
            internalMediaRecorder = null;
        }
        return (pendingRecordedAudioFile != null && pendingRecordedAudioFile.exists() && pendingRecordedAudioFile.length() > 0)
                ? pendingRecordedAudioFile : null;
    }

    private int getHandsFreeRecordingDurationInternal(@NonNull Activity activity) {
        if (activity instanceof BlindUserDashboardActivity) {
            return ((BlindUserDashboardActivity) activity).getHandsFreeRecordingSeconds();
        }
        return pendingRecordedAudioDuration > 0 ? pendingRecordedAudioDuration : 3;
    }

    private void stopAndDiscardHandsFreeRecording(@NonNull Activity activity) {
        cleanupSpeechRecognizer();
        isHandsFreeVoiceRecordingActive = false;
        if (activity instanceof BlindUserDashboardActivity) {
            ((BlindUserDashboardActivity) activity).cancelHandsFreeRecording();
        }
        if (internalMediaRecorder != null) {
            try {
                internalMediaRecorder.stop();
                internalMediaRecorder.release();
            } catch (Exception ignored) {}
            internalMediaRecorder = null;
        }
        if (pendingRecordedAudioFile != null && pendingRecordedAudioFile.exists()) {
            pendingRecordedAudioFile.delete();
            pendingRecordedAudioFile = null;
        }
        pendingConfirmationType = ConfirmationType.NONE;
    }

    private void clearVoiceMessageConfirmation() {
        pendingConfirmationType = ConfirmationType.NONE;
    }

    public void simulateRecordingInput(@NonNull Activity activity,
                                       @NonNull String utterance,
                                       @NonNull VoiceAssistantCallback callback) {
        handleRecordingResponseFlow(activity, utterance, getCurrentUserRole(), getCurrentLanguageCode(), callback);
    }

    public void simulateConfirmationInput(@NonNull Activity activity,
                                          @NonNull String utterance,
                                          @NonNull VoiceAssistantCallback callback) {
        handleVoiceMessageConfirmationFlow(activity, utterance, getCurrentUserRole(), getCurrentLanguageCode(), callback);
    }

    private void handleReadMessagesFlow(@NonNull Activity activity,
                                        @Nullable String userRole,
                                        @NonNull VoiceAssistantCallback callback) {
        Log.i(TAG, "COMMAND_EXECUTED_ONCE: " + VoiceCommandConstants.CMD_READ_MESSAGES);

        // 1. Voice call active check
        if (isVoiceCallActive(activity)) {
            Log.i(TAG, "READ_MESSAGES rejected: active call");
            setAssistantState(AssistantState.SPEAKING);
            String callResp = VoiceLanguageConfig.getCallActiveResponse(getCurrentLanguageCode());
            if (isDeafUser(userRole)) {
                DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                if (deafResp != null) {
                    deafResp.showMicConflict(callResp);
                }
            }
            Log.i(TAG, "TTS_STARTED: " + callResp);
            speakResponse(callResp, () -> {
                Log.i(TAG, "TTS_COMPLETED: " + callResp);
                callback.onResponseSpoken(callResp);
                onAssistantFinished(activity);
            });
            return;
        }

        // 2. Set state to READING_MESSAGE and pause wake-word
        setAssistantState(AssistantState.READING_MESSAGE);
        cleanupSpeechRecognizer();
        WakeWordManager.getInstance(appContext).pauseListening();

        com.kannada.speechassistant.CaregiverChatAdapter.stopAudioPlayback();
        stopVoicePlayer();

        // 3. Try to get message from BlindUserDashboardActivity directly
        ChatMessage latestMsg = null;
        if (activity instanceof BlindUserDashboardActivity) {
            latestMsg = ((BlindUserDashboardActivity) activity).getLatestCaregiverMessage();
        }

        if (latestMsg != null) {
            deliverCaregiverMessage(activity, latestMsg, callback);
        } else {
            fetchLatestCaregiverTextMessageFallback(activity, caregiverMsg -> {
                if (caregiverMsg != null) {
                    deliverCaregiverMessage(activity, caregiverMsg, callback);
                } else {
                    speakNoNewMessages(activity, callback);
                }
            });
        }
    }

    private void deliverCaregiverMessage(@NonNull Activity activity,
                                         @NonNull ChatMessage msg,
                                         @NonNull VoiceAssistantCallback callback) {
        boolean hasAudio = (msg.isVoice() || (msg.getAudioUrl() != null && !msg.getAudioUrl().trim().isEmpty()))
                && msg.getAudioUrl() != null && !msg.getAudioUrl().trim().isEmpty();

        if (hasAudio) {
            playCaregiverVoiceMessage(activity, msg, callback);
        } else {
            speakCaregiverMessage(activity, msg, callback);
        }
    }

    private void playCaregiverVoiceMessage(@NonNull Activity activity,
                                           @NonNull ChatMessage msg,
                                           @NonNull VoiceAssistantCallback callback) {
        final String audioUrl = msg.getAudioUrl();
        if (audioUrl == null || audioUrl.trim().isEmpty()) {
            speakNoNewMessages(activity, callback);
            return;
        }

        this.lastReadCaregiverMessage = msg;

        String introText = VoiceLanguageConfig.getCaregiverVoiceMessageIntro(getCurrentLanguageCode());
        if (isDeafUser(getCurrentUserRole())) {
            DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
            if (deafResp != null) {
                deafResp.showSuccess(introText);
            }
        }
        setAssistantState(AssistantState.SPEAKING);
        Log.i(TAG, "TTS_STARTED: " + introText);

        speakResponse(introText, () -> {
            Log.i(TAG, "TTS_COMPLETED: " + introText);
            activity.runOnUiThread(() -> {
                playAudioUrl(activity, audioUrl.trim(), new VoicePlaybackListener() {
                    @Override
                    public void onPlaybackStarted() {
                        Log.i(TAG, "VOICE_MESSAGE_PLAYING: " + audioUrl);
                        setAssistantState(AssistantState.READING_MESSAGE);
                    }

                    @Override
                    public void onPlaybackCompleted() {
                        Log.i(TAG, "VOICE_MESSAGE_PLAYBACK_FINISHED");
                        callback.onResponseSpoken(introText);
                        onAssistantFinished(activity);
                    }

                    @Override
                    public void onPlaybackError(String errorMsg) {
                        Log.w(TAG, "VOICE_MESSAGE_PLAYBACK_ERROR: " + errorMsg);
                        callback.onError(errorMsg, ERROR_COMMAND_PROCESSING);
                        onAssistantFinished(activity);
                    }
                });
            });
        });
    }

    private interface VoicePlaybackListener {
        void onPlaybackStarted();
        void onPlaybackCompleted();
        void onPlaybackError(String errorMsg);
    }

    private synchronized void playAudioUrl(@NonNull Activity activity,
                                           @NonNull String audioUrl,
                                           @NonNull VoicePlaybackListener listener) {
        stopVoicePlayer();

        if (activity.isFinishing() || activity.isDestroyed()) {
            listener.onPlaybackError("Activity destroyed");
            return;
        }

        try {
            try {
                AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
                if (am != null) {
                    int maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    int currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                    if (currentVol < (int) (maxVol * 0.7f)) {
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (maxVol * 0.85f), 0);
                    }
                }
            } catch (Exception ignored) {}

            activeVoicePlayer = new MediaPlayer();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activeVoicePlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                );
            } else {
                activeVoicePlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }

            activeVoicePlayer.setDataSource(activity.getApplicationContext(), Uri.parse(audioUrl));
            activeVoicePlayer.setOnPreparedListener(mp -> {
                try {
                    mp.start();
                    listener.onPlaybackStarted();
                } catch (Exception e) {
                    Log.e(TAG, "Error starting voice playback", e);
                    stopVoicePlayer();
                    listener.onPlaybackError("Failed to start audio playback");
                }
            });

            activeVoicePlayer.setOnCompletionListener(mp -> {
                stopVoicePlayer();
                listener.onPlaybackCompleted();
            });

            activeVoicePlayer.setOnErrorListener((mp, what, extra) -> {
                Log.w(TAG, "MediaPlayer error during voice playback: what=" + what + ", extra=" + extra);
                stopVoicePlayer();
                listener.onPlaybackError("MediaPlayer error: " + what);
                return true;
            });

            activeVoicePlayer.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize MediaPlayer for URL: " + audioUrl, e);
            stopVoicePlayer();
            listener.onPlaybackError("Failed to play audio: " + e.getMessage());
        }
    }

    private void speakCaregiverMessage(@NonNull Activity activity,
                                       @NonNull ChatMessage msg,
                                       @NonNull VoiceAssistantCallback callback) {
        String rawContent = msg.getMessageText();
        if (rawContent == null || rawContent.trim().isEmpty()) {
            rawContent = msg.getMessage();
        }
        if (rawContent == null || rawContent.trim().isEmpty()) {
            speakNoNewMessages(activity, callback);
            return;
        }

        this.lastReadCaregiverMessage = msg;

        String prefix = VoiceLanguageConfig.getCaregiverSaysPrefix(getCurrentLanguageCode());
        String finalSpokenText = prefix + rawContent.trim();
        if (isDeafUser(getCurrentUserRole())) {
            DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
            if (deafResp != null) {
                deafResp.showSuccess(finalSpokenText);
            }
        }
        setAssistantState(AssistantState.SPEAKING);
        Log.i(TAG, "TTS_STARTED: " + finalSpokenText);

        speakResponseWithLanguage(finalSpokenText, msg.getLanguage(), () -> {
            Log.i(TAG, "TTS_COMPLETED: " + finalSpokenText);
            callback.onResponseSpoken(finalSpokenText);
            onAssistantFinished(activity);
        });
    }

    private void speakNoNewMessages(@NonNull Activity activity,
                                    @NonNull VoiceAssistantCallback callback) {
        setAssistantState(AssistantState.SPEAKING);
        String noMsgText = VoiceLanguageConfig.getNoNewMessagesResponse(getCurrentLanguageCode());
        if (isDeafUser(getCurrentUserRole())) {
            DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
            if (deafResp != null) {
                deafResp.showSuccess(noMsgText);
            }
        }
        Log.i(TAG, "TTS_STARTED: " + noMsgText);

        speakResponse(noMsgText, () -> {
            Log.i(TAG, "TTS_COMPLETED: " + noMsgText);
            callback.onResponseSpoken(noMsgText);
            onAssistantFinished(activity);
        });
    }

    private interface MessageResultCallback {
        void onResult(@Nullable ChatMessage message);
    }

    private void fetchLatestCaregiverTextMessageFallback(@NonNull Activity activity,
                                                          @NonNull MessageResultCallback onResult) {
        String currentUid = sessionManager.getUserDetails().get(SessionManager.KEY_USER_UID);
        if (currentUid == null || currentUid.isEmpty()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) currentUid = user.getUid();
        }
        if (currentUid == null || currentUid.isEmpty()) {
            onResult.onResult(null);
            return;
        }

        final String myUid = currentUid;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("caregiver_messages")
                .whereEqualTo("receiverId", myUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        onResult.onResult(null);
                        return;
                    }
                    List<ChatMessage> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        list.add(new ChatMessage(doc));
                    }
                    Collections.sort(list, (m1, m2) -> {
                        if (m1.getTimestamp() == null && m2.getTimestamp() == null) return 0;
                        if (m1.getTimestamp() == null) return 1;
                        if (m2.getTimestamp() == null) return -1;
                        return m1.getTimestamp().compareTo(m2.getTimestamp());
                    });

                    // 1. Unread first (text or voice)
                    for (int i = list.size() - 1; i >= 0; i--) {
                        ChatMessage m = list.get(i);
                        if (myUid.equals(m.getSenderId())) continue;
                        boolean isVoice = (m.isVoice() || (m.getAudioUrl() != null && !m.getAudioUrl().trim().isEmpty()))
                                && m.getAudioUrl() != null && !m.getAudioUrl().trim().isEmpty();
                        String text = m.getMessageText();
                        if (text == null || text.trim().isEmpty()) text = m.getMessage();
                        boolean hasText = text != null && !text.trim().isEmpty();

                        if (isVoice || hasText) {
                            if (!m.isSeen() && !"read".equalsIgnoreCase(m.getStatus())) {
                                onResult.onResult(m);
                                return;
                            }
                        }
                    }

                    // 2. Latest chronological (text or voice)
                    for (int i = list.size() - 1; i >= 0; i--) {
                        ChatMessage m = list.get(i);
                        if (myUid.equals(m.getSenderId())) continue;
                        boolean isVoice = (m.isVoice() || (m.getAudioUrl() != null && !m.getAudioUrl().trim().isEmpty()))
                                && m.getAudioUrl() != null && !m.getAudioUrl().trim().isEmpty();
                        String text = m.getMessageText();
                        if (text == null || text.trim().isEmpty()) text = m.getMessage();
                        boolean hasText = text != null && !text.trim().isEmpty();

                        if (isVoice || hasText) {
                            onResult.onResult(m);
                            return;
                        }
                    }

                    onResult.onResult(null);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Fallback caregiver message query failed: " + e.getMessage());
                    onResult.onResult(null);
                });
    }

    private void handleMessageCountFlow(@NonNull Activity activity,
                                        @Nullable String userRole,
                                        @NonNull VoiceAssistantCallback callback) {
        Log.i(TAG, "COMMAND_EXECUTED_ONCE: " + VoiceCommandConstants.CMD_MESSAGE_COUNT);

        // 1. Voice call active check
        if (isVoiceCallActive(activity)) {
            Log.i(TAG, "MESSAGE_COUNT rejected: active call");
            setAssistantState(AssistantState.SPEAKING);
            String callResp = VoiceLanguageConfig.getCallActiveResponse(getCurrentLanguageCode());
            if (isDeafUser(userRole)) {
                DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                if (deafResp != null) {
                    deafResp.showMicConflict(callResp);
                }
            }
            Log.i(TAG, "TTS_STARTED: " + callResp);
            speakResponse(callResp, () -> {
                Log.i(TAG, "TTS_COMPLETED: " + callResp);
                callback.onResponseSpoken(callResp);
                onAssistantFinished(activity);
            });
            return;
        }

        // 2. Set state to MESSAGE_COUNTING and pause wake-word
        setAssistantState(AssistantState.MESSAGE_COUNTING);
        cleanupSpeechRecognizer();
        WakeWordManager.getInstance(appContext).pauseListening();

        // 3. Try to get message count from BlindUserDashboardActivity directly
        int count = -1;
        if (activity instanceof BlindUserDashboardActivity) {
            count = ((BlindUserDashboardActivity) activity).getCaregiverTextMessageCount();
        }

        if (count >= 0) {
            speakMessageCount(activity, count, callback);
        } else {
            fetchCaregiverTextMessageCountFallback(activity, fallbackCount -> {
                speakMessageCount(activity, fallbackCount, callback);
            });
        }
    }

    private void speakMessageCount(@NonNull Activity activity,
                                   int count,
                                   @NonNull VoiceAssistantCallback callback) {
        String responseText = VoiceLanguageConfig.getMessageCountResponse(count, getCurrentLanguageCode());
        if (isDeafUser(getCurrentUserRole())) {
            DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
            if (deafResp != null) {
                deafResp.showSuccess(responseText);
            }
        }
        setAssistantState(AssistantState.SPEAKING);
        Log.i(TAG, "TTS_STARTED: " + responseText);

        speakResponse(responseText, () -> {
            Log.i(TAG, "TTS_COMPLETED: " + responseText);
            callback.onResponseSpoken(responseText);
            onAssistantFinished(activity);
        });
    }

    private void fetchCaregiverTextMessageCountFallback(@NonNull Activity activity,
                                                        @NonNull MessageCountCallback onResult) {
        String currentUid = sessionManager.getUserDetails().get(SessionManager.KEY_USER_UID);
        if (currentUid == null || currentUid.isEmpty()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) currentUid = user.getUid();
        }
        if (currentUid == null || currentUid.isEmpty()) {
            onResult.onCount(0);
            return;
        }

        final String myUid = currentUid;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("caregiver_messages")
                .whereEqualTo("receiverId", myUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        onResult.onCount(0);
                        return;
                    }
                    int count = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        ChatMessage m = new ChatMessage(doc);
                        if (BlindUserDashboardActivity.isCaregiverTextMessage(m, myUid)) {
                            count++;
                        }
                    }
                    onResult.onCount(count);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Fallback caregiver message count query failed: " + e.getMessage());
                    onResult.onCount(0);
                });
    }

    private interface MessageCountCallback {
        void onCount(int count);
    }

    private void handleRepeatMessageFlow(@NonNull Activity activity,
                                         @Nullable String userRole,
                                         @NonNull VoiceAssistantCallback callback) {
        Log.i(TAG, "COMMAND_EXECUTED_ONCE: " + VoiceCommandConstants.CMD_REPEAT_MESSAGE);

        // 1. Voice call active check
        if (isVoiceCallActive(activity)) {
            Log.i(TAG, "REPEAT_MESSAGE rejected: active call");
            setAssistantState(AssistantState.SPEAKING);
            String callResp = VoiceLanguageConfig.getCallActiveResponse(getCurrentLanguageCode());
            if (isDeafUser(userRole)) {
                DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                if (deafResp != null) {
                    deafResp.showMicConflict(callResp);
                }
            }
            Log.i(TAG, "TTS_STARTED: " + callResp);
            speakResponse(callResp, () -> {
                Log.i(TAG, "TTS_COMPLETED: " + callResp);
                callback.onResponseSpoken(callResp);
                onAssistantFinished(activity);
            });
            return;
        }

        // 2. Set state to REPEATING_MESSAGE and pause wake-word
        setAssistantState(AssistantState.REPEATING_MESSAGE);
        cleanupSpeechRecognizer();
        WakeWordManager.getInstance(appContext).pauseListening();

        // 3. Repeat last read caregiver message if available
        if (lastReadCaregiverMessage != null) {
            deliverCaregiverMessage(activity, lastReadCaregiverMessage, callback);
        } else {
            speakNoMessageToRepeat(activity, callback);
        }
    }

    private void speakNoMessageToRepeat(@NonNull Activity activity,
                                        @NonNull VoiceAssistantCallback callback) {
        setAssistantState(AssistantState.SPEAKING);
        String noMsgText = VoiceLanguageConfig.getNoMessageToRepeatResponse(getCurrentLanguageCode());
        if (isDeafUser(getCurrentUserRole())) {
            DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
            if (deafResp != null) {
                deafResp.showSuccess(noMsgText);
            }
        }
        Log.i(TAG, "TTS_STARTED: " + noMsgText);

        speakResponse(noMsgText, () -> {
            Log.i(TAG, "TTS_COMPLETED: " + noMsgText);
            callback.onResponseSpoken(noMsgText);
            onAssistantFinished(activity);
        });
    }

    private void handleReadNotificationsFlow(@NonNull Activity activity,
                                             @Nullable String userRole,
                                             @NonNull VoiceAssistantCallback callback) {
        Log.i(TAG, "COMMAND_EXECUTED_ONCE: " + VoiceCommandConstants.CMD_READ_NOTIFICATIONS);

        // 1. Voice call active check
        if (isVoiceCallActive(activity)) {
            Log.i(TAG, "READ_NOTIFICATIONS rejected: active call");
            setAssistantState(AssistantState.SPEAKING);
            String callResp = VoiceLanguageConfig.getCallActiveResponse(getCurrentLanguageCode());
            if (isDeafUser(userRole)) {
                DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                if (deafResp != null) {
                    deafResp.showMicConflict(callResp);
                }
            }
            Log.i(TAG, "TTS_STARTED: " + callResp);
            speakResponse(callResp, () -> {
                Log.i(TAG, "TTS_COMPLETED: " + callResp);
                callback.onResponseSpoken(callResp);
                onAssistantFinished(activity);
            });
            return;
        }

        // 2. Set state to READING_NOTIFICATIONS and pause wake-word
        setAssistantState(AssistantState.READING_NOTIFICATIONS);
        cleanupSpeechRecognizer();
        WakeWordManager.getInstance(appContext).pauseListening();

        // 3. Query existing notifications (max 3, newest first)
        List<NotificationService.NotificationItem> notifications =
                NotificationService.getReadableNotifications(activity, 3);

        if (notifications.isEmpty()) {
            speakNoNotifications(activity, callback);
            return;
        }

        String finalSpokenText;
        String langCode = getCurrentLanguageCode();

        if (notifications.size() == 1) {
            String intro = VoiceLanguageConfig.getSingleNotificationIntro(langCode);
            finalSpokenText = intro + notifications.get(0).getSpokenText();
        } else {
            String intro = VoiceLanguageConfig.getMultipleNotificationsIntro(notifications.size(), langCode);
            StringBuilder sb = new StringBuilder(intro);
            for (NotificationService.NotificationItem item : notifications) {
                sb.append(" ").append(item.getSpokenText());
                if (!item.getSpokenText().endsWith(".")) {
                    sb.append(".");
                }
            }
            finalSpokenText = sb.toString().trim();
        }

        if (isDeafUser(userRole)) {
            DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
            if (deafResp != null) {
                deafResp.showSuccess(finalSpokenText);
            }
        }

        setAssistantState(AssistantState.SPEAKING);
        Log.i(TAG, "TTS_STARTED: " + finalSpokenText);

        speakResponse(finalSpokenText, () -> {
            Log.i(TAG, "TTS_COMPLETED: " + finalSpokenText);
            callback.onResponseSpoken(finalSpokenText);
            onAssistantFinished(activity);
        });
    }

    private void speakNoNotifications(@NonNull Activity activity,
                                      @NonNull VoiceAssistantCallback callback) {
        setAssistantState(AssistantState.SPEAKING);
        String noNotifText = VoiceLanguageConfig.getNoNotificationsResponse(getCurrentLanguageCode());
        if (isDeafUser(getCurrentUserRole())) {
            DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
            if (deafResp != null) {
                deafResp.showSuccess(noNotifText);
            }
        }
        Log.i(TAG, "TTS_STARTED: " + noNotifText);

        speakResponse(noNotifText, () -> {
            Log.i(TAG, "TTS_COMPLETED: " + noNotifText);
            callback.onResponseSpoken(noNotifText);
            onAssistantFinished(activity);
        });
    }

    private void handleGoBackFlow(@NonNull Activity activity,
                                  @Nullable String userRole,
                                  @NonNull VoiceAssistantCallback callback) {
        Log.i(TAG, "COMMAND_EXECUTED_ONCE: " + VoiceCommandConstants.CMD_GO_BACK);

        // 1. Voice call active check
        if (isVoiceCallActive(activity)) {
            Log.i(TAG, "GO_BACK rejected: active call");
            setAssistantState(AssistantState.SPEAKING);
            String callResp = VoiceCommandConstants.RESPONSE_CALL_ACTIVE;
            if (isDeafUser(userRole)) {
                DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
                if (deafResp != null) {
                    deafResp.showMicConflict(callResp);
                }
            }
            Log.i(TAG, "TTS_STARTED: " + callResp);
            speakResponse(callResp, () -> {
                Log.i(TAG, "TTS_COMPLETED: " + callResp);
                callback.onResponseSpoken(callResp);
                onAssistantFinished(activity);
            });
            return;
        }

        // 2. Set state to GOING_BACK and pause wake-word
        setAssistantState(AssistantState.GOING_BACK);
        cleanupSpeechRecognizer();
        WakeWordManager.getInstance(appContext).pauseListening();

        Activity targetActivity = activity;
        try {
            Activity fg = WakeWordManager.getInstance(appContext).getCurrentActivity();
            if (fg != null && !fg.isFinishing() && !fg.isDestroyed()) {
                targetActivity = fg;
            }
        } catch (Throwable ignored) {}

        // 3. Check if back navigation is possible or if we are at root/first screen
        boolean canBack = VoiceActionDispatcher.canNavigateBack(targetActivity);
        final String responseText;
        final Activity finalTarget = targetActivity;
        if (canBack) {
            responseText = VoiceLanguageConfig.getLocalizedResponse(
                    VoiceCommandConstants.CMD_GO_BACK,
                    VoiceCommandConstants.RESPONSE_GOING_BACK,
                    getCurrentLanguageCode());
            finalTarget.runOnUiThread(() -> {
                VoiceActionDispatcher.handleGoBack(finalTarget);
            });
        } else {
            responseText = VoiceLanguageConfig.getLocalizedResponse(
                    VoiceCommandConstants.CMD_GO_BACK,
                    VoiceCommandConstants.RESPONSE_ALREADY_FIRST_SCREEN,
                    getCurrentLanguageCode());
        }

        if (isDeafUser(userRole)) {
            DeafAssistantResponseManager deafResp = getDeafAssistantResponseManager();
            if (deafResp != null) {
                deafResp.showSuccess(responseText);
            }
        }

        setAssistantState(AssistantState.SPEAKING);
        Log.i(TAG, "TTS_STARTED: " + responseText);
        speakResponse(responseText, () -> {
            Log.i(TAG, "TTS_COMPLETED: " + responseText);
            callback.onResponseSpoken(responseText);
            onAssistantFinished(finalTarget);
        });
    }

    /**
     * Controlled completion pathway for Voice Assistant sessions.
     * Transitions state from SPEAKING -> IDLE after an audio clearance settling delay of 600ms,
     * ensuring device speaker audio output and acoustic reverberation have fully dissipated
     * before resuming WakeWordManager listening.
     */
    private void onAssistantFinished(@Nullable Activity activity) {
        cleanupSpeechRecognizer();
        isHandsFreeVoiceRecordingActive = false;
        clearVoiceMessageConfirmation();
        stopVoicePlayer();

        mainHandler.postDelayed(() -> {
            setAssistantState(AssistantState.IDLE);
            Log.i(TAG, "RETURNING_TO_IDLE");
            commandHandled.set(false);
            Activity targetActivity = activity;
            Activity fg = WakeWordManager.getInstance(appContext).getCurrentActivity();
            if (fg != null && !fg.isFinishing() && !fg.isDestroyed()) {
                targetActivity = fg;
            } else if (targetActivity == null || targetActivity.isFinishing() || targetActivity.isDestroyed()) {
                targetActivity = fg;
            }
            if (targetActivity != null && !targetActivity.isFinishing() && !targetActivity.isDestroyed()
                    && (targetActivity instanceof BlindUserDashboardActivity
                    || targetActivity instanceof SpeechImpairedDashboardActivity
                    || targetActivity instanceof com.kannada.speechassistant.ChatActivity)) {
                WakeWordManager.getInstance(appContext).resumeListening(targetActivity);
            }
        }, 600);
    }

    /**
     * Called when hands-free voice recording workflow completely finishes
     * (manual send, cancel, error, or automatic timeout).
     * Cleans up all microphone holders, resets state machine to IDLE, and safely resumes WakeWordManager.
     */
    public void onVoiceRecordingWorkflowFinished(@Nullable Activity activity) {
        cleanupSpeechRecognizer();
        isHandsFreeVoiceRecordingActive = false;
        clearVoiceMessageConfirmation();
        if (internalMediaRecorder != null) {
            try {
                internalMediaRecorder.stop();
                internalMediaRecorder.release();
            } catch (Exception ignored) {}
            internalMediaRecorder = null;
        }

        mainHandler.postDelayed(() -> {
            setAssistantState(AssistantState.IDLE);
            Log.i(TAG, "VOICE_RECORDING_WORKFLOW_FINISHED: returning to IDLE and resuming WakeWordManager");
            commandHandled.set(false);
            Activity targetActivity = activity;
            if (targetActivity == null || targetActivity.isFinishing() || targetActivity.isDestroyed()) {
                targetActivity = WakeWordManager.getInstance(appContext).getCurrentActivity();
            }
            if (targetActivity != null && !targetActivity.isFinishing() && !targetActivity.isDestroyed()
                    && (targetActivity instanceof BlindUserDashboardActivity
                    || targetActivity instanceof SpeechImpairedDashboardActivity
                    || targetActivity instanceof com.kannada.speechassistant.ChatActivity)) {
                WakeWordManager.getInstance(appContext).resumeListening(targetActivity);
            }
        }, 600);
    }

    @NonNull
    public static String getConfirmationPrompt(@NonNull String commandId) {
        switch (commandId) {
            case VoiceCommandConstants.CMD_OPEN_VOICE_CALL:
                return VoiceCommandConstants.PROMPT_CONFIRM_VOICE_CALL;
            case VoiceCommandConstants.CMD_OPEN_EMERGENCY:
            case VoiceCommandConstants.CMD_OPEN_EMERGENCY_ALERT:
                return VoiceCommandConstants.PROMPT_CONFIRM_EMERGENCY_ALERT;
            default:
                return "Are you sure you want to proceed?";
        }
    }

    @NonNull
    public static String getConfirmedExecutionResponse(@NonNull String commandId, @Nullable String userRole) {
        switch (commandId) {
            case VoiceCommandConstants.CMD_OPEN_VOICE_CALL:
                return VoiceCommandConstants.RESPONSE_CALL_CONFIRMED;
            case VoiceCommandConstants.CMD_OPEN_EMERGENCY:
            case VoiceCommandConstants.CMD_OPEN_EMERGENCY_ALERT:
                return RoleManager.ROLE_BLIND_USER.equals(userRole)
                        ? "Opening emergency."
                        : VoiceCommandConstants.RESPONSE_OPEN_EMERGENCY;
            default:
                return "Proceeding.";
        }
    }

    public boolean isAwaitingConfirmation() {
        return pendingConfirmationCommand != null;
    }

    public void clearPendingConfirmation() {
        pendingConfirmationCommand = null;
        pendingConfirmationActivityRef = null;
        pendingConfirmationCallback = null;
    }

    @NonNull
    public static String getClarifyPrompt(@NonNull ClarificationContext context, @Nullable String langCode) {
        switch (context) {
            case HELP:
                return VoiceLanguageConfig.getClarifyHelpPrompt(langCode);
            case CALL:
                return VoiceLanguageConfig.getClarifyCallPrompt(langCode);
            case MESSAGE:
                return VoiceLanguageConfig.getClarifyMessagePrompt(langCode);
            default:
                return VoiceLanguageConfig.getClarifyHelpPrompt(langCode);
        }
    }

    public boolean isAwaitingClarification() {
        return pendingClarificationContext != ClarificationContext.NONE;
    }

    public void clearPendingClarification() {
        pendingClarificationContext = ClarificationContext.NONE;
        pendingClarificationActivityRef = null;
        pendingClarificationCallback = null;
    }

    public ClarificationContext getPendingClarificationContext() {
        return pendingClarificationContext;
    }

    public void setPendingClarificationContextForTesting(@NonNull ClarificationContext context) {
        this.pendingClarificationContext = context;
        if (context != ClarificationContext.NONE) {
            setAssistantState(AssistantState.WAITING_FOR_CLARIFICATION);
        } else {
            setAssistantState(AssistantState.IDLE);
        }
    }

    public void setPendingConfirmationCommandForTesting(@Nullable VoiceCommand command) {
        this.pendingConfirmationCommand = command;
        if (command != null) {
            setAssistantState(AssistantState.WAITING_FOR_CONFIRMATION);
        } else {
            setAssistantState(AssistantState.IDLE);
        }
    }

    @NonNull
    public static String getBlindUserSpokenResponse(@NonNull String commandId, @NonNull String defaultResponse) {
        return getBlindUserSpokenResponse(commandId, defaultResponse, VoiceLanguageConfig.LANG_ENGLISH);
    }

    @NonNull
    public static String getBlindUserSpokenResponse(@NonNull String commandId,
                                                    @NonNull String defaultResponse,
                                                    @Nullable String langCode) {
        String baseResponse = defaultResponse;
        if ("A call is already active.".equals(defaultResponse) ||
                "A call is active.".equals(defaultResponse) ||
                "No caregiver is connected.".equals(defaultResponse) ||
                "No new messages.".equals(defaultResponse) ||
                VoiceCommandConstants.RESPONSE_NO_MESSAGE_TO_REPEAT.equals(defaultResponse) ||
                VoiceCommandConstants.RESPONSE_GOING_BACK.equals(defaultResponse) ||
                VoiceCommandConstants.RESPONSE_ALREADY_FIRST_SCREEN.equals(defaultResponse) ||
                defaultResponse.startsWith(VoiceCommandConstants.PREFIX_CAREGIVER_SAYS) ||
                defaultResponse.startsWith("You have ") ||
                "I didn't hear a message.".equals(defaultResponse) ||
                "Please say your message.".equals(defaultResponse) ||
                "Message sent.".equals(defaultResponse) ||
                VoiceCommandConstants.RESPONSE_MESSAGE_CANCELLED.equals(defaultResponse) ||
                VoiceCommandConstants.RESPONSE_ACTION_CANCELLED.equals(defaultResponse) ||
                "No active call.".equals(defaultResponse)) {
            baseResponse = defaultResponse;
        } else {
            switch (commandId) {
                case VoiceCommandConstants.CMD_READ_MESSAGES:
                case VoiceCommandConstants.CMD_MESSAGE_COUNT:
                case VoiceCommandConstants.CMD_REPEAT_MESSAGE:
                case VoiceCommandConstants.CMD_GO_BACK:
                    baseResponse = defaultResponse;
                    break;
                case VoiceCommandConstants.CMD_OPEN_MESSAGES:
                case VoiceCommandConstants.CMD_OPEN_COMMUNICATION:
                    baseResponse = "Messages is open.";
                    break;
                case VoiceCommandConstants.CMD_SEND_MESSAGE:
                    baseResponse = "Message sent.";
                    break;
                case VoiceCommandConstants.CMD_OPEN_VOICE_CALL:
                    baseResponse = "Calling caregiver.";
                    break;
                case VoiceCommandConstants.CMD_OPEN_VOICE_RECORDER:
                case VoiceCommandConstants.CMD_OPEN_VOICE_MESSAGE:
                case VoiceCommandConstants.CMD_RECORD_VOICE_MESSAGE:
                    baseResponse = "Opening voice recorder.";
                    break;
                case VoiceCommandConstants.CMD_OPEN_HOME:
                    baseResponse = "Home is open.";
                    break;
                case VoiceCommandConstants.CMD_OPEN_PROFILE:
                    baseResponse = "Profile is open.";
                    break;
                case VoiceCommandConstants.CMD_OPEN_SETTINGS:
                    baseResponse = "Settings is open.";
                    break;
                case VoiceCommandConstants.CMD_OPEN_CAREGIVER:
                case VoiceCommandConstants.CMD_OPEN_CAREGIVER_CONNECTION:
                    baseResponse = "Opening caregiver.";
                    break;
                case VoiceCommandConstants.CMD_OPEN_EMERGENCY:
                case VoiceCommandConstants.CMD_OPEN_EMERGENCY_ALERT:
                    baseResponse = "Emergency alert sent.";
                    break;
                case VoiceCommandConstants.CMD_END_CALL:
                    baseResponse = "Call ended.";
                    break;
                case VoiceCommandConstants.CMD_ACCEPT_CALL:
                    baseResponse = "Receiving call.";
                    break;
                case VoiceCommandConstants.CMD_START_LISTENING:
                    baseResponse = "Listening.";
                    break;
                case VoiceCommandConstants.CMD_STOP_LISTENING:
                    baseResponse = "Listening stopped.";
                    break;
                default:
                    baseResponse = defaultResponse;
                    break;
            }
        }
        return VoiceLanguageConfig.getLocalizedResponse(commandId, baseResponse, langCode);
    }

    public void stopListening() {
        if (speechRecognizer != null && isListening) {
            try {
                speechRecognizer.stopListening();
            } catch (Exception ignored) {}
        }
        isListening = false;
    }

    public boolean isListening() {
        return isListening;
    }

    /**
     * Safe microphone-state check.
     * Returns true if the microphone is currently in use by:
     * 1. An active real-time voice call.
     * 2. An active voice recording.
     * 3. Another microphone operation (Assistant already listening, audio mode communication/call, or active system recording configuration).
     */
    public boolean isMicrophoneInUse(@Nullable Activity activity) {
        return isVoiceCallActive(activity)
                || isVoiceRecordingActive(activity)
                || isAnotherMicOperationActive(activity != null ? activity : appContext);
    }

    public static boolean isVoiceCallActive(@Nullable Context context) {
        if (context == null) return false;
        try {
            return VoiceCallManager.getInstance(context).isCallActive();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isVoiceRecordingActive(@Nullable Activity activity) {
        if (activity instanceof BlindUserDashboardActivity) {
            try {
                return ((BlindUserDashboardActivity) activity).isVoiceRecordingActive();
            } catch (Throwable ignored) {
                return false;
            }
        }
        if (activity instanceof ChatActivity) {
            try {
                return ((ChatActivity) activity).isRecordingActive();
            } catch (Throwable ignored) {
                return false;
            }
        }
        if (activity instanceof SpeechImpairedDashboardActivity) {
            try {
                return ((SpeechImpairedDashboardActivity) activity).isRecordingActive();
            } catch (Throwable ignored) {
                return false;
            }
        }
        return false;
    }

    public boolean isAnotherMicOperationActive(@Nullable Context context) {
        // NOTE: We intentionally do NOT use AudioManager.getActiveRecordingConfigurations() here.
        // That API returns WakeWordManager's own AudioRecord (Vosk wake-word listener) as an
        // "active recording", which would cause the voice assistant to permanently block itself.
        // WakeWordManager is paused before STT starts, so its AudioRecord is released by the time
        // this check runs. We only check: (a) isListening flag, (b) real call AudioManager modes.
        if (isListening) {
            // Assistant's own SpeechRecognizer is already active — don't double-start
            return true;
        }
        if (context != null) {
            try {
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    int mode = audioManager.getMode();
                    if (mode == AudioManager.MODE_IN_COMMUNICATION || mode == AudioManager.MODE_IN_CALL) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public void destroy() {
        stopVoicePlayer();
        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {}
            speechRecognizer = null;
        }
        if (textToSpeech != null) {
            try {
                textToSpeech.stop();
                textToSpeech.shutdown();
            } catch (Exception ignored) {}
            textToSpeech = null;
            isTtsReady = false;
        }
        clearPendingConfirmation();
    }
}
