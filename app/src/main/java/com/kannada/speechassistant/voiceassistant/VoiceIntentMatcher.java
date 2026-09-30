package com.kannada.speechassistant.voiceassistant;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Lightweight, robust intent matching engine for the Voice Assistant.
 *
 * Implements structured Multilingual Natural Intent Matching (English, Kannada, Hindi, Malayalam)
 * mapping natural speech patterns, synonyms, and grammatical variations to existing VoiceIntentTypes.
 */
public class VoiceIntentMatcher {

    public static final String PUNCTUATION_REGEX = "[.,?!:;\"'()_\\-\\[\\]{}|/\\\\`~@#$%^&*+=<>।॥]";

    /**
     * Checks whether raw spoken text explicitly includes the wake word ("assistant", "hey assistant",
     * or Kannada, Hindi, Malayalam wake words like "ಹೇ ಅಸಿಸ್ಟೆಂಟ್", "ಅಸಿಸ್ಟೆಂಟ್", "ಹೇ ಸಹಾಯಕ").
     */
    public static boolean hasWakeWord(@Nullable String rawText) {
        if (rawText == null) return false;
        String s = rawText.replaceAll(PUNCTUATION_REGEX, " ")
                .replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        return s.startsWith("assistant") || s.startsWith("hey assistant") ||
                s.startsWith("ok assistant") || s.startsWith("okay assistant") ||
                s.contains(" assistant ") || s.endsWith(" assistant") ||
                s.startsWith("ಅಸಿಸ್ಟೆಂಟ್") || s.startsWith("ಹೇ ಅಸಿಸ್ಟೆಂಟ್") ||
                s.startsWith("ಓಕೆ ಅಸಿಸ್ಟೆಂಟ್") || s.startsWith("ಅಸಿಸ್ಟಂಟ್") ||
                s.startsWith("ಹೇ ಅಸಿಸ್ಟಂಟ್") || s.startsWith("ಹೇ ಸಹಾಯಕ") ||
                s.contains(" ಅಸಿಸ್ಟೆಂಟ್ ") || s.endsWith(" ಅಸಿಸ್ಟೆಂಟ್") ||
                s.contains(" ಅಸಿಸ್ಟಂಟ್ ") || s.endsWith(" ಅಸಿಸ್ಟಂಟ್") ||
                s.startsWith("असिस्टेंट") || s.startsWith("हे असिस्टेंट") ||
                s.startsWith("हे सहायक") || s.contains(" असिस्टेंट ") || s.endsWith(" असिस्टेंट") ||
                s.startsWith("അസിസ്റ്റന്റ്") || s.startsWith("ഹേ അസിസ്റ്റന്റ്") ||
                s.startsWith("ഹേ സഹായി") || s.contains(" അസിസ്റ്റന്റ് ") || s.endsWith(" അസിസ്റ്റന്റ്");
    }

    /**
     * Extracts only the command portion of spoken text by stripping wake word prefixes across all supported languages.
     */
    @NonNull
    public static String extractCommandText(@Nullable String rawText) {
        if (rawText == null) return "";
        // Strip zero-width characters (ZWNJ \u200C, ZWJ \u200D, ZWSP \u200B, BOM \uFEFF)
        String cleaned = rawText.replaceAll("[\u200B-\u200D\uFEFF]", "");
        cleaned = cleaned.replaceAll(PUNCTUATION_REGEX, " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);

        // Strip wake word prefixes: English, Kannada, Hindi, Malayalam
        String[] wakePrefixes = {
                "hey assistant ", "ok assistant ", "okay assistant ", "assistant ",
                "ಹೇ ಅಸಿಸ್ಟೆಂಟ್ ", "ಓಕೆ ಅಸಿಸ್ಟೆಂಟ್ ", "ಅಸಿಸ್ಟೆಂಟ್ ",
                "ಹೇ ಅಸಿಸ್ಟಂಟ್ ", "ಅಸಿಸ್ಟಂಟ್ ", "ಹೇ ಸಹಾಯಕ ",
                "हे असिस्टेंट ", "ओके असिस्टेंट ", "असिस्टेंट ", "हे सहायक ",
                "ഹേ അസിസ്റ്റന്റ് ", "ഓക്കെ അസിസ്റ്റന്റ് ", "അസിസ്റ്റന്റ് ", "ഹേ സഹായി "
        };
        for (String prefix : wakePrefixes) {
            if (cleaned.startsWith(prefix)) {
                cleaned = cleaned.substring(prefix.length()).trim();
                break;
            }
        }

        // Exact wake word only (Style 2 wake trigger)
        if (cleaned.equals("hey assistant") || cleaned.equals("assistant") ||
                cleaned.equals("ok assistant") || cleaned.equals("okay assistant") ||
                cleaned.equals("ಹೇ ಅಸಿಸ್ಟೆಂಟ್") || cleaned.equals("ಅಸಿಸ್ಟೆಂಟ್") ||
                cleaned.equals("ಓಕೆ ಅಸಿಸ್ಟೆಂಟ್") || cleaned.equals("ಹೇ ಅಸಿಸ್ಟಂಟ್") ||
                cleaned.equals("ಅಸಿಸ್ಟಂಟ್") || cleaned.equals("ಹೇ ಸಹಾಯಕ") ||
                cleaned.equals("हे असिस्टेंट") || cleaned.equals("असिस्टेंट") ||
                cleaned.equals("हे सहायक") || cleaned.equals("ഹേ അസിസ്റ്റന്റ്") ||
                cleaned.equals("അസിസ്റ്റന്റ്") || cleaned.equals("ഹേ സഹായി")) {
            return "";
        }
        // Normalize compound Kannada caregiver transliterations and split suffixes
        cleaned = cleaned.replaceAll("ಕೇರ್\\s+ಗಿವರ್", "ಕೇರ್ಗಿವರ್");
        cleaned = cleaned.replaceAll("ಕೇರ್\\s+ಟೇಕರ್", "ಕೇರ್ಟೇಕರ್");
        cleaned = cleaned.replaceAll("ಆರೈಕೆ\\s+ಮಾಡುವವ(ರಿಗೆ|ನಿಗೆ|ಗೆ)", "ಆರೈಕೆದಾರ$1");
        cleaned = cleaned.replaceAll("ಆರೈಕೆ\\s+ಮಾಡುವವರ", "ಆರೈಕೆದಾರರ");
        cleaned = cleaned.replaceAll("ಆರೈಕೆ\\s+ಮಾಡುವವರಿಂದ", "ಆರೈಕೆದಾರರಿಂದ");
        cleaned = cleaned.replaceAll("ಆರೈಕೆ\\s+ಮಾಡುವವರನ್ನು", "ಆರೈಕೆದಾರರನ್ನು");
        cleaned = cleaned.replaceAll("ಆರೈಕೆ\\s+ಮಾಡುವವರು", "ಆರೈಕೆದಾರ");
        cleaned = cleaned.replaceAll("(ಆರೈಕೆದಾರ|ಸಹಾಯಕ|ಕೇರ್ಗಿವರ್|ಕೇರ್ಟೇಕರ್|ಪಾಲಕ)\\s+(ರಿಗೆ|ನಿಗೆ|ಗೆ|ರ|ನ|ರಿಂದ|ನಿಂದ|ವನ್ನು|ಗಳ|ಗಳು|ಗಳನ್ನು|ರನ್ನು|ನನ್ನು|ಿಗೆ)", "$1$2");
        // Normalize compound Hindi caregiver transliterations and spacing
        cleaned = cleaned.replaceAll("केयर\\s+गिवर", "केयरगिवर");
        cleaned = cleaned.replaceAll("केयर\\s+टेकर", "केयरटेकर");
        cleaned = cleaned.replaceAll("देखभाल\\s+करने\\s+वाला", "देखभाल करने वाला");
        cleaned = cleaned.replaceAll("देखभाल\\s+करने\\s+वाले", "देखभाल करने वाले");
        cleaned = cleaned.replaceAll("देखभाल\\s+करने\\s+वाली", "देखभाल करने वाली");
        cleaned = cleaned.replaceAll("देखभालकर्ता", "देखभाल करने वाले");
        cleaned = cleaned.replaceAll("परिचारक", "सहायक");
        // Normalize compound Malayalam caregiver transliterations and spacing
        cleaned = cleaned.replaceAll("കെയർ\\s+ഗിവർ", "കെയർഗിവർ");
        cleaned = cleaned.replaceAll("കെയർ\\s+ടേക്കർ", "കെയർടേക്കർ");
        cleaned = cleaned.replaceAll("പരിചരണം\\s+നൽകുന്ന", "പരിചരിക്കുന്ന");
        cleaned = cleaned.replaceAll("പരിചരണം\\s+നൽകുന്നയാൾക്ക്", "പരിചരിക്കുന്നയാൾക്ക്");
        cleaned = cleaned.replaceAll("പരിചരണം\\s+നൽകുന്നയാളെ", "പരിചരിക്കുന്നയാളെ");
        cleaned = cleaned.replaceAll("പരിചരണം\\s+നൽകുന്നയാളുടെ", "പരിചരിക്കുന്നയാളുടെ");
        cleaned = cleaned.replaceAll("ശുശ്രൂഷകന്", "പരിചാരകന്");
        cleaned = cleaned.replaceAll("ശുശ്രൂഷകനെ", "പരിചാരകനെ");
        cleaned = cleaned.replaceAll("ശുശ്രൂഷകന്റെ", "പരിചാരകന്റെ");
        cleaned = cleaned.replaceAll("ശുശ്രൂഷകൻ", "പരിചാരകൻ");
        cleaned = cleaned.replaceAll("പരിചരിക്കുന്ന\\s+ആൾ", "പരിചരിക്കുന്നയാൾ");
        cleaned = cleaned.replaceAll("പരിചരിക്കുന്ന\\s+ആളെ", "പരിചരിക്കുന്നയാളെ");
        cleaned = cleaned.replaceAll("പരിചരിക്കുന്ന\\s+ആൾക്ക്", "പരിചരിക്കുന്നയാൾക്ക്");
        cleaned = cleaned.replaceAll("പരിചരിക്കുന്ന\\s+ആളുടെ", "പരിചരിക്കുന്നയാളുടെ");
        // Normalize compound English caregiver spacing and possessives
        cleaned = cleaned.replaceAll("person\\s+taking\\s+care\\s+of\\s+me", "caregiver");
        cleaned = cleaned.replaceAll("my\\s+assistant\\s+at\\s+home", "my caregiver");
        cleaned = cleaned.replaceAll("\\bcare\\s+giver\\b", "caregiver");
        cleaned = cleaned.replaceAll("\\bcare\\s+taker\\b", "caretaker");
        cleaned = cleaned.replaceAll("\\bcareworker\\b", "care worker");
        cleaned = cleaned.replaceAll("\\bcare\\s+workers\\b", "care worker");
        cleaned = cleaned.replaceAll("\\b(caregiver|carer|caretaker|helper|attendant)s\\b", "$1");
        cleaned = cleaned.replaceAll("\\b(caregiver|carer|caretaker|care worker|helper|attendant)\\s+s\\b", "$1");
        return cleaned;
    }

    /**
     * Normalizes raw spoken text by stripping punctuation, collapsing multiple spaces, and converting to lowercase.
     */
    @NonNull
    public static String normalizeText(@Nullable String rawText) {
        return extractCommandText(rawText);
    }

    public static boolean containsKannadaCaregiverAny(@NonNull String s) {
        return s.contains("ಆರೈಕೆದಾರ") || s.contains("ಸಹಾಯಕ") ||
                s.contains("ಕೇರ್ಗಿವರ್") || s.contains("ಕೇರ್ಟೇಕರ್") ||
                s.contains("ಪಾಲಕ");
    }

    public static boolean containsKannadaCaregiverDative(@NonNull String s) {
        return s.contains("ಆರೈಕೆದಾರರಿಗೆ") || s.contains("ಆರೈಕೆದಾರನಿಗೆ") ||
                s.contains("ಸಹಾಯಕನಿಗೆ") || s.contains("ಸಹಾಯಕರಿಗೆ") || s.contains("ಸಹಾಯಕಿಗೆ") ||
                s.contains("ಪಾಲಕರಿಗೆ") || s.contains("ಪಾಲಕನಿಗೆ") || s.contains("ಪಾಲಕಿಗೆ") ||
                s.contains("ಕೇರ್ಗಿವರ್ಗೆ") || s.contains("ಕೇರ್ಟೇಕರ್ಗೆ") ||
                s.contains("ಕೇರ್ಗಿವರಿಗೆ") || s.contains("ಕೇರ್ಟೇಕರಿಗೆ") ||
                s.contains("ಕೇರ್ಗಿವರ್‌ಗೆ") || s.contains("ಕೇರ್ಟೇಕರ್‌ಗೆ");
    }

    public static boolean containsKannadaCaregiverGenitive(@NonNull String s) {
        return s.contains("ಆರೈಕೆದಾರರ") || s.contains("ಆರೈಕೆದಾರನ") ||
                s.contains("ಸಹಾಯಕನ") || s.contains("ಸಹಾಯಕರ") ||
                s.contains("ಪಾಲಕರ") || s.contains("ಪಾಲಕನ") ||
                s.contains("ಕೇರ್ಗಿವರ್ನ") || s.contains("ಕೇರ್ಟೇಕರ್ನ") ||
                s.contains("ಕೇರ್ಗಿವರ್‌ನ") || s.contains("ಕೇರ್ಟೇಕರ್‌ನ") ||
                s.contains("ಕೇರ್ಗಿವರ್ ಸಂದೇಶ") || s.contains("ಕೇರ್ಗಿವರ್ ಮೆಸೇಜ್") ||
                s.contains("ಕೇರ್ಟೇಕರ್ ಸಂದೇಶ") || s.contains("ಕೇರ್ಟೇಕರ್ ಮೆಸೇಜ್") ||
                s.contains("ಆರೈಕೆದಾರ ಸಂದೇಶ") || s.contains("ಆರೈಕೆದಾರ ಮೆಸೇಜ್") ||
                s.contains("ಸಹಾಯಕ ಸಂದೇಶ") || s.contains("ಸಹಾಯಕ ಮೆಸೇಜ್") ||
                s.contains("ಪಾಲಕ ಸಂದೇಶ") || s.contains("ಪಾಲಕ ಮೆಸೇಜ್");
    }

    public static boolean containsKannadaCaregiverAblative(@NonNull String s) {
        return s.contains("ಆರೈಕೆದಾರರಿಂದ") || s.contains("ಆರೈಕೆದಾರನಿಂದ") ||
                s.contains("ಸಹಾಯಕನಿಂದ") || s.contains("ಸಹಾಯಕರಿಂದ") ||
                s.contains("ಪಾಲಕರಿಂದ") || s.contains("ಪಾಲಕನಿಂದ") ||
                s.contains("ಕೇರ್ಗಿವರ್ನಿಂದ") || s.contains("ಕೇರ್ಟೇಕರ್ನಿಂದ") ||
                s.contains("ಕೇರ್ಗಿವರ್‌ನಿಂದ") || s.contains("ಕೇರ್ಟೇಕರ್‌ನಿಂದ") ||
                s.contains("ಆರೈಕೆದಾರರ") || s.contains("ಆರೈಕೆದಾರನ") ||
                s.contains("ಸಹಾಯಕನ") || s.contains("ಸಹಾಯಕರ") ||
                s.contains("ಪಾಲಕರ") || s.contains("ಪಾಲಕನ") ||
                s.contains("ಕೇರ್ಗಿವರ್ನ") || s.contains("ಕೇರ್ಟೇಕರ್ನ") ||
                s.contains("ಕೇರ್ಗಿವರ್‌ನ") || s.contains("ಕೇರ್ಟೇಕರ್‌ನ") ||
                s.contains("ಕೇರ್ಗಿವರ್ ಎಷ್ಟು") || s.contains("ಕೇರ್ಟೇಕರ್ ಎಷ್ಟು") ||
                s.contains("ಸಹಾಯಕ ಎಷ್ಟು") || s.contains("ಪಾಲಕ ಎಷ್ಟು");
    }

    public static boolean isGenericCaregiverInquiry(@NonNull String s) {
        if (!containsKannadaCaregiverAny(s)) {
            return false;
        }
        if (s.contains("ಯಾರು") || s.contains("ಬಗ್ಗೆ") || s.contains("ಮಾಹಿತಿ") ||
                s.contains("ಏನು ಮಾಡುತ್ತಾನೆ") || s.contains("ಏನು ಮಾಡುತ್ತಾರೆ") ||
                (s.contains("ಬೇಕು") && !s.contains("ಸಹಾಯ ಬೇಕು") && !s.contains("ಕಳುಹಿಸಬೇಕು") && !s.contains("ಕಳಿಸಬೇಕು") && !s.contains("ಮಾಡಬೇಕು") && !s.contains("ಮುಗಿಸಬೇಕು") && !s.contains("ಮಾತನಾಡಬೇಕು") && !s.contains("ಸಂಪರ್ಕ"))) {
            return true;
        }
        return false;
    }

    public static boolean containsHindiCaregiverAny(@NonNull String s) {
        return s.contains("देखभाल करने वाले") || s.contains("देखभाल करने वाला") ||
                s.contains("देखभाल करने वाली") || s.contains("सहायक") ||
                s.contains("केयरगिवर") || s.contains("केयरटेकर");
    }

    public static boolean containsHindiCaregiverDative(@NonNull String s) {
        return s.contains("देखभाल करने वाले को") || s.contains("देखभाल करने वाले") ||
                s.contains("सहायक को") || s.contains("केयरगिवर को") || s.contains("केयरटेकर को") ||
                s.contains("मेरे देखभाल करने वाले") || s.contains("मेरे सहायक") ||
                s.contains("मेरे केयरगिवर") || s.contains("मेरे केयरटेकर") ||
                s.contains("मेरा सहायक") || s.contains("मेरा केयरगिवर") || s.contains("मेरा केयरटेकर");
    }

    public static boolean containsHindiCaregiverGenitive(@NonNull String s) {
        return s.contains("देखभाल करने वाले के") || s.contains("देखभाल करने वाले का") || s.contains("देखभाल करने वाले की") ||
                s.contains("सहायक के") || s.contains("सहायक का") || s.contains("सहायक की") ||
                s.contains("केयरगिवर के") || s.contains("केयरगिवर का") || s.contains("केयरगिवर की") ||
                s.contains("केयरटेकर के") || s.contains("केयरटेकर का") || s.contains("केयरटेकर की") ||
                s.contains("केयरगिवर संदेश") || s.contains("केयरगिवर मैसेज") ||
                s.contains("केयरटेकर संदेश") || s.contains("केयरटेकर मैसेज");
    }

    public static boolean containsHindiCaregiverAblative(@NonNull String s) {
        return s.contains("देखभाल करने वाले से") || s.contains("सहायक से") ||
                s.contains("केयरगिवर से") || s.contains("केयरटेकर से") ||
                containsHindiCaregiverGenitive(s);
    }

    public static boolean isGenericHindiCaregiverInquiry(@NonNull String s) {
        if (!containsHindiCaregiverAny(s)) {
            return false;
        }
        if (s.contains("कौन") || s.contains("बारे में") || s.contains("जानकारी") ||
                s.contains("क्या करता") || s.contains("क्या करते") ||
                s.contains("क्या है") || s.contains("क्या हैं") ||
                (s.contains("चाहिए") && !s.contains("मदद चाहिए") && !s.contains("सहायता चाहिए"))) {
            return true;
        }
        return false;
    }

    public static boolean containsMalayalamCaregiverAny(@NonNull String s) {
        return s.contains("പരിചരിക്കുന്നയാൾ") || s.contains("പരിചരിക്കുന്നയാളെ") ||
                s.contains("പരിചരിക്കുന്ന ആളെ") || s.contains("പരിചരിക്കുന്ന ആൾ") ||
                s.contains("പരിചരണം നൽകുന്ന") || s.contains("പരിചരണം") ||
                s.contains("പരിചാരകൻ") || s.contains("പരിചാരകനെ") ||
                s.contains("സഹായി") || s.contains("സഹായിയെ") ||
                s.contains("സഹായകൻ") || s.contains("സഹായകനെ") || s.contains("സഹായക") ||
                s.contains("കെയർഗിവർ") || s.contains("കെയർഗിവറെ") ||
                s.contains("കെയർടേക്കർ") || s.contains("കെയർടേക്കറെ");
    }

    public static boolean containsMalayalamCaregiverDative(@NonNull String s) {
        return s.contains("പരിചരിക്കുന്നയാൾക്ക്") || s.contains("പരിചരിക്കുന്നയാളെ") ||
                s.contains("പരിചാരകന്") || s.contains("പരിചാരകനെ") ||
                s.contains("സഹായിക്ക്") || s.contains("സഹായിയെ") ||
                s.contains("കെയർഗിവറിന്") || s.contains("കെയർഗിവർക്ക്") || s.contains("കെയർഗിവറെ") ||
                s.contains("കെയർടേക്കറിന്") || s.contains("കെയർടേക്കർക്ക്") || s.contains("കെയർടേക്കറെ");
    }

    public static boolean containsMalayalamCaregiverGenitive(@NonNull String s) {
        return s.contains("പരിചരിക്കുന്നയാളുടെ") || s.contains("പരിചരിക്കുന്ന ആളുടെ") ||
                s.contains("പരിചാരകന്റെ") || s.contains("സഹായിയുടെ") ||
                s.contains("കെയർഗിവറിന്റെ") || s.contains("കെയർഗിവറുടെ") ||
                s.contains("കെയർടേക്കറിന്റെ") || s.contains("കെയർടേക്കറുടെ") ||
                s.contains("കെയർഗിവർ സന്ദേശം") || s.contains("കെയർഗിവർ മെസേജ്") ||
                s.contains("കെയർടേക്കർ സന്ദേശം") || s.contains("കെയർടേക്കർ മെസേജ്");
    }

    public static boolean isGenericMalayalamCaregiverInquiry(@NonNull String s) {
        if (!containsMalayalamCaregiverAny(s)) {
            return false;
        }
        if (s.contains("ആരാണ്") || s.contains("എന്താണ്") ||
                s.contains("കുറിച്ച്") || s.contains("ക്കുറിച്ച്") ||
                s.contains("വിവരങ്ങൾ") || s.contains("വേണം")) {
            return true;
        }
        return false;
    }

    public static boolean containsEnglishCaregiverAny(@NonNull String s, @NonNull Set<String> words) {
        return words.contains("caregiver") || words.contains("carer") ||
                words.contains("caretaker") || words.contains("helper") ||
                words.contains("attendant") || words.contains("guardian") ||
                words.contains("assistant") ||
                s.contains("care worker") || s.contains("person taking care of me");
    }

    public static boolean isCancelMessagePhrase(@Nullable String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) return false;
        String s = normalizeText(rawText);
        if (s.isEmpty()) return false;
        if (s.equals("cancel") || s.equals("cancel message") || s.equals("don t send") ||
                s.equals("dont send") || s.equals("do not send") || s.equals("never mind") ||
                s.equals("stop") || s.equals("abort") ||
                s.equals("ಬೇಡ") || s.equals("ರದ್ದು") || s.equals("ರದ್ದು ಮಾಡಿ") ||
                s.equals("ಕಳುಹಿಸಬೇಡಿ") || s.equals("ಕಳಿಸಬೇಡಿ") ||
                s.equals("नहीं") || s.equals("रद्द") || s.equals("रद्द करो") ||
                s.equals("मत भेजो") || s.equals("संदेश मत भेजो") ||
                s.equals("വേണ്ട") || s.equals("റദ്ദാക്കുക") || s.equals("അയക്കരുത്") ||
                s.equals("സന്ദേശം അയക്കരുത്")) {
            return true;
        }
        if (matchIntent(rawText).getIntentType() == VoiceIntentType.CONFIRM_NO) {
            return true;
        }
        return false;
    }

