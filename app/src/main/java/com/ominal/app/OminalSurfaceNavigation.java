package com.ominal.app;

/** Direction and completion rules for the History <-> Chat <-> Computer surface. */
final class OminalSurfaceNavigation {
    static final int NONE = 0;
    static final int HISTORY = 1;
    static final int COMPUTER = 2;
    static final int CHAT = 3;
    static final int CHAT_FROM_HISTORY = 4;

    private OminalSurfaceNavigation() {
    }

    static int directionFor(boolean computerVisible, boolean twoFingerGesture,
                            float deltaX, float deltaY, int touchSlop) {
        if (Math.abs(deltaX) < touchSlop || Math.abs(deltaX) <= Math.abs(deltaY) * 1.25f)
            return NONE;
        if (computerVisible)
            return twoFingerGesture && deltaX > 0f ? CHAT : NONE;
        return deltaX > 0f ? HISTORY : COMPUTER;
    }

    static int directionFromHistory(float deltaX, float deltaY, int touchSlop) {
        if (Math.abs(deltaX) < touchSlop || Math.abs(deltaX) <= Math.abs(deltaY) * 1.25f)
            return NONE;
        return deltaX < 0f ? CHAT_FROM_HISTORY : NONE;
    }

    static boolean shouldCommit(int direction, float deltaX, float width, float velocityX,
                                float minimumVelocity) {
        if (direction == NONE || width <= 0f) return false;
        boolean movesLeft = direction == COMPUTER || direction == CHAT_FROM_HISTORY;
        float directionalDistance = movesLeft ? -deltaX : deltaX;
        float directionalVelocity = movesLeft ? -velocityX : velocityX;
        return directionalDistance >= width * 0.22f
            || (directionalDistance > 0f && directionalVelocity >= minimumVelocity);
    }

    static long settleDuration(float current, float target, float width) {
        if (width <= 0f) return 180L;
        float fraction = Math.min(1f, Math.abs(target - current) / width);
        return 120L + Math.round(100f * fraction);
    }
}
