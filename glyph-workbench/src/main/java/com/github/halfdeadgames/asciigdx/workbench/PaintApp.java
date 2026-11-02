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

/** Paint screen: edits one font data file glyph by glyph (step 1: toggle and drag pixels, save). */
public final class PaintApp implements WorkbenchScreen {

    private static final String TAG = "Paint";
    private static final int MARGIN = 12;
    private static final int TEXT_LINE = Painter.TEXT_SIZE;
    private static final int COLUMNS = PaintSheetView.COLUMNS;
    private static final int WHEEL_ROWS = 3;
    private static final String PROMPT = "UNSAVED: S SAVE, D DISCARD, ESC CANCEL";

    private static final Color BACKGROUND = new Color(0.06f, 0.06f, 0.08f, 1f);
    private static final Color TEXT_COLOR = new Color(0.9f, 0.9f, 0.9f, 1f);
    private static final Color LABEL_COLOR = new Color(0.8f, 0.6f, 1f, 1f);
    private static final Color WARNING_COLOR = new Color(1f, 0.35f, 0.3f, 1f);
    private static final Color MODIFIED_COLOR = Color.ORANGE;
    private static final Color FRAME_COLOR = new Color(0.32f, 0.38f, 0.55f, 1f);

    private final ScreenViewport viewport = new ScreenViewport();
    private final Vector2 pointer = new Vector2();
    private final StringBuilder line = new StringBuilder();
    private final DevCapture devCapture = new DevCapture();
    private final KeyRepeat repeatLeft = new KeyRepeat();
    private final KeyRepeat repeatRight = new KeyRepeat();
    private final KeyRepeat repeatUp = new KeyRepeat();
    private final KeyRepeat repeatDown = new KeyRepeat();

