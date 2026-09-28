# MASTER BLIND USER VOICE COMMAND DICTIONARY & AUDIT SPECIFICATION

**Project**: Kannada Speech Assistant (Android)  
**Target Module**: Blind User Module (`BlindUserDashboardActivity` & `AppVoiceAssistant`)  
**Document Version**: 1.0 (Audit & Specification Only — Step 1)  
**Author**: Antigravity Technical Assistant  
**Date**: September 2026  
**Status**: READ-ONLY AUDIT & FORMAL SPECIFICATION (Zero Source Code Modified)  

---

## 1. EXECUTIVE SUMMARY & CURRENT ARCHITECTURE

### 1.1 Architecture Overview
The Blind User Voice Assistant is implemented as a modular on-device and cloud-assisted voice processing pipeline:

```
[Hardware Microphone]
       │
       ▼
[WakeWordManager (Offline Vosk ASR, model-en-us)]
       │ (Keyword Spotted: "assistant" or "hey assistant")
       ▼
[Microphone Conflict Check & Yield] ──► Checks active WebRTC call / Voice recording
       │
       ▼
[Android SpeechRecognizer (STT)] ──► Configured with session locale (kn-IN, hi-IN, ml-IN, en-IN)
       │
       ▼
[VoiceIntentMatcher] ──► Punctuation removal, script normalization, token parsing
       │
       ▼
[VoiceCommandProcessor] ──► Role authorization (ROLE_BLIND_USER check) & VoiceIntentType mapping
       │
       ▼
[AppVoiceAssistant / VoiceActionDispatcher] ──► Executes existing Activity/Firestore/WebRTC feature
       │
       ▼
[VoiceLanguageConfig + Google TTS] ──► Multilingual localized spoken audio feedback
```

### 1.2 Audited Source Files
1. `app/src/main/java/com/kannada/speechassistant/voiceassistant/AppVoiceAssistant.java`
2. `app/src/main/java/com/kannada/speechassistant/voiceassistant/WakeWordManager.java`
3. `app/src/main/java/com/kannada/speechassistant/voiceassistant/VoiceIntentMatcher.java`
4. `app/src/main/java/com/kannada/speechassistant/voiceassistant/VoiceActionDispatcher.java`
5. `app/src/main/java/com/kannada/speechassistant/voiceassistant/VoiceLanguageConfig.java`
6. `app/src/main/java/com/kannada/speechassistant/voiceassistant/VoiceIntentType.java`
7. `app/src/main/java/com/kannada/speechassistant/voiceassistant/VoiceCommandProcessor.java`
8. `app/src/main/java/com/kannada/speechassistant/voiceassistant/VoiceCommandConstants.java`
9. `app/src/main/java/com/kannada/speechassistant/BlindUserDashboardActivity.java`
10. `app/src/main/java/com/kannada/speechassistant/VoiceCallActivity.java`
11. `app/src/main/java/com/kannada/speechassistant/IncomingCallActivity.java`
12. `app/src/main/java/com/kannada/speechassistant/EmergencyActivity.java`
13. `app/src/main/java/com/kannada/speechassistant/NotificationService.java`

---

## PART A — MASTER INTENT AUDIT

The following table documents every existing voice-controlled or related function in the Blind User Module.

| # | Intent Name | Existing Enum / Command ID | Method / Class Executing Action | Required Role | Dependencies | Interaction Type | Confirmation | Safe by Voice? | Current Limitations |
|---|---|---|---|---|---|---|---|---|---|
| 1 | **CALL_CAREGIVER** | `VoiceIntentType.CALL_CAREGIVER` / `CMD_OPEN_VOICE_CALL` | `BlindUserDashboardActivity.startCaregiverVoiceCall()` | `ROLE_BLIND_USER` | WebRTC, Firestore `voice_calls`, `connectedCaregiverUid` | Immediate | Optional (currently direct) | Yes | Requires active connected caregiver UID and working TURN relay for 4G/5G carrier NAT. |
| 2 | **END_CALL** | `VoiceIntentType.END_CALL` / `CMD_END_CALL` | `VoiceCallManager.getInstance(context).endCall(...)` | All roles | WebRTC PeerConnection, Firestore call doc | Immediate | No | Yes | Only acts when an active call is actually ongoing; responds "No active call" otherwise. |
| 3 | **SEND_MESSAGE** | `VoiceIntentType.SEND_MESSAGE` / `CMD_SEND_MESSAGE` | `AppVoiceAssistant.handleSendMessageFlow()`, `BlindUserDashboardActivity.sendTextMessageToCaregiver()` | All roles / `ROLE_BLIND_USER` | Android STT, Firestore `caregiver_messages` | Multi-step (Prompt $\to$ Dictate $\to$ Send) | Implicit (user speaks message) | Yes | Text messages only. Does not record voice audio messages. |
| 4 | **READ_MESSAGES** | `VoiceIntentType.READ_MESSAGES` / `CMD_READ_MESSAGES` | `AppVoiceAssistant.handleReadMessagesFlow()`, `BlindUserDashboardActivity.getLatestCaregiverTextMessage()` | All roles / `ROLE_BLIND_USER` | Firestore `caregiver_messages`, Google TTS | Immediate | No | Yes | Only reads text messages. Audio voice recordings (.m4a) sent by caregiver are ignored. |
| 5 | **MESSAGE_COUNT** | `VoiceIntentType.MESSAGE_COUNT` / `CMD_MESSAGE_COUNT` | `AppVoiceAssistant.handleMessageCountFlow()`, `BlindUserDashboardActivity.getCaregiverTextMessageCount()` | All roles / `ROLE_BLIND_USER` | Firestore `caregiver_messages`, Google TTS | Immediate | No | Yes | Counts text messages from caregiver only. |
| 6 | **REPEAT_MESSAGE** | `VoiceIntentType.REPEAT_MESSAGE` / `CMD_REPEAT_MESSAGE` | `AppVoiceAssistant.handleRepeatMessageFlow()` | All roles / `ROLE_BLIND_USER` | In-memory cache `lastReadCaregiverMessage`, Google TTS | Immediate | No | Yes | Only works if a caregiver message was already read during the session; otherwise speaks "No message to repeat." |
| 7 | **READ_NOTIFICATIONS** | `VoiceIntentType.READ_NOTIFICATIONS` / `CMD_READ_NOTIFICATIONS` | `AppVoiceAssistant.handleReadNotificationsFlow()`, `NotificationService.getReadableNotifications()` | All roles | `NotificationService`, Google TTS | Immediate | No | Yes | Reads at most 3 recent notifications. |
| 8 | **EMERGENCY_SOS** | `VoiceIntentType.OPEN_EMERGENCY`, `OPEN_EMERGENCY_ALERT` / `CMD_OPEN_EMERGENCY` | `BlindUserDashboardActivity.triggerEmergencySOS()`, `EmergencyActivity` | All roles | Firestore `emergencies`, MediaPlayer (Siren), Vibrator | Immediate | Recommended YES (Currently direct with 2s debounce) | Safety Critical | Directly launches siren and sends alert; single-word "help" is blocked, but compound "emergency help" fires immediately. |
| 9 | **OPEN_HOME** | `VoiceIntentType.OPEN_HOME` / `CMD_OPEN_HOME` | `VoiceActionDispatcher.handleOpenHome()` | All roles | `BottomNavigationView` (`R.id.nav_home`), UI views | Immediate | No | Yes | Visual tab switch; provides spoken "Home is open." |
| 10 | **OPEN_PROFILE** | `VoiceIntentType.OPEN_PROFILE` / `CMD_OPEN_PROFILE` | `VoiceActionDispatcher.handleOpenProfile()` | `PROFILE_ROLES` | `BottomNavigationView` (`R.id.nav_profile`), UI views | Immediate | No | Yes | Visual tab switch; provides spoken "Profile is open." |
| 11 | **OPEN_SETTINGS** | `VoiceIntentType.OPEN_SETTINGS` / `CMD_OPEN_SETTINGS` | `VoiceActionDispatcher.handleOpenSettings()` | All roles | `R.id.btnBellNotificationSettings` click | Immediate | No | Yes | Opens notification settings dialog. |
| 12 | **OPEN_MESSAGES** | `VoiceIntentType.OPEN_MESSAGES`, `OPEN_COMMUNICATION` / `CMD_OPEN_MESSAGES` | `VoiceActionDispatcher.handleOpenMessages()` | All roles | `R.id.btnOpenFullChat`, `ChatActivity` | Immediate | No | Yes | Opens the full `ChatActivity` screen. |
| 13 | **OPEN_CAREGIVER** | `VoiceIntentType.OPEN_CAREGIVER`, `OPEN_CAREGIVER_CONNECTION` / `CMD_OPEN_CAREGIVER` | `VoiceActionDispatcher.handleOpenCaregiver()` | All roles | `BottomNavigationView` (`R.id.nav_caregiver`) | Immediate | No | Yes | Switches to caregiver connection tab. |
| 14 | **GO_BACK** | `VoiceIntentType.GO_BACK` / `CMD_GO_BACK` | `AppVoiceAssistant.handleGoBackFlow()`, `VoiceActionDispatcher.handleGoBack()` | All roles | UI Navigation hierarchy | Immediate | No | Yes | Returns to Home if on a sub-tab; says "Already at the first screen." if already on Home. |
| 15 | **OPEN_VOICE_RECORDER** | `VoiceIntentType.OPEN_VOICE_RECORDER`, `OPEN_VOICE_MESSAGE` / `CMD_OPEN_VOICE_RECORDER` | `VoiceActionDispatcher.handleOpenVoiceRecorder()` | `ROLE_BLIND_USER` | `R.id.cardVoiceMessage` focus | Immediate | No | Yes | Focuses/scrolls to voice recorder card. DOES NOT record audio automatically. |
| 16 | **START_VOICE_RECORDING** | None (NOT IMPLEMENTED) | UI click `btnMicVoiceMessage` only | `ROLE_BLIND_USER` | MediaRecorder, local AAC file | None | No | No | **NOT CURRENTLY IMPLEMENTED VIA VOICE**. Only triggered via UI button press. |
| 17 | **STOP_VOICE_RECORDING** | None (NOT IMPLEMENTED) | UI click `btnStopRecording` only | `ROLE_BLIND_USER` | MediaRecorder stop, Firebase/Supabase upload | None | No | No | **NOT CURRENTLY IMPLEMENTED VIA VOICE**. Only triggered via UI button press. |
| 18 | **SEND_VOICE_MESSAGE** | None (NOT IMPLEMENTED) | UI click `stopAndSendVoiceRecording()` | `ROLE_BLIND_USER` | Storage upload + Firestore doc | None | No | No | **NOT CURRENTLY IMPLEMENTED VIA VOICE**. |
| 19 | **STOP_ASSISTANT** | `VoiceIntentType.STOP_LISTENING` / `CMD_STOP_LISTENING` | `AppVoiceAssistant.stopListening()` | All roles | `SpeechRecognizer` | Immediate | No | Yes | Stops listening and releases microphone. |
| 20 | **RESUME_ASSISTANT** | `VoiceIntentType.START_LISTENING` / `CMD_START_LISTENING` | `AppVoiceAssistant.startListeningFlow()` | All roles | `SpeechRecognizer` | Immediate | No | Yes | Re-opens microphone for commands. |
| 21 | **CONFIRM_YES** | `VoiceIntentType.CONFIRM_YES` / `CMD_CONFIRM_YES` | `AppVoiceAssistant.handleConfirmationResponseFlow()` | All roles | Internal confirmation latch | Immediate | N/A | Yes | Evaluated only when `pendingConfirmationCommand != null`. |
| 22 | **CONFIRM_NO** | `VoiceIntentType.CONFIRM_NO` / `CMD_CONFIRM_NO` | `AppVoiceAssistant.handleConfirmationResponseFlow()` | All roles | Internal confirmation latch | Immediate | N/A | Yes | Cancels pending confirmation action. |
| 23 | **OPEN_APP** | `VoiceIntentType.OPEN_APP` / `CMD_OPEN_APP` | `BackgroundVoiceLaunchService` / `VoiceActionDispatcher` | All roles / `ROLE_BLIND_USER` | `BackgroundVoiceLaunchService`, Vosk offline recognizer, Full-Screen Intent | Immediate | No | Yes | **Works Outside App / Closed State**: Spoken in background ("Hey Assistant, open speech assistant app") launches app. ONLY this command works outside the app; all other commands are ignored while closed. |

