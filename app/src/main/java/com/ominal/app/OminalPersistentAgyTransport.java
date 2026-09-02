package com.ominal.app;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ominal.shared.runtime.OminalConstants.OMINAL_APP.OMINAL_SERVICE;
import com.ominal.shared.runtime.shell.OminalShellManager;
import com.ominal.shared.runtime.shell.command.runner.terminal.OminalSession;
import com.ominal.shared.shell.command.ExecutionCommand;
import com.ominal.shared.shell.command.ExecutionCommand.ShellCreateMode;
import com.ominal.terminal.TerminalEmulator;
import com.ominal.terminal.TerminalSession;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

/**
 * Keeps one Antigravity TUI process alive per chat and bridges its documented
 * hook/transcript surface back into Monolith chat.
 */
public final class OminalPersistentAgyTransport implements OminalAgentTransport {
    private static final long SESSION_START_TIMEOUT_MS = 20_000L;
    private static final long SESSION_RECOVERY_DELAY_MS = 300L;
    private static final int MAX_RECOVERY_ATTEMPTS = 1;
    private static final long EVENT_POLL_MS = 180L;
    private static final long TRANSCRIPT_RETRY_MS = 120L;
    private static final int TRANSCRIPT_READ_ATTEMPTS = 8;

    private final Context mContext;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final String mSessionId;
    private final String mSessionName;
    private final File mHostChatRoot;
    private final File mHostWorkspace;
    private final File mEventFile;
    private final File mRootfs;
    private final File mHostAgyHome;

    private ActiveTurn mActiveTurn;
    private volatile TerminalSession mTerminalSession;
    private int mGeneration;

    public OminalPersistentAgyTransport(@NonNull Context context,
                                       @NonNull String hostChatRoot,
                                       @NonNull String sessionId) {
        mContext = context.getApplicationContext();
        mSessionId = sessionId;
        mSessionName = OminalHarnessTerminal.sessionName(
            sessionId, OminalHarnessTerminal.ANTIGRAVITY_ID);
        mHostChatRoot = new File(hostChatRoot);
        mHostWorkspace = new File(new File(mHostChatRoot, sessionId), "workspace");
        mEventFile = new File(mHostWorkspace, ".ominal/antigravity-events.jsonl");
        File hostHome = mHostChatRoot.getParentFile();
        File hostOminalHome = hostHome == null ? mHostChatRoot : hostHome;
        mRootfs = new File(hostOminalHome,
            ".ominal/runtime/linux/rootfs");
        mHostAgyHome = new File(hostOminalHome, "harnesses/antigravity");
    }

    @NonNull
    @Override
    public String harnessId() {
        return OminalHarnessTerminal.ANTIGRAVITY_ID;
    }

    @NonNull
    @Override
    public String transportId() {
        return "antigravity-persistent-tui";
    }

    @Override
    public synchronized boolean submit(@NonNull TurnRequest request,
                                       @NonNull Listener listener) {
        if (mActiveTurn != null) return false;
        if (!mHostWorkspace.isDirectory() && !mHostWorkspace.mkdirs()) {
            listener.onError("The chat workspace could not be created.");
            return false;
        }
        int generation = ++mGeneration;
        mActiveTurn = new ActiveTurn(generation, request, listener, mEventFile.length());
        listener.onStatus("Opening Antigravity");
        mMainHandler.post(() -> ensureTerminal(generation, System.currentTimeMillis()));
        return true;
    }

    @Override
    public synchronized boolean cancel() {
        if (mActiveTurn == null) return false;
        mGeneration++;
        mActiveTurn = null;
        TerminalSession terminal = mTerminalSession;
        if (terminal != null && terminal.isRunning()) terminal.write("\u0003");
        return true;
    }

    @Override
    public synchronized boolean steer(@NonNull String message) {
        String guidance = message.trim();
        TerminalSession terminal = mTerminalSession;
        if (mActiveTurn == null || guidance.isEmpty() || terminal == null
            || !terminal.isRunning() || terminal.getEmulator() == null) {
            return false;
        }
        terminal.getEmulator().paste(guidance);
        terminal.write("\r");
        mActiveTurn.listener.onStatus("Applying your update");
        return true;
    }

    @Override
    public synchronized void shutdown() {
        mGeneration++;
        mActiveTurn = null;
        // The named terminal belongs to the chat and intentionally outlives this adapter.
        mTerminalSession = null;
    }

