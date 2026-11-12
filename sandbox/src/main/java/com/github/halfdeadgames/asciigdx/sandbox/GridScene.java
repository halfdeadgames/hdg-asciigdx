package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.render.IntegerScaling;
import com.github.halfdeadgames.asciigdx.render.LayerStack;

/** A scene drawn through an integer-scaled viewport. Holds a main grid and a {@link LayerStack} for extra layers; subclasses fill them and may change them in {@link #update(float)}. */
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
        SceneDrawing.clearViewportArea(viewport);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        layers.draw(batch, 0, viewport.getWorldHeight());
        drawOverGrid(batch);
        batch.end();
    }

    /** Draws extra sprites in grid pixels after the layers, inside the open batch. */
    protected void drawOverGrid(SpriteBatch batch) {
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
