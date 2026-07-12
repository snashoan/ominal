package com.ominal.shared.runtime.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominal.shared.android.PackageUtils;
import com.ominal.shared.settings.preferences.AppSharedPreferences;
import com.ominal.shared.settings.preferences.SharedPreferenceUtils;
import com.ominal.shared.runtime.OminalConstants;
import com.ominal.shared.runtime.OminalUtils;
import com.ominal.shared.runtime.settings.preferences.OminalPreferenceConstants.OMINAL_TASKER_APP;
import com.ominal.shared.logger.Logger;

public class OminalTaskerAppSharedPreferences extends AppSharedPreferences {

    private static final String LOG_TAG = "OminalTaskerAppSharedPreferences";

    private  OminalTaskerAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                OminalConstants.OMINAL_TASKER_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                OminalConstants.OMINAL_TASKER_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));
    }

    /**
     * Get {@link OminalTaskerAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link OminalConstants#OMINAL_TASKER_PACKAGE_NAME}.
     * @return Returns the {@link OminalTaskerAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static OminalTaskerAppSharedPreferences build(@NonNull final Context context) {
        Context ominalTaskerPackageContext = PackageUtils.getContextForPackage(context, OminalConstants.OMINAL_TASKER_PACKAGE_NAME);
        if (ominalTaskerPackageContext == null)
            return null;
        else
            return new OminalTaskerAppSharedPreferences(ominalTaskerPackageContext);
    }

    /**
     * Get {@link OminalTaskerAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link OminalConstants#OMINAL_TASKER_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link OminalTaskerAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static  OminalTaskerAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context ominalTaskerPackageContext = OminalUtils.getContextForPackageOrExitApp(context, OminalConstants.OMINAL_TASKER_PACKAGE_NAME, exitAppOnError);
        if (ominalTaskerPackageContext == null)
            return null;
        else
            return new OminalTaskerAppSharedPreferences(ominalTaskerPackageContext);
    }



    public int getLogLevel(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, OMINAL_TASKER_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        else
            return SharedPreferenceUtils.getInt(mSharedPreferences, OMINAL_TASKER_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel, boolean commitToFile) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, OMINAL_TASKER_APP.KEY_LOG_LEVEL, logLevel, commitToFile);
    }



    public int getLastPendingIntentRequestCode() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, OMINAL_TASKER_APP.KEY_LAST_PENDING_INTENT_REQUEST_CODE, OMINAL_TASKER_APP.DEFAULT_VALUE_KEY_LAST_PENDING_INTENT_REQUEST_CODE);
    }

    public void setLastPendingIntentRequestCode(int lastPendingIntentRequestCode) {
        SharedPreferenceUtils.setInt(mSharedPreferences, OMINAL_TASKER_APP.KEY_LAST_PENDING_INTENT_REQUEST_CODE, lastPendingIntentRequestCode, false);
    }

}
