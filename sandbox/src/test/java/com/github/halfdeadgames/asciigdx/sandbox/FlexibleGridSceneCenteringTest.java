package com.github.halfdeadgames.asciigdx.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.Random;
import org.junit.jupiter.api.Test;

class FlexibleGridSceneCenteringTest {
    private static final int MARKER_WIDTH = 12;
    private static final int MARKER_HEIGHT = 5;
    private static final int MIN_COLUMNS = 14;
    private static final int MIN_ROWS = 7;

    private static int glyphAt(int column, int row) {
        return 'A' + row * MARKER_WIDTH + column;
    }

    private static int colorAt(int column, int row) {
        return 0x01000000 + (row * MARKER_WIDTH + column) * 0x100 + 0xFF;
    }

    private static void writeMarker(TerminalGrid grid, int left, int top) {
        for (int row = 0; row < MARKER_HEIGHT; row++) {
            for (int column = 0; column < MARKER_WIDTH; column++) {
                grid.set(left + column, top + row, glyphAt(column, row), colorAt(column, row), colorAt(row, column));
            }
        }
    }

    private static void assertMarkerIntact(TerminalGrid grid, int left, int top) {
        for (int row = 0; row < MARKER_HEIGHT; row++) {
            for (int column = 0; column < MARKER_WIDTH; column++) {
                assertEquals(glyphAt(column, row), grid.getGlyph(left + column, top + row));
                assertEquals(colorAt(column, row), grid.getForeground(left + column, top + row));
                assertEquals(colorAt(row, column), grid.getBackground(left + column, top + row));
            }
        }
    }

    private static int[] resizeTo(TerminalGrid grid, int columns, int rows, int left, int top) {
        int shiftX = FlexibleGridScene.centeringShift(left, MARKER_WIDTH, columns);
        int shiftY = FlexibleGridScene.centeringShift(top, MARKER_HEIGHT, rows);
        grid.resize(columns, rows, shiftX, shiftY);
        return new int[] {left + shiftX, top + shiftY};
    }

    private static void step(TerminalGrid grid, int columns, int rows, int[] position) {
        int[] next = resizeTo(grid, columns, rows, position[0], position[1]);
        position[0] = next[0];
        position[1] = next[1];
        assertEquals(columns, grid.getWidth());
        assertEquals(rows, grid.getHeight());
        assertEquals((columns - MARKER_WIDTH) / 2, position[0]);
        assertEquals((rows - MARKER_HEIGHT) / 2, position[1]);
        assertMarkerIntact(grid, position[0], position[1]);
    }

    @Test
    void markerStaysCenteredAndIntactThroughSeededResizes() {
        TerminalGrid grid = new TerminalGrid(40, 30);
        int[] position = {(40 - MARKER_WIDTH) / 2, (30 - MARKER_HEIGHT) / 2};
        writeMarker(grid, position[0], position[1]);

        int columns = 40;
        int rows = 30;
        for (int i = 0; i < 20; i++) {
            columns++;
            step(grid, columns, rows, position);
        }
        for (int i = 0; i < 20; i++) {
            columns--;
            rows++;
            step(grid, columns, rows, position);
        }
        for (int i = 0; i < 20; i++) {
            columns--;
            rows--;
            step(grid, columns, rows, position);
        }

        Random random = new Random(12345L);
        for (int i = 0; i < 300; i++) {
            if (random.nextInt(4) == 0) {
                columns = MIN_COLUMNS + random.nextInt(200);
                rows = MIN_ROWS + random.nextInt(120);
            } else {
                columns = Math.max(MIN_COLUMNS, columns + random.nextInt(7) - 3);
                rows = Math.max(MIN_ROWS, rows + random.nextInt(7) - 3);
            }
            step(grid, columns, rows, position);
        }
    }

    @Test
    void halvedSizeChangeFormulaDriftsOnSingleColumnGrowth() {
        TerminalGrid grid = new TerminalGrid(40, 30);
        int left = (40 - MARKER_WIDTH) / 2;
        writeMarker(grid, left, 12);

        int columns = 40;
        for (int i = 0; i < 19; i++) {
            int oldColumns = columns;
            columns++;
            grid.resize(columns, 30, (columns - oldColumns) / 2, 0);
        }

        assertEquals(59, grid.getWidth());
        assertEquals(glyphAt(0, 0), grid.getGlyph(left, 12));
    }
}
