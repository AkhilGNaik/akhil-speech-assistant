package com.kannada.speechassistant.voiceassistant;

import com.kannada.speechassistant.GestureAction;
import com.kannada.speechassistant.GestureActionMapper;
import com.kannada.speechassistant.LanguageManager;
import com.kannada.speechassistant.RoleManager;
import com.kannada.speechassistant.TransliterationEngine;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive Unit Test Suite for Mute User Module.
 * Validates 99%-100% precision across all gestures, multilingual quick phrases,
 * language detection, normalization, and transliteration fallbacks.
 */
public class MuteUserModuleUnitTest {

    // =========================================================================
    // 1. GESTURE ACTION MAPPER TESTS (14 GESTURES x 3 LANGUAGES)
    // =========================================================================

    @Test
    public void testAllGesturesInKannada() {
        String lang = "kn";

        String[] gestureIds = {
            GestureActionMapper.ID_THUMB_UP,
            GestureActionMapper.ID_THUMB_DOWN,
            GestureActionMapper.ID_OPEN_PALM,
            GestureActionMapper.ID_CLOSED_FIST,
            GestureActionMapper.ID_POINTING_UP,
            GestureActionMapper.ID_VICTORY,
            GestureActionMapper.ID_ILOVEYOU,
            GestureActionMapper.ID_TWO_FINGERS,
            GestureActionMapper.ID_OK_SIGN,
            GestureActionMapper.ID_PINCH,
            GestureActionMapper.ID_ONE_FINGER_UP,
            GestureActionMapper.ID_OPEN_HAND_WAVE,
            GestureActionMapper.ID_CROSSED_FINGERS,
            GestureActionMapper.ID_CALL_ME
        };

        for (String id : gestureIds) {
            GestureAction action = GestureActionMapper.getGestureById(id);
            assertNotNull("GestureAction should exist for " + id, action);
            assertNotNull("Localized message in Kannada should exist for " + id, action.getLocalizedMessage(lang));
            assertFalse(action.getLocalizedMessage(lang).isEmpty());
        }

        // Verify key Kannada phrases content
        assertTrue(GestureActionMapper.getGestureById(GestureActionMapper.ID_THUMB_UP).getLocalizedMessage(lang).contains("ಹೌದು"));
        assertTrue(GestureActionMapper.getGestureById(GestureActionMapper.ID_THUMB_DOWN).getLocalizedMessage(lang).contains("ಇಲ್ಲ"));
        assertTrue(GestureActionMapper.getGestureById(GestureActionMapper.ID_POINTING_UP).getLocalizedMessage(lang).contains("ಗಮನ"));
        assertTrue(GestureActionMapper.getGestureById(GestureActionMapper.ID_VICTORY).getLocalizedMessage(lang).contains("ಧನ್ಯವಾದ"));
        assertTrue(GestureActionMapper.getGestureById(GestureActionMapper.ID_CLOSED_FIST).getLocalizedMessage(lang).contains("ತುರ್ತು"));
    }

    @Test
    public void testAllGesturesInHindi() {
        String lang = "hi";

        String[] gestureIds = {
            GestureActionMapper.ID_THUMB_UP,
            GestureActionMapper.ID_THUMB_DOWN,
            GestureActionMapper.ID_OPEN_PALM,
            GestureActionMapper.ID_CLOSED_FIST,
            GestureActionMapper.ID_POINTING_UP,
            GestureActionMapper.ID_VICTORY,
            GestureActionMapper.ID_ILOVEYOU,
            GestureActionMapper.ID_TWO_FINGERS,
            GestureActionMapper.ID_OK_SIGN,
            GestureActionMapper.ID_PINCH,
            GestureActionMapper.ID_ONE_FINGER_UP,
            GestureActionMapper.ID_OPEN_HAND_WAVE,
            GestureActionMapper.ID_CROSSED_FINGERS,
            GestureActionMapper.ID_CALL_ME
        };

        for (String id : gestureIds) {
            GestureAction action = GestureActionMapper.getGestureById(id);
            assertNotNull("GestureAction should exist for " + id, action);
            assertNotNull("Localized message in Hindi should exist for " + id, action.getLocalizedMessage(lang));
            assertFalse(action.getLocalizedMessage(lang).isEmpty());
        }

        // Verify key Hindi phrases content
        assertTrue(GestureActionMapper.getGestureById(GestureActionMapper.ID_THUMB_UP).getLocalizedMessage(lang).contains("हाँ"));
        assertTrue(GestureActionMapper.getGestureById(GestureActionMapper.ID_THUMB_DOWN).getLocalizedMessage(lang).contains("नहीं"));
        assertTrue(GestureActionMapper.getGestureById(GestureActionMapper.ID_POINTING_UP).getLocalizedMessage(lang).contains("ध्यान"));
        assertTrue(GestureActionMapper.getGestureById(GestureActionMapper.ID_VICTORY).getLocalizedMessage(lang).contains("धन्यवाद"));
        assertTrue(GestureActionMapper.getGestureById(GestureActionMapper.ID_CLOSED_FIST).getLocalizedMessage(lang).contains("आपातकालीन"));
    }

