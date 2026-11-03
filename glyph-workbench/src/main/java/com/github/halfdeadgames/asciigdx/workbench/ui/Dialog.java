package com.github.halfdeadgames.asciigdx.workbench.ui;

import com.badlogic.gdx.graphics.Color;
import com.github.halfdeadgames.asciigdx.workbench.Painter;
import java.util.ArrayList;
import java.util.List;

/** A centred, focused panel over a dimmed screen, with a shadow and keycap buttons. */
public final class Dialog {

    public enum Tone { OK, WARN, DIM }

    public record Button(String key, String label, Tone tone) {

        /** Cells the button takes: " KEY ", a gap, and the label. */
        int width() {
            return key.length() + 3 + label.length();
        }
    }

    /** Where everything goes, in cells. */
    public record Layout(int x, int y, int width, int height, int textX, int textY, int buttonY, List<Integer> buttonXs) {

        /** Cells available to a text line between the frame paddings. */
        public int textWidth() {
            return width - 2 * (PADDING + 1);
        }
    }

    private static final int CELL = Painter.TEXT_SIZE;
    private static final int PADDING = 2;
    private static final int BUTTON_GAP = 3;
    private static final int MIN_BUTTON_GAP = 1;
    private static final int BUTTON_EDGE_PADDING = 1;
    private static final int SHADOW_MARGIN = 2;
    private static final int MIN_CAPPED_WIDTH = 12;
    private static final float DIM_ALPHA = 0.55f;
    private static final Color DIM_COLOR = new Color(0f, 0f, 0f, DIM_ALPHA);

    private Dialog() {
    }

    /** Sizes the dialog so text and buttons keep {@link #PADDING} cells from the frame, and centres it. */
    public static Layout layout(String title, List<String> lines, List<Button> buttons, int columns, int rows) {
        return layout(title, lines, buttons, 0, 0, columns, rows);
    }

    /**
     * Like {@link #layout(String, List, List, int, int)}, but centred in a cell rectangle and capped so the
     * frame and its one-cell shadow stay inside it.
     */
    public static Layout layout(String title, List<String> lines, List<Button> buttons,
                                int areaX, int areaY, int areaWidth, int areaHeight) {
        int content = Math.max(buttonsWidth(buttons, BUTTON_GAP), title.length() + 2);
        for (String line : lines) {
            content = Math.max(content, line.length());
        }
        int width = content + 2 * (PADDING + 1);
        width = Math.min(width, Math.max(MIN_CAPPED_WIDTH, areaWidth - SHADOW_MARGIN));
        // The button row keeps BUTTON_EDGE_PADDING cells inside the frame: the gap shrinks first, then the frame grows.
        int gap = BUTTON_GAP;
        while (gap > MIN_BUTTON_GAP && buttonsWidth(buttons, gap) + 2 * (1 + BUTTON_EDGE_PADDING) > width) {
            gap--;
        }
        width = Math.max(width, buttonsWidth(buttons, gap) + 2 * (1 + BUTTON_EDGE_PADDING));
        int buttonsWidth = buttonsWidth(buttons, gap);
        int height = 2 + 1 + lines.size() + 1 + 1 + 1;
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

    private static int buttonsWidth(List<Button> buttons, int gap) {
        int total = 0;
        for (Button button : buttons) {
            total += button.width() + (total == 0 ? 0 : gap);
        }
        return total;
    }

    public static void draw(Painter painter, int columns, int rows, String title, List<String> lines, List<Button> buttons) {
        draw(painter, columns, rows, title, lines, buttons, 0, 0, columns, rows);
    }

    /** Dims the whole screen but places the dialog and its shadow inside the given cell rectangle. */
    public static void draw(Painter painter, int columns, int rows, String title, List<String> lines,
                            List<Button> buttons, int areaX, int areaY, int areaWidth, int areaHeight) {
        painter.fill(DIM_COLOR, 0, 0, columns * CELL, rows * CELL);
        Layout layout = layout(title, lines, buttons, areaX, areaY, areaWidth, areaHeight);
        for (int i = 1; i <= layout.height(); i++) {
            painter.text("▓", (layout.x() + layout.width()) * CELL, (layout.y() + i) * CELL, UiPalette.SHADOW);
        }
        for (int i = 1; i <= layout.width(); i++) {
            painter.text("▓", (layout.x() + i) * CELL, (layout.y() + layout.height()) * CELL, UiPalette.SHADOW);
        }
        Panel.draw(painter, layout.x(), layout.y(), layout.width(), layout.height(), title, true);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            painter.text(line.length() > layout.textWidth() ? line.substring(0, layout.textWidth()) : line,
                layout.textX() * CELL, (layout.textY() + i) * CELL, UiPalette.TEXT);
        }
        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            int capWidth = button.key().length() + 2;
            int bx = layout.buttonXs().get(i);
            painter.fill(UiPalette.KEYBG, bx * CELL, layout.buttonY() * CELL, capWidth * CELL, CELL);
            painter.text(" " + button.key() + " ", bx * CELL, layout.buttonY() * CELL, UiPalette.TEXT);
            Color label = switch (button.tone()) {
                case OK -> UiPalette.OK;
                case WARN -> UiPalette.WARN;
                case DIM -> UiPalette.DIM;
            };
            painter.text(button.label(), (bx + capWidth + 1) * CELL, layout.buttonY() * CELL, label);
        }
    }
}
