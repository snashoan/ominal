package com.ominal.shared.runtime.shell.command.environment;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominal.shared.android.PackageUtils;
import com.ominal.shared.android.SELinuxUtils;
import com.ominal.shared.data.DataUtils;
import com.ominal.shared.shell.command.environment.ShellEnvironmentUtils;
import com.ominal.shared.runtime.OminalBootstrap;
import com.ominal.shared.runtime.OminalConstants;
import com.ominal.shared.runtime.OminalUtils;

import java.util.HashMap;

/**
 * Environment for {@link OminalConstants#OMINAL_PACKAGE_NAME} app.
 */
public class OminalAppShellEnvironment {

    /** Ominal app environment variables. */
    public static HashMap<String, String> ominalAppEnvironment;

    /** Environment variable for the Ominal app version. */
    public static final String ENV_OMINAL_VERSION = OminalConstants.OMINAL_ENV_PREFIX_ROOT + "_VERSION";

    /** Environment variable prefix for the Ominal app. */
    public static final String OMINAL_APP_ENV_PREFIX = OminalConstants.OMINAL_ENV_PREFIX_ROOT + "_APP__";

    /** Environment variable for the Ominal app version name. */
    public static final String ENV_OMINAL_APP__VERSION_NAME = OMINAL_APP_ENV_PREFIX + "VERSION_NAME";
    /** Environment variable for the Ominal app version code. */
    public static final String ENV_OMINAL_APP__VERSION_CODE = OMINAL_APP_ENV_PREFIX + "VERSION_CODE";
    /** Environment variable for the Ominal app package name. */
    public static final String ENV_OMINAL_APP__PACKAGE_NAME = OMINAL_APP_ENV_PREFIX + "PACKAGE_NAME";
    /** Environment variable for the Ominal app process id. */
    public static final String ENV_OMINAL_APP__PID = OMINAL_APP_ENV_PREFIX + "PID";
    /** Environment variable for the Ominal app uid. */
    public static final String ENV_OMINAL_APP__UID = OMINAL_APP_ENV_PREFIX + "UID";
    /** Environment variable for the Ominal app targetSdkVersion. */
    public static final String ENV_OMINAL_APP__TARGET_SDK = OMINAL_APP_ENV_PREFIX + "TARGET_SDK";
    /** Environment variable for the Ominal app is debuggable apk build. */
    public static final String ENV_OMINAL_APP__IS_DEBUGGABLE_BUILD = OMINAL_APP_ENV_PREFIX + "IS_DEBUGGABLE_BUILD";
    /** Environment variable for the Ominal app {@link OminalConstants} APK_RELEASE_*. */
    public static final String ENV_OMINAL_APP__APK_RELEASE = OMINAL_APP_ENV_PREFIX + "APK_RELEASE";
    /** Environment variable for the Ominal app install path. */
    public static final String ENV_OMINAL_APP__APK_PATH = OMINAL_APP_ENV_PREFIX + "APK_PATH";
    /** Environment variable for the Ominal app is installed on external/portable storage. */
    public static final String ENV_OMINAL_APP__IS_INSTALLED_ON_EXTERNAL_STORAGE = OMINAL_APP_ENV_PREFIX + "IS_INSTALLED_ON_EXTERNAL_STORAGE";

    /** Environment variable for the Ominal app process selinux context. */
    public static final String ENV_OMINAL_APP__SE_PROCESS_CONTEXT = OMINAL_APP_ENV_PREFIX + "SE_PROCESS_CONTEXT";
    /** Environment variable for the Ominal app data files selinux context. */
    public static final String ENV_OMINAL_APP__SE_FILE_CONTEXT = OMINAL_APP_ENV_PREFIX + "SE_FILE_CONTEXT";
    /** Environment variable for the Ominal app seInfo tag found in selinux policy used to set app process and app data files selinux context. */
    public static final String ENV_OMINAL_APP__SE_INFO = OMINAL_APP_ENV_PREFIX + "SE_INFO";
    /** Environment variable for the Ominal app user id. */
    public static final String ENV_OMINAL_APP__USER_ID = OMINAL_APP_ENV_PREFIX + "USER_ID";
    /** Environment variable for the Ominal app profile owner. */
    public static final String ENV_OMINAL_APP__PROFILE_OWNER = OMINAL_APP_ENV_PREFIX + "PROFILE_OWNER";

    /** Environment variable for the Ominal app {@link OminalBootstrap#OMINAL_APP_PACKAGE_MANAGER}. */
    public static final String ENV_OMINAL_APP__PACKAGE_MANAGER = OMINAL_APP_ENV_PREFIX + "PACKAGE_MANAGER";
    /** Environment variable for the Ominal app {@link OminalBootstrap#OMINAL_APP_PACKAGE_VARIANT}. */
    public static final String ENV_OMINAL_APP__PACKAGE_VARIANT = OMINAL_APP_ENV_PREFIX + "PACKAGE_VARIANT";
    /** Environment variable for the Ominal app files directory. */
    public static final String ENV_OMINAL_APP__FILES_DIR = OMINAL_APP_ENV_PREFIX + "FILES_DIR";


