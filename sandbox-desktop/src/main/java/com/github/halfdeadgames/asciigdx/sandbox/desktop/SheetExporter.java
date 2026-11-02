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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.IntPredicate;

/**
 * Bakes a TTF into a glyph sheet PNG plus its character list {@code .txt}, without opening a
 * window. Run through the {@code exportSheet} Gradle task, which passes the settings as
 * {@code asciigdx.export.*} system properties.
 */
public final class SheetExporter {

    private static final String BUNDLED_FONT_FOLDER = "com/github/halfdeadgames/asciigdx/fonts/dungeon-mode/";

    /** Empty pixels in the exported sheet: RGBA8888 with alpha 0. */
    private static final IntPredicate EXPORT_BACKGROUND = pixel -> (pixel & 0xFF) == 0;

    private SheetExporter() {
    }

    private interface SheetScaler {
        int[] scale(int[] pixels, int width, int height, int cellSize, int[] codePoints);
    }

    private static SheetScaler scale2xScaler(PixelScaler.Scale2xMode scale2xMode) {
        return (pixels, width, height, cellSize, codePoints) ->
            PixelScaler.scale2xGlyphSheet(pixels, width, height, cellSize, codePoints, scale2xMode, EXPORT_BACKGROUND);
    }

    public static void main(String[] args) {
        GdxNativesLoader.load();
        new SharedLibraryLoader().load("gdx-freetype");
        Gdx.files = new Lwjgl3Files();

        String fontSetting = System.getProperty("asciigdx.export.font", "dungeon-mode");
        int cellHeight = Integer.parseInt(System.getProperty("asciigdx.export.cellHeight", "8"));
        int columns = Integer.parseInt(System.getProperty("asciigdx.export.columns", "16"));
        String charsSetting = System.getProperty("asciigdx.export.chars", "cp437");
        String upscale = System.getProperty("asciigdx.export.upscale", "nearest");
        if (upscale.equalsIgnoreCase("hand")) {
            exportHandDrawn(Gdx.files.absolute(System.getProperty("asciigdx.export.output")), columns);
            return;
        }
        if (upscale.equalsIgnoreCase("final")) {
            exportFinal(Gdx.files.absolute(System.getProperty("asciigdx.export.output")), columns);
            return;
        }
        SheetScaler mode = null;
        String suffix = null;
        if (upscale.equalsIgnoreCase("scale2x")) {
            mode = scale2xScaler(PixelScaler.Scale2xMode.FULL);
            suffix = "scale2x";
        } else if (upscale.equalsIgnoreCase("scale2x-fill")) {
            mode = scale2xScaler(PixelScaler.Scale2xMode.FILL_ONLY);
            suffix = "scale2x-fill";
        } else if (upscale.equalsIgnoreCase("scale2x-trim")) {
            mode = scale2xScaler(PixelScaler.Scale2xMode.TRIM_ONLY);
            suffix = "scale2x-trim";
        } else if (upscale.equalsIgnoreCase("mmpx")) {
            mode = (pixels, width, height, cellSize, codePoints) -> PixelScaler.mmpx(
                pixels, width, height, cellSize, cellSize, PixelScaler.lockedSlots(codePoints), PixelScaler::lumaRgba8888);
            suffix = "mmpx";
        } else if (upscale.equalsIgnoreCase("scale2x-sfx")) {
            mode = (pixels, width, height, cellSize, codePoints) -> PixelScaler.scale2xSfx(
                pixels, width, height, cellSize, cellSize, PixelScaler.lockedSlots(codePoints));
            suffix = "scale2x-sfx";
        } else if (upscale.equalsIgnoreCase("light")) {
            mode = (pixels, width, height, cellSize, codePoints) -> PixelScaler.light(
                pixels, width, height, cellSize, cellSize, PixelScaler.lockedSlots(codePoints), EXPORT_BACKGROUND);
            suffix = "light";
        } else if (!upscale.equalsIgnoreCase("nearest")) {
            throw new IllegalArgumentException(
                "upscale must be nearest, scale2x, scale2x-fill, scale2x-trim, mmpx, scale2x-sfx, or light: " + upscale);
        }
        boolean scale2x = mode != null;
        int scale = Integer.parseInt(System.getProperty("asciigdx.export.scale", scale2x ? "2" : "1"));
        if (scale < 1) {
            throw new IllegalArgumentException("scale must be at least 1: " + scale);
        }
        if (scale2x && scale != 2) {
            throw new IllegalArgumentException("upscale=scale2x needs scale 2: " + scale);
        }
        FileHandle outputFolder = Gdx.files.absolute(System.getProperty("asciigdx.export.output"));
        if (System.getProperty("asciigdx.export.generated") != null) {
            exportGenerated(outputFolder, columns, scale, mode, suffix);
            return;
        }
        if (System.getProperty("asciigdx.export.assemble") != null) {
            assembleDefaultSheet(outputFolder, columns, scale, mode, suffix);
            return;
        }
        if (scale2x) {
            throw new IllegalArgumentException("upscale=scale2x only applies to -Passemble or -Pgenerated sheets");
        }

        FileHandle font = fontFile(fontSetting);
        GlyphOrder order = charsSetting.equalsIgnoreCase("cp437")
            ? GlyphOrder.CP437
            : GlyphOrderText.read(Gdx.files.absolute(charsSetting).readString("UTF-8"), columns);
        String name = System.getProperty("asciigdx.export.sheetName", font.nameWithoutExtension()) + "-" + cellHeight * scale;

        FreeTypeGlyphBaker.Result baked = FreeTypeGlyphBaker.bake(font, cellHeight, order, columns, true);
        Pixmap output = scaled(baked.getPixmap(), scale, null, null);
        try {
            FileHandle png = outputFolder.child(name + ".png");
            FileHandle txt = outputFolder.child(name + ".txt");
            PixmapIO.writePNG(png, output);
            txt.writeString(GlyphOrderText.write(order, columns), false, "UTF-8");
            verifyRoundTrip(output, order, columns, png, txt);
            System.out.println("Exported " + order.size() + " glyphs, " + baked.getCellWidth() * scale + "x"
                + baked.getCellHeight() * scale + " cells, " + missingCount(baked, order) + " missing from the font");
            System.out.println("  " + png.file().getAbsolutePath());
            System.out.println("  " + txt.file().getAbsolutePath());
        } finally {
            if (output != baked.getPixmap()) {
                output.dispose();
            }
            baked.getPixmap().dispose();
        }
    }

