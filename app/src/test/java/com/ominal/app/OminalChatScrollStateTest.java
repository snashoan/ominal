package com.ominal.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OminalChatScrollStateTest {
    @Test
    public void userCanReadEarlierMessagesWhileOutputStreams() {
        OminalChatScrollState state = new OminalChatScrollState();

        state.onUserScroll(500, 800, 2400, 64);

        assertFalse(state.shouldFollowLatest());
    }

    @Test
    public void reachingBottomResumesFollowingLatestOutput() {
        OminalChatScrollState state = new OminalChatScrollState();
        state.onUserScroll(500, 800, 2400, 64);

        state.onUserScroll(1540, 800, 2400, 64);

        assertTrue(state.shouldFollowLatest());
    }

    @Test
    public void sendingMessageRestoresLatestPosition() {
        OminalChatScrollState state = new OminalChatScrollState();
        state.onUserScroll(500, 800, 2400, 64);

        state.followLatest();

        assertTrue(state.shouldFollowLatest());
    }
}
