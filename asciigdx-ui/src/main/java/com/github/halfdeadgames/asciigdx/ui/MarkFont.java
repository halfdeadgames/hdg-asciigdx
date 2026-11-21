package com.github.halfdeadgames.asciigdx.ui;

/**
 * The font a small drawn piece, such as the mark of a check box or the close mark of a window,
 * is written in. The choice changes how much room each code point of the piece takes, so it
 * changes the width of the widget that holds it.
 *
 * <p>Used by {@link ToggleStyle#withMarkFont(MarkFont)} and
 * {@link WindowStyle#withCloseFont(MarkFont)}. In a skin file the constants are named in
 * lowercase, {@code "text"} and {@code "square"}, by the key {@code markFont} of a
 * {@code checkBoxes} or {@code radioButtons} entry and by the key {@code closeFont} of a
 * {@code windows} entry.
 *
 * <p>Example:
 * <pre>{@code
 * ToggleStyle style = ToggleStyle.of(buttonStyle, "[ ]", "[x]").withMarkFont(MarkFont.SQUARE);
 * }</pre>
 *
 * <p>This enum may gain constants in a later version, so callers should not rely on it having
 * exactly two values.
 */
public enum MarkFont {

    /**
     * The text font. Each code point takes one text column, which is half a UI cell, so two code
     * points fit in one cell.
     */
    TEXT(1),

    /**
     * The square font. Each code point takes one UI cell, which is two text columns, so a piece
     * of this font is twice as wide as the same piece in {@link #TEXT}.
     */
    SQUARE(UiSurface.TEXT_COLUMNS_PER_CELL);

    private final int textColumnsPerCodePoint;

    MarkFont(int textColumnsPerCodePoint) {
        this.textColumnsPerCodePoint = textColumnsPerCodePoint;
    }

    int getTextColumnsPerCodePoint() {
        return textColumnsPerCodePoint;
    }
}
