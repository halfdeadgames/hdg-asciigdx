package com.github.halfdeadgames.asciigdx.ui;

import java.util.Arrays;
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

    private static final UiListener[] NO_LISTENERS = new UiListener[0];

    private UiListener[] listeners = NO_LISTENERS;
    private int x;
    private int y;
    private int width;
    private int height;
    private WidgetGroup parent;
    private boolean layoutNeeded = true;
    private boolean redrawNeeded = true;
    private boolean inLayout;
    private boolean focusShown;
    private boolean keyboardFocused;
    private boolean visible = true;

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
     * <p>When the position actually changes, this widget is marked for a redraw (not for layout),
     * and every ancestor is marked for layout and a redraw, because a plain {@link WidgetGroup}
     * sizes itself from the positions of its children. As in {@link #invalidate()}, layout marks
     * stop below the nearest ancestor that is currently running its own {@link #layout()}, so a
     * layout that places its children does not request itself again; redraw marks always reach
     * every ancestor. Setting the same position changes no marks.
     *
     * @param x column of the left edge, in UI cells; any value, including negative
     * @param y row of the top edge, in UI cells; any value, including negative
     */
    public void setPosition(int x, int y) {
        if (this.x == x && this.y == y) {
            return;
        }
        this.x = x;
        this.y = y;
        redrawNeeded = true;
        markAncestors();
    }

    /**
     * Resizes the widget. Takes effect on the next {@link #draw(UiSurface)}. When the size
     * actually changes, calls {@link #invalidate()}; setting the same size changes no marks.
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
        if (this.width == width && this.height == height) {
            return;
        }
        this.width = width;
        this.height = height;
        invalidate();
    }

    /**
     * Marks this widget and its ancestors as needing layout and a redraw, because a change here
     * can alter the space its ancestors give it. Call it after changing anything that affects
     * the widget's preferred size.
     *
     * <p>Propagation rules:
     * <ul>
     *   <li>Redraw marks always reach this widget and every ancestor.
     *   <li>Layout marks reach this widget and the ancestors up to, but not including, the
     *       nearest ancestor that is currently running its own {@link #layout()} (inside
     *       {@link #validate()}); that ancestor and those above it are left alone.
     *   <li>If this widget is itself running its own {@code layout()}, it gets the redraw mark
     *       but not the layout mark.
     * </ul>
     * Together these let a layout set the sizes of its children, or of itself, without
     * requesting layout again every frame.
     */
    public void invalidate() {
        redrawNeeded = true;
        if (!inLayout) {
            layoutNeeded = true;
        }
        markAncestors();
    }

    private void markAncestors() {
        boolean layoutStopped = false;
        for (Widget ancestor = parent; ancestor != null; ancestor = ancestor.parent) {
            ancestor.redrawNeeded = true;
            layoutStopped |= ancestor.inLayout;
            if (!layoutStopped) {
                ancestor.layoutNeeded = true;
            }
        }
    }

    /**
     * Marks a visual-only change: this widget and every ancestor need a redraw. No layout mark is
     * set. Call it when the look changes but the size does not.
     */
    public void requestRedraw() {
        for (Widget current = this; current != null; current = current.parent) {
            current.redrawNeeded = true;
        }
    }

    /**
     * Returns whether this widget must run {@link #layout()} on the next {@link #validate()}. A
     * new widget starts with this set.
     *
     * @return {@code true} when layout is needed
     */
    public boolean isLayoutNeeded() {
        return layoutNeeded;
    }

    /**
     * Returns whether this widget, or a descendant, changed since the last redraw. A new widget
     * starts with this set. {@link UiRoot} clears the mark after it
     * redraws; {@link #draw(UiSurface)} does not clear it.
     *
     * @return {@code true} when a redraw is needed
     */
    public boolean isRedrawNeeded() {
        return redrawNeeded;
    }

    /**
     * Runs layout where it is needed: if this widget is marked for layout, clears the mark and
     * calls {@link #layout()}, then validates the children (for a group) in draw order. So this
     * widget's layout always runs before its children's. Call it once per frame before drawing;
     * it is cheap and safe to call every frame when nothing is marked.
     *
     * <p>If {@code layout()} throws, the layout mark stays cleared and the exception propagates;
     * the children are not validated.
     */
    public final void validate() {
        if (layoutNeeded) {
            layoutNeeded = false;
            inLayout = true;
            try {
                layout();
            } finally {
                inLayout = false;
            }
        }
        validateChildren();
    }

    /**
     * Places this widget's children by setting their positions and sizes. Subclass contract:
     * <ul>
     *   <li>{@link #validate()} calls it only when layout is needed; place and size the children
     *       here.
     *   <li>Marks raised during this call, such as by {@code setSize} on a child, do not mark
     *       this widget or its ancestors for layout again (see {@link #invalidate()}); redraw
     *       marks still go up.
     *   <li>The children are validated right after it returns.
     *   <li>Do not call {@code validate()} or draw from it.
     *   <li>There is no need to call {@code super.layout()}.
     * </ul>
     * Does nothing by default.
     */
    protected void layout() {
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
     * <p>Drawing starts at a shown widget: a hidden widget (see {@link #setVisible(boolean)})
     * draws nothing for itself or its children. This method does not check the ancestors, so
     * drawing a widget under a hidden ancestor directly still draws it; {@link UiRoot} and
     * {@link WidgetGroup} skip hidden children, so a tree drawn from its top is never affected.
     *
     * @param target surface to draw into; must not be {@code null}
     * @throws NullPointerException if {@code target} is {@code null}
     */
    public final void draw(UiSurface target) {
        Objects.requireNonNull(target, "target");
        if (!visible) {
            return;
        }
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

    /**
     * Returns the deepest widget at a point. A plain widget answers itself when the point lies in
     * its bounds and {@code null} otherwise; {@link WidgetGroup} also checks its children.
     * {@link #isFocusable()} plays no part. A hidden widget (see {@link #setVisible(boolean)}) is
     * never hit, nor are its descendants. A parent {@link WidgetGroup} and {@link UiRoot} skip a
     * hidden child without calling this method, so an override cannot make a hidden widget
     * hittable.
     *
     * @param column column in this widget's local coordinates, where 0 is its left edge, in UI
     *         cells; any value
     * @param row row in this widget's local coordinates, where 0 is its top edge, in UI cells; any
     *         value
     * @return this widget when it is visible and {@code 0 <= column < getWidth()} and
     *         {@code 0 <= row < getHeight()}, otherwise {@code null}
     */
    public Widget hit(int column, int row) {
        return visible && isInsideBounds(column, row) ? this : null;
    }

    /**
     * Returns whether the widget can receive keyboard focus. {@link UiRoot} gives focus on a
     * pointer press to the deepest widget under the pointer, or its nearest ancestor, for which
     * this returns {@code true}. The value may change at any time.
     *
     * @return {@code true} if the widget accepts keyboard focus; {@code false} by default
     */
    public boolean isFocusable() {
        return false;
    }

    /**
     * Shows or hides this widget and everything inside it. A hidden widget keeps its position,
     * size, and layout space, so a layout and the preferred sizes of its ancestors do not change.
     * It is not drawn, cannot be hit by a pointer, and is skipped by keyboard navigation, and so
     * are its descendants. For keyboard focus, hover, and pointer capture, hiding a widget or any
     * ancestor acts like removing it from its {@link UiRoot}: keyboard focus on it is cleared, a
     * pointer captured by it is canceled, and hover moves off it. The root notices this when it
     * next reads focus, handles pointer input, or runs {@code update} or {@code draw}. Showing the
     * widget again restores none of these: it has no focus, no capture, and no hover until new
     * input gives them. A change marks this widget and its ancestors for a redraw; setting the
     * same value changes nothing.
     *
     * @param visible {@code true} to show the widget, {@code false} to hide it
     */
    public void setVisible(boolean visible) {
        if (this.visible == visible) {
            return;
        }
        this.visible = visible;
        requestRedraw();
    }

    /**
     * Returns this widget's own visibility flag. A widget under a hidden ancestor still returns
     * {@code true} here but is not shown, because the ancestor hides everything inside it.
     *
     * @return {@code false} after {@code setVisible(false)}; {@code true} for a new widget
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Returns whether keyboard navigation (Tab, Shift+Tab, and the arrow keys) may move focus to
     * this widget. The default returns {@link #isFocusable()}. A widget that this returns
     * {@code false} for can still get focus from a pointer press, but navigation skips it.
     * Override it for a widget that is focusable but should not take part in navigation, such as
     * a container that only forwards presses. The root calls it during focus navigation, on the
     * render thread; keep it cheap and free of side effects.
     *
     * @return {@code true} if navigation may focus this widget
     */
    protected boolean isInFocusOrder() {
        return isFocusable();
    }

    boolean isShown() {
        for (Widget current = this; current != null; current = current.parent) {
            if (!current.visible) {
                return false;
            }
        }
        return true;
    }

    /**
     * Adds a listener for the input events that reach this widget, either aimed at it or bubbling
     * up from a descendant (see {@link UiListener}). Listeners run in the order they were added.
     * Adding the same listener twice makes it run twice. Safe to call from inside a listener,
     * including one of this widget: a dispatch that is already running uses the listeners that
     * existed when it reached this widget, and the change applies from the next dispatch on.
     *
     * @param listener listener to add; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void addListener(UiListener listener) {
        Objects.requireNonNull(listener, "listener");
        UiListener[] grown = Arrays.copyOf(listeners, listeners.length + 1);
        grown[listeners.length] = listener;
        listeners = grown;
    }

    /**
     * Removes the first occurrence of a listener. Does nothing if it was not added. The same
     * dispatch rules as in {@link #addListener(UiListener)} apply.
     *
     * @param listener listener to remove; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void removeListener(UiListener listener) {
        Objects.requireNonNull(listener, "listener");
        int index = Arrays.asList(listeners).indexOf(listener);
        if (index < 0) {
            return;
        }
        UiListener[] shrunk = new UiListener[listeners.length - 1];
        System.arraycopy(listeners, 0, shrunk, 0, index);
        System.arraycopy(listeners, index + 1, shrunk, index, shrunk.length - index);
        listeners = shrunk.length == 0 ? NO_LISTENERS : shrunk;
    }

    boolean isInsideBounds(int column, int row) {
        return column >= 0 && column < width && row >= 0 && row < height;
    }

    /**
     * Reacts to an event that reaches this widget, either aimed at it or bubbling up from a
     * descendant. It runs first, before any listener added with
     * {@link #addListener(UiListener)}; when it returns {@code true}, the event counts as
     * consumed and no listener of this widget runs, and it does not bubble further. The default
     * handles nothing and returns {@code false}, so a direct subclass of {@code Widget} need not
     * call {@code super.handleEvent(event)}. A subclass of a built-in widget, such as
     * {@link TextButton}, must call {@code super.handleEvent(event)} to keep that widget's
     * behavior. Runs on the render thread while the root delivers input. The event object is
     * reused, so do not keep a reference to it. Do not call input methods of the {@link UiRoot}
     * from here.
     *
     * <p>This is an input handler, so it returns whether the event was consumed.
     *
     * @param event event being delivered; never {@code null}
     * @return {@code true} if the event was consumed
     */
    protected boolean handleEvent(UiEvent event) {
        return false;
    }

    final boolean dispatch(UiEvent event) {
        if (handleEvent(event)) {
            return true;
        }
        UiListener[] snapshot = listeners;
        for (int i = 0; i < snapshot.length; i++) {
            if (snapshot[i].handle(event)) {
                return true;
            }
        }
        return false;
    }

    void validateChildren() {
    }

    /**
     * Returns whether this widget has keyboard focus and the focus look should be drawn. Call it
     * while drawing content to choose between the normal and the focused look. It is {@code false}
     * when the widget has focus but the last input was a pointer press (see
     * {@link UiRoot#isFocusVisible()}).
     *
     * @return {@code true} while this widget has keyboard focus and the focus look should be drawn
     */
    protected boolean isFocusShown() {
        return focusShown;
    }

    void setFocusShown(boolean focusShown) {
        if (this.focusShown == focusShown) {
            return;
        }
        this.focusShown = focusShown;
        requestRedraw();
    }

    // Set by UiRoot on the widget that owns keyboard focus, whether or not the focus look shows.
    boolean hasKeyboardFocus() {
        return keyboardFocused;
    }

    final void setKeyboardFocused(boolean keyboardFocused) {
        if (this.keyboardFocused == keyboardFocused) {
            return;
        }
        this.keyboardFocused = keyboardFocused;
        onKeyboardFocusChanged();
        requestRedraw();
    }

    /** Hook run when this widget gains or loses keyboard focus. */
    void onKeyboardFocusChanged() {
    }

    /** Hook run by {@link UiRoot#update(float)} on the focus owner, with the frame time. */
    void updateWhileFocused(float deltaSeconds) {
    }

    void clearRedrawNeeded() {
        redrawNeeded = false;
    }

    void attachTo(WidgetGroup newParent) {
        parent = newParent;
    }
}
