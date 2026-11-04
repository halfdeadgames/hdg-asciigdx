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
import com.github.halfdeadgames.asciigdx.workbench.ui.Dialog;
import com.github.halfdeadgames.asciigdx.workbench.ui.MenuBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.MenuBarView;
import com.github.halfdeadgames.asciigdx.workbench.ui.MenuKeys;
import com.github.halfdeadgames.asciigdx.workbench.ui.MenuPointer;
import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;
import com.github.halfdeadgames.asciigdx.workbench.ui.Panel;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScrollText;
import com.github.halfdeadgames.asciigdx.workbench.ui.StatusBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.TopBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/** Paint screen: edits one font data file glyph by glyph (step 1: toggle and drag pixels, save). */
public final class PaintApp implements WorkbenchScreen {

    private static final String TAG = "Paint";
    private static final String LEAVE_QUESTION = "before leaving?";
    private static final String SWITCH_SIZE_QUESTION = "before switching size?";
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
    private static final int MENU_ROW = 0;
    private static final int CRUMB_ROW = 1;
    static final int TEXT_PADDING = 1;

    private static final List<StatusBar.Key> KEYS = List.of(
        new StatusBar.Key("D", "DERIVE"), new StatusBar.Key("CTRL+S", "SAVE"),
        new StatusBar.Key("CTRL+Z", "UNDO"), new StatusBar.Key("F1", "RULES"), new StatusBar.Key("F2", "GUIDES"),
        new StatusBar.Key("ESC", "BACK"));
    private static final List<StatusBar.Key> PICK_KEYS = List.of(
        new StatusBar.Key("ENTER", "USE SOURCE"), new StatusBar.Key("ARROWS", "MOVE SOURCE"),
        new StatusBar.Key("ESC", "CANCEL"));
    private static final List<StatusBar.Key> MENU_KEYS = List.of(
        new StatusBar.Key("ARROWS", "CHOOSE"), new StatusBar.Key("ENTER", "SELECT"),
        new StatusBar.Key("ESC", "CANCEL"));
    private static final List<StatusBar.Key> REPLACE_KEYS = List.of(
        new StatusBar.Key("R", "REPLACE"), new StatusBar.Key("ESC", "BACK"));
    private static final List<StatusBar.Key> OVERLAY_PICK_KEYS = List.of(
        new StatusBar.Key("[ ]", "SHIFT"), new StatusBar.Key("ENTER", "ADD"),
        new StatusBar.Key("BKSP", "UNDO PART"), new StatusBar.Key("F", "FINISH"),
        new StatusBar.Key("ESC", "CANCEL"));
    private static final List<Dialog.Button> QUIT_BUTTONS = List.of(
        new Dialog.Button("S", "SAVE", Dialog.Tone.OK), new Dialog.Button("D", "DISCARD", Dialog.Tone.WARN),
        new Dialog.Button("ESC", "CANCEL", Dialog.Tone.DIM));

    private final DeriveFlow flow = new DeriveFlow();
    // Rebuilt only when the auto-size mark changes, which never happens while a menu is open.
    private MenuBar<PaintMenu.Action> menuBar = PaintMenu.create(true);
    private boolean menuMarksAutoScale = true;
    // A menu state requested by the dev hook stays as set until the pointer moves, a click lands, or Esc is pressed.
    private boolean menuPinned;
    private boolean menuPinCellKnown;
    private int menuPinX;
    private int menuPinY;
    private final MenuPointer<PaintMenu.Action> menuPointer = new MenuPointer<>(MENU_ROW);
    private final MenuKeys<PaintMenu.Action> menuKeys = new MenuKeys<>();
    // Alt as the input events saw it, because typed characters arrive before this frame's polling can say so.
    private boolean altSeenDown;
    private final Predicate<PaintMenu.Action> menuAvailability =
        action -> PaintMenu.isAvailable(action, menuContext());
    private PaintDerivePick derivePick;
    private PaintDerive derive;
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
    private final NewGlyphDialog newGlyph = new NewGlyphDialog(this::addGlyphSlot, codePoint -> slotOf(codePoint) >= 0);
    private final NewSizeDialog newSize = new NewSizeDialog(this::confirmNewSize);
    private final PaintNewFont newFont;
    private PaintGuideListPanel guideListPanel;

    private SpriteBatch batch;
    private Texture whitePixel;
    private TextureGlyphSource source;
    private Painter painter;
    private Painter rulesPainter;
    private PaintUiScale uiScale;
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
    private final PaintDeriveMenu deriveMenu = new PaintDeriveMenu(flow, messages);
    // Static so a paint screen opened later in the same run does not open the capture dialog again.
    private static boolean newFontCaptureOpened;
    private PaintLayout.Result rects;
    private boolean layoutBarShown;
    private final ControlsOverlay controls = new ControlsOverlay();
    private String lastProblem = "";
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
    private PaintGuidesFile guidesFile;
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
    private final WheelTicks wheelTicks = new WheelTicks();
    private PaintScrollRouting scrollRouting;
    private int pointerX;
    private int pointerY;

