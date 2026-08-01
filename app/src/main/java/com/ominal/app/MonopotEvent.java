package com.ominal.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/** Provider-neutral event envelope used to project harness activity onto Monolith surfaces. */
public final class MonopotEvent {
    public static final String PROTOCOL = "monopot/1";
    public static final String CHANNEL_STATE = "state";
    public static final String CHANNEL_THREAD = "thread";
    public static final String CHANNEL_MESSAGE = "message";
    public static final String CHANNEL_OPERATION = "operation";
    public static final String CHANNEL_ARTIFACT = "artifact";
    public static final String CHANNEL_USAGE = "usage";
    public static final String CHANNEL_INPUT_REQUEST = "input_request";
    public static final String CHANNEL_TRACE = "trace";
    public static final String CHANNEL_RESULT = "result";

    public static final class Draft {
        @NonNull public final String channel;
        @NonNull public final String state;
        @NonNull public final String summary;
        @NonNull public final JSONObject detail;

        public Draft(@NonNull String channel, @Nullable String state,
                     @Nullable String summary, @Nullable JSONObject detail) {
            if (!isChannel(channel)) throw new IllegalArgumentException("Unknown Monopot channel");
            this.channel = channel;
            this.state = state == null ? "" : state;
            this.summary = summary == null ? "" : summary;
            this.detail = copy(detail);
        }

        @NonNull
        public static Draft operation(@NonNull String state, @Nullable String summary,
                                      @Nullable JSONObject detail) {
            return new Draft(CHANNEL_OPERATION, state, summary, detail);
        }
    }

    @NonNull public final String chatId;
    @NonNull public final String turnId;
    public final long sequence;
    @NonNull public final String harnessId;
    @NonNull public final String channel;
    @NonNull public final String state;
    @NonNull public final String summary;
    @NonNull public final JSONObject detail;
    public final long timestamp;

    public MonopotEvent(@NonNull String chatId, @NonNull String turnId, long sequence,
                        @NonNull String harnessId, @NonNull Draft draft, long timestamp) {
        if (sequence < 1L) throw new IllegalArgumentException("Sequence must be positive");
        this.chatId = chatId;
        this.turnId = turnId;
        this.sequence = sequence;
        this.harnessId = harnessId;
        channel = draft.channel;
        state = draft.state;
        summary = draft.summary;
        detail = copy(draft.detail);
        this.timestamp = timestamp;
    }

    @NonNull
    public JSONObject toJson() throws JSONException {
        return new JSONObject()
            .put("protocol", PROTOCOL)
            .put("chatId", chatId)
            .put("turnId", turnId)
            .put("sequence", sequence)
            .put("harnessId", harnessId)
            .put("channel", channel)
            .put("state", state)
            .put("summary", summary)
            .put("detail", detail)
            .put("timestamp", timestamp);
    }

    @Nullable
    public static MonopotEvent fromJson(@Nullable JSONObject object) {
        if (object == null || !PROTOCOL.equals(object.optString("protocol", ""))) return null;
        String channel = object.optString("channel", "");
        long sequence = object.optLong("sequence", 0L);
        if (!isChannel(channel) || sequence < 1L) return null;
        try {
            return new MonopotEvent(object.optString("chatId", ""),
                object.optString("turnId", ""), sequence,
                object.optString("harnessId", ""),
                new Draft(channel, object.optString("state", ""),
                    object.optString("summary", ""), object.optJSONObject("detail")),
                object.optLong("timestamp", 0L));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static boolean isChannel(@Nullable String channel) {
        return CHANNEL_STATE.equals(channel) || CHANNEL_THREAD.equals(channel)
            || CHANNEL_MESSAGE.equals(channel) || CHANNEL_OPERATION.equals(channel)
            || CHANNEL_ARTIFACT.equals(channel) || CHANNEL_USAGE.equals(channel)
            || CHANNEL_INPUT_REQUEST.equals(channel) || CHANNEL_TRACE.equals(channel)
            || CHANNEL_RESULT.equals(channel);
    }

    private static JSONObject copy(@Nullable JSONObject value) {
        if (value == null) return new JSONObject();
        try {
            return new JSONObject(value.toString());
        } catch (JSONException ignored) {
            return new JSONObject();
        }
    }
}
