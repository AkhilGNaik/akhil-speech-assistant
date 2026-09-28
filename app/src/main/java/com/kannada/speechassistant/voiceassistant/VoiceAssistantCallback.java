package com.kannada.speechassistant.voiceassistant;

import androidx.annotation.NonNull;

/**
 * Callback listener interface for observing the Voice Assistant processing pipeline lifecycle:
 * Speech -> STT -> Command Processing -> Intent Detection -> App Function -> TTS Response.
 */
public interface VoiceAssistantCallback {

    /**
     * Fired when the voice assistant has successfully validated user authentication and role,
     * and is ready to accept voice input.
     */
    void onAssistantReady(@NonNull String userRole, @NonNull String languageCode);

    /**
     * Fired when the speech recognizer has engaged the microphone and began listening.
     */
    void onListeningStarted();

    /**
     * Fired when the speech recognizer stops listening (silence reached, speech ended, or manually stopped).
     */
    void onListeningStopped();

    /**
     * Fired when STT has successfully converted user speech into text.
     *
     * @param rawText The raw recognized utterance.
     */
    void onSpeechRecognized(@NonNull String rawText);

    /**
     * Fired when intent detection analysis has completed.
     *
     * @param intent The identified intent model.
     */
    void onIntentDetected(@NonNull VoiceIntent intent);

    /**
     * Fired when the command processor has resolved an actionable command.
     *
     * @param command The resolved command model.
     */
    void onCommandResolved(@NonNull VoiceCommand command);

    /**
     * Fired when the assistant finishes articulating the synthesized vocal response.
     *
     * @param ttsResponse The spoken text response.
     */
    void onResponseSpoken(@NonNull String ttsResponse);

    /**
     * Fired when an error occurs in the authentication, STT, intent, or TTS pipeline.
     *
     * @param errorMessage Human-readable error description.
     * @param errorCode    Distinct error code indicating the failure stage.
     */
    void onError(@NonNull String errorMessage, int errorCode);
}
