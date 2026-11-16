package com.github.halfdeadgames.asciigdx.layer;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/** The cell shader that grid renderers can share. Needs a GL context to create. */
final class GridRenderResources implements Disposable {

    private static final String VERTEX_SOURCE =
            "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
                    + "attribute vec4 " + CellVertexLayout.BACKGROUND_COLOR_ATTRIBUTE + ";\n"
                    + "attribute vec4 " + CellVertexLayout.FOREGROUND_COLOR_ATTRIBUTE + ";\n"
                    + "attribute vec2 " + CellVertexLayout.TEXCOORD_ATTRIBUTE + ";\n"
                    + "uniform mat4 u_projTrans;\n"
                    + "varying vec4 v_background;\n"
                    + "varying vec4 v_foreground;\n"
                    + "varying vec2 v_texCoords;\n"
                    + "\n"
                    + "void main()\n"
                    + "{\n"
                    + "   v_background = " + CellVertexLayout.BACKGROUND_COLOR_ATTRIBUTE + ";\n"
                    + "   v_background.a = v_background.a * (255.0/254.0);\n"
                    + "   v_foreground = " + CellVertexLayout.FOREGROUND_COLOR_ATTRIBUTE + ";\n"
                    + "   v_foreground.a = v_foreground.a * (255.0/254.0);\n"
                    + "   v_texCoords = " + CellVertexLayout.TEXCOORD_ATTRIBUTE + ";\n"
                    + "   gl_Position = u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
                    + "}\n";

    // Glyph over background (Porter-Duff over), then the layer effects as one color matrix built
    // from saturation, tint, mix, and invert in that order (see ColorEffects), then alpha. Alpha
    // fades the cell as one color; the matrix never touches it. Mix comes before invert so a flash
    // can reach its color even on a darkly tinted layer; invert comes last so it negates the
    // finished look.
    private static final String FRAGMENT_SOURCE =
            "#ifdef GL_ES\n"
                    + "#define LOWP lowp\n"
                    + "precision mediump float;\n"
                    + "#else\n"
                    + "#define LOWP \n"
                    + "#endif\n"
                    + "varying LOWP vec4 v_background;\n"
                    + "varying LOWP vec4 v_foreground;\n"
                    + "varying vec2 v_texCoords;\n"
                    + "uniform sampler2D u_texture;\n"
                    + "uniform mat4 u_colorMatrix;\n"
                    + "uniform float u_alpha;\n"
                    + "void main()\n"
                    + "{\n"
                    + "  vec4 glyph = v_foreground * texture2D(u_texture, v_texCoords);\n"
                    + "  float alpha = glyph.a + v_background.a * (1.0 - glyph.a);\n"
                    + "  vec3 premultiplied = glyph.rgb * glyph.a + "
                            + "v_background.rgb * v_background.a * (1.0 - glyph.a);\n"
                    + "  vec3 rgb = alpha > 0.0 ? premultiplied / alpha : vec3(0.0);\n"
                    + "  rgb = (u_colorMatrix * vec4(rgb, 1.0)).rgb;\n"
                    + "  gl_FragColor = vec4(rgb, alpha * u_alpha);\n"
                    + "}";

    private final ShaderProgram shader;

    GridRenderResources() {
        shader = createShader();
    }

    private static ShaderProgram createShader() {
        ShaderProgram program = new ShaderProgram(VERTEX_SOURCE, FRAGMENT_SOURCE);
        if (!program.isCompiled()) {
            String log = program.getLog();
            program.dispose();
            throw new IllegalStateException("Grid shader failed to compile: " + log);
        }
        return program;
    }

    ShaderProgram getShader() {
        return shader;
    }

    @Override
    public void dispose() {
        shader.dispose();
    }
}
