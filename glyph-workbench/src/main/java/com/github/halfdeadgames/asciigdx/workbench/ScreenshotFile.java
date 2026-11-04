package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.ScreenUtils;

public final class ScreenshotFile {

    private static final int BYTES_PER_PIXEL = 4;
    private static final int ALPHA_OFFSET = 3;
    private static final byte OPAQUE = (byte) 255;

    private ScreenshotFile() {
    }

    public static void writeBackBuffer(String path) {
        int width = Gdx.graphics.getBackBufferWidth();
        int height = Gdx.graphics.getBackBufferHeight();
        byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, width, height, true);
        makeOpaque(pixels);
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        try {
            pixmap.getPixels().put(pixels).rewind();
            PixmapIO.writePNG(Gdx.files.absolute(path), pixmap);
        } finally {
            pixmap.dispose();
        }
    }

    // Translucent draws leave partial alpha in the framebuffer. The window ignores it, but image viewers would not.
    static void makeOpaque(byte[] rgbaPixels) {
        for (int i = ALPHA_OFFSET; i < rgbaPixels.length; i += BYTES_PER_PIXEL) {
            rgbaPixels[i] = OPAQUE;
        }
    }
}
