package com.ominal.app;

/** Exact portrait display bounds shared by Android, the X server, and agent tools. */
public final class OminalDisplayGeometry {
    public static final int PIXEL_DEPTH = 24;
    public static final int MAX_GUEST_DENSITY_DPI = 640;

    public final int widthPixels;
    public final int heightPixels;
    public final int densityDpi;

    private OminalDisplayGeometry(int widthPixels, int heightPixels, int densityDpi) {
        this.widthPixels = Math.max(1, widthPixels);
        this.heightPixels = Math.max(1, heightPixels);
        this.densityDpi = Math.min(MAX_GUEST_DENSITY_DPI, Math.max(160, densityDpi));
    }

    public static OminalDisplayGeometry fromBounds(int contentWidth, int contentHeight,
                                                    int fallbackWidth, int fallbackHeight,
                                                    int densityDpi) {
        int width = contentWidth > 0 ? contentWidth : fallbackWidth;
        int height = contentHeight > 0 ? contentHeight : fallbackHeight;
        if (width > height) {
            int previousWidth = width;
            width = height;
            height = previousWidth;
        }
        return new OminalDisplayGeometry(width, height, densityDpi);
    }

    public static OminalDisplayGeometry fromViewport(int contentWidth, int contentHeight,
                                                      int windowWidth, int windowHeight,
                                                      int insetLeft, int insetTop,
                                                      int insetRight, int insetBottom,
                                                      int densityDpi) {
        int fallbackWidth = Math.max(1,
            windowWidth - Math.max(0, insetLeft) - Math.max(0, insetRight));
        int fallbackHeight = Math.max(1,
            windowHeight - Math.max(0, insetTop) - Math.max(0, insetBottom));
        return fromBounds(contentWidth, contentHeight, fallbackWidth, fallbackHeight, densityDpi);
    }

    public static int keyboardOcclusion(int imeBottom, int navigationBottom) {
        return Math.max(0, imeBottom - Math.max(0, navigationBottom));
    }

    public static int fullscreenTopInset(int minimumInset, int cutoutTop) {
        return Math.max(Math.max(0, minimumInset), Math.max(0, cutoutTop));
    }

    public static int interactiveBottomInset(int systemBottom, int gestureBottom,
                                               int cutoutBottom, int imeBottom) {
        return Math.max(Math.max(0, imeBottom),
            Math.max(Math.max(0, systemBottom),
                Math.max(Math.max(0, gestureBottom), Math.max(0, cutoutBottom))));
    }

    public static int unconsumedSystemInset(int systemBottom, int windowHeight,
                                             int decorHeight) {
        int alreadyExcluded = Math.max(0, windowHeight - Math.max(0, decorHeight));
        return remainingInset(systemBottom, alreadyExcluded);
    }

    public static int remainingInset(int requestedInset, int alreadyExcluded) {
        return Math.max(0, Math.max(0, requestedInset) - Math.max(0, alreadyExcluded));
    }

    public String toX11Spec() {
        return widthPixels + "x" + heightPixels + "x" + PIXEL_DEPTH;
    }

    public int mapTouchX(float x, int viewWidth) {
        return mapCoordinate(x, viewWidth, widthPixels);
    }

    public int mapTouchY(float y, int viewHeight) {
        return mapCoordinate(y, viewHeight, heightPixels);
    }

    private static int mapCoordinate(float coordinate, int viewSize, int displaySize) {
        if (viewSize <= 0) return 0;
        int mapped = Math.round(coordinate * displaySize / viewSize);
        return Math.max(0, Math.min(displaySize - 1, mapped));
    }
}
