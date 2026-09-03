package com.ominal.app;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Writes an immutable projection of other chats into one harness workspace. */
final class OminalConversationArchive {
    static final int SCHEMA_VERSION = 1;

    static final class Message {
        @NonNull final String role;
        @NonNull final String text;
        @NonNull final String timestamp;

        Message(@NonNull String role, @NonNull String text, @NonNull String timestamp) {
            this.role = role;
            this.text = text;
            this.timestamp = timestamp;
        }
    }

    static final class Conversation {
        @NonNull final String id;
        @NonNull final String title;
        final long createdAt;
        final long updatedAt;
        @NonNull final List<Message> messages;

        Conversation(@NonNull String id, @NonNull String title, long createdAt, long updatedAt,
                     @NonNull List<Message> messages) {
            this.id = id;
            this.title = title;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
        }
    }

    private OminalConversationArchive() {}

    static void write(@NonNull File target, @NonNull List<Conversation> conversations)
        throws IOException, JSONException {
        File parent = target.getParentFile();
        if (parent == null) throw new IOException("Conversation archive has no parent directory");
        if (!parent.isDirectory() && !parent.mkdirs())
            throw new IOException("Could not create conversation archive directory");
        File temporary = new File(parent, target.getName() + ".tmp");
        try (FileOutputStream stream = new FileOutputStream(temporary, false);
             BufferedWriter writer = new BufferedWriter(
                 new OutputStreamWriter(stream, StandardCharsets.UTF_8))) {
            for (Conversation conversation : conversations) {
                writer.write(toJson(conversation).toString());
                writer.newLine();
            }
            writer.flush();
            stream.getFD().sync();
        }
        try {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        target.setReadable(true, true);
        target.setWritable(true, true);
    }

    @NonNull
    static JSONObject toJson(@NonNull Conversation conversation) throws JSONException {
        JSONArray messages = new JSONArray();
        for (Message message : conversation.messages) {
            messages.put(new JSONObject()
                .put("role", message.role)
                .put("text", message.text)
                .put("timestamp", message.timestamp));
        }
        return new JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("id", conversation.id)
            .put("title", conversation.title)
            .put("createdAt", conversation.createdAt)
            .put("updatedAt", conversation.updatedAt)
            .put("messages", messages);
    }
}
