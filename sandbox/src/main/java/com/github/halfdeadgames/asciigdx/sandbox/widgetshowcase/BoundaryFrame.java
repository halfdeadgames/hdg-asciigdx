package com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.UiSurface;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;

/**
 * EXPERIMENT: wraps one demo widget and draws a one-cell ring of red cells just outside its
 * box. The frame is one cell larger than the widget on every side, so the layout keeps the same
 * space whether the ring is shown or hidden.
 */
final class BoundaryFrame extends WidgetGroup {
    static final int RING_CELLS = 1;
    private static final int CORNER_GLYPH = '#';
    private static final int ODD_GLYPH = '+';
    private static final int EVEN_GLYPH = '-';
    private static final int GLYPH_RGBA = 0x000000FF;

    private final Widget widget;

    BoundaryFrame(Widget widget) {
        this.widget = widget;
        widget.setPosition(RING_CELLS, RING_CELLS);
        addChild(widget);
        pack();
        WidgetBoundary.register(this);
    }

    @Override
    public int getPreferredWidth() {
        return widthOf(widget) + 2 * RING_CELLS;
    }

    @Override
    public int getPreferredHeight() {
        return heightOf(widget) + 2 * RING_CELLS;
    }

    @Override
    protected void layout() {
        setSize(getPreferredWidth(), getPreferredHeight());
    }

    @Override
    protected void drawContent(UiSurface target, int left, int top) {
        super.drawContent(target, left, top);
        if (!WidgetBoundary.isShown()) {
            return;
        }
        int width = widthOf(widget) + 2 * RING_CELLS;
        int height = heightOf(widget) + 2 * RING_CELLS;
        int rgba = WidgetBoundary.currentRgba();
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

    private static int widthOf(Widget widget) {
        return widget.getWidth() > 0 ? widget.getWidth() : widget.getPreferredWidth();
    }

    private static int heightOf(Widget widget) {
        return widget.getHeight() > 0 ? widget.getHeight() : widget.getPreferredHeight();
    }
}
