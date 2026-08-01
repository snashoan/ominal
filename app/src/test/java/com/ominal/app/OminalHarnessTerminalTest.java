package com.ominal.app;

import android.content.Intent;

import com.ominal.shared.runtime.OminalConstants;
import com.ominal.shared.runtime.OminalConstants.OMINAL_APP.OMINAL_SERVICE;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class OminalHarnessTerminalTest {

    private static final String HOME = OminalConstants.OMINAL_HOME_DIR_PATH;

    @Test
    public void configuresValidatedHarnessSession() {
        Intent intent = new Intent();
        String workspace = HOME + "/.ominal/chats/chat-1/workspace";

        OminalHarnessTerminal.configureIntent(
            intent, OminalHarnessTerminal.CLAUDE_CODE_ID, workspace);

        assertEquals(OminalHarnessTerminal.EXECUTABLE_PATH, intent.getData().getPath());
        assertEquals(workspace, intent.getStringExtra(OMINAL_SERVICE.EXTRA_WORKDIR));
        assertArrayEquals(new String[]{"claude-code", workspace},
            intent.getStringArrayExtra(OMINAL_SERVICE.EXTRA_ARGUMENTS));
    }

    @Test
    public void passesAntigravitySessionOptionsWithoutShellParsing() {
        Intent intent = new Intent();
        String workspace = HOME + "/.ominal/chats/chat-2/workspace";

        OminalHarnessTerminal.configureIntent(intent,
            OminalHarnessTerminal.ANTIGRAVITY_ID, workspace,
            "gemini-3.6-flash-low", "high", "First request");

        assertArrayEquals(new String[]{
                "antigravity", workspace,
                "--model", "gemini-3.6-flash-low",
                "--effort", "high",
                "--prompt-interactive", "First request"
            },
            intent.getStringArrayExtra(OMINAL_SERVICE.EXTRA_ARGUMENTS));
        assertEquals("ominal-proot-chat-2-antigravity",
            OminalHarnessTerminal.sessionName("chat-2", "antigravity"));
    }

    @Test
    public void rejectsUnknownHarnessBeforeItReachesTheShell() {
        assertThrows(IllegalArgumentException.class,
            () -> OminalHarnessTerminal.configureIntent(
                new Intent(), "claude-code; echo unsafe", HOME + "/workspace"));
    }

    @Test
    public void exposesOnlyImplementedTerminalHarnesses() {
        assertTrue(OminalHarnessTerminal.isSupported("codex"));
        assertTrue(OminalHarnessTerminal.isSupported("claude-code"));
        assertTrue(OminalHarnessTerminal.isSupported("antigravity"));
        assertFalse(OminalHarnessTerminal.isSupported("unknown"));
    }
}
