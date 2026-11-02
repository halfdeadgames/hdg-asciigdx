package com.github.halfdeadgames.asciigdx.sandbox.desktop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Decides which candidate supplies each cell of the final 16x16 sheet, from the workbench picks file. */
final class FinalSheetPicks {

    static final String NEAREST = "nearest";
    static final String HAND = "hand";
    static final String NONE = "none";

    private static final Set<String> KNOWN_IDS = Set.of(
        NEAREST, "scale2x", "scale2x-fill", "scale2x-trim", "mmpx", "scale2x-sfx", "light", HAND);

    private FinalSheetPicks() {
    }

    /** Reads code point to pick id from the tab-separated picks file; extra columns are ignored. */
    static Map<Integer, String> parse(String text) {
        Map<Integer, String> picks = new LinkedHashMap<>();
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].endsWith("\r") ? lines[i].substring(0, lines[i].length() - 1) : lines[i];
            if (line.isEmpty()) {
                continue;
            }
            String[] fields = line.split("\t", -1);
            if (fields.length < 3 || !fields[0].startsWith("U+") || fields[2].isEmpty()) {
                throw new IllegalArgumentException("Picks line " + (i + 1) + " is malformed: " + line);
            }
            int codePoint;
            try {
                codePoint = Integer.parseInt(fields[0].substring(2), 16);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Picks line " + (i + 1) + " has a bad code point: " + fields[0]);
            }
            if (picks.put(codePoint, fields[2]) != null) {
                throw new IllegalArgumentException("Picks line " + (i + 1) + " repeats " + fields[0]);
            }
        }
        return picks;
    }

    /**
     * Returns the candidate id for every sheet slot. Slots without a pick, and blank slots, use nearest.
     *
     * @throws IllegalArgumentException listing every code point whose pick cannot be honored
     */
    static String[] select(int[] sheetCodePoints, boolean[] blank, Map<Integer, String> picks, Set<Integer> handArt) {
        String[] chosen = new String[sheetCodePoints.length];
        Set<Integer> onSheet = new HashSet<>();
        List<String> nonePicks = new ArrayList<>();
        List<String> unknownIds = new ArrayList<>();
        List<String> missingHandArt = new ArrayList<>();
        for (int slot = 0; slot < sheetCodePoints.length; slot++) {
            int codePoint = sheetCodePoints[slot];
            onSheet.add(codePoint);
            String pick = picks.get(codePoint);
            if (pick == null || blank[slot]) {
                chosen[slot] = NEAREST;
            } else if (NONE.equals(pick)) {
                nonePicks.add(label(codePoint));
            } else if (!KNOWN_IDS.contains(pick)) {
                unknownIds.add(label(codePoint) + " (" + pick + ")");
            } else if (HAND.equals(pick) && !handArt.contains(codePoint)) {
                missingHandArt.add(label(codePoint));
            } else {
                chosen[slot] = pick;
            }
        }
        List<String> notOnSheet = new ArrayList<>();
        for (int codePoint : picks.keySet()) {
            if (!onSheet.contains(codePoint)) {
                notOnSheet.add(label(codePoint));
            }
        }
        StringBuilder problems = new StringBuilder();
        appendProblem(problems, "pick is none", nonePicks);
        appendProblem(problems, "unknown pick id", unknownIds);
        appendProblem(problems, "hand pick without hand art", missingHandArt);
        appendProblem(problems, "code point not in the sheet", notOnSheet);
        if (problems.length() > 0) {
            throw new IllegalArgumentException("Final sheet picks cannot be honored:" + problems);
        }
        return chosen;
    }

    private static String label(int codePoint) {
        return String.format("U+%04X", codePoint);
    }

    private static void appendProblem(StringBuilder problems, String title, List<String> entries) {
        if (!entries.isEmpty()) {
            problems.append("\n  ").append(title).append(": ").append(String.join(", ", entries));
        }
    }
}
