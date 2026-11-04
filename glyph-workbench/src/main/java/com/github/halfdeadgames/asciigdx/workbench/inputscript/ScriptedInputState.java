package com.github.halfdeadgames.asciigdx.workbench.inputscript;

import com.badlogic.gdx.Input;
import java.util.HashSet;
import java.util.Set;

public final class ScriptedInputState {

  private final Set<Integer> keysDown = new HashSet<>();
  private final Set<Integer> keysJustPressed = new HashSet<>();
  private final Set<Integer> buttonsDown = new HashSet<>();
  private final Set<Integer> buttonsJustPressed = new HashSet<>();
  private int mouseX;
  private int mouseY;
  private int deltaX;
  private int deltaY;

  public void beginFrame() {
    keysJustPressed.clear();
    buttonsJustPressed.clear();
    deltaX = 0;
    deltaY = 0;
  }

  public void pressKey(int keyCode) {
    if (keysDown.add(keyCode)) keysJustPressed.add(keyCode);
  }

  public void releaseKey(int keyCode) {
    keysDown.remove(keyCode);
  }

  public void pressButton(int button) {
    if (buttonsDown.add(button)) buttonsJustPressed.add(button);
  }

  public void releaseButton(int button) {
    buttonsDown.remove(button);
  }

  public void moveMouse(int x, int y) {
    deltaX += x - mouseX;
    deltaY += y - mouseY;
    mouseX = x;
    mouseY = y;
  }

  public boolean isKeyDown(int keyCode) {
    if (keyCode == Input.Keys.ANY_KEY) return !keysDown.isEmpty();
    return keysDown.contains(keyCode);
  }

  public boolean isKeyJustPressed(int keyCode) {
    if (keyCode == Input.Keys.ANY_KEY) return !keysJustPressed.isEmpty();
    return keysJustPressed.contains(keyCode);
  }

  public boolean isButtonDown(int button) {
    return buttonsDown.contains(button);
  }

  public boolean isButtonJustPressed(int button) {
    return buttonsJustPressed.contains(button);
  }

  public boolean isTouched() {
    return !buttonsDown.isEmpty();
  }

  public boolean justTouched() {
    return !buttonsJustPressed.isEmpty();
  }

  public int mouseX() {
    return mouseX;
  }

  public int mouseY() {
    return mouseY;
  }

  public int deltaX() {
    return deltaX;
  }

  public int deltaY() {
    return deltaY;
  }
}
