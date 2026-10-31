package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.github.halfdeadgames.asciigdx.render.IntegerScaling;
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
    private ScalingViewport viewport;

    @Override
    public void create() {
        batch = new SpriteBatch();
        glyphs = TextureGlyphSource.loadDungeonMode();
        grid = new TerminalGrid(GRID_COLUMNS, GRID_ROWS);
        renderer = new GridRenderer(grid, glyphs);
        viewport = new ScalingViewport(IntegerScaling.INSTANCE,
            GRID_COLUMNS * glyphs.getCellWidth(), GRID_ROWS * glyphs.getCellHeight());
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
        viewport.update(width, height, true);
        showScaleIndicator();
    }

    private void showScaleIndicator() {
        float scale = viewport.getScreenWidth() / viewport.getWorldWidth();
        String label = scale >= 1f
            ? String.format(" %dx ", Math.round(scale))
            : String.format(" %.2fx fit ", scale);
        int row = GRID_ROWS - 1;
        for (int column = 1; column < GRID_COLUMNS - 1; column++) {
            grid.set(column, row, '─', WHITE, TerminalGrid.TRANSPARENT_RGBA);
        }
        writeText(GRID_COLUMNS - 1 - label.length(), row, label, CYAN, TerminalGrid.TRANSPARENT_RGBA);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        clearGridArea();
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        renderer.draw(batch, 0, viewport.getWorldHeight());
        batch.end();
    }

    private void clearGridArea() {
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        HdpiUtils.glScissor(viewport.getScreenX(), viewport.getScreenY(),
            viewport.getScreenWidth(), viewport.getScreenHeight());
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }

    @Override
    public void dispose() {
        renderer.dispose();
        batch.dispose();
        glyphs.dispose();
    }
}
