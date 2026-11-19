package com.github.halfdeadgames.asciigdx.ui;

/**
 * Receives the selection changes a user makes in a {@link ButtonGroup}, added with
 * {@link ButtonGroup#addChangeListener(GroupChangeListener)}. It is called once per user action
 * that changes which radio button is checked, after the listeners of the radio buttons involved,
 * and never for changes made from code.
 *
 * <p>It is called once per user selection even if a radio button listener removed a radio button
 * from the group or unchecked one meanwhile, so {@link ButtonGroup#getChecked()} may then return
 * {@code null} or a different radio button.
 */
@FunctionalInterface
public interface GroupChangeListener {

    /**
     * Called after the user changed the selection of a group. Read the new selection with
     * {@link ButtonGroup#getChecked()}.
     *
     * @param group the group whose selection changed; never {@code null}
     */
    void changed(ButtonGroup group);
}
