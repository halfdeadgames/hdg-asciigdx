package com.github.halfdeadgames.asciigdx.workbench.paint;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.github.halfdeadgames.asciigdx.workbench.AtomicFiles;
import com.github.halfdeadgames.asciigdx.workbench.CursorNavigator;
import com.github.halfdeadgames.asciigdx.workbench.DevCapture;
import com.github.halfdeadgames.asciigdx.workbench.WorkbenchScreen;
import com.github.halfdeadgames.asciigdx.workbench.derive.DeriveFlow;
import com.github.halfdeadgames.asciigdx.workbench.editor.FontEditor;
import com.github.halfdeadgames.asciigdx.workbench.export.PaintExport;
import com.github.halfdeadgames.asciigdx.workbench.export.PaintExports;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataParser;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphListFile;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphOp;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.PixelGlyph;
import com.github.halfdeadgames.asciigdx.workbench.guides.BrokenGuides;
import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGridLabels;
import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGuideListPanel;
import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGuides;
import com.github.halfdeadgames.asciigdx.workbench.guides.PaintGuidesFile;
import com.github.halfdeadgames.asciigdx.workbench.help.ControlsScreen;
import com.github.halfdeadgames.asciigdx.workbench.help.PaintHelp;
import com.github.halfdeadgames.asciigdx.workbench.help.PaintHelpPanel;
import com.github.halfdeadgames.asciigdx.workbench.newitems.NewGlyphDialog;
import com.github.halfdeadgames.asciigdx.workbench.newitems.NewSizeDialog;
import com.github.halfdeadgames.asciigdx.workbench.newitems.NewSizePlan;
import com.github.halfdeadgames.asciigdx.workbench.newitems.NewSizePrompt;
import com.github.halfdeadgames.asciigdx.workbench.newitems.PaintNewFont;
import com.github.halfdeadgames.asciigdx.workbench.preview.PaintPreview;
import com.github.halfdeadgames.asciigdx.workbench.preview.PreviewTabs;
import com.github.halfdeadgames.asciigdx.workbench.rules.RuleChecker;
import com.github.halfdeadgames.asciigdx.workbench.rules.RulesFile;
import com.github.halfdeadgames.asciigdx.workbench.rules.RulesFileLoad;
import com.github.halfdeadgames.asciigdx.workbench.rules.RulesLoadResult;
import com.github.halfdeadgames.asciigdx.workbench.rules.RuleWarning;
import com.github.halfdeadgames.asciigdx.workbench.rules.RulesScreen;
import com.github.halfdeadgames.asciigdx.workbench.ui.ArrowRepeats;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.Dialog;
import com.github.halfdeadgames.asciigdx.workbench.ui.DialogContent;
import com.github.halfdeadgames.asciigdx.workbench.ui.GlyphSize;
import com.github.halfdeadgames.asciigdx.workbench.ui.KeyRepeat;
import com.github.halfdeadgames.asciigdx.workbench.ui.Keys;
import com.github.halfdeadgames.asciigdx.workbench.ui.MenuBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.MenuBarView;
import com.github.halfdeadgames.asciigdx.workbench.ui.MenuKeys;
import com.github.halfdeadgames.asciigdx.workbench.ui.MenuPointer;
import com.github.halfdeadgames.asciigdx.workbench.ui.MessageStrip;
import com.github.halfdeadgames.asciigdx.workbench.ui.Painter;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScreenKit;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScrollState;
import com.github.halfdeadgames.asciigdx.workbench.ui.StatusBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.TopBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.TopBarStatus;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiScale;
import com.github.halfdeadgames.asciigdx.workbench.ui.WheelTicks;
import com.github.halfdeadgames.asciigdx.workbench.upscale.UpscaleSourceFonts;
import com.github.halfdeadgames.asciigdx.workbench.upscale.UpscaleSources;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Paint screen: edits one font data file glyph by glyph. */
public final class PaintScreen implements WorkbenchScreen {

