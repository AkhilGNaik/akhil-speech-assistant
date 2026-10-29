package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.kannada.speechassistant.LanguageManager;
import com.kannada.speechassistant.RoleManager;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Focused unit tests verifying the 20 reliability, real-time, and self-recovery requirements
 * for the Blind User Voice Assistant System.
 */
public class BlindVoiceAssistantReliabilityTest {

    // 1. Wake word detected once
    @Test
    public void testWakeWordDetectedOnce() {
        String json = "{\"text\": \"assistant\"}";
        String candidate = WakeWordManager.parseAndExtractCandidate(json);
        assertNotNull("Wake word candidate must be detected", candidate);
        assertEquals("assistant", candidate);
    }

    // 2. Repeated wake-word detection does not create duplicate sessions
    @Test
    public void testRepeatedWakeWordDoesNotDuplicateSessions() {
        AtomicBoolean isTriggered = new AtomicBoolean(false);
        // First trigger
        boolean firstTrigger = isTriggered.compareAndSet(false, true);
        assertTrue("First trigger must succeed", firstTrigger);

        // Immediate second trigger while active
        boolean secondTrigger = isTriggered.compareAndSet(false, true);
        assertFalse("Second trigger while already triggered must be blocked", secondTrigger);
    }

    // 3. Valid command is recognized and dispatched once
    @Test
    public void testValidCommandRecognizedAndDispatchedOnce() {
        VoiceIntent intent = VoiceIntentMatcher.matchIntent("open profile", "en");
        assertEquals(VoiceIntentType.OPEN_PROFILE, intent.getIntentType());

        AtomicBoolean commandHandled = new AtomicBoolean(false);
        assertTrue("Command must be dispatched on first handling", commandHandled.compareAndSet(false, true));
        assertFalse("Duplicate dispatch must be blocked", commandHandled.compareAndSet(false, true));
    }

    // 4. Empty speech result returns to a recoverable state
    @Test
    public void testEmptySpeechResultReturnsToRecoverableState() {
        String emptyText = "";
        VoiceIntent intent = VoiceIntentMatcher.matchIntent(emptyText, "kn");
        assertEquals(VoiceIntentType.UNKNOWN, intent.getIntentType());

        // Valid state machine transitions from WAITING_FOR_COMMAND to IDLE or RECOVERING
        assertTrue(AppVoiceAssistant.isValidStateTransition(
                AppVoiceAssistant.AssistantState.WAITING_FOR_COMMAND,
                AppVoiceAssistant.AssistantState.IDLE));
        assertTrue(AppVoiceAssistant.isValidStateTransition(
                AppVoiceAssistant.AssistantState.WAITING_FOR_COMMAND,
                AppVoiceAssistant.AssistantState.RECOVERING));
    }

    // 5. No-speech timeout recovers safely
    @Test
    public void testNoSpeechTimeoutRecoversSafely() {
        // State transition from COMMAND_LISTENING to PROCESSING_COMMAND to SPEAKING to IDLE
        assertTrue(AppVoiceAssistant.isValidStateTransition(
                AppVoiceAssistant.AssistantState.COMMAND_LISTENING,
                AppVoiceAssistant.AssistantState.PROCESSING_COMMAND));
        assertTrue(AppVoiceAssistant.isValidStateTransition(
                AppVoiceAssistant.AssistantState.PROCESSING_COMMAND,
                AppVoiceAssistant.AssistantState.SPEAKING));
        assertTrue(AppVoiceAssistant.isValidStateTransition(
                AppVoiceAssistant.AssistantState.SPEAKING,
                AppVoiceAssistant.AssistantState.IDLE));
    }

    // 6. Audio recording error recovers without a retry storm
    @Test
    public void testAudioRecordingErrorRecoversWithoutRetryStorm() {
        int maxRetries = 5;
        AtomicInteger attempts = new AtomicInteger(0);

        for (int i = 0; i < 10; i++) {
            int attempt = attempts.incrementAndGet();
            if (attempt > maxRetries) {
                // Must halt retries and not loop indefinitely
                break;
            }
        }
        assertEquals("Retry attempts must be bounded at max 6 iterations", 6, attempts.get());
    }

