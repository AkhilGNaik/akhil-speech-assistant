package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.kannada.speechassistant.RoleManager;

import org.junit.Before;
import org.junit.Test;

public class VoiceAssistantRoleAwarenessTest {

    private VoiceIntent homeIntent;
    private VoiceIntent profileIntent;
    private VoiceIntent settingsIntent;
    private VoiceIntent messagesIntent;
    private VoiceIntent communicationIntent;
    private VoiceIntent caregiverIntent;
    private VoiceIntent caregiverConnIntent;
    private VoiceIntent voiceRecorderIntent;
    private VoiceIntent voiceMessageIntent;
    private VoiceIntent voiceCallIntent;
    private VoiceIntent emergencyIntent;

    private VoiceCommand cmdHome;
    private VoiceCommand cmdProfile;
    private VoiceCommand cmdSettings;
    private VoiceCommand cmdMessages;
    private VoiceCommand cmdCommunication;
    private VoiceCommand cmdCaregiver;
    private VoiceCommand cmdCaregiverConn;
    private VoiceCommand cmdVoiceRecorder;
    private VoiceCommand cmdVoiceMessage;
    private VoiceCommand cmdVoiceCall;
    private VoiceCommand cmdEmergency;

    @Before
    public void setUp() {
        homeIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_HOME, "open home", 1.0f, null);
        profileIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_PROFILE, "open profile", 1.0f, null);
        settingsIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_SETTINGS, "open settings", 1.0f, null);
        messagesIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_MESSAGES, "open messages", 1.0f, null);
        communicationIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_COMMUNICATION, "open communication", 1.0f, null);
        caregiverIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_CAREGIVER, "open caregiver", 1.0f, null);
        caregiverConnIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_CAREGIVER_CONNECTION, "open caregiver connection", 1.0f, null);
        voiceRecorderIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_VOICE_RECORDER, "open voice recorder", 1.0f, null);
        voiceMessageIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_VOICE_MESSAGE, "open voice message", 1.0f, null);
        voiceCallIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_VOICE_CALL, "open voice call", 1.0f, null);
        emergencyIntent = new VoiceIntent(VoiceCommandConstants.INTENT_OPEN_EMERGENCY, "emergency", 1.0f, null);

        cmdHome = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_HOME, homeIntent, VoiceCommandProcessor.ALL_ROLES, false);
        cmdProfile = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_PROFILE, profileIntent, VoiceCommandProcessor.PROFILE_ROLES, false);
        cmdSettings = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_SETTINGS, settingsIntent, VoiceCommandProcessor.ALL_ROLES, false);
        cmdMessages = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_MESSAGES, messagesIntent, VoiceCommandProcessor.ALL_ROLES, false);
        cmdCommunication = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_COMMUNICATION, communicationIntent, VoiceCommandProcessor.ALL_ROLES, false);
        cmdCaregiver = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_CAREGIVER, caregiverIntent, VoiceCommandProcessor.ALL_ROLES, false);
        cmdCaregiverConn = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_CAREGIVER_CONNECTION, caregiverConnIntent, VoiceCommandProcessor.ALL_ROLES, false);
        cmdVoiceRecorder = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_VOICE_RECORDER, voiceRecorderIntent, VoiceCommandProcessor.BLIND_ONLY_ROLES, false);
        cmdVoiceMessage = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_VOICE_MESSAGE, voiceMessageIntent, VoiceCommandProcessor.BLIND_ONLY_ROLES, false);
        cmdVoiceCall = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_VOICE_CALL, voiceCallIntent, VoiceCommandProcessor.BLIND_ONLY_ROLES, false);
        cmdEmergency = new VoiceCommand(VoiceCommandConstants.CMD_OPEN_EMERGENCY, emergencyIntent, VoiceCommandProcessor.ALL_ROLES, false);
    }

    @Test
    public void testRoleNormalization() {
        assertEquals(RoleManager.ROLE_MUTE_USER, VoiceCommand.normalizeRole("Mute User"));
        assertEquals(RoleManager.ROLE_BLIND_USER, VoiceCommand.normalizeRole("Blind User"));
        assertEquals(RoleManager.ROLE_PHYSICALLY_DISABLED, VoiceCommand.normalizeRole("Physically Disabled User"));
        assertEquals(RoleManager.ROLE_DEAF_USER, VoiceCommand.normalizeRole("Deaf User"));
        assertEquals(RoleManager.ROLE_DEAF_USER, VoiceCommand.normalizeRole("Speech-Impaired User"));
        assertEquals(RoleManager.ROLE_ADMIN_CAREGIVER, VoiceCommand.normalizeRole("Admin/Caregiver"));
        assertEquals(RoleManager.ROLE_ADMIN_CAREGIVER, VoiceCommand.normalizeRole("Caregiver"));
        assertEquals(RoleManager.ROLE_ADMIN_CAREGIVER, VoiceCommand.normalizeRole("Admin"));
    }

    @Test
    public void testBlindUserCapabilities() {
        String role = RoleManager.ROLE_BLIND_USER;
        assertTrue(cmdHome.isRoleAuthorized(role));
        assertTrue(cmdProfile.isRoleAuthorized(role));
        assertTrue(cmdSettings.isRoleAuthorized(role));
        assertTrue(cmdMessages.isRoleAuthorized(role));
        assertTrue(cmdCommunication.isRoleAuthorized(role));
        assertTrue(cmdCaregiver.isRoleAuthorized(role));
        assertTrue(cmdCaregiverConn.isRoleAuthorized(role));
        assertTrue(cmdVoiceRecorder.isRoleAuthorized(role));
        assertTrue(cmdVoiceMessage.isRoleAuthorized(role));
        assertTrue(cmdVoiceCall.isRoleAuthorized(role));
        assertTrue(cmdEmergency.isRoleAuthorized(role));
    }

    @Test
    public void testMuteUserCapabilities() {
        String role = RoleManager.ROLE_MUTE_USER;
        assertTrue(cmdHome.isRoleAuthorized(role));
        assertTrue(cmdProfile.isRoleAuthorized(role));
        assertTrue(cmdSettings.isRoleAuthorized(role));
        assertTrue(cmdMessages.isRoleAuthorized(role));
        assertTrue(cmdCommunication.isRoleAuthorized(role));
        assertTrue(cmdCaregiver.isRoleAuthorized(role));
        assertTrue(cmdCaregiverConn.isRoleAuthorized(role));
        assertTrue(cmdEmergency.isRoleAuthorized(role));

        // Unauthorized for Mute User
        assertFalse(cmdVoiceRecorder.isRoleAuthorized(role));
        assertFalse(cmdVoiceMessage.isRoleAuthorized(role));
        assertFalse(cmdVoiceCall.isRoleAuthorized(role));
    }

    @Test
    public void testDeafUserCapabilities() {
        String role = RoleManager.ROLE_DEAF_USER;
        assertTrue(cmdHome.isRoleAuthorized(role));
        assertTrue(cmdProfile.isRoleAuthorized(role));
        assertTrue(cmdSettings.isRoleAuthorized(role));
        assertTrue(cmdMessages.isRoleAuthorized(role));
        assertTrue(cmdCommunication.isRoleAuthorized(role));
        assertTrue(cmdCaregiver.isRoleAuthorized(role));
        assertTrue(cmdCaregiverConn.isRoleAuthorized(role));
        assertTrue(cmdEmergency.isRoleAuthorized(role));

        // Unauthorized for Deaf User
        assertFalse(cmdVoiceRecorder.isRoleAuthorized(role));
        assertFalse(cmdVoiceMessage.isRoleAuthorized(role));
        assertFalse(cmdVoiceCall.isRoleAuthorized(role));

        // Test alias "Speech-Impaired User"
        String speechImpaired = RoleManager.ROLE_SPEECH_IMPAIRED;
        assertTrue(cmdHome.isRoleAuthorized(speechImpaired));
        assertTrue(cmdEmergency.isRoleAuthorized(speechImpaired));
        assertFalse(cmdVoiceRecorder.isRoleAuthorized(speechImpaired));
        assertFalse(cmdVoiceCall.isRoleAuthorized(speechImpaired));
    }

    @Test
    public void testAdminCaregiverCapabilities() {
        String role = RoleManager.ROLE_ADMIN_CAREGIVER;
        assertTrue(cmdHome.isRoleAuthorized(role));
        assertTrue(cmdSettings.isRoleAuthorized(role));
        assertTrue(cmdMessages.isRoleAuthorized(role));
        assertTrue(cmdCommunication.isRoleAuthorized(role));
        assertTrue(cmdCaregiver.isRoleAuthorized(role));
        assertTrue(cmdCaregiverConn.isRoleAuthorized(role));
        assertTrue(cmdEmergency.isRoleAuthorized(role));

        // Unauthorized for Admin/Caregiver
        assertFalse(cmdProfile.isRoleAuthorized(role));
        assertFalse(cmdVoiceRecorder.isRoleAuthorized(role));
        assertFalse(cmdVoiceMessage.isRoleAuthorized(role));
        assertFalse(cmdVoiceCall.isRoleAuthorized(role));

        // Test alias "Caregiver"
        assertTrue(cmdHome.isRoleAuthorized("Caregiver"));
        assertTrue(cmdEmergency.isRoleAuthorized("Caregiver"));
        assertFalse(cmdProfile.isRoleAuthorized("Caregiver"));
        assertFalse(cmdVoiceCall.isRoleAuthorized("Caregiver"));

        // Test alias "Admin"
        assertTrue(cmdHome.isRoleAuthorized("Admin"));
        assertTrue(cmdEmergency.isRoleAuthorized("Admin"));
        assertFalse(cmdProfile.isRoleAuthorized("Admin"));
        assertFalse(cmdVoiceCall.isRoleAuthorized("Admin"));
    }

    @Test
    public void testUnauthorizedResponseConstants() {
        assertEquals("That function is not available for your user type.", VoiceCommandConstants.RESPONSE_ROLE_UNAUTHORIZED);
    }
}
