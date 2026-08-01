package com.ominal.x11;

import android.os.ParcelFileDescriptor;

/** Minimal in-process entry point for Ominal's native X server. */
public final class CmdEntryPoint {
    private static boolean sStarted;

    static {
        System.loadLibrary("ominal-display-env");
        System.loadLibrary("ominal-display");
    }

    public static synchronized boolean startServer(String temporaryDirectory,
                                                   String xkbConfigRoot,
                                                   int densityDpi) {
        prepareEnvironment(temporaryDirectory, xkbConfigRoot);
        if (sStarted) return true;
        new java.io.File(temporaryDirectory, ".X11-unix/X20").delete();
        new java.io.File(temporaryDirectory, ".X20-lock").delete();
        sStarted = start(new String[]{
            ":20", "-nolisten", "tcp", "-nocursor", "-dpi", Integer.toString(densityDpi)
        });
        return sStarted;
    }

    public ParcelFileDescriptor createViewConnection() {
        return getXConnection();
    }

    private static native void prepareEnvironment(String temporaryDirectory, String xkbConfigRoot);
    private static native boolean start(String[] args);
    private native ParcelFileDescriptor getXConnection();
    public static native boolean connected();
}
