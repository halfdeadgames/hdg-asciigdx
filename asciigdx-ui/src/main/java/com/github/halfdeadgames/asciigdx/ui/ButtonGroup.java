package com.github.halfdeadgames.asciigdx.ui;

import java.util.Arrays;
import java.util.Objects;

/**
 * A set of {@link RadioButton}s of which at most one is checked. Turning one on turns the others
 * off. The user cannot turn off the checked member by clicking it; code can, with
 * {@link RadioButton#setChecked(boolean)} or {@link #clearChecked()}, so a group can be left with
 * no checked member. A radio button belongs to at most one group.
 *
 * <p>The class is named {@code ButtonGroup}, as in libGDX and VisUI, which most users know,
 * although it holds only radio buttons.
 *
 * <p>To take a radio button out of its group, call {@link #remove(RadioButton)} on the radio
 * button's own group, which is {@link RadioButton#getGroup()}. Removing it from another group does
 * nothing.
 *
 * <p>Toggling a member does not allocate. Adding and removing members does.
 *
 * <p>Example:
 * <pre>{@code
 * ButtonGroup level = new ButtonGroup();
 * RadioButton easy = new RadioButton("Easy", style);
 * RadioButton hard = new RadioButton("Hard", style);
 * level.add(easy, hard);
 * level.setChecked(easy);
 * hard.addChangeListener(radio -> {
 *     if (radio.isChecked()) {
 *         startHardMode();
 *     }
 * });
 * level.addChangeListener(group -> saveLevel(group.getChecked()));
 * }</pre>
 *
 * <p>Not thread-safe. Use it from the render thread, like the widgets.
 */
public final class ButtonGroup {

    private static final RadioButton[] NO_MEMBERS = new RadioButton[0];
    private static final GroupChangeListener[] NO_LISTENERS = new GroupChangeListener[0];

    private RadioButton[] members = NO_MEMBERS;
    private GroupChangeListener[] changeListeners = NO_LISTENERS;

    /** Creates an empty group. */
    public ButtonGroup() {
    }

    /**
     * Adds radio buttons to the group. All arguments are checked before any is added. If several
     * of the members are checked, the first checked radio button in group order (existing members,
     * then the arguments in order) stays checked and the others are unchecked without calling
     * listeners. An empty call does nothing.
     *
     * @param radioButtons radio buttons to add; none may be {@code null}, already in a group
     *         (this one included), or repeated
     * @throws NullPointerException if the array or an element is {@code null}
     * @throws IllegalArgumentException if a radio button already belongs to a group, or appears
     *         twice in the arguments
     */
    public void add(RadioButton... radioButtons) {
        Objects.requireNonNull(radioButtons, "radioButtons");
        requireAddable(radioButtons);
        for (RadioButton radioButton : radioButtons) {
            boolean keepChecked = radioButton.isChecked() && getChecked() == null;
            members = Arrays.copyOf(members, members.length + 1);
            members[members.length - 1] = radioButton;
            radioButton.joinGroup(this);
            if (radioButton.isChecked() && !keepChecked) {
                radioButton.applyChecked(false);
            }
        }
    }

    /**
     * Removes a radio button from the group. It keeps its checked state. Does nothing if the radio
     * button is not a member, so call it on {@link RadioButton#getGroup()}.
     *
     * @param radioButton radio button to remove; must not be {@code null}
     * @throws NullPointerException if {@code radioButton} is {@code null}
     */
    public void remove(RadioButton radioButton) {
        Objects.requireNonNull(radioButton, "radioButton");
        int index = Arrays.asList(members).indexOf(radioButton);
        if (index < 0) {
            return;
        }
        RadioButton[] shrunk = new RadioButton[members.length - 1];
        System.arraycopy(members, 0, shrunk, 0, index);
        System.arraycopy(members, index + 1, shrunk, index, shrunk.length - index);
        members = shrunk.length == 0 ? NO_MEMBERS : shrunk;
        radioButton.leaveGroup();
    }

    /**
     * Returns the checked member.
     *
     * @return the checked radio button, or {@code null} when none is checked
     */
    public RadioButton getChecked() {
        for (int i = 0; i < members.length; i++) {
            if (members[i].isChecked()) {
                return members[i];
            }
        }
        return null;
    }

