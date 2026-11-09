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
        PixelGrid cell = Upscaler.upscale(edgeGlyph(), 2, Upscaler.Method.NEAREST);
        assertEquals(16, cell.getWidth());
        assertEquals(16, cell.getHeight());
        assertTrue(cell.isLit(15, 0));
        assertFalse(columnIsEmpty(cell, 14));
        assertFalse(columnIsEmpty(cell, 15));
    }

    @Test
    void nearestTriplesTheWholeCell() {
        PixelGrid cell = Upscaler.upscale(edgeGlyph(), 3, Upscaler.Method.NEAREST);
        assertEquals(24, cell.getWidth());
        assertEquals(24, cell.getHeight());
        assertTrue(cell.isLit(23, 0));
        assertFalse(columnIsEmpty(cell, 21));
    }

    @Test
    void everyFactorTwoMethodFillsTheTargetCellSize() {
        for (Upscaler.Method method : Upscaler.methodsFor(2)) {
            PixelGrid cell = Upscaler.upscale(edgeGlyph(), 2, method);
            assertEquals(16, cell.getWidth(), method.label);
            assertEquals(16, cell.getHeight(), method.label);
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
        for (Upscaler.Method method : Upscaler.methodsFor(2)) {
            PixelGrid ordinary = Upscaler.upscale(staircaseGlyph('A'), 2, method);
            PixelGrid special = Upscaler.upscale(staircaseGlyph(codePoint), 2, method);
            assertEquals(ordinary, special, method.label);
            someMethodSmooths |= !ordinary.equals(
                Upscaler.upscale(staircaseGlyph('A'), 2, Upscaler.Method.NEAREST));
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
        assertEquals(6, Upscaler.methodsFor(2).size());
        assertEquals(List.of(Upscaler.Method.NEAREST), Upscaler.methodsFor(3));
        assertEquals(List.of(Upscaler.Method.NEAREST), Upscaler.methodsFor(4));
        assertThrows(IllegalArgumentException.class,
            () -> Upscaler.upscale(edgeGlyph(), 3, Upscaler.Method.MMPX));
    }
}
