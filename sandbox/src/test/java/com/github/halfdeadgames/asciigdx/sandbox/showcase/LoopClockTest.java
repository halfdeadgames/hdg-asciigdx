package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LoopClockTest {

    private static final float EPSILON = 1e-5f;

    @Test
    void advanceIgnoresNaNDelta() {
        LoopClock clock = new LoopClock(10f, 1f);
        clock.setTime(3f);
        clock.advance(Float.NaN);
        assertEquals(3f, clock.time(), EPSILON);
    }

    @Test
    void setTimeRejectsNonFiniteValues() {
        LoopClock clock = new LoopClock(10f, 1f);
        assertThrows(IllegalArgumentException.class, () -> clock.setTime(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> clock.setTime(Float.POSITIVE_INFINITY));
    }

    @Test
    void advanceAccumulatesTime() {
        LoopClock clock = new LoopClock(10f, 1f);
        clock.advance(0.5f);
        clock.advance(0.25f);
        assertEquals(0.75f, clock.time(), EPSILON);
    }

    @Test
    void advanceWrapsWhenCrossingLoopEnd() {
        LoopClock clock = new LoopClock(2f, 5f);
        clock.setTime(1.75f);
        clock.advance(0.5f);
        assertEquals(0.25f, clock.time(), EPSILON);
    }

    @Test
    void advanceWrapsMultipleLoopsInOneDelta() {
        LoopClock clock = new LoopClock(1f, 10f);
        clock.advance(3.25f);
        assertEquals(0.25f, clock.time(), EPSILON);
    }

    @Test
    void advanceCapsDelta() {
        LoopClock clock = new LoopClock(100f, 0.25f);
        clock.advance(50f);
        assertEquals(0.25f, clock.time(), EPSILON);
    }

    @Test
    void advanceIgnoresNegativeDelta() {
        LoopClock clock = new LoopClock(10f, 1f);
        clock.setTime(3f);
        clock.advance(-2f);
        assertEquals(3f, clock.time(), EPSILON);
    }

    @Test
    void setTimeWrapsOverLength() {
        LoopClock clock = new LoopClock(4f, 1f);
        clock.setTime(9f);
        assertEquals(1f, clock.time(), EPSILON);
    }

    @Test
    void setTimeWrapsNegative() {
        LoopClock clock = new LoopClock(4f, 1f);
        clock.setTime(-1f);
        assertEquals(3f, clock.time(), EPSILON);
    }

    @Test
    void timeStaysBelowLoopLengthForTinyNegative() {
        LoopClock clock = new LoopClock(4f, 1f);
        clock.setTime(-1e-10f);
        assertTrue(clock.time() >= 0f && clock.time() < 4f);
    }

    @Test
    void reportsLoopSeconds() {
        assertEquals(6f, new LoopClock(6f, 1f).loopSeconds(), EPSILON);
    }

    @Test
    void rejectsNonPositiveArguments() {
        assertThrows(IllegalArgumentException.class, () -> new LoopClock(0f, 1f));
        assertThrows(IllegalArgumentException.class, () -> new LoopClock(-1f, 1f));
        assertThrows(IllegalArgumentException.class, () -> new LoopClock(1f, 0f));
        assertThrows(IllegalArgumentException.class, () -> new LoopClock(1f, -1f));
    }
}
