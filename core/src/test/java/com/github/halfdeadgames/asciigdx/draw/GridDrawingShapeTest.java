package com.github.halfdeadgames.asciigdx.draw;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.shape.CellShape;
import com.github.halfdeadgames.asciigdx.shape.CircleShape;
import com.github.halfdeadgames.asciigdx.shape.HorizontalLineShape;
import com.github.halfdeadgames.asciigdx.shape.RectangleShape;
import com.github.halfdeadgames.asciigdx.shape.ShapeFill;
import com.github.halfdeadgames.asciigdx.shape.VerticalLineShape;
import org.junit.jupiter.api.Test;

class GridDrawingShapeTest {

    private static final int RED = 0xFF0000FF;
    private static final int BLUE = 0x0000FFFF;
    private static final int EMPTY = TerminalGrid.EMPTY_CODE_POINT;

    @Test
    void filledCircleWritesGlyphAndColorsOnlyInsideTheCircle() {
        TerminalGrid grid = new TerminalGrid(12, 12);
        CircleShape circle = new CircleShape(6f, 6f, 3f, ShapeFill.FILLED, 1f);

        GridDrawing.fillShape(grid, circle, '*', RED, BLUE);

        assertEquals('*', grid.getCodePoint(6, 6));
        assertEquals(RED, grid.getForeground(6, 6));
        assertEquals(BLUE, grid.getBackground(6, 6));
        assertEquals('*', grid.getCodePoint(6, 3));
        assertEquals(EMPTY, grid.getCodePoint(0, 0));
        assertEquals(EMPTY, grid.getCodePoint(3, 3));
        assertEquals(EMPTY, grid.getCodePoint(11, 11));
        assertEquals(countCells(circle), countGlyphs(grid, '*'));
    }

    @Test
    void outlineRectangleLeavesInsideUntouched() {
        TerminalGrid grid = new TerminalGrid(8, 8);

        GridDrawing.fillShape(grid, RectangleShape.forSquareCells(1, 1, 5, 5, ShapeFill.OUTLINE),
                '#', RED, BLUE);

        assertEquals('#', grid.getCodePoint(1, 1));
        assertEquals('#', grid.getCodePoint(5, 5));
        assertEquals('#', grid.getCodePoint(1, 3));
        assertEquals('#', grid.getCodePoint(3, 1));
        assertEquals(EMPTY, grid.getCodePoint(3, 3));
        assertEquals(EMPTY, grid.getCodePoint(2, 2));
        assertEquals(16, countGlyphs(grid, '#'));
    }

    @Test
    void shapePartlyOffGridWritesOnlyTheVisiblePart() {
        TerminalGrid grid = new TerminalGrid(4, 4);
        RectangleShape topLeft = RectangleShape.forSquareCells(-2, -2, 4, 4, ShapeFill.FILLED);
        RectangleShape bottomRight = RectangleShape.forSquareCells(3, 3, 5, 5, ShapeFill.FILLED);

        assertDoesNotThrow(() -> GridDrawing.fillShape(grid, topLeft, 'a', RED, BLUE));
        assertDoesNotThrow(() -> GridDrawing.fillShape(grid, bottomRight, 'b', RED, BLUE));

        assertEquals(4 + 1, countGlyphs(grid, 'a') + countGlyphs(grid, 'b'));
        assertEquals('a', grid.getCodePoint(0, 0));
        assertEquals('a', grid.getCodePoint(1, 1));
        assertEquals(EMPTY, grid.getCodePoint(2, 2));
        assertEquals('b', grid.getCodePoint(3, 3));
    }

    @Test
    void shapeEntirelyOffGridWritesNothing() {
        TerminalGrid grid = new TerminalGrid(4, 4);

        GridDrawing.fillShape(grid, RectangleShape.forSquareCells(-10, 0, 5, 4, ShapeFill.FILLED),
                '#', RED, BLUE);
        GridDrawing.fillShape(grid, RectangleShape.forSquareCells(4, 0, 3, 4, ShapeFill.FILLED),
                '#', RED, BLUE);
        GridDrawing.fillShape(grid, RectangleShape.forSquareCells(0, -5, 4, 5, ShapeFill.FILLED),
                '#', RED, BLUE);
        GridDrawing.fillShape(grid, RectangleShape.forSquareCells(0, 4, 4, 2, ShapeFill.FILLED),
                '#', RED, BLUE);

        assertEquals(0, countGlyphs(grid, '#'));
    }

