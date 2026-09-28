package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Dedicated unit test suite for Blind User Voice Assistant Kannada Caregiver Command Recognition.
 *
 * Verifies:
 * A. Natural, easy Kannada caregiver call aliases (ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ, ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ, etc.) -> CALL_CAREGIVER
 * B. Natural, easy Kannada caregiver messaging aliases (ನನ್ನ ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ, ಸಹಾಯಕನಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ, etc.) -> SEND_MESSAGE
 * C. Strict intent separation: emergency/help requests (ನನಗೆ ಸಹಾಯ ಬೇಕು, ಸಹಾಯ ಮಾಡಿ, ತುರ್ತು ಸಹಾಯ ಬೇಕು) MUST NOT trigger CALL_CAREGIVER
 * D. Isolated negative tokens and random speech do NOT trigger CALL_CAREGIVER
 * E. Existing English, Hindi, and Malayalam commands are preserved 100%
 */
public class KannadaCaregiverVoiceCommandTest {

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
    public void testKannadaCallCaregiverAliases() {
        String lang = "kn";

        // Required variants from Task Specification
        String[] callCommands = {
                "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ",
                "ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ",
                "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ಸಹಾಯಕನಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ನನ್ನ ಸಹಾಯಕರಿಗೆ ಕರೆ ಮಾಡಿ",
                "ಸಹಾಯಕರಿಗೆ ಕರೆ ಮಾಡಿ",
                "ನನ್ನ ಸಹಾಯಕರಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ಸಹಾಯಕರಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಫೋನ್ ಮಾಡಿ",
                "ಸಹಾಯಕನಿಗೆ ಫೋನ್ ಮಾಡಿ",
                "ನನ್ನ ಸಹಾಯಕರಿಗೆ ಫೋನ್ ಮಾಡಿ",
                "ಸಹಾಯಕರಿಗೆ ಫೋನ್ ಮಾಡಿ",
                "Assistant, ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ",
                "Assistant, ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ",
                "Assistant, ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "Assistant, ಸಹಾಯಕನಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "Assistant, ನನ್ನ ಸಹಾಯಕರಿಗೆ ಕರೆ ಮಾಡಿ"
        };

        for (String phrase : callCommands) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match VoiceIntentType.CALL_CAREGIVER",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
            assertEquals("Phrase '" + phrase + "' must map to CMD_OPEN_VOICE_CALL intent name",
                    VoiceCommandConstants.INTENT_OPEN_VOICE_CALL, intent.getIntentName());
        }
    }

    @Test
    public void testExistingKannadaCallCommandsPreserved() {
        String lang = "kn";

        String[] preservedCallCommands = {
                "ಕೇರ್‌ಗಿವರ್‌ಗೆ ಕರೆ ಮಾಡಿ",
                "ಕೇರ್ಗಿವರ್ಗೆ ಕರೆ ಮಾಡಿ",
                "ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ",
                "ಆರೈಕೆದಾರನಿಗೆ ಕರೆ ಮಾಡಿ",
                "ಧ್ವನಿ ಕರೆ",
                "Assistant, ಕೇರ್ಗಿವರ್ಗೆ ಕರೆ ಮಾಡಿ",
                "Assistant, ಕೇರ್‌ಗಿವರ್‌ಗೆ ಕರೆ ಮಾಡಿ"
        };

        for (String phrase : preservedCallCommands) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Preserved phrase '" + phrase + "' must match VoiceIntentType.CALL_CAREGIVER",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
        }
    }

    @Test
    public void testKannadaSendMessageAliases() {
        String lang = "kn";

        // Required variants from Task Specification
        String[] msgCommands = {
                "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ಸಹಾಯಕನಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ನನ್ನ ಸಹಾಯಕರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಸಹಾಯಕರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ನನ್ನ ಸಹಾಯಕರಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ಸಹಾಯಕರಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ",
                "ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ",
                "ನನ್ನ ಸಹಾಯಕರಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ",
                "ಸಹಾಯಕರಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ",
                "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಮೆಸೇಜ್ ಕಳಿಸಿ",
                "ಸಹಾಯಕನಿಗೆ ಮೆಸೇಜ್ ಕಳಿಸಿ",
                "Assistant, ನನ್ನ ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "Assistant, ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "Assistant, ನನ್ನ ಸಹಾಯಕನಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "Assistant, ಸಹಾಯಕನಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ"
        };

        for (String phrase : msgCommands) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match VoiceIntentType.SEND_MESSAGE",
                    VoiceIntentType.SEND_MESSAGE, intent.getIntentType());
            assertEquals("Phrase '" + phrase + "' must map to CMD_SEND_MESSAGE intent name",
                    VoiceCommandConstants.INTENT_SEND_MESSAGE, intent.getIntentName());
            assertNotEquals("Phrase '" + phrase + "' must NOT be CALL_CAREGIVER",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
        }
    }

    @Test
    public void testExistingKannadaSendMessageCommandsPreserved() {
        String lang = "kn";

        String[] preservedMsgCommands = {
                "ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ಕೇರ್ಗಿವರ್ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಕೇರ್‌ಗಿವರ್‌ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಆರೈಕೆದಾರರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಆರೈಕೆದಾರನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ"
        };

        for (String phrase : preservedMsgCommands) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Preserved phrase '" + phrase + "' must match VoiceIntentType.SEND_MESSAGE",
                    VoiceIntentType.SEND_MESSAGE, intent.getIntentType());
        }
    }

    @Test
    public void testEmergencyIntentSeparation() {
        String lang = "kn";

        // Negative tests: These phrases must NEVER trigger CALL_CAREGIVER
        String[] emergencyPhrases = {
                "ನನಗೆ ಸಹಾಯ ಬೇಕು",
                "ಸಹಾಯ ಮಾಡಿ",
                "ತುರ್ತು ಸಹಾಯ ಬೇಕು",
                "ತುರ್ತು ಸಹಾಯ",
                "ತುರ್ತು ಎಚ್ಚರಿಕೆ",
                "ಕಾಪಾಡಿ"
        };

        for (String phrase : emergencyPhrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertNotEquals("Emergency phrase '" + phrase + "' MUST NOT trigger CALL_CAREGIVER",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
            assertNotEquals("Emergency phrase '" + phrase + "' MUST NOT trigger SEND_MESSAGE",
                    VoiceIntentType.SEND_MESSAGE, intent.getIntentType());
            assertTrue("Emergency phrase '" + phrase + "' must match emergency intent",
                    intent.getIntentType() == VoiceIntentType.OPEN_EMERGENCY ||
                    intent.getIntentType() == VoiceIntentType.OPEN_EMERGENCY_ALERT);
        }
    }

    @Test
    public void testIsolatedTokensAndRandomSpeechNegativeTests() {
        String lang = "kn";

        // Generic isolated tokens must NOT trigger CALL_CAREGIVER
        String[] isolatedTokens = {
                "ಸಹಾಯ",
                "ಕರೆ",
                "ಕಾಲ್",
                "ಕರೆ ಮಾಡಿ",
                "ಕಾಲ್ ಮಾಡಿ",
                "ಸಂದೇಶ",
                "ಮೆಸೇಜ್",
                "ಸಹಾಯಕ",
                "ಆರೈಕೆದಾರ",
                "ಕೇರ್ಗಿವರ್",
                "ಕೇರ್‌ಗಿವರ್"
        };

        for (String phrase : isolatedTokens) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for token: " + phrase, intent);
            assertNotEquals("Isolated token '" + phrase + "' must NOT trigger CALL_CAREGIVER",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
            assertEquals("Isolated token '" + phrase + "' must be UNKNOWN",
                    VoiceIntentType.UNKNOWN, intent.getIntentType());
        }

        // Random Kannada speech must NOT trigger CALL_CAREGIVER
        String[] randomSpeech = {
                "ಇಂದು ಹವಾಮಾನ ಹೇಗಿದೆ",
                "ಊಟ ಆಯ್ತಾ",
                "ಬೆಂಗಳೂರಿನಲ್ಲಿ ಮಳೆ ಬರುತ್ತಿದೆ",
                "ನಮಸ್ಕಾರ ಶುಭೋದಯ",
                "ಸಮಯ ಎಷ್ಟು ಆಗಿದೆ"
        };

        for (String phrase : randomSpeech) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertNotEquals("Random speech '" + phrase + "' must NOT trigger CALL_CAREGIVER",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
            assertEquals("Random speech '" + phrase + "' must be UNKNOWN",
                    VoiceIntentType.UNKNOWN, intent.getIntentType());
        }
    }

    @Test
    public void testPreservedEnglishHindiMalayalamCommands() {
        // English
        VoiceIntent enCall = processor.detectIntent("Assistant, call my caregiver", "en");
        assertEquals(VoiceIntentType.CALL_CAREGIVER, enCall.getIntentType());

        VoiceIntent enMsg = processor.detectIntent("Assistant, send a message", "en");
        assertEquals(VoiceIntentType.SEND_MESSAGE, enMsg.getIntentType());

        // Hindi
        VoiceIntent hiCall = processor.detectIntent("सहायक को कॉल करो", "hi");
        assertEquals(VoiceIntentType.CALL_CAREGIVER, hiCall.getIntentType());

        VoiceIntent hiMsg = processor.detectIntent("सहायक को मैसेज भेजो", "hi");
        assertEquals(VoiceIntentType.SEND_MESSAGE, hiMsg.getIntentType());

        // Malayalam
        VoiceIntent mlCall = processor.detectIntent("സഹായിയെ വിളിക്കൂ", "ml");
        assertEquals(VoiceIntentType.CALL_CAREGIVER, mlCall.getIntentType());

        VoiceIntent mlMsg = processor.detectIntent("സഹായിക്ക് സന്ദേശം അയക്കൂ", "ml");
        assertEquals(VoiceIntentType.SEND_MESSAGE, mlMsg.getIntentType());
    }

    @Test
    public void testPalakaCaregiverEquivalence() {
        String lang = "kn";

        // Caregiver calls with ಪಾಲಕ and ಸಹಾಯಕ equivalence
        String[] palakaCalls = {
                "ಪಾಲಕರಿಗೆ ಕರೆ ಮಾಡಿ",
                "ನನ್ನ ಪಾಲಕರಿಗೆ ಕರೆ ಮಾಡಿ",
                "ಪಾಲಕರಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ನನ್ನ ಪಾಲಕರಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ಪಾಲಕರಿಗೆ ಫೋನ್ ಮಾಡಿ",
                "ನನ್ನ ಪಾಲಕರಿಗೆ ಫೋನ್ ಮಾಡಿ",
                "ನನ್ನ ಪಾಲಕರನ್ನು ಕರೆ ಮಾಡಿ",
                "ಪಾಲಕರಿಗೆ ಕರೆ ಮಾಡು",
                "ಪಾಲಕರ ಕರೆ",
                "Assistant, ಪಾಲಕರಿಗೆ ಕರೆ ಮಾಡಿ",
                "Assistant, ನನ್ನ ಪಾಲಕರಿಗೆ ಕರೆ ಮಾಡಿ"
        };
        for (String phrase : palakaCalls) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match CALL_CAREGIVER",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
            assertEquals(VoiceCommandConstants.INTENT_OPEN_VOICE_CALL, intent.getIntentName());
        }

        // Caregiver messages with ಪಾಲಕ and ಸಹಾಯಕ equivalence
        String[] palakaMsgs = {
                "ಪಾಲಕರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ನನ್ನ ಪಾಲಕರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಪಾಲಕರಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ",
                "ಪಾಲಕರಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ಪಾಲಕರಿಗೆ ಮೆಸೇಜ್ ಕಳಿಸಿ",
                "Assistant, ಪಾಲಕರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "Assistant, ನನ್ನ ಪಾಲಕರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ"
        };
        for (String phrase : palakaMsgs) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match SEND_MESSAGE",
                    VoiceIntentType.SEND_MESSAGE, intent.getIntentType());
            assertEquals(VoiceCommandConstants.INTENT_SEND_MESSAGE, intent.getIntentName());
        }
    }

    @Test
    public void testKannadaEndCallVariations() {
        String lang = "kn";
        String[] phrases = {
                "ಕರೆಯನ್ನು ಮುಗಿಸಿ",
                "ಕರೆ ಮುಗಿಸಿ",
                "ಕರೆ ಮುಗಿಸು",
                "ಕರೆಯನ್ನು ಮುಗಿಸು",
                "ಕರೆಯನ್ನು ನಿಲ್ಲಿಸಿ",
                "ಕರೆ ನಿಲ್ಲಿಸಿ",
                "ಕಾಲ್ ಮುಗಿಸಿ",
                "ಕಾಲ್ ಕಟ್ ಮಾಡಿ",
                "ಫೋನ್ ಕಟ್ ಮಾಡಿ",
                "ಫೋನ್ ಮುಗಿಸಿ",
                "ಈ ಕರೆಯನ್ನು ಮುಗಿಸಿ",
                "ಈ ಕರೆ ನಿಲ್ಲಿಸಿ",
                "Assistant, ಕರೆಯನ್ನು ಮುಗಿಸಿ"
        };
        for (String phrase : phrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match END_CALL",
                    VoiceIntentType.END_CALL, intent.getIntentType());
        }
    }

    @Test
    public void testKannadaRepeatMessageVariations() {
        String lang = "kn";
        String[] phrases = {
                "ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಸಂದೇಶವನ್ನು ಪುನರಾವರ್ತಿಸಿ",
                "ಪಾಲಕರ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಪಾಲಕರ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಪಾಲಕರ ಮೆಸೇಜ್ ಮತ್ತೆ ಹೇಳಿ",
                "ಸಹಾಯಕರ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಸಹಾಯಕನ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಮತ್ತೆ ಓದಿ",
                "ಮತ್ತೊಮ್ಮೆ ಓದಿ",
                "ಸಂದೇಶವನ್ನು ಮತ್ತೊಮ್ಮೆ ಓದಿ",
                "ಇದನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಇನ್ನೊಮ್ಮೆ ಹೇಳಿ",
                "ಮತ್ತೊಮ್ಮೆ ಕೇಳಿಸಿ",
                "ಮತ್ತೆ ಕೇಳಿಸಿ"
        };
        for (String phrase : phrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match REPEAT_MESSAGE",
                    VoiceIntentType.REPEAT_MESSAGE, intent.getIntentType());
        }
    }

    @Test
    public void testKannadaMessageCountVariations() {
        String lang = "kn";
        String[] phrases = {
                "ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಎಷ್ಟು ಸಂದೇಶ ಇದೆ",
                "ಎಷ್ಟು ಸಂದೇಶಗಳು ಬಂದಿವೆ",
                "ಎಷ್ಟು ಸಂದೇಶ ಬಂದಿದೆ",
                "ನನಗೆ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ನನ್ನ ಸಂದೇಶಗಳು ಎಷ್ಟಿವೆ",
                "ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ",
                "ಪಾಲಕರಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಪಾಲಕರ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಸಹಾಯಕರಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಸಹಾಯಕರ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಪಾಲಕರಿಂದ ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ"
        };
        for (String phrase : phrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match MESSAGE_COUNT",
                    VoiceIntentType.MESSAGE_COUNT, intent.getIntentType());
        }
    }

    @Test
    public void testKannadaReadNotificationsVariations() {
        String lang = "kn";
        String[] phrases = {
                "ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ",
                "ನನ್ನ ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ",
                "ಅಧಿಸೂಚನೆಗಳನ್ನು ಪರಿಶೀಲಿಸಿ",
                "ಅಧಿಸೂಚನೆಗಳನ್ನು ಹೇಳಿ",
                "ಅಧಿಸೂಚನೆ ಹೇಳಿ",
                "ನೋಟಿಫಿಕೇಶನ್ಗಳನ್ನು ಓದಿ",
                "ನೋಟಿಫಿಕೇಶನ್ಗಳನ್ನು ಹೇಳಿ",
                "ನೋಟಿಫಿಕೇಶನ್ಗಳನ್ನು ಪರಿಶೀಲಿಸಿ",
                "ನೋಟಿಫಿಕೇಶನ್ ಓದಿ",
                "ನೋಟಿಫಿಕೇಶನ್ ಹೇಳಿ",
                "ನನ್ನ ನೋಟಿಫಿಕೇಶನ್ಗಳನ್ನು ಓದಿ"
        };
        for (String phrase : phrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match READ_NOTIFICATIONS",
                    VoiceIntentType.READ_NOTIFICATIONS, intent.getIntentType());
        }
    }

    @Test
    public void testKannadaReadMessagesVariations() {
        String lang = "kn";
        String[] phrases = {
                "ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ಸಂದೇಶ ಓದಿ",
                "ನನ್ನ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ಸಂದೇಶಗಳನ್ನು ಹೇಳಿ",
                "ಸಂದೇಶ ಹೇಳಿ",
                "ಸಂದೇಶಗಳನ್ನು ಕೇಳಿಸಿ",
                "ಸಂದೇಶ ಕೇಳಿಸಿ",
                "ಸಂದೇಶಗಳನ್ನು ಓದು",
                "ಸಂದೇಶ ಓದು",
                "ಮೆಸೇಜ್ಗಳನ್ನು ಓದಿ",
                "ಮೆಸೇಜ್ ಓದಿ",
                "ಮೆಸೇಜ್ ಹೇಳಿ",
                "ಮೆಸೇಜ್ ಕೇಳಿಸಿ",
                "ಪಾಲಕರ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ಪಾಲಕರ ಸಂದೇಶ ಓದಿ",
                "ಪಾಲಕರ ಮೆಸೇಜ್ ಓದಿ",
                "ಪಾಲಕರ ಸಂದೇಶಗಳನ್ನು ಹೇಳಿ",
                "ಪಾಲಕರ ಸಂದೇಶಗಳನ್ನು ಕೇಳಿಸಿ",
                "ಸಹಾಯಕರ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ಸಹಾಯಕನ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ಸಹಾಯಕರ ಸಂದೇಶಗಳನ್ನು ಹೇಳಿ",
                "ಸಹಾಯಕರ ಸಂದೇಶಗಳನ್ನು ಕೇಳಿಸಿ",
                "ನನ್ನ ಪಾಲಕರ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ನನ್ನ ಸಹಾಯಕರ ಸಂದೇಶಗಳನ್ನು ಓದಿ"
        };
        for (String phrase : phrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match READ_MESSAGES",
                    VoiceIntentType.READ_MESSAGES, intent.getIntentType());
        }
    }

    @Test
    public void testKannadaOpenMessagesVariations() {
        String lang = "kn";
        String[] phrases = {
                "ಸಂದೇಶಗಳನ್ನು ತೆರೆಯಿರಿ",
                "ಸಂದೇಶ ತೆರೆಯಿರಿ",
                "ಸಂದೇಶಗಳನ್ನು ತೋರಿಸಿ",
                "ಸಂದೇಶ ತೋರಿಸಿ",
                "ಮೆಸೇಜ್ಗಳನ್ನು ತೋರಿಸಿ",
                "ಮೆಸೇಜ್ ತೋರಿಸಿ",
                "ಮೆಸೇಜ್ಗಳನ್ನು ತೆರೆಯಿರಿ",
                "ಮೆಸೇಜ್ ತೆರೆಯಿರಿ"
        };
        for (String phrase : phrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match OPEN_MESSAGES",
                    VoiceIntentType.OPEN_MESSAGES, intent.getIntentType());
        }
    }

    @Test
    public void testKannadaGoBackVariations() {
        String lang = "kn";
        String[] phrases = {
                "ಹಿಂದಕ್ಕೆ ಹೋಗಿ",
                "ಹಿಂದೆ ಹೋಗಿ",
                "ಹಿಂದಕ್ಕೆ",
                "ಹಿಂದಿನ ಪುಟಕ್ಕೆ ಹೋಗಿ",
                "ಹಿಂದಿನ ಪುಟಕ್ಕೆ ಹಿಂತಿರುಗಿ",
                "ಹಿಂದೆ ಬನ್ನಿ",
                "ವಾಪಸ್ ಬನ್ನಿ",
                "go back",
                "ಹಿಂದೆ ಹೋಗು",
                "ಹಿಂದಕ್ಕೆ ಹೋಗು",
                "ಹಿಂದಕ್ಕೆ ಬಾ",
                "ಮರಳಿ ಬಾ",
                "ಹಿಂದಿನ ಪುಟಕ್ಕೆ ಹೋಗು"
        };
        for (String phrase : phrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match GO_BACK",
                    VoiceIntentType.GO_BACK, intent.getIntentType());
        }
    }

    @Test
    public void testUserRequestedBottomNavImage2Phrases() {
        // 1. Home page / ಮುಖ್ಯ ಪುಟ
        String[] homePhrases = {
                "Home page",
                "home page",
                "ಮುಖ್ಯ ಪುಟ",
                "ಮುಖ್ಯ ಪುಟಕ್ಕೆ ಹೋಗು"
        };
        for (String phrase : homePhrases) {
            VoiceIntent intent = processor.detectIntent(phrase, "kn");
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match OPEN_HOME",
                    VoiceIntentType.OPEN_HOME, intent.getIntentType());
        }

        // 2. My profile page / ಪ್ರೊಫೈಲ್
        String[] profilePhrases = {
                "my profile page",
                "profile page",
                "ಪ್ರೊಫೈಲ್",
                "ನನ್ನ ಪ್ರೊಫೈಲ್"
        };
        for (String phrase : profilePhrases) {
            VoiceIntent intent = processor.detectIntent(phrase, "kn");
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match OPEN_PROFILE",
                    VoiceIntentType.OPEN_PROFILE, intent.getIntentType());
        }

        // 3. Caregiver connection page / ಸಹಾಯಕರ ಸಂಪರ್ಕ
        String[] connPhrases = {
                "caregiver connection page",
                "caregiver conection page",
                "caregiver page",
                "helpers page",
                "ಸಹಾಯಕರ ಪುಟ",
                "ಸಹಾಯಕರ ಸಂಪರ್ಕ",
                "ಸಹಾಯಕರ ಸಂಪರ್ಕ ಪುಟ"
        };
        for (String phrase : connPhrases) {
            VoiceIntent intent = processor.detectIntent(phrase, "kn");
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match OPEN_CAREGIVER_CONNECTION",
                    VoiceIntentType.OPEN_CAREGIVER_CONNECTION, intent.getIntentType());
        }
    }

    @Test
    public void testKannadaSettingsProfileHomeVariations() {
        String lang = "kn";

        // Settings
        String[] settingsPhrases = {
                "ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಿರಿ",
                "ಸೆಟ್ಟಿಂಗ್ ತೆರೆಯಿರಿ",
                "ಸೆಟ್ಟಿಂಗ್ಸ್ಗೆ ಹೋಗಿ",
                "ಸೆಟ್ಟಿಂಗ್ಸ್ ತೋರಿಸಿ",
                "ಸೆಟ್ಟಿಂಗ್ ತೋರಿಸಿ",
                "ಧ್ವನಿ ಸೆಟ್ಟಿಂಗ್"
        };
        for (String phrase : settingsPhrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match OPEN_SETTINGS",
                    VoiceIntentType.OPEN_SETTINGS, intent.getIntentType());
        }

        // Profile
        String[] profilePhrases = {
                "ಪ್ರೊಫೈಲ್ ತೆರೆಯಿರಿ",
                "ಪ್ರೊಫೈಲ್ ತೋರಿಸಿ",
                "ನನ್ನ ಪ್ರೊಫೈಲ್ ತೆರೆಯಿರಿ",
                "ನನ್ನ ಪ್ರೊಫೈಲ್ ತೋರಿಸಿ",
                "ವೈಯಕ್ತಿಕ ಮಾಹಿತಿ"
        };
        for (String phrase : profilePhrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match OPEN_PROFILE",
                    VoiceIntentType.OPEN_PROFILE, intent.getIntentType());
        }

        // Home
        String[] homePhrases = {
                "ಮುಖಪುಟಕ್ಕೆ ಹೋಗಿ",
                "ಮುಖ್ಯ ಪುಟಕ್ಕೆ ಹೋಗಿ",
                "ಮುಖಪುಟ ತೆರೆಯಿರಿ",
                "ಮುಖಪುಟ ತೋರಿಸಿ",
                "ಮನೆಗೆ ಹೋಗಿ"
        };
        for (String phrase : homePhrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match OPEN_HOME",
                    VoiceIntentType.OPEN_HOME, intent.getIntentType());
        }
    }

    @Test
    public void testExpandedNegativeTokens() {
        String lang = "kn";
        String[] negatives = {
                "ಪಾಲಕ",
                "ಪಾಲಕರು",
                "ಸಹಾಯಕ",
                "ಸಹಾಯಕರು",
                "ಸಹಾಯ",
                "ಕರೆ",
                "ಕಾಲ್",
                "ಫೋನ್",
                "ಸಂದೇಶ",
                "ಮೆಸೇಜ್",
                "ಅಧಿಸೂಚನೆ",
                "ನೋಟಿಫಿಕೇಶನ್"
        };
        for (String phrase : negatives) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for token: " + phrase, intent);
            assertEquals("Token '" + phrase + "' must be UNKNOWN",
                    VoiceIntentType.UNKNOWN, intent.getIntentType());
        }
    }

    @Test
    public void testGoBackCommandAllUserPhrases() {
        String lang = "kn";

        String[] goBackPhrases = {
                // English
                "go back",
                "navigate back",
                "previous page",
                "previous screen",
                // Kannada
                "ಹಿಂದೆ ಹೋಗು",
                "ಹಿಂದಕ್ಕೆ ಹೋಗು",
                "ಹಿಂದಕ್ಕೆ ಬಾ",
                "ಹಿಂದೆ ಬಾ",
                "ಮರಳಿ ಬಾ",
                "ಮರಳಿ ಬನ್ನಿ",
                "ಮರಳಿ ಹೋಗು",
                "ಮರಳಿ ಹೋಗಿ",
                "ಹಿಂದಿನ ಪುಟಕ್ಕೆ ಹೋಗು",
                "ಹಿಂದಿನ ಪುಟಕ್ಕೆ ಹೋಗಿ",
                "ಹಿಂದಿನ ಪುಟಕ್ಕೆ ಬಾ",
                "ಹಿಂದಿನ ಪುಟಕ್ಕೆ ಬನ್ನಿ",
                "ವಾಪಸ್ ಬಾ",
                "ವಾಪಸ್ ಹೋಗು",
                "ವಾಪಸ್ ಹೋಗಿ",
                "ವಾಪಸ್ ಬನ್ನಿ",
                "ಗೋ ಬ್ಯಾಕ್",
                "ಬ್ಯಾಕ್ ಹೋಗು",
                // With English and Kannada Wake Word Prefixes
                "hey assistant go back",
                "Assistant, go back",
                "ಹೇ ಅಸಿಸ್ಟೆಂಟ್ ಹಿಂದೆ ಹೋಗು",
                "ಅಸಿಸ್ಟೆಂಟ್ ಹಿಂದೆ ಹೋಗು",
                "ಹೇ ಅಸಿಸ್ಟೆಂಟ್ ಹಿಂದಕ್ಕೆ ಹೋಗು",
                "ಹೇ ಅಸಿಸ್ಟೆಂಟ್ ಹಿಂದಕ್ಕೆ ಬಾ",
                "ಹೇ ಅಸಿಸ್ಟೆಂಟ್ ಮರಳಿ ಬಾ",
                "ಹೇ ಅಸಿಸ್ಟೆಂಟ್ ಹಿಂದಿನ ಪುಟಕ್ಕೆ ಹೋಗು",
                "ಹೇ ಅಸಿಸ್ಟೆಂಟ್ ವಾಪಸ್ ಬಾ",
                "ಹೇ ಅಸಿಸ್ಟೆಂಟ್ ವಾಪಸ್ ಹೋಗು"
        };

        for (String phrase : goBackPhrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertNotNull("Intent should not be null for phrase: " + phrase, intent);
            assertEquals("Phrase '" + phrase + "' must match GO_BACK",
                    VoiceIntentType.GO_BACK, intent.getIntentType());
        }
    }

    @Test
    public void testMultilingualWakeWordDetectionAndExtraction() {
        // English wake words
        assertTrue(VoiceIntentMatcher.hasWakeWord("assistant"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("hey assistant"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("hey assistant go back"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("assistant open home"));

        // Kannada wake words
        assertTrue(VoiceIntentMatcher.hasWakeWord("ಅಸಿಸ್ಟೆಂಟ್"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("ಹೇ ಅಸಿಸ್ಟೆಂಟ್"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("ಹೇ ಅಸಿಸ್ಟೆಂಟ್ ಹಿಂದೆ ಹೋಗು"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("ಅಸಿಸ್ಟೆಂಟ್ ಹಿಂದೆ ಹೋಗು"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("ಹೇ ಸಹಾಯಕ"));

        // Wake word extraction
        assertEquals("go back", VoiceIntentMatcher.extractCommandText("hey assistant go back"));
        assertEquals("go back", VoiceIntentMatcher.extractCommandText("assistant go back"));
        assertEquals("ಹಿಂದೆ ಹೋಗು", VoiceIntentMatcher.extractCommandText("ಹೇ ಅಸಿಸ್ಟೆಂಟ್ ಹಿಂದೆ ಹೋಗು"));
        assertEquals("ಹಿಂದೆ ಹೋಗು", VoiceIntentMatcher.extractCommandText("ಅಸಿಸ್ಟೆಂಟ್ ಹಿಂದೆ ಹೋಗು"));
        assertEquals("ಹಿಂದೆ ಹೋಗು", VoiceIntentMatcher.extractCommandText("ಹೇ ಸಹಾಯಕ ಹಿಂದೆ ಹೋಗು"));
        assertEquals("", VoiceIntentMatcher.extractCommandText("hey assistant"));
        assertEquals("", VoiceIntentMatcher.extractCommandText("assistant"));
        assertEquals("", VoiceIntentMatcher.extractCommandText("ಹೇ ಅಸಿಸ್ಟೆಂಟ್"));
        assertEquals("", VoiceIntentMatcher.extractCommandText("ಅಸಿಸ್ಟೆಂಟ್"));

        // Vosk candidate parsing accepts wake words (and returns null for non-wake utterances)
        assertEquals("assistant go back", WakeWordManager.parseAndExtractCandidate("{\"text\":\"assistant go back\"}"));
        assertEquals("assistant", WakeWordManager.parseAndExtractCandidate("{\"text\":\"assistant\"}"));
        assertNull(WakeWordManager.parseAndExtractCandidate("{\"text\":\"open home\"}"));
    }
}
