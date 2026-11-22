package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.github.halfdeadgames.asciigdx.fit.IntegerScaling;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.layer.LayerStack;
import com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase.BoundaryState;
import com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase.ExampleFrame;
import com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase.ExamplePicker;
import com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase.PopupBoundaryOverlay;
import com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase.ShowcaseBackdrop;
import com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase.WidgetExample;
import com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase.WidgetExamples;
import com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase.SkinFileWatcher;
import com.github.halfdeadgames.asciigdx.ui.CellArea;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.Menu;
import com.github.halfdeadgames.asciigdx.ui.MenuBar;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.SkinFileException;
import com.github.halfdeadgames.asciigdx.ui.UiInput;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.UiSurface;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shows every widget of the skin in all its styles and forms, one widget class at a time, to tune
 * {@code default-skin.json}. Choose the example from the "Widgets" menu, rebuild it with F5.
 */
class UiShowcaseScene implements SandboxScene {
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
    private static final String RELOADED_RESULT = "Skin reloaded";
    private static final String ERROR_PREFIX = "Skin error: ";

    private final SkinFileWatcher skinWatcher =
            new SkinFileWatcher(Gdx.files.internal(SKIN_FILE));
    private final List<WidgetExample> examples = WidgetExamples.createAll();
    private final ScalingViewport viewport;
    private final GlyphSource squareGlyphs;
    private final GlyphSource textGlyphs;
    private Skin skin = skinWatcher.load();
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

    UiShowcaseScene(GlyphSource squareGlyphs, GlyphSource textGlyphs) {
        this.squareGlyphs = squareGlyphs;
        this.textGlyphs = textGlyphs;
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
        picker = new ExamplePicker(uiRoot, skin, examples, this::loadExample);
        layers = new LayerStack();
        content = null;
        surface.addTo(layers, textGlyphs, squareGlyphs);
        uiRoot.getRoot().addChild(new ShowcaseBackdrop(columns, rows, skin.getRgba("black")));
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
        menuBar = MenuBar.of(uiRoot, skin.getMenuBarStyle(BAR_STYLE));
        menuBar.setSize(columns, 0);
        menuBar.setPosition(0, overlayRows);
        Menu widgetsMenu = menuBar.addMenu("Widgets", 'W');
        widgetsMenu.addItem("Choose widget…", 'C', this::openPicker);
        widgetsMenu.addItem("Rebuild example (F5)", 'R', this::rebuildExample);
        widgetsMenu.addItem("Reload skin", 'k', this::reloadSkin);
        widgetsMenu.addItem("Show or hide widget border (F6)", 'B', this::toggleBorder);
        uiRoot.getRoot().addChild(menuBar);
    }

    private void createStatusLine() {
        statusLine = new Label("", skin.getLabelStyle(LABEL_STYLE));
        statusLine.setPosition(1, overlayRows + MENU_BAR_ROWS);
        uiRoot.getRoot().addChild(statusLine);
    }

    private void createBorderNote() {
        borderNote = new Label("", skin.getLabelStyle(LABEL_STYLE));
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
        statusLine.setText("Example: " + examples.get(exampleIndex).title() + "  |  Last: "
                + lastResult);
        statusLine.pack();
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
        try {
            skin = skinWatcher.load();
        } catch (SkinFileException exception) {
            lastResult = ERROR_PREFIX + exception.getMessage();
            refreshStatusLine();
            uiRoot.draw();
            return;
        }
        lastResult = RELOADED_RESULT;
        buildUi();
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

    @Override
    public String id() {
        return "ui-showcase";
    }

    @Override
    public String title() {
        return "UI: showcase";
    }

    @Override
    public SceneGroup group() {
        return SceneGroup.UI;
    }

    @Override
    public Optional<WindowSize> preferredWindowSize() {
        return Optional.of(WindowSize.FULL_HD);
    }

    @Override
    public String statusText() {
        return "Example: " + examples.get(exampleIndex).title();
    }

    @Override
    public boolean keyDown(int keycode) {
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

    @Override
    public boolean keyUp(int keycode) {
        return uiInput.keyUp(keycode);
    }

    @Override
    public boolean keyTyped(char character) {
        return uiInput.keyTyped(character);
    }

    @Override
    public List<String> captureVariants() {
        List<String> ids = new ArrayList<>(examples.size() + 1);
        for (int i = 0; i < examples.size(); i++) {
            ids.add(examples.get(i).id());
        }
        ids.add(PICKER_OPEN_VARIANT);
        return ids;
    }

    @Override
    public void showCaptureVariant(String variant) {
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

    @Override
    public void show() {
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void resize(int width, int height) {
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

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return uiInput.touchDown(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return uiInput.touchDragged(screenX, screenY, pointer);
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return uiInput.touchCancelled(screenX, screenY, pointer, button);
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return uiInput.scrolled(amountX, amountY);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return uiInput.mouseMoved(screenX, screenY);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return uiInput.touchUp(screenX, screenY, pointer, button);
    }

    @Override
    public void render(SpriteBatch batch, float delta) {
        skinWatcher.update(delta);
        if (skinWatcher.hasChangeBeenDetected()) {
            reloadSkin();
        }
        uiRoot.update(delta);
        uiRoot.draw();
        applyExampleEffects(delta);
        SceneDrawing.clearViewportArea(viewport);
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

    // A key-up that arrives after the scene is hidden never reaches the root, so a held arrow
    // would keep its repeat armed.
    @Override
    public void hide() {
        uiRoot.releaseHeldKeys();
    }

    @Override
    public void dispose() {
        layers.dispose();
    }

    @Override
    public int cellCount() {
        return columns * rows;
    }
}
