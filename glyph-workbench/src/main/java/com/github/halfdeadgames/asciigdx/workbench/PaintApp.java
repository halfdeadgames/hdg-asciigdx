package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataParser;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphOp;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import com.github.halfdeadgames.asciigdx.workbench.ui.Dialog;
import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;
import com.github.halfdeadgames.asciigdx.workbench.ui.Panel;
import com.github.halfdeadgames.asciigdx.workbench.ui.SelectList;
import com.github.halfdeadgames.asciigdx.workbench.ui.StatusBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.TopBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Paint screen: edits one font data file glyph by glyph (step 1: toggle and drag pixels, save). */
public final class PaintApp implements WorkbenchScreen {

    private static final String TAG = "Paint";
    private static final String KEY_DERIVED = "derived";
    private static final String KEY_EMPTY = "empty";
    private static final String KEY_GAP = "gap";
    private static final String KEY_PICK = "pick";
    private static final String KEY_PICK_PROBLEM = "pickProblem";
    private static final String KEY_REVIEW = "review";
    private static final int MARGIN = 12;
    private static final int TEXT_LINE = Painter.TEXT_SIZE;
    private static final int COLUMNS = PaintSheetView.COLUMNS;
    private static final int WHEEL_ROWS = 3;

    private static final List<StatusBar.Key> KEYS = List.of(
        new StatusBar.Key("D", "DERIVE"), new StatusBar.Key("CTRL+S", "SAVE"),
        new StatusBar.Key("CTRL+Z/Y", "UNDO"), new StatusBar.Key("F1", "RULES"), new StatusBar.Key("ESC", "BACK"));
    private static final List<StatusBar.Key> PICK_KEYS = List.of(
        new StatusBar.Key("ENTER", "USE SOURCE"), new StatusBar.Key("ARROWS", "MOVE SOURCE"),
        new StatusBar.Key("ESC", "CANCEL"));
    private static final List<Dialog.Button> QUIT_BUTTONS = List.of(
        new Dialog.Button("S", "SAVE", Dialog.Tone.OK), new Dialog.Button("D", "DISCARD", Dialog.Tone.WARN),
        new Dialog.Button("ESC", "CANCEL", Dialog.Tone.DIM));

    private static final String MENU_TITLE = "DERIVE";
    private static final int MENU_WIDTH = 26;
    private static final List<Dialog.Button> MENU_BUTTONS = List.of(
        new Dialog.Button("ENTER", "SELECT", Dialog.Tone.OK), new Dialog.Button("ESC", "CANCEL", Dialog.Tone.DIM));
    private static final List<Dialog.Button> REPLACE_BUTTONS = List.of(
        new Dialog.Button("R", "REPLACE", Dialog.Tone.WARN), new Dialog.Button("ESC", "CANCEL", Dialog.Tone.DIM));
    // Blank dialog lines reserve the room the select list is drawn into, two cells per operation.
    private static final List<String> MENU_LINES = Collections.nCopies(
        DeriveFlow.OPERATIONS.size() * SelectList.PITCH - 1, " ".repeat(MENU_WIDTH));
    private static final List<SelectList.Row> MENU_ROWS = menuRows();

    private final DeriveFlow flow = new DeriveFlow();
    private PixelGlyph pickShown;
    private PixelGlyph blankPick;
    private boolean pickValid;
    private final ScreenViewport viewport = new ScreenViewport();
    private final Vector2 pointer = new Vector2();
    private final StringBuilder line = new StringBuilder();
    private final DevCapture devCapture = new DevCapture();
    private final KeyRepeat repeatLeft = new KeyRepeat();
    private final KeyRepeat repeatRight = new KeyRepeat();
    private final KeyRepeat repeatUp = new KeyRepeat();
    private final KeyRepeat repeatDown = new KeyRepeat();
    private final KeyRepeat repeatUndo = new KeyRepeat();
    private final KeyRepeat repeatRedo = new KeyRepeat();

