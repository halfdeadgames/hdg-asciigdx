package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrderText;
import java.util.Objects;

/** Tells which code points a candidate sheet really has a glyph for. */
final class GlyphAvailability {

    private static final GlyphAvailability EVERY_GLYPH = new GlyphAvailability(null);

    private final GlyphOrder listed;

    private GlyphAvailability(GlyphOrder listed) {
        this.listed = listed;
    }

    /** Returns an availability that includes every code point. */
    static GlyphAvailability everyGlyph() {
        return EVERY_GLYPH;
    }

    /** Returns an availability that includes only the code points in the sidecar text. */
    static GlyphAvailability listedIn(String sidecarText) {
        Objects.requireNonNull(sidecarText, "sidecarText must not be null");
        return new GlyphAvailability(GlyphOrderText.readAnyRowLength(sidecarText));
    }

    boolean includes(int codePoint) {
        return listed == null || listed.contains(codePoint);
    }
}
