package com.github.halfdeadgames.asciigdx.workbench.guides;

import com.github.halfdeadgames.asciigdx.workbench.ui.LabelField;
import java.util.ArrayList;
import java.util.List;

/**
 * The rows of the guide list and which one is selected. Row i is guide i in file order, and rows are rebuilt after
 * each edit. Edits work on a copy; the guides the list was opened with stay available through {@link #original()}.
 */
final class GuideListModel {

    /** @param guide the guide this row stands for */
    record Row(String text, PaintGuides.Listed guide) {
    }

    private static final String RENAME_CURSOR = "_";
    private static final String HIDDEN_CURSOR = " ";
    private static final String EMPTY_LABEL_PLACEHOLDER = "_";

    private final LabelField label = new LabelField();

    private final PaintGuides original;
    private final int cellWidth;
    private final int cellHeight;
    private PaintGuides current;
    private List<Row> rows;
    private int selected;
    private int firstVisible;
    private boolean adding;
    private PaintGuides guidesBeforeAdd;
    private int selectionBeforeAdd;

    GuideListModel(PaintGuides guides, int cellWidth, int cellHeight) {
        this.original = guides;
        this.current = guides;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        rows = buildRows();
    }

    private List<Row> buildRows() {
        List<Row> built = new ArrayList<>();
        for (PaintGuides.Listed guide : current.allGuides()) {
            built.add(new Row(guideText(guide), guide));
        }
        return List.copyOf(built);
    }

    /** The guides as edited so far. */
    PaintGuides guides() {
        return current;
    }

    /** The guides as the grid should show them: while a label is typed, the selected guide carries the typed text. */
    PaintGuides guidesForDisplay() {
        if (!label.isActive()) {
            return current;
        }
        return current.withGuideRelabeled(selected, label.isEmpty() ? EMPTY_LABEL_PLACEHOLDER : label.value());
    }

    /** The selected guide with the typed text as its label while renaming; null when the list is empty. */
    PaintGuides.Listed selectedGuideForDisplay() {
        if (rows.isEmpty()) {
            return null;
        }
        return guidesForDisplay().allGuides().get(selected);
    }

    /** The guides the list was opened with, untouched by any edit. */
    PaintGuides original() {
        return original;
    }

    /** Whether the edited guides would be written differently from the original ones. */
    boolean isChanged() {
        return !current.toText().equals(original.toText());
    }

    /** Moves the selected guide's position by {@code delta}. */
    void adjust(int delta) {
        current = current.withGuideMoved(selected, delta, cellWidth, cellHeight);
        rows = buildRows();
    }

    /** Whether the selected guide is a dotted band, which is the only kind that has a size. */
    boolean canResizeSelected() {
        PaintGuides.Listed guide = selectedGuide();
        return guide != null
            && (guide.kind() == PaintGuides.ListedKind.BAR || guide.kind() == PaintGuides.ListedKind.COLUMN_BAR);
    }

    /** Grows or shrinks the selected dotted band by {@code delta}; at a size limit or on a solid guide nothing changes. */
    void resizeSelected(int delta) {
        current = current.withGuideResized(selected, delta, cellWidth, cellHeight);
        rows = buildRows();
    }

    /** Removes the selected guide. */
    void deleteSelected() {
        current = current.withoutGuide(selected);
        rows = buildRows();
        select(selected);
    }

    /** Adds a row guide after the last row guide and selects it. */
    void addRow() {
        current = current.withRowAdded();
        selectAdded(current.rowCount() - 1);
    }

    /** Adds a column guide after the last column guide and selects it. */
    void addColumn() {
        current = current.withColumnAdded();
        selectAdded(current.rowCount() + current.columnCount() - 1);
    }

    /** Adds a one-row bar after the last bar and selects it. */
    void addBar() {
        current = current.withBarAdded();
        selectAdded(current.rowCount() + current.columnCount() + current.barCount() - 1);
    }

    /** Adds a centered two-column bar after the last column bar and selects it. */
    void addColumnBar() {
        current = current.withColumnBarAdded(cellWidth);
        selectAdded(current.rowCount() + current.columnCount() + current.barCount() + current.columnBarCount() - 1);
    }

    private void selectAdded(int guideIndex) {
        rows = buildRows();
        select(guideIndex);
    }

    // Bars show their first row plus the row count, and column bars their first column plus the column count,
    // for example 6+1.
    private static String guideText(PaintGuides.Listed guide) {
        return guideText(guide, guide.label());
    }

