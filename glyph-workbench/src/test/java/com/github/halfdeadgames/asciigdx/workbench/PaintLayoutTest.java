package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PaintLayoutTest {

    private static final int[] OTHER_CELL_SIZES = {8, 16, 32};

    @Test
    void panelsFillTheScreenWithoutOverlap() {
        PaintLayout.Result result = PaintLayout.compute(79, 41, 18, 24, 8, 8, 24);
        assertEquals(19, result.grid().x());
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
        PaintLayout.Result result = PaintLayout.compute(79, 41, 18, 24, 32, 32, 24);
        int columnHeight = result.messages().y() - result.preview().y();
        assertTrue(result.preview().height() <= columnHeight * 40 / 100);
        assertTrue(result.note().height() >= 6);
    }

    @Test
    void tallNarrowCellsKeepTheLargestScaleThatFits() {
        PaintLayout.Result result = PaintLayout.compute(79, 41, 18, 24, 8, 16, 24);
        assertEquals(PaintLayout.previewHeight(16, 8, 24), result.preview().height());
    }

    @Test
    void narrowWindowsFitTheSplitWithoutOverlap() {
        PaintColumns.Split split = PaintColumns.split(1000, 24, 1, 8, 24);
        PaintLayout.Result result = PaintLayout.compute(1000 / 24, 30, split.sheetCells(), split.rightCells(), 8, 8, 24);
        assertEquals(split.editorCells(), result.grid().width());
        assertTrue(result.grid().x() + result.grid().width() < result.preview().x());
        assertTrue(result.note().height() >= 6);
    }

    @Test
    void minimumHeightHoldsTheEditorAndTheRightColumn() {
        assertEquals(18 * 24, PaintLayout.minimumHeight(8, 24));
        assertEquals(24 * 24, PaintLayout.minimumHeight(32, 24));
        PaintLayout.Result result = PaintLayout.compute(60, 18, 10, 16, 8, 8, 24);
        assertTrue(result.note().bottom() <= result.messages().y());
        assertEquals(18 - 1, result.messages().bottom());
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
                assertTrue(minimum / cell >= 2 + rightRows + PaintLayout.MESSAGES_HEIGHT + 1);
            }
        }
    }

    @Test
    void panelsDoNotOverlapAtEveryCellSize() {
        for (int cell : OTHER_CELL_SIZES) {
            PaintLayout.Result result = PaintLayout.compute(79, 41, 18, 24, 8, 8, cell);
            assertEquals(19, result.grid().x());
            assertEquals(41 - 1, result.messages().bottom());
            assertEquals(result.messages().y(), result.grid().bottom());
            assertEquals(result.preview().bottom(), result.note().y());
            assertEquals(79, result.preview().x() + result.preview().width());
            assertTrue(result.grid().x() + result.grid().width() < result.preview().x());
            assertTrue(result.note().height() >= 6);
        }
    }
}
