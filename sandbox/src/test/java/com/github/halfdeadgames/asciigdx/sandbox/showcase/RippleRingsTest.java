package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RippleRingsTest {

    private static final float SPEED = 9f;
    private static final float HALF_WIDTH = 1.2f;
    private static final float EPSILON = 1e-5f;
    private final RippleRings rings = new RippleRings(7.6f, 0.15f, 3, 0.9f, SPEED, HALF_WIDTH);

    @Test
    void isZeroEverywhereBeforeAndAfterTheRipples() {
        for (float distance = 0f; distance < 14f; distance += 0.5f) {
            assertEquals(0f, rings.liftAt(distance, 7.59f), EPSILON);
            assertEquals(0f, rings.liftAt(distance, 8.8f), EPSILON);
            assertEquals(0f, rings.liftAt(distance, 11f), EPSILON);
        }
        assertFalse(rings.isActiveAt(7.59f));
        assertTrue(rings.isActiveAt(7.6f));
        assertFalse(rings.isActiveAt(8.8f));
        assertEquals(8.8f, rings.endSeconds(), EPSILON);
    }

    @Test
    void peaksOnTheRingRadius() {
        float time = 7.6f + 0.2f;
        float radius = 0.2f * SPEED;
        float expectedAmplitude = 1f - 0.2f / 0.9f;
        assertEquals(expectedAmplitude, rings.liftAt(radius, time), EPSILON);
        assertTrue(rings.liftAt(radius, time) > rings.liftAt(radius + 0.5f, time));
        assertTrue(rings.liftAt(radius, time) > rings.liftAt(radius - 0.5f, time));
    }

    @Test
    void isZeroWellInsideAndOutsideARing() {
        RippleRings single = new RippleRings(7.6f, 0.15f, 1, 0.9f, SPEED, HALF_WIDTH);
        float time = 7.6f + 0.3f;
        float radius = 0.3f * SPEED;
        assertEquals(0f, single.liftAt(radius + HALF_WIDTH + 0.1f, time), EPSILON);
        assertEquals(0f, single.liftAt(radius - HALF_WIDTH - 0.1f, time), EPSILON);
    }

    @Test
    void decaysWithAge() {
        float early = rings.liftAt(0.1f * SPEED, 7.7f);
        float late = rings.liftAt(0.6f * SPEED, 8.2f);
        assertTrue(early > late);
        assertTrue(late > 0f);
    }

    @Test
    void laterRingsStartLater() {
        RippleRings single = new RippleRings(7.6f, 0.15f, 1, 0.9f, SPEED, HALF_WIDTH);
        for (float distance = 0f; distance < 6f; distance += 0.25f) {
            assertEquals(single.liftAt(distance, 7.7f), rings.liftAt(distance, 7.7f), EPSILON);
        }
        float secondRingRadius = (7.9f - 7.75f) * SPEED;
        assertEquals(0f, single.liftAt(secondRingRadius, 7.9f), EPSILON);
        assertTrue(rings.liftAt(secondRingRadius, 7.9f) > 0f);
    }

    @Test
    void sameInputsGiveSameOutputs() {
        RippleRings other = new RippleRings(7.6f, 0.15f, 3, 0.9f, SPEED, HALF_WIDTH);
        for (float distance = 0f; distance < 10f; distance += 0.37f) {
            assertEquals(rings.liftAt(distance, 8.1f), other.liftAt(distance, 8.1f));
        }
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new RippleRings(Float.NaN, 0.15f, 3, 0.9f, 9f, 1.2f));
        assertThrows(IllegalArgumentException.class, () -> new RippleRings(7.6f, -0.1f, 3, 0.9f, 9f, 1.2f));
        assertThrows(IllegalArgumentException.class, () -> new RippleRings(7.6f, 0.15f, 0, 0.9f, 9f, 1.2f));
        assertThrows(IllegalArgumentException.class, () -> new RippleRings(7.6f, 0.15f, 3, 0f, 9f, 1.2f));
        assertThrows(IllegalArgumentException.class, () -> new RippleRings(7.6f, 0.15f, 3, 0.9f, -9f, 1.2f));
        assertThrows(IllegalArgumentException.class, () -> new RippleRings(7.6f, 0.15f, 3, 0.9f, 9f, Float.POSITIVE_INFINITY));
    }
}
