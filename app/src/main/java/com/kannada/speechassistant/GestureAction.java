package com.kannada.speechassistant;

import androidx.annotation.NonNull;

/**
 * Immutable model representing a distinct gesture action.
 * Maps MediaPipe standard gesture categories to communication actions,
 * emojis, and localized messages across Kannada, Hindi, Malayalam, and English.
 */
public class GestureAction {

    private final String gestureId;
    private final String mediaPipeCategory;
    private final String emoji;
    private final String displayNameEnglish;
    private final String displayNameKannada;
    private final String displayNameHindi;
    private final String displayNameMalayalam;
    private final String messageKannada;
    private final String messageHindi;
    private final String messageMalayalam;
    private final String messageEnglish;
    private final String actionId;
    private final String actionDescription;
    private final boolean isEmergency;

    public GestureAction(
            @NonNull String gestureId,
            @NonNull String mediaPipeCategory,
            @NonNull String emoji,
            @NonNull String displayNameEnglish,
            @NonNull String displayNameKannada,
            @NonNull String displayNameHindi,
            @NonNull String displayNameMalayalam,
            @NonNull String messageKannada,
            @NonNull String messageHindi,
            @NonNull String messageMalayalam,
            @NonNull String messageEnglish,
            @NonNull String actionId,
            @NonNull String actionDescription,
            boolean isEmergency) {
        this.gestureId = gestureId;
        this.mediaPipeCategory = mediaPipeCategory;
        this.emoji = emoji;
        this.displayNameEnglish = displayNameEnglish;
        this.displayNameKannada = displayNameKannada;
        this.displayNameHindi = displayNameHindi;
        this.displayNameMalayalam = displayNameMalayalam;
        this.messageKannada = messageKannada;
        this.messageHindi = messageHindi;
        this.messageMalayalam = messageMalayalam;
        this.messageEnglish = messageEnglish;
        this.actionId = actionId;
        this.actionDescription = actionDescription;
        this.isEmergency = isEmergency;
    }

    public String getGestureId() {
        return gestureId;
    }

    public String getMediaPipeCategory() {
        return mediaPipeCategory;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getDisplayNameEnglish() {
        return displayNameEnglish;
    }

    public String getDisplayNameKannada() {
        return displayNameKannada;
    }

    public String getDisplayNameHindi() {
        return displayNameHindi;
    }

    public String getDisplayNameMalayalam() {
        return displayNameMalayalam;
    }

    public String getActionId() {
        return actionId;
    }

    public String getActionDescription() {
        return actionDescription;
    }

    public boolean isEmergency() {
        return isEmergency;
    }

    /**
     * Resolves display name for UI based on language code ("kn", "hi", "ml", "en").
     */
    public String getDisplayName(String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        if (LanguageManager.LANG_MALAYALAM.equals(code)) {
            return displayNameMalayalam;
        } else if (LanguageManager.LANG_HINDI.equals(code)) {
            return displayNameHindi;
        } else {
            return displayNameKannada;
        }
    }

    /**
     * Resolves localized speech / communication message based on language code ("kn", "hi", "ml", "en").
     */
    public String getLocalizedMessage(String langCode) {
        String code = LanguageManager.normalizeLanguageCode(langCode);
        if (LanguageManager.LANG_MALAYALAM.equals(code)) {
            return messageMalayalam;
        } else if (LanguageManager.LANG_HINDI.equals(code)) {
            return messageHindi;
        } else {
            return messageKannada;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "GestureAction{" +
                "gestureId='" + gestureId + '\'' +
                ", mediaPipeCategory='" + mediaPipeCategory + '\'' +
                ", emoji='" + emoji + '\'' +
                ", actionId='" + actionId + '\'' +
                '}';
    }
}
