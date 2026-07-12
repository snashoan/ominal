package com.ominal.shared.runtime;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominal.shared.R;
import com.ominal.shared.android.AndroidUtils;
import com.ominal.shared.data.DataUtils;
import com.ominal.shared.file.FileUtils;
import com.ominal.shared.reflection.ReflectionUtils;
import com.ominal.shared.shell.command.runner.app.AppShell;
import com.ominal.shared.runtime.file.OminalFileUtils;
import com.ominal.shared.logger.Logger;
import com.ominal.shared.markdown.MarkdownUtils;
import com.ominal.shared.shell.command.ExecutionCommand;
import com.ominal.shared.errors.Error;
import com.ominal.shared.android.PackageUtils;
import com.ominal.shared.runtime.OminalConstants.OMINAL_APP;
import com.ominal.shared.runtime.shell.command.environment.OminalShellEnvironment;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Pattern;

public class OminalUtils {

    /** The modes used by {@link #getAppInfoMarkdownString(Context, AppInfoMode, String)}. */
    public enum AppInfoMode {
        /** Get info for Ominal app only. */
        OMINAL_PACKAGE,
        /** Get info for Ominal app and plugin app if context is of plugin app. */
        OMINAL_AND_PLUGIN_PACKAGE,
        /** Get info for Ominal app and its plugins listed in {@link OminalConstants#OMINAL_PLUGIN_APP_PACKAGE_NAMES_LIST}. */
        OMINAL_AND_PLUGIN_PACKAGES,
        /* Get info for all the Ominal app plugins listed in {@link OminalConstants#OMINAL_PLUGIN_APP_PACKAGE_NAMES_LIST}. */
        OMINAL_PLUGIN_PACKAGES,
        /* Get info for Ominal app and the calling package that called a Ominal API. */
        OMINAL_AND_CALLING_PACKAGE,
    }

    private static final String LOG_TAG = "OminalUtils";

    /**
     * Get the {@link Context} for {@link OminalConstants#OMINAL_PACKAGE_NAME} package with the
     * {@link Context#CONTEXT_RESTRICTED} flag.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the package.
     * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
     */
    public static Context getOminalPackageContext(@NonNull Context context) {
        return PackageUtils.getContextForPackage(context, OminalConstants.OMINAL_PACKAGE_NAME);
    }

    /**
     * Get the {@link Context} for {@link OminalConstants#OMINAL_PACKAGE_NAME} package with the
     * {@link Context#CONTEXT_INCLUDE_CODE} flag.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the package.
     * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
     */
    public static Context getOminalPackageContextWithCode(@NonNull Context context) {
        return PackageUtils.getContextForPackage(context, OminalConstants.OMINAL_PACKAGE_NAME, Context.CONTEXT_INCLUDE_CODE);
    }

    /**
     * Get the {@link Context} for {@link OminalConstants#OMINAL_API_PACKAGE_NAME} package.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the package.
     * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
     */
    public static Context getOminalAPIPackageContext(@NonNull Context context) {
        return PackageUtils.getContextForPackage(context, OminalConstants.OMINAL_API_PACKAGE_NAME);
    }

    /**
     * Get the {@link Context} for {@link OminalConstants#OMINAL_BOOT_PACKAGE_NAME} package.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the package.
     * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
     */
    public static Context getOminalBootPackageContext(@NonNull Context context) {
        return PackageUtils.getContextForPackage(context, OminalConstants.OMINAL_BOOT_PACKAGE_NAME);
    }

    /**
     * Get the {@link Context} for {@link OminalConstants#OMINAL_FLOAT_PACKAGE_NAME} package.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the package.
     * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
     */
    public static Context getOminalFloatPackageContext(@NonNull Context context) {
        return PackageUtils.getContextForPackage(context, OminalConstants.OMINAL_FLOAT_PACKAGE_NAME);
    }

    /**
     * Get the {@link Context} for {@link OminalConstants#OMINAL_STYLING_PACKAGE_NAME} package.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the package.
     * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
     */
    public static Context getOminalStylingPackageContext(@NonNull Context context) {
        return PackageUtils.getContextForPackage(context, OminalConstants.OMINAL_STYLING_PACKAGE_NAME);
    }

