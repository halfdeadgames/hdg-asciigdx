package com.github.halfdeadgames.asciigdx.sandbox;

import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

/** Integer nearest-neighbour upscaling of packed pixel arrays: every pixel becomes a square block, values untouched. */
public final class PixelScaler {

    private PixelScaler() {
    }

    public static int[] scale(int[] pixels, int width, int height, int scale) {
        if (scale < 1) {
            throw new IllegalArgumentException("scale must be at least 1: " + scale);
        }
        if (pixels.length != width * height) {
            throw new IllegalArgumentException("pixels length " + pixels.length + " does not match " + width + "x" + height);
        }
        int scaledWidth = width * scale;
        int[] scaled = new int[scaledWidth * height * scale];
        for (int y = 0; y < height * scale; y++) {
            int sourceRow = (y / scale) * width;
            int targetRow = y * scaledWidth;
            for (int x = 0; x < scaledWidth; x++) {
                scaled[targetRow + x] = pixels[sourceRow + x / scale];
            }
        }
        return scaled;
    }

    /** Glyphs that must stay plain 2x under Scale2x: line art, blocks, and private-use pieces that tile or layer. */
    public static boolean isScale2xLocked(int codePoint) {
        return (codePoint >= 0x2500 && codePoint <= 0x259F)
            || (codePoint >= 0xE000 && codePoint <= 0xF8FF);
    }

    /**
     * Scale2x (EPX) of a sheet made of cellWidth x cellHeight cells. Each cell is scaled on its own and
     * edge pixels stand in for neighbours outside the cell. Cells for which {@code locked} returns true
     * (by slot, row-major) are scaled as plain 2x. Output pixels are copies of input pixels.
     */
    public static int[] scale2x(int[] pixels, int width, int height, int cellWidth, int cellHeight, IntPredicate locked) {
        return scale2x(pixels, width, height, cellWidth, cellHeight, locked, Scale2xMode.FULL, pixel -> false);
    }

    /** Which Scale2x changes are kept; the rest stay as nearest-neighbour 2x. */
    public enum Scale2xMode {
        /** Every Scale2x change. */
        FULL,
        /** Only changes that turn a background sub-pixel into foreground, so pixels are only added. */
        FILL_ONLY,
        /** Only changes that turn a foreground sub-pixel into background, so pixels are only removed. */
        TRIM_ONLY
    }

    /** Same as the plain variant, with {@code mode} filtering which changes apply; {@code background} tells empty pixels apart. */
    public static int[] scale2x(int[] pixels, int width, int height, int cellWidth, int cellHeight, IntPredicate locked,
        Scale2xMode mode, IntPredicate background) {
        if (pixels.length != width * height) {
            throw new IllegalArgumentException("pixels length " + pixels.length + " does not match " + width + "x" + height);
        }
        if (width % cellWidth != 0 || height % cellHeight != 0) {
            throw new IllegalArgumentException("size " + width + "x" + height + " is not a whole number of "
                + cellWidth + "x" + cellHeight + " cells");
        }
        int columns = width / cellWidth;
        int scaledWidth = width * 2;
        int[] scaled = new int[scaledWidth * height * 2];
        for (int y = 0; y < height; y++) {
            int cellTop = y - y % cellHeight;
            int cellBottom = cellTop + cellHeight - 1;
            for (int x = 0; x < width; x++) {
                int cellLeft = x - x % cellWidth;
                int cellRight = cellLeft + cellWidth - 1;
                int e = pixels[y * width + x];
                int e0 = e;
                int e1 = e;
                int e2 = e;
                int e3 = e;
                if (!locked.test((y / cellHeight) * columns + x / cellWidth)) {
                    int b = pixels[Math.max(y - 1, cellTop) * width + x];
                    int h = pixels[Math.min(y + 1, cellBottom) * width + x];
                    int d = pixels[y * width + Math.max(x - 1, cellLeft)];
                    int f = pixels[y * width + Math.min(x + 1, cellRight)];
                    if (b != h && d != f) {
                        e0 = d == b ? d : e;
                        e1 = b == f ? f : e;
                        e2 = d == h ? d : e;
                        e3 = h == f ? f : e;
                        e0 = filtered(mode, background, e, e0);
                        e1 = filtered(mode, background, e, e1);
                        e2 = filtered(mode, background, e, e2);
                        e3 = filtered(mode, background, e, e3);
                    }
                }
                int target = y * 2 * scaledWidth + x * 2;
                scaled[target] = e0;
                scaled[target + 1] = e1;
                scaled[target + scaledWidth] = e2;
                scaled[target + scaledWidth + 1] = e3;
            }
        }
        return scaled;
    }

