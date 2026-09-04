package com.ominal.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominal.BuildConfig;
import com.ominal.shared.logger.Logger;
import com.ominal.shared.runtime.OminalConstants;
import com.ominal.shared.runtime.shell.command.environment.OminalShellEnvironment;
import com.ominal.shared.shell.command.ExecutionCommand;
import com.ominal.shared.shell.command.runner.app.AppShell;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ominal.app.OminalAgentTransport.Listener;
import com.ominal.app.OminalAgentTransport.TokenUsage;
import com.ominal.app.OminalAgentTransport.TurnRequest;

/** Maintains the stdio Codex app-server used by Ominal chat sessions. */
public final class OminalCodexAppServer implements OminalAgentTransport {
    private static final String LOG_TAG = "OminalCodexAppServer";
    static final String AUTHENTICATION_REQUIRED_MESSAGE =
        "Your Codex session expired. Sign in again to continue.";
    private static final String APP_SERVER_COMMAND = "exec codex app-server --listen stdio://";
    private static final long HEARTBEAT_INTERVAL_MS = 5_000L;

    private interface ResponseHandler {
        void onResult(JSONObject result);
        void onError(String message);
    }

    public interface CapabilityListener {
        void onReady();
        void onError(@NonNull String message);
    }

    private static final class ActiveTurn {
        final TurnRequest request;
        final Listener listener;
        final StringBuilder response = new StringBuilder();
        final Map<String, String> itemPhases = new HashMap<>();
        final OminalAgentTrace trace = new OminalAgentTrace();
        final OminalActivityNarrator narrator = new OminalActivityNarrator();
        final long startedAt = SystemClock.elapsedRealtime();
        long lastSignalAt = startedAt;
        String threadId = "";
        String turnId = "";
        String lastAgentMessage = "";
        TokenUsage tokenUsage;

        ActiveTurn(TurnRequest request, Listener listener) {
            this.request = request;
            this.listener = listener;
        }
    }

    private final Context mContext;
    private final String mSessionId;
    private final String mHostWorkspace;
    private final Map<Integer, ResponseHandler> mPendingRequests = new LinkedHashMap<>();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final Runnable mCapabilityTimeout = () -> finishCapabilityFromCacheOrFail(
        "Codex model discovery timed out. Try again when the network is stable.");
    private AppShell mShell;
    private ActiveTurn mActiveTurn;
    private CapabilityListener mCapabilityListener;
    private boolean mCapabilityRequestInFlight;
    private int mNextRequestId = 1;
    private int mGeneration;
    private boolean mInitialized;
    private boolean mStopping;

    public OminalCodexAppServer(@NonNull Context context, @NonNull String hostChatRoot,
                                @NonNull String sessionId) {
        mContext = context.getApplicationContext();
        mSessionId = sessionId;
        mHostWorkspace = new File(new File(hostChatRoot, sessionId), "workspace")
            .getAbsolutePath();
    }

    @NonNull
    @Override
    public String harnessId() {
        return OminalHarnessTerminal.CODEX_ID;
    }

    @NonNull
    @Override
    public String transportId() {
        return "codex-app-server";
    }

    @Override
    public synchronized boolean submit(@NonNull TurnRequest request, @NonNull Listener listener) {
        if (mActiveTurn != null) return false;
        mStopping = false;
        mActiveTurn = new ActiveTurn(request, listener);
        listener.onStatus(mInitialized ? "Opening chat" : "Starting Codex");
        scheduleHeartbeat(mActiveTurn);
        if (mInitialized && mShell != null) {
            openThread();
        } else {
            startProcess(request.environment);
        }
        return true;
    }

    @Override
    public synchronized boolean cancel() {
        ActiveTurn active = mActiveTurn;
        if (active == null) return false;
        if (!active.threadId.isEmpty() && !active.turnId.isEmpty()) {
            try {
                writeLine(new JSONObject()
                    .put("method", "turn/interrupt")
                    .put("id", mNextRequestId++)
                    .put("params", new JSONObject()
                        .put("threadId", active.threadId)
                        .put("turnId", active.turnId)));
            } catch (JSONException ignored) {
            }
        }
        mActiveTurn = null;
        mInitialized = false;
        mPendingRequests.clear();
        mGeneration++;
        AppShell shell = mShell;
        mShell = null;
        if (shell != null) shell.kill();
        return true;
    }

