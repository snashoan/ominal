package com.ominal.shared.runtime.crash;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominal.shared.activities.ReportActivity;
import com.ominal.shared.android.AndroidUtils;
import com.ominal.shared.crash.CrashHandler;
import com.ominal.shared.data.DataUtils;
import com.ominal.shared.errors.Error;
import com.ominal.shared.file.FileUtils;
import com.ominal.shared.logger.Logger;
import com.ominal.shared.markdown.MarkdownUtils;
import com.ominal.shared.models.ReportInfo;
import com.ominal.shared.notification.NotificationUtils;
import com.ominal.shared.runtime.OminalConstants;
import com.ominal.shared.runtime.OminalConstants.OMINAL_APP;
import com.ominal.shared.runtime.OminalUtils;
import com.ominal.shared.runtime.models.UserAction;
import com.ominal.shared.runtime.notification.OminalNotificationUtils;
import com.ominal.shared.runtime.settings.preferences.OminalAppSharedPreferences;
import com.ominal.shared.runtime.settings.preferences.OminalPreferenceConstants;

import java.nio.charset.Charset;

public class OminalCrashUtils implements CrashHandler.CrashHandlerClient {

    public enum TYPE {
        UNCAUGHT_EXCEPTION,
        CAUGHT_EXCEPTION;
    }

    private final TYPE mType;

    private static final String LOG_TAG = "OminalCrashUtils";

    OminalCrashUtils(TYPE type) {
        mType = type;
    }

    /**
     * Set default uncaught crash handler of the app to {@link CrashHandler} for Ominal app
     * and its plugins to log crashes at {@link OminalConstants#OMINAL_CRASH_LOG_FILE_PATH}.
     */
    public static void setDefaultCrashHandler(@NonNull final Context context) {
        CrashHandler.setDefaultCrashHandler(context, new OminalCrashUtils(TYPE.UNCAUGHT_EXCEPTION));
    }

    /**
     * Set uncaught crash handler of current non-main thread to {@link CrashHandler} for Ominal app
     * and its plugins to log crashes at {@link OminalConstants#OMINAL_CRASH_LOG_FILE_PATH}.
     */
    public static void setCrashHandler(@NonNull final Context context) {
        CrashHandler.setCrashHandler(context, new OminalCrashUtils(TYPE.CAUGHT_EXCEPTION));
    }

    /**
     * Get {@link CrashHandler} for Ominal app and its plugins that can be set as the uncaught
     * crash handler of a non-main thread to log crashes at {@link OminalConstants#OMINAL_CRASH_LOG_FILE_PATH}.
     */
    public static CrashHandler getCrashHandler(@NonNull final Context context) {
        return CrashHandler.getCrashHandler(context, new OminalCrashUtils(TYPE.CAUGHT_EXCEPTION));
    }

    /**
     * Log a crash to {@link OminalConstants#OMINAL_CRASH_LOG_FILE_PATH} and notify ominal app
     * by sending it the {@link OMINAL_APP.OMINAL_ACTIVITY#ACTION_NOTIFY_APP_CRASH} broadcast.
     *
     * @param context The {@link Context} for operations.
     * @param throwable The {@link Throwable} thrown for the crash.
     */
    public static void logCrash(@NonNull final Context context, final Throwable throwable) {
        if (throwable == null) return;
        CrashHandler.logCrash(context, new OminalCrashUtils(TYPE.CAUGHT_EXCEPTION), Thread.currentThread(), throwable);
    }

    @Override
    public boolean onPreLogCrash(Context context, Thread thread, Throwable throwable) {
        return false;
    }

