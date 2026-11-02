package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;

/** Draws rectangles, glyphs, and text using window pixels with the origin at the top-left. */
final class Painter {

    static final int TEXT_SIZE = 24;

    private final SpriteBatch batch;
    private final Texture whitePixel;
    private final GlyphSource font;
    private final Viewport viewport;

    Painter(SpriteBatch batch, Texture whitePixel, GlyphSource font, Viewport viewport) {
        this.batch = batch;
        this.whitePixel = whitePixel;
        this.font = font;
        this.viewport = viewport;
    }

    void fill(Color color, int x, int top, int width, int height) {
        batch.setColor(color);
        batch.draw(whitePixel, x, flipY(top, height), width, height);
    }

    /** Draws the lit pixels of a grid as filled squares of {@code scale} pixels. */
    void pixels(PixelGrid grid, int x, int top, int scale, Color color) {
        for (int py = 0; py < grid.getHeight(); py++) {
            for (int px = 0; px < grid.getWidth(); px++) {
                if (grid.isLit(px, py)) {
                    fill(color, x + px * scale, top + py * scale, scale, scale);
                }
            }
        }
    }

    void region(TextureRegion region, int x, int top, int size, Color tint) {
        batch.setColor(tint);
        batch.draw(region, x, flipY(top, size), size, size);
    }

    void text(CharSequence value, int x, int top, Color color) {
        batch.setColor(color);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != ' ') {
                batch.draw(font.glyph(c), x + i * TEXT_SIZE, flipY(top, TEXT_SIZE), TEXT_SIZE, TEXT_SIZE);
            }
        }
    }

    /** Draws a double-line border of text-sized cells; the size must be at least 2x2 cells. */
    void frame(Color color, int x, int top, int cellsWide, int cellsHigh) {
        int right = x + (cellsWide - 1) * TEXT_SIZE;
        int bottom = top + (cellsHigh - 1) * TEXT_SIZE;
        batch.setColor(color);
        cell('╔', x, top);
        cell('╗', right, top);
        cell('╚', x, bottom);
        cell('╝', right, bottom);
        for (int col = 1; col < cellsWide - 1; col++) {
            cell('═', x + col * TEXT_SIZE, top);
            cell('═', x + col * TEXT_SIZE, bottom);
        }
        for (int row = 1; row < cellsHigh - 1; row++) {
            cell('║', x, top + row * TEXT_SIZE);
            cell('║', right, top + row * TEXT_SIZE);
        }
    }

    private void cell(char c, int x, int top) {
        batch.draw(font.glyph(c), x, flipY(top, TEXT_SIZE), TEXT_SIZE, TEXT_SIZE);
    }

    /** Rounds a pixel length up to whole text cells. */
    static int roundUpToCells(int pixels) {
        return (pixels + TEXT_SIZE - 1) / TEXT_SIZE * TEXT_SIZE;
    }

    private float flipY(int top, int height) {
        return viewport.getWorldHeight() - top - height;
    }
}
