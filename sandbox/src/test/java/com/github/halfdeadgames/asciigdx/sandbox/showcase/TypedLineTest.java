package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TypedLineTest {

    private static final TypedLine LINE = new TypedLine(2f, "abcd", 2f);

    @Test
    void nothingVisibleBeforeStart() {
        assertEquals(0, LINE.visibleCharacters(1f));
        assertFalse(LINE.hasStarted(1.9f));
    }

    @Test
    void startsExactlyAtStartTime() {
        assertTrue(LINE.hasStarted(2f));
        assertEquals(0, LINE.visibleCharacters(2f));
    }

    @Test
    void revealsCharactersWhileTyping() {
        assertEquals(1, LINE.visibleCharacters(2.5f));
        assertEquals(2, LINE.visibleCharacters(3.2f));
    }

    @Test
    void showsEverythingAtExactEnd() {
        assertEquals(4, LINE.visibleCharacters(4f));
    }

    @Test
    void showsEverythingAfterEnd() {
        assertEquals(4, LINE.visibleCharacters(100f));
    }

    @Test
    void zeroLengthTextHasNothingToShow() {
        TypedLine empty = new TypedLine(1f, "", 30f);
        assertEquals(0, empty.visibleCharacters(0f));
        assertEquals(0, empty.visibleCharacters(5f));
        assertTrue(empty.hasStarted(1f));
    }

    @Test
    void exposesText() {
        assertEquals("abcd", LINE.text());
    }

    @Test
    void rejectsBadArguments() {
        assertThrows(IllegalArgumentException.class, () -> new TypedLine(0f, null, 30f));
        assertThrows(IllegalArgumentException.class, () -> new TypedLine(0f, "a", 0f));
        assertThrows(IllegalArgumentException.class, () -> new TypedLine(0f, "a", -1f));
        assertThrows(IllegalArgumentException.class, () -> new TypedLine(0f, "a", Float.NaN));
    }
}
