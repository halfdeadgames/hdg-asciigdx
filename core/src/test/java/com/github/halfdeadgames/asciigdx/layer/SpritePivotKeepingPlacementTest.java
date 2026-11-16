package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import org.junit.jupiter.api.Test;

class SpritePivotKeepingPlacementTest {

    private static final float CELL_WIDTH = 8f;
    private static final float CELL_HEIGHT = 12f;
    private static final float TOLERANCE = 1e-3f;
    private static final int STRIDE = CellVertexLayout.FLOATS_PER_VERTEX;
    private static final int CORNERS = 4;

    private static final class FakeSource implements GlyphSource {
        @Override
        public int getCellWidth() {
            return (int) CELL_WIDTH;
        }

        @Override
        public int getCellHeight() {
            return (int) CELL_HEIGHT;
        }

        @Override
        public TextureRegion getGlyphRegion(int codePoint) {
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

    private final LayerStack stack = TestStacks.withRenderers((grid, source, shared) -> new LayerRenderer() {
        @Override
        public void draw(SpriteBatch batch, Affine2 layerTransform, ResolvedEffects effects,
                Array<GlyphSprite> sprites, boolean pixelSnapping) {
        }

        @Override
        public void setGlyphSource(GlyphSource glyphSource) {
        }

        @Override
        public void dispose() {
        }
    });
    private final Layer layer = stack.add(new TerminalGrid(6, 5), new FakeSource());
    private final GlyphSprite sprite = layer.addSprite('@', 0xFFFFFFFF, 20f, 15f);
    private final CellHandle handle = layer.cell(3, 2);

    private static float[] quadOf(SpriteTransform transform, float x, float y) {
        float[] vertices = new float[CellVertexLayout.FLOATS_PER_QUAD];
        SpriteQuadWriter.writeQuad(vertices, 0, transform, 0xFFFFFFFF, x, y, CELL_WIDTH, CELL_HEIGHT,
                0f, 0f, 1f, 1f);
        return vertices;
    }

    private static float[] spriteQuad(GlyphSprite sprite) {
        return quadOf(sprite, sprite.getX(), sprite.getY());
    }

    private static float[] handleQuad(CellHandle handle) {
        return quadOf(handle, handle.getColumn() * CELL_WIDTH + handle.getOffsetX(),
                handle.getRow() * CELL_HEIGHT + handle.getOffsetY());
    }

    private static void assertSameCorners(float[] before, float[] after) {
        for (int corner = 0; corner < CORNERS; corner++) {
            int index = corner * STRIDE + CellVertexLayout.POSITION_OFFSET;
            assertEquals(before[index], after[index], TOLERANCE, "x of corner " + corner);
            assertEquals(before[index + 1], after[index + 1], TOLERANCE, "y of corner " + corner);
        }
    }

    private void assertSpriteKeepsQuad() {
        float[] before = spriteQuad(sprite);

        sprite.setPivotKeepingPlacement(0f, 1f);

        assertSameCorners(before, spriteQuad(sprite));
    }

    private void assertHandleKeepsQuad() {
        float[] before = handleQuad(handle);

        handle.setPivotKeepingPlacement(0f, 1f);

        assertSameCorners(before, handleQuad(handle));
    }

    @Test
    void spriteRotationOnlyKeepsQuad() {
        sprite.setRotation(37f);

        assertSpriteKeepsQuad();
    }

    @Test
    void spriteNonUniformScaleOnlyKeepsQuad() {
        sprite.setScale(2.5f, 0.75f);

        assertSpriteKeepsQuad();
    }

    @Test
    void spriteMirrorKeepsQuad() {
        sprite.setScale(-1.5f, 1f);

        assertSpriteKeepsQuad();
    }

    @Test
    void spriteRotationAndScaleKeepQuad() {
        sprite.setRotation(-125f);
        sprite.setScale(-2f, 1.5f);

        assertSpriteKeepsQuad();
    }

    @Test
    void spriteQuarterTurnKeepsQuad() {
        sprite.setRotation(90f);
        sprite.setScale(2f, 3f);

        assertSpriteKeepsQuad();
    }

    @Test
    void spriteIdentityKeepsPosition() {
        sprite.setPivotKeepingPlacement(0f, 1f);

        assertEquals(20f, sprite.getX(), TOLERANCE);
        assertEquals(15f, sprite.getY(), TOLERANCE);
        assertEquals(0f, sprite.getPivotX(), TOLERANCE);
        assertEquals(1f, sprite.getPivotY(), TOLERANCE);
    }

    @Test
    void laterRotationHappensAboutNewPivot() {
        sprite.setPivotKeepingPlacement(0f, 1f);
        sprite.setRotation(90f);

        float[] quad = spriteQuad(sprite);

        // The bottom-left corner is the pivot and stays at the sprite's box corner (x 20, y 15 + 12 down,
        // stored negated because quad vertices are y-up).
        assertEquals(20f, quad[CellVertexLayout.POSITION_OFFSET], TOLERANCE);
        assertEquals(-27f, quad[CellVertexLayout.POSITION_OFFSET + 1], TOLERANCE);
    }

    @Test
    void removedSpriteRejectsPivotKeepingPlacement() {
        layer.removeSprite(sprite);

        assertThrows(IllegalStateException.class, () -> sprite.setPivotKeepingPlacement(0f, 1f));
    }

    @Test
    void handleRotationAndScaleKeepQuad() {
        handle.setOffset(3f, -2f);
        handle.setRotation(52f);
        handle.setScale(1.5f, -0.5f);

        assertHandleKeepsQuad();
    }

    @Test
    void handleRotationOnlyKeepsQuad() {
        handle.setRotation(-33f);

        assertHandleKeepsQuad();
    }

    @Test
    void handleWithDefaultMotionStaysUnlifted() {
        handle.setPivotKeepingPlacement(0f, 1f);

        assertFalse(handle.isLifted());
        assertEquals(0f, handle.getOffsetX());
        assertEquals(0f, handle.getOffsetY());
        assertEquals(0f, handle.getPivotX());
        assertEquals(1f, handle.getPivotY());
    }

    @Test
    void releasedHandleRejectsPivotKeepingPlacement() {
        layer.releaseIdleCellHandles();

        assertThrows(IllegalStateException.class, () -> handle.setPivotKeepingPlacement(0f, 1f));
    }

    @Test
    void helperMatchesHandWorkedQuarterTurn() {
        // Clockwise quarter turn, scale (2, 3), shift (4, 1): M = [0 -3; 2 0] in y-down,
        // M * shift = (-3, 8), so the position moves by -(shift - M * shift) = -(7, -7).
        assertEquals(10f - 7f, SpritePlacement.xKeepingPlacement(10f, 4f, 1f, 2f, 3f, 90f), TOLERANCE);
        assertEquals(20f + 7f, SpritePlacement.yKeepingPlacement(20f, 4f, 1f, 2f, 3f, 90f), TOLERANCE);
    }
}
