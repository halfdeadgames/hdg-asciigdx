package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextFieldModelTest {
    private static final String LONG_TEXT = "abcdefghijklmnopqrst";

    private final TextFieldModel model = new TextFieldModel();

    private String render(int width) {
        model.scrollToCaret(width);
        StringBuilder cells = new StringBuilder();
        for (int cell = 0; cell < width; cell++) {
            int codePoint = model.getTextColumnCodePoint(cell, width);
            cells.appendCodePoint(codePoint == TextFieldModel.NO_GLYPH ? ' ' : codePoint);
        }
        return cells.toString();
    }

    @Test
    void insertPutsCodePointAtCaretAndMovesCaret() {
        model.setText("ac");
        model.moveCaretToStart();
        model.moveCaretRight();
        model.insert('b');

        assertEquals("abc", model.getText());
        assertEquals(2, model.getCaret());
    }

    @Test
    void backspaceAndDeleteStopAtTheEnds() {
        model.setText("abc");
        model.moveCaretToStart();
        model.deleteBeforeCaret();
        assertEquals("abc", model.getText());
        model.deleteAtCaret();
        assertEquals("bc", model.getText());
        model.moveCaretToEnd();
        model.deleteAtCaret();
        model.deleteBeforeCaret();
        assertEquals("b", model.getText());
        assertEquals(1, model.getCaret());
    }

    @Test
    void caretMovesStayInRange() {
        model.setText("ab");
        model.moveCaretRight();
        assertEquals(2, model.getCaret());
        model.moveCaretToStart();
        model.moveCaretLeft();
        assertEquals(0, model.getCaret());
        model.placeCaret(99);
        assertEquals(2, model.getCaret());
        model.placeCaret(-5);
        assertEquals(0, model.getCaret());
    }

    @Test
    void codePointsCountAsOneEach() {
        int smiley = 0x1F600;
        model.setText("a" + new String(Character.toChars(smiley)) + "b");

        assertEquals(3, model.getLength());
        model.moveCaretLeft();
        model.deleteBeforeCaret();
        assertEquals("ab", model.getText());
        model.insert(smiley);
        assertEquals(3, model.getLength());
    }

    @Test
    void maxLengthMakesModelFullButKeepsLongerText() {
        model.setMaxLength(3);
        model.setText("abcd");
        assertTrue(model.isFull());
        model.deleteBeforeCaret();
        assertTrue(model.isFull());
        model.deleteBeforeCaret();
        assertFalse(model.isFull());
    }

    @Test
    void textThatFitsHasNoEllipsisAndLeavesRoomForCaretAtEnd() {
        model.setText("abcd");

        assertEquals("abcd      ", render(10));
        assertEquals(4, model.getCaretTextColumn());
    }

    @Test
    void textExactlyAsWideAsWindowScrollsOneCellForTheCaret() {
        model.setText("abcdefgh");

        assertEquals("…cdefgh ", render(8));
        assertEquals(7, model.getCaretTextColumn());
    }

    @Test
    void caretAtStartShowsEllipsisAtTheRightEndOnly() {
        model.setText(LONG_TEXT);
        model.moveCaretToStart();

        assertEquals("abcdefg…", render(8));
        assertEquals(0, model.getCaretTextColumn());
    }

    @Test
    void caretAtEndShowsEllipsisAtTheLeftEndOnly() {
        model.setText(LONG_TEXT);

        assertEquals("…opqrst ", render(8));
        assertEquals(7, model.getCaretTextColumn());
    }

    @Test
    void caretInTheMiddleShowsBothEllipses() {
        model.setText(LONG_TEXT);
        render(8);
        model.placeCaret(10);

        assertEquals("…klmnop…", render(8));
        assertEquals(1, model.getCaretTextColumn());
    }

    @Test
    void caretNeverSitsOnAnEllipsisWhileMovingThroughTheText() {
        model.setText(LONG_TEXT);
        model.moveCaretToStart();
        for (int step = 0; step <= LONG_TEXT.length(); step++) {
            String cells = render(8);
            int caretCell = model.getCaretTextColumn();
            assertTrue(caretCell >= 0 && caretCell < 8, "caret off screen at " + step);
            assertTrue(cells.charAt(caretCell) != '…', "caret on ellipsis at " + step);
            model.moveCaretRight();
        }
        for (int step = 0; step <= LONG_TEXT.length(); step++) {
            String cells = render(8);
            int caretCell = model.getCaretTextColumn();
            assertTrue(cells.charAt(caretCell) != '…', "caret on ellipsis going left at " + step);
            model.moveCaretLeft();
        }
    }

    @Test
    void windowStaysPutWhileCaretMovesInsideIt() {
        model.setText(LONG_TEXT);
        render(8);
        model.placeCaret(10);
        render(8);
        int start = model.getWindowStart();

        model.moveCaretRight();
        render(8);

        assertEquals(start, model.getWindowStart());
    }

    @Test
    void deletingTextPullsWindowBack() {
        model.setText(LONG_TEXT);
        render(8);
        for (int i = 0; i < 15; i++) {
            model.deleteBeforeCaret();
        }

        assertEquals("abcde   ", render(8));
        assertEquals(0, model.getWindowStart());
    }

    @Test
    void narrowWindowsShowNoEllipsisButKeepCaretVisible() {
        model.setText("abcdef");

        assertEquals(" ", render(1));
        assertEquals(0, model.getCaretTextColumn());
        assertEquals("f ", render(2));
        model.moveCaretToStart();
        assertEquals("ab", render(2));
    }

    @Test
    void widthOfThreeCanHoldBothEllipses() {
        model.setText("abcdefgh");
        model.placeCaret(4);

        String cells = render(3);

        assertEquals("…e…", cells.substring(0, 1) + cells.charAt(1) + cells.charAt(2));
        assertEquals(1, model.getCaretTextColumn());
    }
}
