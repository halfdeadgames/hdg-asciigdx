package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PaintColumnsTest {

    private static final int[] WIDTHS = {1900, 1500, 1280, 1000};
    // slot width, glyph width
    private static final int[][] FONTS = {{24, 8}, {16, 8}, {32, 32}};

    private static PaintColumns.Split split(int windowWidth, int[] font) {
        return PaintColumns.split(windowWidth, font[0], PaintSheetView.GAP, font[1]);
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
    void rightColumnShrinksOnceTheSheetIsAtItsMinimum() {
        PaintColumns.Split split = split(1280, FONTS[0]);
        assertEquals(4, split.sheetColumns());
        assertEquals(23, split.rightCells());
        assertEquals(20, split.editorCells());
    }

    @Test
    void editorShrinksLast() {
        PaintColumns.Split split = split(1000, FONTS[0]);
        assertEquals(4, split.sheetColumns());
        assertEquals(16, split.rightCells());
        assertEquals(15, split.editorCells());
    }

    @Test
    void everyFontKeepsTheShrinkOrderMinimumsAndNoOverlap() {
        for (int[] font : FONTS) {
            int comfort = PaintColumns.editorComfortCells(font[1]);
            int floor = PaintColumns.editorFloorCells(font[1]);
            int minimumWidth = PaintColumns.minimumWidth(font[0], PaintSheetView.GAP, font[1]);
            for (int width : WIDTHS) {
                PaintColumns.Split split = split(width, font);
                assertTrue(split.sheetColumns() >= 4 && split.sheetColumns() <= 16);
                assertTrue(split.rightCells() >= 16 && split.rightCells() <= 24);
                if (split.sheetColumns() > 4) {
                    assertEquals(24, split.rightCells());
                }
                if (split.rightCells() < 24) {
                    assertEquals(4, split.sheetColumns());
                }
                if (split.editorCells() < comfort) {
                    assertEquals(4, split.sheetColumns());
                    assertEquals(16, split.rightCells());
                }
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
        assertEquals(8, PaintColumns.sheetCells(4, 24, PaintSheetView.GAP));
        assertEquals(20, PaintColumns.sheetCells(16, 24, PaintSheetView.GAP));
        assertEquals(6, PaintColumns.sheetCells(4, 16, PaintSheetView.GAP));
    }

    @Test
    void leftoverPixelsSplitEvenlyAroundThePanel() {
        assertEquals(9, PaintColumns.panelInset(8, 4 * 25 + 1));
        assertEquals(0, PaintColumns.panelInset(6, 4 * 17 + 1 + 100));
    }

    @Test
    void titleShowsWholeOrNotAtAll() {
        assertEquals("SHEET", PaintColumns.sheetTitle(11));
        assertEquals("", PaintColumns.sheetTitle(10));
        assertEquals("", PaintColumns.sheetTitle(8));
    }

    @Test
    void minimumWidthHoldsAllThreeAtTheirMinimum() {
        assertEquals(37 * 24, PaintColumns.minimumWidth(24, PaintSheetView.GAP, 8));
        assertEquals(46 * 24, PaintColumns.minimumWidth(32, PaintSheetView.GAP, 32));
        for (int[] font : FONTS) {
            int minimum = PaintColumns.minimumWidth(font[0], PaintSheetView.GAP, font[1]);
            PaintColumns.Split atMinimum = PaintColumns.split(minimum, font[0], PaintSheetView.GAP, font[1]);
            assertEquals(4, atMinimum.sheetColumns());
            assertEquals(16, atMinimum.rightCells());
            assertEquals(PaintColumns.editorFloorCells(font[1]), atMinimum.editorCells());
            PaintColumns.Split narrower = PaintColumns.split(minimum - 24, font[0], PaintSheetView.GAP, font[1]);
            assertTrue(narrower.editorCells() < PaintColumns.editorFloorCells(font[1]));
        }
    }
}
