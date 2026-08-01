package com.ominal.shared.runtime.shell.command.environment;

import android.content.Context;
import android.os.Build;

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
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Environment for Ominal.
 */
public class OminalShellEnvironment extends AndroidShellEnvironment {

    private static final String LOG_TAG = "OminalShellEnvironment";
    private static final String SYSTEM_LINKER_64_PATH = "/system/bin/linker64";
    private static final String ENV_ANDROID_BUILD_VERSION_SDK = "ANDROID__BUILD_VERSION_SDK";
    private static final String ENV_BOOTSTRAP_ROOTFS = "TERMUX__ROOTFS";
    private static final String ENV_BOOTSTRAP_HOME = "TERMUX__HOME";
    private static final String ENV_BOOTSTRAP_PREFIX = "TERMUX__PREFIX";
    private static final String ENV_BOOTSTRAP_APP_DATA_DIR = "TERMUX_APP__DATA_DIR";
    private static final String ENV_BOOTSTRAP_SE_PROCESS_CONTEXT = "TERMUX__SE_PROCESS_CONTEXT";
    private static final String ENV_BOOTSTRAP_APP_SE_PROCESS_CONTEXT = "TERMUX_APP__SE_PROCESS_CONTEXT";
    private static final String ENV_LD_PRELOAD = "LD_PRELOAD";
    private static final String ENV_PROOT_LOADER = "PROOT_LOADER";
    private static final String NATIVE_PROOT_LOADER_NAME = "libominal-proot-loader.so";

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
        addBootstrapExecutionEnvironment(environment);
        File nativeProotLoader = new File(currentPackageContext.getApplicationInfo().nativeLibraryDir,
            NATIVE_PROOT_LOADER_NAME);
        if (nativeProotLoader.isFile())
            environment.put(ENV_PROOT_LOADER, nativeProotLoader.getAbsolutePath());

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
        String[] commandArguments = OminalShellUtils.setupShellCommandArguments(executable, arguments);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || commandArguments.length == 0
            || !commandArguments[0].startsWith(OminalConstants.OMINAL_FILES_DIR_PATH + "/")) {
            return commandArguments;
        }

        // Android 10+ prevents an app that targets API 29+ from exec-ing a file in its
        // private data directory. Starting the system linker is permitted; the bootstrap
        // execution library then handles later exec calls inside the child process.
        ArrayList<String> linkerArguments = new ArrayList<>(commandArguments.length + 1);
        linkerArguments.add(SYSTEM_LINKER_64_PATH);
        for (String commandArgument : commandArguments) linkerArguments.add(commandArgument);
        return linkerArguments.toArray(new String[0]);
    }

    private static void addBootstrapExecutionEnvironment(@NonNull HashMap<String, String> environment) {
        // The bootstrap currently contains termux-exec v2. Its private compatibility
        // variables let the binary run from Ominal's rebranded rootfs until the package
        // repository is independently rebuilt.
        environment.put(ENV_ANDROID_BUILD_VERSION_SDK, String.valueOf(Build.VERSION.SDK_INT));
        environment.put(ENV_BOOTSTRAP_ROOTFS, OminalConstants.OMINAL_FILES_DIR_PATH);
        environment.put(ENV_BOOTSTRAP_HOME, OminalConstants.OMINAL_HOME_DIR_PATH);
        environment.put(ENV_BOOTSTRAP_PREFIX, OminalConstants.OMINAL_PREFIX_DIR_PATH);
        environment.put(ENV_BOOTSTRAP_APP_DATA_DIR, OminalConstants.OMINAL_INTERNAL_PRIVATE_APP_DATA_DIR_PATH);

        String processContext = environment.get(OminalAppShellEnvironment.ENV_OMINAL_APP__SE_PROCESS_CONTEXT);
        if (processContext != null && !processContext.isEmpty()) {
            environment.put(ENV_BOOTSTRAP_SE_PROCESS_CONTEXT, processContext);
            environment.put(ENV_BOOTSTRAP_APP_SE_PROCESS_CONTEXT, processContext);
        }

        File preloadLibrary = new File(OminalConstants.OMINAL_LIB_PREFIX_DIR_PATH,
            "libtermux-exec-ld-preload.so");
        if (preloadLibrary.isFile()) environment.put(ENV_LD_PRELOAD, preloadLibrary.getAbsolutePath());
    }

}
