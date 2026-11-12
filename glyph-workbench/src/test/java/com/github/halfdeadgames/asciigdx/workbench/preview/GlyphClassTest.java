package com.github.halfdeadgames.asciigdx.workbench.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.github.halfdeadgames.asciigdx.workbench.unicodenames.UnicodeFacts;
import org.junit.jupiter.api.Test;

class GlyphClassTest {

    private static final int NEW_LETTER = 0x31350;
    private static final int NEW_DIGIT = 0x1E5F1;
    private static final int NEW_SYMBOL = 0x1FA8F;
    private static final int NEW_PUNCTUATION = 0x2E5E;
    private static final int NEW_SPACE = 0x1E5F2;

    private static final UnicodeFacts OLD_JDK = UnicodeFacts.fromTableText("""
        31350 Lo NEW LETTER
        1E5F1 Nd NEW DIGIT
        1FA8F So SHOVEL
        2E5E Po NEW MARK
        1E5F2 Zs NEW SPACE
        """, codePoint -> Character.UNASSIGNED, codePoint -> null);

    @Test
    void newerLetterFromTheTableIsALetter() {
        assertEquals(GlyphClass.LETTER, GlyphClass.of(NEW_LETTER, OLD_JDK));
    }

    @Test
    void newerCharactersAreClassifiedByTheirTableCategory() {
        assertEquals(GlyphClass.DIGIT, GlyphClass.of(NEW_DIGIT, OLD_JDK));
        assertEquals(GlyphClass.SYMBOL, GlyphClass.of(NEW_SYMBOL, OLD_JDK));
        assertEquals(GlyphClass.PUNCTUATION, GlyphClass.of(NEW_PUNCTUATION, OLD_JDK));
    }

    @Test
    void newerSeparatorIsNotVisible() {
        assertNull(GlyphClass.of(NEW_SPACE, OLD_JDK));
    }

    @Test
    void codePointsTheJdkKnowsKeepTheirClass() {
        assertEquals(GlyphClass.LETTER, GlyphClass.of('A'));
        assertEquals(GlyphClass.DIGIT, GlyphClass.of('7'));
        assertEquals(GlyphClass.PUNCTUATION, GlyphClass.of('!'));
        assertEquals(GlyphClass.SYMBOL, GlyphClass.of('+'));
        assertEquals(GlyphClass.BOX_AND_BLOCK, GlyphClass.of(0x2588));
        assertNull(GlyphClass.of(' '));
        assertNull(GlyphClass.of('\n'));
        assertNull(GlyphClass.of(-1));
    }
}
