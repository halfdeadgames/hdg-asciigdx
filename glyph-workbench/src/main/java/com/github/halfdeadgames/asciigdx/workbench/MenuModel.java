package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.FontDiscovery.FontChoice;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A menu list: the selectable items, the lines to show, and the selection. There are two menus: the
 * list of fonts and the actions for one font (paint each size, add a new size).
 */
final class MenuModel {

    enum Kind {
        /** Opens the action menu of a font. */
        FONT,
        PAINT,
        /** Opens the New size dialog. */
        NEW_SIZE,
        /** Opens the dialog that makes a font folder; the last entry of the font list. */
        NEW_FONT
    }

    /** A selectable entry. {@code size} is set for paint entries, and null for the others. */
    record Item(Kind kind, String label, String folderName, CellSize size) {
    }

    /**
     * One displayed line; {@code itemIndex} is -1 for headings and notes that cannot be selected.
     * Dimmed lines are shown grayed out.
     */
    record Line(String text, int itemIndex, int indent, boolean dimmed) {
    }

    private static final int MAX_NUMBERED = 9;

    private final List<Item> items = new ArrayList<>();
    private final List<Line> lines = new ArrayList<>();
    private int selected;

    private MenuModel() {
    }

    /**
     * The font list: one item per font folder, in the order the folders appear in {@code fonts}, then the
     * new font entry, which is there even when there are no fonts.
     */
    static MenuModel fontList(List<FontChoice> fonts) {
        MenuModel model = new MenuModel();
        List<String> folders = new ArrayList<>();
        for (FontChoice font : fonts) {
            if (!folders.contains(font.folderName())) {
                folders.add(font.folderName());
            }
        }
        if (folders.isEmpty()) {
            model.lines.add(new Line("NO FONTS FOUND", -1, 1, true));
        }
        for (String folder : folders) {
            model.addItem(Kind.FONT, folder.toUpperCase(Locale.ROOT), folder, null, 1);
        }
        model.addItem(Kind.NEW_FONT, "NEW FONT", "", null, 1);
        return model;
    }

    /** The actions of one font: paint every existing size, then the new size entry. */
    static MenuModel fontActions(String folderName, List<FontChoice> fonts) {
        MenuModel model = new MenuModel();
        List<FontChoice> sizes = new ArrayList<>();
        for (FontChoice font : fonts) {
            if (font.folderName().equals(folderName)) {
                sizes.add(font);
            }
        }
        for (FontChoice font : sizes) {
            model.addItem(Kind.PAINT, "PAINT " + font.size().toString().toUpperCase(Locale.ROOT), folderName,
                font.size(), 0);
        }
        if (!sizes.isEmpty()) {
            model.addItem(Kind.NEW_SIZE, "NEW SIZE", folderName, null, 0);
        } else {
            model.lines.add(new Line("NO SIZE FILES FOUND", -1, 0, true));
        }
        return model;
    }

    private void addItem(Kind kind, String label, String folderName, CellSize size, int indent) {
        int index = items.size();
        String number = index < MAX_NUMBERED ? (index + 1) + " " : "  ";
        items.add(new Item(kind, number + label, folderName, size));
        lines.add(new Line(number + label, index, indent, false));
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

    /** Index of the first item of a kind, or -1 (which {@link #select} ignores). */
    int indexOfKind(Kind kind) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).kind() == kind) {
                return i;
            }
        }
        return -1;
    }

    /** The selected item, or null when the menu has none. */
    Item selectedItem() {
        return items.isEmpty() ? null : items.get(selected);
    }

    /** Moves the selection, wrapping around at both ends; does nothing without items. */
    void move(int steps) {
        if (!items.isEmpty()) {
            selected = Math.floorMod(selected + steps, items.size());
        }
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
