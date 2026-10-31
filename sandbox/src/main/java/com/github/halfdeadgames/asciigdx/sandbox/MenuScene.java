package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;

/** The scene list, drawn with the engine itself. Selected row is shown with inverted colors. */
class MenuScene extends GridScene {
    private static final int COLUMNS = 40;
    private static final int TITLE_ROW = 1;
    private static final int FIRST_ITEM_ROW = 3;
    private static final int ROWS_BELOW_ITEMS = 3;
    private static final int TEXT_COLUMN = 2;

    private final Array<SandboxScene> scenes;
    private final float cellHeight;
    private final Vector2 touchPoint = new Vector2();
    private int selectedIndex;

    MenuScene(GlyphSource glyphs, Array<SandboxScene> scenes) {
        super(glyphs, COLUMNS, FIRST_ITEM_ROW + scenes.size + ROWS_BELOW_ITEMS);
        this.scenes = scenes;
        cellHeight = glyphs.getCellHeight();
        writeText(TEXT_COLUMN, TITLE_ROW, "HDG-AsciiGDX sandbox", GOLD, CLEAR);
        writeText(TEXT_COLUMN, grid.getHeight() - 2, helpText(), CYAN, CLEAR);
        drawItems();
    }

    private static String helpText() {
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            return "Tap to open, Back to return";
        }
        return "Up/Down select, Enter open, Esc back";
    }

    @Override
    public String id() {
        return "menu";
    }

    @Override
    public String title() {
        return "Menu";
    }

    int selectedIndex() {
        return selectedIndex;
    }

    void moveSelection(int step) {
        selectedIndex = Math.floorMod(selectedIndex + step, scenes.size);
        drawItems();
    }

    /** Scene index under the given screen position, or -1 when it is not on an item row. */
    int indexAt(int screenX, int screenY) {
        touchPoint.set(screenX, screenY);
        viewport.unproject(touchPoint);
        int row = (int) ((viewport.getWorldHeight() - touchPoint.y) / cellHeight);
        int index = row - FIRST_ITEM_ROW;
        return index >= 0 && index < scenes.size ? index : -1;
    }

    private void drawItems() {
        for (int i = 0; i < scenes.size; i++) {
            boolean selected = i == selectedIndex;
            int foreground = selected ? BLACK : WHITE;
            int background = selected ? WHITE : CLEAR;
            int row = FIRST_ITEM_ROW + i;
            for (int column = 1; column < COLUMNS - 1; column++) {
                grid.set(column, row, ' ', foreground, background);
            }
            writeText(TEXT_COLUMN, row, scenes.get(i).title(), foreground, background);
        }
    }
}
