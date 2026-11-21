package com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase;

import java.util.List;

/** The list of examples the showcase scene offers, in picker order. */
public final class WidgetExamples {
    private WidgetExamples() {
    }

    /** Returns a new list with one example per widget class; add a new example on a new line. */
    public static List<WidgetExample> createAll() {
        return List.of(
                new LabelExample(),
                new TextButtonExample(),
                new CheckBoxExample(),
                new RadioButtonExample(),
                new ProgressBarExample(),
                new SliderExample(),
                new TextFieldExample(),
                new TableExample(),
                new ScrollPaneExample(),
                new WindowExample(),
                new DialogExample(),
                new MenuBarExample(),
                new TooltipExample());
    }
}
