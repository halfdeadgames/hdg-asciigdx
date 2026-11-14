package com.github.halfdeadgames.asciigdx.glyph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.Objects;
import java.util.function.IntPredicate;

/**
 * A {@link GlyphSource} backed by a single sheet image uploaded as one {@link Texture}.
 *
 * <p>The texture uses no mipmaps and clamped edges. By default it uses nearest filtering so glyph
 * pixels stay sharp; pass {@link GlyphFilter#LINEAR} for smooth scaling and rotation, which
 * uploads a slightly larger sheet with a transparent gutter around every glyph. Slots of the
 * layout beyond the size of the {@link GlyphOrder} are unused.
 *
 * <p>Must be created and disposed on the render thread, because it uploads a texture.
 */
public final class TextureGlyphSource implements GlyphSource {

    /** Fallback code point used by the short {@code load} overload. */
    public static final int DEFAULT_FALLBACK_CODE_POINT = '?';

    private final Texture texture;
    private final GlyphOrder order;
    private final TextureRegion[] regionsBySlot;
    private final TextureRegion fallbackRegion;
    private final IntPredicate presentInSheet;
    private final int cellWidth;
    private final int cellHeight;
    private boolean disposed;

    private TextureGlyphSource(
        Texture texture, GlyphSheetLayout layout, GlyphOrder order, int fallbackSlot, IntPredicate presentInSheet) {
        this.texture = texture;
        this.order = order;
        this.presentInSheet = presentInSheet;
        cellWidth = layout.getCellWidth();
        cellHeight = layout.getCellHeight();
        regionsBySlot = new TextureRegion[order.size()];
        TextureRegion fallback = new TextureRegion(
            texture, layout.slotX(fallbackSlot), layout.slotY(fallbackSlot), cellWidth, cellHeight);
        for (int slot = 0; slot < regionsBySlot.length; slot++) {
            regionsBySlot[slot] = presentInSheet.test(order.codePointAt(slot))
                ? new TextureRegion(texture, layout.slotX(slot), layout.slotY(slot), cellWidth, cellHeight)
                : fallback;
        }
        fallbackRegion = fallback;
    }

    /**
     * Loads a sheet using its top-left pixel as the transparency key and {@code '?'} as the
     * fallback glyph.
     *
     * @throws IllegalArgumentException if the image is too small, the order has more glyphs
     *         than the layout has slots, or the order lacks the fallback
     */
    public static TextureGlyphSource load(
        FileHandle file, GlyphSheetLayout layout, GlyphOrder order, TransparencyMode mode) {
        return load(file, layout, order, mode, null, DEFAULT_FALLBACK_CODE_POINT);
    }

