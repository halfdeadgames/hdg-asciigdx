package com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase;

import com.github.halfdeadgames.asciigdx.layer.Layer;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.List;

/** One widget class shown in every style and form of the skin, in the "UI: showcase" scene. */
public interface WidgetExample {
    /** Lowercase identifier, used in capture file names. */
    String id();

    /** Name shown in the picker and the status line. */
    String title();

    /**
     * Names the skin file sections whose styles the example draws, using the constants of
     * {@code SkinSchema}. The side panel offers the styles of these sections for editing.
     */
    List<String> skinSections();

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

    /**
     * Called every frame while the example is shown, after the widgets are drawn and before the
     * layers are drawn, so an example can apply cell effects to the text layer. Does nothing by
     * default.
     *
     * @param textLayer the layer that draws the text grid of the surface
     * @param seconds time since the scene started, in seconds
     */
    default void applyEffects(Layer textLayer, float seconds) {
    }
}