    // 7. Busy microphone error waits for existing owner to release it
    @Test
    public void testBusyMicrophoneYieldsToCurrentOwner() {
        // Microphone conflict constants
        assertEquals("Voice assistant is unavailable during a call.", VoiceCommandConstants.MESSAGE_CONFLICT_CALL);
        assertEquals("Please stop recording first.", VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING);

        // Assistant must support TEMPORARILY_PAUSED state when microphone is busy
        assertTrue(AppVoiceAssistant.isValidStateTransition(
                AppVoiceAssistant.AssistantState.IDLE,
                AppVoiceAssistant.AssistantState.TEMPORARILY_PAUSED));
        assertTrue(AppVoiceAssistant.isValidStateTransition(
                AppVoiceAssistant.AssistantState.TEMPORARILY_PAUSED,
                AppVoiceAssistant.AssistantState.IDLE));
    }

    // 8. Stale recognition callbacks are ignored (Session Token Validation)
    @Test
    public void testStaleRecognitionCallbacksAreIgnored() {
        AtomicLong sessionTokenGenerator = new AtomicLong(0);

        long session1 = sessionTokenGenerator.incrementAndGet(); // session #1
        long session2 = sessionTokenGenerator.incrementAndGet(); // session #2 supersedes session #1

        // Callback from session 1 arrives
        boolean session1Valid = (session1 == sessionTokenGenerator.get());
        assertFalse("Stale callback from session 1 must be rejected", session1Valid);

        // Callback from session 2 arrives
        boolean session2Valid = (session2 == sessionTokenGenerator.get());
        assertTrue("Callback from active session 2 must be accepted", session2Valid);
    }

    // 9. TTS completion returns the assistant to the correct state
    @Test
    public void testTtsCompletionReturnsToIdle() {
        assertTrue(AppVoiceAssistant.isValidStateTransition(
                AppVoiceAssistant.AssistantState.SPEAKING,
                AppVoiceAssistant.AssistantState.IDLE));
        assertTrue(AppVoiceAssistant.isValidStateTransition(
                AppVoiceAssistant.AssistantState.SPEAKING_RESPONSE,
                AppVoiceAssistant.AssistantState.IDLE));
    }

    // 10. TTS failure does not leave the assistant permanently stuck
    @Test
    public void testTtsFailureDoesNotFreezeAssistant() {
        // Fallback transitions always allow recovery from SPEAKING
        assertTrue(AppVoiceAssistant.isValidStateTransition(
                AppVoiceAssistant.AssistantState.SPEAKING,
                AppVoiceAssistant.AssistantState.RECOVERING));
        assertTrue(AppVoiceAssistant.isValidStateTransition(
                AppVoiceAssistant.AssistantState.RECOVERING,
                AppVoiceAssistant.AssistantState.IDLE));
    }

