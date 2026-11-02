package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;

/** All glyph cells of one candidate as a texture, one region per sheet slot; empty slots are blank. */
final class UpscaledSheet implements Disposable {

    private static final int COLUMNS = PaintSheetView.COLUMNS;
    private static final int LIT = 0xFFFFFFFF;

    final TextureRegion[] regions;

    private final PixelGrid[] cells;
    private final Texture texture;

    /** @param cells the whole cell of each slot, or null where the slot has no glyph */
    UpscaledSheet(PixelGrid[] cells, int cellWidth, int cellHeight) {
        this.cells = cells;
        int rows = (cells.length + COLUMNS - 1) / COLUMNS;
        Pixmap pixmap = new Pixmap(COLUMNS * cellWidth, rows * cellHeight, Pixmap.Format.RGBA8888);
        try {
            pixmap.setBlending(Pixmap.Blending.None);
            pixmap.setColor(0);
            pixmap.fill();
            for (int slot = 0; slot < cells.length; slot++) {
                if (cells[slot] != null) {
                    drawCell(pixmap, cells[slot], (slot % COLUMNS) * cellWidth, (slot / COLUMNS) * cellHeight);
                }
            }
            texture = new Texture(pixmap);
        } finally {
            pixmap.dispose();
        }
        regions = new TextureRegion[cells.length];
        for (int slot = 0; slot < cells.length; slot++) {
            regions[slot] = new TextureRegion(
                texture, (slot % COLUMNS) * cellWidth, (slot / COLUMNS) * cellHeight, cellWidth, cellHeight);
        }
    }

    private static void drawCell(Pixmap pixmap, PixelGrid cell, int left, int top) {
        for (int y = 0; y < cell.getHeight(); y++) {
            for (int x = 0; x < cell.getWidth(); x++) {
                if (cell.isLit(x, y)) {
                    pixmap.drawPixel(left + x, top + y, LIT);
                }
            }
        }
    }

    boolean hasGlyph(int slot) {
        return cells[slot] != null;
    }

    /** The whole cell of a slot, or null when it has no glyph. */
    PixelGrid cell(int slot) {
        return cells[slot];
    }

    /** The cell's pixels as 1 (lit) and 0, for telling candidates apart; blank slots are all 0. */
    int[] cellBits(int slot) {
        PixelGrid cell = cells[slot];
        if (cell == null) {
            return new int[0];
        }
        int[] bits = new int[cell.getWidth() * cell.getHeight()];
        for (int y = 0; y < cell.getHeight(); y++) {
            for (int x = 0; x < cell.getWidth(); x++) {
                bits[y * cell.getWidth() + x] = cell.isLit(x, y) ? 1 : 0;
            }
        }
        return bits;
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
