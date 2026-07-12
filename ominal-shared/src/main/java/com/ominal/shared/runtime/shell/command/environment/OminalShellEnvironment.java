package com.ominal.shared.runtime.shell.command.environment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.ominal.shared.errors.Error;
import com.ominal.shared.file.FileUtils;
import com.ominal.shared.logger.Logger;
import com.ominal.shared.shell.command.ExecutionCommand;
import com.ominal.shared.shell.command.environment.AndroidShellEnvironment;
import com.ominal.shared.shell.command.environment.ShellEnvironmentUtils;
import com.ominal.shared.shell.command.environment.ShellCommandShellEnvironment;
import com.ominal.shared.runtime.OminalBootstrap;
import com.ominal.shared.runtime.OminalConstants;
import com.ominal.shared.runtime.shell.OminalShellUtils;

import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * Environment for Ominal.
 */
public class OminalShellEnvironment extends AndroidShellEnvironment {

    private static final String LOG_TAG = "OminalShellEnvironment";

    /** Environment variable for the ominal {@link OminalConstants#OMINAL_PREFIX_DIR_PATH}. */
    public static final String ENV_PREFIX = "PREFIX";

    public OminalShellEnvironment() {
        super();
        shellCommandShellEnvironment = new OminalShellCommandShellEnvironment();
    }


    /** Init {@link OminalShellEnvironment} constants and caches. */
    public synchronized static void init(@NonNull Context currentPackageContext) {
        OminalAppShellEnvironment.setOminalAppEnvironment(currentPackageContext);
    }

    /** Init {@link OminalShellEnvironment} constants and caches. */
    public synchronized static void writeEnvironmentToFile(@NonNull Context currentPackageContext) {
        HashMap<String, String> environmentMap = new OminalShellEnvironment().getEnvironment(currentPackageContext, false);
        String environmentString = ShellEnvironmentUtils.convertEnvironmentToDotEnvFile(environmentMap);

        // Write environment string to temp file and then move to final location since otherwise
        // writing may happen while file is being sourced/read
        Error error = FileUtils.writeTextToFile("ominal.env.tmp", OminalConstants.OMINAL_ENV_TEMP_FILE_PATH,
            Charset.defaultCharset(), environmentString, false);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
            return;
        }

        error = FileUtils.moveRegularFile("ominal.env.tmp", OminalConstants.OMINAL_ENV_TEMP_FILE_PATH, OminalConstants.OMINAL_ENV_FILE_PATH, true);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
        }
    }

    /** Get shell environment for Ominal. */
    @NonNull
    @Override
    public HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext, boolean isFailSafe) {

        // Ominal environment builds upon the Android environment
        HashMap<String, String> environment = super.getEnvironment(currentPackageContext, isFailSafe);

        HashMap<String, String> ominalAppEnvironment = OminalAppShellEnvironment.getEnvironment(currentPackageContext);
        if (ominalAppEnvironment != null)
            environment.putAll(ominalAppEnvironment);

        HashMap<String, String> ominalApiAppEnvironment = OminalAPIShellEnvironment.getEnvironment(currentPackageContext);
        if (ominalApiAppEnvironment != null)
            environment.putAll(ominalApiAppEnvironment);

        environment.put(ENV_HOME, OminalConstants.OMINAL_HOME_DIR_PATH);
        environment.put(ENV_PREFIX, OminalConstants.OMINAL_PREFIX_DIR_PATH);

        // If failsafe is not enabled, then we keep default PATH and TMPDIR so that system binaries can be used
        if (!isFailSafe) {
            environment.put(ENV_TMPDIR, OminalConstants.OMINAL_TMP_PREFIX_DIR_PATH);
            if (OminalBootstrap.isAppPackageVariantAPTAndroid5()) {
                // Ominal in android 5/6 era shipped busybox binaries in applets directory
                environment.put(ENV_PATH, OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH + ":" + OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH + "/applets");
                environment.put(ENV_LD_LIBRARY_PATH, OminalConstants.OMINAL_LIB_PREFIX_DIR_PATH);
            } else {
                // Ominal binaries on Android 7+ rely on DT_RUNPATH, so LD_LIBRARY_PATH should be unset by default
                environment.put(ENV_PATH, OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH);
                environment.remove(ENV_LD_LIBRARY_PATH);
            }
        }

        return environment;
    }


    @NonNull
    @Override
    public String getDefaultWorkingDirectoryPath() {
        return OminalConstants.OMINAL_HOME_DIR_PATH;
    }

    @NonNull
    @Override
    public String getDefaultBinPath() {
        return OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH;
    }

    @NonNull
    @Override
    public String[] setupShellCommandArguments(@NonNull String executable, String[] arguments) {
        return OminalShellUtils.setupShellCommandArguments(executable, arguments);
    }

}
