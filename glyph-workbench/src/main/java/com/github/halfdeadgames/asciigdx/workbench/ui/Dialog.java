package com.github.halfdeadgames.asciigdx.workbench.ui;

import com.badlogic.gdx.graphics.Color;
import java.util.ArrayList;
import java.util.List;

/** A centered, focused panel over a dimmed screen, with a shadow and keycap buttons. */
public final class Dialog {

    public enum Tone {OK, WARN, DIM}

    public record Button(String key, String label, Tone tone) {

        /** Cells the button takes: " KEY ", a gap, and the label. */
        int width() {
            return TextCells.cellCount(key) + 3 + TextCells.cellCount(label);
        }
    }

    /** Where everything goes, in cells. */
    public record Layout(int x, int y, int width, int height, int textX, int textY, int buttonY,
                         List<Integer> buttonXs) {

        /** Cells available to a text line between the frame paddings. */
        public int textWidth() {
            return width - 2 * (PADDING + 1);
        }

        /** Whether the cell lies within the dialog frame. */
        public boolean containsCell(int column, int row) {
            return column >= x && column < x + width && row >= y && row < y + height;
        }

        /** Whether the pixel lies within the dialog frame. */
        public boolean containsPixel(int pixelX, int pixelY, int cellPixels) {
            return containsCell(Math.floorDiv(pixelX, cellPixels),
                    Math.floorDiv(pixelY, cellPixels));
        }
    }

    // Frame, title gap, blank rows around the text, and the button row.
    private static final int NON_LINE_CELLS = 6;
    private static final int SHADOW_ROWS = 1;
    private static final int PADDING = 2;
    private static final int BUTTON_GAP = 3;
    private static final int MIN_BUTTON_GAP = 1;
    private static final int BUTTON_EDGE_PADDING = 1;
    private static final int SHADOW_MARGIN = 2;
    private static final int MIN_CAPPED_WIDTH = 12;
    private static final float DIM_ALPHA = 0.55f;
    private static final Color DIM_COLOR = new Color(0f, 0f, 0f, DIM_ALPHA);
    private static final Color INPUT_BACKGROUND = new Color(0x000000FF);
    private static final Color INPUT_TEXT = new Color(0xFFFFFFFF);
    private static final int INPUT_INSET_DIVISOR = 4;
    private static final int MIN_INPUT_INSET = 2;

    private Dialog() {
    }

    /**
     * Sizes the dialog so text and buttons keep {@link #PADDING} cells from the frame, and centers
     * it in the area, capped so the frame and its one-cell shadow stay inside it.
     */
    public static Layout layout(DialogContent dialog, CellRect area) {
        String title = dialog.title();
        List<String> lines = dialog.lines();
        List<Button> buttons = dialog.buttons();
        int areaX = area.x();
        int areaY = area.y();
        int areaWidth = area.width();
        int areaHeight = area.height();
        int content = Math.max(buttonsWidth(buttons, BUTTON_GAP), TextCells.cellCount(title) + 2);
        for (String line : lines) {
            content = Math.max(content, TextCells.cellCount(line));
        }
        int width = content + 2 * (PADDING + 1);
        width = Math.min(width, Math.max(MIN_CAPPED_WIDTH, areaWidth - SHADOW_MARGIN));
        // The button row keeps BUTTON_EDGE_PADDING cells inside the frame: the gap shrinks first,
        // then the frame grows.
        int gap = BUTTON_GAP;
        while (gap > MIN_BUTTON_GAP
                && buttonsWidth(buttons, gap) + 2 * (1 + BUTTON_EDGE_PADDING) > width) {
            gap--;
        }
        width = Math.max(width, buttonsWidth(buttons, gap) + 2 * (1 + BUTTON_EDGE_PADDING));
        int buttonsWidth = buttonsWidth(buttons, gap);
        int height = NON_LINE_CELLS + lines.size();
        int x = Math.max(areaX, areaX + (areaWidth - width) / 2);
        int y = Math.max(areaY, areaY + (areaHeight - height) / 2);
        int textX = x + PADDING + 1;
        int buttonY = y + height - 3;
        List<Integer> buttonXs = new ArrayList<>();
        int cursor = x + (width - buttonsWidth) / 2;
        for (Button button : buttons) {
            buttonXs.add(cursor);
            cursor += button.width() + gap;
        }
        return new Layout(x, y, width, height, textX, y + 2, buttonY, buttonXs);
    }

