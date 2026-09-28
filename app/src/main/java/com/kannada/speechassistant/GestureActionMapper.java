package com.kannada.speechassistant;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Single source of truth for all gesture mappings in the application.
 * Centralizes the definition of supported MediaPipe gestures and ensures
 * real camera detection and virtual simulator buttons share identical actions,
 * localized phrases, and behavior.
 */
public class GestureActionMapper {

    // Stable Gesture Identifiers (DO NOT USE EMOJIS)
    public static final String ID_THUMB_UP = "THUMB_UP";
    public static final String ID_THUMB_DOWN = "THUMB_DOWN";
    public static final String ID_OPEN_PALM = "OPEN_PALM";
    public static final String ID_CLOSED_FIST = "CLOSED_FIST";
    public static final String ID_POINTING_UP = "POINTING_UP";
    public static final String ID_VICTORY = "VICTORY";
    public static final String ID_ILOVEYOU = "ILOVEYOU";
    public static final String ID_TWO_FINGERS = "TWO_FINGERS";
    public static final String ID_OK_SIGN = "OK_SIGN";
    public static final String ID_PINCH = "PINCH";
    public static final String ID_ONE_FINGER_UP = "ONE_FINGER_UP";
    public static final String ID_OPEN_HAND_WAVE = "OPEN_HAND_WAVE";
    public static final String ID_CROSSED_FINGERS = "CROSSED_FINGERS";
    public static final String ID_CALL_ME = "CALL_ME";

    // Stable Action Identifiers
    public static final String ACTION_YES = "ACTION_YES";
    public static final String ACTION_NO = "ACTION_NO";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String ACTION_EMERGENCY_SOS = "ACTION_EMERGENCY_SOS";
    public static final String ACTION_ATTENTION = "ACTION_ATTENTION";
    public static final String ACTION_THANK_YOU = "ACTION_THANK_YOU";
    public static final String ACTION_LOVE_CARE = "ACTION_LOVE_CARE";
    public static final String ACTION_TWO_MINUTES = "ACTION_TWO_MINUTES";
    public static final String ACTION_OKAY_THANKS = "ACTION_OKAY_THANKS";
    public static final String ACTION_A_LITTLE = "ACTION_A_LITTLE";
    public static final String ACTION_ONE_MINUTE = "ACTION_ONE_MINUTE";
    public static final String ACTION_HELLO = "ACTION_HELLO";
    public static final String ACTION_PLEASE_WAIT = "ACTION_PLEASE_WAIT";
    public static final String ACTION_CALL_ME = "ACTION_CALL_ME";

    private static final List<GestureAction> SUPPORTED_GESTURES = new ArrayList<>();
    private static final Map<String, GestureAction> BY_GESTURE_ID = new HashMap<>();
    private static final Map<String, GestureAction> BY_MEDIAPIPE_CATEGORY = new HashMap<>();

