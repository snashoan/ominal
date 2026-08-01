package com.ominal.x11;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import java.io.File;

/** Connects the chat renderer to the native X server running in :display. */
public final class OminalNativeDisplay {
    private static final String TAG = "OminalNativeDisplay";
    private static final Object LOCK = new Object();

    public interface Callback {
        void onConnected();
        void onDisconnected();
        void onError(String message);
    }

    private static IOminalDisplayService sService;
    private static Request sPendingRequest;
    private static Request sActiveRequest;
    private static boolean sBinding;
    private static boolean sConnectInFlight;
    private static long sConnectionGeneration;

    private static final ServiceConnection SERVICE_CONNECTION = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Request pending;
            synchronized (LOCK) {
                sService = IOminalDisplayService.Stub.asInterface(binder);
                sBinding = false;
                pending = sPendingRequest;
            }
            Log.i(TAG, "Display service connected");
            if (pending != null) connect(pending);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            OminalNativeDisplay.handleServiceLoss("Display service disconnected");
        }

        @Override
        public void onBindingDied(ComponentName name) {
            OminalNativeDisplay.handleServiceLoss("Display service binding died");
        }
    };

    private OminalNativeDisplay() {}

    public static void startAndConnect(LorieView view, File temporaryDirectory,
                                       File xkbConfigRoot, int densityDpi,
                                       Callback callback) {
        if (!temporaryDirectory.isDirectory() && !temporaryDirectory.mkdirs()) {
            callback.onError("Screen storage is unavailable");
            return;
        }
        if (!xkbConfigRoot.isDirectory()) {
            callback.onError("Linux keyboard data is unavailable");
            return;
        }
        if (LorieView.connected()) {
            view.refreshDisplaySize();
            callback.onConnected();
            return;
        }

        Request request = new Request(view, temporaryDirectory, xkbConfigRoot,
            densityDpi, callback);
        synchronized (LOCK) {
            sPendingRequest = request;
            if (sService != null) {
                connect(request);
                return;
            }
        }

        bindDisplayService(request);
    }

    public static void release(LorieView view) {
        if (view == null) return;
        synchronized (LOCK) {
            if (sPendingRequest != null && sPendingRequest.view == view) sPendingRequest = null;
            if (sActiveRequest != null && sActiveRequest.view == view) sActiveRequest = null;
        }
    }

    private static void bindDisplayService(Request request) {
        boolean shouldBind;
        synchronized (LOCK) {
            if (sService != null) {
                connect(request);
                return;
            }
            shouldBind = !sBinding;
            if (shouldBind) sBinding = true;
        }
        if (!shouldBind) return;

        Context context = request.view.getContext().getApplicationContext();
        Intent intent = new Intent(context, OminalDisplayService.class);
        int flags = Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT;
        if (!context.bindService(intent, SERVICE_CONNECTION, flags))
            finishWithError(request, "Native screen service is unavailable");
    }

    private static void connect(Request request) {
        long generation;
        synchronized (LOCK) {
            if (sConnectInFlight) return;
            sConnectInFlight = true;
            generation = ++sConnectionGeneration;
        }
        new Thread(() -> {
            IOminalDisplayService service;
            synchronized (LOCK) {
                service = sService;
            }
            if (service == null) {
                request.view.post(() -> finishWithError(request,
                    "Native screen service disconnected", generation));
                return;
            }

            try {
                ParcelFileDescriptor descriptor = service.openRendererConnection(
                    request.temporaryDirectory.getAbsolutePath(),
                    request.xkbConfigRoot.getAbsolutePath(), request.densityDpi);
                request.view.post(() -> {
                    if (!isCurrentGeneration(generation)) {
                        if (descriptor != null) {
                            try {
                                descriptor.close();
                            } catch (Exception ignored) {
                            }
                        }
                        return;
                    }
                    if (descriptor == null) {
                        finishWithError(request, "Native screen connection failed", generation);
                        return;
                    }
                    LorieView.connect(descriptor.detachFd());
                    Log.i(TAG, "Renderer connection requested");
                    waitForRenderer(request, generation);
                });
            } catch (RemoteException e) {
                request.view.post(() -> {
                    if (isCurrentGeneration(generation))
                        handleServiceLoss("Native screen service failed");
                });
            }
        }, "OminalDisplayBinder").start();
    }

    private static void handleServiceLoss(String reason) {
        Request reconnectRequest;
        synchronized (LOCK) {
            sService = null;
            sBinding = false;
            sConnectInFlight = false;
            sConnectionGeneration++;
            reconnectRequest = sPendingRequest != null ? sPendingRequest : sActiveRequest;
            sActiveRequest = null;
            sPendingRequest = reconnectRequest;
        }
        LorieView.connect(-1);
        Log.w(TAG, reason);
        if (reconnectRequest == null) return;
        reconnectRequest.view.post(() -> {
            reconnectRequest.callback.onDisconnected();
            reconnectRequest.view.postDelayed(
                () -> bindDisplayService(reconnectRequest), 250);
        });
    }

    private static void waitForRenderer(Request request, long generation) {
        new Thread(() -> {
            for (int attempt = 0; attempt < 80; attempt++) {
                if (!isCurrentGeneration(generation)) return;
                if (LorieView.connected()) {
                    request.view.post(() -> {
                        if (!isCurrentGeneration(generation)) return;
                        Log.i(TAG, "Renderer connected");
                        request.view.refreshDisplaySize();
                        request.view.postDelayed(request.view::refreshDisplaySize, 250);
                        synchronized (LOCK) {
                            sConnectInFlight = false;
                            if (sPendingRequest == request) sPendingRequest = null;
                            sActiveRequest = request;
                        }
                        request.callback.onConnected();
                    });
                    return;
                }
                try {
                    Thread.sleep(125);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            request.view.post(() -> finishWithError(request,
                "Native screen renderer timed out", generation));
        }, "OminalDisplayRenderer").start();
    }

    private static void finishWithError(Request request, String message) {
        synchronized (LOCK) {
            sBinding = false;
            sConnectInFlight = false;
            if (sPendingRequest == request) sPendingRequest = null;
        }
        request.callback.onError(message);
    }

    private static void finishWithError(Request request, String message, long generation) {
        synchronized (LOCK) {
            if (generation != sConnectionGeneration) return;
            sBinding = false;
            sConnectInFlight = false;
            if (sPendingRequest == request) sPendingRequest = null;
        }
        request.callback.onError(message);
    }

    private static boolean isCurrentGeneration(long generation) {
        synchronized (LOCK) {
            return generation == sConnectionGeneration;
        }
    }

    private static final class Request {
        final LorieView view;
        final File temporaryDirectory;
        final File xkbConfigRoot;
        final int densityDpi;
        final Callback callback;

        Request(LorieView view, File temporaryDirectory, File xkbConfigRoot,
                int densityDpi, Callback callback) {
            this.view = view;
            this.temporaryDirectory = temporaryDirectory;
            this.xkbConfigRoot = xkbConfigRoot;
            this.densityDpi = densityDpi;
            this.callback = callback;
        }
    }
}