    private void ensureTerminal(int generation, long startedAt) {
        ActiveTurn turn = activeTurn(generation);
        if (turn == null) return;

        TerminalSession terminal = findTerminal();
        if (terminal == null) {
            if (!turn.launchRequested) {
                turn.launchRequested = true;
                launchTerminal(turn);
            }
            if (System.currentTimeMillis() - startedAt >= SESSION_START_TIMEOUT_MS) {
                fail(generation, "Antigravity terminal did not start.");
                return;
            }
            mMainHandler.postDelayed(() -> ensureTerminal(generation, startedAt), 120L);
            return;
        }

        mTerminalSession = terminal;
        if (terminal.getEmulator() == null)
            terminal.updateSize(100, 36, 8, 16);
        if (turn.promptInjectedAtLaunch) {
            turn.listener.onStatus("Planning next action");
            startEventMonitor(generation);
        } else {
            mMainHandler.postDelayed(() -> sendPrompt(generation), 100L);
        }
    }

    private void launchTerminal(ActiveTurn turn) {
        turn.promptInjectedAtLaunch = true;
        Intent intent = new Intent(OMINAL_SERVICE.ACTION_SERVICE_EXECUTE);
        intent.setClass(mContext, OminalService.class);
        OminalHarnessTerminal.configureIntent(intent,
            OminalHarnessTerminal.ANTIGRAVITY_ID, mHostWorkspace.getAbsolutePath(),
            turn.request.modelId, turn.request.effortId, promptFor(turn.request));
        intent.putExtra(OMINAL_SERVICE.EXTRA_RUNNER,
            ExecutionCommand.Runner.TERMINAL_SESSION.getName());
        intent.putExtra(OMINAL_SERVICE.EXTRA_SHELL_NAME, mSessionName);
        intent.putExtra(OMINAL_SERVICE.EXTRA_SHELL_CREATE_MODE,
            ShellCreateMode.NO_SHELL_WITH_NAME.getMode());
        intent.putExtra(OMINAL_SERVICE.EXTRA_COMMAND_LABEL, "Antigravity");
        intent.putExtra(OMINAL_SERVICE.EXTRA_SESSION_ACTION,
            Integer.toString(
                OMINAL_SERVICE.VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY));
        ContextCompat.startForegroundService(mContext, intent);
    }

    @Nullable
    private TerminalSession findTerminal() {
        OminalShellManager manager = OminalShellManager.getShellManager();
        if (manager == null) return null;
        for (OminalSession session : new ArrayList<>(manager.mOminalSessions)) {
            String name = session.getExecutionCommand().shellName;
            if (!mSessionName.equals(name)) continue;
            TerminalSession terminal = session.getTerminalSession();
            if (terminal.isRunning() || terminal.getEmulator() == null) return terminal;
        }
        return null;
    }

    private void sendPrompt(int generation) {
        ActiveTurn turn = activeTurn(generation);
        TerminalSession terminal = mTerminalSession;
        if (turn == null || terminal == null || !terminal.isRunning()) {
            if (!recoverTerminal(generation))
                fail(generation, "Antigravity terminal stopped before accepting the request.");
            return;
        }
        TerminalEmulator emulator = terminal.getEmulator();
        if (emulator == null) {
            fail(generation, "Antigravity terminal is not ready.");
            return;
        }

        emulator.paste(promptFor(turn.request));
        terminal.write("\r");
        turn.listener.onStatus("Planning next action");
        startEventMonitor(generation);
    }

