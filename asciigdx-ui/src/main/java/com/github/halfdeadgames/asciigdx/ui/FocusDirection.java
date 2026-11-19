package com.github.halfdeadgames.asciigdx.ui;

/**
 * A direction in which keyboard focus can move, for {@link UiRoot#moveFocus(FocusDirection)}.
 * {@link #NEXT} and {@link #PREVIOUS} follow the order of the widgets in the main tree; the four
 * others move to the nearest focusable widget on the surface in that direction. On the surface,
 * columns grow to the right and rows grow downward (y-down), so {@link #DOWN} moves to a higher row
 * number.
 */
public enum FocusDirection {
    /** The next focusable widget in tree order; wraps from the last to the first. */
    NEXT,
    /** The previous focusable widget in tree order; wraps from the first to the last. */
    PREVIOUS,
    /** The nearest focusable widget above the focused one; does not wrap. */
    UP,
    /** The nearest focusable widget below the focused one; does not wrap. */
    DOWN,
    /** The nearest focusable widget left of the focused one; does not wrap. */
    LEFT,
    /** The nearest focusable widget right of the focused one; does not wrap. */
    RIGHT
}
