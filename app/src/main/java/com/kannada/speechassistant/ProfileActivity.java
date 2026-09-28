package com.kannada.speechassistant;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller class for the User Profile Activity.
 * Fetches profile metadata from Firebase Firestore, manages editable states, language changes, and handles sign-out flows.
 */
public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    // System Utilities
    private SessionManager sessionManager;
    private String currentUid;
    private boolean isEditMode = false;
    private String initialLanguageCode = LanguageManager.DEFAULT_LANGUAGE;

    // UI elements
    private MaterialButton btnBack;
    private TextView txtHeaderName;
    private TextView txtHeaderRole;
    
    private TextInputEditText editProfileName;
    private TextInputEditText editProfileEmail;
    private TextInputEditText editProfileRole;
    private AutoCompleteTextView editProfileLanguage;
    
    private TextInputEditText editCaregiverName;
    private TextInputEditText editCaregiverEmail;
    private TextInputEditText editCaregiverPhone;
    
    private MaterialButton btnEditSaveProfile;
    private MaterialButton btnProfileLogout;
    private FrameLayout loadingOverlay;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);

        // Security Check: Verify user login
        if (!sessionManager.isLoggedIn() || FirebaseAuth.getInstance().getCurrentUser() == null) {
            RoleManager.redirectToLogin(this);
            finish();
            return;
        }

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Bind layouts
        btnBack = findViewById(R.id.btnBack);
        txtHeaderName = findViewById(R.id.txtHeaderName);
        txtHeaderRole = findViewById(R.id.txtHeaderRole);
        editProfileName = findViewById(R.id.editProfileName);
        editProfileEmail = findViewById(R.id.editProfileEmail);
        editProfileRole = findViewById(R.id.editProfileRole);
        editProfileLanguage = findViewById(R.id.editProfileLanguage);
        editCaregiverName = findViewById(R.id.editCaregiverName);
        editCaregiverEmail = findViewById(R.id.editCaregiverEmail);
        editCaregiverPhone = findViewById(R.id.editCaregiverPhone);
        btnEditSaveProfile = findViewById(R.id.btnEditSaveProfile);
        btnProfileLogout = findViewById(R.id.btnProfileLogout);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        // Setup dropdown for language
        setupLanguageDropdown();

        // Bind events
        setupActions();

        // Load profile data from Firestore
        fetchUserProfile();
    }

    private void setupLanguageDropdown() {
        String[] languages = {"Kannada", "Hindi", "Malayalam"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                languages
        );
        editProfileLanguage.setAdapter(adapter);
    }

    /**
     * Set up clicks and actions listeners
     */
    private void setupActions() {
        btnBack.setOnClickListener(v -> finish());

        btnEditSaveProfile.setOnClickListener(v -> {
            if (isEditMode) {
                attemptSaveProfile();
            } else {
                toggleEditMode(true);
            }
        });

        btnProfileLogout.setOnClickListener(v -> performLogout());
    }

    /**
     * Retrieve user profile from Firestore
     */
    private void fetchUserProfile() {
        loadingOverlay.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance().collection("users").document(currentUid)
                .get()
                .addOnCompleteListener(task -> {
                    loadingOverlay.setVisibility(View.GONE);
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            String email = doc.getString("email");
                            String role = doc.getString("role");
                            String lang = doc.getString("language");
                            initialLanguageCode = LanguageManager.normalizeLanguageCode(lang);

                            String caregiverName = doc.getString("caregiverName");
                            String caregiverEmail = doc.getString("caregiverEmail");
                            String caregiverPhone = doc.getString("caregiverPhone");

                            // Set values to fields
                            txtHeaderName.setText(name != null ? name : "User Name");
                            if (RoleManager.ROLE_MUTE_USER.equals(role)) {
                                txtHeaderRole.setText("Role: Mute User");
                                editProfileRole.setText("Mute User");
                            } else if (RoleManager.ROLE_SPEECH_IMPAIRED.equals(role) || RoleManager.ROLE_DEAF_USER.equals(role)) {
                                txtHeaderRole.setText("Role: Deaf User");
                                editProfileRole.setText("Deaf User");
                            } else if (RoleManager.ROLE_BLIND_USER.equals(role)) {
                                txtHeaderRole.setText("Role: Blind User");
                                editProfileRole.setText("Blind User");
                            } else {
                                txtHeaderRole.setText(role != null ? "Role: " + role : "Role: Mute User");
                                editProfileRole.setText(role);
                            }
                            
                            editProfileName.setText(name);
                            editProfileEmail.setText(email);
                            
                            String langDisplayName = LanguageManager.getLanguageDisplayName(initialLanguageCode);
                            editProfileLanguage.setText(langDisplayName, false);

                            editCaregiverName.setText(caregiverName);
                            editCaregiverEmail.setText(caregiverEmail);
                            editCaregiverPhone.setText(caregiverPhone);
                        } else {
                            Toast.makeText(this, "Profile metadata does not exist in Firebase.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Database offline. Failed to sync profile.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Toggles text fields between enabled and disabled mode
     */
    private void toggleEditMode(boolean enable) {
        isEditMode = enable;
        btnEditSaveProfile.setText(enable ? getString(R.string.save_changes) : getString(R.string.edit_profile));
        
        editProfileName.setEnabled(enable);
        editProfileLanguage.setEnabled(enable);
        editCaregiverName.setEnabled(enable);
        editCaregiverEmail.setEnabled(enable);
        editCaregiverPhone.setEnabled(enable);

        if (enable) {
            editProfileName.requestFocus();
        }
    }

    /**
     * Perform validation check and submit modifications to Firebase Firestore
     */
    private void attemptSaveProfile() {
        final String nameStr = editProfileName.getText() != null ? editProfileName.getText().toString().trim() : "";
        final String selectedLangDisplay = editProfileLanguage.getText() != null ? editProfileLanguage.getText().toString().trim() : "Kannada";
        final String newLangCode = LanguageManager.getLanguageCodeFromDisplayName(selectedLangDisplay);

        final String caregiverNameStr = editCaregiverName.getText() != null ? editCaregiverName.getText().toString().trim() : "";
        final String caregiverEmailStr = editCaregiverEmail.getText() != null ? editCaregiverEmail.getText().toString().trim() : "";
        final String caregiverPhoneStr = editCaregiverPhone.getText() != null ? editCaregiverPhone.getText().toString().trim() : "";

        // Form Validation
        if (TextUtils.isEmpty(nameStr)) {
            editProfileName.setError("Name is required");
            editProfileName.requestFocus();
            return;
        }

        if (!TextUtils.isEmpty(caregiverEmailStr) && !Patterns.EMAIL_ADDRESS.matcher(caregiverEmailStr).matches()) {
            editCaregiverEmail.setError("Enter a valid email address");
            editCaregiverEmail.requestFocus();
            return;
        }

        loadingOverlay.setVisibility(View.VISIBLE);

        // Map updates
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", nameStr);
        updates.put("language", newLangCode);
        updates.put("caregiverName", caregiverNameStr);
        updates.put("caregiverEmail", caregiverEmailStr.toLowerCase().trim());
        updates.put("caregiverPhone", caregiverPhoneStr);

        FirebaseFirestore.getInstance().collection("users").document(currentUid)
                .update(updates)
                .addOnCompleteListener(task -> {
                    loadingOverlay.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        toggleEditMode(false);
                        txtHeaderName.setText(nameStr);

                        // Save updated language into session manager
                        sessionManager.saveLanguage(newLangCode);

                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();

                        // Check if language changed
                        if (!newLangCode.equals(initialLanguageCode)) {
                            initialLanguageCode = newLangCode;
                            // Recreate ProfileActivity and return to updated dashboard in new language
                            String userRole = sessionManager.getUserRole();
                            RoleManager.navigateToDashboard(ProfileActivity.this, userRole);
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Database update failed. Offline status active.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Logout from systems and redirect
     */
    private void performLogout() {
        sessionManager.logoutUser();
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
        RoleManager.redirectToLogin(this);
        finish();
    }
}
