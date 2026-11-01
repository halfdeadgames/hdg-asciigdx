package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;
import java.util.List;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.halfdeadgames.asciigdx.glyph.ColorKeyConversion;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrder;
import com.github.halfdeadgames.asciigdx.glyph.GlyphOrderText;
import com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource;

/** Shows the default glyph sheet as a grid and lets the user pick one glyph. */
public final class WorkbenchApp extends ApplicationAdapter {

    private static final String SHEET_FOLDER = "com/github/halfdeadgames/asciigdx/fonts/dungeon-mode/";
    private static final int COLUMNS = 16;
    private static final int GLYPH_SIZE = 8;
    private static final int CELL_SCALE = 3;
    private static final int TEXT_LINE = Painter.TEXT_SIZE;
    private static final int SAMPLE_WORDS = 4;
    private static final float PULSE_SECONDS = 1f;
    private static final int MARGIN = 12;
    private static final int OUTLINE = 2;

    private static final Color BACKGROUND = new Color(0.06f, 0.06f, 0.08f, 1f);
    private static final Color CELL_FILL = new Color(0.14f, 0.14f, 0.18f, 1f);
    private static final Color BLANK_FILL = new Color(0.09f, 0.09f, 0.11f, 1f);
    private static final Color GLYPH_TINT = Color.WHITE;
    private static final Color CURSOR_COLOR = Color.YELLOW;
    private static final Color SELECTION_COLOR = Color.CYAN;
    private static final Color TEXT_COLOR = new Color(0.9f, 0.9f, 0.9f, 1f);
    private static final Color PULSE_HIGHLIGHT = Color.ORANGE;
    private static final Color FRAME_COLOR = new Color(0.32f, 0.38f, 0.55f, 1f);
    private static final Color LABEL_COLOR = new Color(0.8f, 0.6f, 1f, 1f);
    private static final Color BAR_COLOR = new Color(0.75f, 0.8f, 0.95f, 1f);
    private static final Color BAR_DIM_COLOR = new Color(0.25f, 0.27f, 0.34f, 1f);
    private static final int BAR_WIDTH = Painter.TEXT_SIZE;
    private static final int WHEEL_ROWS = 3;
    private static final int HINT_LINES = 3;

    private static final String TAG = "Workbench";
    private static final String RESULTS_PATH = "../build/glyph-workbench/results.txt";
    private static final int MARK_SIZE = 6;
    private static final int NONE_CELLS_WIDE = 20;
    private static final int NONE_CELLS_HIGH = 3;
    private static final int MENU_CELLS_WIDE = 24;
    private static final Color HOVER_COLOR = Color.YELLOW;
    private static final Color PICKED_COLOR = new Color(0.25f, 0.9f, 0.35f, 1f);
    private static final Color WARNING_COLOR = new Color(1f, 0.35f, 0.3f, 1f);
    private static final Color ROW_FOCUS_FILL = new Color(0.2f, 0.25f, 0.4f, 1f);
    private static final Color MARK_PERFECT = new Color(0.25f, 0.9f, 0.35f, 1f);
    private static final Color MARK_NEEDS_WORK = Color.ORANGE;
    private static final Color MARK_NONE = new Color(0.95f, 0.25f, 0.25f, 1f);

    private static final Color VIEW_COLOR = new Color(0.4f, 0.75f, 1f, 1f);
    private static final int NOTE_LINES = 7;
    private static final int NOTE_MAX_CELLS = 44;
    private static final int NOTE_TITLE_CELLS = 31;
    private static final String NOTE_TITLE = "NOTE (ENTER SAVES, ESC SKIPS)";
    private static final int CURSOR_THICKNESS = 3;

    private final VerdictMenu menu = new VerdictMenu();
    private final ViewedColumn viewedColumn = new ViewedColumn();
    private final NoteEditor noteEditor = new NoteEditor();
    private GlyphAnswer pendingAnswer;
    private final KeyRepeat repeatLeft = new KeyRepeat();
    private final KeyRepeat repeatRight = new KeyRepeat();
    private final KeyRepeat repeatUp = new KeyRepeat();
    private final KeyRepeat repeatDown = new KeyRepeat();
    private final StringBuilder rowText = new StringBuilder();
    private AnswerStore answers = new AnswerStore();
    private boolean savingEnabled = true;
    private String warning = "";
    private String[] groupPicks = new String[0];
    private String pendingPick;
    private boolean noneVisible;
    private boolean noneHovered;
    private int noneLeft;
    private int noneTop;
    private int menuLeft;
    private int menuRowsTop;
    private int pointerX;
    private int pointerY;

