package com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.TextButton;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;

/** Lays out text buttons side by side, one cell apart. */
final class ButtonRow {
    private static final int GAP_COLUMNS = 1;
    private static final String BUTTON_STYLE = "button";

    private final Skin skin;
    private final WidgetGroup row = new WidgetGroup();
    private int nextColumn;

    ButtonRow(Skin skin) {
        this.skin = skin;
    }

    /** Adds a button that runs the action when clicked, and returns it. */
    TextButton add(String text, Runnable action) {
        TextButton button = new TextButton(text, skin.getButtonStyle(BUTTON_STYLE));
        button.setPosition(nextColumn, 0);
        button.addClickListener(clicked -> action.run());
        row.addChild(button);
        nextColumn += button.getWidth() + GAP_COLUMNS;
        return button;
    }

    /** Returns the group that holds the buttons added so far, sized to fit them. */
    WidgetGroup build() {
        row.pack();
        return row;
    }
}
