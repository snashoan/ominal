package com.ominal.app;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominal.shared.logger.Logger;
import com.ominal.shared.runtime.OminalConstants;
import com.ominal.shared.runtime.OminalConstants.OMINAL_APP.OMINAL_SERVICE;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Process-level owner for the selected harness transport and its current turn.
 *
 * The Android activity is a view of this state, not its owner. A terminal result
 * remains available until an activity persists it into the selected chat.
 */
public final class OminalAgentRuntime {
    private static final String LOG_TAG = "OminalAgentRuntime";
    private static final String STATE_FILE_NAME = ".agent-state.json";
    private static final String PHASE_IDLE = "idle";
    private static final String PHASE_RUNNING = "running";
    private static final String PHASE_COMPLETE = "complete";
    private static final String PHASE_CANCELLED = "cancelled";
    private static final String PHASE_ERROR = "error";
    private static final String STATE_DIRECTORY_NAME = ".agent-state";

    public interface Observer {
        void onAgentStateChanged(@NonNull Snapshot snapshot);
    }

    public static final class Snapshot {
        public final long revision;
        @NonNull public final String phase;
        @NonNull public final String sessionId;
        @NonNull public final String harnessId;
        @NonNull public final String threadId;
        @NonNull public final String status;
        @NonNull public final String message;
        @NonNull public final OminalAgentTrace.Snapshot trace;
        @Nullable public final OminalAgentTransport.TokenUsage usage;

        private Snapshot(long revision, @NonNull String phase, @NonNull String sessionId,
                         @NonNull String harnessId, @NonNull String threadId,
                         @NonNull String status,
                         @NonNull String message,
                         @NonNull OminalAgentTrace.Snapshot trace,
                         @Nullable OminalAgentTransport.TokenUsage usage) {
            this.revision = revision;
            this.phase = phase;
            this.sessionId = sessionId;
            this.harnessId = harnessId;
            this.threadId = threadId;
            this.status = status;
            this.message = message;
            this.trace = trace;
            this.usage = usage;
        }

        public boolean isIdle() {
            return PHASE_IDLE.equals(phase);
        }

        public boolean isRunning() {
            return PHASE_RUNNING.equals(phase);
        }

        public boolean isComplete() {
            return PHASE_COMPLETE.equals(phase);
        }

        public boolean isError() {
            return PHASE_ERROR.equals(phase);
        }

        public boolean isCancelled() {
            return PHASE_CANCELLED.equals(phase);
        }

        public boolean isTerminal() {
            return isComplete() || isCancelled() || isError();
        }
    }

    private static final Object INSTANCE_LOCK = new Object();
    private static OminalAgentRuntime sInstance;

    @NonNull
    public static OminalAgentRuntime get(@NonNull Context context, @NonNull String hostChatRoot) {
        synchronized (INSTANCE_LOCK) {
            if (sInstance == null)
                sInstance = new OminalAgentRuntime(context.getApplicationContext(), hostChatRoot);
            return sInstance;
        }
    }

    private final Context mContext;
    private final String mHostChatRoot;
    private final File mLegacyStateFile;
    private final File mStateDirectory;
    private final Map<String, OminalAgentTransport> mSessionTransports = new HashMap<>();
    private final Map<String, OminalAgentTransport> mActiveTransports = new HashMap<>();
    private final Map<String, Snapshot> mSnapshots = new HashMap<>();
    private final Map<String, TurnStream> mTurnStreams = new HashMap<>();
    private final CopyOnWriteArraySet<Observer> mObservers = new CopyOnWriteArraySet<>();
    private Snapshot mSnapshot;
    private long mNextRevision;
    private int mKeepAliveCount;

    private static final class TurnStream {
        @NonNull final OminalAgentTransport.TurnRequest request;
        @NonNull final String transportId;
        @NonNull final String turnId;
        @Nullable final OminalExecutionReceipt.WorkspaceSnapshot before;
        final long startedAt;
        long nextSequence = 1L;

