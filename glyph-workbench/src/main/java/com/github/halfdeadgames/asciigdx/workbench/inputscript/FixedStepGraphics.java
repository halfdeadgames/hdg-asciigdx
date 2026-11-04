package com.github.halfdeadgames.asciigdx.workbench.inputscript;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.GL31;
import com.badlogic.gdx.graphics.GL32;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.GLVersion;

/** Wraps the real graphics but reports the same frame time every frame, so scripted runs animate identically. */
public final class FixedStepGraphics implements Graphics {

  public static final float FRAME_SECONDS = 1f / 60f;

  private final Graphics delegate;

  public FixedStepGraphics(Graphics delegate) {
    this.delegate = delegate;
  }

  @Override
  public float getDeltaTime() {
    return FRAME_SECONDS;
  }

  @Override
  public float getRawDeltaTime() {
    return FRAME_SECONDS;
  }

  @Override
  public GLVersion getGLVersion() {
    return delegate.getGLVersion();
  }

  @Override
  public boolean isGL30Available() {
    return delegate.isGL30Available();
  }

  @Override
  public boolean isGL31Available() {
    return delegate.isGL31Available();
  }

  @Override
  public boolean isGL32Available() {
    return delegate.isGL32Available();
  }

  @Override
  public GL20 getGL20() {
    return delegate.getGL20();
  }

  @Override
  public GL30 getGL30() {
    return delegate.getGL30();
  }

  @Override
  public GL31 getGL31() {
    return delegate.getGL31();
  }

  @Override
  public GL32 getGL32() {
    return delegate.getGL32();
  }

  @Override
  public void setGL20(GL20 gl20) {
    delegate.setGL20(gl20);
  }

  @Override
  public void setGL30(GL30 gl30) {
    delegate.setGL30(gl30);
  }

  @Override
  public void setGL31(GL31 gl31) {
    delegate.setGL31(gl31);
  }

  @Override
  public void setGL32(GL32 gl32) {
    delegate.setGL32(gl32);
  }

  @Override
  public int getWidth() {
    return delegate.getWidth();
  }

  @Override
  public int getHeight() {
    return delegate.getHeight();
  }

  @Override
  public int getBackBufferWidth() {
    return delegate.getBackBufferWidth();
  }

  @Override
  public int getBackBufferHeight() {
    return delegate.getBackBufferHeight();
  }

  @Override
  public float getBackBufferScale() {
    return delegate.getBackBufferScale();
  }

  @Override
  public int getSafeInsetLeft() {
    return delegate.getSafeInsetLeft();
  }

  @Override
  public int getSafeInsetTop() {
    return delegate.getSafeInsetTop();
  }

  @Override
  public int getSafeInsetBottom() {
    return delegate.getSafeInsetBottom();
  }

  @Override
  public int getSafeInsetRight() {
    return delegate.getSafeInsetRight();
  }

  @Override
  public long getFrameId() {
    return delegate.getFrameId();
  }

  @Override
  public int getFramesPerSecond() {
    return delegate.getFramesPerSecond();
  }

  @Override
  public GraphicsType getType() {
    return delegate.getType();
  }

  @Override
  public float getPpiX() {
    return delegate.getPpiX();
  }

  @Override
  public float getPpiY() {
    return delegate.getPpiY();
  }

  @Override
  public float getPpcX() {
    return delegate.getPpcX();
  }

  @Override
  public float getPpcY() {
    return delegate.getPpcY();
  }

  @Override
  public float getDensity() {
    return delegate.getDensity();
  }

  @Override
  public boolean supportsDisplayModeChange() {
    return delegate.supportsDisplayModeChange();
  }

  @Override
  public Monitor getPrimaryMonitor() {
    return delegate.getPrimaryMonitor();
  }

  @Override
  public Monitor getMonitor() {
    return delegate.getMonitor();
  }

  @Override
  public Monitor[] getMonitors() {
    return delegate.getMonitors();
  }

  @Override
  public DisplayMode[] getDisplayModes() {
    return delegate.getDisplayModes();
  }

  @Override
  public DisplayMode[] getDisplayModes(Monitor monitor) {
    return delegate.getDisplayModes(monitor);
  }

  @Override
  public DisplayMode getDisplayMode() {
    return delegate.getDisplayMode();
  }

  @Override
  public DisplayMode getDisplayMode(Monitor monitor) {
    return delegate.getDisplayMode(monitor);
  }

  @Override
  public boolean setFullscreenMode(DisplayMode displayMode) {
    return delegate.setFullscreenMode(displayMode);
  }

  @Override
  public boolean setWindowedMode(int width, int height) {
    return delegate.setWindowedMode(width, height);
  }

  @Override
  public void setTitle(String title) {
    delegate.setTitle(title);
  }

  @Override
  public void setUndecorated(boolean undecorated) {
    delegate.setUndecorated(undecorated);
  }

  @Override
  public void setResizable(boolean resizable) {
    delegate.setResizable(resizable);
  }

  @Override
  public void setVSync(boolean vsync) {
    delegate.setVSync(vsync);
  }

  @Override
  public void setForegroundFPS(int fps) {
    delegate.setForegroundFPS(fps);
  }

  @Override
  public BufferFormat getBufferFormat() {
    return delegate.getBufferFormat();
  }

  @Override
  public boolean supportsExtension(String extension) {
    return delegate.supportsExtension(extension);
  }

  @Override
  public void setContinuousRendering(boolean isContinuous) {
    delegate.setContinuousRendering(isContinuous);
  }

  @Override
  public boolean isContinuousRendering() {
    return delegate.isContinuousRendering();
  }

  @Override
  public void requestRendering() {
    delegate.requestRendering();
  }

  @Override
  public boolean isFullscreen() {
    return delegate.isFullscreen();
  }

  @Override
  public Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot) {
    return delegate.newCursor(pixmap, xHotspot, yHotspot);
  }

  @Override
  public void setCursor(Cursor cursor) {
    delegate.setCursor(cursor);
  }

  @Override
  public void setSystemCursor(Cursor.SystemCursor systemCursor) {
    delegate.setSystemCursor(systemCursor);
  }
}
