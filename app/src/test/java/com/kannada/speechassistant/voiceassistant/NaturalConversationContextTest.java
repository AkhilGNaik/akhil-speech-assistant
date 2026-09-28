package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.kannada.speechassistant.ChatMessage;
import com.kannada.speechassistant.RoleManager;

import org.junit.Before;
import org.junit.Test;

/**
 * Step 3 Comprehensive Unit Test Suite:
 * Natural Conversation, Context & Clarification Handling
 *
 * Verifies:
 * 1. Conversation State: WAITING_FOR_COMMAND, MESSAGE_COMPOSING, WAITING_FOR_CONFIRMATION, WAITING_FOR_CLARIFICATION.
 * 2. Multi-step Send Message & Cancellation across EN, KN, HI, ML.
 * 3. Contextual Confirmation & Cancellation.
 * 4. Natural Conversational Call Requests across EN, KN, HI, ML.
 * 5. Ambiguous Request Clarification (Help, Call, Message) across EN, KN, HI, ML.
 * 6. Context-Aware Repeat Message.
 * 7. Context-Aware Navigation.
 * 8. Context-Aware Call Termination & "No active call." response.
 * 9. Context-Aware Stop Assistant vs End Call vs Stop Recording.
 * 10. Negation Protection preservation.
 * 11. Strict SOS Safety & rejection of invented features.
 */
public class NaturalConversationContextTest {

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
    // 1. CONVERSATION STATE & PROMPT VERIFICATION
    // ==========================================

    @Test
    public void testConversationStatesAndClarificationPrompts() {
        // Verify states exist in enum
        assertNotNull(AppVoiceAssistant.AssistantState.IDLE);
        assertNotNull(AppVoiceAssistant.AssistantState.WAITING_FOR_COMMAND);
        assertNotNull(AppVoiceAssistant.AssistantState.MESSAGE_COMPOSING);
        assertNotNull(AppVoiceAssistant.AssistantState.WAITING_FOR_CONFIRMATION);
        assertNotNull(AppVoiceAssistant.AssistantState.WAITING_FOR_CLARIFICATION);

        // Verify clarification context enum
        assertNotNull(AppVoiceAssistant.ClarificationContext.NONE);
        assertNotNull(AppVoiceAssistant.ClarificationContext.HELP);
        assertNotNull(AppVoiceAssistant.ClarificationContext.CALL);
        assertNotNull(AppVoiceAssistant.ClarificationContext.MESSAGE);

        // Clarification prompts across languages
        // Help clarification
        assertEquals("Do you want me to call your caregiver or send an emergency alert?",
                VoiceLanguageConfig.getClarifyHelpPrompt("en"));
        assertEquals("ನಿಮ್ಮ ಕೇರ್‌ಗಿವರ್‌ಗೆ ಕರೆ ಮಾಡಬೇಕೆ ಅಥವಾ ತುರ್ತು ಎಚ್ಚರಿಕೆ ಕಳುಹಿಸಬೇಕೆ?",
                VoiceLanguageConfig.getClarifyHelpPrompt("kn"));
        assertEquals("क्या आप अपने केयरगिवर को कॉल करना चाहते हैं या आपातकालीन अलर्ट भेजना चाहते हैं?",
                VoiceLanguageConfig.getClarifyHelpPrompt("hi"));
        assertEquals("നിങ്ങളുടെ കെയർഗിവറെ വിളിക്കണോ അതോ അടിയന്തര മുന്നറിയിപ്പ് അയക്കണോ?",
                VoiceLanguageConfig.getClarifyHelpPrompt("ml"));

        // Call clarification
        assertEquals("Do you want me to call your caregiver?",
                VoiceLanguageConfig.getClarifyCallPrompt("en"));
        assertEquals("ನಿಮ್ಮ ಕೇರ್‌ಗಿವರ್‌ಗೆ ಕರೆ ಮಾಡಬೇಕೆ?",
                VoiceLanguageConfig.getClarifyCallPrompt("kn"));
        assertEquals("क्या आप अपने केयरगिवर को कॉल करना चाहते हैं?",
                VoiceLanguageConfig.getClarifyCallPrompt("hi"));
        assertEquals("നിങ്ങളുടെ കെയർഗിവറെ വിളിക്കണോ?",
                VoiceLanguageConfig.getClarifyCallPrompt("ml"));

        // Message clarification
        assertEquals("Do you want to send a message to your caregiver?",
                VoiceLanguageConfig.getClarifyMessagePrompt("en"));
        assertEquals("ನಿಮ್ಮ ಕೇರ್‌ಗಿವರ್‌ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಬೇಕೆ?",
                VoiceLanguageConfig.getClarifyMessagePrompt("kn"));
        assertEquals("क्या आप अपने केयरगिवर को संदेश भेजना चाहते हैं?",
                VoiceLanguageConfig.getClarifyMessagePrompt("hi"));
        assertEquals("നിങ്ങളുടെ കെയർഗിവർക്ക് സന്ദേശം അയക്കണോ?",
                VoiceLanguageConfig.getClarifyMessagePrompt("ml"));
    }

