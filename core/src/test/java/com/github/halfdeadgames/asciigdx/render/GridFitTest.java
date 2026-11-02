package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

class GridFitTest {

    private static final float EPSILON = 1e-4f;

    @Test
    void fitsSixteenHundredByNineHundred() {
        GridFit fit = GridFit.calculate(1600, 900, 8, 8, 80, 45);
        assertEquals(2, fit.getScale());
        assertEquals(100, fit.getColumns());
        assertEquals(56, fit.getRows());
        assertEquals(0, fit.getLeftoverX());
        assertEquals(4, fit.getLeftoverY());
        assertTrue(fit.meetsMinimum());
        assertFalse(fit.isEmpty());
    }

    @Test
    void fitsFullHdWithNoLeftover() {
        GridFit fit = GridFit.calculate(1920, 1080, 8, 8, 80, 45);
        assertEquals(3, fit.getScale());
        assertEquals(80, fit.getColumns());
        assertEquals(45, fit.getRows());
        assertEquals(0, fit.getLeftoverX());
        assertEquals(0, fit.getLeftoverY());
    }

    @Test
    void heightCanLimitScale() {
        GridFit fit = GridFit.calculate(2000, 400, 8, 8, 10, 10);
        assertEquals(5, fit.getScale());
        assertEquals(50, fit.getColumns());
        assertEquals(10, fit.getRows());
    }

    @Test
    void nonSquareCells() {
        GridFit fit = GridFit.calculate(1000, 700, 8, 16, 40, 20);
        assertEquals(2, fit.getScale());
        assertEquals(62, fit.getColumns());
        assertEquals(21, fit.getRows());
        assertEquals(8, fit.getLeftoverX());
        assertEquals(28, fit.getLeftoverY());
        assertEquals(62 * 8, fit.getWorldWidth());
        assertEquals(21 * 16, fit.getWorldHeight());
        assertEquals(8, fit.getCellWidth());
        assertEquals(16, fit.getCellHeight());
    }

    @Test
    void tooSmallWindowStaysAtOneTimes() {
        GridFit fit = GridFit.calculate(500, 300, 8, 8, 80, 45);
        assertEquals(1, fit.getScale());
        assertEquals(62, fit.getColumns());
        assertEquals(37, fit.getRows());
        assertEquals(4, fit.getLeftoverX());
        assertEquals(4, fit.getLeftoverY());
        assertFalse(fit.meetsMinimum());
        assertFalse(fit.isEmpty());
    }

    @Test
    void windowSmallerThanOneCellIsEmpty() {
        GridFit fit = GridFit.calculate(5, 5, 8, 8, 1, 1);
        assertTrue(fit.isEmpty());
        assertEquals(1, fit.getScale());
        assertEquals(0, fit.getColumns());
        assertEquals(0, fit.getRows());
        assertFalse(fit.meetsMinimum());
    }

    @Test
    void windowNarrowerThanOneCellIsEmpty() {
        GridFit fit = GridFit.calculate(5, 500, 8, 8, 1, 1);
        assertTrue(fit.isEmpty());
        assertEquals(1, fit.getScale());
    }

    @Test
    void zeroAndNegativeWindowAreEmpty() {
        int[][] windows = {{0, 0}, {0, 100}, {100, 0}, {-5, 100}, {100, -5}, {-1, -1}};
        for (int[] window : windows) {
            GridFit fit = GridFit.calculate(window[0], window[1], 8, 8, 10, 10);
            assertTrue(fit.isEmpty());
            assertFalse(fit.meetsMinimum());
            assertEquals(0, fit.getScale());
            assertEquals(0, fit.getColumns());
            assertEquals(0, fit.getRows());
            assertEquals(0, fit.getLeftoverX());
            assertEquals(0, fit.getLeftoverY());
        }
    }

