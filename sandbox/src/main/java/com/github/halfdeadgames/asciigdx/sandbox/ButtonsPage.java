package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.ui.ButtonStyle;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.TextButton;
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

    private final ButtonStyle tallStyle;
    private final ButtonStyle squareStyle;
    private final LabelStyle caption;

    ButtonsPage(Skin skin) {
        tallStyle = skin.getButtonStyle("tallButton");
        squareStyle = skin.getButtonStyle("button");
        caption = skin.getLabelStyle("text");
    }

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
        Label lastClick = new Label("Last click: none", caption);
        lastClick.setPosition(0, 14);
        root.addChild(lastClick);
        addCaption(root, "Tall buttons: normal, pointer over, pressed, disabled", 0);
        addRow(root, tallStyle, 2, lastClick);
        addCaption(root, "Square buttons", 8);
        addRow(root, squareStyle, 10, lastClick);
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

    private void addCaption(WidgetGroup root, String text, int y) {
        Label captionLabel = new Label(text, caption);
        captionLabel.setPosition(0, y);
        root.addChild(captionLabel);
    }
}