    private static int filtered(Scale2xMode mode, IntPredicate background, int nearest, int smooth) {
        switch (mode) {
            case FILL_ONLY:
                return background.test(nearest) && !background.test(smooth) ? smooth : nearest;
            case TRIM_ONLY:
                return !background.test(nearest) && background.test(smooth) ? smooth : nearest;
            default:
                return smooth;
        }
    }

    /** Slot predicate for sheets where {@code codePoints[slot]} names the glyph in each cell: true for locked glyphs. */
    public static IntPredicate lockedSlots(int[] codePoints) {
        return slot -> slot < codePoints.length && isScale2xLocked(codePoints[slot]);
    }

    /** Brightness of an RGBA8888 pixel (alpha in the low byte); fully transparent pixels are the darkest. */
    public static int lumaRgba8888(int pixel) {
        return ((pixel >>> 24) + ((pixel >>> 16) & 0xFF) + ((pixel >>> 8) & 0xFF)) * (pixel & 0xFF);
    }

    /** Scales one cell; {@code cell} is cellWidth x cellHeight and the result is twice that in each direction. */
    private interface CellScaler {
        int[] scale(int[] cell, int cellWidth, int cellHeight);
    }

    private static int[] scaleCells(int[] pixels, int width, int height, int cellWidth, int cellHeight,
        IntPredicate locked, CellScaler scaler) {
        if (pixels.length != width * height) {
            throw new IllegalArgumentException("pixels length " + pixels.length + " does not match " + width + "x" + height);
        }
        if (width % cellWidth != 0 || height % cellHeight != 0) {
            throw new IllegalArgumentException("size " + width + "x" + height + " is not a whole number of "
                + cellWidth + "x" + cellHeight + " cells");
        }
        int columns = width / cellWidth;
        int scaledWidth = width * 2;
        int[] scaled = new int[scaledWidth * height * 2];
        int[] cell = new int[cellWidth * cellHeight];
        for (int top = 0; top < height; top += cellHeight) {
            for (int left = 0; left < width; left += cellWidth) {
                for (int y = 0; y < cellHeight; y++) {
                    System.arraycopy(pixels, (top + y) * width + left, cell, y * cellWidth, cellWidth);
                }
                int slot = (top / cellHeight) * columns + left / cellWidth;
                int[] result = locked.test(slot) ? scale(cell, cellWidth, cellHeight, 2) : scaler.scale(cell, cellWidth, cellHeight);
                int resultWidth = cellWidth * 2;
                for (int y = 0; y < cellHeight * 2; y++) {
                    System.arraycopy(result, y * resultWidth, scaled, (top * 2 + y) * scaledWidth + left * 2, resultWidth);
                }
            }
        }
        return scaled;
    }

    private static int at(int[] cell, int width, int height, int x, int y) {
        return cell[Math.min(Math.max(y, 0), height - 1) * width + Math.min(Math.max(x, 0), width - 1)];
    }

    /**
     * MMPX (McGuire and Gagiu, "MMPX Style-Preserving Pixel Art Magnification", JCGT 2021) per cell.
     * {@code luma} ranks pixel brightness; edge pixels stand in for neighbours outside the cell.
     */
    public static int[] mmpx(int[] pixels, int width, int height, int cellWidth, int cellHeight, IntPredicate locked,
        IntUnaryOperator luma) {
        return scaleCells(pixels, width, height, cellWidth, cellHeight, locked,
            (cell, cols, rows) -> mmpxCell(cell, cols, rows, luma));
    }

