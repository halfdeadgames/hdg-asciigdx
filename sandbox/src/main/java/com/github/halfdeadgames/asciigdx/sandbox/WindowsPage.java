package com.github.halfdeadgames.asciigdx.sandbox;

import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.TextButton;
import com.github.halfdeadgames.asciigdx.ui.TextField;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;
import com.github.halfdeadgames.asciigdx.ui.Window;
import com.github.halfdeadgames.asciigdx.ui.WindowStyle;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive check of windows in the root's window layer: a window with a close button that holds
 * buttons and a text field, a window without a close button, and a window that also closes on
 * Escape. Drag a window by its title bar, click one to raise it, press Tab to see that focus stays
 * inside the window, and close windows to bring them back with "Reopen windows". The windows are
 * removed from the layer when another page is shown.
 */
class WindowsPage implements UiPage {
    private static final int HINT_X = 4;
    private static final int HINT_Y = 1;
    private static final int REOPEN_BUTTON_Y = 4;
    private static final int SETTINGS_X = 6;
    private static final int SETTINGS_Y = 14;
    private static final int NOTES_X = 50;
    private static final int NOTES_Y = 16;
    private static final int ESCAPE_X = 30;
    private static final int ESCAPE_Y = 32;
    private static final int NAME_FIELD_WIDTH = 18;
    private static final int BUTTON_GAP = 1;
    private static final int FORM_ROW_STEP = 2;

    private final UiRoot uiRoot;
    private final Skin skin;
    private final List<Window> windows = new ArrayList<>();

    WindowsPage(UiRoot uiRoot, Skin skin) {
        this.uiRoot = uiRoot;
        this.skin = skin;
    }

    @Override
    public String id() {
        return "windows";
    }

    @Override
    public String title() {
        return "Windows";
    }

    @Override
    public Widget createContent(int columns, int rows) {
        removeWindowsFromLayer();
        windows.clear();
        windows.add(createSettingsWindow());
        windows.add(createNotesWindow());
        windows.add(createEscapeWindow());
        openClosedWindows();
        return createPageContent();
    }

    @Override
    public void pageHidden() {
        removeWindowsFromLayer();
    }

    private Widget createPageContent() {
        WidgetGroup content = new WidgetGroup();
        Label hint = new Label("Drag a title bar to move a window. Click a window to raise it. "
                + "Tab stays inside the focused window.", skin.getLabelStyle("text"));
        hint.setPosition(HINT_X, HINT_Y);
        content.addChild(hint);
        TextButton reopen = new TextButton("Reopen windows", skin.getButtonStyle("button"));
        reopen.setPosition(HINT_X, REOPEN_BUTTON_Y);
        reopen.addClickListener(clicked -> openClosedWindows());
        content.addChild(reopen);
        return content;
    }

    private Window createSettingsWindow() {
        Window window = new Window("Settings", skin.getWindowStyle("window"));
        WidgetGroup form = new WidgetGroup();
        TextButton apply = new TextButton("Apply", skin.getButtonStyle("button"));
        form.addChild(apply);
        TextButton reset = new TextButton("Reset", skin.getButtonStyle("button"));
        reset.setPosition(apply.getWidth() + BUTTON_GAP, 0);
        form.addChild(reset);
        TextField name = new TextField(skin.getTextFieldStyle("default"), NAME_FIELD_WIDTH);
        name.setPlaceholderText("Name");
        name.setPosition(0, FORM_ROW_STEP);
        form.addChild(name);
        reset.addClickListener(clicked -> name.setText(""));
        TextButton cancel = new TextButton("Cancel", skin.getButtonStyle("button"));
        cancel.setPosition(0, 2 * FORM_ROW_STEP);
        cancel.addClickListener(clicked -> window.close());
        form.addChild(cancel);
        return placed(window, form, SETTINGS_X, SETTINGS_Y);
    }

    private Window createNotesWindow() {
        Window window = new Window("Notes", withoutCloseButton(skin.getWindowStyle("window")));
        Label text = new Label("No close button here. Move me or raise me.",
                skin.getLabelStyle("text"));
        return placed(window, text, NOTES_X, NOTES_Y);
    }

    private Window createEscapeWindow() {
        Window window = new Window("Esc closes", skin.getWindowStyle("window"));
        window.setCloseOnEscape(true);
        WidgetGroup body = new WidgetGroup();
        Label text = new Label("Focus the button, then press Esc.", skin.getLabelStyle("text"));
        body.addChild(text);
        TextButton done = new TextButton("Done", skin.getButtonStyle("button"));
        done.setPosition(0, FORM_ROW_STEP);
        done.addClickListener(clicked -> window.close());
        body.addChild(done);
        return placed(window, body, ESCAPE_X, ESCAPE_Y);
    }

    private static Window placed(Window window, Widget content, int x, int y) {
        window.setContent(content);
        window.pack();
        window.setPosition(x, y);
        return window;
    }

    private static WindowStyle withoutCloseButton(WindowStyle style) {
        return WindowStyle.of(style.getBox(), style.getBorder(), style.getTitle(),
                style.getBackgroundRgba()).withFocusedBorder(style.getFocusedBorder());
    }

    private void openClosedWindows() {
        for (Window window : windows) {
            if (window.getParent() == null) {
                uiRoot.getWindowLayer().addChild(window);
            }
        }
    }

    private void removeWindowsFromLayer() {
        for (Window window : windows) {
            if (window.getParent() != null) {
                uiRoot.getWindowLayer().removeChild(window);
            }
        }
    }
}
