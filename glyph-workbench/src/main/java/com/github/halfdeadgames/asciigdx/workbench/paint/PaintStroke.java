package com.github.halfdeadgames.asciigdx.workbench.paint;

import com.github.halfdeadgames.asciigdx.workbench.editor.FontEditor;

/**
 * One mouse stroke: the first pixel toggles, and every pixel dragged over gets the colour that
 * first pixel turned into. Fast drags are filled in with a straight line between mouse samples.
 * The whole stroke is one undo step.
 */
final class PaintStroke {

    private final FontEditor editor;
    private boolean active;
    private int codePoint;
    private boolean paintLit;
    private int lastX;
    private int lastY;

    PaintStroke(FontEditor editor) {
        this.editor = editor;
    }

    boolean isActive() {
        return active;
    }

    /** Starts a stroke on a pixel; stays inactive when that pixel cannot be painted. */
    void begin(int codePoint, int x, int y) {
        if (!editor.isPaintable(codePoint, x, y)) {
            return;
        }
        this.codePoint = codePoint;
        editor.beginEdit();
        paintLit = !editor.isLit(codePoint, x, y);
        editor.setPixel(codePoint, x, y, paintLit);
        lastX = x;
        lastY = y;
        active = true;
    }

    void drag(int x, int y) {
        if (!active) {
            return;
        }
        int dx = Math.abs(x - lastX);
        int dy = Math.abs(y - lastY);
        int stepX = lastX < x ? 1 : -1;
        int stepY = lastY < y ? 1 : -1;
        int error = dx - dy;
        int currentX = lastX;
        int currentY = lastY;
        while (currentX != x || currentY != y) {
            int doubled = 2 * error;
            if (doubled > -dy) {
                error -= dy;
                currentX += stepX;
            }
            if (doubled < dx) {
                error += dx;
                currentY += stepY;
            }
            editor.setPixel(codePoint, currentX, currentY, paintLit);
        }
        lastX = x;
        lastY = y;
    }

    void end() {
        if (active) {
            editor.commitEdit();
        }
        active = false;
    }
}
