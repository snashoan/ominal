package com.ominal.shared.runtime.settings.properties;

import android.content.Context;

import androidx.annotation.NonNull;

import com.ominal.shared.runtime.OminalConstants;

public class OminalAppSharedProperties extends OminalSharedProperties {

    private static OminalAppSharedProperties properties;


    private OminalAppSharedProperties(@NonNull Context context) {
        super(context, OminalConstants.OMINAL_APP_NAME,
            OminalConstants.OMINAL_PROPERTIES_FILE_PATHS_LIST, OminalPropertyConstants.OMINAL_APP_PROPERTIES_LIST,
            new OminalSharedProperties.SharedPropertiesParserClient());
    }

    /**
     * Initialize the {@link #properties} and load properties from disk.
     *
     * @param context The {@link Context} for operations.
     * @return Returns the {@link OminalAppSharedProperties}.
     */
    public static OminalAppSharedProperties init(@NonNull Context context) {
        if (properties == null)
            properties = new OminalAppSharedProperties(context);

        return properties;
    }

    /**
     * Get the {@link #properties}.
     *
     * @return Returns the {@link OminalAppSharedProperties}.
     */
    public static OminalAppSharedProperties getProperties() {
        return properties;
    }

}
