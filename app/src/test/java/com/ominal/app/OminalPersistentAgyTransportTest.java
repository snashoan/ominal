package com.ominal.app;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class OminalPersistentAgyTransportTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void readsLatestCompletedPlannerResponseFromTranscript() throws Exception {
        File transcript = temporaryFolder.newFile("transcript.jsonl");
        String jsonl =
            "{\"source\":\"MODEL\",\"type\":\"PLANNER_RESPONSE\","
                + "\"status\":\"DONE\",\"content\":\"First answer\"}\n"
                + "{\"source\":\"MODEL\",\"type\":\"PLANNER_RESPONSE\","
                + "\"status\":\"RUNNING\",\"content\":\"Draft\"}\n"
                + "partially-written-line\n"
                + "{\"source\":\"MODEL\",\"type\":\"PLANNER_RESPONSE\","
                + "\"status\":\"DONE\",\"content\":\"Final answer\"}\n";
        Files.write(transcript.toPath(), jsonl.getBytes(StandardCharsets.UTF_8));

        assertEquals("Final answer",
            OminalPersistentAgyTransport.readLatestModelResponse(transcript));
    }

    @Test
    public void returnsEmptyWhenNoCompletedPlannerResponseExists() throws Exception {
        File transcript = temporaryFolder.newFile("transcript.jsonl");
        Files.write(transcript.toPath(),
            "{\"source\":\"USER\",\"content\":\"Question\"}\n"
                .getBytes(StandardCharsets.UTF_8));

        assertEquals("",
            OminalPersistentAgyTransport.readLatestModelResponse(transcript));
    }

    @Test
    public void ignoresCompletedResponsesFromBeforeTheCurrentTurn() throws Exception {
        File transcript = temporaryFolder.newFile("transcript.jsonl");
        String previous =
            "{\"source\":\"MODEL\",\"type\":\"PLANNER_RESPONSE\","
                + "\"status\":\"DONE\",\"content\":\"Previous answer\"}\n";
        Files.write(transcript.toPath(), previous.getBytes(StandardCharsets.UTF_8));
        long currentTurnOffset = transcript.length();
        String current =
            "{\"source\":\"USER\",\"content\":\"Next question\"}\n"
                + "{\"source\":\"MODEL\",\"type\":\"PLANNER_RESPONSE\","
                + "\"status\":\"DONE\",\"content\":\"Current answer\"}\n";
        Files.write(transcript.toPath(), current.getBytes(StandardCharsets.UTF_8),
            java.nio.file.StandardOpenOption.APPEND);

        assertEquals("Current answer",
            OminalPersistentAgyTransport.readLatestModelResponse(
                transcript, currentTurnOffset));
    }

    @Test
    public void resolvesAntigravityTranscriptFromItsBoundHostHome() throws Exception {
        File workspace = temporaryFolder.newFolder("workspace");
        File rootfs = temporaryFolder.newFolder("rootfs");
        File agyHome = temporaryFolder.newFolder("antigravity");

        assertEquals(
            new File(agyHome,
                "antigravity-cli/brain/thread/.system_generated/logs/transcript_full.jsonl"),
            OminalPersistentAgyTransport.resolveGuestFile(
                workspace, rootfs, agyHome,
                "/root/.gemini/antigravity-cli/brain/thread/.system_generated/logs/"
                    + "transcript_full.jsonl"));
        assertEquals(new File(workspace, "src/Main.java"),
            OminalPersistentAgyTransport.resolveGuestFile(
                workspace, rootfs, agyHome, "/root/workspace/src/Main.java"));
    }
}
