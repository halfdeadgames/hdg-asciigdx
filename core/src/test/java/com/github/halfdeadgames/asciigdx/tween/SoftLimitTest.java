package com.github.halfdeadgames.asciigdx.tween;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

class SoftLimitTest {

    private static final float KNEE = 0.5f;
    private static final float MAX = 0.75f;
    private static final float SLOPE_STEP = 1e-3f;
    private static final float SLOPE_TOLERANCE = 1e-2f;
    private static final float LENGTH_TOLERANCE = 1e-5f;
    private static final int SWEEP_COUNT = 2000;
    private static final float SWEEP_STEP = 0.005f;

    private final SoftLimit limit = SoftLimit.of(KNEE, MAX);

    @Test
    void getLimitedDistance_belowKnee_returnsDistanceUnchanged() {
        assertEquals(0f, limit.getLimitedDistance(0f));
        assertEquals(0.3f, limit.getLimitedDistance(0.3f));
    }

    @Test
    void getLimitedDistance_atKnee_returnsKneeExactly() {
        assertEquals(KNEE, limit.getLimitedDistance(KNEE));
    }

    @Test
    void getLimitedDistance_aboveKnee_staysBetweenKneeAndMax() {
        for (float distance : new float[] {0.51f, 0.7f, 1f, 2f}) {
            var result = limit.getLimitedDistance(distance);
            assertTrue(result > KNEE, "result " + result + " for " + distance);
            assertTrue(result < MAX, "result " + result + " for " + distance);
            assertTrue(result <= distance);
        }
    }

    @Test
    void getLimitedDistance_farBeyondMax_roundsToAtMostMax() {
        for (float distance : new float[] {5f, 100f, 1e30f}) {
            assertTrue(limit.getLimitedDistance(distance) <= MAX, "for " + distance);
        }
    }

    @Test
    void getLimitedDistance_sweep_isMonotonicallyIncreasing() {
        var previous = limit.getLimitedDistance(0f);
        for (int i = 1; i <= SWEEP_COUNT; i++) {
            var current = limit.getLimitedDistance(i * SWEEP_STEP);
            assertTrue(current >= previous, "decrease at step " + i);
            previous = current;
        }
    }

    @Test
    void getLimitedDistance_hugeDistance_neverExceedsMax() {
        assertTrue(limit.getLimitedDistance(1e30f) <= MAX);
        assertTrue(limit.getLimitedDistance(Float.MAX_VALUE) <= MAX);
    }

    @Test
    void getLimitedDistance_atKnee_hasSlopeOneFromBothSides() {
        var below = (limit.getLimitedDistance(KNEE) - limit.getLimitedDistance(KNEE - SLOPE_STEP))
                / SLOPE_STEP;
        var above = (limit.getLimitedDistance(KNEE + SLOPE_STEP) - limit.getLimitedDistance(KNEE))
                / SLOPE_STEP;
        assertEquals(1f, below, SLOPE_TOLERANCE);
        assertEquals(1f, above, SLOPE_TOLERANCE);
    }

    @Test
    void getLimitedDistance_zeroKnee_reducesSmallDistancesAndStaysBelowMax() {
        var zeroKnee = SoftLimit.of(0f, 1f);
        var small = zeroKnee.getLimitedDistance(0.5f);
        assertTrue(small < 0.5f);
        assertTrue(small > 0f);
        assertTrue(zeroKnee.getLimitedDistance(2f) < 1f);
        assertTrue(zeroKnee.getLimitedDistance(10f) <= 1f);
    }

    @Test
    void limit_longVector_keepsDirectionAndLimitsLength() {
        var offset = new Vector2(3f, 4f);
        var original = offset.cpy();
        limit.limit(offset);
        assertEquals(original.angleRad(), offset.angleRad(), LENGTH_TOLERANCE);
        assertEquals(limit.getLimitedDistance(original.len()), offset.len(), LENGTH_TOLERANCE);
    }

    @Test
    void limit_longVector_modifiesTheSameInstance() {
        var offset = new Vector2(0.6f, 0.8f);
        var sameReference = offset;
        limit.limit(offset);
        assertSame(sameReference, offset);
        assertTrue(offset.len() < MAX);
    }

    @Test
    void limit_zeroVector_staysUnchanged() {
        var offset = new Vector2(0f, 0f);
        limit.limit(offset);
        assertEquals(0f, offset.x);
        assertEquals(0f, offset.y);
    }

    @Test
    void limit_shortVector_staysExactlyUnchanged() {
        var offset = new Vector2(0.2f, -0.3f);
        limit.limit(offset);
        assertEquals(0.2f, offset.x);
        assertEquals(-0.3f, offset.y);
    }

    @Test
    void limit_nullVector_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> limit.limit(null));
    }

    @Test
    void limit_nonFiniteComponent_throwsAndLeavesVectorUntouched() {
        var nanOffset = new Vector2(Float.NaN, 3f);
        assertThrows(IllegalArgumentException.class, () -> limit.limit(nanOffset));
        assertTrue(Float.isNaN(nanOffset.x));
        assertEquals(3f, nanOffset.y);

        var infiniteOffset = new Vector2(2f, Float.POSITIVE_INFINITY);
        assertThrows(IllegalArgumentException.class, () -> limit.limit(infiniteOffset));
        assertEquals(2f, infiniteOffset.x);
    }

    @Test
    void of_negativeKnee_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> SoftLimit.of(-0.1f, 1f));
    }

    @Test
    void of_maxNotGreaterThanKnee_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> SoftLimit.of(1f, 1f));
        assertThrows(IllegalArgumentException.class, () -> SoftLimit.of(1f, 0.5f));
    }

    @Test
    void of_nonFiniteArguments_throwIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> SoftLimit.of(Float.NaN, 1f));
        assertThrows(IllegalArgumentException.class,
                () -> SoftLimit.of(Float.POSITIVE_INFINITY, 1f));
        assertThrows(IllegalArgumentException.class, () -> SoftLimit.of(0.5f, Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> SoftLimit.of(0.5f, Float.POSITIVE_INFINITY));
    }

    @Test
    void getLimitedDistance_negativeOrNonFiniteDistance_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> limit.getLimitedDistance(-0.1f));
        assertThrows(IllegalArgumentException.class, () -> limit.getLimitedDistance(Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> limit.getLimitedDistance(Float.POSITIVE_INFINITY));
    }

    @Test
    void getters_returnCreationValues() {
        assertEquals(KNEE, limit.getKneeDistance());
        assertEquals(MAX, limit.getMaxDistance());
    }

    @Test
    void equalsAndHashCode_sameInputs_areEqual() {
        var same = SoftLimit.of(KNEE, MAX);
        assertEquals(limit, same);
        assertEquals(limit.hashCode(), same.hashCode());
        assertEquals(limit.toString(), same.toString());
    }

    @Test
    void equals_differentInputs_areNotEqual() {
        assertNotEquals(limit, SoftLimit.of(KNEE, 0.8f));
        assertNotEquals(limit, SoftLimit.of(0.4f, MAX));
        assertNotEquals(limit, null);
    }
}
