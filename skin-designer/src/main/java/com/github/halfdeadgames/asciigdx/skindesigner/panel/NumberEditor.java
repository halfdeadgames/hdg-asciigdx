package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.badlogic.gdx.utils.JsonValue;
import com.github.halfdeadgames.asciigdx.ui.SkinKeyType;
import com.github.halfdeadgames.asciigdx.ui.TextFieldFilter;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.function.Consumer;

/** Edits a whole or decimal number in a text field that accepts only the characters of a number. */
final class NumberEditor implements KeyEditor {
    private static final int FIELD_CELLS = 8;

    private final EditorContext context;

    NumberEditor(EditorContext context) {
        this.context = context;
    }

    @Override
    public Widget createControls(KeyRow row) {
        SkinKeyType type = row.definition().type();
        boolean isWhole = type instanceof SkinKeyType.WholeNumber;
        TextFieldFilter filter = isWhole ? TextFieldFilter.digits() : TextFieldFilter.decimals();
        return ControlRow.of(context.widgets().createCommittedField(row.location().describe(),
                FIELD_CELLS,
                row.value().asString(), filter, context.textCommit(row.location(),
                        (location, text) -> editFor(type, location, text))));
    }

    private static Consumer<JsonValue> editFor(SkinKeyType type, KeyLocation location,
            String text) {
        if (type instanceof SkinKeyType.WholeNumber whole) {
            return SkinEdits.setWholeNumber(location, text, whole.minimum());
        }
        SkinKeyType.DecimalNumber decimal = (SkinKeyType.DecimalNumber) type;
        return SkinEdits.setDecimalNumber(location, text, decimal.minimum(), decimal.maximum());
    }
}
