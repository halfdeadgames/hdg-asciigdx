package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphOp;
import java.util.ArrayList;
import java.util.List;

/** Builds the rows of the paint screen help panel for the current state. It holds no drawing or input code. */
final class PaintHelp {

    /** One overlay part as the help shows it. */
    record PartView(int codePoint, int shift) { }

    /**
     * What the help depends on.
     *
     * @param mode the derive flow mode; IDLE means plain painting
     * @param operation the highlighted operation in the menu, or the chosen one while picking
     * @param committedParts overlay parts already added, in order
     * @param pending the overlay part being picked, or null when the source is the target
     */
    record State(DeriveFlow.Mode mode, GlyphOp.Kind operation, List<PartView> committedParts, PartView pending,
                 OverlayCheck.Result check) {

        /** A state with no overlay check. */
        State(DeriveFlow.Mode mode, GlyphOp.Kind operation, List<PartView> committedParts, PartView pending) {
            this(mode, operation, committedParts, pending, null);
        }
    }

    /**
     * TEXT and WARNING rows are prose (already wrapped, or blank); the others are tables whose action may be bounced.
     * WARNING is the highlighted overlay warning and INFO a table row of facts, such as the shift range.
     */
    enum Kind { TEXT, HEADING, CONTROL, PART, PENDING_PART, INFO, WARNING }

    /**
     * One help row. A table row has a key and an action that starts at {@code actionColumn}; a text row
     * keeps all its text in {@code action} with an empty key.
     */
    record Row(Kind kind, String key, String action, int actionColumn) {

        boolean isTable() {
            return kind == Kind.CONTROL || kind == Kind.PART || kind == Kind.PENDING_PART || kind == Kind.INFO;
        }

        /** The row as plain text, key padded to the action column; used when nothing is bounced. */
        String plain() {
            StringBuilder out = new StringBuilder(key);
            while (out.length() < actionColumn) {
                out.append(' ');
            }
            return out.append(action).toString();
        }
    }

    private static final int CONTINUATION_INDENT = 2;
    private static final int KEY_GAP = 2;
    private static final int PART_KEY_GAP = 1;
    private static final String PARTS_TITLE = "PARTS";

    // Each control is {key, action}; a block lines its actions up in one column.
    private static final String[][] PAINT_CONTROLS = {
        {"CLICK", "PAINT PIXEL"}, {"DRAG", "PAINT STROKE"}, {"TABS", "SWITCH VIEW"}, {"ARROWS", "PICK GLYPH"}, {"PGUP", "SCROLL UP"}, {"PGDN", "SCROLL DOWN"},
        {"HOME", "TOP"}, {"END", "BOTTOM"}, {"[ ]", "SCROLL NOTE"}, {"D", "DERIVE"}, {"U", "UNLOCK DERIVED"},
        {"G", "GO TO SOURCE"}, {"CTRL+S", "SAVE"}, {"CTRL+Z", "UNDO"}, {"CTRL+Y", "REDO"},
        {"F1", "RULES"}, {"ESC", "BACK"}};
    private static final String[][] MENU_CONTROLS = {
        {"UP/DOWN", "CHOOSE"}, {"ENTER", "SELECT"}, {"ESC", "CANCEL"}};
    private static final String[][] PICK_CONTROLS = {
        {"ARROWS", "MOVE CURSOR"}, {"ENTER", "CONFIRM"}, {"CLICK", "MOVE OR CONFIRM"}, {"ESC", "CANCEL"}};
    private static final String[][] OVERLAY_CONTROLS = {
        {"ARROWS", "MOVE CURSOR"}, {"[ ]", "SHIFT 1 COLUMN"}, {"ENTER", "ADD PART"},
        {"BKSP", "REMOVE LAST"}, {"F", "FINISH"}, {"ESC", "CANCEL"}};
    private static final String[][] REPLACE_CONTROLS = {{"R", "REPLACE"}, {"ESC", "BACK"}};

    private PaintHelp() {
    }

    /** The help rows for a state; prose is wrapped to {@code width} characters, table rows never are. */
    static List<Row> rows(State state, int width) {
        List<Row> out = new ArrayList<>();
        switch (state.mode()) {
            case IDLE -> addControls(out, PAINT_CONTROLS);
            case MENU -> {
                addText(out, describe(state.operation()), width);
                addBlank(out);
                addControls(out, MENU_CONTROLS);
            }
            case PICK_SOURCE -> {
                if (state.operation() == GlyphOp.Kind.OVERLAY) {
                    addText(out, "Add glyph parts, each shifted sideways, then finish.", width);
                    addBlank(out);
                    addControls(out, OVERLAY_CONTROLS);
                    addBlank(out);
                    addParts(out, state, width);
                } else {
                    addText(out, "Pick the source glyph.", width);
                    addBlank(out);
                    addControls(out, PICK_CONTROLS);
                }
            }
            case CONFIRM_REPLACE -> {
                addText(out, "This glyph already holds a drawing. Deriving replaces it; undo restores it.", width);
                addBlank(out);
                addControls(out, REPLACE_CONTROLS);
            }
        }
        return out;
    }

