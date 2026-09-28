package com.kannada.speechassistant;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller class for the Login Activity.
 * Handles form validation, client-side Firebase authentication,
 * backend token verification for role-based access, and accessibility features.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    
    // UI Elements
    private TextInputLayout emailInputLayout;
    private TextInputEditText emailEditText;
    private TextInputLayout passwordInputLayout;
    private TextInputEditText passwordEditText;
    private CheckBox checkboxRemember;
    private TextView btnForgotPassword;
    private MaterialButton btnLogin;
    private ProgressBar loginProgressBar;
    private LinearLayout btnVoiceAssist;
    private MaterialCardView errorCardView;
    private TextView errorTextView;
    private TextView btnRegisterRedirect;

    // Firebase & Local Utilities
    private FirebaseAuth mAuth;
    private SessionManager sessionManager;
    private TextToSpeech tts;
    private ExecutorService networkExecutor;

    // Backend URL (10.0.2.2 points to host localhost in Android Emulator)
    private static final String BACKEND_LOGIN_URL = "http://10.0.2.2:5000/api/login";

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        android.content.res.Configuration config = new android.content.res.Configuration(newBase.getResources().getConfiguration());
        config.setLocale(java.util.Locale.ENGLISH);
        super.attachBaseContext(newBase.createConfigurationContext(config));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth & Session Manager
        mAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);
        networkExecutor = Executors.newSingleThreadExecutor();

        // Bind UI Elements
        emailInputLayout = findViewById(R.id.emailInputLayout);
        emailEditText = findViewById(R.id.emailEditText);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        passwordEditText = findViewById(R.id.passwordEditText);
        checkboxRemember = findViewById(R.id.checkboxRemember);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        btnLogin = findViewById(R.id.btnLogin);
        loginProgressBar = findViewById(R.id.loginProgressBar);
        btnVoiceAssist = findViewById(R.id.btnVoiceAssist);
        errorCardView = findViewById(R.id.errorCardView);
        errorTextView = findViewById(R.id.errorTextView);
        btnRegisterRedirect = findViewById(R.id.btnRegisterRedirect);

        // Setup Accessibility Text-to-Speech
        initTextToSpeech();

        // Listeners
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });

        btnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });

        btnVoiceAssist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakInstructions();
            }
        });

        btnRegisterRedirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to RegisterActivity
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Initializes the TTS engine for accessibility narration.
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
     * Speaks voice-assistance instructions for logging in.
     */
    private void speakInstructions() {
        if (tts != null) {
            String instructions = "Welcome to the Speech Assistant Login Screen. " +
                    "Please enter your email address in the first text field. " +
                    "Enter your password in the second text field. " +
                    "Select the checkbox to remember your login session. " +
                    "Tap the Login button at the bottom to continue. " +
                    "If you forgot your password, select the Forgot Password link next to the checkbox.";
            tts.speak(instructions, TextToSpeech.QUEUE_FLUSH, null, "login_instructions");
            Toast.makeText(this, "Reading instructions...", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Runs validations and signs the user in via Firebase.
     */
    private void attemptLogin() {
        // Clear previous error messages
        errorCardView.setVisibility(View.GONE);
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;

        // Validate Password
        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError("Password is required");
            focusView = passwordEditText;
            cancel = true;
        } else if (password.length() < 6) {
            passwordInputLayout.setError("Password must be at least 6 characters");
            focusView = passwordEditText;
            cancel = true;
        }

        // Validate Email
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError("Email address is required");
            focusView = emailEditText;
            cancel = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Enter a valid email address");
            focusView = emailEditText;
            cancel = true;
        }

        if (cancel) {
            // Focus the first field with an error
            focusView.requestFocus();
        } else {
            // Perform Firebase sign in
            setLoadingState(true);
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Firebase sign-in successful.");
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    fetchTokenAndGetRole(user);
                                }
                            } else {
                                Log.w(TAG, "Firebase sign-in failed.", task.getException());
                                setLoadingState(false);
                                showError("Authentication Failed: " + getFirebaseErrorMessage(task.getException()));
                            }
                        }
                    });
        }
    }

    /**
     * Fetches the user's role and language directly from Firestore after successful Firebase authentication.
     */
    private void fetchTokenAndGetRole(@NonNull final FirebaseUser user) {
        final String uid = user.getUid();
        final String email = user.getEmail();

        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users")
                .document(uid)
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
                                syncRoleWithBackend(user);
                                onBackendSuccess(uid, email, role, language);
                                return;
                            }
                        }
                    }

                    Log.w(TAG, "Failed to retrieve valid user role from Firestore for UID: " + uid);
                    setLoadingState(false);
                    showError("User profile or role not found. Please sign up or contact administrator.");
                });
    }

    /**
     * Background fire-and-forget sync to register the logged-in user session with Python backend.
     */
    private void syncRoleWithBackend(@NonNull final FirebaseUser user) {
        user.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
            @Override
            public void onComplete(@NonNull Task<GetTokenResult> task) {
                if (task.isSuccessful() && task.getResult().getToken() != null) {
                    final String idToken = task.getResult().getToken();
                    networkExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            HttpURLConnection conn = null;
                            try {
                                URL url = new URL(BACKEND_LOGIN_URL);
                                conn = (HttpURLConnection) url.openConnection();
                                conn.setRequestMethod("POST");
                                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                                conn.setRequestProperty("Accept", "application/json");
                                conn.setDoOutput(true);
                                conn.setConnectTimeout(2000);
                                conn.setReadTimeout(2000);

                                JSONObject jsonParam = new JSONObject();
                                jsonParam.put("idToken", idToken);

                                OutputStream os = conn.getOutputStream();
                                os.write(jsonParam.toString().getBytes("UTF-8"));
                                os.flush();
                                os.close();

                                int responseCode = conn.getResponseCode();
                                Log.i(TAG, "Backend sync completed. Response: " + responseCode);
                            } catch (Exception e) {
                                Log.e(TAG, "Background backend sync failed: " + e.getMessage());
                            } finally {
                                if (conn != null) {
                                    conn.disconnect();
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * Triggered on successful verification. Saves session and redirects.
     */
    private void onBackendSuccess(String uid, String email, String role, String language) {
        setLoadingState(false);
        
        // Update online status in Firestore
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .update("online", true);

        // Save local session with language preference
        sessionManager.createLoginSession(uid, email, role, language);

        Toast.makeText(this, "Welcome! Logged in as " + role, Toast.LENGTH_SHORT).show();

        // Redirect to corresponding dashboard
        RoleManager.navigateToDashboard(LoginActivity.this, role);
        finish();
    }

    /**
     * Toggle UI widgets during background requests
     */
    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            loginProgressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
            emailEditText.setEnabled(false);
            passwordEditText.setEnabled(false);
            checkboxRemember.setEnabled(false);
            btnForgotPassword.setEnabled(false);
            btnRegisterRedirect.setEnabled(false);
        } else {
            loginProgressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            emailEditText.setEnabled(true);
            passwordEditText.setEnabled(true);
            checkboxRemember.setEnabled(true);
            btnForgotPassword.setEnabled(true);
            btnRegisterRedirect.setEnabled(true);
        }
    }

    /**
     * Formats error message and shows banner
     */
    private void showError(String message) {
        errorTextView.setText(message);
        errorCardView.setVisibility(View.VISIBLE);
        // Announce error to screen readers
        errorCardView.announceForAccessibility(message);
    }

    /**
     * Handles forgot password dialog prompt and Firebase recovery email dispatch.
     */
    private void showForgotPasswordDialog() {
        final EditText resetEmailInput = new EditText(this);
        resetEmailInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        resetEmailInput.setHint("email@example.com");

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        FrameLayoutContainer frame = new FrameLayoutContainer(padding, resetEmailInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your registered email address below, and we will send you instructions to reset your password.")
                .setView(frame.view)
                .setPositiveButton("Send Reset Link", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String email = resetEmailInput.getText().toString().trim();
                        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            Toast.makeText(LoginActivity.this, "Please enter a valid email.", Toast.LENGTH_LONG).show();
                        } else {
                            sendPasswordReset(email);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendPasswordReset(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, 
                                "Password reset email sent successfully.", 
                                Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LoginActivity.this, 
                                "Error: " + getFirebaseErrorMessage(task.getException()), 
                                Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Map common Firebase exceptions to user friendly error messages
     */
    private String getFirebaseErrorMessage(Exception exception) {
        if (exception == null) return "Unknown error occurred.";
        String message = exception.getLocalizedMessage();
        if (message == null) return "Error connecting to service.";

        if (message.contains("user-not-found") || message.contains("no user record")) {
            return "No account exists with this email address.";
        } else if (message.contains("wrong-password")) {
            return "Incorrect password. Please try again.";
        } else if (message.contains("invalid-email")) {
            return "The email format is invalid.";
        } else if (message.contains("user-disabled")) {
            return "This user account has been disabled.";
        } else if (message.contains("network-request-failed") || message.contains("network connection")) {
            return "Network error. Check your internet connection.";
        }
        return message;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown executor and TTS engine
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (networkExecutor != null) {
            networkExecutor.shutdown();
        }
    }

    // Helper wrapper class to apply margins around dialog view components
    private class FrameLayoutContainer {
        android.widget.FrameLayout view;
        FrameLayoutContainer(int padding, View child) {
            view = new android.widget.FrameLayout(LoginActivity.this);
            android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT, 
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            );
            params.leftMargin = padding;
            params.rightMargin = padding;
            params.topMargin = padding / 2;
            params.bottomMargin = padding / 2;
            child.setLayoutParams(params);
            view.addView(child);
        }
    }
}
