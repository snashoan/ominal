package com.ominal.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OminalHarnessRegistryTest {
    @Test
    public void registeredHarnessesAreSelectable() {
        assertTrue(OminalHarnessRegistry.isSelectable("codex"));
        assertTrue(OminalHarnessRegistry.isSelectable("antigravity"));
        assertFalse(OminalHarnessRegistry.isSelectable("claude-code"));
        assertFalse(OminalHarnessRegistry.isSelectable("missing"));
    }

    @Test
    public void validSelectionIsPreservedAndInvalidSelectionFallsBackToCodex() {
        assertEquals("codex", OminalHarnessRegistry.normalizeSelectedId(null));
        assertEquals("codex", OminalHarnessRegistry.normalizeSelectedId("claude-code"));
        assertEquals("antigravity",
            OminalHarnessRegistry.normalizeSelectedId("antigravity"));
        assertEquals("codex", OminalHarnessRegistry.normalizeSelectedId("missing"));
    }

    @Test
    public void codexSeparatesHarnessFromProviderIdentity() {
        OminalAgentHarness harness = OminalHarnessRegistry.activeOrDefault("codex");

        assertNotNull(harness);
        assertEquals("codex", harness.getId());
        assertEquals("openai", harness.getProviderId());
        assertEquals("OpenAI", harness.getPublisherName());
        assertEquals("app-server", harness.getTransport());
        assertTrue(harness.getAuthModes().contains(OminalAgentHarness.AuthMode.BROWSER));
    }
}
