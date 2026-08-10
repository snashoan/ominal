package com.ominal.app;

/** Tracks whether streaming chat updates should continue following the latest content. */
final class OminalChatScrollState {
    private boolean mFollowLatest = true;

    void reset() {
        mFollowLatest = true;
    }

    void followLatest() {
        mFollowLatest = true;
    }

    void onUserScroll(int scrollY, int viewportHeight, int contentHeight, int thresholdPx) {
        int remaining = Math.max(0, contentHeight - Math.max(0, viewportHeight)
            - Math.max(0, scrollY));
        mFollowLatest = remaining <= Math.max(0, thresholdPx);
    }

    boolean shouldFollowLatest() {
        return mFollowLatest;
    }
}
