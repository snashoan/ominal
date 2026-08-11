package com.ominal.app;

import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class OminalUrlRequestBridgeTest {
    @Test
    public void acceptsOnlyWebUrls() {
        Uri uri = OminalUrlRequestBridge.parseUrl("https://example.com/login?code=abc");

        assertEquals("https://example.com/login?code=abc", uri.toString());
        assertNull(OminalUrlRequestBridge.parseUrl("file:///root/.ssh/id_ed25519"));
        assertNull(OminalUrlRequestBridge.parseUrl("javascript:alert(1)"));
        assertNull(OminalUrlRequestBridge.parseUrl("https://example.com/\nnext"));
    }
}
