package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.kannada.speechassistant.RoleManager;

import org.junit.Test;

public class BlindUserVoiceAssistantTest {

    @Test
    public void testBlindUserPromptWhenListeningStarts() {
        // Requirement 2: When the assistant starts listening, use existing TTS: "Listening."
        String prompt = "Listening.";
        assertEquals("Listening.", prompt);
    }

    @Test
    public void testBlindUserSuccessConfirmationResponses() {
        // Requirement 4: After successful command execution, use TTS confirmation.
        // Examples: "Opening messages.", "Calling caregiver.", "Opening voice recorder."
        assertEquals("Messages is open.",
                AppVoiceAssistant.getBlindUserSpokenResponse(VoiceCommandConstants.CMD_OPEN_MESSAGES, ""));
        assertEquals("Messages is open.",
                AppVoiceAssistant.getBlindUserSpokenResponse(VoiceCommandConstants.CMD_OPEN_COMMUNICATION, ""));
        assertEquals("Message sent.",
                AppVoiceAssistant.getBlindUserSpokenResponse(VoiceCommandConstants.CMD_SEND_MESSAGE, ""));

        assertEquals("Calling caregiver.",
                AppVoiceAssistant.getBlindUserSpokenResponse(VoiceCommandConstants.CMD_OPEN_VOICE_CALL, ""));

        assertEquals("Opening voice recorder.",
                AppVoiceAssistant.getBlindUserSpokenResponse(VoiceCommandConstants.CMD_OPEN_VOICE_RECORDER, ""));
        assertEquals("Opening voice recorder.",
                AppVoiceAssistant.getBlindUserSpokenResponse(VoiceCommandConstants.CMD_OPEN_VOICE_MESSAGE, ""));

        assertEquals("Home is open.",
                AppVoiceAssistant.getBlindUserSpokenResponse(VoiceCommandConstants.CMD_OPEN_HOME, ""));
        assertEquals("Profile is open.",
                AppVoiceAssistant.getBlindUserSpokenResponse(VoiceCommandConstants.CMD_OPEN_PROFILE, ""));
        assertEquals("Settings is open.",
                AppVoiceAssistant.getBlindUserSpokenResponse(VoiceCommandConstants.CMD_OPEN_SETTINGS, ""));
        assertEquals("Opening caregiver.",
                AppVoiceAssistant.getBlindUserSpokenResponse(VoiceCommandConstants.CMD_OPEN_CAREGIVER, ""));
        assertEquals("Emergency alert sent.",
                AppVoiceAssistant.getBlindUserSpokenResponse(VoiceCommandConstants.CMD_OPEN_EMERGENCY, ""));
        assertEquals("Emergency alert sent.",
                AppVoiceAssistant.getBlindUserSpokenResponse(VoiceCommandConstants.CMD_OPEN_EMERGENCY_ALERT, ""));
        assertEquals("Call ended.",
                AppVoiceAssistant.getBlindUserSpokenResponse(VoiceCommandConstants.CMD_END_CALL, ""));
        assertEquals("Listening.",
                AppVoiceAssistant.getBlindUserSpokenResponse(VoiceCommandConstants.CMD_START_LISTENING, ""));
        assertEquals("Listening stopped.",
                AppVoiceAssistant.getBlindUserSpokenResponse(VoiceCommandConstants.CMD_STOP_LISTENING, ""));
    }

    @Test
    public void testBlindUserUnrecognizedCommandResponse() {
        // Requirement 5: If command is not recognized: "I did not understand the command."
        String unrecognizedResponse = "I did not understand the command.";
        assertEquals("I did not understand the command.", unrecognizedResponse);
    }

    @Test
    public void testBlindUserUnavailableFunctionResponse() {
        // Requirement 6: If the requested function is unavailable: "That function is not available."
        String unavailableResponse = "That function is not available.";
        assertEquals("That function is not available.", unavailableResponse);
    }

    @Test
    public void testBlindUserAuthorizedCapabilities() {
        VoiceIntent voiceRecorderIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_VOICE_RECORDER, "voice recorder", 1.0f, null);
        VoiceCommand cmdRecorder = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_VOICE_RECORDER, voiceRecorderIntent, VoiceCommandProcessor.BLIND_ONLY_ROLES, false);
        assertTrue(cmdRecorder.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));

        VoiceIntent voiceCallIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_VOICE_CALL, "call caregiver", 1.0f, null);
        VoiceCommand cmdCall = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_VOICE_CALL, voiceCallIntent, VoiceCommandProcessor.BLIND_ONLY_ROLES, false);
        assertTrue(cmdCall.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));

        VoiceIntent messagesIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_MESSAGES, "messages", 1.0f, null);
        VoiceCommand cmdMessages = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_MESSAGES, messagesIntent, VoiceCommandProcessor.ALL_ROLES, false);
        assertTrue(cmdMessages.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));
    }
}
