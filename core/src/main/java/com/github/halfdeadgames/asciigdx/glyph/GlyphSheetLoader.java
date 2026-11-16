package com.github.halfdeadgames.asciigdx.glyph;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;

// Reads, checks, and color-converts glyph sheet images. Knows nothing about textures.
final class GlyphSheetLoader {

    // The caller owns the pixmap and must dispose it.
    record LoadedSheet(Pixmap pixmap, GlyphSheetLayout layout, GlyphOrder order, int fallbackSlot) {
    }

    private GlyphSheetLoader() {
    }

    // Disposes the decoded pixmap if the layout cannot be built.
    static LoadedSheet readSheet(
            FileHandle image, FileHandle characterList, int cellWidth, int cellHeight,
            int fallbackCodePoint) {
        String text = characterList.readString("UTF-8");
        Pixmap pixmap = new Pixmap(image);
        try {
            int columns = pixmap.getWidth() / cellWidth;
            if (columns == 0) {
                throw new IllegalArgumentException(
                        "Image " + image.path() + " is narrower than one " + cellWidth
                                + " pixel cell");
            }
            GlyphOrder order = GlyphOrderText.read(text, columns);
            GlyphSheetLayout layout =
                    GlyphSheetLayout.packed(cellWidth, cellHeight, columns,
                            (order.getGlyphCount() + columns - 1) / columns);
            int fallbackSlot = order.findSlot(fallbackCodePoint);
            if (fallbackSlot < 0) {
                throw new IllegalArgumentException(
                        "Character list " + characterList.path()
                                + " has no '?' to use as the fallback glyph");
            }
            return new LoadedSheet(pixmap, layout, order, fallbackSlot);
        } catch (RuntimeException e) {
            pixmap.dispose();
            throw e;
        }
    }

    static void requireOrderFits(GlyphOrder order, GlyphSheetLayout layout) {
        if (order.getGlyphCount() > layout.getSlotCount()) {
            throw new IllegalArgumentException(
                    "Order has " + order.getGlyphCount() + " glyphs but the layout has only "
                            + layout.getSlotCount() + " slots");
        }
    }

    static void requireImageFits(Pixmap image, String imageName, GlyphSheetLayout layout) {
        if (image.getWidth() < layout.getRequiredImageWidth()
                || image.getHeight() < layout.getRequiredImageHeight()) {
            throw new IllegalArgumentException(
                    imageName + " is " + image.getWidth() + "x" + image.getHeight()
                            + " but the layout needs at least " + layout.getRequiredImageWidth()
                            + "x" + layout.getRequiredImageHeight());
        }
    }

    // Returns the given pixmap when no conversion happens or it is already RGBA8888 (converted in
    // place). Otherwise returns a new RGBA8888 pixmap and disposes the given one. If it throws, the
    // given pixmap stays undisposed and still belongs to the caller. The caller owns the result.
    static Pixmap applyTransparencyMode(Pixmap pixmap, TransparencyMode mode, int keyRgba8888) {
        if (!mode.shouldConvert(hasNonOpaquePixel(pixmap))) {
            return pixmap;
        }
        Pixmap target =
                pixmap.getFormat() == Pixmap.Format.RGBA8888 ? pixmap : copyAsRgba8888(pixmap);
        try {
            target.setBlending(Pixmap.Blending.None);
            convertToWhiteWithAlpha(target, keyRgba8888);
        } catch (RuntimeException e) {
            disposeUnlessSame(target, pixmap);
            throw e;
        }
        disposeUnlessSame(pixmap, target);
        return target;
    }

    // Same as the explicit-key overload, with the top-left pixel (as RGBA8888) as the key.
    static Pixmap applyTransparencyModeWithTopLeftKey(Pixmap pixmap, TransparencyMode mode) {
        return applyTransparencyMode(pixmap, mode, readTopLeftKey(pixmap));
    }

    static int readTopLeftKey(Pixmap pixmap) {
        return pixmap.getPixel(0, 0);
    }

    private static Pixmap copyAsRgba8888(Pixmap pixmap) {
        Pixmap copy = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), Pixmap.Format.RGBA8888);
        try {
            copy.setBlending(Pixmap.Blending.None);
            copy.drawPixmap(pixmap, 0, 0);
            return copy;
        } catch (RuntimeException e) {
            copy.dispose();
            throw e;
        }
    }

    private static void disposeUnlessSame(Pixmap toDispose, Pixmap other) {
        if (toDispose != other) {
            toDispose.dispose();
        }
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
                pixmap.drawPixel(x, y,
                        ColorKeyConversion.toWhiteWithAlpha(pixmap.getPixel(x, y), key));
            }
        }
    }
}
