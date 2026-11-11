package com.github.halfdeadgames.asciigdx.workbench.upscale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UpscalerTest {

    // An 8-wide glyph whose last column is lit in every row, so the right edge of the scaled cell is checked.
    private static PixelGlyph edgeGlyph() {
        List<String> rows = new ArrayList<>();
        for (int y = 0; y < 8; y++) {
            rows.add(y == 3 ? "########" : ".......#");
        }
        return new PixelGlyph('A', PixelGrid.ofRows(rows));
    }

    private static boolean columnIsEmpty(PixelGrid cell, int column) {
        for (int y = 0; y < cell.getHeight(); y++) {
            if (cell.isLit(column, y)) {
                return false;
            }
        }
        return true;
    }

    @Test
    void nearestDoublesTheWholeCell() {
        PixelGrid cell = Upscaler.upscale(edgeGlyph(), 2, 2, Upscaler.Method.NEAREST);
        assertEquals(16, cell.getWidth());
        assertEquals(16, cell.getHeight());
        assertTrue(cell.isLit(15, 0));
        assertFalse(columnIsEmpty(cell, 14));
        assertFalse(columnIsEmpty(cell, 15));
    }

    @Test
    void nearestTriplesTheWholeCell() {
        PixelGrid cell = Upscaler.upscale(edgeGlyph(), 3, 3, Upscaler.Method.NEAREST);
        assertEquals(24, cell.getWidth());
        assertEquals(24, cell.getHeight());
        assertTrue(cell.isLit(23, 0));
        assertFalse(columnIsEmpty(cell, 21));
    }

    @Test
    void everyFactorTwoMethodFillsTheTargetCellSize() {
        for (Upscaler.Method method : Upscaler.methodsFor(2, 2)) {
            PixelGrid cell = Upscaler.upscale(edgeGlyph(), 2, 2, method);
            assertEquals(16, cell.getWidth(), method.name());
            assertEquals(16, cell.getHeight(), method.name());
        }
    }

    private static PixelGlyph staircaseGlyph(int codePoint) {
        return new PixelGlyph(codePoint, PixelGrid.ofRows(List.of(
            "#.......",
            "##......",
            "###.....",
            "####....",
            "#####...",
            "######..",
            "#######.",
            "########")));
    }

    private static void assertCodePointDoesNotChangeSmoothing(int codePoint) {
        boolean someMethodSmooths = false;
        for (Upscaler.Method method : Upscaler.methodsFor(2, 2)) {
            PixelGrid ordinary = Upscaler.upscale(staircaseGlyph('A'), 2, 2, method);
            PixelGrid special = Upscaler.upscale(staircaseGlyph(codePoint), 2, 2, method);
            assertEquals(ordinary, special, method.name());
            someMethodSmooths |= !ordinary.equals(
                Upscaler.upscale(staircaseGlyph('A'), 2, 2, Upscaler.Method.NEAREST));
        }
        assertTrue(someMethodSmooths, "the staircase must differ from nearest under some method");
    }

    @Test
    void boxDrawingGlyphsAreSmoothedLikeOrdinaryGlyphs() {
        assertCodePointDoesNotChangeSmoothing(0x2500);
        assertCodePointDoesNotChangeSmoothing(0x2514);
    }

    @Test
    void privateUseGlyphsAreSmoothedLikeOrdinaryGlyphs() {
        assertCodePointDoesNotChangeSmoothing(0xE000);
    }

    @Test
    void smoothingMethodsOnlyExistAtFactorTwo() {
        assertEquals(6, Upscaler.methodsFor(2, 2).size());
        assertEquals(List.of(Upscaler.Method.NEAREST), Upscaler.methodsFor(3, 3));
        assertEquals(List.of(Upscaler.Method.NEAREST), Upscaler.methodsFor(4, 4));
        assertThrows(IllegalArgumentException.class,
            () -> Upscaler.upscale(edgeGlyph(), 3, 3, Upscaler.Method.MMPX));
    }

    private static void assertEveryPixelMatchesItsSource(PixelGlyph glyph, PixelGrid cell, int factorX, int factorY) {
        PixelGrid source = glyph.pixels();
        assertEquals(source.getWidth() * factorX, cell.getWidth());
        assertEquals(source.getHeight() * factorY, cell.getHeight());
        for (int y = 0; y < cell.getHeight(); y++) {
            for (int x = 0; x < cell.getWidth(); x++) {
                assertEquals(source.isLit(x / factorX, y / factorY), cell.isLit(x, y), x + "," + y);
            }
        }
    }

    @Test
    void nearestAtTwoByOneDoublesColumnsOnly() {
        PixelGrid cell = Upscaler.upscale(edgeGlyph(), 2, 1, Upscaler.Method.NEAREST);
        assertEquals(16, cell.getWidth());
        assertEquals(8, cell.getHeight());
        assertEveryPixelMatchesItsSource(edgeGlyph(), cell, 2, 1);
    }

    @Test
    void nearestAtOneByThreeTriplesRowsOnly() {
        PixelGrid cell = Upscaler.upscale(edgeGlyph(), 1, 3, Upscaler.Method.NEAREST);
        assertEquals(8, cell.getWidth());
        assertEquals(24, cell.getHeight());
        assertEveryPixelMatchesItsSource(edgeGlyph(), cell, 1, 3);
    }

    @Test
    void smoothingMethodsNeedFactorTwoOnBothAxes() {
        assertEquals(List.of(Upscaler.Method.NEAREST), Upscaler.methodsFor(2, 1));
        assertEquals(List.of(Upscaler.Method.NEAREST), Upscaler.methodsFor(1, 2));
        assertEquals(6, Upscaler.methodsFor(2, 2).size());
        assertThrows(IllegalArgumentException.class,
            () -> Upscaler.upscale(edgeGlyph(), 2, 1, Upscaler.Method.SCALE2X));
    }

    @Test
    void factorBelowOneIsRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> Upscaler.upscale(edgeGlyph(), 0, 2, Upscaler.Method.NEAREST));
        assertThrows(IllegalArgumentException.class,
            () -> Upscaler.upscale(edgeGlyph(), 2, 0, Upscaler.Method.NEAREST));
    }

    @Test
    void factorLabelShowsOneFactorWhenEqual() {
        assertEquals("2X", Upscaler.factorLabel(2, 2));
        assertEquals("2X/1X", Upscaler.factorLabel(2, 1));
    }
}
