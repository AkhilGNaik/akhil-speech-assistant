package com.kannada.speechassistant.voiceassistant;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Clear intent enum model representing discrete user intentions
 * recognized by the In-App Voice Assistant without relying on fragile string equality.
 */
public enum VoiceIntentType {
    OPEN_HOME(VoiceCommandConstants.INTENT_OPEN_HOME, VoiceCommandConstants.CMD_OPEN_HOME),
    OPEN_PROFILE(VoiceCommandConstants.INTENT_OPEN_PROFILE, VoiceCommandConstants.CMD_OPEN_PROFILE),
    OPEN_SETTINGS(VoiceCommandConstants.INTENT_OPEN_SETTINGS, VoiceCommandConstants.CMD_OPEN_SETTINGS),
    OPEN_MESSAGES(VoiceCommandConstants.INTENT_OPEN_MESSAGES, VoiceCommandConstants.CMD_OPEN_MESSAGES),
    SEND_MESSAGE(VoiceCommandConstants.INTENT_SEND_MESSAGE, VoiceCommandConstants.CMD_SEND_MESSAGE),
    READ_MESSAGES(VoiceCommandConstants.INTENT_READ_MESSAGES, VoiceCommandConstants.CMD_READ_MESSAGES),
    MESSAGE_COUNT(VoiceCommandConstants.INTENT_MESSAGE_COUNT, VoiceCommandConstants.CMD_MESSAGE_COUNT),
    REPEAT_MESSAGE(VoiceCommandConstants.INTENT_REPEAT_MESSAGE, VoiceCommandConstants.CMD_REPEAT_MESSAGE),
    READ_NOTIFICATIONS(VoiceCommandConstants.INTENT_READ_NOTIFICATIONS, VoiceCommandConstants.CMD_READ_NOTIFICATIONS),
    GO_BACK(VoiceCommandConstants.INTENT_GO_BACK, VoiceCommandConstants.CMD_GO_BACK),
    OPEN_COMMUNICATION(VoiceCommandConstants.INTENT_OPEN_COMMUNICATION, VoiceCommandConstants.CMD_OPEN_COMMUNICATION),
    OPEN_CAREGIVER(VoiceCommandConstants.INTENT_OPEN_CAREGIVER, VoiceCommandConstants.CMD_OPEN_CAREGIVER),
    OPEN_CAREGIVER_CONNECTION(VoiceCommandConstants.INTENT_OPEN_CAREGIVER_CONNECTION, VoiceCommandConstants.CMD_OPEN_CAREGIVER_CONNECTION),
    OPEN_VOICE_RECORDER(VoiceCommandConstants.INTENT_OPEN_VOICE_RECORDER, VoiceCommandConstants.CMD_OPEN_VOICE_RECORDER),
    OPEN_VOICE_MESSAGE(VoiceCommandConstants.INTENT_OPEN_VOICE_MESSAGE, VoiceCommandConstants.CMD_OPEN_VOICE_MESSAGE),
    RECORD_VOICE_MESSAGE(VoiceCommandConstants.INTENT_RECORD_VOICE_MESSAGE, VoiceCommandConstants.CMD_RECORD_VOICE_MESSAGE),
    CALL_CAREGIVER(VoiceCommandConstants.INTENT_OPEN_VOICE_CALL, VoiceCommandConstants.CMD_OPEN_VOICE_CALL),
    START_LISTENING(VoiceCommandConstants.INTENT_START_LISTENING, VoiceCommandConstants.CMD_START_LISTENING),
    STOP_LISTENING(VoiceCommandConstants.INTENT_STOP_LISTENING, VoiceCommandConstants.CMD_STOP_LISTENING),
    OPEN_EMERGENCY(VoiceCommandConstants.INTENT_OPEN_EMERGENCY, VoiceCommandConstants.CMD_OPEN_EMERGENCY),
    OPEN_EMERGENCY_ALERT(VoiceCommandConstants.INTENT_OPEN_EMERGENCY_ALERT, VoiceCommandConstants.CMD_OPEN_EMERGENCY_ALERT),
    CONFIRM_YES(VoiceCommandConstants.INTENT_CONFIRM_YES, VoiceCommandConstants.CMD_CONFIRM_YES),
    CONFIRM_NO(VoiceCommandConstants.INTENT_CONFIRM_NO, VoiceCommandConstants.CMD_CONFIRM_NO),
    END_CALL(VoiceCommandConstants.INTENT_END_CALL, VoiceCommandConstants.CMD_END_CALL),
    ACCEPT_CALL(VoiceCommandConstants.INTENT_ACCEPT_CALL, VoiceCommandConstants.CMD_ACCEPT_CALL),
    UNKNOWN(VoiceCommandConstants.INTENT_UNKNOWN, VoiceCommandConstants.CMD_UNKNOWN);

    private final String intentId;
    private final String commandId;

    VoiceIntentType(@NonNull String intentId, @NonNull String commandId) {
        this.intentId = intentId;
        this.commandId = commandId;
    }

    @NonNull
    public String getIntentId() {
        return intentId;
    }

    @NonNull
    public String getCommandId() {
        return commandId;
    }

    @NonNull
    public static VoiceIntentType fromIntentId(@Nullable String intentId) {
        if (intentId == null || intentId.trim().isEmpty()) {
            return UNKNOWN;
        }
        for (VoiceIntentType type : values()) {
            if (type.getIntentId().equalsIgnoreCase(intentId) || type.name().equalsIgnoreCase(intentId)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