    // Returns the source itself when scale is 1; the caller disposes the result only if it differs.
    // Non-null scale2xCodePoints (one per cell slot, scale must be 2) selects Scale2x instead of nearest.
    private static Pixmap scaled(Pixmap source, int scale, int[] scale2xCodePoints, SheetScaler mode) {
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
        int[] scaledPixels = scale2xCodePoints == null
            ? PixelScaler.scale(pixels, width, height, scale)
            : mode.scale(pixels, width, height, GeneratedGlyphs.SIZE, scale2xCodePoints);
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

    /** Glyphs taken from the dungeon.mode TTF that the CP437 sheet lacks. */
    private static final String TTF_PICKS = "☠⚇⚉⚱⚷†◇⏺⟁⸙⛑⭦⬚⛨★☝✕";

    // The default sheet: the CP437 sheet's cells first (unchanged), then glyphs baked from the
    // TTF, then the generated glyphs. Code points already placed are skipped. Every cell is
    // stored as white with alpha 0 or 255.
    private static void assembleDefaultSheet(FileHandle outputFolder, int columns, int scale,
        SheetScaler mode, String suffix) {
        int size = GeneratedGlyphs.SIZE;
        Map<Integer, boolean[][]> glyphs = assembleGlyphs(columns);
        String name = scale > 1 ? "dungeon-extended-" + size * scale : "dungeon-extended";
        writeSheet(outputFolder, name, glyphs, columns, scale, mode, suffix);
    }

    private static Map<Integer, boolean[][]> assembleGlyphs(int columns) {
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
        return glyphs;
    }

    // Every cell comes from the candidate picked for its glyph; candidates are computed here, per
    // sheet, with the same scalers as the other upscale options. Cells are white at alpha 0 or 255.
    private static void exportFinal(FileHandle outputFolder, int columns) {
        int size = GeneratedGlyphs.SIZE;
        Map<Integer, boolean[][]> glyphs = assembleGlyphs(columns);
        int slots = glyphs.size();
        int rows = (slots + columns - 1) / columns;
        int width = columns * size;
        int height = rows * size;
        int[] codePoints = new int[slots];
        boolean[] blank = new boolean[slots];
        int[] pixels = new int[width * height];
        int slot = 0;
        for (Map.Entry<Integer, boolean[][]> glyph : glyphs.entrySet()) {
            codePoints[slot] = glyph.getKey();
            blank[slot] = true;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (glyph.getValue()[y][x]) {
                        blank[slot] = false;
                        pixels[((slot / columns) * size + y) * width + (slot % columns) * size + x] = 0xFFFFFFFF;
                    }
                }
            }
            slot++;
        }

