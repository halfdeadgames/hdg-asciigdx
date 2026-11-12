package com.github.halfdeadgames.asciigdx.workbench.unicodenames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UnicodeNamesGeneratorTest {

    private static final String NAMES = """
        # DerivedName-18.0.0.txt
        # comment
        0041          ; LATIN CAPITAL LETTER A
        1FA8F         ; SHOVEL
        10000         ; OLD THIRTEEN
        10001         ; NEW FOURTEEN
        3400..4DBF    ; CJK UNIFIED IDEOGRAPH-*
        18CFF         ; KHITAN SMALL SCRIPT CHARACTER-*
        """;

    private static final String AGES = """
        # DerivedAge-18.0.0.txt
        0000..001F    ; 1.1 #  [32] <control-0000>..<control-001F>
        0041          ; 1.1 # LATIN CAPITAL LETTER A
        3400..4DBF    ; 3.0 #  [6592] CJK
        10000         ; 13.0 # OLD
        10001         ; 14.0 # NEW
        18CFF         ; 13.0 # KHITAN
        1FA8F         ; 16.0 # SHOVEL
        """;

    private static final String CATEGORIES = """
        # DerivedGeneralCategory-18.0.0.txt
        0041..005A    ; Lu #  [26] LATIN CAPITAL LETTER A..LATIN CAPITAL LETTER Z
        3400..4DBF    ; Lo # [6592] CJK UNIFIED IDEOGRAPH-3400..CJK UNIFIED IDEOGRAPH-4DBF
        10000..10001  ; Lo #   [2] LINEAR B SYLLABLE B008 A..LINEAR B SYLLABLE B038 E
        18CFF         ; Lo # KHITAN SMALL SCRIPT CHARACTER-18CFF
        1FA8F         ; So # SHOVEL
        """;

    private static final String BLOCKS = """
        # Blocks-18.0.0.txt
        # @missing: 0000..10FFFF; No_Block
        1FA80..1FAFF; Symbols and Pictographs Extended-A
        0000..007F; Basic Latin
        """;

    private static String generate() {
        return UnicodeNamesGenerator.generate(NAMES, AGES, CATEGORIES, BLOCKS, "# header\n", "13.0");
    }

    @Test
    void keepsOnlyNamesNewerThanTheCutoff() {
        String output = generate();
        assertTrue(output.contains("\n10001 Lo NEW FOURTEEN\n"));
        assertTrue(output.contains("\n1FA8F So SHOVEL\n"));
        assertFalse(output.contains("OLD THIRTEEN"));
        assertFalse(output.contains("LATIN CAPITAL LETTER A"));
    }

    @Test
    void keepsAllPatternRangesWhateverTheirAge() {
        String output = generate();
        assertTrue(output.contains("\n3400..4DBF Lo CJK UNIFIED IDEOGRAPH-*\n"));
        assertTrue(output.contains("\n18CFF..18CFF Lo KHITAN SMALL SCRIPT CHARACTER-*\n"));
    }

    @Test
    void writesEveryBlockInCodePointOrderAfterTheRanges() {
        String output = generate();
        assertTrue(output.contains("\nBLOCK 0000..007F Basic Latin\n"));
        assertTrue(output.contains("\nBLOCK 1FA80..1FAFF Symbols and Pictographs Extended-A\n"));
        assertTrue(output.indexOf("BLOCK 0000..007F") < output.indexOf("BLOCK 1FA80..1FAFF"));
        assertTrue(output.indexOf("18CFF..") < output.indexOf("BLOCK 0000"));
    }

    @Test
    void singlesComeBeforeRangesAndAreSorted() {
        String output = generate();
        assertTrue(output.indexOf("10001 ") < output.indexOf("1FA8F "));
        assertTrue(output.indexOf("1FA8F ") < output.indexOf("3400.."));
        assertTrue(output.indexOf("3400..") < output.indexOf("18CFF.."));
    }

    @Test
    void sortsUnorderedInput() {
        String names = "# DerivedName-1.0.0.txt\n1FA8F ; SHOVEL\n10001 ; ALPHA\n";
        String ages = "10001 ; 14.0 # A\n1FA8F ; 16.0 # S\n";
        String categories = "10001 ; Lo\n1FA8F ; So\n";
        String output = UnicodeNamesGenerator.generate(names, ages, categories, "", "# h\n", "13.0");
        assertTrue(output.indexOf("10001 Lo ALPHA") < output.indexOf("1FA8F So SHOVEL"));
    }

    @Test
    void startsWithHeaderAndEndsWithNewlineOnly() {
        String output = generate();
        assertTrue(output.startsWith("# header\n\n"));
        assertTrue(output.endsWith("\n"));
        assertFalse(output.contains("\r"));
    }

    @Test
    void outputIsDeterministicAndReadableByTheParser() {
        assertEquals(generate(), generate());
        UnicodeNameTable table = UnicodeNameTable.parse(generate());
        assertEquals("SHOVEL", table.nameOf(0x1FA8F));
        assertEquals(Character.OTHER_SYMBOL, table.typeOf(0x1FA8F));
        assertEquals("CJK UNIFIED IDEOGRAPH-4DBF", table.nameOf(0x4DBF));
        assertEquals(Character.OTHER_LETTER, table.typeOf(0x4DBF));
        assertEquals("KHITAN SMALL SCRIPT CHARACTER-18CFF", table.nameOf(0x18CFF));
        assertEquals("Symbols and Pictographs Extended-A", table.blockOf(0x1FA8F));
        assertEquals("Basic Latin", table.blockOf(0x41));
    }

    @Test
    void headerWithoutTrailingNewlineStillSeparates() {
        String output = UnicodeNamesGenerator.generate(NAMES, AGES, CATEGORIES, BLOCKS, "# header", "13.0");
        assertTrue(output.startsWith("# header\n\n"));
    }

    @Test
    void patternRangeWithTwoCategoriesIsSplitAndNoted() {
        String names = "3400..3403 ; CJK-*\n";
        String ages = "3400..3403 ; 3.0\n";
        String categories = "3400..3401 ; Lo\n3402..3403 ; Nd\n";
        String output = UnicodeNamesGenerator.generate(names, ages, categories, "", "# h\n", "13.0");
        assertTrue(output.contains("\n# Split by category: 3400..3403 CJK-*\n3400..3401 Lo CJK-*\n"
            + "3402..3403 Nd CJK-*\n"));
        UnicodeNameTable table = UnicodeNameTable.parse(output);
        assertEquals(Character.OTHER_LETTER, table.typeOf(0x3401));
        assertEquals(Character.DECIMAL_DIGIT_NUMBER, table.typeOf(0x3402));
        assertEquals("CJK-3403", table.nameOf(0x3403));
    }

    @Test
    void adjacentCategoryEntriesWithTheSameCodeStayOneLine() {
        String names = "3400..3403 ; CJK-*\n";
        String categories = "3400..3401 ; Lo\n3402..3403 ; Lo\n";
        String output = UnicodeNamesGenerator.generate(names, "3400..3403 ; 3.0\n", categories, "", "# h\n", "13.0");
        assertTrue(output.contains("\n3400..3403 Lo CJK-*\n"));
        assertFalse(output.contains("Split"));
    }

    @Test
    void missingCategoryFailsLoudly() {
        assertThrows(IllegalArgumentException.class, () -> UnicodeNamesGenerator.generate(
            "1FA8F ; SHOVEL\n", "1FA8F ; 16.0\n", "0041 ; Lu\n", "", "#\n", "13.0"));
        IllegalArgumentException gap = assertThrows(IllegalArgumentException.class,
            () -> UnicodeNamesGenerator.generate("3400..3403 ; CJK-*\n", "3400..3403 ; 3.0\n",
                "3400..3401 ; Lo\n", "", "#\n", "13.0"));
        assertTrue(gap.getMessage().contains("3402"), gap.getMessage());
    }

    @Test
    void unknownCategoryCodeFailsLoudly() {
        assertThrows(IllegalArgumentException.class, () -> UnicodeNamesGenerator.generate(
            "1FA8F ; SHOVEL\n", "1FA8F ; 16.0\n", "1FA8F ; Xx\n", "", "#\n", "13.0"));
    }

    @Test
    void missingAgeFailsLoudly() {
        assertThrows(IllegalArgumentException.class, () -> UnicodeNamesGenerator.generate(
            "1FA8F ; SHOVEL\n", "0041 ; 1.1\n", "1FA8F ; So\n", "", "#\n", "13.0"));
    }

    @Test
    void rangeWithoutPatternFailsLoudly() {
        assertThrows(IllegalArgumentException.class, () -> UnicodeNamesGenerator.generate(
            "0041..0042 ; PLAIN\n", "0041..0042 ; 1.1\n", "0041..0042 ; Lu\n", "", "#\n", "13.0"));
    }

    @Test
    void badBlockLineFailsLoudly() {
        assertThrows(IllegalArgumentException.class, () -> UnicodeNamesGenerator.generate(
            NAMES, AGES, CATEGORIES, "ZZZZ; Broken\n", "#\n", "13.0"));
    }

    @Test
    void headerNamesVersionCutoffFilesAndLicense() {
        String version = UnicodeNamesHeader.readVersion(NAMES);
        assertEquals("18.0.0", version);
        String header = UnicodeNamesHeader.build(version, "13.0", "UNICODE LICENSE V3\n\nCopyright\n");
        assertTrue(header.contains("Unicode Character Database 18.0.0"));
        assertTrue(header.contains("newer than Unicode 13.0"));
        assertTrue(header.contains("updateUnicodeNames"));
        assertTrue(header.contains("https://www.unicode.org/Public/UCD/latest/ucd/"));
        for (String file : new String[] {"DerivedName.txt", "DerivedGeneralCategory.txt", "DerivedAge.txt",
            "Blocks.txt", "license.txt"}) {
            assertTrue(header.contains(file), file);
        }
        assertTrue(header.contains("'<HEX> <CATEGORY> <NAME>'"));
        assertTrue(header.contains("'<HEX>..<HEX> <CATEGORY> <PREFIX>*'"));
        assertTrue(header.contains("'BLOCK <HEX>..<HEX> <NAME>'"));
        assertTrue(header.contains("# UNICODE LICENSE V3\n#\n# Copyright\n"));
        for (String line : header.split("\n")) {
            assertTrue(line.startsWith("#"), line);
        }
    }

    @Test
    void headerWithoutVersionLineIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> UnicodeNamesHeader.readVersion("0041 ; A\n"));
    }
}
