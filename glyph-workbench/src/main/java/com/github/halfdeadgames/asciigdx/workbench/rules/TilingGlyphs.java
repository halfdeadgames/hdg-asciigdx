package com.github.halfdeadgames.asciigdx.workbench.rules;

/** Box drawing and block element glyphs fill the whole cell and join with neighbours, so letter metrics do not apply. */
public final class TilingGlyphs {

    private static final int BOX_DRAWING_FIRST = 0x2500;
    private static final int BOX_DRAWING_LAST = 0x257F;
    private static final int BLOCK_ELEMENTS_FIRST = 0x2580;
    private static final int BLOCK_ELEMENTS_LAST = 0x259F;

    private TilingGlyphs() {
    }

    /** Whether the code point lies in the Box Drawing or Block Elements block. */
    public static boolean isTiling(int codePoint) {
        return isInRange(codePoint, BOX_DRAWING_FIRST, BOX_DRAWING_LAST)
            || isInRange(codePoint, BLOCK_ELEMENTS_FIRST, BLOCK_ELEMENTS_LAST);
    }

    private static boolean isInRange(int codePoint, int first, int last) {
        return codePoint >= first && codePoint <= last;
    }
}
