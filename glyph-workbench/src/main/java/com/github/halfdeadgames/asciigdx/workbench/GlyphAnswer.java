package com.github.halfdeadgames.asciigdx.workbench;

/** The user's answer for one glyph: which candidate was picked, the verdict, an optional reason, and a free note. */
record GlyphAnswer(int codePoint, String pick, String verdict, String reason, String note) {

    static final String PERFECT = "perfect";
    static final String NEEDS_WORK = "needs-work";
    static final String NEEDS_HAND_DRAWN = "needs-hand-drawn";
    static final String NONE_PICK = "none";
    /** Verdict spelling written by older versions; read as {@link #NEEDS_HAND_DRAWN}. */
    static final String LEGACY_NONE_VERDICT = "none";

    GlyphAnswer(int codePoint, String pick, String verdict, String reason) {
        this(codePoint, pick, verdict, reason, "");
    }

    GlyphAnswer withNote(String newNote) {
        return new GlyphAnswer(codePoint, pick, verdict, reason, newNote);
    }

    /** No candidate fits: needs hand drawn. Otherwise a note means there is something to fix. */
    static String verdictFor(String pick, String note) {
        if (NONE_PICK.equals(pick)) {
            return NEEDS_HAND_DRAWN;
        }
        return note.isEmpty() ? PERFECT : NEEDS_WORK;
    }

    static boolean isKnownVerdict(String verdict) {
        return PERFECT.equals(verdict) || NEEDS_WORK.equals(verdict) || NEEDS_HAND_DRAWN.equals(verdict)
            || LEGACY_NONE_VERDICT.equals(verdict);
    }
}
