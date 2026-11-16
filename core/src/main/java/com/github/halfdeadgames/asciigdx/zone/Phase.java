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
        float getCyclesAt(ZoneCell where);
    }

    /**
     * Returns a phase that puts every cell at the same point of its cycle.
     *
     * @return the shared phase that is 0 for every cell
     */
    public static Phase sync() {
        return SYNC;
    }

    /**
     * Returns a phase that grows with the cell's column in the zone, so the pattern travels toward
     * higher columns. Suits waves that run along a line of text.
     *
     * @param cellsPerCycle columns that make up one full cycle; finite and greater than 0
     * @return a phase equal to the cell's zone column divided by {@code cellsPerCycle}
     * @throws IllegalArgumentException if {@code cellsPerCycle} is not finite or not above 0
     */
    public static Phase alongColumns(float cellsPerCycle) {
        EffectArguments.requirePositive("cellsPerCycle", cellsPerCycle);
        return new Phase(where -> where.getZoneColumn() / cellsPerCycle);
    }

    /**
     * Returns a phase that grows with the distance from the shape's center, so rings spread
     * outward over time.
     *
     * @param cellsPerCycle distance between rings, in column widths (see
     *     {@link ZoneCell#getDistance()}); finite and greater than 0
     * @return a phase equal to the cell's distance divided by {@code cellsPerCycle}
     * @throws IllegalArgumentException if {@code cellsPerCycle} is not finite or not above 0
     */
    public static Phase byDistance(float cellsPerCycle) {
        EffectArguments.requirePositive("cellsPerCycle", cellsPerCycle);
        return new Phase(where -> where.getDistance() / cellsPerCycle);
    }

    /**
     * Returns a phase that gives each grid cell its own stable pseudo-random value from 0
     * (inclusive) to 1 (exclusive). The same seed, column, and row always give the same value. The
     * value depends on the grid position, not the position inside the zone.
     *
     * @param seed any int; different seeds give different patterns
     * @return a phase that depends only on the seed and the cell's grid column and row
     */
    public static Phase scattered(int seed) {
        return new Phase(where -> CellHash.toUnit(
                CellHash.hash(where.getColumn(), where.getRow(), 0, seed)));
    }

    /** Returns the cell's phase in cycles. */
    public float getCyclesAt(ZoneCell where) {
        return source.getCyclesAt(where);
    }
}
