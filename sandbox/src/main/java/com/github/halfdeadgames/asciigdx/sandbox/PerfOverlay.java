package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.layer.GridRenderer;

/** fps / frame time / cell count line drawn over whichever scene is open, at the top-left of the screen. */
class PerfOverlay {
    private static final float SAMPLE_WINDOW_SECONDS = 0.5f;
    private static final int WIDTH = 40;

    private final TerminalGrid grid = new TerminalGrid(WIDTH, 2);
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

    /** Updates the shown cell count without restarting the fps measurement. */
    void setCellCount(int cellCount) {
        this.cellCount = cellCount;
        rebuildReadout();
    }

    void setStatus(String text) {
        int background = text.isEmpty() ? GridScene.CLEAR : GridScene.BLACK;
        SceneDrawing.writePadded(grid, 0, 1, WIDTH, text, GridScene.WHITE, background);
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

    /** Reads the count out of Mesh.getManagedStatus(), which looks like "Managed meshes/app: { 3 }". */
    private static int managedMeshCount() {
        String managedStatus = Mesh.getManagedStatus();
        int count = 0;
        for (int i = managedStatus.indexOf('{') + 1; i > 0 && i < managedStatus.length(); i++) {
            char digit = managedStatus.charAt(i);
            if (digit >= '0' && digit <= '9') {
                count = count * 10 + (digit - '0');
            } else if (count > 0) {
                break;
            }
        }
        return count;
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
        readout.append(cellCount).append(" cells meshes ").append(managedMeshCount());
        SceneDrawing.writePadded(grid, 0, 0, WIDTH, readout, GridScene.WHITE, GridScene.BLACK);
    }
}
