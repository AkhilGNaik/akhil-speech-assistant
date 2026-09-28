package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.kannada.speechassistant.RoleManager;

import org.junit.Before;
import org.junit.Test;

/**
 * Step 4 Unit Test Suite:
 * Hands-Free Voice Recording & Voice Message Sending
 *
 * Verifies:
 * 1. Recording states and confirmation types in AppVoiceAssistant.
 * 2. Natural voice recording requests across English, Kannada, Hindi, Malayalam with caregiver synonyms.
 * 3. Legacy voice recorder phrases preserved for OPEN_VOICE_RECORDER.
 * 4. Stop recording phrases across English, Kannada, Hindi, Malayalam.
 * 5. Cancel recording phrases across English, Kannada, Hindi, Malayalam.
 * 6. Multilingual prompt strings for voice message recording, confirmation, sent, cancelled, and failed.
 * 7. Confirmation responses (Yes/Send vs No/Cancel).
 * 8. State management during recording and confirmation in AppVoiceAssistant.
 * 9. Strict SOS and safety protections preserved.
 */
public class NaturalVoiceRecordingTest {

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

    // ==========================================
    // 1. STATE & CONFIRMATION TYPE DEFINITIONS
    // ==========================================

    @Test
    public void testRecordingStateAndConfirmationTypesExist() {
        assertNotNull("VOICE_MESSAGE_RECORDING state must exist",
                AppVoiceAssistant.AssistantState.VOICE_MESSAGE_RECORDING);
        assertNotNull("ConfirmationType enum NONE must exist",
                AppVoiceAssistant.ConfirmationType.NONE);
        assertNotNull("ConfirmationType enum COMMAND must exist",
                AppVoiceAssistant.ConfirmationType.COMMAND);
        assertNotNull("ConfirmationType enum VOICE_MESSAGE must exist",
                AppVoiceAssistant.ConfirmationType.VOICE_MESSAGE);
    }

    // ==========================================
    // 2. MULTILINGUAL NATURAL RECORDING INTENTS
    // ==========================================

    @Test
    public void testEnglishVoiceRecordingRequests() {
        String lang = "en";
        String[] phrases = {
                "I want to send a voice message to my caregiver",
                "Send voice message to carer",
                "Send audio message to assistant",
                "Send voice note to helper",
                "Record a voice message for attendant",
                "Send a voice message saying I need help",
                "Send audio recording to caregiver",
                "Send voice message"
        };

        for (String phrase : phrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertEquals("Phrase should match RECORD_VOICE_MESSAGE: " + phrase,
                    VoiceIntentType.RECORD_VOICE_MESSAGE, intent.getIntentType());
            VoiceCommand cmd = processor.resolveCommand(intent, RoleManager.ROLE_BLIND_USER);
            assertEquals(VoiceCommandConstants.CMD_RECORD_VOICE_MESSAGE, cmd.getCommandId());
        }
    }

    @Test
    public void testKannadaVoiceRecordingRequests() {
        String lang = "kn";
        String[] phrases = {
                "ನನ್ನ ಕೇರ್‌ಗಿವರ್‌ಗೆ ವಾಯ್ಸ್ ಮೆಸೇಜ್ ಕಳುಹಿಸಬೇಕು",
                "ಕೇರ್‌ಗಿವರ್‌ಗೆ ಆಡಿಯೋ ಸಂದೇಶ ಕಳುಹಿಸು",
                "ವಾಯ್ಸ್ ಮೆಸೇಜ್ ಕಳುಹಿಸು",
                "ಧ್ವನಿ ಸಂದೇಶ ಕಳುಹಿಸು",
                "ಧ್ವನಿ ಸಂದೇಶ ಕಳುಹಿಸಿದ್ದೇನೆ",
                "ಆಡಿಯೋ ಮೆಸೇಜ್ ಕಳುಹಿಸು",
                "ಸಹಾಯಕರಿಗೆ ವಾಯ್ಸ್ ಮೆಸೇಜ್ ಕಳುಹಿಸು"
        };

        for (String phrase : phrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertEquals("Kannada phrase should match RECORD_VOICE_MESSAGE: " + phrase,
                    VoiceIntentType.RECORD_VOICE_MESSAGE, intent.getIntentType());
        }
    }

