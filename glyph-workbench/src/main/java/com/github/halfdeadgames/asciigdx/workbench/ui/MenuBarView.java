package com.github.halfdeadgames.asciigdx.workbench.ui;

import com.badlogic.gdx.graphics.Color;
import com.github.halfdeadgames.asciigdx.workbench.Painter;
import java.util.List;
import java.util.function.Predicate;

/** Draws a {@link MenuBar}: the titles on one row and the open menu's drop-down below it. */
public final class MenuBarView {

    private MenuBarView() {
    }

    /** Paints the whole row first, so the bar stays clear when drawn over a dimmed screen. */
    public static <A> void drawBar(Painter painter, MenuBar<A> bar, int columns, int row, Predicate<A> availability) {
        int cell = painter.cell();
        painter.fill(UiPalette.MENU, 0, row * cell, columns * cell, cell);
        for (int i = 0; i < bar.menuCount(); i++) {
            String title = bar.menu(i).title();
            int x = bar.titleColumn(i);
            boolean locked = bar.isLocked(i, availability);
            boolean open = i == bar.openMenu();
            if (open) {
                painter.fill(UiPalette.FOCUS, (x - 1) * cell, row * cell, (title.length() + 2) * cell, cell);
                painter.text(title, x * cell, row * cell, UiPalette.BG);
                continue;
            }
            if (i == bar.hoveredTitle()) {
                painter.fill(UiPalette.FRAME, (x - 1) * cell, row * cell, (title.length() + 2) * cell, cell);
            }
            if (i == bar.inspectedTitle()) {
                painter.fill(UiPalette.MENU_DISABLED, (x - 1) * cell, row * cell, (title.length() + 2) * cell, cell);
                painter.text(title, x * cell, row * cell, UiPalette.MENU_DISABLED_HOVER_TEXT);
                continue;
            }
            painter.text(title, x * cell, row * cell, locked ? UiPalette.MENU_DISABLED : UiPalette.TEXT);
            if (!locked) {
                painter.text(title.substring(0, 1), x * cell, row * cell, UiPalette.ACCENT);
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
        int cell = painter.cell();
        List<MenuBar.Item<A>> items = bar.menu(menu).items();
        int x = bar.dropDownColumn(menu, columns);
        int width = bar.dropDownWidth(menu);
        int top = barRow + 1;
        Panel.draw(painter, x, top, width, items.size() + 2, "", false);
        painter.fill(UiPalette.MENU, (x + 1) * cell, (top + 1) * cell, (width - 2) * cell, items.size() * cell);
        for (int i = 0; i < items.size(); i++) {
            MenuBar.Item<A> item = items.get(i);
            int y = (top + 1 + i) * cell;
            boolean available = bar.isItemAvailable(menu, i, availability);
            boolean highlighted = i == bar.highlightedItem();
            if (highlighted) {
                painter.fill(UiPalette.FRAME, (x + 1) * cell, y, (width - 2) * cell, cell);
            }
            Color color = !available ? UiPalette.MENU_DISABLED : highlighted ? UiPalette.ACCENT : UiPalette.TEXT;
            if (i == bar.inspectedItem()) {
                painter.fill(UiPalette.MENU_DISABLED, (x + 1) * cell, y, (width - 2) * cell, cell);
                color = UiPalette.MENU_DISABLED_HOVER_TEXT;
            }
            painter.text(item.label(), (x + 1) * cell, y, color);
            painter.text(item.shortcut(), (x + width - 1 - item.shortcut().length()) * cell, y, color);
        }
    }
}