    /**
     * Get the {@link Context} for {@link OminalConstants#OMINAL_TASKER_PACKAGE_NAME} package.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the package.
     * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
     */
    public static Context getOminalTaskerPackageContext(@NonNull Context context) {
        return PackageUtils.getContextForPackage(context, OminalConstants.OMINAL_TASKER_PACKAGE_NAME);
    }

    /**
     * Get the {@link Context} for {@link OminalConstants#OMINAL_WIDGET_PACKAGE_NAME} package.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the package.
     * @return Returns the {@link Context}. This will {@code null} if an exception is raised.
     */
    public static Context getOminalWidgetPackageContext(@NonNull Context context) {
        return PackageUtils.getContextForPackage(context, OminalConstants.OMINAL_WIDGET_PACKAGE_NAME);
    }

    /** Wrapper for {@link PackageUtils#getContextForPackageOrExitApp(Context, String, boolean, String)}. */
    public static Context getContextForPackageOrExitApp(@NonNull Context context, String packageName,
                                                        final boolean exitAppOnError) {
        return PackageUtils.getContextForPackageOrExitApp(context, packageName, exitAppOnError, OminalConstants.OMINAL_GITHUB_REPO_URL);
    }

    /**
     * Check if Ominal app is installed and enabled. This can be used by external apps that don't
     * share `sharedUserId` with the Ominal app.
     *
     * If your third-party app is targeting sdk `30` (android `11`), then it needs to add `com.ominal`
     * package to the `queries` element or request `QUERY_ALL_PACKAGES` permission in its
     * `AndroidManifest.xml`. Otherwise it will get `PackageSetting{...... com.ominal/......} BLOCKED`
     * errors in `logcat` and `RUN_COMMAND` won't work.
     * Check [package-visibility](https://developer.android.com/training/basics/intents/package-visibility#package-name),
     * `QUERY_ALL_PACKAGES` [googleplay policy](https://support.google.com/googleplay/android-developer/answer/10158779
     * and this [article](https://medium.com/androiddevelopers/working-with-package-visibility-dc252829de2d) for more info.
     *
     * {@code
     * <manifest
     *     <queries>
     *         <package android:name="com.ominal" />
     *    </queries>
     * </manifest>
     * }
     *
     * @param context The context for operations.
     * @return Returns {@code errmsg} if {@link OminalConstants#OMINAL_PACKAGE_NAME} is not installed
     * or disabled, otherwise {@code null}.
     */
    public static String isOminalAppInstalled(@NonNull final Context context) {
        return PackageUtils.isAppInstalled(context, OminalConstants.OMINAL_APP_NAME, OminalConstants.OMINAL_PACKAGE_NAME);
    }

    /**
     * Check if Ominal:API app is installed and enabled. This can be used by external apps that don't
     * share `sharedUserId` with the Ominal:API app.
     *
     * @param context The context for operations.
     * @return Returns {@code errmsg} if {@link OminalConstants#OMINAL_API_PACKAGE_NAME} is not installed
     * or disabled, otherwise {@code null}.
     */
    public static String isOminalAPIAppInstalled(@NonNull final Context context) {
        return PackageUtils.isAppInstalled(context, OminalConstants.OMINAL_API_APP_NAME, OminalConstants.OMINAL_API_PACKAGE_NAME);
    }

