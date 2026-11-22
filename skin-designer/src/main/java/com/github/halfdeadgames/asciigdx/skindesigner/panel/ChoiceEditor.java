package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.github.halfdeadgames.asciigdx.ui.SkinKeyType;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.List;

/** Edits a key that takes one of a fixed list of words, with a drop-down of them. */
final class ChoiceEditor implements KeyEditor {
    private final EditorContext context;

    ChoiceEditor(EditorContext context) {
        this.context = context;
    }

    @Override
    public Widget createControls(KeyRow row) {
        KeyLocation location = row.location();
        List<String> options = ((SkinKeyType.Choice) row.definition().type()).options();
        String current = row.value().asString();
        return ControlRow.of(context.widgets().createChoiceBox(options, current,
                chosen -> context.applyClickEdit(location, SkinEdits.setText(location, chosen))));
    }
}