    @Test
    void horizontalLineWritesOneRow() {
        TerminalGrid grid = new TerminalGrid(6, 3);

        GridDrawing.fillShape(grid, new HorizontalLineShape(1, 1, 4), '-', RED, BLUE);

        for (int column = 0; column < 6; column++) {
            boolean onLine = column >= 1 && column <= 4;
            assertEquals(onLine ? '-' : EMPTY, grid.getCodePoint(column, 1));
        }
        assertEquals(4, countGlyphs(grid, '-'));
    }

    @Test
    void verticalLineWritesOneColumnAndClipsAtBottom() {
        TerminalGrid grid = new TerminalGrid(3, 4);

        GridDrawing.fillShape(grid, VerticalLineShape.forSquareCells(1, 2, 10), '|', RED, BLUE);

        assertEquals('|', grid.getCodePoint(1, 2));
        assertEquals('|', grid.getCodePoint(1, 3));
        assertEquals(EMPTY, grid.getCodePoint(1, 1));
        assertEquals(2, countGlyphs(grid, '|'));
    }

    @Test
    void tallAspectCircleCoversFewerRowsThanColumns() {
        TerminalGrid grid = new TerminalGrid(20, 20);

        GridDrawing.fillShape(
                grid, new CircleShape(10f, 10f, 6f, ShapeFill.FILLED, 2f), 'o', RED, BLUE);

        int rows = countRowsWithGlyph(grid, 'o');
        int columns = countColumnsWithGlyph(grid, 'o');
        assertTrue(rows < columns, "rows " + rows + " should be fewer than columns " + columns);
    }

    @Test
    void consecutiveCallsDoNotLeakVisitorState() {
        TerminalGrid first = new TerminalGrid(4, 4);
        TerminalGrid second = new TerminalGrid(4, 4);
        RectangleShape shape = RectangleShape.forSquareCells(0, 0, 2, 2, ShapeFill.FILLED);

        GridDrawing.fillShape(first, shape, 'a', RED, BLUE);
        GridDrawing.fillShape(second, shape, 'b', BLUE, RED);

        assertEquals('a', first.getCodePoint(1, 1));
        assertEquals(RED, first.getForeground(1, 1));
        assertEquals('b', second.getCodePoint(1, 1));
        assertEquals(BLUE, second.getForeground(1, 1));
        assertEquals(RED, second.getBackground(1, 1));
        assertEquals(4, countGlyphs(first, 'a'));
        assertEquals(0, countGlyphs(first, 'b'));
    }

    @Test
    void fillShapeWithNullArgumentsThrows() {
        TerminalGrid grid = new TerminalGrid(4, 4);
        RectangleShape shape = RectangleShape.forSquareCells(0, 0, 2, 2, ShapeFill.FILLED);

        assertThrows(NullPointerException.class,
                () -> GridDrawing.fillShape(null, shape, '#', RED, BLUE));
        assertThrows(NullPointerException.class,
                () -> GridDrawing.fillShape(grid, null, '#', RED, BLUE));
    }

    private static int countCells(CellShape shape) {
        int[] count = new int[1];
        shape.forEachCell((column, row, distance, fraction) -> count[0]++);
        return count[0];
    }

    private static int countGlyphs(TerminalGrid grid, int glyph) {
        int count = 0;
        for (int row = 0; row < grid.getRows(); row++) {
            for (int column = 0; column < grid.getColumns(); column++) {
                if (grid.getCodePoint(column, row) == glyph) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countRowsWithGlyph(TerminalGrid grid, int glyph) {
        int count = 0;
        for (int row = 0; row < grid.getRows(); row++) {
            for (int column = 0; column < grid.getColumns(); column++) {
                if (grid.getCodePoint(column, row) == glyph) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private static int countColumnsWithGlyph(TerminalGrid grid, int glyph) {
        int count = 0;
        for (int column = 0; column < grid.getColumns(); column++) {
            for (int row = 0; row < grid.getRows(); row++) {
                if (grid.getCodePoint(column, row) == glyph) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }
}
