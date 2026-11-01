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
    void endDeactivates() {
        NoteEditor editor = new NoteEditor();
        editor.begin("a", false);
        editor.end();
        assertFalse(editor.isActive());
        assertFalse(editor.type('b'));
    }
}
