package com.github.halfdeadgames.asciigdx.workbench.fontdata;

import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Decides which glyph goes in which sheet slot. The first 256 slots follow Code Page 437;
 * then come the other glyphs by code point, each Unicode block starting on a new row; then
 * the private use glyphs, laid out the same way.
 *
 * <p>The character list file cannot leave a slot out, so empty slots hold {@link #FILLER}.
 */
public final class SheetOrder {

    /** Columns per sheet row. */
    public static final int COLUMNS = 16;

    /** Code point listed for an empty slot: the last private use code point, never a real glyph. */
    public static final int FILLER = 0x10FFFD;

    private SheetOrder() {
    }

    /**
     * Returns the slots for a set of glyphs.
     *
     * @param codePoints the glyphs the font has
     * @return code points by slot, with {@link #FILLER} in empty slots
     */
    public static int[] slotsFor(Collection<Integer> codePoints) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < GlyphOrder.CP437.size(); slot++) {
            int codePoint = GlyphOrder.CP437.codePointAt(slot);
            slots.add(codePoints.contains(codePoint) ? codePoint : FILLER);
        }
        TreeSet<Integer> others = new TreeSet<>();
        TreeSet<Integer> privateUse = new TreeSet<>();
        for (int codePoint : codePoints) {
            if (GlyphOrder.CP437.contains(codePoint)) {
                continue;
            }
            (isPrivateUse(codePoint) ? privateUse : others).add(codePoint);
        }
        appendByBlock(slots, others);
        appendByBlock(slots, privateUse);
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void appendByBlock(List<Integer> slots, Collection<Integer> sortedCodePoints) {
        Character.UnicodeBlock currentBlock = null;
        for (int codePoint : sortedCodePoints) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
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

    private static boolean isPrivateUse(int codePoint) {
        return Character.getType(codePoint) == Character.PRIVATE_USE;
    }
}
