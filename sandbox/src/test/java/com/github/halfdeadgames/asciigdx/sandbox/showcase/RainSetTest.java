package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class RainSetTest {
    private static final float CYCLE = 1f;
    private static final int CYCLES_PER_LOOP = 12;
    private static final float PEAK = 0.6f;
    private static final float DELTA = 1e-4f;

    private static RainSet setWithPhase(float phase) {
        return new RainSet(CYCLE, CYCLES_PER_LOOP, phase, -3f, 3f, PEAK, 0.25f);
    }

    @Test
    void cycleIndexIsPeriodicOverTheLoopAndInRange() {
        float loopSeconds = CYCLE * CYCLES_PER_LOOP;
        float[] times = {0f, 0.001f, 0.01f, 0.3f, 4.7f, 11.9f};
        for (float phase : new float[]{0f, 1f / 3f, 2f / 3f}) {
            RainSet set = setWithPhase(phase);
            for (float time : times) {
                int index = set.cycleIndexAt(time);
                assertEquals(index, set.cycleIndexAt(time + loopSeconds));
                assertTrue(index >= 0 && index < CYCLES_PER_LOOP);
            }
        }
    }

    @Test
    void alphaIsZeroAtCycleBoundaries() {
        RainSet set = setWithPhase(0f);
        assertEquals(0f, set.alphaAt(0f), DELTA);
        assertEquals(0f, set.alphaAt(1f), DELTA);
        assertEquals(0f, set.alphaAt(2f), DELTA);
    }

    @Test
    void alphaPeaksInTheMiddleAndRampsLinearly() {
        RainSet set = setWithPhase(0f);
        assertEquals(PEAK, set.alphaAt(0.5f), DELTA);
        assertEquals(PEAK / 2f, set.alphaAt(0.125f), DELTA);
        assertEquals(PEAK / 2f, set.alphaAt(0.875f), DELTA);
        assertEquals(PEAK, set.alphaAt(0.25f), DELTA);
    }

    @Test
    void staggeredSetReachesPeakLater() {
        RainSet first = setWithPhase(0f);
        RainSet second = setWithPhase(1f / 3f);
        assertEquals(PEAK, first.alphaAt(0.25f), DELTA);
        assertEquals(PEAK, second.alphaAt(0.25f + 1f / 3f), DELTA);
        assertTrue(second.alphaAt(0.25f) < PEAK);
        assertEquals(first.alphaAt(0.1f), second.alphaAt(0.1f + 1f / 3f), DELTA);
    }

    @Test
    void slideProgressStartsAtZeroAndApproachesOne() {
        RainSet set = setWithPhase(0f);
        assertEquals(0f, set.slideProgressAt(0f), DELTA);
        assertEquals(0.5f, set.slideProgressAt(0.5f), DELTA);
        assertTrue(set.slideProgressAt(0.9999f) > 0.99f);
        assertEquals(-3f * 0.5f, set.slideColumnsAt(0.5f), DELTA);
        assertEquals(3f * 0.5f, set.slideRowsAt(0.5f), DELTA);
    }

    @Test
    void cycleIndexChangesAtBoundaries() {
        RainSet set = setWithPhase(0f);
        assertEquals(0, set.cycleIndexAt(0.99f));
        assertEquals(1, set.cycleIndexAt(1.01f));
    }

    @Test
    void cycleIndexHonorsPhaseShiftAndNegativeLocalTime() {
        RainSet set = setWithPhase(1f / 3f);
        assertEquals(CYCLES_PER_LOOP - 1, set.cycleIndexAt(0f));
        assertEquals(CYCLES_PER_LOOP - 1, set.cycleIndexAt(0.3f));
        assertEquals(0, set.cycleIndexAt(0.34f));
        assertEquals(0, set.cycleIndexAt(1.3f));
        assertEquals(1, set.cycleIndexAt(1.34f));
        assertEquals(2f / 3f, set.slideProgressAt(0f), DELTA);
        assertEquals(0f, set.alphaAt(1f / 3f), DELTA);
    }

    @Test
    void sameSeedGivesSamePositionsAndOtherCycleDiffers() {
        RainSet set = setWithPhase(0f);
        Random random = new Random();
        int[] columnsA = new int[12];
        int[] rowsA = new int[12];
        int[] columnsB = new int[12];
        int[] rowsB = new int[12];
        set.fillDropPositions(random, RainSet.seedFor(1, 4), 28, 17, columnsA, rowsA);
        set.fillDropPositions(random, RainSet.seedFor(1, 4), 28, 17, columnsB, rowsB);
        assertArrayEquals(columnsA, columnsB);
        assertArrayEquals(rowsA, rowsB);
        set.fillDropPositions(random, RainSet.seedFor(1, 5), 28, 17, columnsB, rowsB);
        assertFalse(java.util.Arrays.equals(columnsA, columnsB)
                && java.util.Arrays.equals(rowsA, rowsB));
    }

    @Test
    void positionsStayInsideTheGrid() {
        RainSet set = setWithPhase(0f);
        Random random = new Random();
        int[] columns = new int[12];
        int[] rows = new int[12];
        for (int cycle = 0; cycle < 50; cycle++) {
            set.fillDropPositions(random, RainSet.seedFor(0, cycle), 28, 17, columns, rows);
            for (int drop = 0; drop < columns.length; drop++) {
                assertTrue(columns[drop] >= 0 && columns[drop] < 28);
                assertTrue(rows[drop] >= 0 && rows[drop] < 17);
            }
        }
    }
}
