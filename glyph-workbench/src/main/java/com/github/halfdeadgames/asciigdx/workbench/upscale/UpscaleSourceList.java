package com.github.halfdeadgames.asciigdx.workbench.upscale;

import com.github.halfdeadgames.asciigdx.workbench.ui.Dialog;
import com.github.halfdeadgames.asciigdx.workbench.ui.GroupedPickList;
import com.github.halfdeadgames.asciigdx.workbench.ui.SelectList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The state of the upscale source list: options grouped under their font folder, the target's own folder first.
 * Unusable options are shown greyed with their reason, and the cursor never lands on them.
 */
public final class UpscaleSourceList {

    public static final int LINE_CELLS = 26;
    /** Dialog text lines above the list: the "Into" line and a blank one. */
    public static final int LINES_ABOVE_LIST = 2;

    // Room for a heading and its first size at least.
    private static final int MIN_SHOWN_ROWS = 2;
    private static final String ELLIPSIS = "..";
    private static final String INDENT = "  ";
    private static final int NO_OPTION = -1;

    private final GroupedPickList<UpscaleSources.Option> list = new GroupedPickList<>();
    private UpscaleSources.Font target;
    private boolean open;

    /** Builds the list for {@code target} and puts the cursor on the preferred source, if any is usable. */
    public void open(UpscaleSources.Font target, List<UpscaleSources.Font> fonts) {
        this.target = target;
        List<UpscaleSources.Option> options = UpscaleSources.options(target, fonts);
        List<SelectList.Row> rows = new ArrayList<>();
        List<UpscaleSources.Option> entries = new ArrayList<>();
        int[] rowOfOption = new int[options.size()];
        for (List<Integer> group : groupsByFolder(target, options)) {
            addGroup(options, group, rows, entries, rowOfOption);
        }
        list.setRows(rows, entries);
        selectDefault(UpscaleSources.defaultIndex(target, options), rowOfOption);
        open = true;
    }

    public void close() {
        list.clear();
        target = null;
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    /** Whether some source can be chosen; when false the dialog only tells why. */
    public boolean hasChoices() {
        return list.hasSelection();
    }

    /** Why nothing can be chosen, as two lines that fit the dialog; empty while the list is closed. */
    public List<String> noChoiceLines() {
        if (target == null) {
            return List.of();
        }
        return List.of("No smaller font divides", target.cellWidth() + "X" + target.cellHeight() + " evenly");
    }

    /** The highlighted source, or null when none can be chosen. */
    public UpscaleSources.Option selected() {
        return list.selected();
    }

    public GroupedPickList<UpscaleSources.Option> list() {
        return list;
    }

    public void move(int step) {
        list.move(step);
    }

    /**
     * Sizes the visible window for a dialog drawn in an area this many cells tall. Rows that do not fit are
     * scrolled; the title, the text above the list, and the buttons always stay.
     */
    public void fitTo(int areaHeight) {
        list.setCapacity(Math.max(MIN_SHOWN_ROWS, (Dialog.maxLines(areaHeight) - LINES_ABOVE_LIST + 1) / SelectList.PITCH));
    }

    /**
     * Dialog text lines: the "Into" line and a blank one, then blank lines reserving the room the list is drawn
     * into, or the reason nothing can be chosen. Empty while the list is closed.
     */
    public List<String> lines() {
        if (target == null) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        lines.add(intoLine());
        lines.add("");
        if (hasChoices()) {
            lines.addAll(Collections.nCopies(list.shownRows() * SelectList.PITCH - 1, " ".repeat(list.listWidth())));
        } else {
            lines.addAll(noChoiceLines());
        }
        return lines;
    }

    private String intoLine() {
        String line = "Into " + target.folderName().toUpperCase(Locale.ROOT) + " " + target.cellWidth() + "X"
            + target.cellHeight();
        return line.length() > LINE_CELLS ? line.substring(0, LINE_CELLS - ELLIPSIS.length()) + ELLIPSIS : line;
    }

    // Option indexes per folder: the target's folder first, the others in first-appearance order, empty folders left out.
    private static List<List<Integer>> groupsByFolder(UpscaleSources.Font target, List<UpscaleSources.Option> options) {
        Map<String, List<Integer>> byFolder = new LinkedHashMap<>();
        byFolder.put(target.folderName(), new ArrayList<>());
        for (int i = 0; i < options.size(); i++) {
            byFolder.computeIfAbsent(options.get(i).font().folderName(), folder -> new ArrayList<>()).add(i);
        }
        List<List<Integer>> groups = new ArrayList<>();
        for (List<Integer> group : byFolder.values()) {
            if (!group.isEmpty()) {
                groups.add(group);
            }
        }
        return groups;
    }

    private static void addGroup(List<UpscaleSources.Option> options, List<Integer> group, List<SelectList.Row> rows,
                                 List<UpscaleSources.Option> entries, int[] rowOfOption) {
        String folderName = options.get(group.get(0)).font().folderName();
        rows.add(new SelectList.Row(folderName.toUpperCase(Locale.ROOT), false, false, 0));
        entries.add(null);
        for (int optionIndex : group) {
            UpscaleSources.Option option = options.get(optionIndex);
            rowOfOption[optionIndex] = rows.size();
            rows.add(rowFor(option));
            entries.add(option.usable() ? option : null);
        }
    }

    private static SelectList.Row rowFor(UpscaleSources.Option option) {
        UpscaleSources.Font font = option.font();
        String size = font.cellWidth() + "X" + font.cellHeight();
        // SelectList indents only selectable rows, so an unusable row carries the indent in its text.
        if (!option.usable()) {
            return new SelectList.Row(INDENT + size + "  " + option.problem(), false, true, 0);
        }
        return new SelectList.Row(size + "  " + option.factor() + "X", true, false, 0);
    }

    private void selectDefault(int defaultOptionIndex, int[] rowOfOption) {
        if (defaultOptionIndex == NO_OPTION) {
            return;
        }
        list.hover(rowOfOption[defaultOptionIndex]);
    }
}
