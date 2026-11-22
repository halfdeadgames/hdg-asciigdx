package com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.UiSurface;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;

/**
 * EXPERIMENT: wraps one demo widget and draws a one-cell ring of red cells just outside its
 * box. The frame is one cell larger than the widget on every side, so the layout keeps the same
 * space whether the ring is shown or hidden.
 */
final class BoundaryFrame extends WidgetGroup {
    static final int RING_CELLS = BoundaryRing.RING_CELLS;

    private final Widget widget;
    private final BoundaryState state;

    BoundaryFrame(Widget widget, BoundaryState state) {
        this.widget = widget;
        this.state = state;
        widget.setPosition(RING_CELLS, RING_CELLS);
        addChild(widget);
        pack();
        state.addFrame(this);
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
        if (!state.isShown()) {
            return;
        }
        BoundaryRing.draw(target, left, top, getPreferredWidth(), getPreferredHeight(),
                state.getRgba());
    }

    private static int widthOf(Widget widget) {
        return widget.getWidth() > 0 ? widget.getWidth() : widget.getPreferredWidth();
    }

    private static int heightOf(Widget widget) {
        return widget.getHeight() > 0 ? widget.getHeight() : widget.getPreferredHeight();
    }
}
