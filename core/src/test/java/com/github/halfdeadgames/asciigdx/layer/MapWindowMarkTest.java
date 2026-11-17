package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MapWindowMarkTest {

    private static final int COLUMNS = 10;
    private static final int ROWS = 6;
    private static final int CELL = 16;
    private static final int ORIGIN_COLUMN = 3;
    private static final int ORIGIN_ROW = 2;

    private static final class SquareSource implements GlyphSource {
        @Override
        public int getCellWidth() {
            return CELL;
        }

        @Override
        public int getCellHeight() {
            return CELL;
        }

        @Override
        public TextureRegion getGlyphRegion(int codePoint) {
            return null;
        }

        @Override
        public boolean hasGlyph(int codePoint) {
            return false;
        }

        @Override
        public void dispose() {
        }
    }

    private final LayerStack stack =
            TestStacks.withRenderers((grid, source, shared) -> new SilentRenderer());
    private final TerminalGrid grid = new TerminalGrid(COLUMNS, ROWS);
    private final Layer layer = stack.add(grid, new SquareSource());
    private final MapWindow window = enableFilledAtOrigin();

    /** Puts the window's origin at (ORIGIN_COLUMN, ORIGIN_ROW) with no cell flagged. */
    private MapWindow enableFilledAtOrigin() {
        stack.setCamera(ORIGIN_COLUMN * CELL, ORIGIN_ROW * CELL);
        layer.enableMapWindow();
        MapWindow enabled = layer.getMapWindow();
        enabled.markAllFilled();
        return enabled;
    }

    private List<String> collectToFill() {
        List<String> visited = new ArrayList<>();
        window.forEachCellToFill((mapColumn, mapRow) -> visited.add(mapColumn + "," + mapRow));
        return visited;
    }

    private static List<String> rectangleCells(int firstColumn, int lastColumn, int firstRow,
            int lastRow) {
        List<String> cells = new ArrayList<>();
        for (int row = firstRow; row <= lastRow; row++) {
            for (int column = firstColumn; column <= lastColumn; column++) {
                cells.add(column + "," + row);
            }
        }
        return cells;
    }

    @Test
    void markNeedsFillFlagsOnlyThatCell() {
        window.markNeedsFill(5, 4);

        assertEquals(List.of("5,4"), collectToFill());
    }

    @Test
    void markingTheSameCellTwiceVisitsItOnce() {
        window.markNeedsFill(5, 4);
        window.markNeedsFill(5, 4);

        assertEquals(List.of("5,4"), collectToFill());
    }

    @Test
    void markAllFilledClearsMarkedCells() {
        window.markNeedsFill(5, 4);

        window.markAllFilled();

        assertTrue(collectToFill().isEmpty());
    }

    @Test
    void markNeedsFillOutsideTheWindowIsSkippedSilently() {
        window.markNeedsFill(ORIGIN_COLUMN - 1, ORIGIN_ROW);
        window.markNeedsFill(ORIGIN_COLUMN, ORIGIN_ROW - 1);
        window.markNeedsFill(ORIGIN_COLUMN + COLUMNS, ORIGIN_ROW);
        window.markNeedsFill(ORIGIN_COLUMN, ORIGIN_ROW + ROWS);

        assertTrue(collectToFill().isEmpty());
    }

    @Test
    void markAreaNeedsFillFlagsTheRectangleInRowMajorOrder() {
        window.markAreaNeedsFill(4, 3, 3, 2);

        assertEquals(rectangleCells(4, 6, 3, 4), collectToFill());
    }

    @Test
    void markAreaNeedsFillClipsAtTheTopLeftEdges() {
        window.markAreaNeedsFill(ORIGIN_COLUMN - 2, ORIGIN_ROW - 1, 4, 3);

        assertEquals(rectangleCells(ORIGIN_COLUMN, ORIGIN_COLUMN + 1, ORIGIN_ROW, ORIGIN_ROW + 1),
                collectToFill());
    }

    @Test
    void markAreaNeedsFillClipsAtTheBottomRightEdges() {
        int lastColumn = ORIGIN_COLUMN + COLUMNS - 1;
        int lastRow = ORIGIN_ROW + ROWS - 1;

        window.markAreaNeedsFill(lastColumn - 1, lastRow, 5, 5);

        assertEquals(rectangleCells(lastColumn - 1, lastColumn, lastRow, lastRow),
                collectToFill());
    }

    @Test
    void markAreaNeedsFillCoveringTheWholeWindowFlagsEveryCell() {
        window.markAreaNeedsFill(-100, -100, 1000, 1000);

        assertEquals(COLUMNS * ROWS, collectToFill().size());
    }

    @Test
    void markAreaNeedsFillFullyOutsideTheWindowFlagsNothing() {
        window.markAreaNeedsFill(ORIGIN_COLUMN + COLUMNS, ORIGIN_ROW, 3, 3);
        window.markAreaNeedsFill(ORIGIN_COLUMN - 3, ORIGIN_ROW, 3, 3);
        window.markAreaNeedsFill(ORIGIN_COLUMN, ORIGIN_ROW + ROWS, 3, 3);
        window.markAreaNeedsFill(ORIGIN_COLUMN, ORIGIN_ROW - 3, 3, 3);

        assertTrue(collectToFill().isEmpty());
    }

    @Test
    void markAreaNeedsFillWithZeroSizeFlagsNothing() {
        window.markAreaNeedsFill(4, 3, 0, 2);
        window.markAreaNeedsFill(4, 3, 2, 0);

        assertTrue(collectToFill().isEmpty());
    }

    @Test
    void markAreaNeedsFillNearIntegerLimitsDoesNotOverflow() {
        window.markAreaNeedsFill(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MAX_VALUE);
        window.markAreaNeedsFill(Integer.MIN_VALUE, Integer.MIN_VALUE, 1, 1);

        assertTrue(collectToFill().isEmpty());
    }

    @Test
    void markAreaNeedsFillRejectsNegativeSizes() {
        assertThrows(IllegalArgumentException.class, () -> window.markAreaNeedsFill(4, 3, -1, 2));
        assertThrows(IllegalArgumentException.class, () -> window.markAreaNeedsFill(4, 3, 2, -1));
    }

    @Test
    void markAllNeedFillFlagsEveryCell() {
        window.markAllNeedFill();

        assertEquals(rectangleCells(ORIGIN_COLUMN, ORIGIN_COLUMN + COLUMNS - 1, ORIGIN_ROW,
                ORIGIN_ROW + ROWS - 1), collectToFill());
    }

    @Test
    void markedCellsMoveWithTheContentOnScroll() {
        window.markNeedsFill(5, 4);

        stack.setCamera((ORIGIN_COLUMN + 1) * CELL, ORIGIN_ROW * CELL);

        List<String> expected = new ArrayList<>(List.of("5,4"));
        expected.addAll(rectangleCells(ORIGIN_COLUMN + COLUMNS, ORIGIN_COLUMN + COLUMNS,
                ORIGIN_ROW, ORIGIN_ROW + ROWS - 1));
        List<String> visited = collectToFill();
        visited.sort(null);
        expected.sort(null);
        assertEquals(expected, visited);
    }

    @Test
    void markAfterAGridResizeUsesTheNewSize() {
        grid.resize(4, 3);
        window.markAllFilled();

        window.markNeedsFill(ORIGIN_COLUMN + 3, ORIGIN_ROW + 2);

        assertEquals(List.of((ORIGIN_COLUMN + 3) + "," + (ORIGIN_ROW + 2)), collectToFill());
    }

    @Test
    void markRightAfterAGridResizeFlagsEveryCellOfTheNewSize() {
        grid.resize(4, 3);

        window.markNeedsFill(ORIGIN_COLUMN, ORIGIN_ROW);
        window.markAreaNeedsFill(ORIGIN_COLUMN, ORIGIN_ROW, 2, 2);

        assertEquals(rectangleCells(ORIGIN_COLUMN, ORIGIN_COLUMN + 3, ORIGIN_ROW, ORIGIN_ROW + 2),
                collectToFill());
    }

    @Test
    void markMethodsThrowWhenDetached() {
        layer.disableMapWindow();

        assertThrows(IllegalStateException.class, () -> window.markNeedsFill(5, 4));
        assertThrows(IllegalStateException.class, () -> window.markAreaNeedsFill(5, 4, 1, 1));
        assertThrows(IllegalStateException.class, window::markAllNeedFill);
    }
}
