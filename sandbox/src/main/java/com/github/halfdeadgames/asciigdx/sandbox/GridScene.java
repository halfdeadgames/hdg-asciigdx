package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.render.IntegerScaling;
import com.github.halfdeadgames.asciigdx.render.LayerStack;

/** A scene made of one integer-scaled grid. Subclasses fill the grid and may change it in {@link #update(float)}. */
abstract class GridScene implements SandboxScene {
    static final int BLACK = 0x000000FF;
    static final int WHITE = 0xFFFFFFFF;
    static final int GOLD = 0xFFD700FF;
    static final int CYAN = 0x00FFFFFF;
    static final int CLEAR = TerminalGrid.TRANSPARENT_RGBA;

    protected final TerminalGrid grid;
    protected final ScalingViewport viewport;
    protected final LayerStack layers = new LayerStack();

    GridScene(GlyphSource glyphs, int columns, int rows) {
        grid = new TerminalGrid(columns, rows);
        layers.add(grid, glyphs);
        viewport = new ScalingViewport(IntegerScaling.INSTANCE,
            columns * glyphs.getCellWidth(), rows * glyphs.getCellHeight());
    }

    static void writeText(TerminalGrid target, int column, int row, CharSequence text, int foreground, int background) {
        for (int i = 0; i < text.length(); ) {
            int codePoint = Character.codePointAt(text, i);
            target.set(column++, row, codePoint, foreground, background);
            i += Character.charCount(codePoint);
        }
    }

    protected void writeText(int column, int row, CharSequence text, int foreground, int background) {
        writeText(grid, column, row, text, foreground, background);
    }

    @Override
    public void show() {
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    protected void update(float delta) {
    }

    @Override
    public void render(SpriteBatch batch, float delta) {
        update(delta);
        clearGridArea();
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        layers.draw(batch, 0, viewport.getWorldHeight());
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
    public void hide() {
    }

    @Override
    public void dispose() {
        layers.dispose();
    }

    @Override
    public int cellCount() {
        return grid.getWidth() * grid.getHeight();
    }
}
