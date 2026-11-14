package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tommyettinger.colorful.rgb.ColorTools;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SpriteQuadWriterTest {

    private static final float CELL_WIDTH = 8f;
    private static final float CELL_HEIGHT = 12f;
    private static final int COLOR = 0xFF8040C0;
    private static final float U = 0.125f;
    private static final float V = 0.25f;
    private static final float U2 = 0.5f;
    private static final float V2 = 0.75f;
    private static final int STRIDE = CellVertexLayout.FLOATS_PER_VERTEX;
    private static final int QUAD = CellVertexLayout.FLOATS_PER_QUAD;

    private static final int BOTTOM_LEFT = 0;
    private static final int TOP_LEFT = 1;
    private static final int TOP_RIGHT = 2;
    private static final int BOTTOM_RIGHT = 3;

    /** Sprite whose unscaled box is the cell at column 2, row 2 (x 16..24, y-up -36..-24). */
    private static GlyphSprite spriteAtCellTwoTwo() {
        GlyphSprite sprite = new GlyphSprite('@', COLOR);
        sprite.setPosition(2 * CELL_WIDTH, 2 * CELL_HEIGHT);
        return sprite;
    }

    private static float[] write(GlyphSprite sprite) {
        float[] vertices = new float[QUAD];
        SpriteQuadWriter.writeQuad(vertices, 0, sprite, CELL_WIDTH, CELL_HEIGHT, U, V, U2, V2);
        return vertices;
    }

    private static void assertCorner(float[] quad, int corner, float x, float y) {
        assertEquals(x, quad[corner * STRIDE + CellVertexLayout.POSITION_OFFSET], 0f, "x of corner " + corner);
        assertEquals(y, quad[corner * STRIDE + CellVertexLayout.POSITION_OFFSET + 1], 0f, "y of corner " + corner);
    }

    private static void assertUv(float[] quad, int corner, float u, float v) {
        assertEquals(u, quad[corner * STRIDE + CellVertexLayout.UV_OFFSET], 0f, "u of corner " + corner);
        assertEquals(v, quad[corner * STRIDE + CellVertexLayout.UV_OFFSET + 1], 0f, "v of corner " + corner);
    }

    private static void assertRotatedQuarterTurn(float[] quad) {
        assertCorner(quad, BOTTOM_LEFT, 26f, -34f);
        assertCorner(quad, TOP_LEFT, 14f, -34f);
        assertCorner(quad, TOP_RIGHT, 14f, -26f);
        assertCorner(quad, BOTTOM_RIGHT, 26f, -26f);
    }

    @Test
    void identitySpriteMatchesCellVertexCache() {
        int column = 2;
        int row = 1;
        CellVertexCache cache = new CellVertexCache(5, 4, CELL_WIDTH, CELL_HEIGHT);
        cache.writeCell(column, row, 0, COLOR, U, V, U2, V2);
        int offset = (row * 5 + column) * QUAD;
        float[] expected = Arrays.copyOfRange(cache.getVertices(), offset, offset + QUAD);

        GlyphSprite sprite = new GlyphSprite('@', COLOR);
        sprite.setPosition(column * CELL_WIDTH, row * CELL_HEIGHT);

        assertArrayEquals(expected, write(sprite), 0f);
    }

    @Test
    void identitySpriteWritesTransparentBackgroundAndPackedForeground() {
        float[] quad = write(spriteAtCellTwoTwo());
        for (int corner = 0; corner < CellVertexLayout.VERTICES_PER_QUAD; corner++) {
            int base = corner * STRIDE;
            assertEquals(ColorTools.fromRGBA8888(0), quad[base + CellVertexLayout.BACKGROUND_COLOR_OFFSET], 0f);
            assertEquals(ColorTools.fromRGBA8888(COLOR), quad[base + CellVertexLayout.FOREGROUND_COLOR_OFFSET], 0f);
        }
    }

    @Test
    void writesAtTheGivenStartAndLeavesOtherFloatsAlone() {
        float[] vertices = new float[QUAD * 3];
        Arrays.fill(vertices, -99f);
        SpriteQuadWriter.writeQuad(vertices, QUAD, spriteAtCellTwoTwo(), CELL_WIDTH, CELL_HEIGHT, U, V, U2, V2);

        for (int i = 0; i < QUAD; i++) {
            assertEquals(-99f, vertices[i]);
            assertEquals(-99f, vertices[2 * QUAD + i]);
        }
        assertArrayEquals(write(spriteAtCellTwoTwo()), Arrays.copyOfRange(vertices, QUAD, 2 * QUAD), 0f);
    }

    @Test
    void explicitPositionOverridesTheSpritePositionAndKeepsRotationAndScale() {
        GlyphSprite sprite = spriteAtCellTwoTwo();
        sprite.setRotation(90f);
        float[] snapped = new float[QUAD];
        SpriteQuadWriter.writeQuad(snapped, 0, sprite, 16f, 24f, CELL_WIDTH, CELL_HEIGHT, U, V, U2, V2);
        assertArrayEquals(write(sprite), snapped, 0f);

        sprite.setPosition(16.4f, 24.4f);
        SpriteQuadWriter.writeQuad(snapped, 0, sprite, ClipMath.snap(sprite.getX()), ClipMath.snap(sprite.getY()),
                CELL_WIDTH, CELL_HEIGHT, U, V, U2, V2);
        assertRotatedQuarterTurn(snapped);
    }

    @Test
    void identityUsesDefaultUvOrder() {
        float[] quad = write(spriteAtCellTwoTwo());
        assertUv(quad, BOTTOM_LEFT, U, V2);
        assertUv(quad, TOP_LEFT, U, V);
        assertUv(quad, TOP_RIGHT, U2, V);
        assertUv(quad, BOTTOM_RIGHT, U2, V2);
    }

    @Test
    void rotationOf90LandsExactlyAroundTheCenter() {
        GlyphSprite sprite = spriteAtCellTwoTwo();
        sprite.setRotation(90f);
        float[] quad = write(sprite);
        assertRotatedQuarterTurn(quad);
        assertUv(quad, BOTTOM_LEFT, U, V2);
        assertUv(quad, TOP_RIGHT, U2, V);
    }

    @Test
    void rotationOf360EqualsIdentity() {
        GlyphSprite rotated = spriteAtCellTwoTwo();
        rotated.setRotation(360f);
        assertArrayEquals(write(spriteAtCellTwoTwo()), write(rotated), 0f);
    }

    @Test
    void rotationOf180FlipsAroundTheCenter() {
        GlyphSprite sprite = spriteAtCellTwoTwo();
        sprite.setRotation(180f);
        float[] quad = write(sprite);
        assertCorner(quad, BOTTOM_LEFT, 24f, -24f);
        assertCorner(quad, TOP_LEFT, 24f, -36f);
        assertCorner(quad, TOP_RIGHT, 16f, -36f);
        assertCorner(quad, BOTTOM_RIGHT, 16f, -24f);
    }

    @Test
    void negativeQuarterTurnAnd270MatchEachOther() {
        GlyphSprite minus90 = spriteAtCellTwoTwo();
        minus90.setRotation(-90f);
        GlyphSprite plus270 = spriteAtCellTwoTwo();
        plus270.setRotation(270f);

        float[] quad = write(minus90);
        assertArrayEquals(quad, write(plus270), 0f);
        assertCorner(quad, BOTTOM_LEFT, 14f, -26f);
        assertCorner(quad, TOP_LEFT, 26f, -26f);
        assertCorner(quad, TOP_RIGHT, 26f, -34f);
        assertCorner(quad, BOTTOM_RIGHT, 14f, -34f);
    }

    @Test
    void rotationBeyondOneTurnWrapsExactly() {
        GlyphSprite sprite = spriteAtCellTwoTwo();
        sprite.setRotation(450f);
        assertRotatedQuarterTurn(write(sprite));
        sprite.setRotation(-270f);
        assertRotatedQuarterTurn(write(sprite));
    }

    @Test
    void rotationOf45KeepsTheCenterAndRotatesCounterclockwise() {
        GlyphSprite sprite = new GlyphSprite('@', COLOR);
        sprite.setPosition(-4f, -6f);
        sprite.setRotation(45f);
        float[] quad = write(sprite);

        float centerX = 0f;
        float centerY = 0f;
        for (int corner = 0; corner < CellVertexLayout.VERTICES_PER_QUAD; corner++) {
            centerX += quad[corner * STRIDE] / 4f;
            centerY += quad[corner * STRIDE + 1] / 4f;
        }
        assertEquals(0f, centerX, 1e-5f);
        assertEquals(0f, centerY, 1e-5f);
        // Counterclockwise on screen sends the right edge upward (greater y-up).
        assertTrue(quad[TOP_RIGHT * STRIDE + 1] > quad[TOP_LEFT * STRIDE + 1]);
    }

    @Test
    void scaleOfTwoDoublesTheBoxAroundTheCenter() {
        GlyphSprite sprite = spriteAtCellTwoTwo();
        sprite.setScale(2f);
        float[] quad = write(sprite);
        assertCorner(quad, BOTTOM_LEFT, 12f, -42f);
        assertCorner(quad, TOP_LEFT, 12f, -18f);
        assertCorner(quad, TOP_RIGHT, 28f, -18f);
        assertCorner(quad, BOTTOM_RIGHT, 28f, -42f);
    }

    @Test
    void scaleAboutTopLeftPivotKeepsTheTopLeftCornerFixed() {
        GlyphSprite sprite = spriteAtCellTwoTwo();
        sprite.setPivot(0f, 0f);
        sprite.setScale(2f);
        float[] quad = write(sprite);
        assertCorner(quad, TOP_LEFT, 16f, -24f);
        assertCorner(quad, TOP_RIGHT, 32f, -24f);
        assertCorner(quad, BOTTOM_LEFT, 16f, -48f);
        assertCorner(quad, BOTTOM_RIGHT, 32f, -48f);
    }

    @Test
    void rotationAboutTopLeftPivotKeepsTheTopLeftCornerFixed() {
        GlyphSprite sprite = spriteAtCellTwoTwo();
        sprite.setPivot(0f, 0f);
        sprite.setRotation(90f);
        float[] quad = write(sprite);
        assertCorner(quad, TOP_LEFT, 16f, -24f);
        assertCorner(quad, TOP_RIGHT, 16f, -16f);
        assertCorner(quad, BOTTOM_LEFT, 28f, -24f);
        assertCorner(quad, BOTTOM_RIGHT, 28f, -16f);
    }

    @Test
    void negativeScaleXMirrorsAndUvsStayWithTheirVertices() {
        GlyphSprite sprite = spriteAtCellTwoTwo();
        sprite.setScale(-1f, 1f);
        float[] quad = write(sprite);
        assertCorner(quad, BOTTOM_LEFT, 24f, -36f);
        assertCorner(quad, TOP_LEFT, 24f, -24f);
        assertCorner(quad, TOP_RIGHT, 16f, -24f);
        assertCorner(quad, BOTTOM_RIGHT, 16f, -36f);
        assertUv(quad, BOTTOM_LEFT, U, V2);
        assertUv(quad, TOP_LEFT, U, V);
        assertUv(quad, TOP_RIGHT, U2, V);
        assertUv(quad, BOTTOM_RIGHT, U2, V2);
    }

    @Test
    void negativeScaleYMirrorsVertically() {
        GlyphSprite sprite = spriteAtCellTwoTwo();
        sprite.setScale(1f, -1f);
        float[] quad = write(sprite);
        assertCorner(quad, BOTTOM_LEFT, 16f, -24f);
        assertCorner(quad, TOP_LEFT, 16f, -36f);
    }
}
