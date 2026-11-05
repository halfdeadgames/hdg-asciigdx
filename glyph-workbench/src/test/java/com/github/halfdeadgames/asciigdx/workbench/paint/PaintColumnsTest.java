package com.github.halfdeadgames.asciigdx.workbench.paint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.ui.Panel;
import org.junit.jupiter.api.Test;

class PaintColumnsTest {

    private static final int[] WIDTHS = {1900, 1500, 1280, 1000};
    // slot width, glyph width
    private static final int[][] FONTS = {{24, 8}, {16, 8}, {32, 32}};

    private static PaintColumns.Split split(int windowWidth, int[] font) {
        return PaintColumns.split(windowWidth, font[0], PaintSheetView.GAP, font[1], 24);
    }

    private static void assertShrinkOrder(PaintColumns.Split split, int comfort) {
        if (split.sheetColumns() > 8) {
            assertEquals(24, split.rightCells());
        }
        if (split.rightCells() < 24) {
            assertEquals(4, split.sheetColumns());
        }
        if (split.editorCells() < comfort) {
            assertTrue(split.sheetColumns() <= 8);
        }
    }

    @Test
    void wideWindowKeepsEverythingAsBefore() {
        for (int[] font : FONTS) {
            PaintColumns.Split split = split(1900, font);
            assertEquals(16, split.sheetColumns());
            assertEquals(24, split.rightCells());
        }
        PaintColumns.Split plain = split(1900, FONTS[0]);
        assertEquals(20, plain.sheetCells());
        assertEquals(33, plain.editorCells());
    }

    @Test
    void sheetShrinksFirst() {
        PaintColumns.Split split = split(1500, FONTS[0]);
        assertEquals(12, split.sheetColumns());
        assertEquals(24, split.rightCells());
        assertEquals(20, split.editorCells());
    }

    @Test
    void editorShrinksBelowComfortBeforeTheRightColumnDoes() {
        PaintColumns.Split split = split(1280, FONTS[0]);
        assertEquals(8, split.sheetColumns());
        assertEquals(24, split.rightCells());
        assertEquals(15, split.editorCells());
    }

    @Test
    void sheetThenRightColumnShrinkOnceTheEditorIsAtItsFloor() {
        PaintColumns.Split split = split(1000, FONTS[0]);
        assertEquals(4, split.sheetColumns());
        assertEquals(20, split.rightCells());
        assertEquals(11, split.editorCells());
        PaintColumns.Split narrow = split(1128, FONTS[0]);
        assertEquals(6, narrow.sheetColumns());
        assertEquals(24, narrow.rightCells());
        assertEquals(11, narrow.editorCells());
    }

    @Test
    void atTheRoomyWidthTheSheetHasEightColumnsTheRightColumnIsFullAndTheEditorIsAtItsFloor() {
        for (int cell : new int[] {8, 16, 24, 32}) {
            int roomy = PaintColumns.roomyWidth(cell, PaintSheetView.GAP, 8, cell);
            PaintColumns.Split atRoomy = PaintColumns.split(roomy, cell, PaintSheetView.GAP, 8, cell);
            assertEquals(8, atRoomy.sheetColumns());
            assertEquals(24, atRoomy.rightCells());
            assertEquals(PaintColumns.editorFloorCells(8, cell), atRoomy.editorCells());
            PaintColumns.Split narrower = PaintColumns.split(roomy - cell, cell, PaintSheetView.GAP, 8, cell);
            assertTrue(narrower.sheetColumns() < 8);
            assertEquals(24, narrower.rightCells());
            assertTrue(narrower.editorCells() >= PaintColumns.editorFloorCells(8, cell));
        }
    }

    @Test
    void everyFontKeepsTheShrinkOrderMinimumsAndNoOverlap() {
        for (int[] font : FONTS) {
            int comfort = PaintColumns.editorComfortCells(font[1], 24);
            int floor = PaintColumns.editorFloorCells(font[1], 24);
            int minimumWidth = PaintColumns.minimumWidth(font[0], PaintSheetView.GAP, font[1], 24);
            for (int width : WIDTHS) {
                PaintColumns.Split split = split(width, font);
                assertTrue(split.sheetColumns() >= 4 && split.sheetColumns() <= 16);
                assertTrue(split.rightCells() >= 16 && split.rightCells() <= 24);
                assertShrinkOrder(split, comfort);
                if (width >= minimumWidth) {
                    assertTrue(split.editorCells() >= floor);
                    // sheet frame, a gap cell on each side of the editor, and the right column
                    assertTrue(split.sheetCells() + 1 + split.editorCells() + 1 + split.rightCells() <= width / 24);
                }
            }
        }
    }

    @Test
    void sheetFrameFitsGlyphColumnsAndScrollBar() {
        // 24 + 1 px per column: 4 columns are 101 px, plus a bar cell, rounded up to whole cells, plus two borders
        assertEquals(8, PaintColumns.sheetCells(4, 24, PaintSheetView.GAP, 24));
        assertEquals(20, PaintColumns.sheetCells(16, 24, PaintSheetView.GAP, 24));
        assertEquals(6, PaintColumns.sheetCells(4, 16, PaintSheetView.GAP, 24));
    }

    @Test
    void leftoverPixelsSplitEvenlyAroundThePanel() {
        assertEquals(9, PaintColumns.panelInset(8, 4 * 25 + 1, 24));
        assertEquals(0, PaintColumns.panelInset(6, 4 * 17 + 1 + 100, 24));
    }

