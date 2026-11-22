package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.github.halfdeadgames.asciigdx.ui.TextField;

/**
 * A text field whose text counts as one edit when the user presses Enter or leaves the field, not
 * with every keystroke. Leaving the field is noticed by {@link FocusLossCommits}.
 */
final class CommittedTextField {

    /** Receives the committed text of a field. */
    @FunctionalInterface
    interface TextCommit {
        /**
         * Handles the new text.
         *
         * @param text the text the user committed
         * @param revertField puts the previous text back in the field; call it when the text was
         *        refused
         */
        void commit(String text, Runnable revertField);
    }

    private final TextField field;
    private final TextCommit commit;
    private final String focusKey;
    private String committedText;

    /**
     * @param focusKey a name that is the same for the same key in every rebuilt panel, so focus
     *        can return to the field after a rebuild
     */
    CommittedTextField(TextField field, String focusKey, String initialText, TextCommit commit) {
        this.field = field;
        this.focusKey = focusKey;
        this.commit = commit;
        this.committedText = initialText;
        field.setText(initialText);
        field.addEnterListener(source -> {
            commitIfChanged();
            return true;
        });
    }

    String getFocusKey() {
        return focusKey;
    }

    TextField getField() {
        return field;
    }

    void commitIfChanged() {
        String text = field.getText();
        if (text.equals(committedText)) {
            return;
        }
        String previousText = committedText;
        committedText = text;
        commit.commit(text, () -> {
            committedText = previousText;
            field.setText(previousText);
        });
    }
}
