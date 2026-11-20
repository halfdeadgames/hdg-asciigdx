package com.github.halfdeadgames.asciigdx.ui;

/**
 * A box that is checked or unchecked, drawn as a mark, a gap, and one line of text, such as
 * {@code [x] Sound}. It fills its whole area with the background of its current state and writes
 * the mark and the text on its top row, starting after the horizontal padding of the style's
 * {@link ButtonStyle}. The mark and the text take one UI column per code point and are cut off at
 * the widget's width.
 *
 * <p>The user toggles it with a click, or with Enter, numpad Enter, or Space while it has keyboard
 * focus. The press rules are those of {@link TextButton}: the primary button presses it and
 * captures the pointer, dragging outside removes the pressed look, and releasing inside toggles.
 * A disabled check box ignores input, never toggles, and is not focusable.
 *
 * <p>State look, in priority order: disabled, pressed, pointer over, focused, normal. The focused
 * look shows only while the check box has keyboard focus and the {@link UiRoot} shows focus (see
 * {@link UiRoot#isFocusVisible()}). The mark uses the same look as the text. Every state change
 * requests a redraw.
 *
 * <p>A {@link ChangeListener} is called only after a toggle by the user. {@link #setChecked}
 * never calls listeners.
 *
 * <p>A new check box is unchecked and packed to its preferred size. Changing the text or the style
 * keeps the current size, so call {@link #pack()} afterward.
 *
 * <p>Example:
 * <pre>{@code
 * CheckBox sound = new CheckBox("Sound", skin.getCheckBoxStyle("checkBox"));
 * sound.setChecked(true);
 * sound.addChangeListener(box -> setSoundOn(box.isChecked()));
 * ui.getRoot().addChild(sound);
 * }</pre>
 */
public final class CheckBox extends Widget {

    private final ToggleBody<CheckBox> body;

    /**
     * Creates an unchecked check box sized to fit its mark and text.
     *
     * @param text text to show after the mark; must not be {@code null}; may be empty
     * @param style looks, marks, and gap; must not be {@code null}
     * @throws NullPointerException if {@code text} or {@code style} is {@code null}
     */
    public CheckBox(String text, ToggleStyle style) {
        body = new ToggleBody<>(this, text, style, this::toggleByUser);
        pack();
    }

    /**
     * Returns the text.
     *
     * @return the text, never {@code null}
     */
    public String getText() {
        return body.getText();
    }

    /**
     * Replaces the text. The size stays as it is; call {@link #pack()} to fit the new text. When
     * the text differs from the current one, calls {@link #invalidate()}.
     *
     * @param text new text; must not be {@code null}; may be empty
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public void setText(String text) {
        body.setText(text);
    }

    /**
     * Returns the looks, marks, and gap.
     *
     * @return the style, never {@code null}
     */
    public ToggleStyle getStyle() {
        return body.getStyle();
    }

    /**
     * Replaces the looks, marks, and gap. The size stays as it is, so call {@link #pack()} after a
     * change of font, padding, mark length, or gap. A different style is marked by
     * {@link #invalidate()} if one of those changed, otherwise by {@link #requestRedraw()}. An
     * equal style changes no marks.
     *
     * @param style new style; must not be {@code null}
     * @throws NullPointerException if {@code style} is {@code null}
     */
    public void setStyle(ToggleStyle style) {
        body.setStyle(style);
    }

    /**
     * Returns whether the box is checked.
     *
     * @return {@code true} when checked
     */
    public boolean isChecked() {
        return body.isChecked();
    }

    /**
     * Checks or unchecks the box from code. A change requests a redraw; listeners are not called.
     *
     * @param checked {@code true} to check the box
     */
    public void setChecked(boolean checked) {
        body.setChecked(checked);
    }

    /**
     * Enables or disables the check box. Disabling ends a press in progress without a toggle. A
     * change requests a redraw. A disabled check box ignores keys. Disabling the check box that
     * owns keyboard focus clears the focus the next time the {@link UiRoot} reads it or runs
     * {@code update} or {@code draw}, as for a hidden widget; enabling it again does not restore
     * the focus.
     *
     * @param disabled {@code true} to disable the check box
     */
    public void setDisabled(boolean disabled) {
        body.setDisabled(disabled);
    }

    /**
     * Returns whether the check box is disabled.
     *
     * @return {@code true} when disabled
     */
    public boolean isDisabled() {
        return body.isDisabled();
    }

    /**
     * Returns whether the check box currently shows its pressed look: it was pressed and the
     * pointer is still inside it.
     *
     * @return {@code true} while pressed
     */
    public boolean isPressed() {
        return body.isPressed();
    }

    /**
     * Returns whether the hovering pointer is over this check box, as reported by the
     * {@link UiRoot}.
     *
     * @return {@code true} while the pointer is over the check box
     */
    public boolean isPointerOver() {
        return body.isPointerOver();
    }

    /**
     * Adds a listener that is called after every toggle by the user, in the order the listeners
     * were added. Safe to call from inside a listener; the change applies from the next toggle.
     *
     * @param listener listener to add; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void addChangeListener(ChangeListener<? super CheckBox> listener) {
        body.addChangeListener(listener);
    }

    /**
     * Removes the first occurrence of a change listener. Does nothing if it was not added.
     *
     * @param listener listener to remove; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void removeChangeListener(ChangeListener<? super CheckBox> listener) {
        body.removeChangeListener(listener);
    }

    /**
     * Returns whether the check box can take keyboard focus, which is the case unless it is
     * disabled.
     *
     * @return {@code true} when enabled
     */
    @Override
    public boolean isFocusable() {
        return !body.isDisabled();
    }

    /**
     * Returns 2 when the style's font is {@link UiFont#TALL}, otherwise 1.
     *
     * @return the row multiple, 1 or 2
     */
    @Override
    public int getRowSnap() {
        return body.getRowSnap();
    }

    /**
     * Returns the padding on both sides plus the mark, the gap, and the text, one cell per code
     * point.
     *
     * @return the width in UI cells, zero or greater
     */
    @Override
    public int getPreferredWidth() {
        return body.getPreferredWidth();
    }

    /**
     * Returns 2 for the tall font and 1 for the square font.
     *
     * @return the height in UI cells, 1 or 2
     */
    @Override
    public int getPreferredHeight() {
        return body.getPreferredHeight();
    }

    @Override
    protected void drawContent(UiSurface target, int left, int top) {
        body.draw(target, left, top);
    }

    @Override
    protected boolean handleEvent(UiEvent event) {
        return body.handleEvent(event);
    }

    private void toggleByUser() {
        body.setChecked(!body.isChecked());
        body.fireChanged();
    }
}
