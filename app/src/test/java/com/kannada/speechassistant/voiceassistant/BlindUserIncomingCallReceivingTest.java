package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;

import com.kannada.speechassistant.RoleManager;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests verifying:
 * 1. Blind User incoming call receiving command detection across English, Kannada, Hindi, and Malayalam.
 * 2. Natural variation handling (e.g. "accept call", "receive call", "answer call", "pick up", "ಕರೆ ಸ್ವೀಕರಿಸಿ", "ಕಾಲ್ ರಿಸೀವ್ ಮಾಡಿ", "ಕರೆ ಎತ್ತಿ").
 * 3. Wake-word prefixed incoming call receiving commands ("Hey Assistant, accept call", "ಅಸಿಸ್ಟೆಂಟ್, ಕರೆ ಸ್ವೀಕರಿಸಿ").
 * 4. Proper resolution to VoiceCommandConstants.CMD_ACCEPT_CALL and VoiceIntentType.ACCEPT_CALL.
 * 5. Accurate multilingual spoken response resolution for Blind Users.
 * 6. Disambiguation ensuring no false positive conflicts with CALL_CAREGIVER or END_CALL.
 */
public class BlindUserIncomingCallReceivingTest {

    private VoiceCommandProcessor processor;

    private static class DummyContext extends ContextWrapper {
        public DummyContext() {
            super(null);
        }

        @Override
        public Context getApplicationContext() {
            return this;
        }
    }

    @Before
    public void setUp() {
        processor = new VoiceCommandProcessor(new DummyContext());
    }

    @Test
    public void testEnglishIncomingCallReceivingCommands() {
        assertAcceptCallIntent("accept call", "en");
        assertAcceptCallIntent("receive call", "en");
        assertAcceptCallIntent("answer call", "en");
        assertAcceptCallIntent("pick up call", "en");
        assertAcceptCallIntent("pick up", "en");
        assertAcceptCallIntent("pickup", "en");
        assertAcceptCallIntent("answer the call", "en");
        assertAcceptCallIntent("receive the call", "en");
        assertAcceptCallIntent("accept the call", "en");
        assertAcceptCallIntent("pick up the call", "en");
        assertAcceptCallIntent("pick up the phone", "en");
        assertAcceptCallIntent("answer the phone", "en");
        assertAcceptCallIntent("attend call", "en");
        assertAcceptCallIntent("attend the call", "en");
        assertAcceptCallIntent("take call", "en");
        assertAcceptCallIntent("take the call", "en");
        assertAcceptCallIntent("receive incoming call", "en");
        assertAcceptCallIntent("accept incoming call", "en");
        assertAcceptCallIntent("answer incoming call", "en");
        assertAcceptCallIntent("pick up incoming call", "en");
        assertAcceptCallIntent("receive caregiver call", "en");
        assertAcceptCallIntent("accept caregiver call", "en");
        assertAcceptCallIntent("connect incoming call", "en");
    }

    @Test
    public void testEnglishWakeWordIncomingCallCommands() {
        assertAcceptCallIntent("Assistant, accept call", "en");
        assertAcceptCallIntent("Hey Assistant, accept call", "en");
        assertAcceptCallIntent("Assistant, receive call", "en");
        assertAcceptCallIntent("Hey Assistant, receive call", "en");
        assertAcceptCallIntent("Assistant, answer call", "en");
        assertAcceptCallIntent("Assistant, pick up", "en");
        assertAcceptCallIntent("Hey Assistant, pick up the call", "en");
    }

