package com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.ScrollPane;
import com.github.halfdeadgames.asciigdx.ui.ScrollPaneStyle;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.Table;
import com.github.halfdeadgames.asciigdx.ui.TextButton;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;

/** The scroll pane style of the skin over content that scrolls in both directions. */
final class ScrollPaneExample implements WidgetExample {
    private static final String STYLE_NAME = "scrollPane";
    private static final int PANE_COLUMNS = 40;
    private static final int PANE_ROWS = 12;
    private static final int TABLE_ROWS = 30;
    private static final int TABLE_COLUMNS = 14;
    private static final int LINE_COUNT = 20;

    @Override
    public String id() {
        return "scroll-pane";
    }

    @Override
    public String title() {
        return "ScrollPane";
    }

    @Override
    public Widget create(Skin skin, UiRoot uiRoot, int columns, int rows) {
        DemoSheet sheet = new DemoSheet(skin);
        ScrollPaneStyle style = skin.getScrollPaneStyle(STYLE_NAME);
        ScrollPane tablePane = createPane(createLabelTable(skin), style);
        Label position = new Label("Scroll: 0, 0", skin.getLabelStyle("text"));
        sheet.addHeading("Live controls");
        sheet.addDemo("live: scroll the table pane",
                createJumpButtons(skin, tablePane, position));
        sheet.addDemo("live: scroll position", position);
        sheet.addHeading(STYLE_NAME);
        sheet.addDemo(STYLE_NAME + " (both bars)", tablePane);
        sheet.addDemo(STYLE_NAME + " (text lines)",
                createPane(createTextLines(skin), style));
        return sheet.build();
    }

    private static ScrollPane createPane(Widget content, ScrollPaneStyle style) {
        ScrollPane pane = new ScrollPane(content, style);
        pane.setSize(PANE_COLUMNS, PANE_ROWS);
        return pane;
    }

    private Widget createJumpButtons(Skin skin, ScrollPane pane, Label position) {
        WidgetGroup buttons = new WidgetGroup();
        TextButton topLeft = new TextButton("Top left", skin.getButtonStyle("button"));
        topLeft.addClickListener(clicked -> {
            pane.setScrollPosition(0, 0);
            showPosition(pane, position);
        });
        TextButton bottomRight = new TextButton("Bottom right", skin.getButtonStyle("button"));
        bottomRight.setPosition(topLeft.getWidth() + 1, 0);
        bottomRight.addClickListener(clicked -> {
            pane.setScrollPosition(pane.getMaxScrollColumn(), pane.getMaxScrollRow());
            showPosition(pane, position);
        });
        buttons.addChild(topLeft);
        buttons.addChild(bottomRight);
        buttons.pack();
        return buttons;
    }

    private static void showPosition(ScrollPane pane, Label position) {
        position.setText("Scroll: " + pane.getScrollColumn() + ", " + pane.getScrollRow()
                + " of " + pane.getMaxScrollColumn() + ", " + pane.getMaxScrollRow());
        position.pack();
    }

    private Widget createLabelTable(Skin skin) {
        LabelStyle plain = skin.getLabelStyle("text");
        LabelStyle highlight = skin.getLabelStyle("highlight");
        Table table = new Table();
        for (int row = 0; row < TABLE_ROWS; row++) {
            for (int column = 0; column < TABLE_COLUMNS; column++) {
                String text = String.format("R%02dC%02d", row, column);
                table.add(new Label(text, (row + column) % 2 == 0 ? plain : highlight))
                        .padRight(1);
            }
            table.row();
        }
        table.pack();
        return table;
    }

    private Widget createTextLines(Skin skin) {
        LabelStyle plain = skin.getLabelStyle("text");
        WidgetGroup lines = new WidgetGroup();
        for (int i = 0; i < LINE_COUNT; i++) {
            Label line = new Label("Text line " + (i + 1), plain);
            line.setPosition(0, i);
            lines.addChild(line);
        }
        lines.pack();
        return lines;
    }
}
