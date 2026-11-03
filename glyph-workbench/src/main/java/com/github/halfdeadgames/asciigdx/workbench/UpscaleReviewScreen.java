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
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontData;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataParser;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphResolver;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGrid;
import com.github.halfdeadgames.asciigdx.workbench.ui.Dialog;
import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;
import com.github.halfdeadgames.asciigdx.workbench.ui.Panel;
import com.github.halfdeadgames.asciigdx.workbench.ui.StatusBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.TopBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reviews upscale candidates for one font: the sheet shows the source font, and the columns show
 * what each method makes of the selected glyph at the target size. Picking a column writes that
 * candidate into the target size file and records the answer, with an optional note, in the review file.
 */
final class UpscaleReviewScreen implements WorkbenchScreen {

    private static final String TAG = "UpscaleReview";
    private static final int CELL = Painter.TEXT_SIZE;
    private static final int WHEEL_ROWS = 3;

    private static final Color CELL_FILL = new Color(0.14f, 0.14f, 0.18f, 1f);
    private static final Color CURSOR_COLOR = Color.YELLOW;
    private static final Color PICKED_COLOR = UiPalette.OK;
    private static final Color NONE_MARK_COLOR = Color.ORANGE;

    private static final List<StatusBar.Key> BROWSE_KEYS = List.of(
        new StatusBar.Key("1-9", "PICK"), new StatusBar.Key("0", "NONE GOOD"),
        new StatusBar.Key("N", "NEXT NEW"), new StatusBar.Key("T", "NOTE"),
        new StatusBar.Key("F1", "RULES"), new StatusBar.Key("ESC", "BACK"));
    private static final List<StatusBar.Key> NOTE_KEYS = List.of(
        new StatusBar.Key("ENTER", "SAVE NOTE"), new StatusBar.Key("ESC", "KEEP OLD"),
        new StatusBar.Key("ARROWS", "MOVE CARET"), new StatusBar.Key("PGUP PGDN", "JUMP"));
    private static final List<Dialog.Button> REPLACE_BUTTONS = List.of(
        new Dialog.Button("Y", "REPLACE", Dialog.Tone.WARN), new Dialog.Button("N", "KEEP", Dialog.Tone.DIM));

    /** One method applied to one source size; {@code fullBySlot} tells which glyphs are full-cell glyphs. */
    private record Candidate(String label, String pickId, UpscaledSheet sheet, boolean[] fullBySlot) {
    }

    private final Runnable onExit;
    private final String fontDir;
    private final UpscalePlan.Choice choice;
    private final ScreenViewport viewport = new ScreenViewport();
    private final Vector2 pointer = new Vector2();
    private final StringBuilder line = new StringBuilder();
    private final DevCapture devCapture = new DevCapture();
    private final KeyRepeat repeatLeft = new KeyRepeat();
    private final KeyRepeat repeatRight = new KeyRepeat();
    private final KeyRepeat repeatUp = new KeyRepeat();
    private final KeyRepeat repeatDown = new KeyRepeat();
    private final KeyRepeat repeatBackspace = new KeyRepeat();
    private final KeyRepeat repeatDelete = new KeyRepeat();
    private final NoteEditor noteEditor = new NoteEditor();
    private final MessageStrip messages = new MessageStrip();
    private final List<Candidate> candidates = new ArrayList<>();
    private final List<Candidate> offered = new ArrayList<>();
    private final List<Candidate> shown = new ArrayList<>();

    private SpriteBatch batch;
    private Texture whitePixel;
    private TextureGlyphSource source;
    private Painter painter;
    private FontEditor sheetFont;
    private PaintSheetView sheet;
    private ColumnPanel columnPanel;
    private NotePanel notePanel;
    private RulesOverlay rules;
    private UpscaledSheet original;
    private UpscaleTarget target;
    private Path targetPath;
    private Path reviewPath;
    private ReviewFile answers = new ReviewFile();
    private ReviewLayout.Result rects;
    private boolean[] unselectable;
    private boolean[] noGlyph;
    private int[] codePoints;
    private int cursor;
    private int titleCodePoint = -1;
    private float titleSeconds;
    private int builtFor = -1;
    private boolean revealCursor = true;
    private int hoveredColumn = -1;
    private int pickedColumn = -1;
    private int replaceColumn = -1;
    private int wheelTicks;
    private int pointerX;
    private int pointerY;

