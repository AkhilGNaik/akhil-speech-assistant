package com.kannada.speechassistant.voiceassistant;

import androidx.annotation.NonNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Data model representing the intent extracted from user speech in the Voice Assistant pipeline:
 * Speech -> STT -> Command Processing -> Intent Detection -> Existing App Function -> TTS Response.
 */
public class VoiceIntent {

    public static final String INTENT_UNKNOWN = "INTENT_UNKNOWN";

    private final VoiceIntentType intentType;
    private final String intentName;
    private final String rawQuery;
    private final float confidence;
    private final Map<String, String> parameters;

    public VoiceIntent(@NonNull VoiceIntentType intentType, @NonNull String rawQuery, float confidence, Map<String, String> parameters) {
        this.intentType = intentType;
        this.intentName = intentType.getIntentId();
        this.rawQuery = rawQuery;
        this.confidence = confidence;
        this.parameters = (parameters != null) ? new HashMap<>(parameters) : new HashMap<>();
    }

    public VoiceIntent(@NonNull VoiceIntentType intentType, @NonNull String rawQuery) {
        this(intentType, rawQuery, 1.0f, null);
    }

    public VoiceIntent(@NonNull String intentName, @NonNull String rawQuery, float confidence, Map<String, String> parameters) {
        this.intentName = intentName;
        this.intentType = VoiceIntentType.fromIntentId(intentName);
        this.rawQuery = rawQuery;
        this.confidence = confidence;
        this.parameters = (parameters != null) ? new HashMap<>(parameters) : new HashMap<>();
    }

    public VoiceIntent(@NonNull String intentName, @NonNull String rawQuery) {
        this(intentName, rawQuery, 1.0f, null);
    }

    @NonNull
    public VoiceIntentType getIntentType() {
        return intentType != null ? intentType : VoiceIntentType.fromIntentId(intentName);
    }

    @NonNull
    public String getIntentName() {
        return intentName;
    }

    @NonNull
    public String getRawQuery() {
        return rawQuery;
    }

    public float getConfidence() {
        return confidence;
    }

    @NonNull
    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public String getParameter(String key, String defaultValue) {
        if (parameters.containsKey(key)) {
            return parameters.get(key);
        }
        return defaultValue;
    }

    public boolean isKnown() {
        return !INTENT_UNKNOWN.equalsIgnoreCase(intentName);
    }

    @NonNull
    @Override
    public String toString() {
        return "VoiceIntent{" +
                "intentName='" + intentName + '\'' +
                ", rawQuery='" + rawQuery + '\'' +
                ", confidence=" + confidence +
                ", parameters=" + parameters +
                '}';
    }
}
