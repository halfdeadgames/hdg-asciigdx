package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.graphics.Color;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.ui.Panel;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;

/** Live 1x and 2x preview of the selected glyph's whole cell, at real font pixel size, in a "PREVIEW" panel. */
final class PaintPreview {

    private static final int TEXT = Painter.TEXT_SIZE;
    private static final int SECTION_GAP = 12;
    private static final Color BACKDROP = new Color(0.14f, 0.14f, 0.18f, 1f);

    private final Painter painter;
    private final FontEditor editor;

    PaintPreview(Painter painter, FontEditor editor) {
        this.painter = painter;
        this.editor = editor;
    }

    void draw(ReviewLayout.Rect rect, int codePoint) {
        draw(rect, codePoint, null);
    }

    /** @param shown glyph to show instead of the slot's own, or null */
    void draw(ReviewLayout.Rect rect, int codePoint, PixelGlyph shown) {
        Panel.draw(painter, rect.x(), rect.y(), rect.width(), rect.height(), "PREVIEW", false);
        int left = (rect.x() + 2) * TEXT;
        int rowTop = (rect.y() + 1) * TEXT;
        int cellWidth = editor.cellWidth();
        int cellHeight = editor.cellHeight();
        PixelGlyph glyph = shown != null ? shown : editor.glyph(codePoint);
        for (int scale = 1; scale <= 2; scale++) {
            painter.text(scale + "X", left, rowTop, UiPalette.DIM);
            rowTop += TEXT;
            painter.fill(BACKDROP, left, rowTop, cellWidth * scale, cellHeight * scale);
            if (glyph != null) {
                painter.pixels(glyph.toCell(cellWidth), left, rowTop, scale, Color.WHITE);
            }
            rowTop += cellHeight * scale + SECTION_GAP;
        }
    }
}
