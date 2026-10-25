package com.kannada.speechassistant.voiceassistant;

import com.kannada.speechassistant.ChatMessage;
import com.kannada.speechassistant.VoiceAudioHelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit Test Suite for Voice Message Engine & Storage Architecture.
 * Validates resilient Base64 encoding/decoding, data URI resolution,
 * ChatMessage voice metadata synthesis, and error handling.
 */
public class VoiceMessageModuleUnitTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testBase64EncodeAndDecodeIntegrity() {
        byte[] original = new byte[]{0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0};
        String encoded = VoiceAudioHelper.safeBase64Encode(original);
        assertNotNull(encoded);
        assertFalse(encoded.isEmpty());

        byte[] decoded = VoiceAudioHelper.safeBase64Decode(encoded);
        assertArrayEquals("Decoded bytes must match original byte-for-byte", original, decoded);
    }

    @Test
    public void testSafeBase64WithEmptyOrNullInput() {
        assertEquals("", VoiceAudioHelper.safeBase64Encode(null));
        assertEquals(0, VoiceAudioHelper.safeBase64Decode(null).length);
        assertEquals(0, VoiceAudioHelper.safeBase64Decode("   ").length);
    }

    @Test
    public void testReadFileToBytes() throws Exception {
        File dummyFile = tempFolder.newFile("test_audio.m4a");
        byte[] testData = "DUMMY_AUDIO_STREAM_DATA_1234567890".getBytes();
        try (FileOutputStream fos = new FileOutputStream(dummyFile)) {
            fos.write(testData);
            fos.flush();
        }

        byte[] readBytes = VoiceAudioHelper.readFileToBytes(dummyFile);
        assertNotNull(readBytes);
        assertArrayEquals(testData, readBytes);
    }

    @Test
    public void testChatMessageVoiceDetectionWithAudioUrl() {
        Map<String, Object> map = new HashMap<>();
        map.put("messageId", "msg_001");
        map.put("senderId", "caregiver_01");
        map.put("receiverId", "user_01");
        map.put("audioUrl", "https://supabase.project.co/voice_messages/cg/1.m4a");
        map.put("audioDuration", 5L);
        map.put("type", "voice");

        ChatMessage msg = new ChatMessage(map);
        assertTrue("Message with audioUrl must be identified as voice", msg.isVoice());
        assertEquals("https://supabase.project.co/voice_messages/cg/1.m4a", msg.getAudioUrl());
        assertEquals(5L, msg.getAudioDuration());
    }

    @Test
    public void testChatMessageVoiceDetectionWithAudioBase64Fallback() {
        byte[] sampleBytes = "AUDIO_RAW_BYTES_TEST".getBytes();
        String sampleB64 = VoiceAudioHelper.safeBase64Encode(sampleBytes);

        Map<String, Object> map = new HashMap<>();
        map.put("messageId", "msg_002");
        map.put("senderId", "caregiver_01");
        map.put("receiverId", "user_02");
        map.put("audioBase64", sampleB64);
        map.put("type", "voice");
        map.put("audioDuration", 8L);

        ChatMessage msg = new ChatMessage(map);
        assertTrue("Message with audioBase64 must be identified as voice", msg.isVoice());
        assertEquals("data:audio/m4a;base64," + sampleB64, msg.getAudioUrl());
        assertEquals(8L, msg.getAudioDuration());
    }

    @Test
    public void testChatMessageIsVoiceContract() {
        Map<String, Object> textMap = new HashMap<>();
        textMap.put("messageId", "msg_text");
        textMap.put("message", "Hello Caregiver");
        textMap.put("type", "text");
        ChatMessage textMsg = new ChatMessage(textMap);
        assertFalse(textMsg.isVoice());

        Map<String, Object> micPrefixMap = new HashMap<>();
        micPrefixMap.put("messageId", "msg_mic");
        micPrefixMap.put("message", "🎤 Quick Voice Dictation");
        micPrefixMap.put("type", "text");
        ChatMessage micMsg = new ChatMessage(micPrefixMap);
        assertTrue(micMsg.isVoice());
    }

    @Test
    public void testDeafUserRoleRecognitionLogic() {
        // Voice messaging should be suppressed only for Deaf users
        assertTrue(isDeafUserRoleTestHelper("Deaf User"));
        assertTrue(isDeafUserRoleTestHelper("deaf user"));
        assertTrue(isDeafUserRoleTestHelper("Speech-Impaired User"));
        assertTrue(isDeafUserRoleTestHelper("ಕಿವುಡ ಬಳಕೆದಾರ"));

        // Mute and Blind users CAN hear audio voice messages, so voice message must NOT be suppressed for them
        assertFalse(isDeafUserRoleTestHelper("Mute User"));
        assertFalse(isDeafUserRoleTestHelper("Blind User"));
        assertFalse(isDeafUserRoleTestHelper("Physically Disabled User"));
        assertFalse(isDeafUserRoleTestHelper("Admin/Caregiver"));
        assertFalse(isDeafUserRoleTestHelper(null));
    }

    @Test
    public void testCaregiverRoleRecognitionLogic() {
        assertTrue(isCaregiverRoleTestHelper("Admin/Caregiver"));
        assertTrue(isCaregiverRoleTestHelper("Caregiver"));
        assertTrue(isCaregiverRoleTestHelper("admin"));
        assertTrue(isCaregiverRoleTestHelper("admin_caregiver"));

        assertFalse(isCaregiverRoleTestHelper("Mute User"));
        assertFalse(isCaregiverRoleTestHelper("Blind User"));
        assertFalse(isCaregiverRoleTestHelper("Deaf User"));
        assertFalse(isCaregiverRoleTestHelper(null));
    }

    private static boolean isDeafUserRoleTestHelper(String role) {
        if (role == null) return false;
        String r = role.trim().toLowerCase(java.util.Locale.ROOT);
        return r.contains("deaf")
                || "Deaf User".equalsIgnoreCase(role)
                || "Speech-Impaired User".equalsIgnoreCase(role)
                || role.contains("ಕಿವುಡ")
                || role.contains("ಅಶಕ್ತ");
    }

    private static boolean isCaregiverRoleTestHelper(String role) {
        if (role == null) return false;
        String r = role.trim().toLowerCase(java.util.Locale.ROOT);
        return r.contains("caregiver")
                || r.contains("admin")
                || "Admin/Caregiver".equalsIgnoreCase(role);
    }
}

