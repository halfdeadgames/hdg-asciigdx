package com.github.halfdeadgames.asciigdx.workbench.guides;

import com.github.halfdeadgames.asciigdx.workbench.ui.LabelField;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The rows of the guide list and which one is selected. Rows are the two warning settings first, then every guide
 * in file order, and are rebuilt after each edit. A warning row selects no guide. Edits work on a copy; the guides
 * the list was opened with stay available through {@link #original()}.
 */
final class GuideListModel {

    /** @param guide the guide this row stands for, or null for a warning setting */
    record Row(String text, PaintGuides.Listed guide) {
    }

    private static final int GROUP_ABBREVIATION = 3;
    private static final String NOT_SET = "NOT SET";
    private static final int WARNING_ROWS = 2;
    private static final int ACCENT_ROW = 0;
    private static final String RENAME_CURSOR = "_";

    private final LabelField label = new LabelField();

    private final PaintGuides original;
    private final int cellWidth;
    private final int cellHeight;
    private PaintGuides current;
    private List<Row> rows;
    private int selected;
    private int firstVisible;

    GuideListModel(PaintGuides guides, int cellWidth, int cellHeight) {
        this.original = guides;
        this.current = guides;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        rows = buildRows();
    }

    private List<Row> buildRows() {
        List<Row> built = new ArrayList<>();
        built.add(new Row("WARN ACCENT ROWS " + valueText(current.accentRows().isPresent(),
            current.accentRows().orElse(0)), null));
        built.add(new Row("WARN DESC FROM " + valueText(current.descenderFirstRow().isPresent(),
            current.descenderFirstRow().orElse(0)), null));
        for (PaintGuides.Listed guide : current.allGuides()) {
            built.add(new Row(guideText(guide), guide));
        }
        return List.copyOf(built);
    }

    /** The guides as edited so far. */
    PaintGuides guides() {
        return current;
    }

    /** The guides the list was opened with, untouched by any edit. */
    PaintGuides original() {
        return original;
    }

    /** Whether the edited guides would be written differently from the original ones. */
    boolean isChanged() {
        return !current.toText().equals(original.toText());
    }

    /** Changes the selected row's value by {@code delta}: a guide's position or a warning setting. */
    void adjust(int delta) {
        if (selected < WARNING_ROWS) {
            current = selected == ACCENT_ROW
                ? current.withAccentRowsMoved(delta, cellHeight)
                : current.withDescenderFirstRowMoved(delta, cellHeight);
        } else {
            current = current.withGuideMoved(selected - WARNING_ROWS, delta, cellWidth, cellHeight);
        }
        rows = buildRows();
    }

    /** Removes the selected guide, or turns the selected warning setting off. */
    void deleteSelected() {
        if (selected < WARNING_ROWS) {
            current = selected == ACCENT_ROW ? current.withoutAccentRows() : current.withoutDescenderFirstRow();
        } else {
            current = current.withoutGuide(selected - WARNING_ROWS);
        }
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

    private void selectAdded(int guideIndex) {
        rows = buildRows();
        select(WARNING_ROWS + guideIndex);
    }

    private static String valueText(boolean present, int value) {
        return present ? String.valueOf(value) : NOT_SET;
    }

    // Bars show first row plus row count, such as 6+1; groups are cut to three letters each.
    private static String guideText(PaintGuides.Listed guide) {
        return guideText(guide, guide.label());
    }

    private static String guideText(PaintGuides.Listed guide, String label) {
        StringBuilder text = new StringBuilder(guide.kind().name()).append(' ').append(label).append(' ')
            .append(guide.position());
        if (guide.kind() == PaintGuides.ListedKind.BAR) {
            text.append('+').append(guide.rowCount());
        }
        text.append(' ');
        String[] names = guide.groups().split(",");
        for (int i = 0; i < names.length; i++) {
            if (i > 0) {
                text.append(',');
            }
            String name = names[i].toUpperCase(Locale.ROOT);
            text.append(name.equals("ALL") ? name : name.substring(0, Math.min(GROUP_ABBREVIATION, name.length())));
        }
        return text.toString();
    }

    int size() {
        return rows.size();
    }

    /** While renaming, the selected row shows the typed text followed by a cursor. */
    Row row(int index) {
        Row row = rows.get(index);
        if (label.isActive() && index == selected) {
            return new Row(guideText(row.guide(), label.value() + RENAME_CURSOR), row.guide());
        }
        return row;
    }

    /** What happened to a request to switch a group on or off. */
    enum GroupToggle { TOGGLED, LAST_GROUP, NOT_A_GUIDE }

    /**
     * Switches a rules group (0 uppercase, 1 lowercase, 2 digits, 3 punctuation) on or off for the selected guide.
     * A guide keeps at least one group, so switching off the last one changes nothing. Warning rows and renaming
     * ignore it.
     */
    GroupToggle toggleGroup(int groupIndex) {
        if (selectedGuide() == null || label.isActive() || groupIndex < 0 || groupIndex >= PaintGuides.groupCount()) {
            return GroupToggle.NOT_A_GUIDE;
        }
        PaintGuides toggled = current.withGroupToggled(selected - WARNING_ROWS, groupIndex);
        if (toggled == current) {
            return GroupToggle.LAST_GROUP;
        }
        current = toggled;
        rows = buildRows();
        return GroupToggle.TOGGLED;
    }

    /** Starts renaming the selected guide from its current label; a warning row cannot be renamed. */
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
        current = current.withGuideRelabeled(selected - WARNING_ROWS, label.value());
        label.end();
        rows = buildRows();
    }

    /** Whether {@link #acceptRename} would apply: a rename is in progress and the typed label is not empty. */
    boolean canAcceptRename() {
        return label.isActive() && !label.isEmpty();
    }

    /** Ends the rename and keeps the old label. */
    void cancelRename() {
        label.end();
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

    /** The selected guide, or null when a warning row is selected. */
    PaintGuides.Listed selectedGuide() {
        return rows.get(selected).guide();
    }

    /** Index of the first row to draw when {@code visibleRows} fit; scrolls only as far as keeping the selection in view. */
    int firstVisible(int visibleRows) {
        int fit = Math.max(1, visibleRows);
        if (selected < firstVisible) {
            firstVisible = selected;
        } else if (selected >= firstVisible + fit) {
            firstVisible = selected - fit + 1;
        }
        firstVisible = Math.max(0, Math.min(firstVisible, Math.max(0, rows.size() - fit)));
        return firstVisible;
    }
}
