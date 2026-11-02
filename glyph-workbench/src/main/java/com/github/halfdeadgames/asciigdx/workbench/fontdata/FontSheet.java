package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrderText;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSheetLayout;
import java.awt.image.BufferedImage;
import java.util.Map;

/** A font laid out as a glyph sheet: every glyph resolved to a full cell and placed in its slot. */
public final class FontSheet {

    private static final int LIT_PIXEL = 0xFFFFFFFF;

    private final FontData data;
    private final Map<Integer, PixelGlyph> glyphs;
    private final int[] slots;
    private final GlyphSheetLayout layout;

    private FontSheet(FontData data, Map<Integer, PixelGlyph> glyphs, int[] slots) {
        this.data = data;
        this.glyphs = glyphs;
        this.slots = slots;
        int rows = (slots.length + SheetOrder.COLUMNS - 1) / SheetOrder.COLUMNS;
        layout = GlyphSheetLayout.grid(data.cellWidth(), data.cellHeight(), SheetOrder.COLUMNS, rows);
    }

    /**
     * Resolves a font and lays it out.
     *
     * @param data the font
     * @return the sheet
     * @throws FontDataException if a derived glyph cannot be resolved
     */
    public static FontSheet of(FontData data) {
        Map<Integer, PixelGlyph> glyphs = GlyphResolver.resolveAll(data);
        return new FontSheet(data, glyphs, SheetOrder.slotsFor(glyphs.keySet()));
    }

    /** Returns the sheet layout: cells of the font's size, 16 per row. */
    public GlyphSheetLayout getLayout() {
        return layout;
    }

    /** Returns the code point of each slot; empty slots hold {@link SheetOrder#FILLER}. */
    public int[] getSlots() {
        return slots.clone();
    }

    /**
     * Returns the pixels of a slot's whole cell, letter gap included.
     *
     * @param slot slot index
     * @return the cell, blank for an empty slot
     */
    public PixelGrid cellAt(int slot) {
        PixelGlyph glyph = glyphs.get(slots[slot]);
        return glyph == null
            ? PixelGrid.blank(data.cellWidth(), data.cellHeight())
            : glyph.toCell(data.cellWidth());
    }

    /** Returns the slot order as a character list, ready for the sheet's {@code .txt} file. */
    public String toOrderText() {
        return GlyphOrderText.write(GlyphOrder.ofCodePoints(slots), SheetOrder.COLUMNS);
    }

    /** Returns the sheet image: opaque white glyph pixels on a transparent background. */
    public BufferedImage toImage() {
        BufferedImage image = new BufferedImage(
            layout.requiredImageWidth(), layout.requiredImageHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int slot = 0; slot < slots.length; slot++) {
            drawCell(image, slot);
        }
        return image;
    }

    private void drawCell(BufferedImage image, int slot) {
        PixelGrid cell = cellAt(slot);
        for (int y = 0; y < cell.getHeight(); y++) {
            for (int x = 0; x < cell.getWidth(); x++) {
                if (cell.isLit(x, y)) {
                    image.setRGB(layout.slotX(slot) + x, layout.slotY(slot) + y, LIT_PIXEL);
                }
            }
        }
    }
}
