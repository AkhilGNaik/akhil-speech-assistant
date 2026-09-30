package com.kannada.speechassistant.voiceassistant;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kannada.speechassistant.LanguageManager;
import com.kannada.speechassistant.SessionManager;

import java.util.Locale;

/**
 * Small, centralized configuration for Voice Assistant language behavior.
 * Provides language codes, SpeechRecognizer locales, TTS locales, safe TTS availability
 * verification with graceful fallback, and language-specific system response strings.
 *
 * This class contains configuration only and no business or execution logic.
 */
public final class VoiceLanguageConfig {

    private static final String TAG = "VoiceLanguageConfig";

    public static final String LANG_KANNADA = LanguageManager.LANG_KANNADA;     // "kn"
    public static final String LANG_HINDI = LanguageManager.LANG_HINDI;         // "hi"
    public static final String LANG_MALAYALAM = LanguageManager.LANG_MALAYALAM; // "ml"
    public static final String LANG_ENGLISH = LanguageManager.LANG_ENGLISH;     // "en"

    public static final String DEFAULT_LANGUAGE = LanguageManager.DEFAULT_LANGUAGE; // "kn"

    private VoiceLanguageConfig() {
        // Prevent instantiation
    }

    /**
     * Reads the current application language code from the user's session.
     */
    @NonNull
    public static String getAppLanguageCode(@Nullable Context context) {
        if (context == null) {
            return DEFAULT_LANGUAGE;
        }
        SessionManager sessionManager = new SessionManager(context);
        return LanguageManager.normalizeLanguageCode(sessionManager.getLanguage());
    }

    /**
     * Returns the speech recognition language tag (e.g. "kn-IN", "hi-IN", "ml-IN", "en-IN").
     */
    @NonNull
    public static String getSpeechRecognizerLanguageTag(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        return LanguageManager.getSpeechLanguageTag(code);
    }

