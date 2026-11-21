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
    private static final int CARET_GLYPH = 0x258F;
    private static final float BLINK = 0.5f;
    private static final char BACKSPACE_CHARACTER = '\b';
    private static final char DELETE_CHARACTER = 127;
    private static final CaretStyle BAR = CaretStyle.none().withGlyph(CARET_GLYPH)
            .withForegroundRgba(CARET_FG).withBlinkSeconds(BLINK);
    private static final TextFieldStyle STYLE = TextFieldStyle.of(
            LabelStyle.of(TEXT, NORMAL_BG))
            .withFocused(LabelStyle.of(TEXT, FOCUSED_BG))
            .withDisabled(LabelStyle.of(TEXT, DISABLED_BG))
            .withPlaceholder(LabelStyle.of(PLACEHOLDER_FG, NORMAL_BG))
            .withCaret(BAR);

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

    private TerminalGrid squareGrid() {
        ui.draw();
        return surface.getSquareGrid();
    }

    private TerminalGrid textGrid() {
        ui.draw();
        return surface.getTextGrid();
    }

    private TerminalGrid overlayGrid() {
        ui.draw();
        return surface.getOverlayGrid();
    }

    private String textRow(int row, int fromTextColumn, int count) {
        TerminalGrid grid = textGrid();
        StringBuilder builder = new StringBuilder();
        for (int column = fromTextColumn; column < fromTextColumn + count; column++) {
            builder.appendCodePoint(grid.getCodePoint(column, row));
        }
        return builder.toString();
    }

    @Test
    void sizeIsGivenWidthAndOneRow() {
        TextField field = new TextField(STYLE, 12);

        assertEquals(12, field.getWidth());
        assertEquals(1, field.getHeight());
        assertThrows(IllegalArgumentException.class, () -> new TextField(STYLE, 0));
    }

    @Test
    void placeholderKeepsSurrogatePairAsOneCodePoint() {
        TextField field = fieldAt(2, 1, 10);

        field.setPlaceholderText("a😀");

        assertEquals("a😀", field.getPlaceholderText());
        assertEquals("a😀", textRow(1, 4, 2));
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
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, overlayGrid().getCodePoint(4, 1));

        field.setStyle(TextFieldStyle.of(LabelStyle.of(TEXT, FOCUSED_BG)).withCaret(BAR));

        assertEquals(CARET_GLYPH, overlayGrid().getCodePoint(4, 1));
    }

    @Test
    void pressOnTheFocusedFieldAsksForTheKeyboardAgain() {
        List<String> calls = new ArrayList<>();
        ui.addTextInputWantedListener(new UiRoot.TextInputWantedListener() {
            @Override
            public void textInputWantedChanged(boolean wanted) {
                calls.add("changed " + wanted);
            }

            @Override
            public void textInputRequestedAgain() {
                calls.add("again");
            }
        });
        TextField field = fieldAt(2, 1, 10);

        ui.pointerDown(3, 1, 0, 0);
        ui.pointerUp(3, 1, 0, 0);
        assertEquals(List.of("changed true"), calls);
        ui.pointerDown(4, 1, 0, 0);
        ui.pointerUp(4, 1, 0, 0);

        assertEquals(List.of("changed true", "again"), calls);
        assertSame(field, ui.getKeyboardFocus());
    }

    @Test
    void disablingTheFocusedFieldClearsFocusAndStopsWantingTheKeyboard() {
        List<Boolean> wanted = new ArrayList<>();
        ui.addTextInputWantedListener(wanted::add);
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
                LabelStyle.of(TEXT, NORMAL_BG)));
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
    void pressFocusesAndPlacesCaretAtTheLeftHalfOfTheClickedCell() {
        TextField field = fieldAt(2, 1, 10);
        field.setText("hello");

        assertTrue(ui.pointerDown(2 + 3, 1, 0, 0));
        ui.pointerUp(2 + 3, 1, 0, 0);

        assertSame(field, ui.getKeyboardFocus());
        assertEquals(5, field.getCaretIndex());

        ui.pointerDown(2 + 1, 1, 0, 0);
        ui.pointerUp(2 + 1, 1, 0, 0);
        assertEquals(2, field.getCaretIndex());
    }

    @Test
    void pressPlacesCaretByTextColumnWhenTheEventTextColumnIsOdd() {
        TextField field = fieldAt(2, 1, 10);
        field.setText("hello");
        int fieldLeftTextColumn = 2 * 2;

        assertTrue(ui.pointerDownAtTextColumn(fieldLeftTextColumn + 3, 1, 0, 0));
        ui.pointerUpAtTextColumn(fieldLeftTextColumn + 3, 1, 0, 0);

        assertSame(field, ui.getKeyboardFocus());
        assertEquals(3, field.getCaretIndex());

        ui.pointerDownAtTextColumn(fieldLeftTextColumn + 1, 1, 0, 0);
        ui.pointerUpAtTextColumn(fieldLeftTextColumn + 1, 1, 0, 0);
        assertEquals(1, field.getCaretIndex());
    }

    @Test
    void pressPastTheEndOfTheTextPutsTheCaretAtTheEnd() {
        TextField field = fieldAt(2, 1, 10);
        field.setText("hello");

        ui.pointerDownAtTextColumn(2 * 2 + 15, 1, 0, 0);
        ui.pointerUpAtTextColumn(2 * 2 + 15, 1, 0, 0);

        assertEquals(5, field.getCaretIndex());
    }

    @Test
    void pressOnScrolledFieldCountsFromTheWindowStartInTextColumns() {
        TextField field = fieldAt(0, 0, 8);
        field.setText("abcdefghijklmnopqrst");
        ui.draw();

        // The caret is at the end, so the 16 text columns scroll to start at index 5.
        ui.pointerDownAtTextColumn(3, 0, 0, 0);
        ui.pointerUpAtTextColumn(3, 0, 0, 0);

        assertEquals(8, field.getCaretIndex());
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
        assertEquals(DISABLED_BG, squareGrid().getBackground(3, 1));
    }

    @Test
    void drawsTextInTextColumnsOnFieldBackground() {
        TextField field = fieldAt(2, 1, 10);
        field.setText("hi");

        TerminalGrid text = textGrid();

        assertEquals('h', text.getCodePoint(4, 1));
        assertEquals('i', text.getCodePoint(5, 1));
        assertEquals(TEXT, text.getForeground(4, 1));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, text.getCodePoint(6, 1));
        assertEquals(NORMAL_BG, surface.getSquareGrid().getBackground(11, 1));
    }

    @Test
    void fieldShowsTwoCharactersPerCellOfWidth() {
        TextField field = fieldAt(1, 2, 5);
        field.setText("abcdefghi");

        assertEquals("abcdefghi", textRow(2, 2, 9));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, surface.getTextGrid().getCodePoint(11, 2));
        assertEquals(NORMAL_BG, surface.getSquareGrid().getBackground(5, 2));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, surface.getTextGrid().getCodePoint(12, 2));
    }

    @Test
    void drawsLeftEllipsisAndScrollsWhenTheCaretIsAtTheEnd() {
        TextField field = fieldAt(0, 0, 8);
        field.setText("abcdefghijklmnopqrst");

        TerminalGrid text = textGrid();

        assertEquals(0x2026, text.getCodePoint(0, 0));
        assertEquals('g', text.getCodePoint(1, 0));
        assertEquals('t', text.getCodePoint(14, 0));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, text.getCodePoint(15, 0));
    }

    @Test
    void drawsRightEllipsisWhenTheTextIsCutAtTheEnd() {
        TextField field = fieldAt(0, 0, 8);
        field.setText("abcdefghijklmnopqrst");
        ui.setKeyboardFocus(field);
        ui.keyDown(Input.Keys.HOME);

        TerminalGrid text = textGrid();

        assertEquals('a', text.getCodePoint(0, 0));
        assertEquals('o', text.getCodePoint(14, 0));
        assertEquals(0x2026, text.getCodePoint(15, 0));
    }

    @Test
    void caretShowsOnlyWhileFocusedAndLeavesTheLetterUnderItUnrecolored() {
        TextField field = fieldAt(2, 1, 10);
        field.setText("hi");

        assertEquals(TerminalGrid.EMPTY_CODE_POINT, overlayGrid().getCodePoint(6, 1));
        ui.setKeyboardFocus(field);
        TerminalGrid overlay = overlayGrid();
        assertEquals(CARET_GLYPH, overlay.getCodePoint(6, 1));
        assertEquals(CARET_FG, overlay.getForeground(6, 1));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, overlay.getCodePoint(5, 1));

        ui.keyDown(Input.Keys.HOME);
        overlay = overlayGrid();
        assertEquals(CARET_GLYPH, overlay.getCodePoint(4, 1));
        assertEquals(CARET_FG, overlay.getForeground(4, 1));
        assertEquals('h', surface.getTextGrid().getCodePoint(4, 1));
        assertEquals(TEXT, surface.getTextGrid().getForeground(4, 1));
        assertEquals(FOCUSED_BG, surface.getSquareGrid().getBackground(2, 1));
        ui.setKeyboardFocus(null);
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, overlayGrid().getCodePoint(4, 1));
    }

    @Test
    void caretOnAnOddTextColumnSitsInTheRightHalfOfItsCell() {
        TextField field = fieldAt(2, 1, 10);
        field.setText("h");
        ui.setKeyboardFocus(field);

        TerminalGrid overlay = overlayGrid();

        assertEquals(CARET_GLYPH, overlay.getCodePoint(5, 1));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, overlay.getCodePoint(4, 1));
    }

    @Test
    void caretShowsWithoutVisibleFocusLook() {
        TextField field = fieldAt(2, 1, 10);
        ui.setKeyboardFocus(field);

        TerminalGrid overlay = overlayGrid();

        assertFalse(ui.isFocusVisible());
        assertEquals(CARET_GLYPH, overlay.getCodePoint(4, 1));
        assertEquals(NORMAL_BG, surface.getSquareGrid().getBackground(2, 1));
        assertEquals(NORMAL_BG, surface.getSquareGrid().getBackground(3, 1));
    }

    @Test
    void focusedLookShowsAfterKeyboardNavigation() {
        TextField field = fieldAt(2, 1, 10);
        ui.moveFocus(FocusDirection.NEXT);

        assertTrue(ui.isFocusVisible());
        assertEquals(FOCUSED_BG, squareGrid().getBackground(8, 1));
        assertSame(field, ui.getKeyboardFocus());
    }

    @Test
    void caretBlinksWithSecondsAndResetsOnEdit() {
        TextField field = fieldAt(2, 1, 10);
        ui.setKeyboardFocus(field);
        assertEquals(CARET_GLYPH, overlayGrid().getCodePoint(4, 1));

        ui.update(BLINK);
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, overlayGrid().getCodePoint(4, 1));
        ui.keyTyped('a');
        assertEquals(CARET_GLYPH, overlayGrid().getCodePoint(5, 1));
        ui.update(BLINK);
        ui.update(BLINK);
        assertEquals(CARET_GLYPH, overlayGrid().getCodePoint(5, 1));
    }

    @Test
    void caretWithoutForegroundUsesTheTextColor() {
        TextField field = fieldAt(2, 1, 10);
        field.setStyle(TextFieldStyle.of(LabelStyle.of(TEXT, NORMAL_BG))
                .withCaret(CaretStyle.none().withGlyph('_')));
        field.setText("hi");
        ui.setKeyboardFocus(field);

        TerminalGrid overlay = overlayGrid();

        assertEquals('_', overlay.getCodePoint(6, 1));
        assertEquals(TEXT, overlay.getForeground(6, 1));
        assertEquals(NORMAL_BG, surface.getSquareGrid().getBackground(3, 1));
    }

    @Test
    void caretWithoutAGlyphDrawsNothing() {
        TextField field = fieldAt(2, 1, 10);
        field.setStyle(TextFieldStyle.of(LabelStyle.of(TEXT, NORMAL_BG))
                .withCaret(CaretStyle.none()));
        ui.setKeyboardFocus(field);

        TerminalGrid overlay = overlayGrid();

        for (int textColumn = 0; textColumn < surface.getTextColumns(); textColumn++) {
            assertEquals(TerminalGrid.EMPTY_CODE_POINT, overlay.getCodePoint(textColumn, 1));
        }
    }

    @Test
    void placeholderShowsInTextColumnsOnlyWhileEmpty() {
        TextField field = fieldAt(2, 1, 10);
        field.setPlaceholderText("name");

        TerminalGrid text = textGrid();
        assertEquals("name", textRow(1, 4, 4));
        assertEquals(PLACEHOLDER_FG, text.getForeground(4, 1));

        field.setText("x");
        text = textGrid();
        assertEquals('x', text.getCodePoint(4, 1));
        assertEquals(TEXT, text.getForeground(4, 1));
        assertEquals(TerminalGrid.EMPTY_CODE_POINT, text.getCodePoint(5, 1));
    }

    @Test
    void caretStillShowsOverPlaceholder() {
        TextField field = fieldAt(2, 1, 10);
        field.setPlaceholderText("name");
        ui.setKeyboardFocus(field);

        TerminalGrid overlay = overlayGrid();

        assertEquals(CARET_GLYPH, overlay.getCodePoint(4, 1));
        assertEquals(CARET_FG, overlay.getForeground(4, 1));
        assertEquals('n', surface.getTextGrid().getCodePoint(4, 1));
        assertEquals(PLACEHOLDER_FG, surface.getTextGrid().getForeground(4, 1));
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
        ui.addTextInputWantedListener(wanted::add);
        TextField field = fieldAt(2, 1, 10);
        TextButton button = new TextButton("Go", ButtonStyle.of(
                LabelStyle.of(TEXT, NORMAL_BG)));
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
