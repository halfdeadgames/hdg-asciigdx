package com.github.halfdeadgames.asciigdx.workbench;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class GlyphCloudTest {

    private static boolean[] blankSheet() {
        boolean[] blank = new boolean[64];
        for (int i = 0; i < blank.length; i++) {
            blank[i] = i % 3 == 0;
        }
        return blank;
    }

    @Test
    void placesTargetInCentreCell() {
        int[] cells = GlyphCloud.build(blankSheet(), 9, 5, 7, 42);
        assertEquals(45, cells.length);
        assertEquals(7, cells[2 * 9 + 4]);
    }

    @Test
    void usesOnlyNonBlankSlots() {
        boolean[] blank = blankSheet();
        for (int slot : GlyphCloud.build(blank, 9, 5, 7, 42)) {
            assertFalse(blank[slot]);
        }
    }

    @Test
    void sameSeedGivesSameCloud() {
        assertArrayEquals(
            GlyphCloud.build(blankSheet(), 9, 5, 7, 99), GlyphCloud.build(blankSheet(), 9, 5, 7, 99));
    }

    @Test
    void fillsWithTargetWhenEverythingIsBlank() {
        int[] cells = GlyphCloud.build(new boolean[] {true, true}, 3, 3, 1, 5);
        for (int slot : cells) {
            assertEquals(1, slot);
        }
    }
}
