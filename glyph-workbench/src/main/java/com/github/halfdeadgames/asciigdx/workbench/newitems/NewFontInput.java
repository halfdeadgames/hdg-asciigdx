package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Validation and sizing rules for the "new font" prompt. */
final class NewFontInput {

    /** The largest width or height of a cell, in pixels. */
    static final int MAX_CELL_SIDE = 128;

    private static final int MAX_NAME_LENGTH = 32;
    private static final List<String> RESERVED_NAMES = List.of(
        "con", "prn", "aux", "nul",
        "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
        "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9");

    private NewFontInput() {
    }

    /**
     * Checks a font folder name.
     *
     * @param name the typed name
     * @param existingFolders folder names already in use
     * @return null when the name is fine, otherwise a user-facing message
     */
    static String nameError(String name, Collection<String> existingFolders) {
        if (name == null || name.isEmpty()) {
            return "Type a font name";
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return "Use at most 32 characters";
        }
        if (!isAllowedName(name)) {
            return "Use only a-z, 0-9, and hyphens";
        }
        if (RESERVED_NAMES.contains(name)) {
            return "Windows reserves that name";
        }
        for (var existing : existingFolders) {
            if (existing.toLowerCase(Locale.ROOT).equals(name)) {
                return "A font with that name already exists";
            }
        }
        return null;
    }

    /**
     * Checks a cell size against the largest side allowed, then against the allowed shapes (square or twice as
     * tall). {@link CellSize} itself already refuses sides below 1.
     *
     * @param size the cell size
     * @return null when the size is fine, otherwise a user-facing message
     */
    static String sizeError(CellSize size) {
        if (size.width() > MAX_CELL_SIDE || size.height() > MAX_CELL_SIDE) {
            return "Width and height go up to " + MAX_CELL_SIDE;
        }
        if (!size.isSquareOrTall()) {
            return "Must be square or 2x tall";
        }
        return null;
    }

    /**
     * Checks a size typed as {@code WxH}. Surrounding spaces and a capital X are accepted.
     *
     * @param typed the typed text
     * @return null when the size is fine, otherwise a user-facing message
     */
    static String typedSizeError(String typed) {
        String normalized = normalizeTypedSize(typed);
        if (normalized.isEmpty()) {
            return "Type a size like 16x16";
        }
        CellSize parsed = parseOrNull(normalized);
        if (parsed == null) {
            return unparsedSizeError(normalized);
        }
        return sizeError(parsed);
    }

    /**
     * Reads a size typed as {@code WxH}, with the same leniency as {@link #typedSizeError}.
     *
     * @param typed text that {@link #typedSizeError} accepts
     * @return the parsed size
     * @throws IllegalArgumentException if the text is not an acceptable size
     */
    static CellSize typedSize(String typed) {
        String problem = typedSizeError(typed);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        return CellSize.parse(normalizeTypedSize(typed));
    }

    private static String normalizeTypedSize(String typed) {
        if (typed == null) {
            return "";
        }
        return typed.trim().replace('X', 'x');
    }

    // Digits-x-digits that CellSize still refused: a zero side, or a side too long to be a sane number.
    private static String unparsedSizeError(String normalized) {
        if (!normalized.matches("\\d+x\\d+")) {
            return "Write the size as WxH, e.g. 16x16";
        }
        if (normalized.matches("0+x\\d+|\\d+x0+")) {
            return "Width and height start at 1";
        }
        return "Width and height go up to " + MAX_CELL_SIDE;
    }

    private static CellSize parseOrNull(String normalized) {
        try {
            return CellSize.parse(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isAllowedName(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean allowed = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-';
            if (!allowed) {
                return false;
            }
        }
        return true;
    }
}
