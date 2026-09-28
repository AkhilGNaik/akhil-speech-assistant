package com.kannada.speechassistant;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Centrally manages Role-Based Access Control (RBAC) in the Kannada Speech Assistant.
 * Handles roles:
 * - Mute User
 * - Deaf User
 * - Physically Disabled User
 * - Blind User
 * - Admin/Caregiver
 *
 * Implements Firestore retrieval, navigation/redirection, and security verification.
 */
public class RoleManager {

    private static final String TAG = "RoleManager";

    // Standard Role Strings matching registration drop-down values
    public static final String ROLE_MUTE_USER = "Mute User";
    public static final String ROLE_DEAF_USER = "Deaf User";
    public static final String ROLE_SPEECH_IMPAIRED = "Speech-Impaired User";
    public static final String ROLE_PHYSICALLY_DISABLED = "Physically Disabled User";
    public static final String ROLE_BLIND_USER = "Blind User";
    public static final String ROLE_ADMIN_CAREGIVER = "Admin/Caregiver";

    public interface RoleCallback {
        void onRoleRetrieved(String role);
        void onError(Exception e);
    }

    /**
     * Retrieves the user's role from Firebase Firestore (collection "users").
     * Uses the user's unique authenticated UID.
     */
    public static void fetchUserRole(@NonNull String uid, @NonNull final RoleCallback callback) {
        if (uid.isEmpty()) {
            callback.onError(new IllegalArgumentException("UID cannot be empty."));
            return;
        }

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                String role = document.getString("role");
                                if (role != null && !role.trim().isEmpty()) {
                                    Log.d(TAG, "Role retrieved from Firestore for UID " + uid + ": " + role);
                                    callback.onRoleRetrieved(role.trim());
                                    return;
                                }
                            }
                        }

                        Log.w(TAG, "Firestore role retrieval failed or profile missing for UID: " + uid);
                        callback.onError(new Exception("User profile or role not found in Firestore."));
                    }
                });
    }

    /**
     * Resolves the target Activity class corresponding to the user's role.
     * Returns null if the role is missing or invalid.
     */
    public static Class<?> getDashboardClassForRole(String role) {
        if (role == null) {
            return null;
        }

        switch (role) {
            case ROLE_MUTE_USER:
                return MuteUserDashboardActivity.class;
            case ROLE_BLIND_USER:
                return BlindUserDashboardActivity.class;
            case ROLE_ADMIN_CAREGIVER:
                return AdminDashboardActivity.class;
            case ROLE_DEAF_USER:
            case ROLE_SPEECH_IMPAIRED:
            case "Mute, Deaf & Blind User":
            case "Mute, Deaf & Blind Users":
            case "Mute, Deaf & Blind":
                return SpeechImpairedDashboardActivity.class;
            default:
                return null;
        }
    }

    /**
     * Navigates to the corresponding dashboard and clears the back stack.
     */
    public static void navigateToDashboard(@NonNull Context context, String role) {
        Class<?> targetActivity = getDashboardClassForRole(role);
        if (targetActivity == null) {
            Log.e(TAG, "Navigating failed: Invalid or missing role '" + role + "'");
            Toast.makeText(context, "Unable to determine your user role. Please sign in again.", Toast.LENGTH_LONG).show();
            redirectToLogin(context);
            return;
        }

        Log.i(TAG, "Navigating to: " + targetActivity.getSimpleName() + " for role: " + role);
        Intent intent = new Intent(context, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    /**
     * Per-screen Security Check.
     * Checks if the user is authenticated and if their role matches the allowed roles.
     * Returns true if authorized, false otherwise (will redirect automatically).
     */
    public static boolean checkAccess(@NonNull Activity activity, String currentRole, @NonNull String... allowedRoles) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Security Check Failed: User not authenticated. Redirecting to Login.");
            Toast.makeText(activity, "Please log in to continue.", Toast.LENGTH_SHORT).show();
            redirectToLogin(activity);
            return false;
        }

        if (currentRole == null) {
            Log.w(TAG, "Security Check: Missing role. Redirecting to login.");
            redirectToLogin(activity);
            return false;
        }

        for (String allowedRole : allowedRoles) {
            if (currentRole.equals(allowedRole)) {
                return true; // Authorized
            }
            if ((ROLE_DEAF_USER.equals(allowedRole) || ROLE_SPEECH_IMPAIRED.equals(allowedRole)) &&
                (ROLE_DEAF_USER.equals(currentRole) || ROLE_SPEECH_IMPAIRED.equals(currentRole) || "Mute, Deaf & Blind User".equals(currentRole) || "Mute, Deaf & Blind Users".equals(currentRole))) {
                return true;
            }
        }

        Log.w(TAG, "Security Check Failed: Role '" + currentRole + "' unauthorized for this screen.");
        Toast.makeText(activity, "Access Denied: Unauthorized role.", Toast.LENGTH_LONG).show();
        
        // Redirect to the user's own dashboard
        navigateToDashboard(activity, currentRole);
        return false;
    }

    /**
     * Redirects to the login screen and clears the activity stack.
     */
    public static void redirectToLogin(@NonNull Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
