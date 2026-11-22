package com.github.halfdeadgames.asciigdx.skindesigner;

import java.util.Arrays;

/** Median, extremes, and relative spread of a set of timing samples in milliseconds. */
record PerfSummary(float medianMs, float minMs, float maxMs, long spreadPercent) {

    /**
     * Summarizes the samples without modifying the given array.
     *
     * @throws IllegalArgumentException if the array is null or empty
     */
    static PerfSummary of(float[] samplesMs) {
        if (samplesMs == null || samplesMs.length == 0) {
            throw new IllegalArgumentException("samplesMs must not be null or empty");
        }
        float[] sorted = samplesMs.clone();
        Arrays.sort(sorted);
        float min = sorted[0];
        float max = sorted[sorted.length - 1];
        float median = medianOfSorted(sorted);
        long spreadPercent = median > 0f ? Math.round((max - min) / median * 100f) : 0L;
        return new PerfSummary(median, min, max, spreadPercent);
    }

    private static float medianOfSorted(float[] sorted) {
        int middle = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2f;
    }
}
