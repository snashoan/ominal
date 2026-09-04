package com.ominal.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OminalActivityNarratorTest {
    @Test
    public void carriesCompletedActionIntoNextAction() {
        OminalActivityNarrator narrator = new OminalActivityNarrator();

        assertEquals("Now checking the workspace",
            narrator.started("Inspecting files in this chat"));
        assertEquals("Checked the workspace",
            narrator.completed("Inspecting files in this chat"));
        assertEquals("Checked the workspace\nNow running a command",
            narrator.started("Running command"));
    }

    @Test
    public void heartbeatDescribesOnlyKnownWork() {
        OminalActivityNarrator narrator = new OminalActivityNarrator();
        assertEquals("Waiting for Codex  ·  5s", narrator.waiting("Codex", 5));

        narrator.started("Editing files");
        assertEquals("Still updating files  ·  15s", narrator.waiting("Codex", 15));
    }

    @Test
    public void normalizesPrivateLifecycleLabelsWithoutPayloads() {
        assertEquals("Running a command",
            OminalActivityNarrator.presentAction("Running a command in this chat"));
        assertEquals("Used an installed tool",
            OminalActivityNarrator.completedAction("Using an installed tool"));
    }
}
