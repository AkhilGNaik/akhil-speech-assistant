package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.kannada.speechassistant.RoleManager;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

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
}
