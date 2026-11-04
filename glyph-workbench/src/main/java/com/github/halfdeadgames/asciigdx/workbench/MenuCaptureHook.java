package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.github.halfdeadgames.asciigdx.workbench.ui.MenuBar;
import java.util.function.Predicate;

final class MenuCaptureHook {

    private MenuCaptureHook() {
    }

    static void apply(DevCapture.MenuRequest request, MenuBar menuBar, Predicate<PaintMenu.Action> availability,
                      int cell) {
        for (int i = 0; i < menuBar.menuCount(); i++) {
            if (!menuBar.menu(i).title().equalsIgnoreCase(request.title())) {
                continue;
            }
            if (request.inspect()) {
                inspect(menuBar, availability, cell, i, request.item());
            } else if (request.hoverOnly()) {
                menuBar.hover(menuBar.titleColumn(i), MenuBar.NOT_IN_DROP_DOWN, 0, availability);
            } else if (menuBar.openByAccessKey(menuBar.menu(i).accessKey(), availability)) {
                int moves = menuBar.menu(i).items().size();
                while (request.item() >= 0 && menuBar.highlightedItem() != request.item() && moves-- > 0) {
                    menuBar.moveItem(1, availability);
                }
            }
            return;
        }
    }

    /** Hovers a title (item -1) or an item of that menu, opened via any available entry, as the pointer would. */
    private static void inspect(MenuBar menuBar, Predicate<PaintMenu.Action> availability, int cell, int menu,
                                int item) {
        if (item < 0) {
            menuBar.hover(menuBar.titleColumn(menu), MenuBar.NOT_IN_DROP_DOWN, 0, availability);
            return;
        }
        if (!menuBar.openByAccessKey(menuBar.menu(menu).accessKey(), availability)) {
            return;
        }
        int columns = Gdx.graphics.getWidth() / cell;
        menuBar.hover(menuBar.dropDownColumn(menu, columns) + 1, item, columns, availability);
    }
}
