package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.kannada.speechassistant.RoleManager;

import org.junit.Before;
import org.junit.Test;

/**
 * Validates confirmation handling for high-impact actions:
 * - Start Voice Call: requires confirmation
 * - Emergency Alert: requires confirmation
 * - Simple navigation: does NOT require confirmation
 * - "Yes" confirms and executes existing function
 * - "No" cancels the action
 */
public class ConfirmationHandlingTest {

    private VoiceCommandProcessor processor;

    @Before
    public void setUp() {
        processor = new VoiceCommandProcessor(new DummyContext());
    }

    private static class DummyContext extends android.content.ContextWrapper {
        public DummyContext() {
            super(null);
        }

        @Override
        public android.content.Context getApplicationContext() {
            return this;
        }
    }

    @Test
    public void testActionsRequiringConfirmation() {
        String lang = "en";

        // Emergency commands execute directly without confirmation for immediate safety
        VoiceIntent emergencyIntent = processor.detectIntent("Emergency help", lang);
        VoiceCommand emergencyCmd = processor.resolveCommand(emergencyIntent, RoleManager.ROLE_BLIND_USER);
        assertFalse("Emergency must execute directly without confirmation", emergencyCmd.requiresConfirmation());

        VoiceIntent alertIntent = processor.detectIntent("Send emergency alert", lang);
        VoiceCommand alertCmd = processor.resolveCommand(alertIntent, RoleManager.ROLE_BLIND_USER);
        assertFalse("Emergency alert must execute directly without confirmation", alertCmd.requiresConfirmation());
    }

    @Test
    public void testSimpleNavigationDoesNotRequireConfirmation() {
        String lang = "en";

        // Open Home
        VoiceCommand homeCmd = processor.resolveCommand(processor.detectIntent("Open home", lang), RoleManager.ROLE_BLIND_USER);
        assertFalse("Open home should not require confirmation", homeCmd.requiresConfirmation());

        // Open Profile
        VoiceCommand profileCmd = processor.resolveCommand(processor.detectIntent("Open profile", lang), RoleManager.ROLE_BLIND_USER);
        assertFalse("Open profile should not require confirmation", profileCmd.requiresConfirmation());

        // Open Settings
        VoiceCommand settingsCmd = processor.resolveCommand(processor.detectIntent("Open settings", lang), RoleManager.ROLE_BLIND_USER);
        assertFalse("Open settings should not require confirmation", settingsCmd.requiresConfirmation());

        // Open Messages
        VoiceCommand msgCmd = processor.resolveCommand(processor.detectIntent("Open messages", lang), RoleManager.ROLE_BLIND_USER);
        assertFalse("Open messages should not require confirmation", msgCmd.requiresConfirmation());

        // Open Communication
        VoiceCommand commCmd = processor.resolveCommand(processor.detectIntent("Open communication", lang), RoleManager.ROLE_BLIND_USER);
        assertFalse("Open communication should not require confirmation", commCmd.requiresConfirmation());

        // Open Caregiver Connection
        VoiceCommand connCmd = processor.resolveCommand(processor.detectIntent("Open caregiver connection", lang), RoleManager.ROLE_BLIND_USER);
        assertFalse("Open caregiver connection should not require confirmation", connCmd.requiresConfirmation());

        // Open Voice Recorder
        VoiceCommand recorderCmd = processor.resolveCommand(processor.detectIntent("Open voice recorder", lang), RoleManager.ROLE_BLIND_USER);
        assertFalse("Open voice recorder should not require confirmation", recorderCmd.requiresConfirmation());

        // Call Caregiver (Direct call initiation for Blind User)
        VoiceCommand callCmd = processor.resolveCommand(processor.detectIntent("Call my caregiver", lang), RoleManager.ROLE_BLIND_USER);
        assertFalse("Call caregiver should not require confirmation", callCmd.requiresConfirmation());

        // Start / Stop Listening
        VoiceCommand startListeningCmd = processor.resolveCommand(processor.detectIntent("Start listening", lang), RoleManager.ROLE_BLIND_USER);
        assertFalse("Start listening should not require confirmation", startListeningCmd.requiresConfirmation());

        VoiceCommand stopListeningCmd = processor.resolveCommand(processor.detectIntent("Stop listening", lang), RoleManager.ROLE_BLIND_USER);
        assertFalse("Stop listening should not require confirmation", stopListeningCmd.requiresConfirmation());
    }

