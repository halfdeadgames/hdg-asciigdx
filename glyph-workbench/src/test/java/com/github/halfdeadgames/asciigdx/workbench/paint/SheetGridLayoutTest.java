package com.github.halfdeadgames.asciigdx.workbench.paint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.halfdeadgames.asciigdx.workbench.ui.GlyphSize;
import org.junit.jupiter.api.Test;

class SheetGridLayoutTest {

    // Panel at (10, 20), 24 px cells, 1 px gaps, 16 x 39 cells: cell 0 spans x 11..34, y 21..44.
    private final SheetGridLayout layout = new SheetGridLayout(10, 20, new GlyphSize(24, 24), 1, 16, 39);

    @Test
    void computesPanelSize() {
        assertEquals(401, layout.panelWidth());
        assertEquals(976, layout.panelHeight());
    }

    @Test
    void computesCellOrigins() {
        assertEquals(11, layout.cellLeft(0));
        assertEquals(21, layout.cellTop(0));
        assertEquals(36, layout.cellLeft(1));
        assertEquals(46, layout.cellTop(16));
    }

    @Test
    void mapsPixelsInsideCells() {
        assertEquals(0, layout.cellAt(11, 21));
        assertEquals(0, layout.cellAt(34, 44));
        assertEquals(1, layout.cellAt(36, 21));
        assertEquals(16, layout.cellAt(11, 46));
        assertEquals(16 * 39 - 1, layout.cellAt(10 + 400 - 1, 20 + 975 - 1));
    }

    @Test
    void gapsMapToNone() {
        assertEquals(-1, layout.cellAt(10, 21));
        assertEquals(-1, layout.cellAt(35, 21));
        assertEquals(-1, layout.cellAt(11, 20));
        assertEquals(-1, layout.cellAt(11, 45));
    }

    @Test
    void scrollOffsetShiftsMappingAndOrigins() {
        assertEquals(21, layout.cellTop(48, 3));
        assertEquals(48, layout.cellAt(11, 21, 3, 10));
        assertEquals(3 * 16 + 16, layout.cellAt(11, 46, 3, 10));
        assertEquals(12 * 16, layout.cellAt(11, 21 + 9 * 25, 3, 10));
    }

    @Test
    void rowsBelowTheVisibleWindowMapToNone() {
        assertEquals(-1, layout.cellAt(11, 21 + 10 * 25, 3, 10));
        assertEquals(-1, layout.cellAt(11, 21 + 25, 38, 10));
    }

    @Test
    void computesVisibleRowFit() {
        assertEquals(35, layout.rowsThatFit(876));
        assertEquals(34, layout.rowsThatFit(875));
        assertEquals(1, layout.rowsThatFit(0));
        assertEquals(876, layout.heightForRows(35));
    }

    @Test
    void nonSquareCellsUseSeparatePitchPerAxis() {
        // Panel at (10, 20), 16x32 cells, 1 px gaps, 4 x 3 cells: x pitch 17, y pitch 33.
        SheetGridLayout tall = new SheetGridLayout(10, 20, new GlyphSize(16, 32), 1, 4, 3);
        assertEquals(69, tall.panelWidth());
        assertEquals(100, tall.panelHeight());
        assertEquals(28, tall.cellLeft(1));
        assertEquals(54, tall.cellTop(4));
        assertEquals(0, tall.cellAt(11, 21));
        assertEquals(0, tall.cellAt(26, 52));
        assertEquals(5, tall.cellAt(28, 54));
        assertEquals(11, tall.cellAt(10 + 68 - 1, 20 + 99 - 1));
        assertEquals(-1, tall.cellAt(27, 21));
        assertEquals(-1, tall.cellAt(11, 53));
        assertEquals(-1, tall.cellAt(11, 20 + 100));
        assertEquals(-1, tall.cellAt(10 + 69, 21));
        assertEquals(3, tall.rowsThatFit(100));
        assertEquals(2, tall.rowsThatFit(98));
        assertEquals(100, tall.heightForRows(3));
    }

    @Test
    void horizontalOffsetShiftsMappingAndOrigins() {
        // 6 of 16 columns shown, first shown column 5, no row scroll.
        SheetGridLayout window = new SheetGridLayout(10, 20, new GlyphSize(24, 24), 1, 16, 6, 39);
        assertEquals(11, window.cellLeft(5, 5));
        assertEquals(36, window.cellLeft(6, 5));
        assertEquals(5, window.cellAt(11, 21, 0, 10, 5));
        assertEquals(16 + 7, window.cellAt(11 + 2 * 25, 46, 0, 10, 5));
        assertEquals(10, window.cellAt(11 + 5 * 25, 21, 0, 10, 5));
    }

    @Test
    void columnsOutsideTheHorizontalWindowMapToNone() {
        SheetGridLayout window = new SheetGridLayout(10, 20, new GlyphSize(24, 24), 1, 16, 6, 39);
        assertEquals(-1, window.cellAt(11 + 6 * 25, 21, 0, 10, 5));
        assertEquals(15, window.cellAt(11 + 5 * 25, 21, 0, 10, 10));
        assertEquals(-1, window.cellAt(11 + 6 * 25, 21, 0, 10, 10));
    }

    @Test
    void outsideThePanelMapsToNone() {
        assertEquals(-1, layout.cellAt(-5, -5));
        assertEquals(-1, layout.cellAt(5, 100));
        assertEquals(-1, layout.cellAt(10 + 401, 100));
        assertEquals(-1, layout.cellAt(100, 20 + 976));
    }
}
