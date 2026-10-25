package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.kannada.speechassistant.ChatMessage;
import com.kannada.speechassistant.RoleManager;
import com.kannada.speechassistant.SpeechImpairedDashboardActivity;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * JVM Unit Test Suite for TASK 1:
 * Deaf User Visual Voice Assistant Foundation.
 */
public class DeafUserVoiceAssistantUnitTest {

    @Test
    public void testDeafUserRoleCapabilities() {
        String role = RoleManager.ROLE_DEAF_USER;

        VoiceIntent profileIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_PROFILE, "open profile", 1.0f, null);
        VoiceCommand cmdProfile = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_PROFILE, profileIntent, VoiceCommandProcessor.PROFILE_ROLES, false);
        assertTrue("Existing feature Open Profile must be authorized for Deaf User", cmdProfile.isRoleAuthorized(role));

        VoiceIntent homeIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_HOME, "open home", 1.0f, null);
        VoiceCommand cmdHome = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_HOME, homeIntent, VoiceCommandProcessor.ALL_ROLES, false);
        assertTrue("Open Home must be authorized for Deaf User", cmdHome.isRoleAuthorized(role));

        // Caregiver calling must NOT be added
        VoiceIntent callIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_VOICE_CALL, "open voice call", 1.0f, null);
        VoiceCommand cmdCall = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_VOICE_CALL, callIntent, VoiceCommandProcessor.BLIND_ONLY_ROLES, false);
        assertFalse("CAREGIVER CALLING ADDED = NO: Voice call must NOT be authorized for Deaf User", cmdCall.isRoleAuthorized(role));

        VoiceIntent msgIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_VOICE_MESSAGE, "open voice message", 1.0f, null);
        VoiceCommand cmdMsg = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_VOICE_MESSAGE, msgIntent, VoiceCommandProcessor.BLIND_ONLY_ROLES, false);
        assertFalse("Voice message must NOT be authorized for Deaf User", cmdMsg.isRoleAuthorized(role));
    }

    @Test
    public void testBlindAssistantRemainsUnchanged() {
        String role = RoleManager.ROLE_BLIND_USER;

        VoiceIntent callIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_VOICE_CALL, "call caregiver", 1.0f, null);
        VoiceCommand cmdCall = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_VOICE_CALL, callIntent, VoiceCommandProcessor.BLIND_ONLY_ROLES, false);
        assertTrue("BLIND ASSISTANT MODIFIED = NO: Blind User voice call authorization preserved", cmdCall.isRoleAuthorized(role));

        VoiceIntent profileIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_PROFILE, "open profile", 1.0f, null);
        VoiceCommand cmdProfile = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_PROFILE, profileIntent, VoiceCommandProcessor.PROFILE_ROLES, false);
        assertTrue("Blind User profile authorization preserved", cmdProfile.isRoleAuthorized(role));
    }

    @Test
    public void testMultilingualVisualStrings() {
        // Kannada
        assertEquals("ಸಹಾಯಕ ಸಕ್ರಿಯಗೊಂಡಿದೆ", VoiceLanguageConfig.getDeafAssistantActivatedText("kn"));
        assertEquals("ಆಲಿಸುತ್ತಿದೆ...", VoiceLanguageConfig.getDeafListeningText("kn"));
        assertEquals("ನಿಮ್ಮ ಆಜ್ಞೆಯನ್ನು ಮಾತನಾಡಿ", VoiceLanguageConfig.getDeafSpeakCommandPrompt("kn"));
        assertEquals("ಪ್ರಕ್ರಿಯೆ ನಡೆಯುತ್ತಿದೆ...", VoiceLanguageConfig.getDeafProcessingText("kn"));
        assertEquals("ಕ್ಷಮಿಸಿ, ಆಜ್ಞೆ ಅರ್ಥವಾಗಲಿಲ್ಲ.", VoiceLanguageConfig.getDeafCommandNotUnderstoodText("kn"));
        assertEquals("ಈ ಕಾರ್ಯ ಲಭ್ಯವಿಲ್ಲ.", VoiceLanguageConfig.getDeafFunctionNotAvailableText("kn"));
        assertEquals("ಏನೋ ತಪ್ಪಾಗಿದೆ. ದಯವಿಟ್ಟು ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ.", VoiceLanguageConfig.getDeafErrorText("kn"));
        assertEquals("ಪ್ರೊಫೈಲ್ ತೆರೆಯಲಾಗಿದೆ", VoiceLanguageConfig.getDeafProfileOpenedText("kn"));

        // Hindi
        assertEquals("सहायक सक्रिय हुआ", VoiceLanguageConfig.getDeafAssistantActivatedText("hi"));
        assertEquals("सुन रहा है...", VoiceLanguageConfig.getDeafListeningText("hi"));
        assertEquals("अपना आदेश बोलें", VoiceLanguageConfig.getDeafSpeakCommandPrompt("hi"));
        assertEquals("प्रक्रिया जारी है...", VoiceLanguageConfig.getDeafProcessingText("hi"));
        assertEquals("क्षमा करें, आदेश समझ नहीं आया।", VoiceLanguageConfig.getDeafCommandNotUnderstoodText("hi"));
        assertEquals("यह कार्य उपलब्ध नहीं है।", VoiceLanguageConfig.getDeafFunctionNotAvailableText("hi"));
        assertEquals("कुछ गलत हो गया। कृपया पुनः प्रयास करें।", VoiceLanguageConfig.getDeafErrorText("hi"));
        assertEquals("प्रोफ़ाइल खोला गया", VoiceLanguageConfig.getDeafProfileOpenedText("hi"));

        // Malayalam
        assertEquals("അസിസ്റ്റന്റ് സജീവമായി", VoiceLanguageConfig.getDeafAssistantActivatedText("ml"));
        assertEquals("കേൾക്കുന്നു...", VoiceLanguageConfig.getDeafListeningText("ml"));
        assertEquals("നിങ്ങളുടെ കമാൻഡ് പറയുക", VoiceLanguageConfig.getDeafSpeakCommandPrompt("ml"));
        assertEquals("പ്രോസസ്സ് ചെയ്യുന്നു...", VoiceLanguageConfig.getDeafProcessingText("ml"));
        assertEquals("ക്ഷമിക്കണം, കമാൻഡ് മനസ്സിലായില്ല.", VoiceLanguageConfig.getDeafCommandNotUnderstoodText("ml"));
        assertEquals("ഈ പ്രവർത്തനം ലഭ്യമല്ല.", VoiceLanguageConfig.getDeafFunctionNotAvailableText("ml"));
        assertEquals("എന്തോ കുഴപ്പം സംഭവിച്ചു. ദയവായി വീണ്ടും ശ്രമിക്കുക.", VoiceLanguageConfig.getDeafErrorText("ml"));
        assertEquals("പ്രൊഫൈൽ തുറന്നു", VoiceLanguageConfig.getDeafProfileOpenedText("ml"));

        // English
        assertEquals("Assistant activated", VoiceLanguageConfig.getDeafAssistantActivatedText("en"));
        assertEquals("Listening...", VoiceLanguageConfig.getDeafListeningText("en"));
        assertEquals("Speak your command", VoiceLanguageConfig.getDeafSpeakCommandPrompt("en"));
        assertEquals("Processing...", VoiceLanguageConfig.getDeafProcessingText("en"));
        assertEquals("Sorry, I didn't understand the command.", VoiceLanguageConfig.getDeafCommandNotUnderstoodText("en"));
        assertEquals("This function is not available.", VoiceLanguageConfig.getDeafFunctionNotAvailableText("en"));
        assertEquals("Something went wrong. Please try again.", VoiceLanguageConfig.getDeafErrorText("en"));
        assertEquals("Profile opened", VoiceLanguageConfig.getDeafProfileOpenedText("en"));
    }

    @Test
    public void testUniversalWakeWordAssistant() {
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("assistant"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("ASSISTANT"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant open profile"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("hey assistant open profile"));
    }

    @Test
    public void testLayoutIntegrityAndQuickPhrasesRemoval() throws Exception {
        File layoutFile = new File("src/main/res/layout/activity_speech_impaired_dashboard.xml");
        assertTrue(layoutFile.exists());

        String content = new String(Files.readAllBytes(layoutFile.toPath()));
        assertTrue(content.contains("id=\"@+id/cardDeafAssistantResponse\""));
        assertTrue(content.contains("id=\"@+id/txtDeafAssistantTitle\""));
        assertTrue(content.contains("id=\"@+id/txtDeafAssistantMessage\""));
        assertTrue(content.contains("id=\"@+id/txtDeafAssistantSubMessage\""));
        assertTrue(content.contains("id=\"@+id/progressDeafAssistant\""));
        assertTrue(content.contains("id=\"@+id/btnDismissDeafAssistant\""));

        // Quick Phrases must remain removed
        assertFalse(content.contains("layoutQuickPhrases"));
        assertFalse(content.contains("Quick Phrases"));
    }

    @Test
    public void testProtectedFilesZeroModifications() {
        String[] protectedFiles = new String[]{
                "src/main/java/com/kannada/speechassistant/WebRTCManager.java",
                "src/main/java/com/kannada/speechassistant/VoiceCallManager.java",
                "src/main/java/com/kannada/speechassistant/VoiceCallSignalingManager.java",
                "src/main/java/com/kannada/speechassistant/VoiceCallActivity.java",
                "src/main/java/com/kannada/speechassistant/IncomingCallActivity.java",
                "src/main/java/com/kannada/speechassistant/FirestoreRealtimeService.java"
        };

        for (String relativePath : protectedFiles) {
            File f = new File(relativePath);
            assertTrue("Protected file must exist: " + relativePath, f.exists());
        }
    }

    @Test
    public void testDeafUserRoleIdentification() {
        assertTrue(AppVoiceAssistant.isDeafUserRole(RoleManager.ROLE_DEAF_USER));
        assertTrue(AppVoiceAssistant.isDeafUserRole(RoleManager.ROLE_SPEECH_IMPAIRED));
        assertTrue(AppVoiceAssistant.isDeafUserRole("Deaf"));
        assertTrue(AppVoiceAssistant.isDeafUserRole("Speech-Impaired"));
        assertTrue(AppVoiceAssistant.isDeafUserRole("deaf user"));
        assertTrue(AppVoiceAssistant.isDeafUserRole("speech-impaired user"));

        assertFalse(AppVoiceAssistant.isDeafUserRole(RoleManager.ROLE_BLIND_USER));
        assertFalse(AppVoiceAssistant.isDeafUserRole(RoleManager.ROLE_PHYSICALLY_DISABLED));
        assertFalse(AppVoiceAssistant.isDeafUserRole(RoleManager.ROLE_MUTE_USER));
        assertFalse(AppVoiceAssistant.isDeafUserRole(RoleManager.ROLE_ADMIN_CAREGIVER));
        assertFalse(AppVoiceAssistant.isDeafUserRole(null));
        assertFalse(AppVoiceAssistant.isDeafUserRole(""));
    }

    @Test
    public void testDeafSuccessTextForCommandMapping() {
        String[] languages = {"kn", "hi", "ml", "en"};
        for (String lang : languages) {
            // Home
            assertEquals(VoiceLanguageConfig.getDeafHomeOpenedText(lang),
                    VoiceLanguageConfig.getDeafSuccessTextForCommand(VoiceCommandConstants.CMD_OPEN_HOME, "default", lang));
            // Profile
            assertEquals(VoiceLanguageConfig.getDeafProfileOpenedText(lang),
                    VoiceLanguageConfig.getDeafSuccessTextForCommand(VoiceCommandConstants.CMD_OPEN_PROFILE, "default", lang));
            // Settings
            assertEquals(VoiceLanguageConfig.getDeafSettingsOpenedText(lang),
                    VoiceLanguageConfig.getDeafSuccessTextForCommand(VoiceCommandConstants.CMD_OPEN_SETTINGS, "default", lang));
            // Caregiver
            assertEquals(VoiceLanguageConfig.getDeafCaregiverOpenedText(lang),
                    VoiceLanguageConfig.getDeafSuccessTextForCommand(VoiceCommandConstants.CMD_OPEN_CAREGIVER, "default", lang));
            assertEquals(VoiceLanguageConfig.getDeafCaregiverOpenedText(lang),
                    VoiceLanguageConfig.getDeafSuccessTextForCommand(VoiceCommandConstants.CMD_OPEN_CAREGIVER_CONNECTION, "default", lang));
            assertEquals(VoiceLanguageConfig.getDeafCaregiverOpenedText(lang),
                    VoiceLanguageConfig.getDeafSuccessTextForCommand(VoiceCommandConstants.CMD_OPEN_MESSAGES, "default", lang));
            assertEquals(VoiceLanguageConfig.getDeafCaregiverOpenedText(lang),
                    VoiceLanguageConfig.getDeafSuccessTextForCommand(VoiceCommandConstants.CMD_OPEN_COMMUNICATION, "default", lang));
            // Emergency
            assertEquals(VoiceLanguageConfig.getDeafEmergencyAlertSentText(lang),
                    VoiceLanguageConfig.getDeafSuccessTextForCommand(VoiceCommandConstants.CMD_OPEN_EMERGENCY, "default", lang));
            assertEquals(VoiceLanguageConfig.getDeafEmergencyAlertSentText(lang),
                    VoiceLanguageConfig.getDeafSuccessTextForCommand(VoiceCommandConstants.CMD_OPEN_EMERGENCY_ALERT, "default", lang));
        }
    }

    @Test
    public void testDeafMultilingualNavigationAndEmergencyTexts() {
        // Kannada
        assertEquals("ಮುಖಪುಟ ತೆರೆಯಲಾಗಿದೆ", VoiceLanguageConfig.getDeafHomeOpenedText("kn"));
        assertEquals("ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಲಾಗಿದೆ", VoiceLanguageConfig.getDeafSettingsOpenedText("kn"));
        assertEquals("ಕೇರ್ಗಿವರ್ ತೆರೆಯಲಾಗಿದೆ", VoiceLanguageConfig.getDeafCaregiverOpenedText("kn"));
        assertEquals("ತುರ್ತು ಎಚ್ಚರಿಕೆ ಕಳುಹಿಸಲಾಗಿದೆ", VoiceLanguageConfig.getDeafEmergencyAlertSentText("kn"));

        // Hindi
        assertEquals("होम खोला गया", VoiceLanguageConfig.getDeafHomeOpenedText("hi"));
        assertEquals("सेटिंग्स खोली गई", VoiceLanguageConfig.getDeafSettingsOpenedText("hi"));
        assertEquals("केयरगिवर खोला गया", VoiceLanguageConfig.getDeafCaregiverOpenedText("hi"));
        assertEquals("आपातकालीन अलर्ट भेजा गया", VoiceLanguageConfig.getDeafEmergencyAlertSentText("hi"));

        // Malayalam
        assertEquals("ഹോം തുറന്നു", VoiceLanguageConfig.getDeafHomeOpenedText("ml"));
        assertEquals("സെറ്റിംഗ്സ് തുറന്നു", VoiceLanguageConfig.getDeafSettingsOpenedText("ml"));
        assertEquals("കെയർഗിവർ തുറന്നു", VoiceLanguageConfig.getDeafCaregiverOpenedText("ml"));
        assertEquals("അടിയന്തര മുന്നറിയിപ്പ് അയച്ചു", VoiceLanguageConfig.getDeafEmergencyAlertSentText("ml"));

        // English
        assertEquals("Home opened", VoiceLanguageConfig.getDeafHomeOpenedText("en"));
        assertEquals("Settings opened", VoiceLanguageConfig.getDeafSettingsOpenedText("en"));
        assertEquals("Caregiver opened", VoiceLanguageConfig.getDeafCaregiverOpenedText("en"));
        assertEquals("Emergency alert sent", VoiceLanguageConfig.getDeafEmergencyAlertSentText("en"));
    }

    @Test
    public void testDeafVoiceIntentMatching() {
        // English
        assertEquals(VoiceIntentType.OPEN_HOME, VoiceIntentMatcher.match("open home"));
        assertEquals(VoiceIntentType.OPEN_PROFILE, VoiceIntentMatcher.match("open profile"));
        assertEquals(VoiceIntentType.OPEN_EMERGENCY_ALERT, VoiceIntentMatcher.match("emergency alert"));
        assertEquals(VoiceIntentType.OPEN_EMERGENCY, VoiceIntentMatcher.match("open emergency"));

        // Kannada
        assertEquals(VoiceIntentType.OPEN_HOME, VoiceIntentMatcher.matchIntent("ಮನೆ ತೆರೆ", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_EMERGENCY_ALERT, VoiceIntentMatcher.matchIntent("ತುರ್ತು ಎಚ್ಚರಿಕೆ", "kn").getIntentType());
    }

    @Test
    public void testDeleteLastMessageRoleAuthorizationForDeafUser() {
        VoiceCommandProcessor processor = new VoiceCommandProcessor();
        VoiceIntent deleteIntent = new VoiceIntent(VoiceIntentType.DELETE_LAST_MESSAGE, "delete last message");

        VoiceCommand deafCmd = processor.resolveCommand(deleteIntent, RoleManager.ROLE_DEAF_USER);
        assertTrue("Deaf User must be authorized for Delete Message", deafCmd.isRoleAuthorized(RoleManager.ROLE_DEAF_USER));
        assertTrue("Speech-Impaired User must be authorized for Delete Message", deafCmd.isRoleAuthorized(RoleManager.ROLE_SPEECH_IMPAIRED));
        assertTrue("Deaf alias must be authorized", deafCmd.isRoleAuthorized("Deaf"));

        // Caregiver and Mute User must NOT be authorized for Delete Last Message
        assertFalse("Caregiver must NOT be authorized for Delete Message", deafCmd.isRoleAuthorized(RoleManager.ROLE_ADMIN_CAREGIVER));
        assertFalse("Mute User must NOT be authorized for Delete Message", deafCmd.isRoleAuthorized(RoleManager.ROLE_MUTE_USER));
    }

    @Test
    public void testDeafUserEligibleOutgoingMessageFiltering() {
        String deafUid = "deaf_user_123";
        String caregiverUid = "caregiver_999";

        // 1. Valid outgoing text message sent by current deaf user -> ELIGIBLE
        Map<String, Object> validOutgoing = new HashMap<>();
        validOutgoing.put("messageId", "msg_001");
        validOutgoing.put("senderId", deafUid);
        validOutgoing.put("messageText", "Hello Caregiver");
        validOutgoing.put("type", "text");
        ChatMessage validMsg = new ChatMessage(validOutgoing);
        assertTrue("Outgoing deaf user message must be eligible",
                SpeechImpairedDashboardActivity.isEligibleOutgoingDeafUserMessage(validMsg, deafUid));

        // 2. Incoming caregiver message -> STRICTLY INELIGIBLE (Never delete incoming message)
        Map<String, Object> incomingCaregiver = new HashMap<>();
        incomingCaregiver.put("messageId", "msg_002");
        incomingCaregiver.put("senderId", caregiverUid);
        incomingCaregiver.put("messageText", "Take your medicine");
        incomingCaregiver.put("type", "text");
        ChatMessage incomingMsg = new ChatMessage(incomingCaregiver);
        assertFalse("Incoming caregiver message must NOT be eligible for deletion",
                SpeechImpairedDashboardActivity.isEligibleOutgoingDeafUserMessage(incomingMsg, deafUid));

        // 3. Emergency SOS message -> INELIGIBLE (SOS records must never be deleted)
        Map<String, Object> sosMessage = new HashMap<>();
        sosMessage.put("messageId", "msg_003");
        sosMessage.put("senderId", deafUid);
        sosMessage.put("messageText", "EMERGENCY SOS");
        sosMessage.put("type", "emergency");
        ChatMessage sosMsg = new ChatMessage(sosMessage);
        assertFalse("Emergency message must NOT be eligible for deletion",
                SpeechImpairedDashboardActivity.isEligibleOutgoingDeafUserMessage(sosMsg, deafUid));

        // 4. Call record -> INELIGIBLE
        Map<String, Object> callMessage = new HashMap<>();
        callMessage.put("messageId", "msg_004");
        callMessage.put("senderId", deafUid);
        callMessage.put("messageText", "Call ended");
        callMessage.put("type", "call");
        ChatMessage callMsg = new ChatMessage(callMessage);
        assertFalse("Call record must NOT be eligible for deletion",
                SpeechImpairedDashboardActivity.isEligibleOutgoingDeafUserMessage(callMsg, deafUid));

        // 5. Empty / null messages -> INELIGIBLE
        assertFalse("Null message must not be eligible",
                SpeechImpairedDashboardActivity.isEligibleOutgoingDeafUserMessage(null, deafUid));
        assertFalse("Null UID must not be eligible",
                SpeechImpairedDashboardActivity.isEligibleOutgoingDeafUserMessage(validMsg, null));
    }

    @Test
    public void testDeafUserVoiceCommandsForDeleteMessage() {
        String[] variations = {
                "Hey Assistant, delete last message",
                "Delete my last message.",
                "Assistant, delete my last message.",
                "delete last message"
        };

        // English commands must match across all 4 language configurations
        String[] languages = {"en", "kn", "hi", "ml"};
        for (String lang : languages) {
            for (String var : variations) {
                VoiceIntent intent = VoiceIntentMatcher.matchIntent(var, lang);
                assertEquals("Voice command variation '" + var + "' must match DELETE_LAST_MESSAGE in " + lang,
                        VoiceIntentType.DELETE_LAST_MESSAGE, intent.getIntentType());
            }
        }

        // Multilingual regional commands
        // Kannada
        VoiceIntent knIntent = VoiceIntentMatcher.matchIntent("ಕೊನೆಯ ಸಂದೇಶವನ್ನು ಅಳಿಸು", "kn");
        assertEquals(VoiceIntentType.DELETE_LAST_MESSAGE, knIntent.getIntentType());

        // Hindi
        VoiceIntent hiIntent = VoiceIntentMatcher.matchIntent("आखिरी संदेश हटाओ", "hi");
        assertEquals(VoiceIntentType.DELETE_LAST_MESSAGE, hiIntent.getIntentType());

        // Malayalam
        VoiceIntent mlIntent = VoiceIntentMatcher.matchIntent("അവസാന സന്ദേശം ഇല്ലാതാക്കൂ", "ml");
        assertEquals(VoiceIntentType.DELETE_LAST_MESSAGE, mlIntent.getIntentType());

        // Language isolation: Kannada command in Hindi mode must NOT trigger deletion
        VoiceIntent crossLang = VoiceIntentMatcher.matchIntent("ಕೊನೆಯ ಸಂದೇಶವನ್ನು ಅಳಿಸು", "hi");
        assertFalse("Kannada delete command must not trigger deletion in Hindi mode",
                crossLang.getIntentType() == VoiceIntentType.DELETE_LAST_MESSAGE);
    }

    @Test
    public void testDeafUserTouchDeleteButtonLayoutIntegrity() throws Exception {
        // 1. Verify item_caregiver_chat.xml
        File caregiverChatLayout = new File("src/main/res/layout/item_caregiver_chat.xml");
        assertTrue("item_caregiver_chat.xml must exist", caregiverChatLayout.exists());
        String caregiverContent = new String(Files.readAllBytes(caregiverChatLayout.toPath()));
        assertTrue("Must contain btnDeleteRightMessage in item_caregiver_chat.xml",
                caregiverContent.contains("android:id=\"@+id/btnDeleteRightMessage\""));

        // Must be in layoutRight, not layoutLeft
        int rightIndex = caregiverContent.indexOf("id=\"@+id/layoutRight\"");
        int btnIndex = caregiverContent.indexOf("id=\"@+id/btnDeleteRightMessage\"");
        assertTrue("btnDeleteRightMessage must appear inside or after layoutRight definition",
                btnIndex > rightIndex);

        // 2. Verify item_message.xml
        File messageLayout = new File("src/main/res/layout/item_message.xml");
        assertTrue("item_message.xml must exist", messageLayout.exists());
        String messageContent = new String(Files.readAllBytes(messageLayout.toPath()));
        assertTrue("Must contain btnDeleteRightMessage in item_message.xml",
                messageContent.contains("android:id=\"@+id/btnDeleteRightMessage\""));
    }
}
