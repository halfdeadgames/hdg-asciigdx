package com.github.halfdeadgames.asciigdx.skindesigner.files;

import com.github.halfdeadgames.asciigdx.ui.CellArea;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.TextButton;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;
import com.github.halfdeadgames.asciigdx.ui.Window;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/** The "Known skins" window: one button per skin file, recent files first. */
public final class KnownSkinsPicker {
    private static final String WINDOW_TITLE = "Known skins";
    private static final String WINDOW_STYLE = "window";
    private static final String BUTTON_STYLE = "button";
    private static final String LABEL_STYLE = "text";
    private static final String EMPTY_TEXT = "No skin files found yet.";

    private final UiRoot uiRoot;
    private final Skin skin;
    private final Consumer<File> chosenHandler;
    private Window window;

    /**
     * Creates a closed picker.
     *
     * @param chosenHandler called with the chosen file after the picker has closed
     */
    public KnownSkinsPicker(UiRoot uiRoot, Skin skin, Consumer<File> chosenHandler) {
        this.uiRoot = uiRoot;
        this.skin = skin;
        this.chosenHandler = chosenHandler;
    }

    /** Returns whether the picker window is in the root's window layer. */
    public boolean isOpen() {
        return window != null && window.getParent() != null;
    }

    /**
     * Opens the picker centered in the given area, in UI cells; does nothing when it is already
     * open.
     */
    public void open(List<File> files, CellArea area) {
        if (isOpen()) {
            return;
        }
        window = new Window(WINDOW_TITLE, skin.getWindowStyle(WINDOW_STYLE));
        window.setCloseOnEscape(true);
        window.setContent(createContent(files));
        window.pack();
        window.setPosition(
                area.column() + Math.max(0, (area.columnCount() - window.getWidth()) / 2),
                area.row() + Math.max(0, (area.rowCount() - window.getHeight()) / 2));
        uiRoot.getWindowLayer().addChild(window);
    }

    private WidgetGroup createContent(List<File> files) {
        WidgetGroup content = new WidgetGroup();
        if (files.isEmpty()) {
            content.addChild(new Label(EMPTY_TEXT, skin.getLabelStyle(LABEL_STYLE)));
        }
        for (int i = 0; i < files.size(); i++) {
            content.addChild(createButton(files.get(i), i));
        }
        content.pack();
        return content;
    }

    private TextButton createButton(File file, int row) {
        TextButton button = new TextButton(describe(file), skin.getButtonStyle(BUTTON_STYLE));
        button.setPosition(0, row);
        button.addClickListener(clicked -> {
            window.close();
            chosenHandler.accept(file);
        });
        return button;
    }

    private static String describe(File file) {
        File folder = file.getParentFile();
        return folder == null ? file.getName() : file.getName() + "  (" + folder.getName() + ")";
    }
}