    @Test
    void sheetFrameAndInsetFollowTheCellSize() {
        // slots one cell wide: four columns plus a bar cell round up to the same six cells plus borders
        assertEquals(8, PaintColumns.sheetCells(4, 8, PaintSheetView.GAP, 8));
        assertEquals(8, PaintColumns.sheetCells(4, 16, PaintSheetView.GAP, 16));
        assertEquals(8, PaintColumns.sheetCells(4, 32, PaintSheetView.GAP, 32));
        assertEquals(1, PaintColumns.panelInset(8, 4 * 9 + 1, 8));
        assertEquals(5, PaintColumns.panelInset(8, 4 * 17 + 1, 16));
        assertEquals(13, PaintColumns.panelInset(8, 4 * 33 + 1, 32));
    }

    @Test
    void editorFloorTracksTheCellSizeButNotTheZoom() {
        assertEquals(16, PaintColumns.editorFloorCells(8, 8));
        assertEquals(12, PaintColumns.editorFloorCells(8, 16));
        assertEquals(10, PaintColumns.editorFloorCells(8, 32));
        assertEquals(20, PaintColumns.editorComfortCells(8, 8));
        assertEquals(20, PaintColumns.editorComfortCells(8, 32));
    }

    @Test
    void otherCellSizesKeepTheShrinkOrderAndNoOverlap() {
        for (int cell : new int[] {8, 16, 32}) {
            int floor = PaintColumns.editorFloorCells(8, cell);
            int minimumWidth = PaintColumns.minimumWidth(cell, PaintSheetView.GAP, 8, cell);
            for (int cells : new int[] {79, 62, 50, 42}) {
                PaintColumns.Split split = PaintColumns.split(cells * cell, cell, PaintSheetView.GAP, 8, cell);
                assertTrue(split.sheetColumns() >= 4 && split.sheetColumns() <= 16);
                assertTrue(split.rightCells() >= 16 && split.rightCells() <= 24);
                assertShrinkOrder(split, PaintColumns.editorComfortCells(8, cell));
                if (cells * cell >= minimumWidth) {
                    assertTrue(split.editorCells() >= floor);
                    assertTrue(split.sheetCells() + 1 + split.editorCells() + 1 + split.rightCells() <= cells);
                }
            }
        }
    }

    @Test
    void titleAlwaysShowsOnAVisibleFrameShortenedWhenNarrow() {
        assertEquals(" SHEET ", Panel.titleLabel(11, PaintColumns.sheetTitle(11)));
        assertEquals(" SHEE ", Panel.titleLabel(10, PaintColumns.sheetTitle(10)));
        assertEquals(" SH ", Panel.titleLabel(8, PaintColumns.sheetTitle(8)));
        for (int cell : new int[] {8, 16, 24, 32}) {
            int narrowest = PaintColumns.sheetCells(PaintColumns.MIN_SHEET_COLUMNS, cell, PaintSheetView.GAP, cell);
            assertTrue(!Panel.titleLabel(narrowest, PaintColumns.sheetTitle(narrowest)).isEmpty());
        }
    }

    @Test
    void minimumWidthHoldsAllThreeAtTheirMinimum() {
        assertEquals(37 * 24, PaintColumns.minimumWidth(24, PaintSheetView.GAP, 8, 24));
        assertEquals(46 * 24, PaintColumns.minimumWidth(32, PaintSheetView.GAP, 32, 24));
        for (int[] font : FONTS) {
            int minimum = PaintColumns.minimumWidth(font[0], PaintSheetView.GAP, font[1], 24);
            PaintColumns.Split atMinimum = PaintColumns.split(minimum, font[0], PaintSheetView.GAP, font[1], 24);
            assertEquals(4, atMinimum.sheetColumns());
            assertEquals(16, atMinimum.rightCells());
            assertEquals(PaintColumns.editorFloorCells(font[1], 24), atMinimum.editorCells());
            PaintColumns.Split narrower = PaintColumns.split(minimum - 24, font[0], PaintSheetView.GAP, font[1], 24);
            assertTrue(narrower.editorCells() < PaintColumns.editorFloorCells(font[1], 24));
        }
    }

    @Test
    void minimumWidthScalesWithTheCellSize() {
        assertEquals(42 * 8, PaintColumns.minimumWidth(8, PaintSheetView.GAP, 8, 8));
        assertEquals(38 * 16, PaintColumns.minimumWidth(16, PaintSheetView.GAP, 8, 16));
        assertEquals(36 * 32, PaintColumns.minimumWidth(32, PaintSheetView.GAP, 8, 32));
        for (int cell : new int[] {8, 16, 32}) {
            int minimum = PaintColumns.minimumWidth(cell, PaintSheetView.GAP, 8, cell);
            PaintColumns.Split atMinimum = PaintColumns.split(minimum, cell, PaintSheetView.GAP, 8, cell);
            assertEquals(4, atMinimum.sheetColumns());
            assertEquals(16, atMinimum.rightCells());
            assertEquals(PaintColumns.editorFloorCells(8, cell), atMinimum.editorCells());
            PaintColumns.Split narrower = PaintColumns.split(minimum - cell, cell, PaintSheetView.GAP, 8, cell);
            assertTrue(narrower.editorCells() < PaintColumns.editorFloorCells(8, cell));
        }
    }
}
