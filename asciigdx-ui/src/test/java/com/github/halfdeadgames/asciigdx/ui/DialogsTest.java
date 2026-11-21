package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.draw.BoxStyle;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DialogsTest {

    private static final LabelStyle LOOK = LabelStyle.of(0xFFFFFFFF, 0x111111FF);
    private static final ButtonStyle BUTTON = ButtonStyle.of(LOOK, LOOK, LOOK, LOOK);
    private static final WindowStyle WINDOW =
            WindowStyle.of(BoxStyle.SINGLE, LOOK, LOOK, 0x333333FF);
    private static final DialogStyle STYLE =
            DialogStyle.of(WINDOW, LOOK, BUTTON, TextFieldStyle.of(LOOK));

    private final UiSurface surface = UiSurface.ofSize(60, 20);
    private final UiRoot ui = UiRoot.of(surface);
    private final List<String> events = new ArrayList<>();

    private void show(Dialog dialog) {
        dialog.show(ui);
        ui.draw();
    }

    private static void collect(Widget widget, List<Widget> found) {
        found.add(widget);
        if (widget instanceof WidgetGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                collect(group.getChild(i), found);
            }
        }
    }

    private static List<Widget> descendants(Dialog dialog) {
        List<Widget> found = new ArrayList<>();
        collect(dialog, found);
        return found;
    }

    private static TextButton buttonNamed(Dialog dialog, String text) {
        for (Widget widget : descendants(dialog)) {
            if (widget instanceof TextButton button && button.getText().equals(text)) {
                return button;
            }
        }
        throw new AssertionError("No button " + text);
    }

    private static List<String> buttonTexts(Dialog dialog) {
        List<String> texts = new ArrayList<>();
        for (Widget widget : descendants(dialog)) {
            if (widget instanceof TextButton button) {
                texts.add(button.getText());
            }
        }
        return texts;
    }

    private static List<String> labelTexts(Dialog dialog) {
        List<String> texts = new ArrayList<>();
        for (Widget widget : descendants(dialog)) {
            if (widget instanceof Label label) {
                texts.add(label.getText());
            }
        }
        return texts;
    }

    private static TextField fieldOf(Dialog dialog) {
        for (Widget widget : descendants(dialog)) {
            if (widget instanceof TextField field) {
                return field;
            }
        }
        throw new AssertionError("No text field");
    }

    private void click(Widget widget) {
        int left = 0;
        int top = 0;
        for (Widget current = widget; current != null; current = current.getParent()) {
            left += current.getX();
            top += current.getY();
        }
        ui.pointerDown(left + 1, top, 0, 0);
        ui.pointerUp(left + 1, top, 0, 0);
    }

    private ChoiceListener recorder() {
        return (dialog, choice) -> events.add(choice.name());
    }

    // Information and question dialogs

    @Test
    void okHasOneButtonAndEscapeAnswersOk() {
        Dialog dialog = Dialogs.ok(STYLE, "Info", "Saved.", recorder());
        show(dialog);

        assertEquals(List.of("OK"), buttonTexts(dialog));
        assertEquals(DialogChoice.OK, dialog.getBackOutChoice());
        ui.keyDown(Input.Keys.ESCAPE);

        assertEquals(List.of("OK"), events);
        assertFalse(dialog.isShowing());
    }

    @Test
    void okWithoutAListenerStillWorks() {
        Dialog dialog = Dialogs.ok(STYLE, "Info", "Saved.");
        show(dialog);

        click(buttonNamed(dialog, "OK"));

        assertFalse(dialog.isShowing());
    }

    @Test
    void yesNoHasTwoButtonsAndEscapeAnswersNo() {
        Dialog dialog = Dialogs.yesNo(STYLE, "Quit", "Sure?", recorder());
        show(dialog);

        assertEquals(List.of("Yes", "No"), buttonTexts(dialog));
        click(buttonNamed(dialog, "Yes"));
        assertEquals(List.of("YES"), events);

        show(dialog);
        ui.keyDown(Input.Keys.ESCAPE);
        assertEquals(List.of("YES", "NO"), events);
    }

    @Test
    void yesNoCancelHasThreeButtonsAndEscapeAnswersCancel() {
        Dialog dialog = Dialogs.yesNoCancel(STYLE, "Save", "Save first?", recorder());
        show(dialog);

        assertEquals(List.of("Yes", "No", "Cancel"), buttonTexts(dialog));
        click(buttonNamed(dialog, "No"));
        show(dialog);
        ui.keyDown(Input.Keys.ESCAPE);

        assertEquals(List.of("NO", "CANCEL"), events);
    }

    @Test
    void theMessageBecomesOneLabelPerLine() {
        Dialog dialog = Dialogs.ok(STYLE, "T", "one\ntwo\r\nthree");

        assertEquals(List.of("one", "two", "three"), labelTexts(dialog));
    }

    @Test
    void theDialogIsAsWideAsTheLongestLine() {
        Dialog shortLines = Dialogs.ok(STYLE, "T", "a\nb");
        Dialog longLine = Dialogs.ok(STYLE, "T", "a\nthis line is clearly longer than a button");
        show(shortLines);
        shortLines.dismiss();

        show(longLine);

        assertTrue(longLine.getWidth() > shortLines.getWidth());
        String longestLine = "this line is clearly longer than a button";
        int textCells = (longestLine.length() + 1) / 2;
        assertTrue(longLine.getWidth() >= textCells);
        assertTrue(longLine.getWidth() < longestLine.length());
    }

    // Text input

    @Test
    void textInputStartsWithTheInitialTextAndFocusInTheField() {
        Dialog dialog = Dialogs.textInput(STYLE, "Name", "Your name?", "Ann", 12,
                text -> events.add(text));
        show(dialog);

        TextField field = fieldOf(dialog);
        assertEquals("Ann", field.getText());
        assertSame(field, ui.getKeyboardFocus());
        assertEquals(List.of("OK", "Cancel"), buttonTexts(dialog));
        assertEquals(DialogChoice.CANCEL, dialog.getBackOutChoice());
    }

    @Test
    void okReportsTheChoiceThenTheText() {
        Dialog dialog = Dialogs.textInput(STYLE, "Name", "Your name?", "Ann", 12,
                text -> events.add("entered " + text));
        dialog.addChoiceListener(recorder());
        show(dialog);
        ui.keyTyped('!');

        click(buttonNamed(dialog, "OK"));

        assertEquals(List.of("OK", "entered Ann!"), events);
        assertFalse(dialog.isShowing());
    }

    @Test
    void enterInTheFieldActsLikeOk() {
        Dialog dialog = Dialogs.textInput(STYLE, "Name", "Your name?", "", 12,
                text -> events.add("entered [" + text + "]"));
        show(dialog);

        assertTrue(ui.keyDown(Input.Keys.ENTER));

        assertEquals(List.of("entered []"), events);
        assertFalse(dialog.isShowing());
    }

    @Test
    void cancelEscapeAndTheCloseCallNeverReportTheText() {
        Dialog dialog = Dialogs.textInput(STYLE, "Name", "Your name?", "Ann", 12,
                text -> events.add("entered " + text));
        dialog.addChoiceListener(recorder());

        show(dialog);
        click(buttonNamed(dialog, "Cancel"));
        show(dialog);
        ui.keyDown(Input.Keys.ESCAPE);
        show(dialog);
        dialog.close();

        assertEquals(List.of("CANCEL", "CANCEL", "CANCEL"), events);
    }

    // Arguments

    @Test
    void argumentsAreChecked() {
        assertThrows(NullPointerException.class, () -> Dialogs.ok(null, "T", "M"));
        assertThrows(NullPointerException.class, () -> Dialogs.ok(STYLE, null, "M"));
        assertThrows(NullPointerException.class, () -> Dialogs.ok(STYLE, "T", null));
        assertThrows(NullPointerException.class, () -> Dialogs.ok(STYLE, "T", "M", null));
        assertThrows(NullPointerException.class,
                () -> Dialogs.yesNo(STYLE, "T", "M", null));
        assertThrows(NullPointerException.class,
                () -> Dialogs.yesNoCancel(null, "T", "M", recorder()));
        assertThrows(NullPointerException.class,
                () -> Dialogs.textInput(STYLE, "T", "M", null, 5, text -> { }));
        assertThrows(NullPointerException.class,
                () -> Dialogs.textInput(STYLE, "T", "M", "", 5, null));
        assertThrows(IllegalArgumentException.class,
                () -> Dialogs.textInput(STYLE, "T", "M", "", 0, text -> { }));
    }

    @Test
    void theReturnedDialogIsNotShownYet() {
        Dialog dialog = Dialogs.ok(STYLE, "T", "M");

        assertFalse(dialog.isShowing());
        assertFalse(ui.isModalShown());
        assertNull(dialog.getParent());
    }
}
