package com.kannada.speechassistant.voiceassistant;

/**
 * Constants for predefined voice assistant commands, intents, and default feedback responses.
 */
public class VoiceCommandConstants {

    // Command Identifiers
    public static final String CMD_OPEN_HOME = "CMD_OPEN_HOME";
    public static final String CMD_OPEN_PROFILE = "CMD_OPEN_PROFILE";
    public static final String CMD_OPEN_SETTINGS = "CMD_OPEN_SETTINGS";
    public static final String CMD_OPEN_MESSAGES = "CMD_OPEN_MESSAGES";
    public static final String CMD_OPEN_COMMUNICATION = "CMD_OPEN_COMMUNICATION";
    public static final String CMD_OPEN_CAREGIVER = "CMD_OPEN_CAREGIVER";
    public static final String CMD_OPEN_CAREGIVER_CONNECTION = "CMD_OPEN_CAREGIVER_CONNECTION";
    public static final String CMD_OPEN_VOICE_RECORDER = "CMD_OPEN_VOICE_RECORDER";
    public static final String CMD_OPEN_VOICE_MESSAGE = "CMD_OPEN_VOICE_MESSAGE";
    public static final String CMD_RECORD_VOICE_MESSAGE = "CMD_RECORD_VOICE_MESSAGE";
    public static final String CMD_OPEN_VOICE_CALL = "CMD_OPEN_VOICE_CALL";
    public static final String CMD_CALL_CAREGIVER = CMD_OPEN_VOICE_CALL;
    public static final String CMD_OPEN_EMERGENCY = "CMD_OPEN_EMERGENCY";
    public static final String CMD_OPEN_EMERGENCY_ALERT = "CMD_OPEN_EMERGENCY_ALERT";
    public static final String CMD_START_LISTENING = "CMD_START_LISTENING";
    public static final String CMD_STOP_LISTENING = "CMD_STOP_LISTENING";
    public static final String CMD_END_CALL = "CMD_END_CALL";
    public static final String CMD_ACCEPT_CALL = "CMD_ACCEPT_CALL";
    public static final String CMD_RECEIVE_CALL = CMD_ACCEPT_CALL;
    public static final String CMD_SEND_MESSAGE = "CMD_SEND_MESSAGE";
    public static final String CMD_READ_MESSAGES = "CMD_READ_MESSAGES";
    public static final String CMD_MESSAGE_COUNT = "CMD_MESSAGE_COUNT";
    public static final String CMD_REPEAT_MESSAGE = "CMD_REPEAT_MESSAGE";
    public static final String CMD_READ_NOTIFICATIONS = "CMD_READ_NOTIFICATIONS";
    public static final String CMD_GO_BACK = "CMD_GO_BACK";
    public static final String CMD_CONFIRM_YES = "CMD_CONFIRM_YES";
    public static final String CMD_CONFIRM_NO = "CMD_CONFIRM_NO";
    public static final String CMD_OPEN_APP = "CMD_OPEN_APP";
    public static final String CMD_UNKNOWN = "CMD_UNKNOWN";

    // Intent Identifiers
    public static final String INTENT_OPEN_HOME = "INTENT_OPEN_HOME";
    public static final String INTENT_OPEN_APP = "INTENT_OPEN_APP";
    public static final String INTENT_OPEN_PROFILE = "INTENT_OPEN_PROFILE";
    public static final String INTENT_OPEN_SETTINGS = "INTENT_OPEN_SETTINGS";
    public static final String INTENT_OPEN_MESSAGES = "INTENT_OPEN_MESSAGES";
    public static final String INTENT_SEND_MESSAGE = "INTENT_SEND_MESSAGE";
    public static final String INTENT_READ_MESSAGES = "INTENT_READ_MESSAGES";
    public static final String INTENT_MESSAGE_COUNT = "INTENT_MESSAGE_COUNT";
    public static final String INTENT_REPEAT_MESSAGE = "INTENT_REPEAT_MESSAGE";
    public static final String INTENT_READ_NOTIFICATIONS = "INTENT_READ_NOTIFICATIONS";
    public static final String INTENT_GO_BACK = "INTENT_GO_BACK";
    public static final String INTENT_OPEN_COMMUNICATION = "INTENT_OPEN_COMMUNICATION";
    public static final String INTENT_OPEN_CAREGIVER = "INTENT_OPEN_CAREGIVER";
    public static final String INTENT_OPEN_CAREGIVER_CONNECTION = "INTENT_OPEN_CAREGIVER_CONNECTION";
    public static final String INTENT_OPEN_VOICE_RECORDER = "INTENT_OPEN_VOICE_RECORDER";
    public static final String INTENT_OPEN_VOICE_MESSAGE = "INTENT_OPEN_VOICE_MESSAGE";
    public static final String INTENT_RECORD_VOICE_MESSAGE = "INTENT_RECORD_VOICE_MESSAGE";
    public static final String INTENT_OPEN_VOICE_CALL = "INTENT_OPEN_VOICE_CALL";
    public static final String INTENT_CALL_CAREGIVER = INTENT_OPEN_VOICE_CALL;
    public static final String INTENT_OPEN_EMERGENCY = "INTENT_OPEN_EMERGENCY";
    public static final String INTENT_OPEN_EMERGENCY_ALERT = "INTENT_OPEN_EMERGENCY_ALERT";
    public static final String INTENT_START_LISTENING = "INTENT_START_LISTENING";
    public static final String INTENT_STOP_LISTENING = "INTENT_STOP_LISTENING";
    public static final String INTENT_END_CALL = "INTENT_END_CALL";
    public static final String INTENT_ACCEPT_CALL = "INTENT_ACCEPT_CALL";
    public static final String INTENT_RECEIVE_CALL = INTENT_ACCEPT_CALL;
    public static final String INTENT_CONFIRM_YES = "INTENT_CONFIRM_YES";
    public static final String INTENT_CONFIRM_NO = "INTENT_CONFIRM_NO";
    public static final String INTENT_UNKNOWN = "INTENT_UNKNOWN";