---

## PART B & C — MULTILINGUAL COMMAND SPECIFICATION & FUNCTION GROUPS

Below is the verified specification for every existing and required function, complete with natural vocabulary, dialogue flows, and phrase variations across English, Kannada, Hindi, and Malayalam.

---

### GROUP 1: CAREGIVER CALLING
* **Intent Identifier**: `CALL_CAREGIVER`
* **Underlying Command**: `CMD_OPEN_VOICE_CALL`
* **Action Target**: `BlindUserDashboardActivity.startCaregiverVoiceCall()` $\to$ `VoiceCallActivity` (WebRTC P2P/Relay)
* **Safety Class**: `COMMUNICATION_ACTION`

#### Spoken Dialog Flow:
* User: *"Assistant, call my caregiver"*
* Action: WebRTC voice call initializes, UI transitions to `VoiceCallActivity`.
* TTS Feedback: Localized confirmation:
  * English: *"Calling caregiver."*
  * Kannada: *"ಕೇರ್‌ಗಿವರ್‌ಗೆ ಕರೆ ಮಾಡಲಾಗುತ್ತಿದೆ."*
  * Hindi: *"केयरगिवर को कॉल किया जा रहा है।"*
  * Malayalam: *"കെയർഗിവറെ വിളിക്കുന്നു."*

#### Natural Sentence Patterns:
* **English**:
  1. "Call caregiver"
  2. "Call my caregiver"
  3. "Please call my caregiver"
  4. "Phone my caregiver"
  5. "Start a voice call with my caregiver"
  6. "Connect me to my caregiver"
  7. "I want to talk to my caregiver"
  8. "Make a call to my helper"
  9. "Call my caretaker"
  10. "Voice call caregiver"
* **Kannada (ಕನ್ನಡ)**:
  1. "ಕೇರ್ಗಿವರ್ಗೆ ಕರೆ ಮಾಡಿ"
  2. "ನನ್ನ ಕೇರ್ಗಿವರ್ಗೆ ಕರೆ ಮಾಡಿ"
  3. "ದಯವಿಟ್ಟು ಕೇರ್ಗಿವರ್ಗೆ ಕಾಲ್ ಮಾಡಿ"
  4. "ಆರೈಕೆದಾರರಿಗೆ ಕರೆ ಮಾಡಿ"
  5. "ನನ್ನ ಆರೈಕೆದಾರರಿಗೆ ಫೋನ್ ಮಾಡಿ"
  6. "ಸಹಾಯಕನಿಗೆ ಕರೆ ಮಾಡಿ"
  7. "ನನ್ನ ಸಹಾಯಕರಿಗೆ ಕಾಲ್ ಮಾಡಿ"
  8. "ಪಾಲಕರಿಗೆ ಕರೆ ಮಾಡಿ"
  9. "ಕೇರ್ಗಿವರ್ ಜೊತೆ ಮಾತನಾಡಬೇಕು"
  10. "ಧ್ವನಿ ಕರೆ ಪ್ರಾರಂಭಿಸಿ"
* **Hindi (हिन्दी)**:
  1. "केयरगिवर को कॉल करो"
  2. "मेरे केयरगिवर को फोन लगाओ"
  3. "कृपया केयरगिवर को कॉल करें"
  4. "सहायक को कॉल करो"
  5. "मेरे सहायक से बात कराओ"
  6. "देखभाल करने वाले को फोन करो"
  7. "केयरटेकर को कॉल लगाओ"
  8. "वॉइस कॉल शुरू करो"
  9. "मुझे केयरगिवर से बात करनी है"
  10. "केयरगिवर को फोन मिलाओ"
* **Malayalam (മലയാളം)**:
  1. "കെയർഗിവറെ വിളിക്കൂ"
  2. "എന്റെ കെയർഗിവറെ വിളിക്കുക"
  3. "ദയവായി കെയർഗിവറെ ഫോൺ ചെയ്യൂ"
  4. "സഹായിയെ വിളിക്കൂ"
  5. "എന്റെ സഹായിയെ കോൾ ചെയ്യൂ"
  6. "പരിചരിക്കുന്നയാളെ വിളിക്കുക"
  7. "കെയർടേക്കറെ ഫോൺ ചെയ്യുക"
  8. "വോയ്സ് കോൾ തുടങ്ങുക"
  9. "എനിക്ക് കെയർഗിവറോട് സംസാരിക്കണം"
  10. "കെയർഗിവർക്ക് കോൾ ചെയ്യൂ"

---

### GROUP 2: END / DISCONNECT CALL
* **Intent Identifier**: `END_CALL`
* **Underlying Command**: `CMD_END_CALL`
* **Action Target**: `VoiceCallManager.getInstance(context).endCall(...)`
* **Safety Class**: `COMMUNICATION_ACTION`

#### Spoken Dialog Flow:
* User: *"Assistant, end the call"*
* Action: WebRTC peer connection closed, Firestore call state set to `ended`.
* TTS Feedback:
  * English: *"Call ended."* (or *"No active call."* if idle)
  * Kannada: *"ಕರೆ ಮುಕ್ತಾಯವಾಗಿದೆ."*
  * Hindi: *"कॉल समाप्त हुई।"*
  * Malayalam: *"കോൾ അവസാനിച്ചു."*

#### Natural Sentence Patterns:
* **English**:
  1. "End call"
  2. "End the call"
  3. "Hang up"
  4. "Hang up the call"
  5. "Disconnect call"
  6. "Cut the call"
  7. "Stop call"
  8. "Close call"
  9. "Finish the call"
  10. "Drop the call"
* **Kannada (ಕನ್ನಡ)**:
  1. "ಕರೆ ಮುಗಿಸಿ"
  2. "ಕರೆಯನ್ನು ಮುಗಿಸಿ"
  3. "ಕರೆ ಕೊನೆಗೊಳಿಸಿ"
  4. "ಕರೆಯನ್ನು ಕೊನೆಗೊಳಿಸಿ"
  5. "ಕರೆ ಕಟ್ ಮಾಡಿ"
  6. "ಫೋನ್ ಕಟ್ ಮಾಡಿ"
  7. "ಕಾಲ್ ಮುಗಿಸಿ"
  8. "ಈ ಕರೆ ನಿಲ್ಲಿಸಿ"
  9. "ಕರೆಯನ್ನು ನಿಲ್ಲಿಸಿ"
  10. "ಫೋನ್ ಇಡಿ"
* **Hindi (हिन्दी)**:
  1. "कॉल समाप्त करो"
  2. "कॉल काटो"
  3. "फ़ोन काटो"
  4. "कॉल बंद करो"
  5. "कॉल खत्म करो"
  6. "बातचीत समाप्त करो"
  7. "कॉल डिस्कनेक्ट करो"
  8. "फ़ोन रख दो"
  9. "कॉल रोक दो"
  10. "कॉल काटो प्लीज"
* **Malayalam (മലയാളം)**:
  1. "കോൾ അവസാനിപ്പിക്കുക"
  2. "കോൾ കട്ട് ചെയ്യുക"
  3. "ഫോൺ കട്ട് ചെയ്യുക"
  4. "കോൾ നിർത്തുക"
  5. "ഫോൺ വെക്കുക"
  6. "കോൾ വിച്ഛേദിക്കുക"
  7. "സംഭാഷണം അവസാനിപ്പിക്കൂ"
  8. "കോൾ കട്ട് ചെയ്യൂ"
  9. "ഫോൺ കട്ട് ചെയ്യൂ"
  10. "കോൾ എൻഡ് ചെയ്യുക"

---

### GROUP 3: SEND TEXT MESSAGE
* **Intent Identifier**: `SEND_MESSAGE`
* **Underlying Command**: `CMD_SEND_MESSAGE`
* **Action Target**: `AppVoiceAssistant.handleSendMessageFlow()` $\to$ `BlindUserDashboardActivity.sendTextMessageToCaregiver()`
* **Safety Class**: `COMMUNICATION_ACTION`

