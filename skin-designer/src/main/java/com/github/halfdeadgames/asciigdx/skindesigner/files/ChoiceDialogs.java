package com.github.halfdeadgames.asciigdx.skindesigner.files;

import com.github.halfdeadgames.asciigdx.ui.ChoiceListener;
import com.github.halfdeadgames.asciigdx.ui.Dialog;
import com.github.halfdeadgames.asciigdx.ui.DialogChoice;
import com.github.halfdeadgames.asciigdx.ui.DialogStyle;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.Table;

/** Builds question dialogs whose buttons have our own texts instead of Yes, No, and Cancel. */
final class ChoiceDialogs {
    private static final String LINE_BREAK_PATTERN = "\\R";

    /** One button: its text and the choice it reports. */
    record ChoiceButton(String text, DialogChoice choice) {
    }

    private ChoiceDialogs() {
    }

    /**
     * Creates a dialog that is not shown yet.
     *
     * @param backOutChoice what Escape and the close button report; one of the button choices
     */
    static Dialog create(DialogStyle style, String title, String message,
            DialogChoice backOutChoice, ChoiceListener listener, ChoiceButton... buttons) {
        Dialog dialog = new Dialog(title, style, backOutChoice);
        dialog.setContent(createMessage(style, message));
        for (ChoiceButton button : buttons) {
            dialog.addButton(button.text(), button.choice());
        }
        dialog.addChoiceListener(listener);
        return dialog;
    }

    private static Table createMessage(DialogStyle style, String message) {
        Table lines = new Table();
        for (String text : message.split(LINE_BREAK_PATTERN, -1)) {
            lines.add(new Label(text, style.getMessage()));
            lines.row();
        }
        return lines;
    }
}
