package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * A single line of text on a solid background, in the text font. The text takes one text column
 * (half a UI column) per code point, does not wrap, and is cut off at the label's width.
 * {@code '\n'} does not start a new line; it is drawn like any other code point.
 *
 * <p>Drawing a label erases every glyph in its rectangle, so cells past the end of the text are
 * blank. When the style's background has alpha 0, the label keeps each cell's existing
 * background, so it shows whatever was drawn there before, such as a panel. Any other
 * background, including a partly transparent one, replaces the cell's background.
 *
 * <p>The text is written along the label's top row. By default it starts at the label's left
 * edge; see {@link #setTextAlignment} for other places when the label is wider than its text. The
 * label's width is in whole UI cells and rounds up from the text, so a text of an odd length
 * leaves a blank half cell, and a label cannot be sized in half cells.
 *
 * <p>A new label is packed: its size fits its text. Changing the text keeps the current size, so
 * call {@link #pack()} afterward to fit the new text.
 */
public class Label extends Widget {

    private static final int ALPHA_MASK = 0xFF;

    private String text;
    private LabelStyle style;
    private HorizontalAlignment textAlignment = HorizontalAlignment.LEFT;

    /**
     * Returns where the text sits when the label is wider than its text.
     *
     * @return the alignment, never {@code null}; {@link HorizontalAlignment#LEFT} for a new label
     */
    public HorizontalAlignment getTextAlignment() {
        return textAlignment;
    }

    /**
     * Sets where the text sits when the label is wider than its text. The leftover space is
     * counted in text columns (half UI cells): {@code LEFT} puts the text at the left edge,
     * {@code RIGHT} ends it at the right edge, and {@code CENTER} puts half of the leftover,
     * rounded down, before the text, so centered text can start on a half cell. Text that is
     * longer than the label is not affected: it starts at the left edge and is cut off at the
     * right edge. A change requests a redraw (not layout); an equal alignment changes nothing.
     *
     * @param alignment the new alignment; must not be {@code null}
     * @throws NullPointerException if {@code alignment} is {@code null}
     */
    public void setTextAlignment(HorizontalAlignment alignment) {
        Objects.requireNonNull(alignment, "alignment");
        if (alignment == textAlignment) {
            return;
        }
        textAlignment = alignment;
        requestRedraw();
    }

    /**
     * Creates a label sized to fit its text.
     *
     * @param text text to show; must not be {@code null}; may be empty
     * @param style colors to use; must not be {@code null}
     * @throws NullPointerException if {@code text} or {@code style} is {@code null}
     */
    public Label(String text, LabelStyle style) {
        this.text = Objects.requireNonNull(text, "text");
        this.style = Objects.requireNonNull(style, "style");
        setSize(getPreferredWidth(), getPreferredHeight());
    }

    /**
     * Returns the text.
     *
     * @return the text, never {@code null}
     */
    public String getText() {
        return text;
    }

    /**
     * Replaces the text. The size stays as it is; call {@link #pack()} to fit the new text. When
     * the text differs from the current one, calls {@link #invalidate()}; equal text changes no
     * marks. Layout is marked even though the size is kept, because the preferred size changes
     * and {@link #pack()} and parent layouts rely on that mark.
     *
     * @param text new text; must not be {@code null}; may be empty
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public void setText(String text) {
        Objects.requireNonNull(text, "text");
        if (text.equals(this.text)) {
            return;
        }
        this.text = text;
        invalidate();
    }

    /**
     * Returns the colors.
     *
     * @return the style, never {@code null}
     */
    public LabelStyle getStyle() {
        return style;
    }

    /**
     * Replaces the colors. The size is unaffected. When the style differs from the current one,
     * the label is marked by {@link #requestRedraw()}. An equal style changes no marks.
     *
     * @param style new style; must not be {@code null}
     * @throws NullPointerException if {@code style} is {@code null}
     */
    public void setStyle(LabelStyle style) {
        Objects.requireNonNull(style, "style");
        if (style.equals(this.style)) {
            return;
        }
        this.style = style;
        requestRedraw();
    }

    /**
     * Returns the width of the text, rounded up to whole UI cells. The text takes one text column
     * per code point, so a character above U+FFFF counts once, and two code points fit in one UI
     * cell.
     * The width always rounds up to whole UI cells, so a text of odd length leaves a blank half
     * cell at its end, and a label cannot be sized in half cells.
     *
     * @return the width in UI cells (not text columns), zero or greater
     */
    @Override
    public int getPreferredWidth() {
        int codePoints = text.codePointCount(0, text.length());
        return (codePoints + UiSurface.TEXT_COLUMNS_PER_CELL - 1)
                / UiSurface.TEXT_COLUMNS_PER_CELL;
    }

    /**
     * Returns the height of one line of text.
     *
     * @return 1 UI cell
     */
    @Override
    public int getPreferredHeight() {
        return 1;
    }

    @Override
    protected void drawContent(UiSurface target, int left, int top) {
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }
        target.eraseGlyphs(left, top, width, height);
        int background = style.getBackgroundRgba();
        if ((background & ALPHA_MASK) != 0) {
            target.fillBackground(left, top, width, height, background);
        }
        writeClippedText(target, left, top, width);
    }

    // Text that fills or overflows the label always starts at the left edge.
    private int alignmentOffset(int textColumnLimit, int letters) {
        int spare = textColumnLimit - letters;
        if (spare <= 0) {
            return 0;
        }
        return switch (textAlignment) {
            case LEFT -> 0;
            case CENTER -> spare / 2;
            case RIGHT -> spare;
        };
    }

    private void writeClippedText(UiSurface target, int left, int top, int width) {
        int foreground = style.getForegroundRgba();
        int length = text.length();
        int textColumnLimit = UiSurface.TEXT_COLUMNS_PER_CELL * width;
        int leftTextColumn = UiSurface.TEXT_COLUMNS_PER_CELL * left;
        int index = 0;
        int textOffset = alignmentOffset(textColumnLimit, text.codePointCount(0, length));
        while (index < length && textOffset < textColumnLimit
                && leftTextColumn + textOffset < target.getTextColumns()) {
            int codePoint = text.codePointAt(index);
            target.writeTextGlyph(leftTextColumn + textOffset, top, codePoint, foreground);
            index += Character.charCount(codePoint);
            textOffset++;
        }
    }
}