    private SpriteBatch batch;
    private Texture whitePixel;
    private TextureGlyphSource source;
    private Painter painter;
    private FileHandle dataFile;
    private String folderName;
    private FontEditor editor;
    private PaintStroke stroke;
    private PaintSheetView sheet;
    private PaintGridView grid;
    private PaintPreview preview;
    private RulesOverlay rules;
    private NotePanel notePanel;
    private final MessageStrip messages = new MessageStrip();
    private PaintLayout.Result rects;
    private String lastProblem = "";
    private String pickPromptText = "";
    private String pickProblemText = "";
    private int derivedTextGlyph = -1;
    private int derivedTextSource = -1;
    private String derivedText = "";
    private int gapTextGlyph = -1;
    private String gapText = "";
    private int gapWatchGlyph = -1;
    private boolean gapWatchInk;
    private ReviewFile review = new ReviewFile();
    private int shownNoteFor = -1;
    private boolean[] unselectable;
    private int[] codePoints;
    private int cursor;
    private boolean revealCursor = true;
    private boolean confirmingQuit;
    private int wheelTicks;
    private int pointerX;
    private int pointerY;

    private final Runnable onExit;
    private final String fontDir;
    private final String fontSize;

    /**
     * @param onExit called when the user leaves the screen with Esc (after the unsaved prompt)
     * @param fontDir absolute path of the font folder
     * @param fontSize size name, which is also the data file name without {@code .txt}
     */
    public PaintApp(Runnable onExit, String fontDir, String fontSize) {
        this.onExit = onExit;
        this.fontDir = fontDir;
        this.fontSize = fontSize;
    }