    // Confirmation & Clarification Prompts
    public static final String PROMPT_CONFIRM_VOICE_CALL = "Do you want to call your caregiver?";
    public static final String PROMPT_CONFIRM_EMERGENCY_ALERT = "Do you want to send an emergency alert?";
    public static final String PROMPT_SAY_MESSAGE = "Please say your message.";
    public static final String PROMPT_CLARIFY_HELP = "Do you want me to call your caregiver or send an emergency alert?";
    public static final String PROMPT_CLARIFY_CALL = "Do you want me to call your caregiver?";
    public static final String PROMPT_CLARIFY_SEND_MESSAGE = "Do you want to send a message to your caregiver?";
    public static final String RESPONSE_CALL_CONFIRMED = "Calling your caregiver.";
    public static final String RESPONSE_EMERGENCY_CONFIRMED = "Opening emergency.";
    public static final String RESPONSE_ACTION_CANCELLED = "Action cancelled.";
    public static final String RESPONSE_MESSAGE_CANCELLED = "Message cancelled.";
    public static final String PROMPT_SPEAK_VOICE_MESSAGE = "Please speak the message you want to send to your caregiver.";
    public static final String PROMPT_CONFIRM_SEND_VOICE_MESSAGE = "Your voice message is ready. Do you want to send it to your caregiver?";
    public static final String RESPONSE_VOICE_MESSAGE_SENT = "Voice message sent.";
    public static final String RESPONSE_VOICE_MESSAGE_CANCELLED = "Voice message cancelled.";
    public static final String RESPONSE_VOICE_MESSAGE_FAILED = "Unable to send voice message. Please try again.";

    // Standard Spoken Feedback Responses
    public static final String RESPONSE_UNKNOWN = "Sorry, I did not understand that command.";
    public static final String RESPONSE_OPEN_HOME = "Home is open.";
    public static final String RESPONSE_OPEN_APP = "Speech Assistant is open.";
    public static final String RESPONSE_OPEN_PROFILE = "Profile is open.";
    public static final String RESPONSE_OPEN_SETTINGS = "Settings is open.";
    public static final String RESPONSE_OPEN_MESSAGES = "Messages is open.";
    public static final String RESPONSE_OPEN_COMMUNICATION = "Messages is open.";
    public static final String RESPONSE_MESSAGE_SENT = "Message sent.";
    public static final String RESPONSE_NO_NEW_MESSAGES = "No new messages.";
    public static final String RESPONSE_NO_MESSAGE_TO_REPEAT = "No message to repeat.";
    public static final String RESPONSE_NO_NOTIFICATIONS = "No new notifications.";
    public static final String RESPONSE_GOING_BACK = "Going back.";
    public static final String RESPONSE_ALREADY_FIRST_SCREEN = "Already at the first screen.";
    public static final String PREFIX_CAREGIVER_SAYS = "Your caregiver says: ";
    public static final String RESPONSE_NO_CAREGIVER = "No caregiver is connected.";
    public static final String RESPONSE_CALL_ACTIVE = "A call is active.";
    public static final String RESPONSE_NO_MESSAGE_HEARD = "I didn't hear a message.";
    public static final String RESPONSE_OPEN_CAREGIVER = "Opening Caregiver.";
    public static final String RESPONSE_OPEN_CAREGIVER_CONNECTION = "Opening Caregiver Connection.";
    public static final String RESPONSE_OPEN_VOICE_RECORDER = "Opening Voice Recorder.";
    public static final String RESPONSE_OPEN_VOICE_MESSAGE = "Opening Voice Message.";
    public static final String RESPONSE_OPEN_VOICE_CALL = "Calling caregiver.";
    public static final String RESPONSE_OPEN_EMERGENCY = "Emergency alert sent.";
    public static final String RESPONSE_OPEN_EMERGENCY_ALERT = "Emergency alert sent.";
    public static final String RESPONSE_START_LISTENING = "Listening.";
    public static final String RESPONSE_STOP_LISTENING = "Listening stopped.";
    public static final String RESPONSE_END_CALL = "Call ended.";
    public static final String RESPONSE_ACCEPT_CALL = "Receiving call.";
    public static final String RESPONSE_ROLE_UNAUTHORIZED = "That function is not available for your user type.";

    // Microphone Conflict Messages
    public static final String MESSAGE_CONFLICT_CALL = "Voice assistant is unavailable during a call.";
    public static final String MESSAGE_CONFLICT_RECORDING = "Please stop recording first.";
}
