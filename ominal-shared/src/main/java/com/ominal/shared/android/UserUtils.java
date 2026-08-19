package com.ominal.shared.android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominal.shared.logger.Logger;

public class UserUtils {

    public static final String LOG_TAG = "UserUtils";

    /**
     * Get the user name for user id from the package manager, falling back to public Android
     * constants for well-known platform users.
     *
     * @param context The {@link Context} for operations.
     * @param uid The user id.
     * @return Returns the user name if found, otherwise {@code null}.
     */
    @Nullable
    public static String getNameForUid(@NonNull Context context, int uid) {
        String name = getNameForUidFromPackageManager(context, uid);
        if (name == null)
            name = getNameForPlatformUid(uid);
        return name;
    }

    /**
     * Get the user name for user id with a call to {@link PackageManager#getNameForUid(int)}.
     *
     * This will not return names for non-app users such as root. Use
     * {@link #getNameForPlatformUid(int)} for known platform users.
     *
     * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/content/pm/PackageManager.java;l=5556
     * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/app/ApplicationPackageManager.java;l=1028
     * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/services/core/java/com/android/server/pm/PackageManagerService.java;l=10293
     *
     * @param context The {@link Context} for operations.
     * @param uid The user id.
     * @return Returns the user name if found, otherwise {@code null}.
     */
    @Nullable
    public static String getNameForUidFromPackageManager(@NonNull Context context, int uid) {
        if (uid < 0) return null;

        try {
            String name = context.getPackageManager().getNameForUid(uid);
            if (name != null && name.endsWith(":" + uid))
                name = name.replaceAll(":" + uid + "$", ""); // Remove ":<uid>" suffix
            return name;
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to get name for uid \"" + uid + "\" from package manager", e);
            return null;
        }
    }

    /** Return a public Android name for a well-known platform uid. */
    @Nullable
    public static String getNameForPlatformUid(int uid) {
        if (uid == Process.ROOT_UID) return "root";
        if (uid == Process.SYSTEM_UID) return "system";
        if (uid == Process.SHELL_UID) return "shell";
        if (uid == Process.BLUETOOTH_UID) return "bluetooth";
        return null;
    }

}
