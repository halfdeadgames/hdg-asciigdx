package com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.Widget;

/** One widget class shown in every style and form of the skin, in the "UI: showcase" scene. */
public interface WidgetExample {
    /** Lowercase identifier, used in capture file names. */
    String id();

    /** Name shown in the picker and the status line. */
    String title();

    /**
     * Builds the example's widget tree at position (0, 0), without a parent.
     *
     * @param skin the skin to take every style from
     * @param uiRoot the root the tree will live in; needed by widgets that register with it
     * @param boundaryState the state the demo frames register with and follow
     * @param columns width of the example area in square cells
     * @param rows height of the example area in square cells; the scene wraps a bigger tree in a
     *         scroll pane
     */
    Widget create(Skin skin, UiRoot uiRoot, BoundaryState boundaryState, int columns, int rows);

    /**
     * Called before the tree is replaced or the whole UI is rebuilt, so an example that put
     * widgets outside its tree (such as in a layer of the root) can remove them. Does nothing by
     * default.
     */
    default void exampleHidden() {
    }
}
