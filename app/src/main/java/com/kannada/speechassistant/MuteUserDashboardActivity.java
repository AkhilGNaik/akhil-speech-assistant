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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

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
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer;
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmark;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import com.google.firebase.Timestamp;

/**
 * Controller class for the Mute User Dashboard.
 * Includes Bottom Navigation for transitioning across Home, Speech, Profile, and Caregiver tabs.
 * Supports Google TTS, Coqui TTS, emergency alarms, and Firestore integrations.
 */
public class MuteUserDashboardActivity extends AppCompatActivity {

    private static final String TAG = "MuteUserDashboard";

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
    private MaterialButton btnHomeSpeak;
    private MaterialCardView cardEmergency;
    private MaterialCardView cardQuickWater;
    private MaterialCardView cardQuickFood;
    private MaterialCardView cardQuickHelp;
    private MaterialCardView cardQuickRest;
    private MaterialCardView cardQuickOutside;
    private MaterialCardView cardQuickToilet;
    private MaterialCardView cardQuickQuiet;

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
    private long lastSpeakClickTime = 0;
    private long lastSendClickTime = 0;
    private android.os.Vibrator vibrator;
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

    // Hand Gesture Recognition Components
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    private ExecutorService cameraExecutor;
    private float gestureThreshold = 0.6f;
    private int cameraLensFacing = CameraSelector.LENS_FACING_FRONT;
    private boolean isTrackingEnabled = true;
    private GestureRecognizer gestureRecognizer;
    private boolean isMediaPipeInitialized = false;

    // Gesture UI Widgets (Bottom of Home Tab)
    private GestureOverlayView gestureOverlayView = null;
    private SwitchMaterial switchGestureDetection;
    private View gridOutputs;
    private View cardDeveloperSandbox;
    private TextView txtGestureName;
    private TextView txtGestureConfidence;
    private TextView txtGestureAction;
    private TextView txtDetectionStatus;
    private TextView txtKannadaText;
    private TextView lblTranslatedText;
    private TextView txtMessageSentStatus;
    private TextView txtDetectionTime;
    private LinearLayout bannerOfflineSimulator;

    // Gesture Simulator Buttons
    private MaterialButton btnSimulateThumbsUp;
    private MaterialButton btnSimulateThumbsDown;
    private MaterialButton btnSimulateOpenPalm;
    private MaterialButton btnSimulateFist;
    private MaterialButton btnSimulateIndexUp;
    private MaterialButton btnSimulateVictorySign;
    private MaterialButton btnSimulateLoveCare;
    private MaterialButton btnSimulateTwoFingers;
    private MaterialButton btnSimulateOkSign;
    private MaterialButton btnSimulatePinch;
    private MaterialButton btnSimulateOneFingerUp;
    private MaterialButton btnSimulateWave;
    private MaterialButton btnSimulateCrossedFingers;
    private MaterialButton btnSimulateCallMe;

    // Gesture stability, confirmation and cooldown state
    private static final int REQUIRED_CONSECUTIVE_FRAMES = 3;
    private static final long GESTURE_COOLDOWN_MS = 1500;
    private String currentTrackingGestureId = null;
    private long trackingGestureStartTime = 0;
    private int consecutiveStableFrames = 0;
    private boolean isGestureConfirmed = false;
    private long lastActionExecutionTime = 0;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        networkExecutor = Executors.newSingleThreadExecutor();
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Security Check: Verify user permissions
        String role = sessionManager.getUserRole();
        if (!RoleManager.checkAccess(this, role, RoleManager.ROLE_MUTE_USER)) {
            finish();
            return;
        }
        SecurityGuard.verifyRole(this, RoleManager.ROLE_MUTE_USER, null);

        setContentView(R.layout.activity_mute_user_dashboard);

        // Bind Base UI Layouts
        txtUserRole = findViewById(R.id.txtUserRole);
        layoutStatus = findViewById(R.id.layoutStatus);
        viewStatusDot = findViewById(R.id.viewStatusDot);
        txtStatusLabel = findViewById(R.id.txtStatusLabel);
        btnBellNotificationSettings = findViewById(R.id.btnBellNotificationSettings);
        if (btnBellNotificationSettings != null) {
            btnBellNotificationSettings.setOnClickListener(v -> showNotificationSettingsDialog());
        }
        bottomNav = findViewById(R.id.bottomNavMuteUser);

