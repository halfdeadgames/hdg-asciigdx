package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
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
import com.github.halfdeadgames.asciigdx.workbench.ui.ScrollText;
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
    private static final List<String> RULE_KEYS = List.of(
        GlyphRules.KEY_GAP, GlyphRules.KEY_ACCENT_INK, GlyphRules.KEY_DESCENDER_INK);
    private static final String KEY_PICK = "pick";
    private static final String KEY_PICK_PROBLEM = "pickProblem";
    private static final String KEY_REVIEW = "review";
    private static final String KEY_GUIDES = "guides";
    private static final String KEY_SCALE = "scale";
    private static final int COLUMNS = PaintSheetView.COLUMNS;
    private static final int WHEEL_ROWS = 3;
    private static final int HELP_PADDING = 1;

    private static final List<StatusBar.Key> KEYS = List.of(
        new StatusBar.Key("D", "DERIVE"), new StatusBar.Key("CTRL+S", "SAVE"),
        new StatusBar.Key("CTRL+Z", "UNDO"), new StatusBar.Key("F1", "RULES"), new StatusBar.Key("F2", "GUIDES"),
        new StatusBar.Key("ESC", "BACK"));
    private static final List<StatusBar.Key> GUIDE_LIST_KEYS = List.of(
        new StatusBar.Key("ENTER", "SAVE"), new StatusBar.Key("ESC", "CANCEL"));
    private static final List<String[]> GUIDE_EDIT_LEGEND = List.of(
        new String[] {"ARROWS", "PICK"}, new String[] {"[ ]", "MOVE"},
        new String[] {"A C B", "ADD"}, new String[] {"DEL", "REMOVE"},
        new String[] {"1-4", "GROUPS"}, new String[] {"L", "RENAME"});
    private static final int GUIDE_LEGEND_ACTION_COLUMN = 8;
    private static final int GUIDE_LEGEND_MIN_LIST_ROWS = 8;
    private static final List<StatusBar.Key> GUIDE_RENAME_KEYS = List.of(
        new StatusBar.Key("ENTER", "OK"), new StatusBar.Key("ESC", "CANCEL"));
    private static final int[] GROUP_KEYS = {Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4};
    private static final int[] GROUP_NUMPAD_KEYS = {
        Input.Keys.NUMPAD_1, Input.Keys.NUMPAD_2, Input.Keys.NUMPAD_3, Input.Keys.NUMPAD_4};
    // The L that starts a rename can also arrive as a typed character a frame later; it must not enter the text.
    private static final int RENAME_KEY_SWALLOW_FRAMES = 2;
    private static final List<StatusBar.Key> PICK_KEYS = List.of(
        new StatusBar.Key("ENTER", "USE SOURCE"), new StatusBar.Key("ARROWS", "MOVE SOURCE"),
        new StatusBar.Key("ESC", "CANCEL"));
    private static final List<StatusBar.Key> OVERLAY_PICK_KEYS = List.of(
        new StatusBar.Key("[ ]", "SHIFT"), new StatusBar.Key("ENTER", "ADD"),
        new StatusBar.Key("BKSP", "UNDO PART"), new StatusBar.Key("F", "FINISH"),
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
    private final KeyRepeat repeatLabelBackspace = new KeyRepeat();
    private int renameKeySwallowFrames;

    private SpriteBatch batch;
    private Texture whitePixel;
    private TextureGlyphSource source;
    private Painter painter;
    private Painter rulesPainter;
    private UiScale uiScale = UiScale.DEFAULT;
    private int autoFactor = UiScale.DEFAULT_FACTOR;
    private Integer overrideFactor;
    private String scaleLabel = "";
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
    private float ghostSeconds;
    private int derivedTextGlyph = -1;
    private int derivedTextSource = -1;
    private String derivedText = "";
    private List<RuleWarning> ruleWarnings = List.of();
    private int ruleGlyph = -1;
    private int ruleRevision = -1;
    private int gapWatchGlyph = -1;
    private boolean gapWatchInk;
    private ReviewFile review = new ReviewFile();
    private PaintGuides guides = PaintGuides.NONE;
    private FileHandle guidesFile;
    private boolean guidesFileUnreadable;
    private GuideListModel guideList;
    private boolean guideListOpen;
    private float guideListSeconds;
    private String guideListTitle = "GUIDES";
    private int shownNoteFor = -1;
    private PaintHelp.State helpShownState;
    private int helpShownChars;
    private int helpShownVisible;
    private List<HelpWrap.Line> helpLines = List.of();
    private final ScrollState helpScroll = new ScrollState(0);
    private ReviewLayout.Rect helpRect;
    private boolean helpShown;
    private boolean[] unselectable;
    private int[] codePoints;
    private int cursor;
    private int titleCodePoint = -1;
    private float titleSeconds;
    private boolean revealCursor = true;
    private boolean confirmingQuit;
    private int wheelTicks;
    private int sidewaysTicks;
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
        rulesPainter = new Painter(batch, whitePixel, source, viewport);
        overrideFactor = launchOverride();
        autoFactor = PaintScale.bestFactor(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
            PaintSheetView.GAP, editor.cellWidth(), editor.cellHeight());
        int firstFactor = chosenFactor(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        showScale(firstFactor);
        uiScale = new UiScale(firstFactor);
        painter.setScale(uiScale);
        rulesPainter.setScale(uiScale);
        sheet = new PaintSheetView(painter, editor, 0);
        applyMinimumWindowSize();
        loadGuides(directory);
        grid = new PaintGridView(painter, editor, guides);
        guideList = new GuideListModel(guides, editor.cellWidth(), editor.cellHeight());
        preview = new PaintPreview(painter, editor);
        PreviewTabs.Tab requestedTab = DevCapture.requestedTab();
        if (requestedTab != null) {
            preview.selectTab(requestedTab);
        }
        rules = new RulesOverlay(rulesPainter, fontDir);
        notePanel = new NotePanel(painter);
        loadReviewNotes(directory);
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                    || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
                float sideways = amountX != 0 ? amountX : shift ? amountY : 0;
                sidewaysTicks += sideways > 0 ? 1 : sideways < 0 ? -1 : 0;
                if (!shift || amountX != 0) {
                    wheelTicks += amountY > 0 ? 1 : amountY < 0 ? -1 : 0;
                }
                return true;
            }

            @Override
            public boolean keyTyped(char character) {
                if (!guideListOpen || !guideList.isRenaming() || rules.isOpen() || controlDown()) {
                    return false;
                }
                boolean startingKey = character == 'l' || character == 'L';
                if (!(renameKeySwallowFrames > 0 && startingKey)) {
                    guideList.typeChar(character);
                }
                return true;
            }
        });
        openDeriveForCapture();
        int guideRequest = DevCapture.guidesRequest();
        if (guideRequest >= 0 && flow.mode() == DeriveFlow.Mode.IDLE) {
            guideList.select(guideRequest);
            openGuideList();
        }
    }

    private int cell() {
        return uiScale.cell();
    }

    private static Integer launchOverride() {
        String text = System.getProperty("workbench.scale");
        if (text == null) {
            return null;
        }
        int factor = PaintScale.parseFactor(text);
        if (factor == 0) {
            Gdx.app.error(TAG, "Ignoring workbench.scale=" + text + ": use a whole number from "
                + UiScale.MIN_FACTOR + " to " + UiScale.MAX_FACTOR);
            return null;
        }
        return factor;
    }

    // The requested override is kept as asked; only what is applied is cut down to fit the window.
    private int chosenFactor(int width, int height) {
        if (overrideFactor == null) {
            return autoFactor;
        }
        return PaintScale.fittingFactor(overrideFactor, width, height, PaintSheetView.GAP,
            editor.cellWidth(), editor.cellHeight());
    }

    private void showScale(int factor) {
        scaleLabel = PaintScale.label(factor, overrideFactor == null);
        if (overrideFactor != null && factor < overrideFactor) {
            messages.setCondition(KEY_SCALE, true,
                "UI " + overrideFactor + "X too big");
        } else {
            messages.clearCondition(KEY_SCALE);
        }
    }

    private static boolean controlDown() {
        return Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
    }

    // Runs before the layout each frame, so a new scale never draws one frame with the old layout.
    private void handleScaleKeys() {
        if (!controlDown()) {
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.EQUALS) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ADD)) {
            overrideFactor = PaintScale.stepped(uiScale.factor(), 1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_SUBTRACT)) {
            overrideFactor = PaintScale.stepped(uiScale.factor(), -1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0)) {
            overrideFactor = null;
        } else {
            return;
        }
        updateScale(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void updateScale(int width, int height) {
        autoFactor = PaintScale.bestFactor(width, height, PaintSheetView.GAP, editor.cellWidth(), editor.cellHeight());
        int factor = chosenFactor(width, height);
        showScale(factor);
        if (factor != uiScale.factor()) {
            uiScale = new UiScale(factor);
            painter.setScale(uiScale);
            rulesPainter.setScale(uiScale);
            revealCursor = true;
        }
    }

    // Only the lwjgl3 desktop window has size limits; GLFW enforces them, also on a window that is already smaller.
    // The limits are the smallest scale's minimum, so the window can shrink far enough to step the scale down.
    private void applyMinimumWindowSize() {
        if (Gdx.graphics instanceof Lwjgl3Graphics graphics) {
            graphics.getWindow().setSizeLimits(
                PaintScale.minimumWidth(UiScale.MIN_FACTOR, PaintSheetView.GAP, editor.cellWidth(), editor.cellHeight()),
                PaintScale.minimumHeight(UiScale.MIN_FACTOR, editor.cellHeight()), -1, -1);
        }
    }

    private void clearWindowSizeLimits() {
        if (Gdx.graphics instanceof Lwjgl3Graphics graphics) {
            graphics.getWindow().setSizeLimits(-1, -1, -1, -1);
        }
    }

    private void openGuideList() {
        messages.nextAction();
        stroke.end();
        guideListOpen = true;
        guideListSeconds = 0f;
        refreshGuideListTitle();
    }

    private void refreshGuideListTitle() {
        guideListTitle = "GUIDES " + (guideList.selectedIndex() + 1) + "/" + guideList.size();
        guideListSeconds = 0f;
    }

    private void closeGuideList() {
        guideListOpen = false;
        wheelTicks = 0;
        guideList = new GuideListModel(guides, editor.cellWidth(), editor.cellHeight());
    }

    // Guide edits are live; the grid and the rule warnings are told so they drop what they cached.
    private void showGuides(PaintGuides shownGuides) {
        guides = shownGuides;
        grid.setGuides(shownGuides);
        ruleRevision = -1;
    }

    // Writes nothing when the list is unchanged, so comments in the file survive a look at the list.
    private void saveGuides() {
        messages.nextAction();
        if (!guideList.isChanged()) {
            messages.ok("Guides unchanged");
            closeGuideList();
            return;
        }
        if (guidesFileUnreadable) {
            messages.warn("Not saved: " + guidesFile.name() + " could not be read and would be overwritten");
            return;
        }
        try {
            AtomicFiles.write(guidesFile.file().toPath(), guideList.guides().toText());
            guidesFileUnreadable = false;
            messages.ok("Saved " + guidesFile.name());
            closeGuideList();
        } catch (java.io.IOException e) {
            messages.warn("Save failed: see log");
            Gdx.app.error(TAG, "Cannot write " + guidesFile.path() + ": " + e.getMessage());
        }
    }

    // Returns whether a key changed the guides.
    private boolean handleGuideEditKeys() {
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
            return false;
        }
        boolean edited = true;
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
            guideList.adjust(-1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            guideList.adjust(1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            guideList.addRow();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            guideList.addColumn();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            guideList.addBar();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL)) {
            guideList.deleteSelected();
        } else if (pressedGroupKey() >= 0) {
            edited = toggleGroup(pressedGroupKey());
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            if (guideList.beginRename()) {
                messages.nextAction();
                renameKeySwallowFrames = RENAME_KEY_SWALLOW_FRAMES;
            }
            edited = false;
        } else {
            edited = false;
        }
        return edited;
    }

    // Index of the group whose key (top row or numpad) was just pressed, or -1.
    private static int pressedGroupKey() {
        for (int i = 0; i < GROUP_KEYS.length; i++) {
            if (Gdx.input.isKeyJustPressed(GROUP_KEYS[i]) || Gdx.input.isKeyJustPressed(GROUP_NUMPAD_KEYS[i])) {
                return i;
            }
        }
        return -1;
    }

    // Returns whether the groups changed; a refusal shows a warning and changes nothing.
    private boolean toggleGroup(int groupIndex) {
        GuideListModel.GroupToggle result = guideList.toggleGroup(groupIndex);
        if (result == GuideListModel.GroupToggle.LAST_GROUP) {
            messages.warn("A guide needs at least one group");
        }
        return result == GuideListModel.GroupToggle.TOGGLED;
    }

    // Only Enter, Esc, and Backspace act here; typed characters arrive through keyTyped. Returning right after
    // Enter or Esc keeps that key from also saving or closing the list in the same frame.
    private void handleRename(float delta) {
        wheelTicks = 0;
        renameKeySwallowFrames = Math.max(0, renameKeySwallowFrames - 1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            guideList.cancelRename();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
            if (guideList.acceptRename()) {
                messages.nextAction();
                showGuides(guideList.guides());
                refreshGuideListTitle();
            } else {
                messages.warn("A label needs at least one character");
            }
            return;
        }
        int backspaces = repeatLabelBackspace.update(Gdx.input.isKeyPressed(Input.Keys.BACKSPACE), delta);
        for (int i = 0; i < backspaces; i++) {
            guideList.backspace();
        }
    }

    // Esc and F2 cancel the edits here, so Esc never reaches the unsaved-changes prompt.
    private void handleGuideList(float delta) {
        if (guideList.isRenaming()) {
            handleRename(delta);
            return;
        }
        repeatLabelBackspace.update(false, 0f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            showGuides(guideList.original());
            closeGuideList();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
            saveGuides();
            return;
        }
        int step = repeatDown.update(Gdx.input.isKeyPressed(Input.Keys.DOWN), delta)
            - repeatUp.update(Gdx.input.isKeyPressed(Input.Keys.UP), delta);
        if (wheelTicks != 0) {
            pointer.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(pointer);
            int column = Math.floorDiv((int) Math.floor(pointer.x), cell());
            int row = Math.floorDiv((int) Math.floor(viewport.getWorldHeight() - pointer.y), cell());
            ReviewLayout.Rect area = rects.note();
            if (column >= area.x() && column < area.x() + area.width() && row >= area.y() && row < area.bottom()) {
                step += wheelTicks;
            }
            wheelTicks = 0;
        }
        if (step != 0) {
            int before = guideList.selectedIndex();
            guideList.move(step);
            if (guideList.selectedIndex() != before) {
                refreshGuideListTitle();
            }
        }
        if (handleGuideEditKeys()) {
            messages.nextAction();
            showGuides(guideList.guides());
            refreshGuideListTitle();
        }
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
        DevCapture.OverlayRequest overlay = DevCapture.parseOverlayRequest(request);
        if (overlay != null) {
            flow.highlightMenuRow(DeriveFlow.OPERATIONS.indexOf(GlyphOp.Kind.OVERLAY));
            beginPick();
            int overlaySlot = slotOf(overlay.codePoint());
            if (overlaySlot >= 0 && !unselectable[overlaySlot]) {
                flow.moveSource(overlaySlot);
                sheet.keepVisible(overlaySlot);
            }
            flow.shiftSource(overlay.shift());
            refreshPick();
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
        } catch (IllegalArgumentException | GdxRuntimeException e) {
            messages.setCondition(KEY_REVIEW, true, "Review file is bad: notes are off");
            Gdx.app.error(TAG, "Cannot read " + reviewFile.path() + ": " + e.getMessage());
        }
    }

    // A missing guides file means no guides; a bad one warns and also means no guides.
    private void loadGuides(FileHandle directory) {
        guidesFile = directory.child(fontSize + ".guides.txt");
        if (!guidesFile.exists()) {
            return;
        }
        try {
            guides = PaintGuides.parse(guidesFile.readString("UTF-8"));
        } catch (IllegalArgumentException | GdxRuntimeException e) {
            guidesFileUnreadable = true;
            messages.setCondition(KEY_GUIDES, true, "Guides file is bad: guides are off");
            Gdx.app.error(TAG, "Cannot read " + guidesFile.path() + ": " + e.getMessage());
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        if (editor != null) {
            updateScale(width, height);
        }
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
        handleScaleKeys();
        PaintColumns.Split split = PaintColumns.split(
            worldWidth(), sheet.slotWidth(), PaintSheetView.GAP, editor.cellWidth(), cell());
        sheet.setVisibleColumns(split.sheetColumns());
        sheet.fitToHeight(worldHeight(), cell(), cursor, revealCursor);
        revealCursor = false;
        rects = PaintLayout.compute(worldWidth() / cell(), worldHeight() / cell(),
            sheet.frameRight() / cell(), split.rightCells(), editor.cellWidth(), editor.cellHeight(),
            cell());
        fitGrid();
        scrollSheetSideways();
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
        if (guideListOpen) {
            handleGuideList(delta);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2) && flow.mode() == DeriveFlow.Mode.IDLE) {
            openGuideList();
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
            if (flow.isOverlay()) {
                handleOverlayKeys();
            }
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
        int left = (panel.x() + 2) * cell();
        int top = (panel.y() + 1 + PaintGridView.LABEL_ROWS_ABOVE) * cell();
        grid.fitTo(left, top, (panel.width() - 3) * cell(), (panel.bottom() - 1) * cell() - top - cell() / 2);
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
                describeGlyph(codePoint).append(" is derived from ");
                appendCodeAndName(line, from);
                derivedText = line.append(": not editable").toString();
            }
            messages.setCondition(KEY_DERIVED, true, derivedText);
        } else {
            messages.clearCondition(KEY_DERIVED);
        }
        if (!derived && !editor.hasGlyph(codePoint) && !flow.isActive()) {
            messages.setCondition(KEY_EMPTY, false, "Empty slot: click the grid to create it");
        } else {
            messages.clearCondition(KEY_EMPTY);
        }
        refreshRuleWarnings(codePoint);
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

    // Rules run again only when the glyph or the editor state changed, and never while a stroke is still being drawn.
    private void refreshRuleWarnings(int codePoint) {
        if (stroke.isActive() || (codePoint == ruleGlyph && editor.revision() == ruleRevision)) {
            return;
        }
        ruleGlyph = codePoint;
        ruleRevision = editor.revision();
        PixelGlyph glyph = editor.isDerived(codePoint) ? null : editor.glyph(codePoint);
        ruleWarnings = GlyphRules.check(codePoint, glyph, editor.bodyWidth(),
            guides.accentRows(), guides.descenderFirstRow());
        boolean ink = false;
        for (String key : RULE_KEYS) {
            messages.clearCondition(key);
        }
        for (RuleWarning warning : ruleWarnings) {
            messages.setCondition(warning.key(), true, warning.message());
            ink |= warning.key().equals(GlyphRules.KEY_GAP);
        }
        if (gapWatchGlyph == codePoint && gapWatchInk && !ink) {
            messages.ok(describeGlyph(codePoint).append(" gap column is empty: back to normal width").toString());
        }
        gapWatchGlyph = codePoint;
        gapWatchInk = ink;
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
            showChangedGlyph(editor.undo(), "Undid change to ");
        }
        for (int i = 0; i < redos; i++) {
            showChangedGlyph(editor.redo(), "Redid change to ");
        }
    }

    private void showChangedGlyph(int codePoint, String notice) {
        if (codePoint == FontEditor.NO_GLYPH) {
            return;
        }
        messages.nextAction();
        line.setLength(0);
        line.append(notice);
        appendCodeAndName(line, codePoint);
        messages.ok(line.toString());
        for (int slot = 0; slot < codePoints.length; slot++) {
            if (codePoints[slot] == codePoint && !unselectable[slot]) {
                cursor = slot;
                sheet.keepVisible(cursor);
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
            sheet.keepVisible(position);
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
                sheet.keepVisible(cursor);
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
        appendCodeAndName(line, codePoint);
        return line;
    }

    private static void appendCodeAndName(StringBuilder out, int codePoint) {
        out.append("U+");
        appendHex(out, codePoint);
        out.append(' ').append(GlyphName.fullName(codePoint));
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
            int column = pointerX / cell();
            int row = SelectList.rowAt(pointerY / cell(), layout.textY(), DeriveFlow.OPERATIONS.size());
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
        boolean overlay = flow.operation() == GlyphOp.Kind.OVERLAY;
        line.setLength(0);
        line.append(DeriveFlow.label(flow.operation())).append(' ');
        appendCodeAndName(line, codePoints[cursor]);
        line.append(':');
        if (withSource) {
            line.append(overlay ? " PART FROM " : " FROM ");
            appendCodeAndName(line, codePoints[flow.sourceSlot()]);
        } else {
            line.append(overlay ? " PICK A PART" : " PICK SOURCE");
        }
        if (overlay) {
            int shift = flow.pendingShift();
            line.append("  SHIFT ").append(shift > 0 ? "+" : "").append(shift)
                .append("  PARTS ").append(flow.parts().size());
        }
        return line.toString();
    }

    private String deriveSummary(int target) {
        describeGlyph(target).append(" (").append(DeriveFlow.label(flow.operation())).append(')');
        if (flow.operation() == GlyphOp.Kind.OVERLAY) {
            return line.append(" FROM ").append(flow.parts().size()).append(" PARTS").toString();
        }
        line.append(" FROM ");
        appendCodeAndName(line, codePoints[flow.sourceSlot()]);
        return line.toString();
    }

    private GlyphOp currentOp() {
        if (flow.operation() == GlyphOp.Kind.OVERLAY) {
            return DeriveFlow.overlayOp(flow.parts(), codePoints);
        }
        return new GlyphOp(flow.operation(), List.of(new GlyphOp.Source(codePoints[flow.sourceSlot()], 0)));
    }

    // Overlay keys: [ and ] shift the pending part, Backspace removes the last part, F finishes.
    private void handleOverlayKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
            messages.nextAction();
            flow.shiftSource(-1);
            refreshPick();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            messages.nextAction();
            flow.shiftSource(1);
            refreshPick();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            messages.nextAction();
            if (flow.removeLastPart()) {
                refreshPick();
            } else {
                messages.warn("No part to remove");
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            finishOverlay();
        }
    }

    private void addOverlayPart() {
        OverlayCheck.Result check = currentOverlayCheck();
        if (check.blocksAdd()) {
            messages.warn(check.message());
        } else if (!pickValid) {
            messages.warn(pickProblemText.isEmpty() ? "This part cannot be added" : pickProblemText);
        } else {
            flow.addPart();
            refreshPick();
        }
    }

    private void finishOverlay() {
        messages.nextAction();
        if (flow.parts().isEmpty()) {
            messages.warn("Add at least one part first");
        } else if (currentOverlayCheck().blocksFinish()) {
            messages.warn(currentOverlayCheck().message());
        } else if (editor.wouldReplaceDrawing(codePoints[cursor])) {
            flow.askReplace();
        } else {
            applyDerive();
        }
    }

    // Previews the committed parts plus the pending one; if the pending one is refused, the valid parts still show.
    private void refreshOverlayPick() {
        int target = codePoints[cursor];
        List<DeriveFlow.Part> all = flow.partsWithPending();
        pickShown = null;
        pickValid = false;
        pickProblemText = "";
        if (!all.isEmpty()) {
            PixelGlyph result = editor.previewDerived(target, DeriveFlow.overlayOp(all, codePoints));
            pickProblemText = editor.problem();
            if (result != null) {
                pickValid = true;
                pickShown = result;
            } else if (!flow.parts().isEmpty()) {
                pickShown = editor.previewDerived(target, DeriveFlow.overlayOp(flow.parts(), codePoints));
            }
            OverlayCheck.Result check = currentOverlayCheck();
            if (check.hasWarning() && check.problem() != OverlayCheck.Problem.SAME_AS_TARGET) {
                pickProblemText = check.message();
                pickValid = false;
            }
            if (pickShown == null) {
                pickShown = blankPick;
            }
            lastProblem = editor.problem();
        }
        pickPromptText = pickPrompt(!flow.sourceIsTarget());
    }

    // Runs only when the source changes. With the source on the target there is nothing to preview yet.
    private void refreshPick() {
        if (flow.operation() == GlyphOp.Kind.OVERLAY) {
            refreshOverlayPick();
            return;
        }
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
        if (flow.isOverlay()) {
            addOverlayPart();
            return;
        }
        if (!pickValid) {
            if (flow.sourceSlot() == cursor) {
                messages.warn(OverlayCheck.SAME_AS_TARGET_MESSAGE);
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
        String summary = deriveSummary(target);
        messages.nextAction();
        if (editor.derive(target, currentOp())) {
            lastProblem = "";
            flow.finish();
            pickShown = null;
            sheet.keepVisible(cursor);
            messages.ok("Derived: " + summary);
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
        sheet.keepVisible(cursor);
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
        ReviewLayout.Rect area = rects.grid();
        return Dialog.layout(MENU_TITLE, MENU_LINES, MENU_BUTTONS, area.x(), area.y(), area.width(), area.height());
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
        if (flow.isOverlay()) {
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
            scrollNoteOrHelp(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            scrollNoteOrHelp(1);
        }
    }

    // The keys follow the note while it overflows; otherwise they scroll the help.
    private void scrollNoteOrHelp(int rows) {
        if (notePanel.canScroll()) {
            notePanel.scrollDisplay(rows);
        } else if (helpShown) {
            helpScroll.scrollBy(rows);
        }
    }

    // Applied before the modal checks so ticks never pile up; the view scroll never moves the cursor.
    private void scrollSheetSideways() {
        if (sidewaysTicks == 0) {
            return;
        }
        pointer.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(pointer);
        int x = (int) Math.floor(pointer.x);
        int y = (int) Math.floor(viewport.getWorldHeight() - pointer.y);
        if (sheet.contains(x, y)) {
            sheet.columnScroll().scrollBy(sidewaysTicks);
        }
        sidewaysTicks = 0;
    }

    private void trackPointer() {
        pointer.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(pointer);
        pointerX = (int) Math.floor(pointer.x);
        pointerY = (int) Math.floor(viewport.getWorldHeight() - pointer.y);
        if (wheelTicks != 0) {
            if (messages.contains(Math.floorDiv(pointerX, cell()), Math.floorDiv(pointerY, cell()))) {
                messages.scrollBy(wheelTicks);
            } else if (notePanel.contains(pointerX, pointerY)) {
                notePanel.scrollDisplay(wheelTicks * WHEEL_ROWS);
            } else if (helpContains(pointerX, pointerY)) {
                helpScroll.scrollBy(wheelTicks * WHEEL_ROWS);
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
            preview.click(rects.preview(), Math.floorDiv(pointerX, cell()), Math.floorDiv(pointerY, cell()));
            sheet.clickBar(pointerX, pointerY);
            sheet.pressColumnBar(pointerX, pointerY);
            messages.click(Math.floorDiv(pointerX, cell()), Math.floorDiv(pointerY, cell()));
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
        } else if (sheet.isDraggingColumnBar()) {
            if (pressed) {
                sheet.dragColumnBar(pointerX);
            } else {
                sheet.endColumnBarDrag();
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
        int columns = worldWidth() / cell();
        int rows = worldHeight() / cell();
        TopBar.draw(painter, columns, 0,
            List.of("GLYPH WORKBENCH", folderName, "PAINT " + fontSize), scaleLabel,
            editor.isModified() ? "● MODIFIED" : "", UiPalette.ORANGE);
        boolean choosingSource = flow.isPicking() || flow.mode() == DeriveFlow.Mode.CONFIRM_REPLACE;
        sheet.draw(cursor, choosingSource ? flow.sourceSlot() : -1);
        ReviewLayout.Rect panel = rects.grid();
        line.setLength(0);
        line.append("U+");
        appendHex(line, codePoint);
        line.append("  ");
        int nameChars = Math.max(1, panel.width() - 6 - line.length());
        if (codePoint != titleCodePoint) {
            titleCodePoint = codePoint;
            titleSeconds = 0f;
        }
        titleSeconds += Gdx.graphics.getDeltaTime();
        line.append(BounceScroll.window(GlyphName.fullName(codePoint), nameChars, titleSeconds));
        Panel.draw(painter, panel.x(), panel.y(), panel.width(), panel.height(), line.toString(), true);
        ghostSeconds += Gdx.graphics.getDeltaTime();
        grid.setHighlight(guideListOpen ? guideList.selectedGuide() : null);
        grid.draw(codePoint, pickShown, pendingGhost(), ghostSeconds);
        preview.draw(rects.preview(), codePoint, pickShown);
        drawReviewNote(codePoint);
        ReviewLayout.Rect strip = rects.messages();
        messages.draw(painter, strip.x(), strip.y(), strip.width(), strip.height(), Gdx.graphics.getDeltaTime());
        StatusBar.draw(painter, columns, rows - 1,
            guideListOpen ? (guideList.isRenaming() ? GUIDE_RENAME_KEYS : GUIDE_LIST_KEYS) : !choosingSource ? KEYS : flow.isOverlay() ? OVERLAY_PICK_KEYS : PICK_KEYS);
        if (confirmingQuit) {
            Dialog.draw(painter, columns, rows, "UNSAVED CHANGES",
                List.of("Save changes to " + dataFile.name(), "before leaving?"), QUIT_BUTTONS);
        }
        if (flow.mode() == DeriveFlow.Mode.MENU) {
            ReviewLayout.Rect area = rects.grid();
            Dialog.draw(painter, columns, rows, MENU_TITLE, MENU_LINES, MENU_BUTTONS,
                area.x(), area.y(), area.width(), area.height());
            Dialog.Layout menu = menuLayout();
            SelectList.draw(painter, menu.textX(), menu.textY(), Math.min(MENU_WIDTH, menu.textWidth()),
                MENU_ROWS, flow.menuIndex());
        } else if (flow.mode() == DeriveFlow.Mode.CONFIRM_REPLACE) {
            line.setLength(0);
            line.append("U+");
            appendHex(line, codePoint);
            ReviewLayout.Rect area = rects.grid();
            Dialog.draw(painter, columns, rows, "REPLACE DRAWING",
                List.of(line + " holds a drawing.", "Deriving replaces it.", "Undo restores it."), REPLACE_BUTTONS,
                area.x(), area.y(), area.width(), area.height());
        }
        if (confirmingQuit || flow.mode() == DeriveFlow.Mode.MENU || flow.mode() == DeriveFlow.Mode.CONFIRM_REPLACE) {
            drawHelp(helpRect);
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
        if (guideListOpen) {
            drawGuideList(rects.note());
            notePanel.markHidden();
            helpShown = false;
            return;
        }
        ReviewFile.Answer answer = review.get(codePoint);
        boolean hasNote = answer != null && !answer.note().isEmpty();
        helpRect = PaintLayout.helpArea(rects, hasNote);
        drawHelp(helpRect);
        if (hasNote) {
            notePanel.drawReadOnly(PaintLayout.noteArea(rects), answer.pick(), answer.note());
        } else {
            notePanel.markHidden();
        }
    }

    // One row per text line; only the selected row bounces when it is too long, the others are cut.
    private void drawGuideList(ReviewLayout.Rect area) {
        Panel.draw(painter, area.x(), area.y(), area.width(), area.height(), guideListTitle, true);
        guideListSeconds += Gdx.graphics.getDeltaTime();
        int visible = area.height() - 2;
        int chars = Math.max(1, area.width() - 2 - 2 * HELP_PADDING);
        if (visible - GUIDE_EDIT_LEGEND.size() >= GUIDE_LEGEND_MIN_LIST_ROWS) {
            visible -= GUIDE_EDIT_LEGEND.size();
            if (!guideList.isRenaming()) {
                drawGuideLegend(area, visible);
            }
        }
        int first = guideList.firstVisible(visible);
        int shown = Math.min(visible, guideList.size() - first);
        for (int i = 0; i < shown; i++) {
            int index = first + i;
            String text = guideList.row(index).text();
            int top = (area.y() + 1 + i) * cell();
            int textX = (area.x() + 1 + HELP_PADDING) * cell();
            if (index == guideList.selectedIndex()) {
                painter.fill(UiPalette.KEYBG, (area.x() + 1) * cell(), top, (area.width() - 2) * cell(), cell());
                painter.text("►", (area.x() + 1) * cell(), top, UiPalette.ACCENT);
                painter.text(BounceScroll.window(text, chars, guideListSeconds), textX, top, UiPalette.ACCENT);
            } else {
                painter.text(text.length() > chars ? text.substring(0, chars) : text, textX, top, UiPalette.TEXT);
            }
        }
    }

    private void drawGuideLegend(ReviewLayout.Rect area, int rowsAbove) {
        int textX = (area.x() + 1 + HELP_PADDING) * cell();
        for (int i = 0; i < GUIDE_EDIT_LEGEND.size(); i++) {
            String[] entry = GUIDE_EDIT_LEGEND.get(i);
            int top = (area.y() + 1 + rowsAbove + i) * cell();
            painter.text(entry[0], textX, top, UiPalette.TITLE);
            painter.text(entry[1], textX + GUIDE_LEGEND_ACTION_COLUMN * cell(), top, UiPalette.TEXT);
        }
    }

    // The help lines are rebuilt only when the state or panel size changes; they wrap, and scroll when taller than the panel.
    private void drawHelp(ReviewLayout.Rect area) {
        Panel.draw(painter, area.x(), area.y(), area.width(), area.height(), "HELP", false);
        PaintHelp.State state = helpState();
        int chars = Math.max(1, area.width() - 2 * HELP_PADDING - 2);
        int visible = Math.max(1, area.height() - 2);
        if (!state.equals(helpShownState)) {
            helpScroll.toTop();
        }
        if (!state.equals(helpShownState) || chars != helpShownChars || visible != helpShownVisible) {
            helpShownState = state;
            helpShownChars = chars;
            helpShownVisible = visible;
            helpLines = HelpWrap.lines(state, chars, visible);
        }
        helpScroll.setVisibleRows(visible);
        helpScroll.setTotalRows(helpLines.size());
        helpRect = area;
        helpShown = true;
        int first = helpScroll.offset();
        int last = Math.min(helpLines.size(), first + visible);
        int textX = (area.x() + 1 + HELP_PADDING) * cell();
        for (int i = first; i < last; i++) {
            HelpWrap.Line line = helpLines.get(i);
            Color color = line.kind() == PaintHelp.Kind.HEADING ? UiPalette.TITLE
                : line.kind() == PaintHelp.Kind.PENDING_PART ? UiPalette.ACCENT
                : line.kind() == PaintHelp.Kind.WARNING ? UiPalette.WARN : UiPalette.TEXT;
            int top = (area.y() + 1 + i - first) * cell();
            if (!line.key().isEmpty()) {
                painter.text(line.key(), textX, top, color == UiPalette.TEXT ? UiPalette.FOCUS : color);
            }
            painter.text(line.action(), textX + line.actionColumn() * cell(), top, color);
        }
        if (helpScroll.needed() && visible >= 2) {
            ScrollText.drawBar(painter, area.x() + area.width() - 2, area.y() + 1, visible,
                helpLines.size(), visible, first);
        }
    }

    private boolean helpContains(int pixelX, int pixelY) {
        return helpShown && helpRect != null
            && pixelX >= helpRect.x() * cell() && pixelX < (helpRect.x() + helpRect.width()) * cell()
            && pixelY >= helpRect.y() * cell() && pixelY < (helpRect.y() + helpRect.height()) * cell();
    }

    private PaintHelp.State helpState() {
        DeriveFlow.Mode mode = flow.mode();
        GlyphOp.Kind operation = mode == DeriveFlow.Mode.MENU ? DeriveFlow.OPERATIONS.get(flow.menuIndex()) : flow.operation();
        if (!flow.isOverlay() || mode != DeriveFlow.Mode.PICK_SOURCE) {
            return new PaintHelp.State(mode, operation, List.of(), null);
        }
        List<PaintHelp.PartView> committed = new ArrayList<>();
        for (DeriveFlow.Part part : flow.parts()) {
            committed.add(new PaintHelp.PartView(codePoints[part.slot()], part.shift()));
        }
        PaintHelp.PartView pending = flow.sourceIsTarget()
            ? null : new PaintHelp.PartView(codePoints[flow.sourceSlot()], flow.pendingShift());
        return new PaintHelp.State(mode, operation, committed, pending, currentOverlayCheck());
    }

    // The pending overlay part shows as a ghost only while its shift puts ink outside the body.
    private PaintGridView.Ghost pendingGhost() {
        if (!flow.isOverlay() || !flow.isPicking() || flow.sourceIsTarget()
            || currentOverlayCheck().problem() != OverlayCheck.Problem.OUTSIDE) {
            return null;
        }
        return new PaintGridView.Ghost(editor.glyph(codePoints[flow.sourceSlot()]).pixels(), flow.pendingShift());
    }

    private OverlayCheck.Result currentOverlayCheck() {
        PixelGlyph pendingSource = editor.glyph(codePoints[flow.sourceSlot()]);
        List<DeriveFlow.Part> committed = flow.parts();
        PixelGlyph firstPart = committed.isEmpty() ? null : editor.glyph(codePoints[committed.get(0).slot()]);
        return OverlayCheck.check(flow.sourceIsTarget(), pendingSource, firstPart, flow.pendingShift());
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
        clearWindowSizeLimits();
        batch.dispose();
        whitePixel.dispose();
        source.dispose();
    }
}