    @Override
    public synchronized boolean steer(@NonNull String message) {
        ActiveTurn active = mActiveTurn;
        String guidance = message.trim();
        if (active == null || guidance.isEmpty() || active.threadId.isEmpty()
            || active.turnId.isEmpty() || mShell == null) {
            return false;
        }
        try {
            JSONObject params = new JSONObject()
                .put("threadId", active.threadId)
                .put("input", new JSONArray().put(new JSONObject()
                    .put("type", "text")
                    .put("text", guidance)))
                .put("expectedTurnId", active.turnId);
            sendRequest("turn/steer", params, new ResponseHandler() {
                @Override
                public void onResult(JSONObject result) {
                    ActiveTurn current = mActiveTurn;
                    if (current != null)
                        current.listener.onStatus(current.narrator.started("Applying your update"));
                }

                @Override
                public void onError(String error) {
                    ActiveTurn current = mActiveTurn;
                    if (current != null)
                        current.listener.onStatus(current.narrator.current("Continuing current work"));
                }
            });
            return true;
        } catch (JSONException ignored) {
            return false;
        }
    }

    public synchronized boolean refreshCapabilities(
        @NonNull HashMap<String, String> environment,
        @NonNull CapabilityListener listener) {
        if (mCapabilityRequestInFlight) return false;
        mCapabilityRequestInFlight = true;
        mCapabilityListener = listener;
        mMainHandler.postDelayed(mCapabilityTimeout, 25_000L);
        if (mInitialized && mShell != null) requestModelPage(null, new JSONArray());
        else startProcess(environment);
        return true;
    }

    @Override
    public synchronized void shutdown() {
        mStopping = true;
        mInitialized = false;
        mPendingRequests.clear();
        failCapability("Codex stopped.");
        mGeneration++;
        AppShell shell = mShell;
        mShell = null;
        if (shell != null) shell.kill();
        mActiveTurn = null;
    }

    private void startProcess(HashMap<String, String> environment) {
        mInitialized = false;
        mPendingRequests.clear();
        mNextRequestId = 1;
        final int generation = ++mGeneration;

        File workspace = new File(mHostWorkspace);
        if (!workspace.isDirectory() && !workspace.mkdirs()) {
            failAll("The chat workspace could not be created.");
            return;
        }
        ExecutionCommand command = new ExecutionCommand(-1,
            OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH + "/sh",
            new String[]{"-lc", APP_SERVER_COMMAND}, null, mHostWorkspace,
            ExecutionCommand.Runner.APP_SHELL.getName(), false);
        command.commandLabel = "Codex app server";

        AppShell shell = AppShell.execute(mContext, command,
            exited -> onProcessExited(generation, exited), new OminalShellEnvironment(), environment,
            line -> onStdoutLine(generation, line), line -> onStderrLine(generation, line), false);
        if (shell == null) {
            failAll("Codex could not start.");
            return;
        }
        mShell = shell;
        initialize();
    }

    private synchronized void onProcessExited(int generation, AppShell shell) {
        if (generation != mGeneration) return;
        mShell = null;
        mInitialized = false;
        mPendingRequests.clear();
        if (!mStopping) {
            failAll("Codex stopped unexpectedly. Try again.");
        }
    }

    private synchronized void onStdoutLine(int generation, String line) {
        if (generation != mGeneration || line == null || line.trim().isEmpty()) return;
        if (mActiveTurn != null) mActiveTurn.lastSignalAt = SystemClock.elapsedRealtime();
        try {
            handleMessage(new JSONObject(line));
        } catch (JSONException e) {
            Logger.logWarn(LOG_TAG, "Ignoring non-JSON app-server output");
        }
    }

