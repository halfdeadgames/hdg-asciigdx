package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * A single line of text on a solid background, in the style's font. The text takes one UI column
 * per code point, does not wrap, and is cut off at the label's width. {@code '\n'} does not
 * start a new line; it is drawn like any other code point.
 *
 * <p>Drawing a label erases every glyph in its rectangle, including any tall glyph that overlaps
 * the rectangle's rows, so cells past the end of the text are blank. When the style's background
 * has alpha 0, the label keeps each cell's existing background, so it shows whatever was drawn
 * there before, such as a panel. Any other background, including a partly transparent
 * one, replaces the cell's background.
 *
 * <p>The text is written along the label's top row. A tall label is 2 rows high, and tall text on
 * an odd row of the surface snaps up and is drawn from the even row above.
 *
 * <p>A new label is packed: its size fits its text. Changing the text keeps the current size, so
 * call {@link #pack()} afterward to fit the new text.
 */
public class Label extends Widget {

    private static final int ALPHA_MASK = 0xFF;

    private String text;
    private LabelStyle style;

    /**
     * Creates a label sized to fit its text.
     *
     * @param text text to show; must not be {@code null}; may be empty
     * @param style font and colors to use; must not be {@code null}
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
     * Returns the font and colors.
     *
     * @return the style, never {@code null}
     */
    public LabelStyle getStyle() {
        return style;
    }

    /**
     * Replaces the font and colors. The size stays as it is, so after switching between the
     * square and the tall font, call {@link #pack()} to fit the new height. When the style
     * differs from the current one, the label is marked: if the font changed, by
     * {@link #invalidate()}, because the preferred height and row snap depend on the font; if only
     * the colors changed, by {@link #requestRedraw()}, because the size is unaffected. An equal
     * style changes no marks.
     *
     * @param style new style; must not be {@code null}
     * @throws NullPointerException if {@code style} is {@code null}
     */
    public void setStyle(LabelStyle style) {
        Objects.requireNonNull(style, "style");
        if (style.equals(this.style)) {
            return;
        }
        boolean fontChanged = style.getFont() != this.style.getFont();
        this.style = style;
        if (fontChanged) {
            invalidate();
        } else {
            requestRedraw();
        }
    }

    /**
     * Returns 2 when the style's font is {@link UiFont#TALL}, otherwise 1. Tall glyphs span two
     * square rows, and {@link UiSurface} snaps tall text to even rows, so a tall label's top must
     * sit on an even row.
     *
     * @return the row multiple, 1 or 2
     */
    @Override
    public int getRowSnap() {
        return style.getFont() == UiFont.TALL ? 2 : 1;
    }

    /**
     * Returns the number of code points in the text, so a character above U+FFFF counts once.
     *
     * @return the width in UI cells, zero or greater
     */
    @Override
    public int getPreferredWidth() {
        return text.codePointCount(0, text.length());
    }

    /**
     * Returns the height of one line of text: 1 for the square font, 2 for the tall font.
     *
     * @return the height in UI cells, 1 or 2
     */
    @Override
    public int getPreferredHeight() {
        return style.getFont() == UiFont.TALL ? 2 : 1;
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

    private void writeClippedText(UiSurface target, int left, int top, int width) {
        UiFont font = style.getFont();
        int foreground = style.getForegroundRgba();
        int length = text.length();
        int index = 0;
        int cellOffset = 0;
        while (index < length && cellOffset < width && left + cellOffset < target.getColumns()) {
            int codePoint = text.codePointAt(index);
            target.writeGlyph(font, left + cellOffset, top, codePoint, foreground);
            index += Character.charCount(codePoint);
            cellOffset++;
        }
    }
}