    private final ScreenViewport viewport = new ScreenViewport();
    private final Vector2 pointer = new Vector2();
    private final StringBuilder line = new StringBuilder();
    private final Array<SheetCandidate> candidates = new Array<>();
    private final Color pulseTint = new Color();
    private final DevCapture devCapture = new DevCapture();

    private SpriteBatch batch;
    private Texture whitePixel;
    private Painter painter;
    private ColumnPanel columnPanel;
    private SamplePanel samplePanel;
    private GlyphOrder order;
    private int builtFor = -2;
    private int hoveredColumn = -1;
    private int[] cloud = new int[0];
    private SampleWords.Sample[] words = new SampleWords.Sample[0];
    private float elapsed;
    private TextureGlyphSource source;
    private TextureRegion[] regions;
    private int[] codePoints;
    private boolean[] blank;
    private int rows;
    private SheetGridLayout layout;
    private int cursor;
    private int selected = -1;
    private int lastMouseX = -1;
    private int lastMouseY = -1;
    private ScrollState scroll;
    private final StringBuilder barGlyph = new StringBuilder(1);
    private int wheelTicks;
    private boolean revealCursor = true;
    private int sheetFrameTop;
    private int sheetFrameCellsWide;
    private int sheetFrameCellsHigh;
    private int barLeft;
    private int barCells;

