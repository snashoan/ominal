package com.ominal.app;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominal.shared.net.uri.UriUtils;
import com.ominal.shared.runtime.OminalConstants;
import com.ominal.shared.runtime.OminalConstants.OMINAL_APP.OMINAL_SERVICE;

import java.io.File;

/** Defines the single Linux guest used by chat and user-created terminal sessions. */
public final class OminalProotTerminal {

    public static final String EXECUTABLE_PATH =
        OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH + "/ominal-proot-shell";

    private static final String HOME = OminalConstants.OMINAL_HOME_DIR_PATH;
    private static final String USER_DATA_HOME = "/data/user/0/com.ominal/files/home";
    private static final String GUEST_WORKSPACE = "/root/workspace";
    private static final String RUNTIME_READY_PATH =
        HOME + "/.ominal/runtime/linux/rootfs/.ominal-rootfs-ready";

    private OminalProotTerminal() {}

    public static void configureIntent(@NonNull Intent intent, @Nullable String workingDirectory) {
        String workspace = normalizeWorkspace(workingDirectory);
        intent.setData(UriUtils.getFileUri(EXECUTABLE_PATH));
        intent.putExtra(OMINAL_SERVICE.EXTRA_ARGUMENTS, new String[]{workspace});
        intent.putExtra(OMINAL_SERVICE.EXTRA_WORKDIR, workspace);
    }

    public static boolean isReady() {
        return new File(EXECUTABLE_PATH).canExecute() && new File(RUNTIME_READY_PATH).isFile();
    }

    @NonNull
    public static String normalizeWorkspace(@Nullable String workingDirectory) {
        if (workingDirectory == null || workingDirectory.trim().isEmpty()
            || GUEST_WORKSPACE.equals(workingDirectory)
            || workingDirectory.startsWith(GUEST_WORKSPACE + "/")) {
            return HOME + "/workspace";
        }

        String normalized = workingDirectory;
        if (normalized.equals(USER_DATA_HOME) || normalized.startsWith(USER_DATA_HOME + "/"))
            normalized = HOME + normalized.substring(USER_DATA_HOME.length());

        if (normalized.equals(HOME) || normalized.startsWith(HOME + "/"))
            return normalized;
        return HOME + "/workspace";
    }
}
