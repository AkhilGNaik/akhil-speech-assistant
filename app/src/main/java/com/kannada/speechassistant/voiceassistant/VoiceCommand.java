package com.kannada.speechassistant.voiceassistant;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kannada.speechassistant.RoleManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Encapsulates an executable voice command derived from intent processing.
 * Enforces role-based authorization matching existing application user roles.
 */
public class VoiceCommand {

    public static final String COMMAND_UNRESOLVED = "COMMAND_UNRESOLVED";

    private final String commandId;
    private final VoiceIntent intent;
    private final Set<String> allowedRoles;
    private final boolean requiresConfirmation;

    public VoiceCommand(@NonNull String commandId,
                        @NonNull VoiceIntent intent,
                        @Nullable Set<String> allowedRoles,
                        boolean requiresConfirmation) {
        this.commandId = commandId;
        this.intent = intent;
        this.allowedRoles = (allowedRoles != null) ? new HashSet<>(allowedRoles) : new HashSet<>();
        this.requiresConfirmation = requiresConfirmation;
    }

    public VoiceCommand(@NonNull String commandId, @NonNull VoiceIntent intent) {
        this(commandId, intent, null, false);
    }

    @NonNull
    public String getCommandId() {
        return commandId;
    }

    @NonNull
    public VoiceIntent getIntent() {
        return intent;
    }

    @NonNull
    public Set<String> getAllowedRoles() {
        return Collections.unmodifiableSet(allowedRoles);
    }

    public boolean requiresConfirmation() {
        return requiresConfirmation;
    }

    /**
     * Checks if the specified role is permitted to execute this voice command.
     * An empty allowedRoles set signifies permission for all authenticated roles.
     */
    public boolean isRoleAuthorized(@Nullable String userRole) {
        if (allowedRoles.isEmpty()) {
            return true;
        }
        if (userRole == null || userRole.trim().isEmpty()) {
            return false;
        }

        String normalizedCurrent = normalizeRole(userRole);
        for (String allowed : allowedRoles) {
            if (normalizedCurrent.equalsIgnoreCase(normalizeRole(allowed))) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    public static String normalizeRole(@NonNull String role) {
        String trimmed = role.trim();
        if (trimmed.equalsIgnoreCase(RoleManager.ROLE_MUTE_USER)) {
            return RoleManager.ROLE_MUTE_USER;
        }
        if (trimmed.equalsIgnoreCase(RoleManager.ROLE_BLIND_USER)) {
            return RoleManager.ROLE_BLIND_USER;
        }
        if (trimmed.equalsIgnoreCase(RoleManager.ROLE_PHYSICALLY_DISABLED)) {
            return RoleManager.ROLE_PHYSICALLY_DISABLED;
        }
        if (trimmed.equalsIgnoreCase(RoleManager.ROLE_DEAF_USER)
                || trimmed.equalsIgnoreCase(RoleManager.ROLE_SPEECH_IMPAIRED)
                || trimmed.toLowerCase(Locale.ROOT).contains("deaf")
                || trimmed.toLowerCase(Locale.ROOT).contains("speech-impaired")) {
            return RoleManager.ROLE_DEAF_USER;
        }
        if (trimmed.equalsIgnoreCase(RoleManager.ROLE_ADMIN_CAREGIVER)
                || trimmed.equalsIgnoreCase("Caregiver")
                || trimmed.equalsIgnoreCase("Admin")
                || trimmed.toLowerCase(Locale.ROOT).contains("admin")
                || trimmed.toLowerCase(Locale.ROOT).contains("caregiver")) {
            return RoleManager.ROLE_ADMIN_CAREGIVER;
        }
        return trimmed;
    }

    public boolean isResolved() {
        return !COMMAND_UNRESOLVED.equalsIgnoreCase(commandId);
    }

    @NonNull
    @Override
    public String toString() {
        return "VoiceCommand{" +
                "commandId='" + commandId + '\'' +
                ", intent=" + intent +
                ", allowedRoles=" + allowedRoles +
                ", requiresConfirmation=" + requiresConfirmation +
                '}';
    }
}
