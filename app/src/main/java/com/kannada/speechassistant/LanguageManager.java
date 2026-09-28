package com.kannada.speechassistant;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;

import java.util.Locale;

/**
 * Manages locale resolution, setting, loading, and context wrapping for multilingual support:
 * - Kannada ("kn") [Default]
 * - Hindi ("hi")
 * - Malayalam ("ml")
 */
public class LanguageManager {

    public static final String LANG_KANNADA = "kn";
    public static final String LANG_HINDI = "hi";
    public static final String LANG_MALAYALAM = "ml";
    public static final String LANG_ENGLISH = "en";

    public static final String DEFAULT_LANGUAGE = LANG_KANNADA;

    /**
     * Normalizes input language string into a valid language code ("kn", "hi", "ml", "en").
     * Falls back to "kn" if input is null, empty, or invalid.
     */
    public static String normalizeLanguageCode(String rawLang) {
        if (rawLang == null || rawLang.trim().isEmpty()) {
            return DEFAULT_LANGUAGE;
        }

        String lower = rawLang.trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith("hi") || lower.contains("hindi")) {
            return LANG_HINDI;
        } else if (lower.startsWith("ml") || lower.contains("malayalam")) {
            return LANG_MALAYALAM;
        } else if (lower.startsWith("en") || lower.contains("english")) {
            return LANG_ENGLISH;
        } else if (lower.startsWith("kn") || lower.contains("kannada")) {
            return LANG_KANNADA;
        }

