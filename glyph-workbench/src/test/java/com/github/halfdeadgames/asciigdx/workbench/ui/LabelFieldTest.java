package com.github.halfdeadgames.asciigdx.workbench.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LabelFieldTest {

    private static LabelField field(String initial) {
        LabelField field = new LabelField();
        field.begin(initial);
        return field;
    }

    private static void type(LabelField field, String characters) {
        for (char character : characters.toCharArray()) {
            field.type(character);
        }
    }

    @Test
    void startsFromTheGivenText() {
        assertEquals("MID", field("MID").value());
    }

    @Test
    void lettersAreUppercased() {
        LabelField field = field("");
        type(field, "xHt");
        assertEquals("XHT", field.value());
    }

    @Test
    void whitespaceAndControlCharactersAreRefused() {
        LabelField field = field("");
        // U+00A0 is a no-break space.
        type(field, "a b\tc\nd\re\u00A0f\u0007g");
        assertEquals("ABCDEFG", field.value());
    }

    @Test
    void privateUseCharactersAreRefused() {
        LabelField field = field("");
        // U+F5E1, U+E000, and U+F8FF are private-use characters; U+DFFF is a lone low surrogate.
        type(field, "a\uF5E1b\uE000c\uF8FFd\uDFFF");
        assertEquals("ABCD", field.value());
    }

    @Test
    void lengthStopsAtTheMaximum() {
        LabelField field = field("");
        type(field, "abcdefghij");
        assertEquals("ABCDEFGH", field.value());
        assertEquals(LabelField.MAX_LENGTH, field.value().length());
    }

    @Test
    void aHashCannotStartTheLabelButCanFollowOtherCharacters() {
        LabelField field = field("");
        type(field, "#");
        assertTrue(field.isEmpty());
        type(field, "a#");
        assertEquals("A#", field.value());
        field.backspace();
        field.backspace();
        type(field, "#b");
        assertEquals("B", field.value());
    }

    @Test
    void backspaceDeletesTheLastCharacterAndStopsAtEmpty() {
        LabelField field = field("AB");
        field.backspace();
        assertEquals("A", field.value());
        field.backspace();
        field.backspace();
        assertTrue(field.isEmpty());
    }

    @Test
    void typingIsIgnoredWhenNotEditing() {
        LabelField field = new LabelField();
        type(field, "abc");
        assertFalse(field.isActive());
        assertTrue(field.isEmpty());
        field.begin("A");
        field.end();
        type(field, "bc");
        field.backspace();
        assertEquals("A", field.value());
    }
}