    private synchronized void onStderrLine(int generation, String line) {
        if (generation != mGeneration || line == null || line.trim().isEmpty()) return;
        if (mActiveTurn != null) mActiveTurn.lastSignalAt = SystemClock.elapsedRealtime();
        Logger.logWarn(LOG_TAG, "Codex app server: " + line);
        if (isAuthenticationError(line)) failAll(AUTHENTICATION_REQUIRED_MESSAGE);
    }

    private void initialize() {
        try {
            JSONObject clientInfo = new JSONObject()
                .put("name", "ominal_android")
                .put("title", "Ominal")
                .put("version", BuildConfig.VERSION_NAME);
            JSONObject capabilities = new JSONObject().put("experimentalApi", true);
            JSONObject params = new JSONObject()
                .put("clientInfo", clientInfo)
                .put("capabilities", capabilities);
            sendRequest("initialize", params, new ResponseHandler() {
                @Override
                public void onResult(JSONObject result) {
                    sendNotification("initialized", new JSONObject());
                    mInitialized = true;
                    if (mCapabilityListener != null)
                        requestModelPage(null, new JSONArray());
                    if (mActiveTurn != null) openThread();
                }

                @Override
                public void onError(String message) {
                    failAll("Codex initialization failed: " + message);
                }
            });
        } catch (JSONException e) {
            failAll("Codex initialization failed.");
        }
    }

    private void requestModelPage(@Nullable String cursor, @NonNull JSONArray catalog) {
        try {
            JSONObject params = new JSONObject()
                .put("limit", 100)
                .put("includeHidden", false);
            if (cursor != null && !cursor.isEmpty()) params.put("cursor", cursor);
            sendRequest("model/list", params, new ResponseHandler() {
                @Override
                public void onResult(JSONObject result) {
                    JSONArray data = result == null ? null : result.optJSONArray("data");
                    if (data != null) {
                        for (int index = 0; index < data.length() && catalog.length() < 128; index++)
                            catalog.put(data.optJSONObject(index));
                    }
                    String next = result == null ? "" : result.optString("nextCursor", "");
                    if (!next.isEmpty() && catalog.length() < 128) {
                        requestModelPage(next, catalog);
                        return;
                    }
                    if (writeModelManifest(catalog)) finishCapability();
                    else failCapability("Codex returned an invalid model catalog.");
                }

                @Override
                public void onError(String message) {
                    finishCapabilityFromCacheOrFail(
                        "Could not read Codex models: " + message);
                }
            });
        } catch (JSONException e) {
            finishCapabilityFromCacheOrFail("Could not read Codex models.");
        }
    }