    @Test
    public void testKannadaIncomingCallReceivingCommands() {
        assertAcceptCallIntent("ಕರೆ ಸ್ವೀಕರಿಸಿ", "kn");
        assertAcceptCallIntent("ಕರೆ ಸ್ವೀಕರಿಸು", "kn");
        assertAcceptCallIntent("ಕರೆಯನ್ನು ಸ್ವೀಕರಿಸಿ", "kn");
        assertAcceptCallIntent("ಕರೆಯನ್ನು ಸ್ವೀಕರಿಸು", "kn");
        assertAcceptCallIntent("ಕಾಲ್ ಸ್ವೀಕರಿಸಿ", "kn");
        assertAcceptCallIntent("ಕಾಲ್ ರಿಸೀವ್ ಮಾಡಿ", "kn");
        assertAcceptCallIntent("ಕರೆ ರಿಸೀವ್ ಮಾಡಿ", "kn");
        assertAcceptCallIntent("ಕರೆಯನ್ನು ರಿಸೀವ್ ಮಾಡಿ", "kn");
        assertAcceptCallIntent("ಫೋನ್ ಸ್ವೀಕರಿಸಿ", "kn");
        assertAcceptCallIntent("ಫೋನ್ ರಿಸೀವ್ ಮಾಡಿ", "kn");
        assertAcceptCallIntent("ಕರೆ ಎತ್ತಿ", "kn");
        assertAcceptCallIntent("ಫೋನ್ ಎತ್ತಿ", "kn");
        assertAcceptCallIntent("ಕಾಲ್ ಎತ್ತಿ", "kn");
        assertAcceptCallIntent("ಕರೆಯನ್ನು ಎತ್ತಿ", "kn");
        assertAcceptCallIntent("ಸ್ವೀಕರಿಸಿ", "kn");
        assertAcceptCallIntent("ರಿಸೀವ್ ಮಾಡಿ", "kn");
        assertAcceptCallIntent("ಕರೆ ತಗೆದುಕೊಳ್ಳಿ", "kn");
        assertAcceptCallIntent("ಕಾಲ್ ಅಕ್ಸೆಪ್ಟ್ ಮಾಡಿ", "kn");
        assertAcceptCallIntent("ಬರುತ್ತಿರುವ ಕರೆ ಸ್ವೀಕರಿಸಿ", "kn");
        assertAcceptCallIntent("ಕೇರ್ಗಿವರ್ ಕರೆ ಸ್ವೀಕರಿಸಿ", "kn");
        assertAcceptCallIntent("ಕೇರ್ಗಿವರ್ ಕಾಲ್ ರಿಸೀವ್ ಮಾಡಿ", "kn");
    }

    @Test
    public void testKannadaWakeWordIncomingCallCommands() {
        assertAcceptCallIntent("ಅಸಿಸ್ಟೆಂಟ್, ಕರೆ ಸ್ವೀಕರಿಸಿ", "kn");
        assertAcceptCallIntent("ಹೇ ಅಸಿಸ್ಟೆಂಟ್, ಕರೆ ಸ್ವೀಕರಿಸಿ", "kn");
        assertAcceptCallIntent("ಅಸಿಸ್ಟೆಂಟ್, ಕಾಲ್ ರಿಸೀವ್ ಮಾಡಿ", "kn");
        assertAcceptCallIntent("ಹೇ ಅಸಿಸ್ಟೆಂಟ್, ಕರೆ ಎತ್ತಿ", "kn");
        assertAcceptCallIntent("ಅಸಿಸ್ಟೆಂಟ್, ಸ್ವೀಕರಿಸಿ", "kn");
        assertAcceptCallIntent("Assistant, ಕರೆ ಸ್ವೀಕರಿಸಿ", "kn");
    }

    @Test
    public void testHindiIncomingCallReceivingCommands() {
        assertAcceptCallIntent("कॉल उठाओ", "hi");
        assertAcceptCallIntent("कॉल रिसीव करो", "hi");
        assertAcceptCallIntent("फोन उठाओ", "hi");
        assertAcceptCallIntent("कॉल स्वीकार करो", "hi");
        assertAcceptCallIntent("कॉल स्वीकार करें", "hi");
        assertAcceptCallIntent("कॉल उठाएं", "hi");
        assertAcceptCallIntent("फोन उठाइए", "hi");
        assertAcceptCallIntent("कॉल एक्सेप्ट करो", "hi");
        assertAcceptCallIntent("फोन रिसीव करो", "hi");
        assertAcceptCallIntent("आने वाली कॉल उठाओ", "hi");
        assertAcceptCallIntent("केयरगिवर की कॉल उठाओ", "hi");
        assertAcceptCallIntent("असिस्टेंट, कॉल उठाओ", "hi");
        assertAcceptCallIntent("हे असिस्टेंट, कॉल रिसीव करो", "hi");
    }

    @Test
    public void testMalayalamIncomingCallReceivingCommands() {
        assertAcceptCallIntent("കോൾ സ്വീകരിക്കുക", "ml");
        assertAcceptCallIntent("കോൾ എടുക്കുക", "ml");
        assertAcceptCallIntent("ഫോൺ എടുക്കുക", "ml");
        assertAcceptCallIntent("കോൾ റിസീവ് ചെയ്യുക", "ml");
        assertAcceptCallIntent("കോൾ അറ്റൻഡ് ചെയ്യുക", "ml");
        assertAcceptCallIntent("ഫോൺ സ്വീകരിക്കുക", "ml");
        assertAcceptCallIntent("കോൾ സ്വീകരിക്കൂ", "ml");
        assertAcceptCallIntent("ഫോൺ എടുക്കൂ", "ml");
        assertAcceptCallIntent("കോൾ എടുക്കൂ", "ml");
        assertAcceptCallIntent("അസിസ്റ്റന്റ്, കോൾ സ്വീകരിക്കുക", "ml");
        assertAcceptCallIntent("ഹേ അസിസ്റ്റന്റ്, കോൾ എടുക്കുക", "ml");
    }

