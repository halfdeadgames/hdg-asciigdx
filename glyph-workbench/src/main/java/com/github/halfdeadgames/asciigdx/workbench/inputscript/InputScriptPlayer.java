package com.github.halfdeadgames.asciigdx.workbench.inputscript;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class InputScriptPlayer {

  private static final int NOTHING_PENDING = Integer.MIN_VALUE;
  private static final int POINTER = 0;

  private final List<ScriptStep> steps;
  private final ScriptedInputState state;
  private final ScriptListener listener;
  private final Supplier<InputProcessor> processorSupplier;

  private int stepIndex;
  private int keyToRelease = NOTHING_PENDING;
  private int buttonToRelease = NOTHING_PENDING;
  private boolean waitStarted;
  private float waitRemaining;
  private int typedPosition;

  public InputScriptPlayer(
      List<ScriptStep> steps,
      ScriptedInputState state,
      ScriptListener listener,
      Supplier<InputProcessor> processorSupplier) {
    this.steps = List.copyOf(steps);
    this.state = state;
    this.listener = listener;
    this.processorSupplier = processorSupplier;
  }

  public boolean isFinished() {
    return stepIndex >= steps.size()
        && keyToRelease == NOTHING_PENDING
        && buttonToRelease == NOTHING_PENDING;
  }

  public void advanceFrame(float deltaSeconds) {
    state.beginFrame();
    if (releasePending()) return;
    runStepsUntilFrameEnds(deltaSeconds);
  }

  private void runStepsUntilFrameEnds(float deltaSeconds) {
    while (stepIndex < steps.size()) {
      if (runStep(steps.get(stepIndex), deltaSeconds)) return;
    }
  }

  private boolean runStep(ScriptStep step, float deltaSeconds) {
    if (step instanceof ScriptStep.WaitSeconds wait) return runWait(wait.seconds(), deltaSeconds);
    if (step instanceof ScriptStep.WaitFrames wait) return runWait(wait.frames(), 1f);
    if (step instanceof ScriptStep.TypeText typeText) return runTypeText(typeText.text());
    if (step instanceof ScriptStep.Capture capture) return runCapture(capture.name());
    if (step instanceof ScriptStep.Quit) return runQuit();
    runInputStep(step);
    return true;
  }

  private void runInputStep(ScriptStep step) {
    stepIndex++;
    if (step instanceof ScriptStep.Press press) {
      keyDown(press.keyCode());
      keyToRelease = press.keyCode();
    } else if (step instanceof ScriptStep.Hold hold) {
      keyDown(hold.keyCode());
    } else if (step instanceof ScriptStep.Release release) {
      keyUp(release.keyCode());
    } else if (step instanceof ScriptStep.MoveMouse move) {
      moveMouse(move.x(), move.y());
    } else if (step instanceof ScriptStep.Click click) {
      moveMouse(click.x(), click.y());
      buttonDown(click.button());
      buttonToRelease = click.button();
    } else if (step instanceof ScriptStep.MouseDown down) {
      buttonDown(down.button());
    } else if (step instanceof ScriptStep.MouseUp up) {
      buttonUp(up.button());
    } else if (step instanceof ScriptStep.Wheel wheel) {
      send(processor -> processor.scrolled(0f, wheel.ticks()));
    }
  }

  private boolean runWait(float total, float elapsedThisFrame) {
    if (!waitStarted) {
      waitStarted = true;
      waitRemaining = total;
    } else {
      waitRemaining -= elapsedThisFrame;
    }
    if (waitRemaining > 0) return true;
    waitStarted = false;
    stepIndex++;
    return false;
  }

  private boolean runTypeText(String text) {
    if (text.isEmpty()) {
      stepIndex++;
      return false;
    }
    char character = text.charAt(typedPosition++);
    send(processor -> processor.keyTyped(character));
    if (typedPosition >= text.length()) {
      typedPosition = 0;
      stepIndex++;
    }
    return true;
  }

  private boolean runCapture(String name) {
    stepIndex++;
    listener.captureRequested(name);
    return true;
  }

  private boolean runQuit() {
    stepIndex = steps.size();
    listener.quitRequested();
    return true;
  }

  private boolean releasePending() {
    boolean released = false;
    if (keyToRelease != NOTHING_PENDING) {
      keyUp(keyToRelease);
      keyToRelease = NOTHING_PENDING;
      released = true;
    }
    if (buttonToRelease != NOTHING_PENDING) {
      buttonUp(buttonToRelease);
      buttonToRelease = NOTHING_PENDING;
      released = true;
    }
    return released;
  }

  private void keyDown(int keyCode) {
    state.pressKey(keyCode);
    send(processor -> processor.keyDown(keyCode));
    char typed = TypedCharacters.forKey(keyCode, isShiftDown());
    if (typed != TypedCharacters.NONE) send(processor -> processor.keyTyped(typed));
  }

  private void keyUp(int keyCode) {
    state.releaseKey(keyCode);
    send(processor -> processor.keyUp(keyCode));
  }

  private void moveMouse(int x, int y) {
    state.moveMouse(x, y);
    if (state.isTouched()) {
      send(processor -> processor.touchDragged(x, y, POINTER));
    } else {
      send(processor -> processor.mouseMoved(x, y));
    }
  }

  private void buttonDown(int button) {
    state.pressButton(button);
    send(processor -> processor.touchDown(state.mouseX(), state.mouseY(), POINTER, button));
  }

  private void buttonUp(int button) {
    state.releaseButton(button);
    send(processor -> processor.touchUp(state.mouseX(), state.mouseY(), POINTER, button));
  }

  private boolean isShiftDown() {
    return state.isKeyDown(Input.Keys.SHIFT_LEFT) || state.isKeyDown(Input.Keys.SHIFT_RIGHT);
  }

  private void send(Consumer<InputProcessor> event) {
    InputProcessor processor = processorSupplier.get();
    if (processor != null) event.accept(processor);
  }
}