    /**
     * @param fontDir absolute path of the font folder
     * @param choice the font and the sizes to upscale between; must have a source
     */
    UpscaleReviewScreen(Runnable onExit, String fontDir, UpscalePlan.Choice choice) {
        this.onExit = onExit;
        this.fontDir = fontDir;
        this.choice = choice;
    }

    @Override
    public void show() {
        FileHandle directory = Gdx.files.absolute(fontDir);
        List<FontData> sourceData = new ArrayList<>();
        List<Map<Integer, PixelGlyph>> resolvedSources = new ArrayList<>();
        for (int size : choice.sources()) {
            FontData data = FontDataParser.parse(directory.child(size + ".txt").readString("UTF-8"));
            sourceData.add(data);
            resolvedSources.add(GlyphResolver.resolveAll(data));
        }
        loadTargetAndAnswers(directory);
        int primary = choice.sources().size() - 1;
        sheetFont = new FontEditor(sourceData.get(primary));
        codePoints = new int[sheetFont.slotCount()];
        unselectable = new boolean[codePoints.length];
        noGlyph = new boolean[codePoints.length];
        for (int slot = 0; slot < codePoints.length; slot++) {
            codePoints[slot] = sheetFont.codePointAtSlot(slot);
            unselectable[slot] = codePoints[slot] == FontEditor.NO_GLYPH;
            noGlyph[slot] = unselectable[slot] || !sheetFont.hasGlyph(codePoints[slot]);
        }
        cursor = Math.max(0, CursorNavigator.firstNonBlank(unselectable));
        int preselected = DevCapture.slotToSelect(codePoints, unselectable);
        if (preselected >= 0) {
            cursor = preselected;
        }
        buildCandidates(sourceData, resolvedSources, primary);

        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        whitePixel = new Texture(pixel);
        pixel.dispose();
        source = TextureGlyphSource.loadDungeonMode();
        batch = new SpriteBatch();
        painter = new Painter(batch, whitePixel, source, viewport);
        sheet = new PaintSheetView(painter, sheetFont, 0);
        columnPanel = new ColumnPanel(painter, CELL_FILL, UiPalette.TEXT, UiPalette.ACCENT, UiPalette.FRAME);
        notePanel = new NotePanel(painter);
        rules = new RulesOverlay(painter, fontDir);
        refreshMarks();
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                wheelTicks += amountY > 0 ? 1 : amountY < 0 ? -1 : 0;
                return true;
            }

