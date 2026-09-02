package com.ominal.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class OminalUserProfileTest {
    @Test
    public void roundTripsProviderNeutralProfile() throws Exception {
        OminalUserProfile profile = new OminalUserProfile(
            "Ada Lovelace", "Ada", "English", "Europe/London", "Builds analytical engines.");
        OminalUserProfile restored = OminalUserProfile.fromJson(profile.toJson());

        assertEquals("Ada Lovelace", restored.displayName);
        assertEquals("Ada", restored.preferredName);
        assertEquals("English", restored.language);
        assertEquals("Europe/London", restored.locationOrTimeZone);
        assertEquals("Builds analytical engines.", restored.about);
        assertEquals("shared-across-runtimes", profile.toJson().getString("scope"));
        assertTrue(profile.toJson().getBoolean("available"));
    }

    @Test
    public void normalizesUntrustedProfileText() {
        OminalUserProfile profile = new OminalUserProfile(
            "  Ada\u0000  Lovelace  ", "", "", "", "Line one\n\nLine two");

        assertEquals("Ada Lovelace", profile.displayName);
        assertEquals("Line one\nLine two", profile.about);
        assertFalse(profile.isEmpty());
        assertEquals("Ada Lovelace", profile.label());
        assertTrue(OminalUserProfile.empty().isEmpty());
    }
}
