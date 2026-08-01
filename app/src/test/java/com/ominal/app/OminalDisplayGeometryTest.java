package com.ominal.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OminalDisplayGeometryTest {
    @Test
    public void preservesExactPortraitContentBounds() {
        OminalDisplayGeometry geometry = OminalDisplayGeometry.fromBounds(
            1080, 2260, 1080, 2400, 440);

        assertEquals(1080, geometry.widthPixels);
        assertEquals(2260, geometry.heightPixels);
        assertEquals(440, geometry.densityDpi);
        assertEquals("1080x2260x24", geometry.toX11Spec());
    }

    @Test
    public void normalizesLandscapeFallbackAndClampsTouch() {
        OminalDisplayGeometry geometry = OminalDisplayGeometry.fromBounds(
            0, 0, 2400, 1080, 420);

        assertEquals("1080x2400x24", geometry.toX11Spec());
        assertEquals(0, geometry.mapTouchX(-20, 1080));
        assertEquals(1079, geometry.mapTouchX(1200, 1080));
        assertEquals(1200, geometry.mapTouchY(1200, 2400));
    }

    @Test
    public void derivesFallbackFromNavigationSafePhoneViewport() {
        OminalDisplayGeometry geometry = OminalDisplayGeometry.fromViewport(
            0, 0, 1080, 2340, 0, 0, 0, 80, 440);

        assertEquals("1080x2260x24", geometry.toX11Spec());
    }

    @Test
    public void removesNavigationInsetFromKeyboardOcclusion() {
        assertEquals(920, OminalDisplayGeometry.keyboardOcclusion(1000, 80));
        assertEquals(0, OminalDisplayGeometry.keyboardOcclusion(0, 80));
    }

    @Test
    public void appliesOnlySystemInsetNotAlreadyExcludedByDecor() {
        assertEquals(44, OminalDisplayGeometry.unconsumedSystemInset(44, 2340, 2340));
        assertEquals(0, OminalDisplayGeometry.unconsumedSystemInset(44, 2340, 2296));
    }
}
