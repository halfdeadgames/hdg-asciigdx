package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import java.util.List;
import java.util.Locale;

/**
 * Unattended run: menu screenshot, then per scene a screenshot, perf line and optional GIF, then
 * exit. Driven by {@link #onFrameRendered(long, long, long)}.
 */
class CaptureScript {
    private static final int GIF_FRAMES = 30;
    /**
     * Frames skipped after opening a scene, so setup and the previous scene's file writes stay out
     * of the timing. Warm-up also lasts at least the configured warm-up seconds.
     */
    private static final int WARM_UP_FRAMES = 30;
    /** Longest wait for a scene's requested window size before warm-up starts anyway. */
    private static final float WINDOW_SETTLE_TIMEOUT_SECONDS = 2f;
    private static final float NANOS_PER_MILLISECOND = 1_000_000f;
    private static final int NOT_SWEEPING = -1;
    private static final String PERF_TAG = "CaptureScript";
    /** Passed as GPU draw time when the platform has no GPU timer. */
    static final long GPU_DRAW_UNMEASURED = -1L;

    private enum Phase {
        MENU_REQUEST, MENU_SAVING, SETTLING_WINDOW, WARMING_UP, MEASURING, SAVING, RECORDING, DONE
    }

    private final SandboxApp app;
    private final ScreenCapture capture;
    private Phase phase = Phase.MENU_REQUEST;
    private int sceneIndex;
    private int framesInPhase;
    private float secondsInPhase;

    private final StringBuilder report = new StringBuilder();
    private final float sampleSeconds;
    private final float warmUpSeconds;
    private final float[] sampleMeansMs;
    private final float[] gpuWaitMeansMs;
    private final float[] cpuMeansMs;
    private final float[] gpuDrawMeansMs;
    private boolean gpuDrawMeasured;
    private int sampleCount;
    private int windowFrames;
    private float windowSeconds;
    private long windowRenderNanos;
    private long windowGpuWaitNanos;
    private long windowGpuDrawNanos;
    private int totalMeasuredFrames;
    private final int[] sceneOrder;
    private int visitPosition;
    private final List<Integer> dirtyPercents;
    private final List<Integer> layerCounts;
    private int sweepIndex = NOT_SWEEPING;
    private List<Integer> sceneLayerCounts;
    private List<Integer> sceneDirtyPercents;

    /** @throws IllegalArgumentException when the config names a scene id that is not registered */
    CaptureScript(SandboxApp app, ScreenCapture capture, CaptureConfig config) {
        this.app = app;
        this.capture = capture;
        this.sampleSeconds = config.getSampleSeconds();
        this.warmUpSeconds = config.getWarmUpSeconds();
        this.sampleMeansMs = new float[config.getSamples()];
        this.gpuWaitMeansMs = new float[config.getSamples()];
        this.cpuMeansMs = new float[config.getSamples()];
        this.gpuDrawMeansMs = new float[config.getSamples()];
        this.dirtyPercents = config.getDirtyPercents();
        this.layerCounts = config.getLayerCounts();
        this.sceneOrder = resolveSceneOrder(app, config);
    }

    private static int[] resolveSceneOrder(SandboxApp app, CaptureConfig config) {
        List<String> requestedIds = config.getSceneIds();
        if (requestedIds.isEmpty()) {
            int[] all = new int[app.getSceneCount()];
            for (int i = 0; i < all.length; i++) {
                all[i] = i;
            }
            return all;
        }
        int[] order = new int[requestedIds.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = indexOfScene(app, requestedIds.get(i));
        }
        return order;
    }

    private static int indexOfScene(SandboxApp app, String id) {
        StringBuilder validIds = new StringBuilder();
        for (int i = 0; i < app.getSceneCount(); i++) {
            String validId = app.getScene(i).id();
            if (validId.equals(id)) {
                return i;
            }
            if (i > 0) {
                validIds.append(", ");
            }
            validIds.append(validId);
        }
        throw new IllegalArgumentException("Unknown scene id '" + id + "'; valid ids: " + validIds);
    }

