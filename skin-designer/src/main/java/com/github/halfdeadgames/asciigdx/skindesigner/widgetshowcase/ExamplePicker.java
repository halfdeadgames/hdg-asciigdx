package com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.ButtonGroup;
import com.github.halfdeadgames.asciigdx.ui.RadioButton;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.TextButton;
import com.github.halfdeadgames.asciigdx.ui.ToggleStyle;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;
import com.github.halfdeadgames.asciigdx.ui.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** The "Choose a widget" window: one radio button per example and a button that loads it. */
public final class ExamplePicker {
    private static final String WINDOW_TITLE = "Choose a widget";
    private static final String LOAD_BUTTON_TEXT = "Load widget example";
    private static final String WINDOW_STYLE = "window";
    private static final String RADIO_STYLE = "radioButton";
    private static final String BUTTON_STYLE = "button";

    private final UiRoot uiRoot;
    private final Skin skin;
    private final List<WidgetExample> examples;
    private final Consumer<WidgetExample> loadHandler;
    private Window window;

    /**
     * Creates a closed picker.
     *
     * @param loadHandler called with the chosen example after the picker has closed
     */
    public ExamplePicker(UiRoot uiRoot, Skin skin, List<WidgetExample> examples,
            Consumer<WidgetExample> loadHandler) {
        this.uiRoot = uiRoot;
        this.skin = skin;
        this.examples = examples;
        this.loadHandler = loadHandler;
    }

    /** Returns whether the picker window is in the root's window layer. */
    public boolean isOpen() {
        return window != null && window.getParent() != null;
    }

    /**
     * Opens the picker centered in the given area, with the current example preselected. Does
     * nothing when it is already open.
     */
    public void open(int currentIndex, int areaLeft, int areaTop, int areaColumns,
            int areaRows) {
        if (isOpen()) {
            return;
        }
        List<RadioButton> radios = new ArrayList<>();
        window = createWindow(currentIndex, radios);
        window.setPosition(areaLeft + (areaColumns - window.getWidth()) / 2,
                areaTop + (areaRows - window.getHeight()) / 2);
        uiRoot.getWindowLayer().addChild(window);
        uiRoot.setKeyboardFocus(radios.get(currentIndex));
    }

    /** Closes the picker; does nothing when it is closed. */
    public void close() {
        if (isOpen()) {
            window.close();
        }
    }

    private Window createWindow(int currentIndex, List<RadioButton> radios) {
        Window pickerWindow = new Window(WINDOW_TITLE, skin.getWindowStyle(WINDOW_STYLE));
        pickerWindow.setCloseOnEscape(true);
        pickerWindow.setContent(createContent(currentIndex, radios));
        pickerWindow.pack();
        return pickerWindow;
    }

    private WidgetGroup createContent(int currentIndex, List<RadioButton> radios) {
        WidgetGroup content = new WidgetGroup();
        ToggleStyle radioStyle = skin.getRadioButtonStyle(RADIO_STYLE);
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < examples.size(); i++) {
            RadioButton radio = new RadioButton(examples.get(i).title(), radioStyle);
            radio.setPosition(0, i);
            group.add(radio);
            content.addChild(radio);
            radios.add(radio);
        }
        group.setChecked(radios.get(currentIndex));
        TextButton loadButton = new TextButton(LOAD_BUTTON_TEXT,
                skin.getButtonStyle(BUTTON_STYLE));
        loadButton.setPosition(0, examples.size() + 1);
        loadButton.addClickListener(clicked -> loadChosen(group, radios));
        content.addChild(loadButton);
        content.pack();
        return content;
    }

    private void loadChosen(ButtonGroup group, List<RadioButton> radios) {
        int chosenIndex = radios.indexOf(group.getChecked());
        close();
        loadHandler.accept(examples.get(chosenIndex));
    }
}