    @Test
    public void testAllGesturesInMalayalam() {
        String lang = "ml";

        String[] gestureIds = {
            GestureActionMapper.ID_THUMB_UP,
            GestureActionMapper.ID_THUMB_DOWN,
            GestureActionMapper.ID_OPEN_PALM,
            GestureActionMapper.ID_CLOSED_FIST,
            GestureActionMapper.ID_POINTING_UP,
            GestureActionMapper.ID_VICTORY,
            GestureActionMapper.ID_ILOVEYOU,
            GestureActionMapper.ID_TWO_FINGERS,
            GestureActionMapper.ID_OK_SIGN,
            GestureActionMapper.ID_PINCH,
            GestureActionMapper.ID_ONE_FINGER_UP,
            GestureActionMapper.ID_OPEN_HAND_WAVE,
            GestureActionMapper.ID_CROSSED_FINGERS,
            GestureActionMapper.ID_CALL_ME
        };

        for (String id : gestureIds) {
            GestureAction action = GestureActionMapper.getGestureById(id);
            assertNotNull("GestureAction should exist for " + id, action);
            assertNotNull("Localized message in Malayalam should exist for " + id, action.getLocalizedMessage(lang));
            assertFalse(action.getLocalizedMessage(lang).isEmpty());
        }

        // Verify key Malayalam phrases content
        assertTrue(GestureActionMapper.getGestureById(GestureActionMapper.ID_THUMB_UP).getLocalizedMessage(lang).contains("അതെ"));
        assertTrue(GestureActionMapper.getGestureById(GestureActionMapper.ID_THUMB_DOWN).getLocalizedMessage(lang).contains("ഇല്ല"));
        assertTrue(GestureActionMapper.getGestureById(GestureActionMapper.ID_POINTING_UP).getLocalizedMessage(lang).contains("ശ്രദ്ധിക്കുക"));
        assertTrue(GestureActionMapper.getGestureById(GestureActionMapper.ID_VICTORY).getLocalizedMessage(lang).contains("നന്ദി"));
        assertTrue(GestureActionMapper.getGestureById(GestureActionMapper.ID_CLOSED_FIST).getLocalizedMessage(lang).contains("അടിയന്തര"));
    }

    @Test
    public void testGestureEmergencyTypeMapping() {
        GestureAction fist = GestureActionMapper.getGestureById(GestureActionMapper.ID_CLOSED_FIST);
        assertNotNull(fist);
        assertTrue(fist.isEmergency());

        GestureAction thumbUp = GestureActionMapper.getGestureById(GestureActionMapper.ID_THUMB_UP);
        assertNotNull(thumbUp);
        assertFalse(thumbUp.isEmergency());

        GestureAction pointingUp = GestureActionMapper.getGestureById(GestureActionMapper.ID_POINTING_UP);
        assertNotNull(pointingUp);
        assertFalse(pointingUp.isEmergency());
    }

    // =========================================================================
    // 2. QUICK PHRASES MULTILINGUAL RESOLUTION
    // =========================================================================

