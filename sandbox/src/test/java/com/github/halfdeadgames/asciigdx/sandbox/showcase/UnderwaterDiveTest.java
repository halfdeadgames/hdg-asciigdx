package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Interpolation;
import com.github.halfdeadgames.asciigdx.tween.Tween;
import org.junit.jupiter.api.Test;

class UnderwaterDiveTest {

    private static final float EPSILON = 1e-5f;
    private static final float MINIMUM_SEE_THROUGH = 0.35f;
    private final UnderwaterDive dive = new UnderwaterDive(
            new Tween(7.6f, 0.4f, 0f, 1f, Interpolation.sineOut),
            new Tween(8.0f, 0.6f, 0f, 1f, Interpolation.sine));

    @Test
    void isUnderwaterOnlyInsideTheWindow() {
        assertFalse(dive.isUnderwaterAt(0f));
        assertFalse(dive.isUnderwaterAt(7.59f));
        assertTrue(dive.isUnderwaterAt(7.6f));
        assertTrue(dive.isUnderwaterAt(8.59f));
        assertFalse(dive.isUnderwaterAt(8.6f));
        assertFalse(dive.isUnderwaterAt(11.4f));
    }

    @Test
    void surfaceProgressRunsOnlyDuringTheSurfacing() {
        assertEquals(0f, dive.surfaceProgressAt(7.9f), EPSILON);
        assertEquals(0f, dive.surfaceProgressAt(8.0f), EPSILON);
        assertEquals(0.5f, dive.surfaceProgressAt(8.3f), EPSILON);
        assertEquals(1f, dive.surfaceProgressAt(8.6f), EPSILON);
        assertEquals(1f, dive.surfaceProgressAt(11.4f), EPSILON);
    }

    @Test
    void seeThroughIsZeroOutsideTheWindow() {
        assertEquals(0f, dive.seeThroughAt(0f), EPSILON);
        assertEquals(0f, dive.seeThroughAt(7.59f), EPSILON);
        assertEquals(0f, dive.seeThroughAt(7.6f), EPSILON);
        assertEquals(0f, dive.seeThroughAt(8.6f), EPSILON);
        assertEquals(0f, dive.seeThroughAt(0f), EPSILON);
        assertEquals(0f, dive.seeThroughAt(11.4f), EPSILON);
    }

    @Test
    void seeThroughRampsInOverTheFirstTenthOfASecond() {
        assertTrue(dive.seeThroughAt(7.65f) > 0f);
        assertTrue(dive.seeThroughAt(7.65f) < dive.seeThroughAt(7.7f));
    }

    @Test
    void seeThroughFallsToTheMinimumAtTheSinkEnd() {
        assertTrue(dive.seeThroughAt(7.7f) < 1f);
        assertTrue(dive.seeThroughAt(7.8f) < dive.seeThroughAt(7.7f));
        assertTrue(dive.seeThroughAt(7.95f) < dive.seeThroughAt(7.8f));
        assertEquals(MINIMUM_SEE_THROUGH, dive.seeThroughAt(8.0f), EPSILON);
    }

    @Test
    void seeThroughRisesDuringTheSurfacing() {
        assertTrue(dive.seeThroughAt(8.2f) > MINIMUM_SEE_THROUGH);
        assertTrue(dive.seeThroughAt(8.4f) > dive.seeThroughAt(8.2f));
        assertTrue(dive.seeThroughAt(8.5f) > dive.seeThroughAt(8.4f));
    }

    @Test
    void seeThroughRampsOutAtTheEndOfTheWindow() {
        assertTrue(dive.seeThroughAt(8.55f) < dive.seeThroughAt(8.5f));
        assertTrue(dive.seeThroughAt(8.599f) < 0.02f);
    }

    @Test
    void seeThroughHasNoJumps() {
        for (float time = 7.7f; time < 8.55f; time += 0.001f) {
            float step = Math.abs(dive.seeThroughAt(time + 0.001f) - dive.seeThroughAt(time));
            assertTrue(step < 0.03f, "jump at " + time + ": " + step);
        }
    }

    @Test
    void waterIsMostSeeThroughAtTheCenterAtTheDeepestPoint() {
        assertEquals(1f - 0.5f * MINIMUM_SEE_THROUGH, dive.waterAlphaAt(0f, 8.0f), EPSILON);
    }

    @Test
    void waterIsOpaqueFarAwayAndOutsideTheWindow() {
        assertEquals(1f, dive.waterAlphaAt(1.5f, 8.0f), EPSILON);
        assertEquals(1f, dive.waterAlphaAt(4f, 8.0f), EPSILON);
        assertEquals(1f, dive.waterAlphaAt(0f, 7.59f), EPSILON);
        assertEquals(1f, dive.waterAlphaAt(0f, 8.6f), EPSILON);
        assertEquals(1f, dive.waterAlphaAt(0f, 0f), EPSILON);
        assertEquals(1f, dive.waterAlphaAt(0f, 11.4f), EPSILON);
    }

    @Test
    void waterGrowsMoreOpaqueWithDistance() {
        assertTrue(dive.waterAlphaAt(0.5f, 8.0f) > dive.waterAlphaAt(0f, 8.0f));
        assertTrue(dive.waterAlphaAt(1f, 8.0f) > dive.waterAlphaAt(0.5f, 8.0f));
    }

    @Test
    void rejectsInvalidArguments() {
        Tween sink = new Tween(7.6f, 0.4f, 0f, 1f);
        assertThrows(IllegalArgumentException.class, () -> new UnderwaterDive(null, sink));
        assertThrows(IllegalArgumentException.class, () -> new UnderwaterDive(sink, null));
        assertThrows(IllegalArgumentException.class,
                () -> new UnderwaterDive(sink, new Tween(7.9f, 0.6f, 0f, 1f)));
        assertThrows(IllegalArgumentException.class, () -> dive.waterAlphaAt(-1f, 8.0f));
        assertThrows(IllegalArgumentException.class, () -> dive.waterAlphaAt(Float.NaN, 8.0f));
    }
}
