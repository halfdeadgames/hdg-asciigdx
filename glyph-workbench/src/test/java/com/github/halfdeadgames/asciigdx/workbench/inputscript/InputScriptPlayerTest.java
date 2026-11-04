package com.github.halfdeadgames.asciigdx.workbench.inputscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InputScriptPlayerTest {

  private static final float FRAME = 1f / 60f;

  private final ScriptedInputState state = new ScriptedInputState();
  private final List<String> events = new ArrayList<>();
  private final List<String> callbacks = new ArrayList<>();
  private InputProcessor processor = new RecordingProcessor(events);

  private InputScriptPlayer play(ScriptStep... steps) {
    ScriptListener listener =
        new ScriptListener() {
          @Override
          public void captureRequested(String name) {
            callbacks.add("capture " + name);
          }

          @Override
          public void quitRequested() {
            callbacks.add("quit");
          }
        };
    return new InputScriptPlayer(List.of(steps), state, listener, () -> processor);
  }

  private static void advance(InputScriptPlayer player, int frames) {
    for (int i = 0; i < frames; i++) player.advanceFrame(FRAME);
  }

  @Test
  void pressGoesDownThenUpOnNextFrame() {
    InputScriptPlayer player = play(new ScriptStep.Press(Input.Keys.ESCAPE));

    player.advanceFrame(FRAME);
    assertTrue(state.isKeyDown(Input.Keys.ESCAPE));
    assertFalse(player.isFinished());

    player.advanceFrame(FRAME);
    assertFalse(state.isKeyDown(Input.Keys.ESCAPE));
    assertEquals(
        List.of("keyDown " + Input.Keys.ESCAPE, "keyUp " + Input.Keys.ESCAPE), events);
    assertTrue(player.isFinished());
  }

  @Test
  void justPressedLastsExactlyOneFrame() {
    InputScriptPlayer player = play(new ScriptStep.Hold(Input.Keys.ESCAPE));

    player.advanceFrame(FRAME);
    assertTrue(state.isKeyJustPressed(Input.Keys.ESCAPE));
    player.advanceFrame(FRAME);
    assertFalse(state.isKeyJustPressed(Input.Keys.ESCAPE));
    assertTrue(state.isKeyDown(Input.Keys.ESCAPE));
  }

  @Test
  void consecutiveStepsTakeSeparateFramesWithReleaseBetween() {
    InputScriptPlayer player =
        play(new ScriptStep.Press(Input.Keys.ESCAPE), new ScriptStep.Press(Input.Keys.ENTER));

    player.advanceFrame(FRAME);
    assertTrue(state.isKeyJustPressed(Input.Keys.ESCAPE));
    assertFalse(state.isKeyDown(Input.Keys.ENTER));

    player.advanceFrame(FRAME);
    assertFalse(state.isKeyDown(Input.Keys.ESCAPE));
    assertFalse(state.isKeyDown(Input.Keys.ENTER));

    player.advanceFrame(FRAME);
    assertTrue(state.isKeyJustPressed(Input.Keys.ENTER));
  }

  @Test
  void holdStaysDownUntilReleaseStep() {
    InputScriptPlayer player =
        play(new ScriptStep.Hold(Input.Keys.SHIFT_LEFT), new ScriptStep.Release(Input.Keys.SHIFT_LEFT));

    advance(player, 1);
    assertTrue(state.isKeyDown(Input.Keys.SHIFT_LEFT));
    assertFalse(player.isFinished());
    advance(player, 1);
    assertFalse(state.isKeyDown(Input.Keys.SHIFT_LEFT));
    assertTrue(player.isFinished());
  }

  @Test
  void anyKeyMatchesAnyDownKey() {
    InputScriptPlayer player = play(new ScriptStep.Hold(Input.Keys.A));

    assertFalse(state.isKeyDown(Input.Keys.ANY_KEY));
    player.advanceFrame(FRAME);
    assertTrue(state.isKeyDown(Input.Keys.ANY_KEY));
    assertTrue(state.isKeyJustPressed(Input.Keys.ANY_KEY));
    player.advanceFrame(FRAME);
    assertFalse(state.isKeyJustPressed(Input.Keys.ANY_KEY));
    assertTrue(state.isKeyDown(Input.Keys.ANY_KEY));
  }

  @Test
  void pressingPrintableKeyTypesLowercaseLetter() {
    play(new ScriptStep.Press(Input.Keys.A)).advanceFrame(FRAME);

    assertEquals(List.of("keyDown 29", "keyTyped a"), events);
  }

  @Test
  void shiftMakesLettersUppercase() {
    InputScriptPlayer player = play(new ScriptStep.Hold(Input.Keys.SHIFT_RIGHT), new ScriptStep.Hold(Input.Keys.A));

    advance(player, 2);

    assertTrue(events.contains("keyTyped A"));
  }

  @Test
  void digitsSpaceAndEditingKeysType() {
    InputScriptPlayer player =
        play(
            new ScriptStep.Hold(Input.Keys.NUM_7),
            new ScriptStep.Hold(Input.Keys.SPACE),
            new ScriptStep.Hold(Input.Keys.ENTER),
            new ScriptStep.Hold(Input.Keys.BACKSPACE),
            new ScriptStep.Hold(Input.Keys.TAB),
            new ScriptStep.Hold(Input.Keys.FORWARD_DEL),
            new ScriptStep.Hold(Input.Keys.ESCAPE));

    advance(player, 7);

    List<String> typed = events.stream().filter(e -> e.startsWith("keyTyped")).toList();
    assertEquals(
        List.of("keyTyped 7", "keyTyped  ", "keyTyped \n", "keyTyped \b", "keyTyped \t", "keyTyped " + (char) 127),
        typed);
  }

  @Test
  void typeTextEmitsOneCharacterPerFrameAndOnlyKeyTyped() {
    InputScriptPlayer player = play(new ScriptStep.TypeText("Hi!"));

    player.advanceFrame(FRAME);
    assertEquals(List.of("keyTyped H"), events);
    player.advanceFrame(FRAME);
    assertEquals(List.of("keyTyped H", "keyTyped i"), events);
    assertFalse(player.isFinished());
    player.advanceFrame(FRAME);
    assertEquals(List.of("keyTyped H", "keyTyped i", "keyTyped !"), events);
    assertTrue(player.isFinished());
  }

  @Test
  void waitFramesDelaysFollowingStep() {
    InputScriptPlayer player = play(new ScriptStep.WaitFrames(2), new ScriptStep.Hold(Input.Keys.ESCAPE));

    advance(player, 2);
    assertFalse(state.isKeyDown(Input.Keys.ESCAPE));
    advance(player, 1);
    assertTrue(state.isKeyDown(Input.Keys.ESCAPE));
  }

  @Test
  void waitSecondsAccumulatesDelta() {
    InputScriptPlayer player = play(new ScriptStep.WaitSeconds(1f), new ScriptStep.Hold(Input.Keys.ESCAPE));

    player.advanceFrame(0f);
    player.advanceFrame(0.5f);
    assertFalse(state.isKeyDown(Input.Keys.ESCAPE));
    player.advanceFrame(0.25f);
    assertFalse(state.isKeyDown(Input.Keys.ESCAPE));
    player.advanceFrame(0.25f);
    assertTrue(state.isKeyDown(Input.Keys.ESCAPE));
  }

  @Test
  void clickMovesThenPressesThenReleasesOnNextFrame() {
    InputScriptPlayer player = play(new ScriptStep.Click(10, 20, Input.Buttons.LEFT));

    player.advanceFrame(FRAME);
    assertEquals(List.of("mouseMoved 10 20", "touchDown 10 20 0 0"), events);
    assertTrue(state.isButtonJustPressed(Input.Buttons.LEFT));
    assertFalse(player.isFinished());

    player.advanceFrame(FRAME);
    assertEquals("touchUp 10 20 0 0", events.get(2));
    assertFalse(state.isButtonDown(Input.Buttons.LEFT));
    assertTrue(player.isFinished());
  }

  @Test
  void moveWhileButtonDownIsDragAndOtherwiseMouseMoved() {
    InputScriptPlayer player =
        play(
            new ScriptStep.MoveMouse(1, 2),
            new ScriptStep.MouseDown(Input.Buttons.LEFT),
            new ScriptStep.MoveMouse(5, 6),
            new ScriptStep.MouseUp(Input.Buttons.LEFT));

    advance(player, 4);

    assertEquals(
        List.of("mouseMoved 1 2", "touchDown 1 2 0 0", "touchDragged 5 6 0", "touchUp 5 6 0 0"), events);
  }

  @Test
  void mouseDeltaIsClearedEachFrame() {
    InputScriptPlayer player = play(new ScriptStep.MoveMouse(4, 9));

    player.advanceFrame(FRAME);
    assertEquals(4, state.deltaX());
    assertEquals(9, state.deltaY());
    player.advanceFrame(FRAME);
    assertEquals(0, state.deltaX());
    assertEquals(4, state.mouseX());
  }

  @Test
  void wheelScrollsVertically() {
    play(new ScriptStep.Wheel(-3)).advanceFrame(FRAME);

    assertEquals(List.of("scrolled 0.0 -3.0"), events);
  }

  @Test
  void captureAfterInputFiresOnFollowingFrame() {
    InputScriptPlayer player = play(new ScriptStep.Hold(Input.Keys.A), new ScriptStep.Capture("shot"));

    player.advanceFrame(FRAME);
    assertTrue(callbacks.isEmpty());
    player.advanceFrame(FRAME);
    assertEquals(List.of("capture shot"), callbacks);
    assertTrue(player.isFinished());
  }

  @Test
  void atMostOneCapturePerFrame() {
    InputScriptPlayer player = play(new ScriptStep.Capture("a"), new ScriptStep.Capture("b"));

    player.advanceFrame(FRAME);
    assertEquals(List.of("capture a"), callbacks);
    player.advanceFrame(FRAME);
    assertEquals(List.of("capture a", "capture b"), callbacks);
  }

  @Test
  void quitAfterInputWaitsForFollowingFrameAndFinishes() {
    InputScriptPlayer player = play(new ScriptStep.Hold(Input.Keys.A), new ScriptStep.Quit());

    player.advanceFrame(FRAME);
    assertTrue(callbacks.isEmpty());
    assertFalse(player.isFinished());
    player.advanceFrame(FRAME);
    assertEquals(List.of("quit"), callbacks);
    assertTrue(player.isFinished());
  }

  @Test
  void emptyScriptIsFinishedImmediately() {
    assertTrue(play().isFinished());
  }

  @Test
  void nullProcessorStillUpdatesState() {
    processor = null;
    InputScriptPlayer player = play(new ScriptStep.Hold(Input.Keys.A), new ScriptStep.Wheel(1));

    advance(player, 2);

    assertTrue(state.isKeyDown(Input.Keys.A));
    assertTrue(events.isEmpty());
  }

  private record RecordingProcessor(List<String> events) implements InputProcessor {

    @Override
    public boolean keyDown(int keycode) {
      events.add("keyDown " + keycode);
      return false;
    }

    @Override
    public boolean keyUp(int keycode) {
      events.add("keyUp " + keycode);
      return false;
    }

    @Override
    public boolean keyTyped(char character) {
      events.add("keyTyped " + character);
      return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
      events.add("touchDown " + screenX + " " + screenY + " " + pointer + " " + button);
      return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
      events.add("touchUp " + screenX + " " + screenY + " " + pointer + " " + button);
      return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
      events.add("touchCancelled");
      return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
      events.add("touchDragged " + screenX + " " + screenY + " " + pointer);
      return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
      events.add("mouseMoved " + screenX + " " + screenY);
      return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
      events.add("scrolled " + amountX + " " + amountY);
      return false;
    }
  }
}
