package com.ominal.x11;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.SurfaceView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@RunWith(RobolectricTestRunner.class)
public class LorieViewNativeContractTest {

    @Test
    public void rendererZoomEntryPointRemainsAvailableForNativeRegistration() throws Exception {
        Class<?> type = Class.forName(
            "com.ominal.x11.LorieView", false, getClass().getClassLoader());
        Method method = type.getDeclaredMethod("setRendererZoom", int.class);

        assertTrue(Modifier.isNative(method.getModifiers()));
        assertFalse(Modifier.isStatic(method.getModifiers()));
    }

    @Test
    public void nativeDisplayUsesStableSurfaceRenderer() throws Exception {
        Class<?> type = Class.forName(
            "com.ominal.x11.LorieView", false, getClass().getClassLoader());

        assertTrue(SurfaceView.class.isAssignableFrom(type));
    }
}
