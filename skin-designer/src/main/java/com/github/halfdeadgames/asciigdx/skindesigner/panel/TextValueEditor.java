package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.github.halfdeadgames.asciigdx.ui.TextFieldFilter;
import com.github.halfdeadgames.asciigdx.ui.Widget;

/** Edits a mark, which is free text, in a text field. */
final class TextValueEditor implements KeyEditor {
    private static final int FIELD_CELLS = 12;

    private final EditorContext context;

    TextValueEditor(EditorContext context) {
        this.context = context;
    }

    @Override
    public Widget createControls(KeyRow row) {
        return ControlRow.of(context.widgets().createCommittedField(row.location().describe(),
                FIELD_CELLS,
                row.value().asString(), TextFieldFilter.any(),
                context.textCommit(row.location(), SkinEdits::setText)));
    }
}
