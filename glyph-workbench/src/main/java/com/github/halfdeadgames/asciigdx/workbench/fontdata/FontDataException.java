package com.github.halfdeadgames.asciigdx.workbench.fontdata;

/** A font data file or glyph derivation is invalid. */
public final class FontDataException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Line number the problem was found on, or 0 when it has no line. */
    private final int line;

    /**
     * Creates an exception.
     *
     * @param line 1-based line number, or 0 when unknown
     * @param problem what is wrong
     */
    public FontDataException(int line, String problem) {
        super(line > 0 ? "Line " + line + ": " + problem : problem);
        this.line = line;
    }

    /** Returns the 1-based line number the problem was found on, or 0 when unknown. */
    public int getLine() {
        return line;
    }
}
