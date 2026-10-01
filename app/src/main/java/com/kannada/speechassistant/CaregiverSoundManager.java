package com.kannada.speechassistant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages per-user notification ringtones (Default System, Custom File, Silent),
 * notification volume levels, separate TTS volume levels, and audio playback 
 * (3x sequence for regular messages, continuous looping for emergency alerts).
 */
public class CaregiverSoundManager {
    private static final String TAG = "CaregiverSoundManager";
    private static final String PREF_BASE_NAME = "CaregiverSoundSettings";
    private static final String KEY_CUSTOM_URI = "customRingtoneUri";
    private static final String KEY_CUSTOM_NAME = "customRingtoneName";
    private static final String KEY_USE_CUSTOM = "useCustomRingtone";
    private static final String KEY_SOUND_MODE = "soundMode"; // "default", "custom", "silent"
    private static final String KEY_VOLUME_PERCENT = "notificationVolumePercent";
    private static final String KEY_TTS_VOLUME_PERCENT = "ttsVolumePercent";
    private static final String LOCAL_RINGTONE_FILENAME = "custom_ringtone";

    public static final String MODE_DEFAULT = "default";
    public static final String MODE_CUSTOM = "custom";
    public static final String MODE_SILENT = "silent";

    private static MediaPlayer activeMediaPlayer = null;
    private static Ringtone activeEmergencyRingtone = null;
    private static Handler notificationHandler = null;
    private static Runnable notificationRunnable = null;
    private static boolean isNotificationPlaying = false;
    private static final java.util.Set<String> processedNotificationIds = new java.util.HashSet<>();
    private static final java.util.Map<String, Long> processedMessageContentMap = new java.util.HashMap<>();

