package com.ominal.app;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class OminalAgentEventLogTest {
    @Test
    public void summarizesStructuredUserInputRequest() {
        ArrayList<OminalAgentEventLog.Event> events = new ArrayList<>();
        events.add(OminalAgentEventLog.parse(
            "{\"schemaVersion\":1,\"type\":\"status\",\"message\":\"Signing in\"}"));
        events.add(OminalAgentEventLog.parse(
            "{\"schemaVersion\":1,\"type\":\"request_user_input\",\"message\":\"Enter the code\"}"));

        OminalAgentEventLog.Summary summary = OminalAgentEventLog.summarize(events);

        assertTrue(summary.openDisplay);
        assertTrue(summary.userInputRequired);
        assertEquals("Enter the code", summary.reason);
        assertEquals("Signing in", summary.status);
        assertFalse(summary.reloadUi);
    }

    @Test
    public void ignoresMalformedAndUnknownEvents() {
        assertNull(OminalAgentEventLog.parse("not json"));
        assertNull(OminalAgentEventLog.parse(
            "{\"schemaVersion\":2,\"type\":\"request_user_input\"}"));
        assertNull(OminalAgentEventLog.parse(
            "{\"schemaVersion\":1,\"type\":\"launch_everything\"}"));

        OminalAgentEventLog.Summary summary = OminalAgentEventLog.summarize(new ArrayList<>());
        assertFalse(summary.openDisplay);
        assertFalse(summary.userInputRequired);
    }

    @Test
    public void collectsOnlySupportedAndroidRequests() {
        ArrayList<OminalAgentEventLog.Event> events = new ArrayList<>();
        events.add(OminalAgentEventLog.parse(
            "{\"schemaVersion\":1,\"type\":\"android_settings\",\"message\":\"\"}"));
        events.add(OminalAgentEventLog.parse(
            "{\"schemaVersion\":1,\"type\":\"android_open\",\"message\":\"https://openai.com\"}"));
        events.add(OminalAgentEventLog.parse(
            "{\"schemaVersion\":1,\"type\":\"status\",\"message\":\"Working\"}"));

        OminalAgentEventLog.Summary summary = OminalAgentEventLog.summarize(events);

        assertEquals(2, summary.androidRequests.size());
        assertEquals(OminalAgentEventLog.TYPE_ANDROID_SETTINGS,
            summary.androidRequests.get(0).type);
        assertEquals("https://openai.com", summary.androidRequests.get(1).message);
    }

    @Test
    public void requestsUiReloadWithoutOpeningDisplay() {
        ArrayList<OminalAgentEventLog.Event> events = new ArrayList<>();
        events.add(OminalAgentEventLog.parse(
            "{\"schemaVersion\":1,\"type\":\"reload_ui\",\"message\":\"\"}"));

        OminalAgentEventLog.Summary summary = OminalAgentEventLog.summarize(events);

        assertTrue(summary.reloadUi);
        assertFalse(summary.openDisplay);
        assertFalse(summary.userInputRequired);
    }
}
