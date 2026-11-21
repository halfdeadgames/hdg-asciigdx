package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * Looks of a {@link CheckBox} or {@link RadioButton}: the {@link ButtonStyle} that gives the look
 * in each state (normal, pointer over, pressed, disabled, and focused), a mark for the unchecked
 * state, a mark for the checked state, the gap between the mark and the text, and the font of the
 * mark. The mark is drawn with the same look as the text, followed by the gap and the words. By
 * default the mark is in the text font, one text column (half a UI cell) per code point; with
 * {@link MarkFont#SQUARE} it is in the square font, one UI cell (two text columns) per code
 * point. The words are always in the text font. Immutable, so one instance can be shared by any
 * number of widgets.
 *
 * <p>Both marks have the same number of code points, so a widget keeps its width when it is
 * toggled.
 *
 * <p>Check boxes and radio buttons use this same type, so a style made for one works for the
 * other; only the marks usually differ, such as {@code "[ ]"} and {@code "[x]"} for check boxes and
 * {@code "( )"} and {@code "(*)"} for radio buttons.
 */
public final class ToggleStyle {

    /** Gap between the mark and the text of a new style, in text columns (half a UI cell). */
    public static final int DEFAULT_GAP = 1;

    private final ButtonStyle button;
    private final String uncheckedMark;
    private final String checkedMark;
    private final int gap;
    private final MarkFont markFont;

    private ToggleStyle(ButtonStyle button, String uncheckedMark, String checkedMark, int gap,
            MarkFont markFont) {
        this.button = button;
        this.uncheckedMark = uncheckedMark;
        this.checkedMark = checkedMark;
        this.gap = gap;
        this.markFont = markFont;
    }

    /**
     * Creates a style with a gap of {@link #DEFAULT_GAP} and a mark in the text font
     * ({@link MarkFont#TEXT}). The unchecked mark comes first and the checked mark second:
     * <pre>{@code
     * ToggleStyle style = ToggleStyle.of(buttonStyle, "[ ]", "[x]");
     * // unchecked shows "[ ] Sound", checked shows "[x] Sound"
     * }</pre>
     *
     * @param button text looks per state and the horizontal padding; must not be {@code null}
     * @param uncheckedMark text drawn while unchecked, such as {@code "[ ]"}; must not be
     *         {@code null} or empty
     * @param checkedMark text drawn while checked, such as {@code "[x]"}; must not be {@code null}
     *         and must have the same number of code points as {@code uncheckedMark}
     * @return a new style
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if a mark is empty or the marks differ in code point count
     */
    public static ToggleStyle of(ButtonStyle button, String uncheckedMark, String checkedMark) {
        Objects.requireNonNull(button, "button");
        Objects.requireNonNull(uncheckedMark, "uncheckedMark");
        Objects.requireNonNull(checkedMark, "checkedMark");
        int uncheckedLength = countCodePoints(uncheckedMark);
        int checkedLength = countCodePoints(checkedMark);
        if (uncheckedLength == 0) {
            throw new IllegalArgumentException("A mark needs at least one code point");
        }
        if (uncheckedLength != checkedLength) {
            throw new IllegalArgumentException("Both marks need the same number of code points, "
                    + "were " + uncheckedLength + " and " + checkedLength);
        }
        return new ToggleStyle(button, uncheckedMark, checkedMark, DEFAULT_GAP, MarkFont.TEXT);
    }

    /**
     * Returns a copy with another gap between the mark and the text.
     *
     * @param columns gap in text columns (half a UI cell); zero or greater
     * @return a style with the same looks, marks, and mark font and that gap
     * @throws IllegalArgumentException if {@code columns} is negative
     */
    public ToggleStyle withGap(int columns) {
        if (columns < 0) {
            throw new IllegalArgumentException("Gap must not be negative, was " + columns);
        }
        return new ToggleStyle(button, uncheckedMark, checkedMark, columns, markFont);
    }

    /**
     * Returns a copy with another font for the mark. The gap and the words stay in text columns
     * and in the text font. A square mark starts at a cell boundary, because the padding is a
     * whole number of cells. {@link CheckBox#setStyle(ToggleStyle)} and
     * {@link RadioButton#setStyle(ToggleStyle)} apply a new mark font and invalidate the layout,
     * because the mark width changes; the widget keeps its size until it is packed.
     *
     * @param markFont font of the mark; must not be {@code null}
     * @return a style with the same looks, marks, and gap and that mark font
     * @throws NullPointerException if {@code markFont} is {@code null}
     */
    public ToggleStyle withMarkFont(MarkFont markFont) {
        Objects.requireNonNull(markFont, "markFont");
        return new ToggleStyle(button, uncheckedMark, checkedMark, gap, markFont);
    }

    /**
     * Returns the button style that gives the looks per state and the horizontal padding.
     *
     * @return the button style, never {@code null}
     */
    public ButtonStyle getButtonStyle() {
        return button;
    }

    /**
     * Returns the mark drawn while unchecked.
     *
     * @return the mark, never {@code null} or empty
     */
    public String getUncheckedMark() {
        return uncheckedMark;
    }

    /**
     * Returns the mark drawn while checked.
     *
     * @return the mark, never {@code null} or empty
     */
    public String getCheckedMark() {
        return checkedMark;
    }

    /**
     * Returns the font of the mark.
     *
     * @return the font, never {@code null}; {@link MarkFont#TEXT} unless changed
     */
    public MarkFont getMarkFont() {
        return markFont;
    }

    /**
     * Returns the width of the mark in text columns (half a UI cell each), not in code points.
     * With {@link MarkFont#TEXT} that is the code point count of either mark; with
     * {@link MarkFont#SQUARE} it is twice that count, because each code point takes one UI cell.
     * For example, the mark {@code "[ ]"} gives 3 for TEXT and 6 for SQUARE. A convenience for
     * layout code.
     *
     * @return the mark length in text columns, 1 or greater
     */
    public int getMarkLength() {
        return markFont.getTextColumnsPerCodePoint() * countCodePoints(uncheckedMark);
    }

    /**
     * Returns the space between the mark and the text.
     *
     * @return the gap in text columns (half a UI cell), zero or greater
     */
    public int getGap() {
        return gap;
    }

    private static int countCodePoints(String text) {
        return text.codePointCount(0, text.length());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ToggleStyle)) {
            return false;
        }
        ToggleStyle that = (ToggleStyle) other;
        return gap == that.gap && markFont == that.markFont && button.equals(that.button)
                && uncheckedMark.equals(that.uncheckedMark)
                && checkedMark.equals(that.checkedMark);
    }

    @Override
    public int hashCode() {
        return Objects.hash(button, uncheckedMark, checkedMark, gap, markFont);
    }

    @Override
    public String toString() {
        return "ToggleStyle[button=" + button + ", uncheckedMark=" + uncheckedMark
                + ", checkedMark=" + checkedMark + ", gap=" + gap + ", markFont=" + markFont
                + "]";
    }
}
