package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;

/** Starts, continues, and ends a paint stroke on the pixel grid. */
final class GridStrokePointerTarget implements PointerTarget {

    /** What starting a stroke needs from the paint screen. */
    interface StrokeContext {

        int cursorCodePoint();

        void clearLastProblem();
    }

    private final PaintGridView grid;
    private final PaintStroke stroke;
    private final DeriveFlow flow;
    private final MessageStrip messages;
    private final FontEditor editor;
    private final StrokeContext start;

    GridStrokePointerTarget(PaintGridView grid, PaintStroke stroke, DeriveFlow flow, MessageStrip messages,
                            FontEditor editor, StrokeContext start) {
        this.grid = grid;
        this.stroke = stroke;
        this.flow = flow;
        this.messages = messages;
        this.editor = editor;
        this.start = start;
    }

    @Override
    public boolean claimPress(int x, int y) {
        if (flow.isPicking()) {
            return false;
        }
        int column = grid.columnAt(x);
        int row = grid.rowAt(y);
        if (column < 0 || column >= editor.cellWidth() || row < 0 || row >= editor.cellHeight()) {
            return false;
        }
        messages.nextAction();
        start.clearLastProblem();
        stroke.begin(start.cursorCodePoint(), column, row);
        return true;
    }

    @Override
    public boolean isCaptured() {
        return stroke.isActive();
    }

    @Override
    public void drag(int x, int y) {
        stroke.drag(grid.columnAt(x), grid.rowAt(y));
    }

    @Override
    public void release() {
        stroke.end();
    }
}
