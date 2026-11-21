package com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase;

import com.github.halfdeadgames.asciigdx.ui.CheckBox;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import com.github.halfdeadgames.asciigdx.ui.WidgetGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Stacks demo rows for an example: a style-name caption on the left and the widget on the right.
 * Each row is as high as its widget, followed by one empty row.
 */
public final class DemoSheet {
    private static final int MARGIN = 2;
    private static final int GAP_ROWS = 1;
    private static final int CAPTION_COLUMNS = 26;
    private static final String CAPTION_STYLE = "text";
    private static final String HEADING_STYLE = "title";
    private static final String TOGGLE_STYLE = "checkBox";

    private final Skin skin;
    private final WidgetGroup content = new WidgetGroup();
    private final LabelStyle captionStyle;
    private final LabelStyle headingStyle;
    private final List<Consumer<Boolean>> disabledSetters = new ArrayList<>();
    private final BoundaryState boundaryState;
    private int nextRow = MARGIN;

    /**
     * Creates an empty sheet that takes its caption and heading looks from the skin and wraps its
     * demos in frames that follow the boundary state.
     */
    public DemoSheet(Skin skin, BoundaryState boundaryState) {
        this.skin = skin;
        this.boundaryState = boundaryState;
        captionStyle = skin.getLabelStyle(CAPTION_STYLE);
        headingStyle = skin.getLabelStyle(HEADING_STYLE);
    }

    /** Adds a heading row, after one empty row when the sheet already has rows. */
    public void addHeading(String text) {
        if (nextRow > MARGIN) {
            nextRow += GAP_ROWS;
        }
        Label heading = new Label(text, headingStyle);
        heading.setPosition(MARGIN, nextRow);
        content.addChild(heading);
        nextRow += stepFor(heading);
    }

    private static int stepFor(Widget widget) {
        int height = widget.getHeight() > 0 ? widget.getHeight() : widget.getPreferredHeight();
        return Math.max(1, height) + GAP_ROWS;
    }

    /** Adds a row with a caption on the left and the widget on the right. */
    public void addDemo(String caption, Widget widget) {
        Label captionLabel = new Label(caption, captionStyle);
        captionLabel.setPosition(MARGIN, nextRow + BoundaryFrame.RING_CELLS);
        content.addChild(captionLabel);
        addDemo(widget);
    }

    /** Adds a row with only the widget, for the follow-up rows of a group. */
    public void addDemo(Widget widget) {
        BoundaryFrame frame = new BoundaryFrame(widget, boundaryState);
        frame.setPosition(MARGIN + CAPTION_COLUMNS - BoundaryFrame.RING_CELLS, nextRow);
        content.addChild(frame);
        nextRow += stepFor(frame);
    }

    /**
     * Leaves empty rows, without a boundary frame, as many as a framed demo of that height takes.
     */
    public void addEmptyRows(int rows) {
        nextRow += rows + 2 * BoundaryFrame.RING_CELLS + GAP_ROWS;
    }

    /** Remembers a setter that the "Disabled" check box calls with its checked state. */
    public void trackDisabled(Consumer<Boolean> disabledSetter) {
        disabledSetters.add(disabledSetter);
    }

    /** Adds the "Disabled" check box that disables or enables every tracked demo widget. */
    public void addDisabledToggle() {
        CheckBox toggle = new CheckBox("Disabled", skin.getCheckBoxStyle(TOGGLE_STYLE));
        toggle.addChangeListener(source -> setAllDisabled(source.isChecked()));
        addDemo("live: disable the demos", toggle);
    }

    private void setAllDisabled(boolean disabled) {
        for (int i = 0; i < disabledSetters.size(); i++) {
            disabledSetters.get(i).accept(disabled);
        }
    }

    /** Returns the tree of all rows added so far, sized to fit them. */
    public Widget build() {
        content.pack();
        return content;
    }
}
