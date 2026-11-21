package com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.Menu;
import com.github.halfdeadgames.asciigdx.ui.MenuBar;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.HashMap;
import java.util.Map;

/** Every menu bar style of the skin, with menus whose items report to a label. */
final class MenuBarExample implements WidgetExample {
    private static final String[] STYLE_NAMES = {"menuBar"};
    private static final int BAR_COLUMNS = 40;
    private static final int DROP_DOWN_SPACE_ROWS = 6;

    // A menu bar registers with its root for the root's whole life, so each root gets one.
    private final Map<String, MenuBar> barsOfCurrentRoot = new HashMap<>();
    private UiRoot barsRoot;
    private Label chosenLabel;

    @Override
    public String id() {
        return "menu-bar";
    }

    @Override
    public String title() {
        return "MenuBar";
    }

    @Override
    public Widget create(Skin skin, UiRoot uiRoot, BoundaryState boundaryState, int columns,
            int rows) {
        if (uiRoot != barsRoot) {
            barsOfCurrentRoot.clear();
            barsRoot = uiRoot;
        }
        DemoSheet sheet = new DemoSheet(skin, boundaryState);
        chosenLabel = new Label("Last item: none", skin.getLabelStyle("text"));
        sheet.addHeading("Live controls");
        sheet.addDemo("live: last menu item", chosenLabel);
        for (String styleName : STYLE_NAMES) {
            sheet.addHeading(styleName);
            MenuBar bar = barOf(skin, uiRoot, styleName);
            detach(bar);
            sheet.addDemo(styleName + " (open a menu)", bar);
            sheet.addEmptyRows(DROP_DOWN_SPACE_ROWS);
        }
        return sheet.build();
    }

    @Override
    public void exampleHidden() {
        for (MenuBar bar : barsOfCurrentRoot.values()) {
            if (bar.isOpen()) {
                bar.close();
            }
        }
    }

    private MenuBar barOf(Skin skin, UiRoot uiRoot, String styleName) {
        MenuBar existing = barsOfCurrentRoot.get(styleName);
        if (existing != null) {
            return existing;
        }
        MenuBar bar = MenuBar.of(uiRoot, skin.getMenuBarStyle(styleName));
        bar.setSize(BAR_COLUMNS, 0);
        addMenu(bar, "File", 'F', "New", "Open", "Save");
        addMenu(bar, "Edit", 'E', "Cut", "Copy", "Paste");
        addMenu(bar, "Help", 'H', "About");
        barsOfCurrentRoot.put(styleName, bar);
        return bar;
    }

    private void addMenu(MenuBar bar, String title, char trigger, String... itemTexts) {
        Menu menu = bar.addMenu(title, trigger);
        for (String itemText : itemTexts) {
            menu.addItem(itemText, itemText.charAt(0), () -> showChosen(title, itemText));
        }
    }

    // Reads the field at click time, so a cached bar updates the label of the current tree.
    private void showChosen(String menuTitle, String itemText) {
        chosenLabel.setText("Last item: " + menuTitle + " > " + itemText);
        chosenLabel.pack();
    }

    private static void detach(MenuBar bar) {
        if (bar.getParent() != null) {
            bar.getParent().removeChild(bar);
        }
    }
}