        Map<Integer, boolean[][]> handArt = HandDrawnGlyphs.all();
        String picksPath = System.getProperty("asciigdx.export.picks");
        FileHandle picksFile = Gdx.files.absolute(picksPath);
        if (!picksFile.exists()) {
            throw new IllegalArgumentException("Picks file not found: " + picksPath);
        }
        String[] chosen = FinalSheetPicks.select(
            codePoints, blank, FinalSheetPicks.parse(picksFile.readString("UTF-8")), handArt.keySet());

        int outputSize = size * 2;
        int outputWidth = width * 2;
        int[] output = new int[outputWidth * height * 2];
        Map<String, int[]> candidates = new HashMap<>();
        Map<String, Integer> usage = new TreeMap<>();
        for (slot = 0; slot < slots; slot++) {
            String id = chosen[slot];
            usage.merge(id, 1, Integer::sum);
            int left = (slot % columns) * outputSize;
            int top = (slot / columns) * outputSize;
            if (FinalSheetPicks.HAND.equals(id)) {
                boolean[][] art = handArt.get(codePoints[slot]);
                for (int y = 0; y < outputSize; y++) {
                    for (int x = 0; x < outputSize; x++) {
                        output[(top + y) * outputWidth + left + x] = art[y][x] ? 0xFFFFFFFF : 0;
                    }
                }
                continue;
            }
            int[] candidate = candidates.get(id);
            if (candidate == null) {
                candidate = candidatePixels(id, pixels, width, height, codePoints);
                candidates.put(id, candidate);
            }
            for (int y = 0; y < outputSize; y++) {
                System.arraycopy(candidate, (top + y) * outputWidth + left, output, (top + y) * outputWidth + left,
                    outputSize);
            }
        }

