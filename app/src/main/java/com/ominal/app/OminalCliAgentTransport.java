package com.ominal.app;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Hidden stream-json bridge for harnesses that expose a non-interactive CLI. */
public final class OminalCliAgentTransport implements OminalAgentTransport {
    private static final String LOG_TAG = "OminalCliAgentTransport";
    private static final String BRIDGE =
        OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH + "/ominal-harness-chat";
    private static final Pattern ANSI_ESCAPE =
        Pattern.compile("\\u001B(?:\\[[0-?]*[ -/]*[@-~]|\\][^\\u0007]*(?:\\u0007|\\u001B\\\\))");

    private static final class ActiveTurn {
        final TurnRequest request;
        final Listener listener;
        final File promptFile;
        final File instructionsFile;
        final StringBuilder stderr = new StringBuilder();
        final StringBuilder stdout = new StringBuilder();
        final OminalAgentTrace trace = new OminalAgentTrace();
        final HashMap<String, TokenUsage> usageByStep = new HashMap<>();
        String threadId = "";
        String response = "";
        TokenUsage usage;
        boolean terminalEventReceived;

        ActiveTurn(TurnRequest request, Listener listener, File promptFile, File instructionsFile) {
            this.request = request;
            this.listener = listener;
            this.promptFile = promptFile;
            this.instructionsFile = instructionsFile;
        }
    }

    private final Context mContext;
    private final String mHostChatRoot;
    private final String mHarnessId;
    private AppShell mShell;
    private ActiveTurn mActiveTurn;
    private int mGeneration;

    public OminalCliAgentTransport(@NonNull Context context, @NonNull String hostChatRoot,
                                   @NonNull String harnessId) {
        OminalHarnessManifest manifest = OminalHarnessManifest.load(harnessId);
        boolean runtimeAdapter = manifest != null && !manifest.adapterCommand.isEmpty();
        if (!OminalHarnessTerminal.CLAUDE_CODE_ID.equals(harnessId)
            && !OminalHarnessTerminal.ANTIGRAVITY_ID.equals(harnessId)
            && !runtimeAdapter) {
            throw new IllegalArgumentException("Unsupported CLI harness: " + harnessId);
        }
        mContext = context.getApplicationContext();
        mHostChatRoot = hostChatRoot;
        mHarnessId = harnessId;
    }

    @NonNull
    @Override
    public String harnessId() {
        return mHarnessId;
    }

    @NonNull
    @Override
    public String transportId() {
        OminalHarnessManifest manifest = OminalHarnessManifest.load(mHarnessId);
        return manifest == null || manifest.transportId.isEmpty()
            ? "cli-stdio" : manifest.transportId;
    }

    @Override
    public synchronized boolean submit(@NonNull TurnRequest request,
                                       @NonNull Listener listener) {
        if (mActiveTurn != null) return false;
        if (!mHarnessId.equals(request.harnessId)) {
            listener.onError("The selected harness changed before the request started.");
            return false;
        }

        try {
            File requestDirectory = new File(mHostChatRoot, ".agent-requests");
            if (!requestDirectory.isDirectory() && !requestDirectory.mkdirs())
                throw new IOException("Could not create request directory");
            String requestId = UUID.randomUUID().toString();
            File promptFile = new File(requestDirectory, requestId + ".prompt");
            File instructionsFile = new File(requestDirectory, requestId + ".instructions");
            writeFile(promptFile, request.prompt);
            writeFile(instructionsFile, request.developerInstructions);
            mActiveTurn = new ActiveTurn(request, listener, promptFile, instructionsFile);
        } catch (IOException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Could not stage harness request", e);
            listener.onError("Monolith could not prepare that request.");
            return false;
        }

        listener.onStatus("Starting " + harnessName(mHarnessId));
        String guestRequestRoot = "/root/workspace/.agent-requests/";
        String requestBase = stripExtension(mActiveTurn.promptFile.getName());
        ExecutionCommand command = new ExecutionCommand(-1, BRIDGE,
            new String[]{
                mHarnessId,
                "run",
                request.guestWorkingDirectory,
                request.savedThreadId,
                guestRequestRoot + requestBase + ".prompt",
                guestRequestRoot + requestBase + ".instructions",
                request.modelId,
                request.effortId
            }, null, mHostChatRoot, ExecutionCommand.Runner.APP_SHELL.getName(), false);
        command.commandLabel = harnessName(mHarnessId) + " chat";
        start(command, request.environment);
        return mShell != null;
    }

