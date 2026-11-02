package com.github.halfdeadgames.asciigdx.workbench.ui;

import com.github.halfdeadgames.asciigdx.workbench.Painter;
import java.util.ArrayList;
import java.util.List;

/** The bottom strip of keycaps: " KEY " on a lighter background, then its label. */
public final class StatusBar {

    public record Key(String key, String label) {
    }

    /** One keycap and its label, with their cell columns. */
    public record Cap(String capText, int capX, String label, int labelX) {
    }

    private static final int CELL = Painter.TEXT_SIZE;

    private StatusBar() {
    }

    public static List<Cap> layout(List<Key> keys) {
        List<Cap> caps = new ArrayList<>();
        int x = 1;
        for (Key key : keys) {
            String capText = " " + key.key() + " ";
            int labelX = x + capText.length() + 1;
            caps.add(new Cap(capText, x, key.label(), labelX));
            x = labelX + key.label().length() + 2;
        }
        return caps;
    }

    public static void draw(Painter painter, int columns, int row, List<Key> keys) {
        painter.fill(UiPalette.PANEL, 0, row * CELL, columns * CELL, CELL);
        for (Cap cap : layout(keys)) {
            painter.fill(UiPalette.KEYBG, cap.capX() * CELL, row * CELL, cap.capText().length() * CELL, CELL);
            painter.text(cap.capText(), cap.capX() * CELL, row * CELL, UiPalette.TEXT);
            painter.text(cap.label(), cap.labelX() * CELL, row * CELL, UiPalette.DIM);
        }
    }
}
