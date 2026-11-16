package com.github.halfdeadgames.asciigdx.glyph;

/**
 * The cell sizes the bundled Saiksistt font is drawn in. Square cells suit maps and gameplay, and
 * tall cells suit long text. Pass a size to
 * {@link TextureGlyphSource#loadSaiksistt(SaiksisttSize)}.
 */
public enum SaiksisttSize {

    /** Square cells, 8 pixels wide and 8 pixels high. */
    SQUARE_8X8(8, 8),

    /** Square cells, 16 pixels wide and 16 pixels high. This is the default size. */
    SQUARE_16X16(16, 16),

    /** Tall cells, 8 pixels wide and 16 pixels high. */
    TALL_8X16(8, 16),

    /** Tall cells, 16 pixels wide and 32 pixels high. */
    TALL_16X32(16, 32);

    private static final String FOLDER = "com/github/halfdeadgames/asciigdx/fonts/saiksistt/";

    private final int cellWidth;
    private final int cellHeight;

    SaiksisttSize(int cellWidth, int cellHeight) {
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
    }

    /**
     * Returns the width of one cell.
     *
     * @return the width in pixels; always positive
     */
    public int getCellWidth() {
        return cellWidth;
    }

    /**
     * Returns the height of one cell.
     *
     * @return the height in pixels; always positive
     */
    public int getCellHeight() {
        return cellHeight;
    }

    /**
     * Returns the classpath resource path of the sheet image. Open it with
     * {@code Gdx.files.classpath(path)}. Use it with
     * {@link TextureGlyphSource#loadSheet(com.badlogic.gdx.files.FileHandle,
     * com.badlogic.gdx.files.FileHandle, int, int, TransparencyMode)}
     * to load the font with a transparency mode of your choice.
     *
     * @return the path, relative to the classpath root; never null
     */
    public String getSheetPath() {
        return FOLDER + baseName() + ".png";
    }

    /**
     * Returns the classpath resource path of the character list, the UTF-8 text file that names
     * the glyph in each cell of the sheet (see {@link GlyphOrderText}). Open it with
     * {@code Gdx.files.classpath(path)}. Use it together with {@link #getSheetPath()}.
     *
     * @return the path, relative to the classpath root; never null
     */
    public String getCharacterListPath() {
        return FOLDER + baseName() + ".txt";
    }

    String baseName() {
        return "saiksistt-" + cellWidth + "x" + cellHeight;
    }
}
