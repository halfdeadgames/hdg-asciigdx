package com.github.halfdeadgames.asciigdx.glyph;

/**
 * The cell sizes the bundled Saiksistt font is drawn in. Square cells suit maps and gameplay, and
 * tall cells suit long text.
 */
public enum SaiksisttSize {
    SQUARE_8X8(8, 8),
    SQUARE_16X16(16, 16),
    TALL_8X16(8, 16),
    TALL_16X32(16, 32);

    private static final String FOLDER = "com/github/halfdeadgames/asciigdx/fonts/saiksistt/";

    private final int cellWidth;
    private final int cellHeight;

    SaiksisttSize(int cellWidth, int cellHeight) {
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
    }

    public int cellWidth() {
        return cellWidth;
    }

    public int cellHeight() {
        return cellHeight;
    }

    /** Classpath resource path of the sheet image, usable with {@code TextureGlyphSource.loadSheet} for a custom fallback glyph or transparency mode. */
    public String sheetPath() {
        return FOLDER + baseName() + ".png";
    }

    /** Classpath resource path of the character list, usable with {@code TextureGlyphSource.loadSheet} for a custom fallback glyph or transparency mode. */
    public String characterListPath() {
        return FOLDER + baseName() + ".txt";
    }

    String baseName() {
        return "saiksistt-" + cellWidth + "x" + cellHeight;
    }
}
