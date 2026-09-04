package com.ominal.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/** Produces a human-readable projection of verified operation events. */
final class OminalEngineeringTrace {
    private OminalEngineeringTrace() {}

    static boolean isVisible(@Nullable MonopotEvent event) {
        if (event == null || !MonopotEvent.CHANNEL_OPERATION.equals(event.channel)) return false;
        JSONObject item = item(event);
        String type = item.optString("type", "");
        if ("reasoning".equals(type) || "plan".equals(type)
            || "agentMessage".equals(type) || "userMessage".equals(type)) {
            return false;
        }
        String summary = event.summary.trim();
        return !type.isEmpty() || !summary.isEmpty() && !"working".equalsIgnoreCase(summary);
    }

    @NonNull
    static String activeLabel(@Nullable JSONObject item, @Nullable String chatId,
                              @Nullable String fallback) {
        JSONObject value = item == null ? new JSONObject() : item;
        Action action = primaryAction(value, chatId);
        if (action != null) return action.present;

        String type = value.optString("type", "");
        if ("commandExecution".equals(type)) {
            String directory = displayPath(value.optString("cwd", ""), chatId);
            return directory.isEmpty() || "workspace".equals(directory)
                ? "Running a command" : "Running a command in " + directory;
        }
        if ("fileChange".equals(type)) return "Updating files";
        return clean(fallback);
    }

    @NonNull
    static String eventLabel(@NonNull MonopotEvent event) {
        JSONObject item = item(event);
        Action action = primaryAction(item, event.chatId);
        boolean failed = "failed".equals(event.state);
        boolean complete = "completed".equals(event.state) || failed;
        if (action != null) {
            if (failed) return action.failed;
            return complete ? action.completed : action.present;
        }

        String type = item.optString("type", "");
        if ("commandExecution".equals(type)) {
            String directory = displayPath(item.optString("cwd", ""), event.chatId);
            String suffix = directory.isEmpty() || "workspace".equals(directory)
                ? "" : " in " + directory;
            if (failed) return "Command failed" + suffix;
            return complete ? "Ran a command" + suffix : "Running a command" + suffix;
        }
        if ("fileChange".equals(type)) {
            if (failed) return "File update failed";
            return complete ? "Updated files" : "Updating files";
        }
        return clean(event.summary).isEmpty() ? "Operation" : clean(event.summary);
    }

    @NonNull
    static JSONObject item(@NonNull MonopotEvent event) {
        JSONObject item = event.detail.optJSONObject("item");
        return item == null ? event.detail : item;
    }

    @NonNull
    static String displayPath(@Nullable String path, @Nullable String chatId) {
        String value = clean(path).replace('\\', '/');
        while (value.startsWith("./")) value = value.substring(2);
        if (value.isEmpty() || ".".equals(value)) return "workspace";

        String id = clean(chatId);
        if (!id.isEmpty()) {
            String legacy = "/root/workspace/" + id + "/workspace";
            String normalized = workspaceRelative(value, legacy);
            if (normalized != null) return normalized;
            String host = "/.ominal/chats/" + id + "/workspace";
            int hostOffset = value.indexOf(host);
            if (hostOffset >= 0) {
                normalized = workspaceRelative(value, value.substring(0, hostOffset) + host);
                if (normalized != null) return normalized;
            }
        }
        String normalized = workspaceRelative(value, "/root/workspace");
        if (normalized != null) return normalized;
        return value.startsWith("/") ? lastSegments(value, 3) : value;
    }

    @Nullable
    private static Action primaryAction(JSONObject item, @Nullable String chatId) {
        JSONArray actions = item.optJSONArray("commandActions");
        if (actions != null) {
            for (int index = 0; index < actions.length(); index++) {
                JSONObject action = actions.optJSONObject(index);
                if (action == null) continue;
                Action result = action(action.optString("type", ""),
                    first(action.optString("path", ""), action.optString("name", "")), chatId);
                if (result != null) return result;
            }
        }

        JSONArray changes = item.optJSONArray("changes");
        if (changes != null && changes.length() > 0) {
            JSONObject change = changes.optJSONObject(0);
            if (change != null) {
                return action(change.optString("kind", "edit"),
                    change.optString("path", ""), chatId);
            }
        }
        return null;
    }

    @Nullable
    private static Action action(String type, String path, @Nullable String chatId) {
        String target = displayPath(path, chatId);
        if (target.isEmpty()) return null;
        switch (clean(type).toLowerCase(Locale.ROOT)) {
            case "read":
            case "view":
            case "open":
                return new Action("Reading " + target, "Read " + target,
                    "Could not read " + target);
            case "search":
            case "find":
                return new Action("Searching " + target, "Searched " + target,
                    "Could not search " + target);
            case "create":
            case "add":
                return new Action("Creating " + target, "Created " + target,
                    "Could not create " + target);
            case "delete":
            case "remove":
                return new Action("Removing " + target, "Removed " + target,
                    "Could not remove " + target);
            case "write":
            case "edit":
            case "update":
            case "modify":
                return new Action("Updating " + target, "Updated " + target,
                    "Could not update " + target);
            default:
                return null;
        }
    }

    @Nullable
    private static String workspaceRelative(String value, String root) {
        if (!value.equals(root) && !value.startsWith(root + "/")) return null;
        String suffix = value.substring(root.length());
        while (suffix.startsWith("/")) suffix = suffix.substring(1);
        return suffix.isEmpty() ? "workspace" : suffix;
    }

    @NonNull
    private static String lastSegments(String value, int count) {
        String[] segments = value.split("/");
        StringBuilder result = new StringBuilder();
        int start = Math.max(0, segments.length - count);
        for (int index = start; index < segments.length; index++) {
            if (segments[index].isEmpty()) continue;
            if (result.length() > 0) result.append('/');
            result.append(segments[index]);
        }
        return result.toString();
    }

    @NonNull
    private static String first(@Nullable String first, @Nullable String second) {
        String value = clean(first);
        return value.isEmpty() ? clean(second) : value;
    }

    @NonNull
    private static String clean(@Nullable String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static final class Action {
        final String present;
        final String completed;
        final String failed;

        Action(String present, String completed, String failed) {
            this.present = present;
            this.completed = completed;
            this.failed = failed;
        }
    }
}