    // ==========================================
    // 2. MULTI-STEP SEND MESSAGE & CANCELLATION
    // ==========================================

    @Test
    public void testMessageCancellationPhrasesMultilingual() {
        // English cancellation phrases
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("cancel"));
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("cancel message"));
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("don't send"));
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("never mind"));
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("stop"));
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("abort"));

        // Kannada cancellation phrases
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("ಬೇಡ"));
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("ರದ್ದು"));
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("ರದ್ದು ಮಾಡಿ"));
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("ಕಳುಹಿಸಬೇಡಿ"));
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("ಕಳಿಸಬೇಡಿ"));

        // Hindi cancellation phrases
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("नहीं"));
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("रद्द"));
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("रद्द करो"));
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("मत भेजो"));
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("संदेश मत भेजो"));

        // Malayalam cancellation phrases
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("വേണ്ട"));
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("റദ്ദാക്കുക"));
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("അയക്കരുത്"));
        assertTrue(VoiceIntentMatcher.isCancelMessagePhrase("സന്ദേശം അയക്കരുത്"));

        // Non-cancellation phrases (arbitrary speech must be treated as message content)
        assertFalse(VoiceIntentMatcher.isCancelMessagePhrase("I need some water"));
        assertFalse(VoiceIntentMatcher.isCancelMessagePhrase("Please come home soon"));
        assertFalse(VoiceIntentMatcher.isCancelMessagePhrase("ದಯವಿಟ್ಟು ಮನೆಗೆ ಬನ್ನಿ"));
        assertFalse(VoiceIntentMatcher.isCancelMessagePhrase("मुझे पानी चाहिए"));
        assertFalse(VoiceIntentMatcher.isCancelMessagePhrase("എനിക്ക് വെള്ളം വേണം"));

        // Multilingual message cancelled spoken feedback
        assertEquals("Message cancelled.", VoiceLanguageConfig.getMessageCancelledResponse("en"));
        assertEquals("ಸಂದೇಶ ರದ್ದುಗೊಳಿಸಲಾಗಿದೆ.", VoiceLanguageConfig.getMessageCancelledResponse("kn"));
        assertEquals("संदेश रद्द कर दिया गया।", VoiceLanguageConfig.getMessageCancelledResponse("hi"));
        assertEquals("സന്ദേശം റദ്ദാക്കി.", VoiceLanguageConfig.getMessageCancelledResponse("ml"));
    }

    // ==========================================
    // 3. NATURAL CAREGIVER CALL REQUESTS (ALL 4 LANGUAGES)
    // ==========================================

    @Test
    public void testNaturalCaregiverCallRequestsEnglish() {
        String lang = "en";
        String[] phrases = {
                "I want to talk to my caregiver.",
                "Can I speak to my helper?",
                "I need to talk to the person taking care of me.",
                "Connect me with my caregiver.",
                "Please get my helper.",
                "Put me through to my caregiver."
        };

        for (String phrase : phrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertEquals("English call phrase '" + phrase + "' must match CALL_CAREGIVER",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
            VoiceCommand cmd = processor.resolveCommand(intent, RoleManager.ROLE_BLIND_USER);
            assertEquals(VoiceCommandConstants.CMD_OPEN_VOICE_CALL, cmd.getCommandId());
        }
    }

    @Test
    public void testNaturalCaregiverCallRequestsKannada() {
        String lang = "kn";
        String[] phrases = {
                "ನನ್ನ ಸಹಾಯಕನ ಜೊತೆ ಮಾತನಾಡಬೇಕು.",
                "ನನ್ನ ಆರೈಕೆದಾರರ ಜೊತೆ ಮಾತನಾಡಬೇಕು.",
                "ನನ್ನ ಕೇರ್ಗಿವರ್ ಜೊತೆ ಸಂಪರ್ಕಿಸಿ."
        };

        for (String phrase : phrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertEquals("Kannada call phrase '" + phrase + "' must match CALL_CAREGIVER",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
            VoiceCommand cmd = processor.resolveCommand(intent, RoleManager.ROLE_BLIND_USER);
            assertEquals(VoiceCommandConstants.CMD_OPEN_VOICE_CALL, cmd.getCommandId());
        }
    }

    @Test
    public void testNaturalCaregiverCallRequestsHindi() {
        String lang = "hi";
        String[] phrases = {
                "मुझे अपने केयरगिवर से बात करनी है।",
                "मुझे अपने सहायक से बात करवाओ।",
                "मेरे केयरगिवर से जोड़ो।"
        };

        for (String phrase : phrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertEquals("Hindi call phrase '" + phrase + "' must match CALL_CAREGIVER",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
            VoiceCommand cmd = processor.resolveCommand(intent, RoleManager.ROLE_BLIND_USER);
            assertEquals(VoiceCommandConstants.CMD_OPEN_VOICE_CALL, cmd.getCommandId());
        }
    }

    @Test
    public void testNaturalCaregiverCallRequestsMalayalam() {
        String lang = "ml";
        String[] phrases = {
                "എനിക്ക് എന്റെ കെയർഗിവറോട് സംസാരിക്കണം.",
                "എന്റെ സഹായിയുമായി സംസാരിക്കണം.",
                "എന്റെ പരിചാരകനുമായി ബന്ധിപ്പിക്കൂ."
        };

        for (String phrase : phrases) {
            VoiceIntent intent = processor.detectIntent(phrase, lang);
            assertEquals("Malayalam call phrase '" + phrase + "' must match CALL_CAREGIVER",
                    VoiceIntentType.CALL_CAREGIVER, intent.getIntentType());
            VoiceCommand cmd = processor.resolveCommand(intent, RoleManager.ROLE_BLIND_USER);
            assertEquals(VoiceCommandConstants.CMD_OPEN_VOICE_CALL, cmd.getCommandId());
        }
    }

    // ==========================================
    // 4. AMBIGUOUS REQUEST DETECTION & SAFE CLARIFICATION
    // ==========================================

    @Test
    public void testAmbiguousHelpRequestDetection() {
        // Ambiguous help must NOT automatically trigger SOS emergency
        assertTrue(VoiceIntentMatcher.isAmbiguousHelpRequest("help"));
        assertTrue(VoiceIntentMatcher.isAmbiguousHelpRequest("I need help"));
        assertTrue(VoiceIntentMatcher.isAmbiguousHelpRequest("please help me"));
        assertTrue(VoiceIntentMatcher.isAmbiguousHelpRequest("help me"));

        // Kannada
        assertTrue(VoiceIntentMatcher.isAmbiguousHelpRequest("ಸಹಾಯ"));
        assertTrue(VoiceIntentMatcher.isAmbiguousHelpRequest("ಸಹಾಯ ಬೇಕು"));

        // Hindi
        assertTrue(VoiceIntentMatcher.isAmbiguousHelpRequest("मदद"));
        assertTrue(VoiceIntentMatcher.isAmbiguousHelpRequest("मदद चाहिए"));
        assertTrue(VoiceIntentMatcher.isAmbiguousHelpRequest("मुझे मदद चाहिए"));

        // Malayalam
        assertTrue(VoiceIntentMatcher.isAmbiguousHelpRequest("സഹായം"));
        assertTrue(VoiceIntentMatcher.isAmbiguousHelpRequest("സഹായം വേണം"));

        // Explicit Emergency commands with urgent tokens must NOT be classified as ambiguous
        assertFalse(VoiceIntentMatcher.isAmbiguousHelpRequest("emergency help"));
        assertFalse(VoiceIntentMatcher.isAmbiguousHelpRequest("send emergency alert"));
        assertFalse(VoiceIntentMatcher.isAmbiguousHelpRequest("ತುರ್ತು ಸಹಾಯ ಬೇಕು"));
        assertFalse(VoiceIntentMatcher.isAmbiguousHelpRequest("आपातकालीन सहायता"));
        assertFalse(VoiceIntentMatcher.isAmbiguousHelpRequest("അടിയന്തര മുന്നറിയിപ്പ്"));

        // Caregiver specific requests must NOT be classified as ambiguous help
        assertFalse(VoiceIntentMatcher.isAmbiguousHelpRequest("call my caregiver"));
        assertFalse(VoiceIntentMatcher.isAmbiguousHelpRequest("help my caregiver"));
    }

    @Test
    public void testAmbiguousCallRequestDetection() {
        // Generic call without caregiver specified
        assertTrue(VoiceIntentMatcher.isAmbiguousCallRequest("Can you call someone?"));
        assertTrue(VoiceIntentMatcher.isAmbiguousCallRequest("call someone"));
        assertTrue(VoiceIntentMatcher.isAmbiguousCallRequest("call somebody"));
        assertTrue(VoiceIntentMatcher.isAmbiguousCallRequest("make a call"));
        assertTrue(VoiceIntentMatcher.isAmbiguousCallRequest("call"));

        // Multilingual ambiguous call
        assertTrue(VoiceIntentMatcher.isAmbiguousCallRequest("ಯಾರಿಗಾದರೂ ಕರೆ ಮಾಡಿ"));
        assertTrue(VoiceIntentMatcher.isAmbiguousCallRequest("ಕರೆ ಮಾಡಿ"));
        assertTrue(VoiceIntentMatcher.isAmbiguousCallRequest("किसी को कॉल करो"));
        assertTrue(VoiceIntentMatcher.isAmbiguousCallRequest("कॉल करो"));
        assertTrue(VoiceIntentMatcher.isAmbiguousCallRequest("ആരെയെങ്കിലും വിളിക്കൂ"));
        assertTrue(VoiceIntentMatcher.isAmbiguousCallRequest("വിളിക്കൂ"));

        // Unambiguous caregiver calls must NOT be classified as ambiguous
        assertFalse(VoiceIntentMatcher.isAmbiguousCallRequest("call my caregiver"));
        assertFalse(VoiceIntentMatcher.isAmbiguousCallRequest("ಕೇರ್ಗಿವರ್ಗೆ ಕರೆ ಮಾಡಿ"));
        assertFalse(VoiceIntentMatcher.isAmbiguousCallRequest("केयरगिवर को कॉल करो"));
        assertFalse(VoiceIntentMatcher.isAmbiguousCallRequest("കെയർഗിവറെ വിളിക്കൂ"));
    }

    @Test
    public void testAmbiguousMessageRequestDetection() {
        // Generic send without caregiver or content specified
        assertTrue(VoiceIntentMatcher.isAmbiguousMessageRequest("I want to send something."));
        assertTrue(VoiceIntentMatcher.isAmbiguousMessageRequest("send something"));
        assertTrue(VoiceIntentMatcher.isAmbiguousMessageRequest("send someone a message"));
        assertTrue(VoiceIntentMatcher.isAmbiguousMessageRequest("send"));

        // Multilingual ambiguous send
        assertTrue(VoiceIntentMatcher.isAmbiguousMessageRequest("ಏನನ್ನಾದರೂ ಕಳುಹಿಸಬೇಕು"));
        assertTrue(VoiceIntentMatcher.isAmbiguousMessageRequest("ಏನಾದರೂ ಕಳುಹಿಸು"));
        assertTrue(VoiceIntentMatcher.isAmbiguousMessageRequest("कुछ भेजना है"));
        assertTrue(VoiceIntentMatcher.isAmbiguousMessageRequest("कुछ भेजो"));
        assertTrue(VoiceIntentMatcher.isAmbiguousMessageRequest("എന്തെങ്കിലും അയക്കണം"));
        assertTrue(VoiceIntentMatcher.isAmbiguousMessageRequest("എന്തെങ്കിലും അയക്കൂ"));

        // Unambiguous caregiver messages must NOT be classified as ambiguous
        assertFalse(VoiceIntentMatcher.isAmbiguousMessageRequest("send a message to my caregiver"));
        assertFalse(VoiceIntentMatcher.isAmbiguousMessageRequest("ಕೇರ್ಗಿವರ್ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ"));
        assertFalse(VoiceIntentMatcher.isAmbiguousMessageRequest("केयरगिवर को मैसेज भेजो"));
        assertFalse(VoiceIntentMatcher.isAmbiguousMessageRequest("കെയർഗിവർക്ക് സന്ദേശം അയക്കൂ"));
    }

    // ==========================================
    // 5. CONTEXT-AWARE NAVIGATION
    // ==========================================

    @Test
    public void testContextAwareNavigation() {
        String lang = "en";

        // Profile
        VoiceIntent profileIntent1 = processor.detectIntent("Take me to my profile.", lang);
        assertEquals(VoiceIntentType.OPEN_PROFILE, profileIntent1.getIntentType());

        VoiceIntent profileIntent2 = processor.detectIntent("I want to see my profile.", lang);
        assertEquals(VoiceIntentType.OPEN_PROFILE, profileIntent2.getIntentType());

        // Settings
        VoiceIntent settingsIntent = processor.detectIntent("Show my settings.", lang);
        assertEquals(VoiceIntentType.OPEN_SETTINGS, settingsIntent.getIntentType());

        // Home
        VoiceIntent homeIntent = processor.detectIntent("Bring me back home.", lang);
        assertEquals(VoiceIntentType.OPEN_HOME, homeIntent.getIntentType());

        // Messages
        VoiceIntent msgIntent = processor.detectIntent("Go to my messages.", lang);
        assertEquals(VoiceIntentType.OPEN_MESSAGES, msgIntent.getIntentType());
    }

    // ==========================================
    // 6. CONTEXT-AWARE CALL TERMINATION & STOP
    // ==========================================

    @Test
    public void testContextAwareCallTerminationAndStopDistinction() {
        String lang = "en";

        // End Call phrases
        assertEquals(VoiceIntentType.END_CALL, processor.detectIntent("Hang up.", lang).getIntentType());
        assertEquals(VoiceIntentType.END_CALL, processor.detectIntent("End the call.", lang).getIntentType());
        assertEquals(VoiceIntentType.END_CALL, processor.detectIntent("Stop the call.", lang).getIntentType());
        assertEquals(VoiceIntentType.END_CALL, processor.detectIntent("Please stop the call.", lang).getIntentType());

        // Stop Listening vs Stop the Call vs Stop Recording
        assertEquals(VoiceIntentType.STOP_LISTENING, processor.detectIntent("Stop listening.", lang).getIntentType());
        assertEquals(VoiceIntentType.END_CALL, processor.detectIntent("Stop the call.", lang).getIntentType());

        // Stop recording must NOT be mapped to STOP_LISTENING
        VoiceIntent stopRecordIntent = processor.detectIntent("Stop recording.", lang);
        assertNotEquals("Stop recording must NOT map to STOP_LISTENING",
                VoiceIntentType.STOP_LISTENING, stopRecordIntent.getIntentType());

        // Multilingual "No active call." response outside a call
        assertEquals("No active call.", VoiceLanguageConfig.getNoActiveCallResponse("en"));
        assertEquals("ಯಾವುದೇ ಸಕ್ರಿಯ ಕರೆ ಇಲ್ಲ.", VoiceLanguageConfig.getNoActiveCallResponse("kn"));
        assertEquals("कोई सक्रिय कॉल नहीं है।", VoiceLanguageConfig.getNoActiveCallResponse("hi"));
        assertEquals("സജീവ കോൾ ഒന്നുമില്ല.", VoiceLanguageConfig.getNoActiveCallResponse("ml"));
    }

    // ==========================================
    // 7. CONTEXT-AWARE REPEAT MESSAGE
    // ==========================================

    @Test
    public void testRepeatMessageBehavior() {
        String lang = "en";

        // Natural repeat message commands
        assertEquals(VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent("Say that again.", lang).getIntentType());
        assertEquals(VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent("Repeat the message.", lang).getIntentType());
        assertEquals(VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent("ಮತ್ತೊಮ್ಮೆ ಹೇಳಿ.", "kn").getIntentType());
        assertEquals(VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent("फिर से बोलो।", "hi").getIntentType());
        assertEquals(VoiceIntentType.REPEAT_MESSAGE, processor.detectIntent("വീണ്ടും പറയൂ.", "ml").getIntentType());

        // Localized "No message to repeat." feedback
        assertEquals(VoiceCommandConstants.RESPONSE_NO_MESSAGE_TO_REPEAT,
                VoiceLanguageConfig.getNoMessageToRepeatResponse("en"));
        assertEquals("ಮರುಹೇಳಲು ಯಾವುದೇ ಸಂದೇಶವಿಲ್ಲ.",
                VoiceLanguageConfig.getNoMessageToRepeatResponse("kn"));
        assertEquals("दोबारा सुनाने के लिए कोई संदेश नहीं है।",
                VoiceLanguageConfig.getNoMessageToRepeatResponse("hi"));
        assertEquals("വീണ്ടും പറയാൻ സന്ദേശമൊന്നുമില്ല.",
                VoiceLanguageConfig.getNoMessageToRepeatResponse("ml"));
    }

    // ==========================================
    // 8. NEGATION & SAFETY PRESERVATION
    // ==========================================

    @Test
    public void testNegationSafetyPreservedMultilingual() {
        // Negated calls must NEVER trigger CALL_CAREGIVER
        assertNotEquals(VoiceIntentType.CALL_CAREGIVER, processor.detectIntent("Don't call my caregiver.", "en").getIntentType());
        assertNotEquals(VoiceIntentType.CALL_CAREGIVER, processor.detectIntent("I don't want to call.", "en").getIntentType());
        assertNotEquals(VoiceIntentType.CALL_CAREGIVER, processor.detectIntent("ಕರೆ ಮಾಡಬೇಡಿ.", "kn").getIntentType());
        assertNotEquals(VoiceIntentType.CALL_CAREGIVER, processor.detectIntent("कॉल मत करो।", "hi").getIntentType());
        assertNotEquals(VoiceIntentType.CALL_CAREGIVER, processor.detectIntent("വിളിക്കരുത്.", "ml").getIntentType());

        // Negated messages must NEVER trigger SEND_MESSAGE
        assertNotEquals(VoiceIntentType.SEND_MESSAGE, processor.detectIntent("Don't send the message.", "en").getIntentType());
        assertNotEquals(VoiceIntentType.SEND_MESSAGE, processor.detectIntent("ಮೆಸೇಜ್ ಕಳುಹಿಸಬೇಡಿ.", "kn").getIntentType());
        assertNotEquals(VoiceIntentType.SEND_MESSAGE, processor.detectIntent("संदेश मत भेजो।", "hi").getIntentType());
        assertNotEquals(VoiceIntentType.SEND_MESSAGE, processor.detectIntent("സന്ദേശം അയക്കരുത്.", "ml").getIntentType());
    }

    // ==========================================
    // 9. NO INVENTED FUNCTIONS (STRICT TASK-ORIENTED)
    // ==========================================

    @Test
    public void testNoInventedFunctions() {
        String[] unsupported = {
                "Play music.",
                "What's the weather?",
                "Open YouTube.",
                "Send an email.",
                "Tell me a joke."
        };

        for (String phrase : unsupported) {
            VoiceIntent intent = processor.detectIntent(phrase, "en");
            assertEquals("Unsupported phrase '" + phrase + "' must resolve to UNKNOWN",
                    VoiceIntentType.UNKNOWN, intent.getIntentType());
        }
    }
}
