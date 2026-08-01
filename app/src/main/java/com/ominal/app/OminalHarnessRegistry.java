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
            "Codex",
            "app-server",
            OminalAgentHarness.Availability.AVAILABLE,
            Arrays.asList(
                OminalAgentHarness.AuthMode.BROWSER,
                OminalAgentHarness.AuthMode.DEVICE_CODE,
                OminalAgentHarness.AuthMode.API_KEY)));
        register(harnesses, new OminalAgentHarness(
            "claude-code",
            "anthropic",
            "Claude Code",
            "harness-native",
            OminalAgentHarness.Availability.AVAILABLE,
            Arrays.asList(
                OminalAgentHarness.AuthMode.BROWSER,
                OminalAgentHarness.AuthMode.API_KEY)));
        register(harnesses, new OminalAgentHarness(
            "antigravity",
            "google",
            "Antigravity",
            "harness-native",
            OminalAgentHarness.Availability.AVAILABLE,
            Collections.singletonList(OminalAgentHarness.AuthMode.BROWSER)));
        HARNESSES = Collections.unmodifiableMap(harnesses);
    }

    private OminalHarnessRegistry() {}

    public static List<OminalAgentHarness> all() {
        return Collections.unmodifiableList(Arrays.asList(
            HARNESSES.values().toArray(new OminalAgentHarness[0])));
    }

    public static OminalAgentHarness find(String id) {
        return id == null ? null : HARNESSES.get(id);
    }

    public static boolean isSelectable(String id) {
        OminalAgentHarness harness = find(id);
        return harness != null && harness.isAvailable();
    }

    public static String normalizeSelectedId(String requestedId) {
        return isSelectable(requestedId) ? requestedId : DEFAULT_HARNESS_ID;
    }

    public static OminalAgentHarness activeOrDefault(String requestedId) {
        return HARNESSES.get(normalizeSelectedId(requestedId));
    }

    private static void register(Map<String, OminalAgentHarness> harnesses,
                                 OminalAgentHarness harness) {
        if (harnesses.put(harness.getId(), harness) != null) {
            throw new IllegalStateException("Duplicate harness id: " + harness.getId());
        }
    }
}
