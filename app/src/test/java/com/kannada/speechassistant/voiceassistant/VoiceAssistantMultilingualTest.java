package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

public class VoiceAssistantMultilingualTest {

    private VoiceCommandProcessor processor;

    @Before
    public void setUp() {
        // VoiceCommandProcessor detectIntent and preprocessInput do not require Android Context
        // Passing dummy/null or mock since detectIntent is a pure string-processing method
        processor = new VoiceCommandProcessor(new DummyContext());
    }

    // DummyContext subclassing minimal android context or testing detectIntent directly
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
    public void testKannadaCommandRecognition() {
        String lang = "kn";

        assertEquals(VoiceCommandConstants.INTENT_OPEN_HOME,
                processor.detectIntent("ಹೋಮ್ ತೆರೆಯಿರಿ", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_PROFILE,
                processor.detectIntent("ನನ್ನ ಪ್ರೊಫೈಲ್ ತೋರಿಸಿ", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_SETTINGS,
                processor.detectIntent("ಧ್ವನಿ ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಿರಿ", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_MESSAGES,
                processor.detectIntent("ಸಂದೇಶಗಳನ್ನು ತೆರೆಯಿರಿ", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_CAREGIVER,
                processor.detectIntent("ಕೇರ್‌ಗಿವರ್ ತೋರಿಸಿ", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_VOICE_RECORDER,
                processor.detectIntent("ಆಡಿಯೋ ರೆಕಾರ್ಡ್ ಮಾಡಿ", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_VOICE_CALL,
                processor.detectIntent("ಕೇರ್‌ಗಿವರ್‌ಗೆ ಕರೆ ಮಾಡಿ", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_EMERGENCY,
                processor.detectIntent("ತುರ್ತು ಪರಿಸ್ಥಿತಿ ಸಹಾಯ ಬೇಕು", lang).getIntentName());
    }

    @Test
    public void testHindiCommandRecognition() {
        String lang = "hi";

        assertEquals(VoiceCommandConstants.INTENT_OPEN_HOME,
                processor.detectIntent("होम स्क्रीन खोलो", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_PROFILE,
                processor.detectIntent("मेरी प्रोफ़ाइल दिखाओ", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_SETTINGS,
                processor.detectIntent("ध्वनि सेटिंग्स खोलो", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_MESSAGES,
                processor.detectIntent("संदेश दिखाओ", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_CAREGIVER,
                processor.detectIntent("केयरगिवर खोलो", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_VOICE_RECORDER,
                processor.detectIntent("ऑडियो रिकॉर्ड करो", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_VOICE_CALL,
                processor.detectIntent("केयरगिवर को कॉल करो", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_EMERGENCY,
                processor.detectIntent("आपातकालीन मदद चाहिए", lang).getIntentName());
    }

    @Test
    public void testMalayalamCommandRecognition() {
        String lang = "ml";

        assertEquals(VoiceCommandConstants.INTENT_OPEN_HOME,
                processor.detectIntent("ഹോം തുറക്കുക", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_PROFILE,
                processor.detectIntent("എന്റെ പ്രൊഫൈൽ കാണിക്കുക", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_SETTINGS,
                processor.detectIntent("ശബ്ദ ക്രമീകരണങ്ങൾ", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_MESSAGES,
                processor.detectIntent("സന്ദേശങ്ങൾ തുറക്കുക", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_CAREGIVER,
                processor.detectIntent("കെയർഗിവർ കാണിക്കുക", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_VOICE_RECORDER,
                processor.detectIntent("ഓഡിയോ റെക്കോർഡ് ചെയ്യുക", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_VOICE_CALL,
                processor.detectIntent("കെയർഗിവറെ വിളിക്കുക", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_EMERGENCY,
                processor.detectIntent("അടിയന്തര സഹായം വേണം", lang).getIntentName());
    }

    @Test
    public void testEnglishNaturalVariations() {
        String lang = "kn"; // English works seamlessly regardless of session language

        assertEquals(VoiceCommandConstants.INTENT_OPEN_HOME,
                processor.detectIntent("take me to the main dashboard screen", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_MESSAGES,
                processor.detectIntent("please show my messages right now", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_VOICE_CALL,
                processor.detectIntent("start a voice call with caregiver", lang).getIntentName());

        assertEquals(VoiceCommandConstants.INTENT_OPEN_EMERGENCY,
                processor.detectIntent("SOS danger I need help urgently!", lang).getIntentName());
    }

    @Test
    public void testSpeechNormalization() {
        assertEquals("open messages", processor.preprocessInput("  Open messages!?..  ", "kn"));
        assertEquals("ತುರ್ತು ಸಹಾಯ", processor.preprocessInput("  ತುರ್ತು...  ಸಹಾಯ!!  ", "kn"));
        assertEquals("आपातकालीन मदद", processor.preprocessInput("...आपातकालीन, मदद???", "hi"));
        assertEquals("അടിയന്തര സഹായം", processor.preprocessInput("അടിയന്തര: സഹായം!!!", "ml"));
    }
}
