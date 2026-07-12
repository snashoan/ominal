package com.ominal.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominal.R;
import com.ominal.app.event.SystemEventReceiver;
import com.ominal.app.terminal.OminalTerminalSessionActivityClient;
import com.ominal.app.terminal.OminalTerminalSessionServiceClient;
import com.ominal.shared.runtime.plugins.OminalPluginUtils;
import com.ominal.shared.data.IntentUtils;
import com.ominal.shared.net.uri.UriUtils;
import com.ominal.shared.errors.Errno;
import com.ominal.shared.shell.ShellUtils;
import com.ominal.shared.shell.command.runner.app.AppShell;
import com.ominal.shared.runtime.settings.properties.OminalAppSharedProperties;
import com.ominal.shared.runtime.shell.command.environment.OminalShellEnvironment;
import com.ominal.shared.runtime.shell.OminalShellUtils;
import com.ominal.shared.runtime.OminalConstants;
import com.ominal.shared.runtime.OminalConstants.OMINAL_APP.OMINAL_ACTIVITY;
import com.ominal.shared.runtime.OminalConstants.OMINAL_APP.OMINAL_SERVICE;
import com.ominal.shared.runtime.settings.preferences.OminalAppSharedPreferences;
import com.ominal.shared.runtime.shell.OminalShellManager;
import com.ominal.shared.runtime.shell.command.runner.terminal.OminalSession;
import com.ominal.shared.runtime.terminal.OminalTerminalSessionClientBase;
import com.ominal.shared.logger.Logger;
import com.ominal.shared.notification.NotificationUtils;
import com.ominal.shared.android.PermissionUtils;
import com.ominal.shared.data.DataUtils;
import com.ominal.shared.shell.command.ExecutionCommand;
import com.ominal.shared.shell.command.ExecutionCommand.Runner;
import com.ominal.shared.shell.command.ExecutionCommand.ShellCreateMode;
import com.ominal.terminal.TerminalEmulator;
import com.ominal.terminal.TerminalSession;
import com.ominal.terminal.TerminalSessionClient;

import java.util.ArrayList;
import java.util.List;

/**
 * A service holding a list of {@link OminalSession} in {@link OminalShellManager#mOminalSessions} and background {@link AppShell}
 * in {@link OminalShellManager#mOminalTasks}, showing a foreground notification while running so that it is not terminated.
 * The user interacts with the session through {@link OminalActivity}, but this service may outlive
 * the activity when the user or the system disposes of the activity. In that case the user may
 * restart {@link OminalActivity} later to yet again access the sessions.
 * <p/>
 * In order to keep both terminal sessions and spawned processes (who may outlive the terminal sessions) alive as long
 * as wanted by the user this service is a foreground service, {@link Service#startForeground(int, Notification)}.
 * <p/>
 * Optionally may hold a wake and a wifi lock, in which case that is shown in the notification - see
 * {@link #buildNotification()}.
 */
public final class OminalService extends Service implements AppShell.AppShellClient, OminalSession.OminalSessionClient {

    /** This service is only bound from inside the same process and never uses IPC. */
    class LocalBinder extends Binder {
        public final OminalService service = OminalService.this;
    }

    private final IBinder mBinder = new LocalBinder();

    private final Handler mHandler = new Handler();


    /** The full implementation of the {@link TerminalSessionClient} interface to be used by {@link TerminalSession}
     * that holds activity references for activity related functions.
     * Note that the service may often outlive the activity, so need to clear this reference.
     */
    private OminalTerminalSessionActivityClient mOminalTerminalSessionActivityClient;

    /** The basic implementation of the {@link TerminalSessionClient} interface to be used by {@link TerminalSession}
     * that does not hold activity references and only a service reference.
     */
    private final OminalTerminalSessionServiceClient mOminalTerminalSessionServiceClient = new OminalTerminalSessionServiceClient(this);

    /**
     * Ominal app shared properties manager, loaded from ominal.properties
     */
    private OminalAppSharedProperties mProperties;

    /**
     * Ominal app shell manager
     */
    private OminalShellManager mShellManager;

    /** The wake lock and wifi lock are always acquired and released together. */
    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;

    /** If the user has executed the {@link OMINAL_SERVICE#ACTION_STOP_SERVICE} intent. */
    boolean mWantsToStop = false;

    private static final String LOG_TAG = "OminalService";

    @Override
    public void onCreate() {
        Logger.logVerbose(LOG_TAG, "onCreate");

        // Get Ominal app SharedProperties without loading from disk since OminalApplication handles
        // load and OminalActivity handles reloads
        mProperties = OminalAppSharedProperties.getProperties();

        mShellManager = OminalShellManager.getShellManager();

        runStartForeground();

        SystemEventReceiver.registerPackageUpdateEvents(this);
    }

    @SuppressLint("Wakelock")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.logDebug(LOG_TAG, "onStartCommand");

        // Run again in case service is already started and onCreate() is not called
        runStartForeground();

        String action = null;
        if (intent != null) {
            Logger.logVerboseExtended(LOG_TAG, "Intent Received:\n" + IntentUtils.getIntentString(intent));
            action = intent.getAction();
        }

