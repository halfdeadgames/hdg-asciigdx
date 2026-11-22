package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.github.halfdeadgames.asciigdx.ui.SkinKeyType;

/** Which editor the panel shows for a key, decided by the type of the key's value. */
enum KeyEditorKind {
    COLOR,
    GLYPH,
    MARK,
    WHOLE_NUMBER,
    DECIMAL_NUMBER,
    FLAG,
    CHOICE,
    STYLE_REFERENCE,
    GROUP,
    /** A key type added to the library after this panel was written; it is shown read only. */
    UNSUPPORTED;

    static KeyEditorKind forType(SkinKeyType type) {
        if (type instanceof SkinKeyType.Color) {
            return COLOR;
        }
        if (type instanceof SkinKeyType.Glyph) {
            return GLYPH;
        }
        if (type instanceof SkinKeyType.Mark) {
            return MARK;
        }
        if (type instanceof SkinKeyType.WholeNumber) {
            return WHOLE_NUMBER;
        }
        if (type instanceof SkinKeyType.DecimalNumber) {
            return DECIMAL_NUMBER;
        }
        if (type instanceof SkinKeyType.Flag) {
            return FLAG;
        }
        if (type instanceof SkinKeyType.Choice) {
            return CHOICE;
        }
        if (type instanceof SkinKeyType.StyleReference) {
            return STYLE_REFERENCE;
        }
        if (type instanceof SkinKeyType.Group) {
            return GROUP;
        }
        return UNSUPPORTED;
    }
}