    @Test
    public void testConfirmationPrompts() {
        // Voice Call Prompt
        assertEquals("Do you want to call your caregiver?",
                AppVoiceAssistant.getConfirmationPrompt(VoiceCommandConstants.CMD_OPEN_VOICE_CALL));

        // Emergency Prompts
        assertEquals("Do you want to send an emergency alert?",
                AppVoiceAssistant.getConfirmationPrompt(VoiceCommandConstants.CMD_OPEN_EMERGENCY));
        assertEquals("Do you want to send an emergency alert?",
                AppVoiceAssistant.getConfirmationPrompt(VoiceCommandConstants.CMD_OPEN_EMERGENCY_ALERT));

        // Confirmed Response for Voice Call
        assertEquals("Calling your caregiver.",
                AppVoiceAssistant.getConfirmedExecutionResponse(VoiceCommandConstants.CMD_OPEN_VOICE_CALL, RoleManager.ROLE_BLIND_USER));

        // Cancelled Response
        assertEquals("Action cancelled.", VoiceCommandConstants.RESPONSE_ACTION_CANCELLED);
    }

    @Test
    public void testAffirmativeConfirmationMatching() {
        String lang = "en";

        // English Variations
        assertEquals(VoiceIntentType.CONFIRM_YES, processor.detectIntent("yes", lang).getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_YES, processor.detectIntent("yeah", lang).getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_YES, processor.detectIntent("sure", lang).getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_YES, processor.detectIntent("ok", lang).getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_YES, processor.detectIntent("confirm", lang).getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_YES, processor.detectIntent("proceed", lang).getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_YES, processor.detectIntent("yes please", lang).getIntentType());

        // Kannada
        assertEquals(VoiceIntentType.CONFIRM_YES, processor.detectIntent("ಹೌದು", "kn").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_YES, processor.detectIntent("ಸರಿ", "kn").getIntentType());

        // Hindi
        assertEquals(VoiceIntentType.CONFIRM_YES, processor.detectIntent("हाँ", "hi").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_YES, processor.detectIntent("ठीक है", "hi").getIntentType());

        // Malayalam
        assertEquals(VoiceIntentType.CONFIRM_YES, processor.detectIntent("അതെ", "ml").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_YES, processor.detectIntent("ശരി", "ml").getIntentType());
    }

    @Test
    public void testNegativeConfirmationMatching() {
        String lang = "en";

        // English Variations
        assertEquals(VoiceIntentType.CONFIRM_NO, processor.detectIntent("no", lang).getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_NO, processor.detectIntent("cancel", lang).getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_NO, processor.detectIntent("don't", lang).getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_NO, processor.detectIntent("never mind", lang).getIntentType());

        // Kannada
        assertEquals(VoiceIntentType.CONFIRM_NO, processor.detectIntent("ಇಲ್ಲ", "kn").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_NO, processor.detectIntent("ಬೇಡ", "kn").getIntentType());

        // Hindi
        assertEquals(VoiceIntentType.CONFIRM_NO, processor.detectIntent("नहीं", "hi").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_NO, processor.detectIntent("मत करो", "hi").getIntentType());

        // Malayalam
        assertEquals(VoiceIntentType.CONFIRM_NO, processor.detectIntent("വേണ്ട", "ml").getIntentType());
        assertEquals(VoiceIntentType.CONFIRM_NO, processor.detectIntent("റദ്ദാക്കുക", "ml").getIntentType());
    }
}
