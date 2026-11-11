package com.github.halfdeadgames.asciigdx.workbench.editor;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.CodePoints;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.DerivedGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontData;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataException;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataWriter;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphEntry;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphList;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphOp;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphResolver;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.SheetOrder;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The editable state of one font: its glyphs, the sheet slots, and the unsaved flag. Derived
 * glyphs are shown but cannot be painted. Every glyph covers the whole cell.
 */
public final class FontEditor {

    /** Slot content for padding slots that hold no glyph and cannot be selected. */
    public static final int NO_GLYPH = -1;

    public static final int MAX_UNDO_STEPS = 200;

    private final int cellWidth;
    private final int cellHeight;
    private final boolean reserveCp437;
    private final Map<Integer, GlyphEntry> entries = new TreeMap<>();
    // Every code point the sheet has ever had a slot for; only grows, so undo never removes a slot.
    private final Set<Integer> knownCodePoints = new TreeSet<>();
    private int[] slots;
    private Map<Integer, PixelGlyph> resolved;
    // Each edit builds a new resolved map, so keeping the old reference is a free snapshot.
    private Map<Integer, PixelGlyph> savedGlyphs;
    private String problem = "";

    // Every distinct state has its own revision id, so "modified" is simply "not the saved revision".
    private int revision;
    private int savedRevision;
    private int nextRevision = 1;
    private final Deque<State> undoSteps = new ArrayDeque<>();
    private final Deque<State> redoSteps = new ArrayDeque<>();
    private State openEdit;

    private record State(Map<Integer, GlyphEntry> entries, int revision) {
    }

    /**
     * Creates the editor.
     *
     * @param data the font
     * @param glyphList the font's glyph list; its planned code points get empty, selectable slots, and the
     *        cp437 preset reserves the first 256 slots
     */
    public FontEditor(FontData data, GlyphList glyphList) {
        cellWidth = data.cellWidth();
        cellHeight = data.cellHeight();
        entries.putAll(data.entries());
        resolved = GlyphResolver.resolveAll(data);
        savedGlyphs = resolved;
        reserveCp437 = glyphList.usesCp437();
        knownCodePoints.addAll(data.entries().keySet());
        knownCodePoints.addAll(glyphList.plannedCodePoints());
        slots = buildSlots(knownCodePoints, reserveCp437);
    }

    // With the cp437 preset, empty CP437 slots keep their own code point so they stay selectable. Other padding
    // is not selectable. Slots are only added during a session (see addSlot), never removed, so a slot survives undo.
    private static int[] buildSlots(Set<Integer> codePoints, boolean reserveCp437) {
        int[] ordered = SheetOrder.slotsFor(codePoints, reserveCp437);
        for (int slot = 0; slot < ordered.length; slot++) {
            if (reserveCp437 && slot < GlyphOrder.CP437.size()) {
                ordered[slot] = GlyphOrder.CP437.codePointAt(slot);
            } else if (ordered[slot] == SheetOrder.FILLER) {
                ordered[slot] = NO_GLYPH;
            }
        }
        return ordered;
    }

    public int cellWidth() {
        return cellWidth;
    }

    public int cellHeight() {
        return cellHeight;
    }

    public int slotCount() {
        return slots.length;
    }

    public int codePointAtSlot(int slot) {
        return slots[slot];
    }

    /**
     * The slot the cursor starts on: the slot of U+0021 when the font has no glyphs yet, otherwise the
     * first selectable slot. Falls back to slot 0 when nothing is selectable.
     */
    public int startSlot() {
        if (entries.isEmpty()) {
            int exclamationSlot = slotOf('!');
            if (exclamationSlot != NO_GLYPH) {
                return exclamationSlot;
            }
        }
        for (int slot = 0; slot < slots.length; slot++) {
            if (slots[slot] != NO_GLYPH) {
                return slot;
            }
        }
        return 0;
    }

