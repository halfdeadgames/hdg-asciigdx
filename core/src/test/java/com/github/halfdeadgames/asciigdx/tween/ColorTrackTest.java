package com.github.halfdeadgames.asciigdx.tween;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Interpolation;
import com.github.halfdeadgames.asciigdx.color.CellColor;
import org.junit.jupiter.api.Test;

class ColorTrackTest {

    private static final int FROM = 0x203040FF;
    private static final int TO = 0xE0D0C0FF;

    private final ColorTrack track = new ColorTrack(new Tween(1f, 2f, 0f, 1f), FROM, TO);

    @Test
    void colorBeforeStartIsFromExactly() {
        assertEquals(FROM, track.colorAt(0f));
    }

    @Test
    void colorAfterEndIsToExactly() {
        assertEquals(TO, track.colorAt(10f));
    }

    @Test
    void colorAtMidpointIsTheOklabMix() {
        assertEquals(CellColor.mixOklab(FROM, TO, 0.5f), track.colorAt(2f));
    }

    @Test
    void flashSequenceReturnsToFrom() {
        FloatTrack flash = TweenSequence.startingAt(0f, 0f).to(1f, 1f).to(0f, 1f).build();
        ColorTrack flashing = new ColorTrack(flash, FROM, TO);
        assertEquals(FROM, flashing.colorAt(0f));
        assertEquals(TO, flashing.colorAt(1f));
        assertEquals(FROM, flashing.colorAt(2f));
    }

    @Test
    void overshootingAmountIsClampedToTo() {
        Tween overshoot = new Tween(0f, 1f, 0f, 1f, Interpolation.swingOut);
        ColorTrack overshooting = new ColorTrack(overshoot, FROM, TO);
        float overshootTime = 0.8f;
        assertTrue(overshoot.valueAt(overshootTime) > 1f);
        assertEquals(TO, overshooting.colorAt(overshootTime));
    }

    @Test
    void alphaIsInterpolated() {
        int transparent = 0x10203000;
        int opaque = 0x102030FF;
        ColorTrack fade = new ColorTrack(new Tween(0f, 2f, 0f, 1f), transparent, opaque);
        assertEquals(CellColor.mixOklab(transparent, opaque, 0.5f), fade.colorAt(1f));
    }

    @Test
    void startAndEndDelegateToTheAmountTrack() {
        assertEquals(1f, track.start(), 0f);
        assertEquals(3f, track.end(), 0f);
    }

    @Test
    void gettersReturnTheEndColors() {
        assertEquals(FROM, track.fromRgba());
        assertEquals(TO, track.toRgba());
    }

    @Test
    void nullAmountThrows() {
        assertThrows(IllegalArgumentException.class, () -> new ColorTrack(null, FROM, TO));
    }
}