    /**
     * Call at the end of render, after {@link ScreenCapture#onFrameRendered()}.
     *
     * @param renderNanos nanoseconds the scene's render work took this frame, GPU work included; 0
     *        when no scene was drawn
     * @param gpuWaitNanos the part of renderNanos spent waiting for the GPU to finish after all
     *        draw calls were submitted; the rest is CPU time
     * @param gpuDrawNanos nanoseconds the GPU itself spent on the scene's commands, from a GPU
     *        timer query; {@link #GPU_DRAW_UNMEASURED} when the platform has no GPU timer
     */
    void onFrameRendered(long renderNanos, long gpuWaitNanos, long gpuDrawNanos) {
        switch (phase) {
            case MENU_REQUEST -> {
                capture.requestScreenshot("menu.png");
                phase = Phase.MENU_SAVING;
            }
            case MENU_SAVING -> openScene(0);
            case SETTLING_WINDOW -> {
                // The first frame's delta still holds the previous scene's file writes.
                if (framesInPhase > 0) {
                    secondsInPhase += Gdx.graphics.getDeltaTime();
                }
                framesInPhase++;
                if (app.isWindowSettled()) {
                    beginWarmUp();
                } else if (secondsInPhase >= WINDOW_SETTLE_TIMEOUT_SECONDS) {
                    Gdx.app.log(PERF_TAG, "Window size not settled after "
                            + WINDOW_SETTLE_TIMEOUT_SECONDS + " s in "
                            + app.getScene(sceneIndex).id() + "; measuring anyway");
                    beginWarmUp();
                }
            }
            case WARMING_UP -> {
                framesInPhase++;
                secondsInPhase += Gdx.graphics.getDeltaTime();
                if (framesInPhase >= WARM_UP_FRAMES && secondsInPhase >= warmUpSeconds) {
                    beginMeasuring();
                }
            }
            case MEASURING -> {
                totalMeasuredFrames++;
                windowFrames++;
                windowRenderNanos += renderNanos;
                windowGpuWaitNanos += gpuWaitNanos;
                gpuDrawMeasured = gpuDrawNanos != GPU_DRAW_UNMEASURED;
                windowGpuDrawNanos += Math.max(gpuDrawNanos, 0L);
                windowSeconds += Gdx.graphics.getDeltaTime();
                if (windowSeconds >= sampleSeconds) {
                    closeWindow();
                }
                if (sampleCount >= sampleMeansMs.length) {
                    finishMeasuring();
                }
            }
            case SAVING -> {
                if (isSweeping() && sweepIndex + 1 < sweepStepCount()) {
                    startSweepStep(sweepIndex + 1);
                } else {
                    openNextScene();
                }
            }
            case RECORDING -> {
                if (capture.getRecordedFrameCount() >= GIF_FRAMES) {
                    capture.stopRecording(app.getScene(sceneIndex).id() + ".gif");
                    openNextScene();
                }
            }
            case DONE -> {
            }
        }
    }

    private void beginMeasuring() {
        sampleCount = 0;
        totalMeasuredFrames = 0;
        resetWindow();
        phase = Phase.MEASURING;
    }

    private void resetWindow() {
        windowFrames = 0;
        windowSeconds = 0f;
        windowRenderNanos = 0L;
        windowGpuWaitNanos = 0L;
        windowGpuDrawNanos = 0L;
    }

    private void closeWindow() {
        float renderMs = windowRenderNanos / (float) windowFrames / NANOS_PER_MILLISECOND;
        float gpuWaitMs = windowGpuWaitNanos / (float) windowFrames / NANOS_PER_MILLISECOND;
        sampleMeansMs[sampleCount] = renderMs;
        gpuWaitMeansMs[sampleCount] = gpuWaitMs;
        cpuMeansMs[sampleCount] = renderMs - gpuWaitMs;
        gpuDrawMeansMs[sampleCount] =
                windowGpuDrawNanos / (float) windowFrames / NANOS_PER_MILLISECOND;
        sampleCount++;
        resetWindow();
    }

    private void finishMeasuring() {
        SandboxScene scene = app.getScene(sceneIndex);
        if (isSweeping()) {
            capture.requestScreenshot(scene.id() + sweepSuffix("") + ".png");
            appendPerf(scene, scene.id() + sweepSuffix("%"));
            phase = Phase.SAVING;
            return;
        }
        capture.requestScreenshot(scene.id() + ".png");
        appendPerf(scene, scene.id());
        if (scene.recordsAnimation()) {
            capture.startRecording();
            phase = Phase.RECORDING;
        } else {
            phase = Phase.SAVING;
        }
    }

    private void openNextScene() {
        openScene(visitPosition + 1);
    }

