package com.ominal.app;

import android.content.Context;
import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
public class OminalTermuxConfigMigrationTest {
    @Test
    public void importsOnlySupportedTermuxSettings() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        File root = uniqueDirectory(context, "termux-import-ok");
        File archive = new File(root, "settings.tgz");
        File home = new File(root, "home");
        assertTrue(home.mkdirs());

        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(".termux/colors.properties", "foreground=#ffffff\n");
        entries.put(".bashrc", "export EDITOR=vim\n");
        entries.put(".ssh/config", "Host forge\n");
        writeArchive(archive, entries);

        OminalTermuxConfigMigration.Result result = OminalTermuxConfigMigration.importArchive(context,
            Uri.fromFile(archive), home);

        assertEquals(3, result.copiedEntries);
        assertEquals("foreground=#ffffff\n", readFile(new File(home, ".termux/colors.properties")));
        assertEquals("export EDITOR=vim\n", readFile(new File(home, ".bashrc")));
        assertEquals("Host forge\n", readFile(new File(home, ".ssh/config")));
    }

    @Test
    public void rejectsTraversalEntries() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        File root = uniqueDirectory(context, "termux-import-bad");
        File archive = new File(root, "settings.tgz");
        File home = new File(root, "home");
        assertTrue(home.mkdirs());

        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("../outside", "nope\n");
        writeArchive(archive, entries);

        try {
            OminalTermuxConfigMigration.importArchive(context, Uri.fromFile(archive), home);
            fail("Traversal archive should be rejected");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("unsafe"));
        }
        assertTrue(!new File(root.getParentFile(), "outside").exists());
    }

    private static File uniqueDirectory(Context context, String prefix) {
        File root = new File(context.getCacheDir(), prefix + "-" + System.nanoTime());
        assertTrue(root.mkdirs());
        return root;
    }

    private static void writeArchive(File archive, Map<String, String> entries) throws Exception {
        try (GZIPOutputStream output = new GZIPOutputStream(new FileOutputStream(archive))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                byte[] content = entry.getValue().getBytes(StandardCharsets.UTF_8);
                byte[] header = new byte[512];
                byte[] path = entry.getKey().getBytes(StandardCharsets.UTF_8);
                System.arraycopy(path, 0, header, 0, path.length);
                writeOctal(header, 100, 8, 0600);
                writeOctal(header, 124, 12, content.length);
                header[156] = '0';
                output.write(header);
                output.write(content);
                int padding = (512 - content.length % 512) % 512;
                if (padding > 0) output.write(new byte[padding]);
            }
            output.write(new byte[1024]);
        }
    }

    private static void writeOctal(byte[] output, int offset, int length, long value) {
        String encoded = Long.toOctalString(value);
        int start = offset + length - encoded.length() - 1;
        for (int index = offset; index < start; index++) output[index] = '0';
        byte[] bytes = encoded.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, output, start, bytes.length);
    }

    private static String readFile(File file) throws Exception {
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toString("UTF-8");
        }
    }
}
