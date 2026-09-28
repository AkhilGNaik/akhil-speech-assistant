package com.kannada.speechassistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles incoming Firebase Cloud Messaging (FCM) notifications.
 * Automatically saves new registration tokens to Firestore and posts local status bar alerts.
 */
public class NotificationService extends FirebaseMessagingService {

    private static final String TAG = "NotificationService";
    private static final String CHANNEL_FCM = "fcm_channel_v2";

    public static class NotificationItem {
        private final String title;
        private final String message;
        private final String type;
        private final long timestamp;

        public NotificationItem(@Nullable String title, @Nullable String message, @Nullable String type, long timestamp) {
            this.title = title != null ? title : "";
            this.message = message != null ? message : "";
            this.type = type != null ? type : "";
            this.timestamp = timestamp;
        }

        @NonNull
        public String getTitle() {
            return title;
        }

        @NonNull
        public String getMessage() {
            return message;
        }

        @NonNull
        public String getType() {
            return type;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @NonNull
        public String getSpokenText() {
            if (message.isEmpty()) {
                return title;
            }
            if (title.isEmpty() || "Notification".equalsIgnoreCase(title)) {
                return message;
            }
            if (message.startsWith(title)) {
                return message;
            }
            return title + ": " + message;
        }
    }

    private static final List<NotificationItem> recentNotifications = new ArrayList<>();
    private static final int MAX_RECENT_NOTIFICATIONS = 20;

    public static synchronized void recordNotification(@Nullable String title, @Nullable String message, @Nullable String type, long timestamp) {
        if ((title == null || title.trim().isEmpty()) && (message == null || message.trim().isEmpty())) {
            return;
        }
        recentNotifications.add(0, new NotificationItem(title, message, type, timestamp));
        while (recentNotifications.size() > MAX_RECENT_NOTIFICATIONS) {
            recentNotifications.remove(recentNotifications.size() - 1);
        }
    }

    public static synchronized void clearRecentNotificationsForTesting() {
        recentNotifications.clear();
    }

    @NonNull
    public static synchronized List<NotificationItem> getReadableNotifications(@NonNull Context context, int maxLimit) {
        List<NotificationItem> result = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        // 1. Query active system status bar notifications posted by this application
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StatusBarNotification[] sbns = nm.getActiveNotifications();
                if (sbns != null) {
                    List<StatusBarNotification> sbnList = new ArrayList<>(Arrays.asList(sbns));
                    Collections.sort(sbnList, (s1, s2) -> Long.compare(s2.getPostTime(), s1.getPostTime()));

                    for (StatusBarNotification sbn : sbnList) {
                        Notification n = sbn.getNotification();
                        if (n == null) continue;

                        if (Notification.CATEGORY_CALL.equalsIgnoreCase(n.category)) {
                            continue;
                        }

                        Bundle extras = n.extras;
                        CharSequence titleCs = extras != null ? extras.getCharSequence(Notification.EXTRA_TITLE) : null;
                        CharSequence textCs = extras != null ? extras.getCharSequence(Notification.EXTRA_BIG_TEXT) : null;
                        if (textCs == null && extras != null) {
                            textCs = extras.getCharSequence(Notification.EXTRA_TEXT);
                        }

                        String title = titleCs != null ? titleCs.toString().trim() : "";
                        String text = textCs != null ? textCs.toString().trim() : "";

                        if (!text.isEmpty() || !title.isEmpty()) {
                            String key = title + ":::" + text;
                            if (!seenKeys.contains(key)) {
                                seenKeys.add(key);
                                result.add(new NotificationItem(title, text, n.category != null ? n.category : "text", sbn.getPostTime()));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error fetching active status bar notifications: " + e.getMessage());
        }

        // 2. Supplement with recent in-memory notifications from NotificationService
        for (NotificationItem item : recentNotifications) {
            String key = item.getTitle() + ":::" + item.getMessage();
            if (!seenKeys.contains(key)) {
                seenKeys.add(key);
                result.add(item);
            }
        }

        // 3. Sort overall result by timestamp descending (newest first)
        Collections.sort(result, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

        // 4. Return at most maxLimit items
        if (result.size() > maxLimit) {
            return new ArrayList<>(result.subList(0, maxLimit));
        }
        return result;
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM Token registered: " + token);
        
        // Cache token in SessionManager
        SessionManager sessionManager = new SessionManager(this);
        sessionManager.getUserDetails(); // Make sure session references are initialized

        // Save token to Firestore if user is authenticated
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("users")
                    .document(currentUser.getUid())
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token updated in database"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update FCM token in database", e));
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "FCM message received from: " + remoteMessage.getFrom());

        // Check if message contains data payload
        Map<String, String> data = remoteMessage.getData();
        String title = "Notification";
        String message = "";
        String type = "text";
        String senderId = "";
        String senderName = "";
        String senderRole = "";

        if (!data.isEmpty()) {
            title = data.get("title");
            message = data.get("message");
            type = data.get("type");
            senderId = data.get("senderId");
            senderName = data.get("senderName");
            senderRole = data.get("senderRole");
        }

        // Also check if message contains notification payload
        if (remoteMessage.getNotification() != null) {
            if (title == null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (message == null) {
                message = remoteMessage.getNotification().getBody();
            }
        }

        if (title != null && message != null) {
            showNotification(title, message, type, senderId, senderName, senderRole);
        }
    }

    private void showNotification(String title, String message, String type, 
                                  String senderId, String senderName, String senderRole) {
        createNotificationChannel();

        boolean isSosEmergency = "emergency".equalsIgnoreCase(type) || "SOS".equalsIgnoreCase(title) || "sos".equalsIgnoreCase(type);
        Intent intent;

        SessionManager sessionManager = new SessionManager(this);
        String userRole = sessionManager.getUserRole();

        if (isSosEmergency) {
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
            intent.putExtra("patientId", senderId);
            intent.putExtra("patientName", senderName);
            intent.putExtra("message", message);
        } else if (senderId != null && !senderId.isEmpty()) {
            intent = new Intent(this, ChatActivity.class);
            intent.putExtra("receiverId", senderId);
            intent.putExtra("receiverName", senderName);
            intent.putExtra("receiverRole", senderRole);
            intent.putExtra("isEmergency", false);
        } else {
            intent = new Intent(this, DashboardActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (senderId != null ? senderId.hashCode() : (int) System.currentTimeMillis()),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        int icon = isSosEmergency ? android.R.drawable.stat_notify_error : android.R.drawable.stat_notify_chat;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_FCM)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(isSosEmergency ? NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_HIGH)
                .setCategory(isSosEmergency ? NotificationCompat.CATEGORY_ALARM : NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_LIGHTS)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (isSosEmergency) {
            builder.setVibrate(new long[]{1000, 1000, 1000, 1000});
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((senderId != null ? senderId.hashCode() : (int) System.currentTimeMillis()), builder.build());
        }

        // Cache in memory for instant read-only voice assistant access
        recordNotification(title, message, type, System.currentTimeMillis());

        // Trigger per-user audio sound playback and module-specific TTS
        String msgId = (senderId != null && !senderId.isEmpty() ? senderId : "fcm") + "_" + (message != null ? message.hashCode() : System.currentTimeMillis());
        CaregiverSoundManager.handleIncomingMessage(this, msgId, senderId, message, type, senderName, senderRole);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_FCM,
                    "FCM Default Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Firebase Cloud Messaging events notification channel");
            channel.setSound(null, null);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
