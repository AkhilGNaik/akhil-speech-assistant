package com.kannada.speechassistant;

import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import android.Manifest;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import com.kannada.speechassistant.voiceassistant.VoiceIntent;
import com.kannada.speechassistant.voiceassistant.VoiceIntentMatcher;
import com.kannada.speechassistant.voiceassistant.VoiceIntentType;
import com.kannada.speechassistant.voiceassistant.VoiceLanguageConfig;
import com.kannada.speechassistant.voiceassistant.WakeWordManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * Controller for Caregiver Incoming Call Screen.
 * Displays incoming caller identity, plays ringing sound & vibration,
 * and handles Accept and Reject actions.
 */
public class IncomingCallActivity extends AppCompatActivity {

    private static final String TAG = "IncomingCallActivity";
    private static WeakReference<IncomingCallActivity> activeInstance;

    public static IncomingCallActivity getActiveInstance() {
        return activeInstance != null ? activeInstance.get() : null;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase));
    }

    private TextView txtIncomingCallerName;
    private TextView txtIncomingCallerRole;
    private MaterialButton btnAcceptCall;
    private MaterialButton btnRejectCall;

    private VoiceCallManager voiceCallManager;
    private SessionManager sessionManager;
    private ListenerRegistration callDocListener;

    private String callId;
    private String callerUid;
    private String callerName;
    private String callerRole;
    private String calleeUid;
    private String currentUid;
    private String currentRole;
    private String currentName;

    private Ringtone ringtone;
    private Vibrator vibrator;

    // Hands-free Voice Assistant for Blind User
    private TextToSpeech incomingTts;
    private boolean isTtsReady = false;
    private boolean hasAnnouncedCaller = false;
    private SpeechRecognizer incomingSpeechRecognizer;
    private boolean isListeningVoice = false;
    private boolean pendingManualAccept = false;
    private final Handler voiceLoopHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activeInstance = new WeakReference<>(this);

        // Turn screen on & show over lock screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        setContentView(R.layout.activity_incoming_call);

        txtIncomingCallerName = findViewById(R.id.txtIncomingCallerName);
        txtIncomingCallerRole = findViewById(R.id.txtIncomingCallerRole);
        btnAcceptCall = findViewById(R.id.btnAcceptCall);
        btnRejectCall = findViewById(R.id.btnRejectCall);

        sessionManager = new SessionManager(this);
        voiceCallManager = VoiceCallManager.getInstance(this);

        HashMap<String, String> userDetails = sessionManager.getUserDetails();
        currentUid = userDetails.get(SessionManager.KEY_USER_UID);
        currentRole = sessionManager.getUserRole();
        String currentEmail = userDetails.get(SessionManager.KEY_USER_EMAIL);
        if (currentEmail != null && !currentEmail.trim().isEmpty()) {
            currentName = currentEmail.split("@")[0];
        } else {
            currentName = (currentRole != null && !currentRole.trim().isEmpty()) ? currentRole : "User";
        }

        if (currentUid != null && !currentUid.trim().isEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(currentUid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String fetchedName = documentSnapshot.getString("name");
                            if (fetchedName != null && !fetchedName.trim().isEmpty()) {
                                currentName = fetchedName;
                            }
                        }
                    });
        }

        Intent intent = getIntent();
        callId = intent.getStringExtra("callId");
        callerUid = intent.getStringExtra("callerUid");
        callerName = intent.getStringExtra("callerName");
        callerRole = intent.getStringExtra("callerRole");
        calleeUid = intent.getStringExtra("calleeUid");

        resolveAndDisplayCallerIdentity();

        startRingingSoundAndVibration();
        listenToCallState();

        if (isBlindUser()) {
            initTtsAndVoiceForBlindUser();
        }

        btnAcceptCall.setOnClickListener(v -> {
            stopVoiceListening();
            stopTts();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                pendingManualAccept = true;
                Log.d(TAG, "Requesting RECORD_AUDIO permission before accepting call");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_RECORD_AUDIO);
                return;
            }
            proceedWithCallAccept();
        });

        btnRejectCall.setOnClickListener(v -> {
            stopVoiceListening();
            stopTts();
            cancelIncomingCallNotification();
            stopRingingSoundAndVibration();
            removeCallDocListener();
            voiceCallManager.rejectCall(callId);
            finish();
        });
    }

    private boolean isCallerNameValid(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        String trimmed = name.trim();
        if ("Blind User".equalsIgnoreCase(trimmed) || "Caller".equalsIgnoreCase(trimmed)) {
            return false;
        }
        if (currentName != null && trimmed.equalsIgnoreCase(currentName.trim())) {
            return false;
        }
        return true;
    }

    private void resolveAndDisplayCallerIdentity() {
        // 1. Caller Role Display
        if (callerRole != null && !callerRole.trim().isEmpty()) {
            txtIncomingCallerRole.setText(callerRole + " is calling you...");
        } else {
            txtIncomingCallerRole.setText("Incoming voice call...");
        }

        // 2. Caller Name Resolution
        // Step 1: Initial display with valid call-data name or placeholder while loading from Firestore
        if (isCallerNameValid(callerName)) {
            txtIncomingCallerName.setText(callerName);
        } else {
            txtIncomingCallerName.setText("Incoming Call...");
        }

        // Step 2: Retrieve caller profile information from Firestore users/{callerUid}
        if (callerUid != null && !callerUid.trim().isEmpty()) {
            fetchCallerProfileFromFirestore(callerUid);
        } else if (callId != null && !callId.trim().isEmpty()) {
            FirebaseFirestore.getInstance().collection("voice_calls").document(callId)
                    .get()
                    .addOnSuccessListener(callDoc -> {
                        if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
                            return;
                        }
                        if (callDoc != null && callDoc.exists()) {
                            String docCallerUid = callDoc.getString("callerUid");
                            String docCallerName = callDoc.getString("callerName");
                            if (docCallerUid != null && !docCallerUid.trim().isEmpty()) {
                                callerUid = docCallerUid;
                                fetchCallerProfileFromFirestore(callerUid);
                            } else if (isCallerNameValid(docCallerName)) {
                                callerName = docCallerName;
                                txtIncomingCallerName.setText(callerName);
                            }
                        }
                    });
        }
    }

    private void fetchCallerProfileFromFirestore(String uid) {
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String fetchedName = documentSnapshot.getString("name");
                        String fetchedEmail = documentSnapshot.getString("email");
                        if (fetchedName != null && !fetchedName.trim().isEmpty() && !"Caller".equalsIgnoreCase(fetchedName.trim())) {
                            callerName = fetchedName.trim();
                        } else if (fetchedEmail != null && !fetchedEmail.trim().isEmpty()) {
                            callerName = fetchedEmail.trim();
                        } else if (!isCallerNameValid(callerName)) {
                            callerName = uid;
                        }
                    } else if (!isCallerNameValid(callerName)) {
                        callerName = uid;
                    }
                    txtIncomingCallerName.setText(callerName);
                    if (isBlindUser() && !hasAnnouncedCaller && isTtsReady) {
                        announceIncomingCallToBlindUser();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
                        return;
                    }
                    if (!isCallerNameValid(callerName)) {
                        callerName = uid;
                    }
                    txtIncomingCallerName.setText(callerName);
                    if (isBlindUser() && !hasAnnouncedCaller && isTtsReady) {
                        announceIncomingCallToBlindUser();
                    }
                });
    }

    private static final int REQUEST_CODE_RECORD_AUDIO = 801;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingManualAccept) {
                    pendingManualAccept = false;
                    Log.d(TAG, "RECORD_AUDIO permission granted manually, proceeding to accept call");
                    proceedWithCallAccept();
                } else if (isBlindUser()) {
                    Log.d(TAG, "RECORD_AUDIO permission granted, starting incoming voice listening for blind user");
                    startIncomingVoiceListening();
                } else {
                    Log.d(TAG, "RECORD_AUDIO permission granted, proceeding to accept call");
                    proceedWithCallAccept();
                }
            } else {
                pendingManualAccept = false;
                Log.w(TAG, "RECORD_AUDIO permission was denied");
                Toast.makeText(this, "Microphone permission is required to accept the voice call.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void proceedWithCallAccept() {
        stopVoiceListening();
        stopTts();
        cancelIncomingCallNotification();
        stopRingingSoundAndVibration();
        removeCallDocListener();

        String localAcceptorName = (currentName != null && !currentName.trim().isEmpty())
                ? currentName
                : ((currentRole != null && !currentRole.trim().isEmpty()) ? currentRole : "User");

        voiceCallManager.acceptCall(callId, currentUid, localAcceptorName, currentRole,
                callerUid, callerName, callerRole, null);

        String passRemoteName = (callerName != null && !callerName.trim().isEmpty() &&
                !"Incoming Call...".equals(callerName) && !"Blind User".equalsIgnoreCase(callerName))
                ? callerName : "Caregiver";

        Intent callIntent = new Intent(IncomingCallActivity.this, VoiceCallActivity.class);
        callIntent.putExtra("remoteName", passRemoteName);
        startActivity(callIntent);
        finish();
    }

    public void acceptCallFromVoice() {
        Log.d(TAG, "acceptCallFromVoice executed");
        runOnUiThread(() -> {
            stopVoiceListening();
            stopTts();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                pendingManualAccept = true;
                Log.d(TAG, "Requesting RECORD_AUDIO permission before accepting call");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_RECORD_AUDIO);
                return;
            }
            proceedWithCallAccept();
        });
    }

    public void rejectCallFromVoice() {
        Log.d(TAG, "rejectCallFromVoice executed");
        runOnUiThread(() -> {
            stopVoiceListening();
            stopTts();
            cancelIncomingCallNotification();
            stopRingingSoundAndVibration();
            removeCallDocListener();
            voiceCallManager.rejectCall(callId);
            finish();
        });
    }

    private boolean isBlindUser() {
        if (RoleManager.ROLE_BLIND_USER.equalsIgnoreCase(currentRole)) {
            return true;
        }
        if (sessionManager != null) {
            String role = sessionManager.getUserRole();
            return RoleManager.ROLE_BLIND_USER.equalsIgnoreCase(role);
        }
        return false;
    }

    private void initTtsAndVoiceForBlindUser() {
        incomingTts = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true;
                String lang = sessionManager != null ? sessionManager.getLanguage() : "kn";
                Locale locale = VoiceLanguageConfig.getTtsLocale(lang);
                int res = incomingTts.setLanguage(locale);
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    incomingTts.setLanguage(Locale.US);
                }
                announceIncomingCallToBlindUser();
            }
        });
    }

    private synchronized void announceIncomingCallToBlindUser() {
        if (!isBlindUser() || isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
            return;
        }
        if (!isTtsReady || incomingTts == null || hasAnnouncedCaller) {
            return;
        }

        hasAnnouncedCaller = true;
        stopRingingSoundAndVibration();

        String nameToSpeak = (isCallerNameValid(callerName)) ? callerName : "Caregiver";
        String lang = sessionManager != null ? sessionManager.getLanguage() : "kn";
        String announcement;
        if (VoiceLanguageConfig.LANG_HINDI.equals(lang)) {
            announcement = nameToSpeak + " की कॉल आ रही है। कॉल उठाने के लिए 'कॉल उठाओ' बोलें।";
        } else if (VoiceLanguageConfig.LANG_MALAYALAM.equals(lang)) {
            announcement = nameToSpeak + " ൽ നിന്നുള്ള കോൾ. സ്വീകരിക്കാൻ 'കോൾ സ്വീകരിക്കുക' എന്ന് പറയുക.";
        } else if (VoiceLanguageConfig.LANG_ENGLISH.equals(lang)) {
            announcement = "Incoming call from " + nameToSpeak + ". Say 'accept call' or 'receive call' to answer.";
        } else {
            announcement = nameToSpeak + " ಅವರಿಂದ ಕರೆ ಬರುತ್ತಿದೆ. ಸ್ವೀಕರಿಸಲು 'ಕರೆ ಸ್ವೀಕರಿಸಿ' ಎಂದು ಹೇಳಿ.";
        }

        incomingTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) {}
            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        startIncomingVoiceListening();
                    }
                });
            }
            @Override
            public void onError(String utteranceId) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        startIncomingVoiceListening();
                    }
                });
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            incomingTts.speak(announcement, TextToSpeech.QUEUE_FLUSH, null, "INCOMING_CALL_ANNOUNCEMENT");
        } else {
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "INCOMING_CALL_ANNOUNCEMENT");
            incomingTts.speak(announcement, TextToSpeech.QUEUE_FLUSH, params);
        }
    }

    private void startIncomingVoiceListening() {
        if (!isBlindUser() || isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_RECORD_AUDIO);
            return;
        }

        try {
            WakeWordManager.getInstance(this).pauseListening();
        } catch (Throwable ignored) {}

        try {
            if (incomingSpeechRecognizer != null) {
                try {
                    incomingSpeechRecognizer.destroy();
                } catch (Throwable ignored) {}
            }
            incomingSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            String lang = sessionManager != null ? sessionManager.getLanguage() : "kn";
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, VoiceLanguageConfig.getSpeechRecognizerLanguageTag(lang));

            incomingSpeechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    isListeningVoice = true;
                    Log.d(TAG, "Incoming call voice recognition listening ready");
                }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override
                public void onEndOfSpeech() {
                    isListeningVoice = false;
                }
                @Override
                public void onError(int error) {
                    isListeningVoice = false;
                    Log.d(TAG, "Incoming call speech error: " + error);
                    scheduleVoiceListeningRetry();
                }
                @Override
                public void onResults(Bundle results) {
                    isListeningVoice = false;
                    handleRecognizedSpeech(results);
                }
                @Override
                public void onPartialResults(Bundle partialResults) {
                    handleRecognizedSpeech(partialResults);
                }
                @Override public void onEvent(int eventType, Bundle params) {}
            });

            incomingSpeechRecognizer.startListening(recognizerIntent);
            isListeningVoice = true;
        } catch (Throwable e) {
            Log.e(TAG, "Error starting incoming call voice listening: " + e.getMessage(), e);
            scheduleVoiceListeningRetry();
        }
    }

    private void handleRecognizedSpeech(Bundle results) {
        if (results == null) return;
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches == null || matches.isEmpty()) return;

        String lang = sessionManager != null ? sessionManager.getLanguage() : "kn";
        for (String match : matches) {
            if (match == null || match.trim().isEmpty()) continue;
            Log.d(TAG, "Recognized incoming call speech candidate: " + match);
            VoiceIntent intent = VoiceIntentMatcher.matchIntent(match, lang);
            if (intent.getIntentType() == VoiceIntentType.ACCEPT_CALL
                    || intent.getIntentType() == VoiceIntentType.CONFIRM_YES) {
                acceptCallFromVoice();
                return;
            } else if (intent.getIntentType() == VoiceIntentType.END_CALL
                    || intent.getIntentType() == VoiceIntentType.CONFIRM_NO) {
                rejectCallFromVoice();
                return;
            }
        }
        scheduleVoiceListeningRetry();
    }

    private void scheduleVoiceListeningRetry() {
        if (!isBlindUser() || isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
            return;
        }
        voiceLoopHandler.removeCallbacksAndMessages(null);
        voiceLoopHandler.postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                startRingingSoundAndVibration();
                voiceLoopHandler.postDelayed(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        stopRingingSoundAndVibration();
                        startIncomingVoiceListening();
                    }
                }, 2500);
            }
        }, 1000);
    }

    private void stopVoiceListening() {
        isListeningVoice = false;
        if (voiceLoopHandler != null) {
            voiceLoopHandler.removeCallbacksAndMessages(null);
        }
        if (incomingSpeechRecognizer != null) {
            try {
                incomingSpeechRecognizer.stopListening();
                incomingSpeechRecognizer.cancel();
                incomingSpeechRecognizer.destroy();
            } catch (Throwable ignored) {}
            incomingSpeechRecognizer = null;
        }
    }

    private void stopTts() {
        if (incomingTts != null) {
            try {
                incomingTts.stop();
                incomingTts.shutdown();
            } catch (Throwable ignored) {}
            incomingTts = null;
        }
    }

    private void cancelIncomingCallNotification() {
        if (callId != null) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.cancel(callId.hashCode());
            }
        }
    }

    private void startRingingSoundAndVibration() {
        try {
            Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (alert == null) {
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), alert);
            if (ringtone != null) {
                ringtone.play();
            }

            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 1000, 1000};
                vibrator.vibrate(pattern, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting ringtone/vibration: " + e.getMessage(), e);
        }
    }

    private void stopRingingSoundAndVibration() {
        try {
            if (ringtone != null && ringtone.isPlaying()) {
                ringtone.stop();
                ringtone = null;
            }
            if (vibrator != null) {
                vibrator.cancel();
                vibrator = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping ringtone: " + e.getMessage(), e);
        }
    }

    private void listenToCallState() {
        if (callId == null) return;
        callDocListener = FirebaseFirestore.getInstance().collection("voice_calls").document(callId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) return;
                    String status = snapshot.getString("status");
                    if ("CANCELLED".equals(status) || "ENDED".equals(status) || "MISSED".equals(status) || "REJECTED".equals(status)) {
                        Log.d(TAG, "Call was ended or cancelled remotely: " + status);
                        stopVoiceListening();
                        stopTts();
                        cancelIncomingCallNotification();
                        stopRingingSoundAndVibration();
                        removeCallDocListener();
                        finish();
                    }
                });
    }

    private void removeCallDocListener() {
        if (callDocListener != null) {
            callDocListener.remove();
            callDocListener = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopVoiceListening();
        stopTts();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activeInstance != null && activeInstance.get() == this) {
            activeInstance = null;
        }
        stopVoiceListening();
        stopTts();
        cancelIncomingCallNotification();
        stopRingingSoundAndVibration();
        removeCallDocListener();
    }
}