    /**
     * Returns the SpeechRecognizer Locale corresponding to the language code.
     */
    @NonNull
    public static Locale getSpeechRecognizerLocale(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_HINDI:
                return new Locale("hi", "IN");
            case LANG_MALAYALAM:
                return new Locale("ml", "IN");
            case LANG_ENGLISH:
                return new Locale("en", "IN");
            case LANG_KANNADA:
            default:
                return new Locale("kn", "IN");
        }
    }

    /**
     * Returns the target TTS Locale corresponding to the language code.
     */
    @NonNull
    public static Locale getTtsLocale(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_HINDI:
                return new Locale("hi", "IN");
            case LANG_MALAYALAM:
                return new Locale("ml", "IN");
            case LANG_ENGLISH:
                return new Locale("en", "IN");
            case LANG_KANNADA:
            default:
                return new Locale("kn", "IN");
        }
    }

    /**
     * Returns the safe default fallback Locale if a language is unavailable.
     */
    @NonNull
    public static Locale getSafeTtsFallbackLocale() {
        return Locale.US;
    }

    /**
     * Safely checks whether the requested TTS language is available on the device.
     * If available, returns the appropriate Locale.
     * If unavailable:
     * 1. Does not crash
     * 2. Does not enter a retry loop
     * 3. Uses the safe TTS fallback behavior
     * 4. Logs the unavailable language clearly
     */
    @NonNull
    public static Locale resolveAvailableTtsLocale(@Nullable TextToSpeech tts, @Nullable String langCode) {
        if (tts == null) {
            return getSafeTtsFallbackLocale();
        }

        String code = LanguageManager.normalizeLanguageCode(langCode);
        Locale targetLocale = getTtsLocale(code);

        try {
            int availability = tts.isLanguageAvailable(targetLocale);
            if (availability >= TextToSpeech.LANG_AVAILABLE || availability == TextToSpeech.LANG_MISSING_DATA) {
                return targetLocale;
            }

            // Secondary check: Language without country variant
            Locale langOnlyLocale = new Locale(code);
            int langOnlyAvailability = tts.isLanguageAvailable(langOnlyLocale);
            if (langOnlyAvailability >= TextToSpeech.LANG_AVAILABLE || langOnlyAvailability == TextToSpeech.LANG_MISSING_DATA) {
                return langOnlyLocale;
            }

            // For non-English Indian languages (Kannada, Hindi, Malayalam), falling back to US English
            // completely breaks Indic text-to-speech. Return targetLocale to allow engine network voice synthesis.
            if (LANG_KANNADA.equals(code) || LANG_HINDI.equals(code) || LANG_MALAYALAM.equals(code)) {
                Log.w(TAG, "TTS voice data for '" + code + "' reported availability=" + availability + ", but retaining target locale for synthesis.");
                return targetLocale;
            }

            Log.w(TAG, "TTS voice for language '" + code + "' (locale " + targetLocale + ") is unavailable on this device (availability=" + availability + "). Falling back to safe default.");
            
            // Verify fallback availability
            Locale fallback = getSafeTtsFallbackLocale();
            int fallbackAvailability = tts.isLanguageAvailable(fallback);
            if (fallbackAvailability >= TextToSpeech.LANG_AVAILABLE) {
                return fallback;
            }

            return Locale.getDefault();
        } catch (Throwable t) {
            Log.e(TAG, "Error checking TTS language availability for: " + langCode, t);
            return getSafeTtsFallbackLocale();
        }
    }

    /**
     * Returns the language-specific "Listening." feedback string.
     */
    @NonNull
    public static String getListeningText(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_HINDI:
                return "सुन रहा हूँ।";
            case LANG_MALAYALAM:
                return "കേൾക്കുന്നു.";
            case LANG_KANNADA:
                return "ಕೇಳಿಸಿಕೊಳ್ಳುತ್ತಿದ್ದೇನೆ.";
            case LANG_ENGLISH:
            default:
                return "Listening.";
        }
    }

    @NonNull
    public static String getSayMessagePrompt(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ದಯವಿಟ್ಟು ನಿಮ್ಮ ಸಂದೇಶವನ್ನು ಹೇಳಿ.";
            case LANG_HINDI: return "कृपया अपना संदेश बोलिए।";
            case LANG_MALAYALAM: return "ദയവായി നിങ്ങളുടെ സന്ദേശം പറയൂ.";
            case LANG_ENGLISH: default: return VoiceCommandConstants.PROMPT_SAY_MESSAGE;
        }
    }

    @NonNull
    public static String getMessageSentResponse(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ಸಂದೇಶ ಕಳುಹಿಸಲಾಗಿದೆ.";
            case LANG_HINDI: return "संदेश भेज दिया गया है।";
            case LANG_MALAYALAM: return "സന്ദേശം അയച്ചു.";
            case LANG_ENGLISH: default: return VoiceCommandConstants.RESPONSE_MESSAGE_SENT;
        }
    }

    @NonNull
    public static String getClarifyHelpPrompt(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ನಿಮ್ಮ ಕೇರ್‌ಗಿವರ್‌ಗೆ ಕರೆ ಮಾಡಬೇಕೆ ಅಥವಾ ತುರ್ತು ಎಚ್ಚರಿಕೆ ಕಳುಹಿಸಬೇಕೆ?";
            case LANG_HINDI: return "क्या आप अपने केयरगिवर को कॉल करना चाहते हैं या आपातकालीन अलर्ट भेजना चाहते हैं?";
            case LANG_MALAYALAM: return "നിങ്ങളുടെ കെയർഗിവറെ വിളിക്കണോ അതോ അടിയന്തര മുന്നറിയിപ്പ് അയക്കണോ?";
            case LANG_ENGLISH: default: return VoiceCommandConstants.PROMPT_CLARIFY_HELP;
        }
    }

    @NonNull
    public static String getClarifyCallPrompt(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ನಿಮ್ಮ ಕೇರ್‌ಗಿವರ್‌ಗೆ ಕರೆ ಮಾಡಬೇಕೆ?";
            case LANG_HINDI: return "क्या आप अपने केयरगिवर को कॉल करना चाहते हैं?";
            case LANG_MALAYALAM: return "നിങ്ങളുടെ കെയർഗിവറെ വിളിക്കണോ?";
            case LANG_ENGLISH: default: return VoiceCommandConstants.PROMPT_CLARIFY_CALL;
        }
    }

    @NonNull
    public static String getClarifyMessagePrompt(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ನಿಮ್ಮ ಕೇರ್‌ಗಿವರ್‌ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಬೇಕೆ?";
            case LANG_HINDI: return "क्या आप अपने केयरगिवर को संदेश भेजना चाहते हैं?";
            case LANG_MALAYALAM: return "നിങ്ങളുടെ കെയർഗിവർക്ക് സന്ദേശം അയക്കണോ?";
            case LANG_ENGLISH: default: return VoiceCommandConstants.PROMPT_CLARIFY_SEND_MESSAGE;
        }
    }

    @NonNull
    public static String getMessageCancelledResponse(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ಸಂದೇಶ ರದ್ದುಗೊಳಿಸಲಾಗಿದೆ.";
            case LANG_HINDI: return "संदेश रद्द कर दिया गया।";
            case LANG_MALAYALAM: return "സന്ദേശം റദ്ദാക്കി.";
            case LANG_ENGLISH: default: return VoiceCommandConstants.RESPONSE_MESSAGE_CANCELLED;
        }
    }

    @NonNull
    public static String getActionCancelledResponse(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ಕ್ರಿಯೆ ರದ್ದುಗೊಳಿಸಲಾಗಿದೆ.";
            case LANG_HINDI: return "कार्रवाई रद्द कर दी गई।";
            case LANG_MALAYALAM: return "പ്രവർത്തനം റദ്ദാക്കി.";
            case LANG_ENGLISH: default: return VoiceCommandConstants.RESPONSE_ACTION_CANCELLED;
        }
    }


    /**
     * Short audio cue spoken to blind user immediately after wake word ("Assistant") is detected
     * in two-step mode, signalling that the mic is now open for their command.
     * Keep this very short (&lt; 1 word if possible) so the STT session opens quickly.
     */
    @NonNull
    public static String getListeningReadyCue(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ಹೇಳಿ.";          // "Tell me." / "Speak."
            case LANG_HINDI:   return "बोलिए।";           // "Please speak."
            case LANG_MALAYALAM: return "പറയൂ.";         // "Please say."
            case LANG_ENGLISH:
            default:           return "Yes?";
        }
    }

    @NonNull
    public static String getSpeakVoiceMessagePrompt(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ನೀವು ಕಳುಹಿಸಲು ಬಯಸುವ ಧ್ವನಿ ಸಂದೇಶವನ್ನು ಮಾತನಾಡಿ.";
            case LANG_HINDI: return "कृपया वह संदेश बोलें जो आप अपने केयरगिवर को भेजना चाहते हैं।";
            case LANG_MALAYALAM: return "നിങ്ങളുടെ കെയർഗിവർക്ക് അയക്കേണ്ട സന്ദേശം ദയവായി പറയൂ.";
            case LANG_ENGLISH: default: return VoiceCommandConstants.PROMPT_SPEAK_VOICE_MESSAGE;
        }
    }

    @NonNull
    public static String getConfirmSendVoiceMessagePrompt(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ಈ ಧ್ವನಿ ಸಂದೇಶವನ್ನು ಕಳುಹಿಸಬೇಕೇ?";
            case LANG_HINDI: return "आपका वॉइस मैसेज तैयार है। क्या आप इसे अपने केयरगिवर को भेजना चाहते हैं?";
            case LANG_MALAYALAM: return "നിങ്ങളുടെ വോയ്സ് മെസേജ് തയ്യാറാണ്. ഇത് നിങ്ങളുടെ കെയർഗിവർക്ക് അയക്കണോ?";
            case LANG_ENGLISH: default: return VoiceCommandConstants.PROMPT_CONFIRM_SEND_VOICE_MESSAGE;
        }
    }

    @NonNull
    public static String getVoiceMessageSentResponse(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ಧ್ವನಿ ಸಂದೇಶ ಕಳುಹಿಸಿದ್ದೇನೆ";
            case LANG_HINDI: return "वॉइस संदेश भेज दिया गया है।";
            case LANG_MALAYALAM: return "വോയ്സ് മെസേജ് അയച്ചു.";
            case LANG_ENGLISH: default: return VoiceCommandConstants.RESPONSE_VOICE_MESSAGE_SENT;
        }
    }

    @NonNull
    public static String getVoiceMessageCancelledResponse(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ಧ್ವನಿ ಸಂದೇಶ ರದ್ದುಗೊಳಿಸಲಾಗಿದೆ.";
            case LANG_HINDI: return "वॉइस संदेश रद्द कर दिया गया।";
            case LANG_MALAYALAM: return "വോയ്സ് മെസേജ് റദ്ദാക്കി.";
            case LANG_ENGLISH: default: return VoiceCommandConstants.RESPONSE_VOICE_MESSAGE_CANCELLED;
        }
    }

    @NonNull
    public static String getVoiceMessageFailedResponse(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ಧ್ವನಿ ಸಂದೇಶ ಕಳುಹಿಸಲು ಸಾಧ್ಯವಾಗುತ್ತಿಲ್ಲ. ದಯವಿಟ್ಟು ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ.";
            case LANG_HINDI: return "वॉइस संदेश नहीं भेजा जा सका। कृपया पुनः प्रयास करें।";
            case LANG_MALAYALAM: return "വോയ്സ് മെസേജ് അയക്കാൻ കഴിഞ്ഞില്ല. ദയവായി വീണ്ടും ശ്രമിക്കുക.";
            case LANG_ENGLISH: default: return VoiceCommandConstants.RESPONSE_VOICE_MESSAGE_FAILED;
        }
    }

    @NonNull
    public static String getNoActiveCallResponse(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ಯಾವುದೇ ಸಕ್ರಿಯ ಕರೆ ಇಲ್ಲ.";
            case LANG_HINDI: return "कोई सक्रिय कॉल नहीं है।";
            case LANG_MALAYALAM: return "സജീവ കോൾ ഒന്നുമില്ല.";
            case LANG_ENGLISH: default: return "No active call.";
        }
    }

    @NonNull
    public static String getNoCaregiverResponse(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ಯಾವುದೇ ಕೇರ್ಗಿವರ್ ಸಂಪರ್ಕಗೊಂಡಿಲ್ಲ.";
            case LANG_HINDI: return "कोई केयरगिवर जुड़ा नहीं है।";
            case LANG_MALAYALAM: return "ഒരു കെയർഗിവറും ബന്ധിപ്പിച്ചിട്ടില്ല.";
            case LANG_ENGLISH: default: return VoiceCommandConstants.RESPONSE_NO_CAREGIVER;
        }
    }

    @NonNull
    public static String getCallActiveResponse(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ಕರೆ ಸಕ್ರಿಯವಾಗಿದೆ.";
            case LANG_HINDI: return "एक कॉल सक्रिय है।";
            case LANG_MALAYALAM: return "ഒരു കോൾ സജീവമാണ്.";
            case LANG_ENGLISH: default: return VoiceCommandConstants.RESPONSE_CALL_ACTIVE;
        }
    }

    @NonNull
    public static String getNoMessageHeardResponse(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ನನಗೆ ಯಾವುದೇ ಸಂದೇಶ ಕೇಳಿಸಲಿಲ್ಲ.";
            case LANG_HINDI: return "मुझे कोई संदेश सुनाई नहीं दिया।";
            case LANG_MALAYALAM: return "എനിക്ക് ഒരു സന്ദേശവും കേൾക്കാനായില്ല.";
            case LANG_ENGLISH: default: return VoiceCommandConstants.RESPONSE_NO_MESSAGE_HEARD;
        }
    }

    @NonNull
    public static String getCaregiverSaysPrefix(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_HINDI: return "आपके केयरगिवर कहते हैं: ";
            case LANG_MALAYALAM: return "നിങ്ങളുടെ കെയർഗിവർ പറയുന്നു: ";
            case LANG_KANNADA: return "ನಿಮ್ಮ ಕೇರ್ಗಿವರ್ ಹೇಳುತ್ತಾರೆ: ";
            case LANG_ENGLISH: default: return VoiceCommandConstants.PREFIX_CAREGIVER_SAYS;
        }
    }

    @NonNull
    public static String getCaregiverVoiceMessageIntro(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_HINDI: return "आपके केयरगिवर ने एक वॉइस संदेश भेजा है।";
            case LANG_MALAYALAM: return "നിങ്ങളുടെ കെയർഗിവർ ഒരു വോയ്സ് മെസേജ് അയച്ചിട്ടുണ്ട്.";
            case LANG_KANNADA: return "ನಿಮ್ಮ ಕೇರ್ಗಿವರ್ ಧ್ವನಿ ಸಂದೇಶ ಕಳುಹಿಸಿದ್ದಾರೆ.";
            case LANG_ENGLISH: default: return "Your caregiver sent a voice message.";
        }
    }

    @NonNull
    public static String getNoNewMessagesResponse(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_HINDI: return "कोई नया संदेश नहीं है।";
            case LANG_MALAYALAM: return "പുതിയ സന്ദേശങ്ങളൊന്നുമില്ല.";
            case LANG_KANNADA: return "ಹೊಸ ಸಂದೇಶಗಳಿಲ್ಲ.";
            case LANG_ENGLISH: default: return VoiceCommandConstants.RESPONSE_NO_NEW_MESSAGES;
        }
    }

    @NonNull
    public static String getMessageCountResponse(int count, @Nullable String langCode) {
        if (count <= 0) {
            return getNoNewMessagesResponse(langCode);
        }
        String code = LanguageManager.normalizeLanguageCode(langCode);
        if (count == 1) {
            switch (code) {
                case LANG_HINDI: return "आपके पास 1 संदेश है।";
                case LANG_MALAYALAM: return "നിങ്ങളുടെ പക്കൽ 1 സന്ദേശമുണ്ട്.";
                case LANG_KANNADA: return "ನಿಮ್ಮ ಬಳಿ 1 ಸಂದೇಶವಿದೆ.";
                case LANG_ENGLISH: default: return "You have 1 message.";
            }
        } else {
            switch (code) {
                case LANG_HINDI: return "आपके पास " + count + " संदेश हैं।";
                case LANG_MALAYALAM: return "നിങ്ങളുടെ പക്കൽ " + count + " സന്ദേശങ്ങളുണ്ട്.";
                case LANG_KANNADA: return "ನಿಮ್ಮ ಬಳಿ " + count + " ಸಂದೇಶಗಳಿವೆ.";
                case LANG_ENGLISH: default: return "You have " + count + " messages.";
            }
        }
    }

    @NonNull
    public static String getNoMessageToRepeatResponse(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ಮರುಹೇಳಲು ಯಾವುದೇ ಸಂದೇಶವಿಲ್ಲ.";
            case LANG_HINDI: return "दोबारा सुनाने के लिए कोई संदेश नहीं है।";
            case LANG_MALAYALAM: return "വീണ്ടും പറയാൻ സന്ദേശമൊന്നുമില്ല.";
            case LANG_ENGLISH: default: return VoiceCommandConstants.RESPONSE_NO_MESSAGE_TO_REPEAT;
        }
    }

    @NonNull
    public static String getNoNotificationsResponse(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ಹೊಸ ಅಧಿಸೂಚನೆಗಳಿಲ್ಲ.";
            case LANG_HINDI: return "कोई नई सूचना नहीं है।";
            case LANG_MALAYALAM: return "പുതിയ അറിയിപ്പുകളൊന്നുമില്ല.";
            case LANG_ENGLISH: default: return VoiceCommandConstants.RESPONSE_NO_NOTIFICATIONS;
        }
    }

    @NonNull
    public static String getSingleNotificationIntro(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ನಿಮ್ಮ ಇತ್ತೀಚಿನ ಅಧಿಸೂಚನೆ ಹೀಗಿದೆ: ";
            case LANG_HINDI: return "आपकी नवीनतम सूचना कहती है: ";
            case LANG_MALAYALAM: return "നിങ്ങളുടെ ഏറ്റവും പുതിയ അറിയിപ്പ് പറയുന്നത്: ";
            case LANG_ENGLISH: default: return "Your latest notification says: ";
        }
    }

    @NonNull
    public static String getMultipleNotificationsIntro(int count, @Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_KANNADA: return "ನಿಮ್ಮ ಬಳಿ " + count + " ಇತ್ತೀಚಿನ ಅಧಿಸೂಚನೆಗಳಿವೆ.";
            case LANG_HINDI: return "आपके पास " + count + " हाल की सूचनाएँ हैं।";
            case LANG_MALAYALAM: return "നിങ്ങളുടെ പക്കൽ " + count + " പുതിയ അറിയിപ്പുകളുണ്ട്.";
            case LANG_ENGLISH: default: return "You have " + count + " recent notifications.";
        }
    }

    /**
     * Returns language-specific spoken responses for Blind User standard voice commands.
     */
    @NonNull
    public static String getLocalizedResponse(@NonNull String commandId,
                                              @NonNull String defaultEnglishResponse,
                                              @Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        if (LANG_ENGLISH.equals(code)) {
            return defaultEnglishResponse;
        }

        if (VoiceCommandConstants.RESPONSE_ACTION_CANCELLED.equals(defaultEnglishResponse)) {
            return getActionCancelledResponse(code);
        }
        if (VoiceCommandConstants.RESPONSE_MESSAGE_CANCELLED.equals(defaultEnglishResponse)) {
            return getMessageCancelledResponse(code);
        }
        if (VoiceCommandConstants.RESPONSE_VOICE_MESSAGE_SENT.equals(defaultEnglishResponse)) {
            return getVoiceMessageSentResponse(code);
        }
        if (VoiceCommandConstants.RESPONSE_VOICE_MESSAGE_CANCELLED.equals(defaultEnglishResponse)) {
            return getVoiceMessageCancelledResponse(code);
        }
        if (VoiceCommandConstants.RESPONSE_VOICE_MESSAGE_FAILED.equals(defaultEnglishResponse)) {
            return getVoiceMessageFailedResponse(code);
        }
        if (VoiceCommandConstants.PROMPT_SPEAK_VOICE_MESSAGE.equals(defaultEnglishResponse)) {
            return getSpeakVoiceMessagePrompt(code);
        }
        if (VoiceCommandConstants.PROMPT_CONFIRM_SEND_VOICE_MESSAGE.equals(defaultEnglishResponse)) {
            return getConfirmSendVoiceMessagePrompt(code);
        }

        switch (commandId) {
            case VoiceCommandConstants.CMD_OPEN_HOME:
                switch (code) {
                    case LANG_HINDI: return "होम खुल गया है।";
                    case LANG_MALAYALAM: return "ഹോം തുറന്നു.";
                    case LANG_KANNADA: default: return "ಮನೆ ತೆರೆಯಲಾಗಿದೆ.";
                }
            case VoiceCommandConstants.CMD_OPEN_PROFILE:
                switch (code) {
                    case LANG_HINDI: return "प्रोफ़ाइल खुल गई है।";
                    case LANG_MALAYALAM: return "പ്രൊഫൈൽ തുറന്നു.";
                    case LANG_KANNADA: default: return "ಪ್ರೊಫೈಲ್ ತೆರೆಯಲಾಗಿದೆ.";
                }
            case VoiceCommandConstants.CMD_OPEN_SETTINGS:
                switch (code) {
                    case LANG_HINDI: return "सेटिंग्स खुल गई हैं।";
                    case LANG_MALAYALAM: return "സെറ്റിംഗ്സ് തുറന്നു.";
                    case LANG_KANNADA: default: return "ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಲಾಗಿದೆ.";
                }
            case VoiceCommandConstants.CMD_GO_BACK:
                if (VoiceCommandConstants.RESPONSE_CALL_ACTIVE.equals(defaultEnglishResponse)) {
                    return defaultEnglishResponse;
                }
                if (VoiceCommandConstants.RESPONSE_ALREADY_FIRST_SCREEN.equals(defaultEnglishResponse)) {
                    switch (code) {
                        case LANG_HINDI: return "पहले से ही पहले पेज पर हैं।";
                        case LANG_MALAYALAM: return "ഇതിനകം ആദ്യ സ്ക്രീനിലാണ്.";
                        case LANG_KANNADA: default: return "ಈಗಾಗಲೇ ಮೊದಲ ಪರದೆಯಲ್ಲಿದೆ.";
                    }
                }
                switch (code) {
                    case LANG_HINDI: return "वापस जा रहा हूँ।";
                    case LANG_MALAYALAM: return "തിരികെ പോകുന്നു.";
                    case LANG_KANNADA: default: return "ಹಿಂದೆ ಹೋಗುತ್ತಿದ್ದೇನೆ.";
                }
            case VoiceCommandConstants.CMD_START_LISTENING:
                return getListeningText(code);
            case VoiceCommandConstants.CMD_STOP_LISTENING:
                switch (code) {
                    case LANG_HINDI: return "सुनना बंद किया गया।";
                    case LANG_MALAYALAM: return "കേൾക്കുന്നത് നിർത്തി.";
                    case LANG_KANNADA: default: return "ಆಲಿಸುವುದನ್ನು ನಿಲ್ಲಿಸಲಾಗಿದೆ.";
                }
            case VoiceCommandConstants.CMD_OPEN_VOICE_CALL:
                switch (code) {
                    case LANG_HINDI: return "केयरगिवर को कॉल किया जा रहा है।";
                    case LANG_MALAYALAM: return "കെയർഗിവറെ വിളിക്കുന്നു.";
                    case LANG_KANNADA: default: return "ಕೇರ್‌ಗಿವರ್‌ಗೆ ಕರೆ ಮಾಡಲಾಗುತ್ತಿದೆ.";
                }
            case VoiceCommandConstants.CMD_END_CALL:
                if ("No active call.".equals(defaultEnglishResponse)) {
                    return getNoActiveCallResponse(code);
                }
                switch (code) {
                    case LANG_HINDI: return "कॉल समाप्त हुई।";
                    case LANG_MALAYALAM: return "കോൾ അവസാനിച്ചു.";
                    case LANG_KANNADA: default: return "ಕರೆ ಮುಕ್ತಾಯವಾಗಿದೆ.";
                }
            case VoiceCommandConstants.CMD_ACCEPT_CALL:
                switch (code) {
                    case LANG_HINDI: return "कॉल रिसीव की जा रही है।";
                    case LANG_MALAYALAM: return "കോൾ സ്വീകരിക്കുന്നു.";
                    case LANG_KANNADA: default: return "ಕರೆ ಸ್ವೀಕರಿಸಲಾಗುತ್ತಿದೆ.";
                }
            case VoiceCommandConstants.CMD_OPEN_EMERGENCY:
            case VoiceCommandConstants.CMD_OPEN_EMERGENCY_ALERT:
                switch (code) {
                    case LANG_HINDI: return "आपातकालीन अलर्ट भेजा गया।";
                    case LANG_MALAYALAM: return "അടിയന്തര മുന്നറിയിപ്പ് അയച്ചു.";
                    case LANG_KANNADA: default: return "ತುರ್ತು ಎಚ್ಚರಿಕೆಯನ್ನು ಕಳುಹಿಸಲಾಗಿದೆ.";
                }
            case VoiceCommandConstants.CMD_SEND_MESSAGE:
                if (VoiceCommandConstants.PROMPT_SAY_MESSAGE.equals(defaultEnglishResponse)) {
                    return getSayMessagePrompt(code);
                }
                if (VoiceCommandConstants.RESPONSE_CALL_ACTIVE.equals(defaultEnglishResponse)) {
                    return getCallActiveResponse(code);
                }
                if (VoiceCommandConstants.RESPONSE_NO_CAREGIVER.equals(defaultEnglishResponse)) {
                    return getNoCaregiverResponse(code);
                }
                if (VoiceCommandConstants.RESPONSE_NO_MESSAGE_HEARD.equals(defaultEnglishResponse)) {
                    return getNoMessageHeardResponse(code);
                }
                if (VoiceCommandConstants.RESPONSE_MESSAGE_SENT.equals(defaultEnglishResponse)) {
                    return getMessageSentResponse(code);
                }
                return defaultEnglishResponse;
            case VoiceCommandConstants.CMD_RECORD_VOICE_MESSAGE:
                return getSpeakVoiceMessagePrompt(code);
            case VoiceCommandConstants.CMD_OPEN_VOICE_RECORDER:
                switch (code) {
                    case LANG_HINDI: return "वॉइस रिकॉर्डर खोला जा रहा है।";
                    case LANG_MALAYALAM: return "വോയ്സ് റെക്കോർഡർ തുറക്കുന്നു.";
                    case LANG_KANNADA: default: return "ಧ್ವನಿ ರೆಕಾರ್ಡರ್ ತೆರೆಯಲಾಗುತ್ತಿದೆ.";
                }
            case VoiceCommandConstants.CMD_OPEN_VOICE_MESSAGE:
                switch (code) {
                    case LANG_HINDI: return "वॉइस मैसेज खोला जा रहा है।";
                    case LANG_MALAYALAM: return "വോയ്സ് മെസേജ് തുറക്കുന്നു.";
                    case LANG_KANNADA: default: return "ಧ್ವನಿ ಸಂದೇಶ ತೆರೆಯಲಾಗುತ್ತಿದೆ.";
                }
            case VoiceCommandConstants.CMD_OPEN_MESSAGES:
            case VoiceCommandConstants.CMD_OPEN_COMMUNICATION:
                switch (code) {
                    case LANG_HINDI: return "संदेश खुले हैं।";
                    case LANG_MALAYALAM: return "സന്ദേശങ്ങൾ തുറന്നു.";
                    case LANG_KANNADA: default: return "ಸಂವಾದ ತೆರೆಯಲಾಗಿದೆ.";
                }
            case VoiceCommandConstants.CMD_OPEN_CAREGIVER:
            case VoiceCommandConstants.CMD_OPEN_CAREGIVER_CONNECTION:
                switch (code) {
                    case LANG_HINDI: return "केयरगिवर खोला जा रहा है।";
                    case LANG_MALAYALAM: return "കെയർഗിവർ തുറക്കുന്നു.";
                    case LANG_KANNADA: default: return "ಸಹಾಯಕರ ಸಂಪರ್ಕ ತೆರೆಯಲಾಗಿದೆ.";
                }
            case VoiceCommandConstants.CMD_READ_MESSAGES:
                if (VoiceCommandConstants.RESPONSE_CALL_ACTIVE.equals(defaultEnglishResponse)) {
                    return getCallActiveResponse(code);
                }
                if (VoiceCommandConstants.RESPONSE_NO_NEW_MESSAGES.equals(defaultEnglishResponse)) {
                    return getNoNewMessagesResponse(code);
                }
                return defaultEnglishResponse;
            case VoiceCommandConstants.CMD_MESSAGE_COUNT:
                if (VoiceCommandConstants.RESPONSE_CALL_ACTIVE.equals(defaultEnglishResponse)) {
                    return getCallActiveResponse(code);
                }
                if (VoiceCommandConstants.RESPONSE_NO_NEW_MESSAGES.equals(defaultEnglishResponse)) {
                    return getNoNewMessagesResponse(code);
                }
                if (defaultEnglishResponse.startsWith("You have ")) {
                    try {
                        String[] parts = defaultEnglishResponse.split(" ");
                        if (parts.length >= 3) {
                            int count = Integer.parseInt(parts[2]);
                            return getMessageCountResponse(count, code);
                        }
                    } catch (Exception ignored) {}
                }
                return defaultEnglishResponse;
            case VoiceCommandConstants.CMD_REPEAT_MESSAGE:
                if (VoiceCommandConstants.RESPONSE_CALL_ACTIVE.equals(defaultEnglishResponse)) {
                    return getCallActiveResponse(code);
                }
                if (VoiceCommandConstants.RESPONSE_NO_MESSAGE_TO_REPEAT.equals(defaultEnglishResponse)) {
                    return getNoMessageToRepeatResponse(code);
                }
                return defaultEnglishResponse;
            case VoiceCommandConstants.CMD_READ_NOTIFICATIONS:
                if (VoiceCommandConstants.RESPONSE_CALL_ACTIVE.equals(defaultEnglishResponse)) {
                    return getCallActiveResponse(code);
                }
                if (VoiceCommandConstants.RESPONSE_NO_NOTIFICATIONS.equals(defaultEnglishResponse)) {
                    return getNoNotificationsResponse(code);
                }
                return defaultEnglishResponse;
            default:
                return defaultEnglishResponse;
        }
    }

    // --- Deaf User Visual Assistant Multilingual Responses ---

    @NonNull
    public static String getDeafAssistantActivatedText(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_ENGLISH: return "Assistant activated";
            case LANG_HINDI: return "सहायक सक्रिय हुआ";
            case LANG_MALAYALAM: return "അസിസ്റ്റന്റ് സജീവമായി";
            case LANG_KANNADA: default: return "ಸಹಾಯಕ ಸಕ್ರಿಯಗೊಂಡಿದೆ";
        }
    }

    @NonNull
    public static String getDeafListeningText(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_ENGLISH: return "Listening...";
            case LANG_HINDI: return "सुन रहा है...";
            case LANG_MALAYALAM: return "കേൾക്കുന്നു...";
            case LANG_KANNADA: default: return "ಆಲಿಸುತ್ತಿದೆ...";
        }
    }

    @NonNull
    public static String getDeafSpeakCommandPrompt(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_ENGLISH: return "Speak your command";
            case LANG_HINDI: return "अपना आदेश बोलें";
            case LANG_MALAYALAM: return "നിങ്ങളുടെ കമാൻഡ് പറയുക";
            case LANG_KANNADA: default: return "ನಿಮ್ಮ ಆಜ್ಞೆಯನ್ನು ಮಾತನಾಡಿ";
        }
    }

    @NonNull
    public static String getDeafProcessingText(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_ENGLISH: return "Processing...";
            case LANG_HINDI: return "प्रक्रिया जारी है...";
            case LANG_MALAYALAM: return "പ്രോസസ്സ് ചെയ്യുന്നു...";
            case LANG_KANNADA: default: return "ಪ್ರಕ್ರಿಯೆ ನಡೆಯುತ್ತಿದೆ...";
        }
    }

    @NonNull
    public static String getDeafCommandNotUnderstoodText(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_ENGLISH: return "Sorry, I didn't understand the command.";
            case LANG_HINDI: return "क्षमा करें, आदेश समझ नहीं आया।";
            case LANG_MALAYALAM: return "ക്ഷമിക്കണം, കമാൻഡ് മനസ്സിലായില്ല.";
            case LANG_KANNADA: default: return "ಕ್ಷಮಿಸಿ, ಆಜ್ಞೆ ಅರ್ಥವಾಗಲಿಲ್ಲ.";
        }
    }

    @NonNull
    public static String getDeafFunctionNotAvailableText(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_ENGLISH: return "This function is not available.";
            case LANG_HINDI: return "यह कार्य उपलब्ध नहीं है।";
            case LANG_MALAYALAM: return "ഈ പ്രവർത്തനം ലഭ്യമല്ല.";
            case LANG_KANNADA: default: return "ಈ ಕಾರ್ಯ ಲಭ್ಯವಿಲ್ಲ.";
        }
    }

    @NonNull
    public static String getDeafErrorText(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_ENGLISH: return "Something went wrong. Please try again.";
            case LANG_HINDI: return "कुछ गलत हो गया। कृपया पुनः प्रयास करें।";
            case LANG_MALAYALAM: return "എന്തോ കുഴപ്പം സംഭവിച്ചു. ദയവായി വീണ്ടും ശ്രമിക്കുക.";
            case LANG_KANNADA: default: return "ಏನೋ ತಪ್ಪಾಗಿದೆ. ದಯವಿಟ್ಟು ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ.";
        }
    }

    @NonNull
    public static String getDeafPleaseSpeakCommandText(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_ENGLISH: return "Please speak a command.";
            case LANG_HINDI: return "कृपया एक आदेश बोलें।";
            case LANG_MALAYALAM: return "ദയവായി ഒരു കമാൻഡ് പറയുക.";
            case LANG_KANNADA: default: return "ದಯವಿಟ್ಟು ಆಜ್ಞೆಯನ್ನು ಮಾತನಾಡಿ.";
        }
    }

    @NonNull
    public static String getDeafCommandNotAvailableText(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_ENGLISH: return "Command not available.";
            case LANG_HINDI: return "आदेश उपलब्ध नहीं है।";
            case LANG_MALAYALAM: return "കമാൻഡ് ലഭ്യമല്ല.";
            case LANG_KANNADA: default: return "ಆಜ್ಞೆ ಲಭ್ಯವಿಲ್ಲ.";
        }
    }

    @NonNull
    public static String getDeafUnableToCompleteCommandText(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_ENGLISH: return "Unable to complete command.";
            case LANG_HINDI: return "आदेश पूरा करने में असमर्थ।";
            case LANG_MALAYALAM: return "കമാൻഡ് പൂർത്തിയാക്കാൻ കഴിഞ്ഞില്ല.";
            case LANG_KANNADA: default: return "ಆಜ್ಞೆಯನ್ನು ಪೂರ್ಣಗೊಳಿಸಲು ಸಾಧ್ಯವಾಗುತ್ತಿಲ್ಲ.";
        }
    }

    @NonNull
    public static String getDeafProfileOpenedText(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_ENGLISH: return "Profile opened";
            case LANG_HINDI: return "प्रोफ़ाइल खोला गया";
            case LANG_MALAYALAM: return "പ്രൊഫൈൽ തുറന്നു";
            case LANG_KANNADA: default: return "ಪ್ರೊಫೈಲ್ ತೆರೆಯಲಾಗಿದೆ";
        }
    }

    @NonNull
    public static String getDeafHomeOpenedText(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_HINDI: return "होम खोला गया";
            case LANG_MALAYALAM: return "ഹോം തുറന്നു";
            case LANG_KANNADA: default: return "ಮುಖಪುಟ ತೆರೆಯಲಾಗಿದೆ";
        }
    }

    @NonNull
    public static String getDeafSettingsOpenedText(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_HINDI: return "सेटिंग्स खोली गई";
            case LANG_MALAYALAM: return "സെറ്റിംഗ്സ് തുറന്നു";
            case LANG_KANNADA: default: return "ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಲಾಗಿದೆ";
        }
    }

    @NonNull
    public static String getDeafCaregiverOpenedText(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_HINDI: return "केयरगिवर खोला गया";
            case LANG_MALAYALAM: return "കെയർഗിവർ തുറന്നു";
            case LANG_KANNADA: default: return "ಕೇರ್ಗಿವರ್ ತೆರೆಯಲಾಗಿದೆ";
        }
    }

    @NonNull
    public static String getDeafEmergencyAlertSentText(@Nullable String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_HINDI: return "आपातकालीन अलर्ट भेजा गया";
            case LANG_MALAYALAM: return "അടിയന്തര മുന്നറിയിപ്പ് അയച്ചു";
            case LANG_KANNADA: default: return "ತುರ್ತು ಎಚ್ಚರಿಕೆ ಕಳುಹಿಸಲಾಗಿದೆ";
        }
    }
}
