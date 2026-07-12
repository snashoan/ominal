package com.ominal.shared.runtime.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominal.shared.logger.Logger;
import com.ominal.shared.android.PackageUtils;
import com.ominal.shared.settings.preferences.AppSharedPreferences;
import com.ominal.shared.settings.preferences.SharedPreferenceUtils;
import com.ominal.shared.runtime.OminalUtils;
import com.ominal.shared.runtime.settings.preferences.OminalPreferenceConstants.OMINAL_WIDGET_APP;
import com.ominal.shared.runtime.OminalConstants;

import java.util.UUID;

public class OminalWidgetAppSharedPreferences extends AppSharedPreferences {

    private static final String LOG_TAG = "OminalWidgetAppSharedPreferences";

    private OminalWidgetAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                OminalConstants.OMINAL_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                OminalConstants.OMINAL_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));
    }

    /**
     * Get {@link OminalWidgetAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link OminalConstants#OMINAL_WIDGET_PACKAGE_NAME}.
     * @return Returns the {@link OminalWidgetAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static OminalWidgetAppSharedPreferences build(@NonNull final Context context) {
        Context ominalWidgetPackageContext = PackageUtils.getContextForPackage(context, OminalConstants.OMINAL_WIDGET_PACKAGE_NAME);
        if (ominalWidgetPackageContext == null)
            return null;
        else
            return new OminalWidgetAppSharedPreferences(ominalWidgetPackageContext);
    }

    /**
     * Get the {@link OminalWidgetAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link OminalConstants#OMINAL_WIDGET_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link OminalWidgetAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static OminalWidgetAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context ominalWidgetPackageContext = OminalUtils.getContextForPackageOrExitApp(context, OminalConstants.OMINAL_WIDGET_PACKAGE_NAME, exitAppOnError);
        if (ominalWidgetPackageContext == null)
            return null;
        else
            return new OminalWidgetAppSharedPreferences(ominalWidgetPackageContext);
    }



    public static String getGeneratedToken(@NonNull Context context) {
        OminalWidgetAppSharedPreferences preferences = OminalWidgetAppSharedPreferences.build(context, true);
        if (preferences == null) return null;
        return preferences.getGeneratedToken();
    }

    public String getGeneratedToken() {
        String token =  SharedPreferenceUtils.getString(mSharedPreferences, OMINAL_WIDGET_APP.KEY_TOKEN, null, true);
        if (token == null) {
            token = UUID.randomUUID().toString();
            SharedPreferenceUtils.setString(mSharedPreferences, OMINAL_WIDGET_APP.KEY_TOKEN, token, true);
        }
        return token;
    }



    public int getLogLevel(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, OMINAL_WIDGET_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        else
            return SharedPreferenceUtils.getInt(mSharedPreferences, OMINAL_WIDGET_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel, boolean commitToFile) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, OMINAL_WIDGET_APP.KEY_LOG_LEVEL, logLevel, commitToFile);
    }

}