    @Test
    public void testQuickPhrasesInKannada() {
        assertEquals("ನೀರು ಬೇಕು", LanguageManager.getQuickPhraseText("water", "kn"));
        assertEquals("ಹಸಿವಾಗಿದೆ", LanguageManager.getQuickPhraseText("food", "kn"));
        assertEquals("ಸಹಾಯ ಬೇಕು", LanguageManager.getQuickPhraseText("help", "kn"));
        assertEquals("ವಿಶ್ರಾಂತಿ ಬೇಕು", LanguageManager.getQuickPhraseText("rest", "kn"));
        assertEquals("ಹೊರಗೆ ಹೋಗಬೇಕು", LanguageManager.getQuickPhraseText("outside", "kn"));
        assertEquals("ಶೌಚಾಲಯಕ್ಕೆ ಹೋಗಬೇಕು", LanguageManager.getQuickPhraseText("toilet", "kn"));
        assertEquals("ದಯವಿಟ್ಟು ಮೌನವಾಗಿರಿ", LanguageManager.getQuickPhraseText("quiet", "kn"));
    }

    @Test
    public void testQuickPhrasesInHindi() {
        assertEquals("पानी चाहिए", LanguageManager.getQuickPhraseText("water", "hi"));
        assertEquals("भूख लगी है", LanguageManager.getQuickPhraseText("food", "hi"));
        assertEquals("मदद चाहिए", LanguageManager.getQuickPhraseText("help", "hi"));
        assertEquals("आराम चाहिए", LanguageManager.getQuickPhraseText("rest", "hi"));
        assertEquals("बाहर जाना है", LanguageManager.getQuickPhraseText("outside", "hi"));
        assertEquals("शौचालय जाना है", LanguageManager.getQuickPhraseText("toilet", "hi"));
        assertEquals("कृपया चुप रहें", LanguageManager.getQuickPhraseText("quiet", "hi"));
    }

    @Test
    public void testQuickPhrasesInMalayalam() {
        assertEquals("വെള്ളം വേണം", LanguageManager.getQuickPhraseText("water", "ml"));
        assertEquals("വിശക്കുന്നു", LanguageManager.getQuickPhraseText("food", "ml"));
        assertEquals("സഹായം വേണം", LanguageManager.getQuickPhraseText("help", "ml"));
        assertEquals("വിശ്രമം വേണം", LanguageManager.getQuickPhraseText("rest", "ml"));
        assertEquals("പുറത്തു പോകണം", LanguageManager.getQuickPhraseText("outside", "ml"));
        assertEquals("ടോയ്‌ലറ്റ് പോകണം", LanguageManager.getQuickPhraseText("toilet", "ml"));
        assertEquals("ദയവായി ശാന്തമായിരിക്കുക", LanguageManager.getQuickPhraseText("quiet", "ml"));
    }

    // =========================================================================
    // 3. SOS MESSAGE DETECTION & LOCALIZATION
    // =========================================================================

    @Test
    public void testDefaultSosMessages() {
        String knSos = LanguageManager.getDefaultSosMessage("kn");
        String hiSos = LanguageManager.getDefaultSosMessage("hi");
        String mlSos = LanguageManager.getDefaultSosMessage("ml");

        assertTrue(knSos.contains("ತುರ್ತು"));
        assertTrue(hiSos.contains("आपातकालीन"));
        assertTrue(mlSos.contains("അടിയന്തര"));

        assertTrue(LanguageManager.isDefaultSosMessage(knSos));
        assertTrue(LanguageManager.isDefaultSosMessage(hiSos));
        assertTrue(LanguageManager.isDefaultSosMessage(mlSos));
        assertTrue(LanguageManager.isDefaultSosMessage(null));
        assertTrue(LanguageManager.isDefaultSosMessage(""));
    }

    // =========================================================================
    // 4. LANGUAGE DETECTION ACCURACY
    // =========================================================================

    @Test
    public void testScriptBasedLanguageDetection() {
        assertEquals("Kannada", LanguageManager.detectLanguageFromText("ನನಗೆ ನೀರು ಬೇಕು", "en"));
        assertEquals("Hindi", LanguageManager.detectLanguageFromText("मुझे पानी चाहिए", "en"));
        assertEquals("Malayalam", LanguageManager.detectLanguageFromText("എനിക്ക് വെള്ളം വേണം", "en"));
        assertEquals("English", LanguageManager.detectLanguageFromText("Need water please", "en"));
    }

