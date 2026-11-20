package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * A display-only bar that shows how far a value has come between a minimum and a maximum. It is
 * drawn with block glyphs: full blocks for the filled cells and one eighth block at the boundary,
 * so the fill is shown in steps of one eighth of a cell. The filled share is rounded to the nearest
 * eighth of the whole bar.
 *
 * <p>A horizontal bar fills from left to right. A vertical bar fills from the bottom row up to the
 * top row (rows grow downward, so the bottom row has the highest row number). The length counts
 * glyph cells along the bar: with the square font a cell is one UI cell, and with the tall font a
 * cell is one column wide and two rows high, so a vertical tall bar of length n is 2n rows high.
 * The bar fills its whole area with the empty color and draws the glyphs in its first band of
 * cells, so any extra area across the bar stays empty. If the size is set larger than the
 * preferred size, the bar stretches along its axis and shows more cells; if it is set too small
 * for one cell, the bar only clears its area.
 *
 * <p>A progress bar is not focusable, takes no input, and has no listener. A tall bar should sit
 * on an even row (see {@link #getRowSnap()}). The font needs the glyphs U+2581 to U+2588 and
 * U+2589 to U+258F.
 *
 * <p>Example:
 * <pre>{@code
 * LabelStyle fill = LabelStyle.of(UiFont.SQUARE, 0x00FF00FF, 0x000000FF);
 * LabelStyle empty = LabelStyle.of(UiFont.SQUARE, 0x000000FF, 0x202020FF);
 * ProgressBar loading = ProgressBar.horizontal(ValueRange.of(0f, 100f, 1f), 20,
 *         ProgressBarStyle.of(fill, empty));
 * loading.setValue(42f);
 * ui.getRoot().addChild(loading);
 * }</pre>
 */
public final class ProgressBar extends Widget {

    private final BarOrientation orientation;
    private final ValueRange range;
    private final int preferredLengthCells;
    private ProgressBarStyle style;
    private BarAxis axis;
    private float value;

    private ProgressBar(BarOrientation orientation, ValueRange range, int preferredLengthCells,
            ProgressBarStyle style) {
        if (preferredLengthCells < 1) {
            throw new IllegalArgumentException(
                    "lengthCells must be at least 1, was " + preferredLengthCells);
        }
        this.orientation = orientation;
        this.range = Objects.requireNonNull(range, "range");
        this.preferredLengthCells = preferredLengthCells;
        this.style = Objects.requireNonNull(style, "style");
        this.axis = new BarAxis(orientation, style.getFont());
        this.value = range.getMin();
        pack();
    }

    /**
     * Creates a horizontal bar at its minimum, packed to its preferred size.
     *
     * @param range minimum, maximum, and step size of the shown value; must not be {@code null}
     * @param lengthCells number of glyph cells along the bar; 1 or greater
     * @param style looks; must not be {@code null}
     * @return a new bar
     * @throws NullPointerException if {@code range} or {@code style} is {@code null}
     * @throws IllegalArgumentException if {@code lengthCells} is less than 1
     */
    public static ProgressBar horizontal(ValueRange range, int lengthCells,
            ProgressBarStyle style) {
        return new ProgressBar(BarOrientation.HORIZONTAL, range, lengthCells, style);
    }

    /**
     * Creates a vertical bar at its minimum, packed to its preferred size. The bar fills from the
     * bottom up.
     *
     * @param range minimum, maximum, and step size of the shown value; must not be {@code null}
     * @param lengthCells number of glyph cells along the bar; 1 or greater
     * @param style looks; must not be {@code null}
     * @return a new bar
     * @throws NullPointerException if {@code range} or {@code style} is {@code null}
     * @throws IllegalArgumentException if {@code lengthCells} is less than 1
     */
    public static ProgressBar vertical(ValueRange range, int lengthCells,
            ProgressBarStyle style) {
        return new ProgressBar(BarOrientation.VERTICAL, range, lengthCells, style);
    }

    /**
     * Returns the current value.
     *
     * @return the value, between the minimum and the maximum of {@link #getRange()}
     */
    public float getValue() {
        return value;
    }

    /**
     * Sets the value. The value is snapped to the nearest step counted from the minimum and
     * clamped to the range, so {@link #getValue()} may differ from the argument; infinities clamp
     * to the minimum or the maximum. A change requests a redraw.
     *
     * @param value new value; any number except NaN
     * @throws IllegalArgumentException if {@code value} is NaN
     */
    public void setValue(float value) {
        float snapped = range.snap(value);
        if (snapped == this.value) {
            return;
        }
        this.value = snapped;
        requestRedraw();
    }

    /**
     * Returns the range of the value.
     *
     * @return the range, never {@code null}
     */
    public ValueRange getRange() {
        return range;
    }

    /**
     * Returns the direction of the bar.
     *
     * @return the orientation, never {@code null}
     */
    public BarOrientation getOrientation() {
        return orientation;
    }

    /**
     * Returns the length the bar was created with, which is the length of its preferred size.
     *
     * @return the number of glyph cells along the bar, 1 or greater
     */
    public int getPreferredLengthCells() {
        return preferredLengthCells;
    }

    /**
     * Returns the looks.
     *
     * @return the style, never {@code null}
     */
    public ProgressBarStyle getStyle() {
        return style;
    }

    /**
     * Replaces the looks. The size stays as it is, so call {@link #pack()} after a change of font.
     * A different style is marked by {@link #invalidate()} if the font changed, otherwise by
     * {@link #requestRedraw()}. An equal style changes no marks.
     *
     * @param style new style; must not be {@code null}
     * @throws NullPointerException if {@code style} is {@code null}
     */
    public void setStyle(ProgressBarStyle style) {
        Objects.requireNonNull(style, "style");
        if (style.equals(this.style)) {
            return;
        }
        boolean fontChanged = style.getFont() != this.style.getFont();
        this.style = style;
        axis = new BarAxis(orientation, style.getFont());
        if (fontChanged) {
            invalidate();
        } else {
            requestRedraw();
        }
    }

    /**
     * Returns 2 when the style's font is {@link UiFont#TALL}, otherwise 1.
     *
     * @return the row multiple, 1 or 2
     */
    @Override
    public int getRowSnap() {
        return axis.getRowsPerCell();
    }

    /**
     * Returns the length for a horizontal bar and 1 for a vertical bar.
     *
     * @return the width in UI cells
     */
    @Override
    public int getPreferredWidth() {
        return axis.widthFor(preferredLengthCells);
    }

    /**
     * Returns the height of one cell (1 for the square font, 2 for the tall font) for a
     * horizontal bar, and the length times that for a vertical bar.
     *
     * @return the height in UI cells
     */
    @Override
    public int getPreferredHeight() {
        return axis.heightFor(preferredLengthCells);
    }

    @Override
    protected void drawContent(UiSurface target, int left, int top) {
        int width = getWidth();
        int height = getHeight();
        target.eraseGlyphs(left, top, width, height);
        target.fillBackground(left, top, width, height, style.getEmpty().getBackgroundRgba());
        int cellCount = axis.cellCountIn(width, height);
        if (cellCount == 0) {
            return;
        }
        long totalEighths = EighthBlocks.totalEighths(cellCount, range.fractionOf(value));
        for (int index = 0; index < cellCount; index++) {
            int glyph = EighthBlocks.glyphForCell(orientation, totalEighths, index);
            if (glyph != EighthBlocks.EMPTY_GLYPH) {
                target.writeGlyph(style.getFont(), axis.columnOf(left, index),
                        axis.rowOf(top, index, cellCount), glyph,
                        style.getFill().getForegroundRgba());
            }
        }
    }
}
