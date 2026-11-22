package com.github.halfdeadgames.asciigdx.ui;

/**
 * How wide the open list of a {@link SelectBox} is. Set with
 * {@link SelectBox#setListWidth(SelectBoxListWidth)}.
 *
 * <p>Example:
 * <pre>{@code
 * box.setListWidth(SelectBoxListWidth.MATCH_BUTTON);
 * }</pre>
 *
 * <p>This enum may gain constants in a later version, so callers should not rely on it having
 * exactly two values.
 */
public enum SelectBoxListWidth {

    /**
     * The list is exactly as wide as the select box. An entry that does not fit is cut off and
     * ends with an ellipsis (U+2026).
     */
    MATCH_BUTTON,

    /**
     * The list is at least as wide as the select box and wider when an entry needs more room. The
     * column of the scroll bar, when it is shown, is added to that width. The list is moved left to
     * stay on the surface. An entry ends with an ellipsis only when the list would be wider than
     * the whole surface.
     */
    FIT_ENTRIES
}
