package com.github.halfdeadgames.asciigdx.ui;

/** The kinds of style a skin file key can refer to by name. */
enum SkinStyleKind {
    LABEL("label"),
    BUTTON("button"),
    WINDOW("window"),
    TEXT_FIELD("text field");

    private final String displayName;

    SkinStyleKind(String displayName) {
        this.displayName = displayName;
    }

    /** Returns the name used in errors, such as {@code text field}. */
    String getDisplayName() {
        return displayName;
    }

    /** Returns the name of the style, such as {@code text field style}. */
    String getStyleName() {
        return displayName + " style";
    }

    /** Returns the name of the inline object, such as {@code label object}. */
    String getObjectName() {
        return displayName + " object";
    }
}
