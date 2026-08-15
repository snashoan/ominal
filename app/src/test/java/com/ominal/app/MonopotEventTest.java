package com.ominal.app;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class MonopotEventTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void roundTripsProviderNeutralEnvelope() throws Exception {
        MonopotEvent source = new MonopotEvent("chat", "turn", 3L, "codex",
            "web.chatgpt",
            MonopotEvent.Draft.operation("completed", "Updated a file",
                new JSONObject().put("path", "src/App.java")), 42L);

        MonopotEvent restored = MonopotEvent.fromJson(source.toJson());

        assertNotNull(restored);
        assertEquals("monopot/1", restored.toJson().getString("protocol"));
        assertEquals("operation", restored.channel);
        assertEquals("codex", restored.harnessId);
        assertEquals("web.chatgpt", restored.transportId);
        assertEquals("src/App.java", restored.detail.getString("path"));
        assertEquals(3L, restored.sequence);
    }

    @Test
    public void readsLegacyEnvelopeWithoutTransportIdentity() throws Exception {
        MonopotEvent restored = MonopotEvent.fromJson(new JSONObject()
            .put("protocol", MonopotEvent.PROTOCOL)
            .put("chatId", "chat")
            .put("turnId", "turn")
            .put("sequence", 1L)
            .put("harnessId", "codex")
            .put("channel", MonopotEvent.CHANNEL_STATE));

        assertNotNull(restored);
        assertEquals("codex", restored.transportId);
    }

    @Test
    public void rejectsUnknownChannels() throws Exception {
        assertNull(MonopotEvent.fromJson(new JSONObject()
            .put("protocol", MonopotEvent.PROTOCOL)
            .put("channel", "provider_secret")
            .put("sequence", 1L)));
    }

    @Test
    public void appendsOrderedLocalStream() throws Exception {
        File chat = temporaryFolder.newFolder("chat");
        MonopotEventLog.append(chat, new MonopotEvent("chat", "turn", 1L, "codex",
            new MonopotEvent.Draft(MonopotEvent.CHANNEL_STATE, "active", "Planning", null), 1L));
        MonopotEventLog.append(chat, new MonopotEvent("chat", "turn", 2L, "codex",
            MonopotEvent.Draft.operation("started", "Running tests", null), 2L));

        List<MonopotEvent> events = MonopotEventLog.read(chat);

        assertEquals(2, events.size());
        assertEquals(1L, events.get(0).sequence);
        assertEquals("Running tests", events.get(1).summary);
    }

    @Test
    public void representsCancellationAsProviderNeutralOperation() throws Exception {
        MonopotEvent.Draft cancellation = MonopotEvent.Draft.operation(
            "started", "Stopping", new JSONObject().put("operation", "cancel"));

        MonopotEvent event = new MonopotEvent(
            "chat", "turn", 1L, "codex", "codex-app-server", cancellation, 42L);
        MonopotEvent restored = MonopotEvent.fromJson(event.toJson());

        assertNotNull(restored);
        assertEquals(MonopotEvent.CHANNEL_OPERATION, restored.channel);
        assertEquals("cancel", restored.detail.getString("operation"));
        assertEquals("codex-app-server", restored.transportId);
    }
}
