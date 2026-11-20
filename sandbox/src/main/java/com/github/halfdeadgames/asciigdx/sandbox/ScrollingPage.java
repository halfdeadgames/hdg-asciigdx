package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.ui.ButtonStyle;
import com.github.halfdeadgames.asciigdx.ui.CheckBox;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.ScrollPane;
import com.github.halfdeadgames.asciigdx.ui.ScrollPaneStyle;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.Table;
import com.github.halfdeadgames.asciigdx.ui.TextButton;
import com.github.halfdeadgames.asciigdx.ui.ToggleStyle;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;

/**
 * Interactive check of scroll panes, all in the square font: a pane over a tall column of check
 * boxes and buttons (focus with Tab and the view follows), a pane over a wide and tall table of
 * labels with both scrollbars, and a small pane over labels only (it takes focus itself, and the
 * arrow keys scroll it). Try the wheel, the thumbs, clicks on the tracks, dragging the content,
 * and Page Up, Page Down, Home, and End.
 */
class ScrollingPage implements UiPage {
    private static final int LEFT_X = 2;
    private static final int RIGHT_X = 40;
    private static final int CAPTION_Y = 0;
    private static final int PANES_Y = 2;
    private static final int PANE_ROWS = 12;
    private static final int FOCUS_PANE_COLUMNS = 34;
    private static final int TABLE_PANE_COLUMNS = 40;
    private static final int LABELS_PANE_Y = 17;
    private static final int LABELS_PANE_COLUMNS = 34;
    private static final int LABELS_PANE_ROWS = 6;
    private static final int FOCUS_ITEM_COUNT = 20;
    private static final int TABLE_ROWS = 30;
    private static final int TABLE_COLUMNS = 10;
    private static final int LABEL_LINE_COUNT = 14;

    private final LabelStyle caption;
    private final LabelStyle highlight;
    private final ButtonStyle buttonStyle;
    private final ToggleStyle checkBoxStyle;
    private final ScrollPaneStyle paneStyle;

    ScrollingPage(Skin skin) {
        caption = skin.getLabelStyle("text");
        highlight = skin.getLabelStyle("highlight");
        buttonStyle = skin.getButtonStyle("button");
        checkBoxStyle = skin.getCheckBoxStyle("checkBox");
        paneStyle = skin.getScrollPaneStyle("scrollPane");
    }

    @Override
    public String id() {
        return "scrolling";
    }

    @Override
    public String title() {
        return "Scrolling";
    }

    @Override
    public Widget createContent(int columns, int rows) {
        WidgetGroup content = new WidgetGroup();
        addCaption(content, "Buttons and check boxes (Tab follows)", LEFT_X, CAPTION_Y);
        addPane(content, createFocusColumn(), LEFT_X, PANES_Y, FOCUS_PANE_COLUMNS, PANE_ROWS);
        addCaption(content, "Table of labels (two scrollbars)", RIGHT_X, CAPTION_Y);
        addPane(content, createLabelTable(), RIGHT_X, PANES_Y, TABLE_PANE_COLUMNS, PANE_ROWS);
        addCaption(content, "Labels only (pane takes focus, arrows scroll)", LEFT_X,
                LABELS_PANE_Y - 2);
        addPane(content, createLabelLines(), LEFT_X, LABELS_PANE_Y, LABELS_PANE_COLUMNS,
                LABELS_PANE_ROWS);
        return content;
    }

    private void addCaption(WidgetGroup parent, String text, int x, int y) {
        Label label = new Label(text, caption);
        label.setPosition(x, y);
        parent.addChild(label);
    }

    private void addPane(WidgetGroup parent, Widget paneContent, int x, int y, int width,
            int height) {
        ScrollPane pane = new ScrollPane(paneContent, paneStyle);
        pane.setPosition(x, y);
        pane.setSize(width, height);
        parent.addChild(pane);
    }

    private Widget createFocusColumn() {
        WidgetGroup column = new WidgetGroup();
        for (int i = 0; i < FOCUS_ITEM_COUNT; i++) {
            Widget item = i % 2 == 0
                    ? new CheckBox("Option " + (i + 1), checkBoxStyle)
                    : new TextButton("Button " + (i + 1), buttonStyle);
            item.setPosition(0, i);
            column.addChild(item);
        }
        return column;
    }

    private Widget createLabelTable() {
        Table table = new Table();
        for (int row = 0; row < TABLE_ROWS; row++) {
            for (int column = 0; column < TABLE_COLUMNS; column++) {
                String text = String.format("R%02dC%02d", row, column);
                table.add(new Label(text, (row + column) % 2 == 0 ? caption : highlight))
                        .padRight(1);
            }
            table.row();
        }
        return table;
    }

    private Widget createLabelLines() {
        WidgetGroup lines = new WidgetGroup();
        for (int i = 0; i < LABEL_LINE_COUNT; i++) {
            Label line = new Label("Line " + (i + 1) + ": a label that cannot take focus",
                    i % 2 == 0 ? caption : highlight);
            line.setPosition(0, i);
            lines.addChild(line);
        }
        return lines;
    }
}
