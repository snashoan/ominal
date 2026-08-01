package com.ominal.app;

import android.content.Intent;

import com.ominal.shared.runtime.OminalConstants;
import com.ominal.shared.runtime.OminalConstants.OMINAL_APP.OMINAL_SERVICE;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class OminalProotTerminalTest {

    private static final String HOME = OminalConstants.OMINAL_HOME_DIR_PATH;

    @Test
    public void configuresTerminalIntentForCanonicalProotShell() {
        Intent intent = new Intent();
        String workspace = HOME + "/.oringutan/chats/chat-1/workspace";

        OminalProotTerminal.configureIntent(intent, workspace);

        assertEquals(OminalProotTerminal.EXECUTABLE_PATH, intent.getData().getPath());
        assertEquals(workspace, intent.getStringExtra(OMINAL_SERVICE.EXTRA_WORKDIR));
        assertArrayEquals(new String[]{workspace},
            intent.getStringArrayExtra(OMINAL_SERVICE.EXTRA_ARGUMENTS));
    }

    @Test
    public void mapsAndroidAndGuestAliasesToHostWorkspace() {
        assertEquals(HOME + "/project",
            OminalProotTerminal.normalizeWorkspace("/data/user/0/com.ominal/files/home/project"));
        assertEquals(HOME + "/workspace",
            OminalProotTerminal.normalizeWorkspace("/root/workspace"));
        assertEquals(HOME + "/workspace",
            OminalProotTerminal.normalizeWorkspace("/sdcard/Download"));
    }
}