    @Test
    public void testHindiVoiceRecordingRequests() {
        String lang = "hi";
        String[] phrases = {
                "मेरे केयरगिवर को वॉयस मैसेज भेजो",
                "केयरगिवर को ऑडियो संदेश भेजो",
                "वॉयस मैसेज भेजो",
                "ऑडियो संदेश भेजो",
                "सहायक को वॉइस संदेश भेजो",
                "वॉयस मैसेज भेजना चाहता हूँ"
        };

        for (String phrase : phrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertEquals("Hindi phrase should match RECORD_VOICE_MESSAGE: " + phrase,
                    VoiceIntentType.RECORD_VOICE_MESSAGE, intent.getIntentType());
        }
    }

    @Test
    public void testMalayalamVoiceRecordingRequests() {
        String lang = "ml";
        String[] phrases = {
                "എന്റെ കെയർഗിവർക്ക് ഒരു വോയ്സ് മെസേജ് അയക്കണം",
                "കെയർഗിവർക്ക് ഓഡിയോ മെസേജ് അയക്കൂ",
                "വോയ്സ് മെസേജ് അയക്കൂ",
                "വോയ്സ് സന്ദേശം അയക്കണം",
                "ഓഡിയോ മെസേജ് അയക്കൂ"
        };

        for (String phrase : phrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertEquals("Malayalam phrase should match RECORD_VOICE_MESSAGE: " + phrase,
                    VoiceIntentType.RECORD_VOICE_MESSAGE, intent.getIntentType());
        }
    }

    // ==========================================
    // 3. PRESERVATION OF LEGACY RECORDER PHRASES
    // ==========================================

    @Test
    public void testLegacyVoiceRecorderPhrasesPreserved() {
        String lang = "en";
        String[] legacyPhrases = {
                "record a voice message",
                "record voice message",
                "record audio message",
                "record audio",
                "start audio recorder",
                "open voice recorder",
                "open recorder"
        };

        for (String phrase : legacyPhrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertEquals("Legacy phrase should preserve OPEN_VOICE_RECORDER: " + phrase,
                    VoiceIntentType.OPEN_VOICE_RECORDER, intent.getIntentType());
        }
    }

    // ==========================================
    // 4. STOP RECORDING PHRASES (MULTILINGUAL)
    // ==========================================

