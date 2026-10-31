package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.render.GridRenderer;

/** fps / frame time / cell count line drawn over whichever scene is open, at the top-left of the screen. */
class PerfOverlay {
    private static final float SAMPLE_WINDOW_SECONDS = 0.5f;
    private static final int WIDTH = 28;

    private final TerminalGrid grid = new TerminalGrid(WIDTH, 1);
    private final GridRenderer renderer;
    private final ScreenViewport viewport = new ScreenViewport();
    private final StringBuilder readout = new StringBuilder(WIDTH);
    private int cellCount;
    private float sampleSeconds;
    private int sampleFrames;

    PerfOverlay(GlyphSource glyphs) {
        renderer = new GridRenderer(grid, glyphs);
        viewport.setUnitsPerPixel(1f / Math.max(1, Math.round(Gdx.graphics.getDensity())));
    }

    /** Restarts measuring for a newly opened scene. */
    void begin(int cellCount) {
        this.cellCount = cellCount;
        sampleSeconds = 0f;
        sampleFrames = 0;
        rebuildReadout();
    }

    void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    void update(float deltaSeconds) {
        sampleSeconds += deltaSeconds;
        sampleFrames++;
        if (sampleSeconds >= SAMPLE_WINDOW_SECONDS) {
            rebuildReadout();
            sampleSeconds = 0f;
            sampleFrames = 0;
        }
    }

    void render(SpriteBatch batch) {
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        renderer.draw(batch, 0, viewport.getWorldHeight());
        batch.end();
    }

    void dispose() {
        renderer.dispose();
    }

    private void rebuildReadout() {
        readout.setLength(0);
        if (sampleFrames > 0) {
            int tenthsOfMillisecond = Math.round(sampleSeconds / sampleFrames * 10000f);
            readout.append(Math.round(sampleFrames / sampleSeconds)).append(" fps ")
                .append(tenthsOfMillisecond / 10).append('.').append(tenthsOfMillisecond % 10)
                .append(" ms ");
        } else {
            readout.append("measuring ");
        }
        readout.append(cellCount).append(" cells");
        for (int column = 0; column < WIDTH; column++) {
            char character = column < readout.length() ? readout.charAt(column) : ' ';
            grid.set(column, 0, character, GridScene.WHITE, GridScene.BLACK);
        }
    }
}
