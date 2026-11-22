package com.github.halfdeadgames.asciigdx.skindesigner;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.github.halfdeadgames.asciigdx.fit.IntegerScaling;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.layer.LayerStack;
import com.github.halfdeadgames.asciigdx.skindesigner.document.SkinDocument;
import com.github.halfdeadgames.asciigdx.skindesigner.files.SkinFileMenu;
import com.github.halfdeadgames.asciigdx.skindesigner.files.SkinSession;
import com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase.BoundaryState;
import com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase.ExampleFrame;
import com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase.ExamplePicker;
import com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase.PopupBoundaryOverlay;
import com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase.ShowcaseBackdrop;
import com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase.WidgetExample;
import com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase.WidgetExamples;
import com.github.halfdeadgames.asciigdx.ui.CellArea;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.Menu;
import com.github.halfdeadgames.asciigdx.ui.MenuBar;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.UiInput;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.UiSurface;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows every widget of the skin in all its styles and forms, one widget class at a time, to tune
 * {@code default-skin.json}. Choose the example from the "Widgets" menu, rebuild it with F5.
 */
class UiShowcaseScene {
    private static final int INITIAL_COLUMNS = 120;
    private static final int INITIAL_ROWS = 67;
    private static final int MIN_ROWS_PER_SHORT_SIDE = 67;
    private static final int MENU_BAR_ROWS = 2;
    // One status row plus one empty row.
    private static final int STATUS_ROWS = 2;
    private static final String SKIN_FILE = "default-skin.json";
    private static final String PICKER_OPEN_VARIANT = "picker-open";
    private static final String SCROLL_PANE_STYLE = "scrollPane";
    private static final String LABEL_STYLE = "text";
    private static final String BAR_STYLE = "menuBar";
    private static final String INITIAL_RESULT = "Loaded";
    private static final String REBUILD_RESULT = "Rebuilt the example";
    private static final float UNSAVED_CHECK_SECONDS = 0.25f;

    private final List<WidgetExample> examples = WidgetExamples.createAll();
    private final ScalingViewport viewport;
    private final GlyphSource squareGlyphs;
    private final GlyphSource textGlyphs;
    private SkinDocument document;
    private final SkinFileMenu fileMenu;
    private final Vector2 worldPoint = new Vector2();
    private final BoundaryState boundaryState = new BoundaryState();
    private UiSurface surface;
    private UiRoot uiRoot;
    private UiInput uiInput;
    private MenuBar menuBar;
    private ExamplePicker picker;
    private Label statusLine;
    private Label borderNote;
    private LayerStack layers;
    private Widget content;
    private int columns = INITIAL_COLUMNS;
    private int rows = INITIAL_ROWS;
    private int exampleIndex;
    private int overlayRows = MENU_BAR_ROWS;
    private float sceneSeconds;
    private String lastResult = INITIAL_RESULT;
    private float secondsSinceUnsavedCheck;
    private boolean shownUnsaved;

    UiShowcaseScene(GlyphSource squareGlyphs, GlyphSource textGlyphs) {
        this.squareGlyphs = squareGlyphs;
        this.textGlyphs = textGlyphs;
        document = SkinDocument.open(Gdx.files.internal(SKIN_FILE), true);
        fileMenu = new SkinFileMenu(SkinSession.createForRunningApp(document), this::reportResult,
                this::adoptLoadedDocument, () -> uiRoot, this::exampleArea);
        viewport = new ScalingViewport(IntegerScaling.INSTANCE,
                columns * squareGlyphs.getCellWidth(), rows * squareGlyphs.getCellHeight());
        buildUi();
    }

    // A UiSurface never resizes, so a new cell count means a whole new surface, root, and widgets.
    // The glyph sources are shared and owned by the caller; LayerStack.dispose leaves them alone.
    private void buildUi() {
        LayerStack oldLayers = layers;
        if (content != null) {
            examples.get(exampleIndex).exampleHidden();
        }
        surface = UiSurface.ofSize(columns, rows);
        uiRoot = UiRoot.of(surface);
        uiInput = UiInput.forRoot(uiRoot, this::pickCell);
        picker = new ExamplePicker(uiRoot, document.getSkin(), examples, this::loadExample);
        layers = new LayerStack();
        content = null;
        surface.addTo(layers, textGlyphs, squareGlyphs);
        uiRoot.getRoot().addChild(
                new ShowcaseBackdrop(columns, rows, document.getSkin().getRgba("black")));
        createMenuBar();
        createStatusLine();
        createBorderNote();
        showExample(exampleIndex);
        PopupBoundaryOverlay.installOn(uiRoot, boundaryState, columns, rows);
        limitWindowsToExampleArea();
        if (oldLayers != null) {
            oldLayers.dispose();
        }
    }

