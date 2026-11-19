package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * Looks of a {@link CheckBox} or {@link RadioButton}: the {@link ButtonStyle} that gives the text
 * look in each state (normal, pointer over, pressed, disabled, and focused), a mark for the unchecked state, a mark for the checked state, and the gap
 * between the mark and the text. The mark is drawn with the same look as the text. Immutable, so
 * one instance can be shared by any number of widgets.
 *
 * <p>Both marks have the same number of code points, so a widget keeps its width when it is
 * toggled.
 *
 * <p>Check boxes and radio buttons use this same type, so a style made for one works for the
 * other; only the marks usually differ, such as {@code "[ ]"} and {@code "[x]"} for check boxes and
 * {@code "( )"} and {@code "(*)"} for radio buttons.
 */
public final class ToggleStyle {

    /** Gap between the mark and the text of a new style, in UI cells. */
    public static final int DEFAULT_GAP = 1;

    private final ButtonStyle button;
    private final String uncheckedMark;
    private final String checkedMark;
    private final int gap;

    private ToggleStyle(ButtonStyle button, String uncheckedMark, String checkedMark, int gap) {
        this.button = button;
        this.uncheckedMark = uncheckedMark;
        this.checkedMark = checkedMark;
        this.gap = gap;
    }

    /**
     * Creates a style with a gap of {@link #DEFAULT_GAP}. The unchecked mark comes first and the
     * checked mark second:
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
        return new ToggleStyle(button, uncheckedMark, checkedMark, DEFAULT_GAP);
    }

    /**
     * Returns a copy with another gap between the mark and the text.
     *
     * @param cells gap in UI cells; zero or greater
     * @return a style with the same looks and marks and that gap
     * @throws IllegalArgumentException if {@code cells} is negative
     */
    public ToggleStyle withGap(int cells) {
        if (cells < 0) {
            throw new IllegalArgumentException("Gap must not be negative, was " + cells);
        }
        return new ToggleStyle(button, uncheckedMark, checkedMark, cells);
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
     * Returns the number of code points in each mark, which is one UI column per code point. A
     * convenience for layout code; it equals the code point count of either mark.
     *
     * @return the mark length in UI cells, 1 or greater
     */
    public int getMarkLength() {
        return countCodePoints(uncheckedMark);
    }

    /**
     * Returns the space between the mark and the text.
     *
     * @return the gap in UI cells, zero or greater
     */
    public int getGap() {
        return gap;
    }

    /**
     * Returns the font shared by all looks. A convenience for {@code getButtonStyle().getFont()}.
     *
     * @return the font, never {@code null}
     */
    public UiFont getFont() {
        return button.getFont();
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
        return gap == that.gap && button.equals(that.button)
                && uncheckedMark.equals(that.uncheckedMark)
                && checkedMark.equals(that.checkedMark);
    }

    @Override
    public int hashCode() {
        return Objects.hash(button, uncheckedMark, checkedMark, gap);
    }

    @Override
    public String toString() {
        return "ToggleStyle[button=" + button + ", uncheckedMark=" + uncheckedMark
                + ", checkedMark=" + checkedMark + ", gap=" + gap + "]";
    }
}
