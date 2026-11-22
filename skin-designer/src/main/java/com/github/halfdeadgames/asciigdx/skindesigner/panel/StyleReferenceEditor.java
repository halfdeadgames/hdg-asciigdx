package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.github.halfdeadgames.asciigdx.ui.SkinKeyType;
import com.github.halfdeadgames.asciigdx.ui.SkinSchema;
import com.github.halfdeadgames.asciigdx.ui.SkinStyleKind;
import com.github.halfdeadgames.asciigdx.ui.Widget;

/**
 * Edits a reference to a style by name, with a button that cycles through the styles of that
 * kind. A style written in place (an inline object) has no editor here; the style table lays its
 * keys out as nested rows.
 */
final class StyleReferenceEditor implements KeyEditor {

    private final EditorContext context;

    StyleReferenceEditor(EditorContext context) {
        this.context = context;
    }

    @Override
    public Widget createControls(KeyRow row) {
        KeyLocation location = row.location();
        SkinKeyType.StyleReference type = (SkinKeyType.StyleReference) row.definition().type();
        String current = row.value().asString();
        return ControlRow.of(context.widgets().createCycleButton(
                SeedValues.namesIn(context.tree(), sectionOf(type.kind())), current, current,
                chosen -> context.applyClickEdit(location,
                        SkinEdits.setText(location, chosen))));
    }

    static String sectionOf(SkinStyleKind kind) {
        return switch (kind) {
            case LABEL -> SkinSchema.LABELS;
            case BUTTON -> SkinSchema.BUTTONS;
            case WINDOW -> SkinSchema.WINDOWS;
            case TEXT_FIELD -> SkinSchema.TEXT_FIELDS;
        };
    }
}
