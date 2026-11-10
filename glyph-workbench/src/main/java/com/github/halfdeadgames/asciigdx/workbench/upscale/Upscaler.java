package com.github.halfdeadgames.asciigdx.workbench.upscale;

import com.github.halfdeadgames.asciigdx.sandbox.PixelScaler;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * Upscales one glyph by a whole factor per axis. The entire cell is scaled. The smoothing methods exist
 * only at factor 2 on both axes.
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
    }

    private static final int ON = 0xFFFFFFFF;
    private static final int OFF = 0;
    private static final IntPredicate NO_LOCKED_SLOTS = slot -> false;

    private Upscaler() {
    }

    /** Nearest always; the smoothing methods only when both factors are 2. */
    static List<Method> methodsFor(int factorX, int factorY) {
        return factorX == 2 && factorY == 2 ? List.of(Method.values()) : List.of(Method.NEAREST);
    }

    /** The factor as shown to the user: {@code 2X} for equal factors, {@code 2X/1X} otherwise. */
    static String factorLabel(int factorX, int factorY) {
        return factorX == factorY ? factorX + "X" : factorX + "X/" + factorY + "X";
    }

    /**
     * Returns the entire target cell: the source cell scaled by a whole factor across and another down.
     */
    static PixelGrid upscale(PixelGlyph glyph, int factorX, int factorY, Method method) {
        if (factorX < 1 || factorY < 1) {
            throw new IllegalArgumentException("Factors must be at least 1, but they are " + factorX + " and " + factorY);
        }
        if (method != Method.NEAREST && (factorX != 2 || factorY != 2)) {
            throw new IllegalArgumentException(method + " needs factor 2 on both axes, but the factors are "
                + factorX + " and " + factorY);
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
        int[] scaled = scale(packed, width, height, factorX, factorY, method);
        return toGrid(scaled, width * factorX, height * factorY);
    }

    private static int[] scale(int[] packed, int width, int height, int factorX, int factorY, Method method) {
        switch (method) {
            case SCALE2X:
                return PixelScaler.scale2x(packed, width, height, width, height, NO_LOCKED_SLOTS,
                    PixelScaler.Scale2xMode.FULL, pixel -> pixel == OFF);
            case FILL:
                return PixelScaler.scale2x(packed, width, height, width, height, NO_LOCKED_SLOTS,
                    PixelScaler.Scale2xMode.FILL_ONLY, pixel -> pixel == OFF);
            case TRIM:
                return PixelScaler.scale2x(packed, width, height, width, height, NO_LOCKED_SLOTS,
                    PixelScaler.Scale2xMode.TRIM_ONLY, pixel -> pixel == OFF);
            case MMPX:
                return PixelScaler.mmpx(packed, width, height, width, height, NO_LOCKED_SLOTS,
                    PixelScaler::lumaRgba8888);
            case LIGHT:
                return PixelScaler.light(packed, width, height, width, height, NO_LOCKED_SLOTS,
                    pixel -> pixel == OFF);
            default:
                return repeatPixels(packed, width, height, factorX, factorY);
        }
    }

    private static int[] repeatPixels(int[] packed, int width, int height, int factorX, int factorY) {
        int targetWidth = width * factorX;
        int[] repeated = new int[targetWidth * height * factorY];
        for (int y = 0; y < height * factorY; y++) {
            int sourceRow = (y / factorY) * width;
            for (int x = 0; x < targetWidth; x++) {
                repeated[y * targetWidth + x] = packed[sourceRow + x / factorX];
            }
        }
        return repeated;
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
