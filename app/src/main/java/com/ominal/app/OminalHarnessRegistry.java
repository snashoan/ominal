package com.ominal.app;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Catalog of intelligence harnesses understood by this app build. */
public final class OminalHarnessRegistry {
    public static final String PREFERENCE_KEY = "agent_harness";
    public static final String DEFAULT_HARNESS_ID = "codex";

    private static final Map<String, OminalAgentHarness> HARNESSES;

    static {
        LinkedHashMap<String, OminalAgentHarness> harnesses = new LinkedHashMap<>();
        register(harnesses, new OminalAgentHarness(
            "codex",
            "openai",
            "OpenAI",
            "Codex",
            "app-server",
            OminalAgentHarness.Availability.AVAILABLE,
            Arrays.asList(
                OminalAgentHarness.AuthMode.BROWSER,
                OminalAgentHarness.AuthMode.DEVICE_CODE,
                OminalAgentHarness.AuthMode.API_KEY)));
        register(harnesses, new OminalAgentHarness(
            "antigravity",
            "google",
            "Google",
            "Antigravity",
            "harness-native",
            OminalAgentHarness.Availability.AVAILABLE,
            Collections.singletonList(OminalAgentHarness.AuthMode.BROWSER)));
        HARNESSES = Collections.unmodifiableMap(harnesses);
    }

    private OminalHarnessRegistry() {}

    public static List<OminalAgentHarness> all() {
        LinkedHashMap<String, OminalAgentHarness> combined = new LinkedHashMap<>(HARNESSES);
        for (OminalHarnessManifest manifest : OminalHarnessManifest.installed()) {
            if (combined.containsKey(manifest.harnessId) || manifest.adapterCommand.isEmpty())
                continue;
            OminalAgentHarness runtime = fromManifest(manifest);
            combined.put(runtime.getId(), runtime);
        }
        return Collections.unmodifiableList(Arrays.asList(
            combined.values().toArray(new OminalAgentHarness[0])));
    }

    public static OminalAgentHarness find(String id) {
        if (id == null) return null;
        OminalAgentHarness builtIn = HARNESSES.get(id);
        if (builtIn != null) return builtIn;
        OminalHarnessManifest manifest = OminalHarnessManifest.load(id);
        return manifest == null || manifest.adapterCommand.isEmpty()
            ? null : fromManifest(manifest);
    }

    public static boolean isSelectable(String id) {
        OminalAgentHarness harness = find(id);
        return harness != null && harness.isAvailable();
    }

    public static String normalizeSelectedId(String requestedId) {
        return isSelectable(requestedId) ? requestedId : DEFAULT_HARNESS_ID;
    }

    public static OminalAgentHarness activeOrDefault(String requestedId) {
        OminalAgentHarness harness = find(normalizeSelectedId(requestedId));
        return harness == null ? HARNESSES.get(DEFAULT_HARNESS_ID) : harness;
    }

    public static String resolvedDisplayName(OminalAgentHarness harness) {
        OminalHarnessManifest manifest = OminalHarnessManifest.load(harness.getId());
        return manifest == null || manifest.displayName.isEmpty()
            ? harness.getDisplayName() : manifest.displayName;
    }

    public static String resolvedPublisherName(OminalAgentHarness harness) {
        OminalHarnessManifest manifest = OminalHarnessManifest.load(harness.getId());
        return manifest == null || manifest.publisher.isEmpty()
            ? harness.getPublisherName() : manifest.publisher;
    }

    private static void register(Map<String, OminalAgentHarness> harnesses,
                                 OminalAgentHarness harness) {
        if (harnesses.put(harness.getId(), harness) != null) {
            throw new IllegalStateException("Duplicate harness id: " + harness.getId());
        }
    }

    private static OminalAgentHarness fromManifest(OminalHarnessManifest manifest) {
        String displayName = manifest.displayName.isEmpty()
            ? manifest.harnessId : manifest.displayName;
        String publisher = manifest.publisher.isEmpty()
            ? "Runtime adapter" : manifest.publisher;
        return new OminalAgentHarness(manifest.harnessId, manifest.providerId, publisher,
            displayName, manifest.transportId, OminalAgentHarness.Availability.AVAILABLE,
            Collections.singletonList(OminalAgentHarness.AuthMode.RUNTIME));
    }
}
