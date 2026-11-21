package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.text.ColorNames;
import org.junit.jupiter.api.Test;

class SkinColorNamesTest {

    private static final Skin SKIN = Skin.fromJson("""
            { "colors": { "navy": "#203070", "clear": "#00000000" } }
            """);

    @Test
    void findsEachColorOfTheColorsSection() {
        ColorNames names = SKIN.getColorNames();

        assertEquals(0x203070FF, names.findRgba("navy").getAsInt());
        assertEquals(0x00000000, names.findRgba("clear").getAsInt());
    }

    @Test
    void unknownNameIsEmptyAndMatchingIsExact() {
        ColorNames names = SKIN.getColorNames();

        assertFalse(names.findRgba("missing").isPresent());
        assertFalse(names.findRgba("Navy").isPresent());
        assertFalse(names.findRgba("").isPresent());
    }

    @Test
    void sameViewEveryCallAndEmptySkinKnowsNothing() {
        assertSame(SKIN.getColorNames(), SKIN.getColorNames());
        assertTrue(Skin.fromJson("{}").getColorNames().findRgba("navy").isEmpty());
    }
}
