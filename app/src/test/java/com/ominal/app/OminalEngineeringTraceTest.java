package com.ominal.app;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class OminalEngineeringTraceTest {
    @Test
    public void describesVerifiedFileReadRelativeToWorkspace() throws Exception {
        JSONObject item = new JSONObject()
            .put("type", "commandExecution")
            .put("cwd", "/root/workspace")
            .put("commandActions", new JSONArray().put(new JSONObject()
                .put("type", "read")
                .put("path", "/root/workspace/app/src/Main.java")));
        MonopotEvent event = event("completed", "Running command", item);

        assertTrue(OminalEngineeringTrace.isVisible(event));
        assertEquals("Read app/src/Main.java", OminalEngineeringTrace.eventLabel(event));
        assertEquals("Reading app/src/Main.java",
            OminalEngineeringTrace.activeLabel(item, "chat-1", "Running command"));
    }

    @Test
    public void normalizesLegacyChatWorkspacePath() {
        assertEquals("src/main.rs", OminalEngineeringTrace.displayPath(
            "/root/workspace/chat-1/workspace/src/main.rs", "chat-1"));
        assertEquals("workspace", OminalEngineeringTrace.displayPath(
            "/root/workspace/chat-1/workspace", "chat-1"));
    }

    @Test
    public void hidesPrivateReasoningEvents() throws Exception {
        MonopotEvent event = event("started", "Working",
            new JSONObject().put("type", "reasoning"));

        assertFalse(OminalEngineeringTrace.isVisible(event));
    }

    private static MonopotEvent event(String state, String summary, JSONObject item)
        throws Exception {
        return new MonopotEvent("chat-1", "turn-1", 1L, "codex",
            MonopotEvent.Draft.operation(state, summary,
                new JSONObject().put("item", item)), 1L);
    }
}
