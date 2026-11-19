package com.github.halfdeadgames.asciigdx.draw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.List;
import org.junit.jupiter.api.Test;

class GridDrawingTest {

    private static final int RED = 0xFF0000FF;
    private static final int BLUE = 0x0000FFFF;

    @Test
    void drawTextWritesGlyphsAndColors() {
        TerminalGrid grid = new TerminalGrid(6, 2);

        GridDrawing.drawText(grid, 1, 1, "abc", RED, BLUE);

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
    void drawTextUsesOneCellPerSupplementaryCodePoint() {
        TerminalGrid grid = new TerminalGrid(4, 1);
        int dagger = 0x1F5E1;

        GridDrawing.drawText(grid, 0, 0, new StringBuilder().appendCodePoint(dagger).append('x'),
                RED, BLUE);

        assertEquals(dagger, grid.getCodePoint(0, 0));
        assertEquals('x', grid.getCodePoint(1, 0));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(2, 0));
    }

    @Test
    void drawTextIsClippedAtRightEdge() {
        TerminalGrid grid = new TerminalGrid(3, 1);

        GridDrawing.drawText(grid, 1, 0, "abcd", RED, BLUE);

        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(0, 0));
        assertEquals('a', grid.getCodePoint(1, 0));
        assertEquals('b', grid.getCodePoint(2, 0));
    }

    @Test
    void drawTextIsClippedAtNegativeColumn() {
        TerminalGrid grid = new TerminalGrid(3, 1);

        GridDrawing.drawText(grid, -2, 0, "abcd", RED, BLUE);

        assertEquals('c', grid.getCodePoint(0, 0));
        assertEquals('d', grid.getCodePoint(1, 0));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(2, 0));
    }

    @Test
    void drawTextOutsideRowsOrColumnsWritesNothing() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        GridDrawing.drawText(grid, 0, -1, "abc", RED, BLUE);
        GridDrawing.drawText(grid, 0, 2, "abc", RED, BLUE);
        GridDrawing.drawText(grid, Integer.MAX_VALUE, 0, "abc", RED, BLUE);

        assertBlank(grid, 0, 0, 3, 2);
    }

    @Test
    void drawTextWithNullArgumentsThrows() {
        TerminalGrid grid = new TerminalGrid(3, 2);

        assertThrows(NullPointerException.class,
                () -> GridDrawing.drawText(null, 0, 0, "abc", RED, BLUE));
        assertThrows(NullPointerException.class,
                () -> GridDrawing.drawText(grid, 0, 0, null, RED, BLUE));
        assertThrows(NullPointerException.class,
                () -> GridDrawing.drawText(grid, 0, 99, null, RED, BLUE));
    }

    @Test
    void fillRectangleSetsRectangleAndNothingOutside() {
        TerminalGrid grid = new TerminalGrid(5, 4);

        GridDrawing.fillRectangle(grid, 1, 1, 3, 2, '#', RED, BLUE);

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
    void fillRectangleWithZeroSizeWritesNothing() {
        TerminalGrid grid = new TerminalGrid(3, 3);

        GridDrawing.fillRectangle(grid, 0, 0, 0, 3, '#', RED, BLUE);
        GridDrawing.fillRectangle(grid, 0, 0, 3, 0, '#', RED, BLUE);

        assertBlank(grid, 0, 0, 3, 3);
    }

    @Test
    void fillRectangleWithNegativeSizeThrows() {
        TerminalGrid grid = new TerminalGrid(3, 3);

        assertThrows(IllegalArgumentException.class,
                () -> GridDrawing.fillRectangle(grid, 0, 0, -1, 1, '#', RED, BLUE));
        assertThrows(IllegalArgumentException.class,
                () -> GridDrawing.fillRectangle(grid, 0, 0, 1, -1, '#', RED, BLUE));
    }

    @Test
    void fillRectangleWithNullGridThrows() {
        assertThrows(NullPointerException.class,
                () -> GridDrawing.fillRectangle(null, 0, 0, 1, 1, '#', RED, BLUE));
    }

    @Test
    void fillRectangleClipsHugeAndOffGridRectangles() {
        TerminalGrid grid = new TerminalGrid(3, 3);

        GridDrawing.fillRectangle(
                grid, -5, -5, Integer.MAX_VALUE, Integer.MAX_VALUE, '#', RED, BLUE);

        assertFilled(grid, 0, 0, 3, 3, '#');
    }

    @Test
    void fillRectangleFarOffGridWritesNothing() {
        TerminalGrid grid = new TerminalGrid(3, 3);

        GridDrawing.fillRectangle(grid, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 3, '#', RED, BLUE);
        GridDrawing.fillRectangle(grid, Integer.MIN_VALUE, 0, 10, 3, '#', RED, BLUE);

        assertBlank(grid, 0, 0, 3, 3);
    }

    @Test
    void drawBoxDrawsCornersEdgesAndKeepsInside() {
        TerminalGrid grid = new TerminalGrid(6, 5);

        GridDrawing.drawBox(grid, 1, 1, 4, 3, RED, BLUE);

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
    void drawBoxSmallerThanTwoDrawsNothing() {
        TerminalGrid grid = new TerminalGrid(4, 4);

        GridDrawing.drawBox(grid, 0, 0, 1, 4, RED, BLUE);
        GridDrawing.drawBox(grid, 0, 0, 4, 1, RED, BLUE);
        GridDrawing.drawBox(grid, 0, 0, 0, 0, RED, BLUE);

        assertBlank(grid, 0, 0, 4, 4);
    }

    @Test
    void drawBoxWithNegativeSizeThrows() {
        TerminalGrid grid = new TerminalGrid(4, 4);

        assertThrows(IllegalArgumentException.class,
                () -> GridDrawing.drawBox(grid, 0, 0, -1, 3, RED, BLUE));
        assertThrows(IllegalArgumentException.class,
                () -> GridDrawing.drawBox(grid, 0, 0, 3, -1, RED, BLUE));
    }

    @Test
    void drawBoxWithNullGridThrows() {
        assertThrows(NullPointerException.class,
                () -> GridDrawing.drawBox(null, 0, 0, 1, 1, RED, BLUE));
    }

    @Test
    void drawBoxPartlyOffGridDrawsVisiblePart() {
        TerminalGrid grid = new TerminalGrid(4, 4);

        GridDrawing.drawBox(grid, -2, -1, 5, 4, RED, BLUE);

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
    void drawBoxFarOffGridWritesNothing() {
        TerminalGrid grid = new TerminalGrid(4, 4);

        GridDrawing.drawBox(grid, Integer.MAX_VALUE - 1, 0, Integer.MAX_VALUE, 4, RED, BLUE);
        GridDrawing.drawBox(grid, -100, -100, 10, 10, RED, BLUE);

        assertBlank(grid, 0, 0, 4, 4);
    }

    @Test
    void drawBoxUsesGlyphsOfEachStyle() {
        assertBoxGlyphs(BoxStyle.SINGLE, "─│┌┐└┘");
        assertBoxGlyphs(BoxStyle.DOUBLE, "═║╔╗╚╝");
        assertBoxGlyphs(BoxStyle.HEAVY, "━┃┏┓┗┛");
        assertBoxGlyphs(BoxStyle.ROUNDED, "─│╭╮╰╯");
    }

    @Test
    void drawBoxWithStyleKeepsColorsAndInside() {
        TerminalGrid grid = new TerminalGrid(5, 4);

        GridDrawing.drawBox(grid, 0, 0, 5, 4, BoxStyle.DOUBLE, RED, BLUE);

        assertEquals(RED, grid.getForeground(0, 0));
        assertEquals(BLUE, grid.getBackground(2, 3));
        assertBlank(grid, 1, 1, 3, 2);
    }

    @Test
    void drawBoxWithoutStyleStaysSingleLine() {
        TerminalGrid plain = new TerminalGrid(4, 3);
        TerminalGrid single = new TerminalGrid(4, 3);

        GridDrawing.drawBox(plain, 0, 0, 4, 3, RED, BLUE);
        GridDrawing.drawBox(single, 0, 0, 4, 3, BoxStyle.SINGLE, RED, BLUE);

        assertEquals('┌', plain.getCodePoint(0, 0));
        assertSameGlyphs(single, plain);
    }

    @Test
    void drawBoxWithNullStyleThrows() {
        TerminalGrid grid = new TerminalGrid(4, 4);

        assertThrows(NullPointerException.class,
                () -> GridDrawing.drawBox(grid, 0, 0, 3, 3, null, RED, BLUE));
    }

    @Test
    void horizontalBarFullAndEmpty() {
        assertEquals("████", renderHorizontalBar(4, 1f));
        assertEquals("    ", renderHorizontalBar(4, 0f));
    }

    @Test
    void horizontalBarEachPartialEighthUsesItsBlock() {
        List<Integer> leftEighths =
                List.of(0x258F, 0x258E, 0x258D, 0x258C, 0x258B, 0x258A, 0x2589);
        for (int eighths = 1; eighths <= 7; eighths++) {
            String bar = renderHorizontalBar(4, eighths / 32f);

            assertEquals((int) leftEighths.get(eighths - 1), bar.codePointAt(0));
            assertEquals("   ", bar.substring(1));
        }
    }

    @Test
    void horizontalBarPartialCellFollowsFullCells() {
        assertEquals("█▌  ", renderHorizontalBar(4, 12f / 32f));
    }

    @Test
    void horizontalBarClampsFraction() {
        assertEquals(renderHorizontalBar(4, 1f), renderHorizontalBar(4, 5f));
        assertEquals(renderHorizontalBar(4, 0f), renderHorizontalBar(4, -2f));
    }

    @Test
    void horizontalBarRoundsToNearestEighth() {
        assertEquals("▏", renderHorizontalBar(1, 0.14f));
        assertEquals(" ", renderHorizontalBar(1, 0.06f));
        assertEquals("█", renderHorizontalBar(1, 0.97f));
    }

    @Test
    void horizontalBarWritesColorsToEveryCell() {
        TerminalGrid grid = new TerminalGrid(4, 1);

        GridDrawing.drawHorizontalBar(grid, 0, 0, 4, 0.5f, RED, BLUE);

        for (int column = 0; column < 4; column++) {
            assertEquals(RED, grid.getForeground(column, 0));
            assertEquals(BLUE, grid.getBackground(column, 0));
        }
    }

    @Test
    void shrinkingHorizontalBarErasesOldFill() {
        TerminalGrid grid = new TerminalGrid(4, 1);

        GridDrawing.drawHorizontalBar(grid, 0, 0, 4, 1f, RED, BLUE);
        GridDrawing.drawHorizontalBar(grid, 0, 0, 4, 0.25f, RED, BLUE);

        assertEquals("█   ", readRow(grid, 0));
    }

    @Test
    void horizontalBarIsClippedToGrid() {
        TerminalGrid grid = new TerminalGrid(4, 2);

        GridDrawing.drawHorizontalBar(grid, -2, 0, 4, 1f, RED, BLUE);
        GridDrawing.drawHorizontalBar(grid, 3, 1, 4, 1f, RED, BLUE);
        GridDrawing.drawHorizontalBar(grid, 0, 2, 4, 1f, RED, BLUE);
        GridDrawing.drawHorizontalBar(grid, 0, -1, 4, 1f, RED, BLUE);

        assertEquals("██  ", readRow(grid, 0));
        assertEquals("   █", readRow(grid, 1));
    }

    @Test
    void horizontalBarFarOffGridOrZeroLengthWritesNothing() {
        TerminalGrid grid = new TerminalGrid(4, 1);

        GridDrawing.drawHorizontalBar(grid, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 1f, RED, BLUE);
        GridDrawing.drawHorizontalBar(grid, Integer.MIN_VALUE, 0, 10, 1f, RED, BLUE);
        GridDrawing.drawHorizontalBar(grid, 0, 0, 0, 1f, RED, BLUE);

        assertBlank(grid, 0, 0, 4, 1);
    }

    @Test
    void horizontalBarRejectsBadArguments() {
        TerminalGrid grid = new TerminalGrid(4, 1);

        assertThrows(IllegalArgumentException.class,
                () -> GridDrawing.drawHorizontalBar(grid, 0, 0, 4, Float.NaN, RED, BLUE));
        assertThrows(IllegalArgumentException.class,
                () -> GridDrawing.drawHorizontalBar(grid, 0, 0, -1, 0.5f, RED, BLUE));
        assertThrows(NullPointerException.class,
                () -> GridDrawing.drawHorizontalBar(null, 0, 0, 4, 0.5f, RED, BLUE));
    }

    @Test
    void verticalBarFullAndEmpty() {
        assertEquals("████", renderVerticalBar(4, 1f));
        assertEquals("    ", renderVerticalBar(4, 0f));
    }

    @Test
    void verticalBarFillGrowsUpFromBottomCell() {
        // Listed top row first: the full cell sits at the bottom.
        assertEquals("  ▄█", renderVerticalBar(4, 12f / 32f));
    }

    @Test
    void verticalBarEachPartialEighthUsesItsBlock() {
        List<Integer> lowerEighths =
                List.of(0x2581, 0x2582, 0x2583, 0x2584, 0x2585, 0x2586, 0x2587);
        for (int eighths = 1; eighths <= 7; eighths++) {
            String bar = renderVerticalBar(4, eighths / 32f);

            assertEquals((int) lowerEighths.get(eighths - 1), bar.codePointAt(3));
            assertEquals("   ", bar.substring(0, 3));
        }
    }

    @Test
    void verticalBarClampsAndRounds() {
        assertEquals(renderVerticalBar(4, 1f), renderVerticalBar(4, 5f));
        assertEquals(renderVerticalBar(4, 0f), renderVerticalBar(4, -2f));
        assertEquals("▁", renderVerticalBar(1, 0.14f));
        assertEquals(" ", renderVerticalBar(1, 0.06f));
        assertEquals("█", renderVerticalBar(1, 0.97f));
    }

    @Test
    void shrinkingVerticalBarErasesOldFill() {
        TerminalGrid grid = new TerminalGrid(1, 4);

        GridDrawing.drawVerticalBar(grid, 0, 0, 4, 1f, RED, BLUE);
        GridDrawing.drawVerticalBar(grid, 0, 0, 4, 0.25f, RED, BLUE);

        assertEquals("   █", readColumn(grid, 0));
        assertEquals(BLUE, grid.getBackground(0, 0));
    }

    @Test
    void verticalBarIsClippedToGrid() {
        TerminalGrid grid = new TerminalGrid(2, 4);

        GridDrawing.drawVerticalBar(grid, 0, -2, 4, 1f, RED, BLUE);
        GridDrawing.drawVerticalBar(grid, 1, 3, 4, 1f, RED, BLUE);
        GridDrawing.drawVerticalBar(grid, 2, 0, 4, 1f, RED, BLUE);
        GridDrawing.drawVerticalBar(grid, -1, 0, 4, 1f, RED, BLUE);

        assertEquals("██  ", readColumn(grid, 0));
        assertEquals("   █", readColumn(grid, 1));
    }

    @Test
    void verticalBarFarOffGridOrZeroLengthWritesNothing() {
        TerminalGrid grid = new TerminalGrid(1, 4);

        GridDrawing.drawVerticalBar(grid, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, 1f, RED, BLUE);
        GridDrawing.drawVerticalBar(grid, 0, Integer.MIN_VALUE, 10, 1f, RED, BLUE);
        GridDrawing.drawVerticalBar(grid, 0, 0, 0, 1f, RED, BLUE);

        assertBlank(grid, 0, 0, 1, 4);
    }

    @Test
    void verticalBarRejectsBadArguments() {
        TerminalGrid grid = new TerminalGrid(1, 4);

        assertThrows(IllegalArgumentException.class,
                () -> GridDrawing.drawVerticalBar(grid, 0, 0, 4, Float.NaN, RED, BLUE));
        assertThrows(IllegalArgumentException.class,
                () -> GridDrawing.drawVerticalBar(grid, 0, 0, -1, 0.5f, RED, BLUE));
        assertThrows(NullPointerException.class,
                () -> GridDrawing.drawVerticalBar(null, 0, 0, 4, 0.5f, RED, BLUE));
    }

    @Test
    void hugeVerticalBarShowsItsOnGridBottomCells() {
        int hugeLength = 400_000_000;
        int top = -(hugeLength - 3);
        TerminalGrid fullGrid = new TerminalGrid(1, 3);
        TerminalGrid partialGrid = new TerminalGrid(1, 3);

        GridDrawing.drawVerticalBar(fullGrid, 0, top, hugeLength, 1f, RED, BLUE);
        GridDrawing.drawVerticalBar(partialGrid, 0, top, hugeLength, 3f / (8f * hugeLength),
                RED, BLUE);

        assertEquals("███", readColumn(fullGrid, 0));
        assertEquals("  ▃", readColumn(partialGrid, 0));
    }

    @Test
    void hugeHorizontalBarShowsItsOnGridLastCells() {
        int hugeLength = 400_000_000;
        int left = -(hugeLength - 3);
        TerminalGrid fullGrid = new TerminalGrid(3, 1);
        TerminalGrid halfGrid = new TerminalGrid(3, 1);

        GridDrawing.drawHorizontalBar(fullGrid, left, 0, hugeLength, 1f, RED, BLUE);
        GridDrawing.drawHorizontalBar(halfGrid, left, 0, hugeLength, 0.5f, RED, BLUE);

        assertEquals("███", readRow(fullGrid, 0));
        assertEquals("   ", readRow(halfGrid, 0));
    }

    private static void assertBoxGlyphs(BoxStyle style, String glyphs) {
        TerminalGrid grid = new TerminalGrid(4, 3);

        GridDrawing.drawBox(grid, 0, 0, 4, 3, style, RED, BLUE);

        assertEquals(glyphs.charAt(0), grid.getCodePoint(1, 0), style + " horizontal top");
        assertEquals(glyphs.charAt(0), grid.getCodePoint(2, 2), style + " horizontal bottom");
        assertEquals(glyphs.charAt(1), grid.getCodePoint(0, 1), style + " vertical left");
        assertEquals(glyphs.charAt(1), grid.getCodePoint(3, 1), style + " vertical right");
        assertEquals(glyphs.charAt(2), grid.getCodePoint(0, 0), style + " top left");
        assertEquals(glyphs.charAt(3), grid.getCodePoint(3, 0), style + " top right");
        assertEquals(glyphs.charAt(4), grid.getCodePoint(0, 2), style + " bottom left");
        assertEquals(glyphs.charAt(5), grid.getCodePoint(3, 2), style + " bottom right");
    }

    private static void assertSameGlyphs(TerminalGrid expected, TerminalGrid actual) {
        for (int y = 0; y < expected.getRows(); y++) {
            for (int x = 0; x < expected.getColumns(); x++) {
                assertEquals(expected.getCodePoint(x, y), actual.getCodePoint(x, y));
            }
        }
    }

    private static String renderHorizontalBar(int lengthCells, float fraction) {
        TerminalGrid grid = new TerminalGrid(lengthCells, 1);
        GridDrawing.drawHorizontalBar(grid, 0, 0, lengthCells, fraction, RED, BLUE);
        return readRow(grid, 0);
    }

    private static String renderVerticalBar(int lengthCells, float fraction) {
        TerminalGrid grid = new TerminalGrid(1, lengthCells);
        GridDrawing.drawVerticalBar(grid, 0, 0, lengthCells, fraction, RED, BLUE);
        return readColumn(grid, 0);
    }

    private static String readRow(TerminalGrid grid, int row) {
        StringBuilder text = new StringBuilder();
        for (int column = 0; column < grid.getColumns(); column++) {
            text.appendCodePoint(visibleGlyph(grid.getCodePoint(column, row)));
        }
        return text.toString();
    }

    private static String readColumn(TerminalGrid grid, int column) {
        StringBuilder text = new StringBuilder();
        for (int row = 0; row < grid.getRows(); row++) {
            text.appendCodePoint(visibleGlyph(grid.getCodePoint(column, row)));
        }
        return text.toString();
    }

    private static int visibleGlyph(int codePoint) {
        return codePoint == TerminalGrid.EMPTY_CODE_POINT ? ' ' : codePoint;
    }

    private static void assertBlank(TerminalGrid grid, int left, int top, int width, int height) {
        assertFilled(grid, left, top, width, height, TerminalGrid.EMPTY_CODE_POINT);
    }

    private static void assertFilled(TerminalGrid grid, int left, int top, int width, int height,
            int glyph) {
        for (int y = top; y < top + height; y++) {
            for (int x = left; x < left + width; x++) {
                assertEquals(glyph, grid.getCodePoint(x, y), "cell (" + x + ", " + y + ")");
            }
        }
    }
}
