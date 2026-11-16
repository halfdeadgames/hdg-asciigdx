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
        FileHandle image, FileHandle characterList, int cellWidth, int cellHeight, int fallbackCodePoint) {
        String text = characterList.readString("UTF-8");
        Pixmap pixmap = new Pixmap(image);
        try {
            int columns = pixmap.getWidth() / cellWidth;
            if (columns == 0) {
                throw new IllegalArgumentException(
                    "Image " + image.path() + " is narrower than one " + cellWidth + " pixel cell");
            }
            GlyphOrder order = GlyphOrderText.read(text, columns);
            GlyphSheetLayout layout =
                GlyphSheetLayout.grid(cellWidth, cellHeight, columns, (order.size() + columns - 1) / columns);
            int fallbackSlot = order.slotOf(fallbackCodePoint);
            if (fallbackSlot < 0) {
                throw new IllegalArgumentException(
                    "Character list " + characterList.path() + " has no '?' to use as the fallback glyph");
            }
            return new LoadedSheet(pixmap, layout, order, fallbackSlot);
        } catch (RuntimeException e) {
            pixmap.dispose();
            throw e;
        }
    }

    static void requireOrderFits(GlyphOrder order, GlyphSheetLayout layout) {
        if (order.size() > layout.slotCount()) {
            throw new IllegalArgumentException(
                "Order has " + order.size() + " glyphs but the layout has only "
                    + layout.slotCount() + " slots");
        }
    }

    static void requireImageFits(Pixmap image, String imageName, GlyphSheetLayout layout) {
        if (image.getWidth() < layout.requiredImageWidth() || image.getHeight() < layout.requiredImageHeight()) {
            throw new IllegalArgumentException(
                imageName + " is " + image.getWidth() + "x" + image.getHeight()
                    + " but the layout needs at least " + layout.requiredImageWidth()
                    + "x" + layout.requiredImageHeight());
        }
    }

    // Returns the given pixmap when no conversion happens or it is already RGBA8888 (converted in
    // place). Otherwise returns a new RGBA8888 pixmap and disposes the given one. If it throws, the
    // given pixmap stays undisposed and still belongs to the caller. The caller owns the result.
    static Pixmap applyTransparencyMode(Pixmap pixmap, TransparencyMode mode, Integer keyRgba8888OrNull) {
        int key = keyRgba8888OrNull != null ? keyRgba8888OrNull : pixmap.getPixel(0, 0);
        if (!mode.shouldConvert(hasNonOpaquePixel(pixmap))) {
            return pixmap;
        }
        Pixmap target = pixmap.getFormat() == Pixmap.Format.RGBA8888 ? pixmap : copyAsRgba8888(pixmap);
        try {
            target.setBlending(Pixmap.Blending.None);
            convertToWhiteWithAlpha(target, key);
        } catch (RuntimeException e) {
            disposeUnlessSame(target, pixmap);
            throw e;
        }
        disposeUnlessSame(pixmap, target);
        return target;
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
                pixmap.drawPixel(x, y, ColorKeyConversion.toWhiteWithAlpha(pixmap.getPixel(x, y), key));
            }
        }
    }
}
