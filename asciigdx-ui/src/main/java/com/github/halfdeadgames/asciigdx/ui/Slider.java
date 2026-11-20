package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.Input;
import java.util.Arrays;
import java.util.Objects;

/**
 * A control that lets the user pick a value between a minimum and a maximum. It is drawn as a
 * line of track glyphs with one knob glyph at the position of the value. The knob always sits on
 * a whole cell: its cell index is {@code round(fraction * (cells - 1))}, where the fraction is
 * the share of the range below the value.
 *
 * <p>Because the knob moves in whole cells, the pointer reaches only the steps nearest the cell
 * positions. To reach every step with the pointer, the slider needs
 * {@code cells >= (max - min) / stepSize + 1}. The keys reach every step regardless of the length.
 *
 * <p>A horizontal slider runs from the minimum at the left to the maximum at the right. A vertical
 * slider runs from the minimum at the bottom row to the maximum at the top row (rows grow
 * downward, so the bottom row has the highest row number). The length counts glyph cells along the
 * slider: with the square font a cell is one UI cell, and with the tall font a cell is one column
 * wide and two rows high, so a vertical tall slider of length n is 2n rows high. The track fills
 * the whole area with its background and draws the glyphs in its first band of cells. If the size
 * is set larger than the preferred size, the slider stretches along its axis and shows more
 * cells; if it is set too small for one cell, the slider only clears its area. A tall slider
 * should sit on an even row (see {@link #getRowSnap()}). The glyphs come from the
 * {@link SliderStyle}; without explicit glyphs the track glyph depends on the orientation.
 *
 * <p>Keyboard, while the slider has focus (the key must go to the slider, so it must be the
 * focused widget of a {@link UiRoot}). Every key-down event moves one step, so a repeated key-down
 * from a held key moves one step each time:
 * <ul>
 *   <li>Along the slider, one step: Left (down) and Right (up) when horizontal, Down (down) and
 *       Up (up) when vertical. At an end the key is still consumed but changes nothing.
 *   <li>Home sets the minimum and End sets the maximum.
 *   <li>The arrow keys across the slider are not consumed, so they move focus to a neighbor.
 * </ul>
 *
 * <p>Pointer: pressing the primary button inside the slider arms it and captures the pointer;
 * the knob jumps to the pressed cell, follows the pointer while it is dragged (also outside, where
 * it stays at the nearest end), and releasing or a cancel disarms it. The value is the nearest
 * step to the cell's position.
 *
 * <p>A disabled slider ignores input, is not focusable, and shows its disabled look. Disabling it
 * during a drag ends the drag; the release or cancel of that pointer is still consumed. The focused
 * look shows only while the slider has keyboard focus and the {@link UiRoot} shows focus (see
 * {@link UiRoot#isFocusVisible()}).
 *
 * <p>A {@link ChangeListener} is called only after a change by the user, and only when the value
 * actually changed. {@link #setValue} never calls listeners.
 *
 * <p>Example:
 * <pre>{@code
 * Slider volume = Slider.horizontal(ValueRange.of(0f, 10f, 1f), 11,
 *         skin.getSliderStyle("slider"));
 * volume.setValue(5f);
 * volume.addChangeListener(slider -> setVolume(slider.getValue()));
 * ui.getRoot().addChild(volume);
 * }</pre>
 */
public final class Slider extends Widget {

    private static final ChangeListener<?>[] NO_LISTENERS = new ChangeListener<?>[0];
    private static final int PRIMARY_BUTTON = 0;
    private static final int NO_POINTER = -1;

    private final BarOrientation orientation;
    private final ValueRange range;
    private final int preferredLengthCells;
    private SliderStyle style;
    private BarAxis axis;
    // Every element accepts Slider, because only addChangeListener(ChangeListener<? super Slider>)
    // adds.
    private ChangeListener<?>[] changeListeners = NO_LISTENERS;
    private float value;
    private boolean disabled;
    private int armedPointer = NO_POINTER;
    // The pointer that was dragging when the slider got disabled; its release is still consumed.
    private int abandonedPointer = NO_POINTER;

