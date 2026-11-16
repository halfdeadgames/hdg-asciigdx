package com.github.halfdeadgames.asciigdx.sandbox;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PerfSummaryTest {
    private static final float DELTA = 1e-6f;

    @Test
    void oddCountUsesMiddleValue() {
        PerfSummary summary = PerfSummary.of(new float[]{1f, 2f, 9f});

        assertEquals(2f, summary.medianMs(), DELTA);
    }

    @Test
    void evenCountAveragesTwoMiddleValues() {
        PerfSummary summary = PerfSummary.of(new float[]{1f, 2f, 4f, 10f});

        assertEquals(3f, summary.medianMs(), DELTA);
    }

    @Test
    void singleSampleHasNoSpread() {
        PerfSummary summary = PerfSummary.of(new float[]{4f});

        assertEquals(4f, summary.medianMs(), DELTA);
        assertEquals(4f, summary.minMs(), DELTA);
        assertEquals(4f, summary.maxMs(), DELTA);
        assertEquals(0L, summary.spreadPercent());
    }

    @Test
    void minAndMaxComeFromUnsortedInput() {
        PerfSummary summary = PerfSummary.of(new float[]{5f, 1f, 9f, 3f, 7f});

        assertEquals(1f, summary.minMs(), DELTA);
        assertEquals(9f, summary.maxMs(), DELTA);
    }

    @Test
    void spreadPercentIsRoundedRelativeToMedian() {
        PerfSummary summary = PerfSummary.of(new float[]{2f, 3f, 4f});

        assertEquals(67L, summary.spreadPercent());
    }

    @Test
    void zeroMedianGivesZeroSpread() {
        PerfSummary summary = PerfSummary.of(new float[]{0f, 0f, 5f});

        assertEquals(0f, summary.medianMs(), DELTA);
        assertEquals(0L, summary.spreadPercent());
    }

    @Test
    void inputArrayIsNotModified() {
        float[] samples = {3f, 1f, 2f};

        PerfSummary.of(samples);

        assertArrayEquals(new float[]{3f, 1f, 2f}, samples, DELTA);
    }

    @Test
    void emptyArrayThrows() {
        assertThrows(IllegalArgumentException.class, () -> PerfSummary.of(new float[0]));
    }

    @Test
    void nullArrayThrows() {
        assertThrows(IllegalArgumentException.class, () -> PerfSummary.of(null));
    }
}
