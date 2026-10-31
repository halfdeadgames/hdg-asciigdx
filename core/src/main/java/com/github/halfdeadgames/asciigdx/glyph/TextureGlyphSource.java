package com.github.halfdeadgames.asciigdx.glyph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.Objects;

/**
 * A {@link GlyphSource} backed by a single sheet image uploaded as one {@link Texture}.
 *
 * <p>The texture uses nearest filtering, no mipmaps and clamped edges so glyph pixels stay
 * sharp. Slots of the layout beyond the size of the {@link GlyphOrder} are unused.
 *
 * <p>Must be created and disposed on the render thread, because it uploads a texture.
 */
public final class TextureGlyphSource implements GlyphSource {

    /** Fallback code point used by the short {@code load} overload. */
    public static final int DEFAULT_FALLBACK_CODE_POINT = '?';

    private static final String DUNGEON_MODE_PATH =
        "com/github/halfdeadgames/asciigdx/fonts/dungeon-mode/dungeon-437.png";

    private final Texture texture;
    private final GlyphOrder order;
    private final TextureRegion[] regionsBySlot;
    private final TextureRegion fallbackRegion;
    private final int cellWidth;
    private final int cellHeight;
    private boolean disposed;

    private TextureGlyphSource(Texture texture, GlyphSheetLayout layout, GlyphOrder order, int fallbackSlot) {
        this.texture = texture;
        this.order = order;
        cellWidth = layout.getCellWidth();
        cellHeight = layout.getCellHeight();
        regionsBySlot = new TextureRegion[order.size()];
        for (int slot = 0; slot < regionsBySlot.length; slot++) {
            regionsBySlot[slot] = new TextureRegion(
                texture, layout.slotX(slot), layout.slotY(slot), cellWidth, cellHeight);
        }
        fallbackRegion = regionsBySlot[fallbackSlot];
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
        if (order.size() > layout.slotCount()) {
            throw new IllegalArgumentException(
                "Order has " + order.size() + " glyphs but the layout has only "
                    + layout.slotCount() + " slots");
        }
        int fallbackSlot = order.slotOf(fallbackCodePoint);
        if (fallbackSlot < 0) {
            throw new IllegalArgumentException(
                "Order has no glyph for fallback code point " + fallbackCodePoint);
        }

        Pixmap pixmap = new Pixmap(file);
        try {
            if (pixmap.getWidth() < layout.requiredImageWidth()
                || pixmap.getHeight() < layout.requiredImageHeight()) {
                throw new IllegalArgumentException(
                    "Image " + file.path() + " is " + pixmap.getWidth() + "x" + pixmap.getHeight()
                        + " but the layout needs at least " + layout.requiredImageWidth()
                        + "x" + layout.requiredImageHeight());
            }
            int key = keyRgba8888OrNull != null ? keyRgba8888OrNull : pixmap.getPixel(0, 0);
            if (mode.shouldConvert(hasTransparentPixel(pixmap))) {
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

            Texture texture = new Texture(pixmap, false);
            try {
                texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
                return new TextureGlyphSource(texture, layout, order, fallbackSlot);
            } catch (RuntimeException e) {
                texture.dispose();
                throw e;
            }
        } finally {
            pixmap.dispose();
        }
    }

    /** Loads the bundled Dungeon Mode font: 8x8 cells, 16x16 grid, Code Page 437. */
    public static TextureGlyphSource loadDungeonMode() {
        return load(
            Gdx.files.classpath(DUNGEON_MODE_PATH),
            GlyphSheetLayout.grid(8, 8, 16, 16),
            GlyphOrder.CP437,
            TransparencyMode.AUTO);
    }

    private static boolean hasTransparentPixel(Pixmap pixmap) {
        for (int y = 0; y < pixmap.getHeight(); y++) {
            for (int x = 0; x < pixmap.getWidth(); x++) {
                if (ColorKeyConversion.isTransparent(pixmap.getPixel(x, y))) {
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
        return order.contains(codePoint);
    }

    /**
     * Returns the sheet texture. Every region of this source belongs to it, so a renderer
     * can batch all glyphs of the source with a single texture bind. Owned by this source:
     * do not dispose it.
     */
    public Texture getTexture() {
        return texture;
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
