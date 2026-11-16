package com.github.halfdeadgames.asciigdx.workbench.help;

import com.github.halfdeadgames.asciigdx.workbench.derive.DeriveFlow;
import com.github.halfdeadgames.asciigdx.workbench.derive.OverlayCheck;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.CodePoints;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphOp;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the rows of the paint screen help panel for the current state. It holds no drawing or
 * input code.
 */
public final class PaintHelp {

    /** One overlay part as the help shows it. */
    public record PartView(int codePoint, int shift, int down) {
    }

    /**
     * What the help depends on.
     *
     * @param mode the derive flow mode; IDLE means plain painting
     * @param operation the highlighted operation in the menu, or the chosen one while picking
     * @param committedParts overlay parts already added, in order
     * @param pending the overlay part being picked, or null when the source is the target
     * @param upscale the upscale stage being shown, or null; it replaces the derive and paint help
     */
    public record State(DeriveFlow.Mode mode, GlyphOp.Kind operation, List<PartView> committedParts,
                        PartView pending,
                        OverlayCheck.Result check, UpscaleHelp upscale) {

        /** A derive or paint state. */
        public State(DeriveFlow.Mode mode, GlyphOp.Kind operation, List<PartView> committedParts,
                PartView pending,
                OverlayCheck.Result check) {
            this(mode, operation, committedParts, pending, check, null);
        }

        /** A derive or paint state with no overlay check. */
        public State(DeriveFlow.Mode mode, GlyphOp.Kind operation, List<PartView> committedParts,
                PartView pending) {
            this(mode, operation, committedParts, pending, null);
        }

        private static final State UPSCALE_PICKING = upscaleState(
                new UpscaleHelp(UpscaleHelp.Stage.PICK_SOURCE, List.of(), 0));

        /** The state while an upscale source glyph is picked. */
        public static State upscalePicking() {
            return UPSCALE_PICKING;
        }

        /**
         * The state while one upscaled version is chosen; {@code selected} is the 0-based index of
         * the marked label.
         */
        public static State upscaleChoosing(List<String> labels, int selected) {
            return upscaleState(
                    new UpscaleHelp(UpscaleHelp.Stage.CHOOSE_CANDIDATE, List.copyOf(labels),
                            selected));
        }

        private static State upscaleState(UpscaleHelp upscale) {
            return new State(DeriveFlow.Mode.IDLE, null, List.of(), null, null, upscale);
        }
    }

    /**
     * The upscale part of the help.
     *
     * @param stage which upscale step is shown
     * @param labels the candidate names, empty while picking the source glyph
     * @param selected index of the marked label
     */
    public record UpscaleHelp(Stage stage, List<String> labels, int selected) {

        /** The upscale steps that have their own help. */
        public enum Stage { PICK_SOURCE, CHOOSE_CANDIDATE }
    }

    /**
     * TEXT and WARNING rows are prose (already wrapped, or blank); the others are table rows with a
     * key and an action. WARNING is the highlighted overlay warning and INFO a table row of facts,
     * such as the shift range.
     */
    enum Kind { TEXT, HEADING, CONTROL, PART, PENDING_PART, INFO, WARNING }

    /**
     * One help row. A table row has a key and an action that starts at {@code actionColumn}; a text
     * row keeps all its text in {@code action} with an empty key.
     */
    record Row(Kind kind, String key, String action, int actionColumn) {

        boolean isTable() {
            return kind == Kind.CONTROL || kind == Kind.PART || kind == Kind.PENDING_PART
                    || kind == Kind.INFO;
        }
    }

    private static final int CONTINUATION_INDENT = 2;
    private static final int KEY_GAP = 2;
    private static final int PART_KEY_GAP = 1;
    private static final String PARTS_TITLE = "PARTS";

    private record Control(String key, String action) {
    }

