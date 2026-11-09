package com.github.halfdeadgames.asciigdx.workbench.newitems;

/** What the first-glyph page does once it looks at its dialog. */
enum FirstGlyphClose {
    /** The dialog is still open. */
    WAIT,
    /** The dialog closed after a failed save; ask again. */
    REOPEN,
    /** The dialog closed without adding anything. */
    GO_BACK,
    /** A glyph was added; the page is finished. */
    FINISHED;

    static FirstGlyphClose decide(boolean dialogOpen, boolean added, boolean saveFailed) {
        if (dialogOpen) {
            return WAIT;
        }
        if (added) {
            return FINISHED;
        }
        return saveFailed ? REOPEN : GO_BACK;
    }
}
