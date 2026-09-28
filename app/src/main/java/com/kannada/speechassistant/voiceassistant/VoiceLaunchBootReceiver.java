package com.kannada.speechassistant.voiceassistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.kannada.speechassistant.FirestoreRealtimeService;
import com.kannada.speechassistant.RoleManager;
import com.kannada.speechassistant.SessionManager;

/**
 * Ensures BackgroundVoiceLaunchService and FirestoreRealtimeService restart
 * on device boot for Blind Users so voice launch remains active without manual intervention.
 */
public class VoiceLaunchBootReceiver extends BroadcastReceiver {

    private static final String TAG = "VoiceLaunchBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        Log.i(TAG, "Received broadcast action: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            SessionManager sessionManager = new SessionManager(context);
            if (sessionManager.isLoggedIn()) {
                // 1. Restart Central Firestore Service
                try {
                    Intent fsIntent = new Intent(context, FirestoreRealtimeService.class);
                    context.startService(fsIntent);
                } catch (Exception e) {
                    Log.w(TAG, "Error starting FirestoreRealtimeService on boot: " + e.getMessage());
                }

                // 2. Restart Background Voice Launch Service if user is a Blind User
                if (RoleManager.ROLE_BLIND_USER.equals(sessionManager.getUserRole())) {
                    try {
                        Intent launchIntent = new Intent(context, BackgroundVoiceLaunchService.class);
                        launchIntent.setAction(BackgroundVoiceLaunchService.ACTION_START);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(launchIntent);
                        } else {
                            context.startService(launchIntent);
                        }
                        Log.i(TAG, "BackgroundVoiceLaunchService successfully started on device boot.");
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting BackgroundVoiceLaunchService on boot: " + e.getMessage(), e);
                    }
                }
            }
        }
    }
}
