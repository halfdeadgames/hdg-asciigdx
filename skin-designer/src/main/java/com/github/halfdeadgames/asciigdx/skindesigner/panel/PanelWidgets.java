package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.github.halfdeadgames.asciigdx.skindesigner.panel.CommittedTextField.TextCommit;
import com.github.halfdeadgames.asciigdx.ui.CheckBox;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.TextButton;
import com.github.halfdeadgames.asciigdx.ui.TextField;
import com.github.halfdeadgames.asciigdx.ui.TextFieldFilter;
import java.util.List;
import java.util.function.Consumer;

/** Creates the widgets of the panel, all in the looks of the skin being edited. */
final class PanelWidgets {
    private static final String LABEL_STYLE = "text";
    private static final String BUTTON_STYLE = "button";
    private static final String CHECK_BOX_STYLE = "checkBox";
    private static final String TEXT_FIELD_STYLE = "default";
    private static final int MAX_FIELD_LENGTH = 16;

    private final Skin skin;
    private final FocusLossCommits focusCommits;

    PanelWidgets(Skin skin, FocusLossCommits focusCommits) {
        this.skin = skin;
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
     * Creates a button that shows one option and, when clicked, chooses the next option of the
     * list.
     *
     * @param shownText what the button says
     * @param currentOption the option now in effect, or text that is not in the list
     */
    TextButton createCycleButton(List<String> options, String currentOption, String shownText,
            Consumer<String> onChosen) {
        return createButton(shownText,
                () -> onChosen.accept(OptionCycle.next(options, currentOption)));
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
