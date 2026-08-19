package com.ominal.shared.android;

import android.os.Process;
import android.system.Os;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominal.shared.logger.Logger;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

public class SELinuxUtils {

    private static final String LOG_TAG = "SELinuxUtils";
    private static final String SELINUX_XATTR_NAME = "security.selinux";

    /**
     * Gets the security context of the current process.
     *
     * @return Returns a {@link String} representing the security context of the current process.
     * This will be {@code null} if an exception is raised.
     */
    @Nullable
    public static String getContext() {
        return getPidContext(Process.myPid());
    }

    /**
     * Get the security context of a given process id.
     *
     * @param pid The pid of process.
     * @return Returns a {@link String} representing the security context of the given pid.
     * This will be {@code null} if an exception is raised.
     */
    @Nullable
    public static String getPidContext(int pid) {
        if (pid <= 0) return null;
        String path = "/proc/" + pid + "/attr/current";
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            return normalizeContext(reader.readLine());
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG,
                "Failed to read SELinux process context from " + path, e);
            return null;
        }
    }

    /**
     * Get the security context of a file object.
     *
     * @param path The pathname of the file object.
     * @return Returns a {@link String} representing the security context of the file.
     * This will be {@code null} if an exception is raised.
     */
    @Nullable
    public static String getFileContext(@NonNull String path) {
        try {
            byte[] value = Os.getxattr(path, SELINUX_XATTR_NAME);
            return normalizeContext(new String(value, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG,
                "Failed to read SELinux file context for " + path, e);
            return null;
        }
    }

    @Nullable
    private static String normalizeContext(@Nullable String value) {
        if (value == null) return null;
        String normalized = value.replace("\u0000", "").trim();
        return normalized.isEmpty() ? null : normalized;
    }

}
