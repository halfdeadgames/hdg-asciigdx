package com.github.halfdeadgames.asciigdx.workbench.paint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.halfdeadgames.asciigdx.workbench.ui.GlyphSize;
import org.junit.jupiter.api.Test;

class SheetGridLayoutTest {

    // Panel at (10, 20), 24 px slots, 1 px gaps, 16 x 39 slots: slot 0 spans x 11..34, y 21..44.
    private final SheetGridLayout layout = new SheetGridLayout(10, 20, new GlyphSize(24, 24), 1, 16, 16, 39);

    @Test
    void computesPanelSize() {
        assertEquals(401, layout.panelWidth());
        assertEquals(976, layout.panelHeight());
    }

    @Test
    void computesCellOrigins() {
        assertEquals(11, layout.slotLeft(0, 0));
        assertEquals(21, layout.slotTop(0, 0));
        assertEquals(36, layout.slotLeft(1, 0));
        assertEquals(46, layout.slotTop(16, 0));
    }

    @Test
    void mapsPixelsInsideCells() {
        assertEquals(0, layout.slotAt(11, 21, 0, 39, 0));
        assertEquals(0, layout.slotAt(34, 44, 0, 39, 0));
        assertEquals(1, layout.slotAt(36, 21, 0, 39, 0));
        assertEquals(16, layout.slotAt(11, 46, 0, 39, 0));
        assertEquals(16 * 39 - 1, layout.slotAt(10 + 400 - 1, 20 + 975 - 1, 0, 39, 0));
    }

    @Test
    void gapsMapToNone() {
        assertEquals(-1, layout.slotAt(10, 21, 0, 39, 0));
        assertEquals(-1, layout.slotAt(35, 21, 0, 39, 0));
        assertEquals(-1, layout.slotAt(11, 20, 0, 39, 0));
        assertEquals(-1, layout.slotAt(11, 45, 0, 39, 0));
    }

    @Test
    void scrollOffsetShiftsMappingAndOrigins() {
        assertEquals(21, layout.slotTop(48, 3));
        assertEquals(48, layout.slotAt(11, 21, 3, 10, 0));
        assertEquals(3 * 16 + 16, layout.slotAt(11, 46, 3, 10, 0));
        assertEquals(12 * 16, layout.slotAt(11, 21 + 9 * 25, 3, 10, 0));
    }

    @Test
    void rowsBelowTheVisibleWindowMapToNone() {
        assertEquals(-1, layout.slotAt(11, 21 + 10 * 25, 3, 10, 0));
        assertEquals(-1, layout.slotAt(11, 21 + 25, 38, 10, 0));
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
        // Panel at (10, 20), 16x32 slots, 1 px gaps, 4 x 3 slots: x pitch 17, y pitch 33.
        SheetGridLayout tall = new SheetGridLayout(10, 20, new GlyphSize(16, 32), 1, 4, 4, 3);
        assertEquals(69, tall.panelWidth());
        assertEquals(100, tall.panelHeight());
        assertEquals(28, tall.slotLeft(1, 0));
        assertEquals(54, tall.slotTop(4, 0));
        assertEquals(0, tall.slotAt(11, 21, 0, 3, 0));
        assertEquals(0, tall.slotAt(26, 52, 0, 3, 0));
        assertEquals(5, tall.slotAt(28, 54, 0, 3, 0));
        assertEquals(11, tall.slotAt(10 + 68 - 1, 20 + 99 - 1, 0, 3, 0));
        assertEquals(-1, tall.slotAt(27, 21, 0, 3, 0));
        assertEquals(-1, tall.slotAt(11, 53, 0, 3, 0));
        assertEquals(-1, tall.slotAt(11, 20 + 100, 0, 3, 0));
        assertEquals(-1, tall.slotAt(10 + 69, 21, 0, 3, 0));
        assertEquals(3, tall.rowsThatFit(100));
        assertEquals(2, tall.rowsThatFit(98));
        assertEquals(100, tall.heightForRows(3));
    }

    @Test
    void horizontalOffsetShiftsMappingAndOrigins() {
        // 6 of 16 columns shown, first shown column 5, no row scroll.
        SheetGridLayout window = new SheetGridLayout(10, 20, new GlyphSize(24, 24), 1, 16, 6, 39);
        assertEquals(11, window.slotLeft(5, 5));
        assertEquals(36, window.slotLeft(6, 5));
        assertEquals(5, window.slotAt(11, 21, 0, 10, 5));
        assertEquals(16 + 7, window.slotAt(11 + 2 * 25, 46, 0, 10, 5));
        assertEquals(10, window.slotAt(11 + 5 * 25, 21, 0, 10, 5));
    }

    @Test
    void columnsOutsideTheHorizontalWindowMapToNone() {
        SheetGridLayout window = new SheetGridLayout(10, 20, new GlyphSize(24, 24), 1, 16, 6, 39);
        assertEquals(-1, window.slotAt(11 + 6 * 25, 21, 0, 10, 5));
        assertEquals(15, window.slotAt(11 + 5 * 25, 21, 0, 10, 10));
        assertEquals(-1, window.slotAt(11 + 6 * 25, 21, 0, 10, 10));
    }

    @Test
    void outsideThePanelMapsToNone() {
        assertEquals(-1, layout.slotAt(-5, -5, 0, 39, 0));
        assertEquals(-1, layout.slotAt(5, 100, 0, 39, 0));
        assertEquals(-1, layout.slotAt(10 + 401, 100, 0, 39, 0));
        assertEquals(-1, layout.slotAt(100, 20 + 976, 0, 39, 0));
    }
}
