package com.kannada.speechassistant;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller class for the Registration Activity.
 * Handles client-side user account registration via Firebase Authentication,
 * saves profiles to Firebase Firestore, syncs user role mappings with the backend,
 * initializes session management, and provides Text-To-Speech accessibility guides.
 */
public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    // UI Elements
    private TextInputLayout nameInputLayout;
    private TextInputEditText nameEditText;
    private TextInputLayout emailInputLayout;
    private TextInputEditText emailEditText;
    private TextInputLayout passwordInputLayout;
    private TextInputEditText passwordEditText;
    private TextInputLayout confirmPasswordInputLayout;
    private TextInputEditText confirmPasswordEditText;
    private TextInputLayout roleInputLayout;
    private AutoCompleteTextView roleAutoCompleteTextView;
    private TextInputLayout languageInputLayout;
    private AutoCompleteTextView languageAutoCompleteTextView;
    private MaterialButton btnRegister;
    private ProgressBar registerProgressBar;
    private LinearLayout btnVoiceAssist;
    private TextView btnSignInRedirect;
    private MaterialCardView errorCardView;
    private TextView errorTextView;

    // Firebase & Utilities
    private FirebaseAuth mAuth;
    private SessionManager sessionManager;
    private TextToSpeech tts;
    private ExecutorService networkExecutor;

    // Backend Registration URL
    private static final String BACKEND_REGISTER_URL = "http://10.0.2.2:5000/api/register";

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        android.content.res.Configuration config = new android.content.res.Configuration(newBase.getResources().getConfiguration());
        config.setLocale(java.util.Locale.ENGLISH);
        super.attachBaseContext(newBase.createConfigurationContext(config));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase & Session utilities
        mAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);
        networkExecutor = Executors.newSingleThreadExecutor();

        // Bind UI Elements
        nameInputLayout = findViewById(R.id.nameInputLayout);
        nameEditText = findViewById(R.id.nameEditText);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        emailEditText = findViewById(R.id.emailEditText);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        roleInputLayout = findViewById(R.id.roleInputLayout);
        roleAutoCompleteTextView = findViewById(R.id.roleAutoCompleteTextView);
        languageInputLayout = findViewById(R.id.languageInputLayout);
        languageAutoCompleteTextView = findViewById(R.id.languageAutoCompleteTextView);
        btnRegister = findViewById(R.id.btnRegister);
        registerProgressBar = findViewById(R.id.registerProgressBar);
        btnVoiceAssist = findViewById(R.id.btnVoiceAssist);
        btnSignInRedirect = findViewById(R.id.btnSignInRedirect);
        errorCardView = findViewById(R.id.errorCardView);
        errorTextView = findViewById(R.id.errorTextView);

        // Populate User Roles dropdown spinner menu
        setupRolesDropdown();

        // Populate Language dropdown menu
        setupLanguagesDropdown();

        // Initialize Text-To-Speech
        initTextToSpeech();

        // View Action Listeners
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegistration();
            }
        });

        btnSignInRedirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to LoginActivity
                finish();
            }
        });

        btnVoiceAssist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakInstructions();
            }
        });
    }

    /**
     * Populates drop down choices for roles
     */
    private void setupRolesDropdown() {
        String[] roles = {"Mute User", "Deaf User", "Blind User", "Admin/Caregiver"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                roles
        );
        roleAutoCompleteTextView.setAdapter(adapter);
    }

    /**
     * Populates drop down choices for languages: Kannada, Hindi, Malayalam
     */
    private void setupLanguagesDropdown() {
        String[] languages = {"Kannada", "Hindi", "Malayalam"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                languages
        );
        languageAutoCompleteTextView.setAdapter(adapter);
        // Default selection: Kannada
        languageAutoCompleteTextView.setText("Kannada", false);
    }

    /**
     * Sets up TextToSpeech for accessibility narration
     */
    private void initTextToSpeech() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "TTS Language is not supported or missing data.");
                    }
                } else {
                    Log.e(TAG, "TTS Initialization failed.");
                }
            }
        });
    }

    /**
     * Reads registration instructions aloud
     */
    private void speakInstructions() {
        if (tts != null) {
            String instructions = "Welcome to the Speech Assistant Registration Screen. " +
                    "Please enter your full name in the first text field. " +
                    "Enter your email address in the second text field. " +
                    "Enter your password in the third text field, which must be at least six characters. " +
                    "Re-enter your password in the fourth text field to confirm. " +
                    "Select your user role from the dropdown menu. " +
                    "Select your preferred language: Kannada, Hindi, or Malayalam. " +
                    "Finally, tap the Sign Up button at the bottom to register. " +
                    "To navigate back to the login page, tap the Sign In link at the very bottom.";
            tts.speak(instructions, TextToSpeech.QUEUE_FLUSH, null, "register_instructions");
            Toast.makeText(this, "Reading instructions...", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Performs form validation and triggers Firebase User account creation
     */
    private void attemptRegistration() {
        // Reset visual layout error states
        errorCardView.setVisibility(View.GONE);
        nameInputLayout.setError(null);
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);
        roleInputLayout.setError(null);
        languageInputLayout.setError(null);

        final String name = nameEditText.getText() != null ? nameEditText.getText().toString().trim() : "";
        final String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        final String password = passwordEditText.getText() != null ? passwordEditText.getText().toString().trim() : "";
        String confirmPassword = confirmPasswordEditText.getText() != null ? confirmPasswordEditText.getText().toString().trim() : "";
        final String role = roleAutoCompleteTextView.getText() != null ? roleAutoCompleteTextView.getText().toString().trim() : "";
        final String languageDisplay = languageAutoCompleteTextView.getText() != null ? languageAutoCompleteTextView.getText().toString().trim() : "Kannada";
        final String languageCode = LanguageManager.getLanguageCodeFromDisplayName(languageDisplay);

        boolean cancel = false;
        View focusView = null;

        // Validate Language Dropdown Selection
        if (TextUtils.isEmpty(languageDisplay)) {
            languageInputLayout.setError("Language is required");
            focusView = languageAutoCompleteTextView;
            cancel = true;
        }

        // Validate User Role Dropdown Selection
        if (TextUtils.isEmpty(role)) {
            roleInputLayout.setError("Role is required");
            focusView = roleAutoCompleteTextView;
            cancel = true;
        }

        // Validate Confirm Password matching
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInputLayout.setError("Confirm password is required");
            focusView = confirmPasswordEditText;
            cancel = true;
        } else if (!confirmPassword.equals(password)) {
            confirmPasswordInputLayout.setError("Passwords do not match");
            focusView = confirmPasswordEditText;
            cancel = true;
        }

        // Validate Password length
        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError("Password is required");
            focusView = passwordEditText;
            cancel = true;
        } else if (password.length() < 6) {
            passwordInputLayout.setError("Password must be at least 6 characters");
            focusView = passwordEditText;
            cancel = true;
        }

        // Validate Email format
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Email address is required");
            focusView = emailEditText;
            cancel = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Enter a valid email address");
            focusView = emailEditText;
            cancel = true;
        }

        // Validate Full Name
        if (TextUtils.isEmpty(name)) {
            nameInputLayout.setError("Full name is required");
            focusView = nameEditText;
            cancel = true;
        }

        if (cancel) {
            // Focus on the layout containing the error
            focusView.requestFocus();
        } else {
            // Trigger loading view and call Firebase API
            setLoadingState(true);
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    Log.d(TAG, "Firebase account created successfully. UID: " + user.getUid());
                                    saveUserProfileToFirestore(user.getUid(), name, email, role, languageCode);
                                }
                            } else {
                                Log.w(TAG, "Firebase account registration failed.", task.getException());
                                setLoadingState(false);
                                showError("Registration Failed: " + getFirebaseErrorMessage(task.getException()));
                            }
                        }
                    });
        }
    }

    /**
     * Stores the registered user's profile details into Firestore
     */
    private void saveUserProfileToFirestore(final String uid, final String name, final String email, final String role, final String languageCode) {
        String finalRole = role;
        if ("Mute User".equalsIgnoreCase(role) || getString(R.string.role_mute_user).equalsIgnoreCase(role)) {
            finalRole = RoleManager.ROLE_MUTE_USER;
        } else if ("Deaf User".equalsIgnoreCase(role) || "Deaf Users".equalsIgnoreCase(role) || "Mute, Deaf & Blind Users".equalsIgnoreCase(role) || "Mute, Deaf & Blind User".equalsIgnoreCase(role) || getString(R.string.role_speech_impaired).equalsIgnoreCase(role)) {
            finalRole = RoleManager.ROLE_DEAF_USER;
        } else if ("Blind User".equalsIgnoreCase(role) || getString(R.string.role_blind_user).equalsIgnoreCase(role)) {
            finalRole = RoleManager.ROLE_BLIND_USER;
        } else if ("Admin/Caregiver".equalsIgnoreCase(role) || "Caregiver/Admin".equalsIgnoreCase(role) || getString(R.string.role_caregiver).equalsIgnoreCase(role)) {
            finalRole = RoleManager.ROLE_ADMIN_CAREGIVER;
        }

        final String finalRoleToUse = finalRole;
        final String finalLangToUse = LanguageManager.normalizeLanguageCode(languageCode);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid);
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("role", finalRoleToUse);
        userMap.put("language", finalLangToUse);
        userMap.put("registrationDate", com.google.firebase.Timestamp.now());

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(userMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User profile successfully saved in Firestore.");
                        // Clean up any stale duplicate document with the same email if different UID exists
                        if (email != null && !email.trim().isEmpty()) {
                            final String normalizedEmail = email.trim().toLowerCase();
                            FirebaseFirestore.getInstance().collection("users")
                                    .get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        if (querySnapshot != null) {
                                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                                String docEmail = doc.getString("email");
                                                if (docEmail != null && docEmail.trim().equalsIgnoreCase(normalizedEmail) && !uid.equals(doc.getId())) {
                                                    Log.i(TAG, "Cleaning up obsolete user document with same email on registration: " + doc.getId());
                                                    doc.getReference().delete();
                                                }
                                            }
                                        }
                                    });
                        }
                        // Push user role record to backend server in the background (fire-and-forget)
                        syncRoleWithBackend(uid, email, finalRoleToUse);
                        // Complete registration immediately without waiting for backend connection/timeout
                        onRegistrationSuccess(uid, email, finalRoleToUse, finalLangToUse);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error writing user document to Firestore.", e);
                        setLoadingState(false);
                        showError("Database Error: Failed to save user details. " + e.getLocalizedMessage());
                    }
                });
    }

    /**
     * Syncs user credentials and selected role with the Python local Flask server.
     * This is a background fire-and-forget sync.
     */
    private void syncRoleWithBackend(final String uid, final String email, final String role) {
        networkExecutor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(BACKEND_REGISTER_URL);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(2000); // 2 seconds timeout
                    conn.setReadTimeout(2000);

                    // Build JSON request payload
                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("uid", uid);
                    jsonParam.put("email", email);
                    jsonParam.put("role", role);

                    OutputStream os = conn.getOutputStream();
                    os.write(jsonParam.toString().getBytes("UTF-8"));
                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        Log.i(TAG, "Backend user role registration synchronized successfully.");
                    } else {
                        Log.e(TAG, "Backend server responded with error code: " + responseCode);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Backend sync failed: " + e.getMessage());
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        });
    }

    /**
     * Triggers on successful register & database save. Saves local session and opens Dashboard.
     */
    private void onRegistrationSuccess(String uid, String email, String role, String language) {
        setLoadingState(false);

        // Save local session state with language preference
        sessionManager.createLoginSession(uid, email, role, language);

        Toast.makeText(this, "Welcome! Registration successful.", Toast.LENGTH_SHORT).show();

        // Redirect to corresponding dashboard
        RoleManager.navigateToDashboard(RegisterActivity.this, role);
        finish();
    }

    /**
     * Toggles UI loading states during Firebase / Network calls
     */
    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            registerProgressBar.setVisibility(View.VISIBLE);
            btnRegister.setEnabled(false);
            nameEditText.setEnabled(false);
            emailEditText.setEnabled(false);
            passwordEditText.setEnabled(false);
            confirmPasswordEditText.setEnabled(false);
            roleAutoCompleteTextView.setEnabled(false);
            btnSignInRedirect.setEnabled(false);
        } else {
            registerProgressBar.setVisibility(View.GONE);
            btnRegister.setEnabled(true);
            nameEditText.setEnabled(true);
            emailEditText.setEnabled(true);
            passwordEditText.setEnabled(true);
            confirmPasswordEditText.setEnabled(true);
            roleAutoCompleteTextView.setEnabled(true);
            btnSignInRedirect.setEnabled(true);
        }
    }

    /**
     * Shows error banner
     */
    private void showError(String message) {
        errorTextView.setText(message);
        errorCardView.setVisibility(View.VISIBLE);
        // Announce error to screen readers
        errorCardView.announceForAccessibility(message);
    }

    /**
     * Maps common Firebase Auth exceptions to readable descriptions
     */
    private String getFirebaseErrorMessage(Exception exception) {
        if (exception == null) return "Unknown error occurred.";
        String message = exception.getLocalizedMessage();
        if (message == null) return "Error connecting to service.";

        if (message.contains("email-already-in-use") || message.contains("email address is already in use")) {
            return "An account already exists with this email address.";
        } else if (message.contains("weak-password")) {
            return "The password is too weak. Please use at least 6 characters.";
        } else if (message.contains("invalid-email")) {
            return "The email format is invalid.";
        } else if (message.contains("network-request-failed") || message.contains("network connection")) {
            return "Network error. Check your internet connection.";
        }
        return message;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release resources
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (networkExecutor != null) {
            networkExecutor.shutdown();
        }
    }
}
