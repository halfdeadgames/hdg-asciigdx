package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.FontDiscovery.FontChoice;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A menu list: the selectable items, the lines to show, and the selection. There are two menus: the
 * list of fonts, and the actions for one font (paint each size, upscale to the next size).
 */
final class MenuModel {

    enum Kind {
        /** Opens the action menu of a font. */
        FONT,
        PAINT,
        UPSCALE
    }

    /**
     * A selectable entry. {@code size} is set for paint entries and {@code upscale} for upscale entries.
     */
    record Item(Kind kind, String label, String folderName, int size, UpscalePlan.Choice upscale) {
    }

    /**
     * One displayed line; {@code itemIndex} is -1 for headings and notes that cannot be selected.
     * Dimmed lines are shown greyed out.
     */
    record Line(String text, int itemIndex, int indent, boolean dimmed) {
    }

    private static final int MAX_NUMBERED = 9;
    private static final String NO_SOURCE_NOTE = " (NO EVEN SOURCE)";

    private final List<Item> items = new ArrayList<>();
    private final List<Line> lines = new ArrayList<>();
    private int selected;

    private MenuModel() {
    }

    /** The font list: one item per font folder, in the order the folders appear in {@code fonts}. */
    static MenuModel fontList(List<FontChoice> fonts) {
        MenuModel model = new MenuModel();
        List<String> folders = new ArrayList<>();
        for (FontChoice font : fonts) {
            if (!folders.contains(font.folderName())) {
                folders.add(font.folderName());
            }
        }
        model.lines.add(new Line("FONTS", -1, 0, false));
        if (folders.isEmpty()) {
            model.lines.add(new Line("NO FONTS FOUND", -1, 1, true));
        }
        for (String folder : folders) {
            model.addItem(Kind.FONT, folder.toUpperCase(Locale.ROOT), folder, 0, null, 1);
        }
        return model;
    }

    /** The actions of one font: paint every existing size, then the upscale to the next size. */
    static MenuModel fontActions(String folderName, List<FontChoice> fonts) {
        MenuModel model = new MenuModel();
        List<FontChoice> sizes = new ArrayList<>();
        for (FontChoice font : fonts) {
            if (font.folderName().equals(folderName)) {
                sizes.add(font);
            }
        }
        for (FontChoice font : sizes) {
            model.addItem(Kind.PAINT, "PAINT " + font.size(), folderName, font.size(), null, 0);
        }
        if (!sizes.isEmpty()) {
            UpscalePlan.Choice choice = UpscalePlan.plan(sizes).get(0);
            String text = "UPSCALE " + choice.largest() + " -> " + choice.target();
            if (choice.hasSource()) {
                model.addItem(Kind.UPSCALE, text, folderName, choice.target(), choice, 0);
            } else {
                model.lines.add(new Line("  " + text + NO_SOURCE_NOTE, -1, 0, true));
            }
        } else {
            model.lines.add(new Line("NO SIZE FILES FOUND", -1, 0, true));
        }
        return model;
    }

    private void addItem(Kind kind, String label, String folderName, int size, UpscalePlan.Choice upscale, int indent) {
        int index = items.size();
        String number = index < MAX_NUMBERED ? (index + 1) + " " : "  ";
        items.add(new Item(kind, number + label, folderName, size, upscale));
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
