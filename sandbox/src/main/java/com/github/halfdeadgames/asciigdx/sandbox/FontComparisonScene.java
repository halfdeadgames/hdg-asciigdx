package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.github.halfdeadgames.asciigdx.glyph.FreeTypeGlyphBaker;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.render.GridRenderer;
import com.github.halfdeadgames.asciigdx.render.IntegerScaling;

/** Alternates one grid between the bundled dungeon.mode PNG sheet and the same font baked from its TTF. */
class FontComparisonScene implements SandboxScene {
    private static final String TTF_PATH = "com/github/halfdeadgames/asciigdx/fonts/dungeon-mode/dungeon-mode.ttf";
    private static final int CELL_SIZE = 8;
    private static final float SECONDS_PER_SOURCE = 1.5f;

    private static final int COLUMNS = 24;
    private static final int BLOCK_SIZE = 16;
    private static final int BLOCK_LEFT = (COLUMNS - BLOCK_SIZE) / 2;
    private static final int LABEL_ROW = 0;
    private static final int BLOCK_TOP = 1;
    private static final int SAMPLE_TOP = BLOCK_TOP + BLOCK_SIZE + 1;
    private static final int ROWS = SAMPLE_TOP + 4;

    private static final int BLACK = GridScene.BLACK;
    private static final int WHITE = GridScene.WHITE;
    private static final int DIFFERING_SLOT_BACKGROUND = 0x2A3A55FF;
    private static final int[] DIFFERING_SLOTS = {37, 74, 75, 102, 104, 105, 106, 125, 134, 139, 178, 200, 255};

    private final TerminalGrid grid = new TerminalGrid(COLUMNS, ROWS);
    private final ScalingViewport viewport =
        new ScalingViewport(IntegerScaling.INSTANCE, COLUMNS * CELL_SIZE, ROWS * CELL_SIZE);
    private final GlyphSource pngSource;

    private TextureGlyphSource ttfSource;
    private GridRenderer pngRenderer;
    private GridRenderer ttfRenderer;
    private boolean showingTtf;
    private boolean paused;
    private float secondsOnSource;

    FontComparisonScene(GlyphSource pngSource) {
        this.pngSource = pngSource;
    }

    @Override
    public String id() {
        return "font-comparison";
    }

    @Override
    public String title() {
        return "Font comparison: PNG vs TTF";
    }

    @Override
    public String statusText() {
        return paused ? "Paused; space/tap resumes" : "Swapping; space/tap pauses";
    }

    @Override
    public boolean recordsAnimation() {
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode != Input.Keys.SPACE) {
            return false;
        }
        togglePaused();
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY) {
        togglePaused();
        return true;
    }

    private void togglePaused() {
        paused = !paused;
        writeLabel();
    }

    @Override
    public void show() {
        ttfSource = FreeTypeGlyphBaker.loadSource(Gdx.files.classpath(TTF_PATH), CELL_SIZE, GlyphOrder.CP437);
        pngRenderer = new GridRenderer(grid, pngSource);
        ttfRenderer = new GridRenderer(grid, ttfSource);

        showingTtf = false;
        paused = false;
        secondsOnSource = 0f;
        fillGrid();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void fillGrid() {
        grid.clear();
        for (int slot = 0; slot < BLOCK_SIZE * BLOCK_SIZE; slot++) {
            grid.set(BLOCK_LEFT + slot % BLOCK_SIZE, BLOCK_TOP + slot / BLOCK_SIZE,
                GlyphOrder.CP437.codePointAt(slot), WHITE, BLACK);
        }
        for (int slot : DIFFERING_SLOTS) {
            int column = BLOCK_LEFT + slot % BLOCK_SIZE;
            int row = BLOCK_TOP + slot / BLOCK_SIZE;
            grid.set(column, row, grid.getGlyph(column, row), WHITE, DIFFERING_SLOT_BACKGROUND);
        }
        GridScene.writeText(grid, 0, SAMPLE_TOP, "┌─┬─┐", WHITE, BLACK);
        GridScene.writeText(grid, 0, SAMPLE_TOP + 1, "│ ┼ │", WHITE, BLACK);
        GridScene.writeText(grid, 0, SAMPLE_TOP + 2, "└─┴─┘", WHITE, BLACK);
        GridScene.writeText(grid, 0, SAMPLE_TOP + 3, "The quick brown fox 100%", WHITE, BLACK);
        writeLabel();
    }

    private void writeLabel() {
        String label = (showingTtf ? "TTF baked" : "PNG sheet") + (paused ? " (paused)" : "");
        for (int column = 0; column < COLUMNS; column++) {
            grid.set(column, LABEL_ROW, column < label.length() ? label.charAt(column) : ' ', WHITE, BLACK);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render(SpriteBatch batch, float delta) {
        if (!paused) {
            secondsOnSource += delta;
            if (secondsOnSource >= SECONDS_PER_SOURCE) {
                secondsOnSource = 0f;
                showingTtf = !showingTtf;
                writeLabel();
            }
        }
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        HdpiUtils.glScissor(viewport.getScreenX(), viewport.getScreenY(),
            viewport.getScreenWidth(), viewport.getScreenHeight());
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        (showingTtf ? ttfRenderer : pngRenderer).draw(batch, 0, viewport.getWorldHeight());
        batch.end();
    }

    @Override
    public void hide() {
        if (pngRenderer != null) {
            pngRenderer.dispose();
            pngRenderer = null;
        }
        if (ttfRenderer != null) {
            ttfRenderer.dispose();
            ttfRenderer = null;
        }
        if (ttfSource != null) {
            ttfSource.dispose();
            ttfSource = null;
        }
    }

    @Override
    public void dispose() {
        hide();
    }

    @Override
    public int cellCount() {
        return COLUMNS * ROWS;
    }
}
