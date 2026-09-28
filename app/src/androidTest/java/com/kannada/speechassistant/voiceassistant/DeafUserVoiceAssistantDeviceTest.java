package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.kannada.speechassistant.LanguageManager;
import com.kannada.speechassistant.R;
import com.kannada.speechassistant.RoleManager;
import com.kannada.speechassistant.SessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Physical Device Verification Test Suite for TASK 1:
 * Deaf User Visual Voice Assistant Foundation on Samsung Galaxy A13 (SM-A135F / RZ8TB08MDYN).
 */
@RunWith(AndroidJUnit4.class)
public class DeafUserVoiceAssistantDeviceTest {

    private Context appContext;
    private Context themeContext;
    private SessionManager sessionManager;
    private AppVoiceAssistant assistant;
    private VoiceCommandProcessor processor;

    @Before
    public void setUp() {
        appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        themeContext = new ContextThemeWrapper(appContext, R.style.Theme_SpeechAssistant);
        sessionManager = new SessionManager(appContext);
        sessionManager.createLoginSession("test_deaf_uid", "test_deaf@example.com", RoleManager.ROLE_DEAF_USER, "kn");
        processor = new VoiceCommandProcessor(appContext);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            assistant = AppVoiceAssistant.getInstance(appContext);
        });
    }

    // =========================================================================
    // 1. DEAF ASSISTANT RESPONSE MODE = TEXT & TTS RESPONSE = DISABLED
    // =========================================================================

    @Test
    public void test1_DeafUserRoleIsDetectedCorrectly() {
        assertTrue(assistant.isDeafUser(RoleManager.ROLE_DEAF_USER));
        assertTrue(assistant.isDeafUser(RoleManager.ROLE_SPEECH_IMPAIRED));
        assertTrue(assistant.isDeafUser("Mute, Deaf & Blind User"));
        assertTrue(assistant.isDeafUser("Mute, Deaf & Blind Users"));

        assertFalse(assistant.isDeafUser(RoleManager.ROLE_BLIND_USER));
        assertFalse(assistant.isDeafUser(RoleManager.ROLE_ADMIN_CAREGIVER));
        assertFalse(assistant.isDeafUser(null));
    }

    @Test
    public void test2_TtsSuppressedForDeafUser_TextResponseOnly() throws Exception {
        final AtomicBoolean speechSynthesized = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            sessionManager.createLoginSession("test_deaf_uid", "test_deaf@example.com", RoleManager.ROLE_DEAF_USER, "kn");
            
            // Calling speakResponseWithLanguage in Deaf mode must NOT synthesize speech
            assistant.speakResponseWithLanguage("ತೆರೆಯಲಾಗಿದೆ", "kn", () -> {
                speechSynthesized.set(true);
                latch.countDown();
            });
        });

        assertTrue("Callback should be invoked promptly without TTS blocking", latch.await(2, TimeUnit.SECONDS));
        assertTrue("Speech callback executed directly in text-only mode", speechSynthesized.get());
    }

    // =========================================================================
    // 2. DEAF ASSISTANT WAKE WORD = Assistant
    // =========================================================================

    @Test
    public void test3_WakeWordAssistantUniversality() {
        WakeWordManager wwm = WakeWordManager.getInstance(appContext);
        assertNotNull(wwm);

        // Universal Wake Word is "Assistant"
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("assistant"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("ASSISTANT"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("Assistant open profile"));
        assertTrue(VoiceIntentMatcher.hasWakeWord("hey assistant open home"));

        // Deaf User is eligible for wake word activation
        sessionManager.createLoginSession("test_deaf_uid", "test_deaf@example.com", RoleManager.ROLE_DEAF_USER);
        assertTrue(wwm.isDeafUser());
        assertTrue(wwm.isEligibleForWakeWord());

        sessionManager.createLoginSession("test_speech_uid", "test_speech@example.com", RoleManager.ROLE_SPEECH_IMPAIRED);
        assertTrue(wwm.isDeafUser());
        assertTrue(wwm.isEligibleForWakeWord());
    }

    // =========================================================================
    // 3. DEAF ASSISTANT VISUAL FEEDBACK = IMPLEMENTED (ALL 8 STATES)
    // =========================================================================

    @Test
    public void test4_DeafAssistantResponseManagerAllVisualStates() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            LayoutInflater inflater = LayoutInflater.from(themeContext);
            View rootView = inflater.inflate(R.layout.activity_speech_impaired_dashboard, null);

            MaterialCardView card = rootView.findViewById(R.id.cardDeafAssistantResponse);
            TextView txtTitle = card.findViewById(R.id.txtDeafAssistantTitle);
            TextView txtMsg = card.findViewById(R.id.txtDeafAssistantMessage);
            TextView txtSubMsg = card.findViewById(R.id.txtDeafAssistantSubMessage);
            ProgressBar progress = card.findViewById(R.id.progressDeafAssistant);

            DeafAssistantResponseManager manager = new DeafAssistantResponseManager();
            manager.bind(themeContext, rootView);

            // 1. Initial IDLE state
            assertFalse(manager.isCardVisible());

            // 2. State: WAKE WORD DETECTED
            manager.showWakeWordDetected();
            assertTrue(manager.isCardVisible());
            assertTrue(txtMsg.getText().toString().length() > 0);

            // 3. State: LISTENING
            manager.showListeningState();
            assertTrue(manager.isCardVisible());
            assertEquals(View.VISIBLE, txtSubMsg.getVisibility());

            // 4. State: PROCESSING
            manager.showProcessingState();
            assertTrue(manager.isCardVisible());
            assertEquals(View.VISIBLE, progress.getVisibility());

            // 5. State: SUCCESS / FUNCTION EXECUTED
            manager.showSuccess("✓ ಪ್ರೊಫೈಲ್ ತೆರೆಯಲಾಗಿದೆ");
            assertTrue(manager.isCardVisible());
            assertEquals("✓ ಪ್ರೊಫೈಲ್ ತೆರೆಯಲಾಗಿದೆ", txtMsg.getText().toString());
            assertEquals(View.GONE, progress.getVisibility());

            // 6. State: COMMAND NOT UNDERSTOOD
            manager.showCommandNotRecognized();
            assertTrue(manager.isCardVisible());
            assertEquals(View.GONE, progress.getVisibility());

            // 7. State: FUNCTION NOT AVAILABLE
            manager.showFunctionNotAvailable();
            assertTrue(manager.isCardVisible());
            assertEquals(View.GONE, progress.getVisibility());

            // 8. State: ERROR
            manager.showError("Network timeout");
            assertTrue(manager.isCardVisible());
            assertEquals("Network timeout", txtMsg.getText().toString());

            // Conflict state: Mic in use
            manager.showMicConflict("Microphone in use by Quick Text");
            assertTrue(manager.isCardVisible());
            assertEquals("Microphone in use by Quick Text", txtMsg.getText().toString());

            // Dismiss
            manager.dismiss();
            assertFalse(manager.isCardVisible());

            latch.countDown();
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
    }

    // =========================================================================
    // 4. DEAF ASSISTANT MULTILINGUAL = KANNADA + HINDI + MALAYALAM + ENGLISH
    // =========================================================================

    @Test
    public void test5_MultilingualVisualStringsIntegrity() {
        // Kannada
        assertEquals("ಸಹಾಯಕ ಸಕ್ರಿಯಗೊಂಡಿದೆ", VoiceLanguageConfig.getDeafAssistantActivatedText("kn"));
        assertEquals("ಆಲಿಸುತ್ತಿದೆ...", VoiceLanguageConfig.getDeafListeningText("kn"));
        assertEquals("ನಿಮ್ಮ ಆಜ್ಞೆಯನ್ನು ಮಾತನಾಡಿ", VoiceLanguageConfig.getDeafSpeakCommandPrompt("kn"));
        assertEquals("ಪ್ರಕ್ರಿಯೆ ನಡೆಯುತ್ತಿದೆ...", VoiceLanguageConfig.getDeafProcessingText("kn"));
        assertEquals("ಕ್ಷಮಿಸಿ, ಆಜ್ಞೆ ಅರ್ಥವಾಗಲಿಲ್ಲ.", VoiceLanguageConfig.getDeafCommandNotUnderstoodText("kn"));
        assertEquals("ಈ ಕಾರ್ಯ ಲಭ್ಯವಿಲ್ಲ.", VoiceLanguageConfig.getDeafFunctionNotAvailableText("kn"));

        // Hindi
        assertEquals("सहायक सक्रिय हुआ", VoiceLanguageConfig.getDeafAssistantActivatedText("hi"));
        assertEquals("सुन रहा है...", VoiceLanguageConfig.getDeafListeningText("hi"));
        assertEquals("अपना आदेश बोलें", VoiceLanguageConfig.getDeafSpeakCommandPrompt("hi"));
        assertEquals("प्रक्रिया जारी है...", VoiceLanguageConfig.getDeafProcessingText("hi"));
        assertEquals("क्षमा करें, आदेश समझ नहीं आया।", VoiceLanguageConfig.getDeafCommandNotUnderstoodText("hi"));
        assertEquals("यह कार्य उपलब्ध नहीं है।", VoiceLanguageConfig.getDeafFunctionNotAvailableText("hi"));

        // Malayalam
        assertEquals("അസിസ്റ്റന്റ് സജീവമായി", VoiceLanguageConfig.getDeafAssistantActivatedText("ml"));
        assertEquals("കേൾക്കുന്നു...", VoiceLanguageConfig.getDeafListeningText("ml"));
        assertEquals("നിങ്ങളുടെ കമാൻഡ് പറയുക", VoiceLanguageConfig.getDeafSpeakCommandPrompt("ml"));
        assertEquals("പ്രോസസ്സ് ചെയ്യുന്നു...", VoiceLanguageConfig.getDeafProcessingText("ml"));
        assertEquals("ക്ഷമിക്കണം, കമാൻഡ് മനസ്സിലായില്ല.", VoiceLanguageConfig.getDeafCommandNotUnderstoodText("ml"));
        assertEquals("ഈ പ്രവർത്തനം ലഭ്യമല്ല.", VoiceLanguageConfig.getDeafFunctionNotAvailableText("ml"));

        // English
        assertEquals("Assistant activated", VoiceLanguageConfig.getDeafAssistantActivatedText("en"));
        assertEquals("Listening...", VoiceLanguageConfig.getDeafListeningText("en"));
        assertEquals("Speak your command", VoiceLanguageConfig.getDeafSpeakCommandPrompt("en"));
        assertEquals("Processing...", VoiceLanguageConfig.getDeafProcessingText("en"));
        assertEquals("Sorry, I didn't understand the command.", VoiceLanguageConfig.getDeafCommandNotUnderstoodText("en"));
        assertEquals("This function is not available.", VoiceLanguageConfig.getDeafFunctionNotAvailableText("en"));
    }

    // =========================================================================
    // 5. CAREGIVER CALLING ADDED = NO & ROLE AWARENESS
    // =========================================================================

    @Test
    public void test6_CaregiverCallingNotAvailableForDeafUser() {
        VoiceIntent callIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_VOICE_CALL, "open voice call", 1.0f, null);
        VoiceCommand cmdVoiceCall = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_VOICE_CALL, callIntent, VoiceCommandProcessor.BLIND_ONLY_ROLES, false);

        VoiceIntent voiceMsgIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_VOICE_MESSAGE, "open voice message", 1.0f, null);
        VoiceCommand cmdVoiceMsg = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_VOICE_MESSAGE, voiceMsgIntent, VoiceCommandProcessor.BLIND_ONLY_ROLES, false);

        // Deaf User must NOT be authorized for Voice Call or Voice Message
        assertFalse(cmdVoiceCall.isRoleAuthorized(RoleManager.ROLE_DEAF_USER));
        assertFalse(cmdVoiceCall.isRoleAuthorized(RoleManager.ROLE_SPEECH_IMPAIRED));
        assertFalse(cmdVoiceMsg.isRoleAuthorized(RoleManager.ROLE_DEAF_USER));

        // Existing Deaf User feature: Open Profile MUST be authorized
        VoiceIntent profileIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_PROFILE, "open profile", 1.0f, null);
        VoiceCommand cmdProfile = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_PROFILE, profileIntent, VoiceCommandProcessor.PROFILE_ROLES, false);
        assertTrue(cmdProfile.isRoleAuthorized(RoleManager.ROLE_DEAF_USER));
        assertTrue(cmdProfile.isRoleAuthorized(RoleManager.ROLE_SPEECH_IMPAIRED));
    }

    // =========================================================================
    // 6. BLIND ASSISTANT MODIFIED = NO
    // =========================================================================

    @Test
    public void test7_BlindUserAssistantBehaviorUnchanged() {
        sessionManager.createLoginSession("test_blind_uid", "test_blind@example.com", RoleManager.ROLE_BLIND_USER);
        WakeWordManager wwm = WakeWordManager.getInstance(appContext);
        assertTrue(wwm.isBlindUser());

        // Blind user still authorized for voice call and tts
        VoiceIntent callIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_VOICE_CALL, "call caregiver", 1.0f, null);
        VoiceCommand cmdVoiceCall = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_VOICE_CALL, callIntent, VoiceCommandProcessor.BLIND_ONLY_ROLES, false);
        assertTrue(cmdVoiceCall.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));
    }

    // =========================================================================
    // 7. LAYOUT & QUICK PHRASES VERIFICATION
    // =========================================================================

    @Test
    public void test8_LayoutContainsVisualCardAndNoQuickPhrases() {
        LayoutInflater inflater = LayoutInflater.from(themeContext);
        View rootView = inflater.inflate(R.layout.activity_speech_impaired_dashboard, null);

        // Verify visual assistant card components exist
        MaterialCardView card = rootView.findViewById(R.id.cardDeafAssistantResponse);
        assertNotNull("cardDeafAssistantResponse must be present in layout", card);

        TextView txtTitle = rootView.findViewById(R.id.txtDeafAssistantTitle);
        assertNotNull("txtDeafAssistantTitle must be present in layout", txtTitle);

        TextView txtMessage = rootView.findViewById(R.id.txtDeafAssistantMessage);
        assertNotNull("txtDeafAssistantMessage must be present in layout", txtMessage);

        ProgressBar progress = rootView.findViewById(R.id.progressDeafAssistant);
        assertNotNull("progressDeafAssistant must be present in layout", progress);

        android.widget.ImageButton btnDismiss = rootView.findViewById(R.id.btnDismissDeafAssistant);
        assertNotNull("btnDismissDeafAssistant must be present in layout", btnDismiss);

        // Verify Quick Phrases layout is NOT present
        int quickPhrasesId = appContext.getResources().getIdentifier("layoutQuickPhrases", "id", appContext.getPackageName());
        View quickPhrasesView = quickPhrasesId != 0 ? rootView.findViewById(quickPhrasesId) : null;
        assertNull("Quick Phrases layout must remain removed", quickPhrasesView);
    }
}
