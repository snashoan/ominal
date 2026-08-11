package com.ominal.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ominal.R;
import com.ominal.shared.logger.Logger;
import com.ominal.shared.runtime.OminalConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

/** Claims browser requests emitted by Linux runtime adapters. */
public final class OminalUrlRequestBridge {
    public static final String ACTION_OPEN_INTERNAL_URL =
        "com.ominal.action.OPEN_INTERNAL_URL";

    private static final String LOG_TAG = "OminalUrlRequestBridge";
    private static final String REQUEST_SUFFIX = ".request";
    private static final int MAX_URL_LENGTH = 8192;
    private static final Object CLAIM_LOCK = new Object();

    private final Activity mActivity;
    private final File mRequestDirectory;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final Runnable mDrainRunnable = this::drain;
    private FileObserver mObserver;

    public OminalUrlRequestBridge(@NonNull Activity activity) {
        mActivity = activity;
        mRequestDirectory = new File(OminalConstants.OMINAL_HOME_DIR_PATH,
            ".ominal/bridge");
    }

    public void start() {
        if (mObserver != null) return;
        if (!mRequestDirectory.isDirectory() && !mRequestDirectory.mkdirs()) {
            Logger.logError(LOG_TAG, "Could not create the runtime bridge directory");
            return;
        }
        mObserver = new FileObserver(mRequestDirectory.getAbsolutePath(),
            FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
            @Override
            public void onEvent(int event, @Nullable String path) {
                if (path != null && path.endsWith(REQUEST_SUFFIX)) scheduleDrain();
            }
        };
        mObserver.startWatching();
        scheduleDrain();
    }

    public void stop() {
        mMainHandler.removeCallbacks(mDrainRunnable);
        if (mObserver == null) return;
        mObserver.stopWatching();
        mObserver = null;
    }

    private void scheduleDrain() {
        mMainHandler.removeCallbacks(mDrainRunnable);
        mMainHandler.postDelayed(mDrainRunnable, 40);
    }

    private void drain() {
        File request = claimOldestRequest(mRequestDirectory);
        if (request == null) return;
        Uri uri = null;
        try {
            uri = parseUrl(readFirstLine(request));
        } catch (IOException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Could not read URL request", e);
        } finally {
            if (!request.delete()) Logger.logWarn(LOG_TAG, "Could not remove URL request");
        }
        if (uri != null && !mActivity.isFinishing()) showDestinationChooser(mActivity, uri);
        if (hasPendingRequests(mRequestDirectory)) scheduleDrain();
    }

    @Nullable
    static Uri parseUrl(@Nullable String value) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_URL_LENGTH) return null;
        for (int i = 0; i < normalized.length(); i++) {
            if (Character.isISOControl(normalized.charAt(i))) return null;
        }
        Uri uri = Uri.parse(normalized);
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
            return null;
        return uri;
    }

    public static void showDestinationChooser(@NonNull Activity activity, @NonNull Uri uri) {
        new AlertDialog.Builder(activity)
            .setTitle(R.string.url_route_title)
            .setItems(new CharSequence[]{
                activity.getString(R.string.url_route_internal),
                activity.getString(R.string.url_route_external)
            }, (dialog, which) -> {
                if (which == 0) {
                    Intent internal = new Intent(activity, OringutanActivity.class)
                        .setAction(ACTION_OPEN_INTERNAL_URL)
                        .setData(uri)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    activity.startActivity(internal);
                } else {
                    Intent external = new Intent(Intent.ACTION_VIEW, uri);
                    activity.startActivity(Intent.createChooser(external,
                        activity.getString(R.string.url_route_external_title)));
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    @Nullable
    private static File claimOldestRequest(File directory) {
        synchronized (CLAIM_LOCK) {
            File[] requests = directory.listFiles(file -> file.isFile()
                && file.getName().endsWith(REQUEST_SUFFIX));
            if (requests == null || requests.length == 0) return null;
            Arrays.sort(requests, (left, right) -> left.getName().compareTo(right.getName()));
            for (File request : requests) {
                File claimed = new File(directory, request.getName() + ".processing");
                if (request.renameTo(claimed)) return claimed;
            }
            return null;
        }
    }

    private static boolean hasPendingRequests(File directory) {
        File[] requests = directory.listFiles(file -> file.isFile()
            && file.getName().endsWith(REQUEST_SUFFIX));
        return requests != null && requests.length > 0;
    }

    private static String readFirstLine(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return reader.readLine();
        }
    }
}