        TurnStream(@NonNull String sessionId,
                   @NonNull OminalAgentTransport.TurnRequest request,
                   @NonNull String transportId,
                   @Nullable OminalExecutionReceipt.WorkspaceSnapshot before,
                   long startedAt) {
            this.request = request;
            this.transportId = transportId;
            this.before = before;
            this.startedAt = startedAt;
            turnId = sessionId + "-" + startedAt;
        }
    }

    private OminalAgentRuntime(@NonNull Context context, @NonNull String hostChatRoot) {
        mContext = context;
        mHostChatRoot = hostChatRoot;
        mLegacyStateFile = new File(hostChatRoot, STATE_FILE_NAME);
        mStateDirectory = new File(hostChatRoot, STATE_DIRECTORY_NAME);
        loadSnapshots();
        mSnapshot = latestSnapshot();
        mNextRevision = Math.max(System.currentTimeMillis(), mSnapshot.revision + 1);
    }

    public void addObserver(@NonNull Observer observer) {
        mObservers.add(observer);
        List<Snapshot> snapshots;
        synchronized (this) {
            snapshots = new ArrayList<>(mSnapshots.values());
            snapshots.sort(Comparator.comparingLong(snapshot -> snapshot.revision));
        }
        if (snapshots.isEmpty()) observer.onAgentStateChanged(snapshot());
        else for (Snapshot snapshot : snapshots) observer.onAgentStateChanged(snapshot);
    }

    public void removeObserver(@NonNull Observer observer) {
        mObservers.remove(observer);
    }

    @NonNull
    public synchronized Snapshot snapshot() {
        return mSnapshot;
    }

    @NonNull
    public synchronized Snapshot snapshot(@Nullable String sessionId) {
        Snapshot snapshot = sessionId == null ? null : mSnapshots.get(sessionId);
        return snapshot == null ? idleSnapshot(Math.max(0L, mNextRevision - 1L)) : snapshot;
    }

    public boolean submit(@NonNull String sessionId,
                          @NonNull OminalAgentTransport.TurnRequest request) {
        OminalAgentTransport transport = transportFor(sessionId, request.harnessId);
        if (transport == null) return false;
        long startedAt = System.currentTimeMillis();
        OminalExecutionReceipt.WorkspaceSnapshot before = null;
        try {
            before = OminalExecutionReceipt.capture(workspaceDirectory(sessionId));
        } catch (IOException e) {
            Logger.logStackTraceWithMessage(LOG_TAG,
                "Could not capture workspace before turn", e);
        }
        synchronized (this) {
            Snapshot existing = mSnapshots.get(sessionId);
            if (existing != null && !existing.isIdle()) return false;
            mTurnStreams.put(sessionId, new TurnStream(sessionId, request,
                transport.transportId(), before, startedAt));
            Snapshot started = new Snapshot(nextRevision(), PHASE_RUNNING, sessionId,
                request.harnessId, request.savedThreadId,
                "Starting " + harnessName(request.harnessId), "",
                OminalAgentTrace.Snapshot.empty(), null);
            mSnapshots.put(sessionId, started);
            mActiveTransports.put(sessionId, transport);
            mSnapshot = started;
            persistSnapshot(started);
        }
        setKeepAlive(true);
        notifyObservers(snapshot(sessionId));
        recordEvent(sessionId, new MonopotEvent.Draft(MonopotEvent.CHANNEL_STATE,
            "started", "Starting " + harnessName(request.harnessId), null));

        boolean accepted = transport.submit(request, new OminalAgentTransport.Listener() {
            @Override
            public void onEvent(@NonNull MonopotEvent.Draft event) {
                publishEvent(sessionId, event);
            }

            @Override
            public void onThreadReady(@NonNull String threadId) {
                try {
                    publishEvent(sessionId, new MonopotEvent.Draft(
                        MonopotEvent.CHANNEL_THREAD, "ready", "",
                        new JSONObject().put("threadId", threadId)));
                } catch (JSONException ignored) {
                }
            }

            @Override
            public void onStatus(@NonNull String status) {
                publishEvent(sessionId, new MonopotEvent.Draft(
                    MonopotEvent.CHANNEL_STATE, "active", status, null));
            }

            @Override
            public void onMessageChanged(@NonNull String message) {
                publishMessage(sessionId, message);
            }

            @Override
            public void onTraceChanged(@NonNull OminalAgentTrace.Snapshot trace) {
                try {
                    publishEvent(sessionId, new MonopotEvent.Draft(
                        MonopotEvent.CHANNEL_TRACE, "updated", "", trace.toJson()));
                } catch (JSONException ignored) {
                }
            }

            @Override
            public void onTokenUsage(@NonNull OminalAgentTransport.TokenUsage usage) {
                try {
                    publishEvent(sessionId, new MonopotEvent.Draft(
                        MonopotEvent.CHANNEL_USAGE, "updated", "", tokenUsageToJson(usage)));
                } catch (JSONException ignored) {
                }
            }

            @Override
            public void onComplete(@NonNull String message,
                                   @Nullable OminalAgentTransport.TokenUsage usage) {
                if (usage != null) onTokenUsage(usage);
                publishResult(sessionId, PHASE_COMPLETE, message);
            }

            @Override
            public void onError(@NonNull String message) {
                publishResult(sessionId, PHASE_ERROR, message);
            }
        });
        if (!accepted) {
            publishResult(sessionId, PHASE_ERROR, harnessName(request.harnessId)
                + " is already working on another request.");
            return false;
        }
        return true;
    }

