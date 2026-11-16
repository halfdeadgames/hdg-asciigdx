package com.github.halfdeadgames.asciigdx.glyph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.function.UnaryOperator;

/**
 * A {@link GlyphSource} backed by a single sheet image uploaded as one {@link Texture}.
 *
 * <p>The texture uses no mipmaps and clamped edges. By default it uses nearest filtering so glyph
 * pixels stay sharp; pass {@link GlyphFilter#LINEAR} for smooth scaling and rotation, which
 * uploads a slightly larger sheet with a transparent gutter around every glyph. Slots of the
 * layout beyond the size of the {@link GlyphOrder} are unused.
 *
 * <p>Create one with a static factory: {@link #loadSaiksistt()} for the bundled font,
 * {@link #loadSheet(FileHandle, FileHandle, int, int, TransparencyMode)} for a sheet image with a
 * character list, {@link #load(FileHandle, GlyphSheetLayout, GlyphOrder, TransparencyMode)} for a
 * sheet image with an explicit layout, or {@link #fromPixmap(Pixmap, GlyphSheetLayout, GlyphOrder,
 * IntPredicate, int)} for a sheet already in memory.
 *
 * <p>Lookups ({@link #getGlyphRegion(int)}, {@link #hasGlyph(int)}) allocate nothing. Glyphs
 * missing from the order draw the fallback glyph chosen at load time.
 *
 * <p>Must be created and disposed on the render thread, because it uploads a texture. The
 * caller owns every source a factory returns and must call {@link #dispose()} once it is no
 * longer drawn. Disposing invalidates every region obtained from it. Not thread-safe.
 */
public final class TextureGlyphSource implements GlyphSource {

