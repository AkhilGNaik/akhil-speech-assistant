package com.kannada.speechassistant;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import android.Manifest;
import android.content.pm.PackageManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.AnimatorSet;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.os.Vibrator;
import android.os.VibrationEffect;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import com.google.android.material.imageview.ShapeableImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.Color;
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Controller class for the Speech-Impaired User Dashboard.
 * Includes Bottom Navigation for transitioning across Home, Speech, Profile, and Caregiver tabs.
 * Supports Google TTS, Coqui TTS, emergency alarms, and Firestore integrations.
 */
public class SpeechImpairedDashboardActivity extends AppCompatActivity {

    private static final String TAG = "SpeechImpairedDashboard";

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase));
    }

    // System Utilities
    private SessionManager sessionManager;
    private TextToSpeech tts;
    private MediaPlayer mediaPlayer;
    private ExecutorService networkExecutor;
    private boolean isTtsInitialized = false;
    private com.google.firebase.firestore.ListenerRegistration connectionsListener;
    private com.google.firebase.firestore.ListenerRegistration caregiverPresenceListener;

    // Persistent Settings
    private String coquiUrl;
    private String caregiverEmail;
    private String emergencyMessage;
    private boolean useCoquiTts = false;

    // View Components (Top Toolbar)
    private TextView txtUserRole;
    private LinearLayout layoutStatus;
    private View viewStatusDot;
    private TextView txtStatusLabel;
    private android.widget.ImageButton btnBellNotificationSettings;
    private TextView txtActiveRingtoneDialogName;
    private com.google.android.material.button.MaterialButton btnDialogSilentToggle;
    private static final int REQUEST_CODE_PICK_RINGTONE = 505;

    // Navigation and Container Layouts
    private BottomNavigationView bottomNav;
    private android.view.View layoutHome;
    private ScrollView layoutProfile;
    private android.view.View layoutCaregiver;

    // Home Page Widgets (With Dual Keyboard & Suggestion Support)
    private EditText editHomeQuickText;
    private MaterialCardView cardEmergency;

    // Dual Keyboard & Phonetic Suggestions
    private com.google.android.material.button.MaterialButtonToggleGroup toggleKeyboardGroup;
    private com.google.android.material.button.MaterialButton btnToggleEnglish;
    private com.google.android.material.button.MaterialButton btnToggleKannada;
    private View layoutHomeTranslitStatus;
    private ProgressBar progressHomeTranslit;
    private TextView txtHomeTranslitStatus;
    private View layoutKannadaKeyboard;
    private com.google.android.material.button.MaterialButton[] row1Keys = new com.google.android.material.button.MaterialButton[10];
    private com.google.android.material.button.MaterialButton[] row2Keys = new com.google.android.material.button.MaterialButton[10];
    private com.google.android.material.button.MaterialButton[] row3Keys = new com.google.android.material.button.MaterialButton[9];
    private com.google.android.material.button.MaterialButton[] row4Keys = new com.google.android.material.button.MaterialButton[7];
    private com.google.android.material.button.MaterialButton btnKeyboardShift;
    private com.google.android.material.button.MaterialButton btnKeyboardBackspace;
    private com.google.android.material.button.MaterialButton btnKeyboardModeToggle;
    private com.google.android.material.button.MaterialButton btnKeyboardComma;
    private com.google.android.material.button.MaterialButton btnKeyboardSpace;
    private com.google.android.material.button.MaterialButton btnKeyboardPeriod;
    private com.google.android.material.button.MaterialButton btnKeyboardEnter;

    private int keyboardState = 0; // 0 = UNSHIFTED, 1 = SHIFTED, 2 = MATRAS, 3 = SYMBOLS1, 4 = SYMBOLS2
    private com.google.android.material.button.MaterialButton btnHomeClear;
    private com.google.android.material.button.MaterialButton btnHomeSend;
    private LinearLayout layoutHomeRecentPhrases;
    private boolean isTranslitInternalChange = false;
    private long lastSendClickTime = 0;
    private long lastEmergencyTriggerTime = 0;
    
    // Speech-to-Text variables
    private com.google.android.material.imageview.ShapeableImageView btnMicInput;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private boolean isRecording = false;
    private com.kannada.speechassistant.voiceassistant.DeafAssistantResponseManager deafAssistantResponseManager;

    public com.kannada.speechassistant.voiceassistant.DeafAssistantResponseManager getDeafAssistantResponseManager() {
        return deafAssistantResponseManager;
    }

    public boolean isRecordingActive() {
        return isRecording;
    }

    /**
     * Checks if a caregiver is currently connected for the Deaf/Speech-Impaired user.
     * Returns true only when connectedCaregiverUid contains a valid non-empty value.
     */
    public boolean isCaregiverConnected() {
        return connectedCaregiverUid != null && !connectedCaregiverUid.trim().isEmpty();
    }

    public String getConnectedCaregiverUid() {
        return connectedCaregiverUid;
    }

    public String getCaregiverEmail() {
        return caregiverEmail;
    }

    public void sendTextMessageToCaregiver(String msg) {
        sendTextMessageToCaregiver(msg, null);
    }

    public void sendTextMessageToCaregiver(String msg, Runnable onComplete) {
        if (msg == null || msg.trim().isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }
        sendDirectMessageToCaregiver(msg.trim());
        if (onComplete != null) onComplete.run();
    }

    private ObjectAnimator micScaleXAnimator;
    private ObjectAnimator micScaleYAnimator;
    private Handler idleResetHandler = new Handler(Looper.getMainLooper());
    private Runnable idleResetRunnable;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private android.os.Vibrator vibrator;

    // Haptic feedback type constants
    private static final int HAPTIC_MIC_START  = 1; // double-buzz  → recording started
    private static final int HAPTIC_MIC_STOP   = 2; // long single  → recording stopped
    private static final int HAPTIC_SUCCESS    = 3; // triple-short → text recognised
    private static final int HAPTIC_ERROR      = 4; // long harsh   → recognition error

    private final String[] ROW1_UNSHIFTED = {"ಕ", "ಖ", "ಗ", "ಘ", "ಙ", "ಚ", "ಛ", "ಜ", "ಝ", "ಞ"};
    private final String[] ROW2_UNSHIFTED = {"ಟ", "ಠ", "ಡ", "ಢ", "ಣ", "ತ", "ಥ", "ದ", "ಧ", "ನ"};
    private final String[] ROW3_UNSHIFTED = {"ಪ", "ಫ", "ಬ", "ಭ", "ಮ", "ಯ", "ರ", "ಲ", "ವ"};
    private final String[] ROW4_UNSHIFTED = {"ಶ", "ಷ", "ಸ", "ಹ", "ಳ", "ಕ್ಷ", "ಜ್ಞ"};

    private final String[] ROW1_SHIFTED = {"ಅ", "ಆ", "ಇ", "ಈ", "ಉ", "ಊ", "ಋ", "ೠ", "ಌ", "ೡ"};
    private final String[] ROW2_SHIFTED = {"ಎ", "ಏ", "ಐ", "ಒ", "ಓ", "ಔ", "ಅಂ", "ಅಃ", "್", "ಂ"};
    private final String[] ROW3_SHIFTED = {"ಃ", "ಾ", "ಿ", "ೀ", "ು", "ೂ", "ೃ", "ೄ", "ೆ"};
    private final String[] ROW4_SHIFTED = {"ೇ", "ೈ", "ೊ", "ೋ", "ೌ", "ಁ", "ಃ"};

    // Malayalam Keyboard Rows
    private final String[] ROW1_UNSHIFTED_ML = {"ക", "ഖ", "ഗ", "ഘ", "ങ", "ച", "ഛ", "ജ", "ഝ", "ഞ"};
    private final String[] ROW2_UNSHIFTED_ML = {"ട", "ഠ", "ഡ", "ഢ", "ണ", "ത", "ഥ", "ദ", "ധ", "ന"};
    private final String[] ROW3_UNSHIFTED_ML = {"പ", "ഫ", "ബ", "ഭ", "മ", "യ", "ര", "ല", "വ"};
    private final String[] ROW4_UNSHIFTED_ML = {"ശ", "ഷ", "സ", "ഹ", "ള", "ഴ", "റ"};

    private final String[] ROW1_SHIFTED_ML = {"അ", "ആ", "ഇ", "ഈ", "ഉ", "ഊ", "ഋ", "എ", "ഏ", "ഐ"};
    private final String[] ROW2_SHIFTED_ML = {"ഒ", "ഓ", "ഔ", "അം", "അഃ", "്", "ാ", "ി", "ീ", "ു"};
    private final String[] ROW3_SHIFTED_ML = {"ൂ", "ൃ", "െ", "േ", "ൈ", "ൊ", "ോ", "ൗ", "ം"};
    private final String[] ROW4_SHIFTED_ML = {"ഃ", "ൻ", "ർ", "ൽ", "ൾ", "ൿ", "ഃ"};

    // Hindi Keyboard Rows
    private final String[] ROW1_UNSHIFTED_HI = {"क", "ख", "ग", "घ", "ङ", "च", "छ", "ज", "झ", "ञ"};
    private final String[] ROW2_UNSHIFTED_HI = {"ट", "ठ", "ड", "ढ", "ण", "त", "थ", "द", "ध", "न"};
    private final String[] ROW3_UNSHIFTED_HI = {"प", "फ", "ब", "भ", "म", "य", "र", "ल", "व"};
    private final String[] ROW4_UNSHIFTED_HI = {"श", "ष", "स", "ह", "क्ष", "त्र", "ज्ञ"};

    private final String[] ROW1_SHIFTED_HI = {"अ", "आ", "इ", "ई", "उ", "ऊ", "ऋ", "ए", "ऐ", "ओ"};
    private final String[] ROW2_SHIFTED_HI = {"औ", "अं", "अः", "्", "ा", "ि", "ी", "ु", "ू", "ृ"};
    private final String[] ROW3_SHIFTED_HI = {"े", "ै", "ो", "ौ", "ं", "ः", "ँ", "ॅ", "ॉ"};
    private final String[] ROW4_SHIFTED_HI = {"़", "ऽ", "।", "॥", "०", "९", "।"};

    private final String[] ROW1_MATRAS = {"೧", "೨", "೩", "೪", "೫", "೬", "೭", "೮", "೯", "೦"};
    private final String[] ROW2_MATRAS = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
    private final String[] ROW3_MATRAS = {"ಾ", "ಿ", "ೀ", "ು", "ೂ", "ೃ", "ೄ", "್", "ಂ"};
    private final String[] ROW4_MATRAS = {"ಃ", "ೆ", "ೇ", "ೈ", "ೊ", "ೋ", "ೌ"};

    // Malayalam Matras & Numerals
    private final String[] ROW1_MATRAS_ML = {"൧", "൨", "൩", "൪", "൫", "൬", "൭", "൮", "൯", "൦"};
    private final String[] ROW2_MATRAS_ML = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
    private final String[] ROW3_MATRAS_ML = {"ാ", "ി", "ീ", "ു", "ൂ", "ൃ", "െ", "േ", "ൈ"};
    private final String[] ROW4_MATRAS_ML = {"ൊ", "ോ", "ൌ", "്", "ം", "ഃ", "്ര"};

    // Hindi Matras & Numerals
    private final String[] ROW1_MATRAS_HI = {"०", "१", "२", "३", "४", "५", "६", "७", "८", "९"};
    private final String[] ROW2_MATRAS_HI = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
    private final String[] ROW3_MATRAS_HI = {"ा", "ि", "ी", "ु", "ू", "ृ", "े", "ै", "ो"};
    private final String[] ROW4_MATRAS_HI = {"ौ", "्", "ं", "ः", "ँ", "ॅ", "ॉ"};

    private final String[] ROW1_SYMBOLS1 = {"+", "-", "*", "/", "=", "%", "₹", "$", "£", "€"};
    private final String[] ROW2_SYMBOLS1 = {"?", "!", ":", ";", "(", ")", "[", "]", "{", "}"};
    private final String[] ROW3_SYMBOLS1 = {"\"", "'", "@", "#", "&", "^", "_", "\\", "|"};
    private final String[] ROW4_SYMBOLS1 = {"~", "<", ">", ",", ".", "।", "॥"};

    private final String[] ROW1_SYMBOLS2 = {"+", "-", "*", "/", "=", "%", "₹", "$", "£", "€"};
    private final String[] ROW2_SYMBOLS2 = {"?", "!", ":", ";", "(", ")", "[", "]", "{", "}"};
    private final String[] ROW3_SYMBOLS2 = {"\"", "'", "@", "#", "&", "^", "_", "\\", "|"};
    private final String[] ROW4_SYMBOLS2 = {"~", "<", ">", ",", ".", "।", "॥"};

    private final String[] KANNADA_CONSONANTS = {
        "ಕ", "ಖ", "ಗ", "ಘ", "ಙ",
        "ಚ", "ಛ", "ಜ", "ಝ", "ಞ",
        "ಟ", "ಠ", "ಡ", "ಢ", "ಣ",
        "ತ", "ಥ", "ದ", "ಧ", "ನ",
        "ಪ", "ಫ", "ಬ", "ಭ", "ಮ",
        "ಯ", "ರ", "ಲ", "ವ", "ಶ",
        "ಷ", "ಸ", "ಹ", "ಳ", "ಕ್ಷ", "ಜ್ಞ"
    };

    private final String[] KANNADA_MODIFIERS = {
        "್", "ಾ", "ಿ", "ೀ", "ು", "ೂ", "ೃ", "ೆ", "ೇ", "ೈ", "ೊ", "ೋ", "ೌ", "ಂ", "ಃ"
    };


    // Profile Page Widgets
    private TextView txtProfileRole;
    private TextView txtProfileEmail;
    private TextView txtProfileUid;
    private android.widget.AutoCompleteTextView editProfileLanguage;
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

    private String currentUid;
    private String currentEmail;
    private String currentName = "";
    private String currentRole;

    private String foundUid;
    private String foundEmail;
    private String foundName;
    private String foundRole;
    private com.google.firebase.firestore.FirebaseFirestore db;

    // Caregiver Chat Section
    private RecyclerView rvCaregiverChatMessages;
    private View layoutCaregiverChatEmptyState;
    private CaregiverChatAdapter caregiverChatAdapter;
    private String connectedCaregiverUid = null;
    private boolean hasPendingRequest = false;
    private ListenerRegistration caregiverChatListener = null;
    private final java.util.Set<String> autoPlayedVoiceMsgIds = new java.util.HashSet<>();
    private List<DocumentSnapshot> latestCaregiverDocs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        networkExecutor = Executors.newSingleThreadExecutor();

        // Security Check: Verify user permissions
        String role = sessionManager.getUserRole();
        if (!RoleManager.checkAccess(this, role, RoleManager.ROLE_DEAF_USER, RoleManager.ROLE_SPEECH_IMPAIRED)) {
            finish();
            return;
        }
        SecurityGuard.verifyRoles(this, new String[]{RoleManager.ROLE_DEAF_USER, RoleManager.ROLE_SPEECH_IMPAIRED}, null);

        setContentView(R.layout.activity_speech_impaired_dashboard);

        // Bind Base UI Layouts
        txtUserRole = findViewById(R.id.txtUserRole);
        layoutStatus = findViewById(R.id.layoutStatus);
        viewStatusDot = findViewById(R.id.viewStatusDot);
        txtStatusLabel = findViewById(R.id.txtStatusLabel);
        btnBellNotificationSettings = findViewById(R.id.btnBellNotificationSettings);
        if (btnBellNotificationSettings != null) {
            btnBellNotificationSettings.setOnClickListener(v -> showNotificationSettingsDialog());
        }
        bottomNav = findViewById(R.id.bottomNavSpeechImpaired);

        layoutHome = findViewById(R.id.layoutHome);
        layoutProfile = findViewById(R.id.layoutProfile);
        layoutCaregiver = findViewById(R.id.layoutCaregiver);

        // Bind Deaf Assistant Visual Response Manager
        deafAssistantResponseManager = new com.kannada.speechassistant.voiceassistant.DeafAssistantResponseManager();
        deafAssistantResponseManager.bind(this, findViewById(android.R.id.content));
        com.kannada.speechassistant.voiceassistant.AppVoiceAssistant.getInstance(this)
                .setDeafAssistantResponseManager(deafAssistantResponseManager);

        // Load Persistent Settings
        loadSettings();

        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ? 
            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        currentRole = role;
        currentEmail = sessionManager.getUserDetails().get(SessionManager.KEY_USER_EMAIL);
        if (currentUid != null && !currentUid.isEmpty()) {
            db.collection("users").document(currentUid).update("online", true, "typingTo", null);
        }
        if (txtUserRole != null) {
            txtUserRole.setText(getString(R.string.role_speech_impaired));
        }

        fetchCurrentUserProfileName();

        // Bind and setup widgets
        setupNavigation();
        setupHomeWidgets();
        setupProfileWidgets();
        setupCaregiverWidgets();
        startRealTimeSync();

        // Initialize TTS Engine
        initGoogleTextToSpeech();

        // Check Online Connectivity status
        checkConnectionStatus();

        // Request Android 13+ Notification Permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Start Central Real-Time Background Notification Service
        Intent serviceIntent = new Intent(this, FirestoreRealtimeService.class);
        startService(serviceIntent);
    }

    /**
     * Set default preferences and load user details.
     */
    private void loadSettings() {
        SharedPreferences settingsPref = getSharedPreferences("SpeechSettings", MODE_PRIVATE);
        coquiUrl = settingsPref.getString("coquiUrl", "http://10.0.2.2:5000");
        caregiverEmail = settingsPref.getString("caregiverEmail", "");
        connectedCaregiverUid = settingsPref.getString("connectedCaregiverUid", null);
        
        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String normLang = LanguageManager.normalizeLanguageCode(userLang);
        String defaultSos = LanguageManager.getDefaultSosMessage(normLang);

        emergencyMessage = settingsPref.getString("emergencyMessage", defaultSos);
        if (emergencyMessage == null || emergencyMessage.trim().isEmpty() || LanguageManager.isDefaultSosMessage(emergencyMessage)) {
            emergencyMessage = defaultSos;
        }
        useCoquiTts = settingsPref.getBoolean("useCoquiTts", false);
    }

    /**
     * Helper method to set TTS language based on the user's selected language preference.
     */
    private int setTtsLanguageForUser(TextToSpeech engine) {
        String langCode = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        Locale targetLocale;
        switch (langCode) {
            case LanguageManager.LANG_HINDI:
                targetLocale = new Locale("hi", "IN");
                break;
            case LanguageManager.LANG_MALAYALAM:
                targetLocale = new Locale("ml", "IN");
                break;
            case LanguageManager.LANG_KANNADA:
            default:
                targetLocale = new Locale("kn", "IN");
                break;
        }

        int res = engine.setLanguage(targetLocale);
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            res = engine.setLanguage(new Locale(langCode));
        }
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            res = engine.setLanguage(Locale.getDefault());
        }
        return res;
    }

    /**
     * Initializes Google Text-to-Speech engine.
     */
    private void initGoogleTextToSpeech() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = setTtsLanguageForUser(tts);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "TTS locale not supported. Falling back to default system locale.");
                    tts.setLanguage(Locale.getDefault());
                }
                isTtsInitialized = true;
            } else {
                Log.e(TAG, "Google TTS initialization failed with package. Retrying with default engine.");
                new Handler(Looper.getMainLooper()).post(this::initDefaultTextToSpeech);
            }
        }, "com.google.android.tts");
    }

    /**
     * Initializes Default Text-to-Speech engine as fallback.
     */
    private void initDefaultTextToSpeech() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = setTtsLanguageForUser(tts);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "TTS locale not supported on default engine. Falling back to default system locale.");
                    tts.setLanguage(Locale.getDefault());
                }
                isTtsInitialized = true;
                Log.i(TAG, "Default TTS engine initialized successfully.");
            } else {
                Log.e(TAG, "Default TTS engine initialization failed.");
            }
        });
    }

    /**
     * Checks if Firestore is reachable.
     */
    private void checkConnectionStatus() {
        if (currentUid == null || currentUid.isEmpty()) {
            return;
        }
        FirebaseFirestore.getInstance().collection("users")
                .document(currentUid)
                .get()
                .addOnCompleteListener(task -> {
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

    /**
     * Setup navigation click listeners.
     */
    private void setupNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // Set all layouts to GONE initially
            layoutHome.setVisibility(View.GONE);
            layoutProfile.setVisibility(View.GONE);
            layoutCaregiver.setVisibility(View.GONE);

            if (itemId == R.id.nav_home) {
                layoutHome.setVisibility(View.VISIBLE);
                if (bottomNav != null) {
                    bottomNav.removeBadge(R.id.nav_home);
                }
                if (latestCaregiverDocs != null) {
                    markCaregiverMessagesAsRead(latestCaregiverDocs);
                }
            } else if (itemId == R.id.nav_profile) {
                layoutProfile.setVisibility(View.VISIBLE);
                refreshProfileDetails();
            } else if (itemId == R.id.nav_caregiver) {
                layoutCaregiver.setVisibility(View.VISIBLE);
                refreshConnectionState();
                CaregiverSoundManager.stopNotificationSound(SpeechImpairedDashboardActivity.this);
            }
            return true;
        });
    }

    /**
     * Setup widgets inside the Home tab.
     */
        /**
     * Setup widgets inside the Home tab.
     */
    private void setupHomeWidgets() {
        editHomeQuickText = findViewById(R.id.editHomeQuickText);
        com.google.android.material.button.MaterialButton btnVoiceAssistant = findViewById(R.id.btnVoiceAssistant);
        if (btnVoiceAssistant != null) {
            com.kannada.speechassistant.voiceassistant.AppVoiceAssistant.getInstance(this)
                    .attachVoiceAssistantButton(this, btnVoiceAssistant, null);
        }
        cardEmergency = findViewById(R.id.cardEmergency);

        // Bind Dual Keyboard Components
        toggleKeyboardGroup = findViewById(R.id.toggleKeyboardGroup);
        btnToggleEnglish = findViewById(R.id.btnToggleEnglish);
        btnToggleKannada = findViewById(R.id.btnToggleKannada);
        layoutHomeTranslitStatus = findViewById(R.id.layoutHomeTranslitStatus);
        progressHomeTranslit = findViewById(R.id.progressHomeTranslit);
        txtHomeTranslitStatus = findViewById(R.id.txtHomeTranslitStatus);
        layoutKannadaKeyboard = findViewById(R.id.layoutKannadaKeyboard);
        // Row 1: 10 Keys
        for (int i = 0; i < 10; i++) {
            int resId = getResources().getIdentifier("btnRow1Key" + (i + 1), "id", getPackageName());
            row1Keys[i] = findViewById(resId);
            final int index = i;
            if (row1Keys[i] != null) {
                row1Keys[i].setOnClickListener(v -> handleKannadaKeyPress(row1Keys[index].getText().toString()));
            }
        }

        // Row 2: 10 Keys
        for (int i = 0; i < 10; i++) {
            int resId = getResources().getIdentifier("btnRow2Key" + (i + 1), "id", getPackageName());
            row2Keys[i] = findViewById(resId);
            final int index = i;
            if (row2Keys[i] != null) {
                row2Keys[i].setOnClickListener(v -> handleKannadaKeyPress(row2Keys[index].getText().toString()));
            }
        }

        // Row 3: 9 Keys
        for (int i = 0; i < 9; i++) {
            int resId = getResources().getIdentifier("btnRow3Key" + (i + 1), "id", getPackageName());
            row3Keys[i] = findViewById(resId);
            final int index = i;
            if (row3Keys[i] != null) {
                row3Keys[i].setOnClickListener(v -> handleKannadaKeyPress(row3Keys[index].getText().toString()));
            }
        }

        // Row 4: 7 Keys
        for (int i = 0; i < 7; i++) {
            int resId = getResources().getIdentifier("btnRow4Key" + (i + 1), "id", getPackageName());
            row4Keys[i] = findViewById(resId);
            final int index = i;
            if (row4Keys[i] != null) {
                row4Keys[i].setOnClickListener(v -> handleKannadaKeyPress(row4Keys[index].getText().toString()));
            }
        }

        btnKeyboardShift = findViewById(R.id.btnKeyboardShift);
        btnKeyboardBackspace = findViewById(R.id.btnKeyboardBackspace);
        btnKeyboardModeToggle = findViewById(R.id.btnKeyboardModeToggle);
        btnKeyboardComma = findViewById(R.id.btnKeyboardComma);
        btnKeyboardSpace = findViewById(R.id.btnKeyboardSpace);
        btnKeyboardPeriod = findViewById(R.id.btnKeyboardPeriod);
        btnKeyboardEnter = findViewById(R.id.btnKeyboardEnter);

        if (btnKeyboardShift != null) btnKeyboardShift.setOnClickListener(v -> toggleKeyboardShift());
        if (btnKeyboardBackspace != null) btnKeyboardBackspace.setOnClickListener(v -> handleBackspace());
        if (btnKeyboardModeToggle != null) btnKeyboardModeToggle.setOnClickListener(v -> toggleKeyboardModeState());
        if (btnKeyboardComma != null) btnKeyboardComma.setOnClickListener(v -> handleKannadaKeyPress(","));
        if (btnKeyboardPeriod != null) btnKeyboardPeriod.setOnClickListener(v -> handleKannadaKeyPress("."));
        if (btnKeyboardSpace != null) btnKeyboardSpace.setOnClickListener(v -> handleKannadaKeyPress(" "));
        if (btnKeyboardEnter != null) btnKeyboardEnter.setOnClickListener(v -> handleKannadaKeyPress("\n"));

        btnHomeClear = findViewById(R.id.btnHomeClear);
        btnHomeSend = findViewById(R.id.btnHomeSend);
        layoutHomeRecentPhrases = findViewById(R.id.layoutHomeRecentPhrases);

        // Auto-transliterate English input on space key or after a 1.2s delay of inactivity
        editHomeQuickText.addTextChangedListener(new android.text.TextWatcher() {
            private final Handler debounceHandler = new Handler(Looper.getMainLooper());
            private Runnable debounceRunnable;
            private boolean isCurrentlyTyping = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.toString().trim().length() > 0;
                if (hasText != isCurrentlyTyping) {
                    isCurrentlyTyping = hasText;
                    if (currentUid != null && !currentUid.isEmpty()) {
                        db.collection("users").document(currentUid)
                            .update("typingTo", isCurrentlyTyping ? connectedCaregiverUid : null);
                    }
                }

                if (isTranslitInternalChange) return;

                if (toggleKeyboardGroup.getCheckedButtonId() != R.id.btnToggleEnglish) {
                    return;
                }

                if (count > 0 && s.length() > 0) {
                    char lastChar = s.charAt(start + count - 1);
                    if (lastChar == ' ' || lastChar == '\n') {
                        triggerHomeTransliteration();
                        return;
                    }
                }

                if (debounceRunnable != null) {
                    debounceHandler.removeCallbacks(debounceRunnable);
                }
                debounceRunnable = () -> triggerHomeTransliteration();
                debounceHandler.postDelayed(debounceRunnable, 1200);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        updateKeyboardTabLabels();

        // Toggle Keyboard mode listener
        toggleKeyboardGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                setKeyboardMode(checkedId == R.id.btnToggleKannada);
            }
        });

        // Control buttons
        btnHomeClear.setOnClickListener(v -> editHomeQuickText.setText(""));
        btnHomeSend.setOnClickListener(v -> triggerHomeSendMessage());

        cardEmergency.setOnClickListener(v -> triggerEmergencySOS());

        // Initialize Caregiver Chat
        rvCaregiverChatMessages = findViewById(R.id.rvCaregiverChatMessages);
        layoutCaregiverChatEmptyState = findViewById(R.id.layoutCaregiverChatEmptyState);
        caregiverChatAdapter = new CaregiverChatAdapter(currentUid, this::speakText);
        rvCaregiverChatMessages.setLayoutManager(new LinearLayoutManager(this));
        rvCaregiverChatMessages.setAdapter(caregiverChatAdapter);
        rvCaregiverChatMessages.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        // Initialize haptic feedback
        vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);

        btnMicInput = findViewById(R.id.btnMicInput);
        if (btnMicInput != null) {
            btnMicInput.setOnClickListener(v -> {
                if (isRecording) {
                    AccessibleMicFeedbackManager.triggerShortVibration(this);
                    stopSpeechRecognition();
                } else {
                    checkAndStartSpeechRecognition();
                }
            });
        }

        // Load initially used phrases
        updateHomeRecentPhrasesList();
    }

    private void setKeyboardMode(boolean isKannadaMode) {
        if (isKannadaMode) {
            layoutHomeTranslitStatus.setVisibility(View.GONE);
            if (layoutKannadaKeyboard.getVisibility() != View.VISIBLE) {
                layoutKannadaKeyboard.setVisibility(View.VISIBLE);
                layoutKannadaKeyboard.post(() -> {
                    layoutKannadaKeyboard.setTranslationY(layoutKannadaKeyboard.getHeight());
                    layoutKannadaKeyboard.animate().translationY(0).setDuration(220).start();
                });
            }
            editHomeQuickText.setShowSoftInputOnFocus(false);
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(editHomeQuickText.getWindowToken(), 0);
            }
            keyboardState = 0; // Default unshifted
            updateKeyboardLabels();
        } else {
            layoutHomeTranslitStatus.setVisibility(View.VISIBLE);
            if (layoutKannadaKeyboard.getVisibility() == View.VISIBLE) {
                layoutKannadaKeyboard.animate()
                    .translationY(layoutKannadaKeyboard.getHeight())
                    .setDuration(200)
                    .withEndAction(() -> layoutKannadaKeyboard.setVisibility(View.GONE))
                    .start();
            } else {
                layoutKannadaKeyboard.setVisibility(View.GONE);
            }
            editHomeQuickText.setShowSoftInputOnFocus(true);
            editHomeQuickText.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editHomeQuickText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    private void updateKeyboardTabLabels() {
        MaterialButton btnToggleKannada = findViewById(R.id.btnToggleKannada);
        MaterialButton btnToggleEnglish = findViewById(R.id.btnToggleEnglish);
        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String normLang = LanguageManager.normalizeLanguageCode(userLang);

        if (btnToggleEnglish != null) {
            btnToggleEnglish.setText("English Keyboard");
        }
        if (btnToggleKannada != null) {
            if (LanguageManager.LANG_HINDI.equals(normLang)) {
                btnToggleKannada.setText("Hindi Keyboard");
            } else if (LanguageManager.LANG_MALAYALAM.equals(normLang)) {
                btnToggleKannada.setText("Malayalam Keyboard");
            } else {
                btnToggleKannada.setText("Kannada Keyboard");
            }
        }
    }

    private void handleKannadaKeyPress(String str) {
        if (editHomeQuickText == null) return;
        int start = Math.max(0, editHomeQuickText.getSelectionStart());
        int end = Math.max(0, editHomeQuickText.getSelectionEnd());
        int selMin = Math.min(start, end);
        int selMax = Math.max(start, end);
        editHomeQuickText.getText().replace(selMin, selMax, str);
        editHomeQuickText.setSelection(selMin + str.length());
    }

    private void toggleKeyboardShift() {
        if (keyboardState == 0) {
            keyboardState = 1; // Shifted
            if (btnKeyboardShift != null) btnKeyboardShift.setTextColor(0xFF4F46E5);
        } else if (keyboardState == 1) {
            keyboardState = 0; // Unshifted
            if (btnKeyboardShift != null) btnKeyboardShift.setTextColor(0xFF0F172A);
        } else {
            keyboardState = 0; // Back to alphabet
            if (btnKeyboardShift != null) btnKeyboardShift.setTextColor(0xFF0F172A);
            if (btnKeyboardModeToggle != null) btnKeyboardModeToggle.setText("? 123");
        }
        updateKeyboardLabels();
    }

    private void toggleKeyboardModeState() {
        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String normLang = LanguageManager.normalizeLanguageCode(userLang);
        String defaultModeChar = LanguageManager.LANG_MALAYALAM.equals(normLang) ? "ക" : (LanguageManager.LANG_HINDI.equals(normLang) ? "क" : "ಕ");

        if (keyboardState != 2 && keyboardState != 3 && keyboardState != 4) {
            keyboardState = 2; // Matras
            if (btnKeyboardModeToggle != null) btnKeyboardModeToggle.setText("123");
        } else if (keyboardState == 2) {
            keyboardState = 3; // Symbols Page 1
            if (btnKeyboardModeToggle != null) btnKeyboardModeToggle.setText("#+=");
        } else if (keyboardState == 3) {
            keyboardState = 4; // Symbols Page 2
            if (btnKeyboardModeToggle != null) btnKeyboardModeToggle.setText(defaultModeChar);
        } else {
            keyboardState = 0; // Back to Unshifted
            if (btnKeyboardModeToggle != null) btnKeyboardModeToggle.setText("? 123");
        }
        updateKeyboardLabels();
    }

    private void updateKeyboardLabels() {
        String[] row1;
        String[] row2;
        String[] row3;
        String[] row4;

        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String normLang = LanguageManager.normalizeLanguageCode(userLang);
        String defaultModeChar = LanguageManager.LANG_MALAYALAM.equals(normLang) ? "ക" : (LanguageManager.LANG_HINDI.equals(normLang) ? "क" : "ಕ");

        if (keyboardState == 1) {
            if (LanguageManager.LANG_MALAYALAM.equals(normLang)) {
                row1 = ROW1_SHIFTED_ML;
                row2 = ROW2_SHIFTED_ML;
                row3 = ROW3_SHIFTED_ML;
                row4 = ROW4_SHIFTED_ML;
            } else if (LanguageManager.LANG_HINDI.equals(normLang)) {
                row1 = ROW1_SHIFTED_HI;
                row2 = ROW2_SHIFTED_HI;
                row3 = ROW3_SHIFTED_HI;
                row4 = ROW4_SHIFTED_HI;
            } else {
                row1 = ROW1_SHIFTED;
                row2 = ROW2_SHIFTED;
                row3 = ROW3_SHIFTED;
                row4 = ROW4_SHIFTED;
            }
            if (btnKeyboardShift != null) {
                btnKeyboardShift.setText("⇧");
                btnKeyboardShift.setTextColor(0xFF4F46E5);
            }
        } else if (keyboardState == 2) {
            if (LanguageManager.LANG_MALAYALAM.equals(normLang)) {
                row1 = ROW1_MATRAS_ML;
                row2 = ROW2_MATRAS_ML;
                row3 = ROW3_MATRAS_ML;
                row4 = ROW4_MATRAS_ML;
            } else if (LanguageManager.LANG_HINDI.equals(normLang)) {
                row1 = ROW1_MATRAS_HI;
                row2 = ROW2_MATRAS_HI;
                row3 = ROW3_MATRAS_HI;
                row4 = ROW4_MATRAS_HI;
            } else {
                row1 = ROW1_MATRAS;
                row2 = ROW2_MATRAS;
                row3 = ROW3_MATRAS;
                row4 = ROW4_MATRAS;
            }
            if (btnKeyboardShift != null) {
                btnKeyboardShift.setText(defaultModeChar);
                btnKeyboardShift.setTextColor(0xFF0F172A);
            }
        } else if (keyboardState == 3) {
            row1 = ROW1_SYMBOLS1;
            row2 = ROW2_SYMBOLS1;
            row3 = ROW3_SYMBOLS1;
            row4 = ROW4_SYMBOLS1;
            if (btnKeyboardShift != null) {
                btnKeyboardShift.setText(defaultModeChar);
                btnKeyboardShift.setTextColor(0xFF0F172A);
            }
        } else if (keyboardState == 4) {
            row1 = ROW1_SYMBOLS2;
            row2 = ROW2_SYMBOLS2;
            row3 = ROW3_SYMBOLS2;
            row4 = ROW4_SYMBOLS2;
            if (btnKeyboardShift != null) {
                btnKeyboardShift.setText(defaultModeChar);
                btnKeyboardShift.setTextColor(0xFF0F172A);
            }
        } else {
            if (LanguageManager.LANG_MALAYALAM.equals(normLang)) {
                row1 = ROW1_UNSHIFTED_ML;
                row2 = ROW2_UNSHIFTED_ML;
                row3 = ROW3_UNSHIFTED_ML;
                row4 = ROW4_UNSHIFTED_ML;
            } else if (LanguageManager.LANG_HINDI.equals(normLang)) {
                row1 = ROW1_UNSHIFTED_HI;
                row2 = ROW2_UNSHIFTED_HI;
                row3 = ROW3_UNSHIFTED_HI;
                row4 = ROW4_UNSHIFTED_HI;
            } else {
                row1 = ROW1_UNSHIFTED;
                row2 = ROW2_UNSHIFTED;
                row3 = ROW3_UNSHIFTED;
                row4 = ROW4_UNSHIFTED;
            }
            if (btnKeyboardShift != null) {
                btnKeyboardShift.setText("⇧");
                btnKeyboardShift.setTextColor(0xFF0F172A);
            }
        }

        for (int i = 0; i < 10; i++) {
            if (row1Keys[i] != null) row1Keys[i].setText(row1[i]);
        }
        for (int i = 0; i < 10; i++) {
            if (row2Keys[i] != null) row2Keys[i].setText(row2[i]);
        }
        for (int i = 0; i < 9; i++) {
            if (row3Keys[i] != null) row3Keys[i].setText(row3[i]);
        }
        for (int i = 0; i < 7; i++) {
            if (row4Keys[i] != null) row4Keys[i].setText(row4[i]);
        }
    }

    /**
     * Handles keyboard deletion logic.
     */
    private void handleBackspace() {
        if (editHomeQuickText == null) return;
        int start = editHomeQuickText.getSelectionStart();
        int end = editHomeQuickText.getSelectionEnd();
        if (start < 0) return;
        if (start != end) {
            int selMin = Math.min(start, end);
            int selMax = Math.max(start, end);
            editHomeQuickText.getText().delete(selMin, selMax);
        } else if (start > 0) {
            android.text.Editable editable = editHomeQuickText.getText();
            if (start >= 2 && Character.isSurrogatePair(editable.charAt(start - 2), editable.charAt(start - 1))) {
                editable.delete(start - 2, start);
            } else {
                editable.delete(start - 1, start);
            }
        }
    }

    /**
     * Converts phonetic English letters in text area to target script (Malayalam, Hindi, or Kannada).
     */
    private void triggerHomeTransliteration() {
        final String text = editHomeQuickText.getText().toString();
        if (text.trim().isEmpty()) {
            return;
        }

        runOnUiThread(() -> {
            progressHomeTranslit.setVisibility(View.VISIBLE);
            txtHomeTranslitStatus.setText("Converting...");
        });

        final String langCode = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        final String itcCode = LanguageManager.getInputToolCode(langCode);

        networkExecutor.execute(() -> {
            HttpURLConnection conn = null;
            BufferedReader reader = null;
            try {
                String encodedText = Uri.encode(text.trim());
                URL url = new URL("https://inputtools.google.com/request?text=" + encodedText + "&itc=" + itcCode + "&num=1");
                
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    StringBuilder response = new StringBuilder();
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    org.json.JSONArray rootArray = new org.json.JSONArray(response.toString());
                    if (rootArray.length() > 1 && "SUCCESS".equals(rootArray.getString(0))) {
                        org.json.JSONArray items = rootArray.getJSONArray(1);
                        if (items.length() > 0) {
                            org.json.JSONObject itemObj = items.getJSONObject(0);
                            org.json.JSONArray destArray = itemObj.getJSONArray("translit_dest");
                            if (destArray.length() > 0) {
                                final String converted = destArray.getString(0) + (text.endsWith(" ") ? " " : "");
                                runOnUiThread(() -> {
                                    if (!editHomeQuickText.getText().toString().equals(converted)) {
                                        isTranslitInternalChange = true;
                                        editHomeQuickText.setText(converted);
                                        editHomeQuickText.setSelection(converted.length());
                                        isTranslitInternalChange = false;
                                    }
                                    progressHomeTranslit.setVisibility(View.GONE);
                                    txtHomeTranslitStatus.setText("Translit active. (Auto-converted)");
                                });
                                return;
                            }
                        }
                    }
                }
                throw new Exception("Transliteration request failed.");

            } catch (Exception e) {
                Log.w(TAG, "Online transliteration failed. Falling back to local offline rules.", e);
                final String converted = TransliterationEngine.transliterate(text.trim(), langCode) + (text.endsWith(" ") ? " " : "");
                runOnUiThread(() -> {
                    if (!editHomeQuickText.getText().toString().equals(converted)) {
                        isTranslitInternalChange = true;
                        editHomeQuickText.setText(converted);
                        editHomeQuickText.setSelection(converted.length());
                        isTranslitInternalChange = false;
                    }
                    progressHomeTranslit.setVisibility(View.GONE);
                    txtHomeTranslitStatus.setText("Converted (Offline Fallback)");
                });
            } finally {
                try {
                    if (reader != null) reader.close();
                } catch (Exception ex) {}
                if (conn != null) conn.disconnect();
            }
        });
    }

    private void triggerHomeSendMessage() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSendClickTime < 1000) {
            return; // Prevent rapid double-tap / duplicate messages
        }

        final String msg = editHomeQuickText.getText().toString().trim();
        if (msg.isEmpty()) {
            Toast.makeText(this, "Please enter some text to send.", Toast.LENGTH_SHORT).show();
            return;
        }

        lastSendClickTime = currentTime;
        editHomeQuickText.setText("");
        sendDirectMessageToCaregiver(msg);
        saveSpokenHistory(msg);
        updateHomeRecentPhrasesList();
    }

    private void sendDirectMessageToCaregiver(String msg) {
        if (connectedCaregiverUid == null || connectedCaregiverUid.isEmpty()) {
            Toast.makeText(this, "No caregiver connected to send message.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate deterministic chatId
        String chatId;
        if (currentUid.compareTo(connectedCaregiverUid) < 0) {
            chatId = currentUid + "_" + connectedCaregiverUid;
        } else {
            chatId = connectedCaregiverUid + "_" + currentUid;
        }

        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String lang = LanguageManager.detectLanguageFromText(msg, userLang);

        String msgType = "text";
        if (LanguageManager.isDefaultSosMessage(msg)) {
            msgType = "emergency";
        }

        // Save to chats/{chatId}/messages
        Map<String, Object> chatMsgMap = new HashMap<>();
        chatMsgMap.put("chatId", chatId);
        chatMsgMap.put("senderId", currentUid);
        chatMsgMap.put("senderUid", currentUid);
        chatMsgMap.put("receiverId", connectedCaregiverUid);
        chatMsgMap.put("recipientEmail", caregiverEmail != null ? caregiverEmail.toLowerCase().trim() : "");
        chatMsgMap.put("senderRole", "Speech-Impaired User");
        chatMsgMap.put("receiverRole", "Admin/Caregiver");
        chatMsgMap.put("message", msg);
        chatMsgMap.put("messageText", msg);
        chatMsgMap.put("language", lang);
        chatMsgMap.put("messageType", msgType);
        chatMsgMap.put("type", msgType);
        chatMsgMap.put("status", "sent");
        chatMsgMap.put("readStatus", false);
        chatMsgMap.put("delivered", false);
        chatMsgMap.put("seen", false);
        chatMsgMap.put("timestamp", com.google.firebase.Timestamp.now()); // local time first for immediate UI display

        db.collection("caregiver_messages")
                .add(chatMsgMap)
                .addOnSuccessListener(ref -> {
                    // Update status to "sent" and set server timestamp
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("messageId", ref.getId());
                    updates.put("status", "sent");
                    updates.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    ref.update(updates);

                    // Post delayed handler (1000ms) to update status to "delivered"
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        ref.update("status", "delivered", "delivered", true);
                    }, 1000);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send message to caregiver chat thread", e);
                    // Save locally as offline fallback
                    LocalConnectionSimulator.saveLocalMessage(SpeechImpairedDashboardActivity.this, chatId, chatMsgMap);
                    Toast.makeText(this, "Saved locally (Offline)", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Renders scroll chips list representing recently spoken sentences.
     */
    private List<String> getInitialDefaultPhrases() {
        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String normLang = LanguageManager.normalizeLanguageCode(userLang);

        List<String> list = new ArrayList<>();
        if (LanguageManager.LANG_MALAYALAM.equals(normLang)) {
            list.add("എനിക്ക് വിശക്കുന്നു");
            list.add("എനിക്ക് സഹായം വേണം, ദയവായി ശ്രദ്ധിക്കുക");
            list.add("എനിക്ക് പുറത്ത് പോകണം");
        } else if (LanguageManager.LANG_HINDI.equals(normLang)) {
            list.add("मुझे भूख लगी है");
            list.add("मुझे मदद चाहिए, कृपया ध्यान दें");
            list.add("मुझे बाहर जाना है");
        } else {
            list.add("ನನಗೆ ಹಸಿವಾಗಿದೆ");
            list.add("ನನಗೆ ಸಹಾಯ ಬೇಕು, ದಯವಿಟ್ಟು ಗಮನಿಸಿ");
            list.add("ನಾನು ಹೊರಗೆ ಹೋಗಬೇಕಾಗಿದೆ");
        }
        return list;
    }

    private void updateHomeRecentPhrasesList() {
        if (layoutHomeRecentPhrases == null) return;
        layoutHomeRecentPhrases.removeAllViews();
        List<String> history = getSpokenHistory();
        if (history.isEmpty()) {
            history = getInitialDefaultPhrases();
        }

        for (final String phrase : history) {
            com.google.android.material.button.MaterialButton chip = new com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.style.Widget_MaterialComponents_Button_OutlinedButton);
            chip.setText(phrase);
            chip.setTextSize(11);
            chip.setAllCaps(false);
            chip.setCornerRadius((int) (16 * getResources().getDisplayMetrics().density));
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (int) (36 * getResources().getDisplayMetrics().density)
            );
            params.setMargins(0, 0, 12, 0);
            chip.setLayoutParams(params);
            chip.setTextColor(0xFF475569);
            chip.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFFCBD5E1));

            chip.setOnClickListener(v -> {
                editHomeQuickText.setText(phrase);
                editHomeQuickText.setSelection(phrase.length());
            });
            layoutHomeRecentPhrases.addView(chip);
        }
    }



    /**
     * Setup widgets inside the Profile tab.
     */
    private void setupProfileWidgets() {
        txtProfileRole = findViewById(R.id.txtProfileRole);
        txtProfileEmail = findViewById(R.id.txtProfileEmail);
        txtProfileUid = findViewById(R.id.txtProfileUid);
        editProfileLanguage = findViewById(R.id.editProfileLanguage);
        if (editProfileLanguage != null) {
            String[] languages = {"Kannada", "Hindi", "Malayalam"};
            android.widget.ArrayAdapter<String> langAdapter = new android.widget.ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    languages
            );
            editProfileLanguage.setAdapter(langAdapter);
            String currentLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
            editProfileLanguage.setText(LanguageManager.getLanguageDisplayName(currentLang), false);
            editProfileLanguage.setOnClickListener(v -> editProfileLanguage.showDropDown());
        }
        editCoquiUrl = findViewById(R.id.editCoquiUrl);
        editCaregiverEmail = findViewById(R.id.editCaregiverEmail);
        editEmergencyMessage = findViewById(R.id.editEmergencyMessage);
        btnSaveProfileSettings = findViewById(R.id.btnSaveProfileSettings);
        btnProfileLogout = findViewById(R.id.btnProfileLogout);

        btnSaveProfileSettings.setOnClickListener(v -> {
            String url = editCoquiUrl.getText().toString().trim();
            String email = editCaregiverEmail.getText().toString().trim();
            String sosMsg = editEmergencyMessage.getText().toString().trim();
            String selectedLangDisplay = editProfileLanguage != null ? editProfileLanguage.getText().toString().trim() : "";
            String selectedLangCode = LanguageManager.getLanguageCodeFromDisplayName(selectedLangDisplay);

            if (url.isEmpty() || email.isEmpty() || sosMsg.isEmpty()) {
                Toast.makeText(this, "Please fill out all configuration settings.", Toast.LENGTH_SHORT).show();
                return;
            }

            saveSettings(url, email, sosMsg, selectedLangCode);
        });

        btnProfileLogout.setOnClickListener(v -> performLogout());
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

        btnSearch.setOnClickListener(v -> searchUserByEmail());
        btnSendRequest.setOnClickListener(v -> sendConnectionRequest());
    }

    /**
     * Refreshes the details under the profile tab.
     */
    private void refreshProfileDetails() {
        HashMap<String, String> userDetails = sessionManager.getUserDetails();
        txtProfileEmail.setText(userDetails.get(SessionManager.KEY_USER_EMAIL));
        String roleStr = userDetails.get(SessionManager.KEY_USER_ROLE);
        String locRole = getString(R.string.role_speech_impaired);
        if (RoleManager.ROLE_PHYSICALLY_DISABLED.equals(roleStr)) {
            locRole = getString(R.string.role_physically_disabled);
        } else if (RoleManager.ROLE_ADMIN_CAREGIVER.equals(roleStr)) {
            locRole = getString(R.string.role_caregiver);
        }
        txtProfileRole.setText(getString(R.string.user_role) + ": " + locRole);
        txtProfileUid.setText("UID: " + userDetails.get(SessionManager.KEY_USER_UID));

        if (editProfileLanguage != null) {
            String currentLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
            editProfileLanguage.setText(LanguageManager.getLanguageDisplayName(currentLang), false);
        }

        editCoquiUrl.setText(coquiUrl);
        if (connectedCaregiverUid != null && !connectedCaregiverUid.isEmpty() && caregiverEmail != null && !caregiverEmail.isEmpty()) {
            editCaregiverEmail.setText(caregiverEmail);
        } else {
            editCaregiverEmail.setText("");
            editCaregiverEmail.setHint(getString(R.string.enter_email_address));
        }
        editEmergencyMessage.setText(emergencyMessage);
    }

    /**
     * Saves user settings locally in Preferences and updates active language.
     */
    private void saveSettings(String url, String email, String sosMsg, String newLangCode) {
        String oldLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String normalizedNewLang = LanguageManager.normalizeLanguageCode(newLangCode);
        boolean langChanged = !oldLang.equals(normalizedNewLang);

        if (sosMsg == null || sosMsg.trim().isEmpty() || LanguageManager.isDefaultSosMessage(sosMsg)) {
            sosMsg = LanguageManager.getDefaultSosMessage(normalizedNewLang);
        }

        getSharedPreferences("SpeechSettings", MODE_PRIVATE).edit()
                .putString("coquiUrl", url)
                .putString("caregiverEmail", email)
                .putString("emergencyMessage", sosMsg)
                .apply();
        coquiUrl = url;
        caregiverEmail = email;
        emergencyMessage = sosMsg;

        if (sessionManager != null) {
            sessionManager.saveLanguage(normalizedNewLang);
        }

        if (currentUid != null && !currentUid.isEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(currentUid)
                    .update("language", normalizedNewLang)
                    .addOnFailureListener(e -> Log.e(TAG, "Failed syncing language to Firestore", e));
        }

        LanguageManager.setLocale(this, normalizedNewLang);
        if (tts != null) {
            setTtsLanguageForUser(tts);
        }

        Toast.makeText(this, "System Settings Saved Successfully!", Toast.LENGTH_SHORT).show();

        if (langChanged) {
            recreate();
        }
    }

    /**
     * Cleans input phrase to remove parenthesized translations.
     */
    private String cleanKannadaText(String text) {
        if (text == null) return "";
        String cleaned = text.split("\\(")[0].trim();
        cleaned = cleaned.split("\\[")[0].trim();
        return cleaned;
    }

    /**
     * Synthesizes and plays out text speech.
     */
    private void speakText(String text) {
        speakText(text, false);
    }

    private void speakText(String text, boolean isFallback) {
        String cleanedText = cleanKannadaText(text);
        if (cleanedText.isEmpty()) return;

        // Speak instantly on device via local Google TTS for zero latency
        speakGoogleTts(cleanedText, isFallback);

        // Optionally request neural backend in background if enabled
        if (useCoquiTts && !isFallback) {
            speakCoquiTts(cleanedText, true);
        }
    }

    /**
     * Synthesizes Kannada/English text using Google's local on-device TTS.
     */
    private void speakGoogleTts(String message) {
        speakGoogleTts(message, false);
    }

    private void speakGoogleTts(String message, boolean isFallback) {
        boolean canSpeakLocally = false;
        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String langName = LanguageManager.detectLanguageFromText(message, userLang);
        Locale targetLocale = LanguageManager.getLocale(langName);

        if (isTtsInitialized && tts != null) {
            int availability = tts.isLanguageAvailable(targetLocale);
            if (availability >= TextToSpeech.LANG_AVAILABLE) {
                tts.setLanguage(targetLocale);
                canSpeakLocally = true;
            } else {
                Locale baseLocale = new Locale(LanguageManager.normalizeLanguageCode(langName));
                if (tts.isLanguageAvailable(baseLocale) >= TextToSpeech.LANG_AVAILABLE) {
                    tts.setLanguage(baseLocale);
                    canSpeakLocally = true;
                } else {
                    tts.setLanguage(Locale.getDefault());
                    canSpeakLocally = true;
                }
            }
        }

        if (canSpeakLocally && tts != null) {
            tts.setPitch(1.0f);
            tts.setSpeechRate(1.0f);

            CaregiverSoundManager.speakWithVolume(tts, message, "speech_assist_google", this);
            saveSpokenHistory(message);
        } else {
            if (!isFallback) {
                Log.w(TAG, "Local Google TTS model not ready. Falling back to Coqui TTS.");
                speakCoquiTts(message, true);
            } else {
                Toast.makeText(this, "Speech failed: Language TTS model not supported locally and server is offline.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Synthesizes voice via Coqui TTS backend with a fallback to Google TTS if offline.
     */
    private void speakCoquiTts(final String message) {
        speakCoquiTts(message, false);
    }

    private void speakCoquiTts(final String message, boolean isFallback) {
        Toast.makeText(this, "Fetching Neural Synthesis (Coqui TTS)...", Toast.LENGTH_SHORT).show();
        networkExecutor.execute(() -> {
            HttpURLConnection conn = null;
            InputStream is = null;
            FileOutputStream fos = null;
            try {
                URL url = new URL(coquiUrl + "/api/tts");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "audio/wav, audio/mpeg, application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(12000);

                JSONObject json = new JSONObject();
                json.put("text", message);

                java.io.OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    is = conn.getInputStream();

                    final File tempFile = new File(getCacheDir(), "coqui_temp_voice.dat");
                    if (tempFile.exists()) tempFile.delete();

                    fos = new FileOutputStream(tempFile);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    fos.flush();
                    fos.close();

                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            if (mediaPlayer != null) {
                                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                                mediaPlayer.release();
                            }
                            mediaPlayer = new MediaPlayer();
                            mediaPlayer.setDataSource(tempFile.getAbsolutePath());
                            mediaPlayer.prepare();
                            mediaPlayer.start();

                            saveSpokenHistory(message);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to play synthesized Coqui audio stream", e);
                            if (!isFallback) {
                                Toast.makeText(this, "Playback error. Falling back to Google TTS.", Toast.LENGTH_SHORT).show();
                                speakGoogleTts(message, true);
                            } else {
                                Toast.makeText(this, "Speech playback failed.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    throw new Exception("Server response code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Coqui network synthesis request failed.", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isFallback) {
                        Toast.makeText(this, "Coqui Server Offline. Redirecting to Google TTS.", Toast.LENGTH_LONG).show();
                        speakGoogleTts(message, true);
                    } else {
                        Toast.makeText(this, "Speech request failed (Offline).", Toast.LENGTH_LONG).show();
                    }
                });
            } finally {
                try {
                    if (is != null) is.close();
                    if (fos != null) fos.close();
                } catch (Exception ex) {}
                if (conn != null) conn.disconnect();
            }
        });
    }

    /**
     * sound siren alarm and post emergency alert parameters to Firestore.
     */
    private void triggerEmergencySOS() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEmergencyTriggerTime < 2000) {
            return; // Prevent rapid duplicate emergency triggers
        }
        lastEmergencyTriggerTime = currentTime;

        // Ensure connectedCaregiverUid is available from persistent settings if null in memory
        if (connectedCaregiverUid == null || connectedCaregiverUid.trim().isEmpty()) {
            SharedPreferences settingsPref = getSharedPreferences("SpeechSettings", MODE_PRIVATE);
            connectedCaregiverUid = settingsPref.getString("connectedCaregiverUid", null);
        }

        Toast.makeText(this, "EMERGENCY ALARM TRIGGERED!", Toast.LENGTH_LONG).show();

        // 0. Maximize System Media Volume for Loud External Output
        try {
            android.media.AudioManager am = (android.media.AudioManager) getSystemService(android.content.Context.AUDIO_SERVICE);
            if (am != null) {
                int maxVol = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
                am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, maxVol, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed setting maximum volume for emergency alert", e);
        }

        // 1. System Emergency Alert Trigger (Siren sound deleted)

        // 2. Trigger Strong Emergency Vibration Pattern
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(new long[]{0, 500, 200, 500, 200, 500, 200, 500}, -1));
                } else {
                    vibrator.vibrate(new long[]{0, 500, 200, 500, 200, 500, 200, 500}, -1);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed playing emergency vibration", e);
        }

        // 3. Speak Out Emergency Message Loudly in User's Selected Language (Kannada / Hindi / Malayalam)
        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String normLang = LanguageManager.normalizeLanguageCode(userLang);

        if (emergencyMessage == null || emergencyMessage.trim().isEmpty() || LanguageManager.isDefaultSosMessage(emergencyMessage)) {
            emergencyMessage = LanguageManager.getDefaultSosMessage(normLang);
        }
        speakText(emergencyMessage);

        // 4. Send Emergency Message to Connected Caregiver Immediately
        if (connectedCaregiverUid != null && !connectedCaregiverUid.isEmpty()) {
            sendEmergencyChatMessage(emergencyMessage);
        }

        // 3. Write Emergency Log to Firestore
        HashMap<String, String> userDetails = sessionManager.getUserDetails();
        String uid = userDetails.get(SessionManager.KEY_USER_UID);
        String email = userDetails.get(SessionManager.KEY_USER_EMAIL);
        String displayName = (currentName != null && !currentName.isEmpty()) ? currentName : (email != null ? email.split("@")[0] : "Speech Impaired User");

        Map<String, Object> sosLog = new HashMap<>();
        sosLog.put("uid", uid);
        sosLog.put("email", email);
        sosLog.put("userName", displayName);
        sosLog.put("role", RoleManager.ROLE_SPEECH_IMPAIRED);
        sosLog.put("message", emergencyMessage);
        sosLog.put("timestamp", com.google.firebase.Timestamp.now());
        sosLog.put("status", "Unresolved");
        if (connectedCaregiverUid != null && !connectedCaregiverUid.trim().isEmpty()) {
            sosLog.put("caregiverUid", connectedCaregiverUid);
        }

        FirebaseFirestore.getInstance().collection("emergencies")
                .add(sosLog)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "SOS Alert uploaded successfully to Firestore.", Toast.LENGTH_SHORT).show();
                    
                    // Also write to EmergencyAlerts for FCM and Service syncing
                    Map<String, Object> fcmAlert = new HashMap<>();
                    String alertId = FirebaseFirestore.getInstance().collection("EmergencyAlerts").document().getId();
                    fcmAlert.put("alertId", alertId);
                    fcmAlert.put("userId", uid);
                    fcmAlert.put("userName", displayName);
                    fcmAlert.put("message", emergencyMessage);
                    fcmAlert.put("timestamp", com.google.firebase.Timestamp.now());
                    fcmAlert.put("status", "active");
                    fcmAlert.put("role", RoleManager.ROLE_SPEECH_IMPAIRED);
                    if (connectedCaregiverUid != null && !connectedCaregiverUid.trim().isEmpty()) {
                        fcmAlert.put("caregiverUid", connectedCaregiverUid);
                    }

                    FirebaseFirestore.getInstance().collection("EmergencyAlerts").document(alertId)
                            .set(fcmAlert)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "EmergencyAlerts doc written"))
                            .addOnFailureListener(err -> Log.e(TAG, "Failed to write to EmergencyAlerts", err));

                    // Also write to emergency_alerts for redesigned Caregiver dashboard
                    Map<String, Object> newAlert = new HashMap<>();
                    String newAlertId = FirebaseFirestore.getInstance().collection("emergency_alerts").document().getId();
                    newAlert.put("alertId", newAlertId);
                    newAlert.put("patientId", uid);
                    newAlert.put("patientName", displayName);
                    newAlert.put("message", emergencyMessage);
                    newAlert.put("timestamp", com.google.firebase.Timestamp.now());
                    newAlert.put("status", "NEW");
                    newAlert.put("acknowledged", false);
                    newAlert.put("resolved", false);
                    if (connectedCaregiverUid != null && !connectedCaregiverUid.trim().isEmpty()) {
                        newAlert.put("caregiverUid", connectedCaregiverUid);
                    }

                    FirebaseFirestore.getInstance().collection("emergency_alerts").document(newAlertId)
                            .set(newAlert)
                            .addOnSuccessListener(aVoid2 -> Log.d(TAG, "emergency_alerts doc written"))
                            .addOnFailureListener(err -> Log.e(TAG, "Failed to write to emergency_alerts", err));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload emergency log to Firestore", e);
                    Toast.makeText(this, "Local Alarm sounding. Offline status saved.", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchCurrentUserProfileName() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentName = doc.getString("name");
                        String firestoreLang = doc.getString("language");
                        if (firestoreLang != null && !firestoreLang.trim().isEmpty()) {
                            String normLang = LanguageManager.normalizeLanguageCode(firestoreLang);
                            String currentSessionLang = sessionManager.getLanguage();
                            if (!normLang.equals(currentSessionLang)) {
                                Log.i(TAG, "Syncing language preference from Firestore: " + normLang);
                                sessionManager.saveLanguage(normLang);
                                LanguageManager.setLocale(this, normLang);
                                updateKeyboardLabels();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch current user profile name", e));
    }

    private void searchUserByEmail() {
        if (connectedCaregiverUid != null && !connectedCaregiverUid.isEmpty()) {
            Toast.makeText(this, "You already have a connected caregiver. Please disconnect first.", Toast.LENGTH_LONG).show();
            return;
        }
        if (hasPendingRequest) {
            Toast.makeText(this, "You already have a pending caregiver connection request. Please cancel it first.", Toast.LENGTH_LONG).show();
            return;
        }

        String searchEmail = editSearchEmail.getText().toString().trim().toLowerCase();
        if (android.text.TextUtils.isEmpty(searchEmail)) {
            Toast.makeText(this, "Please enter an email address.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (searchEmail.equalsIgnoreCase(currentEmail)) {
            Toast.makeText(this, "You cannot search for yourself.", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingOverlay.setVisibility(View.VISIBLE);
        cardSearchResult.setVisibility(View.GONE);

        db.collection("users")
                .whereEqualTo("email", searchEmail)
                .get()
                .addOnCompleteListener(task -> {
                    loadingOverlay.setVisibility(View.GONE);
                    boolean found = false;

                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot doc = task.getResult().getDocuments().get(0);
                        foundUid = doc.getString("uid");
                        foundEmail = doc.getString("email");
                        foundName = doc.getString("name");
                        foundRole = doc.getString("role");
                        found = true;
                    } else {
                        // Offline/Local mock users fallback
                        if (searchEmail.equals("mcomshreya123@gmail.com") || searchEmail.equals("mcomshreya123@mail.com")) {
                            foundUid = "mock_caregiver_uid";
                            foundEmail = "mcomshreya123@gmail.com";
                            foundName = "Shreya (Caregiver)";
                            foundRole = RoleManager.ROLE_ADMIN_CAREGIVER;
                            found = true;
                        } else if (searchEmail.equals("stusukesh123@gmail.com")) {
                            foundUid = "mock_speech_impaired_uid";
                            foundEmail = "stusukesh123@gmail.com";
                            foundName = "Sukesh (Deaf User)";
                            foundRole = RoleManager.ROLE_SPEECH_IMPAIRED;
                            found = true;
                        } else if (searchEmail.equals("stuananya123@gmail.com")) {
                            foundUid = "mock_physically_disabled_uid";
                            foundEmail = "stuananya123@gmail.com";
                            foundName = "Ananya (Physically Disabled)";
                            foundRole = RoleManager.ROLE_PHYSICALLY_DISABLED;
                            found = true;
                        }
                    }

                    if (found) {
                        // Role Verification Guard
                        if (RoleManager.ROLE_ADMIN_CAREGIVER.equals(currentRole)) {
                            if (RoleManager.ROLE_ADMIN_CAREGIVER.equals(foundRole)) {
                                Toast.makeText(this, "Caregivers can only request connection with deaf or physically disabled users.", Toast.LENGTH_LONG).show();
                                return;
                            }
                        } else {
                            if (!RoleManager.ROLE_ADMIN_CAREGIVER.equals(foundRole)) {
                                Toast.makeText(this, "You can only request connection with caregivers.", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }

                        // Display result
                        txtResultName.setText(foundName != null ? foundName : "User");
                        txtResultEmail.setText(foundEmail);
                        txtResultRole.setText("Role: " + (foundRole != null ? foundRole : "N/A"));
                        cardSearchResult.setVisibility(View.VISIBLE);
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
        connection.put("timestamp", com.google.firebase.Timestamp.now());

        // Unification for Caregiver Console compatibility
        if (RoleManager.ROLE_ADMIN_CAREGIVER.equals(currentRole)) {
            connection.put("caregiverUid", currentUid);
            connection.put("caregiverEmail", currentEmail);
            connection.put("patientUid", foundUid);
            connection.put("patientName", foundName);
            connection.put("patientEmail", foundEmail);
        } else {
            connection.put("patientUid", currentUid);
            connection.put("patientName", currentName != null && !currentName.isEmpty() ? currentName : "User");
            connection.put("patientEmail", currentEmail);
            connection.put("caregiverUid", foundUid);
            connection.put("caregiverEmail", foundEmail);
        }

        // Save locally instantly
        LocalConnectionSimulator.saveConnection(this, connection);
        cardSearchResult.setVisibility(View.GONE);
        editSearchEmail.setText("");
        Toast.makeText(this, "Connection request sent!", Toast.LENGTH_SHORT).show();
        refreshConnectionState();

        // Run online write in background
        db.collection("caregiver_connections")
                .add(connection)
                .addOnFailureListener(e -> Log.e(TAG, "Background Firestore write failed", e));
    }

    private void refreshConnectionState() {
        loadActiveConnections();
        loadPendingRequests();
    }

    private void updateSearchCardVisibility() {
        if (cardSearchSection == null) return;
        boolean hasCaregiver = (connectedCaregiverUid != null && !connectedCaregiverUid.isEmpty());
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
                    DocumentSnapshot activeConnection = null;
                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot result = task.getResult();
                        for (DocumentSnapshot doc : result.getDocuments()) {
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
                                activeConnection = doc;
                                break;
                            }
                        }
                    }

                    if (activeConnection != null) {
                        final String docId = activeConnection.getId();
                        String senderUid = activeConnection.getString("senderUid");
                        if (senderUid == null || senderUid.isEmpty()) senderUid = activeConnection.getString("patientUid");
                        String receiverUid = activeConnection.getString("receiverUid");
                        if (receiverUid == null || receiverUid.isEmpty()) receiverUid = activeConnection.getString("caregiverUid");
                        String senderEmail = activeConnection.getString("senderEmail");
                        if (senderEmail == null || senderEmail.isEmpty()) senderEmail = activeConnection.getString("patientEmail");
                        String receiverEmail = activeConnection.getString("receiverEmail");
                        if (receiverEmail == null || receiverEmail.isEmpty()) receiverEmail = activeConnection.getString("caregiverEmail");
                        String senderName = activeConnection.getString("senderName");
                        if (senderName == null || senderName.isEmpty()) senderName = activeConnection.getString("patientName");
                        String receiverName = activeConnection.getString("receiverName");
                        if (receiverName == null || receiverName.isEmpty()) receiverName = activeConnection.getString("caregiverName");
                        String senderRole = activeConnection.getString("senderRole");
                        if (senderRole == null || senderRole.isEmpty()) senderRole = activeConnection.getString("patientRole");
                        String receiverRole = activeConnection.getString("receiverRole");
                        if (receiverRole == null || receiverRole.isEmpty()) receiverRole = activeConnection.getString("caregiverRole");

                        processActiveCaregiver(docId, senderUid, receiverUid, senderEmail, receiverEmail, senderName, receiverName, senderRole, receiverRole);
                        return;
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

                    // No active connection found
                    cardConnectedUser.setVisibility(View.GONE);
                    connectedCaregiverUid = null;
                    caregiverEmail = "";
                    startCaregiverChatListener(null);
                    if (editCaregiverEmail != null) {
                        editCaregiverEmail.setText("");
                        editCaregiverEmail.setHint("Not Connected to any Caregiver");
                    }
                    updateSearchCardVisibility();
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
            otherRole = senderRole != null ? senderRole : RoleManager.ROLE_SPEECH_IMPAIRED;
        }

        // Mock caregiver UID resolution if email is available
        if (("mock_caregiver_uid".equals(otherUid) || otherUid == null || otherUid.isEmpty())
                && otherEmail != null && !otherEmail.trim().isEmpty()) {
            final String finalDocId = docId;
            final String finalOtherName = otherName;
            final String finalOtherEmail = otherEmail;
            final String finalOtherRole = otherRole;
            final String fallbackUid = (otherUid != null && !otherUid.isEmpty()) ? otherUid : "mock_caregiver_uid";

            db.collection("users")
                    .whereEqualTo("email", otherEmail.toLowerCase().trim())
                    .whereEqualTo("role", RoleManager.ROLE_ADMIN_CAREGIVER)
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
                                    LocalConnectionSimulator.updateConnectionUid(SpeechImpairedDashboardActivity.this, finalDocId, realUid);
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
        if (RoleManager.ROLE_ADMIN_CAREGIVER.equals(currentRole)) {
            txtConnectedTitle.setText(getString(R.string.connected_patient_title));
        } else {
            txtConnectedTitle.setText(getString(R.string.connected_caregiver_title));
        }

        txtConnectedName.setText(otherName);
        txtConnectedEmail.setText(otherEmail);
        String locOtherRole = RoleManager.ROLE_ADMIN_CAREGIVER.equals(otherRole) ? getString(R.string.role_caregiver) : getString(R.string.role_speech_impaired);
        txtConnectedRole.setText("Role: " + locOtherRole);

        caregiverEmail = otherEmail;
        connectedCaregiverUid = otherUid;

        // Persist active connection to SpeechSettings
        if (connectedCaregiverUid != null && !connectedCaregiverUid.trim().isEmpty()) {
            getSharedPreferences("SpeechSettings", MODE_PRIVATE).edit()
                    .putString("connectedCaregiverUid", connectedCaregiverUid)
                    .putString("caregiverEmail", caregiverEmail != null ? caregiverEmail : "")
                    .apply();
        }

        final String finalOtherUid = otherUid;
        final String finalOtherName = otherName;
        final String finalOtherRole = otherRole;
        final String finalOtherEmail = otherEmail;
        btnOpenFullChat.setOnClickListener(v -> {
            if (bottomNav != null) {
                bottomNav.removeBadge(R.id.nav_home);
            }
            if (latestCaregiverDocs != null) {
                markCaregiverMessagesAsRead(latestCaregiverDocs);
            }
            Intent intent = new Intent(SpeechImpairedDashboardActivity.this, ChatActivity.class);
            intent.putExtra("receiverId", finalOtherUid);
            intent.putExtra("receiverName", finalOtherName);
            intent.putExtra("receiverRole", finalOtherRole);
            intent.putExtra("receiverEmail", finalOtherEmail);
            startActivity(intent);
        });

        btnDisconnect.setOnClickListener(v -> disconnectUser(docId));
        cardConnectedUser.setVisibility(View.VISIBLE);
        startCaregiverChatListener(otherUid);
        updateSearchCardVisibility();
    }

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

    private void cancelOutgoingRequest(String docId) {
        LocalConnectionSimulator.deleteConnection(this, docId);
        Toast.makeText(this, "Connection request cancelled.", Toast.LENGTH_SHORT).show();
        refreshConnectionState();

        db.collection("caregiver_connections").document(docId)
                .delete()
                .addOnFailureListener(e -> Log.e(TAG, "Background cancel failed", e));
    }

    private void loadPendingRequests() {
        layoutReceivedRequestsList.removeAllViews();

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
                        txtPendingSectionTitle.setVisibility(View.GONE);
                        updateSearchCardVisibility();
                        return;
                    }

                    hasPendingRequest = true;
                    txtPendingSectionTitle.setVisibility(View.VISIBLE);

                    for (Map<String, Object> doc : requestsList) {
                        final String docId = (String) doc.get("docId");
                        String sender = (String) doc.get("senderUid");
                        String senderEmail = (String) doc.get("senderEmail");
                        
                        String name, email, role;
                        boolean isOutgoing = currentUid.equals(sender) || currentEmail.equalsIgnoreCase(senderEmail);

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
                            String locRole = RoleManager.ROLE_ADMIN_CAREGIVER.equals(role) ? getString(R.string.role_caregiver) : getString(R.string.role_speech_impaired);
                            txtRole.setText("Role: " + locRole);
                            btnAccept.setText(getString(R.string.accept_request));
                            btnReject.setText(getString(R.string.reject_request));
                            btnAccept.setOnClickListener(v -> acceptRequest(docId));
                            btnReject.setOnClickListener(v -> rejectRequest(docId));
                        }

                        layoutReceivedRequestsList.addView(itemView);
                    }
                    updateSearchCardVisibility();
                });
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

    private void disconnectUser(String docId) {
        LocalConnectionSimulator.deleteConnection(this, docId);
        getSharedPreferences("SpeechSettings", MODE_PRIVATE).edit()
                .remove("connectedCaregiverUid")
                .remove("caregiverEmail")
                .apply();
        connectedCaregiverUid = null;
        caregiverEmail = "";
        Toast.makeText(this, "Disconnected successfully.", Toast.LENGTH_SHORT).show();
        refreshConnectionState();

        if (docId != null && !docId.isEmpty()) {
            db.collection("caregiver_connections").document(docId)
                    .delete()
                    .addOnFailureListener(e -> Log.e(TAG, "Background disconnect failed", e));
        }
    }


    // --- Spoken History SharedPreferences Helpers ---

    private List<String> getSpokenHistory() {
        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String normLang = LanguageManager.normalizeLanguageCode(userLang);
        SharedPreferences historyPref = getSharedPreferences("SpeechHistory", MODE_PRIVATE);
        String historyStr = historyPref.getString("history_" + normLang, "");
        List<String> list = new ArrayList<>();
        if (!historyStr.isEmpty()) {
            String[] parts = historyStr.split("\\|\\|");
            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    list.add(part);
                }
            }
        }
        return list;
    }

    private void saveSpokenHistory(String phrase) {
        if (phrase == null || phrase.trim().isEmpty()) return;
        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String normLang = LanguageManager.normalizeLanguageCode(userLang);
        List<String> list = getSpokenHistory();
        list.remove(phrase.trim());
        list.add(0, phrase.trim());
        if (list.size() > 10) {
            list = list.subList(0, 10);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append("||");
            }
        }
        getSharedPreferences("SpeechHistory", MODE_PRIVATE).edit()
                .putString("history_" + normLang, sb.toString())
                .apply();
    }

    private void clearSpokenHistory() {
        getSharedPreferences("SpeechHistory", MODE_PRIVATE).edit()
                .remove("history")
                .apply();
    }


    /**
     * Helper method to perform logout.
     */
    private void performLogout() {
        if (currentUid != null && !currentUid.isEmpty()) {
            FirebaseFirestore.getInstance().collection("users")
                    .document(currentUid)
                    .update("online", false, "lastSeen", com.google.firebase.Timestamp.now(), "typingTo", null);
        }
        com.kannada.speechassistant.voiceassistant.WakeWordManager.getInstance(this).stopListening();
        if (deafAssistantResponseManager != null) {
            deafAssistantResponseManager.dismiss();
        }
        sessionManager.logoutUser();
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
        RoleManager.redirectToLogin(this);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        com.kannada.speechassistant.voiceassistant.WakeWordManager.getInstance(this).pauseListening();
        if (isRecording) {
            stopSpeechRecognition();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        com.kannada.speechassistant.voiceassistant.WakeWordManager.getInstance(this).stopListening();
        if (deafAssistantResponseManager != null) {
            deafAssistantResponseManager.dismiss();
        }
        if (currentUid != null && !currentUid.isEmpty()) {
            FirebaseFirestore.getInstance().collection("users")
                    .document(currentUid)
                    .update("online", false, "lastSeen", com.google.firebase.Timestamp.now(), "typingTo", null);
        }
        if (caregiverPresenceListener != null) {
            caregiverPresenceListener.remove();
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        if (connectionsListener != null) {
            connectionsListener.remove();
        }
        if (caregiverChatListener != null) {
            caregiverChatListener.remove();
        }
        if (networkExecutor != null) {
            networkExecutor.shutdown();
        }
        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {}
            speechRecognizer = null;
        }
    }

    // --- Speech-to-Text (Voice input) Implementation ---

    private void checkAndStartSpeechRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            runOnUiThread(() -> {
                layoutHomeTranslitStatus.setVisibility(View.VISIBLE);
                progressHomeTranslit.setVisibility(View.GONE);
                txtHomeTranslitStatus.setText("⚠ Speech recognition service unavailable on this device.");
                txtHomeTranslitStatus.setTextColor(Color.RED);
                AccessibleMicFeedbackManager.triggerShortVibration(SpeechImpairedDashboardActivity.this);
                scheduleStatusReset();
                Toast.makeText(SpeechImpairedDashboardActivity.this, "Speech recognition is not available on this device.", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        AccessibleMicFeedbackManager.triggerShortVibration(this);
        startSpeechRecognition();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                AccessibleMicFeedbackManager.triggerShortVibration(this);
                startSpeechRecognition();
            } else {
                runOnUiThread(() -> {
                    layoutHomeTranslitStatus.setVisibility(View.VISIBLE);
                    progressHomeTranslit.setVisibility(View.GONE);
                    txtHomeTranslitStatus.setText("⚠ " + AccessibleMicFeedbackManager.getPermissionRequiredText(userLang));
                    txtHomeTranslitStatus.setTextColor(Color.RED);
                    AccessibleMicFeedbackManager.triggerShortVibration(SpeechImpairedDashboardActivity.this);
                    scheduleStatusReset();
                });
            }
        } else if (requestCode == com.kannada.speechassistant.voiceassistant.AppVoiceAssistant.REQUEST_CODE_VOICE_ASSISTANT_PERMISSION) {
            com.kannada.speechassistant.voiceassistant.AppVoiceAssistant.getInstance(this)
                    .handlePermissionsResult(this, requestCode, grantResults, findViewById(R.id.btnVoiceAssistant));
        }
    }

    private void startSpeechRecognition() {
        if (isRecording) {
            stopSpeechRecognition();
            return;
        }

        isRecording = true;
        com.kannada.speechassistant.voiceassistant.WakeWordManager.getInstance(this).pauseListening();

        if (idleResetRunnable != null) {
            idleResetHandler.removeCallbacks(idleResetRunnable);
        }

        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {}
            speechRecognizer = null;
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create SpeechRecognizer", e);
            AccessibleMicFeedbackManager.resetSessionState();
            isRecording = false;
            com.kannada.speechassistant.voiceassistant.WakeWordManager.getInstance(this).resumeListening(this);
            runOnUiThread(() -> {
                txtHomeTranslitStatus.setText("⚠ Failed to create speech recognizer.");
                txtHomeTranslitStatus.setTextColor(Color.RED);
                scheduleStatusReset();
            });
            return;
        }

        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        final String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        final String speechTag = LanguageManager.getSpeechLanguageTag(userLang);

        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechTag);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, speechTag);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

        // Generous silence lengths for natural pauses and regional accents
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                runOnUiThread(() -> {
                    layoutHomeTranslitStatus.setVisibility(View.VISIBLE);
                    progressHomeTranslit.setVisibility(View.GONE);
                    String langDisplay = LanguageManager.getLanguageDisplayName(userLang);
                    txtHomeTranslitStatus.setText("🎤 Listening (" + langDisplay + ")... Speak now");
                    txtHomeTranslitStatus.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));

                    editHomeQuickText.setHint("Listening in " + langDisplay + "...");
                    startMicPulseAnimation();
                });
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {
                runOnUiThread(() -> {
                    float scale = 1.0f + Math.max(0.0f, rmsdB / 10.0f) * 0.25f;
                    if (btnMicInput != null) {
                        btnMicInput.setScaleX(scale);
                        btnMicInput.setScaleY(scale);
                    }
                });
            }

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                runOnUiThread(() -> {
                    stopMicPulseAnimation();
                    progressHomeTranslit.setVisibility(View.VISIBLE);
                    txtHomeTranslitStatus.setText("Processing speech...");
                });
            }

            @Override
            public void onError(int error) {
                runOnUiThread(() -> {
                    isRecording = false;
                    stopMicPulseAnimation();
                    progressHomeTranslit.setVisibility(View.GONE);
                    com.kannada.speechassistant.voiceassistant.WakeWordManager.getInstance(SpeechImpairedDashboardActivity.this).resumeListening(SpeechImpairedDashboardActivity.this);

                    AccessibleMicFeedbackManager.triggerShortVibration(SpeechImpairedDashboardActivity.this);

                    String errorMsg;
                    switch (error) {
                        case SpeechRecognizer.ERROR_NO_MATCH:
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                            errorMsg = "⚠ " + AccessibleMicFeedbackManager.getNoSpeechErrorText(userLang);
                            break;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                            errorMsg = "⚠ " + AccessibleMicFeedbackManager.getPermissionRequiredText(userLang);
                            break;
                        case SpeechRecognizer.ERROR_NETWORK:
                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                            errorMsg = "⚠ Network error. Please check internet connection.";
                            break;
                        case SpeechRecognizer.ERROR_AUDIO:
                            errorMsg = "⚠ Audio recording error. Please check microphone.";
                            break;
                        case SpeechRecognizer.ERROR_SERVER:
                        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                            errorMsg = "⚠ Speech recognition service busy. Please try again.";
                            break;
                        case 12: // SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED (API 33+)
                        case 13: // SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE (API 33+)
                            errorMsg = "⚠ " + LanguageManager.getLanguageDisplayName(userLang) + " speech package not available on device.";
                            break;
                        default:
                            errorMsg = "⚠ Speech recognition error (" + error + "). Please retry.";
                            break;
                    }

                    txtHomeTranslitStatus.setText(errorMsg);
                    txtHomeTranslitStatus.setTextColor(Color.RED);
                    editHomeQuickText.setHint(getString(R.string.quick_text_hint));
                    scheduleStatusReset();
                });
            }

            @Override
            public void onResults(Bundle results) {
                runOnUiThread(() -> {
                    isRecording = false;
                    stopMicPulseAnimation();
                    progressHomeTranslit.setVisibility(View.GONE);
                    com.kannada.speechassistant.voiceassistant.WakeWordManager.getInstance(SpeechImpairedDashboardActivity.this).resumeListening(SpeechImpairedDashboardActivity.this);

                    if (results != null) {
                        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (matches != null && !matches.isEmpty()) {
                            String recognizedText = matches.get(0);
                            if (recognizedText != null && !recognizedText.trim().isEmpty()) {
                                String cleanText = recognizedText.trim();

                                editHomeQuickText.setText(cleanText);
                                editHomeQuickText.setSelection(cleanText.length());

                                AccessibleMicFeedbackManager.triggerShortVibration(SpeechImpairedDashboardActivity.this);
                                saveSpokenHistory(cleanText);
                                updateHomeRecentPhrasesList();

                                txtHomeTranslitStatus.setText("✓ Speech recognized (" + LanguageManager.getLanguageDisplayName(userLang) + ")");
                                txtHomeTranslitStatus.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
                                editHomeQuickText.setHint(getString(R.string.quick_text_hint));
                                scheduleStatusReset();
                                return;
                            }
                        }
                    }

                    txtHomeTranslitStatus.setText("⚠ " + AccessibleMicFeedbackManager.getNoSpeechErrorText(userLang));
                    txtHomeTranslitStatus.setTextColor(Color.RED);
                    AccessibleMicFeedbackManager.triggerShortVibration(SpeechImpairedDashboardActivity.this);
                    editHomeQuickText.setHint(getString(R.string.quick_text_hint));
                    scheduleStatusReset();
                });
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                if (partialResults == null) return;
                ArrayList<String> partialMatches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (partialMatches != null && !partialMatches.isEmpty()) {
                    String partialText = partialMatches.get(0);
                    if (partialText != null && !partialText.trim().isEmpty()) {
                        runOnUiThread(() -> {
                            editHomeQuickText.setText(partialText);
                            editHomeQuickText.setSelection(partialText.length());
                        });
                    }
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(speechRecognizerIntent);
    }

    private void stopSpeechRecognition() {
        isRecording = false;
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
            } catch (Exception ignored) {}
        }
        com.kannada.speechassistant.voiceassistant.WakeWordManager.getInstance(this).resumeListening(this);
        runOnUiThread(() -> {
            stopMicPulseAnimation();
            progressHomeTranslit.setVisibility(View.GONE);
            editHomeQuickText.setHint(getString(R.string.quick_text_hint));
            resetStatusToDefault();
        });
    }

    private void startMicPulseAnimation() {
        if (btnMicInput == null) return;
        
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(Color.parseColor("#FEE2E2")); // Red pulse background
        btnMicInput.setBackground(shape);
        btnMicInput.setImageTintList(ColorStateList.valueOf(Color.parseColor("#EF4444"))); // Red microphone icon tint

        micScaleXAnimator = ObjectAnimator.ofFloat(btnMicInput, "scaleX", 1.0f, 1.15f, 1.0f);
        micScaleYAnimator = ObjectAnimator.ofFloat(btnMicInput, "scaleY", 1.0f, 1.15f, 1.0f);
        
        micScaleXAnimator.setDuration(1200);
        micScaleXAnimator.setRepeatCount(ValueAnimator.INFINITE);
        micScaleYAnimator.setDuration(1200);
        micScaleYAnimator.setRepeatCount(ValueAnimator.INFINITE);

        micScaleXAnimator.start();
        micScaleYAnimator.start();
    }

    private void stopMicPulseAnimation() {
        if (btnMicInput == null) return;
        
        if (micScaleXAnimator != null) {
            micScaleXAnimator.cancel();
            micScaleXAnimator = null;
        }
        if (micScaleYAnimator != null) {
            micScaleYAnimator.cancel();
            micScaleYAnimator = null;
        }
        
        btnMicInput.setScaleX(1.0f);
        btnMicInput.setScaleY(1.0f);
        btnMicInput.setBackgroundResource(0);
        
        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
        btnMicInput.setBackgroundResource(outValue.resourceId);
        btnMicInput.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.text_secondary, getTheme())));
    }

    /**
     * Triggers distinct vibration patterns for blind-user accessibility feedback.
     *
     * HAPTIC_MIC_START  → double-buzz  (tap-tap): "I'm listening"
     * HAPTIC_MIC_STOP   → single long : "Stopped"
     * HAPTIC_SUCCESS    → triple short : "Got it!"
     * HAPTIC_ERROR      → one long harsh: "Try again"
     */
    private void triggerHapticFeedback(int type) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        VibrationEffect effect;
        switch (type) {
            case HAPTIC_MIC_START:
                // Double buzz: tap-tap → "Recording started"
                effect = VibrationEffect.createWaveform(
                        new long[]{0, 80, 70, 80}, VibrationEffect.DEFAULT_AMPLITUDE);
                break;
            case HAPTIC_MIC_STOP:
                // Single medium buzz → "Recording stopped"
                effect = VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE);
                break;
            case HAPTIC_SUCCESS:
                // Triple quick taps → "Text recognised!"
                effect = VibrationEffect.createWaveform(
                        new long[]{0, 50, 40, 50, 40, 50}, VibrationEffect.DEFAULT_AMPLITUDE);
                break;
            case HAPTIC_ERROR:
            default:
                // One long harsh buzz → "Error / no input"
                effect = VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE);
                break;
        }
        vibrator.vibrate(effect);
    }

    private void scheduleStatusReset() {
        if (idleResetRunnable != null) {
            idleResetHandler.removeCallbacks(idleResetRunnable);
        }
        idleResetRunnable = this::resetStatusToDefault;
        idleResetHandler.postDelayed(idleResetRunnable, 2500);
    }

    private void resetStatusToDefault() {
        boolean isKannadaMode = (toggleKeyboardGroup.getCheckedButtonId() == R.id.btnToggleKannada);
        if (isKannadaMode) {
            layoutHomeTranslitStatus.setVisibility(View.GONE);
        } else {
            layoutHomeTranslitStatus.setVisibility(View.VISIBLE);
            progressHomeTranslit.setVisibility(View.GONE);
            txtHomeTranslitStatus.setText("English Translit active. (Auto-converting)");
            txtHomeTranslitStatus.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
        }
    }

    private void startCaregiverChatListener(String caregiverUid) {
        if (caregiverChatListener != null) {
            caregiverChatListener.remove();
            caregiverChatListener = null;
        }

        if (caregiverPresenceListener != null) {
            caregiverPresenceListener.remove();
            caregiverPresenceListener = null;
        }

        if (caregiverUid == null || caregiverUid.isEmpty()) {
            connectedCaregiverUid = null;
            if (layoutCaregiverChatEmptyState != null) {
                layoutCaregiverChatEmptyState.setVisibility(View.VISIBLE);
            }
            if (rvCaregiverChatMessages != null) {
                rvCaregiverChatMessages.setVisibility(View.GONE);
            }
            return;
        }

        connectedCaregiverUid = caregiverUid;

        // Listen to caregiver profile presence
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

        // Generate deterministic chatId
        String chatId;
        if (currentUid.compareTo(caregiverUid) < 0) {
            chatId = currentUid + "_" + caregiverUid;
        } else {
            chatId = caregiverUid + "_" + currentUid;
        }

        Query chatQuery = db.collection("caregiver_messages")
                .whereEqualTo("chatId", chatId)
                .orderBy("timestamp", Query.Direction.ASCENDING);

        caregiverChatListener = chatQuery.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Caregiver chat listener failed", error);
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
                            Math.abs(m.getTimestamp().toDate().getTime() - ((com.google.firebase.Timestamp)local.get("timestamp")).toDate().getTime()) < 5000) {
                            dup = true;
                            break;
                        }
                    }
                    if (!dup) {
                        chatMsgs.add(new ChatMessage(local));
                    }
                }



                // Auto-play incoming caregiver messages loud through speaker & custom ringtone
                boolean isFirstLoad = autoPlayedVoiceMsgIds.isEmpty();
                for (ChatMessage m : chatMsgs) {
                    if (!currentUid.equals(m.getSenderId())) {
                        if (!autoPlayedVoiceMsgIds.contains(m.getId())) {
                            autoPlayedVoiceMsgIds.add(m.getId());
                            if (!isFirstLoad) {
                                CaregiverSoundManager.handleIncomingMessage(this, m.getId(), m.getSenderId(), m.getMessage(), m.getType(), "", "Caregiver");
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
                        rvCaregiverChatMessages.scrollToPosition(chatMsgs.size() - 1);
                    }
                }

                latestCaregiverDocs = docs;

                // Visual Unread Indicator & Read Status Updates
                int unreadCount = 0;
                for (ChatMessage m : chatMsgs) {
                    if (!currentUid.equals(m.getSenderId()) && !m.isSeen() && !"read".equalsIgnoreCase(m.getStatus())) {
                        unreadCount++;
                    }
                }

                if (bottomNav != null) {
                    if (unreadCount > 0 && layoutHome != null && layoutHome.getVisibility() != View.VISIBLE) {
                        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_home);
                        badge.setVisible(true);
                        badge.setNumber(unreadCount);
                    } else {
                        bottomNav.removeBadge(R.id.nav_home);
                    }
                }

                if (layoutHome != null && layoutHome.getVisibility() == View.VISIBLE) {
                    markCaregiverMessagesAsRead(docs);
                }
            }
        });
    }

    private void markCaregiverMessagesAsRead(List<DocumentSnapshot> docs) {
        if (docs == null || db == null || currentUid == null) return;
        WriteBatch batch = db.batch();
        boolean hasUpdates = false;
        for (DocumentSnapshot doc : docs) {
            String senderId = doc.getString("senderId");
            String status = doc.getString("status");
            if (senderId != null && !senderId.equals(currentUid) && !"read".equalsIgnoreCase(status)) {
                batch.update(doc.getReference(), "status", "read");
                batch.update(doc.getReference(), "seen", true);
                batch.update(doc.getReference(), "readStatus", true);
                hasUpdates = true;
            }
        }
        if (hasUpdates) {
            batch.commit().addOnFailureListener(e -> Log.e(TAG, "Failed to mark caregiver messages as read", e));
        }
    }

    private void speakIncomingVoiceMessageLoud(String text) {
        if (android.text.TextUtils.isEmpty(text)) return;

        // 1. Synchronize audio volume with Master App Volume
        CaregiverSoundManager.applySystemStreamVolume(this, CaregiverSoundManager.getVolumePercent(this));

        // 2. Hardware vibration pulse
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    v.vibrate(android.os.VibrationEffect.createOneShot(600, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(600);
                }
            }
        } catch (Exception ex) {}

        // 3. Localized Toast alert
        Toast.makeText(this, "📢 Voice Message: " + text, Toast.LENGTH_LONG).show();

        // 4. Speak out loud in detected language via TTS
        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String langName = LanguageManager.detectLanguageFromText(text, userLang);
        java.util.Locale locale = LanguageManager.getLocale(langName);
        if (tts != null && isTtsInitialized) {
            tts.setLanguage(locale);
            CaregiverSoundManager.speakWithVolume(tts, text, "CaregiverVoiceMsgReadout_" + System.currentTimeMillis(), this);
        } else {
            speakGoogleTts(text);
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
                rvCaregiverChatMessages.scrollToPosition(chatMsgs.size() - 1);
            }
        }
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

    private void sendEmergencyChatMessage(String msg) {
        if (connectedCaregiverUid == null || connectedCaregiverUid.isEmpty()) {
            SharedPreferences settingsPref = getSharedPreferences("SpeechSettings", MODE_PRIVATE);
            connectedCaregiverUid = settingsPref.getString("connectedCaregiverUid", null);
            if (connectedCaregiverUid == null || connectedCaregiverUid.isEmpty()) {
                return;
            }
        }

        String chatId = currentUid.compareTo(connectedCaregiverUid) < 0 ? currentUid + "_" + connectedCaregiverUid : connectedCaregiverUid + "_" + currentUid;

        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String lang = LanguageManager.detectLanguageFromText(msg, userLang);

        Map<String, Object> chatMsgMap = new HashMap<>();
        chatMsgMap.put("chatId", chatId);
        chatMsgMap.put("senderId", currentUid);
        chatMsgMap.put("senderUid", currentUid);
        chatMsgMap.put("receiverId", connectedCaregiverUid);
        chatMsgMap.put("recipientEmail", caregiverEmail != null ? caregiverEmail.toLowerCase().trim() : "");
        chatMsgMap.put("senderRole", currentRole != null ? currentRole : "Speech-Impaired User");
        chatMsgMap.put("receiverRole", "Admin/Caregiver");
        chatMsgMap.put("message", msg);
        chatMsgMap.put("messageText", msg);
        chatMsgMap.put("language", lang);
        chatMsgMap.put("messageType", "emergency");
        chatMsgMap.put("type", "emergency");
        chatMsgMap.put("priority", "high");
        chatMsgMap.put("acknowledged", false);
        chatMsgMap.put("status", "sent");
        chatMsgMap.put("readStatus", false);
        chatMsgMap.put("delivered", false);
        chatMsgMap.put("seen", false);
        chatMsgMap.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("caregiver_messages")
                .add(chatMsgMap)
                .addOnSuccessListener(ref -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("messageId", ref.getId());
                    updates.put("status", "sent");
                    updates.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    ref.update(updates);

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        ref.update("status", "delivered", "delivered", true);
                    }, 1000);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to send emergency message", e));
    }

    private void showNotificationSettingsDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notification_settings, null);
        builder.setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        txtActiveRingtoneDialogName = dialogView.findViewById(R.id.txtCurrentRingtoneName);
        com.google.android.material.button.MaterialButton btnSelectFileRingtone = dialogView.findViewById(R.id.btnSelectFileRingtone);
        com.google.android.material.button.MaterialButton btnTestPlayRingtone = dialogView.findViewById(R.id.btnTestPlayRingtone);
        com.google.android.material.button.MaterialButton btnResetDefaultRingtone = dialogView.findViewById(R.id.btnResetDefaultRingtone);
        btnDialogSilentToggle = btnResetDefaultRingtone;
        SeekBar sbNotificationVolume = dialogView.findViewById(R.id.sbNotificationVolume);
        TextView txtVolumePercentage = dialogView.findViewById(R.id.txtVolumePercentage);

        com.google.android.material.button.MaterialButton btnSaveNotificationSettings = dialogView.findViewById(R.id.btnSaveNotificationSettings);

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
                    CaregiverSoundManager.saveNotificationVolumePercent(SpeechImpairedDashboardActivity.this, currentUid, progress);
                    txtActiveRingtoneDialogName.setText(CaregiverSoundManager.getActiveRingtoneName(SpeechImpairedDashboardActivity.this, currentUid));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                CaregiverSoundManager.saveNotificationVolumePercent(SpeechImpairedDashboardActivity.this, currentUid, seekBar.getProgress());
                txtActiveRingtoneDialogName.setText(CaregiverSoundManager.getActiveRingtoneName(SpeechImpairedDashboardActivity.this, currentUid));
                CaregiverSoundManager.testPlaySound(SpeechImpairedDashboardActivity.this, currentUid);
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
                    Toast.makeText(SpeechImpairedDashboardActivity.this, "File Manager not found on device", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnTestPlayRingtone.setOnClickListener(v -> {
            CaregiverSoundManager.testPlaySound(SpeechImpairedDashboardActivity.this, currentUid);
        });

        btnResetDefaultRingtone.setOnClickListener(v -> {
            CaregiverSoundManager.toggleSilentMode(SpeechImpairedDashboardActivity.this, currentUid);
            boolean nowSilent = CaregiverSoundManager.isSilentMode(SpeechImpairedDashboardActivity.this, currentUid);
            if (nowSilent) {
                btnResetDefaultRingtone.setText("🔔 Enable Sound");
                Toast.makeText(SpeechImpairedDashboardActivity.this, "🔇 Silent Mode ON (Sound OFF, Vibration ON)", Toast.LENGTH_SHORT).show();
            } else {
                btnResetDefaultRingtone.setText("🔇 Clear (Silent)");
                Toast.makeText(SpeechImpairedDashboardActivity.this, "🔔 Notification Sound Enabled", Toast.LENGTH_SHORT).show();
                CaregiverSoundManager.testPlaySound(SpeechImpairedDashboardActivity.this, currentUid);
            }
            txtActiveRingtoneDialogName.setText(CaregiverSoundManager.getActiveRingtoneName(SpeechImpairedDashboardActivity.this, currentUid));
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
                        CaregiverSoundManager.saveTtsVolumePercent(SpeechImpairedDashboardActivity.this, currentUid, progress);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    CaregiverSoundManager.saveTtsVolumePercent(SpeechImpairedDashboardActivity.this, currentUid, seekBar.getProgress());
                    CaregiverSoundManager.testPlayTts(SpeechImpairedDashboardActivity.this, currentUid);
                }
            });
        }

        btnSaveNotificationSettings.setOnClickListener(v -> {
            CaregiverSoundManager.saveNotificationVolumePercent(SpeechImpairedDashboardActivity.this, currentUid, sbNotificationVolume.getProgress());
            if (sbTtsVolume != null) {
                CaregiverSoundManager.saveTtsVolumePercent(SpeechImpairedDashboardActivity.this, currentUid, sbTtsVolume.getProgress());
            }
            CaregiverSoundManager.stopNotificationSound(SpeechImpairedDashboardActivity.this);
            Toast.makeText(SpeechImpairedDashboardActivity.this, "Notification sound & volume saved successfully!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.setOnDismissListener(d -> CaregiverSoundManager.stopNotificationSound(SpeechImpairedDashboardActivity.this));
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

    @Override
    protected void onResume() {
        super.onResume();
        CaregiverSoundManager.stopNotificationSound(this);
        CaregiverSoundManager.stopEmergencySound(this);
        updateKeyboardTabLabels();
        updateKeyboardLabels();

        // Hands-free Wake Word listening for Deaf User
        com.kannada.speechassistant.voiceassistant.WakeWordManager.getInstance(this).startListening(this);
    }
}
