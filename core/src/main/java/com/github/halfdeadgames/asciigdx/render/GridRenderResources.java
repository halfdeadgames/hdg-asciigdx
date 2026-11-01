package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/** The shader and white pixel texture that grid renderers can share. Needs a GL context to create. */
final class GridRenderResources implements Disposable {

    private static final String VERTEX_SOURCE =
            "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
            + "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"
            + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n"
            + "uniform mat4 u_projTrans;\n"
            + "uniform vec4 u_tint;\n"
            + "varying vec4 v_color;\n"
            + "varying vec2 v_texCoords;\n"
            + "\n"
            + "void main()\n"
            + "{\n"
            + "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"
            + "   v_color.a = v_color.a * (255.0/254.0);\n"
            + "   v_color = v_color * u_tint;\n"
            + "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n"
            + "   gl_Position = u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
            + "}\n";

    private static final String FRAGMENT_SOURCE =
            "#ifdef GL_ES\n"
            + "#define LOWP lowp\n"
            + "precision mediump float;\n"
            + "#else\n"
            + "#define LOWP \n"
            + "#endif\n"
            + "varying LOWP vec4 v_color;\n"
            + "varying vec2 v_texCoords;\n"
            + "uniform sampler2D u_texture;\n"
            + "void main()\n"
            + "{\n"
            + "  gl_FragColor = v_color * texture2D(u_texture, v_texCoords);\n"
            + "}";

    private final ShaderProgram shader;
    private final Texture whitePixel;

    GridRenderResources() {
        shader = createShader();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();
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

    Texture getWhitePixel() {
        return whitePixel;
    }

    @Override
    public void dispose() {
        shader.dispose();
        whitePixel.dispose();
    }
}
