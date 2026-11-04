package com.github.halfdeadgames.asciigdx.workbench.inputscript;

import com.badlogic.gdx.Gdx;
import com.github.halfdeadgames.asciigdx.workbench.ScreenshotFile;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Plays an input script into the running app and saves the captures it asks for. */
public final class ScriptSession implements ScriptListener {

  public static final String SCRIPT_PROPERTY = "workbench.script";
  public static final String REPO_ROOT_PROPERTY = "workbench.repoRoot";

  private static final String TAG = "Workbench";
  private static final String CAPTURE_DIRECTORY = "build/captures/script";

  private final Input realInput;
  private final ScriptedInput scriptedInput;
  private final InputScriptPlayer player;
  private final Path captureDirectory;

  private String pendingCaptureName;
  private boolean quitPending;
  private boolean realInputRestored;

  public ScriptSession(String scriptPath) {
    String repoRoot = System.getProperty(REPO_ROOT_PROPERTY);
    if (repoRoot == null) {
      throw new GdxRuntimeException(REPO_ROOT_PROPERTY + " is not set");
    }
    captureDirectory = Path.of(repoRoot, CAPTURE_DIRECTORY);
    realInput = Gdx.input;
    ScriptedInputState state = new ScriptedInputState();
    scriptedInput = new ScriptedInput(realInput, state);
    player =
        new InputScriptPlayer(
            InputScriptParser.parse(readScript(scriptPath)),
            state,
            this,
            scriptedInput::currentProcessor);
    Gdx.input = scriptedInput;
  }

  public void beforeFrame(float delta) {
    if (realInputRestored) return;
    player.advanceFrame(delta);
  }

  public void afterFrame() {
    savePendingCapture();
    if (quitPending) {
      Gdx.app.exit();
      return;
    }
    if (!realInputRestored && player.isFinished()) restoreRealInput();
  }

  @Override
  public void captureRequested(String name) {
    pendingCaptureName = name;
  }

  @Override
  public void quitRequested() {
    quitPending = true;
  }

  private static String readScript(String scriptPath) {
    try {
      return Files.readString(Path.of(scriptPath), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new GdxRuntimeException("Cannot read script " + scriptPath + ": " + e.getMessage(), e);
    }
  }

  private void restoreRealInput() {
    realInputRestored = true;
    Gdx.input = realInput;
    realInput.setInputProcessor(scriptedInput.currentProcessor());
  }

  private void savePendingCapture() {
    if (pendingCaptureName == null) return;
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
