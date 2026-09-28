package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.kannada.speechassistant.NotificationService;
import com.kannada.speechassistant.CaregiverSoundManager;

/**
 * Validates lightweight intent matching using the clear VoiceIntentType model,
 * ensuring users do not have to speak an exact fragile sentence.
 */
public class LightweightIntentMatchingTest {

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
    public void testOpenMessagesVariations() {
        String lang = "en";

        // Required examples for OPEN_MESSAGES:
        // "Open messages", "Show messages", "Go to messages", "Open my messages", "Show my messages"
        String[] variations = {
                "Open messages",
                "Show messages",
                "Go to messages",
                "Open my messages",
                "Show my messages",
                "Please show my messages right now",
                "Can you open messages for me",
                "View my messages"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.OPEN_MESSAGES",
                    VoiceIntentType.OPEN_MESSAGES, intent.getIntentType());
            assertEquals("Phrase '" + phrase + "' should have INTENT_OPEN_MESSAGES name",
                    VoiceCommandConstants.INTENT_OPEN_MESSAGES, intent.getIntentName());
        }
    }

    @Test
    public void testCallCaregiverVariations() {
        String lang = "en";

        // Required examples for CALL_CAREGIVER:
        // "Call caregiver", "Call my caregiver", "Call caretaker", "Call my caretaker"
        String[] variations = {
                "Call caregiver",
                "Call my caregiver",
                "Call caretaker",
                "Call my caretaker",
                "Phone my caregiver",
                "Start a call with caregiver",
                "Please call my caregiver",
                "Start voice call",
                "Open voice call",
                "Phone call caregiver"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.CALL_CAREGIVER",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
            assertEquals("Phrase '" + phrase + "' should map to voice call intent ID",
                    VoiceCommandConstants.INTENT_OPEN_VOICE_CALL, intent.getIntentName());
        }

        // Generic words alone must NOT trigger CALL_CAREGIVER
        VoiceIntent genericCall = processor.detectIntent("call", lang);
        assertNotEquals("Generic 'call' alone must not match CALL_CAREGIVER",
                VoiceIntentType.CALL_CAREGIVER, genericCall.getIntentType());

        VoiceIntent genericPhone = processor.detectIntent("phone", lang);
        assertNotEquals("Generic 'phone' alone must not match CALL_CAREGIVER",
                VoiceIntentType.CALL_CAREGIVER, genericPhone.getIntentType());
    }

    @Test
    public void testOpenVoiceRecorderVariations() {
        String lang = "en";

        // Required examples for OPEN_VOICE_RECORDER:
        // "Open voice recorder", "Start voice recorder", "Open recorder", "Record a voice message"
        String[] variations = {
                "Open voice recorder",
                "Start voice recorder",
                "Open recorder",
                "Record a voice message",
                "Please open recorder",
                "Start audio recorder",
                "Record audio message"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.OPEN_VOICE_RECORDER",
                    VoiceIntentType.OPEN_VOICE_RECORDER, intent.getIntentType());
            assertEquals("Phrase '" + phrase + "' should map to voice recorder intent ID",
                    VoiceCommandConstants.INTENT_OPEN_VOICE_RECORDER, intent.getIntentName());
        }
    }

    @Test
    public void testClearIntentEnumModel() {
        String lang = "en";

        // OPEN_HOME
        assertEquals(VoiceIntentType.OPEN_HOME,
                processor.detectIntent("Take me home please", lang).getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME,
                processor.detectIntent("Open main dashboard screen", lang).getIntentType());

        // OPEN_PROFILE
        assertEquals(VoiceIntentType.OPEN_PROFILE,
                processor.detectIntent("Show my profile details", lang).getIntentType());
        assertEquals(VoiceIntentType.OPEN_PROFILE,
                processor.detectIntent("Open user account", lang).getIntentType());

        // OPEN_SETTINGS
        assertEquals(VoiceIntentType.OPEN_SETTINGS,
                processor.detectIntent("Open sound volume settings", lang).getIntentType());
        assertEquals(VoiceIntentType.OPEN_SETTINGS,
                processor.detectIntent("Show my notification settings", lang).getIntentType());

        // OPEN_CAREGIVER
        assertEquals(VoiceIntentType.OPEN_CAREGIVER,
                processor.detectIntent("Open my caregiver", lang).getIntentType());
        assertEquals(VoiceIntentType.OPEN_CAREGIVER,
                processor.detectIntent("Show caregiver helper", lang).getIntentType());

        // OPEN_EMERGENCY
        assertEquals(VoiceIntentType.OPEN_EMERGENCY,
                processor.detectIntent("Help me danger emergency SOS", lang).getIntentType());
        assertEquals(VoiceIntentType.OPEN_EMERGENCY,
                processor.detectIntent("Urgent emergency help needed", lang).getIntentType());
    }

    @Test
    public void testOpenProfileVariations() {
        String[] variations = {
                "open profile",
                "show profile",
                "go to profile",
                "profile",
                "open my profile",
                "show my profile",
                "my profile"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, "en");
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.OPEN_PROFILE",
                    VoiceIntentType.OPEN_PROFILE, intent.getIntentType());
            assertEquals(VoiceCommandConstants.INTENT_OPEN_PROFILE, intent.getIntentName());
        }

        // Multilingual variations
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("ಪ್ರೊಫೈಲ್", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("ನನ್ನ ಪ್ರೊಫೈಲ್", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("प्रोफ़ाइल", "hi").getIntentType());
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("मेरी प्रोफ़ाइल", "hi").getIntentType());
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("പ്രൊഫൈൽ", "ml").getIntentType());
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("എന്റെ പ്രൊഫൈൽ", "ml").getIntentType());
    }

    @Test
    public void testOpenHomeVariations() {
        String[] variations = {
                "open home",
                "go home",
                "show home",
                "home",
                "open home screen",
                "go to home",
                "take me home"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, "en");
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.OPEN_HOME",
                    VoiceIntentType.OPEN_HOME, intent.getIntentType());
            assertEquals(VoiceCommandConstants.INTENT_OPEN_HOME, intent.getIntentName());
        }

        // Multilingual variations
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("ಹೋಮ್", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("ಮುಖಪುಟ", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("होम", "hi").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("मुख्य पृष्ठ", "hi").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("ഹോം", "ml").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("പ്രധാന പേജ്", "ml").getIntentType());
    }

    @Test
    public void testOpenSettingsVariations() {
        String[] variations = {
                "open settings",
                "go to settings",
                "show settings",
                "settings",
                "sound settings",
                "volume settings",
                "notification settings"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, "en");
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.OPEN_SETTINGS",
                    VoiceIntentType.OPEN_SETTINGS, intent.getIntentType());
            assertEquals(VoiceCommandConstants.INTENT_OPEN_SETTINGS, intent.getIntentName());
        }

        // Multilingual variations
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("ಸೆಟ್ಟಿಂಗ್ಸ್", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("ಸೆಟ್ಟಿಂಗ್", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("सेटिंग्स", "hi").getIntentType());
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("सेटिंग", "hi").getIntentType());
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("സെറ്റിംഗ്സ്", "ml").getIntentType());
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("സെറ്റിംഗ്", "ml").getIntentType());
    }

    @Test
    public void testEndCallVariations() {
        String lang = "en";

        // Required examples for END_CALL:
        // "end call", "hang up", "disconnect call", "end the call", "hang up the call"
        String[] variations = {
                "end call",
                "hang up",
                "disconnect call",
                "end the call",
                "hang up the call"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.END_CALL",
                    VoiceIntentType.END_CALL, intent.getIntentType());
            assertEquals("Phrase '" + phrase + "' should map to end call intent ID",
                    VoiceCommandConstants.INTENT_END_CALL, intent.getIntentName());
        }

        // "stop" must resolve to STOP_LISTENING, NOT END_CALL
        VoiceIntent stopIntent = processor.detectIntent("stop", lang);
        assertEquals("Phrase 'stop' must match STOP_LISTENING",
                VoiceIntentType.STOP_LISTENING, stopIntent.getIntentType());
        assertNotEquals("Phrase 'stop' must not match END_CALL",
                VoiceIntentType.END_CALL, stopIntent.getIntentType());

        // Generic isolated words alone must NOT trigger END_CALL
        String[] genericRejections = {"call", "end", "disconnect", "phone", "cut"};
        for (String word : genericRejections) {
            VoiceIntent intent = processor.detectIntent(word, lang);
            assertNotEquals("Isolated word '" + word + "' must NOT match END_CALL",
                    VoiceIntentType.END_CALL, intent.getIntentType());
        }
    }

    @Test
    public void testEmergencyVariations() {
        String lang = "en";

        // Required examples for EMERGENCY:
        // "emergency help", "send emergency", "sos", "send sos"
        String[] variations = {
                "emergency help",
                "send emergency",
                "sos",
                "send sos"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.OPEN_EMERGENCY",
                    VoiceIntentType.OPEN_EMERGENCY, intent.getIntentType());
            assertEquals("Phrase '" + phrase + "' should map to emergency intent ID",
                    VoiceCommandConstants.INTENT_OPEN_EMERGENCY, intent.getIntentName());
        }

        // Generic isolated words alone must NOT trigger OPEN_EMERGENCY
        String[] genericRejections = {"help", "emergency", "send", "alert"};
        for (String word : genericRejections) {
            VoiceIntent intent = processor.detectIntent(word, lang);
            assertNotEquals("Isolated word '" + word + "' must NOT match OPEN_EMERGENCY",
                    VoiceIntentType.OPEN_EMERGENCY, intent.getIntentType());
        }

        // Multilingual variations
        assertEquals(VoiceIntentType.OPEN_EMERGENCY, processor.detectIntent("ತುರ್ತು ಸಹಾಯ", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_EMERGENCY, processor.detectIntent("आपातकालीन सहायता", "hi").getIntentType());
        assertEquals(VoiceIntentType.OPEN_EMERGENCY, processor.detectIntent("അടിയന്തര സഹായം", "ml").getIntentType());
    }

    @Test
    public void testSendMessageVariations() {
        String lang = "en";

        // Required examples for SEND_MESSAGE:
        // "send message", "send a message", "message caregiver", "send message to caregiver"
        String[] variations = {
                "send message",
                "send a message",
                "message caregiver",
                "send message to caregiver",
                "send a message to caregiver"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.SEND_MESSAGE",
                    VoiceIntentType.SEND_MESSAGE, intent.getIntentType());
            assertEquals("Phrase '" + phrase + "' should map to send message intent ID",
                    VoiceCommandConstants.INTENT_SEND_MESSAGE, intent.getIntentName());
        }

        // Generic isolated words alone must NOT trigger SEND_MESSAGE
        String[] genericRejections = {"send", "message", "caregiver"};
        for (String word : genericRejections) {
            VoiceIntent intent = processor.detectIntent(word, lang);
            assertNotEquals("Isolated word '" + word + "' must NOT match SEND_MESSAGE",
                    VoiceIntentType.SEND_MESSAGE, intent.getIntentType());
        }

        // Multilingual variations
        assertEquals(VoiceIntentType.SEND_MESSAGE, processor.detectIntent("ಸಂದೇಶ ಕಳುಹಿಸಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.SEND_MESSAGE, processor.detectIntent("संदेश भेजो", "hi").getIntentType());
        assertEquals(VoiceIntentType.SEND_MESSAGE, processor.detectIntent("സന്ദേശം അയക്കുക", "ml").getIntentType());
    }

    @Test
    public void testReadMessagesVariations() {
        String lang = "en";

        String[] variations = {
                "read messages",
                "read my messages",
                "read caregiver message",
                "read caregiver messages",
                "check messages",
                "check my messages"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.READ_MESSAGES",
                    VoiceIntentType.READ_MESSAGES, intent.getIntentType());
            assertEquals("Phrase '" + phrase + "' should map to read messages intent ID",
                    VoiceCommandConstants.INTENT_READ_MESSAGES, intent.getIntentName());
        }

        // Generic isolated words alone must NOT trigger READ_MESSAGES
        String[] genericRejections = {"read", "messages", "message", "check"};
        for (String word : genericRejections) {
            VoiceIntent intent = processor.detectIntent(word, lang);
            assertNotEquals("Isolated word '" + word + "' must NOT match READ_MESSAGES",
                    VoiceIntentType.READ_MESSAGES, intent.getIntentType());
        }

        // Multilingual variations
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("ಸಂದೇಶಗಳನ್ನು ಓದಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("संदेश पढ़ो", "hi").getIntentType());
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("സന്ദേശങ്ങൾ വായിക്കുക", "ml").getIntentType());
    }

    @Test
    public void testMessageCountVariations() {
        String lang = "en";

        String[] variations = {
                "how many messages",
                "how many messages do I have",
                "count messages",
                "count my messages",
                "how many caregiver messages",
                "how many new messages"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.MESSAGE_COUNT",
                    VoiceIntentType.MESSAGE_COUNT, intent.getIntentType());
            assertEquals("Phrase '" + phrase + "' should map to message count intent ID",
                    VoiceCommandConstants.INTENT_MESSAGE_COUNT, intent.getIntentName());
        }

        // Negative tests: isolated/generic words must strictly remain UNKNOWN
        String[] genericRejections = {"how many", "messages", "message", "count"};
        for (String word : genericRejections) {
            VoiceIntent intent = processor.detectIntent(word, lang);
            assertEquals("Isolated word '" + word + "' must remain UNKNOWN",
                    VoiceIntentType.UNKNOWN, intent.getIntentType());
        }

        // Multilingual variations
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("ಎಷ್ಟು ಸಂದೇಶಗಳು", "kn").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("कितने संदेश हैं", "hi").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("എത്ര സന്ദേശങ്ങൾ", "ml").getIntentType());
    }

    @Test
    public void testRepeatMessageVariations() {
        String lang = "en";

        String[] variations = {
                "repeat message",
                "repeat the message",
                "repeat caregiver message",
                "repeat my message",
                "say that again",
                "repeat that"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.REPEAT_MESSAGE",
                    VoiceIntentType.REPEAT_MESSAGE, intent.getIntentType());
            assertEquals("Phrase '" + phrase + "' should map to repeat message intent ID",
                    VoiceCommandConstants.INTENT_REPEAT_MESSAGE, intent.getIntentName());
        }

        // Negative tests: isolated/generic words must strictly remain UNKNOWN
        String[] genericRejections = {"repeat", "message", "again", "say", "that"};
        for (String word : genericRejections) {
            VoiceIntent intent = processor.detectIntent(word, lang);
            assertEquals("Isolated word '" + word + "' must remain UNKNOWN",
                    VoiceIntentType.UNKNOWN, intent.getIntentType());
        }

        // Multilingual variations
        assertEquals(VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent("ಸಂದೇಶವನ್ನು ಪುನರಾವರ್ತಿಸಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent("संदेश दोहराएं", "hi").getIntentType());
        assertEquals(VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent("സന്ദേശം ആവർത്തിക്കുക", "ml").getIntentType());
    }

    @Test
    public void testGoBackVariations() {
        String lang = "en";

        String[] variations = {
                "go back",
                "go back please",
                "go backward",
                "return back",
                "return",
                "previous page"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.GO_BACK",
                    VoiceIntentType.GO_BACK, intent.getIntentType());
            assertEquals("Phrase '" + phrase + "' should map to go back intent ID",
                    VoiceCommandConstants.INTENT_GO_BACK, intent.getIntentName());
        }

        // Negative tests: isolated/generic words must strictly remain UNKNOWN
        String[] genericRejections = {"back", "go", "previous", "page"};
        for (String word : genericRejections) {
            VoiceIntent intent = processor.detectIntent(word, lang);
            assertEquals("Isolated word '" + word + "' must remain UNKNOWN",
                    VoiceIntentType.UNKNOWN, intent.getIntentType());
        }

        // Multilingual variations
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("ಹಿಂದೆ ಹೋಗಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("वापस जाओ", "hi").getIntentType());
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("തിരികെ പോകുക", "ml").getIntentType());
    }

    @Test
    public void testMultilingualFoundation_LocalesAndTags() {
        // 1. Kannada language configuration
        assertEquals("kn-IN", VoiceLanguageConfig.getSpeechRecognizerLanguageTag("kn"));
        assertEquals(new java.util.Locale("kn", "IN"), VoiceLanguageConfig.getSpeechRecognizerLocale("kn"));
        assertEquals(new java.util.Locale("kn", "IN"), VoiceLanguageConfig.getTtsLocale("kn"));

        // 2. Hindi language configuration
        assertEquals("hi-IN", VoiceLanguageConfig.getSpeechRecognizerLanguageTag("hi"));
        assertEquals(new java.util.Locale("hi", "IN"), VoiceLanguageConfig.getSpeechRecognizerLocale("hi"));
        assertEquals(new java.util.Locale("hi", "IN"), VoiceLanguageConfig.getTtsLocale("hi"));

        // 3. Malayalam language configuration
        assertEquals("ml-IN", VoiceLanguageConfig.getSpeechRecognizerLanguageTag("ml"));
        assertEquals(new java.util.Locale("ml", "IN"), VoiceLanguageConfig.getSpeechRecognizerLocale("ml"));
        assertEquals(new java.util.Locale("ml", "IN"), VoiceLanguageConfig.getTtsLocale("ml"));

        // 4. English language configuration
        assertEquals("en-IN", VoiceLanguageConfig.getSpeechRecognizerLanguageTag("en"));
        assertEquals(new java.util.Locale("en", "IN"), VoiceLanguageConfig.getSpeechRecognizerLocale("en"));
        assertEquals(new java.util.Locale("en", "IN"), VoiceLanguageConfig.getTtsLocale("en"));

        // Fallback locale
        assertEquals(java.util.Locale.US, VoiceLanguageConfig.getSafeTtsFallbackLocale());
    }

    @Test
    public void testMultilingualFoundation_TtsSafetyWithoutCrash() {
        // Safe check: null TTS instance returns safe fallback without crash or exception
        java.util.Locale resolved = VoiceLanguageConfig.resolveAvailableTtsLocale(null, "kn");
        assertNotNull(resolved);
        assertEquals(VoiceLanguageConfig.getSafeTtsFallbackLocale(), resolved);

        java.util.Locale invalidLangResolved = VoiceLanguageConfig.resolveAvailableTtsLocale(null, "unsupported_xyz");
        assertNotNull(invalidLangResolved);
        assertEquals(VoiceLanguageConfig.getSafeTtsFallbackLocale(), invalidLangResolved);
    }

    @Test
    public void testMultilingualFoundation_UnicodeNormalizationPreservesScripts() {
        // Kannada preservation
        String knInput = "Assistant, ಮನೆಗೆ ಹೋಗಿ";
        assertTrue("Kannada text should have wake word", VoiceIntentMatcher.hasWakeWord(knInput));
        String knCommand = VoiceIntentMatcher.extractCommandText(knInput);
        assertEquals("ಮನೆಗೆ ಹೋಗಿ", knCommand);
        assertEquals("ಮನೆಗೆ ಹೋಗಿ", VoiceIntentMatcher.normalizeText(knInput));

        // Hindi / Devanagari preservation with Danda
        String hiInput = "Assistant, घर जाओ।";
        assertTrue("Hindi text should have wake word", VoiceIntentMatcher.hasWakeWord(hiInput));
        String hiCommand = VoiceIntentMatcher.extractCommandText(hiInput);
        assertEquals("घर जाओ", hiCommand);

        // Malayalam preservation
        String mlInput = "Assistant, വീട്ടിലേക്ക് പോകൂ";
        assertTrue("Malayalam text should have wake word", VoiceIntentMatcher.hasWakeWord(mlInput));
        String mlCommand = VoiceIntentMatcher.extractCommandText(mlInput);
        assertEquals("വീട്ടിലേക്ക് പോകൂ", mlCommand);
    }

    @Test
    public void testMultilingualFoundation_OpenHomeFoundationalIntents() {
        // Step 5 requirement: Kannada, Hindi, Malayalam, and English home phrases all resolve to OPEN_HOME
        assertEquals("Kannada 'ಮನೆಗೆ ಹೋಗಿ' must resolve to OPEN_HOME",
                VoiceIntentType.OPEN_HOME, processor.detectIntent("ಮನೆಗೆ ಹೋಗಿ", "kn").getIntentType());
        assertEquals("Hindi 'घर जाओ' must resolve to OPEN_HOME",
                VoiceIntentType.OPEN_HOME, processor.detectIntent("घर जाओ", "hi").getIntentType());
        assertEquals("Malayalam 'വീട്ടിലേക്ക് പോകൂ' must resolve to OPEN_HOME",
                VoiceIntentType.OPEN_HOME, processor.detectIntent("വീട്ടിലേക്ക് പോകൂ", "ml").getIntentType());
        assertEquals("English 'go home' must resolve to OPEN_HOME",
                VoiceIntentType.OPEN_HOME, processor.detectIntent("go home", "en").getIntentType());
    }

    @Test
    public void testMultilingualFoundation_LocalizedResponses() {
        // English
        assertEquals("Home is open.", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_OPEN_HOME, "Home is open.", "en"));
        // Kannada
        assertEquals("ಮನೆ ತೆರೆಯಲಾಗಿದೆ.", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_OPEN_HOME, "Home is open.", "kn"));
        // Hindi
        assertEquals("होम खुल गया है।", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_OPEN_HOME, "Home is open.", "hi"));
        // Malayalam
        assertEquals("ഹോം തുറന്നു.", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_OPEN_HOME, "Home is open.", "ml"));
    }

    // ==========================================
    // TASK 17 MULTILINGUAL NAVIGATION TESTS
    // ==========================================

    @Test
    public void testTask17_OpenHome_MultilingualPhrasesAndRejections() {
        // English
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("open home", "en").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("go home", "en").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("show home", "en").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("home", "en").getIntentType());

        // Kannada
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("ಮನೆ ತೆರೆಯಿರಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("ಮನೆಗೆ ಹೋಗಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("ಹೋಮ್ ತೆರೆಯಿರಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("ಹೋಮ್ಗೆ ಹೋಗಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("ಹೋಮ್ ಗೆ ಹೋಗಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("ಮುಖಪುಟ", "kn").getIntentType());

        // Hindi
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("होम खोलो", "hi").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("घर जाओ", "hi").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("होम पर जाओ", "hi").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("होम दिखाओ", "hi").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("घर चलो", "hi").getIntentType());

        // Malayalam
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("ഹോം തുറക്കൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("വീട്ടിലേക്ക് പോകൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("ഹോം കാണിക്കൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("ഹോമിലേക്ക് പോകൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("ഹോം തുറക്കുക", "ml").getIntentType());

        // Generic False-Positive Rejections: "homework", "home screen issue", "show me", "open", "go"
        assertNotEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("homework", "en").getIntentType());
        assertNotEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("home screen issue", "en").getIntentType());
        assertNotEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("show me", "en").getIntentType());
        assertNotEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("open", "en").getIntentType());
        assertNotEquals(VoiceIntentType.OPEN_HOME, processor.detectIntent("go", "en").getIntentType());
    }

    @Test
    public void testTask17_OpenProfile_MultilingualPhrasesAndRejections() {
        // English
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("open profile", "en").getIntentType());
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("show profile", "en").getIntentType());
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("go to profile", "en").getIntentType());
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("profile", "en").getIntentType());

        // Kannada
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("ಪ್ರೊಫೈಲ್ ತೆರೆಯಿರಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("ಪ್ರೊಫೈಲ್ ತೋರಿಸಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("ಪ್ರೊಫೈಲ್ಗೆ ಹೋಗಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("ಪ್ರೊಫೈಲ್ ಗೆ ಹೋಗಿ", "kn").getIntentType());

        // Hindi
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("प्रोफाइल खोलो", "hi").getIntentType());
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("प्रोफाइल दिखाओ", "hi").getIntentType());
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("प्रोफाइल पर जाओ", "hi").getIntentType());

        // Malayalam
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("പ്രൊഫൈൽ തുറക്കൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("പ്രൊഫൈൽ കാണിക്കൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("പ്രൊഫൈലിലേക്ക് പോകൂ", "ml").getIntentType());

        // Generic False-Positive Rejections
        assertNotEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("open", "en").getIntentType());
        assertNotEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("show", "en").getIntentType());
        assertNotEquals(VoiceIntentType.OPEN_PROFILE, processor.detectIntent("user", "en").getIntentType());
    }

    @Test
    public void testTask17_OpenSettings_MultilingualPhrasesAndRejections() {
        // English
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("open settings", "en").getIntentType());
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("go to settings", "en").getIntentType());
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("show settings", "en").getIntentType());
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("settings", "en").getIntentType());

        // Kannada
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಿರಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("ಸೆಟ್ಟಿಂಗ್ಸ್ಗೆ ಹೋಗಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("ಸೆಟ್ಟಿಂಗ್ಸ್ ಗೆ ಹೋಗಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("ಸೆಟ್ಟಿಂಗ್ಸ್ ತೋರಿಸಿ", "kn").getIntentType());

        // Hindi
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("सेटिंग्स खोलो", "hi").getIntentType());
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("सेटिंग्स पर जाओ", "hi").getIntentType());
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("सेटिंग्स दिखाओ", "hi").getIntentType());

        // Malayalam
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("സെറ്റിംഗ്സ് തുറക്കൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("സെറ്റിംഗ്സിലേക്ക് പോകൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("സെറ്റിംഗ്സ് കാണിക്കൂ", "ml").getIntentType());

        // Generic False-Positive Rejections
        assertNotEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("open", "en").getIntentType());
        assertNotEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("show", "en").getIntentType());
        assertNotEquals(VoiceIntentType.OPEN_SETTINGS, processor.detectIntent("show me", "en").getIntentType());
    }

    @Test
    public void testTask17_GoBack_MultilingualPhrasesAndRejections() {
        // English
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("go back", "en").getIntentType());
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("go back please", "en").getIntentType());
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("go backward", "en").getIntentType());
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("return back", "en").getIntentType());
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("return", "en").getIntentType());
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("previous page", "en").getIntentType());

        // Kannada
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("ಹಿಂದೆ ಹೋಗಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("ಹಿಂದಕ್ಕೆ ಹೋಗಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("ವಾಪಸ್ ಹೋಗಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("ಹಿಂದಕ್ಕೆ ಬನ್ನಿ", "kn").getIntentType());

        // Hindi
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("वापस जाओ", "hi").getIntentType());
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("पीछे जाओ", "hi").getIntentType());
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("वापस जाइए", "hi").getIntentType());
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("पिछले पेज पर जाओ", "hi").getIntentType());

        // Malayalam
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("തിരികെ പോകൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("പിന്നിലേക്ക് പോകൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("വീണ്ടും പിന്നിലേക്ക് പോകൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.GO_BACK, processor.detectIntent("തിരികെ പോകുക", "ml").getIntentType());

        // Negative rejections: isolated words must remain UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("back", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("go", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("previous", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("page", "en").getIntentType());
    }

    @Test
    public void testTask17_NavigationSpokenResponses_AllLanguages() {
        // OPEN_HOME
        assertEquals("Home is open.", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_OPEN_HOME, "Home is open.", "en"));
        assertEquals("ಮನೆ ತೆರೆಯಲಾಗಿದೆ.", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_OPEN_HOME, "Home is open.", "kn"));
        assertEquals("होम खुल गया है।", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_OPEN_HOME, "Home is open.", "hi"));
        assertEquals("ഹോം തുറന്നു.", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_OPEN_HOME, "Home is open.", "ml"));

        // OPEN_PROFILE
        assertEquals("Profile is open.", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_OPEN_PROFILE, "Profile is open.", "en"));
        assertEquals("ಪ್ರೊಫೈಲ್ ತೆರೆಯಲಾಗಿದೆ.", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_OPEN_PROFILE, "Profile is open.", "kn"));
        assertEquals("प्रोफ़ाइल खुल गई है।", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_OPEN_PROFILE, "Profile is open.", "hi"));
        assertEquals("പ്രൊഫൈൽ തുറന്നു.", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_OPEN_PROFILE, "Profile is open.", "ml"));

        // OPEN_SETTINGS
        assertEquals("Settings is open.", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_OPEN_SETTINGS, "Settings is open.", "en"));
        assertEquals("ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಲಾಗಿದೆ.", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_OPEN_SETTINGS, "Settings is open.", "kn"));
        assertEquals("सेटिंग्स खुल गई हैं।", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_OPEN_SETTINGS, "Settings is open.", "hi"));
        assertEquals("സെറ്റിംഗ്സ് തുറന്നു.", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_OPEN_SETTINGS, "Settings is open.", "ml"));

        // GO_BACK
        assertEquals("Going back.", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_GO_BACK, "Going back.", "en"));
        assertEquals("ಹಿಂದೆ ಹೋಗುತ್ತಿದ್ದೇನೆ.", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_GO_BACK, "Going back.", "kn"));
        assertEquals("वापस जा रहा हूँ।", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_GO_BACK, "Going back.", "hi"));
        assertEquals("തിരികെ പോകുന്നു.", VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_GO_BACK, "Going back.", "ml"));
    }

    // ========================================================
    // TASK 18 — MULTILINGUAL SEND_MESSAGE TESTS
    // ========================================================

    @Test
    public void testTask18_EnglishSendMessagePhrasesAndRejections() {
        String[] phrases = {
                "send message",
                "send a message",
                "message caregiver",
                "send message to caregiver"
        };
        for (String p : phrases) {
            assertEquals("English SEND_MESSAGE: " + p, VoiceIntentType.SEND_MESSAGE,
                    processor.detectIntent(p, "en").getIntentType());
        }

        String[] rejections = {"message", "send", "caregiver", "send it"};
        for (String r : rejections) {
            assertEquals("English isolated rejection: " + r, VoiceIntentType.UNKNOWN,
                    processor.detectIntent(r, "en").getIntentType());
        }
    }

    @Test
    public void testTask18_KannadaSendMessagePhrasesAndRejections() {
        String[] phrases = {
                "ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಒಂದು ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಕೇರ್ಗಿವರ್ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಕೇರ್ಗಿವರ್ಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ಮೆಸೇಜ್ ಕಳುಹಿಸಿ"
        };
        for (String p : phrases) {
            assertEquals("Kannada SEND_MESSAGE: " + p, VoiceIntentType.SEND_MESSAGE,
                    processor.detectIntent(p, "kn").getIntentType());
        }

        String[] rejections = {"ಸಂದೇಶ", "ಕಳುಹಿಸಿ", "ಕೇರ್ಗಿವರ್", "ಕೇರ್‌ಗಿವರ್"};
        for (String r : rejections) {
            assertEquals("Kannada isolated rejection: " + r, VoiceIntentType.UNKNOWN,
                    processor.detectIntent(r, "kn").getIntentType());
        }
    }

    @Test
    public void testTask18_HindiSendMessagePhrasesAndRejections() {
        String[] phrases = {
                "संदेश भेजो",
                "एक संदेश भेजो",
                "केयरगिवर को संदेश भेजो",
                "केयरटेकर को संदेश भेजो",
                "मैसेज भेजो"
        };
        for (String p : phrases) {
            assertEquals("Hindi SEND_MESSAGE: " + p, VoiceIntentType.SEND_MESSAGE,
                    processor.detectIntent(p, "hi").getIntentType());
        }

        String[] rejections = {"संदेश", "भेजो", "केयरगिवर"};
        for (String r : rejections) {
            assertEquals("Hindi isolated rejection: " + r, VoiceIntentType.UNKNOWN,
                    processor.detectIntent(r, "hi").getIntentType());
        }
    }

    @Test
    public void testTask18_MalayalamSendMessagePhrasesAndRejections() {
        String[] phrases = {
                "സന്ദേശം അയയ്ക്കൂ",
                "ഒരു സന്ദേശം അയയ്ക്കൂ",
                "കെയർഗിവറിന് സന്ദേശം അയയ്ക്കൂ",
                "കെയർടേക്കറിന് സന്ദേശം അയയ്ക്കൂ",
                "മെസേജ് അയയ്ക്കൂ"
        };
        for (String p : phrases) {
            assertEquals("Malayalam SEND_MESSAGE: " + p, VoiceIntentType.SEND_MESSAGE,
                    processor.detectIntent(p, "ml").getIntentType());
        }

        String[] rejections = {"സന്ദേശം", "അയയ്ക്കൂ", "കെയർഗിവർ"};
        for (String r : rejections) {
            assertEquals("Malayalam isolated rejection: " + r, VoiceIntentType.UNKNOWN,
                    processor.detectIntent(r, "ml").getIntentType());
        }
    }

    @Test
    public void testTask18_WakeWordExtractionIndicScripts() {
        // Single utterance with wake word
        String enUtterance = "Assistant, send message";
        assertTrue(VoiceIntentMatcher.hasWakeWord(enUtterance));
        assertEquals("send message", VoiceIntentMatcher.extractCommandText(enUtterance));
        assertEquals(VoiceIntentType.SEND_MESSAGE, processor.detectIntent("send message", "en").getIntentType());

        String knUtterance = "Assistant, ಸಂದೇಶ ಕಳುಹಿಸಿ";
        assertTrue(VoiceIntentMatcher.hasWakeWord(knUtterance));
        assertEquals("ಸಂದೇಶ ಕಳುಹಿಸಿ", VoiceIntentMatcher.extractCommandText(knUtterance));
        assertEquals(VoiceIntentType.SEND_MESSAGE, processor.detectIntent("ಸಂದೇಶ ಕಳುಹಿಸಿ", "kn").getIntentType());

        String hiUtterance = "Assistant, संदेश भेजो";
        assertTrue(VoiceIntentMatcher.hasWakeWord(hiUtterance));
        assertEquals("संदेश भेजो", VoiceIntentMatcher.extractCommandText(hiUtterance));
        assertEquals(VoiceIntentType.SEND_MESSAGE, processor.detectIntent("संदेश भेजो", "hi").getIntentType());

        String mlUtterance = "Assistant, സന്ദേശം അയയ്ക്കൂ";
        assertTrue(VoiceIntentMatcher.hasWakeWord(mlUtterance));
        assertEquals("സന്ദേശം അയയ്ക്കൂ", VoiceIntentMatcher.extractCommandText(mlUtterance));
        assertEquals(VoiceIntentType.SEND_MESSAGE, processor.detectIntent("സന്ദേശം അയയ്ക്കൂ", "ml").getIntentType());

        // Without wake word: must be ignored by wake-word detector
        assertFalse(VoiceIntentMatcher.hasWakeWord("send message"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("ಸಂದೇಶ ಕಳುಹಿಸಿ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("संदेश भेजो"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("സന്ദേശം അയയ്ക്കൂ"));
    }

    @Test
    public void testTask18_LocalizedPromptsAndFeedbackResponses_AllLanguages() {
        // PROMPT: "Please say your message."
        assertEquals("Please say your message.", VoiceLanguageConfig.getSayMessagePrompt("en"));
        assertEquals("ದಯವಿಟ್ಟು ನಿಮ್ಮ ಸಂದೇಶವನ್ನು ಹೇಳಿ.", VoiceLanguageConfig.getSayMessagePrompt("kn"));
        assertEquals("कृपया अपना संदेश बोलिए।", VoiceLanguageConfig.getSayMessagePrompt("hi"));
        assertEquals("ദയവായി നിങ്ങളുടെ സന്ദേശം പറയൂ.", VoiceLanguageConfig.getSayMessagePrompt("ml"));

        // CONFIRMATION: "Message sent."
        assertEquals("Message sent.", VoiceLanguageConfig.getMessageSentResponse("en"));
        assertEquals("ಸಂದೇಶ ಕಳುಹಿಸಲಾಗಿದೆ.", VoiceLanguageConfig.getMessageSentResponse("kn"));
        assertEquals("संदेश भेज दिया गया है।", VoiceLanguageConfig.getMessageSentResponse("hi"));
        assertEquals("സന്ദേശം അയച്ചു.", VoiceLanguageConfig.getMessageSentResponse("ml"));

        // NO CAREGIVER: "No caregiver is connected."
        assertEquals("No caregiver is connected.", VoiceLanguageConfig.getNoCaregiverResponse("en"));
        assertEquals("ಯಾವುದೇ ಕೇರ್ಗಿವರ್ ಸಂಪರ್ಕಗೊಂಡಿಲ್ಲ.", VoiceLanguageConfig.getNoCaregiverResponse("kn"));
        assertEquals("कोई केयरगिवर जुड़ा नहीं है।", VoiceLanguageConfig.getNoCaregiverResponse("hi"));
        assertEquals("ഒരു കെയർഗിവറും ബന്ധിപ്പിച്ചിട്ടില്ല.", VoiceLanguageConfig.getNoCaregiverResponse("ml"));

        // CALL ACTIVE: "A call is active."
        assertEquals("A call is active.", VoiceLanguageConfig.getCallActiveResponse("en"));
        assertEquals("ಕರೆ ಸಕ್ರಿಯವಾಗಿದೆ.", VoiceLanguageConfig.getCallActiveResponse("kn"));
        assertEquals("एक कॉल सक्रिय है।", VoiceLanguageConfig.getCallActiveResponse("hi"));
        assertEquals("ഒരു കോൾ സജീവമാണ്.", VoiceLanguageConfig.getCallActiveResponse("ml"));

        // NO MESSAGE HEARD: "I didn't hear a message."
        assertEquals("I didn't hear a message.", VoiceLanguageConfig.getNoMessageHeardResponse("en"));
        assertEquals("ನನಗೆ ಯಾವುದೇ ಸಂದೇಶ ಕೇಳಿಸಲಿಲ್ಲ.", VoiceLanguageConfig.getNoMessageHeardResponse("kn"));
        assertEquals("मुझे कोई संदेश सुनाई नहीं दिया।", VoiceLanguageConfig.getNoMessageHeardResponse("hi"));
        assertEquals("എനിക്ക് ഒരു സന്ദേശവും കേൾക്കാനായില്ല.", VoiceLanguageConfig.getNoMessageHeardResponse("ml"));
    }

    @Test
    public void testTask19_EnglishReadMessagesVariations() {
        // 1. "read messages" -> READ_MESSAGES
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("read messages", "en").getIntentType());
        // 2. "read my messages" -> READ_MESSAGES
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("read my messages", "en").getIntentType());
        // 3. "read caregiver message" -> READ_MESSAGES
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("read caregiver message", "en").getIntentType());
        // 4. "read caregiver messages" -> READ_MESSAGES
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("read caregiver messages", "en").getIntentType());
        // 5. "check messages" -> READ_MESSAGES
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("check messages", "en").getIntentType());
        // 6. "check my messages" -> READ_MESSAGES
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("check my messages", "en").getIntentType());
    }

    @Test
    public void testTask19_KannadaReadMessagesVariations() {
        // 7. "ಸಂದೇಶಗಳನ್ನು ಓದಿ" -> READ_MESSAGES
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("ಸಂದೇಶಗಳನ್ನು ಓದಿ", "kn").getIntentType());
        // 8. "ನನ್ನ ಸಂದೇಶಗಳನ್ನು ಓದಿ" -> READ_MESSAGES
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("ನನ್ನ ಸಂದೇಶಗಳನ್ನು ಓದಿ", "kn").getIntentType());
        // 9. "ಕೇರ್ಗಿವರ್ ಸಂದೇಶಗಳನ್ನು ಓದಿ" -> READ_MESSAGES
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("ಕೇರ್ಗಿವರ್ ಸಂದೇಶಗಳನ್ನು ಓದಿ", "kn").getIntentType());
        // 10. "ಸಂದೇಶಗಳನ್ನು ಪರಿಶೀಲಿಸಿ" -> READ_MESSAGES
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("ಸಂದೇಶಗಳನ್ನು ಪರಿಶೀಲಿಸಿ", "kn").getIntentType());
        // Additional natural variants
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("ಮೆಸೇಜ್ ಓದಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("ಮೆಸೇಜ್ಗಳನ್ನು ಓದಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("ನನ್ನ ಮೆಸೇಜ್ಗಳನ್ನು ಓದಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("ಕೇರ್ಗಿವರ್ ಮೆಸೇಜ್ ಓದಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("ಕೇರ್‌ಗಿವರ್ ಸಂದೇಶ ಓದಿ", "kn").getIntentType());
    }

    @Test
    public void testTask19_HindiReadMessagesVariations() {
        // 11. "संदेश पढ़ो" -> READ_MESSAGES
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("संदेश पढ़ो", "hi").getIntentType());
        // 12. "मेरे संदेश पढ़ो" -> READ_MESSAGES
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("मेरे संदेश पढ़ो", "hi").getIntentType());
        // 13. "केयरगिवर के संदेश पढ़ो" -> READ_MESSAGES
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("केयरगिवर के संदेश पढ़ो", "hi").getIntentType());
        // 14. "संदेश चेक करो" -> READ_MESSAGES
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("संदेश चेक करो", "hi").getIntentType());
        // Additional natural variants
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("मैसेज पढ़ो", "hi").getIntentType());
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("मैसेज चेक करो", "hi").getIntentType());
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("मेरे मैसेज पढ़ो", "hi").getIntentType());
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("केयरगिवर का संदेश पढ़ो", "hi").getIntentType());
    }

    @Test
    public void testTask19_MalayalamReadMessagesVariations() {
        // 15. "സന്ദേശങ്ങൾ വായിക്കൂ" -> READ_MESSAGES
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("സന്ദേശങ്ങൾ വായിക്കൂ", "ml").getIntentType());
        // 16. "എന്റെ സന്ദേശങ്ങൾ വായിക്കൂ" -> READ_MESSAGES
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("എന്റെ സന്ദേശങ്ങൾ വായിക്കൂ", "ml").getIntentType());
        // 17. "കെയർഗിവറിന്റെ സന്ദേശങ്ങൾ വായിക്കൂ" -> READ_MESSAGES
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("കെയർഗിവറിന്റെ സന്ദേശങ്ങൾ വായിക്കൂ", "ml").getIntentType());
        // 18. "സന്ദേശങ്ങൾ പരിശോധിക്കൂ" -> READ_MESSAGES
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("സന്ദേശങ്ങൾ പരിശോധിക്കൂ", "ml").getIntentType());
        // Additional natural variants
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("മെസേജുകൾ വായിക്കൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("മെസേജ് വായിക്കൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("എന്റെ മെസേജുകൾ വായിക്കൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("മെസേജുകൾ പരിശോധിക്കൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("കെയർഗിവറുടെ സന്ദേശം വായിക്കുക", "ml").getIntentType());
    }

    @Test
    public void testTask19_NegativeIsolatedTokens_MustRemainUnknown() {
        // 19. generic "read" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("read", "en").getIntentType());
        // 20. generic "message" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("message", "en").getIntentType());
        // 21. generic "messages" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("messages", "en").getIntentType());
        // generic "check" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("check", "en").getIntentType());
        // generic "caregiver" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("caregiver", "en").getIntentType());

        // 22. generic "ಓದಿ" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಓದಿ", "kn").getIntentType());
        // 23. generic "ಸಂದೇಶ" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಸಂದೇಶ", "kn").getIntentType());
        // generic "ಸಂದೇಶಗಳು" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಸಂದೇಶಗಳು", "kn").getIntentType());
        // generic "ಮೆಸೇಜ್" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಮೆಸೇಜ್", "kn").getIntentType());
        // generic "ಪರಿಶೀಲಿಸಿ" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಪರಿಶೀಲಿಸಿ", "kn").getIntentType());

        // 24. generic "पढ़ो" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("पढ़ो", "hi").getIntentType());
        // 25. generic "संदेश" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("संदेश", "hi").getIntentType());
        // generic "मैसेज" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("मैसेज", "hi").getIntentType());
        // generic "चेक" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("चेक", "hi").getIntentType());
        // generic "केयरगिवर" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("केयरगिवर", "hi").getIntentType());

        // 26. generic "വായിക്കൂ" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("വായിക്കൂ", "ml").getIntentType());
        // 27. generic "സന്ദേശം" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("സന്ദേശം", "ml").getIntentType());
        // generic "സന്ദേശങ്ങൾ" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("സന്ദേശങ്ങൾ", "ml").getIntentType());
        // generic "മെസേജ്" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("മെസേജ്", "ml").getIntentType());
        // generic "പരിശോധിക്കൂ" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("പരിശോധിക്കൂ", "ml").getIntentType());
        // generic "കെയർഗിവർ" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("കെയർഗിവർ", "ml").getIntentType());
    }

    @Test
    public void testTask19_WakeWordGating_SingleUtteranceAndRejection() {
        // 28. "Assistant, read messages" -> READ_MESSAGES
        String enUtterance = "Assistant, read messages";
        assertTrue(VoiceIntentMatcher.hasWakeWord(enUtterance));
        assertEquals("read messages", VoiceIntentMatcher.extractCommandText(enUtterance));
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("read messages", "en").getIntentType());

        // 29. "Assistant, ಸಂದೇಶಗಳನ್ನು ಓದಿ" -> READ_MESSAGES
        String knUtterance = "Assistant, ಸಂದೇಶಗಳನ್ನು ಓದಿ";
        assertTrue(VoiceIntentMatcher.hasWakeWord(knUtterance));
        assertEquals("ಸಂದೇಶಗಳನ್ನು ಓದಿ", VoiceIntentMatcher.extractCommandText(knUtterance));
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("ಸಂದೇಶಗಳನ್ನು ಓದಿ", "kn").getIntentType());

        // 30. "Assistant, संदेश पढ़ो" -> READ_MESSAGES
        String hiUtterance = "Assistant, संदेश पढ़ो";
        assertTrue(VoiceIntentMatcher.hasWakeWord(hiUtterance));
        assertEquals("संदेश पढ़ो", VoiceIntentMatcher.extractCommandText(hiUtterance));
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("संदेश पढ़ो", "hi").getIntentType());

        // 31. "Assistant, സന്ദേശങ്ങൾ വായിക്കൂ" -> READ_MESSAGES
        String mlUtterance = "Assistant, സന്ദേശങ്ങൾ വായിക്കൂ";
        assertTrue(VoiceIntentMatcher.hasWakeWord(mlUtterance));
        assertEquals("സന്ദേശങ്ങൾ വായിക്കൂ", VoiceIntentMatcher.extractCommandText(mlUtterance));
        assertEquals(VoiceIntentType.READ_MESSAGES, processor.detectIntent("സന്ദേശങ്ങൾ വായിക്കൂ", "ml").getIntentType());

        // 32. Without wake word: must be ignored by wake-word detector
        assertFalse(VoiceIntentMatcher.hasWakeWord("read messages"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("ಸಂದೇಶಗಳನ್ನು ಓದಿ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("संदेश पढ़ो"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("സന്ദേശങ്ങൾ വായിക്കൂ"));
    }

    @Test
    public void testTask19_DataOnlyCaregiverMessageContent() {
        // 33. Caregiver message containing "Assistant, go home" remains data
        String cmdMsg1 = "Assistant, go home";
        String prefixEn = VoiceLanguageConfig.getCaregiverSaysPrefix("en");
        String finalSpoken1 = prefixEn + cmdMsg1.trim();
        assertEquals("Your caregiver says: Assistant, go home", finalSpoken1);

        // 34. Caregiver message containing "Assistant, call caregiver" remains data
        String cmdMsg2 = "Assistant, call caregiver";
        String prefixKn = VoiceLanguageConfig.getCaregiverSaysPrefix("kn");
        String finalSpoken2 = prefixKn + cmdMsg2.trim();
        assertEquals("ನಿಮ್ಮ ಕೇರ್ಗಿವರ್ ಹೇಳುತ್ತಾರೆ: Assistant, call caregiver", finalSpoken2);

        // 35. Caregiver message containing "Assistant, send emergency" remains data
        String cmdMsg3 = "Assistant, send emergency";
        String prefixHi = VoiceLanguageConfig.getCaregiverSaysPrefix("hi");
        String finalSpoken3 = prefixHi + cmdMsg3.trim();
        assertEquals("आपके केयरगिवर कहते हैं: Assistant, send emergency", finalSpoken3);

        String cmdMsg4 = "Assistant, send emergency";
        String prefixMl = VoiceLanguageConfig.getCaregiverSaysPrefix("ml");
        String finalSpoken4 = prefixMl + cmdMsg4.trim();
        assertEquals("നിങ്ങളുടെ കെയർഗിവർ പറയുന്നു: Assistant, send emergency", finalSpoken4);
    }

    @Test
    public void testTask19_LocalizedIntroductionsAndResponses_AllLanguages() {
        // Localized introductions:
        // English: "Your caregiver says: <message>"
        assertEquals("Your caregiver says: ", VoiceLanguageConfig.getCaregiverSaysPrefix("en"));
        // Kannada: "ನಿಮ್ಮ ಕೇರ್ಗಿವರ್ ಹೇಳುತ್ತಾರೆ: <message>"
        assertEquals("ನಿಮ್ಮ ಕೇರ್ಗಿವರ್ ಹೇಳುತ್ತಾರೆ: ", VoiceLanguageConfig.getCaregiverSaysPrefix("kn"));
        // Hindi: "आपके केयरगिवर कहते हैं: <message>"
        assertEquals("आपके केयरगिवर कहते हैं: ", VoiceLanguageConfig.getCaregiverSaysPrefix("hi"));
        // Malayalam: "നിങ്ങളുടെ കെയർഗിവർ പറയുന്നു: <message>"
        assertEquals("നിങ്ങളുടെ കെയർഗിവർ പറയുന്നു: ", VoiceLanguageConfig.getCaregiverSaysPrefix("ml"));

        // Localized "No new messages." response:
        assertEquals("No new messages.", VoiceLanguageConfig.getNoNewMessagesResponse("en"));
        assertEquals("ಹೊಸ ಸಂದೇಶಗಳಿಲ್ಲ.", VoiceLanguageConfig.getNoNewMessagesResponse("kn"));
        assertEquals("कोई नया संदेश नहीं है।", VoiceLanguageConfig.getNoNewMessagesResponse("hi"));
        assertEquals("പുതിയ സന്ദേശങ്ങളൊന്നുമില്ല.", VoiceLanguageConfig.getNoNewMessagesResponse("ml"));

        // Localized "A call is active." response:
        assertEquals("A call is active.", VoiceLanguageConfig.getCallActiveResponse("en"));
        assertEquals("ಕರೆ ಸಕ್ರಿಯವಾಗಿದೆ.", VoiceLanguageConfig.getCallActiveResponse("kn"));
        assertEquals("एक कॉल सक्रिय है।", VoiceLanguageConfig.getCallActiveResponse("hi"));
        assertEquals("ഒരു കോൾ സജീവമാണ്.", VoiceLanguageConfig.getCallActiveResponse("ml"));

        // getLocalizedResponse routing
        assertEquals("No new messages.", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_READ_MESSAGES, VoiceCommandConstants.RESPONSE_NO_NEW_MESSAGES, "en"));
        assertEquals("ಹೊಸ ಸಂದೇಶಗಳಿಲ್ಲ.", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_READ_MESSAGES, VoiceCommandConstants.RESPONSE_NO_NEW_MESSAGES, "kn"));
        assertEquals("कोई नया संदेश नहीं है।", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_READ_MESSAGES, VoiceCommandConstants.RESPONSE_NO_NEW_MESSAGES, "hi"));
        assertEquals("പുതിയ സന്ദേശങ്ങളൊന്നുമില്ല.", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_READ_MESSAGES, VoiceCommandConstants.RESPONSE_NO_NEW_MESSAGES, "ml"));
    }

    @Test
    public void testTask20_EnglishMessageCountVariations() {
        // 1. "how many messages" -> MESSAGE_COUNT
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("how many messages", "en").getIntentType());
        // 2. "how many messages do I have" -> MESSAGE_COUNT
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("how many messages do I have", "en").getIntentType());
        // 3. "count messages" -> MESSAGE_COUNT
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("count messages", "en").getIntentType());
        // 4. "count my messages" -> MESSAGE_COUNT
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("count my messages", "en").getIntentType());
        // 5. "how many caregiver messages" -> MESSAGE_COUNT
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("how many caregiver messages", "en").getIntentType());
        // 6. "how many new messages" -> MESSAGE_COUNT
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("how many new messages", "en").getIntentType());
        // Additional variants
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("how many caregiver messages do I have", "en").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("tell me how many messages I have", "en").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("what is the message count", "en").getIntentType());
    }

    @Test
    public void testTask20_KannadaMessageCountVariations() {
        // 7. "ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ" -> MESSAGE_COUNT
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ", "kn").getIntentType());
        // 8. "ನನ್ನ ಬಳಿ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ" -> MESSAGE_COUNT
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("ನನ್ನ ಬಳಿ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ", "kn").getIntentType());
        // 9. "ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ" -> MESSAGE_COUNT
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ", "kn").getIntentType());
        // 10. "ಎಷ್ಟು ಹೊಸ ಸಂದೇಶಗಳಿವೆ" -> MESSAGE_COUNT
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("ಎಷ್ಟು ಹೊಸ ಸಂದೇಶಗಳಿವೆ", "kn").getIntentType());
        // Additional variants
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ ನನ್ನ ಬಳಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("ನನ್ನ ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("ಎಷ್ಟು ಕೇರ್ಗಿವರ್ ಸಂದೇಶಗಳಿವೆ", "kn").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("ನನ್ನ ಬಳಿ ಎಷ್ಟು ಹೊಸ ಸಂದೇಶಗಳಿವೆ", "kn").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("ಎಷ್ಟು ಮೆಸೇಜ್ಗಳಿವೆ", "kn").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("ಎಷ್ಟು ಮೆಸೇಜುಗಳಿವೆ", "kn").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("ನನ್ನ ಮೆಸೇಜ್ಗಳನ್ನು ಎಣಿಸಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("ಎಷ್ಟು ಕೇರ್ಗಿವರ್ ಮೆಸೇಜ್ಗಳಿವೆ", "kn").getIntentType());
    }

    @Test
    public void testTask20_HindiMessageCountVariations() {
        // 11. "कितने संदेश हैं" -> MESSAGE_COUNT
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("कितने संदेश हैं", "hi").getIntentType());
        // 12. "मेरे पास कितने संदेश हैं" -> MESSAGE_COUNT
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("मेरे पास कितने संदेश हैं", "hi").getIntentType());
        // 13. "संदेश गिनो" -> MESSAGE_COUNT
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("संदेश गिनो", "hi").getIntentType());
        // 14. "कितने नए संदेश हैं" -> MESSAGE_COUNT
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("कितने नए संदेश हैं", "hi").getIntentType());
        // Additional variants
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("मेरे संदेश गिनो", "hi").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("कितने केयरगिवर संदेश हैं", "hi").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("मेरे पास कितने नए संदेश हैं", "hi").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("मुझे बताओ कितने संदेश हैं", "hi").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("कितने मैसेज हैं", "hi").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("मेरे पास कितने मैसेज हैं", "hi").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("मैसेज गिनो", "hi").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("मेरे मैसेज गिनो", "hi").getIntentType());
    }

    @Test
    public void testTask20_MalayalamMessageCountVariations() {
        // 15. "എത്ര സന്ദേശങ്ങളുണ്ട്" -> MESSAGE_COUNT
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("എത്ര സന്ദേശങ്ങളുണ്ട്", "ml").getIntentType());
        // 16. "എന്റെ പക്കൽ എത്ര സന്ദേശങ്ങളുണ്ട്" -> MESSAGE_COUNT
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("എന്റെ പക്കൽ എത്ര സന്ദേശങ്ങളുണ്ട്", "ml").getIntentType());
        // 17. "സന്ദേശങ്ങൾ എണ്ണൂ" -> MESSAGE_COUNT
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("സന്ദേശങ്ങൾ എണ്ണൂ", "ml").getIntentType());
        // 18. "എത്ര പുതിയ സന്ദേശങ്ങളുണ്ട്" -> MESSAGE_COUNT
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("എത്ര പുതിയ സന്ദേശങ്ങളുണ്ട്", "ml").getIntentType());
        // Additional variants
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("എന്റെ സന്ദേശങ്ങൾ എണ്ണൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("എത്ര കെയർഗിവർ സന്ദേശങ്ങളുണ്ട്", "ml").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("എന്റെ പക്കൽ എത്ര പുതിയ സന്ദേശങ്ങളുണ്ട്", "ml").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("എത്ര സന്ദേശങ്ങൾ ഉണ്ടെന്ന് പറയൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("എത്ര മെസേജുകളുണ്ട്", "ml").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("എന്റെ പക്കൽ എത്ര മെസേജുകളുണ്ട്", "ml").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("മെസേജുകൾ എണ്ണൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("എന്റെ മെസേജുകൾ എണ്ണൂ", "ml").getIntentType());
    }

    @Test
    public void testTask20_NegativeIsolatedTokens_MustRemainUnknown() {
        // 19. "how many" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("how many", "en").getIntentType());
        // 20. "messages" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("messages", "en").getIntentType());
        // 21. "count" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("count", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("message", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("new", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("caregiver", "en").getIntentType());

        // 22. "ಎಷ್ಟು" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಎಷ್ಟು", "kn").getIntentType());
        // 23. "ಸಂದೇಶಗಳು" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಸಂದೇಶಗಳು", "kn").getIntentType());
        // 24. "ಎಣಿಸಿ" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಎಣಿಸಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಸಂದೇಶ", "kn").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಮೆಸೇಜ್", "kn").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಕೇರ್ಗಿವರ್", "kn").getIntentType());

        // 25. "कितने" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("कितने", "hi").getIntentType());
        // 26. "संदेश" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("संदेश", "hi").getIntentType());
        // 27. "गिनो" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("गिनो", "hi").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("मैसेज", "hi").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("नए", "hi").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("केयरगिवर", "hi").getIntentType());

        // 28. "എത്ര" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("എത്ര", "ml").getIntentType());
        // 29. "സന്ദേശങ്ങൾ" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("സന്ദേശങ്ങൾ", "ml").getIntentType());
        // 30. "എണ്ണൂ" -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("എണ്ണൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("സന്ദേശം", "ml").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("മെസേജ്", "ml").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("പുതിയ", "ml").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("കെയർഗിവർ", "ml").getIntentType());
    }

    @Test
    public void testTask20_WakeWordGating_SingleUtteranceAndRejection() {
        // 31. "Assistant, how many messages" -> MESSAGE_COUNT
        String enUtterance = "Assistant, how many messages";
        assertTrue(VoiceIntentMatcher.hasWakeWord(enUtterance));
        assertEquals("how many messages", VoiceIntentMatcher.extractCommandText(enUtterance));
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("how many messages", "en").getIntentType());

        // 32. "Assistant, ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ" -> MESSAGE_COUNT
        String knUtterance = "Assistant, ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ";
        assertTrue(VoiceIntentMatcher.hasWakeWord(knUtterance));
        assertEquals("ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ", VoiceIntentMatcher.extractCommandText(knUtterance));
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ", "kn").getIntentType());

        // 33. "Assistant, कितने संदेश हैं" -> MESSAGE_COUNT
        String hiUtterance = "Assistant, कितने संदेश हैं";
        assertTrue(VoiceIntentMatcher.hasWakeWord(hiUtterance));
        assertEquals("कितने संदेश हैं", VoiceIntentMatcher.extractCommandText(hiUtterance));
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("कितने संदेश हैं", "hi").getIntentType());

        // 34. "Assistant, എത്ര സന്ദേശങ്ങളുണ്ട്" -> MESSAGE_COUNT
        String mlUtterance = "Assistant, എത്ര സന്ദേശങ്ങളുണ്ട്";
        assertTrue(VoiceIntentMatcher.hasWakeWord(mlUtterance));
        assertEquals("എത്ര സന്ദേശങ്ങളുണ്ട്", VoiceIntentMatcher.extractCommandText(mlUtterance));
        assertEquals(VoiceIntentType.MESSAGE_COUNT, processor.detectIntent("എത്ര സന്ദേശങ്ങളുണ്ട്", "ml").getIntentType());

        // 35. Without wake word: must be ignored by wake-word detector
        assertFalse(VoiceIntentMatcher.hasWakeWord("how many messages"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("कितने संदेश हैं"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("എത്ര സന്ദേശങ്ങളുണ്ട്"));
    }

    @Test
    public void testTask20_LocalizedCountResponses_AllLanguages() {
        // 36. count = 0 -> localized no-message response
        assertEquals("No new messages.", VoiceLanguageConfig.getMessageCountResponse(0, "en"));
        assertEquals("ಹೊಸ ಸಂದೇಶಗಳಿಲ್ಲ.", VoiceLanguageConfig.getMessageCountResponse(0, "kn"));
        assertEquals("कोई नया संदेश नहीं है।", VoiceLanguageConfig.getMessageCountResponse(0, "hi"));
        assertEquals("പുതിയ സന്ദേശങ്ങളൊന്നുമില്ല.", VoiceLanguageConfig.getMessageCountResponse(0, "ml"));

        // 37. count = 1 -> localized singular response
        assertEquals("You have 1 message.", VoiceLanguageConfig.getMessageCountResponse(1, "en"));
        assertEquals("ನಿಮ್ಮ ಬಳಿ 1 ಸಂದೇಶವಿದೆ.", VoiceLanguageConfig.getMessageCountResponse(1, "kn"));
        assertEquals("आपके पास 1 संदेश है।", VoiceLanguageConfig.getMessageCountResponse(1, "hi"));
        assertEquals("നിങ്ങളുടെ പക്കൽ 1 സന്ദേശമുണ്ട്.", VoiceLanguageConfig.getMessageCountResponse(1, "ml"));

        // 38. count = 2 -> localized plural response
        assertEquals("You have 2 messages.", VoiceLanguageConfig.getMessageCountResponse(2, "en"));
        assertEquals("ನಿಮ್ಮ ಬಳಿ 2 ಸಂದೇಶಗಳಿವೆ.", VoiceLanguageConfig.getMessageCountResponse(2, "kn"));
        assertEquals("आपके पास 2 संदेश हैं।", VoiceLanguageConfig.getMessageCountResponse(2, "hi"));
        assertEquals("നിങ്ങളുടെ പക്കൽ 2 സന്ദേശങ്ങളുണ്ട്.", VoiceLanguageConfig.getMessageCountResponse(2, "ml"));

        // 39. count = 10 -> localized plural response
        assertEquals("You have 10 messages.", VoiceLanguageConfig.getMessageCountResponse(10, "en"));
        assertEquals("ನಿಮ್ಮ ಬಳಿ 10 ಸಂದೇಶಗಳಿವೆ.", VoiceLanguageConfig.getMessageCountResponse(10, "kn"));
        assertEquals("आपके पास 10 संदेश हैं।", VoiceLanguageConfig.getMessageCountResponse(10, "hi"));
        assertEquals("നിങ്ങളുടെ പക്കൽ 10 സന്ദേശങ്ങളുണ്ട്.", VoiceLanguageConfig.getMessageCountResponse(10, "ml"));

        // 40. Active call -> localized active-call response
        assertEquals("A call is active.", VoiceLanguageConfig.getCallActiveResponse("en"));
        assertEquals("ಕರೆ ಸಕ್ರಿಯವಾಗಿದೆ.", VoiceLanguageConfig.getCallActiveResponse("kn"));
        assertEquals("एक कॉल सक्रिय है।", VoiceLanguageConfig.getCallActiveResponse("hi"));
        assertEquals("ഒരു കോൾ സജീവമാണ്.", VoiceLanguageConfig.getCallActiveResponse("ml"));

        // getLocalizedResponse routing for CMD_MESSAGE_COUNT
        assertEquals("No new messages.", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_MESSAGE_COUNT, VoiceCommandConstants.RESPONSE_NO_NEW_MESSAGES, "en"));
        assertEquals("ಹೊಸ ಸಂದೇಶಗಳಿಲ್ಲ.", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_MESSAGE_COUNT, VoiceCommandConstants.RESPONSE_NO_NEW_MESSAGES, "kn"));
        assertEquals("ನಿಮ್ಮ ಬಳಿ 3 ಸಂದೇಶಗಳಿವೆ.", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_MESSAGE_COUNT, "You have 3 messages.", "kn"));
        assertEquals("आपके पास 3 संदेश हैं।", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_MESSAGE_COUNT, "You have 3 messages.", "hi"));
        assertEquals("നിങ്ങളുടെ പക്കൽ 3 സന്ദേശങ്ങളുണ്ട്.", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_MESSAGE_COUNT, "You have 3 messages.", "ml"));
    }

    @Test
    public void testTask20_DataSafety_CommandLikeCaregiverMessageContent() {
        // 41. caregiver message containing "Assistant, how many messages?" remains data
        String cmdMsg1 = "Assistant, how many messages?";
        String spoken1 = VoiceLanguageConfig.getCaregiverSaysPrefix("en") + cmdMsg1;
        assertEquals("Your caregiver says: Assistant, how many messages?", spoken1);

        // 42. caregiver message containing "Assistant, count messages" remains data
        String cmdMsg2 = "Assistant, count messages";
        String spoken2 = VoiceLanguageConfig.getCaregiverSaysPrefix("kn") + cmdMsg2;
        assertEquals("ನಿಮ್ಮ ಕೇರ್ಗಿವರ್ ಹೇಳುತ್ತಾರೆ: Assistant, count messages", spoken2);
    }

    // =========================================================================
    // TASK 21: MULTILINGUAL REPEAT_MESSAGE TESTS (EN, KN, HI, ML)
    // =========================================================================

    @Test
    public void testTask21_MultilingualRepeatMessage_EnglishPhrases() {
        String[] phrases = {
                "repeat message",
                "repeat the message",
                "repeat caregiver message",
                "repeat caregiver messages",
                "repeat my message",
                "say that again",
                "repeat that",
                "repeat the last message",
                "repeat last message",
                "repeat the caregiver message",
                "repeat what you said",
                "say the message again",
                "say that message again",
                "tell me that again"
        };
        for (String phrase : phrases) {
            assertEquals("Failed for English phrase: " + phrase,
                    VoiceIntentType.REPEAT_MESSAGE,
                    processor.detectIntent(phrase, "en").getIntentType());
        }
    }

    @Test
    public void testTask21_MultilingualRepeatMessage_KannadaPhrases() {
        String[] phrases = {
                "ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಮತ್ತೆ ಸಂದೇಶ ಹೇಳಿ",
                "ಆ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಆ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಕೇರ್ಗಿವರ್ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಕೇರ್ಗಿವರ್ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಕೇರ್‌ಗಿವರ್ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಕೇರ್‌ಗಿವರ್ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಮತ್ತೆ ಹೇಳಿ",
                "ಅದನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಆ ಮೆಸೇಜ್ ಮತ್ತೆ ಹೇಳಿ",
                "ಮೆಸೇಜ್ ಮತ್ತೆ ಹೇಳಿ",
                "ಕೊನೆಯ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಕೊನೆಯ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಮತ್ತೊಮ್ಮೆ ಹೇಳಿ"
        };
        for (String phrase : phrases) {
            assertEquals("Failed for Kannada phrase: " + phrase,
                    VoiceIntentType.REPEAT_MESSAGE,
                    processor.detectIntent(phrase, "kn").getIntentType());
        }
    }

    @Test
    public void testTask21_MultilingualRepeatMessage_HindiPhrases() {
        String[] phrases = {
                "संदेश फिर से पढ़ो",
                "संदेश फिर से बताओ",
                "संदेश दोबारा पढ़ो",
                "संदेश दोबारा बताओ",
                "उस संदेश को फिर से पढ़ो",
                "उस संदेश को दोबारा बताओ",
                "केयरगिवर का संदेश फिर से पढ़ो",
                "केयरगिवर का संदेश दोबारा बताओ",
                "मैसेज फिर से पढ़ो",
                "मैसेज दोबारा पढ़ो",
                "फिर से बताओ",
                "वह फिर से बताओ",
                "आखिरी संदेश फिर से पढ़ो",
                "आखिरी संदेश दोबारा बताओ",
                "वही संदेश फिर से बताओ"
        };
        for (String phrase : phrases) {
            assertEquals("Failed for Hindi phrase: " + phrase,
                    VoiceIntentType.REPEAT_MESSAGE,
                    processor.detectIntent(phrase, "hi").getIntentType());
        }
    }

    @Test
    public void testTask21_MultilingualRepeatMessage_MalayalamPhrases() {
        String[] phrases = {
                "സന്ദേശം വീണ്ടും പറയൂ",
                "സന്ദേശം വീണ്ടും വായിക്കൂ",
                "സന്ദേശം വീണ്ടും വായിക്കുക",
                "സന്ദേശം വീണ്ടും പറയുക",
                "ആ സന്ദേശം വീണ്ടും പറയൂ",
                "ആ സന്ദേശം വീണ്ടും വായിക്കൂ",
                "കെയർഗിവറിന്റെ സന്ദേശം വീണ്ടും പറയൂ",
                "കെയർഗിവറുടെ സന്ദേശം വീണ്ടും പറയൂ",
                "കെയർഗിവർ സന്ദേശം വീണ്ടും വായിക്കൂ",
                "മെസേജ് വീണ്ടും പറയൂ",
                "മെസേജ് വീണ്ടും വായിക്കൂ",
                "വീണ്ടും പറയൂ",
                "അത് വീണ്ടും പറയൂ",
                "അവസാന സന്ദേശം വീണ്ടും പറയൂ",
                "അവസാന സന്ദേശം വീണ്ടും വായിക്കൂ"
        };
        for (String phrase : phrases) {
            assertEquals("Failed for Malayalam phrase: " + phrase,
                    VoiceIntentType.REPEAT_MESSAGE,
                    processor.detectIntent(phrase, "ml").getIntentType());
        }
    }

    @Test
    public void testTask21_MultilingualRepeatMessage_NegativeRejections() {
        // English single word rejections -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("repeat", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("message", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("again", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("say", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("that", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("messages", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("caregiver", "en").getIntentType());

        // Kannada single word rejections -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಮತ್ತೆ", "kn").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಸಂದೇಶ", "kn").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಹೇಳಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಮೆಸೇಜ್", "kn").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಕೇರ್ಗಿವರ್", "kn").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಕೇರ್‌ಗಿವರ್", "kn").getIntentType());

        // Hindi single word rejections -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("फिर", "hi").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("संदेश", "hi").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("बताओ", "hi").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("पढ़ो", "hi").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("दोबारा", "hi").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("मैसेज", "hi").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("केयरगिवर", "hi").getIntentType());

        // Malayalam single word rejections -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("വീണ്ടും", "ml").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("സന്ദേശം", "ml").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("പറയൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("വായിക്കൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("മെസേജ്", "ml").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("കെയർഗിവർ", "ml").getIntentType());
    }

    @Test
    public void testTask21_MultilingualRepeatMessage_WakeWordGating() {
        // English wake word + command
        String enUtterance = "Assistant, repeat message";
        assertTrue(VoiceIntentMatcher.hasWakeWord(enUtterance));
        assertEquals("repeat message", VoiceIntentMatcher.extractCommandText(enUtterance));
        assertEquals(VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent("repeat message", "en").getIntentType());

        // Kannada wake word + command
        String knUtterance = "Assistant, ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ";
        assertTrue(VoiceIntentMatcher.hasWakeWord(knUtterance));
        assertEquals("ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ", VoiceIntentMatcher.extractCommandText(knUtterance));
        assertEquals(VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent("ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ", "kn").getIntentType());

        // Hindi wake word + command
        String hiUtterance = "Assistant, संदेश फिर से पढ़ो";
        assertTrue(VoiceIntentMatcher.hasWakeWord(hiUtterance));
        assertEquals("संदेश फिर से पढ़ो", VoiceIntentMatcher.extractCommandText(hiUtterance));
        assertEquals(VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent("संदेश फिर से पढ़ो", "hi").getIntentType());

        // Malayalam wake word + command
        String mlUtterance = "Assistant, സന്ദേശം വീണ്ടും പറയൂ";
        assertTrue(VoiceIntentMatcher.hasWakeWord(mlUtterance));
        assertEquals("സന്ദേശം വീണ്ടും പറയൂ", VoiceIntentMatcher.extractCommandText(mlUtterance));
        assertEquals(VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent("സന്ദേശം വീണ്ടും പറയൂ", "ml").getIntentType());

        // Without wake word: ignored by wake word detector
        assertFalse(VoiceIntentMatcher.hasWakeWord("repeat message"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("संदेश फिर से पढ़ो"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("സന്ദേശം വീണ്ടും പറയൂ"));
    }

    @Test
    public void testTask21_MultilingualRepeatMessage_Responses() {
        // No message to repeat
        assertEquals("No message to repeat.", VoiceLanguageConfig.getNoMessageToRepeatResponse("en"));
        assertEquals("ಮರುಹೇಳಲು ಯಾವುದೇ ಸಂದೇಶವಿಲ್ಲ.", VoiceLanguageConfig.getNoMessageToRepeatResponse("kn"));
        assertEquals("दोबारा सुनाने के लिए कोई संदेश नहीं है।", VoiceLanguageConfig.getNoMessageToRepeatResponse("hi"));
        assertEquals("വീണ്ടും പറയാൻ സന്ദേശമൊന്നുമില്ല.", VoiceLanguageConfig.getNoMessageToRepeatResponse("ml"));

        // Active call
        assertEquals("A call is active.", VoiceLanguageConfig.getCallActiveResponse("en"));
        assertEquals("ಕರೆ ಸಕ್ರಿಯವಾಗಿದೆ.", VoiceLanguageConfig.getCallActiveResponse("kn"));
        assertEquals("एक कॉल सक्रिय है।", VoiceLanguageConfig.getCallActiveResponse("hi"));
        assertEquals("ഒരു കോൾ സജീവമാണ്.", VoiceLanguageConfig.getCallActiveResponse("ml"));

        // Localized prefix for caregiver says
        assertEquals("Your caregiver says: ", VoiceLanguageConfig.getCaregiverSaysPrefix("en"));
        assertEquals("ನಿಮ್ಮ ಕೇರ್ಗಿವರ್ ಹೇಳುತ್ತಾರೆ: ", VoiceLanguageConfig.getCaregiverSaysPrefix("kn"));
        assertEquals("आपके केयरगिवर कहते हैं: ", VoiceLanguageConfig.getCaregiverSaysPrefix("hi"));
        assertEquals("നിങ്ങളുടെ കെയർഗിവർ പറയുന്നു: ", VoiceLanguageConfig.getCaregiverSaysPrefix("ml"));

        // getLocalizedResponse routing for CMD_REPEAT_MESSAGE
        assertEquals("No message to repeat.", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_REPEAT_MESSAGE, VoiceCommandConstants.RESPONSE_NO_MESSAGE_TO_REPEAT, "en"));
        assertEquals("ಮರುಹೇಳಲು ಯಾವುದೇ ಸಂದೇಶವಿಲ್ಲ.", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_REPEAT_MESSAGE, VoiceCommandConstants.RESPONSE_NO_MESSAGE_TO_REPEAT, "kn"));
        assertEquals("दोबारा सुनाने के लिए कोई संदेश नहीं है।", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_REPEAT_MESSAGE, VoiceCommandConstants.RESPONSE_NO_MESSAGE_TO_REPEAT, "hi"));
        assertEquals("വീണ്ടും പറയാൻ സന്ദേശമൊന്നുമില്ല.", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_REPEAT_MESSAGE, VoiceCommandConstants.RESPONSE_NO_MESSAGE_TO_REPEAT, "ml"));

        assertEquals("A call is active.", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_REPEAT_MESSAGE, VoiceCommandConstants.RESPONSE_CALL_ACTIVE, "en"));
        assertEquals("ಕರೆ ಸಕ್ರಿಯವಾಗಿದೆ.", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_REPEAT_MESSAGE, VoiceCommandConstants.RESPONSE_CALL_ACTIVE, "kn"));
        assertEquals("एक कॉल सक्रिय है।", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_REPEAT_MESSAGE, VoiceCommandConstants.RESPONSE_CALL_ACTIVE, "hi"));
        assertEquals("ഒരു കോൾ സജീവമാണ്.", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_REPEAT_MESSAGE, VoiceCommandConstants.RESPONSE_CALL_ACTIVE, "ml"));
    }

    @Test
    public void testTask21_MultilingualRepeatMessage_DataSafety() {
        // Caregiver message containing "Assistant, repeat message" remains data
        String cmdMsg1 = "Assistant, repeat message";
        String spoken1 = VoiceLanguageConfig.getCaregiverSaysPrefix("en") + cmdMsg1;
        assertEquals("Your caregiver says: Assistant, repeat message", spoken1);

        // Caregiver message containing "Assistant, say that again" remains data
        String cmdMsg2 = "Assistant, say that again";
        String spoken2 = VoiceLanguageConfig.getCaregiverSaysPrefix("kn") + cmdMsg2;
        assertEquals("ನಿಮ್ಮ ಕೇರ್ಗಿವರ್ ಹೇಳುತ್ತಾರೆ: Assistant, say that again", spoken2);
    }

    // =========================================================================
    // TASK 22: MULTILINGUAL READ_NOTIFICATIONS TESTS (EN, KN, HI, ML)
    // =========================================================================

    @Test
    public void testTask22_MultilingualReadNotifications_EnglishPhrases() {
        String[] phrases = {
                "read notifications",
                "read my notifications",
                "read the notifications",
                "check notifications",
                "check my notifications",
                "read notification",
                "check notification",
                "tell me my notifications",
                "what are my notifications",
                "read new notifications",
                "read latest notifications",
                "read recent notifications",
                "tell me the latest notification",
                "read my latest notification"
        };
        for (String phrase : phrases) {
            assertEquals("Failed for English notification phrase: " + phrase,
                    VoiceIntentType.READ_NOTIFICATIONS,
                    processor.detectIntent(phrase, "en").getIntentType());
        }
    }

    @Test
    public void testTask22_MultilingualReadNotifications_KannadaPhrases() {
        String[] phrases = {
                "ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ",
                "ನನ್ನ ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ",
                "ಅಧಿಸೂಚನೆಗಳನ್ನು ಪರಿಶೀಲಿಸಿ",
                "ನನ್ನ ಅಧಿಸೂಚನೆಗಳನ್ನು ಪರಿಶೀಲಿಸಿ",
                "ಅಧಿಸೂಚನೆ ಓದಿ",
                "ಅಧಿಸೂಚನೆ ಪರಿಶೀಲಿಸಿ",
                "ಹೊಸ ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ",
                "ಇತ್ತೀಚಿನ ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ",
                "ನನ್ನ ಹೊಸ ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ",
                "ಇತ್ತೀಚಿನ ಅಧಿಸೂಚನೆಯನ್ನು ಹೇಳಿ",
                "ನನ್ನ ಅಧಿಸೂಚನೆಗಳನ್ನು ಹೇಳಿ"
        };
        for (String phrase : phrases) {
            assertEquals("Failed for Kannada notification phrase: " + phrase,
                    VoiceIntentType.READ_NOTIFICATIONS,
                    processor.detectIntent(phrase, "kn").getIntentType());
        }
    }

    @Test
    public void testTask22_MultilingualReadNotifications_HindiPhrases() {
        String[] phrases = {
                "सूचनाएँ पढ़ो",
                "मेरी सूचनाएँ पढ़ो",
                "नोटिफिकेशन पढ़ो",
                "मेरे नोटिफिकेशन पढ़ो",
                "सूचनाएँ चेक करो",
                "नोटिफिकेशन चेक करो",
                "मेरी सूचनाएँ चेक करो",
                "नए नोटिफिकेशन पढ़ो",
                "नए नोटिफिकेशन बताओ",
                "नवीनतम नोटिफिकेशन पढ़ो",
                "मेरी नवीनतम सूचना पढ़ो",
                "मुझे मेरी सूचनाएँ बताओ"
        };
        for (String phrase : phrases) {
            assertEquals("Failed for Hindi notification phrase: " + phrase,
                    VoiceIntentType.READ_NOTIFICATIONS,
                    processor.detectIntent(phrase, "hi").getIntentType());
        }
    }

    @Test
    public void testTask22_MultilingualReadNotifications_MalayalamPhrases() {
        String[] phrases = {
                "അറിയിപ്പുകൾ വായിക്കൂ",
                "എന്റെ അറിയിപ്പുകൾ വായിക്കൂ",
                "അറിയിപ്പുകൾ പരിശോധിക്കൂ",
                "എന്റെ അറിയിപ്പുകൾ പരിശോധിക്കൂ",
                "നോട്ടിഫിക്കേഷൻ വായിക്കൂ",
                "എന്റെ നോട്ടിഫിക്കേഷനുകൾ വായിക്കൂ",
                "പുതിയ അറിയിപ്പുകൾ വായിക്കൂ",
                "പുതിയ നോട്ടിഫിക്കേഷനുകൾ വായിക്കൂ",
                "ഏറ്റവും പുതിയ അറിയിപ്പ് വായിക്കൂ",
                "എന്റെ പുതിയ അറിയിപ്പുകൾ പറയൂ",
                "എന്റെ അറിയിപ്പുകൾ പറയൂ"
        };
        for (String phrase : phrases) {
            assertEquals("Failed for Malayalam notification phrase: " + phrase,
                    VoiceIntentType.READ_NOTIFICATIONS,
                    processor.detectIntent(phrase, "ml").getIntentType());
        }
    }

    @Test
    public void testTask22_MultilingualReadNotifications_NegativeRejections() {
        // English single word rejections -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("read", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("notification", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("notifications", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("check", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("new", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("latest", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("tell", "en").getIntentType());

        // Kannada single word rejections -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಅಧಿಸೂಚನೆ", "kn").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಅಧಿಸೂಚನೆಗಳು", "kn").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಓದಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಪರಿಶೀಲಿಸಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಹೊಸದು", "kn").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ಇತ್ತೀಚಿನ", "kn").getIntentType());

        // Hindi single word rejections -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("सूचना", "hi").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("सूचनाएँ", "hi").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("नोटिफिकेशन", "hi").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("पढ़ो", "hi").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("चेक", "hi").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("नया", "hi").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("नवीनतम", "hi").getIntentType());

        // Malayalam single word rejections -> UNKNOWN
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("അറിയിപ്പ്", "ml").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("അറിയിപ്പുകൾ", "ml").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("വായിക്കൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("പരിശോധിക്കൂ", "ml").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("പുതിയ", "ml").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, processor.detectIntent("ഏറ്റവും പുതിയത്", "ml").getIntentType());
    }

    @Test
    public void testTask22_MultilingualReadNotifications_WakeWordGating() {
        // English wake word + command
        String enUtterance = "Assistant, read notifications";
        assertTrue(VoiceIntentMatcher.hasWakeWord(enUtterance));
        assertEquals("read notifications", VoiceIntentMatcher.extractCommandText(enUtterance));
        assertEquals(VoiceIntentType.READ_NOTIFICATIONS, processor.detectIntent("read notifications", "en").getIntentType());

        // Kannada wake word + command
        String knUtterance = "Assistant, ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ";
        assertTrue(VoiceIntentMatcher.hasWakeWord(knUtterance));
        assertEquals("ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ", VoiceIntentMatcher.extractCommandText(knUtterance));
        assertEquals(VoiceIntentType.READ_NOTIFICATIONS, processor.detectIntent("ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ", "kn").getIntentType());

        // Hindi wake word + command
        String hiUtterance = "Assistant, सूचनाएँ पढ़ो";
        assertTrue(VoiceIntentMatcher.hasWakeWord(hiUtterance));
        assertEquals("सूचनाएँ पढ़ो", VoiceIntentMatcher.extractCommandText(hiUtterance));
        assertEquals(VoiceIntentType.READ_NOTIFICATIONS, processor.detectIntent("सूचनाएँ पढ़ो", "hi").getIntentType());

        // Malayalam wake word + command
        String mlUtterance = "Assistant, അറിയിപ്പുകൾ വായിക്കൂ";
        assertTrue(VoiceIntentMatcher.hasWakeWord(mlUtterance));
        assertEquals("അറിയിപ്പുകൾ വായിക്കൂ", VoiceIntentMatcher.extractCommandText(mlUtterance));
        assertEquals(VoiceIntentType.READ_NOTIFICATIONS, processor.detectIntent("അറിയിപ്പുകൾ വായിക്കൂ", "ml").getIntentType());

        // Without wake word: ignored by wake word detector
        assertFalse(VoiceIntentMatcher.hasWakeWord("read notifications"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("सूचनाएँ पढ़ो"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("അറിയിപ്പുകൾ വായിക്കൂ"));
    }

    @Test
    public void testTask22_MultilingualReadNotifications_Responses() {
        // No notifications
        assertEquals("No new notifications.", VoiceLanguageConfig.getNoNotificationsResponse("en"));
        assertEquals("ಹೊಸ ಅಧಿಸೂಚನೆಗಳಿಲ್ಲ.", VoiceLanguageConfig.getNoNotificationsResponse("kn"));
        assertEquals("कोई नई सूचना नहीं है।", VoiceLanguageConfig.getNoNotificationsResponse("hi"));
        assertEquals("പുതിയ അറിയിപ്പുകളൊന്നുമില്ല.", VoiceLanguageConfig.getNoNotificationsResponse("ml"));

        // Single notification intro
        assertEquals("Your latest notification says: ", VoiceLanguageConfig.getSingleNotificationIntro("en"));
        assertEquals("ನಿಮ್ಮ ಇತ್ತೀಚಿನ ಅಧಿಸೂಚನೆ ಹೀಗಿದೆ: ", VoiceLanguageConfig.getSingleNotificationIntro("kn"));
        assertEquals("आपकी नवीनतम सूचना कहती है: ", VoiceLanguageConfig.getSingleNotificationIntro("hi"));
        assertEquals("നിങ്ങളുടെ ഏറ്റവും പുതിയ അറിയിപ്പ് പറയുന്നത്: ", VoiceLanguageConfig.getSingleNotificationIntro("ml"));

        // Multiple notification intro
        assertEquals("You have 3 recent notifications.", VoiceLanguageConfig.getMultipleNotificationsIntro(3, "en"));
        assertEquals("ನಿಮ್ಮ ಬಳಿ 3 ಇತ್ತೀಚಿನ ಅಧಿಸೂಚನೆಗಳಿವೆ.", VoiceLanguageConfig.getMultipleNotificationsIntro(3, "kn"));
        assertEquals("आपके पास 3 हाल की सूचनाएँ हैं।", VoiceLanguageConfig.getMultipleNotificationsIntro(3, "hi"));
        assertEquals("നിങ്ങളുടെ പക്കൽ 3 പുതിയ അറിയിപ്പുകളുണ്ട്.", VoiceLanguageConfig.getMultipleNotificationsIntro(3, "ml"));

        // Active call response
        assertEquals("A call is active.", VoiceLanguageConfig.getCallActiveResponse("en"));
        assertEquals("ಕರೆ ಸಕ್ರಿಯವಾಗಿದೆ.", VoiceLanguageConfig.getCallActiveResponse("kn"));
        assertEquals("एक कॉल सक्रिय है।", VoiceLanguageConfig.getCallActiveResponse("hi"));
        assertEquals("ഒരു കോൾ സജീവമാണ്.", VoiceLanguageConfig.getCallActiveResponse("ml"));

        // getLocalizedResponse routing for CMD_READ_NOTIFICATIONS
        assertEquals("No new notifications.", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_READ_NOTIFICATIONS, VoiceCommandConstants.RESPONSE_NO_NOTIFICATIONS, "en"));
        assertEquals("ಹೊಸ ಅಧಿಸೂಚನೆಗಳಿಲ್ಲ.", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_READ_NOTIFICATIONS, VoiceCommandConstants.RESPONSE_NO_NOTIFICATIONS, "kn"));
        assertEquals("कोई नई सूचना नहीं है।", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_READ_NOTIFICATIONS, VoiceCommandConstants.RESPONSE_NO_NOTIFICATIONS, "hi"));
        assertEquals("പുതിയ അറിയിപ്പുകളൊന്നുമില്ല.", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_READ_NOTIFICATIONS, VoiceCommandConstants.RESPONSE_NO_NOTIFICATIONS, "ml"));

        assertEquals("A call is active.", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_READ_NOTIFICATIONS, VoiceCommandConstants.RESPONSE_CALL_ACTIVE, "en"));
        assertEquals("ಕರೆ ಸಕ್ರಿಯವಾಗಿದೆ.", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_READ_NOTIFICATIONS, VoiceCommandConstants.RESPONSE_CALL_ACTIVE, "kn"));
        assertEquals("एक कॉल सक्रिय है।", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_READ_NOTIFICATIONS, VoiceCommandConstants.RESPONSE_CALL_ACTIVE, "hi"));
        assertEquals("ഒരു കോൾ സജീവമാണ്.", VoiceLanguageConfig.getLocalizedResponse(
                VoiceCommandConstants.CMD_READ_NOTIFICATIONS, VoiceCommandConstants.RESPONSE_CALL_ACTIVE, "ml"));
    }

    @Test
    public void testTask22_MultilingualReadNotifications_DataSafety() {
        // Notification text containing command phrases remains data
        NotificationService.NotificationItem item1 =
                new NotificationService.NotificationItem("Notification", "Assistant, read notifications", "text", 1000L);
        assertEquals("Assistant, read notifications", item1.getSpokenText());

        NotificationService.NotificationItem item2 =
                new NotificationService.NotificationItem("Notification", "Assistant, open profile", "text", 1000L);
        assertEquals("Assistant, open profile", item2.getSpokenText());

        NotificationService.NotificationItem item3 =
                new NotificationService.NotificationItem("Notification", "Assistant, call caregiver", "text", 1000L);
        assertEquals("Assistant, call caregiver", item3.getSpokenText());

        NotificationService.NotificationItem item4 =
                new NotificationService.NotificationItem("Caregiver", "Hello from caregiver", "text", 1000L);
        assertEquals("Caregiver: Hello from caregiver", item4.getSpokenText());
    }

    @Test
    public void testTask23_CaregiverMessageAndNotificationReliability() {
        // 1. Deduplication logic in CaregiverSoundManager
        String msgId = "test_msg_" + System.currentTimeMillis();
        String senderId = "caregiver_uid_123";
        String content = "Please take your evening medicine.";

        assertTrue("First markMessageProcessed should return true",
                CaregiverSoundManager.markMessageProcessed(msgId, senderId, content));
        assertFalse("Duplicate messageId should return false",
                CaregiverSoundManager.markMessageProcessed(msgId, senderId, content));
        assertFalse("Duplicate content from same sender within 8s window should return false",
                CaregiverSoundManager.markMessageProcessed("different_id_" + System.currentTimeMillis(), senderId, content));

        // 2. NotificationService caching
        NotificationService.clearRecentNotificationsForTesting();
        NotificationService.recordNotification("Caregiver", "Dinner is ready", "text", System.currentTimeMillis());
        NotificationService.recordNotification("Caregiver", "Hello Blind User", "text", System.currentTimeMillis() + 100);

        // Verification of NotificationItem retrieval
        NotificationService.NotificationItem top =
                new NotificationService.NotificationItem("Caregiver", "Hello Blind User", "text", System.currentTimeMillis());
        assertEquals("Caregiver: Hello Blind User", top.getSpokenText());

        // 3. Separation of sound modes
        assertEquals("default", CaregiverSoundManager.MODE_DEFAULT);
        assertEquals("custom", CaregiverSoundManager.MODE_CUSTOM);
        assertEquals("silent", CaregiverSoundManager.MODE_SILENT);
    }

    @Test
    public void testTask24_CallCaregiverAliases() {
        String lang = "kn";
        String[] variations = {
                "Assistant, ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ",
                "Assistant, ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ",
                "Assistant, ಕೇರ್ಗಿವರ್ಗೆ ಕರೆ ಮಾಡಿ",
                "Assistant, ಕೇರ್‌ಗಿವರ್‌ಗೆ ಕರೆ ಮಾಡಿ",
                "Assistant, ಕೇರ್ಟೇಕರ್ಗೆ ಕರೆ ಮಾಡಿ",
                "Assistant, ಕೇರ್ ಟೇಕರ್ಗೆ ಕರೆ ಮಾಡಿ",
                "ಆರೈಕೆದಾರರಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ಆರೈಕೆದಾರನಿಗೆ ಕರೆ ಮಾಡಿ",
                "ಆರೈಕೆದಾರನಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ಸಹಾಯಕನಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ಸಹಾಯಕರಿಗೆ ಕರೆ ಮಾಡಿ",
                "ಸಹಾಯಕರಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ಕೇರ್ಗಿವರ್ಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ಕೇರ್ಟೇಕರ್ಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ನನ್ನ ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ",
                "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ",
                "ನನ್ನ ಕೇರ್ಗಿವರ್ಗೆ ಕರೆ ಮಾಡಿ",
                "ನನ್ನ ಕೇರ್ಟೇಕರ್ಗೆ ಕರೆ ಮಾಡಿ",
                "ಆರೈಕೆದಾರರಿಗೆ ಕರೆ",
                "ಸಹಾಯಕನಿಗೆ ಕರೆ",
                "ಕೇರ್ಗಿವರ್ಗೆ ಕರೆ",
                "ಕೇರ್ಟೇಕರ್ಗೆ ಕರೆ"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.CALL_CAREGIVER",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
        }
    }

    @Test
    public void testTask24_SendMessageAliases() {
        String lang = "kn";
        String[] variations = {
                "Assistant, ಆರೈಕೆದಾರರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "Assistant, ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "Assistant, ಕೇರ್ಗಿವರ್ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "Assistant, ಕೇರ್‌ಗಿವರ್‌ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "Assistant, ಕೇರ್ಟೇಕರ್ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "Assistant, ಕೇರ್ ಟೇಕರ್ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಆರೈಕೆದಾರನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಆರೈಕೆದಾರರಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ಆರೈಕೆದಾರನಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ಆರೈಕೆದಾರರಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ",
                "ಸಹಾಯಕನಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ಸಹಾಯಕರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಸಹಾಯಕರಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ",
                "ಕೇರ್ಗಿವರ್ಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ಕೇರ್ಟೇಕರ್ಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ನನ್ನ ಆರೈಕೆದಾರರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ನನ್ನ ಕೇರ್ಗಿವರ್ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ನನ್ನ ಕೇರ್ಟೇಕರ್ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.SEND_MESSAGE",
                    VoiceIntentType.SEND_MESSAGE, intent.getIntentType());
        }
    }

    @Test
    public void testTask24_ReadMessagesAliases() {
        String lang = "kn";
        String[] variations = {
                "Assistant, ಆರೈಕೆದಾರರ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "Assistant, ಸಹಾಯಕನ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "Assistant, ಕೇರ್ಗಿವರ್ನ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "Assistant, ಕೇರ್ಟೇಕರ್ನ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ಆರೈಕೆದಾರನ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ಆರೈಕೆದಾರರ ಸಂದೇಶ ಓದಿ",
                "ಆರೈಕೆದಾರರ ಮೆಸೇಜ್ ಓದಿ",
                "ಆರೈಕೆದಾರರ ಸಂದೇಶಗಳನ್ನು ಪರಿಶೀಲಿಸಿ",
                "ಸಹಾಯಕರ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ಸಹಾಯಕನ ಸಂದೇಶ ಓದಿ",
                "ಸಹಾಯಕನ ಮೆಸೇಜ್ ಓದಿ",
                "ಸಹಾಯಕನ ಸಂದೇಶಗಳನ್ನು ಪರಿಶೀಲಿಸಿ",
                "ಕೇರ್ಗಿವರ್ನ ಸಂದೇಶ ಓದಿ",
                "ಕೇರ್ಗಿವರ್ನ ಮೆಸೇಜ್ ಓದಿ",
                "ಕೇರ್ಟೇಕರ್ನ ಸಂದೇಶ ಓದಿ",
                "ಕೇರ್ಟೇಕರ್ನ ಮೆಸೇಜ್ ಓದಿ",
                "ನನ್ನ ಆರೈಕೆದಾರರ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ನನ್ನ ಸಹಾಯಕನ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ನನ್ನ ಕೇರ್ಗಿವರ್ನ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ನನ್ನ ಕೇರ್ಟೇಕರ್ನ ಸಂದೇಶಗಳನ್ನು ಓದಿ"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.READ_MESSAGES",
                    VoiceIntentType.READ_MESSAGES, intent.getIntentType());
        }
    }

    @Test
    public void testTask24_MessageCountAliases() {
        String lang = "kn";
        String[] variations = {
                "Assistant, ಆರೈಕೆದಾರರಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "Assistant, ಸಹಾಯಕನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "Assistant, ಕೇರ್ಗಿವರ್ನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "Assistant, ಕೇರ್ಟೇಕರ್ನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಆರೈಕೆದಾರನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಆರೈಕೆದಾರರ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಆರೈಕೆದಾರರಿಂದ ಎಷ್ಟು ಮೆಸೇಜ್ಗಳಿವೆ",
                "ಆರೈಕೆದಾರರಿಂದ ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ",
                "ಸಹಾಯಕರಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಸಹಾಯಕನ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಸಹಾಯಕನಿಂದ ಎಷ್ಟು ಮೆಸೇಜ್ಗಳಿವೆ",
                "ಸಹಾಯಕನಿಂದ ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ",
                "ಕೇರ್ಗಿವರ್ನಿಂದ ಎಷ್ಟು ಮೆಸೇಜ್ಗಳಿವೆ",
                "ಕೇರ್ಟೇಕರ್ನಿಂದ ಎಷ್ಟು ಮೆಸೇಜ್ಗಳಿವೆ",
                "ನನ್ನ ಆರೈಕೆದಾರರಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ನನ್ನ ಸಹಾಯಕನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ನನ್ನ ಕೇರ್ಗಿವರ್ನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ನನ್ನ ಕೇರ್ಟೇಕರ್ನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.MESSAGE_COUNT",
                    VoiceIntentType.MESSAGE_COUNT, intent.getIntentType());
        }
    }

    @Test
    public void testTask24_RepeatMessageAliases() {
        String lang = "kn";
        String[] variations = {
                "Assistant, ಆರೈಕೆದಾರರ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "Assistant, ಸಹಾಯಕನ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "Assistant, ಕೇರ್ಗಿವರ್ನ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "Assistant, ಕೇರ್ಟೇಕರ್ನ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಆರೈಕೆದಾರನ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಆರೈಕೆದಾರರ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಆರೈಕೆದಾರರ ಮೆಸೇಜ್ ಮತ್ತೆ ಹೇಳಿ",
                "ಆರೈಕೆದಾರರ ಸಂದೇಶವನ್ನು ಪುನರಾವರ್ತಿಸಿ",
                "ಸಹಾಯಕರ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಸಹಾಯಕನ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಸಹಾಯಕನ ಮೆಸೇಜ್ ಮತ್ತೆ ಹೇಳಿ",
                "ಸಹಾಯಕನ ಸಂದೇಶವನ್ನು ಪುನರಾವರ್ತಿಸಿ",
                "ಕೇರ್ಗಿವರ್ನ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಕೇರ್ಗಿವರ್ನ ಮೆಸೇಜ್ ಮತ್ತೆ ಹೇಳಿ",
                "ಕೇರ್ಟೇಕರ್ನ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಕೇರ್ಟೇಕರ್ನ ಮೆಸೇಜ್ ಮತ್ತೆ ಹೇಳಿ",
                "ನನ್ನ ಆರೈಕೆದಾರರ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ನನ್ನ ಸಹಾಯಕನ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ನನ್ನ ಕೇರ್ಗಿವರ್ನ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ನನ್ನ ಕೇರ್ಟೇಕರ್ನ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.REPEAT_MESSAGE",
                    VoiceIntentType.REPEAT_MESSAGE, intent.getIntentType());
        }
    }

    @Test
    public void testTask24_CriticalNegativeRejections() {
        String lang = "kn";
        String[] negatives = {
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

        for (String phrase : negatives) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Negative phrase '" + phrase + "' MUST resolve to UNKNOWN",
                    VoiceIntentType.UNKNOWN, intent.getIntentType());
        }
    }

    @Test
    public void testTask24_UniversalWakeWordGatingAndDataSafety() {
        // Universal wake word gating
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Hey assistant, ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ"));

        // Extracted command
        assertEquals("ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ", VoiceIntentMatcher.extractCommandText("Assistant, ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ"));
        assertEquals("ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ", VoiceIntentMatcher.extractCommandText("Hey assistant, ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ"));

        // Message data isolation: incoming message text containing commands is treated as plain string data
        String incomingMessagePayload = "ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ ಮತ್ತು ಸಂದೇಶ ಕಳುಹಿಸಿ";
        NotificationService.NotificationItem item =
                new NotificationService.NotificationItem("Caregiver", incomingMessagePayload, "text", 2000L);
        assertEquals("Caregiver: ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ ಮತ್ತು ಸಂದೇಶ ಕಳುಹಿಸಿ", item.getSpokenText());
    }

    @Test
    public void testTask25_HindiCallCaregiverAliases() {
        String lang = "hi";
        String[] variations = {
                "Assistant, देखभाल करने वाले को कॉल करो",
                "Assistant, मेरे देखभाल करने वाले को कॉल करो",
                "Assistant, देखभाल करने वाले को फोन करो",
                "Assistant, मेरे देखभाल करने वाले को फोन करो",
                "Assistant, सहायक को कॉल करो",
                "Assistant, मेरे सहायक को कॉल करो",
                "Assistant, सहायक को फोन करो",
                "Assistant, मेरे सहायक को फोन करो",
                "Assistant, केयरगिवर को कॉल करो",
                "Assistant, मेरे केयरगिवर को कॉल करो",
                "Assistant, केयरगिवर को फोन करो",
                "Assistant, केयरटेकर को कॉल करो",
                "Assistant, मेरे केयरटेकर को कॉल करो",
                "Assistant, केयरटेकर को फोन करो",
                "देखभाल करने वाले को कॉल करें",
                "सहायक को कॉल करें",
                "केयरगिवर को कॉल करें",
                "केयरटेकर को कॉल करें",
                "देखभाल करने वाले को कॉल लगाओ",
                "सहायक को कॉल लगाओ",
                "देखभाल करने वाले को फोन लगाओ",
                "सहायक को फोन लगाओ"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.CALL_CAREGIVER",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
        }
    }

    @Test
    public void testTask25_HindiSendMessageAliases() {
        String lang = "hi";
        String[] variations = {
                "Assistant, देखभाल करने वाले को संदेश भेजो",
                "Assistant, मेरे देखभाल करने वाले को संदेश भेजो",
                "Assistant, सहायक को संदेश भेजो",
                "Assistant, मेरे सहायक को संदेश भेजो",
                "Assistant, केयरगिवर को संदेश भेजो",
                "Assistant, मेरे केयरगिवर को संदेश भेजो",
                "Assistant, केयरटेकर को संदेश भेजो",
                "Assistant, मेरे केयरटेकर को संदेश भेजो",
                "देखभाल करने वाले को मैसेज भेजो",
                "सहायक को मैसेज भेजो",
                "केयरगिवर को मैसेज भेजो",
                "केयरटेकर को मैसेज भेजो",
                "देखभाल करने वाले को संदेश भेजें",
                "सहायक को संदेश भेजें",
                "केयरगिवर को संदेश भेजें",
                "केयरटेकर को संदेश भेजें"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.SEND_MESSAGE",
                    VoiceIntentType.SEND_MESSAGE, intent.getIntentType());
        }
    }

    @Test
    public void testTask25_HindiReadMessagesAliases() {
        String lang = "hi";
        String[] variations = {
                "Assistant, देखभाल करने वाले के संदेश पढ़ो",
                "Assistant, मेरे देखभाल करने वाले के संदेश पढ़ो",
                "Assistant, सहायक के संदेश पढ़ो",
                "Assistant, मेरे सहायक के संदेश पढ़ो",
                "Assistant, केयरगिवर के संदेश पढ़ो",
                "Assistant, मेरे केयरगिवर के संदेश पढ़ो",
                "Assistant, केयरटेकर के संदेश पढ़ो",
                "देखभाल करने वाले के संदेश पढ़कर सुनाओ",
                "सहायक के संदेश पढ़कर सुनाओ",
                "केयरगिवर के संदेश पढ़कर सुनाओ",
                "केयरटेकर के संदेश पढ़कर सुनाओ",
                "देखभाल करने वाले के संदेश सुनाओ",
                "सहायक के संदेश सुनाओ",
                "केयरगिवर के मैसेज पढ़ो",
                "केयरटेकर के मैसेज पढ़ो",
                "संदेश पढ़कर सुनाओ",
                "मैसेज पढ़कर सुनाओ",
                "संदेश सुनाओ",
                "मैसेज सुनाओ"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.READ_MESSAGES",
                    VoiceIntentType.READ_MESSAGES, intent.getIntentType());
        }
    }

    @Test
    public void testTask25_HindiMessageCountAliases() {
        String lang = "hi";
        String[] variations = {
                "Assistant, देखभाल करने वाले के कितने संदेश हैं",
                "Assistant, मेरे देखभाल करने वाले के कितने संदेश हैं",
                "Assistant, सहायक के कितने संदेश हैं",
                "Assistant, मेरे सहायक के कितने संदेश हैं",
                "Assistant, केयरगिवर के कितने संदेश हैं",
                "Assistant, मेरे केयरगिवर के कितने संदेश हैं",
                "Assistant, केयरटेकर के कितने संदेश हैं",
                "देखभाल करने वाले के कितने मैसेज हैं",
                "सहायक के कितने मैसेज हैं",
                "केयरगिवर के कितने मैसेज हैं",
                "केयरटेकर के कितने मैसेज हैं",
                "देखभाल करने वाले से कितने संदेश हैं",
                "सहायक से कितने संदेश हैं",
                "देखभाल करने वाले के संदेश गिनो",
                "सहायक के संदेश गिनो",
                "केयरगिवर के संदेश गिनो",
                "केयरटेकर के संदेश गिनो"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.MESSAGE_COUNT",
                    VoiceIntentType.MESSAGE_COUNT, intent.getIntentType());
        }
    }

    @Test
    public void testTask25_HindiRepeatMessageAliases() {
        String lang = "hi";
        String[] variations = {
                "Assistant, देखभाल करने वाले का संदेश फिर से सुनाओ",
                "Assistant, मेरे देखभाल करने वाले का संदेश फिर से बताओ",
                "Assistant, सहायक का संदेश फिर से सुनाओ",
                "Assistant, सहायक का संदेश फिर से बताओ",
                "Assistant, मेरे सहायक का संदेश फिर से सुनाओ",
                "Assistant, केयरगिवर का संदेश फिर से सुनाओ",
                "Assistant, केयरगिवर का संदेश दोबारा सुनाओ",
                "Assistant, केयरटेकर का संदेश फिर से बताओ",
                "Assistant, केयरटेकर का संदेश दोबारा सुनाओ",
                "देखभाल करने वाले का संदेश दोबारा बताओ",
                "सहायक का संदेश दोबारा बताओ",
                "दोबारा सुनाओ",
                "फिर से बताओ",
                "फिर से सुनाओ"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.REPEAT_MESSAGE",
                    VoiceIntentType.REPEAT_MESSAGE, intent.getIntentType());
        }
    }

    @Test
    public void testTask25_HindiCriticalNegativeRejections() {
        String lang = "hi";
        String[] negatives = {
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

        for (String phrase : negatives) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Negative phrase '" + phrase + "' MUST resolve to UNKNOWN",
                    VoiceIntentType.UNKNOWN, intent.getIntentType());
        }
    }

    @Test
    public void testTask25_HindiWakeWordGatingAndDataSafety() {
        // Universal wake word gating
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, देखभाल करने वाले को कॉल करो"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Hey assistant, सहायक को संदेश भेजो"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("देखभाल करने वाले को कॉल करो"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("सहायक को संदेश भेजो"));

        // Extracted command
        assertEquals("देखभाल करने वाले को कॉल करो", VoiceIntentMatcher.extractCommandText("Assistant, देखभाल करने वाले को कॉल करो"));
        assertEquals("सहायक को संदेश भेजो", VoiceIntentMatcher.extractCommandText("Hey assistant, सहायक को संदेश भेजो"));

        // Message data isolation: incoming message text containing commands is treated as plain string data
        String incomingMessagePayload = "देखभाल करने वाले को कॉल करो और सहायक को संदेश भेजो";
        NotificationService.NotificationItem item =
                new NotificationService.NotificationItem("Caregiver", incomingMessagePayload, "text", 4000L);
        assertEquals("Caregiver: देखभाल करने वाले को कॉल करो और सहायक को संदेश भेजो", item.getSpokenText());
    }

    // ==========================================
    // TASK 26 NATURAL MALAYALAM CAREGIVER ALIASES TESTS
    // ==========================================

    @Test
    public void testTask26_MalayalamCallCaregiverAliases() {
        String lang = "ml";
        String[] variations = {
                "Assistant, പരിചരിക്കുന്നയാളെ വിളിക്കൂ",
                "Assistant, എന്റെ പരിചരിക്കുന്നയാളെ വിളിക്കൂ",
                "Assistant, പരിചാരകനെ വിളിക്കൂ",
                "Assistant, എന്റെ പരിചാരകനെ വിളിക്കൂ",
                "Assistant, സഹായിയെ വിളിക്കൂ",
                "Assistant, എന്റെ സഹായിയെ വിളിക്കൂ",
                "Assistant, കെയർഗിവറെ വിളിക്കൂ",
                "Assistant, എന്റെ കെയർഗിവറെ വിളിക്കൂ",
                "Assistant, കെയർ ഗിവറെ വിളിക്കൂ",
                "Assistant, കെയർടേക്കറെ വിളിക്കൂ",
                "Assistant, എന്റെ കെയർടേക്കറെ വിളിക്കൂ",
                "Assistant, കെയർ ടേക്കറെ വിളിക്കൂ",
                "പരിചരിക്കുന്നയാളെ ഫോൺ ചെയ്യൂ",
                "പരിചാരകനെ ഫോൺ ചെയ്യൂ",
                "സഹായിയെ ഫോൺ ചെയ്യൂ",
                "കെയർഗിവറെ ഫോൺ ചെയ്യൂ",
                "കെയർടേക്കറെ ഫോൺ ചെയ്യൂ",
                "പരിചരിക്കുന്നയാളെ കോൾ ചെയ്യൂ",
                "പരിചാരകനെ കോൾ ചെയ്യൂ",
                "സഹായിയെ കോൾ ചെയ്യൂ",
                "കെയർഗിവറെ കോൾ ചെയ്യൂ",
                "കെയർടേക്കറെ കോൾ ചെയ്യൂ",
                "പരിചരിക്കുന്നയാളെ വിളിക്കുക",
                "പരിചാരകനെ വിളിക്കുക",
                "സഹായിയെ വിളിക്കുക"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.CALL_CAREGIVER",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
        }
    }

    @Test
    public void testTask26_MalayalamSendMessageAliases() {
        String lang = "ml";
        String[] variations = {
                "Assistant, പരിചരിക്കുന്നയാൾക്ക് സന്ദേശം അയയ്ക്കൂ",
                "Assistant, എന്റെ പരിചരിക്കുന്നയാൾക്ക് സന്ദേശം അയയ്ക്കൂ",
                "Assistant, പരിചാരകന് സന്ദേശം അയയ്ക്കൂ",
                "Assistant, എന്റെ പരിചാരകന് സന്ദേശം അയയ്ക്കൂ",
                "Assistant, സഹായിക്ക് സന്ദേശം അയയ്ക്കൂ",
                "Assistant, എന്റെ സഹായിക്ക് സന്ദേശം അയയ്ക്കൂ",
                "Assistant, കെയർഗിവറിന് സന്ദേശം അയയ്ക്കൂ",
                "Assistant, എന്റെ കെയർഗിവറിന് സന്ദേശം അയയ്ക്കൂ",
                "Assistant, കെയർടേക്കറിന് സന്ദേശം അയയ്ക്കൂ",
                "Assistant, എന്റെ കെയർടേക്കറിന് സന്ദേശം അയയ്ക്കൂ",
                "പരിചരിക്കുന്നയാൾക്ക് മെസേജ് അയയ്ക്കൂ",
                "പരിചാരകന് മെസേജ് അയയ്ക്കൂ",
                "സഹായിക്ക് മെസേജ് അയയ്ക്കൂ",
                "കെയർഗിവറിന് മെസേജ് അയയ്ക്കൂ",
                "കെയർടേക്കറിന് മെസേജ് അയയ്ക്കൂ",
                "പരിചരിക്കുന്നയാൾക്ക് സന്ദേശം അയക്കൂ",
                "പരിചാരകന് സന്ദേശം അയക്കൂ",
                "സഹായിക്ക് സന്ദേശം അയക്കൂ",
                "കെയർഗിവറിന് സന്ദേശം അയക്കൂ",
                "കെയർടേക്കറിന് സന്ദേശം അയക്കൂ"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.SEND_MESSAGE",
                    VoiceIntentType.SEND_MESSAGE, intent.getIntentType());
        }
    }

    @Test
    public void testTask26_MalayalamReadMessagesAliases() {
        String lang = "ml";
        String[] variations = {
                "Assistant, പരിചരിക്കുന്നയാളുടെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "Assistant, എന്റെ പരിചരിക്കുന്നയാളുടെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "Assistant, പരിചാരകന്റെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "Assistant, എന്റെ പരിചാരകന്റെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "Assistant, സഹായിയുടെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "Assistant, എന്റെ സഹായിയുടെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "Assistant, കെയർഗിവറിന്റെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "Assistant, എന്റെ കെയർഗിവറിന്റെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "Assistant, കെയർടേക്കറിന്റെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "Assistant, എന്റെ കെയർടേക്കറിന്റെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "പരിചരിക്കുന്നയാളുടെ സന്ദേശം വായിക്കൂ",
                "പരിചാരകന്റെ സന്ദേശം വായിക്കൂ",
                "സഹായിയുടെ സന്ദേശം വായിക്കൂ",
                "കെയർഗിവറിന്റെ സന്ദേശം വായിക്കൂ",
                "കെയർടേക്കറിന്റെ സന്ദേശം വായിക്കൂ",
                "പരിചരിക്കുന്നയാളുടെ സന്ദേശങ്ങൾ കേൾപ്പിക്കൂ",
                "സഹായിയുടെ സന്ദേശങ്ങൾ കേൾപ്പിക്കൂ",
                "കെയർഗിവറിന്റെ സന്ദേശങ്ങൾ കേൾപ്പിക്കൂ",
                "സന്ദേശങ്ങൾ കേൾപ്പിക്കൂ",
                "സന്ദേശം കേൾപ്പിക്കൂ"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.READ_MESSAGES",
                    VoiceIntentType.READ_MESSAGES, intent.getIntentType());
        }
    }

    @Test
    public void testTask26_MalayalamMessageCountAliases() {
        String lang = "ml";
        String[] variations = {
                "Assistant, പരിചരിക്കുന്നയാളുടെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "Assistant, എന്റെ പരിചരിക്കുന്നയാളുടെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "Assistant, പരിചാരകന്റെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "Assistant, എന്റെ പരിചാരകന്റെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "Assistant, സഹായിയുടെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "Assistant, എന്റെ സഹായിയുടെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "Assistant, കെയർഗിവറിന്റെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "Assistant, എന്റെ കെയർഗിവറിന്റെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "Assistant, കെയർടേക്കറിന്റെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "പരിചരിക്കുന്നയാളുടെ എത്ര മെസേജുകളുണ്ട്",
                "പരിചാരകന്റെ എത്ര മെസേജുകളുണ്ട്",
                "സഹായിയുടെ എത്ര മെസേജുകളുണ്ട്",
                "കെയർഗിവറിന്റെ എത്ര മെസേജുകളുണ്ട്",
                "കെയർടേക്കറിന്റെ എത്ര മെസേജുകളുണ്ട്",
                "പരിചരിക്കുന്നയാളുടെ സന്ദേശങ്ങൾ എണ്ണൂ",
                "സഹായിയുടെ സന്ദേശങ്ങൾ എണ്ണൂ",
                "കെയർഗിവറിന്റെ സന്ദേശങ്ങൾ എണ്ണൂ",
                "സന്ദേശങ്ങൾ എണ്ണൂ",
                "മെസേജുകൾ എണ്ണൂ"
        };

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.MESSAGE_COUNT",
                    VoiceIntentType.MESSAGE_COUNT, intent.getIntentType());
        }
    }

    @Test
    public void testTask26_MalayalamRepeatMessageAliases() {
        String lang = "ml";
        String[] variations = {
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

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.REPEAT_MESSAGE",
                    VoiceIntentType.REPEAT_MESSAGE, intent.getIntentType());
        }
    }

    @Test
    public void testTask26_MalayalamCriticalNegativeRejections() {
        String lang = "ml";
        String[] negatives = {
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

        for (String phrase : negatives) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Negative phrase '" + phrase + "' MUST resolve to UNKNOWN",
                    VoiceIntentType.UNKNOWN, intent.getIntentType());
        }
    }

    @Test
    public void testTask26_MalayalamWakeWordGatingAndDataSafety() {
        // Universal wake word gating
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, പരിചരിക്കുന്നയാളെ വിളിക്കൂ"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Hey assistant, സഹായിക്ക് സന്ദേശം അയയ്ക്കൂ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("പരിചരിക്കുന്നയാളെ വിളിക്കൂ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("സഹായിയെ വിളിക്കൂ"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("കെയർഗിവറെ വിളിക്കൂ"));

        // Extracted command
        assertEquals("പരിചരിക്കുന്നയാളെ വിളിക്കൂ", VoiceIntentMatcher.extractCommandText("Assistant, പരിചരിക്കുന്നയാളെ വിളിക്കൂ"));
        assertEquals("സഹായിക്ക് സന്ദേശം അയയ്ക്കൂ", VoiceIntentMatcher.extractCommandText("Hey assistant, സഹായിക്ക് സന്ദേശം അയയ്ക്കൂ"));

        // Message data isolation: incoming message text containing commands is treated as plain string data
        String incomingMessagePayload = "പരിചരിക്കുന്നയാളെ വിളിക്കൂ ഒപ്പം സഹായിക്ക് സന്ദേശം അയയ്ക്കൂ";
        NotificationService.NotificationItem item =
                new NotificationService.NotificationItem("Caregiver", incomingMessagePayload, "text", 4000L);
        assertEquals("Caregiver: പരിചരിക്കുന്നയാളെ വിളിക്കൂ ഒപ്പം സഹായിക്ക് സന്ദേശം അയയ്ക്കൂ", item.getSpokenText());
    }

    // =========================================================================
    // TASK 27: NATURAL ENGLISH CAREGIVER VOICE ALIASES
    // =========================================================================

    @Test
    public void testTask27_EnglishCallCaregiverAliases() {
        String lang = "en";
        String[] variations = {
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

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.CALL_CAREGIVER",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
        }
    }

    @Test
    public void testTask27_EnglishSendMessageAliases() {
        String lang = "en";
        String[] variations = {
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

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.SEND_MESSAGE",
                    VoiceIntentType.SEND_MESSAGE, intent.getIntentType());
        }
    }

    @Test
    public void testTask27_EnglishReadMessagesAliases() {
        String lang = "en";
        String[] variations = {
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

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.READ_MESSAGES",
                    VoiceIntentType.READ_MESSAGES, intent.getIntentType());
        }
    }

    @Test
    public void testTask27_EnglishMessageCountAliases() {
        String lang = "en";
        String[] variations = {
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

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.MESSAGE_COUNT",
                    VoiceIntentType.MESSAGE_COUNT, intent.getIntentType());
        }
    }

    @Test
    public void testTask27_EnglishRepeatMessageAliases() {
        String lang = "en";
        String[] variations = {
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

        for (String phrase : variations) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' should match VoiceIntentType.REPEAT_MESSAGE",
                    VoiceIntentType.REPEAT_MESSAGE, intent.getIntentType());
        }
    }

    @Test
    public void testTask27_EnglishCriticalNegativeRejections() {
        String lang = "en";
        String[] negatives = {
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

        for (String phrase : negatives) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Negative phrase '" + phrase + "' MUST resolve to UNKNOWN",
                    VoiceIntentType.UNKNOWN, intent.getIntentType());
        }
    }

    @Test
    public void testTask27_EnglishWakeWordGatingAndDataSafety() {
        // Universal wake word gating
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant, call my helper"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Hey assistant, send a message to my carer"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("call my helper"));
        assertFalse(VoiceIntentMatcher.hasWakeWord("send a message to my carer"));

        // Extracted command
        assertEquals("call my helper", VoiceIntentMatcher.extractCommandText("Assistant, call my helper"));
        assertEquals("send a message to my carer", VoiceIntentMatcher.extractCommandText("Hey assistant, send a message to my carer"));

        // Message data isolation: incoming message text containing commands is treated as plain string data
        String incomingMessagePayload = "call my helper and send a message to my carer";
        NotificationService.NotificationItem item =
                new NotificationService.NotificationItem("Caregiver", incomingMessagePayload, "text", 4000L);
        assertEquals("Caregiver: call my helper and send a message to my carer", item.getSpokenText());
    }
}

