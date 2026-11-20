package com.github.halfdeadgames.asciigdx.ui;

/** The kinds of input event a {@link UiRoot} delivers to widgets. */
public enum UiEventType {
    /** A pointer (mouse button or finger) went down on a widget. Bubbles. */
    POINTER_DOWN,
    /** A pointer that was pressed on a widget went up. Goes to the pressed widget. Bubbles. */
    POINTER_UP,
    /** The mouse moved with no button down, over a widget. Bubbles. */
    POINTER_MOVED,
    /** A pressed pointer moved. Goes to the widget that was pressed, even outside it. Bubbles. */
    POINTER_DRAGGED,
    /** The system canceled a pressed pointer. Goes to the pressed widget. Bubbles. */
    POINTER_CANCELLED,
    /** The pointer came over a widget. Does not bubble; the return value is ignored. */
    POINTER_ENTERED,
    /** The pointer left a widget. Does not bubble; the return value is ignored. */
    POINTER_EXITED,
    /** A key went down. Goes to the keyboard-focused widget. Bubbles. */
    KEY_DOWN,
    /** A key went up. Goes to the keyboard-focused widget. Bubbles. */
    KEY_UP,
    /** A character was typed. Goes to the keyboard-focused widget. Bubbles. */
    KEY_TYPED,
    /**
     * The wheel or a trackpad scrolled over the hovered widget, by whole steps (see
     * {@link UiRoot#scrolled}). Goes to the widget under pointer 0 and bubbles. Carries no
     * position; read the steps with {@link UiEvent#getScrollStepsX()} and
     * {@link UiEvent#getScrollStepsY()}.
     */
    SCROLLED
}
