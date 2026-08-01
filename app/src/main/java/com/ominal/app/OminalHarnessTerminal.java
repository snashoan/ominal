package com.ominal.app;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominal.shared.net.uri.UriUtils;
import com.ominal.shared.runtime.OminalConstants;
import com.ominal.shared.runtime.OminalConstants.OMINAL_APP.OMINAL_SERVICE;

import java.io.File;

/** Launches supported intelligence harnesses as user-controlled terminal sessions. */
public final class OminalHarnessTerminal {

    public static final String CODEX_ID = "codex";
    public static final String CLAUDE_CODE_ID = "claude-code";
    public static final String ANTIGRAVITY_ID = "antigravity";
    public static final String EXECUTABLE_PATH =
        OminalConstants.OMINAL_BIN_PREFIX_DIR_PATH + "/ominal-harness-tui";

    private OminalHarnessTerminal() {}

    public static void configureIntent(@NonNull Intent intent, @NonNull String harnessId,
                                       @Nullable String workingDirectory) {
        configureIntent(intent, harnessId, workingDirectory, "", "");
    }

    public static void configureIntent(@NonNull Intent intent, @NonNull String harnessId,
                                       @Nullable String workingDirectory,
                                       @Nullable String modelId, @Nullable String effortId) {
        configureIntent(intent, harnessId, workingDirectory, modelId, effortId, "");
    }

    public static void configureIntent(@NonNull Intent intent, @NonNull String harnessId,
                                       @Nullable String workingDirectory,
                                       @Nullable String modelId, @Nullable String effortId,
                                       @Nullable String initialPrompt) {
        if (!isSupported(harnessId))
            throw new IllegalArgumentException("Unsupported harness: " + harnessId);

        String workspace = OminalProotTerminal.normalizeWorkspace(workingDirectory);
        java.util.ArrayList<String> arguments = new java.util.ArrayList<>();
        arguments.add(harnessId);
        arguments.add(workspace);
        if (ANTIGRAVITY_ID.equals(harnessId)) {
            if (modelId != null && !modelId.trim().isEmpty()) {
                arguments.add("--model");
                arguments.add(modelId.trim());
            }
            if (effortId != null && !effortId.trim().isEmpty()) {
                arguments.add("--effort");
                arguments.add(effortId.trim());
            }
            if (initialPrompt != null && !initialPrompt.trim().isEmpty()) {
                arguments.add("--prompt-interactive");
                arguments.add(initialPrompt);
            }
        }
        intent.setData(UriUtils.getFileUri(EXECUTABLE_PATH));
        intent.putExtra(OMINAL_SERVICE.EXTRA_ARGUMENTS, arguments.toArray(new String[0]));
        intent.putExtra(OMINAL_SERVICE.EXTRA_WORKDIR, workspace);
    }

    @NonNull
    public static String sessionName(@NonNull String chatId, @NonNull String harnessId) {
        return "ominal-proot-" + chatId + "-" + harnessId;
    }

    public static boolean isReady() {
        return new File(EXECUTABLE_PATH).canExecute() && OminalProotTerminal.isReady();
    }

    public static boolean isSupported(@Nullable String harnessId) {
        return CODEX_ID.equals(harnessId)
            || CLAUDE_CODE_ID.equals(harnessId)
            || ANTIGRAVITY_ID.equals(harnessId);
    }
}