    private final Runnable onExit;
    private final String fontDir;
    private final String fontSize;
    private final IntConsumer openSize;
    // What the unsaved-changes prompt continues with once the changes are saved or discarded.
    private Runnable afterUnsavedPrompt = () -> { };
    private String unsavedQuestion = LEAVE_QUESTION;

    /**
     * @param onExit called when the user leaves the screen with Esc (after the unsaved prompt)
     * @param fontDir absolute path of the font folder
     * @param fontSize size name, which is also the data file name without {@code .txt}
     * @param openSize called with the height of a size file this screen just created, to paint it next
     * @param openFont called with the folder and first size height of a font this screen just created, to paint it next
     */
    public PaintApp(Runnable onExit, String fontDir, String fontSize, IntConsumer openSize,
                    BiConsumer<Path, Integer> openFont) {
        this.onExit = onExit;
        this.fontDir = fontDir;
        this.fontSize = fontSize;
        this.openSize = openSize;
        this.newFont = new PaintNewFont(this::fontsRoot, this::runAfterUnsavedCheck, openFont, messages);
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
        derivePick = new PaintDerivePick(editor, flow);
        derive = new PaintDerive(flow, derivePick, deriveMenu, editor, messages, new DeriveHost());

        readSlots();
        cursor = editor.startSlot();
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
        uiScale = new PaintUiScale(editor.cellWidth(), editor.cellHeight(), launchOverride());
        syncScaleMark();
        uiScale.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        showScaleMessage();
        painter.setScale(uiScale.scale());
        rulesPainter.setScale(uiScale.scale());
        sheet = new PaintSheetView(painter, editor, 0, PaintLayout.TOP_ROWS);
        applyMinimumWindowSize();
        loadGuides(directory);
        grid = new PaintGridView(painter, editor, guides);
        guideListPanel = new PaintGuideListPanel(painter, messages, guidesFile, editor, repeatUp, repeatDown,
            new GuideListHost());
        preview = new PaintPreview(painter, editor);
        PreviewTabs.Tab requestedTab = DevCapture.requestedTab();
        if (requestedTab != null) {
            preview.selectTab(requestedTab);
        }
        rules = new RulesOverlay(rulesPainter, fontDir);
        notePanel = new NotePanel(painter);
        scrollRouting = new PaintScrollRouting(messages, notePanel, helpScroll, sheet, wheelTicks, new ScrollHost());
        loadReviewNotes(directory);
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (menuPointer.isCapturing(menuBar)) {
                    return true;
                }
                boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                    || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
                wheelTicks.add(amountX, amountY, shift);
                return true;
            }

            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ALT_LEFT || keycode == Input.Keys.ALT_RIGHT) {
                    altSeenDown = true;
                }
                return false;
            }

            @Override
            public boolean keyUp(int keycode) {
                if (keycode == Input.Keys.ALT_LEFT || keycode == Input.Keys.ALT_RIGHT) {
                    altSeenDown = false;
                }
                return false;
            }

            @Override
            public boolean keyTyped(char character) {
                if (menuPointer.isCapturing(menuBar) || altSeenDown || altDown()) {
                    return true;
                }
                if (newGlyph.isOpen()) {
                    newGlyph.type(character);
                    return true;
                }
                if (newFont.isOpen()) {
                    newFont.type(character);
                    return true;
                }
                if (!guideListPanel.isRenaming() || rules.isOpen() || controls.isOpen() || controlDown()) {
                    return false;
                }
                guideListPanel.typeChar(character);
                return true;
            }
        });
        openDeriveForCapture();
        guideListPanel.openForCapture(flow.mode() == DeriveFlow.Mode.IDLE);
        openMenuForCapture();
        openNewGlyphForCapture();
        openNewSizeForCapture();
        openNewFontForCapture();
    }

    private void readSlots() {
        codePoints = new int[editor.slotCount()];
        unselectable = new boolean[editor.slotCount()];
        for (int slot = 0; slot < codePoints.length; slot++) {
            codePoints[slot] = editor.codePointAtSlot(slot);
            unselectable[slot] = codePoints[slot] == FontEditor.NO_GLYPH;
        }
    }

    private void openNewGlyphForCapture() {
        String typed = DevCapture.newGlyphRequest();
        if (typed == null || flow.isActive() || guideListPanel.isOpen() || confirmingQuit) {
            return;
        }
        openNewGlyph();
        newGlyph.typeForCapture(typed);
    }

    private void openNewSizeForCapture() {
        int moves = DevCapture.newSizeRequest();
        if (moves < 0 || flow.isActive() || guideListPanel.isOpen() || confirmingQuit || newGlyph.isOpen()) {
            return;
        }
        try {
            openNewSize(NewSizePlan.options(java.nio.file.Path.of(fontDir)));
        } catch (java.io.IOException | IllegalStateException e) {
            Gdx.app.error(TAG, "Cannot list sizes: " + e.getMessage());
            return;
        }
        newSize.moveForCapture(moves);
    }

    private void openNewFontForCapture() {
        if (newFontCaptureOpened || flow.isActive() || guideListPanel.isOpen() || confirmingQuit || newGlyph.isOpen() || newSize.isOpen()) {
            return;
        }
        stroke.end();
        newFontCaptureOpened = newFont.openForCapture();
    }

    private void openNewFont() {
        stroke.end();
        newFont.open();
    }

    private Path fontsRoot() {
        return Path.of(fontDir).toAbsolutePath().getParent();
    }

    private void openNewSize(List<NewSizePlan.SizeOption> options) {
        stroke.end();
        updatePointerCell();
        newSize.open(options, pointerX, pointerY);
    }

    private void handleNewSize() {
        wheelTicks.clear();
        if (newSize.handleKeys()) {
            return;
        }
        updatePointerCell();
        newSize.handlePointer(leftClicked(), pointerX, pointerY, cell(), rects.grid());
    }

    private void openNewSizeFromMenu() {
        try {
            openNewSize(NewSizePlan.options(java.nio.file.Path.of(fontDir)));
        } catch (java.io.IOException | IllegalStateException e) {
            Gdx.app.error(TAG, "Cannot list sizes: " + e.getMessage());
            messages.nextAction();
            messages.warn("Cannot list sizes: " + e.getMessage());
        }
    }

    // Unsaved changes are asked about after the pick, so cancelling the prompt loses nothing and creates nothing.
    private void confirmNewSize(NewSizePlan.SizeOption chosen) {
        runAfterUnsavedCheck(() -> createSize(chosen), SWITCH_SIZE_QUESTION);
    }

    private void createSize(NewSizePlan.SizeOption chosen) {
        messages.nextAction();
        try {
            NewSizePlan.create(java.nio.file.Path.of(fontDir), chosen.height());
        } catch (java.nio.file.FileAlreadyExistsException e) {
            messages.warn("Size " + NewSizePrompt.label(chosen) + " already exists");
            return;
        } catch (java.io.IOException | RuntimeException e) {
            Gdx.app.error(TAG, "Cannot create size " + chosen.height() + ": " + e.getMessage());
            messages.warn("Cannot create " + NewSizePrompt.label(chosen) + ": " + e.getMessage());
            return;
        }
        openSize.accept(chosen.height());
    }

    private void openNewGlyph() {
        stroke.end();
        newGlyph.open();
    }

    private void handleNewGlyph(float delta) {
        wheelTicks.clear();
        newGlyph.handle(delta);
    }

    // Later slots shift when a slot is inserted, so everything indexed by slot is rebuilt before the cursor moves.
    private void addGlyphSlot(int codePoint) {
        boolean existed = slotOf(codePoint) >= 0;
        editor.addSlot(codePoint);
        readSlots();
        sheet.refreshSlots();
        int slot = slotOf(codePoint);
        if (slot < 0) {
            return;
        }
        cursor = slot;
        sheet.keepVisible(cursor);
        revealCursor = true;
        messages.nextAction();
        line.setLength(0);
        GlyphLabel.appendCodeAndName(line, codePoint);
        line.append(existed ? " already has a slot" : " added");
        messages.ok(line.toString());
    }

    private void openMenuForCapture() {
        if (DevCapture.opensControls(DevCapture.menuRequest())) {
            controls.open();
            return;
        }
        DevCapture.MenuRequest request = DevCapture.parseMenuRequest(DevCapture.menuRequest());
        if (request == null) {
            return;
        }
        menuPinned = true;
        MenuCaptureHook.apply(request, menuBar, menuAvailability, cell());
    }

    private int cell() {
        return uiScale.scale().cell();
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

    private void showScaleMessage() {
        if (uiScale.overrideTooBig()) {
            messages.setCondition(KEY_SCALE, true,
                "UI " + uiScale.overrideFactor() + "X too big");
        } else {
            messages.clearCondition(KEY_SCALE);
        }
    }

    private static boolean altDown() {
        return Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT);
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
            stepScale(1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_SUBTRACT)) {
            stepScale(-1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0)) {
            useAutoScale();
        }
    }

    private void stepScale(int delta) {
        int oldFactor = uiScale.factor();
        uiScale.step(delta, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        applyScaleChange(oldFactor);
        syncScaleMark();
    }

    private void useAutoScale() {
        int oldFactor = uiScale.factor();
        uiScale.useAuto(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        applyScaleChange(oldFactor);
        syncScaleMark();
    }

    private void syncScaleMark() {
        boolean auto = uiScale.isAuto();
        if (auto != menuMarksAutoScale) {
            menuMarksAutoScale = auto;
            menuBar = PaintMenu.create(auto);
        }
    }

    private void updateScale(int width, int height) {
        int oldFactor = uiScale.factor();
        uiScale.update(width, height);
        applyScaleChange(oldFactor);
    }

    private void applyScaleChange(int oldFactor) {
        showScaleMessage();
        if (uiScale.factor() == oldFactor) {
            return;
        }
        painter.setScale(uiScale.scale());
        rulesPainter.setScale(uiScale.scale());
        revealCursor = true;
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

    // Guide edits are live; the grid and the rule warnings are told so they drop what they cached.
    private void showGuides(PaintGuides shownGuides) {
        guides = shownGuides;
        grid.setGuides(shownGuides);
        ruleRevision = -1;
    }

    private boolean pointerOverNotePanel() {
        pointer.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(pointer);
        int column = Math.floorDiv((int) Math.floor(pointer.x), cell());
        int row = Math.floorDiv((int) Math.floor(viewport.getWorldHeight() - pointer.y), cell());
        ReviewLayout.Rect area = rects.note();
        return column >= area.x() && column < area.x() + area.width() && row >= area.y() && row < area.bottom();
    }

    private final class GuideListHost implements PaintGuideListPanel.Host {

        @Override
        public PaintGuides currentGuides() {
            return guides;
        }

        @Override
        public void showGuides(PaintGuides shownGuides) {
            PaintApp.this.showGuides(shownGuides);
        }

        @Override
        public void endStroke() {
            stroke.end();
        }

        @Override
        public void clearWheelTicks() {
            wheelTicks.clearVertical();
        }

        @Override
        public boolean pointerOverList() {
            return pointerOverNotePanel();
        }
    }

    private final class DeriveHost implements PaintDerive.Host {

        @Override
        public int[] codePoints() {
            return codePoints;
        }

        @Override
        public int cursor() {
            return cursor;
        }

        @Override
        public void setCursor(int slot) {
            cursor = slot;
        }

        @Override
        public void endStroke() {
            stroke.end();
        }

        @Override
        public void keepVisible(int slot) {
            sheet.keepVisible(slot);
        }

        @Override
        public int slotOf(int codePoint) {
            return PaintApp.this.slotOf(codePoint);
        }

        @Override
        public String describeGlyph(int codePoint, String suffix) {
            return PaintApp.this.describeGlyph(codePoint).append(suffix).toString();
        }

        @Override
        public void rememberProblem(String problem) {
            lastProblem = problem;
        }

        @Override
        public void trackPointer() {
            PaintApp.this.trackPointer();
        }

        @Override
        public boolean menuClicked(PaintDeriveMenu menu) {
            return menu.handleClick(rects.grid(), cell(), pointerX, pointerY, leftClicked());
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
            derive.beginPick();
            int overlaySlot = slotOf(overlay.codePoint());
            if (overlaySlot >= 0 && !unselectable[overlaySlot]) {
                flow.moveSource(overlaySlot);
                sheet.keepVisible(overlaySlot);
            }
            flow.shiftSource(overlay.shift());
            derive.refreshPick();
            return;
        }
        int sourceSlot = slotOf(DevCapture.parseCodePoint(request));
        if (sourceSlot < 0 || unselectable[sourceSlot]) {
            return;
        }
        derive.beginPick();
        flow.moveSource(sourceSlot);
        derive.refreshPick();
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
        guidesFile = PaintGuidesFile.open(directory.file().toPath(), fontSize);
        guides = guidesFile.guides();
        if (!guidesFile.isUnreadable()) {
            return;
        }
        messages.setCondition(KEY_GUIDES, true, "Guides file is bad: guides are off");
        Gdx.app.error(TAG, "Cannot read " + guidesFile.displayPath() + ": " + guidesFile.readError());
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

    private PaintMenu.Context menuContext() {
        if (controls.isOpen()) {
            return PaintMenu.Context.CONTROLS_OPEN;
        }
        if (rules.isShowing()) {
            return PaintMenu.Context.RULES_OPEN;
        }
        if (confirmingQuit || guideListPanel.isOpen() || newGlyph.isOpen() || newSize.isOpen() || newFont.isOpen()) {
            return PaintMenu.Context.MODAL;
        }
        return flow.isActive() ? PaintMenu.Context.DERIVING : PaintMenu.Context.IDLE;
    }

    // Runs before everything else. While the menu owns the frame, nothing below it may read input.
    private boolean handleMenuInput(float delta) {
        updatePointerCell();
        if (!altDown()) {
            altSeenDown = false;
        }
        if (menuPinned) {
            if (!menuPinCellKnown) {
                menuPinCellKnown = true;
                menuPinX = pointerX;
                menuPinY = pointerY;
            }
            if (pointerX == menuPinX && pointerY == menuPinY && !leftClicked()
                && !Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                return menuBar.isOpen();
            }
            menuPinned = false;
        }
        boolean wasOpen = menuBar.isOpen();
        boolean escape = Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE);
        PaintMenu.Action chosen = menuPointer.update(menuBar, menuAvailability,
            Math.floorDiv(pointerX, cell()), Math.floorDiv(pointerY, cell()), worldWidth() / cell(),
            escape, leftClicked(),
            Gdx.input.isButtonPressed(Input.Buttons.LEFT), stroke.isActive() || sheet.isDraggingColumnBar());
        boolean keysOwnFrame = false;
        if (chosen == null && (!escape || altDown())) {
            chosen = routeMenuKeys(delta);
            keysOwnFrame = menuKeys.consumed();
        }
        if (chosen != null) {
            stroke.end();
            runMenuAction(chosen);
        }
        if (menuBar.isOpen() != wasOpen) {
            repeatLeft.hold(Gdx.input.isKeyPressed(Input.Keys.LEFT));
            repeatRight.hold(Gdx.input.isKeyPressed(Input.Keys.RIGHT));
            repeatUp.hold(Gdx.input.isKeyPressed(Input.Keys.UP));
            repeatDown.hold(Gdx.input.isKeyPressed(Input.Keys.DOWN));
        }
        return menuPointer.captured() || keysOwnFrame;
    }

    // Arrow repeats are read only while a menu is open, so a closed menu leaves them to the cursor.
    private PaintMenu.Action routeMenuKeys(float delta) {
        boolean alt = altDown();
        boolean steering = menuBar.isOpen() && !alt;
        int lefts = steering ? repeatLeft.update(Gdx.input.isKeyPressed(Input.Keys.LEFT), delta) : 0;
        int rights = steering ? repeatRight.update(Gdx.input.isKeyPressed(Input.Keys.RIGHT), delta) : 0;
        int ups = steering ? repeatUp.update(Gdx.input.isKeyPressed(Input.Keys.UP), delta) : 0;
        int downs = steering ? repeatDown.update(Gdx.input.isKeyPressed(Input.Keys.DOWN), delta) : 0;
        boolean enter = Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER);
        return menuKeys.route(menuBar, menuAvailability, !guideListPanel.isRenaming(), alt,
            alt ? pressedAccessKey() : MenuKeys.NO_KEY, Gdx.input.isKeyJustPressed(Input.Keys.F10), enter,
            lefts, rights, ups, downs);
    }

    private char pressedAccessKey() {
        for (int i = 0; i < menuBar.menuCount(); i++) {
            char key = Character.toLowerCase(menuBar.menu(i).accessKey());
            if (key >= 'a' && key <= 'z' && Gdx.input.isKeyJustPressed(Input.Keys.A + (key - 'a'))) {
                return key;
            }
        }
        return MenuKeys.NO_KEY;
    }

    private void runMenuAction(PaintMenu.Action action) {
        switch (action) {
            case SAVE -> save();
            case BACK -> leave();
            case UNDO -> undoOnce();
            case REDO -> redoOnce();
            case NEW_SIZE -> openNewSizeFromMenu();
            case NEW_FONT -> openNewFont();
            case NEW_GLYPH -> openNewGlyph();
            case DERIVE -> derive.openDerive();
            case UNLOCK_DERIVED -> derive.unlockDerived();
            case GO_TO_SOURCE -> derive.goToSource();
            case CANCEL_DERIVE -> derive.cancelDerive();
            case NEXT_PREVIEW -> preview.cycleTab(false);
            case GUIDES -> guideListPanel.open();
            case BIGGER_UI -> stepScale(1);
            case SMALLER_UI -> stepScale(-1);
            case AUTO_UI -> useAutoScale();
            case RULES -> {
                rules.open();
                wheelTicks.clearVertical();
                stroke.end();
            }
            case CONTROLS -> {
                controls.open();
                wheelTicks.clear();
                stroke.end();
            }
        }
    }

    // Drops queued wheel ticks and keeps the repeaters tracking the real keys without producing moves,
    // so a key held through the menu neither bursts nor counts as freshly pressed when the menu closes.
    private void swallowInput() {
        wheelTicks.clear();
        boolean control = controlDown();
        boolean shift = shiftDown();
        repeatLeft.update(Gdx.input.isKeyPressed(Input.Keys.LEFT), 0f);
        repeatRight.update(Gdx.input.isKeyPressed(Input.Keys.RIGHT), 0f);
        repeatUp.update(Gdx.input.isKeyPressed(Input.Keys.UP), 0f);
        repeatDown.update(Gdx.input.isKeyPressed(Input.Keys.DOWN), 0f);
        repeatUndo.update(undoHeld(control, shift), 0f);
        repeatRedo.update(redoHeld(control, shift), 0f);
        guideListPanel.swallowInput();
    }

    private void handleInput(float delta) {
        // The Controls panel takes the frame before the menu does, so a click on the bar row can still close it.
        boolean controlsOpen = controls.isOpen();
        boolean menuOwnsInput = !controlsOpen && handleMenuInput(delta);
        if (controlsOpen) {
            updatePointerCell();
            menuBar.hover(-1, MenuBar.NOT_IN_DROP_DOWN, 0, menuAvailability);
        } else if (!menuOwnsInput && !newGlyph.isOpen() && !newSize.isOpen() && !newFont.isOpen()) {
            handleScaleKeys();
        }
        if (dialogDimsScreen() && !menuBar.isOpen()) {
            menuBar.hover(-1, MenuBar.NOT_IN_DROP_DOWN, 0, menuAvailability);
        }
        layoutPanels();
        if (controlsOpen) {
            controls.handleInput(delta, wheelTicks.vertical(), leftClicked(),
                Math.floorDiv(pointerX, cell()), Math.floorDiv(pointerY, cell()),
                worldWidth() / cell(), worldHeight() / cell());
        }
        if (menuOwnsInput || controlsOpen) {
            stroke.end();
            swallowInput();
            updateConditions();
            return;
        }
        if (newGlyph.isOpen()) {
            stroke.end();
            updateConditions();
            handleNewGlyph(delta);
            return;
        }
        if (newSize.isOpen()) {
            stroke.end();
            updateConditions();
            handleNewSize();
            return;
        }
        if (newFont.isOpen()) {
            stroke.end();
            updateConditions();
            if (confirmingQuit) {
                handleQuitPrompt();
                wheelTicks.clear();
                return;
            }
            int wheel = wheelTicks.vertical();
            wheelTicks.clear();
            newFont.handleInput(delta, wheel, leftClicked(), pointerX, pointerY, cell(), rects.grid());
            return;
        }
        scrollRouting.scrollSheetSideways();
        updateConditions();
        if (rules.handleInput(delta, wheelTicks.vertical())) {
            wheelTicks.clearVertical();
            stroke.end();
            return;
        }
        if (confirmingQuit) {
            handleQuitPrompt();
            wheelTicks.clearVertical();
            return;
        }
        if (guideListPanel.isOpen()) {
            guideListPanel.handle(delta, wheelTicks.vertical());
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2) && flow.mode() == DeriveFlow.Mode.IDLE) {
            guideListPanel.open();
            return;
        }
        boolean control = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        if (control && Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            save();
        }
        if (derive.handleModalKeys()) {
            wheelTicks.clearVertical();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (flow.isPicking()) {
                derive.cancelDerive();
            } else {
                leave();
            }
            return;
        }
        handleUndoKeys(control, delta);
        if (flow.isPicking()) {
            if (derive.handlePickingKeys()) {
                return;
            }
        } else if (!control) {
            handleDeriveKeys();
            if (flow.isActive() || newGlyph.isOpen()) {
                return;
            }
        }
        handlePreviewTabKey();
        moveCursor(delta);
        scrollRouting.handleScrollKeys();
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

    // The key bar owns the last row only while deriving or editing guides; otherwise the sheet and messages panels reach it.
    private boolean statusBarShown() {
        return flow.isActive() || guideListPanel.isOpen();
    }

    private void layoutPanels() {
        layoutBarShown = statusBarShown();
        PaintColumns.Split split = PaintColumns.split(
            worldWidth(), sheet.slotWidth(), PaintSheetView.GAP, editor.cellWidth(), cell());
        sheet.setVisibleColumns(split.sheetColumns());
        sheet.fitToHeight(worldHeight(), layoutBarShown, cursor, revealCursor);
        revealCursor = false;
        rects = PaintLayout.compute(worldWidth() / cell(), worldHeight() / cell(),
            sheet.frameRight() / cell(), split.rightCells(), editor.cellWidth(), editor.cellHeight(),
            cell(), layoutBarShown);
        fitGrid();
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
                GlyphLabel.appendCodeAndName(line, from);
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
            messages.setCondition(KEY_PICK, false, derivePick.promptText());
            if (derivePick.problemText().isEmpty()) {
                messages.clearCondition(KEY_PICK_PROBLEM);
            } else {
                messages.setCondition(KEY_PICK_PROBLEM, true, derivePick.problemText());
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

    private void handlePreviewTabKey() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            preview.cycleTab(shiftDown());
        }
    }

    // Esc and the Back menu item: leaves at once, or asks about unsaved changes first.
    private void leave() {
        runAfterUnsavedCheck(onExit, LEAVE_QUESTION);
    }

    // Runs the action now, or after the user saves or discards when there are unsaved changes.
    private void runAfterUnsavedCheck(Runnable action, String question) {
        if (editor.isModified()) {
            messages.nextAction();
            afterUnsavedPrompt = action;
            unsavedQuestion = question;
            confirmingQuit = true;
            stroke.end();
        } else {
            action.run();
        }
    }

    private static boolean shiftDown() {
        return Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
    }

    private static boolean undoHeld(boolean control, boolean shift) {
        return control && Gdx.input.isKeyPressed(Input.Keys.Z) && !shift;
    }

    private static boolean redoHeld(boolean control, boolean shift) {
        return control && (Gdx.input.isKeyPressed(Input.Keys.Y) || (shift && Gdx.input.isKeyPressed(Input.Keys.Z)));
    }

    // Ctrl+Z undoes; Ctrl+Y and Ctrl+Shift+Z redo. Ignored while a stroke is active.
    private void handleUndoKeys(boolean control, float delta) {
        boolean shift = shiftDown();
        int undos = repeatUndo.update(undoHeld(control, shift), delta);
        int redos = repeatRedo.update(redoHeld(control, shift), delta);
        if (stroke.isActive() || flow.isActive()) {
            return;
        }
        for (int i = 0; i < undos; i++) {
            undoOnce();
        }
        for (int i = 0; i < redos; i++) {
            redoOnce();
        }
    }

    private void undoOnce() {
        showChangedGlyph(editor.undo(), "Undid change to ");
    }

    private void redoOnce() {
        showChangedGlyph(editor.redo(), "Redid change to ");
    }

    private void showChangedGlyph(int codePoint, String notice) {
        if (codePoint == FontEditor.NO_GLYPH) {
            return;
        }
        messages.nextAction();
        line.setLength(0);
        line.append(notice);
        GlyphLabel.appendCodeAndName(line, codePoint);
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
                derive.moveSource(position);
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

    // N opens the new glyph prompt; other derive shortcuts go to PaintDerive.
    private void handleDeriveKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.N) && !altDown() && !shiftDown()) {
            openNewGlyph();
            newGlyph.swallowOpeningKey();
        } else {
            derive.handleShortcutKeys();
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
        GlyphLabel.appendCodeAndName(line, codePoint);
        return line;
    }

    private final class ScrollHost implements PaintScrollRouting.Host {

        @Override
        public boolean helpShown() {
            return helpShown;
        }

        @Override
        public boolean helpContains(int pixelX, int pixelY) {
            return PaintApp.this.helpContains(pixelX, pixelY);
        }

        @Override
        public boolean isOverlay() {
            return flow.isOverlay();
        }

        @Override
        public int cell() {
            return PaintApp.this.cell();
        }

        @Override
        public void unprojectInputToPointer() {
            PaintApp.this.unprojectInputToPointer();
        }

        @Override
        public int unprojectedPixelX() {
            return (int) Math.floor(pointer.x);
        }

        @Override
        public int unprojectedPixelY() {
            return (int) Math.floor(viewport.getWorldHeight() - pointer.y);
        }
    }

    private void unprojectInputToPointer() {
        pointer.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(pointer);
    }

    private void updatePointerCell() {
        unprojectInputToPointer();
        pointerX = (int) Math.floor(pointer.x);
        pointerY = (int) Math.floor(viewport.getWorldHeight() - pointer.y);
    }

    private void trackPointer() {
        updatePointerCell();
        scrollRouting.routeVerticalWheel(pointerX, pointerY);
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
                } else {
                    derive.clickSource(slot);
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
            confirmingQuit = false;
            if (!editor.isModified()) {
                afterUnsavedPrompt.run();
            }
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            confirmingQuit = false;
            afterUnsavedPrompt.run();
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
        // Input this frame may have started or ended derive or the guide list after the layout was made.
        if (statusBarShown() != layoutBarShown) {
            layoutPanels();
        }
        ScreenUtils.clear(UiPalette.BG);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        int codePoint = codePoints[cursor];
        int columns = worldWidth() / cell();
        int rows = worldHeight() / cell();
        TopBar.draw(painter, columns, CRUMB_ROW,
            List.of("GLYPH WORKBENCH", folderName, "PAINT " + fontSize), uiScale.label(),
            editor.isModified() ? "● MODIFIED" : "", UiPalette.ORANGE);
        boolean choosingSource = flow.isPicking() || flow.mode() == DeriveFlow.Mode.CONFIRM_REPLACE;
        sheet.draw(cursor, choosingSource ? flow.sourceSlot() : -1);
        ReviewLayout.Rect panel = rects.grid();
        line.setLength(0);
        line.append("U+");
        GlyphLabel.appendHex(line, codePoint);
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
        grid.setHighlight(guideListPanel.highlightedGuide());
        grid.draw(codePoint, derivePick.shown(), derive.pendingGhost(),ghostSeconds);
        preview.draw(rects.preview(), codePoint, derivePick.shown());
        drawReviewNote(codePoint);
        ReviewLayout.Rect strip = rects.messages();
        messages.draw(painter, strip.x(), strip.y(), strip.width(), strip.height(), Gdx.graphics.getDeltaTime());
        if (layoutBarShown) {
            StatusBar.draw(painter, columns, rows - 1,
                statusKeys(choosingSource));
        }
        // Each dialog dims the screen itself, so one under an overlay would dim the background twice.
        boolean overlayShown = controls.isOpen() || rules.isShowing();
        // A dialog dims everything, so the bar is drawn beneath the dim, unless a menu is open and must stay clear.
        boolean promptShown = dialogDimsScreen() && !overlayShown && !menuBar.isOpen();
        if (promptShown) {
            MenuBarView.drawBar(painter, menuBar, columns, MENU_ROW, menuAvailability);
        }
        if (confirmingQuit && !overlayShown) {
            Dialog.draw(painter, columns, rows, "UNSAVED CHANGES",
                List.of("Save changes to " + dataFile.name(), unsavedQuestion), QUIT_BUTTONS);
        }
        if (newGlyph.isOpen() && !overlayShown) {
            newGlyph.draw(painter, columns, rows, rects.grid());
        }
        // Under the unsaved-changes prompt the dialog is hidden but kept, so cancelling returns to it.
        if (newFont.isOpen() && !overlayShown && !confirmingQuit) {
            newFont.draw(painter, columns, rows, rects.grid());
        }
        if (newSize.isOpen() && !overlayShown) {
            newSize.draw(painter, columns, rows, rects.grid());
        }
        if (!overlayShown) {
            derive.drawDialog(painter, columns, rows, rects.grid(), codePoint);
        }
        // The derive help describes the highlighted operation, so it is drawn again above the dim.
        if (!overlayShown && derive.dialogShown()) {
            drawHelp(helpRect);
        }
        if (!promptShown) {
            MenuBarView.drawBar(painter, menuBar, columns, MENU_ROW, menuAvailability);
        }
        MenuBarView.drawDropDown(painter, menuBar, columns, MENU_ROW, menuAvailability);
        rules.draw(worldWidth(), worldHeight(), codePoint);
        if (controls.isOpen()) {
            controls.draw(painter, columns, rows);
        }
        batch.end();
    }

    // The dialogs that dim the whole screen when drawn.
    private boolean dialogDimsScreen() {
        return confirmingQuit || newGlyph.isOpen() || newSize.isOpen() || newFont.isOpen()
            || flow.mode() == DeriveFlow.Mode.MENU || flow.mode() == DeriveFlow.Mode.CONFIRM_REPLACE;
    }

    private List<StatusBar.Key> statusKeys(boolean choosingSource) {
        if (guideListPanel.isOpen()) {
            return guideListPanel.statusKeys();
        }
        if (flow.mode() == DeriveFlow.Mode.MENU) {
            return MENU_KEYS;
        }
        if (flow.mode() == DeriveFlow.Mode.CONFIRM_REPLACE) {
            return REPLACE_KEYS;
        }
        if (!choosingSource) {
            return KEYS;
        }
        return flow.isOverlay() ? OVERLAY_PICK_KEYS : PICK_KEYS;
    }

    // Read-only: the note and pick the upscale review recorded for this glyph, if it has a note.
    private void drawReviewNote(int codePoint) {
        if (shownNoteFor != codePoint) {
            shownNoteFor = codePoint;
            notePanel.resetDisplayScroll();
        }
        if (guideListPanel.isOpen()) {
            guideListPanel.draw(rects.note(), cell());
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

    // The help lines are rebuilt only when the state or panel size changes; they wrap, and scroll when taller than the panel.
    private void drawHelp(ReviewLayout.Rect area) {
        Panel.draw(painter, area.x(), area.y(), area.width(), area.height(), "HELP", false);
        PaintHelp.State state = derive.helpState();
        int chars = Math.max(1, area.width() - 2 * TEXT_PADDING - 2);
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
        int textX = (area.x() + 1 + TEXT_PADDING) * cell();
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

    @Override
    public void hide() {
        clearWindowSizeLimits();
        batch.dispose();
        whitePixel.dispose();
        source.dispose();
    }
}
