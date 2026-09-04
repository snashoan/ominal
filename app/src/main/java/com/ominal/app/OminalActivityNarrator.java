package com.ominal.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/** Turns verified lifecycle events into compact, readable progress copy. */
final class OminalActivityNarrator {
    private String lastCompleted = "";
    private String active = "";

    @NonNull
    synchronized String started(@Nullable String action) {
        String normalized = presentAction(action);
        if (!normalized.isEmpty()) active = normalized;
        return compose(active.isEmpty() ? "Working" : "Now " + lowerFirst(active));
    }

    @NonNull
    synchronized String completed(@Nullable String action) {
        String source = presentAction(action);
        if (source.isEmpty()) source = active;
        if (!source.isEmpty()) lastCompleted = completedAction(source);
        active = "";
        return lastCompleted;
    }

    @NonNull
    synchronized String waiting(@NonNull String harnessName, long seconds) {
        String current = active.isEmpty()
            ? "Waiting for " + harnessName
            : "Still " + lowerFirst(active);
        return compose(current + "  ·  " + Math.max(1L, seconds) + "s");
    }

    @NonNull
    synchronized String current(@Nullable String fallback) {
        if (!active.isEmpty()) return compose("Now " + lowerFirst(active));
        String normalized = clean(fallback);
        return compose(normalized.isEmpty() ? "Working" : normalized);
    }

    @NonNull
    private String compose(@NonNull String current) {
        if (lastCompleted.isEmpty() || lastCompleted.equals(current)) return current;
        return lastCompleted + "\n" + current;
    }

    @NonNull
    static String presentAction(@Nullable String action) {
        String value = clean(action);
        switch (value.toLowerCase(Locale.ROOT)) {
            case "planning":
            case "planning next action":
            case "planning the work":
            case "planning the next step":
            case "working":
                return "Planning the next step";
            case "running command":
            case "running a command in this chat":
                return "Running a command";
            case "editing files":
            case "editing files in this chat":
                return "Updating files";
            case "inspecting files in this chat":
                return "Checking the workspace";
            case "using chrome to inspect the page":
                return "Checking the page";
            case "using chrome to open a page":
                return "Opening the page";
            case "using chrome to interact with the page":
                return "Working in the page";
            case "using connected tool":
            case "using a connected service":
                return "Using a connected service";
            case "using tool":
            case "using an installed tool":
                return "Using an installed tool";
            case "searching the web":
                return "Searching the web";
            case "inspecting image":
                return "Checking the image";
            case "creating image":
                return "Creating the image";
            case "waiting for your input":
                return "Waiting for your input";
            case "organizing the conversation":
            case "compacting context":
                return "Organizing the conversation";
            case "writing response":
            case "writing the reply":
            case "responding":
                return "Writing the reply";
            default:
                return value;
        }
    }

    @NonNull
    static String completedAction(@Nullable String action) {
        String value = presentAction(action);
        switch (value.toLowerCase(Locale.ROOT)) {
            case "planning the next step": return "Planned the next step";
            case "running a command": return "Ran a command";
            case "updating files": return "Updated files";
            case "checking the workspace": return "Checked the workspace";
            case "checking the page": return "Checked the page";
            case "opening the page": return "Opened the page";
            case "working in the page": return "Finished the page action";
            case "using a connected service": return "Used a connected service";
            case "using an installed tool": return "Used an installed tool";
            case "searching the web": return "Searched the web";
            case "checking the image": return "Checked the image";
            case "creating the image": return "Created the image";
            case "waiting for your input": return "Received your input";
            case "organizing the conversation": return "Organized the conversation";
            case "writing the reply": return "Prepared the reply";
            default:
                return value.isEmpty() ? "" : "Finished " + lowerFirst(value);
        }
    }

    @NonNull
    private static String clean(@Nullable String value) {
        if (value == null) return "";
        String text = value.trim().replaceAll("\\s+", " ");
        while (text.endsWith(".") || text.endsWith(":"))
            text = text.substring(0, text.length() - 1).trim();
        return text.length() > 120 ? text.substring(0, 117).trim() + "..." : text;
    }

    @NonNull
    private static String lowerFirst(@NonNull String value) {
        if (value.isEmpty()) return value;
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }
}
