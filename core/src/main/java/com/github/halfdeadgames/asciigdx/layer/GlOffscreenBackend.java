package com.github.halfdeadgames.asciigdx.layer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.FloatBuffer;

/**
 * One backbuffer-sized RGBA8888 target plus a composite shader and quad, created on first use.
 * The target is recreated when the backbuffer size changes. Nothing touches GL until
 * {@link #begin} is first called.
 *
 * <p>The composite always blends premultiplied (ONE, ONE_MINUS_SRC_ALPHA), whatever blend
 * functions the batch has. The app's clear color is kept. Not usable while another framebuffer
 * is bound, because ending returns to the default framebuffer.
 */
final class GlOffscreenBackend implements OffscreenBackend {

    private static final String VERTEX_SOURCE =
            "attribute vec2 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
            + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n"
            + "varying vec2 v_texCoords;\n"
            + "void main()\n"
            + "{\n"
            + "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n"
            + "   gl_Position = vec4(" + ShaderProgram.POSITION_ATTRIBUTE + ", 0.0, 1.0);\n"
            + "}\n";

    private static final String FRAGMENT_SOURCE =
            "#ifdef GL_ES\n"
            + "precision mediump float;\n"
            + "#endif\n"
            + "varying vec2 v_texCoords;\n"
            + "uniform sampler2D u_texture;\n"
            + "uniform float u_alpha;\n"
            + "void main()\n"
            + "{\n"
            + "  gl_FragColor = texture2D(u_texture, v_texCoords) * u_alpha;\n"
            + "}";

    private final float[] quadVertices = new float[OffscreenFade.COMPOSITE_FLOATS];
    // GL may write up to 16 floats for a get call, so the buffer is larger than the 4 used.
    private final FloatBuffer savedClearColor = BufferUtils.newFloatBuffer(16);
    private FrameBuffer target;
    private ShaderProgram compositeShader;
    private Mesh compositeQuad;

    @Override
    public void begin(SpriteBatch batch, int[] viewport) {
        ensureTargetMatchesBackbuffer();
        batch.flush();
        target.begin();
        Gdx.gl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        clearToTransparent();
    }

    private void clearToTransparent() {
        savedClearColor.clear();
        Gdx.gl.glGetFloatv(GL20.GL_COLOR_CLEAR_VALUE, savedClearColor);
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glClearColor(savedClearColor.get(0), savedClearColor.get(1),
                savedClearColor.get(2), savedClearColor.get(3));
    }

    @Override
    public void end(int[] viewport) {
        target.end();
        Gdx.gl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
    }

    @Override
    public void composite(SpriteBatch batch, float alpha, int[] viewport) {
        Texture texture = target.getColorBufferTexture();
        OffscreenFade.writeCompositeQuad(viewport, texture.getWidth(), texture.getHeight(), quadVertices);
        compositeQuad.setVertices(quadVertices);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFuncSeparate(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA,
                GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
        compositeShader.bind();
        compositeShader.setUniformi("u_texture", 0);
        compositeShader.setUniformf("u_alpha", alpha);
        texture.bind(0);
        compositeQuad.render(compositeShader, GL20.GL_TRIANGLES);
        batch.getShader().bind();
    }

    private void ensureTargetMatchesBackbuffer() {
        int width = Gdx.graphics.getBackBufferWidth();
        int height = Gdx.graphics.getBackBufferHeight();
        if (target != null && target.getWidth() == width && target.getHeight() == height) {
            return;
        }
        if (target != null) {
            target.dispose();
        }
        target = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
        target.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        ensureCompositeResources();
    }

    private void ensureCompositeResources() {
        if (compositeShader != null) {
            return;
        }
        compositeShader = createCompositeShader();
        compositeQuad = createCompositeQuad();
    }

    private static ShaderProgram createCompositeShader() {
        ShaderProgram program = new ShaderProgram(VERTEX_SOURCE, FRAGMENT_SOURCE);
        if (!program.isCompiled()) {
            String log = program.getLog();
            program.dispose();
            throw new IllegalStateException("Composite shader failed to compile: " + log);
        }
        return program;
    }

    private static Mesh createCompositeQuad() {
        Mesh quad = new Mesh(false, 4, 6,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));
        quad.setIndices(new short[] {0, 1, 2, 2, 3, 0});
        return quad;
    }

    @Override
    public void dispose() {
        if (target != null) {
            target.dispose();
            target = null;
        }
        if (compositeShader != null) {
            compositeShader.dispose();
            compositeShader = null;
            compositeQuad.dispose();
            compositeQuad = null;
        }
    }
}
