package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.kannada.speechassistant.RoleManager;

import org.junit.Test;

/**
 * Unit tests verifying the OPEN_APP voice command specifically for the Blind User Module.
 *
 * Validates:
 * 1. Intent matching: "Hey Assistant, open app" (and variations) maps to VoiceIntentType.OPEN_APP.
 * 2. Background restriction: Only OPEN_APP phrases are recognized by the background service.
 *    All other commands (CALL_CAREGIVER, SEND_MESSAGE, EMERGENCY_SOS, etc.) are strictly rejected in the background.
 * 3. Role authorization: OPEN_APP is strictly authorized ONLY for RoleManager.ROLE_BLIND_USER.
 * 4. Spoken response: Returns VoiceCommandConstants.RESPONSE_OPEN_APP.
 */
public class BlindUserOpenAppCommandTest {

    @Test
    public void testOpenAppIntentMatchingVariations() {
        // English variations
        assertEquals(VoiceIntentType.OPEN_APP, VoiceIntentMatcher.matchIntent("open app").getIntentType());
        assertEquals(VoiceIntentType.OPEN_APP, VoiceIntentMatcher.matchIntent("hey assistant open app").getIntentType());
        assertEquals(VoiceIntentType.OPEN_APP, VoiceIntentMatcher.matchIntent("assistant open app").getIntentType());
        assertEquals(VoiceIntentType.OPEN_APP, VoiceIntentMatcher.matchIntent("launch app").getIntentType());
        assertEquals(VoiceIntentType.OPEN_APP, VoiceIntentMatcher.matchIntent("hey assistant launch app").getIntentType());
        assertEquals(VoiceIntentType.OPEN_APP, VoiceIntentMatcher.matchIntent("start app").getIntentType());
        assertEquals(VoiceIntentType.OPEN_APP, VoiceIntentMatcher.matchIntent("open the app").getIntentType());
        assertEquals(VoiceIntentType.OPEN_APP, VoiceIntentMatcher.matchIntent("open my app").getIntentType());
        assertEquals(VoiceIntentType.OPEN_APP, VoiceIntentMatcher.matchIntent("open speech assistant").getIntentType());
        assertEquals(VoiceIntentType.OPEN_APP, VoiceIntentMatcher.matchIntent("hey assistant open speech assistant").getIntentType());

        // Kannada variations
        assertEquals(VoiceIntentType.OPEN_APP, VoiceIntentMatcher.matchIntent("ಆ್ಯಪ್ ತೆರೆ", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_APP, VoiceIntentMatcher.matchIntent("ಆ್ಯಪ್ ತೆರೆಯಿರಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_APP, VoiceIntentMatcher.matchIntent("ಆ್ಯಪ್ ಓಪನ್ ಮಾಡು", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_APP, VoiceIntentMatcher.matchIntent("ಸ್ಪೀಚ್ ಅಸಿಸ್ಟೆಂಟ್ ತೆರೆ", "kn").getIntentType());
    }

    @Test
    public void testBackgroundServiceRecognizesOnlyOpenApp() {
        // Must return true for open app phrases
        assertTrue(BlindWakeWordService.isOpenAppPhrase("hey assistant open app"));
        assertTrue(BlindWakeWordService.isOpenAppPhrase("assistant open app"));
        assertTrue(BlindWakeWordService.isOpenAppPhrase("open app"));
        assertTrue(BlindWakeWordService.isOpenAppPhrase("launch app"));
        assertTrue(BlindWakeWordService.isOpenAppPhrase("open speech assistant"));
        assertTrue(BlindWakeWordService.isOpenAppPhrase("hey assistant launch app"));
        assertTrue(BlindWakeWordService.isOpenAppPhrase("ಆ್ಯಪ್ ತೆರೆ"));
        assertTrue(BlindWakeWordService.isOpenAppPhrase("ಆ್ಯಪ್ ಓಪನ್ ಮಾಡು"));
    }

    @Test
    public void testBackgroundServiceRejectsAllOtherCommands() {
        // STRICT REQUIREMENT: Other voice commands must NOT work in the background!
        assertFalse("CALL_CAREGIVER must be rejected in background",
                BlindWakeWordService.isOpenAppPhrase("hey assistant call caregiver"));
        assertFalse("CALL_CAREGIVER must be rejected in background",
                BlindWakeWordService.isOpenAppPhrase("call caregiver"));
        assertFalse("CALL_CAREGIVER must be rejected in background",
                BlindWakeWordService.isOpenAppPhrase("call caretaker"));

        assertFalse("SEND_MESSAGE must be rejected in background",
                BlindWakeWordService.isOpenAppPhrase("hey assistant send message"));
        assertFalse("SEND_MESSAGE must be rejected in background",
                BlindWakeWordService.isOpenAppPhrase("send message"));

        assertFalse("EMERGENCY_SOS must be rejected in background",
                BlindWakeWordService.isOpenAppPhrase("hey assistant emergency"));
        assertFalse("EMERGENCY_SOS must be rejected in background",
                BlindWakeWordService.isOpenAppPhrase("sos"));
        assertFalse("EMERGENCY_SOS must be rejected in background",
                BlindWakeWordService.isOpenAppPhrase("emergency"));

        assertFalse("RECORD_VOICE_MESSAGE must be rejected in background",
                BlindWakeWordService.isOpenAppPhrase("hey assistant record voice message"));
        assertFalse("RECORD_VOICE_MESSAGE must be rejected in background",
                BlindWakeWordService.isOpenAppPhrase("record voice message"));

        assertFalse("READ_MESSAGES must be rejected in background",
                BlindWakeWordService.isOpenAppPhrase("hey assistant read messages"));
        assertFalse("MESSAGE_COUNT must be rejected in background",
                BlindWakeWordService.isOpenAppPhrase("hey assistant count messages"));
        assertFalse("REPEAT_MESSAGE must be rejected in background",
                BlindWakeWordService.isOpenAppPhrase("hey assistant repeat message"));
        assertFalse("READ_NOTIFICATIONS must be rejected in background",
                BlindWakeWordService.isOpenAppPhrase("hey assistant read notifications"));

        assertFalse("GO_BACK must be rejected in background",
                BlindWakeWordService.isOpenAppPhrase("hey assistant go back"));
        assertFalse("OPEN_PROFILE must be rejected in background",
                BlindWakeWordService.isOpenAppPhrase("hey assistant open profile"));
        assertFalse("OPEN_SETTINGS must be rejected in background",
                BlindWakeWordService.isOpenAppPhrase("hey assistant open settings"));
        assertFalse("STOP_LISTENING must be rejected in background",
                BlindWakeWordService.isOpenAppPhrase("hey assistant stop listening"));
    }

    @Test
    public void testOpenAppRoleAuthorizationBlindOnly() {
        VoiceIntent intent = new VoiceIntent(VoiceIntentType.OPEN_APP, "open app", 1.0f, null);
        VoiceCommand command = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_APP, intent, VoiceCommandProcessor.BLIND_ONLY_ROLES, false);

        // Authorized ONLY for Blind User
        assertTrue(command.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));

        // Unauthorized for other user roles
        assertFalse(command.isRoleAuthorized(RoleManager.ROLE_MUTE_USER));
        assertFalse(command.isRoleAuthorized(RoleManager.ROLE_DEAF_USER));
        assertFalse(command.isRoleAuthorized(RoleManager.ROLE_SPEECH_IMPAIRED));
        assertFalse(command.isRoleAuthorized(RoleManager.ROLE_ADMIN_CAREGIVER));
        assertFalse(command.isRoleAuthorized(RoleManager.ROLE_PHYSICALLY_DISABLED));
    }

    @Test
    public void testOpenAppSpokenFeedbackResponse() {
        assertEquals("Speech Assistant is open.", VoiceCommandConstants.RESPONSE_OPEN_APP);
    }
}
