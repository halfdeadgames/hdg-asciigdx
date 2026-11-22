package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import java.util.List;

/**
 * What the panel keeps between rebuilds, so a change to the skin does not lose the chosen style or
 * the scroll position.
 */
public final class SkinEditorMemory {
    private StyleChoice selectedStyle;
    private int scrollRow;
    private boolean scrollResetPending;
    private String focusedFieldKey;

    /** Returns the remembered style if it is still offered, otherwise the first one. */
    StyleChoice pickFrom(List<StyleChoice> choices) {
        if (selectedStyle != null && choices.contains(selectedStyle)) {
            return selectedStyle;
        }
        return choices.get(0);
    }

    void rememberSelection(StyleChoice choice) {
        selectedStyle = choice;
    }

    /** Makes the next panel start at the top, as when another example is shown. */
    public void forgetScrollPosition() {
        scrollRow = 0;
    }

    int getScrollRow() {
        return scrollRow;
    }

    /**
     * Remembers where the panel was scrolled to. A pending reset wins once: the next call stores
     * the top instead, so a position saved while the old panel is torn down cannot undo it.
     */
    void rememberScrollRow(int row) {
        if (scrollResetPending) {
            scrollResetPending = false;
            scrollRow = 0;
            return;
        }
        scrollRow = row;
    }

    /** Makes the next {@link #rememberScrollRow} store the top, whatever it is given. */
    void requestScrollReset() {
        scrollResetPending = true;
    }

    void rememberFocusedField(String focusKey) {
        focusedFieldKey = focusKey;
    }

    /** Returns the key of the field that had focus before the rebuild, or {@code null}. */
    String takeFocusedField() {
        String focusKey = focusedFieldKey;
        focusedFieldKey = null;
        return focusKey;
    }
}
