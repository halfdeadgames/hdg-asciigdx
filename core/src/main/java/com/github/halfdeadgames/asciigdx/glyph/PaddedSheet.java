package com.github.halfdeadgames.asciigdx.glyph;

import com.badlogic.gdx.graphics.Pixmap;

/** Repacks a glyph sheet with a transparent gutter around every glyph, for linear filtering. */
final class PaddedSheet {

    static final int GUTTER = 1;

    /** White with zero alpha, so linear filtering does not darken glyph edges. */
    static final int CLEAR_WHITE = 0xFFFFFF00;

    private PaddedSheet() {
    }

    /** Returns a layout with the same cells, columns, and rows, but a gutter around each cell. */
    static GlyphSheetLayout layoutFor(GlyphSheetLayout source) {
        return new GlyphSheetLayout(
                source.getCellWidth(), source.getCellHeight(), source.getColumns(),
                source.getRows(), GUTTER, GUTTER);
    }

    /**
     * Builds an RGBA8888 pixmap in the padded layout holding the first {@code slotCount} slots of
     * the source. The caller disposes the result.
     */
    static Pixmap copy(Pixmap source, GlyphSheetLayout sourceLayout, GlyphSheetLayout paddedLayout,
            int slotCount) {
        Pixmap padded = new Pixmap(
                paddedLayout.getRequiredImageWidth(), paddedLayout.getRequiredImageHeight(),
                Pixmap.Format.RGBA8888);
        try {
            padded.setBlending(Pixmap.Blending.None);
            padded.setColor(CLEAR_WHITE);
            padded.fill();
            for (int slot = 0; slot < slotCount; slot++) {
                copySlot(source, sourceLayout, padded, paddedLayout, slot);
            }
            return padded;
        } catch (RuntimeException e) {
            padded.dispose();
            throw e;
        }
    }

    private static void copySlot(
            Pixmap source, GlyphSheetLayout sourceLayout, Pixmap padded,
            GlyphSheetLayout paddedLayout, int slot) {
        int sourceX = sourceLayout.getSlotX(slot);
        int sourceY = sourceLayout.getSlotY(slot);
        int paddedX = paddedLayout.getSlotX(slot);
        int paddedY = paddedLayout.getSlotY(slot);
        for (int y = 0; y < sourceLayout.getCellHeight(); y++) {
            for (int x = 0; x < sourceLayout.getCellWidth(); x++) {
                padded.drawPixel(paddedX + x, paddedY + y,
                        cleanTexel(source.getPixel(sourceX + x, sourceY + y)));
            }
        }
    }

    private static int cleanTexel(int rgba8888) {
        return (rgba8888 & 0xFF) == 0 ? CLEAR_WHITE : rgba8888;
    }
}
