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
    void overlayHasTheSquareGridSizeAndTransparentBackgrounds() {
        assertEquals(6, overlay.getColumns());
        assertEquals(8, overlay.getRows());
        for (int row = 0; row < overlay.getRows(); row++) {
            for (int column = 0; column < overlay.getColumns(); column++) {
                assertEquals(TerminalGrid.TRANSPARENT_RGBA, overlay.getBackground(column, row));
            }
        }
    }

    @Test
    void writeOverlayGlyphStoresCodePointAndForegroundOnlyInTheOverlay() {
        surface.writeOverlayGlyph(2, 3, UNDERSCORE, BLUE);

        assertEquals(UNDERSCORE, overlay.getCodePoint(2, 3));
        assertEquals(BLUE, overlay.getForeground(2, 3));
        assertEquals(EMPTY, surface.getSquareGrid().getCodePoint(2, 3));
        assertEquals(EMPTY, surface.getTallGrid().getCodePoint(2, 1));
        assertEquals(TerminalGrid.TRANSPARENT_RGBA, overlay.getBackground(2, 3));
    }

    @Test
    void overlayWritesOutsideTheSurfaceAreSkipped() {
        assertDoesNotThrow(() -> {
            surface.writeOverlayGlyph(-1, 0, UNDERSCORE, WHITE);
            surface.writeOverlayGlyph(0, -1, UNDERSCORE, WHITE);
            surface.writeOverlayGlyph(6, 0, UNDERSCORE, WHITE);
            surface.writeOverlayGlyph(0, 8, UNDERSCORE, WHITE);
        });
        assertEquals(EMPTY, overlay.getCodePoint(0, 0));
    }

    @Test
    void squareGlyphClearsOnlyItsOwnOverlayCell() {
        surface.writeOverlayGlyph(1, 2, UNDERSCORE, WHITE);
        surface.writeOverlayGlyph(1, 3, UNDERSCORE, WHITE);

        surface.writeGlyph(UiFont.SQUARE, 1, 2, 'a', WHITE);

        assertEquals(EMPTY, overlay.getCodePoint(1, 2));
        assertEquals(UNDERSCORE, overlay.getCodePoint(1, 3));
    }

    @Test
    void tallGlyphClearsTheOverlayInBothRowsItCovers() {
        surface.writeOverlayGlyph(1, 2, UNDERSCORE, WHITE);
        surface.writeOverlayGlyph(1, 3, UNDERSCORE, WHITE);
        surface.writeOverlayGlyph(1, 4, UNDERSCORE, WHITE);

        surface.writeGlyph(UiFont.TALL, 1, 3, 'a', WHITE);

        assertEquals(EMPTY, overlay.getCodePoint(1, 2));
        assertEquals(EMPTY, overlay.getCodePoint(1, 3));
        assertEquals(UNDERSCORE, overlay.getCodePoint(1, 4));
    }

    @Test
    void eraseGlyphsClearsTheOverlayInTheRectangleOnly() {
        surface.writeOverlayGlyph(1, 1, UNDERSCORE, WHITE);
        surface.writeOverlayGlyph(4, 1, UNDERSCORE, WHITE);

        surface.eraseGlyphs(0, 0, 3, 2);

        assertEquals(EMPTY, overlay.getCodePoint(1, 1));
        assertEquals(UNDERSCORE, overlay.getCodePoint(4, 1));
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
        source.writeOverlayGlyph(3, 5, UNDERSCORE, BLUE);

        surface.copyFrom(source);

        assertEquals(UNDERSCORE, overlay.getCodePoint(3, 5));
        assertEquals(BLUE, overlay.getForeground(3, 5));
    }
}
