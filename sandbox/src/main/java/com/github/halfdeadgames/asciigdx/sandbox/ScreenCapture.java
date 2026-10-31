package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.github.tommyettinger.anim8.AnimatedGif;
import com.github.tommyettinger.anim8.Dithered;
import com.github.tommyettinger.anim8.PaletteReducer;

import java.nio.ByteBuffer;
import java.util.zip.Deflater;

/** Saves PNG screenshots and GIF recordings of the back buffer. Call {@link #onFrameRendered()} at the end of every render. */
public class ScreenCapture {
    public static final int MAX_GIF_FRAMES = 120;
    private static final int GIF_FRAMES_PER_SECOND = 30;
    private static final int PALETTE_SIZE = 256;
    private static final double PALETTE_THRESHOLD = 100.0;

    private final FileHandle outputDirectory;
    private final Array<Pixmap> recordedFrames = new Array<>(MAX_GIF_FRAMES);
    private String pendingScreenshotName;
    private boolean recording;

    public ScreenCapture(FileHandle outputDirectory) {
        this.outputDirectory = outputDirectory;
        outputDirectory.mkdirs();
    }

    public FileHandle getOutputDirectory() {
        return outputDirectory;
    }

    public boolean isRecording() {
        return recording;
    }

    public int getRecordedFrameCount() {
        return recordedFrames.size;
    }

    /** The screenshot is taken at the end of the current frame, once the frame has been drawn. */
    public void requestScreenshot(String fileName) {
        pendingScreenshotName = fileName;
    }

    public void startRecording() {
        clearRecordedFrames();
        recording = true;
    }

    public void toggleManualRecording() {
        if (recording) {
            stopRecording("recording-" + TimeUtils.millis() + ".gif");
        } else {
            startRecording();
        }
    }

    public void requestTimestampedScreenshot() {
        requestScreenshot("screenshot-" + TimeUtils.millis() + ".png");
    }

    public void onFrameRendered() {
        if (pendingScreenshotName != null) {
            savePng(pendingScreenshotName);
            pendingScreenshotName = null;
        }
        if (recording) {
            recordedFrames.add(readBackBufferTopDown());
            if (recordedFrames.size >= MAX_GIF_FRAMES) {
                stopRecording("recording-" + TimeUtils.millis() + ".gif");
            }
        }
    }

    public void stopRecording(String fileName) {
        recording = false;
        if (recordedFrames.size > 0) {
            writeGif(outputDirectory.child(fileName));
        }
        clearRecordedFrames();
    }

    private void savePng(String fileName) {
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0,
            Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            PixmapIO.writePNG(outputDirectory.child(fileName), pixmap, Deflater.DEFAULT_COMPRESSION, true);
        } finally {
            pixmap.dispose();
        }
    }

    private Pixmap readBackBufferTopDown() {
        Pixmap bottomUp = Pixmap.createFromFrameBuffer(0, 0,
            Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        int width = bottomUp.getWidth();
        int height = bottomUp.getHeight();
        Pixmap topDown = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        ByteBuffer source = bottomUp.getPixels();
        ByteBuffer target = topDown.getPixels();
        int rowBytes = width * 4;
        byte[] row = new byte[rowBytes];
        for (int y = 0; y < height; y++) {
            source.position((height - 1 - y) * rowBytes);
            source.get(row);
            target.position(y * rowBytes);
            target.put(row);
        }
        source.rewind();
        target.rewind();
        bottomUp.dispose();
        return topDown;
    }

    private void writeGif(FileHandle file) {
        AnimatedGif gif = new AnimatedGif();
        gif.setFlipY(false);
        gif.setDitherAlgorithm(Dithered.DitherAlgorithm.NONE);
        gif.palette = new PaletteReducer();
        gif.palette.analyze(recordedFrames, PALETTE_THRESHOLD, PALETTE_SIZE);
        gif.write(file, recordedFrames, GIF_FRAMES_PER_SECOND);
    }

    private void clearRecordedFrames() {
        for (int i = 0; i < recordedFrames.size; i++) {
            recordedFrames.get(i).dispose();
        }
        recordedFrames.clear();
    }

    public void dispose() {
        clearRecordedFrames();
    }
}
