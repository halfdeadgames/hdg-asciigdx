package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.github.halfdeadgames.asciigdx.ui.ScrollPane;
import com.github.halfdeadgames.asciigdx.ui.SelectBox;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.Table;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.List;

/**
 * The docked side panel: value editors for the styles of the current example, in a scroll pane
 * {@value #COLUMNS} columns wide. Build one per UI build and call {@link #dispose()} before
 * dropping it.
 */
public final class SkinEditorPanel {
    /** Width of the panel in square cells. */
    public static final int COLUMNS = 40;

    private static final String SCROLL_PANE_STYLE = "scrollPane";
    private static final String CHOOSER_HEADING = "Style:";
    private static final String NO_STYLES_TEXT = "This example uses no styles you can edit.";
    private static final int WHEEL_STEP_CELLS = 3;
    private static final int PADDING_CELLS = 1;

    private final FocusLossCommits focusCommits;
    private final SkinEditorMemory memory;
    private final ScrollPane view;

    private SkinEditorPanel(FocusLossCommits focusCommits, SkinEditorMemory memory,
            ScrollPane view) {
        this.focusCommits = focusCommits;
        this.memory = memory;
        this.view = view;
    }

    /**
     * Builds the panel from the document's current tree.
     *
     * @param sections the skin sections whose styles the panel offers
     * @param memory the state kept between panels of one session
     * @param rows height of the panel in square cells
     */
    public static SkinEditorPanel create(PanelHost host, List<String> sections,
            SkinEditorMemory memory, int rows) {
        Skin skin = host.document().getSkin();
        FocusLossCommits focusCommits = new FocusLossCommits(host.uiRoot());
        PanelWidgets widgets = new PanelWidgets(skin, host.uiRoot(), focusCommits);
        Widget content = createContent(host, widgets, sections, memory);
        ScrollPane view = new ScrollPane(content, skin.getScrollPaneStyle(SCROLL_PANE_STYLE));
        view.setSize(COLUMNS, rows);
        view.setWheelStepCells(WHEEL_STEP_CELLS);
        view.setScrollRow(memory.getScrollRow());
        focusCommits.start();
        return new SkinEditorPanel(focusCommits, memory, view);
    }

    private static Widget createContent(PanelHost host, PanelWidgets widgets,
            List<String> sections, SkinEditorMemory memory) {
        Table content = new Table();
        content.setPadding(PADDING_CELLS);
        var tree = host.document().getTree();
        List<StyleChoice> choices = StyleChoice.listIn(tree, sections);
        if (choices.isEmpty()) {
            content.add(widgets.createLabel(NO_STYLES_TEXT));
            content.pack();
            return content;
        }
        StyleChoice selected = memory.pickFrom(choices);
        content.add(widgets.createLabel(CHOOSER_HEADING));
        content.row();
        content.add(createChooser(host, widgets, choices, selected, memory));
        content.row();
        EditorContext context = new EditorContext(widgets, tree,
                new EditGateway(host.document(), host.statusSink()));
        content.add(new StyleEditorTable(context).create(selected)).padTop(1);
        content.pack();
        return content;
    }

    private static SelectBox<String> createChooser(PanelHost host, PanelWidgets widgets,
            List<StyleChoice> choices, StyleChoice selected, SkinEditorMemory memory) {
        List<String> names = choices.stream().map(StyleChoice::displayName).toList();
        return widgets.createChoiceBox(names, selected.displayName(), chosenName -> {
            memory.rememberSelection(choices.get(names.indexOf(chosenName)));
            memory.requestScrollReset();
            host.styleChosen().run();
        });
    }

    /** Returns the widget to add to the root, at the right edge of the example area. */
    public Widget getView() {
        return view;
    }

    /**
     * Remembers the scroll position and the focused field, so the next panel starts where this
     * one was.
     */
    public void saveViewState() {
        memory.rememberScrollRow(view.getScrollRow());
        memory.rememberFocusedField(focusCommits.findFocusedKey());
    }

    /** Commits every field whose text was typed but not yet committed. */
    public void flushEdits() {
        focusCommits.commitAllChanged();
    }

    /**
     * Gives focus back to the field that had it before the last rebuild. Call it after the view
     * is in the root.
     */
    public void restoreFocus() {
        String focusKey = memory.takeFocusedField();
        if (focusKey != null) {
            focusCommits.focusFieldWithKey(focusKey);
        }
    }

    /** Stops the panel from watching the root. Call it before the panel is dropped. */
    public void dispose() {
        focusCommits.stop();
    }
}
