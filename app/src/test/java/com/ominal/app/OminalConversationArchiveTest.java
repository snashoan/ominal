package com.ominal.app;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class OminalConversationArchiveTest {
    @Test
    public void writesProviderNeutralConversationJsonl() throws Exception {
        File directory = Files.createTempDirectory("gir-chat-archive").toFile();
        File archive = new File(directory, "archive.jsonl");
        OminalConversationArchive.Message message =
            new OminalConversationArchive.Message("user", "Build the app", "10:42");
        OminalConversationArchive.write(archive, Arrays.asList(
            new OminalConversationArchive.Conversation("chat-2", "Build", 10L, 20L,
                Arrays.asList(message))));

        String text = new String(Files.readAllBytes(archive.toPath()), StandardCharsets.UTF_8);
        JSONObject chat = new JSONObject(text.trim());
        assertEquals(1, chat.getInt("schemaVersion"));
        assertEquals("chat-2", chat.getString("id"));
        assertEquals("Build the app",
            chat.getJSONArray("messages").getJSONObject(0).getString("text"));
        assertFalse(text.contains("media"));
        assertTrue(archive.isFile());
    }
}
