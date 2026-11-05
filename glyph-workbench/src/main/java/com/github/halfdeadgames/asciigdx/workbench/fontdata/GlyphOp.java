package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import java.util.List;
import java.util.Locale;

/**
 * How a derived glyph is made from other glyphs.
 *
 * @param kind the operation
 * @param sources glyphs it reads; exactly one unless {@code kind} is {@link Kind#OVERLAY}
 */
public record GlyphOp(Kind kind, List<Source> sources) {

    /** The operations a recipe can use. */
    public enum Kind {
        INVERT, ROTATE90, ROTATE180, ROTATE270, MIRROR, FLIP, OVERLAY;

        /** Returns the keyword used in data files. */
        public String keyword() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * A glyph an operation reads.
     *
     * @param codePoint the glyph's code point
     * @param shift columns to move it right before combining; 0 except for overlays
     */
    public record Source(int codePoint, int shift) {
    }

    public GlyphOp {
        sources = List.copyOf(sources);
        boolean overlay = kind == Kind.OVERLAY;
        if (overlay ? sources.isEmpty() : sources.size() != 1) {
            throw new IllegalArgumentException(kind.keyword() + " needs "
                + (overlay ? "at least one source" : "exactly one source"));
        }
    }
}
