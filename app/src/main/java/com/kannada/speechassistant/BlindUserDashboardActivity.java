package com.kannada.speechassistant;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.kannada.speechassistant.voiceassistant.WakeWordManager;

/**
 * Controller class for the Blind User Dashboard.
 * Includes Caregiver Connection functionality identical to reference user modules,
 * alongside accessible Voice Messaging on the Home tab.
 */
public class BlindUserDashboardActivity extends AppCompatActivity {

    private static final String TAG = "BlindUserDashboard";
    private static final int REQUEST_CODE_PICK_RINGTONE = 505;
    private static final int REQUEST_CODE_RECORD_AUDIO = 606;
    private static final int REQUEST_CODE_CALL_RECORD_AUDIO = 707;
    private static final int REQUEST_CODE_STARTUP_PERMISSIONS = 808;
    private static final int MAX_RECORDING_SECONDS = 60;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase));
    }

    // System Utilities
    private SessionManager sessionManager;
    private ExecutorService networkExecutor;
    private ListenerRegistration connectionsListener;
    private ListenerRegistration caregiverPresenceListener;

    // Persistent Settings
    private String coquiUrl;
    private String caregiverEmail;
    private String emergencyMessage;

    // View Components (Top Toolbar Header)
    private TextView txtUserRole;
    private LinearLayout layoutStatus;
    private View viewStatusDot;
    private TextView txtStatusLabel;
    private ImageButton btnBellNotificationSettings;
    private TextView txtActiveRingtoneDialogName;
    private MaterialButton btnDialogSilentToggle;

    // Navigation and Container Layouts
    private BottomNavigationView bottomNav;
    private View layoutHome;
    private ScrollView layoutProfile;
    private View layoutCaregiver;

    // Profile Page Widgets
    private TextView txtProfileRole;
    private TextView txtProfileEmail;
    private TextView txtProfileUid;
    private EditText editCoquiUrl;
    private EditText editCaregiverEmail;
    private EditText editEmergencyMessage;
    private MaterialButton btnSaveProfileSettings;
    private MaterialButton btnProfileLogout;

    // Caregiver Page Widgets
    private EditText editSearchEmail;
    private MaterialButton btnSearch;
    private FrameLayout loadingOverlay;
    private MaterialCardView cardSearchResult;
    private MaterialCardView cardSearchSection;
    private TextView txtResultName;
    private TextView txtResultEmail;
    private TextView txtResultRole;
    private MaterialButton btnSendRequest;
    private MaterialCardView cardConnectedUser;
    private TextView txtConnectedTitle;
    private TextView txtConnectedName;
    private TextView txtConnectedEmail;
    private TextView txtConnectedRole;
    private MaterialButton btnDisconnect;
    private MaterialButton btnOpenFullChat;
    private TextView txtPendingSectionTitle;
    private LinearLayout layoutReceivedRequestsList;

    // Home Voice Message Widgets
    private MaterialCardView cardHomeCaregiverStatus;
    private TextView txtHomeCaregiverName;
    private MaterialButton btnHomeConnectCaregiver;
    private MaterialButton btnVoiceCallCaregiver;
    private MaterialButton btnMicVoiceMessage;
    private TextView txtMicStatus;
    private LinearLayout layoutRecordingTimer;
    private TextView txtTimerDisplay;
    private LinearLayout layoutRecordingActions;
    private MaterialButton btnStopRecording;
    private MaterialButton btnCancelRecording;

    // Home Emergency SOS Physical Trigger
    private MaterialCardView cardEmergency;
    private MaterialButton btnSOS;
    private long lastEmergencyTriggerTime = 0;

    // Home Caregiver Chat Box Widgets
    private CaregiverChatAdapter caregiverChatAdapter;
    private RecyclerView rvCaregiverChatMessages;
    private LinearLayout layoutCaregiverChatEmptyState;
    private ListenerRegistration caregiverChatListener;
    private final Set<String> autoPlayedVoiceMsgIds = new HashSet<>();

    // Auth & Identity State
    private String currentUid;
    private String currentEmail;
    private String currentName = "";
    private String currentRole;

    private String foundUid;
    private String foundEmail;
    private String foundName;
    private String foundRole;
    private FirebaseFirestore db;

    private String connectedCaregiverUid = null;
    private String connectedCaregiverName = null;
    private boolean hasPendingRequest = false;

    // TextToSpeech for Accessible Feedback
    private TextToSpeech tts;
    private boolean isTtsInitialized = false;

    // Audio Recorder Engine State
    private MediaRecorder mediaRecorder;
    private File tempAudioFile;
    private boolean isRecording = false;
    private boolean isUploading = false;
    private boolean isHandsFreeRecordingMode = false;
    private int recordingSeconds = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    public boolean isVoiceRecordingActive() {
        return isRecording;
    }

    public boolean isCaregiverConnected() {
        if (connectedCaregiverUid != null && !connectedCaregiverUid.trim().isEmpty() && !"mock_caregiver_uid".equals(connectedCaregiverUid)) {
            return true;
        }
        if (caregiverEmail != null && !caregiverEmail.trim().isEmpty()) {
            return true;
        }
        SharedPreferences settingsPref = getSharedPreferences("BlindUserSettings", MODE_PRIVATE);
        String savedUid = settingsPref.getString("connectedCaregiverUid", null);
        if (savedUid != null && !savedUid.trim().isEmpty() && !"mock_caregiver_uid".equals(savedUid)) {
            return true;
        }
        String savedEmail = settingsPref.getString("caregiverEmail", null);
        if (savedEmail != null && !savedEmail.trim().isEmpty()) {
            return true;
        }
        try {
            List<Map<String, Object>> localConns = LocalConnectionSimulator.getConnections(this);
            for (Map<String, Object> conn : localConns) {
                if ("Accepted".equalsIgnoreCase((String) conn.get("status"))) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public String getConnectedCaregiverUid() {
        return connectedCaregiverUid;
    }

    private final java.util.ArrayDeque<Integer> tabHistory = new java.util.ArrayDeque<>();
    private boolean isNavigatingHistory = false;

    public boolean canNavigateBack() {
        return !tabHistory.isEmpty() || (bottomNav != null && bottomNav.getSelectedItemId() != R.id.nav_home);
    }

    public boolean navigateBack() {
        if (!tabHistory.isEmpty()) {
            int prevTab = tabHistory.pop();
            isNavigatingHistory = true;
            try {
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(prevTab);
                }
                updateTabVisibility(prevTab);
                return true;
            } finally {
                isNavigatingHistory = false;
            }
        } else if (bottomNav != null && bottomNav.getSelectedItemId() != R.id.nav_home) {
            isNavigatingHistory = true;
            try {
                bottomNav.setSelectedItemId(R.id.nav_home);
                updateTabVisibility(R.id.nav_home);
                return true;
            } finally {
                isNavigatingHistory = false;
            }
        }
        return false;
    }

    public void updateTabVisibility(int itemId) {
        if (layoutHome != null) layoutHome.setVisibility(View.GONE);
        if (layoutProfile != null) layoutProfile.setVisibility(View.GONE);
        if (layoutCaregiver != null) layoutCaregiver.setVisibility(View.GONE);
        if (cardEmergency != null) cardEmergency.setVisibility(View.GONE);

        if (itemId == R.id.nav_home) {
            if (layoutHome != null) layoutHome.setVisibility(View.VISIBLE);
            if (cardEmergency != null) cardEmergency.setVisibility(View.VISIBLE);
            updateHomeCaregiverCard();
        } else if (itemId == R.id.nav_profile) {
            if (layoutProfile != null) layoutProfile.setVisibility(View.VISIBLE);
            refreshProfileDetails();
        } else if (itemId == R.id.nav_caregiver) {
            if (layoutCaregiver != null) layoutCaregiver.setVisibility(View.VISIBLE);
            refreshConnectionState();
            CaregiverSoundManager.stopNotificationSound(BlindUserDashboardActivity.this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        networkExecutor = Executors.newSingleThreadExecutor();

        // Security Check: Verify user role permission
        String role = sessionManager.getUserRole();
        if (!RoleManager.checkAccess(this, role, RoleManager.ROLE_BLIND_USER)) {
            finish();
            return;
        }
        SecurityGuard.verifyRole(this, RoleManager.ROLE_BLIND_USER, null);

        setContentView(R.layout.activity_blind_user_dashboard);

        // Initialize TTS for accessible audio feedback
        initTextToSpeech();

        // Bind Base UI Layouts
        txtUserRole = findViewById(R.id.txtUserRole);
        layoutStatus = findViewById(R.id.layoutStatus);
        viewStatusDot = findViewById(R.id.viewStatusDot);
        txtStatusLabel = findViewById(R.id.txtStatusLabel);
        btnBellNotificationSettings = findViewById(R.id.btnBellNotificationSettings);
        if (btnBellNotificationSettings != null) {
            btnBellNotificationSettings.setOnClickListener(v -> showNotificationSettingsDialog());
        }
        bottomNav = findViewById(R.id.bottomNavBlindUser);

        layoutHome = findViewById(R.id.layoutHome);
        layoutProfile = findViewById(R.id.layoutProfile);
        layoutCaregiver = findViewById(R.id.layoutCaregiver);

        // Load Persistent Settings
        loadSettings();

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        if ((currentUid == null || currentUid.isEmpty()) && sessionManager != null) {
            String sessionUid = sessionManager.getUserDetails().get(SessionManager.KEY_USER_UID);
            if (sessionUid != null && !sessionUid.trim().isEmpty()) {
                currentUid = sessionUid.trim();
            }
        }
        if (currentUid == null) {
            currentUid = "";
        }
        currentRole = role;
        currentEmail = sessionManager.getUserDetails().get(SessionManager.KEY_USER_EMAIL);

        if (currentUid != null && !currentUid.isEmpty()) {
            db.collection("users").document(currentUid).update("online", true, "typingTo", null);
        }
        if (txtUserRole != null) {
            txtUserRole.setText(getString(R.string.role_blind_user));
        }

        fetchCurrentUserProfileName();

        // Bind and setup widgets
        setupNavigation();
        setupProfileWidgets();
        setupCaregiverWidgets();
        setupHomeVoiceMessageWidgets();
        setupEmergencyButton();
        startRealTimeSync();

        // Check Online Connectivity status
        checkConnectionStatus();

        // Request Hands-Free Microphone and Notification Permissions if needed
        List<String> requiredPermissions = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.RECORD_AUDIO);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (!requiredPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, requiredPermissions.toArray(new String[0]), REQUEST_CODE_STARTUP_PERMISSIONS);
        }

        // Start Central Real-Time Background Notification Service
        Intent serviceIntent = new Intent(this, FirestoreRealtimeService.class);
        startService(serviceIntent);

        // Connect back navigation to navigate from secondary tabs back to Home tab
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (canNavigateBack()) {
                    navigateBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        handleTargetTabIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleTargetTabIntent(intent);
    }

    public void handleTargetTabIntent(@Nullable Intent intent) {
        if (intent == null || bottomNav == null) return;
        String targetTab = intent.getStringExtra("EXTRA_TARGET_TAB");
        if ("home".equalsIgnoreCase(targetTab)) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        } else if ("profile".equalsIgnoreCase(targetTab)) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
        } else if ("caregiver".equalsIgnoreCase(targetTab)) {
            openCaregiverTab();
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
                    Log.i("BlindVoiceMessage", "TTS initialized with Google TTS engine successfully.");
                } else {
                    Log.e("BlindVoiceMessage", "TTS initialization failed with code: " + status);
                }
            }, "com.google.android.tts");
        } catch (Exception e) {
            tts = new TextToSpeech(getApplicationContext(), status -> {
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true;
                    String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
                    Locale locale = LanguageManager.getLocale(userLang);
                    tts.setLanguage(locale);
                }
            });
        }
    }

    @Nullable
    public TextToSpeech getTts() {
        return (isTtsInitialized && tts != null) ? tts : null;
    }

    private void speakAccessibleFeedback(String enText, Runnable onDoneAction) {
        AccessibleMicFeedbackManager.triggerShortVibration(this);

        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String langCode = LanguageManager.normalizeLanguageCode(userLang);
        String spokenText;
        if (enText != null && (enText.contains("ಧ್ವನಿ") || enText.contains("ಕಳುಹಿಸಿದ್ದೇನೆ") || enText.contains("ಸಂದೇಶ"))) {
            spokenText = enText;
        } else {
            spokenText = getLocalizedFeedbackText(enText, langCode);
        }

        if (tts != null && isTtsInitialized) {
            Locale locale = LanguageManager.getLocale(langCode);
            tts.setLanguage(locale);

            // Ensure media volume is sufficiently audible for blind user
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

            final String utteranceId = "BLIND_VOICE_MSG_" + System.currentTimeMillis();
            if (onDoneAction != null) {
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId1) {
                        Log.i("BlindVoiceMessage", "TTS feedback started: " + utteranceId1);
                    }
                    @Override
                    public void onDone(String utteranceId1) {
                        Log.i("BlindVoiceMessage", "TTS feedback completed: " + utteranceId1);
                        if (utteranceId.equals(utteranceId1)) {
                            new Handler(Looper.getMainLooper()).post(onDoneAction);
                        }
                    }
                    @Override
                    public void onError(String utteranceId1) {
                        Log.w("BlindVoiceMessage", "TTS feedback error: " + utteranceId1);
                        if (utteranceId.equals(utteranceId1)) {
                            new Handler(Looper.getMainLooper()).post(onDoneAction);
                        }
                    }
                });
            }

            Log.i("BlindVoiceMessage", "Speaking accessible feedback via TTS: '" + spokenText + "'");
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
        } else {
            Toast.makeText(this, spokenText, Toast.LENGTH_SHORT).show();
            if (onDoneAction != null) {
                new Handler(Looper.getMainLooper()).post(onDoneAction);
            }
        }
    }

    private String getLocalizedFeedbackText(String englishText, String langCode) {
        if ("No caregiver is connected. Please connect to a caregiver first.".equals(englishText)) {
            switch (langCode) {
                case LanguageManager.LANG_MALAYALAM: return "കെയർഗിവർ കണക്റ്റ് ചെയ്തിട്ടില്ല. ദയവായി ആദ്യം ഒരു കെയർഗിവറുമായി ബന്ധപ്പെടുക.";
                case LanguageManager.LANG_HINDI: return "कोई देखभालकर्ता कनेक्ट नहीं है। कृपया पहले किसी देखभालकर्ता से कनेक्ट करें।";
                case LanguageManager.LANG_KANNADA: default: return "ಯಾವುದೇ ಕೇರ್‌ಗಿವರ್ ಸಂಪರ್ಕಗೊಂಡಿಲ್ಲ. ದಯವಿಟ್ಟು ಮೊದಲು ಕೇರ್‌ಗಿವರ್‌ಗೆ ಸಂಪರ್ಕಿಸಿ.";
            }
        } else if ("Microphone permission is required to record a voice message.".equals(englishText)) {
            switch (langCode) {
                case LanguageManager.LANG_MALAYALAM: return "വോയ്‌സ് സന്ദേശം റെക്കോർഡ് ചെയ്യാൻ മൈക്രോഫോൺ അനുമതി ആവശ്യമാണ്.";
                case LanguageManager.LANG_HINDI: return "वॉइस मैसेज रिकॉर्ड करने के लिए माइक्रोफ़ोन अनुमति की आवश्यकता है।";
                case LanguageManager.LANG_KANNADA: default: return "ಧ್ವನಿ ಸಂದೇಶ ರೆಕಾರ್ಡ್ ಮಾಡಲು ಮೈಕ್ರೋಫೋನ್ ಅನುಮತಿ ಅಗತ್ಯವಿದೆ.";
            }
        } else if ("Microphone permission was denied. Please enable microphone permission in app settings.".equals(englishText)) {
            switch (langCode) {
                case LanguageManager.LANG_MALAYALAM: return "മൈക്രോഫോൺ അനുമതി നിരസിച്ചു. ആപ്പ് ക്രമീകരണങ്ങളിൽ മൈക്രോഫോൺ അനുമതി പ്രവർത്തനക്ഷമമാക്കുക.";
                case LanguageManager.LANG_HINDI: return "माइक्रोफ़ोन अनुमति अस्वीकृत कर दी गई। कृपया ऐप सेटिंग्स में माइक्रोफ़ोन अनुमति सक्षम करें।";
                case LanguageManager.LANG_KANNADA: default: return "ಮೈಕ್ರೋಫೋನ್ ಅನುಮತಿಯನ್ನು ನಿರಾಕರಿಸಲಾಗಿದೆ. ದಯವಿಟ್ಟು ಆ್ಯಪ್ ಸೆಟ್ಟಿಂಗ್‌ಗಳಲ್ಲಿ ಮೈಕ್ರೋಫೋನ್ ಅನುಮತಿಯನ್ನು ಸಕ್ರಿಯಗೊಳಿಸಿ.";
            }
        } else if ("Recording started.".equals(englishText)) {
            switch (langCode) {
                case LanguageManager.LANG_MALAYALAM: return "റെക്കോർഡിംഗ് ആരംഭിച്ചു.";
                case LanguageManager.LANG_HINDI: return "रिकॉर्डिंग शुरू हो गई है।";
                case LanguageManager.LANG_KANNADA: default: return "ರೆಕಾರ್ಡಿಂಗ್ ಪ್ರಾರಂಭವಾಗಿದೆ.";
            }
        } else if ("Recording stopped. Sending message.".equals(englishText)) {
            switch (langCode) {
                case LanguageManager.LANG_MALAYALAM: return "റെക്കോർഡിംഗ് നിർത്തി. സന്ദേശം അയക്കുന്നു.";
                case LanguageManager.LANG_HINDI: return "रिकॉर्डिंग रोक दी गई है। संदेश भेजा जा रहा है।";
                case LanguageManager.LANG_KANNADA: default: return "ರೆಕಾರ್ಡಿಂಗ್ ನಿಲ್ಲಿಸಲಾಗಿದೆ. ಸಂದೇಶವನ್ನು ಕಳುಹಿಸಲಾಗುತ್ತಿದೆ.";
            }
        } else if ("Maximum recording time reached. Sending message.".equals(englishText)) {
            switch (langCode) {
                case LanguageManager.LANG_MALAYALAM: return "പരമാവധി റെക്കോർഡിംഗ് സമയം പൂർത്തിയായി. സന്ദേശം അയക്കുന്നു.";
                case LanguageManager.LANG_HINDI: return "अधिकतम रिकॉर्डिंग समय पूरा हो गया। संदेश भेजा जा रहा है।";
                case LanguageManager.LANG_KANNADA: default: return "ಗರಿಷ್ಠ ರೆಕಾರ್ಡಿಂಗ್ ಸಮಯ ತಲುಪಿದೆ. ಸಂದೇಶವನ್ನು ಕಳುಹಿಸಲಾಗುತ್ತಿದೆ.";
            }
        } else if ("Recording cancelled.".equals(englishText)) {
            switch (langCode) {
                case LanguageManager.LANG_MALAYALAM: return "റെക്കോർഡിംഗ് റദ്ദാക്കി.";
                case LanguageManager.LANG_HINDI: return "रिकॉर्डिंग रद्द कर दी गई।";
                case LanguageManager.LANG_KANNADA: default: return "ರೆಕಾರ್ಡಿಂಗ್ ರದ್ದುಗೊಳಿಸಲಾಗಿದೆ.";
            }
        } else if ("Voice message sent.".equals(englishText)) {
            switch (langCode) {
                case LanguageManager.LANG_MALAYALAM: return "വോയ്‌സ് സന്ദേശം അയച്ചു.";
                case LanguageManager.LANG_HINDI: return "वॉइस मैसेज भेज दिया गया है।";
                case LanguageManager.LANG_KANNADA: default: return "ಧ್ವನಿ ಸಂದೇಶ ಕಳುಹಿಸಿದ್ದೇನೆ";
            }
        } else if ("Unable to send voice message. Please try again.".equals(englishText)) {
            switch (langCode) {
                case LanguageManager.LANG_MALAYALAM: return "വോയ്‌സ് സന്ദേശം അയക്കാൻ കഴിഞ്ഞില്ല. ദയവായി വീണ്ടും ശ്രമിക്കുക.";
                case LanguageManager.LANG_HINDI: return "वॉइस मैसेज भेजने में असमर्थ। कृपया फिर से प्रयास करें।";
                case LanguageManager.LANG_KANNADA: default: return "ಧ್ವನಿ ಸಂದೇಶವನ್ನು ಕಳುಹಿಸಲು ಸಾಧ್ಯವಾಗಲಿಲ್ಲ. ದಯವಿಟ್ಟು ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ.";
            }
        }
        return englishText;
    }

    private void loadSettings() {
        SharedPreferences settingsPref = getSharedPreferences("BlindUserSettings", MODE_PRIVATE);
        coquiUrl = settingsPref.getString("coquiUrl", "http://10.0.2.2:5000");
        caregiverEmail = settingsPref.getString("caregiverEmail", "");
        if (connectedCaregiverUid == null || connectedCaregiverUid.trim().isEmpty() || "mock_caregiver_uid".equals(connectedCaregiverUid)) {
            String savedUid = settingsPref.getString("connectedCaregiverUid", null);
            if (savedUid != null && !savedUid.trim().isEmpty() && !"mock_caregiver_uid".equals(savedUid)) {
                connectedCaregiverUid = savedUid;
            }
        }
        if (connectedCaregiverName == null || connectedCaregiverName.trim().isEmpty()) {
            String savedName = settingsPref.getString("connectedCaregiverName", null);
            if (savedName != null && !savedName.trim().isEmpty()) {
                connectedCaregiverName = savedName;
            }
        }
        
        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String normLang = LanguageManager.normalizeLanguageCode(userLang);
        String defaultSos = LanguageManager.getDefaultSosMessage(normLang);

        emergencyMessage = settingsPref.getString("emergencyMessage", defaultSos);
        if (emergencyMessage == null || emergencyMessage.trim().isEmpty() || LanguageManager.isDefaultSosMessage(emergencyMessage)) {
            emergencyMessage = defaultSos;
        }
    }

    private void saveCaregiverConnectionSettings(String uid, String name, String email) {
        SharedPreferences settingsPref = getSharedPreferences("BlindUserSettings", MODE_PRIVATE);
        SharedPreferences.Editor editor = settingsPref.edit();
        if (uid != null && !uid.trim().isEmpty() && !"mock_caregiver_uid".equals(uid)) {
            editor.putString("connectedCaregiverUid", uid);
        }
        if (name != null && !name.trim().isEmpty()) {
            editor.putString("connectedCaregiverName", name);
        }
        if (email != null && !email.trim().isEmpty()) {
            editor.putString("caregiverEmail", email);
        }
        editor.apply();
    }

    private void checkConnectionStatus() {
        if (currentUid == null || currentUid.isEmpty()) return;
        db.collection("users").document(currentUid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (layoutStatus != null) layoutStatus.setBackgroundResource(R.drawable.bg_status_pill_online);
                if (viewStatusDot != null) viewStatusDot.setBackgroundResource(R.drawable.bg_status_dot_online);
                if (txtStatusLabel != null) {
                    txtStatusLabel.setText(getString(R.string.online));
                    txtStatusLabel.setTextColor(0xFF047857);
                }
            } else {
                if (layoutStatus != null) layoutStatus.setBackgroundResource(R.drawable.bg_status_pill_offline);
                if (viewStatusDot != null) viewStatusDot.setBackgroundResource(R.drawable.bg_status_dot_offline);
                if (txtStatusLabel != null) {
                    txtStatusLabel.setText(getString(R.string.offline));
                    txtStatusLabel.setTextColor(0xFFB91C1C);
                }
            }
        });
    }

    private void setupNavigation() {
        if (bottomNav == null) return;
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            int currentId = bottomNav.getSelectedItemId();

            if (!isNavigatingHistory && currentId != itemId) {
                tabHistory.push(currentId);
            }

            updateTabVisibility(itemId);
            return true;
        });
    }

    public void openCaregiverTab() {
        runOnUiThread(() -> {
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_caregiver);
            }
            if (layoutHome != null) layoutHome.setVisibility(View.GONE);
            if (layoutProfile != null) layoutProfile.setVisibility(View.GONE);
            if (cardEmergency != null) cardEmergency.setVisibility(View.GONE);
            if (layoutCaregiver != null) layoutCaregiver.setVisibility(View.VISIBLE);
            refreshConnectionState();
            CaregiverSoundManager.stopNotificationSound(BlindUserDashboardActivity.this);
        });
    }

    public void openInteractiveChat() {
        runOnUiThread(() -> {
            if (connectedCaregiverUid != null && !connectedCaregiverUid.isEmpty()) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("receiverId", connectedCaregiverUid);
                intent.putExtra("receiverName", connectedCaregiverName != null && !connectedCaregiverName.isEmpty() ? connectedCaregiverName : (caregiverEmail != null && caregiverEmail.contains("@") ? caregiverEmail.split("@")[0] : "Caregiver"));
                intent.putExtra("receiverRole", RoleManager.ROLE_ADMIN_CAREGIVER);
                intent.putExtra("receiverEmail", caregiverEmail != null ? caregiverEmail : "");
                intent.putExtra("isBlindUser", true);
                startActivity(intent);
            } else if (btnOpenFullChat != null && btnOpenFullChat.getVisibility() == View.VISIBLE) {
                btnOpenFullChat.performClick();
            } else {
                openCaregiverTab();
                speakAccessibleFeedback("No caregiver is connected. Please connect to a caregiver first.", null);
            }
        });
    }

    private void setupProfileWidgets() {
        txtProfileRole = findViewById(R.id.txtProfileRole);
        txtProfileEmail = findViewById(R.id.txtProfileEmail);
        txtProfileUid = findViewById(R.id.txtProfileUid);
        editCoquiUrl = findViewById(R.id.editCoquiUrl);
        editCaregiverEmail = findViewById(R.id.editCaregiverEmail);
        editEmergencyMessage = findViewById(R.id.editEmergencyMessage);
        btnSaveProfileSettings = findViewById(R.id.btnSaveProfileSettings);
        btnProfileLogout = findViewById(R.id.btnProfileLogout);

        if (btnSaveProfileSettings != null) {
            btnSaveProfileSettings.setOnClickListener(v -> {
                if (editCaregiverEmail == null || editEmergencyMessage == null) return;
                String url = (editCoquiUrl != null && editCoquiUrl.getText() != null && !editCoquiUrl.getText().toString().trim().isEmpty())
                        ? editCoquiUrl.getText().toString().trim()
                        : (coquiUrl != null && !coquiUrl.isEmpty() ? coquiUrl : "http://10.0.2.2:5000");
                String email = editCaregiverEmail.getText() != null ? editCaregiverEmail.getText().toString().trim() : "";
                String sosMsg = editEmergencyMessage.getText() != null ? editEmergencyMessage.getText().toString().trim() : "";

                if (email.isEmpty() || sosMsg.isEmpty()) {
                    Toast.makeText(this, "Please fill out all configuration settings.", Toast.LENGTH_SHORT).show();
                    return;
                }

                saveSettings(url, email, sosMsg);
            });
        }

        if (btnProfileLogout != null) {
            btnProfileLogout.setOnClickListener(v -> performLogout());
        }
    }

    private void setupCaregiverWidgets() {
        editSearchEmail = findViewById(R.id.editSearchEmail);
        btnSearch = findViewById(R.id.btnSearch);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        cardSearchSection = findViewById(R.id.cardSearchSection);
        cardSearchResult = findViewById(R.id.cardSearchResult);
        txtResultName = findViewById(R.id.txtResultName);
        txtResultEmail = findViewById(R.id.txtResultEmail);
        txtResultRole = findViewById(R.id.txtResultRole);
        btnSendRequest = findViewById(R.id.btnSendRequest);

        cardConnectedUser = findViewById(R.id.cardConnectedUser);
        txtConnectedTitle = findViewById(R.id.txtConnectedTitle);
        txtConnectedName = findViewById(R.id.txtConnectedName);
        txtConnectedEmail = findViewById(R.id.txtConnectedEmail);
        txtConnectedRole = findViewById(R.id.txtConnectedRole);
        btnDisconnect = findViewById(R.id.btnDisconnect);

        txtPendingSectionTitle = findViewById(R.id.txtPendingSectionTitle);
        layoutReceivedRequestsList = findViewById(R.id.layoutReceivedRequestsList);
        
        btnOpenFullChat = findViewById(R.id.btnOpenFullChat);

        if (btnSearch != null) btnSearch.setOnClickListener(v -> searchUserByEmail());
        if (btnSendRequest != null) btnSendRequest.setOnClickListener(v -> sendConnectionRequest());
    }

    private void setupHomeVoiceMessageWidgets() {
        cardHomeCaregiverStatus = findViewById(R.id.cardHomeCaregiverStatus);
        txtHomeCaregiverName = findViewById(R.id.txtHomeCaregiverName);
        btnHomeConnectCaregiver = findViewById(R.id.btnHomeConnectCaregiver);
        btnVoiceCallCaregiver = findViewById(R.id.btnVoiceCallCaregiver);

        btnMicVoiceMessage = findViewById(R.id.btnMicVoiceMessage);
        txtMicStatus = findViewById(R.id.txtMicStatus);
        layoutRecordingTimer = findViewById(R.id.layoutRecordingTimer);
        txtTimerDisplay = findViewById(R.id.txtTimerDisplay);
        layoutRecordingActions = findViewById(R.id.layoutRecordingActions);
        btnStopRecording = findViewById(R.id.btnStopRecording);
        btnCancelRecording = findViewById(R.id.btnCancelRecording);

        if (btnHomeConnectCaregiver != null) {
            btnHomeConnectCaregiver.setOnClickListener(v -> {
                if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_caregiver);
            });
        }

        if (btnVoiceCallCaregiver != null) {
            btnVoiceCallCaregiver.setOnClickListener(v -> onStartVoiceCallClicked());
        }

        if (btnMicVoiceMessage != null) {
            btnMicVoiceMessage.setOnClickListener(v -> onMicButtonClicked());
        }

        if (btnStopRecording != null) {
            btnStopRecording.setOnClickListener(v -> stopAndSendVoiceRecording(false));
        }

        if (btnCancelRecording != null) {
            btnCancelRecording.setOnClickListener(v -> cancelVoiceRecording());
        }

        com.google.android.material.button.MaterialButton btnVoiceAssistant = findViewById(R.id.btnVoiceAssistant);
        if (btnVoiceAssistant != null) {
            btnVoiceAssistant.setOnClickListener(v -> {
                WakeWordManager.getInstance(BlindUserDashboardActivity.this).pauseListening();

                com.kannada.speechassistant.voiceassistant.AppVoiceAssistant assistant =
                        com.kannada.speechassistant.voiceassistant.AppVoiceAssistant.getInstance(BlindUserDashboardActivity.this);
                if (assistant.isListening()) {
                    assistant.stopListening();
                    btnVoiceAssistant.setText("🎤 Voice Assistant");
                    WakeWordManager.getInstance(BlindUserDashboardActivity.this).resumeListening(BlindUserDashboardActivity.this);
                    return;
                }
                if (com.kannada.speechassistant.voiceassistant.AppVoiceAssistant.isVoiceCallActive(BlindUserDashboardActivity.this)) {
                    speakAccessibleFeedback(com.kannada.speechassistant.voiceassistant.VoiceCommandConstants.MESSAGE_CONFLICT_CALL, null);
                    WakeWordManager.getInstance(BlindUserDashboardActivity.this).resumeListening(BlindUserDashboardActivity.this);
                    return;
                }
                if (isRecording || com.kannada.speechassistant.voiceassistant.AppVoiceAssistant.isVoiceRecordingActive(BlindUserDashboardActivity.this)) {
                    speakAccessibleFeedback(com.kannada.speechassistant.voiceassistant.VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING, null);
                    WakeWordManager.getInstance(BlindUserDashboardActivity.this).resumeListening(BlindUserDashboardActivity.this);
                    return;
                }
                if (assistant.isMicrophoneInUse(BlindUserDashboardActivity.this)) {
                    speakAccessibleFeedback(com.kannada.speechassistant.voiceassistant.VoiceCommandConstants.MESSAGE_CONFLICT_RECORDING, null);
                    WakeWordManager.getInstance(BlindUserDashboardActivity.this).resumeListening(BlindUserDashboardActivity.this);
                    return;
                }
                if (androidx.core.content.ContextCompat.checkSelfPermission(BlindUserDashboardActivity.this,
                        android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    speakAccessibleFeedback("Microphone permission is required for Voice Assistant.", () -> {
                        androidx.core.app.ActivityCompat.requestPermissions(BlindUserDashboardActivity.this,
                                new String[]{android.Manifest.permission.RECORD_AUDIO},
                                com.kannada.speechassistant.voiceassistant.AppVoiceAssistant.REQUEST_CODE_VOICE_ASSISTANT_PERMISSION);
                    });
                    return;
                }
                assistant.startListeningFlow(BlindUserDashboardActivity.this, btnVoiceAssistant, new com.kannada.speechassistant.voiceassistant.VoiceAssistantCallback() {
                    @Override
                    public void onAssistantReady(@NonNull String userRole, @NonNull String languageCode) {}

                    @Override
                    public void onListeningStarted() {}

                    @Override
                    public void onListeningStopped() {}

                    @Override
                    public void onSpeechRecognized(@NonNull String rawText) {}

                    @Override
                    public void onIntentDetected(@NonNull com.kannada.speechassistant.voiceassistant.VoiceIntent intent) {}

                    @Override
                    public void onCommandResolved(@NonNull com.kannada.speechassistant.voiceassistant.VoiceCommand command) {}

                    @Override
                    public void onResponseSpoken(@NonNull String ttsResponse) {}

                    @Override
                    public void onError(@NonNull String errorMessage, int errorCode) {}
                });
            });
        }

        rvCaregiverChatMessages = findViewById(R.id.rvCaregiverChatMessages);
        layoutCaregiverChatEmptyState = findViewById(R.id.layoutCaregiverChatEmptyState);
        if (rvCaregiverChatMessages != null) {
            rvCaregiverChatMessages.setLayoutManager(new LinearLayoutManager(this));
            caregiverChatAdapter = new CaregiverChatAdapter(currentUid, text -> {
                speakAccessibleFeedback(text, null);
            });
            rvCaregiverChatMessages.setAdapter(caregiverChatAdapter);
        }

        updateHomeCaregiverCard();
    }

    private void updateHomeCaregiverCard() {
        if (txtHomeCaregiverName == null) return;

        if (connectedCaregiverUid != null && !connectedCaregiverUid.isEmpty()) {
            String name = connectedCaregiverName != null && !connectedCaregiverName.isEmpty() ? connectedCaregiverName : caregiverEmail;
            txtHomeCaregiverName.setText("Connected: " + (name != null ? name : "Caregiver"));
            txtHomeCaregiverName.setTextColor(Color.parseColor("#047857"));
            if (btnHomeConnectCaregiver != null) btnHomeConnectCaregiver.setVisibility(View.GONE);
        } else {
            txtHomeCaregiverName.setText("No caregiver connected");
            txtHomeCaregiverName.setTextColor(Color.parseColor("#B91C1C"));
            if (btnHomeConnectCaregiver != null) btnHomeConnectCaregiver.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Binds and configures the physical Emergency SOS button on the Blind User Home screen.
     * Reuses the existing EmergencyActivity and emergency workflow.
     */
    private void setupEmergencyButton() {
        cardEmergency = findViewById(R.id.cardEmergency);
        btnSOS = findViewById(R.id.btnSOS);

        View.OnClickListener emergencyListener = v -> triggerEmergencySOS();

        if (cardEmergency != null) {
            cardEmergency.setOnClickListener(emergencyListener);
        }
        if (btnSOS != null) {
            btnSOS.setOnClickListener(emergencyListener);
        }
    }

    /**
     * Sends an emergency SOS alert directly to the caregiver without leaving the dashboard:
     * 1. Strong emergency haptic vibration.
     * 2. Direct message in caregiver_messages (type = "emergency").
     * 3. Dispatches to emergencies and emergency_alerts collections (triggers caregiver siren).
     */
    public void sendDirectEmergencyToCaregiver() {
        runOnUiThread(() -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastEmergencyTriggerTime < 2000) {
                return; // Prevent duplicate rapid triggers
            }
            lastEmergencyTriggerTime = currentTime;

            // 1. Strong Emergency Vibration
            try {
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 400, 150, 400, 150, 400}, -1));
                    } else {
                        vibrator.vibrate(new long[]{0, 400, 150, 400, 150, 400}, -1);
                    }
                }
            } catch (Exception ignored) {}

            String emergencyText = "ತುರ್ತು ಪರಿಸ್ಥಿತಿ! ಸಹಾಯ ಬೇಕು";
            String myUid = currentUid;
            if (myUid == null || myUid.isEmpty()) {
                if (sessionManager != null) myUid = sessionManager.getUserDetails().get(SessionManager.KEY_USER_UID);
            }
            if (myUid == null || myUid.isEmpty()) {
                com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) myUid = user.getUid();
            }
            String myName = currentName != null && !currentName.isEmpty() ? currentName : "Blind User";
            String myEmail = currentEmail != null && !currentEmail.isEmpty() ? currentEmail : "";

            // 2. Direct message write to caregiver_messages
            String cUid = connectedCaregiverUid;
            if (cUid == null || cUid.isEmpty() || "mock_caregiver_uid".equals(cUid)) {
                SharedPreferences settingsPref = getSharedPreferences("BlindUserSettings", MODE_PRIVATE);
                cUid = settingsPref.getString("connectedCaregiverUid", null);
            }

            if (cUid != null && !cUid.isEmpty() && !"mock_caregiver_uid".equals(cUid) && myUid != null && !myUid.isEmpty()) {
                String cId;
                if (myUid.compareTo(cUid) < 0) {
                    cId = myUid + "_" + cUid;
                } else {
                    cId = cUid + "_" + myUid;
                }
                String rEmail = caregiverEmail != null && !caregiverEmail.isEmpty() ? caregiverEmail : "";

                java.util.Map<String, Object> chatMsgMap = new java.util.HashMap<>();
                chatMsgMap.put("chatId", cId);
                chatMsgMap.put("senderId", myUid);
                chatMsgMap.put("senderUid", myUid);
                chatMsgMap.put("receiverId", cUid);
                chatMsgMap.put("recipientEmail", rEmail.toLowerCase().trim());
                chatMsgMap.put("senderRole", RoleManager.ROLE_BLIND_USER);
                chatMsgMap.put("receiverRole", RoleManager.ROLE_ADMIN_CAREGIVER);
                chatMsgMap.put("message", emergencyText);
                chatMsgMap.put("messageText", emergencyText);
                chatMsgMap.put("language", sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE);
                chatMsgMap.put("messageType", "emergency");
                chatMsgMap.put("type", "emergency");
                chatMsgMap.put("isVoice", false);
                chatMsgMap.put("status", "sent");
                chatMsgMap.put("readStatus", false);
                chatMsgMap.put("delivered", false);
                chatMsgMap.put("seen", false);
                chatMsgMap.put("timestamp", FieldValue.serverTimestamp());

                db.collection("caregiver_messages")
                        .add(chatMsgMap)
                        .addOnSuccessListener(ref -> Log.d("BlindEmergency", "Emergency message sent directly to caregiver_messages"))
                        .addOnFailureListener(e -> Log.e("BlindEmergency", "Failed sending emergency message", e));
            }

            // 3. Dispatch to emergencies collection
            java.util.Map<String, Object> alert = new java.util.HashMap<>();
            alert.put("uid", myUid);
            alert.put("userName", myName);
            alert.put("email", myEmail);
            alert.put("role", RoleManager.ROLE_BLIND_USER);
            alert.put("message", emergencyText);
            alert.put("timestamp", Timestamp.now());
            alert.put("status", "Unresolved");
            db.collection("emergencies").add(alert);

            // 4. Dispatch to emergency_alerts collection (triggers caregiver dashboard siren)
            String newAlertId = db.collection("emergency_alerts").document().getId();
            java.util.Map<String, Object> newAlert = new java.util.HashMap<>();
            newAlert.put("alertId", newAlertId);
            newAlert.put("patientId", myUid);
            newAlert.put("patientName", myName);
            newAlert.put("message", emergencyText);
            newAlert.put("timestamp", Timestamp.now());
            newAlert.put("status", "NEW");
            newAlert.put("acknowledged", false);
            newAlert.put("resolved", false);
            db.collection("emergency_alerts").document(newAlertId).set(newAlert);

            // 5. Dispatch to EmergencyAlerts collection
            String alertId = db.collection("EmergencyAlerts").document().getId();
            java.util.Map<String, Object> fcmAlert = new java.util.HashMap<>();
            fcmAlert.put("alertId", alertId);
            fcmAlert.put("userId", myUid);
            fcmAlert.put("userName", myName);
            fcmAlert.put("message", emergencyText);
            fcmAlert.put("timestamp", Timestamp.now());
            fcmAlert.put("status", "active");
            db.collection("EmergencyAlerts").document(alertId).set(fcmAlert);

            Toast.makeText(this, "🚨 " + emergencyText, Toast.LENGTH_SHORT).show();
        });
    }

    public void triggerEmergencySOS() {
        sendDirectEmergencyToCaregiver();
        speakAccessibleFeedback("ತುರ್ತು ಎಚ್ಚರಿಕೆಯನ್ನು ಕಳುಹಿಸಲಾಗಿದೆ.", null);
    }

    private void onMicButtonClicked() {
        Log.d("BlindVoiceMessage", "VOICE_BUTTON_CLICKED");
        WakeWordManager.getInstance(this).pauseListening();
        com.kannada.speechassistant.voiceassistant.AppVoiceAssistant.getInstance(this).stopListening();

        if (isUploading) {
            Toast.makeText(this, "Voice message is being sent. Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isRecording) {
            stopAndSendVoiceRecording(false);
            return;
        }

        // 1. Verify Connected Caregiver or attempt email lookup fallback
        if (connectedCaregiverUid == null || connectedCaregiverUid.isEmpty()) {
            if (caregiverEmail != null && !caregiverEmail.trim().isEmpty()) {
                Log.d("BlindVoiceMessage", "Resolving connected caregiver UID from email: " + caregiverEmail);
                db.collection("users")
                        .whereEqualTo("email", caregiverEmail.toLowerCase().trim())
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                connectedCaregiverUid = queryDocumentSnapshots.getDocuments().get(0).getId();
                                connectedCaregiverName = queryDocumentSnapshots.getDocuments().get(0).getString("name");
                                Log.d("BlindVoiceMessage", "Resolved CONNECTED_CAREGIVER_UID: " + connectedCaregiverUid);
                                onMicButtonClicked();
                            } else {
                                Log.e("BlindVoiceMessage", "Caregiver email query returned empty");
                                speakAccessibleFeedback("No caregiver is connected. Please connect to a caregiver first.", () -> {
                                    if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_caregiver);
                                });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("BlindVoiceMessage", "Caregiver email lookup failed: " + e.getMessage(), e);
                            speakAccessibleFeedback("No caregiver is connected. Please connect to a caregiver first.", () -> {
                                if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_caregiver);
                            });
                        });
                return;
            }

            speakAccessibleFeedback("No caregiver is connected. Please connect to a caregiver first.", () -> {
                if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_caregiver);
            });
            return;
        }

        // 2. Verify Microphone Permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            speakAccessibleFeedback("Microphone permission is required to record a voice message.", () -> {
                ActivityCompat.requestPermissions(BlindUserDashboardActivity.this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_RECORD_AUDIO);
            });
            return;
        }

        // 3. Permission Granted -> Speak TTS feedback first, then start recording when prompt completes
        speakAccessibleFeedback("Recording started.", this::startVoiceRecording);
    }

    private void startVoiceRecording() {
        try {
            tempAudioFile = new File(getExternalCacheDir(), "blind_voice_" + System.currentTimeMillis() + ".m4a");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mediaRecorder = new MediaRecorder(this);
            } else {
                mediaRecorder = new MediaRecorder();
            }
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(tempAudioFile.getAbsolutePath());

            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            recordingSeconds = 0;

            Log.d("BlindVoiceMessage", "RECORDING_STARTED");

            // UI Update
            if (txtMicStatus != null) {
                txtMicStatus.setText("🔴 Recording...");
                txtMicStatus.setTextColor(Color.parseColor("#EF4444"));
            }
            if (layoutRecordingTimer != null) layoutRecordingTimer.setVisibility(View.VISIBLE);
            if (txtTimerDisplay != null) txtTimerDisplay.setText("00:00");
            if (layoutRecordingActions != null) layoutRecordingActions.setVisibility(View.VISIBLE);

            if (btnMicVoiceMessage != null) {
                btnMicVoiceMessage.setText("⏹");
                btnMicVoiceMessage.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EF4444")));
            }

            // Timer Handler
            timerRunnable = new Runnable() {
                private int silenceCount = 0;
                private boolean hasSpoken = false;

                @Override
                public void run() {
                    if (isRecording) {
                        recordingSeconds++;
                        int mins = recordingSeconds / 60;
                        int secs = recordingSeconds % 60;
                        if (txtTimerDisplay != null) {
                            txtTimerDisplay.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
                        }

                        if (isHandsFreeRecordingMode) {
                            int amp = 0;
                            try {
                                if (mediaRecorder != null) {
                                    amp = mediaRecorder.getMaxAmplitude();
                                }
                            } catch (Exception ignored) {}

                            if (amp > 1500) {
                                hasSpoken = true;
                                silenceCount = 0;
                            } else if (hasSpoken) {
                                silenceCount++;
                            }

                            // Auto-stop after 4 seconds of silence after speech, or 15s max limit
                            if ((hasSpoken && silenceCount >= 4) || recordingSeconds >= 15) {
                                Log.i("BlindVoiceMessage", "Hands-free recording completed (speech=" + hasSpoken + ", silence=" + silenceCount + ", secs=" + recordingSeconds + "). Sending directly.");
                                stopAndSendVoiceRecording(recordingSeconds >= 15);
                                return;
                            }
                        } else {
                            if (recordingSeconds >= MAX_RECORDING_SECONDS) {
                                stopAndSendVoiceRecording(true);
                                return;
                            }
                        }
                        timerHandler.postDelayed(this, 1000);
                    }
                }
            };
            timerHandler.postDelayed(timerRunnable, 1000);

        } catch (Exception e) {
            Log.e("BlindVoiceMessage", "Failed to start audio recording: " + e.getMessage(), e);
            isRecording = false;
            Toast.makeText(this, "Failed to start recorder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            resetMicUI();
        }
    }

    private void stopAndSendVoiceRecording(boolean isMaxLimit) {
        if (!isRecording) return;
        isRecording = false;

        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }
        } catch (Exception e) {
            Log.e("BlindVoiceMessage", "Error stopping media recorder: " + e.getMessage(), e);
        }

        Log.d("BlindVoiceMessage", "RECORDING_STOPPED");

        if (tempAudioFile == null || !tempAudioFile.exists() || tempAudioFile.length() <= 0) {
            long size = (tempAudioFile != null && tempAudioFile.exists()) ? tempAudioFile.length() : 0;
            Log.e("BlindVoiceMessage", "RECORDING_FILE_CREATED: Error - File invalid or 0 bytes! Size: " + size);
            speakAccessibleFeedback("Unable to send voice message. Please try again.", null);
            resetMicUI();
            return;
        }

        Log.d("BlindVoiceMessage", "VOICE_MESSAGE:");
        Log.d("BlindVoiceMessage", "Recording file created");
        Log.d("BlindVoiceMessage", "File size: " + tempAudioFile.length() + " bytes");
        Log.d("BlindVoiceMessage", "RECORDING_FILE_CREATED: Path=" + tempAudioFile.getAbsolutePath() + ", Size=" + tempAudioFile.length() + " bytes, Duration=" + recordingSeconds + "s");

        String prompt = isMaxLimit ? "Maximum recording time reached. Sending message." : "Recording stopped. Sending message.";

        if (txtMicStatus != null) {
            txtMicStatus.setText("📤 Uploading / Sending message...");
            txtMicStatus.setTextColor(Color.parseColor("#4F46E5"));
        }
        if (btnMicVoiceMessage != null) btnMicVoiceMessage.setEnabled(false);
        if (btnStopRecording != null) btnStopRecording.setEnabled(false);
        if (btnCancelRecording != null) btnCancelRecording.setEnabled(false);

        isUploading = true;

        if (!isHandsFreeRecordingMode) {
            speakAccessibleFeedback(prompt, null);
        }
        uploadVoiceMessageToFirebase(tempAudioFile, recordingSeconds);
    }

    private void cancelVoiceRecording() {
        if (!isRecording) return;
        isRecording = false;

        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }
        } catch (Exception e) {
            Log.e("BlindVoiceMessage", "Error stopping media recorder on cancel: " + e.getMessage(), e);
        }

        if (tempAudioFile != null && tempAudioFile.exists()) {
            tempAudioFile.delete();
        }

        Log.d("BlindVoiceMessage", "RECORDING_CANCELLED");
        speakAccessibleFeedback("Recording cancelled.", null);
        resetMicUI();
    }

    public void startHandsFreeRecording() {
        isHandsFreeRecordingMode = true;
        startVoiceRecording();
    }

    public File stopHandsFreeRecording() {
        if (!isRecording) {
            return (tempAudioFile != null && tempAudioFile.exists() && tempAudioFile.length() > 0) ? tempAudioFile : null;
        }
        isRecording = false;

        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }
        } catch (Exception e) {
            Log.e("BlindVoiceMessage", "Error stopping media recorder: " + e.getMessage(), e);
        }

        Log.d("BlindVoiceMessage", "HANDS_FREE_RECORDING_STOPPED");
        if (txtMicStatus != null) {
            txtMicStatus.setText("✓ Recording completed");
            txtMicStatus.setTextColor(Color.parseColor("#059669"));
        }
        return (tempAudioFile != null && tempAudioFile.exists() && tempAudioFile.length() > 0) ? tempAudioFile : null;
    }

    public int getHandsFreeRecordingSeconds() {
        return recordingSeconds > 0 ? recordingSeconds : 1;
    }

    public void cancelHandsFreeRecording() {
        cancelVoiceRecording();
    }

    public void sendHandsFreeVoiceMessage(File audioFile, int durationSeconds) {
        isHandsFreeRecordingMode = true;
        File targetFile = audioFile != null ? audioFile : tempAudioFile;
        int targetDuration = durationSeconds > 0 ? durationSeconds : (recordingSeconds > 0 ? recordingSeconds : 1);
        uploadVoiceMessageToFirebase(targetFile, targetDuration);
    }

    private void uploadVoiceMessageToFirebase(File audioFile, int durationSeconds) {
        Log.d("BlindVoiceMessage", "CURRENT_USER_UID: " + currentUid);
        Log.d("BlindVoiceMessage", "CONNECTED_CAREGIVER_UID: " + connectedCaregiverUid);

        if (currentUid == null || currentUid.isEmpty()) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        }

        if (audioFile == null || !audioFile.exists() || audioFile.length() <= 0) {
            isUploading = false;
            Log.e("BlindVoiceMessage", "Upload aborted: audio file missing or 0 bytes");
            speakAccessibleFeedback("Unable to send voice message. Please try again.", null);
            resetMicUI();
            return;
        }

        if (connectedCaregiverUid == null || connectedCaregiverUid.isEmpty()) {
            if (caregiverEmail != null && !caregiverEmail.trim().isEmpty()) {
                Log.d("BlindVoiceMessage", "Resolving connected caregiver UID from email before upload: " + caregiverEmail);
                db.collection("users")
                        .whereEqualTo("email", caregiverEmail.toLowerCase().trim())
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                connectedCaregiverUid = queryDocumentSnapshots.getDocuments().get(0).getId();
                                connectedCaregiverName = queryDocumentSnapshots.getDocuments().get(0).getString("name");
                                Log.d("BlindVoiceMessage", "Resolved CONNECTED_CAREGIVER_UID: " + connectedCaregiverUid);
                                uploadVoiceMessageToFirebase(audioFile, durationSeconds);
                            } else {
                                isUploading = false;
                                Log.e("BlindVoiceMessage", "Upload aborted: Caregiver email query returned empty");
                                speakAccessibleFeedback("No caregiver is connected. Please connect to a caregiver first.", null);
                                resetMicUI();
                            }
                        })
                        .addOnFailureListener(e -> {
                            isUploading = false;
                            Log.e("BlindVoiceMessage", "Upload aborted: Caregiver email lookup failed: " + e.getMessage(), e);
                            speakAccessibleFeedback("Unable to send voice message. Please try again.", null);
                            resetMicUI();
                        });
                return;
            }

            isUploading = false;
            Log.e("BlindVoiceMessage", "Upload aborted: CONNECTED_CAREGIVER_UID is null/empty");
            speakAccessibleFeedback("No caregiver is connected. Please connect to a caregiver first.", null);
            resetMicUI();
            return;
        }

        String storagePath = "voice_messages/" + currentUid + "/" + System.currentTimeMillis() + ".m4a";
        Log.d("BlindVoiceMessage", "VOICE_MESSAGE: Upload started");
        Log.d("BlindVoiceMessage", "UPLOAD_STARTED: Storage path = " + storagePath);

        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(storagePath);

        storageRef.putFile(Uri.fromFile(audioFile))
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        String downloadUrl = downloadUri.toString();
                        Log.d("BlindVoiceMessage", "VOICE_MESSAGE: Upload successful");
                        Log.d("BlindVoiceMessage", "VOICE_MESSAGE: Download URL obtained");
                        Log.d("BlindVoiceMessage", "DOWNLOAD_URL_RECEIVED: " + downloadUrl);
                        saveVoiceMessageToFirestore(downloadUrl, durationSeconds, audioFile);
                    }).addOnFailureListener(e -> {
                        Log.e("BlindVoiceMessage", "Firebase Storage download URL error: " + e.getMessage(), e);
                        uploadViaDirectStorage(audioFile, durationSeconds, storagePath);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("BlindVoiceMessage", "Firebase Storage upload error: " + e.getMessage(), e);
                    uploadViaDirectStorage(audioFile, durationSeconds, storagePath);
                });
    }

    private void uploadViaDirectStorage(File audioFile, int durationSeconds, String storagePath) {
        if (networkExecutor == null) {
            networkExecutor = Executors.newSingleThreadExecutor();
        }
        networkExecutor.execute(() -> {
            try {
                URL url = new URL("https://kfzdvzeahvhfcbkhxhmn.supabase.co/storage/v1/object/" + storagePath);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);
                String apiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtmemR2emVhaHZoZmNia2h4aG1uIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjY0Nzc5NDAsImV4cCI6MjA4MjA1Mzk0MH0.-UzIibKLFuOAyTeECHLal4wNkX8o73ctAuG8BhcsOYs";
                conn.setRequestProperty("apikey", apiKey);
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "audio/m4a");
                conn.setFixedLengthStreamingMode(audioFile.length());

                try (FileInputStream fis = new FileInputStream(audioFile);
                     OutputStream os = conn.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    String downloadUrl = "https://kfzdvzeahvhfcbkhxhmn.supabase.co/storage/v1/object/public/" + storagePath;
                    Log.d("BlindVoiceMessage", "VOICE_MESSAGE: Upload successful");
                    Log.d("BlindVoiceMessage", "VOICE_MESSAGE: Download URL obtained");
                    Log.d("BlindVoiceMessage", "DOWNLOAD_URL_RECEIVED: " + downloadUrl);
                    runOnUiThread(() -> saveVoiceMessageToFirestore(downloadUrl, durationSeconds, audioFile));
                } else {
                    Log.e("BlindVoiceMessage", "Direct storage upload failed with HTTP code: " + responseCode);
                    runOnUiThread(() -> {
                        isUploading = false;
                        speakAccessibleFeedback("Unable to send voice message. Please try again.", null);
                        resetMicUI();
                    });
                }
            } catch (Exception e) {
                Log.e("BlindVoiceMessage", "Direct storage upload error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    isUploading = false;
                    speakAccessibleFeedback("Unable to send voice message. Please try again.", null);
                    resetMicUI();
                });
            }
        });
    }

    private void saveVoiceMessageToFirestore(String audioUrl, int durationSeconds, File localTempFile) {
        String chatId;
        if (currentUid.compareTo(connectedCaregiverUid) < 0) {
            chatId = currentUid + "_" + connectedCaregiverUid;
        } else {
            chatId = connectedCaregiverUid + "_" + currentUid;
        }

        String rEmail = caregiverEmail != null && !caregiverEmail.isEmpty() ? caregiverEmail : "";

        Map<String, Object> msg = new HashMap<>();
        msg.put("chatId", chatId);
        msg.put("senderId", currentUid);
        msg.put("senderUid", currentUid);
        msg.put("receiverId", connectedCaregiverUid);
        msg.put("recipientEmail", rEmail.toLowerCase().trim());
        msg.put("senderRole", RoleManager.ROLE_BLIND_USER);
        msg.put("receiverRole", RoleManager.ROLE_ADMIN_CAREGIVER);
        msg.put("message", "🎤 Voice Message");
        msg.put("messageText", "🎤 Voice Message");
        msg.put("language", sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE);
        msg.put("messageType", "voice");
        msg.put("type", "voice");
        msg.put("isVoice", true);
        msg.put("audioUrl", audioUrl);
        msg.put("audioDuration", durationSeconds);
        msg.put("status", "sent");
        msg.put("delivered", false);
        msg.put("seen", false);
        msg.put("timestamp", FieldValue.serverTimestamp());

        Log.d("BlindVoiceMessage", "VOICE_MESSAGE: Firestore message write started");
        Log.d("BlindVoiceMessage", "FIRESTORE_MESSAGE_WRITE_STARTED: chatId=" + chatId + ", senderId=" + currentUid + ", receiverId=" + connectedCaregiverUid);

        db.collection("caregiver_messages")
                .add(msg)
                .addOnSuccessListener(documentReference -> {
                    isUploading = false;
                    Log.d("BlindVoiceMessage", "VOICE_MESSAGE: Firestore message write successful");
                    Log.d("BlindVoiceMessage", "FIRESTORE_MESSAGE_WRITE_SUCCESS: docId=" + documentReference.getId());

                    if (localTempFile != null && localTempFile.exists()) {
                        localTempFile.delete();
                    }

                    if (txtMicStatus != null) {
                        txtMicStatus.setText("✓ Voice message sent!");
                        txtMicStatus.setTextColor(Color.parseColor("#059669"));
                    }

                    String feedbackText = com.kannada.speechassistant.voiceassistant.VoiceLanguageConfig.getVoiceMessageSentResponse(
                            sessionManager != null ? sessionManager.getLanguage() : "kn");
                    speakAccessibleFeedback(feedbackText, () -> {
                        timerHandler.postDelayed(this::resetMicUI, 1200);
                    });
                })
                .addOnFailureListener(e -> {
                    isUploading = false;
                    Log.e("BlindVoiceMessage", "FIRESTORE_MESSAGE_WRITE_FAILED: " + e.getMessage(), e);
                    speakAccessibleFeedback("Unable to send voice message. Please try again.", null);
                    resetMicUI();
                });
    }

    public void sendTextMessageToCaregiver(String msg) {
        sendTextMessageToCaregiver(msg, null);
    }

    public void sendTextMessageToCaregiver(String msg, Runnable onComplete) {
        if (msg == null || msg.trim().isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        final String finalMsg = msg.trim();

        if (connectedCaregiverUid == null || connectedCaregiverUid.isEmpty()) {
            if (caregiverEmail != null && !caregiverEmail.trim().isEmpty()) {
                db.collection("users")
                        .whereEqualTo("email", caregiverEmail.toLowerCase().trim())
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                connectedCaregiverUid = queryDocumentSnapshots.getDocuments().get(0).getId();
                                executeFirestoreTextMessageWrite(finalMsg, onComplete);
                            } else {
                                if (onComplete != null) onComplete.run();
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (onComplete != null) onComplete.run();
                        });
                return;
            }
            if (onComplete != null) onComplete.run();
            return;
        }

        executeFirestoreTextMessageWrite(finalMsg, onComplete);
    }

    private void executeFirestoreTextMessageWrite(String msg, Runnable onComplete) {
        String chatId;
        if (currentUid.compareTo(connectedCaregiverUid) < 0) {
            chatId = currentUid + "_" + connectedCaregiverUid;
        } else {
            chatId = connectedCaregiverUid + "_" + currentUid;
        }

        String rEmail = caregiverEmail != null && !caregiverEmail.isEmpty() ? caregiverEmail : "";

        Map<String, Object> chatMsgMap = new HashMap<>();
        chatMsgMap.put("chatId", chatId);
        chatMsgMap.put("senderId", currentUid);
        chatMsgMap.put("senderUid", currentUid);
        chatMsgMap.put("receiverId", connectedCaregiverUid);
        chatMsgMap.put("recipientEmail", rEmail.toLowerCase().trim());
        chatMsgMap.put("senderRole", RoleManager.ROLE_BLIND_USER);
        chatMsgMap.put("receiverRole", RoleManager.ROLE_ADMIN_CAREGIVER);
        chatMsgMap.put("message", msg);
        chatMsgMap.put("messageText", msg);
        chatMsgMap.put("language", sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE);
        chatMsgMap.put("messageType", "text");
        chatMsgMap.put("type", "text");
        chatMsgMap.put("isVoice", false);
        chatMsgMap.put("status", "sent");
        chatMsgMap.put("readStatus", false);
        chatMsgMap.put("delivered", false);
        chatMsgMap.put("seen", false);
        chatMsgMap.put("timestamp", FieldValue.serverTimestamp());

        Log.d("BlindTextMessage", "FIRESTORE_TEXT_MESSAGE_WRITE_STARTED: chatId=" + chatId + ", senderId=" + currentUid + ", receiverId=" + connectedCaregiverUid);

        db.collection("caregiver_messages")
                .add(chatMsgMap)
                .addOnSuccessListener(documentReference -> {
                    Log.d("BlindTextMessage", "FIRESTORE_TEXT_MESSAGE_WRITE_SUCCESS: docId=" + documentReference.getId());
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        documentReference.update("status", "delivered", "delivered", true);
                    }, 1000);
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("BlindTextMessage", "FIRESTORE_TEXT_MESSAGE_WRITE_FAILED: " + e.getMessage(), e);
                    LocalConnectionSimulator.saveLocalMessage(BlindUserDashboardActivity.this, chatId, chatMsgMap);
                    if (onComplete != null) onComplete.run();
                });
    }

    public static boolean isCaregiverTextMessage(@Nullable ChatMessage m, @Nullable String myUid) {
        if (m == null) return false;
        if (m.isVoice()) return false;
        String type = m.getType();
        String messageType = m.getMessageType();
        if ("emergency".equalsIgnoreCase(type) || "emergency".equalsIgnoreCase(messageType) ||
                "system".equalsIgnoreCase(type) || "alert".equalsIgnoreCase(type) ||
                "call".equalsIgnoreCase(type) || "call".equalsIgnoreCase(messageType)) {
            return false;
        }
        if (myUid != null && myUid.equals(m.getSenderId())) {
            return false;
        }
        boolean isCaregiverSender = (myUid != null && !myUid.equals(m.getSenderId())) ||
                RoleManager.ROLE_ADMIN_CAREGIVER.equalsIgnoreCase(m.getSenderRole());
        if (!isCaregiverSender) {
            return false;
        }
        String text = m.getMessageText();
        if (text == null || text.trim().isEmpty()) text = m.getMessage();
        return text != null && !text.trim().isEmpty();
    }

    public int getCaregiverTextMessageCount() {
        if (caregiverChatAdapter == null) return 0;
        List<ChatMessage> list = caregiverChatAdapter.getMessages();
        if (list == null || list.isEmpty()) return 0;

        String myUid = currentUid;
        if (myUid == null || myUid.isEmpty()) {
            if (sessionManager != null) {
                myUid = sessionManager.getUserDetails().get(SessionManager.KEY_USER_UID);
            }
        }

        int count = 0;
        for (ChatMessage m : list) {
            if (isCaregiverTextMessage(m, myUid)) {
                count++;
            }
        }
        return count;
    }

    public static boolean isCaregiverVoiceMessage(@Nullable ChatMessage m, @Nullable String myUid) {
        if (m == null) return false;
        if (!m.isVoice() && (m.getAudioUrl() == null || m.getAudioUrl().trim().isEmpty())) return false;
        if (myUid != null && myUid.equals(m.getSenderId())) {
            return false;
        }
        boolean isCaregiverSender = (myUid != null && !myUid.equals(m.getSenderId())) ||
                RoleManager.ROLE_ADMIN_CAREGIVER.equalsIgnoreCase(m.getSenderRole());
        return isCaregiverSender && m.getAudioUrl() != null && !m.getAudioUrl().trim().isEmpty();
    }

    public static boolean isCaregiverAnyMessage(@Nullable ChatMessage m, @Nullable String myUid) {
        return isCaregiverTextMessage(m, myUid) || isCaregiverVoiceMessage(m, myUid);
    }

    @Nullable
    public ChatMessage getLatestCaregiverMessage() {
        if (caregiverChatAdapter == null) return null;
        List<ChatMessage> list = caregiverChatAdapter.getMessages();
        if (list == null || list.isEmpty()) return null;

        String myUid = currentUid;
        if (myUid == null || myUid.isEmpty()) {
            if (sessionManager != null) {
                myUid = sessionManager.getUserDetails().get(SessionManager.KEY_USER_UID);
            }
        }

        // 1. First priority: Newest unread caregiver message (text OR voice)
        for (int i = list.size() - 1; i >= 0; i--) {
            ChatMessage m = list.get(i);
            if (isCaregiverAnyMessage(m, myUid)) {
                boolean isUnread = !m.isSeen() && !"read".equalsIgnoreCase(m.getStatus());
                if (isUnread) {
                    return m;
                }
            }
        }

        // 2. Second priority: Newest caregiver message chronologically (text OR voice)
        for (int i = list.size() - 1; i >= 0; i--) {
            ChatMessage m = list.get(i);
            if (isCaregiverAnyMessage(m, myUid)) {
                return m;
            }
        }

        return null;
    }

    @Nullable
    public ChatMessage getLatestCaregiverVoiceMessage() {
        if (caregiverChatAdapter == null) return null;
        List<ChatMessage> list = caregiverChatAdapter.getMessages();
        if (list == null || list.isEmpty()) return null;

        String myUid = currentUid;
        if (myUid == null || myUid.isEmpty()) {
            if (sessionManager != null) {
                myUid = sessionManager.getUserDetails().get(SessionManager.KEY_USER_UID);
            }
        }

        for (int i = list.size() - 1; i >= 0; i--) {
            ChatMessage m = list.get(i);
            if (isCaregiverVoiceMessage(m, myUid)) {
                return m;
            }
        }
        return null;
    }

    @Nullable
    public ChatMessage getLatestCaregiverTextMessage() {
        if (caregiverChatAdapter == null) return null;
        List<ChatMessage> list = caregiverChatAdapter.getMessages();
        if (list == null || list.isEmpty()) return null;

        String myUid = currentUid;
        if (myUid == null || myUid.isEmpty()) {
            if (sessionManager != null) {
                myUid = sessionManager.getUserDetails().get(SessionManager.KEY_USER_UID);
            }
        }

        // 1. First priority: Newest unread caregiver text message
        for (int i = list.size() - 1; i >= 0; i--) {
            ChatMessage m = list.get(i);
            if (isCaregiverTextMessage(m, myUid)) {
                boolean isUnread = !m.isSeen() && !"read".equalsIgnoreCase(m.getStatus());
                if (isUnread) {
                    return m;
                }
            }
        }

        // 2. Second priority: Newest caregiver text message chronologically
        for (int i = list.size() - 1; i >= 0; i--) {
            ChatMessage m = list.get(i);
            if (isCaregiverTextMessage(m, myUid)) {
                return m;
            }
        }

        return null;
    }

    private void resetMicUI() {
        isRecording = false;
        isUploading = false;
        isHandsFreeRecordingMode = false;
        recordingSeconds = 0;

        if (btnMicVoiceMessage != null) {
            btnMicVoiceMessage.setText("🎤");
            btnMicVoiceMessage.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4F46E5")));
            btnMicVoiceMessage.setEnabled(true);
        }
        if (txtMicStatus != null) {
            txtMicStatus.setText(getString(R.string.tap_microphone_to_record));
            txtMicStatus.setTextColor(Color.parseColor("#1E1B4B"));
        }
        if (layoutRecordingTimer != null) layoutRecordingTimer.setVisibility(View.GONE);
        if (layoutRecordingActions != null) layoutRecordingActions.setVisibility(View.GONE);
        if (btnStopRecording != null) btnStopRecording.setEnabled(true);
        if (btnCancelRecording != null) btnCancelRecording.setEnabled(true);

        // Notify voice assistant that recording workflow has fully completed to release resources and safely resume wake word
        com.kannada.speechassistant.voiceassistant.AppVoiceAssistant.getInstance(this).onVoiceRecordingWorkflowFinished(this);
    }

    public void startCaregiverVoiceCall() {
        runOnUiThread(() -> {
            if (bottomNav != null && bottomNav.getSelectedItemId() != R.id.nav_home) {
                bottomNav.setSelectedItemId(R.id.nav_home);
                if (layoutProfile != null) layoutProfile.setVisibility(View.GONE);
                if (layoutCaregiver != null) layoutCaregiver.setVisibility(View.GONE);
                if (layoutHome != null) layoutHome.setVisibility(View.VISIBLE);
            }
            onStartVoiceCallClicked();
        });
    }

    private void onStartVoiceCallClicked() {
        Log.d("BlindVoiceCall", "VOICE_CALL_BUTTON_CLICKED");
        WakeWordManager.getInstance(this).pauseListening();
        com.kannada.speechassistant.voiceassistant.AppVoiceAssistant.getInstance(this).stopListening();

        if (VoiceCallManager.getInstance(this).isCallActive()) {
            Toast.makeText(this, "A call is already in progress.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Verify Connected Caregiver or attempt connection resolution fallback
        if (connectedCaregiverUid == null || connectedCaregiverUid.trim().isEmpty() || "mock_caregiver_uid".equals(connectedCaregiverUid)) {
            // First check saved preferences
            SharedPreferences settingsPref = getSharedPreferences("BlindUserSettings", MODE_PRIVATE);
            String savedUid = settingsPref.getString("connectedCaregiverUid", null);
            String savedName = settingsPref.getString("connectedCaregiverName", null);
            String savedEmail = settingsPref.getString("caregiverEmail", null);
            if (savedUid != null && !savedUid.trim().isEmpty() && !"mock_caregiver_uid".equals(savedUid)) {
                connectedCaregiverUid = savedUid;
                if (savedName != null && !savedName.trim().isEmpty()) connectedCaregiverName = savedName;
                if (savedEmail != null && !savedEmail.trim().isEmpty()) caregiverEmail = savedEmail;
                onStartVoiceCallClicked();
                return;
            }

            // Second check LocalConnectionSimulator
            try {
                List<Map<String, Object>> localConns = LocalConnectionSimulator.getConnections(this);
                for (Map<String, Object> conn : localConns) {
                    if ("Accepted".equalsIgnoreCase((String) conn.get("status"))) {
                        String sender = (String) conn.get("senderUid");
                        if (sender == null || sender.isEmpty()) sender = (String) conn.get("patientUid");
                        String receiver = (String) conn.get("receiverUid");
                        if (receiver == null || receiver.isEmpty()) receiver = (String) conn.get("caregiverUid");
                        String senderEmail = (String) conn.get("senderEmail");
                        if (senderEmail == null || senderEmail.isEmpty()) senderEmail = (String) conn.get("patientEmail");
                        String receiverEmail = (String) conn.get("receiverEmail");
                        if (receiverEmail == null || receiverEmail.isEmpty()) receiverEmail = (String) conn.get("caregiverEmail");

                        if ((sender != null && currentUid.equals(sender)) || (receiver != null && currentUid.equals(receiver)) ||
                                (currentEmail != null && (currentEmail.equalsIgnoreCase(senderEmail) || currentEmail.equalsIgnoreCase(receiverEmail)))) {
                            String otherUid = (currentUid.equals(sender) || (currentEmail != null && currentEmail.equalsIgnoreCase(senderEmail))) ? receiver : sender;
                            String otherName = (currentUid.equals(sender) || (currentEmail != null && currentEmail.equalsIgnoreCase(senderEmail))) ? (String) conn.get("receiverName") : (String) conn.get("senderName");
                            String otherEmail = (currentUid.equals(sender) || (currentEmail != null && currentEmail.equalsIgnoreCase(senderEmail))) ? receiverEmail : senderEmail;
                            if (otherName == null && otherEmail != null) otherName = otherEmail.split("@")[0];
                            if (otherName == null) otherName = "Caregiver";

                            if (otherUid != null && !otherUid.trim().isEmpty() && !"mock_caregiver_uid".equals(otherUid)) {
                                connectedCaregiverUid = otherUid;
                                connectedCaregiverName = otherName;
                                caregiverEmail = otherEmail;
                                saveCaregiverConnectionSettings(otherUid, otherName, otherEmail);
                                onStartVoiceCallClicked();
                                return;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}

            // Third check if caregiverEmail is available, resolve from users collection
            String emailToQuery = (caregiverEmail != null && !caregiverEmail.trim().isEmpty()) ? caregiverEmail : savedEmail;
            if (emailToQuery != null && !emailToQuery.trim().isEmpty()) {
                Log.d("BlindVoiceCall", "Resolving connected caregiver UID from email: " + emailToQuery);
                db.collection("users")
                        .whereEqualTo("email", emailToQuery.toLowerCase().trim())
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                connectedCaregiverUid = queryDocumentSnapshots.getDocuments().get(0).getId();
                                connectedCaregiverName = queryDocumentSnapshots.getDocuments().get(0).getString("name");
                                caregiverEmail = emailToQuery;
                                saveCaregiverConnectionSettings(connectedCaregiverUid, connectedCaregiverName, caregiverEmail);
                                Log.d("BlindVoiceCall", "Resolved CONNECTED_CAREGIVER_UID: " + connectedCaregiverUid);
                                onStartVoiceCallClicked();
                            } else {
                                queryCaregiverConnectionsForCall();
                            }
                        })
                        .addOnFailureListener(e -> {
                            queryCaregiverConnectionsForCall();
                        });
                return;
            }

            // Fourth check: query caregiver_connections directly
            queryCaregiverConnectionsForCall();
            return;
        }

        // 2. Verify Microphone Permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            speakAccessibleFeedback("Microphone permission is required to make a voice call.", () -> {
                ActivityCompat.requestPermissions(BlindUserDashboardActivity.this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_CALL_RECORD_AUDIO);
            });
            return;
        }

        // 3. Initiate WebRTC Voice Call
        initiateOutgoingVoiceCall();
    }

    private void queryCaregiverConnectionsForCall() {
        db.collection("caregiver_connections")
                .whereEqualTo("status", "Accepted")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && !snap.isEmpty()) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            String sender = doc.getString("senderUid");
                            if (sender == null || sender.isEmpty()) sender = doc.getString("patientUid");
                            String receiver = doc.getString("receiverUid");
                            if (receiver == null || receiver.isEmpty()) receiver = doc.getString("caregiverUid");
                            String senderEmail = doc.getString("senderEmail");
                            if (senderEmail == null || senderEmail.isEmpty()) senderEmail = doc.getString("patientEmail");
                            String receiverEmail = doc.getString("receiverEmail");
                            if (receiverEmail == null || receiverEmail.isEmpty()) receiverEmail = doc.getString("caregiverEmail");
                            String senderName = doc.getString("senderName");
                            if (senderName == null || senderName.isEmpty()) senderName = doc.getString("patientName");
                            String receiverName = doc.getString("receiverName");
                            if (receiverName == null || receiverName.isEmpty()) receiverName = doc.getString("caregiverName");

                            if ((sender != null && currentUid.equals(sender)) || (receiver != null && currentUid.equals(receiver)) ||
                                    (currentEmail != null && (currentEmail.equalsIgnoreCase(senderEmail) || currentEmail.equalsIgnoreCase(receiverEmail)))) {
                                String otherUid = (currentUid.equals(sender) || (currentEmail != null && currentEmail.equalsIgnoreCase(senderEmail))) ? receiver : sender;
                                String otherName = (currentUid.equals(sender) || (currentEmail != null && currentEmail.equalsIgnoreCase(senderEmail))) ? receiverName : senderName;
                                String otherEmail = (currentUid.equals(sender) || (currentEmail != null && currentEmail.equalsIgnoreCase(senderEmail))) ? receiverEmail : senderEmail;
                                if (otherName == null && otherEmail != null) otherName = otherEmail.split("@")[0];
                                if (otherName == null) otherName = "Caregiver";

                                if (otherUid != null && !otherUid.trim().isEmpty() && !"mock_caregiver_uid".equals(otherUid)) {
                                    connectedCaregiverUid = otherUid;
                                    connectedCaregiverName = otherName;
                                    caregiverEmail = otherEmail;
                                    saveCaregiverConnectionSettings(otherUid, otherName, otherEmail);
                                    onStartVoiceCallClicked();
                                    return;
                                }
                            }
                        }
                    }
                    speakAccessibleFeedback("No caregiver is connected. Please connect to a caregiver first.", () -> {
                        if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_caregiver);
                    });
                })
                .addOnFailureListener(e -> {
                    speakAccessibleFeedback("No caregiver is connected. Please connect to a caregiver first.", () -> {
                        if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_caregiver);
                    });
                });
    }

    private void initiateOutgoingVoiceCall() {
        Log.i("BlindVoiceCall", "VOICE_MIC_OWNER = NONE");
        Log.i("BlindVoiceCall", "VOICE_MIC_OWNER = VOICE_CALL");
        String cName = connectedCaregiverName != null && !connectedCaregiverName.isEmpty() ? connectedCaregiverName : "Caregiver";

        VoiceCallManager.getInstance(this).startCall(
                currentUid,
                currentName != null && !currentName.isEmpty() ? currentName : "Blind User",
                RoleManager.ROLE_BLIND_USER,
                connectedCaregiverUid,
                cName,
                RoleManager.ROLE_ADMIN_CAREGIVER,
                null
        );

        String callId = VoiceCallManager.getInstance(this).getCurrentCallId();
        Log.i("CALL_DEBUG", "callerUid=" + currentUid + ", caregiverUid=" + connectedCaregiverUid + ", callId=" + callId);
        Log.i("VoiceCallSignaling", "CALL_DEBUG: callerUid=" + currentUid + ", caregiverUid=" + connectedCaregiverUid + ", callId=" + callId);

        Intent intent = new Intent(this, VoiceCallActivity.class);
        intent.putExtra("remoteName", cName);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                WakeWordManager.getInstance(this).startListening(this);
                speakAccessibleFeedback("Recording started.", this::startVoiceRecording);
            } else {
                speakAccessibleFeedback("Microphone permission was denied. Please enable microphone permission in app settings.", null);
            }
        } else if (requestCode == REQUEST_CODE_CALL_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initiateOutgoingVoiceCall();
            } else {
                speakAccessibleFeedback("Microphone permission was denied. Please enable microphone permission in app settings.", null);
            }
        } else if (requestCode == com.kannada.speechassistant.voiceassistant.AppVoiceAssistant.REQUEST_CODE_VOICE_ASSISTANT_PERMISSION) {
            com.kannada.speechassistant.voiceassistant.AppVoiceAssistant.getInstance(this)
                    .handlePermissionsResult(this, requestCode, grantResults, findViewById(R.id.btnVoiceAssistant));
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                WakeWordManager.getInstance(this).startListening(this);
            }
        } else if (requestCode == REQUEST_CODE_STARTUP_PERMISSIONS) {
            boolean micGranted = false;
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.RECORD_AUDIO.equals(permissions[i]) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    micGranted = true;
                    break;
                }
            }
            if (micGranted) {
                Log.i(TAG, "Startup RECORD_AUDIO permission granted. Starting WakeWordManager hands-free listening.");
                WakeWordManager.getInstance(this).startListening(this);
            }
        }
    }

    private void refreshProfileDetails() {
        HashMap<String, String> userDetails = sessionManager != null ? sessionManager.getUserDetails() : null;
        String email = userDetails != null ? userDetails.get(SessionManager.KEY_USER_EMAIL) : "";
        String uid = userDetails != null ? userDetails.get(SessionManager.KEY_USER_UID) : "";

        if (txtProfileEmail != null) txtProfileEmail.setText(email != null ? email : "");
        if (txtProfileRole != null) txtProfileRole.setText(getString(R.string.user_role) + ": " + getString(R.string.role_blind_user));
        if (txtProfileUid != null) txtProfileUid.setText("UID: " + (uid != null ? uid : ""));

        if (editCoquiUrl != null) editCoquiUrl.setText(coquiUrl != null ? coquiUrl : "");
        if (editCaregiverEmail != null) {
            if (connectedCaregiverUid != null && !connectedCaregiverUid.isEmpty() && caregiverEmail != null && !caregiverEmail.isEmpty()) {
                editCaregiverEmail.setText(caregiverEmail);
            } else {
                editCaregiverEmail.setText("");
                editCaregiverEmail.setHint(getString(R.string.enter_email_address));
            }
        }
        if (editEmergencyMessage != null) editEmergencyMessage.setText(emergencyMessage != null ? emergencyMessage : "");
    }

    private void saveSettings(String url, String email, String sosMsg) {
        getSharedPreferences("BlindUserSettings", MODE_PRIVATE).edit()
                .putString("coquiUrl", url)
                .putString("caregiverEmail", email)
                .putString("emergencyMessage", sosMsg)
                .apply();
        coquiUrl = url;
        caregiverEmail = email;
        emergencyMessage = sosMsg;
        Toast.makeText(this, "System Settings Saved Successfully!", Toast.LENGTH_SHORT).show();
    }

    private void fetchCurrentUserProfileName() {
        if (currentUid == null || currentUid.isEmpty()) return;
        db.collection("users").document(currentUid).get().addOnSuccessListener(doc -> {
            if (doc != null && doc.exists()) {
                String n = doc.getString("name");
                if (n != null && !n.isEmpty()) {
                    currentName = n;
                }
            }
        });
    }

    // =========================================================================
    // CAREGIVER CONNECTION ENGINE (Identical 1:1 to Reference User Modules)
    // =========================================================================

    private void startRealTimeSync() {
        connectionsListener = db.collection("caregiver_connections")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Connections listener failed: " + error.getMessage());
                        return;
                    }
                    if (value != null) {
                        refreshConnectionState();
                    }
                });
    }

    private void refreshConnectionState() {
        loadActiveConnections();
        loadPendingRequests();
    }

    private void updateSearchCardVisibility() {
        if (cardSearchSection == null) return;
        boolean hasCaregiver = (cardConnectedUser != null && cardConnectedUser.getVisibility() == View.VISIBLE)
                || (connectedCaregiverUid != null && !connectedCaregiverUid.isEmpty())
                || (caregiverEmail != null && !caregiverEmail.trim().isEmpty());
        if (hasCaregiver || hasPendingRequest) {
            cardSearchSection.setVisibility(View.GONE);
        } else {
            cardSearchSection.setVisibility(View.VISIBLE);
        }
    }

    private void loadActiveConnections() {
        db.collection("caregiver_connections")
                .whereEqualTo("status", "Accepted")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            String sender = doc.getString("senderUid");
                            if (sender == null || sender.isEmpty()) sender = doc.getString("patientUid");
                            String receiver = doc.getString("receiverUid");
                            if (receiver == null || receiver.isEmpty()) receiver = doc.getString("caregiverUid");
                            String senderEmail = doc.getString("senderEmail");
                            if (senderEmail == null || senderEmail.isEmpty()) senderEmail = doc.getString("patientEmail");
                            String receiverEmail = doc.getString("receiverEmail");
                            if (receiverEmail == null || receiverEmail.isEmpty()) receiverEmail = doc.getString("caregiverEmail");

                            if (sender != null && receiver != null && (currentUid.equals(sender) || currentUid.equals(receiver) ||
                                    (currentEmail != null && (currentEmail.equalsIgnoreCase(senderEmail) || currentEmail.equalsIgnoreCase(receiverEmail))))) {
                                String docId = doc.getId();
                                String senderName = doc.getString("senderName");
                                if (senderName == null || senderName.isEmpty()) senderName = doc.getString("patientName");
                                String receiverName = doc.getString("receiverName");
                                if (receiverName == null || receiverName.isEmpty()) receiverName = doc.getString("caregiverName");
                                String senderRole = doc.getString("senderRole");
                                if (senderRole == null || senderRole.isEmpty()) senderRole = doc.getString("patientRole");
                                String receiverRole = doc.getString("receiverRole");
                                if (receiverRole == null || receiverRole.isEmpty()) receiverRole = doc.getString("caregiverRole");

                                processActiveCaregiver(docId, sender, receiver, senderEmail, receiverEmail, senderName, receiverName, senderRole, receiverRole);
                                return;
                            }
                        }
                    }

                    // Fallback to LocalConnectionSimulator
                    List<Map<String, Object>> localConns = LocalConnectionSimulator.getConnections(this);
                    for (Map<String, Object> conn : localConns) {
                        String status = (String) conn.get("status");
                        if ("Accepted".equalsIgnoreCase(status)) {
                            String sender = (String) conn.get("senderUid");
                            if (sender == null || sender.isEmpty()) sender = (String) conn.get("patientUid");
                            String receiver = (String) conn.get("receiverUid");
                            if (receiver == null || receiver.isEmpty()) receiver = (String) conn.get("caregiverUid");
                            String senderEmail = (String) conn.get("senderEmail");
                            if (senderEmail == null || senderEmail.isEmpty()) senderEmail = (String) conn.get("patientEmail");
                            String receiverEmail = (String) conn.get("receiverEmail");
                            if (receiverEmail == null || receiverEmail.isEmpty()) receiverEmail = (String) conn.get("caregiverEmail");

                            if (sender != null && receiver != null && (currentUid.equals(sender) || currentUid.equals(receiver) ||
                                    (currentEmail != null && (currentEmail.equalsIgnoreCase(senderEmail) || currentEmail.equalsIgnoreCase(receiverEmail))))) {
                                String docId = (String) conn.get("docId");
                                String senderName = (String) conn.get("senderName");
                                if (senderName == null || senderName.isEmpty()) senderName = (String) conn.get("patientName");
                                String receiverName = (String) conn.get("receiverName");
                                if (receiverName == null || receiverName.isEmpty()) receiverName = (String) conn.get("caregiverName");
                                String senderRole = (String) conn.get("senderRole");
                                if (senderRole == null || senderRole.isEmpty()) senderRole = (String) conn.get("patientRole");
                                String receiverRole = (String) conn.get("receiverRole");
                                if (receiverRole == null || receiverRole.isEmpty()) receiverRole = (String) conn.get("caregiverRole");

                                processActiveCaregiver(docId, sender, receiver, senderEmail, receiverEmail, senderName, receiverName, senderRole, receiverRole);
                                return;
                            }
                        }
                    }

                    // Check if caregiverEmail or connectedCaregiverUid exists from saved settings / profile
                    if ((caregiverEmail != null && !caregiverEmail.trim().isEmpty()) ||
                            (connectedCaregiverUid != null && !connectedCaregiverUid.trim().isEmpty())) {
                        String email = (caregiverEmail != null && !caregiverEmail.trim().isEmpty()) ? caregiverEmail.trim() : "";
                        String name = (connectedCaregiverName != null && !connectedCaregiverName.isEmpty()) ? connectedCaregiverName : (email.contains("@") ? email.split("@")[0] : "Caregiver");
                        bindActiveCaregiver(connectedCaregiverUid, name, email, RoleManager.ROLE_ADMIN_CAREGIVER, "saved_setting");
                        return;
                    }

                    // No active connection found
                    if (cardConnectedUser != null) cardConnectedUser.setVisibility(View.GONE);
                    connectedCaregiverUid = null;
                    connectedCaregiverName = null;
                    caregiverEmail = "";
                    startCaregiverPresenceListener(null);
                    stopCaregiverChatListener();
                    if (editCaregiverEmail != null) {
                        editCaregiverEmail.setText("");
                        editCaregiverEmail.setHint("Not Connected to any Caregiver");
                    }
                    updateSearchCardVisibility();
                    updateHomeCaregiverCard();
                });
    }

    private void processActiveCaregiver(String docId, String senderUid, String receiverUid,
                                        String senderEmail, String receiverEmail,
                                        String senderName, String receiverName,
                                        String senderRole, String receiverRole) {
        String otherUid;
        String otherName;
        String otherEmail;
        String otherRole;

        if (currentUid.equals(senderUid) || (currentEmail != null && currentEmail.equalsIgnoreCase(senderEmail))) {
            otherUid = receiverUid;
            otherName = receiverName != null ? receiverName : (receiverEmail != null ? receiverEmail.split("@")[0] : "Caregiver");
            otherEmail = receiverEmail;
            otherRole = receiverRole != null ? receiverRole : RoleManager.ROLE_ADMIN_CAREGIVER;
        } else {
            otherUid = senderUid;
            otherName = senderName != null ? senderName : (senderEmail != null ? senderEmail.split("@")[0] : "Patient");
            otherEmail = senderEmail;
            otherRole = senderRole != null ? senderRole : RoleManager.ROLE_BLIND_USER;
        }

        if (("mock_caregiver_uid".equals(otherUid) || otherUid == null || otherUid.isEmpty())
                && otherEmail != null && !otherEmail.trim().isEmpty()) {
            final String finalDocId = docId;
            final String finalOtherName = otherName;
            final String finalOtherEmail = otherEmail;
            final String finalOtherRole = otherRole;
            final String fallbackUid = (otherUid != null && !otherUid.isEmpty()) ? otherUid : "mock_caregiver_uid";

            db.collection("users")
                    .whereEqualTo("email", otherEmail.toLowerCase().trim())
                    .get()
                    .addOnCompleteListener(uTask -> {
                        String resolvedUid = fallbackUid;
                        if (uTask.isSuccessful() && uTask.getResult() != null && !uTask.getResult().isEmpty()) {
                            DocumentSnapshot uDoc = uTask.getResult().getDocuments().get(0);
                            String realUid = uDoc.getString("uid");
                            if (realUid == null || realUid.isEmpty()) realUid = uDoc.getId();
                            if (realUid != null && !realUid.isEmpty()) {
                                resolvedUid = realUid;
                                if (finalDocId != null && !finalDocId.isEmpty()) {
                                    LocalConnectionSimulator.updateConnectionUid(BlindUserDashboardActivity.this, finalDocId, realUid);
                                    db.collection("caregiver_connections").document(finalDocId)
                                            .update("caregiverUid", realUid, "receiverUid", realUid);
                                }
                            }
                        }
                        bindActiveCaregiver(resolvedUid, finalOtherName, finalOtherEmail, finalOtherRole, finalDocId);
                    });
        } else {
            bindActiveCaregiver(otherUid, otherName, otherEmail, otherRole, docId);
        }
    }

    private void bindActiveCaregiver(String otherUid, String otherName, String otherEmail, String otherRole, String docId) {
        if (txtConnectedTitle != null) {
            txtConnectedTitle.setText(getString(R.string.connected_caregiver_title));
        }

        if (txtConnectedName != null) txtConnectedName.setText(otherName != null ? otherName : "Caregiver");
        if (txtConnectedEmail != null) txtConnectedEmail.setText(otherEmail != null ? otherEmail : "");
        if (txtConnectedRole != null) {
            String locOtherRole = RoleManager.ROLE_ADMIN_CAREGIVER.equals(otherRole) ? getString(R.string.role_caregiver) : "User";
            txtConnectedRole.setText("Role: " + locOtherRole);
        }

        caregiverEmail = otherEmail != null ? otherEmail : "";
        connectedCaregiverUid = otherUid;
        connectedCaregiverName = otherName;

        saveCaregiverConnectionSettings(connectedCaregiverUid, connectedCaregiverName, caregiverEmail);

        final String finalOtherUid = otherUid;
        final String finalOtherName = otherName;
        final String finalOtherRole = otherRole;
        final String finalOtherEmail = otherEmail;
        if (btnOpenFullChat != null) {
            btnOpenFullChat.setOnClickListener(v -> {
                Intent intent = new Intent(BlindUserDashboardActivity.this, ChatActivity.class);
                intent.putExtra("receiverId", finalOtherUid);
                intent.putExtra("receiverName", finalOtherName);
                intent.putExtra("receiverRole", finalOtherRole);
                intent.putExtra("receiverEmail", finalOtherEmail);
                intent.putExtra("isBlindUser", true);
                startActivity(intent);
            });
        }

        if (btnDisconnect != null) {
            btnDisconnect.setOnClickListener(v -> disconnectUser(docId));
        }

        if (cardConnectedUser != null) cardConnectedUser.setVisibility(View.VISIBLE);
        startCaregiverChatListener(otherUid);
        startCaregiverPresenceListener(otherUid);
        updateSearchCardVisibility();
        updateHomeCaregiverCard();
    }

    private void startCaregiverPresenceListener(String caregiverUid) {
        if (caregiverPresenceListener != null) {
            caregiverPresenceListener.remove();
            caregiverPresenceListener = null;
        }

        if (caregiverUid == null || caregiverUid.isEmpty() || "mock_caregiver_uid".equals(caregiverUid)) {
            return;
        }

        caregiverPresenceListener = db.collection("users").document(caregiverUid)
                .addSnapshotListener((doc, err) -> {
                    if (err != null || doc == null || !doc.exists()) {
                        return;
                    }
                    Boolean online = doc.getBoolean("online");
                    Timestamp lastSeen = doc.getTimestamp("lastSeen");
                    String typingTo = doc.getString("typingTo");
                    TextView txtStatus = findViewById(R.id.txtCaregiverStatus);
                    if (txtStatus != null) {
                        if (online != null && online) {
                            if (currentUid.equals(typingTo)) {
                                txtStatus.setText("Typing...");
                                txtStatus.setTextColor(Color.parseColor("#10B981"));
                            } else {
                                txtStatus.setText("Online");
                                txtStatus.setTextColor(Color.parseColor("#10B981"));
                            }
                        } else {
                            txtStatus.setTextColor(Color.parseColor("#64748B"));
                            if (lastSeen != null) {
                                txtStatus.setText("Last seen " + formatLastSeenTime(lastSeen));
                            } else {
                                txtStatus.setText("Offline");
                            }
                        }
                    }
                });
    }

    private String formatLastSeenTime(Timestamp timestamp) {
        if (timestamp == null) return "Offline";
        Date date = timestamp.toDate();
        Calendar cal = Calendar.getInstance();
        Calendar msgCal = Calendar.getInstance();
        msgCal.setTime(date);

        if (cal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)) {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return "today at " + sdf.format(date);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault());
            return sdf.format(date);
        }
    }

    private void startCaregiverChatListener(String caregiverUid) {
        if (caregiverChatListener != null) {
            caregiverChatListener.remove();
            caregiverChatListener = null;
        }

        if (caregiverUid == null || caregiverUid.isEmpty() || currentUid == null || currentUid.isEmpty()) {
            if (layoutCaregiverChatEmptyState != null) {
                layoutCaregiverChatEmptyState.setVisibility(View.VISIBLE);
            }
            if (rvCaregiverChatMessages != null) {
                rvCaregiverChatMessages.setVisibility(View.GONE);
            }
            return;
        }

        // Generate deterministic chatId
        String chatId;
        if (currentUid.compareTo(caregiverUid) < 0) {
            chatId = currentUid + "_" + caregiverUid;
        } else {
            chatId = caregiverUid + "_" + currentUid;
        }

        Log.d("BlindVoiceMessage", "START_CAREGIVER_CHAT_LISTENER: chatId = " + chatId);

        Query chatQuery = db.collection("caregiver_messages")
                .whereEqualTo("chatId", chatId);

        caregiverChatListener = chatQuery.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("BlindVoiceMessage", "Caregiver chat listener error: " + error.getMessage(), error);
                loadCaregiverLocalMessagesFallback(chatId);
                return;
            }

            if (value != null) {
                LocalConnectionSimulator.clearLocalMessages(this, chatId);
                List<DocumentSnapshot> docs = value.getDocuments();
                List<ChatMessage> chatMsgs = new ArrayList<>();
                for (DocumentSnapshot doc : docs) {
                    chatMsgs.add(new ChatMessage(doc));
                }

                // Merge local offline fallback messages
                List<Map<String, Object>> locals = LocalConnectionSimulator.getLocalMessages(this, chatId);
                for (Map<String, Object> local : locals) {
                    boolean dup = false;
                    for (ChatMessage m : chatMsgs) {
                        if (m.getMessage().equals(local.get("message")) &&
                                m.getTimestamp() != null &&
                                local.get("timestamp") != null &&
                                Math.abs(m.getTimestamp().toDate().getTime() - ((Timestamp) local.get("timestamp")).toDate().getTime()) < 5000) {
                            dup = true;
                            break;
                        }
                    }
                    if (!dup) {
                        chatMsgs.add(new ChatMessage(local));
                    }
                }

                // Sort messages chronologically by timestamp in Java memory
                Collections.sort(chatMsgs, (m1, m2) -> {
                    if (m1.getTimestamp() == null && m2.getTimestamp() == null) return 0;
                    if (m1.getTimestamp() == null) return 1;
                    if (m2.getTimestamp() == null) return -1;
                    return m1.getTimestamp().compareTo(m2.getTimestamp());
                });

                // Auto-play / announce incoming caregiver messages
                boolean isFirstLoad = autoPlayedVoiceMsgIds.isEmpty();
                for (ChatMessage m : chatMsgs) {
                    if (!currentUid.equals(m.getSenderId())) {
                        if (!autoPlayedVoiceMsgIds.contains(m.getId())) {
                            autoPlayedVoiceMsgIds.add(m.getId());
                            if (!isFirstLoad) {
                                CaregiverSoundManager.handleIncomingMessage(this, m.getId(), m.getSenderId(), m.getMessage(), m.getType(), connectedCaregiverName, "Caregiver");
                            }
                        }
                    }
                }

                if (chatMsgs.isEmpty()) {
                    if (layoutCaregiverChatEmptyState != null) {
                        layoutCaregiverChatEmptyState.setVisibility(View.VISIBLE);
                    }
                    if (rvCaregiverChatMessages != null) {
                        rvCaregiverChatMessages.setVisibility(View.GONE);
                    }
                } else {
                    if (layoutCaregiverChatEmptyState != null) {
                        layoutCaregiverChatEmptyState.setVisibility(View.GONE);
                    }
                    if (rvCaregiverChatMessages != null) {
                        rvCaregiverChatMessages.setVisibility(View.VISIBLE);
                    }
                    if (caregiverChatAdapter != null) {
                        caregiverChatAdapter.setMessages(chatMsgs);
                    }
                    if (rvCaregiverChatMessages != null) {
                        rvCaregiverChatMessages.scrollToPosition(chatMsgs.size() - 1);
                    }
                }

                // Mark unread received messages as read
                markMessagesAsRead(docs);
            }
        });
    }

    private void stopCaregiverChatListener() {
        if (caregiverChatListener != null) {
            caregiverChatListener.remove();
            caregiverChatListener = null;
        }
        if (layoutCaregiverChatEmptyState != null) {
            layoutCaregiverChatEmptyState.setVisibility(View.VISIBLE);
        }
        if (rvCaregiverChatMessages != null) {
            rvCaregiverChatMessages.setVisibility(View.GONE);
        }
    }

    private void loadCaregiverLocalMessagesFallback(String chatId) {
        List<Map<String, Object>> locals = LocalConnectionSimulator.getLocalMessages(this, chatId);
        List<ChatMessage> chatMsgs = new ArrayList<>();
        for (Map<String, Object> local : locals) {
            chatMsgs.add(new ChatMessage(local));
        }
        if (chatMsgs.isEmpty()) {
            if (layoutCaregiverChatEmptyState != null) {
                layoutCaregiverChatEmptyState.setVisibility(View.VISIBLE);
            }
            if (rvCaregiverChatMessages != null) {
                rvCaregiverChatMessages.setVisibility(View.GONE);
            }
        } else {
            if (layoutCaregiverChatEmptyState != null) {
                layoutCaregiverChatEmptyState.setVisibility(View.GONE);
            }
            if (rvCaregiverChatMessages != null) {
                rvCaregiverChatMessages.setVisibility(View.VISIBLE);
            }
            if (caregiverChatAdapter != null) {
                caregiverChatAdapter.setMessages(chatMsgs);
                if (rvCaregiverChatMessages != null) {
                    rvCaregiverChatMessages.scrollToPosition(chatMsgs.size() - 1);
                }
            }
        }
    }

    private void markMessagesAsRead(List<DocumentSnapshot> docs) {
        if (docs == null || docs.isEmpty() || currentUid == null || currentUid.isEmpty()) return;
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        boolean hasUpdates = false;

        for (DocumentSnapshot doc : docs) {
            String senderId = doc.getString("senderId");
            String status = doc.getString("status");
            if (senderId != null && !senderId.equals(currentUid) && !"read".equals(status)) {
                batch.update(doc.getReference(), "status", "read");
                batch.update(doc.getReference(), "seen", true);
                hasUpdates = true;
            }
        }

        if (hasUpdates) {
            batch.commit().addOnFailureListener(e -> Log.e(TAG, "Failed to update message status to read", e));
        }
    }

    private void loadPendingRequests() {
        if (layoutReceivedRequestsList != null) layoutReceivedRequestsList.removeAllViews();

        db.collection("caregiver_connections")
                .get()
                .addOnCompleteListener(task -> {
                    List<Map<String, Object>> requestsList = new ArrayList<>();

                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            String status = doc.getString("status");
                            String sender = doc.getString("senderUid");
                            if (sender == null || sender.isEmpty()) sender = doc.getString("patientUid");
                            String receiver = doc.getString("receiverUid");
                            if (receiver == null || receiver.isEmpty()) receiver = doc.getString("caregiverUid");
                            String senderEmail = doc.getString("senderEmail");
                            if (senderEmail == null || senderEmail.isEmpty()) senderEmail = doc.getString("patientEmail");
                            String receiverEmail = doc.getString("receiverEmail");
                            if (receiverEmail == null || receiverEmail.isEmpty()) receiverEmail = doc.getString("caregiverEmail");
                            
                            if ("Pending".equalsIgnoreCase(status) && sender != null && receiver != null && 
                                (currentUid.equals(sender) || currentUid.equals(receiver) ||
                                 (currentEmail != null && (currentEmail.equalsIgnoreCase(senderEmail) || currentEmail.equalsIgnoreCase(receiverEmail))))) {
                                
                                String senderName = doc.getString("senderName");
                                if (senderName == null || senderName.isEmpty()) senderName = doc.getString("patientName");
                                String receiverName = doc.getString("receiverName");
                                if (receiverName == null || receiverName.isEmpty()) receiverName = doc.getString("caregiverName");
                                String senderRole = doc.getString("senderRole");
                                if (senderRole == null || senderRole.isEmpty()) senderRole = doc.getString("patientRole");
                                String receiverRole = doc.getString("receiverRole");
                                if (receiverRole == null || receiverRole.isEmpty()) receiverRole = doc.getString("caregiverRole");
                                
                                boolean exists = false;
                                for (Map<String, Object> existing : requestsList) {
                                    String extSenderEmail = (String) existing.get("senderEmail");
                                    String extReceiverEmail = (String) existing.get("receiverEmail");
                                    if (extSenderEmail != null && extReceiverEmail != null && senderEmail != null && receiverEmail != null) {
                                        if ((extSenderEmail.equalsIgnoreCase(senderEmail) && extReceiverEmail.equalsIgnoreCase(receiverEmail)) ||
                                            (extSenderEmail.equalsIgnoreCase(receiverEmail) && extReceiverEmail.equalsIgnoreCase(senderEmail))) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                }
                                if (!exists) {
                                    Map<String, Object> map = new HashMap<>(doc.getData());
                                    map.put("docId", doc.getId());
                                    map.put("senderUid", sender);
                                    map.put("receiverUid", receiver);
                                    map.put("senderEmail", senderEmail);
                                    map.put("receiverEmail", receiverEmail);
                                    map.put("senderName", senderName);
                                    map.put("receiverName", receiverName);
                                    map.put("senderRole", senderRole);
                                    map.put("receiverRole", receiverRole);
                                    requestsList.add(map);
                                }
                            }
                        }
                    }

                    // Add local simulator pending requests
                    List<Map<String, Object>> local = LocalConnectionSimulator.getConnections(this);
                    for (Map<String, Object> conn : local) {
                        String status = (String) conn.get("status");
                        String sender = conn.containsKey("senderUid") ? (String) conn.get("senderUid") : null;
                        if (sender == null || sender.isEmpty()) sender = (String) conn.get("patientUid");
                        String receiver = conn.containsKey("receiverUid") ? (String) conn.get("receiverUid") : null;
                        if (receiver == null || receiver.isEmpty()) receiver = (String) conn.get("caregiverUid");
                        String senderEmail = conn.containsKey("senderEmail") ? (String) conn.get("senderEmail") : null;
                        if (senderEmail == null || senderEmail.isEmpty()) senderEmail = (String) conn.get("patientEmail");
                        String receiverEmail = conn.containsKey("receiverEmail") ? (String) conn.get("receiverEmail") : null;
                        if (receiverEmail == null || receiverEmail.isEmpty()) receiverEmail = (String) conn.get("caregiverEmail");
                        
                        if ("Pending".equalsIgnoreCase(status) && sender != null && receiver != null && 
                            (currentUid.equals(sender) || currentUid.equals(receiver) ||
                             (currentEmail != null && (currentEmail.equalsIgnoreCase(senderEmail) || currentEmail.equalsIgnoreCase(receiverEmail))))) {
                            
                            String senderName = conn.containsKey("senderName") ? (String) conn.get("senderName") : null;
                            if (senderName == null || senderName.isEmpty()) senderName = (String) conn.get("patientName");
                            String receiverName = conn.containsKey("receiverName") ? (String) conn.get("receiverName") : null;
                            if (receiverName == null || receiverName.isEmpty()) receiverName = (String) conn.get("caregiverName");
                            String senderRole = conn.containsKey("senderRole") ? (String) conn.get("senderRole") : null;
                            if (senderRole == null || senderRole.isEmpty()) senderRole = (String) conn.get("patientRole");
                            String receiverRole = conn.containsKey("receiverRole") ? (String) conn.get("receiverRole") : null;
                            if (receiverRole == null || receiverRole.isEmpty()) receiverRole = (String) conn.get("caregiverRole");
                            
                            boolean exists = false;
                            for (Map<String, Object> existing : requestsList) {
                                String extSenderEmail = (String) existing.get("senderEmail");
                                String extReceiverEmail = (String) existing.get("receiverEmail");
                                if (extSenderEmail != null && extReceiverEmail != null && senderEmail != null && receiverEmail != null) {
                                    if ((extSenderEmail.equalsIgnoreCase(senderEmail) && extReceiverEmail.equalsIgnoreCase(receiverEmail)) ||
                                        (extSenderEmail.equalsIgnoreCase(receiverEmail) && extReceiverEmail.equalsIgnoreCase(senderEmail))) {
                                        exists = true;
                                        break;
                                    }
                                }
                            }
                            if (!exists) {
                                conn.put("senderUid", sender);
                                conn.put("receiverUid", receiver);
                                conn.put("senderEmail", senderEmail);
                                conn.put("receiverEmail", receiverEmail);
                                conn.put("senderName", senderName);
                                conn.put("receiverName", receiverName);
                                conn.put("senderRole", senderRole);
                                conn.put("receiverRole", receiverRole);
                                requestsList.add(conn);
                            }
                        }
                    }

                    if (requestsList.isEmpty()) {
                        hasPendingRequest = false;
                        if (txtPendingSectionTitle != null) txtPendingSectionTitle.setVisibility(View.GONE);
                        updateSearchCardVisibility();
                        return;
                    }

                    hasPendingRequest = true;
                    if (txtPendingSectionTitle != null) txtPendingSectionTitle.setVisibility(View.VISIBLE);

                    for (Map<String, Object> doc : requestsList) {
                        final String docId = (String) doc.get("docId");
                        String sender = (String) doc.get("senderUid");
                        String senderEmail = (String) doc.get("senderEmail");
                        
                        String name, email, role;
                        boolean isOutgoing = currentUid.equals(sender) || (currentEmail != null && currentEmail.equalsIgnoreCase(senderEmail));

                        if (isOutgoing) {
                            name = (String) doc.get("receiverName");
                            email = (String) doc.get("receiverEmail");
                            role = (String) doc.get("receiverRole");
                        } else {
                            name = (String) doc.get("senderName");
                            email = (String) doc.get("senderEmail");
                            role = (String) doc.get("senderRole");
                        }

                        View itemView = getLayoutInflater().inflate(R.layout.item_connection_request, null);
                        TextView txtName = itemView.findViewById(R.id.txtReqName);
                        TextView txtEmail = itemView.findViewById(R.id.txtReqEmail);
                        TextView txtRole = itemView.findViewById(R.id.txtReqRole);
                        MaterialButton btnAccept = itemView.findViewById(R.id.btnAccept);
                        MaterialButton btnReject = itemView.findViewById(R.id.btnReject);

                        txtName.setText(name);
                        txtEmail.setText(email);
                        
                        if (isOutgoing) {
                            txtRole.setText(getString(R.string.status_pending_approval));
                            btnAccept.setVisibility(View.GONE);
                            btnReject.setText(getString(R.string.cancel_request));
                            btnReject.setOnClickListener(v -> cancelOutgoingRequest(docId));
                        } else {
                            String locRole = RoleManager.ROLE_ADMIN_CAREGIVER.equals(role) ? getString(R.string.role_caregiver) : "User";
                            txtRole.setText("Role: " + locRole);
                            btnAccept.setText(getString(R.string.accept_request));
                            btnReject.setText(getString(R.string.reject_request));
                            btnAccept.setOnClickListener(v -> acceptRequest(docId));
                            btnReject.setOnClickListener(v -> rejectRequest(docId));
                        }

                        if (layoutReceivedRequestsList != null) {
                            layoutReceivedRequestsList.addView(itemView);
                        }
                    }
                    updateSearchCardVisibility();
                });
    }

    private void searchUserByEmail() {
        if (editSearchEmail == null) return;
        String searchEmail = editSearchEmail.getText().toString().trim().toLowerCase();
        if (searchEmail.isEmpty()) {
            Toast.makeText(this, "Please enter an email address.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (searchEmail.equalsIgnoreCase(currentEmail)) {
            Toast.makeText(this, "You cannot connect with your own email.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cardSearchResult != null) cardSearchResult.setVisibility(View.GONE);

        db.collection("users")
                .whereEqualTo("email", searchEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean found = false;
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        foundUid = doc.getId();
                        foundEmail = doc.getString("email");
                        foundName = doc.getString("name");
                        foundRole = doc.getString("role");
                        found = true;
                    } else {
                        found = false;
                    }

                    if (found) {
                        // Role Verification Guard
                        if (!RoleManager.ROLE_ADMIN_CAREGIVER.equals(foundRole)) {
                            Toast.makeText(this, "You can only request connection with caregivers.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Display result
                        if (txtResultName != null) txtResultName.setText(foundName != null ? foundName : "User");
                        if (txtResultEmail != null) txtResultEmail.setText(foundEmail);
                        if (txtResultRole != null) txtResultRole.setText("Role: " + (foundRole != null ? foundRole : "Caregiver"));
                        if (cardSearchResult != null) cardSearchResult.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(this, "No user found with this email.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendConnectionRequest() {
        if (connectedCaregiverUid != null && !connectedCaregiverUid.isEmpty()) {
            Toast.makeText(this, "You already have a connected caregiver.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (hasPendingRequest) {
            Toast.makeText(this, "You already have a pending caregiver connection request.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (foundUid == null) return;

        Map<String, Object> connection = new HashMap<>();
        connection.put("senderUid", currentUid);
        connection.put("senderEmail", currentEmail);
        connection.put("senderName", currentName != null && !currentName.isEmpty() ? currentName : "User");
        connection.put("senderRole", currentRole);
        connection.put("receiverUid", foundUid);
        connection.put("receiverEmail", foundEmail);
        connection.put("receiverName", foundName);
        connection.put("receiverRole", foundRole);
        connection.put("status", "Pending");
        connection.put("timestamp", Timestamp.now());

        connection.put("patientUid", currentUid);
        connection.put("patientName", currentName != null && !currentName.isEmpty() ? currentName : "User");
        connection.put("patientEmail", currentEmail);
        connection.put("caregiverUid", foundUid);
        connection.put("caregiverEmail", foundEmail);

        // Save locally instantly
        LocalConnectionSimulator.saveConnection(this, connection);
        if (cardSearchResult != null) cardSearchResult.setVisibility(View.GONE);
        if (editSearchEmail != null) editSearchEmail.setText("");
        Toast.makeText(this, "Connection request sent!", Toast.LENGTH_SHORT).show();
        refreshConnectionState();

        // Run online write in background
        db.collection("caregiver_connections")
                .add(connection)
                .addOnFailureListener(e -> Log.e(TAG, "Background Firestore write failed", e));
    }

    private void acceptRequest(String docId) {
        LocalConnectionSimulator.updateConnectionStatus(this, docId, "Accepted");
        Toast.makeText(this, "Connection request accepted!", Toast.LENGTH_SHORT).show();
        refreshConnectionState();

        db.collection("caregiver_connections").document(docId)
                .update("status", "Accepted")
                .addOnFailureListener(e -> Log.e(TAG, "Background accept failed", e));
    }

    private void rejectRequest(String docId) {
        LocalConnectionSimulator.updateConnectionStatus(this, docId, "Rejected");
        Toast.makeText(this, "Connection request rejected.", Toast.LENGTH_SHORT).show();
        refreshConnectionState();

        db.collection("caregiver_connections").document(docId)
                .update("status", "Rejected")
                .addOnFailureListener(e -> Log.e(TAG, "Background reject failed", e));
    }

    private void cancelOutgoingRequest(String docId) {
        LocalConnectionSimulator.deleteConnection(this, docId);
        Toast.makeText(this, "Connection request cancelled.", Toast.LENGTH_SHORT).show();
        refreshConnectionState();

        db.collection("caregiver_connections").document(docId)
                .delete()
                .addOnFailureListener(e -> Log.e(TAG, "Background cancel failed", e));
    }

    private void disconnectUser(String docId) {
        if (docId != null && !docId.isEmpty() && !"saved_setting".equals(docId)) {
            LocalConnectionSimulator.deleteConnection(this, docId);
            db.collection("caregiver_connections").document(docId)
                    .delete()
                    .addOnFailureListener(e -> Log.e(TAG, "Background disconnect failed", e));
        }

        SharedPreferences settingsPref = getSharedPreferences("BlindUserSettings", MODE_PRIVATE);
        settingsPref.edit()
                .remove("connectedCaregiverUid")
                .remove("connectedCaregiverName")
                .remove("caregiverEmail")
                .apply();

        connectedCaregiverUid = null;
        connectedCaregiverName = null;
        caregiverEmail = "";

        if (cardConnectedUser != null) cardConnectedUser.setVisibility(View.GONE);
        if (editCaregiverEmail != null) {
            editCaregiverEmail.setText("");
            editCaregiverEmail.setHint("Not Connected to any Caregiver");
        }

        startCaregiverPresenceListener(null);
        stopCaregiverChatListener();
        Toast.makeText(this, "Disconnected successfully.", Toast.LENGTH_SHORT).show();
        refreshConnectionState();
    }

    private void showNotificationSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notification_settings, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        txtActiveRingtoneDialogName = dialogView.findViewById(R.id.txtCurrentRingtoneName);
        MaterialButton btnSelectFileRingtone = dialogView.findViewById(R.id.btnSelectFileRingtone);
        MaterialButton btnTestPlayRingtone = dialogView.findViewById(R.id.btnTestPlayRingtone);
        MaterialButton btnResetDefaultRingtone = dialogView.findViewById(R.id.btnResetDefaultRingtone);
        btnDialogSilentToggle = btnResetDefaultRingtone;
        SeekBar sbNotificationVolume = dialogView.findViewById(R.id.sbNotificationVolume);
        TextView txtVolumePercentage = dialogView.findViewById(R.id.txtVolumePercentage);

        MaterialButton btnSaveNotificationSettings = dialogView.findViewById(R.id.btnSaveNotificationSettings);

        txtActiveRingtoneDialogName.setText(CaregiverSoundManager.getActiveRingtoneName(this, currentUid));
        int currentVol = CaregiverSoundManager.getNotificationVolumePercent(this, currentUid);
        sbNotificationVolume.setProgress(currentVol);
        txtVolumePercentage.setText(currentVol + "%");

        boolean isSilent = CaregiverSoundManager.isSilentMode(this, currentUid);
        if (isSilent) {
            btnResetDefaultRingtone.setText("🔔 Enable Sound");
        } else {
            btnResetDefaultRingtone.setText("🔇 Clear (Silent)");
        }

        sbNotificationVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txtVolumePercentage.setText(progress + "%");
                if (fromUser) {
                    CaregiverSoundManager.saveNotificationVolumePercent(BlindUserDashboardActivity.this, currentUid, progress);
                    txtActiveRingtoneDialogName.setText(CaregiverSoundManager.getActiveRingtoneName(BlindUserDashboardActivity.this, currentUid));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                CaregiverSoundManager.saveNotificationVolumePercent(BlindUserDashboardActivity.this, currentUid, seekBar.getProgress());
                txtActiveRingtoneDialogName.setText(CaregiverSoundManager.getActiveRingtoneName(BlindUserDashboardActivity.this, currentUid));
                CaregiverSoundManager.testPlaySound(BlindUserDashboardActivity.this, currentUid);
            }
        });

        btnSelectFileRingtone.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, "Select Notification Sound from File Manager"), REQUEST_CODE_PICK_RINGTONE);
            } catch (Exception e) {
                try {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.setType("audio/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, REQUEST_CODE_PICK_RINGTONE);
                } catch (Exception ex) {
                    Toast.makeText(BlindUserDashboardActivity.this, "File Manager not found on device", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnTestPlayRingtone.setOnClickListener(v -> CaregiverSoundManager.testPlaySound(BlindUserDashboardActivity.this, currentUid));

        btnResetDefaultRingtone.setOnClickListener(v -> {
            CaregiverSoundManager.toggleSilentMode(BlindUserDashboardActivity.this, currentUid);
            boolean nowSilent = CaregiverSoundManager.isSilentMode(BlindUserDashboardActivity.this, currentUid);
            if (nowSilent) {
                btnResetDefaultRingtone.setText("🔔 Enable Sound");
                Toast.makeText(BlindUserDashboardActivity.this, "🔇 Silent Mode ON (Sound OFF, Vibration ON)", Toast.LENGTH_SHORT).show();
            } else {
                btnResetDefaultRingtone.setText("🔇 Clear (Silent)");
                Toast.makeText(BlindUserDashboardActivity.this, "🔔 Notification Sound Enabled", Toast.LENGTH_SHORT).show();
                CaregiverSoundManager.testPlaySound(BlindUserDashboardActivity.this, currentUid);
            }
            txtActiveRingtoneDialogName.setText(CaregiverSoundManager.getActiveRingtoneName(BlindUserDashboardActivity.this, currentUid));
        });

        View layoutTtsVolumeSection = dialogView.findViewById(R.id.layoutTtsVolumeSection);
        SeekBar sbTtsVolume = dialogView.findViewById(R.id.sbTtsVolume);
        TextView txtTtsVolumePercentage = dialogView.findViewById(R.id.txtTtsVolumePercentage);

        if (layoutTtsVolumeSection != null && sbTtsVolume != null && txtTtsVolumePercentage != null) {
            layoutTtsVolumeSection.setVisibility(View.VISIBLE);
            int currentTtsVol = CaregiverSoundManager.getTtsVolumePercent(this, currentUid);
            sbTtsVolume.setProgress(currentTtsVol);
            txtTtsVolumePercentage.setText(currentTtsVol + "%");

            sbTtsVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    txtTtsVolumePercentage.setText(progress + "%");
                    if (fromUser) {
                        CaregiverSoundManager.saveTtsVolumePercent(BlindUserDashboardActivity.this, currentUid, progress);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    CaregiverSoundManager.saveTtsVolumePercent(BlindUserDashboardActivity.this, currentUid, seekBar.getProgress());
                    CaregiverSoundManager.testPlayTts(BlindUserDashboardActivity.this, currentUid);
                }
            });
        }

        btnSaveNotificationSettings.setOnClickListener(v -> {
            CaregiverSoundManager.saveNotificationVolumePercent(BlindUserDashboardActivity.this, currentUid, sbNotificationVolume.getProgress());
            if (sbTtsVolume != null) {
                CaregiverSoundManager.saveTtsVolumePercent(BlindUserDashboardActivity.this, currentUid, sbTtsVolume.getProgress());
            }
            CaregiverSoundManager.stopNotificationSound(BlindUserDashboardActivity.this);
            Toast.makeText(BlindUserDashboardActivity.this, "Notification sound & volume saved successfully!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.setOnDismissListener(d -> CaregiverSoundManager.stopNotificationSound(BlindUserDashboardActivity.this));
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_RINGTONE && resultCode == RESULT_OK && data != null) {
            Uri selectedAudioUri = data.getData();
            if (selectedAudioUri != null) {
                boolean success = CaregiverSoundManager.saveCustomRingtone(this, selectedAudioUri, currentUid);
                if (success) {
                    Toast.makeText(this, "🔔 Custom Notification Ringtone saved from File Manager!", Toast.LENGTH_LONG).show();
                    if (txtActiveRingtoneDialogName != null) {
                        txtActiveRingtoneDialogName.setText(CaregiverSoundManager.getActiveRingtoneName(this, currentUid));
                    }
                    if (btnDialogSilentToggle != null) {
                        btnDialogSilentToggle.setText("🔇 Clear (Silent)");
                    }
                    CaregiverSoundManager.testPlaySound(this, currentUid);
                } else {
                    Toast.makeText(this, "Failed to load audio file from File Manager", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void performLogout() {
        WakeWordManager.getInstance(this).stopListening();
        sessionManager.logoutUser();
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
        RoleManager.redirectToLogin(this);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CaregiverSoundManager.stopNotificationSound(this);
        CaregiverSoundManager.stopEmergencySound(this);
        updateHomeCaregiverCard();

        if (connectedCaregiverUid != null && !connectedCaregiverUid.isEmpty() && caregiverChatListener == null) {
            startCaregiverChatListener(connectedCaregiverUid);
        }

        WakeWordManager.getInstance(this).setForegroundActivity(this);
        WakeWordManager.getInstance(this).startListening(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        WakeWordManager.getInstance(this).pauseListeningForActivity(this);
        ChatAdapter.stopAudioPlayback();
        CaregiverChatAdapter.stopAudioPlayback();
        if (isRecording) {
            cancelVoiceRecording();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WakeWordManager.getInstance(this).stopListening();
        ChatAdapter.stopAudioPlayback();
        CaregiverChatAdapter.stopAudioPlayback();
        if (caregiverChatListener != null) {
            caregiverChatListener.remove();
            caregiverChatListener = null;
        }
        if (isRecording) {
            cancelVoiceRecording();
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (connectionsListener != null) {
            connectionsListener.remove();
        }
        if (caregiverPresenceListener != null) {
            caregiverPresenceListener.remove();
        }
        if (networkExecutor != null) {
            networkExecutor.shutdown();
        }
    }
}