    private void createMenuBar() {
        menuBar = MenuBar.of(uiRoot, document.getSkin().getMenuBarStyle(BAR_STYLE));
        menuBar.setSize(columns, 0);
        menuBar.setPosition(0, overlayRows);
        createFileMenu();
        Menu widgetsMenu = menuBar.addMenu("Widgets", 'W');
        widgetsMenu.addItem("Choose widget…", 'C', this::openPicker);
        widgetsMenu.addItem("Rebuild example (F5)", 'R', this::rebuildExample);
        widgetsMenu.addItem("Reload skin", 'k', this::reloadSkin);
        widgetsMenu.addItem("Show or hide widget border (F6)", 'B', this::toggleBorder);
        uiRoot.getRoot().addChild(menuBar);
    }

    private void createFileMenu() {
        fileMenu.addTo(menuBar);
    }

    private void createStatusLine() {
        statusLine = new Label("", document.getSkin().getLabelStyle(LABEL_STYLE));
        statusLine.setPosition(1, overlayRows + MENU_BAR_ROWS);
        uiRoot.getRoot().addChild(statusLine);
    }

    private void createBorderNote() {
        borderNote = new Label("", document.getSkin().getLabelStyle(LABEL_STYLE));
        borderNote.setPosition(1, overlayRows + MENU_BAR_ROWS + 1);
        uiRoot.getRoot().addChild(borderNote);
        refreshBorderNote();
    }

    private void refreshBorderNote() {
        borderNote.setText(boundaryState.isShown()
                ? "The red border is NOT part of the widget. Everything drawn inside the"
                        + " border is the widget, also for menus, tooltips, windows, and"
                        + " dialogs. F6 hides the border."
                : "Widget border hidden. F6 shows it again.");
        borderNote.pack();
    }

    private void toggleBorder() {
        boundaryState.toggleShown();
        refreshBorderNote();
        uiRoot.draw();
    }

    private void refreshStatusLine() {
        shownUnsaved = document.hasUnsavedChanges();
        statusLine.setText("Example: " + examples.get(exampleIndex).title() + "  |  File: "
                + fileMenu.describeFile() + "  |  Last: " + lastResult);
        statusLine.pack();
    }

    private void reportResult(String message) {
        lastResult = message;
        refreshStatusLine();
        uiRoot.draw();
    }

    private void adoptLoadedDocument() {
        document = fileMenu.getDocument();
        buildUi();
    }

    // Edits and undo change the document without going through the File menu, so the "unsaved"
    // marker is checked a few times a second; formatting the tree every frame would be wasteful.
    private void refreshUnsavedMarker(float delta) {
        secondsSinceUnsavedCheck += delta;
        if (secondsSinceUnsavedCheck < UNSAVED_CHECK_SECONDS) {
            return;
        }
        secondsSinceUnsavedCheck = 0f;
        if (document.hasUnsavedChanges() != shownUnsaved) {
            refreshStatusLine();
        }
    }

    private CellArea exampleArea() {
        return new CellArea(0, contentTop(), columns, Math.max(1, rows - contentTop()));
    }

    boolean hasUnsavedChanges() {
        return document.hasUnsavedChanges();
    }

    /** Runs the quit action now, or after Save or Discard when changes are unsaved. */
    void requestQuit(Runnable quitAction) {
        fileMenu.requestQuit(quitAction);
    }

    private void showExample(int index) {
        if (content != null) {
            examples.get(exampleIndex).exampleHidden();
            uiRoot.getRoot().removeChild(content);
        }
        exampleIndex = index;
        boundaryState.forgetFrames();
        int areaTop = contentTop();
        int areaRows = rows - areaTop;
        Skin skin = document.getSkin();
        Widget tree = examples.get(index).create(skin, uiRoot, boundaryState, columns, areaRows);
        content = ExampleFrame.fit(tree, columns, areaRows,
                skin.getScrollPaneStyle(SCROLL_PANE_STYLE));
        content.setPosition(0, areaTop);
        uiRoot.getRoot().addChild(content);
        refreshStatusLine();
        uiRoot.draw();
    }

    private void loadExample(WidgetExample example) {
        lastResult = "Loaded " + example.title();
        showExample(examples.indexOf(example));
    }

    private void openPicker() {
        int areaTop = contentTop();
        picker.open(exampleIndex, 0, areaTop, columns, rows - areaTop);
        uiRoot.draw();
    }

    private void rebuildExample() {
        lastResult = REBUILD_RESULT;
        buildUi();
    }

    // A broken skin file keeps the old skin and the current screen; only the status line changes.
    private void reloadSkin() {
        fileMenu.reloadFromDisk();
    }

    private void limitWindowsToExampleArea() {
        int areaRows = Math.max(1, rows - contentTop());
        uiRoot.setWindowDragBounds(new CellArea(0, contentTop(), columns, areaRows));
    }

    private int contentTop() {
        return overlayRows + MENU_BAR_ROWS + STATUS_ROWS;
    }

    private int exampleIndexOf(String exampleId) {
        for (int i = 0; i < examples.size(); i++) {
            if (examples.get(i).id().equals(exampleId)) {
                return i;
            }
        }
        return -1;
    }