#### Spoken Dialog Flow:
1. User: *"Send a message to my caregiver"*
2. Assistant: *"Please say your message."* (TTS in user's language)
3. User: *"I need some water"*
4. Assistant: *"Message sent."* (Writes to Firestore `caregiver_messages`, confirms with TTS)

#### Natural Sentence Patterns:
* **English**:
  1. "Send message"
  2. "Send a message"
  3. "Send message to caregiver"
  4. "Send a message to my caregiver"
  5. "Message caregiver"
  6. "Message my helper"
  7. "Tell my caregiver something"
  8. "Send this to my caregiver"
  9. "Write a message to caregiver"
  10. "Send a text to caregiver"
* **Kannada (ಕನ್ನಡ)**:
  1. "ಸಂದೇಶ ಕಳುಹಿಸಿ"
  2. "ಸಂದೇಶ ಕಳಿಸಿ"
  3. "ಕೇರ್ಗಿವರ್ಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ"
  4. "ನನ್ನ ಕೇರ್ಗಿವರ್ಗೆ ಸಂದೇಶ ಕಳಿಸಿ"
  5. "ಆರೈಕೆದಾರರಿಗೆ ಸಂದೇಶ ಕಳುಹಿಸಿ"
  6. "ಸಹಾಯಕನಿಗೆ ಮೆಸೇಜ್ ಕಳುಹಿಸಿ"
  7. "ಪಾಲಕರಿಗೆ ಸಂದೇಶ ಕಳಿಸಿ"
  8. "ಕೇರ್ಗಿವರ್ಗೆ ಒಂದು ಸಂದೇಶ ಕಳುಹಿಸಿ"
  9. "ನನ್ನ ಸಹಾಯಕನಿಗೆ ಒಂದು ಮೆಸೇಜ್ ಕಳಿಸಿ"
  10. "ಸಂದೇಶ ಕಳುಹಿಸಬೇಕು"
* **Hindi (हिन्दी)**:
  1. "संदेश भेजो"
  2. "मैसेज भेजो"
  3. "केयरगिवर को संदेश भेजो"
  4. "मेरे केयरगिवर को मैसेज भेजो"
  5. "सहायक को संदेश भेजो"
  6. "देखभाल करने वाले को मैसेज करो"
  7. "केयरटेकर को एक संदेश भेजो"
  8. "केयरगिवर को कुछ बताना है"
  9. "एक संदेश भेजना है"
  10. "कृपया केयरगिवर को मैसेज भेजें"
* **Malayalam (മലയാളം)**:
  1. "സന്ദേശം അയക്കൂ"
  2. "മെസേജ് അയക്കൂ"
  3. "കെയർഗിവർക്ക് സന്ദേശം അയക്കുക"
  4. "എന്റെ കെയർഗിവർക്ക് മെസേജ് അയക്കൂ"
  5. "സഹായിക്ക് സന്ദേശം അയക്കൂ"
  6. "പരിചരിക്കുന്നയാൾക്ക് സന്ദേശം അയക്കുക"
  7. "കെയർടേക്കർക്ക് മെസേജ് അയക്കൂ"
  8. "കെയർഗിവറോട് ഒരു കാര്യം പറയണം"
  9. "ഒരു സന്ദേശം അയക്കണം"
  10. "ദയവായി കെയർഗിവർക്ക് സന്ദേശം അയക്കൂ"

---

### GROUP 4: READ CAREGIVER MESSAGES
* **Intent Identifier**: `READ_MESSAGES`
* **Underlying Command**: `CMD_READ_MESSAGES`
* **Action Target**: `AppVoiceAssistant.handleReadMessagesFlow()` $\to$ `BlindUserDashboardActivity.getLatestCaregiverTextMessage()`
* **Safety Class**: `SAFE_IMMEDIATE`

#### Spoken Dialog Flow:
* User: *"Assistant, read caregiver message"*
* TTS Feedback:
  * If found: *"Your caregiver says: [Message content]"*
  * If none: *"No new messages."* (or localized equivalent)

#### Natural Sentence Patterns:
* **English**:
  1. "Read messages"
  2. "Read my messages"
  3. "Read caregiver message"
  4. "Read the latest message"
  5. "Check messages"
  6. "Check my messages"
  7. "Tell me what my caregiver said"
  8. "What did my caregiver say?"
  9. "Did my caregiver send a message?"
  10. "Read caregiver messages to me"
* **Kannada (ಕನ್ನಡ)**:
  1. "ಸಂದೇಶಗಳನ್ನು ಓದಿ"
  2. "ನನ್ನ ಸಂದೇಶಗಳನ್ನು ಓದಿ"
  3. "ಕೇರ್ಗಿವರ್ ಸಂದೇಶ ಓದಿ"
  4. "ಕೇರ್ಗಿವರ್ ಸಂದೇಶವನ್ನು ಓದಿ"
  5. "ಆರೈಕೆದಾರರ ಸಂದೇಶ ಓದಿ"
  6. "ಸಹಾಯಕನ ಸಂದೇಶ ಹೇಳಿ"
  7. "ಕೇರ್ಗಿವರ್ ಏನು ಹೇಳಿದ್ದಾರೆ?"
  8. "ಹೊಸ ಸಂದೇಶಗಳನ್ನು ಓದಿ"
  9. "ನನಗೆ ಬಂದ ಸಂದೇಶಗಳನ್ನು ಓದಿ"
  10. "ಮೆಸೇಜ್ ಓದಿ ಹೇಳಿ"
* **Hindi (हिन्दी)**:
  1. "संदेश पढ़ो"
  2. "मेरे संदेश पढ़ो"
  3. "केयरगिवर का संदेश पढ़ो"
  4. "केयरगिवर का मैसेज सुनाओ"
  5. "सहायक का संदेश सुनाओ"
  6. "मैसेज चेक करो"
  7. "केयरगिवर ने क्या कहा?"
  8. "नया संदेश पढ़कर सुनाओ"
  9. "मुझे संदेश पढ़कर बताओ"
  10. "क्या कोई नया मैसेज है?"
* **Malayalam (മലയാളം)**:
  1. "സന്ദേശങ്ങൾ വായിക്കൂ"
  2. "എന്റെ സന്ദേശങ്ങൾ വായിക്കുക"
  3. "കെയർഗിവറുടെ സന്ദേശം വായിക്കൂ"
  4. "കെയർഗിവറുടെ മെസേജ് കേൾപ്പിക്കൂ"
  5. "സഹായിയുടെ സന്ദേശം വായിക്കൂ"
  6. "സന്ദേശങ്ങൾ പരിശോധിക്കുക"
  7. "കെയർഗിവർ എന്താണ് പറഞ്ഞത്?"
  8. "പുതിയ സന്ദേശം വായിച്ചു കേൾപ്പിക്കൂ"
  9. "എനിക്ക് വന്ന മെസേജ് വായിക്കൂ"
  10. "കെയർടേക്കറുടെ സന്ദേശം പറയൂ"

---

### GROUP 5: MESSAGE COUNT
* **Intent Identifier**: `MESSAGE_COUNT`
* **Underlying Command**: `CMD_MESSAGE_COUNT`
* **Action Target**: `AppVoiceAssistant.handleMessageCountFlow()` $\to$ `BlindUserDashboardActivity.getCaregiverTextMessageCount()`
* **Safety Class**: `SAFE_IMMEDIATE`

#### Spoken Dialog Flow:
* User: *"How many messages do I have?"*
* TTS Feedback:
  * If count > 0: *"You have 3 messages."* (or localized equivalent)
  * If count == 0: *"No new messages."*

#### Natural Sentence Patterns:
* **English**:
  1. "How many messages"
  2. "Count messages"
  3. "Count my messages"
  4. "How many messages do I have"
  5. "How many caregiver messages"
  6. "How many new messages"
  7. "Check message count"
  8. "Tell me how many messages I got"
  9. "Do I have any messages?"
  10. "Total messages"
* **Kannada (ಕನ್ನಡ)**:
  1. "ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ?"
  2. "ಸಂದೇಶಗಳನ್ನು ಎಣಿಸಿ"
  3. "ನನ್ನ ಬಳಿ ಎಷ್ಟು ಸಂದೇಶಗಳಿವೆ?"
  4. "ಕೇರ್ಗಿವರ್ ಎಷ್ಟು ಸಂದೇಶ ಕಳುಹಿಸಿದ್ದಾರೆ?"
  5. "ಆರೈಕೆದಾರರಿಂದ ಎಷ್ಟು ಮೆಸೇಜ್ ಬಂದಿದೆ?"
  6. "ಎಷ್ಟು ಹೊಸ ಸಂದೇಶಗಳು ಬಂದಿವೆ?"
  7. "ಮೆಸೇಜ್ ಕೌಂಟ್ ಎಷ್ಟು?"
  8. "ನನಗೆ ಸಂದೇಶಗಳು ಬಂದಿವೆಯೇ?"
  9. "ಸಂದೇಶಗಳ ಸಂಖ್ಯೆ ಎಷ್ಟು?"
  10. "ಸಹಾಯಕ ಎಷ್ಟು ಸಂದೇಶ ಕಳುಹಿಸಿದ್ದಾನೆ?"
* **Hindi (हिन्दी)**:
  1. "कितने संदेश हैं?"
  2. "संदेश गिनो"
  3. "मेरे कितने मैसेज हैं?"
  4. "केयरगिवर के कितने संदेश आए हैं?"
  5. "कितने नए मैसेज आए हैं?"
  6. "मैसेज की गिनती बताओ"
  7. "क्या मेरे पास कोई संदेश है?"
  8. "सहायक से कितने संदेश हैं?"
  9. "कुल कितने मैसेज हैं?"
  10. "कितने संदेश बाकी हैं?"
* **Malayalam (മലയാളം)**:
  1. "എത്ര സന്ദേശങ്ങളുണ്ട്?"
  2. "സന്ദേശങ്ങൾ എണ്ണൂ"
  3. "എനിക്ക് എത്ര മെസേജ് ഉണ്ട്?"
  4. "കെയർഗിവറുടെ എത്ര സന്ദേശങ്ങൾ വന്നു?"
  5. "എത്ര പുതിയ സന്ദേശങ്ങൾ ഉണ്ട്?"
  6. "സന്ദേശങ്ങളുടെ എണ്ണം പറയൂ"
  7. "എനിക്ക് എന്തെങ്കിലും സന്ദേശം വന്നിട്ടുണ്ടോ?"
  8. "സഹായിയുടെ എത്ര സന്ദേശങ്ങളുണ്ട്?"
  9. "ആകെ എത്ര സന്ദേശങ്ങൾ ഉണ്ട്?"
  10. "പുതിയ മെസേജ് എത്രയുണ്ട്?"

---

### GROUP 6: REPEAT LAST MESSAGE
* **Intent Identifier**: `REPEAT_MESSAGE`
* **Underlying Command**: `CMD_REPEAT_MESSAGE`
* **Action Target**: `AppVoiceAssistant.handleRepeatMessageFlow()` $\to$ `lastReadCaregiverMessage`
* **Safety Class**: `SAFE_IMMEDIATE`

#### Spoken Dialog Flow:
* User: *"Assistant, say that again"*
* TTS Feedback:
  * If cached: *"Your caregiver says: [Repeated Message]"*
  * If none cached: *"No message to repeat."* (Localized: *"ಮರುಹೇಳಲು ಯಾವುದೇ ಸಂದೇಶವಿಲ್ಲ."* / *"दोबारा सुनाने के लिए कोई संदेश नहीं है।"*)

#### Natural Sentence Patterns:
* **English**:
  1. "Repeat message"
  2. "Repeat the message"
  3. "Repeat caregiver message"
  4. "Say that again"
  5. "Repeat that"
  6. "Say it one more time"
  7. "Tell me again"
  8. "I didn't hear that"
  9. "Repeat what you said"
  10. "Say the message again"
* **Kannada (ಕನ್ನಡ)**:
  1. "ಸಂದೇಶವನ್ನು ಪುನರಾವರ್ತಿಸಿ"
  2. "ಮತ್ತೆ ಹೇಳಿ"
  3. "ಇನ್ನೊಮ್ಮೆ ಹೇಳಿ"
  4. "ಮತ್ತೊಮ್ಮೆ ಹೇಳಿ"
  5. "ಕೇರ್ಗಿವರ್ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ"
  6. "ಅದನ್ನು ಇನ್ನೊಮ್ಮೆ ಓದಿ"
  7. "ನನಗೆ ಕೇಳಿಸಲಿಲ್ಲ, ಮತ್ತೆ ಹೇಳಿ"
  8. "ಕೊನೆಯ ಸಂದೇಶ ಮತ್ತೆ ಹೇಳಿ"
  9. "ಇನ್ನೊಂದು ಸಲ ಹೇಳಿ"
  10. "ಪುನರಾವರ್ತಿಸಿ"
* **Hindi (हिन्दी)**:
  1. "संदेश दोबारा सुनाओ"
  2. "फिर से बताओ"
  3. "दोबारा बोलो"
  4. "एक बार फिर कहो"
  5. "केयरगिवर का मैसेज फिर से सुनाओ"
  6. "मुझे सुनाई नहीं दिया, दोबारा बोलो"
  7. "वही संदेश फिर से पढ़ो"
  8. "दोहराओ"
  9. "फिर से सुनाइए"
  10. "अंतिम संदेश दोबारा बोलो"
* **Malayalam (മലയാളം)**:
  1. "സന്ദേശം വീണ്ടും പറയൂ"
  2. "വീണ്ടും കേൾപ്പിക്കൂ"
  3. "ഒരിക്കൽ കൂടി പറയൂ"
  4. "കെയർഗിവറുടെ സന്ദേശം വീണ്ടും പറയുക"
  5. "എനിക്ക് കേട്ടില്ല, ഒന്നുകൂടി പറയൂ"
  6. "അത് വീണ്ടും വായിക്കൂ"
  7. "അവസാന സന്ദേശം വീണ്ടും പറയൂ"
  8. "വീണ്ടും പറയുക"
  9. "ഒന്നുകൂടി കേൾപ്പിക്കാമോ"
  10. "ആ സന്ദേശം വീണ്ടും പറയൂ"

---

### GROUP 7: READ NOTIFICATIONS
* **Intent Identifier**: `READ_NOTIFICATIONS`
* **Underlying Command**: `CMD_READ_NOTIFICATIONS`
* **Action Target**: `AppVoiceAssistant.handleReadNotificationsFlow()` $\to$ `NotificationService.getReadableNotifications(activity, 3)`
* **Safety Class**: `SAFE_IMMEDIATE`

#### Spoken Dialog Flow:
* User: *"Check notifications"*
* TTS Feedback:
  * 1 Notification: *"Your latest notification says: [Text]"*
  * >1 Notifications: *"You have [N] recent notifications: [N1]. [N2]."*
  * None: *"No new notifications."* (Localized: *"ಹೊಸ ಅಧಿಸೂಚನೆಗಳಿಲ್ಲ."* / *"कोई नई सूचना नहीं है।"*)

#### Natural Sentence Patterns:
* **English**:
  1. "Read notifications"
  2. "Read my notifications"
  3. "Check notifications"
  4. "Check my notifications"
  5. "Tell me my notifications"
  6. "What notifications do I have?"
  7. "Read the latest notification"
  8. "Any new notifications?"
  9. "List notifications"
  10. "Read alerts"
* **Kannada (ಕನ್ನಡ)**:
  1. "ಅಧಿಸೂಚನೆಗಳನ್ನು ಓದಿ"
  2. "ನನ್ನ ನೋಟಿಫಿಕೇಶನ್ ಓದಿ"
  3. "ನೋಟಿಫಿಕೇಶನ್ ಹೇಳಿ"
  4. "ನೋಟಿಫಿಕೇಶನ್‌ಗಳನ್ನು ಪರಿಶೀಲಿಸಿ"
  5. "ಯಾವುದಾದರೂ ಅಧಿಸೂಚನೆ ಬಂದಿದೆಯೇ?"
  6. "ಹೊಸ ನೋಟಿಫಿಕೇಶನ್ ಓದಿ"
  7. "ಇತ್ತೀಚಿನ ಅಧಿಸೂಚನೆ ಏನು?"
  8. "ನೋಟಿಫಿಕೇಶನ್ ಚೆಕ್ ಮಾಡಿ"
  9. "ಅಧಿಸೂಚನೆಗಳನ್ನು ಹೇಳಿ"
  10. "ಎಚ್ಚರಿಕೆಗಳನ್ನು ಓದಿ"
* **Hindi (हिन्दी)**:
  1. "सूचनाएं पढ़ो"
  2. "मेरी नोटिफिकेशन पढ़ो"
  3. "नोटिफिकेशन सुनाओ"
  4. "नोटिफिकेशन चेक करो"
  5. "क्या कोई नई सूचना है?"
  6. "मेरी ताज़ा सूचनाएं बताओ"
  7. "अलर्ट्स पढ़कर सुनाओ"
  8. "नई नोटिफिकेशन बताओ"
  9. "सूचनाएं पढ़कर सुनाइए"
  10. "क्या नोटिफिकेशन आई है?"
* **Malayalam (മലയാളം)**:
  1. "അറിയിപ്പുകൾ വായിക്കൂ"
  2. "നോട്ടിഫിക്കേഷൻ വായിക്കൂ"
  3. "എന്റെ അറിയിപ്പുകൾ കേൾപ്പിക്കൂ"
  4. "നോട്ടിഫിക്കേഷൻ പരിശോധിക്കുക"
  5. "പുതിയ അറിയിപ്പുകൾ എന്തെങ്കിലും ഉണ്ടോ?"
  6. "ഏറ്റവും പുതിയ അറിയിപ്പ് വായിക്കൂ"
  7. "നോട്ടിഫിക്കേഷൻ ചെക്ക് ചെയ്യൂ"
  8. "അറിയിപ്പുകൾ പറയൂ"
  9. "പുതിയ അലർട്ടുകൾ വായിക്കുക"
  10. "അറിയിപ്പുകൾ കേൾപ്പിക്കുക"

---

### GROUP 8: EMERGENCY / SOS
* **Intent Identifier**: `EMERGENCY_SOS` (maps to `OPEN_EMERGENCY` / `OPEN_EMERGENCY_ALERT`)
* **Underlying Command**: `CMD_OPEN_EMERGENCY`, `CMD_OPEN_EMERGENCY_ALERT`
* **Action Target**: `BlindUserDashboardActivity.triggerEmergencySOS()` $\to$ `EmergencyActivity`
* **Safety Class**: `SAFETY_CRITICAL`
* **Confirmation Policy**: Currently triggers immediately with a 2-second rapid-tap hardware debounce. Single-word "help" and single-word "emergency" are strictly filtered out to prevent false alarms. In Step 2, a rapid verbal confirmation `"Do you want to send emergency alert?"` can be optionally attached if requested by safety guidelines.

#### Spoken Dialog Flow:
* User: *"Assistant, emergency help"*
* Action: Siren sounds, emergency alert sent to Caregiver Firestore console, `EmergencyActivity` opened.
* TTS Feedback: *"Emergency alert sent."* (Localized: *"ತುರ್ತು ಎಚ್ಚರಿಕೆ ಕಳುಹಿಸಲಾಗಿದೆ."* / *"आपातकालीन अलर्ट भेजा गया।"*)

#### Natural Sentence Patterns:
* **English**:
  1. "Emergency help"
  2. "Send emergency"
  3. "SOS"
  4. "Send SOS"
  5. "Send emergency alert"
  6. "Trigger emergency alert"
  7. "I need emergency help"
  8. "I need urgent help"
  9. "Get help immediately"
  10. "Save me"
* **Kannada (ಕನ್ನಡ)**:
  1. "ತುರ್ತು ಸಹಾಯ"
  2. "ತುರ್ತು ಸಹಾಯ ಬೇಕು"
  3. "ನನಗೆ ತುರ್ತು ಸಹಾಯ ಬೇಕು"
  4. "ಸಹಾಯ ಬೇಕು"
  5. "ನನಗೆ ಸಹಾಯ ಬೇಕು"
  6. "ಕಾಪಾಡಿ"
  7. "ತಕ್ಷಣ ಸಹಾಯ ಬೇಕು"
  8. "ಎಸ್ ಓ ಎಸ್"
  9. "ತುರ್ತು ಎಚ್ಚರಿಕೆ ಕಳುಹಿಸಿ"
  10. "ಆಪತ್ಕಾಲೀನ ಅಲರ್ಟ್"
* **Hindi (हिन्दी)**:
  1. "आपातकालीन सहायता"
  2. "इमरजेंसी सहायता"
  3. "एसओएस"
  4. "एस ओ एस"
  5. "मदद चाहिए"
  6. "मुझे तुरंत मदद चाहिए"
  7. "बचाओ"
  8. "आपातकालीन अलर्ट भेजो"
  9. "खतरे में हूँ मदद करो"
  10. "इमरजेंसी भेजो"
* **Malayalam (മലയാളം)**:
  1. "അടിയന്തര സഹായം"
  2. "സഹായം വേണം"
  3. "എനിക്ക് അടിയന്തര സഹായം വേണം"
  4. "എസ് ഒ എസ്"
  5. "രക്ഷിക്കൂ"
  6. "ഉടൻ സഹായം വേണം"
  7. "എമർജൻസി അലർട്ട് അയക്കൂ"
  8. "അടിയന്തര മുന്നറിയിപ്പ്"
  9. "എന്നെ രക്ഷിക്കൂ"
  10. "അടിയന്തരമായി സഹായിക്കൂ"

---

### GROUP 9: HOME NAVIGATION
* **Intent Identifier**: `OPEN_HOME`
* **Underlying Command**: `CMD_OPEN_HOME`
* **Action Target**: `VoiceActionDispatcher.handleOpenHome()` $\to$ `BottomNavigationView.setSelectedItemId(R.id.nav_home)`
* **Safety Class**: `SAFE_IMMEDIATE`

#### Natural Sentence Patterns:
* **English**:
  1. "Open home"
  2. "Go home"
  3. "Take me home"
  4. "Show home"
  5. "Home screen"
  6. "Go to home screen"
  7. "Open home page"
  8. "Return to home"
  9. "Main screen"
  10. "Go to main page"
* **Kannada (ಕನ್ನಡ)**:
  1. "ಮುಖಪುಟ ತೆರೆಯಿರಿ"
  2. "ಹೋಮ್ ತೆರೆಯಿರಿ"
  3. "ಹೋಮ್ ಸ್ಕ್ರೀನ್"
  4. "ಮುಖಪುಟಕ್ಕೆ ಹೋಗಿ"
  5. "ಹೋಮ್ ತೋರಿಸಿ"
  6. "ಮುಖ್ಯ ಪುಟಕ್ಕೆ ಹೋಗಿ"
  7. "ಹೋಮ್ ಪೇಜ್ ತೆರೆಯಿರಿ"
  8. "ಮನೆ ಪುಟ ತೋರಿಸಿ"
  9. "ಮುಖಪುಟ"
  10. "ಹೋಮ್ ಗೆ ಹೋಗಿ"
* **Hindi (हिन्दी)**:
  1. "होम खोलो"
  2. "होम पेज पर जाओ"
  3. "होम स्क्रीन दिखाओ"
  4. "मुख्य स्क्रीन पर जाओ"
  5. "होम दिखाओ"
  6. "होम स्क्रीन खोलो"
  7. "वापस होम पर जाओ"
  8. "मेन पेज खोलो"
  9. "होम पर चलो"
  10. "होम"
* **Malayalam (മലയാളം)**:
  1. "ഹോം തുറക്കൂ"
  2. "ഹോം പേജിലേക്ക് പോകുക"
  3. "ഹോം സ്ക്രീൻ കാണിക്കൂ"
  4. "പ്രധാന പേജിലേക്ക് പോകുക"
  5. "ഹോം കാണിക്കൂ"
  6. "ഹോം തുറക്കുക"
  7. "മെയിൻ സ്ക്രീനിലേക്ക് പോകൂ"
  8. "ഹോമിലേക്ക് മടങ്ങുക"
  9. "ഹോം പേജ്"
  10. "ഹോം"

---

### GROUP 10: PROFILE NAVIGATION
* **Intent Identifier**: `OPEN_PROFILE`
* **Underlying Command**: `CMD_OPEN_PROFILE`
* **Action Target**: `VoiceActionDispatcher.handleOpenProfile()` $\to$ `BottomNavigationView.setSelectedItemId(R.id.nav_profile)`
* **Safety Class**: `SAFE_IMMEDIATE`

#### Natural Sentence Patterns:
* **English**:
  1. "Open profile"
  2. "Show profile"
  3. "Go to profile"
  4. "My profile"
  5. "Open my profile"
  6. "Show my profile"
  7. "Go to my profile"
  8. "Profile page"
  9. "User profile"
  10. "View profile"
* **Kannada (ಕನ್ನಡ)**:
  1. "ಪ್ರೊಫೈಲ್ ತೆರೆಯಿರಿ"
  2. "ನನ್ನ ಪ್ರೊಫೈಲ್ ತೆರೆಯಿರಿ"
  3. "ಪ್ರೊಫೈಲ್ ತೋರಿಸಿ"
  4. "ಪ್ರೊಫೈಲ್‌ಗೆ ಹೋಗಿ"
  5. "ನನ್ನ ಪ್ರೊಫೈಲ್ ತೋರಿಸಿ"
  6. "ಪ್ರೊಫೈಲ್ ಪುಟ ತೆರೆಯಿರಿ"
  7. "ನನ್ನ ವಿವರಗಳನ್ನು ತೋರಿಸಿ"
  8. "ಬಳಕೆದಾರರ ಪ್ರೊಫೈಲ್"
  9. "ಪ್ರೊಫೈಲ್ ಪೇಜ್"
  10. "ಪ್ರೊಫೈಲ್ ನೋಡಿ"
* **Hindi (हिन्दी)**:
  1. "प्रोफ़ाइल खोलो"
  2. "मेरी प्रोफ़ाइल खोलो"
  3. "प्रोफ़ाइल दिखाओ"
  4. "प्रोफ़ाइल पर जाओ"
  5. "मेरी प्रोफ़ाइल दिखाओ"
  6. "प्रोफ़ाइल पेज खोलो"
  7. "उपयोगकर्ता प्रोफ़ाइल"
  8. "मेरा खाता दिखाओ"
  9. "मेरी जानकारी दिखाओ"
  10. "प्रोफ़ाइल देखें"
* **Malayalam (മലയാളം)**:
  1. "പ്രൊഫൈൽ തുറക്കൂ"
  2. "എന്റെ പ്രൊഫൈൽ തുറക്കുക"
  3. "പ്രൊഫൈൽ കാണിക്കൂ"
  4. "പ്രൊഫൈലിലേക്ക് പോകുക"
  5. "എന്റെ പ്രൊഫൈൽ കാണിക്കുക"
  6. "പ്രൊഫൈൽ പേജ് തുറക്കൂ"
  7. "യൂസർ പ്രൊഫൈൽ"
  8. "എന്റെ വിവരങ്ങൾ കാണിക്കൂ"
  9. "പ്രൊഫൈൽ പേജിലേക്ക് പോകൂ"
  10. "പ്രൊഫൈൽ"

---

### GROUP 11: SETTINGS NAVIGATION
* **Intent Identifier**: `OPEN_SETTINGS`
* **Underlying Command**: `CMD_OPEN_SETTINGS`
* **Action Target**: `VoiceActionDispatcher.handleOpenSettings()` $\to$ clicks `btnBellNotificationSettings`
* **Safety Class**: `SAFE_IMMEDIATE`

#### Natural Sentence Patterns:
* **English**:
  1. "Open settings"
  2. "Go to settings"
  3. "Show settings"
  4. "Notification settings"
  5. "Open notification settings"
  6. "App settings"
  7. "Settings page"
  8. "Open app settings"
  9. "Adjust settings"
  10. "Settings"
* **Kannada (ಕನ್ನಡ)**:
  1. "ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಿರಿ"
  2. "ಸೆಟ್ಟಿಂಗ್ಸ್‌ಗೆ ಹೋಗಿ"
  3. "ಸೆಟ್ಟಿಂಗ್ಸ್ ತೋರಿಸಿ"
  4. "ಅಧಿಸೂಚನೆ ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಿರಿ"
  5. "ಆ್ಯಪ್ ಸೆಟ್ಟಿಂಗ್ಸ್"
  6. "ಸೆಟ್ಟಿಂಗ್ಸ್ ಪುಟ"
  7. "ಸೆಟ್ಟಿಂಗ್ಸ್ ಓಪನ್ ಮಾಡಿ"
  8. "ನನ್ನ ಸೆಟ್ಟಿಂಗ್ಸ್ ತೋರಿಸಿ"
  9. "ಅಧಿಸೂಚನೆ ಸೆಟ್ಟಿಂಗ್ಸ್"
  10. "ಸೆಟ್ಟಿಂಗ್ಸ್"
* **Hindi (हिन्दी)**:
  1. "सेटिंग्स खोलो"
  2. "सेटिंग्स में जाओ"
  3. "सेटिंग्स दिखाओ"
  4. "नोटिफिकेशन सेटिंग्स खोलो"
  5. "ऐप सेटिंग्स"
  6. "सेटिंग्स पेज"
  7. "सेटिंग्स ओपन करो"
  8. "मेरी सेटिंग्स दिखाओ"
  9. "सूचना सेटिंग्स"
  10. "सेटिंग्स"
* **Malayalam (മലയാളം)**:
  1. "സെറ്റിംഗ്സ് തുറക്കൂ"
  2. "സെറ്റിംഗ്സിലേക്ക് പോകുക"
  3. "സെറ്റിംഗ്സ് കാണിക്കൂ"
  4. "അറിയിപ്പ് സെറ്റിംഗ്സ് തുറക്കൂ"
  5. "ആപ്പ് സെറ്റിംഗ്സ്"
  6. "സെറ്റിംഗ്സ് പേജ്"
  7. "നോട്ടിഫിക്കേഷൻ സെറ്റിംഗ്സ്"
  8. "എന്റെ സെറ്റിംഗ്സ് കാണിക്കുക"
  9. "സെറ്റിംഗ്സ് തുറക്കുക"
  10. "സെറ്റിംഗ്സ്"

---

### GROUP 12: MESSAGES / CHAT NAVIGATION
* **Intent Identifier**: `OPEN_MESSAGES` (alias `OPEN_COMMUNICATION`)
* **Underlying Command**: `CMD_OPEN_MESSAGES`
* **Action Target**: `VoiceActionDispatcher.handleOpenMessages()` $\to$ clicks `btnOpenFullChat` $\to$ launches `ChatActivity`
* **Safety Class**: `SAFE_IMMEDIATE`

#### Natural Sentence Patterns:
* **English**:
  1. "Open messages"
  2. "Open chat"
  3. "Show messages"
  4. "Go to messages"
  5. "Open caregiver chat"
  6. "Open communication"
  7. "Show communication screen"
  8. "Open full chat"
  9. "Go to chat screen"
  10. "View messages"
* **Kannada (ಕನ್ನಡ)**:
  1. "ಸಂದೇಶಗಳನ್ನು ತೆರೆಯಿರಿ"
  2. "ಚಾಟ್ ತೆರೆಯಿರಿ"
  3. "ಮೆಸೇಜ್ ತೆರೆಯಿರಿ"
  4. "ಸಂವಹನ ತೆರೆಯಿರಿ"
  5. "ಕೇರ್ಗಿವರ್ ಚಾಟ್ ತೆರೆಯಿರಿ"
  6. "ಸಂದೇಶಗಳ ಪುಟಕ್ಕೆ ಹೋಗಿ"
  7. "ಪೂರ್ಣ ಚಾಟ್ ತೋರಿಸಿ"
  8. "ಚಾಟ್ ಸ್ಕ್ರೀನ್ ತೆರೆಯಿರಿ"
  9. "ಸಂವಹನ ತೋರಿಸಿ"
  10. "ಸಂದೇಶಗಳು"
* **Hindi (हिन्दी)**:
  1. "संदेश खोलो"
  2. "चैट खोलो"
  3. "मैसेज खोलो"
  4. "संचार खोलो"
  5. "केयरगिवर चैट खोलो"
  6. "संदेश पेज पर जाओ"
  7. "पूरी चैट दिखाओ"
  8. "बातचीत स्क्रीन खोलो"
  9. "चैट स्क्रीन पर जाओ"
  10. "मैसेज स्क्रीन"
* **Malayalam (മലയാളം)**:
  1. "സന്ദേശങ്ങൾ തുറക്കൂ"
  2. "ചാറ്റ് തുറക്കൂ"
  3. "മെസേജ് തുറക്കൂ"
  4. "ആശയവിനിമയം തുറക്കൂ"
  5. "കെയർഗിവർ ചാറ്റ് തുറക്കുക"
  6. "സന്ദേശങ്ങളുടെ പേജിലേക്ക് പോകൂ"
  7. "ഫുൾ ചാറ്റ് കാണിക്കൂ"
  8. "സംഭാഷണം കാണിക്കുക"
  9. "ചാറ്റ് സ്ക്രീൻ തുറക്കൂ"
  10. "സന്ദേശങ്ങൾ"

---

### GROUP 13: BACK NAVIGATION
* **Intent Identifier**: `GO_BACK`
* **Underlying Command**: `CMD_GO_BACK`
* **Action Target**: `AppVoiceAssistant.handleGoBackFlow()` $\to$ `VoiceActionDispatcher.handleGoBack()` (selects `nav_home` if on sub-tab)
* **Safety Class**: `SAFE_IMMEDIATE`

#### Natural Sentence Patterns:
* **English**:
  1. "Go back"
  2. "Back"
  3. "Previous page"
  4. "Return"
  5. "Go to previous screen"
  6. "Take me back"
  7. "Go backward"
  8. "Return back"
  9. "Previous screen"
  10. "Go back please"
* **Kannada (ಕನ್ನಡ)**:
  1. "ಹಿಂದೆ ಹೋಗಿ"
  2. "ಹಿಂದಕ್ಕೆ ಹೋಗಿ"
  3. "ಹಿಂದಿನ ಪುಟ"
  4. "ಹಿಂದೆ ಬನ್ನಿ"
  5. "ಹಿಂದಿನ ಸ್ಕ್ರೀನ್‌ಗೆ ಹೋಗಿ"
  6. "ದಯವಿಟ್ಟು ಹಿಂದೆ ಹೋಗಿ"
  7. "ಹಿಂದಕ್ಕೆ ಬನ್ನಿ"
  8. "ವಾಪಸ್ ಹೋಗಿ"
  9. "ಹಿಂದಿನ ಪುಟಕ್ಕೆ ಹೋಗಿ"
  10. "ಹಿಂದೆ"
* **Hindi (हिन्दी)**:
  1. "पीछे जाओ"
  2. "वापस जाओ"
  3. "पिछला पेज"
  4. "पिछली स्क्रीन पर जाओ"
  5. "पीछे चलो"
  6. "वापस चलो"
  7. "कृपया वापस जाएं"
  8. "वापस आओ"
  9. "पिछला स्क्रीन दिखाओ"
  10. "पीछे"
* **Malayalam (മലയാളം)**:
  1. "പിന്നിലേക്ക് പോവുക"
  2. "തിരികെ പോകൂ"
  3. "മുമ്പത്തെ പേജ്"
  4. "മുമ്പത്തെ സ്ക്രീനിലേക്ക് പോകുക"
  5. "തിരിച്ചു പോകുക"
  6. "ദയവായി പിന്നോട്ട് പോകുക"
  7. "തിരികെ വരൂ"
  8. "മുമ്പത്തെ പേജിലേക്ക് പോകൂ"
  9. "പിന്നോട്ട് പോകുക"
  10. "ബാക്ക്"

---

### GROUP 14: VOICE RECORDER CARD FOCUS
* **Intent Identifier**: `OPEN_VOICE_RECORDER` (alias `OPEN_VOICE_MESSAGE`)
* **Underlying Command**: `CMD_OPEN_VOICE_RECORDER`, `CMD_OPEN_VOICE_MESSAGE`
* **Action Target**: `VoiceActionDispatcher.handleOpenVoiceRecorder()` $\to$ navigates to `nav_home` and requests focus on `cardVoiceMessage`
* **Safety Class**: `SAFE_IMMEDIATE`
* **CRITICAL ARCHITECTURAL DISTINCTION**:
  * "Open voice recorder" **ONLY scrolls and highlights the voice recording card** on the screen.
  * It does **NOT** start capturing audio.
  * It does **NOT** send audio to Firestore/Storage.

#### Natural Sentence Patterns:
* **English**:
  1. "Open voice recorder"
  2. "Open recorder"
  3. "Start voice recorder"
  4. "Show voice recorder"
  5. "Go to voice recorder"
  6. "Open voice message"
  7. "Open audio recorder"
  8. "I want to record a voice message"
  9. "Voice recording section"
  10. "Voice recorder card"
* **Kannada (ಕನ್ನಡ)**:
  1. "ಧ್ವನಿ ರೆಕಾರ್ಡರ್ ತೆರೆಯಿರಿ"
  2. "ರೆಕಾರ್ಡರ್ ತೆರೆಯಿರಿ"
  3. "ಧ್ವನಿ ಸಂದೇಶ ತೆರೆಯಿರಿ"
  4. "ಆಡಿಯೋ ರೆಕಾರ್ಡರ್ ತೋರಿಸಿ"
  5. "ರೆಕಾರ್ಡರ್‌ಗೆ ಹೋಗಿ"
  6. "ಧ್ವನಿ ರೆಕಾರ್ಡರ್ ತೋರಿಸಿ"
  7. "ರೆಕಾರ್ಡಿಂಗ್ ಕಾರ್ಡ್ ತೆರೆಯಿರಿ"
  8. "ಧ್ವನಿ ಸಂದೇಶ ವಿಭಾಗಕ್ಕೆ ಹೋಗಿ"
  9. "ರೆಕಾರ್ಡರ್ ಕಾರ್ಡ್"
  10. "ಧ್ವನಿ ರೆಕಾರ್ಡರ್"
* **Hindi (हिन्दी)**:
  1. "वॉइस रिकॉर्डर खोलो"
  2. "रिकॉर्डर खोलो"
  3. "वॉइस मैसेज खोलो"
  4. "ऑडियो रिकॉर्डर दिखाओ"
  5. "रिकॉर्डर पर जाओ"
  6. "वॉइस रिकॉर्डर दिखाओ"
  7. "रिकॉर्डिंग सेक्शन खोलो"
  8. "आवाज रिकॉर्डर खोलो"
  9. "ऑडियो रिकॉर्डिंग खोलो"
  10. "वॉइस रिकॉर्डर"
* **Malayalam (മലയാളം)**:
  1. "വോയ്സ് റെക്കോർഡർ തുറക്കൂ"
  2. "റെക്കോർഡർ തുറക്കൂ"
  3. "വോയ്സ് മെസേജ് തുറക്കുക"
  4. "ഓഡിയോ റെക്കോർഡർ കാണിക്കൂ"
  5. "റെക്കോർഡറിലേക്ക് പോകുക"
  6. "വോയ്സ് റെക്കോർഡർ കാണിക്കുക"
  7. "ശബ്ദ റെക്കോർഡർ തുറക്കൂ"
  8. "വോയ്സ് റെക്കോർഡിംഗ് ഭാഗത്തേക്ക് പോകൂ"
  9. "ഓഡിയോ റെക്കോർഡിംഗ് തുറക്കൂ"
  10. "വോയ്സ് റെക്കോർഡർ"

---

### GROUP 15, 16, 17: VOICE RECORDING CONTROLS (AUDIT FINDING)
* **Intent `START_VOICE_RECORDING`**: **NOT CURRENTLY IMPLEMENTED**
  * *Audit Detail*: On `BlindUserDashboardActivity`, audio recording is triggered exclusively by user touch on `btnMicVoiceMessage` (`onMicButtonClicked()`). No voice intent exists to start recording.
* **Intent `STOP_VOICE_RECORDING`**: **NOT CURRENTLY IMPLEMENTED**
  * *Audit Detail*: Stopping is triggered exclusively by touching `btnStopRecording` or when the 60-second timer expires.
* **Intent `SEND_VOICE_MESSAGE`**: **NOT CURRENTLY IMPLEMENTED**
  * *Audit Detail*: Sending audio is tied directly into `stopAndSendVoiceRecording(false)` upon pressing the stop button. It uploads `.m4a` files to Firebase Storage (with Supabase HTTP fallback) and saves the metadata to `caregiver_messages`.

---

### GROUP 18: ASSISTANT LISTENING CONTROL
* **Intent Identifiers**:
  * `STOP_ASSISTANT` (maps to `CMD_STOP_LISTENING`)
  * `RESUME_ASSISTANT` (maps to `CMD_START_LISTENING`)
* **Action Target**: `AppVoiceAssistant.stopListening()` / `AppVoiceAssistant.startListeningFlow()`
* **Safety Class**: `SAFE_IMMEDIATE`
* *Distinction from Wake Word*: Wake word is offline keyword spotting in Vosk. These commands control the online Google `SpeechRecognizer` session.

#### Natural Sentence Patterns:
* **Stop Assistant (English)**: "Stop listening", "Stop assistant", "Don't listen", "Be quiet", "Halt", "Shut up", "Cancel listening"
* **Stop Assistant (Kannada)**: "ಕೇಳುವುದನ್ನು ನಿಲ್ಲಿಸಿ", "ನಿಲ್ಲಿಸಿ", "ಸಾಕು", "ಆಲಿಸುವುದನ್ನು ನಿಲ್ಲಿಸಿ", "ಸುಮ್ಮನಿರಿ"
* **Stop Assistant (Hindi)**: "सुनना बंद करो", "बंद करो", "रुक जाओ", "शांत रहो", "सुनना बंद कीजिए"
* **Stop Assistant (Malayalam)**: "കേൾക്കുന്നത് നിർത്തുക", "നിർത്തൂ", "മതി", "മിണ്ടാതിരിക്കൂ", "കേൾക്കണ്ട"
* **Resume Assistant (English)**: "Start listening", "Listen", "Resume listening", "Start assistant", "Listen to me"
* **Resume Assistant (Kannada)**: "ಕೇಳಲು ಪ್ರಾರಂಭಿಸಿ", "ಕೇಳಿಸಿಕೊಳ್ಳಿ", "ಆಲಿಸಿ", "ನನ್ನ ಮಾತು ಕೇಳಿ"
* **Resume Assistant (Hindi)**: "सुनना शुरू करो", "सुनो", "सुनिए", "मेरी बात सुनो"
* **Resume Assistant (Malayalam)**: "കേൾക്കാൻ തുടങ്ങുക", "കേൾക്കൂ", "ശ്രദ്ധിക്കൂ", "പറയുന്നത് കേൾക്കൂ"

---

### GROUP 19 & 20: CONFIRMATION & CANCELLATION
* **Intent Identifiers**: `CONFIRM_YES` / `CONFIRM_NO`
* **Underlying Command**: `CMD_CONFIRM_YES` / `CMD_CONFIRM_NO`
* **State Dependence**: Only active when `pendingConfirmationCommand != null` (e.g. confirming an emergency alert or call).
* **Positive Confirmation Vocabulary**:
  * English: `"yes"`, `"yeah"`, `"yep"`, `"sure"`, `"ok"`, `"confirm"`, `"proceed"`, `"please do"`, `"do it"`
  * Kannada: `"ಹೌದು"`, `"ಸರಿ"`, `"ಖಂಡಿತ"`, `"ಮಾಡಿ"`, `"ಮುಂದುವರಿಸಿ"`
  * Hindi: `"हाँ"`, `"हा"`, `"ठीक है"`, `"ज़रूर"`, `"जरूर"`, `"कर दो"`, `"आगे बढ़ो"`
  * Malayalam: `"അതെ"`, `"ശരി"`, `"തീർച്ചയായും"`, `"ചെയ്യൂ"`, `"തുടങ്ങൂ"`
* **Cancellation Vocabulary**:
  * English: `"no"`, `"cancel"`, `"don't"`, `"dont"`, `"never mind"`, `"cancel action"`, `"do not"`
  * Kannada: `"ಇಲ್ಲ"`, `"ಬೇಡ"`, `"ರದ್ದು"`, `"ಮಾಡಬೇಡಿ"`, `"ಕ್ಯಾನ್ಸಲ್"`
  * Hindi: `"नहीं"`, `"नही"`, `"मत करो"`, `"रद्द"`, `"रहने दो"`, `"कैंसल"`
  * Malayalam: `"വേണ്ട"`, `"ഇല്ല"`, `"റദ്ദാക്കുക"`, `"ചെയ്യേണ്ട"`, `"ക്യാൻസൽ"`

---

## PART D — CAREGIVER SYNONYM & ENTITY DICTIONARY

To accurately match any caregiver-directed command, the engine must recognize all cultural and linguistic references to the caregiver entity while never confusing it with the `"assistant"` wake word.

### 1. English Caregiver Entity Dictionary
* **Primary Nouns**: `caregiver`, `carer`, `caretaker`, `care worker`, `helper`, `attendant`, `guardian`
* **Multi-word / Natural Phrases**: `person taking care of me`, `my assistant at home`, `my nurse`
* **Disambiguation Rule**:
  * `"Assistant, call my helper"` $\to$ Wake Word = `"Assistant"`, Entity = `"helper"` (Caregiver) $\to$ **Valid**
  * `"Assistant, stop listening"` $\to$ Wake Word = `"Assistant"`, Command = `"stop listening"` $\to$ **Valid**
  * `"Call assistant"` $\to$ Disambiguation required: If role is Blind User and user says "call assistant", evaluate whether user meant caregiver. In current code, `caregiver` synonyms are strictly separated from `assistant`.

### 2. Kannada Caregiver Entity Dictionary (ಕನ್ನಡ)
* **Base Synonyms**:
  * ಆರೈಕೆದಾರ (Caregiver)
  * ಆರೈಕೆ ಮಾಡುವವರು (Person caring)
  * ಸಹಾಯಕ / ಸಹಾಯಕರು (Helper)
  * ಪಾಲಕ / ಪಾಲಕರು (Guardian / Caretaker)
  * ಕೇರ್ಗಿವರ್ / ಕೇರ್ ಗಿವರ್ / ಕೇರ್‌ಗಿವರ್ (Caregiver transliteration)
  * ಕೇರ್ಟೇಕರ್ / ಕೇರ್ ಟೇಕರ್ (Caretaker transliteration)
* **Grammatical Case Declensions (Critical for Matching)**:
  * **Dative (`-ಗೆ`, `-ರಿಗೆ`, `-ನಿಗೆ`)**: ಆರೈಕೆದಾರರಿಗೆ, ಆರೈಕೆದಾರನಿಗೆ, ಸಹಾಯಕರಿಗೆ, ಸಹಾಯಕನಿಗೆ, ಪಾಲಕರಿಗೆ, ಪಾಲಕನಿಗೆ, ಕೇರ್ಗಿವರ್ಗೆ, ಕೇರ್‌ಗಿವರ್‌ಗೆ, ಕೇರ್ಟೇಕರ್ಗೆ
  * **Genitive (`-ರ`, `-ನ`)**: ಆರೈಕೆದಾರರ, ಆರೈಕೆದಾರನ, ಸಹಾಯಕರ, ಸಹಾಯಕನ, ಪಾಲಕರ, ಪಾಲಕನ, ಕೇರ್ಗಿವರ್ನ, ಕೇರ್‌ಗಿವರ್‌ನ
  * **Ablative / Instrumental (`-ರಿಂದ`, `-ನಿಂದ`)**: ಆರೈಕೆದಾರರಿಂದ, ಆರೈಕೆದಾರನಿಂದ, ಸಹಾಯಕರಿಂದ, ಸಹಾಯಕನಿಂದ, ಪಾಲಕರಿಂದ, ಪಾಲಕನಿಂದ, ಕೇರ್ಗಿವರ್ನಿಂದ
  * **Accusative (`-ನ್ನು`, `-ರನ್ನು`, `-ನನ್ನು`)**: ಆರೈಕೆದಾರರನ್ನು, ಆರೈಕೆದಾರನನ್ನು, ಸಹಾಯಕರನ್ನು, ಸಹಾಯಕನನ್ನು, ಪಾಲಕರನ್ನು, ಪಾಲಕನನ್ನು, ಕೇರ್ಗಿವರ್‌ನನ್ನು

### 3. Hindi Caregiver Entity Dictionary (हिन्दी)
* **Base Synonyms**:
  * देखभाल करने वाला / देखभाल करने वाले / देखभाल करने वाली
  * देखभालकर्ता
  * सहायक
  * परिचारक
  * केयरगिवर / केयर गिवर
  * केयरटेकर / केयर टेकर
* **Grammatical Post-position Forms**:
  * **Accusative/Dative (`को`)**: केयरगिवर को, सहायक को, परिचारक को, देखभाल करने वाले को, केयरटेकर को
  * **Genitive (`का`, `के`, `की`)**: केयरगिवर का, केयरगिवर के, केयरगिवर की, सहायक का, सहायक के, देखभाल करने वाले का
  * **Ablative/Associative (`से`)**: केयरगिवर से, सहायक से, देखभाल करने वाले से

### 4. Malayalam Caregiver Entity Dictionary (മലയാളം)
* **Base Synonyms**:
  * പരിചരിക്കുന്നയാൾ / പരിചരിക്കുന്ന ആൾ
  * പരിചാരകൻ
  * സഹായി
  * ശുശ്രൂഷകൻ
  * കെയർഗിവർ / കെയർ ഗിവർ
  * കെയർടേക്കർ / കെയർ ടേക്കർ
* **Grammatical Case Forms**:
  * **Accusative (`-എ`, `-യെ`)**: പരിചരിക്കുന്നയാളെ, പരിചാരകനെ, സഹായിയെ, കെയർഗിവറെ, കെയർടേക്കറെ
  * **Dative (`-ക്ക്`, `-ന്`, `-ഇന്`)**: പരിചരിക്കുന്നയാൾക്ക്, പരിചാരകന്, സഹായിക്ക്, കെയർഗിവർക്ക്, കെയർഗിവറിന്, കെയർടേക്കർക്ക്, കെയർടേക്കറിന്
  * **Genitive (`-ുടെ`, `-ന്റെ`)**: പരിചരിക്കുന്നയാളുടെ, പരിചാരകന്റെ, സഹായിയുടെ, കെയർഗിവറുടെ, കെയർഗിവറിന്റെ, കെയർടേക്കറുടെ

---

## PART E & F — ACTION + ENTITY DECOMPOSITION MATRIX

By separating the user's utterance into an **ACTION** token and a **TARGET/ENTITY** token, the voice assistant can understand infinite combinatorial phrasing rather than brittle exact string matches.

| Intent Name | Primary Actions (English / Kannada / Hindi / Malayalam) | Primary Entities / Targets | Resolved Intent Type |
|---|---|---|---|
| **CALL_CAREGIVER** | `CALL`, `PHONE`, `DIAL`, `RING`, `CONNECT` / ಕರೆ, ಕಾಲ್, ಫೋನ್, ಮಾತನಾಡು / कॉल, फोन, बात / വിളി, കോൾ, ഫോൺ | `CAREGIVER`, `CARER`, `HELPER`, `CARETAKER` / ಕೇರ್ಗಿವರ್, ಆರೈಕೆದಾರ, ಸಹಾಯಕ, ಪಾಲಕ / केयरगिवर, सहायक, देखभालकर्ता / കെയർഗിവർ, സഹായി, പരിചാരകൻ | `CALL_CAREGIVER` |
| **END_CALL** | `END`, `HANG UP`, `DISCONNECT`, `CUT`, `STOP`, `FINISH` / ಮುಗಿಸಿ, ಕೊನೆಗೊಳಿಸಿ, ಕಟ್ ಮಾಡಿ, ನಿಲ್ಲಿಸಿ / समाप्त, काटो, बंद, खत्म / അവസാനിപ്പിക്കുക, കട്ട് ചെയ്യുക, നിർത്തുക | `CALL`, `PHONE`, `CONNECTION` / ಕರೆ, ಕಾಲ್, ಫೋನ್ / कॉल, फोन / കോൾ, ഫോൺ | `END_CALL` |
| **SEND_MESSAGE** | `SEND`, `MESSAGE`, `WRITE`, `TELL` / ಕಳುಹಿಸಿ, ಕಳಿಸಿ, ಹೇಳಿ / भेजो, भेजें, बताओ / അയക്കൂ, അയക്കുക, പറയൂ | `MESSAGE`, `TEXT`, `NOTE` + `CAREGIVER` / ಸಂದೇಶ, ಮೆಸೇಜ್ + ಕೇರ್ಗಿವರ್ / संदेश, मैसेज + केयरगिवर / സന്ദേശം, മെസേജ് + കെയർഗിവർ | `SEND_MESSAGE` |
| **READ_MESSAGES** | `READ`, `CHECK`, `LISTEN`, `HEAR`, `WHAT SAID` / ಓದಿ, ಹೇಳಿ, ಪರಿಶೀಲಿಸಿ / पढ़ो, सुनाओ, बताओ / വായിക്കൂ, കേൾപ്പിക്കൂ, പരിശോധിക്കുക | `MESSAGES`, `MESSAGE`, `INBOX` + `CAREGIVER` / ಸಂದೇಶಗಳು, ಮೆಸೇಜ್ / संदेश, मैसेज / സന്ദേശങ്ങൾ, മെസേജ് | `READ_MESSAGES` |
| **MESSAGE_COUNT** | `COUNT`, `HOW MANY`, `TOTAL`, `CHECK` / ಎಷ್ಟು, ಎಣಿಸಿ, ಸಂಖ್ಯೆ / कितने, गिनो, गिनती / എത്ര, എണ്ണൂ, എണ്ണം | `MESSAGES` / ಸಂದೇಶಗಳು, ಮೆಸೇಜ್ / संदेश, मैसेज / സന്ദേശങ്ങൾ, മെസേജ് | `MESSAGE_COUNT` |
| **REPEAT_MESSAGE** | `REPEAT`, `AGAIN`, `ONCE MORE`, `DIDN'T HEAR` / ಮತ್ತೆ, ಇನ್ನೊಮ್ಮೆ, ಪುನರಾವರ್ತಿಸಿ / दोबारा, फिर से, दोहराओ / വീണ്ടും, ഒരിക്കൽ കൂടി | `MESSAGE`, `THAT`, `WHAT SAID` / ಸಂದೇಶ, ಮೆಸೇಜ್, ಮಾತು / संदेश, मैसेज, बात / സന്ദേശം, മെസേജ്, കാര്യം | `REPEAT_MESSAGE` |
| **READ_NOTIFICATIONS** | `READ`, `CHECK`, `TELL`, `LIST` / ಓದಿ, ಹೇಳಿ, ಪರಿಶೀಲಿಸಿ / पढ़ो, सुनाओ, बताओ / വായിക്കൂ, കേൾപ്പിക്കൂ | `NOTIFICATIONS`, `ALERTS` / ಅಧಿಸೂಚನೆಗಳು, ನೋಟಿಫಿಕೇಶನ್ / सूचनाएं, नोटिफिकेशन, अलर्ट / അറിയിപ്പുകൾ, നോട്ടിഫിക്കേഷൻ | `READ_NOTIFICATIONS` |
| **EMERGENCY_SOS** | `EMERGENCY`, `URGENT`, `SOS`, `SAVE`, `DANGER` / ತುರ್ತು, ಸಹಾಯ ಬೇಕು, ಕಾಪಾಡಿ / आपातकाल, इमरजेंसी, बचाओ, मदद / അടിയന്തരം, രക്ഷിക്കൂ, സഹായം | `HELP`, `ALERT`, `ME` / ಸಹಾಯ, ಎಚ್ಚರಿಕೆ / सहायता, अलर्ट / സഹായം, മുന്നറിയിപ്പ് | `OPEN_EMERGENCY` |
| **OPEN_HOME** | `OPEN`, `GO TO`, `SHOW`, `TAKE ME TO` / ತೆರೆಯಿರಿ, ಹೋಗಿ, ತೋರಿಸಿ / खोलो, जाओ, दिखाओ / തുറക്കൂ, പോകുക, കാണിക്കൂ | `HOME`, `HOME SCREEN`, `MAIN SCREEN` / ಮುಖಪುಟ, ಹೋಮ್, ಮುಖ್ಯ ಪುಟ / होम, मुख्य स्क्रीन / ഹോം, പ്രധാന പേജ് | `OPEN_HOME` |
| **OPEN_PROFILE** | `OPEN`, `GO TO`, `SHOW`, `VIEW` / ತೆರೆಯಿರಿ, ಹೋಗಿ, ತೋರಿಸಿ / खोलो, जाओ, दिखाओ / തുറക്കൂ, പോകുക, കാണിക്കൂ | `PROFILE`, `MY PROFILE`, `ACCOUNT` / ಪ್ರೊಫೈಲ್, ನನ್ನ ಪ್ರೊಫೈಲ್ / प्रोफ़ाइल, खाता / പ്രൊഫൈൽ, വിവരങ്ങൾ | `OPEN_PROFILE` |
| **OPEN_SETTINGS** | `OPEN`, `GO TO`, `SHOW`, `ADJUST` / ತೆರೆಯಿರಿ, ಹೋಗಿ, ತೋರಿಸಿ / खोलो, जाओ, दिखाओ / തുറക്കൂ, പോകുക, കാണിക്കൂ | `SETTINGS`, `APP SETTINGS`, `NOTIFICATION SETTINGS` / ಸೆಟ್ಟಿಂಗ್ಸ್, ಅಧಿಸೂಚನೆ ಸೆಟ್ಟಿಂಗ್ಸ್ / सेटिंग्स, सूचना सेटिंग्स / സെറ്റിംഗ്സ്, അറിയിപ്പ് സെറ്റിംഗ്സ് | `OPEN_SETTINGS` |
| **OPEN_MESSAGES** | `OPEN`, `GO TO`, `SHOW`, `VIEW` / ತೆರೆಯಿರಿ, ಹೋಗಿ, ತೋರಿಸಿ / खोलो, जाओ, दिखाओ / തുറക്കൂ, പോകുക, കാണിക്കൂ | `MESSAGES SCREEN`, `CHAT`, `COMMUNICATION` / ಚಾಟ್, ಸಂದೇಶಗಳ ಪುಟ, ಸಂವಹನ / चैट, संदेश स्क्रीन, संचार / ചാറ്റ്, സന്ദേശങ്ങൾ, ആശയവിനിമയം | `OPEN_MESSAGES` |
| **GO_BACK** | `GO`, `RETURN`, `TAKE ME`, `NAVIGATE` / ಹೋಗಿ, ಬನ್ನಿ, ವಾಪಸ್ / जाओ, चलो, वापस / പോവുക, വരൂ, തിരികെ | `BACK`, `PREVIOUS`, `PREVIOUS PAGE` / ಹಿಂದೆ, ಹಿಂದಕ್ಕೆ, ಹಿಂದಿನ ಪುಟ / पीछे, पिछला / പിന്നിലേക്ക്, മുമ്പത്തെ | `GO_BACK` |
| **OPEN_VOICE_RECORDER** | `OPEN`, `GO TO`, `SHOW` / ತೆರೆಯಿರಿ, ತೋರಿಸಿ / खोलो, दिखाओ / തുറക്കൂ, കാണിക്കൂ | `VOICE RECORDER`, `RECORDER CARD` / ಧ್ವನಿ ರೆಕಾರ್ಡರ್ / वॉइस रिकॉर्डर / വോയ്സ് റെക്കോർഡർ | `OPEN_VOICE_RECORDER` |

---

## PART G — SAFETY CLASSIFICATION

Every voice command is classified into three safety tiers:

```
[VOICE INTENT SAFETY TIERS]
  ├── TIER 1: SAFE_IMMEDIATE (Executes with zero confirmation)
  │     ├── OPEN_HOME
  │     ├── OPEN_PROFILE
  │     ├── OPEN_SETTINGS
  │     ├── OPEN_MESSAGES
  │     ├── OPEN_CAREGIVER
  │     ├── GO_BACK
  │     ├── READ_MESSAGES
  │     ├── MESSAGE_COUNT
  │     ├── REPEAT_MESSAGE
  │     ├── READ_NOTIFICATIONS
  │     └── OPEN_VOICE_RECORDER
  │
  ├── TIER 2: COMMUNICATION_ACTION (Interacts with another person)
  │     ├── CALL_CAREGIVER (Initiates voice call)
  │     ├── END_CALL (Terminates voice call)
  │     └── SEND_MESSAGE (Multi-step conversational flow: Prompt -> Dictate -> Send)
  │
  └── TIER 3: SAFETY_CRITICAL (Safety alarms, emergency sirens, high-priority notifications)
        └── EMERGENCY_SOS (Plays siren, alerts all caregivers, logs emergency)
              • Current Mechanism: 2000ms hardware debounce; rejects single-word "help".
              • Recommended Future Setting: REQUIRES_CONFIRMATION = YES (or rapid single-tap override).
```

---

## PART H — AMBIGUOUS PHRASES & BLOCKLIST

The following isolated single words **MUST NOT** trigger any action on their own because of severe ambiguity:

1. **`"help"`**:
   * *Why Ambiguous*: The user might say "I need help with my coffee" or "Can someone help me find my glasses".
   * *Safety Behavior*: Must be combined with "emergency", "urgent", "sos", or "doctor" to trigger `EMERGENCY_SOS`.
2. **`"call"`**:
   * *Why Ambiguous*: "Who called me?", "Don't call", or talking about a phone call.
   * *Safety Behavior*: Rejected alone. Must contain a caregiver target (`caregiver`, `helper`, `caretaker`).
3. **`"send"`**:
   * *Why Ambiguous*: Send what? Send where?
   * *Safety Behavior*: Rejected alone. Requires `message` or explicit target.
4. **`"message"`**:
   * *Why Ambiguous*: User could mean "read message", "send message", "repeat message", or "count messages".
   * *Safety Behavior*: Rejected alone.
5. **`"home"`**:
   * *Why Ambiguous*: "I am at home", "Take me home".
   * *Safety Behavior*: Requires an action verb ("open home", "go home", "show home").
6. **`"assistant"`**:
   * *Why Ambiguous*: This is the wake word. Saying "Assistant" alone only transitions the assistant into `WAITING_FOR_COMMAND` listening mode; it does not execute any command.
7. **`"stop"`**:
   * *Why Ambiguous*: During playback, it means stop speaking; during a call, it might mean hang up; idle, it means stop listening.
   * *Safety Behavior*: Contextually scoped in `AppVoiceAssistant`.
8. **`"yes"` / `"no"`**:
   * *Why Ambiguous*: Meaningless unless the assistant has asked a pending confirmation question.
   * *Safety Behavior*: Ignored unless `pendingConfirmationCommand != null`.

---

## PART I — FALSE POSITIVE PROTECTION & NEGATION HANDLING

To prevent accidental triggers, the voice matcher specification includes negation patterns and conversational conversational filters:

| Spoken Sentence | Why it must NOT trigger | Required Handling |
|---|---|---|
| *"Do not call my caregiver."* | Contains "call my caregiver" but is explicitly negated with "Do not". | Negation detector matches `do not`, `don't`, `ಮಾಡಬೇಡಿ`, `मत`, `ചെയ്യരുത്` $\to$ Returns `UNKNOWN`. |
| *"Don't send the message."* | Explicitly negated command. | Suppresses `SEND_MESSAGE`. |
| *"I was talking about my caregiver."* | Past-tense descriptive statement about caregiver, not an action. | Lacks an action verb (call, send, message) $\to$ Returns `UNKNOWN`. |
| *"Yesterday my caregiver called me."* | Historical recollection containing "caregiver" and "called". | Temporal token `yesterday` / `ನಿನ್ನೆ` / `कल` / `ഇന്നലെ` suppresses action trigger. |
| *"Who is my caregiver?"* | Information inquiry, not a command to call or message. | Inquisitive filter (`who is`, `ಯಾರು`, `कौन`, `ആരാണ്`) suppresses call intent $\to$ Returns `UNKNOWN`. |
| *"Assistant is a helpful app."* | Conversation mentioning the wake word in third person. | Treated as idle chatter $\to$ Ignores speech. |

---

## PART J — STATUS MATRIX: EXISTING VS PROPOSED

| Intent Group | Intent Name | Status | Current Code Location | Limitations / Notes |
|---|---|---|---|---|
| 1 | `CALL_CAREGIVER` | **EXISTING AND VERIFIED** | `WebRTCManager.java`, `BlindUserDashboardActivity.java:320` | Requires connected Caregiver UID and TURN relay on cellular data. |
| 2 | `END_CALL` | **EXISTING AND VERIFIED** | `VoiceActionDispatcher.java:183`, `VoiceCallManager.java` | Acts only during an active call. |
| 3 | `SEND_MESSAGE` | **EXISTING AND VERIFIED** | `AppVoiceAssistant.java:1150`, `BlindUserDashboardActivity.java:1384` | Sends text messages via Firestore; does not record voice audio. |
| 4 | `READ_MESSAGES` | **EXISTING AND VERIFIED** | `AppVoiceAssistant.java:1495`, `BlindUserDashboardActivity.java:1528` | Reads text messages only; voice audio recordings ignored. |
| 5 | `MESSAGE_COUNT` | **EXISTING AND VERIFIED** | `AppVoiceAssistant.java:1668`, `BlindUserDashboardActivity.java:1701` | Accurate for text messages received. |
| 6 | `REPEAT_MESSAGE` | **EXISTING AND VERIFIED** | `AppVoiceAssistant.java:1774` | Repeats last in-memory cached caregiver message. |
| 7 | `READ_NOTIFICATIONS`| **EXISTING AND VERIFIED** | `AppVoiceAssistant.java:1831`, `NotificationService.java` | Reads up to 3 recent notifications. |
| 8 | `EMERGENCY_SOS` | **EXISTING AND VERIFIED** | `BlindUserDashboardActivity.java:783`, `EmergencyActivity.java` | Directly triggers alarm & alert with 2s debounce. |
| 9 | `OPEN_HOME` | **EXISTING AND VERIFIED** | `VoiceActionDispatcher.java:207` | Navigates to Home tab. |
| 10 | `OPEN_PROFILE` | **EXISTING AND VERIFIED** | `VoiceActionDispatcher.java:236` | Navigates to Profile tab. |
| 11 | `OPEN_SETTINGS` | **EXISTING AND VERIFIED** | `VoiceActionDispatcher.java:256` | Opens notification settings dialog. |
| 12 | `OPEN_MESSAGES` | **EXISTING AND VERIFIED** | `VoiceActionDispatcher.java:265` | Navigates to Caregiver tab / opens `ChatActivity`. |
| 13 | `OPEN_CAREGIVER` | **EXISTING AND VERIFIED** | `VoiceActionDispatcher.java:287` | Navigates to Caregiver connections tab. |
| 14 | `GO_BACK` | **EXISTING AND VERIFIED** | `AppVoiceAssistant.java:1924`, `VoiceActionDispatcher.java:118` | Returns to Home tab if on a sub-screen. |
| 15 | `OPEN_VOICE_RECORDER`| **EXISTING BUT LIMITED** | `VoiceActionDispatcher.java:305` | Scrolls/focuses voice recorder card; does NOT record. |
| 16 | `START_VOICE_RECORDING`| **NOT IMPLEMENTED** | None (Touch only) | Voice recording can currently only be started via button click. |
| 17 | `STOP_VOICE_RECORDING` | **NOT IMPLEMENTED** | None (Touch only) | Voice recording can currently only be stopped via button click. |
| 18 | `SEND_VOICE_MESSAGE` | **NOT IMPLEMENTED** | None (Touch only) | Voice recording can currently only be sent via button click. |
| 19 | `STOP_ASSISTANT` | **EXISTING AND VERIFIED** | `VoiceActionDispatcher.java:173`, `AppVoiceAssistant.java:2121` | Stops STT listening. |
| 20 | `RESUME_ASSISTANT` | **EXISTING AND VERIFIED** | `VoiceActionDispatcher.java:168`, `AppVoiceAssistant.java:590` | Starts STT listening. |
| 21 | `CONFIRM_YES` / `NO` | **EXISTING AND VERIFIED** | `AppVoiceAssistant.java:960` | Resolves pending confirmation states. |

---

## PART K — RECOMMENDED STEP-BY-STEP IMPLEMENTATION ROADMAP (FOR STEP 2 ONWARD)

1. **Step 2A — Expand `VoiceIntentMatcher.java` Regex & Token Dictionaries**:
   * Add the verified multilingual synonym lists and natural phrase patterns for all 14 verified intents across English, Kannada, Hindi, and Malayalam.
   * Implement strict negation filtering (`do not`, `don't`, `ಮಾಡಬೇಡಿ`, `मत`, `ചെയ്യരുത്`).
2. **Step 2B — Verify Zero Breakage of Existing Tests**:
   * Run Gradle compilation and unit tests to ensure all existing phrase matches remain 100% backward-compatible.
3. **Step 2C — (Optional Future Enhancement) Voice Recording Hands-Free Control**:
   * If voice-commanded voice recording is desired, wire `START_VOICE_RECORDING` to `BlindUserDashboardActivity.onMicButtonClicked()` and `STOP_VOICE_RECORDING` to `stopAndSendVoiceRecording(false)`.