    /**
     * Check if Ominal app is installed and accessible. This can only be used by apps that share
     * `sharedUserId` with the Ominal app.
     *
     * This is done by checking if first checking if app is installed and enabled and then if
     * {@code currentPackageContext} can be used to get the {@link Context} of the app with
     * {@link OminalConstants#OMINAL_PACKAGE_NAME} and then if
     * {@link OminalConstants#OMINAL_PREFIX_DIR_PATH} exists and has
     * {@link FileUtils#APP_WORKING_DIRECTORY_PERMISSIONS} permissions. The directory will not
     * be automatically created and neither the missing permissions automatically set.
     *
     * @param currentPackageContext The context of current package.
     * @return Returns {@code errmsg} if failed to get ominal package {@link Context} or
     * {@link OminalConstants#OMINAL_PREFIX_DIR_PATH} is accessible, otherwise {@code null}.
     */
    public static String isOminalAppAccessible(@NonNull final Context currentPackageContext) {
        String errmsg = isOminalAppInstalled(currentPackageContext);
        if (errmsg == null) {
            Context ominalPackageContext = OminalUtils.getOminalPackageContext(currentPackageContext);
            // If failed to get Ominal app package context
            if (ominalPackageContext == null)
                errmsg = currentPackageContext.getString(R.string.error_ominal_app_package_context_not_accessible);

            if (errmsg == null) {
                // If OminalConstants.OMINAL_PREFIX_DIR_PATH is not a directory or does not have required permissions
                Error error = OminalFileUtils.isOminalPrefixDirectoryAccessible(false, false);
                if (error != null)
                    errmsg = currentPackageContext.getString(R.string.error_ominal_prefix_dir_path_not_accessible,
                        PackageUtils.getAppNameForPackage(currentPackageContext));
            }
        }

        if (errmsg != null)
            return errmsg + " " + currentPackageContext.getString(R.string.msg_ominal_app_required_by_app,
                PackageUtils.getAppNameForPackage(currentPackageContext));
        else
            return null;
    }



    /**
     * Get a field value from the {@link OMINAL_APP#BUILD_CONFIG_CLASS_NAME} class of the Ominal app
     * APK installed on the device.
     * This can only be used by apps that share `sharedUserId` with the Ominal app.
     *
     * This is a wrapper for {@link #getOminalAppAPKClassField(Context, String, String)}.
     *
     * @param currentPackageContext The context of current package.
     * @param fieldName The name of the field to get.
     * @return Returns the field value, otherwise {@code null} if an exception was raised or failed
     * to get ominal app package context.
     */
    public static Object getOminalAppAPKBuildConfigClassField(@NonNull Context currentPackageContext,
                                                              @NonNull String fieldName) {
        return getOminalAppAPKClassField(currentPackageContext, OMINAL_APP.BUILD_CONFIG_CLASS_NAME, fieldName);
    }

