package com.github.halfdeadgames.asciigdx.draw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class GridDrawingTest {

    private static final int RED = 0xFF0000FF;
    private static final int BLUE = 0x0000FFFF;

    @Test
    void textWritesGlyphsAndColors() {
        TerminalGrid grid = new TerminalGrid(6, 2);

        GridDrawing.text(grid, 1, 1, "abc", RED, BLUE);

        assertEquals('a', grid.getCodePoint(1, 1));
        assertEquals('b', grid.getCodePoint(2, 1));
        assertEquals('c', grid.getCodePoint(3, 1));
        assertEquals(RED, grid.getForeground(3, 1));
        assertEquals(BLUE, grid.getBackground(3, 1));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(0, 1));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(4, 1));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(1, 0));
    }

    @Test
    void textUsesOneCellPerSupplementaryCodePoint() {
        TerminalGrid grid = new TerminalGrid(4, 1);
        int dagger = 0x1F5E1;

        GridDrawing.text(grid, 0, 0, new StringBuilder().appendCodePoint(dagger).append('x'), RED, BLUE);

        assertEquals(dagger, grid.getCodePoint(0, 0));
        assertEquals('x', grid.getCodePoint(1, 0));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(2, 0));
    }

    @Test
    void textIsClippedAtRightEdge() {
        TerminalGrid grid = new TerminalGrid(3, 1);

        GridDrawing.text(grid, 1, 0, "abcd", RED, BLUE);

        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(0, 0));
        assertEquals('a', grid.getCodePoint(1, 0));
        assertEquals('b', grid.getCodePoint(2, 0));
    }

    @Test
    void textIsClippedAtNegativeColumn() {
        TerminalGrid grid = new TerminalGrid(3, 1);

        GridDrawing.text(grid, -2, 0, "abcd", RED, BLUE);

        assertEquals('c', grid.getCodePoint(0, 0));
        assertEquals('d', grid.getCodePoint(1, 0));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(2, 0));
    }

    @Test
    void textOutsideRowsOrColumnsWritesNothing() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        GridDrawing.text(grid, 0, -1, "abc", RED, BLUE);
        GridDrawing.text(grid, 0, 2, "abc", RED, BLUE);
        GridDrawing.text(grid, Integer.MAX_VALUE, 0, "abc", RED, BLUE);

        assertBlank(grid, 0, 0, 3, 2);
    }

    @Test
    void fillSetsRectangleAndNothingOutside() {
        TerminalGrid grid = new TerminalGrid(5, 4);

        GridDrawing.fill(grid, 1, 1, 3, 2, '#', RED, BLUE);

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 5; x++) {
                boolean inside = x >= 1 && x <= 3 && y >= 1 && y <= 2;
                assertEquals(inside ? '#' : TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(x, y));
            }
        }
        assertEquals(RED, grid.getForeground(2, 2));
        assertEquals(BLUE, grid.getBackground(2, 2));
    }

    @Test
    void fillWithZeroSizeWritesNothing() {
        TerminalGrid grid = new TerminalGrid(3, 3);

        GridDrawing.fill(grid, 0, 0, 0, 3, '#', RED, BLUE);
        GridDrawing.fill(grid, 0, 0, 3, 0, '#', RED, BLUE);

        assertBlank(grid, 0, 0, 3, 3);
    }

    @Test
    void fillWithNegativeSizeThrows() {
        TerminalGrid grid = new TerminalGrid(3, 3);

        assertThrows(IllegalArgumentException.class, () -> GridDrawing.fill(grid, 0, 0, -1, 1, '#', RED, BLUE));
        assertThrows(IllegalArgumentException.class, () -> GridDrawing.fill(grid, 0, 0, 1, -1, '#', RED, BLUE));
    }

    @Test
    void fillClipsHugeAndOffGridRectangles() {
        TerminalGrid grid = new TerminalGrid(3, 3);

        GridDrawing.fill(grid, -5, -5, Integer.MAX_VALUE, Integer.MAX_VALUE, '#', RED, BLUE);

        assertFilled(grid, 0, 0, 3, 3, '#');
    }

    @Test
    void fillFarOffGridWritesNothing() {
        TerminalGrid grid = new TerminalGrid(3, 3);

        GridDrawing.fill(grid, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 3, '#', RED, BLUE);
        GridDrawing.fill(grid, Integer.MIN_VALUE, 0, 10, 3, '#', RED, BLUE);

        assertBlank(grid, 0, 0, 3, 3);
    }

    @Test
    void boxDrawsCornersEdgesAndKeepsInside() {
        TerminalGrid grid = new TerminalGrid(6, 5);

        GridDrawing.box(grid, 1, 1, 4, 3, RED, BLUE);

        assertEquals('┌', grid.getCodePoint(1, 1));
        assertEquals('┐', grid.getCodePoint(4, 1));
        assertEquals('└', grid.getCodePoint(1, 3));
        assertEquals('┘', grid.getCodePoint(4, 3));
        assertEquals('─', grid.getCodePoint(2, 1));
        assertEquals('─', grid.getCodePoint(3, 3));
        assertEquals('│', grid.getCodePoint(1, 2));
        assertEquals('│', grid.getCodePoint(4, 2));
        assertEquals(RED, grid.getForeground(2, 1));
        assertEquals(BLUE, grid.getBackground(4, 2));
        assertBlank(grid, 2, 2, 2, 1);
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(0, 0));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(5, 4));
    }

    @Test
    void boxSmallerThanTwoDrawsNothing() {
        TerminalGrid grid = new TerminalGrid(4, 4);

        GridDrawing.box(grid, 0, 0, 1, 4, RED, BLUE);
        GridDrawing.box(grid, 0, 0, 4, 1, RED, BLUE);
        GridDrawing.box(grid, 0, 0, 0, 0, RED, BLUE);

        assertBlank(grid, 0, 0, 4, 4);
    }

    @Test
    void boxWithNegativeSizeThrows() {
        TerminalGrid grid = new TerminalGrid(4, 4);

        assertThrows(IllegalArgumentException.class, () -> GridDrawing.box(grid, 0, 0, -1, 3, RED, BLUE));
        assertThrows(IllegalArgumentException.class, () -> GridDrawing.box(grid, 0, 0, 3, -1, RED, BLUE));
    }

    @Test
    void boxPartlyOffGridDrawsVisiblePart() {
        TerminalGrid grid = new TerminalGrid(4, 4);

        GridDrawing.box(grid, -2, -1, 5, 4, RED, BLUE);

        assertEquals('│', grid.getCodePoint(2, 0));
        assertEquals('│', grid.getCodePoint(2, 1));
        assertEquals('┘', grid.getCodePoint(2, 2));
        assertEquals('─', grid.getCodePoint(0, 2));
        assertEquals('─', grid.getCodePoint(1, 2));
        assertBlank(grid, 0, 0, 2, 2);
        assertBlank(grid, 3, 0, 1, 4);
        assertBlank(grid, 0, 3, 4, 1);
    }

    @Test
    void boxFarOffGridWritesNothing() {
        TerminalGrid grid = new TerminalGrid(4, 4);

        GridDrawing.box(grid, Integer.MAX_VALUE - 1, 0, Integer.MAX_VALUE, 4, RED, BLUE);
        GridDrawing.box(grid, -100, -100, 10, 10, RED, BLUE);

        assertBlank(grid, 0, 0, 4, 4);
    }

    private static void assertBlank(TerminalGrid grid, int left, int top, int width, int height) {
        assertFilled(grid, left, top, width, height, TerminalGrid.EMPTY_CODE_POINT);
    }

    private static void assertFilled(TerminalGrid grid, int left, int top, int width, int height, int glyph) {
        for (int y = top; y < top + height; y++) {
            for (int x = left; x < left + width; x++) {
                assertEquals(glyph, grid.getCodePoint(x, y), "cell (" + x + ", " + y + ")");
            }
        }
    }
}