    public boolean cancel(@NonNull String sessionId) {
        OminalAgentTransport transport;
        Snapshot active;
        synchronized (this) {
            active = mSnapshots.get(sessionId);
            transport = mActiveTransports.get(sessionId);
            if (active == null || !active.isRunning() || transport == null) return false;
        }
        try {
            recordEvent(sessionId, MonopotEvent.Draft.operation(
                "started", "Stopping", new JSONObject().put("operation", "cancel")));
        } catch (JSONException ignored) {
        }
        if (!transport.cancel()) return false;
        publishResult(sessionId, PHASE_CANCELLED, active.message);
        return true;
    }

    public boolean steer(@NonNull String sessionId, @NonNull String message) {
        String guidance = message.trim();
        if (guidance.isEmpty()) return false;
        OminalAgentTransport transport;
        synchronized (this) {
            Snapshot active = mSnapshots.get(sessionId);
            transport = mActiveTransports.get(sessionId);
            if (active == null || !active.isRunning() || transport == null) return false;
        }
        if (!transport.steer(guidance)) return false;
        try {
            publishEvent(sessionId, new MonopotEvent.Draft(
                MonopotEvent.CHANNEL_OPERATION, "started", "Applying your update",
                new JSONObject().put("operation", "steer")));
        } catch (JSONException ignored) {
        }
        return true;
    }

    public synchronized boolean acknowledge(long revision) {
        String acknowledgedSession = null;
        for (Map.Entry<String, Snapshot> entry : mSnapshots.entrySet()) {
            if (entry.getValue().isTerminal() && entry.getValue().revision == revision) {
                acknowledgedSession = entry.getKey();
                break;
            }
        }
        if (acknowledgedSession == null) return false;
        mSnapshots.remove(acknowledgedSession);
        File stateFile = stateFile(acknowledgedSession);
        if (stateFile.exists() && !stateFile.delete())
            Logger.logWarn(LOG_TAG, "Could not delete completed agent state");
        mSnapshot = latestSnapshot();
        return true;
    }

