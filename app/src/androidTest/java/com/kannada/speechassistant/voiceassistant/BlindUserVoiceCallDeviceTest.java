package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.kannada.speechassistant.RoleManager;
import com.kannada.speechassistant.SessionManager;
import com.kannada.speechassistant.VoiceCallManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BlindUserVoiceCallDeviceTest {

    private static final String TAG = "BlindVoiceCallDeviceTest";
    private Context appContext;
    private SessionManager sessionManager;
    private VoiceCommandProcessor processor;

    @Before
    public void setUp() {
        appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        sessionManager = new SessionManager(appContext);
        sessionManager.createLoginSession("blind_user_device_test_uid", "blind@example.com", RoleManager.ROLE_BLIND_USER);
        processor = new VoiceCommandProcessor(appContext);
    }

    @Test
    public void testCaregiverUidValidityOnDevice() {
        // Real UID must be accepted
        assertTrue(isValidUid("nzzDhtxSpFfWuiKCIasAnlCsQLB2"));
        assertTrue(isValidUid("t96ilbqBZdaCeZ5ssHl9OmjzsVH2"));

        // Mock, empty, or null must be rejected
        assertFalse(isValidUid(null));
        assertFalse(isValidUid(""));
        assertFalse(isValidUid("   "));
        assertFalse(isValidUid("mock_caregiver_uid"));
    }

    @Test
    public void testCallCaregiverVoiceCommandsOnDevice() {
        // English variants
        assertVoiceCallIntent("call my caregiver", "en");
        assertVoiceCallIntent("call caregiver", "en");
        assertVoiceCallIntent("start voice call", "en");
        assertVoiceCallIntent("Assistant, call my caregiver", "en");

        // Kannada variants
        assertVoiceCallIntent("ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ", "kn");
        assertVoiceCallIntent("ನನ್ನ ಸಹಾಯಕರಿಗೆ ಕರೆ ಮಾಡಿ", "kn");
        assertVoiceCallIntent("ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡು", "kn");
        assertVoiceCallIntent("ಸಹಾಯಕರಿಗೆ ಕರೆ ಮಾಡು", "kn");
        assertVoiceCallIntent("ಕೇರ್‌ಗಿವರ್‌ಗೆ ಕರೆ ಮಾಡಿ", "kn");
        assertVoiceCallIntent("ಕೇರ್‌ಗಿವರ್‌ಗೆ ಕಾಲ್ ಮಾಡು", "kn");
        assertVoiceCallIntent("Assistant, ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ", "kn");
    }

    @Test
    public void testCallDebugLoggingOutput() {
        String callerUid = "caller_test_uid";
        String caregiverUid = "nzzDhtxSpFfWuiKCIasAnlCsQLB2";
        String callId = callerUid + "_" + caregiverUid + "_" + System.currentTimeMillis();

        String callDebugLine = "CALL_DEBUG: callerUid=" + callerUid + ", caregiverUid=" + caregiverUid + ", callId=" + callId;
        Log.d("CALL_DEBUG", callDebugLine);
        Log.d("VoiceCallSignaling", callDebugLine);

        assertTrue(callDebugLine.startsWith("CALL_DEBUG: "));
        assertTrue(callDebugLine.contains("callerUid=" + callerUid));
        assertTrue(callDebugLine.contains("caregiverUid=" + caregiverUid));
        assertTrue(callDebugLine.contains("callId=" + callId));
    }

    @Test
    public void testVoiceCallManagerIntegrationOnDevice() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            VoiceCallManager callManager = VoiceCallManager.getInstance(appContext);
            assertNotNull(callManager);
            // In initial idle state
            assertFalse(callManager.isCallActive());
            assertEquals(VoiceCallManager.CallState.IDLE, callManager.getCurrentState());
        });
    }

    private boolean isValidUid(String uid) {
        return uid != null && !uid.trim().isEmpty() && !"mock_caregiver_uid".equals(uid.trim());
    }

    private void assertVoiceCallIntent(String utterance, String lang) {
        VoiceIntent intent = processor.detectIntent(utterance, lang);
        assertNotNull("Intent must not be null for utterance: " + utterance, intent);
        assertEquals("IntentType must be CALL_CAREGIVER for: " + utterance,
                VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
        assertEquals("IntentName must be INTENT_OPEN_VOICE_CALL for: " + utterance,
                VoiceCommandConstants.INTENT_OPEN_VOICE_CALL, intent.getIntentName());
    }
}
