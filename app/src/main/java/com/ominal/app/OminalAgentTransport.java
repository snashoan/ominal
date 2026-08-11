package com.ominal.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.HashMap;

/** Provider-neutral transport contract between an intelligence harness and Ominal chat. */
public interface OminalAgentTransport {
    interface Listener {
        void onEvent(@NonNull MonopotEvent.Draft event);
        void onThreadReady(@NonNull String threadId);
        void onStatus(@NonNull String status);
        void onMessageChanged(@NonNull String message);
        void onTraceChanged(@NonNull OminalAgentTrace.Snapshot trace);
        void onTokenUsage(@NonNull TokenUsage usage);
        void onComplete(@NonNull String message, @Nullable TokenUsage usage);
        void onError(@NonNull String message);
    }

    final class TurnRequest {
        @NonNull public final String harnessId;
        @NonNull public final String savedThreadId;
        @NonNull public final String guestWorkingDirectory;
        @NonNull public final String prompt;
        @NonNull public final String developerInstructions;
        @NonNull public final String modelId;
        @NonNull public final String effortId;
        @NonNull public final HashMap<String, String> environment;

        public TurnRequest(@NonNull String harnessId, @Nullable String savedThreadId,
                           @NonNull String guestWorkingDirectory, @NonNull String prompt,
                           @NonNull String developerInstructions,
                           @Nullable String modelId,
                           @Nullable String effortId,
                           @NonNull HashMap<String, String> environment) {
            this.harnessId = harnessId;
            this.savedThreadId = savedThreadId == null ? "" : savedThreadId;
            this.guestWorkingDirectory = guestWorkingDirectory;
            this.prompt = prompt;
            this.developerInstructions = developerInstructions;
            this.modelId = modelId == null ? "" : modelId;
            this.effortId = effortId == null ? "" : effortId;
            this.environment = new HashMap<>(environment);
        }
    }

    final class TokenUsage {
        public final long inputTokens;
        public final long cachedInputTokens;
        public final long outputTokens;
        public final long reasoningOutputTokens;
        public final long totalTokens;

        TokenUsage(long inputTokens, long cachedInputTokens, long outputTokens,
                   long reasoningOutputTokens, long totalTokens) {
            this.inputTokens = inputTokens;
            this.cachedInputTokens = cachedInputTokens;
            this.outputTokens = outputTokens;
            this.reasoningOutputTokens = reasoningOutputTokens;
            this.totalTokens = totalTokens;
        }

        @Nullable
        static TokenUsage fromCodexNotification(@Nullable JSONObject params) {
            JSONObject tokenUsage = params == null ? null : params.optJSONObject("tokenUsage");
            JSONObject last = tokenUsage == null ? null : tokenUsage.optJSONObject("last");
            if (last == null) return null;
            return new TokenUsage(last.optLong("inputTokens"), last.optLong("cachedInputTokens"),
                last.optLong("outputTokens"), last.optLong("reasoningOutputTokens"),
                last.optLong("totalTokens"));
        }
    }

    @NonNull
    String harnessId();

    /** Stable runtime adapter identity; independent from the intelligence provider. */
    @NonNull
    default String transportId() {
        return harnessId();
    }

    boolean submit(@NonNull TurnRequest request, @NonNull Listener listener);

    void shutdown();
}