    /**
     * Get a field value from a class of the Ominal app APK installed on the device.
     * This can only be used by apps that share `sharedUserId` with the Ominal app.
     *
     * This is done by getting first getting ominal app package context and then getting in class
     * loader (instead of current app's) that contains ominal app class info, and then using that to
     * load the required class and then getting required field from it.
     *
     * Note that the value returned is from the APK file and not the current value loaded in Ominal
     * app process, so only default values will be returned.
     *
     * Trying to access {@code null} fields will result in {@link NoSuchFieldException}.
     *
     * @param currentPackageContext The context of current package.
     * @param clazzName The name of the class from which to get the field.
     * @param fieldName The name of the field to get.
     * @return Returns the field value, otherwise {@code null} if an exception was raised or failed
     * to get ominal app package context.
     */
    public static Object getOminalAppAPKClassField(@NonNull Context currentPackageContext,
                                                   @NonNull String clazzName, @NonNull String fieldName) {
        try {
            Context ominalPackageContext = OminalUtils.getOminalPackageContextWithCode(currentPackageContext);
            if (ominalPackageContext == null)
                return null;

            Class<?> clazz = ominalPackageContext.getClassLoader().loadClass(clazzName);
            return ReflectionUtils.invokeField(clazz, fieldName, null).value;
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to get \"" + fieldName + "\" value from \"" + clazzName + "\" class", e);
            return null;
        }
    }



    /** Returns {@code true} if {@link Uri} has `package:` scheme for {@link OminalConstants#OMINAL_PACKAGE_NAME} or its sub plugin package. */
    public static boolean isUriDataForOminalOrPluginPackage(@NonNull Uri data) {
        return data.toString().equals("package:" + OminalConstants.OMINAL_PACKAGE_NAME) ||
            data.toString().startsWith("package:" + OminalConstants.OMINAL_PACKAGE_NAME + ".");
    }

    /** Returns {@code true} if {@link Uri} has `package:` scheme for {@link OminalConstants#OMINAL_PACKAGE_NAME} sub plugin package. */
    public static boolean isUriDataForOminalPluginPackage(@NonNull Uri data) {
        return data.toString().startsWith("package:" + OminalConstants.OMINAL_PACKAGE_NAME + ".");
    }

    /**
     * Send the {@link OminalConstants#BROADCAST_OMINAL_OPENED} broadcast to notify apps that Ominal
     * app has been opened.
     *
     * @param context The Context to send the broadcast.
     */
    public static void sendOminalOpenedBroadcast(@NonNull Context context) {
        Intent broadcast = new Intent(OminalConstants.BROADCAST_OMINAL_OPENED);
        List<ResolveInfo> matches = context.getPackageManager().queryBroadcastReceivers(broadcast, 0);

        // send broadcast to registered Ominal receivers
        // this technique is needed to work around broadcast changes that Oreo introduced
        for (ResolveInfo info : matches) {
            Intent explicitBroadcast = new Intent(broadcast);
            ComponentName cname = new ComponentName(info.activityInfo.applicationInfo.packageName,
                info.activityInfo.name);
            explicitBroadcast.setComponent(cname);
            context.sendBroadcast(explicitBroadcast);
        }
    }



    /**
     * Wrapper for {@link #getAppInfoMarkdownString(Context, AppInfoMode, String)}.
     *
     * @param currentPackageContext The context of current package.
     * @param appInfoMode The {@link AppInfoMode} to decide the app info required.
     * @return Returns the markdown {@link String}.
     */
    public static String getAppInfoMarkdownString(final Context currentPackageContext, final AppInfoMode appInfoMode) {
        return getAppInfoMarkdownString(currentPackageContext, appInfoMode, null);
    }

    /**
     * Get a markdown {@link String} for the apps info of ominal app, its installed plugin apps or
     * external apps that called a Ominal API depending on {@link AppInfoMode} passed.
     *
     * Also check {@link PackageUtils#isAppInstalled(Context, String, String) if targetting targeting
     * sdk `30` (android `11`) since {@link PackageManager.NameNotFoundException} may be thrown while
     * getting info of {@code callingPackageName} app.
     *
     * @param currentPackageContext The context of current package.
     * @param appInfoMode The {@link AppInfoMode} to decide the app info required.
     * @param callingPackageName The optional package name for a plugin or external app.
     * @return Returns the markdown {@link String}.
     */
    public static String getAppInfoMarkdownString(final Context currentPackageContext, final AppInfoMode appInfoMode, @Nullable String callingPackageName) {
        if (appInfoMode == null) return null;

        StringBuilder appInfo = new StringBuilder();
        switch (appInfoMode) {
            case OMINAL_PACKAGE:
                return getAppInfoMarkdownString(currentPackageContext, false);

            case OMINAL_AND_PLUGIN_PACKAGE:
                return getAppInfoMarkdownString(currentPackageContext, true);

            case OMINAL_AND_PLUGIN_PACKAGES:
                appInfo.append(OminalUtils.getAppInfoMarkdownString(currentPackageContext, false));

                String ominalPluginAppsInfo =  OminalUtils.getOminalPluginAppsInfoMarkdownString(currentPackageContext);
                if (ominalPluginAppsInfo != null)
                    appInfo.append("\n\n").append(ominalPluginAppsInfo);
                return appInfo.toString();

            case OMINAL_PLUGIN_PACKAGES:
                return OminalUtils.getOminalPluginAppsInfoMarkdownString(currentPackageContext);

            case OMINAL_AND_CALLING_PACKAGE:
                appInfo.append(OminalUtils.getAppInfoMarkdownString(currentPackageContext, false));
                if (!DataUtils.isNullOrEmpty(callingPackageName)) {
                    String callingPackageAppInfo = null;
                    if (OminalConstants.OMINAL_PLUGIN_APP_PACKAGE_NAMES_LIST.contains(callingPackageName)) {
                        Context ominalPluginAppContext = PackageUtils.getContextForPackage(currentPackageContext, callingPackageName);
                        if (ominalPluginAppContext != null)
                            appInfo.append(getAppInfoMarkdownString(ominalPluginAppContext, false));
                        else
                            callingPackageAppInfo = AndroidUtils.getAppInfoMarkdownString(currentPackageContext, callingPackageName);
                    } else {
                        callingPackageAppInfo = AndroidUtils.getAppInfoMarkdownString(currentPackageContext, callingPackageName);
                    }

                    if (callingPackageAppInfo != null) {
                        ApplicationInfo applicationInfo = PackageUtils.getApplicationInfoForPackage(currentPackageContext, callingPackageName);
                        if (applicationInfo != null) {
                            appInfo.append("\n\n## ").append(PackageUtils.getAppNameForPackage(currentPackageContext, applicationInfo)).append(" App Info\n");
                            appInfo.append(callingPackageAppInfo);
                            appInfo.append("\n##\n");
                        }
                    }
                }
                return appInfo.toString();

            default:
                return null;
        }

    }

    /**
     * Get a markdown {@link String} for the apps info of all/any ominal plugin apps installed.
     *
     * @param currentPackageContext The context of current package.
     * @return Returns the markdown {@link String}.
     */
    public static String getOminalPluginAppsInfoMarkdownString(final Context currentPackageContext) {
        if (currentPackageContext == null) return "null";

        StringBuilder markdownString = new StringBuilder();

        List<String> ominalPluginAppPackageNamesList = OminalConstants.OMINAL_PLUGIN_APP_PACKAGE_NAMES_LIST;

        if (ominalPluginAppPackageNamesList != null) {
            for (int i = 0; i < ominalPluginAppPackageNamesList.size(); i++) {
                String ominalPluginAppPackageName = ominalPluginAppPackageNamesList.get(i);
                Context ominalPluginAppContext = PackageUtils.getContextForPackage(currentPackageContext, ominalPluginAppPackageName);
                // If the package context for the plugin app is not null, then assume its installed and get its info
                if (ominalPluginAppContext != null) {
                    if (i != 0)
                        markdownString.append("\n\n");
                    markdownString.append(getAppInfoMarkdownString(ominalPluginAppContext, false));
                }
            }
        }

        if (markdownString.toString().isEmpty())
            return null;

        return markdownString.toString();
    }

    /**
     * Get a markdown {@link String} for the app info. If the {@code context} passed is different
     * from the {@link OminalConstants#OMINAL_PACKAGE_NAME} package context, then this function
     * must have been called by a different package like a plugin, so we return info for both packages
     * if {@code returnOminalPackageInfoToo} is {@code true}.
     *
     * @param currentPackageContext The context of current package.
     * @param returnOminalPackageInfoToo If set to {@code true}, then will return info of the
     * {@link OminalConstants#OMINAL_PACKAGE_NAME} package as well if its different from current package.
     * @return Returns the markdown {@link String}.
     */
    public static String getAppInfoMarkdownString(final Context currentPackageContext, final boolean returnOminalPackageInfoToo) {
        if (currentPackageContext == null) return "null";

        StringBuilder markdownString = new StringBuilder();

        Context ominalPackageContext = getOminalPackageContext(currentPackageContext);

        String ominalPackageName = null;
        String ominalAppName = null;
        if (ominalPackageContext != null) {
            ominalPackageName = PackageUtils.getPackageNameForPackage(ominalPackageContext);
            ominalAppName = PackageUtils.getAppNameForPackage(ominalPackageContext);
        }

        String currentPackageName = PackageUtils.getPackageNameForPackage(currentPackageContext);
        String currentAppName = PackageUtils.getAppNameForPackage(currentPackageContext);

        boolean isOminalPackage = (ominalPackageName != null && ominalPackageName.equals(currentPackageName));


        if (returnOminalPackageInfoToo && !isOminalPackage)
            markdownString.append("## ").append(currentAppName).append(" App Info (Current)\n");
        else
            markdownString.append("## ").append(currentAppName).append(" App Info\n");
        markdownString.append(getAppInfoMarkdownStringInner(currentPackageContext));
        markdownString.append("\n##\n");

        if (returnOminalPackageInfoToo && ominalPackageContext != null && !isOminalPackage) {
            markdownString.append("\n\n## ").append(ominalAppName).append(" App Info\n");
            markdownString.append(getAppInfoMarkdownStringInner(ominalPackageContext));
            markdownString.append("\n##\n");
        }


        return markdownString.toString();
    }

    /**
     * Get a markdown {@link String} for the app info for the package associated with the {@code context}.
     *
     * @param context The context for operations for the package.
     * @return Returns the markdown {@link String}.
     */
    public static String getAppInfoMarkdownStringInner(@NonNull final Context context) {
        StringBuilder markdownString = new StringBuilder();

        markdownString.append((AndroidUtils.getAppInfoMarkdownString(context)));

        if (context.getPackageName().equals(OminalConstants.OMINAL_PACKAGE_NAME)) {
            AndroidUtils.appendPropertyToMarkdown(markdownString, "OMINAL_APP_PACKAGE_MANAGER", OminalBootstrap.OMINAL_APP_PACKAGE_MANAGER);
            AndroidUtils.appendPropertyToMarkdown(markdownString, "OMINAL_APP_PACKAGE_VARIANT", OminalBootstrap.OMINAL_APP_PACKAGE_VARIANT);
        }

        Error error;
        error = OminalFileUtils.isOminalFilesDirectoryAccessible(context, true, true);
        if (error != null) {
            AndroidUtils.appendPropertyToMarkdown(markdownString, "OMINAL_FILES_DIR", OminalConstants.OMINAL_FILES_DIR_PATH);
            AndroidUtils.appendPropertyToMarkdown(markdownString, "IS_OMINAL_FILES_DIR_ACCESSIBLE", "false - " + Error.getMinimalErrorString(error));
        }

        String signingCertificateSHA256Digest = PackageUtils.getSigningCertificateSHA256DigestForPackage(context);
        if (signingCertificateSHA256Digest != null) {
            AndroidUtils.appendPropertyToMarkdown(markdownString,"APK_RELEASE", getAPKRelease(signingCertificateSHA256Digest));
            AndroidUtils.appendPropertyToMarkdown(markdownString,"SIGNING_CERTIFICATE_SHA256_DIGEST", signingCertificateSHA256Digest);
        }

        return markdownString.toString();
    }

    /**
     * Get a markdown {@link String} for reporting an issue.
     *
     * @param context The context for operations.
     * @return Returns the markdown {@link String}.
     */
    public static String getReportIssueMarkdownString(@NonNull final Context context) {
        if (context == null) return "null";

        StringBuilder markdownString = new StringBuilder();

        markdownString.append("## Where To Report An Issue");

        markdownString.append("\n\n").append(context.getString(R.string.msg_report_issue, OminalConstants.OMINAL_WIKI_URL)).append("\n");

        markdownString.append("\n\n### Email\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_SUPPORT_EMAIL_URL, OminalConstants.OMINAL_SUPPORT_EMAIL_MAILTO_URL)).append("  ");

        markdownString.append("\n\n### Reddit\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_REDDIT_SUBREDDIT, OminalConstants.OMINAL_REDDIT_SUBREDDIT_URL)).append("  ");

        markdownString.append("\n\n### GitHub Issues for Ominal apps\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_APP_NAME, OminalConstants.OMINAL_GITHUB_ISSUES_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_API_APP_NAME, OminalConstants.OMINAL_API_GITHUB_ISSUES_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_BOOT_APP_NAME, OminalConstants.OMINAL_BOOT_GITHUB_ISSUES_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_FLOAT_APP_NAME, OminalConstants.OMINAL_FLOAT_GITHUB_ISSUES_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_STYLING_APP_NAME, OminalConstants.OMINAL_STYLING_GITHUB_ISSUES_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_TASKER_APP_NAME, OminalConstants.OMINAL_TASKER_GITHUB_ISSUES_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_WIDGET_APP_NAME, OminalConstants.OMINAL_WIDGET_GITHUB_ISSUES_REPO_URL)).append("  ");

        markdownString.append("\n\n### GitHub Issues for Ominal packages\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_PACKAGES_GITHUB_REPO_NAME, OminalConstants.OMINAL_PACKAGES_GITHUB_ISSUES_REPO_URL)).append("  ");

        markdownString.append("\n##\n");

        return markdownString.toString();
    }

    /**
     * Get a markdown {@link String} for important links.
     *
     * @param context The context for operations.
     * @return Returns the markdown {@link String}.
     */
    public static String getImportantLinksMarkdownString(@NonNull final Context context) {
        if (context == null) return "null";

        StringBuilder markdownString = new StringBuilder();

        markdownString.append("## Important Links");

        markdownString.append("\n\n### GitHub\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_APP_NAME, OminalConstants.OMINAL_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_API_APP_NAME, OminalConstants.OMINAL_API_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_BOOT_APP_NAME, OminalConstants.OMINAL_BOOT_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_FLOAT_APP_NAME, OminalConstants.OMINAL_FLOAT_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_STYLING_APP_NAME, OminalConstants.OMINAL_STYLING_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_TASKER_APP_NAME, OminalConstants.OMINAL_TASKER_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_WIDGET_APP_NAME, OminalConstants.OMINAL_WIDGET_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_PACKAGES_GITHUB_REPO_NAME, OminalConstants.OMINAL_PACKAGES_GITHUB_REPO_URL)).append("  ");

        markdownString.append("\n\n### Email\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_SUPPORT_EMAIL_URL, OminalConstants.OMINAL_SUPPORT_EMAIL_MAILTO_URL)).append("  ");

        markdownString.append("\n\n### Reddit\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_REDDIT_SUBREDDIT, OminalConstants.OMINAL_REDDIT_SUBREDDIT_URL)).append("  ");

        markdownString.append("\n\n### Wiki\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_WIKI, OminalConstants.OMINAL_WIKI_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_APP_NAME, OminalConstants.OMINAL_GITHUB_WIKI_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(OminalConstants.OMINAL_PACKAGES_GITHUB_REPO_NAME, OminalConstants.OMINAL_PACKAGES_GITHUB_WIKI_REPO_URL)).append("  ");

        markdownString.append("\n##\n");

        return markdownString.toString();
    }



    /**
     * Get a markdown {@link String} for APT info of the app.
     *
     * This will take a few seconds to run due to running {@code apt update} command.
     *
     * @param context The context for operations.
     * @return Returns the markdown {@link String}.
     */
    public static String geAPTInfoMarkdownString(@NonNull final Context context) {

        String aptInfoScript;
        InputStream inputStream = context.getResources().openRawResource(com.ominal.shared.R.raw.apt_info_script);
        try {
            aptInfoScript = IOUtils.toString(inputStream, Charset.defaultCharset());
        } catch (IOException e) {
            Logger.logError(LOG_TAG, "Failed to get APT info script: " + e.getMessage());
            return null;
        }

        IOUtils.closeQuietly(inputStream);

        if (aptInfoScript == null || aptInfoScript.isEmpty()) {
            Logger.logError(LOG_TAG, "The APT info script is null or empty");
            return null;
        }

        aptInfoScript = aptInfoScript.replaceAll(Pattern.quote("@OMINAL_PREFIX@"), OminalConstants.OMINAL_PREFIX_DIR_PATH);

        ExecutionCommand executionCommand = new ExecutionCommand(-1,
            OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH + "/bash", null, aptInfoScript,
            null, ExecutionCommand.Runner.APP_SHELL.getName(), false);
        executionCommand.commandLabel = "APT Info Command";
        executionCommand.backgroundCustomLogLevel = Logger.LOG_LEVEL_OFF;
        AppShell appShell = AppShell.execute(context, executionCommand, null, new OminalShellEnvironment(), null, true);
        if (appShell == null || !executionCommand.isSuccessful() || executionCommand.resultData.exitCode != 0) {
            Logger.logErrorExtended(LOG_TAG, executionCommand.toString());
            return null;
        }

        if (!executionCommand.resultData.stderr.toString().isEmpty())
            Logger.logErrorExtended(LOG_TAG, executionCommand.toString());

        StringBuilder markdownString = new StringBuilder();

        markdownString.append("## ").append(OminalConstants.OMINAL_APP_NAME).append(" APT Info\n\n");
        markdownString.append(executionCommand.resultData.stdout.toString());
        markdownString.append("\n##\n");

        return markdownString.toString();
    }

    /**
     * Get a markdown {@link String} for info for ominal debugging.
     *
     * @param context The context for operations.
     * @return Returns the markdown {@link String}.
     */
    public static String getOminalDebugMarkdownString(@NonNull final Context context) {
        String statInfo = OminalFileUtils.getOminalFilesStatMarkdownString(context);
        String logcatInfo = getLogcatDumpMarkdownString(context);

        if (statInfo != null && logcatInfo != null)
            return statInfo + "\n\n" + logcatInfo;
        else if (statInfo != null)
            return statInfo;
        else
            return logcatInfo;

    }

    /**
     * Get a markdown {@link String} for logcat command dump.
     *
     * @param context The context for operations.
     * @return Returns the markdown {@link String}.
     */
    public static String getLogcatDumpMarkdownString(@NonNull final Context context) {
        // Build script
        // We need to prevent OutOfMemoryError since StreamGobbler StringBuilder + StringBuilder.toString()
        // may require lot of memory if dump is too large.
        // Putting a limit at 3000 lines. Assuming average 160 chars/line will result in 500KB usage
        // per object.
        // That many lines should be enough for debugging for recent issues anyways assuming ominal
        // has not been granted READ_LOGS permission s.
        String logcatScript = "/system/bin/logcat -d -t 3000 2>&1";

        // Run script
        // Logging must be disabled for output of logcat command itself in StreamGobbler
        ExecutionCommand executionCommand = new ExecutionCommand(-1, "/system/bin/sh",
            null, logcatScript + "\n", "/", ExecutionCommand.Runner.APP_SHELL.getName(), true);
        executionCommand.commandLabel = "Logcat dump command";
        executionCommand.backgroundCustomLogLevel = Logger.LOG_LEVEL_OFF;
        AppShell appShell = AppShell.execute(context, executionCommand, null, new OminalShellEnvironment(), null, true);
        if (appShell == null || !executionCommand.isSuccessful()) {
            Logger.logErrorExtended(LOG_TAG, executionCommand.toString());
            return null;
        }

        // Build script output
        StringBuilder logcatOutput = new StringBuilder();
        logcatOutput.append("$ ").append(logcatScript);
        logcatOutput.append("\n").append(executionCommand.resultData.stdout.toString());

        boolean stderrSet = !executionCommand.resultData.stderr.toString().isEmpty();
        if (executionCommand.resultData.exitCode != 0 || stderrSet) {
            Logger.logErrorExtended(LOG_TAG, executionCommand.toString());
            if (stderrSet)
                logcatOutput.append("\n").append(executionCommand.resultData.stderr.toString());
            logcatOutput.append("\n").append("exit code: ").append(executionCommand.resultData.exitCode.toString());
        }

        // Build markdown output
        StringBuilder markdownString = new StringBuilder();
        markdownString.append("## Logcat Dump\n\n");
        markdownString.append("\n\n").append(MarkdownUtils.getMarkdownCodeForString(logcatOutput.toString(), true));
        markdownString.append("\n##\n");

        return markdownString.toString();
    }



    public static String getAPKRelease(String signingCertificateSHA256Digest) {
        if (signingCertificateSHA256Digest == null) return "null";

        switch (signingCertificateSHA256Digest.toUpperCase()) {
            case OminalConstants.APK_RELEASE_FDROID_SIGNING_CERTIFICATE_SHA256_DIGEST:
                return OminalConstants.APK_RELEASE_FDROID;
            case OminalConstants.APK_RELEASE_GITHUB_SIGNING_CERTIFICATE_SHA256_DIGEST:
                return OminalConstants.APK_RELEASE_GITHUB;
            case OminalConstants.APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST:
                return OminalConstants.APK_RELEASE_GOOGLE_PLAYSTORE;
            case OminalConstants.APK_RELEASE_OMINAL_DEVS_SIGNING_CERTIFICATE_SHA256_DIGEST:
                return OminalConstants.APK_RELEASE_OMINAL_DEVS;
            default:
                return "Unknown";
        }
    }


    /**
     * Get a process id of the main app process of the {@link OminalConstants#OMINAL_PACKAGE_NAME}
     * package.
     *
     * @param context The context for operations.
     * @return Returns the process if found and running, otherwise {@code null}.
     */
    public static String getOminalAppPID(final Context context) {
        return PackageUtils.getPackagePID(context, OminalConstants.OMINAL_PACKAGE_NAME);
    }

}
