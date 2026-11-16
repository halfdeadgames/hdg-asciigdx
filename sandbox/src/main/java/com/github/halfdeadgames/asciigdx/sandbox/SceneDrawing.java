package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import java.util.Random;

/** Drawing and random-fill helpers shared by the sandbox scenes. */
final class SceneDrawing {
    static final float VIEWPORT_BACKGROUND_RED = 0.15f;
    static final float VIEWPORT_BACKGROUND_GREEN = 0.15f;
    static final float VIEWPORT_BACKGROUND_BLUE = 0.2f;
    static final float VIEWPORT_BACKGROUND_ALPHA = 1f;

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
     * Clears the screen area covered by the viewport to the viewport background color, leaving
     * the rest alone.
     */
    static void clearViewportArea(Viewport viewport) {
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        HdpiUtils.glScissor(viewport.getScreenX(), viewport.getScreenY(),
                viewport.getScreenWidth(), viewport.getScreenHeight());
        clearToViewportBackground();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }

    /** Clears the whole current target to the viewport background color. */
    static void clearToViewportBackground() {
        ScreenUtils.clear(VIEWPORT_BACKGROUND_RED, VIEWPORT_BACKGROUND_GREEN,
                VIEWPORT_BACKGROUND_BLUE, VIEWPORT_BACKGROUND_ALPHA);
    }
}
