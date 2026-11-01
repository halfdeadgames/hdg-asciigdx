package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;

/**
 * GPU-resident quads for a grid: one set of meshes for backgrounds and one for
 * glyphs, each split into chunks that fit 16-bit indices. Vertices come from a
 * {@link GridVertexCache} and are re-uploaded only when {@link #upload} is called.
 */
final class GridMesh implements Disposable {

    private final int quadCount;
    private final Mesh[] backgroundChunks;
    private final Mesh[] glyphChunks;
    private final ShaderProgram shader;
    private final Matrix4 combinedMatrix = new Matrix4();

    /** The shader is shared and not owned; it is not disposed with the mesh. */
    GridMesh(int quadCount, ShaderProgram shader) {
        this.quadCount = quadCount;
        this.shader = shader;
        int chunkCount = GridVertexLayout.chunkCountFor(quadCount);
        short[] indices = createQuadIndices(Math.min(quadCount, GridVertexLayout.MAX_QUADS_PER_MESH));
        backgroundChunks = createChunks(chunkCount, indices);
        glyphChunks = createChunks(chunkCount, indices);
    }

    private Mesh[] createChunks(int chunkCount, short[] indices) {
        Mesh[] chunks = new Mesh[chunkCount];
        for (int chunk = 0; chunk < chunkCount; chunk++) {
            int quads = GridVertexLayout.quadsInChunk(quadCount, chunk);
            int indexCount = quads * GridVertexLayout.INDICES_PER_QUAD;
            chunks[chunk] = new Mesh(false, quads * GridVertexLayout.VERTICES_PER_QUAD, indexCount,
                    GridVertexLayout.createAttributes());
            chunks[chunk].setIndices(indices, 0, indexCount);
        }
        return chunks;
    }

    private static short[] createQuadIndices(int quads) {
        short[] indices = new short[quads * GridVertexLayout.INDICES_PER_QUAD];
        for (int quad = 0, index = 0, vertex = 0; quad < quads; quad++, vertex += GridVertexLayout.VERTICES_PER_QUAD) {
            indices[index++] = (short) vertex;
            indices[index++] = (short) (vertex + 1);
            indices[index++] = (short) (vertex + 2);
            indices[index++] = (short) (vertex + 2);
            indices[index++] = (short) (vertex + 3);
            indices[index++] = (short) vertex;
        }
        return indices;
    }

    /** Copies the full vertex arrays to the GPU. */
    void upload(float[] backgroundVertices, float[] glyphVertices) {
        uploadChunks(backgroundChunks, backgroundVertices);
        uploadChunks(glyphChunks, glyphVertices);
    }

    private void uploadChunks(Mesh[] chunks, float[] vertices) {
        for (int chunk = 0; chunk < chunks.length; chunk++) {
            int start = chunk * GridVertexLayout.MAX_QUADS_PER_MESH * GridVertexLayout.FLOATS_PER_QUAD;
            int count = GridVertexLayout.quadsInChunk(quadCount, chunk) * GridVertexLayout.FLOATS_PER_QUAD;
            chunks[chunk].setVertices(vertices, start, count);
        }
    }

    /**
     * Draws all backgrounds, then all glyphs, using the batch's projection,
     * transform, and blending, translated by the given offset and multiplied by the
     * tint. The batch must be begun. Leaves the batch's shader bound.
     */
    void draw(SpriteBatch batch, Texture backgroundTexture, Texture glyphTexture, float offsetX, float offsetY,
            float tintR, float tintG, float tintB, float tintA, boolean backgroundsOpaque,
            boolean drawBackgrounds, boolean drawGlyphs) {
        if (!drawBackgrounds && !drawGlyphs) {
            return;
        }
        batch.flush();
        boolean blending = batch.isBlendingEnabled();
        boolean skipBackgroundBlend = blending && backgroundsOpaque && tintA == 1f;
        if (blending && !skipBackgroundBlend) {
            applyBatchBlend(batch);
        } else {
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
        combinedMatrix.set(batch.getProjectionMatrix()).mul(batch.getTransformMatrix())
                .translate(offsetX, offsetY, 0f);
        shader.bind();
        shader.setUniformMatrix("u_projTrans", combinedMatrix);
        shader.setUniformi("u_texture", 0);
        shader.setUniformf("u_tint", tintR, tintG, tintB, tintA);
        if (drawBackgrounds) {
            renderChunks(backgroundChunks, backgroundTexture);
        }
        if (skipBackgroundBlend) {
            applyBatchBlend(batch);
        }
        if (drawGlyphs) {
            renderChunks(glyphChunks, glyphTexture);
        }
        batch.getShader().bind();
    }

    private static void applyBatchBlend(SpriteBatch batch) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFuncSeparate(batch.getBlendSrcFunc(), batch.getBlendDstFunc(),
                batch.getBlendSrcFuncAlpha(), batch.getBlendDstFuncAlpha());
    }

    private void renderChunks(Mesh[] chunks, Texture texture) {
        texture.bind(0);
        for (int chunk = 0; chunk < chunks.length; chunk++) {
            int indexCount = GridVertexLayout.quadsInChunk(quadCount, chunk) * GridVertexLayout.INDICES_PER_QUAD;
            chunks[chunk].render(shader, GL20.GL_TRIANGLES, 0, indexCount);
        }
    }

    @Override
    public void dispose() {
        for (int chunk = 0; chunk < backgroundChunks.length; chunk++) {
            backgroundChunks[chunk].dispose();
            glyphChunks[chunk].dispose();
        }
    }
}
