package com.kannada.speechassistant;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Controller class for the Emergency Alert Module.
 * Integrates Firebase Cloud Firestore to send/receive real-time alarms.
 * Provides custom status bar notification channels, haptics, color flash indicators,
 * and Text-To-Speech readouts.
 */
public class EmergencyActivity extends AppCompatActivity {

    private static final String TAG = "EmergencyActivity";
    private static final String CHANNEL_ID = "emergency_alerts";
    private static final int NOTIFICATION_ID = 888;
    private static final int VIBRATION_PERMISSION_REQUEST = 3003;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase));
    }

    // Services
    private SessionManager sessionManager;
    private TextToSpeech tts;
    private boolean isTtsInitialized = false;
    private FirebaseFirestore db;
    private ListenerRegistration caregiverListener;

    // Base UI Elements
    private MaterialButton btnBack;
    private LinearLayout layoutPatientSOS;
    private LinearLayout layoutCaregiverMonitor;

    // Patient Widgets
    private MaterialButton btnSOS;
    private EditText editSosMessage;
    private MaterialButton btnPresetFall;
    private MaterialButton btnPresetChoke;
    private MaterialButton btnPresetPain;
    private LinearLayout layoutPatientHistoryList;

    // Caregiver Widgets
    private MaterialCardView cardSystemStatus;
    private View viewAlarmIndicator;
    private TextView txtAlarmStatus;
    private LinearLayout layoutActiveEmergencies;
    private LinearLayout layoutResolvedAlerts;

    // Session user cached details
    private String userUid;
    private String userEmail;
    private String userRole;
    private String userName = "Anonymous User";

    // Siren and Alarm States
    private Ringtone localSiren;
    private Handler warningFlashHandler = new Handler(Looper.getMainLooper());
    private boolean isFlashing = false;
    private int flashColorToggle = 0;
    private Set<String> processedAlertIds = new HashSet<>();
    private long activityStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);

        sessionManager = new SessionManager(this);
        db = FirebaseFirestore.getInstance();
        activityStartTime = System.currentTimeMillis();

        // Security check
        if (!sessionManager.isLoggedIn()) {
            RoleManager.redirectToLogin(this);
            finish();
            return;
        }

        // Bind Base Details
        HashMap<String, String> details = sessionManager.getUserDetails();
        userUid = details.get(SessionManager.KEY_USER_UID);
        userEmail = details.get(SessionManager.KEY_USER_EMAIL);
        userRole = details.get(SessionManager.KEY_USER_ROLE);

        // Fetch User Name from Firestore users profiles
        fetchPatientProfileName();

        // Bind Base UI
        btnBack = findViewById(R.id.btnBack);
        layoutPatientSOS = findViewById(R.id.layoutPatientSOS);
        layoutCaregiverMonitor = findViewById(R.id.layoutCaregiverMonitor);

        btnBack.setOnClickListener(v -> finish());

        // Initialize Text-To-Speech
        initGoogleTTS();

        // Configure Layout according to Role
        if (RoleManager.ROLE_ADMIN_CAREGIVER.equals(userRole)) {
            // Configure Caregiver Monitor
            layoutCaregiverMonitor.setVisibility(View.VISIBLE);
            layoutPatientSOS.setVisibility(View.GONE);

            cardSystemStatus = findViewById(R.id.cardSystemStatus);
            viewAlarmIndicator = findViewById(R.id.viewAlarmIndicator);
            txtAlarmStatus = findViewById(R.id.txtAlarmStatus);
            layoutActiveEmergencies = findViewById(R.id.layoutActiveEmergencies);
            layoutResolvedAlerts = findViewById(R.id.layoutResolvedAlerts);

            // Create system tray notifications channel
            createNotificationChannel();

            // Start Listening for real-time Firestore alerts
            startRealTimeCaregiverListener();

        } else {
            // Configure Patient SOS Trigger
            layoutPatientSOS.setVisibility(View.VISIBLE);
            layoutCaregiverMonitor.setVisibility(View.GONE);

            btnSOS = findViewById(R.id.btnSOS);
            editSosMessage = findViewById(R.id.editSosMessage);
            btnPresetFall = findViewById(R.id.btnPresetFall);
            btnPresetChoke = findViewById(R.id.btnPresetChoke);
            btnPresetPain = findViewById(R.id.btnPresetPain);
            layoutPatientHistoryList = findViewById(R.id.layoutPatientHistoryList);

            // Bind preset clips
            btnPresetFall.setOnClickListener(v -> editSosMessage.setText(getString(R.string.preset_fall)));
            btnPresetChoke.setOnClickListener(v -> editSosMessage.setText(getString(R.string.preset_choke)));
            btnPresetPain.setOnClickListener(v -> editSosMessage.setText(getString(R.string.preset_pain)));

            // SOS trigger listener
            btnSOS.setOnClickListener(v -> sendEmergencySOSAlert());

            // Load patient history list
            loadPatientAlertLogs();

            if (getIntent() != null && getIntent().getBooleanExtra("EXTRA_AUTO_TRIGGER_SOS", false)) {
                getIntent().removeExtra("EXTRA_AUTO_TRIGGER_SOS");
                btnSOS.performClick();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        CaregiverSoundManager.stopEmergencySound(this);
        CaregiverSoundManager.stopNotificationSound(this);
        if (RoleManager.ROLE_ADMIN_CAREGIVER.equals(userRole)) {
            startRealTimeCaregiverListener();
        } else {
            loadPatientAlertLogs();
            if (intent != null && intent.getBooleanExtra("EXTRA_AUTO_TRIGGER_SOS", false)) {
                intent.removeExtra("EXTRA_AUTO_TRIGGER_SOS");
                if (btnSOS != null) {
                    btnSOS.performClick();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        CaregiverSoundManager.stopEmergencySound(this);
        CaregiverSoundManager.stopNotificationSound(this);
    }

    private int setTtsLanguageForUser(TextToSpeech engine) {
        String langCode = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        Locale targetLocale = LanguageManager.getLocale(langCode);
        int res = engine.setLanguage(targetLocale);
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            res = engine.setLanguage(new Locale(langCode));
        }
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            res = engine.setLanguage(Locale.getDefault());
        }
        return res;
    }

    private void initGoogleTTS() {
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
                new Handler(Looper.getMainLooper()).post(this::initDefaultTTS);
            }
        }, "com.google.android.tts");
    }

    private void initDefaultTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = setTtsLanguageForUser(tts);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "TTS locale not supported on default engine. Falling back to default system locale.");
                    tts.setLanguage(Locale.getDefault());
                }
                isTtsInitialized = true;
            } else {
                Log.e(TAG, "Default TTS engine initialization failed.");
            }
        });
    }

    /**
     * Resolves the patient's full name from the users collection.
     */
    private void fetchPatientProfileName() {
        if (userUid == null) return;
        db.collection("users").document(userUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("name")) {
                        userName = documentSnapshot.getString("name");
                    } else {
                        userName = userEmail; // Fallback
                    }
                    Log.i(TAG, "Profile Name resolved: " + userName);
                })
                .addOnFailureListener(e -> {
                    userName = userEmail; // Fallback
                    Log.w(TAG, "Profile Name fetch failed: " + e.getMessage());
                });
    }

    /**
     * Patient Function: Uploads a new emergency document to Cloud Firestore.
     */
    private void sendEmergencySOSAlert() {
        String msg = editSosMessage.getText().toString().trim();
        if (msg.isEmpty()) {
            String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
            String normLang = LanguageManager.normalizeLanguageCode(userLang);
            msg = LanguageManager.getDefaultSosMessage(normLang);
        }

        final String finalMsg = msg;

        // Disperses local TTS feedback and vibrate
        triggerLocalSOSFeedback(finalMsg);

        Map<String, Object> alert = new HashMap<>();
        alert.put("uid", userUid);
        alert.put("userName", userName);
        alert.put("email", userEmail);
        alert.put("role", userRole);
        alert.put("message", finalMsg);
        alert.put("timestamp", Timestamp.now());
        alert.put("status", "Unresolved");

        db.collection("emergencies")
                .add(alert)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(EmergencyActivity.this, "Emergency Alert published to Firestore!", Toast.LENGTH_LONG).show();
                    editSosMessage.setText("");
                    loadPatientAlertLogs();
                    
                    // Also write to EmergencyAlerts for FCM and Service syncing
                    Map<String, Object> fcmAlert = new HashMap<>();
                    String alertId = db.collection("EmergencyAlerts").document().getId();
                    fcmAlert.put("alertId", alertId);
                    fcmAlert.put("userId", userUid);
                    fcmAlert.put("userName", userName);
                    fcmAlert.put("message", finalMsg);
                    fcmAlert.put("timestamp", Timestamp.now());
                    fcmAlert.put("status", "active");

                    db.collection("EmergencyAlerts").document(alertId)
                            .set(fcmAlert)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "EmergencyAlerts doc written"))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to write to EmergencyAlerts", e));

                    // Also write to emergency_alerts for redesigned Caregiver dashboard
                    Map<String, Object> newAlert = new HashMap<>();
                    String newAlertId = db.collection("emergency_alerts").document().getId();
                    newAlert.put("alertId", newAlertId);
                    newAlert.put("patientId", userUid);
                    newAlert.put("patientName", userName);
                    newAlert.put("message", finalMsg);
                    newAlert.put("timestamp", Timestamp.now());
                    newAlert.put("status", "NEW");
                    newAlert.put("acknowledged", false);
                    newAlert.put("resolved", false);

                    db.collection("emergency_alerts").document(newAlertId)
                            .set(newAlert)
                            .addOnSuccessListener(aVoid2 -> Log.d(TAG, "emergency_alerts doc written"))
                            .addOnFailureListener(err -> Log.e(TAG, "Failed to write to emergency_alerts", err));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore emergency dispatch failed", e);
                    Toast.makeText(EmergencyActivity.this, "Offline Alarm sound active. Failed to sync.", Toast.LENGTH_SHORT).show();
                });
    }

    private void triggerLocalSOSFeedback(String alertText) {
        // Maximize System Media Volume for Loud External Output
        try {
            android.media.AudioManager am = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                int maxVol = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
                am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, maxVol, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed setting maximum volume for emergency alert", e);
        }

        // Vibrate with strong emergency pattern
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(new long[]{0, 500, 200, 500, 200, 500, 200, 500}, -1));
            } else {
                vibrator.vibrate(new long[]{0, 500, 200, 500, 200, 500, 200, 500}, -1);
            }
        }

        // TTS vocal feedback in user's language (Malayalam / Hindi / Kannada)
        if (isTtsInitialized && tts != null) {
            String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
            String normLang = LanguageManager.normalizeLanguageCode(userLang);
            Locale targetLocale = LanguageManager.getLocale(userLang);
            tts.setLanguage(targetLocale);

            String prefix;
            if (LanguageManager.LANG_MALAYALAM.equals(normLang)) {
                prefix = "അടിയന്തര മുന്നറിയിപ്പ് നൽകി. ";
            } else if (LanguageManager.LANG_HINDI.equals(normLang)) {
                prefix = "आपातकालीन चेतावनी भेजी गई। ";
            } else {
                prefix = "ತುರ್ತು ಎಚ್ಚರಿಕೆಯನ್ನು ಕಳುಹಿಸಲಾಗಿದೆ. ";
            }
            String toSpeak = alertText.split("\\(")[0].trim();
            CaregiverSoundManager.speakWithVolume(tts, prefix + toSpeak, "sos_local", this);
        }
    }

    /**
     * Patient Function: Loads past alerts initiated by this user.
     */
    private void loadPatientAlertLogs() {
        if (userUid == null) return;
        db.collection("emergencies")
                .whereEqualTo("uid", userUid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        layoutPatientHistoryList.removeAllViews();
                        List<DocumentSnapshot> docs = task.getResult().getDocuments();

                        if (docs.isEmpty()) {
                            TextView empty = new TextView(this);
                            empty.setText("No previous emergencies logged.");
                            empty.setTextColor(Color.parseColor("#64748B"));
                            layoutPatientHistoryList.addView(empty);
                            return;
                        }

                        // Sort desc
                        docs.sort((d1, d2) -> {
                            Timestamp t1 = d1.getTimestamp("timestamp");
                            Timestamp t2 = d2.getTimestamp("timestamp");
                            if (t1 != null && t2 != null) return t2.compareTo(t1);
                            return 0;
                        });

                        int limit = 0;
                        for (DocumentSnapshot doc : docs) {
                            if (limit++ >= 5) break;

                            String msg = doc.getString("message");
                            String status = doc.getString("status");
                            Timestamp ts = doc.getTimestamp("timestamp");
                            String dateStr = ts != null ? ts.toDate().toLocaleString() : "Unknown date";

                            View item = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
                            TextView text1 = item.findViewById(android.R.id.text1);
                            TextView text2 = item.findViewById(android.R.id.text2);

                            text1.setText(msg);
                            text1.setTextColor(Color.parseColor("#991B1B"));
                            text2.setText(dateStr + " | Status: " + status);
                            text2.setTextColor(Color.parseColor("#64748B"));

                            layoutPatientHistoryList.addView(item);
                        }
                    }
                });
    }

    /**
     * Caregiver Function: Initiates Snapshot Listener to receive real-time collection updates.
     */
    private void startRealTimeCaregiverListener() {
        caregiverListener = db.collection("emergencies")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@NonNull QuerySnapshot value, FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e(TAG, "Firestore Snapshot Listener failed", error);
                            return;
                        }

                        if (value == null) return;

                        List<DocumentSnapshot> unresolvedList = new ArrayList<>();
                        List<DocumentSnapshot> resolvedList = new ArrayList<>();

                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String status = doc.getString("status");
                            if ("Unresolved".equalsIgnoreCase(status)) {
                                unresolvedList.add(doc);
                            } else {
                                resolvedList.add(doc);
                            }
                        }

                        // Update Caregiver lists
                        updateCaregiverMonitorUI(unresolvedList, resolvedList);
                    }
                });
    }

    /**
     * Caregiver Function: Syncs UI views with snapshot arrays and triggers warnings.
     */
    private void updateCaregiverMonitorUI(List<DocumentSnapshot> unresolved, List<DocumentSnapshot> resolved) {
        layoutActiveEmergencies.removeAllViews();
        layoutResolvedAlerts.removeAllViews();

        // Sort both by timestamp desc
        unresolved.sort((d1, d2) -> {
            Timestamp t1 = d1.getTimestamp("timestamp");
            Timestamp t2 = d2.getTimestamp("timestamp");
            if (t1 != null && t2 != null) return t2.compareTo(t1);
            return 0;
        });

        resolved.sort((d1, d2) -> {
            Timestamp t1 = d1.getTimestamp("timestamp");
            Timestamp t2 = d2.getTimestamp("timestamp");
            if (t1 != null && t2 != null) return t2.compareTo(t1);
            return 0;
        });

        // 1. Populate Unresolved List
        if (unresolved.isEmpty()) {
            // System Safe
            cardSystemStatus.setCardBackgroundColor(Color.parseColor("#ECFDF5"));
            cardSystemStatus.setStrokeColor(Color.parseColor("#10B981"));
            viewAlarmIndicator.setBackgroundColor(Color.parseColor("#10B981"));
            txtAlarmStatus.setText("System Status: All Patients Safe");
            txtAlarmStatus.setTextColor(Color.parseColor("#047857"));

            // Stop alarm sirens & flash warnings
            stopActiveAlertWarnings();

            TextView empty = new TextView(this);
            empty.setText("No active patient emergencies.");
            empty.setTextColor(Color.parseColor("#64748B"));
            layoutActiveEmergencies.addView(empty);

        } else {
            // Alarms active
            cardSystemStatus.setCardBackgroundColor(Color.parseColor("#FEF2F2"));
            cardSystemStatus.setStrokeColor(Color.parseColor("#EF4444"));
            viewAlarmIndicator.setBackgroundColor(Color.parseColor("#EF4444"));
            txtAlarmStatus.setText("🚨 WARNING: " + unresolved.size() + " ACTIVE EMERGENCIES!");
            txtAlarmStatus.setTextColor(Color.parseColor("#991B1B"));

            for (DocumentSnapshot doc : unresolved) {
                String docId = doc.getId();
                String name = doc.getString("userName");
                String role = doc.getString("role");
                String email = doc.getString("email");
                String message = doc.getString("message");
                Timestamp ts = doc.getTimestamp("timestamp");
                String dateStr = ts != null ? ts.toDate().toLocaleString() : "Unknown";

                // Inflate custom card layout
                View card = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
                TextView text1 = card.findViewById(android.R.id.text1);
                TextView text2 = card.findViewById(android.R.id.text2);

                text1.setText("🚨 " + (name != null ? name : email) + " (" + role + "): \"" + message + "\"");
                text1.setTextColor(Color.parseColor("#991B1B"));
                text2.setText(dateStr + " | Click here to Mark as Resolved");
                text2.setTextColor(Color.parseColor("#DC2626"));
                text2.setTextSize(12);

                card.setOnClickListener(v -> markEmergencyAsResolved(docId));
                layoutActiveEmergencies.addView(card);

                // Notification Trigger check: Verify if alert is new (timestamp > activityStartTime)
                long alertTime = ts != null ? ts.toDate().getTime() : System.currentTimeMillis();
                if (alertTime > activityStartTime && !processedAlertIds.contains(docId)) {
                    processedAlertIds.add(docId);
                    // Trigger Alarm Siren, Status bar FCM-like notification, Screen flashes, and TTS
                    triggerIncomingAlertNotification(name, message);
                }
            }
        }

        // 2. Populate Resolved List
        int limit = 0;
        for (DocumentSnapshot doc : resolved) {
            if (limit++ >= 5) break;

            String name = doc.getString("userName");
            String email = doc.getString("email");
            String message = doc.getString("message");
            Timestamp ts = doc.getTimestamp("timestamp");
            String dateStr = ts != null ? ts.toDate().toLocaleString() : "Unknown";

            View card = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
            TextView text1 = card.findViewById(android.R.id.text1);
            TextView text2 = card.findViewById(android.R.id.text2);

            text1.setText((name != null ? name : email) + ": \"" + message + "\"");
            text1.setTextColor(Color.parseColor("#475569"));
            text2.setText("Resolved on " + dateStr);
            text2.setTextColor(Color.parseColor("#64748B"));

            layoutResolvedAlerts.addView(card);
        }
    }

    /**
     * Caregiver Function: Sounds sirens, posts heads-up status alerts, flashes display colors,
     * and triggers TTS announcements.
     */
    private void triggerIncomingAlertNotification(String senderName, String alertMsg) {
        // 1. Post Status Tray Notification
        triggerSystemTrayNotification(senderName, alertMsg);

        // 2. Play Alarm Sound Loop
        soundContinuousSiren();

        // 3. Start Screen Flash Animations
        startVisualFlashWarning();

        // 4. TTS Readout
        if (isTtsInitialized && tts != null) {
            String toSpeak = alertMsg.split("\\(")[0].trim();
            CaregiverSoundManager.speakWithVolume(tts, senderName + " ರಿಂದ ತುರ್ತು ಎಚ್ಚರಿಕೆ ಬಂದಿದೆ. ಸಂದೇಶ: " + toSpeak, TextToSpeech.QUEUE_ADD, "sos_caregiver_alert", this);
        }
    }

    /**
     * Fires local system notification.
     */
    private void triggerSystemTrayNotification(String name, String message) {
        Intent intent = new Intent(this, EmergencyActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("🚨 PATIENT EMERGENCY ALERT!")
                .setContentText(name + " needs help: \"" + message + "\"")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Emergency Alerts Channel";
            String description = "Real-time Patient Emergency notification logs.";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 250, 100, 500, 100, 250, 100, 500});

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void soundContinuousSiren() {
        CaregiverSoundManager.playEmergencyAlertSoundAndVibration(this);
    }

    private void startVisualFlashWarning() {
        if (isFlashing) return;
        isFlashing = true;
        flashWarningLoop();
    }

    private void flashWarningLoop() {
        if (!isFlashing) return;

        // Toggle background color of main layout
        View root = findViewById(R.id.headerCard).getRootView();
        if (flashColorToggle == 0) {
            root.setBackgroundColor(Color.parseColor("#FEE2E2")); // Light Red
            flashColorToggle = 1;
        } else {
            root.setBackgroundColor(Color.parseColor("#F8FAFC")); // Default Off-White
            flashColorToggle = 0;
        }

        warningFlashHandler.postDelayed(this::flashWarningLoop, 500);
    }

    private void stopActiveAlertWarnings() {
        isFlashing = false;
        warningFlashHandler.removeCallbacksAndMessages(null);
        View root = findViewById(R.id.headerCard).getRootView();
        root.setBackgroundColor(Color.parseColor("#F8FAFC")); // Reset background

        if (localSiren != null && localSiren.isPlaying()) {
            localSiren.stop();
        }
    }

    /**
     * Caregiver Function: Updates document status field to Resolved.
     */
    private void markEmergencyAsResolved(String documentId) {
        CaregiverSoundManager.stopEmergencySound(this);
        db.collection("emergencies").document(documentId)
                .update("status", "Resolved")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EmergencyActivity.this, "Alert Marked as Resolved.", Toast.LENGTH_SHORT).show();
                    // Clear notifications if active
                    NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
                })
                .addOnFailureListener(e -> Toast.makeText(EmergencyActivity.this, "Failed to resolve: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopActiveAlertWarnings();
        if (caregiverListener != null) {
            caregiverListener.remove();
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
