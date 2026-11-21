package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class UiSurfaceOverlayTest {
    private static final int EMPTY = TerminalGrid.EMPTY_CODE_POINT;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLUE = 0x0000FFFF;
    private static final int UNDERSCORE = '_';

    private final UiSurface surface = UiSurface.ofSize(6, 8);
    private final TerminalGrid overlay = surface.getOverlayGrid();

    @Test
    void overlayHasTextResolutionAndTransparentBackgrounds() {
        assertEquals(12, overlay.getColumns());
        assertEquals(8, overlay.getRows());
        for (int row = 0; row < overlay.getRows(); row++) {
            for (int column = 0; column < overlay.getColumns(); column++) {
                assertEquals(TerminalGrid.TRANSPARENT_RGBA, overlay.getBackground(column, row));
            }
        }
    }

    @Test
    void writeOverlayGlyphStoresCodePointAndForegroundOnlyInTheOverlay() {
        surface.writeOverlayGlyph(5, 3, UNDERSCORE, BLUE);

        assertEquals(UNDERSCORE, overlay.getCodePoint(5, 3));
        assertEquals(BLUE, overlay.getForeground(5, 3));
        assertEquals(EMPTY, surface.getSquareGrid().getCodePoint(2, 3));
        assertEquals(EMPTY, surface.getTextGrid().getCodePoint(5, 3));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, overlay.getBackground(5, 3));
    }

    @Test
    void overlayAddressesEachTextHalfOfASquareCell() {
        surface.writeOverlayGlyph(4, 1, UNDERSCORE, WHITE);
        surface.writeOverlayGlyph(5, 1, '^', BLUE);

        assertEquals(UNDERSCORE, overlay.getCodePoint(4, 1));
        assertEquals('^', overlay.getCodePoint(5, 1));
        assertEquals(WHITE, overlay.getForeground(4, 1));
        assertEquals(BLUE, overlay.getForeground(5, 1));
    }

    @Test
    void overlayWritesOutsideTheSurfaceAreSkipped() {
        assertDoesNotThrow(() -> {
            surface.writeOverlayGlyph(-1, 0, UNDERSCORE, WHITE);
            surface.writeOverlayGlyph(0, -1, UNDERSCORE, WHITE);
            surface.writeOverlayGlyph(12, 0, UNDERSCORE, WHITE);
            surface.writeOverlayGlyph(0, 8, UNDERSCORE, WHITE);
        });
        assertEquals(EMPTY, overlay.getCodePoint(0, 0));
        assertEquals(EMPTY, overlay.getCodePoint(11, 0));
    }

    @Test
    void squareGlyphClearsOnlyTheTwoOverlayCellsOverIt() {
        surface.writeOverlayGlyph(2, 2, UNDERSCORE, WHITE);
        surface.writeOverlayGlyph(3, 2, UNDERSCORE, WHITE);
        surface.writeOverlayGlyph(4, 2, UNDERSCORE, WHITE);
        surface.writeOverlayGlyph(2, 3, UNDERSCORE, WHITE);

        surface.writeSquareGlyph(1, 2, 'a', WHITE);

        assertEquals(EMPTY, overlay.getCodePoint(2, 2));
        assertEquals(EMPTY, overlay.getCodePoint(3, 2));
        assertEquals(UNDERSCORE, overlay.getCodePoint(4, 2));
        assertEquals(UNDERSCORE, overlay.getCodePoint(2, 3));
    }

    @Test
    void textGlyphClearsOnlyTheOverlayCellAtItsTextColumn() {
        surface.writeOverlayGlyph(2, 2, UNDERSCORE, WHITE);
        surface.writeOverlayGlyph(3, 2, UNDERSCORE, WHITE);

        surface.writeTextGlyph(3, 2, 'a', WHITE);

        assertEquals(UNDERSCORE, overlay.getCodePoint(2, 2));
        assertEquals(EMPTY, overlay.getCodePoint(3, 2));
    }

    @Test
    void eraseGlyphsClearsTheOverlayInTheRectangleOnly() {
        surface.writeOverlayGlyph(2, 1, UNDERSCORE, WHITE);
        surface.writeOverlayGlyph(3, 1, UNDERSCORE, WHITE);
        surface.writeOverlayGlyph(8, 1, UNDERSCORE, WHITE);

        surface.eraseGlyphs(0, 0, 3, 2);

        assertEquals(EMPTY, overlay.getCodePoint(2, 1));
        assertEquals(EMPTY, overlay.getCodePoint(3, 1));
        assertEquals(UNDERSCORE, overlay.getCodePoint(8, 1));
    }

    @Test
    void fillBackgroundLeavesTheOverlayAlone() {
        surface.writeOverlayGlyph(1, 1, UNDERSCORE, WHITE);

        surface.fillBackground(0, 0, 6, 8, BLUE);

        assertEquals(UNDERSCORE, overlay.getCodePoint(1, 1));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, overlay.getBackground(1, 1));
    }

    @Test
    void copyFromCopiesTheOverlay() {
        UiSurface source = UiSurface.ofSize(6, 8);
        source.writeOverlayGlyph(11, 5, UNDERSCORE, BLUE);

        surface.copyFrom(source);

        assertEquals(UNDERSCORE, overlay.getCodePoint(11, 5));
        assertEquals(BLUE, overlay.getForeground(11, 5));
    }
}