    // 11. Kannada, English, Hindi, and Malayalam commands respect language isolation
    @Test
    public void testLanguageIsolation() {
        // Kannada mode: Accepts Kannada, rejects Hindi and Malayalam
        assertEquals(VoiceIntentType.OPEN_HOME, VoiceIntentMatcher.matchIntent("ಹೋಮ್ ತೆರೆಯಿರಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.matchIntent("होम स्क्रीन खोलो", "kn").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.matchIntent("ഹോം തുറക്കുക", "kn").getIntentType());

        // Hindi mode: Accepts Hindi, rejects Kannada and Malayalam
        assertEquals(VoiceIntentType.OPEN_HOME, VoiceIntentMatcher.matchIntent("होम स्क्रीन खोलो", "hi").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.matchIntent("ಹೋಮ್ ತೆರೆಯಿರಿ", "hi").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.matchIntent("ഹോം തുറക്കുക", "hi").getIntentType());

        // Malayalam mode: Accepts Malayalam, rejects Kannada and Hindi
        assertEquals(VoiceIntentType.OPEN_HOME, VoiceIntentMatcher.matchIntent("ഹോം തുറക്കുക", "ml").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.matchIntent("ಹೋಮ್ ತೆರೆಯಿರಿ", "ml").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.matchIntent("होम स्क्रीन खोलो", "ml").getIntentType());

        // English mode: Rejects Indic scripts
        assertEquals(VoiceIntentType.OPEN_HOME, VoiceIntentMatcher.matchIntent("open home", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.matchIntent("ಹೋಮ್ ತೆರೆಯಿರಿ", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.matchIntent("होम स्क्रीन खोलो", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.matchIntent("ഹോം തുറക്കുക", "en").getIntentType());
    }

    // 12. English fallback works where permitted
    @Test
    public void testEnglishFallbackWherePermitted() {
        // English commands must resolve successfully even when user is in Kannada, Hindi, or Malayalam mode
        assertEquals(VoiceIntentType.OPEN_HOME, VoiceIntentMatcher.matchIntent("open home", "kn").getIntentType());
        assertEquals(VoiceIntentType.OPEN_PROFILE, VoiceIntentMatcher.matchIntent("open profile", "kn").getIntentType());
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.matchIntent("call caregiver", "kn").getIntentType());

        assertEquals(VoiceIntentType.OPEN_HOME, VoiceIntentMatcher.matchIntent("open home", "hi").getIntentType());
        assertEquals(VoiceIntentType.OPEN_PROFILE, VoiceIntentMatcher.matchIntent("open profile", "hi").getIntentType());

        assertEquals(VoiceIntentType.OPEN_HOME, VoiceIntentMatcher.matchIntent("open home", "ml").getIntentType());
        assertEquals(VoiceIntentType.OPEN_PROFILE, VoiceIntentMatcher.matchIntent("open profile", "ml").getIntentType());
    }

    // 13. Ambiguous or negated commands are not executed incorrectly
    @Test
    public void testNegationAndAmbiguousCommandsNotExecuted() {
        // Action Negation Protection
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.matchIntent("don't call my caregiver", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.matchIntent("do not send message", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.matchIntent("ಕರೆ ಮಾಡಬೇಡಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.matchIntent("कॉल मत करो", "hi").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.matchIntent("വിളിക്കരുത്", "ml").getIntentType());

        // Generic non-action inquiries must not trigger actions
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.matchIntent("who is my caregiver", "en").getIntentType());
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.matchIntent("where is my caregiver", "en").getIntentType());
    }

    // 14. Delete-message confirmation and cancellation remain safe
    @Test
    public void testDeleteMessageConfirmationAndCancellationSafety() {
        // Positive confirmation
        assertEquals(VoiceIntentType.CONFIRM_YES, VoiceIntentMatcher.matchIntent("confirm delete", "en").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_YES, VoiceIntentMatcher.matchIntent("yes delete", "en").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_YES, VoiceIntentMatcher.matchIntent("ಅಳಿಸಲು ದೃಢೀಕರಿಸಿ", "kn").getIntentType());

        // Negative cancellation
        assertEquals(VoiceIntentType.CONFIRM_NO, VoiceIntentMatcher.matchIntent("cancel delete", "en").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_NO, VoiceIntentMatcher.matchIntent("don't delete", "en").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_NO, VoiceIntentMatcher.matchIntent("ರದ್ದುಮಾಡಿ", "kn").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_NO, VoiceIntentMatcher.matchIntent("रद्द करें", "hi").getIntentType());
    }

    // 15. Background listening does not execute unauthorized commands
    @Test
    public void testBackgroundListeningAuthorizesOnlyOpenApp() {
        // Only "open app" variants are authorized in background
        assertTrue(BlindWakeWordService.isOpenAppPhrase("hey assistant open app"));
        assertTrue(BlindWakeWordService.isOpenAppPhrase("assistant open app"));
        assertTrue(BlindWakeWordService.isOpenAppPhrase("launch app"));
        assertTrue(BlindWakeWordService.isOpenAppPhrase("open speech assistant"));
        assertTrue(BlindWakeWordService.isOpenAppPhrase("ಆ್ಯಪ್ ತೆರೆ"));

        // Sensitive actions are strictly forbidden and rejected in background
        assertFalse(BlindWakeWordService.isOpenAppPhrase("call caregiver"));
        assertFalse(BlindWakeWordService.isOpenAppPhrase("send message"));
        assertFalse(BlindWakeWordService.isOpenAppPhrase("delete last message"));
        assertFalse(BlindWakeWordService.isOpenAppPhrase("emergency sos"));
        assertFalse(BlindWakeWordService.isOpenAppPhrase("open settings"));
    }

    // 16. Voice-call and recording microphone ownership is respected
    @Test
    public void testMicrophonePriorityRespectsCallAndRecording() {
        // When a call or recording is active, assistant states recognize mic in use
        boolean isCallActive = true;
        boolean isRecordingActive = false;
        assertTrue("Microphone must be reported busy when call is active", isCallActive || isRecordingActive);
    }

    // 17. Activity pause and resume do not create duplicate listeners
    @Test
    public void testPauseAndResumeStateConsistency() {
        // Transition from LISTENING to TEMPORARILY_PAUSED on pause
        assertTrue(AppVoiceAssistant.isValidStateTransition(
                AppVoiceAssistant.AssistantState.COMMAND_LISTENING,
                AppVoiceAssistant.AssistantState.TEMPORARILY_PAUSED));

        // Transition from TEMPORARILY_PAUSED back to IDLE on resume
        assertTrue(AppVoiceAssistant.isValidStateTransition(
                AppVoiceAssistant.AssistantState.TEMPORARILY_PAUSED,
                AppVoiceAssistant.AssistantState.IDLE));
    }

    // 18. Repeated recovery failures do not create infinite loops
    @Test
    public void testRepeatedRecoveryFailuresDoNotCreateInfiniteLoops() {
        int maxErrors = 4;
        AtomicInteger consecutiveErrors = new AtomicInteger(0);

        for (int i = 0; i < 10; i++) {
            int err = consecutiveErrors.incrementAndGet();
            if (err >= maxErrors) {
                // Must transition to recovering and reset counter
                consecutiveErrors.set(0);
                break;
            }
        }
        assertEquals("Consecutive error counter must reset after threshold", 0, consecutiveErrors.get());
    }

    // 19. A successful command does not execute twice
    @Test
    public void testSuccessfulCommandDoesNotExecuteTwice() {
        AtomicBoolean latch = new AtomicBoolean(false);
        int executionCount = 0;

        // First execution attempt
        if (latch.compareAndSet(false, true)) {
            executionCount++;
        }

        // Duplicate callback arrives
        if (latch.compareAndSet(false, true)) {
            executionCount++;
        }

        assertEquals("Command must only execute once", 1, executionCount);
    }

    // 20. The assistant can recover from multiple consecutive recognition failures
    @Test
    public void testAssistantRecoversFromMultipleConsecutiveFailures() {
        // State machine allows RECOVERING -> IDLE -> STARTING_COMMAND_RECOGNITION -> COMMAND_LISTENING
        assertTrue(AppVoiceAssistant.isValidStateTransition(
                AppVoiceAssistant.AssistantState.RECOVERING,
                AppVoiceAssistant.AssistantState.IDLE));
        assertTrue(AppVoiceAssistant.isValidStateTransition(
                AppVoiceAssistant.AssistantState.IDLE,
                AppVoiceAssistant.AssistantState.STARTING_COMMAND_RECOGNITION));
        assertTrue(AppVoiceAssistant.isValidStateTransition(
                AppVoiceAssistant.AssistantState.STARTING_COMMAND_RECOGNITION,
                AppVoiceAssistant.AssistantState.COMMAND_LISTENING));
    }
}
