package com.github.halfdeadgames.asciigdx.ui;

/** The kinds of style a skin file key can refer to by name. */
public enum SkinStyleKind {
    /** A label style, from the {@code labels} section. */
    LABEL("label"),
    /** A button style, from the {@code buttons} section. */
    BUTTON("button"),
    /** A window style, from the {@code windows} section. */
    WINDOW("window"),
    /** A text field style, from the {@code textFields} section. */
    TEXT_FIELD("text field"),
    /** A scroll pane style, from the {@code scrollPanes} section. */
    SCROLL_PANE("scroll pane");

    private final String displayName;

    SkinStyleKind(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the name used in errors, such as {@code text field}.
     *
     * @return the lower-case name, never {@code null}
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns wording for messages that names the style, such as {@code text field style}. The
     * exact wording is display text, not a stable format.
     *
     * @return the display name followed by {@code " style"}, never {@code null}
     */
    public String getStylePhrase() {
        return displayName + " style";
    }

    /**
     * Returns wording for messages that names the inline object, such as
     * {@code label object}. The exact wording is display text, not a stable format.
     *
     * @return the display name followed by {@code " object"}, never {@code null}
     */
    public String getObjectPhrase() {
        return displayName + " object";
    }
}
