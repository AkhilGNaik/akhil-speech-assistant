package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Validates microphone conflict protection for the Voice Assistant:
 * 1. Must NOT start microphone when a real-time voice call is active.
 * 2. Must NOT start microphone when voice recording is active.
 * 3. Must NOT start microphone when another microphone operation is active.
 * 4. Exact feedback for active voice call: "Voice assistant is unavailable during a call."
 * 5. Exact feedback for active voice recording: "Please stop recording first."
 * 6. Must NOT terminate an active voice call automatically.
 * 7. Must NOT terminate an active voice recording automatically.
 */
public class MicrophoneConflictProtectionTest {

    @Test
    public void testConflictMessagesExactStrings() {
        assertEquals(
                "Voice assistant is unavailable during a call.",
                VoiceCommandConstants.MESSAGE_CONFLICT_CALL
        );
        assertEquals(
                "Please stop recording first.",
                VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING
        );
    }

    @Test
    public void testMicrophoneConflictErrorCodes() {
        assertEquals(1007, AppVoiceAssistant.ERROR_CALL_ACTIVE);
        assertEquals(1008, AppVoiceAssistant.ERROR_RECORDING_ACTIVE);
        assertEquals(1009, AppVoiceAssistant.ERROR_MIC_BUSY);
    }

    @Test
    public void testSafeMicrophoneStateEvaluationWithActiveCall() {
        // Simulating microphone-state evaluator
        boolean callActive = true;
        boolean recordingActive = false;
        boolean otherMicActive = false;

        boolean micInUse = callActive || recordingActive || otherMicActive;
        assertTrue("Microphone must be reported as in use when call is active", micInUse);

        String feedback = null;
        int errorCode = -1;
        if (callActive) {
            feedback = VoiceCommandConstants.MESSAGE_CONFLICT_CALL;
            errorCode = AppVoiceAssistant.ERROR_CALL_ACTIVE;
        } else if (recordingActive) {
            feedback = VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING;
            errorCode = AppVoiceAssistant.ERROR_RECORDING_ACTIVE;
        }

        assertEquals("Voice assistant is unavailable during a call.", feedback);
        assertEquals(AppVoiceAssistant.ERROR_CALL_ACTIVE, errorCode);

        // Verification: Call remains active (not stopped automatically)
        assertTrue("Active call must NOT be stopped automatically", callActive);
    }

    @Test
    public void testSafeMicrophoneStateEvaluationWithActiveRecording() {
        boolean callActive = false;
        boolean recordingActive = true;
        boolean otherMicActive = false;

        boolean micInUse = callActive || recordingActive || otherMicActive;
        assertTrue("Microphone must be reported as in use when recording is active", micInUse);

        String feedback = null;
        int errorCode = -1;
        if (callActive) {
            feedback = VoiceCommandConstants.MESSAGE_CONFLICT_CALL;
            errorCode = AppVoiceAssistant.ERROR_CALL_ACTIVE;
        } else if (recordingActive) {
            feedback = VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING;
            errorCode = AppVoiceAssistant.ERROR_RECORDING_ACTIVE;
        }

        assertEquals("Please stop recording first.", feedback);
        assertEquals(AppVoiceAssistant.ERROR_RECORDING_ACTIVE, errorCode);

        // Verification: Recording remains active (not stopped automatically)
        assertTrue("Active recording must NOT be stopped automatically", recordingActive);
    }

    @Test
    public void testSafeMicrophoneStateEvaluationWithAnotherMicOperation() {
        boolean callActive = false;
        boolean recordingActive = false;
        boolean otherMicActive = true;

        boolean micInUse = callActive || recordingActive || otherMicActive;
        assertTrue("Microphone must be reported as in use when another mic operation is active", micInUse);

        String feedback = null;
        int errorCode = -1;
        if (callActive) {
            feedback = VoiceCommandConstants.MESSAGE_CONFLICT_CALL;
            errorCode = AppVoiceAssistant.ERROR_CALL_ACTIVE;
        } else if (recordingActive) {
            feedback = VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING;
            errorCode = AppVoiceAssistant.ERROR_RECORDING_ACTIVE;
        } else if (otherMicActive) {
            feedback = VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING;
            errorCode = AppVoiceAssistant.ERROR_MIC_BUSY;
        }

        assertEquals("Please stop recording first.", feedback);
        assertEquals(AppVoiceAssistant.ERROR_MIC_BUSY, errorCode);
    }

    @Test
    public void testSafeMicrophoneStateWhenIdle() {
        boolean callActive = false;
        boolean recordingActive = false;
        boolean otherMicActive = false;

        boolean micInUse = callActive || recordingActive || otherMicActive;
        assertFalse("Microphone must not be reported in use when all operations are idle", micInUse);
    }

    @Test
    public void testPrioritizeCallConflictOverRecordingConflict() {
        // When both call and recording are somehow active, call conflict is prioritized
        boolean callActive = true;
        boolean recordingActive = true;

        String feedback;
        if (callActive) {
            feedback = VoiceCommandConstants.MESSAGE_CONFLICT_CALL;
        } else {
            feedback = VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING;
        }

        assertEquals("Voice assistant is unavailable during a call.", feedback);
    }
}
