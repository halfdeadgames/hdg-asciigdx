package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontRules;
import java.util.List;

/** Draws the font's motto and rules inside a framed, scrollable page: the glyph's group, Core, then the rest. */
final class RulesPanel {

    static final int MAX_CELLS_WIDE = 40;
    private static final int MIN_CELLS_WIDE = 8;
    private static final int SUB_INDENT = 2;
    private static final int PAGE_OVERLAP = 1;

    private record Line(String text, int indent, Color color) {
    }

    private final Painter painter;
    private final Color frameColor;
    private final Color fillColor;
    private final Color textColor;
    private final Color headingColor;
    private final Array<Line> lines = new Array<>();
    private FontRules rules;
    private String missingMessage = "";
    private String shownGroupName;
    private int builtCellsWide = -1;
    private int textChars;
    private int room = 1;
    private int offset;

    RulesPanel(Painter painter, Color frameColor, Color fillColor, Color textColor, Color headingColor) {
        this.painter = painter;
        this.frameColor = frameColor;
        this.fillColor = fillColor;
        this.textColor = textColor;
        this.headingColor = headingColor;
    }

    /** Width in text cells for a window width: comfortable reading, never wider than the window. */
    static int cellsWideFor(int worldWidth) {
        return Math.max(MIN_CELLS_WIDE, Math.min(MAX_CELLS_WIDE, worldWidth / Painter.TEXT_SIZE));
    }

    void setRules(FontRules rules) {
        this.rules = rules;
        this.missingMessage = "";
        shownGroupName = null;
    }

    void setMissing(String message) {
        rules = null;
        missingMessage = message;
        shownGroupName = null;
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
        return Math.max(0, lines.size - room);
    }

    /** Fills the page opaque, then draws the frame and the visible lines with arrows where more text hides. */
    void draw(int x, int top, int cellsWide, int cellsHigh, int pixelHeight, int codePoint) {
        String groupName = FontRules.groupNameFor(codePoint);
        if (!groupName.equals(shownGroupName) || cellsWide != builtCellsWide) {
            rebuild(groupName, codePoint, cellsWide);
        }
        painter.fill(fillColor, x, top, cellsWide * Painter.TEXT_SIZE, pixelHeight);
        painter.frame(frameColor, x, top, cellsWide, cellsHigh);
        room = Math.max(1, cellsHigh - 2);
        offset = Math.max(0, Math.min(maxOffset(), offset));
        int textLeft = x + Painter.TEXT_SIZE;
        int textTop = top + Painter.TEXT_SIZE;
        int end = Math.min(lines.size, offset + room);
        for (int i = offset; i < end; i++) {
            Line line = lines.get(i);
            painter.text(line.text, textLeft + line.indent * Painter.TEXT_SIZE,
                textTop + (i - offset) * Painter.TEXT_SIZE, line.color);
        }
        int arrowLeft = textLeft + textChars * Painter.TEXT_SIZE;
        if (offset > 0) {
            painter.text("▲", arrowLeft, textTop, headingColor);
        }
        if (offset < maxOffset()) {
            painter.text("▼", arrowLeft, textTop + (room - 1) * Painter.TEXT_SIZE, headingColor);
        }
    }

    private void rebuild(String groupName, int codePoint, int cellsWide) {
        shownGroupName = groupName;
        builtCellsWide = cellsWide;
        textChars = Math.max(1, cellsWide - 3);
        lines.clear();
        if (rules == null) {
            addWrapped(missingMessage);
            return;
        }
        if (!rules.motto().isEmpty()) {
            addWrapped(rules.motto());
        }
        FontRules.Group first = rules.groupFor(codePoint);
        if (first != null) {
            addGroup(first);
        }
        FontRules.Group core = rules.group(FontRules.CORE);
        if (core != null && core != first) {
            addGroup(core);
        }
        for (FontRules.Group group : rules.groups()) {
            if (group != first && group != core) {
                addGroup(group);
            }
        }
        offset = 0;
    }

    private void addGroup(FontRules.Group group) {
        if (lines.size > 0) {
            lines.add(new Line("", 0, textColor));
        }
        lines.add(new Line(group.name().toUpperCase(), 0, headingColor));
        for (FontRules.Bullet bullet : group.bullets()) {
            int indent = bullet.sub() ? SUB_INDENT : 0;
            int textWidth = Math.max(1, textChars - indent - SUB_INDENT);
            List<String> wrapped = NoteText.wrap(bullet.text(), textWidth);
            for (int i = 0; i < wrapped.size(); i++) {
                String piece = wrapped.get(i);
                if (piece.isEmpty() && i > 0) {
                    continue;
                }
                lines.add(new Line(i == 0 ? "- " + piece : "  " + piece, indent, textColor));
            }
        }
    }

    private void addWrapped(String text) {
        for (String piece : NoteText.wrap(text, textChars)) {
            if (!piece.isEmpty()) {
                lines.add(new Line(piece, 0, textColor));
            }
        }
    }
}
