package com.github.halfdeadgames.asciigdx.workbench.rules;

import java.util.Optional;

/** The checks a rule can name, spelled in the rules file as {@link #fileName()}. */
enum RuleCheck {
    NO_INK_ROWS("noInkRows"),
    NO_INK_COLUMNS("noInkColumns"),
    RING("ring"),
    CENTERED_X("centeredX"),
    CENTERED_BOTH("centeredBoth");

    private final String fileName;

    RuleCheck(String fileName) {
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }

    /**
     * Looks a check up by the name used in the rules file.
     *
     * @param fileName the spelling in the file, case sensitive
     * @return the check, or empty if no check has that name
     */
    public static Optional<RuleCheck> fromFileName(String fileName) {
        for (RuleCheck check : values()) {
            if (check.fileName.equals(fileName)) {
                return Optional.of(check);
            }
        }
        return Optional.empty();
    }
}
