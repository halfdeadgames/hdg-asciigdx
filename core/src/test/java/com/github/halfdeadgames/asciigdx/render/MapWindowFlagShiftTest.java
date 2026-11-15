package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

/** Checks the needs-fill flags that move with scrolled content against a naive copy. */
class MapWindowFlagShiftTest {

    private static final int COLUMNS = 6;
    private static final int ROWS = 4;
    private static final float CELL_WIDTH = 8f;
    private static final float CELL_HEIGHT = 12f;

    /** Camera moves in cells: {columns, rows}. */
    private static final int[][] CAMERA_MOVES = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
        {1, 1}, {-1, -1}, {1, -1}, {-1, 1},
        {COLUMNS - 1, 0}, {-(COLUMNS - 1), 0}, {0, ROWS - 1}, {0, -(ROWS - 1)},
        {COLUMNS - 1, ROWS - 1}, {-(COLUMNS - 1), -(ROWS - 1)},
        {COLUMNS, 0}, {-COLUMNS, 0}, {0, ROWS}, {0, -ROWS},
        {COLUMNS + 3, 1}, {-(COLUMNS + 3), -1}, {2, ROWS + 5}, {-2, -(ROWS + 5)},
        {40, 40}, {-40, -40},
    };

    private final LayerStack stack = HeadlessLayers.createStack();
    private final Layer layer = HeadlessLayers.add(stack, new TerminalGrid(COLUMNS, ROWS));

    @Test
    void flagsMoveLikeANaiveCopyForEveryShift() {
        for (int[] move : CAMERA_MOVES) {
            MapWindow window = windowWithDistinctivePattern();
            boolean[] before = readFlags(window);
            assertPatternIsDistinctive(before);

            stack.setCamera((3 + move[0]) * CELL_WIDTH, (1 + move[1]) * CELL_HEIGHT);

            boolean[] expected = naiveShift(before, -move[0], -move[1]);
            assertArrayEquals(expected, readFlags(window), "move " + move[0] + ", " + move[1]);
        }
    }

    /** Leaves flags on the bottom row and the right three columns, with the camera at cell (3, 1). */
    private MapWindow windowWithDistinctivePattern() {
        stack.setCamera(0f, 0f);
        layer.enableMapWindow();
        MapWindow window = layer.getMapWindow();
        window.markAllFilled();
        stack.setCamera(CELL_WIDTH, 0f);
        stack.setCamera(CELL_WIDTH, CELL_HEIGHT);
        stack.setCamera(3 * CELL_WIDTH, CELL_HEIGHT);
        return window;
    }

    private static void assertPatternIsDistinctive(boolean[] flags) {
        boolean anyTrue = false;
        boolean anyFalse = false;
        for (boolean flag : flags) {
            anyTrue |= flag;
            anyFalse |= !flag;
        }
        assertTrue(anyTrue);
        assertTrue(anyFalse);
        assertFalse(flags[0]);
        assertTrue(flags[COLUMNS * ROWS - 1]);
    }

    private static boolean[] naiveShift(boolean[] source, int columns, int rows) {
        boolean[] result = new boolean[source.length];
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int sourceRow = row - rows;
                int sourceColumn = column - columns;
                boolean exists = sourceRow >= 0 && sourceRow < ROWS
                        && sourceColumn >= 0 && sourceColumn < COLUMNS;
                result[row * COLUMNS + column] = !exists || source[sourceRow * COLUMNS + sourceColumn];
            }
        }
        return result;
    }

    private static boolean[] readFlags(MapWindow window) {
        boolean[] flags = new boolean[COLUMNS * ROWS];
        window.forEachCellToFill((mapColumn, mapRow) ->
                flags[window.toGridRow(mapRow) * COLUMNS + window.toGridColumn(mapColumn)] = true);
        return flags;
    }
}
