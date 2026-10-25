package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;
import com.kannada.speechassistant.ChatMessage;
import com.kannada.speechassistant.BlindUserDashboardActivity;
import com.kannada.speechassistant.RoleManager;

import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 100% Comprehensive Unit Tests for the Voice-Controlled Delete Last Message feature
 * strictly for the Blind User Module.
 */
public class BlindUserDeleteLastMessageTest {

    // =========================================================================
    // 1. COMMAND RECOGNITION ACROSS LANGUAGES
    // =========================================================================

    @Test
    public void testEnglishCommandsAvailableInAllLanguageSettings() {
        String[] englishCommands = {
                "Hey Assistant, delete last message.",
                "Delete my last message.",
                "Assistant, delete my last message.",
                "delete last message",
                "delete the last message",
                "delete my last sent message",
                "delete last sent message",
                "delete recent message",
                "delete latest message"
        };

        String[] langCodes = {"en", "kn", "hi", "ml"};
        for (String lang : langCodes) {
            for (String cmd : englishCommands) {
                VoiceIntent intent = VoiceIntentMatcher.matchIntent(cmd, lang);
                assertEquals("Failed for cmd '" + cmd + "' in lang '" + lang + "'",
                        VoiceIntentType.DELETE_LAST_MESSAGE, intent.getIntentType());
            }
        }
    }

    @Test
    public void testKannadaCommandsWhenKannadaSelected() {
        String[] kannadaCommands = {
                "ಕೊನೆಯ ಸಂದೇಶವನ್ನು ಅಳಿಸು",
                "ನಾನು ಕಳುಹಿಸಿದ ಕೊನೆಯ ಸಂದೇಶವನ್ನು ಅಳಿಸು",
                "ಕೊನೆಯ ಸಂದೇಶ ಅಳಿಸು",
                "ಕೊನೆಯ ಸಂದೇಶವನ್ನು ಅಳಿಸಿ",
                "ನನ್ನ ಕೊನೆಯ ಸಂದೇಶ ಅಳಿಸು",
                "ಕೊನೆಯ ಮೆಸೇಜ್ ಅಳಿಸು",
                "ನನ್ನ ಕೊನೆಯ ಸಂದೇಶ ಡಿಲೀಟ್ ಮಾಡು"
        };

        for (String cmd : kannadaCommands) {
            VoiceIntent intent = VoiceIntentMatcher.matchIntent(cmd, "kn");
            assertEquals("Failed for Kannada command: " + cmd,
                    VoiceIntentType.DELETE_LAST_MESSAGE, intent.getIntentType());
        }
    }

    @Test
    public void testHindiCommandsWhenHindiSelected() {
        String[] hindiCommands = {
                "आखिरी संदेश हटाओ",
                "मेरा आखिरी भेजा हुआ संदेश हटाओ",
                "आखिरी संदेश हटाएं",
                "मेरा आखिरी संदेश हटाओ",
                "आखिरी मैसेज हटाओ",
                "आखिरी संदेश डिलीट करो"
        };

        for (String cmd : hindiCommands) {
            VoiceIntent intent = VoiceIntentMatcher.matchIntent(cmd, "hi");
            assertEquals("Failed for Hindi command: " + cmd,
                    VoiceIntentType.DELETE_LAST_MESSAGE, intent.getIntentType());
        }
    }

    @Test
    public void testMalayalamCommandsWhenMalayalamSelected() {
        String[] malayalamCommands = {
                "അവസാന സന്ദേശം ഇല്ലാതാക്കൂ",
                "ഞാൻ അവസാനം അയച്ച സന്ദേശം ഇല്ലാതാക്കൂ",
                "അവസാന സന്ദേശം ഇല്ലാതാക്കുക",
                "എന്റെ അവസാന സന്ദേശം ഇല്ലാതാക്കൂ",
                "അവസാന മെസേജ് ഇല്ലാതാക്കൂ",
                "അവസാന സന്ദേശം ഒഴിവാക്കൂ"
        };

        for (String cmd : malayalamCommands) {
            VoiceIntent intent = VoiceIntentMatcher.matchIntent(cmd, "ml");
            assertEquals("Failed for Malayalam command: " + cmd,
                    VoiceIntentType.DELETE_LAST_MESSAGE, intent.getIntentType());
        }
    }

