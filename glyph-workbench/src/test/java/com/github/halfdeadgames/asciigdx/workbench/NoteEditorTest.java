package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NoteEditorTest {

    @Test
    void ignoresTypingWhileInactive() {
        NoteEditor editor = new NoteEditor();
        assertFalse(editor.type('a'));
        assertEquals("", editor.value());
    }

    @Test
    void acceptsPrintableCharactersIncludingSpaceAndRejectsControls() {
        NoteEditor editor = new NoteEditor();
        editor.begin("", false);
        assertTrue(editor.type('a'));
        assertTrue(editor.type(' '));
        assertTrue(editor.type('~'));
        assertFalse(editor.type('\n'));
        assertFalse(editor.type('\t'));
        assertFalse(editor.type((char) 127));
        assertEquals("a ~", editor.value());
    }

    @Test
    void backspaceDeletesTheLastCharacterAndStopsAtEmpty() {
        NoteEditor editor = new NoteEditor();
        editor.begin("ab", false);
        assertTrue(editor.backspace());
        assertTrue(editor.backspace());
        assertFalse(editor.backspace());
        assertEquals("", editor.value());
    }

    @Test
    void stopsAtTheMaximumLength() {
        NoteEditor editor = new NoteEditor();
        editor.begin("x".repeat(NoteEditor.MAX_LENGTH), true);
        assertFalse(editor.type('y'));
        assertEquals(NoteEditor.MAX_LENGTH, editor.value().length());
        assertTrue(editor.isEditingExisting());
    }

    @Test
    void prefillIsCutToTheMaximumLength() {
        NoteEditor editor = new NoteEditor();
        editor.begin("x".repeat(NoteEditor.MAX_LENGTH + 5), true);
        assertEquals(NoteEditor.MAX_LENGTH, editor.value().length());
    }

    @Test
    void beginPlacesTheCaretAtTheEnd() {
        NoteEditor editor = new NoteEditor();
        editor.begin("abc", false);
        assertEquals(3, editor.caret());
    }

    @Test
    void caretMovesStopAtBothEnds() {
        NoteEditor editor = new NoteEditor();
        editor.begin("ab", false);
        assertFalse(editor.caretRight());
        assertTrue(editor.caretLeft());
        assertTrue(editor.caretLeft());
        assertFalse(editor.caretLeft());
        assertEquals(0, editor.caret());
        assertTrue(editor.caretRight());
        assertEquals(1, editor.caret());
    }

    @Test
    void homeAndEndJumpToTheEnds() {
        NoteEditor editor = new NoteEditor();
        editor.begin("abc", false);
        editor.caretToStart();
        assertEquals(0, editor.caret());
        editor.caretToEnd();
        assertEquals(3, editor.caret());
    }

    @Test
    void typingInsertsAtTheCaret() {
        NoteEditor editor = new NoteEditor();
        editor.begin("ac", false);
        editor.caretLeft();
        assertTrue(editor.type('b'));
        assertEquals("abc", editor.value());
        assertEquals(2, editor.caret());
    }

    @Test
    void backspaceDeletesBeforeTheCaret() {
        NoteEditor editor = new NoteEditor();
        editor.begin("abc", false);
        editor.caretLeft();
        assertTrue(editor.backspace());
        assertEquals("ac", editor.value());
        assertEquals(1, editor.caret());
        editor.caretToStart();
        assertFalse(editor.backspace());
    }

    @Test
    void forwardDeleteRemovesAfterTheCaretAndKeepsIt() {
        NoteEditor editor = new NoteEditor();
        editor.begin("abc", false);
        editor.caretToStart();
        assertTrue(editor.deleteForward());
        assertEquals("bc", editor.value());
        assertEquals(0, editor.caret());
        editor.caretToEnd();
        assertFalse(editor.deleteForward());
    }

    @Test
    void caretAndDeletionStepOverWholeCodePoints() {
        NoteEditor editor = new NoteEditor();
        editor.begin("a😀b", false);
        editor.caretLeft();
        editor.caretLeft();
        assertEquals(1, editor.caret());
        editor.caretRight();
        assertEquals(3, editor.caret());
        assertTrue(editor.backspace());
        assertEquals("ab", editor.value());
    }

    @Test
    void rowsAreFixedWidthChunks() {
        NoteEditor editor = new NoteEditor();
        editor.begin("abcdefg", false);
        assertEquals(3, editor.rowCount(3));
        assertEquals("abc", editor.row(0, 3));
        assertEquals("g", editor.row(2, 3));
        assertEquals(2, editor.caretRow(3));
        assertEquals(1, editor.caretColumn(3));
    }

    @Test
    void aFullLastRowPutsTheCaretOnAnEmptyRowBelow() {
        NoteEditor editor = new NoteEditor();
        editor.begin("abcdef", false);
        assertEquals(3, editor.rowCount(3));
        assertEquals("", editor.row(2, 3));
        assertEquals(2, editor.caretRow(3));
        assertEquals(0, editor.caretColumn(3));
    }

    @Test
    void scrollWindowFollowsTheCaretBothWays() {
        NoteEditor editor = new NoteEditor();
        editor.begin("x".repeat(30), false);
        assertEquals(8, editor.firstVisibleRow(3, 3));
        editor.caretToStart();
        assertEquals(0, editor.firstVisibleRow(3, 3));
        editor.caretToEnd();
        editor.firstVisibleRow(3, 3);
        for (int i = 0; i < 12; i++) {
            editor.caretLeft();
        }
        int top = editor.firstVisibleRow(3, 3);
        assertTrue(editor.caretRow(3) >= top && editor.caretRow(3) < top + 3);
    }

    @Test
    void scrollWindowStaysPutWhileTheCaretIsInside() {
        NoteEditor editor = new NoteEditor();
        editor.begin("x".repeat(30), false);
        editor.firstVisibleRow(3, 3);
        editor.caretLeft();
        editor.caretLeft();
        editor.caretLeft();
        assertEquals(8, editor.firstVisibleRow(3, 3));
    }

    @Test
    void endDeactivates() {
        NoteEditor editor = new NoteEditor();
        editor.begin("a", false);
        editor.end();
        assertFalse(editor.isActive());
        assertFalse(editor.type('b'));
    }
}