    private static final String TAG = "Paint";
    private static final String LEAVE_QUESTION = "before leaving?";
    private static final String SWITCH_SIZE_QUESTION = "before switching size?";
    private static final String EXPORT_QUESTION = "Save, then export?";
    private static final String KEY_DERIVED = "derived";
    private static final String KEY_EMPTY = "empty";
    private static final String RULE_KEY_PREFIX = "rule:";
    private static final String BROKEN_GUIDE_KEY_PREFIX = "brokenGuide:";
    private static final String KEY_RULES = "rules";
    private static final String KEY_PICK = "pick";
    private static final String KEY_PICK_PROBLEM = "pickProblem";
    private static final String KEY_GUIDES = "guides";
    private static final String KEY_SCALE = "scale";
    private static final int COLUMNS = PaintSheetView.COLUMNS;
    private static final int MENU_ROW = 0;
    private static final int CRUMB_ROW = 1;

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
        new StatusBar.Key("[ ]", "SHIFT"), new StatusBar.Key("; '", "UP/DOWN"),new StatusBar.Key("ENTER", "ADD"),
        new StatusBar.Key("BKSP", "UNDO PART"), new StatusBar.Key("F", "FINISH"),
        new StatusBar.Key("ESC", "CANCEL"));
    private static final List<Dialog.Button> UNSAVED_BUTTONS = List.of(
        new Dialog.Button("S", "SAVE", Dialog.Tone.OK), new Dialog.Button("D", "DISCARD", Dialog.Tone.WARN),
        new Dialog.Button("ESC", "CANCEL", Dialog.Tone.DIM));
    private static final List<Dialog.Button> SAVE_ONLY_BUTTONS = List.of(
        new Dialog.Button("S", "SAVE", Dialog.Tone.OK), new Dialog.Button("ESC", "CANCEL", Dialog.Tone.DIM));

    private final DeriveFlow flow = new DeriveFlow();
    private final PaintLayers layers = new PaintLayers();
    // Rebuilt only when the auto-size mark changes, which never happens while a menu is open.
    private MenuBar<PaintMenu.Action> menuBar = PaintMenu.create(true);
    private boolean menuMarksAutoScale = true;
    private final MenuPointer<PaintMenu.Action> menuPointer = new MenuPointer<>(MENU_ROW);
    private final MenuKeys<PaintMenu.Action> menuKeys = new MenuKeys<>();
    private final Predicate<PaintMenu.Action> menuAvailability =
        action -> PaintMenu.isAvailable(action, menuContext());
    private PaintDerivePick derivePick;
    private PaintDerive derive;
    private final ScreenKit kit = new ScreenKit();
    private final StringBuilder line = new StringBuilder();
    private final DevCapture devCapture = new DevCapture();
    private final KeyRepeat repeatLeft = new KeyRepeat();
    private final KeyRepeat repeatRight = new KeyRepeat();
    private final KeyRepeat repeatUp = new KeyRepeat();
    private final KeyRepeat repeatDown = new KeyRepeat();
    private final KeyRepeat repeatUndo = new KeyRepeat();
    private final KeyRepeat repeatRedo = new KeyRepeat();
    private final ArrowRepeats arrowRepeats = new ArrowRepeats(repeatLeft, repeatRight, repeatUp, repeatDown);
    private final PaintMenuInput menuInput = new PaintMenuInput(new MenuInputHost(), menuPointer, menuKeys,
        menuAvailability, arrowRepeats);
    private final NewGlyphDialog newGlyph = new NewGlyphDialog(this::addGlyphSlot, codePoint -> slotOf(codePoint) >= 0);
    private final NewSizeDialog newSize = new NewSizeDialog(this::confirmNewSize);
    private final PaintNewFont newFont;
    private PaintGuideListPanel guideListPanel;

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
    private RulesScreen rules;
    private final GlyphTitle glyphTitle = new GlyphTitle();
    private final MessageStrip messages = new MessageStrip();
    private final PaintDeriveMenu deriveMenu = new PaintDeriveMenu(flow, messages);
    private final PaintUpscale upscale = new PaintUpscale(messages, arrowRepeats, new PaintUpscaleHost());
    // Static so a paint screen opened later in the same run does not open the capture dialog again.
    private static boolean newFontCaptureOpened;
    private PaintLayout.Result rects;
    private boolean layoutBarShown;
    private final ControlsScreen controls = new ControlsScreen();
    private String lastProblem = "";
    private float ghostSeconds;
    private int derivedTextGlyph = -1;
    private int derivedTextSource = -1;
    private String derivedText = "";
    private List<RuleWarning> ruleWarnings = List.of();
    private RulesFile rulesFile;
    private final List<String> shownRuleKeys = new ArrayList<>();
    private final List<String> shownBrokenGuideKeys = new ArrayList<>();
    private int ruleGlyph = -1;
    private int ruleRevision = -1;
    private PaintGuides guides = PaintGuides.NONE;
    private PaintGuidesFile guidesFile;
    private boolean rulesFileBad;
    private final ScrollState helpScroll = new ScrollState(0);
    private PaintHelpPanel helpPanel;
    private boolean[] unselectable;
    private int[] codePoints;
    private int cursor;
    private boolean revealCursor = true;
    private boolean confirmingUnsaved;
    private final WheelTicks wheelTicks = new WheelTicks();
    private PaintScrollRouting scrollRouting;
    private PaintPointer pointer;
    private PointerTargets pointerTargets;
    private PointerTargets guideListPointerTargets;

    private final Runnable onExit;
    private final String fontDir;
    private final CellSize fontSize;
    private final Consumer<CellSize> openSize;
    // What the unsaved-changes prompt continues with once the changes are saved or discarded.
    private Runnable afterUnsavedPrompt = () -> { };
    private String unsavedQuestion = LEAVE_QUESTION;
    private List<Dialog.Button> unsavedButtons = UNSAVED_BUTTONS;
    // Built in show(): the folder memory needs preferences, which need the running app.
    private PaintExport paintExport;

    /**
     * @param onExit called when the user leaves the screen with Esc (after the unsaved prompt)
     * @param fontDir absolute path of the font folder
     * @param fontSize cell size, which is also the data file name without {@code .txt}
     * @param openSize called with the size of a size file this screen just created, to paint it next
     * @param openFont called with the folder and first size of a font this screen just created, to paint it next
     */
    public PaintScreen(Runnable onExit, String fontDir, CellSize fontSize, Consumer<CellSize> openSize,
                    BiConsumer<Path, CellSize> openFont) {
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
        loadFont(directory);
        paintExport = PaintExports.create(directory.file(), fontSize, new ExportMessages());
        pickStartCursor();
        createPainters();
        setUpScale();
        buildViews(directory);
        installInputProcessor();
        openCaptureHooks();
    }

    private void loadFont(FileHandle directory) {
        dataFile = directory.child(fontSize.dataFileName());
        folderName = directory.name().toUpperCase(java.util.Locale.ROOT);
        editor = new FontEditor(
            FontDataParser.parseSizeFile(fontSize, dataFile.readString("UTF-8")),
            GlyphListFile.load(directory.file()));
        stroke = new PaintStroke(editor);
        derivePick = new PaintDerivePick(editor, flow);
        derive = new PaintDerive(flow, derivePick, deriveMenu, editor, messages, new PaintDeriveHost());
    }

    private void pickStartCursor() {
        readSlots();
        cursor = editor.startSlot();
        int preselected = DevCapture.slotToSelect(codePoints, unselectable);
        if (preselected >= 0) {
            cursor = preselected;
        }
    }

    private void createPainters() {
        kit.create();
        painter = kit.painter();
        rulesPainter = kit.newPainter();
    }

    private void setUpScale() {
        uiScale = new PaintUiScale(editor.cellWidth(), editor.cellHeight(), launchOverride());
        syncScaleMark();
        uiScale.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        showScaleMessage();
        painter.setScale(uiScale.scale());
        rulesPainter.setScale(uiScale.scale());
    }

    private void buildViews(FileHandle directory) {
        sheet = new PaintSheetView(painter, editor, 0, PaintLayout.TOP_ROWS);
        applyMinimumWindowSize();
        loadGuides(directory);
        grid = new PaintGridView(painter, editor, guides);
        guideListPanel = new PaintGuideListPanel(painter, messages, guidesFile, editor, arrowRepeats,
            new GuideListPanelHost());
        preview = new PaintPreview(painter, editor);
        PreviewTabs.Tab requestedTab = DevCapture.requestedTab();
        if (requestedTab != null) {
            preview.selectTab(requestedTab);
        }
        rules = new RulesScreen(rulesPainter);
        helpPanel = new PaintHelpPanel(painter, helpScroll);
        scrollRouting = new PaintScrollRouting(messages, helpScroll, sheet, wheelTicks, new ScrollRoutingHost());
        pointer = new PaintPointer(kit.viewport(), scrollRouting);
        guideListPointerTargets = new PointerTargets(List.of(
            new GuideListPointerTarget(guideListPanel, () -> rects.belowPreview(), () -> rects.grid(), this::cellPixels)));
        pointerTargets = new PointerTargets(List.of(
            new PreviewPointerTarget(preview, () -> rects.preview(), this::cellPixels),
            new SheetBarPointerTarget(sheet),
            new MessagesPointerTarget(messages, this::cellPixels),
            new SheetSlotPointerTarget(sheet, flow, derive, messages, new CursorSlotSelection()),
            new GridStrokePointerTarget(grid, stroke, flow, messages, editor, new CursorStrokeStart())));
        loadRules(directory);
    }

    private void installInputProcessor() {
        Gdx.input.setInputProcessor(new PaintInputEvents());
    }

    private final class PaintInputEvents extends InputAdapter {
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
                menuInput.noteAltPressed();
            }
            return false;
        }

        @Override
        public boolean keyUp(int keycode) {
            if (keycode == Input.Keys.ALT_LEFT || keycode == Input.Keys.ALT_RIGHT) {
                menuInput.noteAltReleased();
            }
            return false;
        }

        @Override
        public boolean keyTyped(char character) {
            if (menuPointer.isCapturing(menuBar) || menuInput.altSeenDown() || altDown()) {
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
            if (newSize.isOpen()) {
                newSize.type(character);
                return true;
            }
            if (!guideListPanel.isRenaming() || rules.isOpen() || controls.isOpen() || Keys.controlDown()) {
                return false;
            }
            guideListPanel.typeChar(character);
            return true;
        }
    }

    private void openCaptureHooks() {
        openDeriveForCapture();
        guideListPanel.openForCapture(flow.mode() == DeriveFlow.Mode.IDLE);
        openMenuForCapture();
        openNewGlyphForCapture();
        openNewSizeForCapture();
        openUpscaleSourceForCapture();
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
        if (typed == null || flow.isActive() || guideListPanel.isOpen() || confirmingUnsaved) {
            return;
        }
        openNewGlyph();
        newGlyph.typeForCapture(typed);
    }

    private void openNewSizeForCapture() {
        int moves = DevCapture.newSizeRequest();
        if (moves < 0 || flow.isActive() || guideListPanel.isOpen() || confirmingUnsaved || newGlyph.isOpen()) {
            return;
        }
        try {
            openNewSize(NewSizePlan.options(java.nio.file.Path.of(fontDir)));
        } catch (java.io.IOException e) {
            Gdx.app.error(TAG, "Cannot list sizes: " + e.getMessage());
            return;
        }
        newSize.applyCaptureRequest(moves, DevCapture.newSizeOtherRequest());
    }

    private void chooseUpscaleCandidateForCapture() {
        int candidate = DevCapture.upscaleCandidateRequest();
        if (candidate > 0) {
            upscale.chooseCandidateForCapture(candidate);
        }
    }

    private void openUpscaleSourceForCapture() {
        int moves = DevCapture.upscaleSourceRequest();
        if (moves < 0 || flow.isActive() || guideListPanel.isOpen() || confirmingUnsaved || newGlyph.isOpen()
            || newSize.isOpen()) {
            return;
        }
        upscale.open();
        upscale.moveListForCapture(moves);
        if (DevCapture.upscaleSourcePickRequested()) {
            upscale.chooseSelectedForCapture();
            chooseUpscaleCandidateForCapture();
        }
    }

    private void handleUpscale(float delta) {
        int vertical = wheelTicks.vertical();
        int sideways = wheelTicks.sideways();
        wheelTicks.clear();
        pointer.updatePosition();
        upscale.handleInput(delta, vertical, sideways);
    }

    private final class PaintUpscaleHost implements PaintUpscale.Host {

        @Override
        public PaintSheetView createSheetView(FontEditor sourceEditor) {
            return new PaintSheetView(painter, sourceEditor, 0, PaintLayout.TOP_ROWS);
        }

        @Override
        public UpscaleSources.Font targetFont() {
            String targetFolder = Path.of(fontDir).getFileName().toString();
            return new UpscaleSources.Font(targetFolder, editor.cellWidth(), editor.cellHeight());
        }

        @Override
        public List<UpscaleSources.Font> sourceFonts() {
            return UpscaleSourceFonts.discover(fontsRoot());
        }

        @Override
        public Path fontsRoot() {
            return PaintScreen.this.fontsRoot();
        }

        @Override
        public FontEditor targetEditor() {
            return editor;
        }

        @Override
        public void rememberProblem(String problem) {
            lastProblem = problem;
        }

        @Override
        public void keepTargetVisible() {
            sheet.keepVisible(cursor);
        }

        @Override
        public int targetCodePoint() {
            return codePoints[cursor];
        }

        @Override
        public void endStroke() {
            stroke.end();
        }

        @Override
        public int pixelX() {
            return pointer.pixelX();
        }

        @Override
        public int pixelY() {
            return pointer.pixelY();
        }

        @Override
        public boolean leftClicked() {
            return PaintScreen.this.leftClicked();
        }

        @Override
        public int cellPixels() {
            return PaintScreen.this.cellPixels();
        }

        @Override
        public CellRect dialogArea() {
            return rects.grid();
        }
    }

    private void openNewFontForCapture() {
        if (newFontCaptureOpened || flow.isActive() || guideListPanel.isOpen() || confirmingUnsaved || newGlyph.isOpen()
            || newSize.isOpen() || upscale.isActive()) {
            return;
        }
        stroke.end();
        newFontCaptureOpened = newFont.isCaptureRequested();
        newFont.openForCapture();
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
        pointer.updatePosition();
        String folderName = java.nio.file.Path.of(fontDir).getFileName().toString();
        newSize.open(options, folderName, pointer.pixelX(), pointer.pixelY());
    }

    private void handleNewSize(float delta) {
        wheelTicks.clear();
        if (newSize.handleKeys(delta)) {
            return;
        }
        pointer.updatePosition();
        newSize.handlePointer(leftClicked(), pointer.pixelX(), pointer.pixelY(), cellPixels(), rects.grid());
    }

    private void openNewSizeFromMenu() {
        try {
            openNewSize(NewSizePlan.options(java.nio.file.Path.of(fontDir)));
        } catch (java.io.IOException e) {
            Gdx.app.error(TAG, "Cannot list sizes: " + e.getMessage());
            messages.nextAction();
            messages.warn("Cannot list sizes: " + e.getMessage());
        }
    }

    // Unsaved changes are asked about after the pick, so canceling the prompt loses nothing and creates nothing.
    private void confirmNewSize(CellSize chosen) {
        runAfterUnsavedCheck(() -> createSize(chosen), SWITCH_SIZE_QUESTION);
    }

    private void createSize(CellSize chosen) {
        messages.nextAction();
        try {
            NewSizePlan.create(java.nio.file.Path.of(fontDir), chosen);
        } catch (java.nio.file.FileAlreadyExistsException e) {
            messages.warn("Size " + NewSizePrompt.label(chosen) + " already exists");
            return;
        } catch (java.io.IOException | RuntimeException e) {
            Gdx.app.error(TAG, "Cannot create size " + chosen + ": " + e.getMessage());
            messages.warn("Cannot create " + NewSizePrompt.label(chosen) + ": " + e.getMessage());
            return;
        }
        openSize.accept(chosen);
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
        if (!existed) {
            String saveProblem = saveSlotToGlyphList(codePoint);
            if (saveProblem != null) {
                line.append(" was added but could not be saved to glyphs.txt: ").append(saveProblem);
                messages.warn(line.toString());
                return;
            }
        }
        line.append(existed ? " already has a slot" : " added");
        messages.ok(line.toString());
    }

    /** Returns null when the code point was listed, otherwise the reason it was not. */
    private String saveSlotToGlyphList(int codePoint) {
        try {
            GlyphListFile.append(dataFile.parent().file(), codePoint);
            return null;
        } catch (java.io.IOException e) {
            Gdx.app.error(TAG, "Cannot update glyphs.txt: " + e.getMessage());
            return e.getMessage();
        }
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
        menuInput.pin();
        MenuCaptureHook.apply(request, menuBar, menuAvailability, cellPixels());
    }

    private int cellPixels() {
        return uiScale.scale().cellPixels();
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

    // Runs before the layout each frame, so a new scale never draws one frame with the old layout.
    private void handleScaleKeys() {
        if (!Keys.controlDown()) {
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
        refreshBrokenGuideWarnings();
    }

    private void refreshBrokenGuideWarnings() {
        if (rulesFile == null) {
            return;
        }
        clearShownBrokenGuideWarnings();
        List<BrokenGuides.Broken> broken = BrokenGuides.find(guides, rulesFile);
        for (int i = 0; i < broken.size(); i++) {
            String messageKey = BROKEN_GUIDE_KEY_PREFIX + i;
            messages.setCondition(messageKey, true, broken.get(i).message());
            shownBrokenGuideKeys.add(messageKey);
        }
    }

    private void clearShownBrokenGuideWarnings() {
        for (String messageKey : shownBrokenGuideKeys) {
            messages.clearCondition(messageKey);
        }
        shownBrokenGuideKeys.clear();
    }

    private boolean pointerOverGuideList() {
        pointer.unprojectInput();
        int column = Math.floorDiv(pointer.unprojectedPixelX(), cellPixels());
        int row = Math.floorDiv(pointer.unprojectedPixelY(), cellPixels());
        CellRect area = rects.belowPreview();
        return area.contains(column, row);
    }

    private final class GuideListPanelHost implements PaintGuideListPanel.Host {

        @Override
        public PaintGuides currentGuides() {
            return guides;
        }

        @Override
        public void showGuides(PaintGuides shownGuides) {
            PaintScreen.this.showGuides(shownGuides);
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
            return pointerOverGuideList();
        }

        @Override
        public RulesFile currentRules() {
            return rulesFile;
        }
    }

    private final class PaintDeriveHost implements PaintDerive.Host {

        @Override
        public int[] codePoints() {
            return codePoints;
        }

        @Override
        public int cursorSlot() {
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
            return PaintScreen.this.slotOf(codePoint);
        }

        @Override
        public String describeGlyph(int codePoint, String suffix) {
            return PaintScreen.this.describeGlyph(codePoint).append(suffix).toString();
        }

        @Override
        public void rememberProblem(String problem) {
            lastProblem = problem;
        }

        @Override
        public void updateAndRouteWheel() {
            pointer.updateAndRouteWheel();
        }

        @Override
        public boolean menuClicked(PaintDeriveMenu menu) {
            return menu.handleClick(rects.grid(), cellPixels(), pointer.pixelX(), pointer.pixelY(), leftClicked());
        }
    }

    private void openDeriveForCapture() {
        String request = DevCapture.deriveRequest();
        if (request == null) {
            return;
        }
        flow.openMenu(cursor, editor.cellWidth() == editor.cellHeight());
        if ("menu".equals(request.trim())) {
            return;
        }
        DevCapture.OverlayRequest overlay = DevCapture.parseOverlayRequest(request);
        if (overlay != null) {
            flow.highlightMenuRow(flow.operations().indexOf(GlyphOp.Kind.OVERLAY));
            derive.beginPick();
            int overlaySlot = slotOf(overlay.codePoint());
            if (overlaySlot >= 0 && !unselectable[overlaySlot]) {
                flow.moveSource(overlaySlot);
                sheet.keepVisible(overlaySlot);
            }
            flow.shiftSource(overlay.shift());
            flow.shiftSourceDown(overlay.down());
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

    // A missing guides file means no guides; a bad one warns and also means no guides.
    private void loadGuides(FileHandle directory) {
        guidesFile = PaintGuidesFile.open(directory.file().toPath(), fontSize);
        guides = guidesFile.guides();
        if (!guidesFile.isUnreadable()) {
            messages.clearCondition(KEY_GUIDES);
            return;
        }
        messages.setCondition(KEY_GUIDES, true, "Guides file is bad: guides are off");
        Gdx.app.error(TAG, "Cannot read " + guidesFile.displayPath() + ": " + guidesFile.readError());
    }

    // The glyph data file is left alone: it holds unsaved edits and the undo history.
    private void reloadFontFiles() {
        messages.nextAction();
        FileHandle directory = dataFile.parent();
        loadRules(directory);
        reloadGuides(directory);
        if (!rulesFileBad && !guidesFile.isUnreadable()) {
            messages.ok("Reloaded rules, guides, and notes");
        }
    }

    private void reloadGuides(FileHandle directory) {
        loadGuides(directory);
        showGuides(guides);
        guideListPanel.useFile(guidesFile);
    }

    // Run again to reload; the next frame checks the current glyph afresh.
    private void loadRules(FileHandle directory) {
        RulesLoadResult result = RulesFileLoad.load(
            directory.file().toPath(), fontSize, editor.cellWidth(), editor.cellHeight());
        rulesFile = result.rulesFile();
        grid.setRules(rulesFile);
        ruleGlyph = -1;
        ruleRevision = -1;
        refreshBrokenGuideWarnings();
        showRulesLoadState(result);
        rules.show(result, readFontNotes(directory));
    }

    // Notes belong to the font, not to a size; a missing or unreadable file means no notes.
    private String readFontNotes(FileHandle directory) {
        FileHandle notesFile = directory.child("notes.md");
        if (!notesFile.exists()) {
            return null;
        }
        try {
            return notesFile.readString("UTF-8");
        } catch (GdxRuntimeException e) {
            Gdx.app.error(TAG, "Cannot read " + notesFile.path() + ": " + e.getMessage());
            return null;
        }
    }

    private void showRulesLoadState(RulesLoadResult result) {
        rulesFileBad = result.status() == RulesLoadResult.Status.BAD;
        switch (result.status()) {
            case MISSING -> messages.setCondition(KEY_RULES, false, "No rules file");
            case LOADED -> messages.clearCondition(KEY_RULES);
            case BAD -> {
                messages.setCondition(KEY_RULES, true, "Rules file is bad: " + result.errorText());
                Gdx.app.error(TAG, "Cannot read " + result.path() + ": " + result.errorText());
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        kit.resize(width, height);
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
        return Math.round(kit.viewport().getWorldWidth());
    }

    private int worldHeight() {
        return Math.round(kit.viewport().getWorldHeight());
    }

    private PaintMenu.Context menuContext() {
        if (controls.isOpen()) {
            return PaintMenu.Context.CONTROLS_OPEN;
        }
        if (rules.isShowing()) {
            return PaintMenu.Context.RULES_OPEN;
        }
        if (confirmingUnsaved || guideListPanel.isOpen() || newGlyph.isOpen() || newSize.isOpen()
            || upscale.isActive() || newFont.isOpen()) {
            return PaintMenu.Context.MODAL;
        }
        return flow.isActive() ? PaintMenu.Context.DERIVING : PaintMenu.Context.IDLE;
    }

    private void runMenuAction(PaintMenu.Action action) {
        switch (action) {
            case SAVE -> save();
            case EXPORT -> exportFont();
            case RELOAD_FILES -> reloadFontFiles();
            case BACK -> leave();
            case UNDO -> undoOnce();
            case REDO -> redoOnce();
            case NEW_SIZE -> openNewSizeFromMenu();
            case NEW_FONT -> openNewFont();
            case NEW_GLYPH -> openNewGlyph();
            case DERIVE -> derive.openDerive();
            case UPSCALE -> upscale.open();
            case UNLOCK_DERIVED -> derive.unlockDerived();
            case GO_TO_SOURCE -> derive.goToSource();
            case CANCEL_DERIVE -> derive.cancelDerive();
            case NEXT_PREVIEW -> preview.cycleTab(false);
            case GUIDES -> guideListPanel.open();
            case BIGGER_UI -> stepScale(1);
            case SMALLER_UI -> stepScale(-1);
            case AUTO_UI -> useAutoScale();
            case RULES -> openRules();
            case CONTROLS -> openControls();
        }
    }

    private void openRules() {
        rules.open();
        wheelTicks.clearVertical();
        stroke.end();
    }

    private void openControls() {
        controls.open();
        wheelTicks.clear();
        stroke.end();
    }

    // Drops queued wheel ticks and keeps the repeaters tracking the real keys without producing moves,
    // so a key held through the menu neither bursts nor counts as freshly pressed when the menu closes.
    private void swallowInput() {
        wheelTicks.clear();
        boolean control = Keys.controlDown();
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
        if (routeMenuAndControls(delta)) {
            return;
        }
        if (handleNewItemDialogs(delta)) {
            return;
        }
        if (handleModalPanels(delta)) {
            return;
        }
        if (handleCommandKeys(delta)) {
            return;
        }
        handleEditing(delta);
        reportEditProblem();
    }

    private boolean routeMenuAndControls(float delta) {
        // The Controls panel takes the frame before the menu does, so a click on the bar row can still close it.
        boolean controlsOpen = controls.isOpen();
        boolean menuOwnsInput = !controlsOpen && menuInput.handle(delta);
        routeHoverAndScaleKeys(controlsOpen, menuOwnsInput);
        clearMenuHoverUnderDialog();
        layoutPanels();
        if (controlsOpen) {
            handleControlsPanel(delta);
        }
        if (!menuOwnsInput && !controlsOpen) {
            return false;
        }
        stroke.end();
        swallowInput();
        updateConditions();
        return true;
    }

    private void routeHoverAndScaleKeys(boolean controlsOpen, boolean menuOwnsInput) {
        if (controlsOpen) {
            pointer.updatePosition();
            clearMenuHover();
        } else if (!menuOwnsInput && !newItemDialogOpen()) {
            handleScaleKeys();
        }
    }

    private void clearMenuHoverUnderDialog() {
        if (dialogDimsScreen() && !menuBar.isOpen()) {
            clearMenuHover();
        }
    }

    private void clearMenuHover() {
        menuBar.hover(-1, MenuBar.NOT_IN_DROP_DOWN, 0, menuAvailability);
    }

    private boolean newItemDialogOpen() {
        return newGlyph.isOpen() || newSize.isOpen() || upscale.isActive() || newFont.isOpen();
    }

    private void handleControlsPanel(float delta) {
        controls.handleInput(delta, wheelTicks.vertical(), leftClicked(),
            Math.floorDiv(pointer.pixelX(), cellPixels()), Math.floorDiv(pointer.pixelY(), cellPixels()),
            worldWidth() / cellPixels(), worldHeight() / cellPixels());
    }

    private boolean handleNewItemDialogs(float delta) {
        if (newGlyph.isOpen()) {
            stroke.end();
            updateConditions();
            handleNewGlyph(delta);
            return true;
        }
        if (newSize.isOpen()) {
            stroke.end();
            updateConditions();
            handleNewSize(delta);
            return true;
        }
        if (upscale.isActive()) {
            stroke.end();
            updateConditions();
            handleUpscale(delta);
            return true;
        }
        if (newFont.isOpen()) {
            stroke.end();
            updateConditions();
            if (confirmingUnsaved) {
                handleUnsavedPrompt();
                wheelTicks.clear();
                return true;
            }
            wheelTicks.clear();
            newFont.handleInput(delta);
            return true;
        }
        return false;
    }

    private boolean handleModalPanels(float delta) {
        scrollRouting.scrollSheetSideways();
        updateConditions();
        if (rules.handleInput(delta, wheelTicks.vertical())) {
            wheelTicks.clearVertical();
            stroke.end();
            return true;
        }
        if (confirmingUnsaved) {
            handleUnsavedPrompt();
            wheelTicks.clearVertical();
            return true;
        }
        if (guideListPanel.isOpen()) {
            pointer.updatePosition();
            guideListPointerTargets.dispatch(pointer.pixelX(), pointer.pixelY(), leftClicked(),
                Gdx.input.isButtonPressed(Input.Buttons.LEFT));
            guideListPanel.handle(delta, wheelTicks.vertical());
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2) && flow.mode() == DeriveFlow.Mode.IDLE) {
            guideListPanel.open();
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5) && flow.mode() == DeriveFlow.Mode.IDLE) {
            reloadFontFiles();
            return true;
        }
        return false;
    }

    private boolean handleCommandKeys(float delta) {
        boolean control = Keys.controlDown();
        if (control && Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            save();
        }
        if (control && Gdx.input.isKeyJustPressed(Input.Keys.E) && !flow.isActive()) {
            exportFont();
        }
        if (derive.handleModalKeys()) {
            wheelTicks.clearVertical();
            return true;
        }
        if (handleEscapeKey()) {
            return true;
        }
        handleUndoKeys(control, delta);
        return handlePickOrShortcutKeys(control);
    }

    private boolean handleEscapeKey() {
        if (!Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            return false;
        }
        if (flow.isPicking()) {
            derive.cancelDerive();
        } else {
            leave();
        }
        return true;
    }

    private boolean handlePickOrShortcutKeys(boolean control) {
        if (flow.isPicking()) {
            return derive.handlePickingKeys();
        }
        if (control) {
            return false;
        }
        handleShortcutKeys();
        return flow.isActive() || newGlyph.isOpen();
    }

    private void handleEditing(float delta) {
        handlePreviewTabKey();
        moveCursor(delta);
        scrollRouting.handleScrollKeys();
        pointer.updateAndRouteWheel();
        pointerTargets.dispatch(pointer.pixelX(), pointer.pixelY(), leftClicked(),
            Gdx.input.isButtonPressed(Input.Buttons.LEFT));
    }

    private void reportEditProblem() {
        String problem = editor.problem();
        if (problem.equals(lastProblem)) {
            return;
        }
        lastProblem = problem;
        if (!problem.isEmpty()) {
            messages.nextAction();
            messages.warn("Edit rejected: " + problem);
        }
    }

    // The key bar owns the last row only while deriving or editing guides; otherwise the sheet and messages panels reach it.
    private boolean statusBarShown() {
        return flow.isActive() || guideListPanel.isOpen();
    }

    private void layoutPanels() {
        layoutBarShown = statusBarShown();
        PaintColumns.Split split = PaintColumns.split(
            worldWidth(), sheet.slotWidth(), PaintSheetView.GAP, editor.cellWidth(), cellPixels());
        sheet.setVisibleColumns(split.sheetColumns());
        sheet.fitToHeight(worldHeight(), layoutBarShown, cursor, revealCursor);
        upscale.layoutSheet(split.sheetColumns(), sheet.frameCells(), worldHeight(), layoutBarShown, revealCursor);
        revealCursor = false;
        rects = PaintLayout.compute(worldWidth() / cellPixels(), worldHeight() / cellPixels(),
            sheet.frameRight() / cellPixels(), split.rightCells(), new GlyphSize(editor.cellWidth(), editor.cellHeight()),
            cellPixels(), layoutBarShown);
        fitGrid();
    }

    // The grid takes the whole panel interior, with room above for the width labels and to the right for the height labels.
    private void fitGrid() {
        CellRect panel = rects.grid();
        int left = (panel.x() + 2) * cellPixels();
        int top = (panel.y() + 1 + PaintGridLabels.LABEL_ROWS_ABOVE) * cellPixels();
        grid.fitTo(left, top, (panel.width() - 3) * cellPixels(), (panel.bottom() - 1) * cellPixels() - top - cellPixels() / 2);
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
        ruleWarnings = RuleChecker.check(rulesFile, codePoint, glyph);
        rules.showBrokenRules(ruleWarnings);
        clearShownRuleWarnings();
        for (RuleWarning warning : ruleWarnings) {
            String messageKey = RULE_KEY_PREFIX + warning.key();
            messages.setCondition(messageKey, true, warning.message());
            shownRuleKeys.add(messageKey);
        }
    }

    private void clearShownRuleWarnings() {
        for (String messageKey : shownRuleKeys) {
            messages.clearCondition(messageKey);
        }
        shownRuleKeys.clear();
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
        runAfterUnsavedCheck(action, question, UNSAVED_BUTTONS);
    }

    private void exportFont() {
        stroke.end();
        runAfterUnsavedCheck(paintExport::run, EXPORT_QUESTION, SAVE_ONLY_BUTTONS);
    }

    private void runAfterUnsavedCheck(Runnable action, String question, List<Dialog.Button> buttons) {
        if (editor.isModified()) {
            messages.nextAction();
            afterUnsavedPrompt = action;
            unsavedQuestion = question;
            unsavedButtons = buttons;
            confirmingUnsaved = true;
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

    // Ctrl+Z undoes; Ctrl+Y and Ctrl+Shift+Z redo. Ignored while a stroke is active or a derive is in progress.
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
        int changed = editor.undoTarget();
        editor.undo();
        showChangedGlyph(changed, "Undid change to ");
    }

    private void redoOnce() {
        int changed = editor.redoTarget();
        editor.redo();
        showChangedGlyph(changed, "Redid change to ");
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

    private void handleShortcutKeys() {
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

    private final class MenuInputHost implements PaintMenuInput.Host {

        @Override
        public MenuBar<PaintMenu.Action> menuBar() {
            return menuBar;
        }

        @Override
        public void updatePointerPosition() {
            pointer.updatePosition();
        }

        @Override
        public int pixelX() {
            return pointer.pixelX();
        }

        @Override
        public int pixelY() {
            return pointer.pixelY();
        }

        @Override
        public int cellPixels() {
            return PaintScreen.this.cellPixels();
        }

        @Override
        public int worldWidth() {
            return PaintScreen.this.worldWidth();
        }

        @Override
        public boolean altDown() {
            return PaintScreen.altDown();
        }

        @Override
        public boolean leftClicked() {
            return PaintScreen.this.leftClicked();
        }

        @Override
        public boolean pointerBusy() {
            return stroke.isActive() || sheet.isDraggingColumnBar();
        }

        @Override
        public boolean renamingGuide() {
            return guideListPanel.isRenaming();
        }

        @Override
        public void runMenuAction(PaintMenu.Action action) {
            stroke.end();
            PaintScreen.this.runMenuAction(action);
        }
    }

    private final class ScrollRoutingHost implements PaintScrollRouting.Host {

        @Override
        public boolean helpShown() {
            return helpPanel.isShown();
        }

        @Override
        public boolean helpContains(int pixelX, int pixelY) {
            return helpPanel.contains(pixelX, pixelY, PaintScreen.this.cellPixels());
        }

        @Override
        public boolean isOverlayOperation() {
            return flow.isOverlayOperation();
        }

        @Override
        public int cellPixels() {
            return PaintScreen.this.cellPixels();
        }

        @Override
        public void unprojectInputToPointer() {
            pointer.unprojectInput();
        }

        @Override
        public int unprojectedPixelX() {
            return pointer.unprojectedPixelX();
        }

        @Override
        public int unprojectedPixelY() {
            return pointer.unprojectedPixelY();
        }
    }

    private final class CursorSlotSelection implements SheetSlotPointerTarget.Selection {

        @Override
        public boolean isSelectable(int slot) {
            return !unselectable[slot];
        }

        @Override
        public void select(int slot) {
            cursor = slot;
        }
    }

    private final class CursorStrokeStart implements GridStrokePointerTarget.StrokeContext {

        @Override
        public int cursorCodePoint() {
            return codePoints[cursor];
        }

        @Override
        public void clearLastProblem() {
            lastProblem = "";
        }
    }

    private void handleUnsavedPrompt() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            save();
            confirmingUnsaved = false;
            if (!editor.isModified()) {
                afterUnsavedPrompt.run();
            }
        } else if (unsavedButtons == UNSAVED_BUTTONS && Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            confirmingUnsaved = false;
            afterUnsavedPrompt.run();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            messages.nextAction();
            confirmingUnsaved = false;
        }
    }

    private final class ExportMessages implements PaintExport.Messages {

        @Override
        public void startAction() {
            messages.nextAction();
        }

        @Override
        public void ok(String text) {
            messages.ok(text);
        }

        @Override
        public void warn(String text) {
            messages.warn(text);
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
        if (statusBarShown() != layoutBarShown || upscale.needsLayout()) {
            layoutPanels();
        }
        kit.beginFrame();
        int codePoint = codePoints[cursor];
        int columns = worldWidth() / cellPixels();
        int rows = worldHeight() / cellPixels();
        drawBase(codePoint, columns, rows);
        refreshLayers();
        drawDialogs(codePoint, columns, rows);
        drawTopLayers(codePoint, columns, rows);
        kit.endFrame();
    }

    private void drawBase(int codePoint, int columns, int rows) {
        TopBar.draw(painter, columns, CRUMB_ROW,
            List.of("GLYPH WORKBENCH", folderName, "PAINT " + fontSize.toString().toUpperCase(java.util.Locale.ROOT)),
            new TopBarStatus(uiScale.label(), editor.isModified() ? "● MODIFIED" : "", UiPalette.ORANGE));
        boolean choosingSource = flow.isPicking() || flow.mode() == DeriveFlow.Mode.CONFIRM_REPLACE;
        drawSheet(choosingSource);
        CellRect panel = rects.grid();
        glyphTitle.draw(painter, panel, codePoint, Gdx.graphics.getDeltaTime());
        ghostSeconds += Gdx.graphics.getDeltaTime();
        grid.setHighlight(guideListPanel.highlightedGuide());
        PixelGlyph previewGlyph = previewGlyph();
        grid.draw(codePoint, previewGlyph, derive.pendingGhost(), ghostSeconds);
        preview.draw(rects.preview(), codePoint, previewGlyph);
        drawBelowPreviewPanels();
        CellRect strip = rects.messages();
        messages.draw(painter, strip,
            Gdx.graphics.getDeltaTime());
        if (layoutBarShown) {
            StatusBar.draw(painter, columns, rows - 1,
                statusKeys(choosingSource));
        }
    }

    // The source sheet takes the main sheet's place while a glyph is picked for upscale.
    private void drawSheet(boolean choosingSource) {
        if (upscale.isPicking()) {
            upscale.drawSheet();
            return;
        }
        sheet.draw(cursor, choosingSource ? flow.sourceSlot() : -1);
    }

    private void refreshLayers() {
        boolean newGlyphOpen = newGlyph.isOpen();
        boolean newSizeOpen = newSize.isOpen();
        boolean upscaleSourceOpen = upscale.isDialogShown();
        boolean newFontOpen = newFont.isOpen();
        boolean deriveDims = deriveDimsScreen();
        boolean controlsOpen = controls.isOpen();
        boolean rulesShowing = rules.isShowing();
        boolean menuOpen = menuBar.isOpen();
        layers.update(new PaintLayers.State(confirmingUnsaved, newGlyphOpen, newSizeOpen, upscaleSourceOpen,
            newFontOpen, deriveDims, controlsOpen, rulesShowing, menuOpen));
    }

    private void drawDialogs(int codePoint, int columns, int rows) {
        if (layers.menuBarBelowDim()) {
            MenuBarView.drawBar(painter, menuBar, columns, MENU_ROW, menuAvailability);
        }
        if (layers.unsavedPromptShown()) {
            Dialog.draw(painter, new CellRect(0, 0, columns, rows),
                DialogContent.of("UNSAVED CHANGES",
                    List.of("Save changes to " + dataFile.name(), unsavedQuestion), unsavedButtons));
        }
        if (layers.newGlyphShown()) {
            newGlyph.draw(painter, columns, rows, rects.grid());
        }
        if (layers.newFontShown()) {
            newFont.draw(painter, columns, rows, rects.grid());
        }
        if (layers.newSizeShown()) {
            newSize.draw(painter, columns, rows, rects.grid());
        }
        if (layers.upscaleSourceShown()) {
            upscale.drawDialog(painter, columns, rows, rects.grid());
        }
        if (layers.deriveDialogAllowed()) {
            derive.drawDialog(painter, columns, rows, rects.grid(), codePoint);
        }
        if (layers.redrawsDeriveHelp()) {
            helpPanel.redraw(helpState(), cellPixels());
        }
        guideListPanel.drawPicker(columns, rows, rects.grid());
    }

    private void drawTopLayers(int codePoint, int columns, int rows) {
        if (!layers.menuBarBelowDim()) {
            MenuBarView.drawBar(painter, menuBar, columns, MENU_ROW, menuAvailability);
        }
        MenuBarView.drawDropDown(painter, menuBar, columns, MENU_ROW, menuAvailability);
        rules.draw(worldWidth(), worldHeight(), codePoint);
        if (layers.controlsShown()) {
            controls.draw(painter, columns, rows);
        }
    }

    private PaintHelp.State helpState() {
        return upscale.isPicking() ? upscale.helpState() : derive.helpState();
    }

    // The upscale choice and the derive pick never run together.
    private PixelGlyph previewGlyph() {
        PixelGlyph upscaled = upscale.previewGlyph();
        return upscaled != null ? upscaled : derivePick.shown();
    }

    private boolean deriveDimsScreen() {
        return derive.dialogShown();
    }

    // Input runs before the frame's layers are refreshed, so it calls the static check directly.
    private boolean dialogDimsScreen() {
        return PaintLayers.dialogDimsScreen(confirmingUnsaved, newGlyph.isOpen(), newSize.isOpen(),
            upscale.isDialogShown(), newFont.isOpen(), deriveDimsScreen());
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
        return flow.isOverlayOperation() ? OVERLAY_PICK_KEYS : PICK_KEYS;
    }

    // Draws the area under the preview: the guide list when open, otherwise the help panel.
    private void drawBelowPreviewPanels() {
        if (guideListPanel.isOpen()) {
            guideListPanel.draw(rects.belowPreview(), cellPixels());
            helpPanel.markHidden();
            return;
        }
        helpPanel.draw(rects.belowPreview(), helpState(), cellPixels());
    }

    @Override
    public void hide() {
        clearWindowSizeLimits();
        kit.dispose();
    }
}
