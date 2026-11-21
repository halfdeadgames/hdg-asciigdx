package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class UiSurfaceTextBackgroundTest {
    private static final int EMPTY = TerminalGrid.EMPTY_CODE_POINT;
    private static final int TRANSPARENT = TerminalGrid.TRANSPARENT_RGBA;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLUE = 0x0000FFFF;
    private static final int RED = 0xFF0000FF;
    private static final int DIM = 0x204060FF;

    private final UiSurface surface = UiSurface.ofSize(6, 8);
    private final TerminalGrid square = surface.getSquareGrid();
    private final TerminalGrid text = surface.getTextGrid();

    @Test
    void writeTextGlyphWithBackgroundSetsGlyphForegroundAndBackground() {
        surface.writeTextGlyphWithBackground(5, 3, 'T', WHITE, RED);

        assertEquals('T', text.getCodePoint(5, 3));
        assertEquals(WHITE, text.getForeground(5, 3));
        assertEquals(RED, text.getBackground(5, 3));
    }

    @Test
    void writeTextGlyphWithBackgroundTouchesOnlyItsOwnTextColumn() {
        surface.writeTextGlyphWithBackground(5, 3, 'T', WHITE, RED);

        assertEquals(TRANSPARENT, text.getBackground(4, 3));
        assertEquals(TRANSPARENT, text.getBackground(6, 3));
        assertEquals(TRANSPARENT, text.getBackground(5, 2));
        assertEquals(TRANSPARENT, square.getBackground(2, 3));
    }

    @Test
    void writeTextGlyphWithBackgroundErasesLikeWriteTextGlyph() {
        surface.writeSquareGlyph(2, 3, 'S', WHITE);
        surface.writeOverlayGlyph(4, 3, '_', WHITE);

        surface.writeTextGlyphWithBackground(4, 3, 'T', WHITE, RED);

        assertEquals(EMPTY, square.getCodePoint(2, 3));
        assertEquals(EMPTY, surface.getOverlayGrid().getCodePoint(4, 3));
    }

    @Test
    void writeTextGlyphWithBackgroundOutsideSurfaceIsSkippedSilently() {
        assertDoesNotThrow(() -> {
            surface.writeTextGlyphWithBackground(-1, 0, 'x', WHITE, RED);
            surface.writeTextGlyphWithBackground(0, -1, 'x', WHITE, RED);
            surface.writeTextGlyphWithBackground(12, 0, 'x', WHITE, RED);
            surface.writeTextGlyphWithBackground(0, 8, 'x', WHITE, RED);
            surface.writeTextGlyphWithBackground(Integer.MIN_VALUE, Integer.MAX_VALUE, 'x', WHITE,
                    RED);
        });
        for (int row = 0; row < text.getRows(); row++) {
            for (int column = 0; column < text.getColumns(); column++) {
                assertEquals(EMPTY, text.getCodePoint(column, row));
                assertEquals(TRANSPARENT, text.getBackground(column, row));
            }
        }
    }

    @Test
    void writeTextGlyphWithBackgroundIsClippedByUiCell() {
        surface.pushClip(1, 1, 1, 1);

        surface.writeTextGlyphWithBackground(1, 1, 'a', WHITE, RED);
        surface.writeTextGlyphWithBackground(2, 1, 'b', WHITE, RED);
        surface.writeTextGlyphWithBackground(3, 1, 'c', WHITE, RED);
        surface.writeTextGlyphWithBackground(4, 1, 'd', WHITE, RED);
        surface.popClip();

        assertEquals(TRANSPARENT, text.getBackground(1, 1));
        assertEquals(RED, text.getBackground(2, 1));
        assertEquals(RED, text.getBackground(3, 1));
        assertEquals(TRANSPARENT, text.getBackground(4, 1));
    }

    @Test
    void plainWriteTextGlyphResetsAnEarlierBackground() {
        surface.writeTextGlyphWithBackground(5, 3, 'T', WHITE, RED);

        surface.writeTextGlyph(5, 3, 'U', WHITE);

        assertEquals('U', text.getCodePoint(5, 3));
        assertEquals(TRANSPARENT, text.getBackground(5, 3));
    }

    @Test
    void writeSquareGlyphResetsTextBackgroundsOfBothTextColumns() {
        surface.writeTextGlyphWithBackground(4, 3, 'a', WHITE, RED);
        surface.writeTextGlyphWithBackground(5, 3, 'b', WHITE, RED);
        surface.writeTextGlyphWithBackground(3, 3, 'c', WHITE, RED);
        surface.writeTextGlyphWithBackground(6, 3, 'd', WHITE, RED);

        surface.writeSquareGlyph(2, 3, 'S', WHITE);

        assertEquals(TRANSPARENT, text.getBackground(4, 3));
        assertEquals(TRANSPARENT, text.getBackground(5, 3));
        assertEquals(RED, text.getBackground(3, 3));
        assertEquals(RED, text.getBackground(6, 3));
    }

    @Test
    void eraseGlyphsResetsTextBackgroundsInTheRectangleOnly() {
        surface.writeTextGlyphWithBackground(2, 4, 'a', WHITE, RED);
        surface.writeTextGlyphWithBackground(3, 4, 'b', WHITE, RED);
        surface.writeTextGlyphWithBackground(4, 4, 'c', WHITE, RED);
        surface.fillBackground(1, 4, 1, 1, BLUE);

        surface.eraseGlyphs(1, 4, 1, 1);

        assertEquals(TRANSPARENT, text.getBackground(2, 4));
        assertEquals(TRANSPARENT, text.getBackground(3, 4));
        assertEquals(RED, text.getBackground(4, 4));
        assertEquals(BLUE, square.getBackground(1, 4));
    }

    @Test
    void fillBackgroundLeavesTextBackgroundsAlone() {
        surface.writeTextGlyphWithBackground(2, 0, 'T', WHITE, RED);

        surface.fillBackground(0, 0, 3, 2, BLUE);

        assertEquals(RED, text.getBackground(2, 0));
        assertEquals(BLUE, square.getBackground(1, 0));
    }

    @Test
    void writeOverlayGlyphLeavesTextBackgroundsAlone() {
        surface.writeTextGlyphWithBackground(2, 0, 'T', WHITE, RED);

        surface.writeOverlayGlyph(2, 0, '_', WHITE);

        assertEquals(RED, text.getBackground(2, 0));
    }

    @Test
    void copyFromCopiesTextBackgrounds() {
        UiSurface source = UiSurface.ofSize(6, 8);
        source.writeTextGlyphWithBackground(5, 2, 'T', WHITE, RED);
        surface.writeTextGlyphWithBackground(0, 0, 'Z', WHITE, BLUE);

        surface.copyFrom(source);

        assertEquals(RED, text.getBackground(5, 2));
        assertEquals(TRANSPARENT, text.getBackground(0, 0));
    }

    @Test
    void dimPassDimsAVisibleTextBackgroundLikeASquareBackground() {
        square.set(0, 0, 'A', WHITE, 0x10203040);
        surface.writeTextGlyphWithBackground(5, 2, 'T', WHITE, 0x10203040);

        surface.dimAll(DIM, 0.5f);

        assertEquals(0x18304840, text.getBackground(5, 2));
        assertEquals(square.getBackground(0, 0), text.getBackground(5, 2));
    }

    @Test
    void dimPassLeavesATransparentTextBackgroundTransparent() {
        surface.writeTextGlyph(5, 2, 'T', WHITE);

        surface.dimAll(DIM, 0.5f);

        assertEquals(TRANSPARENT, text.getBackground(5, 2));
        assertEquals(TRANSPARENT, text.getBackground(0, 0));
    }

    @Test
    void dimPassStillGivesATransparentSquareBackgroundTheDimColor() {
        surface.dimAll(DIM, 0.5f);

        assertEquals(0x20406080, square.getBackground(0, 0));
    }
}