    public void shutdown() {
        LinkedHashMap<OminalAgentTransport, Boolean> transports = new LinkedHashMap<>();
        synchronized (this) {
            for (OminalAgentTransport transport : mSessionTransports.values())
                transports.put(transport, true);
            for (OminalAgentTransport transport : mActiveTransports.values())
                transports.put(transport, true);
        }
        for (OminalAgentTransport transport : transports.keySet()) transport.shutdown();
        synchronized (this) {
            mActiveTransports.clear();
            mSessionTransports.clear();
            mTurnStreams.clear();
            mSnapshots.clear();
            mSnapshot = idleSnapshot(nextRevision());
            File[] stateFiles = mStateDirectory.listFiles(File::isFile);
            if (stateFiles != null) {
                for (File stateFile : stateFiles) {
                    if (!stateFile.delete())
                        Logger.logWarn(LOG_TAG, "Could not clear agent state " + stateFile);
                }
            }
            mKeepAliveCount = 0;
        }
        sendKeepAlive(false);
        notifyObservers(mSnapshot);
    }

    public boolean refreshCodexCapabilities(
        @NonNull String sessionId,
        @NonNull HashMap<String, String> environment,
        @NonNull OminalCodexAppServer.CapabilityListener listener) {
        OminalAgentTransport transport = transportFor(sessionId, OminalHarnessTerminal.CODEX_ID);
        return transport instanceof OminalCodexAppServer
            && ((OminalCodexAppServer) transport).refreshCapabilities(environment, listener);
    }

