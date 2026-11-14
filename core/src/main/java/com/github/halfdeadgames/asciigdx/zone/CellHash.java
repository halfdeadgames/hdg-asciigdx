package com.github.halfdeadgames.asciigdx.zone;

/** Allocation-free integer hashing for stable pseudo-random values. */
final class CellHash {

    private static final int MIX_A = 0x85EBCA6B;
    private static final int MIX_B = 0xC2B2AE35;
    private static final int PRIME_COLUMN = 0x9E3779B1;
    private static final int PRIME_ROW = 0x85EBCA77;
    private static final int PRIME_STEP = 0xC2B2AE3D;
    private static final int UNIT_BITS = 24;
    private static final float UNIT_SCALE = 1f / (1 << UNIT_BITS);

    private CellHash() {
    }

    static int hash(int column, int row, int step, int seed) {
        int h = seed;
        h = (h ^ column) * PRIME_COLUMN;
        h = (h ^ row) * PRIME_ROW;
        h = (h ^ step) * PRIME_STEP;
        return finalMix(h);
    }

    /** Maps a hash to a float from 0 (inclusive) to 1 (exclusive). */
    static float toUnit(int hash) {
        return (hash >>> (Integer.SIZE - UNIT_BITS)) * UNIT_SCALE;
    }

    private static int finalMix(int value) {
        int h = value;
        h ^= h >>> 16;
        h *= MIX_A;
        h ^= h >>> 13;
        h *= MIX_B;
        h ^= h >>> 16;
        return h;
    }
}
