package com.github.halfdeadgames.asciigdx.zone;

/** Cycle position and waveform helpers shared by the built-in effects. */
final class CycleMath {

    private static final float TWO_PI = (float) (Math.PI * 2.0);

    private CycleMath() {
    }

    /**
     * Position in cycles: time divided by the period, minus the cell's phase. A larger phase delays
     * the cell, so a phase that grows with distance sends the pattern outward over time. The time
     * part is wrapped to 0..1 in double precision first, so the result stays precise after hours of
     * play. The phase is not wrapped, so the result can fall outside 0..1.
     */
    static float position(float timeSeconds, float periodSeconds, Phase phase, ZoneCell where) {
        double cycles = (double) timeSeconds / periodSeconds;
        cycles -= Math.floor(cycles);
        return (float) cycles - phase.cyclesAt(where);
    }

    /** Sine of a full turn per cycle, from -1 to 1. */
    static float sine(float cyclePosition) {
        return (float) Math.sin(TWO_PI * cyclePosition);
    }

    /** Absolute sine of half a turn per cycle: one arch from 0 up to 1 and back per cycle. */
    static float arch(float cyclePosition) {
        return Math.abs((float) Math.sin(TWO_PI * 0.5f * cyclePosition));
    }

    /** Sine rescaled to 0 to 1. */
    static float sineUnit(float cyclePosition) {
        return 0.5f + 0.5f * sine(cyclePosition);
    }
}
