package com.ominal.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Creates provider-neutral, hash-chained receipts for workspace changes made by a turn. */
public final class OminalExecutionReceipt {
    public static final String FILE_NAME = "receipts.jsonl";
    private static final int SCHEMA_VERSION = 1;
    private static final Object APPEND_LOCK = new Object();

    public static final class WorkspaceSnapshot {
        @NonNull final Map<String, FileState> files;

        private WorkspaceSnapshot(@NonNull Map<String, FileState> files) {
            this.files = Collections.unmodifiableMap(new LinkedHashMap<>(files));
        }
    }

    public static final class Turn {
        @NonNull public final String sessionId;
        @NonNull public final String harnessId;
        @NonNull public final String modelId;
        @NonNull public final String effortId;
        public final long startedAt;

        public Turn(@NonNull String sessionId, @NonNull String harnessId,
                    @Nullable String modelId, @Nullable String effortId, long startedAt) {
            this.sessionId = sessionId;
            this.harnessId = harnessId;
            this.modelId = modelId == null ? "" : modelId;
            this.effortId = effortId == null ? "" : effortId;
            this.startedAt = startedAt;
        }
    }

    public static final class Change {
        @NonNull public final String kind;
        @NonNull public final String path;
        @Nullable public final String beforeSha256;
        @Nullable public final String afterSha256;
        public final long beforeBytes;
        public final long afterBytes;

        private Change(@NonNull String kind, @NonNull String path,
                       @Nullable FileState before, @Nullable FileState after) {
            this.kind = kind;
            this.path = path;
            beforeSha256 = before == null ? null : before.sha256;
            afterSha256 = after == null ? null : after.sha256;
            beforeBytes = before == null ? -1L : before.bytes;
            afterBytes = after == null ? -1L : after.bytes;
        }
    }

    private static final class FileState {
        @NonNull final String type;
        @NonNull final String sha256;
        final long bytes;

        FileState(@NonNull String type, @NonNull String sha256, long bytes) {
            this.type = type;
            this.sha256 = sha256;
            this.bytes = bytes;
        }

        boolean sameContent(FileState other) {
            return other != null && type.equals(other.type)
                && sha256.equals(other.sha256) && bytes == other.bytes;
        }
    }

    private OminalExecutionReceipt() {}

    @NonNull
    public static WorkspaceSnapshot capture(@NonNull File workspace) throws IOException {
        LinkedHashMap<String, FileState> files = new LinkedHashMap<>();
        if (!workspace.exists()) return new WorkspaceSnapshot(files);
        if (!workspace.isDirectory()) throw new IOException("Workspace is not a directory");
        captureDirectory(workspace, workspace, files);
        return new WorkspaceSnapshot(files);
    }

    @NonNull
    public static List<Change> changes(@NonNull WorkspaceSnapshot before,
                                       @NonNull WorkspaceSnapshot after) {
        ArrayList<Change> changes = new ArrayList<>();
        TreeSet<String> paths = new TreeSet<>();
        paths.addAll(before.files.keySet());
        paths.addAll(after.files.keySet());
        for (String path : paths) {
            FileState oldState = before.files.get(path);
            FileState newState = after.files.get(path);
            if (oldState == null) changes.add(new Change("created", path, null, newState));
            else if (newState == null) changes.add(new Change("deleted", path, oldState, null));
            else if (!oldState.sameContent(newState))
                changes.add(new Change("modified", path, oldState, newState));
        }
        return Collections.unmodifiableList(changes);
    }

    public static void append(@NonNull File sessionRoot, @NonNull Turn turn,
                              @NonNull WorkspaceSnapshot before,
                              @NonNull WorkspaceSnapshot after,
                              @NonNull String outcome, @NonNull String threadId,
                              @NonNull OminalAgentTrace.Snapshot trace,
                              @Nullable OminalAgentTransport.TokenUsage usage,
                              long completedAt) throws IOException {
        File receiptFile = new File(sessionRoot, FILE_NAME);
        File parent = receiptFile.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs())
            throw new IOException("Could not create receipt directory");

