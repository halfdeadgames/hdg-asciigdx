package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.github.halfdeadgames.asciigdx.ui.Widget;

/** Edits a true-or-false key with a check box. */
final class FlagEditor implements KeyEditor {
    private static final String CHECK_BOX_TEXT = "on";

    private final EditorContext context;

    FlagEditor(EditorContext context) {
        this.context = context;
    }

    @Override
    public Widget createControls(KeyRow row) {
        KeyLocation location = row.location();
        return ControlRow.of(context.widgets().createCheckBox(CHECK_BOX_TEXT,
                row.value().asBoolean(),
                checked -> context.applyClickEdit(location, SkinEdits.setFlag(location, checked))));
    }
}