    private static String guideText(PaintGuides.Listed guide, String label) {
        StringBuilder text = new StringBuilder(kindWord(guide.kind())).append(' ').append(label).append(' ')
            .append(guide.position());
        if (guide.kind() == PaintGuides.ListedKind.BAR || guide.kind() == PaintGuides.ListedKind.COLUMN_BAR) {
            text.append('+').append(guide.rowCount());
        }
        return text.toString();
    }

    private static String kindWord(PaintGuides.ListedKind kind) {
        return kind == PaintGuides.ListedKind.COLUMN_BAR ? "COLBAR" : kind.name();
    }

    int size() {
        return rows.size();
    }

    /** While renaming, the selected row shows the typed text followed by a cursor. */
    Row row(int index) {
        return row(index, true);
    }

    /** A hidden cursor leaves a blank, so the row keeps its length. */
    Row row(int index, boolean cursorVisible) {
        Row row = rows.get(index);
        if (label.isActive() && index == selected) {
            String cursor = cursorVisible ? RENAME_CURSOR : HIDDEN_CURSOR;
            return new Row(guideText(row.guide(), label.value() + cursor), row.guide());
        }
        return row;
    }

    /**
     * Switches a group name on or off for the selected guide, storing its names in {@code definedGroups} order; an
     * empty list and renaming ignore it.
     */
    void toggleGroup(String groupName, List<String> definedGroups) {
        if (selectedGuide() == null || label.isActive()) {
            return;
        }
        current = current.withGroupToggled(selected, groupName, definedGroups);
        rows = buildRows();
    }

    /** Goes back to an earlier state of the edited guides, such as the one taken when the group list opened. */
    void restoreGuides(PaintGuides snapshot) {
        current = snapshot;
        rows = buildRows();
    }

    /** Starts renaming the selected guide from its current label; does nothing when the list is empty. */
    void beginRename() {
        if (!canRename()) {
            return;
        }
        label.begin(selectedGuide().label());
    }

    /** Whether {@link #beginRename} would start: a guide is selected and no rename is in progress. */
    boolean canRename() {
        return selectedGuide() != null && !label.isActive();
    }

    boolean isRenaming() {
        return label.isActive();
    }

    /** The text typed so far while renaming. */
    String renameText() {
        return label.value();
    }

    /** Adds a typed character to the label; ignored when not renaming. */
    void typeChar(char character) {
        label.type(character);
    }

    void backspace() {
        label.backspace();
    }

    /** Applies the typed label. An empty label is refused and the rename stays open. */
    void acceptRename() {
        if (!canAcceptRename()) {
            return;
        }
        current = current.withGuideRelabeled(selected, label.value());
        label.end();
        adding = false;
        rows = buildRows();
    }

    /** Whether {@link #acceptRename} would apply: a rename is in progress and the typed label is not empty. */
    boolean canAcceptRename() {
        return label.isActive() && !label.isEmpty();
    }

    /** Ends the rename and keeps the old label; a guide that was being added is dropped and the old selection returns. */
    void cancelRename() {
        label.end();
        if (!adding) {
            return;
        }
        adding = false;
        current = guidesBeforeAdd;
        rows = buildRows();
        select(selectionBeforeAdd);
    }

    /** Runs {@code add}, which must add one guide and select it, then asks for its label starting empty. */
    void beginAdding(Runnable add) {
        guidesBeforeAdd = current;
        selectionBeforeAdd = selected;
        add.run();
        label.begin("");
        adding = true;
    }

    int selectedIndex() {
        return selected;
    }

    /** Selects a row; an index outside the list is moved to the nearest end. */
    void select(int index) {
        selected = Math.max(0, Math.min(rows.size() - 1, index));
    }

    /** Moves the selection and stops at the first and last rows. */
    void move(int delta) {
        select(selected + delta);
    }

    /** The selected guide, or null when the list is empty. */
    PaintGuides.Listed selectedGuide() {
        if (rows.isEmpty()) {
            return null;
        }
        return rows.get(selected).guide();
    }

    /** Index of the first row to draw when {@code visibleRows} fit; scrolls only as far as keeping the selection in view. */
    int firstVisible(int visibleRows) {
        firstVisible = GuideListRows.scrolledFirst(firstVisible, selected, rows.size(), visibleRows);
        return firstVisible;
    }
}