    @Override
    public void create() {
        source = TextureGlyphSource.loadDungeonMode();
        String characterList = Gdx.files.classpath(SHEET_FOLDER + "dungeon-extended.txt").readString("UTF-8");
        order = GlyphOrderText.read(characterList, COLUMNS);
        rows = (order.size() + COLUMNS - 1) / COLUMNS;

        codePoints = new int[order.size()];
        regions = new TextureRegion[COLUMNS * rows];
        for (int slot = 0; slot < order.size(); slot++) {
            codePoints[slot] = order.codePointAt(slot);
            regions[slot] = new TextureRegion(
                source.getTexture(),
                (slot % COLUMNS) * GLYPH_SIZE,
                (slot / COLUMNS) * GLYPH_SIZE,
                GLYPH_SIZE,
                GLYPH_SIZE);
        }
        blank = scanBlankCells(order.size());
        loadAnswers();
        cursor = Math.max(0, CursorNavigator.firstNonBlank(blank));
        int preselected = DevCapture.slotToSelect(codePoints, blank);
        if (preselected >= 0) {
            cursor = preselected;
            selected = preselected;
        }

        sheetFrameTop = MARGIN + TEXT_LINE;
        layout = new SheetGridLayout(
            MARGIN + TEXT_LINE, sheetFrameTop + TEXT_LINE, GLYPH_SIZE * CELL_SCALE, 1, COLUMNS, rows);
        scroll = new ScrollState(rows);
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
                if (character == '\b') {
                    noteEditor.backspace();
                } else {
                    noteEditor.type(character);
                }
                return true;
            }
        });

        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        whitePixel = new Texture(pixel);
        pixel.dispose();

        batch = new SpriteBatch();
        painter = new Painter(batch, whitePixel, source, viewport);
        columnPanel = new ColumnPanel(painter, CELL_FILL, TEXT_COLOR, CURSOR_COLOR, FRAME_COLOR);
        samplePanel = new SamplePanel(painter, order, CELL_FILL, FRAME_COLOR);
        loadCandidates(order.size());
    }

    private void loadCandidates(int slots) {
        addCandidate("A", "NEAREST", "nearest", "dungeon-extended-16", slots);
        addCandidate("B", "SCALE2X", "scale2x", "dungeon-extended-16-scale2x", slots);
        addCandidate("C", "FILL", "scale2x-fill", "dungeon-extended-16-scale2x-fill", slots);
        addCandidate("D", "TRIM", "scale2x-trim", "dungeon-extended-16-scale2x-trim", slots);
        addCandidate("E", "MMPX", "mmpx", "dungeon-extended-16-mmpx", slots);
        addCandidate("F", "SFX", "scale2x-sfx", "dungeon-extended-16-scale2x-sfx", slots);
        addCandidate("G", "LIGHT", "light", "dungeon-extended-16-light", slots);
        addHandCandidate(slots);
    }

    private void addHandCandidate(int slots) {
        FileHandle present = Gdx.files.local("../build/exports/dungeon-extended-16-hand.present.txt");
        if (!present.exists()) {
            return;
        }
        SheetCandidate candidate = SheetCandidate.load("H", "HAND", "hand", "dungeon-extended-16-hand", slots);
        if (candidate == null) {
            return;
        }
        try {
            candidates.add(candidate.withAvailability(GlyphAvailability.listedIn(present.readString("UTF-8"))));
        } catch (IllegalArgumentException | GdxRuntimeException e) {
            candidate.dispose();
            Gdx.app.error(TAG, "Cannot read " + present.name() + ": " + e.getMessage());
        }
    }

    private void addCandidate(String letter, String name, String id, String baseName, int slots) {
        SheetCandidate candidate = SheetCandidate.load(letter, name, id, baseName, slots);
        if (candidate != null) {
            candidates.add(candidate);
        }
    }

    private void loadAnswers() {
        FileHandle file = Gdx.files.local(RESULTS_PATH);
        if (!file.exists()) {
            return;
        }
        try {
            answers = AnswerStore.parse(file.readString("UTF-8"));
        } catch (IllegalArgumentException | GdxRuntimeException e) {
            savingEnabled = false;
            warning = "RESULTS FILE BAD: SAVING OFF";
            Gdx.app.error(TAG, "Cannot read " + RESULTS_PATH + ": " + e.getMessage());
        }
    }

    private void saveAnswers() {
        if (!savingEnabled) {
            return;
        }
        try {
            Gdx.files.local(RESULTS_PATH).writeString(answers.format(codePoints), false, "UTF-8");
        } catch (GdxRuntimeException e) {
            warning = "SAVE FAILED: SEE LOG";
            Gdx.app.error(TAG, "Cannot write " + RESULTS_PATH + ": " + e.getMessage());
        }
    }

    // Merging depends on the glyph, so columns, cloud, and words are rebuilt when the selection changes.
    private void rebuildForSelection() {
        builtFor = selected;
        menu.close();
        pendingPick = null;
        groupPicks = new String[0];
        viewedColumn.reset();
        Array<GlyphColumn> columns = new Array<>();
        columns.add(new GlyphColumn("", "ORIGINAL", regions));
        if (selected < 0) {
            columnPanel.setColumns(columns);
            return;
        }
        int selectedCodePoint = codePoints[selected];
        Array<SheetCandidate> offered = new Array<>();
        for (SheetCandidate candidate : candidates) {
            if (candidate.hasGlyph(selectedCodePoint)) {
                offered.add(candidate);
            }
        }
        int[][] cells = new int[offered.size][];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = offered.get(i).cellPixels(selected);
        }
        int[] shown = CandidateMerge.firstOfEach(cells);
        groupPicks = new String[shown.length];
        for (int g = 0; g < shown.length; g++) {
            SheetCandidate candidate = offered.get(shown[g]);
            groupPicks[g] = candidate.id;
            columns.add(new GlyphColumn((g + 1) + " " + candidate.letter, candidate.name, candidate.regions));
        }
        columnPanel.setColumns(columns);
        int codePoint = codePoints[selected];
        cloud = GlyphCloud.build(
            blank, SamplePanel.CLOUD_COLUMNS, SamplePanel.CLOUD_ROWS, selected, codePoint);
        words = SampleWords.pick(codePoint, SAMPLE_WORDS);
    }

    // Mirrors the loader's conversion so "blank" means what the user sees: no pixel with any alpha.
    private boolean[] scanBlankCells(int slotCount) {
        Pixmap sheet = new Pixmap(Gdx.files.classpath(SHEET_FOLDER + "dungeon-extended.png"));
        try {
            boolean hasTransparentPixel = false;
            for (int y = 0; y < sheet.getHeight() && !hasTransparentPixel; y++) {
                for (int x = 0; x < sheet.getWidth(); x++) {
                    if (ColorKeyConversion.isTransparent(sheet.getPixel(x, y))) {
                        hasTransparentPixel = true;
                        break;
                    }
                }
            }
            int key = sheet.getPixel(0, 0);
            boolean[] result = new boolean[COLUMNS * rows];
            for (int slot = 0; slot < result.length; slot++) {
                result[slot] = slot >= slotCount || !hasVisiblePixel(sheet, slot, hasTransparentPixel, key);
            }
            return result;
        } finally {
            sheet.dispose();
        }
    }

    private static boolean hasVisiblePixel(Pixmap sheet, int slot, boolean hasTransparentPixel, int key) {
        int left = (slot % COLUMNS) * GLYPH_SIZE;
        int top = (slot / COLUMNS) * GLYPH_SIZE;
        for (int y = top; y < top + GLYPH_SIZE; y++) {
            for (int x = left; x < left + GLYPH_SIZE; x++) {
                int pixel = sheet.getPixel(x, y);
                int alpha = hasTransparentPixel ? pixel & 0xFF : ColorKeyConversion.toWhiteWithAlpha(pixel, key) & 0xFF;
                if (alpha > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        handleInput();
        draw();
        devCapture.afterRender();
    }

    // Fits the sheet frame to the window height; the bar and the visible row count follow from it.
    private void updateSheetLayout() {
        float worldHeight = viewport.getWorldHeight();
        sheetFrameCellsHigh = Math.max(3, (int) ((worldHeight - sheetFrameTop - MARGIN) / TEXT_LINE));
        int innerHeight = (sheetFrameCellsHigh - 2) * TEXT_LINE;
        int innerWidth = Painter.roundUpToCells(layout.panelWidth() + BAR_WIDTH);
        sheetFrameCellsWide = innerWidth / TEXT_LINE + 2;
        barLeft = layout.left() + innerWidth - BAR_WIDTH;
        barCells = innerHeight / TEXT_LINE;
        int before = scroll.visibleRows();
        scroll.setVisibleRows(layout.rowsThatFit(innerHeight));
        if (revealCursor || before != scroll.visibleRows()) {
            scroll.keepVisible(cursor / COLUMNS);
            revealCursor = false;
        }
    }

    private int detailLeft() {
        return MARGIN + sheetFrameCellsWide * TEXT_LINE + MARGIN;
    }

    private void handleInput() {
        updateSheetLayout();
        if (noteEditor.isActive()) {
            handleNoteKeys();
            return;
        }
        int cursorBefore = cursor;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (menu.isOpen()) {
                menu.close();
                pendingPick = null;
            } else {
                Gdx.app.exit();
            }
            return;
        }
        float delta = Gdx.graphics.getDeltaTime();
        int lefts = repeatLeft.update(Gdx.input.isKeyPressed(Input.Keys.LEFT), delta);
        for (int i = 0; i < lefts; i++) {
            cursor = CursorNavigator.move(blank, COLUMNS, rows, cursor, -1, 0);
        }
        int rights = repeatRight.update(Gdx.input.isKeyPressed(Input.Keys.RIGHT), delta);
        for (int i = 0; i < rights; i++) {
            cursor = CursorNavigator.move(blank, COLUMNS, rows, cursor, 1, 0);
        }
        if (menu.isOpen()) {
            repeatUp.update(false, delta);
            repeatDown.update(false, delta);
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                menu.moveFocus(-1);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                menu.moveFocus(1);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
                chooseRow(menu.focus());
            }
        } else {
            int ups = repeatUp.update(Gdx.input.isKeyPressed(Input.Keys.UP), delta);
            for (int i = 0; i < ups; i++) {
                cursor = CursorNavigator.move(blank, COLUMNS, rows, cursor, 0, -1);
            }
            int downs = repeatDown.update(Gdx.input.isKeyPressed(Input.Keys.DOWN), delta);
            for (int i = 0; i < downs; i++) {
                cursor = CursorNavigator.move(blank, COLUMNS, rows, cursor, 0, 1);
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            selected = cursor;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            int next = answers.nextUnanswered(codePoints, blank, cursor);
            if (next >= 0) {
                cursor = next;
                selected = next;
            }
        }
        if ((Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL) || Gdx.input.isKeyJustPressed(Input.Keys.DEL))
            && selected >= 0 && answers.has(codePoints[selected])) {
            answers.remove(codePoints[selected]);
            saveAnswers();
            menu.close();
            pendingPick = null;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.T) && !menu.isOpen() && selected >= 0
            && builtFor == selected && answers.has(codePoints[selected])) {
            pendingAnswer = answers.get(codePoints[selected]);
            noteEditor.begin(pendingAnswer.note(), true);
        }
        handleDigits();
        if (cursor != cursorBefore) {
            scroll.keepVisible(cursor / COLUMNS);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP)) {
            scroll.pageUp();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN)) {
            scroll.pageDown();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.HOME)) {
            scroll.toTop();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.END)) {
            scroll.toBottom();
        }

        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.input.getY();
        boolean clicked = Gdx.input.justTouched() && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        boolean moved = mouseX != lastMouseX || mouseY != lastMouseY;
        pointer.set(mouseX, mouseY);
        viewport.unproject(pointer);
        pointerX = (int) Math.floor(pointer.x);
        pointerY = (int) Math.floor(viewport.getWorldHeight() - pointer.y);
        if (wheelTicks != 0) {
            if (pointerX >= MARGIN && pointerX < MARGIN + sheetFrameCellsWide * TEXT_LINE
                && pointerY >= sheetFrameTop && pointerY < sheetFrameTop + sheetFrameCellsHigh * TEXT_LINE) {
                scroll.scrollBy(wheelTicks * WHEEL_ROWS);
            }
            wheelTicks = 0;
        }
        if (clicked) {
            clickScrollBar();
        }
        if (clicked || moved) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            int hovered = layout.cellAt(pointerX, pointerY, scroll.offset(), scroll.visibleRows());
            if (hovered >= 0 && !blank[hovered]) {
                cursor = hovered;
                if (clicked) {
                    selected = hovered;
                }
            }
        }

        hoveredColumn = columnPanel.columnAt(pointerX, pointerY);
        int noneWidth = NONE_CELLS_WIDE * TEXT_LINE;
        noneHovered = noneVisible && pointerX >= noneLeft && pointerX < noneLeft + noneWidth
            && pointerY >= noneTop && pointerY < noneTop + NONE_CELLS_HIGH * TEXT_LINE;
        int hoveredRow = menu.isOpen()
            ? VerdictMenu.rowAt(
                pointerX, pointerY, menuLeft, menuRowsTop, MENU_CELLS_WIDE * TEXT_LINE, TEXT_LINE, menu.rowCount())
            : -1;
        if (hoveredRow >= 0 && moved) {
            menu.setFocus(hoveredRow);
        }
        if (clicked) {
            if (hoveredRow >= 0) {
                chooseRow(hoveredRow);
            } else if (noneHovered && builtFor == selected) {
                startPick(GlyphAnswer.NONE_PICK);
            } else if (builtFor == selected && selected >= 0 && hoveredColumn == 0) {
                viewedColumn.choose(0);
            } else if (builtFor == selected && hoveredColumn >= 1 && hoveredColumn <= groupPicks.length) {
                viewedColumn.choose(hoveredColumn);
                startPick(groupPicks[hoveredColumn - 1]);
            }
        }
    }

    // While typing, only Enter and Escape act; everything else is text or ignored.
    private void handleNoteKeys() {
        float delta = Gdx.graphics.getDeltaTime();
        repeatLeft.update(false, delta);
        repeatRight.update(false, delta);
        repeatUp.update(false, delta);
        repeatDown.update(false, delta);
        wheelTicks = 0;
        hoveredColumn = -1;
        noneHovered = false;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
            finishNote(true);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            finishNote(false);
        }
    }

    // Skipping keeps the verdict; for a saved answer it also keeps the old note.
    private void finishNote(boolean keepText) {
        if (pendingAnswer != null) {
            if (keepText) {
                answers.put(pendingAnswer.withNote(noteEditor.value().trim()));
            } else if (!noteEditor.isEditingExisting()) {
                answers.put(pendingAnswer.withNote(""));
            }
            saveAnswers();
        }
        pendingAnswer = null;
        noteEditor.end();
    }

    private void clickScrollBar() {
        int barTop = layout.top();
        if (!scroll.needed() || pointerX < barLeft || pointerX >= barLeft + BAR_WIDTH
            || pointerY < barTop || pointerY >= barTop + barCells * TEXT_LINE) {
            return;
        }
        int cell = (pointerY - barTop) / TEXT_LINE;
        if (cell == 0) {
            scroll.scrollBy(-1);
        } else if (cell == barCells - 1) {
            scroll.scrollBy(1);
        } else {
            scroll.clickTrack(cell - 1, barCells - 2);
        }
    }

    private void handleDigits() {
        for (int digit = 0; digit <= 9; digit++) {
            if (!Gdx.input.isKeyJustPressed(Input.Keys.NUM_0 + digit)
                && !Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0 + digit)) {
                continue;
            }
            if (menu.isOpen()) {
                chooseRow(digit - 1);
            } else if (selected >= 0 && builtFor == selected) {
                if (digit == 0) {
                    startPick(GlyphAnswer.NONE_PICK);
                } else if (digit <= groupPicks.length) {
                    viewedColumn.choose(digit);
                    startPick(groupPicks[digit - 1]);
                }
            }
            return;
        }
    }

    private void startPick(String pick) {
        pendingPick = pick;
        menu.open(GlyphAnswer.NONE_PICK.equals(pick));
    }

    private void chooseRow(int index) {
        if (menu.choose(index) && selected >= 0 && pendingPick != null) {
            int codePoint = codePoints[selected];
            pendingAnswer = new GlyphAnswer(codePoint, pendingPick, menu.verdict(), menu.reason());
            GlyphAnswer previous = answers.get(codePoint);
            pendingPick = null;
            noteEditor.begin(previous == null ? "" : previous.note(), false);
        }
    }

    private String shownPick() {
        if (menu.isOpen()) {
            return pendingPick;
        }
        if (pendingAnswer != null) {
            return pendingAnswer.pick();
        }
        GlyphAnswer answer = answers.get(codePoints[selected]);
        return answer == null ? null : answer.pick();
    }

    private void draw() {
        elapsed += Gdx.graphics.getDeltaTime();
        if (builtFor != selected) {
            rebuildForSelection();
        }
        ScreenUtils.clear(BACKGROUND);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        drawSheetPanel();
        drawDetailPanel();
        batch.end();
    }

    private void drawSheetPanel() {
        painter.text("GLYPH SHEET", MARGIN, MARGIN, LABEL_COLOR);
        painter.frame(FRAME_COLOR, MARGIN, sheetFrameTop, sheetFrameCellsWide, sheetFrameCellsHigh);
        drawScrollBar();
        int size = layout.cellSize();
        int firstSlot = scroll.offset() * COLUMNS;
        int endSlot = Math.min(blank.length, (scroll.offset() + scroll.visibleRows()) * COLUMNS);
        for (int slot = firstSlot; slot < endSlot; slot++) {
            int x = layout.cellLeft(slot);
            int top = layout.cellTop(slot, scroll.offset());
            fillTop(blank[slot] ? BLANK_FILL : CELL_FILL, x, top, size, size);
            if (!blank[slot]) {
                painter.region(regions[slot], x, top, size, tintFor(slot));
                GlyphAnswer answer = answers.get(codePoints[slot]);
                if (answer != null) {
                    fillTop(markColor(answer.verdict()), x + size - MARK_SIZE - 1, top + 1, MARK_SIZE, MARK_SIZE);
                }
            }
        }
        if (selected >= 0) {
            outline(SELECTION_COLOR, selected, 0, OUTLINE);
        }
        outline(CURSOR_COLOR, cursor, selected == cursor ? OUTLINE : 0, selected == cursor ? 1 : OUTLINE);
    }

    private void drawScrollBar() {
        boolean active = scroll.needed();
        Color arrowColor = active ? BAR_COLOR : BAR_DIM_COLOR;
        int barTop = layout.top();
        painter.text("▲", barLeft, barTop, arrowColor);
        painter.text("▼", barLeft, barTop + (barCells - 1) * TEXT_LINE, arrowColor);
        int trackCells = barCells - 2;
        int thumbStart = scroll.thumbStart(trackCells);
        int thumbEnd = thumbStart + scroll.thumbSize(trackCells);
        for (int i = 0; i < trackCells; i++) {
            boolean thumb = active && i >= thumbStart && i < thumbEnd;
            barGlyph.setLength(0);
            barGlyph.append(thumb ? '█' : '░');
            painter.text(barGlyph, barLeft, barTop + (i + 1) * TEXT_LINE, active ? BAR_COLOR : BAR_DIM_COLOR);
        }
    }

    private static Color markColor(String verdict) {
        if (GlyphAnswer.PERFECT.equals(verdict)) {
            return MARK_PERFECT;
        }
        return GlyphAnswer.NEEDS_WORK.equals(verdict) ? MARK_NEEDS_WORK : MARK_NONE;
    }

    private Color tintFor(int slot) {
        if (slot == selected) {
            return SELECTION_COLOR;
        }
        return slot == cursor ? CURSOR_COLOR : GLYPH_TINT;
    }

    private void outline(Color color, int slot, int inset, int thickness) {
        if (!scroll.isVisible(slot / COLUMNS)) {
            return;
        }
        int size = layout.cellSize() - inset * 2;
        int x = layout.cellLeft(slot) + inset;
        int top = layout.cellTop(slot, scroll.offset()) + inset;
        fillTop(color, x, top, size, thickness);
        fillTop(color, x, top + size - thickness, size, thickness);
        fillTop(color, x, top, thickness, size);
        fillTop(color, x + size - thickness, top, thickness, size);
    }

    private void drawDetailPanel() {
        int x = detailLeft();
        int top = MARGIN;

        noneVisible = false;
        text("SELECTED GLYPH", x, top);
        top += TEXT_LINE;
        if (selected < 0) {
            text("NONE YET", x, top);
        } else {
            line.setLength(0);
            line.append("U+");
            appendHex(line, codePoints[selected]);
            line.append("  INDEX ").append(selected);
            text(line, x, top);
            top += TEXT_LINE + MARGIN;
            String shown = shownPick();
            int pickedColumn = -1;
            for (int g = 0; g < groupPicks.length; g++) {
                if (groupPicks[g].equals(shown)) {
                    pickedColumn = g + 1;
                }
            }
            columnPanel.setHighlights(hoveredColumn, pickedColumn, HOVER_COLOR, PICKED_COLOR);
            columnPanel.setViewed(viewedColumn.resolve(hoveredColumn, columnPanel.count()), VIEW_COLOR);
            painter.text("CANDIDATES", x, top, LABEL_COLOR);
            top += TEXT_LINE;
            int worldHeight = Math.round(viewport.getWorldHeight());
            int belowColumns = MARGIN + TEXT_LINE + SamplePanel.outerHeight(SAMPLE_WORDS)
                + MARGIN + HINT_LINES * TEXT_LINE + MARGIN;
            columnPanel.setMaxOuterHeight(worldHeight - top - belowColumns);
            top = columnPanel.draw(x, top, Math.round(viewport.getWorldWidth()) - MARGIN - x, selected) + MARGIN;
            int pickLeft = x + SamplePanel.outerWidth() + 2 * MARGIN;
            painter.text("IN CONTEXT", x, top, LABEL_COLOR);
            painter.text("VERDICT", pickLeft, top, LABEL_COLOR);
            top += TEXT_LINE;
            drawSamples(x, top);
            drawPickArea(pickLeft, top, GlyphAnswer.NONE_PICK.equals(shown));
        }

        int hintTop = Math.round(viewport.getWorldHeight()) - MARGIN - TEXT_LINE * 2;
        if (!warning.isEmpty()) {
            painter.text(warning, x, hintTop - TEXT_LINE, WARNING_COLOR);
        }
        if (noteEditor.isActive()) {
            text("TYPING A NOTE: ALL OTHER KEYS ARE OFF", x, hintTop);
            text("ENTER SAVES  ESC SKIPS  BACKSPACE DELETES", x, hintTop + TEXT_LINE);
        } else {
            text("ARROWS MOVE  SPACE SELECTS  N NEXT NEW  T NOTE", x, hintTop);
            text("1-9 PICK  0 HAND DRAWN  DEL CLEARS  ESC BACK", x, hintTop + TEXT_LINE);
        }
    }

    private void drawNoteArea(int left, int top) {
        int roomCells = (Math.round(viewport.getWorldWidth()) - MARGIN - left) / TEXT_LINE;
        int cells = Math.max(NOTE_TITLE_CELLS, Math.min(NOTE_MAX_CELLS, roomCells));
        int innerChars = cells - 2;
        if (noteEditor.isActive()) {
            painter.frame(CURSOR_COLOR, left, top, cells, NOTE_LINES + 3);
            text(NOTE_TITLE, left + TEXT_LINE, top + TEXT_LINE);
            List<String> lines = NoteText.wrap(noteEditor.value(), innerChars);
            int textTop = top + 2 * TEXT_LINE;
            for (int i = 0; i < lines.size() && i < NOTE_LINES; i++) {
                text(lines.get(i), left + TEXT_LINE, textTop + i * TEXT_LINE);
            }
            int row = NoteText.cursorRow(lines, innerChars);
            if (row < NOTE_LINES && (int) (elapsed * 2f) % 2 == 0) {
                int column = NoteText.cursorColumn(lines, innerChars);
                fillTop(CURSOR_COLOR, left + TEXT_LINE + column * TEXT_LINE,
                    textTop + (row + 1) * TEXT_LINE - CURSOR_THICKNESS, TEXT_LINE, CURSOR_THICKNESS);
            }
            return;
        }
        GlyphAnswer answer = selected >= 0 ? answers.get(codePoints[selected]) : null;
        if (answer == null || answer.note().isEmpty()) {
            return;
        }
        painter.text("NOTE (T EDITS)", left, top, LABEL_COLOR);
        List<String> lines = NoteText.wrap(answer.note(), cells);
        for (int i = 0; i < lines.size() && i < NOTE_LINES; i++) {
            painter.text(lines.get(i), left, top + (i + 1) * TEXT_LINE, TEXT_COLOR);
        }
    }

    private void drawPickArea(int left, int top, boolean nonePicked) {
        noneVisible = true;
        noneLeft = left;
        noneTop = top;
        Color noneFrame = noneHovered ? HOVER_COLOR : nonePicked ? PICKED_COLOR : FRAME_COLOR;
        painter.frame(noneFrame, left, top, NONE_CELLS_WIDE, NONE_CELLS_HIGH);
        text("0 NEEDS HAND DRAWN", left + TEXT_LINE, top + TEXT_LINE);
        int menuTop = top + NONE_CELLS_HIGH * TEXT_LINE + MARGIN;
        if (!menu.isOpen()) {
            drawNoteArea(left, menuTop);
            return;
        }
        int rowCount = menu.rowCount();
        painter.frame(FRAME_COLOR, left, menuTop, MENU_CELLS_WIDE, rowCount + 3);
        text(menu.title(), left + TEXT_LINE, menuTop + TEXT_LINE);
        menuLeft = left;
        menuRowsTop = menuTop + 2 * TEXT_LINE;
        for (int i = 0; i < rowCount; i++) {
            int rowTop = menuRowsTop + i * TEXT_LINE;
            boolean focused = i == menu.focus();
            rowText.setLength(0);
            rowText.append(focused ? "> " : "  ").append(i + 1).append(' ').append(menu.rowLabel(i));
            if (focused) {
                fillTop(ROW_FOCUS_FILL, left + TEXT_LINE, rowTop, (MENU_CELLS_WIDE - 2) * TEXT_LINE, TEXT_LINE);
            }
            painter.text(rowText, left + TEXT_LINE, rowTop, focused ? CURSOR_COLOR : TEXT_COLOR);
        }
    }

    // Only the target uses the viewed column; the surroundings stay in the original 8x8 font.
    private void drawSamples(int x, int top) {
        int active = viewedColumn.resolve(hoveredColumn, columnPanel.count());
        float wave = 0.5f - 0.5f * MathUtils.cos(MathUtils.PI2 * elapsed / PULSE_SECONDS);
        pulseTint.set(Color.WHITE).lerp(PULSE_HIGHLIGHT, wave);
        Color wordTint = Blink.isOn(elapsed) ? PULSE_HIGHLIGHT : Color.WHITE;
        samplePanel.draw(
            x, top, columnPanel.column(active), columnPanel.column(0), cloud, words, pulseTint, wordTint);
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

    private void text(CharSequence value, int x, int top) {
        painter.text(value, x, top, TEXT_COLOR);
    }

    private void fillTop(Color color, int x, int top, int width, int height) {
        painter.fill(color, x, top, width, height);
    }

    @Override
    public void dispose() {
        for (SheetCandidate candidate : candidates) {
            candidate.dispose();
        }
        batch.dispose();
        whitePixel.dispose();
        source.dispose();
    }
}
