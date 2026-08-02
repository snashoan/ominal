package com.ominal.app;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reads the JSONL side channel used by the agent to request native UI actions. */
public final class OminalAgentEventLog {
    public static final int SCHEMA_VERSION = 1;
    public static final String TYPE_OPEN_DISPLAY = "open_display";
    public static final String TYPE_REQUEST_USER_INPUT = "request_user_input";
    public static final String TYPE_STATUS = "status";
    public static final String TYPE_RELOAD_UI = "reload_ui";
    public static final String TYPE_ANDROID_OPEN = "android_open";
    public static final String TYPE_ANDROID_SETTINGS = "android_settings";
    public static final String TYPE_ANDROID_APP = "android_app";

    private OminalAgentEventLog() {}

    public static List<Event> read(File file) throws IOException {
        if (file == null || !file.isFile()) return Collections.emptyList();
        ArrayList<Event> events = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Event event = parse(line);
                if (event != null) events.add(event);
            }
        }
        return events;
    }

    public static Event parse(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        try {
            JSONObject object = new JSONObject(line);
            if (object.optInt("schemaVersion", -1) != SCHEMA_VERSION) return null;
            String type = object.optString("type", "").trim();
            if (!TYPE_OPEN_DISPLAY.equals(type)
                && !TYPE_REQUEST_USER_INPUT.equals(type)
                && !TYPE_STATUS.equals(type)
                && !TYPE_RELOAD_UI.equals(type)
                && !TYPE_ANDROID_OPEN.equals(type)
                && !TYPE_ANDROID_SETTINGS.equals(type)
                && !TYPE_ANDROID_APP.equals(type)) return null;
            return new Event(type, object.optString("message", "").trim());
        } catch (JSONException ignored) {
            return null;
        }
    }

    public static Summary summarize(List<Event> events) {
        boolean openDisplay = false;
        boolean userInput = false;
        String reason = "";
        String status = "";
        boolean reloadUi = false;
        ArrayList<Event> androidRequests = new ArrayList<>();
        for (Event event : events) {
            if (TYPE_OPEN_DISPLAY.equals(event.type)) openDisplay = true;
            if (TYPE_REQUEST_USER_INPUT.equals(event.type)) {
                openDisplay = true;
                userInput = true;
            }
            if ((TYPE_OPEN_DISPLAY.equals(event.type) || TYPE_REQUEST_USER_INPUT.equals(event.type))
                && !event.message.isEmpty()) reason = event.message;
            if (TYPE_STATUS.equals(event.type) && !event.message.isEmpty()) status = event.message;
            if (TYPE_RELOAD_UI.equals(event.type)) reloadUi = true;
            if (TYPE_ANDROID_OPEN.equals(event.type)
                || TYPE_ANDROID_SETTINGS.equals(event.type)
                || TYPE_ANDROID_APP.equals(event.type)) androidRequests.add(event);
        }
        return new Summary(openDisplay, userInput, reason, status, reloadUi,
            Collections.unmodifiableList(androidRequests));
    }

    public static final class Event {
        public final String type;
        public final String message;

        Event(String type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static final class Summary {
        public final boolean openDisplay;
        public final boolean userInputRequired;
        public final String reason;
        public final String status;
        public final boolean reloadUi;
        public final List<Event> androidRequests;

        Summary(boolean openDisplay, boolean userInputRequired, String reason, String status,
                boolean reloadUi, List<Event> androidRequests) {
            this.openDisplay = openDisplay;
            this.userInputRequired = userInputRequired;
            this.reason = reason;
            this.status = status;
            this.reloadUi = reloadUi;
            this.androidRequests = androidRequests;
        }
    }
}
