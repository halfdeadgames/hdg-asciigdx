package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;

/** Box, colors, transparency and the integer scale label. */
class RendererBasicsScene extends GridScene {
    private static final int COLUMNS = 32;
    private static final int ROWS = 14;

    private static final int NAVY = 0x1A2A6CFF;
    private static final int DARK_RED = 0x6C1A1AFF;
    private static final int[] STRIPE_COLORS = {
        0xFF0000FF, 0xFF8000FF, 0xFFFF00FF, 0x00FF00FF, 0x0080FFFF, 0x8000FFFF
    };

    RendererBasicsScene(GlyphSource glyphs) {
        super(glyphs, COLUMNS, ROWS);
        fillDemoGrid();
    }

    @Override
    public String id() {
        return "renderer-basics";
    }

    @Override
    public String title() {
        return "Renderer basics";
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        showScaleIndicator();
    }

    private void fillDemoGrid() {
        drawBox(0, 0, COLUMNS, ROWS, WHITE);
        writeText(2, 1, "Top-left: row 0 is the top", WHITE, NAVY);
        writeText(2, 3, "Transparent bg over clear", GOLD, CLEAR);
        writeText(2, 5, "Red bg, cyan glyphs", CYAN, DARK_RED);
        for (int column = 1; column < COLUMNS - 1; column++) {
            int stripeColor = STRIPE_COLORS[column % STRIPE_COLORS.length];
            grid.set(column, 7, ' ', WHITE, stripeColor);
            grid.set(column, 8, '█', stripeColor, CLEAR);
        }
        writeText(2, 10, "Spaces skip:  [   ]", WHITE, CLEAR);
        writeText(2, ROWS - 2, "Bottom row of text", GOLD, NAVY);
    }

    private void drawBox(int left, int top, int width, int height, int color) {
        int right = left + width - 1;
        int bottom = top + height - 1;
        for (int column = left + 1; column < right; column++) {
            grid.set(column, top, '─', color, CLEAR);
            grid.set(column, bottom, '─', color, CLEAR);
        }
        for (int row = top + 1; row < bottom; row++) {
            grid.set(left, row, '│', color, CLEAR);
            grid.set(right, row, '│', color, CLEAR);
        }
        grid.set(left, top, '┌', color, CLEAR);
        grid.set(right, top, '┐', color, CLEAR);
        grid.set(left, bottom, '└', color, CLEAR);
        grid.set(right, bottom, '┘', color, CLEAR);
    }

    private void showScaleIndicator() {
        float scale = viewport.getScreenWidth() / viewport.getWorldWidth();
        String label = scale >= 1f
            ? String.format(" %dx ", Math.round(scale))
            : String.format(" %.2fx fit ", scale);
        int row = ROWS - 1;
        for (int column = 1; column < COLUMNS - 1; column++) {
            grid.set(column, row, '─', WHITE, CLEAR);
        }
        writeText(COLUMNS - 1 - label.length(), row, label, CYAN, CLEAR);
    }
}
