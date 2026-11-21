package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class UiSurfaceClipTest {
    private static final int EMPTY = TerminalGrid.EMPTY_CODE_POINT;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLUE = 0x0000FFFF;

    private final UiSurface surface = UiSurface.ofSize(6, 8);
    private final TerminalGrid square = surface.getSquareGrid();

    private int glyphAt(int column, int row) {
        return square.getCodePoint(column, row);
    }

    @Test
    void writeGlyphSkipsCellsOutsideTheClip() {
        surface.pushClip(1, 1, 3, 2);

        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 6; column++) {
                surface.writeSquareGlyph(column, row, 'A', WHITE);
            }
        }

        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 6; column++) {
                boolean inside = column >= 1 && column <= 3 && row >= 1 && row <= 2;
                assertEquals(inside ? 'A' : EMPTY, glyphAt(column, row), column + "," + row);
            }
        }
    }

    @Test
    void clipsNestByIntersection() {
        surface.pushClip(1, 1, 3, 3);
        surface.pushClip(2, 0, 5, 2);

        surface.writeSquareGlyph(1, 1, 'A', WHITE);
        surface.writeSquareGlyph(2, 2, 'B', WHITE);
        surface.writeSquareGlyph(2, 1, 'C', WHITE);
        surface.writeSquareGlyph(3, 1, 'D', WHITE);
        surface.writeSquareGlyph(4, 1, 'E', WHITE);

        assertEquals(EMPTY, glyphAt(1, 1));
        assertEquals(EMPTY, glyphAt(2, 2));
        assertEquals('C', glyphAt(2, 1));
        assertEquals('D', glyphAt(3, 1));
        assertEquals(EMPTY, glyphAt(4, 1));
    }

    @Test
    void popClipRestoresTheOuterClip() {
        surface.pushClip(1, 1, 3, 3);
        surface.pushClip(2, 2, 1, 1);
        surface.popClip();

        surface.writeSquareGlyph(1, 1, 'A', WHITE);
        surface.writeSquareGlyph(0, 0, 'B', WHITE);
        assertEquals('A', glyphAt(1, 1));
        assertEquals(EMPTY, glyphAt(0, 0));

        surface.popClip();
        surface.writeSquareGlyph(0, 0, 'C', WHITE);
        assertEquals('C', glyphAt(0, 0));
    }

    @Test
    void emptyIntersectionClipsEverythingUntilPopped() {
        surface.pushClip(0, 0, 2, 2);
        surface.pushClip(4, 4, 2, 2);

        surface.writeSquareGlyph(0, 0, 'A', WHITE);
        surface.writeSquareGlyph(4, 4, 'B', WHITE);
        surface.fillBackground(0, 0, 6, 8, BLUE);

        assertEquals(EMPTY, glyphAt(0, 0));
        assertEquals(EMPTY, glyphAt(4, 4));
        assertNotEquals(BLUE, square.getBackground(0, 0));
        assertNotEquals(BLUE, square.getBackground(4, 4));

        surface.popClip();
        surface.writeSquareGlyph(0, 0, 'A', WHITE);
        assertEquals('A', glyphAt(0, 0));
    }

    @Test
    void emptyRectangleClipsEverything() {
        surface.pushClip(1, 1, 0, 3);

        surface.writeSquareGlyph(1, 2, 'A', WHITE);

        assertEquals(EMPTY, glyphAt(1, 2));
    }

    @Test
    void popClipWithoutPushThrows() {
        assertThrows(IllegalStateException.class, surface::popClip);

        surface.pushClip(0, 0, 1, 1);
        surface.popClip();
        assertThrows(IllegalStateException.class, surface::popClip);
    }

    @Test
    void negativeClipSizeThrows() {
        assertThrows(IllegalArgumentException.class, () -> surface.pushClip(0, 0, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> surface.pushClip(0, 0, 1, -1));
    }

    @Test
    void deepClipStackGrowsAndUnwinds() {
        for (int depth = 0; depth < 40; depth++) {
            surface.pushClip(0, 0, 6, 8);
        }
        for (int depth = 0; depth < 40; depth++) {
            surface.popClip();
        }

        assertThrows(IllegalStateException.class, surface::popClip);
    }

    @Test
    void writeOverlayGlyphRespectsTheClipByTextColumn() {
        surface.pushClip(1, 1, 2, 2);

        surface.writeOverlayGlyph(1, 1, '_', WHITE);
        surface.writeOverlayGlyph(2, 1, '_', WHITE);

        assertEquals(EMPTY, surface.getOverlayGrid().getCodePoint(1, 1));
        assertEquals('_', surface.getOverlayGrid().getCodePoint(2, 1));
    }

    @Test
    void fillBackgroundRespectsTheClip() {
        surface.pushClip(1, 1, 2, 2);

        surface.fillBackground(-3, -3, 20, 20, BLUE);

        assertEquals(BLUE, square.getBackground(1, 1));
        assertEquals(BLUE, square.getBackground(2, 2));
        assertNotEquals(BLUE, square.getBackground(0, 1));
        assertNotEquals(BLUE, square.getBackground(3, 2));
        assertNotEquals(BLUE, square.getBackground(2, 3));
    }

    @Test
    void eraseKeepsATextGlyphOutsideTheClip() {
        surface.writeTextGlyph(0, 2, 'T', WHITE);
        surface.pushClip(0, 3, 6, 4);

        surface.eraseGlyphs(0, 0, 6, 8);

        assertEquals('T', surface.getTextGrid().getCodePoint(0, 2));
    }

    @Test
    void eraseRemovesBothTextHalvesInsideTheClip() {
        surface.writeTextGlyph(2, 2, 'T', WHITE);
        surface.writeTextGlyph(3, 2, 'U', WHITE);
        surface.writeTextGlyph(2, 4, 'V', WHITE);
        surface.pushClip(0, 2, 6, 2);

        surface.eraseGlyphs(0, 0, 6, 8);

        assertEquals(EMPTY, surface.getTextGrid().getCodePoint(2, 2));
        assertEquals(EMPTY, surface.getTextGrid().getCodePoint(3, 2));
        assertEquals('V', surface.getTextGrid().getCodePoint(2, 4));
    }

    @Test
    void writeTextGlyphKeepsOrSkipsATextColumnByItsSquareCell() {
        surface.pushClip(1, 0, 1, 8);

        surface.writeTextGlyph(1, 0, 'A', WHITE);
        surface.writeTextGlyph(2, 0, 'B', WHITE);
        surface.writeTextGlyph(3, 0, 'C', WHITE);
        surface.writeTextGlyph(4, 0, 'D', WHITE);

        assertEquals(EMPTY, surface.getTextGrid().getCodePoint(1, 0));
        assertEquals('B', surface.getTextGrid().getCodePoint(2, 0));
        assertEquals('C', surface.getTextGrid().getCodePoint(3, 0));
        assertEquals(EMPTY, surface.getTextGrid().getCodePoint(4, 0));
    }

    @Test
    void clearClipsDropsEveryPushedClip() {
        surface.pushClip(0, 0, 1, 1);
        surface.pushClip(0, 0, 0, 0);

        surface.clearClips();

        surface.writeSquareGlyph(3, 3, 'A', WHITE);
        assertEquals('A', glyphAt(3, 3));
        assertThrows(IllegalStateException.class, surface::popClip);
    }

    @Test
    void uiRootDrawClearsALeakedClip() {
        UiSurface target = UiSurface.ofSize(4, 4);
        UiRoot ui = UiRoot.of(target);
        int[] drawCount = new int[1];
        ui.getRoot().addChild(new Widget() {
            @Override
            public int getPreferredWidth() {
                return 1;
            }

            @Override
            public int getPreferredHeight() {
                return 1;
            }

            @Override
            protected void drawContent(UiSurface surfaceToDraw, int left, int top) {
                surfaceToDraw.writeSquareGlyph(2, 2,'X' + drawCount[0]++, WHITE);
                surfaceToDraw.pushClip(0, 0, 0, 0);
            }
        });

        ui.draw();
        ui.getRoot().invalidate();
        ui.draw();

        assertEquals('Y', target.getSquareGrid().getCodePoint(2, 2));
    }

    @Test
    void eraseGlyphsRespectsTheClip() {
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 6; column++) {
                surface.writeSquareGlyph(column, row, 'A', WHITE);
                surface.writeOverlayGlyph(2 * column, row, '_', WHITE);
                surface.writeOverlayGlyph(2 * column + 1, row, '_', WHITE);
            }
        }
        surface.pushClip(1, 1, 2, 2);

        surface.eraseGlyphs(0, 0, 6, 8);

        assertEquals(EMPTY, glyphAt(1, 1));
        assertEquals(EMPTY, glyphAt(2, 2));
        assertEquals(EMPTY, surface.getOverlayGrid().getCodePoint(2, 1));
        assertEquals(EMPTY, surface.getOverlayGrid().getCodePoint(3, 1));
        assertEquals('A', glyphAt(0, 1));
        assertEquals('A', glyphAt(3, 2));
        assertEquals('A', glyphAt(1, 3));
        assertEquals('_', surface.getOverlayGrid().getCodePoint(6, 2));
        assertEquals('_', surface.getOverlayGrid().getCodePoint(1, 1));
    }

    @Test
    void copyFromIgnoresTheClipForAllThreeGrids() {
        UiSurface source = UiSurface.ofSize(6, 8);
        source.writeSquareGlyph(3, 7, 'Z', WHITE);
        source.writeTextGlyph(11, 7, 'Y', WHITE);
        source.writeOverlayGlyph(10, 7, '_', WHITE);
        surface.pushClip(0, 0, 0, 0);

        surface.copyFrom(source);

        assertEquals('Z', glyphAt(3, 7));
        assertEquals('Y', surface.getTextGrid().getCodePoint(11, 7));
        assertEquals('_', surface.getOverlayGrid().getCodePoint(10, 7));
    }
}
