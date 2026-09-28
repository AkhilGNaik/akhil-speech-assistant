package com.kannada.speechassistant;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Security Guard class to enforce strict role-based access security.
 * Always fetches the fresh role from Firestore, bypasses local caches to avoid tampering,
 * and forces logout + redirect to LoginActivity if unauthorized access or mismatch is detected.
 */
public class SecurityGuard {

    private static final String TAG = "SecurityGuard";

    public interface SecurityCheckCallback {
        void onAuthorized();
    }

    /**
     * Strictly verifies the user's role by fetching it directly from Firestore.
     * If the role does not match or user is not logged in, logs them out and redirects to LoginActivity.
     */
    public static void verifyRole(@NonNull final Activity activity, @NonNull final String expectedRole, final SecurityCheckCallback callback) {
        verifyRoles(activity, new String[]{expectedRole}, callback);
    }

    /**
     * Strictly verifies the user's role against a list of authorized roles by fetching it directly from Firestore.
     */
    public static void verifyRoles(@NonNull final Activity activity, @NonNull final String[] allowedRoles, final SecurityCheckCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No user signed in. Force logout and redirect.");
            forceLogoutAndRedirect(activity, "Please log in to continue.");
            return;
        }

        // Fetch user role directly from Firestore to ensure no local cache tampering or bypass
        RoleManager.fetchUserRole(currentUser.getUid(), new RoleManager.RoleCallback() {
            @Override
            public void onRoleRetrieved(String fetchedRole) {
                if (fetchedRole == null) {
                    Log.e(TAG, "Role retrieval returned null profile.");
                    forceLogoutAndRedirect(activity, "Access Denied: Unauthorized role.");
                    return;
                }

                boolean authorized = false;
                for (String allowed : allowedRoles) {
                    if (fetchedRole.equals(allowed)) {
                        authorized = true;
                        break;
                    }
                    if ((RoleManager.ROLE_DEAF_USER.equals(allowed) || RoleManager.ROLE_SPEECH_IMPAIRED.equals(allowed)) &&
                        (RoleManager.ROLE_DEAF_USER.equals(fetchedRole) || RoleManager.ROLE_SPEECH_IMPAIRED.equals(fetchedRole) || "Mute, Deaf & Blind User".equals(fetchedRole) || "Mute, Deaf & Blind Users".equals(fetchedRole))) {
                        authorized = true;
                        break;
                    }
                }

                if (!authorized) {
                    Log.e(TAG, "Role mismatch! Fetched: " + fetchedRole);
                    forceLogoutAndRedirect(activity, "Access Denied: Unauthorized role.");
                } else {
                    // Update session manager cache to keep it in sync
                    SessionManager sessionManager = new SessionManager(activity);
                    sessionManager.createLoginSession(currentUser.getUid(), currentUser.getEmail(), fetchedRole);
                    if (callback != null) {
                        callback.onAuthorized();
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to fetch user role for security check: " + e.getMessage());
                forceLogoutAndRedirect(activity, "Authentication error. Please log in again.");
            }
        });
    }

    private static void forceLogoutAndRedirect(@NonNull Activity activity, String message) {
        FirebaseAuth.getInstance().signOut();
        SessionManager sessionManager = new SessionManager(activity);
        sessionManager.logoutUser();
        
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        RoleManager.redirectToLogin(activity);
        activity.finish();
    }
}
