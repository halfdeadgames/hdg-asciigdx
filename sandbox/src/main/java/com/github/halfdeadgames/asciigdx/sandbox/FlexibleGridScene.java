package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.halfdeadgames.asciigdx.draw.GridDrawing;
import com.github.halfdeadgames.asciigdx.fit.GridFit;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import java.util.Locale;

/**
 * A grid that follows the window size at an exact scale; content stays centered through resizes.
 */
class FlexibleGridScene extends GridScene {
    private static final int INITIAL_COLUMNS = 80;
    private static final int INITIAL_ROWS = 45;
    private static final int MIN_COLUMNS = 40;
    private static final int MIN_ROWS = 25;

    private static final int MARKER_WIDTH = 12;
    private static final int MARKER_HEIGHT = 5;
    private static final int STATUS_ROWS = 5;
    private static final int STATUS_WIDTH = 30;
    private static final int FONT_ROW = 4;
    private static final int MS_ROW = 5;
    private static final int[] MARKER_COLORS = {
            0xC03030FF, 0xC07030FF, 0xB0B030FF, 0x30A030FF, 0x3070C0FF, 0x7040B0FF
    };
    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;

    private final GlyphSource font;

    private boolean fitIsEmpty;
    private boolean timingResize;
    private long resizeStartNanos;
    private int markerLeft;
    private int markerTop;

    FlexibleGridScene(GlyphSource glyphs) {
        super(glyphs, INITIAL_COLUMNS, INITIAL_ROWS);
        font = glyphs;
        drawMarker();
        drawBorder();
    }

    @Override
    public String id() {
        return "flexible-grid";
    }

    @Override
    public String title() {
        return "Flexible grid";
    }

    @Override
    public void resize(int width, int height) {
        resizeStartNanos = System.nanoTime();
        GridFit fit = GridFit.calculate(width, height, font.getCellWidth(), font.getCellHeight(),
                MIN_COLUMNS, MIN_ROWS);
        fitIsEmpty = fit.isEmpty();
        if (fitIsEmpty) {
            timingResize = false;
            return;
        }
        eraseBorderAndStatus();
        int columns = fit.getColumns();
        int rows = fit.getRows();
        int shiftX = centeringShift(markerLeft, MARKER_WIDTH, columns);
        int shiftY = centeringShift(markerTop, MARKER_HEIGHT, rows);
        grid.resize(columns, rows, shiftX, shiftY);
        markerLeft += shiftX;
        markerTop += shiftY;
        fit.applyTo(viewport, true);
        drawBorder();
        drawStatus(fit);
        timingResize = true;
    }

    @Override
    public void render(SpriteBatch batch, float delta) {
        if (fitIsEmpty) {
            ScreenUtils.clear(SceneDrawing.VIEWPORT_BACKGROUND);
            return;
        }
        super.render(batch, delta);
        if (timingResize) {
            Gdx.gl.glFinish();
            double milliseconds = (System.nanoTime() - resizeStartNanos) / NANOS_PER_MILLISECOND;
            timingResize = false;
            GridDrawing.drawText(grid, 1, MS_ROW,
                    String.format(Locale.ROOT, "resize %.2f ms", milliseconds), WHITE, CLEAR);
        }
    }

    /**
     * Columns or rows to shift content so it is centered; computed from its current start, so it
     * never drifts.
     */
    static int centeringShift(int contentStart, int contentSize, int gridSize) {
        return (gridSize - contentSize) / 2 - contentStart;
    }

    private boolean insideMarker(int x, int y) {
        return x >= markerLeft && x < markerLeft + MARKER_WIDTH
                && y >= markerTop && y < markerTop + MARKER_HEIGHT;
    }

    private void drawMarker() {
        int left = (grid.getColumns() - MARKER_WIDTH) / 2;
        int top = (grid.getRows() - MARKER_HEIGHT) / 2;
        markerLeft = left;
        markerTop = top;
        for (int row = 0; row < MARKER_HEIGHT; row++) {
            for (int column = 0; column < MARKER_WIDTH; column++) {
                int color = MARKER_COLORS[(column + row) % MARKER_COLORS.length];
                grid.set(left + column, top + row, ' ', WHITE, color);
            }
        }
        GridDrawing.drawText(grid, left + (MARKER_WIDTH - 4) / 2, top + MARKER_HEIGHT / 2, "KEPT",
                WHITE, MARKER_COLORS[0]);
    }

    private void drawBorder() {
        GridDrawing.drawBox(grid, 0, 0, grid.getColumns(), grid.getRows(), WHITE, CLEAR);
    }

    private void eraseBorderAndStatus() {
        int right = grid.getColumns() - 1;
        int bottom = grid.getRows() - 1;
        for (int column = 0; column <= right; column++) {
            clearUnlessMarker(column, 0);
            clearUnlessMarker(column, bottom);
        }
        for (int row = 1; row < bottom; row++) {
            clearUnlessMarker(0, row);
            clearUnlessMarker(right, row);
        }
        for (int row = 1; row <= STATUS_ROWS; row++) {
            for (int column = 1; column <= STATUS_WIDTH; column++) {
                clearUnlessMarker(column, row);
            }
        }
    }

    private void clearUnlessMarker(int x, int y) {
        if (!insideMarker(x, y)) {
            grid.set(x, y, ' ', WHITE, CLEAR);
        }
    }

    private void drawStatus(GridFit fit) {
        GridDrawing.drawText(grid, 1, 1, String.format(Locale.ROOT, "scale %dx  %dx%d cells",
                fit.getScale(), fit.getColumns(), fit.getRows()), CYAN, CLEAR);
        GridDrawing.drawText(grid, 1, 2, String.format(Locale.ROOT, "leftover %d,%d px",
                fit.getLeftoverWidth(), fit.getLeftoverHeight()), CYAN, CLEAR);
        GridDrawing.drawText(grid, 1, 3, fit.isMinimumMet() ? "minimum ok" : "below minimum",
                GOLD, CLEAR);
        GridDrawing.drawText(grid, 1, FONT_ROW, String.format(Locale.ROOT, "font %dx%d",
                font.getCellWidth(), font.getCellHeight()), GOLD, CLEAR);
        GridDrawing.drawText(grid, 1, MS_ROW, "resize -- ms", WHITE, CLEAR);
    }
}
