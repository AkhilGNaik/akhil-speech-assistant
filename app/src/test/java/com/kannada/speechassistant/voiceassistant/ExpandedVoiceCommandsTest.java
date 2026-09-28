package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.kannada.speechassistant.RoleManager;

import org.junit.Before;
import org.junit.Test;

/**
 * Validates the expanded Voice Assistant command list:
 *
 * Pipeline:
 * Speech -> Recognized Text -> Normalized Text -> Internal Intent -> Internal Command -> Existing App Function
 *
 * Command Categories Tested:
 * 1. Navigation:
 *    - Open home
 *    - Open profile
 *    - Open settings
 *    - Open messages
 *    - Open communication
 *    - Open caregiver connection
 *
 * 2. Communication:
 *    - Open messages
 *    - Open voice recorder
 *    - Open voice messages
 *    - Call caregiver
 *    - Open voice call
 *
 * 3. Accessibility:
 *    - Start listening
 *    - Stop listening
 *
 * 4. Emergency:
 *    - Open emergency
 *    - Open emergency alert
 */
public class ExpandedVoiceCommandsTest {

    private VoiceCommandProcessor processor;

    @Before
    public void setUp() {
        processor = new VoiceCommandProcessor(new DummyContext());
    }

    private static class DummyContext extends android.content.ContextWrapper {
        public DummyContext() {
            super(null);
        }

        @Override
        public android.content.Context getApplicationContext() {
            return this;
        }
    }

    @Test
    public void testNavigationCommands() {
        String lang = "en";

        // 1. Open home
        VoiceIntent homeIntent = processor.detectIntent("Open home", lang);
        assertEquals(VoiceCommandConstants.INTENT_OPEN_HOME, homeIntent.getIntentName());
        VoiceCommand homeCmd = processor.resolveCommand(homeIntent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_OPEN_HOME, homeCmd.getCommandId());
        assertTrue(homeCmd.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));
        assertEquals(VoiceCommandConstants.RESPONSE_OPEN_HOME, processor.getProposedResponse(homeCmd, RoleManager.ROLE_BLIND_USER));

