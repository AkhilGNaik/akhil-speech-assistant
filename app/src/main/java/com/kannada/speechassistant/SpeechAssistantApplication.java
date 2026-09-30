package com.kannada.speechassistant;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Global Application class for the Kannada Speech Assistant.
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
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {}

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
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }
}
