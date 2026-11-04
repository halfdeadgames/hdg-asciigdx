package com.github.halfdeadgames.asciigdx.workbench.inputscript;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.Graphics;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;

class FixedStepGraphicsTest {

  private static final int REAL_WIDTH = 640;
  private static final float REAL_DELTA = 0.5f;

  private final Graphics real =
      (Graphics)
          Proxy.newProxyInstance(
              Graphics.class.getClassLoader(),
              new Class<?>[] {Graphics.class},
              (proxy, method, args) ->
                  switch (method.getName()) {
                    case "getWidth" -> REAL_WIDTH;
                    case "getDeltaTime", "getRawDeltaTime" -> REAL_DELTA;
                    default -> throw new UnsupportedOperationException(method.getName());
                  });

  private final FixedStepGraphics fixed = new FixedStepGraphics(real);

  @Test
  void reportsTheFixedFrameTimeInsteadOfTheRealOne() {
    assertEquals(1f / 60f, fixed.getDeltaTime());
    assertEquals(1f / 60f, fixed.getRawDeltaTime());
  }

  @Test
  void otherCallsGoToTheWrappedGraphics() {
    assertEquals(REAL_WIDTH, fixed.getWidth());
  }
}
