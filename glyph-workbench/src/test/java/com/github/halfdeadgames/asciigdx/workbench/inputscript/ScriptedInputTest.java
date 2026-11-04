package com.github.halfdeadgames.asciigdx.workbench.inputscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import org.junit.jupiter.api.Test;

class ScriptedInputTest {

  private final ScriptedInputState state = new ScriptedInputState();
  private final ScriptedInput input = new ScriptedInput(null, state);

  @Test
  void queriesComeFromState() {
    state.pressKey(Input.Keys.A);
    state.pressButton(Input.Buttons.LEFT);
    state.moveMouse(7, 8);

    assertTrue(input.isKeyPressed(Input.Keys.A));
    assertTrue(input.isKeyJustPressed(Input.Keys.ANY_KEY));
    assertTrue(input.isButtonPressed(Input.Buttons.LEFT));
    assertTrue(input.isTouched());
    assertTrue(input.justTouched());
    assertEquals(7, input.getX());
    assertEquals(8, input.getY(0));
    assertEquals(7, input.getDeltaX());
    assertFalse(input.isTouched(1));
  }

  @Test
  void processorIsStoredLocally() {
    InputProcessor processor = new InputAdapter();
    assertNull(input.currentProcessor());

    input.setInputProcessor(processor);

    assertSame(processor, input.getInputProcessor());
    assertSame(processor, input.currentProcessor());
  }
}