    /**
     * Loads a sheet image and cuts it into glyph regions.
     *
     * <p>If the mode asks for conversion, every pixel becomes white with an alpha derived
     * from its distance to the key color (see {@link ColorKeyConversion}).
     *
     * @param file the image file
     * @param layout where the glyph cells are in the image
     * @param order which code point each slot holds
     * @param mode whether to convert the image to white glyphs with alpha
     * @param keyRgba8888OrNull the key color as an RGBA8888 int, or null to use the top-left pixel
     * @param fallbackCodePoint code point whose glyph stands in for missing ones; must be in the order
     * @return the loaded source
     * @throws IllegalArgumentException if the image is smaller than the layout needs, the order has
     *         more glyphs than the layout has slots, or the order lacks the fallback
     */
    public static TextureGlyphSource load(
        FileHandle file,
        GlyphSheetLayout layout,
        GlyphOrder order,
        TransparencyMode mode,
        Integer keyRgba8888OrNull,
        int fallbackCodePoint) {
        Objects.requireNonNull(file, "file must not be null");
        Objects.requireNonNull(layout, "layout must not be null");
        Objects.requireNonNull(order, "order must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
        requireOrderFits(order, layout);
        int fallbackSlot = order.slotOf(fallbackCodePoint);
        if (fallbackSlot < 0) {
            throw new IllegalArgumentException(
                "Order has no glyph for fallback code point " + fallbackCodePoint);
        }

        return convertAndUpload(
            new Pixmap(file), file.path(), layout, order, mode, keyRgba8888OrNull, fallbackSlot, GlyphFilter.NEAREST);
    }

    /**
     * Loads a sheet image together with its character list, the {@code .txt} file that names
     * the glyph in each cell (see {@link GlyphOrderText}). The number of columns is the image
     * width divided by the cell width; the last row may be partly empty. Missing glyphs draw
     * {@code '?'}, which the character list must contain.
     *
     * @param image the sheet image
     * @param characterList the character list, UTF-8
     * @param cellWidth width of one cell in pixels, must be positive
     * @param cellHeight height of one cell in pixels, must be positive
     * @param mode whether to convert the image to white glyphs with alpha
     * @return the loaded source
     * @throws IllegalArgumentException if a cell size is not positive, the image is narrower than
     *         one cell, the character list is malformed or does not fit the image, or it lacks
     *         {@code '?'}
     */
    public static TextureGlyphSource loadSheet(
        FileHandle image, FileHandle characterList, int cellWidth, int cellHeight, TransparencyMode mode) {
        return loadSheet(image, characterList, cellWidth, cellHeight, mode, GlyphFilter.NEAREST);
    }

    /**
     * Same as {@link #loadSheet(FileHandle, FileHandle, int, int, TransparencyMode)}, with a
     * choice of texture filter.
     *
     * @param filter how the texture is sampled; {@link GlyphFilter#LINEAR} uploads a repacked,
     *        slightly larger copy of the sheet
     */
    public static TextureGlyphSource loadSheet(
        FileHandle image,
        FileHandle characterList,
        int cellWidth,
        int cellHeight,
        TransparencyMode mode,
        GlyphFilter filter) {
        Objects.requireNonNull(image, "image must not be null");
        Objects.requireNonNull(characterList, "characterList must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
        Objects.requireNonNull(filter, "filter must not be null");
        if (cellWidth <= 0 || cellHeight <= 0) {
            throw new IllegalArgumentException("Cell size must be positive, was " + cellWidth + "x" + cellHeight);
        }
        String text = characterList.readString("UTF-8");
        Pixmap pixmap = new Pixmap(image);
        GlyphSheetLayout layout;
        GlyphOrder order;
        int fallbackSlot;
        try {
            int columns = pixmap.getWidth() / cellWidth;
            if (columns == 0) {
                throw new IllegalArgumentException(
                    "Image " + image.path() + " is narrower than one " + cellWidth + " pixel cell");
            }
            order = GlyphOrderText.read(text, columns);
            layout = GlyphSheetLayout.grid(cellWidth, cellHeight, columns, (order.size() + columns - 1) / columns);
            fallbackSlot = order.slotOf(DEFAULT_FALLBACK_CODE_POINT);
            if (fallbackSlot < 0) {
                throw new IllegalArgumentException(
                    "Character list " + characterList.path() + " has no '?' to use as the fallback glyph");
            }
        } catch (RuntimeException e) {
            pixmap.dispose();
            throw e;
        }
        return convertAndUpload(pixmap, image.path(), layout, order, mode, null, fallbackSlot, filter);
    }

    private static void requireOrderFits(GlyphOrder order, GlyphSheetLayout layout) {
        if (order.size() > layout.slotCount()) {
            throw new IllegalArgumentException(
                "Order has " + order.size() + " glyphs but the layout has only "
                    + layout.slotCount() + " slots");
        }
    }

    private static void requireImageFits(Pixmap image, String imageName, GlyphSheetLayout layout) {
        if (image.getWidth() < layout.requiredImageWidth() || image.getHeight() < layout.requiredImageHeight()) {
            throw new IllegalArgumentException(
                imageName + " is " + image.getWidth() + "x" + image.getHeight()
                    + " but the layout needs at least " + layout.requiredImageWidth()
                    + "x" + layout.requiredImageHeight());
        }
    }

    // Takes ownership of the pixmap and disposes it, also when this method throws.
    private static TextureGlyphSource convertAndUpload(
        Pixmap pixmap,
        String path,
        GlyphSheetLayout layout,
        GlyphOrder order,
        TransparencyMode mode,
        Integer keyRgba8888OrNull,
        int fallbackSlot,
        GlyphFilter filter) {
        try {
            requireImageFits(pixmap, "Image " + path, layout);
            int key = keyRgba8888OrNull != null ? keyRgba8888OrNull : pixmap.getPixel(0, 0);
            if (mode.shouldConvert(hasNonOpaquePixel(pixmap))) {
                if (pixmap.getFormat() != Pixmap.Format.RGBA8888) {
                    Pixmap rgba = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), Pixmap.Format.RGBA8888);
                    Pixmap original = pixmap;
                    pixmap = rgba;
                    try {
                        rgba.setBlending(Pixmap.Blending.None);
                        rgba.drawPixmap(original, 0, 0);
                    } finally {
                        original.dispose();
                    }
                }
                pixmap.setBlending(Pixmap.Blending.None);
                convertToWhiteWithAlpha(pixmap, key);
            }

            return upload(pixmap, layout, order, fallbackSlot, codePoint -> true, filter);
        } finally {
            pixmap.dispose();
        }
    }