        if (action != null) {
            switch (action) {
                case OMINAL_SERVICE.ACTION_STOP_SERVICE:
                    Logger.logDebug(LOG_TAG, "ACTION_STOP_SERVICE intent received");
                    actionStopService();
                    break;
                case OMINAL_SERVICE.ACTION_WAKE_LOCK:
                    Logger.logDebug(LOG_TAG, "ACTION_WAKE_LOCK intent received");
                    actionAcquireWakeLock();
                    break;
                case OMINAL_SERVICE.ACTION_WAKE_UNLOCK:
                    Logger.logDebug(LOG_TAG, "ACTION_WAKE_UNLOCK intent received");
                    actionReleaseWakeLock(true);
                    break;
                case OMINAL_SERVICE.ACTION_SERVICE_EXECUTE:
                    Logger.logDebug(LOG_TAG, "ACTION_SERVICE_EXECUTE intent received");
                    actionServiceExecute(intent);
                    break;
                default:
                    Logger.logError(LOG_TAG, "Invalid action: \"" + action + "\"");
                    break;
            }
        }

        // If this service really do get killed, there is no point restarting it automatically - let the user do on next
        // start of {@link Term):
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Logger.logVerbose(LOG_TAG, "onDestroy");

        OminalShellUtils.clearOminalTMPDIR(true);

        actionReleaseWakeLock(false);
        if (!mWantsToStop)
            killAllOminalExecutionCommands();

        OminalShellManager.onAppExit(this);

        SystemEventReceiver.unregisterPackageUpdateEvents(this);

        runStopForeground();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Logger.logVerbose(LOG_TAG, "onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Logger.logVerbose(LOG_TAG, "onUnbind");

        // Since we cannot rely on {@link OminalActivity.onDestroy()} to always complete,
        // we unset clients here as well if it failed, so that we do not leave service and session
        // clients with references to the activity.
        if (mOminalTerminalSessionActivityClient != null)
            unsetOminalTerminalSessionClient();
        return false;
    }

    /** Make service run in foreground mode. */
    private void runStartForeground() {
        setupNotificationChannel();
        startForeground(OminalConstants.OMINAL_APP_NOTIFICATION_ID, buildNotification());
    }

    /** Make service leave foreground mode. */
    private void runStopForeground() {
        stopForeground(true);
    }

    /** Request to stop service. */
    private void requestStopService() {
        Logger.logDebug(LOG_TAG, "Requesting to stop service");
        runStopForeground();
        stopSelf();
    }

    /** Process action to stop service. */
    private void actionStopService() {
        mWantsToStop = true;
        killAllOminalExecutionCommands();
        requestStopService();
    }

    /** Kill all OminalSessions and OminalTasks by sending SIGKILL to their processes.
     *
     * For OminalSessions, all sessions will be killed, whether user manually exited Ominal or if
     * onDestroy() was directly called because of unintended shutdown. The processing of results
     * will only be done if user manually exited ominal or if the session was started by a plugin
     * which **expects** the result back via a pending intent.
     *
     * For OminalTasks, only tasks that were started by a plugin which **expects** the result
     * back via a pending intent will be killed, whether user manually exited Ominal or if
     * onDestroy() was directly called because of unintended shutdown. The processing of results
     * will always be done for the tasks that are killed. The remaining processes will keep on
     * running until the ominal app process is killed by android, like by OOM, so we let them run
     * as long as they can.
     *
     * Some plugin execution commands may not have been processed and added to mOminalSessions and
     * mOminalTasks lists before the service is killed, so we maintain a separate
     * mPendingPluginExecutionCommands list for those, so that we can notify the pending intent
     * creators that execution was cancelled.
     *
     * Note that if user didn't manually exit Ominal and if onDestroy() was directly called because
     * of unintended shutdown, like android deciding to kill the service, then there will be no
     * guarantee that onDestroy() will be allowed to finish and ominal app process may be killed before
     * it has finished. This means that in those cases some results may not be sent back to their
     * creators for plugin commands but we still try to process whatever results can be processed
     * despite the unreliable behaviour of onDestroy().
     *
     * Note that if don't kill the processes started by plugins which **expect** the result back
     * and notify their creators that they have been killed, then they may get stuck waiting for
     * the results forever like in case of commands started by Ominal:Tasker or RUN_COMMAND intent,
     * since once OminalService has been killed, no result will be sent back. They may still get
     * stuck if ominal app process gets killed, so for this case reasonable timeout values should
     * be used, like in Tasker for the Ominal:Tasker actions.
     *
     * We make copies of each list since items are removed inside the loop.
     */
    private synchronized void killAllOminalExecutionCommands() {
        boolean processResult;

        Logger.logDebug(LOG_TAG, "Killing OminalSessions=" + mShellManager.mOminalSessions.size() +
            ", OminalTasks=" + mShellManager.mOminalTasks.size() +
            ", PendingPluginExecutionCommands=" + mShellManager.mPendingPluginExecutionCommands.size());

        List<OminalSession> ominalSessions = new ArrayList<>(mShellManager.mOminalSessions);
        List<AppShell> ominalTasks = new ArrayList<>(mShellManager.mOminalTasks);
        List<ExecutionCommand> pendingPluginExecutionCommands = new ArrayList<>(mShellManager.mPendingPluginExecutionCommands);

        for (int i = 0; i < ominalSessions.size(); i++) {
            ExecutionCommand executionCommand = ominalSessions.get(i).getExecutionCommand();
            processResult = mWantsToStop || executionCommand.isPluginExecutionCommandWithPendingResult();
            ominalSessions.get(i).killIfExecuting(this, processResult);
            if (!processResult)
                mShellManager.mOminalSessions.remove(ominalSessions.get(i));
        }


        for (int i = 0; i < ominalTasks.size(); i++) {
            ExecutionCommand executionCommand = ominalTasks.get(i).getExecutionCommand();
            if (executionCommand.isPluginExecutionCommandWithPendingResult())
                ominalTasks.get(i).killIfExecuting(this, true);
            else
                mShellManager.mOminalTasks.remove(ominalTasks.get(i));
        }

        for (int i = 0; i < pendingPluginExecutionCommands.size(); i++) {
            ExecutionCommand executionCommand = pendingPluginExecutionCommands.get(i);
            if (!executionCommand.shouldNotProcessResults() && executionCommand.isPluginExecutionCommandWithPendingResult()) {
                if (executionCommand.setStateFailed(Errno.ERRNO_CANCELLED.getCode(), this.getString(com.ominal.shared.R.string.error_execution_cancelled))) {
                    OminalPluginUtils.processPluginExecutionCommandResult(this, LOG_TAG, executionCommand);
                }
            }
        }
    }



