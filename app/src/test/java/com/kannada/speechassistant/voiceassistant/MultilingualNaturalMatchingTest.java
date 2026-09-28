package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * Unit tests verifying natural multilingual intent matching across English, Kannada, Hindi, and Malayalam,
 * including caregiver synonyms, action+entity combinations, single-word ambiguity, negation protection,
 * and conversational non-action filtering.
 */
public class MultilingualNaturalMatchingTest {

    // ==========================================
    // 1. ENGLISH NATURAL COMMANDS (>= 10 tests)
    // ==========================================
    @Test
    public void testEnglishNaturalCommands() {
        // Section 18 mandatory examples
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("Call my caregiver."));
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("Please call my helper."));
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("Connect me with my caregiver."));

        // Additional natural English commands
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("Can you call my caregiver?"));
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("Phone my attendant."));
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("I want to talk to my caregiver."));
        assertEquals(VoiceIntentType.END_CALL, VoiceIntentMatcher.match("Disconnect the phone call."));
        assertEquals(VoiceIntentType.SEND_MESSAGE, VoiceIntentMatcher.match("Send a text message."));
        assertEquals(VoiceIntentType.READ_MESSAGES, VoiceIntentMatcher.match("Read out my new messages."));
        assertEquals(VoiceIntentType.MESSAGE_COUNT, VoiceIntentMatcher.match("How many messages do I have?"));
        assertEquals(VoiceIntentType.REPEAT_MESSAGE, VoiceIntentMatcher.match("Can you repeat the last message?"));
        assertEquals(VoiceIntentType.READ_NOTIFICATIONS, VoiceIntentMatcher.match("Read out all my notifications."));
        assertEquals(VoiceIntentType.OPEN_EMERGENCY, VoiceIntentMatcher.match("Help me this is an emergency!"));
        assertEquals(VoiceIntentType.OPEN_HOME, VoiceIntentMatcher.match("Take me back to the main home screen."));
        assertEquals(VoiceIntentType.OPEN_PROFILE, VoiceIntentMatcher.match("Open my user profile."));
        assertEquals(VoiceIntentType.OPEN_SETTINGS, VoiceIntentMatcher.match("Go into app settings."));
        assertEquals(VoiceIntentType.GO_BACK, VoiceIntentMatcher.match("Go back to the previous screen."));
        assertEquals(VoiceIntentType.STOP_LISTENING, VoiceIntentMatcher.match("Pause voice assistant."));
        assertEquals(VoiceIntentType.START_LISTENING, VoiceIntentMatcher.match("Resume voice assistant."));
    }

    // ==========================================
    // 2. KANNADA NATURAL COMMANDS (>= 10 tests)
    // ==========================================
    @Test
    public void testKannadaNaturalCommands() {
        // Section 18 mandatory examples
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ."));
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("ಕೇರ್ಗಿವರ್ಗೆ ಫೋನ್ ಮಾಡಿ."));

        // Additional natural Kannada commands
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ."));
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("ಆರೈಕೆದಾರರಿಗೆ ಫೋನ್ ಮಾಡಿ."));
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("ಪಾಲಕರಿಗೆ ಕರೆ ಮಾಡಿ."));
        assertEquals(VoiceIntentType.END_CALL, VoiceIntentMatcher.match("ಕರೆ ಕಟ್ ಮಾಡಿ."));
        assertEquals(VoiceIntentType.SEND_MESSAGE, VoiceIntentMatcher.match("ಸಂದೇಶ ಕಳುಹಿಸಿ."));
        assertEquals(VoiceIntentType.READ_MESSAGES, VoiceIntentMatcher.match("ಮೆಸೇಜ್ ಓದಿ."));
        assertEquals(VoiceIntentType.MESSAGE_COUNT, VoiceIntentMatcher.match("ಎಷ್ಟು ಮೆಸೇಜ್ ಬಂದಿದೆ?"));
        assertEquals(VoiceIntentType.REPEAT_MESSAGE, VoiceIntentMatcher.match("ಕೊನೆಯ ಮೆಸೇಜ್ ಪುನರಾವರ್ತಿಸಿ."));
        assertEquals(VoiceIntentType.READ_NOTIFICATIONS, VoiceIntentMatcher.match("ನನ್ನ ನೋಟಿಫಿಕೇಶನ್‌ಗಳನ್ನು ಓದಿ."));
        assertEquals(VoiceIntentType.OPEN_EMERGENCY, VoiceIntentMatcher.match("ತುರ್ತು ಪರಿಸ್ಥಿತಿ ಸಹಾಯ ಮಾಡಿ!"));
        assertEquals(VoiceIntentType.OPEN_HOME, VoiceIntentMatcher.match("ಹೋಮ್ ಸ್ಕ್ರೀನ್ ತೆರೆಯಿರಿ."));
        assertEquals(VoiceIntentType.OPEN_PROFILE, VoiceIntentMatcher.match("ನನ್ನ ಪ್ರೊಫೈಲ್ ತೋರಿಸಿ."));
        assertEquals(VoiceIntentType.OPEN_SETTINGS, VoiceIntentMatcher.match("ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಿರಿ."));
        assertEquals(VoiceIntentType.GO_BACK, VoiceIntentMatcher.match("ಹಿಂದೆ ಹೋಗಿ."));
    }

    // ==========================================
    // 3. HINDI NATURAL COMMANDS (>= 10 tests)
    // ==========================================
    @Test
    public void testHindiNaturalCommands() {
        // Section 18 mandatory examples
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("केयरगिवर को कॉल करो।"));
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("मेरे सहायक को फोन करो।"));

        // Additional natural Hindi commands
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("सहायक को कॉल लगाओ।"));
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("परिचारक से बात कराओ।"));
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("देखभालकर्ता को फोन मिलाओ।"));
        assertEquals(VoiceIntentType.END_CALL, VoiceIntentMatcher.match("कॉल समाप्त करो।"));
        assertEquals(VoiceIntentType.SEND_MESSAGE, VoiceIntentMatcher.match("संदेश भेजें।"));
        assertEquals(VoiceIntentType.READ_MESSAGES, VoiceIntentMatcher.match("मेरे संदेश पढ़कर सुनाओ।"));
        assertEquals(VoiceIntentType.MESSAGE_COUNT, VoiceIntentMatcher.match("कितने संदेश आए हैं?"));
        assertEquals(VoiceIntentType.REPEAT_MESSAGE, VoiceIntentMatcher.match("आखिरी संदेश दोबारा सुनाओ।"));
        assertEquals(VoiceIntentType.READ_NOTIFICATIONS, VoiceIntentMatcher.match("सभी सूचनाएं पढ़कर सुनाओ।"));
        assertEquals(VoiceIntentType.OPEN_EMERGENCY, VoiceIntentMatcher.match("आपातकालीन मदद चाहिए!"));
        assertEquals(VoiceIntentType.OPEN_HOME, VoiceIntentMatcher.match("होम स्क्रीन खोलें।"));
        assertEquals(VoiceIntentType.OPEN_PROFILE, VoiceIntentMatcher.match("मेरी प्रोफाइल दिखाओ।"));
        assertEquals(VoiceIntentType.OPEN_SETTINGS, VoiceIntentMatcher.match("सेटिंग्स में जाओ।"));
        assertEquals(VoiceIntentType.GO_BACK, VoiceIntentMatcher.match("पीछे जाएं।"));
    }

    // ==========================================
    // 4. MALAYALAM NATURAL COMMANDS (>= 10 tests)
    // ==========================================
    @Test
    public void testMalayalamNaturalCommands() {
        // Section 18 mandatory examples
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("കെയർഗിവറെ വിളിക്കൂ."));
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("എന്റെ സഹായിയെ വിളിക്കൂ."));

        // Additional natural Malayalam commands
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("പരിചാരകനെ ഫോൺ ചെയ്യുക."));
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("പരിചരണം നൽകുന്നയാളെ വിളിക്കൂ."));
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("സഹായകനെ വിളിക്കൂ."));
        assertEquals(VoiceIntentType.END_CALL, VoiceIntentMatcher.match("കോൾ അവസാനിപ്പിക്കുക."));
        assertEquals(VoiceIntentType.SEND_MESSAGE, VoiceIntentMatcher.match("സന്ദേശം അയക്കുക."));
        assertEquals(VoiceIntentType.READ_MESSAGES, VoiceIntentMatcher.match("സന്ദേശങ്ങൾ വായിക്കൂ."));
        assertEquals(VoiceIntentType.MESSAGE_COUNT, VoiceIntentMatcher.match("എത്ര സന്ദേശങ്ങൾ ഉണ്ട്?"));
        assertEquals(VoiceIntentType.REPEAT_MESSAGE, VoiceIntentMatcher.match("അവസാന സന്ദേശം വീണ്ടും പറയൂ."));
        assertEquals(VoiceIntentType.READ_NOTIFICATIONS, VoiceIntentMatcher.match("അറിയിപ്പുകൾ വായിക്കൂ."));
        assertEquals(VoiceIntentType.OPEN_EMERGENCY, VoiceIntentMatcher.match("അടിയന്തിര സഹായം വേണം!"));
        assertEquals(VoiceIntentType.OPEN_HOME, VoiceIntentMatcher.match("ഹോം സ്ക്രീൻ തുറക്കൂ."));
        assertEquals(VoiceIntentType.OPEN_PROFILE, VoiceIntentMatcher.match("എന്റെ പ്രൊഫൈൽ കാണിക്കൂ."));
        assertEquals(VoiceIntentType.OPEN_SETTINGS, VoiceIntentMatcher.match("ക്രമീകരണങ്ങൾ തുറക്കുക."));
        assertEquals(VoiceIntentType.GO_BACK, VoiceIntentMatcher.match("പിന്നിലേക്ക് പോകൂ."));
    }

    // ==========================================
    // 5. SINGLE-WORD AMBIGUITY SAFETY
    // ==========================================
    @Test
    public void testSingleWordAmbiguitySafety() {
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("help"));
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("call"));
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("send"));
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("message"));
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("assistant"));

        // Multilingual single word ambiguity
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("ಕರೆ"));
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("ಮೆಸೇಜ್"));
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("कॉल"));
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("संदेश"));
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("വിളിക്കൂ"));
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("സന്ദേശം"));
    }

    // ==========================================
    // 6. NEGATION PROTECTION
    // ==========================================
    @Test
    public void testNegationProtection() {
        // Must NOT trigger actions
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("Don't call my caregiver."));
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("Do not send the message."));
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("Don't read notifications."));

        // Kannada negation
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("ಕರೆ ಮಾಡಬೇಡಿ."));
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("ಮೆಸೇಜ್ ಕಳುಹಿಸಬೇಡಿ."));

        // Hindi negation
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("कॉल मत करो।"));
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("संदेश मत भेजो।"));

        // Malayalam negation
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("വിളിക്കരുത്."));
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("സന്ദേശം അയക്കരുത്."));
    }

    // ==========================================
    // 7. CONVERSATIONAL / UNRELATED PHRASE PROTECTION
    // ==========================================
    @Test
    public void testConversationalProtection() {
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("I talked to my caregiver yesterday."));
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("I had a chat with my helper yesterday."));
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("My profile is complete."));
        assertEquals(VoiceIntentType.UNKNOWN, VoiceIntentMatcher.match("I don't want to send a message."));
    }

    // ==========================================
    // 8. CAREGIVER SYNONYMS, PUNCTUATION & SPACING
    // ==========================================
    @Test
    public void testCaregiverSynonymsAndFormatting() {
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("   Please,   call my helper!  "));
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("phone my attendant?"));
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("connect me to person taking care of me"));
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("ಆರೈಕೆ ಮಾಡುವವರಿಗೆ ಕರೆ ಮಾಡಿ."));
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("देखभाल करने वाले को कॉल करो।"));
        assertEquals(VoiceIntentType.CALL_CAREGIVER, VoiceIntentMatcher.match("എന്റെ പരിചാരകനെ വിളിക്കുക!"));
    }

    // ==========================================
    // 9. GO BACK & SCREEN NAVIGATION COMMANDS
    // ==========================================
    @Test
    public void testGoBackAndNavigationCommands() {
        // Go back in English
        assertEquals(VoiceIntentType.GO_BACK, VoiceIntentMatcher.match("go back"));
        assertEquals(VoiceIntentType.GO_BACK, VoiceIntentMatcher.match("hey assistant go back"));
        assertEquals(VoiceIntentType.GO_BACK, VoiceIntentMatcher.match("hey assistant - go back"));
        assertEquals(VoiceIntentType.GO_BACK, VoiceIntentMatcher.match("go back to previous page"));
        assertEquals(VoiceIntentType.GO_BACK, VoiceIntentMatcher.match("go to old page"));

        // Go back in Kannada requested phrases
        assertEquals(VoiceIntentType.GO_BACK, VoiceIntentMatcher.match("ಹಿಂದೆ ಹೋಗು"));
        assertEquals(VoiceIntentType.GO_BACK, VoiceIntentMatcher.match("ಹಿಂದಕ್ಕೆ ಹೋಗು"));
        assertEquals(VoiceIntentType.GO_BACK, VoiceIntentMatcher.match("ಹಿಂದಕ್ಕೆ ಬಾ"));
        assertEquals(VoiceIntentType.GO_BACK, VoiceIntentMatcher.match("ಮರಳಿ ಬಾ"));
        assertEquals(VoiceIntentType.GO_BACK, VoiceIntentMatcher.match("ಹಿಂದಿನ ಪುಟಕ್ಕೆ ಹೋಗು"));
        assertEquals(VoiceIntentType.GO_BACK, VoiceIntentMatcher.match("ಹಿಂದಿನ ಪುಟ ತೆರೆಯಿರಿ"));
        assertEquals(VoiceIntentType.GO_BACK, VoiceIntentMatcher.match("ಹೇ ಅಸಿಸ್ಟೆಂಟ್ ಹಿಂದೆ ಹೋಗು"));

        // Direct tab navigation intents (Image 3 commands)
        assertEquals(VoiceIntentType.OPEN_HOME, VoiceIntentMatcher.match("home page"));
        assertEquals(VoiceIntentType.OPEN_HOME, VoiceIntentMatcher.match("ಮುಖ್ಯ ಪುಟ ತೆರೆಯಿರಿ"));
        assertEquals(VoiceIntentType.OPEN_PROFILE, VoiceIntentMatcher.match("my profile page"));
        assertEquals(VoiceIntentType.OPEN_PROFILE, VoiceIntentMatcher.match("profile page"));
        assertEquals(VoiceIntentType.OPEN_CAREGIVER_CONNECTION, VoiceIntentMatcher.match("caregiver page"));
        assertEquals(VoiceIntentType.OPEN_CAREGIVER_CONNECTION, VoiceIntentMatcher.match("ಪಾಲಕರ ಪುಟಕ್ಕೆ ಹೋಗು"));
    }
}
