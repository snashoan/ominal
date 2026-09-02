package com.ominal.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OminalSurfaceNavigationTest {
    @Test
    public void chatMapsHorizontalDirectionsToAdjacentSurfaces() {
        assertEquals(OminalSurfaceNavigation.HISTORY,
            OminalSurfaceNavigation.directionFor(false, false, 80f, 8f, 16));
        assertEquals(OminalSurfaceNavigation.COMPUTER,
            OminalSurfaceNavigation.directionFor(false, false, -80f, 8f, 16));
    }

    @Test
    public void settleDurationTracksRemainingDistanceWithoutBecomingAbrupt() {
        assertEquals(220L, OminalSurfaceNavigation.settleDuration(0f, 1000f, 1000f));
        assertEquals(170L, OminalSurfaceNavigation.settleDuration(500f, 1000f, 1000f));
        assertEquals(120L, OminalSurfaceNavigation.settleDuration(1000f, 1000f, 1000f));
    }

    @Test
    public void verticalIntentDoesNotNavigate() {
        assertEquals(OminalSurfaceNavigation.NONE,
            OminalSurfaceNavigation.directionFor(false, false, 30f, 90f, 16));
    }

    @Test
    public void computerReturnRequiresTwoFingers() {
        assertEquals(OminalSurfaceNavigation.CHAT,
            OminalSurfaceNavigation.directionFor(true, true, 90f, 4f, 16));
        assertEquals(OminalSurfaceNavigation.NONE,
            OminalSurfaceNavigation.directionFor(true, false, 90f, 4f, 16));
    }

    @Test
    public void historyOnlyReturnsTowardChat() {
        assertEquals(OminalSurfaceNavigation.CHAT_FROM_HISTORY,
            OminalSurfaceNavigation.directionFromHistory(-80f, 4f, 16));
        assertEquals(OminalSurfaceNavigation.NONE,
            OminalSurfaceNavigation.directionFromHistory(80f, 4f, 16));
    }

    @Test
    public void distanceOrDirectionalVelocityCommits() {
        assertTrue(OminalSurfaceNavigation.shouldCommit(
            OminalSurfaceNavigation.COMPUTER, -240f, 1000f, -200f, 900f));
        assertTrue(OminalSurfaceNavigation.shouldCommit(
            OminalSurfaceNavigation.HISTORY, 80f, 1000f, 1200f, 900f));
        assertTrue(OminalSurfaceNavigation.shouldCommit(
            OminalSurfaceNavigation.CHAT_FROM_HISTORY, -240f, 1000f, -200f, 900f));
        assertFalse(OminalSurfaceNavigation.shouldCommit(
            OminalSurfaceNavigation.CHAT, 80f, 1000f, 200f, 900f));
    }
}