    private void startEventMonitor(int generation) {
        Thread monitor = new Thread(() -> {
            while (activeTurn(generation) != null) {
                try {
                    consumeEvents(generation);
                } catch (IOException | JSONException error) {
                    fail(generation, "Antigravity activity could not be read.");
                    return;
                }
                TerminalSession terminal = mTerminalSession;
                if (terminal != null && !terminal.isRunning()) {
                    if (!recoverTerminal(generation))
                        fail(generation,
                            "Antigravity terminal stopped before returning a response.");
                    return;
                }
                try {
                    Thread.sleep(EVENT_POLL_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    fail(generation, "Antigravity activity was interrupted.");
                    return;
                }
            }
        }, "ominal-agy-" + mSessionId);
        monitor.start();
    }

    private boolean recoverTerminal(int generation) {
        Listener listener;
        synchronized (this) {
            ActiveTurn turn = mActiveTurn;
            if (turn == null || turn.generation != generation
                || turn.recoveryAttempts >= MAX_RECOVERY_ATTEMPTS) {
                return false;
            }
            turn.recoveryAttempts++;
            turn.launchRequested = false;
            turn.promptInjectedAtLaunch = false;
            turn.transcriptPath = "";
            turn.transcriptOffset = -1L;
            mTerminalSession = null;
            listener = turn.listener;
        }
        listener.onStatus("Reconnecting Antigravity");
        mMainHandler.postDelayed(
            () -> ensureTerminal(generation, System.currentTimeMillis()),
            SESSION_RECOVERY_DELAY_MS);
        return true;
    }

    private void consumeEvents(int generation) throws IOException, JSONException {
        ActiveTurn turn = activeTurn(generation);
        if (turn == null || !mEventFile.isFile()) return;
        try (RandomAccessFile input = new RandomAccessFile(mEventFile, "r")) {
            if (turn.eventOffset > input.length()) turn.eventOffset = 0L;
            input.seek(turn.eventOffset);
            String line;
            while ((line = input.readLine()) != null) {
                turn.eventOffset = input.getFilePointer();
                if (line.trim().isEmpty()) continue;
                processEvent(turn, new JSONObject(line));
                if (activeTurn(generation) == null) return;
            }
        }
    }

    private void processEvent(ActiveTurn turn, JSONObject event) {
        String hook = event.optString("hook", "");
        JSONObject payload = event.optJSONObject("payload");
        if (payload == null) payload = new JSONObject();
        String conversationId = payload.optString("conversationId", "");
        if (!conversationId.isEmpty() && !conversationId.equals(turn.threadId)) {
            turn.threadId = conversationId;
            turn.listener.onThreadReady(conversationId);
        }
        String transcriptPath = payload.optString("transcriptPath", "");
        if (!transcriptPath.isEmpty()) {
            turn.transcriptPath = transcriptPath;
            if (turn.transcriptOffset < 0L && !"Stop".equals(hook)) {
                File transcript = guestFile(transcriptPath);
                turn.transcriptOffset = transcript == null ? 0L : transcript.length();
            }
        }

        int step = payload.optInt("stepIdx", payload.optInt("invocationNum", -1));
        String itemId;
        boolean traceChanged = false;
        switch (hook) {
            case "PreInvocation":
                itemId = "invocation-" + step;
                traceChanged = turn.trace.itemStarted(itemId, "reasoning");
                turn.listener.onStatus("Planning next action");
                break;
            case "PostInvocation":
                itemId = "invocation-" + step;
                traceChanged = turn.trace.itemCompleted(itemId, "reasoning");
                break;
            case "PreToolUse":
                JSONObject toolCall = payload.optJSONObject("toolCall");
                String toolName = toolCall == null ? "" : toolCall.optString("name", "");
                String activity = OminalCliAgentTransport.activityType(toolName, "tool");
                itemId = "tool-" + step;
                traceChanged = turn.trace.itemStarted(itemId, activity);
                String label = OminalAgentTrace.labelForType(activity);
                if (!label.isEmpty()) turn.listener.onStatus(label);
                emitToolEvent(turn, "started", label, payload);
                break;
            case "PostToolUse":
                itemId = "tool-" + step;
                traceChanged = turn.trace.itemCompleted(itemId, "");
                emitToolEvent(turn, "completed", "", payload);
                break;
            case "Stop":
                if (!payload.optBoolean("fullyIdle", true)) break;
                traceChanged |= turn.trace.completeAll();
                if (traceChanged) turn.listener.onTraceChanged(turn.trace.snapshot());
                String error = payload.optString("error", "").trim();
                if (!error.isEmpty()) {
                    fail(turn.generation, error);
                } else {
                    completeFromTranscript(turn, 0);
                }
                return;
            default:
                break;
        }
        if (traceChanged) turn.listener.onTraceChanged(turn.trace.snapshot());
    }

    private static void emitToolEvent(@NonNull ActiveTurn turn,
                                      @NonNull String state,
                                      @NonNull String summary,
                                      @NonNull JSONObject payload) {
        try {
            JSONObject detail = new JSONObject()
                .put("hook", "started".equals(state) ? "PreToolUse" : "PostToolUse")
                .put("payload", new JSONObject(payload.toString()));
            turn.listener.onEvent(
                MonopotEvent.Draft.operation(state, summary, detail));
        } catch (JSONException ignored) {
        }
    }

    private void completeFromTranscript(ActiveTurn turn, int attempt) {
        if (activeTurn(turn.generation) != turn) return;
        String response = readLatestModelResponse(
            turn.transcriptPath, Math.max(0L, turn.transcriptOffset));
        if (response.isEmpty()) {
            if (attempt + 1 < TRANSCRIPT_READ_ATTEMPTS) {
                mMainHandler.postDelayed(
                    () -> completeFromTranscript(turn, attempt + 1), TRANSCRIPT_RETRY_MS);
                return;
            }
            fail(turn.generation, "Antigravity finished without a readable response.");
            return;
        }
        Listener listener;
        synchronized (this) {
            if (mActiveTurn != turn) return;
            mActiveTurn = null;
            listener = turn.listener;
        }
        listener.onComplete(response, null);
    }

    private String readLatestModelResponse(String guestPath) {
        return readLatestModelResponse(guestPath, 0L);
    }

    private String readLatestModelResponse(String guestPath, long offset) {
        File transcript = guestFile(guestPath);
        return readLatestModelResponse(transcript, offset);
    }

    static String readLatestModelResponse(@Nullable File transcript) {
        return readLatestModelResponse(transcript, 0L);
    }

    static String readLatestModelResponse(@Nullable File transcript, long offset) {
        if (transcript == null || !transcript.isFile()) return "";
        String response = "";
        try (RandomAccessFile reader = new RandomAccessFile(transcript, "r")) {
            reader.seek(Math.min(Math.max(0L, offset), reader.length()));
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    JSONObject step = new JSONObject(line);
                    if (!"MODEL".equalsIgnoreCase(step.optString("source", ""))) continue;
                    if (!"PLANNER_RESPONSE".equalsIgnoreCase(step.optString("type", "")))
                        continue;
                    if (!"DONE".equalsIgnoreCase(step.optString("status", ""))) continue;
                    String content = step.optString("content", "").trim();
                    if (!content.isEmpty()) response = content;
                } catch (JSONException ignored) {
                    // The transcript may be observed while its newest line is still being written.
                }
            }
        } catch (IOException ignored) {
            return "";
        }
        return response;
    }

    @Nullable
    private File guestFile(String path) {
        return resolveGuestFile(mHostWorkspace, mRootfs, mHostAgyHome, path);
    }

    @Nullable
    static File resolveGuestFile(@NonNull File hostWorkspace, @NonNull File rootfs,
                                 @NonNull File hostAgyHome, String path) {
        if (path == null || path.trim().isEmpty()) return null;
        String normalized = path.trim();
        if (normalized.startsWith("~/")) normalized = "/root/" + normalized.substring(2);
        String agyPrefix = "/root/.gemini/";
        if (normalized.equals("/root/.gemini")) return hostAgyHome;
        if (normalized.startsWith(agyPrefix))
            return new File(hostAgyHome, normalized.substring(agyPrefix.length()));
        String workspacePrefix = "/root/workspace/";
        if (normalized.equals("/root/workspace")) return hostWorkspace;
        if (normalized.startsWith(workspacePrefix))
            return new File(hostWorkspace, normalized.substring(workspacePrefix.length()));
        if (!normalized.startsWith("/")) return null;
        return new File(rootfs, normalized.substring(1));
    }

    @NonNull
    private static String promptFor(@NonNull TurnRequest request) {
        return request.developerInstructions.isEmpty()
            ? request.prompt
            : request.developerInstructions + "\n\nUser request:\n" + request.prompt;
    }

    private void fail(int generation, String message) {
        Listener listener;
        synchronized (this) {
            ActiveTurn turn = mActiveTurn;
            if (turn == null || turn.generation != generation) return;
            mActiveTurn = null;
            listener = turn.listener;
        }
        listener.onError(message == null || message.trim().isEmpty()
            ? "Antigravity could not finish the request." : message.trim());
    }

    @Nullable
    private synchronized ActiveTurn activeTurn(int generation) {
        return mActiveTurn != null && mActiveTurn.generation == generation ? mActiveTurn : null;
    }

    private static final class ActiveTurn {
        final int generation;
        final TurnRequest request;
        final Listener listener;
        final OminalAgentTrace trace = new OminalAgentTrace();
        long eventOffset;
        boolean launchRequested;
        boolean promptInjectedAtLaunch;
        int recoveryAttempts;
        String threadId = "";
        String transcriptPath = "";
        long transcriptOffset = -1L;

        ActiveTurn(int generation, TurnRequest request, Listener listener, long eventOffset) {
            this.generation = generation;
            this.request = request;
            this.listener = listener;
            this.eventOffset = eventOffset;
        }
    }
}