    /**
     * Uploads a sheet that is already in memory, such as one baked from a TTF, and cuts it into
     * glyph regions. No color conversion is done: glyphs should already be white with alpha.
     *
     * <p>Takes ownership of {@code sheet} and disposes it, also when this method throws.
     *
     * @param sheet the sheet image
     * @param layout where the glyph cells are in the image
     * @param order which code point each slot holds
     * @param presentInSheet tells which code points of the order really have a glyph in the
     *        sheet; the others draw the fallback and {@link #hasGlyph(int)} reports them as missing
     * @param fallbackCodePoint code point whose glyph stands in for missing ones; must be in the
     *        order and present in the sheet
     * @return the loaded source
     * @throws IllegalArgumentException if the image is smaller than the layout needs, the order has
     *         more glyphs than the layout has slots, or the fallback is missing
     */
    public static TextureGlyphSource fromPixmap(
        Pixmap sheet, GlyphSheetLayout layout, GlyphOrder order, IntPredicate presentInSheet, int fallbackCodePoint) {
        return fromPixmap(sheet, layout, order, presentInSheet, fallbackCodePoint, GlyphFilter.NEAREST);
    }

    /**
     * Same as {@link #fromPixmap(Pixmap, GlyphSheetLayout, GlyphOrder, IntPredicate, int)}, with a
     * choice of texture filter. {@link GlyphFilter#LINEAR} uploads a repacked, slightly larger
     * copy; {@code sheet} is still disposed by this method.
     *
     * @param filter how the texture is sampled
     */
    public static TextureGlyphSource fromPixmap(
        Pixmap sheet,
        GlyphSheetLayout layout,
        GlyphOrder order,
        IntPredicate presentInSheet,
        int fallbackCodePoint,
        GlyphFilter filter) {
        try {
            Objects.requireNonNull(filter, "filter must not be null");
            Objects.requireNonNull(sheet, "sheet must not be null");
            Objects.requireNonNull(layout, "layout must not be null");
            Objects.requireNonNull(order, "order must not be null");
            Objects.requireNonNull(presentInSheet, "presentInSheet must not be null");
            requireOrderFits(order, layout);
            int fallbackSlot = order.slotOf(fallbackCodePoint);
            if (fallbackSlot < 0 || !presentInSheet.test(fallbackCodePoint)) {
                throw new IllegalArgumentException(
                    "Sheet has no glyph for fallback code point " + fallbackCodePoint);
            }
            requireImageFits(sheet, "Sheet", layout);
            return upload(sheet, layout, order, fallbackSlot, presentInSheet, filter);
        } finally {
            if (sheet != null) {
                sheet.dispose();
            }
        }
    }

