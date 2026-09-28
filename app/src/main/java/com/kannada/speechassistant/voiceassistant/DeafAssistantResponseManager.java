package com.kannada.speechassistant.voiceassistant;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;
import com.kannada.speechassistant.LanguageManager;
import com.kannada.speechassistant.R;
import com.kannada.speechassistant.SessionManager;

import java.lang.ref.WeakReference;

/**
 * Reusable Visual Response Manager exclusively for Deaf User Voice Assistant.
 *
 * Implements strict TEXT-ONLY visual feedback across all 8 Assistant lifecycle states:
 * STATE 1 - IDLE: Assistant waiting (card hidden)
 * STATE 2 - WAKE WORD DETECTED: "Assistant activated"
 * STATE 3 - LISTENING: "Listening..." / "Speak your command"
 * STATE 4 - PROCESSING: "Processing..."
 * STATE 5 - SUCCESS: "✓ Profile opened" (actual execution result)
 * STATE 6 - COMMAND NOT UNDERSTOOD: "Sorry, I didn't understand the command."
 * STATE 7 - FUNCTION NOT AVAILABLE: "This function is not available."
 * STATE 8 - ERROR: "Something went wrong. Please try again."
 *
 * All messages are localized in the user's selected language (Kannada, Hindi, Malayalam, English).
 * TTS audio output is completely disabled.
 */
public class DeafAssistantResponseManager {

    private static final String TAG = "DeafAssistantResponse";
    private static final long AUTO_DISMISS_DELAY_MS = 4000L;
    private static final long WAKE_TRANSITION_DELAY_MS = 1000L;

    private WeakReference<Context> contextRef;
    private MaterialCardView cardResponse;
    private ImageView imgIcon;
    private TextView txtTitle;
    private TextView txtMessage;
    private TextView txtSubMessage;
    private ProgressBar progressBar;
    private ImageButton btnDismiss;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable autoDismissRunnable;

    public DeafAssistantResponseManager() {}

    /**
     * Binds the response manager to the visual card in the host activity or view.
     */
    public void bind(@NonNull Context context, @NonNull View rootView) {
        this.contextRef = new WeakReference<>(context);
        this.cardResponse = rootView.findViewById(R.id.cardDeafAssistantResponse);
        if (this.cardResponse != null) {
            this.imgIcon = cardResponse.findViewById(R.id.imgDeafAssistantIcon);
            this.txtTitle = cardResponse.findViewById(R.id.txtDeafAssistantTitle);
            this.txtMessage = cardResponse.findViewById(R.id.txtDeafAssistantMessage);
            this.txtSubMessage = cardResponse.findViewById(R.id.txtDeafAssistantSubMessage);
            this.progressBar = cardResponse.findViewById(R.id.progressDeafAssistant);
            this.btnDismiss = cardResponse.findViewById(R.id.btnDismissDeafAssistant);

            if (this.btnDismiss != null) {
                this.btnDismiss.setOnClickListener(v -> dismiss());
            }
        }
    }

    private String getLanguageCode() {
        Context ctx = contextRef != null ? contextRef.get() : null;
        if (ctx == null && cardResponse != null) {
            ctx = cardResponse.getContext();
        }
        if (ctx != null) {
            try {
                SessionManager sessionManager = new SessionManager(ctx);
                return LanguageManager.normalizeLanguageCode(sessionManager.getLanguage());
            } catch (Exception ignored) {}
        }
        return LanguageManager.DEFAULT_LANGUAGE;
    }

    private void cancelAutoDismiss() {
        if (autoDismissRunnable != null) {
            mainHandler.removeCallbacks(autoDismissRunnable);
            autoDismissRunnable = null;
        }
    }

    private void scheduleAutoDismiss(long delayMs) {
        cancelAutoDismiss();
        autoDismissRunnable = this::dismiss;
        mainHandler.postDelayed(autoDismissRunnable, delayMs);
    }