    // A block lines its actions up in one column.
    private static final List<Control> IDLE_CONTROLS = List.of(
            new Control("CLICK", "PAINT PIXEL"), new Control("DRAG", "PAINT STROKE"),
            new Control("ARROWS", "PICK GLYPH"), new Control("PGUP", "SCROLL UP"),
            new Control("PGDN", "SCROLL DOWN"), new Control("HOME", "TOP"),
            new Control("END", "BOTTOM"), new Control("[ ]", "SCROLL HELP"));
    private static final List<Control> ALL_CONTROLS = List.of(
            new Control("CLICK", "PAINT PIXEL"), new Control("DRAG", "PAINT STROKE"),
            new Control("TABS", "SWITCH VIEW"), new Control("TAB", "NEXT VIEW"),
            new Control("ARROWS", "PICK GLYPH"), new Control("PGUP", "SCROLL UP"),
            new Control("PGDN", "SCROLL DOWN"), new Control("HOME", "TOP"),
            new Control("END", "BOTTOM"), new Control("[ ]", "SCROLL HELP"),
            new Control("N", "NEW GLYPH"), new Control("D", "DERIVE"),
            new Control("U", "UNLOCK DERIVED"), new Control("G", "GO TO SOURCE"),
            new Control("CTRL+S", "SAVE"), new Control("CTRL+E", "EXPORT"),
            new Control("CTRL+Z", "UNDO"), new Control("CTRL+Y", "REDO"),
            new Control("CTRL+=", "BIGGER UI"), new Control("CTRL+-", "SMALLER UI"),
            new Control("CTRL+0", "AUTO UI SIZE"), new Control("F1", "RULES"),
            new Control("F2", "GUIDES"), new Control("F5", "RELOAD FILES"),
            new Control("ALT+LETTER", "OPEN MENU"), new Control("F10", "MENU BAR"),
            new Control("ESC", "BACK"));
    private static final List<Control> MENU_CONTROLS = List.of(
            new Control("UP/DOWN", "CHOOSE"), new Control("ENTER", "SELECT"),
            new Control("ESC", "CANCEL"));
    private static final List<Control> PICK_CONTROLS = List.of(
            new Control("ARROWS", "MOVE CURSOR"), new Control("ENTER", "CONFIRM"),
            new Control("CLICK", "MOVE OR CONFIRM"), new Control("TAB", "NEXT VIEW"),
            new Control("ESC", "CANCEL"));
    private static final List<Control> OVERLAY_CONTROLS = List.of(
            new Control("ARROWS", "MOVE CURSOR"), new Control("[ ]", "SHIFT 1 COLUMN"),
            new Control("; '", "MOVE 1 ROW"), new Control("ENTER", "ADD PART"),
            new Control("BKSP", "REMOVE LAST"), new Control("F", "FINISH"),
            new Control("TAB", "NEXT VIEW"), new Control("ESC", "CANCEL"));
    private static final List<Control> REPLACE_CONTROLS = List.of(
            new Control("R", "REPLACE"), new Control("ESC", "BACK"));
    private static final List<Control> UPSCALE_PICK_CONTROLS = List.of(
            new Control("ARROWS", "MOVE"), new Control("CLICK", "SELECT"),
            new Control("CLICK AGAIN", "CHOOSE"), new Control("ENTER", "CHOOSE"),
            new Control("WHEEL", "SCROLL"), new Control("ESC", "BACK"));
    private static final List<Control> UPSCALE_CANDIDATE_CONTROLS = List.of(
            new Control("1-9", "PICK"), new Control("ARROWS", "NEXT"),
            new Control("ENTER", "APPLY"), new Control("ESC", "BACK"));

    private PaintHelp() {
    }

