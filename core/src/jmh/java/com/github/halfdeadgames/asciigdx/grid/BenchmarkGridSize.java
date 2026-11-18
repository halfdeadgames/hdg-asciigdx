package com.github.halfdeadgames.asciigdx.grid;

/**
 * Benchmark grid sizes, written as the column count, an "x", and the row count, such as "80x45".
 * Counting 96 bytes of vertices and 12 bytes of grid arrays per cell, SMALL needs about 0.4 MB,
 * which fits in L2 cache only on CPUs with 1 MB or more per core. The largest, a 4K screen at
 * 8-pixel cells, needs about 14 MB, more than a 12 MB L3 cache.
 */
public final class BenchmarkGridSize {

    public static final String SMALL = "80x45";
    public static final String STRESS_SCENE = "240x135";
    public static final String FOUR_K_AT_8_PIXELS = "480x270";

    private BenchmarkGridSize() {
    }

    public static int columnsOf(String gridSize) {
        return Integer.parseInt(gridSize.substring(0, gridSize.indexOf('x')));
    }

    public static int rowsOf(String gridSize) {
        return Integer.parseInt(gridSize.substring(gridSize.indexOf('x') + 1));
    }
}
