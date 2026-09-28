package com.kannada.speechassistant.voiceassistant;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kannada.speechassistant.LanguageManager;
import com.kannada.speechassistant.RoleManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Orchestrates Command Processing and Intent Detection in the Voice Assistant pipeline:
 * Speech -> STT -> Command Processing -> Intent Detection -> Existing App Function -> TTS Response.
 *
 * Evaluates spoken utterances against supported commands and variations,
 * converting them into executable VoiceCommands.
 */
public class VoiceCommandProcessor {

    private static final String TAG = "VoiceCommandProcessor";

    private final Context context;
    private final List<VoiceActionHandler> registeredHandlers = new ArrayList<>();

    public interface ProcessingCallback {
        void onIntentDetected(@NonNull VoiceIntent intent);
        void onCommandResolved(@NonNull VoiceCommand command);
        void onProcessingComplete(@NonNull VoiceCommand command, @Nullable String proposedResponse);
        void onProcessingFailed(@NonNull String errorMessage);
    }

    // All 4 roles supported in app (Blind, Mute, Deaf, Caregiver/Admin)
    public static final Set<String> ALL_ROLES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            RoleManager.ROLE_BLIND_USER,
            RoleManager.ROLE_MUTE_USER,
            RoleManager.ROLE_DEAF_USER,
            RoleManager.ROLE_SPEECH_IMPAIRED,
            RoleManager.ROLE_ADMIN_CAREGIVER,
            "Caregiver",
            "Admin"
    )));

    // Roles with dedicated profile screen / tab (Blind, Mute, Deaf)
    public static final Set<String> PROFILE_ROLES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            RoleManager.ROLE_BLIND_USER,
            RoleManager.ROLE_MUTE_USER,
            RoleManager.ROLE_DEAF_USER,
            RoleManager.ROLE_SPEECH_IMPAIRED
    )));

    // Blind user specific feature roles (Voice Message card, WebRTC Voice Call)
    public static final Set<String> BLIND_ONLY_ROLES = Collections.unmodifiableSet(new HashSet<>(Collections.singletonList(
            RoleManager.ROLE_BLIND_USER
    )));

    public VoiceCommandProcessor(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public synchronized void registerActionHandler(@NonNull VoiceActionHandler handler) {
        if (!registeredHandlers.contains(handler)) {
            registeredHandlers.add(handler);
        }
    }

    public synchronized void unregisterActionHandler(@NonNull VoiceActionHandler handler) {
        registeredHandlers.remove(handler);
    }

    /**
     * Entry point for processing recognized spoken text into structured intents and commands.
     */
    public void processSpokenText(@Nullable String rawSpokenText,
                                  @Nullable String userRole,
                                  @Nullable String languageCode,
                                  @NonNull ProcessingCallback callback) {
        if (TextUtils.isEmpty(rawSpokenText) || rawSpokenText.trim().isEmpty()) {
            callback.onProcessingFailed("No speech input provided for processing.");
            return;
        }

        String normalizedLanguage = LanguageManager.normalizeLanguageCode(languageCode);
        String cleanedText = preprocessInput(rawSpokenText, normalizedLanguage);

        Log.d(TAG, "Processing speech: '" + cleanedText + "' for role: " + userRole + " in lang: " + normalizedLanguage);

        // Stage 1: Intent Detection
        VoiceIntent intent = detectIntent(cleanedText, normalizedLanguage);
        callback.onIntentDetected(intent);

        // Stage 2: Command Resolution & Role Authorization
        VoiceCommand command = resolveCommand(intent, userRole);
        callback.onCommandResolved(command);

        String proposedResponse = getProposedResponse(command, userRole);
        callback.onProcessingComplete(command, proposedResponse);
    }

    /**
     * Cleans, trims, and normalizes input speech across Kannada, Hindi, Malayalam, and English.
     * Strips punctuation, collapses excess whitespaces, and converts Latin text to lower case.
     */
    @NonNull
    public String preprocessInput(@NonNull String rawText, @NonNull String languageCode) {
        return VoiceIntentMatcher.normalizeText(rawText);
    }

    /**
     * Detects user intent from preprocessed speech supporting lightweight natural phrase matching
     * and intent enum mapping across English, Kannada, Hindi, and Malayalam.
     */
    @NonNull
    public VoiceIntent detectIntent(@NonNull String text, @NonNull String languageCode) {
        return VoiceIntentMatcher.matchIntent(text, languageCode);
    }

    /**
     * Resolves an intent into an actionable VoiceCommand with explicit role authorization.
     */
    @NonNull
    public VoiceCommand resolveCommand(@NonNull VoiceIntent intent, @Nullable String userRole) {
        VoiceIntentType type = intent.getIntentType();
        switch (type) {
            case OPEN_APP:
                return new VoiceCommand(VoiceCommandConstants.CMD_OPEN_APP, intent, ALL_ROLES, false);

            case OPEN_HOME:
                return new VoiceCommand(VoiceCommandConstants.CMD_OPEN_HOME, intent, ALL_ROLES, false);

            case OPEN_PROFILE:
                return new VoiceCommand(VoiceCommandConstants.CMD_OPEN_PROFILE, intent, PROFILE_ROLES, false);

            case OPEN_SETTINGS:
                return new VoiceCommand(VoiceCommandConstants.CMD_OPEN_SETTINGS, intent, ALL_ROLES, false);

            case OPEN_MESSAGES:
                return new VoiceCommand(VoiceCommandConstants.CMD_OPEN_MESSAGES, intent, ALL_ROLES, false);

            case SEND_MESSAGE:
                return new VoiceCommand(VoiceCommandConstants.CMD_SEND_MESSAGE, intent, ALL_ROLES, false);

            case READ_MESSAGES:
                return new VoiceCommand(VoiceCommandConstants.CMD_READ_MESSAGES, intent, ALL_ROLES, false);

            case MESSAGE_COUNT:
                return new VoiceCommand(VoiceCommandConstants.CMD_MESSAGE_COUNT, intent, ALL_ROLES, false);

            case REPEAT_MESSAGE:
                return new VoiceCommand(VoiceCommandConstants.CMD_REPEAT_MESSAGE, intent, ALL_ROLES, false);

            case READ_NOTIFICATIONS:
                return new VoiceCommand(VoiceCommandConstants.CMD_READ_NOTIFICATIONS, intent, ALL_ROLES, false);

            case GO_BACK:
                return new VoiceCommand(VoiceCommandConstants.CMD_GO_BACK, intent, ALL_ROLES, false);

            case OPEN_COMMUNICATION:
                return new VoiceCommand(VoiceCommandConstants.CMD_OPEN_COMMUNICATION, intent, ALL_ROLES, false);

            case OPEN_CAREGIVER:
                return new VoiceCommand(VoiceCommandConstants.CMD_OPEN_CAREGIVER, intent, ALL_ROLES, false);

            case OPEN_CAREGIVER_CONNECTION:
                return new VoiceCommand(VoiceCommandConstants.CMD_OPEN_CAREGIVER_CONNECTION, intent, ALL_ROLES, false);

            case OPEN_VOICE_RECORDER:
                return new VoiceCommand(VoiceCommandConstants.CMD_OPEN_VOICE_RECORDER, intent, BLIND_ONLY_ROLES, false);

            case OPEN_VOICE_MESSAGE:
                return new VoiceCommand(VoiceCommandConstants.CMD_OPEN_VOICE_MESSAGE, intent, BLIND_ONLY_ROLES, false);

            case RECORD_VOICE_MESSAGE:
                return new VoiceCommand(VoiceCommandConstants.CMD_RECORD_VOICE_MESSAGE, intent, BLIND_ONLY_ROLES, false);

            case CALL_CAREGIVER:
                return new VoiceCommand(VoiceCommandConstants.CMD_OPEN_VOICE_CALL, intent, BLIND_ONLY_ROLES, false);

            case END_CALL:
                return new VoiceCommand(VoiceCommandConstants.CMD_END_CALL, intent, ALL_ROLES, false);

            case ACCEPT_CALL:
                return new VoiceCommand(VoiceCommandConstants.CMD_ACCEPT_CALL, intent, ALL_ROLES, false);

            case OPEN_EMERGENCY:
                return new VoiceCommand(VoiceCommandConstants.CMD_OPEN_EMERGENCY, intent, ALL_ROLES, false);

            case OPEN_EMERGENCY_ALERT:
                return new VoiceCommand(VoiceCommandConstants.CMD_OPEN_EMERGENCY_ALERT, intent, ALL_ROLES, false);

            case CONFIRM_YES:
                return new VoiceCommand(VoiceCommandConstants.CMD_CONFIRM_YES, intent, ALL_ROLES, false);

            case CONFIRM_NO:
                return new VoiceCommand(VoiceCommandConstants.CMD_CONFIRM_NO, intent, ALL_ROLES, false);

            case START_LISTENING:
                return new VoiceCommand(VoiceCommandConstants.CMD_START_LISTENING, intent, ALL_ROLES, false);

            case STOP_LISTENING:
                return new VoiceCommand(VoiceCommandConstants.CMD_STOP_LISTENING, intent, ALL_ROLES, false);

            default:
                return new VoiceCommand(VoiceCommandConstants.CMD_UNKNOWN, intent, ALL_ROLES, false);
        }
    }

    @NonNull
    public String getProposedResponse(@NonNull VoiceCommand command, @Nullable String userRole) {
        if (!command.isRoleAuthorized(userRole)) {
            return VoiceCommandConstants.RESPONSE_ROLE_UNAUTHORIZED;
        }
        return getProposedResponse(command.getCommandId());
    }

    @NonNull
    public String getProposedResponse(@NonNull String commandId) {
        switch (commandId) {
            case VoiceCommandConstants.CMD_OPEN_APP:
                return VoiceCommandConstants.RESPONSE_OPEN_APP;
            case VoiceCommandConstants.CMD_OPEN_HOME:
                return VoiceCommandConstants.RESPONSE_OPEN_HOME;
            case VoiceCommandConstants.CMD_OPEN_PROFILE:
                return VoiceCommandConstants.RESPONSE_OPEN_PROFILE;
            case VoiceCommandConstants.CMD_OPEN_SETTINGS:
                return VoiceCommandConstants.RESPONSE_OPEN_SETTINGS;
            case VoiceCommandConstants.CMD_OPEN_MESSAGES:
                return VoiceCommandConstants.RESPONSE_OPEN_MESSAGES;
            case VoiceCommandConstants.CMD_SEND_MESSAGE:
                return VoiceCommandConstants.PROMPT_SAY_MESSAGE;
            case VoiceCommandConstants.CMD_READ_MESSAGES:
                return VoiceCommandConstants.RESPONSE_NO_NEW_MESSAGES;
            case VoiceCommandConstants.CMD_MESSAGE_COUNT:
                return VoiceCommandConstants.RESPONSE_NO_NEW_MESSAGES;
            case VoiceCommandConstants.CMD_REPEAT_MESSAGE:
                return VoiceCommandConstants.RESPONSE_NO_MESSAGE_TO_REPEAT;
            case VoiceCommandConstants.CMD_READ_NOTIFICATIONS:
                return VoiceCommandConstants.RESPONSE_NO_NOTIFICATIONS;
            case VoiceCommandConstants.CMD_GO_BACK:
                return VoiceCommandConstants.RESPONSE_GOING_BACK;
            case VoiceCommandConstants.CMD_OPEN_COMMUNICATION:
                return VoiceCommandConstants.RESPONSE_OPEN_COMMUNICATION;
            case VoiceCommandConstants.CMD_OPEN_CAREGIVER:
                return VoiceCommandConstants.RESPONSE_OPEN_CAREGIVER;
            case VoiceCommandConstants.CMD_OPEN_CAREGIVER_CONNECTION:
                return VoiceCommandConstants.RESPONSE_OPEN_CAREGIVER_CONNECTION;
            case VoiceCommandConstants.CMD_OPEN_VOICE_RECORDER:
                return VoiceCommandConstants.RESPONSE_OPEN_VOICE_RECORDER;
            case VoiceCommandConstants.CMD_OPEN_VOICE_MESSAGE:
                return VoiceCommandConstants.RESPONSE_OPEN_VOICE_MESSAGE;
            case VoiceCommandConstants.CMD_RECORD_VOICE_MESSAGE:
                return VoiceCommandConstants.PROMPT_SPEAK_VOICE_MESSAGE;
            case VoiceCommandConstants.CMD_OPEN_VOICE_CALL:
                return VoiceCommandConstants.RESPONSE_OPEN_VOICE_CALL;
            case VoiceCommandConstants.CMD_OPEN_EMERGENCY:
                return VoiceCommandConstants.RESPONSE_OPEN_EMERGENCY;
            case VoiceCommandConstants.CMD_OPEN_EMERGENCY_ALERT:
                return VoiceCommandConstants.RESPONSE_OPEN_EMERGENCY_ALERT;
            case VoiceCommandConstants.CMD_START_LISTENING:
                return VoiceCommandConstants.RESPONSE_START_LISTENING;
            case VoiceCommandConstants.CMD_STOP_LISTENING:
                return VoiceCommandConstants.RESPONSE_STOP_LISTENING;
            case VoiceCommandConstants.CMD_END_CALL:
                return VoiceCommandConstants.RESPONSE_END_CALL;
            case VoiceCommandConstants.CMD_ACCEPT_CALL:
                return VoiceCommandConstants.RESPONSE_ACCEPT_CALL;
            case VoiceCommandConstants.CMD_CONFIRM_YES:
                return VoiceCommandConstants.RESPONSE_CALL_CONFIRMED;
            case VoiceCommandConstants.CMD_CONFIRM_NO:
                return VoiceCommandConstants.RESPONSE_ACTION_CANCELLED;
            default:
                return VoiceCommandConstants.RESPONSE_UNKNOWN;
        }
    }
}
