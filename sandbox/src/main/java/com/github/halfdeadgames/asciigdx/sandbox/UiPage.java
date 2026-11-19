package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.ui.Widget;

/** One page of the "UI: widgets" scene: a widget tree shown below the menu strip. */
interface UiPage {
    /** Lowercase identifier, used in capture file names. */
    String id();

    String title();

    /**
     * Builds the page's widget tree for the area below the reserved rows.
     *
     * @param columns width of the area in square cells
     * @param rows height of the area in square cells
     */
    Widget createContent(int columns, int rows);
}
