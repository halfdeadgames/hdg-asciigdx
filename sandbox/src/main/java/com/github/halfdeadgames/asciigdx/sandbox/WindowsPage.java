package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.ui.Dialog;
import com.github.halfdeadgames.asciigdx.ui.DialogChoice;
import com.github.halfdeadgames.asciigdx.ui.DialogStyle;
import com.github.halfdeadgames.asciigdx.ui.Dialogs;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.TextButton;
import com.github.halfdeadgames.asciigdx.ui.TextField;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;
import com.github.halfdeadgames.asciigdx.ui.Window;
import com.github.halfdeadgames.asciigdx.ui.WindowStyle;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive check of windows in the root's window layer: a window with a close button that holds
 * buttons and a text field, a window without a close button, and a window that also closes on
 * Escape. Drag a window by its title bar, click one to raise it, press Tab to see that focus stays
 * inside the window, and close windows to bring them back with "Reopen windows". The windows are
 * removed from the layer when another page is shown.
 *
 * <p>A row of buttons opens modal dialogs: OK, Yes/No, Yes/No/Cancel, text input, and a nested
 * one (its Yes opens an OK dialog on top while it stays open). Everything under a dialog is dimmed
 * by the skin's dialog style. The last answer shows in a label. Open dialogs are dismissed when
 * another page is shown.
 */
class WindowsPage implements UiPage {
    private static final int HINT_X = 4;
    private static final int HINT_Y = 1;
    private static final int REOPEN_BUTTON_Y = 4;
    private static final int SETTINGS_X = 6;
    private static final int WINDOW_GAP_COLUMNS = 2;
    private static final int WINDOW_GAP_ROWS = 1;
    private static final int NAME_FIELD_WIDTH = 18;
    private static final int BUTTON_GAP = 1;
    private static final int FORM_ROW_STEP = 2;
    private static final int DIALOG_BUTTON_Y = 7;
    private static final int RESULT_Y = 9;
    private static final int TEXT_INPUT_WIDTH = 20;
    private static final String DIALOG_STYLE_NAME = "dialog";
    private static final String NO_ANSWER_TEXT = "Last dialog answer: none yet";

    private final UiRoot uiRoot;
    private final Skin skin;
    private final List<Window> windows = new ArrayList<>();
    private final List<Dialog> shownDialogs = new ArrayList<>();
    private Label resultLabel;
    private int contentColumns;
    private int contentTop;

    WindowsPage(UiRoot uiRoot, Skin skin) {
        this.uiRoot = uiRoot;
        this.skin = skin;
    }

    @Override
    public String id() {
        return "windows";
    }

    @Override
    public String title() {
        return "Windows";
    }

    @Override
    public Widget createContent(int columns, int rows) {
        removeWindowsFromLayer();
        windows.clear();
        windows.add(createSettingsWindow());
        windows.add(createNotesWindow());
        windows.add(createEscapeWindow());
        contentColumns = columns;
        openClosedWindows();
        return createPageContent();
    }

    @Override
    public void contentPlaced(int contentTopRow) {
        contentTop = contentTopRow;
        placeWindows();
    }

    @Override
    public void pageHidden() {
        dismissDialogs();
        removeWindowsFromLayer();
    }

    /** Opens the dimmed OK dialog that the capture variant photographs. */
    void showCaptureDialog() {
        show(Dialogs.ok(dialogStyle(), "Notice", "The file was saved.\nNothing else happened.",
                (dialog, choice) -> report("OK dialog", choice)));
    }

    private DialogStyle dialogStyle() {
        return skin.getDialogStyle(DIALOG_STYLE_NAME);
    }

    private void show(Dialog dialog) {
        shownDialogs.removeIf(shown -> !shown.isShowing());
        shownDialogs.add(dialog);
        dialog.show(uiRoot);
    }

    // The top dialog goes first, so each one hands focus back to the dialog below it.
    void dismissDialogs() {
        for (int i = shownDialogs.size() - 1; i >= 0; i--) {
            shownDialogs.get(i).dismiss();
        }
        shownDialogs.clear();
    }

    private void report(String source, DialogChoice choice) {
        showResult("Last dialog answer: " + source + " -> " + choice);
    }

    private void showResult(String text) {
        resultLabel.setText(text);
        resultLabel.pack();
    }

    private void openYesNoDialog() {
        show(Dialogs.yesNo(dialogStyle(), "Quit", "Really quit?\nEsc answers No.",
                (dialog, choice) -> report("Yes/No dialog", choice)));
    }

    private void openYesNoCancelDialog() {
        show(Dialogs.yesNoCancel(dialogStyle(), "Save", "Save your changes first?",
                (dialog, choice) -> report("Yes/No/Cancel dialog", choice)));
    }

    private void openTextInputDialog() {
        Dialog dialog = Dialogs.textInput(dialogStyle(), "Rename", "New name:", "untitled",
                TEXT_INPUT_WIDTH, text -> showResult(
                        "Last dialog answer: text input -> \"" + text + "\""));
        dialog.addChoiceListener((answered, choice) -> {
            if (choice == DialogChoice.CANCEL) {
                report("text input", choice);
            }
        });
        show(dialog);
    }

    // The outer dialog stays open while the inner OK dialog is on top of it.
    private void openNestedDialog() {
        Dialog outer = Dialogs.yesNo(dialogStyle(), "Delete", "Delete the file?",
                (dialog, choice) -> {
                    if (choice == DialogChoice.YES) {
                        openDeletedDialog(dialog);
                        return;
                    }
                    report("nested dialog", choice);
                    dialog.dismiss();
                });
        outer.setCloseOnChoice(false);
        show(outer);
    }

