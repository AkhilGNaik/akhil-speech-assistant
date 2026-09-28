package com.kannada.speechassistant;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.ImageButton;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.widget.Button;
import android.widget.LinearLayout;
import com.google.android.material.badge.BadgeDrawable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
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
import android.media.MediaRecorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Calendar;
import java.util.Date;

/**
 * Controller class for the Admin/Caregiver Dashboard.
 * Integrates real-time Firestore synchronization, dynamic list querying, filtering,
 * alert broadcast warnings (audio sirening, layout flashing, and Text-To-Speech notifications).
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboardActivity";
    private static final String CHANNEL_ID = "admin_emergency_channel_v2";
    private static final int NOTIFICATION_ID = 999;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase));
    }

    // Services
    private SessionManager sessionManager;
    private FirebaseFirestore db;
    private TextToSpeech tts;
    private boolean isTtsInitialized = false;

    // Listeners
    private ListenerRegistration usersListener;
    private ListenerRegistration emergenciesActiveListener;
    private ListenerRegistration emergenciesResolvedListener;
    private ListenerRegistration connectionsListener;
    private ListenerRegistration communicationListener;

    // Base UI Elements
    private TextView txtUserRole;
    private TextView txtUserEmail;
    private MaterialButton btnLogout;
    private TabLayout tabLayout;
    private androidx.viewpager2.widget.ViewPager2 viewPager;

    // Tab Views
    private View layoutUsersTab;
    private View layoutEmergenciesTab;
    private View layoutConnectionsTab;
    private View layoutCommunicationTab;

    // Users Tab Widgets
    private SearchView searchView;
    private ChipGroup chipGroupRoles;
    private TextView txtUsersCount;
    private RecyclerView rvUsers;
    private UserAdapter userAdapter;
    private String currentSearchQuery = "";
    private String currentRoleFilter = "All";

    // Emergencies Tab Widgets
    private MaterialCardView cardSystemStatus;
    private View viewAlarmIndicator;
    private TextView txtAlarmStatus;
    private RecyclerView rvActiveEmergencies;
    private RecyclerView rvResolvedEmergencies;
    private EmergencyAdapter activeEmergenciesAdapter;
    private EmergencyAdapter resolvedEmergenciesAdapter;
    private SearchView searchViewEmergencies;
    private String currentEmergenciesQuery = "";
    private TextView txtNoActiveEmergencies;
    private TextView txtNoResolvedEmergencies;

    // Connections Tab Widgets
    private LinearLayout layoutActiveConnectionsList;
    private TextView txtPendingSectionTitle;
    private android.widget.LinearLayout layoutReceivedRequestsList;
    private android.widget.FrameLayout loadingOverlayTab;
    private SearchView searchViewConnections;
    private String currentConnectionsQuery = "";
    private TextView txtNoConnectionsFound;
    private List<Map<String, Object>> latestActiveConnectionsList = new ArrayList<>();

    // Communication Tab Widgets
    private RecyclerView rvCommunicationMessages;
    private CaregiverMessageAdapter communicationAdapter;

    // Caregiver Chat Section
    private RecyclerView rvCaregiverChatMessages;
    private View layoutCaregiverChatEmptyState;
    private LinearLayout layoutCaregiverChatWindow;
    private TextView txtChatHeaderName;
    private TextView txtChatHeaderStatus;
    private EditText editCaregiverChatMessage;
    private View btnCaregiverChatSend;
    private View btnCaregiverChatEmoji;
    private View btnCaregiverChatMic;
    private android.speech.SpeechRecognizer caregiverSpeechRecognizer;
    private android.content.Intent caregiverSpeechIntent;
    private boolean isCaregiverRecording = false;
    private boolean isCaregiverVoiceInputMessage = false;
    private MediaRecorder caregiverMediaRecorder;
    private File caregiverTempAudioFile;
    private boolean isCaregiverAudioRecording = false;
    private long caregiverRecordingStartTime = 0;
    private final ExecutorService caregiverNetworkExecutor = Executors.newSingleThreadExecutor();
    private LinearLayout layoutEmojiTray;
    private CaregiverChatAdapter caregiverChatAdapter;
    private PatientListAdapter patientListAdapter;
    private String selectedPatientUid = null;
    private String selectedPatientName = null;
    private String selectedPatientRole = null;
    private String selectedPatientEmail = null;
    private String selectedPatientLanguage = null;
    private ListenerRegistration caregiverChatListener = null;
    private ListenerRegistration activeChatUserPresenceListener = null;
    private List<UserRecord> connectedPatientsList = new ArrayList<>();
    private final Set<String> connectedPatientUids = new HashSet<>();
    private final Set<String> connectedPatientEmails = new HashSet<>();
    private final Map<String, ListenerRegistration> chatInfoListeners = new HashMap<>();
    private final Map<String, ListenerRegistration> onlineStatusListeners = new HashMap<>();
    private final Set<String> playedMessageIds = new HashSet<>();

    // Current User profile info
    private String currentUid;
    private String currentEmail;
    private String currentName;
    private String currentRole;
    private QuerySnapshot latestUsersSnapshot;
    private QuerySnapshot latestActiveEmergenciesSnapshot;
    private QuerySnapshot latestResolvedEmergenciesSnapshot;

    // Alarm Warnings State
    private Ringtone localSiren;
    private Vibrator vibrator;
    private final Handler warningFlashHandler = new Handler(Looper.getMainLooper());
    private android.widget.ImageButton btnBellNotificationSettings;
    private TextView txtActiveRingtoneDialogName = null;
    private static final int REQUEST_CODE_PICK_RINGTONE = 701;
    private boolean isFlashing = false;
    private int flashColorToggle = 0;
    private final Set<String> processedAlertIds = new HashSet<>();
    private long dashboardStartTime;

    // New Caregiver Emergency Alert System
    private android.media.MediaPlayer alarmPlayer;
    private androidx.appcompat.app.AlertDialog activeEmergencyDialog;
    private String currentDialogAlertId = null;
    private final Set<String> shownPopupAlertIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // com.google.firebase.firestore.FirebaseFirestore.getInstance().disableNetwork();

        sessionManager = new SessionManager(this);
        db = FirebaseFirestore.getInstance();
        dashboardStartTime = System.currentTimeMillis();

        // 1. Security Check
        String role = sessionManager.getUserRole();
        if (!RoleManager.checkAccess(this, role, RoleManager.ROLE_ADMIN_CAREGIVER)) {
            finish();
            return;
        }
        SecurityGuard.verifyRole(this, RoleManager.ROLE_ADMIN_CAREGIVER, null);

        setContentView(R.layout.activity_admin_dashboard);

        // 2. Bind Header UI
        txtUserRole = findViewById(R.id.txtUserRole);
        txtUserEmail = findViewById(R.id.txtUserEmail);
        btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setText("Logout");
        }

        HashMap<String, String> userDetails = sessionManager.getUserDetails();
        currentUid = userDetails.get(SessionManager.KEY_USER_UID);
        currentEmail = userDetails.get(SessionManager.KEY_USER_EMAIL);
        currentRole = role;
        currentName = currentEmail != null ? currentEmail.split("@")[0] : "Caregiver";

        if (currentUid != null) {
            db.collection("users").document(currentUid).update("online", true);
            db.collection("users").document(currentUid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            currentName = doc.getString("name");
                        }
                    });
        }

        txtUserEmail.setText(currentEmail != null ? currentEmail : "Not available");
        txtUserRole.setText(role != null ? role : getString(R.string.role_caregiver));

        btnLogout.setOnClickListener(v -> performLogout());

        btnBellNotificationSettings = findViewById(R.id.btnBellNotificationSettings);
        if (btnBellNotificationSettings != null) {
            btnBellNotificationSettings.setOnClickListener(v -> showNotificationSettingsDialog());
        }


        // 3. Bind Navigation & ViewPager2 Elements
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        AdminPagerAdapter pagerAdapter = new AdminPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(3);

        viewPager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (tabLayout != null && tabLayout.getSelectedTabPosition() != position) {
                    TabLayout.Tab tab = tabLayout.getTabAt(position);
                    if (tab != null) {
                        tab.select();
                    }
                }
            }
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (viewPager != null && viewPager.getCurrentItem() != position) {
                    viewPager.setCurrentItem(position, true);
                }
                
                switch (position) {
                    case 0:
                        deselectActiveChat();
                        break;
                    case 1:
                        clearEmergenciesTabBadge();
                        deselectActiveChat();
                        break;
                    case 2:
                        deselectActiveChat();
                        break;
                    case 3:
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 7. Initialize Audio & Notification Utilities
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        createNotificationChannel();
        initGoogleTTS();

        // 8. Load Data & Attach Snapshot Listeners
        startRealTimeSync();

        // Request Android 13+ Notification Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Start Central Real-Time Background Service
        Intent serviceIntent = new Intent(this, FirestoreRealtimeService.class);
        startService(serviceIntent);

        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;

        boolean isEmergency = intent.getBooleanExtra("isEmergency", false);
        int openTab = intent.getIntExtra("openTab", -1);
        String rxId = intent.getStringExtra("receiverId");

        if (isEmergency || openTab == 1) {
            stopEmergencyRingtoneSound();
            CaregiverSoundManager.stopNotificationSound(this);
            if (tabLayout != null) {
                tabLayout.post(() -> {
                    TabLayout.Tab tab = tabLayout.getTabAt(1);
                    if (tab != null) {
                        tab.select();
                    }
                });
            }
        } else if (rxId != null && !rxId.isEmpty()) {
            CaregiverSoundManager.stopNotificationSound(this);
            Intent chatIntent = new Intent(this, ChatActivity.class);
            chatIntent.putExtras(intent);
            startActivity(chatIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        CaregiverSoundManager.stopNotificationSound(this);
    }

    public void onUsersViewCreated(View view) {
        layoutUsersTab = view;
        searchView = view.findViewById(R.id.searchView);
        chipGroupRoles = view.findViewById(R.id.chipGroupRoles);
        txtUsersCount = view.findViewById(R.id.txtUsersCount);
        rvUsers = view.findViewById(R.id.rvUsers);

        setupUsersListView();
        updateUsersAdapter();
    }

    public void onEmergenciesViewCreated(View view) {
        layoutEmergenciesTab = view;
        cardSystemStatus = view.findViewById(R.id.cardSystemStatus);
        viewAlarmIndicator = view.findViewById(R.id.viewAlarmIndicator);
        txtAlarmStatus = view.findViewById(R.id.txtAlarmStatus);
        rvActiveEmergencies = view.findViewById(R.id.rvActiveEmergencies);
        rvResolvedEmergencies = view.findViewById(R.id.rvResolvedEmergencies);
        searchViewEmergencies = view.findViewById(R.id.searchViewEmergencies);
        txtNoActiveEmergencies = view.findViewById(R.id.txtNoActiveEmergencies);
        txtNoResolvedEmergencies = view.findViewById(R.id.txtNoResolvedEmergencies);

        if (searchViewEmergencies != null) {
            searchViewEmergencies.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    currentEmergenciesQuery = query;
                    filterEmergencies();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    currentEmergenciesQuery = newText;
                    filterEmergencies();
                    return true;
                }
            });
        }

        setupEmergenciesView();
        updateActiveEmergenciesAdapter();
        updateResolvedEmergenciesAdapter();
    }

    public void onConnectionsViewCreated(View view) {
        layoutConnectionsTab = view;
        layoutActiveConnectionsList = view.findViewById(R.id.layoutActiveConnectionsList);
        txtPendingSectionTitle = view.findViewById(R.id.txtPendingSectionTitle);
        layoutReceivedRequestsList = view.findViewById(R.id.layoutReceivedRequestsList);
        loadingOverlayTab = view.findViewById(R.id.loadingOverlayTab);
        searchViewConnections = view.findViewById(R.id.searchViewConnections);
        txtNoConnectionsFound = view.findViewById(R.id.txtNoConnectionsFound);

        if (searchViewConnections != null) {
            searchViewConnections.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    currentConnectionsQuery = query;
                    filterConnections(currentConnectionsQuery);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    currentConnectionsQuery = newText;
                    filterConnections(currentConnectionsQuery);
                    return true;
                }
            });
        }

        setupConnectionsView();
    }

    public void onCommunicationViewCreated(View view) {
        layoutCommunicationTab = view;
        rvCommunicationMessages = view.findViewById(R.id.rvCommunicationMessages);
        rvCaregiverChatMessages = view.findViewById(R.id.rvCaregiverChatMessages);
        layoutCaregiverChatEmptyState = view.findViewById(R.id.layoutCaregiverChatEmptyState);
        layoutCaregiverChatWindow = view.findViewById(R.id.layoutCaregiverChatWindow);
        txtChatHeaderName = view.findViewById(R.id.txtChatHeaderName);
        txtChatHeaderStatus = view.findViewById(R.id.txtChatHeaderStatus);
        editCaregiverChatMessage = view.findViewById(R.id.editCaregiverChatMessage);
        btnCaregiverChatSend = view.findViewById(R.id.btnCaregiverChatSend);
        btnCaregiverChatEmoji = view.findViewById(R.id.btnCaregiverChatEmoji);
        layoutEmojiTray = view.findViewById(R.id.layoutEmojiTray);

        setupCommunicationView();
    }

    private void setupUsersListView() {
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(this::showUserDetailsDialog);
        rvUsers.setAdapter(userAdapter);

        // Search Input Listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                userAdapter.filter(currentSearchQuery, currentRoleFilter);
                updateCountLabel();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                userAdapter.filter(currentSearchQuery, currentRoleFilter);
                updateCountLabel();
                return true;
            }
        });

        // Filter Chip Listener
        chipGroupRoles.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) {
                currentRoleFilter = "All";
            } else if (checkedId == R.id.chipMute) {
                currentRoleFilter = RoleManager.ROLE_MUTE_USER;
            } else if (checkedId == R.id.chipSpeech) {
                currentRoleFilter = RoleManager.ROLE_SPEECH_IMPAIRED;
            } else if (checkedId == R.id.chipBlind) {
                currentRoleFilter = RoleManager.ROLE_BLIND_USER;
            } else if (checkedId == R.id.chipCaregivers) {
                currentRoleFilter = RoleManager.ROLE_ADMIN_CAREGIVER;
            }
            userAdapter.filter(currentSearchQuery, currentRoleFilter);
            updateCountLabel();
        });
    }

    private void   updateCountLabel() {
        if (txtUsersCount != null && userAdapter != null) {
            txtUsersCount.setText("Showing: " + userAdapter.getItemCount() + " users");
        }
    }

    private void setupEmergenciesView() {
        rvActiveEmergencies.setLayoutManager(new LinearLayoutManager(this));
        rvResolvedEmergencies.setLayoutManager(new LinearLayoutManager(this));

        activeEmergenciesAdapter = new EmergencyAdapter(new EmergencyAdapter.OnAlertActionListener() {
            @Override
            public void onViewClick(DocumentSnapshot doc) {
                stopEmergencyRingtoneSound();
                showEmergencyDetailsDialog(doc);
            }

            @Override
            public void onAcknowledgeClick(DocumentSnapshot doc) {
                stopEmergencyRingtoneSound();
                String alertId = doc.getString("alertId");
                if (alertId != null) {
                    acknowledgeAlertInFirestore(alertId);
                }
            }

            @Override
            public void onResolveClick(DocumentSnapshot doc) {
                stopEmergencyRingtoneSound();
                String alertId = doc.getString("alertId");
                if (alertId != null) {
                    resolveAlertInFirestore(alertId);
                }
            }
        });

        resolvedEmergenciesAdapter = new EmergencyAdapter(new EmergencyAdapter.OnAlertActionListener() {
            @Override
            public void onViewClick(DocumentSnapshot doc) {
                showEmergencyDetailsDialog(doc);
            }

            @Override
            public void onAcknowledgeClick(DocumentSnapshot doc) {}

            @Override
            public void onResolveClick(DocumentSnapshot doc) {}
        });

        rvActiveEmergencies.setAdapter(activeEmergenciesAdapter);
        rvResolvedEmergencies.setAdapter(resolvedEmergenciesAdapter);
    }

    private void setupConnectionsView() {
        refreshConnectionState();
    }

    private void setupCommunicationView() {
        // Patient List
        rvCommunicationMessages.setLayoutManager(new LinearLayoutManager(this));
        patientListAdapter = new PatientListAdapter(this, patient -> {
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                Intent intent = new Intent(AdminDashboardActivity.this, ChatActivity.class);
                intent.putExtra("receiverId", patient.getId());
                intent.putExtra("receiverName", patient.getString("name"));
                intent.putExtra("receiverRole", patient.getString("role"));
                intent.putExtra("receiverEmail", patient.getString("email"));
                intent.putExtra("receiverLanguage", patient.getString("language"));
                startActivity(intent);
            } else {
                selectPatientForChat(patient);
            }
        });
        rvCommunicationMessages.setAdapter(patientListAdapter);

        // Search Bar Setup
        androidx.appcompat.widget.SearchView searchViewPatients = layoutCommunicationTab != null ?
                layoutCommunicationTab.findViewById(R.id.searchViewPatients) : findViewById(R.id.searchViewPatients);
        if (searchViewPatients != null) {
            searchViewPatients.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    filterPatientsList(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filterPatientsList(newText);
                    return true;
                }
            });
        }

        // Chat Window Widgets
        if (editCaregiverChatMessage != null) {
            editCaregiverChatMessage.addTextChangedListener(new android.text.TextWatcher() {
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
                                .update("typingTo", isCurrentlyTyping ? selectedPatientUid : null);
                        }
                    }
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }

        if (rvCaregiverChatMessages != null) {
            rvCaregiverChatMessages.setLayoutManager(new LinearLayoutManager(this));
            caregiverChatAdapter = new CaregiverChatAdapter(currentUid, text -> {
                if (isTtsInitialized && tts != null) {
                    CaregiverSoundManager.speakWithVolume(tts, text, "CaregiverMessageTTS", AdminDashboardActivity.this);
                }
            });
            rvCaregiverChatMessages.setAdapter(caregiverChatAdapter);
            rvCaregiverChatMessages.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });
        }

        if (btnCaregiverChatSend != null) {
            btnCaregiverChatSend.setOnClickListener(v -> {
                if (isCaregiverAudioRecording) {
                    stopAndSendCaregiverVoiceRecording();
                } else {
                    sendCaregiverChatMessage();
                }
            });
        }

        btnCaregiverChatMic = layoutCommunicationTab != null ? layoutCommunicationTab.findViewById(R.id.btnCaregiverChatMic) : findViewById(R.id.btnCaregiverChatMic);
        if (btnCaregiverChatMic != null) {
            btnCaregiverChatMic.setOnClickListener(v -> toggleCaregiverVoiceRecording());
            btnCaregiverChatMic.setOnLongClickListener(v -> {
                toggleCaregiverVoiceInput();
                return true;
            });
        }

        View btnCaregiverCallPatient = layoutCommunicationTab != null ? layoutCommunicationTab.findViewById(R.id.btnCaregiverCallPatient) : findViewById(R.id.btnCaregiverCallPatient);
        if (btnCaregiverCallPatient != null) {
            btnCaregiverCallPatient.setOnClickListener(v -> {
                if (selectedPatientUid != null && !selectedPatientUid.isEmpty()) {
                    initiateCaregiverVoiceCall(selectedPatientUid, selectedPatientName, selectedPatientRole);
                } else {
                    Toast.makeText(this, "Select a patient to call", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnCaregiverChatEmoji != null && layoutEmojiTray != null) {
            btnCaregiverChatEmoji.setOnClickListener(v -> {
                if (layoutEmojiTray.getVisibility() == View.VISIBLE) {
                    layoutEmojiTray.setVisibility(View.GONE);
                } else {
                    layoutEmojiTray.setVisibility(View.VISIBLE);
                }
            });

            for (int i = 0; i < layoutEmojiTray.getChildCount(); i++) {
                View child = layoutEmojiTray.getChildAt(i);
                if (child instanceof TextView) {
                    TextView emojiTv = (TextView) child;
                    emojiTv.setOnClickListener(ev -> {
                        String emoji = emojiTv.getText().toString();
                        int start = editCaregiverChatMessage.getSelectionStart();
                        int end = editCaregiverChatMessage.getSelectionEnd();
                        if (start >= 0) {
                            editCaregiverChatMessage.getText().replace(Math.min(start, end), Math.max(start, end),
                                    emoji, 0, emoji.length());
                        } else {
                            editCaregiverChatMessage.append(emoji);
                        }
                    });
                }
            }
        }

        // Initialize Responsive Layout
        updateResponsiveCommunicationLayout();

        // Also keep legacy adapter initialized to avoid potential NullPointerExceptions
        communicationAdapter = new CaregiverMessageAdapter(this);

        // Load connected patients
        loadConnectedPatients();
    }

    private void refreshConnectionState() {
        loadActiveConnections();
        loadPendingRequests();
        updateUsersAdapter();
        loadConnectedPatients();
    }

    private void loadActiveConnections() {
        if (layoutActiveConnectionsList == null) return;
        layoutActiveConnectionsList.removeAllViews();

        db.collection("caregiver_connections")
                .whereEqualTo("status", "Accepted")
                .get()
                .addOnCompleteListener(task -> {
                    List<Map<String, Object>> activeList = new ArrayList<>();

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
                                
                                // Auto-heal connection caregiver UID if there's a mismatch
                                if (currentEmail != null && currentEmail.equalsIgnoreCase(receiverEmail) && 
                                    currentUid != null && !currentUid.isEmpty() && !currentUid.equals(receiver)) {
                                    db.collection("caregiver_connections").document(doc.getId())
                                            .update("receiverUid", currentUid, "caregiverUid", currentUid);
                                }
                                
                                String senderName = doc.getString("senderName");
                                if (senderName == null || senderName.isEmpty()) senderName = doc.getString("patientName");
                                String senderRole = doc.getString("senderRole");
                                if (senderRole == null || senderRole.isEmpty()) senderRole = doc.getString("patientRole");

                                String receiverName = doc.getString("receiverName");
                                if (receiverName == null || receiverName.isEmpty()) receiverName = doc.getString("caregiverName");
                                String receiverRole = doc.getString("receiverRole");
                                if (receiverRole == null || receiverRole.isEmpty()) receiverRole = doc.getString("caregiverRole");
                                
                                boolean exists = false;
                                for (Map<String, Object> existing : activeList) {
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
                                    map.put("senderName", senderName != null ? senderName : (senderEmail != null ? senderEmail.split("@")[0] : "Patient"));
                                    map.put("receiverName", receiverName != null ? receiverName : (receiverEmail != null ? receiverEmail.split("@")[0] : "Caregiver"));
                                    map.put("senderRole", senderRole != null ? senderRole : RoleManager.ROLE_SPEECH_IMPAIRED);
                                    map.put("receiverRole", receiverRole != null ? receiverRole : RoleManager.ROLE_ADMIN_CAREGIVER);
                                    activeList.add(map);
                                }
                            }
                        }
                    }

                    // Fallback to local simulator
                    List<Map<String, Object>> local = LocalConnectionSimulator.getConnections(this);
                    for (Map<String, Object> conn : local) {
                        String status = (String) conn.get("status");
                        String sender = (String) conn.get("senderUid");
                        String receiver = (String) conn.get("receiverUid");
                        String senderEmail = (String) conn.get("senderEmail");
                        String receiverEmail = (String) conn.get("receiverEmail");
                        if ("Accepted".equalsIgnoreCase(status) && 
                            (currentUid.equals(sender) || currentUid.equals(receiver) || 
                             (currentEmail != null && (currentEmail.equalsIgnoreCase(senderEmail) || currentEmail.equalsIgnoreCase(receiverEmail))))) {
                            
                            boolean exists = false;
                            for (Map<String, Object> existing : activeList) {
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
                                activeList.add(conn);
                            }
                        }
                    }

                    // Populate latestActiveConnectionsList and filter connections in real-time
                    latestActiveConnectionsList.clear();
                    latestActiveConnectionsList.addAll(activeList);
                    filterConnections(currentConnectionsQuery);
                });
    }

    private void filterConnections(String query) {
        if (layoutActiveConnectionsList == null) return;
        layoutActiveConnectionsList.removeAllViews();

        String lowercaseQuery = query != null ? query.toLowerCase().trim() : "";
        int matchCount = 0;

        for (Map<String, Object> conn : latestActiveConnectionsList) {
            final String docId = (String) conn.get("docId");
            String senderUid = (String) conn.get("senderUid");
            String otherName, otherEmail, otherRole, otherUid;

            if ((currentUid != null && currentUid.equals(senderUid)) || (currentEmail != null && currentEmail.equalsIgnoreCase((String) conn.get("senderEmail")))) {
                otherName = (String) conn.get("receiverName");
                otherEmail = (String) conn.get("receiverEmail");
                otherRole = (String) conn.get("receiverRole");
                otherUid = (String) conn.get("receiverUid");
            } else {
                otherName = (String) conn.get("senderName");
                otherEmail = (String) conn.get("senderEmail");
                otherRole = (String) conn.get("senderRole");
                otherUid = (String) conn.get("senderUid");
            }

            otherName = otherName != null ? otherName : "";
            otherEmail = otherEmail != null ? otherEmail : "";
            otherRole = otherRole != null ? otherRole : "";
            otherUid = otherUid != null ? otherUid : "";

            boolean matches = true;
            if (!lowercaseQuery.isEmpty()) {
                matches = otherName.toLowerCase().contains(lowercaseQuery) ||
                          otherEmail.toLowerCase().contains(lowercaseQuery) ||
                          otherRole.toLowerCase().contains(lowercaseQuery) ||
                          otherUid.toLowerCase().contains(lowercaseQuery) ||
                          (docId != null && docId.toLowerCase().contains(lowercaseQuery));
            }

            if (matches) {
                matchCount++;
                View cardView = getLayoutInflater().inflate(R.layout.item_active_connection, null);
                TextView txtTitle = cardView.findViewById(R.id.txtConnectedTitle);
                TextView txtName = cardView.findViewById(R.id.txtConnectedName);
                TextView txtEmail = cardView.findViewById(R.id.txtConnectedEmail);
                TextView txtRole = cardView.findViewById(R.id.txtConnectedRole);
                MaterialButton btnDisc = cardView.findViewById(R.id.btnDisconnect);

                if (RoleManager.ROLE_ADMIN_CAREGIVER.equals(currentRole)) {
                    if (RoleManager.ROLE_PHYSICALLY_DISABLED.equals(otherRole)) {
                        txtTitle.setText("Connected Physically Disabled User");
                    } else {
                        txtTitle.setText("Connected Deaf User");
                    }
                    btnDisc.setText("Disconnect User");
                } else {
                    txtTitle.setText("Connected Caregiver");
                    btnDisc.setText("Disconnect Caregiver");
                }

                txtName.setText(otherName);
                txtEmail.setText(otherEmail);
                if (RoleManager.ROLE_SPEECH_IMPAIRED.equals(otherRole) || RoleManager.ROLE_DEAF_USER.equals(otherRole)) {
                    txtRole.setText("Role: Deaf User");
                } else {
                    txtRole.setText("Role: " + otherRole);
                }
                btnDisc.setOnClickListener(v -> disconnectUser(docId));

                layoutActiveConnectionsList.addView(cardView);
            }
        }

        if (txtNoConnectionsFound != null) {
            if (!lowercaseQuery.isEmpty() && matchCount == 0) {
                txtNoConnectionsFound.setText("No patients found.");
                txtNoConnectionsFound.setVisibility(View.VISIBLE);
            } else {
                txtNoConnectionsFound.setVisibility(View.GONE);
            }
        }
    }

    private void loadPendingRequests() {
        if (layoutReceivedRequestsList == null) return;
        layoutReceivedRequestsList.removeAllViews();

        db.collection("caregiver_connections")
                .whereEqualTo("status", "Pending")
                .get()
                .addOnCompleteListener(task -> {
                    List<Map<String, Object>> requestsList = new ArrayList<>();

                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            String receiverEmail = doc.getString("receiverEmail");
                            if (receiverEmail == null || receiverEmail.isEmpty()) receiverEmail = doc.getString("caregiverEmail");
                            
                            if (currentEmail != null && currentEmail.equalsIgnoreCase(receiverEmail)) {
                                Map<String, Object> map = new HashMap<>(doc.getData());
                                map.put("docId", doc.getId());
                                
                                String senderUid = doc.getString("senderUid");
                                if (senderUid == null || senderUid.isEmpty()) senderUid = doc.getString("patientUid");
                                String senderEmail = doc.getString("senderEmail");
                                if (senderEmail == null || senderEmail.isEmpty()) senderEmail = doc.getString("patientEmail");
                                String senderName = doc.getString("senderName");
                                if (senderName == null || senderName.isEmpty()) senderName = doc.getString("patientName");
                                String senderRole = doc.getString("senderRole");
                                if (senderRole == null || senderRole.isEmpty()) senderRole = doc.getString("patientRole");

                                map.put("senderUid", senderUid);
                                map.put("senderEmail", senderEmail);
                                map.put("senderName", senderName != null ? senderName : (senderEmail != null ? senderEmail.split("@")[0] : "Patient"));
                                map.put("senderRole", senderRole != null ? senderRole : RoleManager.ROLE_SPEECH_IMPAIRED);

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
                                    requestsList.add(map);
                                }
                            }
                        }
                    }

                    // Add local simulator pending requests
                    List<Map<String, Object>> local = LocalConnectionSimulator.getConnections(this);
                    for (Map<String, Object> conn : local) {
                        String status = (String) conn.get("status");
                        String receiver = (String) conn.get("receiverUid");
                        String receiverEmail = (String) conn.get("receiverEmail");
                        if (receiverEmail == null || receiverEmail.isEmpty()) receiverEmail = (String) conn.get("caregiverEmail");
                        
                        if ("Pending".equalsIgnoreCase(status) && 
                            (currentUid.equals(receiver) || (currentEmail != null && currentEmail.equalsIgnoreCase(receiverEmail)))) {
                            
                            String senderUid = (String) conn.get("senderUid");
                            if (senderUid == null || senderUid.isEmpty()) senderUid = (String) conn.get("patientUid");
                            String senderEmail = (String) conn.get("senderEmail");
                            if (senderEmail == null || senderEmail.isEmpty()) senderEmail = (String) conn.get("patientEmail");
                            String senderName = (String) conn.get("senderName");
                            if (senderName == null || senderName.isEmpty()) senderName = (String) conn.get("patientName");
                            String senderRole = (String) conn.get("senderRole");
                            if (senderRole == null || senderRole.isEmpty()) senderRole = (String) conn.get("patientRole");

                            conn.put("senderUid", senderUid);
                            conn.put("senderEmail", senderEmail);
                            conn.put("senderName", senderName != null ? senderName : (senderEmail != null ? senderEmail.split("@")[0] : "Patient"));
                            conn.put("senderRole", senderRole != null ? senderRole : RoleManager.ROLE_SPEECH_IMPAIRED);
                            
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
                                requestsList.add(conn);
                            }
                        }
                    }

                    layoutReceivedRequestsList.removeAllViews();
                    if (requestsList.isEmpty()) {
                        txtPendingSectionTitle.setVisibility(View.GONE);
                        return;
                    }

                    txtPendingSectionTitle.setVisibility(View.VISIBLE);

                    for (Map<String, Object> doc : requestsList) {
                        final String docId = (String) doc.get("docId");
                        String name = (String) doc.get("senderName");
                        String email = (String) doc.get("senderEmail");
                        String role = (String) doc.get("senderRole");

                        View itemView = getLayoutInflater().inflate(R.layout.item_connection_request, null);
                        TextView txtName = itemView.findViewById(R.id.txtReqName);
                        TextView txtEmail = itemView.findViewById(R.id.txtReqEmail);
                        TextView txtRole = itemView.findViewById(R.id.txtReqRole);
                        MaterialButton btnAccept = itemView.findViewById(R.id.btnAccept);
                        MaterialButton btnReject = itemView.findViewById(R.id.btnReject);

                        txtName.setText(name);
                        txtEmail.setText(email);
                        txtRole.setText("Role: " + role);

                        btnAccept.setOnClickListener(v -> acceptRequest(docId));
                        btnReject.setOnClickListener(v -> rejectRequest(docId));

                        layoutReceivedRequestsList.addView(itemView);
                    }
                });
    }

    private void acceptRequest(String docId) {
        LocalConnectionSimulator.updateConnectionStatus(this, docId, "Accepted");
        if (currentUid != null && !currentUid.isEmpty()) {
            LocalConnectionSimulator.updateConnectionUid(this, docId, currentUid);
        }
        Toast.makeText(this, "Connection request accepted!", Toast.LENGTH_SHORT).show();
        refreshConnectionState();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "Accepted");
        if (currentUid != null && !currentUid.isEmpty()) {
            updates.put("receiverUid", currentUid);
            updates.put("caregiverUid", currentUid);
            updates.put("receiverEmail", currentEmail != null ? currentEmail.toLowerCase().trim() : "");
            updates.put("caregiverEmail", currentEmail != null ? currentEmail.toLowerCase().trim() : "");
            updates.put("receiverName", currentName != null ? currentName : "Caregiver");
            updates.put("caregiverName", currentName != null ? currentName : "Caregiver");
            updates.put("receiverRole", currentRole != null ? currentRole : RoleManager.ROLE_ADMIN_CAREGIVER);
            updates.put("caregiverRole", currentRole != null ? currentRole : RoleManager.ROLE_ADMIN_CAREGIVER);
        }

        db.collection("caregiver_connections").document(docId)
                .update(updates)
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

    private void startRealTimeSync() {
        // A. Listen to user profile creations/changes
        usersListener = db.collection("users")
                .orderBy("registrationDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    latestUsersSnapshot = value;
                    updateUsersAdapter();
                });

        // D. Listen to caregiver direct messages
        if (currentEmail != null) {
            communicationListener = db.collection("caregiver_messages")
                    .whereEqualTo("recipientEmail", currentEmail.toLowerCase().trim())
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Communication listener failed: " + error.getMessage());
                            return;
                        }
                        if (value != null && communicationAdapter != null) {
                            communicationAdapter.setMessages(value.getDocuments());
                        }
                    });
        }

        // B. Listen to emergency alerts (SOS triggers)
        // 1. Listen to active (new and acknowledged) emergency alerts (no composite index needed)
        emergenciesActiveListener = db.collection("emergency_alerts")
                .whereIn("status", Arrays.asList("NEW", "ACKNOWLEDGED"))
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Active emergencies listener failed: " + error.getMessage());
                        return;
                    }
                    if (value == null) return;
                    latestActiveEmergenciesSnapshot = value;
                    updateActiveEmergenciesAdapter();
                });

        // 2. Listen to the 50 most recent emergency alerts and extract resolved ones (no composite index needed)
        emergenciesResolvedListener = db.collection("emergency_alerts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Resolved emergencies listener failed: " + error.getMessage());
                        return;
                    }
                    if (value == null) return;
                    latestResolvedEmergenciesSnapshot = value;
                    updateResolvedEmergenciesAdapter();
                });

        // C. Listen to caregiver linkages
        connectionsListener = db.collection("caregiver_connections")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Connections sync failed: " + error.getMessage());
                        return;
                    }
                    if (value != null) {
                        refreshConnectionState();
                    }
                });
    }

    private void updateUsersAdapter() {
        List<UserRecord> userRecords = new ArrayList<>();
        Set<String> seenEmails = new HashSet<>();
        Set<String> seenUids = new HashSet<>();

        if (latestUsersSnapshot != null) {
            for (DocumentSnapshot doc : latestUsersSnapshot.getDocuments()) {
                String role = doc.getString("role");
                if (!RoleManager.ROLE_ADMIN_CAREGIVER.equals(role)) {
                    String uid = doc.getId();
                    String email = doc.getString("email");
                    String normalizedEmail = (email != null && !email.trim().isEmpty()) ? email.toLowerCase().trim() : null;

                    boolean isConnected = false;
                    if (uid != null && connectedPatientUids.contains(uid)) {
                        isConnected = true;
                    }
                    if (normalizedEmail != null && connectedPatientEmails.contains(normalizedEmail)) {
                        isConnected = true;
                    }
                    if (isConnected) {
                        boolean isDuplicate = false;
                        if (normalizedEmail != null && seenEmails.contains(normalizedEmail)) {
                            isDuplicate = true;
                        }
                        if (uid != null && !uid.trim().isEmpty() && seenUids.contains(uid.trim())) {
                            isDuplicate = true;
                        }

                        if (!isDuplicate) {
                            if (normalizedEmail != null) seenEmails.add(normalizedEmail);
                            if (uid != null && !uid.trim().isEmpty()) seenUids.add(uid.trim());
                            userRecords.add(new UserRecord(doc));
                        } else {
                            // Stale duplicate document found in Firestore users collection
                            // latestUsersSnapshot is sorted by registrationDate DESCENDING,
                            // so the first one seen was the latest. This one is an older duplicate.
                            Log.w(TAG, "Duplicate user found in Firestore with email: " + normalizedEmail + " (docId=" + uid + "). Cleaning up stale duplicate.");
                            try {
                                doc.getReference().delete()
                                        .addOnSuccessListener(aVoid -> Log.i(TAG, "Successfully removed stale duplicate user document: " + uid))
                                        .addOnFailureListener(e -> Log.w(TAG, "Could not delete stale user document: " + e.getMessage()));
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        }

        // Merge locally connected users as offline support fallback
        List<Map<String, Object>> local = LocalConnectionSimulator.getConnections(this);
        for (Map<String, Object> conn : local) {
            String status = (String) conn.get("status");
            String senderEmail = (String) conn.get("senderEmail");
            String receiverEmail = (String) conn.get("receiverEmail");
            String senderUid = (String) conn.get("senderUid");
            String receiverUid = (String) conn.get("receiverUid");

            // Strictly check that the current caregiver is one of the parties in the connection
            boolean isUserParty = false;
            if (currentUid != null && (currentUid.equals(senderUid) || currentUid.equals(receiverUid))) {
                isUserParty = true;
            }
            if (currentEmail != null && (currentEmail.equalsIgnoreCase(senderEmail) || currentEmail.equalsIgnoreCase(receiverEmail))) {
                isUserParty = true;
            }

            if ("Accepted".equalsIgnoreCase(status) && isUserParty) {
                String uEmail, uName, uUid, uRole;
                if (currentEmail != null && currentEmail.equalsIgnoreCase(senderEmail)) {
                    uEmail = receiverEmail;
                    uName = (String) conn.get("receiverName");
                    uUid = (String) conn.get("receiverUid");
                    uRole = (String) conn.get("receiverRole");
                } else {
                    uEmail = senderEmail;
                    uName = (String) conn.get("senderName");
                    uUid = (String) conn.get("senderUid");
                    uRole = (String) conn.get("senderRole");
                }

                if (RoleManager.ROLE_ADMIN_CAREGIVER.equals(uRole)) {
                    continue;
                }

                String normalizedEmail = (uEmail != null && !uEmail.trim().isEmpty()) ? uEmail.toLowerCase().trim() : null;
                boolean exists = false;
                if (normalizedEmail != null && seenEmails.contains(normalizedEmail)) {
                    exists = true;
                }
                if (uUid != null && !uUid.trim().isEmpty() && seenUids.contains(uUid.trim())) {
                    exists = true;
                }

                if (!exists && uEmail != null) {
                    if (normalizedEmail != null) seenEmails.add(normalizedEmail);
                    if (uUid != null && !uUid.trim().isEmpty()) seenUids.add(uUid.trim());
                    Map<String, Object> mockProfile = new HashMap<>();
                    mockProfile.put("uid", uUid != null ? uUid : "mock_uid_" + uEmail.hashCode());
                    mockProfile.put("email", uEmail);
                    mockProfile.put("name", uName != null ? uName : uEmail.split("@")[0]);
                    mockProfile.put("role", uRole != null ? uRole : RoleManager.ROLE_SPEECH_IMPAIRED);
                    mockProfile.put("registrationDate", com.google.firebase.Timestamp.now());
                    userRecords.add(new UserRecord(mockProfile));
                }
            }
        }

        if (userAdapter != null) {
            userAdapter.setUsers(userRecords);
            userAdapter.filter(currentSearchQuery, currentRoleFilter);
        }
        if (communicationAdapter != null) {
            communicationAdapter.setUsers(userRecords);
        }
        updateCountLabel();
    }

    private void updateActiveEmergenciesAdapter() {
        List<DocumentSnapshot> activeList = new ArrayList<>();
        if (latestActiveEmergenciesSnapshot != null) {
            for (DocumentSnapshot doc : latestActiveEmergenciesSnapshot.getDocuments()) {
                String patientId = doc.getString("patientId");
                if (patientId == null || patientId.isEmpty()) patientId = doc.getString("userId");
                if (patientId == null || patientId.isEmpty()) patientId = doc.getString("uid");
                if (patientId != null && connectedPatientUids.contains(patientId)) {
                    activeList.add(doc);
                }
            }
        }

        // Sort activeList by timestamp descending in memory
        activeList.sort((d1, d2) -> {
            Timestamp t1 = d1.getTimestamp("timestamp");
            Timestamp t2 = d2.getTimestamp("timestamp");
            if (t1 != null && t2 != null) return t2.compareTo(t1);
            return 0;
        });

        if (activeEmergenciesAdapter != null) {
            activeEmergenciesAdapter.setUserEmailMap(buildUserEmailMap());
            activeEmergenciesAdapter.setAlerts(activeList);
        }
        updateEmergenciesEmptyState();
        checkForNewEmergencies(activeList);
    }

    private void updateResolvedEmergenciesAdapter() {
        List<DocumentSnapshot> resolvedList = new ArrayList<>();
        if (latestResolvedEmergenciesSnapshot != null) {
            for (DocumentSnapshot doc : latestResolvedEmergenciesSnapshot.getDocuments()) {
                String status = doc.getString("status");
                if ("RESOLVED".equalsIgnoreCase(status)) {
                    String patientId = doc.getString("patientId");
                    if (patientId == null || patientId.isEmpty()) patientId = doc.getString("userId");
                    if (patientId == null || patientId.isEmpty()) patientId = doc.getString("uid");
                    if (patientId != null && connectedPatientUids.contains(patientId)) {
                        resolvedList.add(doc);
                    }
                }
            }
        }

        if (resolvedEmergenciesAdapter != null) {
            resolvedEmergenciesAdapter.setUserEmailMap(buildUserEmailMap());
            resolvedEmergenciesAdapter.setAlerts(resolvedList);
        }
        updateEmergenciesEmptyState();
    }

    private Map<String, String> buildUserEmailMap() {
        Map<String, String> map = new HashMap<>();
        if (latestUsersSnapshot != null) {
            for (DocumentSnapshot doc : latestUsersSnapshot.getDocuments()) {
                String uid = doc.getId();
                String email = doc.getString("email");
                if (uid != null && email != null) {
                    map.put(uid, email);
                }
            }
        }
        return map;
    }

    private void filterEmergencies() {
        Map<String, String> emailMap = buildUserEmailMap();
        if (activeEmergenciesAdapter != null) {
            activeEmergenciesAdapter.setUserEmailMap(emailMap);
            activeEmergenciesAdapter.filter(currentEmergenciesQuery);
        }
        if (resolvedEmergenciesAdapter != null) {
            resolvedEmergenciesAdapter.setUserEmailMap(emailMap);
            resolvedEmergenciesAdapter.filter(currentEmergenciesQuery);
        }
        updateEmergenciesEmptyState();
    }

    private void updateEmergenciesEmptyState() {
        boolean isSearching = currentEmergenciesQuery != null && !currentEmergenciesQuery.trim().isEmpty();
        int activeFilteredCount = activeEmergenciesAdapter != null ? activeEmergenciesAdapter.getFilteredCount() : 0;
        int resolvedFilteredCount = resolvedEmergenciesAdapter != null ? resolvedEmergenciesAdapter.getFilteredCount() : 0;

        if (isSearching) {
            if (activeFilteredCount == 0 && resolvedFilteredCount == 0) {
                if (txtNoActiveEmergencies != null) {
                    txtNoActiveEmergencies.setText("No patients found.");
                    txtNoActiveEmergencies.setVisibility(View.VISIBLE);
                }
                if (txtNoResolvedEmergencies != null) {
                    txtNoResolvedEmergencies.setVisibility(View.GONE);
                }
            } else {
                if (txtNoActiveEmergencies != null) txtNoActiveEmergencies.setVisibility(View.GONE);
                if (txtNoResolvedEmergencies != null) txtNoResolvedEmergencies.setVisibility(View.GONE);
            }
        } else {
            if (txtNoActiveEmergencies != null) txtNoActiveEmergencies.setVisibility(View.GONE);
            if (txtNoResolvedEmergencies != null) txtNoResolvedEmergencies.setVisibility(View.GONE);
        }
    }



    private void checkForNewEmergencies(List<DocumentSnapshot> activeList) {
        boolean hasNewAlert = false;
        DocumentSnapshot newestNewAlert = null;

        for (DocumentSnapshot doc : activeList) {
            String status = doc.getString("status");
            if ("NEW".equalsIgnoreCase(status)) {
                hasNewAlert = true;
                if (newestNewAlert == null) {
                    newestNewAlert = doc;
                }
            }
        }

        if (hasNewAlert) {
            startContinuousVibration();
            playAlarmSound();
            showEmergenciesTabBadge();

            if (newestNewAlert != null) {
                String alertId = newestNewAlert.getString("alertId");
                if (alertId != null && !shownPopupAlertIds.contains(alertId)) {
                    shownPopupAlertIds.add(alertId);
                    showEmergencyPopupDialog(newestNewAlert);
                }
            }
            
            // Auto scroll to latest emergency
            if (activeList.size() > 0 && rvActiveEmergencies != null) {
                rvActiveEmergencies.scrollToPosition(0);
            }
        } else {
            stopSensoryAlerts();
        }

        // Dismiss popup dialog if the active alert was acknowledged
        if (activeEmergencyDialog != null && activeEmergencyDialog.isShowing() && currentDialogAlertId != null) {
            boolean dialogAlertStillNew = false;
            for (DocumentSnapshot doc : activeList) {
                if (currentDialogAlertId.equals(doc.getString("alertId")) && "NEW".equalsIgnoreCase(doc.getString("status"))) {
                    dialogAlertStillNew = true;
                    break;
                }
            }
            if (!dialogAlertStillNew) {
                stopEmergencyRingtoneSound();
                activeEmergencyDialog.dismiss();
                currentDialogAlertId = null;
            }
        }
    }

    private void showEmergencyPopupDialog(DocumentSnapshot doc) {
        if (activeEmergencyDialog != null && activeEmergencyDialog.isShowing()) {
            stopEmergencyRingtoneSound();
            activeEmergencyDialog.dismiss();
        }

        String alertId = doc.getString("alertId");
        String name = doc.getString("patientName");
        String message = doc.getString("message");
        Timestamp ts = doc.getTimestamp("timestamp");

        currentDialogAlertId = alertId;

        String dateStr = "";
        String timeStr = "";
        if (ts != null) {
            SimpleDateFormat dateFor = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
            SimpleDateFormat timeFor = new SimpleDateFormat("hh:mm a", Locale.US);
            dateStr = dateFor.format(ts.toDate());
            timeStr = timeFor.format(ts.toDate());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_emergency_popup, null);
        TextView txtTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView txtName = dialogView.findViewById(R.id.dialogPatientName);
        TextView txtMsg = dialogView.findViewById(R.id.dialogMessage);
        TextView txtDateTime = dialogView.findViewById(R.id.dialogDateTime);
        Button btnOpen = dialogView.findViewById(R.id.btnOpenAlert);
        Button btnAck = dialogView.findViewById(R.id.btnAcknowledge);

        txtTitle.setText("🚨 EMERGENCY ALERT");
        txtName.setText("Patient: " + (name != null ? name : "Unknown"));
        txtMsg.setText("Message: " + (message != null ? message : "I need immediate help."));
        txtDateTime.setText("Date: " + dateStr + " | Time: " + timeStr);

        builder.setView(dialogView);
        builder.setCancelable(false);

        activeEmergencyDialog = builder.create();
        activeEmergencyDialog.setOnDismissListener(dialog -> stopEmergencyRingtoneSound());

        btnOpen.setOnClickListener(v -> {
            stopEmergencyRingtoneSound();
            TabLayout.Tab tab = tabLayout.getTabAt(1);
            if (tab != null) {
                tab.select();
            }
            scrollToAlert(alertId);
            if (activeEmergencyDialog != null && activeEmergencyDialog.isShowing()) {
                activeEmergencyDialog.dismiss();
            }
        });

        btnAck.setOnClickListener(v -> {
            stopEmergencyRingtoneSound();
            acknowledgeAlertInFirestore(alertId);
            if (activeEmergencyDialog != null && activeEmergencyDialog.isShowing()) {
                activeEmergencyDialog.dismiss();
            }
        });

        activeEmergencyDialog.show();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Admin SOS Notifications";
            String description = "Real-time Patient Emergency notification logs.";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setSound(null, null);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 250, 100, 500, 100, 250, 100, 500});

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void initGoogleTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                isTtsInitialized = true;
            } else {
                Log.e(TAG, "TTS Initialization failed.");
            }
        });
    }

    private void soundContinuousSiren() {
        CaregiverSoundManager.playEmergencyAlertSoundAndVibration(this);
    }

    private void startContinuousVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 800, 400, 800, 400, 800, 400};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    private void stopVibrateCaregiver() {
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    private void playAlarmSound() {
        CaregiverSoundManager.playEmergencyAlertSoundAndVibration(this);
    }

    private void stopAlarmSound() {
        if (alarmPlayer != null) {
            try {
                if (alarmPlayer.isPlaying()) {
                    alarmPlayer.stop();
                }
                alarmPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaPlayer", e);
            }
            alarmPlayer = null;
        }
        if (localSiren != null && localSiren.isPlaying()) {
            localSiren.stop();
        }
    }

    private void stopSensoryAlerts() {
        stopVibrateCaregiver();
        stopAlarmSound();
    }

    private void showEmergenciesTabBadge() {
        if (tabLayout != null) {
            TabLayout.Tab tab = tabLayout.getTabAt(1);
            if (tab != null) {
                BadgeDrawable badge = tab.getOrCreateBadge();
                badge.setVisible(true);
            }
        }
    }

    private void clearEmergenciesTabBadge() {
        if (tabLayout != null) {
            TabLayout.Tab tab = tabLayout.getTabAt(1);
            if (tab != null) {
                tab.removeBadge();
            }
        }
    }

    private void stopActiveAlertWarnings() {
        stopSensoryAlerts();
        if (activeEmergencyDialog != null && activeEmergencyDialog.isShowing()) {
            activeEmergencyDialog.dismiss();
        }
    }

    private void scrollToAlert(String alertId) {
        if (alertId == null) return;
        int pos = activeEmergenciesAdapter.getPositionOfAlert(alertId);
        if (pos != -1 && rvActiveEmergencies != null) {
            rvActiveEmergencies.scrollToPosition(pos);
        } else {
            pos = resolvedEmergenciesAdapter.getPositionOfAlert(alertId);
            if (pos != -1 && rvResolvedEmergencies != null) {
                rvResolvedEmergencies.scrollToPosition(pos);
            }
        }
    }

    private void acknowledgeAlertInFirestore(String alertId) {
        db.collection("emergency_alerts").document(alertId)
                .update("status", "ACKNOWLEDGED", "acknowledged", true)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Alert acknowledged"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to acknowledge alert", e));
    }

    private void resolveAlertInFirestore(String alertId) {
        db.collection("emergency_alerts").document(alertId)
                .update("status", "RESOLVED", "resolved", true)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Alert resolved"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to resolve alert", e));
    }

    private void showEmergencyDetailsDialog(DocumentSnapshot doc) {
        String name = doc.getString("patientName");
        String patientId = doc.getString("patientId");
        String message = doc.getString("message");
        String status = doc.getString("status");
        Timestamp ts = doc.getTimestamp("timestamp");

        String dateStr = "";
        String timeStr = "";
        if (ts != null) {
            SimpleDateFormat dateFor = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
            SimpleDateFormat timeFor = new SimpleDateFormat("hh:mm a", Locale.US);
            dateStr = dateFor.format(ts.toDate());
            timeStr = timeFor.format(ts.toDate());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🚨 Emergency Details");

        StringBuilder details = new StringBuilder();
        details.append("Patient: ").append(name != null ? name : "Unknown").append("\n\n");
        details.append("User ID: ").append(patientId != null ? patientId : "Unknown").append("\n\n");
        details.append("Message: ").append(message != null ? message : "No message").append("\n\n");
        details.append("Date: ").append(dateStr).append("\n\n");
        details.append("Time: ").append(timeStr).append("\n\n");
        details.append("Status: ").append(status != null ? status : "NEW");

        builder.setMessage(details.toString());
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        if ("NEW".equalsIgnoreCase(status)) {
            builder.setNegativeButton("Acknowledge", (dialog, which) -> {
                String alertId = doc.getString("alertId");
                if (alertId != null) {
                    acknowledgeAlertInFirestore(alertId);
                }
                dialog.dismiss();
            });
        } else if ("ACKNOWLEDGED".equalsIgnoreCase(status)) {
            builder.setNegativeButton("Resolve", (dialog, which) -> {
                String alertId = doc.getString("alertId");
                if (alertId != null) {
                    resolveAlertInFirestore(alertId);
                }
                dialog.dismiss();
            });
        }
        builder.create().show();
    }

    /**
     * Inflates custom user profile details dialog.
     */
    private void showUserDetailsDialog(UserRecord userDoc) {
        String uid = userDoc.getId();
        String name = userDoc.getString("name");
        String email = userDoc.getString("email");
        String role = userDoc.getString("role");
        Timestamp regTime = userDoc.getTimestamp("registrationDate");

        name = name != null ? name : "Anonymous User";
        email = email != null ? email : "No email address";
        role = role != null ? role : "Patient";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_details, null);
        builder.setView(dialogView);

        TextView dName = dialogView.findViewById(R.id.dialogName);
        TextView dRole = dialogView.findViewById(R.id.dialogRoleBadge);
        TextView dEmail = dialogView.findViewById(R.id.dialogEmail);
        TextView dRegDate = dialogView.findViewById(R.id.dialogRegDate);
        TextView dConnections = dialogView.findViewById(R.id.dialogConnections);
        TextView dAlerts = dialogView.findViewById(R.id.dialogAlertCount);
        MaterialButton dClose = dialogView.findViewById(R.id.dialogBtnClose);

        dName.setText(name);
        dEmail.setText(email);

        // Format role badge in dialog
        if (RoleManager.ROLE_SPEECH_IMPAIRED.equals(role) || RoleManager.ROLE_DEAF_USER.equals(role)) {
            dRole.setText("Deaf User");
        } else {
            dRole.setText(role.replace(" User", ""));
        }
        int badgeBg = Color.parseColor("#F1F5F9");
        int badgeText = Color.parseColor("#475569");
        if (RoleManager.ROLE_SPEECH_IMPAIRED.equals(role) || RoleManager.ROLE_DEAF_USER.equals(role)) {
            badgeBg = Color.parseColor("#E0E7FF");
            badgeText = Color.parseColor("#4F46E5");
        } else if (RoleManager.ROLE_PHYSICALLY_DISABLED.equals(role)) {
            badgeBg = Color.parseColor("#CCFBF1");
            badgeText = Color.parseColor("#0F766E");
        } else if (RoleManager.ROLE_BLIND_USER.equals(role)) {
            badgeBg = Color.parseColor("#FEF3C7");
            badgeText = Color.parseColor("#D97706");
        }
        dRole.setBackgroundTintList(ColorStateList.valueOf(badgeBg));
        dRole.setTextColor(badgeText);

        // Registration Date format
        if (regTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm", Locale.getDefault());
            dRegDate.setText("Registered: " + sdf.format(regTime.toDate()));
        } else {
            dRegDate.setText("Registered: N/A");
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        MaterialButton dChat = dialogView.findViewById(R.id.dialogBtnChat);
        MaterialButton dCall = dialogView.findViewById(R.id.dialogBtnCall);
        String myUid = sessionManager.getUserDetails().get(SessionManager.KEY_USER_UID);
        if (uid != null && uid.equals(myUid)) {
            dChat.setVisibility(View.GONE);
            if (dCall != null) dCall.setVisibility(View.GONE);
        }
        
        final String finalName = name;
        final String finalRole = role;
        final String finalUid = uid;
        final String finalEmail = email;

        if (dCall != null) {
            dCall.setOnClickListener(v -> {
                dialog.dismiss();
                initiateCaregiverVoiceCall(finalUid, finalName, finalRole);
            });
        }

        dChat.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(AdminDashboardActivity.this, ChatActivity.class);
            intent.putExtra("receiverId", finalUid);
            intent.putExtra("receiverName", finalName);
            intent.putExtra("receiverRole", finalRole);
            intent.putExtra("receiverEmail", finalEmail);
            startActivity(intent);
        });

        dClose.setOnClickListener(v -> dialog.dismiss());

        // Asynchronously load user mapping connections
        if (uid != null) {
            // Fetch Caregiver Connections counts & details
            String queryField = RoleManager.ROLE_ADMIN_CAREGIVER.equals(role) ? "caregiverUid" : "patientUid";
            db.collection("caregiver_connections")
                    .whereEqualTo(queryField, uid)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        if (snapshots.isEmpty()) {
                            dConnections.setText("No connections registered.");
                        } else {
                            int count = 0;
                            StringBuilder listStr = new StringBuilder();
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                String status = doc.getString("status");
                                if ("Accepted".equalsIgnoreCase(status)) {
                                    count++;
                                    String targetEmail = RoleManager.ROLE_ADMIN_CAREGIVER.equals(finalRole) ?
                                            doc.getString("patientEmail") : doc.getString("caregiverEmail");
                                    if (listStr.length() > 0) listStr.append(", ");
                                    listStr.append(targetEmail);
                                }
                            }
                            if (count > 0) {
                                dConnections.setText("Connected: (" + count + " active)\n" + listStr.toString());
                            } else {
                                dConnections.setText("No active connected caregivers (Pending connection approval).");
                            }
                        }
                    })
                    .addOnFailureListener(e -> dConnections.setText("Error loading connections."));

            // Fetch patient alert history counts
            if (!RoleManager.ROLE_ADMIN_CAREGIVER.equals(role)) {
                db.collection("emergencies")
                        .whereEqualTo("uid", uid)
                        .get()
                        .addOnSuccessListener(snapshots -> {
                            if (snapshots.isEmpty()) {
                                dAlerts.setText("No emergency alerts registered.");
                            } else {
                                int unresolved = 0;
                                int resolved = 0;
                                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                    String status = doc.getString("status");
                                    if ("Unresolved".equalsIgnoreCase(status)) {
                                        unresolved++;
                                    } else {
                                        resolved++;
                                    }
                                }
                                dAlerts.setText("Total alerts: " + (unresolved + resolved) +
                                        " (" + unresolved + " active, " + resolved + " resolved)");
                            }
                        })
                        .addOnFailureListener(e -> dAlerts.setText("Error loading SOS history."));
            } else {
                dAlerts.setVisibility(View.GONE);
            }
        } else {
            dConnections.setText("Uid not found.");
            dAlerts.setText("Uid not found.");
        }
    }

    private void performLogout() {
        if (currentUid != null) {
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
        if (isCaregiverAudioRecording) {
            cancelCaregiverVoiceRecording();
        }
        CaregiverChatAdapter.stopAudioPlayback();
        ChatAdapter.stopAudioPlayback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isCaregiverAudioRecording) {
            cancelCaregiverVoiceRecording();
        }
        CaregiverChatAdapter.stopAudioPlayback();
        ChatAdapter.stopAudioPlayback();
        stopActiveAlertWarnings();

        if (currentUid != null) {
            FirebaseFirestore.getInstance().collection("users")
                    .document(currentUid)
                    .update("online", false, "lastSeen", com.google.firebase.Timestamp.now(), "typingTo", null);
        }

        if (activeChatUserPresenceListener != null) {
            activeChatUserPresenceListener.remove();
        }

        if (usersListener != null) {
            usersListener.remove();
        }
        if (emergenciesActiveListener != null) {
            emergenciesActiveListener.remove();
        }
        if (emergenciesResolvedListener != null) {
            emergenciesResolvedListener.remove();
        }
        if (connectionsListener != null) {
            connectionsListener.remove();
        }
        if (communicationListener != null) {
            communicationListener.remove();
        }
        if (caregiverChatListener != null) {
            caregiverChatListener.remove();
        }

        for (ListenerRegistration listener : chatInfoListeners.values()) {
            if (listener != null) listener.remove();
        }
        chatInfoListeners.clear();

        for (ListenerRegistration listener : onlineStatusListeners.values()) {
            if (listener != null) listener.remove();
        }
        onlineStatusListeners.clear();

        if (caregiverSpeechRecognizer != null) {
            caregiverSpeechRecognizer.destroy();
            caregiverSpeechRecognizer = null;
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResponsiveCommunicationLayout();
    }

    private void updateResponsiveCommunicationLayout() {
        View root = layoutCommunicationTab != null ? layoutCommunicationTab : this.getWindow().getDecorView();
        LinearLayout layoutCommunicationSplit = root.findViewById(R.id.layoutCommunicationSplit);
        if (layoutCommunicationSplit != null) {
            int orientation = getResources().getConfiguration().orientation;
            View listPane = root.findViewById(R.id.layoutPatientListPane);
            View chatPane = root.findViewById(R.id.layoutChatPane);

            if (orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                layoutCommunicationSplit.setOrientation(LinearLayout.VERTICAL);
                
                if (listPane != null) {
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                    );
                    listPane.setLayoutParams(lp);
                }
                if (chatPane != null) {
                    chatPane.setVisibility(View.GONE);
                }
            } else {
                layoutCommunicationSplit.setOrientation(LinearLayout.HORIZONTAL);
                
                if (listPane != null) {
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            (int) (280 * getResources().getDisplayMetrics().density),
                            LinearLayout.LayoutParams.MATCH_PARENT
                    );
                    listPane.setLayoutParams(lp);
                }
                if (chatPane != null) {
                    chatPane.setVisibility(View.VISIBLE);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1.0f
                    );
                    chatPane.setLayoutParams(lp);
                }
            }
        }
    }

    private void loadConnectedPatients() {
        if (db == null) return;
        
        db.collection("caregiver_connections")
                .whereEqualTo("status", "Accepted")
                .get()
                .addOnCompleteListener(task -> {
                    List<UserRecord> patients = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            String senderUid = doc.getString("senderUid");
                            if (senderUid == null || senderUid.isEmpty()) senderUid = doc.getString("patientUid");
                            
                            String receiverUid = doc.getString("receiverUid");
                            if (receiverUid == null || receiverUid.isEmpty()) receiverUid = doc.getString("caregiverUid");
                            
                            String senderEmail = doc.getString("senderEmail");
                            if (senderEmail == null || senderEmail.isEmpty()) senderEmail = doc.getString("patientEmail");
                            
                            String receiverEmail = doc.getString("receiverEmail");
                            if (receiverEmail == null || receiverEmail.isEmpty()) receiverEmail = doc.getString("caregiverEmail");
                            
                            String senderName = doc.getString("senderName");
                            if (senderName == null || senderName.isEmpty()) senderName = doc.getString("patientName");
                            
                            String receiverName = doc.getString("receiverName");
                            if (receiverName == null || receiverName.isEmpty()) receiverName = doc.getString("caregiverName");
                            
                            String senderRole = doc.getString("senderRole");
                            if (senderRole == null || senderRole.isEmpty()) senderRole = doc.getString("patientRole");
                            
                            String receiverRole = doc.getString("receiverRole");
                            if (receiverRole == null || receiverRole.isEmpty()) receiverRole = doc.getString("caregiverRole");
                            
                            if (senderUid != null && receiverUid != null && (currentUid.equals(senderUid) || currentUid.equals(receiverUid) ||
                                (currentEmail != null && (currentEmail.equalsIgnoreCase(senderEmail) || currentEmail.equalsIgnoreCase(receiverEmail))))) {
                                String pUid, pName, pEmail, pRole;
                                if (currentUid.equals(senderUid) || (currentEmail != null && currentEmail.equalsIgnoreCase(senderEmail))) {
                                    pUid = receiverUid;
                                    pName = receiverName != null ? receiverName : (receiverEmail != null ? receiverEmail.split("@")[0] : "Patient");
                                    pEmail = receiverEmail;
                                    pRole = receiverRole != null ? receiverRole : RoleManager.ROLE_SPEECH_IMPAIRED;
                                } else {
                                    pUid = senderUid;
                                    pName = senderName != null ? senderName : (senderEmail != null ? senderEmail.split("@")[0] : "Patient");
                                    pEmail = senderEmail;
                                    pRole = senderRole != null ? senderRole : RoleManager.ROLE_SPEECH_IMPAIRED;
                                }
                                
                                if (latestUsersSnapshot != null && pUid != null) {
                                    for (DocumentSnapshot uDoc : latestUsersSnapshot.getDocuments()) {
                                        if (pUid.equals(uDoc.getId()) || (pEmail != null && pEmail.equalsIgnoreCase(uDoc.getString("email")))) {
                                            String realRole = uDoc.getString("role");
                                            if (realRole != null && !realRole.isEmpty() && !RoleManager.ROLE_ADMIN_CAREGIVER.equals(realRole)) {
                                                pRole = realRole;
                                            }
                                            String realName = uDoc.getString("name");
                                            if (realName != null && !realName.isEmpty()) {
                                                pName = realName;
                                            }
                                            break;
                                        }
                                    }
                                }
                                
                                if (pUid != null) {
                                    boolean exists = false;
                                    for (UserRecord u : patients) {
                                        String extEmail = u.getString("email");
                                        if (pEmail != null && pEmail.equalsIgnoreCase(extEmail)) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                    if (!exists) {
                                        Map<String, Object> profile = new HashMap<>();
                                        profile.put("uid", pUid);
                                        profile.put("name", pName);
                                        profile.put("email", pEmail != null ? pEmail : "");
                                        profile.put("role", pRole);
                                        if (connectedPatientsList != null) {
                                            for (UserRecord old : connectedPatientsList) {
                                                if (pUid.equals(old.getId())) {
                                                    if (old.getData() != null) {
                                                        profile.put("lastMessage", old.getData().get("lastMessage"));
                                                        profile.put("lastMessageTime", old.getData().get("lastMessageTime"));
                                                        profile.put("unreadCount", old.getData().get("unreadCount"));
                                                        profile.put("online", old.getData().get("online"));
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                        patients.add(new UserRecord(profile));
                                    }
                                }
                            }
                        }
                    }

                    // Fallback to local simulator
                    List<Map<String, Object>> local = LocalConnectionSimulator.getConnections(this);
                    for (Map<String, Object> conn : local) {
                        String status = (String) conn.get("status");
                        if ("Accepted".equalsIgnoreCase(status)) {
                            String senderUid = conn.containsKey("senderUid") ? (String) conn.get("senderUid") : null;
                            if (senderUid == null || senderUid.isEmpty()) senderUid = (String) conn.get("patientUid");
                            
                            String receiverUid = conn.containsKey("receiverUid") ? (String) conn.get("receiverUid") : null;
                            if (receiverUid == null || receiverUid.isEmpty()) receiverUid = (String) conn.get("caregiverUid");
                            
                            String senderEmail = conn.containsKey("senderEmail") ? (String) conn.get("senderEmail") : null;
                            if (senderEmail == null || senderEmail.isEmpty()) senderEmail = (String) conn.get("patientEmail");
                            
                            String receiverEmail = conn.containsKey("receiverEmail") ? (String) conn.get("receiverEmail") : null;
                            if (receiverEmail == null || receiverEmail.isEmpty()) receiverEmail = (String) conn.get("caregiverEmail");
                            
                            String senderName = conn.containsKey("senderName") ? (String) conn.get("senderName") : null;
                            if (senderName == null || senderName.isEmpty()) senderName = (String) conn.get("patientName");
                            
                            String receiverName = conn.containsKey("receiverName") ? (String) conn.get("receiverName") : null;
                            if (receiverName == null || receiverName.isEmpty()) receiverName = (String) conn.get("caregiverName");
                            
                            String senderRole = conn.containsKey("senderRole") ? (String) conn.get("senderRole") : null;
                            if (senderRole == null || senderRole.isEmpty()) senderRole = (String) conn.get("patientRole");
                            
                            String receiverRole = conn.containsKey("receiverRole") ? (String) conn.get("receiverRole") : null;
                            if (receiverRole == null || receiverRole.isEmpty()) receiverRole = (String) conn.get("caregiverRole");
                            
                            if (senderUid != null && receiverUid != null && (currentUid.equals(senderUid) || currentUid.equals(receiverUid) || 
                                (currentEmail != null && (currentEmail.equalsIgnoreCase(senderEmail) || currentEmail.equalsIgnoreCase(receiverEmail))))) {
                                
                                String pUid, pName, pEmail, pRole;
                                if (currentUid.equals(senderUid) || (currentEmail != null && currentEmail.equalsIgnoreCase(senderEmail))) {
                                    pUid = receiverUid;
                                    pName = receiverName != null ? receiverName : (receiverEmail != null ? receiverEmail.split("@")[0] : "Patient");
                                    pEmail = receiverEmail;
                                    pRole = receiverRole != null ? receiverRole : RoleManager.ROLE_SPEECH_IMPAIRED;
                                } else {
                                    pUid = senderUid;
                                    pName = senderName != null ? senderName : (senderEmail != null ? senderEmail.split("@")[0] : "Patient");
                                    pEmail = senderEmail;
                                    pRole = senderRole != null ? senderRole : RoleManager.ROLE_SPEECH_IMPAIRED;
                                }
                                
                                boolean exists = false;
                                for (UserRecord u : patients) {
                                    String extEmail = u.getString("email");
                                    if (pEmail != null && pEmail.equalsIgnoreCase(extEmail)) {
                                        exists = true;
                                        break;
                                    }
                                }
                                
                                if (!exists && pUid != null) {
                                    Map<String, Object> profile = new HashMap<>();
                                    profile.put("uid", pUid);
                                    profile.put("name", pName);
                                    profile.put("email", pEmail != null ? pEmail : "");
                                    profile.put("role", pRole);
                                    if (connectedPatientsList != null) {
                                        for (UserRecord old : connectedPatientsList) {
                                            if (pUid.equals(old.getId())) {
                                                if (old.getData() != null) {
                                                    profile.put("lastMessage", old.getData().get("lastMessage"));
                                                    profile.put("lastMessageTime", old.getData().get("lastMessageTime"));
                                                    profile.put("unreadCount", old.getData().get("unreadCount"));
                                                    profile.put("online", old.getData().get("online"));
                                                }
                                                break;
                                            }
                                        }
                                    }
                                    patients.add(new UserRecord(profile));
                                }
                            }
                        }
                    }

                    connectedPatientsList.clear();
                    connectedPatientsList.addAll(patients);

                    // Update the sets of connected patients
                    connectedPatientUids.clear();
                    connectedPatientEmails.clear();
                    for (UserRecord patient : patients) {
                        if (patient.getId() != null) {
                            connectedPatientUids.add(patient.getId());
                        }
                        String email = patient.getString("email");
                        if (email != null && !email.isEmpty()) {
                            connectedPatientEmails.add(email.toLowerCase().trim());
                        }
                    }

                    // Refresh users and emergency lists with the new connection set
                    updateUsersAdapter();
                    updateActiveEmergenciesAdapter();
                    updateResolvedEmergenciesAdapter();
                    
                    // Filter list by current query if any
                    androidx.appcompat.widget.SearchView searchViewPatients = findViewById(R.id.searchViewPatients);
                    String query = searchViewPatients != null ? searchViewPatients.getQuery().toString() : "";
                    filterPatientsList(query);

                    // Start watchers
                    startPatientWatchers(patients);
                });
    }

    private void startPatientWatchers(List<UserRecord> patients) {
        for (UserRecord patient : patients) {
            String patientUid = patient.getId();
            if (patientUid == null) continue;

            // Watcher A: Chat info listener (last message, timestamp, unread count)
            if (!chatInfoListeners.containsKey(patientUid)) {
                String chatId = currentUid.compareTo(patientUid) < 0 ? currentUid + "_" + patientUid : patientUid + "_" + currentUid;
                Query chatQuery = db.collection("caregiver_messages")
                        .whereEqualTo("chatId", chatId);

                ListenerRegistration infoListener = chatQuery.addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        return;
                    }

                    List<DocumentSnapshot> docs = new ArrayList<>(value.getDocuments());
                    java.util.Collections.sort(docs, (d1, d2) -> {
                        Timestamp t1 = d1.getTimestamp("timestamp");
                        Timestamp t2 = d2.getTimestamp("timestamp");
                        if (t1 == null && t2 == null) return 0;
                        if (t1 == null) return 1;
                        if (t2 == null) return -1;
                        return t1.compareTo(t2);
                    });

                    String lastMessage = "";
                    Timestamp lastTime = null;
                    int unreadCount = 0;

                    if (!docs.isEmpty()) {
                        DocumentSnapshot lastDoc = docs.get(docs.size() - 1);
                        lastMessage = lastDoc.getString("message");
                        lastTime = lastDoc.getTimestamp("timestamp");

                        String lastDocMsgType = lastDoc.getString("messageType");
                        String type = lastDoc.getString("type");
                        Boolean isVoice = lastDoc.getBoolean("isVoice");
                        boolean voiceMsg = "voice".equalsIgnoreCase(type) || "voice".equalsIgnoreCase(lastDocMsgType) || (isVoice != null && isVoice);
                        if (voiceMsg && lastMessage != null && !lastMessage.startsWith("🎤")) {
                            lastMessage = "🎤 " + lastMessage;
                        }

                        for (DocumentSnapshot doc : docs) {
                            String senderId = doc.getString("senderId");
                            String status = doc.getString("status");
                            if (senderId != null && senderId.equals(patientUid) && !"read".equals(status)) {
                                unreadCount++;
                            }
                        }

                        // Play real-time notification sound for new incoming messages from this patient
                        String lastMsgId = lastDoc.getId();
                        String lastSenderId = lastDoc.getString("senderId");
                        String lastStatus = lastDoc.getString("status");
                        Timestamp lastMsgTime = lastDoc.getTimestamp("timestamp");
                        if (lastSenderId != null && lastSenderId.equals(patientUid) && !"read".equals(lastStatus)) {
                            if (!playedMessageIds.contains(lastMsgId)) {
                                playedMessageIds.add(lastMsgId);
                                long msgMillis = lastMsgTime != null ? lastMsgTime.toDate().getTime() : System.currentTimeMillis();
                                if (msgMillis >= dashboardStartTime - 15000) {
                                    String msgType = lastDoc.getString("type");
                                    if ("emergency".equalsIgnoreCase(msgType)) {
                                        playEmergencyRingtoneSound();
                                    } else {
                                        if (patientUid.equals(selectedPatientUid) && layoutCaregiverChatWindow != null && layoutCaregiverChatWindow.getVisibility() == View.VISIBLE) {
                                            CaregiverSoundManager.stopNotificationSound(AdminDashboardActivity.this);
                                            markMessagesAsRead(patientUid);
                                        } else {
                                            playChatNotificationSound3Times();
                                        }
                                    }
                                }
                            }
                        }
                    }

                    for (UserRecord ur : connectedPatientsList) {
                        if (patientUid.equals(ur.getId())) {
                            if (ur.getData() != null) {
                                ur.getData().put("lastMessage", lastMessage);
                                ur.getData().put("lastMessageTime", lastTime);
                                ur.getData().put("unreadCount", unreadCount);
                            }
                            break;
                        }
                    }
                    if (patientListAdapter != null) {
                        patientListAdapter.updatePatientChatInfo(patientUid, lastMessage, lastTime, unreadCount);
                    }
                });
                chatInfoListeners.put(patientUid, infoListener);
            }

            // Watcher B: Online status listener
            if (!onlineStatusListeners.containsKey(patientUid)) {
                ListenerRegistration onlineListener = db.collection("users")
                        .document(patientUid)
                        .addSnapshotListener((doc, error) -> {
                            if (error != null || doc == null || !doc.exists()) {
                                return;
                            }
                            Boolean isOnline = doc.getBoolean("online");
                            for (UserRecord ur : connectedPatientsList) {
                                if (patientUid.equals(ur.getId())) {
                                    if (ur.getData() != null) {
                                        ur.getData().put("online", isOnline != null && isOnline);
                                    }
                                    break;
                                }
                            }
                            if (patientListAdapter != null) {
                                patientListAdapter.updatePatientOnlineStatus(patientUid, isOnline != null && isOnline);
                            }
                        });
                onlineStatusListeners.put(patientUid, onlineListener);
            }
        }
    }

    private void filterPatientsList(String query) {
        if (patientListAdapter == null) return;
        if (query == null || query.trim().isEmpty()) {
            patientListAdapter.setPatients(connectedPatientsList);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            List<UserRecord> filtered = new ArrayList<>();
            for (UserRecord p : connectedPatientsList) {
                String name = p.getString("name");
                if (name != null && name.toLowerCase().contains(lowerQuery)) {
                    filtered.add(p);
                }
            }
            patientListAdapter.setPatients(filtered);
        }
    }

    private void deselectActiveChat() {
        if (isCaregiverAudioRecording) {
            cancelCaregiverVoiceRecording();
        }
        CaregiverChatAdapter.stopAudioPlayback();
        selectedPatientUid = null;
        selectedPatientName = null;
        selectedPatientRole = null;
        selectedPatientEmail = null;
        selectedPatientLanguage = null;
        if (patientListAdapter != null) {
            patientListAdapter.setSelectedPatientUid(null);
        }
        if (caregiverChatListener != null) {
            caregiverChatListener.remove();
            caregiverChatListener = null;
        }
        if (activeChatUserPresenceListener != null) {
            activeChatUserPresenceListener.remove();
            activeChatUserPresenceListener = null;
        }
        if (layoutCaregiverChatWindow != null) {
            layoutCaregiverChatWindow.setVisibility(View.GONE);
        }
        if (layoutCaregiverChatEmptyState != null) {
            layoutCaregiverChatEmptyState.setVisibility(View.VISIBLE);
        }
    }

    private void initiateCaregiverVoiceCall(String targetUid, String targetName, String targetRole) {
        if (targetUid == null || targetUid.isEmpty()) {
            Toast.makeText(this, "Cannot initiate call: User ID unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 102);
            return;
        }

        if (VoiceCallManager.getInstance(this).isCallActive()) {
            Toast.makeText(this, "A voice call is already in progress", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isCaregiverAudioRecording) {
            cancelCaregiverVoiceRecording();
        }
        CaregiverChatAdapter.stopAudioPlayback();
        CaregiverSoundManager.stopNotificationSound(this);
        stopEmergencyRingtoneSound();

        String callTargetName = targetName != null && !targetName.isEmpty() ? targetName : "User";
        String callTargetRole = targetRole != null && !targetRole.isEmpty() ? targetRole : RoleManager.ROLE_BLIND_USER;

        VoiceCallManager.getInstance(this).startCall(
                currentUid,
                currentName != null && !currentName.isEmpty() ? currentName : "Caregiver",
                RoleManager.ROLE_ADMIN_CAREGIVER,
                targetUid,
                callTargetName,
                callTargetRole,
                null
        );

        Intent intent = new Intent(this, VoiceCallActivity.class);
        intent.putExtra("calleeUid", targetUid);
        intent.putExtra("calleeName", callTargetName);
        intent.putExtra("calleeRole", callTargetRole);
        intent.putExtra("remoteName", callTargetName);
        startActivity(intent);
    }

    private void selectPatientForChat(UserRecord patient) {
        if (isCaregiverAudioRecording) {
            cancelCaregiverVoiceRecording();
        }
        CaregiverChatAdapter.stopAudioPlayback();
        CaregiverSoundManager.stopNotificationSound(this);
        selectedPatientUid = patient.getId();
        selectedPatientName = patient.getString("name");
        selectedPatientRole = patient.getString("role");
        selectedPatientEmail = patient.getString("email");
        selectedPatientLanguage = patient.getString("language");
        String name = selectedPatientName;

        txtChatHeaderName.setText(name != null ? name : "Patient");
        if (editCaregiverChatMessage != null) {
            editCaregiverChatMessage.setText(""); // Clear previous typed text
        }

        // Reset user presence watcher
        if (activeChatUserPresenceListener != null) {
            activeChatUserPresenceListener.remove();
            activeChatUserPresenceListener = null;
        }

        if (selectedPatientUid != null) {
            activeChatUserPresenceListener = db.collection("users").document(selectedPatientUid)
                    .addSnapshotListener((doc, err) -> {
                        if (err != null || doc == null || !doc.exists()) {
                            return;
                        }
                        Boolean online = doc.getBoolean("online");
                        Timestamp lastSeen = doc.getTimestamp("lastSeen");
                        String typingTo = doc.getString("typingTo");

                        if (txtChatHeaderStatus != null) {
                            if (online != null && online) {
                                if (currentUid.equals(typingTo)) {
                                    txtChatHeaderStatus.setText("Typing...");
                                    txtChatHeaderStatus.setTextColor(Color.parseColor("#10B981"));
                                } else {
                                    txtChatHeaderStatus.setText("Online");
                                    txtChatHeaderStatus.setTextColor(Color.parseColor("#10B981"));
                                }
                            } else {
                                txtChatHeaderStatus.setTextColor(Color.parseColor("#64748B"));
                                if (lastSeen != null) {
                                    txtChatHeaderStatus.setText("Last seen " + formatLastSeenTime(lastSeen));
                                } else {
                                    txtChatHeaderStatus.setText("Offline");
                                }
                            }
                        }
                    });
        }

        layoutCaregiverChatEmptyState.setVisibility(View.GONE);
        layoutCaregiverChatWindow.setVisibility(View.VISIBLE);

        if (patientListAdapter != null) {
            patientListAdapter.setSelectedPatientUid(selectedPatientUid);
        }

        if (layoutEmojiTray != null) {
            layoutEmojiTray.setVisibility(View.GONE);
        }

        CaregiverSoundManager.stopNotificationSound(this);
        markMessagesAsRead(selectedPatientUid);

        startCaregiverChatListener(selectedPatientUid);
    }

    private void startCaregiverChatListener(String patientUid) {
        if (caregiverChatListener != null) {
            caregiverChatListener.remove();
            caregiverChatListener = null;
        }

        if (patientUid == null || patientUid.isEmpty()) {
            return;
        }

        String chatId;
        if (currentUid.compareTo(patientUid) < 0) {
            chatId = currentUid + "_" + patientUid;
        } else {
            chatId = patientUid + "_" + currentUid;
        }

        Log.d("BlindVoiceMessage", "CAREGIVER_CHAT_LISTENER_STARTED: chatId = " + chatId);
        Query chatQuery = db.collection("caregiver_messages")
                .whereEqualTo("chatId", chatId);

        caregiverChatListener = chatQuery.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("BlindVoiceMessage", "Caregiver chat listener failed: " + error.getMessage(), error);
                loadCaregiverLocalMessagesFallback(patientUid);
                return;
            }

            if (value != null) {
                LocalConnectionSimulator.clearLocalMessages(AdminDashboardActivity.this, chatId);
                List<DocumentSnapshot> docs = value.getDocuments();
                Log.d("BlindVoiceMessage", "VOICE_MESSAGE_RECEIVED: count = " + docs.size());

                List<ChatMessage> chatMsgs = new ArrayList<>();
                for (DocumentSnapshot doc : docs) {
                    chatMsgs.add(new ChatMessage(doc));
                }
                
                // Merge local offline fallback messages
                List<Map<String, Object>> locals = LocalConnectionSimulator.getLocalMessages(AdminDashboardActivity.this, chatId);
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

                // Sort messages chronologically by timestamp in Java memory
                java.util.Collections.sort(chatMsgs, (m1, m2) -> {
                    if (m1.getTimestamp() == null && m2.getTimestamp() == null) return 0;
                    if (m1.getTimestamp() == null) return 1;
                    if (m2.getTimestamp() == null) return -1;
                    return m1.getTimestamp().compareTo(m2.getTimestamp());
                });

                Log.d("BlindVoiceMessage", "VOICE_MESSAGE_ADDED_TO_ADAPTER: total = " + chatMsgs.size());

                // Scan for unacknowledged emergency alerts
                View cardPinnedEmergency = findViewById(R.id.cardPinnedEmergency);
                TextView txtPinnedEmergencyMsg = findViewById(R.id.txtPinnedEmergencyMsg);
                View btnAcknowledgeEmergency = findViewById(R.id.btnAcknowledgeEmergency);
                if (cardPinnedEmergency != null && txtPinnedEmergencyMsg != null) {
                    DocumentSnapshot activeEmergencyDoc = null;
                    for (DocumentSnapshot doc : docs) {
                        String type = doc.getString("type");
                        Boolean acknowledged = doc.getBoolean("acknowledged");
                        if ("emergency".equalsIgnoreCase(type) && (acknowledged == null || !acknowledged)) {
                            activeEmergencyDoc = doc;
                        }
                    }

                    if (activeEmergencyDoc != null) {
                        final DocumentSnapshot emergencyDoc = activeEmergencyDoc;
                        txtPinnedEmergencyMsg.setText(emergencyDoc.getString("message"));
                        cardPinnedEmergency.setVisibility(View.VISIBLE);

                        if (btnAcknowledgeEmergency != null) {
                            btnAcknowledgeEmergency.setOnClickListener(v -> {
                                stopEmergencyRingtoneSound();
                                Map<String, Object> ackUpdates = new HashMap<>();
                                ackUpdates.put("acknowledged", true);
                                ackUpdates.put("status", "read");
                                ackUpdates.put("seen", true);
                                ackUpdates.put("readStatus", true);
                                ackUpdates.put("acknowledgedAt", com.google.firebase.Timestamp.now());
                                emergencyDoc.getReference().update(ackUpdates)
                                    .addOnSuccessListener(aVoid -> {
                                        cardPinnedEmergency.setVisibility(View.GONE);
                                    });
                            });
                        }
                    } else {
                        cardPinnedEmergency.setVisibility(View.GONE);
                    }
                }

                if (caregiverChatAdapter != null) {
                    caregiverChatAdapter.setMessages(chatMsgs);
                }
                if (!chatMsgs.isEmpty() && rvCaregiverChatMessages != null) {
                    rvCaregiverChatMessages.scrollToPosition(chatMsgs.size() - 1);
                }
                markMessagesAsRead(patientUid);
            }
        });
    }

    private void loadCaregiverLocalMessagesFallback(String patientUid) {
        if (patientUid == null) return;
        String chatId = currentUid.compareTo(patientUid) < 0 ? currentUid + "_" + patientUid : patientUid + "_" + currentUid;
        List<Map<String, Object>> locals = LocalConnectionSimulator.getLocalMessages(this, chatId);
        List<ChatMessage> chatMsgs = new ArrayList<>();
        for (Map<String, Object> local : locals) {
            chatMsgs.add(new ChatMessage(local));
        }
        if (caregiverChatAdapter != null) {
            caregiverChatAdapter.setMessages(chatMsgs);
        }
        if (!chatMsgs.isEmpty() && rvCaregiverChatMessages != null) {
            rvCaregiverChatMessages.scrollToPosition(chatMsgs.size() - 1);
        }
    }

    private void markMessagesAsRead(String patientUid) {
        CaregiverSoundManager.stopNotificationSound(this);
        if (patientUid == null || db == null) return;
        String chatId = currentUid.compareTo(patientUid) < 0 ? currentUid + "_" + patientUid : patientUid + "_" + currentUid;
        
        db.collection("caregiver_messages")
                .whereEqualTo("chatId", chatId)
                .whereEqualTo("senderId", patientUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null) return;
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String status = doc.getString("status");
                        if (status != null && !status.equals("read")) {
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("status", "read");
                            updates.put("readStatus", true);
                            doc.getReference().update(updates);
                        }
                    }
                });
    }

    private void sendCaregiverChatMessage() {
        final String msg = editCaregiverChatMessage.getText().toString().trim();
        if (msg.isEmpty()) {
            return;
        }

        if (selectedPatientUid == null || selectedPatientUid.isEmpty()) {
            return;
        }

        editCaregiverChatMessage.setText("");
        if (layoutEmojiTray != null) {
            layoutEmojiTray.setVisibility(View.GONE);
        }

        String chatId;
        if (currentUid.compareTo(selectedPatientUid) < 0) {
            chatId = currentUid + "_" + selectedPatientUid;
        } else {
            chatId = selectedPatientUid + "_" + currentUid;
        }

        String lang = LanguageManager.detectLanguageFromText(msg, "en");

        boolean isVoice = isCaregiverVoiceInputMessage;
        String msgType = isVoice ? "voice" : "text";
        if (isVoice) {
            isCaregiverVoiceInputMessage = false;
        }

        Map<String, Object> chatMsgMap = new HashMap<>();
        chatMsgMap.put("chatId", chatId);
        chatMsgMap.put("senderId", currentUid);
        chatMsgMap.put("senderUid", currentUid);
        chatMsgMap.put("receiverId", selectedPatientUid);
        chatMsgMap.put("recipientEmail", selectedPatientEmail != null ? selectedPatientEmail.toLowerCase().trim() : "");
        chatMsgMap.put("senderRole", "Admin/Caregiver");
        chatMsgMap.put("receiverRole", selectedPatientRole != null ? selectedPatientRole : "Speech-Impaired User");
        chatMsgMap.put("message", msg);
        chatMsgMap.put("messageText", msg);
        chatMsgMap.put("language", lang);
        chatMsgMap.put("messageType", msgType);
        chatMsgMap.put("type", msgType);
        chatMsgMap.put("isVoice", isVoice);
        chatMsgMap.put("status", "sending");
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
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send message to caregiver chat thread", e);
                    // Save locally as offline fallback
                    LocalConnectionSimulator.saveLocalMessage(AdminDashboardActivity.this, chatId, chatMsgMap);
                    loadCaregiverLocalMessagesFallback(selectedPatientUid);
                    Toast.makeText(this, "Saved locally (Offline)", Toast.LENGTH_SHORT).show();
                });
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
                    CaregiverSoundManager.saveNotificationVolumePercent(AdminDashboardActivity.this, currentUid, progress);
                    txtActiveRingtoneDialogName.setText(CaregiverSoundManager.getActiveRingtoneName(AdminDashboardActivity.this, currentUid));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                CaregiverSoundManager.saveNotificationVolumePercent(AdminDashboardActivity.this, currentUid, seekBar.getProgress());
                txtActiveRingtoneDialogName.setText(CaregiverSoundManager.getActiveRingtoneName(AdminDashboardActivity.this, currentUid));
                CaregiverSoundManager.testPlaySound(AdminDashboardActivity.this, currentUid);
            }
        });

        // 1. Choose from File Manager / Gallery
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
                    Toast.makeText(AdminDashboardActivity.this, "File Manager not found on device", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 2. Test Play Sound
        btnTestPlayRingtone.setOnClickListener(v -> {
            CaregiverSoundManager.testPlaySound(AdminDashboardActivity.this, currentUid);
        });

        // 3. Clear Ringtone (Silent) / Sound Toggle
        btnResetDefaultRingtone.setOnClickListener(v -> {
            CaregiverSoundManager.toggleSilentMode(AdminDashboardActivity.this, currentUid);
            boolean nowSilent = CaregiverSoundManager.isSilentMode(AdminDashboardActivity.this, currentUid);
            if (nowSilent) {
                btnResetDefaultRingtone.setText("🔔 Enable Sound");
                Toast.makeText(AdminDashboardActivity.this, "🔇 Silent Mode ON (Sound OFF, Vibration ON)", Toast.LENGTH_SHORT).show();
            } else {
                btnResetDefaultRingtone.setText("🔇 Clear (Silent)");
                Toast.makeText(AdminDashboardActivity.this, "🔔 Notification Sound Enabled", Toast.LENGTH_SHORT).show();
                CaregiverSoundManager.testPlaySound(AdminDashboardActivity.this, currentUid);
            }
            txtActiveRingtoneDialogName.setText(CaregiverSoundManager.getActiveRingtoneName(AdminDashboardActivity.this, currentUid));
        });

        // 4. Save & Close
        btnSaveNotificationSettings.setOnClickListener(v -> {
            CaregiverSoundManager.saveNotificationVolumePercent(AdminDashboardActivity.this, currentUid, sbNotificationVolume.getProgress());
            CaregiverSoundManager.stopNotificationSound(AdminDashboardActivity.this);
            Toast.makeText(AdminDashboardActivity.this, "Notification sound & volume saved successfully!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.setOnDismissListener(d -> CaregiverSoundManager.stopNotificationSound(AdminDashboardActivity.this));
        dialog.show();
    }

    private void playChatNotificationSound3Times() {
        CaregiverSoundManager.playNotificationSound3Times(this);
    }

    private void playEmergencyRingtoneSound() {
        CaregiverSoundManager.playEmergencyAlertSoundAndVibration(this);
    }

    private void stopEmergencyRingtoneSound() {
        CaregiverSoundManager.stopEmergencySound(this);
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

    private void toggleCaregiverVoiceInput() {
        if (isCaregiverRecording) {
            AccessibleMicFeedbackManager.triggerShortVibration(this);
            stopCaregiverVoiceInput();
        } else {
            String langCode = selectedPatientLanguage;
            if (android.text.TextUtils.isEmpty(langCode)) {
                langCode = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
            }
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                String targetLang = langCode;
                AccessibleMicFeedbackManager.onPermissionDenied(this, tts, isTtsInitialized, targetLang, () -> {
                    ActivityCompat.requestPermissions(AdminDashboardActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 301);
                });
            } else {
                AccessibleMicFeedbackManager.startAccessibleMicFlow(this, tts, isTtsInitialized, langCode, this::startCaregiverVoiceInput);
            }
        }
    }

    private void startCaregiverVoiceInput() {
        isCaregiverRecording = true;
        if (caregiverSpeechRecognizer != null) {
            try {
                caregiverSpeechRecognizer.destroy();
            } catch (Exception ignored) {}
            caregiverSpeechRecognizer = null;
        }

        try {
            caregiverSpeechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create SpeechRecognizer", e);
            AccessibleMicFeedbackManager.resetSessionState();
            isCaregiverRecording = false;
            return;
        }

        caregiverSpeechIntent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        caregiverSpeechIntent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        String langCode = selectedPatientLanguage;
        if (android.text.TextUtils.isEmpty(langCode)) {
            langCode = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        }
        String speechTag = LanguageManager.getSpeechLanguageTag(langCode);

        caregiverSpeechIntent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, speechTag);
        caregiverSpeechIntent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, speechTag);
        caregiverSpeechIntent.putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        caregiverSpeechIntent.putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        caregiverSpeechRecognizer.setRecognitionListener(new android.speech.RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                runOnUiThread(() -> {
                    Toast.makeText(AdminDashboardActivity.this, "🎤 Listening caregiver voice... Speak now", Toast.LENGTH_SHORT).show();
                    if (btnCaregiverChatMic != null) btnCaregiverChatMic.setSelected(true);
                });
            }

            @Override
            public void onBeginningOfSpeech() {}
            @Override
            public void onRmsChanged(float rmsdB) {}
            @Override
            public void onBufferReceived(byte[] buffer) {}
            @Override
            public void onEndOfSpeech() {
                runOnUiThread(() -> {
                    if (btnCaregiverChatMic != null) btnCaregiverChatMic.setSelected(false);
                });
            }

            @Override
            public void onError(int error) {
                runOnUiThread(() -> {
                    isCaregiverRecording = false;
                    if (btnCaregiverChatMic != null) btnCaregiverChatMic.setSelected(false);
                    final String activeLang = (!android.text.TextUtils.isEmpty(selectedPatientLanguage)) ? 
                        selectedPatientLanguage : (sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE);
                    AccessibleMicFeedbackManager.onSttErrorReceived(
                        AdminDashboardActivity.this,
                        tts,
                        isTtsInitialized,
                        activeLang,
                        error,
                        () -> {
                            Log.w(TAG, "Caregiver SpeechRecognizer error code: " + error + ". Launching fallback...");
                            launchCaregiverVoiceInputFallback(LanguageManager.getSpeechLanguageTag(activeLang), 502);
                        }
                    );
                });
            }

            @Override
            public void onResults(Bundle results) {
                runOnUiThread(() -> {
                    isCaregiverRecording = false;
                    if (btnCaregiverChatMic != null) btnCaregiverChatMic.setSelected(false);
                    String activeLang = selectedPatientLanguage;
                    if (android.text.TextUtils.isEmpty(activeLang)) {
                        activeLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
                    }
                    if (results != null) {
                        ArrayList<String> matches = results.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                        if (matches != null && !matches.isEmpty()) {
                            String spokenText = matches.get(0);
                            isCaregiverVoiceInputMessage = true;
                            AccessibleMicFeedbackManager.onSttResultReceived(
                                AdminDashboardActivity.this,
                                tts,
                                isTtsInitialized,
                                activeLang,
                                spokenText,
                                () -> {
                                    if (editCaregiverChatMessage != null) {
                                        editCaregiverChatMessage.setText(spokenText);
                                        editCaregiverChatMessage.setSelection(spokenText.length());
                                    }
                                }
                            );
                        } else {
                            AccessibleMicFeedbackManager.onSttErrorReceived(
                                AdminDashboardActivity.this,
                                tts,
                                isTtsInitialized,
                                activeLang,
                                android.speech.SpeechRecognizer.ERROR_NO_MATCH,
                                null
                            );
                        }
                    }
                });
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                if (partialResults != null) {
                    ArrayList<String> matches = partialResults.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String partialText = matches.get(0);
                        runOnUiThread(() -> {
                            if (editCaregiverChatMessage != null && !android.text.TextUtils.isEmpty(partialText)) {
                                editCaregiverChatMessage.setText(partialText);
                                editCaregiverChatMessage.setSelection(partialText.length());
                            }
                        });
                    }
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        caregiverSpeechRecognizer.startListening(caregiverSpeechIntent);
    }

    private void launchCaregiverVoiceInputFallback(String speechTag, int requestCode) {
        try {
            Intent intent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, speechTag);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, speechTag);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "🎤 Speak caregiver message clearly");
            startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            Toast.makeText(this, "Voice input not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 502 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                String spokenText = matches.get(0);
                isCaregiverVoiceInputMessage = true;
                if (editCaregiverChatMessage != null) {
                    editCaregiverChatMessage.setText(spokenText);
                    editCaregiverChatMessage.setSelection(spokenText.length());
                }
            }
        } else if (requestCode == REQUEST_CODE_PICK_RINGTONE && resultCode == RESULT_OK && data != null) {
            Uri selectedAudioUri = data.getData();
            if (selectedAudioUri != null) {
                boolean success = CaregiverSoundManager.saveCustomRingtone(this, selectedAudioUri);
                if (success) {
                    Toast.makeText(this, "🔔 Custom Notification Ringtone saved from File Manager!", Toast.LENGTH_LONG).show();
                    if (txtActiveRingtoneDialogName != null) {
                        txtActiveRingtoneDialogName.setText(CaregiverSoundManager.getActiveRingtoneName(this));
                    }
                } else {
                    Toast.makeText(this, "Failed to load audio file from File Manager", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void stopCaregiverVoiceInput() {
        isCaregiverRecording = false;
        if (btnCaregiverChatMic != null) btnCaregiverChatMic.setSelected(false);
        if (caregiverSpeechRecognizer != null) {
            caregiverSpeechRecognizer.stopListening();
        }
    }

    private void toggleCaregiverVoiceRecording() {
        if (isCaregiverAudioRecording) {
            stopAndSendCaregiverVoiceRecording();
        } else {
            if (selectedPatientUid == null || selectedPatientUid.isEmpty()) {
                Toast.makeText(this, "Please select a patient first to send a voice message", Toast.LENGTH_SHORT).show();
                return;
            }
            if (VoiceCallManager.getInstance(this).isCallActive()) {
                Toast.makeText(this, "Cannot record voice message during an active call", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 301);
                return;
            }
            startCaregiverVoiceRecording();
        }
    }

    private void startCaregiverVoiceRecording() {
        CaregiverChatAdapter.stopAudioPlayback();
        try {
            caregiverTempAudioFile = new File(getExternalCacheDir(), "caregiver_voice_" + System.currentTimeMillis() + ".m4a");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                caregiverMediaRecorder = new MediaRecorder(this);
            } else {
                caregiverMediaRecorder = new MediaRecorder();
            }
            caregiverMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            caregiverMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            caregiverMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            caregiverMediaRecorder.setOutputFile(caregiverTempAudioFile.getAbsolutePath());
            caregiverMediaRecorder.prepare();
            caregiverMediaRecorder.start();

            isCaregiverAudioRecording = true;
            caregiverRecordingStartTime = System.currentTimeMillis();

            if (btnCaregiverChatMic != null) {
                btnCaregiverChatMic.setSelected(true);
                if (btnCaregiverChatMic instanceof MaterialButton) {
                    ((MaterialButton) btnCaregiverChatMic).setIconTint(ColorStateList.valueOf(Color.parseColor("#EF4444")));
                }
            }
            if (editCaregiverChatMessage != null) {
                editCaregiverChatMessage.setHint("🔴 Recording voice message... Tap 🎤 to send");
            }
            Toast.makeText(this, "🔴 Recording voice message... Tap 🎤 again to finish & send", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start caregiver voice recording: " + e.getMessage(), e);
            isCaregiverAudioRecording = false;
            Toast.makeText(this, "Failed to start recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            resetCaregiverMicUI();
        }
    }

    private void stopAndSendCaregiverVoiceRecording() {
        if (!isCaregiverAudioRecording) return;
        isCaregiverAudioRecording = false;

        try {
            if (caregiverMediaRecorder != null) {
                caregiverMediaRecorder.stop();
                caregiverMediaRecorder.release();
                caregiverMediaRecorder = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping caregiver media recorder: " + e.getMessage(), e);
        }

        resetCaregiverMicUI();

        if (caregiverTempAudioFile == null || !caregiverTempAudioFile.exists() || caregiverTempAudioFile.length() <= 100) {
            Toast.makeText(this, "Recording discarded (too short or empty)", Toast.LENGTH_SHORT).show();
            cancelCaregiverVoiceRecording();
            return;
        }

        long durationSeconds = Math.max(1, (System.currentTimeMillis() - caregiverRecordingStartTime) / 1000);
        Toast.makeText(this, "📤 Sending voice message...", Toast.LENGTH_SHORT).show();
        uploadCaregiverVoiceMessage(caregiverTempAudioFile, (int) durationSeconds, selectedPatientUid, selectedPatientRole);
    }

    private void cancelCaregiverVoiceRecording() {
        if (isCaregiverAudioRecording) {
            isCaregiverAudioRecording = false;
            try {
                if (caregiverMediaRecorder != null) {
                    caregiverMediaRecorder.stop();
                    caregiverMediaRecorder.release();
                    caregiverMediaRecorder = null;
                }
            } catch (Exception ignored) {}
        }
        if (caregiverTempAudioFile != null && caregiverTempAudioFile.exists()) {
            caregiverTempAudioFile.delete();
            caregiverTempAudioFile = null;
        }
        resetCaregiverMicUI();
    }

    private void resetCaregiverMicUI() {
        if (btnCaregiverChatMic != null) {
            btnCaregiverChatMic.setSelected(false);
            if (btnCaregiverChatMic instanceof MaterialButton) {
                ((MaterialButton) btnCaregiverChatMic).setIconTint(ColorStateList.valueOf(getResources().getColor(R.color.primary)));
            }
        }
        if (editCaregiverChatMessage != null) {
            editCaregiverChatMessage.setHint("Type a message...");
        }
    }

    private void uploadCaregiverVoiceMessage(File audioFile, int durationSeconds, String targetUid, String targetRole) {
        if (audioFile == null || !audioFile.exists()) return;
        if (targetUid == null || targetUid.isEmpty()) return;

        String storagePath = "voice_messages/" + currentUid + "/" + System.currentTimeMillis() + ".m4a";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(storagePath);

        storageRef.putFile(Uri.fromFile(audioFile))
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        String downloadUrl = downloadUri.toString();
                        saveCaregiverVoiceMessageToFirestore(downloadUrl, durationSeconds, targetUid, targetRole, audioFile);
                    }).addOnFailureListener(e -> {
                        uploadCaregiverViaDirectStorage(audioFile, durationSeconds, storagePath, targetUid, targetRole);
                    });
                })
                .addOnFailureListener(e -> {
                    uploadCaregiverViaDirectStorage(audioFile, durationSeconds, storagePath, targetUid, targetRole);
                });
    }

    private void uploadCaregiverViaDirectStorage(File audioFile, int durationSeconds, String storagePath, String targetUid, String targetRole) {
        caregiverNetworkExecutor.execute(() -> {
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
                    runOnUiThread(() -> saveCaregiverVoiceMessageToFirestore(downloadUrl, durationSeconds, targetUid, targetRole, audioFile));
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(AdminDashboardActivity.this, "Unable to send voice message", Toast.LENGTH_SHORT).show();
                        if (audioFile != null && audioFile.exists()) audioFile.delete();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(AdminDashboardActivity.this, "Unable to send voice message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (audioFile != null && audioFile.exists()) audioFile.delete();
                });
            }
        });
    }

    private void saveCaregiverVoiceMessageToFirestore(String downloadUrl, int durationSeconds, String targetUid, String targetRole, File localFile) {
        String chatId;
        if (currentUid.compareTo(targetUid) < 0) {
            chatId = currentUid + "_" + targetUid;
        } else {
            chatId = targetUid + "_" + currentUid;
        }

        Map<String, Object> msg = new HashMap<>();
        msg.put("chatId", chatId);
        msg.put("senderId", currentUid);
        msg.put("senderUid", currentUid);
        msg.put("receiverId", targetUid);
        msg.put("senderRole", RoleManager.ROLE_ADMIN_CAREGIVER);
        msg.put("receiverRole", targetRole != null ? targetRole : RoleManager.ROLE_BLIND_USER);
        msg.put("message", "🎤 Voice Message");
        msg.put("messageText", "🎤 Voice Message");
        msg.put("language", LanguageManager.DEFAULT_LANGUAGE);
        msg.put("messageType", "voice");
        msg.put("type", "voice");
        msg.put("isVoice", true);
        msg.put("audioUrl", downloadUrl);
        msg.put("audioDuration", durationSeconds);
        msg.put("status", "sent");
        msg.put("delivered", false);
        msg.put("seen", false);
        msg.put("timestamp", FieldValue.serverTimestamp());

        db.collection("caregiver_messages")
                .add(msg)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AdminDashboardActivity.this, "✓ Voice message sent", Toast.LENGTH_SHORT).show();
                    if (localFile != null && localFile.exists()) {
                        localFile.delete();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminDashboardActivity.this, "Failed to deliver voice message", Toast.LENGTH_SHORT).show();
                    if (localFile != null && localFile.exists()) {
                        localFile.delete();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 102) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (selectedPatientUid != null && !selectedPatientUid.isEmpty()) {
                    initiateCaregiverVoiceCall(selectedPatientUid, selectedPatientName, selectedPatientRole);
                }
            } else {
                Toast.makeText(this, "Microphone permission is required to make voice calls", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 301) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCaregiverVoiceRecording();
            } else {
                Toast.makeText(this, "Microphone permission is required to record voice messages", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
