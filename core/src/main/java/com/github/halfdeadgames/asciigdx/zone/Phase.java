package com.github.halfdeadgames.asciigdx.zone;

/**
 * Gives each cell of a zone an offset into its cycle, in cycles (1 is one full cycle). The built-in
 * effects subtract it from the cell's cycle position (time divided by the period), so a bigger
 * phase means the cell lags behind. Immutable: create one when the effect is built and share it
 * freely. Reading it allocates nothing.
 */
public final class Phase {

    private static final Phase SYNC = new Phase(where -> 0f);

    private final Source source;

    private Phase(Source source) {
        this.source = source;
    }

    /** Computes the phase of one cell. */
    private interface Source {
        float cyclesAt(ZoneCell where);
    }

    /** Every cell is at the same point of its cycle. */
    public static Phase sync() {
        return SYNC;
    }

    /**
     * Phase grows with the cell's column in the zone, so the pattern travels toward higher columns.
     * Suits waves that run along a line of text.
     *
     * @param cellsPerCycle columns that make up one full cycle; finite and greater than 0
     * @throws IllegalArgumentException if {@code cellsPerCycle} is not finite or not above 0
     */
    public static Phase alongColumns(float cellsPerCycle) {
        EffectArguments.requirePositive("cellsPerCycle", cellsPerCycle);
        return new Phase(where -> where.getZoneColumn() / cellsPerCycle);
    }

    /**
     * Phase grows with the distance from the shape's center, so rings spread outward over time.
     *
     * @param cellsPerCycle distance between rings, in column widths (see
     *     {@link ZoneCell#getDistance()}); finite and greater than 0
     * @throws IllegalArgumentException if {@code cellsPerCycle} is not finite or not above 0
     */
    public static Phase byDistance(float cellsPerCycle) {
        EffectArguments.requirePositive("cellsPerCycle", cellsPerCycle);
        return new Phase(where -> where.getDistance() / cellsPerCycle);
    }

    /**
     * Gives each grid cell its own stable pseudo-random phase from 0 (inclusive) to 1 (exclusive).
     * The same seed, column, and row always give the same value. The value depends on the grid
     * position, not the position inside the zone.
     *
     * @param seed any int; different seeds give different patterns
     */
    public static Phase scattered(int seed) {
        return new Phase(where -> CellHash.toUnit(
                CellHash.hash(where.getColumn(), where.getRow(), 0, seed)));
    }

    /** Returns the cell's phase in cycles. */
    public float cyclesAt(ZoneCell where) {
        return source.cyclesAt(where);
    }
}
