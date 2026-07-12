package com.ominal.shared.runtime.shell.command.environment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.ominal.shared.shell.command.ExecutionCommand;
import com.ominal.shared.shell.command.environment.ShellCommandShellEnvironment;
import com.ominal.shared.shell.command.environment.ShellEnvironmentUtils;
import com.ominal.shared.runtime.settings.preferences.OminalAppSharedPreferences;
import com.ominal.shared.runtime.shell.OminalShellManager;

import java.util.HashMap;

/**
 * Environment for Ominal {@link ExecutionCommand}.
 */
public class OminalShellCommandShellEnvironment extends ShellCommandShellEnvironment {

    /** Get shell environment containing info for Ominal {@link ExecutionCommand}. */
    @NonNull
    @Override
    public HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext,
                                                  @NonNull ExecutionCommand executionCommand) {
        HashMap<String, String> environment = super.getEnvironment(currentPackageContext, executionCommand);

        OminalAppSharedPreferences preferences = OminalAppSharedPreferences.build(currentPackageContext);
        if (preferences == null) return environment;

        if (ExecutionCommand.Runner.APP_SHELL.equalsRunner(executionCommand.runner)) {
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_SHELL_CMD__APP_SHELL_NUMBER_SINCE_BOOT,
                String.valueOf(preferences.getAndIncrementAppShellNumberSinceBoot()));
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_SHELL_CMD__APP_SHELL_NUMBER_SINCE_APP_START,
                String.valueOf(OminalShellManager.getAndIncrementAppShellNumberSinceAppStart()));

        } else if (ExecutionCommand.Runner.TERMINAL_SESSION.equalsRunner(executionCommand.runner)) {
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_SHELL_CMD__TERMINAL_SESSION_NUMBER_SINCE_BOOT,
                String.valueOf(preferences.getAndIncrementTerminalSessionNumberSinceBoot()));
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_SHELL_CMD__TERMINAL_SESSION_NUMBER_SINCE_APP_START,
                String.valueOf(OminalShellManager.getAndIncrementTerminalSessionNumberSinceAppStart()));
        } else {
            return environment;
        }

        return environment;
    }

}
