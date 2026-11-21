package com.github.halfdeadgames.asciigdx.ui;

/** Draws one row of a menu: a filled background with a line of text after the padding. */
final class MenuRowPainter {

    private static final int UNDERSCORE_CODE_POINT = '_';

    private MenuRowPainter() {
    }

    /**
     * Paints a row. The text is drawn in the text font, one text column per code point, starting
     * after the padding. The glyph at {@code triggerCodePointIndex} (an index among the code
     * points of the text, or {@link MenuTrigger#NO_CELL}) is drawn in the style's trigger color
     * when it has one, and
     * underlined in the overlay, on the same row and at the same text column, when the style has a
     * trigger underline. The underline uses the trigger color, or else the foreground of
     * {@code look}. The size and the padding are in UI cells.
     */
    static void paint(UiSurface target, MenuBarStyle style, LabelStyle look, String text,
            int horizontalPadding, int triggerCodePointIndex, int left, int top, int width,
            int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        target.eraseGlyphs(left, top, width, height);
        target.fillBackground(left, top, width, height, look.getBackgroundRgba());
        int leftTextColumn = UiSurface.TEXT_COLUMNS_PER_CELL * left;
        int textColumnLimit = UiSurface.TEXT_COLUMNS_PER_CELL * width;
        int index = 0;
        int textOffset = UiSurface.TEXT_COLUMNS_PER_CELL * horizontalPadding;
        int textIndex = 0;
        while (index < text.length() && textOffset < textColumnLimit
                && leftTextColumn + textOffset < target.getTextColumns()) {
            int codePoint = text.codePointAt(index);
            boolean isTrigger = textIndex == triggerCodePointIndex;
            int foreground = isTrigger && style.hasTriggerRgba() ? style.getTriggerRgba()
                    : look.getForegroundRgba();
            target.writeTextGlyph(leftTextColumn + textOffset, top, codePoint, foreground);
            if (isTrigger && style.isTriggerUnderline()) {
                target.writeOverlayGlyph(leftTextColumn + textOffset, top,
                        UNDERSCORE_CODE_POINT, foreground);
            }
            index += Character.charCount(codePoint);
            textOffset++;
            textIndex++;
        }
    }
}
