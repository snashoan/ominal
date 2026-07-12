package com.ominal.app;

import android.app.Application;
import android.content.Context;

import com.ominal.BuildConfig;
import com.ominal.shared.errors.Error;
import com.ominal.shared.logger.Logger;
import com.ominal.shared.runtime.OminalBootstrap;
import com.ominal.shared.runtime.OminalConstants;
import com.ominal.shared.runtime.crash.OminalCrashUtils;
import com.ominal.shared.runtime.file.OminalFileUtils;
import com.ominal.shared.runtime.settings.preferences.OminalAppSharedPreferences;
import com.ominal.shared.runtime.settings.properties.OminalAppSharedProperties;
import com.ominal.shared.runtime.shell.command.environment.OminalShellEnvironment;
import com.ominal.shared.runtime.shell.OminalShellManager;
import com.ominal.shared.runtime.theme.OminalThemeUtils;

public class OminalApplication extends Application {

    private static final String LOG_TAG = "OminalApplication";

    public void onCreate() {
        super.onCreate();

        Context context = getApplicationContext();

        // Set crash handler for the app
        OminalCrashUtils.setDefaultCrashHandler(this);

        // Set log config for the app
        setLogConfig(context);

        Logger.logDebug("Starting Application");

        // Set OminalBootstrap.OMINAL_APP_PACKAGE_MANAGER and OminalBootstrap.OMINAL_APP_PACKAGE_VARIANT
        OminalBootstrap.setOminalPackageManagerAndVariant(BuildConfig.OMINAL_PACKAGE_VARIANT);

        // Init app wide SharedProperties loaded from ominal.properties
        OminalAppSharedProperties properties = OminalAppSharedProperties.init(context);

        // Init app wide shell manager
        OminalShellManager shellManager = OminalShellManager.init(context);

        // Set NightMode.APP_NIGHT_MODE
        OminalThemeUtils.setAppNightMode(properties.getNightMode());

        // Check and create ominal files directory. If failed to access it like in case of secondary
        // user or external sd card installation, then don't run files directory related code
        Error error = OminalFileUtils.isOminalFilesDirectoryAccessible(this, true, true);
        boolean isOminalFilesDirectoryAccessible = error == null;
        if (isOminalFilesDirectoryAccessible) {
            Logger.logInfo(LOG_TAG, "Ominal files directory is accessible");

            error = OminalFileUtils.isAppsOminalAppDirectoryAccessible(true, true);
            if (error != null) {
                Logger.logErrorExtended(LOG_TAG, "Create apps/ominal-app directory failed\n" + error);
                return;
            }

        } else {
            Logger.logErrorExtended(LOG_TAG, "Ominal files directory is not accessible\n" + error);
        }

        // Initialize shell environment constants after app storage is ready.
        OminalShellEnvironment.init(this);

        if (isOminalFilesDirectoryAccessible) {
            OminalShellEnvironment.writeEnvironmentToFile(this);
        }
    }

    public static void setLogConfig(Context context) {
        Logger.setDefaultLogTag(OminalConstants.OMINAL_APP_NAME);

        // Load the log level from shared preferences and set it to the {@link Logger.CURRENT_LOG_LEVEL}
        OminalAppSharedPreferences preferences = OminalAppSharedPreferences.build(context);
        if (preferences == null) return;
        preferences.setLogLevel(null, preferences.getLogLevel());
    }

}
