package com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.Dialog;
import com.github.halfdeadgames.asciigdx.ui.DialogChoice;
import com.github.halfdeadgames.asciigdx.ui.DialogStyle;
import com.github.halfdeadgames.asciigdx.ui.Dialogs;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.ArrayList;
import java.util.List;

/** Every dialog style of the skin, with one button per ready-made dialog. */
final class DialogExample implements WidgetExample {
    private static final String[] STYLE_NAMES = {"dialog"};
    private static final int TEXT_INPUT_COLUMNS = 20;
    private static final String NO_ANSWER = "Last answer: none yet";

    private final List<Dialog> shownDialogs = new ArrayList<>();
    private UiRoot uiRoot;

    @Override
    public String id() {
        return "dialog";
    }

    @Override
    public String title() {
        return "Dialog";
    }

    @Override
    public Widget create(Skin skin, UiRoot uiRoot, BoundaryState boundaryState, int columns,
            int rows) {
        this.uiRoot = uiRoot;
        shownDialogs.clear();
        DemoSheet sheet = new DemoSheet(skin, boundaryState);
        Label answer = new Label(NO_ANSWER, skin.getLabelStyle("text"));
        sheet.addHeading("Live controls");
        sheet.addDemo("live: last answer", answer);
        for (String styleName : STYLE_NAMES) {
            addStyleDemos(sheet, skin, styleName, answer);
        }
        return sheet.build();
    }

    // The top dialog goes first, so each one hands focus back to the dialog below it.
    @Override
    public void exampleHidden() {
        for (int i = shownDialogs.size() - 1; i >= 0; i--) {
            shownDialogs.get(i).dismiss();
        }
        shownDialogs.clear();
    }

    private void addStyleDemos(DemoSheet sheet, Skin skin, String styleName, Label answer) {
        DialogStyle style = skin.getDialogStyle(styleName);
        sheet.addHeading(styleName);
        ButtonRow row = new ButtonRow(skin);
        row.add("ok", () -> show(Dialogs.ok(style, "Notice", "The file was saved.",
                (dialog, choice) -> report(answer, "ok", choice))));
        row.add("yesNo", () -> show(Dialogs.yesNo(style, "Quit", "Really quit?",
                (dialog, choice) -> report(answer, "yesNo", choice))));
        row.add("yesNoCancel", () -> show(Dialogs.yesNoCancel(style, "Save",
                "Save your changes first?",
                (dialog, choice) -> report(answer, "yesNoCancel", choice))));
        row.add("textInput", () -> show(createTextInput(style, answer)));
        sheet.addDemo(styleName + " (opens a dialog)", row.build());
    }

    private Dialog createTextInput(DialogStyle style, Label answer) {
        Dialog dialog = Dialogs.textInput(style, "Rename", "New name:", "untitled",
                TEXT_INPUT_COLUMNS, text -> showAnswer(answer, "textInput -> \"" + text + "\""));
        dialog.addChoiceListener((answered, choice) -> {
            if (choice == DialogChoice.CANCEL) {
                report(answer, "textInput", choice);
            }
        });
        return dialog;
    }

    private void show(Dialog dialog) {
        shownDialogs.removeIf(shown -> !shown.isShowing());
        shownDialogs.add(dialog);
        dialog.show(uiRoot);
    }

    private static void report(Label answer, String source, DialogChoice choice) {
        showAnswer(answer, source + " -> " + choice);
    }

    private static void showAnswer(Label answer, String text) {
        answer.setText("Last answer: " + text);
        answer.pack();
    }
}
