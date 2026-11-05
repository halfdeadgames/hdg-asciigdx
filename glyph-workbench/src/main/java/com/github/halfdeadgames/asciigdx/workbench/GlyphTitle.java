package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.ui.Panel;

/** The grid panel title: the code point followed by the glyph name, which scrolls when it does not fit. */
final class GlyphTitle {

    private static final int PANEL_BORDER_CHARS = 6;

    private final StringBuilder line = new StringBuilder();
    private int shownCodePoint = -1;
    private float shownSeconds;

    void draw(Painter painter, ReviewLayout.Rect panel, int codePoint, float deltaSeconds) {
        advance(codePoint, deltaSeconds);
        String title = text(codePoint, panel.width()).toString();
        Panel.draw(painter, panel.toCellRect(), title, true);
    }

    void advance(int codePoint, float deltaSeconds) {
        if (codePoint != shownCodePoint) {
            shownCodePoint = codePoint;
            shownSeconds = 0f;
        }
        shownSeconds += deltaSeconds;
    }

    /** The title for the current timer; valid until the next call. */
    CharSequence text(int codePoint, int panelWidth) {
        line.setLength(0);
        line.append("U+");
        GlyphLabel.appendHex(line, codePoint);
        line.append("  ");
        int nameChars = Math.max(1, panelWidth - PANEL_BORDER_CHARS - line.length());
        line.append(BounceScroll.window(GlyphName.fullName(codePoint), nameChars, shownSeconds));
        return line;
    }
}
