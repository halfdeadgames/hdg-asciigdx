package com.github.halfdeadgames.asciigdx.workbench.paint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.preview.PreviewFit;
import com.github.halfdeadgames.asciigdx.workbench.preview.PreviewPanelSize;
import com.github.halfdeadgames.asciigdx.workbench.ui.GlyphSize;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaintLayoutTest {

    private static final List<Integer> OTHER_CELL_SIZES = List.of(8, 16, 32);

    @Test
    void panelsFillTheScreenWithoutOverlap() {
        PaintLayout.Result result =
                PaintLayout.compute(79, 41, 18, 24, new GlyphSize(8, 8), 24, true);
        assertEquals(19, result.grid().x());
        // row 0 is the menu bar, row 1 the breadcrumbs, row 2 stays blank; the panels start below
        // them
        assertEquals(3, PaintLayout.TOP_ROWS);
        assertEquals(PaintLayout.TOP_ROWS, result.grid().y());
        assertEquals(PaintLayout.TOP_ROWS, result.preview().y());
        assertEquals(41 - 1, result.messages().bottom());
        assertEquals(result.messages().y(), result.grid().bottom());
        assertEquals(result.messages().y(), result.belowPreview().bottom());
        assertEquals(result.preview().bottom(), result.belowPreview().y());
        assertEquals(79, result.preview().x() + result.preview().width());
        assertTrue(result.grid().x() + result.grid().width() < result.preview().x());
        assertEquals(result.grid().x(), result.messages().x());
    }

    @Test
    void largeCellsGetASmallerPreviewThatLeavesRoomForTheHelp() {
        PaintLayout.Result result =
                PaintLayout.compute(79, 41, 18, 24, new GlyphSize(32, 32), 24, true);
        int columnHeight = result.messages().y() - result.preview().y();
        assertTrue(result.preview().height() <= columnHeight * 40 / 100);
        assertTrue(result.belowPreview().height() >= 6);
    }

    @Test
    void tallNarrowCellsKeepTheLargestScaleThatFits() {
        PaintLayout.Result result =
                PaintLayout.compute(79, 41, 18, 24, new GlyphSize(8, 16), 24, true);
        assertEquals(PreviewPanelSize.height(16, 8, 24), result.preview().height());
    }

    @Test
    void narrowWindowsFitTheSplitWithoutOverlap() {
        PaintColumns.Split split = PaintColumns.split(1000, 24, 1, 8, 24);
        PaintLayout.Result result =
                PaintLayout.compute(1000 / 24, 30, split.sheetCells(), split.rightCells(),
                        new GlyphSize(8, 8), 24, true);
        assertEquals(split.editorCells(), result.grid().width());
        assertTrue(result.grid().x() + result.grid().width() < result.preview().x());
        assertTrue(result.belowPreview().height() >= 6);
    }

    @Test
    void minimumHeightHoldsTheEditorAndTheRightColumn() {
        // 21 and 27 rows, each including one row for the menu bar
        assertEquals(21 * 24, PaintLayout.minimumHeight(8, 24));
        assertEquals(27 * 24, PaintLayout.minimumHeight(32, 24));
        int minimumRows = PaintLayout.minimumHeight(8, 24) / 24;
        PaintLayout.Result result =
                PaintLayout.compute(60, minimumRows, 10, 16, new GlyphSize(8, 8), 24, true);
        assertTrue(result.belowPreview().bottom() <= result.messages().y());
        assertEquals(minimumRows - 1, result.messages().bottom());
    }

    @Test
    void theKeyBarComingAndGoingMovesOnlyTheMessagesBottomEdge() {
        for (int cellPixels : OTHER_CELL_SIZES) {
            for (int rows : new int[]{PaintLayout.minimumHeight(8, cellPixels) / cellPixels, 30, 41,
                    60}) {
                PaintLayout.Result shown =
                        PaintLayout.compute(
                                79, rows, 18, 24, new GlyphSize(8, 8), cellPixels, true);
                PaintLayout.Result hidden =
                        PaintLayout.compute(
                                79, rows, 18, 24, new GlyphSize(8, 8), cellPixels, false);
                assertEquals(shown.grid(), hidden.grid());
                assertEquals(shown.preview(), hidden.preview());
                assertEquals(shown.belowPreview(), hidden.belowPreview());
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
        int cellPixels = 24;
        for (int extraPixels : new int[]{0, 5, 23}) {
            int rows = 41;
            int worldHeight = rows * cellPixels + extraPixels;
            PaintLayout.Result shown =
                    PaintLayout.compute(79, rows, 18, 24, new GlyphSize(8, 8), cellPixels, true);
            PaintLayout.Result hidden =
                    PaintLayout.compute(79, rows, 18, 24, new GlyphSize(8, 8), cellPixels, false);
            assertEquals(shown.messages().bottom(),
                    PaintLayout.TOP_ROWS + PaintLayout.sheetRows(worldHeight, cellPixels, true));
            assertEquals(hidden.messages().bottom(),
                    PaintLayout.TOP_ROWS + PaintLayout.sheetRows(worldHeight, cellPixels, false));
            assertEquals(1,
                    PaintLayout.sheetRows(worldHeight, cellPixels, false) - PaintLayout.sheetRows(
                            worldHeight, cellPixels, true));
        }
    }

    @Test
    void theMinimumHeightStillFitsEveryPanelWithTheKeyBarShown() {
        for (int cellPixels : OTHER_CELL_SIZES) {
            for (int glyphHeight : new int[]{8, 16, 32}) {
                int minimum = PaintLayout.minimumHeight(glyphHeight, cellPixels);
                int rows = minimum / cellPixels;
                PaintLayout.Result result =
                        PaintLayout.compute(60, rows, 10, 16, new GlyphSize(8, glyphHeight),
                                cellPixels, true);
                assertTrue(result.belowPreview().bottom() <= result.messages().y());
                assertEquals(rows - 1, result.messages().bottom());
                assertEquals(PaintLayout.MESSAGES_HEIGHT, result.messages().height());
                assertTrue(PaintLayout.sheetRows(minimum, cellPixels, true) >= 3);
            }
        }
    }

    @Test
    void minimumHeightIsWholeCellsAndHoldsTheRightColumnAtEveryCellSize() {
        for (int cellPixels : OTHER_CELL_SIZES) {
            for (int glyphHeight : new int[]{8, 32}) {
                int minimum = PaintLayout.minimumHeight(glyphHeight, cellPixels);
                assertEquals(0, minimum % cellPixels);
                int smallestScale = PreviewFit.SCALES.get(0);
                int rightRows =
                        PreviewPanelSize.height(glyphHeight, smallestScale, cellPixels) + 6;
                assertTrue(minimum / cellPixels
                        >= PaintLayout.TOP_ROWS + rightRows + PaintLayout.MESSAGES_HEIGHT + 1);
            }
        }
    }

    @Test
    void panelsDoNotOverlapAtEveryCellSize() {
        for (int cellPixels : OTHER_CELL_SIZES) {
            PaintLayout.Result result =
                    PaintLayout.compute(79, 41, 18, 24, new GlyphSize(8, 8), cellPixels, true);
            assertEquals(19, result.grid().x());
            assertEquals(PaintLayout.TOP_ROWS, result.grid().y());
            assertEquals(41 - 1, result.messages().bottom());
            assertEquals(result.messages().y(), result.grid().bottom());
            assertEquals(result.preview().bottom(), result.belowPreview().y());
            assertEquals(79, result.preview().x() + result.preview().width());
            assertTrue(result.grid().x() + result.grid().width() < result.preview().x());
            assertTrue(result.belowPreview().height() >= 6);
        }
    }
}
