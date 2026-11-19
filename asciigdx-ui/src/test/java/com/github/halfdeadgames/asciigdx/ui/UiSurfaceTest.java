package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class UiSurfaceTest {
    private static final int EMPTY = TerminalGrid.EMPTY_CODE_POINT;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLUE = 0x0000FFFF;

    private final UiSurface surface = UiSurface.ofSize(6, 8);
    private final TerminalGrid square = surface.getSquareGrid();
    private final TerminalGrid tall = surface.getTallGrid();

    /** Source that only reports a cell size; the glyph methods are never used by these tests. */
    private static final class SizedSource implements GlyphSource {
        private final int width;
        private final int height;

        SizedSource(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public int getCellWidth() {
            return width;
        }

        @Override
        public int getCellHeight() {
            return height;
        }

        @Override
        public TextureRegion getGlyphRegion(int codePoint) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasGlyph(int codePoint) {
            return false;
        }

        @Override
        public void dispose() {
        }
    }

    @Test
    void ofSizeCreatesGridsWithTallRowsRoundedUp() {
        assertEquals(6, square.getColumns());
        assertEquals(8, square.getRows());
        assertEquals(6, tall.getColumns());
        assertEquals(4, tall.getRows());
        UiSurface odd = UiSurface.ofSize(3, 5);
        assertEquals(3, odd.getTallGrid().getRows());
        assertEquals(5, odd.getSquareGrid().getRows());
        assertEquals(3, odd.getColumns());
        assertEquals(5, odd.getRows());
    }

    @Test
    void tallBackgroundsAreTransparent() {
        for (int row = 0; row < tall.getRows(); row++) {
            for (int column = 0; column < tall.getColumns(); column++) {
                assertEquals(TerminalGrid.TRANSPARENT_RGBA, tall.getBackground(column, row));
            }
        }
    }

    @Test
    void ofSizeRejectsSmallSizes() {
        assertThrows(IllegalArgumentException.class, () -> UiSurface.ofSize(0, 1));
        assertThrows(IllegalArgumentException.class, () -> UiSurface.ofSize(1, 0));
    }

    @Test
    void squareGlyphErasesTallGlyphCoveringIt() {
        surface.writeGlyph(UiFont.TALL, 1, 2, 'T', WHITE);
        surface.writeGlyph(UiFont.SQUARE, 1, 3, 'S', WHITE);
        assertEquals(EMPTY, tall.getCodePoint(1, 1));
        assertEquals('S', square.getCodePoint(1, 3));
        assertEquals(WHITE, square.getForeground(1, 3));
    }

    @Test
    void squareGlyphKeepsBackground() {
        surface.fillBackground(0, 0, 6, 8, BLUE);
        surface.writeGlyph(UiFont.SQUARE, 2, 2, 'S', WHITE);
        assertEquals(BLUE, square.getBackground(2, 2));
    }

    @Test
    void tallGlyphErasesBothSquareGlyphs() {
        surface.writeGlyph(UiFont.SQUARE, 2, 4, 'a', WHITE);
        surface.writeGlyph(UiFont.SQUARE, 2, 5, 'b', WHITE);
        surface.writeGlyph(UiFont.TALL, 2, 4, 'T', WHITE);
        assertEquals(EMPTY, square.getCodePoint(2, 4));
        assertEquals(EMPTY, square.getCodePoint(2, 5));
        assertEquals('T', tall.getCodePoint(2, 2));
        assertEquals(WHITE, tall.getForeground(2, 2));
    }

    @Test
    void tallGlyphOnOddRowSnapsToEvenRow() {
        surface.writeGlyph(UiFont.SQUARE, 0, 2, 'a', WHITE);
        surface.writeGlyph(UiFont.TALL, 0, 3, 'T', WHITE);
        assertEquals('T', tall.getCodePoint(0, 1));
        assertEquals(EMPTY, square.getCodePoint(0, 2));
    }

    @Test
    void writesOutsideAreSkippedForBothFonts() {
        for (UiFont font : UiFont.values()) {
            assertDoesNotThrow(() -> {
                surface.writeGlyph(font, -1, 0, 'x', WHITE);
                surface.writeGlyph(font, 0, -1, 'x', WHITE);
                surface.writeGlyph(font, 6, 0, 'x', WHITE);
                surface.writeGlyph(font, 0, 8, 'x', WHITE);
                surface.writeGlyph(font, Integer.MIN_VALUE, Integer.MAX_VALUE, 'x', WHITE);
            });
        }
        for (int row = 0; row < square.getRows(); row++) {
            for (int column = 0; column < square.getColumns(); column++) {
                assertEquals(EMPTY, square.getCodePoint(column, row));
            }
        }
        for (int row = 0; row < tall.getRows(); row++) {
            for (int column = 0; column < tall.getColumns(); column++) {
                assertEquals(EMPTY, tall.getCodePoint(column, row));
            }
        }
    }

    @Test
    void tallWriteOnOddSurfaceLastRowStaysInside() {
        UiSurface odd = UiSurface.ofSize(2, 5);
        odd.writeGlyph(UiFont.TALL, 0, 4, 'T', WHITE);
        assertEquals('T', odd.getTallGrid().getCodePoint(0, 2));
    }

    @Test
    void fillBackgroundTouchesOnlySquareGrid() {
        surface.writeGlyph(UiFont.TALL, 1, 0, 'T', WHITE);
        surface.fillBackground(0, 0, 3, 2, BLUE);
        assertEquals(BLUE, square.getBackground(2, 1));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, square.getBackground(3, 1));
        assertEquals('T', tall.getCodePoint(1, 0));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, tall.getBackground(1, 0));
    }

    @Test
    void fillBackgroundClipsToSurface() {
        surface.fillBackground(-2, -2, 4, 4, BLUE);
        assertEquals(BLUE, square.getBackground(1, 1));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, square.getBackground(2, 1));
        assertDoesNotThrow(() -> surface.fillBackground(5, 7, 100, 100, BLUE));
        assertEquals(BLUE, square.getBackground(5, 7));
    }

    @Test
    void eraseGlyphsCoversOverlappingTallCells() {
        for (int tallRow = 0; tallRow < 4; tallRow++) {
            surface.writeGlyph(UiFont.TALL, 1, 2 * tallRow, 'T', WHITE);
        }
        surface.writeGlyph(UiFont.SQUARE, 1, 3, 'S', WHITE);
        // Rows 3 to 5 (odd start row, odd height) overlap tall rows 1, 2, and 2 again.
        surface.eraseGlyphs(1, 3, 1, 3);
        assertEquals('T', tall.getCodePoint(1, 0));
        assertEquals(EMPTY, tall.getCodePoint(1, 1));
        assertEquals(EMPTY, tall.getCodePoint(1, 2));
        assertEquals('T', tall.getCodePoint(1, 3));
        assertEquals(EMPTY, square.getCodePoint(1, 3));
    }

    @Test
    void eraseGlyphsKeepsBackgroundsAndClips() {
        surface.fillBackground(0, 0, 6, 8, BLUE);
        surface.writeGlyph(UiFont.SQUARE, 0, 0, 'S', WHITE);
        assertDoesNotThrow(() -> surface.eraseGlyphs(-3, -3, 100, 100));
        assertEquals(EMPTY, square.getCodePoint(0, 0));
        assertEquals(BLUE, square.getBackground(0, 0));
    }

    @Test
    void zeroSizedEraseDoesNothing() {
        surface.writeGlyph(UiFont.TALL, 0, 0, 'T', WHITE);
        surface.eraseGlyphs(0, 0, 0, 5);
        surface.eraseGlyphs(0, 0, 5, 0);
        assertEquals('T', tall.getCodePoint(0, 0));
    }

    @Test
    void negativeSizesThrow() {
        assertThrows(IllegalArgumentException.class, () -> surface.fillBackground(0, 0, -1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> surface.fillBackground(0, 0, 1, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> surface.eraseGlyphs(0, 0, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> surface.eraseGlyphs(0, 0, 1, -1));
    }

    @Test
    void nullFontThrows() {
        assertThrows(NullPointerException.class, () -> surface.writeGlyph(null, 0, 0, 'a', WHITE));
    }

    @Test
    void fontPairCheckAcceptsSquareAndTallPair() {
        assertDoesNotThrow(
                () -> UiSurface.requireFontPair(new SizedSource(16, 32), new SizedSource(16, 16)));
    }

    @Test
    void fontPairCheckRejectsMismatchedSizes() {
        GlyphSource square16 = new SizedSource(16, 16);
        assertThrows(IllegalArgumentException.class,
                () -> UiSurface.requireFontPair(new SizedSource(16, 32), new SizedSource(16, 8)));
        assertThrows(IllegalArgumentException.class,
                () -> UiSurface.requireFontPair(new SizedSource(8, 32), square16));
        assertThrows(IllegalArgumentException.class,
                () -> UiSurface.requireFontPair(new SizedSource(16, 16), square16));
        assertThrows(IllegalArgumentException.class,
                () -> UiSurface.requireFontPair(new SizedSource(16, 48), square16));
    }

    @Test
    void fontPairCheckRejectsNulls() {
        GlyphSource square16 = new SizedSource(16, 16);
        assertThrows(NullPointerException.class, () -> UiSurface.requireFontPair(null, square16));
        assertThrows(NullPointerException.class, () -> UiSurface.requireFontPair(square16, null));
    }

    @Test
    void addToRejectsNullsAndBadSizesBeforeAddingAnything() {
        GlyphSource square16 = new SizedSource(16, 16);
        assertThrows(NullPointerException.class,
                () -> surface.addTo(null, new SizedSource(16, 32), square16));
        // A null stack is rejected first; a real stack needs GL, so size validation is covered
        // through requireFontPair, which addTo calls before it adds a layer.
    }
}
