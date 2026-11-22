package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.github.halfdeadgames.asciigdx.ui.Table;
import com.github.halfdeadgames.asciigdx.ui.Widget;

/** Lays controls out side by side with one empty cell between them. */
final class ControlRow {
    private static final int GAP_CELLS = 1;

    private ControlRow() {
    }

    static Table of(Widget... controls) {
        Table row = new Table();
        for (int i = 0; i < controls.length; i++) {
            row.add(controls[i]).padLeft(i == 0 ? 0 : GAP_CELLS);
        }
        row.pack();
        return row;
    }
}
