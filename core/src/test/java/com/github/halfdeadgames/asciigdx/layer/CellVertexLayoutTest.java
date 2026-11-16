package com.github.halfdeadgames.asciigdx.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import org.junit.jupiter.api.Test;

class CellVertexLayoutTest {

    private static final int MAX = CellVertexLayout.MAX_QUADS_PER_MESH;

    @Test
    void maxQuadsKeepsVertexIndicesWithinSixteenBits() {
        assertEquals(16383, MAX);
        assertTrue(MAX * CellVertexLayout.VERTICES_PER_QUAD <= 65535);
    }

    @Test
    void attributesMatchFloatsPerVertex() {
        assertEquals(CellVertexLayout.FLOATS_PER_VERTEX * Float.BYTES,
                CellVertexLayout.createAttributes().vertexSize);
        assertEquals(CellVertexLayout.VERTICES_PER_QUAD * CellVertexLayout.FLOATS_PER_VERTEX,
                CellVertexLayout.FLOATS_PER_QUAD);
    }

    @Test
    void attributeOffsetsMatchOffsetConstants() {
        VertexAttributes attributes = CellVertexLayout.createAttributes();
        assertEquals(4, attributes.size());
        assertAttribute(attributes.get(0), ShaderProgram.POSITION_ATTRIBUTE,
                CellVertexLayout.POSITION_OFFSET);
        assertAttribute(attributes.get(1), CellVertexLayout.BACKGROUND_COLOR_ATTRIBUTE,
                CellVertexLayout.BACKGROUND_COLOR_OFFSET);
        assertAttribute(attributes.get(2), CellVertexLayout.FOREGROUND_COLOR_ATTRIBUTE,
                CellVertexLayout.FOREGROUND_COLOR_OFFSET);
        assertAttribute(attributes.get(3), ShaderProgram.TEXCOORD_ATTRIBUTE + "0",
                CellVertexLayout.UV_OFFSET);
    }

    private static void assertAttribute(VertexAttribute attribute, String alias,
            int offsetInFloats) {
        assertEquals(alias, attribute.alias);
        assertEquals(offsetInFloats * Float.BYTES, attribute.offset, alias);
    }

    @Test
    void attributeNamesAreFixed() {
        assertEquals("a_background", CellVertexLayout.BACKGROUND_COLOR_ATTRIBUTE);
        assertEquals("a_foreground", CellVertexLayout.FOREGROUND_COLOR_ATTRIBUTE);
    }

    @Test
    void chunkCountRoundsUp() {
        assertEquals(1, CellVertexLayout.chunkCountFor(1));
        assertEquals(1, CellVertexLayout.chunkCountFor(MAX));
        assertEquals(2, CellVertexLayout.chunkCountFor(MAX + 1));
        assertEquals(2, CellVertexLayout.chunkCountFor(240 * 135));
    }

    @Test
    void quadsInChunkSumToTotal() {
        int total = 240 * 135;
        int sum = 0;
        for (int chunk = 0; chunk < CellVertexLayout.chunkCountFor(total); chunk++) {
            sum += CellVertexLayout.quadsInChunk(total, chunk);
        }
        assertEquals(total, sum);
        assertEquals(MAX, CellVertexLayout.quadsInChunk(MAX * 2, 0));
        assertEquals(MAX, CellVertexLayout.quadsInChunk(MAX * 2, 1));
    }
}
