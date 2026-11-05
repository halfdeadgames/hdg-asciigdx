package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphOp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The state of deriving one glyph from another: choosing the operation in a menu, picking the source
 * slot, and confirming a replacement. It holds no drawing or input code.
 */
final class DeriveFlow {

    enum Mode { IDLE, MENU, PICK_SOURCE, CONFIRM_REPLACE }

    /** The operations offered by the menu, in menu order. */
    static final List<GlyphOp.Kind> OPERATIONS = List.of(
        GlyphOp.Kind.MIRROR, GlyphOp.Kind.FLIP, GlyphOp.Kind.ROTATE90,
        GlyphOp.Kind.ROTATE180, GlyphOp.Kind.ROTATE270, GlyphOp.Kind.INVERT, GlyphOp.Kind.OVERLAY);

    /** One committed overlay source: the slot it comes from and how far it is shifted. */
    record Part(int slot, int shift) { }

    private final List<Part> parts = new ArrayList<>();
    private int pendingShift;
    private Mode mode = Mode.IDLE;
    private int menuIndex;
    private int targetSlot;
    private int sourceSlot;
    private GlyphOp.Kind operation = OPERATIONS.get(0);

    static String label(GlyphOp.Kind kind) {
        return switch (kind) {
            case ROTATE90 -> "ROTATE 90";
            case ROTATE180 -> "ROTATE 180";
            case ROTATE270 -> "ROTATE 270";
            default -> kind.name();
        };
    }

    Mode mode() {
        return mode;
    }

    boolean isActive() {
        return mode != Mode.IDLE;
    }

    boolean isPicking() {
        return mode == Mode.PICK_SOURCE;
    }

    int menuIndex() {
        return menuIndex;
    }

    int targetSlot() {
        return targetSlot;
    }

    int sourceSlot() {
        return sourceSlot;
    }

    GlyphOp.Kind operation() {
        return operation;
    }

    void openMenu(int targetSlot) {
        this.targetSlot = targetSlot;
        sourceSlot = targetSlot;
        menuIndex = 0;
        clearOverlayState();
        mode = Mode.MENU;
    }

    boolean isOverlay() {
        return operation == GlyphOp.Kind.OVERLAY
            && (mode == Mode.PICK_SOURCE || mode == Mode.CONFIRM_REPLACE);
    }

    boolean sourceIsTarget() {
        return sourceSlot == targetSlot;
    }

    int pendingShift() {
        return pendingShift;
    }

    /** Adds to the shift of the source being picked; only overlay source picking uses it. */
    void shiftSource(int delta) {
        if (isOverlayPicking()) {
            pendingShift += delta;
        }
    }

    void addPart() {
        if (!isOverlayPicking() || sourceIsTarget()) {
            return;
        }
        parts.add(new Part(sourceSlot, pendingShift));
        pendingShift = 0;
    }

    /** Whether an overlay is being picked and has a committed part that {@link #removeLastPart} would remove. */
    boolean canRemovePart() {
        return isOverlayPicking() && !parts.isEmpty();
    }

    void removeLastPart() {
        if (!canRemovePart()) {
            return;
        }
        parts.remove(parts.size() - 1);
    }

    List<Part> parts() {
        return Collections.unmodifiableList(new ArrayList<>(parts));
    }

    /** The committed parts followed by the part being picked, which counts only while the source is not the target. */
    List<Part> partsWithPending() {
        List<Part> all = new ArrayList<>(parts);
        if (isOverlayPicking() && !sourceIsTarget()) {
            all.add(new Part(sourceSlot, pendingShift));
        }
        return all;
    }

    /** Builds the overlay operation for parts, looking each slot up in {@code codePoints}. */
    static GlyphOp overlayOp(List<Part> parts, int[] codePoints) {
        List<GlyphOp.Source> sources = new ArrayList<>(parts.size());
        for (Part part : parts) {
            sources.add(new GlyphOp.Source(codePoints[part.slot()], part.shift()));
        }
        return new GlyphOp(GlyphOp.Kind.OVERLAY, sources);
    }

    private boolean isOverlayPicking() {
        return mode == Mode.PICK_SOURCE && operation == GlyphOp.Kind.OVERLAY;
    }

    private void clearOverlayState() {
        parts.clear();
        pendingShift = 0;
    }

    /** Moves the highlighted menu row, wrapping at both ends. */
    void moveMenu(int delta) {
        if (mode != Mode.MENU) {
            return;
        }
        int count = OPERATIONS.size();
        menuIndex = Math.floorMod(menuIndex + delta, count);
    }

    void highlightMenuRow(int index) {
        if (mode == Mode.MENU && index >= 0 && index < OPERATIONS.size()) {
            menuIndex = index;
        }
    }

    /** Takes the highlighted operation and starts picking, with the source on the target. */
    void chooseOperation() {
        if (mode != Mode.MENU) {
            return;
        }
        operation = OPERATIONS.get(menuIndex);
        sourceSlot = targetSlot;
        clearOverlayState();
        mode = Mode.PICK_SOURCE;
    }

    void moveSource(int slot) {
        if (mode != Mode.PICK_SOURCE || slot == sourceSlot) {
            return;
        }
        sourceSlot = slot;
    }

    /** Whether a click on this slot lands on the current source while picking, which confirms it. */
    boolean isConfirmingClick(int slot) {
        return mode == Mode.PICK_SOURCE && slot == sourceSlot;
    }

    /** Moves the source to the clicked slot; a click on the current source confirms it instead and moves nothing. */
    void clickSource(int slot) {
        if (mode != Mode.PICK_SOURCE) {
            return;
        }
        sourceSlot = slot;
    }

    /** Asks for confirmation before replacing a drawing. */
    void askReplace() {
        if (mode == Mode.PICK_SOURCE) {
            mode = Mode.CONFIRM_REPLACE;
        }
    }

    /** Backs out one level: the menu and source picking end the flow, the replace question returns to picking. */
    void cancel() {
        if (mode == Mode.CONFIRM_REPLACE) {
            mode = Mode.PICK_SOURCE;
        } else {
            clearOverlayState();
            mode = Mode.IDLE;
        }
    }

    /** Ends the flow from any mode; unlike {@link #cancel}, the replace question does not step back to picking. */
    void cancelAll() {
        clearOverlayState();
        mode = Mode.IDLE;
    }

    void finish() {
        clearOverlayState();
        mode = Mode.IDLE;
    }
}
