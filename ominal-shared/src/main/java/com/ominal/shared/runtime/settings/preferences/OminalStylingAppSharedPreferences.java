package com.ominal.shared.runtime.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominal.shared.logger.Logger;
import com.ominal.shared.android.PackageUtils;
import com.ominal.shared.settings.preferences.AppSharedPreferences;
import com.ominal.shared.settings.preferences.SharedPreferenceUtils;
import com.ominal.shared.runtime.OminalUtils;
import com.ominal.shared.runtime.settings.preferences.OminalPreferenceConstants.OMINAL_STYLING_APP;
import com.ominal.shared.runtime.OminalConstants;

public class OminalStylingAppSharedPreferences extends AppSharedPreferences {

    private static final String LOG_TAG = "OminalStylingAppSharedPreferences";

    private OminalStylingAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                OminalConstants.OMINAL_STYLING_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                OminalConstants.OMINAL_STYLING_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));
    }

    /**
     * Get {@link OminalStylingAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link OminalConstants#OMINAL_STYLING_PACKAGE_NAME}.
     * @return Returns the {@link OminalStylingAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static OminalStylingAppSharedPreferences build(@NonNull final Context context) {
        Context ominalStylingPackageContext = PackageUtils.getContextForPackage(context, OminalConstants.OMINAL_STYLING_PACKAGE_NAME);
        if (ominalStylingPackageContext == null)
            return null;
        else
            return new OminalStylingAppSharedPreferences(ominalStylingPackageContext);
    }

    /**
     * Get {@link OminalStylingAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link OminalConstants#OMINAL_STYLING_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link OminalStylingAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static OminalStylingAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context ominalStylingPackageContext = OminalUtils.getContextForPackageOrExitApp(context, OminalConstants.OMINAL_STYLING_PACKAGE_NAME, exitAppOnError);
        if (ominalStylingPackageContext == null)
            return null;
        else
            return new OminalStylingAppSharedPreferences(ominalStylingPackageContext);
    }



    public int getLogLevel(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, OMINAL_STYLING_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        else
            return SharedPreferenceUtils.getInt(mSharedPreferences, OMINAL_STYLING_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel, boolean commitToFile) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, OMINAL_STYLING_APP.KEY_LOG_LEVEL, logLevel, commitToFile);
    }

}
