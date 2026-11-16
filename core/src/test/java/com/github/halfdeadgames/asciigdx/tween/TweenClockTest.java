package com.github.halfdeadgames.asciigdx.tween;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TweenClockTest {

    private static final float EPSILON = 1e-5f;

    @Test
    void updateIgnoresNanDelta() {
        TweenClock clock = TweenClock.looping(10f, 1f);
        clock.setTime(3f);
        clock.update(Float.NaN);
        assertEquals(3f, clock.time(), EPSILON);
    }

    @Test
    void setTimeRejectsNonFiniteValues() {
        TweenClock clock = TweenClock.looping(10f, 1f);
        assertThrows(IllegalArgumentException.class, () -> clock.setTime(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> clock.setTime(Float.POSITIVE_INFINITY));
    }

    @Test
    void updateAccumulatesTime() {
        TweenClock clock = TweenClock.looping(10f, 1f);
        clock.update(0.5f);
        clock.update(0.25f);
        assertEquals(0.75f, clock.time(), EPSILON);
    }

    @Test
    void updateWrapsWhenCrossingLoopEnd() {
        TweenClock clock = TweenClock.looping(2f, 5f);
        clock.setTime(1.75f);
        clock.update(0.5f);
        assertEquals(0.25f, clock.time(), EPSILON);
    }

    @Test
    void updateWrapsMultipleLoopsInOneDelta() {
        TweenClock clock = TweenClock.looping(1f, 10f);
        clock.update(3.25f);
        assertEquals(0.25f, clock.time(), EPSILON);
    }

    @Test
    void updateCapsDelta() {
        TweenClock clock = TweenClock.looping(100f, 0.25f);
        clock.update(50f);
        assertEquals(0.25f, clock.time(), EPSILON);
    }

    @Test
    void updateIgnoresNegativeDelta() {
        TweenClock clock = TweenClock.looping(10f, 1f);
        clock.setTime(3f);
        clock.update(-2f);
        assertEquals(3f, clock.time(), EPSILON);
    }

    @Test
    void setTimeWrapsOverLength() {
        TweenClock clock = TweenClock.looping(4f, 1f);
        clock.setTime(9f);
        assertEquals(1f, clock.time(), EPSILON);
    }

    @Test
    void setTimeWrapsNegative() {
        TweenClock clock = TweenClock.looping(4f, 1f);
        clock.setTime(-1f);
        assertEquals(3f, clock.time(), EPSILON);
    }

    @Test
    void timeStaysBelowLoopLengthForTinyNegative() {
        TweenClock clock = TweenClock.looping(4f, 1f);
        clock.setTime(-1e-10f);
        assertTrue(clock.time() >= 0f && clock.time() < 4f);
    }

    @Test
    void loopingClockReportsLoopSeconds() {
        TweenClock clock = TweenClock.looping(6f, 1f);
        assertTrue(clock.isLooping());
        assertEquals(6f, clock.loopSeconds(), EPSILON);
    }

    @Test
    void loopingRejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> TweenClock.looping(0f, 1f));
        assertThrows(IllegalArgumentException.class, () -> TweenClock.looping(-1f, 1f));
        assertThrows(IllegalArgumentException.class, () -> TweenClock.looping(1f, 0f));
        assertThrows(IllegalArgumentException.class, () -> TweenClock.looping(1f, -1f));
        assertThrows(IllegalArgumentException.class, () -> TweenClock.looping(Float.NaN, 1f));
        assertThrows(IllegalArgumentException.class,
                () -> TweenClock.looping(Float.POSITIVE_INFINITY, 1f));
        assertThrows(IllegalArgumentException.class, () -> TweenClock.looping(1f, Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> TweenClock.looping(1f, Float.POSITIVE_INFINITY));
    }

    @Test
    void unboundedAccumulatesPastAnyLength() {
        TweenClock clock = TweenClock.unbounded(10f);
        for (int i = 0; i < 5; i++) {
            clock.update(10f);
        }
        assertEquals(50f, clock.time(), EPSILON);
        assertFalse(clock.isLooping());
    }

    @Test
    void unboundedCapsDelta() {
        TweenClock clock = TweenClock.unbounded(0.25f);
        clock.update(50f);
        assertEquals(0.25f, clock.time(), EPSILON);
    }

    @Test
    void unboundedIgnoresNonPositiveAndNanDelta() {
        TweenClock clock = TweenClock.unbounded(1f);
        clock.setTime(3f);
        clock.update(-2f);
        clock.update(0f);
        clock.update(Float.NaN);
        assertEquals(3f, clock.time(), EPSILON);
    }

    @Test
    void unboundedSetTimeKeepsValueWithoutWrapping() {
        TweenClock clock = TweenClock.unbounded(1f);
        clock.setTime(1234f);
        assertEquals(1234f, clock.time(), EPSILON);
    }

    @Test
    void unboundedSetTimeRejectsNegative() {
        TweenClock clock = TweenClock.unbounded(1f);
        assertThrows(IllegalArgumentException.class, () -> clock.setTime(-0.5f));
    }

    @Test
    void unboundedSetTimeRejectsNonFiniteValues() {
        TweenClock clock = TweenClock.unbounded(1f);
        assertThrows(IllegalArgumentException.class, () -> clock.setTime(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> clock.setTime(Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> clock.setTime(Float.NEGATIVE_INFINITY));
    }

    @Test
    void unboundedLoopSecondsThrows() {
        TweenClock clock = TweenClock.unbounded(1f);
        assertThrows(IllegalStateException.class, clock::loopSeconds);
    }

    @Test
    void unboundedRejectsInvalidMaxDelta() {
        assertThrows(IllegalArgumentException.class, () -> TweenClock.unbounded(0f));
        assertThrows(IllegalArgumentException.class, () -> TweenClock.unbounded(-1f));
        assertThrows(IllegalArgumentException.class, () -> TweenClock.unbounded(Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> TweenClock.unbounded(Float.POSITIVE_INFINITY));
    }
}
