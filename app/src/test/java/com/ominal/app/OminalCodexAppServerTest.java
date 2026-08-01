package com.ominal.app;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class OminalCodexAppServerTest {
    @Test
    public void parsesLastTurnTokenUsage() throws Exception {
        JSONObject last = new JSONObject()
            .put("inputTokens", 12400)
            .put("cachedInputTokens", 10800)
            .put("outputTokens", 221)
            .put("reasoningOutputTokens", 64)
            .put("totalTokens", 12685);
        JSONObject params = new JSONObject()
            .put("tokenUsage", new JSONObject().put("last", last));

        OminalAgentTransport.TokenUsage usage =
            OminalAgentTransport.TokenUsage.fromCodexNotification(params);

        assertEquals(12400, usage.inputTokens);
        assertEquals(10800, usage.cachedInputTokens);
        assertEquals(221, usage.outputTokens);
        assertEquals(64, usage.reasoningOutputTokens);
        assertEquals(12685, usage.totalTokens);
        assertNull(OminalAgentTransport.TokenUsage.fromCodexNotification(new JSONObject()));
    }

    @Test
    public void exposesOnlySafeLifecycleLabels() throws Exception {
        assertEquals("Working", OminalCodexAppServer.describeItem(
            new JSONObject().put("type", "reasoning").put("summary", "private")));
        assertEquals("Running command", OminalCodexAppServer.describeItem(
            new JSONObject().put("type", "commandExecution").put("command", "secret")));
        assertEquals("Editing files", OminalCodexAppServer.describeItem(
            new JSONObject().put("type", "fileChange")));
        assertEquals("", OminalCodexAppServer.describeItem(
            new JSONObject().put("type", "userMessage")));
    }

    @Test
    public void extractsProtocolErrorWithoutDumpingPayload() throws Exception {
        JSONObject error = new JSONObject().put("message", "not authenticated")
            .put("data", new JSONObject().put("access_token", "hidden"));
        assertEquals("not authenticated", OminalCodexAppServer.protocolError(error));
    }

    @Test
    public void recognizesRevokedAndExpiredAuthentication() {
        assertTrue(OminalCodexAppServer.isAuthenticationError(
            "auth error code: token_revoked"));
        assertTrue(OminalCodexAppServer.isAuthenticationError(
            "Encountered invalidated oauth token"));
        assertTrue(OminalCodexAppServer.isAuthenticationError(
            "Your access token could not be refreshed because you have since logged out "
                + "or signed in to another account. Please sign in again."));
        assertTrue(OminalCodexAppServer.isAuthenticationError("HTTP 401 Unauthorized"));
        assertFalse(OminalCodexAppServer.isAuthenticationError(
            "The network request timed out."));
    }

}
