package com.github.halfdeadgames.asciigdx.workbench;

/** The two-step verdict menu: PERFECT or NEEDS WORK, then a reason. Picking none skips the first step. */
final class VerdictMenu {

    enum Stage { CLOSED, VERDICT, REASON }

    private static final String[] VERDICT_LABELS = {"PERFECT", "NEEDS WORK"};
    private static final String[] REASON_LABELS = {
        "UNREADABLE AT 1X",
        "OFF-CENTER",
        "TOO SIMILAR",
        "UGLY / WRONG SHAPE",
        "WRONG ORIENTATION",
        "CORNERS ROUNDED",
        "BREAKS TILING",
        "NO REASON"
    };
    private static final String[] REASON_VALUES = {
        "unreadable-at-1x",
        "off-center",
        "too-similar",
        "ugly-wrong-shape",
        "wrong-orientation",
        "corners-rounded",
        "breaks-tiling",
        ""
    };

    private Stage stage = Stage.CLOSED;
    private int focus;
    private String verdict = "";
    private String reason = "";

    void open(boolean none) {
        verdict = none ? GlyphAnswer.NEEDS_HAND_DRAWN : "";
        reason = "";
        stage = none ? Stage.REASON : Stage.VERDICT;
        focus = 0;
    }

    void close() {
        stage = Stage.CLOSED;
        focus = 0;
    }

    boolean isOpen() {
        return stage != Stage.CLOSED;
    }

    Stage stage() {
        return stage;
    }

    String title() {
        return stage == Stage.VERDICT ? "HOW IS IT?" : "WHAT IS WRONG?";
    }

    int rowCount() {
        switch (stage) {
            case VERDICT:
                return VERDICT_LABELS.length;
            case REASON:
                return REASON_LABELS.length;
            default:
                return 0;
        }
    }

    String rowLabel(int index) {
        return stage == Stage.VERDICT ? VERDICT_LABELS[index] : REASON_LABELS[index];
    }

    int focus() {
        return focus;
    }

    void moveFocus(int delta) {
        int count = rowCount();
        if (count > 0) {
            focus = Math.floorMod(focus + delta, count);
        }
    }

    void setFocus(int index) {
        if (index >= 0 && index < rowCount()) {
            focus = index;
        }
    }

    /** Returns true when the answer is complete; the menu is then closed and verdict and reason are set. */
    boolean choose(int index) {
        if (index < 0 || index >= rowCount()) {
            return false;
        }
        if (stage == Stage.VERDICT) {
            if (index == 0) {
                verdict = GlyphAnswer.PERFECT;
                reason = "";
                close();
                return true;
            }
            verdict = GlyphAnswer.NEEDS_WORK;
            stage = Stage.REASON;
            focus = 0;
            return false;
        }
        reason = REASON_VALUES[index];
        close();
        return true;
    }

    String verdict() {
        return verdict;
    }

    String reason() {
        return reason;
    }

    /** Returns the row under a pixel for rows of equal height stacked below {@code firstRowTop}, or -1. */
    static int rowAt(int pixelX, int pixelY, int left, int firstRowTop, int width, int rowHeight, int rows) {
        if (pixelX < left || pixelX >= left + width || pixelY < firstRowTop) {
            return -1;
        }
        int index = (pixelY - firstRowTop) / rowHeight;
        return index < rows ? index : -1;
    }
}
