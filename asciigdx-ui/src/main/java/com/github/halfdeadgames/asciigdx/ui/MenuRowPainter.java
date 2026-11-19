package com.github.halfdeadgames.asciigdx.ui;

/** Draws one row of a menu: a filled background with a line of text after the padding. */
final class MenuRowPainter {

    private MenuRowPainter() {
    }

    /**
     * Paints a row. The glyph at {@code triggerCell} (a cell index in the text, or
     * {@link MenuTrigger#NO_CELL}) is drawn in {@code triggerRgba} instead of the look's color.
     */
    static void paint(UiSurface target, LabelStyle look, String text, int horizontalPadding,
            int triggerCell, int triggerRgba, int left, int top, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        target.eraseGlyphs(left, top, width, height);
        target.fillBackground(left, top, width, height, look.getBackgroundRgba());
        int index = 0;
        int cellOffset = horizontalPadding;
        int textCell = 0;
        while (index < text.length() && cellOffset < width
                && left + cellOffset < target.getColumns()) {
            int codePoint = text.codePointAt(index);
            int foreground = textCell == triggerCell ? triggerRgba : look.getForegroundRgba();
            target.writeGlyph(look.getFont(), left + cellOffset, top, codePoint, foreground);
            index += Character.charCount(codePoint);
            cellOffset++;
            textCell++;
        }
    }
}