    static {
        // 1. Thumb_Up -> YES
        registerGesture(new GestureAction(
                ID_THUMB_UP,
                "Thumb_Up",
                "👍",
                "YES",
                "ಹೌದು",
                "हाँ",
                "അതെ",
                "ಹೌದು",
                "हाँ",
                "അതെ",
                "Yes",
                ACTION_YES,
                "Yes check-in shared!",
                false
        ));

        // 2. Thumb_Down -> NO
        registerGesture(new GestureAction(
                ID_THUMB_DOWN,
                "Thumb_Down",
                "👎",
                "NO",
                "ಇಲ್ಲ",
                "नहीं",
                "ഇല്ല",
                "ಇಲ್ಲ",
                "नहीं",
                "ഇല്ല",
                "No",
                ACTION_NO,
                "No check-in shared!",
                false
        ));

        // 3. Open_Palm -> STOP
        registerGesture(new GestureAction(
                ID_OPEN_PALM,
                "Open_Palm",
                "✋",
                "STOP",
                "ನಿಲ್ಲಿಸಿ",
                "रोकें",
                "നിർത്തുക",
                "ದಯವಿಟ್ಟು ನಿಲ್ಲಿಸಿ",
                "कृपया रोकें",
                "ദയവായി നിർത്തുക",
                "Please stop",
                ACTION_STOP,
                "Stop alert triggered!",
                false
        ));

        // 4. Closed_Fist -> EMERGENCY SOS & HELP
        registerGesture(new GestureAction(
                ID_CLOSED_FIST,
                "Closed_Fist",
                "✊",
                "EMERGENCY SOS",
                "ಸಹಾಯ",
                "आपातकालीन",
                "അടിയന്തരം",
                "ತುರ್ತು ಪರಿಸ್ಥಿತಿ! ಸಹಾಯ ಬೇಕು",
                "आपातकालीन मदद चाहिए!",
                "അടിയന്തര സഹായം വേണം!",
                "Emergency! Help needed!",
                ACTION_EMERGENCY_SOS,
                "Emergency SOS Alert!",
                true
        ));

        // 5. Pointing_Up -> ATTENTION
        registerGesture(new GestureAction(
                ID_POINTING_UP,
                "Pointing_Up",
                "👉",
                "ATTENTION",
                "ಗಮನಿಸಿ",
                "ध्यान दें",
                "ശ്രദ്ധിക്കുക",
                "ದಯವಿಟ್ಟು ನನ್ನ ಕಡೆ ಗಮನ ನೀಡಿ",
                "कृपया मेरी तरफ ध्यान दें",
                "ദയവായി എന്നെ ശ്രദ്ധിക്കുക",
                "Please pay attention to me",
                ACTION_ATTENTION,
                "Attention alert triggered!",
                false
        ));

        // 6. Victory -> THANK YOU
        registerGesture(new GestureAction(
                ID_VICTORY,
                "Victory",
                "🙏",
                "THANK YOU",
                "ಧನ್ಯವಾದಗಳು",
                "धन्यवाद",
                "നന്ദി",
                "ತುಂಬಾ ಧನ್ಯವಾದಗಳು",
                "बहुत बहुत धन्यवाद",
                "വളരെ നന്ദി",
                "Thank you very much",
                ACTION_THANK_YOU,
                "Thank you shared!",
                false
        ));

        // 7. ILoveYou -> LOVE & CARE
        registerGesture(new GestureAction(
                ID_ILOVEYOU,
                "ILoveYou",
                "🤟",
                "LOVE & CARE",
                "ಪ್ರೀತಿ",
                "स्नेह",
                "സ്നേഹം",
                "ನಾನು ನಿಮ್ಮನ್ನು ಪ್ರೀತಿಸುತ್ತೇನೆ",
                "मैं आपसे स्नेह करता हूँ",
                "ഞാൻ നിങ്ങളെ സ്നേഹിക്കുന്നു",
                "I love and appreciate you",
                ACTION_LOVE_CARE,
                "Love / Care shared!",
                false
        ));

        // --- 8 NEW SIMPLE GESTURES ---

        // 8. Two Fingers -> TWO MINUTES
        registerGesture(new GestureAction(
                ID_TWO_FINGERS,
                "Two_Fingers",
                "✌️",
                "TWO FINGERS",
                "ಎರಡು ನಿಮಿಷ",
                "दो मिनट",
                "രണ്ട് മിനിറ്റ്",
                "ಎರಡು ನಿಮಿಷ",
                "दो मिनट",
                "രണ്ട് മിനിറ്റ്",
                "Two minutes",
                ACTION_TWO_MINUTES,
                "Two minutes requested",
                false
        ));

        // 9. OK Sign -> OKAY THANK YOU
        registerGesture(new GestureAction(
                ID_OK_SIGN,
                "Ok_Sign",
                "👌",
                "OK SIGN",
                "ಸರಿ, ಧನ್ಯವಾದಗಳು",
                "ठीक है, धन्यवाद",
                "ശരി, നന്ദി",
                "ಸರಿ, ಧನ್ಯವಾದಗಳು",
                "ठीक है, धन्यवाद",
                "ശരി, നന്ദಿ",
                "Okay, thank you",
                ACTION_OKAY_THANKS,
                "Okay, thank you shared",
                false
        ));

        // 11. Pinch -> A LITTLE
        registerGesture(new GestureAction(
                ID_PINCH,
                "Pinch",
                "🤏",
                "PINCH",
                "ಸ್ವಲ್ಪ",
                "थोड़ा",
                "അല്പം",
                "ಸ್ವಲ್ಪ",
                "थोड़ा",
                "അല്പം",
                "A little",
                ACTION_A_LITTLE,
                "A little requested",
                false
        ));

        // 12. One Finger Up -> ONE MINUTE
        registerGesture(new GestureAction(
                ID_ONE_FINGER_UP,
                "One_Finger_Up",
                "☝️",
                "ONE FINGER UP",
                "ಒಂದು ನಿಮಿಷ",
                "एक मिनट",
                "ഒരു മിനിറ്റ്",
                "ಒಂದು ನಿಮಿಷ",
                "एक मिनट",
                "ഒരു മിനിറ്റ്",
                "One minute",
                ACTION_ONE_MINUTE,
                "One minute requested",
                false
        ));

        // 13. Open Hand / Wave -> HELLO
        registerGesture(new GestureAction(
                ID_OPEN_HAND_WAVE,
                "Open_Hand_Wave",
                "👋",
                "OPEN HAND / WAVE",
                "ಹಲೋ",
                "नमस्ते",
                "ഹലോ",
                "ಹಲೋ",
                "नमस्ते",
                "ഹലോ",
                "Hello",
                ACTION_HELLO,
                "Hello greeting shared",
                false
        ));

        // 14. Crossed Fingers -> PLEASE WAIT
        registerGesture(new GestureAction(
                ID_CROSSED_FINGERS,
                "Crossed_Fingers",
                "🤞",
                "CROSSED FINGERS",
                "ದಯವಿಟ್ಟು ಕಾಯಿರಿ",
                "कृपया प्रतीक्षा करें",
                "ദയവായി കാത്തിരിക്കൂ",
                "ದಯವಿಟ್ಟು ಕಾಯಿರಿ",
                "कृपया प्रतीक्षा करें",
                "ദയവായി കാത്തിരിക്കൂ",
                "Please wait",
                ACTION_PLEASE_WAIT,
                "Please wait requested",
                false
        ));

        // 15. Call Me Sign -> CALL ME
        registerGesture(new GestureAction(
                ID_CALL_ME,
                "Call_Me",
                "🤙",
                "CALL ME SIGN",
                "ನನ್ನನ್ನು ಕರೆ ಮಾಡಿ",
                "मुझे कॉल करें",
                "എന്നെ വിളിക്കൂ",
                "ನನ್ನನ್ನು ಕರೆ ಮಾಡಿ",
                "मुझे कॉल करें",
                "എന്നെ വിളിക്കೂ",
                "Call me",
                ACTION_CALL_ME,
                "Call me requested",
                false
        ));

        // Aliases for robustness
        addCategoryAlias("Thumb_Up", ID_THUMB_UP);
        addCategoryAlias("Thumbs Up", ID_THUMB_UP);
        addCategoryAlias("Thumbs_Up", ID_THUMB_UP);

        addCategoryAlias("Thumb_Down", ID_THUMB_DOWN);
        addCategoryAlias("Thumbs Down", ID_THUMB_DOWN);
        addCategoryAlias("Thumbs_Down", ID_THUMB_DOWN);

        addCategoryAlias("Open_Palm", ID_OPEN_PALM);
        addCategoryAlias("Open Palm", ID_OPEN_PALM);

        addCategoryAlias("Closed_Fist", ID_CLOSED_FIST);
        addCategoryAlias("Closed Fist", ID_CLOSED_FIST);
        addCategoryAlias("Fist", ID_CLOSED_FIST);

        addCategoryAlias("Pointing_Up", ID_POINTING_UP);
        addCategoryAlias("Pointing Up", ID_POINTING_UP);
        addCategoryAlias("Index Finger Up", ID_POINTING_UP);
        addCategoryAlias("Point Up", ID_POINTING_UP);

        addCategoryAlias("Victory", ID_VICTORY);
        addCategoryAlias("Victory Sign", ID_VICTORY);

        addCategoryAlias("ILoveYou", ID_ILOVEYOU);
        addCategoryAlias("I Love You", ID_ILOVEYOU);
        addCategoryAlias("I Love You (ASL)", ID_ILOVEYOU);
        addCategoryAlias("ILoveYou (ASL)", ID_ILOVEYOU);

        addCategoryAlias("Two_Fingers", ID_TWO_FINGERS);
        addCategoryAlias("Two Fingers", ID_TWO_FINGERS);

        addCategoryAlias("Ok_Sign", ID_OK_SIGN);
        addCategoryAlias("Ok Sign", ID_OK_SIGN);
        addCategoryAlias("OK", ID_OK_SIGN);

        addCategoryAlias("Pinch", ID_PINCH);
        addCategoryAlias("Pinch Gesture", ID_PINCH);

        addCategoryAlias("One_Finger_Up", ID_ONE_FINGER_UP);
        addCategoryAlias("One Finger Up", ID_ONE_FINGER_UP);

        addCategoryAlias("Open_Hand_Wave", ID_OPEN_HAND_WAVE);
        addCategoryAlias("Open Hand Wave", ID_OPEN_HAND_WAVE);
        addCategoryAlias("Wave", ID_OPEN_HAND_WAVE);

        addCategoryAlias("Crossed_Fingers", ID_CROSSED_FINGERS);
        addCategoryAlias("Crossed Fingers", ID_CROSSED_FINGERS);

        addCategoryAlias("Call_Me", ID_CALL_ME);
        addCategoryAlias("Call Me", ID_CALL_ME);
        addCategoryAlias("Call Me Sign", ID_CALL_ME);
    }

