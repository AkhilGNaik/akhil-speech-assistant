package com.kannada.speechassistant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;
import android.os.Vibrator;
import android.os.Build;
import android.os.VibrationEffect;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.RingtoneManager;
import android.media.Ringtone;
import android.net.Uri;
import android.content.res.ColorStateList;
import android.graphics.Color;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kannada.speechassistant.voiceassistant.AppVoiceAssistant;
import com.kannada.speechassistant.voiceassistant.WakeWordManager;

/**
 * ChatActivity manages real-time messaging between a User and a Caregiver/Admin.
 * Implements real-time listeners, instant sync, typing status, and read receipts.
 */
public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase));
    }

    // Services
    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private ListenerRegistration messagesListener;
    private ListenerRegistration presenceListener;

    // TTS Support
    private TextToSpeech tts;
    private boolean isTtsInitialized = false;

    // UI Widgets
    private TextView txtReceiverName, txtReceiverRole;
    private MaterialButton btnBack, btnSend, btnSendEmergency, btnCall;
    private EditText editMessage;
    private RecyclerView rvMessages;
    private LinearLayout layoutQuickPhrases;

    // Adapters & State
    private ChatAdapter chatAdapter;
    private String currentUserId;
    private String currentUserName;
    private String receiverId;
    private String receiverName;
    private String receiverRole;
    private String receiverEmail;
    private String chatId;

    // Quick phrases list
    private final String[] quickPhrases = {
            "I need water", "I need food", "Help me sit up",
            "Please come here", "I feel sick", "Yes", "No", "Thank you"
    };

    private final Handler typingHandler = new Handler(Looper.getMainLooper());
    private boolean isTyping = false;
    private final Runnable typingTimeoutRunnable = () -> {
        isTyping = false;
        updateTypingStatus(false);
    };

    // Transliteration & Voice Input Support for ChatActivity
    private View layoutHomeTranslitStatus;
    private android.widget.ProgressBar progressHomeTranslit;
    private TextView txtHomeTranslitStatus;
    private boolean isTranslitInternalChange = false;
    private String activeTranslitLanguage = null;
    private java.util.concurrent.ExecutorService networkExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

    // Voice Input Speech Recognizer
    private View btnMicInput;
    private android.speech.SpeechRecognizer speechRecognizer;
    private android.content.Intent speechRecognizerIntent;
    private boolean isRecording = false;
    private boolean isVoiceInputMessage = false;
    private final java.util.Set<String> autoPlayedVoiceMsgIds = new java.util.HashSet<>();

    // Voice Message Recording
    private MediaRecorder chatMediaRecorder;
    private File chatTempAudioFile;
    private boolean isChatAudioRecording = false;
    private long chatRecordingStartTime = 0;

    // Blind User Voice Message Input (Image 5 Function - Only Large Microphone 🎤)
    private View layoutStandardChatInput;
    private View layoutBlindVoiceMessageInput;
    private MaterialButton btnBlindMicVoiceMessage;
    private TextView txtBlindMicStatus;
    private View layoutBlindRecordingTimer;
    private TextView txtBlindTimerDisplay;
    private View layoutBlindRecordingActions;
    private MaterialButton btnBlindStopRecording;
    private MaterialButton btnBlindCancelRecording;
    private boolean isBlindRecording = false;
    private int blindRecordingSeconds = 0;
    private final Handler blindTimerHandler = new Handler(Looper.getMainLooper());
    private Runnable blindTimerRunnable;
    private MediaRecorder blindMediaRecorder;
    private File blindTempAudioFile;
    private boolean isBlindUserMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Initialize TTS for Physically Disabled, Speech Impaired, Mute, and Blind Users
        String myRole = sessionManager.getUserRole();
        if (RoleManager.ROLE_PHYSICALLY_DISABLED.equals(myRole) || RoleManager.ROLE_SPEECH_IMPAIRED.equals(myRole) || RoleManager.ROLE_MUTE_USER.equals(myRole) || RoleManager.ROLE_BLIND_USER.equals(myRole)) {
            initGoogleTextToSpeech();
        }

        // Security check
        if (!sessionManager.isLoggedIn()) {
            RoleManager.redirectToLogin(this);
            finish();
            return;
        }

        // Get Current User info
        HashMap<String, String> userDetails = sessionManager.getUserDetails();
        currentUserId = userDetails.get(SessionManager.KEY_USER_UID);
        currentUserName = userDetails.get(SessionManager.KEY_USER_EMAIL); // Fallback to email

        // Fetch user profile name and language from Firestore
        db.collection("users").document(currentUserId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                if (!TextUtils.isEmpty(name)) {
                    currentUserName = name;
                }
                String userLang = documentSnapshot.getString("language");
                if (!TextUtils.isEmpty(userLang)) {
                    activeTranslitLanguage = LanguageManager.normalizeLanguageCode(userLang);
                }
            }
        });

        // Get Receiver Details from Intent
        receiverId = getIntent().getStringExtra("receiverId");
        receiverName = getIntent().getStringExtra("receiverName");
        receiverRole = getIntent().getStringExtra("receiverRole");
        receiverEmail = getIntent().getStringExtra("receiverEmail");
        String rxLang = getIntent().getStringExtra("receiverLanguage");
        if (!TextUtils.isEmpty(rxLang)) {
            activeTranslitLanguage = LanguageManager.normalizeLanguageCode(rxLang);
        }

        if (TextUtils.isEmpty(receiverId)) {
            Toast.makeText(this, "Error: User connection missing receiver reference", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Fetch receiver's language from Firestore for accurate transliteration
        db.collection("users").document(receiverId).get().addOnSuccessListener(doc -> {
            if (doc != null && doc.exists()) {
                String rLang = doc.getString("language");
                if (!TextUtils.isEmpty(rLang)) {
                    activeTranslitLanguage = LanguageManager.normalizeLanguageCode(rLang);
                }
            }
        });

        // Generate unique chatId deterministically
        if (currentUserId.compareTo(receiverId) < 0) {
            chatId = currentUserId + "_" + receiverId;
        } else {
            chatId = receiverId + "_" + currentUserId;
        }

        // Bind UI Elements
        txtReceiverName = findViewById(R.id.txtReceiverName);
        txtReceiverRole = findViewById(R.id.txtReceiverRole);
        btnBack = findViewById(R.id.btnBack);
        btnSend = findViewById(R.id.btnSend);
        btnSendEmergency = findViewById(R.id.btnSendEmergency);
        btnCall = findViewById(R.id.btnCall);
        btnMicInput = findViewById(R.id.btnMicInput);
        editMessage = findViewById(R.id.editMessage);
        rvMessages = findViewById(R.id.rvMessages);
        layoutQuickPhrases = findViewById(R.id.layoutQuickPhrases);

        if (btnMicInput != null) {
            btnMicInput.setOnClickListener(v -> toggleVoiceMessageRecording());
            btnMicInput.setOnLongClickListener(v -> {
                toggleVoiceInput();
                return true;
            });
        }

        myRole = sessionManager.getUserRole();
        boolean isDeaf = RoleManager.ROLE_DEAF_USER.equals(myRole)
                || RoleManager.ROLE_SPEECH_IMPAIRED.equals(myRole)
                || "Mute, Deaf & Blind User".equals(myRole)
                || "Mute, Deaf & Blind Users".equals(myRole);

        boolean isReceiverDeaf = RoleManager.ROLE_DEAF_USER.equals(receiverRole)
                || RoleManager.ROLE_SPEECH_IMPAIRED.equals(receiverRole)
                || "Mute, Deaf & Blind User".equals(receiverRole)
                || "Mute, Deaf & Blind Users".equals(receiverRole);

        if (btnCall != null) {
            btnCall.setOnClickListener(v -> {
                if (isDeaf || isReceiverDeaf) {
                    Toast.makeText(this, "Voice calls are not supported for Deaf users", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (receiverId == null || receiverId.isEmpty()) {
                    Toast.makeText(this, "Cannot initiate call: recipient user not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (VoiceCallManager.getInstance(this).isCallActive()) {
                    Toast.makeText(this, "A voice call is already in progress", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isChatAudioRecording) {
                    cancelAudioRecording();
                }
                ChatAdapter.stopAudioPlayback();
                CaregiverChatAdapter.stopAudioPlayback();
                initiateVoiceCall();
            });
        }

        // Header configuration
        txtReceiverName.setText(receiverName != null ? receiverName : "Caregiver");
        if (RoleManager.ROLE_MUTE_USER.equals(receiverRole)) {
            txtReceiverRole.setText("Mute User");
        } else if (RoleManager.ROLE_SPEECH_IMPAIRED.equals(receiverRole) || RoleManager.ROLE_DEAF_USER.equals(receiverRole)) {
            txtReceiverRole.setText("Deaf User");
        } else if (RoleManager.ROLE_BLIND_USER.equals(receiverRole)) {
            txtReceiverRole.setText("Blind User");
        } else {
            txtReceiverRole.setText(receiverRole != null ? receiverRole : "Admin/Caregiver");
        }

        // Hide SOS button for Admin/Caregiver users to avoid cluttering their screen
        if (RoleManager.ROLE_ADMIN_CAREGIVER.equals(myRole)) {
            if (btnSendEmergency != null) btnSendEmergency.setVisibility(View.GONE);
            if (btnCall != null) btnCall.setVisibility(isReceiverDeaf ? View.GONE : View.VISIBLE);
            if (layoutQuickPhrases != null) {
                // If it is inside scrollQuickPhrases, hide the scroll view too
                View parent = (View) layoutQuickPhrases.getParent();
                if (parent != null) parent.setVisibility(View.GONE);
                layoutQuickPhrases.setVisibility(View.GONE);
            }
        } else {
            if (btnSendEmergency != null) btnSendEmergency.setVisibility(View.VISIBLE);
            if (btnCall != null) btnCall.setVisibility(isDeaf ? View.GONE : View.VISIBLE);
            if (layoutQuickPhrases != null) {
                View parent = (View) layoutQuickPhrases.getParent();
                if (isDeaf) {
                    if (parent != null) parent.setVisibility(View.GONE);
                    layoutQuickPhrases.setVisibility(View.GONE);
                } else {
                    if (parent != null) parent.setVisibility(View.VISIBLE);
                    layoutQuickPhrases.setVisibility(View.VISIBLE);
                }
            }
        }

        // Bind Transliteration Status Components
        layoutHomeTranslitStatus = findViewById(R.id.layoutHomeTranslitStatus);
        progressHomeTranslit = findViewById(R.id.progressHomeTranslit);
        txtHomeTranslitStatus = findViewById(R.id.txtHomeTranslitStatus);

        // Setup Blind User Voice Message Card (Image 5 Function)
        setupBlindVoiceMessageUI();

        // Setup Chat RecyclerView
        chatAdapter = new ChatAdapter(currentUserId);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(chatAdapter);

        // Header and back listener
        btnBack.setOnClickListener(v -> finish());

        // Send standard text message
        btnSend.setOnClickListener(v -> {
            if (isChatAudioRecording) {
                stopAndSendVoiceRecording();
                return;
            }
            String text = editMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                sendMessage(text, "text");
                editMessage.setText("");
            }
        });

        // Typing status listener and transliteration TextWatcher
        editMessage.addTextChangedListener(new TextWatcher() {
            private final Handler debounceHandler = new Handler(Looper.getMainLooper());
            private Runnable debounceRunnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isTyping) {
                    isTyping = true;
                    updateTypingStatus(true);
                }
                typingHandler.removeCallbacks(typingTimeoutRunnable);
                typingHandler.postDelayed(typingTimeoutRunnable, 2000);

                if (isTranslitInternalChange) return;

                if (count > 0 && s.length() > 0) {
                    char lastChar = s.charAt(start + count - 1);
                    if (lastChar == ' ' || lastChar == '\n') {
                        triggerChatTransliteration();
                        return;
                    }
                }

                if (debounceRunnable != null) {
                    debounceHandler.removeCallbacks(debounceRunnable);
                }
                debounceRunnable = () -> triggerChatTransliteration();
                debounceHandler.postDelayed(debounceRunnable, 1200);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Quick Send SOS Emergency message
        btnSendEmergency.setOnClickListener(v -> {
            String role = sessionManager.getUserRole();
            String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
            String normLang = LanguageManager.normalizeLanguageCode(userLang);

            String defaultSos;
            if (LanguageManager.LANG_MALAYALAM.equals(normLang)) {
                defaultSos = "അടിയന്തര സഹായം വേണം!";
            } else if (LanguageManager.LANG_HINDI.equals(normLang)) {
                defaultSos = "आपातकालीन मदद चाहिए!";
            } else {
                defaultSos = "ತುರ್ತು ಪರಿಸ್ಥಿತಿ! ಸಹಾಯ ಬೇಕು";
            }

            String customSosMsg = null;
            if (RoleManager.ROLE_PHYSICALLY_DISABLED.equals(role)) {
                android.content.SharedPreferences settingsPref = getSharedPreferences("PhysicalSettings", MODE_PRIVATE);
                customSosMsg = settingsPref.getString("emergencyMessage", defaultSos);
            } else if (RoleManager.ROLE_MUTE_USER.equals(role)) {
                android.content.SharedPreferences settingsPref = getSharedPreferences("MuteUserSettings", MODE_PRIVATE);
                customSosMsg = settingsPref.getString("emergencyMessage", defaultSos);
            } else if (RoleManager.ROLE_SPEECH_IMPAIRED.equals(role)) {
                android.content.SharedPreferences settingsPref = getSharedPreferences("SpeechSettings", MODE_PRIVATE);
                customSosMsg = settingsPref.getString("emergencyMessage", defaultSos);
            } else if (RoleManager.ROLE_BLIND_USER.equals(role)) {
                android.content.SharedPreferences settingsPref = getSharedPreferences("BlindUserSettings", MODE_PRIVATE);
                customSosMsg = settingsPref.getString("emergencyMessage", defaultSos);
            } else {
                customSosMsg = defaultSos;
            }

            if (TextUtils.isEmpty(customSosMsg) || 
                (LanguageManager.LANG_MALAYALAM.equals(normLang) && (customSosMsg.contains("ತುರ್ತು") || customSosMsg.contains("ನನಗೆ"))) ||
                (LanguageManager.LANG_HINDI.equals(normLang) && (customSosMsg.contains("ತುರ್ತು") || customSosMsg.contains("ನನಗೆ"))) ||
                (LanguageManager.LANG_KANNADA.equals(normLang) && (customSosMsg.contains("അടിയന്തര") || customSosMsg.contains("ആपातकालीन")))) {
                customSosMsg = defaultSos;
            }

            sendMessage(customSosMsg, "emergency");
            if (RoleManager.ROLE_PHYSICALLY_DISABLED.equals(role)) {
                triggerEmergencySOSPhysical(customSosMsg);
            } else if (RoleManager.ROLE_SPEECH_IMPAIRED.equals(role) || RoleManager.ROLE_MUTE_USER.equals(role)) {
                triggerEmergencySOSSpeech(customSosMsg);
            } else {
                triggerEmergencySOS();
            }
        });

        // Setup Quick Phrases
        setupQuickPhrases();

        // Listen for chats
        startMessagesListener();

        // Listen for presence
        startPresenceListener();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
        if (presenceListener != null) {
            presenceListener.remove();
            presenceListener = null;
        }

        String newReceiverId = intent.getStringExtra("receiverId");
        String newReceiverName = intent.getStringExtra("receiverName");
        String newReceiverRole = intent.getStringExtra("receiverRole");
        String newReceiverEmail = intent.getStringExtra("receiverEmail");

        if (!TextUtils.isEmpty(newReceiverId)) {
            receiverId = newReceiverId;
            if (newReceiverName != null) receiverName = newReceiverName;
            if (newReceiverRole != null) receiverRole = newReceiverRole;
            if (newReceiverEmail != null) receiverEmail = newReceiverEmail;

            if (currentUserId.compareTo(receiverId) < 0) {
                chatId = currentUserId + "_" + receiverId;
            } else {
                chatId = receiverId + "_" + currentUserId;
            }

            if (txtReceiverName != null) txtReceiverName.setText(receiverName != null ? receiverName : "Caregiver");
            if (txtReceiverRole != null) txtReceiverRole.setText(receiverRole != null ? receiverRole : "Admin/Caregiver");

            startMessagesListener();
            startPresenceListener();
        }
    }

    private void updateTypingStatus(boolean typing) {
        db.collection("users").document(currentUserId)
                .update("typingTo", typing ? receiverId : null)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update typing status", e));
    }

    private void setupQuickPhrases() {
        if (layoutQuickPhrases == null) return;
        String currentRole = sessionManager != null ? sessionManager.getUserRole() : null;
        boolean isDeaf = RoleManager.ROLE_DEAF_USER.equals(currentRole)
                || RoleManager.ROLE_SPEECH_IMPAIRED.equals(currentRole)
                || "Mute, Deaf & Blind User".equals(currentRole)
                || "Mute, Deaf & Blind Users".equals(currentRole);
        if (isBlindUserMode || isDeaf) {
            View parent = (View) layoutQuickPhrases.getParent();
            if (parent != null) parent.setVisibility(View.GONE);
            layoutQuickPhrases.setVisibility(View.GONE);
            return;
        }
        layoutQuickPhrases.removeAllViews();
        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        for (String phrase : quickPhrases) {
            String localizedPhrase = LanguageManager.getQuickPhraseText(phrase, userLang);
            MaterialButton btn = new MaterialButton(this, null, com.google.android.material.R.style.Widget_MaterialComponents_Button_OutlinedButton);
            btn.setText(localizedPhrase);
            btn.setAllCaps(false);
            btn.setTextSize(12);
            btn.setCornerRadius((int) (16 * getResources().getDisplayMetrics().density));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            btn.setLayoutParams(params);
            btn.setOnClickListener(v -> sendMessage(localizedPhrase, "quick"));
            layoutQuickPhrases.addView(btn);
        }
    }

    private void startMessagesListener() {
        Log.d("BlindVoiceMessage", "CHAT_LISTENER_STARTED: chatId = " + chatId);
        Query chatQuery = db.collection("caregiver_messages")
                .whereEqualTo("chatId", chatId);

        messagesListener = chatQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e("BlindVoiceMessage", "ChatListener error: " + error.getMessage(), error);
                    loadLocalMessagesFallback();
                    return;
                }

                if (value != null) {
                    LocalConnectionSimulator.clearLocalMessages(ChatActivity.this, chatId);
                    List<DocumentSnapshot> docs = value.getDocuments();
                    Log.d("BlindVoiceMessage", "VOICE_MESSAGE_RECEIVED: count = " + docs.size());

                    List<ChatMessage> chatMsgs = new ArrayList<>();
                    for (DocumentSnapshot doc : docs) {
                        chatMsgs.add(new ChatMessage(doc));
                    }
                    
                    // Merge local offline fallback messages
                    List<Map<String, Object>> locals = LocalConnectionSimulator.getLocalMessages(ChatActivity.this, chatId);
                    for (Map<String, Object> local : locals) {
                        // Prevent duplicates
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
                    chatAdapter.setMessages(chatMsgs);
                    if (!chatMsgs.isEmpty()) {
                        rvMessages.scrollToPosition(chatMsgs.size() - 1);
                    }

                    // Auto-play incoming caregiver messages loud through speaker
                    boolean isFirstLoad = autoPlayedVoiceMsgIds.isEmpty();
                    for (ChatMessage m : chatMsgs) {
                        if (!currentUserId.equals(m.getSenderId())) {
                            if (!autoPlayedVoiceMsgIds.contains(m.getId())) {
                                autoPlayedVoiceMsgIds.add(m.getId());
                                if (!isFirstLoad) {
                                    CaregiverSoundManager.handleIncomingMessage(ChatActivity.this, m.getId(), m.getSenderId(), m.getMessage(), m.getType(), receiverName, receiverRole);
                                }
                            }
                        }
                    }

                    // Mark unread received messages as read
                    markMessagesAsRead(docs);
                }
            }
        });
    }

    private void loadLocalMessagesFallback() {
        List<Map<String, Object>> locals = LocalConnectionSimulator.getLocalMessages(this, chatId);
        List<ChatMessage> chatMsgs = new ArrayList<>();
        for (Map<String, Object> local : locals) {
            chatMsgs.add(new ChatMessage(local));
        }
        chatAdapter.setMessages(chatMsgs);
        if (!chatMsgs.isEmpty()) {
            rvMessages.scrollToPosition(chatMsgs.size() - 1);
        }
    }

    private void startPresenceListener() {
        presenceListener = db.collection("users").document(receiverId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        return;
                    }
                    
                    boolean online = snapshot.getBoolean("online") != null && snapshot.getBoolean("online");
                    String typingTo = snapshot.getString("typingTo");
                    boolean isTypingToMe = currentUserId.equals(typingTo);
                    
                    if (isTypingToMe) {
                        txtReceiverRole.setText("typing...");
                    } else if (online) {
                        txtReceiverRole.setText("online");
                    } else {
                        txtReceiverRole.setText(receiverRole != null ? receiverRole : "Offline");
                    }
                });
    }

    private void markMessagesAsRead(List<DocumentSnapshot> docs) {
        WriteBatch batch = db.batch();
        boolean hasUpdates = false;

        for (DocumentSnapshot doc : docs) {
            String senderId = doc.getString("senderId");
            String status = doc.getString("status");
            if (senderId != null && !senderId.equals(currentUserId) && !"read".equals(status)) {
                batch.update(doc.getReference(), "status", "read");
                batch.update(doc.getReference(), "seen", true);
                batch.update(doc.getReference(), "readStatus", true);
                hasUpdates = true;
            }
        }

        if (hasUpdates) {
            batch.commit().addOnFailureListener(e -> Log.e(TAG, "Failed to update message status to read", e));
        }
    }

    private void sendMessage(String messageText, String type) {
        String lang = "English";
        if (messageText != null) {
            for (char c : messageText.toCharArray()) {
                if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.KANNADA) {
                    lang = "Kannada";
                    break;
                }
            }
        }

        String currentRole = sessionManager.getUserRole();
        String finalSenderRole = currentRole != null ? currentRole : "Speech-Impaired User";
        String finalReceiverRole = receiverRole != null ? receiverRole : "Admin/Caregiver";

        String rEmail = receiverEmail;
        if (rEmail == null || rEmail.isEmpty()) {
            android.content.SharedPreferences settingsPref = getSharedPreferences("AppSettings", MODE_PRIVATE);
            rEmail = settingsPref.getString("caregiverEmail", "caregiver@example.com");
        }

        boolean isVoice = isVoiceInputMessage || "voice".equalsIgnoreCase(type);
        if (isVoice) {
            type = "voice";
            isVoiceInputMessage = false;
        }

        Map<String, Object> msg = new HashMap<>();
        msg.put("chatId", chatId);
        msg.put("senderId", currentUserId);
        msg.put("senderUid", currentUserId);
        msg.put("receiverId", receiverId);
        msg.put("recipientEmail", rEmail.toLowerCase().trim());
        msg.put("senderRole", finalSenderRole);
        msg.put("receiverRole", finalReceiverRole);
        msg.put("message", messageText);
        msg.put("messageText", messageText);
        msg.put("language", lang);
        msg.put("messageType", type);
        msg.put("type", type);
        msg.put("isVoice", isVoice);
        msg.put("status", "sent");
        msg.put("delivered", false);
        msg.put("seen", false);
        msg.put("timestamp", FieldValue.serverTimestamp());

        db.collection("caregiver_messages")
                .add(msg)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Message sent successfully");
                    
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        documentReference.update("status", "delivered", "delivered", true);
                    }, 1000);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message", e);
                    // Save locally as offline fallback
                    LocalConnectionSimulator.saveLocalMessage(ChatActivity.this, chatId, msg);
                    loadLocalMessagesFallback();
                    Toast.makeText(ChatActivity.this, "Saved locally (Offline)", Toast.LENGTH_SHORT).show();
                });
    }

    private void triggerEmergencySOS() {
        String alertId = db.collection("EmergencyAlerts").document().getId();
        Map<String, Object> alert = new HashMap<>();
        alert.put("alertId", alertId);
        alert.put("userId", currentUserId);
        alert.put("userName", currentUserName);
        alert.put("message", "EMERGENCY: SOS triggered via chat!");
        alert.put("timestamp", FieldValue.serverTimestamp());
        alert.put("status", "active");

        db.collection("EmergencyAlerts").document(alertId)
                .set(alert)
                .addOnSuccessListener(aVoid -> Toast.makeText(ChatActivity.this, "Emergency SOS Alert Broadcasted!", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Log.e(TAG, "Failed to broadcast emergency alert", e));
    }

    private void initGoogleTextToSpeech() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                java.util.Locale defaultLocale = java.util.Locale.getDefault();
                int result = tts.setLanguage(defaultLocale);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(java.util.Locale.US);
                }
                isTtsInitialized = true;
            } else {
                Log.e(TAG, "Google TTS initialization failed.");
            }
        });
    }

    private void soundLocalSiren() {
        try {
            Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alert == null) {
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), alert);
            if (r != null) {
                r.play();
                
                // Stop alert after 4 seconds
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (r.isPlaying()) r.stop();
                }, 4000);
            }

            // Trigger hardware haptic feedback
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(2000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Siren alarm sound playback failed.", e);
        }
    }

    private void speakKannadaTTS(String message) {
        if (isTtsInitialized && tts != null && message != null && !message.trim().isEmpty()) {
            String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
            String langName = LanguageManager.detectLanguageFromText(message, userLang);
            java.util.Locale targetLocale = LanguageManager.getLocale(langName);

            int availability = tts.isLanguageAvailable(targetLocale);
            if (availability >= TextToSpeech.LANG_AVAILABLE) {
                tts.setLanguage(targetLocale);
            } else {
                java.util.Locale baseLocale = new java.util.Locale(LanguageManager.normalizeLanguageCode(langName));
                if (tts.isLanguageAvailable(baseLocale) >= TextToSpeech.LANG_AVAILABLE) {
                    tts.setLanguage(baseLocale);
                } else {
                    tts.setLanguage(java.util.Locale.getDefault());
                }
            }
            CaregiverSoundManager.speakWithVolume(tts, message, "sos_local", this);
        }
    }

    private void triggerEmergencySOSPhysical(String customSosMsg) {
        Toast.makeText(this, "EMERGENCY SOS DISPATCHED!", Toast.LENGTH_LONG).show();

        // Sound Siren
        soundLocalSiren();

        // Speak Message
        speakKannadaTTS("Alert. Alert. Help required immediately.");

        // Upload to Firestore
        HashMap<String, String> userDetails = sessionManager.getUserDetails();
        String uid = userDetails.get(SessionManager.KEY_USER_UID);
        String email = userDetails.get(SessionManager.KEY_USER_EMAIL);
        String displayName = (currentUserName != null && !currentUserName.isEmpty()) ? currentUserName : (email != null ? email.split("@")[0] : "Physically Disabled User");

        Map<String, Object> sosLog = new HashMap<>();
        sosLog.put("uid", uid);
        sosLog.put("email", email);
        sosLog.put("userName", displayName);
        sosLog.put("role", RoleManager.ROLE_PHYSICALLY_DISABLED);
        sosLog.put("message", customSosMsg);
        sosLog.put("timestamp", com.google.firebase.Timestamp.now());
        sosLog.put("status", "Unresolved");

        db.collection("emergencies")
                .add(sosLog)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "SOS logged to cloud database.", Toast.LENGTH_SHORT).show();
                    
                    // Also write to EmergencyAlerts for FCM and Service syncing
                    Map<String, Object> fcmAlert = new HashMap<>();
                    String alertId = db.collection("EmergencyAlerts").document().getId();
                    fcmAlert.put("alertId", alertId);
                    fcmAlert.put("userId", uid);
                    fcmAlert.put("userName", displayName);
                    fcmAlert.put("message", customSosMsg);
                    fcmAlert.put("timestamp", com.google.firebase.Timestamp.now());
                    fcmAlert.put("status", "active");
                    fcmAlert.put("role", RoleManager.ROLE_PHYSICALLY_DISABLED);

                    db.collection("EmergencyAlerts").document(alertId)
                            .set(fcmAlert)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "EmergencyAlerts doc written"))
                            .addOnFailureListener(err -> Log.e(TAG, "Failed to write to EmergencyAlerts", err));

                    // Also write to emergency_alerts for redesigned Caregiver dashboard
                    Map<String, Object> newAlert = new HashMap<>();
                    String newAlertId = db.collection("emergency_alerts").document().getId();
                    newAlert.put("alertId", newAlertId);
                    newAlert.put("patientId", uid);
                    newAlert.put("patientName", displayName);
                    newAlert.put("message", customSosMsg);
                    newAlert.put("timestamp", com.google.firebase.Timestamp.now());
                    newAlert.put("status", "NEW");
                    newAlert.put("acknowledged", false);
                    newAlert.put("resolved", false);

                    db.collection("emergency_alerts").document(newAlertId)
                            .set(newAlert)
                            .addOnSuccessListener(aVoid2 -> Log.d(TAG, "emergency_alerts doc written"))
                            .addOnFailureListener(err -> Log.e(TAG, "Failed to write to emergency_alerts", err));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore emergency upload error", e);
                    Toast.makeText(this, "Offline Alert registered.", Toast.LENGTH_SHORT).show();
                });
    }

    private void triggerEmergencySOSSpeech(String customSosMsg) {
        Toast.makeText(this, "EMERGENCY ALARM TRIGGERED!", Toast.LENGTH_LONG).show();

        // Sound Siren
        soundLocalSiren();

        // Speak Message
        speakKannadaTTS(customSosMsg);

        // Upload to Firestore
        HashMap<String, String> userDetails = sessionManager.getUserDetails();
        String uid = userDetails.get(SessionManager.KEY_USER_UID);
        String email = userDetails.get(SessionManager.KEY_USER_EMAIL);
        String displayName = (currentUserName != null && !currentUserName.isEmpty()) ? currentUserName : (email != null ? email.split("@")[0] : "Deaf User");

        Map<String, Object> sosLog = new HashMap<>();
        sosLog.put("uid", uid);
        sosLog.put("email", email);
        sosLog.put("userName", displayName);
        sosLog.put("role", RoleManager.ROLE_SPEECH_IMPAIRED);
        sosLog.put("message", customSosMsg);
        sosLog.put("timestamp", com.google.firebase.Timestamp.now());
        sosLog.put("status", "Unresolved");

        db.collection("emergencies")
                .add(sosLog)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "SOS Alert uploaded successfully to Firestore.", Toast.LENGTH_SHORT).show();
                    
                    // Also write to EmergencyAlerts for FCM and Service syncing
                    Map<String, Object> fcmAlert = new HashMap<>();
                    String alertId = db.collection("EmergencyAlerts").document().getId();
                    fcmAlert.put("alertId", alertId);
                    fcmAlert.put("userId", uid);
                    fcmAlert.put("userName", displayName);
                    fcmAlert.put("message", customSosMsg);
                    fcmAlert.put("timestamp", com.google.firebase.Timestamp.now());
                    fcmAlert.put("status", "active");
                    fcmAlert.put("role", RoleManager.ROLE_SPEECH_IMPAIRED);

                    db.collection("EmergencyAlerts").document(alertId)
                            .set(fcmAlert)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "EmergencyAlerts doc written"))
                            .addOnFailureListener(err -> Log.e(TAG, "Failed to write to EmergencyAlerts", err));

                    // Also write to emergency_alerts for redesigned Caregiver dashboard
                    Map<String, Object> newAlert = new HashMap<>();
                    String newAlertId = db.collection("emergency_alerts").document().getId();
                    newAlert.put("alertId", newAlertId);
                    newAlert.put("patientId", uid);
                    newAlert.put("patientName", displayName);
                    newAlert.put("message", customSosMsg);
                    newAlert.put("timestamp", com.google.firebase.Timestamp.now());
                    newAlert.put("status", "NEW");
                    newAlert.put("acknowledged", false);
                    newAlert.put("resolved", false);

                    db.collection("emergency_alerts").document(newAlertId)
                            .set(newAlert)
                            .addOnSuccessListener(aVoid2 -> Log.d(TAG, "emergency_alerts doc written"))
                            .addOnFailureListener(err -> Log.e(TAG, "Failed to write to emergency_alerts", err));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore emergency upload error", e);
                    Toast.makeText(this, "Offline Alert registered.", Toast.LENGTH_SHORT).show();
                });
    }

    private void triggerChatTransliteration() {
        if (editMessage == null) return;
        final String text = editMessage.getText().toString();
        if (text.trim().isEmpty()) {
            return;
        }

        runOnUiThread(() -> {
            if (progressHomeTranslit != null) progressHomeTranslit.setVisibility(View.VISIBLE);
            if (txtHomeTranslitStatus != null) txtHomeTranslitStatus.setText("Converting...");
        });

        String targetLang = activeTranslitLanguage;
        if (TextUtils.isEmpty(targetLang)) {
            targetLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        }
        final String resolvedLangCode = LanguageManager.normalizeLanguageCode(targetLang);
        final String itcCode = LanguageManager.getInputToolCode(resolvedLangCode);

        networkExecutor.execute(() -> {
            java.net.HttpURLConnection conn = null;
            java.io.BufferedReader reader = null;
            try {
                String encodedText = android.net.Uri.encode(text.trim());
                java.net.URL url = new java.net.URL("https://inputtools.google.com/request?text=" + encodedText + "&itc=" + itcCode + "&num=1");
                
                conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                int code = conn.getResponseCode();
                if (code == java.net.HttpURLConnection.HTTP_OK) {
                    StringBuilder response = new StringBuilder();
                    reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"));
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
                                    if (!editMessage.getText().toString().equals(converted)) {
                                        isTranslitInternalChange = true;
                                        editMessage.setText(converted);
                                        editMessage.setSelection(converted.length());
                                        isTranslitInternalChange = false;
                                    }
                                    if (progressHomeTranslit != null) progressHomeTranslit.setVisibility(View.GONE);
                                    if (txtHomeTranslitStatus != null) txtHomeTranslitStatus.setText("Translit active (" + LanguageManager.getLanguageDisplayName(resolvedLangCode) + ")");
                                });
                                return;
                            }
                        }
                    }
                }
                throw new Exception("Transliteration request failed.");

            } catch (Exception e) {
                Log.w(TAG, "Online transliteration failed. Falling back to local offline rules.", e);
                final String converted = TransliterationEngine.transliterate(text.trim(), resolvedLangCode) + (text.endsWith(" ") ? " " : "");
                runOnUiThread(() -> {
                    if (!editMessage.getText().toString().equals(converted)) {
                        isTranslitInternalChange = true;
                        editMessage.setText(converted);
                        editMessage.setSelection(converted.length());
                        isTranslitInternalChange = false;
                    }
                    if (progressHomeTranslit != null) progressHomeTranslit.setVisibility(View.GONE);
                    if (txtHomeTranslitStatus != null) txtHomeTranslitStatus.setText("Converted (Offline Fallback - " + LanguageManager.getLanguageDisplayName(resolvedLangCode) + ")");
                });
            } finally {
                try {
                    if (reader != null) reader.close();
                } catch (Exception ex) {}
                if (conn != null) conn.disconnect();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        CaregiverSoundManager.stopNotificationSound(this);
        CaregiverSoundManager.stopEmergencySound(this);
        if (currentUserId != null) {
            db.collection("users").document(currentUserId).update("online", true);
        }
        if (isBlindUserMode || (sessionManager != null && RoleManager.ROLE_BLIND_USER.equals(sessionManager.getUserRole()))) {
            WakeWordManager.getInstance(this).setForegroundActivity(this);
            WakeWordManager.getInstance(this).startListening(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isChatAudioRecording) {
            cancelAudioRecording();
        }
        if (isBlindUserMode || (sessionManager != null && RoleManager.ROLE_BLIND_USER.equals(sessionManager.getUserRole()))) {
            WakeWordManager.getInstance(this).pauseListeningForActivity(this);
        }
        ChatAdapter.stopAudioPlayback();
        CaregiverChatAdapter.stopAudioPlayback();
        db.collection("users").document(currentUserId).update("online", false, "typingTo", null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBlindUserMode || (sessionManager != null && RoleManager.ROLE_BLIND_USER.equals(sessionManager.getUserRole()))) {
            WakeWordManager.getInstance(this).pauseListeningForActivity(this);
        }
        if (isChatAudioRecording) {
            cancelAudioRecording();
        }
        if (isBlindRecording) {
            cancelBlindVoiceRecording();
        }
        if (blindTimerRunnable != null) {
            blindTimerHandler.removeCallbacks(blindTimerRunnable);
        }
        ChatAdapter.stopAudioPlayback();
        CaregiverChatAdapter.stopAudioPlayback();
        if (messagesListener != null) {
            messagesListener.remove();
        }
        if (presenceListener != null) {
            presenceListener.remove();
        }
        if (networkExecutor != null) {
            networkExecutor.shutdown();
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    private void toggleVoiceInput() {
        if (isRecording) {
            AccessibleMicFeedbackManager.triggerShortVibration(this);
            stopVoiceInput();
        } else {
            String langCode = activeTranslitLanguage;
            if (TextUtils.isEmpty(langCode)) {
                langCode = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
            }
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                String targetLang = langCode;
                AccessibleMicFeedbackManager.onPermissionDenied(this, tts, isTtsInitialized, targetLang, () -> {
                    androidx.core.app.ActivityCompat.requestPermissions(ChatActivity.this, new String[]{android.Manifest.permission.RECORD_AUDIO}, 201);
                });
            } else {
                AccessibleMicFeedbackManager.startAccessibleMicFlow(this, tts, isTtsInitialized, langCode, this::startVoiceInput);
            }
        }
    }

    private void startVoiceInput() {
        isRecording = true;
        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {}
            speechRecognizer = null;
        }

        try {
            speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create SpeechRecognizer", e);
            AccessibleMicFeedbackManager.resetSessionState();
            isRecording = false;
            return;
        }

        speechRecognizerIntent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        String langCode = activeTranslitLanguage;
        if (TextUtils.isEmpty(langCode)) {
            langCode = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        }
        String speechTag = LanguageManager.getSpeechLanguageTag(langCode);

        speechRecognizerIntent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, speechTag);
        speechRecognizerIntent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, speechTag);
        speechRecognizerIntent.putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizerIntent.putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        speechRecognizer.setRecognitionListener(new android.speech.RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "🎤 Listening... Speak now", Toast.LENGTH_SHORT).show();
                    if (btnMicInput != null) {
                        btnMicInput.setSelected(true);
                    }
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
                    if (btnMicInput != null) btnMicInput.setSelected(false);
                });
            }

            @Override
            public void onError(int error) {
                runOnUiThread(() -> {
                    isRecording = false;
                    if (btnMicInput != null) btnMicInput.setSelected(false);
                    final String activeLang = (!TextUtils.isEmpty(activeTranslitLanguage)) ?
                        activeTranslitLanguage : (sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE);
                    AccessibleMicFeedbackManager.onSttErrorReceived(
                        ChatActivity.this,
                        tts,
                        isTtsInitialized,
                        activeLang,
                        error,
                        () -> {
                            Log.w(TAG, "SpeechRecognizer error code: " + error + ". Launching fallback...");
                            launchVoiceInputFallback(LanguageManager.getSpeechLanguageTag(activeLang), 501);
                        }
                    );
                });
            }

            @Override
            public void onResults(Bundle results) {
                runOnUiThread(() -> {
                    isRecording = false;
                    if (btnMicInput != null) btnMicInput.setSelected(false);
                    String activeLang = activeTranslitLanguage;
                    if (TextUtils.isEmpty(activeLang)) {
                        activeLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
                    }
                    if (results != null) {
                        ArrayList<String> matches = results.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                        if (matches != null && !matches.isEmpty()) {
                            String spokenText = matches.get(0);
                            isVoiceInputMessage = true;
                            AccessibleMicFeedbackManager.onSttResultReceived(
                                ChatActivity.this,
                                tts,
                                isTtsInitialized,
                                activeLang,
                                spokenText,
                                () -> {
                                    if (editMessage != null) {
                                        editMessage.setText(spokenText);
                                        editMessage.setSelection(spokenText.length());
                                    }
                                }
                            );
                        } else {
                            AccessibleMicFeedbackManager.onSttErrorReceived(
                                ChatActivity.this,
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
                            if (editMessage != null && !TextUtils.isEmpty(partialText)) {
                                editMessage.setText(partialText);
                                editMessage.setSelection(partialText.length());
                            }
                        });
                    }
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(speechRecognizerIntent);
    }

    private void launchVoiceInputFallback(String speechTag, int requestCode) {
        try {
            Intent intent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, speechTag);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, speechTag);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "🎤 Speak your message clearly");
            startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            Toast.makeText(this, "Voice input not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 501 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                String spokenText = matches.get(0);
                isVoiceInputMessage = true;
                if (editMessage != null) {
                    editMessage.setText(spokenText);
                    editMessage.setSelection(spokenText.length());
                }
            }
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

        // 4. Speak out loud in target language using Google TTS
        String langCode = activeTranslitLanguage != null ? activeTranslitLanguage : (sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE);
        java.util.Locale locale = LanguageManager.getLocale(langCode);

        if (tts != null && isTtsInitialized) {
            tts.setLanguage(locale);
            CaregiverSoundManager.speakWithVolume(tts, text, "VoiceMsgReadout_" + System.currentTimeMillis(), this);
        } else {
            tts = new TextToSpeech(getApplicationContext(), status -> {
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true;
                    tts.setLanguage(locale);
                    CaregiverSoundManager.speakWithVolume(tts, text, "VoiceMsgReadout_" + System.currentTimeMillis(), ChatActivity.this);
                }
            });
        }
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

    private void stopVoiceInput() {
        isRecording = false;
        if (btnMicInput != null) btnMicInput.setSelected(false);
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    private void initiateVoiceCall() {
        if (receiverId == null || receiverId.isEmpty()) {
            Toast.makeText(this, "Cannot initiate call: recipient user not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 105);
            return;
        }

        if (VoiceCallManager.getInstance(this).isCallActive()) {
            Toast.makeText(this, "A voice call is already in progress", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isChatAudioRecording) {
            cancelAudioRecording();
        }
        ChatAdapter.stopAudioPlayback();
        CaregiverChatAdapter.stopAudioPlayback();
        CaregiverSoundManager.stopNotificationSound(this);
        CaregiverSoundManager.stopEmergencySound(this);

        String myRole = sessionManager.getUserRole();
        if (myRole == null || myRole.isEmpty()) {
            myRole = "user";
        }
        String localName = currentUserName != null && !currentUserName.isEmpty() ? currentUserName : "User";
        String remoteDisplayName = receiverName != null && !receiverName.isEmpty() ? receiverName : "User";
        String remoteRole = receiverRole != null && !receiverRole.isEmpty() ? receiverRole : "patient";

        VoiceCallManager.getInstance(this).startCall(
                currentUserId,
                localName,
                myRole,
                receiverId,
                remoteDisplayName,
                remoteRole,
                null
        );

        Intent callIntent = new Intent(this, VoiceCallActivity.class);
        callIntent.putExtra("calleeUid", receiverId);
        callIntent.putExtra("calleeName", remoteDisplayName);
        callIntent.putExtra("calleeRole", remoteRole);
        callIntent.putExtra("remoteName", remoteDisplayName);
        startActivity(callIntent);
    }

    private void toggleVoiceMessageRecording() {
        if (isChatAudioRecording) {
            stopAndSendVoiceRecording();
        } else {
            if (receiverId == null || receiverId.isEmpty()) {
                Toast.makeText(this, "Recipient not specified", Toast.LENGTH_SHORT).show();
                return;
            }
            if (VoiceCallManager.getInstance(this).isCallActive()) {
                Toast.makeText(this, "Cannot record voice message during an active call", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 106);
                return;
            }
            startAudioRecording();
        }
    }

    private void startAudioRecording() {
        ChatAdapter.stopAudioPlayback();
        CaregiverChatAdapter.stopAudioPlayback();
        try {
            chatTempAudioFile = new File(getExternalCacheDir(), "caregiver_voice_" + System.currentTimeMillis() + ".m4a");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                chatMediaRecorder = new MediaRecorder(this);
            } else {
                chatMediaRecorder = new MediaRecorder();
            }
            chatMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            chatMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            chatMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            chatMediaRecorder.setOutputFile(chatTempAudioFile.getAbsolutePath());
            chatMediaRecorder.prepare();
            chatMediaRecorder.start();

            isChatAudioRecording = true;
            chatRecordingStartTime = System.currentTimeMillis();

            if (btnMicInput != null) {
                btnMicInput.setSelected(true);
                if (btnMicInput instanceof MaterialButton) {
                    ((MaterialButton) btnMicInput).setIconTint(ColorStateList.valueOf(Color.parseColor("#EF4444")));
                    ((MaterialButton) btnMicInput).setStrokeColor(ColorStateList.valueOf(Color.parseColor("#EF4444")));
                }
            }
            if (editMessage != null) {
                editMessage.setHint("🔴 Recording voice message... Tap 🎤 to send");
            }
            Toast.makeText(this, "🔴 Recording voice message... Tap 🎤 again to finish & send", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start audio recording: " + e.getMessage(), e);
            isChatAudioRecording = false;
            Toast.makeText(this, "Failed to start recorder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            resetChatMicUI();
        }
    }

    private void stopAndSendVoiceRecording() {
        if (!isChatAudioRecording) return;
        isChatAudioRecording = false;

        try {
            if (chatMediaRecorder != null) {
                chatMediaRecorder.stop();
                chatMediaRecorder.release();
                chatMediaRecorder = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping media recorder: " + e.getMessage(), e);
        }

        resetChatMicUI();

        if (chatTempAudioFile == null || !chatTempAudioFile.exists() || chatTempAudioFile.length() <= 100) {
            Toast.makeText(this, "Recording discarded (too short or empty)", Toast.LENGTH_SHORT).show();
            cancelAudioRecording();
            return;
        }

        long durationSeconds = Math.max(1, (System.currentTimeMillis() - chatRecordingStartTime) / 1000);
        Toast.makeText(this, "📤 Sending voice message...", Toast.LENGTH_SHORT).show();
        uploadVoiceMessage(chatTempAudioFile, (int) durationSeconds);
    }

    private void cancelAudioRecording() {
        if (isChatAudioRecording) {
            isChatAudioRecording = false;
            try {
                if (chatMediaRecorder != null) {
                    chatMediaRecorder.stop();
                    chatMediaRecorder.release();
                    chatMediaRecorder = null;
                }
            } catch (Exception ignored) {}
        }
        if (chatTempAudioFile != null && chatTempAudioFile.exists()) {
            chatTempAudioFile.delete();
            chatTempAudioFile = null;
        }
        resetChatMicUI();
    }

    private void resetChatMicUI() {
        if (btnMicInput != null) {
            btnMicInput.setSelected(false);
            if (btnMicInput instanceof MaterialButton) {
                int primaryColor = getResources().getColor(R.color.primary);
                ((MaterialButton) btnMicInput).setIconTint(ColorStateList.valueOf(primaryColor));
                ((MaterialButton) btnMicInput).setStrokeColor(ColorStateList.valueOf(primaryColor));
            }
        }
        if (editMessage != null) {
            editMessage.setHint("Type a message...");
        }
    }

    private void uploadVoiceMessage(File audioFile, int durationSeconds) {
        if (audioFile == null || !audioFile.exists()) return;
        if (receiverId == null || receiverId.isEmpty()) return;

        String storagePath = "voice_messages/" + currentUserId + "/" + System.currentTimeMillis() + ".m4a";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(storagePath);

        storageRef.putFile(Uri.fromFile(audioFile))
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        String downloadUrl = downloadUri.toString();
                        saveVoiceMessageToFirestore(downloadUrl, durationSeconds, audioFile);
                    }).addOnFailureListener(e -> {
                        uploadViaDirectStorage(audioFile, durationSeconds, storagePath);
                    });
                })
                .addOnFailureListener(e -> {
                    uploadViaDirectStorage(audioFile, durationSeconds, storagePath);
                });
    }

    private void uploadViaDirectStorage(File audioFile, int durationSeconds, String storagePath) {
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
                    runOnUiThread(() -> saveVoiceMessageToFirestore(downloadUrl, durationSeconds, audioFile));
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(ChatActivity.this, "Unable to send voice message", Toast.LENGTH_SHORT).show();
                        if (audioFile != null && audioFile.exists()) audioFile.delete();
                        if (isBlindUserMode) {
                            resetBlindMicUI();
                        }
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Unable to send voice message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (audioFile != null && audioFile.exists()) audioFile.delete();
                    if (isBlindUserMode) {
                        resetBlindMicUI();
                    }
                });
            }
        });
    }

    private void saveVoiceMessageToFirestore(String downloadUrl, int durationSeconds, File localFile) {
        String currentRole = sessionManager.getUserRole();
        String finalSenderRole = currentRole != null ? currentRole : RoleManager.ROLE_SPEECH_IMPAIRED;
        String finalReceiverRole = receiverRole != null ? receiverRole : RoleManager.ROLE_ADMIN_CAREGIVER;

        String rEmail = receiverEmail;
        if (rEmail == null || rEmail.isEmpty()) {
            android.content.SharedPreferences settingsPref = getSharedPreferences("AppSettings", MODE_PRIVATE);
            rEmail = settingsPref.getString("caregiverEmail", "caregiver@example.com");
        }

        Map<String, Object> msg = new HashMap<>();
        msg.put("chatId", chatId);
        msg.put("senderId", currentUserId);
        msg.put("senderUid", currentUserId);
        msg.put("receiverId", receiverId);
        msg.put("recipientEmail", rEmail != null ? rEmail.toLowerCase().trim() : "");
        msg.put("senderRole", finalSenderRole);
        msg.put("receiverRole", finalReceiverRole);
        msg.put("message", "🎤 Voice Message");
        msg.put("messageText", "🎤 Voice Message");
        msg.put("language", LanguageManager.DEFAULT_LANGUAGE);
        msg.put("messageType", "voice");
        msg.put("type", "voice");
        msg.put("isVoice", true);
        msg.put("audioUrl", downloadUrl);
        msg.put("audioDuration", durationSeconds);
        msg.put("status", "sent");
        msg.put("readStatus", false);
        msg.put("delivered", false);
        msg.put("seen", false);
        msg.put("timestamp", FieldValue.serverTimestamp());

        db.collection("caregiver_messages")
                .add(msg)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Voice message sent successfully: " + documentReference.getId());
                    Toast.makeText(ChatActivity.this, "✓ Voice message sent", Toast.LENGTH_SHORT).show();
                    if (localFile != null && localFile.exists()) {
                        localFile.delete();
                    }
                    if (isBlindUserMode) {
                        resetBlindMicUI();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending voice message", e);
                    LocalConnectionSimulator.saveLocalMessage(ChatActivity.this, chatId, msg);
                    loadLocalMessagesFallback();
                    Toast.makeText(ChatActivity.this, "Saved locally (Offline)", Toast.LENGTH_SHORT).show();
                    if (localFile != null && localFile.exists()) {
                        localFile.delete();
                    }
                    if (isBlindUserMode) {
                        resetBlindMicUI();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 105) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initiateVoiceCall();
            } else {
                Toast.makeText(this, "Microphone permission is required to make voice calls", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 106) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isBlindUserMode) {
                    startBlindAudioRecording();
                } else {
                    startAudioRecording();
                }
            } else {
                Toast.makeText(this, "Microphone permission is required to record voice messages", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // =========================================================================
    // BLIND USER VOICE MESSAGE IMPLEMENTATION (Image 5 Function - Large Mic 🎤)
    // =========================================================================

    public boolean isRecordingActive() {
        return isBlindRecording || isChatAudioRecording;
    }

    private void setupBlindVoiceMessageUI() {
        layoutStandardChatInput = findViewById(R.id.layoutStandardChatInput);
        layoutBlindVoiceMessageInput = findViewById(R.id.layoutBlindVoiceMessageInput);
        btnBlindMicVoiceMessage = findViewById(R.id.btnBlindMicVoiceMessage);
        txtBlindMicStatus = findViewById(R.id.txtBlindMicStatus);
        layoutBlindRecordingTimer = findViewById(R.id.layoutBlindRecordingTimer);
        txtBlindTimerDisplay = findViewById(R.id.txtBlindTimerDisplay);
        layoutBlindRecordingActions = findViewById(R.id.layoutBlindRecordingActions);
        btnBlindStopRecording = findViewById(R.id.btnBlindStopRecording);
        btnBlindCancelRecording = findViewById(R.id.btnBlindCancelRecording);

        String currentRole = sessionManager != null ? sessionManager.getUserRole() : "";
        isBlindUserMode = RoleManager.ROLE_BLIND_USER.equals(currentRole)
                || "Blind User".equalsIgnoreCase(currentRole)
                || "Blind".equalsIgnoreCase(currentRole)
                || getIntent().getBooleanExtra("isBlindUser", false);

        if (isBlindUserMode) {
            // Remove Image 3 (Quick Phrases) and Image 4 (Text Input / Send / Small Mic)
            if (layoutStandardChatInput != null) {
                layoutStandardChatInput.setVisibility(View.GONE);
            }
            if (layoutQuickPhrases != null) {
                layoutQuickPhrases.setVisibility(View.GONE);
            }

            // Show Image 5 (Voice Message Card with Large Microphone 🎤)
            if (layoutBlindVoiceMessageInput != null) {
                layoutBlindVoiceMessageInput.setVisibility(View.VISIBLE);
            }

            if (btnBlindMicVoiceMessage != null) {
                btnBlindMicVoiceMessage.setOnClickListener(v -> {
                    AccessibleMicFeedbackManager.triggerShortVibration(this);
                    if (isBlindRecording) {
                        stopAndSendBlindVoiceRecording(false);
                    } else {
                        checkPermissionAndStartBlindRecording();
                    }
                });
            }

            if (btnBlindStopRecording != null) {
                btnBlindStopRecording.setOnClickListener(v -> {
                    AccessibleMicFeedbackManager.triggerShortVibration(this);
                    stopAndSendBlindVoiceRecording(false);
                });
            }

            if (btnBlindCancelRecording != null) {
                btnBlindCancelRecording.setOnClickListener(v -> {
                    AccessibleMicFeedbackManager.triggerShortVibration(this);
                    cancelBlindVoiceRecording();
                });
            }

            resetBlindMicUI();
        } else {
            if (layoutStandardChatInput != null) {
                layoutStandardChatInput.setVisibility(View.VISIBLE);
            }
            if (layoutBlindVoiceMessageInput != null) {
                layoutBlindVoiceMessageInput.setVisibility(View.GONE);
            }
        }
    }

    private void checkPermissionAndStartBlindRecording() {
        if (receiverId == null || receiverId.isEmpty()) {
            Toast.makeText(this, "Recipient not specified", Toast.LENGTH_SHORT).show();
            return;
        }
        if (VoiceCallManager.getInstance(this).isCallActive()) {
            Toast.makeText(this, "Cannot record voice message during an active call", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 106);
            return;
        }
        startBlindAudioRecording();
    }

    private void startBlindAudioRecording() {
        WakeWordManager.getInstance(this).pauseListening();
        AppVoiceAssistant.getInstance(this).stopListening();
        ChatAdapter.stopAudioPlayback();
        CaregiverChatAdapter.stopAudioPlayback();
        try {
            blindTempAudioFile = new File(getExternalCacheDir(), "blind_voice_" + System.currentTimeMillis() + ".m4a");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                blindMediaRecorder = new MediaRecorder(this);
            } else {
                blindMediaRecorder = new MediaRecorder();
            }
            blindMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            blindMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            blindMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            blindMediaRecorder.setOutputFile(blindTempAudioFile.getAbsolutePath());
            blindMediaRecorder.prepare();
            blindMediaRecorder.start();

            isBlindRecording = true;
            blindRecordingSeconds = 0;

            if (txtBlindMicStatus != null) {
                txtBlindMicStatus.setText("🔴 Recording...");
                txtBlindMicStatus.setTextColor(Color.parseColor("#EF4444"));
            }
            if (layoutBlindRecordingTimer != null) layoutBlindRecordingTimer.setVisibility(View.VISIBLE);
            if (txtBlindTimerDisplay != null) txtBlindTimerDisplay.setText("00:00");
            if (layoutBlindRecordingActions != null) layoutBlindRecordingActions.setVisibility(View.VISIBLE);

            if (btnBlindMicVoiceMessage != null) {
                btnBlindMicVoiceMessage.setText("⏹");
                btnBlindMicVoiceMessage.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EF4444")));
            }

            blindTimerRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isBlindRecording) {
                        blindRecordingSeconds++;
                        int mins = blindRecordingSeconds / 60;
                        int secs = blindRecordingSeconds % 60;
                        if (txtBlindTimerDisplay != null) {
                            txtBlindTimerDisplay.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d", mins, secs));
                        }

                        if (blindRecordingSeconds >= 60) {
                            stopAndSendBlindVoiceRecording(true);
                            return;
                        }
                        blindTimerHandler.postDelayed(this, 1000);
                    }
                }
            };
            blindTimerHandler.postDelayed(blindTimerRunnable, 1000);

        } catch (Exception e) {
            Log.e(TAG, "Failed to start blind audio recording: " + e.getMessage(), e);
            isBlindRecording = false;
            Toast.makeText(this, "Failed to start recorder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            resetBlindMicUI();
        }
    }

    private void stopAndSendBlindVoiceRecording(boolean isMaxLimit) {
        if (!isBlindRecording) return;
        isBlindRecording = false;

        if (blindTimerRunnable != null) {
            blindTimerHandler.removeCallbacks(blindTimerRunnable);
        }

        try {
            if (blindMediaRecorder != null) {
                blindMediaRecorder.stop();
                blindMediaRecorder.release();
                blindMediaRecorder = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping blind media recorder: " + e.getMessage(), e);
        }

        if (blindTempAudioFile == null || !blindTempAudioFile.exists() || blindTempAudioFile.length() <= 100) {
            Toast.makeText(this, "Recording discarded (too short or empty)", Toast.LENGTH_SHORT).show();
            cancelBlindVoiceRecording();
            return;
        }

        if (txtBlindMicStatus != null) {
            txtBlindMicStatus.setText("📤 Uploading / Sending message...");
            txtBlindMicStatus.setTextColor(Color.parseColor("#4F46E5"));
        }
        if (btnBlindMicVoiceMessage != null) btnBlindMicVoiceMessage.setEnabled(false);
        if (btnBlindStopRecording != null) btnBlindStopRecording.setEnabled(false);
        if (btnBlindCancelRecording != null) btnBlindCancelRecording.setEnabled(false);

        int duration = Math.max(1, blindRecordingSeconds);
        uploadVoiceMessage(blindTempAudioFile, duration);
    }

    private void cancelBlindVoiceRecording() {
        if (isBlindRecording) {
            isBlindRecording = false;
            if (blindTimerRunnable != null) {
                blindTimerHandler.removeCallbacks(blindTimerRunnable);
            }
            try {
                if (blindMediaRecorder != null) {
                    blindMediaRecorder.stop();
                    blindMediaRecorder.release();
                    blindMediaRecorder = null;
                }
            } catch (Exception ignored) {}
        }
        if (blindTempAudioFile != null && blindTempAudioFile.exists()) {
            blindTempAudioFile.delete();
            blindTempAudioFile = null;
        }
        Toast.makeText(this, "Recording cancelled", Toast.LENGTH_SHORT).show();
        resetBlindMicUI();
    }

    private void resetBlindMicUI() {
        isBlindRecording = false;
        blindRecordingSeconds = 0;
        if (blindTimerRunnable != null) {
            blindTimerHandler.removeCallbacks(blindTimerRunnable);
        }

        if (btnBlindMicVoiceMessage != null) {
            btnBlindMicVoiceMessage.setText("🎤");
            btnBlindMicVoiceMessage.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4F46E5")));
            btnBlindMicVoiceMessage.setEnabled(true);
        }
        if (txtBlindMicStatus != null) {
            txtBlindMicStatus.setText(getString(R.string.tap_microphone_to_record));
            txtBlindMicStatus.setTextColor(Color.parseColor("#1E1B4B"));
        }
        if (layoutBlindRecordingTimer != null) layoutBlindRecordingTimer.setVisibility(View.GONE);
        if (layoutBlindRecordingActions != null) layoutBlindRecordingActions.setVisibility(View.GONE);
        if (btnBlindStopRecording != null) btnBlindStopRecording.setEnabled(true);
        if (btnBlindCancelRecording != null) btnBlindCancelRecording.setEnabled(true);

        if (isBlindUserMode || (sessionManager != null && RoleManager.ROLE_BLIND_USER.equals(sessionManager.getUserRole()))) {
            WakeWordManager.getInstance(this).resumeListening(this);
        }
    }
}