        synchronized (APPEND_LOCK) {
            String previousHash = readPreviousHash(receiptFile);
            try {
                JSONObject payload = buildPayload(turn, before, after, outcome, threadId,
                    trace, usage, completedAt);
                String receiptHash = sha256(previousHash + "\n" + canonicalJson(payload));
                JSONObject receipt = new JSONObject()
                    .put("schemaVersion", SCHEMA_VERSION)
                    .put("previousReceiptHash", previousHash)
                    .put("payload", payload)
                    .put("receiptHash", receiptHash);
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(receiptFile, true))) {
                    writer.write(receipt.toString());
                    writer.newLine();
                }
            } catch (JSONException e) {
                throw new IOException("Could not encode execution receipt", e);
            }
        }
    }

    public static boolean verifyChain(@NonNull File receiptFile) throws IOException {
        if (!receiptFile.isFile()) return true;
        String previousHash = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(receiptFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    JSONObject receipt = new JSONObject(line);
                    if (receipt.optInt("schemaVersion", -1) != SCHEMA_VERSION) return false;
                    if (!previousHash.equals(receipt.optString("previousReceiptHash", "")))
                        return false;
                    JSONObject payload = receipt.optJSONObject("payload");
                    if (payload == null) return false;
                    String actual = sha256(previousHash + "\n" + canonicalJson(payload));
                    if (!actual.equals(receipt.optString("receiptHash", ""))) return false;
                    previousHash = actual;
                } catch (JSONException e) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void captureDirectory(File root, File directory,
                                         LinkedHashMap<String, FileState> output)
        throws IOException {
        File[] children = directory.listFiles();
        if (children == null) throw new IOException("Could not read " + directory);
        java.util.Arrays.sort(children, Comparator.comparing(File::getName));
        for (File child : children) {
            String path = root.toPath().relativize(child.toPath()).toString()
                .replace(File.separatorChar, '/');
            if (Files.isSymbolicLink(child.toPath())) {
                byte[] target = Files.readSymbolicLink(child.toPath()).toString()
                    .getBytes(StandardCharsets.UTF_8);
                output.put(path, new FileState("symlink", sha256(target), target.length));
            } else if (child.isDirectory()) {
                output.put(path, new FileState("directory", "", 0L));
                captureDirectory(root, child, output);
            } else if (child.isFile()) {
                output.put(path, new FileState("file", sha256(child), child.length()));
            }
        }
    }

    private static JSONObject buildPayload(Turn turn, WorkspaceSnapshot before,
                                           WorkspaceSnapshot after, String outcome,
                                           String threadId, OminalAgentTrace.Snapshot trace,
                                           @Nullable OminalAgentTransport.TokenUsage usage,
                                           long completedAt) throws JSONException {
        JSONArray changedFiles = new JSONArray();
        for (Change change : changes(before, after)) {
            JSONObject value = new JSONObject()
                .put("kind", change.kind)
                .put("path", change.path)
                .put("beforeBytes", change.beforeBytes)
                .put("afterBytes", change.afterBytes);
            if (change.beforeSha256 != null) value.put("beforeSha256", change.beforeSha256);
            if (change.afterSha256 != null) value.put("afterSha256", change.afterSha256);
            changedFiles.put(value);
        }

        JSONArray actions = new JSONArray();
        for (OminalAgentTrace.Entry entry : trace.entries) {
            actions.put(new JSONObject()
                .put("type", entry.type)
                .put("count", entry.count));
        }

        JSONObject payload = new JSONObject()
            .put("receiptId", turn.sessionId + "-" + turn.startedAt)
            .put("sessionId", turn.sessionId)
            .put("harnessId", turn.harnessId)
            .put("modelId", turn.modelId)
            .put("effortId", turn.effortId)
            .put("threadId", threadId)
            .put("startedAt", turn.startedAt)
            .put("completedAt", completedAt)
            .put("outcome", outcome)
            .put("actions", actions)
            .put("workspaceChanges", changedFiles);
        if (usage != null) {
            payload.put("tokenUsage", new JSONObject()
                .put("input", usage.inputTokens)
                .put("cachedInput", usage.cachedInputTokens)
                .put("output", usage.outputTokens)
                .put("reasoningOutput", usage.reasoningOutputTokens)
                .put("total", usage.totalTokens));
        }
        return payload;
    }

    private static String readPreviousHash(File receiptFile) throws IOException {
        if (!receiptFile.isFile()) return "";
        String last = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(receiptFile))) {
            String line;
            while ((line = reader.readLine()) != null)
                if (!line.trim().isEmpty()) last = line;
        }
        if (last.isEmpty()) return "";
        try {
            return new JSONObject(last).optString("receiptHash", "");
        } catch (JSONException e) {
            throw new IOException("Existing receipt chain is invalid", e);
        }
    }

    private static String canonicalJson(Object value) throws JSONException {
        if (value == null || value == JSONObject.NULL) return "null";
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            ArrayList<String> keys = new ArrayList<>();
            Iterator<String> iterator = object.keys();
            while (iterator.hasNext()) keys.add(iterator.next());
            Collections.sort(keys);
            StringBuilder result = new StringBuilder("{");
            for (int index = 0; index < keys.size(); index++) {
                if (index > 0) result.append(',');
                String key = keys.get(index);
                result.append(JSONObject.quote(key)).append(':')
                    .append(canonicalJson(object.get(key)));
            }
            return result.append('}').toString();
        }
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            StringBuilder result = new StringBuilder("[");
            for (int index = 0; index < array.length(); index++) {
                if (index > 0) result.append(',');
                result.append(canonicalJson(array.get(index)));
            }
            return result.append(']').toString();
        }
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        return JSONObject.quote(value.toString());
    }

    private static String sha256(File file) throws IOException {
        MessageDigest digest = newDigest();
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        return hex(digest.digest());
    }

    private static String sha256(byte[] value) throws IOException {
        return hex(newDigest().digest(value));
    }

    private static String sha256(String value) throws IOException {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private static MessageDigest newDigest() throws IOException {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 is unavailable", e);
        }
    }

    private static String hex(byte[] value) {
        StringBuilder result = new StringBuilder(value.length * 2);
        for (byte item : value) result.append(String.format(java.util.Locale.US, "%02x", item & 0xff));
        return result.toString();
    }
}
