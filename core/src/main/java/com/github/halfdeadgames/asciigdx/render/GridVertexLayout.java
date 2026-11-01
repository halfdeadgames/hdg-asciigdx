package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * The single definition of the grid vertex format. It matches SpriteBatch's
 * layout, so {@code SpriteBatch.createDefaultShader()} can draw it. Add new
 * attributes here: the offsets, the stride, and {@link #createAttributes()}.
 */
final class GridVertexLayout {

    static final int VERTICES_PER_QUAD = 4;
    static final int INDICES_PER_QUAD = 6;

    static final int POSITION_OFFSET = 0;
    static final int COLOR_OFFSET = 2;
    static final int UV_OFFSET = 3;
    static final int FLOATS_PER_VERTEX = 5;
    static final int FLOATS_PER_QUAD = VERTICES_PER_QUAD * FLOATS_PER_VERTEX;

    /** Indices are 16-bit, so one mesh addresses at most 65535 vertices. */
    static final int MAX_QUADS_PER_MESH = 65535 / VERTICES_PER_QUAD;

    private GridVertexLayout() {
    }

    static VertexAttributes createAttributes() {
        return new VertexAttributes(
                new VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
                VertexAttribute.ColorPacked(),
                VertexAttribute.TexCoords(0));
    }

    /** Number of meshes needed to hold the given quads. */
    static int chunkCountFor(int quadCount) {
        return (quadCount + MAX_QUADS_PER_MESH - 1) / MAX_QUADS_PER_MESH;
    }

    /** Quads in the given chunk; every chunk is full except possibly the last. */
    static int quadsInChunk(int quadCount, int chunk) {
        return Math.min(MAX_QUADS_PER_MESH, quadCount - chunk * MAX_QUADS_PER_MESH);
    }
}
