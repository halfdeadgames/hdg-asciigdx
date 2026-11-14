package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.Test;

class SpriteMeshTest {

    private static final int MAX = CellVertexLayout.MAX_QUADS_PER_MESH;

    @Test
    void firstGrowthStartsAtTheMinimum() {
        assertEquals(SpriteMesh.MIN_CAPACITY, SpriteMesh.grownCapacity(0, 1));
        assertEquals(SpriteMesh.MIN_CAPACITY, SpriteMesh.grownCapacity(0, SpriteMesh.MIN_CAPACITY));
    }

    @Test
    void growthDoublesUntilTheNeedFits() {
        assertEquals(32, SpriteMesh.grownCapacity(16, 17));
        assertEquals(128, SpriteMesh.grownCapacity(16, 100));
        assertEquals(64, SpriteMesh.grownCapacity(32, 33));
    }

    @Test
    void growthNeverShrinksOrGrowsWhenItAlreadyFits() {
        assertEquals(64, SpriteMesh.grownCapacity(64, 10));
        assertEquals(64, SpriteMesh.grownCapacity(64, 64));
    }

    @Test
    void growthIsCappedAtTheIndexLimit() {
        assertEquals(MAX, SpriteMesh.grownCapacity(8192, MAX));
        assertEquals(MAX, SpriteMesh.grownCapacity(16, MAX + 100));
        assertEquals(MAX, SpriteMesh.grownCapacity(MAX, MAX));
    }

    @Test
    void countsOnlyVisibleSprites() {
        Array<GlyphSprite> sprites = spritesWithColor(3, 0xFFFFFFFF);
        sprites.get(1).setVisible(false);
        assertEquals(2, SpriteMesh.countDrawn(sprites));
    }

    @Test
    void doesNotCountFullyTransparentSprites() {
        Array<GlyphSprite> sprites = spritesWithColor(3, 0xFFFFFFFF);
        sprites.get(1).setAlpha(0f);
        assertEquals(2, SpriteMesh.countDrawn(sprites));
    }

    @Test
    void countsNearlyTransparentSprites() {
        Array<GlyphSprite> sprites = spritesWithColor(1, 0xFFFFFFFF);
        sprites.get(0).setAlpha(0.01f);
        assertEquals(1, SpriteMesh.countDrawn(sprites));
    }

    private static Array<GlyphSprite> spritesWithColor(int count, int color) {
        Array<GlyphSprite> sprites = new Array<>();
        for (int i = 0; i < count; i++) {
            sprites.add(new GlyphSprite('a' + i, color));
        }
        return sprites;
    }
}
