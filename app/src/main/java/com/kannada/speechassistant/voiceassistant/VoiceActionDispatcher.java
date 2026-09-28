package com.kannada.speechassistant.voiceassistant;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.kannada.speechassistant.AdminDashboardActivity;
import com.kannada.speechassistant.BlindUserDashboardActivity;
import com.kannada.speechassistant.ChatActivity;
import com.kannada.speechassistant.EmergencyActivity;
import com.kannada.speechassistant.ProfileActivity;
import com.kannada.speechassistant.R;

/**
 * Dispatches resolved voice commands to existing Activities, Fragments, and UI widgets.
 * Strictly invokes existing functionality without creating replacement screens or duplicated logic.
 */
public class VoiceActionDispatcher {

    private static final String TAG = "VoiceActionDispatcher";

    public interface DispatchCallback {
        void onSuccess(@NonNull String spokenResponse);
        void onUnauthorized(@NonNull String spokenResponse);
        void onUnrecognized(@NonNull String spokenResponse);
    }

    /**
     * Executes the resolved voice command against the currently active Activity,
     * enforcing role-based access control based on the current user role.
     */
    public static void executeCommand(@NonNull Activity activity,
                                      @NonNull VoiceCommand command,
                                      @Nullable String userRole,
                                      @Nullable DispatchCallback callback) {
        String cmdId = command.getCommandId();
        Log.i(TAG, "Dispatching command: " + cmdId + " for role: " + userRole + " on Activity: " + activity.getClass().getSimpleName());

        // Role authorization check
        if (!command.isRoleAuthorized(userRole)) {
            Log.w(TAG, "Command " + cmdId + " unauthorized for role: " + userRole);
            activity.runOnUiThread(() -> {
                if (callback != null) {
                    callback.onUnauthorized(VoiceCommandConstants.RESPONSE_ROLE_UNAUTHORIZED);
                }
            });
            return;
        }

        activity.runOnUiThread(() -> {
            boolean handled = false;
            String spokenResponse = "";

            switch (cmdId) {
                case VoiceCommandConstants.CMD_OPEN_APP:
                    handled = true;
                    spokenResponse = VoiceLanguageConfig.getAppAlreadyOpenResponse(VoiceLanguageConfig.getAppLanguageCode(activity));
                    break;

                case VoiceCommandConstants.CMD_OPEN_HOME:
                    handled = handleOpenHome(activity);
                    spokenResponse = VoiceCommandConstants.RESPONSE_OPEN_HOME;
                    break;

                case VoiceCommandConstants.CMD_OPEN_PROFILE:
                    handled = handleOpenProfile(activity);
                    spokenResponse = VoiceCommandConstants.RESPONSE_OPEN_PROFILE;
                    break;

                case VoiceCommandConstants.CMD_OPEN_SETTINGS:
                    handled = handleOpenSettings(activity);
                    spokenResponse = VoiceCommandConstants.RESPONSE_OPEN_SETTINGS;
                    break;

                case VoiceCommandConstants.CMD_OPEN_MESSAGES:
                    handled = handleOpenMessages(activity);
                    spokenResponse = VoiceCommandConstants.RESPONSE_OPEN_MESSAGES;
                    break;

                case VoiceCommandConstants.CMD_OPEN_COMMUNICATION:
                    handled = handleOpenMessages(activity);
                    spokenResponse = VoiceCommandConstants.RESPONSE_OPEN_COMMUNICATION;
                    break;

                case VoiceCommandConstants.CMD_SEND_MESSAGE:
                    handled = true;
                    spokenResponse = VoiceCommandConstants.PROMPT_SAY_MESSAGE;
                    break;

                case VoiceCommandConstants.CMD_READ_MESSAGES:
                    handled = true;
                    spokenResponse = VoiceCommandConstants.RESPONSE_NO_NEW_MESSAGES;
                    break;

                case VoiceCommandConstants.CMD_MESSAGE_COUNT:
                    handled = true;
                    spokenResponse = VoiceCommandConstants.RESPONSE_NO_NEW_MESSAGES;
                    break;

                case VoiceCommandConstants.CMD_REPEAT_MESSAGE:
                    handled = true;
                    spokenResponse = VoiceCommandConstants.RESPONSE_NO_MESSAGE_TO_REPEAT;
                    break;

                case VoiceCommandConstants.CMD_READ_NOTIFICATIONS:
                    handled = true;
                    spokenResponse = VoiceCommandConstants.RESPONSE_NO_NOTIFICATIONS;
                    break;

                case VoiceCommandConstants.CMD_GO_BACK:
                    if (AppVoiceAssistant.isVoiceCallActive(activity)) {
                        handled = true;
                        spokenResponse = VoiceCommandConstants.RESPONSE_CALL_ACTIVE;
                        break;
                    }
                    boolean canBack = canNavigateBack(activity);
                    if (canBack) {
                        handled = handleGoBack(activity);
                        spokenResponse = VoiceCommandConstants.RESPONSE_GOING_BACK;
                    } else {
                        handled = true;
                        spokenResponse = VoiceCommandConstants.RESPONSE_ALREADY_FIRST_SCREEN;
                    }
                    break;

                case VoiceCommandConstants.CMD_OPEN_CAREGIVER:
                case VoiceCommandConstants.CMD_OPEN_CAREGIVER_CONNECTION:
                    handled = handleOpenCaregiver(activity);
                    spokenResponse = VoiceCommandConstants.RESPONSE_OPEN_CAREGIVER;
                    break;

                case VoiceCommandConstants.CMD_OPEN_VOICE_RECORDER:
                    handled = handleOpenVoiceRecorder(activity);
                    spokenResponse = VoiceCommandConstants.RESPONSE_OPEN_VOICE_RECORDER;
                    break;

                case VoiceCommandConstants.CMD_OPEN_VOICE_MESSAGE:
                    handled = handleOpenVoiceRecorder(activity);
                    spokenResponse = VoiceCommandConstants.RESPONSE_OPEN_VOICE_MESSAGE;
                    break;

                case VoiceCommandConstants.CMD_OPEN_VOICE_CALL:
                    if (AppVoiceAssistant.isVoiceCallActive(activity)) {
                        handled = true;
                        spokenResponse = "A call is already active.";
                        break;
                    }
                    if (activity instanceof BlindUserDashboardActivity) {
                        handled = handleOpenVoiceCall(activity);
                        spokenResponse = VoiceCommandConstants.RESPONSE_OPEN_VOICE_CALL;
                        break;
                    }
                    handled = handleOpenVoiceCall(activity);
                    spokenResponse = VoiceCommandConstants.RESPONSE_OPEN_VOICE_CALL;
                    break;

                case VoiceCommandConstants.CMD_OPEN_EMERGENCY:
                    handled = handleOpenEmergency(activity);
                    spokenResponse = VoiceCommandConstants.RESPONSE_OPEN_EMERGENCY;
                    break;

                case VoiceCommandConstants.CMD_OPEN_EMERGENCY_ALERT:
                    handled = handleOpenEmergency(activity);
                    spokenResponse = VoiceCommandConstants.RESPONSE_OPEN_EMERGENCY_ALERT;
                    break;

                case VoiceCommandConstants.CMD_START_LISTENING:
                    handled = handleStartListening(activity);
                    spokenResponse = VoiceCommandConstants.RESPONSE_START_LISTENING;
                    break;

                case VoiceCommandConstants.CMD_STOP_LISTENING:
                    handled = handleStopListening(activity);
                    spokenResponse = VoiceCommandConstants.RESPONSE_STOP_LISTENING;
                    break;

                case VoiceCommandConstants.CMD_END_CALL:
                    if (!AppVoiceAssistant.isVoiceCallActive(activity)) {
                        handled = true;
                        spokenResponse = "No active call.";
                        break;
                    }
                    handled = handleEndCall(activity);
                    spokenResponse = VoiceCommandConstants.RESPONSE_END_CALL;
                    break;

                case VoiceCommandConstants.CMD_ACCEPT_CALL:
                    handled = handleAcceptCall(activity);
                    spokenResponse = VoiceCommandConstants.RESPONSE_ACCEPT_CALL;
                    break;

                default:
                    handled = false;
                    spokenResponse = VoiceCommandConstants.RESPONSE_UNKNOWN;
                    break;
            }

            if (handled && callback != null) {
                callback.onSuccess(spokenResponse);
            } else if (callback != null) {
                callback.onUnrecognized(VoiceCommandConstants.RESPONSE_UNKNOWN);
            }
        });
    }

