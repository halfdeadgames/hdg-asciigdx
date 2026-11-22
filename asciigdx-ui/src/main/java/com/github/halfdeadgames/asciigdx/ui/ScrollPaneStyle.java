package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * Looks of a {@link ScrollPane}: the colors of its scrollbars and the optional step buttons at
 * their ends. The track is a plain color. The thumb has a normal color, a color while the pointer
 * is over it, and a color while it is dragged. Colors are packed RGBA8888 ints. Immutable, so one
 * instance can be shared by any number of scroll panes.
 *
 * <p>A style made by {@link #of} uses the thumb color for the pointer-over and pressed looks too;
 * change them with {@link #withThumbPointerOver} and {@link #withThumbPressed}.
 *
 * <p>Step buttons are off in a style made by {@link #of}, so code-built styles look as before.
 * {@link #withStepButtonsShown} turns them on: each scrollbar then shows an arrow in its first
 * and last cell. The arrows have a normal, a pointer-over, and a pressed color; until one is set
 * it follows the matching thumb color, including a thumb color that a later call changes. The
 * arrow glyphs default to U+25B2, U+25BC, U+25C4, and U+25BA, and are drawn in the square font
 * unless {@link #withArrowFont} chooses the text font. The arrow cell background is the track
 * color.
 *
 * <p>Example (the order is track, then thumb):
 * <pre>{@code
 * ScrollPaneStyle style = ScrollPaneStyle.of(0x404040FF, 0x909090FF)
 *         .withThumbPressed(0xFFD700FF)
 *         .withStepButtonsShown(true);
 * }</pre>
 */
public final class ScrollPaneStyle {

    private static final int DEFAULT_UP_ARROW = 0x25B2;
    private static final int DEFAULT_DOWN_ARROW = 0x25BC;
    private static final int DEFAULT_LEFT_ARROW = 0x25C4;
    private static final int DEFAULT_RIGHT_ARROW = 0x25BA;

    private final int trackRgba;
    private final int thumbRgba;
    private final int thumbPointerOverRgba;
    private final int thumbPressedRgba;
    private final boolean stepButtonsShown;
    // Null while unset, so the arrow color follows the thumb color.
    private final Integer stepArrowRgba;
    private final Integer stepArrowPointerOverRgba;
    private final Integer stepArrowPressedRgba;
    private final int upArrowGlyph;
    private final int downArrowGlyph;
    private final int leftArrowGlyph;
    private final int rightArrowGlyph;
    private final MarkFont arrowFont;

    private ScrollPaneStyle(Builder values) {
        this.trackRgba = values.trackRgba;
        this.thumbRgba = values.thumbRgba;
        this.thumbPointerOverRgba = values.thumbPointerOverRgba;
        this.thumbPressedRgba = values.thumbPressedRgba;
        this.stepButtonsShown = values.stepButtonsShown;
        this.stepArrowRgba = values.stepArrowRgba;
        this.stepArrowPointerOverRgba = values.stepArrowPointerOverRgba;
        this.stepArrowPressedRgba = values.stepArrowPressedRgba;
        this.upArrowGlyph = values.upArrowGlyph;
        this.downArrowGlyph = values.downArrowGlyph;
        this.leftArrowGlyph = values.leftArrowGlyph;
        this.rightArrowGlyph = values.rightArrowGlyph;
        this.arrowFont = values.arrowFont;
    }

    // Mutable scratch copy of every field, so a with method changes one value and copies the rest.
    private static final class Builder {
        private int trackRgba;
        private int thumbRgba;
        private int thumbPointerOverRgba;
        private int thumbPressedRgba;
        private boolean stepButtonsShown;
        private Integer stepArrowRgba;
        private Integer stepArrowPointerOverRgba;
        private Integer stepArrowPressedRgba;
        private int upArrowGlyph = DEFAULT_UP_ARROW;
        private int downArrowGlyph = DEFAULT_DOWN_ARROW;
        private int leftArrowGlyph = DEFAULT_LEFT_ARROW;
        private int rightArrowGlyph = DEFAULT_RIGHT_ARROW;
        private MarkFont arrowFont = MarkFont.SQUARE;

        Builder(ScrollPaneStyle source) {
            trackRgba = source.trackRgba;
            thumbRgba = source.thumbRgba;
            thumbPointerOverRgba = source.thumbPointerOverRgba;
            thumbPressedRgba = source.thumbPressedRgba;
            stepButtonsShown = source.stepButtonsShown;
            stepArrowRgba = source.stepArrowRgba;
            stepArrowPointerOverRgba = source.stepArrowPointerOverRgba;
            stepArrowPressedRgba = source.stepArrowPressedRgba;
            upArrowGlyph = source.upArrowGlyph;
            downArrowGlyph = source.downArrowGlyph;
            leftArrowGlyph = source.leftArrowGlyph;
            rightArrowGlyph = source.rightArrowGlyph;
            arrowFont = source.arrowFont;
        }

        Builder(int trackRgba, int thumbRgba) {
            this.trackRgba = trackRgba;
            this.thumbRgba = thumbRgba;
            this.thumbPointerOverRgba = thumbRgba;
            this.thumbPressedRgba = thumbRgba;
        }
    }

    /**
     * Creates a style whose pointer-over and pressed thumb colors are copies of the thumb color,
     * with step buttons off.
     *
     * @param trackRgba color of the track cells as RGBA8888
     * @param thumbRgba color of the thumb as RGBA8888
     * @return a new style
     */
    public static ScrollPaneStyle of(int trackRgba, int thumbRgba) {
        return new ScrollPaneStyle(new Builder(trackRgba, thumbRgba));
    }

    /**
     * Returns a copy with another thumb color for while the pointer is over the thumb.
     *
     * @param thumbPointerOverRgba color as RGBA8888
     * @return a style with the same other looks
     */
    public ScrollPaneStyle withThumbPointerOver(int thumbPointerOverRgba) {
        Builder values = new Builder(this);
        values.thumbPointerOverRgba = thumbPointerOverRgba;
        return new ScrollPaneStyle(values);
    }

    /**
     * Returns a copy with another thumb color for while the thumb is dragged.
     *
     * @param thumbPressedRgba color as RGBA8888
     * @return a style with the same other looks
     */
    public ScrollPaneStyle withThumbPressed(int thumbPressedRgba) {
        Builder values = new Builder(this);
        values.thumbPressedRgba = thumbPressedRgba;
        return new ScrollPaneStyle(values);
    }

    /**
     * Returns a copy that shows or hides the step buttons, the arrows in the first and last cell
     * of each scrollbar.
     *
     * @param stepButtonsShown {@code true} to show the arrows
     * @return a style with the same other looks
     */
    public ScrollPaneStyle withStepButtonsShown(boolean stepButtonsShown) {
        Builder values = new Builder(this);
        values.stepButtonsShown = stepButtonsShown;
        return new ScrollPaneStyle(values);
    }

    /**
     * Returns a copy with another normal arrow color. Without this call the arrow color follows
     * the thumb color.
     *
     * @param stepArrowRgba color as RGBA8888
     * @return a style with the same other looks
     */
    public ScrollPaneStyle withStepArrow(int stepArrowRgba) {
        Builder values = new Builder(this);
        values.stepArrowRgba = stepArrowRgba;
        return new ScrollPaneStyle(values);
    }

    /**
     * Returns a copy with another arrow color for while the pointer is over the arrow. Without
     * this call the color follows the pointer-over thumb color.
     *
     * @param stepArrowPointerOverRgba color as RGBA8888
     * @return a style with the same other looks
     */
    public ScrollPaneStyle withStepArrowPointerOver(int stepArrowPointerOverRgba) {
        Builder values = new Builder(this);
        values.stepArrowPointerOverRgba = stepArrowPointerOverRgba;
        return new ScrollPaneStyle(values);
    }

    /**
     * Returns a copy with another arrow color for while the arrow is held. Without this call the
     * color follows the pressed thumb color.
     *
     * @param stepArrowPressedRgba color as RGBA8888
     * @return a style with the same other looks
     */
    public ScrollPaneStyle withStepArrowPressed(int stepArrowPressedRgba) {
        Builder values = new Builder(this);
        values.stepArrowPressedRgba = stepArrowPressedRgba;
        return new ScrollPaneStyle(values);
    }

    /**
     * Returns a copy with another glyph for the first cell of a vertical bar.
     *
     * @param upArrowGlyph glyph as a code point
     * @return a style with the same other looks
     * @throws IllegalArgumentException if {@code upArrowGlyph} is not a valid Unicode code point
     */
    public ScrollPaneStyle withUpArrowGlyph(int upArrowGlyph) {
        Builder values = new Builder(this);
        values.upArrowGlyph = requireCodePoint(upArrowGlyph);
        return new ScrollPaneStyle(values);
    }

    /**
     * Returns a copy with another glyph for the last cell of a vertical bar.
     *
     * @param downArrowGlyph glyph as a code point
     * @return a style with the same other looks
     * @throws IllegalArgumentException if {@code downArrowGlyph} is not a valid Unicode code point
     */
    public ScrollPaneStyle withDownArrowGlyph(int downArrowGlyph) {
        Builder values = new Builder(this);
        values.downArrowGlyph = requireCodePoint(downArrowGlyph);
        return new ScrollPaneStyle(values);
    }

    /**
     * Returns a copy with another glyph for the first cell of a horizontal bar.
     *
     * @param leftArrowGlyph glyph as a code point
     * @return a style with the same other looks
     * @throws IllegalArgumentException if {@code leftArrowGlyph} is not a valid Unicode code point
     */
    public ScrollPaneStyle withLeftArrowGlyph(int leftArrowGlyph) {
        Builder values = new Builder(this);
        values.leftArrowGlyph = requireCodePoint(leftArrowGlyph);
        return new ScrollPaneStyle(values);
    }

    /**
     * Returns a copy with another glyph for the last cell of a horizontal bar.
     *
     * @param rightArrowGlyph glyph as a code point
     * @return a style with the same other looks
     * @throws IllegalArgumentException if {@code rightArrowGlyph} is not a valid Unicode code
     *         point
     */
    public ScrollPaneStyle withRightArrowGlyph(int rightArrowGlyph) {
        Builder values = new Builder(this);
        values.rightArrowGlyph = requireCodePoint(rightArrowGlyph);
        return new ScrollPaneStyle(values);
    }

    private static int requireCodePoint(int codePoint) {
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("Not a valid code point: " + codePoint);
        }
        return codePoint;
    }

    /**
     * Returns a copy that draws the arrows in another font. In {@link MarkFont#TEXT} the glyph is
     * drawn in the left text column of its cell.
     *
     * @param arrowFont font of the arrows; must not be {@code null}
     * @return a style with the same other looks
     * @throws NullPointerException if {@code arrowFont} is {@code null}
     */
    public ScrollPaneStyle withArrowFont(MarkFont arrowFont) {
        Objects.requireNonNull(arrowFont, "arrowFont");
        Builder values = new Builder(this);
        values.arrowFont = arrowFont;
        return new ScrollPaneStyle(values);
    }

    /**
     * Returns the track color.
     *
     * @return the color as RGBA8888
     */
    public int getTrackRgba() {
        return trackRgba;
    }

    /**
     * Returns the normal thumb color.
     *
     * @return the color as RGBA8888
     */
    public int getThumbRgba() {
        return thumbRgba;
    }

    /**
     * Returns the thumb color while the pointer is over the thumb: the color given to
     * {@link #withThumbPointerOver}, or the thumb color if none was given.
     *
     * @return the color as RGBA8888
     */
    public int getThumbPointerOverRgba() {
        return thumbPointerOverRgba;
    }

    /**
     * Returns the thumb color while the thumb is dragged: the color given to
     * {@link #withThumbPressed}, or the thumb color if none was given.
     *
     * @return the color as RGBA8888
     */
    public int getThumbPressedRgba() {
        return thumbPressedRgba;
    }

    /**
     * Returns whether the scrollbars show step buttons.
     *
     * @return {@code true} if they do; {@code false} unless {@link #withStepButtonsShown} turned
     *         them on
     */
    public boolean isStepButtonsShown() {
        return stepButtonsShown;
    }

    /**
     * Returns the normal arrow color: the color given to {@link #withStepArrow}, or the thumb
     * color if none was given.
     *
     * @return the color as RGBA8888
     */
    public int getStepArrowRgba() {
        return stepArrowRgba != null ? stepArrowRgba : thumbRgba;
    }

    /**
     * Returns the arrow color while the pointer is over the arrow: the color given to
     * {@link #withStepArrowPointerOver}, or the pointer-over thumb color if none was given.
     *
     * @return the color as RGBA8888
     */
    public int getStepArrowPointerOverRgba() {
        return stepArrowPointerOverRgba != null ? stepArrowPointerOverRgba : thumbPointerOverRgba;
    }

    /**
     * Returns the arrow color while the arrow is held: the color given to
     * {@link #withStepArrowPressed}, or the pressed thumb color if none was given.
     *
     * @return the color as RGBA8888
     */
    public int getStepArrowPressedRgba() {
        return stepArrowPressedRgba != null ? stepArrowPressedRgba : thumbPressedRgba;
    }

    /**
     * Returns the glyph of the arrow in the first cell of a vertical bar.
     *
     * @return a code point; U+25B2 unless changed
     */
    public int getUpArrowGlyph() {
        return upArrowGlyph;
    }

    /**
     * Returns the glyph of the arrow in the last cell of a vertical bar.
     *
     * @return a code point; U+25BC unless changed
     */
    public int getDownArrowGlyph() {
        return downArrowGlyph;
    }

    /**
     * Returns the glyph of the arrow in the first cell of a horizontal bar.
     *
     * @return a code point; U+25C4 unless changed
     */
    public int getLeftArrowGlyph() {
        return leftArrowGlyph;
    }

    /**
     * Returns the glyph of the arrow in the last cell of a horizontal bar.
     *
     * @return a code point; U+25BA unless changed
     */
    public int getRightArrowGlyph() {
        return rightArrowGlyph;
    }

    /**
     * Returns the font of the arrows.
     *
     * @return the font, never {@code null}; {@link MarkFont#SQUARE} unless changed
     */
    public MarkFont getArrowFont() {
        return arrowFont;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ScrollPaneStyle)) {
            return false;
        }
        ScrollPaneStyle that = (ScrollPaneStyle) other;
        return trackRgba == that.trackRgba && thumbRgba == that.thumbRgba
                && thumbPointerOverRgba == that.thumbPointerOverRgba
                && thumbPressedRgba == that.thumbPressedRgba
                && stepButtonsShown == that.stepButtonsShown
                && getStepArrowRgba() == that.getStepArrowRgba()
                && getStepArrowPointerOverRgba() == that.getStepArrowPointerOverRgba()
                && getStepArrowPressedRgba() == that.getStepArrowPressedRgba()
                && upArrowGlyph == that.upArrowGlyph && downArrowGlyph == that.downArrowGlyph
                && leftArrowGlyph == that.leftArrowGlyph
                && rightArrowGlyph == that.rightArrowGlyph && arrowFont == that.arrowFont;
    }

    @Override
    public int hashCode() {
        return Objects.hash(trackRgba, thumbRgba, thumbPointerOverRgba, thumbPressedRgba,
                stepButtonsShown, getStepArrowRgba(), getStepArrowPointerOverRgba(),
                getStepArrowPressedRgba(), upArrowGlyph, downArrowGlyph, leftArrowGlyph,
                rightArrowGlyph, arrowFont);
    }

    @Override
    public String toString() {
        return "ScrollPaneStyle[track=" + hex(trackRgba) + ", thumb=" + hex(thumbRgba)
                + ", thumbPointerOver=" + hex(thumbPointerOverRgba) + ", thumbPressed="
                + hex(thumbPressedRgba) + ", stepButtonsShown=" + stepButtonsShown
                + ", stepArrow=" + hex(getStepArrowRgba()) + ", stepArrowPointerOver="
                + hex(getStepArrowPointerOverRgba()) + ", stepArrowPressed="
                + hex(getStepArrowPressedRgba()) + ", arrowGlyphs=" + upArrowGlyph + "/"
                + downArrowGlyph + "/" + leftArrowGlyph + "/" + rightArrowGlyph
                + ", arrowFont=" + arrowFont + "]";
    }

    private static String hex(int rgba) {
        return String.format("#%08X", rgba);
    }
}
