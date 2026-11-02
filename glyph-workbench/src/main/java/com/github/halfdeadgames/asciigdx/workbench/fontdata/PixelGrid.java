package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/** An immutable rectangle of lit and unlit pixels. */
public final class PixelGrid {

    private static final char LIT = '#';
    private static final char UNLIT = '.';

    private final int width;
    private final int height;
    private final BitSet lit;

    private PixelGrid(int width, int height, BitSet lit) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Grid size must be positive, was " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.lit = lit;
    }

    /**
     * Creates an empty grid.
     *
     * @param width columns, must be positive
     * @param height rows, must be positive
     * @return the grid
     */
    public static PixelGrid blank(int width, int height) {
        return new PixelGrid(width, height, new BitSet());
    }

    /**
     * Creates a grid from rows of {@code #} (lit) and {@code .} (unlit).
     *
     * @param rows equally long, non-empty rows
     * @return the grid
     * @throws IllegalArgumentException if rows differ in length or hold other characters
     */
    public static PixelGrid ofRows(List<String> rows) {
        if (rows.isEmpty() || rows.get(0).isEmpty()) {
            throw new IllegalArgumentException("A grid needs at least one pixel");
        }
        int width = rows.get(0).length();
        BitSet lit = new BitSet();
        for (int y = 0; y < rows.size(); y++) {
            String row = rows.get(y);
            if (row.length() != width) {
                throw new IllegalArgumentException("Row " + y + " is " + row.length() + " wide, expected " + width);
            }
            for (int x = 0; x < width; x++) {
                char pixel = row.charAt(x);
                if (pixel != LIT && pixel != UNLIT) {
                    throw new IllegalArgumentException("Row " + y + " has '" + pixel + "' at column " + x);
                }
                lit.set(y * width + x, pixel == LIT);
            }
        }
        return new PixelGrid(width, rows.size(), lit);
    }

    /** Returns the number of columns. */
    public int getWidth() {
        return width;
    }

    /** Returns the number of rows. */
    public int getHeight() {
        return height;
    }

    /**
     * Returns whether a pixel is lit.
     *
     * @param x column
     * @param y row
     * @return {@code true} if lit
     * @throws IndexOutOfBoundsException if outside the grid
     */
    public boolean isLit(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException("Pixel " + x + "," + y + " is outside " + width + "x" + height);
        }
        return lit.get(y * width + x);
    }

    /**
     * Returns a copy with one pixel set or cleared.
     *
     * @throws IndexOutOfBoundsException if outside the grid
     */
    public PixelGrid withPixel(int x, int y, boolean on) {
        isLit(x, y);
        BitSet copy = (BitSet) lit.clone();
        copy.set(y * width + x, on);
        return new PixelGrid(width, height, copy);
    }

    /** Returns the rows as {@code #} and {@code .} text. */
    public List<String> toRows() {
        List<String> rows = new ArrayList<>(height);
        for (int y = 0; y < height; y++) {
            StringBuilder row = new StringBuilder(width);
            for (int x = 0; x < width; x++) {
                row.append(isLit(x, y) ? LIT : UNLIT);
            }
            rows.add(row.toString());
        }
        return rows;
    }

    /** Returns a grid with every pixel flipped between lit and unlit. */
    public PixelGrid inverted() {
        return remap(width, height, (x, y) -> !isLit(x, y));
    }

    /** Returns the grid turned 90 degrees clockwise; it must be square. */
    public PixelGrid rotatedClockwise() {
        requireSquare();
        return remap(width, height, (x, y) -> isLit(y, height - 1 - x));
    }

    /** Returns the grid turned 180 degrees. */
    public PixelGrid rotated180() {
        return remap(width, height, (x, y) -> isLit(width - 1 - x, height - 1 - y));
    }

    /** Returns the grid flipped left to right. */
    public PixelGrid mirrored() {
        return remap(width, height, (x, y) -> isLit(width - 1 - x, y));
    }

    /** Returns the grid flipped top to bottom. */
    public PixelGrid flipped() {
        return remap(width, height, (x, y) -> isLit(x, height - 1 - y));
    }

    /**
     * Returns the grid widened with unlit columns on the right.
     *
     * @param newWidth at least the current width
     * @return the widened grid
     */
    public PixelGrid paddedRight(int newWidth) {
        if (newWidth < width) {
            throw new IllegalArgumentException("Cannot pad width " + width + " down to " + newWidth);
        }
        return remap(newWidth, height, (x, y) -> x < width && isLit(x, y));
    }

    /**
     * Returns this grid with another grid's pixels added, the other moved sideways first.
     *
     * @param other grid of the same size
     * @param shift columns to move {@code other} right (negative moves left)
     * @return the combined grid
     * @throws IllegalArgumentException if sizes differ or a lit pixel of {@code other} leaves the grid
     */
    public PixelGrid withOverlay(PixelGrid other, int shift) {
        if (other.width != width || other.height != height) {
            throw new IllegalArgumentException("Overlay is " + other.width + "x" + other.height
                + " but the grid is " + width + "x" + height);
        }
        BitSet combined = (BitSet) lit.clone();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!other.isLit(x, y)) {
                    continue;
                }
                int target = x + shift;
                if (target < 0 || target >= width) {
                    throw new IllegalArgumentException("Shift " + shift + " moves a pixel from column " + x
                        + " outside the " + width + " column body");
                }
                combined.set(y * width + target);
            }
        }
        return new PixelGrid(width, height, combined);
    }

    private void requireSquare() {
        if (width != height) {
            throw new IllegalArgumentException("Rotating by 90 degrees needs a square grid, but it is "
                + width + "x" + height);
        }
    }

    private PixelGrid remap(int newWidth, int newHeight, PixelRule rule) {
        BitSet result = new BitSet();
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                result.set(y * newWidth + x, rule.isLit(x, y));
            }
        }
        return new PixelGrid(newWidth, newHeight, result);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PixelGrid grid
            && grid.width == width && grid.height == height && grid.lit.equals(lit);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * width + height) + lit.hashCode();
    }

    @Override
    public String toString() {
        return String.join("/", toRows());
    }

    private interface PixelRule {
        boolean isLit(int x, int y);
    }
}