    private void start(ExecutionCommand command, HashMap<String, String> environment) {
        final int generation = ++mGeneration;
        mShell = AppShell.execute(mContext, command,
            exited -> onProcessExited(generation, exited),
            new OminalShellEnvironment(), environment,
            line -> onStdoutLine(generation, line),
            line -> onStderrLine(generation, line), false);
        if (mShell == null) failTurn("Could not start " + harnessName(mHarnessId) + ".");
    }

    private synchronized void onStdoutLine(int generation, String line) {
        if (generation != mGeneration || line == null) return;
        ActiveTurn turn = mActiveTurn;
        if (turn == null || line.trim().isEmpty()) return;
        try {
            consumeEvent(turn, new JSONObject(line));
        } catch (JSONException e) {
            if (turn.stdout.length() > 0) turn.stdout.append('\n');
            turn.stdout.append(line);
        }
    }

    private synchronized void onStderrLine(int generation, String line) {
        if (generation != mGeneration || line == null || line.trim().isEmpty()) return;
        if (mActiveTurn != null) mActiveTurn.stderr.append(line).append('\n');
    }

    private synchronized void onProcessExited(int generation, AppShell shell) {
        if (generation != mGeneration) return;
        mShell = null;
        Integer exitCode = shell.getExecutionCommand().resultData.exitCode;
        boolean succeeded = exitCode != null && exitCode == 0;
        ActiveTurn turn = mActiveTurn;
        if (turn == null) return;
        if (turn.terminalEventReceived) {
            clearTurn();
            return;
        }
        String response = firstNonEmpty(turn.response, normalizePlainOutput(turn.stdout.toString()));
        if (succeeded && !response.isEmpty()) {
            turn.listener.onComplete(response, turn.usage);
            clearTurn();
            return;
        }
        String error = cleanError(firstNonEmpty(turn.stderr.toString(), turn.stdout.toString()));
        failTurn(error.isEmpty()
            ? harnessName(mHarnessId) + " stopped before returning a response."
            : error);
    }

    private void consumeEvent(ActiveTurn turn, JSONObject event) {
        if (consumeMonopotEvent(turn, event)) return;
        String type = eventType(event);
        JSONObject body = eventBody(event, type);
        JSONObject conversation = event.optJSONObject("conversation");
        String threadId = firstNonEmpty(stringField(event, "session_id"),
            stringField(event, "conversation_id"), stringField(event, "conversationId"),
            stringField(body, "session_id"), stringField(body, "conversation_id"),
            stringField(body, "conversationId"),
            conversation == null ? "" : firstNonEmpty(stringField(conversation, "id"),
                stringField(conversation, "conversation_id")));
        if (!threadId.isEmpty() && !threadId.equals(turn.threadId)) {
            turn.threadId = threadId;
            turn.listener.onThreadReady(threadId);
        }

        TokenUsage usage = parseUsage(event);
        if (usage != null) {
            if ("step_update".equals(type) && body.has("step_index")) {
                turn.usageByStep.put(Integer.toString(body.optInt("step_index")), usage);
                usage = aggregateUsage(turn.usageByStep);
            }
            turn.usage = usage;
            turn.listener.onTokenUsage(usage);
        }

        String subtype = firstNonEmpty(stringField(event, "subtype"),
            stringField(body, "subtype")).toLowerCase(Locale.ROOT);
        String stepType = firstNonEmpty(stringField(event, "step_type"),
            stringField(body, "step_type")).toLowerCase(Locale.ROOT);
        updateTrace(turn, type, stepType, body);
        if ("init".equals(type) || "system".equals(type) && "init".equals(subtype)) {
            turn.listener.onStatus("Connected");
            return;
        }

        boolean visibleMessageEvent = !"step_update".equals(type)
            || stepType.contains("response")
            || stepType.contains("message")
            || stepType.contains("assistant")
            || stepType.contains("final");
        String message = visibleMessageEvent ? extractMessage(event) : "";
        if (!message.isEmpty()) {
            if (body.has("text_delta")) turn.response += body.optString("text_delta", "");
            else turn.response = message;
            turn.listener.onMessageChanged(turn.response.trim());
        }

        boolean isResult = "result".equals(type)
            || event.has("response")
            || event.has("is_error")
            || event.has("conversation_id") && event.has("status");
        if (!isResult) {
            String status = statusForEvent(event);
            if (!status.isEmpty()) turn.listener.onStatus(status);
            return;
        }

        boolean failed = event.optBoolean("is_error", false)
            || body.optBoolean("is_error", false)
            || "error".equalsIgnoreCase(firstNonEmpty(
                event.optString("status", ""), body.optString("status", "")))
            || "failed".equalsIgnoreCase(firstNonEmpty(
                event.optString("status", ""), body.optString("status", "")));
        if (turn.trace.completeAll()) turn.listener.onTraceChanged(turn.trace.snapshot());
        if (failed) {
            String error = firstNonEmpty(stringField(event, "error"),
                stringField(body, "error"), stringField(event, "result"),
                stringField(body, "result"), message);
            turn.terminalEventReceived = true;
            turn.listener.onError(cleanError(error));
        } else {
            turn.terminalEventReceived = true;
            String result = firstNonEmpty(message, stringField(event, "result"),
                stringField(body, "result"), stringField(event, "response"),
                stringField(body, "response"), turn.response);
            turn.listener.onComplete(result, turn.usage);
        }
    }

