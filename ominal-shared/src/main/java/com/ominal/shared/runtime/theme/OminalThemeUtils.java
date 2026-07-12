package com.ominal.shared.runtime.theme;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominal.shared.runtime.settings.properties.OminalPropertyConstants;
import com.ominal.shared.runtime.settings.properties.OminalSharedProperties;
import com.ominal.shared.theme.NightMode;

public class OminalThemeUtils {

    /** Get the {@link OminalPropertyConstants#KEY_NIGHT_MODE} value from the properties file on disk
     * and set it to app wide night mode value. */
    public static void setAppNightMode(@NonNull Context context) {
        NightMode.setAppNightMode(OminalSharedProperties.getNightMode(context));
    }

    /** Set name as app wide night mode value. */
    public static void setAppNightMode(@Nullable String name) {
        NightMode.setAppNightMode(name);
    }

}