    public static boolean isRecordingStopPhrase(@Nullable String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) return false;
        String s = normalizeText(rawText);
        if (s.isEmpty()) return false;
        Set<String> words = new HashSet<>(Arrays.asList(s.split(" ")));
        // English
        if (s.equals("stop recording") || s.equals("finish recording") || s.equals("end recording") ||
                s.equals("done") || s.equals("i am done") || s.equals("im done") || s.equals("i m done") ||
                s.equals("that s all") || s.equals("thats all") || s.equals("that is all") ||
                s.equals("stop") || s.equals("send it") || s.equals("send") ||
                s.equals("finish my message") || s.equals("finish message") || s.equals("completed") ||
                s.equals("finished") ||
                containsMatch(s, words, "stop recording", "finish recording", "done recording", "i am done", "thats all", "finish my message", "finished")) {
            return true;
        }
        // Kannada
        if (s.equals("ರೆಕಾರ್ಡಿಂಗ್ ನಿಲ್ಲಿಸು") || s.equals("ರೆಕಾರ್ಡಿಂಗ್ ನಿಲ್ಲಿಸಿ") ||
                s.equals("ನಿಲ್ಲಿಸು") || s.equals("ನಿಲ್ಲಿಸಿ") ||
                s.equals("ಮಾತು ಮುಗಿತು") || s.equals("ಮಾತು ಮುಗಿಯಿತು") ||
                s.equals("ಮುಗಿಯಿತು") || s.equals("ಮುಗಿತು") || s.equals("ಮುಗೀತು") ||
                s.equals("ಇಷ್ಟು ಸಾಕು") || s.equals("ಸಾಕು") ||
                s.equals("ಸಂದೇಶ ಮುಗಿತು") || s.equals("ಸಂದೇಶ ಮುಗಿಯಿತು") ||
                s.contains("ರೆಕಾರ್ಡಿಂಗ್ ನಿಲ್ಲಿಸು") || s.contains("ರೆಕಾರ್ಡಿಂಗ್ ನಿಲ್ಲಿಸಿ") ||
                s.contains("ರೆಕಾರ್ಡ್ ನಿಲ್ಲಿಸು") || s.contains("ರೆಕಾರ್ಡ್ ನಿಲ್ಲಿಸಿ") ||
                s.contains("ಮಾತು ಮುಗಿತು") || s.contains("ಸಂದೇಶ ಮುಗಿತು") ||
                s.contains("ಮುಗೀತು") || words.contains("ನಿಲ್ಲಿಸು") || words.contains("ನಿಲ್ಲಿಸಿ") ||
                (s.contains("ರೆಕಾರ್ಡಿಂಗ್") && (s.contains("ನಿಲ್ಲಿಸು") || s.contains("ನಿಲ್ಲಿಸಿ") || s.contains("ಸ್ಟಾಪ್")))) {
            return true;
        }
        // Hindi
        if (s.equals("रिकॉर्डिंग बंद करो") || s.equals("रिकॉर्डिंग खत्म करो") || s.equals("हो गया") ||
                s.equals("बस") || s.equals("मैंने बोल दिया") || s.equals("संदೇಶ पूरा हो गया") ||
                s.equals("संदेश पूरा हो गया") || s.equals("पूरा हो गया") || s.equals("बंद करो") ||
                s.contains("रिकॉर्डिंग बंद") || s.contains("रिकॉर्डिंग खत्म")) {
            return true;
        }
        // Malayalam
        if (s.equals("റെക്കോർഡിംഗ് നിർത്തൂ") || s.equals("റെക്കോർഡിംഗ് കഴിഞ്ഞു") || s.equals("കഴിഞ്ഞു") ||
                s.equals("നിർത്തൂ") || s.equals("ഇത്ര മതി") || s.equals("മതി") ||
                s.equals("സന്ദേശം കഴിഞ്ഞു") || s.contains("റെക്കോർഡിംഗ് നിർത്തൂ") || s.contains("റെക്കോർഡിംഗ് കഴിഞ്ഞു")) {
            return true;
        }
        return false;
    }

    public static boolean isRecordingCancelPhrase(@Nullable String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) return false;
        String s = normalizeText(rawText);
        if (s.isEmpty()) return false;
        if (s.equals("cancel") || s.equals("cancel recording") || s.equals("don t send") ||
                s.equals("dont send") || s.equals("do not send") || s.equals("don t send it") ||
                s.equals("dont send it") || s.equals("do not send it") || s.equals("never mind") ||
                s.equals("stop recording and cancel") || s.equals("stop and cancel") || s.equals("abort") || s.equals("discard") ||
                s.equals("discard recording") || s.equals("cancel message") ||
                // Kannada
                s.equals("ಬೇಡ") || s.equals("ರದ್ದು") || s.equals("ರದ್ದು ಮಾಡಿ") ||
                s.equals("ರೆಕಾರ್ಡಿಂಗ್ ರದ್ದು") || s.equals("ರೆಕಾರ್ಡಿಂಗ್ ರದ್ದು ಮಾಡಿ") ||
                s.equals("ಕಳುಹಿಸಬೇಡಿ") || s.equals("ಕಳಿಸಬೇಡಿ") ||
                // Hindi
                s.equals("नहीं") || s.equals("रद्द") || s.equals("रद्द करो") ||
                s.equals("रिकॉर्डिंग रद्द करो") || s.equals("मत भेजो") || s.equals("संदेश मत भेजो") ||
                // Malayalam
                s.equals("വേണ്ട") || s.equals("റദ്ദാക്കുക") || s.equals("റെക്കോർഡിംഗ് റദ്ദാക്കുക") ||
                s.equals("അയക്കരുത്") || s.equals("സന്ദേശം അയക്കരുത്")) {
            return true;
        }
        if (matchIntent(rawText).getIntentType() == VoiceIntentType.CONFIRM_NO) {
            return true;
        }
        return false;
    }

    public static boolean isAffirmativeResponse(@Nullable String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) return false;
        String s = normalizeText(rawText);
        Set<String> words = new HashSet<>(Arrays.asList(s.split(" ")));
        return s.equals("yes") || s.equals("yeah") || s.equals("yep") || s.equals("sure") || s.equals("ok") ||
                words.contains("yes") || words.contains("send") || words.contains("confirm") ||
                words.contains("proceed") || words.contains("okay") || words.contains("ok") ||
                s.equals("send it") || s.equals("yes send") || s.equals("yes send it") ||
                s.equals("yes please") || words.contains("हाँ") || words.contains("हा") ||
                s.equals("ಹೌದು") || s.equals("ಸರಿ") || s.equals("ಕಳುಹಿಸು") || s.equals("ಕಳುಹಿಸಿ") ||
                s.equals("ಕಳಿಸು") || s.equals("ಕಳಿಸಿ") || s.equals("ಖಂಡಿತ") ||
                s.equals("भेजो") || s.equals("भेजें") || s.equals("भेज दो") ||
                s.equals("ठीक है") || s.equals("ज़रूर") || s.equals("जरूर") ||
                s.equals("അതെ") || s.equals("ശരി") || s.equals("അയക്കൂ") || s.equals("അയക്കുക") ||
                s.equals("തീർച്ചയായും") ||
                matchIntent(rawText).getIntentType() == VoiceIntentType.CONFIRM_YES;
    }

    public static boolean isNegativeResponse(@Nullable String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) return false;
        String s = normalizeText(rawText);
        Set<String> words = new HashSet<>(Arrays.asList(s.split(" ")));
        return s.equals("no") || s.equals("cancel") || s.equals("don t") || s.equals("dont") || s.equals("do not") ||
                s.equals("never mind") || s.equals("no cancel") || s.equals("cancel action") ||
                words.contains("no") || words.contains("cancel") || s.contains("don t") ||
                s.contains("dont") || s.contains("do not") || s.equals("stop") ||
                s.equals("don't send") || s.equals("dont send") || s.equals("discard") ||
                s.equals("ಇಲ್ಲ") || s.equals("ಬೇಡ") || s.equals("ರದ್ದು") || s.equals("ರದ್ದು ಮಾಡಿ") ||
                s.equals("ಕ್ಯಾನ್ಸಲ್ ಮಾಡಿ") ||
                s.equals("नहीं") || s.equals("नही") || s.equals("मत करो") || s.equals("रद्द") || s.equals("मत भेजो") ||
                s.equals("कैंसल करो") || s.equals("रद्द करो") ||
                s.equals("വേണ്ട") || s.equals("റദ്ദാക്കുക") || s.equals("അയക്കരുത്") ||
                matchIntent(rawText).getIntentType() == VoiceIntentType.CONFIRM_NO;
    }

    public static boolean isAmbiguousHelpRequest(@Nullable String rawText) {
        if (rawText == null) return false;
        String s = normalizeText(rawText);
        if (s.isEmpty()) return false;
        if (containsKannadaCaregiverAny(s) || containsHindiCaregiverAny(s) ||
                containsMalayalamCaregiverAny(s)) {
            return false;
        }
        Set<String> words = new HashSet<>(Arrays.asList(s.split(" ")));
        if (containsEnglishCaregiverAny(s, words)) {
            return false;
        }
        if (s.contains("emergency") || s.contains("alert") || s.contains("sos") ||
                s.contains("ತುರ್ತು") || s.contains("ಕಾಪಾಡಿ") || s.contains("ಆಪತ್ಕಾಲೀನ") ||
                s.contains("आपातकाल") || s.contains("बचाओ") || s.contains("खतरे") ||
                s.contains("അടിയന്തര") || s.contains("രക്ഷിക്കൂ")) {
            return false;
        }
        // English ambiguous help phrases
        if (s.equals("help") || s.equals("i need help") || s.equals("please help me") ||
                s.equals("help me") || s.equals("need help") || s.equals("can you help me") ||
                s.equals("can you help") || s.equals("i want help") || s.equals("get help")) {
            return true;
        }
        // Kannada ambiguous help phrases
        if (s.equals("ಸಹಾಯ") || s.equals("ಸಹಾಯ ಬೇಕು") || s.equals("ನನಗೆ ಸಹಾಯ ಬೇಕು") ||
                s.equals("ಸಹಾಯ ಮಾಡಿ") || s.equals("ದಯವಿಟ್ಟು ಸಹಾಯ ಮಾಡಿ") || s.equals("ಸಹಾಯ ಮಾಡು")) {
            return true;
        }
        // Hindi ambiguous help phrases
        if (s.equals("मदद") || s.equals("मदद चाहिए") || s.equals("मुझे मदद चाहिए") ||
                s.equals("मदद करो") || s.equals("कृपया मदद करो") || s.equals("सहायता") || s.equals("सहायता चाहिए")) {
            return true;
        }
        // Malayalam ambiguous help phrases
        if (s.equals("സഹായം") || s.equals("സഹായം വേണം") || s.equals("എനിക്ക് സഹായം വേണം") ||
                s.equals("സഹായിക്കൂ") || s.equals("ദയവായി സഹായിക്കൂ")) {
            return true;
        }
        return false;
    }

    public static boolean isAmbiguousCallRequest(@Nullable String rawText) {
        if (rawText == null) return false;
        String s = normalizeText(rawText);
        if (s.isEmpty()) return false;
        if (containsKannadaCaregiverAny(s) || containsHindiCaregiverAny(s) ||
                containsMalayalamCaregiverAny(s)) {
            return false;
        }
        Set<String> words = new HashSet<>(Arrays.asList(s.split(" ")));
        if (containsEnglishCaregiverAny(s, words)) {
            return false;
        }
        // English ambiguous call phrases
        if (s.equals("call someone") || s.equals("can you call someone") || s.equals("call somebody") ||
                s.equals("can you call") || s.equals("make a call") || s.equals("i want to call someone") ||
                s.equals("call") || s.equals("phone") || s.equals("make call") || s.equals("place a call") ||
                s.equals("dial someone")) {
            return true;
        }
        // Kannada
        if (s.equals("ಯಾರಿಗಾದರೂ ಕರೆ ಮಾಡಿ") || s.equals("ಕರೆ ಮಾಡಿ") || s.equals("ಕಾಲ್ ಮಾಡಿ") ||
                s.equals("ಫೋನ್ ಮಾಡಿ") || s.equals("ಕರೆ ಮಾಡು") || s.equals("ಯಾರಿಗಾದರೂ ಕಾಲ್ ಮಾಡಿ") ||
                s.equals("ಕರೆ") || s.equals("ಕಾಲ್") || s.equals("ಫೋನ್")) {
            return true;
        }
        // Hindi
        if (s.equals("किसी को कॉल करो") || s.equals("कॉल करो") || s.equals("फोन करो") ||
                s.equals("किसी को फोन लगाओ") || s.equals("कॉल लगाओ") || s.equals("फोन लगाओ") ||
                s.equals("किसी को कॉल करें") || s.equals("कॉल") || s.equals("फोन")) {
            return true;
        }
        // Malayalam
        if (s.equals("ആരെയെങ്കിലും വിളിക്കൂ") || s.equals("വിളിക്കൂ") || s.equals("ഫോൺ ചെയ്യൂ") ||
                s.equals("കോൾ ചെയ്യൂ") || s.equals("വിളിക്കുക") || s.equals("ആരെയെങ്കിലും വിളിക്കുക") ||
                s.equals("കോൾ") || s.equals("ഫോൺ")) {
            return true;
        }
        return false;
    }

    public static boolean isAmbiguousMessageRequest(@Nullable String rawText) {
        if (rawText == null) return false;
        String s = normalizeText(rawText);
        if (s.isEmpty()) return false;
        if (containsKannadaCaregiverAny(s) || containsHindiCaregiverAny(s) ||
                containsMalayalamCaregiverAny(s)) {
            return false;
        }
        Set<String> words = new HashSet<>(Arrays.asList(s.split(" ")));
        if (containsEnglishCaregiverAny(s, words)) {
            return false;
        }
        // English ambiguous send phrases
        if (s.equals("i want to send something") || s.equals("send something") ||
                s.equals("send someone a message") || s.equals("i need to send something") ||
                s.equals("send") || s.equals("send a message to someone") || s.equals("send to someone") ||
                s.equals("i want to send a message") || s.equals("send a message")) {
            return true;
        }
        // Kannada
        if (s.equals("ಏನನ್ನಾದರೂ ಕಳುಹಿಸಬೇಕು") || s.equals("ಏನಾದರೂ ಕಳುಹಿಸು") || s.equals("ಏನಾದರೂ ಕಳುಹಿಸಿ") ||
                s.equals("ಯಾರಿಗಾದರೂ ಸಂದೇಶ ಕಳುಹಿಸಿ") || s.equals("ಕಳುಹಿಸು") || s.equals("ಕಳುಹಿಸಿ") ||
                s.equals("ಏನಾದರೂ ಕಳಿಸಿ") || s.equals("ಸಂದೇಶ ಕಳುಹಿಸು") || s.equals("ಸಂದೇಶ ಕಳುಹಿಸಿ")) {
            return true;
        }
        // Hindi
        if (s.equals("कुछ भेजना है") || s.equals("कुछ भेजो") || s.equals("किसी को संदेश भेजना है") ||
                s.equals("भेजो") || s.equals("भेजें") || s.equals("किसी को मैसेज भेजो") ||
                s.equals("संदेश भेजो") || s.equals("मैसेज भेजो")) {
            return true;
        }
        // Malayalam
        if (s.equals("എന്തെങ്കിലും അയക്കണം") || s.equals("എന്തെങ്കിലും അയക്കൂ") ||
                s.equals("ആർക്കെങ്കിലും സന്ദേശം അയക്കണം") || s.equals("അയക്കൂ") || s.equals("അയക്കുക") ||
                s.equals("സന്ദേശം അയക്കൂ") || s.equals("സന്ദേശം അയക്കുക")) {
            return true;
        }
        return false;
    }

    public static boolean isConversationalOrNonActionPhrase(@NonNull String s) {
        if (s.contains("yesterday") || s.contains("talked to") || s.contains("talked with") ||
                s.contains("spoke to") || s.contains("spoke with") || s.contains("last week") ||
                s.contains("is complete") || s.contains("was complete") || s.contains("are complete")) {
            return true;
        }
        return false;
    }

    public static boolean isExplicitActionNegation(@NonNull String s) {
        if (s.equals("no") || s.equals("cancel") || s.equals("don t") || s.equals("dont") || s.equals("do not") ||
                s.equals("never mind") || s.equals("ಇಲ್ಲ") || s.equals("ಬೇಡ") || s.equals("नहीं") || s.equals("नही") ||
                s.equals("मत करो") || s.equals("വേണ്ട") || s.equals("റദ്ദാക്കുക")) {
            return false;
        }
        if (s.startsWith("don t ") || s.startsWith("do not ") || s.startsWith("dont ") || s.startsWith("never ") ||
                s.contains("don t ") || s.contains("do not ") || s.contains("dont ") ||
                s.contains("ಬೇಡಿ") ||
                s.contains("मत ") || s.contains(" मत") ||
                s.contains("അരുത്") || s.endsWith("രുത്") || s.contains("രുത് ")) {
            return true;
        }
        return false;
    }

    public static boolean isGenericEnglishCaregiverInquiry(@NonNull String s, @NonNull Set<String> words) {
        if (s.equals("who is my caregiver") || s.equals("who is my carer") ||
                s.equals("what is a caretaker") || s.equals("what is a care worker") ||
                s.equals("who is my helper") || s.equals("what is a caregiver") ||
                s.equals("what is a carer") || s.equals("who is a carer") ||
                s.equals("what does a care worker mean") || s.equals("who is the care worker") ||
                s.equals("tell me about my caregiver") || s.equals("tell me about the helper") ||
                s.equals("tell me about my helper") || s.equals("what is a helper") ||
                s.equals("i need a helper") || s.equals("i need help") ||
                s.equals("help me") || s.equals("find a helper")) {
            return true;
        }
        if (s.startsWith("who is ") || s.startsWith("what is ") || s.startsWith("tell me about ") ||
                s.startsWith("what does ") || s.startsWith("find a ") ||
                (s.startsWith("i need ") && !s.contains("talk") && !s.contains("speak") && !s.contains("call") && !s.contains("contact") && !s.contains("reach"))) {
            if (containsEnglishCaregiverAny(s, words) || s.contains("help")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIsolatedNegativeToken(@NonNull String s) {
        return s.equals("caregiver") || s.equals("carer") ||
                s.equals("caretaker") || s.equals("care worker") ||
                s.equals("helper") || s.equals("call") ||
                s.equals("phone") || s.equals("ring") ||
                s.equals("dial") || s.equals("message") ||
                s.equals("messages") || s.equals("repeat") ||
                s.equals("again") || s.equals("call my") ||
                s.equals("phone my") ||
                // Kannada
                s.equals("ಆರೈಕೆದಾರ") || s.equals("ಆರೈಕೆದಾರರು") ||
                s.equals("ಸಹಾಯಕ") || s.equals("ಸಹಾಯಕರು") ||
                s.equals("ಪಾಲಕ") || s.equals("ಪಾಲಕರು") ||
                s.equals("ಕೇರ್ಗಿವರ್") || s.equals("ಕೇರ್ಟೇಕರ್") ||
                s.equals("ಕರೆ ಮಾಡಿ") || s.equals("ಕಾಲ್ ಮಾಡಿ") || s.equals("ಫೋನ್ ಮಾಡಿ") ||
                s.equals("ಕರೆ ಮಾಡು") || s.equals("ಕಾಲ್ ಮಾಡು") || s.equals("ಫೋನ್ ಮಾಡು") ||
                s.equals("ಕರೆ") || s.equals("ಕಾಲ್") || s.equals("ಫೋನ್") ||
                s.equals("ಸಂದೇಶ") || s.equals("ಮೆಸೇಜ್") ||
                s.equals("ಸಹಾಯ") ||
                // Hindi Task 25
                s.equals("देखभाल करने वाला") || s.equals("देखभाल करने वाले") ||
                s.equals("सहायक") || s.equals("केयरगिवर") || s.equals("केयरटेकर") ||
                s.equals("कॉल") || s.equals("फोन") || s.equals("कॉल करो") || s.equals("फोन करो") ||
                s.equals("संदेश") || s.equals("मैसेज") ||
                // Malayalam Task 26
                s.equals("പരിചരിക്കുന്നയാൾ") || s.equals("പരിചരിക്കുന്നയാളെ") ||
                s.equals("പരിചരിക്കുന്ന ആളെ") || s.equals("പരിചരിക്കുന്ന ആൾ") ||
                s.equals("പരിചാരകൻ") || s.equals("പരിചാരകനെ") ||
                s.equals("സഹായി") || s.equals("സഹായിയെ") ||
                s.equals("കെയർഗിവർ") || s.equals("കെയർഗിവറെ") ||
                s.equals("കെയർടേക്കർ") || s.equals("കെയർടേക്കറെ") ||
                s.equals("വിളിക്കൂ") || s.equals("വിളിക്കുക") ||
                s.equals("ഫോൺ ചെയ്യൂ") || s.equals("കോൾ ചെയ്യൂ") ||
                s.equals("ഫോൺ") || s.equals("കോൾ") ||
                s.equals("സന്ദേശം") || s.equals("മെസേജ്") ||
                s.equals("വീണ്ടും") || s.equals("പറയൂ") || s.equals("കേൾപ്പിക്കൂ");
    }

    /**
     * Matches input speech and returns the detected VoiceIntent.
     */
    @NonNull
    public static VoiceIntent matchIntent(@NonNull String rawQuery) {
        return matchIntent(rawQuery, "en");
    }

    /**
     * Matches input speech and directly returns the detected VoiceIntentType.
     */
    @NonNull
    public static VoiceIntentType match(@NonNull String rawQuery) {
        return matchIntent(rawQuery, "en").getIntentType();
    }

    /**
     * Matches input speech against the clear VoiceIntentType model using flexible token & phrase heuristics.
     */
    @NonNull
    public static VoiceIntent matchIntent(@NonNull String rawQuery, @NonNull String languageCode) {
        String s = normalizeText(rawQuery);
        if (s.isEmpty()) {
            return new VoiceIntent(VoiceIntentType.UNKNOWN, rawQuery, 0.0f, null);
        }

        Set<String> words = new HashSet<>(Arrays.asList(s.split(" ")));

        // Critical safety checks: reject isolated words and generic non-action inquiries immediately
        if (isIsolatedNegativeToken(s) || isGenericEnglishCaregiverInquiry(s, words) ||
                isGenericCaregiverInquiry(s) || isGenericHindiCaregiverInquiry(s) || isGenericMalayalamCaregiverInquiry(s) ||
                isConversationalOrNonActionPhrase(s)) {
            return new VoiceIntent(VoiceIntentType.UNKNOWN, rawQuery, 0.0f, null);
        }

        // Action Negation Protection: Explicit commands like "Don't call my caregiver", "Do not send message", "ಕರೆ ಮಾಡಬೇಡಿ", "कॉल मत करो", "വിളിക്കരുത്"
        if (isExplicitActionNegation(s)) {
            return new VoiceIntent(VoiceIntentType.UNKNOWN, rawQuery, 0.0f, null);
        }

        // 0. Confirmation Responses
        if (s.equals("yes") || s.equals("yeah") || s.equals("yep") || s.equals("sure") || s.equals("ok") ||
                words.contains("हाँ") || words.contains("हा") ||
                containsMatch(s, words,
                        "confirm", "proceed", "yes please", "yes call", "yes send", "please do",
                        "ಹೌದು", "ಸರಿ", "ಖಂಡಿತ",
                        "ठीक है", "ज़रूर", "जरूर",
                        "അതെ", "ശരി", "തീർച്ചയായും")) {
            return new VoiceIntent(VoiceIntentType.CONFIRM_YES, rawQuery, 1.0f, null);
        }

        if (s.equals("no") || s.equals("cancel") || s.equals("don t") || s.equals("dont") || s.equals("do not") ||
                s.equals("never mind") || s.equals("no cancel") || s.equals("cancel action") ||
                s.equals("ಇಲ್ಲ") || s.equals("ಬೇಡ") || s.equals("ರದ್ದು") ||
                s.equals("नहीं") || s.equals("नही") || s.equals("मत करो") || s.equals("रद्द") ||
                s.equals("വേണ്ട") || s.equals("റദ്ദാക്കുക") ||
                containsMatch(s, words, "never mind", "no cancel", "cancel action", "do not proceed", "don t proceed")) {
            return new VoiceIntent(VoiceIntentType.CONFIRM_NO, rawQuery, 1.0f, null);
        }

        // 1. Accessibility: Stop Listening
        if (!s.contains("ಕರೆ") && !s.contains("ಕಾಲ್") && !s.contains("ಫೋನ್") && !s.contains("call") && !s.contains("phone")
                && !s.contains("record") && !s.contains("recording")
                && !s.contains("ರೆಕಾರ್ಡ್") && !s.contains("ರೆಕಾರ್ಡಿಂಗ್")
                && !s.contains("रिकॉर्ड") && !s.contains("रिकॉर्डिंग")
                && !s.contains("റെക്കോർഡ്") && !s.contains("റെക്കോർഡിംഗ്")) {
            if (s.equals("stop") || s.equals("quiet") || s.equals("pause") || containsMatch(s, words,
                    "stop listening", "cancel listening", "end listening", "stop assistant", "stop voice assistant",
                    "pause assistant", "pause voice assistant", "pause listening",
                    "quiet", "shut up", "halt",
                    "be quiet", "don t listen",
                    "ಕೇಳುವುದನ್ನು ನಿಲ್ಲಿಸಿ", "ನಿಲ್ಲಿಸಿ", "ಸಾಕು", "ಆಲಿಸುವುದನ್ನು ನಿಲ್ಲಿಸಿ", "ಸುಮ್ಮನಿರಿ",
                    "सुनना बंद करो", "बंद करो", "रुक जाओ", "शांत", "शांत रहो", "सुनना बंद कीजिए",
                    "കേൾക്കുന്നത് നിർത്തുക", "നിർത്തൂ", "മതി", "മിണ്ടാതിരിക്കൂ", "കേൾക്കണ്ട")) {
                return new VoiceIntent(VoiceIntentType.STOP_LISTENING, rawQuery, 1.0f, null);
            }
        }

        // 2. Accessibility: Start Listening
        if (s.equals("listen") || s.equals("resume") || containsMatch(s, words,
                "start listening", "begin listening", "resume listening", "start assistant", "start voice assistant",
                "resume assistant", "resume voice assistant",
                "assistant listen", "listen to me",
                "ಕೇಳಲು ಪ್ರಾರಂಭಿಸಿ", "ಕೇಳಿಸಿಕೊಳ್ಳಿ", "ಆಲಿಸಿ", "ನನ್ನ ಮಾತು ಕೇಳಿ",
                "सुनना शुरू करो", "सुनो", "सुनिए", "मेरी बात सुनो",
                "കേൾക്കാൻ തുടങ്ങുക", "കേൾക്കൂ", "ശ്രദ്ധിക്കൂ", "പറയുന്നത് കേൾക്കൂ")) {
            return new VoiceIntent(VoiceIntentType.START_LISTENING, rawQuery, 1.0f, null);
        }

        // 3. Emergency Alert
        if (containsMatch(s, words,
                "emergency alert", "open emergency alert", "trigger emergency alert", "send emergency alert",
                "ತುರ್ತು ಎಚ್ಚರಿಕೆ", "ಆಪತ್ಕಾಲೀನ ಅಲರ್ಟ್",
                "आपातकालीन अलर्ट", "इमरजेंसी अलर्ट",
                "അടിയന്തര മുന്നറിയിപ്പ്", "എമർജൻസി അലർട്ട്")) {
            return new VoiceIntent(VoiceIntentType.OPEN_EMERGENCY_ALERT, rawQuery, 1.0f, null);
        }

        // 4. Emergency
        if (matchesEmergency(s, words)) {
            return new VoiceIntent(VoiceIntentType.OPEN_EMERGENCY, rawQuery, 1.0f, null);
        }

        // 4.5 Accept / Receive Incoming Call
        if (matchesAcceptCall(s, words)) {
            return new VoiceIntent(VoiceIntentType.ACCEPT_CALL, rawQuery, 1.0f, null);
        }

        // 4.6 End Active Call
        if (matchesEndCall(s, words)) {
            return new VoiceIntent(VoiceIntentType.END_CALL, rawQuery, 1.0f, null);
        }

        // 5. Call Caregiver / Voice Call
        if (matchesCallCaregiver(s, words)) {
            return new VoiceIntent(VoiceIntentType.CALL_CAREGIVER, rawQuery, 1.0f, null);
        }

        // 5.8 Record Voice Message
        if (matchesRecordVoiceMessage(s, words)) {
            return new VoiceIntent(VoiceIntentType.RECORD_VOICE_MESSAGE, rawQuery, 1.0f, null);
        }

        // 6. Voice Recorder / Voice Message
        if (matchesVoiceRecorderOrMessage(s, words)) {
            if (s.contains("voice messages") || s.contains("open voice messages") ||
                    s.contains("ಧ್ವನಿ ಸಂದೇಶಗಳು") || s.contains("ശബ്ദ സന്ദേശങ്ങൾ")) {
                return new VoiceIntent(VoiceIntentType.OPEN_VOICE_MESSAGE, rawQuery, 1.0f, null);
            }
            return new VoiceIntent(VoiceIntentType.OPEN_VOICE_RECORDER, rawQuery, 1.0f, null);
        }

        // 7. Communication
        if (containsMatch(s, words,
                "open communication", "show communication", "go to communication", "communication", "talk",
                "interactive chat", "open interactive chat", "open chat", "show chat", "start chat",
                "chat",
                "ಸಂವಾದವನ್ನು ತೆರೆಯಿರಿ", "ಸಂವಾದ ತೆರೆಯಿರಿ", "ಸಂವಾದ ತೋರಿಸಿ", "ಸಂವಾದ",
                "ಸಹಾಯಕರೊಂದಿಗೆ ಸಂವಾದ", "ಸಹಾಯಕರ ಚಾಟ್", "ಚಾಟ್ ತೆರೆಯಿರಿ", "ಚಾಟ್ ತೋರಿಸಿ", "ಚಾಟ್",
                "ಸಂವಹನ",
                "संचार", "बातचीत",
                "ആശയവിനിമയം")) {
            return new VoiceIntent(VoiceIntentType.OPEN_COMMUNICATION, rawQuery, 1.0f, null);
        }

        // 7a. Repeat Message
        if (matchesRepeatMessage(s, words)) {
            return new VoiceIntent(VoiceIntentType.REPEAT_MESSAGE, rawQuery, 1.0f, null);
        }

        // 7b. Message Count
        if (matchesMessageCount(s, words)) {
            return new VoiceIntent(VoiceIntentType.MESSAGE_COUNT, rawQuery, 1.0f, null);
        }

        // 7ca. Read Notifications
        if (matchesReadNotifications(s, words)) {
            return new VoiceIntent(VoiceIntentType.READ_NOTIFICATIONS, rawQuery, 1.0f, null);
        }

        // 7c. Read Messages
        if (matchesReadMessages(s, words)) {
            return new VoiceIntent(VoiceIntentType.READ_MESSAGES, rawQuery, 1.0f, null);
        }

        // 7d. Send Message
        if (matchesSendMessage(s, words)) {
            return new VoiceIntent(VoiceIntentType.SEND_MESSAGE, rawQuery, 1.0f, null);
        }

        // 7e. Go Back
        if (matchesGoBack(s, words)) {
            return new VoiceIntent(VoiceIntentType.GO_BACK, rawQuery, 1.0f, null);
        }

        // 8. Messages
        if (matchesMessages(s, words)) {
            return new VoiceIntent(VoiceIntentType.OPEN_MESSAGES, rawQuery, 1.0f, null);
        }

        // 9. Caregiver Connection
        if (containsMatch(s, words,
                "caregiver connection", "open caregiver connection", "open connection", "open connections",
                "connection", "connections", "show connection", "show connections",
                "helper connection", "open helper connection", "open helper", "caregiver page", "helper page",
                "caregiver screen", "helper screen",
                "caregiver connection page", "caregiver conection page", "caregiver connection screen",
                "helper connection page", "helpers page",
                "open caregiver page", "open caregiver connection page", "go to caregiver connection page", "go to caregiver page",
                "ಸಹಾಯಕರ ಸಂಪರ್ಕ", "ಸಹಾಯಕರ ಸಂಪರ್ಕ ತೆರೆಯಿರಿ", "ಸಹಾಯಕರ ಸಂಪರ್ಕ ತೋರಿಸಿ", "ಸಹಾಯಕರ ಪುಟ",
                "ಸಹಾಯಕರ ಸ್ಕ್ರೀನ್", "ಸಹಾಯಕರನ್ನು ತೋರಿಸಿ", "ಸಹಾಯಕರು", "ಸಹಾಯಕರ ಪುಟ ತೆರೆಯಿರಿ",
                "ಸಹಾಯಕ ಪುಟ", "ಸಹಾಯಕರಿಗೆ ಹೋಗಿ", "ಸಹಾಯಕರ ಸಂಪರ್ಕ ಪುಟ", "ಸಹಾಯಕರಿಗೆ ಹೋಗು",
                "ಸಹಾಯಕರ ಪುಟಕ್ಕೆ ಹೋಗು", "ಸಹಾಯಕರ ಪುಟಕ್ಕೆ ಹೋಗಿ", "ಸಹಾಯಕರ ಸಂಪರ್ಕ ಪುಟಕ್ಕೆ ಹೋಗು", "ಸಹಾಯಕರ ಸಂಪರ್ಕ ಪುಟಕ್ಕೆ ಹೋಗಿ",
                "ಸಹಾಯಕರ ಕನೆಕ್ಷನ್", "ಸಹಾಯಕ ಕನೆಕ್ಷನ್", "ಸಹಾಯಕ ಪುಟಕ್ಕೆ ಹೋಗು", "ಸಹಾಯಕ ಪುಟಕ್ಕೆ ಹೋಗಿ",
                "ಪಾಲಕರ ಸಂಪರ್ಕ", "ಪಾಲಕರ ಸಂಪರ್ಕ ತೆರೆಯಿರಿ", "ಪಾಲಕರನ್ನು ತೋರಿಸಿ", "ಪಾಲಕರ ಪುಟ",
                "ಪಾಲಕರ ಪುಟಕ್ಕೆ ಹೋಗು", "ಪಾಲಕರ ಪುಟಕ್ಕೆ ಹೋಗಿ", "ಪಾಲಕರ ಪುಟ ತೆರೆಯಿರಿ", "ಪಾಲಕರ ಪುಟ ತೋರಿಸಿ", "ಪಾಲಕರಿಗೆ ಹೋಗು", "ಪಾಲಕರಿಗೆ ಹೋಗಿ", "ಪಾಲಕರ ಸ್ಕ್ರೀನ್",
                "ಕನೆಕ್ಷನ್", "ಸಂಪರ್ಕ", "ಸಂಪರ್ಕಗಳು", "ಜೋಡಣೆ",
                "कनेक्शन", "संपर्क", "केयरगिवर कनेक्शन", "सहायक कनेक्शन", "केयरगिवर पेज",
                "കണക്ഷൻ", "ബന്ധം", "കെയർഗിവർ കണക്ഷൻ", "സഹായി പേജ്")) {
            return new VoiceIntent(VoiceIntentType.OPEN_CAREGIVER_CONNECTION, rawQuery, 1.0f, null);
        }

        // 10. Caregiver
        if (!s.equals("caregiver") && !s.equals("carer") &&
                !s.equals("caretaker") && !s.equals("care worker") && !s.equals("helper") &&
                !s.equals("ಕೇರ್ಗಿವರ್") && !s.equals("ಕೇರ್‌ಗಿವರ್") &&
                !s.equals("ಆರೈಕೆದಾರ") && !s.equals("ಆರೈಕೆದಾರರು") &&
                !s.equals("ಸಹಾಯಕ") && !s.equals("ಸಹಾಯಕರು") &&
                !s.equals("ಪಾಲಕ") && !s.equals("ಪಾಲಕರು") &&
                !s.equals("ಕೇರ್ಟೇಕರ್") && !s.equals("ಕೇರ್ ಟೇಕರ್") &&
                !s.equals("केयरगिवर") && !s.equals("केयरटेकर") &&
                !s.equals("सहायक") && !s.equals("देखभाल करने वाला") && !s.equals("देखभाल करने वाले") &&
                !s.equals("കെയർഗിവർ") && !s.equals("കെയർടേക്കർ") &&
                !s.equals("സഹായി") && !s.equals("പരിചാരകൻ") &&
                !s.equals("പരിചരിക്കുന്നയാൾ") && !s.equals("പരിചരിക്കുന്നയാളെ")) {
            if (containsMatch(s, words,
                    "open caregiver", "show caregiver", "go to caregiver", "my caregiver", "open carer",
                    "open caretaker", "open care worker", "open helper", "guardian",
                    "ಕೇರ್‌ಗಿವರ್", "ಕೇರ್ಗಿವರ್", "ಸಹಾಯಕ", "ಆರೈಕೆದಾರ", "ಕೇರ್ಟೇಕರ್",
                    "open caregiver screen", "ಆರೈಕೆದಾರರನ್ನು ತೋರಿಸಿ", "ಸಹಾಯಕರನ್ನು ತೋರಿಸಿ", "ಪಾಲಕರನ್ನು ತೋರಿಸಿ",
                    "केयरगिवर", "सहायक",
                    "കെയർഗിവർ", "സഹായി")) {
                return new VoiceIntent(VoiceIntentType.OPEN_CAREGIVER, rawQuery, 1.0f, null);
            }
        }

        // 11. Settings
        if (matchesSettings(s, words)) {
            return new VoiceIntent(VoiceIntentType.OPEN_SETTINGS, rawQuery, 1.0f, null);
        }

        // 12. Profile
        if (matchesProfile(s, words)) {
            return new VoiceIntent(VoiceIntentType.OPEN_PROFILE, rawQuery, 1.0f, null);
        }

        // 13. Home
        if (matchesHome(s, words)) {
            return new VoiceIntent(VoiceIntentType.OPEN_HOME, rawQuery, 1.0f, null);
        }

        return new VoiceIntent(VoiceIntentType.UNKNOWN, rawQuery, 0.0f, null);
    }

    

    private static boolean matchesEmergency(String s, Set<String> words) {
        // Disallow generic single words such as "help", "emergency", "send", "alert" alone from triggering emergency
        if (s.equals("help") || s.equals("emergency") || s.equals("send") || s.equals("alert") ||
                s.equals("ಸಹಾಯ") || s.equals("ತುರ್ತು") || s.equals("ಮದದ") || s.equals("आपातकाल") ||
                s.equals("സഹായം") || s.equals("അടിയന്തരം")) {
            return false;
        }

        // Strict intent separation: Caregiver references are NEVER emergency triggers
        if (containsKannadaCaregiverAny(s) || containsHindiCaregiverAny(s) ||
                containsMalayalamCaregiverAny(s) || containsEnglishCaregiverAny(s, words)) {
            return false;
        }

        // Explicit command phrase matching for emergency
        if (containsMatch(s, words,
                "emergency help", "send emergency", "sos", "send sos",
                "emergency alert", "open emergency", "open emergency alert", "send emergency alert",
                "trigger emergency", "trigger emergency alert", "urgent help", "danger emergency",
                "save me", "help me danger", "i need emergency help", "i need urgent help", "get help immediately",
                // Kannada
                "ತುರ್ತು ಸಹಾಯ", "ತುರ್ತು ಸಹಾಯ ಬೇಕು", "ನನಗೆ ತುರ್ತು ಸಹಾಯ ಬೇಕು",
                "ಸಹಾಯ ಬೇಕು", "ನನಗೆ ಸಹಾಯ ಬೇಕು", "ಸಹಾಯ ಮಾಡಿ", "ನನಗೆ ಸಹಾಯ ಮಾಡಿ",
                "ತಕ್ಷಣ ಸಹಾಯ ಬೇಕು", "ನನಗೆ ತಕ್ಷಣ ಸಹಾಯ ಬೇಕು", "ತುರ್ತಾಗಿ ಸಹಾಯ ಬೇಕು",
                "ತುರ್ತು ಕಳುಹಿಸಿ", "ಕಾಪಾಡಿ", "ಎಸ್‌ಒಎಸ್", "ಎಸ್ ಓ ಎಸ್", "ತುರ್ತು ಎಚ್ಚರಿಕೆ", "ಆಪತ್ಕಾಲೀನ ಅಲರ್ಟ್",
                // Hindi
                "आपातकालीन सहायता", "इमरजेंसी सहायता", "एसओएस", "एस ओ एस", "आपातकालीन अलर्ट", "मदद चाहिए", "बचाओ",
                "मुझे तुरंत मदद चाहिए", "खतरे में हूँ मदद करो", "खतरे में हूं मदद करो", "इमरजेंसी भेजो",
                // Malayalam
                "അടിയന്തര സഹായം", "എസ് ഒ എസ്", "എമർജൻസി അലർട്ട്", "അടിയന്തര മുന്നറിയിപ്പ്", "രക്ഷിക്കൂ", "സഹായം വേണം",
                "എനിക്ക് അടിയന്തര സഹായം വേണം", "ഉടൻ സഹായം വേണം", "എന്നെ രക്ഷിക്കൂ", "അടിയന്തരമായി സഹായിക്കൂ")) {
            return true;
        }

        // Kannada heuristic: (ಸಹಾಯ / ತುರ್ತು / ಕಾಪಾಡಿ) + (ಬೇಕು / ಮಾಡಿ / ತಕ್ಷಣ / ಕಳುಹಿಸಿ / ಎಚ್ಚರಿಕೆ)
        boolean hasKannadaEmergency = (s.contains("ಸಹಾಯ") && !s.contains("ಸಹಾಯಕ")) || s.contains("ತುರ್ತು") || s.contains("ಕಾಪಾಡಿ");
        boolean hasKannadaUrgentAction = s.contains("ಬೇಕು") || s.contains("ಮಾಡಿ") || s.contains("ಕಳುಹಿಸಿ") || s.contains("ತಕ್ಷಣ") || s.contains("ಎಚ್ಚರಿಕೆ");
        if (hasKannadaEmergency && hasKannadaUrgentAction) {
            return true;
        }

        // Token combinations: action (send/open/trigger/start) + target (emergency/sos)
        boolean hasAction = words.contains("send") || words.contains("open") || words.contains("trigger") || words.contains("start");
        boolean hasTarget = words.contains("emergency") || words.contains("sos");
        if (hasAction && hasTarget) {
            return true;
        }
        return words.contains("emergency") && words.contains("help");
    }

    private static boolean matchesEndCall(String s, Set<String> words) {
        // Disallow accept / receive phrases from falsely matching end call (e.g. attend call)
        if (s.contains("attend") || s.contains("receive") || s.contains("accept") || s.contains("answer")) {
            return false;
        }

        // Disallow generic single words such as "end", "disconnect", "stop", "call" alone from triggering END_CALL
        if (s.equals("end") || s.equals("disconnect") || s.equals("stop") || s.equals("call") ||
                s.equals("phone") || s.equals("cut") ||
                s.equals("ಕರೆ") || s.equals("ಕಾಲ್") || s.equals("ಫೋನ್") ||
                s.equals("ಮುಗಿಸಿ") || s.equals("ನಿಲ್ಲಿಸಿ") || s.equals("ಕೊನೆಗೊಳಿಸಿ") || s.equals("ಮುಗಿಸು")) {
            return false;
        }

        if (containsMatch(s, words,
                "end call", "hang up", "disconnect call", "end the call", "hang up the call",
                "cut call", "stop call", "finish call", "close call", "disconnect", "please disconnect",
                "stop the call", "please stop the call", "end the phone call",
                "drop call", "cancel call", "drop the call", "finish the call", "cut the call",
                // Kannada
                "ಕರೆಯನ್ನು ಕೊನೆಗೊಳಿಸಿ", "ಕರೆಯನ್ನು ಮುಗಿಸಿ", "ಕರೆ ಮುಗಿಸಿ", "ಕರೆ ಕೊನೆಗೊಳಿಸಿ",
                "ಕರೆ ನಿಲ್ಲಿಸಿ", "ಕರೆ ಮುಗಿಸು", "ಕರೆಯನ್ನು ಮುಗಿಸು", "ಕರೆಯನ್ನು ನಿಲ್ಲಿಸಿ",
                "ಕರೆ ಮುಗಿಸಬೇಕು", "ಈ ಕರೆಯನ್ನು ಮುಗಿಸಿ", "ಈ ಕರೆಯನ್ನು ಕೊನೆಗೊಳಿಸಿ", "ಈ ಕರೆ ನಿಲ್ಲಿಸಿ",
                "ಕರೆ ಕಟ್ ಮಾಡಿ", "ಫೋನ್ ಕಟ್ ಮಾಡಿ", "ಕಾಲ್ ಮುಗಿಸಿ", "ಕಾಲ್ ಕಟ್ ಮಾಡಿ", "ಕಾಲ್ ಕೊನೆಗೊಳಿಸಿ",
                "ಫೋನ್ ಕೊನೆಗೊಳಿಸಿ", "ಫೋನ್ ಮುಗಿಸಿ", "ಕರೆಯನ್ನು ಕಟ್ ಮಾಡಿ", "ಫೋನ್ ಇಡಿ",
                // Hindi
                "कॉल समाप्त करो", "कॉल काटो", "कॉल बंद करो", "फ़ोन काटो", "फोन काटो", "कॉल खत्म करो",
                "बातचीत समाप्त करो", "कॉल डिस्कनेक्ट करो", "फोन रख दो", "कॉल रोक दो", "कॉल काटो प्लीज",
                // Malayalam
                "കോൾ അവസാനിപ്പിക്കുക", "കോൾ കട്ട് ചെയ്യുക", "ഫോൺ കട്ട് ചെയ്യുക",
                "കോൾ നിർത്തുക", "ഫോൺ വെക്കുക", "കോൾ വിച്ഛേദിക്കുക", "സംഭാഷണം അവസാനിപ്പിക്കൂ",
                "കോൾ കട്ട് ചെയ്യൂ", "ഫോൺ കട്ട് ചെയ്യൂ", "കോൾ എൻഡ് ചെയ്യുക")) {
            return true;
        }
        boolean hasKannadaEndAction = s.contains("ಕೊನೆಗೊಳಿಸಿ") || s.contains("ಮುಗಿಸಿ") || s.contains("ಮುಗಿಸು") ||
                s.contains("ನಿಲ್ಲಿಸಿ") || s.contains("ಕಟ್ ಮಾಡಿ") || s.contains("ಮುಗಿಸಬೇಕು");
        boolean hasKannadaCallNoun = s.contains("ಕರೆ") || s.contains("ಕಾಲ್") || s.contains("ಫೋನ್");
        if (hasKannadaEndAction && hasKannadaCallNoun) {
            return true;
        }
        boolean hasHindiEndAction = s.contains("समाप्त") || s.contains("काटो") || s.contains("बंद करो") ||
                s.contains("खत्म") || s.contains("डिस्कनेक्ट") || s.contains("रख दो") || s.contains("रोक");
        boolean hasHindiCallNoun = s.contains("कॉल") || s.contains("फोन") || s.contains("बातचीत");
        if (hasHindiEndAction && hasHindiCallNoun) {
            return true;
        }
        boolean hasMalayalamEndAction = s.contains("അവസാനിപ്പിക്കുക") || s.contains("കട്ട് ചെയ്യുക") ||
                s.contains("കട്ട് ചെയ്യൂ") || s.contains("നിർത്തുക") || s.contains("വെക്കുക");
        boolean hasMalayalamCallNoun = s.contains("കോൾ") || s.contains("ഫോൺ");
        if (hasMalayalamEndAction && hasMalayalamCallNoun) {
            return true;
        }
        boolean hasEndAction = words.contains("end") || words.contains("hang") || words.contains("cut") || words.contains("disconnect");
        boolean hasCallNoun = words.contains("call") || words.contains("phone");
        return hasEndAction && hasCallNoun;
    }

    private static boolean matchesAcceptCall(String s, Set<String> words) {
        // Disallow end call / disconnect phrases from matching accept call
        if (s.contains("disconnect") || s.contains("hang up") || s.contains("cut call") ||
                s.contains("डिस्कनेक्ट") || s.contains("വിച്ഛേദ") ||
                words.contains("disconnect") || words.contains("hang")) {
            return false;
        }

        // Disallow generic single words such as "call", "phone" alone
        if (s.equals("call") || s.equals("phone") ||
                s.equals("ಕರೆ") || s.equals("ಕಾಲ್") || s.equals("ಫೋನ್") ||
                s.equals("कॉल") || s.equals("फोन") ||
                s.equals("കോൾ") || s.equals("ഫോൺ")) {
            return false;
        }

        if (containsMatch(s, words,
                "accept call", "receive call", "answer call", "pick up call", "pick up the call",
                "pick up", "pickup", "pickup call", "pickup the call",
                "answer the call", "receive the call", "accept the call",
                "pick up the phone", "pickup the phone", "answer phone", "answer the phone", "receive phone", "receive the phone",
                "attend call", "attend the call", "take call", "take the call",
                "receive incoming call", "accept incoming call", "answer incoming call", "pick up incoming call",
                "receive caregiver call", "accept caregiver call", "answer caregiver call",
                "connect incoming call", "get call", "get the call",
                // Kannada
                "ಕರೆ ಸ್ವೀಕರಿಸಿ", "ಕರೆ ಸ್ವೀಕರಿಸು", "ಕರೆಯನ್ನು ಸ್ವೀಕರಿಸಿ", "ಕರೆಯನ್ನು ಸ್ವೀಕರಿಸು",
                "ಕಾಲ್ ಸ್ವೀಕರಿಸಿ", "ಕಾಲ್ ರಿಸೀವ್ ಮಾಡಿ", "ಕರೆ ರಿಸೀವ್ ಮಾಡಿ", "ಕರೆಯನ್ನು ರಿಸೀವ್ ಮಾಡಿ",
                "ಫೋನ್ ಸ್ವೀಕರಿಸಿ", "ಫೋನ್ ರಿಸೀವ್ ಮಾಡಿ", "ಕರೆ ಎತ್ತಿ", "ಫೋನ್ ಎತ್ತಿ", "ಕಾಲ್ ಎತ್ತಿ", "ಕರೆಯನ್ನು ಎತ್ತಿ",
                "ಸ್ವೀಕರಿಸಿ", "ರಿಸೀವ್ ಮಾಡಿ", "ಕರೆ ತಗೆದುಕೊಳ್ಳಿ", "ಕಾಲ್ ಅಕ್ಸೆಪ್ಟ್ ಮಾಡಿ", "ಕರೆ ಅಕ್ಸೆಪ್ಟ್ ಮಾಡಿ",
                "ಬರುತ್ತಿರುವ ಕರೆ ಸ್ವೀಕರಿಸಿ", "ಆಗಮಿಸುವ ಕರೆ ಸ್ವೀಕರಿಸಿ", "ಕೇರ್ಗಿವರ್ ಕರೆ ಸ್ವೀಕರಿಸಿ", "ಕೇರ್ಗಿವರ್ ಕಾಲ್ ರಿಸೀವ್ ಮಾಡಿ",
                "ಕರೆ ಉತ್ತರ ಕೊಡಿ", "ಫೋನ್ ತಗೊಳ್ಳಿ", "ಕರೆ ತಗೊಳ್ಳಿ", "ಫೋನ್ ತಗೆದುಕೊಳ್ಳಿ", "ಕರೆ ಸ್ವೀಕಾರ",
                // Hindi
                "कॉल उठाओ", "कॉल रिसीव करो", "फोन उठाओ", "कॉल स्वीकार करो", "कॉल स्वीकार करें",
                "कॉल उठाएं", "फोन उठाइए", "कॉल एक्सेप्ट करो", "फोन रिसीव करो", "कॉल का जवाब दो",
                "आने वाली कॉल उठाओ", "केयरगिवर की कॉल उठाओ", "बात करो", "कॉल कनेक्ट करो", "कॉल रिसीव करें", "फोन रिसीव करें",
                // Malayalam
                "കോൾ സ്വീകരിക്കുക", "കോൾ എടുക്കുക", "ഫോൺ എടുക്കുക", "കോൾ റിസീവ് ചെയ്യുക", "കോൾ അറ്റൻഡ് ചെയ്യുക",
                "ഫോൺ സ്വീകരിക്കുക", "കോൾ സ്വീകരിക്കൂ", "ഫോൺ എടുക്കൂ", "കോൾ എടുക്കൂ", "ഇൻകമിംഗ് കോൾ സ്വീകരിക്കുക")) {
            return true;
        }

        boolean hasAcceptAction = words.contains("accept") || words.contains("receive") || words.contains("answer") ||
                words.contains("attend") || words.contains("take");
        boolean hasCallNoun = words.contains("call") || words.contains("phone");
        if (hasAcceptAction && hasCallNoun) {
            return true;
        }

        boolean hasKannadaAcceptAction = s.contains("ಸ್ವೀಕರಿಸಿ") || s.contains("ಸ್ವೀಕರಿಸು") || s.contains("ರಿಸೀವ್") ||
                s.contains("ಎತ್ತಿ") || s.contains("ತಗೆದುಕೊಳ್ಳಿ") || s.contains("ತಗೊಳ್ಳಿ") || s.contains("ಅಕ್ಸೆಪ್ಟ್");
        boolean hasKannadaCallNoun = s.contains("ಕರೆ") || s.contains("ಕಾಲ್") || s.contains("ಫೋನ್");
        if (hasKannadaAcceptAction && hasKannadaCallNoun) {
            return true;
        }

        boolean hasHindiAcceptAction = s.contains("उठाओ") || s.contains("उठाइए") || s.contains("उठाएं") ||
                s.contains("रिसीव") || s.contains("स्वीकार") || s.contains("एक्सेप्ट");
        boolean hasHindiCallNoun = s.contains("कॉल") || s.contains("फोन");
        if (hasHindiAcceptAction && hasHindiCallNoun) {
            return true;
        }

        boolean hasMalayalamAcceptAction = s.contains("സ്വീകരിക്കുക") || s.contains("സ്വീകരിക്കൂ") ||
                s.contains("എടുക്കുക") || s.contains("എടുക്കൂ") || s.contains("റിസീവ്") || s.contains("അറ്റൻഡ്");
        boolean hasMalayalamCallNoun = s.contains("കോൾ") || s.contains("ഫോൺ");
        if (hasMalayalamAcceptAction && hasMalayalamCallNoun) {
            return true;
        }

        return false;
    }

    private static boolean matchesCallCaregiver(String s, Set<String> words) {
        // Disallow generic single words such as "call" alone from triggering caregiver call
        if (s.equals("call") || s.equals("phone") || s.equals("dial") || s.equals("ring") ||
                s.equals("call my") || s.equals("phone my") ||
                s.equals("caregiver") || s.equals("carer") || s.equals("caretaker") ||
                s.equals("care worker") || s.equals("helper") ||
                s.equals("ಕರೆ") || s.equals("ಕಾಲ್") || s.equals("ಫೋನ್") ||
                s.equals("ಕರೆ ಮಾಡಿ") || s.equals("ಕಾಲ್ ಮಾಡಿ") || s.equals("ಫೋನ್ ಮಾಡಿ") ||
                s.equals("ಕರೆ ಮಾಡು") || s.equals("ಕಾಲ್ ಮಾಡು") || s.equals("ಫೋನ್ ಮಾಡು") ||
                s.equals("कॉल") || s.equals("फोन") || s.equals("कॉल करो") || s.equals("फोन करो") ||
                s.equals("कॉल करें") || s.equals("फोन करें") || s.equals("कॉल लगाओ") || s.equals("फोन लगाओ") ||
                s.equals("को कॉल करो") || s.equals("को फोन करो") ||
                s.equals("കോൾ") || s.equals("ഫോൺ") || s.equals("വിളിക്കുക") || s.equals("വിളിക്കൂ") ||
                s.equals("കോൾ ചെയ്യൂ") || s.equals("ഫോൺ ചെയ്യൂ") || s.equals("കോൾ ചെയ്യുക") || s.equals("ഫോൺ ചെയ്യുക")) {
            return false;
        }

        // Exclude message and connection page intents from triggering call
        if (words.contains("message") || words.contains("messages") ||
                s.contains("ಸಂದೇಶ") || s.contains("ಮೆಸೇಜ್") ||
                s.contains("संदेश") || s.contains("मैसेज") ||
                s.contains("സന്ദേശം") || s.contains("മെസേജ്") ||
                s.contains("connection") || s.contains("page") || s.contains("ಸ್ಕ್ರೀನ್") ||
                s.contains("ಪುಟ") || s.contains("ಕನೆಕ್ಷನ್") ||
                s.contains("ಸಂಪರ್ಕ ಪುಟ") || s.equals("ಸಹಾಯಕರ ಸಂಪರ್ಕ") || s.equals("ಆರೈಕೆದಾರರ ಸಂಪರ್ಕ") ||
                s.equals("ಪಾಲಕರ ಸಂಪರ್ಕ") || s.equals("ಕೇರ್ಗಿವರ್ ಸಂಪರ್ಕ") || s.equals("ಸಹಾಯಕ ಸಂಪರ್ಕ")) {
            return false;
        }

        // Direct call variations
        if (containsMatch(s, words,
                // English Task 27 aliases & natural variations
                "call caregiver", "call my caregiver", "call the caregiver",
                "call carer", "call my carer", "call the carer",
                "call caretaker", "call my caretaker", "call the caretaker",
                "call care worker", "call my care worker", "call the care worker",
                "call helper", "call my helper", "call the helper",
                "phone caregiver", "phone my caregiver", "phone the caregiver",
                "phone carer", "phone my carer", "phone the carer",
                "phone caretaker", "phone my caretaker", "phone the caretaker",
                "phone care worker", "phone my care worker", "phone the care worker",
                "phone helper", "phone my helper", "phone the helper",
                "ring caregiver", "ring my caregiver", "ring the caregiver",
                "ring carer", "ring my carer", "ring the carer",
                "ring caretaker", "ring my caretaker", "ring the caretaker",
                "ring care worker", "ring my care worker", "ring the care worker",
                "ring helper", "ring my helper", "ring the helper",
                "dial caregiver", "dial my caregiver", "dial the caregiver",
                "dial carer", "dial my carer", "dial the carer",
                "dial caretaker", "dial my caretaker", "dial the caretaker",
                "dial care worker", "dial my care worker", "dial the care worker",
                "dial helper", "dial my helper", "dial the helper",
                "start a call with caregiver", "start a call with my caregiver",
                "open caregiver call", "make a call to caregiver", "make a call to my caregiver",
                "start voice call", "open voice call", "make voice call",
                "voice call", "phone call", "caregiver call",
                "connect me with my caregiver", "connect me to my caregiver", "connect me with caregiver", "connect caregiver",
                "connect me with my helper", "connect me to my helper",
                "i want to talk to my caregiver", "i want to speak to my caregiver", "i want to talk with my caregiver", "i want to speak with my caregiver",
                "talk to my caregiver", "speak to my caregiver", "talk to caregiver", "speak to caregiver",
                "can i speak to my helper", "can i speak to my caregiver", "can i talk to my helper", "can i talk to my caregiver",
                "can you call my caregiver", "please call my caregiver", "please call my helper", "phone my helper", "call my helper",
                "call my attendant", "call the person taking care of me", "i need to talk to the person taking care of me",
                "please get my helper", "get my helper", "please get my caregiver", "get my caregiver",
                "put me through to my caregiver", "put me through to caregiver", "put me through to my helper", "put me through to helper",
                // Kannada
                "ಧ್ವನಿ ಕರೆ", "ಕೇರ್‌ಗಿವರ್‌ಗೆ ಕರೆ", "ಕೇರ್ಗಿವರ್ಗೆ ಕರೆ", "ಫೋನ್ ಕರೆ", "ಕರೆ ಪ್ರಾರಂಭಿಸಿ",
                "ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ", "ಆರೈಕೆದಾರರಿಗೆ ಕಾಲ್ ಮಾಡಿ", "ಆರೈಕೆದಾರನಿಗೆ ಕರೆ ಮಾಡಿ", "ಆರೈಕೆದಾರನಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ನನ್ನ ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ", "ನನ್ನ ಆರೈಕೆದಾರನಿಗೆ ಕರೆ ಮಾಡಿ", "ನನ್ನ ಆರೈಕೆದಾರರಿಗೆ ಕಾಲ್ ಮಾಡಿ", "ನನ್ನ ಆರೈಕೆದಾರನಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ", "ಸಹಾಯಕನಿಗೆ ಕಾಲ್ ಮಾಡಿ", "ಸಹಾಯಕರಿಗೆ ಕರೆ ಮಾಡಿ", "ಸಹಾಯಕರಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ", "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕಾಲ್ ಮಾಡಿ", "ನನ್ನ ಸಹಾಯಕರಿಗೆ ಕರೆ ಮಾಡಿ", "ನನ್ನ ಸಹಾಯಕರಿಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ಸಹಾಯಕನಿಗೆ ಫೋನ್ ಮಾಡಿ", "ಸಹಾಯಕರಿಗೆ ಫೋನ್ ಮಾಡಿ", "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಫೋನ್ ಮಾಡಿ", "ನನ್ನ ಸಹಾಯಕರಿಗೆ ಫೋನ್ ಮಾಡಿ",
                "ಪಾಲಕರಿಗೆ ಕರೆ ಮಾಡಿ", "ಪಾಲಕರಿಗೆ ಕಾಲ್ ಮಾಡಿ", "ಪಾಲಕರಿಗೆ ಫೋನ್ ಮಾಡಿ",
                "ನನ್ನ ಪಾಲಕರಿಗೆ ಕರೆ ಮಾಡಿ", "ನನ್ನ ಪಾಲಕರಿಗೆ ಕಾಲ್ ಮಾಡಿ", "ನನ್ನ ಪಾಲಕರಿಗೆ ಫೋನ್ ಮಾಡಿ",
                "ಪಾಲಕನಿಗೆ ಕರೆ ಮಾಡಿ", "ಪಾಲಕನಿಗೆ ಕಾಲ್ ಮಾಡಿ", "ಪಾಲಕನಿಗೆ ಫೋನ್ ಮಾಡಿ",
                "ನನ್ನ ಪಾಲಕನಿಗೆ ಕರೆ ಮಾಡಿ", "ನನ್ನ ಪಾಲಕನಿಗೆ ಕಾಲ್ ಮಾಡಿ", "ನನ್ನ ಪಾಲಕನಿಗೆ ಫೋನ್ ಮಾಡಿ",
                "ಸಹಾಯಕನಿಗೆ ಕರೆ", "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕರೆ", "ಸಹಾಯಕರಿಗೆ ಕರೆ", "ನನ್ನ ಸಹಾಯಕರಿಗೆ ಕರೆ",
                "ಪಾಲಕರಿಗೆ ಕರೆ", "ನನ್ನ ಪಾಲಕರಿಗೆ ಕರೆ", "ಪಾಲಕನಿಗೆ ಕರೆ", "ನನ್ನ ಪಾಲಕನಿಗೆ ಕರೆ",
                "ನನ್ನ ಸಹಾಯಕನನ್ನು ಕರೆ ಮಾಡಿ", "ನನ್ನ ಸಹಾಯಕರನ್ನು ಕರೆ ಮಾಡಿ", "ಸಹಾಯಕನನ್ನು ಕರೆ ಮಾಡಿ", "ಸಹಾಯಕರನ್ನು ಕರೆ ಮಾಡಿ",
                "ನನ್ನ ಪಾಲಕರನ್ನು ಕರೆ ಮಾಡಿ", "ನನ್ನ ಪಾಲಕನನ್ನು ಕರೆ ಮಾಡಿ", "ಪಾಲಕರನ್ನು ಕರೆ ಮಾಡಿ", "ಪಾಲಕನನ್ನು ಕರೆ ಮಾಡಿ",
                "ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡು", "ಸಹಾಯಕರಿಗೆ ಕರೆ ಮಾಡು", "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡು", "ನನ್ನ ಸಹಾಯಕರಿಗೆ ಕರೆ ಮಾಡು",
                "ಪಾಲಕರಿಗೆ ಕರೆ ಮಾಡು", "ಪಾಲಕನಿಗೆ ಕರೆ ಮಾಡು", "ನನ್ನ ಪಾಲಕರಿಗೆ ಕರೆ ಮಾಡು", "ನನ್ನ ಪಾಲಕನಿಗೆ ಕರೆ ಮಾಡು",
                "ಸಹಾಯಕನಿಗೆ ಕಾಲ್ ಮಾಡು", "ಪಾಲಕರಿಗೆ ಕಾಲ್ ಮಾಡು", "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕಾಲ್ ಮಾಡು", "ನನ್ನ ಪಾಲಕರಿಗೆ ಕಾಲ್ ಮಾಡು",
                "ಸಹಾಯಕನಿಗೆ ಫೋನ್ ಮಾಡು", "ಪಾಲಕರಿಗೆ ಫೋನ್ ಮಾಡು", "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಫೋನ್ ಮಾಡು", "ನನ್ನ ಪಾಲಕರಿಗೆ ಫೋನ್ ಮಾಡು",
                "ಕೇರ್ಗಿವರ್ಗೆ ಕರೆ ಮಾಡಿ", "ಕೇರ್ಗಿವರ್ಗೆ ಕಾಲ್ ಮಾಡಿ", "ಕೇರ್ಟೇಕರ್ಗೆ ಕರೆ ಮಾಡಿ", "ಕೇರ್ಟೇಕರ್ಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ನನ್ನ ಕೇರ್ಗಿವರ್ಗೆ ಕರೆ ಮಾಡಿ", "ನನ್ನ ಕೇರ್ಗಿವರ್ಗೆ ಕಾಲ್ ಮಾಡಿ", "ನನ್ನ ಕೇರ್‌ಗಿವರ್‌ಗೆ ಕರೆ ಮಾಡಿ", "ನನ್ನ ಕೇರ್‌ಗಿವರ್‌ಗೆ ಕಾಲ್ ಮಾಡಿ",
                "ಆರೈಕೆದಾರರಿಗೆ ಕರೆ", "ಆರೈಕೆದಾರರಿಗೆ ಕಾಲ್", "ಸಹಾಯಕನಿಗೆ ಕರೆ", "ಸಹಾಯಕನಿಗೆ ಕಾಲ್", "ಸಹಾಯಕರಿಗೆ ಕರೆ", "ಸಹಾಯಕರಿಗೆ ಕಾಲ್",
                "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕರೆ", "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಕಾಲ್", "ನನ್ನ ಸಹಾಯಕರಿಗೆ ಕರೆ", "ನನ್ನ ಸಹಾಯಕರಿಗೆ ಕಾಲ್",
                "ಕೇರ್ಗಿವರ್ಗೆ ಕರೆ", "ಕೇರ್ಟೇಕರ್ಗೆ ಕರೆ", "ಆರೈಕೆದಾರರ ಕರೆ", "ಸಹಾಯಕನ ಕರೆ", "ಸಹಾಯಕರ ಕರೆ", "ಪಾಲಕರ ಕರೆ", "ಪಾಲಕನ ಕರೆ",
                "ಕೇರ್ಗಿವರ್ ಜೊತೆ ಮಾತನಾಡಬೇಕು", "ಸಹಾಯಕನ ಜೊತೆ ಮಾತನಾಡಬೇಕು", "ದಯವಿಟ್ಟು ಕೇರ್ಗಿವರ್ಗೆ ಕಾಲ್ ಮಾಡಿ", "ದಯವಿಟ್ಟು ಕೇರ್ಗಿವರ್ಗೆ ಕರೆ ಮಾಡಿ",
                "ನನ್ನ ಸಹಾಯಕನ ಜೊತೆ ಮಾತನಾಡಬೇಕು", "ನನ್ನ ಆರೈಕೆದಾರರ ಜೊತೆ ಮಾತನಾಡಬೇಕು", "ನನ್ನ ಕೇರ್ಗಿವರ್ ಜೊತೆ ಸಂಪರ್ಕಿಸಿ",
                "ಕೇರ್ಗಿವರ್ ಜೊತೆ ಸಂಪರ್ಕಿಸಿ", "ಸಹಾಯಕನ ಜೊತೆ ಸಂಪರ್ಕಿಸಿ", "ಆರೈಕೆದಾರರ ಜೊತೆ ಸಂಪರ್ಕಿಸಿ",
                // Hindi
                "वॉइस कॉल", "कॉल करो", "केयरगिवर को कॉल", "फोन करो", "कॉल शुरू करो",
                "देखभाल करने वाले को कॉल करो", "मेरे देखभाल करने वाले को कॉल करो",
                "देखभाल करने वाले को फोन करो", "मेरे देखभाल करने वाले को फोन करो",
                "सहायक को कॉल करो", "मेरे सहायक को कॉल करो",
                "सहायक को फोन करो", "मेरे सहायक को फोन करो",
                "केयरगिवर को कॉल करो", "मेरे केयरगिवर को कॉल करो",
                "केयरगिवर को फोन करो", "मेरे केयरगिवर को फोन करो",
                "केयरटेकर को कॉल करो", "मेरे केयरटेकर को कॉल करो",
                "केयरटेकर को फोन करो", "मेरे केयरटेकर को फोन करो",
                "देखभाल करने वाले को कॉल करें", "मेरे देखभाल करने वाले को कॉल करें",
                "देखभाल करने वाले को फोन करें", "मेरे देखभाल करने वाले को फोन करें",
                "सहायक को कॉल करें", "मेरे सहायक को कॉल करें",
                "सहायक को फोन करें", "मेरे सहायक को फोन करें",
                "केयरगिवर को कॉल करें", "केयरटेकर को कॉल करें",
                "देखभाल करने वाले को कॉल लगाओ", "मेरे देखभाल करने वाले को कॉल लगाओ",
                "देखभाल करने वाले को फोन लगाओ", "मेरे देखभाल करने वाले को फोन लगाओ",
                "सहायक को कॉल लगाओ", "मेरे सहायक को कॉल लगाओ",
                "सहायक को फोन लगाओ", "मेरे सहायक को फोन लगाओ",
                "केयरगिवर को कॉल लगाओ", "केयरटेकर को कॉल लगाओ",
                "केयरगिवर को फोन लगाओ", "केयरटेकर को फोन लगाओ",
                "कृपया केयरगिवर को कॉल करें", "मुझे केयरगिवर से बात करनी है", "केयरगिवर को फोन मिलाओ", "मेरे सहायक से बात कराओ",
                "मुझे अपने केयरगिवर से बात करनी है", "मुझे अपने सहायक से बात करवाओ", "मेरे केयरगिवर से जोड़ो", "मुझे अपने केयरगिवर से जोड़ो",
                // Malayalam
                "വോയ്സ് കോൾ", "കോൾ ചെയ്യുക", "കെയർഗിവറെ വിളിക്കുക", "ഫോൺ ചെയ്യുക",
                "പരിചരിക്കുന്നയാളെ വിളിക്കൂ", "എന്റെ പരിചരിക്കുന്നയാളെ വിളിക്കൂ", "പരിചരിക്കുന്ന ആളെ വിളിക്കൂ",
                "പരിചാരകനെ വിളിക്കൂ", "എന്റെ പരിചാരകനെ വിളിക്കൂ",
                "സഹായിയെ വിളിക്കൂ", "എന്റെ സഹായിയെ വിളിക്കൂ",
                "കെയർഗിവറെ വിളിക്കൂ", "എന്റെ കെയർഗിവറെ വിളിക്കൂ", "കെയർ ഗിവറെ വിളിക്കൂ",
                "കെയർടേക്കറെ വിളിക്കൂ", "എന്റെ കെയർടേക്കറെ വിളിക്കൂ", "കെയർ ടേക്കറെ വിളിക്കൂ",
                "പരിചരിക്കുന്നയാളെ ഫോൺ ചെയ്യൂ", "പരിചാരകനെ ഫോൺ ചെയ്യൂ", "സഹായിയെ ഫോൺ ചെയ്യൂ",
                "കെയർഗിവറെ ഫോൺ ചെയ്യൂ", "കെയർടേക്കറെ ഫോൺ ചെയ്യൂ",
                "പരിചരിക്കുന്നയാളെ കോൾ ചെയ്യൂ", "പരിചാരകനെ കോൾ ചെയ്യൂ", "സഹായിയെ കോൾ ചെയ്യൂ",
                "കെയർഗിവറെ കോൾ ചെയ്യൂ", "കെയർടേക്കറെ കോൾ ചെയ്യൂ",
                "പരിചരിക്കുന്നയാളെ വിളിക്കുക", "പരിചാരകനെ വിളിക്കുക", "സഹായിയെ വിളിക്കുക",
                "കെയർഗിവറെ വിളിക്കുക", "കെയർടേക്കറെ വിളിക്കുക",
                "ദയവായി കെയർഗിവറെ ഫോൺ ചെയ്യൂ", "എനിക്ക് കെയർഗിവറോട് സംസാരിക്കണം", "കെയർഗിവർക്ക് കോൾ ചെയ്യൂ",
                "എനിക്ക് എന്റെ കെയർഗിവറോട് സംസാരിക്കണം", "എന്റെ സഹായിയുമായി സംസാരിക്കണം", "എന്റെ പരിചാരകനുമായി ബന്ധിപ്പിക്കൂ",
                "കെയർഗിവറുമായി ബന്ധിപ്പിക്കൂ", "സഹായിയുമായി ബന്ധിപ്പിക്കൂ")) {
            return true;
        }

        // Kannada alias + call action token combinations
        boolean hasKannadaCaregiver = containsKannadaCaregiverAny(s);
        boolean hasKannadaCallAction = s.contains("ಕರೆ") || s.contains("ಕಾಲ್") || s.contains("ಫೋನ್") ||
                s.contains("ಮಾತನಾಡು") || s.contains("ಮಾತನಾಡಬೇಕು") || s.contains("ಜೊತೆ ಸಂಪರ್ಕಿಸಿ") || s.contains("ಸಂಪರ್ಕಿಸಿ");
        if (hasKannadaCaregiver && hasKannadaCallAction && !s.contains("ಪುಟ") && !s.contains("ಸ್ಕ್ರೀನ್") && !s.contains("ಸಂಪರ್ಕ ಪುಟ") && !s.equals("ಸಹಾಯಕರ ಸಂಪರ್ಕ")) {
            return true;
        }

        // Hindi alias + call action token combinations
        boolean hasHindiCaregiver = containsHindiCaregiverAny(s);
        boolean hasHindiCallAction = s.contains("कॉल") || s.contains("फोन") ||
                s.contains("बात करनी") || s.contains("बात कराओ") || s.contains("बात करवाओ") ||
                s.contains("मिलाओ") || s.contains("लगाओ") || s.contains("जोड़ो");
        if (hasHindiCaregiver && hasHindiCallAction) {
            return true;
        }

        // Malayalam alias + call action token combinations
        boolean hasMalayalamCaregiver = containsMalayalamCaregiverAny(s);
        boolean hasMalayalamCallAction = s.contains("വിളിക്കൂ") || s.contains("വിളിക്കുക") ||
                s.contains("ഫോൺ ചെയ്യൂ") || s.contains("ഫോൺ ചെയ്യുക") ||
                s.contains("കോൾ ചെയ്യൂ") || s.contains("കോൾ ചെയ്യുക") ||
                s.contains("വിളി") || s.contains("സംസാരിക്കണം") || s.contains("സംസാരിക്കുക") ||
                s.contains("ബന്ധിപ്പിക്കൂ") || s.contains("ബന്ധിപ്പിക്കുക");
        if (hasMalayalamCaregiver && hasMalayalamCallAction) {
            return true;
        }

        // Token combinations: action (call/phone/dial/connect/talk/speak) + target (caregiver/carer/caretaker/care worker/helper/attendant)
        boolean hasCallAction = words.contains("call") || words.contains("phone") || words.contains("dial") || words.contains("ring") ||
                words.contains("connect") || (words.contains("talk") && (s.contains("talk to") || s.contains("talk with") || s.contains("speak to") || s.contains("speak with"))) ||
                (words.contains("speak") && (s.contains("speak to") || s.contains("speak with") || s.contains("speak to my") || s.contains("speak with my"))) ||
                s.contains("put me through") || s.contains("get my helper") || s.contains("get my caregiver");
        boolean hasCaregiver = containsEnglishCaregiverAny(s, words) || words.contains("guardian");
        return hasCallAction && hasCaregiver;
    }

    private static boolean matchesRecordVoiceMessage(String s, Set<String> words) {
        // Exclude generic single words or open recorder commands
        if (s.equals("voice") || s.equals("message") || s.equals("audio") || s.equals("record") ||
                words.contains("open") || s.equals("record a voice message") || s.equals("record voice message") ||
                s.equals("record audio message") || s.equals("record audio") || s.equals("start audio recorder") ||
                s.equals("audio recorder") || s.equals("voice recorder")) {
            return false;
        }

        // Direct phrases
        if (containsMatch(s, words,
                // English Task phrases
                "i want to send a voice message", "i want to send a voice message to my caregiver",
                "send a voice message to my caregiver", "send voice message to my caregiver",
                "send a voice message to caregiver", "send voice message to caregiver",
                "send a voice message to my helper", "send voice message to my helper",
                "send a voice message to my carer", "send voice message to my carer",
                "send a voice message to my caretaker", "send voice message to my caretaker",
                "send a voice message to my care worker", "send voice message to my care worker",
                "send a voice message to my attendant", "send voice message to my attendant",
                "send a voice message to my assistant", "send voice message to my assistant",
                "send a voice message to the person taking care of me", "send voice message to the person taking care of me",
                "i want to send a voice message to the person taking care of me",
                "i want to record a message", "record a message for my caregiver", "record a message for caregiver",
                "record a message for my helper", "i want to send my voice",
                "send a voice message", "send voice message", "send my voice",
                "send audio message to assistant", "send voice note to helper", "send voice note",
                "record a voice message for attendant", "send audio recording to caregiver",
                // Kannada Task phrases
                "ನನ್ನ ಆರೈಕೆದಾರರಿಗೆ ಧ್ವನಿ ಸಂದೇಶ ಕಳುಹಿಸಬೇಕು", "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಧ್ವನಿ ಸಂದೇಶ ಕಳುಹಿಸಬೇಕು",
                "ಧ್ವನಿ ಸಂದೇಶ ಕಳುಹಿಸಬೇಕು", "ಧ್ವನಿ ಸಂದೇಶ ರೆಕಾರ್ಡ್ ಮಾಡಬೇಕು", "ವಾಯ್ಸ್ ಮೆಸೇಜ್ ಕಳುಹಿಸಬೇಕು",
                "ನನ್ನ ಆರೈಕೆ ಮಾಡುವವರಿಗೆ ಕಳುಹಿಸಬೇಕು", "ಆರೈಕೆದಾರರಿಗೆ ಧ್ವನಿ ಸಂದೇಶ ಕಳುಹಿಸಬೇಕು",
                "ಸಹಾಯಕನಿಗೆ ಧ್ವನಿ ಸಂದೇಶ ಕಳುಹಿಸಬೇಕು", "ಧ್ವನಿ ಸಂದೇಶ ಕಳುಹಿಸು", "ಧ್ವನಿ ಸಂದೇಶ ಕಳುಹಿಸಿ", "ಧ್ವನಿ ಸಂದೇಶ ಕಳುಹಿಸಿದ್ದೇನೆ",
                "ಧ್ವನಿ ಸಂದೇಶ ಕಳಿಸಿ", "ಧ್ವನಿ ಸಂದೇಶ ಕಳಿಸು", "ವಾಯ್ಸ್ ಮೆಸೇಜ್ ಕಳಿಸಿ", "ವಾಯ್ಸ್ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ನನ್ನ ಕೇರ್‌ಗಿವರ್‌ಗೆ ವಾಯ್ಸ್ ಮೆಸೇಜ್ ಕಳುಹಿಸಬೇಕು", "ಕೇರ್‌ಗಿವರ್‌ಗೆ ಆಡಿಯೋ ಸಂದೇಶ ಕಳುಹಿಸು",
                "ಆಡಿಯೋ ಮೆಸೇಜ್ ಕಳುಹಿಸು", "ಸಹಾಯಕರಿಗೆ ವಾಯ್ಸ್ ಮೆಸೇಜ್ ಕಳುಹಿಸು",
                // Hindi Task phrases
                "मैं अपने देखभालकर्ता को वॉइस मैसेज भेजना चाहता हूँ", "मैं अपने देखभालकर्ता को वॉइस मैसेज भेजना चाहता हूं",
                "मेरे सहायक को वॉइस मैसेज भेजना है", "सहायक को वॉइस मैसेज भेजना है",
                "वॉइस मैसेज भेजना है", "वॉइस मैसेज रिकॉर्ड करना है", "वॉइस मैसेज भेजना चाहता हूँ",
                "वॉइस मैसेज भेजना चाहता हूं", "केयरगिवर को वॉइस मैसेज भेजना है", "केयरगिवर को वॉइस मैसेज भेजो",
                "मेरे केयरगिवर को वॉयस मैसेज भेजो", "केयरगिवर को ऑडियो संदेश भेजो",
                "वॉयस मैसेज भेजो", "ऑडियो संदेश भेजो", "सहायक को वॉइस संदेश भेजो",
                "वॉयस मैसेज भेजना चाहता हूँ", "वॉयस मैसेज भेजना चाहता हूं",
                // Malayalam Task phrases
                "എന്റെ പരിചാരകന് വോയ്സ് മെസേജ് അയക്കണം", "എന്റെ സഹായകനു വോയ്സ് മെസേജ് അയക്കണം",
                "സഹായകനു വോയ്സ് മെസേജ് അയക്കണം", "പരിചാരകന് വോയ്സ് മെസേജ് അയക്കണം",
                "വോയ്സ് മെസേജ് അയക്കണം", "വോയ്സ് മെസേജ് റെക്കോർഡ് ചെയ്യണം",
                "കെയർഗിവർക്ക് വോയ്സ് മെസേജ് അയക്കണം", "കെയർഗിവർക്ക് ശബ്ദ സന്ദേശം അയക്കണം",
                "എന്റെ കെയർഗിവർക്ക് ഒരു വോയ്‌സ് മെസ്സേജ് അയക്കണം", "കെയർഗിവർക്ക് ഓഡിയോ മെസ്സേജ് അയക്കൂ",
                "വോയ്‌സ് മെസ്സേജ് അയക്കൂ", "വോയ്‌സ് സന്ദേശം അയക്കണം", "ഓഡിയോ മെസ്സേജ് അയക്കൂ")) {
            return true;
        }

        // Combinations:
        // English: (send / want / record) + (voice message / audio message / voice note / audio recording / my voice)
        boolean hasEnglishVoiceMsg = ((words.contains("voice") || words.contains("audio")) &&
                (words.contains("message") || words.contains("note") || words.contains("recording"))) || s.contains("my voice");
        boolean hasSendAction = words.contains("send") || words.contains("record") || s.startsWith("i want to") || s.startsWith("i would like to");
        if (hasEnglishVoiceMsg && hasSendAction && !words.contains("open")) {
            return true;
        }

        // Kannada: (ಧ್ವನಿ ಸಂದೇಶ / ವಾಯ್ಸ್ ಮೆಸೇಜ್ / ಆಡಿಯೋ ಸಂದೇಶ / ಆಡಿಯೋ ಮೆಸೇಜ್) + (ಕಳುಹಿಸ / ಕಳಿಸ / ರೆಕಾರ್ಡ್ / ಬೇಕು)
        if ((s.contains("ಧ್ವನಿ ಸಂದೇಶ") || s.contains("ವಾಯ್ಸ್ ಮೆಸೇಜ್") || s.contains("ಆಡಿಯೋ ಸಂದೇಶ") || s.contains("ಆಡಿಯೋ ಮೆಸೇಜ್")) &&
                (s.contains("ಕಳುಹಿಸ") || s.contains("ಕಳಿಸ") || s.contains("ರೆಕಾರ್ಡ್") || s.contains("ಬೇಕು") || s.contains("ಮಾಡು"))) {
            return true;
        }

        // Hindi: (वॉइस मैसेज / वॉयस मैसेज / ध्वनि संदेश / ऑडियो संदेश / ऑडियो मैसेज / वॉइस संदेश / वॉयस संदेश) + (भेजना / भेजो / रिकॉर्ड / चाहता / है)
        if ((s.contains("वॉइस") || s.contains("वॉयस") || s.contains("ध्वनि") || s.contains("ऑडियो")) &&
                (s.contains("मैसेज") || s.contains("संदेश")) &&
                (s.contains("भेज") || s.contains("रिकॉर्ड") || s.contains("चाहता") || s.contains("चाहती") || s.contains("है") || containsHindiCaregiverAny(s))) {
            return true;
        }

        // Malayalam: (വോയ്സ് മെസേജ് / വോയ്സ് മെസ്സേജ് / വോയ്‌സ് മെസ്സേജ് / ഓഡിയോ മെസ്സേജ് / ഓഡിയോ മെസേജ് / ശബ്ദ സന്ദേശം / വോയ്‌സ് സന്ദേശം)
        if ((s.contains("വോയ്സ്") || s.contains("വോയ്‌സ്") || s.contains("ഓഡിയോ") || s.contains("ശബ്ദ")) &&
                (s.contains("മെസേജ്") || s.contains("മെസ്സേജ്") || s.contains("സന്ദേശ")) &&
                (s.contains("അയക്ക") || s.contains("അയക്കൂ") || s.contains("റെക്കോർഡ്") || s.contains("വേണം") || s.contains("ചെയ്യണം") || s.contains("ആണ്") || containsMalayalamCaregiverAny(s))) {
            return true;
        }

        return false;
    }

    private static boolean matchesVoiceRecorderOrMessage(String s, Set<String> words) {
        if (containsMatch(s, words,
                "voice recorder", "open voice recorder", "start voice recorder", "open recorder",
                "record a voice message", "record voice message", "record a message",
                "voice message", "voice messages", "open voice message", "open voice messages",
                "record audio", "send voice message", "audio recorder", "voice note", "recorder",
                "voice recording section", "voice recorder card",
                // Kannada
                "ಧ್ವನಿ ರೆಕಾರ್ಡರ್", "ಧ್ವನಿ ಸಂದೇಶ", "ಧ್ವನಿ ಸಂದೇಶಗಳು", "ರೆಕಾರ್ಡರ್", "ರೆಕಾರ್ಡ್", "ಆಡಿಯೋ ರೆಕಾರ್ಡ್", "ರೆಕಾರ್ಡ್ ಮಾಡಿ", "ಆಡಿಯೋ",
                "ಆಡಿಯೋ ರೆಕಾರ್ಡರ್ ತೋರಿಸಿ", "ರೆಕಾರ್ಡಿಂಗ್ ಕಾರ್ಡ್ ತೆರೆಯಿರಿ", "ಧ್ವನಿ ಸಂದೇಶ ವಿಭಾಗಕ್ಕೆ ಹೋಗಿ",
                // Hindi
                "ध्वनि रिकॉर्डर", "वॉइस संदेश", "ऑडियो रिकॉर्ड", "रिकॉर्डर", "रिकॉर्ड", "आवाज रिकॉर्ड", "ऑडियो",
                "वॉइस रिकॉर्डर खोलो", "वॉइस मैसेज खोलो", "ऑडियो रिकॉर्डर दिखाओ", "वॉइस रिकॉर्डर दिखाओ", "रिकॉर्डिंग सेक्शन खोलो",
                // Malayalam
                "ശബ്ദ റെക്കോർഡർ", "ശബ്ദ സന്ദേശം", "ശബ്ദ സന്ദേശങ്ങൾ", "വോയ്സ് മെസ്സേജ്", "ഓഡിയോ റെക്കോർഡ്", "റെക്കോർഡർ", "റെക്കോർഡ്", "ഓഡിയോ",
                "വോയ്സ് റെക്കോർഡർ തുറക്കൂ", "ഓഡിയോ റെക്കോർഡർ കാണിക്കൂ", "വോയ്സ് റെക്കോർഡർ കാണിക്കുക")) {
            return true;
        }

        // Token combinations: (open/start/record) + (recorder/audio/note)
        boolean hasRecordAction = words.contains("record") || words.contains("start") || words.contains("open");
        boolean hasRecorderNoun = words.contains("recorder") || (words.contains("voice") && words.contains("message"));
        return hasRecordAction && hasRecorderNoun;
    }

    private static boolean matchesSendMessage(String s, Set<String> words) {
        // Disallow generic single words or non-command phrases alone from triggering SEND_MESSAGE
        if (s.equals("send") || s.equals("message") || s.equals("messages") ||
                s.equals("caregiver") || s.equals("carer") || s.equals("caretaker") ||
                s.equals("care worker") || s.equals("helper") || s.equals("send it") ||
                s.equals("ಕಳುಹಿಸಿ") || s.equals("ಕಳಿಸಿ") || s.equals("ಸಂದೇಶ") ||
                s.equals("ಕೇರ್ಗಿವರ್") || s.equals("ಕೇರ್‌ಗಿವರ್") || s.equals("ಆರೈಕೆದಾರ") || s.equals("ಸಹಾಯಕ") || s.equals("ಕೇರ್ಟೇಕರ್") ||
                s.equals("ಪಾಲಕ") || s.equals("ಪಾಲಕರು") || s.equals("ಸಹಾಯಕರು") ||
                s.equals("ಮೆಸೇಜ್") ||
                s.equals("भेजो") || s.equals("संदेश") || s.equals("मैसेज") || s.equals("केयरगिवर") ||
                s.equals("देखभाल करने वाला") || s.equals("देखभाल करने वाले") || s.equals("केयरटेकर") ||
                s.equals("अयയ്ക്കൂ") || s.equals("അയക്കുക") || s.equals("സന്ദേശം") || s.equals("മെസേജ്") ||
                s.equals("മെസ്സേജ്") || s.equals("കെയർഗിവർ") || s.equals("കെയർടേക്കർ") ||
                s.equals("സഹായി") || s.equals("പരിചാരകൻ") || s.equals("പരിചരിക്കുന്നയാൾ")) {
            return false;
        }

        // Exclude audio/voice recording and reading/counting/repeating intents
        if (words.contains("voice") || words.contains("audio") || words.contains("recorder") || words.contains("record") ||
                words.contains("read") || words.contains("check") || words.contains("count") || words.contains("how") ||
                words.contains("repeat") || words.contains("again") ||
                s.contains("ಓದಿ") || s.contains("ಓದು") || s.contains("ಹೇಳಿ") || s.contains("ಹೇಳು") ||
                s.contains("ಎಷ್ಟು") || s.contains("ಎಣಿಸಿ") || s.contains("ಮತ್ತೆ") || s.contains("ಮತ್ತೊಮ್ಮೆ") ||
                s.contains("पढ़ो") || s.contains("गिनो") || s.contains("कितने") || s.contains("सुनाओ") || s.contains("दोबारा") ||
                s.contains("വായിക്കൂ") || s.contains("കേൾപ്പിക്കൂ") || s.contains("എത്ര") || s.contains("എണ്ണൂ") || s.contains("വീണ്ടും")) {
            return false;
        }

        if (containsMatch(s, words,
                // English Task 27 aliases & natural variations
                "send message", "send a message", "message caregiver", "send message to caregiver",
                "send a message to caregiver", "message to caregiver", "send caregiver a message",
                "send a message to my caregiver", "send my caregiver a message", "message my caregiver",
                "send a message to my carer", "send my carer a message", "message my carer", "message carer", "send message to carer",
                "send a message to my caretaker", "send my caretaker a message", "message my caretaker", "message caretaker", "send message to caretaker",
                "send a message to my care worker", "send my care worker a message", "message my care worker", "message care worker", "send message to care worker",
                "send a message to my helper", "send my helper a message", "message my helper", "message helper", "send message to helper",
                "send a message to the caregiver", "send a message to the carer", "send a message to the caretaker",
                "send a message to the care worker", "send a message to the helper",
                "send a text to caregiver", "write a message to caregiver", "send this to my caregiver", "tell my caregiver something",
                // Kannada
                "ಸಂದೇಶ ಕಳುಹಿಸಿ", "ಸಂದೇಶವನ್ನು ಕಳುಹಿಸಿ", "ಒಂದು ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಸಂದೇಶ ಕಳುಹಿಸು", "ಸಂದೇಶವನ್ನು ಕಳುಹಿಸು", "ಸಂದೇಶ ಕಳುಹಿಸಬೇಕು", "ಸಂದೇಶ ಕಳುಹಿಸೋಣ",
                "ಸಂದೇಶ ಕಳಿಸಿ", "ಸಂದೇಶವನ್ನು ಕಳಿಸಿ", "ಸಂದೇಶ ಕಳಿಸು", "ಸಂದೇಶ ಕಳಿಸಬೇಕು", "ಸಂದೇಶ ಕಳಿಸೋಣ",
                "ಮೆಸೇಜ್ ಕಳುಹಿಸಿ", "ಮೆಸೇಜ್ ಕಳಿಸಿ", "ಮೆಸೇಜ್ ಕಳುಹಿಸು", "ಮೆಸೇಜ್ ಕಳಿಸು", "ಮೆಸೇಜ್ ಕಳುಹಿಸಬೇಕು",
                "ಕೇರ್ಗಿವರ್ಗೆ ಒಂದು ಸಂದೇಶ ಕಳುಹಿಸಿ", "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಒಂದು ಮೆಸೇಜ್ ಕಳಿಸಿ",
                // Caregiver specific
                "ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ", "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಸಹಾಯಕರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ", "ನನ್ನ ಸಹಾಯಕರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ", "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ",
                "ಸಹಾಯಕರಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ", "ನನ್ನ ಸಹಾಯಕರಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ",
                "ಸಹಾಯಕನಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ", "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ಸಹಾಯಕರಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ", "ನನ್ನ ಸಹಾಯಕರಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ಸಹಾಯಕನಿಗೆ ಮೆಸೇಜ್ ಕಳಿಸಿ", "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಮೆಸೇಜ್ ಕಳಿಸಿ",
                "ಸಹಾಯಕರಿಗೆ ಮೆಸೇಜ್ ಕಳಿಸಿ", "ನನ್ನ ಸಹಾಯಕರಿಗೆ ಮೆಸೇಜ್ ಕಳಿಸಿ",
                "ಪಾಲಕರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ", "ನನ್ನ ಪಾಲಕರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಪಾಲಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ", "ನನ್ನ ಪಾಲಕನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಪಾಲಕರಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ", "ನನ್ನ ಪಾಲಕರಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ",
                "ಪಾಲಕನಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ", "ನನ್ನ ಪಾಲಕನಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ",
                "ಪಾಲಕರಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ", "ನನ್ನ ಪಾಲಕರಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ಪಾಲಕನಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ", "ನನ್ನ ಪಾಲಕನಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ಪಾಲಕರಿಗೆ ಮೆಸೇಜ್ ಕಳಿಸಿ", "ನನ್ನ ಪಾಲಕರಿಗೆ ಮೆಸೇಜ್ ಕಳಿಸಿ",
                "ಪಾಲಕನಿಗೆ ಮೆಸೇಜ್ ಕಳಿಸಿ", "ನನ್ನ ಪಾಲಕನಿಗೆ ಮೆಸೇಜ್ ಕಳಿಸಿ",
                "ಆರೈಕೆದಾರರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ", "ಆರೈಕೆದಾರನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ನನ್ನ ಆರೈಕೆದಾರರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ", "ನನ್ನ ಆರೈಕೆದಾರನಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಆರೈಕೆದಾರರಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ", "ಆರೈಕೆದಾರನಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ಆರೈಕೆದಾರರಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ", "ಆರೈಕೆದಾರನಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ",
                "ಕೇರ್ಗಿವರ್ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ", "ಕೇರ್‌ಗಿವರ್‌ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ನನ್ನ ಕೇರ್ಗಿವರ್ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ", "ನನ್ನ ಕೇರ್‌ಗಿವರ್‌ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಕೇರ್ಗಿವರ್ಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ", "ಕೇರ್‌ಗಿವರ್‌ಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ",
                "ಕೇರ್ಗಿವರ್ಗೆ ಸಂದೇಶ ಕಳಿಸಿ", "ಕೇರ್ಟೇಕರ್ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ",
                "ಕೇರ್ಟೇಕರ್ಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ", "ಕೇರ್ಟೇಕರ್ಗೆ ಸಂದೇಶ ಕಳಿಸಿ",
                // Hindi
                "संदेश भेजो", "एक संदेश भेजो", "केयरगिवर को संदेश भेजो", "केयरटेकर को संदेश भेजो", "मैसेज भेजो",
                "केयरगिवर को मैसेज भेजो", "केयरटेकर को मैसेज भेजो", "संदेश भेजें", "मैसेज भेजें", "एक मैसेज भेजो",
                "देखभाल करने वाले को संदेश भेजो", "मेरे देखभाल करने वाले को संदेश भेजो",
                "देखभाल करने वाले को मैसेज भेजो", "मेरे देखभाल करने वाले को मैसेज भेजो",
                "देखभाल करने वाले को संदेश भेजें", "मेरे देखभाल करने वाले को संदेश भेजें",
                "देखभाल करने वाले को मैसेज भेजें", "मेरे देखभाल करने वाले को मैसेज भेजें",
                "सहायक को संदेश भेजो", "मेरे सहायक को संदेश भेजो",
                "सहायक को मैसेज भेजो", "मेरे सहायक को मैसेज भेजो",
                "सहायक को संदेश भेजें", "मेरे सहायक को संदेश भेजें",
                "सहायक को मैसेज भेजें", "मेरे सहायक को मैसेज भेजें",
                "केयरगिवर को संदेश भेजो", "मेरे केयरगिवर को संदेश भेजो",
                "केयरगिवर को मैसेज भेजो", "मेरे केयरगिवर को मैसेज भेजो",
                "केयरटेकर को संदेश भेजो", "मेरे केयरटेकर को संदेश भेजो",
                "केयरटेकर को मैसेज भेजो", "मेरे केयरटेकर को मैसेज भेजो",
                "केयरगिवर को संदेश भेजें", "केयरटेकर को संदेश भेजें",
                "केयरगिवर को मैसेज भेजें", "केयरटेकर को मैसेज भेजें",
                "एक संदेश भेजना है", "कृपया केयरगिवर को मैसेज भेजें", "केयरगिवर को कुछ बताना है", "देखभाल करने वाले को मैसेज करो",
                // Malayalam
                "സന്ദേശം അയയ്ക്കൂ", "ഒരു സന്ദേശം അയയ്ക്കൂ", "കെയർഗിവറിന് സന്ദേശം അയയ്ക്കൂ", "കെയർടേക്കറിന് സന്ദേശം അയയ്ക്കൂ",
                "മെസേജ് അയയ്ക്കൂ", "സന്ദേശം അയക്കുക", "ഒരു സന്ദേശം അയക്കുക", "കെയർഗിവർക്ക് സന്ദേശം അയക്കുക",
                "കെയർഗിവറിന് മെസേജ് അയയ്ക്കൂ", "മെസ്സേജ് അയക്കുക", "മെസേജ് അയക്കുക",
                "കെയർഗിവറോട് ഒരു കാര്യം പറയണം", "ഒരു സന്ദേശം അയക്കണം", "ദയവായി കെയർഗിവർക്ക് സന്ദേശം അയക്കൂ", "സന്ദേശം അയക്കൂ",
                "പരിചരിക്കുന്നയാൾക്ക് സന്ദേശം അയയ്ക്കൂ", "എന്റെ പരിചരിക്കുന്നയാൾക്ക് സന്ദേശം അയയ്ക്കൂ",
                "പരിചാരകന് സന്ദേശം അയയ്ക്കൂ", "എന്റെ പരിചാരകന് സന്ദേശം അയയ്ക്കൂ",
                "സഹായിക്ക് സന്ദേശം അയയ്ക്കൂ", "എന്റെ സഹായിക്ക് സന്ദേശം അയയ്ക്കൂ",
                "കെയർഗിവറിന് സന്ദേശം അയയ്ക്കൂ", "എന്റെ കെയർഗിവറിന് സന്ദേശം അയയ്ക്കൂ",
                "കെയർടേക്കറിന് സന്ദേശം അയയ്ക്കൂ", "എന്റെ കെയർടേക്കറിന് സന്ദേശം അയയ്ക്കൂ",
                "പരിചരിക്കുന്നയാൾക്ക് മെസേജ് അയയ്ക്കൂ", "പരിചാരകന് മെസേജ് അയയ്ക്കൂ",
                "സഹായിക്ക് മെസേജ് അയയ്ക്കൂ", "കെയർടേക്കറിന് മെസേജ് അയയ്ക്കൂ",
                "പരിചരിക്കുന്നയാൾക്ക് സന്ദേശം അയക്കൂ", "പരിചാരകന് സന്ദേശം അയക്കൂ",
                "സഹായിക്ക് സന്ദേശം അയക്കൂ", "കെയർഗിവറിന് സന്ദേശം അയക്കൂ", "കെയർടേക്കറിന് സന്ദേശം അയക്കൂ",
                "പരിചരിക്കുന്നയാൾക്ക് മെസേജ് അയക്കൂ", "പരിചാരകന് മെസേജ് അയക്കൂ",
                "സഹായിക്ക് മെസേജ് അയക്കൂ", "കെയർടേക്കറിന് മെസേജ് അയക്കൂ",
                "പരിചരിക്കുന്നയാൾക്ക് സന്ദേശം അയക്കുക", "പരിചാരകന് സന്ദേശം അയക്കുക",
                "സഹായിക്ക് സന്ദേശം അയക്കുക", "കെയർടേക്കറിന് സന്ദേശം അയക്കുക")) {
            return true;
        }

        boolean hasKannadaSend = s.contains("ಕಳುಹಿಸಿ") || s.contains("ಕಳುಹಿಸು") || s.contains("ಕಳುಹಿಸಬೇಕು") ||
                s.contains("ಕಳುಹಿಸೋಣ") || s.contains("ಕಳಿಸಿ") || s.contains("ಕಳಿಸು") || s.contains("ಕಳಿಸಬೇಕು") || s.contains("ಕಳಿಸೋಣ");
        boolean hasKannadaMsg = s.contains("ಸಂದೇಶ") || s.contains("ಮೆಸೇಜ್");
        boolean hasKannadaCaregiver = containsKannadaCaregiverAny(s);
        if (hasKannadaSend && (hasKannadaMsg || hasKannadaCaregiver)) {
            return true;
        }
        if (hasKannadaMsg && containsKannadaCaregiverDative(s)) {
            return true;
        }

        boolean hasHindiSend = s.contains("भेजो") || s.contains("भेजें") || s.contains("भेजिए") || s.contains("भेजना");
        boolean hasHindiMsg = s.contains("संदेश") || s.contains("मैसेज");
        boolean hasHindiCaregiver = containsHindiCaregiverAny(s);
        if (hasHindiSend && (hasHindiMsg || hasHindiCaregiver)) {
            return true;
        }
        if (hasHindiMsg && containsHindiCaregiverDative(s)) {
            return true;
        }

        boolean hasMalayalamSend = s.contains("അയയ്ക്കൂ") || s.contains("അയക്കൂ") || s.contains("അയക്കുക") || s.contains("അയക്കണം");
        boolean hasMalayalamMsg = s.contains("സന്ദേശം") || s.contains("മെസേജ്") || s.contains("മെസ്സേജ്");
        boolean hasMalayalamCaregiver = containsMalayalamCaregiverAny(s);
        if (hasMalayalamSend && (hasMalayalamMsg || hasMalayalamCaregiver)) {
            return true;
        }
        if (hasMalayalamMsg && containsMalayalamCaregiverDative(s)) {
            return true;
        }

        boolean hasSend = words.contains("send") || words.contains("write") || words.contains("text");
        boolean hasMsg = words.contains("message") || words.contains("messages") || words.contains("text");
        boolean hasCaregiver = containsEnglishCaregiverAny(s, words);

        if (hasSend && (hasMsg || hasCaregiver)) return true;
        if (hasMsg && hasCaregiver) return true;

        return false;
    }

    private static boolean matchesRepeatMessage(String s, Set<String> words) {
        // Disallow generic single/isolated words from triggering REPEAT_MESSAGE
        if (s.equals("repeat") || s.equals("message") || s.equals("again") || s.equals("say") || s.equals("that") ||
                s.equals("messages") || s.equals("how many") || s.equals("count") ||
                s.equals("caregiver") || s.equals("carer") || s.equals("caretaker") ||
                s.equals("care worker") || s.equals("helper") ||
                // Kannada
                s.equals("ಮತ್ತೆ") || s.equals("ಮತ್ತೊಮ್ಮೆ") || s.equals("ಇನ್ನೊಮ್ಮೆ") ||
                s.equals("ಸಂದೇಶ") || s.equals("ಹೇಳಿ") || s.equals("ಓದಿ") || s.equals("ಮೆಸೇಜ್") ||
                s.equals("ಕೇರ್ಗಿವರ್") || s.equals("ಕೇರ್‌ಗಿವರ್") || s.equals("ಸಹಾಯಕ") || s.equals("ಪಾಲಕರು") ||
                s.equals("ಪುನರಾವರ್ತಿಸಿ") ||
                // Hindi
                s.equals("फिर") || s.equals("संदेश") || s.equals("बताओ") || s.equals("पढ़ो") || s.equals("दोबारा") || s.equals("मैसेज") || s.equals("केयरगिवर") || s.equals("दोहराओ") || s.equals("बोलो") ||
                // Malayalam
                s.equals("വീണ്ടും") || s.equals("സന്ദേശം") || s.equals("പറയൂ") || s.equals("വായിക്കൂ") || s.equals("മെസേജ്") || s.equals("കെയർഗിവർ") || s.equals("പറയുക") || s.equals("വായിക്കുക")) {
            return false;
        }

        // Exclude voice messages/recording, sending, and counting actions
        if (words.contains("voice") || words.contains("audio") || words.contains("recorder") || words.contains("record") ||
                words.contains("send") || words.contains("count") || words.contains("how") ||
                words.contains("ಕಳುಹಿಸಿ") || words.contains("भेजो") || words.contains("അയക്കൂ") ||
                words.contains("ಎಣಿಸಿ") || words.contains("गिनो") || words.contains("എണ്ണൂ")) {
            return false;
        }

        if (containsMatch(s, words,
                // English
                "repeat message", "repeat the message", "repeat caregiver message", "repeat caregiver messages",
                "repeat my message", "say that again", "repeat that", "repeat the last message",
                "repeat last message", "repeat the caregiver message", "repeat what you said",
                "say the message again", "say that message again", "tell me that again",
                "say again", "repeat again", "say it again", "repeat it", "say it one more time", "tell me again", "i didn t hear that",
                // English Task 27 aliases
                "repeat my caregiver message", "repeat the caregiver message",
                "repeat my carer message", "repeat the carer message",
                "repeat my caretaker message", "repeat the caretaker message",
                "repeat my care worker message", "repeat the care worker message",
                "repeat my helper message", "repeat the helper message",
                "say my caregiver message again", "say my carer message again",
                "say my caretaker message again", "say my care worker message again",
                "say my helper message again",
                "read my caregiver message again", "read my carer message again",
                "read my caretaker message again", "read my care worker message again",
                "read my helper message again",
                "tell me that again", "repeat that message",
                // Kannada
                "ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ", "ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ", "ಮತ್ತೆ ಸಂದೇಶ ಹೇಳಿ", "ಆ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಆ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ", "ಮತ್ತೆ ಹೇಳಿ", "ಮತ್ತೊಮ್ಮೆ ಹೇಳಿ", "ಮತ್ತೆ ಓದಿ", "ಮತ್ತೊಮ್ಮೆ ಓದಿ",
                "ಸಂದೇಶವನ್ನು ಮತ್ತೊಮ್ಮೆ ಓದಿ", "ಸಂದೇಶ ಮತ್ತೊಮ್ಮೆ ಓದಿ", "ಇದನ್ನು ಮತ್ತೆ ಹೇಳಿ", "ಇನ್ನೊಮ್ಮೆ ಹೇಳಿ",
                "ಮತ್ತೊಮ್ಮೆ ಕೇಳಿಸಿ", "ಮತ್ತೆ ಕೇಳಿಸಿ", "ಸಂದೇಶವನ್ನು ಪುನರಾವರ್ತಿಸಿ", "ಸಂದೇಶ ಪುನರಾವರ್ತಿಸಿ",
                "ಅದನ್ನು ಮತ್ತೆ ಹೇಳಿ", "ಆ ಮೆಸೇಜ್ ಮತ್ತೆ ಹೇಳಿ", "ಮೆಸೇಜ್ ಮತ್ತೆ ಹೇಳಿ", "ಮೆಸೇಜ್ ಮತ್ತೊಮ್ಮೆ ಹೇಳಿ",
                "ಕೊನೆಯ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ", "ಕೊನೆಯ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ", "ಅದನ್ನು ಇನ್ನೊಮ್ಮೆ ಓದಿ", "ಇನ್ನೊಂದು ಸಲ ಹೇಳಿ", "ಪುನರಾವರ್ತಿಸಿ",
                // Caregivers
                "ಪಾಲಕರ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ", "ಪಾಲಕರ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ", "ಪಾಲಕನ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ", "ಪಾಲಕನ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಪಾಲಕರ ಮೆಸೇಜ್ ಮತ್ತೆ ಹೇಳಿ", "ಪಾಲಕರ ಸಂದೇಶವನ್ನು ಪುನರಾವರ್ತಿಸಿ", "ನನ್ನ ಪಾಲಕರ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಆರೈಕೆದಾರರ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ", "ಆರೈಕೆದಾರನ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಆರೈಕೆದಾರರ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ", "ಆರೈಕೆದಾರನ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಆರೈಕೆದಾರರ ಮೆಸೇಜ್ ಮತ್ತೆ ಹೇಳಿ", "ಆರೈಕೆದಾರನ ಮೆಸೇಜ್ ಮತ್ತೆ ಹೇಳಿ",
                "ಆರೈಕೆದಾರರ ಸಂದೇಶವನ್ನು ಪುನರಾವರ್ತಿಸಿ", "ಆರೈಕೆದಾರನ ಸಂದೇಶವನ್ನು ಪುನರಾವರ್ತಿಸಿ",
                "ಸಹಾಯಕನ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ", "ಸಹಾಯಕರ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಸಹಾಯಕನ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ", "ಸಹಾಯಕರ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಸಹಾಯಕನ ಮೆಸೇಜ್ ಮತ್ತೆ ಹೇಳಿ", "ಸಹಾಯಕರ ಮೆಸೇಜ್ ಮತ್ತೆ ಹೇಳಿ",
                "ಸಹಾಯಕನ ಸಂದೇಶವನ್ನು ಪುನರಾವರ್ತಿಸಿ", "ಸಹಾಯಕರ ಸಂದೇಶವನ್ನು ಪುನರಾವರ್ತಿಸಿ",
                "ನನ್ನ ಸಹಾಯಕನ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ", "ನನ್ನ ಸಹಾಯಕರ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಕೇರ್ಗಿವರ್ನ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ", "ಕೇರ್ಗಿವರ್ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಕೇರ್ಗಿವರ್ನ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ", "ಕೇರ್ಗಿವರ್ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಕೇರ್ಗಿವರ್ನ ಮೆಸೇಜ್ ಮತ್ತೆ ಹೇಳಿ", "ಕೇರ್ಗಿವರ್ ಮೆಸೇಜ್ ಮತ್ತೆ ಹೇಳಿ",
                "ಕೇರ್ಗಿವರ್ನ ಸಂದೇಶವನ್ನು ಪುನರಾವರ್ತಿಸಿ",
                "ಕೇರ್ಟೇಕರ್ನ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ", "ಕೇರ್ಟೇಕರ್ ಸಂದೇಶವನ್ನು ಮತ್ತೆ ಹೇಳಿ",
                "ಕೇರ್ಟೇಕರ್ನ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ", "ಕೇರ್ಟೇಕರ್ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ",
                "ಕೇರ್ಟೇಕರ್ನ ಮೆಸೇಜ್ ಮತ್ತೆ ಹೇಳಿ", "ಕೇರ್ಟೇಕರ್ ಮೆಸೇಜ್ ಮತ್ತೆ ಹೇಳಿ",
                "ಕೇರ್ಟೇಕರ್ನ ಸಂದೇಶವನ್ನು ಪುನರಾವರ್ತಿಸಿ",
                // Hindi
                "संदेश फिर से पढ़ो", "संदेश फिर से बताओ", "संदेश दोबारा पढ़ो", "संदेश दोबारा बताओ",
                "उस संदेश को फिर से पढ़ो", "उस संदेश को दोबारा बताओ",
                "केयरगिवर का संदेश फिर से पढ़ो", "केयरगिवर का संदेश दोबारा बताओ",
                "मैसेज फिर से पढ़ो", "मैसेज दोबारा पढ़ो", "फिर से बताओ", "वह फिर से बताओ",
                "आखिरी संदेश फिर से पढ़ो", "आखिरी संदेश दोबारा बताओ", "वही संदेश फिर से बताओ",
                "फिर से बोलो", "संदेश दोहराओ", "संदेश दोहराएं", "मैसेज फिर से बोलो", "मैसेज दोहराओ", "मैसेज दोहराएं", "दोबारा बोलो",
                "संदेश दोबारा सुनाओ", "एक बार फिर कहो", "दोहराओ", "फिर से सुनाइए", "अंतिम संदेश दोबारा बोलो",
                // Hindi Task 25 aliases
                "देखभाल करने वाले का संदेश फिर से सुनाओ", "मेरे देखभाल करने वाले का संदेश फिर से सुनाओ",
                "देखभाल करने वाले का संदेश फिर से बताओ", "मेरे देखभाल करने वाले का संदेश फिर से बताओ",
                "देखभाल करने वाले का संदेश दोबारा सुनाओ", "मेरे देखभाल करने वाले का संदेश दोबारा सुनाओ",
                "देखभाल करने वाले का संदेश दोबारा बताओ", "मेरे देखभाल करने वाले का संदेश दोबारा बताओ",
                "देखभाल करने वाले का मैसेज फिर से सुनाओ", "मेरे देखभाल करने वाले का मैसेज फिर से सुनाओ",
                "देखभाल करने वाले का मैसेज दोबारा सुनाओ", "मेरे देखभाल करने वाले का मैसेज दोबारा सुनाओ",
                "सहायक का संदेश फिर से सुनाओ", "मेरे सहायक का संदेश फिर से सुनाओ",
                "सहायक का संदेश फिर से बताओ", "मेरे सहायक का संदेश फिर से बताओ",
                "सहायक का संदेश दोबारा सुनाओ", "मेरे सहायक का संदेश दोबारा सुनाओ",
                "सहायक का संदेश दोबारा बताओ", "मेरे सहायक का संदेश दोबारा बताओ",
                "सहायक का मैसेज फिर से सुनाओ", "मेरे सहायक का मैसेज फिर से सुनाओ",
                "सहायक का मैसेज दोबारा सुनाओ", "मेरे सहायक का मैसेज दोबारा सुनाओ",
                "केयरगिवर का संदेश फिर से सुनाओ", "मेरे केयरगिवर का संदेश फिर से सुनाओ",
                "केयरगिवर का संदेश दोबारा सुनाओ", "मेरे केयरगिवर का संदेश दोबारा सुनाओ",
                "केयरगिवर का संदेश फिर से बताओ", "मेरे केयरगिवर का संदेश फिर से बताओ",
                "केयरगिवर का संदेश दोबारा बताओ", "मेरे केयरगिवर का संदेश दोबारा बताओ",
                "केयरगिवर का मैसेज फिर से सुनाओ", "मेरे केयरगिवर का मैसेज दोबारा सुनाओ",
                "केयरटेकर का संदेश फिर से बताओ", "मेरे केयरटेकर का संदेश फिर से बताओ",
                "केयरटेकर का संदेश फिर से सुनाओ", "मेरे केयरटेकर का संदेश फिर से सुनाओ",
                "केयरटेकर का संदेश दोबारा सुनाओ", "मेरे केयरटेकर का संदेश दोबारा सुनाओ",
                "केयरटेकर का संदेश दोबारा बताओ", "मेरे केयरटेकर का संदेश दोबारा बताओ",
                "केयरटेकर का मैसेज फिर से बताओ", "केयरटेकर का मैसेज दोबारा सुनाओ",
                "फिर से सुनाओ", "दोबारा सुनाओ",
                // Malayalam
                "സന്ദേശം വീണ്ടും പറയൂ", "സന്ദേശം വീണ്ടും വായിക്കൂ", "സന്ദേശം വീണ്ടും വായിക്കുക", "സന്ദേശം വീണ്ടും പറയുക",
                "ആ സന്ദേശം വീണ്ടും പറയൂ", "ആ സന്ദേശം വീണ്ടും വായിക്കൂ",
                "കെയർഗിവറിന്റെ സന്ദേശം വീണ്ടും പറയൂ", "കെയർഗിവറുടെ സന്ദേശം വീണ്ടും പറയൂ", "കെയർഗിവർ സന്ദേശം വീണ്ടും വായിക്കൂ",
                "മെസേജ് വീണ്ടും പറയൂ", "മെസേജ് വീണ്ടും വായിക്കൂ", "വീണ്ടും പറയൂ", "അത് വീണ്ടും പറയൂ",
                "അവസാന സന്ദേശം വീണ്ടും പറയൂ", "അവസാന സന്ദേശം വീണ്ടും വായിക്കൂ",
                "സന്ദേശം ആവർത്തിക്കുക", "വീണ്ടും കേൾപ്പിക്കുക", "വീണ്ടും പറയുക", "ഒരിക്കൽ കൂടി പറയൂ", "ഒന്ന് കൂടി പറയൂ", "അത് വീണ്ടും വായിക്കൂ",
                // Malayalam Task 26 aliases
                "പരിചരിക്കുന്നയാളുടെ സന്ദേശം വീണ്ടും കേൾപ്പിക്കൂ", "എന്റെ പരിചരിക്കുന്നയാളുടെ സന്ദേശം വീണ്ടും കേൾപ്പിക്കൂ",
                "പരിചരിക്കുന്നയാളുടെ സന്ദേശം വീണ്ടും പറയൂ", "എന്റെ പരിചരിക്കുന്നയാളുടെ സന്ദേശം വീണ്ടും പറയൂ",
                "പരിചാരകന്റെ സന്ദേശം വീണ്ടും കേൾപ്പിക്കൂ", "എന്റെ പരിചാരകന്റെ സന്ദേശം വീണ്ടും കേൾപ്പിക്കൂ",
                "പരിചാരകന്റെ സന്ദേശം വീണ്ടും പറയൂ", "എന്റെ പരിചാരകന്റെ സന്ദേശം വീണ്ടും പറയൂ",
                "സഹായിയുടെ സന്ദേശം വീണ്ടും കേൾപ്പിക്കൂ", "എന്റെ സഹായിയുടെ സന്ദേശം വീണ്ടും കേൾപ്പിക്കൂ",
                "സഹായിയുടെ സന്ദേശം വീണ്ടും പറയೂ", "എന്റെ സഹായിയുടെ സന്ദേശം വീണ്ടും പറയൂ",
                "കെയർഗിവറിന്റെ സന്ദേശം വീണ്ടും കേൾപ്പിക്കൂ", "എന്റെ കെയർഗിവറിന്റെ സന്ദേശം വീണ്ടും കേൾപ്പിക്കൂ",
                "കെയർഗിവറിന്റെ സന്ദേശം വീണ്ടും പറയൂ", "എന്റെ കെയർഗിവറിന്റെ സന്ദേശം വീണ്ടും പറയൂ",
                "കെയർടേക്കറിന്റെ സന്ദേശം വീണ്ടും കേൾപ്പിക്കൂ", "എന്റെ കെയർടേക്കറിന്റെ സന്ദേശം വീണ്ടും കേൾപ്പിക്കൂ",
                "കെയർടേക്കറിന്റെ സന്ദേശം വീണ്ടും പറയൂ", "എന്റെ കെയർടേക്കറിന്റെ സന്ദേശം വീണ്ടും പറയൂ",
                "പരിചരിക്കുന്നയാളുടെ മെസേജ് വീണ്ടും പറയൂ", "പരിചാരകന്റെ മെസേജ് വീണ്ടും പറയൂ",
                "സഹായിയുടെ മെസേജ് വീണ്ടും പറയൂ", "കെയർഗിവറിന്റെ മെസേജ് വീണ്ടും പറയൂ", "കെയർടേക്കറിന്റെ മെസേജ് വീണ്ടും പറയൂ",
                "ഒരിക്കൽ കൂടി പറയൂ", "ഒന്ന് കൂടി പറയൂ", "വീണ്ടും കേൾപ്പിക്കൂ", "വീണ്ടും പറയൂ")) {
            return true;
        }

        boolean hasKannadaRepeat = s.contains("ಮತ್ತೆ") || s.contains("ಮತ್ತೊಮ್ಮೆ") || s.contains("ಪುನರಾವರ್ತಿಸಿ") || s.contains("ಇನ್ನೊಮ್ಮೆ");
        boolean hasKannadaTarget = s.contains("ಸಂದೇಶ") || s.contains("ಮೆಸೇಜ್") || s.contains("ಹೇಳಿ") ||
                s.contains("ಅದನ್ನು") || containsKannadaCaregiverAny(s);
        if (hasKannadaRepeat && hasKannadaTarget) {
            return true;
        }

        boolean hasHindiRepeat = s.contains("फिर से") || s.contains("दोबारा") || s.contains("दोहराओ") || s.contains("एक बार फिर");
        boolean hasHindiAction = s.contains("सुनाओ") || s.contains("बताओ") || s.contains("पढ़ो") || s.contains("बोलो") || s.contains("सुनाइए");
        boolean hasHindiTarget = s.contains("संदेश") || s.contains("मैसेज") || containsHindiCaregiverAny(s);
        if (hasHindiRepeat && (hasHindiAction || hasHindiTarget)) {
            return true;
        }

        boolean hasMalayalamRepeat = s.contains("വീണ്ടും") || s.contains("ഒരിക്കൽ കൂടി") || s.contains("ഒന്ന് കൂടി") || s.contains("ആവർത്തിക്കുക");
        boolean hasMalayalamAction = s.contains("പറയൂ") || s.contains("പറയുക") || s.contains("കേൾപ്പിക്കൂ") || s.contains("കേൾപ്പിക്കുക") || s.contains("വായിക്കൂ") || s.contains("വായിക്കുക");
        boolean hasMalayalamTarget = s.contains("സന്ദേശം") || s.contains("മെസേജ്") || containsMalayalamCaregiverAny(s) || s.contains("അത്") || s.contains("അവസാന");
        if (hasMalayalamRepeat && (hasMalayalamAction || hasMalayalamTarget)) {
            return true;
        }

        boolean hasRepeat = words.contains("repeat") ||
                (words.contains("say") && words.contains("again")) ||
                (words.contains("tell") && words.contains("again")) ||
                (words.contains("read") && words.contains("again"));
        boolean hasEnglishCaregiver = containsEnglishCaregiverAny(s, words);
        boolean hasTarget = words.contains("message") || words.contains("messages") ||
                words.contains("that") || words.contains("it") || hasEnglishCaregiver;
        if (hasRepeat && hasTarget) {
            return true;
        }

        return false;
    }

    private static boolean matchesGoBack(String s, Set<String> words) {
        // Disallow generic single/isolated words from triggering GO_BACK
        if (s.equals("back") || s.equals("go") || s.equals("previous") || s.equals("page") ||
                s.equals("backward") ||
                s.equals("ಹಿಂದೆ") || s.equals("ಹೋಗಿ") || s.equals("ಪುಟ") || s.equals("ವಾಪಸ್") ||
                s.equals("ಹೋಗು") || s.equals("ಬನ್ನಿ") ||
                s.equals("वापस") || s.equals("पीछे") || s.equals("जाओ") || s.equals("पेज") ||
                s.equals("തിരികെ") || s.equals("പോകുക") || s.equals("പോകൂ") || s.equals("പേജ്")) {
            return false;
        }

        // Exclude home navigation, voice recording, emergency, calling, and reading/sending messages
        if (words.contains("home") || s.contains("home") || s.contains("ಮುಖಪುಟ") || s.contains("ಮನೆ") ||
                s.contains("ಹೋಮ್") || s.contains("घर") || s.contains("मुख्य") || s.contains("main") || s.contains("ഹോം") ||
                words.contains("voice") || words.contains("audio") || words.contains("recorder") || words.contains("record") ||
                words.contains("emergency") || words.contains("sos") || words.contains("call") ||
                words.contains("send") || words.contains("read") || words.contains("count") || words.contains("repeat")) {
            return false;
        }

        if (containsMatch(s, words,
                "go back", "go back please", "go backward", "return back", "return", "previous page",
                "back please", "take me back", "navigate back", "previous screen", "go to previous screen",
                "go back to previous page", "go back to previous screen", "back to previous page", "back to previous screen",
                "go to previous page", "return to previous page", "return to previous screen", "take me to previous page",
                "previous page please", "open previous page", "open previous screen",
                "go to old page", "old page", "open old page", "go back to old page",
                // Kannada
                "ಹಿಂದಕ್ಕೆ ಹೋಗಿ", "ಹಿಂದೆ ಹೋಗಿ", "ಹಿಂದಕ್ಕೆ ಹೋಗು", "ಹಿಂದೆ ಹೋಗು",
                "ಹಿಂದಕ್ಕೆ ಬಾ", "ಹಿಂದೆ ಬಾ", "ಮರಳಿ ಬಾ", "ಮರಳಿ ಬನ್ನಿ", "ಮರಳಿ ಹೋಗು", "ಮರಳಿ ಹೋಗಿ",
                "ಹಿಂದಿನ ಪುಟಕ್ಕೆ ಹೋಗಿ", "ಹಿಂದಿನ ಪುಟಕ್ಕೆ ಹೋಗು", "ಹಿಂದಿನ ಪುಟಕ್ಕೆ ಬಾ", "ಹಿಂದಿನ ಪುಟಕ್ಕೆ ಬನ್ನಿ", "ಹಿಂದಿನ ಪುಟಕ್ಕೆ ಹಿಂತಿರುಗಿ",
                "ಹಿಂದಕ್ಕೆ ಬನ್ನಿ", "ಹಿಂದೆ ಬನ್ನಿ", "ವಾಪಸ್ ಹೋಗಿ", "ವಾಪಸ್ ಹೋಗು", "ವಾಪಸ್ ಬಾ", "ವಾಪಸ್ ಬನ್ನಿ",
                "ಹಿಂದಿನ ಪುಟ", "ಹಿಂದಕ್ಕೆ", "ಹಿಂದಿನ ಸ್ಕ್ರೀನ್‌ಗೆ ಹೋಗಿ", "ಹಿಂದಿನ ಸ್ಕ್ರೀನ್‌ಗೆ ಹೋಗು", "ದಯವಿಟ್ಟು ಹಿಂದೆ ಹೋಗಿ", "ದಯವಿಟ್ಟು ಹಿಂದೆ ಹೋಗು",
                "ಹಿಂತಿರುಗು", "ಹಿಂತಿರುಗಿ",
                "ಹಿಂದಿನ ಪುಟ ತೆರೆಯಿರಿ", "ಹಿಂದಿನ ಪುಟ ತೋರಿಸಿ", "ಹಿಂದಿನ ಪೇಜ್", "ಹಿಂದಿನ ಪೇಜ್‌ಗೆ ಹೋಗು", "ಹಿಂದಿನ ಪೇಜ್‌ಗೆ ಹೋಗಿ",
                "ಹಳೆಯ ಪುಟಕ್ಕೆ ಹೋಗು", "ಹಳೇ ಪುಟಕ್ಕೆ ಹೋಗು", "ಹಿಂದಿನ ಸ್ಕ್ರೀನ್",
                "ಗೋ ಬ್ಯಾಕ್", "ಗೋ ಬ್ಯಾಕ್ ಪ್ಲೀಸ್", "ಬ್ಯಾಕ್ ಹೋಗು", "ಬ್ಯಾಕ್ ಹೋಗಿ", "ಬ್ಯಾಕ್ ಬಾ", "ಬ್ಯಾಕ್ ಬನ್ನಿ",
                // Hindi
                "वापस जाओ", "पीछे जाओ", "वापस जाइए", "वापस चलो", "पिछला पेज", "पिछले पेज पर जाओ",
                "पिछली स्क्रीन पर जाओ", "पीछे चलो", "कृपया वापस जाएं", "वापस आओ", "पिछला स्क्रीन दिखाओ",
                "पीछे जाएं", "पीछे जाइए", "वापस जाएं", "पीछे चलो", "पीछे",
                // Malayalam
                "തിരികെ പോകൂ", "പിന്നിലേക്ക് പോകൂ", "വീണ്ടും പിന്നിലേക്ക് പോകൂ", "തിരികെ പോകുക", "പിന്നിലേക്ക് പോകുക",
                "പിന്നിലേക്ക്", "മുമ്പത്തെ പേജ്", "മുമ്പത്തെ സ്ക്രീനിലേക്ക് പോകുക", "തിരിച്ചു പോകുക", "ദയവായി പിന്നോട്ട് പോകുക", "തിരികെ വരൂ", "മുമ്പത്തെ പേജിലേക്ക് പോകൂ", "പിന്നോട്ട് പോകുക", "ബാക്ക്")) {
            return true;
        }

        boolean hasKannadaBack = s.contains("ಹಿಂದೆ") || s.contains("ಹಿಂದಕ್ಕೆ") || s.contains("ಹಿಂದಿನ") || s.contains("ವಾಪಸ್") || s.contains("ಹಿಂತಿರುಗಿ") || s.contains("ಮರಳಿ") || s.contains("ಬ್ಯಾಕ್");
        boolean hasKannadaNavAction = s.contains("ಹೋಗಿ") || s.contains("ಹೋಗು") || s.contains("ಬನ್ನಿ") || s.contains("ಬಾ") || s.contains("ಪುಟ") || s.contains("ಹಿಂತಿರುಗಿ") || s.contains("ಮರಳಿ") || s.contains("ಹಿಂತಿರುಗು") || s.contains("ಪೇಜ್") || s.contains("ಸ್ಕ್ರೀನ್");
        if (hasKannadaBack && hasKannadaNavAction) {
            return true;
        }

        boolean hasHindiBack = s.contains("वापस") || s.contains("पीछे") || s.contains("पिछला") || s.contains("पिछली");
        boolean hasHindiNavAction = s.contains("जाओ") || s.contains("जाएं") || s.contains("जाइए") || s.contains("चलो") || s.contains("आओ") || s.contains("पेज") || s.contains("स्क्रीन");
        if (hasHindiBack && hasHindiNavAction) {
            return true;
        }

        boolean hasBackAction = words.contains("go") || words.contains("return") || words.contains("navigate") || words.contains("take");
        boolean hasBackTarget = words.contains("back") || words.contains("backward");
        if (hasBackAction && hasBackTarget) {
            return true;
        }

        boolean hasPreviousPage = words.contains("previous") && (words.contains("page") || words.contains("screen"));
        if (hasPreviousPage) {
            return true;
        }

        return false;
    }

    private static boolean matchesMessageCount(String s, Set<String> words) {
        // Disallow generic single/isolated words from triggering MESSAGE_COUNT (must remain UNKNOWN)
        if (s.equals("how many") || s.equals("messages") || s.equals("message") || s.equals("count") || s.equals("new") ||
                s.equals("caregiver") || s.equals("carer") || s.equals("caretaker") ||
                s.equals("care worker") || s.equals("helper") ||
                s.equals("repeat") || s.equals("again") || s.equals("say") || s.equals("that") ||
                // Kannada
                s.equals("ಎಷ್ಟು") || s.equals("ಸಂದೇಶ") || s.equals("ಸಂದೇಶಗಳು") || s.equals("ಮೆಸೇಜ್") || s.equals("ಎಣಿಸಿ") ||
                s.equals("ಕೇರ್ಗಿವರ್") || s.equals("ಕೇರ್‌ಗಿವರ್") || s.equals("ಸಹಾಯಕ") || s.equals("ಪಾಲಕರು") ||
                // Hindi
                s.equals("कितने") || s.equals("संदेश") || s.equals("मैसेज") || s.equals("गिनो") || s.equals("नए") || s.equals("केयरगिवर") ||
                // Malayalam
                s.equals("എത്ര") || s.equals("സന്ദേശം") || s.equals("സന്ദേശങ്ങൾ") || s.equals("മെസേജ്") || s.equals("എണ്ണൂ") || s.equals("എണ്ണുക") || s.equals("പുതിയ") || s.equals("കെയർഗിവർ")) {
            return false;
        }

        // Exclude voice messages/recording, sending, repeating, and reading actions
        if (words.contains("voice") || words.contains("audio") || words.contains("recorder") || words.contains("record") ||
                words.contains("send") || words.contains("write") || words.contains("read") || words.contains("check") ||
                words.contains("repeat") || words.contains("again") ||
                s.contains("ಓದಿ") || s.contains("ಓದು") || s.contains("ಹೇಳಿ") || s.contains("ಹೇಳು") ||
                s.contains("ಪರಿಶೀಲಿಸಿ") || s.contains("ಕಳುಹಿಸಿ") || s.contains("ಕಳಿಸಿ") ||
                words.contains("पढ़ो") || words.contains("चेक") || words.contains("भेजो") ||
                words.contains("വായിക്കൂ") || words.contains("പരിശോധിക്കൂ") || words.contains("അയക്കൂ")) {
            return false;
        }

        if (containsMatch(s, words,
                // English
                "how many messages", "how many messages do i have", "count messages", "count my messages",
                "how many caregiver messages", "how many new messages", "count the messages", "count caregiver messages",
                "how many caregiver messages do i have", "tell me how many messages i have", "what is the message count",
                "how many messages are there", "how many unread messages", "count my caregiver messages",
                "tell me how many messages i got", "do i have any messages", "total messages", "check message count",
                // English Task 27 aliases
                "how many messages from my caregiver", "how many messages does my caregiver have", "how many messages do i have from my caregiver",
                "how many messages from my carer", "how many messages does my carer have", "how many messages has my carer sent",
                "how many messages from my caretaker", "how many messages does my caretaker have",
                "how many messages from my care worker", "how many messages does my care worker have",
                "how many messages from my helper", "how many messages does my helper have",
                "count my caregiver messages", "count the caregiver messages",
                "count my carer messages", "count the carer messages",
                "count my caretaker messages", "count the caretaker messages",
                "count my care worker messages", "count the care worker messages",
                "count my helper messages", "count the helper messages",
                // Kannada
                "ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ", "ಎಷ್ಟು ಸಂದೇಶ ಇದೆ", "ಎಷ್ಟು ಸಂದೇಶಗಳು ಬಂದಿವೆ", "ಎಷ್ಟು ಸಂದೇಶ ಬಂದಿದೆ",
                "ನನಗೆ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ", "ನನ್ನ ಸಂದೇಶಗಳು ಎಷ್ಟಿವೆ", "ಎಷ್ಟು ಸಂದೇಶ ಬಂದಿವೆ", "ಎಷ್ಟು ಮೆಸೇಜ್ಗಳಿವೆ",
                "ಎಷ್ಟು ಮೆಸೇಜ್ ಬಂದಿದೆ", "ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ", "ನನ್ನ ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ", "ಸಂದೇಶ ಎಣಿಸಿ",
                "ಎಷ್ಟು ಸಂದೇಶಗಳು", "ನನ್ನ ಬಳಿ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ", "ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ ನನ್ನ ಬಳಿ",
                "ಎಷ್ಟು ಹೊಸ ಸಂದೇಶಗಳಿವೆ", "ನನ್ನ ಬಳಿ ಎಷ್ಟು ಹೊಸ ಸಂದೇಶಗಳಿವೆ",
                "ಎಷ್ಟು ಮೆಸೇಜುಗಳಿವೆ", "ನನ್ನ ಮೆಸೇಜ್ಗಳನ್ನು ಎಣಿಸಿ", "ಎಷ್ಟು ಕೇರ್ಗಿವರ್ ಮೆಸೇಜ್ಗಳಿವೆ", "ಎಷ್ಟು ಕೇರ್‌ಗಿವರ್ ಮೆಸೇಜ್ಗಳಿವೆ",
                "ಎಷ್ಟು ಕೇರ್ಗಿವರ್ ಸಂದೇಶಗಳಿವೆ", "ಎಷ್ಟು ಕೇರ್‌ಗಿವರ್ ಸಂದೇಶಗಳಿವೆ",
                "ನನಗೆ ಸಂದೇಶಗಳು ಬಂದಿವೆಯೇ", "ಸಂದೇಶಗಳ ಸಂಖ್ಯೆ ಎಷ್ಟು", "ಮೆಸೇಜ್ ಕೌಂಟ್ ಎಷ್ಟು",
                // Caregiver variations
                "ಪಾಲಕರಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ", "ಪಾಲಕನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ", "ಪಾಲಕರ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಪಾಲಕರಿಂದ ಎಷ್ಟು ಮೆಸೇಜ್ಗಳಿವೆ", "ಪಾಲಕರಿಂದ ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ", "ಪಾಲಕರ ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ",
                "ನನ್ನ ಪಾಲಕರಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ", "ನನ್ನ ಪಾಲಕರ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಆರೈಕೆದಾರರಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ", "ಆರೈಕೆದಾರನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಆರೈಕೆದಾರರ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ", "ಆರೈಕೆದಾರರಿಂದ ಎಷ್ಟು ಮೆಸೇಜ್ಗಳಿವೆ",
                "ಆರೈಕೆದಾರರಿಂದ ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ", "ಆರೈಕೆದಾರರ ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ",
                "ಸಹಾಯಕನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ", "ಸಹಾಯಕರಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಸಹಾಯಕನ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ", "ಸಹಾಯಕನಿಂದ ಎಷ್ಟು ಮೆಸೇಜ್ಗಳಿವೆ",
                "ಸಹಾಯಕನಿಂದ ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ", "ಸಹಾಯಕರ ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ",
                "ನನ್ನ ಸಹಾಯಕನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ", "ನನ್ನ ಸಹಾಯಕರಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಕೇರ್ಗಿವರ್ನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ", "ಕೇರ್ಗಿವರ್ನ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಕೇರ್ಗಿವರ್ನಿಂದ ಎಷ್ಟು ಮೆಸೇಜ್ಗಳಿವೆ", "ಕೇರ್ಗಿವರ್ನಿಂದ ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ",
                "ಕೇರ್ಟೇಕರ್ನಿಂದ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ", "ಕೇರ್ಟೇಕರ್ನ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ",
                "ಕೇರ್ಟೇಕರ್ನಿಂದ ಎಷ್ಟು ಮೆಸೇಜ್ಗಳಿವೆ", "ಕೇರ್ಟೇಕರ್ನಿಂದ ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ",
                // Hindi
                "कितने संदेश हैं", "मेरे पास कितने संदेश हैं", "संदेश गिनो", "मेरे संदेश गिनो",
                "कितने केयरगिवर संदेश हैं", "कितने नए संदेश हैं", "मेरे पास कितने नए संदेश हैं", "मुझे बताओ कितने संदेश हैं",
                "कितने मैसेज हैं", "मेरे पास कितने मैसेज हैं", "मैसेज गिनो", "मेरे मैसेज गिनो",
                "कितने केयरगिवर संदेश", "कितने नए मैसेज", "मैसेज कितने हैं",
                "मैसेज की गिनती बताओ", "क्या मेरे पास कोई संदेश है", "कुल कितने मैसेज हैं", "कितने संदेश बाकी हैं",
                // Hindi Task 25 aliases
                "देखभाल करने वाले के कितने संदेश हैं", "मेरे देखभाल करने वाले के कितने संदेश हैं",
                "देखभाल करने वाले के कितने मैसेज हैं", "मेरे देखभाल करने वाले के कितने मैसेज हैं",
                "देखभाल करने वाले से कितने संदेश हैं", "मेरे देखभाल करने वाले से कितने संदेश हैं",
                "देखभाल करने वाले के संदेश गिनो", "मेरे देखभाल करने वाले के संदेश गिनो",
                "देखभाल करने वाले के मैसेज गिनो", "मेरे देखभाल करने वाले के मैसेज गिनो",
                "सहायक के कितने संदेश हैं", "मेरे सहायक के कितने संदेश हैं",
                "सहायक के कितने मैसेज हैं", "मेरे सहायक के कितने मैसेज हैं",
                "सहायक से कितने संदेश हैं", "मेरे सहायक से कितने संदेश हैं",
                "सहायक के संदेश गिनो", "मेरे सहायक के संदेश गिनो",
                "सहायक के मैसेज गिनो", "मेरे सहायक के मैसेज गिनो",
                "केयरगिवर के कितने संदेश हैं", "मेरे केयरगिवर के कितने संदेश हैं",
                "केयरगिवर के कितने मैसेज हैं", "मेरे केयरगिवर के कितने मैसेज हैं",
                "केयरगिवर से कितने संदेश हैं", "मेरे केयरगिवर से कितने संदेश हैं",
                "केयरगिवर के संदेश गिनो", "मेरे केयरगिवर के संदेश गिनो",
                "केयरटेकर के कितने संदेश हैं", "मेरे केयरटेकर के कितने संदेश हैं",
                "केयरटेकर के कितने मैसेज हैं", "मेरे केयरटेकर के कितने मैसेज हैं",
                "केयरटेकर से कितने संदेश हैं", "मेरे केयरटेकर से कितने संदेश हैं",
                "केयरटेकर के संदेश गिनो", "मेरे केयरटेकर के संदेश गिनो",
                // Malayalam
                "എത്ര സന്ദേശങ്ങളുണ്ട്", "എന്റെ പക്കൽ എത്ര സന്ദേശങ്ങളുണ്ട്", "സന്ദേശങ്ങൾ എണ്ണൂ", "എന്റെ സന്ദേശങ്ങൾ എണ്ണൂ",
                "എത്ര കെയർഗിവർ സന്ദേശങ്ങളുണ്ട്", "എത്ര പുതിയ സന്ദേശങ്ങളുണ്ട്", "എന്റെ പക്കൽ എത്ര പുതിയ സന്ദേശങ്ങളുണ്ട്", "എത്ര സന്ദേശങ്ങൾ ഉണ്ടെന്ന് പറയൂ",
                "എത്ര മെസേജുകളുണ്ട്", "എന്റെ പക്കൽ എത്ര മെസേജുകളുണ്ട്", "മെസേജുകൾ എണ്ണൂ", "എന്റെ മെസേജുകൾ എണ്ണൂ",
                "സന്ദേശങ്ങൾ എണ്ണുക", "എനിക്ക് എത്ര സന്ദേശങ്ങൾ ഉണ്ട്", "എത്ര സന്ദേശങ്ങൾ",
                "സന്ദേശങ്ങളുടെ എണ്ണം പറയൂ", "എനിക്ക് എന്തെങ്കിലും സന്ദേശം വന്നിട്ടുണ്ടോ", "ആകെ എത്ര സന്ദേശങ്ങൾ ഉണ്ട്", "പുതിയ മെസേജ് എത്രയുണ്ട്",
                // Malayalam Task 26 aliases
                "പരിചരിക്കുന്നയാളുടെ എത്ര സന്ദേശങ്ങളുണ്ട്", "എന്റെ പരിചരിക്കുന്നയാളുടെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "പരിചരിക്കുന്നയാളുടെ എത്ര മെസേജുകളുണ്ട്", "എന്റെ പരിചരിക്കുന്നയാളുടെ എത്ര മെസേജുകളുണ്ട്",
                "പരിചരിക്കുന്നയാളുടെ സന്ദേശങ്ങൾ എണ്ണൂ", "എന്റെ പരിചരിക്കുന്നയാളുടെ സന്ദേശങ്ങൾ എണ്ണൂ",
                "പരിചാരകന്റെ എത്ര സന്ദേശങ്ങളുണ്ട്", "എന്റെ പരിചാരകന്റെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "പരിചാരകന്റെ എത്ര മെസേജുകളുണ്ട്", "എന്റെ പരിചാരകന്റെ എത്ര മെസേജുകളുണ്ട്",
                "പരിചാരകന്റെ സന്ദേശങ്ങൾ എണ്ണൂ", "എന്റെ പരിചാരകന്റെ സന്ദേശങ്ങൾ എണ്ണൂ",
                "സഹായിയുടെ എത്ര സന്ദേശങ്ങളുണ്ട്", "എന്റെ സഹായിയുടെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "സഹായിയുടെ എത്ര മെസേജുകളുണ്ട്", "എന്റെ സഹായിയുടെ എത്ര മെസേജുകളുണ്ട്",
                "സഹായിയുടെ സന്ദേശങ്ങൾ എണ്ണൂ", "എന്റെ സഹായിയുടെ സന്ദേശങ്ങൾ എണ്ണൂ",
                "കെയർഗിവറിന്റെ എത്ര സന്ദേശങ്ങളുണ്ട്", "എന്റെ കെയർഗിവറിന്റെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "കെയർഗിവറിന്റെ എത്ര മെസേജുകളുണ്ട്", "എന്റെ കെയർഗിവറിന്റെ എത്ര മെസേജുകളുണ്ട്",
                "കെയർഗിവറിന്റെ സന്ദേശങ്ങൾ എണ്ണൂ", "എന്റെ കെയർഗിവറിന്റെ സന്ദേശങ്ങൾ എണ്ണൂ",
                "കെയർടേക്കറിന്റെ എത്ര സന്ദേശങ്ങളുണ്ട്", "എന്റെ കെയർടേക്കറിന്റെ എത്ര സന്ദേശങ്ങളുണ്ട്",
                "കെയർടേക്കറിന്റെ എത്ര മെസേജുകളുണ്ട്", "എന്റെ കെയർടേക്കറിന്റെ എത്ര മെസേജുകളുണ്ട്",
                "കെയർടേക്കറിന്റെ സന്ദേശങ്ങൾ എണ്ണൂ", "എന്റെ കെയർടേക്കറിന്റെ സന്ദേശങ്ങൾ എണ്ണൂ",
                "പരിചരിക്കുന്നയാളുടെ എത്ര സന്ദേശങ്ങൾ", "പരിചാരകന്റെ എത്ര സന്ദേശങ്ങൾ",
                "സഹായിയുടെ എത്ര സന്ദേശങ്ങൾ", "കെയർഗിവറിന്റെ എത്ര സന്ദേശങ്ങൾ", "കെയർടേക്കറിന്റെ എത്ര സന്ദേശങ്ങൾ")) {
            return true;
        }

        boolean hasKannadaCount = s.contains("ಎಷ್ಟು") || s.contains("ಎಣಿಸಿ");
        boolean hasKannadaMsg = s.contains("ಸಂದೇಶ") || s.contains("ಮೆಸೇಜ್");
        if (hasKannadaCount && (hasKannadaMsg || containsKannadaCaregiverAny(s))) {
            return true;
        }

        boolean hasHindiCount = s.contains("कितने") || s.contains("गिनो") || s.contains("गिनती");
        boolean hasHindiMsg = s.contains("संदेश") || s.contains("मैसेज");
        if (hasHindiCount && (hasHindiMsg || containsHindiCaregiverAny(s))) {
            return true;
        }

        boolean hasMalayalamCount = s.contains("എത്ര") || s.contains("എണ്ണൂ") || s.contains("എണ്ണുക") || s.contains("എണ്ണം");
        boolean hasMalayalamMsg = s.contains("സന്ദേശം") || s.contains("സന്ദേശങ്ങൾ") || s.contains("മെസേജ്") || s.contains("മെസേജുകൾ") || s.contains("മെസ്സേജ്");
        if (hasMalayalamCount && (hasMalayalamMsg || containsMalayalamCaregiverAny(s))) {
            return true;
        }

        boolean hasCountAction = s.contains("how many") || words.contains("count") || s.contains("total messages");
        boolean hasMessageNoun = words.contains("messages") || words.contains("message");
        boolean hasEnglishCaregiver = containsEnglishCaregiverAny(s, words);
        if (hasCountAction && (hasMessageNoun || hasEnglishCaregiver)) {
            return true;
        }

        return false;
    }

    private static boolean matchesReadNotifications(String s, Set<String> words) {
        // Disallow generic single/isolated words from triggering READ_NOTIFICATIONS (must remain UNKNOWN)
        if (s.equals("read") || s.equals("notification") || s.equals("notifications") || s.equals("check") ||
                s.equals("new") || s.equals("latest") || s.equals("tell") || s.equals("recent") ||
                // Kannada
                s.equals("ಅಧಿಸೂಚನೆ") || s.equals("ಅಧಿಸೂಚನೆಗಳು") || s.equals("ಅಧಿಸೂಚನೆಗಳನ್ನು") ||
                s.equals("ನೋಟಿಫಿಕೇಶನ್") || s.equals("ನೋಟಿಫಿಕೇಶನ್ಗಳು") || s.equals("ನೋಟಿಫಿಕೇಶನ್ಗಳನ್ನು") ||
                s.equals("ಓದಿ") || s.equals("ಪರಿಶೀಲಿಸಿ") || s.equals("ಹೊಸದು") || s.equals("ಇತ್ತೀಚಿನ") ||
                // Hindi
                s.equals("सूचना") || s.equals("सूचनाएँ") || s.equals("सूचनाएं") || s.equals("नोटिफिकेशन") ||
                s.equals("पढ़ो") || s.equals("चेक") || s.equals("नया") || s.equals("नवीनतम") ||
                // Malayalam
                s.equals("അറിയിപ്പ്") || s.equals("അറിയിപ്പുകൾ") ||
                s.equals("വായിക്കൂ") || s.equals("പരിശോധിക്കൂ") || s.equals("പുതിയ") || s.equals("ഏറ്റവും പുതിയത്") ||
                s.equals("നോട്ടിഫിക്കേഷൻ") || s.equals("നോട്ടിഫിക്കേഷനുകൾ")) {
            return false;
        }

        // Exclude voice messages/recording, emergency, calling, sending, message repeat, message counting
        if (words.contains("voice") || words.contains("audio") || words.contains("recorder") || words.contains("record") ||
                words.contains("call") || words.contains("emergency") || words.contains("sos") ||
                words.contains("repeat") || words.contains("again") ||
                words.contains("send") || words.contains("count") || words.contains("how")) {
            return false;
        }

        if (containsMatch(s, words,
                // English
                "read notifications", "read my notifications", "read the notifications",
                "check notifications", "check my notifications", "read notification", "check notification",
                "tell me my notifications", "what are my notifications", "read new notifications",
                "read latest notifications", "read recent notifications", "tell me the latest notification",
                "read my latest notification", "tell me notifications", "my notifications",
                "what notifications do i have", "any new notifications", "list notifications", "read alerts",
                // Kannada
                "ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ", "ನನ್ನ ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ", "ಅಧಿಸೂಚನೆಗಳನ್ನು ಪರಿಶೀಲಿಸಿ", "ನನ್ನ ಅಧಿಸೂಚನೆಗಳನ್ನು ಪರಿಶೀಲಿಸಿ",
                "ಅಧಿಸೂಚನೆ ಓದಿ", "ಅಧಿಸೂಚನೆ ಪರಿಶೀಲಿಸಿ", "ಹೊಸ ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ", "ಇತ್ತೀಚಿನ ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ",
                "ನನ್ನ ಹೊಸ ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ", "ಇತ್ತೀಚಿನ ಅಧಿಸೂಚನೆಯನ್ನು ಹೇಳಿ", "ನನ್ನ ಅಧಿಸೂಚನೆಗಳನ್ನು ಹೇಳಿ",
                "ಅಧಿಸೂಚನೆ ಹೇಳಿ", "ಇತ್ತೀಚಿನ ಅಧಿಸೂಚನೆ ಹೇಳಿ",
                "ಅಧಿಸೂಚನೆಗಳನ್ನು ಹೇಳಿ", "ಅಧಿಸೂಚನೆಗಳನ್ನು ಕೇಳಿಸಿ", "ಅಧಿಸೂಚನೆಗಳನ್ನು ತೋರಿಸಿ", "ಅಧಿಸೂಚನೆ ತೋರಿಸಿ",
                "ನೋಟಿಫಿಕೇಶನ್ಗಳನ್ನು ಓದಿ", "ನೋಟಿಫಿಕೇಶನ್ಗಳನ್ನು ಹೇಳಿ", "ನೋಟಿಫಿಕೇಶನ್ಗಳನ್ನು ಪರಿಶೀಲಿಸಿ", "ನೋಟಿಫಿಕೇಶನ್ಗಳನ್ನು ಕೇಳಿಸಿ",
                "ನೋಟಿಫಿಕೇಶನ್ ಓದಿ", "ನೋಟಿಫಿಕೇಶನ್ ಹೇಳಿ", "ನೋಟಿಫಿಕೇಶನ್ ಪರಿಶೀಲಿಸಿ", "ನೋಟಿಫಿಕೇಶನ್ ಕೇಳಿಸಿ",
                "ನನ್ನ ನೋಟಿಫಿಕೇಶನ್ಗಳನ್ನು ಓದಿ", "ನನ್ನ ನೋಟಿಫಿಕೇಶನ್ಗಳನ್ನು ಹೇಳಿ", "ನನ್ನ ನೋಟಿಫಿಕೇಶನ್ ಓದಿ",
                "ಯಾವುದಾದರೂ ಅಧಿಸೂಚನೆ ಬಂದಿದೆಯೇ", "ಹೊಸ ನೋಟಿಫಿಕೇಶನ್ ಓದಿ", "ಇತ್ತೀಚಿನ ಅಧಿಸೂಚನೆ ಏನು", "ನೋಟಿಫಿಕೇಶನ್ ಚೆಕ್ ಮಾಡಿ", "ಎಚ್ಚರಿಕೆಗಳನ್ನು ಓದಿ",
                // Hindi
                "सूचनाएँ पढ़ो", "सूचनाएं पढ़ो", "मेरी सूचनाएँ पढ़ो", "नोटिफिकेशन पढ़ो", "मेरे नोटिफिकेशन पढ़ो",
                "सूचनाएँ चेक करो", "नोटिफिकेशन चेक करो", "मेरी सूचनाएँ चेक करो",
                "नए नोटिफिकेशन पढ़ो", "नए नोटिफिकेशन बताओ", "नवीनतम नोटिफिकेशन पढ़ो",
                "मेरी नवीनतम सूचना पढ़ो", "मुझे मेरी सूचनाएँ बताओ", "मेरी सूचना बताओ",
                "सूचना पढ़ो", "नोटिफिकेशन बताओ", "मेरी नोटिफिकेशन पढ़ो", "नोटिफिकेशन सुनाओ", "अलर्ट्स पढ़कर सुनाओ", "नई नोटिफिकेशन बताओ",
                // Malayalam
                "അറിയിപ്പുകൾ വായിക്കൂ", "എന്റെ അറിയിപ്പുകൾ വായിക്കൂ", "അറിയിപ്പുകൾ പരിശോധിക്കൂ", "എന്റെ അറിയിപ്പുകൾ പരിശോധിക്കൂ",
                "നോട്ടിഫിക്കേഷൻ വായിക്കൂ", "എന്റെ നോട്ടിഫിക്കേഷനുകൾ വായിക്കൂ", "പുതിയ അറിയിപ്പുകൾ വായിക്കൂ",
                "പുതിയ നോട്ടിഫിക്കേഷനുകൾ വായിക്കൂ", "ഏറ്റവും പുതിയ അറിയിപ്പ് വായിക്കൂ",
                "എന്റെ പുതിയ അറിയിപ്പുകൾ പറയൂ", "എന്റെ അറിയിപ്പുകൾ പറയൂ",
                "അറിയിപ്പ് വായിക്കൂ", "അറിയിപ്പുകൾ പറയൂ", "നോട്ടിഫിക്കേഷൻ പരിശോധിക്കൂ",
                "എന്റെ അറിയിപ്പുകൾ കേൾപ്പിക്കൂ", "നോട്ടിഫിക്കേഷൻ ചെക്ക് ചെയ്യൂ", "പുതിയ അലർട്ടുകൾ വായിക്കുക", "അറിയിപ്പുകൾ കേൾപ്പിക്കുക")) {
            return true;
        }

        boolean hasKannadaNotifNoun = s.contains("ಅಧಿಸೂಚನೆ") || s.contains("ನೋಟಿಫಿಕೇಶನ್");
        boolean hasKannadaNotifVerb = s.contains("ಓದಿ") || s.contains("ಹೇಳಿ") || s.contains("ಪರಿಶೀಲಿಸಿ") ||
                s.contains("ಕೇಳಿಸಿ") || s.contains("ತೋರಿಸಿ") || s.contains("ಓದು") || s.contains("ಹೇಳು");
        if (hasKannadaNotifNoun && hasKannadaNotifVerb) {
            return true;
        }

        boolean hasHindiNotifNoun = s.contains("सूचना") || s.contains("नोटिफिकेशन") || s.contains("अलर्ट");
        boolean hasHindiNotifVerb = s.contains("पढ़ो") || s.contains("सुनाओ") || s.contains("बताओ") || s.contains("चेक");
        if (hasHindiNotifNoun && hasHindiNotifVerb) {
            return true;
        }

        boolean hasMalayalamNotifNoun = s.contains("അറിയിപ്പ്") || s.contains("നോട്ടിഫിക്കേഷൻ") || s.contains("അലർട്ട്");
        boolean hasMalayalamNotifVerb = s.contains("വായിക്കൂ") || s.contains("വായിക്കുക") || s.contains("പറയൂ") || s.contains("കേൾപ്പിക്കൂ") || s.contains("പരിശോധിക്കൂ");
        if (hasMalayalamNotifNoun && hasMalayalamNotifVerb) {
            return true;
        }

        boolean hasReadOrCheck = words.contains("read") || words.contains("check") || words.contains("tell") || words.contains("list");
        boolean hasNotifNoun = words.contains("notifications") || words.contains("notification") || words.contains("alerts") || words.contains("alert");
        if (hasReadOrCheck && hasNotifNoun) {
            return true;
        }

        return false;
    }

    private static boolean matchesReadMessages(String s, Set<String> words) {
        // Disallow generic single words alone from triggering READ_MESSAGES (must remain UNKNOWN)
        if (s.equals("read") || s.equals("messages") || s.equals("message") || s.equals("check") ||
                s.equals("caregiver") || s.equals("carer") || s.equals("caretaker") ||
                s.equals("care worker") || s.equals("helper") ||
                s.equals("repeat") || s.equals("again") || s.equals("say") || s.equals("that") ||
                s.equals("notification") || s.equals("notifications") || s.equals("tell") ||
                // Kannada
                s.equals("ಓದಿ") || s.equals("ಸಂದೇಶ") || s.equals("ಸಂದೇಶಗಳು") || s.equals("ಮೆಸೇಜ್") ||
                s.equals("ಮೆಸೇಜುಗಳನ್ನು") || s.equals("ಪರಿಶೀಲಿಸಿ") || s.equals("ಕೇರ್ಗಿವರ್") || s.equals("ಕೇರ್‌ಗಿವರ್") ||
                s.equals("ಆರೈಕೆದಾರ") || s.equals("ಆರೈಕೆದಾರರು") || s.equals("ಸಹಾಯಕ") || s.equals("ಸಹಾಯಕರು") ||
                s.equals("ಪಾಲಕ") || s.equals("ಪಾಲಕರು") || s.equals("ಹೇಳಿ") || s.equals("ಕೇಳಿಸಿ") ||
                s.equals("ಅಧಿಸೂಚನೆ") || s.equals("ಅಧಿಸೂಚನೆಗಳು") || s.equals("ಅಧಿಸೂಚನೆಗಳನ್ನು") ||
                s.equals("ನೋಟಿಫಿಕೇಶನ್") || s.equals("ನೋಟಿಫಿಕೇಶನ್ಗಳು") || s.equals("ನೋಟಿಫಿಕೇಶನ್ಗಳನ್ನು") ||
                // Hindi
                s.equals("पढ़ो") || s.equals("संदेश") || s.equals("मैसेज") || s.equals("चेक") || s.equals("केयरगिवर") ||
                s.equals("सूचना") || s.equals("सूचनाएँ") || s.equals("नोटिफिकेशन") ||
                // Malayalam
                s.equals("വായിക്കൂ") || s.equals("വായിക്കുക") || s.equals("സന്ദേശം") || s.equals("സന്ദേശങ്ങൾ") ||
                s.equals("മെസേജ്") || s.equals("മെസ്സേജ്") || s.equals("പരിശോധിക്കൂ") || s.equals("പരിശോധിക്കുക") ||
                s.equals("കെയർഗിവർ") || s.equals("കെയർടേക്കർ") || s.equals("സഹായി") || s.equals("പരിചാരകൻ") || s.equals("പരിചരിക്കുന്നയാൾ") ||
                s.equals("അറിയിപ്പ്") || s.equals("അറിയിപ്പുകൾ") || s.equals("നോട്ടിഫിക്കേഷൻ")) {
            return false;
        }

        // Direct voice message play phrases take precedence before exclusions
        if (containsMatch(s, words,
                "play voice message", "play audio message", "play voice note",
                "listen to voice message", "listen to the voice message", "play caregiver voice message",
                "ಧ್ವನಿ ಸಂದೇಶ ಕೇಳಿಸಿ", "ಧ್ವನಿ ಸಂದೇಶ ಓದಿ", "ಆಡಿಯೋ ಸಂದೇಶ ಕೇಳಿಸಿ", "ಆಡಿಯೋ ಕೇಳಿಸಿ",
                "ಧ್ವನಿ ಸಂದೇಶ ಪ್ಲೇ ಮಾಡಿ", "ಆಡಿಯೋ ಸಂದೇಶ ಪ್ಲೇ ಮಾಡಿ", "ಕೇರ್ಗಿವರ್ ಧ್ವನಿ ಸಂದೇಶ ಕೇಳಿಸಿ",
                "ಕೇರ್‌ಗಿವರ್ ಧ್ವನಿ ಸಂದೇಶ ಕೇಳಿಸಿ",
                "वॉइस मैसेज सुनाओ", "ऑडियो मैसेज सुनाओ", "वॉइस मैसेज बजाओ",
                "വോയ്സ് മെസേജ് കേൾപ്പിക്കൂ", "ഓഡിയോ സന്ദേശം കേൾപ്പിക്കൂ")) {
            return true;
        }

        // Exclude voice messages/recording, counting, sending, repeating, and notification intents
        if (words.contains("voice") || words.contains("audio") || words.contains("recorder") || words.contains("record") ||
                words.contains("count") || words.contains("how") ||
                words.contains("repeat") || words.contains("again") ||
                words.contains("send") || words.contains("write") ||
                s.contains("ವೀണ്ടും") || s.contains("ഒരിക്കൽ കൂടി") || s.contains("ഒന്ന് കൂടി") || s.contains("ആവർത്തിക്കുക") ||
                s.contains("ಮತ್ತೆ") || s.contains("ಮತ್ತೊಮ್ಮೆ") || s.contains("ಇನ್ನೊಮ್ಮೆ") || s.contains("ಪುನರಾವರ್ತಿಸಿ") ||
                s.contains("ಎಷ್ಟು") || s.contains("ಎಣಿಸಿ") ||
                words.contains("notification") || words.contains("notifications") ||
                s.contains("ಅಧಿಸೂಚನೆ") || s.contains("ನೋಟಿಫಿಕೇಶನ್") ||
                words.contains("ಅಧಿಸೂಚನೆ") || words.contains("ಅಧಿಸೂಚನೆಗಳು") || words.contains("ಅಧಿಸೂಚನೆಗಳನ್ನು") ||
                words.contains("सूचना") || words.contains("सूचनाएँ") || words.contains("सूचनाएं") || words.contains("नोटिफिकेशन") ||
                words.contains("അറിയിപ്പ്") || words.contains("അറിയിപ്പുകൾ") || words.contains("നോട്ടിഫിക്കേഷൻ") ||
                words.contains("ಕಳುಹಿಸಿ") || words.contains("ಕಳಿಸಿ") || words.contains("भेजो") || words.contains("അയക്കൂ")) {
            return false;
        }

        if (containsMatch(s, words,
                // English
                "read messages", "read my messages", "read caregiver message", "read caregiver messages",
                "read the messages", "read message", "read the message", "read my message", "read new messages",
                "read new message", "read latest message", "read latest messages",
                "check messages", "check my messages", "check caregiver message", "check caregiver messages",
                "check the messages", "check message", "check the message", "check my message", "check new messages",
                "check new message", "check latest message", "check latest messages",
                // English Task 27 aliases
                "read my caregiver messages", "read my caregiver message", "read messages from my caregiver", "read message from my caregiver",
                "read the caregiver messages", "read the caregiver message",
                "read my carer messages", "read my carer message", "read messages from my carer", "read message from my carer",
                "read the carer messages", "read the carer message",
                "read my caretaker messages", "read my caretaker message", "read messages from my caretaker", "read message from my caretaker",
                "read the caretaker messages", "read the caretaker message",
                "read my care worker messages", "read my care worker message", "read messages from my care worker", "read message from my care worker",
                "read the care worker messages", "read the care worker message",
                "read my helper messages", "read my helper message", "read messages from my helper", "read message from my helper",
                "read the helper messages", "read the helper message",
                "tell me what my caregiver said", "tell me what my carer said", "tell me what my caretaker said",
                "tell me what my care worker said", "tell me what my helper said",
                "read the latest message from my caregiver", "read the latest message from my carer",
                "read the latest message from my caretaker", "read the latest message from my care worker",
                "read the latest message from my helper",
                "what did my caregiver say", "did my caregiver send a message", "read caregiver messages to me",
                // Kannada
                "ಸಂದೇಶಗಳನ್ನು ಓದಿ", "ಸಂದೇಶ ಓದಿ", "ನನ್ನ ಸಂದೇಶಗಳನ್ನು ಓದಿ", "ಕೇರ್ಗಿವರ್ ಸಂದೇಶವನ್ನು ಓದಿ",
                "ಕೇರ್ಗಿವರ್ ಸಂದೇಶಗಳನ್ನು ಓದಿ", "ಕೇರ್‌ಗಿವರ್ ಸಂದೇಶಗಳನ್ನು ಓದಿ", "ಕೇರ್ಗಿವರ್ ಸಂದೇಶ ಓದಿ", "ಕೇರ್‌ಗಿವರ್ ಸಂದೇಶ ಓದಿ",
                "ಸಂದೇಶಗಳನ್ನು ಪರಿಶೀಲಿಸಿ", "ನನ್ನ ಸಂದೇಶಗಳನ್ನು ಪರಿಶೀಲಿಸಿ", "ಸಂದೇಶ ಪರಿಶೀಲಿಸಿ",
                "ಮೆಸೇಜ್ ಓದಿ", "ಮೆಸೇಜ್ಗಳನ್ನು ಓದಿ", "ಮೆಸೇಜ್‌ಗಳನ್ನು ಓದಿ", "ನನ್ನ ಮೆಸೇಜ್ಗಳನ್ನು ಓದಿ", "ನನ್ನ ಮೆಸೇಜ್‌ಗಳನ್ನು ಓದಿ",
                "ಕೇರ್ಗಿವರ್ ಮೆಸೇಜ್ ಓದಿ", "ಕೇರ್‌ಗಿವರ್ ಮೆಸೇಜ್ ಓದಿ", "ಹೊಸ ಸಂದೇಶಗಳನ್ನು ಓದಿ", "ಹೊಸ ಸಂದೇಶ ಓದಿ",
                "ಸಂದೇಶಗಳನ್ನು ಹೇಳಿ", "ಸಂದೇಶ ಹೇಳಿ", "ಸಂದೇಶಗಳನ್ನು ಓದಿ ಹೇಳಿ", "ಸಂದೇಶ ಓದಿ ಹೇಳಿ",
                "ಸಂದೇಶಗಳನ್ನು ನನಗೆ ಓದಿ", "ನನಗೆ ಸಂದೇಶಗಳನ್ನು ಓದಿ", "ಸಂದೇಶಗಳನ್ನು ಕೇಳಿಸು", "ಸಂದೇಶಗಳನ್ನು ಕೇಳಿಸಿ",
                "ಸಂದೇಶ ಕೇಳಿಸಿ", "ಸಂದೇಶ ಕೇಳಿಸು", "ಸಂದೇಶಗಳನ್ನು ಓದು", "ಸಂದೇಶ ಓದು",
                "ಮೆಸೇಜ್ಗಳನ್ನು ಹೇಳಿ", "ಮೆಸೇಜ್ ಹೇಳಿ", "ಮೆಸೇಜ್ಗಳನ್ನು ಕೇಳಿಸಿ", "ಮೆಸೇಜ್ ಕೇಳಿಸಿ", "ಮೆಸೇಜ್ಗಳನ್ನು ಓದು", "ಮೆಸೇಜ್ ಓದು",
                "ಕೇರ್ಗಿವರ್ ಏನು ಹೇಳಿದ್ದಾರೆ", "ನನಗೆ ಬಂದ ಸಂದೇಶಗಳನ್ನು ಓದಿ", "ಮೆಸೇಜ್ ಓದಿ ಹೇಳಿ",
                // Kannada Task 24 aliases & Caregiver expansions
                "ಆರೈಕೆದಾರರ ಸಂದೇಶಗಳನ್ನು ಓದಿ", "ಆರೈಕೆದಾರನ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ಆರೈಕೆದಾರರ ಸಂದೇಶ ಓದಿ", "ಆರೈಕೆದಾರನ ಸಂದೇಶ ಓದಿ",
                "ಆರೈಕೆದಾರರ ಮೆಸೇಜ್ ಓದಿ", "ಆರೈಕೆದಾರನ ಮೆಸೇಜ್ ಓದಿ",
                "ಆರೈಕೆದಾರರ ಸಂದೇಶಗಳನ್ನು ಪರಿಶೀಲಿಸಿ", "ಆರೈಕೆದಾರನ ಸಂದೇಶಗಳನ್ನು ಪರಿಶೀಲಿಸಿ",
                "ಸಹಾಯಕನ ಸಂದೇಶಗಳನ್ನು ಓದಿ", "ಸಹಾಯಕರ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ಸಹಾಯಕನ ಸಂದೇಶ ಓದಿ", "ಸಹಾಯಕರ ಸಂದೇಶ ಓದಿ",
                "ಸಹಾಯಕನ ಮೆಸೇಜ್ ಓದಿ", "ಸಹಾಯಕರ ಮೆಸೇಜ್ ಓದಿ",
                "ಸಹಾಯಕನ ಸಂದೇಶಗಳನ್ನು ಪರಿಶೀಲಿಸಿ", "ಸಹಾಯಕರ ಸಂದೇಶಗಳನ್ನು ಪರಿಶೀಲಿಸಿ",
                "ಸಹಾಯಕರ ಸಂದೇಶಗಳನ್ನು ಹೇಳಿ", "ಸಹಾಯಕನ ಸಂದೇಶಗಳನ್ನು ಹೇಳಿ", "ಸಹಾಯಕರ ಸಂದೇಶ ಹೇಳಿ", "ಸಹಾಯಕನ ಸಂದೇಶ ಹೇಳಿ",
                "ಸಹಾಯಕರ ಸಂದೇಶಗಳನ್ನು ಕೇಳಿಸಿ", "ಸಹಾಯಕನ ಸಂದೇಶಗಳನ್ನು ಕೇಳಿಸಿ", "ಸಹಾಯಕರ ಸಂದೇಶ ಕೇಳಿಸಿ", "ಸಹಾಯಕನ ಸಂದೇಶ ಕೇಳಿಸಿ",
                "ನನ್ನ ಸಹಾಯಕರ ಸಂದೇಶಗಳನ್ನು ಓದಿ", "ನನ್ನ ಸಹಾಯಕನ ಸಂದೇಶ ಓದಿ",
                "ಸಹಾಯಕರಿಂದ ಬಂದ ಸಂದೇಶ ಓದಿ", "ಸಹಾಯಕರಿಂದ ಬಂದ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ಪಾಲಕರ ಸಂದೇಶಗಳನ್ನು ಓದಿ", "ಪಾಲಕನ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ಪಾಲಕರ ಸಂದೇಶ ಓದಿ", "ಪಾಲಕನ ಸಂದೇಶ ಓದಿ",
                "ಪಾಲಕರ ಮೆಸೇಜ್ ಓದಿ", "ಪಾಲಕನ ಮೆಸೇಜ್ ಓದಿ",
                "ಪಾಲಕರ ಸಂದೇಶಗಳನ್ನು ಪರಿಶೀಲಿಸಿ", "ಪಾಲಕನ ಸಂದೇಶಗಳನ್ನು ಪರಿಶೀಲಿಸಿ",
                "ಪಾಲಕರ ಸಂದೇಶಗಳನ್ನು ಹೇಳಿ", "ಪಾಲಕನ ಸಂದೇಶಗಳನ್ನು ಹೇಳಿ", "ಪಾಲಕರ ಸಂದೇಶ ಹೇಳಿ", "ಪಾಲಕನ ಸಂದೇಶ ಹೇಳಿ",
                "ಪಾಲಕರ ಸಂದೇಶಗಳನ್ನು ಕೇಳಿಸಿ", "ಪಾಲಕನ ಸಂದೇಶಗಳನ್ನು ಕೇಳಿಸಿ", "ಪಾಲಕರ ಸಂದೇಶ ಕೇಳಿಸಿ", "ಪಾಲಕನ ಸಂದೇಶ ಕೇಳಿಸಿ",
                "ನನ್ನ ಪಾಲಕರ ಸಂದೇಶಗಳನ್ನು ಓದಿ", "ನನ್ನ ಪಾಲಕರ ಸಂದೇಶ ಓದಿ", "ನನ್ನ ಪಾಲಕನ ಸಂದೇಶ ಓದಿ",
                "ಪಾಲಕರಿಂದ ಬಂದ ಸಂದೇಶ ಓದಿ", "ಪಾಲಕರಿಂದ ಬಂದ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ಕೇರ್ಗಿವರ್ನ ಸಂದೇಶಗಳನ್ನು ಓದಿ", "ಕೇರ್ಗಿವರ್ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ಕೇರ್ಗಿವರ್ನ ಸಂದೇಶ ಓದಿ", "ಕೇರ್ಗಿವರ್ ಸಂದೇಶ ಓದಿ",
                "ಕೇರ್ಗಿವರ್ನ ಮೆಸೇಜ್ ಓದಿ", "ಕೇರ್ಗಿವರ್ ಮೆಸೇಜ್ ಓದಿ",
                "ಕೇರ್ಗಿವರ್ನ ಸಂದೇಶಗಳನ್ನು ಪರಿಶೀಲಿಸಿ",
                "ಕೇರ್ಟೇಕರ್ನ ಸಂದೇಶಗಳನ್ನು ಓದಿ", "ಕೇರ್ಟೇಕರ್ ಸಂದೇಶಗಳನ್ನು ಓದಿ",
                "ಕೇರ್ಟೇಕರ್ನ ಸಂದೇಶ ಓದಿ", "ಕೇರ್ಟೇಕರ್ ಸಂದೇಶ ಓದಿ",
                "ಕೇರ್ಟೇಕರ್ನ ಮೆಸೇಜ್ ಓದಿ", "ಕೇರ್ಟೇಕರ್ ಮೆಸೇಜ್ ಓದಿ",
                "ಕೇರ್ಟೇಕರ್ನ ಸಂದೇಶಗಳನ್ನು ಪರಿಶೀಲಿಸಿ",
                // Hindi
                "संदेश पढ़ो", "मेरे संदेश पढ़ो", "केयरगिवर का संदेश पढ़ो", "केयरगिवर के संदेश पढ़ो",
                "संदेश चेक करो", "मेरे संदेश चेक करो", "मैसेज पढ़ो", "मैसेज चेक करो", "मेरे मैसेज पढ़ो",
                "सारे संदेश पढ़ो", "संदेश पढ़कर सुनाओ", "मैसेज पढ़कर सुनाओ", "नया मैसेज पढ़ो", "नए संदेश पढ़ो",
                "संदेश देखें", "केयरगिवर का मैसेज सुनाओ", "सहायक का संदेश सुनाओ", "केयरगिवर ने क्या कहा", "मुझे संदेश पढ़कर बताओ",
                // Hindi Task 25 aliases
                "देखभाल करने वाले के संदेश पढ़ो", "मेरे देखभाल करने वाले के संदेश पढ़ो",
                "देखभाल करने वाले के संदेश पढ़कर सुनाओ", "मेरे देखभाल करने वाले के संदेश पढ़कर सुनाओ",
                "देखभाल करने वाले के संदेश सुनाओ", "मेरे देखभाल करने वाले के संदेश सुनाओ",
                "देखभाल करने वाले के मैसेज पढ़ो", "मेरे देखभाल करने वाले के मैसेज पढ़ो",
                "देखभाल करने वाले के मैसेज पढ़कर सुनाओ", "मेरे देखभाल करने वाले के मैसेज पढ़कर सुनाओ",
                "देखभाल करने वाले के मैसेज सुनाओ", "मेरे देखभाल करने वाले के मैसेज सुनाओ",
                "देखभाल करने वाले का संदेश पढ़ो", "मेरे देखभाल करने वाले का संदेश पढ़ो",
                "सहायक के संदेश पढ़ो", "मेरे सहायक के संदेश पढ़ो",
                "सहायक के संदेश पढ़कर सुनाओ", "मेरे सहायक के संदेश पढ़कर सुनाओ",
                "सहायक के संदेश सुनाओ", "मेरे सहायक के संदेश सुनाओ",
                "सहायक का संदेश पढ़ो", "मेरे सहायक का संदेश पढ़ो",
                "सहायक के मैसेज पढ़ो", "मेरे सहायक के मैसेज पढ़ो",
                "सहायक के मैसेज पढ़कर सुनाओ", "मेरे सहायक के मैसेज पढ़कर सुनाओ",
                "सहायक के मैसेज सुनाओ", "मेरे सहायक के मैसेज सुनाओ",
                "केयरगिवर के संदेश पढ़ो", "मेरे केयरगिवर के संदेश पढ़ो",
                "केयरगिवर के संदेश पढ़कर सुनाओ", "मेरे केयरगिवर के संदेश पढ़कर सुनाओ",
                "केयरगिवर के संदेश सुनाओ", "मेरे केयरगिवर के संदेश सुनाओ",
                "केयरगिवर के मैसेज पढ़ो", "मेरे केयरगिवर के मैसेज पढ़ो",
                "केयरगिवर के मैसेज पढ़कर सुनाओ", "केयरगिवर के मैसेज सुनाओ",
                "केयरटेकर के संदेश पढ़ो", "मेरे केयरटेकर के संदेश पढ़ो",
                "केयरटेकर के संदेश पढ़कर सुनाओ", "मेरे केयरटेकर के संदेश पढ़कर सुनाओ",
                "केयरटेकर के संदेश सुनाओ", "मेरे केयरटेकर के संदेश सुनाओ",
                "केयरटेकर के मैसेज पढ़ो", "मेरे केयरटेकर के मैसेज पढ़ो",
                "केयरटेकर के मैसेज पढ़कर सुनाओ", "केयरटेकर के मैसेज सुनाओ",
                "संदेश पढ़कर सुनाओ", "मैसेज पढ़कर सुनाओ", "संदेश सुनाओ", "मैसेज सुनाओ",
                // Malayalam
                "സന്ദേശങ്ങൾ വായിക്കൂ", "സന്ദേശങ്ങൾ വായിക്കുക", "എന്റെ സന്ദേശങ്ങൾ വായിക്കൂ", "എന്റെ സന്ദേശങ്ങൾ വായിക്കുക",
                "കെയർഗിവറിന്റെ സന്ദേശം വായിക്കൂ", "കെയർഗിവറിന്റെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "കെയർഗിവറുടെ സന്ദേശം വായിക്കുക", "കെയർഗിവറുടെ സന്ദേശങ്ങൾ വായിക്കുക", "കെയർഗിവറുടെ സന്ദേശം വായിക്കൂ",
                "സന്ദേശങ്ങൾ പരിശോധിക്കൂ", "സന്ദേശങ്ങൾ പരിശോധിക്കുക", "എന്റെ സന്ദേശങ്ങൾ പരിശോധിക്കൂ", "എന്റെ സന്ദേശങ്ങൾ പരിശോധിക്കുക",
                "മെസേജുകൾ വായിക്കൂ", "മെസേജ് വായിക്കൂ", "എന്റെ മെസേജുകൾ വായിക്കൂ", "മെസേജുകൾ പരിശോധിക്കൂ",
                "സന്ദേശം വായിക്കുക", "സന്ദേശം വായിക്കൂ", "സന്ദേശം പരിശോധിക്കുക", "സന്ദേശം പരിശോധിക്കൂ",
                "പുതിയ സന്ദേശം വായിക്കുക", "പുതിയ സന്ദേശം വായിക്കൂ",
                "കെയർഗിവറുടെ മെസേജ് കേൾപ്പിക്കൂ", "സഹായിയുടെ സന്ദേശം വായിക്കൂ", "കെയർഗിവർ എന്താണ് പറഞ്ഞത്", "പുതിയ സന്ദേശം വായിച്ചു കേൾപ്പിക്കൂ", "എനിക്ക് വന്ന മെസേജ് വായിക്കൂ",
                // Malayalam Task 26 aliases
                "പരിചരിക്കുന്നയാളുടെ സന്ദേശങ്ങൾ വായിക്കൂ", "എന്റെ പരിചരിക്കുന്നയാളുടെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "പരിചരിക്കുന്നയാളുടെ സന്ദേശം വായിക്കൂ", "എന്റെ പരിചരിക്കുന്നയാളുടെ സന്ദേശം വായിക്കൂ",
                "പരിചരിക്കുന്നയാളുടെ സന്ദേശങ്ങൾ കേൾപ്പിക്കൂ", "എന്റെ പരിചരിക്കുന്നയാളുടെ സന്ദേശങ്ങൾ കേൾപ്പിക്കൂ",
                "പരിചരിക്കുന്നയാളുടെ സന്ദേശം കേൾപ്പിക്കൂ", "എന്റെ പരിചരിക്കുന്നയാളുടെ സന്ദേശം കേൾപ്പിക്കൂ",
                "പരിചാരകന്റെ സന്ദേശങ്ങൾ വായിക്കൂ", "എന്റെ പരിചാരകന്റെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "പരിചാരകന്റെ സന്ദേശം വായിക്കൂ", "എന്റെ പരിചാരകന്റെ സന്ദേശം വായിക്കൂ",
                "പരിചാരകന്റെ സന്ദേശങ്ങൾ കേൾപ്പിക്കൂ", "എന്റെ പരിചാരകന്റെ സന്ദേശങ്ങൾ കേൾപ്പിക്കൂ",
                "പരിചാരകന്റെ സന്ദേശം കേൾപ്പിക്കൂ", "എന്റെ പരിചാരകന്റെ സന്ദേശം കേൾപ്പിക്കൂ",
                "സഹായിയുടെ സന്ദേശങ്ങൾ വായിക്കൂ", "എന്റെ സഹായിയുടെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "സഹായിയുടെ സന്ദേശം വായിക്കൂ", "എന്റെ സഹായിയുടെ സന്ദേശം വായിക്കൂ",
                "സഹായിയുടെ സന്ദേശങ്ങൾ കേൾപ്പിക്കൂ", "എന്റെ സഹായിയുടെ സന്ദേശങ്ങൾ കേൾപ്പിക്കൂ",
                "സഹായിയുടെ സന്ദേശം കേൾപ്പിക്കൂ", "എന്റെ സഹായിയുടെ സന്ദേശം കേൾപ്പിക്കൂ",
                "കെയർഗിവറിന്റെ സന്ദേശങ്ങൾ വായിക്കൂ", "എന്റെ കെയർഗിവറിന്റെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "കെയർഗിവറിന്റെ സന്ദേശം വായിക്കൂ", "എന്റെ കെയർഗിവറിന്റെ സന്ദേശം വായിക്കൂ",
                "കെയർഗിവറിന്റെ സന്ദേശങ്ങൾ കേൾപ്പിക്കൂ", "എന്റെ കെയർഗിവറിന്റെ സന്ദേശങ്ങൾ കേൾപ്പിക്കൂ",
                "കെയർഗിവറിന്റെ സന്ദേശം കേൾപ്പിക്കൂ", "എന്റെ കെയർഗിവറിന്റെ സന്ദേശം കേൾപ്പിക്കൂ",
                "കെയർടേക്കറിന്റെ സന്ദേശങ്ങൾ വായിക്കൂ", "എന്റെ കെയർടേക്കറിന്റെ സന്ദേശങ്ങൾ വായിക്കൂ",
                "കെയർടേക്കറിന്റെ സന്ദേശം വായിക്കൂ", "എന്റെ കെയർടേക്കറിന്റെ സന്ദേശം വായിക്കൂ",
                "കെയർടേക്കറിന്റെ സന്ദേശങ്ങൾ കേൾപ്പിക്കൂ", "എന്റെ കെയർടേക്കറിന്റെ സന്ദേശങ്ങൾ കേൾപ്പിക്കൂ",
                "കെയർടേക്കറിന്റെ സന്ദേശം കേൾപ്പിക്കൂ", "എന്റെ കെയർടേക്കറിന്റെ സന്ദേശം കേൾപ്പിക്കൂ",
                "പരിചരിക്കുന്നയാളുടെ മെസേജുകൾ വായിക്കൂ", "പരിചാരകന്റെ മെസേജുകൾ വായിക്കൂ",
                "സഹായിയുടെ മെസേജുകൾ വായിക്കൂ", "കെയർഗിവറിന്റെ മെസേജുകൾ വായിക്കൂ", "കെയർടേക്കറിന്റെ മെസേജുകൾ വായിക്കൂ",
                "സന്ദേശങ്ങൾ കേൾപ്പിക്കൂ", "സന്ദേശം കേൾപ്പിക്കൂ",
                "പരിചരിക്കുന്നയാളുടെ സന്ദേശങ്ങൾ പരിശോധിക്കൂ", "പരിചാരകന്റെ സന്ദേശങ്ങൾ പരിശോധിക്കൂ",
                "സഹായിയുടെ സന്ദേശങ്ങൾ പരിശോധിക്കൂ", "കെയർഗിവറിന്റെ സന്ദേശങ്ങൾ പരിശോധിക്കൂ", "കെയർടേക്കറിന്റെ സന്ദേശങ്ങൾ പരിശോധിക്കൂ")) {
            return true;
        }

        boolean hasKannadaRead = s.contains("ಓದಿ") || s.contains("ಓದು") || s.contains("ಪರಿಶೀಲಿಸಿ") ||
                s.contains("ಹೇಳಿ") || s.contains("ಹೇಳು") || s.contains("ಕೇಳಿಸಿ") || s.contains("ಕೇಳಿಸು") || s.contains("ಏನು ಹೇಳಿದ್ದಾರೆ");
        boolean hasKannadaMsg = s.contains("ಸಂದೇಶ") || s.contains("ಮೆಸೇಜ್");
        if (hasKannadaRead && (hasKannadaMsg || containsKannadaCaregiverAny(s))) {
            return true;
        }

        boolean hasHindiRead = s.contains("पढ़ो") || s.contains("सुनाओ") || s.contains("सुनाइए") || s.contains("चेक") || s.contains("बताओ") || s.contains("क्या कहा");
        boolean hasHindiMsg = s.contains("संदेश") || s.contains("मैसेज");
        if (hasHindiRead && (hasHindiMsg || containsHindiCaregiverAny(s))) {
            return true;
        }

        boolean hasMalayalamRead = s.contains("വായിക്കൂ") || s.contains("വായിക്കുക") ||
                s.contains("കേൾപ്പിക്കൂ") || s.contains("കേൾപ്പിക്കുക") ||
                s.contains("പരിശോധിക്കൂ") || s.contains("പരിശോധിക്കുക") || s.contains("പറയൂ") || s.contains("എന്താണ് പറഞ്ഞത്");
        boolean hasMalayalamMsg = s.contains("സന്ദേശം") || s.contains("സന്ദേശങ്ങൾ") ||
                s.contains("മെസേജ്") || s.contains("മെസേജുകൾ") || s.contains("മെസ്സേജ്");
        if (hasMalayalamRead && (hasMalayalamMsg || containsMalayalamCaregiverAny(s))) {
            return true;
        }

        boolean hasReadAction = words.contains("read") || words.contains("check") || words.contains("listen") || words.contains("hear") || (s.contains("what") && s.contains("said"));
        boolean hasMessageNoun = words.contains("messages") || words.contains("message");
        boolean hasEnglishCaregiver = containsEnglishCaregiverAny(s, words);
        if (hasReadAction && (hasMessageNoun || hasEnglishCaregiver)) {
            return true;
        }
        if (s.contains("what") && s.contains("said") && hasEnglishCaregiver) {
            return true;
        }

        return false;
    }

    private static boolean matchesMessages(String s, Set<String> words) {
        // Disallow generic single words alone from triggering OPEN_MESSAGES (must remain UNKNOWN)
        if (s.equals("messages") || s.equals("message") || s.equals("how many") || s.equals("count") ||
                s.equals("repeat") || s.equals("again") || s.equals("say") || s.equals("that") ||
                s.equals("notification") || s.equals("notifications") ||
                s.equals("ಸಂದೇಶಗಳು") || s.equals("ಸಂದೇಶ") || s.equals("ಮೆಸೇಜ್") ||
                s.equals("ಅಧಿಸೂಚನೆ") || s.equals("ಅಧಿಸೂಚನೆಗಳು") ||
                s.equals("संदेश") || s.equals("मैसेज") ||
                s.equals("सूचना") || s.equals("सूचनाएँ") || s.equals("नोटिफिकेशन") ||
                s.equals("സന്ദേശങ്ങൾ") || s.equals("സന്ദേശം") || s.equals("മെസേജ്") ||
                s.equals("അറിയിപ്പ്") || s.equals("അറിയിപ്പുകൾ") || s.equals("നോട്ടിഫിക്കേഷൻ")) {
            return false;
        }

        // Exclude voice messages/recording, counting, sending, reading, repeating, and notification actions
        if (words.contains("voice") || words.contains("audio") || words.contains("recorder") || words.contains("record") ||
                words.contains("send") || words.contains("read") || words.contains("check") ||
                words.contains("count") || words.contains("how") ||
                words.contains("repeat") || words.contains("again") ||
                words.contains("notification") || words.contains("notifications") ||
                words.contains("ಅಧಿಸೂಚನೆ") || words.contains("ಅಧಿಸೂಚನೆಗಳು") ||
                words.contains("सूचना") || words.contains("सूचनाएँ") || words.contains("नोटिफिकेशन") ||
                words.contains("അറിയിപ്പ്") || words.contains("അറിയിപ്പുകൾ")) {
            return false;
        }

        if (containsMatch(s, words,
                "open messages", "show messages", "go to messages", "open my messages", "show my messages",
                "view messages", "display messages", "chat", "open chat", "show chat",
                "chat with caregiver", "inbox", "sms", "open caregiver chat", "open full chat", "go to chat screen",
                "interactive chat", "open interactive chat",
                // Kannada
                "ಸಂವಾದವನ್ನು ತೆರೆಯಿರಿ", "ಸಂವಾದ ತೆರೆಯಿರಿ", "ಸಂವಾದ ತೋರಿಸಿ", "ಸಂವಾದ",
                "ಸಹಾಯಕರೊಂದಿಗೆ ಸಂವಾದ", "ಸಹಾಯಕರ ಚಾಟ್", "ಚಾಟ್ ತೆರೆಯಿರಿ", "ಚಾಟ್ ತೋರಿಸಿ", "ಚಾಟ್",
                "ಸಂದೇಶಗಳನ್ನು ತೆರೆಯಿರಿ", "ಸಂದೇಶ ತೆರೆಯಿರಿ", "ಸಂದೇಶಗಳನ್ನು ತೋರಿಸಿ", "ಸಂದೇಶ ತೋರಿಸಿ", "ಸಂದೇಶಗಳನ್ನು ತೋರಿಸು", "ಸಂದೇಶ ತೋರಿಸು",
                "ಮೆಸೇಜ್ಗಳನ್ನು ತೋರಿಸಿ", "ಮೆಸೇಜ್ ತೋರಿಸಿ", "ಮೆಸೇಜ್ಗಳನ್ನು ತೆರೆಯಿರಿ", "ಮೆಸೇಜ್ ತೆರೆಯಿರಿ",
                "ಕೇರ್ಗಿವರ್ ಚಾಟ್ ತೆರೆಯಿರಿ", "ಸಂದೇಶಗಳ ಪುಟಕ್ಕೆ ಹೋಗಿ", "ಪೂರ್ಣ ಚಾಟ್ ತೋರಿಸಿ", "ಚಾಟ್ ಸ್ಕ್ರೀನ್ ತೆರೆಯಿರಿ",
                // Hindi
                "संदेश खोलो", "संदेश दिखाओ", "मैसेज खोलो", "मैसेज दिखाओ", "चैट", "चैट खोलो",
                "केयरगिवर चैट खोलो", "संदेश पेज पर जाओ", "पूरी चैट दिखाओ", "बातचीत स्क्रीन खोलो", "चैट स्क्रीन पर जाओ",
                // Malayalam
                "സന്ദേശങ്ങൾ തുറക്കുക", "സന്ദേശം തുറക്കുക", "സന്ദേശങ്ങൾ കാണിക്കുക", "ചാറ്റ്",
                "കെയർഗിവർ ചാറ്റ് തുറക്കുക", "സന്ദേശങ്ങളുടെ പേജിലേക്ക് പോകൂ", "ഫുൾ ചാറ്റ് കാണിക്കൂ", "സംഭാഷണം കാണിക്കുക", "ചാറ്റ് സ്ക്രീൻ തുറക്കൂ")) {
            return true;
        }

        // Token combinations: action + messages/chat (e.g. open/show/view/go + messages)
        boolean hasAction = words.contains("open") || words.contains("show") || words.contains("go") ||
                words.contains("view") || words.contains("see") || words.contains("display") ||
                words.contains("ತೆರೆಯಿರಿ") || words.contains("ತೋರಿಸಿ") || words.contains("ತೋರಿಸು") ||
                s.contains("ತೆರೆಯಿರಿ") || s.contains("ತೋರಿಸಿ") || s.contains("ತೋರಿಸು") ||
                words.contains("खोलो") || words.contains("दिखाओ") ||
                words.contains("തുറക്കുക") || words.contains("കാണിക്കുക");
        boolean hasMessageNoun = words.contains("messages") || words.contains("message") || words.contains("chat") || words.contains("inbox") ||
                words.contains("ಸಂದೇಶಗಳು") || words.contains("ಸಂದೇಶಗಳನ್ನು") || words.contains("ಸಂದೇಶ") ||
                s.contains("ಸಂದೇಶ") || s.contains("ಮೆಸೇಜ್") ||
                words.contains("संदेश") || words.contains("मैसेज") ||
                words.contains("സന്ദേശങ്ങൾ") || words.contains("സന്ദേശം");
        return hasAction && hasMessageNoun;
    }

    private static boolean matchesSettings(String s, Set<String> words) {
        // Disallow generic false positives
        if (s.equals("open") || s.equals("show") || s.equals("go") || s.equals("show me")) {
            return false;
        }

        if (containsMatch(s, words,
                "open settings", "show settings", "go to settings", "settings", "setting",
                "sound settings", "volume settings", "volume", "sound", "notification settings", "preferences",
                "app settings", "settings page", "open app settings", "adjust settings", "open notification settings",
                // Kannada
                "ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಿರಿ", "ಸೆಟ್ಟಿಂಗ್ಸ್ಗೆ ಹೋಗಿ", "ಸೆಟ್ಟಿಂಗ್ಸ್ ಗೆ ಹೋಗಿ", "ಸೆಟ್ಟಿಂಗ್ಸ್ ತೋರಿಸಿ",
                "ಸೆಟ್ಟಿಂಗ್ ತೆರೆಯಿರಿ", "ಸೆಟ್ಟಿಂಗ್ಗೆ ಹೋಗಿ", "ಸೆಟ್ಟಿಂಗ್ ಗೆ ಹೋಗಿ", "ಸೆಟ್ಟಿಂಗ್ ತೋರಿಸಿ",
                "ಸೆಟ್ಟಿಂಗ್ಸ್ ಓಪನ್ ಮಾಡಿ", "ಸೆಟ್ಟಿಂಗ್ ಓಪನ್ ಮಾಡಿ", "ಸೆಟ್ಟಿಂಗ್ಗಳನ್ನು ತೆರೆಯಿರಿ",
                "ಸೆಟ್ಟಿಂಗ್ಸ್", "ಸೆಟ್ಟಿಂಗ್", "ಧ್ವನಿ ಸೆಟ್ಟಿಂಗ್", "ಧ್ವನಿ ಸೆಟ್ಟಿಂಗ್ಸ್", "ಧ್ವನಿ", "ವಾಲ್ಯೂಮ್", "ಶಬ್ದ",
                "ಅಧಿಸೂಚನೆ ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಿರಿ", "ಆ್ಯಪ್ ಸೆಟ್ಟಿಂಗ್ಸ್", "ಸೆಟ್ಟಿಂಗ್ಸ್ ಪುಟ", "ನನ್ನ ಸೆಟ್ಟಿಂಗ್ಸ್ ತೋರಿಸಿ", "ಅಧಿಸೂಚನೆ ಸೆಟ್ಟಿಂಗ್ಸ್",
                // Hindi
                "सेटिंग्स खोलो", "सेटिंग्स पर जाओ", "सेटिंग्स दिखाओ",
                "सेटिंग्स", "सेटिंग", "ध्वनि सेटिंग", "ध्वनि", "वॉल्यूम", "आवाज",
                "नोटिफिकेशन सेटिंग्स खोलो", "ऐप सेटिंग्स", "सेटिंग्स पेज", "सेटिंग्स ओपन करो", "मेरी सेटिंग्स दिखाओ", "सूचना सेटिंग्स",
                // Malayalam
                "സെറ്റിംഗ്സ് തുറക്കൂ", "സെറ്റിംഗ്സിലേക്ക് പോകൂ", "സെറ്റിംഗ്സ് കാണിക്കൂ",
                "സെറ്റിംഗ്സ് തുറക്കുക", "സെറ്റിംഗ്സ് കാണിക്കുക",
                "സെറ്റിംഗ്സ്", "സെറ്റിംഗ്", "ശബ്ദ ക്രമീകരണ", "ശബ്ദം", "വോളിയം", "ക്രമീകരണം", "ക്രമീകരണങ്ങൾ",
                "അറിയിപ്പ് സെറ്റിംഗ്സ് തുറക്കൂ", "ആപ്പ് സെറ്റിംഗ്സ്", "സെറ്റിംഗ്സ് പേജ്", "നോട്ടിഫിക്കേഷൻ സെറ്റിംഗ്സ്", "എന്റെ സെറ്റിംഗ്സ് കാണിക്കുക")) {
            return true;
        }

        return words.contains("settings") || words.contains("volume") || words.contains("preferences");
    }

    private static boolean matchesProfile(String s, Set<String> words) {
        // Disallow generic false positives and conversational states like "my profile is complete"
        if (s.equals("open") || s.equals("show") || s.equals("go") || s.equals("show me") ||
                s.equals("user") || s.equals("details") || s.equals("ವಿವರಗಳು") || s.equals("जानकारी") ||
                s.contains("complete") || s.contains("done") || s.contains("updated")) {
            return false;
        }

        if (containsMatch(s, words,
                "open profile", "show profile", "go to profile", "open my profile", "show my profile",
                "profile", "my profile", "account", "my account", "user details", "my details",
                "go to my profile", "profile page", "my profile page", "open profile page", "open my profile page", "user profile", "view profile",
                "take me to my profile", "take me to profile", "i want to see my profile", "see my profile", "see profile",
                // Kannada
                "ಪ್ರೊಫೈಲ್ ತೆರೆಯಿರಿ", "ಪ್ರೊಫೈಲ್ ತೋರಿಸಿ", "ಪ್ರೊಫೈಲ್ಗೆ ಹೋಗಿ", "ಪ್ರೊಫೈಲ್ ಗೆ ಹೋಗಿ",
                "ಪ್ರೊಫೈಲ್ಗೆ ಹೋಗು", "ಪ್ರೊಫೈಲ್ ಗೆ ಹೋಗು", "ಪ್ರೊಫೈಲ್ ಪುಟಕ್ಕೆ ಹೋಗು", "ಪ್ರೊಫೈಲ್ ಪುಟಕ್ಕೆ ಹೋಗಿ",
                "ಪ್ರೊಫೈಲ್ ಓಪನ್ ಮಾಡಿ", "ನನ್ನ ಪ್ರೊಫೈಲ್ ತೆರೆಯಿರಿ", "ನನ್ನ ಪ್ರೊಫೈಲ್ ತೋರಿಸಿ",
                "ನನ್ನ ಪ್ರೊಫೈಲ್ಗೆ ಹೋಗಿ", "ನನ್ನ ಪ್ರೊಫೈಲ್ ಗೆ ಹೋಗಿ", "ನನ್ನ ಪ್ರೊಫೈಲ್ಗೆ ಹೋಗು", "ನನ್ನ ಪ್ರೊಫೈಲ್ ಗೆ ಹೋಗು",
                "ನನ್ನ ಪ್ರೊಫೈಲ್ ಪುಟ", "ಪ್ರೊಫೈಲ್ ಪುಟ", "ಪ್ರೊಫೈಲ್ ಪೇಜ್",
                "ವೈಯಕ್ತಿಕ ಮಾಹಿತಿ ತೆರೆಯಿರಿ", "ವೈಯಕ್ತಿಕ ಮಾಹಿತಿ ತೋರಿಸಿ", "ವೈಯಕ್ತಿಕ ಮಾಹಿತಿ",
                "ಖಾತೆ", "ನನ್ನ ಪ್ರೊಫೈಲ್", "ನನ್ನ ಖಾತೆ", "ಖಾತೆ ತೆರೆಯಿರಿ", "ಪ್ರೊಫೈಲ್",
                "ಪ್ರೊಫೈಲ್ ಪುಟ ತೆರೆಯಿರಿ", "ನನ್ನ ವಿವರಗಳನ್ನು ತೋರಿಸಿ", "ಬಳಕೆದಾರರ ಪ್ರೊಫೈಲ್", "ಪ್ರೊಫೈಲ್ ನೋಡಿ",
                // Hindi
                "प्रोफाइल खोलो", "प्रोफाइल दिखाओ", "प्रोफाइल पर जाओ",
                "प्रोफ़ाइल खोलो", "प्रोफ़ाइल दिखाओ", "प्रोफ़ाइल पर जाओ",
                "प्रोफ़ाइल", "प्रोफाइल", "खाता", "मेरी प्रोफ़ाइल", "मेरी प्रोफाइल", "मेरी जानकारी",
                "प्रोफ़ाइल पेज खोलो", "उपयोगकर्ता प्रोफ़ाइल", "मेरा खाता दिखाओ", "प्रोफ़ाइल देखें",
                // Malayalam
                "പ്രൊഫൈൽ തുറക്കൂ", "പ്രൊഫൈൽ കാണിക്കൂ", "പ്രൊഫൈലിലേക്ക് പോകൂ",
                "പ്രൊഫൈൽ തുറക്കുക", "പ്രൊഫൈൽ കാണിക്കുക",
                "പ്രൊഫൈൽ", "അക്കൗണ്ട്", "എന്റെ പ്രൊഫൈൽ",
                "പ്രൊഫൈൽ പേജ് തുറക്കൂ", "യൂസർ പ്രൊഫൈൽ", "എന്റെ വിവരങ്ങൾ കാണിക്കൂ", "പ്രൊഫൈൽ പേജിലേക്ക് പോകൂ")) {
            return true;
        }

        if (s.contains("ಪ್ರೊಫೈಲ್") || s.contains("profile page") || s.contains("my profile")) {
            return true;
        }

        boolean hasProfileAction = words.contains("open") || words.contains("show") || words.contains("go") || words.contains("view") || words.contains("take") || words.contains("see");
        boolean hasProfileNoun = words.contains("profile") || words.contains("account");
        if (hasProfileAction && hasProfileNoun) {
            return true;
        }

        return s.equals("profile") || s.equals("my profile") || s.equals("account") || s.equals("my account");
    }

    private static boolean matchesHome(String s, Set<String> words) {
        // Disallow generic false-positive phrases: "homework", "home screen issue", "show me", "open", "go"
        if (s.equals("open") || s.equals("go") || s.equals("show me") || s.equals("show") || s.equals("me") ||
                s.equals("homework") || s.contains("home screen issue") || s.contains("screen issue") ||
                s.contains("homework") || s.contains("issue")) {
            return false;
        }

        if (s.contains("ಮುಖ್ಯ ಪುಟ") || s.contains("ಮುಖಪುಟ") || s.contains("home page")) {
            return true;
        }

        if (containsMatch(s, words,
                "open home", "go home", "show home", "home",
                "open home screen", "go to home", "take me home", "bring me back home", "bring me home", "take me back home",
                "main screen", "dashboard", "homepage", "start screen",
                "home page", "return to home", "go to main page", "open home page", "go to home page",
                // Kannada
                "ಮನೆ ತೆರೆಯಿರಿ", "ಮನೆಗೆ ಹೋಗಿ", "ಮನೆಗೆ ಹೋಗು", "ಹೋಮ್ ತೆರೆಯಿರಿ", "ಹೋಮ್ಗೆ ಹೋಗಿ", "ಹೋಮ್ ಗೆ ಹೋಗಿ",
                "ಮುಖಪುಟಕ್ಕೆ ಹೋಗಿ", "ಮುಖಪುಟಕ್ಕೆ ಹೋಗು", "ಮುಖ್ಯ ಪುಟಕ್ಕೆ ಹೋಗಿ", "ಮುಖ್ಯ ಪುಟಕ್ಕೆ ಹೋಗು",
                "ಮುಖಪುಟ ತೆರೆಯಿರಿ", "ಮುಖ್ಯ ಪುಟ ತೆರೆಯಿರಿ", "ಮುಖಪುಟ ತೋರಿಸಿ", "ಮುಖ್ಯ ಪುಟ ತೋರಿಸಿ",
                "ಮನೆ ಪುಟಕ್ಕೆ ಹೋಗಿ", "ಮನೆ ಪುಟಕ್ಕೆ ಹೋಗು", "ಮನೆ ಪುಟ", "ಮುಖ್ಯ ಪುಟ",
                "ಹೋಮ್", "ಮುಖಪುಟ", "ಮನೆ", "ಮನೆಗೆ ಹೋಗು", "ಮುಖ್ಯ", "ಡ್ಯಾಶ್‌ಬೋರ್ಡ್",
                "ಹೋಮ್ ಸ್ಕ್ರೀನ್", "ಹೋಮ್ ಪೇಜ್ ತೆರೆಯಿರಿ", "ಹೋಮ್ ಪೇಜ್", "ಹೋಮ್ ಪುಟ", "ಮನೆ ಪುಟ ತೋರಿಸಿ",
                // Hindi
                "होम खोलो", "घर जाओ", "होम पर जाओ", "होम दिखाओ",
                "घर चलो", "घर", "होम", "मुख्य पृष्ठ", "डैशबोर्ड", "मुख्य स्क्रीन", "होम स्क्रीन",
                "होम पेज पर जाओ", "वापस होम पर जाओ", "मेन पेज खोलो", "होम पर चलो",
                // Malayalam
                "ഹോം തുറക്കൂ", "വീട്ടിലേക്ക് പോകൂ", "ഹോം കാണിക്കൂ", "ഹോമിലേക്ക് പോകൂ",
                "ഹോം തുറക്കുക", "വീട്ടിലേക്ക് പോകുക", "ഹോം", "പ്രധാന പേജ്", "ഡാഷ്‌ബോർഡ്", "പ്രധാന സ്ക്രീൻ", "ഹോം സ്ക്രീൻ",
                "ഹോം പേജിലേക്ക് പോകുക", "മെയിൻ സ്ക്രീനിലേക്ക് പോകൂ", "ഹോമിലേക്ക് മടങ്ങുക", "ഹോം പേജ്")) {
            return true;
        }

        return words.contains("home") || words.contains("dashboard");
    }

    private static boolean containsMatch(String input, Set<String> words, String... candidates) {
        if (input == null) return false;
        for (String c : candidates) {
            if (c.contains(" ")) {
                if (input.contains(c)) return true;
            } else if (isAsciiWord(c)) {
                if (words.contains(c)) return true;
            } else {
                if (input.contains(c)) return true;
            }
        }
        return false;
    }

    private static boolean isAsciiWord(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                continue;
            }
            return false;
        }
        return true;
    }
}
