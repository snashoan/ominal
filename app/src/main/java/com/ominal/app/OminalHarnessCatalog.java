package com.ominal.app;

import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/** Watches validated local harness packages and coalesces registry updates for the UI. */
final class OminalHarnessCatalog {
    private static final int EVENTS = FileObserver.CREATE | FileObserver.CLOSE_WRITE
        | FileObserver.DELETE | FileObserver.MOVED_FROM | FileObserver.MOVED_TO;
    private static final long CHANGE_DEBOUNCE_MS = 180L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable listener;
    private final Runnable changeRunnable;
    private final ArrayList<FileObserver> observers = new ArrayList<>();
    private boolean started;
    private String fingerprint = "";

    OminalHarnessCatalog(@NonNull Runnable listener) {
        this.listener = listener;
        changeRunnable = () -> {
            if (!started) return;
            rebuildObservers();
            String current = fingerprint();
            if (current.equals(fingerprint)) return;
            fingerprint = current;
            this.listener.run();
        };
    }

    void start() {
        if (started) return;
        started = true;
        ensureDirectory(OminalHarnessManifest.registryDirectory());
        ensureDirectory(OminalHarnessManifest.legacyDirectory());
        rebuildObservers();
        fingerprint = fingerprint();
    }

    void refresh() {
        if (!started) return;
        rebuildObservers();
        scheduleChange();
    }

    void stop() {
        started = false;
        mainHandler.removeCallbacksAndMessages(null);
        stopObservers();
    }

    private void rebuildObservers() {
        stopObservers();
        watch(OminalHarnessManifest.registryDirectory());
        watch(OminalHarnessManifest.legacyDirectory());
        File[] packages = OminalHarnessManifest.registryDirectory().listFiles(File::isDirectory);
        if (packages != null) {
            for (File directory : packages) watch(directory);
        }
    }

    private void watch(File directory) {
        if (!directory.isDirectory()) return;
        FileObserver observer = new FileObserver(directory.getAbsolutePath(), EVENTS) {
            @Override
            public void onEvent(int event, String path) {
                if (!started) return;
                scheduleChange();
            }
        };
        observer.startWatching();
        observers.add(observer);
    }

    private void scheduleChange() {
        mainHandler.removeCallbacks(changeRunnable);
        mainHandler.postDelayed(changeRunnable, CHANGE_DEBOUNCE_MS);
    }

    private void stopObservers() {
        for (FileObserver observer : observers) observer.stopWatching();
        observers.clear();
    }

    private static void ensureDirectory(File directory) {
        if (!directory.isDirectory()) directory.mkdirs();
    }

    private static String fingerprint() {
        ArrayList<String> entries = new ArrayList<>();
        appendFiles(entries, OminalHarnessManifest.legacyDirectory());
        File registry = OminalHarnessManifest.registryDirectory();
        File[] packages = registry.listFiles(File::isDirectory);
        if (packages != null) {
            for (File directory : packages) appendFiles(entries, directory);
        }
        Collections.sort(entries);
        return entries.toString();
    }

    private static void appendFiles(ArrayList<String> entries, File directory) {
        File[] files = directory.listFiles(File::isFile);
        if (files == null) return;
        for (File file : files) {
            entries.add(file.getAbsolutePath() + ':' + file.length() + ':' + file.lastModified());
        }
    }
}
