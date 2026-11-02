package com.github.halfdeadgames.asciigdx.workbench;

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

    // A 7-wide body whose last column is lit in every row, so any leak into the gap would show.
    private static PixelGlyph bodyGlyph() {
        List<String> rows = new ArrayList<>();
        for (int y = 0; y < 8; y++) {
            rows.add(y == 3 ? "#######" : "......#");
        }
        return new PixelGlyph('A', false, PixelGrid.ofRows(rows));
    }

    private static PixelGlyph fullGlyph() {
        List<String> rows = new ArrayList<>();
        for (int y = 0; y < 8; y++) {
            rows.add("#.......");
        }
        return new PixelGlyph(0x2588, true, PixelGrid.ofRows(rows));
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
    void nearestDoublesTheBodyAndAddsAnEmptyGapOfTwo() {
        PixelGrid cell = Upscaler.upscale(bodyGlyph(), 2, Upscaler.Method.NEAREST, 16);
        assertEquals(16, cell.getWidth());
        assertEquals(16, cell.getHeight());
        assertTrue(cell.isLit(13, 0));
        assertTrue(columnIsEmpty(cell, 14));
        assertTrue(columnIsEmpty(cell, 15));
    }

    @Test
    void nearestTriplesTheBodyAndAddsAnEmptyGapOfThree() {
        PixelGrid cell = Upscaler.upscale(bodyGlyph(), 3, Upscaler.Method.NEAREST, 24);
        assertEquals(24, cell.getWidth());
        assertEquals(24, cell.getHeight());
        assertTrue(cell.isLit(20, 0));
        for (int gap = 21; gap < 24; gap++) {
            assertTrue(columnIsEmpty(cell, gap), "gap column " + gap);
        }
    }

    @Test
    void fullGlyphsScaleTheWholeCell() {
        PixelGrid cell = Upscaler.upscale(fullGlyph(), 2, Upscaler.Method.NEAREST, 16);
        assertEquals(16, cell.getWidth());
        assertTrue(cell.isLit(0, 0));
        assertTrue(cell.isLit(1, 15));
        assertFalse(cell.isLit(2, 0));
    }

    @Test
    void everyFactorTwoMethodKeepsTheGapEmpty() {
        for (Upscaler.Method method : Upscaler.methodsFor(2)) {
            PixelGrid cell = Upscaler.upscale(bodyGlyph(), 2, method, 16);
            assertEquals(16, cell.getWidth(), method.label);
            assertTrue(columnIsEmpty(cell, 14), method.label);
            assertTrue(columnIsEmpty(cell, 15), method.label);
        }
    }

    @Test
    void smoothingMethodsOnlyExistAtFactorTwo() {
        assertEquals(6, Upscaler.methodsFor(2).size());
        assertEquals(List.of(Upscaler.Method.NEAREST), Upscaler.methodsFor(3));
        assertEquals(List.of(Upscaler.Method.NEAREST), Upscaler.methodsFor(4));
        assertThrows(IllegalArgumentException.class,
            () -> Upscaler.upscale(bodyGlyph(), 3, Upscaler.Method.MMPX, 24));
    }
}
