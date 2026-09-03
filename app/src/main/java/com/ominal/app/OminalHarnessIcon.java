package com.ominal.app;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

/** Decodes bounded local harness artwork without allowing untrusted vector content. */
final class OminalHarnessIcon {
    private static final int MAX_DIMENSION = 1024;
    private static final int TARGET_DIMENSION = 128;

    private OminalHarnessIcon() {}

    @Nullable
    static Drawable load(@NonNull Resources resources,
                         @Nullable OminalHarnessManifest manifest,
                         boolean preferMonochrome) {
        File file = manifest == null ? null : manifest.iconFile(preferMonochrome);
        if (file == null) return null;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0
            || bounds.outWidth > MAX_DIMENSION || bounds.outHeight > MAX_DIMENSION) return null;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        int largest = Math.max(bounds.outWidth, bounds.outHeight);
        while (largest / options.inSampleSize > TARGET_DIMENSION * 2)
            options.inSampleSize *= 2;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        return bitmap == null ? null : new BitmapDrawable(resources, bitmap);
    }
}