    /**
     * Makes sure a code point has a slot. A new slot is empty and sits in normal
     * sheet order, so the indexes of later slots can shift. This neither adds a glyph nor counts as a
     * change: it touches no revision, modified flag, or undo history.
     *
     * @throws IllegalArgumentException if the code point is not a valid glyph code point
     */
    public void addSlot(int codePoint) {
        if (!Character.isValidCodePoint(codePoint) || codePoint == SheetOrder.FILLER) {
            throw new IllegalArgumentException(CodePoints.label(codePoint) + " cannot have a slot");
        }
        int existingSlot = slotOf(codePoint);
        if (existingSlot != NO_GLYPH) {
            return;
        }
        knownCodePoints.add(codePoint);
        slots = buildSlots(knownCodePoints, reserveCp437);
    }

    private int slotOf(int codePoint) {
        for (int slot = 0; slot < slots.length; slot++) {
            if (slots[slot] == codePoint) {
                return slot;
            }
        }
        return NO_GLYPH;
    }

    public boolean hasGlyph(int codePoint) {
        return entries.containsKey(codePoint);
    }

    public boolean isDerived(int codePoint) {
        return entries.get(codePoint) instanceof DerivedGlyph;
    }

    /** First source of a derived glyph, or {@link #NO_GLYPH} for any other glyph. */
    public int derivedFrom(int codePoint) {
        return entries.get(codePoint) instanceof DerivedGlyph derived
            ? derived.op().sources().get(0).codePoint()
            : NO_GLYPH;
    }

    /** The drawn pixels of a glyph with derived ones resolved, or null when the slot is empty. */
    public PixelGlyph glyph(int codePoint) {
        return resolved.get(codePoint);
    }

    /** The code points of every glyph that has at least one lit pixel, derived ones included. */
    public Set<Integer> codePointsWithInk() {
        Set<Integer> withInk = new HashSet<>();
        for (Map.Entry<Integer, PixelGlyph> entry : resolved.entrySet()) {
            if (entry.getValue().pixels().hasLitPixel()) {
                withInk.add(entry.getKey());
            }
        }
        return withInk;
    }