    public static void executeCommand(@NonNull Activity activity,
                                      @NonNull VoiceCommand command,
                                      @Nullable DispatchCallback callback) {
        executeCommand(activity, command, null, callback);
    }

    private static boolean handleOpenHome(@NonNull Activity activity) {
        if (activity instanceof ChatActivity) {
            Intent intent = new Intent(activity, BlindUserDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("EXTRA_TARGET_TAB", "home");
            activity.startActivity(intent);
            activity.finish();
            return true;
        }

        BottomNavigationView bnv = findActiveBottomNav(activity);
        if (bnv != null) {
            bnv.setSelectedItemId(R.id.nav_home);
            View layoutHome = activity.findViewById(R.id.layoutHome);
            View layoutProfile = activity.findViewById(R.id.layoutProfile);
            View layoutCaregiver = activity.findViewById(R.id.layoutCaregiver);
            if (layoutProfile != null) layoutProfile.setVisibility(View.GONE);
            if (layoutCaregiver != null) layoutCaregiver.setVisibility(View.GONE);
            if (layoutHome != null) layoutHome.setVisibility(View.VISIBLE);
            return true;
        }

        if (activity instanceof AdminDashboardActivity) {
            ViewPager2 viewPager = activity.findViewById(R.id.viewPager);
            if (viewPager != null) {
                viewPager.setCurrentItem(0, true);
                return true;
            }
        }

        if (activity instanceof ProfileActivity) {
            activity.finish();
            return true;
        }

        return false;
    }

    private static boolean handleOpenProfile(@NonNull Activity activity) {
        if (activity instanceof ChatActivity) {
            Intent intent = new Intent(activity, BlindUserDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("EXTRA_TARGET_TAB", "profile");
            activity.startActivity(intent);
            activity.finish();
            return true;
        }

        BottomNavigationView bnv = findActiveBottomNav(activity);
        if (bnv != null) {
            bnv.setSelectedItemId(R.id.nav_profile);
            View layoutProfile = activity.findViewById(R.id.layoutProfile);
            View layoutHome = activity.findViewById(R.id.layoutHome);
            View layoutCaregiver = activity.findViewById(R.id.layoutCaregiver);
            if (layoutHome != null) layoutHome.setVisibility(View.GONE);
            if (layoutCaregiver != null) layoutCaregiver.setVisibility(View.GONE);
            if (layoutProfile != null) layoutProfile.setVisibility(View.VISIBLE);
            return true;
        }

        if (activity instanceof ProfileActivity) {
            return true;
        }

        return false;
    }

    private static boolean handleOpenSettings(@NonNull Activity activity) {
        View btnSettings = activity.findViewById(R.id.btnBellNotificationSettings);
        if (btnSettings != null) {
            btnSettings.performClick();
            return true;
        }
        return false;
    }

    private static boolean handleOpenMessages(@NonNull Activity activity) {
        if (activity instanceof BlindUserDashboardActivity) {
            ((BlindUserDashboardActivity) activity).openInteractiveChat();
            return true;
        }

        if (activity instanceof AdminDashboardActivity) {
            ViewPager2 viewPager = activity.findViewById(R.id.viewPager);
            if (viewPager != null) {
                viewPager.setCurrentItem(3, true); // Tab 3 is Communication
                return true;
            }
        }

        BottomNavigationView bnv = findActiveBottomNav(activity);
        if (bnv != null) {
            bnv.setSelectedItemId(R.id.nav_caregiver);
            View btnOpenChat = activity.findViewById(R.id.btnOpenFullChat);
            if (btnOpenChat != null && btnOpenChat.getVisibility() == View.VISIBLE) {
                btnOpenChat.performClick();
            }
            return true;
        }

        return false;
    }

    private static boolean handleOpenCaregiver(@NonNull Activity activity) {
        if (activity instanceof ChatActivity) {
            Intent intent = new Intent(activity, BlindUserDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("EXTRA_TARGET_TAB", "caregiver");
            activity.startActivity(intent);
            activity.finish();
            return true;
        }

        if (activity instanceof BlindUserDashboardActivity) {
            ((BlindUserDashboardActivity) activity).openCaregiverTab();
            return true;
        }

        if (activity instanceof AdminDashboardActivity) {
            ViewPager2 viewPager = activity.findViewById(R.id.viewPager);
            if (viewPager != null) {
                viewPager.setCurrentItem(2, true); // Tab 2 is Connections
                return true;
            }
        }

        BottomNavigationView bnv = findActiveBottomNav(activity);
        if (bnv != null) {
            bnv.setSelectedItemId(R.id.nav_caregiver);
            View layoutHome = activity.findViewById(R.id.layoutHome);
            View layoutProfile = activity.findViewById(R.id.layoutProfile);
            View layoutCaregiver = activity.findViewById(R.id.layoutCaregiver);
            View cardEmergency = activity.findViewById(R.id.cardEmergency);
            if (layoutHome != null) layoutHome.setVisibility(View.GONE);
            if (layoutProfile != null) layoutProfile.setVisibility(View.GONE);
            if (cardEmergency != null) cardEmergency.setVisibility(View.GONE);
            if (layoutCaregiver != null) layoutCaregiver.setVisibility(View.VISIBLE);
            return true;
        }

        return false;
    }

    private static boolean handleOpenVoiceRecorder(@NonNull Activity activity) {
        View cardVoiceMessage = activity.findViewById(R.id.cardVoiceMessage);
        if (cardVoiceMessage != null) {
            BottomNavigationView bnv = findActiveBottomNav(activity);
            if (bnv != null) {
                bnv.setSelectedItemId(R.id.nav_home);
            }
            cardVoiceMessage.requestFocus();
            return true;
        }
        return false;
    }

    private static boolean handleOpenVoiceCall(@NonNull Activity activity) {
        if (activity instanceof BlindUserDashboardActivity) {
            ((BlindUserDashboardActivity) activity).startCaregiverVoiceCall();
            return true;
        }
        View btnCall = activity.findViewById(R.id.btnVoiceCallCaregiver);
        if (btnCall != null) {
            btnCall.performClick();
            return true;
        }
        return false;
    }

    private static boolean handleOpenEmergency(@NonNull Activity activity) {
        if (activity instanceof BlindUserDashboardActivity) {
            ((BlindUserDashboardActivity) activity).sendDirectEmergencyToCaregiver();
            return true;
        }

        // 1. Check if the current activity has an emergency button/card wired up
        View btnEmergency = activity.findViewById(R.id.cardEmergency);
        if (btnEmergency == null) {
            btnEmergency = activity.findViewById(R.id.btnSOS);
        }
        if (btnEmergency != null) {
            btnEmergency.performClick();
            return true;
        }

        // 2. Caregiver console tab navigation
        if (activity instanceof AdminDashboardActivity) {
            ViewPager2 viewPager = activity.findViewById(R.id.viewPager);
            if (viewPager != null) {
                viewPager.setCurrentItem(1, true); // Tab 1 is Emergencies in Caregiver Console
                return true;
            }
        }

        // 3. If already on EmergencyActivity, trigger SOS button directly
        if (activity instanceof EmergencyActivity) {
            View btnSos = activity.findViewById(R.id.btnSOS);
            if (btnSos != null) {
                btnSos.performClick();
            }
            return true;
        }

        // 4. Otherwise launch EmergencyActivity with auto-trigger SOS
        try {
            Intent intent = new Intent(activity, EmergencyActivity.class);
            intent.putExtra("EXTRA_AUTO_TRIGGER_SOS", true);
            activity.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch EmergencyActivity", e);
            return false;
        }
    }

    private static boolean handleStartListening(@NonNull Activity activity) {
        return true;
    }

    private static boolean handleStopListening(@NonNull Activity activity) {
        try {
            AppVoiceAssistant.getInstance(activity).stopListening();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping voice assistant listening", e);
        }
        return true;
    }

    private static boolean handleEndCall(@NonNull Activity activity) {
        try {
            com.kannada.speechassistant.VoiceCallManager callManager =
                    com.kannada.speechassistant.VoiceCallManager.getInstance(activity);
            if (callManager.isCallActive()) {
                callManager.endCall();
                return true;
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error ending call via VoiceCallManager", e);
        }
        return false;
    }

    private static boolean handleAcceptCall(@NonNull Activity activity) {
        try {
            com.kannada.speechassistant.IncomingCallActivity inc =
                    com.kannada.speechassistant.IncomingCallActivity.getActiveInstance();
            if (inc != null && !inc.isFinishing() && !inc.isDestroyed()) {
                inc.acceptCallFromVoice();
                return true;
            }
            if (activity instanceof com.kannada.speechassistant.IncomingCallActivity) {
                ((com.kannada.speechassistant.IncomingCallActivity) activity).acceptCallFromVoice();
                return true;
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error accepting call from voice via IncomingCallActivity", e);
        }
        return false;
    }

    @Nullable
    private static BottomNavigationView findActiveBottomNav(@NonNull Activity activity) {
        BottomNavigationView bnv = activity.findViewById(R.id.bottomNavBlindUser);
        if (bnv != null) return bnv;

        bnv = activity.findViewById(R.id.bottomNavMuteUser);
        if (bnv != null) return bnv;

        bnv = activity.findViewById(R.id.bottomNavSpeechImpaired);
        return bnv;
    }

    public static boolean canNavigateBack(@NonNull Activity activity) {
        Activity targetActivity = activity;
        try {
            Activity fg = WakeWordManager.getInstance(activity.getApplicationContext()).getCurrentActivity();
            if (fg != null && !fg.isFinishing() && !fg.isDestroyed()) {
                targetActivity = fg;
            }
        } catch (Throwable ignored) {}

        if (targetActivity.isFinishing() || targetActivity.isDestroyed()) {
            return false;
        }

        if (targetActivity instanceof ChatActivity || targetActivity instanceof ProfileActivity || targetActivity instanceof EmergencyActivity) {
            return true;
        }

        if (targetActivity instanceof BlindUserDashboardActivity) {
            if (((BlindUserDashboardActivity) targetActivity).canNavigateBack()) {
                return true;
            }
            return !targetActivity.isTaskRoot();
        }

        BottomNavigationView bnv = findActiveBottomNav(targetActivity);
        if (bnv != null && bnv.getSelectedItemId() != R.id.nav_home) {
            return true;
        }

        if (targetActivity instanceof AdminDashboardActivity) {
            ViewPager2 viewPager = targetActivity.findViewById(R.id.viewPager);
            if (viewPager != null && viewPager.getCurrentItem() > 0) {
                return true;
            }
        }

        return !targetActivity.isTaskRoot();
    }

    public static boolean handleGoBack(@NonNull Activity activity) {
        Activity targetActivity = activity;
        try {
            Activity fg = WakeWordManager.getInstance(activity.getApplicationContext()).getCurrentActivity();
            if (fg != null && !fg.isFinishing() && !fg.isDestroyed()) {
                targetActivity = fg;
            }
        } catch (Throwable ignored) {}

        if (targetActivity.isFinishing() || targetActivity.isDestroyed()) {
            return false;
        }

        if (targetActivity instanceof ChatActivity || targetActivity instanceof ProfileActivity || targetActivity instanceof EmergencyActivity) {
            targetActivity.finish();
            return true;
        }

        if (targetActivity instanceof BlindUserDashboardActivity) {
            if (((BlindUserDashboardActivity) targetActivity).canNavigateBack()) {
                return ((BlindUserDashboardActivity) targetActivity).navigateBack();
            }
            if (!targetActivity.isTaskRoot()) {
                targetActivity.finish();
                return true;
            }
            return false;
        }

        BottomNavigationView bnv = findActiveBottomNav(targetActivity);
        if (bnv != null && bnv.getSelectedItemId() != R.id.nav_home) {
            bnv.setSelectedItemId(R.id.nav_home);
            View layoutHome = targetActivity.findViewById(R.id.layoutHome);
            View layoutProfile = targetActivity.findViewById(R.id.layoutProfile);
            View layoutCaregiver = targetActivity.findViewById(R.id.layoutCaregiver);
            if (layoutProfile != null) layoutProfile.setVisibility(View.GONE);
            if (layoutCaregiver != null) layoutCaregiver.setVisibility(View.GONE);
            if (layoutHome != null) layoutHome.setVisibility(View.VISIBLE);
            return true;
        }

        if (targetActivity instanceof AdminDashboardActivity) {
            ViewPager2 viewPager = targetActivity.findViewById(R.id.viewPager);
            if (viewPager != null && viewPager.getCurrentItem() > 0) {
                viewPager.setCurrentItem(0, true);
                return true;
            }
        }

        if (!targetActivity.isTaskRoot()) {
            if (targetActivity instanceof androidx.activity.ComponentActivity) {
                ((androidx.activity.ComponentActivity) targetActivity).getOnBackPressedDispatcher().onBackPressed();
            } else {
                targetActivity.onBackPressed();
            }
            return true;
        }

        return false;
    }
}
