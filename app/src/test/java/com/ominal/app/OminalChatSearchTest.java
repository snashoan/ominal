package com.ominal.app;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OminalChatSearchTest {
    @Test
    public void exactTitleRanksAboveTranscriptMatch() {
        int exact = OminalChatSearch.score("release build", "Release build",
            Collections.emptyList());
        int transcript = OminalChatSearch.score("release build", "Android work",
            Collections.singletonList("Prepare the release build for testing"));

        assertTrue(exact > transcript);
    }

    @Test
    public void searchesAcrossOlderMessagesAndMultipleTerms() {
        int score = OminalChatSearch.score("network sandbox", "Runtime debugging",
            Arrays.asList("Check package versions", "Fix the sandbox network configuration"));

        assertTrue(score > 0);
    }

    @Test
    public void normalizesPunctuationCaseAndAccents() {
        assertEquals(OminalChatSearch.normalize("Cafe setup"),
            OminalChatSearch.normalize("CAF\u00c9---setup"));
    }

    @Test
    public void rejectsUnrelatedChat() {
        assertEquals(0, OminalChatSearch.score("firefox", "Build Android app",
            Collections.singletonList("Run the unit tests")));
    }
}
