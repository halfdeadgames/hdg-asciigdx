package com.github.halfdeadgames.asciigdx.grid;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;

public final class TerminalGrid {

    public static final int EMPTY_GLYPH = 0;
    public static final int TRANSPARENT = 0x00000000;

    private final int width;
    private final int height;
    private final int[] glyphs;
    private final int[] foregrounds;
    private final int[] backgrounds;
    private final BitSet dirtyCells;

    public TerminalGrid(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                "Grid size must be positive, was " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        int cellCount = cellCountOf(width, height);
        glyphs = new int[cellCount];
        foregrounds = new int[cellCount];
        backgrounds = new int[cellCount];
        dirtyCells = new BitSet(cellCount);
        dirtyCells.set(0, cellCount);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean contains(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public void set(int x, int y, int codePoint, int foregroundRgba, int backgroundRgba) {
        if (!contains(x, y)) {
            return;
        }
        int index = indexOf(x, y);
        boolean changed = glyphs[index] != codePoint
            || foregrounds[index] != foregroundRgba
            || backgrounds[index] != backgroundRgba;
        if (!changed) {
            return;
        }
        glyphs[index] = codePoint;
        foregrounds[index] = foregroundRgba;
        backgrounds[index] = backgroundRgba;
        dirtyCells.set(index);
    }

    public boolean isDirty(int x, int y) {
        return dirtyCells.get(checkedIndexOf(x, y));
    }

    public boolean hasDirtyCells() {
        return !dirtyCells.isEmpty();
    }

    public void forEachDirtyCell(DirtyCellVisitor visitor) {
        Objects.requireNonNull(visitor, "visitor");
        for (int index = dirtyCells.nextSetBit(0); index >= 0; index = dirtyCells.nextSetBit(index + 1)) {
            visitor.visit(index % width, index / width);
        }
    }

    public void clearDirty() {
        dirtyCells.clear();
    }

    public void markAllDirty() {
        dirtyCells.set(0, glyphs.length);
    }

    public int getGlyph(int x, int y) {
        return glyphs[checkedIndexOf(x, y)];
    }

    public int getForeground(int x, int y) {
        return foregrounds[checkedIndexOf(x, y)];
    }

    public int getBackground(int x, int y) {
        return backgrounds[checkedIndexOf(x, y)];
    }

    public void clear() {
        for (int index = 0; index < glyphs.length; index++) {
            if (glyphs[index] != EMPTY_GLYPH
                || foregrounds[index] != TRANSPARENT
                || backgrounds[index] != TRANSPARENT) {
                dirtyCells.set(index);
            }
        }
        Arrays.fill(glyphs, EMPTY_GLYPH);
        Arrays.fill(foregrounds, TRANSPARENT);
        Arrays.fill(backgrounds, TRANSPARENT);
    }

    private static int cellCountOf(int width, int height) {
        try {
            return Math.multiplyExact(width, height);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException(
                "Grid size " + width + "x" + height + " is too large", overflow);
        }
    }

    private int indexOf(int x, int y) {
        return y * width + x;
    }

    private int checkedIndexOf(int x, int y) {
        if (!contains(x, y)) {
            throw new IndexOutOfBoundsException(
                "Cell (" + x + ", " + y + ") is outside a " + width + "x" + height + " grid");
        }
        return indexOf(x, y);
    }
}
