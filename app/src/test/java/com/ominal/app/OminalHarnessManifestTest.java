package com.ominal.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class OminalHarnessManifestTest {
    @Test
    public void parsesValidatedHarnessCapabilities() throws Exception {
        JSONObject manifest = validManifest()
            .put("identity", new JSONObject()
                .put("name", "Antigravity")
                .put("publisher", "Google"))
            .put("models", new JSONArray().put(new JSONObject()
                .put("id", "agy-pro")
                .put("label", "Pro")
                .put("efforts", new JSONArray().put("low").put("high"))))
            .put("commands", new JSONArray()
                .put(new JSONObject().put("name", "/model").put("type", "model"))
                .put(new JSONObject().put("name", "/effort").put("type", "effort")));

        OminalHarnessManifest parsed = OminalHarnessManifest.fromJson(manifest);

        assertEquals("antigravity", parsed.harnessId);
        assertEquals("Antigravity", parsed.displayName);
        assertEquals("Google", parsed.publisher);
        assertEquals("stream-json", parsed.outputFormat);
        assertEquals("--conversation", parsed.resumeFlag);
        assertEquals("--model", parsed.modelFlag);
        assertEquals("--effort", parsed.effortFlag);
        assertTrue(parsed.autonomyEnabledByDefault);
        assertEquals("agy-pro", parsed.models.get(0).id);
        assertEquals("/model", parsed.commandNames().get(0));
    }

    @Test(expected = JSONException.class)
    public void rejectsExecutableTextInFlags() throws Exception {
        JSONObject manifest = validManifest();
        manifest.getJSONObject("autonomy")
            .put("flag", "--dangerously-skip-permissions;rm");
        OminalHarnessManifest.fromJson(manifest);
    }

    @Test(expected = JSONException.class)
    public void rejectsUnrecognizedCommandTypes() throws Exception {
        JSONObject manifest = validManifest().put("commands", new JSONArray()
            .put(new JSONObject().put("name", "/model").put("type", "shell")));
        OminalHarnessManifest.fromJson(manifest);
    }

    @Test(expected = JSONException.class)
    public void rejectsAutonomyWithoutVerifiedFlag() throws Exception {
        JSONObject manifest = validManifest();
        manifest.getJSONObject("autonomy").put("flag", "");
        OminalHarnessManifest.fromJson(manifest);
    }

    @Test(expected = JSONException.class)
    public void rejectsUnsafeRuntimeIdentity() throws Exception {
        JSONObject manifest = validManifest().put("identity", new JSONObject()
            .put("name", "Broken\nRuntime")
            .put("publisher", "Publisher"));
        OminalHarnessManifest.fromJson(manifest);
    }

    @Test
    public void acceptsProviderNeutralMonopotAdapter() throws Exception {
        JSONObject manifest = validManifest();
        manifest.put("harness", "custom-provider");
        manifest.put("identity", new JSONObject()
            .put("name", "Custom provider")
            .put("publisher", "User configured")
            .put("provider", "custom"));
        manifest.put("transport", new JSONObject()
            .put("outputFormat", "monopot-jsonl")
            .put("adapterCommand", "my-monopot-adapter")
            .put("id", "stdio.custom"));

        OminalHarnessManifest parsed = OminalHarnessManifest.fromJson(manifest);

        assertEquals("my-monopot-adapter", parsed.adapterCommand);
        assertEquals("stdio.custom", parsed.transportId);
        assertEquals("custom", parsed.providerId);
    }

    @Test(expected = JSONException.class)
    public void rejectsAdapterCommandWithShellSyntax() throws Exception {
        JSONObject manifest = validManifest();
        manifest.getJSONObject("transport")
            .put("outputFormat", "monopot-jsonl")
            .put("adapterCommand", "adapter --unsafe");
        OminalHarnessManifest.fromJson(manifest);
    }

    private static JSONObject validManifest() throws Exception {
        return new JSONObject()
            .put("schemaVersion", 1)
            .put("harness", "antigravity")
            .put("binaryVersion", "1.1.8")
            .put("transport", new JSONObject()
                .put("outputFormat", "stream-json")
                .put("resumeFlag", "--conversation")
                .put("modelFlag", "--model")
                .put("effortFlag", "--effort"))
            .put("autonomy", new JSONObject()
                .put("flag", "--dangerously-skip-permissions")
                .put("enabledByDefault", true));
    }
}
