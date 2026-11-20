package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.draw.BoxStyle;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DialogTest {

    private static final LabelStyle LOOK = LabelStyle.of(UiFont.SQUARE, 0xFFFFFFFF, 0x111111FF);
    private static final LabelStyle TALL_LOOK = LabelStyle.of(UiFont.TALL, 0xFFFFFFFF, 0x111111FF);
    private static final ButtonStyle BUTTON = ButtonStyle.of(LOOK, LOOK, LOOK, LOOK);
    private static final WindowStyle WINDOW =
            WindowStyle.of(BoxStyle.SINGLE, LOOK, LOOK, 0x333333FF);
    private static final DialogStyle STYLE =
            DialogStyle.of(WINDOW, LOOK, BUTTON, TextFieldStyle.of(LOOK));
    private static final int COLUMNS = 60;
    private static final int ROWS = 20;

    private final UiSurface surface = UiSurface.ofSize(COLUMNS, ROWS);
    private final UiRoot ui = UiRoot.of(surface);
    private final List<String> events = new ArrayList<>();
    private final TextButton rootButton = new TextButton("Go", BUTTON);

    DialogTest() {
        rootButton.setPosition(1, 14);
        ui.getRoot().addChild(rootButton);
    }

    private Dialog dialog(DialogChoice backOut) {
        Dialog dialog = new Dialog("Title", STYLE, backOut);
        dialog.setContent(new Label("Message", LOOK));
        return dialog;
    }

    private void record(Dialog dialog, DialogChoice choice) {
        events.add(choice + " showing=" + dialog.isShowing() + " modal=" + ui.isModalShown());
    }

    private void show(Dialog dialog) {
        dialog.show(ui);
        ui.draw();
    }

    private static int absoluteLeft(Widget widget) {
        int left = 0;
        for (Widget current = widget; current != null; current = current.getParent()) {
            left += current.getX();
        }
        return left;
    }

    private static int absoluteTop(Widget widget) {
        int top = 0;
        for (Widget current = widget; current != null; current = current.getParent()) {
            top += current.getY();
        }
        return top;
    }

    private void click(Widget widget) {
        ui.pointerDown(absoluteLeft(widget) + 1, absoluteTop(widget), 0, 0);
        ui.pointerUp(absoluteLeft(widget) + 1, absoluteTop(widget), 0, 0);
    }

    // Showing

    @Test
    void showCentersTheDialogAndModalsItWithFocusOnTheFirstButton() {
        Dialog dialog = dialog(DialogChoice.NO);
        TextButton yes = dialog.addButton("Yes", DialogChoice.YES);
        dialog.addButton("No", DialogChoice.NO);

        show(dialog);

        assertTrue(dialog.isShowing());
        assertSame(dialog, ui.getTopModal());
        assertSame(yes, ui.getKeyboardFocus());
        assertEquals((COLUMNS - dialog.getWidth()) / 2, dialog.getX());
        assertEquals((ROWS - dialog.getHeight()) / 2, dialog.getY());
    }

    @Test
    void theButtonsSitInOneRowBelowTheContent() {
        Dialog dialog = dialog(DialogChoice.NO);
        TextButton yes = dialog.addButton("Yes", DialogChoice.YES);
        TextButton no = dialog.addButton("No", DialogChoice.NO);

        show(dialog);

        assertEquals(absoluteTop(yes), absoluteTop(no));
        assertTrue(absoluteLeft(yes) < absoluteLeft(no));
        assertTrue(absoluteTop(yes) > absoluteTop(dialog.getContent()));
    }

    @Test
    void tallContentStartsOnAnEvenSurfaceRow() {
        Dialog dialog = new Dialog("T", STYLE, DialogChoice.OK);
        dialog.setContent(new Label("Tall", TALL_LOOK));
        dialog.addButton("OK", DialogChoice.OK);

        show(dialog);

        assertEquals(0, (dialog.getY() + Window.BORDER_THICKNESS) % 2);
        assertEquals(0, absoluteTop(dialog.getContent()) % 2);
    }

    @Test
    void showTwiceNullAndWithoutButtonsAreRejected() {
        Dialog dialog = dialog(DialogChoice.OK);
        dialog.addButton("OK", DialogChoice.OK);
        assertThrows(NullPointerException.class, () -> dialog.show(null));
        assertThrows(IllegalStateException.class, () -> dialog(DialogChoice.OK).show(ui));

        show(dialog);

        assertThrows(IllegalStateException.class, () -> dialog.show(ui));
    }

    @Test
    void anInitialFocusOutsideTheDialogFallsBackToTheFirstButton() {
        Dialog dialog = dialog(DialogChoice.OK);
        TextButton first = dialog.addButton("A", DialogChoice.OK);
        dialog.addButton("B", DialogChoice.CANCEL);
        dialog.setInitialFocus(rootButton);

        show(dialog);

        assertSame(first, ui.getKeyboardFocus());
    }

    @Test
    void aClosedDialogCanBeShownAgain() {
        Dialog dialog = dialog(DialogChoice.OK);
        dialog.addButton("OK", DialogChoice.OK);
        show(dialog);
        dialog.dismiss();

        show(dialog);

        assertTrue(dialog.isShowing());
        assertSame(dialog, ui.getTopModal());
    }

    @Test
    void aDialogRemovedByHandCanBeShownAgain() {
        Dialog dialog = dialog(DialogChoice.OK);
        dialog.addButton("OK", DialogChoice.OK);
        show(dialog);
        ui.getWindowLayer().removeChild(dialog);

        show(dialog);

        assertTrue(dialog.isShowing());
    }

    @Test
    void escapeReportsTheBackOutChoiceOnlyWhileCloseOnEscapeIsOn() {
        Dialog dialog = dialog(DialogChoice.CANCEL);
        dialog.addButton("OK", DialogChoice.OK);
        dialog.addChoiceListener(this::record);
        assertTrue(dialog.isCloseOnEscape());
        dialog.setCloseOnEscape(false);
        show(dialog);

        ui.keyDown(Input.Keys.ESCAPE);

        assertTrue(dialog.isShowing());
        assertTrue(events.isEmpty());
        assertFalse(dialog.isCloseOnEscape());

        dialog.setCloseOnEscape(true);
        ui.keyDown(Input.Keys.ESCAPE);

        assertEquals(List.of("CANCEL showing=false modal=false"), events);
    }

    @Test
    void chooseFromAChoiceListenerThrowsOnceTheDialogHasClosed() {
        Dialog dialog = dialog(DialogChoice.OK);
        dialog.addButton("OK", DialogChoice.OK);
        dialog.addChoiceListener((answered, choice) -> answered.choose(DialogChoice.OK));
        show(dialog);

        assertThrows(IllegalStateException.class, () -> dialog.choose(DialogChoice.OK));
    }

    @Test
    void anInitialFocusOverridesTheFirstButton() {
        Dialog dialog = dialog(DialogChoice.OK);
        dialog.addButton("A", DialogChoice.OK);
        TextButton second = dialog.addButton("B", DialogChoice.CANCEL);
        dialog.setInitialFocus(second);

        show(dialog);

        assertSame(second, ui.getKeyboardFocus());
    }

    // Choosing

    @Test
    void clickingAButtonClosesTheDialogBeforeTheListenersRun() {
        Dialog dialog = dialog(DialogChoice.NO);
        dialog.addButton("Yes", DialogChoice.YES);
        TextButton no = dialog.addButton("No", DialogChoice.NO);
        dialog.addChoiceListener(this::record);
        show(dialog);

        click(no);

        assertEquals(List.of("NO showing=false modal=false"), events);
        assertEquals(0, ui.getWindowLayer().getChildCount());
    }

    @Test
    void focusIsRestoredAfterTheChoice() {
        ui.setKeyboardFocus(rootButton);
        Dialog dialog = dialog(DialogChoice.OK);
        TextButton ok = dialog.addButton("OK", DialogChoice.OK);
        show(dialog);
        assertSame(ok, ui.getKeyboardFocus());

        click(ok);

        assertSame(rootButton, ui.getKeyboardFocus());
    }

    @Test
    void enterOnTheFocusedButtonChoosesIt() {
        Dialog dialog = dialog(DialogChoice.NO);
        dialog.addButton("Yes", DialogChoice.YES);
        dialog.addChoiceListener(this::record);
        show(dialog);

        ui.keyDown(Input.Keys.ENTER);

        assertEquals(List.of("YES showing=false modal=false"), events);
    }

    @Test
    void escapeReportsTheBackOutChoice() {
        Dialog dialog = dialog(DialogChoice.CANCEL);
        dialog.addButton("OK", DialogChoice.OK);
        dialog.addChoiceListener(this::record);
        show(dialog);

        assertTrue(ui.keyDown(Input.Keys.ESCAPE));

        assertEquals(List.of("CANCEL showing=false modal=false"), events);
    }

    @Test
    void closeWhileShowingReportsTheBackOutChoice() {
        Dialog dialog = dialog(DialogChoice.NO);
        dialog.addButton("Yes", DialogChoice.YES);
        dialog.addChoiceListener(this::record);
        show(dialog);

        dialog.close();

        assertEquals(List.of("NO showing=false modal=false"), events);
    }

    @Test
    void closeWhileNotShowingIsSilent() {
        Dialog dialog = dialog(DialogChoice.NO);
        dialog.addButton("Yes", DialogChoice.YES);
        dialog.addChoiceListener(this::record);

        dialog.close();

        assertTrue(events.isEmpty());
    }

    @Test
    void theWindowCloseButtonReportsTheBackOutChoice() {
        WindowStyle withClose = WINDOW.withCloseButton(BUTTON);
        DialogStyle style = DialogStyle.of(withClose, LOOK, BUTTON, TextFieldStyle.of(LOOK));
        Dialog dialog = new Dialog("T", style, DialogChoice.NO);
        dialog.setContent(new Label("M", LOOK));
        dialog.addButton("Yes", DialogChoice.YES);
        dialog.addChoiceListener(this::record);
        show(dialog);
        Widget closeButton = dialog.getChild(dialog.getChildCount() - 1);

        click(closeButton);

        assertEquals(List.of("NO showing=false modal=false"), events);
    }

    @Test
    void chooseWhileNotShowingIsRejected() {
        Dialog dialog = dialog(DialogChoice.NO);
        dialog.addButton("Yes", DialogChoice.YES);

        assertThrows(IllegalStateException.class, () -> dialog.choose(DialogChoice.YES));
        assertThrows(NullPointerException.class, () -> dialog.choose(null));
    }

    @Test
    void keepingTheDialogOpenLeavesItShownUntilDismissed() {
        Dialog dialog = dialog(DialogChoice.CANCEL);
        TextButton ok = dialog.addButton("OK", DialogChoice.OK);
        dialog.setCloseOnChoice(false);
        dialog.addChoiceListener(this::record);
        show(dialog);
        assertFalse(dialog.isCloseOnChoice());

        click(ok);
        assertTrue(dialog.isShowing());
        assertEquals(List.of("OK showing=true modal=true"), events);

        dialog.dismiss();
        assertFalse(dialog.isShowing());
        assertFalse(ui.isModalShown());
        assertEquals(1, events.size());
    }

    @Test
    void dismissIsSilentAndRestoresFocus() {
        ui.setKeyboardFocus(rootButton);
        Dialog dialog = dialog(DialogChoice.NO);
        dialog.addButton("Yes", DialogChoice.YES);
        dialog.addChoiceListener(this::record);
        show(dialog);

        dialog.dismiss();
        dialog.dismiss();

        assertTrue(events.isEmpty());
        assertSame(rootButton, ui.getKeyboardFocus());
    }

    @Test
    void aListenerCanShowTheDialogAgainAndAnotherOne() {
        Dialog first = dialog(DialogChoice.OK);
        TextButton ok = first.addButton("OK", DialogChoice.OK);
        Dialog second = dialog(DialogChoice.OK);
        second.addButton("OK", DialogChoice.OK);
        first.addChoiceListener((answered, choice) -> second.show(ui));
        show(first);

        click(ok);

        assertTrue(second.isShowing());
        assertFalse(first.isShowing());
        assertSame(second, ui.getTopModal());
        first.show(ui);
        assertSame(first, ui.getTopModal());
    }

    @Test
    void removedChoiceListenersAreNotCalled() {
        Dialog dialog = dialog(DialogChoice.OK);
        dialog.addButton("OK", DialogChoice.OK);
        ChoiceListener listener = this::record;
        dialog.addChoiceListener(listener);
        dialog.removeChoiceListener(listener);
        dialog.removeChoiceListener(listener);
        show(dialog);

        dialog.choose(DialogChoice.OK);

        assertTrue(events.isEmpty());
    }

    // Modal behavior

    @Test
    void nestedDialogsRestoreFocusInOrder() {
        Dialog lower = dialog(DialogChoice.OK);
        TextButton lowerButton = lower.addButton("OK", DialogChoice.OK);
        Dialog upper = dialog(DialogChoice.OK);
        upper.addButton("OK", DialogChoice.OK);
        show(lower);

        show(upper);
        assertSame(upper, ui.getTopModal());
        upper.choose(DialogChoice.OK);

        assertSame(lower, ui.getTopModal());
        assertSame(lowerButton, ui.getKeyboardFocus());
        lower.choose(DialogChoice.OK);
        assertFalse(ui.isModalShown());
    }

    @Test
    void aPressOutsideTheDialogIsSwallowed() {
        Dialog dialog = dialog(DialogChoice.OK);
        dialog.addButton("OK", DialogChoice.OK);
        show(dialog);

        assertTrue(ui.pointerDown(rootButton.getX() + 1, rootButton.getY(), 0, 0));

        assertFalse(rootButton.isPressed());
    }

    @Test
    void theStyleDimDarkensTheCellsUnderTheDialog() {
        DialogStyle dimming = STYLE.withDim(0x000000FF, 1f);
        Dialog dialog = new Dialog("T", dimming, DialogChoice.OK);
        dialog.addButton("OK", DialogChoice.OK);
        ui.draw();
        TerminalGrid grid = surface.getSquareGrid();
        assertEquals(0x111111FF, grid.getBackground(1, 14));

        show(dialog);

        assertEquals(0x000000FF, grid.getBackground(1, 14));
    }

    // Content area

    @Test
    void contentIsReplacedAndClearedWithoutTouchingTheButtons() {
        Dialog dialog = new Dialog("T", STYLE, DialogChoice.OK);
        TextButton ok = dialog.addButton("OK", DialogChoice.OK);
        assertFalse(dialog.hasContent());
        assertThrows(IllegalStateException.class, dialog::getContent);
        Label first = new Label("First", LOOK);
        Label second = new Label("Second", LOOK);

        dialog.setContent(first);
        dialog.setContent(second);

        assertTrue(dialog.hasContent());
        assertSame(second, dialog.getContent());
        assertNull(first.getParent());
        dialog.clearContent();
        assertFalse(dialog.hasContent());
        assertNull(second.getParent());
        assertNotNullParent(ok);
        dialog.clearContent();
    }

    private static void assertNotNullParent(Widget widget) {
        assertTrue(widget.getParent() != null);
    }

    @Test
    void rejectedContentLeavesTheOldContent() {
        Dialog dialog = dialog(DialogChoice.OK);
        Widget old = dialog.getContent();
        WidgetGroup owner = new WidgetGroup();
        Label owned = new Label("Owned", LOOK);
        owner.addChild(owned);

        assertThrows(NullPointerException.class, () -> dialog.setContent(null));
        assertThrows(IllegalArgumentException.class, () -> dialog.setContent(owned));
        assertThrows(IllegalArgumentException.class, () -> dialog.setContent(dialog));

        assertSame(old, dialog.getContent());
    }

    @Test
    void aCustomButtonDoesNothingByItself() {
        Dialog dialog = dialog(DialogChoice.CANCEL);
        TextButton custom = new TextButton("Help", BUTTON);
        custom.addClickListener(clicked -> events.add("help"));
        dialog.addCustomButton(custom);
        dialog.addChoiceListener(this::record);
        show(dialog);

        click(custom);

        assertEquals(List.of("help"), events);
        assertTrue(dialog.isShowing());
        assertThrows(NullPointerException.class, () -> dialog.addCustomButton(null));
        assertThrows(IllegalArgumentException.class, () -> dialog.addCustomButton(custom));
    }

    @Test
    void argumentsAreChecked() {
        assertThrows(NullPointerException.class,
                () -> new Dialog(null, STYLE, DialogChoice.OK));
        assertThrows(NullPointerException.class,
                () -> new Dialog("T", null, DialogChoice.OK));
        assertThrows(NullPointerException.class, () -> new Dialog("T", STYLE, null));
        Dialog dialog = dialog(DialogChoice.NO);
        assertThrows(NullPointerException.class, () -> dialog.addButton(null, DialogChoice.OK));
        assertThrows(NullPointerException.class, () -> dialog.addButton("A", null));
        assertThrows(NullPointerException.class, () -> dialog.addChoiceListener(null));
        assertSame(STYLE, dialog.getDialogStyle());
        assertEquals(DialogChoice.NO, dialog.getBackOutChoice());
    }
}
