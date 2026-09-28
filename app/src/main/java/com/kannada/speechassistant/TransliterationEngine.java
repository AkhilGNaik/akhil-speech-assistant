package com.kannada.speechassistant;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight phonetic transliteration engine for converting English-typed text
 * into Kannada, Malayalam, or Hindi Unicode script when offline.
 */
public class TransliterationEngine {

    // Kannada Maps
    private static final Map<String, String> KN_CONSONANTS = new HashMap<>();
    private static final Map<String, String> KN_VOWELS_IND = new HashMap<>();
    private static final Map<String, String> KN_VOWELS_DEP = new HashMap<>();

    // Malayalam Maps
    private static final Map<String, String> ML_CONSONANTS = new HashMap<>();
    private static final Map<String, String> ML_VOWELS_IND = new HashMap<>();
    private static final Map<String, String> ML_VOWELS_DEP = new HashMap<>();

    // Hindi Maps
    private static final Map<String, String> HI_CONSONANTS = new HashMap<>();
    private static final Map<String, String> HI_VOWELS_IND = new HashMap<>();
    private static final Map<String, String> HI_VOWELS_DEP = new HashMap<>();

    static {
        // --- KANNADA MAPPING ---
        KN_CONSONANTS.put("shh", "ಷ"); KN_CONSONANTS.put("sh", "ಶ"); KN_CONSONANTS.put("chh", "ಛ"); KN_CONSONANTS.put("ch", "ಚ");
        KN_CONSONANTS.put("kh", "ಖ"); KN_CONSONANTS.put("gh", "ಘ"); KN_CONSONANTS.put("jh", "ಝ"); KN_CONSONANTS.put("th", "ಥ");
        KN_CONSONANTS.put("dh", "ಧ"); KN_CONSONANTS.put("ph", "ಫ"); KN_CONSONANTS.put("bh", "ಭ"); KN_CONSONANTS.put("ny", "ಞ");
        KN_CONSONANTS.put("ng", "ಙ"); KN_CONSONANTS.put("Th", "ಠ"); KN_CONSONANTS.put("Dh", "ಢ");
        KN_CONSONANTS.put("k", "ಕ"); KN_CONSONANTS.put("g", "ಗ"); KN_CONSONANTS.put("j", "ಜ"); KN_CONSONANTS.put("t", "ತ");
        KN_CONSONANTS.put("d", "ದ"); KN_CONSONANTS.put("n", "ನ"); KN_CONSONANTS.put("p", "ಪ"); KN_CONSONANTS.put("b", "ಬ");
        KN_CONSONANTS.put("m", "ಮ"); KN_CONSONANTS.put("y", "ಯ"); KN_CONSONANTS.put("r", "ರ"); KN_CONSONANTS.put("l", "ಲ");
        KN_CONSONANTS.put("v", "ವ"); KN_CONSONANTS.put("w", "ವ"); KN_CONSONANTS.put("s", "ಸ"); KN_CONSONANTS.put("h", "ಹ");
        KN_CONSONANTS.put("T", "ಟ"); KN_CONSONANTS.put("D", "ಡ"); KN_CONSONANTS.put("N", "ಣ"); KN_CONSONANTS.put("L", "ಳ");

        KN_VOWELS_IND.put("aa", "ಆ"); KN_VOWELS_IND.put("ii", "ಈ"); KN_VOWELS_IND.put("uu", "ಊ"); KN_VOWELS_IND.put("ee", "ಏ");
        KN_VOWELS_IND.put("oo", "ಓ"); KN_VOWELS_IND.put("ai", "ಐ"); KN_VOWELS_IND.put("au", "ಔ"); KN_VOWELS_IND.put("A", "ಆ");
        KN_VOWELS_IND.put("I", "ಈ"); KN_VOWELS_IND.put("U", "ಊ"); KN_VOWELS_IND.put("E", "ಏ"); KN_VOWELS_IND.put("O", "ಓ");
        KN_VOWELS_IND.put("a", "ಅ"); KN_VOWELS_IND.put("i", "ಇ"); KN_VOWELS_IND.put("u", "ಉ"); KN_VOWELS_IND.put("e", "ಎ"); KN_VOWELS_IND.put("o", "ಒ");

        KN_VOWELS_DEP.put("aa", "ಾ"); KN_VOWELS_DEP.put("ii", "ೀ"); KN_VOWELS_DEP.put("uu", "ೂ"); KN_VOWELS_DEP.put("ee", "ೇ");
        KN_VOWELS_DEP.put("oo", "ೋ"); KN_VOWELS_DEP.put("ai", "ೈ"); KN_VOWELS_DEP.put("au", "ೌ"); KN_VOWELS_DEP.put("A", "ಾ");
        KN_VOWELS_DEP.put("I", "ೀ"); KN_VOWELS_DEP.put("U", "ೂ"); KN_VOWELS_DEP.put("E", "ೇ"); KN_VOWELS_DEP.put("O", "ೋ");
        KN_VOWELS_DEP.put("a", ""); KN_VOWELS_DEP.put("i", "ಿ"); KN_VOWELS_DEP.put("u", "ು"); KN_VOWELS_DEP.put("e", "ೆ"); KN_VOWELS_DEP.put("o", "ೊ");

        // --- MALAYALAM MAPPING ---
        ML_CONSONANTS.put("shh", "ഷ"); ML_CONSONANTS.put("sh", "ശ"); ML_CONSONANTS.put("chh", "ഛ"); ML_CONSONANTS.put("ch", "ച");
        ML_CONSONANTS.put("kh", "ഖ"); ML_CONSONANTS.put("gh", "ഘ"); ML_CONSONANTS.put("jh", "ഝ"); ML_CONSONANTS.put("th", "ഥ");
        ML_CONSONANTS.put("dh", "ധ"); ML_CONSONANTS.put("ph", "ഫ"); ML_CONSONANTS.put("bh", "ഭ"); ML_CONSONANTS.put("ny", "ഞ");
        ML_CONSONANTS.put("ng", "ങ"); ML_CONSONANTS.put("zh", "ഴ"); ML_CONSONANTS.put("Th", "ഠ"); ML_CONSONANTS.put("Dh", "ഢ");
        ML_CONSONANTS.put("k", "ക"); ML_CONSONANTS.put("g", "ഗ"); ML_CONSONANTS.put("j", "ജ"); ML_CONSONANTS.put("t", "ത");
        ML_CONSONANTS.put("d", "ദ"); ML_CONSONANTS.put("n", "ന"); ML_CONSONANTS.put("p", "പ"); ML_CONSONANTS.put("b", "ബ");
        ML_CONSONANTS.put("m", "മ"); ML_CONSONANTS.put("y", "യ"); ML_CONSONANTS.put("r", "ര"); ML_CONSONANTS.put("l", "ല");
        ML_CONSONANTS.put("v", "വ"); ML_CONSONANTS.put("w", "വ"); ML_CONSONANTS.put("s", "സ"); ML_CONSONANTS.put("h", "ഹ");
        ML_CONSONANTS.put("T", "ട"); ML_CONSONANTS.put("D", "ഡ"); ML_CONSONANTS.put("N", "ണ"); ML_CONSONANTS.put("L", "ള"); ML_CONSONANTS.put("R", "റ");

        ML_VOWELS_IND.put("aa", "ആ"); ML_VOWELS_IND.put("ii", "ഈ"); ML_VOWELS_IND.put("uu", "ഊ"); ML_VOWELS_IND.put("ee", "ഏ");
        ML_VOWELS_IND.put("oo", "ഓ"); ML_VOWELS_IND.put("ai", "ഐ"); ML_VOWELS_IND.put("au", "ഔ"); ML_VOWELS_IND.put("A", "ആ");
        ML_VOWELS_IND.put("I", "ഈ"); ML_VOWELS_IND.put("U", "ഊ"); ML_VOWELS_IND.put("E", "ഏ"); ML_VOWELS_IND.put("O", "ഓ");
        ML_VOWELS_IND.put("a", "അ"); ML_VOWELS_IND.put("i", "ഇ"); ML_VOWELS_IND.put("u", "ഉ"); ML_VOWELS_IND.put("e", "എ"); ML_VOWELS_IND.put("o", "ഒ");

        ML_VOWELS_DEP.put("aa", "ാ"); ML_VOWELS_DEP.put("ii", "ീ"); ML_VOWELS_DEP.put("uu", "ൂ"); ML_VOWELS_DEP.put("ee", "േ");
        ML_VOWELS_DEP.put("oo", "ോ"); ML_VOWELS_DEP.put("ai", "ൈ"); ML_VOWELS_DEP.put("au", "ൌ"); ML_VOWELS_DEP.put("A", "ാ");
        ML_VOWELS_DEP.put("I", "ീ"); ML_VOWELS_DEP.put("U", "ൂ"); ML_VOWELS_DEP.put("E", "േ"); ML_VOWELS_DEP.put("O", "ോ");
        ML_VOWELS_DEP.put("a", ""); ML_VOWELS_DEP.put("i", "ി"); ML_VOWELS_DEP.put("u", "ു"); ML_VOWELS_DEP.put("e", "െ"); ML_VOWELS_DEP.put("o", "ൊ");

        // --- HINDI MAPPING ---
        HI_CONSONANTS.put("shh", "ष"); HI_CONSONANTS.put("sh", "श"); HI_CONSONANTS.put("chh", "छ"); HI_CONSONANTS.put("ch", "च");
        HI_CONSONANTS.put("kh", "ख"); HI_CONSONANTS.put("gh", "घ"); HI_CONSONANTS.put("jh", "झ"); HI_CONSONANTS.put("th", "थ");
        HI_CONSONANTS.put("dh", "ध"); HI_CONSONANTS.put("ph", "फ"); HI_CONSONANTS.put("bh", "भ"); HI_CONSONANTS.put("ny", "ञ");
        HI_CONSONANTS.put("ng", "ङ"); HI_CONSONANTS.put("Th", "ठ"); HI_CONSONANTS.put("Dh", "ढ");
        HI_CONSONANTS.put("k", "क"); HI_CONSONANTS.put("g", "ग"); HI_CONSONANTS.put("j", "ज"); HI_CONSONANTS.put("t", "त");
        HI_CONSONANTS.put("d", "द"); HI_CONSONANTS.put("n", "न"); HI_CONSONANTS.put("p", "प"); HI_CONSONANTS.put("b", "ब");
        HI_CONSONANTS.put("m", "म"); HI_CONSONANTS.put("y", "य"); HI_CONSONANTS.put("r", "र"); HI_CONSONANTS.put("l", "ल");
        HI_CONSONANTS.put("v", "व"); HI_CONSONANTS.put("w", "व"); HI_CONSONANTS.put("s", "स"); HI_CONSONANTS.put("h", "ह");
        HI_CONSONANTS.put("T", "ट"); HI_CONSONANTS.put("D", "ड"); HI_CONSONANTS.put("N", "ण");

        HI_VOWELS_IND.put("aa", "आ"); HI_VOWELS_IND.put("ii", "ई"); HI_VOWELS_IND.put("uu", "ऊ"); HI_VOWELS_IND.put("ee", "ए");
        HI_VOWELS_IND.put("oo", "ओ"); HI_VOWELS_IND.put("ai", "ऐ"); HI_VOWELS_IND.put("au", "औ"); HI_VOWELS_IND.put("A", "आ");
        HI_VOWELS_IND.put("I", "ई"); HI_VOWELS_IND.put("U", "ऊ"); HI_VOWELS_IND.put("E", "ए"); HI_VOWELS_IND.put("O", "ओ");
        HI_VOWELS_IND.put("a", "अ"); HI_VOWELS_IND.put("i", "इ"); HI_VOWELS_IND.put("u", "उ"); HI_VOWELS_IND.put("e", "ए"); HI_VOWELS_IND.put("o", "ओ");

        HI_VOWELS_DEP.put("aa", "ा"); HI_VOWELS_DEP.put("ii", "ी"); HI_VOWELS_DEP.put("uu", "ू"); HI_VOWELS_DEP.put("ee", "े");
        HI_VOWELS_DEP.put("oo", "ो"); HI_VOWELS_DEP.put("ai", "ै"); HI_VOWELS_DEP.put("au", "ौ"); HI_VOWELS_DEP.put("A", "ा");
        HI_VOWELS_DEP.put("I", "ी"); HI_VOWELS_DEP.put("U", "ू"); HI_VOWELS_DEP.put("E", "े"); HI_VOWELS_DEP.put("O", "ो");
        HI_VOWELS_DEP.put("a", ""); HI_VOWELS_DEP.put("i", "ि"); HI_VOWELS_DEP.put("u", "ु"); HI_VOWELS_DEP.put("e", "े"); HI_VOWELS_DEP.put("o", "ो");
    }

