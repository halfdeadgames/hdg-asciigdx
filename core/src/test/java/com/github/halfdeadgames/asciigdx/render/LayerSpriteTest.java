package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class LayerSpriteTest {

    private static final class FakeSource implements GlyphSource {
        @Override
        public int getCellWidth() {
            return 8;
        }

        @Override
        public int getCellHeight() {
            return 12;
        }

        @Override
        public TextureRegion glyph(int codePoint) {
            return null;
        }

        @Override
        public boolean hasGlyph(int codePoint) {
            return false;
        }

        @Override
        public void dispose() {
        }
    }

    private Array<GlyphSprite> lastSprites;
    private boolean lastSnapping;
    private int drawCount;

    private final ClipBackend backend = new ClipBackend() {
        @Override
        public void flush(SpriteBatch batch) {
        }

        @Override
        public void getCombinedMatrix(SpriteBatch batch, Matrix4 out) {
            out.idt().setToOrtho2D(0f, 0f, 200f, 100f);
        }

        @Override
        public void getViewport(int[] out) {
            out[2] = 200;
            out[3] = 100;
        }

        @Override
        public boolean push(Rectangle windowRect) {
            return true;
        }

        @Override
        public void pop() {
        }
    };

    private final LayerStack stack = new LayerStack((grid, source, shared) -> new LayerRenderer() {
        @Override
        public void draw(SpriteBatch batch, float x, float y, LayerEffects effects,
                Array<GlyphSprite> sprites, boolean pixelSnapping) {
            lastSprites = sprites;
            lastSnapping = pixelSnapping;
            drawCount++;
        }

        @Override
        public void setGlyphSource(GlyphSource glyphSource) {
        }

        @Override
        public void dispose() {
        }
    }, backend);

    private Layer addLayer() {
        return stack.add(new TerminalGrid(4, 3), new FakeSource());
    }

    @Test
    void addKeepsOrderCountAndInitialValues() {
        Layer layer = addLayer();
        GlyphSprite first = layer.addSprite('a', 0x11223344, 5f, 6f);
        GlyphSprite second = layer.addSprite('b', 0, 0f, 0f);

        assertEquals(2, layer.getSpriteCount());
        assertSame(first, layer.getSprite(0));
        assertSame(second, layer.getSprite(1));
        assertEquals('a', first.getCodePoint());
        assertEquals(0x11223344, first.getColor());
        assertEquals(5f, first.getX());
        assertEquals(6f, first.getY());
    }

    @Test
    void getSpriteRejectsBadIndex() {
        Layer layer = addLayer();
        layer.addSprite('a', 0, 0f, 0f);
        assertThrows(IndexOutOfBoundsException.class, () -> layer.getSprite(1));
        assertThrows(IndexOutOfBoundsException.class, () -> layer.getSprite(-1));
    }

    @Test
    void removeKeepsRemainingOrder() {
        Layer layer = addLayer();
        GlyphSprite first = layer.addSprite('a', 0, 0f, 0f);
        GlyphSprite middle = layer.addSprite('b', 0, 0f, 0f);
        GlyphSprite last = layer.addSprite('c', 0, 0f, 0f);

        layer.removeSprite(middle);

        assertEquals(2, layer.getSpriteCount());
        assertSame(first, layer.getSprite(0));
        assertSame(last, layer.getSprite(1));
    }

    @Test
    void removeRejectsNullDoubleRemoveAndForeignSprites() {
        Layer layer = addLayer();
        Layer other = addLayer();
        GlyphSprite sprite = layer.addSprite('a', 0, 0f, 0f);
        GlyphSprite foreign = other.addSprite('b', 0, 0f, 0f);

        assertThrows(IllegalArgumentException.class, () -> layer.removeSprite(null));
        assertThrows(IllegalArgumentException.class, () -> layer.removeSprite(foreign));
        layer.removeSprite(sprite);
        assertThrows(IllegalStateException.class, () -> layer.removeSprite(sprite));
        assertEquals(1, other.getSpriteCount());
    }

    @Test
    void removedSpriteKeepsGettersButRejectsEveryMutator() {
        Layer layer = addLayer();
        GlyphSprite sprite = layer.addSprite('a', 0x11223344, 5f, 6f);
        layer.removeSprite(sprite);

        assertEquals('a', sprite.getCodePoint());
        assertEquals(5f, sprite.getX());
        assertTrue(sprite.isVisible());
        assertThrows(IllegalStateException.class, () -> sprite.setCodePoint('b'));
        assertThrows(IllegalStateException.class, () -> sprite.setColor(1));
        assertThrows(IllegalStateException.class, () -> sprite.setAlpha(0.5f));
        assertThrows(IllegalStateException.class, () -> sprite.setPosition(1f, 1f));
        assertThrows(IllegalStateException.class, () -> sprite.moveBy(1f, 1f));
        assertThrows(IllegalStateException.class, () -> sprite.setRotation(1f));
        assertThrows(IllegalStateException.class, () -> sprite.rotateBy(1f));
        assertThrows(IllegalStateException.class, () -> sprite.setScale(2f));
        assertThrows(IllegalStateException.class, () -> sprite.setScale(2f, 3f));
        assertThrows(IllegalStateException.class, () -> sprite.setPivot(0f, 0f));
        assertThrows(IllegalStateException.class, () -> sprite.setVisible(false));
        assertEquals('a', sprite.getCodePoint());
    }

    @Test
    void layerRemovalDetachesItsSprites() {
        Layer layer = addLayer();
        GlyphSprite sprite = layer.addSprite('a', 0, 0f, 0f);

        layer.remove();

        assertEquals(0, layer.getSpriteCount());
        assertThrows(IllegalStateException.class, () -> sprite.setVisible(false));
        assertThrows(IllegalStateException.class, () -> layer.addSprite('b', 0, 0f, 0f));
        assertThrows(IllegalStateException.class, () -> layer.removeSprite(sprite));
    }

    @Test
    void stackDisposeDetachesSprites() {
        GlyphSprite sprite = addLayer().addSprite('a', 0, 0f, 0f);

        stack.dispose();

        assertThrows(IllegalStateException.class, () -> sprite.setPosition(1f, 1f));
    }

    @Test
    void addingPastTheLimitThrows() {
        Layer layer = addLayer();
        for (int i = 0; i < CellVertexLayout.MAX_QUADS_PER_MESH; i++) {
            layer.addSprite('a', 0, 0f, 0f);
        }

        assertThrows(IllegalStateException.class, () -> layer.addSprite('a', 0, 0f, 0f));
        assertEquals(CellVertexLayout.MAX_QUADS_PER_MESH, layer.getSpriteCount());
    }

    @Test
    void stackPassesTheLayersSpritesAndSnappingFlag() {
        Layer layer = addLayer();
        layer.addSprite('a', 0, 0f, 0f);

        stack.draw(null, 0f, 0f);
        assertSame(layer.getSprites(), lastSprites);
        assertTrue(lastSnapping);

        stack.setPixelSnapping(false);
        stack.draw(null, 0f, 0f);
        assertFalse(lastSnapping);
        assertEquals(2, drawCount);
    }

    @Test
    void clippedLayerAlsoGetsItsSprites() {
        Layer layer = addLayer();
        layer.addSprite('a', 0, 0f, 0f);
        layer.setClip(0f, 0f, 10f, 10f);

        stack.draw(null, 0f, 100f);

        assertSame(layer.getSprites(), lastSprites);
        assertTrue(lastSnapping);
        assertEquals(1, drawCount);
    }
}
