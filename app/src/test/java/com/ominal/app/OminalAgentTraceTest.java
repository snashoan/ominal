package com.ominal.app;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class OminalAgentTraceTest {
    @Test
    public void aggregatesAdjacentEventsAndTracksCompletion() {
        OminalAgentTrace trace = new OminalAgentTrace();

        trace.itemStarted("one", "commandExecution");
        trace.itemStarted("two", "commandExecution");
        trace.itemCompleted("one", "commandExecution");

        OminalAgentTrace.Snapshot running = trace.snapshot();
        assertEquals(1, running.entries.size());
        assertEquals(2, running.entries.get(0).count);
        assertTrue(running.entries.get(0).running);

        trace.itemCompleted("two", "commandExecution");
        assertFalse(trace.snapshot().entries.get(0).running);
    }

    @Test
    public void rejectsPayloadLikeAndUnknownTypes() {
        OminalAgentTrace trace = new OminalAgentTrace();

        assertFalse(trace.itemStarted("one", "cat /private/file"));
        assertFalse(trace.itemStarted("two", "rawReasoning"));
        assertTrue(trace.snapshot().isEmpty());
    }

    @Test
    public void persistedTraceRebuildsLabelsFromAllowlist() throws Exception {
        OminalAgentTrace trace = new OminalAgentTrace();
        trace.itemStarted("one", "webSearch");
        trace.itemCompleted("one", "webSearch");

        JSONObject persisted = trace.snapshot().toJson();
        persisted.getJSONArray("entries").getJSONObject(0).put("label", "secret payload");
        OminalAgentTrace.Snapshot restored = OminalAgentTrace.Snapshot.fromJson(persisted);

        assertEquals("Searching the web", restored.entries.get(0).label);
        assertFalse(restored.entries.get(0).running);
    }

    @Test
    public void capsTraceToEightEntries() {
        OminalAgentTrace trace = new OminalAgentTrace();
        String[] types = {
            "plan", "reasoning", "commandExecution", "fileChange", "mcpToolCall",
            "dynamicToolCall", "webSearch", "imageView", "imageGeneration"
        };
        for (int i = 0; i < types.length; i++) {
            String id = Integer.toString(i);
            trace.itemStarted(id, types[i]);
            trace.itemCompleted(id, types[i]);
        }

        assertEquals(8, trace.snapshot().entries.size());
        assertEquals("reasoning", trace.snapshot().entries.get(0).type);
    }
}
