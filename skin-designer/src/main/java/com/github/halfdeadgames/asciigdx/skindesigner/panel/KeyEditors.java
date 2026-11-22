package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.github.halfdeadgames.asciigdx.ui.Widget;

/** Picks the editor for each kind of key. */
final class KeyEditors {
    private static final String READ_ONLY_TEXT = "(not editable here)";

    private final EditorContext context;
    private final KeyEditor color;
    private final KeyEditor glyph;
    private final KeyEditor mark;
    private final KeyEditor number;
    private final KeyEditor flag;
    private final KeyEditor choice;
    private final KeyEditor styleReference;

    KeyEditors(EditorContext context) {
        this.context = context;
        color = new ColorEditor(context);
        glyph = new GlyphEditor(context);
        mark = new TextValueEditor(context);
        number = new NumberEditor(context);
        flag = new FlagEditor(context);
        choice = new ChoiceEditor(context);
        styleReference = new StyleReferenceEditor(context);
    }

    /** Returns the editor for a kind; groups are laid out by the caller and have no editor. */
    KeyEditor forKind(KeyEditorKind kind) {
        return switch (kind) {
            case COLOR -> color;
            case GLYPH -> glyph;
            case MARK -> mark;
            case WHOLE_NUMBER, DECIMAL_NUMBER -> number;
            case FLAG -> flag;
            case CHOICE -> choice;
            case STYLE_REFERENCE -> styleReference;
            case GROUP, UNSUPPORTED -> this::createReadOnlyControls;
        };
    }

    private Widget createReadOnlyControls(KeyRow row) {
        return ControlRow.of(context.widgets().createLabel(READ_ONLY_TEXT));
    }
}
