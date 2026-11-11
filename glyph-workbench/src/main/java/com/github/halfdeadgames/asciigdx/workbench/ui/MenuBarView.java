package com.github.halfdeadgames.asciigdx.workbench.ui;

import com.badlogic.gdx.graphics.Color;
import java.util.List;
import java.util.function.Predicate;

/** Draws a {@link MenuBar}: the titles on one row and the open menu's drop-down below it. */
public final class MenuBarView {

    private MenuBarView() {
    }

    /** Paints the whole row first, so the bar stays clear when drawn over a dimmed screen. */
    public static <A> void drawBar(Painter painter, MenuBar<A> bar, int columns, int row, Predicate<A> availability) {
        int cellPixels = painter.cellPixels();
        painter.fill(UiPalette.MENU, 0, row * cellPixels, columns * cellPixels, cellPixels);
        for (int i = 0; i < bar.menuCount(); i++) {
            String title = bar.menu(i).title();
            int x = bar.titleColumn(i);
            boolean locked = bar.isLocked(i, availability);
            boolean open = i == bar.openMenu();
            if (open) {
                painter.fill(UiPalette.FOCUS, (x - 1) * cellPixels, row * cellPixels,
                    (title.length() + 2) * cellPixels, cellPixels);
                painter.text(title, x * cellPixels, row * cellPixels, UiPalette.BG);
                continue;
            }
            if (i == bar.hoveredTitle()) {
                painter.fill(UiPalette.FRAME, (x - 1) * cellPixels, row * cellPixels,
                    (title.length() + 2) * cellPixels, cellPixels);
            }
            if (i == bar.inspectedTitle()) {
                painter.fill(UiPalette.MENU_DISABLED, (x - 1) * cellPixels, row * cellPixels,
                    (title.length() + 2) * cellPixels, cellPixels);
                painter.text(title, x * cellPixels, row * cellPixels, UiPalette.MENU_DISABLED_HOVER_TEXT);
                continue;
            }
            painter.text(title, x * cellPixels, row * cellPixels, locked ? UiPalette.MENU_DISABLED : UiPalette.TEXT);
            if (!locked) {
                painter.text(title.substring(0, 1), x * cellPixels, row * cellPixels, UiPalette.ACCENT);
            }
        }
    }

    /** Draws the open menu's panel just below the bar row; does nothing when no menu is open. */
    public static <A> void drawDropDown(Painter painter, MenuBar<A> bar, int columns, int barRow,
                                        Predicate<A> availability) {
        int menu = bar.openMenu();
        if (menu < 0) {
            return;
        }
        int cellPixels = painter.cellPixels();
        List<MenuBar.Item<A>> items = bar.menu(menu).items();
        int x = bar.dropDownColumn(menu, columns);
        int width = bar.dropDownWidth(menu);
        int top = barRow + 1;
        Panel.draw(painter, new CellRect(x, top, width, items.size() + 2), "", false);
        painter.fill(UiPalette.MENU, (x + 1) * cellPixels, (top + 1) * cellPixels, (width - 2) * cellPixels,
            items.size() * cellPixels);
        for (int i = 0; i < items.size(); i++) {
            MenuBar.Item<A> item = items.get(i);
            int y = (top + 1 + i) * cellPixels;
            boolean available = bar.isItemAvailable(menu, i, availability);
            boolean highlighted = i == bar.highlightedItem();
            if (highlighted) {
                painter.fill(UiPalette.FRAME, (x + 1) * cellPixels, y, (width - 2) * cellPixels, cellPixels);
            }
            Color color = !available ? UiPalette.MENU_DISABLED : highlighted ? UiPalette.ACCENT : UiPalette.TEXT;
            if (i == bar.inspectedItem()) {
                painter.fill(UiPalette.MENU_DISABLED, (x + 1) * cellPixels, y, (width - 2) * cellPixels, cellPixels);
                color = UiPalette.MENU_DISABLED_HOVER_TEXT;
            }
            painter.text(item.label(), (x + 1) * cellPixels, y, color);
            painter.text(item.shortcut(), (x + width - 1 - item.shortcut().length()) * cellPixels, y, color);
        }
    }
}
