package com.github.halfdeadgames.asciigdx.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GridVertexLayoutTest {

    private static final int MAX = GridVertexLayout.MAX_QUADS_PER_MESH;

    @Test
    void maxQuadsKeepsVertexIndicesWithinSixteenBits() {
        assertEquals(16383, MAX);
        assertEquals(true, MAX * GridVertexLayout.VERTICES_PER_QUAD <= 65535);
    }

    @Test
    void attributesMatchFloatsPerVertex() {
        assertEquals(GridVertexLayout.FLOATS_PER_VERTEX * Float.BYTES,
                GridVertexLayout.createAttributes().vertexSize);
    }

    @Test
    void chunkCountRoundsUp() {
        assertEquals(1, GridVertexLayout.chunkCountFor(1));
        assertEquals(1, GridVertexLayout.chunkCountFor(MAX));
        assertEquals(2, GridVertexLayout.chunkCountFor(MAX + 1));
        assertEquals(2, GridVertexLayout.chunkCountFor(240 * 135));
    }

    @Test
    void quadsInChunkSumToTotal() {
        int total = 240 * 135;
        int sum = 0;
        for (int chunk = 0; chunk < GridVertexLayout.chunkCountFor(total); chunk++) {
            sum += GridVertexLayout.quadsInChunk(total, chunk);
        }
        assertEquals(total, sum);
        assertEquals(MAX, GridVertexLayout.quadsInChunk(MAX * 2, 0));
        assertEquals(MAX, GridVertexLayout.quadsInChunk(MAX * 2, 1));
    }
}
