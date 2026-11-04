package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PaintLayoutTest {

    private static final int[] OTHER_CELL_SIZES = {8, 16, 32};

    @Test
    void panelsFillTheScreenWithoutOverlap() {
        PaintLayout.Result result = PaintLayout.compute(79, 41, 18, 24, 8, 8, 24, true);
        assertEquals(19, result.grid().x());
        // row 0 is the menu bar, row 1 the breadcrumbs, row 2 stays blank; the panels start below them
        assertEquals(3, PaintLayout.TOP_ROWS);
        assertEquals(PaintLayout.TOP_ROWS, result.grid().y());
        assertEquals(PaintLayout.TOP_ROWS, result.preview().y());
        assertEquals(41 - 1, result.messages().bottom());
        assertEquals(result.messages().y(), result.grid().bottom());
        assertEquals(result.messages().y(), result.note().bottom());
        assertEquals(result.preview().bottom(), result.note().y());
        assertEquals(79, result.preview().x() + result.preview().width());
        assertTrue(result.grid().x() + result.grid().width() < result.preview().x());
        assertEquals(result.grid().x(), result.messages().x());
    }

    @Test
    void previewHeightGrowsWithTheGlyphHeight() {
        assertEquals(8, PaintLayout.previewHeight(8, 8, 24));
        assertTrue(PaintLayout.previewHeight(32, 8, 24) > PaintLayout.previewHeight(16, 8, 24));
        // tab row, gap, label row, and the 8x block must fit inside the borders
        for (int height : new int[] {8, 16, 24, 32, 40}) {
            int content = 24 + 12 + 24 + 8 * height;
            assertTrue((PaintLayout.previewHeight(height, 8, 24) - 2) * 24 >= content);
        }
    }

    @Test
    void largeCellsGetASmallerPreviewThatLeavesRoomForTheHelp() {
        PaintLayout.Result result = PaintLayout.compute(79, 41, 18, 24, 32, 32, 24, true);
        int columnHeight = result.messages().y() - result.preview().y();
        assertTrue(result.preview().height() <= columnHeight * 40 / 100);
        assertTrue(result.note().height() >= 6);
    }

    @Test
    void tallNarrowCellsKeepTheLargestScaleThatFits() {
        PaintLayout.Result result = PaintLayout.compute(79, 41, 18, 24, 8, 16, 24, true);
        assertEquals(PaintLayout.previewHeight(16, 8, 24), result.preview().height());
    }

    @Test
    void narrowWindowsFitTheSplitWithoutOverlap() {
        PaintColumns.Split split = PaintColumns.split(1000, 24, 1, 8, 24);
        PaintLayout.Result result = PaintLayout.compute(1000 / 24, 30, split.sheetCells(), split.rightCells(), 8, 8, 24, true);
        assertEquals(split.editorCells(), result.grid().width());
        assertTrue(result.grid().x() + result.grid().width() < result.preview().x());
        assertTrue(result.note().height() >= 6);
    }

    @Test
    void minimumHeightHoldsTheEditorAndTheRightColumn() {
        // 21 and 27 rows, each including one row for the menu bar
        assertEquals(21 * 24, PaintLayout.minimumHeight(8, 24));
        assertEquals(27 * 24, PaintLayout.minimumHeight(32, 24));
        int minimumRows = PaintLayout.minimumHeight(8, 24) / 24;
        PaintLayout.Result result = PaintLayout.compute(60, minimumRows, 10, 16, 8, 8, 24, true);
        assertTrue(result.note().bottom() <= result.messages().y());
        assertEquals(minimumRows - 1, result.messages().bottom());
    }

    @Test
    void theKeyBarComingAndGoingMovesOnlyTheMessagesBottomEdge() {
        for (int cell : OTHER_CELL_SIZES) {
            for (int rows : new int[] {PaintLayout.minimumHeight(8, cell) / cell, 30, 41, 60}) {
                PaintLayout.Result shown = PaintLayout.compute(79, rows, 18, 24, 8, 8, cell, true);
                PaintLayout.Result hidden = PaintLayout.compute(79, rows, 18, 24, 8, 8, cell, false);
                assertEquals(shown.grid(), hidden.grid());
                assertEquals(shown.preview(), hidden.preview());
                assertEquals(shown.note(), hidden.note());
                assertEquals(PaintLayout.helpArea(shown, true), PaintLayout.helpArea(hidden, true));
                assertEquals(PaintLayout.noteArea(shown), PaintLayout.noteArea(hidden));
                assertEquals(shown.messages().x(), hidden.messages().x());
                assertEquals(shown.messages().y(), hidden.messages().y());
                assertEquals(shown.messages().width(), hidden.messages().width());
                assertEquals(rows - 1, shown.messages().bottom());
                assertEquals(rows, hidden.messages().bottom());
            }
        }
    }

    @Test
    void theSheetReachesTheBottomRowExactlyWhenTheKeyBarIsHidden() {
        int cell = 24;
        for (int extraPixels : new int[] {0, 5, 23}) {
            int rows = 41;
            int worldHeight = rows * cell + extraPixels;
            PaintLayout.Result shown = PaintLayout.compute(79, rows, 18, 24, 8, 8, cell, true);
            PaintLayout.Result hidden = PaintLayout.compute(79, rows, 18, 24, 8, 8, cell, false);
            assertEquals(shown.messages().bottom(), PaintLayout.TOP_ROWS + PaintLayout.sheetRows(worldHeight, cell, true));
            assertEquals(hidden.messages().bottom(), PaintLayout.TOP_ROWS + PaintLayout.sheetRows(worldHeight, cell, false));
            assertEquals(1, PaintLayout.sheetRows(worldHeight, cell, false) - PaintLayout.sheetRows(worldHeight, cell, true));
        }
    }

    @Test
    void theMinimumHeightStillFitsEveryPanelWithTheKeyBarShown() {
        for (int cell : OTHER_CELL_SIZES) {
            for (int glyphHeight : new int[] {8, 16, 32}) {
                int minimum = PaintLayout.minimumHeight(glyphHeight, cell);
                int rows = minimum / cell;
                PaintLayout.Result result = PaintLayout.compute(60, rows, 10, 16, 8, glyphHeight, cell, true);
                assertTrue(result.note().bottom() <= result.messages().y());
                assertEquals(rows - 1, result.messages().bottom());
                assertEquals(PaintLayout.MESSAGES_HEIGHT, result.messages().height());
                assertTrue(PaintLayout.sheetRows(minimum, cell, true) >= 3);
            }
        }
    }

    @Test
    void previewHeightFitsItsContentAtEveryCellSize() {
        for (int cell : OTHER_CELL_SIZES) {
            for (int height : new int[] {8, 16, 32}) {
                int content = cell + cell / 2 + cell + 8 * height;
                int rows = PaintLayout.previewHeight(height, 8, cell);
                assertTrue((rows - 2) * cell >= content);
                assertTrue((rows - 3) * cell < content);
            }
        }
    }

    @Test
    void previewBudgetAndInnerWidthScaleWithTheCellSize() {
        for (int cell : OTHER_CELL_SIZES) {
            assertEquals(21 * cell, PaintLayout.previewInnerWidth(24, cell));
            int rows = PaintLayout.previewHeight(8, 8, cell);
            assertTrue(PaintLayout.previewBlockBudget(rows, cell) >= 8 * 8);
            assertTrue(PaintLayout.previewBlockBudget(rows - 1, cell) < 8 * 8);
        }
    }

    @Test
    void minimumHeightIsWholeCellsAndHoldsTheRightColumnAtEveryCellSize() {
        for (int cell : OTHER_CELL_SIZES) {
            for (int glyphHeight : new int[] {8, 32}) {
                int minimum = PaintLayout.minimumHeight(glyphHeight, cell);
                assertEquals(0, minimum % cell);
                int rightRows = PaintLayout.previewHeight(glyphHeight, PreviewFit.SCALES[0], cell) + 6;
                assertTrue(minimum / cell >= PaintLayout.TOP_ROWS + rightRows + PaintLayout.MESSAGES_HEIGHT + 1);
            }
        }
    }

    @Test
    void panelsDoNotOverlapAtEveryCellSize() {
        for (int cell : OTHER_CELL_SIZES) {
            PaintLayout.Result result = PaintLayout.compute(79, 41, 18, 24, 8, 8, cell, true);
            assertEquals(19, result.grid().x());
            assertEquals(PaintLayout.TOP_ROWS, result.grid().y());
            assertEquals(41 - 1, result.messages().bottom());
            assertEquals(result.messages().y(), result.grid().bottom());
            assertEquals(result.preview().bottom(), result.note().y());
            assertEquals(79, result.preview().x() + result.preview().width());
            assertTrue(result.grid().x() + result.grid().width() < result.preview().x());
            assertTrue(result.note().height() >= 6);
        }
    }
}
