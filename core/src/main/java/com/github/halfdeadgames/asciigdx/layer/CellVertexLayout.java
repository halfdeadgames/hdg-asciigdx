package com.github.halfdeadgames.asciigdx.layer;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * The single definition of the cell vertex format: one quad per cell, with both
 * the background and the foreground color on every vertex, so a shader can mix
 * the glyph over the background in one pass. Add new attributes here: the
 * offsets, the stride, and {@link #createAttributes()}.
 */
final class CellVertexLayout {

    static final String BACKGROUND_COLOR_ATTRIBUTE = "a_background";
    static final String FOREGROUND_COLOR_ATTRIBUTE = "a_foreground";
    static final String TEXCOORD_ATTRIBUTE = ShaderProgram.TEXCOORD_ATTRIBUTE + "0";
    static final String CELL_ALPHA_ATTRIBUTE = "a_cellAlpha";

    static final int VERTICES_PER_QUAD = 4;
    static final int INDICES_PER_QUAD = 6;

    static final int POSITION_OFFSET = 0;
    static final int BACKGROUND_COLOR_OFFSET = 2;
    static final int FOREGROUND_COLOR_OFFSET = 3;
    static final int UV_OFFSET = 4;
    static final int CELL_ALPHA_OFFSET = 6;
    static final int FLOATS_PER_VERTEX = 7;
    static final float OPAQUE_CELL_ALPHA = 1f;
    static final int FLOATS_PER_QUAD = VERTICES_PER_QUAD * FLOATS_PER_VERTEX;

    /** Indices are 16-bit, so one mesh addresses at most 65535 vertices. */
    static final int MAX_QUADS_PER_MESH = 65535 / VERTICES_PER_QUAD;

    private CellVertexLayout() {
    }

    static VertexAttributes createAttributes() {
        return new VertexAttributes(
                new VertexAttribute(VertexAttributes.Usage.Position, 2,
                        ShaderProgram.POSITION_ATTRIBUTE),
                packedColor(BACKGROUND_COLOR_ATTRIBUTE),
                packedColor(FOREGROUND_COLOR_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2,
                        TEXCOORD_ATTRIBUTE, 0),
                new VertexAttribute(VertexAttributes.Usage.Generic, 1, GL20.GL_FLOAT, false,
                        CELL_ALPHA_ATTRIBUTE));
    }

    private static VertexAttribute packedColor(String name) {
        return new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, GL20.GL_UNSIGNED_BYTE,
                true, name);
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