        return DEFAULT_LANGUAGE;
    }

    /**
     * Converts language code to display name for UI dropdowns.
     */
    public static String getLanguageDisplayName(String langCode) {
        String code = normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_HINDI:
                return "Hindi";
            case LANG_MALAYALAM:
                return "Malayalam";
            case LANG_ENGLISH:
                return "English";
            case LANG_KANNADA:
            default:
                return "Kannada";
        }
    }

    /**
     * Converts UI dropdown display name to language code.
     */
    public static String getLanguageCodeFromDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return DEFAULT_LANGUAGE;
        }
        String lower = displayName.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("hindi")) {
            return LANG_HINDI;
        } else if (lower.contains("malayalam")) {
            return LANG_MALAYALAM;
        } else if (lower.contains("english")) {
            return LANG_ENGLISH;
        } else if (lower.contains("kannada")) {
            return LANG_KANNADA;
        }
        return DEFAULT_LANGUAGE;
    }

    /**
     * Wraps context with target Locale for Activity attachBaseContext.
     */
    public static Context setLocale(Context context, String langCode) {
        String code = normalizeLanguageCode(langCode);
        Locale locale = new Locale(code);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());

        Context newContext = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            newContext = context.createConfigurationContext(config);
        } else {
            config.locale = locale;
        }
        resources.updateConfiguration(config, resources.getDisplayMetrics());
        return newContext;
    }

    /**
     * Gets Google Input Tools Transliteration API code for target language.
     * Kannada: kn-t-i0-und
     * Hindi: hi-t-i0-und
     * Malayalam: ml-t-i0-und
     */
    public static String getInputToolCode(String langCode) {
        String code = normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_HINDI:
                return "hi-t-i0-und";
            case LANG_MALAYALAM:
                return "ml-t-i0-und";
            case LANG_KANNADA:
            default:
                return "kn-t-i0-und";
        }
    }

    /**
     * Gets speech recognition / STT language tag for target language.
     * Kannada: kn-IN
     * Hindi: hi-IN
     * Malayalam: ml-IN
     * English: en-IN
     */
    public static String getSpeechLanguageTag(String langCode) {
        String code = normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_HINDI:
                return "hi-IN";
            case LANG_MALAYALAM:
                return "ml-IN";
            case LANG_ENGLISH:
                return "en-IN";
            case LANG_KANNADA:
            default:
                return "kn-IN";
        }
    }

    /**
     * Resolves Locale instance for TTS and system formatting.
     */
    public static Locale getLocale(String langCode) {
        String code = normalizeLanguageCode(langCode);
        return new Locale(code, "IN");
    }

    /**
     * Gets default localized Emergency SOS message for target language.
     * Kannada: ತುರ್ತು ಪರಿಸ್ಥಿತಿ! ಸಹಾಯ ಬೇಕು
     * Hindi: आपातकालीन मदद चाहिए!
     * Malayalam: അടിയന്തര സഹായം വേണം!
     */
    public static String getDefaultSosMessage(String langCode) {
        String code = normalizeLanguageCode(langCode);
        switch (code) {
            case LANG_MALAYALAM:
                return "അടിയന്തര സഹായം വേണം!";
            case LANG_HINDI:
                return "आपातकालीन मदद चाहिए!";
            case LANG_KANNADA:
            default:
                return "ತುರ್ತು ಪರಿಸ್ಥಿತಿ! ಸಹಾಯ ಬೇಕು";
        }
    }

    /**
     * Dynamically detects language name ("Kannada", "Hindi", "Malayalam") from script block,
     * falling back to user's saved session language code.
     */
    public static String detectLanguageFromText(String text, String fallbackLangCode) {
        if (text != null) {
            for (char c : text.toCharArray()) {
                Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
                if (block == Character.UnicodeBlock.KANNADA) {
                    return "Kannada";
                } else if (block == Character.UnicodeBlock.DEVANAGARI) {
                    return "Hindi";
                } else if (block == Character.UnicodeBlock.MALAYALAM) {
                    return "Malayalam";
                }
            }
        }
        String code = normalizeLanguageCode(fallbackLangCode);
        switch (code) {
            case LANG_HINDI:
                return "Hindi";
            case LANG_MALAYALAM:
                return "Malayalam";
            case LANG_ENGLISH:
                return "English";
            case LANG_KANNADA:
            default:
                return "Kannada";
        }
    }

    /**
     * Checks if a message string matches any default SOS phrase across all supported languages.
     */
    public static boolean isDefaultSosMessage(String message) {
        if (message == null || message.trim().isEmpty()) return true;
        String trimmed = message.trim();
        return trimmed.equals("ತುರ್ತು ಪರಿಸ್ಥಿತಿ! ಸಹಾಯ ಬೇಕು")
                || trimmed.equals("ತುರ್ತು ಪರಿಸ್ಥಿತಿ ಸಹಾಯ ಬೇಕು")
                || trimmed.equals("ಅടിയന്തര സഹായം വേണം!")
                || trimmed.equals("आपातकालीन मदद चाहिए!")
                || trimmed.contains("ತುರ್ತು")
                || trimmed.contains("ಅടിയന്തര")
                || trimmed.contains("आपातकालीन");
    }

    /**
     * Resolves localized phrase text for Quick Phrases (Water, Food, Help, Rest, Outside, Toilet, Quiet)
     * according to the active language code ("kn", "hi", "ml").
     */
    public static String getQuickPhraseText(String phraseKey, String langCode) {
        if (phraseKey == null || phraseKey.trim().isEmpty()) return "";
        String code = normalizeLanguageCode(langCode);
        String k = phraseKey.trim().toLowerCase(Locale.ROOT);

        if (k.contains("water") || k.contains("ನೀರು") || k.contains("पानी") || k.contains("വെള്ളം")) {
            switch (code) {
                case LANG_MALAYALAM: return "വെള്ളം വേണം";
                case LANG_HINDI: return "पानी चाहिए";
                case LANG_KANNADA: default: return "ನೀರು ಬೇಕು";
            }
        } else if (k.contains("food") || k.contains("ಹಸಿವಾಗಿದೆ") || k.contains("ಆಹಾರ") || k.contains("भूख") || k.contains("खाना") || k.contains("വിശക്കുന്നു") || k.contains("ഭക്ഷണം")) {
            switch (code) {
                case LANG_MALAYALAM: return "വിശക്കുന്നു";
                case LANG_HINDI: return "भूख लगी है";
                case LANG_KANNADA: default: return "ಹಸಿವಾಗಿದೆ";
            }
        } else if (k.contains("help") || k.contains("ಸಹಾಯ") || k.contains("ಮದದ") || k.contains("मदद") || k.contains("സഹായം")) {
            switch (code) {
                case LANG_MALAYALAM: return "സഹായം വേണം";
                case LANG_HINDI: return "मदद चाहिए";
                case LANG_KANNADA: default: return "ಸಹಾಯ ಬೇಕು";
            }
        } else if (k.contains("rest") || k.contains("ವಿಶ್ರಾಂತಿ") || k.contains("ಆರಾಮ") || k.contains("आराम") || k.contains("വിശ്രമം")) {
            switch (code) {
                case LANG_MALAYALAM: return "വിശ്രമം വേണം";
                case LANG_HINDI: return "आराम चाहिए";
                case LANG_KANNADA: default: return "ವಿಶ್ರಾಂತಿ ಬೇಕು";
            }
        } else if (k.contains("outside") || k.contains("ಹೊರಗೆ") || k.contains("बाहर") || k.contains("പുറത്തു") || k.contains("പുറത്ത്")) {
            switch (code) {
                case LANG_MALAYALAM: return "പുറത്തു പോകണം";
                case LANG_HINDI: return "बाहर जाना है";
                case LANG_KANNADA: default: return "ಹೊರಗೆ ಹೋಗಬೇಕು";
            }
        } else if (k.contains("toilet") || k.contains("ಶೌಚಾಲಯ") || k.contains("शौचालय") || k.contains("ടോയ്‌ലറ്റ്")) {
            switch (code) {
                case LANG_MALAYALAM: return "ടോയ്‌ലറ്റ് പോകണം";
                case LANG_HINDI: return "शौचालय जाना है";
                case LANG_KANNADA: default: return "ಶೌಚಾಲಯಕ್ಕೆ ಹೋಗಬೇಕು";
            }
        } else if (k.contains("quiet") || k.contains("ಮೌನವಾಗಿರಿ") || k.contains("ಮೌನ") || k.contains("ಶಾಂತ") || k.contains("ಚುಪ") || k.contains("चुप") || k.contains("शांत") || k.contains("शान्त") || k.contains("ശാന്തമായിരിക്കുക") || k.contains("ശാന്തം")) {
            switch (code) {
                case LANG_MALAYALAM: return "ദയവായി ശാന്തമായിരിക്കുക";
                case LANG_HINDI: return "कृपया चुप रहें";
                case LANG_KANNADA: default: return "ದಯವಿಟ್ಟು ಮೌನವಾಗಿರಿ";
            }
        }

        return phraseKey;
    }

    /**
     * Wraps context using saved session language.
     */
    public static Context wrapContext(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        String langCode = sessionManager.getLanguage();
        return setLocale(context, langCode);
    }
}
