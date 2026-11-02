package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.graphics.Color;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;

/** Live 1x and 2x preview of the selected glyph's whole cell, at real font pixel size. */
final class PaintPreview {

    static final int CELLS_WIDE = 10;
    private static final int TEXT = Painter.TEXT_SIZE;
    private static final int SECTION_GAP = 12;

    private static final Color FRAME_COLOR = new Color(0.32f, 0.38f, 0.55f, 1f);
    private static final Color BACKDROP = new Color(0.14f, 0.14f, 0.18f, 1f);
    private static final Color LABEL_COLOR = new Color(0.8f, 0.6f, 1f, 1f);

    private final Painter painter;
    private final FontEditor editor;

    PaintPreview(Painter painter, FontEditor editor) {
        this.painter = painter;
        this.editor = editor;
    }

    /** Draws the frame and both previews; returns the next free row below the frame. */
    int draw(int x, int top, int codePoint) {
        int cellWidth = editor.cellWidth();
        int cellHeight = editor.cellHeight();
        int contentHeight = TEXT + cellHeight + SECTION_GAP + TEXT + 2 * cellHeight;
        int cellsHigh = 2 + Painter.roundUpToCells(contentHeight) / TEXT;
        painter.text("PREVIEW", x, top, LABEL_COLOR);
        top += TEXT;
        painter.frame(FRAME_COLOR, x, top, CELLS_WIDE, cellsHigh);
        int innerLeft = x + TEXT;
        int rowTop = top + TEXT;
        PixelGlyph glyph = editor.glyph(codePoint);
        for (int scale = 1; scale <= 2; scale++) {
            painter.text(scale + "X", innerLeft, rowTop, LABEL_COLOR);
            rowTop += TEXT;
            painter.fill(BACKDROP, innerLeft, rowTop, cellWidth * scale, cellHeight * scale);
            if (glyph != null) {
                painter.pixels(glyph.toCell(cellWidth), innerLeft, rowTop, scale, Color.WHITE);
            }
            rowTop += cellHeight * scale + SECTION_GAP;
        }
        return top + cellsHigh * TEXT;
    }
}