    @Override
    public void onPostLogCrash(final Context currentPackageContext, Thread thread, Throwable throwable) {
        if (currentPackageContext == null) return;
        String currentPackageName = currentPackageContext.getPackageName();

        // Do not notify if is a non-ominal app
        final Context context = OminalUtils.getOminalPackageContext(currentPackageContext);
        if (context == null) {
            Logger.logWarn(LOG_TAG, "Ignoring call to onPostLogCrash() since failed to get \"" + OminalConstants.OMINAL_PACKAGE_NAME + "\" package context from \"" + currentPackageName + "\" context");
            return;
        }

        // If an uncaught exception, then do not notify since the ominal app itself would be crashing
        if (TYPE.UNCAUGHT_EXCEPTION.equals(mType) && OminalConstants.OMINAL_PACKAGE_NAME.equals(currentPackageName))
            return;

        String message = OMINAL_APP.OMINAL_ACTIVITY_NAME + " that \"" + currentPackageName + "\" app crashed";

       try {
           Logger.logInfo(LOG_TAG, "Sending broadcast to notify " + message);
            Intent intent = new Intent(OMINAL_APP.OMINAL_ACTIVITY.ACTION_NOTIFY_APP_CRASH);
            intent.setPackage(OminalConstants.OMINAL_PACKAGE_NAME);
            context.sendBroadcast(intent);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG,"Failed to notify " + message, e);
        }
    }

    @NonNull
    @Override
    public String getCrashLogFilePath(Context context) {
        return OminalConstants.OMINAL_CRASH_LOG_FILE_PATH;
    }

    @Override
    public String getAppInfoMarkdownString(Context context) {
        return OminalUtils.getAppInfoMarkdownString(context, true);
    }

    /**
     * Notify the user of an app crash by reading the crash info from the crash log file
     * at {@link OminalConstants#OMINAL_CRASH_LOG_FILE_PATH}. The crash log file would have been
     * created by {@link com.ominal.shared.crash.CrashHandler}.
     *
     * If the crash log file exists and is not empty and
     * {@link OminalPreferenceConstants.OMINAL_APP#KEY_CRASH_REPORT_NOTIFICATIONS_ENABLED} is
     * enabled, then a notification will be shown for the crash on the
     * {@link OminalConstants#OMINAL_CRASH_REPORTS_NOTIFICATION_CHANNEL_NAME} channel, otherwise nothing will be done.
     *
     * After reading from the crash log file, it will be moved to {@link OminalConstants#OMINAL_CRASH_LOG_BACKUP_FILE_PATH}.
     *
     * @param currentPackageContext The {@link Context} of current package.
     * @param logTagParam The log tag to use for logging.
     */
    public static void notifyAppCrashFromCrashLogFile(final Context currentPackageContext, final String logTagParam) {
        if (currentPackageContext == null) return;
        String currentPackageName = currentPackageContext.getPackageName();

        final Context context = OminalUtils.getOminalPackageContext(currentPackageContext);
        if (context == null) {
            Logger.logWarn(LOG_TAG, "Ignoring call to notifyAppCrash() since failed to get \"" + OminalConstants.OMINAL_PACKAGE_NAME + "\" package context from \"" + currentPackageName + "\" context");
            return;
        }

        OminalAppSharedPreferences preferences = OminalAppSharedPreferences.build(context);
        if (preferences == null) return;

        // If user has disabled notifications for crashes
        if (!preferences.areCrashReportNotificationsEnabled(false))
            return;

        new Thread() {
            @Override
            public void run() {
                notifyAppCrashFromCrashLogFileInner(context, logTagParam);
            }
        }.start();
    }

    private static synchronized void notifyAppCrashFromCrashLogFileInner(final Context context, final String logTagParam) {
        String logTag = DataUtils.getDefaultIfNull(logTagParam, LOG_TAG);

        if (!FileUtils.regularFileExists(OminalConstants.OMINAL_CRASH_LOG_FILE_PATH, false))
            return;

        Error error;
        StringBuilder reportStringBuilder = new StringBuilder();

        // Read report string from crash log file
        error = FileUtils.readTextFromFile("crash log", OminalConstants.OMINAL_CRASH_LOG_FILE_PATH, Charset.defaultCharset(), reportStringBuilder, false);
        if (error != null) {
            Logger.logErrorExtended(logTag, error.toString());
            return;
        }

        // Move crash log file to backup location if it exists
        error = FileUtils.moveRegularFile("crash log", OminalConstants.OMINAL_CRASH_LOG_FILE_PATH, OminalConstants.OMINAL_CRASH_LOG_BACKUP_FILE_PATH, true);
        if (error != null) {
            Logger.logErrorExtended(logTag, error.toString());
        }

        String reportString = reportStringBuilder.toString();

        if (reportString.isEmpty())
            return;

        Logger.logDebug(logTag, "A crash log file found at \"" + OminalConstants.OMINAL_CRASH_LOG_FILE_PATH +  "\".");

        sendCrashReportNotification(context, logTag, null, null, reportString, false, false, null, false);
    }




    /**
     * Send a crash report notification for {@link OminalConstants#OMINAL_CRASH_REPORTS_NOTIFICATION_CHANNEL_ID}
     * and {@link OminalConstants#OMINAL_CRASH_REPORTS_NOTIFICATION_CHANNEL_NAME}.
     *
     * @param currentPackageContext The {@link Context} of current package.
     * @param logTag The log tag to use for logging.
     * @param title The title for the crash report and notification.
     * @param message The message for the crash report.
     * @param throwable The {@link Throwable} for the crash report.
     */
    public static void sendCrashReportNotification(final Context currentPackageContext, String logTag,
                                                   CharSequence title, String message, Throwable throwable) {
        sendCrashReportNotification(currentPackageContext, logTag,
            title, message,
            MarkdownUtils.getMarkdownCodeForString(Logger.getMessageAndStackTraceString(message, throwable), true),
            false, false, true);
    }

    /**
     * Send a crash report notification for {@link OminalConstants#OMINAL_CRASH_REPORTS_NOTIFICATION_CHANNEL_ID}
     * and {@link OminalConstants#OMINAL_CRASH_REPORTS_NOTIFICATION_CHANNEL_NAME}.
     *
     * @param currentPackageContext The {@link Context} of current package.
     * @param logTag The log tag to use for logging.
     * @param title The title for the crash report and notification.
     * @param notificationTextString The text of the notification.
     * @param message The message for the crash report.
     */
    public static void sendCrashReportNotification(final Context currentPackageContext, String logTag,
                                                   CharSequence title, String notificationTextString,
                                                   String message) {
        sendCrashReportNotification(currentPackageContext, logTag,
            title, notificationTextString, message,
            false, false, true);
    }

    /**
     * Send a crash report notification for {@link OminalConstants#OMINAL_CRASH_REPORTS_NOTIFICATION_CHANNEL_ID}
     * and {@link OminalConstants#OMINAL_CRASH_REPORTS_NOTIFICATION_CHANNEL_NAME}.
     *
     * @param currentPackageContext The {@link Context} of current package.
     * @param logTag The log tag to use for logging.
     * @param title The title for the crash report and notification.
     * @param notificationTextString The text of the notification.
     * @param message The message for the crash report.
     * @param forceNotification If set to {@code true}, then a notification will be shown
     *                          regardless of if pending intent is {@code null} or
     *                          {@link OminalPreferenceConstants.OMINAL_APP#KEY_CRASH_REPORT_NOTIFICATIONS_ENABLED}
     *                          is {@code false}.
     * @param showToast If set to {@code true}, then a toast will be shown for {@code notificationTextString}.
     * @param addDeviceInfo If set to {@code true}, then device info should be appended to the message.
     */
    public static void sendCrashReportNotification(final Context currentPackageContext, String logTag,
                                                   CharSequence title, String notificationTextString,
                                                   String message, boolean forceNotification,
                                                   boolean showToast,
                                                   boolean addDeviceInfo) {
        sendCrashReportNotification(currentPackageContext, logTag,
            title, notificationTextString, "## " + title + "\n\n" + message + "\n\n",
            forceNotification, showToast, OminalUtils.AppInfoMode.OMINAL_AND_PLUGIN_PACKAGE, addDeviceInfo);
    }

    /**
     * Send a crash report notification for {@link OminalConstants#OMINAL_CRASH_REPORTS_NOTIFICATION_CHANNEL_ID}
     * and {@link OminalConstants#OMINAL_CRASH_REPORTS_NOTIFICATION_CHANNEL_NAME}.
     *
     * @param currentPackageContext The {@link Context} of current package.
     * @param logTag The log tag to use for logging.
     * @param title The title for the crash report and notification.
     * @param notificationTextString The text of the notification.
     * @param message The message for the crash report.
     * @param forceNotification If set to {@code true}, then a notification will be shown
     *                          regardless of if pending intent is {@code null} or
     *                          {@link OminalPreferenceConstants.OMINAL_APP#KEY_CRASH_REPORT_NOTIFICATIONS_ENABLED}
     *                          is {@code false}.
     * @param showToast If set to {@code true}, then a toast will be shown for {@code notificationTextString}.
     * @param appInfoMode The {@link OminalUtils.AppInfoMode} to use to add app info to the message.
     *                    Set to {@code null} if app info should not be appended to the message.
     * @param addDeviceInfo If set to {@code true}, then device info should be appended to the message.
     */
    public static void sendCrashReportNotification(final Context currentPackageContext, String logTag,
                                                   CharSequence title,
                                                   String notificationTextString,
                                                   String message, boolean forceNotification,
                                                   boolean showToast,
                                                   OminalUtils.AppInfoMode appInfoMode,
                                                   boolean addDeviceInfo) {
        // Note: Do not change currentPackageContext or ominalPackageContext passed to functions or things will break

        if (currentPackageContext == null) return;
        String currentPackageName = currentPackageContext.getPackageName();

        final Context ominalPackageContext = OminalUtils.getOminalPackageContext(currentPackageContext);
        if (ominalPackageContext == null) {
            Logger.logWarn(LOG_TAG, "Ignoring call to sendCrashReportNotification() since failed to get \"" + OminalConstants.OMINAL_PACKAGE_NAME + "\" package context from \"" + currentPackageName + "\" context");
            return;
        }

        OminalAppSharedPreferences preferences = OminalAppSharedPreferences.build(ominalPackageContext);
        if (preferences == null) return;

        // If user has disabled notifications for crashes
        if (!preferences.areCrashReportNotificationsEnabled(true) && !forceNotification)
            return;

        logTag = DataUtils.getDefaultIfNull(logTag, LOG_TAG);

        if (showToast)
            Logger.showToast(currentPackageContext, notificationTextString, true);

        // Send a notification to show the crash log which when clicked will open the {@link ReportActivity}
        // to show the details of the crash
        if (title == null || title.toString().isEmpty())
            title = OminalConstants.OMINAL_APP_NAME + " Crash Report";

        Logger.logDebug(logTag, "Sending \"" + title + "\" notification.");

        StringBuilder reportString = new StringBuilder(message);

        if (appInfoMode != null)
            reportString.append("\n\n").append(OminalUtils.getAppInfoMarkdownString(currentPackageContext, appInfoMode, currentPackageName));

        if (addDeviceInfo)
            reportString.append("\n\n").append(AndroidUtils.getDeviceInfoMarkdownString(currentPackageContext, true));

        String userActionName = UserAction.CRASH_REPORT.getName();

        ReportInfo reportInfo = new ReportInfo(userActionName, logTag, title.toString());
        reportInfo.setReportString(reportString.toString());
        reportInfo.setReportStringSuffix("\n\n" + OminalUtils.getReportIssueMarkdownString(currentPackageContext));
        reportInfo.setAddReportInfoHeaderToMarkdown(true);
        reportInfo.setReportSaveFileLabelAndPath(userActionName,
            Environment.getExternalStorageDirectory() + "/" +
                FileUtils.sanitizeFileName(OminalConstants.OMINAL_APP_NAME + "-" + userActionName + ".log", true, true));

        ReportActivity.NewInstanceResult result = ReportActivity.newInstance(ominalPackageContext, reportInfo);
        if (result.contentIntent == null) return;

        // Must ensure result code for PendingIntents and id for notification are unique otherwise will override previous
        int nextNotificationId = OminalNotificationUtils.getNextNotificationId(ominalPackageContext);

        PendingIntent contentIntent = PendingIntent.getActivity(ominalPackageContext, nextNotificationId, result.contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent deleteIntent = null;
        if (result.deleteIntent != null)
            deleteIntent = PendingIntent.getBroadcast(ominalPackageContext, nextNotificationId, result.deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Setup the notification channel if not already set up
        setupCrashReportsNotificationChannel(ominalPackageContext);

        // Use markdown in notification
        CharSequence notificationTextCharSequence = MarkdownUtils.getSpannedMarkdownText(ominalPackageContext, notificationTextString);
        //CharSequence notificationTextCharSequence = notificationTextString;

        // Build the notification
        Notification.Builder builder = getCrashReportsNotificationBuilder(currentPackageContext, ominalPackageContext,
            title, notificationTextCharSequence, notificationTextCharSequence, contentIntent, deleteIntent,
            NotificationUtils.NOTIFICATION_MODE_VIBRATE);
        if (builder == null) return;

        // Send the notification
        NotificationManager notificationManager = NotificationUtils.getNotificationManager(ominalPackageContext);
        if (notificationManager != null)
            notificationManager.notify(nextNotificationId, builder.build());
    }

    /**
     * Get {@link Notification.Builder} for {@link OminalConstants#OMINAL_CRASH_REPORTS_NOTIFICATION_CHANNEL_ID}
     * and {@link OminalConstants#OMINAL_CRASH_REPORTS_NOTIFICATION_CHANNEL_NAME}.
     *
     * @param currentPackageContext The {@link Context} of current package.
     * @param ominalPackageContext The {@link Context} of ominal package.
     * @param title The title for the notification.
     * @param notificationText The second line text of the notification.
     * @param notificationBigText The full text of the notification that may optionally be styled.
     * @param contentIntent The {@link PendingIntent} which should be sent when notification is clicked.
     * @param deleteIntent The {@link PendingIntent} which should be sent when notification is deleted.
     * @param notificationMode The notification mode. It must be one of {@code NotificationUtils.NOTIFICATION_MODE_*}.
     * @return Returns the {@link Notification.Builder}.
     */
    @Nullable
    public static Notification.Builder getCrashReportsNotificationBuilder(final Context currentPackageContext,
                                                                          final Context ominalPackageContext,
                                                                          final CharSequence title,
                                                                          final CharSequence notificationText,
                                                                          final CharSequence notificationBigText,
                                                                          final PendingIntent contentIntent,
                                                                          final PendingIntent deleteIntent,
                                                                          final int notificationMode) {
        return OminalNotificationUtils.getOminalOrPluginAppNotificationBuilder(
            currentPackageContext, ominalPackageContext,
            OminalConstants.OMINAL_CRASH_REPORTS_NOTIFICATION_CHANNEL_ID, Notification.PRIORITY_HIGH,
            title, notificationText, notificationBigText, contentIntent, deleteIntent, notificationMode);
    }

    /**
     * Setup the notification channel for {@link OminalConstants#OMINAL_CRASH_REPORTS_NOTIFICATION_CHANNEL_ID} and
     * {@link OminalConstants#OMINAL_CRASH_REPORTS_NOTIFICATION_CHANNEL_NAME}.
     *
     * @param context The {@link Context} for operations.
     */
    public static void setupCrashReportsNotificationChannel(final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationUtils.setupNotificationChannel(context, OminalConstants.OMINAL_CRASH_REPORTS_NOTIFICATION_CHANNEL_ID,
            OminalConstants.OMINAL_CRASH_REPORTS_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
    }

}