    private boolean consumeMonopotEvent(ActiveTurn turn, JSONObject event) {
        if (!MonopotEvent.PROTOCOL.equals(event.optString("protocol", ""))) return false;
        String channel = event.optString("channel", "");
        if (!MonopotEvent.isChannel(channel)) return true;
        String state = event.optString("state", "");
        if (MonopotEvent.CHANNEL_RESULT.equals(channel)) {
            if ("completed".equals(state) || "done".equals(state)) state = "complete";
            else if ("failed".equals(state)) state = "error";
            if (!"complete".equals(state) && !"cancelled".equals(state)
                && !"error".equals(state)) state = "complete";
            turn.terminalEventReceived = true;
        }
        turn.listener.onEvent(new MonopotEvent.Draft(channel, state,
            event.optString("summary", ""), event.optJSONObject("detail")));
        return true;
    }

    private static void updateTrace(ActiveTurn turn, String type, String stepType,
                                    JSONObject body) {
        String state = body.optString("state", "").toLowerCase(Locale.ROOT);
        String toolName = firstNonEmpty(stringField(body, "tool_name"),
            stringField(body.optJSONObject("tool_info"), "name"));
        String activityType = activityType(toolName, stepType);
        if (activityType.isEmpty() && ("assistant".equals(type)
            || stepType.contains("agent_response"))) {
            activityType = "agentMessage";
        }
        if (activityType.isEmpty()) return;
        String itemId = firstNonEmpty(stringField(body, "id"),
            stringField(body, "step_id"),
            body.has("step_index") ? "step-" + body.optInt("step_index") : "");
        boolean changed;
        boolean completed = "done".equals(state) || "complete".equals(state)
            || "completed".equals(state) || "failed".equals(state);
        if (completed) {
            changed = turn.trace.itemCompleted(itemId, activityType);
        } else {
            changed = turn.trace.itemStarted(itemId, activityType);
        }
        if (!changed) return;
        turn.listener.onTraceChanged(turn.trace.snapshot());
        if (toolName.isEmpty()) return;
        try {
            JSONObject detail = new JSONObject()
                .put("type", type)
                .put("stepType", stepType)
                .put("tool", toolName)
                .put("payload", new JSONObject(body.toString()));
            String eventState = "failed".equals(state) ? "failed"
                : completed ? "completed" : "started";
            turn.listener.onEvent(MonopotEvent.Draft.operation(eventState,
                OminalAgentTrace.labelForType(activityType), detail));
        } catch (JSONException ignored) {
        }
    }

