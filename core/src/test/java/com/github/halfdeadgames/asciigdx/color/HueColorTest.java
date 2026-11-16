package com.github.halfdeadgames.asciigdx.color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class HueColorTest {

    @Test
    void primaryAndSecondaryHuesLandOnTheirColors() {
        assertEquals(0xFF0000FF, HueColor.fromHue(0f));
        assertEquals(0xFFFF00FF, HueColor.fromHue(1f / 6f));
        assertEquals(0x00FF00FF, HueColor.fromHue(1f / 3f));
        assertEquals(0x00FFFFFF, HueColor.fromHue(0.5f));
        assertEquals(0x0000FFFF, HueColor.fromHue(2f / 3f));
        assertEquals(0xFF00FFFF, HueColor.fromHue(5f / 6f));
    }

    @Test
    void valuesOutsideZeroToOneWrap() {
        assertEquals(HueColor.fromHue(0.25f), HueColor.fromHue(1.25f));
        assertEquals(HueColor.fromHue(0.75f), HueColor.fromHue(-0.25f));
    }

    @Test
    void justBelowOneStaysAValidColor() {
        assertEquals(0xFF0000FF, HueColor.fromHue(0.9999999f));
    }

    @Test
    void nonFiniteHueThrows() {
        assertThrows(IllegalArgumentException.class, () -> HueColor.fromHue(Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> HueColor.fromHue(Float.POSITIVE_INFINITY));
    }
}