    @Test
    public void testCommandResolutionAndBlindUserRoleAuthorization() {
        VoiceIntent intent = VoiceIntentMatcher.matchIntent("accept call", "en");
        assertEquals(VoiceIntentType.ACCEPT_CALL, intent.getIntentType());

        VoiceCommand command = processor.resolveCommand(intent, RoleManager.ROLE_BLIND_USER);
        assertNotNull(command);
        assertEquals(VoiceCommandConstants.CMD_ACCEPT_CALL, command.getCommandId());
        assertTrue(command.isRoleAuthorized(RoleManager.ROLE_BLIND_USER));

        // Spoken response validation
        String englishResponse = VoiceCommandConstants.RESPONSE_ACCEPT_CALL;
        assertEquals("Receiving call.", englishResponse);

        String blindSpoken = AppVoiceAssistant.getBlindUserSpokenResponse(VoiceCommandConstants.CMD_ACCEPT_CALL, englishResponse, "en");
        assertEquals("Receiving call.", blindSpoken);

        // Multilingual localized responses
        String kannadaResponse = VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_ACCEPT_CALL, englishResponse, "kn");
        assertEquals("ಕರೆ ಸ್ವೀಕರಿಸಲಾಗುತ್ತಿದೆ.", kannadaResponse);

        String hindiResponse = VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_ACCEPT_CALL, englishResponse, "hi");
        assertEquals("कॉल रिसीव की जा रही है।", hindiResponse);

        String malayalamResponse = VoiceLanguageConfig.getLocalizedResponse(VoiceCommandConstants.CMD_ACCEPT_CALL, englishResponse, "ml");
        assertEquals("കോൾ സ്വീകരിക്കുന്നു.", malayalamResponse);
    }

    @Test
    public void testDisambiguationWithOtherCallIntents() {
        // CALL_CAREGIVER must NOT match ACCEPT_CALL
        VoiceIntent callIntent = VoiceIntentMatcher.matchIntent("call my caregiver", "en");
        assertEquals(VoiceIntentType.CALL_CAREGIVER, callIntent.getIntentType());
        assertNotEquals(VoiceIntentType.ACCEPT_CALL, callIntent.getIntentType());

        VoiceIntent knCallIntent = VoiceIntentMatcher.matchIntent("ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ", "kn");
        assertEquals(VoiceIntentType.CALL_CAREGIVER, knCallIntent.getIntentType());
        assertNotEquals(VoiceIntentType.ACCEPT_CALL, knCallIntent.getIntentType());

        // END_CALL must NOT match ACCEPT_CALL
        VoiceIntent endIntent = VoiceIntentMatcher.matchIntent("end call", "en");
        assertEquals(VoiceIntentType.END_CALL, endIntent.getIntentType());
        assertNotEquals(VoiceIntentType.ACCEPT_CALL, endIntent.getIntentType());

        VoiceIntent knEndIntent = VoiceIntentMatcher.matchIntent("ಕರೆ ಮುಗಿಸಿ", "kn");
        assertEquals(VoiceIntentType.END_CALL, knEndIntent.getIntentType());
        assertNotEquals(VoiceIntentType.ACCEPT_CALL, knEndIntent.getIntentType());

        // Lone "call" or "phone" must not trigger ACCEPT_CALL
        VoiceIntent loneCall = VoiceIntentMatcher.matchIntent("call", "en");
        assertNotEquals(VoiceIntentType.ACCEPT_CALL, loneCall.getIntentType());

        VoiceIntent loneKnCall = VoiceIntentMatcher.matchIntent("ಕರೆ", "kn");
        assertNotEquals(VoiceIntentType.ACCEPT_CALL, loneKnCall.getIntentType());
    }

    private void assertAcceptCallIntent(String query, String langCode) {
        VoiceIntent intent = VoiceIntentMatcher.matchIntent(query, langCode);
        assertNotNull("Intent should not be null for query: " + query, intent);
        assertEquals("Expected ACCEPT_CALL for query: '" + query + "' (" + langCode + ")",
                VoiceIntentType.ACCEPT_CALL, intent.getIntentType());
    }
}
