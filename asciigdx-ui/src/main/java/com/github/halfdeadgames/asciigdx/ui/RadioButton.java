package com.github.halfdeadgames.asciigdx.ui;

/**
 * A round choice that is on or off, drawn as a mark, a gap, and one line of text, such as
 * {@code (*) Easy}. Drawing, sizing, press rules, keys, and the disabled state are those of
 * {@link CheckBox}; see there for the details.
 *
 * <p>A click, Enter, numpad Enter, or Space turns a radio button on. A radio button that is
 * already on stays on, so the user cannot turn it off. Radio buttons that should exclude each
 * other are added to one {@link ButtonGroup}; a radio button without a group turns on and
 * stays on until code turns it off.
 *
 * <p>A {@link ChangeListener} is called only after a change by the user, and the listener of the
 * radio button the user turned on is called last. {@link #setChecked} never calls listeners, and
 * neither does the group when code changes a selection.
 *
 * <p>In a group, one click calls the listener of the radio button that turned off and then the
 * listener of the one that turned on.
 *
 * <p>Important: listeners also fire when a radio button turns off because the user chose another
 * member of its group. A listener must check {@link #isChecked()} before it acts, as in the
 * example.
 *
 * <p>Example:
 * <pre>{@code
 * RadioButton easy = new RadioButton("Easy", skin.getRadioButtonStyle("radioButton"));
 * easy.addChangeListener(radio -> {
 *     if (radio.isChecked()) {
 *         setLevel(Level.EASY);
 *     }
 * });
 * }</pre>
 */
public final class RadioButton extends Widget {

    private final ToggleBody<RadioButton> body;
    private ButtonGroup group;

    /**
     * Creates an unchecked radio button without a group, sized to fit its mark and text.
     *
     * @param text text to show after the mark; must not be {@code null}; may be empty
     * @param style looks, marks, and gap; must not be {@code null}
     * @throws NullPointerException if {@code text} or {@code style} is {@code null}
     */
    public RadioButton(String text, ToggleStyle style) {
        body = new ToggleBody<>(this, text, style, this::selectByUser);
        pack();
    }

    /**
     * Returns the group this radio button was added to.
     *
     * @return the group, or {@code null} when it has none
     */
    public ButtonGroup getGroup() {
        return group;
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
     * Replaces the looks, marks, and gap, with the marking rules of
     * {@link CheckBox#setStyle(ToggleStyle)}.
     *
     * @param style new style; must not be {@code null}
     * @throws NullPointerException if {@code style} is {@code null}
     */
    public void setStyle(ToggleStyle style) {
        body.setStyle(style);
    }

    /**
     * Returns whether the radio button is on.
     *
     * @return {@code true} when checked
     */
    public boolean isChecked() {
        return body.isChecked();
    }

    /**
     * Turns the radio button on or off from code. Turning it on also turns off the other members
     * of its group. A change requests a redraw; no listener is called, including those of the radio
     * buttons the group turned off. Turning a member off leaves its group with no checked member.
     * For a member, {@code setChecked(true)} is equivalent to
     * {@link ButtonGroup#setChecked(RadioButton)}; unlike that method, this one never throws, even
     * on a radio button without a group.
     *
     * @param checked {@code true} to turn the radio button on
     */
    public void setChecked(boolean checked) {
        if (checked && group != null) {
            group.select(this);
            return;
        }
        body.setChecked(checked);
    }

    /**
     * Enables or disables the radio button, with the rules of
     * {@link CheckBox#setDisabled(boolean)}.
     *
     * @param disabled {@code true} to disable the radio button
     */
    public void setDisabled(boolean disabled) {
        body.setDisabled(disabled);
    }

    /**
     * Returns whether the radio button is disabled.
     *
     * @return {@code true} when disabled
     */
    public boolean isDisabled() {
        return body.isDisabled();
    }

    /**
     * Returns whether the radio button currently shows its pressed look.
     *
     * @return {@code true} while pressed
     */
    public boolean isPressed() {
        return body.isPressed();
    }

    /**
     * Returns whether the hovering pointer is over this radio button, as reported by the
     * {@link UiRoot}.
     *
     * @return {@code true} while the pointer is over the radio button
     */
    public boolean isPointerOver() {
        return body.isPointerOver();
    }

    /**
     * Adds a listener that is called after every change by the user, in the order the listeners
     * were added. The change is either this radio button turning on, or, in a group, another
     * member turning on and this one turning off. Safe to call from inside a listener; the change
     * applies from the next change.
     *
     * @param listener listener to add; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void addChangeListener(ChangeListener<? super RadioButton> listener) {
        body.addChangeListener(listener);
    }

    /**
     * Removes the first occurrence of a change listener. Does nothing if it was not added.
     *
     * @param listener listener to remove; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void removeChangeListener(ChangeListener<? super RadioButton> listener) {
        body.removeChangeListener(listener);
    }

    /**
     * Returns whether the radio button can take keyboard focus, which is the case unless it is
     * disabled.
     *
     * @return {@code true} when enabled
     */
    @Override
    public boolean isFocusable() {
        return !body.isDisabled();
    }

    /**
     * Returns the padding on both sides plus the mark (one UI cell per code point), the gap, and
     * the text (two code points per UI cell, rounded up).
     *
     * @return the width in UI cells, zero or greater
     */
    @Override
    public int getPreferredWidth() {
        return body.getPreferredWidth();
    }

    /**
     * Returns the height of one line.
     *
     * @return 1 UI cell
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

    void joinGroup(ButtonGroup newGroup) {
        group = newGroup;
    }

    void leaveGroup() {
        group = null;
    }

    void applyChecked(boolean checked) {
        body.setChecked(checked);
    }

    void fireChanged() {
        body.fireChanged();
    }

    private void selectByUser() {
        if (body.isChecked()) {
            return;
        }
        if (group != null) {
            group.selectByUser(this);
            return;
        }
        body.setChecked(true);
        body.fireChanged();
    }
}
