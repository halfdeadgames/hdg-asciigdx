package com.github.halfdeadgames.asciigdx.sandbox.review;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrderText;

import java.util.Arrays;
import java.util.function.IntPredicate;

/** The code points to review, with the number of requested ones the glyph source cannot draw. */
public final class ReviewList {
    private final int[] codePoints;
    private final int missingCount;

    private ReviewList(int[] codePoints, int missingCount) {
        this.codePoints = codePoints;
        this.missingCount = missingCount;
    }

    /**
     * Reads a glyph order text file, keeps each distinct code point once in first-seen order, and
     * drops those the source lacks.
     *
     * @throws IllegalArgumentException when the text is malformed or no code point is left
     */
    public static ReviewList fromText(String text, IntPredicate hasGlyph) {
        GlyphOrder order = GlyphOrderText.readAnyRowLength(text);
        int[] kept = new int[order.size()];
        int keptCount = 0;
        int missing = 0;
        for (int slot = 0; slot < order.size(); slot++) {
            int codePoint = order.codePointAt(slot);
            if (order.slotOf(codePoint) != slot) {
                continue;
            }
            if (hasGlyph.test(codePoint)) {
                kept[keptCount++] = codePoint;
            } else {
                missing++;
            }
        }
        if (keptCount == 0) {
            throw new IllegalArgumentException("None of the " + missing + " listed glyphs are in the sheet");
        }
        return new ReviewList(Arrays.copyOf(kept, keptCount), missing);
    }

    public int[] codePoints() {
        return codePoints.clone();
    }

    /** For each code point in {@link #codePoints()} order, whether the given source can draw it. */
    public boolean[] presentIn(IntPredicate hasGlyph) {
        boolean[] present = new boolean[codePoints.length];
        for (int i = 0; i < present.length; i++) {
            present[i] = hasGlyph.test(codePoints[i]);
        }
        return present;
    }

    public int missingCount() {
        return missingCount;
    }
}
