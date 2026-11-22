package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.UiUpdateListener;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.ArrayList;
import java.util.List;

/**
 * Commits a watched text field when it loses keyboard focus. The text field offers no focus
 * notification, so this checks the focus owner once per frame.
 */
final class FocusLossCommits implements UiUpdateListener {
    private final UiRoot uiRoot;
    private final List<CommittedTextField> watchedFields = new ArrayList<>();
    private final List<Boolean> hadFocus = new ArrayList<>();

    FocusLossCommits(UiRoot uiRoot) {
        this.uiRoot = uiRoot;
    }

    void start() {
        uiRoot.addUpdateListener(this);
    }

    void stop() {
        uiRoot.removeUpdateListener(this);
    }

    void watch(CommittedTextField field) {
        watchedFields.add(field);
        hadFocus.add(Boolean.FALSE);
    }

    /** Commits every watched field whose text differs from its last committed text. */
    void commitAllChanged() {
        for (int i = 0; i < watchedFields.size(); i++) {
            watchedFields.get(i).commitIfChanged();
        }
    }

    /** Returns the focus key of the watched field that has keyboard focus, or {@code null}. */
    String findFocusedKey() {
        Widget focused = uiRoot.getKeyboardFocus();
        for (int i = 0; i < watchedFields.size(); i++) {
            if (focused == watchedFields.get(i).getField()) {
                return watchedFields.get(i).getFocusKey();
            }
        }
        return null;
    }

    /** Gives keyboard focus to the watched field with the key; does nothing if there is none. */
    void focusFieldWithKey(String focusKey) {
        for (int i = 0; i < watchedFields.size(); i++) {
            if (watchedFields.get(i).getFocusKey().equals(focusKey)) {
                uiRoot.setKeyboardFocus(watchedFields.get(i).getField());
                return;
            }
        }
    }

    @Override
    public void update(float deltaSeconds) {
        Widget focused = uiRoot.getKeyboardFocus();
        for (int i = 0; i < watchedFields.size(); i++) {
            CommittedTextField watched = watchedFields.get(i);
            boolean hasFocus = focused == watched.getField();
            boolean lostFocus = hadFocus.get(i) && !hasFocus;
            hadFocus.set(i, hasFocus);
            if (lostFocus) {
                watched.commitIfChanged();
            }
        }
    }
}