    /**
     * Checks a member and unchecks the others, from code. A change requests a redraw; no listener
     * is called, neither those of the radio buttons nor those of the group. For a member this is
     * equivalent to {@code radioButton.setChecked(true)}; unlike that method, it throws for a
     * non-member.
     *
     * @param radioButton the member to check; must not be {@code null}
     * @throws NullPointerException if {@code radioButton} is {@code null}
     * @throws IllegalArgumentException if {@code radioButton} is not a member of this group
     */
    public void setChecked(RadioButton radioButton) {
        Objects.requireNonNull(radioButton, "radioButton");
        if (radioButton.getGroup() != this) {
            throw new IllegalArgumentException(
                    "\"" + radioButton.getText() + "\" is not a member of this group");
        }
        select(radioButton);
    }

    /**
     * Unchecks every member, from code. A change requests a redraw; no listener is called.
     */
    public void clearChecked() {
        for (int i = 0; i < members.length; i++) {
            members[i].applyChecked(false);
        }
    }

    /**
     * Adds a listener that is called once after every selection change made by the user, in the
     * order the listeners were added, after the listeners of the radio buttons involved. It is
     * never called for changes made from code. Safe to call from inside a listener; the change
     * applies from the next selection change.
     *
     * <p>It is called once per user selection even if a radio button listener removed a radio
     * button from the group or unchecked one meanwhile, so {@link #getChecked()} may then return
     * {@code null} or a different radio button. An exception thrown by any listener propagates to
     * the input caller and skips the listeners not yet called.
     *
     * @param listener listener to add; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void addChangeListener(GroupChangeListener listener) {
        Objects.requireNonNull(listener, "listener");
        GroupChangeListener[] grown = Arrays.copyOf(changeListeners, changeListeners.length + 1);
        grown[changeListeners.length] = listener;
        changeListeners = grown;
    }

    /**
     * Removes the first occurrence of a group change listener. Does nothing if it was not added.
     *
     * @param listener listener to remove; must not be {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void removeChangeListener(GroupChangeListener listener) {
        Objects.requireNonNull(listener, "listener");
        int index = Arrays.asList(changeListeners).indexOf(listener);
        if (index < 0) {
            return;
        }
        GroupChangeListener[] shrunk = new GroupChangeListener[changeListeners.length - 1];
        System.arraycopy(changeListeners, 0, shrunk, 0, index);
        System.arraycopy(changeListeners, index + 1, shrunk, index, shrunk.length - index);
        changeListeners = shrunk.length == 0 ? NO_LISTENERS : shrunk;
    }

    private void requireAddable(RadioButton[] radioButtons) {
        for (int i = 0; i < radioButtons.length; i++) {
            RadioButton candidate =
                    Objects.requireNonNull(radioButtons[i], "radioButtons element");
            if (candidate.getGroup() != null) {
                throw new IllegalArgumentException(
                        "\"" + candidate.getText() + "\" already belongs to a group");
            }
            for (int earlier = 0; earlier < i; earlier++) {
                if (radioButtons[earlier] == candidate) {
                    throw new IllegalArgumentException(
                            "\"" + candidate.getText() + "\" appears twice");
                }
            }
        }
    }

    /** Checks a member and unchecks the others, without calling listeners. */
    void select(RadioButton chosen) {
        for (int i = 0; i < members.length; i++) {
            if (members[i] != chosen) {
                members[i].applyChecked(false);
            }
        }
        chosen.applyChecked(true);
    }

    // A radio listener may move the radio to another group, so the winner is only notified while
    // it still belongs here and is still checked.
    void selectByUser(RadioButton chosen) {
        RadioButton previous = getChecked();
        select(chosen);
        if (previous != null && previous != chosen) {
            previous.fireChanged();
        }
        if (chosen.isChecked() && chosen.getGroup() == this) {
            chosen.fireChanged();
        }
        fireGroupChanged();
    }

    private void fireGroupChanged() {
        GroupChangeListener[] snapshot = changeListeners;
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i].changed(this);
        }
    }
}
