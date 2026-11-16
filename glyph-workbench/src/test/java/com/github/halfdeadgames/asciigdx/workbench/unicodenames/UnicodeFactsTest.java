package com.github.halfdeadgames.asciigdx.workbench.unicodenames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UnicodeFactsTest {

    private static final int SHOVEL = 0x1FA8F;
    private static final int OPEN_QUOTE = 0x2E42;
    private static final int CLOSE_QUOTE = 0x2E43;
    private static final String TABLE = """
            1FA8F So SHOVEL
            2E42 Pi NEW OPENING QUOTE
            2E43 Pf NEW CLOSING QUOTE
            31350..33479 Lo CJK UNIFIED IDEOGRAPH-*
            BLOCK 0000..007F Basic Latin
            BLOCK 1FA80..1FAFF Symbols and Pictographs Extended-A
            BLOCK 1FB00..1FBFF Symbols for Legacy Computing
            """;

    private static int unknownToJdk(int codePoint) {
        return Character.UNASSIGNED;
    }

    private static final UnicodeFacts OLD_JDK = UnicodeFacts.fromTableText(TABLE,
            UnicodeFactsTest::unknownToJdk, codePoint -> null);
    private static final UnicodeFacts REAL_JDK =
            UnicodeFacts.fromTableText(TABLE, Character::getType,
                    Character::getName);

    @Test
    void jdkTypeWinsForACodePointTheJdkKnows() {
        assertEquals(Character.UPPERCASE_LETTER, REAL_JDK.type('A'));
    }

    @Test
    void tableTypeFillsInWhatTheJdkDoesNotKnow() {
        assertEquals(Character.OTHER_SYMBOL, OLD_JDK.type(SHOVEL));
        assertEquals(Character.OTHER_LETTER, OLD_JDK.type(0x32000));
    }

    @Test
    void unlistedCodePointsStayUnassigned() {
        assertEquals(Character.UNASSIGNED, OLD_JDK.type(SHOVEL + 1));
        assertEquals(Character.UNASSIGNED, OLD_JDK.type('A'));
    }

    @Test
    void quoteCategoriesMapToJavaQuoteConstants() {
        assertEquals(Character.INITIAL_QUOTE_PUNCTUATION, OLD_JDK.type(OPEN_QUOTE));
        assertEquals(Character.FINAL_QUOTE_PUNCTUATION, OLD_JDK.type(CLOSE_QUOTE));
    }

    @Test
    void blockComesFromTheTableEvenWhenTheJdkKnowsTheCodePoint() {
        assertEquals("Basic Latin", REAL_JDK.blockOf('A'));
    }

    @Test
    void blockBoundariesAreInclusive() {
        assertNull(OLD_JDK.blockOf(0x1FA7F));
        assertEquals("Symbols and Pictographs Extended-A", OLD_JDK.blockOf(0x1FA80));
        assertEquals("Symbols and Pictographs Extended-A", OLD_JDK.blockOf(0x1FAFF));
        assertEquals("Symbols for Legacy Computing", OLD_JDK.blockOf(0x1FB00));
        assertEquals("Symbols for Legacy Computing", OLD_JDK.blockOf(0x1FBFF));
        assertNull(OLD_JDK.blockOf(0x1FC00));
    }

    @Test
    void nameFallsBackFromJdkToTable() {
        assertEquals("LATIN CAPITAL LETTER A", REAL_JDK.name('A'));
        assertEquals("SHOVEL", OLD_JDK.name(SHOVEL));
        assertEquals("CJK UNIFIED IDEOGRAPH-32000", OLD_JDK.name(0x32000));
        assertNull(OLD_JDK.name(SHOVEL + 1));
    }

    @Test
    void categoryTypesTellLettersAndSeparators() {
        assertTrue(CategoryTypes.isLetter(Character.OTHER_LETTER));
        assertTrue(CategoryTypes.isLetter(Character.TITLECASE_LETTER));
        assertFalse(CategoryTypes.isLetter(Character.LETTER_NUMBER));
        assertTrue(CategoryTypes.isSeparator(Character.SPACE_SEPARATOR));
        assertTrue(CategoryTypes.isSeparator(Character.PARAGRAPH_SEPARATOR));
        assertFalse(CategoryTypes.isSeparator(Character.CONTROL));
    }
}