        // 2. Open profile
        VoiceIntent profileIntent = processor.detectIntent("Open profile", lang);
        assertEquals(VoiceCommandConstants.INTENT_OPEN_PROFILE, profileIntent.getIntentName());
        VoiceCommand profileCmd = processor.resolveCommand(profileIntent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_OPEN_PROFILE, profileCmd.getCommandId());
        assertTrue(profileCmd.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));

        // 3. Open settings
        VoiceIntent settingsIntent = processor.detectIntent("Open settings", lang);
        assertEquals(VoiceCommandConstants.INTENT_OPEN_SETTINGS, settingsIntent.getIntentName());
        VoiceCommand settingsCmd = processor.resolveCommand(settingsIntent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_OPEN_SETTINGS, settingsCmd.getCommandId());
        assertTrue(settingsCmd.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));

        // 4. Open messages
        VoiceIntent messagesIntent = processor.detectIntent("Open messages", lang);
        assertEquals(VoiceCommandConstants.INTENT_OPEN_MESSAGES, messagesIntent.getIntentName());
        VoiceCommand messagesCmd = processor.resolveCommand(messagesIntent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_OPEN_MESSAGES, messagesCmd.getCommandId());
        assertTrue(messagesCmd.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));

        // 5. Open communication
        VoiceIntent commIntent = processor.detectIntent("Open communication", lang);
        assertEquals(VoiceCommandConstants.INTENT_OPEN_COMMUNICATION, commIntent.getIntentName());
        VoiceCommand commCmd = processor.resolveCommand(commIntent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_OPEN_COMMUNICATION, commCmd.getCommandId());
        assertTrue(commCmd.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));

        // 6. Open caregiver connection
        VoiceIntent connIntent = processor.detectIntent("Open caregiver connection", lang);
        assertEquals(VoiceCommandConstants.INTENT_OPEN_CAREGIVER_CONNECTION, connIntent.getIntentName());
        VoiceCommand connCmd = processor.resolveCommand(connIntent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_OPEN_CAREGIVER_CONNECTION, connCmd.getCommandId());
        assertTrue(connCmd.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));
    }

    @Test
    public void testCommunicationCommands() {
        String lang = "en";

        // 1. Open messages
        VoiceIntent msgIntent = processor.detectIntent("open messages", lang);
        assertEquals(VoiceCommandConstants.INTENT_OPEN_MESSAGES, msgIntent.getIntentName());

        // 2. Open voice recorder
        VoiceIntent recorderIntent = processor.detectIntent("Open voice recorder", lang);
        assertEquals(VoiceCommandConstants.INTENT_OPEN_VOICE_RECORDER, recorderIntent.getIntentName());
        VoiceCommand recorderCmd = processor.resolveCommand(recorderIntent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_OPEN_VOICE_RECORDER, recorderCmd.getCommandId());
        assertTrue(recorderCmd.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));

        // 3. Open voice messages
        VoiceIntent voiceMsgIntent = processor.detectIntent("Open voice messages", lang);
        assertEquals(VoiceCommandConstants.INTENT_OPEN_VOICE_MESSAGE, voiceMsgIntent.getIntentName());
        VoiceCommand voiceMsgCmd = processor.resolveCommand(voiceMsgIntent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_OPEN_VOICE_MESSAGE, voiceMsgCmd.getCommandId());
        assertTrue(voiceMsgCmd.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));

        // 4. Call caregiver
        VoiceIntent callCaregiverIntent = processor.detectIntent("Call caregiver", lang);
        assertEquals(VoiceCommandConstants.INTENT_OPEN_VOICE_CALL, callCaregiverIntent.getIntentName());
        VoiceCommand callCaregiverCmd = processor.resolveCommand(callCaregiverIntent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_OPEN_VOICE_CALL, callCaregiverCmd.getCommandId());
        assertTrue(callCaregiverCmd.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));

        // 5. Open voice call
        VoiceIntent voiceCallIntent = processor.detectIntent("Open voice call", lang);
        assertEquals(VoiceCommandConstants.INTENT_OPEN_VOICE_CALL, voiceCallIntent.getIntentName());
        VoiceCommand voiceCallCmd = processor.resolveCommand(voiceCallIntent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_OPEN_VOICE_CALL, voiceCallCmd.getCommandId());
        assertTrue(voiceCallCmd.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));
    }

    @Test
    public void testAccessibilityCommands() {
        String lang = "en";

        // 1. Start listening
        VoiceIntent startIntent = processor.detectIntent("Start listening", lang);
        assertEquals(VoiceCommandConstants.INTENT_START_LISTENING, startIntent.getIntentName());
        VoiceCommand startCmd = processor.resolveCommand(startIntent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_START_LISTENING, startCmd.getCommandId());
        assertTrue(startCmd.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));
        assertEquals(VoiceCommandConstants.RESPONSE_START_LISTENING, processor.getProposedResponse(startCmd, RoleManager.ROLE_BLIND_USER));

        // Natural variation: "listen"
        assertEquals(VoiceCommandConstants.INTENT_START_LISTENING, processor.detectIntent("listen", lang).getIntentName());

        // 2. Stop listening
        VoiceIntent stopIntent = processor.detectIntent("Stop listening", lang);
        assertEquals(VoiceCommandConstants.INTENT_STOP_LISTENING, stopIntent.getIntentName());
        VoiceCommand stopCmd = processor.resolveCommand(stopIntent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_STOP_LISTENING, stopCmd.getCommandId());
        assertTrue(stopCmd.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));
        assertEquals(VoiceCommandConstants.RESPONSE_STOP_LISTENING, processor.getProposedResponse(stopCmd, RoleManager.ROLE_BLIND_USER));

        // Natural variation: "stop"
        assertEquals(VoiceCommandConstants.INTENT_STOP_LISTENING, processor.detectIntent("stop", lang).getIntentName());
    }

    @Test
    public void testEmergencyCommands() {
        String lang = "en";

        // 1. Open emergency
        VoiceIntent emergencyIntent = processor.detectIntent("Open emergency", lang);
        assertEquals(VoiceCommandConstants.INTENT_OPEN_EMERGENCY, emergencyIntent.getIntentName());
        VoiceCommand emergencyCmd = processor.resolveCommand(emergencyIntent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_OPEN_EMERGENCY, emergencyCmd.getCommandId());
        assertTrue(emergencyCmd.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));

        // 2. Open emergency alert
        VoiceIntent emergencyAlertIntent = processor.detectIntent("Open emergency alert", lang);
        assertEquals(VoiceCommandConstants.INTENT_OPEN_EMERGENCY_ALERT, emergencyAlertIntent.getIntentName());
        VoiceCommand alertCmd = processor.resolveCommand(emergencyAlertIntent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_OPEN_EMERGENCY_ALERT, alertCmd.getCommandId());
        assertTrue(alertCmd.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));
        assertEquals(VoiceCommandConstants.RESPONSE_OPEN_EMERGENCY_ALERT, processor.getProposedResponse(alertCmd, RoleManager.ROLE_BLIND_USER));
    }

    @Test
    public void testMultilingualAccessibilityAndAlerts() {
        // Kannada
        assertEquals(VoiceCommandConstants.INTENT_START_LISTENING,
                processor.detectIntent("ಕೇಳಲು ಪ್ರಾರಂಭಿಸಿ", "kn").getIntentName());
        assertEquals(VoiceCommandConstants.INTENT_STOP_LISTENING,
                processor.detectIntent("ಕೇಳುವುದನ್ನು ನಿಲ್ಲಿಸಿ", "kn").getIntentName());
        assertEquals(VoiceCommandConstants.INTENT_OPEN_EMERGENCY_ALERT,
                processor.detectIntent("ತುರ್ತು ಎಚ್ಚರಿಕೆ", "kn").getIntentName());

        // Hindi
        assertEquals(VoiceCommandConstants.INTENT_START_LISTENING,
                processor.detectIntent("सुनना शुरू करो", "hi").getIntentName());
        assertEquals(VoiceCommandConstants.INTENT_STOP_LISTENING,
                processor.detectIntent("सुनना बंद करो", "hi").getIntentName());
        assertEquals(VoiceCommandConstants.INTENT_OPEN_EMERGENCY_ALERT,
                processor.detectIntent("आपातकालीन अलर्ट", "hi").getIntentName());

        // Malayalam
        assertEquals(VoiceCommandConstants.INTENT_START_LISTENING,
                processor.detectIntent("കേൾക്കാൻ തുടങ്ങുക", "ml").getIntentName());
        assertEquals(VoiceCommandConstants.INTENT_STOP_LISTENING,
                processor.detectIntent("കേൾക്കുന്നത് നിർത്തുക", "ml").getIntentName());
        assertEquals(VoiceCommandConstants.INTENT_OPEN_EMERGENCY_ALERT,
                processor.detectIntent("അടിയന്തര മുന്നറിയിപ്പ്", "ml").getIntentName());
    }
}
