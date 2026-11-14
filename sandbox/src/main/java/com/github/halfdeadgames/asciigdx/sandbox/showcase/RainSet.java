package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import java.util.Random;

/**
 * Timing and drop placement for one set of rain drops. The set repeats a cycle of fade in, slide, and fade out,
 * shifted by a phase so several sets can be staggered.
 */
public final class RainSet {
    private final double cycleSeconds;
    private final double phaseFraction;
    private final float slideColumns;
    private final float slideRows;
    private final float peakAlpha;
    private final double rampFraction;
    private final int cyclesPerLoop;

    /**
     * @param cyclesPerLoop whole cycles in one loop; cycleSeconds times this is the loop length
     * @param phaseFraction share of a cycle this set starts after time 0
     * @param rampFraction share of a cycle spent fading in, and the same share fading out; at most 0.5
     */
    public RainSet(float cycleSeconds, int cyclesPerLoop, float phaseFraction, float slideColumns, float slideRows, float peakAlpha, float rampFraction) {
        if (cyclesPerLoop < 1) {
            throw new IllegalArgumentException("cyclesPerLoop must be >= 1: " + cyclesPerLoop);
        }
        this.cyclesPerLoop = cyclesPerLoop;
        if (!(cycleSeconds > 0f)) {
            throw new IllegalArgumentException("cycleSeconds must be > 0: " + cycleSeconds);
        }
        if (!(rampFraction > 0f && rampFraction <= 0.5f)) {
            throw new IllegalArgumentException("rampFraction must be in (0, 0.5]: " + rampFraction);
        }
        this.cycleSeconds = cycleSeconds;
        this.phaseFraction = phaseFraction;
        this.slideColumns = slideColumns;
        this.slideRows = slideRows;
        this.peakAlpha = peakAlpha;
        this.rampFraction = rampFraction;
    }

    /** Seed for a set's drop layout, so the same set and cycle always give the same drops. */
    public static long seedFor(int setIndex, int cycleIndex) {
        return setIndex * 1_000_003L + cycleIndex;
    }

    /** Index in [0, cyclesPerLoop), repeating every loop so a phased set never reseeds while visible at the wrap. */
    public int cycleIndexAt(float timeSeconds) {
        return (int) Math.floorMod((long) Math.floor(cyclePositionAt(timeSeconds)), (long) cyclesPerLoop);
    }

    public float alphaAt(float timeSeconds) {
        double fraction = slideProgressAt(timeSeconds);
        if (fraction < rampFraction) {
            return (float) (peakAlpha * fraction / rampFraction);
        }
        if (fraction > 1.0 - rampFraction) {
            return (float) (peakAlpha * (1.0 - fraction) / rampFraction);
        }
        return peakAlpha;
    }

    /** How far through the current cycle the set is, 0 at its start and approaching 1 at its end. */
    public float slideProgressAt(float timeSeconds) {
        double position = cyclePositionAt(timeSeconds);
        return (float) (position - Math.floor(position));
    }

    /** Group shift in columns at this time, 0 at cycle start; a negative slide moves left. */
    public float slideColumnsAt(float timeSeconds) {
        return slideColumns * slideProgressAt(timeSeconds);
    }

    /** Group shift in rows at this time, 0 at cycle start; a positive slide moves down. */
    public float slideRowsAt(float timeSeconds) {
        return slideRows * slideProgressAt(timeSeconds);
    }

    /** Reseeds the random, then writes one drop per slot of the output arrays, both of the same length. */
    public void fillDropPositions(Random random, long seed, int gridColumns, int gridRows, int[] dropColumns, int[] dropRows) {
        random.setSeed(seed);
        for (int drop = 0; drop < dropColumns.length; drop++) {
            dropColumns[drop] = random.nextInt(gridColumns);
            dropRows[drop] = random.nextInt(gridRows);
        }
    }

    private double cyclePositionAt(float timeSeconds) {
        return timeSeconds / cycleSeconds - phaseFraction;
    }
}