    /**
     * Most text lines a dialog can have and still fit, with its shadow, in an area this many cells
     * tall.
     */
    public static int maxLines(int areaHeight) {
        return areaHeight - NON_LINE_CELLS - SHADOW_ROWS;
    }

    private static int buttonsWidth(List<Button> buttons, int gap) {
        int total = 0;
        for (Button button : buttons) {
            total += button.width() + (total == 0 ? 0 : gap);
        }
        return total;
    }

    /** Dims the screen and centers the dialog in it. */
    public static void draw(Painter painter, CellRect screen, DialogContent content) {
        draw(painter, screen, content, screen);
    }

    /**
     * Dims the whole screen but places the dialog and its shadow inside the given cell rectangle.
     * Draws the content's {@code inputLine} as a text field: a black box {@code inputCells} wide
     * behind white text.
     */
    public static void draw(Painter painter, CellRect screen, DialogContent content,
            CellRect area) {
        String title = content.title();
        List<String> lines = content.lines();
        List<Button> buttons = content.buttons();
        int inputLine = content.inputLine();
        int inputCells = content.inputCells();
        int cellPixels = painter.cellPixels();
        painter.fill(DIM_COLOR, 0, 0, screen.width() * cellPixels, screen.height() * cellPixels);
        Layout layout = layout(content, area);
        drawShadow(painter, layout);
        Panel.draw(painter, new CellRect(layout.x(), layout.y(), layout.width(), layout.height()),
                title, true);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            boolean input = i == inputLine;
            if (input) {
                int inset = Math.max(MIN_INPUT_INSET, cellPixels / INPUT_INSET_DIVISOR);
                painter.fill(INPUT_BACKGROUND, layout.textX() * cellPixels - inset,
                        (layout.textY() + i) * cellPixels - inset,
                        Math.min(inputCells, layout.textWidth()) * cellPixels + 2 * inset,
                        cellPixels + 2 * inset);
            }
            painter.text(TextCells.prefix(line, layout.textWidth()),
                    layout.textX() * cellPixels, (layout.textY() + i) * cellPixels,
                    input ? INPUT_TEXT : UiPalette.TEXT);
        }
        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            int capWidth = TextCells.cellCount(button.key()) + 2;
            int bx = layout.buttonXs().get(i);
            painter.fill(UiPalette.KEYBG, bx * cellPixels, layout.buttonY() * cellPixels,
                    capWidth * cellPixels,
                    cellPixels);
            painter.text(" " + button.key() + " ", bx * cellPixels, layout.buttonY() * cellPixels,
                    UiPalette.TEXT);
            Color label = switch (button.tone()) {
                case OK -> UiPalette.OK;
                case WARN -> UiPalette.WARN;
                case DIM -> UiPalette.DIM;
            };
            painter.text(button.label(), (bx + capWidth + 1) * cellPixels,
                    layout.buttonY() * cellPixels, label);
        }
    }

    private static void drawShadow(Painter painter, Layout layout) {
        int cellPixels = painter.cellPixels();
        for (int i = 1; i <= layout.height(); i++) {
            painter.text("▓", (layout.x() + layout.width()) * cellPixels,
                    (layout.y() + i) * cellPixels,
                    UiPalette.SHADOW);
        }
        for (int i = 1; i <= layout.width(); i++) {
            painter.text("▓", (layout.x() + i) * cellPixels,
                    (layout.y() + layout.height()) * cellPixels,
                    UiPalette.SHADOW);
        }
    }
}
