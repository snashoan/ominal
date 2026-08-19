package com.ominal.shared.android;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominal.shared.logger.Logger;

import java.util.Collections;
import java.util.Map;

/**
 * Reads explicit feature-flag overrides exposed through the public {@link Settings.Global} API.
 * Android does not expose the platform's complete feature-flag registry through the public SDK, so
 * unset defaults are reported as unsupported instead of inspecting private framework internals.
 */
public class FeatureFlagUtils {

    public enum FeatureFlagValue {

        /** Unknown like due to exception raised while getting value. */
        UNKNOWN("<unknown>"),

        /** Flag is unsupported on current android build. */
        UNSUPPORTED("<unsupported>"),

        /** Flag is enabled. */
        TRUE("true"),

        /** Flag is not enabled. */
        FALSE("false");

        private final String name;

        FeatureFlagValue(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }

    private static final String LOG_TAG = "FeatureFlagUtils";

    /**
     * Get all feature flags in their raw form.
     */
    @NonNull
    public static Map<String, String> getAllFeatureFlags() {
        return Collections.emptyMap();
    }

    /**
     * Check if a feature flag exists.
     *
     * @return Returns {@code true} if flag exists, otherwise {@code false}. This will be
     * {@code null} if an exception is raised.
     */
    @Nullable
    public static Boolean featureFlagExists(@NonNull String feature) {
        return getAllFeatureFlags().containsKey(feature);
    }

    /**
     * Get {@link FeatureFlagValue} for a feature.
     *
     * @param context The {@link Context} for operations.
     * @param feature The {@link String} name for feature.
     * @return Returns {@link FeatureFlagValue}.
     */
    @NonNull
    public static FeatureFlagValue getFeatureFlagValueString(@NonNull Context context, @NonNull String feature) {
        Boolean featureFlagValue = isFeatureEnabled(context, feature);
        if (featureFlagValue == null) {
            return FeatureFlagValue.UNSUPPORTED;
        } else {
            return featureFlagValue ? FeatureFlagValue.TRUE : FeatureFlagValue.FALSE;
        }
    }

    /**
     * Read an explicit public Settings.Global override for a feature flag.
     *
     * @param context The {@link Context} for operations.
     * @param feature The {@link String} name for feature.
     * @return Returns {@code true} if flag exists, otherwise {@code false}. This will be
     * {@code null} if an exception is raised.
     */
    @Nullable
    public static Boolean isFeatureEnabled(@NonNull Context context, @NonNull String feature) {
        try {
            String value = Settings.Global.getString(context.getContentResolver(), feature);
            if ("1".equals(value) || "true".equalsIgnoreCase(value)) return true;
            if ("0".equals(value) || "false".equalsIgnoreCase(value)) return false;
            return null;
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to check if feature flag \"" + feature + "\" is enabled", e);
            return null;
        }
    }

}
