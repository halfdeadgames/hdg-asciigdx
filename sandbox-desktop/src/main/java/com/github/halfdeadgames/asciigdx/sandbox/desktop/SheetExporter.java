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
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntPredicate;

/**
 * Bakes a TTF into a glyph sheet PNG plus its character list {@code .txt}, without opening a
 * window. Run through the {@code exportSheet} Gradle task, which passes the settings as
 * {@code asciigdx.export.*} system properties.
 */
public final class SheetExporter {

    private static final String BUNDLED_FONT_FOLDER = "com/github/halfdeadgames/asciigdx/fonts/dungeon-mode/";

    private SheetExporter() {
    }

    public static void main(String[] args) {
        GdxNativesLoader.load();
        new SharedLibraryLoader().load("gdx-freetype");
        Gdx.files = new Lwjgl3Files();

        String fontSetting = System.getProperty("asciigdx.export.font", "dungeon-mode");
        int cellHeight = Integer.parseInt(System.getProperty("asciigdx.export.cellHeight", "8"));
        int columns = Integer.parseInt(System.getProperty("asciigdx.export.columns", "16"));
        String charsSetting = System.getProperty("asciigdx.export.chars", "cp437");
        FileHandle outputFolder = Gdx.files.absolute(System.getProperty("asciigdx.export.output"));
        if (System.getProperty("asciigdx.export.generated") != null) {
            exportGenerated(outputFolder, columns);
            return;
        }
        if (System.getProperty("asciigdx.export.assemble") != null) {
            assembleDefaultSheet(outputFolder, columns);
            return;
        }

        FileHandle font = fontFile(fontSetting);
        GlyphOrder order = charsSetting.equalsIgnoreCase("cp437")
            ? GlyphOrder.CP437
            : GlyphOrderText.read(Gdx.files.absolute(charsSetting).readString("UTF-8"), columns);
        String name = System.getProperty("asciigdx.export.sheetName", font.nameWithoutExtension()) + "-" + cellHeight;

        FreeTypeGlyphBaker.Result baked = FreeTypeGlyphBaker.bake(font, cellHeight, order, columns, true);
        try {
            FileHandle png = outputFolder.child(name + ".png");
            FileHandle txt = outputFolder.child(name + ".txt");
            PixmapIO.writePNG(png, baked.getPixmap());
            txt.writeString(GlyphOrderText.write(order, columns), false, "UTF-8");
            verifyRoundTrip(baked, order, columns, png, txt);
            System.out.println("Exported " + order.size() + " glyphs, " + baked.getCellWidth() + "x"
                + baked.getCellHeight() + " cells, " + missingCount(baked, order) + " missing from the font");
            System.out.println("  " + png.file().getAbsolutePath());
            System.out.println("  " + txt.file().getAbsolutePath());
        } finally {
            baked.getPixmap().dispose();
        }
    }

    /** Glyphs taken from the dungeon.mode TTF that the CP437 sheet lacks. */
    private static final String TTF_PICKS = "☠⚇⚉⚱⚷†◇⏺⟁⸙⛑⭦⬚⛨★☝✕";

    // The default sheet: the CP437 sheet's cells first (unchanged), then glyphs baked from the
    // TTF, then the generated glyphs. Code points already placed are skipped. Every cell is
    // stored as white with alpha 0 or 255.
    private static void assembleDefaultSheet(FileHandle outputFolder, int columns) {
        int size = GeneratedGlyphs.SIZE;
        Map<Integer, boolean[][]> glyphs = new LinkedHashMap<>();

        Pixmap cp437 = new Pixmap(Gdx.files.classpath(BUNDLED_FONT_FOLDER + "dungeon-437.png"));
        try {
            int background = cp437.getPixel(0, 0);
            for (int slot = 0; slot < GlyphOrder.CP437.size(); slot++) {
                glyphs.put(GlyphOrder.CP437.codePointAt(slot), cell(cp437, slot, 16, size, pixel -> pixel != background));
            }
        } finally {
            cp437.dispose();
        }

        GlyphOrder picks = GlyphOrder.ofString(TTF_PICKS);
        FreeTypeGlyphBaker.Result baked = FreeTypeGlyphBaker.bake(
            Gdx.files.classpath(BUNDLED_FONT_FOLDER + "dungeon-mode.ttf"), size, picks, columns, true);
        try {
            for (int slot = 0; slot < picks.size(); slot++) {
                int codePoint = picks.codePointAt(slot);
                if (!baked.hasGlyph(codePoint)) {
                    throw new IllegalStateException("TTF lacks pick U+" + Integer.toHexString(codePoint));
                }
                glyphs.putIfAbsent(codePoint, cell(baked.getPixmap(), slot, columns, size, pixel -> (pixel & 0xFF) > 127));
            }
        } finally {
            baked.getPixmap().dispose();
        }

        GeneratedGlyphs.all().forEach(glyphs::putIfAbsent);
        writeSheet(outputFolder, "dungeon-extended", glyphs, columns);
    }

