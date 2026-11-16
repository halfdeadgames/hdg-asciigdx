package com.github.halfdeadgames.asciigdx.workbench.rules;

import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Draws the rules page and the font's notes inside a framed, scrollable page; rebuilds the lines
 * only on change.
 */
final class RulesPanel {

    private static final int MAX_CELLS_WIDE = 40;
    private static final int MIN_CELLS_WIDE = 8;
    private static final int PAGE_OVERLAP = 1;

    private final Painter painter;
    private final int frameColor;
    private final int fillColor;
    private final int textColor;
    private final int headingColor;
    private final int warningColor;
    private final int inactiveColor;
    private List<RulesPageLines.PageLine> lines = List.of();
    private RulesLoadResult rules = RulesLoadResult.missing(Path.of(""));
    private String notes;
    private Set<String> brokenRuleNames = Set.of();
    private boolean stale = true;
    private int shownCodePoint = -1;
    private int builtCellsWide = -1;
    private int textChars;
    private int room = 1;
    private int offset;

    RulesPanel(Painter painter, int frameColor, int fillColor, int textColor, int headingColor,
            int warningColor, int inactiveColor) {
        this.painter = painter;
        this.frameColor = frameColor;
        this.fillColor = fillColor;
        this.textColor = textColor;
        this.headingColor = headingColor;
        this.warningColor = warningColor;
        this.inactiveColor = inactiveColor;
    }

    /** Width in text cells for a window width: comfortable reading, never wider than the window. */
    static int cellsWideFor(int worldWidth, int cellPixels) {
        return Math.max(MIN_CELLS_WIDE, Math.min(MAX_CELLS_WIDE, worldWidth / cellPixels));
    }

    void show(RulesLoadResult rules, String notes) {
        this.rules = rules;
        this.notes = notes;
        stale = true;
    }

    void setBrokenRuleNames(Set<String> names) {
        if (names.equals(brokenRuleNames)) {
            return;
        }
        brokenRuleNames = names;
        stale = true;
    }

    void resetScroll() {
        offset = 0;
    }

    void scrollBy(int rowCount) {
        offset = Math.max(0, Math.min(maxOffset(), offset + rowCount));
    }

    void pageUp() {
        scrollBy(-Math.max(1, room - PAGE_OVERLAP));
    }

    void pageDown() {
        scrollBy(Math.max(1, room - PAGE_OVERLAP));
    }

    private int maxOffset() {
        return Math.max(0, lines.size() - room);
    }

    /**
     * Fills the page opaque, then draws the frame and the visible lines with arrows where more text
     * hides.
     */
    void draw(int x, int top, int cellsWide, int cellsHigh, int pixelHeight, int codePoint) {
        if (stale || codePoint != shownCodePoint || cellsWide != builtCellsWide) {
            rebuild(codePoint, cellsWide);
        }
        int cellPixels = painter.cellPixels();
        painter.fill(fillColor, x, top, cellsWide * cellPixels, pixelHeight);
        painter.frame(frameColor, x, top, cellsWide, cellsHigh);
        room = Math.max(1, cellsHigh - 2);
        offset = Math.max(0, Math.min(maxOffset(), offset));
        int textLeft = x + cellPixels;
        int textTop = top + cellPixels;
        int end = Math.min(lines.size(), offset + room);
        for (int i = offset; i < end; i++) {
            RulesPageLines.PageLine line = lines.get(i);
            painter.text(line.text(), textLeft,
                    textTop + (i - offset) * cellPixels, colorOf(line.role()));
        }
        int arrowLeft = textLeft + textChars * cellPixels;
        if (offset > 0) {
            painter.text("▲", arrowLeft, textTop, headingColor);
        }
        if (offset < maxOffset()) {
            painter.text("▼", arrowLeft, textTop + (room - 1) * cellPixels, headingColor);
        }
    }

    private void rebuild(int codePoint, int cellsWide) {
        shownCodePoint = codePoint;
        builtCellsWide = cellsWide;
        stale = false;
        textChars = Math.max(1, cellsWide - 3);
        lines = RulesPageLines.build(rules, notes, codePoint, brokenRuleNames, textChars);
    }

    private int colorOf(RulesPageLines.Role role) {
        return switch (role) {
            case TEXT -> textColor;
            case HEADING -> headingColor;
            case WARNING -> warningColor;
            case INACTIVE -> inactiveColor;
        };
    }
}
