package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;

/** Unattended run: menu screenshot, then per scene a screenshot, perf line and optional GIF, then exit. Driven by {@link #onFrameRendered()}. */
class CaptureScript {
    private static final int MEASURED_FRAMES = 600;
    private static final int GIF_FRAMES = 30;
    /** Frames skipped after opening a scene, so setup and the previous scene's file writes stay out of the timing. */
    private static final int WARM_UP_FRAMES = 30;
    private static final String PERF_TAG = "CaptureScript";

    private enum Phase { MENU_REQUEST, MENU_SAVING, WARMING_UP, MEASURING, SAVING, RECORDING, DONE }

    private final SandboxApp app;
    private final ScreenCapture capture;
    private Phase phase = Phase.MENU_REQUEST;
    private int sceneIndex;
    private int framesInPhase;
    private float secondsInPhase;

    private final StringBuilder report = new StringBuilder();

    CaptureScript(SandboxApp app, ScreenCapture capture) {
        this.app = app;
        this.capture = capture;
    }

    /** Call at the end of render, after {@link ScreenCapture#onFrameRendered()}. */
    void onFrameRendered() {
        switch (phase) {
            case MENU_REQUEST -> {
                capture.requestScreenshot("menu.png");
                phase = Phase.MENU_SAVING;
            }
            case MENU_SAVING -> openScene(0);
            case WARMING_UP -> {
                framesInPhase++;
                if (framesInPhase >= WARM_UP_FRAMES) {
                    framesInPhase = 0;
                    secondsInPhase = 0f;
                    phase = Phase.MEASURING;
                }
            }
            case MEASURING -> {
                framesInPhase++;
                secondsInPhase += Gdx.graphics.getDeltaTime();
                if (framesInPhase >= MEASURED_FRAMES) {
                    finishMeasuring();
                }
            }
            case SAVING -> openNextScene();
            case RECORDING -> {
                if (capture.getRecordedFrameCount() >= GIF_FRAMES) {
                    capture.stopRecording(app.getScene(sceneIndex).id() + ".gif");
                    openNextScene();
                }
            }
            case DONE -> { }
        }
    }

    private void finishMeasuring() {
        SandboxScene scene = app.getScene(sceneIndex);
        capture.requestScreenshot(scene.id() + ".png");
        appendPerf(scene);
        if (scene.recordsAnimation()) {
            capture.startRecording();
            phase = Phase.RECORDING;
        } else {
            phase = Phase.SAVING;
        }
    }

    private void openNextScene() {
        openScene(sceneIndex + 1);
    }

    private void openScene(int index) {
        if (index >= app.getSceneCount()) {
            capture.getOutputDirectory().child("perf.txt").writeString(report.toString(), false);
            Gdx.app.log(PERF_TAG, "Capture written to " + capture.getOutputDirectory().path());
            phase = Phase.DONE;
            Gdx.app.exit();
            return;
        }
        sceneIndex = index;
        app.openScene(index);
        framesInPhase = 0;
        secondsInPhase = 0f;
        phase = Phase.WARMING_UP;
    }

    private void appendPerf(SandboxScene scene) {
        float averageMilliseconds = secondsInPhase / framesInPhase * 1000f;
        report.append(scene.id())
            .append(": fps=").append(String.format("%.1f", framesInPhase / secondsInPhase))
            .append(" avgFrameMs=").append(String.format("%.2f", averageMilliseconds))
            .append(" cells=").append(scene.cellCount())
            .append(" frames=").append(framesInPhase)
            .append('\n');
    }
}