    private static boolean[][] cell(Pixmap sheet, int slot, int columns, int size, IntPredicate isLit) {
        boolean[][] pixels = new boolean[size][size];
        int left = (slot % columns) * size;
        int top = (slot / columns) * size;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                pixels[y][x] = isLit.test(sheet.getPixel(left + x, top + y));
            }
        }
        return pixels;
    }

    private static void exportGenerated(FileHandle outputFolder, int columns) {
        writeSheet(outputFolder, "generated-" + GeneratedGlyphs.SIZE, GeneratedGlyphs.all(), columns);
    }

    private static void writeSheet(FileHandle outputFolder, String name, Map<Integer, boolean[][]> glyphs, int columns) {
        int size = GeneratedGlyphs.SIZE;
        int rows = (glyphs.size() + columns - 1) / columns;
        Pixmap sheet = new Pixmap(columns * size, rows * size, Pixmap.Format.RGBA8888);
        try {
            sheet.setBlending(Pixmap.Blending.None);
            int[] codePoints = new int[glyphs.size()];
            int slot = 0;
            for (Map.Entry<Integer, boolean[][]> glyph : glyphs.entrySet()) {
                codePoints[slot] = glyph.getKey();
                int left = (slot % columns) * size;
                int top = (slot / columns) * size;
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        if (glyph.getValue()[y][x]) {
                            sheet.drawPixel(left + x, top + y, 0xFFFFFFFF);
                        }
                    }
                }
                slot++;
            }
            FileHandle png = outputFolder.child(name + ".png");
            FileHandle txt = outputFolder.child(name + ".txt");
            PixmapIO.writePNG(png, sheet);
            txt.writeString(GlyphOrderText.write(GlyphOrder.ofCodePoints(codePoints), columns), false, "UTF-8");
            System.out.println("Exported " + glyphs.size() + " glyphs, " + columns + "x" + rows + " cells");
            System.out.println("  " + png.file().getAbsolutePath());
            System.out.println("  " + txt.file().getAbsolutePath());
        } finally {
            sheet.dispose();
        }
    }

    private static FileHandle fontFile(String setting) {
        if (setting.equalsIgnoreCase("dungeon-mode") || setting.equalsIgnoreCase("dungeon-mode-inverted")) {
            return Gdx.files.classpath(BUNDLED_FONT_FOLDER + setting.toLowerCase(Locale.ROOT) + ".ttf");
        }
        FileHandle file = Gdx.files.absolute(setting);
        if (!file.exists()) {
            throw new IllegalArgumentException("Font not found: " + setting);
        }
        return file;
    }

    private static int missingCount(FreeTypeGlyphBaker.Result baked, GlyphOrder order) {
        int missing = 0;
        for (int slot = 0; slot < order.size(); slot++) {
            if (!baked.hasGlyph(order.codePointAt(slot))) {
                missing++;
            }
        }
        return missing;
    }

    private static void verifyRoundTrip(
        FreeTypeGlyphBaker.Result baked, GlyphOrder order, int columns, FileHandle png, FileHandle txt) {
        GlyphOrder reread = GlyphOrderText.read(new String(txt.readBytes(), StandardCharsets.UTF_8), columns);
        if (reread.size() != order.size()) {
            throw new IllegalStateException("Character list did not round-trip: size " + reread.size());
        }
        for (int slot = 0; slot < order.size(); slot++) {
            if (reread.codePointAt(slot) != order.codePointAt(slot)) {
                throw new IllegalStateException("Character list did not round-trip at slot " + slot);
            }
        }
        Pixmap written = new Pixmap(png);
        try {
            Pixmap original = baked.getPixmap();
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
