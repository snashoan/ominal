package com.ominal.app;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class OminalCliAgentTransportTest {
    @Test
    public void parsesClaudeMessageAndUsage() throws Exception {
        JSONObject event = new JSONObject()
            .put("type", "assistant")
            .put("message", new JSONObject()
                .put("content", new JSONArray()
                    .put(new JSONObject().put("type", "text").put("text", "Done.")))
                .put("usage", new JSONObject()
                    .put("input_tokens", 120)
                    .put("cache_read_input_tokens", 80)
                    .put("output_tokens", 30)));

        assertEquals("Done.", OminalCliAgentTransport.extractMessage(event));
        OminalAgentTransport.TokenUsage usage =
            OminalCliAgentTransport.parseUsage(event);
        assertEquals(120, usage.inputTokens);
        assertEquals(80, usage.cachedInputTokens);
        assertEquals(30, usage.outputTokens);
        assertEquals(150, usage.totalTokens);
    }

    @Test
    public void parsesAntigravityResultObject() throws Exception {
        JSONObject event = new JSONObject()
            .put("type", "result")
            .put("result", new JSONObject()
                .put("text", "Ready.")
                .put("usage", new JSONObject()
                    .put("input_tokens", 250)
                    .put("cache_read_tokens", 100)
                    .put("output_tokens", 50)
                    .put("total_tokens", 300)));

        assertEquals("Ready.", OminalCliAgentTransport.extractMessage(event));
        OminalAgentTransport.TokenUsage usage =
            OminalCliAgentTransport.parseUsage(event);
        assertEquals(100, usage.cachedInputTokens);
        assertEquals(300, usage.totalTokens);
    }

    @Test
    public void parsesAntigravityEnvelopeAndHumanToolStatus() throws Exception {
        JSONObject event = new JSONObject()
            .put("event", "step_update")
            .put("step_update", new JSONObject()
                .put("conversation_id", "conversation-1")
                .put("step_index", 3)
                .put("state", "ACTIVE")
                .put("step_type", "tool")
                .put("tool_name", "browser_get_dom")
                .put("usage", new JSONObject()
                    .put("input_tokens", 30)
                    .put("output_tokens", 5)));

        assertEquals("Using Chrome to inspect the page",
            OminalCliAgentTransport.statusForEvent(event));
        OminalAgentTransport.TokenUsage usage =
            OminalCliAgentTransport.parseUsage(event);
        assertEquals(35, usage.totalTokens);
    }

    @Test
    public void describesStructuredToolUpdateWithoutLeakingPayload() throws Exception {
        JSONObject event = new JSONObject()
            .put("type", "step_update")
            .put("step_type", "tool_call")
            .put("tool_info", new JSONObject()
                .put("name", "shell")
                .put("output", "private"));

        assertEquals("Using an installed tool",
            OminalCliAgentTransport.statusForEvent(event));
        assertEquals("", OminalCliAgentTransport.extractMessage(event));
    }

    @Test
    public void preservesPlainTextFallbackAndRemovesAnsi() {
        assertEquals("First line\nSecond line",
            OminalCliAgentTransport.normalizePlainOutput(
                "\u001b[32mFirst line\u001b[0m\nSecond line"));
    }
}
