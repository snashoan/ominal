package com.ominal.x11;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;

/** Native, cursor-free Android surface for the shared Ominal X display. */
public final class LorieView extends SurfaceView {
    private static final String TAG = "OminalDisplaySurface";
    private static final int PIXEL_FORMAT_BGRA_8888 = 5;
    private static final int XI_TOUCH_BEGIN = 18;
    private static final int XI_TOUCH_UPDATE = 19;
    private static final int XI_TOUCH_END = 20;
    private static final int MAX_TOUCH_POINTS = 10;
    private static final float CONTENT_ZOOM_STEP = 1.12f;
    private static final int MOUSE_WHEEL_UP = 4;
    private static final int MOUSE_WHEEL_DOWN = 5;
    private static WeakReference<LorieView> sActiveView = new WeakReference<>(null);

    private final Point mDisplaySize = new Point(1, 1);
    private final Rect mInputViewport = new Rect(0, 0, 1, 1);
    private float mSourceLeft;
    private float mSourceTop;
    private float mSourceWidth = 1;
    private float mSourceHeight = 1;
    private final boolean[] mActiveTouches = new boolean[MAX_TOUCH_POINTS];
    private final int[] mLastTouchX = new int[MAX_TOUCH_POINTS];
    private final int[] mLastTouchY = new int[MAX_TOUCH_POINTS];
    private float mTapDownX;
    private float mTapDownY;
    private boolean mTapMoved;
    private final int mTouchSlop;
    private final float mScrollStep;
    private final ScaleGestureDetector mScaleDetector;
    private float mContentZoomAccumulator = 1f;
    private float mLastMultiTouchY;
    private float mMultiTouchScrollRemainder;
    private boolean mContentZooming;
    private boolean mScalingGesture;
    private boolean mViewportUpdatesEnabled = true;
    private boolean mInputBridgeActive;
    private boolean mClipboardListenerRegistered;
    private boolean mApplyingXClipboard;
    private long mLastClipboardTimestamp;
    private String mLastAnnouncedClipboardText;
    private ClipboardManager mClipboardManager;
    private InputMethodManager mInputMethodManager;
    private final Runnable mShowKeyboardAfterLongPress = this::showKeyboard;
    private final ClipboardManager.OnPrimaryClipChangedListener mClipboardListener =
        this::handleClipboardChange;

