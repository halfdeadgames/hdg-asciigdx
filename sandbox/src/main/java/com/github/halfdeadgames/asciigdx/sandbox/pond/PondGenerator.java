package com.github.halfdeadgames.asciigdx.sandbox.pond;

import java.util.Random;

/** Builds a seeded pond: shallow water, a few small islands, and scattered lilies. */
public final class PondGenerator {

    private static final int MIN_ISLANDS = 5;
    private static final int MAX_ISLANDS = 7;
    private static final int MIN_RADIUS = 1;
    private static final int MAX_RADIUS = 3;
    /** Keeps at least one water cell between an island and the map edge. */
    private static final int EDGE_MARGIN = MAX_RADIUS + 1;
    private static final int LILY_ODDS = 3;
    /**
     * Separate stream for colors, so picking them does not change where the main stream places
     * lilies.
     */
    private static final long COLOR_SEED_SALT = 0x5DEECE66DL;

    private PondGenerator() {
    }

    /**
     * Generates the same map for the same arguments.
     *
     * @param startColumn column of the player start; it and its neighbors stay free of land
     * @param startRow    row of the player start
     */
    public static PondMap generate(int width, int height, long seed, int startColumn,
            int startRow) {
        if (width <= 2 * EDGE_MARGIN || height <= 2 * EDGE_MARGIN) {
            throw new IllegalArgumentException(
                    "map too small for islands: " + width + "x" + height);
        }
        var random = new Random(seed);
        var map = new PondMap(width, height);
        placeIslands(map, random, startColumn, startRow);
        scatterLilies(map, random, new Random(seed ^ COLOR_SEED_SALT));
        return map;
    }

    private static void placeIslands(PondMap map, Random random, int startColumn, int startRow) {
        int islandCount = MIN_ISLANDS + random.nextInt(MAX_ISLANDS - MIN_ISLANDS + 1);
        for (int i = 0; i < islandCount; i++) {
            int centerColumn = EDGE_MARGIN + random.nextInt(map.getWidth() - 2 * EDGE_MARGIN);
            int centerRow = EDGE_MARGIN + random.nextInt(map.getHeight() - 2 * EDGE_MARGIN);
            int radiusX = MIN_RADIUS + random.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
            int radiusY = MIN_RADIUS + random.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
            fillEllipse(map, centerColumn, centerRow, radiusX, radiusY, startColumn, startRow);
        }
    }

    private static void fillEllipse(PondMap map, int centerColumn, int centerRow, int radiusX,
            int radiusY,
            int startColumn, int startRow) {
        for (int row = centerRow - radiusY; row <= centerRow + radiusY; row++) {
            for (int column = centerColumn - radiusX; column <= centerColumn + radiusX; column++) {
                if (isInsideEllipse(column - centerColumn, row - centerRow, radiusX, radiusY)
                        && !isNearStart(column, row, startColumn, startRow)) {
                    map.cell(column, row).setTerrain(Terrain.LAND);
                }
            }
        }
    }

    private static boolean isInsideEllipse(int deltaColumn, int deltaRow, int radiusX,
            int radiusY) {
        double normalizedX = deltaColumn / (radiusX + 0.5);
        double normalizedY = deltaRow / (radiusY + 0.5);
        return normalizedX * normalizedX + normalizedY * normalizedY <= 1.0;
    }

    private static boolean isNearStart(int column, int row, int startColumn, int startRow) {
        return Math.abs(column - startColumn) <= 1 && Math.abs(row - startRow) <= 1;
    }

    private static void scatterLilies(PondMap map, Random random, Random colorRandom) {
        for (int row = 0; row < map.getHeight(); row++) {
            for (int column = 0; column < map.getWidth(); column++) {
                var cell = map.cell(column, row);
                if (cell.getTerrain() == Terrain.WATER) {
                    scatterLiliesInCell(cell, random, colorRandom);
                }
            }
        }
    }

    private static void scatterLiliesInCell(PondCell cell, Random random, Random colorRandom) {
        for (LilySlot slot : LilySlot.values()) {
            if (random.nextInt(LILY_ODDS) == 0) {
                cell.putLily(slot, newLily(colorRandom));
            }
        }
    }

    private static Lily newLily(Random colorRandom) {
        var lily = new Lily();
        lily.setColor(LilyPalette.colorAt(colorRandom.nextInt(LilyPalette.size())));
        return lily;
    }
}
