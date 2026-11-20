package com.github.halfdeadgames.asciigdx.ui;

import java.util.Objects;

/**
 * Ready-made {@link Dialog}s: an information dialog, two question dialogs, and a text input
 * dialog. Each factory returns a dialog that is not shown yet; call {@link Dialog#show(UiRoot)},
 * and add more listeners first if you like: a dialog made without a listener, such as by
 * {@link #ok(DialogStyle, String, String)}, can take {@link Dialog#addChoiceListener} afterward.
 * A dialog from here can be changed like any other (extra buttons, another content widget).
 *
 * <p>Messages: a message is split into lines at each line break ({@code \n}, {@code \r\n}), and
 * each line is its own {@link Label}. Lines are never wrapped, so the dialog is as wide as the
 * longest line.
 *
 * <p>Example:
 * <pre>{@code
 * Dialogs.yesNo(style, "Quit", "Really quit?\nUnsaved work is lost.",
 *         (dialog, choice) -> {
 *             if (choice == DialogChoice.YES) {
 *                 game.quit();
 *             }
 *         }).show(ui);
 * }</pre>
 */
public final class Dialogs {

    private static final String OK_TEXT = "OK";
    private static final String YES_TEXT = "Yes";
    private static final String NO_TEXT = "No";
    private static final String CANCEL_TEXT = "Cancel";
    private static final String LINE_BREAK_PATTERN = "\\R";
    private static final int GAP_ABOVE_FIELD = 1;

    private Dialogs() {
    }

    /**
     * Creates an information dialog with one OK button. Escape and the close button also report
     * {@link DialogChoice#OK}.
     *
     * @param style looks of the dialog; must not be {@code null}
     * @param title text for the top border; must not be {@code null}
     * @param message text of the dialog, one label per line; must not be {@code null}
     * @return a new dialog that is not shown
     * @throws NullPointerException if an argument is {@code null}
     */
    public static Dialog ok(DialogStyle style, String title, String message) {
        Dialog dialog = new Dialog(title, Objects.requireNonNull(style, "style"),
                DialogChoice.OK);
        dialog.setContent(createMessage(style, message));
        dialog.addButton(OK_TEXT, DialogChoice.OK);
        return dialog;
    }

    /**
     * Creates an information dialog like {@link #ok(DialogStyle, String, String)} that also
     * calls a listener when it is answered.
     *
     * @param style looks of the dialog; must not be {@code null}
     * @param title text for the top border; must not be {@code null}
     * @param message text of the dialog, one label per line; must not be {@code null}
     * @param listener called after the dialog closed; must not be {@code null}
     * @return a new dialog that is not shown
     * @throws NullPointerException if an argument is {@code null}
     */
    public static Dialog ok(DialogStyle style, String title, String message,
            ChoiceListener listener) {
        Objects.requireNonNull(listener, "listener");
        Dialog dialog = ok(style, title, message);
        dialog.addChoiceListener(listener);
        return dialog;
    }

    /**
     * Creates a question dialog with Yes and No buttons. Escape and the close button report
     * {@link DialogChoice#NO}.
     *
     * @param style looks of the dialog; must not be {@code null}
     * @param title text for the top border; must not be {@code null}
     * @param message text of the question, one label per line; must not be {@code null}
     * @param listener called after the dialog closed, with YES or NO; must not be {@code null}
     * @return a new dialog that is not shown
     * @throws NullPointerException if an argument is {@code null}
     */
    public static Dialog yesNo(DialogStyle style, String title, String message,
            ChoiceListener listener) {
        Objects.requireNonNull(listener, "listener");
        Dialog dialog = new Dialog(title, Objects.requireNonNull(style, "style"),
                DialogChoice.NO);
        dialog.setContent(createMessage(style, message));
        dialog.addButton(YES_TEXT, DialogChoice.YES);
        dialog.addButton(NO_TEXT, DialogChoice.NO);
        dialog.addChoiceListener(listener);
        return dialog;
    }

    /**
     * Creates a question dialog with Yes, No, and Cancel buttons. Escape and the close button
     * report {@link DialogChoice#CANCEL}.
     *
     * @param style looks of the dialog; must not be {@code null}
     * @param title text for the top border; must not be {@code null}
     * @param message text of the question, one label per line; must not be {@code null}
     * @param listener called after the dialog closed, with YES, NO, or CANCEL; must not be
     *         {@code null}
     * @return a new dialog that is not shown
     * @throws NullPointerException if an argument is {@code null}
     */
    public static Dialog yesNoCancel(DialogStyle style, String title, String message,
            ChoiceListener listener) {
        Objects.requireNonNull(listener, "listener");
        Dialog dialog = new Dialog(title, Objects.requireNonNull(style, "style"),
                DialogChoice.CANCEL);
        dialog.setContent(createMessage(style, message));
        dialog.addButton(YES_TEXT, DialogChoice.YES);
        dialog.addButton(NO_TEXT, DialogChoice.NO);
        dialog.addButton(CANCEL_TEXT, DialogChoice.CANCEL);
        dialog.addChoiceListener(listener);
        return dialog;
    }

    /**
     * Creates a dialog that asks for a line of text. It shows the message, a text field that
     * starts with {@code initialText}, and OK and Cancel buttons. Keyboard focus starts in the
     * field. The OK button, and Enter while the field has focus, report
     * {@link DialogChoice#OK} and then call {@code listener} with the text; the text may be
     * empty. Cancel, Escape, and the close button report {@link DialogChoice#CANCEL} and do not
     * call {@code listener}. To react to a cancel, add a {@link ChoiceListener} to the returned
     * dialog.
     *
     * @param style looks of the dialog; must not be {@code null}
     * @param title text for the top border; must not be {@code null}
     * @param message text above the field, one label per line; must not be {@code null}
     * @param initialText text the field starts with; must not be {@code null}; may be empty
     * @param fieldWidthCells width of the text field in UI cells; 1 or more
     * @param listener called with the text after the dialog closed on OK; must not be
     *         {@code null}
     * @return a new dialog that is not shown
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code fieldWidthCells} is less than 1
     */
    public static Dialog textInput(DialogStyle style, String title, String message,
            String initialText, int fieldWidthCells, TextEnteredListener listener) {
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(initialText, "initialText");
        Objects.requireNonNull(listener, "listener");
        TextField field = new TextField(style.getTextField(), fieldWidthCells);
        field.setText(initialText);
        Table body = createMessage(style, message);
        body.row();
        body.add(field).padTop(GAP_ABOVE_FIELD);
        Dialog dialog = new Dialog(title, style, DialogChoice.CANCEL);
        dialog.setContent(body);
        dialog.addButton(OK_TEXT, DialogChoice.OK);
        dialog.addButton(CANCEL_TEXT, DialogChoice.CANCEL);
        dialog.setInitialFocus(field);
        field.addEnterListener(enteredField -> {
            dialog.choose(DialogChoice.OK);
            return true;
        });
        dialog.setFinalChoiceListener((answered, choice) -> {
            if (choice == DialogChoice.OK) {
                listener.entered(field.getText());
            }
        });
        return dialog;
    }

    private static Table createMessage(DialogStyle style, String message) {
        Objects.requireNonNull(message, "message");
        Table lines = new Table();
        String[] texts = message.split(LINE_BREAK_PATTERN, -1);
        for (int i = 0; i < texts.length; i++) {
            lines.add(new Label(texts[i], style.getMessage()));
            lines.row();
        }
        return lines;
    }
}