    public LorieView(Context context) {
        super(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mScrollStep = 28f * getResources().getDisplayMetrics().density;
        mScaleDetector = new ScaleGestureDetector(context,
            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScaleBegin(ScaleGestureDetector detector) {
                    removeCallbacks(mShowKeyboardAfterLongPress);
                    endAllTouches();
                    mContentZoomAccumulator = 1f;
                    mContentZooming = false;
                    mScalingGesture = true;
                    return true;
                }

                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    mContentZoomAccumulator *= detector.getScaleFactor();
                    while (mContentZoomAccumulator >= CONTENT_ZOOM_STEP) {
                        mContentZooming = true;
                        sendContentZoomShortcut(true);
                        mContentZoomAccumulator /= CONTENT_ZOOM_STEP;
                    }
                    while (mContentZoomAccumulator <= 1f / CONTENT_ZOOM_STEP) {
                        mContentZooming = true;
                        sendContentZoomShortcut(false);
                        mContentZoomAccumulator *= CONTENT_ZOOM_STEP;
                    }
                    return true;
                }

                @Override
                public void onScaleEnd(ScaleGestureDetector detector) {
                    mContentZoomAccumulator = 1f;
                    mContentZooming = false;
                    mScalingGesture = false;
                }
            });
        initialize();
    }

    private void initialize() {
        sActiveView = new WeakReference<>(this);
        mClipboardManager = (ClipboardManager)
            getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        mInputMethodManager = (InputMethodManager)
            getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        setFocusable(false);
        setFocusableInTouchMode(false);
        setBackground(new ColorDrawable(Color.TRANSPARENT));
        setContentDescription("Linux display");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_NULL));
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                holder.setFormat(PIXEL_FORMAT_BGRA_8888);
                Log.i(TAG, "Surface created");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                LorieView.this.surfaceChanged(holder.getSurface());
                Log.i(TAG, "Surface bound " + width + "x" + height);
                if (mViewportUpdatesEnabled) updateDisplaySize(width, height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                LorieView.this.surfaceChanged(null);
                Log.i(TAG, "Surface destroyed");
            }
        });
        nativeInit();
        setFiltering(1);
        setClipboardSyncEnabled(true, true);
    }

    /** Enables Android input ownership while the native display is frontmost. */
    public void activateInputBridge() {
        if (mInputBridgeActive) {
            updateClipboardListener();
            return;
        }
        mInputBridgeActive = true;
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        updateClipboardListener();
        announceAndroidClipboard(true);
    }

    /** Releases the IME and clipboard listener before the display is hidden or backgrounded. */
    public void deactivateInputBridge() {
        mInputBridgeActive = false;
        removeCallbacks(mShowKeyboardAfterLongPress);
        endAllTouches();
        updateClipboardListener();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindowInsetsController();
            if (controller != null) controller.hide(WindowInsets.Type.ime());
        }
        if (mInputMethodManager != null && getWindowToken() != null)
            mInputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);

        clearFocus();
        setFocusableInTouchMode(false);
        setFocusable(false);
        if (mInputMethodManager != null) mInputMethodManager.restartInput(this);
    }

    public void refreshDisplaySize() {
        if (!mViewportUpdatesEnabled) return;
        Surface surface = getHolder().getSurface();
        if (surface != null && surface.isValid()) {
            surfaceChanged(surface);
            updateDisplaySize(getWidth(), getHeight());
        }
    }

    public void setViewportUpdatesEnabled(boolean enabled) {
        mViewportUpdatesEnabled = enabled;
    }

    private void updateDisplaySize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        mDisplaySize.set(width, height);
        mInputViewport.set(0, 0, width, height);
        mSourceLeft = 0;
        mSourceTop = 0;
        mSourceWidth = width;
        mSourceHeight = height;
        setViewport(0, 0, width, height, width, height);
        sendWindowChange(width, height,
            getDisplay() == null ? 60 : Math.max(30, Math.round(getDisplay().getRefreshRate())),
            "builtin");
        Log.i(TAG, "Viewport requested " + width + "x" + height
            + " connected=" + connected());
    }

    @SuppressWarnings("unused")
    private static void setRendererViewport(int left, int top, int width, int height,
                                            float sourceLeft, float sourceTop,
                                            float sourceWidth, float sourceHeight) {
        LorieView view = sActiveView.get();
        if (view == null) return;
        view.post(() -> {
            view.mInputViewport.set(left, top, left + Math.max(1, width), top + Math.max(1, height));
            view.mSourceLeft = sourceLeft;
            view.mSourceTop = sourceTop;
            view.mSourceWidth = Math.max(1, sourceWidth);
            view.mSourceHeight = Math.max(1, sourceHeight);
        });
    }

    @SuppressWarnings("unused")
    private void resetIme() {
        post(() -> {
            if (mInputMethodManager != null) mInputMethodManager.restartInput(this);
        });
    }

    public void showKeyboard() {
        if (!mInputBridgeActive) return;
        requestFocusFromTouch();
        postDelayed(() -> {
            if (mInputMethodManager == null || !mInputBridgeActive || !hasWindowFocus()) return;
            mInputMethodManager.restartInput(this);
            boolean shown = mInputMethodManager.showSoftInput(
                this, InputMethodManager.SHOW_IMPLICIT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindowInsetsController();
                if (controller != null) controller.show(WindowInsets.Type.ime());
            }
            Log.i(TAG, "Keyboard requested shown=" + shown);
        }, 80);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return mInputBridgeActive;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        if (!mInputBridgeActive) return null;
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            | InputType.TYPE_TEXT_FLAG_MULTI_LINE
            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN;
        return new BaseInputConnection(this, true) {
            private void flushCommittedText() {
                Editable content = getEditable();
                commitImeText(content);
                content.clear();
            }

            @Override
            public boolean finishComposingText() {
                super.finishComposingText();
                flushCommittedText();
                return true;
            }

            @Override
            public boolean commitText(CharSequence text, int newCursorPosition) {
                super.commitText(text, newCursorPosition);
                flushCommittedText();
                return true;
            }

            @Override
            public boolean deleteSurroundingText(int beforeLength, int afterLength) {
                for (int i = 0; i < beforeLength; i++) sendAndroidKey(KeyEvent.KEYCODE_DEL);
                for (int i = 0; i < afterLength; i++) sendAndroidKey(KeyEvent.KEYCODE_FORWARD_DEL);
                return true;
            }

            @Override
            public boolean sendKeyEvent(KeyEvent event) {
                return LorieView.this.sendKeyEvent(event.getScanCode(), event.getKeyCode(),
                    event.getAction() == KeyEvent.ACTION_DOWN, 0);
            }

            @Override
            public boolean performEditorAction(int actionCode) {
                sendAndroidKey(KeyEvent.KEYCODE_ENTER);
                return true;
            }
        };
    }

    private void commitImeText(CharSequence text) {
        if (text == null || text.length() == 0) return;
        String value = text.toString();
        int chunkStart = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character != '\n' && character != '\r') continue;
            if (index > chunkStart)
                sendTextEvent(value.substring(chunkStart, index).getBytes(StandardCharsets.UTF_8));
            sendAndroidKey(KeyEvent.KEYCODE_ENTER);
            if (character == '\r' && index + 1 < value.length() && value.charAt(index + 1) == '\n')
                index++;
            chunkStart = index + 1;
        }
        if (chunkStart < value.length())
            sendTextEvent(value.substring(chunkStart).getBytes(StandardCharsets.UTF_8));
    }

    private void sendAndroidKey(int keyCode) {
        sendKeyEvent(0, keyCode, true, 0);
        sendKeyEvent(0, keyCode, false, 0);
    }

    private void sendContentZoomShortcut(boolean zoomIn) {
        sendKeyEvent(0, KeyEvent.KEYCODE_CTRL_LEFT, true, 0);
        if (zoomIn) sendKeyEvent(0, KeyEvent.KEYCODE_SHIFT_LEFT, true, 0);
        int keyCode = zoomIn ? KeyEvent.KEYCODE_EQUALS : KeyEvent.KEYCODE_MINUS;
        sendKeyEvent(0, keyCode, true, 0);
        sendKeyEvent(0, keyCode, false, 0);
        if (zoomIn) sendKeyEvent(0, KeyEvent.KEYCODE_SHIFT_LEFT, false, 0);
        sendKeyEvent(0, KeyEvent.KEYCODE_CTRL_LEFT, false, 0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) return super.onKeyDown(keyCode, event);
        return sendKeyEvent(event.getScanCode(), keyCode, true, 0) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) return super.onKeyUp(keyCode, event);
        return sendKeyEvent(event.getScanCode(), keyCode, false, 0) || super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!connected() || !mInputBridgeActive) return true;
        requestFocus();
        int action = event.getActionMasked();
        boolean wasScaling = mScalingGesture;
        if (action == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() > 1) {
            mLastMultiTouchY = averageY(event);
            mMultiTouchScrollRemainder = 0f;
            mContentZooming = false;
        }
        mScaleDetector.onTouchEvent(event);
        if (mScalingGesture || wasScaling || event.getPointerCount() > 1
                || action == MotionEvent.ACTION_POINTER_DOWN
                || action == MotionEvent.ACTION_POINTER_UP) {
            removeCallbacks(mShowKeyboardAfterLongPress);
            if (event.getPointerCount() > 1 || action == MotionEvent.ACTION_POINTER_DOWN)
                endAllTouches();
            if (action == MotionEvent.ACTION_MOVE && event.getPointerCount() > 1)
                handleMultiTouchScroll(event);
            if (action == MotionEvent.ACTION_POINTER_UP
                || action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_CANCEL) {
                mMultiTouchScrollRemainder = 0f;
                mContentZooming = false;
            }
            return true;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            mTapDownX = event.getX();
            mTapDownY = event.getY();
            mTapMoved = false;
            postDelayed(mShowKeyboardAfterLongPress, ViewConfiguration.getLongPressTimeout());
        }

        if (action == MotionEvent.ACTION_MOVE) {
            if (Math.abs(event.getX() - mTapDownX) > mTouchSlop
                || Math.abs(event.getY() - mTapDownY) > mTouchSlop) {
                mTapMoved = true;
                removeCallbacks(mShowKeyboardAfterLongPress);
            }
            for (int index = 0; index < event.getPointerCount(); index++)
                sendNativeTouch(XI_TOUCH_UPDATE, event, index);
        } else if (action == MotionEvent.ACTION_CANCEL) {
            removeCallbacks(mShowKeyboardAfterLongPress);
            endAllTouches();
        } else {
            int index = event.getActionIndex();
            int id = event.getPointerId(index);
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                if (isSupportedTouchId(id)) {
                    mActiveTouches[id] = true;
                    sendNativeTouch(XI_TOUCH_BEGIN, event, index);
                }
            } else if (action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_POINTER_UP) {
                removeCallbacks(mShowKeyboardAfterLongPress);
                sendNativeTouch(XI_TOUCH_UPDATE, event, index);
                sendNativeTouch(XI_TOUCH_END, event, index);
                if (isSupportedTouchId(id)) mActiveTouches[id] = false;
            }
        }
        return true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        updateClipboardListener();
        if (hasWindowFocus && mInputBridgeActive) announceAndroidClipboard(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        deactivateInputBridge();
        super.onDetachedFromWindow();
    }

    private void updateClipboardListener() {
        boolean shouldRegister = mClipboardManager != null
            && mInputBridgeActive && hasWindowFocus();
        if (shouldRegister == mClipboardListenerRegistered) return;
        if (shouldRegister)
            mClipboardManager.addPrimaryClipChangedListener(mClipboardListener);
        else
            mClipboardManager.removePrimaryClipChangedListener(mClipboardListener);
        mClipboardListenerRegistered = shouldRegister;
    }

    private void handleClipboardChange() {
        if (mApplyingXClipboard) return;
        announceAndroidClipboard(false);
    }

    private void announceAndroidClipboard(boolean force) {
        if (!mInputBridgeActive || mClipboardManager == null || !connected()) return;
        ClipDescription description = mClipboardManager.getPrimaryClipDescription();
        if (!isTextClipboard(description)) return;

        long timestamp = description.getTimestamp();
        if (!force && timestamp > 0 && timestamp <= mLastClipboardTimestamp) return;
        String text = readAndroidClipboard();
        if (text == null) return;
        if (!force && text.equals(mLastAnnouncedClipboardText)) return;

        mLastClipboardTimestamp = timestamp > 0
            ? timestamp : System.currentTimeMillis();
        mLastAnnouncedClipboardText = text;
        sendClipboardAnnounce();
        Log.d(TAG, "Android clipboard announced to X11");
    }

    private String readAndroidClipboard() {
        if (mClipboardManager == null || !mClipboardManager.hasPrimaryClip()) return null;
        ClipData clip = mClipboardManager.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return null;
        CharSequence value = clip.getItemAt(0).coerceToText(getContext());
        return value == null ? null : value.toString();
    }

    private static boolean isTextClipboard(ClipDescription description) {
        return description != null
            && (description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                || description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML));
    }

    /** Called by the native renderer when an X11 client owns CLIPBOARD. */
    @SuppressWarnings("unused")
    private void setClipboardText(String text) {
        post(() -> {
            if (mClipboardManager == null) return;
            String value = text == null ? "" : text;
            if (value.equals(readAndroidClipboard())) return;

            mApplyingXClipboard = true;
            mLastClipboardTimestamp = System.currentTimeMillis() + 250;
            mLastAnnouncedClipboardText = value;
            try {
                mClipboardManager.setPrimaryClip(
                    ClipData.newPlainText("Linux clipboard", value));
            } finally {
                post(() -> mApplyingXClipboard = false);
            }
            Log.d(TAG, "X11 clipboard copied to Android");
        });
    }

    /** Called by the native renderer after Android announces new clipboard content. */
    @SuppressWarnings("unused")
    private void requestClipboard() {
        post(() -> {
            String text = mInputBridgeActive ? readAndroidClipboard() : null;
            sendClipboardEvent((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
        });
    }

    private void sendNativeTouch(int action, MotionEvent event, int index) {
        int id = event.getPointerId(index);
        if (!isSupportedTouchId(id)) return;
        int x = Math.round(mapX(event.getX(index)));
        int y = Math.round(mapY(event.getY(index)));
        mLastTouchX[id] = x;
        mLastTouchY[id] = y;
        sendTouchEvent(action, id, x, y);
    }

    private void handleMultiTouchScroll(MotionEvent event) {
        float currentY = averageY(event);
        float deltaY = currentY - mLastMultiTouchY;
        mLastMultiTouchY = currentY;
        if (mContentZooming) {
            mMultiTouchScrollRemainder = 0f;
            return;
        }

        mMultiTouchScrollRemainder += deltaY;
        while (Math.abs(mMultiTouchScrollRemainder) >= mScrollStep) {
            boolean scrollDown = mMultiTouchScrollRemainder < 0;
            sendMouseWheel(event, scrollDown);
            mMultiTouchScrollRemainder += scrollDown ? mScrollStep : -mScrollStep;
        }
    }

    private void sendMouseWheel(MotionEvent event, boolean scrollDown) {
        float averageX = 0f;
        for (int index = 0; index < event.getPointerCount(); index++)
            averageX += event.getX(index);
        averageX /= Math.max(1, event.getPointerCount());
        float x = mapX(averageX);
        float y = mapY(averageY(event));
        int button = scrollDown ? MOUSE_WHEEL_DOWN : MOUSE_WHEEL_UP;
        sendMouseEvent(x, y, button, true, false);
        sendMouseEvent(x, y, button, false, false);
    }

    private static float averageY(MotionEvent event) {
        float value = 0f;
        for (int index = 0; index < event.getPointerCount(); index++)
            value += event.getY(index);
        return value / Math.max(1, event.getPointerCount());
    }

    private void endAllTouches() {
        for (int id = 0; id < mActiveTouches.length; id++) {
            if (!mActiveTouches[id]) continue;
            sendTouchEvent(XI_TOUCH_END, id, mLastTouchX[id], mLastTouchY[id]);
            mActiveTouches[id] = false;
        }
    }

    private static boolean isSupportedTouchId(int id) {
        return id >= 0 && id < MAX_TOUCH_POINTS;
    }

    private float mapX(float x) {
        float normalized = (x - mInputViewport.left) / Math.max(1f, mInputViewport.width());
        return clamp(mSourceLeft + normalized * mSourceWidth, 0, mDisplaySize.x - 1);
    }

    private float mapY(float y) {
        float normalized = (y - mInputViewport.top) / Math.max(1f, mInputViewport.height());
        return clamp(mSourceTop + normalized * mSourceHeight, 0, mDisplaySize.y - 1);
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private native void nativeInit();
    private native void surfaceChanged(Surface surface);
    private native void setFiltering(int filtering);
    static native void connect(int fd);
    public static native boolean connected();
    static native void startLogcat(int fd);
    static native void setClipboardSyncEnabled(boolean enabled, boolean ignored);
    public native void sendClipboardAnnounce();
    public native void sendClipboardEvent(byte[] text);
    static native void sendWindowChange(int width, int height, int framerate, String name);
    static native void setViewport(int x, int y, int width, int height,
                                   int expectedWidth, int expectedHeight);
    public native void sendMouseEvent(float x, float y, int whichButton,
                                      boolean buttonDown, boolean relative);
    public native void sendTouchEvent(int action, int id, int x, int y);
    public native void sendStylusEvent(float x, float y, int pressure, int tiltX, int tiltY,
                                       int orientation, int buttons, boolean eraser, boolean mouseMode);
    public static native void requestStylusEnabled(boolean enabled);
    public native boolean sendKeyEvent(int scanCode, int keyCode, boolean keyDown, int flags);
    public native void sendTextEvent(byte[] text);
    public static native boolean requestConnection();
    // Retained for the native display library's RegisterNatives ABI contract.
    private native void setRendererZoom(int percent);

    static {
        System.loadLibrary("ominal-display");
    }
}
