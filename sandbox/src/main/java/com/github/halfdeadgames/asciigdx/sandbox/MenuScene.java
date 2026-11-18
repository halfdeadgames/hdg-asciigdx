package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.github.halfdeadgames.asciigdx.draw.GridDrawing;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;

/**
 * The scene list, drawn with the engine itself. A group list leads to one page per group. The
 * selected row is shown with inverted colors.
 */
class MenuScene extends GridScene {
    private static final int COLUMNS = 40;
    private static final int TITLE_ROW = 1;
    private static final int FIRST_ITEM_ROW = 3;
    private static final int ROWS_BELOW_ITEMS = 3;
    private static final int TEXT_COLUMN = 2;
    private static final int FIRST_ROW_INDEX = 0;
    private static final int NO_ROW = -1;
    private static final String GROUP_LIST_HEADING = "HDG-AsciiGDX sandbox";
    private static final String GROUP_PAGE_HEADING_PREFIX = "Sandbox > ";

    private final Array<SandboxScene> scenes;
    private final SceneGroup[] groups = SceneGroup.values();
    private final IntArray[] sceneIndicesByGroup = new IntArray[groups.length];
    private final IntArray listedGroupOrdinals = new IntArray();
    private final float cellHeight;
    private final Vector2 touchPoint = new Vector2();
    private boolean showingGroupList = true;
    private int openGroupOrdinal;
    private int selectedRow;

    MenuScene(GlyphSource glyphs, Array<SandboxScene> scenes) {
        super(glyphs, COLUMNS, FIRST_ITEM_ROW + longestListSize(scenes) + ROWS_BELOW_ITEMS);
        this.scenes = scenes;
        cellHeight = glyphs.getCellHeight();
        collectScenesByGroup();
        openGroupOrdinal = listedGroupOrdinals.get(FIRST_ROW_INDEX);
        GridDrawing.drawText(grid, TEXT_COLUMN, grid.getRows() - 2, helpText(), CYAN, CLEAR);
        redraw();
    }

    private static int longestListSize(Array<SandboxScene> scenes) {
        int[] sceneCounts = new int[SceneGroup.values().length];
        int longestGroupSize = 0;
        for (int i = 0; i < scenes.size; i++) {
            int groupSize = ++sceneCounts[scenes.get(i).group().ordinal()];
            longestGroupSize = Math.max(longestGroupSize, groupSize);
        }
        return Math.max(longestGroupSize, SceneGroup.values().length);
    }

    private static String helpText() {
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            return "Tap to open, Back to return";
        }
        return "Up/Down select, Enter open, Esc back";
    }

    private void collectScenesByGroup() {
        for (int ordinal = 0; ordinal < groups.length; ordinal++) {
            sceneIndicesByGroup[ordinal] = new IntArray();
        }
        for (int i = 0; i < scenes.size; i++) {
            sceneIndicesByGroup[scenes.get(i).group().ordinal()].add(i);
        }
        for (int ordinal = 0; ordinal < groups.length; ordinal++) {
            if (sceneIndicesByGroup[ordinal].size > 0) {
                listedGroupOrdinals.add(ordinal);
            }
        }
    }

    @Override
    public String id() {
        return "menu";
    }

    @Override
    public String title() {
        return "Menu";
    }

    boolean isShowingGroupList() {
        return showingGroupList;
    }

    /** Global scene index of the selected row; only valid while a group page is showing. */
    int selectedSceneIndex() {
        return sceneIndicesByGroup[openGroupOrdinal].get(selectedRow);
    }

    void moveSelection(int step) {
        selectedRow = Math.floorMod(selectedRow + step, currentListSize());
        redraw();
    }

    /** Row index in the current list under the screen position, or -1 when it is not on a row. */
    int rowAt(int screenX, int screenY) {
        touchPoint.set(screenX, screenY);
        viewport.unproject(touchPoint);
        int gridRow = (int) ((viewport.getWorldHeight() - touchPoint.y) / cellHeight);
        int row = gridRow - FIRST_ITEM_ROW;
        return row >= 0 && row < currentListSize() ? row : NO_ROW;
    }

    void selectRow(int row) {
        if (row < 0 || row >= currentListSize()) {
            return;
        }
        selectedRow = row;
        redraw();
    }

    /** Opens the page of the selected group, selecting its first scene. */
    void openSelectedGroup() {
        if (!showingGroupList) {
            return;
        }
        openGroupOrdinal = listedGroupOrdinals.get(selectedRow);
        showingGroupList = false;
        selectedRow = FIRST_ROW_INDEX;
        redraw();
    }

    /** Goes back to the group list with the group of the open page selected. */
    void showGroupList() {
        if (showingGroupList) {
            return;
        }
        showingGroupList = true;
        selectedRow = listedGroupOrdinals.indexOf(openGroupOrdinal);
        redraw();
    }

    /** Shows the page of the scene's group with that scene selected. */
    void showSceneInGroup(int sceneIndex) {
        openGroupOrdinal = scenes.get(sceneIndex).group().ordinal();
        showingGroupList = false;
        selectedRow = sceneIndicesByGroup[openGroupOrdinal].indexOf(sceneIndex);
        redraw();
    }

    private int currentListSize() {
        return showingGroupList
                ? listedGroupOrdinals.size
                : sceneIndicesByGroup[openGroupOrdinal].size;
    }

    private void redraw() {
        drawHeading();
        clearItemArea();
        for (int row = 0; row < currentListSize(); row++) {
            drawItem(row);
        }
    }

    private void drawHeading() {
        String heading = showingGroupList
                ? GROUP_LIST_HEADING
                : GROUP_PAGE_HEADING_PREFIX + groups[openGroupOrdinal].title();
        GridDrawing.fillRectangle(grid, 1, TITLE_ROW, COLUMNS - 2, 1, ' ', GOLD, CLEAR);
        GridDrawing.drawText(grid, TEXT_COLUMN, TITLE_ROW, heading, GOLD, CLEAR);
    }

    private void clearItemArea() {
        int itemAreaRows = grid.getRows() - FIRST_ITEM_ROW - ROWS_BELOW_ITEMS;
        GridDrawing.fillRectangle(grid, 1, FIRST_ITEM_ROW, COLUMNS - 2, itemAreaRows, ' ', WHITE,
                CLEAR);
    }

    private void drawItem(int row) {
        boolean selected = row == selectedRow;
        int foreground = selected ? BLACK : WHITE;
        int background = selected ? WHITE : CLEAR;
        int gridRow = FIRST_ITEM_ROW + row;
        GridDrawing.fillRectangle(grid, 1, gridRow, COLUMNS - 2, 1, ' ', foreground, background);
        GridDrawing.drawText(grid, TEXT_COLUMN, gridRow, itemText(row), foreground, background);
    }

    private String itemText(int row) {
        if (!showingGroupList) {
            return scenes.get(sceneIndicesByGroup[openGroupOrdinal].get(row)).title();
        }
        int ordinal = listedGroupOrdinals.get(row);
        return groups[ordinal].title() + " (" + sceneIndicesByGroup[ordinal].size + ")";
    }
}
