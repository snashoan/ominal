package com.ominal.app;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class OminalChatMediaTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void discoversOnlyNewMediaFiles() throws Exception {
        File workspace = temporaryFolder.newFolder("workspace");
        File existing = write(workspace, "existing.png", "old");
        HashMap<String, String> before = OminalChatMedia.snapshot(workspace);

        write(workspace, "notes.txt", "not media");
        write(workspace, "media/result.png", "new image");

        ArrayList<OminalChatMedia.Item> changed =
            OminalChatMedia.changedSince(workspace, before);

        assertEquals(1, changed.size());
        assertEquals("media/result.png", changed.get(0).path);
        assertEquals("image/png", changed.get(0).mimeType);
        assertTrue(existing.isFile());
    }

    @Test
    public void detectsModifiedMediaFiles() throws Exception {
        File workspace = temporaryFolder.newFolder("workspace");
        File image = write(workspace, "media/result.webp", "first");
        HashMap<String, String> before = OminalChatMedia.snapshot(workspace);

        Files.write(image.toPath(), "second version".getBytes(StandardCharsets.UTF_8));

        ArrayList<OminalChatMedia.Item> changed =
            OminalChatMedia.changedSince(workspace, before);

        assertEquals(1, changed.size());
        assertEquals("media/result.webp", changed.get(0).path);
    }

    @Test
    public void persistsMediaMetadataAndRejectsTraversal() throws Exception {
        File workspace = temporaryFolder.newFolder("workspace");
        write(workspace, "attachments/photo.jpg", "image");
        OminalChatMedia.Item item = OminalChatMedia.fromRelativePath(
            workspace, "attachments/photo.jpg", "image/jpeg");

        JSONArray encoded = OminalChatMedia.toJson(java.util.Collections.singletonList(item));
        ArrayList<OminalChatMedia.Item> decoded = OminalChatMedia.fromJson(encoded);

        assertEquals(1, decoded.size());
        assertEquals("photo.jpg", decoded.get(0).name);
        assertTrue(decoded.get(0).isImage());
        assertTrue(new OminalChatMedia.Item("photo.png", "application/octet-stream", "")
            .isImage());
        assertNull(OminalChatMedia.resolve(workspace, "../outside.png"));
        assertTrue(OminalChatMedia.fromJson(new JSONArray()
            .put(new JSONObject().put("path", "../outside.png"))).isEmpty());
        assertFalse(OminalChatMedia.mimeTypeForName("report.pdf").startsWith("image/"));
    }

    private static File write(File root, String relativePath, String value) throws Exception {
        File file = new File(root, relativePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory()) assertTrue(parent.mkdirs());
        Files.write(file.toPath(), value.getBytes(StandardCharsets.UTF_8));
        return file;
    }
}
