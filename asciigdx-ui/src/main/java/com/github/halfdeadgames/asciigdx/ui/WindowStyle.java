package com.github.halfdeadgames.asciigdx.ui;

import com.github.halfdeadgames.asciigdx.draw.BoxStyle;
import java.util.Objects;

/**
 * Looks of a {@link Window}: the line style of its border, the look of the border and of the title
 * drawn into it, the background of the area inside the border, and an optional close button.
 * The border is drawn in the square font and the title in the text font. The close mark is drawn
 * in the square font unless {@link #withCloseFont(MarkFont)} chooses the text font. Immutable, so one instance can be shared by any number of windows. Colors are packed RGBA8888
 * ints, as in {@code TerminalGrid}.
 *
 * <p>The border is drawn with {@link #getBorder()}, or with {@link #getFocusedBorder()} while
 * keyboard focus is inside the window. A style made by {@link #of} uses the same look for both;
 * change it with {@link #withFocusedBorder(LabelStyle)}. The title is drawn in the top border row
 * with {@link #getTitle()}. A style has no close button until {@link #withCloseButton} is called.
 *
 * <p>Example (the parameters of {@code of} are the box, the border look, the title look, and the
 * background color):
 * <pre>{@code
 * WindowStyle style = WindowStyle.of(BoxStyle.ROUNDED, border, title, 0x303030FF)
 *         .withFocusedBorder(focusedBorder)
 *         .withCloseButton(buttonStyle);
 * }</pre>
 */
public final class WindowStyle {

    /** Text of the close button of a style that sets none: {@code "x"}. */
    public static final String DEFAULT_CLOSE_TEXT = "x";

    private final BoxStyle box;
    private final LabelStyle border;
    private final LabelStyle focusedBorder;
    private final LabelStyle title;
    private final int backgroundRgba;
    private final ButtonStyle closeButton;
    private final String closeText;
    private final MarkFont closeFont;

    private WindowStyle(BoxStyle box, LabelStyle border, LabelStyle focusedBorder,
            LabelStyle title, int backgroundRgba, ButtonStyle closeButton, String closeText,
            MarkFont closeFont) {
        this.box = box;
        this.border = border;
        this.focusedBorder = focusedBorder;
        this.title = title;
        this.backgroundRgba = backgroundRgba;
        this.closeButton = closeButton;
        this.closeText = closeText;
        this.closeFont = closeFont;
    }

    /**
     * Creates a style without a close button, whose focused border is a copy of the border and
     * whose close text is {@link #DEFAULT_CLOSE_TEXT}, drawn in the square font
     * ({@link MarkFont#SQUARE}).
     *
     * @param box line style of the border; must not be {@code null}
     * @param border look of the border glyphs; must not be {@code null}
     * @param title look of the title text; must not be {@code null}
     * @param backgroundRgba background of the area inside the border, as RGBA8888
     * @return a new style
     * @throws NullPointerException if an object argument is {@code null}
     */
    public static WindowStyle of(BoxStyle box, LabelStyle border, LabelStyle title,
            int backgroundRgba) {
        Objects.requireNonNull(box, "box");
        Objects.requireNonNull(border, "border");
        Objects.requireNonNull(title, "title");
        return new WindowStyle(box, border, border, title, backgroundRgba, null,
                DEFAULT_CLOSE_TEXT, MarkFont.SQUARE);
    }

    /**
     * Returns a copy with another look for the border while keyboard focus is inside the window.
     *
     * @param focusedBorder look of the border glyphs while focused; must not be {@code null}
     * @return a style with the same other parts and this focused border
     * @throws NullPointerException if {@code focusedBorder} is {@code null}
     */
    public WindowStyle withFocusedBorder(LabelStyle focusedBorder) {
        Objects.requireNonNull(focusedBorder, "focusedBorder");
        return new WindowStyle(box, border, focusedBorder, title, backgroundRgba, closeButton,
                closeText, closeFont);
    }

    /**
     * Returns a copy with a close button in the top border row.
     *
     * @param closeButton looks and padding of the button; must not be {@code null}
     * @return a style with the same other parts and this close button
     * @throws NullPointerException if {@code closeButton} is {@code null}
     */
    public WindowStyle withCloseButton(ButtonStyle closeButton) {
        Objects.requireNonNull(closeButton, "closeButton");
        return new WindowStyle(box, border, focusedBorder, title, backgroundRgba, closeButton,
                closeText, closeFont);
    }

