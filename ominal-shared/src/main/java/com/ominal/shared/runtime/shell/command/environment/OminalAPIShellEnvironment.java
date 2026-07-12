package com.ominal.shared.runtime.shell.command.environment;

import android.content.Context;
import android.content.pm.PackageInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominal.shared.android.PackageUtils;
import com.ominal.shared.shell.command.environment.ShellEnvironmentUtils;
import com.ominal.shared.runtime.OminalConstants;
import com.ominal.shared.runtime.OminalUtils;

import java.util.HashMap;

/**
 * Environment for {@link OminalConstants#OMINAL_API_PACKAGE_NAME} app.
 */
public class OminalAPIShellEnvironment {

    /** Environment variable prefix for the Ominal:API app. */
    public static final String OMINAL_API_APP_ENV_PREFIX = OminalConstants.OMINAL_ENV_PREFIX_ROOT + "_API_APP__";

    /** Environment variable for the Ominal:API app version. */
    public static final String ENV_OMINAL_API_APP__VERSION_NAME = OMINAL_API_APP_ENV_PREFIX + "VERSION_NAME";

    /** Get shell environment for Ominal:API app. */
    @Nullable
    public static HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext) {
        if (OminalUtils.isOminalAPIAppInstalled(currentPackageContext) != null) return null;

        String packageName = OminalConstants.OMINAL_API_PACKAGE_NAME;
        PackageInfo packageInfo = PackageUtils.getPackageInfoForPackage(currentPackageContext, packageName);
        if (packageInfo == null) return null;

        HashMap<String, String> environment = new HashMap<>();

        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_OMINAL_API_APP__VERSION_NAME, PackageUtils.getVersionNameForPackage(packageInfo));

        return environment;
    }

}