    static String activityType(String toolName, String stepType) {
        String tool = toolName == null ? "" : toolName.toLowerCase(Locale.ROOT);
        if (tool.startsWith("browser_get_") || tool.startsWith("browser_list_")
            || tool.startsWith("read_browser") || tool.contains("screenshot")
            || tool.contains("console_log")) return "browserInspect";
        if (tool.startsWith("open_browser") || tool.startsWith("browser_refresh"))
            return "browserNavigate";
        if (tool.startsWith("browser_") || tool.startsWith("click_browser")
            || tool.startsWith("execute_browser")) return "browserInteract";
        if (tool.equals("list_dir") || tool.equals("view_file")
            || tool.equals("find_by_name") || tool.equals("grep_search")
            || tool.equals("code_search") || tool.equals("read_resource")
            || tool.equals("read_url_content")) return "workspaceRead";
        if (tool.contains("write") || tool.contains("replace")
            || tool.contains("edit") || tool.equals("delete_knowledge"))
            return "fileChange";
        if (tool.equals("run_command") || tool.equals("send_command_input")
            || tool.equals("command_status")) return "commandExecution";
        if (tool.equals("search_web") || tool.equals("web_search")) return "webSearch";
        if (tool.equals("view_image")) return "imageView";
        if (tool.equals("generate_image")) return "imageGeneration";
        if (tool.equals("ask_question") || tool.equals("ask_permission"))
            return "question";
        if (!tool.isEmpty()) return "dynamicToolCall";
        return stepType != null && stepType.contains("tool") ? "dynamicToolCall" : "";
    }

    @Nullable
    static TokenUsage parseUsage(JSONObject event) {
        JSONObject usage = event.optJSONObject("usage");
        String type = eventType(event);
        JSONObject body = eventBody(event, type);
        if (usage == null) usage = body.optJSONObject("usage");
        if (usage == null) {
            JSONObject result = event.optJSONObject("result");
            usage = result == null ? null : result.optJSONObject("usage");
        }
        if (usage == null) {
            JSONObject message = event.optJSONObject("message");
            usage = message == null ? null : message.optJSONObject("usage");
        }
        if (usage == null) return null;
        long input = firstLong(usage, "input_tokens", "inputTokens");
        long cached = firstLong(usage, "cache_read_input_tokens", "cache_read_tokens",
            "cachedInputTokens");
        long output = firstLong(usage, "output_tokens", "outputTokens");
        long hidden = firstLong(usage, "thinking_tokens", "reasoning_output_tokens",
            "reasoningOutputTokens");
        long total = firstLong(usage, "total_tokens", "totalTokens");
        if (total <= 0) total = input + output;
        return new TokenUsage(input, cached, output, hidden, total);
    }

    private static long firstLong(JSONObject object, String... keys) {
        for (String key : keys) {
            if (object.has(key)) return object.optLong(key);
        }
        return 0L;
    }

    private static TokenUsage aggregateUsage(HashMap<String, TokenUsage> usageByStep) {
        long input = 0;
        long cached = 0;
        long output = 0;
        long reasoning = 0;
        long total = 0;
        for (TokenUsage usage : usageByStep.values()) {
            input += usage.inputTokens;
            cached += usage.cachedInputTokens;
            output += usage.outputTokens;
            reasoning += usage.reasoningOutputTokens;
            total += usage.totalTokens;
        }
        return new TokenUsage(input, cached, output, reasoning, total);
    }

    static String extractMessage(JSONObject event) {
        String type = eventType(event);
        JSONObject body = eventBody(event, type);
        String direct = firstNonEmpty(stringField(event, "response"),
            stringField(event, "result"), stringField(event, "text"),
            stringField(event, "output"), stringField(body, "response"),
            stringField(body, "result"), stringField(body, "text"),
            stringField(body, "text_delta"), stringField(body, "output"));
        if (!direct.isEmpty()) return direct;
        String result = textFromContent(event.opt("result"));
        if (!result.isEmpty()) return result;
        JSONObject message = event.optJSONObject("message");
        if (message != null) {
            String messageText = firstNonEmpty(message.optString("text", ""),
                message.optString("response", ""));
            if (!messageText.isEmpty()) return messageText;
            String text = textFromContent(message.opt("content"));
            if (!text.isEmpty()) return text;
        }
        String bodyContent = textFromContent(body.opt("content"));
        return bodyContent.isEmpty() ? textFromContent(event.opt("content")) : bodyContent;
    }

