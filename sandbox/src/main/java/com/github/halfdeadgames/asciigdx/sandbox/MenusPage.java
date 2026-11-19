package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.ui.ButtonStyle;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.Menu;
import com.github.halfdeadgames.asciigdx.ui.MenuBar;
import com.github.halfdeadgames.asciigdx.ui.MenuBarStyle;
import com.github.halfdeadgames.asciigdx.ui.TextButton;
import com.github.halfdeadgames.asciigdx.ui.UiFont;
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
    private static final int BAR_WIDTH = 40;
    private static final int BUTTON_Y = 5;
    private static final int CAPTION_Y = 10;
    private static final int LAST_ACTION_Y = 14;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0x000000FF;
    private static final int NORMAL_BLUE = 0x203070FF;
    private static final int OVER_TEAL = 0x00A0A0FF;
    private static final int PRESSED_GOLD = 0xFFD700FF;
    private static final int TRIGGER_PINK = 0xFF40C0FF;
    private static final ButtonStyle LOOK = ButtonStyle.of(
            LabelStyle.of(UiFont.SQUARE, WHITE, NORMAL_BLUE),
            LabelStyle.of(UiFont.SQUARE, BLACK, OVER_TEAL),
            LabelStyle.of(UiFont.SQUARE, BLACK, PRESSED_GOLD),
            LabelStyle.of(UiFont.SQUARE, WHITE, NORMAL_BLUE));
    private static final LabelStyle CAPTION = LabelStyle.of(UiFont.SQUARE, WHITE, BLACK);

    private final UiRoot uiRoot;
    private WidgetGroup content;
    private MenuBar bar;

    MenusPage(UiRoot uiRoot) {
        this.uiRoot = uiRoot;
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
            content = buildContent();
        }
        return content;
    }

    private WidgetGroup buildContent() {
        WidgetGroup group = new WidgetGroup();
        Label lastAction = new Label("Last action: none", CAPTION);
        lastAction.setPosition(BAR_X, LAST_ACTION_Y);
        group.addChild(lastAction);
        bar = createBar(lastAction);
        group.addChild(bar);
        group.addChild(createCaption("Click a title; Escape or an outside press closes",
                CAPTION_Y));
        group.addChild(createCaption("Alt+letter opens a menu; Alt+F cycles File and Format",
                CAPTION_Y + 1));
        group.addChild(createCaption(
                "In a menu: letters, arrows, and Enter work; Z cycles the zoom items",
                CAPTION_Y + 2));
        group.addChild(createUnderneathButton(lastAction));
        return group;
    }

    private MenuBar createBar(Label lastAction) {
        MenuBar bar = MenuBar.of(uiRoot,
                MenuBarStyle.of(LOOK, LOOK).withTriggerRgba(TRIGGER_PINK)
                        .withTriggerUnderline(true));
        bar.setSize(BAR_WIDTH, 0);
        bar.setPosition(BAR_X, BAR_Y);
        addMenu(bar, "File", lastAction, "New", "Open file", "Save", "Quit");
        addMenu(bar, "Edit", lastAction, "Undo", "Redo");
        addMenu(bar, "View", lastAction, "Zoom in", "Zoom out", "Full screen mode");
        addMenu(bar, "Format", lastAction, "Bold", "Italic");
        return bar;
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

    private static Label createCaption(String text, int y) {
        Label caption = new Label(text, CAPTION);
        caption.setPosition(BAR_X, y);
        return caption;
    }

    private static TextButton createUnderneathButton(Label lastAction) {
        TextButton button = new TextButton(
                "Separate button: a press that only closes a menu must not click it", LOOK);
        button.setPosition(BAR_X, BUTTON_Y);
        button.addClickListener(clicked -> showAction(lastAction, "button underneath"));
        return button;
    }
}
