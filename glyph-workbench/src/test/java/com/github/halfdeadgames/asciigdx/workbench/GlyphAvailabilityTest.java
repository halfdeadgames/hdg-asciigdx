package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlyphAvailabilityTest {

    @Test
    void everyGlyphIncludesAnyCodePoint() {
        assertTrue(GlyphAvailability.everyGlyph().includes('A'));
        assertTrue(GlyphAvailability.everyGlyph().includes(0x2665));
    }

    @Test
    void listedAvailabilityIncludesOnlyListedCodePoints() {
        GlyphAvailability availability = GlyphAvailability.listedIn("A♥a\n");
        assertTrue(availability.includes('A'));
        assertTrue(availability.includes(0x2665));
        assertTrue(availability.includes('a'));
        assertFalse(availability.includes('B'));
    }
}
