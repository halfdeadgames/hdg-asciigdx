package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.render.GridRenderer;

/** Sandbox application for visually checking rendering features. */
public class SandboxApp extends ApplicationAdapter {
    private static final int GRID_COLUMNS = 32;
    private static final int GRID_ROWS = 14;

    private static final int WHITE = 0xFFFFFFFF;
    private static final int GOLD = 0xFFD700FF;
    private static final int CYAN = 0x00FFFFFF;
    private static final int NAVY = 0x1A2A6CFF;
    private static final int DARK_RED = 0x6C1A1AFF;
    private static final int[] STRIPE_COLORS = {
        0xFF0000FF, 0xFF8000FF, 0xFFFF00FF, 0x00FF00FF, 0x0080FFFF, 0x8000FFFF
    };

    private SpriteBatch batch;
    private GlyphSource glyphs;
    private TerminalGrid grid;
    private GridRenderer renderer;
    private float viewHeight;

    @Override
    public void create() {
        batch = new SpriteBatch();
        glyphs = TextureGlyphSource.loadDungeonMode();
        grid = new TerminalGrid(GRID_COLUMNS, GRID_ROWS);
        renderer = new GridRenderer(grid, glyphs);
        fillDemoGrid();
    }

    private void fillDemoGrid() {
        drawBox(0, 0, GRID_COLUMNS, GRID_ROWS, WHITE);
        writeText(2, 1, "Top-left: row 0 is the top", WHITE, NAVY);
        writeText(2, 3, "Transparent bg over clear", GOLD, TerminalGrid.TRANSPARENT_RGBA);
        writeText(2, 5, "Red bg, cyan glyphs", CYAN, DARK_RED);
        for (int column = 1; column < GRID_COLUMNS - 1; column++) {
            int stripeColor = STRIPE_COLORS[column % STRIPE_COLORS.length];
            grid.set(column, 7, ' ', WHITE, stripeColor);
            grid.set(column, 8, '█', stripeColor, TerminalGrid.TRANSPARENT_RGBA);
        }
        writeText(2, 10, "Spaces skip:  [   ]", WHITE, TerminalGrid.TRANSPARENT_RGBA);
        writeText(2, GRID_ROWS - 2, "Bottom row of text", GOLD, NAVY);
    }

    private void drawBox(int left, int top, int width, int height, int color) {
        int right = left + width - 1;
        int bottom = top + height - 1;
        int clear = TerminalGrid.TRANSPARENT_RGBA;
        for (int column = left + 1; column < right; column++) {
            grid.set(column, top, '─', color, clear);
            grid.set(column, bottom, '─', color, clear);
        }
        for (int row = top + 1; row < bottom; row++) {
            grid.set(left, row, '│', color, clear);
            grid.set(right, row, '│', color, clear);
        }
        grid.set(left, top, '┌', color, clear);
        grid.set(right, top, '┐', color, clear);
        grid.set(left, bottom, '└', color, clear);
        grid.set(right, bottom, '┘', color, clear);
    }

    private void writeText(int column, int row, String text, int foreground, int background) {
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            grid.set(column++, row, codePoint, foreground, background);
            i += Character.charCount(codePoint);
        }
    }

    @Override
    public void resize(int width, int height) {
        int scale = integerScale(width, height);
        viewHeight = (float) height / scale;
        batch.getProjectionMatrix().setToOrtho2D(0, 0, (float) width / scale, viewHeight);
    }

    private int integerScale(int width, int height) {
        int columnScale = width / (GRID_COLUMNS * glyphs.getCellWidth());
        int rowScale = height / (GRID_ROWS * glyphs.getCellHeight());
        return Math.max(1, Math.min(columnScale, rowScale));
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        batch.begin();
        renderer.draw(batch, 0, viewHeight);
        batch.end();
    }

    @Override
    public void dispose() {
        renderer.dispose();
        batch.dispose();
        glyphs.dispose();
    }
}