    /**
     * The fallback code point, {@code '?'}, used by every factory that takes no fallback
     * argument. The order or character list of such a load must contain it.
     */
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
            Texture texture, GlyphSheetLayout layout, GlyphOrder order, int fallbackSlot,
            IntPredicate presentInSheet) {
        this.texture = texture;
        this.order = order;
        this.presentInSheet = presentInSheet;
        cellWidth = layout.getCellWidth();
        cellHeight = layout.getCellHeight();
        regionsBySlot = new TextureRegion[order.getGlyphCount()];
        TextureRegion fallback = new TextureRegion(
                texture, layout.getSlotX(fallbackSlot), layout.getSlotY(fallbackSlot), cellWidth,
                cellHeight);
        for (int slot = 0; slot < regionsBySlot.length; slot++) {
            regionsBySlot[slot] = presentInSheet.test(order.getCodePoint(slot))
                    ? new TextureRegion(texture, layout.getSlotX(slot), layout.getSlotY(slot),
                    cellWidth, cellHeight)
                    : fallback;
        }
        fallbackRegion = fallback;
    }

    /**
     * Loads a sheet image using its top-left pixel as the transparency key and {@code '?'}
     * ({@link #DEFAULT_FALLBACK_CODE_POINT}) as the fallback glyph. The texture uses nearest
     * filtering. Behaves like
     * {@link #load(FileHandle, GlyphSheetLayout, GlyphOrder, TransparencyMode, int)} with
     * {@code '?'} as the fallback.
     *
     * <p>Call on the render thread, because it uploads a texture. The caller owns the returned
     * source and must dispose it. The decoded image is released before this method returns.
     *
     * @param file the image file; must not be null
     * @param layout where the glyph cells are in the image; must not be null
     * @param order which code point each slot holds; must not be null and must contain {@code '?'}
     * @param mode whether to convert the image to white glyphs with alpha; must not be null
     * @return the loaded source
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if the image is smaller than the layout needs, the order has
     *         more glyphs than the layout has slots, or the order lacks {@code '?'}
     */
    public static TextureGlyphSource load(
            FileHandle file, GlyphSheetLayout layout, GlyphOrder order, TransparencyMode mode) {
        return load(file, layout, order, mode, DEFAULT_FALLBACK_CODE_POINT);
    }

    /**
     * Loads a sheet image and cuts it into glyph regions, using the top-left pixel of the image
     * as the transparency key.
     *
     * <p>If the mode asks for conversion, every pixel becomes white with an alpha derived
     * from its distance to the key color. With {@link TransparencyMode#NEVER} no conversion
     * happens and the key is not used. The texture uses nearest filtering.
     *
     * <p>Slot {@code i} of the order is cut from the cell at slot {@code i} of the layout.
     * Slot positions are in image pixels, measured from the top-left, with {@code y} growing
     * downward, and slots are numbered left to right, then top to bottom.
     *
     * <p>Call on the render thread, because it uploads a texture. The caller owns the returned
     * source and must dispose it. The decoded image is released before this method returns.
     *
     * @param file the image file; must not be null
     * @param layout where the glyph cells are in the image; must not be null
     * @param order which code point each slot holds; must not be null
     * @param mode whether to convert the image to white glyphs with alpha; must not be null
     * @param fallbackCodePoint code point whose glyph stands in for missing ones; must be in the
     *        order
     * @return the loaded source
     * @throws NullPointerException if {@code file}, {@code layout}, {@code order}, or {@code mode}
     *         is null
     * @throws IllegalArgumentException if the image is smaller than the layout needs, the order has
     *         more glyphs than the layout has slots, or the order lacks the fallback
     */
    public static TextureGlyphSource load(
            FileHandle file, GlyphSheetLayout layout, GlyphOrder order, TransparencyMode mode,
            int fallbackCodePoint) {
        return loadFile(
                file, layout, order, mode, fallbackCodePoint,
                pixmap -> GlyphSheetLoader.applyTransparencyModeWithTopLeftKey(pixmap, mode));
    }

    /**
     * Loads a sheet image and cuts it into glyph regions, using an explicit transparency key.
     *
     * <p>If the mode asks for conversion, every pixel becomes white with an alpha derived
     * from its distance to the key color. With {@link TransparencyMode#NEVER} no conversion
     * happens and the key is not used. The texture uses nearest filtering.
     *
     * <p>Slot {@code i} of the order is cut from the cell at slot {@code i} of the layout.
     * Slot positions are in image pixels, measured from the top-left, with {@code y} growing
     * downward, and slots are numbered left to right, then top to bottom.
     *
     * <p>Call on the render thread, because it uploads a texture. The caller owns the returned
     * source and must dispose it. The decoded image is released before this method returns.
     *
     * @param file the image file; must not be null
     * @param layout where the glyph cells are in the image; must not be null
     * @param order which code point each slot holds; must not be null
     * @param mode whether to convert the image to white glyphs with alpha; must not be null
     * @param keyRgba8888 the key color as an RGBA8888 int ({@code 0xRRGGBBAA}); pixels matching it
     *        become transparent. Its alpha byte is ignored by the conversion
     * @param fallbackCodePoint code point whose glyph stands in for missing ones; must be in the
     *        order
     * @return the loaded source
     * @throws NullPointerException if {@code file}, {@code layout}, {@code order}, or {@code mode}
     *         is null
     * @throws IllegalArgumentException if the image is smaller than the layout needs, the order has
     *         more glyphs than the layout has slots, or the order lacks the fallback
     */
    public static TextureGlyphSource loadWithColorKey(
            FileHandle file,
            GlyphSheetLayout layout,
            GlyphOrder order,
            TransparencyMode mode,
            int keyRgba8888,
            int fallbackCodePoint) {
        return loadFile(
                file, layout, order, mode, fallbackCodePoint,
                pixmap -> GlyphSheetLoader.applyTransparencyMode(pixmap, mode, keyRgba8888));
    }

    private static TextureGlyphSource loadFile(
            FileHandle file,
            GlyphSheetLayout layout,
            GlyphOrder order,
            TransparencyMode mode,
            int fallbackCodePoint,
            UnaryOperator<Pixmap> transparencyStep) {
        Objects.requireNonNull(file, "file must not be null");
        Objects.requireNonNull(layout, "layout must not be null");
        Objects.requireNonNull(order, "order must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
        GlyphSheetLoader.requireOrderFits(order, layout);
        int fallbackSlot = order.findSlot(fallbackCodePoint);
        if (fallbackSlot < 0) {
            throw new IllegalArgumentException(
                    "Order has no glyph for fallback code point " + fallbackCodePoint);
        }

        return convertAndUpload(
                new Pixmap(file), file.path(), layout, order, transparencyStep, fallbackSlot,
                GlyphFilter.NEAREST);
    }

    /**
     * Loads a sheet image together with its character list, the {@code .txt} file that names
     * the glyph in each cell (see {@link GlyphOrderText}). The number of columns is the image
     * width divided by the cell width, rounded down; the last row may be partly empty. Cells
     * are packed with no margin and no spacing, starting at the top-left pixel. Missing glyphs
     * draw {@code '?'}, which the character list must contain. The top-left pixel of the image
     * is the transparency key. The texture uses nearest filtering.
     *
     * <p>Call on the render thread, because it uploads a texture. The caller owns the returned
     * source and must dispose it. The decoded image is released before this method returns.
     *
     * @param image the sheet image; must not be null
     * @param characterList the character list, read as UTF-8; must not be null
     * @param cellWidth width of one cell in pixels, must be positive
     * @param cellHeight height of one cell in pixels, must be positive
     * @param mode whether to convert the image to white glyphs with alpha; must not be null
     * @return the loaded source
     * @throws NullPointerException if {@code image}, {@code characterList}, or {@code mode} is null
     * @throws IllegalArgumentException if a cell size is not positive, the image is narrower than
     *         one cell or too short for the rows of the character list, the character list is
     *         malformed or does not fit the image, or it lacks {@code '?'}
     */
    public static TextureGlyphSource loadSheet(
            FileHandle image, FileHandle characterList, int cellWidth, int cellHeight,
            TransparencyMode mode) {
        return loadSheet(image, characterList, cellWidth, cellHeight, mode, GlyphFilter.NEAREST);
    }

    /**
     * Loads a sheet image together with its character list and chooses the texture filter. The
     * number of columns is the image width divided by the cell width, rounded down; the last row
     * may be partly empty. Cells are packed with no margin and no spacing, starting at the
     * top-left pixel. Missing glyphs draw {@code '?'}, which the character list must contain.
     * The top-left pixel of the image is the transparency key. See
     * {@link #loadSheet(FileHandle, FileHandle, int, int, TransparencyMode)} for the character
     * list format.
     *
     * <p>Call on the render thread, because it uploads a texture. The caller owns the returned
     * source and must dispose it. The decoded image is released before this method returns.
     *
     * @param image the sheet image; must not be null
     * @param characterList the character list, read as UTF-8; must not be null
     * @param cellWidth width of one cell in pixels, must be positive
     * @param cellHeight height of one cell in pixels, must be positive
     * @param mode whether to convert the image to white glyphs with alpha; must not be null
     * @param filter how the texture is sampled; must not be null. {@link GlyphFilter#LINEAR}
     *        uploads a repacked, slightly larger copy of the sheet
     * @return the loaded source
     * @throws NullPointerException if {@code image}, {@code characterList}, {@code mode}, or
     *         {@code filter} is null
     * @throws IllegalArgumentException if a cell size is not positive, the image is narrower than
     *         one cell or too short for the rows of the character list, the character list is
     *         malformed or does not fit the image, or it lacks {@code '?'}
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
            throw new IllegalArgumentException(
                    "Cell size must be positive, was " + cellWidth + "x" + cellHeight);
        }
        GlyphSheetLoader.LoadedSheet sheet = GlyphSheetLoader.readSheet(
                image, characterList, cellWidth, cellHeight, DEFAULT_FALLBACK_CODE_POINT);
        return convertAndUpload(
                sheet.pixmap(),
                image.path(),
                sheet.layout(),
                sheet.order(),
                pixmap -> GlyphSheetLoader.applyTransparencyModeWithTopLeftKey(pixmap, mode),
                sheet.fallbackSlot(),
                filter);
    }

    // Takes ownership of the pixmap and disposes it, also when this method throws.
    // The transparency step follows the GlyphSheetLoader.applyTransparencyMode ownership rules.
    private static TextureGlyphSource convertAndUpload(
            Pixmap pixmap,
            String path,
            GlyphSheetLayout layout,
            GlyphOrder order,
            UnaryOperator<Pixmap> transparencyStep,
            int fallbackSlot,
            GlyphFilter filter) {
        Pixmap ready = pixmap;
        try {
            GlyphSheetLoader.requireImageFits(pixmap, "Image " + path, layout);
            ready = transparencyStep.apply(pixmap);
            return upload(ready, layout, order, fallbackSlot, codePoint -> true, filter);
        } finally {
            ready.dispose();
        }
    }

    /**
     * Uploads a sheet that is already in memory, such as one baked from a TTF, and cuts it into
     * glyph regions. No color conversion is done: glyphs should already be white with alpha.
     *
     * <p>Takes ownership of {@code sheet} and disposes it, also when this method throws. Do not
     * use the pixmap afterwards. The texture uses nearest filtering.
     *
     * <p>Call on the render thread, because it uploads a texture. The caller owns the returned
     * source and must dispose it.
     *
     * @param sheet the sheet image; must not be null
     * @param layout where the glyph cells are in the image, in pixels from the top-left with
     *        {@code y} growing downward; must not be null
     * @param order which code point each slot holds; must not be null
     * @param presentInSheet tells which code points of the order have a glyph drawn in the
     *        sheet; the others draw the fallback and {@link #hasGlyph(int)} reports them as
     *        missing; must not be null
     * @param fallbackCodePoint code point whose glyph stands in for missing ones; must be in the
     *        order and present in the sheet
     * @return the loaded source
     * @throws NullPointerException if {@code sheet}, {@code layout}, {@code order}, or
     *         {@code presentInSheet} is null
     * @throws IllegalArgumentException if the image is smaller than the layout needs, the order has
     *         more glyphs than the layout has slots, or the fallback is missing from the order or
     *         the sheet
     */
    public static TextureGlyphSource fromPixmap(
            Pixmap sheet, GlyphSheetLayout layout, GlyphOrder order, IntPredicate presentInSheet,
            int fallbackCodePoint) {
        return fromPixmap(sheet, layout, order, presentInSheet, fallbackCodePoint,
                GlyphFilter.NEAREST);
    }

    /**
     * Uploads a sheet that is already in memory, such as one baked from a TTF, cuts it into
     * glyph regions, and chooses the texture filter. No color conversion is done: glyphs should
     * already be white with alpha.
     *
     * <p>Takes ownership of {@code sheet} and disposes it, also when this method throws and also
     * with {@link GlyphFilter#LINEAR}, which uploads a repacked, slightly larger copy. Do not use
     * the pixmap afterwards.
     *
     * <p>Call on the render thread, because it uploads a texture. The caller owns the returned
     * source and must dispose it.
     *
     * @param sheet the sheet image; must not be null
     * @param layout where the glyph cells are in the image, in pixels from the top-left with
     *        {@code y} growing downward; must not be null
     * @param order which code point each slot holds; must not be null
     * @param presentInSheet tells which code points of the order have a glyph drawn in the
     *        sheet; the others draw the fallback and {@link #hasGlyph(int)} reports them as
     *        missing; must not be null
     * @param fallbackCodePoint code point whose glyph stands in for missing ones; must be in the
     *        order and present in the sheet
     * @param filter how the texture is sampled; must not be null
     * @return the loaded source
     * @throws NullPointerException if {@code sheet}, {@code layout}, {@code order},
     *         {@code presentInSheet}, or {@code filter} is null
     * @throws IllegalArgumentException if the image is smaller than the layout needs, the order has
     *         more glyphs than the layout has slots, or the fallback is missing from the order or
     *         the sheet
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
            GlyphSheetLoader.requireOrderFits(order, layout);
            int fallbackSlot = order.findSlot(fallbackCodePoint);
            if (fallbackSlot < 0 || !presentInSheet.test(fallbackCodePoint)) {
                throw new IllegalArgumentException(
                        "Sheet has no glyph for fallback code point " + fallbackCodePoint);
            }
            GlyphSheetLoader.requireImageFits(sheet, "Sheet", layout);
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
        return uploadTexture(pixmap, layout, order, fallbackSlot, presentInSheet,
                Texture.TextureFilter.Nearest);
    }

    private static TextureGlyphSource uploadPadded(
            Pixmap pixmap, GlyphSheetLayout layout, GlyphOrder order, int fallbackSlot,
            IntPredicate presentInSheet) {
        GlyphSheetLayout paddedLayout = PaddedSheet.layoutFor(layout);
        Pixmap padded = PaddedSheet.copy(pixmap, layout, paddedLayout, order.getGlyphCount());
        try {
            return uploadTexture(padded, paddedLayout, order, fallbackSlot, presentInSheet,
                    Texture.TextureFilter.Linear);
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
     * default size; use {@link #loadSaiksistt(SaiksisttSize)} to pick another. Missing glyphs
     * draw {@code '?'}. The texture uses nearest filtering.
     *
     * <p>Call on the render thread after libGDX has started, because it reads classpath files and
     * uploads a texture. The caller owns the returned source and must dispose it.
     *
     * @return the loaded source with 16 by 16 pixel cells
     */
    public static TextureGlyphSource loadSaiksistt() {
        return loadSaiksistt(SaiksisttSize.SQUARE_16X16);
    }

    /**
     * Loads the bundled Saiksistt font in the given cell size. Missing glyphs draw {@code '?'}.
     * The texture uses nearest filtering.
     *
     * <p>Call on the render thread after libGDX has started, because it reads classpath files and
     * uploads a texture. The caller owns the returned source and must dispose it.
     *
     * @param size the cell size to load; must not be null
     * @return the loaded source, with cells of the size's width and height
     * @throws NullPointerException if {@code size} is null
     */
    public static TextureGlyphSource loadSaiksistt(SaiksisttSize size) {
        return loadSaiksistt(size, GlyphFilter.NEAREST);
    }

    /**
     * Loads the bundled Saiksistt font in the given cell size with the given texture filter.
     * Missing glyphs draw {@code '?'}.
     *
     * <p>Call on the render thread after libGDX has started, because it reads classpath files and
     * uploads a texture. The caller owns the returned source and must dispose it.
     *
     * @param size the cell size to load; must not be null
     * @param filter how the texture is sampled; must not be null
     * @return the loaded source, with cells of the size's width and height
     * @throws NullPointerException if {@code size} or {@code filter} is null
     */
    public static TextureGlyphSource loadSaiksistt(SaiksisttSize size, GlyphFilter filter) {
        Objects.requireNonNull(size, "size must not be null");
        Objects.requireNonNull(filter, "filter must not be null");
        return loadSheet(
                Gdx.files.classpath(size.getSheetPath()),
                Gdx.files.classpath(size.getCharacterListPath()),
                size.getCellWidth(),
                size.getCellHeight(),
                TransparencyMode.AUTO,
                filter);
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
    public TextureRegion getGlyphRegion(int codePoint) {
        int slot = order.findSlot(codePoint);
        return slot >= 0 ? regionsBySlot[slot] : fallbackRegion;
    }

    @Override
    public boolean hasGlyph(int codePoint) {
        return order.contains(codePoint) && presentInSheet.test(codePoint);
    }

    /**
     * Releases the texture. Safe to call more than once; later calls do nothing. Call on the
     * render thread. Every region obtained from this source is invalid afterwards.
     */
    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        texture.dispose();
    }
}
