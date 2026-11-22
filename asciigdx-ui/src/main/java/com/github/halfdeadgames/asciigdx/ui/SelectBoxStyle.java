package com.github.halfdeadgames.asciigdx.ui;

import java.util.Locale;
import java.util.Objects;

/**
 * Looks of a {@link SelectBox}: a {@link ButtonStyle} for the closed box, a {@link ButtonStyle}
 * for the rows of the open list, a {@link ScrollPaneStyle} for the list's scroll bar, and the arrow
 * drawn in the far-right cell of the closed box. Immutable, so one instance can be shared by any
 * number of select boxes.
 *
 * <p>The closed box uses all five looks of its button style (normal, pointer over, pressed,
 * disabled, and focused): the pressed look shows while the list is open. A row uses the normal
 * look, the pointer-over look for the highlighted row, and the pressed look while it is pressed;
 * the disabled and focused looks of the list style are not used. The horizontal padding of each
 * button style is the space left and right of the text in the box and in a row.
 *
 * <p>The arrow is one code point, {@link #DEFAULT_ARROW_GLYPH} (a down-pointing triangle) by
 * default, drawn in the square font by default (see {@link #withArrowFont(MarkFont)}). It takes
 * the text color of the box's current look.
 *
 * <p>Example:
 * <pre>{@code
 * SelectBoxStyle style = SelectBoxStyle.of(boxStyle, rowStyle, paneStyle)
 *         .withArrowGlyph('v');
 * }</pre>
 */
public final class SelectBoxStyle {

    /** The default arrow glyph, a black down-pointing triangle ({@code U+25BC}), as a code point. */
    public static final int DEFAULT_ARROW_GLYPH = 0x25BC;

    private final ButtonStyle boxStyle;
    private final ButtonStyle rowStyle;
    private final ScrollPaneStyle scrollPane;
    private final int arrowGlyph;
    private final MarkFont arrowFont;

    private SelectBoxStyle(ButtonStyle boxStyle, ButtonStyle rowStyle,
            ScrollPaneStyle scrollPane, int arrowGlyph, MarkFont arrowFont) {
        this.boxStyle = boxStyle;
        this.rowStyle = rowStyle;
        this.scrollPane = scrollPane;
        this.arrowGlyph = arrowGlyph;
        this.arrowFont = arrowFont;
    }

    /**
     * Creates a style with the default arrow glyph ({@link #DEFAULT_ARROW_GLYPH}) in the square
     * font.
     *
     * @param boxStyle looks and padding of the closed box; must not be {@code null}
     * @param rowStyle looks and padding of the rows in the open list; must not be {@code null}
     * @param scrollPane colors of the list's scroll bar; must not be {@code null}
     * @return a new style
     * @throws NullPointerException if an argument is {@code null}
     */
    public static SelectBoxStyle of(ButtonStyle boxStyle, ButtonStyle rowStyle,
            ScrollPaneStyle scrollPane) {
        return new SelectBoxStyle(Objects.requireNonNull(boxStyle, "boxStyle"),
                Objects.requireNonNull(rowStyle, "rowStyle"),
                Objects.requireNonNull(scrollPane, "scrollPane"), DEFAULT_ARROW_GLYPH,
                MarkFont.SQUARE);
    }

    /**
     * Returns a copy with another arrow glyph.
     *
     * @param codePoint Unicode code point of the arrow, which the font set by
     *         {@link #withArrowFont(MarkFont)} must hold
     * @return a style with the same looks and font and that arrow
     * @throws IllegalArgumentException if {@code codePoint} is not a valid Unicode code point
     */
    public SelectBoxStyle withArrowGlyph(int codePoint) {
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("Not a valid code point: " + codePoint);
        }
        return new SelectBoxStyle(boxStyle, rowStyle, scrollPane, codePoint, arrowFont);
    }

    /**
     * Returns a copy that draws the arrow in another font. {@link MarkFont#SQUARE} (the default)
     * draws it as one whole cell; {@link MarkFont#TEXT} draws it in the left half of the cell.
     *
     * @param font the font of the arrow; must not be {@code null}
     * @return a style with the same looks and arrow and that font
     * @throws NullPointerException if {@code font} is {@code null}
     */
    public SelectBoxStyle withArrowFont(MarkFont font) {
        Objects.requireNonNull(font, "font");
        return new SelectBoxStyle(boxStyle, rowStyle, scrollPane, arrowGlyph, font);
    }

    /**
     * Returns the looks and padding of the closed box.
     *
     * @return the style, never {@code null}
     */
    public ButtonStyle getBoxStyle() {
        return boxStyle;
    }

    /**
     * Returns the looks and padding of the rows in the open list.
     *
     * @return the style, never {@code null}
     */
    public ButtonStyle getRowStyle() {
        return rowStyle;
    }

    /**
     * Returns the colors of the list's scroll bar.
     *
     * @return the style, never {@code null}
     */
    public ScrollPaneStyle getScrollPane() {
        return scrollPane;
    }

    /**
     * Returns the arrow glyph in the far-right cell of the closed box.
     *
     * @return the Unicode code point
     */
    public int getArrowGlyph() {
        return arrowGlyph;
    }

    /**
     * Returns the font of the arrow.
     *
     * @return the font, never {@code null}
     */
    public MarkFont getArrowFont() {
        return arrowFont;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SelectBoxStyle)) {
            return false;
        }
        SelectBoxStyle that = (SelectBoxStyle) other;
        return boxStyle.equals(that.boxStyle) && rowStyle.equals(that.rowStyle)
                && scrollPane.equals(that.scrollPane) && arrowGlyph == that.arrowGlyph
                && arrowFont == that.arrowFont;
    }

    @Override
    public int hashCode() {
        return Objects.hash(boxStyle, rowStyle, scrollPane, arrowGlyph, arrowFont);
    }

    @Override
    public String toString() {
        return "SelectBoxStyle[boxStyle=" + boxStyle + ", rowStyle=" + rowStyle
                + ", scrollPane=" + scrollPane + ", arrowGlyph=U+"
                + Integer.toHexString(arrowGlyph).toUpperCase(Locale.ROOT) + ", arrowFont="
                + arrowFont + "]";
    }
}
