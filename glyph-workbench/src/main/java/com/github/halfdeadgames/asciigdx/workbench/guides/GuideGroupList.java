package com.github.halfdeadgames.asciigdx.workbench.guides;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The checklist of rules-file groups for one guide: a row per defined group in file order, then a checked row for
 * each group the guide names that the file does not define. A guide for all glyphs has nothing checked.
 */
final class GuideGroupList {

    private static final String CHECKED_BOX = "[X] ";
    private static final String UNCHECKED_BOX = "[ ] ";
    private static final String UNKNOWN_MARK = " (UNKNOWN)";

    record Row(String name, boolean checked, boolean unknown) {

        String text() {
            return (checked ? CHECKED_BOX : UNCHECKED_BOX) + name.toUpperCase(Locale.ROOT) + (unknown ? UNKNOWN_MARK : "");
        }
    }

    private final List<String> definedGroups;
    private List<Row> rows;
    private int selected;
    private int firstVisible;

    GuideGroupList(List<String> definedGroups, String guideGroupsText) {
        this.definedGroups = List.copyOf(definedGroups);
        this.rows = buildRows(guideGroupsText);
    }

    /** Whether a font with these group names has anything to choose from. */
    static boolean canOpen(List<String> definedGroups) {
        return !definedGroups.isEmpty();
    }

    /** Rebuilds the rows from the guide's current groups; the selection stays at its place unless the list got shorter. */
    void showGroups(String guideGroupsText) {
        rows = buildRows(guideGroupsText);
        select(selected);
    }

    private List<Row> buildRows(String guideGroupsText) {
        Set<String> named = GuideGroups.namesIn(guideGroupsText);
        List<Row> built = new ArrayList<>();
        for (String group : definedGroups) {
            built.add(new Row(group, named.contains(group), false));
        }
        for (String name : named) {
            if (!definedGroups.contains(name)) {
                built.add(new Row(name, true, true));
            }
        }
        return List.copyOf(built);
    }

    int size() {
        return rows.size();
    }

    Row row(int index) {
        return rows.get(index);
    }

    String rowText(int index) {
        return rows.get(index).text();
    }

    String selectedName() {
        return rows.get(selected).name();
    }

    int selectedIndex() {
        return selected;
    }

    /** Selects a row; an index outside the list is moved to the nearest end. */
    void select(int index) {
        selected = Math.max(0, Math.min(rows.size() - 1, index));
    }

    void move(int delta) {
        select(selected + delta);
    }

    int firstVisible(int visibleRows) {
        firstVisible = GuideListRows.scrolledFirst(firstVisible, selected, rows.size(), visibleRows);
        return firstVisible;
    }
}
