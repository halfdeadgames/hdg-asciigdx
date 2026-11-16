package com.github.halfdeadgames.asciigdx.workbench.unicodenames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UnicodeNameTableTest {

    private static final int SHOVEL = 0x1FA8F;
    private static final String CJK_RANGE = "31350..33479 Lo CJK UNIFIED IDEOGRAPH-*\n";

    private static void assertRejectedAtLine(int lineNumber, String text) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> UnicodeNameTable.parse(text));
        assertTrue(error.getMessage().contains("line " + lineNumber), error.getMessage());
    }

    @Test
    void parsesEntryAndSkipsCommentsAndBlankLines() {
        UnicodeNameTable table =
                UnicodeNameTable.parse("# header\n\n1FA8F So SHOVEL\n   \n# end\n");
        assertEquals("SHOVEL", table.nameOf(SHOVEL));
        assertNull(table.nameOf(SHOVEL + 1));
    }

    @Test
    void singleEntryKeepsItsCategoryAsCharacterType() {
        UnicodeNameTable table = UnicodeNameTable.parse("1FA8F So SHOVEL\n");
        assertEquals(Character.OTHER_SYMBOL, table.typeOf(SHOVEL));
        assertEquals(Character.UNASSIGNED, table.typeOf(SHOVEL + 1));
    }

    @Test
    void rangeEntryKeepsItsCategoryForEveryCodePoint() {
        UnicodeNameTable table = UnicodeNameTable.parse(CJK_RANGE);
        assertEquals(Character.OTHER_LETTER, table.typeOf(0x31350));
        assertEquals(Character.OTHER_LETTER, table.typeOf(0x33479));
        assertEquals(Character.UNASSIGNED, table.typeOf(0x3347A));
    }

    @Test
    void blockLinesGiveBlockNamesWithExactBoundaries() {
        UnicodeNameTable table = UnicodeNameTable.parse(
                "BLOCK 1FB00..1FBFF Symbols for Legacy Computing\n"
                        + "BLOCK 1FA80..1FAFF Pictographs Extended-A\n");
        assertEquals("Symbols for Legacy Computing", table.blockOf(0x1FB00));
        assertEquals("Symbols for Legacy Computing", table.blockOf(0x1FBFF));
        assertEquals("Pictographs Extended-A", table.blockOf(0x1FAFF));
        assertNull(table.blockOf(0x1FA7F));
        assertNull(table.blockOf(0x1FC00));
        assertNull(table.blockOf(0x0041));
    }

    @Test
    void blockLinesDoNotAffectNames() {
        UnicodeNameTable table = UnicodeNameTable.parse("BLOCK 1FB00..1FBFF Legacy\n");
        assertNull(table.nameOf(0x1FB00));
        assertEquals(Character.UNASSIGNED, table.typeOf(0x1FB00));
    }

    @Test
    void everyCategoryCodeIsAccepted() {
        String[] codes =
                {"Lu", "Ll", "Lt", "Lm", "Lo", "Mn", "Mc", "Me", "Nd", "Nl", "No", "Pc", "Pd", "Ps",
                        "Pe", "Pi", "Pf", "Po", "Sm", "Sc", "Sk", "So", "Zs", "Zl", "Zp", "Cc",
                        "Cf", "Cs", "Co", "Cn"};
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < codes.length; index++) {
            text.append(Integer.toHexString(0x1000 + index)).append(' ').append(codes[index])
                    .append(" N\n");
        }
        UnicodeNameTable table = UnicodeNameTable.parse(text.toString());
        assertEquals(Character.INITIAL_QUOTE_PUNCTUATION, table.typeOf(0x1000 + 15));
        assertEquals(Character.FINAL_QUOTE_PUNCTUATION, table.typeOf(0x1000 + 16));
        assertEquals(Character.UPPERCASE_LETTER, table.typeOf(0x1000));
        assertEquals(Character.PRIVATE_USE, table.typeOf(0x1000 + 28));
    }

    @Test
    void malformedLineNamesItsLineNumber() {
        assertRejectedAtLine(3, "# header\n1FA8F So SHOVEL\nZZZZ So BROKEN\n");
    }

    @Test
    void lineWithoutCategoryOrNameIsRejected() {
        assertRejectedAtLine(1, "1FA8F");
        assertRejectedAtLine(1, "1FA8F SHOVEL");
        assertRejectedAtLine(2, "# c\n1FA8F So");
    }

    @Test
    void unknownCategoryIsRejectedWithLineNumber() {
        assertRejectedAtLine(2, "1FA8E So A\n1FA8F Xx SHOVEL\n");
        assertRejectedAtLine(1, "1FA8F lu SHOVEL\n");
    }

    @Test
    void malformedBlockLinesAreRejectedWithLineNumber() {
        assertRejectedAtLine(2, "BLOCK 100..1FF A\nBLOCK 1FF..100 Backwards\n");
        assertRejectedAtLine(1, "BLOCK ZZ..100 Bad\n");
        assertRejectedAtLine(1, "BLOCK 100..1FF\n");
        assertRejectedAtLine(2, "\nBLOCK\n");
    }

    @Test
    void overlappingBlocksAreRejectedWithLineNumber() {
        assertRejectedAtLine(2, "BLOCK 100..200 A\nBLOCK 200..300 B\n");
    }

    @Test
    void duplicateCodePointIsRejected() {
        assertRejectedAtLine(2, "41 Lu A\n41 Lu B\n");
    }

    @Test
    void rangeGivesPrefixPlusFiveDigitHex() {
        UnicodeNameTable table = UnicodeNameTable.parse(CJK_RANGE);
        assertEquals("CJK UNIFIED IDEOGRAPH-31350", table.nameOf(0x31350));
        assertEquals("CJK UNIFIED IDEOGRAPH-33479", table.nameOf(0x33479));
        assertEquals("CJK UNIFIED IDEOGRAPH-32000", table.nameOf(0x32000));
    }

    @Test
    void rangeHexIsPaddedToFourDigitsAndUppercase() {
        UnicodeNameTable table = UnicodeNameTable.parse("3400..4DBF Lo CJK UNIFIED IDEOGRAPH-*\n");
        assertEquals("CJK UNIFIED IDEOGRAPH-3400", table.nameOf(0x3400));
        assertEquals("CJK UNIFIED IDEOGRAPH-4DBF", table.nameOf(0x4DBF));
        UnicodeNameTable low = UnicodeNameTable.parse("A..F Lo SMALL-*\n");
        assertEquals("SMALL-000A", low.nameOf(0xA));
    }

    @Test
    void codePointsOutsideRangesHaveNoName() {
        UnicodeNameTable table =
                UnicodeNameTable.parse(CJK_RANGE + "3D000..3FC3F Lo SMALL SEAL CHARACTER-*\n");
        assertNull(table.nameOf(0x3134F));
        assertNull(table.nameOf(0x3347A));
        assertEquals("SMALL SEAL CHARACTER-3D000", table.nameOf(0x3D000));
    }

    @Test
    void singleRangeEntryWorks() {
        UnicodeNameTable table =
                UnicodeNameTable.parse("18CFF..18CFF Lo KHITAN SMALL SCRIPT CHARACTER-*\n");
        assertEquals("KHITAN SMALL SCRIPT CHARACTER-18CFF", table.nameOf(0x18CFF));
    }

    @Test
    void rangesAndSinglesInAnyFileOrderAreFound() {
        UnicodeNameTable table =
                UnicodeNameTable.parse("3D000..3FC3F Lo B-*\n1FA8F So SHOVEL\n" + CJK_RANGE);
        assertEquals("CJK UNIFIED IDEOGRAPH-31350", table.nameOf(0x31350));
        assertEquals("B-3D000", table.nameOf(0x3D000));
        assertEquals("SHOVEL", table.nameOf(SHOVEL));
    }

    @Test
    void overlappingRangesAreRejectedWithLineNumber() {
        assertRejectedAtLine(2, "100..200 Lo A-*\n150..300 Lo B-*\n");
    }

    @Test
    void touchingRangesAreAllowed() {
        UnicodeNameTable table = UnicodeNameTable.parse("100..200 Lo A-*\n201..300 Lo B-*\n");
        assertEquals("A-0200", table.nameOf(0x200));
        assertEquals("B-0201", table.nameOf(0x201));
    }

    @Test
    void singleInsideRangeIsRejected() {
        assertRejectedAtLine(2, CJK_RANGE + "32000 Lo INSIDE\n");
        assertRejectedAtLine(2, "32000 Lo INSIDE\n" + CJK_RANGE);
    }

    @Test
    void malformedRangesAreRejected() {
        assertRejectedAtLine(1, "200..100 Lo A-*\n");
        assertRejectedAtLine(1, "ZZ..100 Lo A-*\n");
        assertRejectedAtLine(1, "100..200 Lo NO STAR\n");
        assertRejectedAtLine(1, "100..200 Lo A*B\n");
        assertRejectedAtLine(1, "41 Lu STAR*\n");
    }

    @Test
    void bundledFileLoadsAndHasShovel() {
        UnicodeNameTable table = UnicodeNameTable.loadBundled();
        assertEquals("SHOVEL", table.nameOf(SHOVEL));
        assertEquals(Character.OTHER_SYMBOL, table.typeOf(SHOVEL));
        assertEquals("Basic Latin", table.blockOf(0x41));
    }
}
