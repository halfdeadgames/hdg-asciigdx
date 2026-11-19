package com.github.halfdeadgames.asciigdx.draw;

/**
 * Line style of a border drawn by
 * {@link GridDrawing#drawBox(com.github.halfdeadgames.asciigdx.grid.TerminalGrid, int, int, int,
 * int, BoxStyle, int, int)}.
 *
 * <p>Each style uses six Unicode box-drawing glyphs: horizontal and vertical edges plus four
 * corners. The font must contain these glyphs for the border to look right.
 *
 * <p>Constants may be added in later releases, so a {@code switch} over this enum should include
 * a {@code default} branch.
 */
public enum BoxStyle {

    /** Thin single lines with square corners: {@code ─ │ ┌ ┐ └ ┘}. */
    SINGLE('─', '│', '┌', '┐', '└', '┘'),

    /** Double lines with square corners: {@code ═ ║ ╔ ╗ ╚ ╝}. */
    DOUBLE('═', '║', '╔', '╗', '╚', '╝'),

    /** Thick single lines with square corners: {@code ━ ┃ ┏ ┓ ┗ ┛}. */
    HEAVY('━', '┃', '┏', '┓', '┗', '┛'),

    /** Thin single lines with rounded corners: {@code ─ │ ╭ ╮ ╰ ╯}. */
    ROUNDED('─', '│', '╭', '╮', '╰', '╯');

    private final int horizontalGlyph;
    private final int verticalGlyph;
    private final int topLeftGlyph;
    private final int topRightGlyph;
    private final int bottomLeftGlyph;
    private final int bottomRightGlyph;

    BoxStyle(int horizontalGlyph, int verticalGlyph, int topLeftGlyph, int topRightGlyph,
            int bottomLeftGlyph, int bottomRightGlyph) {
        this.horizontalGlyph = horizontalGlyph;
        this.verticalGlyph = verticalGlyph;
        this.topLeftGlyph = topLeftGlyph;
        this.topRightGlyph = topRightGlyph;
        this.bottomLeftGlyph = bottomLeftGlyph;
        this.bottomRightGlyph = bottomRightGlyph;
    }

    int horizontalGlyph() {
        return horizontalGlyph;
    }

    int verticalGlyph() {
        return verticalGlyph;
    }

    int topLeftGlyph() {
        return topLeftGlyph;
    }

    int topRightGlyph() {
        return topRightGlyph;
    }

    int bottomLeftGlyph() {
        return bottomLeftGlyph;
    }

    int bottomRightGlyph() {
        return bottomRightGlyph;
    }
}