    private static int[] mmpxCell(int[] cell, int cols, int rows, IntUnaryOperator luma) {
        int outWidth = cols * 2;
        int[] out = new int[outWidth * rows * 2];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int a = at(cell, cols, rows, x - 1, y - 1);
                int b = at(cell, cols, rows, x, y - 1);
                int c = at(cell, cols, rows, x + 1, y - 1);
                int d = at(cell, cols, rows, x - 1, y);
                int e = at(cell, cols, rows, x, y);
                int f = at(cell, cols, rows, x + 1, y);
                int g = at(cell, cols, rows, x - 1, y + 1);
                int h = at(cell, cols, rows, x, y + 1);
                int i = at(cell, cols, rows, x + 1, y + 1);
                int j = e;
                int k = e;
                int l = e;
                int m = e;
                if (((a ^ e) | (b ^ e) | (c ^ e) | (d ^ e) | (f ^ e) | (g ^ e) | (h ^ e) | (i ^ e)) != 0) {
                    int p = at(cell, cols, rows, x, y - 2);
                    int s = at(cell, cols, rows, x, y + 2);
                    int q = at(cell, cols, rows, x - 2, y);
                    int r = at(cell, cols, rows, x + 2, y);
                    int bl = luma.applyAsInt(b);
                    int dl = luma.applyAsInt(d);
                    int el = luma.applyAsInt(e);
                    int fl = luma.applyAsInt(f);
                    int hl = luma.applyAsInt(h);

                    // 1:1 slope rules
                    if (d == b && d != h && d != f && (el >= dl || e == a) && anyEq3(e, a, c, g)
                        && (el < dl || a != d || e != p || e != q)) {
                        j = d;
                    }
                    if (b == f && b != d && b != h && (el >= bl || e == c) && anyEq3(e, a, c, i)
                        && (el < bl || c != b || e != p || e != r)) {
                        k = b;
                    }
                    if (h == d && h != f && h != b && (el >= hl || e == g) && anyEq3(e, a, g, i)
                        && (el < hl || g != h || e != s || e != q)) {
                        l = h;
                    }
                    if (f == h && f != b && f != d && (el >= fl || e == i) && anyEq3(e, c, g, i)
                        && (el < fl || i != h || e != r || e != s)) {
                        m = f;
                    }

                    // Intersection rules
                    if (e != f && allEq4(e, c, i, d, q) && allEq2(f, b, h) && f != at(cell, cols, rows, x + 3, y)) {
                        k = f;
                        m = f;
                    }
                    if (e != d && allEq4(e, a, g, f, r) && allEq2(d, b, h) && d != at(cell, cols, rows, x - 3, y)) {
                        j = d;
                        l = d;
                    }
                    if (e != h && allEq4(e, g, i, b, p) && allEq2(h, d, f) && h != at(cell, cols, rows, x, y + 3)) {
                        l = h;
                        m = h;
                    }
                    if (e != b && allEq4(e, a, c, h, s) && allEq2(b, d, f) && b != at(cell, cols, rows, x, y - 3)) {
                        j = b;
                        k = b;
                    }

                    // Isolated pixel clean-up
                    if (bl < el && allEq4(e, g, h, i, s) && noneEq4(e, a, d, c, f)) {
                        j = b;
                        k = b;
                    }
                    if (hl < el && allEq4(e, a, b, c, p) && noneEq4(e, d, g, i, f)) {
                        l = h;
                        m = h;
                    }
                    if (fl < el && allEq4(e, a, d, g, q) && noneEq4(e, b, c, i, h)) {
                        k = f;
                        m = f;
                    }
                    if (dl < el && allEq4(e, c, f, i, r) && noneEq4(e, b, a, g, h)) {
                        j = d;
                        l = d;
                    }

                    // 2:1 slope rules
                    if (h != b) {
                        if (h != a && h != e && h != c) {
                            if (allEq3(h, g, f, r) && noneEq2(h, d, at(cell, cols, rows, x + 2, y - 1))) {
                                l = m;
                            }
                            if (allEq3(h, i, d, q) && noneEq2(h, f, at(cell, cols, rows, x - 2, y - 1))) {
                                m = l;
                            }
                        }
                        if (b != i && b != g && b != e) {
                            if (allEq3(b, a, f, r) && noneEq2(b, d, at(cell, cols, rows, x + 2, y + 1))) {
                                j = k;
                            }
                            if (allEq3(b, c, d, q) && noneEq2(b, f, at(cell, cols, rows, x - 2, y + 1))) {
                                k = j;
                            }
                        }
                    }
                    if (f != d) {
                        if (d != i && d != e && d != c) {
                            if (allEq3(d, a, h, s) && noneEq2(d, b, at(cell, cols, rows, x + 1, y + 2))) {
                                j = l;
                            }
                            if (allEq3(d, g, b, p) && noneEq2(d, h, at(cell, cols, rows, x + 1, y - 2))) {
                                l = j;
                            }
                        }
                        if (f != e && f != a && f != g) {
                            if (allEq3(f, c, h, s) && noneEq2(f, b, at(cell, cols, rows, x - 1, y + 2))) {
                                k = m;
                            }
                            if (allEq3(f, i, b, p) && noneEq2(f, h, at(cell, cols, rows, x - 1, y - 2))) {
                                m = k;
                            }
                        }
                    }
                }
                int target = y * 2 * outWidth + x * 2;
                out[target] = j;
                out[target + 1] = k;
                out[target + outWidth] = l;
                out[target + outWidth + 1] = m;
            }
        }
        return out;
    }

    private static boolean anyEq3(int value, int a, int b, int c) {
        return value == a || value == b || value == c;
    }

    private static boolean allEq2(int value, int a, int b) {
        return value == a && value == b;
    }

    private static boolean allEq3(int value, int a, int b, int c) {
        return value == a && value == b && value == c;
    }

    private static boolean allEq4(int value, int a, int b, int c, int d) {
        return value == a && value == b && value == c && value == d;
    }

    private static boolean noneEq2(int value, int a, int b) {
        return value != a && value != b;
    }

    private static boolean noneEq4(int value, int a, int b, int c, int d) {
        return value != a && value != b && value != c && value != d;
    }

    /** Scale2xSFX (Sp00kyFox's Scale2x variant that looks two pixels out along the axes) per cell. */
    public static int[] scale2xSfx(int[] pixels, int width, int height, int cellWidth, int cellHeight, IntPredicate locked) {
        return scaleCells(pixels, width, height, cellWidth, cellHeight, locked, PixelScaler::scale2xSfxCell);
    }

    private static int[] scale2xSfxCell(int[] cell, int cols, int rows) {
        int outWidth = cols * 2;
        int[] out = new int[outWidth * rows * 2];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int a = at(cell, cols, rows, x - 1, y - 1);
                int b = at(cell, cols, rows, x, y - 1);
                int c = at(cell, cols, rows, x + 1, y - 1);
                int d = at(cell, cols, rows, x - 1, y);
                int e = at(cell, cols, rows, x, y);
                int f = at(cell, cols, rows, x + 1, y);
                int g = at(cell, cols, rows, x - 1, y + 1);
                int h = at(cell, cols, rows, x, y + 1);
                int i = at(cell, cols, rows, x + 1, y + 1);
                int up = at(cell, cols, rows, x, y - 2);
                int left = at(cell, cols, rows, x - 2, y);
                int right = at(cell, cols, rows, x + 2, y);
                int down = at(cell, cols, rows, x, y + 2);
                int e0 = d == b && b != f && d != h && (e != a || e == c || e == g || a != up || a != left) ? d : e;
                int e1 = b == f && b != d && f != h && (e != c || e == a || e == i || c != up || c != right) ? f : e;
                int e2 = d == h && d != b && h != f && (e != g || e == a || e == i || g != left || g != down) ? d : e;
                int e3 = h == f && d != h && b != f && (e != i || e == c || e == g || i != right || i != down) ? f : e;
                int target = y * 2 * outWidth + x * 2;
                out[target] = e0;
                out[target + 1] = e1;
                out[target + outWidth] = e2;
                out[target + outWidth + 1] = e3;
            }
        }
        return out;
    }

    /**
     * Plain 2x per cell, then every foreground pixel in the right half of its source pixel is cleared when
     * its right neighbour inside the cell is background and its left neighbour is foreground. Vertical
     * strokes end up 1 px wide while horizontal strokes keep their 2 px height.
     */
    public static int[] light(int[] pixels, int width, int height, int cellWidth, int cellHeight, IntPredicate locked,
        IntPredicate background) {
        return scaleCells(pixels, width, height, cellWidth, cellHeight, locked,
            (cell, cols, rows) -> lightCell(cell, cols, rows, background));
    }

    private static int[] lightCell(int[] cell, int cols, int rows, IntPredicate background) {
        int[] out = scale(cell, cols, rows, 2);
        int outWidth = cols * 2;
        for (int y = 0; y < rows * 2; y++) {
            for (int x = 1; x < outWidth - 1; x += 2) {
                int index = y * outWidth + x;
                if (!background.test(out[index]) && background.test(out[index + 1]) && !background.test(out[index - 1])) {
                    out[index] = out[index + 1];
                }
            }
        }
        return out;
    }

    /** Scale2x of a sheet where {@code codePoints[slot]} names the glyph in each cell; locked glyphs stay plain 2x. */
    public static int[] scale2xGlyphSheet(int[] pixels, int width, int height, int cellSize, int[] codePoints) {
        return scale2xGlyphSheet(pixels, width, height, cellSize, codePoints, Scale2xMode.FULL, pixel -> false);
    }

    public static int[] scale2xGlyphSheet(int[] pixels, int width, int height, int cellSize, int[] codePoints,
        Scale2xMode mode, IntPredicate background) {
        return scale2x(pixels, width, height, cellSize, cellSize,
            slot -> slot < codePoints.length && isScale2xLocked(codePoints[slot]), mode, background);
    }
}
