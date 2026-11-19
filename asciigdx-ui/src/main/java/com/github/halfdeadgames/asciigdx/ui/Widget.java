package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * Base class of everything that can be drawn into a {@link UiSurface}. A widget has a position
 * and a size, both in UI cells of the surface, and an optional parent {@link WidgetGroup}.
 *
 * <p>Coordinates: columns grow to the right and rows grow downward (y-down). The position is
 * relative to the parent's top-left corner; a widget with no parent is placed relative to the
 * target surface's top-left corner.
 *
 * <p>Not thread-safe. Use widgets from the render thread, like {@link UiSurface}.
 */
public abstract class Widget {

    private int x;
    private int y;
    private int width;
    private int height;
    private WidgetGroup parent;

    /** Creates a widget at position (0, 0), with size 0 x 0 and no parent. */
    protected Widget() {
    }

    /**
     * Returns the column of the left edge, relative to the parent's left edge, in UI cells.
     *
     * @return the column, which may be negative
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the row of the top edge, relative to the parent's top edge, in UI cells.
     *
     * @return the row, which may be negative
     */
    public int getY() {
        return y;
    }

    /**
     * Returns the width in UI cells.
     *
     * @return the width, zero or greater
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height in UI cells.
     *
     * @return the height, zero or greater
     */
    public int getHeight() {
        return height;
    }

    /**
     * Moves the widget. The position is relative to the parent's top-left corner, or to the
     * target surface's top-left corner when there is no parent. Takes effect on the next
     * {@link #draw(UiSurface)}.
     *
     * @param x column of the left edge, in UI cells; any value, including negative
     * @param y row of the top edge, in UI cells; any value, including negative
     */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Resizes the widget. Takes effect on the next {@link #draw(UiSurface)}.
     *
     * @param width width in UI cells; zero or greater
     * @param height height in UI cells; zero or greater
     * @throws IllegalArgumentException if {@code width} or {@code height} is negative
     */
    public void setSize(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException(
                    "Size must not be negative, was " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
    }

    /**
     * Returns the group this widget belongs to.
     *
     * @return the parent, or {@code null} when the widget has none
     */
    public WidgetGroup getParent() {
        return parent;
    }

    /**
     * Returns the width this widget needs to show its content completely.
     *
     * @return the preferred width in UI cells, zero or greater
     */
    public abstract int getPreferredWidth();

    /**
     * Returns the height this widget needs to show its content completely.
     *
     * @return the preferred height in UI cells, zero or greater
     */
    public abstract int getPreferredHeight();

    /**
     * Sets the size to the preferred size, as in
     * {@code setSize(getPreferredWidth(), getPreferredHeight())}.
     */
    public void pack() {
        setSize(getPreferredWidth(), getPreferredHeight());
    }

    /**
     * Draws the widget at its absolute position, which is its position plus the positions of all
     * its ancestors, in UI cells of the surface. Parts outside the surface are skipped. Does not
     * allocate. Call it from the render thread, each frame or whenever the widget must be
     * redrawn.
     *
     * @param target surface to draw into; must not be {@code null}
     * @throws NullPointerException if {@code target} is {@code null}
     */
    public final void draw(UiSurface target) {
        Objects.requireNonNull(target, "target");
        int left = 0;
        int top = 0;
        for (Widget current = this; current != null; current = current.parent) {
            left += current.x;
            top += current.y;
        }
        drawContent(target, left, top);
    }

    /**
     * Draws this widget's own content. Called by {@link #draw(UiSurface)} with the absolute
     * position already computed. Implementations must skip cells outside the surface and must not
     * allocate.
     *
     * @param target surface to draw into; never {@code null}
     * @param left column of the widget's left edge in {@code target}, in UI cells; may be negative
     *         or past the surface
     * @param top row of the widget's top edge in {@code target}, in UI cells; may be negative or
     *         past the surface
     */
    protected abstract void drawContent(UiSurface target, int left, int top);

    void attachTo(WidgetGroup newParent) {
        parent = newParent;
    }
}
