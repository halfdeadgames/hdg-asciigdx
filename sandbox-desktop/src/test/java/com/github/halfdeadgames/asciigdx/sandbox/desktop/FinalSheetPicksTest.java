package com.github.halfdeadgames.asciigdx.sandbox.desktop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FinalSheetPicksTest {

    private static final int[] SHEET = {0x41, 0x42, 0x43, 0x20};
    private static final boolean[] BLANK = {false, false, false, true};

    @Test
    void parseReadsCodePointAndPickAndIgnoresOtherColumns() {
        Map<Integer, String> picks = FinalSheetPicks.parse("U+0041\tA\tmmpx\tperfect\t\tnote\r\nU+0042\tB\tnone\tneeds-hand-drawn\t\n\n");
        assertEquals(Map.of(0x41, "mmpx", 0x42, "none"), picks);
    }

    @Test
    void codePointsWithoutAPickFallBackToNearest() {
        String[] chosen = FinalSheetPicks.select(SHEET, BLANK, Map.of(0x41, "light"), Set.of());
        assertArrayEquals(new String[] {"light", "nearest", "nearest", "nearest"}, chosen);
    }

    @Test
    void blankGlyphsUseNearestEvenWithAPick() {
        String[] chosen = FinalSheetPicks.select(SHEET, BLANK, Map.of(0x20, "mmpx"), Set.of());
        assertEquals("nearest", chosen[3]);
    }

    @Test
    void everyKnownIdIsSelected() {
        Map<Integer, String> picks = Map.of(0x41, "scale2x", 0x42, "scale2x-sfx", 0x43, "hand");
        String[] chosen = FinalSheetPicks.select(SHEET, BLANK, picks, Set.of(0x43));
        assertArrayEquals(new String[] {"scale2x", "scale2x-sfx", "hand", "nearest"}, chosen);
    }

    @Test
    void nonePickFails() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> FinalSheetPicks.select(SHEET, BLANK, Map.of(0x41, "none"), Set.of()));
        assertTrue(error.getMessage().contains("pick is none: U+0041"));
    }

    @Test
    void unknownIdFails() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> FinalSheetPicks.select(SHEET, BLANK, Map.of(0x42, "bogus"), Set.of()));
        assertTrue(error.getMessage().contains("unknown pick id: U+0042 (bogus)"));
    }

    @Test
    void handPickWithoutArtFails() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> FinalSheetPicks.select(SHEET, BLANK, Map.of(0x43, "hand"), Set.of(0x41)));
        assertTrue(error.getMessage().contains("hand pick without hand art: U+0043"));
    }

    @Test
    void codePointNotInSheetFails() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> FinalSheetPicks.select(SHEET, BLANK, Map.of(0x1F600, "mmpx"), Set.of()));
        assertTrue(error.getMessage().contains("code point not in the sheet: U+1F600"));
    }
}
