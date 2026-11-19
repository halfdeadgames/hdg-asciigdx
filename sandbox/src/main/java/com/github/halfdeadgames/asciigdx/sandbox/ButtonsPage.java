package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.ui.ButtonStyle;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.TextButton;
import com.github.halfdeadgames.asciigdx.ui.UiFont;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;

/**
 * Interactive check of text buttons: tall and square styles, four clearly different state looks,
 * a disabled button, and a label that shows the last click. Click with the mouse, or focus a button
 * by pressing it and use Enter or Space.
 */
class ButtonsPage implements UiPage {
    private static final int ROOT_X = 4;
    private static final int ROOT_Y = 2;
    private static final int BUTTON_GAP = 2;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0x000000FF;
    private static final int NORMAL_BLUE = 0x203070FF;
    private static final int OVER_TEAL = 0x00A0A0FF;
    private static final int PRESSED_GOLD = 0xFFD700FF;
    private static final int DISABLED_GRAY = 0x404040FF;
    private static final int DISABLED_TEXT = 0x909090FF;
    private static final ButtonStyle TALL_STYLE = createStyle(UiFont.TALL);
    private static final ButtonStyle SQUARE_STYLE = createStyle(UiFont.SQUARE);
    private static final LabelStyle CAPTION = LabelStyle.of(UiFont.SQUARE, WHITE, BLACK);

    @Override
    public String id() {
        return "buttons";
    }

    @Override
    public String title() {
        return "Buttons";
    }

    @Override
    public Widget createContent(int columns, int rows) {
        WidgetGroup content = new WidgetGroup();
        WidgetGroup root = new WidgetGroup();
        root.setPosition(ROOT_X, ROOT_Y);
        Label lastClick = new Label("Last click: none", CAPTION);
        lastClick.setPosition(0, 14);
        root.addChild(lastClick);
        addCaption(root, "Tall buttons: normal, pointer over, pressed, disabled", 0);
        addRow(root, TALL_STYLE, 2, lastClick);
        addCaption(root, "Square buttons", 8);
        addRow(root, SQUARE_STYLE, 10, lastClick);
        addCaption(root, "Press, drag out and back in, release inside to click", 12);
        content.addChild(root);
        return content;
    }

    private static void addRow(WidgetGroup root, ButtonStyle style, int y, Label lastClick) {
        int x = 0;
        x = addButton(root, "Start", style, x, y, lastClick, false);
        x = addButton(root, "Options", style, x, y, lastClick, false);
        addButton(root, "Disabled", style, x, y, lastClick, true);
    }

    private static int addButton(WidgetGroup root, String text, ButtonStyle style, int x, int y,
            Label lastClick, boolean disabled) {
        TextButton button = new TextButton(text, style);
        button.setPosition(x, y);
        button.setDisabled(disabled);
        button.addClickListener(clicked -> {
            lastClick.setText("Last click: " + clicked.getText());
            lastClick.pack();
        });
        root.addChild(button);
        return x + button.getWidth() + BUTTON_GAP;
    }

    private static void addCaption(WidgetGroup root, String text, int y) {
        Label caption = new Label(text, CAPTION);
        caption.setPosition(0, y);
        root.addChild(caption);
    }

    private static ButtonStyle createStyle(UiFont font) {
        return ButtonStyle.of(LabelStyle.of(font, WHITE, NORMAL_BLUE),
                LabelStyle.of(font, BLACK, OVER_TEAL), LabelStyle.of(font, BLACK, PRESSED_GOLD),
                LabelStyle.of(font, DISABLED_TEXT, DISABLED_GRAY));
    }
}