    // =========================================================================
    // 2. STRICT LANGUAGE ISOLATION
    // =========================================================================

    @Test
    public void testStrictLanguageIsolationBetweenRegionalLanguages() {
        // Kannada command must NOT match when Hindi or Malayalam or English is active
        VoiceIntent knInHi = VoiceIntentMatcher.matchIntent("ಕೊನೆಯ ಸಂದೇಶವನ್ನು ಅಳಿಸು", "hi");
        assertFalse("Kannada must not match in Hindi mode", knInHi.getIntentType() == VoiceIntentType.DELETE_LAST_MESSAGE);

        VoiceIntent knInMl = VoiceIntentMatcher.matchIntent("ಕೊನೆಯ ಸಂದೇಶವನ್ನು ಅಳಿಸು", "ml");
        assertFalse("Kannada must not match in Malayalam mode", knInMl.getIntentType() == VoiceIntentType.DELETE_LAST_MESSAGE);

        // Hindi command must NOT match when Kannada or Malayalam is active
        VoiceIntent hiInKn = VoiceIntentMatcher.matchIntent("आखिरी संदेश हटाओ", "kn");
        assertFalse("Hindi must not match in Kannada mode", hiInKn.getIntentType() == VoiceIntentType.DELETE_LAST_MESSAGE);

        VoiceIntent hiInMl = VoiceIntentMatcher.matchIntent("आखिरी संदेश हटाओ", "ml");
        assertFalse("Hindi must not match in Malayalam mode", hiInMl.getIntentType() == VoiceIntentType.DELETE_LAST_MESSAGE);

        // Malayalam command must NOT match when Kannada or Hindi is active
        VoiceIntent mlInKn = VoiceIntentMatcher.matchIntent("അവസാന സന്ദേശം ഇല്ലാതാക്കൂ", "kn");
        assertFalse("Malayalam must not match in Kannada mode", mlInKn.getIntentType() == VoiceIntentType.DELETE_LAST_MESSAGE);

        VoiceIntent mlInHi = VoiceIntentMatcher.matchIntent("അവസാന സന്ദേശം ഇല്ലാതാക്കൂ", "hi");
        assertFalse("Malayalam must not match in Hindi mode", mlInHi.getIntentType() == VoiceIntentType.DELETE_LAST_MESSAGE);
    }

    @Test
    public void testBareTokensDoNotAccidentallyTriggerDeletion() {
        String[] bareTokens = {"delete", "remove", "clear", "ಅಳಿಸು", "हटाओ", "ഇല്ലാതാക്കൂ", "message", "last"};
        for (String token : bareTokens) {
            VoiceIntent intent = VoiceIntentMatcher.matchIntent(token, "en");
            assertFalse("Bare token '" + token + "' must not trigger delete",
                    intent.getIntentType() == VoiceIntentType.DELETE_LAST_MESSAGE);
        }
    }

    // =========================================================================
    // 3. ROLE AUTHORIZATION
    // =========================================================================

    @Test
    public void testRoleAuthorizationBlindAndDeafUsers() {
        VoiceCommandProcessor processor = new VoiceCommandProcessor();
        VoiceIntent deleteIntent = new VoiceIntent(VoiceIntentType.DELETE_LAST_MESSAGE, "delete last message");

        // Blind and Deaf users must be authorized
        VoiceCommand cmd = processor.resolveCommand(deleteIntent, RoleManager.ROLE_BLIND_USER);
        assertTrue("Blind user must be authorized", cmd.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));
        assertTrue("Deaf user must be authorized", cmd.isRoleAuthorized(RoleManager.ROLE_DEAF_USER));
        assertTrue("Speech-impaired user must be authorized", cmd.isRoleAuthorized(RoleManager.ROLE_SPEECH_IMPAIRED));

