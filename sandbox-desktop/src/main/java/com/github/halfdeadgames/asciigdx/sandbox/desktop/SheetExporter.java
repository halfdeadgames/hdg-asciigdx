package com.github.halfdeadgames.asciigdx.sandbox.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import com.github.halfdeadgames.asciigdx.glyph.FreeTypeGlyphBaker;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrderText;
import com.github.halfdeadgames.asciigdx.sandbox.PixelScaler;
import java.nio.charset.StandardCharsets;

/**
 * Bakes a TTF into a glyph sheet PNG plus its character list {@code .txt}, without opening a
 * window. Run through the {@code exportSheet} Gradle task, which passes the settings as
 * {@code asciigdx.export.*} system properties.
 */
public final class SheetExporter {

    private static final String PROPERTY_PREFIX = "asciigdx.export.";

    private SheetExporter() {
    }

    public static void main(String[] args) {
        GdxNativesLoader.load();
        new SharedLibraryLoader().load("gdx-freetype");
        Gdx.files = new Lwjgl3Files();

        FileHandle font = fontFile(System.getProperty(PROPERTY_PREFIX + "font"));
        int cellHeight = Integer.parseInt(System.getProperty(PROPERTY_PREFIX + "cellHeight", "8"));
        int columns = Integer.parseInt(System.getProperty(PROPERTY_PREFIX + "columns", "16"));
        int scale = Integer.parseInt(System.getProperty(PROPERTY_PREFIX + "scale", "1"));
        if (scale < 1) {
            throw new IllegalArgumentException("scale must be at least 1: " + scale);
        }
        GlyphOrder order =
                glyphOrder(System.getProperty(PROPERTY_PREFIX + "chars", "cp437"), columns);
        String name = System.getProperty(PROPERTY_PREFIX + "sheetName", font.nameWithoutExtension())
                + "-" + cellHeight * scale;
        FileHandle outputFolder =
                Gdx.files.absolute(System.getProperty(PROPERTY_PREFIX + "output"));

        exportSheet(font, cellHeight, order, columns, scale, outputFolder, name);
    }

    private static void exportSheet(FileHandle font, int cellHeight, GlyphOrder order, int columns,
            int scale,
            FileHandle outputFolder, String name) {
        FreeTypeGlyphBaker.BakedSheet baked =
                FreeTypeGlyphBaker.bake(font, cellHeight, order, columns, true);
        Pixmap output = null;
        try {
            output = scaled(baked.getPixmap(), scale);
            FileHandle png = outputFolder.child(name + ".png");
            FileHandle txt = outputFolder.child(name + ".txt");
            PixmapIO.writePNG(png, output);
            txt.writeString(GlyphOrderText.write(order, columns), false, "UTF-8");
            verifyRoundTrip(output, order, columns, png, txt);
            System.out.println("Exported " + order.getGlyphCount() + " glyphs, "
                    + baked.getLayout().getCellWidth() * scale + "x"
                    + baked.getLayout().getCellHeight() * scale + " cells, " + missingCount(baked,
                    order) + " missing from the font");
            System.out.println("  " + png.file().getAbsolutePath());
            System.out.println("  " + txt.file().getAbsolutePath());
        } finally {
            if (output != null && output != baked.getPixmap()) {
                output.dispose();
            }
            baked.getPixmap().dispose();
        }
    }

    private static GlyphOrder glyphOrder(String charsSetting, int columns) {
        if (charsSetting.equalsIgnoreCase("cp437")) {
            return GlyphOrder.CP437;
        }
        return GlyphOrderText.read(Gdx.files.absolute(charsSetting).readString("UTF-8"), columns);
    }

    // Returns the source itself when scale is 1; the caller disposes the result only if it differs.
    private static Pixmap scaled(Pixmap source, int scale) {
        if (scale == 1) {
            return source;
        }
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = source.getPixel(x, y);
            }
        }
        int[] scaledPixels = PixelScaler.scale(pixels, width, height, scale);
        int scaledWidth = width * scale;
        Pixmap result = new Pixmap(scaledWidth, height * scale, Pixmap.Format.RGBA8888);
        result.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < height * scale; y++) {
            for (int x = 0; x < scaledWidth; x++) {
                result.drawPixel(x, y, scaledPixels[y * scaledWidth + x]);
            }
        }
        return result;
    }

    private static FileHandle fontFile(String setting) {
        if (setting == null || setting.isEmpty()) {
            throw new IllegalArgumentException("Missing -Pfont=<path to a TTF file>");
        }
        FileHandle file = Gdx.files.absolute(setting);
        if (!file.exists()) {
            throw new IllegalArgumentException("Font not found: " + setting);
        }
        return file;
    }

    private static int missingCount(FreeTypeGlyphBaker.BakedSheet baked, GlyphOrder order) {
        int missing = 0;
        for (int slot = 0; slot < order.getGlyphCount(); slot++) {
            if (!baked.hasGlyph(order.getCodePoint(slot))) {
                missing++;
            }
        }
        return missing;
    }

    private static void verifyRoundTrip(
            Pixmap original, GlyphOrder order, int columns, FileHandle png, FileHandle txt) {
        GlyphOrder reread =
                GlyphOrderText.read(new String(txt.readBytes(), StandardCharsets.UTF_8), columns);
        if (reread.getGlyphCount() != order.getGlyphCount()) {
            throw new IllegalStateException(
                    "Character list did not round-trip: size " + reread.getGlyphCount());
        }
        for (int slot = 0; slot < order.getGlyphCount(); slot++) {
            if (reread.getCodePoint(slot) != order.getCodePoint(slot)) {
                throw new IllegalStateException(
                        "Character list did not round-trip at slot " + slot);
            }
        }
        Pixmap written = new Pixmap(png);
        try {
            for (int y = 0; y < original.getHeight(); y++) {
                for (int x = 0; x < original.getWidth(); x++) {
                    if (written.getPixel(x, y) != original.getPixel(x, y)) {
                        throw new IllegalStateException("PNG did not round-trip at " + x + "," + y);
                    }
                }
            }
        } finally {
            written.dispose();
        }
    }
}