    private static String getPrefName(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return PREF_BASE_NAME;
        }
        return PREF_BASE_NAME + "_" + userId.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private static String getCustomRingtoneFileName(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return LOCAL_RINGTONE_FILENAME + ".mp3";
        }
        return LOCAL_RINGTONE_FILENAME + "_" + userId.replaceAll("[^a-zA-Z0-9_-]", "_") + ".mp3";
    }

    public static String getCurrentUserId(Context context) {
        if (context == null) return null;
        try {
            SessionManager sm = new SessionManager(context);
            if (sm.isLoggedIn()) {
                String uid = sm.getUserDetails().get(SessionManager.KEY_USER_UID);
                if (uid != null && !uid.trim().isEmpty()) {
                    return uid;
                }
            }
        } catch (Exception ignored) {}
        try {
            com.google.firebase.auth.FirebaseUser u = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (u != null && u.getUid() != null && !u.getUid().trim().isEmpty()) {
                return u.getUid();
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Saves selected custom ringtone from File Manager / Gallery into internal app storage for a specific user.
     */
    public static boolean saveCustomRingtone(Context context, Uri sourceUri) {
        return saveCustomRingtone(context, sourceUri, getCurrentUserId(context));
    }

    public static boolean saveCustomRingtone(Context context, Uri sourceUri, String userId) {
        if (context == null || sourceUri == null) return false;
        try {
            stopNotificationSound(context);

            try {
                int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                context.getContentResolver().takePersistableUriPermission(sourceUri, takeFlags);
            } catch (Exception ignored) {}

            String displayName = getFileNameFromUri(context, sourceUri);
            String filename = getCustomRingtoneFileName(userId);

            File destFile = new File(context.getFilesDir(), filename);
            try (InputStream in = context.getContentResolver().openInputStream(sourceUri);
                 FileOutputStream out = new FileOutputStream(destFile)) {
                if (in == null) return false;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }

            SharedPreferences pref = context.getSharedPreferences(getPrefName(userId), Context.MODE_PRIVATE);
            pref.edit()
                    .putString(KEY_CUSTOM_URI, destFile.getAbsolutePath())
                    .putString(KEY_CUSTOM_NAME, displayName != null ? displayName : "Custom Ringtone")
                    .putBoolean(KEY_USE_CUSTOM, true)
                    .putString(KEY_SOUND_MODE, MODE_CUSTOM)
                    .apply();

            syncSettingsToFirestore(context, userId);
            Log.i(TAG, "Successfully saved custom ringtone for user " + userId + ": " + displayName);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed saving custom ringtone from Uri for user " + userId, e);
            return false;
        }
    }

    /**
     * Resets ringtone sound back to system default for a specific user.
     */
    public static void resetToDefaultRingtone(Context context) {
        resetToDefaultRingtone(context, getCurrentUserId(context));
    }

    public static void resetToDefaultRingtone(Context context, String userId) {
        if (context == null) return;
        stopNotificationSound(context);
        File destFile = new File(context.getFilesDir(), getCustomRingtoneFileName(userId));
        if (destFile.exists()) {
            destFile.delete();
        }
        SharedPreferences pref = context.getSharedPreferences(getPrefName(userId), Context.MODE_PRIVATE);
        pref.edit()
                .remove(KEY_CUSTOM_URI)
                .remove(KEY_CUSTOM_NAME)
                .putBoolean(KEY_USE_CUSTOM, false)
                .putString(KEY_SOUND_MODE, MODE_DEFAULT)
                .apply();

        syncSettingsToFirestore(context, userId);
    }

    /**
     * Sets ringtone mode to Silent (Muted) for a specific user.
     */
    public static void setModeSilent(Context context) {
        setModeSilent(context, getCurrentUserId(context));
    }

    public static void setModeSilent(Context context, String userId) {
        if (context == null) return;
        stopNotificationSound(context);
        SharedPreferences pref = context.getSharedPreferences(getPrefName(userId), Context.MODE_PRIVATE);
        pref.edit()
                .putString(KEY_SOUND_MODE, MODE_SILENT)
                .apply();

        syncSettingsToFirestore(context, userId);
    }

    public static void enableSoundMode(Context context) {
        enableSoundMode(context, getCurrentUserId(context));
    }

    public static void enableSoundMode(Context context, String userId) {
        if (context == null) return;
        stopNotificationSound(context);
        SharedPreferences pref = context.getSharedPreferences(getPrefName(userId), Context.MODE_PRIVATE);
        boolean useCustom = pref.getBoolean(KEY_USE_CUSTOM, false);
        File customFile = new File(context.getFilesDir(), getCustomRingtoneFileName(userId));
        String targetMode = (useCustom && customFile.exists()) ? MODE_CUSTOM : MODE_DEFAULT;
        pref.edit()
                .putString(KEY_SOUND_MODE, targetMode)
                .apply();

        syncSettingsToFirestore(context, userId);
    }

    public static void toggleSilentMode(Context context) {
        toggleSilentMode(context, getCurrentUserId(context));
    }

    public static void toggleSilentMode(Context context, String userId) {
        if (context == null) return;
        if (isSilentMode(context, userId)) {
            enableSoundMode(context, userId);
        } else {
            setModeSilent(context, userId);
        }
    }

    public static boolean isSilentMode(Context context) {
        return isSilentMode(context, getCurrentUserId(context));
    }

    public static boolean isSilentMode(Context context, String userId) {
        if (context == null) return false;
        SharedPreferences pref = context.getSharedPreferences(getPrefName(userId), Context.MODE_PRIVATE);
        String mode = pref.getString(KEY_SOUND_MODE, MODE_DEFAULT);
        return MODE_SILENT.equals(mode);
    }

    /**
     * Gets active ringtone mode ("default", "custom", or "silent") for a specific user.
     */
    public static String getSoundMode(Context context) {
        return getSoundMode(context, getCurrentUserId(context));
    }

    public static String getSoundMode(Context context, String userId) {
        if (context == null) return MODE_DEFAULT;
        SharedPreferences pref = context.getSharedPreferences(getPrefName(userId), Context.MODE_PRIVATE);
        String mode = pref.getString(KEY_SOUND_MODE, null);
        if (mode != null) return mode;

        boolean useCustom = pref.getBoolean(KEY_USE_CUSTOM, false);
        File customFile = new File(context.getFilesDir(), getCustomRingtoneFileName(userId));
        if (useCustom && customFile.exists()) {
            return MODE_CUSTOM;
        }
        return MODE_DEFAULT;
    }

    /**
     * Saves notification volume percentage (0 - 100) for a specific user without mutating system hardware volume streams.
     */
    public static void saveVolumePercent(Context context, int percent) {
        saveNotificationVolumePercent(context, getCurrentUserId(context), percent);
    }

    public static void saveNotificationVolumePercent(Context context, String userId, int percent) {
        if (context == null) return;
        percent = Math.max(0, Math.min(100, percent));
        SharedPreferences pref = context.getSharedPreferences(getPrefName(userId), Context.MODE_PRIVATE);
        pref.edit().putInt(KEY_VOLUME_PERCENT, percent).apply();

        syncSettingsToFirestore(context, userId);
    }

    public static int getVolumePercent(Context context) {
        return getNotificationVolumePercent(context, getCurrentUserId(context));
    }

    public static int getNotificationVolumePercent(Context context, String userId) {
        if (context == null) return 100;
        SharedPreferences pref = context.getSharedPreferences(getPrefName(userId), Context.MODE_PRIVATE);
        return pref.getInt(KEY_VOLUME_PERCENT, 100);
    }

    /**
     * Saves TTS volume percentage (0 - 100) for a specific user.
     */
    public static void saveTtsVolumePercent(Context context, String userId, int percent) {
        if (context == null) return;
        percent = Math.max(0, Math.min(100, percent));
        SharedPreferences pref = context.getSharedPreferences(getPrefName(userId), Context.MODE_PRIVATE);
        pref.edit().putInt(KEY_TTS_VOLUME_PERCENT, percent).apply();

        syncSettingsToFirestore(context, userId);
    }

    public static int getTtsVolumePercent(Context context, String userId) {
        if (context == null) return 100;
        SharedPreferences pref = context.getSharedPreferences(getPrefName(userId), Context.MODE_PRIVATE);
        return pref.getInt(KEY_TTS_VOLUME_PERCENT, 100);
    }

    /**
     * Deprecated system stream volume modifier retained for safe call compatibility if referenced,
     * but does NOT force change hardware media/notif volumes globally to respect User Request.
     */
    @Deprecated
    public static void applySystemStreamVolume(Context context, int volPercent) {
        // Intentionally no-op to preserve media, music, call, and alarm streams independently.
    }

    /**
     * Synthesizes voice via TextToSpeech strictly obeying the user's TTS Volume percentage.
     */
    public static void speakWithVolume(android.speech.tts.TextToSpeech tts, String text, String utteranceId, Context context) {
        speakWithVolume(tts, text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, utteranceId, context, getCurrentUserId(context));
    }

    public static void speakWithVolume(android.speech.tts.TextToSpeech tts, String text, int queueMode, String utteranceId, Context context) {
        speakWithVolume(tts, text, queueMode, utteranceId, context, getCurrentUserId(context));
    }

    public static void speakWithVolume(android.speech.tts.TextToSpeech tts, String text, int queueMode, String utteranceId, Context context, String userId) {
        if (tts == null || text == null || text.trim().isEmpty() || context == null) return;
        try {
            int volPercent = getTtsVolumePercent(context, userId);
            if (volPercent <= 0) {
                // TTS volume is Muted (0%) -> Do not speak
                return;
            }

            float volFloat = volPercent / 100.0f;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                android.os.Bundle params = new android.os.Bundle();
                params.putFloat(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME, volFloat);
                tts.speak(text, queueMode, params, utteranceId);
            } else {
                java.util.HashMap<String, String> params = new java.util.HashMap<>();
                params.put(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME, String.valueOf(volFloat));
                tts.speak(text, queueMode, params);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed speaking TTS with TTS volume for user: " + userId, e);
        }
    }

    private static float calculateMediaVolume(int volPercent) {
        if (volPercent <= 0) return 0.0f;
        if (volPercent >= 100) return 1.0f;
        // Human ear perception logarithmic curve for Android MediaPlayer (0.0f to 1.0f)
        return (float) (1.0 - (Math.log(101 - volPercent) / Math.log(101)));
    }

    public static boolean isSoundPlaying() {
        return isNotificationPlaying || (activeMediaPlayer != null && activeMediaPlayer.isPlaying());
    }

    public static String getActiveRingtoneName(Context context) {
        return getActiveRingtoneName(context, getCurrentUserId(context));
    }

    public static String getActiveRingtoneName(Context context, String userId) {
        if (context == null) return "Default System Ringtone";
        SharedPreferences pref = context.getSharedPreferences(getPrefName(userId), Context.MODE_PRIVATE);

        String name = "Default System Ringtone";
        String mode = getSoundMode(context, userId);
        File customFile = new File(context.getFilesDir(), getCustomRingtoneFileName(userId));
        boolean useCustom = MODE_CUSTOM.equals(mode) && customFile.exists();
        if (useCustom) {
            String customName = pref.getString(KEY_CUSTOM_NAME, null);
            if (customName != null && !customName.trim().isEmpty()) {
                name = customName;
            } else {
                name = "Custom Ringtone";
            }
        }

        if (MODE_SILENT.equals(mode)) {
            return name + " (Silent)";
        }

        return name;
    }

    /**
     * Stops notification sound playback immediately.
     */
    public static synchronized void stopNotificationSound(Context context) {
        isNotificationPlaying = false;
        try {
            if (notificationHandler != null && notificationRunnable != null) {
                notificationHandler.removeCallbacks(notificationRunnable);
                notificationRunnable = null;
                notificationHandler = null;
            }
            if (activeMediaPlayer != null) {
                try {
                    if (activeMediaPlayer.isPlaying()) {
                        activeMediaPlayer.stop();
                    }
                    activeMediaPlayer.release();
                } catch (Exception ignored) {}
                activeMediaPlayer = null;
            }
            if (context != null) {
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) v.cancel();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed stopping notification sound", e);
        }
    }

    public static boolean isMessageProcessed(String messageId) {
        if (messageId == null || messageId.isEmpty()) return false;
        synchronized (processedNotificationIds) {
            return processedNotificationIds.contains(messageId);
        }
    }

    public static boolean markMessageProcessed(String messageId) {
        return markMessageProcessed(messageId, null, null);
    }

    public static boolean markMessageProcessed(String messageId, String senderId, String messageText) {
        long now = System.currentTimeMillis();
        synchronized (processedNotificationIds) {
            // Prune old entries from content map older than 30 seconds
            java.util.Iterator<java.util.Map.Entry<String, Long>> iterator = processedMessageContentMap.entrySet().iterator();
            while (iterator.hasNext()) {
                if (now - iterator.next().getValue() > 30000) {
                    iterator.remove();
                }
            }

            boolean isNewId = false;
            if (messageId != null && !messageId.trim().isEmpty()) {
                if (processedNotificationIds.contains(messageId)) {
                    return false; // Already processed by ID
                }
                isNewId = true;
            }

            if (senderId != null && messageText != null && !messageText.trim().isEmpty()) {
                String contentKey = senderId + "::" + messageText.trim();
                Long lastTime = processedMessageContentMap.get(contentKey);
                if (lastTime != null && (now - lastTime < 8000)) {
                    return false; // Duplicate content payload within 8s window
                }
                processedMessageContentMap.put(contentKey, now);
            }

            if (isNewId) {
                processedNotificationIds.add(messageId);
            }

            return true; // Newly marked for processing
        }
    }

    private static android.speech.tts.TextToSpeech helperTts = null;

    public static synchronized void speakIncomingMessageTts(Context context, String messageText, String userId, String userLanguage) {
        if (context == null || messageText == null || messageText.trim().isEmpty()) return;
        int ttsVol = getTtsVolumePercent(context, userId);
        if (ttsVol <= 0) return; // Muted via TTS Volume setting

        // Resolve target language strictly from receiving Physically Disabled User's account preference
        String langCode = userLanguage;
        if (langCode == null || langCode.trim().isEmpty()) {
            try {
                SessionManager sm = new SessionManager(context);
                langCode = sm.getLanguage();
            } catch (Exception ignored) {}
        }
        String normLangCode = LanguageManager.normalizeLanguageCode(langCode);
        java.util.Locale locale = LanguageManager.getLocale(normLangCode);

        if (helperTts == null) {
            final String finalNormLang = normLangCode;
            final java.util.Locale finalLocale = locale;
            helperTts = new android.speech.tts.TextToSpeech(context.getApplicationContext(), status -> {
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    setTtsLanguageEngine(helperTts, finalLocale, finalNormLang);
                    speakWithVolume(helperTts, messageText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, "IncomingTTS_" + System.currentTimeMillis(), context, userId);
                }
            });
        } else {
            setTtsLanguageEngine(helperTts, locale, normLangCode);
            speakWithVolume(helperTts, messageText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, "IncomingTTS_" + System.currentTimeMillis(), context, userId);
        }
    }

    private static void setTtsLanguageEngine(android.speech.tts.TextToSpeech ttsEngine, java.util.Locale locale, String normLangCode) {
        if (ttsEngine == null) return;
        try {
            int result = ttsEngine.setLanguage(locale);
            if (result == android.speech.tts.TextToSpeech.LANG_MISSING_DATA || result == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsEngine.setLanguage(new java.util.Locale(normLangCode));
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not set TTS language for locale: " + normLangCode, e);
        }
    }

    /**
     * Handles incoming messages across all 3 user modules cleanly, enforcing strict atomic de-duplication,
     * module-specific notification ringtone (2x play), emergency alert looping, and Physically Disabled TTS.
     */
    public static void handleIncomingMessage(Context context, String messageId, String senderId, String messageText, String messageType, String senderName, String senderRole) {
        if (context == null) return;
        String currentUserId = getCurrentUserId(context);
        
        // Rule 1: Do NOT trigger notification sound or TTS for messages sent by the current logged-in user
        if (currentUserId != null && currentUserId.equals(senderId)) {
            return;
        }

        // Rule 2: De-duplication: process each message exactly once
        if (!markMessageProcessed(messageId, senderId, messageText)) {
            return; // Already processed
        }

        // Cache incoming notification in NotificationService for instant voice assistant accessibility
        NotificationService.recordNotification(
                senderName != null && !senderName.isEmpty() ? senderName : "Caregiver",
                messageText,
                messageType != null ? messageType : "text",
                System.currentTimeMillis()
        );

        SessionManager sm = new SessionManager(context);
        String userRole = sm.getUserRole();
        boolean isEmergency = "emergency".equalsIgnoreCase(messageType) || "sos".equalsIgnoreCase(messageType);

        if (isEmergency) {
            // Emergency Alert: If Deaf User, vibrate only silently. For other roles, play emergency sound & vibration loop.
            if (RoleManager.ROLE_SPEECH_IMPAIRED.equals(userRole)) {
                vibrateNotification(context);
            } else {
                playEmergencyAlertSoundAndVibration(context, currentUserId);
            }
        } else {
            // Regular Message:
            if (RoleManager.ROLE_PHYSICALLY_DISABLED.equals(userRole)) {
                // Physically Disabled User: Play sound 2 times -> Speak received message using TTS in user's saved language
                playNotificationSound2Times(context, currentUserId, messageId, () -> {
                    speakIncomingMessageTts(context, messageText, currentUserId, sm.getLanguage());
                });
            } else if (RoleManager.ROLE_SPEECH_IMPAIRED.equals(userRole)) {
                // Deaf User: Notification sound is disabled, vibrate only
                vibrateNotification(context);
            } else {
                // Caregiver/Admin & Mute User: Play sound 2 times -> STOP (No TTS)
                playNotificationSound2Times(context, currentUserId, messageId, null);
            }
        }
    }

    /**
     * Helper method to trigger double pulse vibration pattern.
     */
    public static void vibrateNotification(Context context) {
        if (context == null) return;
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                long[] pattern = {0, 250, 150, 250};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(pattern, -1));
                } else {
                    v.vibrate(pattern, -1);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Vibration failed", e);
        }
    }

    /**
     * Plays notification sound EXACTLY 2 TIMES sequentially for regular incoming messages using current user settings.
     */
    public static synchronized void playNotificationSound2Times(Context context) {
        playNotificationSound2Times(context, getCurrentUserId(context), null, null);
    }

    public static synchronized void playNotificationSound2Times(Context context, String recipientUserId) {
        playNotificationSound2Times(context, recipientUserId, null, null);
    }

    public static synchronized void playNotificationSound2Times(Context context, String recipientUserId, String messageId) {
        playNotificationSound2Times(context, recipientUserId, messageId, null);
    }

    public static synchronized void playNotificationSound2Times(Context context, String recipientUserId, String messageId, Runnable onFinished) {
        if (context == null) {
            if (onFinished != null) onFinished.run();
            return;
        }

        try {
            stopNotificationSound(context);

            // Active Call Protection: If voice call is active or ringing, suppress notification audio to avoid interrupting call
            if (VoiceCallManager.getInstance(context).isCallActive()) {
                Log.d(TAG, "Voice call is currently active. Suppressing regular notification audio to preserve call audio.");
                if (onFinished != null) onFinished.run();
                return;
            }

            // Always trigger vibration for incoming regular messages
            vibrateNotification(context);

            int volPercent = getNotificationVolumePercent(context, recipientUserId);
            String mode = getSoundMode(context, recipientUserId);
            if (volPercent <= 0 || MODE_SILENT.equals(mode)) {
                if (onFinished != null) onFinished.run();
                return; // Muted sound, vibration was performed above
            }

            File customFile = new File(context.getFilesDir(), getCustomRingtoneFileName(recipientUserId));
            boolean useCustom = MODE_CUSTOM.equals(mode) && customFile.exists();

            isNotificationPlaying = true;
            notificationHandler = new Handler(Looper.getMainLooper());
            final int[] count = {0};

            if (useCustom) {
                notificationRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (!isNotificationPlaying || VoiceCallManager.getInstance(context).isCallActive()) {
                            isNotificationPlaying = false;
                            stopNotificationSound(context);
                            if (onFinished != null) onFinished.run();
                            return;
                        }
                        if (count[0] < 2) {
                            count[0]++;
                            playCustomAudioFile(context, customFile, volPercent, () -> {
                                if (isNotificationPlaying && count[0] < 2 && notificationHandler != null && notificationRunnable != null) {
                                    if (VoiceCallManager.getInstance(context).isCallActive()) {
                                        isNotificationPlaying = false;
                                        stopNotificationSound(context);
                                        if (onFinished != null) onFinished.run();
                                        return;
                                    }
                                    notificationHandler.postDelayed(notificationRunnable, 400);
                                } else {
                                    isNotificationPlaying = false;
                                    if (onFinished != null) onFinished.run();
                                }
                            });
                        } else {
                            isNotificationPlaying = false;
                            if (onFinished != null) onFinished.run();
                        }
                    }
                };
            } else {
                notificationRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (!isNotificationPlaying || VoiceCallManager.getInstance(context).isCallActive()) {
                            isNotificationPlaying = false;
                            stopNotificationSound(context);
                            if (onFinished != null) onFinished.run();
                            return;
                        }
                        if (count[0] < 2) {
                            count[0]++;
                            playDefaultRingtoneOnce(context, volPercent, () -> {
                                if (isNotificationPlaying && count[0] < 2 && notificationHandler != null && notificationRunnable != null) {
                                    if (VoiceCallManager.getInstance(context).isCallActive()) {
                                        isNotificationPlaying = false;
                                        stopNotificationSound(context);
                                        if (onFinished != null) onFinished.run();
                                        return;
                                    }
                                    notificationHandler.postDelayed(notificationRunnable, 400);
                                } else {
                                    isNotificationPlaying = false;
                                    if (onFinished != null) onFinished.run();
                                }
                            });
                        } else {
                            isNotificationPlaying = false;
                            if (onFinished != null) onFinished.run();
                        }
                    }
                };
            }
            notificationHandler.post(notificationRunnable);

        } catch (Exception e) {
            Log.e(TAG, "Failed playing 2x notification sound for recipient: " + recipientUserId, e);
            if (onFinished != null) onFinished.run();
        }
    }

    @Deprecated
    public static synchronized void playNotificationSound3Times(Context context) {
        playNotificationSound2Times(context);
    }

    @Deprecated
    public static synchronized void playNotificationSound3Times(Context context, String recipientUserId) {
        playNotificationSound2Times(context, recipientUserId);
    }

    @Deprecated
    public static synchronized void playNotificationSound3Times(Context context, String recipientUserId, String messageId) {
        playNotificationSound2Times(context, recipientUserId, messageId, null);
    }

    /**
     * Plays test sample preview of active notification sound for logged-in user.
     */
    public static void testPlaySound(Context context) {
        testPlaySound(context, getCurrentUserId(context));
    }

    public static void testPlaySound(Context context, String userId) {
        if (context == null) return;
        try {
            if (isSoundPlaying()) {
                stopNotificationSound(context);
                Toast.makeText(context, "Notification sound test stopped", Toast.LENGTH_SHORT).show();
                return;
            }
            stopNotificationSound(context);

            String mode = getSoundMode(context, userId);
            if (MODE_SILENT.equals(mode)) {
                Toast.makeText(context, "🔇 Silent mode is active (Sound muted, vibration only)", Toast.LENGTH_SHORT).show();
                vibrateNotification(context);
                return;
            }

            int volPercent = getNotificationVolumePercent(context, userId);
            if (volPercent <= 0) {
                Toast.makeText(context, "Notification volume is 0%", Toast.LENGTH_SHORT).show();
                vibrateNotification(context);
                return;
            }

            File customFile = new File(context.getFilesDir(), getCustomRingtoneFileName(userId));
            boolean useCustom = MODE_CUSTOM.equals(mode) && customFile.exists();

            if (useCustom) {
                playCustomAudioFile(context, customFile, volPercent, null);
            } else {
                playDefaultRingtoneOnce(context, volPercent, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed playing test sound for user: " + userId, e);
        }
    }

    /**
     * Plays sample test speech via TTS at current Voice Assistant volume setting.
     */
    public static void testPlayTts(Context context, String userId) {
        if (context == null) return;
        int ttsVol = getTtsVolumePercent(context, userId);
        if (ttsVol <= 0) {
            Toast.makeText(context, "Voice Assistant volume is 0% (Muted)", Toast.LENGTH_SHORT).show();
            vibrateNotification(context);
            return;
        }
        SessionManager sm = new SessionManager(context);
        String lang = sm.getLanguage();
        String normLang = LanguageManager.normalizeLanguageCode(lang);
        String testPhrase;
        if ("kn".equalsIgnoreCase(normLang)) {
            testPhrase = "ಧ್ವನಿ ಸಹಾಯಕರ ವಾಲ್ಯೂಮ್ ಪರೀಕ್ಷೆ.";
        } else if ("hi".equalsIgnoreCase(normLang)) {
            testPhrase = "वॉयस असिस्टेंट आवाज़ परीक्षण।";
        } else if ("ml".equalsIgnoreCase(normLang)) {
            testPhrase = "വോയ്‌സ് അസിസ്റ്റന്റ് ശബ്ദം പരിശോധന.";
        } else {
            testPhrase = "Testing Voice Assistant sound volume.";
        }
        speakIncomingMessageTts(context, testPhrase, userId, lang);
    }

    public static void testPlayTts(Context context, android.speech.tts.TextToSpeech tts, String userId) {
        if (context == null) return;
        int ttsVol = getTtsVolumePercent(context, userId);
        if (ttsVol <= 0) {
            Toast.makeText(context, "Voice Assistant volume is 0% (Muted)", Toast.LENGTH_SHORT).show();
            vibrateNotification(context);
            return;
        }
        testPlayTts(context, userId);
    }

    /**
     * Plays emergency alert sound continuously in a loop for SOS alerts.
     */
    public static void playEmergencyAlertSoundAndVibration(Context context) {
        playEmergencyAlertSoundAndVibration(context, getCurrentUserId(context));
    }

    public static void playEmergencyAlertSoundAndVibration(Context context, String recipientUserId) {
        if (context == null) return;
        try {
            int volPercent = getNotificationVolumePercent(context, recipientUserId);
            if (volPercent <= 0) volPercent = 100; // Emergency alerts fallback to audible volume

            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                long[] pattern = {0, 600, 200, 600, 200, 600};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(pattern, 0));
                } else {
                    v.vibrate(pattern, 0);
                }
            }

            File customFile = new File(context.getFilesDir(), getCustomRingtoneFileName(recipientUserId));
            String mode = getSoundMode(context, recipientUserId);
            boolean useCustom = MODE_CUSTOM.equals(mode) && customFile.exists();

            if (useCustom) {
                playCustomAudioFileLoop(context, customFile, volPercent);
            } else {
                playDefaultEmergencyRingtoneLoop(context, volPercent);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed playing emergency sound & vibration", e);
        }
    }

    private static void playDefaultEmergencyRingtoneLoop(Context context, int volPercent) {
        try {
            stopNotificationSound(context);

            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }

            activeMediaPlayer = new MediaPlayer();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activeMediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
            } else {
                activeMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            }
            activeMediaPlayer.setDataSource(context.getApplicationContext(), alarmUri);
            float volume = calculateMediaVolume(volPercent);
            activeMediaPlayer.setVolume(volume, volume);
            activeMediaPlayer.setLooping(true);
            activeMediaPlayer.prepare();
            activeMediaPlayer.setVolume(volume, volume);
            activeMediaPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "Failed playing default emergency ringtone loop via MediaPlayer", e);
        }
    }

    public static void stopEmergencySound(Context context) {
        try {
            if (activeEmergencyRingtone != null && activeEmergencyRingtone.isPlaying()) {
                activeEmergencyRingtone.stop();
            }
            stopNotificationSound(context);
        } catch (Exception ignored) {}
    }

    private static void stopActiveMediaPlayerOnly() {
        try {
            if (activeMediaPlayer != null) {
                try {
                    if (activeMediaPlayer.isPlaying()) {
                        activeMediaPlayer.stop();
                    }
                    activeMediaPlayer.release();
                } catch (Exception ignored) {}
                activeMediaPlayer = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed stopping active media player", e);
        }
    }

    private static void playCustomAudioFileLoop(Context context, File audioFile, int volPercent) {
        try {
            stopNotificationSound(context);

            if (volPercent <= 0) volPercent = 100;

            activeMediaPlayer = new MediaPlayer();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activeMediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
            } else {
                activeMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            }
            activeMediaPlayer.setDataSource(audioFile.getAbsolutePath());
            float volume = calculateMediaVolume(volPercent);
            activeMediaPlayer.setVolume(volume, volume);
            activeMediaPlayer.setLooping(true);
            activeMediaPlayer.prepare();
            activeMediaPlayer.setVolume(volume, volume);
            activeMediaPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "Failed to play custom emergency audio loop", e);
        }
    }

    private static void playCustomAudioFile(Context context, File audioFile, int volPercent, Runnable onComplete) {
        try {
            stopActiveMediaPlayerOnly();

            if (volPercent <= 0) {
                if (onComplete != null) onComplete.run();
                return;
            }

            activeMediaPlayer = new MediaPlayer();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activeMediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
            } else {
                activeMediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            }
            activeMediaPlayer.setDataSource(audioFile.getAbsolutePath());
            float volume = calculateMediaVolume(volPercent);
            activeMediaPlayer.setVolume(volume, volume);
            activeMediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                activeMediaPlayer = null;
                if (onComplete != null) onComplete.run();
            });
            activeMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.w(TAG, "MediaPlayer error on custom audio playback: what=" + what + ", extra=" + extra);
                try {
                    mp.release();
                } catch (Exception ignored) {}
                activeMediaPlayer = null;
                if (onComplete != null) onComplete.run();
                return true;
            });
            activeMediaPlayer.prepare();
            activeMediaPlayer.setVolume(volume, volume);
            activeMediaPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "Failed to play custom audio file", e);
            playDefaultRingtoneOnce(context, volPercent, onComplete);
        }
    }

    private static void playDefaultRingtoneOnce(Context context, int volPercent, Runnable onComplete) {
        try {
            stopActiveMediaPlayerOnly();

            if (volPercent <= 0) {
                if (onComplete != null) onComplete.run();
                return;
            }

            Uri notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (notificationUri == null) {
                notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }

            activeMediaPlayer = new MediaPlayer();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activeMediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
            } else {
                activeMediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            }
            activeMediaPlayer.setDataSource(context.getApplicationContext(), notificationUri);
            float volume = calculateMediaVolume(volPercent);
            activeMediaPlayer.setVolume(volume, volume);
            activeMediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                activeMediaPlayer = null;
                if (onComplete != null) onComplete.run();
            });
            activeMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.w(TAG, "MediaPlayer error on default ringtone playback: what=" + what + ", extra=" + extra);
                try {
                    mp.release();
                } catch (Exception ignored) {}
                activeMediaPlayer = null;
                if (onComplete != null) onComplete.run();
                return true;
            });
            activeMediaPlayer.prepare();
            activeMediaPlayer.setVolume(volume, volume);
            activeMediaPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "Failed to play default ringtone via MediaPlayer", e);
            if (onComplete != null) onComplete.run();
        }
    }

    private static String getFileNameFromUri(Context context, Uri uri) {
        String fileName = "custom_ringtone.mp3";
        if (uri == null) return fileName;
        try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
            }
        } catch (Exception ignored) {}
        return fileName;
    }

    /**
     * Syncs local user notification settings to Firestore users collection.
     */
    public static void syncSettingsToFirestore(Context context, String userId) {
        if (userId == null || userId.trim().isEmpty()) return;
        try {
            SharedPreferences pref = context.getSharedPreferences(getPrefName(userId), Context.MODE_PRIVATE);
            Map<String, Object> settings = new HashMap<>();
            settings.put("soundMode", pref.getString(KEY_SOUND_MODE, MODE_DEFAULT));
            settings.put("customRingtoneName", pref.getString(KEY_CUSTOM_NAME, ""));
            settings.put("notificationVolumePercent", pref.getInt(KEY_VOLUME_PERCENT, 100));
            settings.put("ttsVolumePercent", pref.getInt(KEY_TTS_VOLUME_PERCENT, 100));
            settings.put("updatedAt", System.currentTimeMillis());

            FirebaseFirestore.getInstance()
                    .collection("users").document(userId)
                    .update("notificationSettings", settings)
                    .addOnFailureListener(e -> Log.w(TAG, "Failed syncing settings to Firestore: " + e.getMessage()));
        } catch (Exception e) {
            Log.w(TAG, "Could not sync settings to Firestore", e);
        }
    }

    /**
     * Fetches user notification settings from Firestore and caches locally in SharedPreferences.
     */
    public static void fetchSettingsFromFirestore(Context context, String userId, Runnable onComplete) {
        if (context == null || userId == null || userId.trim().isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }
        try {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && documentSnapshot.contains("notificationSettings")) {
                            Map<String, Object> map = (Map<String, Object>) documentSnapshot.get("notificationSettings");
                            if (map != null) {
                                SharedPreferences pref = context.getSharedPreferences(getPrefName(userId), Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = pref.edit();

                                if (map.containsKey("soundMode")) {
                                    editor.putString(KEY_SOUND_MODE, (String) map.get("soundMode"));
                                }
                                if (map.containsKey("customRingtoneName")) {
                                    editor.putString(KEY_CUSTOM_NAME, (String) map.get("customRingtoneName"));
                                }
                                if (map.containsKey("notificationVolumePercent")) {
                                    Long notifVol = (Long) map.get("notificationVolumePercent");
                                    if (notifVol != null) editor.putInt(KEY_VOLUME_PERCENT, notifVol.intValue());
                                }
                                if (map.containsKey("ttsVolumePercent")) {
                                    Long ttsVol = (Long) map.get("ttsVolumePercent");
                                    if (ttsVol != null) editor.putInt(KEY_TTS_VOLUME_PERCENT, ttsVol.intValue());
                                }
                                editor.apply();
                            }
                        }
                        if (onComplete != null) onComplete.run();
                    })
                    .addOnFailureListener(e -> {
                        if (onComplete != null) onComplete.run();
                    });
        } catch (Exception e) {
            if (onComplete != null) onComplete.run();
        }
    }
}
