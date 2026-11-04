package com.github.halfdeadgames.asciigdx.workbench;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Validation and sizing rules for the "new font" prompt. */
final class NewFontInput {

    /** Cell proportions: square is W x W, tall is W x 2W. */
    enum Shape { SQUARE, TALL }

    private static final int MAX_NAME_LENGTH = 32;
    private static final int STEP = 8;
    private static final int MAX_HEIGHT = 64;
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
     * Checks a cell height for the chosen shape.
     *
     * @param height the height the font file is named after
     * @param shape the cell proportions
     * @return null when the size is fine, otherwise a user-facing message
     */
    static String sizeError(int height, Shape shape) {
        if (shape == Shape.SQUARE) {
            boolean valid = height >= STEP && height <= MAX_HEIGHT && height % STEP == 0;
            return valid ? null : "Square heights are 8, 16, 24, up to 64";
        }
        boolean valid = height >= 2 * STEP && height <= MAX_HEIGHT && height % (2 * STEP) == 0;
        return valid ? null : "Tall heights are 16, 32, 48, or 64";
    }

    /** Returns the cell width: the height for square cells, half of it for tall cells. */
    static int cellWidth(int height, Shape shape) {
        return shape == Shape.SQUARE ? height : height / 2;
    }

    /** Returns SQUARE when the cell is as wide as it is high, otherwise TALL. */
    static Shape shapeOf(int cellWidth, int cellHeight) {
        return cellWidth == cellHeight ? Shape.SQUARE : Shape.TALL;
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
