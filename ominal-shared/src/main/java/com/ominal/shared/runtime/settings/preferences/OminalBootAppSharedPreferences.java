package com.ominal.shared.runtime.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominal.shared.logger.Logger;
import com.ominal.shared.android.PackageUtils;
import com.ominal.shared.settings.preferences.AppSharedPreferences;
import com.ominal.shared.settings.preferences.SharedPreferenceUtils;
import com.ominal.shared.runtime.OminalUtils;
import com.ominal.shared.runtime.settings.preferences.OminalPreferenceConstants.OMINAL_BOOT_APP;
import com.ominal.shared.runtime.OminalConstants;

public class OminalBootAppSharedPreferences extends AppSharedPreferences {

    private static final String LOG_TAG = "OminalBootAppSharedPreferences";

    private OminalBootAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                OminalConstants.OMINAL_BOOT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                OminalConstants.OMINAL_BOOT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));
    }

    /**
     * Get {@link OminalBootAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link OminalConstants#OMINAL_BOOT_PACKAGE_NAME}.
     * @return Returns the {@link OminalBootAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static OminalBootAppSharedPreferences build(@NonNull final Context context) {
        Context ominalBootPackageContext = PackageUtils.getContextForPackage(context, OminalConstants.OMINAL_BOOT_PACKAGE_NAME);
        if (ominalBootPackageContext == null)
            return null;
        else
            return new OminalBootAppSharedPreferences(ominalBootPackageContext);
    }

    /**
     * Get {@link OminalBootAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link OminalConstants#OMINAL_BOOT_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link OminalBootAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static OminalBootAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context ominalBootPackageContext = OminalUtils.getContextForPackageOrExitApp(context, OminalConstants.OMINAL_BOOT_PACKAGE_NAME, exitAppOnError);
        if (ominalBootPackageContext == null)
            return null;
        else
            return new OminalBootAppSharedPreferences(ominalBootPackageContext);
    }



    public int getLogLevel(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, OMINAL_BOOT_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        else
            return SharedPreferenceUtils.getInt(mSharedPreferences, OMINAL_BOOT_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel, boolean commitToFile) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, OMINAL_BOOT_APP.KEY_LOG_LEVEL, logLevel, commitToFile);
    }

}
