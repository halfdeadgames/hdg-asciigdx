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
        Array<GlyphSprite> sprites = new Array<>();
        sprites.add(new GlyphSprite('a', 0));
        sprites.add(new GlyphSprite('b', 0));
        sprites.add(new GlyphSprite('c', 0));
        sprites.get(1).setVisible(false);
        assertEquals(2, SpriteMesh.countVisible(sprites));
    }
}