            @Override
            public boolean keyTyped(char character) {
                if (!noteEditor.isActive()) {
                    return false;
                }
                noteEditor.type(character);
                return true;
            }
        });
    }

    private void loadTargetAndAnswers(FileHandle directory) {
        targetPath = Path.of(fontDir, choice.target() + ".txt");
        reviewPath = Path.of(fontDir, choice.target() + ".review.txt");
        FileHandle targetFile = directory.child(choice.target() + ".txt");
        if (targetFile.exists()) {
            target = UpscaleTarget.parse(targetFile.readString("UTF-8"));
        }
        FileHandle reviewFile = directory.child(choice.target() + ".review.txt");
        if (reviewFile.exists()) {
            try {
                answers = ReviewFile.parse(reviewFile.readString("UTF-8"));
            } catch (IllegalArgumentException e) {
                throw new GdxRuntimeException(reviewFile.name() + ": " + e.getMessage(), e);
            }
        }
    }

    // Candidates are made once, for every slot, so changing the selection only picks regions.
    private void buildCandidates(List<FontData> sourceData, List<Map<Integer, PixelGlyph>> resolved, int primary) {
        FontData primaryData = sourceData.get(primary);
        PixelGrid[] originalCells = new PixelGrid[codePoints.length];
        for (int slot = 0; slot < codePoints.length; slot++) {
            PixelGlyph glyph = resolved.get(primary).get(codePoints[slot]);
            originalCells[slot] = glyph == null ? null : glyph.toCell(primaryData.cellWidth());
        }
        original = new UpscaledSheet(originalCells, primaryData.cellWidth(), primaryData.cellHeight());
        boolean several = choice.sources().size() > 1;
        for (int i = 0; i < choice.sources().size(); i++) {
            int size = choice.sources().get(i);
            int factor = choice.factorFrom(size);
            FontData data = sourceData.get(i);
            boolean[] full = new boolean[codePoints.length];
            for (int slot = 0; slot < codePoints.length; slot++) {
                PixelGlyph glyph = resolved.get(i).get(codePoints[slot]);
                full[slot] = glyph != null && glyph.full();
            }
            for (Upscaler.Method method : Upscaler.methodsFor(factor)) {
                PixelGrid[] cells = new PixelGrid[codePoints.length];
                for (int slot = 0; slot < codePoints.length; slot++) {
                    PixelGlyph glyph = resolved.get(i).get(codePoints[slot]);
                    cells[slot] = glyph == null
                        ? null
                        : Upscaler.upscale(glyph, factor, method, data.cellWidth() * factor);
                }
                candidates.add(new Candidate(
                    labelFor(method, factor, size, several),
                    Upscaler.pickId(method, size, several),
                    new UpscaledSheet(cells, data.cellWidth() * factor, data.cellHeight() * factor),
                    full));
            }
        }
    }

    private static String labelFor(Upscaler.Method method, int factor, int size, boolean several) {
        String name = method == Upscaler.Method.NEAREST ? method.label + " " + factor + "X" : method.label;
        return several ? name + " (" + size + ")" : name;
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

    private int columns() {
        return Math.round(viewport.getWorldWidth()) / CELL;
    }

    private int rows() {
        return Math.round(viewport.getWorldHeight()) / CELL;
    }

    private void handleInput(float delta) {
        sheet.fitToHeight(Math.round(viewport.getWorldHeight()), CELL, cursor, revealCursor);
        revealCursor = false;
        rects = ReviewLayout.compute(columns(), rows(), sheet.frameRight() / CELL);
        if (rules.handleInput(delta, wheelTicks)) {
            wheelTicks = 0;
            return;
        }
        if (noteEditor.isActive()) {
            handleNoteKeys(delta);
            return;
        }
        if (replaceColumn >= 0) {
            handleReplacePrompt();
            wheelTicks = 0;
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            onExit.run();
            return;
        }
        moveCursor(delta);
        handleScrollKeys();
        trackPointer();
        handleActions();
    }

    // While typing, the other keys are off; the arrows, Home, End, and the delete keys edit the note.
    private void handleNoteKeys(float delta) {
        int chars = NotePanel.textChars(rects.note().width());
        int rowsShown = NotePanel.editorRows(rects.note().height());
        if (wheelTicks != 0) {
            noteEditor.scrollView(wheelTicks * WHEEL_ROWS);
            wheelTicks = 0;
        }
        int lefts = repeatLeft.update(Gdx.input.isKeyPressed(Input.Keys.LEFT), delta);
        for (int i = 0; i < lefts; i++) {
            noteEditor.caretLeft();
        }
        int rights = repeatRight.update(Gdx.input.isKeyPressed(Input.Keys.RIGHT), delta);
        for (int i = 0; i < rights; i++) {
            noteEditor.caretRight();
        }
        int ups = repeatUp.update(Gdx.input.isKeyPressed(Input.Keys.UP), delta);
        for (int i = 0; i < ups; i++) {
            noteEditor.caretRows(-1, chars);
        }
        int downs = repeatDown.update(Gdx.input.isKeyPressed(Input.Keys.DOWN), delta);
        for (int i = 0; i < downs; i++) {
            noteEditor.caretRows(1, chars);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP)) {
            noteEditor.caretRows(-rowsShown, chars);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN)) {
            noteEditor.caretRows(rowsShown, chars);
        }
        int backspaces = repeatBackspace.update(Gdx.input.isKeyPressed(Input.Keys.BACKSPACE), delta);
        for (int i = 0; i < backspaces; i++) {
            noteEditor.backspace();
        }
        int deletes = repeatDelete.update(Gdx.input.isKeyPressed(Input.Keys.FORWARD_DEL), delta);
        for (int i = 0; i < deletes; i++) {
            noteEditor.deleteForward();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.HOME)) {
            noteEditor.caretToStart();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.END)) {
            noteEditor.caretToEnd();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
            saveNote();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            noteEditor.end();
        }
    }

    private void saveNote() {
        messages.nextAction();
        ReviewFile.Answer answer = answers.get(codePoints[cursor]);
        if (answer != null) {
            answers.put(answer.withNote(noteEditor.value().strip()));
            saveReviewFile();
            notePanel.resetDisplayScroll();
        }
        noteEditor.end();
    }

    private void handleReplacePrompt() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.Y)) {
            int column = replaceColumn;
            replaceColumn = -1;
            commitPick(column);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.N) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            replaceColumn = -1;
            messages.nextAction();
            messages.ok("Kept the existing glyph");
        }
    }

    private void moveCursor(float delta) {
        int before = cursor;
        int rowCount = codePoints.length / PaintSheetView.COLUMNS;
        int lefts = repeatLeft.update(Gdx.input.isKeyPressed(Input.Keys.LEFT), delta);
        for (int i = 0; i < lefts; i++) {
            cursor = CursorNavigator.move(unselectable, PaintSheetView.COLUMNS, rowCount, cursor, -1, 0);
        }
        int rights = repeatRight.update(Gdx.input.isKeyPressed(Input.Keys.RIGHT), delta);
        for (int i = 0; i < rights; i++) {
            cursor = CursorNavigator.move(unselectable, PaintSheetView.COLUMNS, rowCount, cursor, 1, 0);
        }
        int ups = repeatUp.update(Gdx.input.isKeyPressed(Input.Keys.UP), delta);
        for (int i = 0; i < ups; i++) {
            cursor = CursorNavigator.move(unselectable, PaintSheetView.COLUMNS, rowCount, cursor, 0, -1);
        }
        int downs = repeatDown.update(Gdx.input.isKeyPressed(Input.Keys.DOWN), delta);
        for (int i = 0; i < downs; i++) {
            cursor = CursorNavigator.move(unselectable, PaintSheetView.COLUMNS, rowCount, cursor, 0, 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            int next = answers.nextUnanswered(codePoints, noGlyph, cursor);
            if (next >= 0) {
                cursor = next;
            }
        }
        if (cursor != before) {
            messages.nextAction();
            sheet.scroll().keepVisible(cursor / PaintSheetView.COLUMNS);
        }
    }

    // Page and Home/End scroll the sheet; the bracket keys scroll a long saved note.
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
            if (messages.contains(Math.floorDiv(pointerX, CELL), Math.floorDiv(pointerY, CELL))) {
                messages.scrollBy(wheelTicks);
            } else if (notePanel.contains(pointerX, pointerY)) {
                notePanel.scrollDisplay(wheelTicks * WHEEL_ROWS);
            } else if (sheet.contains(pointerX, pointerY)) {
                sheet.scroll().scrollBy(wheelTicks * WHEEL_ROWS);
            }
        }
        wheelTicks = 0;
        hoveredColumn = columnPanel.columnAt(pointerX, pointerY);
    }

    private void handleActions() {
        if (Gdx.input.justTouched() && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            sheet.clickBar(pointerX, pointerY);
            messages.click(Math.floorDiv(pointerX, CELL), Math.floorDiv(pointerY, CELL));
            int slot = sheet.slotAt(pointerX, pointerY);
            if (slot >= 0 && !unselectable[slot]) {
                if (slot != cursor) {
                    messages.nextAction();
                }
                cursor = slot;
            } else if (hoveredColumn >= 1) {
                pick(hoveredColumn);
            }
        }
        for (int digit = 1; digit <= 9; digit++) {
            if ((Gdx.input.isKeyJustPressed(Input.Keys.NUM_0 + digit)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0 + digit)) && digit < columnPanel.count()) {
                pick(digit);
            }
        }
        if ((Gdx.input.isKeyJustPressed(Input.Keys.NUM_0) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0))
            && !noGlyph[cursor]) {
            messages.nextAction();
            recordAnswer(ReviewFile.NONE, -1);
            messages.ok("None good: nothing written to " + choice.target() + ".txt");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.T) && answers.has(codePoints[cursor])) {
            messages.nextAction();
            noteEditor.begin(answers.get(codePoints[cursor]).note(), true);
        }
    }

    private void pick(int column) {
        if (builtFor != cursor || column < 1 || column > shown.size()) {
            return;
        }
        messages.nextAction();
        PixelGrid cell = shown.get(column - 1).sheet.cell(cursor);
        if (target != null && target.wouldReplace(codePoints[cursor], cell)) {
            replaceColumn = column;
            return;
        }
        commitPick(column);
    }

    // Writes the target file first, so an answer is only recorded for a glyph that really was written.
    // A failed write puts the in-memory target back, so it never holds a glyph that is not on disk.
    private void commitPick(int column) {
        messages.nextAction();
        Candidate candidate = shown.get(column - 1);
        PixelGrid cell = candidate.sheet.cell(cursor);
        UpscaleTarget before = target;
        UpscaleTarget.Snapshot snapshot = target == null ? null : target.snapshot();
        try {
            if (target == null) {
                target = UpscaleTarget.empty(cell.getWidth(), cell.getHeight());
            }
            target.put(codePoints[cursor], candidate.fullBySlot[cursor], cell);
            AtomicFiles.write(targetPath, target.toText());
        } catch (IOException | IllegalArgumentException e) {
            if (snapshot != null) {
                target.restore(snapshot);
            } else {
                target = before;
            }
            messages.warn("Write failed: " + e.getMessage());
            Gdx.app.error(TAG, "Cannot write " + targetPath + ": " + e.getMessage());
            return;
        }
        messages.ok("Wrote " + candidate.label + " to " + choice.target() + ".txt");
        recordAnswer(candidate.pickId, column);
    }

    private void recordAnswer(String pick, int column) {
        int codePoint = codePoints[cursor];
        ReviewFile.Answer previous = answers.get(codePoint);
        answers.put(new ReviewFile.Answer(codePoint, pick, previous == null ? "" : previous.note()));
        saveReviewFile();
        pickedColumn = column;
        refreshMarks();
        notePanel.resetDisplayScroll();
        noteEditor.begin(previous == null ? "" : previous.note(), previous != null);
    }

    private void saveReviewFile() {
        try {
            AtomicFiles.write(reviewPath, answers.format());
        } catch (IOException e) {
            messages.warn("Review file not saved: see log");
            Gdx.app.error(TAG, "Cannot write " + reviewPath + ": " + e.getMessage());
        }
    }

    private void refreshMarks() {
        Color[] marks = new Color[codePoints.length];
        for (int slot = 0; slot < marks.length; slot++) {
            ReviewFile.Answer answer = noGlyph[slot] ? null : answers.get(codePoints[slot]);
            if (answer != null) {
                marks[slot] = ReviewFile.NONE.equals(answer.pick()) ? NONE_MARK_COLOR : PICKED_COLOR;
            }
        }
        sheet.setMarks(marks);
    }

    // The columns depend on the glyph, and identical results are shown once.
    private void rebuildColumns() {
        builtFor = cursor;
        pickedColumn = -1;
        notePanel.resetDisplayScroll();
        Array<GlyphColumn> columns = new Array<>();
        columns.add(new GlyphColumn("", "ORIGINAL " + choice.primarySource(), original.regions));
        offered.clear();
        shown.clear();
        for (Candidate candidate : candidates) {
            if (candidate.sheet.hasGlyph(cursor)) {
                offered.add(candidate);
            }
        }
        int[][] cells = new int[offered.size()][];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = offered.get(i).sheet.cellBits(cursor);
        }
        int[] keptIndexes = CandidateMerge.firstOfEach(cells);
        for (int g = 0; g < keptIndexes.length; g++) {
            Candidate candidate = offered.get(keptIndexes[g]);
            shown.add(candidate);
            columns.add(new GlyphColumn(String.valueOf(g + 1), candidate.label, candidate.sheet.regions));
        }
        columnPanel.setColumns(columns);
        pickedColumn = savedColumn(cells, keptIndexes);
    }

    // The saved pick's column; when its result was identical to an earlier candidate, that candidate's column.
    private int savedColumn(int[][] cells, int[] keptIndexes) {
        ReviewFile.Answer answer = answers.get(codePoints[cursor]);
        if (answer == null || ReviewFile.NONE.equals(answer.pick())) {
            return -1;
        }
        for (int i = 0; i < offered.size(); i++) {
            if (offered.get(i).pickId.equals(answer.pick())) {
                for (int g = 0; g < keptIndexes.length; g++) {
                    if (Arrays.equals(cells[keptIndexes[g]], cells[i])) {
                        return g + 1;
                    }
                }
            }
        }
        return -1;
    }

    private void draw() {
        if (builtFor != cursor) {
            rebuildColumns();
        }
        ScreenUtils.clear(UiPalette.BG);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        int codePoint = codePoints[cursor];
        drawTopBar();
        sheet.draw(cursor);
        drawCandidates(codePoint);
        drawNote(codePoint);
        ReviewLayout.Rect strip = rects.messages();
        messages.draw(painter, strip.x(), strip.y(), strip.width(), strip.height(), Gdx.graphics.getDeltaTime());
        StatusBar.draw(painter, columns(), rows() - 1, noteEditor.isActive() ? NOTE_KEYS : BROWSE_KEYS);
        if (replaceColumn >= 0) {
            Dialog.draw(painter, columns(), rows(), "REPLACE GLYPH",
                List.of("REPLACE EXISTING " + target.cellWidth() + "X" + target.cellHeight() + " GLYPH?"),
                REPLACE_BUTTONS);
        }
        rules.draw(Math.round(viewport.getWorldWidth()), Math.round(viewport.getWorldHeight()), codePoint);
        batch.end();
    }

    private void drawTopBar() {
        String status = noGlyph[cursor] ? "NO GLYPH IN SOURCE"
            : answers.isEmpty() ? "" : answers.all().size() + " ANSWERED";
        TopBar.draw(painter, columns(), 0,
            List.of("GLYPH WORKBENCH", choice.folderName().toUpperCase(Locale.ROOT),
                "UPSCALE " + choice.largest() + " -> " + choice.target()),
            status, noGlyph[cursor] ? UiPalette.WARN : UiPalette.TEXT);
    }

    private void drawCandidates(int codePoint) {
        ReviewLayout.Rect area = rects.candidates();
        line.setLength(0);
        line.append("U+");
        appendHex(line, codePoint);
        line.append("  ");
        int nameChars = Math.max(1, area.width() - 6 - line.length());
        if (codePoint != titleCodePoint) {
            titleCodePoint = codePoint;
            titleSeconds = 0f;
        }
        titleSeconds += Gdx.graphics.getDeltaTime();
        line.append(BounceScroll.window(GlyphName.fullName(codePoint), nameChars, titleSeconds));
        Panel.draw(painter, area.x(), area.y(), area.width(), area.height(), line.toString(), true);
        columnPanel.setHighlights(hoveredColumn, pickedColumn, CURSOR_COLOR, PICKED_COLOR);
        columnPanel.setViewed(-1, CURSOR_COLOR);
        columnPanel.setMaxOuterHeight((area.height() - 2) * CELL);
        columnPanel.draw((area.x() + 1) * CELL, (area.y() + 1) * CELL, (area.width() - 2) * CELL, cursor);
    }

    private void drawNote(int codePoint) {
        if (noteEditor.isActive()) {
            notePanel.drawEditor(rects.note(), noteEditor);
            return;
        }
        ReviewFile.Answer answer = answers.get(codePoint);
        if (answer != null) {
            notePanel.drawReadOnly(rects.note(), answer.pick(), answer.note());
        } else {
            notePanel.markHidden();
            Panel.draw(painter, rects.note().x(), rects.note().y(), rects.note().width(), rects.note().height(),
                "REVIEW NOTE   [ ] SCROLLS", false);
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
        for (Candidate candidate : candidates) {
            candidate.sheet.dispose();
        }
        candidates.clear();
        original.dispose();
        batch.dispose();
        whitePixel.dispose();
        source.dispose();
    }
}
