package com.ominal.app;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class OminalExecutionReceiptTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void capturesCreatedModifiedAndDeletedWorkspacePaths() throws Exception {
        File workspace = temporaryFolder.newFolder("workspace");
        File changed = write(workspace, "src/changed.txt", "before");
        File deleted = write(workspace, "deleted.txt", "gone");
        OminalExecutionReceipt.WorkspaceSnapshot before =
            OminalExecutionReceipt.capture(workspace);

        Files.write(changed.toPath(), "after".getBytes(StandardCharsets.UTF_8));
        assertTrue(deleted.delete());
        write(workspace, "created.txt", "new");
        OminalExecutionReceipt.WorkspaceSnapshot after =
            OminalExecutionReceipt.capture(workspace);

        List<OminalExecutionReceipt.Change> changes =
            OminalExecutionReceipt.changes(before, after);
        assertEquals(3, changes.size());
        assertEquals("created", changes.get(0).kind);
        assertEquals("created.txt", changes.get(0).path);
        assertEquals("deleted", changes.get(1).kind);
        assertEquals("deleted.txt", changes.get(1).path);
        assertEquals("modified", changes.get(2).kind);
        assertEquals("src/changed.txt", changes.get(2).path);
    }

    @Test
    public void appendsAndVerifiesHashChainedReceipts() throws Exception {
        File session = temporaryFolder.newFolder("chat");
        File workspace = new File(session, "workspace");
        assertTrue(workspace.mkdirs());
        OminalExecutionReceipt.WorkspaceSnapshot before =
            OminalExecutionReceipt.capture(workspace);
        write(workspace, "answer.txt", "first");
        OminalExecutionReceipt.WorkspaceSnapshot after =
            OminalExecutionReceipt.capture(workspace);
        OminalAgentTrace trace = new OminalAgentTrace();
        trace.itemStarted("one", "fileChange");
        trace.itemCompleted("one", "fileChange");

        OminalExecutionReceipt.append(session,
            new OminalExecutionReceipt.Turn("chat", "codex", "gpt", "high", 10L),
            before, after, "complete", "thread", trace.snapshot(), null, 20L);
        OminalExecutionReceipt.append(session,
            new OminalExecutionReceipt.Turn("chat", "codex", "gpt", "high", 30L),
            after, after, "complete", "thread", trace.snapshot(), null, 40L);

        File receipts = new File(session, OminalExecutionReceipt.FILE_NAME);
        assertTrue(OminalExecutionReceipt.verifyChain(receipts));
        String content = new String(Files.readAllBytes(receipts.toPath()), StandardCharsets.UTF_8);
        Files.write(receipts.toPath(), content.replace("answer.txt", "other.txt")
            .getBytes(StandardCharsets.UTF_8));
        assertFalse(OminalExecutionReceipt.verifyChain(receipts));
    }

    private static File write(File root, String relativePath, String value) throws Exception {
        File file = new File(root, relativePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory()) assertTrue(parent.mkdirs());
        Files.write(file.toPath(), value.getBytes(StandardCharsets.UTF_8));
        return file;
    }
}
