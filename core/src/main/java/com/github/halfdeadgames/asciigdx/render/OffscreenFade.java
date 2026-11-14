package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.utils.Array;

/** Pure rules and math for fading a layer through an offscreen target. */
final class OffscreenFade {

    /** Floats per composite vertex: x, y, u, v. */
    static final int COMPOSITE_VERTEX_FLOATS = 4;
    static final int COMPOSITE_FLOATS = 4 * COMPOSITE_VERTEX_FLOATS;

    private OffscreenFade() {
    }

    /**
     * A faded layer needs the offscreen path only when a drawn sprite or a lifted cell could
     * overlap something. A sprite with color alpha 0 draws nothing, so it cannot overlap; partial
     * alpha still counts. Without them the cells never overlap, so the direct path fades them
     * correctly.
     */
    static boolean isNeeded(float tintAlpha, Array<GlyphSprite> sprites, Array<CellTransform> cells) {
        if (tintAlpha >= 1f) {
            return false;
        }
        return SpriteMesh.countDrawn(sprites) > 0 || SpriteMesh.hasLifted(cells);
    }

    /**
     * Writes the composite quad into {@code out}: four vertices (bottom-left, bottom-right,
     * top-right, top-left), each x, y, u, v. Positions are clip space, so with the GL viewport
     * set to {@code viewport} the quad covers exactly that rectangle. The texture coordinates
     * select the same rectangle of a target of the given size, one texel per pixel. A framebuffer
     * texture is stored y-up like the backbuffer, so v runs upward with y and needs no flip.
     *
     * @param viewport x, y, width, height in backbuffer pixels, y-up
     */
    static void writeCompositeQuad(int[] viewport, int bufferWidth, int bufferHeight, float[] out) {
        float u = (float) viewport[0] / bufferWidth;
        float u2 = (float) (viewport[0] + viewport[2]) / bufferWidth;
        float v = (float) viewport[1] / bufferHeight;
        float v2 = (float) (viewport[1] + viewport[3]) / bufferHeight;
        writeVertex(out, 0, -1f, -1f, u, v);
        writeVertex(out, 1, 1f, -1f, u2, v);
        writeVertex(out, 2, 1f, 1f, u2, v2);
        writeVertex(out, 3, -1f, 1f, u, v2);
    }

    private static void writeVertex(float[] out, int vertex, float x, float y, float u, float v) {
        int offset = vertex * COMPOSITE_VERTEX_FLOATS;
        out[offset] = x;
        out[offset + 1] = y;
        out[offset + 2] = u;
        out[offset + 3] = v;
    }
}