    private void moveBelowOverlay() {
        menuBar.setPosition(0, overlayRows);
        statusLine.setPosition(1, overlayRows + MENU_BAR_ROWS);
        borderNote.setPosition(1, overlayRows + MENU_BAR_ROWS + 1);
        content.setPosition(0, contentTop());
        limitWindowsToExampleArea();
        uiRoot.draw();
    }

    private void pickCell(int screenX, int screenY, GridPoint2 cellOut) {
        worldPoint.set(screenX, screenY);
        viewport.unproject(worldPoint);
        cellOut.x = (int) Math.floor(worldPoint.x / textGlyphs.getCellWidth());
        cellOut.y = (int) Math.floor(
                (viewport.getWorldHeight() - worldPoint.y) / squareGlyphs.getCellHeight());
    }

    String statusText() {
        return "Example: " + examples.get(exampleIndex).title();
    }

    boolean keyDown(int keycode) {
        if (uiInput.keyDown(keycode)) {
            return true;
        }
        if (keycode == Input.Keys.F5) {
            rebuildExample();
            return true;
        }
        if (keycode == Input.Keys.F6) {
            toggleBorder();
            return true;
        }
        if (keycode == Input.Keys.ESCAPE && picker.isOpen()) {
            picker.close();
            return true;
        }
        return false;
    }

    boolean keyUp(int keycode) {
        return uiInput.keyUp(keycode);
    }

    boolean keyTyped(char character) {
        return uiInput.keyTyped(character);
    }

    List<String> captureVariants() {
        List<String> ids = new ArrayList<>(examples.size() + 1);
        for (int i = 0; i < examples.size(); i++) {
            ids.add(examples.get(i).id());
        }
        ids.add(PICKER_OPEN_VARIANT);
        return ids;
    }

    void showCaptureVariant(String variant) {
        if (variant.equals(PICKER_OPEN_VARIANT)) {
            openPicker();
            return;
        }
        int index = exampleIndexOf(variant);
        if (index < 0) {
            return;
        }
        picker.close();
        lastResult = INITIAL_RESULT;
        showExample(index);
    }

    void resize(int width, int height) {
        boolean minimized = width <= 0 || height <= 0;
        if (minimized) {
            return;
        }
        int integerScale = integerScaleFor(width, height);
        int cellWidthInPixels = squareGlyphs.getCellWidth() * integerScale;
        int cellHeightInPixels = squareGlyphs.getCellHeight() * integerScale;
        int newColumns = Math.max(1, width / cellWidthInPixels);
        int newRows = Math.max(1, height / cellHeightInPixels);
        int newOverlayRows = rowsCoveredByPerfOverlay(integerScale);
        boolean overlayMoved = newOverlayRows != overlayRows;
        overlayRows = newOverlayRows;
        viewport.setWorldSize(newColumns * squareGlyphs.getCellWidth(),
                newRows * squareGlyphs.getCellHeight());
        viewport.update(width, height, true);
        if (newColumns != columns || newRows != rows) {
            columns = newColumns;
            rows = newRows;
            buildUi();
            return;
        }
        if (overlayMoved) {
            moveBelowOverlay();
        }
    }

    // Grows the cells once the short side holds several 67-row screens, so 1080p stays at 1x.
    private int integerScaleFor(int width, int height) {
        int shortSide = Math.min(width, height);
        return Math.max(1,
                shortSide / (MIN_ROWS_PER_SHORT_SIDE * squareGlyphs.getCellHeight()));
    }

    private int rowsCoveredByPerfOverlay(int integerScale) {
        int cellHeightInPixels = squareGlyphs.getCellHeight() * integerScale;
        int overlayHeightInPixels = PerfOverlay.heightInScreenPixels(squareGlyphs);
        int rowsCovered = (overlayHeightInPixels + cellHeightInPixels - 1) / cellHeightInPixels;
        return rowsCovered + rowsCovered % 2;
    }

    boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return uiInput.touchDown(screenX, screenY, pointer, button);
    }

    boolean touchDragged(int screenX, int screenY, int pointer) {
        return uiInput.touchDragged(screenX, screenY, pointer);
    }

    boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return uiInput.touchCancelled(screenX, screenY, pointer, button);
    }

    boolean scrolled(float amountX, float amountY) {
        return uiInput.scrolled(amountX, amountY);
    }

    boolean mouseMoved(int screenX, int screenY) {
        return uiInput.mouseMoved(screenX, screenY);
    }

    boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return uiInput.touchUp(screenX, screenY, pointer, button);
    }

    void render(SpriteBatch batch, float delta) {
        fileMenu.update(delta);
        refreshUnsavedMarker(delta);
        uiRoot.update(delta);
        uiRoot.draw();
        applyExampleEffects(delta);
        ViewportBackground.clearArea(viewport);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        layers.draw(batch, 0, viewport.getWorldHeight());
        batch.end();
    }

    private void applyExampleEffects(float delta) {
        sceneSeconds += delta;
        layers.beginCellEffects(delta);
        examples.get(exampleIndex).applyEffects(surface.getTextLayer(), sceneSeconds);
    }

    void dispose() {
        layers.dispose();
    }

    int cellCount() {
        return columns * rows;
    }
}