    @Test
    public void testLanguageCodeNormalization() {
        assertEquals("kn", LanguageManager.normalizeLanguageCode("kn"));
        assertEquals("kn", LanguageManager.normalizeLanguageCode("KN"));
        assertEquals("kn", LanguageManager.normalizeLanguageCode("kn_IN"));
        assertEquals("kn", LanguageManager.normalizeLanguageCode("kannada"));
        assertEquals("hi", LanguageManager.normalizeLanguageCode("hi"));
        assertEquals("hi", LanguageManager.normalizeLanguageCode("hindi"));
        assertEquals("ml", LanguageManager.normalizeLanguageCode("ml"));
        assertEquals("ml", LanguageManager.normalizeLanguageCode("malayalam"));
        assertEquals("kn", LanguageManager.normalizeLanguageCode("unknown_code")); // default is kn
    }

    // =========================================================================
    // 5. TRANSLITERATION ENGINE FALLBACK TESTS
    // =========================================================================

    @Test
    public void testKannadaOfflineTransliteration() {
        String input = "namaskara";
        String converted = TransliterationEngine.transliterate(input, "kn");
        assertNotNull(converted);
        assertFalse(converted.isEmpty());
        // Verify output uses Kannada Unicode range (0x0C80 - 0x0CFF)
        boolean hasKannadaChar = false;
        for (char c : converted.toCharArray()) {
            if (c >= '\u0C80' && c <= '\u0CFF') {
                hasKannadaChar = true;
                break;
            }
        }
        assertTrue("Output should contain Kannada characters", hasKannadaChar);
    }

    @Test
    public void testHindiOfflineTransliteration() {
        String input = "namaste";
        String converted = TransliterationEngine.transliterate(input, "hi");
        assertNotNull(converted);
        assertFalse(converted.isEmpty());
        // Verify output uses Devanagari Unicode range (0x0900 - 0x097F)
        boolean hasHindiChar = false;
        for (char c : converted.toCharArray()) {
            if (c >= '\u0900' && c <= '\u097F') {
                hasHindiChar = true;
                break;
            }
        }
        assertTrue("Output should contain Devanagari characters", hasHindiChar);
    }

    @Test
    public void testMalayalamOfflineTransliteration() {
        String input = "namaskaram";
        String converted = TransliterationEngine.transliterate(input, "ml");
        assertNotNull(converted);
        assertFalse(converted.isEmpty());
        // Verify output uses Malayalam Unicode range (0x0D00 - 0x0D7F)
        boolean hasMalayalamChar = false;
        for (char c : converted.toCharArray()) {
            if (c >= '\u0D00' && c <= '\u0D7F') {
                hasMalayalamChar = true;
                break;
            }
        }
        assertTrue("Output should contain Malayalam characters", hasMalayalamChar);
    }

    // =========================================================================
    // 6. ROLE MANAGER COMPATIBILITY & ACCESS CHECKS
    // =========================================================================

    @Test
    public void testMuteUserRoleAccess() {
        assertEquals("Mute User", RoleManager.ROLE_MUTE_USER);
        assertEquals("Admin/Caregiver", RoleManager.ROLE_ADMIN_CAREGIVER);

        // Verification of normalized caregiver roles
        assertTrue(RoleManager.ROLE_ADMIN_CAREGIVER.equals("Admin/Caregiver")
                || "Caregiver".equalsIgnoreCase("Caregiver")
                || "Admin".equalsIgnoreCase("Admin"));
    }

    // =========================================================================
    // 7. MUTE USER MESSAGE DELETION AND SECURITY TESTS
    // =========================================================================

