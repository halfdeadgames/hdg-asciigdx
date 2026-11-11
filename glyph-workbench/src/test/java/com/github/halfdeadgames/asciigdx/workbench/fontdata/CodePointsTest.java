package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CodePointsTest {

    @Test
    void readsBothPrefixCasesAndHexCases() {
        assertEquals(0x41, CodePoints.readUPlus("U+0041"));
        assertEquals(0x41, CodePoints.readUPlus("u+0041"));
        assertEquals(0xE9, CodePoints.readUPlus("U+e9"));
        assertEquals(0xAB, CodePoints.readUPlus("U+aB"));
    }

    @Test
    void acceptsOneToEightDigits() {
        assertEquals(0xA, CodePoints.readUPlus("U+A"));
        assertEquals(0x10FFFF, CodePoints.readUPlus("U+10FFFF"));
        assertEquals(0x1234, CodePoints.readUPlus("U+001234"));
        assertEquals(0x41, CodePoints.readUPlus("U+0000041"));
        assertEquals(0x41, CodePoints.readUPlus("U+00000041"));
    }

    @Test
    void valuesAboveUnicodeComeBackAsOutOfUnicode() {
        assertEquals(CodePoints.OUT_OF_UNICODE, CodePoints.readUPlus("U+110000"));
        assertEquals(CodePoints.OUT_OF_UNICODE, CodePoints.readUPlus("U+12345678"));
        assertEquals(CodePoints.OUT_OF_UNICODE, CodePoints.readUPlus("U+FFFFFFFF"));
        assertFalse(Character.isValidCodePoint(CodePoints.OUT_OF_UNICODE));
    }

    @Test
    void rejectsOtherForms() {
        for (String bad : List.of("", "U", "U+", "u+", "+0041", "0041", "U0041", "U+000000041",
            "U+00G1", "U+ 41", "U+41 ", " U+41", "U+-1", "V+0041", "U+０041", "U+0041..U+0042")) {
            assertEquals(CodePoints.NOT_A_CODE, CodePoints.readUPlus(bad), bad);
        }
    }

    @Test
    void readsBareHexDigits() {
        assertEquals(0x41, CodePoints.readHexDigits("41"));
        assertEquals(0x10FFFF, CodePoints.readHexDigits("10ffff"));
        assertEquals(0x41, CodePoints.readHexDigits("00000041"));
        for (String bad : List.of("", "123456789", "4G", "U+41", " 41")) {
            assertEquals(CodePoints.NOT_A_CODE, CodePoints.readHexDigits(bad), bad);
        }
    }

    @Test
    void ordinaryCodePointsAreUsable() {
        assertTrue(CodePoints.isUsableAsGlyph('A'));
        assertTrue(CodePoints.isUsableAsGlyph(0x10FFFF));
        assertTrue(CodePoints.isUsableAsGlyph(0x1F600));
    }

    @Test
    void invalidSurrogateAndFillerAreNotUsable() {
        assertFalse(CodePoints.isUsableAsGlyph(-1));
        assertFalse(CodePoints.isUsableAsGlyph(0x110000));
        assertFalse(CodePoints.isUsableAsGlyph(0xD800));
        assertFalse(CodePoints.isUsableAsGlyph(0xDFFF));
        assertFalse(CodePoints.isUsableAsGlyph(SheetOrder.FILLER));
    }
}
