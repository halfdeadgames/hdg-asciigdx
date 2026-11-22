package com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.UiSurface;

/** Draws the one-cell ring of counting glyphs that marks where a widget ends. */
final class BoundaryRing {
    static final int RING_CELLS = 1;
    private static final int CORNER_GLYPH = '#';
    private static final int ODD_GLYPH = '+';
    private static final int EVEN_GLYPH = '-';
    private static final int GLYPH_RGBA = 0x000000FF;

    private BoundaryRing() {
    }

    /** Draws the ring inside the given outer box; the ring box is one cell larger than a widget. */
    static void draw(UiSurface target, int left, int top, int width, int height, int rgba) {
        target.fillBackground(left, top, width, RING_CELLS, rgba);
        target.fillBackground(left, top + height - RING_CELLS, width, RING_CELLS, rgba);
        target.fillBackground(left, top + RING_CELLS, RING_CELLS, height - 2 * RING_CELLS, rgba);
        target.fillBackground(left + width - RING_CELLS, top + RING_CELLS, RING_CELLS,
                height - 2 * RING_CELLS, rgba);
        drawCountingGlyphs(target, left, top, width, height);
    }

    // Corners get their own glyph; the cells between them alternate two glyphs, counted from a
    // corner, so cells can be counted along any side.
    private static void drawCountingGlyphs(UiSurface target, int left, int top, int width,
            int height) {
        for (int column = 0; column < width; column++) {
            int glyph = glyphAt(column, width);
            target.writeSquareGlyph(left + column, top, glyph, GLYPH_RGBA);
            target.writeSquareGlyph(left + column, top + height - 1, glyph, GLYPH_RGBA);
        }
        for (int row = 1; row < height - 1; row++) {
            int glyph = glyphAt(row, height);
            target.writeSquareGlyph(left, top + row, glyph, GLYPH_RGBA);
            target.writeSquareGlyph(left + width - 1, top + row, glyph, GLYPH_RGBA);
        }
    }

    private static int glyphAt(int index, int length) {
        if (index == 0 || index == length - 1) {
            return CORNER_GLYPH;
        }
        return index % 2 == 1 ? ODD_GLYPH : EVEN_GLYPH;
    }
}