    /** Process action to acquire Power and Wi-Fi WakeLocks. */
    @SuppressLint({"WakelockTimeout", "BatteryLife"})
    private void actionAcquireWakeLock() {
        if (mWakeLock != null) {
            Logger.logDebug(LOG_TAG, "Ignoring acquiring WakeLocks since they are already held");
            return;
        }

        Logger.logDebug(LOG_TAG, "Acquiring WakeLocks");

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, OminalConstants.OMINAL_APP_NAME.toLowerCase() + ":service-wakelock");
        mWakeLock.acquire();

        // http://tools.android.com/tech-docs/lint-in-studio-2-3#TOC-WifiManager-Leak
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, OminalConstants.OMINAL_APP_NAME.toLowerCase());
        mWifiLock.acquire();

        if (!PermissionUtils.checkIfBatteryOptimizationsDisabled(this)) {
            PermissionUtils.requestDisableBatteryOptimizations(this);
        }

        updateNotification();

        Logger.logDebug(LOG_TAG, "WakeLocks acquired successfully");

    }

    /** Process action to release Power and Wi-Fi WakeLocks. */
    private void actionReleaseWakeLock(boolean updateNotification) {
        if (mWakeLock == null && mWifiLock == null) {
            Logger.logDebug(LOG_TAG, "Ignoring releasing WakeLocks since none are already held");
            return;
        }

        Logger.logDebug(LOG_TAG, "Releasing WakeLocks");

        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }

        if (mWifiLock != null) {
            mWifiLock.release();
            mWifiLock = null;
        }

        if (updateNotification)
            updateNotification();

        Logger.logDebug(LOG_TAG, "WakeLocks released successfully");
    }

    /** Process {@link OMINAL_SERVICE#ACTION_SERVICE_EXECUTE} intent to execute a shell command in
     * a foreground OminalSession or in a background OminalTask. */
    private void actionServiceExecute(Intent intent) {
        if (intent == null) {
            Logger.logError(LOG_TAG, "Ignoring null intent to actionServiceExecute");
            return;
        }

        ExecutionCommand executionCommand = new ExecutionCommand(OminalShellManager.getNextShellId());

        executionCommand.executableUri = intent.getData();
        executionCommand.isPluginExecutionCommand = true;

        // If EXTRA_RUNNER is passed, use that, otherwise check EXTRA_BACKGROUND and default to Runner.TERMINAL_SESSION
        executionCommand.runner = IntentUtils.getStringExtraIfSet(intent, OMINAL_SERVICE.EXTRA_RUNNER,
            (intent.getBooleanExtra(OMINAL_SERVICE.EXTRA_BACKGROUND, false) ? Runner.APP_SHELL.getName() : Runner.TERMINAL_SESSION.getName()));
        if (Runner.runnerOf(executionCommand.runner) == null) {
            String errmsg = this.getString(R.string.error_ominal_service_invalid_execution_command_runner, executionCommand.runner);
            executionCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), errmsg);
            OminalPluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
            return;
        }

        if (executionCommand.executableUri != null) {
            Logger.logVerbose(LOG_TAG, "uri: \"" + executionCommand.executableUri + "\", path: \"" + executionCommand.executableUri.getPath() + "\", fragment: \"" + executionCommand.executableUri.getFragment() + "\"");

            // Get full path including fragment (anything after last "#")
            executionCommand.executable = UriUtils.getUriFilePathWithFragment(executionCommand.executableUri);
            executionCommand.arguments = IntentUtils.getStringArrayExtraIfSet(intent, OMINAL_SERVICE.EXTRA_ARGUMENTS, null);
            if (Runner.APP_SHELL.equalsRunner(executionCommand.runner))
                executionCommand.stdin = IntentUtils.getStringExtraIfSet(intent, OMINAL_SERVICE.EXTRA_STDIN, null);
            executionCommand.backgroundCustomLogLevel = IntentUtils.getIntegerExtraIfSet(intent, OMINAL_SERVICE.EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL, null);
        }

        executionCommand.workingDirectory = IntentUtils.getStringExtraIfSet(intent, OMINAL_SERVICE.EXTRA_WORKDIR, null);
        executionCommand.isFailsafe = intent.getBooleanExtra(OMINAL_ACTIVITY.EXTRA_FAILSAFE_SESSION, false);
        executionCommand.sessionAction = intent.getStringExtra(OMINAL_SERVICE.EXTRA_SESSION_ACTION);
        executionCommand.shellName = IntentUtils.getStringExtraIfSet(intent, OMINAL_SERVICE.EXTRA_SHELL_NAME, null);
        executionCommand.shellCreateMode = IntentUtils.getStringExtraIfSet(intent, OMINAL_SERVICE.EXTRA_SHELL_CREATE_MODE, null);
        executionCommand.commandLabel = IntentUtils.getStringExtraIfSet(intent, OMINAL_SERVICE.EXTRA_COMMAND_LABEL, "Execution Intent Command");
        executionCommand.commandDescription = IntentUtils.getStringExtraIfSet(intent, OMINAL_SERVICE.EXTRA_COMMAND_DESCRIPTION, null);
        executionCommand.commandHelp = IntentUtils.getStringExtraIfSet(intent, OMINAL_SERVICE.EXTRA_COMMAND_HELP, null);
        executionCommand.pluginAPIHelp = IntentUtils.getStringExtraIfSet(intent, OMINAL_SERVICE.EXTRA_PLUGIN_API_HELP, null);
        executionCommand.resultConfig.resultPendingIntent = intent.getParcelableExtra(OMINAL_SERVICE.EXTRA_PENDING_INTENT);
        executionCommand.resultConfig.resultDirectoryPath = IntentUtils.getStringExtraIfSet(intent, OMINAL_SERVICE.EXTRA_RESULT_DIRECTORY, null);
        if (executionCommand.resultConfig.resultDirectoryPath != null) {
            executionCommand.resultConfig.resultSingleFile = intent.getBooleanExtra(OMINAL_SERVICE.EXTRA_RESULT_SINGLE_FILE, false);
            executionCommand.resultConfig.resultFileBasename = IntentUtils.getStringExtraIfSet(intent, OMINAL_SERVICE.EXTRA_RESULT_FILE_BASENAME, null);
            executionCommand.resultConfig.resultFileOutputFormat = IntentUtils.getStringExtraIfSet(intent, OMINAL_SERVICE.EXTRA_RESULT_FILE_OUTPUT_FORMAT, null);
            executionCommand.resultConfig.resultFileErrorFormat = IntentUtils.getStringExtraIfSet(intent, OMINAL_SERVICE.EXTRA_RESULT_FILE_ERROR_FORMAT, null);
            executionCommand.resultConfig.resultFilesSuffix = IntentUtils.getStringExtraIfSet(intent, OMINAL_SERVICE.EXTRA_RESULT_FILES_SUFFIX, null);
        }

        if (executionCommand.shellCreateMode == null)
            executionCommand.shellCreateMode = ShellCreateMode.ALWAYS.getMode();

        // Add the execution command to pending plugin execution commands list
        mShellManager.mPendingPluginExecutionCommands.add(executionCommand);

        if (Runner.APP_SHELL.equalsRunner(executionCommand.runner))
            executeOminalTaskCommand(executionCommand);
        else if (Runner.TERMINAL_SESSION.equalsRunner(executionCommand.runner))
            executeOminalSessionCommand(executionCommand);
        else {
            String errmsg = getString(R.string.error_ominal_service_unsupported_execution_command_runner, executionCommand.runner);
            executionCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), errmsg);
            OminalPluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
        }
    }





    /** Execute a shell command in background OminalTask. */
    private void executeOminalTaskCommand(ExecutionCommand executionCommand) {
        if (executionCommand == null) return;

        Logger.logDebug(LOG_TAG, "Executing background \"" + executionCommand.getCommandIdAndLabelLogString() + "\" OminalTask command");

        // Transform executable path to shell/session name, e.g. "/bin/do-something.sh" => "do-something.sh".
        if (executionCommand.shellName == null && executionCommand.executable != null)
            executionCommand.shellName = ShellUtils.getExecutableBasename(executionCommand.executable);

        AppShell newOminalTask = null;
        ShellCreateMode shellCreateMode = processShellCreateMode(executionCommand);
        if (shellCreateMode == null) return;
        if (ShellCreateMode.NO_SHELL_WITH_NAME.equals(shellCreateMode)) {
            newOminalTask = getOminalTaskForShellName(executionCommand.shellName);
            if (newOminalTask != null)
                Logger.logVerbose(LOG_TAG, "Existing OminalTask with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
            else
                Logger.logVerbose(LOG_TAG, "No existing OminalTask with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
        }

        if (newOminalTask == null)
            newOminalTask = createOminalTask(executionCommand);
    }

    /** Create a OminalTask. */
    @Nullable
    public AppShell createOminalTask(String executablePath, String[] arguments, String stdin, String workingDirectory) {
        return createOminalTask(new ExecutionCommand(OminalShellManager.getNextShellId(), executablePath,
            arguments, stdin, workingDirectory, Runner.APP_SHELL.getName(), false));
    }

    /** Create a OminalTask. */
    @Nullable
    public synchronized AppShell createOminalTask(ExecutionCommand executionCommand) {
        if (executionCommand == null) return null;

        Logger.logDebug(LOG_TAG, "Creating \"" + executionCommand.getCommandIdAndLabelLogString() + "\" OminalTask");

        if (!Runner.APP_SHELL.equalsRunner(executionCommand.runner)) {
            Logger.logDebug(LOG_TAG, "Ignoring wrong runner \"" + executionCommand.runner + "\" command passed to createOminalTask()");
            return null;
        }

        executionCommand.setShellCommandShellEnvironment = true;

        if (Logger.getLogLevel() >= Logger.LOG_LEVEL_VERBOSE)
            Logger.logVerboseExtended(LOG_TAG, executionCommand.toString());

        AppShell newOminalTask = AppShell.execute(this, executionCommand, this,
            new OminalShellEnvironment(), null,false);
        if (newOminalTask == null) {
            Logger.logError(LOG_TAG, "Failed to execute new OminalTask command for:\n" + executionCommand.getCommandIdAndLabelLogString());
            // If the execution command was started for a plugin, then process the error
            if (executionCommand.isPluginExecutionCommand)
                OminalPluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
            else {
                Logger.logError(LOG_TAG, "Set log level to debug or higher to see error in logs");
                Logger.logErrorPrivateExtended(LOG_TAG, executionCommand.toString());
            }
            return null;
        }

        mShellManager.mOminalTasks.add(newOminalTask);

        // Remove the execution command from the pending plugin execution commands list since it has
        // now been processed
        if (executionCommand.isPluginExecutionCommand)
            mShellManager.mPendingPluginExecutionCommands.remove(executionCommand);

        updateNotification();

        return newOminalTask;
    }

    /** Callback received when a OminalTask finishes. */
    @Override
    public void onAppShellExited(final AppShell ominalTask) {
        mHandler.post(() -> {
            if (ominalTask != null) {
                ExecutionCommand executionCommand = ominalTask.getExecutionCommand();

                Logger.logVerbose(LOG_TAG, "The onOminalTaskExited() callback called for \"" + executionCommand.getCommandIdAndLabelLogString() + "\" OminalTask command");

                // If the execution command was started for a plugin, then process the results
                if (executionCommand != null && executionCommand.isPluginExecutionCommand)
                    OminalPluginUtils.processPluginExecutionCommandResult(this, LOG_TAG, executionCommand);

                mShellManager.mOminalTasks.remove(ominalTask);
            }

            updateNotification();
        });
    }





    /** Execute a shell command in a foreground {@link OminalSession}. */
    private void executeOminalSessionCommand(ExecutionCommand executionCommand) {
        if (executionCommand == null) return;

        Logger.logDebug(LOG_TAG, "Executing foreground \"" + executionCommand.getCommandIdAndLabelLogString() + "\" OminalSession command");

        // Transform executable path to shell/session name, e.g. "/bin/do-something.sh" => "do-something.sh".
        if (executionCommand.shellName == null && executionCommand.executable != null)
            executionCommand.shellName = ShellUtils.getExecutableBasename(executionCommand.executable);

        OminalSession newOminalSession = null;
        ShellCreateMode shellCreateMode = processShellCreateMode(executionCommand);
        if (shellCreateMode == null) return;
        if (ShellCreateMode.NO_SHELL_WITH_NAME.equals(shellCreateMode)) {
            newOminalSession = getOminalSessionForShellName(executionCommand.shellName);
            if (newOminalSession != null)
                Logger.logVerbose(LOG_TAG, "Existing OminalSession with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
            else
                Logger.logVerbose(LOG_TAG, "No existing OminalSession with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
        }

        if (newOminalSession == null)
            newOminalSession = createOminalSession(executionCommand);
        if (newOminalSession == null) return;

        handleSessionAction(DataUtils.getIntFromString(executionCommand.sessionAction,
            OMINAL_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY),
            newOminalSession.getTerminalSession());
    }

    /**
     * Create a {@link OminalSession}.
     * Currently called by {@link OminalTerminalSessionActivityClient#addNewSession(boolean, String)} to add a new {@link OminalSession}.
     */
    @Nullable
    public OminalSession createOminalSession(String executablePath, String[] arguments, String stdin,
                                             String workingDirectory, boolean isFailSafe, String sessionName) {
        ExecutionCommand executionCommand = new ExecutionCommand(OminalShellManager.getNextShellId(),
            executablePath, arguments, stdin, workingDirectory, Runner.TERMINAL_SESSION.getName(), isFailSafe);
        executionCommand.shellName = sessionName;
        return createOminalSession(executionCommand);
    }

    /** Create a {@link OminalSession}. */
    @Nullable
    public synchronized OminalSession createOminalSession(ExecutionCommand executionCommand) {
        if (executionCommand == null) return null;

        Logger.logDebug(LOG_TAG, "Creating \"" + executionCommand.getCommandIdAndLabelLogString() + "\" OminalSession");

        if (!Runner.TERMINAL_SESSION.equalsRunner(executionCommand.runner)) {
            Logger.logDebug(LOG_TAG, "Ignoring wrong runner \"" + executionCommand.runner + "\" command passed to createOminalSession()");
            return null;
        }

        executionCommand.setShellCommandShellEnvironment = true;
        executionCommand.terminalTranscriptRows = mProperties.getTerminalTranscriptRows();

        if (Logger.getLogLevel() >= Logger.LOG_LEVEL_VERBOSE)
            Logger.logVerboseExtended(LOG_TAG, executionCommand.toString());

        // If the execution command was started for a plugin, only then will the stdout be set
        // Otherwise if command was manually started by the user like by adding a new terminal session,
        // then no need to set stdout
        OminalSession newOminalSession = OminalSession.execute(this, executionCommand, getOminalTerminalSessionClient(),
            this, new OminalShellEnvironment(), null, executionCommand.isPluginExecutionCommand);
        if (newOminalSession == null) {
            Logger.logError(LOG_TAG, "Failed to execute new OminalSession command for:\n" + executionCommand.getCommandIdAndLabelLogString());
            // If the execution command was started for a plugin, then process the error
            if (executionCommand.isPluginExecutionCommand)
                OminalPluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
            else {
                Logger.logError(LOG_TAG, "Set log level to debug or higher to see error in logs");
                Logger.logErrorPrivateExtended(LOG_TAG, executionCommand.toString());
            }
            return null;
        }

        mShellManager.mOminalSessions.add(newOminalSession);

        // Remove the execution command from the pending plugin execution commands list since it has
        // now been processed
        if (executionCommand.isPluginExecutionCommand)
            mShellManager.mPendingPluginExecutionCommands.remove(executionCommand);

        // Notify {@link OminalSessionsListViewController} that sessions list has been updated if
        // activity in is foreground
        if (mOminalTerminalSessionActivityClient != null)
            mOminalTerminalSessionActivityClient.ominalSessionListNotifyUpdated();

        updateNotification();

        // No need to recreate the activity since it likely just started and theme should already have applied
        OminalActivity.updateOminalActivityStyling(this, false);

        return newOminalSession;
    }

    /** Remove a OminalSession. */
    public synchronized int removeOminalSession(TerminalSession sessionToRemove) {
        int index = getIndexOfSession(sessionToRemove);

        if (index >= 0)
            mShellManager.mOminalSessions.get(index).finish();

        return index;
    }

    /** Callback received when a {@link OminalSession} finishes. */
    @Override
    public void onOminalSessionExited(final OminalSession ominalSession) {
        if (ominalSession != null) {
            ExecutionCommand executionCommand = ominalSession.getExecutionCommand();

            Logger.logVerbose(LOG_TAG, "The onOminalSessionExited() callback called for \"" + executionCommand.getCommandIdAndLabelLogString() + "\" OminalSession command");

            // If the execution command was started for a plugin, then process the results
            if (executionCommand != null && executionCommand.isPluginExecutionCommand)
                OminalPluginUtils.processPluginExecutionCommandResult(this, LOG_TAG, executionCommand);

            mShellManager.mOminalSessions.remove(ominalSession);

            // Notify {@link OminalSessionsListViewController} that sessions list has been updated if
            // activity in is foreground
            if (mOminalTerminalSessionActivityClient != null)
                mOminalTerminalSessionActivityClient.ominalSessionListNotifyUpdated();
        }

        updateNotification();
    }





    private ShellCreateMode processShellCreateMode(@NonNull ExecutionCommand executionCommand) {
        if (ShellCreateMode.ALWAYS.equalsMode(executionCommand.shellCreateMode))
            return ShellCreateMode.ALWAYS; // Default
        else if (ShellCreateMode.NO_SHELL_WITH_NAME.equalsMode(executionCommand.shellCreateMode))
            if (DataUtils.isNullOrEmpty(executionCommand.shellName)) {
                OminalPluginUtils.setAndProcessPluginExecutionCommandError(this, LOG_TAG, executionCommand, false,
                    getString(R.string.error_ominal_service_execution_command_shell_name_unset, executionCommand.shellCreateMode));
                return null;
            } else {
               return ShellCreateMode.NO_SHELL_WITH_NAME;
            }
        else {
            OminalPluginUtils.setAndProcessPluginExecutionCommandError(this, LOG_TAG, executionCommand, false,
                getString(R.string.error_ominal_service_unsupported_execution_command_shell_create_mode, executionCommand.shellCreateMode));
            return null;
        }
    }

    /** Process session action for new session. */
    private void handleSessionAction(int sessionAction, TerminalSession newTerminalSession) {
        Logger.logDebug(LOG_TAG, "Processing sessionAction \"" + sessionAction + "\" for session \"" + newTerminalSession.mSessionName + "\"");

        switch (sessionAction) {
            case OMINAL_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY:
                setCurrentStoredTerminalSession(newTerminalSession);
                if (mOminalTerminalSessionActivityClient != null)
                    mOminalTerminalSessionActivityClient.setCurrentSession(newTerminalSession);
                startOminalActivity();
                break;
            case OMINAL_SERVICE.VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY:
                if (getOminalSessionsSize() == 1)
                    setCurrentStoredTerminalSession(newTerminalSession);
                startOminalActivity();
                break;
            case OMINAL_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY:
                setCurrentStoredTerminalSession(newTerminalSession);
                if (mOminalTerminalSessionActivityClient != null)
                    mOminalTerminalSessionActivityClient.setCurrentSession(newTerminalSession);
                break;
            case OMINAL_SERVICE.VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY:
                if (getOminalSessionsSize() == 1)
                    setCurrentStoredTerminalSession(newTerminalSession);
                break;
            default:
                Logger.logError(LOG_TAG, "Invalid sessionAction: \"" + sessionAction + "\". Force using default sessionAction.");
                handleSessionAction(OMINAL_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY, newTerminalSession);
                break;
        }
    }

    /** Launch the {@link }OminalActivity} to bring it to foreground. */
    private void startOminalActivity() {
        // For android >= 10, apps require Display over other apps permission to start foreground activities
        // from background (services). If it is not granted, then OminalSessions that are started will
        // show in Ominal notification but will not run until user manually clicks the notification.
        if (PermissionUtils.validateDisplayOverOtherAppsPermissionForPostAndroid10(this, true)) {
            OminalActivity.startOminalActivity(this);
        } else {
            OminalAppSharedPreferences preferences = OminalAppSharedPreferences.build(this);
            if (preferences == null) return;
            if (preferences.arePluginErrorNotificationsEnabled(false))
                Logger.showToast(this, this.getString(R.string.error_display_over_other_apps_permission_not_granted_to_start_terminal), true);
        }
    }





    /** If {@link OminalActivity} has not bound to the {@link OminalService} yet or is destroyed, then
     * interface functions requiring the activity should not be available to the terminal sessions,
     * so we just return the {@link #mOminalTerminalSessionServiceClient}. Once {@link OminalActivity} bind
     * callback is received, it should call {@link #setOminalTerminalSessionClient} to set the
     * {@link OminalService#mOminalTerminalSessionActivityClient} so that further terminal sessions are directly
     * passed the {@link OminalTerminalSessionActivityClient} object which fully implements the
     * {@link TerminalSessionClient} interface.
     *
     * @return Returns the {@link OminalTerminalSessionActivityClient} if {@link OminalActivity} has bound with
     * {@link OminalService}, otherwise {@link OminalTerminalSessionServiceClient}.
     */
    public synchronized OminalTerminalSessionClientBase getOminalTerminalSessionClient() {
        if (mOminalTerminalSessionActivityClient != null)
            return mOminalTerminalSessionActivityClient;
        else
            return mOminalTerminalSessionServiceClient;
    }

    /** This should be called when {@link OminalActivity#onServiceConnected} is called to set the
     * {@link OminalService#mOminalTerminalSessionActivityClient} variable and update the {@link TerminalSession}
     * and {@link TerminalEmulator} clients in case they were passed {@link OminalTerminalSessionServiceClient}
     * earlier.
     *
     * @param ominalTerminalSessionActivityClient The {@link OminalTerminalSessionActivityClient} object that fully
     * implements the {@link TerminalSessionClient} interface.
     */
    public synchronized void setOminalTerminalSessionClient(OminalTerminalSessionActivityClient ominalTerminalSessionActivityClient) {
        mOminalTerminalSessionActivityClient = ominalTerminalSessionActivityClient;

        for (int i = 0; i < mShellManager.mOminalSessions.size(); i++)
            mShellManager.mOminalSessions.get(i).getTerminalSession().updateTerminalSessionClient(mOminalTerminalSessionActivityClient);
    }

    /** This should be called when {@link OminalActivity} has been destroyed and in {@link #onUnbind(Intent)}
     * so that the {@link OminalService} and {@link TerminalSession} and {@link TerminalEmulator}
     * clients do not hold an activity references.
     */
    public synchronized void unsetOminalTerminalSessionClient() {
        for (int i = 0; i < mShellManager.mOminalSessions.size(); i++)
            mShellManager.mOminalSessions.get(i).getTerminalSession().updateTerminalSessionClient(mOminalTerminalSessionServiceClient);

        mOminalTerminalSessionActivityClient = null;
    }





    private Notification buildNotification() {
        Resources res = getResources();

        // Set pending intent to be launched when notification is clicked
        Intent notificationIntent = OminalActivity.newInstance(this);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);


        // Set notification text
        int sessionCount = getOminalSessionsSize();
        int taskCount = mShellManager.mOminalTasks.size();
        String notificationText = sessionCount + " session" + (sessionCount == 1 ? "" : "s");
        if (taskCount > 0) {
            notificationText += ", " + taskCount + " task" + (taskCount == 1 ? "" : "s");
        }

        final boolean wakeLockHeld = mWakeLock != null;
        if (wakeLockHeld) notificationText += " (wake lock held)";


        // Set notification priority
        // If holding a wake or wifi lock consider the notification of high priority since it's using power,
        // otherwise use a low priority
        int priority = (wakeLockHeld) ? Notification.PRIORITY_HIGH : Notification.PRIORITY_LOW;


        // Build the notification
        Notification.Builder builder =  NotificationUtils.geNotificationBuilder(this,
            OminalConstants.OMINAL_APP_NOTIFICATION_CHANNEL_ID, priority,
            OminalConstants.OMINAL_APP_NAME, notificationText, null,
            contentIntent, null, NotificationUtils.NOTIFICATION_MODE_SILENT);
        if (builder == null)  return null;

        // No need to show a timestamp:
        builder.setShowWhen(false);

        // Set notification icon
        builder.setSmallIcon(R.drawable.ic_service_notification);

        // Set background color for small notification icon
        builder.setColor(0xFF607D8B);

        // OminalSessions are always ongoing
        builder.setOngoing(true);


        // Set Exit button action
        Intent exitIntent = new Intent(this, OminalService.class).setAction(OMINAL_SERVICE.ACTION_STOP_SERVICE);
        builder.addAction(android.R.drawable.ic_delete, res.getString(R.string.notification_action_exit), PendingIntent.getService(this, 0, exitIntent, 0));


        // Set Wakelock button actions
        String newWakeAction = wakeLockHeld ? OMINAL_SERVICE.ACTION_WAKE_UNLOCK : OMINAL_SERVICE.ACTION_WAKE_LOCK;
        Intent toggleWakeLockIntent = new Intent(this, OminalService.class).setAction(newWakeAction);
        String actionTitle = res.getString(wakeLockHeld ? R.string.notification_action_wake_unlock : R.string.notification_action_wake_lock);
        int actionIcon = wakeLockHeld ? android.R.drawable.ic_lock_idle_lock : android.R.drawable.ic_lock_lock;
        builder.addAction(actionIcon, actionTitle, PendingIntent.getService(this, 0, toggleWakeLockIntent, 0));


        return builder.build();
    }

    private void setupNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationUtils.setupNotificationChannel(this, OminalConstants.OMINAL_APP_NOTIFICATION_CHANNEL_ID,
            OminalConstants.OMINAL_APP_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
    }

    /** Update the shown foreground service notification after making any changes that affect it. */
    private synchronized void updateNotification() {
        if (mWakeLock == null && mShellManager.mOminalSessions.isEmpty() && mShellManager.mOminalTasks.isEmpty()) {
            // Exit if we are updating after the user disabled all locks with no sessions or tasks running.
            requestStopService();
        } else {
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(OminalConstants.OMINAL_APP_NOTIFICATION_ID, buildNotification());
        }
    }





    private void setCurrentStoredTerminalSession(TerminalSession terminalSession) {
        if (terminalSession == null) return;
        // Make the newly created session the current one to be displayed
        OminalAppSharedPreferences preferences = OminalAppSharedPreferences.build(this);
        if (preferences == null) return;
        preferences.setCurrentSession(terminalSession.mHandle);
    }

    public synchronized boolean isOminalSessionsEmpty() {
        return mShellManager.mOminalSessions.isEmpty();
    }

    public synchronized int getOminalSessionsSize() {
        return mShellManager.mOminalSessions.size();
    }

    public synchronized List<OminalSession> getOminalSessions() {
        return mShellManager.mOminalSessions;
    }

    @Nullable
    public synchronized OminalSession getOminalSession(int index) {
        if (index >= 0 && index < mShellManager.mOminalSessions.size())
            return mShellManager.mOminalSessions.get(index);
        else
            return null;
    }

    @Nullable
    public synchronized OminalSession getOminalSessionForTerminalSession(TerminalSession terminalSession) {
        if (terminalSession == null) return null;

        for (int i = 0; i < mShellManager.mOminalSessions.size(); i++) {
            if (mShellManager.mOminalSessions.get(i).getTerminalSession().equals(terminalSession))
                return mShellManager.mOminalSessions.get(i);
        }

        return null;
    }

    public synchronized OminalSession getLastOminalSession() {
        return mShellManager.mOminalSessions.isEmpty() ? null : mShellManager.mOminalSessions.get(mShellManager.mOminalSessions.size() - 1);
    }

    public synchronized int getIndexOfSession(TerminalSession terminalSession) {
        if (terminalSession == null) return -1;

        for (int i = 0; i < mShellManager.mOminalSessions.size(); i++) {
            if (mShellManager.mOminalSessions.get(i).getTerminalSession().equals(terminalSession))
                return i;
        }
        return -1;
    }

    public synchronized TerminalSession getTerminalSessionForHandle(String sessionHandle) {
        TerminalSession terminalSession;
        for (int i = 0, len = mShellManager.mOminalSessions.size(); i < len; i++) {
            terminalSession = mShellManager.mOminalSessions.get(i).getTerminalSession();
            if (terminalSession.mHandle.equals(sessionHandle))
                return terminalSession;
        }
        return null;
    }

    public synchronized AppShell getOminalTaskForShellName(String name) {
        if (DataUtils.isNullOrEmpty(name)) return null;
        AppShell appShell;
        for (int i = 0, len = mShellManager.mOminalTasks.size(); i < len; i++) {
            appShell = mShellManager.mOminalTasks.get(i);
            String shellName = appShell.getExecutionCommand().shellName;
            if (shellName != null && shellName.equals(name))
                return appShell;
        }
        return null;
    }

    public synchronized OminalSession getOminalSessionForShellName(String name) {
        if (DataUtils.isNullOrEmpty(name)) return null;
        OminalSession ominalSession;
        for (int i = 0, len = mShellManager.mOminalSessions.size(); i < len; i++) {
            ominalSession = mShellManager.mOminalSessions.get(i);
            String shellName = ominalSession.getExecutionCommand().shellName;
            if (shellName != null && shellName.equals(name))
                return ominalSession;
        }
        return null;
    }



    public boolean wantsToStop() {
        return mWantsToStop;
    }

}
