package com.github.halfdeadgames.asciigdx.layer;

import com.github.tommyettinger.colorful.rgb.ColorTools;

/**
 * Writes a {@link GlyphSprite} as one quad in the {@link CellVertexLayout} format, in the same
 * space and vertex and uv order as {@link CellVertexCache}: grid top-left at (0, 0), y growing
 * upward, vertices bottom-left, top-left, top-right, bottom-right.
 */
final class SpriteQuadWriter {

    private static final float QUARTER_TURN_DEGREES = 90f;

    private SpriteQuadWriter() {
    }

    /**
     * Writes the sprite's quad with a fully transparent background.
     *
     * @param vertices destination array
     * @param start index of the quad's first float
     * @param cellWidth unscaled cell width in virtual pixels
     * @param cellHeight unscaled cell height in virtual pixels
     * @param u left texture coordinate
     * @param v top texture coordinate
     * @param u2 right texture coordinate
     * @param v2 bottom texture coordinate
     */
    static void writeQuad(float[] vertices, int start, GlyphSprite sprite, float cellWidth, float cellHeight,
                          float u, float v, float u2, float v2) {
        writeQuad(vertices, start, sprite, sprite.getX(), sprite.getY(), cellWidth, cellHeight, u, v, u2, v2);
    }

    /** Like the overload without a position, but places the box at the given position instead of the sprite's. */
    static void writeQuad(float[] vertices, int start, GlyphSprite sprite, float x, float y,
                          float cellWidth, float cellHeight, float u, float v, float u2, float v2) {
        writeQuad(vertices, start, sprite, sprite.getColor(), x, y, cellWidth, cellHeight, u, v, u2, v2);
    }

    /** Like the overload with a sprite, for any transform and an explicit glyph color (RGBA8888). */
    static void writeQuad(float[] vertices, int start, SpriteTransform sprite, int color, float x, float y,
                          float cellWidth, float cellHeight, float u, float v, float u2, float v2) {
        // Public rotation is clockwise on screen; the quad math here is y-up, where positive is counterclockwise.
        float yUpDegrees = -sprite.getRotation();
        float sin = exactSin(yUpDegrees);
        float cos = exactCos(yUpDegrees);
        float pivotX = pivotX(sprite, x, cellWidth);
        float pivotY = pivotY(sprite, y, cellHeight);
        float leftOffset = leftOffset(sprite, cellWidth);
        float rightOffset = rightOffset(sprite, cellWidth);
        float topOffset = topOffset(sprite, cellHeight);
        float bottomOffset = bottomOffset(sprite, cellHeight);
        float background = ColorTools.fromRGBA8888(0);
        float foreground = ColorTools.fromRGBA8888(color);

        putVertex(vertices, start, pivotX, pivotY, leftOffset, bottomOffset, sin, cos, background, foreground, u, v2);
        putVertex(vertices, start + CellVertexLayout.FLOATS_PER_VERTEX, pivotX, pivotY,
                leftOffset, topOffset, sin, cos, background, foreground, u, v);
        putVertex(vertices, start + 2 * CellVertexLayout.FLOATS_PER_VERTEX, pivotX, pivotY,
                rightOffset, topOffset, sin, cos, background, foreground, u2, v);
        putVertex(vertices, start + 3 * CellVertexLayout.FLOATS_PER_VERTEX, pivotX, pivotY,
                rightOffset, bottomOffset, sin, cos, background, foreground, u2, v2);
    }

    private static void putVertex(float[] vertices, int start, float pivotX, float pivotY,
                                  float offsetX, float offsetY, float sin, float cos,
                                  float background, float foreground, float u, float v) {
        vertices[start + CellVertexLayout.POSITION_OFFSET] = rotatedX(pivotX, offsetX, offsetY, sin, cos);
        vertices[start + CellVertexLayout.POSITION_OFFSET + 1] = rotatedY(pivotY, offsetX, offsetY, sin, cos);
        vertices[start + CellVertexLayout.BACKGROUND_COLOR_OFFSET] = background;
        vertices[start + CellVertexLayout.FOREGROUND_COLOR_OFFSET] = foreground;
        vertices[start + CellVertexLayout.UV_OFFSET] = u;
        vertices[start + CellVertexLayout.UV_OFFSET + 1] = v;
    }

    private static float pivotX(SpriteTransform sprite, float x, float cellWidth) {
        return x + sprite.getPivotX() * cellWidth;
    }

    private static float pivotY(SpriteTransform sprite, float y, float cellHeight) {
        return -(y + sprite.getPivotY() * cellHeight);
    }

    private static float leftOffset(SpriteTransform sprite, float cellWidth) {
        return -sprite.getPivotX() * cellWidth * sprite.getScaleX();
    }

    private static float rightOffset(SpriteTransform sprite, float cellWidth) {
        return (1f - sprite.getPivotX()) * cellWidth * sprite.getScaleX();
    }

    private static float topOffset(SpriteTransform sprite, float cellHeight) {
        return sprite.getPivotY() * cellHeight * sprite.getScaleY();
    }

    private static float bottomOffset(SpriteTransform sprite, float cellHeight) {
        return -(1f - sprite.getPivotY()) * cellHeight * sprite.getScaleY();
    }

    private static float rotatedX(float pivotX, float offsetX, float offsetY, float sin, float cos) {
        return pivotX + cos * offsetX - sin * offsetY;
    }

    private static float rotatedY(float pivotY, float offsetX, float offsetY, float sin, float cos) {
        return pivotY + sin * offsetX + cos * offsetY;
    }

    static float exactSin(float degrees) {
        if (!isQuarterTurn(degrees)) {
            return (float) Math.sin(Math.toRadians(degrees));
        }
        return switch (quarterTurnsModFour(degrees)) {
            case 1 -> 1f;
            case 3 -> -1f;
            default -> 0f;
        };
    }

    static float exactCos(float degrees) {
        if (!isQuarterTurn(degrees)) {
            return (float) Math.cos(Math.toRadians(degrees));
        }
        return switch (quarterTurnsModFour(degrees)) {
            case 0 -> 1f;
            case 2 -> -1f;
            default -> 0f;
        };
    }

    private static boolean isQuarterTurn(float degrees) {
        return degrees % QUARTER_TURN_DEGREES == 0f;
    }

    private static int quarterTurnsModFour(float degrees) {
        long quarterTurns = (long) (degrees / QUARTER_TURN_DEGREES);
        return (int) (((quarterTurns % 4) + 4) % 4);
    }
}
