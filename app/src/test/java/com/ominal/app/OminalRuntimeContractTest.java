package com.ominal.app;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class OminalRuntimeContractTest {
    @Test
    public void exposesChatDisplayToolsAndNoSecrets() throws Exception {
        OminalDisplayGeometry geometry = OminalDisplayGeometry.fromBounds(
            1080, 2260, 1080, 2400, 440);
        String json = OminalRuntimeContract.create("chat-1", "Build app", "/root/workspace",
            Arrays.asList("attachments/spec.txt"), geometry, "native-x11-surface",
            false, true, true, "/root/workspace/.ominal/events.jsonl", "thread-1", false);
        JSONObject contract = new JSONObject(json);

        assertEquals(6, contract.getInt("schemaVersion"));
        assertEquals("/root/workspace", contract.getJSONObject("session").getString("workspace"));
        assertEquals("native-x11-surface", contract.getJSONObject("display").getString("renderer"));
        assertEquals("absolute", contract.getJSONObject("display")
            .getJSONObject("input").getString("touch"));
        assertFalse(contract.getJSONObject("display")
            .getJSONObject("input").getBoolean("pointerVisible"));
        assertEquals("ready_idle", contract.getJSONObject("display").getString("state"));
        assertTrue(contract.getJSONObject("display").getBoolean("availableForAgent"));
        assertEquals("ominal-screen status",
            contract.getJSONObject("display").getString("readinessCommand"));
        assertTrue(contract.getJSONObject("agent").getBoolean("authenticated"));
        assertEquals("openai", contract.getJSONObject("agent").getString("provider"));
        assertEquals("codex", contract.getJSONObject("agent").getString("harness"));
        assertEquals("ominal-proot", contract.getJSONObject("agent").getString("sandboxBoundary"));
        assertEquals("danger-full-access", contract.getJSONObject("agent").getString("sandbox"));
        assertEquals("app-server", contract.getJSONObject("agent").getString("transport"));
        assertEquals("thread-1", contract.getJSONObject("agent").getString("threadId"));
        assertFalse(contract.getJSONObject("android").getBoolean("enabled"));
        assertFalse(contract.getJSONObject("permissions").getBoolean("androidBridge"));
        assertTrue(contract.getJSONObject("permissions").getBoolean("uiAppearanceControl"));
        assertEquals("GIR", contract.getJSONObject("ui").getString("publicName"));
        assertEquals("theme-list",
            contract.getJSONObject("ui").getString("appearanceControl"));
        assertEquals("dark", contract.getJSONObject("ui")
            .getJSONArray("builtInThemes").getString(0));
        assertEquals("light", contract.getJSONObject("ui")
            .getJSONArray("builtInThemes").getString(1));
        assertTrue(contract.getJSONObject("ui").getBoolean("namedThemes"));
        assertTrue(contract.getJSONObject("ui")
            .getBoolean("namedThemesVisibleInAppearance"));
        assertTrue(contract.getJSONObject("ui")
            .getBoolean("namedThemesRequireExplicitUserRequest"));
        assertTrue(contract.getJSONObject("ui").getBoolean("semanticControlsImmutable"));
        assertEquals("attachments/spec.txt", contract.getJSONArray("attachments").getString(0));
        assertFalse(json.toLowerCase().contains("api_key"));
        assertFalse(json.toLowerCase().contains("access_token"));
    }

    @Test
    public void exposesLoloModeOnlyWhenExplicitlyEnabled() throws Exception {
        OminalDisplayGeometry geometry = OminalDisplayGeometry.fromBounds(
            1080, 2260, 1080, 2400, 440);
        JSONObject contract = new JSONObject(OminalRuntimeContract.create(
            "chat-2", "Android task", "/root/workspace", Arrays.asList(), geometry,
            "native-x11-surface", false, true, true,
            "/root/workspace/.ominal/events.jsonl", "", true));

        assertTrue(contract.getJSONObject("android").getBoolean("enabled"));
        assertTrue(contract.getJSONObject("android").getBoolean("appUidOnly"));
        assertTrue(contract.getJSONObject("android").getJSONObject("capabilities")
            .getBoolean("openAppsAndLinks"));
        assertFalse(contract.getJSONObject("android").getJSONObject("capabilities")
            .getBoolean("globalTouch"));
        assertTrue(contract.getJSONObject("permissions").getBoolean("androidBridge"));
        boolean deviceToolAvailable = false;
        for (int i = 0; i < contract.getJSONArray("tools").length(); i++) {
            JSONObject tool = contract.getJSONArray("tools").getJSONObject(i);
            if ("ominal-device".equals(tool.getString("name")))
                deviceToolAvailable = tool.getBoolean("available");
        }
        assertTrue(deviceToolAvailable);
    }
}