    public void releaseSessionTransport(@NonNull String sessionId) {
        LinkedHashMap<OminalAgentTransport, Boolean> transports = new LinkedHashMap<>();
        synchronized (this) {
            OminalAgentTransport active = mActiveTransports.remove(sessionId);
            if (active != null) transports.put(active, true);
            String prefix = sessionId + "\n";
            java.util.Iterator<Map.Entry<String, OminalAgentTransport>> iterator =
                mSessionTransports.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, OminalAgentTransport> entry = iterator.next();
                if (!entry.getKey().startsWith(prefix)) continue;
                transports.put(entry.getValue(), true);
                iterator.remove();
            }
        }
        for (OminalAgentTransport transport : transports.keySet()) transport.shutdown();
    }

    public void forgetSession(@NonNull String sessionId) {
        cancel(sessionId);
        releaseSessionTransport(sessionId);
        Snapshot latest;
        synchronized (this) {
            mSnapshots.remove(sessionId);
            mTurnStreams.remove(sessionId);
            File stateFile = stateFile(sessionId);
            if (stateFile.exists() && !stateFile.delete())
                Logger.logWarn(LOG_TAG, "Could not remove agent state for " + sessionId);
            mSnapshot = latestSnapshot();
            latest = mSnapshot;
        }
        notifyObservers(latest);
    }

    @NonNull
    private File chatDirectory(@NonNull String sessionId) {
        return new File(mHostChatRoot, sessionId);
    }

    @NonNull
    private File workspaceDirectory(@NonNull String sessionId) {
        return new File(chatDirectory(sessionId), "workspace");
    }

    @Nullable
    private MonopotEvent recordEvent(@NonNull String sessionId,
                                      @NonNull MonopotEvent.Draft draft) {
        synchronized (this) {
            TurnStream stream = mTurnStreams.get(sessionId);
            if (stream == null) return null;
            MonopotEvent event = new MonopotEvent(sessionId, stream.turnId,
                stream.nextSequence++, stream.request.harnessId, stream.transportId, draft,
                System.currentTimeMillis());
            try {
                MonopotEventLog.append(chatDirectory(sessionId), event);
                MonopotEventLog.append(
                    new File(workspaceDirectory(sessionId), ".ominal/monopot"), event);
            } catch (IOException e) {
                Logger.logStackTraceWithMessage(LOG_TAG,
                    "Could not append Monopot event", e);
            }
            return event;
        }
    }

    private void publishEvent(@NonNull String sessionId,
                              @NonNull MonopotEvent.Draft draft) {
        MonopotEvent event = recordEvent(sessionId, draft);
        if (event != null) applyEvent(event);
    }

    private void publishMessage(@NonNull String sessionId, @NonNull String message) {
        String currentMessage;
        synchronized (this) {
            Snapshot current = mSnapshots.get(sessionId);
            if (current == null || !current.isRunning()) return;
            currentMessage = current.message;
        }
        try {
            JSONObject detail = new JSONObject();
            String state;
            if (message.startsWith(currentMessage)) {
                String delta = message.substring(currentMessage.length());
                if (delta.isEmpty()) return;
                state = "delta";
                detail.put("delta", delta);
            } else {
                state = "replace";
                detail.put("text", message);
            }
            publishEvent(sessionId, new MonopotEvent.Draft(
                MonopotEvent.CHANNEL_MESSAGE, state, "", detail));
        } catch (JSONException ignored) {
        }
    }

    private void publishResult(@NonNull String sessionId, @NonNull String phase,
                               @NonNull String message) {
        try {
            publishEvent(sessionId, new MonopotEvent.Draft(
                MonopotEvent.CHANNEL_RESULT, phase, "",
                new JSONObject().put("message", message)));
        } catch (JSONException ignored) {
        }
    }

    private void applyEvent(@NonNull MonopotEvent event) {
        switch (event.channel) {
            case MonopotEvent.CHANNEL_STATE:
                updateRunning(event.chatId, null,
                    event.summary.isEmpty() ? null : event.summary,
                    null, null, false);
                break;
            case MonopotEvent.CHANNEL_THREAD:
                String threadId = event.detail.optString("threadId", "");
                if (!threadId.isEmpty())
                    updateRunning(event.chatId, threadId, null, null, null, true);
                break;
            case MonopotEvent.CHANNEL_MESSAGE:
                String message;
                synchronized (this) {
                    Snapshot current = mSnapshots.get(event.chatId);
                    if (current == null || !current.isRunning()) return;
                    message = "delta".equals(event.state)
                        ? current.message + event.detail.optString("delta", "")
                        : event.detail.optString("text", current.message);
                }
                updateRunning(event.chatId, null, null, message, null, false);
                break;
            case MonopotEvent.CHANNEL_TRACE:
                updateTrace(event.chatId,
                    OminalAgentTrace.Snapshot.fromJson(event.detail));
                break;
            case MonopotEvent.CHANNEL_USAGE:
                updateRunning(event.chatId, null, null, null,
                    tokenUsageFromJson(event.detail), false);
                break;
            case MonopotEvent.CHANNEL_OPERATION:
                if ("started".equals(event.state) && !event.summary.isEmpty())
                    updateRunning(event.chatId, null, event.summary, null, null, false);
                break;
            case MonopotEvent.CHANNEL_INPUT_REQUEST:
                if (mObservers.isEmpty()) {
                    String inputDetail = event.summary.isEmpty()
                        ? "Open GIR to continue the task." : event.summary;
                    OminalAgentNotification.post(mContext, event.chatId, true, inputDetail);
                }
            case MonopotEvent.CHANNEL_ARTIFACT:
                if (!event.summary.isEmpty())
                    updateRunning(event.chatId, null, event.summary, null, null, false);
                break;
            case MonopotEvent.CHANNEL_RESULT:
                Snapshot current = snapshot(event.chatId);
                finish(event.chatId, event.state,
                    event.detail.optString("message", ""), current.usage);
                break;
            default:
                break;
        }
    }

    private void updateRunning(@NonNull String sessionId,
                               @Nullable String threadId, @Nullable String status,
                               @Nullable String message,
                               @Nullable OminalAgentTransport.TokenUsage usage,
                               boolean persist) {
        Snapshot updated;
        synchronized (this) {
            Snapshot current = mSnapshots.get(sessionId);
            if (current == null || !current.isRunning()) return;
            updated = new Snapshot(nextRevision(), PHASE_RUNNING, current.sessionId,
                current.harnessId, threadId == null ? current.threadId : threadId,
                status == null ? current.status : status,
                message == null ? current.message : message,
                current.trace,
                usage == null ? current.usage : usage);
            mSnapshots.put(sessionId, updated);
            mSnapshot = updated;
            if (persist) persistSnapshot(updated);
        }
        notifyObservers(updated);
    }

    private void updateTrace(@NonNull String sessionId,
                             @NonNull OminalAgentTrace.Snapshot trace) {
        Snapshot updated;
        synchronized (this) {
            Snapshot current = mSnapshots.get(sessionId);
            if (current == null || !current.isRunning()) return;
            updated = new Snapshot(nextRevision(), PHASE_RUNNING, current.sessionId,
                current.harnessId, current.threadId, current.status,
                current.message, trace, current.usage);
            mSnapshots.put(sessionId, updated);
            mSnapshot = updated;
        }
        notifyObservers(updated);
    }

    private void finish(@NonNull String sessionId, @NonNull String phase,
                        @NonNull String message,
                        @Nullable OminalAgentTransport.TokenUsage usage) {
        Snapshot finished;
        TurnStream stream;
        synchronized (this) {
            Snapshot current = mSnapshots.get(sessionId);
            if (current == null || !current.isRunning()) return;
            finished = new Snapshot(nextRevision(), phase, current.sessionId,
                current.harnessId, current.threadId,
                PHASE_COMPLETE.equals(phase) ? "Complete"
                    : PHASE_CANCELLED.equals(phase) ? "Stopped" : "",
                message, current.trace, usage == null ? current.usage : usage);
            mSnapshots.put(sessionId, finished);
            mActiveTransports.remove(sessionId);
            stream = mTurnStreams.remove(sessionId);
            mSnapshot = finished;
            persistSnapshot(finished);
        }
        setKeepAlive(false);
        notifyObservers(finished);
        if (mObservers.isEmpty() && !finished.isCancelled()) {
            boolean attention = finished.isError();
            String detail = attention
                ? "Open GIR to review the interrupted task."
                : harnessName(finished.harnessId) + " finished in the background.";
            OminalAgentNotification.post(mContext, sessionId, attention, detail);
        }
        appendReceiptAsync(sessionId, stream, finished);
    }

    private void appendReceiptAsync(@NonNull String sessionId,
                                    @Nullable TurnStream stream,
                                    @NonNull Snapshot finished) {
        if (stream == null || stream.before == null) return;
        Thread receiptThread = new Thread(() -> {
            try {
                OminalExecutionReceipt.WorkspaceSnapshot after =
                    OminalExecutionReceipt.capture(workspaceDirectory(sessionId));
                OminalExecutionReceipt.append(chatDirectory(sessionId),
                    new OminalExecutionReceipt.Turn(sessionId,
                        stream.request.harnessId, stream.request.modelId,
                        stream.request.effortId, stream.startedAt),
                    stream.before, after, finished.phase, finished.threadId,
                    finished.trace, finished.usage, System.currentTimeMillis());
            } catch (IOException e) {
                Logger.logStackTraceWithMessage(LOG_TAG,
                    "Could not append execution receipt", e);
            }
        }, "ominal-receipt");
        receiptThread.start();
    }

    private void setKeepAlive(boolean active) {
        boolean notifyService;
        synchronized (this) {
            if (active) {
                mKeepAliveCount++;
                notifyService = mKeepAliveCount == 1;
            } else {
                if (mKeepAliveCount > 0) mKeepAliveCount--;
                notifyService = mKeepAliveCount == 0;
            }
        }
        if (notifyService) sendKeepAlive(active);
    }

    private void sendKeepAlive(boolean active) {
        Intent intent = new Intent(mContext, OminalService.class)
            .setAction(active ? OMINAL_SERVICE.ACTION_WAKE_LOCK : OMINAL_SERVICE.ACTION_WAKE_UNLOCK);
        try {
            if (active && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                mContext.startForegroundService(intent);
            else
                mContext.startService(intent);
        } catch (RuntimeException e) {
            Logger.logStackTraceWithMessage(LOG_TAG,
                active ? "Could not start agent keep-alive" : "Could not stop agent keep-alive", e);
        }
    }

    private synchronized long nextRevision() {
        return mNextRevision++;
    }

    private void notifyObservers(@NonNull Snapshot snapshot) {
        for (Observer observer : mObservers) {
            try {
                observer.onAgentStateChanged(snapshot);
            } catch (RuntimeException e) {
                Logger.logStackTraceWithMessage(LOG_TAG, "Agent observer failed", e);
            }
        }
    }

    private void loadSnapshots() {
        Snapshot legacy = loadSnapshot(mLegacyStateFile);
        if (legacy != null && !legacy.sessionId.isEmpty()) {
            mSnapshots.put(legacy.sessionId, legacy);
            persistSnapshot(legacy);
            if (!mLegacyStateFile.delete())
                Logger.logWarn(LOG_TAG, "Could not retire legacy agent state");
        }
        File[] files = mStateDirectory.listFiles(
            file -> file.isFile() && file.getName().endsWith(".json"));
        if (files == null) return;
        for (File file : files) {
            Snapshot snapshot = loadSnapshot(file);
            if (snapshot == null || snapshot.sessionId.isEmpty()) continue;
            Snapshot existing = mSnapshots.get(snapshot.sessionId);
            if (existing == null || snapshot.revision > existing.revision)
                mSnapshots.put(snapshot.sessionId, snapshot);
        }
    }

    @Nullable
    private Snapshot loadSnapshot(@NonNull File stateFile) {
        if (!stateFile.isFile()) return null;
        try {
            String json = new String(java.nio.file.Files.readAllBytes(stateFile.toPath()),
                StandardCharsets.UTF_8);
            JSONObject object = new JSONObject(json);
            String phase = object.optString("phase", PHASE_IDLE);
            OminalAgentTransport.TokenUsage usage = tokenUsageFromJson(
                object.optJSONObject("usage"));
            OminalAgentTrace.Snapshot trace = OminalAgentTrace.Snapshot.fromJson(
                object.optJSONObject("trace"));
            if (PHASE_RUNNING.equals(phase)) {
                phase = PHASE_ERROR;
                object.put("message",
                    "The harness was interrupted while the app process was unavailable.");
            }
            if (!PHASE_COMPLETE.equals(phase) && !PHASE_CANCELLED.equals(phase)
                && !PHASE_ERROR.equals(phase))
                return null;
            return new Snapshot(object.optLong("revision", System.currentTimeMillis()), phase,
                object.optString("sessionId", ""),
                OminalHarnessRegistry.normalizeSelectedId(object.optString("harnessId", "")),
                object.optString("threadId", ""),
                object.optString("status", ""), object.optString("message", ""), trace, usage);
        } catch (IOException | JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Could not restore agent state", e);
            return null;
        }
    }

    private void persistSnapshot(@NonNull Snapshot snapshot) {
        File stateFile = stateFile(snapshot.sessionId);
        File parent = stateFile.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            Logger.logError(LOG_TAG, "Could not create agent state directory");
            return;
        }
        File temporary = new File(stateFile.getAbsolutePath() + ".tmp");
        try {
            JSONObject object = new JSONObject()
                .put("revision", snapshot.revision)
                .put("phase", snapshot.phase)
                .put("sessionId", snapshot.sessionId)
                .put("harnessId", snapshot.harnessId)
                .put("threadId", snapshot.threadId)
                .put("status", snapshot.status)
                .put("message", snapshot.message)
                .put("trace", snapshot.trace.toJson());
            if (snapshot.usage != null) object.put("usage", tokenUsageToJson(snapshot.usage));
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                output.write(object.toString().getBytes(StandardCharsets.UTF_8));
                output.getFD().sync();
            }
            if (!temporary.renameTo(stateFile)) {
                if (stateFile.exists() && !stateFile.delete())
                    throw new IOException("Could not replace old agent state");
                if (!temporary.renameTo(stateFile))
                    throw new IOException("Could not commit agent state");
            }
        } catch (IOException | JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Could not persist agent state", e);
            if (temporary.exists() && !temporary.delete())
                Logger.logWarn(LOG_TAG, "Could not delete temporary agent state");
        }
    }

    @NonNull
    private File stateFile(@NonNull String sessionId) {
        String safeName = sessionId.replaceAll("[^A-Za-z0-9._-]", "_");
        return new File(mStateDirectory, safeName + ".json");
    }

    @NonNull
    private synchronized Snapshot latestSnapshot() {
        Snapshot latest = null;
        for (Snapshot snapshot : mSnapshots.values()) {
            if (latest == null || snapshot.revision > latest.revision) latest = snapshot;
        }
        return latest == null ? idleSnapshot(System.currentTimeMillis()) : latest;
    }

    @NonNull
    private static Snapshot idleSnapshot(long revision) {
        return new Snapshot(revision, PHASE_IDLE, "", "", "", "", "",
            OminalAgentTrace.Snapshot.empty(), null);
    }

    private static JSONObject tokenUsageToJson(@NonNull OminalAgentTransport.TokenUsage usage)
        throws JSONException {
        return new JSONObject()
            .put("inputTokens", usage.inputTokens)
            .put("cachedInputTokens", usage.cachedInputTokens)
            .put("outputTokens", usage.outputTokens)
            .put("reasoningOutputTokens", usage.reasoningOutputTokens)
            .put("totalTokens", usage.totalTokens);
    }

    @Nullable
    private static OminalAgentTransport.TokenUsage tokenUsageFromJson(@Nullable JSONObject usage) {
        if (usage == null) return null;
        return new OminalAgentTransport.TokenUsage(usage.optLong("inputTokens"),
            usage.optLong("cachedInputTokens"), usage.optLong("outputTokens"),
            usage.optLong("reasoningOutputTokens"), usage.optLong("totalTokens"));
    }

    @Nullable
    private synchronized OminalAgentTransport transportFor(@NonNull String sessionId,
                                                            @NonNull String harnessId) {
        if (OminalHarnessTerminal.CODEX_ID.equals(harnessId)
            || OminalHarnessTerminal.ANTIGRAVITY_ID.equals(harnessId)) {
            String key = sessionId + "\n" + harnessId;
            OminalAgentTransport transport = mSessionTransports.get(key);
            if (transport == null) {
                transport = OminalHarnessTerminal.CODEX_ID.equals(harnessId)
                    ? new OminalCodexAppServer(mContext, mHostChatRoot, sessionId)
                    : new OminalPersistentAgyTransport(mContext, mHostChatRoot, sessionId);
                mSessionTransports.put(key, transport);
            }
            return transport;
        }
        OminalHarnessManifest manifest = OminalHarnessManifest.load(harnessId);
        if (manifest != null && !manifest.adapterCommand.isEmpty()) {
            String key = sessionId + "\n" + harnessId;
            OminalAgentTransport transport = mSessionTransports.get(key);
            if (transport == null) {
                transport = new OminalCliAgentTransport(mContext, mHostChatRoot, harnessId);
                mSessionTransports.put(key, transport);
            }
            return transport;
        }
        return null;
    }

    private static String harnessName(String harnessId) {
        OminalAgentHarness harness = OminalHarnessRegistry.find(harnessId);
        return harness == null ? harnessId
            : OminalHarnessRegistry.resolvedDisplayName(harness);
    }
}
