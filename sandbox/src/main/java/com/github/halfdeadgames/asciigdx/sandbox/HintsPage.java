package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.ui.ButtonStyle;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.TextButton;
import com.github.halfdeadgames.asciigdx.ui.Tooltip;
import com.github.halfdeadgames.asciigdx.ui.UiFont;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;

/**
 * Interactive check of hover hints: rest the pointer on a widget to see its hint. Covers a hint
 * in the tall font, one in the square font, a label as target, a button near the right edge whose
 * hint flips to the left, and one on the last row whose hint flips above. Pressing a button hides
 * its hint until the pointer leaves it.
 */
class HintsPage implements UiPage {
    private static final int LEFT_X = 4;
    private static final int TOP_Y = 2;
    private static final int RIGHT_EDGE_MARGIN = 12;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0x000000FF;
    private static final int NORMAL_BLUE = 0x203070FF;
    private static final int OVER_TEAL = 0x00A0A0FF;
    private static final int PRESSED_GOLD = 0xFFD700FF;
    private static final int HINT_BACKGROUND = 0x303030FF;
    private static final int HINT_GOLD = 0xFFD700FF;
    private static final LabelStyle CAPTION = LabelStyle.of(UiFont.SQUARE, WHITE, BLACK);
    private static final LabelStyle TALL_HINT =
            LabelStyle.of(UiFont.TALL, HINT_GOLD, HINT_BACKGROUND);
    private static final LabelStyle SQUARE_HINT =
            LabelStyle.of(UiFont.SQUARE, WHITE, HINT_BACKGROUND);
    private static final ButtonStyle TALL_BUTTON = createButtonStyle(UiFont.TALL);
    private static final ButtonStyle SQUARE_BUTTON = createButtonStyle(UiFont.SQUARE);

    private final UiRoot uiRoot;
    private WidgetGroup content;
    private TextButton rightEdgeButton;

    /**
     * Creates the page.
     *
     * @param uiRoot root whose popup layer shows the hints
     */
    HintsPage(UiRoot uiRoot) {
        this.uiRoot = uiRoot;
    }

    @Override
    public String id() {
        return "hints";
    }

    @Override
    public String title() {
        return "Hints";
    }

    @Override
    public Widget createContent(int columns, int rows) {
        if (content == null) {
            content = buildContent(columns, rows);
        }
        return content;
    }

    /** Column of the right-edge button's left cell, relative to the page content. */
    int captureColumn() {
        return rightEdgeButton.getX();
    }

    /** Row of the right-edge button, relative to the page content. */
    int captureRow() {
        return rightEdgeButton.getY();
    }

    private WidgetGroup buildContent(int columns, int rows) {
        WidgetGroup group = new WidgetGroup();
        addCaption(group, "Rest the pointer on a widget; press to hide its hint", TOP_Y);
        addButton(group, "Tall hint", TALL_BUTTON, LEFT_X, TOP_Y + 2,
                "Tall font hint, on an even row", TALL_HINT);
        addButton(group, "Square hint", SQUARE_BUTTON, LEFT_X, TOP_Y + 6,
                "Square font hint", SQUARE_HINT);
        Label labelTarget = new Label("Hover this label", CAPTION);
        labelTarget.setPosition(LEFT_X, TOP_Y + 9);
        group.addChild(labelTarget);
        Tooltip.attachText(uiRoot, labelTarget, "Labels can have hints too", SQUARE_HINT);
        rightEdgeButton = addButton(group, "Right edge", TALL_BUTTON, columns - RIGHT_EDGE_MARGIN,
                TOP_Y + 2, "Flips to the left of the pointer", SQUARE_HINT);
        addButton(group, "Last row", SQUARE_BUTTON, LEFT_X, rows - 1,
                "Flips above the pointer", SQUARE_HINT);
        return group;
    }

    private TextButton addButton(WidgetGroup group, String text, ButtonStyle style, int x, int y,
            String hintText, LabelStyle hintStyle) {
        TextButton button = new TextButton(text, style);
        button.setPosition(x, y);
        group.addChild(button);
        Tooltip.attachText(uiRoot, button, hintText, hintStyle);
        return button;
    }

    private static void addCaption(WidgetGroup group, String text, int y) {
        Label caption = new Label(text, CAPTION);
        caption.setPosition(LEFT_X, y);
        group.addChild(caption);
    }

    private static ButtonStyle createButtonStyle(UiFont font) {
        return ButtonStyle.of(LabelStyle.of(font, WHITE, NORMAL_BLUE),
                LabelStyle.of(font, BLACK, OVER_TEAL), LabelStyle.of(font, BLACK, PRESSED_GOLD),
                LabelStyle.of(font, WHITE, NORMAL_BLUE));
    }
}
