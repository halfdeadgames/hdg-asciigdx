package com.github.halfdeadgames.asciigdx.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RipplesTest {

    private static final int MAX_RINGS = 4;
    private static final float SPEED = 3f;
    private static final float LIFE = 1f;
    private static final float HALF_WIDTH = 0.8f;
    private static final float TOLERANCE = 1e-4f;

    private static Ripples newRipples() {
        return Ripples.of(MAX_RINGS, SPEED, LIFE, HALF_WIDTH);
    }

    @Test
    void strengthIsZeroBeforeStartAndAtOrAfterLife() {
        var ripples = newRipples();
        ripples.start(5, 5, 10f);
        assertEquals(0f, ripples.getStrengthAt(5, 5, 9.9f));
        assertEquals(0f, ripples.getStrengthAt(5, 5, 10f + LIFE));
        assertEquals(0f, ripples.getStrengthAt(5, 5, 20f));
    }

    @Test
    void strengthPeaksAtStartCellAtAgeZero() {
        var ripples = newRipples();
        ripples.start(5, 5, 10f);
        assertEquals(1f, ripples.getStrengthAt(5, 5, 10f), TOLERANCE);
    }

    @Test
    void ringPositionFollowsSpeedOverTime() {
        var ripples = newRipples();
        ripples.start(5, 5, 0f);
        var arrival = 2f / SPEED;
        var expected = 1f - arrival / LIFE;
        assertEquals(expected, ripples.getStrengthAt(7, 5, arrival), TOLERANCE);
        assertTrue(ripples.getStrengthAt(7, 5, arrival - 0.1f) < expected);
        assertTrue(ripples.getStrengthAt(7, 5, arrival + 0.1f) < expected);
    }

    @Test
    void distanceUsesBothAxes() {
        var ripples = newRipples();
        ripples.start(0, 0, 0f);
        var arrival = (float) Math.sqrt(2.0) / SPEED;
        assertEquals(1f - arrival, ripples.getStrengthAt(1, 1, arrival), TOLERANCE);
    }

    @Test
    void strengthFadesWithAge() {
        var ripples = newRipples();
        ripples.start(0, 0, 0f);
        var early = ripples.getStrengthAt(1, 0, 1f / SPEED);
        var late = ripples.getStrengthAt(2, 0, 2f / SPEED);
        assertTrue(early > late);
        assertTrue(late > 0f);
    }

    @Test
    void strongestRingWinsInsteadOfSumming() {
        var ripples = newRipples();
        ripples.start(5, 5, 0f);
        ripples.start(5, 5, 0.1f);
        assertEquals(1f, ripples.getStrengthAt(5, 5, 0.1f), TOLERANCE);
    }

    @Test
    void startWhenFullReplacesTheOldestRing() {
        var ripples = newRipples();
        for (var ring = 0; ring < MAX_RINGS; ring++) {
            ripples.start(ring * 100, 0, 0f);
        }
        ripples.start(1000, 0, 0f);
        assertEquals(0f, ripples.getStrengthAt(0, 0, 0f));
        assertEquals(1f, ripples.getStrengthAt(1000, 0, 0f), TOLERANCE);
        assertEquals(1f, ripples.getStrengthAt(100, 0, 0f), TOLERANCE);
    }

    @Test
    void isActiveAtCoversTheLifeOfAnyRing() {
        var ripples = newRipples();
        assertFalse(ripples.isActiveAt(0f));
        ripples.start(0, 0, 2f);
        assertFalse(ripples.isActiveAt(1.9f));
        assertTrue(ripples.isActiveAt(2f));
        assertTrue(ripples.isActiveAt(2.9f));
        assertFalse(ripples.isActiveAt(2f + LIFE));
    }

    @Test
    void reachIsSpeedTimesLifePlusHalfWidth() {
        assertEquals(3.8f, newRipples().getReachCells(), TOLERANCE);
    }

    @Test
    void activeBoxCoversLiveRingsAndExcludesExpiredOnes() {
        var ripples = newRipples();
        var reach = (int) Math.ceil(ripples.getReachCells());
        ripples.start(100, 100, 0f);
        ripples.start(10, 20, 5f);
        ripples.start(30, 2, 5.5f);
        assertEquals(10 - reach, ripples.getActiveLeftColumn(5.6f));
        assertEquals(2 - reach, ripples.getActiveTopRow(5.6f));
        assertEquals(30 + reach, ripples.getActiveRightColumn(5.6f));
        assertEquals(20 + reach, ripples.getActiveBottomRow(5.6f));
    }

    @Test
    void activeBoxOfSingleRingSurroundsItsCell() {
        var ripples = newRipples();
        var reach = (int) Math.ceil(ripples.getReachCells());
        ripples.start(7, 9, 0f);
        assertEquals(7 - reach, ripples.getActiveLeftColumn(0.5f));
        assertEquals(9 - reach, ripples.getActiveTopRow(0.5f));
        assertEquals(7 + reach, ripples.getActiveRightColumn(0.5f));
        assertEquals(9 + reach, ripples.getActiveBottomRow(0.5f));
    }

    @Test
    void activeBoxThrowsWhenNothingIsAlive() {
        var ripples = newRipples();
        assertThrows(IllegalStateException.class, () -> ripples.getActiveLeftColumn(0f));
        assertThrows(IllegalStateException.class, () -> ripples.getActiveTopRow(0f));
        assertThrows(IllegalStateException.class, () -> ripples.getActiveRightColumn(0f));
        assertThrows(IllegalStateException.class, () -> ripples.getActiveBottomRow(0f));
        ripples.start(0, 0, 0f);
        assertThrows(IllegalStateException.class, () -> ripples.getActiveLeftColumn(LIFE));
    }

    @Test
    void clearRemovesEveryRing() {
        var ripples = newRipples();
        ripples.start(1, 1, 0f);
        ripples.clear();
        assertFalse(ripples.isActiveAt(0f));
        assertEquals(0f, ripples.getStrengthAt(1, 1, 0f));
    }

    @Test
    void nonFiniteTimeIsRejected() {
        var ripples = newRipples();
        assertThrows(IllegalArgumentException.class, () -> ripples.start(0, 0, Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> ripples.getStrengthAt(0, 0, Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> ripples.isActiveAt(Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> ripples.getActiveLeftColumn(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> ripples.getActiveTopRow(Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> ripples.getActiveRightColumn(Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> ripples.getActiveBottomRow(Float.NaN));
    }

    @Test
    void invalidFactoryArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Ripples.of(0, SPEED, LIFE, HALF_WIDTH));
        assertThrows(IllegalArgumentException.class, () -> Ripples.of(1, 0f, LIFE, HALF_WIDTH));
        assertThrows(IllegalArgumentException.class, () -> Ripples.of(1, SPEED, -1f, HALF_WIDTH));
        assertThrows(IllegalArgumentException.class, () -> Ripples.of(1, SPEED, LIFE, Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> Ripples.of(1, Float.POSITIVE_INFINITY, LIFE, HALF_WIDTH));
    }

    @Test
    void reachAboveTheMaximumIsRejected() {
        var tooFar = Ripples.MAX_REACH_CELLS * 2f;
        assertThrows(IllegalArgumentException.class, () -> Ripples.of(1, tooFar, 1f, HALF_WIDTH));
        assertThrows(IllegalArgumentException.class, () -> Ripples.of(1, 1f, tooFar, HALF_WIDTH));
        assertThrows(IllegalArgumentException.class, () -> Ripples.of(1, 1f, 1f, tooFar));
        assertThrows(IllegalArgumentException.class,
                () -> Ripples.of(1, Float.MAX_VALUE, Float.MAX_VALUE, HALF_WIDTH));
    }

    @Test
    void reachAtTheMaximumIsAccepted() {
        var ripples = Ripples.of(1, Ripples.MAX_REACH_CELLS - 1f, 1f, 1f);
        assertEquals(Ripples.MAX_REACH_CELLS, ripples.getReachCells());
    }
}
