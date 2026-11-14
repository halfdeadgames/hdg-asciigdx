package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlyphSpriteTest {

    private static final int COLOR = 0x11223380;

    private static GlyphSprite newSprite() {
        return new GlyphSprite('@', COLOR);
    }

    @Test
    void defaultsMatchTheSpec() {
        GlyphSprite sprite = newSprite();
        assertEquals('@', sprite.getCodePoint());
        assertEquals(COLOR, sprite.getColor());
        assertEquals(0f, sprite.getX());
        assertEquals(0f, sprite.getY());
        assertEquals(0f, sprite.getRotation());
        assertEquals(1f, sprite.getScaleX());
        assertEquals(1f, sprite.getScaleY());
        assertEquals(0.5f, sprite.getPivotX());
        assertEquals(0.5f, sprite.getPivotY());
        assertTrue(sprite.isVisible());
    }

    @Test
    void positionSetsAndMoves() {
        GlyphSprite sprite = newSprite();
        sprite.setPosition(10f, 20f);
        sprite.moveBy(-3f, 5f);
        assertEquals(7f, sprite.getX());
        assertEquals(25f, sprite.getY());
    }

    @Test
    void rotationSetsAndAdds() {
        GlyphSprite sprite = newSprite();
        sprite.setRotation(30f);
        sprite.rotateBy(15f);
        assertEquals(45f, sprite.getRotation());
    }

    @Test
    void uniformScaleSetsBothAxes() {
        GlyphSprite sprite = newSprite();
        sprite.setScale(2f);
        assertEquals(2f, sprite.getScaleX());
        assertEquals(2f, sprite.getScaleY());
        sprite.setScale(-1f, 3f);
        assertEquals(-1f, sprite.getScaleX());
        assertEquals(3f, sprite.getScaleY());
    }

    @Test
    void pivotSetsBothFractions() {
        GlyphSprite sprite = newSprite();
        sprite.setPivot(0f, 1f);
        assertEquals(0f, sprite.getPivotX());
        assertEquals(1f, sprite.getPivotY());
    }

    @Test
    void alphaIsClampedRoundedAndKeepsRgb() {
        GlyphSprite sprite = newSprite();
        sprite.setAlpha(2f);
        assertEquals(0x112233FF, sprite.getColor());
        assertEquals(1f, sprite.getAlpha());
        sprite.setAlpha(-1f);
        assertEquals(0x11223300, sprite.getColor());
        assertEquals(0f, sprite.getAlpha());
        sprite.setAlpha(0.5f);
        assertEquals(0x11223380, sprite.getColor());
        sprite.setAlpha(0.2f);
        assertEquals(0x11223333, sprite.getColor());
    }

    @Test
    void alphaReadsBackAsFraction() {
        GlyphSprite sprite = new GlyphSprite('x', 0xFFFFFF00 | 51);
        assertEquals(0.2f, sprite.getAlpha(), 1e-6f);
    }

    @Test
    void isRemovedIsFalseUntilDetached() {
        GlyphSprite sprite = newSprite();
        assertFalse(sprite.isRemoved());
        sprite.detach();
        assertTrue(sprite.isRemoved());
    }

    @Test
    void visibilityToggles() {
        GlyphSprite sprite = newSprite();
        sprite.setVisible(false);
        assertFalse(sprite.isVisible());
    }
}
