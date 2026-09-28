package com.kannada.speechassistant.voiceassistant;

import androidx.annotation.NonNull;

/**
 * Interface for decoupling command execution from the Voice Assistant core.
 * Allows activities or functional modules to register handlers that execute commands.
 */
public interface VoiceActionHandler {

    /**
     * Callback for notifying command processing outcome.
     */
    interface ActionExecutionCallback {
        void onSuccess(String spokenConfirmation);
        void onFailure(String errorMessage);
    }

    /**
     * Determines whether this handler can handle the resolved command.
     *
     * @param command The resolved command.
     * @return true if handled by this handler, false otherwise.
     */
    boolean canHandle(@NonNull VoiceCommand command);

    /**
     * Executes the target action for the resolved command.
     *
     * @param command  The resolved command.
     * @param callback Callback to return execution success/failure and response speech.
     */
    void executeAction(@NonNull VoiceCommand command, @NonNull ActionExecutionCallback callback);
}
