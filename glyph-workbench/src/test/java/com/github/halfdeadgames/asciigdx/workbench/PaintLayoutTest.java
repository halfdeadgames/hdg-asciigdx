package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PaintLayoutTest {

    @Test
    void panelsFillTheScreenWithoutOverlap() {
        PaintLayout.Result result = PaintLayout.compute(79, 41, 18, 24, 8, 8);
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
        assertEquals(8, PaintLayout.previewHeight(8, 8));
        assertTrue(PaintLayout.previewHeight(32, 8) > PaintLayout.previewHeight(16, 8));
        // tab row, gap, label row, and the 8x block must fit inside the borders
        for (int height : new int[] {8, 16, 24, 32, 40}) {
            int content = 24 + 12 + 24 + 8 * height;
            assertTrue((PaintLayout.previewHeight(height, 8) - 2) * 24 >= content);
        }
    }

    @Test
    void largeCellsGetASmallerPreviewThatLeavesRoomForTheHelp() {
        PaintLayout.Result result = PaintLayout.compute(79, 41, 18, 24, 32, 32);
        int columnHeight = result.messages().y() - result.preview().y();
        assertTrue(result.preview().height() <= columnHeight * 40 / 100);
        assertTrue(result.note().height() >= 6);
    }

    @Test
    void tallNarrowCellsKeepTheLargestScaleThatFits() {
        PaintLayout.Result result = PaintLayout.compute(79, 41, 18, 24, 8, 16);
        assertEquals(PaintLayout.previewHeight(16, 8), result.preview().height());
    }

    @Test
    void narrowWindowsFitTheSplitWithoutOverlap() {
        PaintColumns.Split split = PaintColumns.split(1000, 24, 1, 8);
        PaintLayout.Result result = PaintLayout.compute(1000 / 24, 30, split.sheetCells(), split.rightCells(), 8, 8);
        assertEquals(split.editorCells(), result.grid().width());
        assertTrue(result.grid().x() + result.grid().width() < result.preview().x());
        assertTrue(result.note().height() >= 6);
    }

    @Test
    void minimumHeightHoldsTheEditorAndTheRightColumn() {
        assertEquals(18 * 24, PaintLayout.minimumHeight(8));
        assertEquals(24 * 24, PaintLayout.minimumHeight(32));
        PaintLayout.Result result = PaintLayout.compute(60, 18, 10, 16, 8, 8);
        assertTrue(result.note().bottom() <= result.messages().y());
        assertEquals(18 - 1, result.messages().bottom());
    }
}
