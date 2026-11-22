package com.github.halfdeadgames.asciigdx.skindesigner;

import com.badlogic.gdx.Gdx;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * Unattended run: for every example of the showcase and the open picker, waits for warm-up,
 * measures the render time, saves a screenshot, and writes a perf line, then exits. Driven by
 * {@link #onFrameRendered(long, long)}.
 */
class CaptureRun {
    private static final String SCENE_ID = "ui-showcase";
    private static final String PERF_TAG = "CaptureRun";
    private static final String PERF_FILE = "perf.txt";
    private static final int WARM_UP_FRAMES = 30;
    private static final float NANOS_PER_MILLISECOND = 1_000_000f;

    private enum Phase {
        STARTING, WARMING_UP, MEASURING, SAVING, DONE
    }

    private final UiShowcaseScene showcase;
    private final ScreenshotWriter screenshots;
    private final Runnable statusRefresher;
    private final BooleanSupplier windowSettled;
    private final List<String> variants;
    private final float sampleSeconds;
    private final float warmUpSeconds;
    private final float[] sampleMeansMs;
    private final float[] gpuWaitMeansMs;
    private final float[] cpuMeansMs;
    private final StringBuilder report = new StringBuilder();
    private Phase phase = Phase.STARTING;
    private int variantIndex;
    private int framesInPhase;
    private float secondsInPhase;
    private int sampleCount;
    private int windowFrames;
    private float windowSeconds;
    private long windowRenderNanos;
    private long windowGpuWaitNanos;
    private int totalMeasuredFrames;

    CaptureRun(UiShowcaseScene showcase, ScreenshotWriter screenshots, CaptureSettings settings,
            Runnable statusRefresher, BooleanSupplier windowSettled) {
        this.windowSettled = windowSettled;
        this.showcase = showcase;
        this.screenshots = screenshots;
        this.statusRefresher = statusRefresher;
        variants = showcase.captureVariants();
        sampleSeconds = settings.sampleSeconds();
        warmUpSeconds = settings.warmUpSeconds();
        sampleMeansMs = new float[settings.samples()];
        gpuWaitMeansMs = new float[settings.samples()];
        cpuMeansMs = new float[settings.samples()];
    }

    /**
     * Call at the end of render, after {@link ScreenshotWriter#onFrameRendered()}.
     *
     * @param renderNanos nanoseconds the showcase's render work took this frame, GPU work included
     * @param gpuWaitNanos the part of renderNanos spent waiting for the GPU to finish after all
     *        draw calls were submitted; the rest is CPU time
     */
    void onFrameRendered(long renderNanos, long gpuWaitNanos) {
        switch (phase) {
            case STARTING -> {
                if (windowSettled.getAsBoolean()) {
                    startVariant(0);
                }
            }
            case WARMING_UP -> warmUp();
            case MEASURING -> measure(renderNanos, gpuWaitNanos);
            case SAVING -> openNextVariantOrFinish();
            case DONE -> {
            }
        }
    }

    private void startVariant(int index) {
        variantIndex = index;
        showcase.showCaptureVariant(variants.get(index));
        statusRefresher.run();
        framesInPhase = 0;
        secondsInPhase = 0f;
        phase = Phase.WARMING_UP;
    }

    private void warmUp() {
        framesInPhase++;
        secondsInPhase += Gdx.graphics.getDeltaTime();
        if (framesInPhase >= WARM_UP_FRAMES && secondsInPhase >= warmUpSeconds) {
            sampleCount = 0;
            totalMeasuredFrames = 0;
            resetWindow();
            phase = Phase.MEASURING;
        }
    }

    private void measure(long renderNanos, long gpuWaitNanos) {
        totalMeasuredFrames++;
        windowFrames++;
        windowRenderNanos += renderNanos;
        windowGpuWaitNanos += gpuWaitNanos;
        windowSeconds += Gdx.graphics.getDeltaTime();
        if (windowSeconds >= sampleSeconds) {
            closeWindow();
        }
        if (sampleCount >= sampleMeansMs.length) {
            saveScreenshotAndReport();
        }
    }

    private void resetWindow() {
        windowFrames = 0;
        windowSeconds = 0f;
        windowRenderNanos = 0L;
        windowGpuWaitNanos = 0L;
    }

    private void closeWindow() {
        float renderMs = windowRenderNanos / (float) windowFrames / NANOS_PER_MILLISECOND;
        float gpuWaitMs = windowGpuWaitNanos / (float) windowFrames / NANOS_PER_MILLISECOND;
        sampleMeansMs[sampleCount] = renderMs;
        gpuWaitMeansMs[sampleCount] = gpuWaitMs;
        cpuMeansMs[sampleCount] = renderMs - gpuWaitMs;
        sampleCount++;
        resetWindow();
    }

    private void saveScreenshotAndReport() {
        String captureName = SCENE_ID + "-" + variants.get(variantIndex);
        screenshots.requestScreenshot(captureName + ".png");
        appendPerfLine(captureName);
        phase = Phase.SAVING;
    }

    private void openNextVariantOrFinish() {
        if (variantIndex + 1 < variants.size()) {
            startVariant(variantIndex + 1);
            return;
        }
        screenshots.getOutputDirectory().child(PERF_FILE).writeString(report.toString(), false);
        Gdx.app.log(PERF_TAG, "Capture written to " + screenshots.getOutputDirectory().path());
        phase = Phase.DONE;
        Gdx.app.exit();
    }

    private void appendPerfLine(String label) {
        PerfSummary summary = PerfSummary.of(sampleMeansMs);
        report.append(label)
                .append(": medianMs=").append(formatMilliseconds(summary.medianMs()))
                .append(" minMs=").append(formatMilliseconds(summary.minMs()))
                .append(" maxMs=").append(formatMilliseconds(summary.maxMs()))
                .append(" spreadPct=").append(summary.spreadPercent())
                .append(" cpuMs=")
                .append(formatMilliseconds(PerfSummary.of(cpuMeansMs).medianMs()))
                .append(" gpuWaitMs=")
                .append(formatMilliseconds(PerfSummary.of(gpuWaitMeansMs).medianMs()))
                .append(" samples=[");
        for (int i = 0; i < sampleMeansMs.length; i++) {
            if (i > 0) {
                report.append(',');
            }
            report.append(formatMilliseconds(sampleMeansMs[i]));
        }
        report.append("] frames=").append(totalMeasuredFrames)
                .append(" cells=").append(showcase.cellCount())
                .append('\n');
    }

    private static String formatMilliseconds(float milliseconds) {
        return String.format(Locale.ROOT, "%.2f", milliseconds);
    }
}
