package com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.UiSurface;
import com.github.halfdeadgames.asciigdx.ui.Widget;

/** Solid fill under everything; lets no pointer hit through it. */
public final class ShowcaseBackdrop extends Widget {
    private final int columns;
    private final int rows;
    private final int fillRgba;

    /** Creates a backdrop of the given size in cells, filled with one color. */
    public ShowcaseBackdrop(int columns, int rows, int fillRgba) {
        this.columns = columns;
        this.rows = rows;
        this.fillRgba = fillRgba;
        setSize(columns, rows);
    }

    @Override
    public Widget hit(int column, int row) {
        return null;
    }

    @Override
    public int getPreferredWidth() {
        return columns;
    }

    @Override
    public int getPreferredHeight() {
        return rows;
    }

    @Override
    protected void drawContent(UiSurface target, int left, int top) {
        target.fillBackground(left, top, columns, rows, fillRgba);
    }
}
