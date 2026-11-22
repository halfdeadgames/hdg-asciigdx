package com.github.halfdeadgames.asciigdx.skindesigner;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.TimeUtils;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;

/** Saves a PNG of the back buffer at the end of the frame in which it was requested. */
class ScreenshotWriter {
    private static final int BYTES_PER_PIXEL = 4;
    private static final int ALPHA_OFFSET = 3;

    private final FileHandle outputDirectory;
    private String pendingFileName;

    ScreenshotWriter(FileHandle outputDirectory) {
        this.outputDirectory = outputDirectory;
        outputDirectory.mkdirs();
    }

    FileHandle getOutputDirectory() {
        return outputDirectory;
    }

    void requestScreenshot(String fileName) {
        pendingFileName = fileName;
    }

    void requestTimestampedScreenshot() {
        requestScreenshot("screenshot-" + TimeUtils.millis() + ".png");
    }

    /** Call at the end of every render. */
    void onFrameRendered() {
        if (pendingFileName == null) {
            return;
        }
        savePng(pendingFileName);
        pendingFileName = null;
    }

    private void savePng(String fileName) {
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0,
                Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            makeOpaque(pixmap);
            PixmapIO.writePNG(outputDirectory.child(fileName), pixmap,
                    Deflater.DEFAULT_COMPRESSION, true);
        } finally {
            pixmap.dispose();
        }
    }

    // Blending leaves the framebuffer alpha below 1 while the screen ignores it, so captures
    // would look partly transparent.
    private static void makeOpaque(Pixmap rgba8888Pixmap) {
        ByteBuffer pixels = rgba8888Pixmap.getPixels();
        int byteCount = rgba8888Pixmap.getWidth() * rgba8888Pixmap.getHeight() * BYTES_PER_PIXEL;
        for (int alphaIndex = ALPHA_OFFSET; alphaIndex < byteCount;
                alphaIndex += BYTES_PER_PIXEL) {
            pixels.put(alphaIndex, (byte) 0xFF);
        }
    }
}
