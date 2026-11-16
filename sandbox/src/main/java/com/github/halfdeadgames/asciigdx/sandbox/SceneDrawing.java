package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.Random;

/** Drawing and random-fill helpers shared by the sandbox scenes. */
final class SceneDrawing {
    static final Color VIEWPORT_BACKGROUND = new Color(0.15f, 0.15f, 0.2f, 1f);

    private static final int FIRST_RANDOM_GLYPH = '!';
    private static final int LAST_RANDOM_GLYPH = '~';

    private SceneDrawing() {
    }

    /**
     * Writes {@code text} from {@code column}, cut off or padded with spaces to exactly
     * {@code width} cells.
     */
    static void writePadded(TerminalGrid grid, int column, int row, int width, CharSequence text,
            int foreground, int background) {
        int index = 0;
        for (int offset = 0; offset < width; offset++) {
            int codePoint = ' ';
            if (index < text.length()) {
                codePoint = Character.codePointAt(text, index);
                index += Character.charCount(codePoint);
            }
            grid.set(column + offset, row, codePoint, foreground, background);
        }
    }

    /** A random RGBA8888 color with full alpha. */
    static int randomOpaqueColor(Random random) {
        return (random.nextInt(0x1000000) << 8) | 0xFF;
    }

    /** A random printable ASCII glyph, from '!' to '~'. */
    static int randomGlyph(Random random) {
        return FIRST_RANDOM_GLYPH + random.nextInt(LAST_RANDOM_GLYPH - FIRST_RANDOM_GLYPH + 1);
    }

    /**
     * Clears the screen area covered by the viewport to {@link #VIEWPORT_BACKGROUND}, leaving the
     * rest alone.
     */
    static void clearViewportArea(Viewport viewport) {
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        HdpiUtils.glScissor(viewport.getScreenX(), viewport.getScreenY(),
                viewport.getScreenWidth(), viewport.getScreenHeight());
        ScreenUtils.clear(VIEWPORT_BACKGROUND);
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }
}