    private static void registerGesture(GestureAction action) {
        SUPPORTED_GESTURES.add(action);
        BY_GESTURE_ID.put(action.getGestureId().toUpperCase(Locale.ROOT), action);
        BY_MEDIAPIPE_CATEGORY.put(action.getMediaPipeCategory().toLowerCase(Locale.ROOT), action);
    }

    private static void addCategoryAlias(String categoryAlias, String gestureId) {
        GestureAction action = BY_GESTURE_ID.get(gestureId.toUpperCase(Locale.ROOT));
        if (action != null) {
            BY_MEDIAPIPE_CATEGORY.put(categoryAlias.toLowerCase(Locale.ROOT), action);
        }
    }

    /**
     * Gets immutable list of all supported gestures for UI initialization.
     */
    public static List<GestureAction> getSupportedGestures() {
        return Collections.unmodifiableList(SUPPORTED_GESTURES);
    }

    /**
     * Resolves GestureAction from a MediaPipe category name (e.g., "Thumb_Up", "Closed_Fist").
     */
    @Nullable
    public static GestureAction getGestureByMediaPipeName(@Nullable String mediaPipeCategory) {
        if (mediaPipeCategory == null || mediaPipeCategory.trim().isEmpty() || "None".equalsIgnoreCase(mediaPipeCategory.trim())) {
            return null;
        }
        String key = mediaPipeCategory.trim().toLowerCase(Locale.ROOT);
        return BY_MEDIAPIPE_CATEGORY.get(key);
    }

    /**
     * Resolves GestureAction by its constant gesture ID (e.g., ID_THUMB_UP, ID_CLOSED_FIST).
     */
    @Nullable
    public static GestureAction getGestureById(@Nullable String gestureId) {
        if (gestureId == null || gestureId.trim().isEmpty()) {
            return null;
        }
        return BY_GESTURE_ID.get(gestureId.trim().toUpperCase(Locale.ROOT));
    }
}