    public static String transliterate(String text) {
        return transliterate(text, null);
    }

    public static String transliterate(String text, String langCode) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String code = LanguageManager.normalizeLanguageCode(langCode);
        Map<String, String> consonants;
        Map<String, String> vowelsInd;
        Map<String, String> vowelsDep;
        String virama;

        switch (code) {
            case LanguageManager.LANG_HINDI:
                consonants = HI_CONSONANTS;
                vowelsInd = HI_VOWELS_IND;
                vowelsDep = HI_VOWELS_DEP;
                virama = "्";
                break;
            case LanguageManager.LANG_MALAYALAM:
                consonants = ML_CONSONANTS;
                vowelsInd = ML_VOWELS_IND;
                vowelsDep = ML_VOWELS_DEP;
                virama = "്";
                break;
            case LanguageManager.LANG_KANNADA:
            default:
                consonants = KN_CONSONANTS;
                vowelsInd = KN_VOWELS_IND;
                vowelsDep = KN_VOWELS_DEP;
                virama = "್";
                break;
        }

        StringBuilder result = new StringBuilder();
        String[] words = text.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            result.append(transliterateWord(words[i], consonants, vowelsInd, vowelsDep, virama, code));
            if (i < words.length - 1) {
                result.append(" ");
            }
        }
        return result.toString();
    }

    private static String transliterateWord(String word, Map<String, String> consonants, Map<String, String> vowelsInd, Map<String, String> vowelsDep, String virama, String langCode) {
        if (word.isEmpty()) {
            return "";
        }

        StringBuilder parsed = new StringBuilder();
        int len = word.length();
        int idx = 0;
        boolean prevWasConsonant = false;
        String lastConsonantToken = null;

        while (idx < len) {
            String vowelToken = null;
            if (idx + 2 <= len) {
                String sub = word.substring(idx, idx + 2);
                if (vowelsInd.containsKey(sub)) {
                    vowelToken = sub;
                }
            }
            if (vowelToken == null && idx + 1 <= len) {
                String sub = word.substring(idx, idx + 1);
                if (vowelsInd.containsKey(sub)) {
                    vowelToken = sub;
                }
            }

            if (vowelToken != null) {
                if (prevWasConsonant) {
                    parsed.append(vowelsDep.get(vowelToken));
                } else {
                    parsed.append(vowelsInd.get(vowelToken));
                }
                prevWasConsonant = false;
                lastConsonantToken = null;
                idx += vowelToken.length();
                continue;
            }

            String consToken = null;
            if (idx + 3 <= len) {
                String sub = word.substring(idx, idx + 3);
                if (consonants.containsKey(sub)) {
                    consToken = sub;
                }
            }
            if (consToken == null && idx + 2 <= len) {
                String sub = word.substring(idx, idx + 2);
                if (consonants.containsKey(sub)) {
                    consToken = sub;
                }
            }
            if (consToken == null && idx + 1 <= len) {
                String sub = word.substring(idx, idx + 1);
                if (consonants.containsKey(sub)) {
                    consToken = sub;
                }
            }

            if (consToken != null) {
                if (prevWasConsonant) {
                    parsed.append(virama);
                }
                parsed.append(consonants.get(consToken));
                prevWasConsonant = true;
                lastConsonantToken = consToken;
                idx += consToken.length();
            } else {
                if (prevWasConsonant) {
                    parsed.append(virama);
                }
                parsed.append(word.charAt(idx));
                prevWasConsonant = false;
                lastConsonantToken = null;
                idx++;
            }
        }

        if (prevWasConsonant) {
            if (LanguageManager.LANG_MALAYALAM.equals(langCode) && lastConsonantToken != null) {
                // Check if last consonant can be converted to Malayalam Chillu character
                if ("l".equalsIgnoreCase(lastConsonantToken)) {
                    // Replace trailing "ല" with chillu "ൽ"
                    if (parsed.length() > 0 && parsed.charAt(parsed.length() - 1) == 'ല') {
                        parsed.setCharAt(parsed.length() - 1, 'ൽ');
                    } else {
                        parsed.append(virama);
                    }
                } else if ("n".equalsIgnoreCase(lastConsonantToken)) {
                    if (parsed.length() > 0 && parsed.charAt(parsed.length() - 1) == 'ന') {
                        parsed.setCharAt(parsed.length() - 1, 'ൻ');
                    } else {
                        parsed.append(virama);
                    }
                } else if ("r".equalsIgnoreCase(lastConsonantToken)) {
                    if (parsed.length() > 0 && parsed.charAt(parsed.length() - 1) == 'ര') {
                        parsed.setCharAt(parsed.length() - 1, 'ർ');
                    } else {
                        parsed.append(virama);
                    }
                } else {
                    parsed.append(virama);
                }
            } else {
                parsed.append(virama);
            }
        }

        return parsed.toString();
    }
}
