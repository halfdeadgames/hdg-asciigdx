package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.workbench.unicodenames.UnicodeFacts;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Decides which glyph goes in which sheet slot. When Code Page 437 is reserved, the first 256 slots
 * follow it; then come the other glyphs by code point, each Unicode block starting on a new row; then
 * the private use glyphs, laid out the same way.
 *
 * <p>The character list file cannot leave a slot out, so empty slots hold {@link #FILLER}.
 */
public final class SheetOrder {

    /** Columns per sheet row. */
    static final int COLUMNS = 16;

    /** Code point listed for an empty slot: the last private use code point, never a real glyph. */
    public static final int FILLER = 0x10FFFD;

    private SheetOrder() {
    }

    /**
     * Returns the slots for a set of glyphs.
     *
     * @param codePoints the glyphs the font has
     * @param reserveCp437 whether the first 256 slots are reserved for Code Page 437; when false, CP437
     *        code points are placed with the others by code point and Unicode block
     * @return code points by slot, with {@link #FILLER} in empty slots
     */
    public static int[] slotsFor(Collection<Integer> codePoints, boolean reserveCp437) {
        return slotsFor(codePoints, reserveCp437, UnicodeFacts.bundled());
    }

    /** Same as {@link #slotsFor(Collection, boolean)}, with the Unicode facts given by the caller. */
    public static int[] slotsFor(Collection<Integer> codePoints, boolean reserveCp437, UnicodeFacts facts) {
        List<Integer> slots = new ArrayList<>();
        if (reserveCp437) {
            for (int slot = 0; slot < GlyphOrder.CP437.getGlyphCount(); slot++) {
                int codePoint = GlyphOrder.CP437.getCodePoint(slot);
                slots.add(codePoints.contains(codePoint) ? codePoint : FILLER);
            }
        }
        TreeSet<Integer> others = new TreeSet<>();
        TreeSet<Integer> privateUse = new TreeSet<>();
        for (int codePoint : codePoints) {
            if (reserveCp437 && GlyphOrder.CP437.contains(codePoint)) {
                continue;
            }
            (isPrivateUse(codePoint, facts) ? privateUse : others).add(codePoint);
        }
        appendByBlock(slots, others, facts);
        appendByBlock(slots, privateUse, facts);
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void appendByBlock(List<Integer> slots, Collection<Integer> sortedCodePoints,
                                      UnicodeFacts facts) {
        String currentBlock = null;
        for (int codePoint : sortedCodePoints) {
            String block = facts.blockOf(codePoint);
            if (!Objects.equals(block, currentBlock)) {
                while (slots.size() % COLUMNS != 0) {
                    slots.add(FILLER);
                }
                currentBlock = block;
            }
            slots.add(codePoint);
        }
        while (slots.size() % COLUMNS != 0) {
            slots.add(FILLER);
        }
    }

    private static boolean isPrivateUse(int codePoint, UnicodeFacts facts) {
        return facts.type(codePoint) == Character.PRIVATE_USE;
    }
}
