package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MapWindowTest {

    private static final int COLUMNS = 10;
    private static final int ROWS = 6;
    private static final int CELL = 16;

    private static final class SquareSource implements GlyphSource {
        private final int cellSize;

        SquareSource(int cellSize) {
            this.cellSize = cellSize;
        }

        @Override
        public int getCellWidth() {
            return cellSize;
        }

        @Override
        public int getCellHeight() {
            return cellSize;
        }

        @Override
        public TextureRegion glyph(int codePoint) {
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

    private final LayerStack stack = TestStacks.withRenderers((grid, source, shared) -> new SilentRenderer());
    private final TerminalGrid grid = new TerminalGrid(COLUMNS, ROWS);
    private final Layer layer = stack.add(grid, new SquareSource(CELL));

    private MapWindow enable() {
        layer.enableMapWindow();
        return layer.getMapWindow();
    }

    private static List<String> collectToFill(MapWindow window) {
        List<String> visited = new ArrayList<>();
        window.forEachCellToFill((mapColumn, mapRow) -> visited.add(mapColumn + "," + mapRow));
        return visited;
    }

    private static List<String> columnCells(int mapColumn, int firstRow, int lastRow) {
        List<String> cells = new ArrayList<>();
        for (int row = firstRow; row <= lastRow; row++) {
            cells.add(mapColumn + "," + row);
        }
        return cells;
    }

    private static List<String> rectangleCells(int firstColumn, int lastColumn, int firstRow, int lastRow) {
        List<String> cells = new ArrayList<>();
        for (int row = firstRow; row <= lastRow; row++) {
            for (int column = firstColumn; column <= lastColumn; column++) {
                cells.add(column + "," + row);
            }
        }
        return cells;
    }

    private static List<String> sorted(List<String> cells) {
        List<String> copy = new ArrayList<>(cells);
        copy.sort(null);
        return copy;
    }

    @Test
    void getMapWindowThrowsWhenDisabled() {
        assertThrows(IllegalStateException.class, layer::getMapWindow);
    }

    @Test
    void getMapWindowReturnsTheSameWindowUntilReenabled() {
        MapWindow window = enable();

        assertSame(window, layer.getMapWindow());
    }

    @Test
    void originGettersFollowTheCamera() {
        stack.setCamera(48f, 32f);
        MapWindow window = enable();

        assertEquals(3, window.getOriginColumn());
        assertEquals(2, window.getOriginRow());

        stack.setCamera(64f, 32f);
        assertEquals(4, window.getOriginColumn());
    }

    @Test
    void cameraMovingTwoCellsShiftsTheOriginByTwoColumnsAndRows() {
        MapWindow window = enable();

        stack.setCamera(2 * CELL, 2 * CELL);

        assertEquals(2, window.getOriginColumn());
        assertEquals(2, window.getOriginRow());
        assertEquals(2 * CELL, window.getOriginPixelsX(CELL));
        assertEquals(2 * CELL, window.getOriginPixelsY(CELL));
    }

    @Test
    void cameraMovingWithinOneCellKeepsTheOriginPixels() {
        MapWindow window = enable();

        stack.setCamera(CELL - 1, CELL - 1);

        assertEquals(0, window.getOriginColumn());
        assertEquals(0, window.getOriginRow());
        assertEquals(0f, window.getOriginPixelsX(CELL));
        assertEquals(0f, window.getOriginPixelsY(CELL));
    }

    @Test
    void originPixelsAreTheOriginTimesTheCellSizeForEachLayer() {
        Layer small = stack.add(new TerminalGrid(COLUMNS, ROWS), new SquareSource(8));
        small.enableMapWindow();
        MapWindow window = enable();

        stack.setCamera(3 * CELL, 2 * CELL);

        assertEquals(3, window.getOriginColumn());
        assertEquals(6, small.getMapWindow().getOriginColumn());
        assertEquals(3 * CELL, layer.getMapOriginPixelsX());
        assertEquals(2 * CELL, layer.getMapOriginPixelsY());
        assertEquals(6 * 8, small.getMapOriginPixelsX());
        assertEquals(4 * 8, small.getMapOriginPixelsY());
        assertEquals(3 * CELL, window.getOriginPixelsX(CELL));
        assertEquals(4 * 8, small.getMapWindow().getOriginPixelsY(8));
    }

    @Test
    void setLandsAtGridPositionMinusOrigin() {
        stack.setCamera(48f, 32f);
        MapWindow window = enable();

        window.set(5, 3, 'x', 0x11223344, 0x55667788);

        assertEquals('x', grid.getGlyph(2, 1));
        assertEquals(0x11223344, grid.getForeground(2, 1));
        assertEquals(0x55667788, grid.getBackground(2, 1));
    }

    @Test
    void setOutsideTheWindowIsSkippedSilently() {
        MapWindow window = enable();

        window.set(-1, 0, 'x', 0, 0);
        window.set(0, -1, 'x', 0, 0);
        window.set(COLUMNS, 0, 'x', 0, 0);
        window.set(0, ROWS, 'x', 0, 0);

        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                assertEquals(TerminalGrid.EMPTY_GLYPH, grid.getGlyph(column, row));
            }
        }
    }

    @Test
    void containsMatchesTheGridInMapCoordinates() {
        stack.setCamera(16f, 0f);
        MapWindow window = enable();

        assertTrue(window.contains(1, 0));
        assertTrue(window.contains(COLUMNS, ROWS - 1));
        assertFalse(window.contains(0, 0));
        assertFalse(window.contains(COLUMNS + 1, 0));
        assertFalse(window.contains(1, ROWS));
    }

    @Test
    void cellReturnsTheHandleAtTheGridPosition() {
        stack.setCamera(32f, 16f);
        MapWindow window = enable();

        assertSame(layer.cell(1, 2), window.cell(3, 3));
    }

    @Test
    void cellOutsideTheWindowThrows() {
        stack.setCamera(32f, 16f);
        MapWindow window = enable();

        assertThrows(IndexOutOfBoundsException.class, () -> window.cell(1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> window.cell(2 + COLUMNS, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> window.cell(2, 1 + ROWS));
    }

    @Test
    void frameCellReturnsTheFrameHandleAtTheGridPosition() {
        stack.setCamera(32f, 16f);
        MapWindow window = enable();

        assertSame(layer.frameCell(1, 2), window.frameCell(3, 3));
        assertTrue(window.frameCell(3, 3).isFrameHandle());
    }

    @Test
    void frameCellOutsideTheWindowThrows() {
        stack.setCamera(32f, 16f);
        MapWindow window = enable();

        assertThrows(IndexOutOfBoundsException.class, () -> window.frameCell(1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> window.frameCell(2 + COLUMNS, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> window.frameCell(2, 1 + ROWS));
    }

    @Test
    void conversionsAreOffsetsWithoutBoundsChecks() {
        stack.setCamera(48f, 32f);
        MapWindow window = enable();

        assertEquals(-3, window.toGridColumn(0));
        assertEquals(98, window.toGridRow(100));
        assertEquals(53, window.toMapColumn(50));
        assertEquals(1, window.toMapRow(-1));
    }

    @Test
    void enablingFlagsEveryCellOnceInRowMajorOrder() {
        stack.setCamera(16f, 32f);
        MapWindow window = enable();

        assertEquals(rectangleCells(1, COLUMNS, 2, ROWS + 1), collectToFill(window));
    }

    @Test
    void queryingDoesNotClearTheFlags() {
        MapWindow window = enable();

        collectToFill(window);

        assertEquals(COLUMNS * ROWS, collectToFill(window).size());
    }

    @Test
    void markAllFilledClearsEveryFlag() {
        MapWindow window = enable();

        window.markAllFilled();

        assertTrue(collectToFill(window).isEmpty());
    }

    @Test
    void stepRightFlagsOnlyTheNewRightColumn() {
        MapWindow window = enable();
        window.markAllFilled();

        stack.setCamera(16f, 0f);

        assertEquals(columnCells(COLUMNS, 0, ROWS - 1), collectToFill(window));
    }

    @Test
    void twoStepsRightBeforeFillingFlagTheTwoRightColumns() {
        MapWindow window = enable();
        window.markAllFilled();

        stack.setCamera(16f, 0f);
        stack.setCamera(32f, 0f);

        assertEquals(rectangleCells(COLUMNS, COLUMNS + 1, 0, ROWS - 1), collectToFill(window));
    }

    @Test
    void unfilledFlagsMoveWithTheContent() {
        MapWindow window = enable();
        window.markAllFilled();
        stack.setCamera(16f, 0f);
        window.markAllFilled();
        stack.setCamera(0f, 0f);
        stack.setCamera(16f, 0f);
        stack.setCamera(32f, 0f);

        assertEquals(rectangleCells(COLUMNS, COLUMNS + 1, 0, ROWS - 1), collectToFill(window));
    }

    @Test
    void diagonalStepFlagsTheLShapeOncePerCell() {
        MapWindow window = enable();
        window.markAllFilled();

        stack.setCamera(16f, 16f);

        List<String> expected = new ArrayList<>();
        expected.addAll(columnCells(COLUMNS, 1, ROWS));
        for (int column = 1; column < COLUMNS; column++) {
            expected.add(column + "," + ROWS);
        }
        List<String> visited = collectToFill(window);
        assertEquals(COLUMNS + ROWS - 1, visited.size());
        assertEquals(sorted(expected), sorted(visited));
    }

    @Test
    void stepBackLeftFlagsTheLeftColumn() {
        stack.setCamera(16f, 0f);
        MapWindow window = enable();
        window.markAllFilled();

        stack.setCamera(0f, 0f);

        assertEquals(columnCells(0, 0, ROWS - 1), collectToFill(window));
    }

    @Test
    void stepUpAndDownFlagTheExposedRows() {
        stack.setCamera(0f, 16f);
        MapWindow window = enable();
        window.markAllFilled();

        stack.setCamera(0f, 0f);
        assertEquals(rectangleCells(0, COLUMNS - 1, 0, 0), collectToFill(window));

        window.markAllFilled();
        stack.setCamera(0f, 16f);
        assertEquals(rectangleCells(0, COLUMNS - 1, ROWS, ROWS), collectToFill(window));
    }

    @Test
    void jumpingFartherThanTheGridFlagsEverything() {
        MapWindow window = enable();
        window.markAllFilled();

        stack.setCamera(16f * 50, 0f);

        assertEquals(COLUMNS * ROWS, collectToFill(window).size());
    }

    @Test
    void visitorCanWriteThroughSet() {
        stack.setCamera(16f, 0f);
        MapWindow window = enable();

        window.forEachCellToFill((mapColumn, mapRow) -> window.set(mapColumn, mapRow, 'm', 0, 0));

        assertEquals('m', grid.getGlyph(0, 0));
        assertEquals('m', grid.getGlyph(COLUMNS - 1, ROWS - 1));
    }

    @Test
    void writingDoesNotClearFlags() {
        MapWindow window = enable();

        window.set(0, 0, 'a', 0, 0);

        assertEquals(COLUMNS * ROWS, collectToFill(window).size());
    }

    @Test
    void gridResizeFlagsEveryCell() {
        MapWindow window = enable();
        window.markAllFilled();

        grid.resize(8, 5);

        assertEquals(rectangleCells(0, 7, 0, 4), collectToFill(window));
    }

    @Test
    void gridResizeBeforeAScrollFlagsEveryCell() {
        MapWindow window = enable();
        window.markAllFilled();

        grid.resize(8, 5);
        stack.setCamera(16f, 0f);

        assertEquals(rectangleCells(1, 8, 0, 4), collectToFill(window));
    }

    @Test
    void gridResizeAfterAScrollKeepsTheOriginAndFlagsEveryCell() {
        stack.setCamera(48f, 32f);
        MapWindow window = enable();
        window.markAllFilled();
        stack.setCamera(64f, 32f);
        window.markAllFilled();

        grid.resize(8, 5);

        assertEquals(4, window.getOriginColumn());
        assertEquals(2, window.getOriginRow());
        assertEquals(rectangleCells(4, 11, 2, 6), collectToFill(window));
        assertTrue(window.contains(11, 6));
        assertFalse(window.contains(12, 2));
        assertFalse(window.contains(4, 7));
    }

    @Test
    void scrollAfterAResizeFlagsOnlyTheExposedColumn() {
        stack.setCamera(48f, 32f);
        MapWindow window = enable();
        window.markAllFilled();
        stack.setCamera(64f, 32f);
        window.markAllFilled();
        grid.resize(8, 5);
        window.markAllFilled();

        stack.setCamera(80f, 32f);

        assertEquals(columnCells(12, 2, 6), collectToFill(window));
    }

    @Test
    void disableDetachesTheWindow() {
        MapWindow window = enable();

        layer.disableMapWindow();

        assertDetached(window);
    }

    @Test
    void reenableDetachesTheOldWindowAndFlagsEverything() {
        MapWindow old = enable();
        old.markAllFilled();

        layer.enableMapWindow();

        assertDetached(old);
        assertNotSame(old, layer.getMapWindow());
        assertEquals(COLUMNS * ROWS, collectToFill(layer.getMapWindow()).size());
    }

    @Test
    void layerRemovalDetachesTheWindow() {
        MapWindow window = enable();

        layer.remove();

        assertDetached(window);
    }

    @Test
    void scrollThrowsInMapWindowModeAndWorksAfterDisable() {
        enable();
        grid.setGlyph(2, 0, 'a');

        assertThrows(IllegalStateException.class, () -> layer.scroll(1, 0));
        assertEquals('a', grid.getGlyph(2, 0));

        layer.disableMapWindow();
        layer.scroll(1, 0);

        assertEquals('a', grid.getGlyph(3, 0));
    }

    private static void assertDetached(MapWindow window) {
        assertThrows(IllegalStateException.class, window::getOriginColumn);
        assertThrows(IllegalStateException.class, window::getOriginRow);
        assertThrows(IllegalStateException.class, () -> window.contains(0, 0));
        assertThrows(IllegalStateException.class, () -> window.set(0, 0, 'a', 0, 0));
        assertThrows(IllegalStateException.class, () -> window.cell(0, 0));
        assertThrows(IllegalStateException.class, () -> window.toGridColumn(0));
        assertThrows(IllegalStateException.class, () -> window.toGridRow(0));
        assertThrows(IllegalStateException.class, () -> window.toMapColumn(0));
        assertThrows(IllegalStateException.class, () -> window.toMapRow(0));
        assertThrows(IllegalStateException.class, () -> window.forEachCellToFill((column, row) -> { }));
        assertThrows(IllegalStateException.class, window::markAllFilled);
    }
}
