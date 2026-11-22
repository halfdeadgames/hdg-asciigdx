package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.github.halfdeadgames.asciigdx.skindesigner.panel.CommittedTextField.TextCommit;
import com.github.halfdeadgames.asciigdx.ui.ButtonStyle;
import com.github.halfdeadgames.asciigdx.ui.CheckBox;
import com.github.halfdeadgames.asciigdx.ui.ItemSwatch;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.ScrollPaneStyle;
import com.github.halfdeadgames.asciigdx.ui.SelectBox;
import com.github.halfdeadgames.asciigdx.ui.SelectBoxStyle;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.TextButton;
import com.github.halfdeadgames.asciigdx.ui.TextField;
import com.github.halfdeadgames.asciigdx.ui.TextFieldFilter;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import java.util.List;
import java.util.function.Consumer;

/** Creates the widgets of the panel, all in the looks of the skin being edited. */
final class PanelWidgets {
    private static final String LABEL_STYLE = "text";
    private static final String BUTTON_STYLE = "button";
    private static final String CHECK_BOX_STYLE = "checkBox";
    private static final String TEXT_FIELD_STYLE = "default";
    private static final String SELECT_BOX_STYLE = "selectBox";
    private static final String SCROLL_PANE_STYLE = "scrollPane";
    private static final int MAX_FIELD_LENGTH = 16;
    private static final int NO_SWATCH_RGBA = 0;

    private final Skin skin;
    private final UiRoot uiRoot;
    private final FocusLossCommits focusCommits;

    PanelWidgets(Skin skin, UiRoot uiRoot, FocusLossCommits focusCommits) {
        this.skin = skin;
        this.uiRoot = uiRoot;
        this.focusCommits = focusCommits;
    }

    Skin getSkin() {
        return skin;
    }

    Label createLabel(String text) {
        Label label = new Label(text, skin.getLabelStyle(LABEL_STYLE));
        label.setMarkupEnabled(false);
        label.pack();
        return label;
    }

    TextButton createButton(String text, Runnable onClick) {
        TextButton button = new TextButton(text, skin.getButtonStyle(BUTTON_STYLE));
        button.addClickListener(clicked -> onClick.run());
        return button;
    }

    /**
     * Creates a drop-down of options. When the current value is not one of them, nothing is
     * selected and the box shows the value as its placeholder.
     */
    SelectBox<String> createChoiceBox(List<String> options, String currentValue,
            Consumer<String> onChosen) {
        return createBox(options, currentValue, null, onChosen);
    }

    /** Creates a drop-down of palette color names, each with a swatch of its color. */
    SelectBox<String> createColorChoiceBox(List<String> paletteNames, String currentValue,
            Consumer<String> onChosen) {
        return createBox(paletteNames, currentValue, this::paletteColorOf, onChosen);
    }

    private SelectBox<String> createBox(List<String> options, String currentValue,
            ItemSwatch<String> swatch, Consumer<String> onChosen) {
        SelectBox<String> box = SelectBox.of(uiRoot, selectBoxStyle());
        box.setItems(options);
        box.setItemSwatch(swatch);
        showCurrentValue(box, currentValue);
        box.addChangeListener(source -> onChosen.accept(source.getSelected()));
        box.pack();
        return box;
    }

    // A skin being edited may have no selectBoxes section, so the panel builds a plain style from
    // the looks it already uses.
    private SelectBoxStyle selectBoxStyle() {
        if (skin.hasSelectBoxStyle(SELECT_BOX_STYLE)) {
            return skin.getSelectBoxStyle(SELECT_BOX_STYLE);
        }
        ButtonStyle buttonStyle = skin.getButtonStyle(BUTTON_STYLE);
        return SelectBoxStyle.of(buttonStyle, buttonStyle, fallbackScrollPaneStyle(buttonStyle));
    }

    private ScrollPaneStyle fallbackScrollPaneStyle(ButtonStyle buttonStyle) {
        if (skin.hasScrollPaneStyle(SCROLL_PANE_STYLE)) {
            return skin.getScrollPaneStyle(SCROLL_PANE_STYLE);
        }
        LabelStyle look = buttonStyle.getNormal();
        return ScrollPaneStyle.of(look.getBackgroundRgba(), look.getForegroundRgba());
    }

    int paletteColorOf(String paletteName) {
        return skin.hasColor(paletteName) ? skin.getRgba(paletteName) : NO_SWATCH_RGBA;
    }

    private static void showCurrentValue(SelectBox<String> box, String currentValue) {
        if (box.getItems().contains(currentValue)) {
            box.setSelected(currentValue);
            return;
        }
        box.setPlaceholder(currentValue);
    }

    CheckBox createCheckBox(String text, boolean checked, Consumer<Boolean> onChanged) {
        CheckBox checkBox = new CheckBox(text, skin.getCheckBoxStyle(CHECK_BOX_STYLE));
        checkBox.setChecked(checked);
        checkBox.addChangeListener(source -> onChanged.accept(source.isChecked()));
        return checkBox;
    }

    TextField createCommittedField(String focusKey, int widthCells, String initialText,
            TextFieldFilter filter, TextCommit commit) {
        TextField field = new TextField(skin.getTextFieldStyle(TEXT_FIELD_STYLE), widthCells);
        field.setFilter(filter);
        field.setMaxLength(MAX_FIELD_LENGTH);
        CommittedTextField committed = new CommittedTextField(field, focusKey, initialText,
                commit);
        focusCommits.watch(committed);
        return field;
    }
}
