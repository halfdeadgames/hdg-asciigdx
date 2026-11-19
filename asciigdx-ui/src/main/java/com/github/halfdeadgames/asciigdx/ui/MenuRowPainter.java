package com.github.halfdeadgames.asciigdx.ui;

/** Draws one row of a menu: a filled background with a line of text after the padding. */
final class MenuRowPainter {

    private static final int UNDERSCORE_CODE_POINT = '_';

    private MenuRowPainter() {
    }

    /**
     * Paints a row. The glyph at {@code triggerCell} (a cell index in the text, or
     * {@link MenuTrigger#NO_CELL}) is drawn in the style's trigger color when it has one, and
     * underlined in the overlay when the style has a trigger underline. The underline uses the
     * trigger color, or else the foreground of {@code look}.
     */
    static void paint(UiSurface target, MenuBarStyle style, LabelStyle look, String text,
            int horizontalPadding, int triggerCell, int left, int top, int width, int height) {
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
            boolean isTrigger = textCell == triggerCell;
            int foreground = isTrigger && style.hasTriggerRgba() ? style.getTriggerRgba()
                    : look.getForegroundRgba();
            target.writeGlyph(look.getFont(), left + cellOffset, top, codePoint, foreground);
            if (isTrigger && style.isTriggerUnderline()) {
                target.writeOverlayGlyph(left + cellOffset, underlineRow(look.getFont(), top),
                        UNDERSCORE_CODE_POINT, foreground);
            }
            index += Character.charCount(codePoint);
            cellOffset++;
            textCell++;
        }
    }

    private static int underlineRow(UiFont font, int top) {
        return font == UiFont.TALL ? 2 * Math.floorDiv(top, 2) + 1 : top;
    }
}
