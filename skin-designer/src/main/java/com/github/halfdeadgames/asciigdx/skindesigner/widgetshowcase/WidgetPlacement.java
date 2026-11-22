package com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.Widget;

/** Finds where a widget sits on the surface, for windows that open next to a button. */
final class WidgetPlacement {
    private WidgetPlacement() {
    }

    static int surfaceColumn(Widget widget) {
        int column = 0;
        for (Widget current = widget; current != null; current = current.getParent()) {
            column += current.getX();
        }
        return column;
    }

    static int surfaceRow(Widget widget) {
        int row = 0;
        for (Widget current = widget; current != null; current = current.getParent()) {
            row += current.getY();
        }
        return row;
    }
}