        // Other roles must be unauthorized
        assertFalse("Caregiver must NOT be authorized", cmd.isRoleAuthorized(RoleManager.ROLE_ADMIN_CAREGIVER));
        assertFalse("Mute user must NOT be authorized", cmd.isRoleAuthorized(RoleManager.ROLE_MUTE_USER));
    }

    // =========================================================================
    // 4. CORRECT MESSAGE SELECTION & ELIGIBILITY FILTERS
    // =========================================================================

    private ChatMessage createMockMessage(String id, String senderId, String text, String type, boolean isVoice) {
        Map<String, Object> map = new HashMap<>();
        map.put("messageId", id);
        map.put("senderId", senderId);
        map.put("receiverId", "caregiver_456");
        map.put("message", text);
        map.put("messageText", text);
        map.put("type", type);
        map.put("messageType", type);
        map.put("status", "sent");
        map.put("timestamp", new Timestamp(new Date()));
        if (isVoice) {
            map.put("audioUrl", "https://example.com/audio.m4a");
            map.put("audioDuration", 5L);
        }
        return new ChatMessage(map);
    }

    @Test
    public void testOutgoingEligibilityFilterAcceptsOnlyBlindUserSentMessages() {
        String myUid = "blind_user_123";

        // 1. Valid text message sent by blind user
        ChatMessage validText = createMockMessage("msg_1", myUid, "Hello caregiver", "text", false);
        assertTrue("Valid text message must be eligible",
                BlindUserDashboardActivity.isEligibleOutgoingBlindUserMessage(validText, myUid));

        // 2. Valid voice message sent by blind user
        ChatMessage validVoice = createMockMessage("msg_2", myUid, "🎤 Voice Message", "voice", true);
        assertTrue("Valid voice message must be eligible",
                BlindUserDashboardActivity.isEligibleOutgoingBlindUserMessage(validVoice, myUid));

        // 3. Incoming message from Caregiver must NEVER be eligible
        ChatMessage incomingCaregiver = createMockMessage("msg_3", "caregiver_456", "How are you?", "text", false);
        assertFalse("Incoming caregiver message must NEVER be eligible for deletion",
                BlindUserDashboardActivity.isEligibleOutgoingBlindUserMessage(incomingCaregiver, myUid));

        // 4. Another user's message must NEVER be eligible
        ChatMessage otherUserMsg = createMockMessage("msg_4", "other_user_789", "Hi", "text", false);
        assertFalse("Another user's message must NEVER be eligible",
                BlindUserDashboardActivity.isEligibleOutgoingBlindUserMessage(otherUserMsg, myUid));

        // 5. Emergency SOS records must NEVER be eligible for voice deletion
        ChatMessage emergencyRecord = createMockMessage("msg_5", myUid, "ತುರ್ತು ಪರಿಸ್ಥಿತಿ! ಸಹಾಯ ಬೇಕು", "emergency", false);
        assertFalse("Emergency record must NEVER be selected for voice deletion",
                BlindUserDashboardActivity.isEligibleOutgoingBlindUserMessage(emergencyRecord, myUid));

        // 6. Call records must NEVER be eligible
        ChatMessage callRecord = createMockMessage("msg_6", myUid, "Voice Call", "call", false);
        assertFalse("Call record must NEVER be selected for voice deletion",
                BlindUserDashboardActivity.isEligibleOutgoingBlindUserMessage(callRecord, myUid));

        // 7. System notification records must NEVER be eligible
        ChatMessage systemRecord = createMockMessage("msg_7", myUid, "System alert", "system", false);
        assertFalse("System alert must NEVER be selected for voice deletion",
                BlindUserDashboardActivity.isEligibleOutgoingBlindUserMessage(systemRecord, myUid));

        // 8. Message with empty ID must be rejected
        ChatMessage emptyIdMsg = createMockMessage("", myUid, "Valid text", "text", false);
        assertFalse("Empty ID message must be rejected",
                BlindUserDashboardActivity.isEligibleOutgoingBlindUserMessage(emptyIdMsg, myUid));
    }

    // =========================================================================
    // 5. CONFIRMATION PROMPTS & LOCALIZED RESPONSES
    // =========================================================================

    @Test
    public void testConfirmationPromptsFormattedCorrectly() {
        // English
        String enPrompt = VoiceLanguageConfig.getConfirmDeleteMessagePrompt("en", "I will be late");
        assertEquals("Your last sent message is: I will be late. Do you want to delete it? Say 'Confirm delete' or 'Cancel'.", enPrompt);

        // Kannada
        String knPrompt = VoiceLanguageConfig.getConfirmDeleteMessagePrompt("kn", "ನಾನು ಬರುತ್ತಿದ್ದೇನೆ");
        assertEquals("ನೀವು ಕೊನೆಯದಾಗಿ ಕಳುಹಿಸಿದ ಸಂದೇಶ: ನಾನು ಬರುತ್ತಿದ್ದೇನೆ. ಇದನ್ನು ಅಳಿಸಬೇಕೇ? ಅಳಿಸಲು ದೃಢೀಕರಿಸಿ ಅಥವಾ ರದ್ದುಮಾಡಿ ಎಂದು ಹೇಳಿ.", knPrompt);

        // Hindi
        String hiPrompt = VoiceLanguageConfig.getConfirmDeleteMessagePrompt("hi", "मैं आ रहा हूँ");
        assertEquals("आपका आखिरी भेजा गया संदेश: मैं आ रहा हूँ। क्या आप इसे हटाना चाहते हैं? पुष्टि करें या रद्द करें।", hiPrompt);

        // Malayalam
        String mlPrompt = VoiceLanguageConfig.getConfirmDeleteMessagePrompt("ml", "ഞാൻ വരുന്നു");
        assertEquals("നിങ്ങൾ അവസാനം അയച്ച സന്ദേശം: ഞാൻ വരുന്നു. ഇത് ഇല്ലാതാക്കണോ? സ്ഥിരീകരിക്കുക അല്ലെങ്കിൽ റദ്ദാക്കുക.", mlPrompt);
    }

    @Test
    public void testLocalizedResponsesForEachLanguage() {
        // English
        assertEquals(VoiceCommandConstants.RESPONSE_NO_MESSAGE_TO_DELETE,
                VoiceLanguageConfig.getNoMessageToDeleteResponse("en"));
        assertEquals(VoiceCommandConstants.RESPONSE_MESSAGE_DELETED,
                VoiceLanguageConfig.getMessageDeletedResponse("en"));
        assertEquals(VoiceCommandConstants.RESPONSE_DELETE_CANCELLED,
                VoiceLanguageConfig.getDeleteCancelledResponse("en"));
        assertEquals(VoiceCommandConstants.RESPONSE_DELETE_FAILED,
                VoiceLanguageConfig.getDeleteFailedResponse("en"));

        // Kannada
        assertEquals("ಅಳಿಸಲು ನೀವು ಕಳುಹಿಸಿದ ಯಾವುದೇ ಸಂದೇಶಗಳಿಲ್ಲ.",
                VoiceLanguageConfig.getNoMessageToDeleteResponse("kn"));
        assertEquals("ನಿಮ್ಮ ಕೊನೆಯ ಸಂದೇಶವನ್ನು ಅಳಿಸಲಾಗಿದೆ.",
                VoiceLanguageConfig.getMessageDeletedResponse("kn"));
        assertEquals("ಸಂದೇಶ ಅಳಿಸುವಿಕೆಯನ್ನು ರದ್ದುಗೊಳಿಸಲಾಗಿದೆ.",
                VoiceLanguageConfig.getDeleteCancelledResponse("kn"));
        assertEquals("ಸಂದೇಶವನ್ನು ಅಳಿಸಲು ಸಾಧ್ಯವಾಗಲಿಲ್ಲ. ದಯವಿಟ್ಟು ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ.",
                VoiceLanguageConfig.getDeleteFailedResponse("kn"));

        // Hindi
        assertEquals("हटाने के लिए आपका कोई भेजा गया संदेश नहीं है।",
                VoiceLanguageConfig.getNoMessageToDeleteResponse("hi"));
        assertEquals("आपका आखिरी संदेश हटा दिया गया है।",
                VoiceLanguageConfig.getMessageDeletedResponse("hi"));
        assertEquals("संदेश हटाना रद्द कर दिया गया।",
                VoiceLanguageConfig.getDeleteCancelledResponse("hi"));
        assertEquals("संदेश हटाया नहीं जा सका। कृपया पुनः प्रयास करें।",
                VoiceLanguageConfig.getDeleteFailedResponse("hi"));

        // Malayalam
        assertEquals("ഇല്ലാതാക്കാൻ നിങ്ങൾ അയച്ച സന്ദേശങ്ങളൊന്നുമില്ല.",
                VoiceLanguageConfig.getNoMessageToDeleteResponse("ml"));
        assertEquals("നിങ്ങളുടെ അവസാന സന്ദേശം ഇല്ലാതാക്കി.",
                VoiceLanguageConfig.getMessageDeletedResponse("ml"));
        assertEquals("സന്ദേശം ഇല്ലാതാക്കുന്നത് റദ്ദാക്കി.",
                VoiceLanguageConfig.getDeleteCancelledResponse("ml"));
        assertEquals("സന്ദേശം ഇല്ലാതാക്കാൻ കഴിഞ്ഞില്ല. ദയവായി വീണ്ടും ശ്രമിക്കുക.",
                VoiceLanguageConfig.getDeleteFailedResponse("ml"));
    }

    // =========================================================================
    // 6. CONFIRMATION & CANCELLATION COMMANDS MATCHING
    // =========================================================================

    @Test
    public void testConfirmationAndCancellationPhrases() {
        // Affirmative English phrases
        assertEquals(VoiceIntentType.CONFIRM_YES, VoiceIntentMatcher.matchIntent("confirm delete").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_YES, VoiceIntentMatcher.matchIntent("yes delete").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_YES, VoiceIntentMatcher.matchIntent("delete it").getIntentType());

        // Negative English phrases
        assertEquals(VoiceIntentType.CONFIRM_NO, VoiceIntentMatcher.matchIntent("cancel delete").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_NO, VoiceIntentMatcher.matchIntent("don't delete").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_NO, VoiceIntentMatcher.matchIntent("do not delete").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_NO, VoiceIntentMatcher.matchIntent("cancel").getIntentType());

        // Affirmative regional phrases
        assertEquals(VoiceIntentType.CONFIRM_YES, VoiceIntentMatcher.matchIntent("ಅಳಿಸಲು ದೃಢೀಕರಿಸಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_YES, VoiceIntentMatcher.matchIntent("ಹೌದು ಅಳಿಸಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_YES, VoiceIntentMatcher.matchIntent("पुष्टि करें", "hi").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_YES, VoiceIntentMatcher.matchIntent("हाँ हटाओ", "hi").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_YES, VoiceIntentMatcher.matchIntent("സ്ഥിരീകരിക്കുക", "ml").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_YES, VoiceIntentMatcher.matchIntent("അതെ ഇല്ലാതാക്കൂ", "ml").getIntentType());

        // Negative regional phrases
        assertEquals(VoiceIntentType.CONFIRM_NO, VoiceIntentMatcher.matchIntent("ರದ್ದುಮಾಡಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_NO, VoiceIntentMatcher.matchIntent("ಅಳಿಸಬೇಡಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_NO, VoiceIntentMatcher.matchIntent("रद्द करें", "hi").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_NO, VoiceIntentMatcher.matchIntent("मत हटाओ", "hi").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_NO, VoiceIntentMatcher.matchIntent("റദ്ദാക്കുക", "ml").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_NO, VoiceIntentMatcher.matchIntent("ഇല്ലാതാക്കരുത്", "ml").getIntentType());
    }
}
