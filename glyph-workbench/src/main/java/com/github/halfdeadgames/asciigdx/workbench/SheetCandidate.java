package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.github.halfdeadgames.asciigdx.glyph.ColorKeyConversion;
import com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource;
import com.github.halfdeadgames.asciigdx.glyph.TransparencyMode;

/** A 16x16 export of the sheet, with a texture to draw and pixels to compare. */
final class SheetCandidate implements Disposable {

    static final int CELL = 16;

    final String letter;
    final String name;
    /** Stable name written to the results file. */
    final String id;
    final TextureRegion[] regions;

    private final TextureGlyphSource source;
    private final int[] pixels;
    private final int width;

    private SheetCandidate(
        String letter, String name, String id, TextureGlyphSource source, int[] pixels, int width, int slots) {
        this.letter = letter;
        this.name = name;
        this.id = id;
        this.source = source;
        this.pixels = pixels;
        this.width = width;
        int columns = width / CELL;
        regions = new TextureRegion[slots];
        for (int slot = 0; slot < slots; slot++) {
            regions[slot] = new TextureRegion(
                source.getTexture(), (slot % columns) * CELL, (slot / columns) * CELL, CELL, CELL);
        }
    }

    /** Returns null, after logging, when the files are missing or do not fit the glyph order. */
    static SheetCandidate load(String letter, String name, String id, String baseName, int slots) {
        FileHandle image = Gdx.files.local("../build/exports/" + baseName + ".png");
        FileHandle characters = Gdx.files.local("../build/exports/" + baseName + ".txt");
        if (!image.exists() || !characters.exists()) {
            return null;
        }
        try {
            TextureGlyphSource source =
                TextureGlyphSource.loadSheet(image, characters, CELL, CELL, TransparencyMode.AUTO);
            Pixmap pixmap = new Pixmap(image);
            try {
                int columns = pixmap.getWidth() / CELL;
                if (columns * (pixmap.getHeight() / CELL) < slots) {
                    source.dispose();
                    Gdx.app.error("Workbench", baseName + " has fewer cells than the 8x8 sheet");
                    return null;
                }
                return new SheetCandidate(letter, name, id, source, readPixels(pixmap), pixmap.getWidth(), slots);
            } finally {
                pixmap.dispose();
            }
        } catch (RuntimeException e) {
            Gdx.app.error("Workbench", "Cannot load " + baseName + ": " + e.getMessage());
            return null;
        }
    }

    // Applies the same conversion the loader does, so equal pixels here mean equal on screen.
    private static int[] readPixels(Pixmap pixmap) {
        int[] result = new int[pixmap.getWidth() * pixmap.getHeight()];
        boolean hasTransparentPixel = false;
        for (int y = 0; y < pixmap.getHeight(); y++) {
            for (int x = 0; x < pixmap.getWidth(); x++) {
                int pixel = pixmap.getPixel(x, y);
                result[y * pixmap.getWidth() + x] = pixel;
                hasTransparentPixel |= ColorKeyConversion.isTransparent(pixel);
            }
        }
        int key = result[0];
        for (int i = 0; i < result.length; i++) {
            if (hasTransparentPixel) {
                result[i] = (result[i] & 0xFF) == 0 ? 0 : result[i];
            } else {
                result[i] = ColorKeyConversion.toWhiteWithAlpha(result[i], key);
            }
        }
        return result;
    }

    int[] cellPixels(int slot) {
        int columns = width / CELL;
        int left = (slot % columns) * CELL;
        int top = (slot / columns) * CELL;
        int[] cell = new int[CELL * CELL];
        for (int y = 0; y < CELL; y++) {
            System.arraycopy(pixels, (top + y) * width + left, cell, y * CELL, CELL);
        }
        return cell;
    }

    @Override
    public void dispose() {
        source.dispose();
    }
}
