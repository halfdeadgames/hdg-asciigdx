package com.github.halfdeadgames.asciigdx.inputscript;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Plays an input script into the running app and saves the captures it asks for. */
public final class ScriptSession implements ScriptListener {

    private static final String TAG = "InputScript";

    private final Input realInput;
    private final ScriptedInput scriptedInput;
    private final Graphics realGraphics;
    private final FixedStepGraphics fixedStepGraphics;
    private final InputScriptPlayer player;
    private final Path captureDirectory;

    private String pendingCaptureName;
    private boolean quitPending;
    private boolean realInputRestored;

    public ScriptSession(Path scriptFile, Path captureDirectory) {
        this.captureDirectory = captureDirectory;
        realInput = Gdx.input;
        ScriptedInputState state = new ScriptedInputState();
        scriptedInput = new ScriptedInput(realInput, state);
        player =
                new InputScriptPlayer(
                        InputScriptParser.parse(readScript(scriptFile)),
                        state,
                        this,
                        scriptedInput::getInputProcessor);
        realGraphics = Gdx.graphics;
        fixedStepGraphics = new FixedStepGraphics(realGraphics);
        installScriptedDevices();
    }

    public void beforeFrame(float delta) {
        if (realInputRestored) {
            return;
        }
        // The backend may reassign the statics when it switches windows.
        installScriptedDevices();
        player.advanceFrame(delta);
    }

    public void afterFrame() {
        savePendingCapture();
        if (quitPending) {
            Gdx.app.exit();
            return;
        }
        if (!realInputRestored && player.isFinished()) {
            restoreRealInput();
        }
    }

    @Override
    public void captureRequested(String name) {
        pendingCaptureName = name;
    }

    @Override
    public void quitRequested() {
        quitPending = true;
    }

    private static String readScript(Path scriptFile) {
        try {
            return Files.readString(scriptFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GdxRuntimeException(
                    "Cannot read script " + scriptFile + ": " + e.getMessage(), e);
        }
    }

    private void installScriptedDevices() {
        Gdx.input = scriptedInput;
        Gdx.graphics = fixedStepGraphics;
    }

    private void restoreRealInput() {
        realInputRestored = true;
        Gdx.input = realInput;
        Gdx.graphics = realGraphics;
        realInput.setInputProcessor(scriptedInput.getInputProcessor());
    }

    private void savePendingCapture() {
        if (pendingCaptureName == null) {
            return;
        }
        String file = captureDirectory.resolve(pendingCaptureName + ".png").toString();
        pendingCaptureName = null;
        try {
            ScreenshotFile.writeBackBuffer(file);
            Gdx.app.log(TAG, "Captured " + file);
        } catch (RuntimeException e) {
            Gdx.app.error(TAG, "Capture failed: " + e.getMessage());
        }
    }
}
