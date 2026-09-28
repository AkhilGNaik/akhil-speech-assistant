package com.kannada.speechassistant;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kannada.speechassistant.voiceassistant.BackgroundVoiceLaunchService;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Global Application class for the Kannada Speech Assistant.
 *
 * Automatically initializes and maintains the BackgroundVoiceLaunchService
 * so that hands-free app opening voice commands ("Hey Assistant, open speech assistant app")
 * work whether the app is in the background, minimized, or closed.
 */
public class SpeechAssistantApplication extends Application {

    private static final String TAG = "SpeechAssistantApp";
    private static SpeechAssistantApplication instance;
    private final AtomicInteger activeActivities = new AtomicInteger(0);

    public static SpeechAssistantApplication getInstance() {
        return instance;
    }

    public boolean isAppInForeground() {
        return activeActivities.get() > 0;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.i(TAG, "SpeechAssistantApplication initialized.");

        registerLifecycleTracker();
    }

    private void registerLifecycleTracker() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                int count = activeActivities.incrementAndGet();
                Log.d(TAG, "Activity started: " + activity.getClass().getSimpleName() + " (Active: " + count + ")");
                BackgroundVoiceLaunchService.onAppForegrounded(activity);
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                BackgroundVoiceLaunchService.onAppForegrounded(activity);
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {}

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                int count = activeActivities.decrementAndGet();
                if (count < 0) {
                    activeActivities.set(0);
                    count = 0;
                }
                Log.d(TAG, "Activity stopped: " + activity.getClass().getSimpleName() + " (Active: " + count + ")");
                if (count == 0) {
                    Log.i(TAG, "All activities stopped. App is now completely in background/closed.");
                    if (androidx.core.content.ContextCompat.checkSelfPermission(activity, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        BackgroundVoiceLaunchService.onAppBackgrounded(activity.getApplicationContext());
                    }
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (activeActivities.get() <= 0) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(activity, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        BackgroundVoiceLaunchService.onAppBackgrounded(activity.getApplicationContext());
                    }
                }
            }
        });
    }

    /**
     * Starts the persistent voice launch service so the user can open the app
     * with voice commands even if the app is closed or outside the app.
     */
    public void startBackgroundVoiceLauncher() {
        try {
            Intent intent = new Intent(this, BackgroundVoiceLaunchService.class);
            intent.setAction(BackgroundVoiceLaunchService.ACTION_START);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            Log.i(TAG, "BackgroundVoiceLaunchService successfully initiated from Application class.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start BackgroundVoiceLaunchService from Application: " + e.getMessage(), e);
        }
    }
}
