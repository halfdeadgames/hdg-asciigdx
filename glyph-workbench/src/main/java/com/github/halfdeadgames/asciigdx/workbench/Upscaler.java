package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.sandbox.PixelScaler;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import java.util.ArrayList;
import java.util.List;

/**
 * Upscales one glyph by a whole factor. Only the drawn part is scaled: a normal glyph's body grows
 * by the factor and its letter gap stays empty and grows with the cell; a full glyph scales the whole
 * cell. The smoothing methods exist for factor 2 only.
 */
final class Upscaler {

    enum Method {
        NEAREST("NEAREST"),
        SCALE2X("SCALE2X"),
        FILL("FILL"),
        TRIM("TRIM"),
        MMPX("MMPX"),
        LIGHT("LIGHT");

        final String label;

        Method(String label) {
            this.label = label;
        }

        /** Lowercase name written to the review file, such as {@code scale2x}. */
        String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** The review file name of a candidate: the method id, with {@code @size} when several source sizes were offered. */
    static String pickId(Method method, int sourceSize, boolean severalSources) {
        return severalSources ? method.id() + "@" + sourceSize : method.id();
    }

    private static final int ON = 0xFFFFFFFF;
    private static final int OFF = 0;

    private Upscaler() {
    }

    /** Nearest always; the smoothing methods only at factor 2. */
    static List<Method> methodsFor(int factor) {
        return factor == 2 ? List.of(Method.values()) : List.of(Method.NEAREST);
    }

    /**
     * Returns the whole target cell: the scaled drawn part, plus the empty gap on the right for a normal glyph.
     *
     * @param targetCellWidth cell width after scaling; a normal glyph's body times the factor plus the gap must equal it
     */
    static PixelGrid upscale(PixelGlyph glyph, int factor, Method method, int targetCellWidth) {
        if (factor < 1 || (factor != 2 && method != Method.NEAREST)) {
            throw new IllegalArgumentException(method + " needs factor 2, but the factor is " + factor);
        }
        PixelGrid pixels = glyph.pixels();
        int width = pixels.getWidth();
        int height = pixels.getHeight();
        int[] packed = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                packed[y * width + x] = pixels.isLit(x, y) ? ON : OFF;
            }
        }
        int[] scaled = scale(packed, width, height, factor, method, PixelScaler.isScale2xLocked(glyph.codePoint()));
        PixelGrid scaledGrid = toGrid(scaled, width * factor, height * factor);
        return new PixelGlyph(glyph.codePoint(), glyph.full(), scaledGrid).toCell(targetCellWidth);
    }

    private static int[] scale(int[] packed, int width, int height, int factor, Method method, boolean locked) {
        switch (method) {
            case SCALE2X:
                return PixelScaler.scale2x(packed, width, height, width, height, slot -> locked,
                    PixelScaler.Scale2xMode.FULL, pixel -> pixel == OFF);
            case FILL:
                return PixelScaler.scale2x(packed, width, height, width, height, slot -> locked,
                    PixelScaler.Scale2xMode.FILL_ONLY, pixel -> pixel == OFF);
            case TRIM:
                return PixelScaler.scale2x(packed, width, height, width, height, slot -> locked,
                    PixelScaler.Scale2xMode.TRIM_ONLY, pixel -> pixel == OFF);
            case MMPX:
                return PixelScaler.mmpx(packed, width, height, width, height, slot -> locked,
                    PixelScaler::lumaRgba8888);
            case LIGHT:
                return PixelScaler.light(packed, width, height, width, height, slot -> locked, pixel -> pixel == OFF);
            default:
                return PixelScaler.scale(packed, width, height, factor);
        }
    }

    private static PixelGrid toGrid(int[] packed, int width, int height) {
        List<String> rows = new ArrayList<>(height);
        for (int y = 0; y < height; y++) {
            StringBuilder row = new StringBuilder(width);
            for (int x = 0; x < width; x++) {
                row.append(packed[y * width + x] == OFF ? '.' : '#');
            }
            rows.add(row.toString());
        }
        return PixelGrid.ofRows(rows);
    }
}
