package com.github.halfdeadgames.asciigdx.workbench.guides;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontRules;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;

/**
 * The guide lines of one font size, read from {@code <size>.guides.txt}. One guide per line, whitespace separated;
 * blank lines and lines starting with {@code #} are skipped:
 * <pre>
 * row    label boundaryRow groups
 * column label boundaryColumn groups
 * bar    label firstRow rowCount groups
 * warn   accent-rows count
 * warn   descender-from row
 * </pre>
 * The two {@code warn} lines are optional, each at most once; a check without its line is off.
 * {@code groups} is {@code all} or a comma list of rules group names (case-insensitive). Labels contain no spaces.
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

    /** The guides to draw for one rules group in one cell size. */
    public record Shown(List<Guide> rows, List<WidthGuide> widths, List<BarGuide> bars) {
    }

    record Scoped<T>(T guide, Set<String> groups) {

        boolean appliesTo(String groupName) {
            return groups.contains(groupName);
        }
    }

    static final List<String> ALL_GROUPS = List.of(
        FontRules.UPPERCASE, FontRules.LOWERCASE, FontRules.DIGITS, FontRules.PUNCTUATION);

    /** The optional {@code warn} settings; an empty value means the file leaves that check off. */
    static final class WarnSettings {
        OptionalInt accentRows = OptionalInt.empty();
        OptionalInt descenderFirstRow = OptionalInt.empty();
    }

    public static final PaintGuides NONE = new PaintGuides(List.of(), List.of(), List.of(), new WarnSettings());

    private final List<Scoped<Guide>> rows;
    private final List<Scoped<WidthGuide>> columns;
    private final List<Scoped<BarGuide>> bars;
    private final OptionalInt accentRows;
    private final OptionalInt descenderFirstRow;

    PaintGuides(List<Scoped<Guide>> rows, List<Scoped<WidthGuide>> columns, List<Scoped<BarGuide>> bars,
                WarnSettings warnSettings) {
        this(rows, columns, bars, warnSettings.accentRows, warnSettings.descenderFirstRow);
    }

    private PaintGuides(List<Scoped<Guide>> rows, List<Scoped<WidthGuide>> columns, List<Scoped<BarGuide>> bars,
                        OptionalInt accentRows, OptionalInt descenderFirstRow) {
        this.rows = List.copyOf(rows);
        this.columns = List.copyOf(columns);
        this.bars = List.copyOf(bars);
        this.accentRows = accentRows;
        this.descenderFirstRow = descenderFirstRow;
    }

    private static final String NEW_LABEL = "NEW";

    /** A copy with a row guide labelled NEW at position 0 for every group, last among the rows. */
    PaintGuides withRowAdded() {
        List<Scoped<Guide>> edited = new ArrayList<>(rows);
        edited.add(new Scoped<>(new Guide(NEW_LABEL, 0), Set.copyOf(ALL_GROUPS)));
        return new PaintGuides(edited, columns, bars, accentRows, descenderFirstRow);
    }

    /** A copy with a column guide labelled NEW at position 0 for every group, last among the columns. */
    PaintGuides withColumnAdded() {
        List<Scoped<WidthGuide>> edited = new ArrayList<>(columns);
        edited.add(new Scoped<>(new WidthGuide(NEW_LABEL, 0, 0), Set.copyOf(ALL_GROUPS)));
        return new PaintGuides(rows, edited, bars, accentRows, descenderFirstRow);
    }

    /** A copy with a one-row bar labelled NEW at row 0 for every group, last among the bars. */
    PaintGuides withBarAdded() {
        List<Scoped<BarGuide>> edited = new ArrayList<>(bars);
        edited.add(new Scoped<>(new BarGuide(NEW_LABEL, 0, 1), Set.copyOf(ALL_GROUPS)));
        return new PaintGuides(rows, columns, edited, accentRows, descenderFirstRow);
    }

    /** A copy without the guide at {@code index} of {@link #allGuides()}; an index out of range changes nothing. */
    PaintGuides withoutGuide(int index) {
        if (index < 0 || index >= rows.size() + columns.size() + bars.size()) {
            return this;
        }
        List<Scoped<Guide>> editedRows = new ArrayList<>(rows);
        List<Scoped<WidthGuide>> editedColumns = new ArrayList<>(columns);
        List<Scoped<BarGuide>> editedBars = new ArrayList<>(bars);
        if (index < rows.size()) {
            editedRows.remove(index);
        } else if (index < rows.size() + columns.size()) {
            editedColumns.remove(index - rows.size());
        } else {
            editedBars.remove(index - rows.size() - columns.size());
        }
        return new PaintGuides(editedRows, editedColumns, editedBars, accentRows, descenderFirstRow);
    }

    /**
     * A copy with the position of the guide at {@code index} of {@link #allGuides()} moved by {@code delta}, kept
     * inside the cell: rows and bars to 0..cellHeight (a bar so it still ends within the cell), columns to
     * 0..cellWidth.
     */
    PaintGuides withGuideMoved(int index, int delta, int cellWidth, int cellHeight) {
        if (index < 0 || index >= rows.size() + columns.size() + bars.size()) {
            return this;
        }
        List<Scoped<Guide>> editedRows = new ArrayList<>(rows);
        List<Scoped<WidthGuide>> editedColumns = new ArrayList<>(columns);
        List<Scoped<BarGuide>> editedBars = new ArrayList<>(bars);
        if (index < rows.size()) {
            Scoped<Guide> row = rows.get(index);
            editedRows.set(index, new Scoped<>(new Guide(row.guide().label(),
                clamp(row.guide().boundaryRow() + delta, cellHeight)), row.groups()));
        } else if (index < rows.size() + columns.size()) {
            int at = index - rows.size();
            Scoped<WidthGuide> column = columns.get(at);
            editedColumns.set(at, new Scoped<>(new WidthGuide(column.guide().label(),
                clamp(column.guide().boundaryColumn() + delta, cellWidth), column.guide().tier()), column.groups()));
        } else {
            int at = index - rows.size() - columns.size();
            Scoped<BarGuide> bar = bars.get(at);
            int rowCount = bar.guide().rowCount();
            editedBars.set(at, new Scoped<>(new BarGuide(bar.guide().label(),
                clamp(bar.guide().firstRow() + delta, cellHeight - rowCount), rowCount), bar.groups()));
        }
        return new PaintGuides(editedRows, editedColumns, editedBars, accentRows, descenderFirstRow);
    }

    /** How many rules groups a guide can be set for. */
    static int groupCount() {
        return ALL_GROUPS.size();
    }

    /**
     * A copy with one rules group switched on or off for the guide at {@code index} of {@link #allGuides()};
     * {@code groupIndex} counts uppercase, lowercase, digits, punctuation from 0. Returns this same object when
     * nothing changes: an index out of range, or switching off the last remaining group.
     */
    PaintGuides withGroupToggled(int index, int groupIndex) {
        if (index < 0 || index >= rows.size() + columns.size() + bars.size()
            || groupIndex < 0 || groupIndex >= ALL_GROUPS.size()) {
            return this;
        }
        Set<String> toggled = new LinkedHashSet<>(groupsAt(index));
        String group = ALL_GROUPS.get(groupIndex);
        if (!toggled.remove(group)) {
            toggled.add(group);
        } else if (toggled.isEmpty()) {
            return this;
        }
        return withGuideEdited(index, null, toggled);
    }

    /** A copy with the label of the guide at {@code index} of {@link #allGuides()} replaced. */
    PaintGuides withGuideRelabeled(int index, String label) {
        if (index < 0 || index >= rows.size() + columns.size() + bars.size()) {
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
        return bars.get(index - rows.size() - columns.size()).groups();
    }

    // A null label keeps the current one.
    private PaintGuides withGuideEdited(int index, String label, Set<String> groups) {
        List<Scoped<Guide>> editedRows = new ArrayList<>(rows);
        List<Scoped<WidthGuide>> editedColumns = new ArrayList<>(columns);
        List<Scoped<BarGuide>> editedBars = new ArrayList<>(bars);
        if (index < rows.size()) {
            Guide row = rows.get(index).guide();
            editedRows.set(index, new Scoped<>(
                new Guide(label != null ? label : row.label(), row.boundaryRow()), groups));
        } else if (index < rows.size() + columns.size()) {
            int at = index - rows.size();
            WidthGuide column = columns.get(at).guide();
            editedColumns.set(at, new Scoped<>(
                new WidthGuide(label != null ? label : column.label(), column.boundaryColumn(), column.tier()), groups));
        } else {
            int at = index - rows.size() - columns.size();
            BarGuide bar = bars.get(at).guide();
            editedBars.set(at, new Scoped<>(
                new BarGuide(label != null ? label : bar.label(), bar.firstRow(), bar.rowCount()), groups));
        }
        return new PaintGuides(editedRows, editedColumns, editedBars, accentRows, descenderFirstRow);
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

    /** A copy with the accent-rows setting changed by {@code delta} within 0..cellHeight; an unset one becomes 0 on a rise and stays unset on a fall. */
    PaintGuides withAccentRowsMoved(int delta, int cellHeight) {
        return new PaintGuides(rows, columns, bars, movedSetting(accentRows, delta, cellHeight), descenderFirstRow);
    }

    /** A copy with the descender first row changed by {@code delta}, with the same rules as the accent rows. */
    PaintGuides withDescenderFirstRowMoved(int delta, int cellHeight) {
        return new PaintGuides(rows, columns, bars, accentRows, movedSetting(descenderFirstRow, delta, cellHeight));
    }

    private static OptionalInt movedSetting(OptionalInt setting, int delta, int cellHeight) {
        if (setting.isEmpty()) {
            return delta > 0 ? OptionalInt.of(0) : setting;
        }
        return OptionalInt.of(clamp(setting.getAsInt() + delta, cellHeight));
    }

    /** A copy with the accent-rows warn setting off. */
    PaintGuides withoutAccentRows() {
        return new PaintGuides(rows, columns, bars, OptionalInt.empty(), descenderFirstRow);
    }

    /** A copy with the descender warn setting off. */
    PaintGuides withoutDescenderFirstRow() {
        return new PaintGuides(rows, columns, bars, accentRows, OptionalInt.empty());
    }

    /** Reads the file text. Throws {@link IllegalArgumentException} naming the line on a bad line. */
    static PaintGuides parse(String text) {
        return PaintGuidesText.parse(text);
    }

    /**
     * The file text that {@link #parse} reads back as the same guides and warn settings: warn lines, then rows,
     * columns, and bars, each kind in its stored order. Comments and blank lines are not kept.
     */
    String toText() {
        return PaintGuidesText.toText(this);
    }

    private static String groupsText(Set<String> groups) {
        if (groups.size() == ALL_GROUPS.size()) {
            return "all";
        }
        StringBuilder names = new StringBuilder();
        for (String group : ALL_GROUPS) {
            if (groups.contains(group)) {
                if (names.length() > 0) {
                    names.append(',');
                }
                names.append(group.toLowerCase(Locale.ROOT));
            }
        }
        return names.toString();
    }

    public enum ListedKind { ROW, COLUMN, BAR }

    /**
     * One guide as the guide list shows it, whatever its kind.
     *
     * @param position boundary row, boundary column, or first row of a bar
     * @param rowCount rows in a bar, 0 for the other kinds
     * @param groups the groups text as {@link #toText} writes it, such as {@code all} or {@code lowercase,digits}
     */
    public record Listed(ListedKind kind, String label, int position, int rowCount, String groups) {

        /** Middle of a bar's band in rows from the top of the grid. */
        public float centerRow() {
            return position + rowCount / 2f;
        }
    }

    /** Every guide, group-filtered or not, in the order {@link #toText} writes them: rows, columns, then bars. */
    List<Listed> allGuides() {
        List<Listed> all = new ArrayList<>();
        for (Scoped<Guide> row : rows) {
            all.add(new Listed(ListedKind.ROW, row.guide().label(), row.guide().boundaryRow(), 0,
                groupsText(row.groups())));
        }
        for (Scoped<WidthGuide> column : columns) {
            all.add(new Listed(ListedKind.COLUMN, column.guide().label(), column.guide().boundaryColumn(), 0,
                groupsText(column.groups())));
        }
        for (Scoped<BarGuide> bar : bars) {
            all.add(new Listed(ListedKind.BAR, bar.guide().label(), bar.guide().firstRow(), bar.guide().rowCount(),
                groupsText(bar.groups())));
        }
        return List.copyOf(all);
    }

    /** Rows at the top reserved for accents, or empty when the file leaves the accent check off. */
    public OptionalInt accentRows() {
        return accentRows;
    }

    /** First row of the descender band, or empty when the file leaves the descender check off. */
    public OptionalInt descenderFirstRow() {
        return descenderFirstRow;
    }

    /** Whether there are no drawing guides; the warn settings are not guides. */
    boolean isEmpty() {
        return rows.isEmpty() && columns.isEmpty() && bars.isEmpty();
    }

    /**
     * The guides of a rules group (see {@code FontRules.groupNameFor}) that fit the cell. Width guides are sorted
     * by column and alternate label tiers 0, 1, 0, ... so neighbouring labels never overlap.
     */
    public Shown forGroup(String groupName, int cellWidth, int cellHeight) {
        List<Guide> shownRows = fittingRows(groupName, cellHeight);
        List<WidthGuide> shownWidths = withAlternatingTiers(fittingColumns(groupName, cellWidth));
        List<BarGuide> shownBars = fittingBars(groupName, cellHeight);
        return new Shown(List.copyOf(shownRows), List.copyOf(shownWidths), List.copyOf(shownBars));
    }

    private List<Guide> fittingRows(String groupName, int cellHeight) {
        List<Guide> shownRows = new ArrayList<>();
        for (Scoped<Guide> row : rows) {
            if (row.appliesTo(groupName) && row.guide().boundaryRow() <= cellHeight) {
                shownRows.add(row.guide());
            }
        }
        return shownRows;
    }

    private List<WidthGuide> fittingColumns(String groupName, int cellWidth) {
        List<WidthGuide> fitting = new ArrayList<>();
        for (Scoped<WidthGuide> column : columns) {
            if (column.appliesTo(groupName) && column.guide().boundaryColumn() <= cellWidth) {
                fitting.add(column.guide());
            }
        }
        fitting.sort(Comparator.comparingInt(WidthGuide::boundaryColumn));
        return fitting;
    }

    private static List<WidthGuide> withAlternatingTiers(List<WidthGuide> sortedGuides) {
        List<WidthGuide> shownWidths = new ArrayList<>();
        for (int i = 0; i < sortedGuides.size(); i++) {
            WidthGuide guide = sortedGuides.get(i);
            shownWidths.add(new WidthGuide(guide.label(), guide.boundaryColumn(), i % 2));
        }
        return shownWidths;
    }

    private List<BarGuide> fittingBars(String groupName, int cellHeight) {
        List<BarGuide> shownBars = new ArrayList<>();
        for (Scoped<BarGuide> bar : bars) {
            if (bar.appliesTo(groupName) && bar.guide().firstRow() + bar.guide().rowCount() <= cellHeight) {
                shownBars.add(bar.guide());
            }
        }
        return shownBars;
    }
}