    private void openScene(int position) {
        if (position >= sceneOrder.length) {
            capture.getOutputDirectory().child("perf.txt").writeString(report.toString(), false);
            Gdx.app.log(PERF_TAG, "Capture written to " + capture.getOutputDirectory().path());
            phase = Phase.DONE;
            Gdx.app.exit();
            return;
        }
        visitPosition = position;
        sceneIndex = sceneOrder[position];
        waitForWindowThenWarmUp();
        app.openScene(sceneIndex);
        sweepIndex = NOT_SWEEPING;
        SandboxScene scene = app.getScene(sceneIndex);
        sceneLayerCounts =
                !layerCounts.isEmpty() && scene.supportsLayerCount() ? layerCounts : null;
        sceneDirtyPercents =
                !dirtyPercents.isEmpty() && scene.supportsDirtyPercent() ? dirtyPercents : null;
        if (sceneLayerCounts != null || sceneDirtyPercents != null) {
            startSweepStep(0);
            return;
        }
        waitForWindowThenWarmUp();
    }

    private int sweepStepCount() {
        return (sceneLayerCounts == null ? 1 : sceneLayerCounts.size())
                * (sceneDirtyPercents == null ? 1 : sceneDirtyPercents.size());
    }

    /**
     * Label tail such as "@L2", "@10" or "@L2@10"; dirtyUnit is appended after the dirty number.
     */
    private String sweepSuffix(String dirtyUnit) {
        StringBuilder suffix = new StringBuilder();
        if (sceneLayerCounts != null) {
            suffix.append("@L").append(sceneLayerCounts.get(sweepIndex / dirtyStepCount()));
        }
        if (sceneDirtyPercents != null) {
            suffix.append('@').append(sceneDirtyPercents.get(sweepIndex % dirtyStepCount()))
                    .append(dirtyUnit);
        }
        return suffix.toString();
    }

    private int dirtyStepCount() {
        return sceneDirtyPercents == null ? 1 : sceneDirtyPercents.size();
    }

    private boolean isSweeping() {
        return sweepIndex != NOT_SWEEPING;
    }

    private void startSweepStep(int index) {
        sweepIndex = index;
        SandboxScene scene = app.getScene(sceneIndex);
        if (sceneLayerCounts != null) {
            scene.setLayerCount(sceneLayerCounts.get(index / dirtyStepCount()));
        }
        if (sceneDirtyPercents != null) {
            scene.setDirtyPercent(sceneDirtyPercents.get(index % dirtyStepCount()));
        }
        app.refreshStatus();
        waitForWindowThenWarmUp();
    }

    private void waitForWindowThenWarmUp() {
        framesInPhase = 0;
        secondsInPhase = 0f;
        phase = Phase.SETTLING_WINDOW;
    }

    private void beginWarmUp() {
        framesInPhase = 0;
        secondsInPhase = 0f;
        phase = Phase.WARMING_UP;
    }

    private void appendPerf(SandboxScene scene, String label) {
        PerfSummary summary = PerfSummary.of(sampleMeansMs);
        report.append(label)
                .append(": medianMs=").append(formatMilliseconds(summary.medianMs()))
                .append(" minMs=").append(formatMilliseconds(summary.minMs()))
                .append(" maxMs=").append(formatMilliseconds(summary.maxMs()))
                .append(" spreadPct=").append(summary.spreadPercent())
                .append(" cpuMs=")
                .append(formatMilliseconds(PerfSummary.of(cpuMeansMs).medianMs()))
                .append(" gpuWaitMs=")
                .append(formatMilliseconds(PerfSummary.of(gpuWaitMeansMs).medianMs()));
        if (gpuDrawMeasured) {
            report.append(" gpuDrawMs=")
                    .append(formatMilliseconds(PerfSummary.of(gpuDrawMeansMs).medianMs()));
        }
        report.append(" samples=[");
        for (int i = 0; i < sampleMeansMs.length; i++) {
            if (i > 0) {
                report.append(',');
            }
            report.append(formatMilliseconds(sampleMeansMs[i]));
        }
        report.append("] frames=").append(totalMeasuredFrames)
                .append(" cells=").append(scene.cellCount())
                .append('\n');
    }

    private static String formatMilliseconds(float milliseconds) {
        return String.format(Locale.ROOT, "%.2f", milliseconds);
    }
}