        Pixmap sheet = new Pixmap(outputWidth, height * 2, Pixmap.Format.RGBA8888);
        try {
            sheet.setBlending(Pixmap.Blending.None);
            for (int y = 0; y < height * 2; y++) {
                for (int x = 0; x < outputWidth; x++) {
                    sheet.drawPixel(x, y, output[y * outputWidth + x]);
                }
            }
            String name = "dungeon-extended-" + outputSize + "-final";
            FileHandle png = outputFolder.child(name + ".png");
            FileHandle txt = outputFolder.child(name + ".txt");
            PixmapIO.writePNG(png, sheet);
            txt.writeString(GlyphOrderText.write(GlyphOrder.ofCodePoints(codePoints), columns), false, "UTF-8");
            System.out.println("Exported " + slots + " glyphs, " + columns + "x" + rows + " cells; picks used: " + usage);
            System.out.println("  " + png.file().getAbsolutePath());
            System.out.println("  " + txt.file().getAbsolutePath());
        } finally {
            sheet.dispose();
        }
    }

    private static int[] candidatePixels(String id, int[] pixels, int width, int height, int[] codePoints) {
        int size = GeneratedGlyphs.SIZE;
        switch (id) {
            case "nearest":
                return PixelScaler.scale(pixels, width, height, 2);
            case "scale2x":
                return scale2xScaler(PixelScaler.Scale2xMode.FULL).scale(pixels, width, height, size, codePoints);
            case "scale2x-fill":
                return scale2xScaler(PixelScaler.Scale2xMode.FILL_ONLY).scale(pixels, width, height, size, codePoints);
            case "scale2x-trim":
                return scale2xScaler(PixelScaler.Scale2xMode.TRIM_ONLY).scale(pixels, width, height, size, codePoints);
            case "mmpx":
                return PixelScaler.mmpx(pixels, width, height, size, size, PixelScaler.lockedSlots(codePoints),
                    PixelScaler::lumaRgba8888);
            case "scale2x-sfx":
                return PixelScaler.scale2xSfx(pixels, width, height, size, size, PixelScaler.lockedSlots(codePoints));
            case "light":
                return PixelScaler.light(pixels, width, height, size, size, PixelScaler.lockedSlots(codePoints),
                    EXPORT_BACKGROUND);
            default:
                throw new IllegalStateException("No candidate for pick " + id);
        }
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

    private static void exportGenerated(FileHandle outputFolder, int columns, int scale,
        SheetScaler mode, String suffix) {
        writeSheet(outputFolder, "generated-" + GeneratedGlyphs.SIZE * scale, GeneratedGlyphs.all(), columns, scale,
            mode, suffix);
    }

    private static void writeSheet(FileHandle outputFolder, String baseName, Map<Integer, boolean[][]> glyphs,
        int columns, int scale, SheetScaler mode, String suffix) {
        String name = mode != null ? baseName + "-" + suffix : baseName;
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
            Pixmap output = scaled(sheet, scale, mode != null ? codePoints : null, mode);
            try {
                FileHandle png = outputFolder.child(name + ".png");
                FileHandle txt = outputFolder.child(name + ".txt");
                PixmapIO.writePNG(png, output);
                txt.writeString(GlyphOrderText.write(GlyphOrder.ofCodePoints(codePoints), columns), false, "UTF-8");
                System.out.println("Exported " + glyphs.size() + " glyphs, " + columns + "x" + rows + " cells");
                System.out.println("  " + png.file().getAbsolutePath());
                System.out.println("  " + txt.file().getAbsolutePath());
            } finally {
                if (output != sheet) {
                    output.dispose();
                }
            }
        } finally {
            sheet.dispose();
        }
    }

    // Same slots as the default sheet. Cells without a hand-drawn glyph stay empty; the sidecar
    // lists the code points that do have one, so readers can ignore the empty cells.
    private static void exportHandDrawn(FileHandle outputFolder, int columns) {
        String listing = Gdx.files.classpath(BUNDLED_FONT_FOLDER + "dungeon-extended.txt").readString("UTF-8");
        GlyphOrder order = GlyphOrderText.read(listing, columns);
        Map<Integer, boolean[][]> handGlyphs = HandDrawnGlyphs.all();
        for (int codePoint : handGlyphs.keySet()) {
            if (!order.contains(codePoint)) {
                throw new IllegalStateException("Default sheet lacks hand-drawn U+" + Integer.toHexString(codePoint));
            }
        }
        int size = HandDrawnGlyphs.SIZE;
        int rows = (order.size() + columns - 1) / columns;
        int[] presentCodePoints = new int[handGlyphs.size()];
        int presentCount = 0;
        Pixmap sheet = new Pixmap(columns * size, rows * size, Pixmap.Format.RGBA8888);
        try {
            sheet.setBlending(Pixmap.Blending.None);
            for (int slot = 0; slot < order.size(); slot++) {
                boolean[][] glyph = handGlyphs.get(order.codePointAt(slot));
                if (glyph == null) {
                    continue;
                }
                presentCodePoints[presentCount++] = order.codePointAt(slot);
                int left = (slot % columns) * size;
                int top = (slot / columns) * size;
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        if (glyph[y][x]) {
                            sheet.drawPixel(left + x, top + y, 0xFFFFFFFF);
                        }
                    }
                }
            }
            String name = "dungeon-extended-" + size + "-hand";
            FileHandle png = outputFolder.child(name + ".png");
            FileHandle txt = outputFolder.child(name + ".txt");
            FileHandle present = outputFolder.child(name + ".present.txt");
            PixmapIO.writePNG(png, sheet);
            txt.writeString(GlyphOrderText.write(order, columns), false, "UTF-8");
            present.writeString(GlyphOrderText.write(
                GlyphOrder.ofCodePoints(Arrays.copyOf(presentCodePoints, presentCount)), columns),
                false, "UTF-8");
            System.out.println("Exported " + presentCount + " hand-drawn glyphs, " + columns + "x" + rows + " cells");
            System.out.println("  " + png.file().getAbsolutePath());
            System.out.println("  " + txt.file().getAbsolutePath());
            System.out.println("  " + present.file().getAbsolutePath());
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
        Pixmap original, GlyphOrder order, int columns, FileHandle png, FileHandle txt) {
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
