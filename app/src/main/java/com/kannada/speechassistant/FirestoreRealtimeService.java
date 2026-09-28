package com.kannada.speechassistant;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.media.Ringtone;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;

/**
 * Centrally manages real-time Firestore listeners in a background Service.
 * Syncs incoming emergency alerts and chat messages, firing local notifications.
 */
public class FirestoreRealtimeService extends Service {

    private static final String TAG = "FirestoreRealtimeService";
    private static final String CHANNEL_EMERGENCY = "emergency_channel_v2";
    private static final String CHANNEL_CHAT = "chat_channel_v3";
    private static final String CHANNEL_VOICE_CALLS = "voice_call_channel_v2";

    private final java.util.Set<String> processedIncomingCallIds = new java.util.HashSet<>();

    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private String userUid;
    private String userRole;

    private ListenerRegistration emergenciesListener;
    private ListenerRegistration chatsListener;
    private ListenerRegistration voiceCallsListener;
    private android.speech.tts.TextToSpeech serviceTts;
    private boolean isServiceTtsInitialized = false;
    private final java.util.Set<String> spokenMsgIds = new java.util.HashSet<>();
    private long serviceStartTime = System.currentTimeMillis();

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        createNotificationChannels();
        initServiceTts();

        if (sessionManager.isLoggedIn()) {
            HashMap<String, String> details = sessionManager.getUserDetails();
            userUid = details.get(SessionManager.KEY_USER_UID);
            userRole = details.get(SessionManager.KEY_USER_ROLE);

            startListening();
        } else {
            stopSelf();
        }
    }

    private void initServiceTts() {
        serviceTts = new android.speech.tts.TextToSpeech(getApplicationContext(), status -> {
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                isServiceTtsInitialized = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    android.media.AudioAttributes aa = new android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build();
                    serviceTts.setAudioAttributes(aa);
                }
            }
        });
    }

    private void speakCaregiverMessageLoud(String messageText) {
        if (android.text.TextUtils.isEmpty(messageText)) return;

        // Vibrate haptic feedback
        try {
            android.os.Vibrator v = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(android.os.VibrationEffect.createOneShot(600, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(600);
                }
            }
        } catch (Exception e) {}

        // Speak via TTS obeying recipient's saved TTS volume
        String userLang = sessionManager != null ? sessionManager.getLanguage() : LanguageManager.DEFAULT_LANGUAGE;
        String langName = LanguageManager.detectLanguageFromText(messageText, userLang);
        java.util.Locale locale = LanguageManager.getLocale(langName);

        if (serviceTts != null && isServiceTtsInitialized) {
            serviceTts.setLanguage(locale);
            CaregiverSoundManager.speakWithVolume(serviceTts, messageText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, "CaregiverVoiceMsg_" + System.currentTimeMillis(), this, userUid);
        } else {
            serviceTts = new android.speech.tts.TextToSpeech(getApplicationContext(), status -> {
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    isServiceTtsInitialized = true;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        android.media.AudioAttributes aa = new android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build();
                        serviceTts.setAudioAttributes(aa);
                    }
                    serviceTts.setLanguage(locale);
                    CaregiverSoundManager.speakWithVolume(serviceTts, messageText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, "CaregiverVoiceMsg_" + System.currentTimeMillis(), FirestoreRealtimeService.this, userUid);
                }
            });
        }
    }

    private void playCaregiverNotificationSound3Times(String msgId) {
        CaregiverSoundManager.playNotificationSound3Times(this, userUid, msgId);
    }

    private void playCaregiverEmergencySoundAndVibration() {
        CaregiverSoundManager.playEmergencyAlertSoundAndVibration(this, userUid);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started");
        if (sessionManager.isLoggedIn()) {
            HashMap<String, String> details = sessionManager.getUserDetails();
            userUid = details.get(SessionManager.KEY_USER_UID);
            userRole = details.get(SessionManager.KEY_USER_ROLE);

            if (emergenciesListener == null && chatsListener == null) {
                startListening();
            }
        }
        return START_STICKY;
    }

    private void startListening() {
        if (userUid == null || userUid.isEmpty()) return;
        serviceStartTime = System.currentTimeMillis();

        // 1. Listen for Emergency Alerts (All users for Admin/Caregivers; Patient's own or active ones)
        if (RoleManager.ROLE_ADMIN_CAREGIVER.equals(userRole)) {
            Query query = db.collection("emergency_alerts")
                    .whereEqualTo("status", "NEW");

            emergenciesListener = query.addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                    if (error != null) {
                        Log.e(TAG, "Emergency listener error", error);
                        return;
                    }
                    if (value != null) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                com.google.firebase.Timestamp ts = dc.getDocument().getTimestamp("timestamp");
                                if (ts != null && ts.toDate().getTime() < serviceStartTime - 5000) {
                                    // Skip old historical emergency alerts
                                    continue;
                                }

                                String name = dc.getDocument().getString("patientName");
                                String msg = dc.getDocument().getString("message");
                                String patientId = dc.getDocument().getString("patientId");

                                if (patientId != null) {
                                    db.collection("users").document(patientId).get()
                                            .addOnSuccessListener(userDoc -> {
                                                String role = "Patient";
                                                if (userDoc.exists()) {
                                                    String fetchedRole = userDoc.getString("role");
                                                    if (fetchedRole != null) role = fetchedRole;
                                                }
                                                playCaregiverEmergencySoundAndVibration();
                                                showEmergencyNotification(name, msg, role);
                                            })
                                            .addOnFailureListener(e -> {
                                                playCaregiverEmergencySoundAndVibration();
                                                showEmergencyNotification(name, msg, "Patient");
                                            });
                                } else {
                                    playCaregiverEmergencySoundAndVibration();
                                    showEmergencyNotification(name, msg, "Patient");
                                }
                            }
                        }
                    }
                }
            });
        }

        // 2. Listen for Chats (Where current user is the receiver)
        Query chatQuery = db.collection("caregiver_messages")
                .whereEqualTo("receiverId", userUid);

        chatsListener = chatQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e(TAG, "Chats group listener error", error);
                    return;
                }
                if (value != null) {
                    for (DocumentChange dc : value.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED || dc.getType() == DocumentChange.Type.MODIFIED) {
                            String msgId = dc.getDocument().getId();
                            String senderId = dc.getDocument().getString("senderId");
                            String text = dc.getDocument().getString("message");
                            if (android.text.TextUtils.isEmpty(text)) {
                                text = dc.getDocument().getString("messageText");
                            }
                            String type = dc.getDocument().getString("type");

                            com.google.firebase.Timestamp ts = dc.getDocument().getTimestamp("timestamp");
                            if (ts != null && ts.toDate().getTime() < serviceStartTime - 5000) {
                                // Track ID as seen, but skip notification shade alert & sound for old messages
                                spokenMsgIds.add(msgId);
                                CaregiverSoundManager.markMessageProcessed(msgId, senderId, text);
                                continue;
                            }

                            if (senderId != null && !senderId.equals(userUid) && !android.text.TextUtils.isEmpty(text)) {
                                if (!spokenMsgIds.contains(msgId)) {
                                    spokenMsgIds.add(msgId);
                                    CaregiverSoundManager.handleIncomingMessage(FirestoreRealtimeService.this, msgId, senderId, text, type, "", "");
                                    fetchSenderAndNotify(senderId, text, type, msgId);
                                }
                            }
                        }
                    }
                }
            }
        });

        // 3. Listen for Incoming Voice Calls (Where current user is the callee)
        Query callQuery = db.collection("voice_calls")
                .whereEqualTo("calleeUid", userUid)
                .whereEqualTo("status", "RINGING");

        voiceCallsListener = callQuery.addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            for (DocumentChange dc : value.getDocumentChanges()) {
                if (dc.getType() == DocumentChange.Type.ADDED || dc.getType() == DocumentChange.Type.MODIFIED) {
                    String callId = dc.getDocument().getId();
                    String callerUid = dc.getDocument().getString("callerUid");
                    String callerName = dc.getDocument().getString("callerName");
                    String callerRole = dc.getDocument().getString("callerRole");

                    com.google.firebase.Timestamp ts = dc.getDocument().getTimestamp("createdAt");
                    if (ts != null && ts.toDate().getTime() < serviceStartTime - 10000) {
                        continue;
                    }

                    // Duplicate Call Protection
                    if (processedIncomingCallIds.contains(callId)) {
                        continue;
                    }
                    processedIncomingCallIds.add(callId);

                    Log.d(TAG, "Incoming voice call detected: " + callId + " from " + callerName + " (" + callerRole + ")");

                    // 1. High-Priority Full-Screen Notification (for Background / Lock-screen reliability)
                    showIncomingCallNotification(callId, callerUid, callerName, callerRole);

                    // 2. Direct Foreground Launch (if app is already in foreground)
                    try {
                        Intent callIntent = new Intent(FirestoreRealtimeService.this, IncomingCallActivity.class);
                        callIntent.putExtra("callId", callId);
                        callIntent.putExtra("callerUid", callerUid);
                        callIntent.putExtra("callerName", callerName);
                        callIntent.putExtra("callerRole", callerRole);
                        callIntent.putExtra("calleeUid", userUid);
                        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(callIntent);
                        Log.d(TAG, "Direct IncomingCallActivity startActivity triggered");
                    } catch (Exception e) {
                        Log.w(TAG, "Direct startActivity blocked by OS in background (handled by full-screen notification): " + e.getMessage());
                    }
                }
            }
        });
    }

    private void showIncomingCallNotification(String callId, String callerUid, String callerName, String callerRole) {
        Intent fullScreenIntent = new Intent(this, IncomingCallActivity.class);
        fullScreenIntent.putExtra("callId", callId);
        fullScreenIntent.putExtra("callerUid", callerUid);
        fullScreenIntent.putExtra("callerName", callerName);
        fullScreenIntent.putExtra("callerRole", callerRole);
        fullScreenIntent.putExtra("calleeUid", userUid);
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                callId.hashCode(),
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        if (ringtoneUri == null) {
            ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        String displayName = callerName != null && !callerName.isEmpty() ? callerName : "Blind User";
        String displayRole = callerRole != null && !callerRole.isEmpty() ? callerRole : "Blind User";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_VOICE_CALLS)
                .setSmallIcon(android.R.drawable.sym_call_incoming)
                .setContentTitle("Incoming Voice Call")
                .setContentText(displayName + " (" + displayRole + ") is calling...")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .setSound(ringtoneUri)
                .setVibrate(new long[]{0, 1000, 500, 1000})
                .setAutoCancel(true)
                .setOngoing(true);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(callId.hashCode(), builder.build());
            Log.d(TAG, "High-priority full-screen incoming call notification posted for callId: " + callId);
        }
    }

    private void fetchSenderAndNotify(String senderId, String messageText, String type, String msgId) {
        db.collection("users").document(senderId).get().addOnSuccessListener(documentSnapshot -> {
            String name = "Caregiver";
            String role = RoleManager.ROLE_ADMIN_CAREGIVER;
            String email = "";
            String language = "English";
            if (documentSnapshot.exists()) {
                String fetchedName = documentSnapshot.getString("name");
                if (fetchedName != null) name = fetchedName;
                String fetchedRole = documentSnapshot.getString("role");
                if (fetchedRole != null) role = fetchedRole;
                String fetchedEmail = documentSnapshot.getString("email");
                if (fetchedEmail != null) email = fetchedEmail;
                String fetchedLang = documentSnapshot.getString("language");
                if (fetchedLang != null) language = fetchedLang;
            }
            showChatNotification(senderId, name, role, email, language, messageText, type, msgId);
        }).addOnFailureListener(e -> {
            showChatNotification(senderId, "Caregiver", RoleManager.ROLE_ADMIN_CAREGIVER, "", "English", messageText, type, msgId);
        });
    }

    private void showEmergencyNotification(String userName, String message, String role) {
        Intent intent;
        if (RoleManager.ROLE_ADMIN_CAREGIVER.equals(userRole)) {
            intent = new Intent(this, AdminDashboardActivity.class);
            intent.putExtra("openTab", 1);
        } else {
            intent = new Intent(this, EmergencyActivity.class);
        }
        intent.putExtra("isEmergency", true);
        intent.putExtra("patientName", userName);
        intent.putExtra("message", message);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = "🚨 EMERGENCY SOS ALERT 🚨";
        if (RoleManager.ROLE_MUTE_USER.equals(role)) {
            title = "🚨 MUTE USER SOS ALERT 🚨";
        } else if (RoleManager.ROLE_SPEECH_IMPAIRED.equals(role) || RoleManager.ROLE_DEAF_USER.equals(role)) {
            title = "🚨 DEAF USER SOS ALERT 🚨";
        } else if (RoleManager.ROLE_PHYSICALLY_DISABLED.equals(role)) {
            title = "🚨 PHYSICALLY-DISABLED SOS ALERT 🚨";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_EMERGENCY)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(title)
                .setContentText((userName != null ? userName : "User") + ": " + message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void showChatNotification(String senderId, String senderName, String senderRole, String senderEmail, String senderLanguage, String messageText, String type, String msgId) {
        boolean isSosEmergency = "emergency".equalsIgnoreCase(type) || "sos".equalsIgnoreCase(type);
        Intent intent;

        if (isSosEmergency) {
            // SOS Emergency message opens EmergencyActivity / Emergency page
            if (RoleManager.ROLE_ADMIN_CAREGIVER.equals(userRole)) {
                intent = new Intent(this, AdminDashboardActivity.class);
                intent.putExtra("openTab", 1);
            } else {
                intent = new Intent(this, EmergencyActivity.class);
            }
            intent.putExtra("isEmergency", true);
            intent.putExtra("receiverId", senderId);
            intent.putExtra("receiverName", senderName);
            intent.putExtra("receiverRole", senderRole);
            intent.putExtra("receiverEmail", senderEmail);
            intent.putExtra("receiverLanguage", senderLanguage);
            intent.putExtra("patientId", senderId);
            intent.putExtra("patientName", senderName);
            intent.putExtra("message", messageText);
        } else {
            // Regular message opens standard ChatActivity for this user/chat
            intent = new Intent(this, ChatActivity.class);
            intent.putExtra("receiverId", senderId);
            intent.putExtra("receiverName", senderName);
            intent.putExtra("receiverRole", senderRole);
            intent.putExtra("receiverEmail", senderEmail);
            intent.putExtra("receiverLanguage", senderLanguage);
            intent.putExtra("isEmergency", false);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int notifId = (msgId != null && !msgId.isEmpty()) ? msgId.hashCode() : (senderId != null ? senderId.hashCode() : (int) System.currentTimeMillis());
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                notifId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String channelId = isSosEmergency ? CHANNEL_EMERGENCY : CHANNEL_CHAT;
        String title;
        if (isSosEmergency) {
            title = "🚨 " + senderName + " needs help! 🚨";
        } else if ("voice".equalsIgnoreCase(type) || (messageText != null && messageText.contains("🎤"))) {
            title = "🎤 New voice message from " + (senderName != null ? senderName : "Blind User");
        } else {
            title = senderName != null ? senderName : "Caregiver Message";
        }
        int icon = isSosEmergency ? android.R.drawable.stat_notify_error : android.R.drawable.stat_notify_chat;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(messageText)
                .setPriority(isSosEmergency ? NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_HIGH)
                .setCategory(isSosEmergency ? NotificationCompat.CATEGORY_ALARM : NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(isSosEmergency ? NotificationCompat.DEFAULT_ALL : (NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_LIGHTS))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notifId, builder.build());
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (alarmUri == null) alarmUri = soundUri;

                android.media.AudioAttributes audioAttr = new android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();

                // Emergency Channel
                NotificationChannel emerChan = new NotificationChannel(
                        CHANNEL_EMERGENCY,
                        "Emergency Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                );
                emerChan.setDescription("High priority emergency alarms");
                emerChan.enableVibration(true);
                emerChan.setVibrationPattern(new long[]{100, 500, 200, 500});
                emerChan.setSound(alarmUri, audioAttr);
                manager.createNotificationChannel(emerChan);

                // Chat Channel
                NotificationChannel chatChan = new NotificationChannel(
                        CHANNEL_CHAT,
                        "Chat Messages",
                        NotificationManager.IMPORTANCE_HIGH
                );
                chatChan.setDescription("One-to-one helper chats");
                chatChan.enableVibration(true);
                chatChan.setSound(null, null);
                manager.createNotificationChannel(chatChan);

                // Voice Calls Channel
                Uri ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                if (ringUri == null) ringUri = soundUri;

                android.media.AudioAttributes callAudioAttr = new android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();

                NotificationChannel callChan = new NotificationChannel(
                        CHANNEL_VOICE_CALLS,
                        "Incoming Voice Calls",
                        NotificationManager.IMPORTANCE_HIGH
                );
                callChan.setDescription("Real-time incoming voice calls");
                callChan.enableVibration(true);
                callChan.setVibrationPattern(new long[]{0, 1000, 500, 1000});
                callChan.setSound(ringUri, callAudioAttr);
                callChan.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
                manager.createNotificationChannel(callChan);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (emergenciesListener != null) {
            emergenciesListener.remove();
        }
        if (chatsListener != null) {
            chatsListener.remove();
        }
        if (voiceCallsListener != null) {
            voiceCallsListener.remove();
        }
        Log.i(TAG, "Service destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
