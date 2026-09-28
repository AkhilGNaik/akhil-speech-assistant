package com.kannada.speechassistant;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controller for the Real-Time Voice Call activity.
 * Supports accessible TTS audio feedback for Blind Users, call duration timer,
 * microphone mute/unmute, and speakerphone toggles.
 */
public class VoiceCallActivity extends AppCompatActivity implements VoiceCallManager.CallListener {

    private static final String TAG = "VoiceCallActivity";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase));
    }

    private TextView txtCallRemoteName;
    private TextView txtCallStatus;
    private TextView txtCallTimer;
    private MaterialButton btnMuteCall;
    private MaterialButton btnSpeakerCall;
    private MaterialButton btnEndCall;
    private TextView txtMuteLabel;
    private TextView txtSpeakerLabel;

    private VoiceCallManager voiceCallManager;
    private SessionManager sessionManager;
    private TextToSpeech tts;
    private boolean isTtsInitialized = false;
    private String pendingSpokenText = null;
    private Runnable pendingOnDoneAction = null;
    private boolean isEndingCall = false;

    private int callSeconds = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private boolean isTimerRunning = false;

    private String isCallerRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on during active call
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_voice_call);

        sessionManager = new SessionManager(this);
        initTextToSpeech();

        txtCallRemoteName = findViewById(R.id.txtCallRemoteName);
        txtCallStatus = findViewById(R.id.txtCallStatus);
        txtCallTimer = findViewById(R.id.txtCallTimer);
        btnMuteCall = findViewById(R.id.btnMuteCall);
        btnSpeakerCall = findViewById(R.id.btnSpeakerCall);
        btnEndCall = findViewById(R.id.btnEndCall);
        txtMuteLabel = findViewById(R.id.txtMuteLabel);
        txtSpeakerLabel = findViewById(R.id.txtSpeakerLabel);

        voiceCallManager = VoiceCallManager.getInstance(this);
        voiceCallManager.setCallListener(this);

        String remoteName = getIntent().getStringExtra("remoteName");
        if (remoteName != null && !remoteName.isEmpty()) {
            txtCallRemoteName.setText(remoteName);
        } else if (voiceCallManager.getRemoteName() != null) {
            txtCallRemoteName.setText(voiceCallManager.getRemoteName());
        }

        btnMuteCall.setOnClickListener(v -> {
            boolean isMuted = voiceCallManager.toggleMute();
            updateMuteButtonUI(isMuted);
        });

        btnSpeakerCall.setOnClickListener(v -> {
            boolean isSpeakerOn = voiceCallManager.toggleSpeaker();
            updateSpeakerButtonUI(isSpeakerOn);
        });

        btnEndCall.setOnClickListener(v -> {
            btnEndCall.setEnabled(false);
            if (!isEndingCall) {
                isEndingCall = true;
                speakAccessibleFeedback("Voice call ended.", () -> {
                    voiceCallManager.endCall();
                    finish();
                });
            }
        });

        updateMuteButtonUI(voiceCallManager.isMuted());
        updateSpeakerButtonUI(voiceCallManager.isSpeakerOn());

        // Initial TTS state feedback for Blind User
        if (voiceCallManager.isCaller()) {
            speakAccessibleFeedback("Calling caregiver", null);
        }
    }

    private void initTextToSpeech() {
        try {
            tts = new TextToSpeech(getApplicationContext(), status -> {
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true;
                    String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
                    Locale locale = LanguageManager.getLocale(userLang);
                    tts.setLanguage(locale);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        android.media.AudioAttributes aa = new android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build();
                        tts.setAudioAttributes(aa);
                    }
                    Log.i(TAG, "TTS initialized with Google TTS engine successfully in VoiceCallActivity.");
                    checkPendingSpeech();
                } else {
                    Log.e(TAG, "TTS initialization failed with code: " + status + ", attempting fallback.");
                    fallbackInitTextToSpeech();
                }
            }, "com.google.android.tts");
        } catch (Exception e) {
            fallbackInitTextToSpeech();
        }
    }

    private void fallbackInitTextToSpeech() {
        try {
            tts = new TextToSpeech(getApplicationContext(), status -> {
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true;
                    String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
                    Locale locale = LanguageManager.getLocale(userLang);
                    tts.setLanguage(locale);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        android.media.AudioAttributes aa = new android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build();
                        tts.setAudioAttributes(aa);
                    }
                    checkPendingSpeech();
                }
            });
        } catch (Exception ignored) {}
    }

    private void checkPendingSpeech() {
        if (pendingSpokenText != null) {
            String text = pendingSpokenText;
            Runnable action = pendingOnDoneAction;
            pendingSpokenText = null;
            pendingOnDoneAction = null;
            runOnUiThread(() -> speakAccessibleFeedback(text, action));
        }
    }

    private void speakAccessibleFeedback(String text, Runnable onDoneAction) {
        // Only trigger TTS if user is Blind User
        String role = sessionManager != null ? sessionManager.getUserRole() : "";
        if (!RoleManager.ROLE_BLIND_USER.equals(role)) {
            if (onDoneAction != null) onDoneAction.run();
            return;
        }

        AccessibleMicFeedbackManager.triggerShortVibration(this);
        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String langCode = LanguageManager.normalizeLanguageCode(userLang);
        String spokenText = getLocalizedCallText(text, langCode);

        if (tts == null || !isTtsInitialized) {
            Log.d(TAG, "TTS not ready yet in VoiceCallActivity, queuing pending feedback: " + spokenText);
            pendingSpokenText = text;
            pendingOnDoneAction = onDoneAction;
            Toast.makeText(this, spokenText, Toast.LENGTH_SHORT).show();
            return;
        }

        Locale locale = LanguageManager.getLocale(langCode);
        tts.setLanguage(locale);

        // Ensure STREAM_MUSIC volume is audible on device
        try {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                int maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (currentVol < (int)(maxVol * 0.7f)) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, (int)(maxVol * 0.85f), 0);
                }
            }
        } catch (Exception ignored) {}

        final String utteranceId = "CALL_TTS_" + System.currentTimeMillis();
        if (onDoneAction != null) {
            final AtomicBoolean actionInvoked = new AtomicBoolean(false);
            final Handler mainHandler = new Handler(Looper.getMainLooper());
            final Runnable safeAction = () -> {
                if (actionInvoked.compareAndSet(false, true)) {
                    mainHandler.post(onDoneAction);
                }
            };

            // 3.5-second fallback timeout so activity won't hang if TTS completes slowly
            mainHandler.postDelayed(safeAction, 3500);

            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String id) {
                    Log.d(TAG, "CALL_TTS_STARTED: " + id);
                }

                @Override
                public void onDone(String id) {
                    Log.d(TAG, "CALL_TTS_COMPLETED: " + id);
                    if (utteranceId.equals(id)) {
                        safeAction.run();
                    }
                }

                @Override
                public void onError(String id) {
                    Log.w(TAG, "CALL_TTS_ERROR: " + id);
                    if (utteranceId.equals(id)) {
                        safeAction.run();
                    }
                }
            });
        }

        Log.i(TAG, "Speaking accessible call feedback via Google TTS: '" + spokenText + "'");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle params = new Bundle();
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f);
            params.putString(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
            tts.speak(spokenText, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
        } else {
            HashMap<String, String> map = new HashMap<>();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            map.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "1.0");
            map.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
            tts.speak(spokenText, TextToSpeech.QUEUE_FLUSH, map);
        }
    }

    private String getLocalizedCallText(String text, String langCode) {
        if ("Calling caregiver".equalsIgnoreCase(text)) {
            switch (langCode) {
                case LanguageManager.LANG_MALAYALAM: return "കെയർഗിവറെ വിളിക്കുന്നു";
                case LanguageManager.LANG_HINDI: return "केयरगिवर को कॉल किया जा रहा है";
                case LanguageManager.LANG_KANNADA: default: return "ಕೇರ್ಗಿವರ್ಗೆ ಕರೆ ಮಾಡಲಾಗುತ್ತಿದೆ";
            }
        } else if ("Voice call connected".equalsIgnoreCase(text)) {
            switch (langCode) {
                case LanguageManager.LANG_MALAYALAM: return "വോയ്‌സ് കോൾ ബന്ധിപ്പിച്ചു";
                case LanguageManager.LANG_HINDI: return "वॉइस कॉल कनेक्ट हो गया";
                case LanguageManager.LANG_KANNADA: default: return "ಧ್ವನಿ ಕರೆ ಸಂಪರ್ಕಗೊಂಡಿದೆ";
            }
        } else if ("Voice call ended.".equalsIgnoreCase(text) || "Voice call ended".equalsIgnoreCase(text)) {
            switch (langCode) {
                case LanguageManager.LANG_MALAYALAM: return "വോയ്‌സ് കോൾ അവസാനിച്ചു";
                case LanguageManager.LANG_HINDI: return "वॉइस कॉल समाप्त हो गया";
                case LanguageManager.LANG_KANNADA: default: return "ಧ್ವನಿ ಕರೆ ಕೊನೆಗೊಂಡಿದೆ";
            }
        } else if ("Caregiver rejected the call.".equalsIgnoreCase(text)) {
            switch (langCode) {
                case LanguageManager.LANG_MALAYALAM: return "കെയർഗിവർ കോൾ നിരസിച്ചു";
                case LanguageManager.LANG_HINDI: return "देखभालकर्ता ने कॉल अस्वीकार कर दिया";
                case LanguageManager.LANG_KANNADA: default: return "ಕೇರ್‌ಗಿವರ್ ಕರೆಯನ್ನು ನಿರಾಕರಿಸಿದ್ದಾರೆ";
            }
        } else if ("Voice call failed".equalsIgnoreCase(text)) {
            switch (langCode) {
                case LanguageManager.LANG_MALAYALAM: return "വോയ്‌സ് കോൾ പരാജയപ്പെട്ടു";
                case LanguageManager.LANG_HINDI: return "वॉइस कॉल विफल रहा";
                case LanguageManager.LANG_KANNADA: default: return "ಧ್ವನಿ ಕರೆ ವಿಫಲವಾಗಿದೆ";
            }
        }
        return text;
    }

    private void updateMuteButtonUI(boolean isMuted) {
        if (btnMuteCall != null) {
            if (isMuted) {
                btnMuteCall.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EF4444")));
                btnMuteCall.setText("🔇");
                if (txtMuteLabel != null) txtMuteLabel.setText("Muted");
            } else {
                btnMuteCall.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#334155")));
                btnMuteCall.setText("🎤");
                if (txtMuteLabel != null) txtMuteLabel.setText("Mute");
            }
        }
    }

    private void updateSpeakerButtonUI(boolean isSpeakerOn) {
        if (btnSpeakerCall != null) {
            if (isSpeakerOn) {
                btnSpeakerCall.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0284C7")));
                if (txtSpeakerLabel != null) txtSpeakerLabel.setText("Speaker ON");
            } else {
                btnSpeakerCall.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#334155")));
                if (txtSpeakerLabel != null) txtSpeakerLabel.setText("Earpiece");
            }
        }
    }

    private void startTimer() {
        if (isTimerRunning) return;
        isTimerRunning = true;
        callSeconds = 0;
        if (txtCallTimer != null) txtCallTimer.setVisibility(View.VISIBLE);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTimerRunning) {
                    callSeconds++;
                    int mins = callSeconds / 60;
                    int secs = callSeconds % 60;
                    if (txtCallTimer != null) {
                        txtCallTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
                    }
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        isTimerRunning = false;
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    @Override
    public void onCallStateChanged(VoiceCallManager.CallState state, String message) {
        if (txtCallStatus != null) {
            txtCallStatus.setText(message);
        }

        switch (state) {
            case CONNECTED:
                startTimer();
                speakAccessibleFeedback("Voice call connected", null);
                break;
            case REJECTED:
                stopTimer();
                if (!isEndingCall) {
                    isEndingCall = true;
                    speakAccessibleFeedback("Caregiver rejected the call.", this::finish);
                }
                break;
            case ENDED:
            case MISSED:
            case FAILED:
                stopTimer();
                if (!isEndingCall) {
                    isEndingCall = true;
                    speakAccessibleFeedback("Voice call ended.", this::finish);
                }
                break;
        }
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        if (!isEndingCall) {
            isEndingCall = true;
            speakAccessibleFeedback("Voice call failed", this::finish);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {}
            tts = null;
        }
    }
}
