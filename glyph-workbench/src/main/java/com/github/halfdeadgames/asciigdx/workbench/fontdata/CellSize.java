package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pixel size of one glyph cell.
 *
 * @param width cell width in pixels, at least 1
 * @param height cell height in pixels, at least 1
 */
public record CellSize(int width, int height) {

    private static final Pattern TEXT_FORM = Pattern.compile("(\\d{1,4})x(\\d{1,4})");
    private static final Pattern DATA_FILE = Pattern.compile("(\\d{1,4}x\\d{1,4})\\.txt");

    /** Rejects sizes with a side below 1. */
    public CellSize {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Cell size must be at least 1x1, got " + width + "x" + height);
        }
    }

    /**
     * Reads a size written as {@code <width>x<height>}, like {@code 16x16}.
     *
     * @param text the size text
     * @return the parsed size
     * @throws IllegalArgumentException if the text is not in that form or a side is below 1
     */
    public static CellSize parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Size is missing, expected width x height, like 16x16");
        }
        Matcher matcher = TEXT_FORM.matcher(text);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Size \"" + text + "\" must be width x height, like 16x16");
        }
        return new CellSize(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    }

    /**
     * Reads the size out of a font data file name such as {@code 16x16.txt}.
     *
     * @param fileName a file name without folders
     * @return the size, or null when the name is not a size data file: sidecars like {@code 16x16.guides.txt} and
     *         height-only names like {@code 16.txt} are ignored.
     */
    public static CellSize ofDataFileName(String fileName) {
        Matcher matcher = DATA_FILE.matcher(fileName);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return parse(matcher.group(1));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Whether the cell is square or exactly twice as tall as wide. */
    public boolean isSquareOrTall() {
        return height == width || height == 2 * width;
    }

    /** Returns the size as {@code <width>x<height>}, the stem of font file names. */
    public String fileStem() {
        return width + "x" + height;
    }

    /** Name of the glyph data file, such as {@code 16x16.txt}. */
    public String dataFileName() {
        return fileStem() + ".txt";
    }

    /** Name of the rules file, such as {@code 16x16.rules.json}. */
    public String rulesFileName() {
        return fileStem() + ".rules.json";
    }

    /** Name of the guides file, such as {@code 16x16.guides.txt}. */
    public String guidesFileName() {
        return fileStem() + ".guides.txt";
    }

    @Override
    public String toString() {
        return fileStem();
    }
}
