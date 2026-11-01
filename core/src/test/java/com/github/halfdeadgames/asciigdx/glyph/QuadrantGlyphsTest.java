package com.github.halfdeadgames.asciigdx.glyph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.glyph.QuadrantGlyphs.Shade;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QuadrantGlyphsTest {

    @Test
    void solidUsesUnicodeQuadrantBlocks() {
        assertEquals(0x2598, QuadrantGlyphs.codePoint(Shade.SOLID, QuadrantGlyphs.TOP_LEFT));
        assertEquals(0x2580, QuadrantGlyphs.codePoint(Shade.SOLID, QuadrantGlyphs.TOP_LEFT | QuadrantGlyphs.TOP_RIGHT));
        assertEquals(0x258C, QuadrantGlyphs.codePoint(Shade.SOLID, QuadrantGlyphs.TOP_LEFT | QuadrantGlyphs.BOTTOM_LEFT));
        assertEquals(0x259A, QuadrantGlyphs.codePoint(Shade.SOLID, QuadrantGlyphs.TOP_LEFT | QuadrantGlyphs.BOTTOM_RIGHT));
        assertEquals(0x2588, QuadrantGlyphs.codePoint(Shade.SOLID, QuadrantGlyphs.ALL));
    }

    @Test
    void patternedShadesFollowThePrivateUseLayout() {
        assertEquals(0xE001, QuadrantGlyphs.codePoint(Shade.BAYER_75, 1));
        assertEquals(0xE01F, QuadrantGlyphs.codePoint(Shade.BAYER_50, 15));
        assertEquals(0xE08F, QuadrantGlyphs.codePoint(Shade.FLIPPED_HATCH_25, 15));
    }

    @Test
    void emptyMaskIsSpaceForEveryShade() {
        for (Shade shade : Shade.values()) {
            assertEquals(' ', QuadrantGlyphs.codePoint(shade, 0));
        }
    }

    @Test
    void everyNonEmptyGlyphHasItsOwnCodePoint() {
        Set<Integer> seen = new HashSet<>();
        for (Shade shade : Shade.values()) {
            for (int mask = 1; mask <= QuadrantGlyphs.ALL; mask++) {
                assertTrue(seen.add(QuadrantGlyphs.codePoint(shade, mask)), shade + " " + mask);
            }
        }
        assertEquals(150, seen.size());
    }

    @Test
    void maskOutOfRangeThrows() {
        assertThrows(IllegalArgumentException.class, () -> QuadrantGlyphs.codePoint(Shade.SOLID, 16));
        assertThrows(IllegalArgumentException.class, () -> QuadrantGlyphs.codePoint(Shade.SOLID, -1));
    }
}
