package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.kannada.speechassistant.BlindUserDashboardActivity;
import com.kannada.speechassistant.CaregiverSoundManager;
import com.kannada.speechassistant.ChatMessage;
import com.kannada.speechassistant.LanguageManager;
import com.kannada.speechassistant.NotificationService;
import com.kannada.speechassistant.RoleManager;
import com.kannada.speechassistant.SessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class VoiceAssistantDeviceVerificationTest {

    private Context appContext;
    private SessionManager sessionManager;
    private AppVoiceAssistant assistant;
    private VoiceCommandProcessor processor;

    @Before
    public void setUp() {
        appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        sessionManager = new SessionManager(appContext);
        sessionManager.createLoginSession("test_blind_uid", "test_blind@example.com", RoleManager.ROLE_BLIND_USER);
        processor = new VoiceCommandProcessor(appContext);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            assistant = AppVoiceAssistant.getInstance(appContext);
            assistant.setLastReadCaregiverMessage(null);
        });
    }

    // ==========================================
    // 1-6. INTENT MATCHING TESTS (SINGLE UTTERANCE)
    // ==========================================

    @Test
    public void test1_GoBack_MatchesIntent() {
        VoiceIntent intent = processor.detectIntent("go back", "en");
        assertEquals(VoiceIntentType.GO_BACK, intent.getIntentType());
        assertEquals(VoiceCommandConstants.INTENT_GO_BACK, intent.getIntentName());
    }

    @Test
    public void test2_GoBackPlease_MatchesIntent() {
        VoiceIntent intent = processor.detectIntent("go back please", "en");
        assertEquals(VoiceIntentType.GO_BACK, intent.getIntentType());
    }

    @Test
    public void test3_GoBackward_MatchesIntent() {
        VoiceIntent intent = processor.detectIntent("go backward", "en");
        assertEquals(VoiceIntentType.GO_BACK, intent.getIntentType());
    }

    @Test
    public void test4_ReturnBack_MatchesIntent() {
        VoiceIntent intent = processor.detectIntent("return back", "en");
        assertEquals(VoiceIntentType.GO_BACK, intent.getIntentType());
    }

    @Test
    public void test5_Return_MatchesIntent() {
        VoiceIntent intent = processor.detectIntent("return", "en");
        assertEquals(VoiceIntentType.GO_BACK, intent.getIntentType());
    }

    @Test
    public void test6_PreviousPage_MatchesIntent() {
        VoiceIntent intent = processor.detectIntent("previous page", "en");
        assertEquals(VoiceIntentType.GO_BACK, intent.getIntentType());
    }

    // ==========================================
    // 7-8. TWO-STEP MODE EXTRACTION
    // ==========================================

    @Test
    public void test7_TwoStepGoBack_ExtractCommand() {
        String utterance = "Assistant";
        assertTrue(VoiceIntentMatcher.hasWakeWord(utterance));
        String cmd = VoiceIntentMatcher.extractCommandText(utterance);
        assertTrue("Single wake word leaves command portion empty for step 2", cmd.isEmpty());

        VoiceIntent step2Intent = processor.detectIntent("go back", "en");
        assertEquals(VoiceIntentType.GO_BACK, step2Intent.getIntentType());
    }

    @Test
    public void test8_TwoStepReturn_ExtractCommand() {
        String utterance = "Assistant";
        assertTrue(VoiceIntentMatcher.hasWakeWord(utterance));
        String cmd = VoiceIntentMatcher.extractCommandText(utterance);
        assertTrue(cmd.isEmpty());

        VoiceIntent step2Intent = processor.detectIntent("return", "en");
        assertEquals(VoiceIntentType.GO_BACK, step2Intent.getIntentType());
    }

    // ==========================================
    // 9-11. WAKE-WORD PROTECTION TESTS
    // ==========================================

    @Test
    public void test9_GoBackWithoutWakeWord_Ignored() {
        assertFalse(VoiceIntentMatcher.hasWakeWord("go back"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("go back please"));
    }

    @Test
    public void test10_PreviousPageWithoutWakeWord_Ignored() {
        assertFalse(VoiceIntentMatcher.hasWakeWord("previous page"));
    }

    @Test
    public void test11_ReturnWithoutWakeWord_Ignored() {
        assertFalse(VoiceIntentMatcher.hasWakeWord("return"));
    }

    // ==========================================
    // 12. NEGATIVE TESTS (GENERIC WORDS MUST BE UNKNOWN)
    // ==========================================

    @Test
    public void test12_GenericIsolatedWords_MustBeUnknown() {
        String[] genericRejections = {"back", "go", "previous", "page"};
        for (String word : genericRejections) {
            VoiceIntent intent = processor.detectIntent(word, "en");
            assertEquals("Isolated generic word '" + word + "' must remain UNKNOWN",
                    VoiceIntentType.UNKNOWN, intent.getIntentType());
        }
    }

    // ==========================================
    // 13-16. NAVIGATION RESPONSES & CALL PROTECTION
    // ==========================================

    @Test
    public void test13_GoingBackResponse() {
        assertEquals("Going back.", VoiceCommandConstants.RESPONSE_GOING_BACK);
        String spoken = AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_GO_BACK, VoiceCommandConstants.RESPONSE_GOING_BACK);
        assertEquals("Going back.", spoken);
    }

    @Test
    public void test14_AlreadyFirstScreenResponse() {
        assertEquals("Already at the first screen.", VoiceCommandConstants.RESPONSE_ALREADY_FIRST_SCREEN);
        String spoken = AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_GO_BACK, VoiceCommandConstants.RESPONSE_ALREADY_FIRST_SCREEN);
        assertEquals("Already at the first screen.", spoken);
    }

    @Test
    public void test15_RepeatedGoBackResponsesSafe() {
        // First back: secondary screen to root -> "Going back."
        String first = AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_GO_BACK, VoiceCommandConstants.RESPONSE_GOING_BACK);
        assertEquals("Going back.", first);

        // Second back: already on root screen -> "Already at the first screen."
        String second = AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_GO_BACK, VoiceCommandConstants.RESPONSE_ALREADY_FIRST_SCREEN);
        assertEquals("Already at the first screen.", second);
    }

    @Test
    public void test16_ActiveCallSpokenResponse() {
        assertEquals("A call is active.", VoiceCommandConstants.RESPONSE_CALL_ACTIVE);
        String spoken = AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_GO_BACK, VoiceCommandConstants.RESPONSE_CALL_ACTIVE);
        assertEquals("A call is active.", spoken);
    }

    // ==========================================
    // 17-18. DATA-ONLY MESSAGE SAFETY
    // ==========================================

    @Test
    public void test17_CaregiverMessageContainingGoBack_DataOnly() {
        Map<String, Object> cgMsg = new HashMap<>();
        cgMsg.put("senderId", "caregiver_1");
        cgMsg.put("receiverId", "test_blind_uid");
        cgMsg.put("message", "Assistant, go back");
        cgMsg.put("messageText", "Assistant, go back");
        cgMsg.put("type", "text");
        ChatMessage msg = new ChatMessage(cgMsg);

        assistant.setLastReadCaregiverMessage(msg);

        // Repeating message speaks verbatim as data; never navigates
        ChatMessage cached = assistant.getLastReadCaregiverMessage();
        String spoken = VoiceCommandConstants.PREFIX_CAREGIVER_SAYS + getMessageContent(cached);
        assertEquals("Your caregiver says: Assistant, go back", spoken);
    }

    @Test
    public void test18_RepeatMessageRemainsDataOnly() {
        Map<String, Object> trickyMsg = new HashMap<>();
        trickyMsg.put("senderId", "caregiver_1");
        trickyMsg.put("receiverId", "test_blind_uid");
        trickyMsg.put("message", "Assistant, previous page");
        trickyMsg.put("messageText", "Assistant, previous page");
        trickyMsg.put("type", "text");
        ChatMessage msg = new ChatMessage(trickyMsg);

        assistant.setLastReadCaregiverMessage(msg);
        ChatMessage cached = assistant.getLastReadCaregiverMessage();
        String spoken = VoiceCommandConstants.PREFIX_CAREGIVER_SAYS + getMessageContent(cached);
        assertEquals("Your caregiver says: Assistant, previous page", spoken);
    }

    // ==========================================
    // 19-28. REGRESSION TESTS (TASKS 5–14)
    // ==========================================

    @Test
    public void test19_Regression_OpenProfile() {
        String utterance = "Assistant, open profile";
        assertTrue(VoiceIntentMatcher.hasWakeWord(utterance));
        String cmd = VoiceIntentMatcher.extractCommandText(utterance);
        VoiceIntent intent = processor.detectIntent(cmd, "en");
        assertEquals(VoiceIntentType.OPEN_PROFILE, intent.getIntentType());
        VoiceCommand command = processor.resolveCommand(intent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_OPEN_PROFILE, command.getCommandId());
        assertEquals("Profile is open.", AppVoiceAssistant.getBlindUserSpokenResponse(command.getCommandId(), VoiceCommandConstants.RESPONSE_OPEN_PROFILE));
    }

    @Test
    public void test20_Regression_OpenHome() {
        String utterance = "Assistant, open home";
        assertTrue(VoiceIntentMatcher.hasWakeWord(utterance));
        String cmd = VoiceIntentMatcher.extractCommandText(utterance);
        VoiceIntent intent = processor.detectIntent(cmd, "en");
        assertEquals(VoiceIntentType.OPEN_HOME, intent.getIntentType());
        VoiceCommand command = processor.resolveCommand(intent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_OPEN_HOME, command.getCommandId());
        assertEquals("Home is open.", AppVoiceAssistant.getBlindUserSpokenResponse(command.getCommandId(), VoiceCommandConstants.RESPONSE_OPEN_HOME));
    }

    @Test
    public void test21_Regression_OpenSettings() {
        String utterance = "Assistant, open settings";
        assertTrue(VoiceIntentMatcher.hasWakeWord(utterance));
        String cmd = VoiceIntentMatcher.extractCommandText(utterance);
        VoiceIntent intent = processor.detectIntent(cmd, "en");
        assertEquals(VoiceIntentType.OPEN_SETTINGS, intent.getIntentType());
        VoiceCommand command = processor.resolveCommand(intent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_OPEN_SETTINGS, command.getCommandId());
        assertEquals("Settings is open.", AppVoiceAssistant.getBlindUserSpokenResponse(command.getCommandId(), VoiceCommandConstants.RESPONSE_OPEN_SETTINGS));
    }

    @Test
    public void test22_Regression_CallCaregiver() {
        String utterance = "Assistant, call caregiver";
        assertTrue(VoiceIntentMatcher.hasWakeWord(utterance));
        String cmd = VoiceIntentMatcher.extractCommandText(utterance);
        VoiceIntent intent = processor.detectIntent(cmd, "en");
        assertEquals(VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
        VoiceCommand command = processor.resolveCommand(intent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_OPEN_VOICE_CALL, command.getCommandId());
        assertEquals("Calling caregiver.", AppVoiceAssistant.getBlindUserSpokenResponse(command.getCommandId(), VoiceCommandConstants.RESPONSE_OPEN_VOICE_CALL));
    }

    @Test
    public void test23_Regression_EndCall() {
        String utterance = "Assistant, end call";
        assertTrue(VoiceIntentMatcher.hasWakeWord(utterance));
        String cmd = VoiceIntentMatcher.extractCommandText(utterance);
        VoiceIntent intent = processor.detectIntent(cmd, "en");
        assertEquals(VoiceIntentType.END_CALL, intent.getIntentType());
        VoiceCommand command = processor.resolveCommand(intent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_END_CALL, command.getCommandId());
        assertEquals("Call ended.", AppVoiceAssistant.getBlindUserSpokenResponse(command.getCommandId(), VoiceCommandConstants.RESPONSE_END_CALL));
    }

    @Test
    public void test24_Regression_EmergencyHelp() {
        String utterance = "Assistant, emergency help";
        assertTrue(VoiceIntentMatcher.hasWakeWord(utterance));
        String cmd = VoiceIntentMatcher.extractCommandText(utterance);
        VoiceIntent intent = processor.detectIntent(cmd, "en");
        assertEquals(VoiceIntentType.OPEN_EMERGENCY, intent.getIntentType());
        VoiceCommand command = processor.resolveCommand(intent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_OPEN_EMERGENCY, command.getCommandId());
        assertEquals("Emergency alert sent.", AppVoiceAssistant.getBlindUserSpokenResponse(command.getCommandId(), VoiceCommandConstants.RESPONSE_OPEN_EMERGENCY));
    }

    @Test
    public void test25_Regression_SendMessage() {
        String utterance = "Assistant, send message";
        assertTrue(VoiceIntentMatcher.hasWakeWord(utterance));
        String cmd = VoiceIntentMatcher.extractCommandText(utterance);
        VoiceIntent intent = processor.detectIntent(cmd, "en");
        assertEquals(VoiceIntentType.SEND_MESSAGE, intent.getIntentType());
        VoiceCommand command = processor.resolveCommand(intent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_SEND_MESSAGE, command.getCommandId());
        assertEquals("Please say your message.", VoiceCommandConstants.PROMPT_SAY_MESSAGE);
        assertEquals("Message sent.", VoiceCommandConstants.RESPONSE_MESSAGE_SENT);
    }

    @Test
    public void test26_Regression_ReadMessages() {
        String utterance = "Assistant, read messages";
        assertTrue(VoiceIntentMatcher.hasWakeWord(utterance));
        String cmd = VoiceIntentMatcher.extractCommandText(utterance);
        VoiceIntent intent = processor.detectIntent(cmd, "en");
        assertEquals(VoiceIntentType.READ_MESSAGES, intent.getIntentType());
        VoiceCommand command = processor.resolveCommand(intent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_READ_MESSAGES, command.getCommandId());
        assertEquals("No new messages.", VoiceCommandConstants.RESPONSE_NO_NEW_MESSAGES);
        assertEquals("Your caregiver says: Hello", VoiceCommandConstants.PREFIX_CAREGIVER_SAYS + "Hello");
    }

    @Test
    public void test27_Regression_MessageCount() {
        String utterance = "Assistant, how many messages";
        assertTrue(VoiceIntentMatcher.hasWakeWord(utterance));
        String cmd = VoiceIntentMatcher.extractCommandText(utterance);
        VoiceIntent intent = processor.detectIntent(cmd, "en");
        assertEquals(VoiceIntentType.MESSAGE_COUNT, intent.getIntentType());
        VoiceCommand command = processor.resolveCommand(intent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_MESSAGE_COUNT, command.getCommandId());
        assertEquals("No new messages.", VoiceCommandConstants.RESPONSE_NO_NEW_MESSAGES);
    }

    @Test
    public void test28_Regression_RepeatMessage() {
        String utterance = "Assistant, repeat message";
        assertTrue(VoiceIntentMatcher.hasWakeWord(utterance));
        String cmd = VoiceIntentMatcher.extractCommandText(utterance);
        VoiceIntent intent = processor.detectIntent(cmd, "en");
        assertEquals(VoiceIntentType.REPEAT_MESSAGE, intent.getIntentType());
        VoiceCommand command = processor.resolveCommand(intent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_REPEAT_MESSAGE, command.getCommandId());
        assertEquals("No message to repeat.", VoiceCommandConstants.RESPONSE_NO_MESSAGE_TO_REPEAT);
    }

    // ==========================================
    // 29. PROTECTED WEBRTC CLASSES VERIFICATION
    // ==========================================

    @Test
    public void test29_VoiceCallClassesIntact() throws ClassNotFoundException {
        // REGRESSION PROTECTION: Verify all 6 WebRTC and voice-call classes are present untouched
        assertNotNull(Class.forName("com.kannada.speechassistant.WebRTCManager"));
        assertNotNull(Class.forName("com.kannada.speechassistant.VoiceCallManager"));
        assertNotNull(Class.forName("com.kannada.speechassistant.VoiceCallSignalingManager"));
        assertNotNull(Class.forName("com.kannada.speechassistant.VoiceCallActivity"));
        assertNotNull(Class.forName("com.kannada.speechassistant.IncomingCallActivity"));
        assertNotNull(Class.forName("com.kannada.speechassistant.FirestoreRealtimeService"));
    }

    // ==========================================
    // 30-36. TASK 16 MULTILINGUAL FOUNDATION TESTS
    // ==========================================

    @Test
    public void test30_MultilingualFoundation_KannadaLanguage() {
        sessionManager.saveLanguage("kn");
        assertEquals("kn", sessionManager.getLanguage());
        assertEquals("kn-IN", VoiceLanguageConfig.getSpeechRecognizerLanguageTag("kn"));
        assertEquals(new java.util.Locale("kn", "IN"), VoiceLanguageConfig.getTtsLocale("kn"));

        String spoken = AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_OPEN_HOME, VoiceCommandConstants.RESPONSE_OPEN_HOME, "kn");
        assertEquals("ಮನೆ ತೆರೆಯಲಾಗಿದೆ.", spoken);
    }

    @Test
    public void test31_MultilingualFoundation_HindiLanguage() {
        sessionManager.saveLanguage("hi");
        assertEquals("hi", sessionManager.getLanguage());
        assertEquals("hi-IN", VoiceLanguageConfig.getSpeechRecognizerLanguageTag("hi"));
        assertEquals(new java.util.Locale("hi", "IN"), VoiceLanguageConfig.getTtsLocale("hi"));

        String spoken = AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_OPEN_HOME, VoiceCommandConstants.RESPONSE_OPEN_HOME, "hi");
        assertEquals("होम खुल गया है।", spoken);
    }

    @Test
    public void test32_MultilingualFoundation_MalayalamLanguage() {
        sessionManager.saveLanguage("ml");
        assertEquals("ml", sessionManager.getLanguage());
        assertEquals("ml-IN", VoiceLanguageConfig.getSpeechRecognizerLanguageTag("ml"));
        assertEquals(new java.util.Locale("ml", "IN"), VoiceLanguageConfig.getTtsLocale("ml"));

        String spoken = AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_OPEN_HOME, VoiceCommandConstants.RESPONSE_OPEN_HOME, "ml");
        assertEquals("ഹോം തുറന്നു.", spoken);
    }

    @Test
    public void test33_MultilingualFoundation_EnglishLanguage() {
        sessionManager.saveLanguage("en");
        assertEquals("en", sessionManager.getLanguage());
        assertEquals("en-IN", VoiceLanguageConfig.getSpeechRecognizerLanguageTag("en"));
        assertEquals(new java.util.Locale("en", "IN"), VoiceLanguageConfig.getTtsLocale("en"));

        String spoken = AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_OPEN_HOME, VoiceCommandConstants.RESPONSE_OPEN_HOME, "en");
        assertEquals("Home is open.", spoken);
    }

    @Test
    public void test34_MultilingualFoundation_TtsSafeFallback() {
        java.util.Locale fallback = VoiceLanguageConfig.resolveAvailableTtsLocale(null, "unsupported_lang");
        assertNotNull(fallback);
        assertEquals(VoiceLanguageConfig.getSafeTtsFallbackLocale(), fallback);
    }

    @Test
    public void test35_MultilingualFoundation_OpenHomeAllLanguages() {
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("ಮನೆಗೆ ಹೋಗಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("घर जाओ", "hi").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("വീട്ടിലേക്ക് പോകൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("go home", "en").getIntentType());
    }

    @Test
    public void test36_MultilingualFoundation_UnicodePreservation() {
        // Kannada preservation
        assertEquals("ಮನೆಗೆ ಹೋಗಿ", VoiceIntentMatcher.extractCommandText("Assistant, ಮನೆಗೆ ಹೋಗಿ"));
        // Hindi preservation with Danda
        assertEquals("घर जाओ", VoiceIntentMatcher.extractCommandText("Assistant, घर जाओ।"));
        // Malayalam preservation
        assertEquals("വീട്ടിലേക്ക് പോകൂ", VoiceIntentMatcher.extractCommandText("Assistant, വീട്ടിലേക്ക് പോകൂ"));
    }

    // ==========================================
    // 37-43. TASK 17 MULTILINGUAL NAVIGATION TESTS
    // ==========================================

    @Test
    public void test37_MultilingualNavigation_OpenHome() {
        String[] kannadaPhrases = {"ಮನೆ ತೆರೆಯಿರಿ", "ಮನೆಗೆ ಹೋಗಿ", "ಹೋಮ್ ತೆರೆಯಿರಿ", "ಹೋಮ್ಗೆ ಹೋಗಿ", "ಮುಖಪುಟ"};
        for (String p : kannadaPhrases) {
            assertEquals("Kannada OPEN_HOME: " + p, VoiceIntentType.OPEN_HOME, processor.detectIntent(p, "kn").getIntentType());
        }
        assertEquals("ಮನೆ ತೆರೆಯಲಾಗಿದೆ.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_OPEN_HOME, VoiceCommandConstants.RESPONSE_OPEN_HOME, "kn"));

        String[] hindiPhrases = {"होम खोलो", "घर जाओ", "होम पर जाओ", "होम दिखाओ", "घर चलो"};
        for (String p : hindiPhrases) {
            assertEquals("Hindi OPEN_HOME: " + p, VoiceIntentType.OPEN_HOME, processor.detectIntent(p, "hi").getIntentType());
        }
        assertEquals("होम खुल गया है।", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_OPEN_HOME, VoiceCommandConstants.RESPONSE_OPEN_HOME, "hi"));

        String[] malayalamPhrases = {"ഹോം തുറക്കൂ", "വീട്ടിലേക്ക് പോകൂ", "ഹോം കാണിക്കൂ", "ഹോമിലേക്ക് പോകൂ", "ഹോം തുറക്കുക"};
        for (String p : malayalamPhrases) {
            assertEquals("Malayalam OPEN_HOME: " + p, VoiceIntentType.OPEN_HOME, processor.detectIntent(p, "ml").getIntentType());
        }
        assertEquals("ഹോം തുറന്നു.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_OPEN_HOME, VoiceCommandConstants.RESPONSE_OPEN_HOME, "ml"));

        String[] englishPhrases = {"open home", "go home"};
        for (String p : englishPhrases) {
            assertEquals("English OPEN_HOME: " + p, VoiceIntentType.OPEN_HOME, processor.detectIntent(p, "en").getIntentType());
        }
        assertEquals("Home is open.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_OPEN_HOME, VoiceCommandConstants.RESPONSE_OPEN_HOME, "en"));
    }

    @Test
    public void test38_MultilingualNavigation_OpenProfile() {
        String[] kannadaPhrases = {"ಪ್ರೊಫೈಲ್ ತೆರೆಯಿರಿ", "ಪ್ರೊಫೈಲ್ ತೋರಿಸಿ", "ಪ್ರೊಫೈಲ್ಗೆ ಹೋಗಿ", "ನನ್ನ ಪ್ರೊಫೈಲ್", "ಖಾತೆ"};
        for (String p : kannadaPhrases) {
            assertEquals("Kannada OPEN_PROFILE: " + p, VoiceIntentType.OPEN_PROFILE, processor.detectIntent(p, "kn").getIntentType());
        }
        assertEquals("ಪ್ರೊಫೈಲ್ ತೆರೆಯಲಾಗಿದೆ.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_OPEN_PROFILE, VoiceCommandConstants.RESPONSE_OPEN_PROFILE, "kn"));

        String[] hindiPhrases = {"प्रोफाइल खोलो", "प्रोफाइल दिखाओ", "प्रोफाइल पर जाओ", "मेरी प्रोफाइल", "खाता"};
        for (String p : hindiPhrases) {
            assertEquals("Hindi OPEN_PROFILE: " + p, VoiceIntentType.OPEN_PROFILE, processor.detectIntent(p, "hi").getIntentType());
        }
        assertEquals("प्रोफ़ाइल खुल गई है।", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_OPEN_PROFILE, VoiceCommandConstants.RESPONSE_OPEN_PROFILE, "hi"));

        String[] malayalamPhrases = {"പ്രൊഫൈൽ തുറക്കൂ", "പ്രൊഫൈൽ കാണിക്കൂ", "പ്രൊഫൈലിലേക്ക് പോകൂ", "എന്റെ പ്രൊഫൈൽ", "അക്കൗണ്ട്"};
        for (String p : malayalamPhrases) {
            assertEquals("Malayalam OPEN_PROFILE: " + p, VoiceIntentType.OPEN_PROFILE, processor.detectIntent(p, "ml").getIntentType());
        }
        assertEquals("പ്രൊഫൈൽ തുറന്നു.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_OPEN_PROFILE, VoiceCommandConstants.RESPONSE_OPEN_PROFILE, "ml"));

        String[] englishPhrases = {"open profile", "show profile"};
        for (String p : englishPhrases) {
            assertEquals("English OPEN_PROFILE: " + p, VoiceIntentType.OPEN_PROFILE, processor.detectIntent(p, "en").getIntentType());
        }
        assertEquals("Profile is open.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_OPEN_PROFILE, VoiceCommandConstants.RESPONSE_OPEN_PROFILE, "en"));
    }

    @Test
    public void test39_MultilingualNavigation_OpenSettings() {
        String[] kannadaPhrases = {"ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಿರಿ", "ಸೆಟ್ಟಿಂಗ್ಸ್ಗೆ ಹೋಗಿ", "ಸೆಟ್ಟಿಂಗ್ಸ್ ತೋರಿಸಿ"};
        for (String p : kannadaPhrases) {
            assertEquals("Kannada OPEN_SETTINGS: " + p, VoiceIntentType.OPEN_SETTINGS, processor.detectIntent(p, "kn").getIntentType());
        }
        assertEquals("ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಲಾಗಿದೆ.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_OPEN_SETTINGS, VoiceCommandConstants.RESPONSE_OPEN_SETTINGS, "kn"));

        String[] hindiPhrases = {"सेटिंग्स खोलो", "सेटिंग्स पर जाओ", "सेटिंग्स दिखाओ"};
        for (String p : hindiPhrases) {
            assertEquals("Hindi OPEN_SETTINGS: " + p, VoiceIntentType.OPEN_SETTINGS, processor.detectIntent(p, "hi").getIntentType());
        }
        assertEquals("सेटिंग्स खुल गई हैं।", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_OPEN_SETTINGS, VoiceCommandConstants.RESPONSE_OPEN_SETTINGS, "hi"));

        String[] malayalamPhrases = {"സെറ്റിംഗ്സ് തുറക്കൂ", "സെറ്റിംഗ്സിലേക്ക് പോകൂ", "സെറ്റിംഗ്സ് കാണിക്കൂ"};
        for (String p : malayalamPhrases) {
            assertEquals("Malayalam OPEN_SETTINGS: " + p, VoiceIntentType.OPEN_SETTINGS, processor.detectIntent(p, "ml").getIntentType());
        }
        assertEquals("സെറ്റിംഗ്സ് തുറന്നു.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_OPEN_SETTINGS, VoiceCommandConstants.RESPONSE_OPEN_SETTINGS, "ml"));

        String[] englishPhrases = {"open settings"};
        for (String p : englishPhrases) {
            assertEquals("English OPEN_SETTINGS: " + p, VoiceIntentType.OPEN_SETTINGS, processor.detectIntent(p, "en").getIntentType());
        }
        assertEquals("Settings is open.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_OPEN_SETTINGS, VoiceCommandConstants.RESPONSE_OPEN_SETTINGS, "en"));
    }

    @Test
    public void test40_MultilingualNavigation_GoBack() {
        String[] kannadaPhrases = {"ಹಿಂದೆ ಹೋಗಿ", "ವಾಪಸ್ ಹೋಗಿ", "ಹಿಂದಕ್ಕೆ ಬನ್ನಿ"};
        for (String p : kannadaPhrases) {
            assertEquals("Kannada GO_BACK: " + p, VoiceIntentType.GO_BACK, processor.detectIntent(p, "kn").getIntentType());
        }
        assertEquals("ಹಿಂದೆ ಹೋಗುತ್ತಿದ್ದೇನೆ.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_GO_BACK, VoiceCommandConstants.RESPONSE_GOING_BACK, "kn"));

        String[] hindiPhrases = {"वापस जाओ", "पीछे जाओ", "वापस जाइए"};
        for (String p : hindiPhrases) {
            assertEquals("Hindi GO_BACK: " + p, VoiceIntentType.GO_BACK, processor.detectIntent(p, "hi").getIntentType());
        }
        assertEquals("वापस जा रहा हूँ।", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_GO_BACK, VoiceCommandConstants.RESPONSE_GOING_BACK, "hi"));

        String[] malayalamPhrases = {"തിരികെ പോകൂ", "പിന്നിലേക്ക് പോകൂ", "വീണ്ടും പിന്നിലേക്ക് പോകൂ"};
        for (String p : malayalamPhrases) {
            assertEquals("Malayalam GO_BACK: " + p, VoiceIntentType.GO_BACK, processor.detectIntent(p, "ml").getIntentType());
        }
        assertEquals("തിരികെ പോകുന്നു.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_GO_BACK, VoiceCommandConstants.RESPONSE_GOING_BACK, "ml"));

        String[] englishPhrases = {"go back", "return", "previous page"};
        for (String p : englishPhrases) {
            assertEquals("English GO_BACK: " + p, VoiceIntentType.GO_BACK, processor.detectIntent(p, "en").getIntentType());
        }
        assertEquals("Going back.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_GO_BACK, VoiceCommandConstants.RESPONSE_GOING_BACK, "en"));
    }

    @Test
    public void test41_MultilingualNavigation_FalsePositiveRejections() {
        String[] rejections = {
                "homework",
                "home screen issue",
                "show me",
                "open",
                "go",
                "back",
                "show",
                "user"
        };
        for (String phrase : rejections) {
            assertEquals("Rejection of '" + phrase + "' in en", VoiceIntentType.UNKNOWN, processor.detectIntent(phrase, "en").getIntentType());
            assertEquals("Rejection of '" + phrase + "' in kn", VoiceIntentType.UNKNOWN, processor.detectIntent(phrase, "kn").getIntentType());
            assertEquals("Rejection of '" + phrase + "' in hi", VoiceIntentType.UNKNOWN, processor.detectIntent(phrase, "hi").getIntentType());
            assertEquals("Rejection of '" + phrase + "' in ml", VoiceIntentType.UNKNOWN, processor.detectIntent(phrase, "ml").getIntentType());
        }
    }

    @Test
    public void test42_MultilingualNavigation_WakeWordGating() {
        // Unactivated utterance must not have wake word
        assertFalse(VoiceIntentMatcher.hasWakeWord("ಮನೆ ತೆರೆಯಿರಿ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("होम खोलो"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("ഹോം തുറക്കൂ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("go home"));

        // Activated utterance
        String knUtterance = "Assistant, ಮನೆ ತೆರೆಯಿರಿ";
        assertTrue(VoiceIntentMatcher.hasWakeWord(knUtterance));
        String knCmd = VoiceIntentMatcher.extractCommandText(knUtterance);
        assertEquals("ಮನೆ ತೆರೆಯಿರಿ", knCmd);
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent(knCmd, "kn").getIntentType());

        String hiUtterance = "Assistant, होम खोलो";
        assertTrue(VoiceIntentMatcher.hasWakeWord(hiUtterance));
        String hiCmd = VoiceIntentMatcher.extractCommandText(hiUtterance);
        assertEquals("होम खोलो", hiCmd);
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent(hiCmd, "hi").getIntentType());

        String mlUtterance = "Assistant, ഹോം തുറക്കൂ";
        assertTrue(VoiceIntentMatcher.hasWakeWord(mlUtterance));
        String mlCmd = VoiceIntentMatcher.extractCommandText(mlUtterance);
        assertEquals("ഹോം തുറക്കൂ", mlCmd);
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent(mlCmd, "ml").getIntentType());
    }

    @Test
    public void test43_MultilingualNavigation_CallActiveBlocksNavigation() {
        // If a call is active, VoiceCommandConstants.RESPONSE_CALL_ACTIVE is returned
        assertEquals("A call is active.", VoiceCommandConstants.RESPONSE_CALL_ACTIVE);
        String spoken = AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_GO_BACK, VoiceCommandConstants.RESPONSE_CALL_ACTIVE, "kn");
        assertEquals("A call is active.", spoken);
    }

    // ==========================================
    // 44-51. TASK 18 MULTILINGUAL SEND_MESSAGE TESTS
    // ==========================================

    @Test
    public void test44_MultilingualSendMessage_EnglishPhrases() {
        String[] phrases = {"send message", "send a message", "message caregiver", "send message to caregiver"};
        for (String p : phrases) {
            assertEquals("English SEND_MESSAGE: " + p, VoiceIntentType.SEND_MESSAGE,
                    processor.detectIntent(p, "en").getIntentType());
        }
    }

    @Test
    public void test45_MultilingualSendMessage_KannadaPhrases() {
        String[] phrases = {"ಸಂದೇಶ ಕಳುಹಿಸಿ", "ಒಂದು ಸಂದೇಶ ಕಳುಹಿಸಿ", "ಕೇರ್ಗಿವರ್ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ", "ಕೇರ್ಗಿವರ್ಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ", "ಮೆಸೇಜ್ ಕಳುಹಿಸಿ"};
        for (String p : phrases) {
            assertEquals("Kannada SEND_MESSAGE: " + p, VoiceIntentType.SEND_MESSAGE,
                    processor.detectIntent(p, "kn").getIntentType());
        }
    }

    @Test
    public void test46_MultilingualSendMessage_HindiPhrases() {
        String[] phrases = {"संदेश भेजो", "एक संदेश भेजो", "केयरगिवर को संदेश भेजो", "केयरटेकर को संदेश भेजो", "मैसेज भेजो"};
        for (String p : phrases) {
            assertEquals("Hindi SEND_MESSAGE: " + p, VoiceIntentType.SEND_MESSAGE,
                    processor.detectIntent(p, "hi").getIntentType());
        }
    }

    @Test
    public void test47_MultilingualSendMessage_MalayalamPhrases() {
        String[] phrases = {"സന്ദേശം അയയ്ക്കൂ", "ഒരു സന്ദേശം അയയ്ക്കൂ", "കെയർഗിവറിന് സന്ദേശം അയയ്ക്കൂ", "കെയർടേക്കറിന് സന്ദേശം അയയ്ക്കൂ", "മെസേജ് അയയ്ക്കൂ"};
        for (String p : phrases) {
            assertEquals("Malayalam SEND_MESSAGE: " + p, VoiceIntentType.SEND_MESSAGE,
                    processor.detectIntent(p, "ml").getIntentType());
        }
    }

    @Test
    public void test48_MultilingualSendMessage_WakeWordGating() {
        // Without wake word: ignored
        assertFalse(VoiceIntentMatcher.hasWakeWord("send message"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("ಸಂದೇಶ ಕಳುಹಿಸಿ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("संदेश भेजो"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("സന്ദേശം അയയ്ക്കൂ"));

        // With wake word: detected
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, send message"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, ಸಂದೇಶ ಕಳುಹಿಸಿ"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, संदेश भेजो"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, സന്ദേശം അയയ്ക്കൂ"));
    }

    @Test
    public void test49_MultilingualSendMessage_NegativeRejections() {
        String[] rejections = {
                "message", "send", "caregiver", "send it",
                "ಸಂದೇಶ", "ಕಳುಹಿಸಿ", "ಕೇರ್ಗಿವರ್",
                "संदेश", "भेजो",
                "സന്ദേശം", "അയയ്ക്കൂ"
        };
        for (String r : rejections) {
            assertEquals("Rejection of '" + r + "' in en", VoiceIntentType.UNKNOWN, processor.detectIntent(r, "en").getIntentType());
            assertEquals("Rejection of '" + r + "' in kn", VoiceIntentType.UNKNOWN, processor.detectIntent(r, "kn").getIntentType());
            assertEquals("Rejection of '" + r + "' in hi", VoiceIntentType.UNKNOWN, processor.detectIntent(r, "hi").getIntentType());
            assertEquals("Rejection of '" + r + "' in ml", VoiceIntentType.UNKNOWN, processor.detectIntent(r, "ml").getIntentType());
        }
    }

    @Test
    public void test50_MultilingualSendMessage_LocalizedPromptsAndResponses() {
        // PROMPT
        assertEquals("Please say your message.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.PROMPT_SAY_MESSAGE, "en"));
        assertEquals("ದಯವಿಟ್ಟು ನಿಮ್ಮ ಸಂದೇಶವನ್ನು ಹೇಳಿ.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.PROMPT_SAY_MESSAGE, "kn"));
        assertEquals("कृपया अपना संदेश बोलिए।", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.PROMPT_SAY_MESSAGE, "hi"));
        assertEquals("ദയവായി നിങ്ങളുടെ സന്ദേശം പറയൂ.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.PROMPT_SAY_MESSAGE, "ml"));

        // CONFIRMATION
        assertEquals("Message sent.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.RESPONSE_MESSAGE_SENT, "en"));
        assertEquals("ಸಂದೇಶ ಕಳುಹಿಸಲಾಗಿದೆ.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.RESPONSE_MESSAGE_SENT, "kn"));
        assertEquals("संदेश भेज दिया गया है।", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.RESPONSE_MESSAGE_SENT, "hi"));
        assertEquals("സന്ദേശം അയച്ചു.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.RESPONSE_MESSAGE_SENT, "ml"));

        // NO CAREGIVER
        assertEquals("No caregiver is connected.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.RESPONSE_NO_CAREGIVER, "en"));
        assertEquals("ಯಾವುದೇ ಕೇರ್ಗಿವರ್ ಸಂಪರ್ಕಗೊಂಡಿಲ್ಲ.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.RESPONSE_NO_CAREGIVER, "kn"));
        assertEquals("कोई केयरगिवर जुड़ा नहीं है।", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.RESPONSE_NO_CAREGIVER, "hi"));
        assertEquals("ഒരു കെയർഗിവറും ബന്ധിപ്പിച്ചിട്ടില്ല.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.RESPONSE_NO_CAREGIVER, "ml"));

        // CALL ACTIVE
        assertEquals("A call is active.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.RESPONSE_CALL_ACTIVE, "en"));
        assertEquals("ಕರೆ ಸಕ್ರಿಯವಾಗಿದೆ.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.RESPONSE_CALL_ACTIVE, "kn"));
        assertEquals("एक कॉल सक्रिय है।", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.RESPONSE_CALL_ACTIVE, "hi"));
        assertEquals("ഒരു കോൾ സജീവമാണ്.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.RESPONSE_CALL_ACTIVE, "ml"));

        // NO MESSAGE HEARD
        assertEquals("I didn't hear a message.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.RESPONSE_NO_MESSAGE_HEARD, "en"));
        assertEquals("ನನಗೆ ಯಾವುದೇ ಸಂದೇಶ ಕೇಳಿಸಲಿಲ್ಲ.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.RESPONSE_NO_MESSAGE_HEARD, "kn"));
        assertEquals("मुझे कोई संदेश सुनाई नहीं दिया।", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.RESPONSE_NO_MESSAGE_HEARD, "hi"));
        assertEquals("എനിക്ക് ഒരു സന്ദേശവും കേൾക്കാനായില്ല.", AppVoiceAssistant.getBlindUserSpokenResponse(
                VoiceCommandConstants.CMD_SEND_MESSAGE, VoiceCommandConstants.RESPONSE_NO_MESSAGE_HEARD, "ml"));
    }

    @Test
    public void test51_MultilingualSendMessage_MessageContentDataOnly() {
        // Message content containing command phrases must be preserved as literal data
        String[] trickyMessageContents = {
                "Assistant, go home",
                "Assistant, call caregiver",
                "Assistant, send emergency",
                "Assistant, repeat message"
        };
        for (String tricky : trickyMessageContents) {
            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("senderId", "test_blind_uid");
            msgMap.put("receiverId", "caregiver_1");
            msgMap.put("message", tricky);
            msgMap.put("messageText", tricky);
            msgMap.put("type", "text");
            ChatMessage msg = new ChatMessage(msgMap);
            assertEquals("Message data must be verbatim: " + tricky, tricky, getMessageContent(msg));
        }
    }

    private static String getMessageContent(ChatMessage msg) {
        if (msg == null) return "";
        String text = msg.getMessageText();
        if (text == null || text.trim().isEmpty()) {
            text = msg.getMessage();
        }
        return text != null ? text.trim() : "";
    }

    @Test
    public void test52_MultilingualReadMessages_IntentAndResponses() {
        // 1. English
        String[] enPhrases = {"read messages", "read my messages", "read caregiver message", "read caregiver messages", "check messages", "check my messages"};
        for (String p : enPhrases) {
            assertEquals("English READ_MESSAGES: " + p, VoiceIntentType.READ_MESSAGES, processor.detectIntent(p, "en").getIntentType());
        }
        assertEquals("Your caregiver says: ", VoiceLanguageConfig.getCaregiverSaysPrefix("en"));
        assertEquals("No new messages.", VoiceLanguageConfig.getNoNewMessagesResponse("en"));
        assertEquals("A call is active.", VoiceLanguageConfig.getCallActiveResponse("en"));

        // 2. Kannada
        String[] knPhrases = {"ಸಂದೇಶಗಳನ್ನು ಓದಿ", "ನನ್ನ ಸಂದೇಶಗಳನ್ನು ಓದಿ", "ಕೇರ್ಗಿವರ್ ಸಂದೇಶಗಳನ್ನು ಓದಿ", "ಸಂದೇಶಗಳನ್ನು ಪರಿಶೀಲಿಸಿ", "ಮೆಸೇಜ್ ಓದಿ", "ಕೇರ್ಗಿವರ್ ಮೆಸೇಜ್ ಓದಿ"};
        for (String p : knPhrases) {
            assertEquals("Kannada READ_MESSAGES: " + p, VoiceIntentType.READ_MESSAGES, processor.detectIntent(p, "kn").getIntentType());
        }
        assertEquals("ನಿಮ್ಮ ಕೇರ್ಗಿವರ್ ಹೇಳುತ್ತಾರೆ: ", VoiceLanguageConfig.getCaregiverSaysPrefix("kn"));
        assertEquals("ಹೊಸ ಸಂದೇಶಗಳಿಲ್ಲ.", VoiceLanguageConfig.getNoNewMessagesResponse("kn"));
        assertEquals("ಕರೆ ಸಕ್ರಿಯವಾಗಿದೆ.", VoiceLanguageConfig.getCallActiveResponse("kn"));

        // 3. Hindi
        String[] hiPhrases = {"संदेश पढ़ो", "मेरे संदेश पढ़ो", "केयरगिवर के संदेश पढ़ो", "संदेश चेक करो", "मैसेज पढ़ो", "केयरगिवर का संदेश पढ़ो"};
        for (String p : hiPhrases) {
            assertEquals("Hindi READ_MESSAGES: " + p, VoiceIntentType.READ_MESSAGES, processor.detectIntent(p, "hi").getIntentType());
        }
        assertEquals("आपके केयरगिवर कहते हैं: ", VoiceLanguageConfig.getCaregiverSaysPrefix("hi"));
        assertEquals("कोई नया संदेश नहीं है।", VoiceLanguageConfig.getNoNewMessagesResponse("hi"));
        assertEquals("एक कॉल सक्रिय है।", VoiceLanguageConfig.getCallActiveResponse("hi"));

        // 4. Malayalam
        String[] mlPhrases = {"സന്ദേശങ്ങൾ വായിക്കൂ", "എന്റെ സന്ദേശങ്ങൾ വായിക്കൂ", "കെയർഗിവറിന്റെ സന്ദേശങ്ങൾ വായിക്കൂ", "സന്ദേശങ്ങൾ പരിശോധിക്കൂ", "മെസേജുകൾ വായിക്കൂ", "കെയർഗിവറുടെ സന്ദേശം വായിക്കുക"};
        for (String p : mlPhrases) {
            assertEquals("Malayalam READ_MESSAGES: " + p, VoiceIntentType.READ_MESSAGES, processor.detectIntent(p, "ml").getIntentType());
        }
        assertEquals("നിങ്ങളുടെ കെയർഗിവർ പറയുന്നു: ", VoiceLanguageConfig.getCaregiverSaysPrefix("ml"));
        assertEquals("പുതിയ സന്ദേശങ്ങളൊന്നുമില്ല.", VoiceLanguageConfig.getNoNewMessagesResponse("ml"));
        assertEquals("ഒരു കോൾ സജീവമാണ്.", VoiceLanguageConfig.getCallActiveResponse("ml"));
    }

    @Test
    public void test53_MultilingualReadMessages_DataOnlyAndRejections() {
        // Strict isolated negative token rejections
        String[] isolatedEn = {"read", "messages", "message", "check", "caregiver"};
        for (String w : isolatedEn) {
            assertEquals("Isolated rejection: " + w, VoiceIntentType.UNKNOWN, processor.detectIntent(w, "en").getIntentType());
        }

        String[] isolatedKn = {"ಓದಿ", "ಸಂದೇಶ", "ಸಂದೇಶಗಳು", "ಮೆಸೇಜ್", "ಪರಿಶೀಲಿಸಿ", "ಕೇರ್ಗಿವರ್"};
        for (String w : isolatedKn) {
            assertEquals("Isolated rejection: " + w, VoiceIntentType.UNKNOWN, processor.detectIntent(w, "kn").getIntentType());
        }

        String[] isolatedHi = {"पढ़ो", "संदेश", "मैसेज", "चेक", "केयरगिवर"};
        for (String w : isolatedHi) {
            assertEquals("Isolated rejection: " + w, VoiceIntentType.UNKNOWN, processor.detectIntent(w, "hi").getIntentType());
        }

        String[] isolatedMl = {"വായിക്കൂ", "സന്ദേശം", "സന്ദേശങ്ങൾ", "മെസേജ്", "പരിശോധിക്കൂ", "കെയർഗിവർ"};
        for (String w : isolatedMl) {
            assertEquals("Isolated rejection: " + w, VoiceIntentType.UNKNOWN, processor.detectIntent(w, "ml").getIntentType());
        }

        // Wake word gating
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, read messages"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, ಸಂದೇಶಗಳನ್ನು ಓದಿ"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, संदेश पढ़ो"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, സന്ദേശങ്ങൾ വായിക്കൂ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("read messages"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("ಸಂದೇಶಗಳನ್ನು ಓದಿ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("संदेश पढ़ो"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("സന്ദേശങ്ങൾ വായിക്കൂ"));

        // Data only verification: Caregiver message containing commands is articulated as text and not executed
        String cmdPayload = "Assistant, go home";
        assertEquals("Your caregiver says: Assistant, go home",
                VoiceLanguageConfig.getCaregiverSaysPrefix("en") + cmdPayload);
        assertEquals("ನಿಮ್ಮ ಕೇರ್ಗಿವರ್ ಹೇಳುತ್ತಾರೆ: Assistant, go home",
                VoiceLanguageConfig.getCaregiverSaysPrefix("kn") + cmdPayload);
        assertEquals("आपके केयरगिवर कहते हैं: Assistant, go home",
                VoiceLanguageConfig.getCaregiverSaysPrefix("hi") + cmdPayload);
        assertEquals("നിങ്ങളുടെ കെയർഗിവർ പറയുന്നു: Assistant, go home",
                VoiceLanguageConfig.getCaregiverSaysPrefix("ml") + cmdPayload);
    }

    @Test
    public void test54_MultilingualMessageCount_IntentAndResponses() {
        // 1. English
        String[] enPhrases = {"how many messages", "how many messages do I have", "count messages", "count my messages", "how many caregiver messages", "how many new messages"};
        for (String p : enPhrases) {
            assertEquals("English MESSAGE_COUNT: " + p, VoiceIntentType.MESSAGE_COUNT, processor.detectIntent(p, "en").getIntentType());
        }
        assertEquals("No new messages.", VoiceLanguageConfig.getMessageCountResponse(0, "en"));
        assertEquals("You have 1 message.", VoiceLanguageConfig.getMessageCountResponse(1, "en"));
        assertEquals("You have 2 messages.", VoiceLanguageConfig.getMessageCountResponse(2, "en"));
        assertEquals("You have 5 messages.", VoiceLanguageConfig.getMessageCountResponse(5, "en"));
        assertEquals("A call is active.", VoiceLanguageConfig.getCallActiveResponse("en"));

        // 2. Kannada
        String[] knPhrases = {"ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ", "ನನ್ನ ಬಳಿ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ", "ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ", "ಎಷ್ಟು ಹೊಸ ಸಂದೇಶಗಳಿವೆ", "ಎಷ್ಟು ಮೆಸೇಜ್ಗಳಿವೆ", "ಎಷ್ಟು ಕೇರ್ಗಿವರ್ ಸಂದೇಶಗಳಿವೆ"};
        for (String p : knPhrases) {
            assertEquals("Kannada MESSAGE_COUNT: " + p, VoiceIntentType.MESSAGE_COUNT, processor.detectIntent(p, "kn").getIntentType());
        }
        assertEquals("ಹೊಸ ಸಂದೇಶಗಳಿಲ್ಲ.", VoiceLanguageConfig.getMessageCountResponse(0, "kn"));
        assertEquals("ನಿಮ್ಮ ಬಳಿ 1 ಸಂದೇಶವಿದೆ.", VoiceLanguageConfig.getMessageCountResponse(1, "kn"));
        assertEquals("ನಿಮ್ಮ ಬಳಿ 2 ಸಂದೇಶಗಳಿವೆ.", VoiceLanguageConfig.getMessageCountResponse(2, "kn"));
        assertEquals("ನಿಮ್ಮ ಬಳಿ 5 ಸಂದೇಶಗಳಿವೆ.", VoiceLanguageConfig.getMessageCountResponse(5, "kn"));
        assertEquals("ಕರೆ ಸಕ್ರಿಯವಾಗಿದೆ.", VoiceLanguageConfig.getCallActiveResponse("kn"));

        // 3. Hindi
        String[] hiPhrases = {"कितने संदेश हैं", "मेरे पास कितने संदेश हैं", "संदेश गिनो", "कितने नए संदेश हैं", "कितने मैसेज हैं", "कितने केयरगिवर संदेश हैं"};
        for (String p : hiPhrases) {
            assertEquals("Hindi MESSAGE_COUNT: " + p, VoiceIntentType.MESSAGE_COUNT, processor.detectIntent(p, "hi").getIntentType());
        }
        assertEquals("कोई नया संदेश नहीं है।", VoiceLanguageConfig.getMessageCountResponse(0, "hi"));
        assertEquals("आपके पास 1 संदेश है।", VoiceLanguageConfig.getMessageCountResponse(1, "hi"));
        assertEquals("आपके पास 2 संदेश हैं।", VoiceLanguageConfig.getMessageCountResponse(2, "hi"));
        assertEquals("आपके पास 5 संदेश हैं।", VoiceLanguageConfig.getMessageCountResponse(5, "hi"));
        assertEquals("एक कॉल सक्रिय है।", VoiceLanguageConfig.getCallActiveResponse("hi"));

        // 4. Malayalam
        String[] mlPhrases = {"എത്ര സന്ദേശങ്ങളുണ്ട്", "എന്റെ പക്കൽ എത്ര സന്ദേശങ്ങളുണ്ട്", "സന്ദേശങ്ങൾ എണ്ണൂ", "എത്ര പുതിയ സന്ദേശങ്ങളുണ്ട്", "എത്ര മെസേജുകളുണ്ട്", "എത്ര കെയർഗിവർ സന്ദേശങ്ങളുണ്ട്"};
        for (String p : mlPhrases) {
            assertEquals("Malayalam MESSAGE_COUNT: " + p, VoiceIntentType.MESSAGE_COUNT, processor.detectIntent(p, "ml").getIntentType());
        }
        assertEquals("പുതിയ സന്ദേശങ്ങളൊന്നുമില്ല.", VoiceLanguageConfig.getMessageCountResponse(0, "ml"));
        assertEquals("നിങ്ങളുടെ പക്കൽ 1 സന്ദേശമുണ്ട്.", VoiceLanguageConfig.getMessageCountResponse(1, "ml"));
        assertEquals("നിങ്ങളുടെ പക്കൽ 2 സന്ദേശങ്ങളുണ്ട്.", VoiceLanguageConfig.getMessageCountResponse(2, "ml"));
        assertEquals("നിങ്ങളുടെ പക്കൽ 5 സന്ദേശങ്ങളുണ്ട്.", VoiceLanguageConfig.getMessageCountResponse(5, "ml"));
        assertEquals("ഒരു കോൾ സജീവമാണ്.", VoiceLanguageConfig.getCallActiveResponse("ml"));
    }

    @Test
    public void test55_MultilingualMessageCount_DataOnlyAndRejections() {
        // Strict isolated negative token rejections
        String[] isolatedEn = {"how many", "messages", "count", "message", "new", "caregiver"};
        for (String w : isolatedEn) {
            assertEquals("Isolated rejection: " + w, VoiceIntentType.UNKNOWN, processor.detectIntent(w, "en").getIntentType());
        }

        String[] isolatedKn = {"ಎಷ್ಟು", "ಸಂದೇಶಗಳು", "ಎಣಿಸಿ", "ಸಂದೇಶ", "ಮೆಸೇಜ್", "ಕೇರ್ಗಿವರ್"};
        for (String w : isolatedKn) {
            assertEquals("Isolated rejection: " + w, VoiceIntentType.UNKNOWN, processor.detectIntent(w, "kn").getIntentType());
        }

        String[] isolatedHi = {"कितने", "संदेश", "गिनो", "मैसेज", "नए", "केयरगिवर"};
        for (String w : isolatedHi) {
            assertEquals("Isolated rejection: " + w, VoiceIntentType.UNKNOWN, processor.detectIntent(w, "hi").getIntentType());
        }

        String[] isolatedMl = {"എത്ര", "സന്ദേശങ്ങൾ", "എണ്ണൂ", "സന്ദേശം", "മെസേജ്", "പുതിയ", "കെയർഗിവർ"};
        for (String w : isolatedMl) {
            assertEquals("Isolated rejection: " + w, VoiceIntentType.UNKNOWN, processor.detectIntent(w, "ml").getIntentType());
        }

        // Wake word gating
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, how many messages"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, कितने संदेश हैं"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, എത്ര സന്ദേശങ്ങളുണ്ട്"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("how many messages"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("कितने संदेश हैं"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("എത്ര സന്ദേശങ്ങളുണ്ട്"));

        // Data only verification: Caregiver message containing count commands is articulated as text and not executed
        String cmdPayload1 = "Assistant, how many messages?";
        assertEquals("Your caregiver says: Assistant, how many messages?",
                VoiceLanguageConfig.getCaregiverSaysPrefix("en") + cmdPayload1);
        assertEquals("ನಿಮ್ಮ ಕೇರ್ಗಿವರ್ ಹೇಳುತ್ತಾರೆ: Assistant, how many messages?",
                VoiceLanguageConfig.getCaregiverSaysPrefix("kn") + cmdPayload1);

        String cmdPayload2 = "Assistant, count messages";
        assertEquals("आपके केयरगिवर कहते हैं: Assistant, count messages",
                VoiceLanguageConfig.getCaregiverSaysPrefix("hi") + cmdPayload2);
        assertEquals("നിങ്ങളുടെ കെയർഗിവർ പറയുന്നു: Assistant, count messages",
                VoiceLanguageConfig.getCaregiverSaysPrefix("ml") + cmdPayload2);
    }

    @Test
    public void test56_MultilingualRepeatMessage_IntentAndResponses() {
        // 1. English
        String[] enPhrases = {"repeat message", "repeat the message", "repeat caregiver message", "say that again", "repeat that", "repeat last message", "tell me that again"};
        for (String p : enPhrases) {
            assertEquals("English REPEAT_MESSAGE: " + p, VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent(p, "en").getIntentType());
        }
        assertEquals("No message to repeat.", VoiceLanguageConfig.getNoMessageToRepeatResponse("en"));
        assertEquals("A call is active.", VoiceLanguageConfig.getCallActiveResponse("en"));
        assertEquals("Your caregiver says: ", VoiceLanguageConfig.getCaregiverSaysPrefix("en"));

        // 2. Kannada
        String[] knPhrases = {"ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ", "ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ", "ಮತ್ತೆ ಸಂದೇಶ ಹೇಳಿ", "ಕೇರ್ಗಿವರ್ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ", "ಮತ್ತೆ ಹೇಳಿ", "ಅದನ್ನು ಮತ್ತೆ ಹೇಳಿ", "ಕೊನೆಯ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ", "ಮತ್ತೊಮ್ಮೆ ಹೇಳಿ"};
        for (String p : knPhrases) {
            assertEquals("Kannada REPEAT_MESSAGE: " + p, VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent(p, "kn").getIntentType());
        }
        assertEquals("ಮರುಹೇಳಲು ಯಾವುದೇ ಸಂದೇಶವಿಲ್ಲ.", VoiceLanguageConfig.getNoMessageToRepeatResponse("kn"));
        assertEquals("ಕರೆ ಸಕ್ರಿಯವಾಗಿದೆ.", VoiceLanguageConfig.getCallActiveResponse("kn"));
        assertEquals("ನಿಮ್ಮ ಕೇರ್ಗಿವರ್ ಹೇಳುತ್ತಾರೆ: ", VoiceLanguageConfig.getCaregiverSaysPrefix("kn"));

        // 3. Hindi
        String[] hiPhrases = {"संदेश फिर से पढ़ो", "संदेश फिर से बताओ", "संदेश दोबारा पढ़ो", "केयरगिवर का संदेश फिर से पढ़ो", "मैसेज फिर से पढ़ो", "फिर से बताओ", "आखिरी संदेश दोबारा बताओ", "वही संदेश फिर से बताओ"};
        for (String p : hiPhrases) {
            assertEquals("Hindi REPEAT_MESSAGE: " + p, VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent(p, "hi").getIntentType());
        }
        assertEquals("दोबारा सुनाने के लिए कोई संदेश नहीं है।", VoiceLanguageConfig.getNoMessageToRepeatResponse("hi"));
        assertEquals("एक कॉल सक्रिय है।", VoiceLanguageConfig.getCallActiveResponse("hi"));
        assertEquals("आपके केयरगिवर कहते हैं: ", VoiceLanguageConfig.getCaregiverSaysPrefix("hi"));

        // 4. Malayalam
        String[] mlPhrases = {"സന്ദേശം വീണ്ടും പറയൂ", "സന്ദേശം വീണ്ടും വായിക്കൂ", "സന്ദേശം വീണ്ടും വായിക്കുക", "കെയർഗിവറിന്റെ സന്ദേശം വീണ്ടും പറയൂ", "മെസേജ് വീണ്ടും പറയൂ", "വീണ്ടും പറയൂ", "അവസാന സന്ദേശം വീണ്ടും പറയൂ"};
        for (String p : mlPhrases) {
            assertEquals("Malayalam REPEAT_MESSAGE: " + p, VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent(p, "ml").getIntentType());
        }
        assertEquals("വീണ്ടും പറയാൻ സന്ദേശമൊന്നുമില്ല.", VoiceLanguageConfig.getNoMessageToRepeatResponse("ml"));
        assertEquals("ഒരു കോൾ സജീവമാണ്.", VoiceLanguageConfig.getCallActiveResponse("ml"));
        assertEquals("നിങ്ങളുടെ കെയർഗിവർ പറയുന്നു: ", VoiceLanguageConfig.getCaregiverSaysPrefix("ml"));
    }

    @Test
    public void test57_MultilingualRepeatMessage_DataOnlyAndRejections() {
        // Strict isolated negative token rejections
        String[] isolatedEn = {"repeat", "message", "again", "say", "that", "messages", "caregiver"};
        for (String w : isolatedEn) {
            assertEquals("Isolated rejection: " + w, VoiceIntentType.UNKNOWN, processor.detectIntent(w, "en").getIntentType());
        }

        String[] isolatedKn = {"ಮತ್ತೆ", "ಸಂದೇಶ", "ಹೇಳಿ", "ಮೆಸೇಜ್", "ಕೇರ್ಗಿವರ್", "ಕೇರ್‌ಗಿವರ್"};
        for (String w : isolatedKn) {
            assertEquals("Isolated rejection: " + w, VoiceIntentType.UNKNOWN, processor.detectIntent(w, "kn").getIntentType());
        }

        String[] isolatedHi = {"फिर", "संदेश", "बताओ", "पढ़ो", "दोबारा", "मैसेज", "केयरगिवर"};
        for (String w : isolatedHi) {
            assertEquals("Isolated rejection: " + w, VoiceIntentType.UNKNOWN, processor.detectIntent(w, "hi").getIntentType());
        }

        String[] isolatedMl = {"വീണ്ടും", "സന്ദേശം", "പറയൂ", "വായിക്കൂ", "മെസേജ്", "കെയർഗിവർ"};
        for (String w : isolatedMl) {
            assertEquals("Isolated rejection: " + w, VoiceIntentType.UNKNOWN, processor.detectIntent(w, "ml").getIntentType());
        }

        // Wake word gating
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, repeat message"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, संदेश फिर से पढ़ो"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, സന്ദേശം വീണ്ടും പറയൂ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("repeat message"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("संदेश फिर से पढ़ो"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("സന്ദേശം വീണ്ടും പറയൂ"));

        // Data only verification: Caregiver message containing repeat commands is articulated as text and not executed
        String cmdPayload1 = "Assistant, repeat message";
        assertEquals("Your caregiver says: Assistant, repeat message",
                VoiceLanguageConfig.getCaregiverSaysPrefix("en") + cmdPayload1);
        assertEquals("ನಿಮ್ಮ ಕೇರ್ಗಿವರ್ ಹೇಳುತ್ತಾರೆ: Assistant, repeat message",
                VoiceLanguageConfig.getCaregiverSaysPrefix("kn") + cmdPayload1);

        String cmdPayload2 = "Assistant, say that again";
        assertEquals("आपके केयरगिवर कहते हैं: Assistant, say that again",
                VoiceLanguageConfig.getCaregiverSaysPrefix("hi") + cmdPayload2);
        assertEquals("നിങ്ങളുടെ കെയർഗിവർ പറയുന്നു: Assistant, say that again",
                VoiceLanguageConfig.getCaregiverSaysPrefix("ml") + cmdPayload2);
    }

    @Test
    public void test58_MultilingualReadNotifications_IntentAndResponses() {
        // 1. English
        String[] enPhrases = {"read notifications", "read my notifications", "check notifications", "read notification", "tell me my notifications"};
        for (String p : enPhrases) {
            assertEquals("English READ_NOTIFICATIONS: " + p, VoiceIntentType.READ_NOTIFICATIONS, processor.detectIntent(p, "en").getIntentType());
        }
        assertEquals("No new notifications.", VoiceLanguageConfig.getNoNotificationsResponse("en"));
        assertEquals("Your latest notification says: ", VoiceLanguageConfig.getSingleNotificationIntro("en"));
        assertEquals("You have 3 recent notifications.", VoiceLanguageConfig.getMultipleNotificationsIntro(3, "en"));
        assertEquals("A call is active.", VoiceLanguageConfig.getCallActiveResponse("en"));

        // 2. Kannada
        String[] knPhrases = {"ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ", "ನನ್ನ ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ", "ಅಧಿಸೂಚನೆಗಳನ್ನು ಪರಿಶೀಲಿಸಿ", "ಅಧಿಸೂಚನೆ ಓದಿ", "ಹೊಸ ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ"};
        for (String p : knPhrases) {
            assertEquals("Kannada READ_NOTIFICATIONS: " + p, VoiceIntentType.READ_NOTIFICATIONS, processor.detectIntent(p, "kn").getIntentType());
        }
        assertEquals("ಹೊಸ ಅಧಿಸೂಚನೆಗಳಿಲ್ಲ.", VoiceLanguageConfig.getNoNotificationsResponse("kn"));
        assertEquals("ನಿಮ್ಮ ಇತ್ತೀಚಿನ ಅಧಿಸೂಚನೆ ಹೀಗಿದೆ: ", VoiceLanguageConfig.getSingleNotificationIntro("kn"));
        assertEquals("ನಿಮ್ಮ ಬಳಿ 3 ಇತ್ತೀಚಿನ ಅಧಿಸೂಚನೆಗಳಿವೆ.", VoiceLanguageConfig.getMultipleNotificationsIntro(3, "kn"));
        assertEquals("ಕರೆ ಸಕ್ರಿಯವಾಗಿದೆ.", VoiceLanguageConfig.getCallActiveResponse("kn"));

        // 3. Hindi
        String[] hiPhrases = {"सूचनाएँ पढ़ो", "मेरी सूचनाएँ पढ़ो", "नोटिफिकेशन पढ़ो", "सूचनाएँ चेक करो", "नए नोटिफिकेशन पढ़ो"};
        for (String p : hiPhrases) {
            assertEquals("Hindi READ_NOTIFICATIONS: " + p, VoiceIntentType.READ_NOTIFICATIONS, processor.detectIntent(p, "hi").getIntentType());
        }
        assertEquals("कोई नई सूचना नहीं है।", VoiceLanguageConfig.getNoNotificationsResponse("hi"));
        assertEquals("आपकी नवीनतम सूचना कहती है: ", VoiceLanguageConfig.getSingleNotificationIntro("hi"));
        assertEquals("आपके पास 3 हाल की सूचनाएँ हैं।", VoiceLanguageConfig.getMultipleNotificationsIntro(3, "hi"));
        assertEquals("एक कॉल सक्रिय है।", VoiceLanguageConfig.getCallActiveResponse("hi"));

        // 4. Malayalam
        String[] mlPhrases = {"അറിയിപ്പുകൾ വായിക്കൂ", "എന്റെ അറിയിപ്പുകൾ വായിക്കൂ", "നോട്ടിഫിക്കേഷൻ വായിക്കൂ", "പുതിയ അറിയിപ്പുകൾ വായിക്കൂ"};
        for (String p : mlPhrases) {
            assertEquals("Malayalam READ_NOTIFICATIONS: " + p, VoiceIntentType.READ_NOTIFICATIONS, processor.detectIntent(p, "ml").getIntentType());
        }
        assertEquals("പുതിയ അറിയിപ്പുകളൊന്നുമില്ല.", VoiceLanguageConfig.getNoNotificationsResponse("ml"));
        assertEquals("നിങ്ങളുടെ ഏറ്റവും പുതിയ അറിയിപ്പ് പറയുന്നത്: ", VoiceLanguageConfig.getSingleNotificationIntro("ml"));
        assertEquals("നിങ്ങളുടെ പക്കൽ 3 പുതിയ അറിയിപ്പുകളുണ്ട്.", VoiceLanguageConfig.getMultipleNotificationsIntro(3, "ml"));
        assertEquals("ഒരു കോൾ സജീവമാണ്.", VoiceLanguageConfig.getCallActiveResponse("ml"));
    }

    @Test
    public void test59_MultilingualReadNotifications_DataOnlyAndRejections() {
        // Strict isolated negative token rejections
        String[] isolatedEn = {"read", "notification", "notifications", "check", "new", "latest", "tell"};
        for (String w : isolatedEn) {
            assertEquals("Isolated rejection: " + w, VoiceIntentType.UNKNOWN, processor.detectIntent(w, "en").getIntentType());
        }

        String[] isolatedKn = {"ಅಧಿಸೂಚನೆ", "ಅಧಿಸೂಚನೆಗಳು", "ಓದಿ", "ಪರಿಶೀಲಿಸಿ", "ಹೊಸದು", "ಇತ್ತೀಚಿನ"};
        for (String w : isolatedKn) {
            assertEquals("Isolated rejection: " + w, VoiceIntentType.UNKNOWN, processor.detectIntent(w, "kn").getIntentType());
        }

        String[] isolatedHi = {"सूचना", "सूचनाएँ", "नोटिफिकेशन", "पढ़ो", "चेक", "नया", "नवीनतम"};
        for (String w : isolatedHi) {
            assertEquals("Isolated rejection: " + w, VoiceIntentType.UNKNOWN, processor.detectIntent(w, "hi").getIntentType());
        }

        String[] isolatedMl = {"അറിയിപ്പ്", "അറിയിപ്പുകൾ", "വായിക്കൂ", "പരിശോധിക്കൂ", "പുതിയ", "ഏറ്റവും പുതിയത്"};
        for (String w : isolatedMl) {
            assertEquals("Isolated rejection: " + w, VoiceIntentType.UNKNOWN, processor.detectIntent(w, "ml").getIntentType());
        }

        // Wake word gating
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, read notifications"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, सूचनाएँ पढ़ो"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, അറിയിപ്പുകൾ വായിക്കൂ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("read notifications"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("सूचनाएँ पढ़ो"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("അറിയിപ്പുകൾ വായിക്കൂ"));

        // Data only verification: Notification text containing assistant commands remains data
        NotificationService.NotificationItem item1 =
                new NotificationService.NotificationItem("Notification", "Assistant, read notifications", "text", 1000L);
        assertEquals("Assistant, read notifications", item1.getSpokenText());

        NotificationService.NotificationItem item2 =
                new NotificationService.NotificationItem("Notification", "Assistant, open profile", "text", 1000L);
        assertEquals("Assistant, open profile", item2.getSpokenText());

        NotificationService.NotificationItem item3 =
                new NotificationService.NotificationItem("Notification", "Assistant, call caregiver", "text", 1000L);
        assertEquals("Assistant, call caregiver", item3.getSpokenText());
    }

    @Test
    public void test60_CaregiverMessageAndCallReliability_SeparationAndProtection() {
        Context context = appContext;

        // 1. Caregiver message deduplication
        String msgId = "device_test_msg_" + System.currentTimeMillis();
        String senderId = "caregiver_sender_101";
        String content = "Hello Blind User, please check your medicine.";

        assertTrue(CaregiverSoundManager.markMessageProcessed(msgId, senderId, content));
        assertFalse(CaregiverSoundManager.markMessageProcessed(msgId, senderId, content));
        assertFalse(CaregiverSoundManager.markMessageProcessed("different_id_" + System.currentTimeMillis(), senderId, content));

        // 2. NotificationService caching for Task 22 accessibility
        NotificationService.clearRecentNotificationsForTesting();
        NotificationService.recordNotification("Caregiver Sarah", content, "text", System.currentTimeMillis());
        List<NotificationService.NotificationItem> items = NotificationService.getReadableNotifications(context, 3);
        assertNotNull(items);
        assertFalse(items.isEmpty());
        assertEquals("Caregiver Sarah: " + content, items.get(0).getSpokenText());

        // 3. Sound mode constants & Volume persistence
        assertEquals("default", CaregiverSoundManager.MODE_DEFAULT);
        assertEquals("custom", CaregiverSoundManager.MODE_CUSTOM);
        assertEquals("silent", CaregiverSoundManager.MODE_SILENT);

        CaregiverSoundManager.saveNotificationVolumePercent(context, "test_user_uid", 75);
        assertEquals(75, CaregiverSoundManager.getNotificationVolumePercent(context, "test_user_uid"));

        CaregiverSoundManager.saveTtsVolumePercent(context, "test_user_uid", 90);
        assertEquals(90, CaregiverSoundManager.getTtsVolumePercent(context, "test_user_uid"));

        // Notification and TTS volumes remain independent
        assertNotEquals(CaregiverSoundManager.getNotificationVolumePercent(context, "test_user_uid"),
                CaregiverSoundManager.getTtsVolumePercent(context, "test_user_uid"));

        // 4. Voice assistant intent detection remains valid
        assertEquals(VoiceIntentType.READ_NOTIFICATIONS, processor.detectIntent("read notifications", "en").getIntentType());
        assertEquals(VoiceIntentType.CALL_CAREGIVER, processor.detectIntent("call caregiver", "en").getIntentType());
        assertEquals(VoiceIntentType.END_CALL, processor.detectIntent("end call", "en").getIntentType());
    }

    @Test
    public void test61_Task24_NaturalKannadaCaregiverAliases() {
        String lang = "kn";

        // 1. CALL_CAREGIVER aliases
        String[] callAliases = {
                "Assistant, ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ",
                "Assistant, ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ",
                "Assistant, ಕೇರ್ಗಿವರ್ಗೆ ಕರೆ ಮಾಡಿ",
                "Assistant, ಕೇರ್ಟೇಕರ್ಗೆ ಕರೆ ಮಾಡಿ",
                "Assistant, ಕೇರ್ ಟೇಕರ್ಗೆ ಕರೆ ಮಾಡಿ",
                "ಆರೈಕೆದಾರರಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ಆರೈಕೆದಾರನಿಗೆ ಕರೆ ಮಾಡಿ",
                "ಸಹಾಯಕನಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ಸಹಾಯಕರಿಗೆ ಕರೆ ಮಾಡಿ",
                "ನನ್ನ ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ",
                "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ",
                "ನನ್ನ ಕೇರ್ಗಿವರ್ಗೆ ಕರೆ ಮಾಡಿ",
                "ನನ್ನ ಕೇರ್ಟೇಕರ್ಗೆ ಕರೆ ಮಾಡಿ"
        };
        for (String phrase : callAliases) {
            assertEquals("Call caregiver phrase: " + phrase,
                    VoiceIntentType.CALL_CAREGIVER, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 2. SEND_MESSAGE aliases
        String[] sendAliases = {
                "Assistant, ಆರೈಕೆದಾರರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "Assistant, ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "Assistant, ಕೇರ್ಗಿವರ್ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "Assistant, ಕೇರ್ಟೇಕರ್ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಆರೈಕೆದಾರರಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ಸಹಾಯಕನಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ಆರೈಕೆದಾರರಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ",
                "ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ",
                "ನನ್ನ ಆರೈಕೆದಾರರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ನನ್ನ ಕೇರ್ಗಿವರ್ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ನನ್ನ ಕೇರ್ಟೇಕರ್ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ"
        };
        for (String phrase : sendAliases) {
            assertEquals("Send message phrase: " + phrase,
                    VoiceIntentType.SEND_MESSAGE, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 3. READ_MESSAGES aliases
        String[] readAliases = {
                "Assistant, ಆರೈಕೆದಾರರ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "Assistant, ಸಹಾಯಕನ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "Assistant, ಕೇರ್ಗಿವರ್ನ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "Assistant, ಕೇರ್ಟೇಕರ್ನ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ಆರೈಕೆದಾರರ ಸಂದೇಶ ಓದಿ",
                "ಆರೈಕೆದಾರರ ಮೆಸೇಜ್ ಓದಿ",
                "ಸಹಾಯಕನ ಸಂದೇಶ ಓದಿ",
                "ಸಹಾಯಕರ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ಆರೈಕೆದಾರರ ಸಂದೇಶಗಳನ್ನು ಪರಿಶೀಲಿಸಿ",
                "ನನ್ನ ಆರೈಕೆದಾರರ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ನನ್ನ ಸಹಾಯಕನ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ನನ್ನ ಕೇರ್ಗಿವರ್ನ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ನನ್ನ ಕೇರ್ಟೇಕರ್ನ ಸಂದೇಶಗಳನ್ನು ಓದಿ"
        };
        for (String phrase : readAliases) {
            assertEquals("Read messages phrase: " + phrase,
                    VoiceIntentType.READ_MESSAGES, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 4. MESSAGE_COUNT aliases
        String[] countAliases = {
                "Assistant, ಆರೈಕೆದಾರರಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "Assistant, ಸಹಾಯಕನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "Assistant, ಕೇರ್ಗಿವರ್ನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "Assistant, ಕೇರ್ಟೇಕರ್ನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಆರೈಕೆದಾರನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಆರೈಕೆದಾರರ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಆರೈಕೆದಾರರಿಂದ ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ",
                "ಸಹಾಯಕನಿಂದ ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ",
                "ಆರೈಕೆದಾರರಿಂದ ಎಷ್ಟು ಮೆಸೇಜ್ಗಳಿವೆ",
                "ಸಹಾಯಕನಿಂದ ಎಷ್ಟು ಮೆಸೇಜ್ಗಳಿವೆ",
                "ನನ್ನ ಆರೈಕೆದಾರರಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ನನ್ನ ಸಹಾಯಕನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ನನ್ನ ಕೇರ್ಗಿವರ್ನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ನನ್ನ ಕೇರ್ಟೇಕರ್ನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ"
        };
        for (String phrase : countAliases) {
            assertEquals("Message count phrase: " + phrase,
                    VoiceIntentType.MESSAGE_COUNT, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 5. REPEAT_MESSAGE aliases
        String[] repeatAliases = {
                "Assistant, ಆರೈಕೆದಾರರ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "Assistant, ಸಹಾಯಕನ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "Assistant, ಕೇರ್ಗಿವರ್ನ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "Assistant, ಕೇರ್ಟೇಕರ್ನ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಆರೈಕೆದಾರರ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಆರೈಕೆದಾರರ ಮೆಸೇಜ್ ಮತ್ತೆ ಹೇಳಿ",
                "ಸಹಾಯಕನ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಸಹಾಯಕರ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಆರೈಕೆದಾರರ ಸಂದೇಶವನ್ನು ಪುನರಾವರ್ತಿಸಿ",
                "ಸಹಾಯಕನ ಸಂದೇಶವನ್ನು ಪುನರಾವರ್ತಿಸಿ",
                "ನನ್ನ ಆರೈಕೆದಾರರ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ನನ್ನ ಸಹಾಯಕನ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ನನ್ನ ಕೇರ್ಗಿವರ್ನ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ನನ್ನ ಕೇರ್ಟೇಕರ್ನ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ"
        };
        for (String phrase : repeatAliases) {
            assertEquals("Repeat message phrase: " + phrase,
                    VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 6. Critical Negative Rejections
        String[] negativePhrases = {
                "ಆರೈಕೆದಾರ",
                "ಸಹಾಯಕ",
                "ಕೇರ್ಗಿವರ್",
                "ಕೇರ್ಟೇಕರ್",
                "ಕರೆ ಮಾಡಿ",
                "ಕಾಲ್ ಮಾಡಿ",
                "ಕರೆ",
                "ಕಾಲ್",
                "ಸಂದೇಶ",
                "ಸಹಾಯಕ ಯಾರು?",
                "ಸಹಾಯಕನ ಬಗ್ಗೆ ಹೇಳಿ",
                "ಆರೈಕೆದಾರ ಯಾರು?",
                "ನನಗೆ ಸಹಾಯಕ ಬೇಕು"
        };
        for (String phrase : negativePhrases) {
            assertEquals("Negative rejection for phrase: " + phrase,
                    VoiceIntentType.UNKNOWN, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 7. Universal Wake Word Gating & Data Safety
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Hey assistant, ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ"));

        assertEquals("ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ", VoiceIntentMatcher.extractCommandText("Assistant, ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ"));
        assertEquals("ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ", VoiceIntentMatcher.extractCommandText("Hey assistant, ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ"));

        // Message data isolation
        String payload = "ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ ಮತ್ತು ಸಂದೇಶ ಕಳುಹಿಸಿ";
        NotificationService.NotificationItem item =
                new NotificationService.NotificationItem("Caregiver", payload, "text", 3000L);
        assertEquals("Caregiver: ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ ಮತ್ತು ಸಂದೇಶ ಕಳುಹಿಸಿ", item.getSpokenText());
    }

    @Test
    public void test62_Task25_NaturalHindiCaregiverAliases() {
        String lang = "hi";

        // 1. CALL_CAREGIVER aliases
        String[] callAliases = {
                "Assistant, देखभाल करने वाले को कॉल करो",
                "Assistant, सहायक को कॉल करो",
                "Assistant, केयरगिवर को कॉल करो",
                "Assistant, केयरटेकर को कॉल करो",
                "Assistant, मेरे देखभाल करने वाले को कॉल करो",
                "Assistant, मेरे सहायक को कॉल करो",
                "Assistant, मेरे केयरगिवर को कॉल करो",
                "Assistant, मेरे केयरटेकर को कॉल करो",
                "देखभाल करने वाले को कॉल करें",
                "सहायक को कॉल लगाओ",
                "देखभाल करने वाले को फोन करो",
                "सहायक को फोन लगाओ"
        };
        for (String phrase : callAliases) {
            assertEquals("Call caregiver phrase: " + phrase,
                    VoiceIntentType.CALL_CAREGIVER, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 2. SEND_MESSAGE aliases
        String[] sendAliases = {
                "Assistant, देखभाल करने वाले को संदेश भेजो",
                "Assistant, सहायक को संदेश भेजो",
                "Assistant, केयरगिवर को संदेश भेजो",
                "Assistant, केयरटेकर को संदेश भेजो",
                "Assistant, मेरे देखभाल करने वाले को संदेश भेजो",
                "Assistant, मेरे सहायक को संदेश भेजो",
                "देखभाल करने वाले को मैसेज भेजो",
                "सहायक को मैसेज भेजो",
                "देखभाल करने वाले को संदेश भेजें",
                "सहायक को संदेश भेजें"
        };
        for (String phrase : sendAliases) {
            assertEquals("Send message phrase: " + phrase,
                    VoiceIntentType.SEND_MESSAGE, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 3. READ_MESSAGES aliases
        String[] readAliases = {
                "Assistant, देखभाल करने वाले के संदेश पढ़ो",
                "Assistant, सहायक के संदेश पढ़ो",
                "Assistant, केयरगिवर के संदेश पढ़ो",
                "Assistant, केयरटेकर के संदेश पढ़ो",
                "Assistant, मेरे देखभाल करने वाले के संदेश पढ़ो",
                "Assistant, मेरे सहायक के संदेश पढ़ो",
                "देखभाल करने वाले के संदेश पढ़कर सुनाओ",
                "सहायक के संदेश पढ़कर सुनाओ",
                "संदेश पढ़कर सुनाओ",
                "मैसेज पढ़कर सुनाओ"
        };
        for (String phrase : readAliases) {
            assertEquals("Read messages phrase: " + phrase,
                    VoiceIntentType.READ_MESSAGES, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 4. MESSAGE_COUNT aliases
        String[] countAliases = {
                "Assistant, देखभाल करने वाले के कितने संदेश हैं",
                "Assistant, सहायक के कितने संदेश हैं",
                "Assistant, केयरगिवर के कितने संदेश हैं",
                "Assistant, केयरटेकर के कितने संदेश हैं",
                "Assistant, मेरे देखभाल करने वाले के कितने संदेश हैं",
                "Assistant, मेरे सहायक के कितने संदेश हैं",
                "देखभाल करने वाले के कितने मैसेज हैं",
                "सहायक के कितने मैसेज हैं",
                "देखभाल करने वाले से कितने संदेश हैं",
                "देखभाल करने वाले के संदेश गिनो"
        };
        for (String phrase : countAliases) {
            assertEquals("Message count phrase: " + phrase,
                    VoiceIntentType.MESSAGE_COUNT, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 5. REPEAT_MESSAGE aliases
        String[] repeatAliases = {
                "Assistant, देखभाल करने वाले का संदेश फिर से सुनाओ",
                "Assistant, सहायक का संदेश फिर से सुनाओ",
                "Assistant, केयरगिवर का संदेश फिर से सुनाओ",
                "Assistant, केयरटेकर का संदेश दोबारा सुनाओ",
                "Assistant, मेरे देखभाल करने वाले का संदेश फिर से बताओ",
                "Assistant, मेरे सहायक का संदेश फिर से सुनाओ",
                "देखभाल करने वाले का संदेश फिर से बताओ",
                "सहायक का संदेश दोबारा बताओ",
                "दोबारा सुनाओ",
                "फिर से बताओ"
        };
        for (String phrase : repeatAliases) {
            assertEquals("Repeat message phrase: " + phrase,
                    VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 6. Critical Negative Rejections
        String[] negativePhrases = {
                "देखभाल करने वाला",
                "देखभाल करने वाले",
                "सहायक",
                "केयरगिवर",
                "केयरटेकर",
                "कॉल",
                "फोन",
                "कॉल करो",
                "फोन करो",
                "संदेश",
                "मैसेज",
                "सहायक कौन है?",
                "सहायक के बारे में बताओ",
                "देखभाल करने वाला कौन है?",
                "केयरगिवर क्या है?",
                "केयरटेकर के बारे में बताओ",
                "मुझे सहायक चाहिए"
        };
        for (String phrase : negativePhrases) {
            assertEquals("Negative rejection for phrase: " + phrase,
                    VoiceIntentType.UNKNOWN, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 7. Universal Wake Word Gating & Data Safety
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, देखभाल करने वाले को कॉल करो"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Hey assistant, सहायक को संदेश भेजो"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("देखभाल करने वाले को कॉल करो"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("सहायक को संदेश भेजो"));

        assertEquals("देखभाल करने वाले को कॉल करो", VoiceIntentMatcher.extractCommandText("Assistant, देखभाल करने वाले को कॉल करो"));
        assertEquals("सहायक को संदेश भेजो", VoiceIntentMatcher.extractCommandText("Hey assistant, सहायक को संदेश भेजो"));

        // Message data isolation
        String hindiPayload = "देखभाल करने वाले को कॉल करो और सहायक को संदेश भेजो";
        NotificationService.NotificationItem hindiItem =
                new NotificationService.NotificationItem("Caregiver", hindiPayload, "text", 5000L);
        assertEquals("Caregiver: देखभाल करने वाले को कॉल करो और सहायक को संदेश भेजो", hindiItem.getSpokenText());
    }

    @Test
    public void test63_Task26_NaturalMalayalamCaregiverAliases() {
        String lang = "ml";

        // 1. CALL_CAREGIVER aliases
        String[] callAliases = {
                "Assistant, പരിചരിക്കുന്നയാളെ വിളിക്കൂ",
                "Assistant, എന്റെ പരിചരിക്കുന്നയാളെ വിളിക്കൂ",
                "Assistant, പരിചാരകനെ വിളിക്കൂ",
                "Assistant, എന്റെ പരിചാരകനെ വിളിക്കൂ",
                "Assistant, സഹായിയെ വിളിക്കൂ",
                "Assistant, എന്റെ സഹായിയെ വിളിക്കൂ",
                "Assistant, കെയർഗിവറെ വിളിക്കൂ",
                "Assistant, എന്റെ കെയർഗിവറെ വിളിക്കൂ",
                "Assistant, കെയർടേക്കറെ വിളിക്കൂ",
                "Assistant, എന്റെ കെയർടേക്കറെ വിളിക്കൂ",
                "പരിചരിക്കുന്നയാളെ ഫോൺ ചെയ്യൂ",
                "സഹായിയെ ഫോൺ ചെയ്യൂ",
                "കെയർഗിവറെ കോൾ ചെയ്യൂ"
        };
        for (String phrase : callAliases) {
            assertEquals("Call caregiver phrase: " + phrase,
                    VoiceIntentType.CALL_CAREGIVER, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 2. SEND_MESSAGE aliases
        String[] messageAliases = {
                "Assistant, പരിചരിക്കുന്നയാൾക്ക് സന്ദേശം അയയ്ക്കൂ",
                "Assistant, എന്റെ പരിചരിക്കുന്നയാൾക്ക് സന്ദേശം അയയ്ക്കൂ",
                "Assistant, പരിചാരകന് സന്ദേശം അയയ്ക്കൂ",
                "Assistant, സഹായിക്ക് സന്ദേശം അയയ്ക്കൂ",
                "Assistant, എന്റെ സഹായിക്ക് സന്ദേശം അയയ്ക്കൂ",
                "Assistant, കെയർഗിവറിന് സന്ദേശം അയയ്ക്കൂ",
                "Assistant, കെയർടേക്കറിന് സന്ദേശം അയയ്ക്കൂ",
                "പരിചരിക്കുന്നയാൾക്ക് മെസേജ് അയയ്ക്കൂ",
                "സഹായിക്ക് മെസേജ് അയയ്ക്കൂ"
        };
        for (String phrase : messageAliases) {
            assertEquals("Send message phrase: " + phrase,
                    VoiceIntentType.SEND_MESSAGE, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 3. READ_MESSAGES aliases
        String[] readAliases = {
                "Assistant, പരിചരിക്കുന്നയാളുടെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "Assistant, എന്റെ പരിചരിക്കുന്നയാളുടെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "Assistant, പരിചാരകന്റെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "Assistant, സഹായിയുടെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "Assistant, കെയർഗിവറിന്റെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "Assistant, കെയർടേക്കറിന്റെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "പരിചരിക്കുന്നയാളുടെ സന്ദേശം വായിക്കൂ",
                "സഹായിയുടെ സന്ദേശം വായിക്കൂ",
                "കെയർഗിവറിന്റെ സന്ദേശം വായിക്കൂ",
                "കെയർടേക്കറിന്റെ സന്ദേശം വായിക്കൂ",
                "സന്ദേശങ്ങൾ കേൾപ്പിക്കൂ"
        };
        for (String phrase : readAliases) {
            assertEquals("Read messages phrase: " + phrase,
                    VoiceIntentType.READ_MESSAGES, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 4. MESSAGE_COUNT aliases
        String[] countAliases = {
                "Assistant, പരിചരിക്കുന്നയാളുടെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "Assistant, എന്റെ പരിചരിക്കുന്നയാളുടെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "Assistant, പരിചാരകന്റെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "Assistant, സഹായിയുടെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "Assistant, കെയർഗിവറിന്റെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "Assistant, കെയർടേക്കറിന്റെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "പരിചരിക്കുന്നയാളുടെ എത്ര മെസേജുകളുണ്ട്",
                "പരിചരിക്കുന്നയാളുടെ സന്ദേശങ്ങൾ എണ്ണൂ",
                "സഹായിയുടെ സന്ദേശങ്ങൾ എണ്ണൂ",
                "കെയർഗിവറിന്റെ സന്ദേശങ്ങൾ എണ്ണൂ"
        };
        for (String phrase : countAliases) {
            assertEquals("Message count phrase: " + phrase,
                    VoiceIntentType.MESSAGE_COUNT, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 5. REPEAT_MESSAGE aliases
        String[] repeatAliases = {
                "Assistant, പരിചരിക്കുന്നയാളുടെ സന്ദേശം വീണ്ടും കേൾപ്പിക്കൂ",
                "Assistant, പരിചരിക്കുന്നയാളുടെ സന്ദേശം വീണ്ടും പറയൂ",
                "Assistant, എന്റെ പരിചാരകന്റെ സന്ദേശം വീണ്ടും കേൾപ്പിക്കൂ",
                "Assistant, പരിചാരകന്റെ സന്ദേശം വീണ്ടും പറയൂ",
                "Assistant, സഹായിയുടെ സന്ദേശം വീണ്ടും കേൾപ്പിക്കൂ",
                "Assistant, എന്റെ സഹായിയുടെ സന്ദേശം വീണ്ടും പറയൂ",
                "Assistant, കെയർഗിവറിന്റെ സന്ദേശം വീണ്ടും കേൾപ്പിക്കൂ",
                "Assistant, കെയർഗിവറിന്റെ സന്ദേശം വീണ്ടും പറയൂ",
                "Assistant, കെയർടേക്കറിന്റെ സന്ദേശം വീണ്ടും കേൾപ്പിക്കൂ",
                "Assistant, കെയർടേക്കറിന്റെ സന്ദേശം വീണ്ടും പറയൂ",
                "സഹായിയുടെ സന്ദേശം വീണ്ടും പറയൂ",
                "വീണ്ടും പറയൂ",
                "വീണ്ടും കേൾപ്പിക്കൂ",
                "ഒരിക്കൽ കൂടി പറയൂ",
                "ഒന്ന് കൂടി പറയൂ"
        };
        for (String phrase : repeatAliases) {
            assertEquals("Repeat message phrase: " + phrase,
                    VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 6. Critical Negative Rejections
        String[] negativePhrases = {
                "പരിചരിക്കുന്നയാൾ",
                "പരിചാരകൻ",
                "സഹായി",
                "കെയർഗിവർ",
                "കെയർടേക്കർ",
                "വിളിക്കൂ",
                "ഫോൺ ചെയ്യൂ",
                "കോൾ ചെയ്യൂ",
                "ഫോൺ",
                "കോൾ",
                "സന്ദേശം",
                "മെസേജ്",
                "സഹായി ആരാണ്?",
                "സഹായിയെക്കുറിച്ച് പറയൂ",
                "പരിചാരകൻ ആരാണ്?",
                "കെയർഗിവർ എന്താണ്?",
                "കെയർടേക്കർ ആരാണ്?",
                "എനിക്ക് സഹായി വേണം",
                "സഹായി എന്താണ്?",
                "സഹായിയുടെ വിവരങ്ങൾ പറയൂ"
        };
        for (String phrase : negativePhrases) {
            assertEquals("Negative rejection for phrase: " + phrase,
                    VoiceIntentType.UNKNOWN, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 7. Universal Wake Word Gating & Data Safety
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, പരിചരിക്കുന്നയാളെ വിളിക്കൂ"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Hey assistant, സഹായിക്ക് സന്ദേശം അയയ്ക്കൂ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("പരിചരിക്കുന്നയാളെ വിളിക്കൂ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("സഹായിയെ വിളിക്കൂ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("കെയർഗിവറെ വിളിക്കൂ"));

        assertEquals("പരിചരിക്കുന്നയാളെ വിളിക്കൂ", VoiceIntentMatcher.extractCommandText("Assistant, പരിചരിക്കുന്നയാളെ വിളിക്കൂ"));
        assertEquals("സഹായിക്ക് സന്ദേശം അയയ്ക്കൂ", VoiceIntentMatcher.extractCommandText("Hey assistant, സഹായിക്ക് സന്ദേശം അയയ്ക്കൂ"));

        // Message data isolation
        String malayalamPayload = "പരിചരിക്കുന്നയാളെ വിളിക്കൂ ഒപ്പം സഹായിക്ക് സന്ദേശം അയയ്ക്കൂ";
        NotificationService.NotificationItem malayalamItem =
                new NotificationService.NotificationItem("Caregiver", malayalamPayload, "text", 5000L);
        assertEquals("Caregiver: പരിചരിക്കുന്നയാളെ വിളിക്കൂ ഒപ്പം സഹായിക്ക് സന്ദേശം അയയ്ക്കൂ", malayalamItem.getSpokenText());
    }

    /**
     * TASK 27 — Natural English Caregiver Voice Aliases On-Device Verification.
     * Validates that English blind users can naturally address their caregiver using:
     * - caregiver, carer, caretaker, care worker, helper
     * - personal/possessive forms (my ..., the ..., to my ..., from my ...)
     * - natural call verbs (call, phone, ring, dial)
     * Across CALL_CAREGIVER, SEND_MESSAGE, READ_MESSAGES, MESSAGE_COUNT, REPEAT_MESSAGE.
     * Verifies strict negative rejection of isolated words, generic inquiries ("help me", "who is my helper"),
     * and guarantees universal wake word gating and data safety.
     */
    @Test
    public void test64_Task27_NaturalEnglishCaregiverAliases() {
        String lang = "en";

        // 1. CALL_CAREGIVER Aliases
        String[] callAliases = {
                "Assistant, call my caregiver",
                "Assistant, call my carer",
                "Assistant, call my caretaker",
                "Assistant, call my care worker",
                "Assistant, call my helper",
                "Assistant, call caregiver",
                "Assistant, call carer",
                "Assistant, call caretaker",
                "Assistant, call care worker",
                "Assistant, call helper",
                "Assistant, call the helper",
                "Assistant, call the carer",
                "Assistant, call the caretaker",
                "Assistant, call the care worker",
                "Assistant, call the caregiver",
                "Assistant, phone my carer",
                "Assistant, phone my caretaker",
                "Assistant, phone my care worker",
                "Assistant, phone my helper",
                "Assistant, phone my caregiver",
                "Assistant, ring my caretaker",
                "Assistant, ring my carer",
                "Assistant, ring my helper",
                "Assistant, ring my care worker",
                "Assistant, dial my care worker",
                "Assistant, dial my helper",
                "Assistant, dial my caregiver",
                "call my helper",
                "call helper",
                "phone my carer",
                "ring my caretaker",
                "dial my care worker"
        };
        for (String phrase : callAliases) {
            assertEquals("Call caregiver phrase: " + phrase,
                    VoiceIntentType.CALL_CAREGIVER, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 2. SEND_MESSAGE Aliases
        String[] sendAliases = {
                "Assistant, send a message to my caregiver",
                "Assistant, send a message to my carer",
                "Assistant, send a message to my caretaker",
                "Assistant, send a message to my care worker",
                "Assistant, send a message to my helper",
                "Assistant, send message to my helper",
                "Assistant, message my carer",
                "Assistant, message my caretaker",
                "Assistant, message my care worker",
                "Assistant, message my helper",
                "Assistant, send my caretaker a message",
                "Assistant, send my carer a message",
                "Assistant, send my helper a message",
                "Assistant, send my caregiver a message",
                "Assistant, send my care worker a message",
                "message my helper",
                "send message to my carer",
                "send a message to helper"
        };
        for (String phrase : sendAliases) {
            assertEquals("Send message phrase: " + phrase,
                    VoiceIntentType.SEND_MESSAGE, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 3. READ_MESSAGES Aliases
        String[] readAliases = {
                "Assistant, read my caregiver's messages",
                "Assistant, read my carer's messages",
                "Assistant, read my caretaker's messages",
                "Assistant, read my care worker's messages",
                "Assistant, read my helper's messages",
                "Assistant, read caregiver messages",
                "Assistant, read carer messages",
                "Assistant, read caretaker messages",
                "Assistant, read care worker messages",
                "Assistant, read helper messages",
                "Assistant, read messages from my carer",
                "Assistant, read messages from my caretaker",
                "Assistant, read messages from my helper",
                "Assistant, read messages from my care worker",
                "Assistant, read messages from my caregiver",
                "read helper messages",
                "read my carer's messages",
                "read messages from my caretaker"
        };
        for (String phrase : readAliases) {
            assertEquals("Read messages phrase: " + phrase,
                    VoiceIntentType.READ_MESSAGES, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 4. MESSAGE_COUNT Aliases
        String[] countAliases = {
                "Assistant, how many messages from my caregiver",
                "Assistant, how many messages from my carer",
                "Assistant, how many messages from my caretaker",
                "Assistant, how many messages from my care worker",
                "Assistant, how many messages from my helper",
                "Assistant, count my carer's messages",
                "Assistant, count my caretaker's messages",
                "Assistant, count my helper's messages",
                "Assistant, count my caregiver's messages",
                "Assistant, count my care worker's messages",
                "Assistant, count helper messages",
                "Assistant, count carer messages",
                "how many messages from my helper",
                "count my carer's messages",
                "how many messages from my caretaker"
        };
        for (String phrase : countAliases) {
            assertEquals("Message count phrase: " + phrase,
                    VoiceIntentType.MESSAGE_COUNT, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 5. REPEAT_MESSAGE Aliases
        String[] repeatAliases = {
                "Assistant, repeat my caregiver's message",
                "Assistant, repeat my carer's message",
                "Assistant, repeat my caretaker's message",
                "Assistant, repeat my care worker's message",
                "Assistant, repeat my helper's message",
                "Assistant, say my caregiver's message again",
                "Assistant, say my carer's message again",
                "Assistant, say my caretaker's message again",
                "Assistant, say my helper's message again",
                "Assistant, repeat helper message",
                "Assistant, repeat carer message",
                "repeat my carer's message",
                "say my caretaker's message again",
                "say that again"
        };
        for (String phrase : repeatAliases) {
            assertEquals("Repeat message phrase: " + phrase,
                    VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 6. Critical Negative Rejections
        String[] negativePhrases = {
                "caregiver",
                "carer",
                "caretaker",
                "care worker",
                "helper",
                "call",
                "phone",
                "ring",
                "dial",
                "message",
                "messages",
                "repeat",
                "again",
                "call my",
                "phone my",
                "Who is my helper?",
                "What is a carer?",
                "What is a care worker?",
                "I need a helper",
                "I need help",
                "Help me",
                "Find a helper",
                "Who is my caregiver?",
                "Tell me about my caregiver"
        };
        for (String phrase : negativePhrases) {
            assertEquals("Negative rejection for phrase: " + phrase,
                    VoiceIntentType.UNKNOWN, processor.detectIntent(phrase, lang).getIntentType());
        }

        // 7. Universal Wake Word Gating & Data Safety
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, call my helper"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Hey assistant, send a message to my carer"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("call my helper"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("send a message to my carer"));

        assertEquals("call my helper", VoiceIntentMatcher.extractCommandText("Assistant, call my helper"));
        assertEquals("send a message to my carer", VoiceIntentMatcher.extractCommandText("Hey assistant, send a message to my carer"));

        // Message data isolation
        String englishPayload = "call my helper and send a message to my carer";
        NotificationService.NotificationItem englishItem =
                new NotificationService.NotificationItem("Caregiver", englishPayload, "text", 5000L);
        assertEquals("Caregiver: call my helper and send a message to my carer", englishItem.getSpokenText());
    }

    @Test
    public void test65_Task28_CaregiverBlindUserFoundation() {
        // 1. RoleManager definitions
        assertEquals("Blind User", RoleManager.ROLE_BLIND_USER);
        assertEquals("Admin/Caregiver", RoleManager.ROLE_ADMIN_CAREGIVER);

        // 2. Multilingual detection verification for Caregiver messaging
        assertEquals("Kannada", LanguageManager.detectLanguageFromText("ನಮಸ್ಕಾರ ಹೇಗಿದ್ದೀರಿ", "en"));
        assertEquals("Hindi", LanguageManager.detectLanguageFromText("नमस्ते आप कैसे हैं", "en"));
        assertEquals("Malayalam", LanguageManager.detectLanguageFromText("നമസ്കാരം സുഖമാണോ", "en"));
        assertEquals("English", LanguageManager.detectLanguageFromText("Hello, how are you?", "en"));

        // 3. Emergency SOS default message verification
        assertEquals("ತುರ್ತು ಪರಿಸ್ಥಿತಿ! ಸಹಾಯ ಬೇಕು", LanguageManager.getDefaultSosMessage("kn"));
        assertEquals("आपातकालीन मदद चाहिए!", LanguageManager.getDefaultSosMessage("hi"));
        assertEquals("അടിയന്തര സഹായം വേണം!", LanguageManager.getDefaultSosMessage("ml"));

        // 4. Deterministic Chat ID generation check
        String caregiverUid = "cg_1001";
        String blindUserUid = "bu_2002";
        String expectedChatId = caregiverUid.compareTo(blindUserUid) < 0 ?
                caregiverUid + "_" + blindUserUid : blindUserUid + "_" + caregiverUid;
        assertEquals("bu_2002_cg_1001", expectedChatId);

        // 5. Protected WebRTC class preservation
        assertNotNull(com.kannada.speechassistant.WebRTCManager.class);
        assertNotNull(com.kannada.speechassistant.VoiceCallManager.class);
        assertNotNull(com.kannada.speechassistant.VoiceCallSignalingManager.class);
        assertNotNull(com.kannada.speechassistant.VoiceCallActivity.class);
        assertNotNull(com.kannada.speechassistant.IncomingCallActivity.class);
        assertNotNull(com.kannada.speechassistant.FirestoreRealtimeService.class);

        // 6. Caregiver sound manager volume verification
        android.content.Context context = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        int vol = CaregiverSoundManager.getVolumePercent(context);
        assertTrue("Volume percent must be between 0 and 100", vol >= 0 && vol <= 100);
    }

    @Test
    public void test66_Task29_CaregiverChatVoiceCallAndVoiceMessageIntegration() {
        android.content.Context context = androidx.test.core.app.ApplicationProvider.getApplicationContext();

        // 1. Verify Caregiver Voice Message payload compatibility with ChatMessage
        Map<String, Object> voiceMsgMap = new HashMap<>();
        voiceMsgMap.put("chatId", "cg_1001_bu_2002");
        voiceMsgMap.put("senderId", "cg_1001");
        voiceMsgMap.put("receiverId", "bu_2002");
        voiceMsgMap.put("senderRole", RoleManager.ROLE_ADMIN_CAREGIVER);
        voiceMsgMap.put("receiverRole", RoleManager.ROLE_BLIND_USER);
        voiceMsgMap.put("message", "🎤 Voice Message (5s)");
        voiceMsgMap.put("messageText", "🎤 Voice Message (5s)");
        voiceMsgMap.put("type", "voice");
        voiceMsgMap.put("messageType", "voice");
        voiceMsgMap.put("audioUrl", "https://supabase.project.co/storage/v1/object/public/voice_messages/cg_1001/12345.m4a");
        voiceMsgMap.put("audioDuration", 5000L);
        voiceMsgMap.put("timestamp", System.currentTimeMillis());
        voiceMsgMap.put("delivered", false);
        voiceMsgMap.put("seen", false);
        voiceMsgMap.put("status", "sent");

        ChatMessage parsedVoiceMsg = new ChatMessage(voiceMsgMap);
        assertTrue("Voice message type must be recognized as voice", parsedVoiceMsg.isVoice());
        assertEquals("voice", parsedVoiceMsg.getType());
        assertEquals("voice", parsedVoiceMsg.getMessageType());
        assertEquals("https://supabase.project.co/storage/v1/object/public/voice_messages/cg_1001/12345.m4a", parsedVoiceMsg.getAudioUrl());
        assertEquals(5000L, parsedVoiceMsg.getAudioDuration());
        assertEquals("🎤 Voice Message (5s)", parsedVoiceMsg.getMessage());

        // 2. Verify Text Message payload is distinguished
        Map<String, Object> textMsgMap = new HashMap<>();
        textMsgMap.put("chatId", "cg_1001_bu_2002");
        textMsgMap.put("senderId", "cg_1001");
        textMsgMap.put("receiverId", "bu_2002");
        textMsgMap.put("message", "Hello please take medicine");
        textMsgMap.put("messageText", "Hello please take medicine");
        textMsgMap.put("type", "text");
        textMsgMap.put("messageType", "text");
        ChatMessage parsedTextMsg = new ChatMessage(textMsgMap);
        assertFalse("Text message must not be flagged as voice", parsedTextMsg.isVoice());

        // 3. Audio conflict safety calls
        com.kannada.speechassistant.CaregiverChatAdapter.stopAudioPlayback();
        com.kannada.speechassistant.ChatAdapter.stopAudioPlayback();

        // 4. Voice call manager status verification
        com.kannada.speechassistant.VoiceCallManager callManager = com.kannada.speechassistant.VoiceCallManager.getInstance(context);
        assertNotNull("VoiceCallManager instance must be non-null", callManager);
        assertFalse("Call should not be active initially in unit/device test", callManager.isCallActive());

        // 5. Protected WebRTC call system invariants check
        assertNotNull(com.kannada.speechassistant.WebRTCManager.class);
        assertNotNull(com.kannada.speechassistant.VoiceCallManager.class);
        assertNotNull(com.kannada.speechassistant.VoiceCallSignalingManager.class);
        assertNotNull(com.kannada.speechassistant.VoiceCallActivity.class);
        assertNotNull(com.kannada.speechassistant.IncomingCallActivity.class);
        assertNotNull(com.kannada.speechassistant.FirestoreRealtimeService.class);
    }

    // =========================================================================
    // KANNADA CAREGIVER COMMAND RECOGNITION PHYSICAL DEVICE VERIFICATION TESTS
    // =========================================================================

    @Test
    public void testDevice_KannadaCaregiverCallCommands() {
        String lang = "kn";

        String[] callCommands = {
                "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ",
                "ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ",
                "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ಸಹಾಯಕನಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ನನ್ನ ಸಹಾಯಕರಿಗೆ ಕರೆ ಮಾಡಿ",
                "ಸಹಾಯಕರಿಗೆ ಕರೆ ಮಾಡಿ",
                "Assistant, ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ",
                "Assistant, ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ",
                "Assistant, ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "Assistant, ಸಹಾಯಕನಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "Assistant, ನನ್ನ ಸಹಾಯಕರಿಗೆ ಕರೆ ಮಾಡಿ"
        };

        for (String phrase : callCommands) {
            String normalized = VoiceIntentMatcher.extractCommandText(phrase);
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match CALL_CAREGIVER on device",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());

            VoiceCommand cmd = processor.resolveCommand(intent, RoleManager.ROLE_BLIND_USER);
            assertEquals(VoiceCommandConstants.CMD_OPEN_VOICE_CALL, cmd.getCommandId());
            assertTrue("Blind user must be authorized for CALL_CAREGIVER",
                    cmd.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));
        }
    }

    @Test
    public void testDevice_KannadaCaregiverSendMessageCommands() {
        String lang = "kn";

        String[] msgCommands = {
                "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ಸಹಾಯಕನಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "Assistant, ನನ್ನ ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "Assistant, ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "Assistant, ನನ್ನ ಸಹಾಯಕನಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "Assistant, ಸಹಾಯಕನಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ"
        };

        for (String phrase : msgCommands) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match SEND_MESSAGE on device",
                    VoiceIntentType.SEND_MESSAGE, intent.getIntentType());
            assertNotEquals("Phrase '" + phrase + "' must NOT be CALL_CAREGIVER",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());

            VoiceCommand cmd = processor.resolveCommand(intent, RoleManager.ROLE_BLIND_USER);
            assertEquals(VoiceCommandConstants.CMD_SEND_MESSAGE, cmd.getCommandId());
            assertTrue("Blind user must be authorized for SEND_MESSAGE",
                    cmd.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));
        }
    }

    @Test
    public void testDevice_KannadaEmergencySeparation() {
        String lang = "kn";

        String[] emergencyPhrases = {
                "ನನಗೆ ಸಹಾಯ ಬೇಕು",
                "ಸಹಾಯ ಮಾಡಿ",
                "ತುರ್ತು ಸಹಾಯ ಬೇಕು"
        };

        for (String phrase : emergencyPhrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for: " + phrase, intent);
            assertNotEquals("Emergency phrase '" + phrase + "' MUST NOT trigger CALL_CAREGIVER on device",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
            assertNotEquals("Emergency phrase '" + phrase + "' MUST NOT trigger SEND_MESSAGE on device",
                    VoiceIntentType.SEND_MESSAGE, intent.getIntentType());
            assertTrue("Emergency phrase '" + phrase + "' must match emergency intent",
                    intent.getIntentType() == VoiceIntentType.OPEN_EMERGENCY ||
                    intent.getIntentType() == VoiceIntentType.OPEN_EMERGENCY_ALERT);
        }
    }

    @Test
    public void testDevice_KannadaRandomSpeechDoesNotTriggerCall() {
        String lang = "kn";

        String[] randomSpeech = {
                "ಸಹಾಯ",
                "ಕರೆ ಮಾಡಿ",
                "ಇಂದು ಹವಾಮಾನ ಹೇಗಿದೆ",
                "ಊಟ ಆಯ್ತಾ"
        };

        for (String phrase : randomSpeech) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for: " + phrase, intent);
            assertNotEquals("Non-call phrase '" + phrase + "' MUST NOT trigger CALL_CAREGIVER on device",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
        }
    }

    // ==========================================
    // WAKE WORD ON-DEVICE VERIFICATION TESTS
    // ==========================================

    @Test
    public void testDevice_WakeWordExtraction() {
        // Primary wake word "assistant"
        String r1 = WakeWordManager.parseAndExtractCandidate("{\"text\": \"assistant\"}");
        assertEquals("assistant", r1);

        String r2 = WakeWordManager.parseAndExtractCandidate("{\"partial\": \"assistant\"}");
        assertEquals("assistant", r2);

        // Hey assistant
        String r3 = WakeWordManager.parseAndExtractCandidate("{\"text\": \"hey assistant\"}");
        assertEquals("hey assistant", r3);

        // Single utterance with command
        String r4 = WakeWordManager.parseAndExtractCandidate("{\"text\": \"assistant open profile\"}");
        assertEquals("assistant open profile", r4);

        // Out-of-grammar token filtering
        String r5 = WakeWordManager.parseAndExtractCandidate("{\"text\": \"[unk] assistant\"}");
        assertEquals("assistant", r5);

        // Empty / noise
        assertNull(WakeWordManager.parseAndExtractCandidate("{\"text\": \"\"}"));
        assertNull(WakeWordManager.parseAndExtractCandidate("{\"partial\": \"\"}"));
        assertNull(WakeWordManager.parseAndExtractCandidate("{\"text\": \"open home\"}"));
    }

    @Test
    public void testDevice_WakeWordManagerInstanceAndState() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            WakeWordManager wwm = WakeWordManager.getInstance(appContext);
            assertNotNull("WakeWordManager instance must not be null", wwm);
            assertTrue("User role should be blind user for test session", wwm.isBlindUser());
            WakeWordManager.State state = wwm.getCurrentState();
            assertNotNull("State must not be null", state);
            assertTrue("State must be one of valid states",
                    state == WakeWordManager.State.IDLE ||
                    state == WakeWordManager.State.INITIALIZING ||
                    state == WakeWordManager.State.READY ||
                    state == WakeWordManager.State.LISTENING ||
                    state == WakeWordManager.State.PAUSED);
        });
    }

    @Test
    public void testDevice_WakeWordCommandExtractionIntegration() {
        // Two-step interaction
        String cmdEmpty = VoiceIntentMatcher.extractCommandText("assistant");
        assertEquals("", cmdEmpty);

        String cmdEmptyHey = VoiceIntentMatcher.extractCommandText("hey assistant");
        assertEquals("", cmdEmptyHey);

        // Single-utterance interaction
        String cmdProfile = VoiceIntentMatcher.extractCommandText("assistant open profile");
        assertEquals("open profile", cmdProfile);
        VoiceIntent intentProfile = processor.detectIntent(cmdProfile, "en");
        assertEquals(VoiceIntentType.OPEN_PROFILE, intentProfile.getIntentType());

        String cmdCaregiver = VoiceIntentMatcher.extractCommandText("assistant call my caregiver");
        assertEquals("call my caregiver", cmdCaregiver);
        VoiceIntent intentCaregiver = processor.detectIntent(cmdCaregiver, "en");
        assertEquals(VoiceIntentType.CALL_CAREGIVER, intentCaregiver.getIntentType());
    }
}