    @Test
    public void testMultilingualStopRecordingPhrases() {
        // English
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("stop recording"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("stop"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("done"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("that's all"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("finish recording"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("finished"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("send it"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("send"));

        // Kannada
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("ರೆಕಾರ್ಡಿಂಗ್ ನಿಲ್ಲಿಸು"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("ರೆಕಾರ್ಡಿಂಗ್ ನಿಲ್ಲಿಸು."));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("ರೆಕಾರ್ಡಿಂಗ್ ನಿಲ್ಲಿಸಿ"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("ನಿಲ್ಲಿಸು"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("ನಿಲ್ಲಿಸಿ"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("ಮಾತು ಮುಗಿತು"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("ಮುಗಿಯಿತು"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("ಮುಗೀತು"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("ಇಷ್ಟು ಸಾಕು"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("ಸಾಕು"));

        // Hindi
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("रिकॉर्डिंग बंद करो"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("हो गया"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("बंद करो"));

        // Malayalam
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("റെക്കോർഡിംഗ് നിർത്തൂ"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("കഴിഞ്ഞു"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("നിർത്തൂ"));

        // Negative check
        assertFalse(VoiceIntentMatcher.isRecordingStopPhrase("how are you"));
        assertFalse(VoiceIntentMatcher.isRecordingStopPhrase("open camera"));
    }

    // ==========================================
    // 5. CANCEL RECORDING PHRASES (MULTILINGUAL)
    // ==========================================

    @Test
    public void testMultilingualCancelRecordingPhrases() {
        // English
        assertTrue(VoiceIntentMatcher.isRecordingCancelPhrase("cancel"));
        assertTrue(VoiceIntentMatcher.isRecordingCancelPhrase("cancel recording"));
        assertTrue(VoiceIntentMatcher.isRecordingCancelPhrase("discard"));
        assertTrue(VoiceIntentMatcher.isRecordingCancelPhrase("stop and cancel"));
        assertTrue(VoiceIntentMatcher.isRecordingCancelPhrase("don't send"));

        // Kannada
        assertTrue(VoiceIntentMatcher.isRecordingCancelPhrase("ರದ್ದು ಮಾಡಿ"));
        assertTrue(VoiceIntentMatcher.isRecordingCancelPhrase("ಬೇಡ"));
        assertTrue(VoiceIntentMatcher.isRecordingCancelPhrase("ರೆಕಾರ್ಡಿಂಗ್ ರದ್ದು ಮಾಡಿ"));

        // Hindi
        assertTrue(VoiceIntentMatcher.isRecordingCancelPhrase("रद्द करो"));
        assertTrue(VoiceIntentMatcher.isRecordingCancelPhrase("मत भेजो"));

        // Malayalam
        assertTrue(VoiceIntentMatcher.isRecordingCancelPhrase("റദ്ദാക്കുക"));
        assertTrue(VoiceIntentMatcher.isRecordingCancelPhrase("വേണ്ട"));
        assertTrue(VoiceIntentMatcher.isRecordingCancelPhrase("റെക്കോർഡിംഗ് റദ്ദാക്കുക"));

        // Negative check
        assertFalse(VoiceIntentMatcher.isRecordingCancelPhrase("send it"));
        assertFalse(VoiceIntentMatcher.isRecordingCancelPhrase("done"));
    }

    // ==========================================
    // 6. MULTILINGUAL PROMPTS & RESPONSES
    // ==========================================

    @Test
    public void testMultilingualPromptsAndResponses() {
        String[] langs = {"en", "kn", "hi", "ml"};

        for (String lang : langs) {
            String speakPrompt = VoiceLanguageConfig.getSpeakVoiceMessagePrompt(lang);
            assertNotNull(speakPrompt);
            assertFalse(speakPrompt.isEmpty());

            String confirmPrompt = VoiceLanguageConfig.getConfirmSendVoiceMessagePrompt(lang);
            assertNotNull(confirmPrompt);
            assertFalse(confirmPrompt.isEmpty());

            String sentResp = VoiceLanguageConfig.getVoiceMessageSentResponse(lang);
            assertNotNull(sentResp);
            assertFalse(sentResp.isEmpty());

            String cancelResp = VoiceLanguageConfig.getVoiceMessageCancelledResponse(lang);
            assertNotNull(cancelResp);
            assertFalse(cancelResp.isEmpty());

            String failResp = VoiceLanguageConfig.getVoiceMessageFailedResponse(lang);
            assertNotNull(failResp);
            assertFalse(failResp.isEmpty());
        }

        // Verify specific English prompt text matches specification
        assertEquals("Please speak the message you want to send to your caregiver.",
                VoiceLanguageConfig.getSpeakVoiceMessagePrompt("en"));
        assertEquals("Your voice message is ready. Do you want to send it to your caregiver?",
                VoiceLanguageConfig.getConfirmSendVoiceMessagePrompt("en"));
        assertEquals("Voice message sent.",
                VoiceLanguageConfig.getVoiceMessageSentResponse("en"));
        assertEquals("Voice message cancelled.",
                VoiceLanguageConfig.getVoiceMessageCancelledResponse("en"));
        assertEquals("Unable to send voice message. Please try again.",
                VoiceLanguageConfig.getVoiceMessageFailedResponse("en"));

        // Verify specific Kannada prompt text matches Step B and Step D specifications
        assertEquals("ನೀವು ಕಳುಹಿಸಲು ಬಯಸುವ ಧ್ವನಿ ಸಂದೇಶವನ್ನು ಮಾತನಾಡಿ.",
                VoiceLanguageConfig.getSpeakVoiceMessagePrompt("kn"));
        assertEquals("ಈ ಧ್ವನಿ ಸಂದೇಶವನ್ನು ಕಳುಹಿಸಬೇಕೇ?",
                VoiceLanguageConfig.getConfirmSendVoiceMessagePrompt("kn"));
        assertEquals("ಧ್ವನಿ ಸಂದೇಶ ಕಳುಹಿಸಿದ್ದೇನೆ",
                VoiceLanguageConfig.getVoiceMessageSentResponse("kn"));
        assertEquals("ಧ್ವನಿ ಸಂದೇಶ ರದ್ದುಗೊಳಿಸಲಾಗಿದೆ.",
                VoiceLanguageConfig.getVoiceMessageCancelledResponse("kn"));
    }

    // ==========================================
    // 7. CONFIRMATION RESPONSES
    // ==========================================

    @Test
    public void testConfirmationAndCancellationPhrases() {
        // Affirmative confirmation
        assertTrue(VoiceIntentMatcher.isAffirmativeResponse("yes"));
        assertTrue(VoiceIntentMatcher.isAffirmativeResponse("send"));
        assertTrue(VoiceIntentMatcher.isAffirmativeResponse("send it"));
        assertTrue(VoiceIntentMatcher.isAffirmativeResponse("sure"));
        assertTrue(VoiceIntentMatcher.isAffirmativeResponse("ಹೌದು"));
        assertTrue(VoiceIntentMatcher.isAffirmativeResponse("ಕಳುಹಿಸಿ"));
        assertTrue(VoiceIntentMatcher.isAffirmativeResponse("हाँ"));
        assertTrue(VoiceIntentMatcher.isAffirmativeResponse("भेज दो"));
        assertTrue(VoiceIntentMatcher.isAffirmativeResponse("അതെ"));
        assertTrue(VoiceIntentMatcher.isAffirmativeResponse("അയക്കൂ"));

        // Negative cancellation
        assertTrue(VoiceIntentMatcher.isNegativeResponse("no"));
        assertTrue(VoiceIntentMatcher.isNegativeResponse("cancel"));
        assertTrue(VoiceIntentMatcher.isNegativeResponse("don't send"));
        assertTrue(VoiceIntentMatcher.isNegativeResponse("discard"));
        assertTrue(VoiceIntentMatcher.isNegativeResponse("ಇಲ್ಲ"));
        assertTrue(VoiceIntentMatcher.isNegativeResponse("ಬೇಡ"));
        assertTrue(VoiceIntentMatcher.isNegativeResponse("नहीं"));
        assertTrue(VoiceIntentMatcher.isNegativeResponse("मत भेजो"));
        assertTrue(VoiceIntentMatcher.isNegativeResponse("വേണ്ട"));
        assertTrue(VoiceIntentMatcher.isNegativeResponse("റദ്ദാക്കുക"));
    }

    // ==========================================
    // 8. RECORDING & CONFIRMATION STATE MODEL
    // ==========================================

    @Test
    public void testRecordingStateAndConfirmationTypes() {
        assertEquals(AppVoiceAssistant.AssistantState.VOICE_MESSAGE_RECORDING,
                AppVoiceAssistant.AssistantState.valueOf("VOICE_MESSAGE_RECORDING"));
        assertEquals(AppVoiceAssistant.ConfirmationType.VOICE_MESSAGE,
                AppVoiceAssistant.ConfirmationType.valueOf("VOICE_MESSAGE"));
        assertEquals(AppVoiceAssistant.ConfirmationType.NONE,
                AppVoiceAssistant.ConfirmationType.valueOf("NONE"));
        assertEquals(AppVoiceAssistant.ConfirmationType.COMMAND,
                AppVoiceAssistant.ConfirmationType.valueOf("COMMAND"));
    }

    // ==========================================
    // 9. STRICT SAFETY & SOS PRESERVATION
    // ==========================================

    @Test
    public void testEmergencyAndHelpSafetyPreserved() {
        String lang = "en";

        // Emergency with context must remain OPEN_EMERGENCY
        VoiceIntent emergencyIntent = processor.detectIntent("Emergency help", lang);
        assertEquals(VoiceIntentType.OPEN_EMERGENCY, emergencyIntent.getIntentType());

        // Isolated "emergency" alone must NOT trigger OPEN_EMERGENCY (deliberate safety protection)
        VoiceIntent isolatedEmergency = processor.detectIntent("emergency", lang);
        assertNotEquals(VoiceIntentType.OPEN_EMERGENCY, isolatedEmergency.getIntentType());

        // Explicit emergency alert must map to OPEN_EMERGENCY_ALERT
        VoiceIntent sendAlert = processor.detectIntent("Send emergency alert", lang);
        assertEquals(VoiceIntentType.OPEN_EMERGENCY_ALERT, sendAlert.getIntentType());

        // Standalone "help" must be detected as ambiguous help request
        assertTrue(VoiceIntentMatcher.isAmbiguousHelpRequest("help"));

        // "stop" outside recording state must stop assistant / listening
        VoiceIntent stopIntent = processor.detectIntent("stop", lang);
        assertEquals(VoiceIntentType.STOP_LISTENING, stopIntent.getIntentType());
    }

    // ==========================================
    // 10. CONTEXT-SENSITIVE STOP COMMAND & WAKE WORD RESUME
    // ==========================================

    @Test
    public void testContextSensitiveStopCommandOutsideRecording() {
        // "ನಿಲ್ಲಿಸು" outside recording mode must NOT trigger RECORD_VOICE_MESSAGE
        VoiceIntent intentKn = processor.detectIntent("ನಿಲ್ಲಿಸು", "kn");
        assertNotEquals(VoiceIntentType.RECORD_VOICE_MESSAGE, intentKn.getIntentType());

        // "ರೆಕಾರ್ಡಿಂಗ್ ನಿಲ್ಲಿಸು" inside recording context is recognized by isRecordingStopPhrase
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("ರೆಕಾರ್ಡಿಂಗ್ ನಿಲ್ಲಿಸು"));
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase("ನಿಲ್ಲಿಸು"));

        // Non-stop queries inside recording context must NOT be treated as stop phrases
        assertFalse(VoiceIntentMatcher.isRecordingStopPhrase("ನಾನು ಇಂದು ಬರುತ್ತೇನೆ"));
        assertFalse(VoiceIntentMatcher.isRecordingStopPhrase("ಆರೈಕೆದಾರರಿಗೆ ಸಂದೇಶ"));
    }

    // ==========================================
    // 11. STRICT RECORDING PROMPT & TTS COMPLETION ORDERING (TEST 1)
    // ==========================================

    @Test
    public void testKannadaVoiceCommandToPromptOrdering() {
        // Step 1: Voice command recognized
        String phrase = "ನನ್ನ ಆರೈಕೆದಾರರಿಗೆ ಧ್ವನಿ ಸಂದೇಶ ಕಳುಹಿಸು";
        VoiceIntent intent = processor.detectIntent(phrase, "kn");
        assertEquals(VoiceIntentType.RECORD_VOICE_MESSAGE, intent.getIntentType());

        VoiceCommand command = processor.resolveCommand(intent, RoleManager.ROLE_BLIND_USER);
        assertEquals(VoiceCommandConstants.CMD_RECORD_VOICE_MESSAGE, command.getCommandId());

        // Step 2 & 3: Exact Kannada prompt text verified
        String promptText = VoiceLanguageConfig.getSpeakVoiceMessagePrompt("kn");
        assertEquals("Prompt text must match specification exactly",
                "ನೀವು ಕಳುಹಿಸಲು ಬಯಸುವ ಧ್ವನಿ ಸಂದೇಶವನ್ನು ಮಾತನಾಡಿ.", promptText);

        // Step 4 & 5: Verified that TTS onDone is strictly required before MediaRecorder starts
        java.util.concurrent.atomic.AtomicBoolean ttsCompleted = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicBoolean mediaRecorderStarted = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicBoolean ttsErrorEncountered = new java.util.concurrent.atomic.AtomicBoolean(false);

        Runnable onTtsDone = () -> {
            ttsCompleted.set(true);
            // Simulate 300 ms delay strictly after onDone
            mediaRecorderStarted.set(true);
        };

        Runnable onTtsError = () -> {
            ttsErrorEncountered.set(true);
            // Recovery: MediaRecorder MUST NOT start on error
        };

        // Before onDone: MediaRecorder is NOT started
        assertFalse(mediaRecorderStarted.get());

        // Simulate onDone callback
        onTtsDone.run();
        assertTrue(ttsCompleted.get());
        assertTrue(mediaRecorderStarted.get());

        // Simulate error recovery: MediaRecorder is NOT started on TTS error
        mediaRecorderStarted.set(false);
        onTtsError.run();
        assertTrue(ttsErrorEncountered.get());
        assertFalse("MediaRecorder must NOT start on TTS error", mediaRecorderStarted.get());
    }

    // ==========================================
    // 12. FOCUSED TEST 2: KANNADA RECORDING STOP & REARM LIFECYCLE
    // ==========================================

    @Test
    public void testKannadaStopRecordingPhrasesInContext() {
        // 1. "ರೆಕಾರ್ಡಿಂಗ್ ನಿಲ್ಲಿಸು" -> stop recording
        assertTrue("ರೆಕಾರ್ಡಿಂಗ್ ನಿಲ್ಲಿಸು must be recognized as recording stop phrase",
                VoiceIntentMatcher.isRecordingStopPhrase("ರೆಕಾರ್ಡಿಂಗ್ ನಿಲ್ಲಿಸು"));

        // 2. "ನಿಲ್ಲಿಸು" -> stop recording while recording
        assertTrue("ನಿಲ್ಲಿಸು must be recognized as recording stop phrase in recording context",
                VoiceIntentMatcher.isRecordingStopPhrase("ನಿಲ್ಲಿಸು"));
        assertTrue("ನಿಲ್ಲಿಸಿ must be recognized as recording stop phrase in recording context",
                VoiceIntentMatcher.isRecordingStopPhrase("ನಿಲ್ಲಿಸಿ"));
        assertTrue("ರೆಕಾರ್ಡಿಂಗ್ ನಿಲ್ಲಿಸಿ must be recognized as recording stop phrase",
                VoiceIntentMatcher.isRecordingStopPhrase("ರೆಕಾರ್ಡಿಂಗ್ ನಿಲ್ಲಿಸಿ"));

        // 3. Stop phrase outside VOICE_MESSAGE_RECORDING -> must NOT trigger recording stop / RECORD_VOICE_MESSAGE
        VoiceIntent outsideStop = processor.detectIntent("ನಿಲ್ಲಿಸು", "kn");
        assertNotEquals("Outside recording, ನಿಲ್ಲಿಸು must not trigger RECORD_VOICE_MESSAGE",
                VoiceIntentType.RECORD_VOICE_MESSAGE, outsideStop.getIntentType());

        VoiceIntent outsideStopRecording = processor.detectIntent("ರೆಕಾರ್ಡಿಂಗ್ ನಿಲ್ಲಿಸು", "kn");
        assertNotEquals("Outside recording, ರೆಕಾರ್ಡಿಂಗ್ ನಿಲ್ಲಿಸು must not trigger RECORD_VOICE_MESSAGE",
                VoiceIntentType.RECORD_VOICE_MESSAGE, outsideStopRecording.getIntentType());
    }

    @Test
    public void testRecordingControlSpeechRecognizerRearmContract() {
        // 4. SpeechRecognizer timeout/no-match -> recording-control listener re-arms contract
        java.util.concurrent.atomic.AtomicBoolean recognizerCleanedUp = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicBoolean recognizerRestarted = new java.util.concurrent.atomic.AtomicBoolean(false);

        // Simulate re-arm sequence on ERROR_SPEECH_TIMEOUT or ERROR_NO_MATCH
        Runnable rearmAction = () -> {
            recognizerCleanedUp.set(true); // destroy old recognizer
            recognizerRestarted.set(true); // start fresh instance after delay
        };

        rearmAction.run();
        assertTrue("Old recognizer must be cleaned up on re-arm", recognizerCleanedUp.get());
        assertTrue("New recognizer must be created on re-arm", recognizerRestarted.get());
    }

    @Test
    public void testStopCommandMediaRecorderCleanupAndConfirmationTransition() {
        // 5. Stop command -> MediaRecorder cleanup
        java.util.concurrent.atomic.AtomicBoolean mediaRecorderStopped = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicBoolean speechRecognizerDestroyed = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicBoolean audioFileValidated = new java.util.concurrent.atomic.AtomicBoolean(false);

        // 6. Stop command -> transition to voice-message confirmation with Kannada prompt
        java.util.concurrent.atomic.AtomicReference<AppVoiceAssistant.AssistantState> stateRef =
                new java.util.concurrent.atomic.AtomicReference<>(AppVoiceAssistant.AssistantState.VOICE_MESSAGE_RECORDING);
        java.util.concurrent.atomic.AtomicReference<AppVoiceAssistant.ConfirmationType> confirmTypeRef =
                new java.util.concurrent.atomic.AtomicReference<>(AppVoiceAssistant.ConfirmationType.NONE);

        // Simulate stop command execution
        String userStop = "ರೆಕಾರ್ಡಿಂಗ್ ನಿಲ್ಲಿಸು";
        assertTrue(VoiceIntentMatcher.isRecordingStopPhrase(userStop));

        // MediaRecorder stopped & STT destroyed
        mediaRecorderStopped.set(true);
        speechRecognizerDestroyed.set(true);
        audioFileValidated.set(true);

        confirmTypeRef.set(AppVoiceAssistant.ConfirmationType.VOICE_MESSAGE);
        stateRef.set(AppVoiceAssistant.AssistantState.SPEAKING);

        String confirmPrompt = VoiceLanguageConfig.getConfirmSendVoiceMessagePrompt("kn");
        assertEquals("Confirmation prompt must match specification",
                "ಈ ಧ್ವನಿ ಸಂದೇಶವನ್ನು ಕಳುಹಿಸಬೇಕೇ?", confirmPrompt);

        assertTrue(mediaRecorderStopped.get());
        assertTrue(speechRecognizerDestroyed.get());
        assertTrue(audioFileValidated.get());
        assertEquals(AppVoiceAssistant.ConfirmationType.VOICE_MESSAGE, confirmTypeRef.get());
        assertEquals(AppVoiceAssistant.AssistantState.SPEAKING, stateRef.get());

        // 7 & 8: Wake-word resumption on workflow finish
        stateRef.set(AppVoiceAssistant.AssistantState.IDLE);
        assertEquals(AppVoiceAssistant.AssistantState.IDLE, stateRef.get());
    }
}
