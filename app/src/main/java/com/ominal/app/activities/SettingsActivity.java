package com.ominal.app.activities;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.ominal.R;
import com.ominal.shared.activities.ReportActivity;
import com.ominal.shared.file.FileUtils;
import com.ominal.shared.models.ReportInfo;
import com.ominal.app.models.UserAction;
import com.ominal.shared.interact.ShareUtils;
import com.ominal.shared.android.PackageUtils;
import com.ominal.shared.runtime.settings.preferences.OminalAPIAppSharedPreferences;
import com.ominal.shared.runtime.settings.preferences.OminalFloatAppSharedPreferences;
import com.ominal.shared.runtime.settings.preferences.OminalTaskerAppSharedPreferences;
import com.ominal.shared.runtime.settings.preferences.OminalWidgetAppSharedPreferences;
import com.ominal.shared.android.AndroidUtils;
import com.ominal.shared.runtime.OminalConstants;
import com.ominal.shared.runtime.OminalUtils;
import com.ominal.shared.activity.media.AppCompatActivityUtils;
import com.ominal.shared.theme.NightMode;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);

        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new RootPreferencesFragment())
                .commit();
        }

        AppCompatActivityUtils.setToolbar(this, com.ominal.shared.R.id.toolbar);
        AppCompatActivityUtils.setShowBackButtonInActionBar(this, true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static class RootPreferencesFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            Context context = getContext();
            if (context == null) return;

            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            new Thread() {
                @Override
                public void run() {
                    configureOminalAPIPreference(context);
                    configureOminalFloatPreference(context);
                    configureOminalTaskerPreference(context);
                    configureOminalWidgetPreference(context);
                    configureAboutPreference(context);
                    configureDonatePreference(context);
                }
            }.start();
        }

        private void configureOminalAPIPreference(@NonNull Context context) {
            Preference ominalAPIPreference = findPreference("ominal_api");
            if (ominalAPIPreference != null) {
                OminalAPIAppSharedPreferences preferences = OminalAPIAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                ominalAPIPreference.setVisible(preferences != null);
            }
        }

        private void configureOminalFloatPreference(@NonNull Context context) {
            Preference ominalFloatPreference = findPreference("ominal_float");
            if (ominalFloatPreference != null) {
                OminalFloatAppSharedPreferences preferences = OminalFloatAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                ominalFloatPreference.setVisible(preferences != null);
            }
        }

        private void configureOminalTaskerPreference(@NonNull Context context) {
            Preference ominalTaskerPreference = findPreference("ominal_tasker");
            if (ominalTaskerPreference != null) {
                OminalTaskerAppSharedPreferences preferences = OminalTaskerAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                ominalTaskerPreference.setVisible(preferences != null);
            }
        }

        private void configureOminalWidgetPreference(@NonNull Context context) {
            Preference ominalWidgetPreference = findPreference("ominal_widget");
            if (ominalWidgetPreference != null) {
                OminalWidgetAppSharedPreferences preferences = OminalWidgetAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                ominalWidgetPreference.setVisible(preferences != null);
            }
        }

        private void configureAboutPreference(@NonNull Context context) {
            Preference aboutPreference = findPreference("about");
            if (aboutPreference != null) {
                aboutPreference.setOnPreferenceClickListener(preference -> {
                    new Thread() {
                        @Override
                        public void run() {
                            String title = "About";

                            StringBuilder aboutString = new StringBuilder();
                            aboutString.append(OminalUtils.getAppInfoMarkdownString(context, OminalUtils.AppInfoMode.OMINAL_AND_PLUGIN_PACKAGES));
                            aboutString.append("\n\n").append(AndroidUtils.getDeviceInfoMarkdownString(context, true));
                            aboutString.append("\n\n").append(OminalUtils.getImportantLinksMarkdownString(context));

                            String userActionName = UserAction.ABOUT.getName();

                            ReportInfo reportInfo = new ReportInfo(userActionName,
                                OminalConstants.OMINAL_APP.OMINAL_SETTINGS_ACTIVITY_NAME, title);
                            reportInfo.setReportString(aboutString.toString());
                            reportInfo.setReportSaveFileLabelAndPath(userActionName,
                                Environment.getExternalStorageDirectory() + "/" +
                                    FileUtils.sanitizeFileName(OminalConstants.OMINAL_APP_NAME + "-" + userActionName + ".log", true, true));

                            ReportActivity.startReportActivity(context, reportInfo);
                        }
                    }.start();

                    return true;
                });
            }
        }

        private void configureDonatePreference(@NonNull Context context) {
            Preference donatePreference = findPreference("donate");
            if (donatePreference != null) {
                String signingCertificateSHA256Digest = PackageUtils.getSigningCertificateSHA256DigestForPackage(context);
                if (signingCertificateSHA256Digest != null) {
                    // If APK is a Google Playstore release, then do not show the donation link
                    // since Ominal isn't exempted from the playstore policy donation links restriction
                    // Check Fund solicitations: https://pay.google.com/intl/en_in/about/policy/
                    String apkRelease = OminalUtils.getAPKRelease(signingCertificateSHA256Digest);
                    if (apkRelease == null || apkRelease.equals(OminalConstants.APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST)) {
                        donatePreference.setVisible(false);
                        return;
                    } else {
                        donatePreference.setVisible(true);
                    }
                }

                donatePreference.setOnPreferenceClickListener(preference -> {
                    ShareUtils.openUrl(context, OminalConstants.OMINAL_DONATE_URL);
                    return true;
                });
            }
        }
    }

}
