package com.ominal.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/** Builds the non-secret runtime context exposed to the active coding agent. */
public final class OminalRuntimeContract {
    public static final int SCHEMA_VERSION = 5;

    private OminalRuntimeContract() {}

    public static String create(String sessionId, String title, String workspacePath,
                                List<String> attachments, OminalDisplayGeometry display,
                                 String displayRenderer, boolean displayVisible,
                                 boolean displayReady, boolean signedIn,
                                 String eventLogPath, String codexThreadId,
                                 boolean loloModeEnabled) throws JSONException {
        return create(sessionId, title, workspacePath, attachments, display, displayRenderer,
            displayVisible, displayReady, OminalHarnessRegistry.activeOrDefault(null), signedIn,
            eventLogPath, codexThreadId, loloModeEnabled);
    }

    public static String create(String sessionId, String title, String workspacePath,
                                List<String> attachments, OminalDisplayGeometry display,
                                String displayRenderer, boolean displayVisible,
                                boolean displayReady, OminalAgentHarness harness,
                                boolean signedIn, String eventLogPath, String threadId,
                                boolean loloModeEnabled) throws JSONException {
        return create(sessionId, title, workspacePath, attachments, display, displayRenderer,
            displayVisible, displayReady, displayReady ? "ready_idle" : "off", harness,
            signedIn, eventLogPath, threadId, loloModeEnabled);
    }

    public static String create(String sessionId, String title, String workspacePath,
                                List<String> attachments, OminalDisplayGeometry display,
                                String displayRenderer, boolean displayVisible,
                                boolean displayReady, String displayState,
                                OminalAgentHarness harness, boolean signedIn,
                                String eventLogPath, String threadId,
                                boolean loloModeEnabled) throws JSONException {
        if (harness == null) throw new IllegalArgumentException("harness is required");
        JSONObject root = new JSONObject();
        root.put("schemaVersion", SCHEMA_VERSION);

        JSONObject app = new JSONObject();
        app.put("name", "Ominal");
        app.put("package", "com.ominal");
        app.put("frontend", "chat");
        root.put("app", app);

        JSONObject session = new JSONObject();
        session.put("id", sessionId);
        session.put("title", title);
        session.put("workspace", workspacePath);
        root.put("session", session);

        JSONArray attachmentArray = new JSONArray();
        for (String attachment : attachments) attachmentArray.put(attachment);
        root.put("attachments", attachmentArray);

        JSONObject displayObject = new JSONObject();
        displayObject.put("display", ":20");
        displayObject.put("renderer", displayRenderer);
        displayObject.put("visible", displayVisible);
        displayObject.put("ready", displayReady);
        displayObject.put("state", normalizeDisplayState(displayState, displayReady));
        displayObject.put("availableForAgent", displayReady);
        displayObject.put("readinessCommand", "ominal-screen status");
        displayObject.put("widthPixels", display.widthPixels);
        displayObject.put("heightPixels", display.heightPixels);
        displayObject.put("densityDpi", display.densityDpi);
        displayObject.put("orientation", "portrait");
        displayObject.put("input", new JSONObject()
            .put("touch", "absolute")
            .put("keyboard", "android-ime")
            .put("pointerVisible", false));
        root.put("display", displayObject);

        JSONObject agent = new JSONObject();
        agent.put("provider", harness.getProviderId());
        agent.put("harness", harness.getId());
        agent.put("displayName", harness.getDisplayName());
        agent.put("transport", harness.getTransport());
        agent.put("threadId", threadId == null ? "" : threadId);
        agent.put("authenticated", signedIn);
        agent.put("sandbox", "danger-full-access");
        agent.put("sandboxBoundary", "ominal-proot");
        agent.put("approvalPolicy", "never-inside-proot");
        agent.put("eventLog", eventLogPath);
        agent.put("eventSchemaVersion", OminalAgentEventLog.SCHEMA_VERSION);
        root.put("agent", agent);

        JSONArray tools = new JSONArray();
        tools.put(new JSONObject()
            .put("name", "ominal-screen")
            .put("purpose", "Inspect and control the shared Linux display"));
        tools.put(new JSONObject()
            .put("name", "ominal-event")
            .put("purpose", "Send structured UI events to the Ominal app"));
        tools.put(new JSONObject()
            .put("name", "ominal-device")
            .put("purpose", "Open selected Android apps, links, and Settings")
            .put("available", loloModeEnabled));
        root.put("tools", tools);

        root.put("android", new JSONObject()
            .put("mode", "lolo")
            .put("experimental", true)
            .put("enabled", loloModeEnabled)
            .put("appUidOnly", true)
            .put("transport", "app-intent-mediator")
            .put("capabilities", new JSONObject()
                .put("openAppsAndLinks", loloModeEnabled)
                .put("openSettings", loloModeEnabled)
                .put("screenCapture", false)
                .put("globalTouch", false)
                .put("protectedSystemActions", false)));

        root.put("permissions", new JSONObject()
            .put("workspaceReadWrite", true)
            .put("displayControl", true)
            .put("requestUserInput", true)
            .put("androidBridge", loloModeEnabled));
        return root.toString(2);
    }

    private static String normalizeDisplayState(String state, boolean ready) {
        if ("off".equals(state) || "starting".equals(state) || "ready_idle".equals(state)
            || "agent_active".equals(state) || "needs_user".equals(state)
            || "error".equals(state)) {
            return state;
        }
        return ready ? "ready_idle" : "off";
    }
}