    private Slider(BarOrientation orientation, ValueRange range, int preferredLengthCells,
            SliderStyle style) {
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
     * Creates a horizontal slider at its minimum, packed to its preferred size.
     *
     * @param range minimum (left end), maximum (right end), and step size; must not be
     *         {@code null}
     * @param lengthCells number of glyph cells along the slider; 1 or greater
     * @param style looks and glyphs; must not be {@code null}
     * @return a new slider
     * @throws NullPointerException if {@code range} or {@code style} is {@code null}
     * @throws IllegalArgumentException if {@code lengthCells} is less than 1
     */
    public static Slider horizontal(ValueRange range, int lengthCells, SliderStyle style) {
        return new Slider(BarOrientation.HORIZONTAL, range, lengthCells, style);
    }

    /**
     * Creates a vertical slider at its minimum, packed to its preferred size. The minimum is at
     * the bottom.
     *
     * @param range minimum (bottom end), maximum (top end), and step size; must not be
     *         {@code null}
     * @param lengthCells number of glyph cells along the slider; 1 or greater
     * @param style looks and glyphs; must not be {@code null}
     * @return a new slider
     * @throws NullPointerException if {@code range} or {@code style} is {@code null}
     * @throws IllegalArgumentException if {@code lengthCells} is less than 1
     */
    public static Slider vertical(ValueRange range, int lengthCells, SliderStyle style) {
        return new Slider(BarOrientation.VERTICAL, range, lengthCells, style);
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
     * Sets the value from code. The value is snapped to the nearest step counted from the minimum
     * and clamped to the range, so {@link #getValue()} may differ from the argument; infinities
     * clamp to the minimum or the maximum. A change requests a redraw; listeners are not called.
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
     * Returns the direction of the slider.
     *
     * @return the orientation, never {@code null}
     */
    public BarOrientation getOrientation() {
        return orientation;
    }

    /**
     * Returns the length the slider was created with, which is the length of its preferred size.
     *
     * @return the number of glyph cells along the slider, 1 or greater
     */
    public int getPreferredLengthCells() {
        return preferredLengthCells;
    }

    /**
     * Returns the looks and glyphs.
     *
     * @return the style, never {@code null}
     */
    public SliderStyle getStyle() {
        return style;
    }

    /**
     * Replaces the looks and glyphs. The size stays as it is, so call {@link #pack()} after a
     * change of font. A different style is marked by {@link #invalidate()} if the font changed,
     * otherwise by {@link #requestRedraw()}. An equal style changes no marks.
     *
     * @param style new style; must not be {@code null}
     * @throws NullPointerException if {@code style} is {@code null}
     */
    public void setStyle(SliderStyle style) {
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
     * Enables or disables the slider. Disabling ends a drag in progress. A change requests a
     * redraw. The keyboard focus is not touched, but a disabled slider ignores keys.
     *
     * @param disabled {@code true} to disable the slider
     */
    public void setDisabled(boolean disabled) {
        if (this.disabled == disabled) {
            return;
        }
        this.disabled = disabled;
        if (disabled) {
            abandonedPointer = armedPointer;
            armedPointer = NO_POINTER;
        }
        requestRedraw();
    }

    /**
     * Returns whether the slider is disabled.
     *
     * @return {@code true} when disabled
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Adds a listener that is called after every change by the user, in the order the listeners
     * were added. Safe to call from inside a listener; the change applies from the next change.
     *
     * @param listener listener to add; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void addChangeListener(ChangeListener<? super Slider> listener) {
        Objects.requireNonNull(listener, "listener");
        ChangeListener<?>[] grown = Arrays.copyOf(changeListeners, changeListeners.length + 1);
        grown[changeListeners.length] = listener;
        changeListeners = grown;
    }

    /**
     * Removes the first occurrence of a change listener. Does nothing if it was not added.
     *
     * @param listener listener to remove; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void removeChangeListener(ChangeListener<? super Slider> listener) {
        Objects.requireNonNull(listener, "listener");
        int index = Arrays.asList(changeListeners).indexOf(listener);
        if (index < 0) {
            return;
        }
        ChangeListener<?>[] shrunk = new ChangeListener<?>[changeListeners.length - 1];
        System.arraycopy(changeListeners, 0, shrunk, 0, index);
        System.arraycopy(changeListeners, index + 1, shrunk, index, shrunk.length - index);
        changeListeners = shrunk.length == 0 ? NO_LISTENERS : shrunk;
    }

    /**
     * Returns whether the slider can take keyboard focus, which is the case unless it is disabled.
     *
     * @return {@code true} when enabled
     */
    @Override
    public boolean isFocusable() {
        return !disabled;
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
     * Returns the length for a horizontal slider and 1 for a vertical slider.
     *
     * @return the width in UI cells
     */
    @Override
    public int getPreferredWidth() {
        return axis.widthFor(preferredLengthCells);
    }

    /**
     * Returns the height of one cell (1 for the square font, 2 for the tall font) for a
     * horizontal slider, and the length times that for a vertical slider.
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
        LabelStyle trackLook = currentTrackLook();
        LabelStyle knobLook = currentKnobLook();
        target.eraseGlyphs(left, top, width, height);
        target.fillBackground(left, top, width, height, trackLook.getBackgroundRgba());
        int cellCount = axis.cellCountIn(width, height);
        if (cellCount == 0) {
            return;
        }
        int trackGlyph = style.resolveTrackGlyph(orientation);
        int knobGlyph = style.resolveKnobGlyph();
        int knobIndex = knobIndexFor(cellCount);
        for (int index = 0; index < cellCount; index++) {
            boolean isKnob = index == knobIndex;
            drawCell(target, left, top, index, cellCount, isKnob ? knobLook : trackLook,
                    isKnob ? knobGlyph : trackGlyph);
        }
    }

    private void drawCell(UiSurface target, int left, int top, int index, int cellCount,
            LabelStyle look, int glyph) {
        int column = axis.columnOf(left, index);
        int row = axis.rowOf(top, index, cellCount);
        target.fillBackground(column, row, axis.widthFor(1), axis.heightFor(1),
                look.getBackgroundRgba());
        target.writeGlyph(look.getFont(), column, row, glyph, look.getForegroundRgba());
    }

    int knobIndexFor(int cellCount) {
        double fraction = range.fractionOf(value);
        return (int) Math.round(fraction * (cellCount - 1));
    }

    private LabelStyle currentTrackLook() {
        if (disabled) {
            return style.getTrackDisabled();
        }
        return isFocusShown() ? style.getTrackFocused() : style.getTrackNormal();
    }

    private LabelStyle currentKnobLook() {
        if (disabled) {
            return style.getKnobDisabled();
        }
        return isFocusShown() ? style.getKnobFocused() : style.getKnobNormal();
    }

    @Override
    protected boolean handleEvent(UiEvent event) {
        return switch (event.getType()) {
            case POINTER_DOWN -> handlePointerDown(event);
            case POINTER_DRAGGED -> handlePointerDragged(event);
            case POINTER_UP -> handlePointerUp(event);
            case POINTER_CANCELLED -> handlePointerCancelled(event);
            case KEY_DOWN -> handleKeyDown(event.getKeycode());
            default -> false;
        };
    }

    private boolean handlePointerDown(UiEvent event) {
        if (disabled || event.getButton() != PRIMARY_BUTTON) {
            return false;
        }
        armedPointer = event.getPointer();
        moveKnobToPointer(event);
        return true;
    }

    private boolean handlePointerDragged(UiEvent event) {
        if (event.getPointer() != armedPointer) {
            return false;
        }
        moveKnobToPointer(event);
        return true;
    }

    private boolean handlePointerUp(UiEvent event) {
        if (event.getButton() != PRIMARY_BUTTON) {
            return false;
        }
        return endDrag(event.getPointer());
    }

    private boolean handlePointerCancelled(UiEvent event) {
        return endDrag(event.getPointer());
    }

    // Consumes the end of a drag, also one that was abandoned by disabling the slider.
    private boolean endDrag(int pointer) {
        if (pointer == armedPointer) {
            armedPointer = NO_POINTER;
            return true;
        }
        if (pointer == abandonedPointer) {
            abandonedPointer = NO_POINTER;
            return true;
        }
        return false;
    }

    private void moveKnobToPointer(UiEvent event) {
        int cellCount = axis.cellCountIn(getWidth(), getHeight());
        if (cellCount == 0) {
            return;
        }
        int index = axis.indexAt(event.getColumn(), event.getRow(), cellCount);
        float fraction = cellCount == 1 ? 0f : (float) index / (cellCount - 1);
        changeByUser(range.valueAtFraction(fraction));
    }

    private boolean handleKeyDown(int keycode) {
        if (disabled) {
            return false;
        }
        if (keycode == Input.Keys.HOME) {
            changeByUser(range.getMin());
            return true;
        }
        if (keycode == Input.Keys.END) {
            changeByUser(range.getMax());
            return true;
        }
        if (keycode == keyForStepUp()) {
            changeByUser(range.stepUp(value));
            return true;
        }
        if (keycode == keyForStepDown()) {
            changeByUser(range.stepDown(value));
            return true;
        }
        return false;
    }

    private int keyForStepUp() {
        return orientation == BarOrientation.HORIZONTAL ? Input.Keys.RIGHT : Input.Keys.UP;
    }

    private int keyForStepDown() {
        return orientation == BarOrientation.HORIZONTAL ? Input.Keys.LEFT : Input.Keys.DOWN;
    }

    private void changeByUser(float newValue) {
        if (newValue == value) {
            return;
        }
        value = newValue;
        requestRedraw();
        fireChanged();
    }

    @SuppressWarnings("unchecked")
    private void fireChanged() {
        ChangeListener<?>[] snapshot = changeListeners;
        for (int i = 0; i < snapshot.length; i++) {
            ((ChangeListener<? super Slider>) snapshot[i]).changed(this);
        }
    }
}
