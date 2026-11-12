package com.github.halfdeadgames.asciigdx.workbench.newitems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import java.util.List;
import org.junit.jupiter.api.Test;

class NewFontInputTest {

    private static final List<String> NONE = List.of();

    @Test
    void ordinaryNamesPass() {
        assertNull(NewFontInput.nameError("saiksistt", NONE));
        assertNull(NewFontInput.nameError("my-font-2", NONE));
        assertNull(NewFontInput.nameError("a", NONE));
    }

    @Test
    void lengthLimitsApply() {
        assertNotNull(NewFontInput.nameError("", NONE));
        assertNotNull(NewFontInput.nameError(null, NONE));
        assertNull(NewFontInput.nameError("a".repeat(32), NONE));
        assertNotNull(NewFontInput.nameError("a".repeat(33), NONE));
    }

    @Test
    void onlyLowercaseDigitsAndHyphenAreAllowed() {
        assertNotNull(NewFontInput.nameError("My-Font", NONE));
        assertNotNull(NewFontInput.nameError("my font", NONE));
        assertNotNull(NewFontInput.nameError("my_font", NONE));
        assertNotNull(NewFontInput.nameError("font.v2", NONE));
        assertNotNull(NewFontInput.nameError("café", NONE));
        assertNotNull(NewFontInput.nameError("a/b", NONE));
    }

    @Test
    void duplicatesAreRefused() {
        assertNotNull(NewFontInput.nameError("saiksistt", List.of("saiksistt")));
        assertNull(NewFontInput.nameError("saiksistt", List.of("other")));
    }

    @Test
    void duplicatesDifferingOnlyInCaseAreRefused() {
        assertNotNull(NewFontInput.nameError("saiksistt", List.of("Saiksistt")));
        assertNotNull(NewFontInput.nameError("saiksistt", List.of("SAIKSISTT")));
    }

    @Test
    void windowsReservedNamesAreRefused() {
        for (var reserved : List.of("con", "prn", "aux", "nul", "com1", "com9", "lpt1", "lpt9")) {
            assertNotNull(NewFontInput.nameError(reserved, NONE), reserved);
        }
        assertNotNull(NewFontInput.nameError("CON", NONE));
        assertNull(NewFontInput.nameError("com0", NONE));
        assertNull(NewFontInput.nameError("com10", NONE));
        assertNull(NewFontInput.nameError("console", NONE));
    }

    @Test
    void squareAndTallCellSizesUpToTheLimitAreAccepted() {
        assertNull(NewFontInput.sizeError(new CellSize(1, 1)));
        assertNull(NewFontInput.sizeError(new CellSize(128, 128)));
        assertNull(NewFontInput.sizeError(new CellSize(64, 128)));
        assertNull(NewFontInput.sizeError(new CellSize(16, 16)));
        assertNull(NewFontInput.sizeError(new CellSize(8, 16)));
        assertNull(NewFontInput.sizeError(new CellSize(5, 10)));
        assertNull(NewFontInput.sizeError(new CellSize(7, 7)));
    }

    @Test
    void otherShapesAreRefused() {
        assertEquals("Must be square or 2x tall", NewFontInput.sizeError(new CellSize(16, 8)));
        assertEquals("Must be square or 2x tall", NewFontInput.sizeError(new CellSize(8, 12)));
        assertEquals("Must be square or 2x tall", NewFontInput.sizeError(new CellSize(7, 13)));
        assertEquals("Must be square or 2x tall", NewFontInput.typedSizeError("7x13"));
    }

    @Test
    void cellSidesAboveTheLimitAreRefused() {
        assertEquals("Width and height go up to 128", NewFontInput.sizeError(new CellSize(129, 16)));
        assertEquals("Width and height go up to 128", NewFontInput.sizeError(new CellSize(16, 129)));
    }

    @Test
    void typedSizeAcceptsCapitalXAndSurroundingSpaces() {
        assertNull(NewFontInput.typedSizeError(" 16X32 "));
        assertEquals(new CellSize(16, 32), NewFontInput.typedSize(" 16X32 "));
        assertEquals(new CellSize(7, 14), NewFontInput.typedSize("7x14"));
    }

    @Test
    void typedSizeExplainsWhatIsWrong() {
        assertEquals("Type a size like 16x16", NewFontInput.typedSizeError(""));
        assertEquals("Type a size like 16x16", NewFontInput.typedSizeError("   "));
        assertEquals("Write the size as WxH, e.g. 16x16", NewFontInput.typedSizeError("16"));
        assertEquals("Write the size as WxH, e.g. 16x16", NewFontInput.typedSizeError("16x"));
        assertEquals("Write the size as WxH, e.g. 16x16", NewFontInput.typedSizeError("axb"));
        assertEquals("Width and height go up to 128", NewFontInput.typedSizeError("200x16"));
    }

    @Test
    void typedSizeRefusesWhatTheErrorCheckRefuses() {
        assertThrows(IllegalArgumentException.class, () -> NewFontInput.typedSize("200x16"));
        assertThrows(IllegalArgumentException.class, () -> NewFontInput.typedSize("axb"));
        assertThrows(IllegalArgumentException.class, () -> NewFontInput.typedSize(""));
    }

    @Test
    void zeroSidesAreNotASize() {
        assertEquals("Width and height start at 1", NewFontInput.typedSizeError("0x16"));
        assertEquals("Width and height start at 1", NewFontInput.typedSizeError("16x00"));
        assertEquals("Width and height go up to 128", NewFontInput.typedSizeError("12345x16"));
    }
}
