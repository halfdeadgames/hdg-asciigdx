package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.ui.TopBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.TopBarStatus;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.util.List;

/** The shell of the new-item screens: top bar with crumbs and the dialog area below it. */
final class NewItemPage {

    private static final int CRUMB_ROW = 0;

    private final List<String> crumbs;

    NewItemPage(List<String> crumbs) {
        this.crumbs = crumbs;
    }

    void drawTopBar(Painter painter, int columns) {
        TopBar.draw(painter, columns, CRUMB_ROW, crumbs, TopBarStatus.of("", UiPalette.TEXT));
    }

    static CellRect dialogArea(int columns, int rows) {
        return new CellRect(0, CRUMB_ROW + 1, columns, Math.max(1, rows - CRUMB_ROW - 1));
    }
}
