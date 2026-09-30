package com.kannada.speechassistant.voiceassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kannada.speechassistant.AccessibleMicFeedbackManager;
import com.kannada.speechassistant.ChatMessage;
import com.kannada.speechassistant.LanguageManager;
import com.kannada.speechassistant.LocalConnectionSimulator;
import com.kannada.speechassistant.NotificationService;
import com.kannada.speechassistant.RoleManager;
import com.kannada.speechassistant.SessionManager;
import com.kannada.speechassistant.SpeechImpairedDashboardActivity;
import com.kannada.speechassistant.VoiceCallManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DEAF USER VOICE ASSISTANT — Dedicated Standalone Class.
 *
 * Module: SpeechImpairedDashboardActivity (Deaf / Speech-Impaired users only).
 *
 * KEY RULES — NEVER BREAK THESE:
 * 1. TTS audio is ALWAYS OFF. Zero spoken output.
 * 2. ALL feedback is shown via DeafAssistantResponseManager (on-screen text cards only).
 * 3. WebRTC voice calls are NOT available for deaf users.
 * 4. Voice message recording is NOT available for deaf users.
 * 5. This class is COMPLETELY SEPARATE from AppVoiceAssistant. Do NOT merge them.
 */
public class DeafVoiceAssistant {

    private static final String TAG = "DeafVoiceAssistant";

    // ── Error Codes ──────────────────────────────────────────────────────
    public static final int REQUEST_CODE_VOICE_ASSISTANT_PERMISSION = 910;
    public static final int ERROR_NOT_LOGGED_IN       = 2001;
    public static final int ERROR_RECORD_AUDIO_PERM   = 2002;
    public static final int ERROR_STT_INIT            = 2003;
    public static final int ERROR_STT_RECOGNITION     = 2005;
    public static final int ERROR_COMMAND_PROCESSING  = 2006;
    public static final int ERROR_CALL_ACTIVE         = 2007;
    public static final int ERROR_MIC_BUSY            = 2009;

    // ── Singleton ────────────────────────────────────────────────────────
    private static volatile DeafVoiceAssistant instance;

    private final Context appContext;
    private final SessionManager sessionManager;
    private final VoiceCommandProcessor commandProcessor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ── STT ──────────────────────────────────────────────────────────────
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private WeakReference<Activity> activeActivityRef;

    // ── Visual Response Manager (deaf-only) ──────────────────────────────
    private WeakReference<DeafAssistantResponseManager> responseManagerRef;

    // ── State Machine ────────────────────────────────────────────────────
    public enum AssistantState {
        IDLE, WAITING_FOR_COMMAND, EXECUTING, PROCESSING,
        MESSAGE_COMPOSING, SENDING_MESSAGE,
        READING_MESSAGE, MESSAGE_COUNTING, REPEATING_MESSAGE,
        READING_NOTIFICATIONS, GOING_BACK,
        WAITING_FOR_CONFIRMATION, WAITING_FOR_CLARIFICATION
    }

    public enum ClarificationContext { NONE, HELP, MESSAGE }

    private volatile AssistantState currentState = AssistantState.IDLE;
    private final AtomicBoolean commandHandled = new AtomicBoolean(false);

    // ── Message Cache ─────────────────────────────────────────────────────
    private volatile ChatMessage lastReadCaregiverMessage = null;

    // ── Confirmation / Clarification State ───────────────────────────────
    private VoiceCommand pendingConfirmationCommand = null;
    private WeakReference<Activity> pendingConfirmationActivityRef = null;
    private VoiceAssistantCallback pendingConfirmationCallback = null;
    private ClarificationContext pendingClarificationContext = ClarificationContext.NONE;

    // ─────────────────────────────────────────────────────────────────────
    // Construction & Singleton
    // ─────────────────────────────────────────────────────────────────────

    private DeafVoiceAssistant(@NonNull Context context) {
        this.appContext      = context.getApplicationContext();
        this.sessionManager  = new SessionManager(appContext);
        this.commandProcessor = new VoiceCommandProcessor(appContext);
        // NOTE: TTS engine intentionally NOT initialized. Deaf users receive zero audio output.
    }

