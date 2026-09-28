package com.kannada.speechassistant;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Routing gateway for the Dashboard.
 * Reads the user session role and navigates to the correct specialized dashboard.
 * If the role is missing locally, it fetches the role from Firestore.
 */
public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SessionManager sessionManager = new SessionManager(this);
        
        // Security Check: Verify user is logged in
        if (!sessionManager.isLoggedIn()) {
            Log.d(TAG, "User not logged in. Redirecting to LoginActivity.");
            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            RoleManager.fetchUserRole(uid, new RoleManager.RoleCallback() {
                @Override
                public void onRoleRetrieved(String fetchedRole) {
                    sessionManager.createLoginSession(
                            uid,
                            FirebaseAuth.getInstance().getCurrentUser().getEmail(),
                            fetchedRole
                    );
                    RoleManager.navigateToDashboard(DashboardActivity.this, fetchedRole);
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Failed to fetch user role on routing.", e);
                    FirebaseAuth.getInstance().signOut();
                    sessionManager.logoutUser();
                    Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
        } else {
            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
