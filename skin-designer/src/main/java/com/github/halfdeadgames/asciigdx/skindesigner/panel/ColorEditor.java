package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.badlogic.gdx.utils.JsonValue;
import com.github.halfdeadgames.asciigdx.ui.SkinSchema;
import com.github.halfdeadgames.asciigdx.ui.TextButton;
import com.github.halfdeadgames.asciigdx.ui.TextField;
import com.github.halfdeadgames.asciigdx.ui.TextFieldFilter;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.ArrayList;
import java.util.List;

/** Edits a color: a button that cycles through the palette, and a field for hex digits. */
final class ColorEditor implements KeyEditor {
    private static final String LITERAL_COLOR_TEXT = "custom";
    private static final int HEX_FIELD_CELLS = 5;

    private final EditorContext context;

    ColorEditor(EditorContext context) {
        this.context = context;
    }

    @Override
    public Widget createControls(KeyRow row) {
        KeyLocation location = row.location();
        String current = row.value().asString();
        boolean isLiteral = current.startsWith("#");
        TextButton paletteButton = context.widgets().createCycleButton(paletteNames(),
                current, isLiteral ? LITERAL_COLOR_TEXT : current,
                chosenName -> context.applyClickEdit(location,
                        SkinEdits.setText(location, chosenName)));
        TextField hexField = context.widgets().createCommittedField(location.describe(),
                HEX_FIELD_CELLS,
                HexColorText.digitsOf(context.tree(), current), TextFieldFilter.any(),
                context.textCommit(location, SkinEdits::setHexColor));
        return ControlRow.of(paletteButton, hexField);
    }

    private List<String> paletteNames() {
        List<String> names = new ArrayList<>();
        JsonValue palette = context.tree().get(SkinSchema.COLORS);
        if (palette == null) {
            return names;
        }
        for (JsonValue color = palette.child; color != null; color = color.next) {
            names.add(color.name);
        }
        return names;
    }
}
