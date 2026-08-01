package com.ominal.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Reduces harness lifecycle events to a bounded, content-free activity trace.
 *
 * Only allowlisted item types are accepted. Protocol payload fields are never
 * stored, which keeps commands, prompts, reasoning, and tool data private.
 */
public final class OminalAgentTrace {
    private static final int SCHEMA_VERSION = 1;
    private static final int MAX_ENTRIES = 8;

    public static final class Entry {
        @NonNull public final String type;
        @NonNull public final String label;
        public final int count;
        public final boolean running;

        private Entry(@NonNull String type, @NonNull String label, int count, boolean running) {
            this.type = type;
            this.label = label;
            this.count = count;
            this.running = running;
        }
    }

    public static final class Snapshot {
        @NonNull public final List<Entry> entries;

        private Snapshot(@NonNull List<Entry> entries) {
            this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        }

        @NonNull
        public static Snapshot empty() {
            return new Snapshot(Collections.emptyList());
        }

        public boolean isEmpty() {
            return entries.isEmpty();
        }

        @NonNull
        public JSONObject toJson() throws JSONException {
            JSONArray values = new JSONArray();
            for (Entry entry : entries) {
                values.put(new JSONObject()
                    .put("type", entry.type)
                    .put("count", entry.count)
                    .put("running", entry.running));
            }
            return new JSONObject()
                .put("schemaVersion", SCHEMA_VERSION)
                .put("entries", values);
        }

        @NonNull
        public static Snapshot fromJson(@Nullable JSONObject object) {
            if (object == null || object.optInt("schemaVersion", -1) != SCHEMA_VERSION)
                return empty();
            JSONArray values = object.optJSONArray("entries");
            if (values == null) return empty();
            ArrayList<Entry> entries = new ArrayList<>();
            int start = Math.max(0, values.length() - MAX_ENTRIES);
            for (int i = start; i < values.length(); i++) {
                JSONObject value = values.optJSONObject(i);
                String type = value == null ? "" : value.optString("type", "");
                String label = labelForType(type);
                if (label.isEmpty()) continue;
                int count = Math.max(1, Math.min(999, value.optInt("count", 1)));
                entries.add(new Entry(type, label, count,
                    value.optBoolean("running", false)));
            }
            return new Snapshot(entries);
        }
    }

    private static final class MutableEntry {
        final long sequence;
        final String type;
        int count;
        int activeCount;

        MutableEntry(long sequence, String type) {
            this.sequence = sequence;
            this.type = type;
            this.count = 1;
            this.activeCount = 1;
        }
    }

    private final ArrayList<MutableEntry> entries = new ArrayList<>();
    private final Map<String, Long> itemEntries = new HashMap<>();
    private long nextSequence = 1L;

    public boolean itemStarted(@Nullable String itemId, @Nullable String itemType) {
        String type = normalizeType(itemType);
        if (type.isEmpty()) return false;
        String id = itemId == null ? "" : itemId.trim();
        if (!id.isEmpty() && itemEntries.containsKey(id)) return false;

        MutableEntry target = entries.isEmpty() ? null : entries.get(entries.size() - 1);
        if (target != null && target.type.equals(type)) {
            target.count++;
            target.activeCount++;
        } else {
            target = new MutableEntry(nextSequence++, type);
            entries.add(target);
            trimToLimit();
        }
        if (!id.isEmpty()) itemEntries.put(id, target.sequence);
        return true;
    }

    public boolean itemCompleted(@Nullable String itemId, @Nullable String itemType) {
        String id = itemId == null ? "" : itemId.trim();
        Long sequence = id.isEmpty() ? null : itemEntries.remove(id);
        if (sequence == null) {
            if (!itemStarted(id, itemType)) return false;
            sequence = id.isEmpty() ? entries.get(entries.size() - 1).sequence
                : itemEntries.remove(id);
        }
        MutableEntry entry = find(sequence);
        if (entry == null) return false;
        entry.activeCount = Math.max(0, entry.activeCount - 1);
        return true;
    }

    public boolean completeAll() {
        boolean changed = false;
        for (MutableEntry entry : entries) {
            if (entry.activeCount > 0) {
                entry.activeCount = 0;
                changed = true;
            }
        }
        itemEntries.clear();
        return changed;
    }

    @NonNull
    public Snapshot snapshot() {
        ArrayList<Entry> values = new ArrayList<>();
        for (MutableEntry entry : entries) {
            String label = labelForType(entry.type);
            if (!label.isEmpty())
                values.add(new Entry(entry.type, label, entry.count, entry.activeCount > 0));
        }
        return new Snapshot(values);
    }

    @NonNull
    static String labelForType(@Nullable String type) {
        if (type == null) return "";
        switch (type) {
            case "plan": return "Planning the work";
            case "reasoning": return "Planning next action";
            case "commandExecution": return "Running a command in this chat";
            case "workspaceRead": return "Inspecting files in this chat";
            case "fileChange": return "Editing files in this chat";
            case "browserInspect": return "Using Chrome to inspect the page";
            case "browserNavigate": return "Using Chrome to open a page";
            case "browserInteract": return "Using Chrome to interact with the page";
            case "mcpToolCall": return "Using a connected service";
            case "dynamicToolCall": return "Using an installed tool";
            case "collabAgentToolCall": return "Delegating part of the work";
            case "webSearch": return "Searching the web";
            case "imageView": return "Inspecting image";
            case "imageGeneration": return "Creating image";
            case "question": return "Waiting for your input";
            case "contextCompaction": return "Organizing the conversation";
            case "agentMessage": return "Writing the reply";
            default: return "";
        }
    }

    private static String normalizeType(@Nullable String type) {
        String normalized = type == null ? "" : type.trim();
        return labelForType(normalized).isEmpty() ? "" : normalized;
    }

    @Nullable
    private MutableEntry find(@Nullable Long sequence) {
        if (sequence == null) return null;
        for (MutableEntry entry : entries)
            if (entry.sequence == sequence) return entry;
        return null;
    }

    private void trimToLimit() {
        while (entries.size() > MAX_ENTRIES) {
            long removed = entries.remove(0).sequence;
            Iterator<Map.Entry<String, Long>> iterator = itemEntries.entrySet().iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getValue() == removed) iterator.remove();
            }
        }
    }
}