    private static String textFromContent(Object content) {
        if (content instanceof String) return ((String) content).trim();
        if (content instanceof JSONObject) {
            JSONObject object = (JSONObject) content;
            String direct = firstNonEmpty(stringField(object, "text"),
                stringField(object, "response"), stringField(object, "output"));
            if (!direct.isEmpty()) return direct;
            return textFromContent(object.opt("content"));
        }
        if (!(content instanceof JSONArray)) return "";
        StringBuilder text = new StringBuilder();
        JSONArray blocks = (JSONArray) content;
        for (int index = 0; index < blocks.length(); index++) {
            JSONObject block = blocks.optJSONObject(index);
            if (block == null || !"text".equals(block.optString("type", "text"))) continue;
            String value = block.optString("text", "").trim();
            if (value.isEmpty()) continue;
            if (text.length() > 0) text.append("\n\n");
            text.append(value);
        }
        return text.toString();
    }

    static String statusForEvent(JSONObject event) {
        String type = eventType(event);
        JSONObject body = eventBody(event, type);
        String stepType = firstNonEmpty(stringField(event, "step_type"),
            stringField(body, "step_type")).toLowerCase(Locale.ROOT);
        if ("ominal_setup".equals(type))
            return firstNonEmpty(event.optString("status", ""), "Setting up harness");
        if ("assistant".equals(type)) return "Responding";
        if (type.contains("tool") || stepType.contains("tool")
            || event.optJSONObject("tool_info") != null
            || body.optJSONObject("tool_info") != null)
            return OminalAgentTrace.labelForType(activityType(
                firstNonEmpty(stringField(body, "tool_name"),
                    stringField(body.optJSONObject("tool_info"), "name")), stepType));
        if ("step_update".equals(type)) {
            if (stepType.contains("response") || stepType.contains("message"))
                return "Responding";
            return "Working";
        }
        return "";
    }

    private static String eventType(JSONObject event) {
        return firstNonEmpty(event.optString("type", ""),
            event.optString("event", "")).toLowerCase(Locale.ROOT);
    }

    private static JSONObject eventBody(JSONObject event, String type) {
        JSONObject body = type.isEmpty() ? null : event.optJSONObject(type);
        return body == null ? event : body;
    }

    static String normalizePlainOutput(String value) {
        return stripAnsi(value == null ? "" : value).trim();
    }

    private synchronized void failTurn(String message) {
        ActiveTurn turn = mActiveTurn;
        if (turn == null) return;
        turn.listener.onError(cleanError(message));
        clearTurn();
    }

    private synchronized void clearTurn() {
        ActiveTurn turn = mActiveTurn;
        mActiveTurn = null;
        if (turn == null) return;
        deleteQuietly(turn.promptFile);
        deleteQuietly(turn.instructionsFile);
    }

    @Override
    public synchronized boolean cancel() {
        if (mActiveTurn == null) return false;
        mGeneration++;
        AppShell shell = mShell;
        mShell = null;
        if (shell != null) shell.kill();
        clearTurn();
        return true;
    }

    @Override
    public synchronized void shutdown() {
        mGeneration++;
        AppShell shell = mShell;
        mShell = null;
        if (shell != null) shell.kill();
        clearTurn();
    }

    private static String cleanError(String value) {
        String cleaned = stripAnsi(value == null ? "" : value).trim();
        if (cleaned.isEmpty()) return "";
        String lower = cleaned.toLowerCase(Locale.ROOT);
        if (lower.contains("not logged in") || lower.contains("not authenticated")
            || lower.contains("please sign in") || lower.contains("authentication")) {
            return "Open the selected harness with /login first.";
        }
        return cleaned.length() > 500 ? cleaned.substring(0, 500).trim() : cleaned;
    }

    private static String harnessName(String harnessId) {
        OminalAgentHarness harness = OminalHarnessRegistry.find(harnessId);
        return harness == null ? harnessId
            : OminalHarnessRegistry.resolvedDisplayName(harness);
    }

    private static String stripAnsi(String value) {
        return ANSI_ESCAPE.matcher(value).replaceAll("");
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private static String stringField(JSONObject object, String key) {
        Object value = object == null ? null : object.opt(key);
        return value instanceof String ? ((String) value).trim() : "";
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    private static void writeFile(File file, String value) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(value.getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
    }

    private static void deleteQuietly(File file) {
        if (file != null && file.exists() && !file.delete())
            Logger.logWarn(LOG_TAG, "Could not delete staged harness request");
    }
}
