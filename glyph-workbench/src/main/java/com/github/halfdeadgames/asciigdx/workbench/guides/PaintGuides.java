package com.github.halfdeadgames.asciigdx.workbench.guides;

import com.github.halfdeadgames.asciigdx.workbench.rules.RulesFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The guide lines of one font size, read from {@code <size>.guides.txt}. One guide per line, whitespace separated;
 * blank lines and lines starting with {@code #} are skipped:
 * <pre>
 * row    label boundaryRow groups
 * column label boundaryColumn groups
 * bar    label firstRow rowCount groups
 * colbar label firstColumn columnCount groups
 * </pre>
 * {@code groups} is {@code all} or a comma list of group names from the rules file, kept as written. Labels contain
 * no spaces.
 */
public final class PaintGuides {

    /** A line across the grid at the top edge of {@code boundaryRow}. */
    public record Guide(String label, int boundaryRow) {
    }

    /** A vertical line at the left edge of {@code boundaryColumn}, drawn above or below by {@code tier}. */
    public record WidthGuide(String label, int boundaryColumn, int tier) {
    }

    /**
     * A bar marked through the middle of a band of rows: it shows a row to fill, not an edge.
     * The band is {@code rowCount} rows starting at {@code firstRow}.
     */
    public record BarGuide(String label, int firstRow, int rowCount) {

        /** Middle of the band in rows from the top of the grid. */
        public float centerRow() {
            return firstRow + rowCount / 2f;
        }
    }

    /**
     * A band of columns to fill, the sideways twin of {@link BarGuide}.
     * The band is {@code columnCount} columns starting at {@code firstColumn}. Its label sits above the grid on
     * {@code tier}, shared with the width guide labels.
     */
    public record ColumnBarGuide(String label, int firstColumn, int columnCount, int tier) {

        public ColumnBarGuide(String label, int firstColumn, int columnCount) {
            this(label, firstColumn, columnCount, 0);
        }

        /** Middle of the band in columns from the left of the grid. */
        public float centerColumn() {
            return firstColumn + columnCount / 2f;
        }
    }

    /** The guides to draw for one rules group in one cell size. */
    public record Shown(List<Guide> rows, List<WidthGuide> widths, List<BarGuide> bars,
                        List<ColumnBarGuide> columnBars) {

        /** Shown guides without column bars. */
        public Shown(List<Guide> rows, List<WidthGuide> widths, List<BarGuide> bars) {
            this(rows, widths, bars, List.of());
        }
    }

    record Scoped<T>(T guide, Set<String> groups) {

        boolean appliesTo(int codePoint, RulesFile rules) {
            return GuideGroups.showsOnGlyph(groups, codePoint, rules);
        }
    }

    /** The group names the keys 1-4 toggle, in key order. */
    static final List<String> KEY_TOGGLE_GROUPS = List.of("uppercase", "lowercase", "digits", "punctuation");

    public static final PaintGuides NONE = new PaintGuides(List.of(), List.of(), List.of(), List.of());

    private final List<Scoped<Guide>> rows;
    private final List<Scoped<WidthGuide>> columns;
    private final List<Scoped<BarGuide>> bars;
    private final List<Scoped<ColumnBarGuide>> columnBars;

    PaintGuides(List<Scoped<Guide>> rows, List<Scoped<WidthGuide>> columns, List<Scoped<BarGuide>> bars,
                List<Scoped<ColumnBarGuide>> columnBars) {
        this.rows = List.copyOf(rows);
        this.columns = List.copyOf(columns);
        this.bars = List.copyOf(bars);
        this.columnBars = List.copyOf(columnBars);
    }

    /** Guides without column bars. */
    PaintGuides(List<Scoped<Guide>> rows, List<Scoped<WidthGuide>> columns, List<Scoped<BarGuide>> bars) {
        this(rows, columns, bars, List.of());
    }

    private static final String NEW_LABEL = "NEW";

    private int guideTotal() {
        return rows.size() + columns.size() + bars.size() + columnBars.size();
    }

    private boolean isOutOfRange(int index) {
        return index < 0 || index >= guideTotal();
    }

    /** A copy with a row guide labelled NEW at position 0 for every group, last among the rows. */
    PaintGuides withRowAdded() {
        List<Scoped<Guide>> edited = new ArrayList<>(rows);
        edited.add(new Scoped<>(new Guide(NEW_LABEL, 0), GuideGroups.EVERY_GLYPH));
        return new PaintGuides(edited, columns, bars, columnBars);
    }

    /** A copy with a column guide labelled NEW at position 0 for every group, last among the columns. */
    PaintGuides withColumnAdded() {
        List<Scoped<WidthGuide>> edited = new ArrayList<>(columns);
        edited.add(new Scoped<>(new WidthGuide(NEW_LABEL, 0, 0), GuideGroups.EVERY_GLYPH));
        return new PaintGuides(rows, edited, bars, columnBars);
    }

    /** A copy with a one-row bar labelled NEW at row 0 for every group, last among the bars. */
    PaintGuides withBarAdded() {
        List<Scoped<BarGuide>> edited = new ArrayList<>(bars);
        edited.add(new Scoped<>(new BarGuide(NEW_LABEL, 0, 1), GuideGroups.EVERY_GLYPH));
        return new PaintGuides(rows, columns, edited, columnBars);
    }

    /**
     * A copy with a column bar labelled NEW for every group, last among the column bars. It starts centered and two
     * columns wide: first column {@code cellWidth / 2 - 1}, so 7-8 in a 16-wide cell and 3-4 in a 9-wide one
     * (one column left of the exact middle). A cell under 2 wide gets a one-column bar at column 0.
     */
    PaintGuides withColumnBarAdded(int cellWidth) {
        int columnCount = cellWidth < 2 ? 1 : 2;
        int firstColumn = cellWidth < 2 ? 0 : cellWidth / 2 - 1;
        List<Scoped<ColumnBarGuide>> edited = new ArrayList<>(columnBars);
        edited.add(new Scoped<>(new ColumnBarGuide(NEW_LABEL, firstColumn, columnCount), GuideGroups.EVERY_GLYPH));
        return new PaintGuides(rows, columns, bars, edited);
    }

    /** A copy without the guide at {@code index} of {@link #allGuides()}; an index out of range changes nothing. */
    PaintGuides withoutGuide(int index) {
        if (isOutOfRange(index)) {
            return this;
        }
        List<Scoped<Guide>> editedRows = new ArrayList<>(rows);
        List<Scoped<WidthGuide>> editedColumns = new ArrayList<>(columns);
        List<Scoped<BarGuide>> editedBars = new ArrayList<>(bars);
        List<Scoped<ColumnBarGuide>> editedColumnBars = new ArrayList<>(columnBars);
        if (index < rows.size()) {
            editedRows.remove(index);
        } else if (index < rows.size() + columns.size()) {
            editedColumns.remove(index - rows.size());
        } else if (index < rows.size() + columns.size() + bars.size()) {
            editedBars.remove(index - rows.size() - columns.size());
        } else {
            editedColumnBars.remove(index - rows.size() - columns.size() - bars.size());
        }
        return new PaintGuides(editedRows, editedColumns, editedBars, editedColumnBars);
    }

    /**
     * A copy with the position of the guide at {@code index} of {@link #allGuides()} moved by {@code delta}, kept
     * inside the cell: rows and bars to 0..cellHeight (a bar so it still ends within the cell), columns to
     * 0..cellWidth, column bars so the whole band stays within 0..cellWidth.
     */
    PaintGuides withGuideMoved(int index, int delta, int cellWidth, int cellHeight) {
        if (isOutOfRange(index)) {
            return this;
        }
        List<Scoped<Guide>> editedRows = new ArrayList<>(rows);
        List<Scoped<WidthGuide>> editedColumns = new ArrayList<>(columns);
        List<Scoped<BarGuide>> editedBars = new ArrayList<>(bars);
        List<Scoped<ColumnBarGuide>> editedColumnBars = new ArrayList<>(columnBars);
        if (index < rows.size()) {
            Scoped<Guide> row = rows.get(index);
            editedRows.set(index, new Scoped<>(new Guide(row.guide().label(),
                clamp(row.guide().boundaryRow() + delta, cellHeight)), row.groups()));
        } else if (index < rows.size() + columns.size()) {
            int at = index - rows.size();
            Scoped<WidthGuide> column = columns.get(at);
            editedColumns.set(at, new Scoped<>(new WidthGuide(column.guide().label(),
                clamp(column.guide().boundaryColumn() + delta, cellWidth), column.guide().tier()), column.groups()));
        } else if (index < rows.size() + columns.size() + bars.size()) {
            int at = index - rows.size() - columns.size();
            Scoped<BarGuide> bar = bars.get(at);
            int rowCount = bar.guide().rowCount();
            editedBars.set(at, new Scoped<>(new BarGuide(bar.guide().label(),
                clamp(bar.guide().firstRow() + delta, cellHeight - rowCount), rowCount), bar.groups()));
        } else {
            int at = index - rows.size() - columns.size() - bars.size();
            Scoped<ColumnBarGuide> columnBar = columnBars.get(at);
            int columnCount = columnBar.guide().columnCount();
            editedColumnBars.set(at, new Scoped<>(new ColumnBarGuide(columnBar.guide().label(),
                clamp(columnBar.guide().firstColumn() + delta, cellWidth - columnCount), columnCount),
                columnBar.groups()));
        }
        return new PaintGuides(editedRows, editedColumns, editedBars, editedColumnBars);
    }

    /**
     * A copy with the band of the bar or column bar at {@code index} of {@link #allGuides()} resized by
     * {@code delta}, its first row or column kept. The size stays at least 1 and the band inside the cell. Returns
     * this same object when nothing changes: a solid guide, an index out of range, or a size limit reached.
     */
    PaintGuides withGuideResized(int index, int delta, int cellWidth, int cellHeight) {
        if (isOutOfRange(index)) {
            return this;
        }
        int barStart = rows.size() + columns.size();
        int columnBarStart = barStart + bars.size();
        if (index >= columnBarStart) {
            return withColumnBarResized(index - columnBarStart, delta, cellWidth);
        }
        if (index >= barStart) {
            return withBarResized(index - barStart, delta, cellHeight);
        }
        return this;
    }

    private PaintGuides withBarResized(int at, int delta, int cellHeight) {
        Scoped<BarGuide> bar = bars.get(at);
        int resized = bar.guide().rowCount() + delta;
        if (resized < 1 || bar.guide().firstRow() + resized > cellHeight) {
            return this;
        }
        List<Scoped<BarGuide>> edited = new ArrayList<>(bars);
        edited.set(at, new Scoped<>(new BarGuide(bar.guide().label(), bar.guide().firstRow(), resized), bar.groups()));
        return new PaintGuides(rows, columns, edited, columnBars);
    }

    private PaintGuides withColumnBarResized(int at, int delta, int cellWidth) {
        Scoped<ColumnBarGuide> columnBar = columnBars.get(at);
        int resized = columnBar.guide().columnCount() + delta;
        if (resized < 1 || columnBar.guide().firstColumn() + resized > cellWidth) {
            return this;
        }
        List<Scoped<ColumnBarGuide>> edited = new ArrayList<>(columnBars);
        edited.set(at, new Scoped<>(new ColumnBarGuide(columnBar.guide().label(), columnBar.guide().firstColumn(),
            resized, columnBar.guide().tier()), columnBar.groups()));
        return new PaintGuides(rows, columns, bars, edited);
    }

    /** How many rules groups a guide can be set for. */
    static int groupCount() {
        return KEY_TOGGLE_GROUPS.size();
    }

    /**
     * A copy with one group switched on or off for the guide at {@code index} of {@link #allGuides()};
     * {@code groupIndex} counts uppercase, lowercase, digits, punctuation from 0. A guide for {@code all} is first
     * spelled out as the four names, and four names collapse back to {@code all}. Returns this same object when
     * nothing changes: an index out of range, or switching off the last remaining group.
     */
    PaintGuides withGroupToggled(int index, int groupIndex) {
        if (isOutOfRange(index) || groupIndex < 0 || groupIndex >= KEY_TOGGLE_GROUPS.size()) {
            return this;
        }
        Set<String> toggled = new LinkedHashSet<>(GuideGroups.spelledOut(groupsAt(index)));
        String group = KEY_TOGGLE_GROUPS.get(groupIndex);
        if (!toggled.remove(group)) {
            toggled.add(group);
        } else if (toggled.isEmpty()) {
            return this;
        }
        return withGuideEdited(index, null, GuideGroups.collapsed(toggled));
    }

    /** A copy with the label of the guide at {@code index} of {@link #allGuides()} replaced. */
    PaintGuides withGuideRelabeled(int index, String label) {
        if (isOutOfRange(index)) {
            return this;
        }
        return withGuideEdited(index, label, groupsAt(index));
    }

    private Set<String> groupsAt(int index) {
        if (index < rows.size()) {
            return rows.get(index).groups();
        }
        if (index < rows.size() + columns.size()) {
            return columns.get(index - rows.size()).groups();
        }
        if (index < rows.size() + columns.size() + bars.size()) {
            return bars.get(index - rows.size() - columns.size()).groups();
        }
        return columnBars.get(index - rows.size() - columns.size() - bars.size()).groups();
    }

    // A null label keeps the current one.
    private PaintGuides withGuideEdited(int index, String label, Set<String> groups) {
        List<Scoped<Guide>> editedRows = new ArrayList<>(rows);
        List<Scoped<WidthGuide>> editedColumns = new ArrayList<>(columns);
        List<Scoped<BarGuide>> editedBars = new ArrayList<>(bars);
        List<Scoped<ColumnBarGuide>> editedColumnBars = new ArrayList<>(columnBars);
        if (index < rows.size()) {
            Guide row = rows.get(index).guide();
            editedRows.set(index, new Scoped<>(
                new Guide(label != null ? label : row.label(), row.boundaryRow()), groups));
        } else if (index < rows.size() + columns.size()) {
            int at = index - rows.size();
            WidthGuide column = columns.get(at).guide();
            editedColumns.set(at, new Scoped<>(
                new WidthGuide(label != null ? label : column.label(), column.boundaryColumn(), column.tier()), groups));
        } else if (index < rows.size() + columns.size() + bars.size()) {
            int at = index - rows.size() - columns.size();
            BarGuide bar = bars.get(at).guide();
            editedBars.set(at, new Scoped<>(
                new BarGuide(label != null ? label : bar.label(), bar.firstRow(), bar.rowCount()), groups));
        } else {
            int at = index - rows.size() - columns.size() - bars.size();
            ColumnBarGuide columnBar = columnBars.get(at).guide();
            editedColumnBars.set(at, new Scoped<>(new ColumnBarGuide(label != null ? label : columnBar.label(),
                columnBar.firstColumn(), columnBar.columnCount()), groups));
        }
        return new PaintGuides(editedRows, editedColumns, editedBars, editedColumnBars);
    }

    private static int clamp(int value, int maximum) {
        return Math.max(0, Math.min(Math.max(0, maximum), value));
    }

    int rowCount() {
        return rows.size();
    }

    int columnCount() {
        return columns.size();
    }

    int barCount() {
        return bars.size();
    }

    int columnBarCount() {
        return columnBars.size();
    }

    /** Reads the file text. Throws {@link IllegalArgumentException} naming the line on a bad line. */
    static PaintGuides parse(String text) {
        return PaintGuidesText.parse(text);
    }

    /**
     * The file text that {@link #parse} reads back as the same guides: rows, columns, bars, and column bars, each kind in its
     * stored order. Comments and blank lines are not kept.
     */
    String toText() {
        return PaintGuidesText.toText(this);
    }

    public enum ListedKind { ROW, COLUMN, BAR, COLUMN_BAR }

    /**
     * One guide as the guide list shows it, whatever its kind.
     *
     * @param position boundary row, boundary column, first row of a bar, or first column of a column bar
     * @param rowCount the band size: rows in a bar, columns in a column bar, 0 for the other kinds
     * @param groups the groups text as {@link #toText} writes it, such as {@code all} or {@code lowercase,digits}
     */
    public record Listed(ListedKind kind, String label, int position, int rowCount, String groups) {

        /** Middle of a bar's band in rows from the top of the grid. */
        public float centerRow() {
            return position + rowCount / 2f;
        }

        /** Middle of a column bar's band in columns from the left of the grid. */
        public float centerColumn() {
            return position + rowCount / 2f;
        }
    }

    /** Every guide, group-filtered or not, in the order {@link #toText} writes them: rows, columns, then bars. */
    List<Listed> allGuides() {
        List<Listed> all = new ArrayList<>();
        for (Scoped<Guide> row : rows) {
            all.add(new Listed(ListedKind.ROW, row.guide().label(), row.guide().boundaryRow(), 0,
                GuideGroups.text(row.groups())));
        }
        for (Scoped<WidthGuide> column : columns) {
            all.add(new Listed(ListedKind.COLUMN, column.guide().label(), column.guide().boundaryColumn(), 0,
                GuideGroups.text(column.groups())));
        }
        for (Scoped<BarGuide> bar : bars) {
            all.add(new Listed(ListedKind.BAR, bar.guide().label(), bar.guide().firstRow(), bar.guide().rowCount(),
                GuideGroups.text(bar.groups())));
        }
        for (Scoped<ColumnBarGuide> columnBar : columnBars) {
            all.add(new Listed(ListedKind.COLUMN_BAR, columnBar.guide().label(), columnBar.guide().firstColumn(),
                columnBar.guide().columnCount(), GuideGroups.text(columnBar.groups())));
        }
        return List.copyOf(all);
    }

    /** Whether there are no drawing guides. */
    boolean isEmpty() {
        return guideTotal() == 0;
    }

    /**
     * The guides that apply to a glyph (see {@link GuideGroups#showsOnGlyph}) and fit the cell. Width guides are
     * sorted by column. Width guides and column bars together are ordered by position and alternate label tiers
     * 0, 1, 0, ... so neighbouring labels never overlap.
     */
    public Shown forGlyph(int codePoint, RulesFile rules, int cellWidth, int cellHeight) {
        List<Guide> shownRows = fittingRows(codePoint, rules, cellHeight);
        LabelTiers.Tiered tiered = LabelTiers.assign(fittingColumns(codePoint, rules, cellWidth),
            fittingColumnBars(codePoint, rules, cellWidth));
        List<BarGuide> shownBars = fittingBars(codePoint, rules, cellHeight);
        return new Shown(List.copyOf(shownRows), tiered.widths(), List.copyOf(shownBars), tiered.columnBars());
    }

    private List<ColumnBarGuide> fittingColumnBars(int codePoint, RulesFile rules, int cellWidth) {
        List<ColumnBarGuide> shownColumnBars = new ArrayList<>();
        for (Scoped<ColumnBarGuide> columnBar : columnBars) {
            if (columnBar.appliesTo(codePoint, rules)
                && columnBar.guide().firstColumn() + columnBar.guide().columnCount() <= cellWidth) {
                shownColumnBars.add(columnBar.guide());
            }
        }
        return shownColumnBars;
    }

    private List<Guide> fittingRows(int codePoint, RulesFile rules, int cellHeight) {
        List<Guide> shownRows = new ArrayList<>();
        for (Scoped<Guide> row : rows) {
            if (row.appliesTo(codePoint, rules) && row.guide().boundaryRow() <= cellHeight) {
                shownRows.add(row.guide());
            }
        }
        return shownRows;
    }

    private List<WidthGuide> fittingColumns(int codePoint, RulesFile rules, int cellWidth) {
        List<WidthGuide> fitting = new ArrayList<>();
        for (Scoped<WidthGuide> column : columns) {
            if (column.appliesTo(codePoint, rules) && column.guide().boundaryColumn() <= cellWidth) {
                fitting.add(column.guide());
            }
        }
        fitting.sort(Comparator.comparingInt(WidthGuide::boundaryColumn));
        return fitting;
    }

    private List<BarGuide> fittingBars(int codePoint, RulesFile rules, int cellHeight) {
        List<BarGuide> shownBars = new ArrayList<>();
        for (Scoped<BarGuide> bar : bars) {
            if (bar.appliesTo(codePoint, rules) && bar.guide().firstRow() + bar.guide().rowCount() <= cellHeight) {
                shownBars.add(bar.guide());
            }
        }
        return shownBars;
    }
}
