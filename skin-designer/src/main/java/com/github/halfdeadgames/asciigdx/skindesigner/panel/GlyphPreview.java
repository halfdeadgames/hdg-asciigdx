package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.github.halfdeadgames.asciigdx.ui.UiSurface;
import com.github.halfdeadgames.asciigdx.ui.Widget;

/** Draws one glyph in one square cell, the way a widget of the skin would draw it. */
final class GlyphPreview extends Widget {
    private static final int CELLS = 1;

    private final int foregroundRgba;
    private final int backgroundRgba;
    private int codePoint;

    GlyphPreview(int codePoint, int foregroundRgba, int backgroundRgba) {
        this.codePoint = codePoint;
        this.foregroundRgba = foregroundRgba;
        this.backgroundRgba = backgroundRgba;
        pack();
    }

    void setCodePoint(int codePoint) {
        if (codePoint == this.codePoint) {
            return;
        }
        this.codePoint = codePoint;
        requestRedraw();
    }

    @Override
    public int getPreferredWidth() {
        return CELLS;
    }

    @Override
    public int getPreferredHeight() {
        return CELLS;
    }

    @Override
    protected void drawContent(UiSurface target, int left, int top) {
        target.eraseGlyphs(left, top, CELLS, CELLS);
        target.fillBackground(left, top, CELLS, CELLS, backgroundRgba);
        target.writeSquareGlyph(left, top, codePoint, foregroundRgba);
    }
}
