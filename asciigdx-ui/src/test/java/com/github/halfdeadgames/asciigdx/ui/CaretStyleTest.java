package com.github.halfdeadgames.asciigdx.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CaretStyleTest {

    @Test
    void unsetPartsThrowWhenRead() {
        CaretStyle none = CaretStyle.none();

        assertFalse(none.hasForeground());
        assertFalse(none.hasGlyph());
        assertThrows(IllegalStateException.class, none::getForegroundRgba);
        assertThrows(IllegalStateException.class, none::getGlyphCodePoint);
    }

    @Test
    void setPartsAreReadable() {
        CaretStyle caret = CaretStyle.none().withForegroundRgba(0x11223344).withGlyph('_')
                .withBlinkSeconds(0.25f);

        assertTrue(caret.hasForeground() && caret.hasGlyph());
        assertEquals(0x11223344, caret.getForegroundRgba());
        assertEquals('_', caret.getGlyphCodePoint());
        assertEquals(0.25f, caret.getBlinkSeconds());
    }

    @Test
    void equalPartsGiveEqualStylesAndToStringWithoutBackground() {
        CaretStyle first = CaretStyle.none().withGlyph('_').withForegroundRgba(1);
        CaretStyle second = CaretStyle.none().withGlyph('_').withForegroundRgba(1);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertFalse(first.toString().contains("background"));
    }

    @Test
    void invalidValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> CaretStyle.none().withBlinkSeconds(-1f));
        assertThrows(IllegalArgumentException.class,
                () -> CaretStyle.none().withBlinkSeconds(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> CaretStyle.none().withGlyph(-1));
    }
}
