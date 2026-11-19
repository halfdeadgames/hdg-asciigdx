package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.ui.ButtonStyle;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.Menu;
import com.github.halfdeadgames.asciigdx.ui.MenuBar;
import com.github.halfdeadgames.asciigdx.ui.MenuBarStyle;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.TextButton;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;

/**
 * Interactive check of a short (square font) menu bar. Open a menu with a click, move over the
 * other titles to switch, choose an item, and close with Escape or a press outside. The button
 * under the drop-downs must not click when a press only closes a menu.
 */
class MenusPage implements UiPage {
    static final int BAR_X = 4;
    static final int BAR_Y = 2;
    private static final int WIDE_LAYOUT_MIN_COLUMNS = 80;
    private static final int BAR_WIDTH = 40;
    private static final int BUTTON_Y = 5;
    private static final int CAPTION_Y = 10;
    private static final int LAST_ACTION_Y = 14;

    private final UiRoot uiRoot;
    private final MenuBarStyle barStyle;
    private final ButtonStyle buttonStyle;
    private final LabelStyle caption;
    private WidgetGroup content;
    private MenuBar bar;

    MenusPage(UiRoot uiRoot, Skin skin) {
        this.uiRoot = uiRoot;
        barStyle = skin.getMenuBarStyle("menuBar");
        buttonStyle = skin.getButtonStyle("button");
        caption = skin.getLabelStyle("text");
    }

    boolean isMenuOpen() {
        return bar != null && bar.isOpen();
    }

    @Override
    public String id() {
        return "menus";
    }

    @Override
    public String title() {
        return "Menus";
    }

    // The menu bar stays registered with the root for the root's whole life, so the page builds
    // its content once and shows the same widgets every time.
    @Override
    public Widget createContent(int columns, int rows) {
        if (content == null) {
            content = buildContent(columns >= WIDE_LAYOUT_MIN_COLUMNS);
        }
        return content;
    }

    private WidgetGroup buildContent(boolean wide) {
        WidgetGroup group = new WidgetGroup();
        Label lastAction = new Label("Last action: none", caption);
        lastAction.setPosition(BAR_X, LAST_ACTION_Y);
        group.addChild(lastAction);
        bar = createBar(lastAction);
        group.addChild(bar);
        group.addChild(createCaption("Click a title; Escape or an outside press closes",
                CAPTION_Y));
        group.addChild(createCaption("Alt+letter opens a menu; Alt+F cycles File and Format",
                CAPTION_Y + 1));
        addMenuKeysCaption(group, wide);
        group.addChild(createUnderneathButton(lastAction, wide));
        return group;
    }

    private void addMenuKeysCaption(WidgetGroup group, boolean wide) {
        if (wide) {
            group.addChild(createCaption(
                    "In a menu: letters, arrows, and Enter work; Z cycles the zoom items",
                    CAPTION_Y + 2));
            return;
        }
        group.addChild(createCaption("In a menu: letters, arrows, and Enter work;", CAPTION_Y + 2));
        group.addChild(createCaption("Z cycles the zoom items", CAPTION_Y + 3));
    }

    private MenuBar createBar(Label lastAction) {
        MenuBar menuBar = MenuBar.of(uiRoot, barStyle);
        menuBar.setSize(BAR_WIDTH, 0);
        menuBar.setPosition(BAR_X, BAR_Y);
        addMenu(menuBar, "File", lastAction, "New", "Open file", "Save", "Quit");
        addMenu(menuBar, "Edit", lastAction, "Undo", "Redo");
        addMenu(menuBar, "View", lastAction, "Zoom in", "Zoom out", "Full screen mode");
        addMenu(menuBar, "Format", lastAction, "Bold", "Italic");
        return menuBar;
    }

    // The first letter of every title and item is its trigger, so "File" and "Format" share F,
    // and "Zoom in" and "Zoom out" share Z, to show the cycling.
    private static void addMenu(MenuBar bar, String title, Label lastAction, String... itemTexts) {
        Menu menu = bar.addMenu(title, title.charAt(0));
        for (String itemText : itemTexts) {
            menu.addItem(itemText, itemText.charAt(0),
                    () -> showAction(lastAction, title + " > " + itemText));
        }
    }

    private static void showAction(Label lastAction, String description) {
        lastAction.setText("Last action: " + description);
        lastAction.pack();
    }

    private Label createCaption(String text, int y) {
        Label captionLabel = new Label(text, caption);
        captionLabel.setPosition(BAR_X, y);
        return captionLabel;
    }

    private TextButton createUnderneathButton(Label lastAction, boolean wide) {
        String text = wide
                ? "Separate button: a press that only closes a menu must not click it"
                : "Separate button: a closing press must not click it";
        TextButton button = new TextButton(text, buttonStyle);
        button.setPosition(BAR_X, BUTTON_Y);
        button.addClickListener(clicked -> showAction(lastAction, "button underneath"));
        return button;
    }
}
