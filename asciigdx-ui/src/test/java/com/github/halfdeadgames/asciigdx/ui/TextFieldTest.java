package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TextFieldTest {
    private static final int TEXT = 0xFFFFFFFF;
    private static final int NORMAL_BG = 0x111111FF;
    private static final int FOCUSED_BG = 0x222222FF;
    private static final int DISABLED_BG = 0x333333FF;
    private static final int PLACEHOLDER_FG = 0x777777FF;
    private static final int CARET_FG = 0x010101FF;
    private static final int CARET_BG = 0xEEEEEEFF;
    private static final float BLINK = 0.5f;
    private static final char BACKSPACE_CHARACTER = '\b';
    private static final char DELETE_CHARACTER = 127;
    private static final CaretStyle BLOCK = CaretStyle.none().withForegroundRgba(CARET_FG)
            .withBackgroundRgba(CARET_BG).withBlinkSeconds(BLINK);
    private static final TextFieldStyle STYLE = TextFieldStyle.of(
            LabelStyle.of(UiFont.SQUARE, TEXT, NORMAL_BG))
            .withFocused(LabelStyle.of(UiFont.SQUARE, TEXT, FOCUSED_BG))
            .withDisabled(LabelStyle.of(UiFont.SQUARE, TEXT, DISABLED_BG))
            .withPlaceholder(LabelStyle.of(UiFont.SQUARE, PLACEHOLDER_FG, NORMAL_BG))
            .withCaret(BLOCK);

    private final UiSurface surface = UiSurface.ofSize(40, 10);
    private final UiRoot ui = UiRoot.of(surface);
    private final List<String> log = new ArrayList<>();

    private TextField fieldAt(int x, int y, int width) {
        TextField field = new TextField(STYLE, width);
        field.setPosition(x, y);
        ui.getRoot().addChild(field);
        return field;
    }

    private TextField focusedField() {
        TextField field = fieldAt(2, 1, 10);
        ui.setKeyboardFocus(field);
        return field;
    }

    private void type(String text) {
        for (int i = 0; i < text.length(); i++) {
            ui.keyTyped(text.charAt(i));
        }
    }

    private TerminalGrid grid() {
        ui.draw();
        return surface.getSquareGrid();
    }

    @Test
    void sizeIsGivenWidthByFontHeight() {
        TextField field = new TextField(STYLE, 12);

        assertEquals(12, field.getWidth());
        assertEquals(1, field.getHeight());
        assertThrows(IllegalArgumentException.class, () -> new TextField(STYLE, 0));
    }

    @Test
    void typingInsertsAndReportsChanges() {
        TextField field = focusedField();
        field.addChangeListener(source -> log.add(source.getText()));

        assertTrue(ui.keyTyped('h'));
        assertTrue(ui.keyTyped('i'));

        assertEquals("hi", field.getText());
        assertEquals(List.of("h", "hi"), log);
    }

    @Test
    void setTextMovesCaretToEndWithoutEvents() {
        TextField field = focusedField();
        field.addChangeListener(source -> log.add("changed"));

        field.setText("hello");

        assertEquals("hello", field.getText());
        assertEquals(5, field.getCaretIndex());
        assertTrue(log.isEmpty());
    }

    @Test
    void controlCharactersAreNotInsertedNorConsumed() {
        TextField field = focusedField();

        field.addChangeListener(source -> log.add("changed"));

        assertFalse(ui.keyTyped('\t'));
        assertFalse(ui.keyTyped('\r'));
        assertFalse(ui.keyTyped('\n'));

        assertEquals("", field.getText());
        assertTrue(log.isEmpty());
    }

    @Test
    void refusedCharactersAreConsumedButNotInserted() {
        TextField field = focusedField();
        field.setFilter(TextFieldFilter.digits());
        field.addChangeListener(source -> log.add("changed"));

        assertTrue(ui.keyTyped('x'));
        assertTrue(ui.keyTyped('7'));

        assertEquals("7", field.getText());
        assertEquals(1, log.size());
    }

    @Test
    void decimalFilterAllowsOnePeriod() {
        TextField field = focusedField();
        field.setFilter(TextFieldFilter.decimals());

        type("1.5.2-3");

        assertEquals("1.523", field.getText());
    }

    @Test
    void maxLengthStopsTypingButNotSetText() {
        TextField field = focusedField();
        field.setMaxLength(3);

        type("abcdef");
        assertEquals("abc", field.getText());
        field.setText("abcdefg");
        assertEquals("abcdefg", field.getText());
        assertEquals(3, field.getMaxLength());
        assertThrows(IllegalArgumentException.class, () -> field.setMaxLength(-1));
    }

    @Test
    void backspaceAndDeleteEditAndReport() {
        TextField field = focusedField();
        field.addChangeListener(source -> log.add(source.getText()));
        field.setText("abc");

        assertTrue(ui.keyTyped(BACKSPACE_CHARACTER));
        ui.keyDown(Input.Keys.HOME);
        assertTrue(ui.keyTyped(DELETE_CHARACTER));
        assertTrue(ui.keyTyped(BACKSPACE_CHARACTER));
        assertTrue(ui.keyTyped(DELETE_CHARACTER));

        assertEquals("", field.getText());
        assertEquals(List.of("ab", "b", ""), log);
    }

    @Test
    void backspaceAndDeleteKeyDownsAreConsumedWithoutEditing() {
        TextField field = focusedField();
        field.setText("abc");
        field.addChangeListener(source -> log.add("changed"));

        assertTrue(ui.keyDown(Input.Keys.BACKSPACE));
        ui.keyDown(Input.Keys.HOME);
        assertTrue(ui.keyDown(Input.Keys.FORWARD_DEL));

        assertEquals("abc", field.getText());
        assertTrue(log.isEmpty());
    }

    @Test
    void setStyleBringsTheCaretBackOn() {
        TextField field = fieldAt(2, 1, 10);
        ui.setKeyboardFocus(field);
        ui.update(BLINK);
        assertEquals(NORMAL_BG, grid().getBackground(2, 1));

        field.setStyle(TextFieldStyle.of(LabelStyle.of(UiFont.SQUARE, TEXT, FOCUSED_BG))
                .withCaret(BLOCK));

        assertEquals(CARET_BG, grid().getBackground(2, 1));
    }

    @Test
    void pressOnTheFocusedFieldAsksForTheKeyboardAgain() {
        List<Boolean> wanted = new ArrayList<>();
        ui.setTextInputWantedListener(wanted::add);
        TextField field = fieldAt(2, 1, 10);

        ui.pointerDown(3, 1, 0, 0);
        ui.pointerUp(3, 1, 0, 0);
        ui.pointerDown(4, 1, 0, 0);
        ui.pointerUp(4, 1, 0, 0);

        assertEquals(List.of(true, true), wanted);
        assertSame(field, ui.getKeyboardFocus());
    }

    @Test
    void disablingTheFocusedFieldClearsFocusAndStopsWantingTheKeyboard() {
        List<Boolean> wanted = new ArrayList<>();
        ui.setTextInputWantedListener(wanted::add);
        TextField field = focusedField();

        field.setDisabled(true);

        assertEquals(null, ui.getKeyboardFocus());
        assertEquals(List.of(true, false), wanted);
        field.setDisabled(false);
        assertEquals(null, ui.getKeyboardFocus());
    }

    @Test
    void disablingTheFocusedButtonClearsFocus() {
        TextButton button = new TextButton("Go", ButtonStyle.of(
                LabelStyle.of(UiFont.SQUARE, TEXT, NORMAL_BG)));
        ui.getRoot().addChild(button);
        ui.setKeyboardFocus(button);
        assertSame(button, ui.getKeyboardFocus());

        button.setDisabled(true);

        assertEquals(null, ui.getKeyboardFocus());
        assertFalse(button.hasKeyboardFocus());
    }

    @Test
    void nonFocusableWidgetKeepsFocusSetByCode() {
        Widget plain = new WidgetForFocusTest();
        ui.getRoot().addChild(plain);

        ui.setKeyboardFocus(plain);

        assertSame(plain, ui.getKeyboardFocus());
    }

    private static final class WidgetForFocusTest extends Widget {
        @Override
        public int getPreferredWidth() {
            return 1;
        }

        @Override
        public int getPreferredHeight() {
            return 1;
        }

        @Override
        protected void drawContent(UiSurface target, int left, int top) {
        }
    }

    @Test
    void filterSeesTheTypedCodePoint() {
        TextField field = focusedField();
        field.setFilter((source, codePoint) -> Character.isLetter(codePoint));

        type("a1b");

        assertEquals("ab", field.getText());
    }

    @Test
    void caretKeysMoveAndEdgesAreConsumed() {
        TextField field = focusedField();
        field.setText("abc");

        assertTrue(ui.keyDown(Input.Keys.RIGHT));
        assertEquals(3, field.getCaretIndex());
        assertTrue(ui.keyDown(Input.Keys.LEFT));
        assertEquals(2, field.getCaretIndex());
        assertTrue(ui.keyDown(Input.Keys.HOME));
        assertTrue(ui.keyDown(Input.Keys.LEFT));
        assertEquals(0, field.getCaretIndex());
        assertTrue(ui.keyDown(Input.Keys.END));
        assertEquals(3, field.getCaretIndex());
    }

    @Test
    void upAndDownAreConsumedAndKeepFocus() {
        TextField field = focusedField();
        TextField below = fieldAt(2, 5, 10);

        assertTrue(ui.keyDown(Input.Keys.DOWN));
        assertTrue(ui.keyDown(Input.Keys.UP));

        assertSame(field, ui.getKeyboardFocus());
        assertFalse(below.hasKeyboardFocus());
    }

    @Test
    void tabIsNotConsumedSoFocusMovesOn() {
        TextField first = focusedField();
        TextField second = fieldAt(2, 5, 10);

        ui.keyDown(Input.Keys.TAB);

        assertSame(second, ui.getKeyboardFocus());
        assertFalse(first.hasKeyboardFocus());
    }

    @Test
    void enterListenersStopAtTheFirstThatHandlesIt() {
        TextField field = focusedField();
        field.addEnterListener(source -> {
            log.add("first");
            return false;
        });
        field.addEnterListener(source -> {
            log.add("second");
            return true;
        });
        field.addEnterListener(source -> {
            log.add("third");
            return true;
        });

        assertTrue(ui.keyDown(Input.Keys.ENTER));

        assertEquals(List.of("first", "second"), log);
    }

    @Test
    void enterFallsThroughWhenNoListenerHandlesIt() {
        TextField field = focusedField();
        assertFalse(ui.keyDown(Input.Keys.ENTER));

        EnterListener declining = source -> false;
        field.addEnterListener(declining);
        assertFalse(ui.keyDown(Input.Keys.NUMPAD_ENTER));

        field.removeEnterListener(declining);
        field.addEnterListener(source -> true);
        assertTrue(ui.keyDown(Input.Keys.NUMPAD_ENTER));
    }

    @Test
    void enterReachesTheUnhandledKeyPathWhenNotHandled() {
        focusedField();
        ui.addUnhandledKeyListener(event -> {
            log.add("unhandled " + event.getKeycode());
            return true;
        });

        assertTrue(ui.keyDown(Input.Keys.ENTER));

        assertEquals(List.of("unhandled " + Input.Keys.ENTER), log);
    }

    @Test
    void pressFocusesAndPlacesCaretAtClickedCell() {
        TextField field = fieldAt(2, 1, 10);
        field.setText("hello");

        assertTrue(ui.pointerDown(2 + 3, 1, 0, 0));
        ui.pointerUp(2 + 3, 1, 0, 0);

        assertSame(field, ui.getKeyboardFocus());
        assertEquals(3, field.getCaretIndex());

        ui.pointerDown(2 + 9, 1, 0, 0);
        ui.pointerUp(2 + 9, 1, 0, 0);
        assertEquals(5, field.getCaretIndex());
    }

    @Test
    void pressOnScrolledFieldCountsFromTheWindowStart() {
        TextField field = fieldAt(0, 0, 8);
        field.setText("abcdefghijklmnopqrst");
        grid();

        ui.pointerDown(1, 0, 0, 0);
        ui.pointerUp(1, 0, 0, 0);

        assertEquals(14, field.getCaretIndex());
    }

    @Test
    void disabledFieldIgnoresInputAndIsNotFocusable() {
        TextField field = fieldAt(2, 1, 10);
        field.setDisabled(true);

        ui.pointerDown(3, 1, 0, 0);
        ui.setKeyboardFocus(field);

        assertFalse(field.isFocusable());
        assertFalse(ui.keyTyped('a'));
        assertFalse(ui.keyDown(Input.Keys.BACKSPACE));
        assertEquals("", field.getText());
        assertEquals(DISABLED_BG, grid().getBackground(3, 1));
    }

    @Test
    void drawsTextOnFieldBackground() {
        TextField field = fieldAt(2, 1, 10);
        field.setText("hi");

        TerminalGrid grid = grid();

        assertEquals('h', grid.getCodePoint(2, 1));
        assertEquals('i', grid.getCodePoint(3, 1));
        assertEquals(TEXT, grid.getForeground(2, 1));
        assertEquals(NORMAL_BG, grid.getBackground(11, 1));
    }

    @Test
    void drawsEllipsisForCutText() {
        TextField field = fieldAt(0, 0, 8);
        field.setText("abcdefghijklmnopqrst");
        field.setStyle(STYLE);

        TerminalGrid grid = grid();

        assertEquals(0x2026, grid.getCodePoint(0, 0));
        assertEquals('o', grid.getCodePoint(1, 0));
    }

    @Test
    void caretShowsOnlyWhileFocusedAndSwapsCellColors() {
        TextField field = fieldAt(2, 1, 10);
        field.setText("hi");

        assertEquals(NORMAL_BG, grid().getBackground(4, 1));
        ui.setKeyboardFocus(field);
        TerminalGrid grid = grid();
        assertEquals(CARET_BG, grid.getBackground(4, 1));
        assertEquals(NORMAL_BG, grid.getBackground(3, 1));

        ui.keyDown(Input.Keys.HOME);
        grid = grid();
        assertEquals(CARET_BG, grid.getBackground(2, 1));
        assertEquals(CARET_FG, grid.getForeground(2, 1));
        assertEquals('h', grid.getCodePoint(2, 1));
        ui.setKeyboardFocus(null);
        assertEquals(NORMAL_BG, grid().getBackground(2, 1));
    }

    @Test
    void caretShowsWithoutVisibleFocusLook() {
        TextField field = fieldAt(2, 1, 10);
        ui.setKeyboardFocus(field);

        TerminalGrid grid = grid();

        assertFalse(ui.isFocusVisible());
        assertEquals(CARET_BG, grid.getBackground(2, 1));
        assertEquals(NORMAL_BG, grid.getBackground(3, 1));
    }

    @Test
    void focusedLookShowsAfterKeyboardNavigation() {
        TextField field = fieldAt(2, 1, 10);
        ui.moveFocus(FocusDirection.NEXT);

        assertTrue(ui.isFocusVisible());
        assertEquals(FOCUSED_BG, grid().getBackground(8, 1));
        assertSame(field, ui.getKeyboardFocus());
    }

    @Test
    void caretBlinksWithSecondsAndResetsOnEdit() {
        TextField field = fieldAt(2, 1, 10);
        ui.setKeyboardFocus(field);
        assertEquals(CARET_BG, grid().getBackground(2, 1));

        ui.update(BLINK);
        assertEquals(NORMAL_BG, grid().getBackground(2, 1));
        ui.keyTyped('a');
        assertEquals(CARET_BG, grid().getBackground(3, 1));
        ui.update(BLINK);
        ui.update(BLINK);
        assertEquals(CARET_BG, grid().getBackground(3, 1));
    }

    @Test
    void underlineCaretWritesOverlayGlyphOnBottomRow() {
        TextField field = fieldAt(2, 1, 10);
        field.setStyle(TextFieldStyle.of(LabelStyle.of(UiFont.SQUARE, TEXT, NORMAL_BG))
                .withCaret(CaretStyle.none().withGlyph('_')));
        field.setText("hi");
        ui.setKeyboardFocus(field);

        ui.draw();

        assertEquals('_', surface.getOverlayGrid().getCodePoint(4, 1));
        assertEquals(TEXT, surface.getOverlayGrid().getForeground(4, 1));
        assertEquals(NORMAL_BG, surface.getSquareGrid().getBackground(4, 1));
    }

    @Test
    void tallFieldDrawsCaretGlyphOnItsLowerRow() {
        TextFieldStyle tall = TextFieldStyle.of(LabelStyle.of(UiFont.TALL, TEXT, NORMAL_BG))
                .withCaret(CaretStyle.none().withGlyph('_'));
        TextField field = new TextField(tall, 6);
        field.setPosition(0, 2);
        ui.getRoot().addChild(field);
        ui.setKeyboardFocus(field);

        ui.draw();

        assertEquals(2, field.getHeight());
        assertEquals('_', surface.getOverlayGrid().getCodePoint(0, 3));
    }

    @Test
    void placeholderShowsOnlyWhileEmpty() {
        TextField field = fieldAt(2, 1, 10);
        field.setPlaceholderText("name");

        TerminalGrid grid = grid();
        assertEquals('n', grid.getCodePoint(2, 1));
        assertEquals(PLACEHOLDER_FG, grid.getForeground(2, 1));

        field.setText("x");
        grid = grid();
        assertEquals('x', grid.getCodePoint(2, 1));
        assertEquals(TEXT, grid.getForeground(2, 1));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, grid.getCodePoint(3, 1));
    }

    @Test
    void caretStillShowsOverPlaceholder() {
        TextField field = fieldAt(2, 1, 10);
        field.setPlaceholderText("name");
        ui.setKeyboardFocus(field);

        TerminalGrid grid = grid();

        assertEquals(CARET_BG, grid.getBackground(2, 1));
        assertEquals('n', grid.getCodePoint(2, 1));
        assertEquals(CARET_FG, grid.getForeground(2, 1));
    }

    @Test
    void autoFocusIsOffByDefault() {
        TextField field = fieldAt(2, 1, 10);

        ui.update(0f);

        assertFalse(ui.isAutoFocusTextFields());
        assertEquals(null, ui.getKeyboardFocus());
        assertFalse(field.hasKeyboardFocus());
    }

    @Test
    void autoFocusPicksTopRowThenLeftColumnWithoutShowingFocus() {
        TextField lowerLeft = fieldAt(1, 4, 10);
        TextField upperRight = fieldAt(20, 1, 10);
        TextField upperLeft = fieldAt(5, 1, 10);
        ui.setAutoFocusTextFields(true);

        ui.update(0f);

        assertSame(upperLeft, ui.getKeyboardFocus());
        assertFalse(ui.isFocusVisible());
        assertFalse(lowerLeft.hasKeyboardFocus());
        assertFalse(upperRight.hasKeyboardFocus());
    }

    @Test
    void autoFocusUsesSurfacePositionsOfNestedFields() {
        WidgetGroup group = new WidgetGroup();
        group.setPosition(10, 0);
        TextField nested = new TextField(STYLE, 5);
        nested.setPosition(0, 2);
        group.addChild(nested);
        ui.getRoot().addChild(group);
        TextField plain = fieldAt(30, 3, 5);
        ui.setAutoFocusTextFields(true);

        ui.update(0f);

        assertSame(nested, ui.getKeyboardFocus());
        assertFalse(plain.hasKeyboardFocus());
    }

    @Test
    void autoFocusSkipsHiddenDisabledAndPopupFields() {
        TextField hidden = fieldAt(0, 0, 5);
        hidden.setVisible(false);
        TextField disabled = fieldAt(0, 1, 5);
        disabled.setDisabled(true);
        TextField popup = new TextField(STYLE, 5);
        ui.getPopupLayer().addChild(popup);
        TextField chosen = fieldAt(0, 5, 5);
        ui.setAutoFocusTextFields(true);

        ui.update(0f);

        assertSame(chosen, ui.getKeyboardFocus());
    }

    @Test
    void autoFocusLeavesExistingFocusAlone() {
        TextField first = fieldAt(0, 0, 5);
        TextField second = fieldAt(0, 3, 5);
        ui.setKeyboardFocus(second);
        ui.setAutoFocusTextFields(true);

        ui.update(0f);

        assertSame(second, ui.getKeyboardFocus());
        assertFalse(first.hasKeyboardFocus());
    }

    @Test
    void onscreenKeyboardIsWantedOnlyWhileATextFieldHasFocus() {
        List<Boolean> wanted = new ArrayList<>();
        ui.setTextInputWantedListener(wanted::add);
        TextField field = fieldAt(2, 1, 10);
        TextButton button = new TextButton("Go", ButtonStyle.of(
                LabelStyle.of(UiFont.SQUARE, TEXT, NORMAL_BG)));
        button.setPosition(2, 4);
        ui.getRoot().addChild(button);

        ui.setKeyboardFocus(button);
        ui.setKeyboardFocus(field);
        ui.setKeyboardFocus(field);
        ui.pointerDown(2, 4, 0, 0);
        ui.pointerUp(2, 4, 0, 0);
        ui.pointerDown(3, 1, 0, 0);
        ui.pointerUp(3, 1, 0, 0);
        ui.setKeyboardFocus(null);

        assertEquals(List.of(true, false, true, false), wanted);
    }
}
