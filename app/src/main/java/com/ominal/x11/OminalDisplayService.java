package com.ominal.x11;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/** Runs the native X server outside the chat renderer process. */
public final class OminalDisplayService extends Service {
    private static final String TAG = "OminalDisplayService";
    private final CmdEntryPoint mServer = new CmdEntryPoint();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private final IOminalDisplayService.Stub mBinder = new IOminalDisplayService.Stub() {
        @Override
        public synchronized ParcelFileDescriptor openRendererConnection(
            String temporaryDirectory, String xkbConfigRoot, int densityDpi) {
            File temporary = new File(temporaryDirectory);
            File keyboardData = new File(xkbConfigRoot);
            if ((!temporary.isDirectory() && !temporary.mkdirs()) || !keyboardData.isDirectory())
                return null;

            if (!startServerOnMainLooper(temporaryDirectory, xkbConfigRoot, densityDpi))
                return null;

            File socket = new File(temporary, ".X11-unix/X20");
            for (int attempt = 0; attempt < 80 && !socket.exists(); attempt++) {
                try {
                    Thread.sleep(125);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            if (!socket.exists()) return null;
            Log.i(TAG, "Opening renderer connection");
            return mServer.createViewConnection();
        }
    };

    private boolean startServerOnMainLooper(
        String temporaryDirectory, String xkbConfigRoot, int densityDpi) {
        if (Looper.myLooper() == Looper.getMainLooper())
            return CmdEntryPoint.startServer(temporaryDirectory, xkbConfigRoot, densityDpi);

        FutureTask<Boolean> task = new FutureTask<>(
            () -> CmdEntryPoint.startServer(temporaryDirectory, xkbConfigRoot, densityDpi));
        mMainHandler.post(task);
        try {
            return task.get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            task.cancel(true);
            Log.e(TAG, "Native display server did not start", e);
            return false;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