    public boolean isLit(int codePoint, int x, int y) {
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
    public boolean wasLitWhenSaved(int codePoint, int x, int y) {
        PixelGlyph glyph = savedGlyphs.get(codePoint);
        return glyph != null && x >= 0 && y >= 0
            && x < glyph.pixels().getWidth() && y < glyph.pixels().getHeight()
            && glyph.pixels().isLit(x, y);
    }

    public boolean isPaintable(int codePoint, int x, int y) {
        return codePoint != NO_GLYPH && !isDerived(codePoint)
            && x >= 0 && y >= 0 && x < cellWidth && y < cellHeight;
    }

    /** Identifies the current state; it changes with every edit, undo, redo, and derive. */
    public int revision() {
        return revision;
    }

    /**
     * Sets a pixel, creating the glyph first if the slot is empty.
     * An edit that would break a derived glyph is undone and explained by {@link #problem()}.
     */
    public void setPixel(int codePoint, int x, int y, boolean on) {
        if (!isPaintable(codePoint, x, y)) {
            return;
        }
        GlyphEntry before = entries.get(codePoint);
        PixelGlyph current = (PixelGlyph) before;
        if (!changesPixel(codePoint, x, y, on, current)) {
            return;
        }
        entries.put(codePoint, glyphWithPixel(codePoint, x, y, on, current));
        try {
            resolved = GlyphResolver.resolveAll(data());
        } catch (FontDataException e) {
            restore(codePoint, before);
            problem = e.getMessage();
            return;
        }
        problem = "";
        revision = nextRevision++;
    }

    private boolean changesPixel(int codePoint, int x, int y, boolean on, PixelGlyph current) {
        if (current == null) {
            return on;
        }
        return isLit(codePoint, x, y) != on;
    }

    private PixelGlyph glyphWithPixel(int codePoint, int x, int y, boolean on, PixelGlyph current) {
        PixelGrid base = current == null ? PixelGrid.blank(cellWidth, cellHeight) : current.pixels();
        return new PixelGlyph(codePoint, base.withPixel(x, y, on));
    }

    /** What {@link #previewDerived} found: the glyph (null when refused) and the reason, empty when allowed. */
    public record DerivePreview(PixelGlyph glyph, String problem) { }

    // The resolved font (null when refused) and the refusal reason, empty when allowed.
    private record Resolution(Map<Integer, PixelGlyph> glyphs, String problem) { }

    /**
     * Shows what {@link #derive} would produce. Changes nothing, including {@link #problem()}; a refusal is
     * explained by the returned {@link DerivePreview#problem()}.
     */
    public DerivePreview previewDerived(int targetCodePoint, GlyphOp op) {
        Resolution resolution = resolveWithDerived(targetCodePoint, op);
        if (resolution.glyphs() == null) {
            return new DerivePreview(null, resolution.problem());
        }
        return new DerivePreview(resolution.glyphs().get(targetCodePoint), "");
    }

    /**
     * Replaces the glyph in a slot with a derived one, as one undo step. Whatever was drawn there is lost
     * (see {@link #wouldReplaceDrawing}). A refusal changes nothing and is explained by {@link #problem()}.
     */
    public void derive(int targetCodePoint, GlyphOp op) {
        Resolution resolution = resolveWithDerived(targetCodePoint, op);
        problem = resolution.problem();
        if (resolution.glyphs() == null) {
            return;
        }
        changeAsOneStep(() -> {
            entries.put(targetCodePoint, new DerivedGlyph(targetCodePoint, op));
            resolved = resolution.glyphs();
        });
    }

    /**
     * Turns a derived glyph into a drawn one holding its current pixels, as one undo step. Does nothing when the
     * glyph is not derived (see {@link #isDerived}).
     */
    public void unlock(int codePoint) {
        if (!isDerived(codePoint)) {
            return;
        }
        PixelGlyph current = resolved.get(codePoint);
        changeAsOneStep(() -> entries.put(codePoint, new PixelGlyph(codePoint, current.pixels())));
    }

    /**
     * Replaces a slot's glyph with the given whole cell, as one undo step. Refuses when the code point is not
     * a slot, the cell is the wrong size, the glyph is derived, or a derived glyph would break;
     * {@link #problem()} then says why and nothing else changes.
     */
    public void replaceGlyph(int codePoint, PixelGrid cell) {
        if (!isSlotCodePoint(codePoint)) {
            problem = CodePoints.label(codePoint) + " is not a glyph slot";
            return;
        }
        if (cell.getWidth() != cellWidth || cell.getHeight() != cellHeight) {
            problem = "cell is " + cell.getWidth() + "x" + cell.getHeight()
                + " but the font cell is " + cellWidth + "x" + cellHeight;
            return;
        }
        if (isDerived(codePoint)) {
            problem = "derived glyph; unlock it first (U)";
            return;
        }
        PixelGlyph replacement = new PixelGlyph(codePoint, cell);
        Resolution resolution = resolveWithEntry(codePoint, replacement);
        problem = resolution.problem();
        if (resolution.glyphs() == null) {
            return;
        }
        changeAsOneStep(() -> {
            entries.put(codePoint, replacement);
            resolved = resolution.glyphs();
        });
    }

    // Resolves the font as if the entry were stored; no glyphs (with a problem) when a derived glyph would break.
    private Resolution resolveWithEntry(int codePoint, GlyphEntry entry) {
        Map<Integer, GlyphEntry> trial = new TreeMap<>(entries);
        trial.put(codePoint, entry);
        try {
            return new Resolution(GlyphResolver.resolveAll(new FontData(cellWidth, cellHeight, trial)), "");
        } catch (FontDataException e) {
            return refused(e.getMessage());
        }
    }

    /** Whether deriving into this slot would discard a drawing, meaning it holds a drawn glyph with a lit pixel. */
    public boolean wouldReplaceDrawing(int codePoint) {
        return entries.get(codePoint) instanceof PixelGlyph drawn && drawn.pixels().hasLitPixel();
    }

    // Resolves the font as if the target were derived; no glyphs (with a problem) when that is refused.
    private Resolution resolveWithDerived(int targetCodePoint, GlyphOp op) {
        if (!isSlotCodePoint(targetCodePoint)) {
            return refused(CodePoints.label(targetCodePoint) + " is not a glyph slot");
        }
        for (GlyphOp.Source source : op.sources()) {
            if (!entries.containsKey(source.codePoint())) {
                return refused("source " + CodePoints.label(source.codePoint()) + " is empty; draw it first");
            }
        }
        return resolveWithEntry(targetCodePoint, new DerivedGlyph(targetCodePoint, op));
    }

    private static Resolution refused(String reason) {
        return new Resolution(null, reason);
    }

    private boolean isSlotCodePoint(int codePoint) {
        for (int slotCodePoint : slots) {
            if (slotCodePoint == codePoint && codePoint != NO_GLYPH) {
                return true;
            }
        }
        return false;
    }

    // Joins an already open edit instead of ending it early.
    private void changeAsOneStep(Runnable change) {
        boolean ownsEdit = openEdit == null;
        beginEdit();
        change.run();
        revision = nextRevision++;
        if (ownsEdit) {
            commitEdit();
        }
    }

    /**
     * Starts one undoable edit; every change until {@link #commitEdit()} becomes a single step.
     * Changes made outside an edit are not undoable.
     */
    public void beginEdit() {
        if (openEdit == null) {
            openEdit = capture();
        }
    }

    /** Ends the open edit. It becomes an undo step and clears redo, unless it left the glyphs as they were. */
    public void commitEdit() {
        if (openEdit == null) {
            return;
        }
        State before = openEdit;
        openEdit = null;
        if (before.entries().equals(entries)) {
            revision = before.revision();
            return;
        }
        undoSteps.push(before);
        if (undoSteps.size() > MAX_UNDO_STEPS) {
            undoSteps.pollLast();
        }
        redoSteps.clear();
    }

    public boolean canUndo() {
        return !undoSteps.isEmpty();
    }

    public boolean canRedo() {
        return !redoSteps.isEmpty();
    }

    /** Undoes the latest step; does nothing while an edit is open or when there is nothing to undo. */
    public void undo() {
        travel(undoSteps, redoSteps);
    }

    /** Redoes the latest undone step; does nothing while an edit is open or when there is nothing to redo. */
    public void redo() {
        travel(redoSteps, undoSteps);
    }

    /** The code point of the glyph {@link #undo()} would change, or {@link #NO_GLYPH} if it would do nothing. */
    public int undoTarget() {
        return travelTarget(undoSteps);
    }

    /** The code point of the glyph {@link #redo()} would change, or {@link #NO_GLYPH} if it would do nothing. */
    public int redoTarget() {
        return travelTarget(redoSteps);
    }

    private int travelTarget(Deque<State> from) {
        if (openEdit != null || from.isEmpty()) {
            return NO_GLYPH;
        }
        return firstDifference(entries, from.peek().entries());
    }

    private void travel(Deque<State> from, Deque<State> to) {
        if (openEdit != null || from.isEmpty()) {
            return;
        }
        State target = from.pop();
        to.push(capture());
        entries.clear();
        entries.putAll(target.entries());
        resolved = GlyphResolver.resolveAll(data());
        revision = target.revision();
        problem = "";
    }

    private State capture() {
        return new State(new TreeMap<>(entries), revision);
    }

    private static int firstDifference(Map<Integer, GlyphEntry> first, Map<Integer, GlyphEntry> second) {
        for (Map.Entry<Integer, GlyphEntry> entry : first.entrySet()) {
            if (!entry.getValue().equals(second.get(entry.getKey()))) {
                return entry.getKey();
            }
        }
        for (Integer codePoint : second.keySet()) {
            if (!first.containsKey(codePoint)) {
                return codePoint;
            }
        }
        return NO_GLYPH;
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
    public String toText() {
        return FontDataWriter.write(data());
    }

    public boolean isModified() {
        return revision != savedRevision;
    }

    public void markSaved() {
        savedRevision = revision;
        savedGlyphs = resolved;
    }

    public String problem() {
        return problem;
    }
}