    @NonNull
    public static DeafVoiceAssistant getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (DeafVoiceAssistant.class) {
                if (instance == null) {
                    instance = new DeafVoiceAssistant(context);
                }
            }
        }
        return instance;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Visual Response Manager Binding
    // ─────────────────────────────────────────────────────────────────────

    public void setResponseManager(@Nullable DeafAssistantResponseManager manager) {
        this.responseManagerRef = (manager != null) ? new WeakReference<>(manager) : null;
    }

    @Nullable
    private DeafAssistantResponseManager getResponseManager() {
        if (responseManagerRef != null) {
            DeafAssistantResponseManager mgr = responseManagerRef.get();
            if (mgr != null) return mgr;
        }
        Activity active = (activeActivityRef != null) ? activeActivityRef.get() : null;
        if (active instanceof SpeechImpairedDashboardActivity) {
            return ((SpeechImpairedDashboardActivity) active).getDeafAssistantResponseManager();
        }
        return null;
    }

    // All responses shown as text on screen — never spoken.
    private void showVisual(@NonNull String message) {
        DeafAssistantResponseManager mgr = getResponseManager();
        if (mgr != null) mgr.showSuccess(message);
        Log.i(TAG, "DEAF_VISUAL_RESPONSE: " + message);
    }

    private void showVisualError(@Nullable String message) {
        DeafAssistantResponseManager mgr = getResponseManager();
        if (mgr != null) mgr.showError(message);
    }

    private void showVisualConflict(@NonNull String message) {
        DeafAssistantResponseManager mgr = getResponseManager();
        if (mgr != null) mgr.showMicConflict(message);
    }

    // ─────────────────────────────────────────────────────────────────────
    // State
    // ─────────────────────────────────────────────────────────────────────

    public void setAssistantState(@NonNull AssistantState state) {
        if (this.currentState != state) {
            Log.i(TAG, "DEAF_STATE: " + state);
            this.currentState = state;
        }
    }

    @NonNull
    public AssistantState getAssistantState() { return currentState; }

    public boolean isListening() { return isListening; }

    public void stopListening() {
        cleanupSpeechRecognizer();
        isListening = false;
        setAssistantState(AssistantState.IDLE);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Auth & Language
    // ─────────────────────────────────────────────────────────────────────

    public boolean isUserAuthenticated() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return sessionManager.isLoggedIn() && user != null;
    }

    @NonNull
    public String getCurrentLanguageCode() {
        return LanguageManager.normalizeLanguageCode(sessionManager.getLanguage());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Wake Word Entry Point
    // Called by WakeWordManager when "assistant" / "hey assistant" is heard.
    // ─────────────────────────────────────────────────────────────────────

    public void executeWakeWordCommand(@NonNull Activity activity,
                                       @NonNull String detectedPhrase,
                                       @Nullable VoiceAssistantCallback customCallback) {
        Log.i(TAG, "DEAF_WAKE_WORD: '" + detectedPhrase + "'");
        setAssistantState(AssistantState.WAITING_FOR_COMMAND);
        commandHandled.set(false);

        DeafAssistantResponseManager mgr = getResponseManager();
        if (mgr != null) mgr.showWakeWordDetected();

        String commandPortion = VoiceIntentMatcher.extractCommandText(detectedPhrase);

        if (commandPortion.isEmpty()) {
            // Style 2: only wake word said → open STT
            openSttForCommand(activity, customCallback);
            return;
        }

        // Style 1: command already in wake phrase
        VoiceIntentType quickCheck = VoiceIntentMatcher.match(commandPortion);
        if (quickCheck == VoiceIntentType.UNKNOWN) {
            Log.w(TAG, "DEAF_STYLE1_UNRECOGNIZED: '" + commandPortion + "', falling back to STT");
            openSttForCommand(activity, customCallback);
            return;
        }

        if (!commandHandled.compareAndSet(false, true)) return;

        setAssistantState(AssistantState.EXECUTING);
        if (mgr != null) mgr.showProcessingState();
        if (customCallback != null) customCallback.onSpeechRecognized(detectedPhrase);

        executeDeafCommandFlow(activity, commandPortion, getCurrentLanguageCode(), safeCallback(customCallback));
    }

    private void openSttForCommand(@NonNull Activity activity,
                                   @Nullable VoiceAssistantCallback callback) {
        mainHandler.post(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                setAssistantState(AssistantState.WAITING_FOR_COMMAND);
                MaterialButton btn = activity.findViewById(com.kannada.speechassistant.R.id.btnVoiceAssistant);
                mainHandler.postDelayed(() -> {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        AccessibleMicFeedbackManager.triggerShortVibration(activity);
                        startListeningFlow(activity, btn, callback);
                    }
                }, 300);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Start Listening Flow (button tap or wake word Style-2)
    // ─────────────────────────────────────────────────────────────────────

    public void startListeningFlow(@NonNull Activity activity,
                                   @Nullable MaterialButton button,
                                   @Nullable VoiceAssistantCallback customCallback) {
        this.activeActivityRef = new WeakReference<>(activity);

        try { WakeWordManager.getInstance(appContext).pauseListening(); } catch (Throwable ignored) {}
        cleanupSpeechRecognizer();
        isListening = false;

        if (!isUserAuthenticated()) {
            showVisualError("Please log in to use the Voice Assistant.");
            if (customCallback != null) customCallback.onError("Not logged in.", ERROR_NOT_LOGGED_IN);
            return;
        }
        if (isCallActive(activity)) {
            showVisualConflict(VoiceCommandConstants.MESSAGE_CONFLICT_CALL);
            if (customCallback != null) customCallback.onError(VoiceCommandConstants.MESSAGE_CONFLICT_CALL, ERROR_CALL_ACTIVE);
            return;
        }
        if (isAnotherMicBusy(activity)) {
            showVisualConflict(VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING);
            if (customCallback != null) customCallback.onError(VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING, ERROR_MIC_BUSY);
            return;
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            showVisualError("Microphone permission is required.");
            if (customCallback != null) customCallback.onError("RECORD_AUDIO permission required.", ERROR_RECORD_AUDIO_PERM);
            return;
        }

        String langCode = getCurrentLanguageCode();
        if (customCallback != null) customCallback.onAssistantReady(RoleManager.ROLE_DEAF_USER, langCode);
        mainHandler.post(() -> launchSpeechRecognizer(activity, langCode, customCallback));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Android STT Launch
    // ─────────────────────────────────────────────────────────────────────

    private void launchSpeechRecognizer(@NonNull Activity activity,
                                        @NonNull String langCode,
                                        @Nullable VoiceAssistantCallback callback) {
        if (speechRecognizer == null) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext);
            } catch (Exception e) {
                Log.e(TAG, "Failed to create SpeechRecognizer", e);
                showVisualError(null);
                if (callback != null) callback.onError("Failed to init speech recognizer.", ERROR_STT_INIT);
                return;
            }
        } else {
            try { speechRecognizer.cancel(); } catch (Exception ignored) {}
        }

        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        String langTag = VoiceLanguageConfig.getSpeechRecognizerLanguageTag(langCode);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                isListening = true;
                Log.i(TAG, "DEAF_STT_STARTED");
                DeafAssistantResponseManager mgr = getResponseManager();
                if (mgr != null) mgr.showListeningState();
                if (callback != null) callback.onListeningStarted();
            }

            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                isListening = false;
                Log.i(TAG, "DEAF_STT_STOPPED");
                DeafAssistantResponseManager mgr = getResponseManager();
                if (mgr != null) mgr.showProcessingState();
                if (callback != null) callback.onListeningStopped();
            }

            @Override
            public void onError(int error) {
                isListening = false;
                if (callback != null) callback.onListeningStopped();
                cleanupSpeechRecognizer();
                DeafAssistantResponseManager mgr = getResponseManager();
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    if (mgr != null) mgr.showPleaseSpeakCommand();
                } else {
                    if (mgr != null) mgr.showError(null);
                }
                if (!commandHandled.compareAndSet(false, true)) return;
                Log.w(TAG, "DEAF_STT_ERROR: code=" + error);
                onAssistantFinished(activity);
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                if (callback != null) callback.onListeningStopped();
                cleanupSpeechRecognizer();

                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches == null || matches.isEmpty()) {
                    DeafAssistantResponseManager mgr = getResponseManager();
                    if (mgr != null) mgr.showPleaseSpeakCommand();
                    if (commandHandled.compareAndSet(false, true)) onAssistantFinished(activity);
                    return;
                }

                String rawUtterance = matches.get(0).trim();
                Log.i(TAG, "DEAF_STT_RESULT: '" + rawUtterance + "'");
                if (callback != null) callback.onSpeechRecognized(rawUtterance);
                if (!commandHandled.compareAndSet(false, true)) return;

                String commandPortion = VoiceIntentMatcher.extractCommandText(rawUtterance);
                if (commandPortion.isEmpty()) commandPortion = rawUtterance;

                setAssistantState(AssistantState.EXECUTING);
                DeafAssistantResponseManager mgr = getResponseManager();
                if (mgr != null) mgr.showProcessingState();

                executeDeafCommandFlow(activity, commandPortion, langCode, safeCallback(callback));
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        try {
            speechRecognizer.startListening(recognizerIntent);
        } catch (Throwable t) {
            mainHandler.postDelayed(() -> {
                try {
                    if (speechRecognizer != null) speechRecognizer.startListening(recognizerIntent);
                } catch (Throwable t2) {
                    Log.e(TAG, "Retry startListening failed: " + t2.getMessage());
                    showVisualError(null);
                    if (callback != null) callback.onError("Microphone unavailable.", ERROR_STT_INIT);
                }
            }, 300);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Core Command Execution Pipeline
    // ─────────────────────────────────────────────────────────────────────

    private void executeDeafCommandFlow(@NonNull Activity activity,
                                        @NonNull String utterance,
                                        @NonNull String langCode,
                                        @NonNull VoiceAssistantCallback callback) {

        // 1. Pending confirmation check
        if (pendingConfirmationCommand != null) {
            handleConfirmationFlow(activity, utterance, langCode, callback);
            return;
        }

        // 2. Pending clarification check
        if (pendingClarificationContext != ClarificationContext.NONE) {
            handleClarificationFlow(activity, utterance, langCode, callback);
            return;
        }

        // 3. MESSAGE_COMPOSING active
        if (currentState == AssistantState.MESSAGE_COMPOSING) {
            handleMessageCompositionResult(activity, utterance, langCode, callback);
            return;
        }

        // 4. Normal command resolution
        commandProcessor.processSpokenText(utterance, RoleManager.ROLE_DEAF_USER, langCode,
                new VoiceCommandProcessor.ProcessingCallback() {

            @Override
            public void onIntentDetected(@NonNull VoiceIntent intent) {
                Log.i(TAG, "DEAF_INTENT: " + intent.getIntentType().name());
                callback.onIntentDetected(intent);
            }

            @Override public void onCommandResolved(@NonNull VoiceCommand command) {
                callback.onCommandResolved(command);
            }

            @Override
            public void onProcessingComplete(@NonNull VoiceCommand command, @Nullable String proposedResponse) {
                if (activity.isFinishing() || activity.isDestroyed()) {
                    onAssistantFinished(activity);
                    return;
                }
                if (command.getIntent().getIntentType() == VoiceIntentType.UNKNOWN) {
                    if (VoiceIntentMatcher.isAmbiguousHelpRequest(utterance)) {
                        initiateClarification(activity, ClarificationContext.HELP, callback);
                        return;
                    }
                    if (VoiceIntentMatcher.isAmbiguousMessageRequest(utterance)) {
                        initiateClarification(activity, ClarificationContext.MESSAGE, callback);
                        return;
                    }
                }
                if (command.requiresConfirmation()) {
                    if (!command.isRoleAuthorized(RoleManager.ROLE_DEAF_USER)) {
                        DeafAssistantResponseManager mgr = getResponseManager();
                        if (mgr != null) mgr.showFunctionNotAvailable();
                        onAssistantFinished(activity);
                        return;
                    }
                    initiateConfirmation(activity, command, callback);
                    return;
                }
                dispatchCommand(activity, command, callback);
            }

            @Override
            public void onProcessingFailed(@NonNull String errorMessage) {
                if (VoiceIntentMatcher.isAmbiguousHelpRequest(utterance)) {
                    initiateClarification(activity, ClarificationContext.HELP, callback);
                    return;
                }
                if (VoiceIntentMatcher.isAmbiguousMessageRequest(utterance)) {
                    initiateClarification(activity, ClarificationContext.MESSAGE, callback);
                    return;
                }
                Log.w(TAG, "DEAF_PROCESSING_FAILED: " + errorMessage);
                DeafAssistantResponseManager mgr = getResponseManager();
                if (mgr != null) mgr.showCommandNotRecognized();
                callback.onError(errorMessage, ERROR_COMMAND_PROCESSING);
                onAssistantFinished(activity);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Command Dispatch
    // ─────────────────────────────────────────────────────────────────────

    private void dispatchCommand(@NonNull Activity activity,
                                 @NonNull VoiceCommand command,
                                 @NonNull VoiceAssistantCallback callback) {
        String cmdId = command.getCommandId();

        // Block: voice call (not for deaf users)
        if (VoiceCommandConstants.CMD_OPEN_VOICE_CALL.equals(cmdId)
                || VoiceCommandConstants.CMD_ACCEPT_CALL.equals(cmdId)) {
            DeafAssistantResponseManager mgr = getResponseManager();
            if (mgr != null) mgr.showFunctionNotAvailable();
            onAssistantFinished(activity);
            return;
        }

        // Block: voice recording (not for deaf users)
        if (VoiceCommandConstants.CMD_RECORD_VOICE_MESSAGE.equals(cmdId)
                || VoiceCommandConstants.CMD_OPEN_VOICE_RECORDER.equals(cmdId)
                || VoiceCommandConstants.CMD_OPEN_VOICE_MESSAGE.equals(cmdId)) {
            DeafAssistantResponseManager mgr = getResponseManager();
            if (mgr != null) mgr.showFunctionNotAvailable();
            onAssistantFinished(activity);
            return;
        }

        // Route specialized flows
        if (VoiceCommandConstants.CMD_SEND_MESSAGE.equals(cmdId))       { handleSendMessageFlow(activity, callback); return; }
        if (VoiceCommandConstants.CMD_READ_MESSAGES.equals(cmdId))      { handleReadMessagesFlow(activity, callback); return; }
        if (VoiceCommandConstants.CMD_MESSAGE_COUNT.equals(cmdId))      { handleMessageCountFlow(activity, callback); return; }
        if (VoiceCommandConstants.CMD_REPEAT_MESSAGE.equals(cmdId))     { handleRepeatMessageFlow(activity, callback); return; }
        if (VoiceCommandConstants.CMD_READ_NOTIFICATIONS.equals(cmdId)) { handleReadNotificationsFlow(activity, callback); return; }
        if (VoiceCommandConstants.CMD_GO_BACK.equals(cmdId))            { handleGoBackFlow(activity, callback); return; }

        // Default: execute via VoiceActionDispatcher
        VoiceActionDispatcher.executeCommand(activity, command, RoleManager.ROLE_DEAF_USER,
                new VoiceActionDispatcher.DispatchCallback() {
            @Override
            public void onSuccess(@NonNull String spokenResponse) {
                String localizedResponse = VoiceLanguageConfig.getLocalizedResponse(
                        cmdId, spokenResponse, getCurrentLanguageCode());
                showVisual(localizedResponse);
                callback.onResponseSpoken(localizedResponse);
                onAssistantFinished(activity);
            }
            @Override
            public void onUnauthorized(@NonNull String spokenResponse) {
                DeafAssistantResponseManager mgr = getResponseManager();
                if (mgr != null) mgr.showFunctionNotAvailable();
                callback.onResponseSpoken(VoiceCommandConstants.RESPONSE_ROLE_UNAUTHORIZED);
                onAssistantFinished(activity);
            }
            @Override
            public void onUnrecognized(@NonNull String spokenResponse) {
                DeafAssistantResponseManager mgr = getResponseManager();
                if (mgr != null) mgr.showCommandNotRecognized();
                callback.onError(spokenResponse, ERROR_COMMAND_PROCESSING);
                onAssistantFinished(activity);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEND MESSAGE FLOW
    // ─────────────────────────────────────────────────────────────────────

    private void handleSendMessageFlow(@NonNull Activity activity,
                                       @NonNull VoiceAssistantCallback callback) {
        Log.i(TAG, "DEAF_SEND_MESSAGE_FLOW");

        if (isCallActive(activity)) {
            String msg = VoiceLanguageConfig.getCallActiveResponse(getCurrentLanguageCode());
            showVisualConflict(msg);
            callback.onResponseSpoken(msg);
            onAssistantFinished(activity);
            return;
        }
        if (!isCaregiverConnected(activity)) {
            String msg = VoiceLanguageConfig.getNoCaregiverResponse(getCurrentLanguageCode());
            showVisualError(msg);
            callback.onResponseSpoken(msg);
            onAssistantFinished(activity);
            return;
        }

        String prompt = VoiceLanguageConfig.getSayMessagePrompt(getCurrentLanguageCode());
        showVisual(prompt);
        callback.onResponseSpoken(prompt);

        mainHandler.postDelayed(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                startMessageDictationListening(activity, callback);
            } else {
                onAssistantFinished(activity);
            }
        }, 600);
    }

    private void startMessageDictationListening(@NonNull Activity activity,
                                                @NonNull VoiceAssistantCallback callback) {
        setAssistantState(AssistantState.MESSAGE_COMPOSING);
        cleanupSpeechRecognizer();
        WakeWordManager.getInstance(appContext).pauseListening();

        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            String err = VoiceLanguageConfig.getNoMessageHeardResponse(getCurrentLanguageCode());
            showVisualError(err);
            callback.onError(err, ERROR_STT_INIT);
            onAssistantFinished(activity);
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        String langTag = VoiceLanguageConfig.getSpeechRecognizerLanguageTag(getCurrentLanguageCode());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag);

        AtomicBoolean processed = new AtomicBoolean(false);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle p) {
                isListening = true;
                DeafAssistantResponseManager mgr = getResponseManager();
                if (mgr != null) mgr.showListeningState();
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float r) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {
                isListening = false;
                DeafAssistantResponseManager mgr = getResponseManager();
                if (mgr != null) mgr.showProcessingState();
            }
            @Override
            public void onError(int error) {
                isListening = false;
                cleanupSpeechRecognizer();
                if (!processed.compareAndSet(false, true)) return;
                String err = VoiceLanguageConfig.getNoMessageHeardResponse(getCurrentLanguageCode());
                showVisualError(err);
                callback.onError(err, ERROR_STT_RECOGNITION);
                onAssistantFinished(activity);
            }
            @Override
            public void onResults(Bundle results) {
                isListening = false;
                cleanupSpeechRecognizer();
                if (!processed.compareAndSet(false, true)) return;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches == null || matches.isEmpty() || matches.get(0) == null || matches.get(0).trim().isEmpty()) {
                    String err = VoiceLanguageConfig.getNoMessageHeardResponse(getCurrentLanguageCode());
                    showVisualError(err);
                    callback.onError(err, ERROR_STT_RECOGNITION);
                    onAssistantFinished(activity);
                    return;
                }
                String rawMessage = matches.get(0).trim();
                Log.i(TAG, "DEAF_MESSAGE_CAPTURED: '" + rawMessage + "'");
                if (VoiceIntentMatcher.isCancelMessagePhrase(rawMessage)) {
                    String cancel = VoiceLanguageConfig.getMessageCancelledResponse(getCurrentLanguageCode());
                    showVisual(cancel);
                    callback.onResponseSpoken(cancel);
                    onAssistantFinished(activity);
                    return;
                }
                setAssistantState(AssistantState.SENDING_MESSAGE);
                dispatchDeafCaregiverMessage(activity, rawMessage, () -> {
                    String confirmation = VoiceLanguageConfig.getMessageSentResponse(getCurrentLanguageCode());
                    showVisual(confirmation);
                    callback.onResponseSpoken(confirmation);
                    onAssistantFinished(activity);
                });
            }
            @Override public void onPartialResults(Bundle b) {}
            @Override public void onEvent(int e, Bundle b) {}
        });

        speechRecognizer.startListening(intent);
    }

    private void handleMessageCompositionResult(@NonNull Activity activity,
                                                @NonNull String utterance,
                                                @NonNull String langCode,
                                                @NonNull VoiceAssistantCallback callback) {
        if (VoiceIntentMatcher.isCancelMessagePhrase(utterance)) {
            setAssistantState(AssistantState.IDLE);
            String cancel = VoiceLanguageConfig.getMessageCancelledResponse(langCode);
            showVisual(cancel);
            callback.onResponseSpoken(cancel);
            onAssistantFinished(activity);
            return;
        }
        setAssistantState(AssistantState.SENDING_MESSAGE);
        dispatchDeafCaregiverMessage(activity, utterance, () -> {
            String confirmation = VoiceLanguageConfig.getMessageSentResponse(langCode);
            showVisual(confirmation);
            callback.onResponseSpoken(confirmation);
            onAssistantFinished(activity);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // READ MESSAGES FLOW
    // ─────────────────────────────────────────────────────────────────────

    private void handleReadMessagesFlow(@NonNull Activity activity,
                                        @NonNull VoiceAssistantCallback callback) {
        Log.i(TAG, "DEAF_READ_MESSAGES_FLOW");
        setAssistantState(AssistantState.READING_MESSAGE);
        cleanupSpeechRecognizer();
        WakeWordManager.getInstance(appContext).pauseListening();

        fetchLatestCaregiverMessage(msg -> {
            if (msg != null) {
                deliverMessageVisually(activity, msg, callback);
            } else {
                String noMsg = VoiceLanguageConfig.getNoNewMessagesResponse(getCurrentLanguageCode());
                showVisual(noMsg);
                callback.onResponseSpoken(noMsg);
                onAssistantFinished(activity);
            }
        });
    }

    private void deliverMessageVisually(@NonNull Activity activity,
                                        @NonNull ChatMessage msg,
                                        @NonNull VoiceAssistantCallback callback) {
        this.lastReadCaregiverMessage = msg;
        String rawContent = msg.getMessageText();
        if (rawContent == null || rawContent.trim().isEmpty()) rawContent = msg.getMessage();
        if (rawContent == null || rawContent.trim().isEmpty()) {
            String noMsg = VoiceLanguageConfig.getNoNewMessagesResponse(getCurrentLanguageCode());
            showVisual(noMsg);
            callback.onResponseSpoken(noMsg);
            onAssistantFinished(activity);
            return;
        }
        String prefix = VoiceLanguageConfig.getCaregiverSaysPrefix(getCurrentLanguageCode());
        String display = prefix + rawContent.trim();
        showVisual(display);
        callback.onResponseSpoken(display);
        onAssistantFinished(activity);
    }

    // ─────────────────────────────────────────────────────────────────────
    // MESSAGE COUNT FLOW
    // ─────────────────────────────────────────────────────────────────────

    private void handleMessageCountFlow(@NonNull Activity activity,
                                        @NonNull VoiceAssistantCallback callback) {
        Log.i(TAG, "DEAF_MESSAGE_COUNT_FLOW");
        setAssistantState(AssistantState.MESSAGE_COUNTING);
        cleanupSpeechRecognizer();
        WakeWordManager.getInstance(appContext).pauseListening();

        fetchCaregiverMessageCount(count -> {
            String response = VoiceLanguageConfig.getMessageCountResponse(count, getCurrentLanguageCode());
            showVisual(response);
            callback.onResponseSpoken(response);
            onAssistantFinished(activity);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // REPEAT MESSAGE FLOW
    // ─────────────────────────────────────────────────────────────────────

    private void handleRepeatMessageFlow(@NonNull Activity activity,
                                         @NonNull VoiceAssistantCallback callback) {
        Log.i(TAG, "DEAF_REPEAT_MESSAGE_FLOW");
        setAssistantState(AssistantState.REPEATING_MESSAGE);
        cleanupSpeechRecognizer();
        WakeWordManager.getInstance(appContext).pauseListening();

        if (lastReadCaregiverMessage != null) {
            deliverMessageVisually(activity, lastReadCaregiverMessage, callback);
        } else {
            String noMsg = VoiceLanguageConfig.getNoMessageToRepeatResponse(getCurrentLanguageCode());
            showVisual(noMsg);
            callback.onResponseSpoken(noMsg);
            onAssistantFinished(activity);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // READ NOTIFICATIONS FLOW
    // ─────────────────────────────────────────────────────────────────────

    private void handleReadNotificationsFlow(@NonNull Activity activity,
                                             @NonNull VoiceAssistantCallback callback) {
        Log.i(TAG, "DEAF_READ_NOTIFICATIONS_FLOW");
        setAssistantState(AssistantState.READING_NOTIFICATIONS);
        cleanupSpeechRecognizer();
        WakeWordManager.getInstance(appContext).pauseListening();

        List<NotificationService.NotificationItem> notifications =
                NotificationService.getReadableNotifications(activity, 3);

        if (notifications.isEmpty()) {
            String noNotif = VoiceLanguageConfig.getNoNotificationsResponse(getCurrentLanguageCode());
            showVisual(noNotif);
            callback.onResponseSpoken(noNotif);
            onAssistantFinished(activity);
            return;
        }

        String langCode = getCurrentLanguageCode();
        String finalText;
        if (notifications.size() == 1) {
            finalText = VoiceLanguageConfig.getSingleNotificationIntro(langCode)
                    + notifications.get(0).getSpokenText();
        } else {
            StringBuilder sb = new StringBuilder(
                    VoiceLanguageConfig.getMultipleNotificationsIntro(notifications.size(), langCode));
            for (NotificationService.NotificationItem item : notifications) {
                sb.append(" ").append(item.getSpokenText());
                if (!item.getSpokenText().endsWith(".")) sb.append(".");
            }
            finalText = sb.toString().trim();
        }
        showVisual(finalText);
        callback.onResponseSpoken(finalText);
        onAssistantFinished(activity);
    }

    // ─────────────────────────────────────────────────────────────────────
    // GO BACK FLOW
    // ─────────────────────────────────────────────────────────────────────

    private void handleGoBackFlow(@NonNull Activity activity,
                                  @NonNull VoiceAssistantCallback callback) {
        Log.i(TAG, "DEAF_GO_BACK_FLOW");
        setAssistantState(AssistantState.GOING_BACK);
        cleanupSpeechRecognizer();
        WakeWordManager.getInstance(appContext).pauseListening();

        boolean canBack = VoiceActionDispatcher.canNavigateBack(activity);
        String response;
        if (canBack) {
            response = VoiceLanguageConfig.getLocalizedResponse(
                    VoiceCommandConstants.CMD_GO_BACK,
                    VoiceCommandConstants.RESPONSE_GOING_BACK,
                    getCurrentLanguageCode());
            activity.runOnUiThread(() -> VoiceActionDispatcher.handleGoBack(activity));
        } else {
            response = VoiceLanguageConfig.getLocalizedResponse(
                    VoiceCommandConstants.CMD_GO_BACK,
                    VoiceCommandConstants.RESPONSE_ALREADY_FIRST_SCREEN,
                    getCurrentLanguageCode());
        }
        showVisual(response);
        callback.onResponseSpoken(response);
        onAssistantFinished(activity);
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONFIRMATION FLOW
    // ─────────────────────────────────────────────────────────────────────

    private void initiateConfirmation(@NonNull Activity activity,
                                      @NonNull VoiceCommand command,
                                      @NonNull VoiceAssistantCallback callback) {
        pendingConfirmationCommand = command;
        pendingConfirmationActivityRef = new WeakReference<>(activity);
        pendingConfirmationCallback = callback;

        String prompt = AppVoiceAssistant.getConfirmationPrompt(command.getCommandId());
        showVisual(prompt);
        callback.onResponseSpoken(prompt);

        mainHandler.postDelayed(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                setAssistantState(AssistantState.WAITING_FOR_CONFIRMATION);
                commandHandled.set(false);
                launchSpeechRecognizer(activity, getCurrentLanguageCode(), callback);
            }
        }, 700);
    }

    private void handleConfirmationFlow(@NonNull Activity activity,
                                        @NonNull String utterance,
                                        @NonNull String langCode,
                                        @NonNull VoiceAssistantCallback callback) {
        VoiceCommand pending = pendingConfirmationCommand;
        Activity target = (pendingConfirmationActivityRef != null && pendingConfirmationActivityRef.get() != null)
                ? pendingConfirmationActivityRef.get() : activity;

        VoiceIntent confirmIntent = commandProcessor.detectIntent(utterance, langCode);

        if (confirmIntent.getIntentType() == VoiceIntentType.CONFIRM_YES) {
            clearPendingConfirmation();
            String confirmed = AppVoiceAssistant.getConfirmedExecutionResponse(pending.getCommandId(), RoleManager.ROLE_DEAF_USER);
            showVisual(confirmed);
            callback.onResponseSpoken(confirmed);
            dispatchCommand(target, pending, callback);
        } else if (confirmIntent.getIntentType() == VoiceIntentType.CONFIRM_NO
                || confirmIntent.getIntentType() == VoiceIntentType.STOP_LISTENING) {
            clearPendingConfirmation();
            String cancel = VoiceCommandConstants.RESPONSE_ACTION_CANCELLED;
            showVisual(cancel);
            callback.onResponseSpoken(cancel);
            onAssistantFinished(activity);
        } else {
            clearPendingConfirmation();
            DeafAssistantResponseManager mgr = getResponseManager();
            if (mgr != null) mgr.showCommandNotRecognized();
            onAssistantFinished(activity);
        }
    }

    private void clearPendingConfirmation() {
        pendingConfirmationCommand = null;
        pendingConfirmationActivityRef = null;
        pendingConfirmationCallback = null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // CLARIFICATION FLOW
    // ─────────────────────────────────────────────────────────────────────

    private void initiateClarification(@NonNull Activity activity,
                                       @NonNull ClarificationContext context,
                                       @NonNull VoiceAssistantCallback callback) {
        pendingClarificationContext = context;
        AppVoiceAssistant.ClarificationContext mappedCtx = (context == ClarificationContext.HELP)
                ? AppVoiceAssistant.ClarificationContext.HELP
                : AppVoiceAssistant.ClarificationContext.MESSAGE;

        String prompt = AppVoiceAssistant.getClarifyPrompt(mappedCtx, getCurrentLanguageCode());
        showVisual(prompt);
        callback.onResponseSpoken(prompt);

        mainHandler.postDelayed(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                setAssistantState(AssistantState.WAITING_FOR_CLARIFICATION);
                commandHandled.set(false);
                launchSpeechRecognizer(activity, getCurrentLanguageCode(), callback);
            }
        }, 700);
    }

    private void handleClarificationFlow(@NonNull Activity activity,
                                         @NonNull String utterance,
                                         @NonNull String langCode,
                                         @NonNull VoiceAssistantCallback callback) {
        ClarificationContext ctx = pendingClarificationContext;
        pendingClarificationContext = ClarificationContext.NONE;

        String s = VoiceIntentMatcher.normalizeText(utterance);
        Set<String> words = new HashSet<>(Arrays.asList(s.split(" ")));
        VoiceIntent intent = commandProcessor.detectIntent(utterance, langCode);

        // Cancellation
        if (intent.getIntentType() == VoiceIntentType.CONFIRM_NO
                || s.equals("no") || s.equals("cancel")
                || s.equals("ಇಲ್ಲ") || s.equals("नहीं") || s.equals("വേണ്ട")) {
            String cancel = VoiceLanguageConfig.getActionCancelledResponse(langCode);
            showVisual(cancel);
            callback.onResponseSpoken(cancel);
            onAssistantFinished(activity);
            return;
        }

        if (ctx == ClarificationContext.MESSAGE) {
            boolean isAffirmative = intent.getIntentType() == VoiceIntentType.CONFIRM_YES
                    || words.contains("yes") || words.contains("send") || words.contains("message");
            if (isAffirmative) {
                handleSendMessageFlow(activity, callback);
                return;
            }
        }

        // Unrecognized
        DeafAssistantResponseManager mgr = getResponseManager();
        if (mgr != null) mgr.showCommandNotRecognized();
        callback.onError("Unrecognized clarification response.", ERROR_COMMAND_PROCESSING);
        onAssistantFinished(activity);
    }

    // ─────────────────────────────────────────────────────────────────────
    // FIRESTORE — Send Caregiver Message
    // ─────────────────────────────────────────────────────────────────────

    private void dispatchDeafCaregiverMessage(@NonNull Activity activity,
                                              @NonNull String messageText,
                                              @NonNull Runnable onComplete) {
        if (activity instanceof SpeechImpairedDashboardActivity) {
            ((SpeechImpairedDashboardActivity) activity).sendTextMessageToCaregiver(messageText, onComplete);
        } else {
            sendMessageDirectly(messageText, onComplete);
        }
    }

    private void sendMessageDirectly(@NonNull String messageText, @NonNull Runnable onComplete) {
        String currentUid = sessionManager.getUserDetails().get(SessionManager.KEY_USER_UID);
        if (currentUid == null || currentUid.isEmpty()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) currentUid = user.getUid();
        }
        SharedPreferences prefs = appContext.getSharedPreferences("SpeechSettings", Context.MODE_PRIVATE);
        String caregiverUid   = prefs.getString("connectedCaregiverUid", null);
        String caregiverEmail = prefs.getString("caregiverEmail", "");
        final String finalUid = currentUid != null ? currentUid : "unknown_user";

        if (caregiverUid != null && !caregiverUid.trim().isEmpty()) {
            writeMessageToFirestore(finalUid, caregiverUid, caregiverEmail, messageText, onComplete);
        } else {
            writeMessageToFirestore(finalUid, "mock_caregiver_uid", caregiverEmail, messageText, onComplete);
        }
    }

    private void writeMessageToFirestore(@NonNull String senderUid,
                                         @NonNull String receiverUid,
                                         @NonNull String recipientEmail,
                                         @NonNull String messageText,
                                         @NonNull Runnable onComplete) {
        String chatId = senderUid.compareTo(receiverUid) < 0
                ? senderUid + "_" + receiverUid
                : receiverUid + "_" + senderUid;
        Map<String, Object> msg = new HashMap<>();
        msg.put("chatId", chatId);
        msg.put("senderId", senderUid);
        msg.put("senderUid", senderUid);
        msg.put("receiverId", receiverUid);
        msg.put("recipientEmail", recipientEmail.toLowerCase().trim());
        msg.put("senderRole", RoleManager.ROLE_DEAF_USER);
        msg.put("receiverRole", RoleManager.ROLE_ADMIN_CAREGIVER);
        msg.put("message", messageText);
        msg.put("messageText", messageText);
        msg.put("language", sessionManager.getLanguage());
        msg.put("messageType", "text");
        msg.put("type", "text");
        msg.put("isVoice", false);
        msg.put("status", "sent");
        msg.put("readStatus", false);
        msg.put("delivered", false);
        msg.put("seen", false);
        msg.put("timestamp", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("caregiver_messages")
                .add(msg)
                .addOnSuccessListener(ref -> {
                    new Handler(Looper.getMainLooper()).postDelayed(
                            () -> ref.update("status", "delivered", "delivered", true), 1000);
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    LocalConnectionSimulator.saveLocalMessage(appContext, chatId, msg);
                    if (onComplete != null) onComplete.run();
                });
    }

    // ─────────────────────────────────────────────────────────────────────
    // FIRESTORE — Fetch Data
    // ─────────────────────────────────────────────────────────────────────

    private interface MessageCallback { void onResult(@Nullable ChatMessage msg); }
    private interface CountCallback { void onCount(int count); }

    private void fetchLatestCaregiverMessage(@NonNull MessageCallback cb) {
        String uid = sessionManager.getUserDetails().get(SessionManager.KEY_USER_UID);
        if (uid == null || uid.isEmpty()) {
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            if (u != null) uid = u.getUid();
        }
        if (uid == null || uid.isEmpty()) { cb.onResult(null); return; }
        final String myUid = uid;

        FirebaseFirestore.getInstance().collection("caregiver_messages")
                .whereEqualTo("receiverId", myUid)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap == null || snap.isEmpty()) { cb.onResult(null); return; }
                    List<ChatMessage> list = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                        list.add(new ChatMessage(d));
                    }
                    Collections.sort(list, (a, b) -> {
                        if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
                        if (a.getTimestamp() == null) return 1;
                        if (b.getTimestamp() == null) return -1;
                        return a.getTimestamp().compareTo(b.getTimestamp());
                    });
                    for (int i = list.size() - 1; i >= 0; i--) {
                        ChatMessage m = list.get(i);
                        if (myUid.equals(m.getSenderId())) continue;
                        String text = m.getMessageText();
                        if (text == null || text.trim().isEmpty()) text = m.getMessage();
                        if (text != null && !text.trim().isEmpty()) { cb.onResult(m); return; }
                    }
                    cb.onResult(null);
                })
                .addOnFailureListener(e -> cb.onResult(null));
    }

    private void fetchCaregiverMessageCount(@NonNull CountCallback cb) {
        String uid = sessionManager.getUserDetails().get(SessionManager.KEY_USER_UID);
        if (uid == null || uid.isEmpty()) {
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            if (u != null) uid = u.getUid();
        }
        if (uid == null || uid.isEmpty()) { cb.onCount(0); return; }
        final String myUid = uid;

        FirebaseFirestore.getInstance().collection("caregiver_messages")
                .whereEqualTo("receiverId", myUid)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap == null || snap.isEmpty()) { cb.onCount(0); return; }
                    int count = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                        ChatMessage m = new ChatMessage(d);
                        if (myUid.equals(m.getSenderId())) continue;
                        String text = m.getMessageText();
                        if (text == null || text.trim().isEmpty()) text = m.getMessage();
                        if (text != null && !text.trim().isEmpty()) count++;
                    }
                    cb.onCount(count);
                })
                .addOnFailureListener(e -> cb.onCount(0));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helper / Conflict Checks
    // ─────────────────────────────────────────────────────────────────────

    private boolean isCallActive(@Nullable Activity activity) {
        try { return VoiceCallManager.getInstance(activity != null ? activity : appContext).isCallActive(); }
        catch (Throwable ignored) { return false; }
    }

    private boolean isAnotherMicBusy(@Nullable Activity activity) {
        Context ctx = activity != null ? activity : appContext;
        try {
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                int mode = am.getMode();
                return mode == AudioManager.MODE_IN_COMMUNICATION || mode == AudioManager.MODE_IN_CALL;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private boolean isCaregiverConnected(@Nullable Activity activity) {
        if (activity instanceof SpeechImpairedDashboardActivity) {
            return ((SpeechImpairedDashboardActivity) activity).isCaregiverConnected();
        }
        SharedPreferences prefs = appContext.getSharedPreferences("SpeechSettings", Context.MODE_PRIVATE);
        String uid = prefs.getString("connectedCaregiverUid", "");
        return uid != null && !uid.trim().isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Button Attachment
    // ─────────────────────────────────────────────────────────────────────

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
            if (isCallActive(activity)) {
                showVisualConflict(VoiceCommandConstants.MESSAGE_CONFLICT_CALL);
                return;
            }
            if (isAnotherMicBusy(activity)) {
                showVisualConflict(VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING);
                return;
            }
            button.setText("🛑 Listening...");
            startListeningFlow(activity, button, new VoiceAssistantCallback() {
                @Override public void onAssistantReady(@NonNull String r, @NonNull String l) {}
                @Override public void onListeningStarted() {}
                @Override public void onListeningStopped() {
                    mainHandler.post(() -> button.setText("🎤 Voice Assistant"));
                }
                @Override public void onSpeechRecognized(@NonNull String t) {}
                @Override public void onIntentDetected(@NonNull VoiceIntent i) {}
                @Override public void onCommandResolved(@NonNull VoiceCommand c) {}
                @Override public void onResponseSpoken(@NonNull String r) {}
                @Override public void onError(@NonNull String e, int code) {
                    mainHandler.post(() -> button.setText("🎤 Voice Assistant"));
                    if (customCallback != null) customCallback.onError(e, code);
                }
            });
        });
    }

    public void handlePermissionsResult(@NonNull Activity activity,
                                        int requestCode,
                                        @NonNull int[] grantResults,
                                        @Nullable android.view.View voiceBtn) {
        if (requestCode == REQUEST_CODE_VOICE_ASSISTANT_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                MaterialButton btn = (voiceBtn instanceof MaterialButton) ? (MaterialButton) voiceBtn : null;
                startListeningFlow(activity, btn, null);
            } else {
                showVisualError("Microphone permission denied.");
                Toast.makeText(activity, "Microphone permission is required.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Cleanup / Lifecycle
    // ─────────────────────────────────────────────────────────────────────

    private synchronized void cleanupSpeechRecognizer() {
        isListening = false;
        if (speechRecognizer != null) {
            try { speechRecognizer.cancel(); } catch (Exception ignored) {}
            try { speechRecognizer.stopListening(); } catch (Exception ignored) {}
        }
        Log.d(TAG, "DEAF_MIC_RELEASED");
    }

    private void onAssistantFinished(@Nullable Activity activity) {
        cleanupSpeechRecognizer();
        mainHandler.postDelayed(() -> {
            setAssistantState(AssistantState.IDLE);
            commandHandled.set(false);
            Log.i(TAG, "DEAF_ASSISTANT_IDLE");
            Activity target = activity;
            try {
                Activity fg = WakeWordManager.getInstance(appContext).getCurrentActivity();
                if (fg != null && !fg.isFinishing() && !fg.isDestroyed()) target = fg;
            } catch (Throwable ignored) {}
            if (target != null && !target.isFinishing() && !target.isDestroyed()
                    && target instanceof SpeechImpairedDashboardActivity) {
                WakeWordManager.getInstance(appContext).resumeListening(target);
            }
        }, 600);
    }

    @NonNull
    private VoiceAssistantCallback safeCallback(@Nullable VoiceAssistantCallback callback) {
        if (callback != null) return callback;
        return new VoiceAssistantCallback() {
            @Override public void onAssistantReady(@NonNull String r, @NonNull String l) {}
            @Override public void onListeningStarted() {}
            @Override public void onListeningStopped() {}
            @Override public void onSpeechRecognized(@NonNull String t) {}
            @Override public void onIntentDetected(@NonNull VoiceIntent i) {}
            @Override public void onCommandResolved(@NonNull VoiceCommand c) {}
            @Override public void onResponseSpoken(@NonNull String r) {}
            @Override public void onError(@NonNull String e, int code) {}
        };
    }

    public void destroy() {
        if (speechRecognizer != null) {
            try { speechRecognizer.destroy(); } catch (Exception ignored) {}
            speechRecognizer = null;
        }
        clearPendingConfirmation();
        Log.i(TAG, "DEAF_VOICE_ASSISTANT_DESTROYED");
    }
}