    /** Get shell environment for Ominal app. */
    @Nullable
    public static HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext) {
        setOminalAppEnvironment(currentPackageContext);
        return ominalAppEnvironment;
    }

    /** Set Ominal app environment variables in {@link #ominalAppEnvironment}. */
    public synchronized static void setOminalAppEnvironment(@NonNull Context currentPackageContext) {
        boolean isOminalApp = OminalConstants.OMINAL_PACKAGE_NAME.equals(currentPackageContext.getPackageName());

        // If current package context is of ominal app and its environment is already set, then no need to set again since it won't change
        // Other apps should always set environment again since ominal app may be installed/updated/deleted in background
        if (ominalAppEnvironment != null && isOminalApp)
            return;

        ominalAppEnvironment = null;

        String packageName = OminalConstants.OMINAL_PACKAGE_NAME;
        PackageInfo packageInfo = PackageUtils.getPackageInfoForPackage(currentPackageContext, packageName);
        if (packageInfo == null) return;
        ApplicationInfo applicationInfo = PackageUtils.getApplicationInfoForPackage(currentPackageContext, packageName);
        if (applicationInfo == null || !applicationInfo.enabled) return;

        HashMap<String, String> environment = new HashMap<>();

        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_OMINAL_VERSION, PackageUtils.getVersionNameForPackage(packageInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_OMINAL_APP__VERSION_NAME, PackageUtils.getVersionNameForPackage(packageInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_OMINAL_APP__VERSION_CODE, String.valueOf(PackageUtils.getVersionCodeForPackage(packageInfo)));

        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_OMINAL_APP__PACKAGE_NAME, packageName);
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_OMINAL_APP__PID, OminalUtils.getOminalAppPID(currentPackageContext));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_OMINAL_APP__UID, String.valueOf(PackageUtils.getUidForPackage(applicationInfo)));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_OMINAL_APP__TARGET_SDK, String.valueOf(PackageUtils.getTargetSDKForPackage(applicationInfo)));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_OMINAL_APP__IS_DEBUGGABLE_BUILD, PackageUtils.isAppForPackageADebuggableBuild(applicationInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_OMINAL_APP__APK_PATH, PackageUtils.getBaseAPKPathForPackage(applicationInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_OMINAL_APP__IS_INSTALLED_ON_EXTERNAL_STORAGE, PackageUtils.isAppInstalledOnExternalStorage(applicationInfo));

        putOminalAPKSignature(currentPackageContext, environment);

        Context ominalPackageContext = OminalUtils.getOminalPackageContext(currentPackageContext);
        if (ominalPackageContext != null) {
            // An app that does not have the same sharedUserId as ominal app will not be able to get
            // get ominal context's classloader to get BuildConfig.OMINAL_PACKAGE_VARIANT via reflection.
            // Check OminalBootstrap.setOminalPackageManagerAndVariantFromOminalApp()
            if (OminalBootstrap.OMINAL_APP_PACKAGE_MANAGER != null)
                environment.put(ENV_OMINAL_APP__PACKAGE_MANAGER, OminalBootstrap.OMINAL_APP_PACKAGE_MANAGER.getName());
            if (OminalBootstrap.OMINAL_APP_PACKAGE_VARIANT != null)
                environment.put(ENV_OMINAL_APP__PACKAGE_VARIANT, OminalBootstrap.OMINAL_APP_PACKAGE_VARIANT.getName());

            String filesDirPath = currentPackageContext.getFilesDir().getAbsolutePath();
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_OMINAL_APP__FILES_DIR, filesDirPath);

            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_OMINAL_APP__SE_PROCESS_CONTEXT, SELinuxUtils.getContext());
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_OMINAL_APP__SE_FILE_CONTEXT, SELinuxUtils.getFileContext(filesDirPath));

            String seInfoUser = PackageUtils.getApplicationInfoSeInfoUserForPackage(applicationInfo);
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_OMINAL_APP__SE_INFO, PackageUtils.getApplicationInfoSeInfoForPackage(applicationInfo) +
                (DataUtils.isNullOrEmpty(seInfoUser) ? "" : seInfoUser));

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_OMINAL_APP__USER_ID, String.valueOf(PackageUtils.getUserIdForPackage(currentPackageContext)));
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_OMINAL_APP__PROFILE_OWNER, PackageUtils.getProfileOwnerPackageNameForUser(currentPackageContext));
        }

        ominalAppEnvironment = environment;
    }

    /** Put {@link #ENV_OMINAL_APP__APK_RELEASE} in {@code environment}. */
    public static void putOminalAPKSignature(@NonNull Context currentPackageContext,
                                             @NonNull HashMap<String, String> environment) {
        String signingCertificateSHA256Digest = PackageUtils.getSigningCertificateSHA256DigestForPackage(currentPackageContext,
            OminalConstants.OMINAL_PACKAGE_NAME);
        if (signingCertificateSHA256Digest != null) {
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_OMINAL_APP__APK_RELEASE,
                OminalUtils.getAPKRelease(signingCertificateSHA256Digest).replaceAll("[^a-zA-Z]", "_").toUpperCase());
        }
    }

}
