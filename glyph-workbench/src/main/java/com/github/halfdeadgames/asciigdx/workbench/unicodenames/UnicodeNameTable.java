package com.github.halfdeadgames.asciigdx.workbench.unicodenames;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Names, General_Category types, and blocks of characters, read from a bundled text file. Single entries
 * and pattern ranges such as "CJK UNIFIED IDEOGRAPH-*" (whose names end in the code point in hex) share
 * one sorted list of non-overlapping ranges.
 */
public final class UnicodeNameTable {

    static final String RESOURCE_PATH = "/unicode-names.txt";

    private final int[] firsts;
    private final int[] lasts;
    private final String[] texts;
    private final byte[] types;
    private final boolean[] patterns;
    private final UnicodeBlocks blocks;

    UnicodeNameTable(int[] firsts, int[] lasts, String[] texts, byte[] types, boolean[] patterns,
                     UnicodeBlocks blocks) {
        this.firsts = firsts;
        this.lasts = lasts;
        this.texts = texts;
        this.types = types;
        this.patterns = patterns;
        this.blocks = blocks;
    }

    /** Reads the bundled file; a missing or malformed file is a packaging bug, so it fails loudly. */
    public static UnicodeNameTable loadBundled() {
        try (InputStream stream = UnicodeNameTable.class.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing resource " + RESOURCE_PATH);
            }
            return parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read resource " + RESOURCE_PATH, e);
        }
    }

    /** See {@link UnicodeNameTableParser} for the file format and the errors. */
    static UnicodeNameTable parse(String text) {
        return new UnicodeNameTableParser().parse(text);
    }

    /** Returns the name of the code point, or null if the table has none. */
    public String nameOf(int codePoint) {
        int index = indexOf(codePoint);
        if (index < 0) {
            return null;
        }
        return patterns[index] ? texts[index] + String.format("%04X", codePoint) : texts[index];
    }

    /** Returns the Character type constant of the code point, or {@link Character#UNASSIGNED} if not listed. */
    public byte typeOf(int codePoint) {
        int index = indexOf(codePoint);
        return index < 0 ? Character.UNASSIGNED : types[index];
    }

    /** Returns the name of the Unicode block holding the code point, or null if it is in none. */
    public String blockOf(int codePoint) {
        return blocks.blockOf(codePoint);
    }

    private int indexOf(int codePoint) {
        return CodePointRanges.indexOfRangeContaining(firsts, lasts, codePoint);
    }
}
