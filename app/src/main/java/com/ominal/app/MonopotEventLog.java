package com.ominal.app;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Local append-only Monopot stream for one chat. It never communicates with a provider. */
public final class MonopotEventLog {
    public static final String FILE_NAME = "monopot.jsonl";
    private static final Object APPEND_LOCK = new Object();

    private MonopotEventLog() {}

    public static void append(@NonNull File chatRoot, @NonNull MonopotEvent event)
        throws IOException {
        File file = new File(chatRoot, FILE_NAME);
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs())
            throw new IOException("Could not create Monopot stream directory");
        synchronized (APPEND_LOCK) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                try {
                    writer.write(event.toJson().toString());
                    writer.newLine();
                } catch (JSONException e) {
                    throw new IOException("Could not encode Monopot event", e);
                }
            }
        }
    }

    @NonNull
    public static List<MonopotEvent> read(@NonNull File chatRoot) throws IOException {
        File file = new File(chatRoot, FILE_NAME);
        if (!file.isFile()) return Collections.emptyList();
        ArrayList<MonopotEvent> events = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    MonopotEvent event = MonopotEvent.fromJson(new JSONObject(line));
                    if (event != null) events.add(event);
                } catch (JSONException ignored) {
                }
            }
        }
        return Collections.unmodifiableList(events);
    }
}
