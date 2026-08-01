package com.ominal.x11;

interface IOminalDisplayService {
    ParcelFileDescriptor openRendererConnection(
        String temporaryDirectory,
        String xkbConfigRoot,
        int densityDpi
    );
}
