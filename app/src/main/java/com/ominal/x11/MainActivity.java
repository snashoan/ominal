package com.ominal.x11;

/** JNI callback target kept separate from the chat activity. */
public final class MainActivity {
    private static final MainActivity INSTANCE = new MainActivity();
    private volatile Runnable mConnectionChanged;

    private MainActivity() {}

    public static MainActivity getInstance() {
        return INSTANCE;
    }

    public void setConnectionChangedListener(Runnable listener) {
        mConnectionChanged = listener;
    }

    public void clientConnectedStateChanged() {
        Runnable listener = mConnectionChanged;
        if (listener != null) listener.run();
    }
}
