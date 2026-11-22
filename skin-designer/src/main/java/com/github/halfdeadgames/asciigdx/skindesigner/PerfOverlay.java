package com.github.halfdeadgames.asciigdx.skindesigner;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.layer.GridRenderer;

/**
 * Frames per second, frame time, and cell count on the first row, and a status line on the
 * second, drawn at the top-left of the screen.
 */
class PerfOverlay {
    private static final float SAMPLE_WINDOW_SECONDS = 0.5f;
    private static final int WIDTH = 40;
    private static final int ROWS = 2;
    private static final int STATUS_ROW = 1;
    private static final int BLACK = 0x000000FF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int CLEAR = TerminalGrid.TRANSPARENT_RGBA;

    private final TerminalGrid grid = new TerminalGrid(WIDTH, ROWS);
    private final GridRenderer renderer;
    private final ScreenViewport viewport = new ScreenViewport();
    private final StringBuilder readout = new StringBuilder(WIDTH);
    private int cellCount;
    private float sampleSeconds;
    private int sampleFrames;

    PerfOverlay(GlyphSource glyphs) {
        renderer = new GridRenderer(grid, glyphs);
        viewport.setUnitsPerPixel(1f / screenPixelsPerUnit());
    }

    /** Returns the height in screen pixels that the overlay covers at the top of the screen. */
    static int heightInScreenPixels(GlyphSource glyphs) {
        return ROWS * glyphs.getCellHeight() * screenPixelsPerUnit();
    }

    private static int screenPixelsPerUnit() {
        return Math.max(1, Math.round(Gdx.graphics.getDensity()));
    }

    void startMeasuring(int cellCount) {
        this.cellCount = cellCount;
        sampleSeconds = 0f;
        sampleFrames = 0;
        rebuildReadout();
    }

    void setCellCount(int cellCount) {
        this.cellCount = cellCount;
        rebuildReadout();
    }

    void setStatus(String text) {
        int background = text.isEmpty() ? CLEAR : BLACK;
        writePadded(STATUS_ROW, text, background);
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

    // Mesh.getManagedStatus() looks like "Managed meshes/app: { 3 }".
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
        writePadded(0, readout, BLACK);
    }

    // Cut off or padded with spaces to exactly the overlay width.
    private void writePadded(int row, CharSequence text, int background) {
        int index = 0;
        for (int column = 0; column < WIDTH; column++) {
            int codePoint = ' ';
            if (index < text.length()) {
                codePoint = Character.codePointAt(text, index);
                index += Character.charCount(codePoint);
            }
            grid.set(column, row, codePoint, WHITE, background);
        }
    }
}
