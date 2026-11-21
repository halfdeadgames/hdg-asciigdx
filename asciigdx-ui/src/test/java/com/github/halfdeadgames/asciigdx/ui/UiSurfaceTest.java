package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private final TerminalGrid text = surface.getTextGrid();
    private final TerminalGrid overlay = surface.getOverlayGrid();

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
    void ofSizeCreatesSquareGridAndDoubleWidthTextGrid() {
        assertEquals(6, square.getColumns());
        assertEquals(8, square.getRows());
        assertEquals(12, text.getColumns());
        assertEquals(8, text.getRows());
        assertEquals(12, overlay.getColumns());
        assertEquals(8, overlay.getRows());
        assertEquals(12, surface.getTextColumns());
        assertEquals(6, surface.getColumns());
        assertEquals(8, surface.getRows());
    }

    @Test
    void oddSizedSurfaceHasTextGridOfTwiceTheColumns() {
        UiSurface odd = UiSurface.ofSize(3, 5);
        assertEquals(6, odd.getTextGrid().getColumns());
        assertEquals(5, odd.getTextGrid().getRows());
        assertEquals(6, odd.getTextColumns());
    }

    @Test
    void textBackgroundsAreTransparent() {
        for (int row = 0; row < text.getRows(); row++) {
            for (int column = 0; column < text.getColumns(); column++) {
                assertEquals(TerminalGrid.TRANSPARENT_RGBA, text.getBackground(column, row));
            }
        }
    }

    @Test
    void ofSizeRejectsSmallSizes() {
        assertThrows(IllegalArgumentException.class, () -> UiSurface.ofSize(0, 1));
        assertThrows(IllegalArgumentException.class, () -> UiSurface.ofSize(1, 0));
    }

    @Test
    void writeTextGlyphClearsSquareGlyphUnderLeftHalf() {
        surface.writeSquareGlyph(2, 3, 'S', WHITE);
        surface.writeTextGlyph(4, 3, 'T', WHITE);
        assertEquals(EMPTY, square.getCodePoint(2, 3));
        assertEquals('T', text.getCodePoint(4, 3));
        assertEquals(WHITE, text.getForeground(4, 3));
    }

    @Test
    void writeTextGlyphClearsSquareGlyphUnderRightHalf() {
        surface.writeSquareGlyph(2, 3, 'S', WHITE);
        surface.writeTextGlyph(5, 3, 'T', WHITE);
        assertEquals(EMPTY, square.getCodePoint(2, 3));
        assertEquals('T', text.getCodePoint(5, 3));
    }

    @Test
    void writeTextGlyphKeepsNeighborSquareGlyphs() {
        surface.writeSquareGlyph(1, 3, 'A', WHITE);
        surface.writeSquareGlyph(3, 3, 'B', WHITE);
        surface.writeTextGlyph(4, 3, 'T', WHITE);
        assertEquals('A', square.getCodePoint(1, 3));
        assertEquals('B', square.getCodePoint(3, 3));
    }

    @Test
    void writeTextGlyphClearsOverlayGlyphAtSameTextColumnOnly() {
        surface.writeOverlayGlyph(4, 3, '_', WHITE);
        surface.writeOverlayGlyph(5, 3, '_', WHITE);
        surface.writeTextGlyph(4, 3, 'T', WHITE);
        assertEquals(EMPTY, overlay.getCodePoint(4, 3));
        assertEquals('_', overlay.getCodePoint(5, 3));
    }

    @Test
    void writeSquareGlyphClearsBothTextHalvesAndOverlay() {
        surface.writeTextGlyph(4, 3, 'a', WHITE);
        surface.writeTextGlyph(5, 3, 'b', WHITE);
        surface.writeOverlayGlyph(4, 3, '_', WHITE);
        surface.writeOverlayGlyph(5, 3, '_', WHITE);
        surface.writeSquareGlyph(2, 3, 'S', WHITE);
        assertEquals(EMPTY, text.getCodePoint(4, 3));
        assertEquals(EMPTY, text.getCodePoint(5, 3));
        assertEquals(EMPTY, overlay.getCodePoint(4, 3));
        assertEquals(EMPTY, overlay.getCodePoint(5, 3));
        assertEquals('S', square.getCodePoint(2, 3));
        assertEquals(WHITE, square.getForeground(2, 3));
    }

    @Test
    void writeSquareGlyphKeepsNeighborTextGlyphs() {
        surface.writeTextGlyph(3, 3, 'a', WHITE);
        surface.writeTextGlyph(6, 3, 'b', WHITE);
        surface.writeSquareGlyph(2, 3, 'S', WHITE);
        assertEquals('a', text.getCodePoint(3, 3));
        assertEquals('b', text.getCodePoint(6, 3));
    }

    @Test
    void squareGlyphKeepsBackground() {
        surface.fillBackground(0, 0, 6, 8, BLUE);
        surface.writeSquareGlyph(2, 2, 'S', WHITE);
        assertEquals(BLUE, square.getBackground(2, 2));
    }

    @Test
    void overlayGlyphIsWrittenAtTextResolutionWithoutErasing() {
        surface.writeSquareGlyph(2, 1, 'S', WHITE);
        surface.writeTextGlyph(4, 1, 'a', WHITE);
        surface.writeOverlayGlyph(5, 1, '_', BLUE);
        assertEquals('_', overlay.getCodePoint(5, 1));
        assertEquals(BLUE, overlay.getForeground(5, 1));
        assertEquals(EMPTY, overlay.getCodePoint(4, 1));
        assertEquals('a', text.getCodePoint(4, 1));
    }

    @Test
    void writesOutsideAreSkippedForAllWriters() {
        assertDoesNotThrow(() -> {
            surface.writeSquareGlyph(-1, 0, 'x', WHITE);
            surface.writeSquareGlyph(0, -1, 'x', WHITE);
            surface.writeSquareGlyph(6, 0, 'x', WHITE);
            surface.writeSquareGlyph(0, 8, 'x', WHITE);
            surface.writeSquareGlyph(Integer.MIN_VALUE, Integer.MAX_VALUE, 'x', WHITE);
            surface.writeTextGlyph(-1, 0, 'x', WHITE);
            surface.writeTextGlyph(0, -1, 'x', WHITE);
            surface.writeTextGlyph(12, 0, 'x', WHITE);
            surface.writeTextGlyph(0, 8, 'x', WHITE);
            surface.writeTextGlyph(Integer.MIN_VALUE, Integer.MAX_VALUE, 'x', WHITE);
            surface.writeOverlayGlyph(-1, 0, 'x', WHITE);
            surface.writeOverlayGlyph(12, 0, 'x', WHITE);
            surface.writeOverlayGlyph(Integer.MAX_VALUE, 0, 'x', WHITE);
        });
        assertGridEmpty(square);
        assertGridEmpty(text);
        assertGridEmpty(overlay);
    }

    private static void assertGridEmpty(TerminalGrid grid) {
        for (int row = 0; row < grid.getRows(); row++) {
            for (int column = 0; column < grid.getColumns(); column++) {
                assertEquals(EMPTY, grid.getCodePoint(column, row));
            }
        }
    }

    @Test
    void fillBackgroundTouchesOnlySquareGrid() {
        surface.writeTextGlyph(2, 0, 'T', WHITE);
        surface.fillBackground(0, 0, 3, 2, BLUE);
        assertEquals(BLUE, square.getBackground(2, 1));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, square.getBackground(3, 1));
        assertEquals('T', text.getCodePoint(2, 0));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, text.getBackground(2, 0));
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
    void eraseGlyphsClearsAllThreeGridsInTheRectangle() {
        surface.writeSquareGlyph(1, 3, 'S', WHITE);
        surface.writeTextGlyph(2, 4, 'a', WHITE);
        surface.writeTextGlyph(3, 4, 'b', WHITE);
        surface.writeOverlayGlyph(2, 4, '_', WHITE);
        surface.writeTextGlyph(4, 4, 'c', WHITE);
        surface.eraseGlyphs(1, 3, 1, 2);
        assertEquals(EMPTY, square.getCodePoint(1, 3));
        assertEquals(EMPTY, text.getCodePoint(2, 4));
        assertEquals(EMPTY, text.getCodePoint(3, 4));
        assertEquals(EMPTY, overlay.getCodePoint(2, 4));
        assertEquals('c', text.getCodePoint(4, 4));
    }

    @Test
    void eraseGlyphsKeepsBackgroundsAndClips() {
        surface.fillBackground(0, 0, 6, 8, BLUE);
        surface.writeSquareGlyph(0, 0, 'S', WHITE);
        assertDoesNotThrow(() -> surface.eraseGlyphs(-3, -3, 100, 100));
        assertEquals(EMPTY, square.getCodePoint(0, 0));
        assertEquals(BLUE, square.getBackground(0, 0));
    }

    @Test
    void zeroSizedEraseDoesNothing() {
        surface.writeTextGlyph(0, 0, 'T', WHITE);
        surface.eraseGlyphs(0, 0, 0, 5);
        surface.eraseGlyphs(0, 0, 5, 0);
        assertEquals('T', text.getCodePoint(0, 0));
    }

    @Test
    void negativeSizesThrow() {
        assertThrows(IllegalArgumentException.class, () -> surface.fillBackground(0, 0, -1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> surface.fillBackground(0, 0, 1, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> surface.eraseGlyphs(0, 0, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> surface.eraseGlyphs(0, 0, 1, -1));
    }

    @Test
    void fontPairCheckAcceptsHalfWidthTextFont() {
        assertDoesNotThrow(
                () -> UiSurface.requireFontPair(new SizedSource(8, 16), new SizedSource(16, 16)));
    }

    @Test
    void fontPairCheckRejectsSameWidthTextFont() {
        assertThrows(IllegalArgumentException.class,
                () -> UiSurface.requireFontPair(new SizedSource(16, 16), new SizedSource(16, 16)));
    }

    @Test
    void fontPairCheckRejectsWrongTextHeight() {
        GlyphSource square16 = new SizedSource(16, 16);
        assertThrows(IllegalArgumentException.class,
                () -> UiSurface.requireFontPair(new SizedSource(8, 32), square16));
        assertThrows(IllegalArgumentException.class,
                () -> UiSurface.requireFontPair(new SizedSource(8, 8), square16));
    }

    @Test
    void fontPairCheckRejectsNonSquareSquareFont() {
        assertThrows(IllegalArgumentException.class,
                () -> UiSurface.requireFontPair(new SizedSource(8, 32), new SizedSource(16, 32)));
    }

    @Test
    void fontPairCheckRejectsNulls() {
        GlyphSource square16 = new SizedSource(16, 16);
        GlyphSource text8 = new SizedSource(8, 16);
        assertThrows(NullPointerException.class, () -> UiSurface.requireFontPair(null, square16));
        assertThrows(NullPointerException.class, () -> UiSurface.requireFontPair(text8, null));
    }

    @Test
    void addToRejectsNullStackBeforeAddingAnything() {
        // A real stack needs GL, so size validation is covered through requireFontPair, which
        // addTo calls before it adds a layer.
        assertThrows(NullPointerException.class,
                () -> surface.addTo(null, new SizedSource(8, 16), new SizedSource(16, 16)));
    }

    @Test
    void copyFromCopiesAllThreeGridsCellForCell() {
        UiSurface source = UiSurface.ofSize(6, 8);
        source.fillBackground(0, 0, 6, 8, BLUE);
        source.writeSquareGlyph(1, 1, 'a', WHITE);
        source.writeTextGlyph(5, 2, 'T', WHITE);
        source.writeOverlayGlyph(11, 7, '_', BLUE);

        surface.copyFrom(source);

        assertEquals(BLUE, square.getBackground(5, 7));
        assertEquals('a', square.getCodePoint(1, 1));
        assertEquals(WHITE, square.getForeground(1, 1));
        assertEquals('T', text.getCodePoint(5, 2));
        assertEquals(WHITE, text.getForeground(5, 2));
        assertEquals('_', overlay.getCodePoint(11, 7));
        assertEquals(BLUE, overlay.getForeground(11, 7));
    }

    @Test
    void copyFromLeavesEqualCellsClean() {
        UiSurface source = UiSurface.ofSize(6, 8);
        surface.copyFrom(source);
        var tracker = square.createDirtyTracker();
        var textTracker = text.createDirtyTracker();
        tracker.markAllClean();
        textTracker.markAllClean();

        surface.copyFrom(source);

        assertFalse(tracker.hasDirtyCells());
        assertFalse(textTracker.hasDirtyCells());
    }

    @Test
    void copyFromRejectsOtherSizesAndNull() {
        assertThrows(IllegalArgumentException.class,
                () -> surface.copyFrom(UiSurface.ofSize(6, 7)));
        assertThrows(IllegalArgumentException.class,
                () -> surface.copyFrom(UiSurface.ofSize(5, 8)));
        assertThrows(NullPointerException.class, () -> surface.copyFrom(null));
    }
}
