package com.kannada.speechassistant.voiceassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for Wake-Word candidate detection and extraction in WakeWordManager.
 */
public class WakeWordDetectionTest {

    @Test
    public void testParseAndExtractCandidate_primaryWakeWordResult() {
        String json = "{\"text\": \"assistant\"}";
        String candidate = WakeWordManager.parseAndExtractCandidate(json);
        assertNotNull(candidate);
        assertEquals("assistant", candidate);
    }

    @Test
    public void testParseAndExtractCandidate_primaryWakeWordPartial() {
        String json = "{\"partial\": \"assistant\"}";
        String candidate = WakeWordManager.parseAndExtractCandidate(json);
        assertNotNull(candidate);
        assertEquals("assistant", candidate);
    }

    @Test
    public void testParseAndExtractCandidate_heyAssistantResult() {
        String json = "{\"text\": \"hey assistant\"}";
        String candidate = WakeWordManager.parseAndExtractCandidate(json);
        assertNotNull(candidate);
        assertEquals("hey assistant", candidate);
    }

    @Test
    public void testParseAndExtractCandidate_heyAssistantPartial() {
        String json = "{\"partial\": \"hey assistant\"}";
        String candidate = WakeWordManager.parseAndExtractCandidate(json);
        assertNotNull(candidate);
        assertEquals("hey assistant", candidate);
    }

    @Test
    public void testParseAndExtractCandidate_singleUtteranceWithCommand() {
        String json = "{\"text\": \"assistant open profile\"}";
        String candidate = WakeWordManager.parseAndExtractCandidate(json);
        assertNotNull(candidate);
        assertEquals("assistant open profile", candidate);
    }

    @Test
    public void testParseAndExtractCandidate_stripUnkTokens() {
        String json = "{\"text\": \"[unk] assistant\"}";
        String candidate = WakeWordManager.parseAndExtractCandidate(json);
        assertNotNull(candidate);
        assertEquals("assistant", candidate);
    }

    @Test
    public void testParseAndExtractCandidate_emptyUtteranceReturnsNull() {
        String emptyText = "{\"text\": \"\"}";
        assertNull(WakeWordManager.parseAndExtractCandidate(emptyText));

        String emptyPartial = "{\"partial\": \"\"}";
        assertNull(WakeWordManager.parseAndExtractCandidate(emptyPartial));

        assertNull(WakeWordManager.parseAndExtractCandidate(null));
        assertNull(WakeWordManager.parseAndExtractCandidate("   "));
    }

    @Test
    public void testParseAndExtractCandidate_outOfGrammarWithoutWakeWordReturnsNull() {
        String json = "{\"text\": \"open home\"}";
        String candidate = WakeWordManager.parseAndExtractCandidate(json);
        assertNull(candidate);

        String json2 = "{\"text\": \"call caregiver\"}";
        assertNull(WakeWordManager.parseAndExtractCandidate(json2));
    }

    @Test
    public void testWakeWordCommandExtraction() {
        // Two-step wake word (only "assistant" or "hey assistant" spoken)
        assertEquals("", VoiceIntentMatcher.extractCommandText("assistant"));
        assertEquals("", VoiceIntentMatcher.extractCommandText("hey assistant"));

        // Single utterance commands (wake word + command in one breath)
        assertEquals("open profile", VoiceIntentMatcher.extractCommandText("assistant open profile"));
        assertEquals("go to home", VoiceIntentMatcher.extractCommandText("hey assistant go to home"));
        assertEquals("call caregiver", VoiceIntentMatcher.extractCommandText("assistant call caregiver"));
        assertEquals("send a message", VoiceIntentMatcher.extractCommandText("assistant send a message"));
    }

    @Test
    public void testWakeWordStates() {
        assertEquals(WakeWordManager.State.IDLE, WakeWordManager.State.valueOf("IDLE"));
        assertEquals(WakeWordManager.State.LISTENING, WakeWordManager.State.valueOf("LISTENING"));
        assertEquals(WakeWordManager.State.TRIGGERED, WakeWordManager.State.valueOf("TRIGGERED"));
        assertEquals(WakeWordManager.State.PAUSED, WakeWordManager.State.valueOf("PAUSED"));
        assertEquals(WakeWordManager.State.DISABLED, WakeWordManager.State.valueOf("DISABLED"));
        assertEquals(WakeWordManager.State.ERROR, WakeWordManager.State.valueOf("ERROR"));
    }
}