    @Test
    void invalidCellSizeAndMinimumsThrow() {
        assertThrows(IllegalArgumentException.class, () -> GridFit.calculate(100, 100, 0, 8, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> GridFit.calculate(100, 100, 8, -1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> GridFit.calculate(100, 100, 8, 8, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> GridFit.calculate(100, 100, 8, 8, 1, -3));
    }

    @Test
    void messageContainsValues() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> GridFit.calculate(100, 100, 7, 0, 3, 4));
        assertTrue(error.getMessage().contains("7"));
        assertTrue(error.getMessage().contains("0"));
        assertTrue(error.getMessage().contains("3"));
        assertTrue(error.getMessage().contains("4"));
    }

    @Test
    void hugeMinimumsDoNotOverflow() {
        GridFit fit = GridFit.calculate(Integer.MAX_VALUE, Integer.MAX_VALUE, 65536, 65536,
                Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(1, fit.getScale());
        assertEquals(Integer.MAX_VALUE / 65536, fit.getColumns());
        assertFalse(fit.meetsMinimum());
    }

    @Test
    void hugeWindowWithBigCellsDoesNotOverflow() {
        GridFit fit = GridFit.calculate(Integer.MAX_VALUE, Integer.MAX_VALUE, 1 << 20, 1 << 20, 1, 1);
        assertEquals(2047, fit.getScale());
        assertEquals(1, fit.getColumns());
        assertTrue(fit.getLeftoverX() >= 0);
    }

    @Test
    void leftoverPixelsAddUpForManyWindows() {
        int[][] cells = {{8, 8}, {8, 16}, {3, 5}, {1, 1}};
        int[][] minimums = {{1, 1}, {10, 5}, {40, 20}, {7, 3}};
        for (int[] cell : cells) {
            for (int[] minimum : minimums) {
                for (int width = 1; width <= 400; width++) {
                    for (int height = 1; height <= 300; height++) {
                        GridFit fit = GridFit.calculate(width, height, cell[0], cell[1],
                                minimum[0], minimum[1]);
                        int scaledWidth = cell[0] * fit.getScale();
                        int scaledHeight = cell[1] * fit.getScale();
                        assertEquals(width, fit.getColumns() * scaledWidth + fit.getLeftoverX());
                        assertEquals(height, fit.getRows() * scaledHeight + fit.getLeftoverY());
                        assertTrue(fit.getLeftoverX() < scaledWidth);
                        assertTrue(fit.getLeftoverY() < scaledHeight);
                    }
                }
            }
        }
    }

    @Test
    void thirtyOneSquareWindowPicksScaleThree() {
        GridFit fit = GridFit.calculate(31, 31, 8, 8, 1, 1);
        assertEquals(3, fit.getScale());
        assertEquals(1, fit.getColumns());
        assertEquals(1, fit.getRows());
        assertEquals(7, fit.getLeftoverX());
    }

    @Test
    void exactScalingMatchesIntegerScalingAcrossManyWindows() {
        int[][] cells = {{8, 8}, {8, 16}, {3, 5}};
        int[][] minimums = {{1, 1}, {2, 2}, {10, 5}, {40, 20}};
        for (int[] cell : cells) {
            for (int[] minimum : minimums) {
                for (int width = 1; width <= 400; width++) {
                    for (int height = 1; height <= 300; height++) {
                        GridFit fit = GridFit.calculate(width, height, cell[0], cell[1],
                                minimum[0], minimum[1]);
                        if (fit.isEmpty()) {
                            continue;
                        }
                        float worldWidth = fit.getWorldWidth();
                        float worldHeight = fit.getWorldHeight();
                        Vector2 exact = new ExactScaling(fit.getScale())
                                .apply(worldWidth, worldHeight, width, height);
                        assertEquals(worldWidth * fit.getScale(), exact.x, EPSILON);
                        assertEquals(worldHeight * fit.getScale(), exact.y, EPSILON);
                        float exactX = exact.x;
                        float exactY = exact.y;
                        Vector2 integer = IntegerScaling.INSTANCE.apply(worldWidth, worldHeight, width, height);
                        assertEquals(exactX, integer.x, EPSILON);
                        assertEquals(exactY, integer.y, EPSILON);
                    }
                }
            }
        }
    }
}
