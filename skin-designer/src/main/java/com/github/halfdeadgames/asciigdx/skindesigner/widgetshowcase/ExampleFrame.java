package com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.ScrollPane;
import com.github.halfdeadgames.asciigdx.ui.ScrollPaneStyle;
import com.github.halfdeadgames.asciigdx.ui.Widget;

/** Fits an example's tree to the example area. */
public final class ExampleFrame {
    private static final int WHEEL_STEP_CELLS = 3;

    private ExampleFrame() {
    }

    /**
     * Returns the tree itself when it fits the area, or a scroll pane of exactly the area's size
     * around it when it does not.
     */
    public static Widget fit(Widget content, int columns, int rows, ScrollPaneStyle paneStyle) {
        boolean fits = content.getPreferredWidth() <= columns
                && content.getPreferredHeight() <= rows;
        if (fits) {
            return content;
        }
        ScrollPane pane = new ScrollPane(content, paneStyle);
        pane.setSize(columns, rows);
        pane.setWheelStepCells(WHEEL_STEP_CELLS);
        return pane;
    }
}