    @Override
    public void show() {
        if (fontDir == null) {
            throw new GdxRuntimeException("workbench.fontDir is not set; start with -Pmode=paint through Gradle");
        }
        FileHandle directory = Gdx.files.absolute(fontDir);
        dataFile = directory.child(fontSize + ".txt");
        folderName = directory.name().toUpperCase(java.util.Locale.ROOT);
        editor = new FontEditor(FontDataParser.parse(dataFile.readString("UTF-8")));
        stroke = new PaintStroke(editor);

        codePoints = new int[editor.slotCount()];
        unselectable = new boolean[editor.slotCount()];
        for (int slot = 0; slot < codePoints.length; slot++) {
            codePoints[slot] = editor.codePointAtSlot(slot);
            unselectable[slot] = codePoints[slot] == FontEditor.NO_GLYPH;
        }
        cursor = Math.max(0, CursorNavigator.firstNonBlank(unselectable));
        int preselected = DevCapture.slotToSelect(codePoints, unselectable);
        if (preselected >= 0) {
            cursor = preselected;
        }

        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        whitePixel = new Texture(pixel);
        pixel.dispose();
        source = TextureGlyphSource.loadDungeonMode();
        batch = new SpriteBatch();
        painter = new Painter(batch, whitePixel, source, viewport);
        sheet = new PaintSheetView(painter, editor, 0, 2 * TEXT_LINE);
        grid = new PaintGridView(painter, editor, PaintGuides.saiksistt(editor.cellWidth()));
        preview = new PaintPreview(painter, editor);
        rules = new RulesOverlay(painter, fontDir);
        notePanel = new NotePanel(painter);
        loadReviewNotes(directory);
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                wheelTicks += amountY > 0 ? 1 : amountY < 0 ? -1 : 0;
                return true;
            }
        });
        openDeriveForCapture();
    }

    private void openDeriveForCapture() {
        String request = DevCapture.deriveRequest();
        if (request == null) {
            return;
        }
        flow.openMenu(cursor);
        if ("menu".equals(request.trim())) {
            return;
        }
        int sourceSlot = slotOf(DevCapture.parseCodePoint(request));
        if (sourceSlot < 0 || unselectable[sourceSlot]) {
            return;
        }
        beginPick();
        flow.moveSource(sourceSlot);
        refreshPick();
    }

    // A review file of this size is read once; its notes are shown read-only next to the grid.
    private void loadReviewNotes(FileHandle directory) {
        FileHandle reviewFile = directory.child(fontSize + ".review.txt");
        if (!reviewFile.exists()) {
            return;
        }
        try {
            review = ReviewFile.parse(reviewFile.readString("UTF-8"));
        } catch (IllegalArgumentException e) {
            messages.setCondition(KEY_REVIEW, true, "Review file is bad: notes are off");
            Gdx.app.error(TAG, "Cannot read " + reviewFile.path() + ": " + e.getMessage());
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        handleInput(Gdx.graphics.getDeltaTime());
        draw();
        devCapture.afterRender();
    }

    private int worldWidth() {
        return Math.round(viewport.getWorldWidth());
    }

    private int worldHeight() {
        return Math.round(viewport.getWorldHeight());
    }

    private void handleInput(float delta) {
        sheet.fitToHeight(worldHeight(), TEXT_LINE, cursor, revealCursor);
        revealCursor = false;
        rects = PaintLayout.compute(worldWidth() / TEXT_LINE, worldHeight() / TEXT_LINE,
            sheet.frameRight() / TEXT_LINE, editor.cellHeight());
        fitGrid();
        updateConditions();
        if (rules.handleInput(delta, wheelTicks)) {
            wheelTicks = 0;
            stroke.end();
            return;
        }
        if (confirmingQuit) {
            handleQuitPrompt();
            wheelTicks = 0;
            return;
        }
        boolean control = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        if (control && Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            save();
        }
        if (flow.mode() == DeriveFlow.Mode.MENU) {
            wheelTicks = 0;
            handleDeriveMenu();
            return;
        }
        if (flow.mode() == DeriveFlow.Mode.CONFIRM_REPLACE) {
            wheelTicks = 0;
            handleReplacePrompt();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (flow.isPicking()) {
                cancelPick();
            } else if (editor.isModified()) {
                messages.nextAction();
                confirmingQuit = true;
                stroke.end();
            } else {
                onExit.run();
            }
            return;
        }
        handleUndoKeys(control, delta);
        if (flow.isPicking()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
                confirmSource();
                return;
            }
        } else if (!control) {
            handleDeriveKeys();
            if (flow.isActive()) {
                return;
            }
        }
        moveCursor(delta);
        handleScrollKeys();
        trackPointer();
        handleMouse();
        String problem = editor.problem();
        if (!problem.equals(lastProblem)) {
            lastProblem = problem;
            if (!problem.isEmpty()) {
                messages.nextAction();
                messages.warn("Edit rejected: " + problem);
            }
        }
    }

    // The grid takes the whole panel interior, with room above for the width labels and to the right for the height labels.
    private void fitGrid() {
        ReviewLayout.Rect panel = rects.grid();
        int left = (panel.x() + 2) * TEXT_LINE;
        int top = (panel.y() + 1 + PaintGridView.LABEL_ROWS_ABOVE) * TEXT_LINE;
        grid.fitTo(left, top, (panel.width() - 3) * TEXT_LINE, (panel.bottom() - 1) * TEXT_LINE - top - TEXT_LINE / 2);
    }

    // Conditions follow the current state every frame; texts are rebuilt only when their inputs change.
    private void updateConditions() {
        int codePoint = codePoints[cursor];
        boolean derived = editor.isDerived(codePoint);
        if (derived) {
            int from = editor.derivedFrom(codePoint);
            if (derivedTextGlyph != codePoint || derivedTextSource != from) {
                derivedTextGlyph = codePoint;
                derivedTextSource = from;
                describeGlyph(codePoint).append(" is derived from U+");
                appendHex(line, from);
                derivedText = line.append(": not editable").toString();
            }
            messages.setCondition(KEY_DERIVED, true, derivedText);
        } else {
            messages.clearCondition(KEY_DERIVED);
        }
        if (!derived && !editor.hasGlyph(codePoint)) {
            messages.setCondition(KEY_EMPTY, false, "Empty slot: click the grid to create it");
        } else {
            messages.clearCondition(KEY_EMPTY);
        }
        boolean ink = editor.hasGapInk(codePoint);
        if (ink) {
            if (gapTextGlyph != codePoint) {
                gapTextGlyph = codePoint;
                gapText = describeGlyph(codePoint).append(" has ink in the gap column").toString();
            }
            messages.setCondition(KEY_GAP, true, gapText);
        } else {
            messages.clearCondition(KEY_GAP);
        }
        if (gapWatchGlyph == codePoint && gapWatchInk && !ink) {
            messages.ok(describeGlyph(codePoint).append(" gap column is empty: back to normal width").toString());
        }
        gapWatchGlyph = codePoint;
        gapWatchInk = ink;
        if (flow.isPicking()) {
            messages.setCondition(KEY_PICK, false, pickPromptText);
            if (pickProblemText.isEmpty()) {
                messages.clearCondition(KEY_PICK_PROBLEM);
            } else {
                messages.setCondition(KEY_PICK_PROBLEM, true, pickProblemText);
            }
        } else {
            messages.clearCondition(KEY_PICK);
            messages.clearCondition(KEY_PICK_PROBLEM);
        }
    }

    // Ctrl+Z undoes; Ctrl+Y and Ctrl+Shift+Z redo. Ignored while a stroke is active.
    private void handleUndoKeys(boolean control, float delta) {
        boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        boolean zDown = control && Gdx.input.isKeyPressed(Input.Keys.Z);
        boolean undoDown = zDown && !shift;
        boolean redoDown = control && (Gdx.input.isKeyPressed(Input.Keys.Y) || (zDown && shift));
        int undos = repeatUndo.update(undoDown, delta);
        int redos = repeatRedo.update(redoDown, delta);
        if (stroke.isActive() || flow.isActive()) {
            return;
        }
        for (int i = 0; i < undos; i++) {
            showChangedGlyph(editor.undo(), "Undid change to U+");
        }
        for (int i = 0; i < redos; i++) {
            showChangedGlyph(editor.redo(), "Redid change to U+");
        }
    }

    private void showChangedGlyph(int codePoint, String notice) {
        if (codePoint == FontEditor.NO_GLYPH) {
            return;
        }
        messages.nextAction();
        line.setLength(0);
        line.append(notice);
        appendHex(line, codePoint);
        messages.ok(line.toString());
        for (int slot = 0; slot < codePoints.length; slot++) {
            if (codePoints[slot] == codePoint && !unselectable[slot]) {
                cursor = slot;
                sheet.scroll().keepVisible(cursor / COLUMNS);
                return;
            }
        }
    }

    // While picking a derive source, the arrows move the source cursor and the target cursor stays put.
    private void moveCursor(float delta) {
        boolean picking = flow.isPicking();
        int before = picking ? flow.sourceSlot() : cursor;
        int position = before;
        int rows = codePoints.length / COLUMNS;
        int lefts = repeatLeft.update(Gdx.input.isKeyPressed(Input.Keys.LEFT), delta);
        for (int i = 0; i < lefts; i++) {
            position = CursorNavigator.move(unselectable, COLUMNS, rows, position, -1, 0);
        }
        int rights = repeatRight.update(Gdx.input.isKeyPressed(Input.Keys.RIGHT), delta);
        for (int i = 0; i < rights; i++) {
            position = CursorNavigator.move(unselectable, COLUMNS, rows, position, 1, 0);
        }
        int ups = repeatUp.update(Gdx.input.isKeyPressed(Input.Keys.UP), delta);
        for (int i = 0; i < ups; i++) {
            position = CursorNavigator.move(unselectable, COLUMNS, rows, position, 0, -1);
        }
        int downs = repeatDown.update(Gdx.input.isKeyPressed(Input.Keys.DOWN), delta);
        for (int i = 0; i < downs; i++) {
            position = CursorNavigator.move(unselectable, COLUMNS, rows, position, 0, 1);
        }
        if (position != before) {
            messages.nextAction();
            if (picking) {
                flow.moveSource(position);
                refreshPick();
            } else {
                cursor = position;
                stroke.end();
            }
            sheet.scroll().keepVisible(position / COLUMNS);
        }
    }

    private boolean leftClicked() {
        return Gdx.input.justTouched() && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
    }

    // D opens the derive menu, U unlocks a derived glyph, G jumps to the source of a derived glyph.
    private void handleDeriveKeys() {
        int codePoint = codePoints[cursor];
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            messages.nextAction();
            stroke.end();
            flow.openMenu(cursor);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.U)) {
            messages.nextAction();
            stroke.end();
            if (editor.unlock(codePoint)) {
                describeGlyph(codePoint).append(" unlocked: now editable");
                messages.ok(line.toString());
            } else {
                messages.warn("Only a derived glyph can be unlocked");
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            messages.nextAction();
            stroke.end();
            int sourceSlot = slotOf(editor.derivedFrom(codePoint));
            if (sourceSlot >= 0) {
                cursor = sourceSlot;
                sheet.scroll().keepVisible(cursor / COLUMNS);
            } else {
                messages.warn("Only a derived glyph has a source to go to");
            }
        }
    }

    private int slotOf(int codePoint) {
        if (codePoint == FontEditor.NO_GLYPH) {
            return -1;
        }
        for (int slot = 0; slot < codePoints.length; slot++) {
            if (codePoints[slot] == codePoint) {
                return slot;
            }
        }
        return -1;
    }

    private StringBuilder describeGlyph(int codePoint) {
        line.setLength(0);
        line.append("U+");
        appendHex(line, codePoint);
        return line;
    }

    private void handleDeriveMenu() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            messages.nextAction();
            flow.cancel();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            messages.nextAction();
            flow.moveMenu(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            messages.nextAction();
            flow.moveMenu(1);
        }
        boolean choose = Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER);
        trackPointer();
        if (leftClicked()) {
            Dialog.Layout layout = menuLayout();
            int column = pointerX / TEXT_LINE;
            int row = SelectList.rowAt(pointerY / TEXT_LINE, layout.textY(), DeriveFlow.OPERATIONS.size());
            if (row >= 0 && column >= layout.x() && column < layout.x() + layout.width()) {
                flow.highlightMenuRow(row);
                choose = true;
            }
        }
        if (choose) {
            beginPick();
        }
    }

    private void beginPick() {
        messages.nextAction();
        flow.chooseOperation();
        stroke.end();
        blankPick = new PixelGlyph(codePoints[cursor], false, PixelGrid.blank(editor.bodyWidth(), editor.cellHeight()));
        pickShown = null;
        pickValid = false;
        pickPromptText = pickPrompt(false);
        pickProblemText = "";
    }

    private String pickPrompt(boolean withSource) {
        describeGlyph(codePoints[cursor]);
        line.insert(0, "PICK SOURCE FOR ");
        line.append(" (").append(DeriveFlow.label(flow.operation())).append(')');
        if (withSource) {
            line.append(" FROM U+");
            appendHex(line, codePoints[flow.sourceSlot()]);
        }
        return line.toString();
    }

    private GlyphOp currentOp() {
        return new GlyphOp(flow.operation(), List.of(new GlyphOp.Source(codePoints[flow.sourceSlot()], 0)));
    }

    // Runs only when the source changes. With the source on the target there is nothing to preview yet.
    private void refreshPick() {
        if (flow.sourceSlot() == cursor) {
            pickShown = null;
            pickValid = false;
            pickPromptText = pickPrompt(false);
            pickProblemText = "";
            return;
        }
        PixelGlyph result = editor.previewDerived(codePoints[cursor], currentOp());
        lastProblem = editor.problem();
        pickValid = result != null;
        pickPromptText = pickPrompt(pickValid);
        if (pickValid) {
            pickShown = result;
            pickProblemText = "";
        } else {
            pickShown = blankPick;
            pickProblemText = editor.problem();
        }
    }

    // A refused source is already listed as a pick problem, so only the same-glyph case needs a result.
    private void confirmSource() {
        messages.nextAction();
        if (!pickValid) {
            if (flow.sourceSlot() == cursor) {
                messages.warn("Pick a different glyph as the source");
            }
            return;
        }
        if (editor.wouldReplaceDrawing(codePoints[cursor])) {
            flow.askReplace();
        } else {
            applyDerive();
        }
    }

    private void applyDerive() {
        int target = codePoints[cursor];
        String summary = pickPrompt(true);
        messages.nextAction();
        if (editor.derive(target, currentOp())) {
            lastProblem = "";
            flow.finish();
            pickShown = null;
            sheet.scroll().keepVisible(cursor / COLUMNS);
            messages.ok("Derived: " + summary.substring("PICK SOURCE FOR ".length()));
        } else {
            lastProblem = editor.problem();
            messages.warn(editor.problem());
            if (flow.mode() == DeriveFlow.Mode.CONFIRM_REPLACE) {
                flow.cancel();
            }
        }
    }

    private void cancelPick() {
        messages.nextAction();
        flow.cancel();
        pickShown = null;
        sheet.scroll().keepVisible(cursor / COLUMNS);
        messages.ok("Derive cancelled");
    }

    private void handleReplacePrompt() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            applyDerive();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            messages.nextAction();
            flow.cancel();
        }
    }

    private Dialog.Layout menuLayout() {
        return Dialog.layout(MENU_TITLE, MENU_LINES, MENU_BUTTONS, worldWidth() / TEXT_LINE, worldHeight() / TEXT_LINE);
    }

    private static List<SelectList.Row> menuRows() {
        List<SelectList.Row> rows = new ArrayList<>();
        for (GlyphOp.Kind kind : DeriveFlow.OPERATIONS) {
            rows.add(new SelectList.Row(DeriveFlow.label(kind), true, false, 0));
        }
        return List.copyOf(rows);
    }

    private void handleScrollKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP)) {
            sheet.scroll().pageUp();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN)) {
            sheet.scroll().pageDown();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.HOME)) {
            sheet.scroll().toTop();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.END)) {
            sheet.scroll().toBottom();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
            notePanel.scrollDisplay(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            notePanel.scrollDisplay(1);
        }
    }

    private void trackPointer() {
        pointer.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(pointer);
        pointerX = (int) Math.floor(pointer.x);
        pointerY = (int) Math.floor(viewport.getWorldHeight() - pointer.y);
        if (wheelTicks != 0) {
            if (notePanel.contains(pointerX, pointerY)) {
                notePanel.scrollDisplay(wheelTicks * WHEEL_ROWS);
            } else if (sheet.contains(pointerX, pointerY)) {
                sheet.scroll().scrollBy(wheelTicks * WHEEL_ROWS);
            }
        }
        wheelTicks = 0;
    }

    private void handleMouse() {
        boolean pressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        boolean clicked = leftClicked();
        int codePoint = codePoints[cursor];
        if (clicked) {
            sheet.clickBar(pointerX, pointerY);
            int slot = sheet.slotAt(pointerX, pointerY);
            if (slot >= 0 && !unselectable[slot]) {
                messages.nextAction();
                if (!flow.isPicking()) {
                    cursor = slot;
                } else if (flow.clickSource(slot)) {
                    confirmSource();
                } else {
                    refreshPick();
                }
            } else if (!flow.isPicking()) {
                int column = grid.columnAt(pointerX);
                int row = grid.rowAt(pointerY);
                if (column >= 0 && column < editor.cellWidth() && row >= 0 && row < editor.cellHeight()) {
                    messages.nextAction();
                    lastProblem = "";
                    stroke.begin(codePoint, column, row);
                }
            }
        } else if (stroke.isActive()) {
            if (pressed) {
                stroke.drag(grid.columnAt(pointerX), grid.rowAt(pointerY));
            } else {
                stroke.end();
            }
        }
    }

    private void handleQuitPrompt() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            save();
            if (!editor.isModified()) {
                onExit.run();
            }
            confirmingQuit = false;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            onExit.run();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            messages.nextAction();
            confirmingQuit = false;
        }
    }

    private void save() {
        messages.nextAction();
        try {
            AtomicFiles.write(dataFile.file().toPath(), editor.toText());
            editor.markSaved();
            messages.ok("Saved " + dataFile.name());
        } catch (java.io.IOException e) {
            messages.warn("Save failed: see log");
            Gdx.app.error(TAG, "Cannot write " + dataFile.path() + ": " + e.getMessage());
        }
    }

    private void draw() {
        ScreenUtils.clear(UiPalette.BG);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        int codePoint = codePoints[cursor];
        int columns = worldWidth() / TEXT_LINE;
        int rows = worldHeight() / TEXT_LINE;
        TopBar.draw(painter, columns, 0,
            List.of("GLYPH WORKBENCH", folderName, "PAINT " + fontSize),
            editor.isModified() ? "● MODIFIED" : "", UiPalette.ORANGE);
        boolean choosingSource = flow.isPicking() || flow.mode() == DeriveFlow.Mode.CONFIRM_REPLACE;
        sheet.draw(cursor, choosingSource ? flow.sourceSlot() : -1);
        ReviewLayout.Rect panel = rects.grid();
        line.setLength(0);
        line.append("U+");
        appendHex(line, codePoint);
        line.append("  ");
        int nameChars = Math.max(1, panel.width() - 6 - line.length());
        line.append(GlyphName.describe(codePoint, nameChars));
        Panel.draw(painter, panel.x(), panel.y(), panel.width(), panel.height(), line.toString(), true);
        grid.draw(codePoint, pickShown);
        preview.draw(rects.preview(), codePoint, pickShown);
        drawReviewNote(codePoint);
        ReviewLayout.Rect strip = rects.messages();
        messages.draw(painter, strip.x(), strip.y(), strip.width(), strip.height());
        StatusBar.draw(painter, columns, rows - 1, choosingSource ? PICK_KEYS : KEYS);
        if (confirmingQuit) {
            Dialog.draw(painter, columns, rows, "UNSAVED CHANGES",
                List.of("Save changes to " + dataFile.name(), "before leaving?"), QUIT_BUTTONS);
        }
        if (flow.mode() == DeriveFlow.Mode.MENU) {
            Dialog.draw(painter, columns, rows, MENU_TITLE, MENU_LINES, MENU_BUTTONS);
            Dialog.Layout menu = menuLayout();
            SelectList.draw(painter, menu.textX(), menu.textY(), MENU_WIDTH, MENU_ROWS, flow.menuIndex());
        } else if (flow.mode() == DeriveFlow.Mode.CONFIRM_REPLACE) {
            describeGlyph(codePoint);
            Dialog.draw(painter, columns, rows, "REPLACE DRAWING",
                List.of(line + " holds a drawing.", "Deriving replaces it (undo restores it)."), REPLACE_BUTTONS);
        }
        rules.draw(worldWidth(), worldHeight(), codePoint);
        batch.end();
    }

    // Read-only: the note and pick the upscale review recorded for this glyph, if it has a note.
    private void drawReviewNote(int codePoint) {
        if (shownNoteFor != codePoint) {
            shownNoteFor = codePoint;
            notePanel.resetDisplayScroll();
        }
        ReviewFile.Answer answer = review.get(codePoint);
        if (answer != null && !answer.note().isEmpty()) {
            notePanel.drawReadOnly(rects.note(), answer.pick(), answer.note());
        } else {
            notePanel.markHidden();
        }
    }

    private static void appendHex(StringBuilder out, int value) {
        int digits = 4;
        while (digits < 6 && (value >>> (digits * 4)) != 0) {
            digits++;
        }
        for (int shift = (digits - 1) * 4; shift >= 0; shift -= 4) {
            out.append(Character.toUpperCase(Character.forDigit((value >>> shift) & 0xF, 16)));
        }
    }

    @Override
    public void hide() {
        batch.dispose();
        whitePixel.dispose();
        source.dispose();
    }
}
