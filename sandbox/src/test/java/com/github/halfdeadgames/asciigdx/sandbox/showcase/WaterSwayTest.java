package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.tween.Tween;
import org.junit.jupiter.api.Test;

class WaterSwayTest {

    private static final float PEAK_DEGREES = 3f;
    private static final float EPSILON = 1e-5f;
    private final WaterSway sway = new WaterSway(new Tween(6.0f, 1.6f, 0f, 1f), PEAK_DEGREES, 1.25f);

    @Test
    void isZeroOutsideTheSway() {
        assertEquals(0f, sway.degreesAt(0f), EPSILON);
        assertEquals(0f, sway.degreesAt(5.99f), EPSILON);
        assertEquals(0f, sway.degreesAt(7.6f), EPSILON);
        assertEquals(0f, sway.degreesAt(11f), EPSILON);
    }

    @Test
    void isZeroAtBothEnds() {
        assertEquals(0f, sway.degreesAt(6.0f), EPSILON);
        assertEquals(0f, sway.degreesAt(7.5999f), 1e-3f);
    }

    @Test
    void crossesZeroEveryHalfCycle() {
        assertEquals(0f, sway.degreesAt(6.4f), 1e-3f);
        assertEquals(0f, sway.degreesAt(6.8f), 1e-3f);
        assertEquals(0f, sway.degreesAt(7.2f), 1e-3f);
    }

    @Test
    void neverExceedsThePeak() {
        for (float time = 5.9f; time < 7.7f; time += 0.001f) {
            assertTrue(Math.abs(sway.degreesAt(time)) <= PEAK_DEGREES + EPSILON, "time " + time);
        }
    }

    @Test
    void rocksBothWaysDuringTheSway() {
        assertTrue(sway.degreesAt(6.2f) > 0f);
        assertTrue(sway.degreesAt(6.6f) < 0f);
        assertNotEquals(0f, sway.degreesAt(6.7f));
    }

    @Test
    void rejectsNullTween() {
        assertThrows(IllegalArgumentException.class, () -> new WaterSway(null, 1f, 1f));
    }
}
