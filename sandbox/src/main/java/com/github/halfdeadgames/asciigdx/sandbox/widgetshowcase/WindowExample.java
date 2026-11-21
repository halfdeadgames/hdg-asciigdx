package com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.TextButton;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;
import com.github.halfdeadgames.asciigdx.ui.Window;
import com.github.halfdeadgames.asciigdx.ui.WindowStyle;
import java.util.ArrayList;
import java.util.List;

/** Every window style of the skin, opened from buttons in the root's window layer. */
final class WindowExample implements WidgetExample {
    private static final String[] STYLE_NAMES = {"window"};
    private static final int CASCADE_STEP = 2;
    private static final int OPEN_BELOW_ROWS = 2;

    private final List<Window> openedWindows = new ArrayList<>();
    private UiRoot uiRoot;
    private int openedCount;

    @Override
    public String id() {
        return "window";
    }

    @Override
    public String title() {
        return "Window";
    }

    @Override
    public Widget create(Skin skin, UiRoot uiRoot, int columns, int rows) {
        this.uiRoot = uiRoot;
        openedWindows.clear();
        openedCount = 0;
        DemoSheet sheet = new DemoSheet(skin);
        Label lastEvent = new Label("Last window event: none", skin.getLabelStyle("text"));
        sheet.addHeading("Live controls");
        sheet.addDemo("live: last window event", lastEvent);
        ButtonRow closeRow = new ButtonRow(skin);
        closeRow.add("Close all", this::closeAll);
        sheet.addDemo("live: close all windows", closeRow.build());
        for (String styleName : STYLE_NAMES) {
            addStyleDemos(sheet, skin, styleName, lastEvent);
        }
        return sheet.build();
    }

    @Override
    public void exampleHidden() {
        closeAll();
    }

    private void addStyleDemos(DemoSheet sheet, Skin skin, String styleName, Label lastEvent) {
        WindowStyle style = skin.getWindowStyle(styleName);
        sheet.addHeading(styleName);
        ButtonRow row = new ButtonRow(skin);
        TextButton[] withClose = new TextButton[1];
        withClose[0] = row.add("With close button",
                () -> open(skin, style, "Close me", withClose[0], lastEvent));
        row.add("Without close button", () -> open(skin, withoutCloseButton(style),
                "No close button", withClose[0], lastEvent));
        sheet.addDemo(styleName + " (opens a window)", row.build());
    }

    private void open(Skin skin, WindowStyle style, String title, Widget anchor,
            Label lastEvent) {
        Window window = new Window(title, style);
        window.setCloseOnEscape(true);
        window.setContent(createContent(skin, window));
        window.pack();
        int cascade = (openedCount % 4) * CASCADE_STEP;
        window.setPosition(WidgetPlacement.surfaceColumn(anchor) + cascade,
                WidgetPlacement.surfaceRow(anchor) + OPEN_BELOW_ROWS + cascade);
        window.addCloseListener(closed -> {
            openedWindows.remove(closed);
            lastEvent.setText("Last window event: closed \"" + closed.getTitle() + "\"");
            lastEvent.pack();
        });
        openedCount++;
        openedWindows.add(window);
        uiRoot.getWindowLayer().addChild(window);
        lastEvent.setText("Last window event: opened \"" + title + "\"");
        lastEvent.pack();
    }

    private Widget createContent(Skin skin, Window window) {
        WidgetGroup body = new WidgetGroup();
        body.addChild(new Label("Drag the title bar. Esc closes.", skin.getLabelStyle(
                "windowText")));
        TextButton done = new TextButton("Done", skin.getButtonStyle("button"));
        done.setPosition(0, 2);
        done.addClickListener(clicked -> window.close());
        body.addChild(done);
        body.pack();
        return body;
    }

    private void closeAll() {
        for (int i = openedWindows.size() - 1; i >= 0; i--) {
            Window window = openedWindows.get(i);
            if (window.getParent() != null) {
                window.close();
            }
        }
        openedWindows.clear();
    }

    private static WindowStyle withoutCloseButton(WindowStyle style) {
        return WindowStyle.of(style.getBox(), style.getBorder(), style.getTitle(),
                style.getBackgroundRgba()).withFocusedBorder(style.getFocusedBorder());
    }
}
