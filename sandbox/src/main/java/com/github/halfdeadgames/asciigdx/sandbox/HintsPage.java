package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.ui.ButtonStyle;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.TextButton;
import com.github.halfdeadgames.asciigdx.ui.Tooltip;
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

    private final UiRoot uiRoot;
    private final LabelStyle caption;
    private final LabelStyle tallTooltipStyle;
    private final LabelStyle tooltipStyle;
    private final ButtonStyle tallButton;
    private final ButtonStyle squareButton;
    private WidgetGroup content;
    private TextButton rightEdgeButton;

    /**
     * Creates the page.
     *
     * @param uiRoot root whose popup layer shows the hints
     * @param skin source of the label and button styles
     */
    HintsPage(UiRoot uiRoot, Skin skin) {
        this.uiRoot = uiRoot;
        caption = skin.getLabelStyle("text");
        tallTooltipStyle = skin.getLabelStyle("tallTooltip");
        tooltipStyle = skin.getLabelStyle("tooltip");
        tallButton = skin.getButtonStyle("tallButton");
        squareButton = skin.getButtonStyle("button");
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
        addButton(group, "Tall hint", tallButton, LEFT_X, TOP_Y + 2,
                "Tall font hint, on an even row", tallTooltipStyle);
        addButton(group, "Square hint", squareButton, LEFT_X, TOP_Y + 6,
                "Square font hint", tooltipStyle);
        Label labelTarget = new Label("Hover this label", caption);
        labelTarget.setPosition(LEFT_X, TOP_Y + 9);
        group.addChild(labelTarget);
        Tooltip.attachText(uiRoot, labelTarget, "Labels can have hints too", tooltipStyle);
        rightEdgeButton = addButton(group, "Right edge", tallButton, columns - RIGHT_EDGE_MARGIN,
                TOP_Y + 2, "Flips to the left of the pointer", tooltipStyle);
        addButton(group, "Last row", squareButton, LEFT_X, rows - 1,
                "Flips above the pointer", tooltipStyle);
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

    private void addCaption(WidgetGroup group, String text, int y) {
        Label captionLabel = new Label(text, caption);
        captionLabel.setPosition(LEFT_X, y);
        group.addChild(captionLabel);
    }
}