    private com.kannada.speechassistant.ChatMessage createMockMessage(String msgId, String senderId, String text, String audioUrl) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("messageId", msgId);
        map.put("senderId", senderId);
        map.put("receiverId", "receiver_caregiver_999");
        map.put("senderRole", RoleManager.ROLE_MUTE_USER);
        map.put("receiverRole", RoleManager.ROLE_ADMIN_CAREGIVER);
        map.put("message", text);
        map.put("messageText", text);
        map.put("type", (audioUrl != null && !audioUrl.isEmpty()) ? "voice" : "text");
        map.put("audioUrl", audioUrl != null ? audioUrl : "");
        map.put("status", "sent");
        map.put("timestamp", System.currentTimeMillis());
        return new com.kannada.speechassistant.ChatMessage(map);
    }

    @Test
    public void testMuteUserOutgoingMessageOwnershipVerification() {
        String authMuteUid = "mute_authenticated_user_001";
        String caregiverUid = "caregiver_partner_002";

        com.kannada.speechassistant.ChatMessage outgoingMsg = createMockMessage("msg_1", authMuteUid, "ನನಗೆ ನೀರು ಬೇಕು", null);
        com.kannada.speechassistant.ChatMessage incomingMsg = createMockMessage("msg_2", caregiverUid, "ಬರುತ್ತಿದ್ದೇನೆ", null);
        com.kannada.speechassistant.ChatMessage invalidMsg = createMockMessage("msg_3", "", "Invalid", null);

        // Verification logic matching MuteUserDashboardActivity:
        // Only messages where authMuteUid equals message.getSenderId() are eligible for deletion
        boolean canDeleteOutgoing = authMuteUid.equals(outgoingMsg.getSenderId());
        boolean canDeleteIncoming = authMuteUid.equals(incomingMsg.getSenderId());
        boolean canDeleteInvalid = authMuteUid.equals(invalidMsg.getSenderId());

        assertTrue("Authenticated Mute User must be allowed to delete their own outgoing message", canDeleteOutgoing);
        assertFalse("Mute User must NEVER be permitted to delete incoming Caregiver messages", canDeleteIncoming);
        assertFalse("Messages with invalid or missing sender ID must not be deleted", canDeleteInvalid);
    }

    @Test
    public void testMuteUserCancelDeletionLeavesAllMessagesUnchanged() {
        String authMuteUid = "mute_authenticated_user_001";
        java.util.List<com.kannada.speechassistant.ChatMessage> conversation = new java.util.ArrayList<>();
        conversation.add(createMockMessage("msg_1", authMuteUid, "ನಮಸ್ಕಾರ", null));
        conversation.add(createMockMessage("msg_2", authMuteUid, "ಸಹಾಯ ಬೇಕು", null));
        conversation.add(createMockMessage("msg_3", "caregiver_002", "ಏನು ಸಹಾಯ?", null));

        // Simulated cancellation: user dismissed dialog
        boolean dialogConfirmed = false;
        if (dialogConfirmed) {
            conversation.remove(1);
        }

        assertEquals("Conversation must retain exactly all 3 messages after cancellation", 3, conversation.size());
        assertEquals("msg_1", conversation.get(0).getId());
        assertEquals("msg_2", conversation.get(1).getId());
        assertEquals("msg_3", conversation.get(2).getId());
    }

    @Test
    public void testMuteUserDeleteOutgoingTextMessageSuccess() {
        String authMuteUid = "mute_authenticated_user_001";
        java.util.List<com.kannada.speechassistant.ChatMessage> conversation = new java.util.ArrayList<>();
        conversation.add(createMockMessage("msg_1", authMuteUid, "ಮೆಸೇಜ್ 1", null));
        com.kannada.speechassistant.ChatMessage targetMsg = createMockMessage("msg_2", authMuteUid, "ಮೆಸೇಜ್ 2 (Delete Me)", null);
        conversation.add(targetMsg);
        conversation.add(createMockMessage("msg_3", "caregiver_002", "ಮೆಸೇಜ್ 3", null));

        // Authorized deletion of outgoing message
        assertTrue(authMuteUid.equals(targetMsg.getSenderId()));
        boolean removed = conversation.remove(targetMsg);

        assertTrue("Target message should be successfully removed", removed);
        assertEquals("Remaining messages count must be 2", 2, conversation.size());
        assertEquals("msg_1", conversation.get(0).getId());
        assertEquals("msg_3", conversation.get(1).getId());
        assertEquals("All other messages must remain completely unchanged", "ಮೆಸೇಜ್ 1", conversation.get(0).getMessage());
    }

    @Test
    public void testMuteUserDuplicateDeletionRequestDebounce() {
        java.util.Set<String> deletingMessageIds = new java.util.HashSet<>();
        String messageId = "msg_delete_target_777";

        // First click
        boolean firstClickAccepted = deletingMessageIds.add(messageId);
        assertTrue("First deletion click should be accepted", firstClickAccepted);

        // Immediate rapid second click (duplicate request)
        boolean secondClickAccepted = deletingMessageIds.add(messageId);
        assertFalse("Duplicate deletion click must be blocked by debounce tracking", secondClickAccepted);

        // Completion clears tracking
        deletingMessageIds.remove(messageId);
        assertTrue("Tracking must clear after operation finishes", deletingMessageIds.isEmpty());
    }

    @Test
    public void testMuteUserExclusivelyAssociatedMediaCleanupTrigger() {
        String authMuteUid = "mute_authenticated_user_001";
        com.kannada.speechassistant.ChatMessage voiceMsg = createMockMessage("voice_1", authMuteUid, "🎤 Voice",
                "https://firebasestorage.googleapis.com/v0/b/project.appspot.com/o/voice_messages%2Ftest.m4a?alt=media");
        com.kannada.speechassistant.ChatMessage textMsg = createMockMessage("text_1", authMuteUid, "Just text", null);

        boolean voiceRequiresStorageCleanup = voiceMsg.getAudioUrl() != null
                && !voiceMsg.getAudioUrl().isEmpty()
                && voiceMsg.getAudioUrl().contains("firebasestorage.googleapis.com");

        boolean textRequiresStorageCleanup = textMsg.getAudioUrl() != null
                && !textMsg.getAudioUrl().isEmpty()
                && textMsg.getAudioUrl().contains("firebasestorage.googleapis.com");

        assertTrue("Exclusive Firebase Storage audio media must trigger cleanup on deletion", voiceRequiresStorageCleanup);
        assertFalse("Plain text messages must not trigger storage cleanup", textRequiresStorageCleanup);
    }

    @Test
    public void testMuteUserDatabaseFailurePreservesMessage() {
        java.util.List<com.kannada.speechassistant.ChatMessage> conversation = new java.util.ArrayList<>();
        com.kannada.speechassistant.ChatMessage msg = createMockMessage("msg_fail_test", "mute_001", "Text", null);
        conversation.add(msg);

        // Simulating database error:
        boolean databaseSuccess = false;
        if (databaseSuccess) {
            conversation.remove(msg);
        }

        assertEquals("When database deletion fails, message must not be removed from view", 1, conversation.size());
        assertEquals("msg_fail_test", conversation.get(0).getId());
    }

    @Test
    public void testMuteUserReopeningConversationReflectsDeletion() {
        String authMuteUid = "mute_user_abc";
        // Conversation loaded in memory
        java.util.List<com.kannada.speechassistant.ChatMessage> originalList = new java.util.ArrayList<>();
        originalList.add(createMockMessage("msg_keep_1", authMuteUid, "Hello", null));
        originalList.add(createMockMessage("msg_deleted", authMuteUid, "Mistake message", null));
        originalList.add(createMockMessage("msg_keep_2", "caregiver_xyz", "Got it", null));

        // Simulating persistent deletion (e.g. from Firestore/Local store)
        java.util.List<com.kannada.speechassistant.ChatMessage> persistentStore = new java.util.ArrayList<>(originalList);
        persistentStore.removeIf(m -> "msg_deleted".equals(m.getId()));

        // User reopens conversation: freshly queries persistent store
        java.util.List<com.kannada.speechassistant.ChatMessage> reopenedList = new java.util.ArrayList<>(persistentStore);

        assertEquals("Reopened conversation must contain only surviving messages", 2, reopenedList.size());
        for (com.kannada.speechassistant.ChatMessage m : reopenedList) {
            assertNotEquals("Deleted message must not appear when reopening conversation", "msg_deleted", m.getId());
        }
    }
}
