package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SheetGridLayoutTest {

    // Panel at (10, 20), 24 px cells, 1 px gaps, 16 x 39 cells: cell 0 spans x 11..34, y 21..44.
    private final SheetGridLayout layout = new SheetGridLayout(10, 20, 24, 1, 16, 39);

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
    void outsideThePanelMapsToNone() {
        assertEquals(-1, layout.cellAt(-5, -5));
        assertEquals(-1, layout.cellAt(5, 100));
        assertEquals(-1, layout.cellAt(10 + 401, 100));
        assertEquals(-1, layout.cellAt(100, 20 + 976));
    }
}
