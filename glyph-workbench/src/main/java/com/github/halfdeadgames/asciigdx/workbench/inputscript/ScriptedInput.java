package com.github.halfdeadgames.asciigdx.workbench.inputscript;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

public final class ScriptedInput implements Input {

  private static final int ONLY_POINTER = 0;

  private final Input delegate;
  private final ScriptedInputState state;
  private InputProcessor processor;

  public ScriptedInput(Input delegate, ScriptedInputState state) {
    this.delegate = delegate;
    this.state = state;
  }

  public InputProcessor currentProcessor() {
    return processor;
  }

  @Override
  public void setInputProcessor(InputProcessor processor) {
    this.processor = processor;
  }

  @Override
  public InputProcessor getInputProcessor() {
    return processor;
  }

  @Override
  public int getX() {
    return state.mouseX();
  }

  @Override
  public int getX(int pointer) {
    return pointer == ONLY_POINTER ? state.mouseX() : 0;
  }

  @Override
  public int getY() {
    return state.mouseY();
  }

  @Override
  public int getY(int pointer) {
    return pointer == ONLY_POINTER ? state.mouseY() : 0;
  }

  @Override
  public int getDeltaX() {
    return state.deltaX();
  }

  @Override
  public int getDeltaX(int pointer) {
    return pointer == ONLY_POINTER ? state.deltaX() : 0;
  }

  @Override
  public int getDeltaY() {
    return state.deltaY();
  }

  @Override
  public int getDeltaY(int pointer) {
    return pointer == ONLY_POINTER ? state.deltaY() : 0;
  }

  @Override
  public boolean isTouched() {
    return state.isTouched();
  }

  @Override
  public boolean isTouched(int pointer) {
    return pointer == ONLY_POINTER && state.isTouched();
  }

  @Override
  public boolean justTouched() {
    return state.justTouched();
  }

  @Override
  public float getPressure() {
    return state.isTouched() ? 1f : 0f;
  }

  @Override
  public float getPressure(int pointer) {
    return pointer == ONLY_POINTER ? getPressure() : 0f;
  }

  @Override
  public int getMaxPointers() {
    return 1;
  }

  @Override
  public boolean isButtonPressed(int button) {
    return state.isButtonDown(button);
  }

  @Override
  public boolean isButtonJustPressed(int button) {
    return state.isButtonJustPressed(button);
  }

  @Override
  public boolean isKeyPressed(int key) {
    return state.isKeyDown(key);
  }

  @Override
  public boolean isKeyJustPressed(int key) {
    return state.isKeyJustPressed(key);
  }

  @Override
  public float getAccelerometerX() {
    return delegate.getAccelerometerX();
  }

  @Override
  public float getAccelerometerY() {
    return delegate.getAccelerometerY();
  }

  @Override
  public float getAccelerometerZ() {
    return delegate.getAccelerometerZ();
  }

  @Override
  public float getGyroscopeX() {
    return delegate.getGyroscopeX();
  }

  @Override
  public float getGyroscopeY() {
    return delegate.getGyroscopeY();
  }

  @Override
  public float getGyroscopeZ() {
    return delegate.getGyroscopeZ();
  }

  @Override
  public void getTextInput(
      TextInputListener listener, String title, String text, String hint) {
    delegate.getTextInput(listener, title, text, hint);
  }

  @Override
  public void getTextInput(
      TextInputListener listener,
      String title,
      String text,
      String hint,
      OnscreenKeyboardType type) {
    delegate.getTextInput(listener, title, text, hint, type);
  }

  @Override
  public void setOnscreenKeyboardVisible(boolean visible) {
    delegate.setOnscreenKeyboardVisible(visible);
  }

  @Override
  public void setOnscreenKeyboardVisible(boolean visible, OnscreenKeyboardType type) {
    delegate.setOnscreenKeyboardVisible(visible, type);
  }

  @Override
  public void vibrate(int milliseconds) {
    delegate.vibrate(milliseconds);
  }

  @Override
  public void vibrate(int milliseconds, boolean fallback) {
    delegate.vibrate(milliseconds, fallback);
  }

  @Override
  public void vibrate(int milliseconds, int amplitude, boolean fallback) {
    delegate.vibrate(milliseconds, amplitude, fallback);
  }

  @Override
  public void vibrate(VibrationType vibrationType) {
    delegate.vibrate(vibrationType);
  }

  @Override
  public void openTextInputField(com.badlogic.gdx.input.NativeInputConfiguration configuration) {
    delegate.openTextInputField(configuration);
  }

  @Override
  public void setKeyboardHeightObserver(KeyboardHeightObserver observer) {
    delegate.setKeyboardHeightObserver(observer);
  }

  @Override
  public float getAzimuth() {
    return delegate.getAzimuth();
  }

  @Override
  public float getPitch() {
    return delegate.getPitch();
  }

  @Override
  public float getRoll() {
    return delegate.getRoll();
  }

  @Override
  public void getRotationMatrix(float[] matrix) {
    delegate.getRotationMatrix(matrix);
  }

  @Override
  public long getCurrentEventTime() {
    return delegate.getCurrentEventTime();
  }

  @Override
  public void setCatchKey(int keycode, boolean catchKey) {
    delegate.setCatchKey(keycode, catchKey);
  }

  @Override
  public boolean isCatchKey(int keycode) {
    return delegate.isCatchKey(keycode);
  }

  @Override
  public boolean isPeripheralAvailable(Peripheral peripheral) {
    return delegate.isPeripheralAvailable(peripheral);
  }

  @Override
  public int getRotation() {
    return delegate.getRotation();
  }

  @Override
  public Orientation getNativeOrientation() {
    return delegate.getNativeOrientation();
  }

  @Override
  public void setCursorCatched(boolean catched) {
    delegate.setCursorCatched(catched);
  }

  @Override
  public boolean isCursorCatched() {
    return delegate.isCursorCatched();
  }

  @Override
  public void setCursorPosition(int x, int y) {
    delegate.setCursorPosition(x, y);
  }
}