    /**
     * Returns a copy with other text on the close button. It matters only for a style that has a
     * close button.
     *
     * @param closeText text of the button; must not be {@code null} or empty
     * @return a style with the same other parts and this close text
     * @throws NullPointerException if {@code closeText} is {@code null}
     * @throws IllegalArgumentException if {@code closeText} is empty
     */
    public WindowStyle withCloseText(String closeText) {
        Objects.requireNonNull(closeText, "closeText");
        if (closeText.isEmpty()) {
            throw new IllegalArgumentException("closeText must not be empty");
        }
        return new WindowStyle(box, border, focusedBorder, title, backgroundRgba, closeButton,
                closeText, closeFont);
    }

    /**
     * Returns a copy with another font for the close button text. It matters only for a style
     * that has a close button. The button takes one UI cell per code point in
     * {@link MarkFont#SQUARE}, and one text column (half a cell, rounded up to whole cells for the
     * whole text) per code point in {@link MarkFont#TEXT}, so the font changes the button's width.
     * {@link Window} has no setStyle, so a window always uses the close font of the style it was
     * created with.
     *
     * @param closeFont font of the close text; must not be {@code null}
     * @return a style with the same other parts and this close font
     * @throws NullPointerException if {@code closeFont} is {@code null}
     */
    public WindowStyle withCloseFont(MarkFont closeFont) {
        Objects.requireNonNull(closeFont, "closeFont");
        return new WindowStyle(box, border, focusedBorder, title, backgroundRgba, closeButton,
                closeText, closeFont);
    }

    /**
     * Returns the line style of the border.
     *
     * @return the box style, never {@code null}
     */
    public BoxStyle getBox() {
        return box;
    }

    /**
     * Returns the look of the border glyphs.
     *
     * @return the look, never {@code null}
     */
    public LabelStyle getBorder() {
        return border;
    }

    /**
     * Returns the look of the border glyphs while keyboard focus is inside the window.
     *
     * @return the look, never {@code null}; equal to {@link #getBorder()} unless
     *         {@link #withFocusedBorder} was called
     */
    public LabelStyle getFocusedBorder() {
        return focusedBorder;
    }

    /**
     * Returns the look of the title text.
     *
     * @return the look, never {@code null}
     */
    public LabelStyle getTitle() {
        return title;
    }

    /**
     * Returns the background of the area inside the border.
     *
     * @return the color as RGBA8888
     */
    public int getBackgroundRgba() {
        return backgroundRgba;
    }

    /**
     * Returns whether the style has a close button.
     *
     * @return {@code true} after {@link #withCloseButton}
     */
    public boolean hasCloseButton() {
        return closeButton != null;
    }

    /**
     * Returns the looks and padding of the close button.
     *
     * @return the button style, never {@code null}
     * @throws IllegalStateException if the style has no close button; check
     *         {@link #hasCloseButton()} first
     */
    public ButtonStyle getCloseButton() {
        if (closeButton == null) {
            throw new IllegalStateException("The style has no close button");
        }
        return closeButton;
    }

    /**
     * Returns the text of the close button. It is meaningful only when {@link #hasCloseButton()}
     * is {@code true}; a style without a close button ignores it.
     *
     * @return the text, at least one character; {@link #DEFAULT_CLOSE_TEXT} unless changed
     */
    public String getCloseText() {
        return closeText;
    }

    /**
     * Returns the font of the close button text. It is meaningful only when
     * {@link #hasCloseButton()} is {@code true}; a style without a close button ignores it when
     * drawing, but {@code equals}, {@code hashCode}, and {@code toString} include the font even
     * then.
     *
     * @return the font, never {@code null}; {@link MarkFont#SQUARE} unless changed
     */
    public MarkFont getCloseFont() {
        return closeFont;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WindowStyle)) {
            return false;
        }
        WindowStyle that = (WindowStyle) other;
        return box == that.box && border.equals(that.border)
                && focusedBorder.equals(that.focusedBorder) && title.equals(that.title)
                && backgroundRgba == that.backgroundRgba
                && Objects.equals(closeButton, that.closeButton)
                && closeText.equals(that.closeText) && closeFont == that.closeFont;
    }

    @Override
    public int hashCode() {
        return Objects.hash(box, border, focusedBorder, title, backgroundRgba, closeButton,
                closeText, closeFont);
    }

    @Override
    public String toString() {
        return "WindowStyle[box=" + box + ", border=" + border + ", focusedBorder="
                + focusedBorder + ", title=" + title + ", backgroundRgba="
                + Integer.toHexString(backgroundRgba) + ", closeButton="
                + (closeButton == null ? "none" : closeButton) + ", closeText=" + closeText + ", closeFont=" + closeFont + "]";
    }
}