    private void runOnMainThread(Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            mainHandler.post(action);
        }
    }

    /**
     * STATE 1 - IDLE: Assistant waiting, card hidden.
     */
    public void showIdleState() {
        runOnMainThread(this::dismiss);
    }

    /**
     * STATE 2 - WAKE WORD DETECTED: Displays "Assistant activated".
     */
    public void showWakeWordDetected() {
        runOnMainThread(() -> {
            cancelAutoDismiss();
            if (cardResponse == null) return;

            String lang = getLanguageCode();
            String title = "🎤 " + (LanguageManager.LANG_KANNADA.equals(lang) ? "ಸಹಾಯಕ" :
                    (LanguageManager.LANG_HINDI.equals(lang) ? "सहायक" :
                    (LanguageManager.LANG_MALAYALAM.equals(lang) ? "അസിസ്റ്റന്റ്" : "Assistant")));
            String msg = VoiceLanguageConfig.getDeafAssistantActivatedText(lang);

            if (txtTitle != null) txtTitle.setText(title);
            if (txtMessage != null) {
                txtMessage.setText(msg);
                txtMessage.setTextColor(0xFF0F172A);
            }
            if (txtSubMessage != null) {
                txtSubMessage.setVisibility(View.GONE);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            cardResponse.setVisibility(View.VISIBLE);
            Log.i(TAG, "DEAF_ASSISTANT_VISUAL_STATE: WAKE_WORD_DETECTED (" + msg + ")");
        });
    }

    /**
     * STATE 3 - LISTENING: Displays "Listening..." / "Speak your command".
     */
    public void showListeningState() {
        runOnMainThread(() -> {
            cancelAutoDismiss();
            if (cardResponse == null) return;

            String lang = getLanguageCode();
            String title = "🔴 " + (LanguageManager.LANG_KANNADA.equals(lang) ? "ಆಲಿಸುತ್ತಿದೆ..." :
                    (LanguageManager.LANG_HINDI.equals(lang) ? "सुन रहा है..." :
                    (LanguageManager.LANG_MALAYALAM.equals(lang) ? "കേൾക്കുന്നു..." : "Listening...")));
            String msg = VoiceLanguageConfig.getDeafListeningText(lang);
            String prompt = VoiceLanguageConfig.getDeafSpeakCommandPrompt(lang);

            if (txtTitle != null) txtTitle.setText("🎤 Assistant");
            if (txtMessage != null) {
                txtMessage.setText(msg);
                txtMessage.setTextColor(0xFF0F172A);
            }
            if (txtSubMessage != null) {
                txtSubMessage.setText(prompt);
                txtSubMessage.setVisibility(View.VISIBLE);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            cardResponse.setVisibility(View.VISIBLE);
            Log.i(TAG, "DEAF_ASSISTANT_VISUAL_STATE: LISTENING (" + msg + ")");
        });
    }

    /**
     * STATE 4 - PROCESSING: Displays "Processing...".
     */
    public void showProcessingState() {
        runOnMainThread(() -> {
            cancelAutoDismiss();
            if (cardResponse == null) return;

            String lang = getLanguageCode();
            String msg = VoiceLanguageConfig.getDeafProcessingText(lang);

            if (txtTitle != null) txtTitle.setText("Assistant");
            if (txtMessage != null) {
                txtMessage.setText(msg);
                txtMessage.setTextColor(0xFF0F172A);
            }
            if (txtSubMessage != null) {
                txtSubMessage.setVisibility(View.GONE);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            cardResponse.setVisibility(View.VISIBLE);
            Log.i(TAG, "DEAF_ASSISTANT_VISUAL_STATE: PROCESSING (" + msg + ")");
        });
    }

    /**
     * STATE 5 - SUCCESS: Displays actual execution result.
     */
    public void showSuccess(@NonNull String successMessage) {
        runOnMainThread(() -> {
            cancelAutoDismiss();
            if (cardResponse == null) return;

            String displayResult = successMessage.startsWith("✓") ? successMessage : ("✓ " + successMessage);

            if (txtTitle != null) txtTitle.setText("Assistant");
            if (txtMessage != null) {
                txtMessage.setText(displayResult);
                txtMessage.setTextColor(0xFF047857); // High contrast emerald green
            }
            if (txtSubMessage != null) {
                txtSubMessage.setVisibility(View.GONE);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            cardResponse.setVisibility(View.VISIBLE);
            scheduleAutoDismiss(AUTO_DISMISS_DELAY_MS);
            Log.i(TAG, "DEAF_ASSISTANT_VISUAL_STATE: SUCCESS (" + displayResult + ")");
        });
    }

    /**
     * STATE 6 - COMMAND NOT UNDERSTOOD: Displays "Sorry, I didn't understand the command."
     */
    public void showCommandNotRecognized() {
        runOnMainThread(() -> {
            cancelAutoDismiss();
            if (cardResponse == null) return;

            String lang = getLanguageCode();
            String msg = VoiceLanguageConfig.getDeafCommandNotUnderstoodText(lang);

            if (txtTitle != null) txtTitle.setText("Assistant");
            if (txtMessage != null) {
                txtMessage.setText(msg);
                txtMessage.setTextColor(0xFFB91C1C); // Red
            }
            if (txtSubMessage != null) {
                txtSubMessage.setVisibility(View.GONE);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            cardResponse.setVisibility(View.VISIBLE);
            scheduleAutoDismiss(AUTO_DISMISS_DELAY_MS);
            Log.i(TAG, "DEAF_ASSISTANT_VISUAL_STATE: COMMAND_NOT_UNDERSTOOD (" + msg + ")");
        });
    }

    /**
     * STATE 7 - FUNCTION NOT AVAILABLE: Displays "This function is not available."
     */
    public void showFunctionNotAvailable() {
        runOnMainThread(() -> {
            cancelAutoDismiss();
            if (cardResponse == null) return;

            String lang = getLanguageCode();
            String msg = VoiceLanguageConfig.getDeafFunctionNotAvailableText(lang);

            if (txtTitle != null) txtTitle.setText("Assistant");
            if (txtMessage != null) {
                txtMessage.setText(msg);
                txtMessage.setTextColor(0xFFB91C1C);
            }
            if (txtSubMessage != null) {
                txtSubMessage.setVisibility(View.GONE);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            cardResponse.setVisibility(View.VISIBLE);
            scheduleAutoDismiss(AUTO_DISMISS_DELAY_MS);
            Log.i(TAG, "DEAF_ASSISTANT_VISUAL_STATE: FUNCTION_NOT_AVAILABLE (" + msg + ")");
        });
    }

    /**
     * STATE 8 - ERROR: Displays "Something went wrong. Please try again." or custom error.
     */
    public void showError(@Nullable String customErrorMessage) {
        runOnMainThread(() -> {
            cancelAutoDismiss();
            if (cardResponse == null) return;

            String lang = getLanguageCode();
            String msg = (customErrorMessage != null && !customErrorMessage.trim().isEmpty())
                    ? customErrorMessage
                    : VoiceLanguageConfig.getDeafErrorText(lang);

            if (txtTitle != null) txtTitle.setText("Assistant");
            if (txtMessage != null) {
                txtMessage.setText(msg);
                txtMessage.setTextColor(0xFFB91C1C);
            }
            if (txtSubMessage != null) {
                txtSubMessage.setVisibility(View.GONE);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            cardResponse.setVisibility(View.VISIBLE);
            scheduleAutoDismiss(AUTO_DISMISS_DELAY_MS);
            Log.i(TAG, "DEAF_ASSISTANT_VISUAL_STATE: ERROR (" + msg + ")");
        });
    }

    /**
     * Specific Error Case A: Wake word detected but no command spoken.
     */
    public void showPleaseSpeakCommand() {
        runOnMainThread(() -> {
            cancelAutoDismiss();
            if (cardResponse == null) return;

            String lang = getLanguageCode();
            String msg = VoiceLanguageConfig.getDeafPleaseSpeakCommandText(lang);

            if (txtTitle != null) txtTitle.setText("Assistant");
            if (txtMessage != null) {
                txtMessage.setText(msg);
                txtMessage.setTextColor(0xFFD97706); // Amber
            }
            if (txtSubMessage != null) {
                txtSubMessage.setVisibility(View.GONE);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            cardResponse.setVisibility(View.VISIBLE);
            scheduleAutoDismiss(AUTO_DISMISS_DELAY_MS);
            Log.i(TAG, "DEAF_ASSISTANT_VISUAL_STATE: PLEASE_SPEAK_COMMAND (" + msg + ")");
        });
    }

    /**
     * Specific Error Case B: Speech recognition error.
     */
    public void showRecognitionError() {
        runOnMainThread(() -> {
            String lang = getLanguageCode();
            showError(VoiceLanguageConfig.getDeafUnableToCompleteCommandText(lang));
        });
    }

    /**
     * Specific Conflict Case: Mic in use by another operation.
     */
    public void showMicConflict(@NonNull String conflictMessage) {
        runOnMainThread(() -> {
            cancelAutoDismiss();
            if (cardResponse == null) return;

            if (txtTitle != null) txtTitle.setText("Assistant");
            if (txtMessage != null) {
                txtMessage.setText(conflictMessage);
                txtMessage.setTextColor(0xFFD97706);
            }
            if (txtSubMessage != null) {
                txtSubMessage.setVisibility(View.GONE);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            cardResponse.setVisibility(View.VISIBLE);
            scheduleAutoDismiss(AUTO_DISMISS_DELAY_MS);
            Log.i(TAG, "DEAF_ASSISTANT_VISUAL_STATE: MIC_CONFLICT (" + conflictMessage + ")");
        });
    }

    /**
     * Shows a generic assistant message.
     */
    public void showAssistantMessage(@NonNull String message) {
        showSuccess(message);
    }

    /**
     * Dismisses and hides the visual response card.
     */
    public void dismiss() {
        cancelAutoDismiss();
        if (cardResponse != null) {
            cardResponse.setVisibility(View.GONE);
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        }
        Log.d(TAG, "DEAF_ASSISTANT_VISUAL_STATE: IDLE (Card dismissed)");
    }

    public boolean isCardVisible() {
        return cardResponse != null && cardResponse.getVisibility() == View.VISIBLE;
    }
}