    private void openDeletedDialog(Dialog outer) {
        show(Dialogs.ok(dialogStyle(), "Deleted", "The file is gone.\nThis dialog is on top.",
                (dialog, choice) -> {
                    report("nested dialog", DialogChoice.YES);
                    outer.dismiss();
                }));
    }

    private Widget createPageContent() {
        WidgetGroup content = new WidgetGroup();
        Label hint = new Label("Drag a title bar to move a window. Click a window to raise it. "
                + "Tab stays inside the focused window.", skin.getLabelStyle("text"));
        hint.setPosition(HINT_X, HINT_Y);
        content.addChild(hint);
        TextButton reopen = new TextButton("Reopen windows", skin.getButtonStyle("button"));
        reopen.setPosition(HINT_X, REOPEN_BUTTON_Y);
        reopen.addClickListener(clicked -> reopenWindows());
        content.addChild(reopen);
        addDialogButtons(content);
        resultLabel = new Label(NO_ANSWER_TEXT, skin.getLabelStyle("text"));
        resultLabel.setPosition(HINT_X, RESULT_Y);
        content.addChild(resultLabel);
        return content;
    }

    private void addDialogButtons(WidgetGroup content) {
        int x = HINT_X;
        x = addDialogButton(content, "OK dialog", x, this::showCaptureDialog);
        x = addDialogButton(content, "Yes/No", x, this::openYesNoDialog);
        x = addDialogButton(content, "Yes/No/Cancel", x, this::openYesNoCancelDialog);
        x = addDialogButton(content, "Text input", x, this::openTextInputDialog);
        addDialogButton(content, "Nested", x, this::openNestedDialog);
    }

    private int addDialogButton(WidgetGroup content, String text, int x, Runnable action) {
        TextButton button = new TextButton(text, skin.getButtonStyle("button"));
        button.setPosition(x, DIALOG_BUTTON_Y);
        button.addClickListener(clicked -> action.run());
        content.addChild(button);
        return x + button.getWidth() + BUTTON_GAP;
    }

    private Window createSettingsWindow() {
        Window window = new Window("Settings", skin.getWindowStyle("window"));
        WidgetGroup form = new WidgetGroup();
        TextButton apply = new TextButton("Apply", skin.getButtonStyle("button"));
        form.addChild(apply);
        TextButton reset = new TextButton("Reset", skin.getButtonStyle("button"));
        reset.setPosition(apply.getWidth() + BUTTON_GAP, 0);
        form.addChild(reset);
        TextField name = new TextField(skin.getTextFieldStyle("default"), NAME_FIELD_WIDTH);
        name.setPlaceholderText("Name");
        name.setPosition(0, FORM_ROW_STEP);
        form.addChild(name);
        reset.addClickListener(clicked -> name.setText(""));
        TextButton cancel = new TextButton("Cancel", skin.getButtonStyle("button"));
        cancel.setPosition(0, 2 * FORM_ROW_STEP);
        cancel.addClickListener(clicked -> window.close());
        form.addChild(cancel);
        return withContent(window, form);
    }

    private Window createNotesWindow() {
        Window window = new Window("Notes", withoutCloseButton(skin.getWindowStyle("window")));
        Label text = new Label("No close button here. Move me or raise me.",
                skin.getLabelStyle("windowText"));
        return withContent(window, text);
    }

    private Window createEscapeWindow() {
        Window window = new Window("Esc closes", skin.getWindowStyle("window"));
        window.setCloseOnEscape(true);
        WidgetGroup body = new WidgetGroup();
        Label text = new Label("Focus the button, then press Esc.",
                skin.getLabelStyle("windowText"));
        body.addChild(text);
        TextButton done = new TextButton("Done", skin.getButtonStyle("button"));
        done.setPosition(0, FORM_ROW_STEP);
        done.addClickListener(clicked -> window.close());
        body.addChild(done);
        return withContent(window, body);
    }

    private static Window withContent(Window window, Widget content) {
        window.setContent(content);
        window.pack();
        return window;
    }

    // Windows go left to right in list order. A window that would pass the right edge starts a new
    // row below every window placed so far, so the rows never overlap.
    private void placeWindows() {
        int firstRowTop = contentTop + RESULT_Y + resultLabel.getHeight() + WINDOW_GAP_ROWS;
        int x = SETTINGS_X;
        int y = firstRowTop;
        int bottom = firstRowTop;
        for (Window window : windows) {
            if (x > SETTINGS_X && x + window.getWidth() > contentColumns) {
                x = SETTINGS_X;
                y = bottom + WINDOW_GAP_ROWS;
            }
            window.setPosition(x, y);
            bottom = Math.max(bottom, y + window.getHeight());
            x += window.getWidth() + WINDOW_GAP_COLUMNS;
        }
    }

    private void reopenWindows() {
        placeWindows();
        openClosedWindows();
    }

    private static WindowStyle withoutCloseButton(WindowStyle style) {
        return WindowStyle.of(style.getBox(), style.getBorder(), style.getTitle(),
                style.getBackgroundRgba()).withFocusedBorder(style.getFocusedBorder());
    }

    private void openClosedWindows() {
        for (Window window : windows) {
            if (window.getParent() == null) {
                uiRoot.getWindowLayer().addChild(window);
            }
        }
    }

    private void removeWindowsFromLayer() {
        for (Window window : windows) {
            if (window.getParent() != null) {
                uiRoot.getWindowLayer().removeChild(window);
            }
        }
    }
}
