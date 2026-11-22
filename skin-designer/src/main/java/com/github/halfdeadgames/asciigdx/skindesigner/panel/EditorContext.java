package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.badlogic.gdx.utils.JsonValue;
import com.github.halfdeadgames.asciigdx.skindesigner.panel.CommittedTextField.TextCommit;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * What every key editor needs: the widget factory, the tree as it was when the panel was built,
 * and the gateway that applies edits. The tree is only read, to list names to choose from; it
 * goes stale after any edit, and the panel is rebuilt then.
 */
record EditorContext(PanelWidgets widgets, JsonValue tree, EditGateway gateway) {

    /** Applies an edit made by a click; a refused edit changes nothing, so nothing is put back. */
    void applyClickEdit(KeyLocation location, Consumer<JsonValue> edit) {
        gateway.apply(location.describe(), edit, () -> { });
    }

    /** Returns a commit handler that turns the committed text into an edit and applies it. */
    TextCommit textCommit(KeyLocation location,
            BiFunction<KeyLocation, String, Consumer<JsonValue>> editForText) {
        return (text, revertField) -> gateway.apply(location.describe(),
                editForText.apply(location, text), revertField);
    }
}