    private static void addBlank(List<Row> out) {
        out.add(new Row(Kind.TEXT, "", "", 0));
    }

    private static void addText(List<Row> out, String text, int width) {
        for (String line : wrap(List.of(text), width)) {
            out.add(new Row(Kind.TEXT, "", line, 0));
        }
    }

    private static void addControls(List<Row> out, String[][] controls) {
        int actionColumn = 0;
        for (String[] control : controls) {
            actionColumn = Math.max(actionColumn, control[0].length() + KEY_GAP);
        }
        for (String[] control : controls) {
            out.add(new Row(Kind.CONTROL, control[0], control[1], actionColumn));
        }
    }

    static String describe(GlyphOp.Kind kind) {
        return switch (kind) {
            case MIRROR -> "Copy a glyph flipped left to right.";
            case FLIP -> "Copy a glyph flipped top to bottom.";
            case ROTATE90 -> "Copy a glyph turned 90 degrees clockwise.";
            case ROTATE180 -> "Copy a glyph turned 180 degrees clockwise.";
            case ROTATE270 -> "Copy a glyph turned 270 degrees clockwise.";
            case INVERT -> "Copy a glyph with ink and blank swapped.";
            case OVERLAY -> "Stack copies of other glyphs, each shifted sideways.";
        };
    }

    private static void addParts(List<Row> out, State state, int width) {
        addPartList(out, state);
        OverlayCheck.Result check = state.check();
        if (check == null) {
            return;
        }
        if (state.pending() != null && check.hasRange()) {
            String key = "SHIFT RANGE";
            out.add(new Row(Kind.INFO, key, check.rangeText(), key.length() + PART_KEY_GAP));
        }
        if (check.hasWarning()) {
            addBlank(out);
            for (String line : wrap(List.of(check.message()), width)) {
                out.add(new Row(Kind.WARNING, "", line, 0));
            }
        }
    }

    private static void addPartList(List<Row> out, State state) {
        out.add(new Row(Kind.HEADING, "", PARTS_TITLE, 0));
        List<PartView> parts = state.committedParts();
        int actionColumn = Math.max(String.valueOf(parts.size()).length(), 1) + PART_KEY_GAP;
        for (int i = 0; i < parts.size(); i++) {
            out.add(new Row(Kind.PART, String.valueOf(i + 1), partText(parts.get(i)), actionColumn));
        }
        if (state.pending() != null) {
            out.add(new Row(Kind.PENDING_PART, ">", partText(state.pending()), actionColumn));
        } else if (parts.isEmpty()) {
            out.add(new Row(Kind.TEXT, "", "NONE YET", 0));
        }
    }

    // The glyph itself is shown only when it is plain printable ASCII, which the UI font can draw.
    private static String partText(PartView part) {
        StringBuilder out = new StringBuilder("U+");
        String hex = Integer.toHexString(part.codePoint()).toUpperCase(java.util.Locale.ROOT);
        for (int pad = hex.length(); pad < 4; pad++) {
            out.append('0');
        }
        out.append(hex).append(' ');
        if (part.codePoint() > ' ' && part.codePoint() < 0x7F) {
            out.append((char) part.codePoint()).append(' ');
        }
        return out.append("SHIFT ").append(part.shift() > 0 ? "+" : "").append(part.shift()).toString();
    }

    /** Breaks prose lines longer than {@code width} at spaces; continuation lines are indented. */
    static List<String> wrap(List<String> lines, int width) {
        List<String> out = new ArrayList<>();
        int limit = Math.max(CONTINUATION_INDENT + 2, width);
        for (String line : lines) {
            String rest = line;
            String prefix = "";
            while (prefix.length() + rest.length() > limit) {
                int room = limit - prefix.length();
                int cut = rest.lastIndexOf(' ', room);
                if (cut < 1) {
                    cut = room;
                }
                out.add(prefix + rest.substring(0, cut));
                rest = rest.substring(cut).stripLeading();
                prefix = " ".repeat(CONTINUATION_INDENT);
            }
            out.add(prefix + rest);
        }
        return out;
    }
}
