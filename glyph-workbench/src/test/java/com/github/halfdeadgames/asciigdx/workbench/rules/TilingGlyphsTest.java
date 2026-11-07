package com.github.halfdeadgames.asciigdx.workbench.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TilingGlyphsTest {

    @Test
    void boxDrawingBlockIsTilingIncludingBothEdges() {
        assertFalse(TilingGlyphs.isTiling(0x24FF));
        assertTrue(TilingGlyphs.isTiling(0x2500));
        assertTrue(TilingGlyphs.isTiling(0x257F));
    }

    @Test
    void blockElementsBlockIsTilingIncludingBothEdges() {
        assertTrue(TilingGlyphs.isTiling(0x2580));
        assertTrue(TilingGlyphs.isTiling(0x259F));
        assertFalse(TilingGlyphs.isTiling(0x25A0));
    }

    @Test
    void lettersAreNotTiling() {
        assertFalse(TilingGlyphs.isTiling('A'));
    }
}
