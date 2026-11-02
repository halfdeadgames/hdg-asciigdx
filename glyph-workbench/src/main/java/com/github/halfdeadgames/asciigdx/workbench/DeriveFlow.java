package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphOp;
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
        GlyphOp.Kind.ROTATE180, GlyphOp.Kind.ROTATE270, GlyphOp.Kind.INVERT);

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
        mode = Mode.MENU;
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
        mode = Mode.PICK_SOURCE;
    }

    /** @return whether the source slot changed */
    boolean moveSource(int slot) {
        if (mode != Mode.PICK_SOURCE || slot == sourceSlot) {
            return false;
        }
        sourceSlot = slot;
        return true;
    }

    /** @return true when the click was on the current source, which confirms it; otherwise the source moves there */
    boolean clickSource(int slot) {
        if (mode != Mode.PICK_SOURCE) {
            return false;
        }
        if (slot == sourceSlot) {
            return true;
        }
        sourceSlot = slot;
        return false;
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
            mode = Mode.IDLE;
        }
    }

    void finish() {
        mode = Mode.IDLE;
    }
}
