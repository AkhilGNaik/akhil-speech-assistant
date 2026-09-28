package com.kannada.speechassistant;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages accessible audio and haptic feedback for blind/visually impaired users:
 * Flow: Tap mic -> Vibration -> "You can speak" (in user language) -> Wait for TTS -> STT listening ->
 * User speaks -> STT result -> Vibration -> Automatic TTS reads recognized text.
 */
public class AccessibleMicFeedbackManager {

    private static final String TAG = "AccessibleMicFeedback";
    private static boolean isSessionActive = false;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Triggers a short, non-continuous haptic vibration feedback.
     */
    public static void triggerShortVibration(Context context) {
        if (context == null) return;
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(60);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error triggering short vibration", e);
        }
    }

    /**
     * Checks if a microphone session is currently active.
     */
    public static boolean isSessionActive() {
        return isSessionActive;
    }

    /**
     * Force reset the active session state lock.
     */
    public static void resetSessionState() {
        isSessionActive = false;
    }

    /**
     * Resolves voice confirmation prompt ("You can speak") in target user language.
     */
    public static String getVoiceConfirmationText(String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LanguageManager.LANG_MALAYALAM:
                return "നിങ്ങൾക്ക് സംസാരിക്കാം";
            case LanguageManager.LANG_HINDI:
                return "आप बोल सकते हैं";
            case LanguageManager.LANG_KANNADA:
            default:
                return "ನೀವು ಮಾತನಾಡಬಹುದು";
        }
    }

    /**
     * Resolves localized STT error message ("No speech detected").
     */
    public static String getNoSpeechErrorText(String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LanguageManager.LANG_MALAYALAM:
                return "ശബ്ദം കണ്ടെത്താനായില്ല. ദയവായി വീണ്ടും ശ്രമിക്കുക.";
            case LanguageManager.LANG_HINDI:
                return "कोई आवाज़ नहीं मिली। कृपया फिर से प्रयास करें।";
            case LanguageManager.LANG_KANNADA:
            default:
                return "ಯಾವುದೇ ಮಾತು ಕೇಳಿಸಲಿಲ್ಲ. ದಯವಿಟ್ಟು ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ.";
        }
    }

    /**
     * Resolves localized microphone permission required message.
     */
    public static String getPermissionRequiredText(String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LanguageManager.LANG_MALAYALAM:
                return "മൈക്രോഫോൺ അനുമതി ആവശ്യമാണ്.";
            case LanguageManager.LANG_HINDI:
                return "माइक्रोफ़ोन अनुमति की आवश्यकता है।";
            case LanguageManager.LANG_KANNADA:
            default:
                return "ಮೈಕ್ರೋಫೋನ್ ಅನುಮತಿ ಅಗತ್ಯವಿದೆ.";
        }
    }

    /**
     * Resolves generic STT failure message.
     */
    public static String getGenericErrorText(String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        switch (code) {
            case LanguageManager.LANG_MALAYALAM:
                return "ശബ്ദം തിരിച്ചറിയുന്നതിൽ പരാജയപ്പെട്ടു. ദയവായി വീണ്ടും ശ്രമിക്കുക.";
            case LanguageManager.LANG_HINDI:
                return "आवाज़ पहचानने में विफलता हुई। कृपया फिर से प्रयास करें।";
            case LanguageManager.LANG_KANNADA:
            default:
                return "ಮಾತು ಗುರುತಿಸುವಿಕೆ ವಿಫಲವಾಗಿದೆ. ದಯವಿಟ್ಟು ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ.";
        }
    }

    /**
     * Initiates the accessible microphone activation sequence:
     * 1. Short vibration.
     * 2. Voice confirmation: "You can speak" (in user language).
     * 3. WAITS for TTS confirmation to finish before executing STT start action.
     * Guaranteed with a 2.5-second safety fallback timeout.
     */
    public static void startAccessibleMicFlow(Context context, TextToSpeech tts, boolean isTtsInitialized, String langCode, Runnable startSttListeningAction) {
        if (isSessionActive) {
            Log.w(TAG, "Microphone session already in progress. Ignoring rapid duplicate tap.");
            return;
        }

        isSessionActive = true;

        // Step 1 — Immediate Short Vibration
        triggerShortVibration(context);

        String promptText = getVoiceConfirmationText(langCode);
        Locale locale = LanguageManager.getLocale(langCode);

        final AtomicBoolean hasStartedStt = new AtomicBoolean(false);
        final Runnable safeStartAction = new Runnable() {
            @Override
            public void run() {
                if (hasStartedStt.compareAndSet(false, true)) {
                    mainHandler.removeCallbacksAndMessages(null);
                    if (startSttListeningAction != null) {
                        try {
                            startSttListeningAction.run();
                        } catch (Exception e) {
                            Log.e(TAG, "Error executing startSttListeningAction", e);
                            isSessionActive = false;
                        }
                    }
                }
            }
        };

        // Safety fallback timer: Start listening after 2.5 seconds max even if TTS listener fails
        mainHandler.postDelayed(safeStartAction, 2500);

        // Step 2 & 3 — Speak Voice Confirmation & Wait for TTS completion before STT
        if (tts != null && isTtsInitialized) {
            try {
                tts.setLanguage(locale);

                final String utteranceId = "ACCESSIBLE_MIC_CONFIRM_" + System.currentTimeMillis();

                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId1) {}

                    @Override
                    public void onDone(String utteranceId1) {
                        if (utteranceId.equals(utteranceId1)) {
                            // Audio clearance padding: wait 350ms for speaker audio output to clear
                            mainHandler.postDelayed(safeStartAction, 350);
                        }
                    }

                    @Override
                    public void onError(String utteranceId1) {
                        if (utteranceId.equals(utteranceId1)) {
                            mainHandler.postDelayed(safeStartAction, 350);
                        }
                    }
                });

                int result;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Bundle params = new Bundle();
                    params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f);
                    result = tts.speak(promptText, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
                } else {
                    HashMap<String, String> map = new HashMap<>();
                    map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
                    result = tts.speak(promptText, TextToSpeech.QUEUE_FLUSH, map);
                }

                if (result == TextToSpeech.ERROR) {
                    Log.e(TAG, "TTS speak returned ERROR. Triggering direct STT start.");
                    mainHandler.postDelayed(safeStartAction, 300);
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception during TTS prompt playback", e);
                mainHandler.postDelayed(safeStartAction, 300);
            }
        } else {
            // TTS unavailable fallback: proceed to STT after short delay
            mainHandler.postDelayed(safeStartAction, 300);
        }
    }

    /**
     * Handles final successful STT result:
     * 1. Updates UI with recognized text.
     * 2. Triggers 1 short vibration.
     * 3. Automatically reads the recognized text out loud via TTS.
     */
    public static void onSttResultReceived(Context context, TextToSpeech tts, boolean isTtsInitialized, String langCode, String recognizedText, Runnable uiUpdateAction) {
        if (uiUpdateAction != null) {
            try {
                uiUpdateAction.run();
            } catch (Exception e) {
                Log.e(TAG, "Error executing uiUpdateAction", e);
            }
        }

        // Short vibration confirmation of successful STT completion
        triggerShortVibration(context);

        // Read recognized text aloud using TTS
        if (recognizedText != null && !recognizedText.trim().isEmpty()) {
            Locale targetLocale = LanguageManager.getLocale(langCode);
            if (tts != null && isTtsInitialized) {
                try {
                    tts.setLanguage(targetLocale);
                    CaregiverSoundManager.speakWithVolume(tts, recognizedText, "ACCESSIBLE_STT_RESULT_" + System.currentTimeMillis(), context);
                } catch (Exception e) {
                    Log.e(TAG, "Error playing automatic TTS for recognized text", e);
                }
            }
        }

        isSessionActive = false;
    }

    /**
     * Handles STT errors:
     * 1. Triggers 1 short vibration.
     * 2. Speaks localized error feedback.
     */
    public static void onSttErrorReceived(Context context, TextToSpeech tts, boolean isTtsInitialized, String langCode, int errorCode, Runnable uiErrorAction) {
        if (uiErrorAction != null) {
            try {
                uiErrorAction.run();
            } catch (Exception e) {
                Log.e(TAG, "Error executing uiErrorAction", e);
            }
        }

        triggerShortVibration(context);

        String errorText;
        if (errorCode == SpeechRecognizer.ERROR_NO_MATCH || errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            errorText = getNoSpeechErrorText(langCode);
        } else if (errorCode == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            errorText = getPermissionRequiredText(langCode);
        } else {
            errorText = getGenericErrorText(langCode);
        }

        if (tts != null && isTtsInitialized) {
            try {
                Locale targetLocale = LanguageManager.getLocale(langCode);
                tts.setLanguage(targetLocale);
                CaregiverSoundManager.speakWithVolume(tts, errorText, "ACCESSIBLE_STT_ERROR_" + System.currentTimeMillis(), context);
            } catch (Exception e) {
                Log.e(TAG, "Error speaking STT error text", e);
            }
        }

        isSessionActive = false;
    }

    /**
     * Handles microphone permission denied feedback:
     * 1. Short vibration.
     * 2. Speaks localized permission required explanation.
     */
    public static void onPermissionDenied(Context context, TextToSpeech tts, boolean isTtsInitialized, String langCode, Runnable uiErrorAction) {
        if (uiErrorAction != null) {
            try {
                uiErrorAction.run();
            } catch (Exception e) {
                Log.e(TAG, "Error executing uiErrorAction", e);
            }
        }

        triggerShortVibration(context);

        String errorText = getPermissionRequiredText(langCode);
        if (tts != null && isTtsInitialized) {
            try {
                Locale targetLocale = LanguageManager.getLocale(langCode);
                tts.setLanguage(targetLocale);
                CaregiverSoundManager.speakWithVolume(tts, errorText, "ACCESSIBLE_STT_PERM_DENIED_" + System.currentTimeMillis(), context);
            } catch (Exception e) {
                Log.e(TAG, "Error speaking permission denied text", e);
            }
        }

        isSessionActive = false;
    }
}
