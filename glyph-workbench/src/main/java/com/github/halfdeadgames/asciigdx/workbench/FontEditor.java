package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.DerivedGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontData;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataException;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataWriter;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphEntry;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphResolver;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.SheetOrder;
import java.util.Map;
import java.util.TreeMap;

/**
 * The editable state of one font: its glyphs, the sheet slots, and the unsaved flag. Derived
 * glyphs are shown but cannot be painted, and gap columns of normal glyphs cannot be painted.
 */
final class FontEditor {

    /** Slot content for padding slots that hold no glyph and cannot be selected. */
    static final int NO_GLYPH = -1;

    private final int cellWidth;
    private final int cellHeight;
    private final Map<Integer, GlyphEntry> entries = new TreeMap<>();
    private final int[] slots;
    private Map<Integer, PixelGlyph> resolved;
    // Each edit builds a new resolved map, so keeping the old reference is a free snapshot.
    private Map<Integer, PixelGlyph> savedGlyphs;
    private boolean modified;
    private String problem = "";

    FontEditor(FontData data) {
        cellWidth = data.cellWidth();
        cellHeight = data.cellHeight();
        entries.putAll(data.entries());
        resolved = GlyphResolver.resolveAll(data);
        savedGlyphs = resolved;
        slots = buildSlots(data);
    }

    // Empty CP437 slots keep their own code point so they stay selectable; other padding is not selectable.
    // Painting only fills existing slots, so the slots never change during a session.
    private static int[] buildSlots(FontData data) {
        int[] ordered = SheetOrder.slotsFor(data.entries().keySet());
        for (int slot = 0; slot < ordered.length; slot++) {
            if (slot < GlyphOrder.CP437.size()) {
                ordered[slot] = GlyphOrder.CP437.codePointAt(slot);
            } else if (ordered[slot] == SheetOrder.FILLER) {
                ordered[slot] = NO_GLYPH;
            }
        }
        return ordered;
    }

    int cellWidth() {
        return cellWidth;
    }

    int cellHeight() {
        return cellHeight;
    }

    int bodyWidth() {
        return cellWidth - cellWidth / 8;
    }

    int slotCount() {
        return slots.length;
    }

    int codePointAtSlot(int slot) {
        return slots[slot];
    }

    boolean hasGlyph(int codePoint) {
        return entries.containsKey(codePoint);
    }

    boolean isDerived(int codePoint) {
        return entries.get(codePoint) instanceof DerivedGlyph;
    }

    /** First source of a derived glyph, or {@link #NO_GLYPH} for any other glyph. */
    int derivedFrom(int codePoint) {
        return entries.get(codePoint) instanceof DerivedGlyph derived
            ? derived.op().sources().get(0).codePoint()
            : NO_GLYPH;
    }

    /** The drawn pixels of a glyph with derived ones resolved, or null when the slot is empty. */
    PixelGlyph glyph(int codePoint) {
        return resolved.get(codePoint);
    }

    /** Columns that hold drawn pixels: the whole cell for full glyphs, otherwise the body. */
    int drawnWidth(int codePoint) {
        PixelGlyph glyph = resolved.get(codePoint);
        return glyph != null && glyph.full() ? cellWidth : bodyWidth();
    }

    boolean isLit(int codePoint, int x, int y) {
        PixelGlyph glyph = resolved.get(codePoint);
        return glyph != null && x >= 0 && y >= 0
            && x < glyph.pixels().getWidth() && y < glyph.pixels().getHeight()
            && glyph.pixels().isLit(x, y);
    }

    /** Whether the glyph existed in the last saved (or loaded) state. */
    boolean hasSavedGlyph(int codePoint) {
        return savedGlyphs.containsKey(codePoint);
    }

    /** Whether a pixel was lit in the last saved (or loaded) state; false for glyphs that were not saved. */
    boolean wasLitWhenSaved(int codePoint, int x, int y) {
        PixelGlyph glyph = savedGlyphs.get(codePoint);
        return glyph != null && x >= 0 && y >= 0
            && x < glyph.pixels().getWidth() && y < glyph.pixels().getHeight()
            && glyph.pixels().isLit(x, y);
    }

    boolean isPaintable(int codePoint, int x, int y) {
        return codePoint != NO_GLYPH && !isDerived(codePoint)
            && x >= 0 && y >= 0 && x < drawnWidth(codePoint) && y < cellHeight;
    }

    /**
     * Sets a pixel, creating the glyph first if the slot is empty. Returns whether anything changed;
     * an edit that would break a derived glyph is undone and explained by {@link #problem()}.
     */
    boolean setPixel(int codePoint, int x, int y, boolean on) {
        if (!isPaintable(codePoint, x, y)) {
            return false;
        }
        GlyphEntry before = entries.get(codePoint);
        PixelGlyph current = (PixelGlyph) before;
        if (current == null && !on) {
            return false;
        }
        PixelGrid base = current == null ? PixelGrid.blank(bodyWidth(), cellHeight) : current.pixels();
        if (current != null && base.isLit(x, y) == on) {
            return false;
        }
        boolean full = current != null && current.full();
        entries.put(codePoint, new PixelGlyph(codePoint, full, base.withPixel(x, y, on)));
        try {
            resolved = GlyphResolver.resolveAll(data());
        } catch (FontDataException e) {
            restore(codePoint, before);
            problem = e.getMessage();
            return false;
        }
        problem = "";
        modified = true;
        return true;
    }

    private void restore(int codePoint, GlyphEntry before) {
        if (before == null) {
            entries.remove(codePoint);
        } else {
            entries.put(codePoint, before);
        }
    }

    FontData data() {
        return new FontData(cellWidth, cellHeight, entries);
    }

    /** The font data text for saving; derived recipes are kept, comment lines are not. */
    String toText() {
        return FontDataWriter.write(data());
    }

    boolean isModified() {
        return modified;
    }

    void markSaved() {
        modified = false;
        savedGlyphs = resolved;
    }

    String problem() {
        return problem;
    }
}