    private boolean writeModelManifest(JSONArray catalog) {
        try {
            JSONArray effectiveCatalog = catalog;
            if (effectiveCatalog.length() == 0) {
                File cache = new File(OminalConstants.OMINAL_HOME_DIR_PATH,
                    ".ominal/codex/models_cache.json");
                if (cache.isFile()) {
                    JSONObject cached = new JSONObject(new String(
                        Files.readAllBytes(cache.toPath()), StandardCharsets.UTF_8));
                    JSONArray cachedModels = cached.optJSONArray("models");
                    if (cachedModels != null) effectiveCatalog = cachedModels;
                }
            }
            JSONArray models = new JSONArray();
            for (int index = 0; index < effectiveCatalog.length(); index++) {
                JSONObject source = effectiveCatalog.optJSONObject(index);
                if (source == null || source.optBoolean("hidden", false)) continue;
                String id = source.optString("model",
                    source.optString("id", source.optString("slug", ""))).trim();
                if (id.isEmpty()) continue;
                JSONArray efforts = new JSONArray();
                JSONArray supported = source.optJSONArray("supportedReasoningEfforts");
                if (supported == null)
                    supported = source.optJSONArray("supported_reasoning_levels");
                if (supported != null) {
                    for (int effortIndex = 0; effortIndex < supported.length(); effortIndex++) {
                        JSONObject effort = supported.optJSONObject(effortIndex);
                        String value = effort == null ? ""
                            : effort.optString("reasoningEffort",
                                effort.optString("effort", "")).trim();
                        if (!value.isEmpty()) efforts.put(value);
                    }
                }
                models.put(new JSONObject()
                    .put("id", id)
                    .put("label", source.optString("displayName",
                        source.optString("display_name", id)))
                    .put("efforts", efforts));
            }
            if (models.length() == 0) return false;
            JSONObject manifest = new JSONObject()
                .put("schemaVersion", OminalHarnessManifest.SCHEMA_VERSION)
                .put("harness", OminalHarnessTerminal.CODEX_ID)
                .put("binaryVersion", "app-server")
                .put("identity", new JSONObject()
                    .put("name", "Codex")
                    .put("publisher", "OpenAI"))
                .put("transport", new JSONObject()
                    .put("outputFormat", "json")
                    .put("resumeFlag", "--resume")
                    .put("modelFlag", "--model")
                    .put("effortFlag", "--effort"))
                .put("autonomy", new JSONObject()
                    .put("flag", "")
                    .put("enabledByDefault", false))
                .put("models", models)
                .put("commands", new JSONArray());
            OminalHarnessManifest.fromJson(manifest);
            File target = OminalHarnessManifest.manifestFile(OminalHarnessTerminal.CODEX_ID);
            File parent = target.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) return false;
            try (FileOutputStream output = new FileOutputStream(target, false)) {
                output.write(manifest.toString(2).getBytes(StandardCharsets.UTF_8));
            }
            return true;
        } catch (IOException | JSONException | RuntimeException error) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Could not save Codex model catalog", error);
            return false;
        }
    }

    private synchronized void finishCapabilityFromCacheOrFail(@NonNull String message) {
        if (!mCapabilityRequestInFlight) return;
        if (writeModelManifest(new JSONArray())) finishCapability();
        else failCapability(message);
    }

    private void openThread() {
        ActiveTurn turn = mActiveTurn;
        if (turn == null) return;
        if (turn.request.savedThreadId.isEmpty()) {
            startThread();
        } else {
            resumeThread(turn.request.savedThreadId);
        }
    }

    private void startThread() {
        ActiveTurn turn = mActiveTurn;
        if (turn == null) return;
        try {
            JSONObject params = threadParams(turn.request)
                .put("ephemeral", false);
            sendRequest("thread/start", params, new ResponseHandler() {
                @Override
                public void onResult(JSONObject result) {
                    acceptThread(result);
                }

                @Override
                public void onError(String message) {
                    failActive("Could not create the Codex chat: " + message);
                }
            });
        } catch (JSONException e) {
            failActive("Could not create the Codex chat.");
        }
    }

    private void resumeThread(String threadId) {
        ActiveTurn turn = mActiveTurn;
        if (turn == null) return;
        try {
            JSONObject params = threadParams(turn.request)
                .put("threadId", threadId);
            sendRequest("thread/resume", params, new ResponseHandler() {
                @Override
                public void onResult(JSONObject result) {
                    acceptThread(result);
                }

                @Override
                public void onError(String message) {
                    Logger.logWarn(LOG_TAG, "Saved Codex thread could not be resumed: " + message);
                    startThread();
                }
            });
        } catch (JSONException e) {
            startThread();
        }
    }

    private JSONObject threadParams(TurnRequest request) throws JSONException {
        JSONObject params = new JSONObject()
            .put("cwd", request.guestWorkingDirectory)
            .put("approvalPolicy", "never")
            .put("sandbox", "danger-full-access")
            .put("developerInstructions", request.developerInstructions);
        if (!request.modelId.isEmpty()) params.put("model", request.modelId);
        if (!request.effortId.isEmpty()) params.put("effort", request.effortId);
        return params;
    }

    private void acceptThread(JSONObject result) {
        ActiveTurn active = mActiveTurn;
        JSONObject thread = result == null ? null : result.optJSONObject("thread");
        String threadId = thread == null ? "" : thread.optString("id", "");
        if (active == null || threadId.isEmpty()) {
            failActive("Codex returned an invalid chat session.");
            return;
        }
        active.threadId = threadId;
        active.listener.onThreadReady(threadId);
        startTurn();
    }

    private void startTurn() {
        ActiveTurn active = mActiveTurn;
        if (active == null) return;
        active.listener.onStatus(active.narrator.started("Planning next action"));
        try {
            JSONObject input = new JSONObject()
                .put("type", "text")
                .put("text", active.request.prompt);
            JSONObject sandboxPolicy = new JSONObject().put("type", "dangerFullAccess");
            JSONObject params = new JSONObject()
                .put("threadId", active.threadId)
                .put("input", new JSONArray().put(input))
                .put("cwd", active.request.guestWorkingDirectory)
                .put("approvalPolicy", "never")
                .put("sandboxPolicy", sandboxPolicy);
            if (!active.request.modelId.isEmpty()) params.put("model", active.request.modelId);
            if (!active.request.effortId.isEmpty()) params.put("effort", active.request.effortId);
            sendRequest("turn/start", params, new ResponseHandler() {
                @Override
                public void onResult(JSONObject result) {
                    ActiveTurn current = mActiveTurn;
                    JSONObject turn = result == null ? null : result.optJSONObject("turn");
                    if (current != null && turn != null)
                        current.turnId = turn.optString("id", current.turnId);
                }

                @Override
                public void onError(String message) {
                    failActive("Codex could not start this request: " + message);
                }
            });
        } catch (JSONException e) {
            failActive("Codex could not start this request.");
        }
    }

    private void handleMessage(JSONObject message) {
        if (mActiveTurn != null) mActiveTurn.lastSignalAt = SystemClock.elapsedRealtime();
        if (message.has("id") && (message.has("result") || message.has("error"))) {
            int id = message.optInt("id", -1);
            ResponseHandler handler = mPendingRequests.remove(id);
            if (handler == null) return;
            JSONObject error = message.optJSONObject("error");
            if (error != null) handler.onError(protocolError(error));
            else handler.onResult(message.optJSONObject("result"));
            return;
        }

        String method = message.optString("method", "");
        JSONObject params = message.optJSONObject("params");
        if (params == null) params = new JSONObject();
        switch (method) {
            case "item/started":
                handleItemStarted(params);
                break;
            case "item/agentMessage/delta":
                handleAgentMessageDelta(params);
                break;
            case "item/completed":
                handleItemCompleted(params);
                break;
            case "thread/tokenUsage/updated":
                handleTokenUsage(params);
                break;
            case "turn/completed":
                handleTurnCompleted(params);
                break;
            default:
                if (message.has("id")) rejectServerRequest(message, method);
                break;
        }
    }

    private void handleItemStarted(JSONObject params) {
        ActiveTurn active = activeFor(params);
        JSONObject item = params.optJSONObject("item");
        if (active == null || item == null) return;
        String itemId = item.optString("id", "");
        if (active.trace.itemStarted(itemId, item.optString("type", "")))
            active.listener.onTraceChanged(active.trace.snapshot());
        String phase = item.optString("phase", "");
        if (!itemId.isEmpty() && !phase.isEmpty()) active.itemPhases.put(itemId, phase);
        if ("agentMessage".equals(item.optString("type")) && "final_answer".equals(phase))
            active.response.setLength(0);
        String status = describeItem(item);
        String engineeringStatus = OminalEngineeringTrace.activeLabel(item, mSessionId, status);
        emitItemEvent(active, item, "started", status);
        if (!engineeringStatus.isEmpty())
            active.listener.onStatus(active.narrator.started(engineeringStatus));
    }

    private void handleAgentMessageDelta(JSONObject params) {
        ActiveTurn active = activeFor(params);
        if (active == null) return;
        String itemId = params.optString("itemId", "");
        String phase = active.itemPhases.get(itemId);
        if ("commentary".equals(phase)) return;
        String delta = params.optString("delta", "");
        if (delta.isEmpty()) return;
        active.response.append(delta);
        active.listener.onMessageChanged(active.response.toString());
    }

    private void handleItemCompleted(JSONObject params) {
        ActiveTurn active = activeFor(params);
        JSONObject item = params.optJSONObject("item");
        if (active == null || item == null) return;
        if (active.trace.itemCompleted(item.optString("id", ""), item.optString("type", "")))
            active.listener.onTraceChanged(active.trace.snapshot());
        String status = describeItem(item);
        String engineeringStatus = OminalEngineeringTrace.activeLabel(item, mSessionId, status);
        emitItemEvent(active, item, "completed", status);
        if (!engineeringStatus.isEmpty() && !"agentMessage".equals(item.optString("type")))
            active.listener.onStatus(active.narrator.completed(engineeringStatus));
        if (!"agentMessage".equals(item.optString("type"))) return;
        String text = item.optString("text", "");
        if (text.isEmpty()) return;
        active.lastAgentMessage = text;
        String phase = item.optString("phase", "");
        if (!"commentary".equals(phase)) {
            active.response.setLength(0);
            active.response.append(text);
            active.listener.onMessageChanged(text);
        }
    }

    private void handleTokenUsage(JSONObject params) {
        ActiveTurn active = activeFor(params);
        TokenUsage usage = TokenUsage.fromCodexNotification(params);
        if (active == null || usage == null) return;
        active.tokenUsage = usage;
        active.listener.onTokenUsage(usage);
    }

    private void handleTurnCompleted(JSONObject params) {
        ActiveTurn active = activeFor(params);
        JSONObject turn = params.optJSONObject("turn");
        if (active == null || turn == null) return;
        String status = turn.optString("status", "completed");
        if ("failed".equals(status)) {
            JSONObject error = turn.optJSONObject("error");
            failActive(error == null ? "Codex could not complete this request."
                : error.optString("message", "Codex could not complete this request."));
            return;
        }
        if ("interrupted".equals(status)) {
            failActive("The Codex request was interrupted.");
            return;
        }
        String response = active.response.toString().trim();
        if (response.isEmpty()) response = active.lastAgentMessage.trim();
        if (response.isEmpty()) response = "Done.";
        if (active.trace.completeAll()) active.listener.onTraceChanged(active.trace.snapshot());
        Listener listener = active.listener;
        TokenUsage usage = active.tokenUsage;
        mActiveTurn = null;
        listener.onComplete(response, usage);
    }

    private ActiveTurn activeFor(JSONObject params) {
        ActiveTurn active = mActiveTurn;
        if (active == null) return null;
        String threadId = params.optString("threadId", "");
        if (!threadId.isEmpty() && !active.threadId.isEmpty() && !threadId.equals(active.threadId))
            return null;
        String turnId = params.optString("turnId", "");
        if (!turnId.isEmpty()) {
            if (!active.turnId.isEmpty() && !turnId.equals(active.turnId)) return null;
            if (active.turnId.isEmpty()) active.turnId = turnId;
        }
        return active;
    }

    private void sendRequest(String method, JSONObject params, ResponseHandler handler) {
        int id = mNextRequestId++;
        try {
            JSONObject request = new JSONObject()
                .put("method", method)
                .put("id", id)
                .put("params", params);
            mPendingRequests.put(id, handler);
            if (!writeLine(request)) {
                mPendingRequests.remove(id);
                handler.onError("app-server connection closed");
            }
        } catch (JSONException e) {
            handler.onError("invalid app-server request");
        }
    }

    private void sendNotification(String method, JSONObject params) {
        try {
            writeLine(new JSONObject().put("method", method).put("params", params));
        } catch (JSONException e) {
            failActive("Codex protocol error.");
        }
    }

    private void rejectServerRequest(JSONObject request, String method) {
        try {
            JSONObject error = new JSONObject()
                .put("code", -32601)
                .put("message", "Unsupported server request: " + method);
            JSONObject response = new JSONObject()
                .put("id", request.opt("id"))
                .put("error", error);
            writeLine(response);
        } catch (JSONException ignored) {
        }
    }

    private boolean writeLine(JSONObject object) {
        AppShell shell = mShell;
        return shell != null && shell.writeStdinLine(object.toString());
    }

    private void failActive(String message) {
        ActiveTurn active = mActiveTurn;
        mActiveTurn = null;
        if (active != null) {
            if (active.trace.completeAll())
                active.listener.onTraceChanged(active.trace.snapshot());
            active.listener.onError(message == null || message.trim().isEmpty()
                ? "Codex could not complete this request." : message.trim());
        }
    }

    private void failAll(String message) {
        failCapability(message);
        failActive(message);
    }

    private synchronized void finishCapability() {
        mMainHandler.removeCallbacks(mCapabilityTimeout);
        mCapabilityRequestInFlight = false;
        CapabilityListener listener = mCapabilityListener;
        mCapabilityListener = null;
        if (listener != null) listener.onReady();
    }

    private synchronized void failCapability(String message) {
        mMainHandler.removeCallbacks(mCapabilityTimeout);
        mCapabilityRequestInFlight = false;
        CapabilityListener listener = mCapabilityListener;
        mCapabilityListener = null;
        if (listener != null) listener.onError(message == null ? "Codex capability check failed."
            : message);
    }

    static String protocolError(JSONObject error) {
        if (error == null) return "unknown app-server error";
        String message = error.optString("message", "").trim();
        return message.isEmpty() ? "unknown app-server error" : message;
    }

    static boolean isAuthenticationError(String message) {
        if (message == null) return true;
        String normalized = message.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("not authenticated")
            || normalized.contains("not signed in")
            || normalized.contains("login required")
            || normalized.contains("sign in once")
            || normalized.contains("sign in again")
            || normalized.contains("auth missing")
            || normalized.contains("no auth")
            || normalized.contains("unauthenticated")
            || normalized.contains("401 unauthorized")
            || normalized.contains("missing bearer")
            || normalized.contains("token_revoked")
            || normalized.contains("invalidated oauth token")
            || normalized.contains("access token could not be refreshed")
            || normalized.contains("logged out or signed in to another account");
    }

    static String describeItem(JSONObject item) {
        if (item == null) return "";
        switch (item.optString("type", "")) {
            case "plan": return "Planning";
            case "reasoning": return "Working";
            case "commandExecution": return "Running command";
            case "fileChange": return "Editing files";
            case "mcpToolCall": return "Using connected tool";
            case "dynamicToolCall": return "Using tool";
            case "collabAgentToolCall": return "Coordinating agents";
            case "webSearch": return "Searching the web";
            case "imageView": return "Inspecting image";
            case "imageGeneration": return "Creating image";
            case "contextCompaction": return "Compacting context";
            case "agentMessage": return "Writing response";
            default: return "";
        }
    }

    private static void emitItemEvent(@NonNull ActiveTurn active,
                                      @NonNull JSONObject item,
                                      @NonNull String state,
                                      @NonNull String summary) {
        String type = item.optString("type", "");
        if ("agentMessage".equals(type) || "userMessage".equals(type)) return;
        try {
            JSONObject detail = new JSONObject()
                .put("item", new JSONObject(item.toString()));
            active.listener.onEvent(
                MonopotEvent.Draft.operation(state, summary, detail));
        } catch (JSONException ignored) {
        }
    }

    private void scheduleHeartbeat(@NonNull ActiveTurn turn) {
        mMainHandler.postDelayed(() -> emitHeartbeat(turn), HEARTBEAT_INTERVAL_MS);
    }

    private synchronized void emitHeartbeat(@NonNull ActiveTurn turn) {
        if (mActiveTurn != turn) return;
        long now = SystemClock.elapsedRealtime();
        if (now - turn.lastSignalAt >= HEARTBEAT_INTERVAL_MS - 250L) {
            long seconds = Math.max(1L, (now - turn.startedAt) / 1000L);
            turn.listener.onStatus(turn.narrator.waiting("Codex", seconds));
        }
        scheduleHeartbeat(turn);
    }
}