        layoutHome = findViewById(R.id.layoutHome);
        layoutProfile = findViewById(R.id.layoutProfile);
        layoutCaregiver = findViewById(R.id.layoutCaregiver);

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
            txtUserRole.setText(getString(R.string.role_mute_user));
        }

        fetchCurrentUserProfileName();

        // Bind and setup widgets
        setupNavigation();
        setupHomeWidgets();
        setupGestureWidgets();
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

        // Request camera permissions and start pipeline for Hand Gesture Recognition
        if (allPermissionsGranted()) {
            startCameraPipeline();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE
            );
        }
    }

    /**
     * Set default preferences and load user details.
     */
    private void loadSettings() {
        SharedPreferences settingsPref = getSharedPreferences("MuteUserSettings", MODE_PRIVATE);
        coquiUrl = settingsPref.getString("coquiUrl", "http://10.0.2.2:5000");
        caregiverEmail = settingsPref.getString("caregiverEmail", "");
        
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
     * Strictly verifies availability and prevents silent fallback to English.
     */
    private int setTtsLanguageForUser(TextToSpeech engine) {
        if (engine == null) return TextToSpeech.LANG_NOT_SUPPORTED;
        String langCode = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String normLang = LanguageManager.normalizeLanguageCode(langCode);
        Locale targetLocale = new Locale(normLang, "IN");

        int res = engine.setLanguage(targetLocale);
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            Locale baseLocale = new Locale(normLang);
            res = engine.setLanguage(baseLocale);
        }
        return res;
    }

    /**
     * Initializes Google Text-to-Speech engine.
     */
    private void initGoogleTextToSpeech() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true;
                int result = setTtsLanguageForUser(tts);
                if (result == TextToSpeech.LANG_MISSING_DATA) {
                    Log.w(TAG, "Google TTS missing voice data for active language.");
                } else if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Google TTS language not supported.");
                } else {
                    Log.i(TAG, "Google TTS initialized successfully with active language.");
                }
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
                isTtsInitialized = true;
                int result = setTtsLanguageForUser(tts);
                if (result == TextToSpeech.LANG_MISSING_DATA) {
                    Log.w(TAG, "Default TTS missing voice data for active language.");
                } else if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Default TTS language not supported.");
                } else {
                    Log.i(TAG, "Default TTS engine initialized successfully.");
                }
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
            } else if (itemId == R.id.nav_profile) {
                layoutProfile.setVisibility(View.VISIBLE);
                refreshProfileDetails();
            } else if (itemId == R.id.nav_caregiver) {
                layoutCaregiver.setVisibility(View.VISIBLE);
                refreshConnectionState();
                CaregiverSoundManager.stopNotificationSound(MuteUserDashboardActivity.this);
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
        btnHomeSpeak = findViewById(R.id.btnHomeSpeak);
        cardEmergency = findViewById(R.id.cardEmergency);

        cardQuickWater = findViewById(R.id.cardQuickWater);
        cardQuickFood = findViewById(R.id.cardQuickFood);
        cardQuickHelp = findViewById(R.id.cardQuickHelp);
        cardQuickRest = findViewById(R.id.cardQuickRest);
        cardQuickOutside = findViewById(R.id.cardQuickOutside);
        cardQuickToilet = findViewById(R.id.cardQuickToilet);
        cardQuickQuiet = findViewById(R.id.cardQuickQuiet);

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

        btnHomeSpeak.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastSpeakClickTime < 800) {
                return; // Prevent rapid double-tap
            }
            lastSpeakClickTime = currentTime;

            String text = editHomeQuickText.getText().toString().trim();
            if (!text.isEmpty()) {
                speakText(text);
                saveSpokenHistory(text);
                updateHomeRecentPhrasesList();
            } else {
                Toast.makeText(this, "Please enter some text.", Toast.LENGTH_SHORT).show();
            }
        });

        cardEmergency.setOnClickListener(v -> triggerEmergencySOS());

        cardQuickWater.setOnClickListener(v -> handleQuickPhraseClick(getString(R.string.phrase_water)));
        cardQuickFood.setOnClickListener(v -> handleQuickPhraseClick(getString(R.string.phrase_food)));
        cardQuickHelp.setOnClickListener(v -> handleQuickPhraseClick(getString(R.string.phrase_help)));
        cardQuickRest.setOnClickListener(v -> handleQuickPhraseClick(getString(R.string.phrase_rest)));
        cardQuickOutside.setOnClickListener(v -> handleQuickPhraseClick(getString(R.string.phrase_outside)));
        cardQuickToilet.setOnClickListener(v -> handleQuickPhraseClick(getString(R.string.phrase_toilet)));
        cardQuickQuiet.setOnClickListener(v -> handleQuickPhraseClick(getString(R.string.phrase_quiet)));

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

        // Load initially used phrases
        updateHomeRecentPhrasesList();
    }

    /**
     * Binds and configures widgets for the Hand Gesture Recognition section.
     */
    private void setupGestureWidgets() {
        switchGestureDetection = findViewById(R.id.switchGestureDetection);
        gridOutputs = findViewById(R.id.gridOutputs);
        cardDeveloperSandbox = findViewById(R.id.cardDeveloperSandbox);
        txtGestureName = findViewById(R.id.txtGestureName);
        txtGestureConfidence = findViewById(R.id.txtGestureConfidence);
        txtGestureAction = findViewById(R.id.txtGestureAction);
        txtDetectionStatus = findViewById(R.id.txtDetectionStatus);
        txtKannadaText = findViewById(R.id.txtKannadaText);
        lblTranslatedText = findViewById(R.id.lblTranslatedText);
        if (lblTranslatedText != null) {
            String uLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
            String langName = LanguageManager.LANG_HINDI.equals(uLang) ? "Hindi" :
                              LanguageManager.LANG_MALAYALAM.equals(uLang) ? "Malayalam" : "Kannada";
            lblTranslatedText.setText("Translated " + langName + ": ");
        }
        txtMessageSentStatus = findViewById(R.id.txtMessageSentStatus);
        txtDetectionTime = findViewById(R.id.txtDetectionTime);

        if (switchGestureDetection != null) {
            updateGestureModuleVisibility(switchGestureDetection.isChecked());
            switchGestureDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isTrackingEnabled = isChecked;
                updateGestureModuleVisibility(isChecked);
                if (txtDetectionStatus != null) {
                    txtDetectionStatus.setText(isChecked ? "ACTIVE" : "INACTIVE");
                }
                if (!isChecked) {
                    resetTrackingStatus();
                }
            });
        }

        // Setup Developer simulation buttons (Canonical & Simple Gestures)
        btnSimulateThumbsUp = findViewById(R.id.btnSimulateThumbsUp);
        btnSimulateThumbsDown = findViewById(R.id.btnSimulateThumbsDown);
        btnSimulateOpenPalm = findViewById(R.id.btnSimulateOpenPalm);
        btnSimulateFist = findViewById(R.id.btnSimulateFist);
        btnSimulateIndexUp = findViewById(R.id.btnSimulateIndexUp);
        btnSimulateVictorySign = findViewById(R.id.btnSimulateVictorySign);
        btnSimulateLoveCare = findViewById(R.id.btnSimulateLoveCare);
        btnSimulateTwoFingers = findViewById(R.id.btnSimulateTwoFingers);
        btnSimulateOkSign = findViewById(R.id.btnSimulateOkSign);
        btnSimulatePinch = findViewById(R.id.btnSimulatePinch);
        btnSimulateOneFingerUp = findViewById(R.id.btnSimulateOneFingerUp);
        btnSimulateWave = findViewById(R.id.btnSimulateWave);
        btnSimulateCrossedFingers = findViewById(R.id.btnSimulateCrossedFingers);
        btnSimulateCallMe = findViewById(R.id.btnSimulateCallMe);

        if (btnSimulateThumbsUp != null) {
            btnSimulateThumbsUp.setOnClickListener(v -> dispatchSimulatedGesture(GestureActionMapper.ID_THUMB_UP, 0.95f));
        }
        if (btnSimulateThumbsDown != null) {
            btnSimulateThumbsDown.setOnClickListener(v -> dispatchSimulatedGesture(GestureActionMapper.ID_THUMB_DOWN, 0.95f));
        }
        if (btnSimulateOpenPalm != null) {
            btnSimulateOpenPalm.setOnClickListener(v -> dispatchSimulatedGesture(GestureActionMapper.ID_OPEN_PALM, 0.95f));
        }
        if (btnSimulateFist != null) {
            btnSimulateFist.setOnClickListener(v -> dispatchSimulatedGesture(GestureActionMapper.ID_CLOSED_FIST, 0.98f));
        }
        if (btnSimulateIndexUp != null) {
            btnSimulateIndexUp.setOnClickListener(v -> dispatchSimulatedGesture(GestureActionMapper.ID_POINTING_UP, 0.92f));
        }
        if (btnSimulateVictorySign != null) {
            btnSimulateVictorySign.setOnClickListener(v -> dispatchSimulatedGesture(GestureActionMapper.ID_VICTORY, 0.95f));
        }
        if (btnSimulateLoveCare != null) {
            btnSimulateLoveCare.setOnClickListener(v -> dispatchSimulatedGesture(GestureActionMapper.ID_ILOVEYOU, 0.95f));
        }
        if (btnSimulateTwoFingers != null) {
            btnSimulateTwoFingers.setOnClickListener(v -> dispatchSimulatedGesture(GestureActionMapper.ID_TWO_FINGERS, 0.95f));
        }
        if (btnSimulateOkSign != null) {
            btnSimulateOkSign.setOnClickListener(v -> dispatchSimulatedGesture(GestureActionMapper.ID_OK_SIGN, 0.95f));
        }
        if (btnSimulatePinch != null) {
            btnSimulatePinch.setOnClickListener(v -> dispatchSimulatedGesture(GestureActionMapper.ID_PINCH, 0.95f));
        }
        if (btnSimulateOneFingerUp != null) {
            btnSimulateOneFingerUp.setOnClickListener(v -> dispatchSimulatedGesture(GestureActionMapper.ID_ONE_FINGER_UP, 0.95f));
        }
        if (btnSimulateWave != null) {
            btnSimulateWave.setOnClickListener(v -> dispatchSimulatedGesture(GestureActionMapper.ID_OPEN_HAND_WAVE, 0.95f));
        }
        if (btnSimulateCrossedFingers != null) {
            btnSimulateCrossedFingers.setOnClickListener(v -> dispatchSimulatedGesture(GestureActionMapper.ID_CROSSED_FINGERS, 0.95f));
        }
        if (btnSimulateCallMe != null) {
            btnSimulateCallMe.setOnClickListener(v -> dispatchSimulatedGesture(GestureActionMapper.ID_CALL_ME, 0.95f));
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCameraPipeline();
            } else {
                Toast.makeText(this, "Camera permission required for gesture control.", Toast.LENGTH_LONG).show();
                enableOfflineSimulatorMode();
            }
        }
    }

    /**
     * Starts the CameraX capture pipeline and initializes MediaPipe Task Vision.
     */
    private void startCameraPipeline() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Build ImageAnalysis UseCase for frame-by-frame processing (background analysis without visible preview)
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();

                // Initialize MediaPipe Gesture Recognizer in background thread
                initializeGestureRecognizer();

                imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        if (isTrackingEnabled && isMediaPipeInitialized) {
                            processFrame(imageProxy);
                        } else {
                            imageProxy.close();
                        }
                    }
                });

                // Select lens direction
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraLensFacing)
                        .build();

                // Configure mirror orientation for landmark overlay if present
                if (gestureOverlayView != null) {
                    if (cameraLensFacing == CameraSelector.LENS_FACING_FRONT) {
                        gestureOverlayView.setScaleX(-1f);
                    } else {
                        gestureOverlayView.setScaleX(1f);
                    }
                }

                // Unbind previous usecases and bind ImageAnalysis to lifecycle
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis);

            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize CameraX provider.", e);
                runOnUiThread(this::enableOfflineSimulatorMode);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Instantiates the MediaPipe Gesture Recognizer.
     * Uses a fallback if the task binary model asset is missing.
     */
    private void initializeGestureRecognizer() {
        if (isMediaPipeInitialized) return;

        try {
            // Task file name in the app assets
            String taskModelPath = "gesture_recognizer.task";

            // Verify if asset actually exists
            try (InputStream is = getAssets().open(taskModelPath)) {
                // If we can open it, proceed
            } catch (Exception assetError) {
                Log.w(TAG, "MediaPipe task file 'gesture_recognizer.task' not found in assets. Falling back to sandbox simulator.");
                runOnUiThread(this::enableOfflineSimulatorMode);
                return;
            }

            BaseOptions baseOptions = BaseOptions.builder()
                    .setModelAssetPath(taskModelPath)
                    .build();

            GestureRecognizer.GestureRecognizerOptions options = GestureRecognizer.GestureRecognizerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setResultListener(this::handleGestureResult)
                    .setMinHandDetectionConfidence(0.5f)
                    .setMinHandPresenceConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .build();

            gestureRecognizer = GestureRecognizer.createFromOptions(this, options);
            isMediaPipeInitialized = true;
            Log.i(TAG, "MediaPipe Gesture Recognizer successfully initialized!");

        } catch (Exception e) {
            Log.e(TAG, "Failed to instantiate MediaPipe Tasks Vision engine. Activating simulator fallback.", e);
            runOnUiThread(this::enableOfflineSimulatorMode);
        }
    }

    /**
     * Activates the virtual sandbox simulator panel.
     */
    private void enableOfflineSimulatorMode() {
        if (bannerOfflineSimulator != null) {
            bannerOfflineSimulator.setVisibility(View.VISIBLE);
        }
        if (txtGestureName != null) {
            txtGestureName.setText("WAITING FOR GESTURE...");
        }
    }

    /**
     * Processes individual CameraX frames.
     * Integrates OpenCV-inspired preprocessing filters (skin segmentation contrast correction).
     */
    private void processFrame(ImageProxy imageProxy) {
        try {
            int width = imageProxy.getWidth();
            int height = imageProxy.getHeight();

            // Extract pixels into a Bitmap
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());

            // Enhance contours and light values dynamically to improve skin gesture tracking
            bitmap = applyContrastEnhancement(bitmap);

            // Handle frame orientation
            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
            if (rotationDegrees != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationDegrees);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
            }

            // Convert to MediaPipe Image structure
            com.google.mediapipe.framework.image.MPImage mpImage = new com.google.mediapipe.framework.image.BitmapImageBuilder(bitmap).build();

            // Analyze frame
            long timestampMs = System.currentTimeMillis();
            if (gestureRecognizer != null) {
                gestureRecognizer.recognizeAsync(mpImage, timestampMs);
            }

        } catch (Exception e) {
            Log.e(TAG, "Frame analyzer encountered processing error.", e);
        } finally {
            imageProxy.close();
        }
    }

    /**
     * OpenCV-inspired filter. Modifies brightness and contrast of the RGB bitmap
     * to highlight hand skin pixels and ease segmentation.
     */
    private Bitmap applyContrastEnhancement(Bitmap src) {
        Bitmap dest = src.copy(src.getConfig(), true);
        int width = dest.getWidth();
        int height = dest.getHeight();
        
        int[] pixels = new int[width * height];
        dest.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int r = (p >> 16) & 0xff;
            int g = (p >> 8) & 0xff;
            int b = p & 0xff;

            r = Math.min(255, (int)(r * 1.15));
            g = Math.min(255, (int)(g * 1.05));
            b = Math.min(255, (int)(b * 0.95));

            pixels[i] = (p & 0xff000000) | (r << 16) | (g << 8) | b;
        }

        dest.setPixels(pixels, 0, width, 0, 0, width, height);
        return dest;
    }

    /**
     * Callback invoked by MediaPipe when hand landmarks and gesture categories are ready.
     */
    private void handleGestureResult(GestureRecognizerResult result, com.google.mediapipe.framework.image.MPImage mpImage) {
        runOnUiThread(() -> {
            if (!isTrackingEnabled) return;

            // 1. Hand Skeleton Overlay Drawing
            if (result.landmarks() != null && !result.landmarks().isEmpty()) {
                List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> handLandmarks = result.landmarks().get(0);
                List<PointF> drawingList = new ArrayList<>();
                for (com.google.mediapipe.tasks.components.containers.NormalizedLandmark landmark : handLandmarks) {
                    drawingList.add(new PointF(landmark.x(), landmark.y()));
                }
                if (gestureOverlayView != null) {
                    gestureOverlayView.updateLandmarks(drawingList);
                }
            } else {
                if (gestureOverlayView != null) {
                    gestureOverlayView.updateLandmarks(null);
                }
            }

            // 2. Extract MediaPipe Neural Model Category Prediction & Confidence
            String detectedCategory = null;
            float confidence = 0.0f;

            if (result.gestures() != null && !result.gestures().isEmpty()) {
                List<com.google.mediapipe.tasks.components.containers.Category> handGestures = result.gestures().get(0);
                if (handGestures != null && !handGestures.isEmpty()) {
                    com.google.mediapipe.tasks.components.containers.Category topCategory = handGestures.get(0);
                    detectedCategory = topCategory.categoryName();
                    confidence = topCategory.score();
                }
            }

            GestureAction gestureAction = null;

            // 3. Resolve central GestureAction mapping for standard MediaPipe categories first
            if (detectedCategory != null && !"None".equalsIgnoreCase(detectedCategory.trim()) && !detectedCategory.trim().isEmpty()) {
                gestureAction = GestureActionMapper.getGestureByMediaPipeName(detectedCategory);
            }

            // 4. If standard MediaPipe category is unclassified, low confidence, or missing, check custom hand landmarks
            if (gestureAction == null || confidence < gestureThreshold) {
                if (result.landmarks() != null && !result.landmarks().isEmpty()) {
                    List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> handLandmarks = result.landmarks().get(0);
                    LandmarkGestureResult lmResult = detectLandmarkGesture(handLandmarks);
                    if (lmResult != null) {
                        gestureAction = lmResult.action;
                        confidence = lmResult.confidence;
                    }
                }
            }

            if (gestureAction == null) {
                resetStabilityTracking();
                return;
            }

            String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;

            // 5. Confidence Threshold Filtering
            if (confidence < gestureThreshold) {
                if (txtGestureName != null) {
                    txtGestureName.setText(gestureAction.getEmoji() + " " + gestureAction.getDisplayName(userLang) + " (Low Confidence)");
                }
                if (txtGestureConfidence != null) {
                    txtGestureConfidence.setText(String.format(Locale.US, "%.1f%%", confidence * 100));
                }
                return;
            }

            // 6. Temporal Stability & Frame Confirmation
            long currentTime = System.currentTimeMillis();

            if (gestureAction.getGestureId().equals(currentTrackingGestureId)) {
                consecutiveStableFrames++;
                long durationMs = currentTime - trackingGestureStartTime;

                if (!isGestureConfirmed) {
                    float holdSec = Math.max(0.1f, (durationMs / 100) / 10.0f);
                    if (txtGestureName != null) {
                        txtGestureName.setText(gestureAction.getEmoji() + " " + gestureAction.getDisplayName(userLang) + " (Holding " + String.format(Locale.US, "%.1fs", holdSec) + ")");
                    }
                    if (txtGestureConfidence != null) {
                        txtGestureConfidence.setText(String.format(Locale.US, "%.1f%%", confidence * 100));
                    }

                    if (consecutiveStableFrames >= REQUIRED_CONSECUTIVE_FRAMES || durationMs >= 500) {
                        // Check Action Cooldown Debounce
                        if (currentTime - lastActionExecutionTime >= GESTURE_COOLDOWN_MS) {
                            isGestureConfirmed = true;
                            lastActionExecutionTime = currentTime;
                            executeGestureAction(gestureAction, confidence, false);
                        } else {
                            if (txtGestureName != null) {
                                txtGestureName.setText(gestureAction.getEmoji() + " " + gestureAction.getDisplayName(userLang) + " (Cooldown)");
                            }
                            if (txtGestureConfidence != null) {
                                txtGestureConfidence.setText(String.format(Locale.US, "%.1f%%", confidence * 100));
                            }
                        }
                    }
                }
            } else {
                // New stable candidate gesture detected
                currentTrackingGestureId = gestureAction.getGestureId();
                trackingGestureStartTime = currentTime;
                consecutiveStableFrames = 1;
                isGestureConfirmed = false;

                if (txtGestureName != null) {
                    txtGestureName.setText(gestureAction.getEmoji() + " " + gestureAction.getDisplayName(userLang) + " (Detecting...)");
                }
                if (txtGestureConfidence != null) {
                    txtGestureConfidence.setText(String.format(Locale.US, "%.1f%%", confidence * 100));
                }
                if (txtGestureAction != null) txtGestureAction.setText("None");
                if (txtKannadaText != null) txtKannadaText.setText("-");
                if (txtMessageSentStatus != null) txtMessageSentStatus.setText("Not Sent");
            }
        });
    }

    private void resetStabilityTracking() {
        currentTrackingGestureId = null;
        trackingGestureStartTime = 0;
        consecutiveStableFrames = 0;
        isGestureConfirmed = false;
    }

    private void updateGestureModuleVisibility(boolean isVisible) {
        int visibility = isVisible ? View.VISIBLE : View.GONE;
        if (gridOutputs != null) {
            gridOutputs.setVisibility(visibility);
        }
        if (cardDeveloperSandbox != null) {
            cardDeveloperSandbox.setVisibility(visibility);
        }
    }

    private void resetTrackingStatus() {
        if (txtGestureName != null) txtGestureName.setText("TRACKING DISABLED");
        if (txtGestureConfidence != null) txtGestureConfidence.setText("0.0%");
        if (txtGestureAction != null) txtGestureAction.setText("None");
        if (gestureOverlayView != null) gestureOverlayView.updateLandmarks(null);
    }

    private static class LandmarkGestureResult {
        final GestureAction action;
        final float confidence;
        LandmarkGestureResult(GestureAction action, float confidence) {
            this.action = action;
            this.confidence = confidence;
        }
    }

    private double getLandmarkDistance(com.google.mediapipe.tasks.components.containers.NormalizedLandmark p1,
                                       com.google.mediapipe.tasks.components.containers.NormalizedLandmark p2) {
        float dx = p1.x() - p2.x();
        float dy = p1.y() - p2.y();
        float dz = p1.z() - p2.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private boolean isFingerExtended(List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> lm,
                                     int tipIdx, int pipIdx, int mcpIdx) {
        double dTip = getLandmarkDistance(lm.get(0), lm.get(tipIdx));
        double dPip = getLandmarkDistance(lm.get(0), lm.get(pipIdx));
        double dMcp = getLandmarkDistance(lm.get(0), lm.get(mcpIdx));
        return dTip > dPip && dTip > (dMcp * 1.12);
    }

    private boolean isThumbExtended(List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> lm) {
        double dTipWrist = getLandmarkDistance(lm.get(0), lm.get(4));
        double dIpWrist = getLandmarkDistance(lm.get(0), lm.get(3));
        double dTipPinky = getLandmarkDistance(lm.get(17), lm.get(4));
        double dMcpPinky = getLandmarkDistance(lm.get(17), lm.get(2));
        return dTipWrist > dIpWrist && dTipPinky > (dMcpPinky * 0.85);
    }

    private LandmarkGestureResult detectLandmarkGesture(List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> lm) {
        if (lm == null || lm.size() < 21) return null;

        boolean thumbExt = isThumbExtended(lm);
        boolean indexExt = isFingerExtended(lm, 8, 6, 5);
        boolean middleExt = isFingerExtended(lm, 12, 10, 9);
        boolean ringExt = isFingerExtended(lm, 16, 14, 13);
        boolean pinkyExt = isFingerExtended(lm, 20, 18, 17);

        double distThumbIndex = getLandmarkDistance(lm.get(4), lm.get(8));
        double distIndexMiddle = getLandmarkDistance(lm.get(8), lm.get(12));

        // 1. Call Me Sign (🤙): Thumb & Pinky extended, Index/Middle/Ring folded
        if (thumbExt && pinkyExt && !indexExt && !middleExt && !ringExt) {
            GestureAction action = GestureActionMapper.getGestureById(GestureActionMapper.ID_CALL_ME);
            if (action != null) return new LandmarkGestureResult(action, 0.92f);
        }

        // 2. OK Sign (👌): Thumb tip & Index tip pinching (dist < 0.08), Middle/Ring/Pinky extended
        if (distThumbIndex < 0.08 && middleExt && ringExt && pinkyExt) {
            GestureAction action = GestureActionMapper.getGestureById(GestureActionMapper.ID_OK_SIGN);
            if (action != null) return new LandmarkGestureResult(action, 0.90f);
        }

        // 3. Pinch (🤏): Thumb tip & Index tip close (dist < 0.06), Middle/Ring/Pinky folded
        if (distThumbIndex < 0.06 && !middleExt && !ringExt && !pinkyExt) {
            GestureAction action = GestureActionMapper.getGestureById(GestureActionMapper.ID_PINCH);
            if (action != null) return new LandmarkGestureResult(action, 0.88f);
        }

        // 4. Crossed Fingers (🤞): Index & Middle extended with tips crossed/touching (dist < 0.05), Ring & Pinky folded
        if (indexExt && middleExt && distIndexMiddle < 0.05 && !ringExt && !pinkyExt) {
            GestureAction action = GestureActionMapper.getGestureById(GestureActionMapper.ID_CROSSED_FINGERS);
            if (action != null) return new LandmarkGestureResult(action, 0.89f);
        }

        // 5. Two Fingers (✌️): Index & Middle extended, Ring & Pinky folded
        if (indexExt && middleExt && !ringExt && !pinkyExt) {
            GestureAction action = GestureActionMapper.getGestureById(GestureActionMapper.ID_TWO_FINGERS);
            if (action != null) return new LandmarkGestureResult(action, 0.88f);
        }

        // 7. One Finger Up (☝️): Index extended, Middle/Ring/Pinky folded
        if (indexExt && !middleExt && !ringExt && !pinkyExt) {
            GestureAction action = GestureActionMapper.getGestureById(GestureActionMapper.ID_ONE_FINGER_UP);
            if (action != null) return new LandmarkGestureResult(action, 0.88f);
        }

        // 8. Open Hand / Wave (👋): All 5 fingers extended
        if (thumbExt && indexExt && middleExt && ringExt && pinkyExt) {
            GestureAction action = GestureActionMapper.getGestureById(GestureActionMapper.ID_OPEN_HAND_WAVE);
            if (action != null) return new LandmarkGestureResult(action, 0.90f);
        }

        return null;
    }

    /**
     * Unified execution path for both camera detection and sandbox simulator.
     */
    private void executeGestureAction(@NonNull GestureAction gestureAction, float confidence, boolean isSimulated) {
        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String localizedMessage = gestureAction.getLocalizedMessage(userLang);
        String actionDescription = gestureAction.getActionDescription();

        // Update Time & UI Outputs
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String timeString = sdf.format(new java.util.Date());

        if (txtGestureName != null) {
            txtGestureName.setText(gestureAction.getEmoji() + " " + gestureAction.getDisplayName(userLang));
        }
        if (txtGestureConfidence != null) {
            txtGestureConfidence.setText(String.format(Locale.US, "%.1f%%", confidence * 100));
        }
        if (lblTranslatedText != null) {
            String langName = LanguageManager.LANG_HINDI.equals(userLang) ? "Hindi" :
                              LanguageManager.LANG_MALAYALAM.equals(userLang) ? "Malayalam" : "Kannada";
            lblTranslatedText.setText("Translated " + langName + ": ");
        }
        if (txtKannadaText != null) {
            txtKannadaText.setText(localizedMessage);
        }
        if (txtGestureAction != null) {
            txtGestureAction.setText(actionDescription);
        }
        if (txtDetectionTime != null) {
            txtDetectionTime.setText(timeString);
        }

        // Play TTS Speech in Selected Language
        speakText(localizedMessage);

        // Send Firebase Message to Connected Caregiver Immediately
        if (connectedCaregiverUid != null && !connectedCaregiverUid.isEmpty()) {
            sendAutoFirebaseGestureMessage(localizedMessage);
            if (txtMessageSentStatus != null) {
                txtMessageSentStatus.setText("Sent to Caregiver");
            }
        } else {
            if (txtMessageSentStatus != null) {
                txtMessageSentStatus.setText("Not Sent (No Caregiver)");
            }
        }

        // Save Recognition History to Firestore
        saveGestureHistory(gestureAction.getGestureId(), localizedMessage, confidence);

        // Trigger SOS alarm if Closed Fist / Emergency
        if (gestureAction.isEmergency()) {
            triggerEmergencySOS();
        }
    }

    /**
     * Dispatches simulated gestures from sandbox buttons through the central GestureActionMapper.
     */
    private void dispatchSimulatedGesture(String gestureIdentifier, float score) {
        GestureAction action = GestureActionMapper.getGestureById(gestureIdentifier);
        if (action == null) {
            action = GestureActionMapper.getGestureByMediaPipeName(gestureIdentifier);
        }
        if (action == null) return;

        // Draw a simulated skeleton overlay (Mock points) for visual feedback
        if (gestureOverlayView != null) {
            List<PointF> mockPoints = new ArrayList<>();
            mockPoints.add(new PointF(0.5f, 0.9f));
            for (int i = 0; i < 20; i++) {
                mockPoints.add(new PointF(0.3f + (0.02f * i), 0.7f - (0.01f * i)));
            }
            gestureOverlayView.updateLandmarks(mockPoints);
        }

        // Execute unified action
        executeGestureAction(action, score, true);
    }

    private void sendAutoFirebaseGestureMessage(String text) {
        if (connectedCaregiverUid == null || connectedCaregiverUid.isEmpty()) {
            return;
        }

        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String lang = LanguageManager.detectLanguageFromText(text, userLang);

        String chatId;
        if (currentUid.compareTo(connectedCaregiverUid) < 0) {
            chatId = currentUid + "_" + connectedCaregiverUid;
        } else {
            chatId = connectedCaregiverUid + "_" + currentUid;
        }
        
        String rEmail = caregiverEmail;
        if (rEmail == null || rEmail.isEmpty()) {
            android.content.SharedPreferences settingsPref = getSharedPreferences("MuteUserSettings", MODE_PRIVATE);
            rEmail = settingsPref.getString("caregiverEmail", "");
        }

        Map<String, Object> msg = new HashMap<>();
        msg.put("chatId", chatId);
        msg.put("senderId", currentUid);
        msg.put("senderUid", currentUid);
        msg.put("receiverId", connectedCaregiverUid);
        msg.put("recipientEmail", rEmail != null ? rEmail.toLowerCase().trim() : "");
        msg.put("senderRole", RoleManager.ROLE_MUTE_USER);
        msg.put("receiverRole", "Admin/Caregiver");
        msg.put("message", text);
        msg.put("language", lang);

        String mType = "gesture";
        if (LanguageManager.isDefaultSosMessage(text) || (emergencyMessage != null && (emergencyMessage.equals(text) || emergencyMessage.contains(text)))) {
            mType = "emergency";
        }
        msg.put("messageType", mType);
        msg.put("type", mType);
        msg.put("status", "sent");
        msg.put("delivered", false);
        msg.put("seen", false);
        msg.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        
        db.collection("caregiver_messages")
                .add(msg)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Gesture message sent successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send gesture message", e);
                    // Save locally as offline fallback
                    LocalConnectionSimulator.saveLocalMessage(MuteUserDashboardActivity.this, chatId, msg);
                    Toast.makeText(this, "Saved locally (Offline)", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveGestureHistory(String gesture, String message, float score) {
        Map<String, Object> historyLog = new HashMap<>();
        historyLog.put("userId", currentUid);
        historyLog.put("gesture", gesture);
        historyLog.put("message", message);
        historyLog.put("confidence", score);
        historyLog.put("timestamp", com.google.firebase.Timestamp.now());
        if (connectedCaregiverUid != null) {
            historyLog.put("recipientId", connectedCaregiverUid);
        }
        
        db.collection("gesture_history")
                .add(historyLog)
                .addOnSuccessListener(ref -> Log.i(TAG, "Gesture history saved successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save gesture history", e));
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
        if (txtHomeTranslitStatus != null) {
            txtHomeTranslitStatus.setText("English Translit (" + LanguageManager.getLanguageDisplayName(normLang) + ")");
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
                                    txtHomeTranslitStatus.setText("Translit active (" + LanguageManager.getLanguageDisplayName(langCode) + ")");
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
                    txtHomeTranslitStatus.setText("Converted (Offline Fallback - " + LanguageManager.getLanguageDisplayName(langCode) + ")");
                });
            } finally {
                try {
                    if (reader != null) reader.close();
                } catch (Exception ex) {}
                if (conn != null) conn.disconnect();
            }
        });
    }

    private void handleQuickPhraseClick(String msg) {
        if (msg == null || msg.trim().isEmpty()) return;

        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String resolvedMsg = LanguageManager.getQuickPhraseText(msg, userLang);

        // 1. Tactile haptic vibration tick
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            }
        } catch (Exception ignored) {}

        // 2. Speak out loud in user language at high volume instantly
        speakText(resolvedMsg);

        // 3. Send message to connected caregiver immediately via Firestore
        sendDirectMessageToCaregiver(resolvedMsg);

        // 4. Update recent phrases chips
        saveSpokenHistory(resolvedMsg);
        updateHomeRecentPhrasesList();
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
        chatMsgMap.put("senderRole", RoleManager.ROLE_MUTE_USER);
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
                    LocalConnectionSimulator.saveLocalMessage(MuteUserDashboardActivity.this, chatId, chatMsgMap);
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
                handleQuickPhraseClick(phrase);
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
        String locRole = getString(R.string.role_mute_user);
        if (RoleManager.ROLE_PHYSICALLY_DISABLED.equals(roleStr)) {
            locRole = getString(R.string.role_physically_disabled);
        } else if (RoleManager.ROLE_SPEECH_IMPAIRED.equals(roleStr)) {
            locRole = getString(R.string.role_speech_impaired);
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

        getSharedPreferences("MuteUserSettings", MODE_PRIVATE).edit()
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
     * Cleans input phrase to remove trailing parenthesized/bracketed translation annotations
     * while safely preserving user-typed content.
     */
    private String cleanTextForSpeech(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return "";

        int pOpen = trimmed.lastIndexOf('(');
        int pClose = trimmed.lastIndexOf(')');
        if (pOpen > 0 && pClose > pOpen) {
            String before = trimmed.substring(0, pOpen).trim();
            if (!before.isEmpty()) {
                trimmed = before;
            }
        }
        int bOpen = trimmed.lastIndexOf('[');
        int bClose = trimmed.lastIndexOf(']');
        if (bOpen > 0 && bClose > bOpen) {
            String before = trimmed.substring(0, bOpen).trim();
            if (!before.isEmpty()) {
                trimmed = before;
            }
        }
        return trimmed;
    }

    /**
     * Synthesizes and plays out text speech.
     */
    private void speakText(String text) {
        speakText(text, false);
    }

    private void speakText(String text, boolean isFallback) {
        String cleanedText = cleanTextForSpeech(text);
        if (cleanedText.isEmpty()) return;

        // Speak instantly on device via local Google TTS
        speakGoogleTts(cleanedText, isFallback);
    }

    /**
     * Synthesizes text using on-device TTS in the user's selected language (kn-IN, hi-IN, ml-IN).
     * Strictly verifies availability, handles missing data, and never silently falls back to English.
     */
    private void speakGoogleTts(String message) {
        speakGoogleTts(message, false);
    }

    private void speakGoogleTts(String message, boolean isFallback) {
        if (message == null || message.trim().isEmpty()) return;

        // 1. Detect target language based on active user language and text script
        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String detectedLang = LanguageManager.detectLanguageFromText(message, userLang);
        String targetLangCode = LanguageManager.normalizeLanguageCode(detectedLang);
        String langDisplayName = LanguageManager.getLanguageDisplayName(targetLangCode);

        // 2. Map to correct Android Locale (kn-IN, hi-IN, ml-IN)
        Locale targetLocale = new Locale(targetLangCode, "IN");

        if (!isTtsInitialized || tts == null) {
            Toast.makeText(this, "Text-to-Speech engine is initializing. Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Check whether TTS engine supports that locale
        int availability = tts.isLanguageAvailable(targetLocale);
        Locale chosenLocale = null;

        if (availability == TextToSpeech.LANG_COUNTRY_AVAILABLE 
                || availability == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE 
                || availability >= TextToSpeech.LANG_AVAILABLE) {
            chosenLocale = targetLocale;
        } else {
            Locale baseLocale = new Locale(targetLangCode);
            int baseAvail = tts.isLanguageAvailable(baseLocale);
            if (baseAvail == TextToSpeech.LANG_COUNTRY_AVAILABLE 
                    || baseAvail == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE 
                    || baseAvail >= TextToSpeech.LANG_AVAILABLE) {
                chosenLocale = baseLocale;
            } else if (availability == TextToSpeech.LANG_MISSING_DATA || baseAvail == TextToSpeech.LANG_MISSING_DATA) {
                Toast.makeText(this, "TTS voice data for " + langDisplayName + " is missing on device.", Toast.LENGTH_LONG).show();
                return;
            } else {
                Toast.makeText(this, "TTS does not support " + langDisplayName + " on this engine.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // 4. Select correct locale and set language
        int setRes = tts.setLanguage(chosenLocale);
        if (setRes == TextToSpeech.LANG_MISSING_DATA) {
            Toast.makeText(this, "TTS voice data for " + langDisplayName + " is missing.", Toast.LENGTH_LONG).show();
            return;
        } else if (setRes == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "TTS does not support " + langDisplayName + ".", Toast.LENGTH_LONG).show();
            return;
        }

        // 5. Prevent overlapping audio & speak strictly with user's configured volume
        try {
            tts.stop();
        } catch (Exception ignored) {}
        tts.setPitch(1.0f);
        tts.setSpeechRate(1.0f);

        CaregiverSoundManager.speakWithVolume(tts, message, "speech_assist_" + System.currentTimeMillis(), this);
        saveSpokenHistory(message);
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
        String displayName = (currentName != null && !currentName.isEmpty()) ? currentName : (email != null ? email.split("@")[0] : "Mute User");

        Map<String, Object> sosLog = new HashMap<>();
        sosLog.put("uid", uid);
        sosLog.put("email", email);
        sosLog.put("userName", displayName);
        sosLog.put("role", RoleManager.ROLE_MUTE_USER);
        sosLog.put("message", emergencyMessage);
        sosLog.put("timestamp", com.google.firebase.Timestamp.now());
        sosLog.put("status", "Unresolved");

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
                    fcmAlert.put("role", RoleManager.ROLE_MUTE_USER);

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
                        
                        String otherName, otherEmail, otherRole, otherUid;

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

                        if (RoleManager.ROLE_ADMIN_CAREGIVER.equals(currentRole)) {
                            txtConnectedTitle.setText(getString(R.string.connected_patient_title));
                        } else {
                            txtConnectedTitle.setText(getString(R.string.connected_caregiver_title));
                        }

                        txtConnectedName.setText(otherName);
                        txtConnectedEmail.setText(otherEmail);
                        String locOtherRole = RoleManager.ROLE_ADMIN_CAREGIVER.equals(otherRole) ? getString(R.string.role_caregiver) : (RoleManager.ROLE_MUTE_USER.equals(otherRole) ? getString(R.string.role_mute_user) : getString(R.string.role_speech_impaired));
                        txtConnectedRole.setText("Role: " + locOtherRole);
                        
                        caregiverEmail = otherEmail;
                        connectedCaregiverUid = otherUid;

                        final String finalOtherUid = otherUid;
                        final String finalOtherName = otherName;
                        final String finalOtherRole = otherRole;
                        final String finalOtherEmail = otherEmail;
                        btnOpenFullChat.setOnClickListener(v -> {
                            Intent intent = new Intent(MuteUserDashboardActivity.this, ChatActivity.class);
                            intent.putExtra("receiverId", finalOtherUid);
                            intent.putExtra("receiverName", finalOtherName);
                            intent.putExtra("receiverRole", finalOtherRole);
                            intent.putExtra("receiverEmail", finalOtherEmail);
                            startActivity(intent);
                        });

                        btnDisconnect.setOnClickListener(v -> disconnectUser(docId));
                        cardConnectedUser.setVisibility(View.VISIBLE);
                        startCaregiverChatListener(otherUid);
                    } else {
                        cardConnectedUser.setVisibility(View.GONE);
                        connectedCaregiverUid = null;
                        caregiverEmail = "";
                        startCaregiverChatListener(null);
                        if (editCaregiverEmail != null) {
                            editCaregiverEmail.setText("");
                            editCaregiverEmail.setHint("Not Connected to any Caregiver");
                        }
                    }
                    updateSearchCardVisibility();
                });
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
                            String locRole = RoleManager.ROLE_ADMIN_CAREGIVER.equals(role) ? getString(R.string.role_caregiver) : (RoleManager.ROLE_MUTE_USER.equals(role) ? getString(R.string.role_mute_user) : getString(R.string.role_speech_impaired));
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
        Toast.makeText(this, "Disconnected successfully.", Toast.LENGTH_SHORT).show();
        refreshConnectionState();

        db.collection("caregiver_connections").document(docId)
                .delete()
                .addOnFailureListener(e -> Log.e(TAG, "Background disconnect failed", e));
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
        sessionManager.logoutUser();
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
        RoleManager.redirectToLogin(this);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tts != null) {
            try {
                tts.stop();
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (gestureRecognizer != null) {
            try {
                gestureRecognizer.close();
            } catch (Exception ignored) {}
            gestureRecognizer = null;
        }

        try {
            com.kannada.speechassistant.voiceassistant.AppVoiceAssistant.getInstance(this).stopListening();
        } catch (Exception ignored) {}
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
            }
        });
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
        speakGoogleTts(text);
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
            return;
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
                    CaregiverSoundManager.saveNotificationVolumePercent(MuteUserDashboardActivity.this, currentUid, progress);
                    txtActiveRingtoneDialogName.setText(CaregiverSoundManager.getActiveRingtoneName(MuteUserDashboardActivity.this, currentUid));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                CaregiverSoundManager.saveNotificationVolumePercent(MuteUserDashboardActivity.this, currentUid, seekBar.getProgress());
                txtActiveRingtoneDialogName.setText(CaregiverSoundManager.getActiveRingtoneName(MuteUserDashboardActivity.this, currentUid));
                CaregiverSoundManager.testPlaySound(MuteUserDashboardActivity.this, currentUid);
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
                    Toast.makeText(MuteUserDashboardActivity.this, "File Manager not found on device", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnTestPlayRingtone.setOnClickListener(v -> {
            CaregiverSoundManager.testPlaySound(MuteUserDashboardActivity.this, currentUid);
        });

        btnResetDefaultRingtone.setOnClickListener(v -> {
            CaregiverSoundManager.toggleSilentMode(MuteUserDashboardActivity.this, currentUid);
            boolean nowSilent = CaregiverSoundManager.isSilentMode(MuteUserDashboardActivity.this, currentUid);
            if (nowSilent) {
                btnResetDefaultRingtone.setText("🔔 Enable Sound");
                Toast.makeText(MuteUserDashboardActivity.this, "🔇 Silent Mode ON (Sound OFF, Vibration ON)", Toast.LENGTH_SHORT).show();
            } else {
                btnResetDefaultRingtone.setText("🔇 Clear (Silent)");
                Toast.makeText(MuteUserDashboardActivity.this, "🔔 Notification Sound Enabled", Toast.LENGTH_SHORT).show();
                CaregiverSoundManager.testPlaySound(MuteUserDashboardActivity.this, currentUid);
            }
            txtActiveRingtoneDialogName.setText(CaregiverSoundManager.getActiveRingtoneName(MuteUserDashboardActivity.this, currentUid));
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
                        CaregiverSoundManager.saveTtsVolumePercent(MuteUserDashboardActivity.this, currentUid, progress);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    CaregiverSoundManager.saveTtsVolumePercent(MuteUserDashboardActivity.this, currentUid, seekBar.getProgress());
                    CaregiverSoundManager.testPlayTts(MuteUserDashboardActivity.this, currentUid);
                }
            });
        }

        btnSaveNotificationSettings.setOnClickListener(v -> {
            CaregiverSoundManager.saveNotificationVolumePercent(MuteUserDashboardActivity.this, currentUid, sbNotificationVolume.getProgress());
            if (sbTtsVolume != null) {
                CaregiverSoundManager.saveTtsVolumePercent(MuteUserDashboardActivity.this, currentUid, sbTtsVolume.getProgress());
            }
            CaregiverSoundManager.stopNotificationSound(MuteUserDashboardActivity.this);
            Toast.makeText(MuteUserDashboardActivity.this, "Notification sound & volume saved successfully!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.setOnDismissListener(d -> CaregiverSoundManager.stopNotificationSound(MuteUserDashboardActivity.this));
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
        try {
            com.kannada.speechassistant.voiceassistant.WakeWordManager.getInstance(this).stopListening();
            com.kannada.speechassistant.voiceassistant.AppVoiceAssistant.getInstance(this).stopListening();
        } catch (Exception ignored) {}
    }
}
