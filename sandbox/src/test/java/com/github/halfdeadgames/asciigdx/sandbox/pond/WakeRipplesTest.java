package com.github.halfdeadgames.asciigdx.sandbox.pond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WakeRipplesTest {

    private static final float TOLERANCE = 1e-4f;

    @Test
    void liftIsZeroBeforeStartAndAtOrAfterLife() {
        var wake = new WakeRipples();
        wake.start(5, 5, 10f);
        assertEquals(0f, wake.liftAt(5, 5, 9.9f));
        assertEquals(0f, wake.liftAt(5, 5, 10f + WakeRipples.LIFE_SECONDS));
        assertEquals(0f, wake.liftAt(5, 5, 20f));
    }

    @Test
    void liftPeaksAtStartCellAtAgeZero() {
        var wake = new WakeRipples();
        wake.start(5, 5, 10f);
        assertEquals(1f, wake.liftAt(5, 5, 10f), TOLERANCE);
    }

    @Test
    void liftAtDistancePeaksWhenRadiusReachesIt() {
        var wake = new WakeRipples();
        wake.start(5, 5, 0f);
        var arrival = 2f / WakeRipples.SPEED_CELLS_PER_SECOND;
        var expected = 1f - arrival / WakeRipples.LIFE_SECONDS;
        assertEquals(expected, wake.liftAt(7, 5, arrival), TOLERANCE);
        assertTrue(wake.liftAt(7, 5, arrival - 0.1f) < expected);
        assertTrue(wake.liftAt(7, 5, arrival + 0.1f) < expected);
    }

    @Test
    void distanceUsesBothAxes() {
        var wake = new WakeRipples();
        wake.start(0, 0, 0f);
        var diagonal = (float) Math.sqrt(2.0);
        var arrival = diagonal / WakeRipples.SPEED_CELLS_PER_SECOND;
        assertEquals(1f - arrival, wake.liftAt(1, 1, arrival), TOLERANCE);
    }

    @Test
    void liftFadesWithAge() {
        var wake = new WakeRipples();
        wake.start(0, 0, 0f);
        var early = wake.liftAt(1, 0, 1f / WakeRipples.SPEED_CELLS_PER_SECOND);
        var late = wake.liftAt(2, 0, 2f / WakeRipples.SPEED_CELLS_PER_SECOND);
        assertTrue(early > late);
        assertTrue(late > 0f);
    }

    @Test
    void overlappingRingsGiveMaxNotSum() {
        var wake = new WakeRipples();
        wake.start(5, 5, 0f);
        wake.start(5, 5, 0.1f);
        // The first ring alone lifts about 0.62 here and the second 1.0, so a sum would exceed 1.
        assertEquals(1f, wake.liftAt(5, 5, 0.1f), TOLERANCE);
    }

    @Test
    void fifthStartDropsTheOldestRing() {
        var wake = new WakeRipples();
        for (var ring = 0; ring < WakeRipples.MAX_RINGS; ring++) {
            wake.start(ring * 100, 0, 0f);
        }
        wake.start(1000, 0, 0f);
        assertEquals(0f, wake.liftAt(0, 0, 0f));
        assertEquals(1f, wake.liftAt(1000, 0, 0f), TOLERANCE);
        assertEquals(1f, wake.liftAt(100, 0, 0f), TOLERANCE);
    }

    @Test
    void isActiveAtCoversTheLifeOfAnyRing() {
        var wake = new WakeRipples();
        assertFalse(wake.isActiveAt(0f));
        wake.start(0, 0, 2f);
        assertFalse(wake.isActiveAt(1.9f));
        assertTrue(wake.isActiveAt(2f));
        assertTrue(wake.isActiveAt(2.9f));
        assertFalse(wake.isActiveAt(2f + WakeRipples.LIFE_SECONDS));
    }

    @Test
    void boundsCoverReachOfLiveRingsAndExcludeExpiredOnes() {
        var wake = new WakeRipples();
        var reach = (int) Math.ceil(WakeRipples.REACH_CELLS);
        wake.start(100, 100, 0f);
        wake.start(10, 20, 5f);
        wake.start(30, 2, 5.5f);
        assertEquals(10 - reach, wake.activeLeft(5.6f));
        assertEquals(2 - reach, wake.activeTop(5.6f));
        assertEquals(30 + reach, wake.activeRight(5.6f));
        assertEquals(20 + reach, wake.activeBottom(5.6f));
    }

    @Test
    void boundsOfSingleRingSurroundItsCell() {
        var wake = new WakeRipples();
        var reach = (int) Math.ceil(WakeRipples.REACH_CELLS);
        wake.start(7, 9, 0f);
        assertEquals(7 - reach, wake.activeLeft(0.5f));
        assertEquals(9 - reach, wake.activeTop(0.5f));
        assertEquals(7 + reach, wake.activeRight(0.5f));
        assertEquals(9 + reach, wake.activeBottom(0.5f));
    }

    @Test
    void clearRemovesEveryRing() {
        var wake = new WakeRipples();
        wake.start(1, 1, 0f);
        wake.clear();
        assertFalse(wake.isActiveAt(0f));
        assertEquals(0f, wake.liftAt(1, 1, 0f));
    }

    @Test
    void nonFiniteTimeIsRejected() {
        var wake = new WakeRipples();
        assertThrows(IllegalArgumentException.class, () -> wake.start(0, 0, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> wake.liftAt(0, 0, Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> wake.isActiveAt(Float.NaN));
    }
}