    /**
     * The help rows for a state; prose is wrapped to {@code width} characters, table rows never
     * are.
     */
    static List<Row> rows(State state, int width) {
        List<Row> out = new ArrayList<>();
        if (state.upscale() != null) {
            addUpscaleRows(out, state.upscale(), width);
            return out;
        }
        switch (state.mode()) {
            case IDLE -> addControls(out, IDLE_CONTROLS);
            case MENU -> {
                addText(out, describe(state.operation()), width);
                addBlank(out);
                addControls(out, MENU_CONTROLS);
            }
            case PICK_SOURCE -> {
                if (state.operation() == GlyphOp.Kind.OVERLAY) {
                    addText(out, "Add glyph parts, each moved into place, then finish.", width);
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
                addText(out, "This glyph already holds a drawing. Deriving replaces it; "
                        + "undo restores it.", width);
                addBlank(out);
                addControls(out, REPLACE_CONTROLS);
            }
        }
        return out;
    }

    /** Every paint control on one aligned column; the Controls panel shows these. */
    static List<Row> allControlRows() {
        List<Row> out = new ArrayList<>();
        addControls(out, ALL_CONTROLS);
        return out;
    }

    private static void addUpscaleRows(List<Row> out, UpscaleHelp upscale, int width) {
        if (upscale.stage() == UpscaleHelp.Stage.PICK_SOURCE) {
            addText(out, "Pick the glyph to upscale from.", width);
            addBlank(out);
            addControls(out, UPSCALE_PICK_CONTROLS);
            return;
        }
        addText(out, "Pick how to upscale.", width);
        addBlank(out);
        addCandidateList(out, upscale);
        addBlank(out);
        addControls(out, UPSCALE_CANDIDATE_CONTROLS);
    }

    private static void addCandidateList(List<Row> out, UpscaleHelp upscale) {
        List<String> labels = upscale.labels();
        int actionColumn = String.valueOf(labels.size()).length() + PART_KEY_GAP + 1;
        for (int i = 0; i < labels.size(); i++) {
            Kind kind = i == upscale.selected() ? Kind.PENDING_PART : Kind.PART;
            out.add(new Row(kind, String.valueOf(i + 1), labels.get(i), actionColumn));
        }
    }

    private static void addBlank(List<Row> out) {
        out.add(new Row(Kind.TEXT, "", "", 0));
    }

    private static void addText(List<Row> out, String text, int width) {
        for (String line : wrap(List.of(text), width)) {
            out.add(new Row(Kind.TEXT, "", line, 0));
        }
    }

    private static void addControls(List<Row> out, List<Control> controls) {
        int actionColumn = 0;
        for (int i = 0; i < controls.size(); i++) {
            actionColumn = Math.max(actionColumn, controls.get(i).key().length() + KEY_GAP);
        }
        for (int i = 0; i < controls.size(); i++) {
            Control control = controls.get(i);
            out.add(new Row(Kind.CONTROL, control.key(), control.action(), actionColumn));
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
            case OVERLAY -> "Stack copies of other glyphs, each moved into place.";
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
            int valueColumn = key.length() + PART_KEY_GAP;
            out.add(new Row(Kind.INFO, key, check.rangeText(), valueColumn));
            out.add(new Row(Kind.INFO, "DOWN RANGE", check.downRangeText(), valueColumn));
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
            out.add(new Row(Kind.PART, String.valueOf(i + 1), partText(parts.get(i)),
                    actionColumn));
        }
        if (state.pending() != null) {
            out.add(new Row(Kind.PENDING_PART, ">", partText(state.pending()), actionColumn));
        } else if (parts.isEmpty()) {
            out.add(new Row(Kind.TEXT, "", "NONE YET", 0));
        }
    }

    // The glyph itself is shown only when it is plain printable ASCII, which the UI font can draw.
    private static String partText(PartView part) {
        StringBuilder out = new StringBuilder(CodePoints.label(part.codePoint())).append(' ');
        if (part.codePoint() > ' ' && part.codePoint() < 0x7F) {
            out.append((char) part.codePoint()).append(' ');
        }
        out.append("SHIFT ").append(OverlayCheck.signed(part.shift()));
        if (part.down() != 0) {
            out.append(" DOWN ").append(OverlayCheck.signed(part.down()));
        }
        return out.toString();
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
