package com.ominal.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class OminalRuntimeArchitectureTest {

    @Test
    public void detectUsesAndroidPreferenceOrder() {
        assertEquals(OminalRuntimeArchitecture.Profile.X86_64,
            OminalRuntimeArchitecture.detect(new String[]{"x86_64", "arm64-v8a"}));
        assertEquals(OminalRuntimeArchitecture.Profile.ARM64,
            OminalRuntimeArchitecture.detect(new String[]{"arm64-v8a", "x86_64"}));
    }

    @Test
    public void architectureNamesMatchLinuxPackages() {
        assertEquals("arm64", OminalRuntimeArchitecture.Profile.ARM64.linuxArchitecture);
        assertEquals("amd64", OminalRuntimeArchitecture.Profile.X86_64.linuxArchitecture);
    }

    @Test
    public void unknownArchitectureIsRejected() {
        assertNull(OminalRuntimeArchitecture.detect(new String[]{"armeabi-v7a"}));
        assertNull(OminalRuntimeArchitecture.detect(null));
    }

    @Test
    public void packagedPayloadSupportIsExplicit() {
        assertTrue(OminalRuntimeArchitecture.hasPackagedRuntime(
            OminalRuntimeArchitecture.Profile.ARM64));
        assertFalse(OminalRuntimeArchitecture.hasPackagedRuntime(
            OminalRuntimeArchitecture.Profile.X86_64));
    }
}
