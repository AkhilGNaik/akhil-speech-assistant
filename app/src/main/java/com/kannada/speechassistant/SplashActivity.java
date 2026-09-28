package com.kannada.speechassistant;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Splash Screen Activity for "Speech Generation and Communication Assistant for Disabled People".
 * Handles:
 * 1. App logo and dynamic layout load.
 * 2. Visual fade-in entry animation for branding.
 * 3. Checking Firebase Authentication session state.
 * 4. Navigating to DashboardActivity (if logged in) or LoginActivity (if not logged in).
 */
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY = 2500; // 2.5 seconds minimum delay for branding & animations

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Reference the container view to run the fade-in animation
        LinearLayout brandContainer = findViewById(R.id.brandContainer);
        if (brandContainer != null) {
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            brandContainer.startAnimation(fadeIn);
        }

        // Post navigation check with delay to allow logo animation to complete
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                checkAuthAndNavigate();
            }
        }, SPLASH_DELAY);
    }

    /**
     * Checks user's authentication status with Firebase.
     * Navigates to the corresponding dashboard if the user session is active, otherwise redirects to Login.
     */
    private void checkAuthAndNavigate() {
        boolean isLoggedIn = false;
        final FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final FirebaseUser currentUser = mAuth.getCurrentUser();

        try {
            // Check if Firebase is initialized in the project
            if (!FirebaseApp.getApps(this).isEmpty()) {
                if (currentUser != null) {
                    isLoggedIn = true;
                    Log.d(TAG, "Firebase user authenticated: " + currentUser.getUid());
                } else {
                    Log.d(TAG, "No Firebase user authenticated.");
                }
            } else {
                Log.w(TAG, "FirebaseApp is not initialized. Defaulting to Login Screen.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking Firebase Auth status. Defaulting to Login Screen.", e);
        }

        if (isLoggedIn && currentUser != null) {
            final SessionManager sessionManager = new SessionManager(SplashActivity.this);
            Log.i(TAG, "User session active. Verifying profile against Firestore for UID: " + currentUser.getUid());

            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            com.google.firebase.firestore.DocumentSnapshot doc = task.getResult();
                            String fetchedRole = doc.getString("role");
                            String fetchedLang = doc.getString("language");

                            if (fetchedRole != null && !fetchedRole.trim().isEmpty()) {
                                String role = fetchedRole.trim();
                                if ("Speech-Impaired User".equals(role) || "Mute, Deaf & Blind User".equals(role) || "Mute, Deaf & Blind Users".equals(role)) {
                                    role = RoleManager.ROLE_DEAF_USER;
                                }

                                if (RoleManager.getDashboardClassForRole(role) != null) {
                                    String language = (fetchedLang != null) ? LanguageManager.normalizeLanguageCode(fetchedLang) : LanguageManager.DEFAULT_LANGUAGE;
                                    sessionManager.createLoginSession(currentUser.getUid(), currentUser.getEmail(), role, language);
                                    RoleManager.navigateToDashboard(SplashActivity.this, role);
                                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                    finish();
                                    return;
                                }
                            }
                        }

                        // Fallback: If network failed or Firestore document could not be fetched, use local cached session!
                        String cachedRole = sessionManager.getUserRole();
                        if (cachedRole != null && RoleManager.getDashboardClassForRole(cachedRole) != null) {
                            Log.i(TAG, "Using cached role on offline/error: " + cachedRole);
                            RoleManager.navigateToDashboard(SplashActivity.this, cachedRole);
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            finish();
                            return;
                        }

                        Log.w(TAG, "Session profile missing or invalid. Signing out and redirecting to Login.");
                        FirebaseAuth.getInstance().signOut();
                        sessionManager.logoutUser();
                        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    });
        } else {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            Log.i(TAG, "Navigating to Login Screen.");
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }
    }
}
