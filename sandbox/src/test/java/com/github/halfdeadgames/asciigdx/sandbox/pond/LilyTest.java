package com.github.halfdeadgames.asciigdx.sandbox.pond;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LilyTest {

    private static final float TOLERANCE = 1e-5f;
    private static final float MAX_OFFSET_LENGTH = 0.75f;
    private static final float FRAME_SECONDS = 1f / 60f;
    private static final float LONG_ENOUGH_SECONDS = 30f;
    private static final float MOVE_START_SECONDS = 0.4f;
    private static final float PUSH_START_SECONDS = 0.1f;
    private static final int OVERFLOW_PUSH_COUNT = 20;

    private static Lily pushedAndMovedLily() {
        var lily = new Lily();
        lily.push(2f, -1f, PUSH_START_SECONDS);
        lily.setTarget(0.3f, 0.2f, MOVE_START_SECONDS);
        return lily;
    }

    private static void advanceInSteps(Lily lily, float fromSeconds, float toSeconds,
            float stepSeconds) {
        var now = fromSeconds;
        while (now + stepSeconds < toSeconds) {
            now += stepSeconds;
            lily.advanceTo(now);
        }
        lily.advanceTo(toSeconds);
    }

    @Test
    void offsetDoesNotDependOnTheFrameRate() {
        var steady = pushedAndMovedLily();
        var irregular = pushedAndMovedLily();
        var jumped = pushedAndMovedLily();
        var checkSeconds = new float[] {0.25f, 0.7f, 1.3f, 2.1f};
        var previous = 0f;
        for (var seconds : checkSeconds) {
            advanceInSteps(steady, previous, seconds, FRAME_SECONDS);
            var shortStepEnd = previous + 1f / 300f;
            irregular.advanceTo(shortStepEnd);
            advanceInSteps(irregular, shortStepEnd, seconds, 1f / 7f);
            jumped.advanceTo(seconds);
            assertEquals(jumped.getOffsetX(), steady.getOffsetX(), TOLERANCE);
            assertEquals(jumped.getOffsetY(), steady.getOffsetY(), TOLERANCE);
            assertEquals(jumped.getOffsetX(), irregular.getOffsetX(), TOLERANCE);
            assertEquals(jumped.getOffsetY(), irregular.getOffsetY(), TOLERANCE);
            previous = seconds;
        }
    }

    @Test
    void offsetNeverExceedsTheMaximumForAHugePush() {
        var lily = new Lily();
        lily.push(500f, -300f, 0f);
        for (var now = 0f; now < 3f; now += FRAME_SECONDS) {
            lily.advanceTo(now);
            var length = (float) Math.hypot(lily.getOffsetX(), lily.getOffsetY());
            assertTrue(length <= MAX_OFFSET_LENGTH);
        }
    }

    @Test
    void lilyGoesHomeWithExactZeroOffsetAfterTargetReturnsToZero() {
        var lily = pushedAndMovedLily();
        lily.setTarget(0f, 0f, 3f);
        lily.advanceTo(LONG_ENOUGH_SECONDS);
        assertTrue(lily.isHome());
        assertEquals(0f, lily.getOffsetX());
        assertEquals(0f, lily.getOffsetY());
    }

    @Test
    void delayedPushHasNoEffectBeforeItsStart() {
        var lily = new Lily();
        lily.push(3f, 0f, 1f);
        lily.advanceTo(0.99f);
        assertEquals(0f, lily.getOffsetX());
        assertFalse(lily.isHome());
        lily.advanceTo(1.05f);
        assertTrue(lily.getOffsetX() > 0f);
    }

    @Test
    void overflowingTheEventStoreDoesNotThrowAndTheLilyStillGoesHome() {
        var lily = new Lily();
        assertDoesNotThrow(() -> {
            for (var i = 0; i < OVERFLOW_PUSH_COUNT; i++) {
                lily.push(1f, 1f, 0f);
            }
        });
        lily.advanceTo(LONG_ENOUGH_SECONDS);
        assertTrue(lily.isHome());
    }

    @Test
    void overflowingWithRestMovesStillEndsAtTheLastTarget() {
        var lily = new Lily();
        for (var i = 1; i <= OVERFLOW_PUSH_COUNT; i++) {
            lily.setTarget(i * 0.01f, 0f, 0f);
        }
        lily.advanceTo(LONG_ENOUGH_SECONDS);
        assertEquals(OVERFLOW_PUSH_COUNT * 0.01f, lily.getOffsetX(), TOLERANCE);
    }

    @Test
    void settingTheSameTargetAddsNoEvent() {
        var lily = new Lily();
        lily.setTarget(0.3f, 0.1f, 0f);
        var eventCount = lily.getEventCount();
        lily.setTarget(0.3f, 0.1f, 1f);
        assertEquals(eventCount, lily.getEventCount());
    }

    @Test
    void resetClearsEventsTargetAndOffset() {
        var lily = pushedAndMovedLily();
        lily.advanceTo(0.6f);
        lily.reset();
        assertTrue(lily.isHome());
        assertEquals(0, lily.getEventCount());
        assertEquals(0f, lily.getTargetX());
        assertEquals(0f, lily.getTargetY());
        assertEquals(0f, lily.getOffsetX());
        assertEquals(0f, lily.getOffsetY());
    }
}
