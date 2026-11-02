package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.FontDiscovery.FontChoice;
import java.util.ArrayList;
import java.util.List;

/** The launch menu: the selectable items, the lines to show, and the selection. */
final class MenuModel {

    enum Kind {
        REVIEW,
        PAINT
    }

    /** A selectable entry; {@code font} is null for the review entry. */
    record Item(Kind kind, String label, FontChoice font) {
    }

    /** One displayed line; {@code itemIndex} is -1 for headings and notes that cannot be selected. */
    record Line(String text, int itemIndex, int indent) {
    }

    private static final int MAX_NUMBERED = 9;

    private final List<Item> items = new ArrayList<>();
    private final List<Line> lines = new ArrayList<>();
    private int selected;

    MenuModel(List<FontChoice> fonts) {
        addItem(Kind.REVIEW, "REVIEW UPSCALES (DUNGEON FONT, 16X16 CANDIDATES)", null, 0);
        lines.add(new Line("PAINT FONT", -1, 0));
        if (fonts.isEmpty()) {
            lines.add(new Line("NO FONTS FOUND", -1, 1));
        }
        for (FontChoice font : fonts) {
            addItem(Kind.PAINT, font.label(), font, 1);
        }
    }

    private void addItem(Kind kind, String label, FontChoice font, int indent) {
        int index = items.size();
        String number = index < MAX_NUMBERED ? (index + 1) + " " : "  ";
        items.add(new Item(kind, number + label, font));
        lines.add(new Line(number + label, index, indent));
    }

    int itemCount() {
        return items.size();
    }

    Item item(int index) {
        return items.get(index);
    }

    List<Line> lines() {
        return lines;
    }

    int selectedIndex() {
        return selected;
    }

    Item selectedItem() {
        return items.get(selected);
    }

    /** Moves the selection, wrapping around at both ends. */
    void move(int steps) {
        selected = Math.floorMod(selected + steps, items.size());
    }

    /** Selects an item; out-of-range indexes are ignored. */
    void select(int index) {
        if (index >= 0 && index < items.size()) {
            selected = index;
        }
    }

    /** The item for a number key (1 is the first item), or -1 when the number has no item. */
    int itemForNumber(int number) {
        return number >= 1 && number <= Math.min(MAX_NUMBERED, items.size()) ? number - 1 : -1;
    }
}
