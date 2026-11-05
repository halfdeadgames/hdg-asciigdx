package com.github.halfdeadgames.asciigdx.workbench.notes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NoteEditorTest {

    private static NoteEditor editor(String initial) {
        NoteEditor editor = new NoteEditor();
        editor.begin(initial, false);
        return editor;
    }

    @Test
    void typesAtTheCaretAndIgnoresControlCharacters() {
        NoteEditor editor = editor("ac");
        editor.caretLeft();
        editor.type('b');
        editor.type('\n');
        editor.type('\t');
        assertEquals("abc", editor.value());
    }

    @Test
    void nothingChangesWhileInactive() {
        NoteEditor editor = new NoteEditor();
        editor.type('x');
        editor.backspace();
        assertEquals("", editor.value());
        assertFalse(editor.isActive());
    }

    @Test
    void backspaceAndDeleteWorkAroundTheCaret() {
        NoteEditor editor = editor("abcd");
        editor.caretLeft();
        editor.backspace();
        assertEquals("abd", editor.value());
        editor.deleteForward();
        assertEquals("ab", editor.value());
        editor.caretToStart();
        editor.backspace();
        assertEquals("ab", editor.value());
    }

    @Test
    void thereIsNoLengthLimit() {
        NoteEditor editor = editor("x".repeat(5000));
        editor.type('y');
        assertEquals(5001, editor.value().length());
    }

    @Test
    void caretMovesByRowsKeepingItsColumn() {
        NoteEditor editor = editor("aaaa bbbb cccc");
        editor.caretToStart();
        editor.caretRight();
        editor.caretRight();
        editor.caretRows(1, 4);
        assertEquals(1, editor.caretRow(4));
        assertEquals(2, editor.caretColumn(4));
        editor.caretRows(1, 4);
        assertEquals(2, editor.caretRow(4));
        editor.caretRows(-2, 4);
        assertEquals(0, editor.caretRow(4));
        assertEquals(2, editor.caretColumn(4));
    }

    @Test
    void caretRowsStopAtTheTextEnds() {
        NoteEditor editor = editor("aaaa bbbb");
        editor.caretRows(-5, 4);
        assertEquals(0, editor.caretRow(4));
        editor.caretRows(50, 4);
        assertEquals(editor.value().length(), editor.value().length());
        assertEquals(editor.rowCount(4) - 1, editor.caretRow(4));
    }

    @Test
    void shortRowsPullTheCaretToTheirEnd() {
        NoteEditor editor = editor("abcd e fghi");
        editor.caretToStart();
        for (int i = 0; i < 3; i++) {
            editor.caretRight();
        }
        editor.caretRows(1, 4);
        assertEquals(1, editor.caretRow(4));
        assertEquals(1, editor.caretColumn(4));
    }

    @Test
    void scrollingTheViewDoesNotMoveTheCaretAndTypingBringsItBack() {
        NoteEditor editor = editor("a b c d e f g h i j");
        int rows = editor.rowCount(1);
        int atCaret = editor.viewTop(1, 3);
        assertEquals(rows - 3, atCaret);
        editor.scrollView(-100);
        assertEquals(0, editor.viewTop(1, 3));
        assertEquals(rows - 1, editor.caretRow(1));
        editor.scrollView(1);
        assertEquals(1, editor.viewTop(1, 3));
        editor.type('z');
        assertEquals(editor.rowCount(1) - 3, editor.viewTop(1, 3));
    }

    @Test
    void viewNeverScrollsPastTheText() {
        NoteEditor editor = editor("a b c");
        editor.scrollView(1000);
        assertEquals(Math.max(0, editor.rowCount(1) - 3), editor.viewTop(1, 3));
    }

    @Test
    void caretFollowsWrappedRows() {
        NoteEditor editor = editor("aaa bbb ccc");
        assertEquals(4, editor.rowCount(3));
        assertEquals(3, editor.caretRow(3));
        assertEquals(0, editor.caretColumn(3));
        editor.caretToStart();
        assertEquals(0, editor.caretRow(3));
        assertEquals(0, editor.caretColumn(3));
        editor.caretRight();
        editor.caretRight();
        editor.caretRight();
        assertEquals(0, editor.caretRow(3));
        assertEquals(3, editor.caretColumn(3));
        editor.caretRight();
        assertEquals(1, editor.caretRow(3));
        assertEquals(0, editor.caretColumn(3));
    }

    @Test
    void aFullLastRowMovesTheCaretDown() {
        NoteEditor editor = editor("abcd");
        assertEquals(2, editor.rowCount(4));
        assertEquals(1, editor.caretRow(4));
        assertEquals(0, editor.caretColumn(4));
    }

    @Test
    void windowKeepsTheCaretRowVisible() {
        NoteEditor editor = editor("a b c d e f");
        assertTrue(editor.viewTop(1, 3) >= 3);
        editor.caretToStart();
        assertEquals(0, editor.viewTop(1, 3));
    }

    @Test
    void remembersIfItEditsAnExistingNote() {
        NoteEditor editor = new NoteEditor();
        editor.begin("old", true);
        assertTrue(editor.isEditingExisting());
        editor.end();
        assertFalse(editor.isActive());
    }
}