    // Does not dispose the given pixmap; the caller keeps ownership.
    private static TextureGlyphSource upload(
        Pixmap pixmap,
        GlyphSheetLayout layout,
        GlyphOrder order,
        int fallbackSlot,
        IntPredicate presentInSheet,
        GlyphFilter filter) {
        if (filter == GlyphFilter.LINEAR) {
            return uploadPadded(pixmap, layout, order, fallbackSlot, presentInSheet);
        }
        return uploadTexture(pixmap, layout, order, fallbackSlot, presentInSheet, Texture.TextureFilter.Nearest);
    }

    private static TextureGlyphSource uploadPadded(
        Pixmap pixmap, GlyphSheetLayout layout, GlyphOrder order, int fallbackSlot, IntPredicate presentInSheet) {
        GlyphSheetLayout paddedLayout = PaddedSheet.layoutFor(layout);
        Pixmap padded = PaddedSheet.copy(pixmap, layout, paddedLayout, order.size());
        try {
            return uploadTexture(padded, paddedLayout, order, fallbackSlot, presentInSheet, Texture.TextureFilter.Linear);
        } finally {
            padded.dispose();
        }
    }

    private static TextureGlyphSource uploadTexture(
        Pixmap pixmap,
        GlyphSheetLayout layout,
        GlyphOrder order,
        int fallbackSlot,
        IntPredicate presentInSheet,
        Texture.TextureFilter textureFilter) {
        Texture texture = new Texture(pixmap, false);
        try {
            texture.setFilter(textureFilter, textureFilter);
            texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
            return new TextureGlyphSource(texture, layout, order, fallbackSlot, presentInSheet);
        } catch (RuntimeException e) {
            texture.dispose();
            throw e;
        }
    }

    /**
     * Loads the bundled default font, Saiksistt, on 16x16 cells: Code Page 437 plus box drawing,
     * blocks, shades, arrows, shapes, game pieces, and pictographs. Released under CC0. This is the
     * default size; use {@link #loadSaiksistt(SaiksisttSize)} to pick another.
     */
    public static TextureGlyphSource loadSaiksistt() {
        return loadSaiksistt(SaiksisttSize.SQUARE_16X16);
    }

    /** Loads the bundled Saiksistt font in the given cell size. */
    public static TextureGlyphSource loadSaiksistt(SaiksisttSize size) {
        return loadSaiksistt(size, GlyphFilter.NEAREST);
    }

    /** Loads the bundled Saiksistt font in the given cell size with the given texture filter. */
    public static TextureGlyphSource loadSaiksistt(SaiksisttSize size, GlyphFilter filter) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(filter, "filter must not be null");
        return loadSheet(
            Gdx.files.classpath(size.sheetPath()),
            Gdx.files.classpath(size.characterListPath()),
            size.cellWidth(),
            size.cellHeight(),
            TransparencyMode.AUTO,
            filter);
    }

    private static boolean hasNonOpaquePixel(Pixmap pixmap) {
        for (int y = 0; y < pixmap.getHeight(); y++) {
            for (int x = 0; x < pixmap.getWidth(); x++) {
                if (ColorKeyConversion.isNotFullyOpaque(pixmap.getPixel(x, y))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void convertToWhiteWithAlpha(Pixmap pixmap, int key) {
        for (int y = 0; y < pixmap.getHeight(); y++) {
            for (int x = 0; x < pixmap.getWidth(); x++) {
                pixmap.drawPixel(x, y, ColorKeyConversion.toWhiteWithAlpha(pixmap.getPixel(x, y), key));
            }
        }
    }

    @Override
    public int getCellWidth() {
        return cellWidth;
    }

    @Override
    public int getCellHeight() {
        return cellHeight;
    }

    @Override
    public TextureRegion glyph(int codePoint) {
        int slot = order.slotOf(codePoint);
        return slot >= 0 ? regionsBySlot[slot] : fallbackRegion;
    }

    @Override
    public boolean hasGlyph(int codePoint) {
        return order.contains(codePoint) && presentInSheet.test(codePoint);
    }

    /** Releases the texture. Safe to call more than once. */
    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        texture.dispose();
    }
}