    private SpriteBatch batch;
    private Texture whitePixel;
    private TextureGlyphSource source;
    private Painter painter;
    private FileHandle dataFile;
    private String title;
    private FontEditor editor;
    private PaintStroke stroke;
    private PaintSheetView sheet;
    private PaintGridView grid;
    private PaintPreview preview;
    private RulesOverlay rules;
    private NoteBox noteBox;
    private ReviewFile review = new ReviewFile();
    private int shownNoteFor = -1;
    private boolean[] unselectable;
    private int[] codePoints;
    private int cursor;
    private boolean revealCursor = true;
    private boolean confirmingQuit;
    private String message = "";
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
        title = "PAINT " + directory.name().toUpperCase() + " " + fontSize;
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
        sheet = new PaintSheetView(painter, editor, MARGIN, MARGIN + 2 * TEXT_LINE);
        grid = new PaintGridView(painter, editor, PaintGuides.saiksistt(editor.cellWidth()));
        preview = new PaintPreview(painter, editor);
        rules = new RulesOverlay(painter, fontDir);
        noteBox = new NoteBox(painter, FRAME_COLOR, TEXT_COLOR, LABEL_COLOR, MODIFIED_COLOR);
        loadReviewNotes(directory);
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                wheelTicks += amountY > 0 ? 1 : amountY < 0 ? -1 : 0;
                return true;
            }
        });
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
            message = "REVIEW FILE BAD: NOTES OFF";
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
        sheet.fitToHeight(worldHeight(), MARGIN, cursor, revealCursor);
        revealCursor = false;
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (editor.isModified()) {
                confirmingQuit = true;
                stroke.end();
            } else {
                onExit.run();
            }
            return;
        }
        moveCursor(delta);
        handleScrollKeys();
        trackPointer();
        handleMouse();
    }

    private void moveCursor(float delta) {
        int before = cursor;
        int rows = codePoints.length / COLUMNS;
        int lefts = repeatLeft.update(Gdx.input.isKeyPressed(Input.Keys.LEFT), delta);
        for (int i = 0; i < lefts; i++) {
            cursor = CursorNavigator.move(unselectable, COLUMNS, rows, cursor, -1, 0);
        }
        int rights = repeatRight.update(Gdx.input.isKeyPressed(Input.Keys.RIGHT), delta);
        for (int i = 0; i < rights; i++) {
            cursor = CursorNavigator.move(unselectable, COLUMNS, rows, cursor, 1, 0);
        }
        int ups = repeatUp.update(Gdx.input.isKeyPressed(Input.Keys.UP), delta);
        for (int i = 0; i < ups; i++) {
            cursor = CursorNavigator.move(unselectable, COLUMNS, rows, cursor, 0, -1);
        }
        int downs = repeatDown.update(Gdx.input.isKeyPressed(Input.Keys.DOWN), delta);
        for (int i = 0; i < downs; i++) {
            cursor = CursorNavigator.move(unselectable, COLUMNS, rows, cursor, 0, 1);
        }
        if (cursor != before) {
            stroke.end();
            message = "";
            sheet.scroll().keepVisible(cursor / COLUMNS);
        }
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
            noteBox.scrollDisplay(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            noteBox.scrollDisplay(1);
        }
    }

    private void trackPointer() {
        pointer.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(pointer);
        pointerX = (int) Math.floor(pointer.x);
        pointerY = (int) Math.floor(viewport.getWorldHeight() - pointer.y);
        if (wheelTicks != 0) {
            if (noteBox.contains(pointerX, pointerY)) {
                noteBox.scrollDisplay(wheelTicks * WHEEL_ROWS);
            } else if (sheet.contains(pointerX, pointerY)) {
                sheet.scroll().scrollBy(wheelTicks * WHEEL_ROWS);
            }
        }
        wheelTicks = 0;
    }

    private void handleMouse() {
        boolean pressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        boolean clicked = Gdx.input.justTouched() && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        int codePoint = codePoints[cursor];
        if (clicked) {
            sheet.clickBar(pointerX, pointerY);
            int slot = sheet.slotAt(pointerX, pointerY);
            if (slot >= 0 && !unselectable[slot]) {
                cursor = slot;
                message = "";
            } else {
                int column = grid.columnAt(pointerX);
                int row = grid.rowAt(pointerY);
                if (column >= 0 && column < editor.cellWidth() && row >= 0 && row < editor.cellHeight()) {
                    message = "";
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
            confirmingQuit = false;
        }
    }

    private void save() {
        try {
            AtomicFiles.write(dataFile.file().toPath(), editor.toText());
            editor.markSaved();
            message = "SAVED";
        } catch (java.io.IOException e) {
            message = "SAVE FAILED: SEE LOG";
            Gdx.app.error(TAG, "Cannot write " + dataFile.path() + ": " + e.getMessage());
        }
    }

    private void draw() {
        ScreenUtils.clear(BACKGROUND);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        int codePoint = codePoints[cursor];
        drawTitle();
        sheet.draw(cursor);
        int middleLeft = sheet.frameRight() + MARGIN;
        int previewLeft = worldWidth() - MARGIN - PaintPreview.CELLS_WIDE * TEXT_LINE;
        int headerTop = MARGIN + TEXT_LINE;
        drawGlyphInfo(middleLeft, headerTop, codePoint, previewLeft - MARGIN - middleLeft);
        int gridTop = headerTop + 2 * TEXT_LINE + MARGIN + PaintGridView.LABEL_ROWS_ABOVE * TEXT_LINE;
        int statusTop = worldHeight() - MARGIN - 3 * TEXT_LINE;
        int boxTop = statusTop - MARGIN - NoteBox.heightPixels();
        int gridBottom = review.isEmpty() ? statusTop - MARGIN : boxTop - MARGIN;
        grid.fitTo(middleLeft + MARGIN, gridTop, previewLeft - MARGIN - middleLeft - MARGIN, gridBottom - gridTop);
        grid.draw(codePoint);
        preview.draw(previewLeft, headerTop, codePoint);
        drawReviewNote(middleLeft, boxTop, previewLeft - MARGIN - middleLeft, codePoint);
        drawStatus(middleLeft, statusTop, codePoint, worldWidth() - MARGIN - middleLeft);
        rules.draw(worldWidth(), worldHeight(), codePoint);
        batch.end();
    }

    // Read-only: the note and pick the upscale review recorded for this glyph, if it has a note.
    private void drawReviewNote(int x, int top, int available, int codePoint) {
        if (shownNoteFor != codePoint) {
            shownNoteFor = codePoint;
            noteBox.resetDisplayScroll();
        }
        ReviewFile.Answer answer = review.get(codePoint);
        if (answer != null && !answer.note().isEmpty()) {
            noteBox.drawReadOnly(x, top, NoteBox.cellsWideFor(available), answer.pick(), answer.note());
        } else {
            noteBox.markHidden();
        }
    }

    private void drawTitle() {
        line.setLength(0);
        line.append(title).append("   F1 RULES");
        painter.text(line, MARGIN, MARGIN, LABEL_COLOR);
        if (editor.isModified()) {
            painter.text("MODIFIED", MARGIN + (line.length() + 3) * TEXT_LINE, MARGIN, MODIFIED_COLOR);
        }
    }

    private void drawGlyphInfo(int x, int top, int codePoint, int width) {
        line.setLength(0);
        line.append("U+");
        appendHex(line, codePoint);
        painter.text(line, x, top, TEXT_COLOR);
        painter.text(GlyphName.describe(codePoint, Math.max(1, width / TEXT_LINE)), x, top + TEXT_LINE, TEXT_COLOR);
    }

    private void drawStatus(int x, int top, int codePoint, int width) {
        String text;
        Color color = TEXT_COLOR;
        if (confirmingQuit) {
            text = PROMPT;
            color = WARNING_COLOR;
        } else if (!message.isEmpty()) {
            text = message;
        } else if (!editor.problem().isEmpty()) {
            text = editor.problem();
            color = WARNING_COLOR;
        } else if (editor.isDerived(codePoint)) {
            line.setLength(0);
            line.append("DERIVED FROM U+");
            appendHex(line, editor.derivedFrom(codePoint));
            text = line.toString();
        } else if (!editor.hasGlyph(codePoint)) {
            text = "EMPTY SLOT: CLICK THE GRID TO CREATE IT";
        } else {
            text = "";
        }
        int chars = Math.max(1, width / TEXT_LINE);
        painter.text(text.length() > chars ? text.substring(0, chars) : text, x, top, color);
        painter.text("ARROWS MOVE  CLICK TOGGLES  DRAG PAINTS", x, top + TEXT_LINE, TEXT_COLOR);
        painter.text("CTRL+S SAVES  [ ] SCROLL NOTE  F1 RULES  ESC LEAVES", x, top + 2 * TEXT_LINE, TEXT_COLOR);
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
