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
import com.github.halfdeadgames.asciigdx.ui.Menu;
import com.github.halfdeadgames.asciigdx.ui.MenuBar;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.TextField;
import com.github.halfdeadgames.asciigdx.ui.Tooltip;
import com.github.halfdeadgames.asciigdx.ui.UiInput;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.UiSurface;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pages of UI widget checks, switched with the number keys 1 to 9 or with the "Pages" menu of the
 * tall menu bar on the first row below the performance overlay.
 */
class UiWidgetsScene implements SandboxScene {
    private static final int INITIAL_COLUMNS = 120;
    private static final int INITIAL_ROWS = 67;
    private static final int MIN_ROWS_PER_SHORT_SIDE = 67;
    private static final int RESERVED_ROWS = 2;
    private static final int MAX_KEY_PAGES = 9;
    private static final String SKIN_FILE = "default-skin.json";
    private static final String OPEN_PAGES_MENU_VARIANT = "pages-menu-open";
    private static final String OPEN_SHORT_MENU_VARIANT = "menus-open";
    private static final String MENUS_PAGE_ID = "menus";

    private final Skin skin = Skin.fromFile(Gdx.files.internal(SKIN_FILE));
    private final ScalingViewport viewport;
    private final GlyphSource squareGlyphs;
    private final GlyphSource tallGlyphs;
    private final Vector2 worldPoint = new Vector2();
    private UiSurface surface;
    private UiRoot uiRoot;
    private UiInput uiInput;
    private ButtonsPage buttonsPage;
    private BarsPage barsPage;
    private HintsPage hintsPage;
    private MenusPage menusPage;
    private TextFieldsPage textFieldsPage;
    private List<UiPage> pages;
    private MenuBar pageBar;
    private LayerStack layers;
    private Widget content;
    private int columns = INITIAL_COLUMNS;
    private int rows = INITIAL_ROWS;
    private int pageIndex;
    private int overlayRows = RESERVED_ROWS;

    UiWidgetsScene(GlyphSource squareGlyphs, GlyphSource tallGlyphs) {
        this.squareGlyphs = squareGlyphs;
        this.tallGlyphs = tallGlyphs;
        viewport = new ScalingViewport(IntegerScaling.INSTANCE,
                columns * squareGlyphs.getCellWidth(), rows * squareGlyphs.getCellHeight());
        buildUi();
    }

    // A UiSurface never resizes, so a new cell count means a whole new surface, root, and widgets.
    // The glyph sources are shared and owned by the caller; LayerStack.dispose leaves them alone.
    private void buildUi() {
        LayerStack oldLayers = layers;
        surface = UiSurface.ofSize(columns, rows);
        uiRoot = UiRoot.of(surface);
        uiInput = UiInput.forRoot(uiRoot, this::pickCell);
        hintsPage = new HintsPage(uiRoot, skin);
        menusPage = new MenusPage(uiRoot, skin);
        buttonsPage = new ButtonsPage(uiRoot, skin);
        textFieldsPage = new TextFieldsPage(uiRoot, skin);
        barsPage = new BarsPage(uiRoot, skin);
        pages = List.of(new LabelsPage(skin), new TablePage(skin), buttonsPage,
                hintsPage, menusPage, new TogglesPage(skin), textFieldsPage, barsPage,
                new RatingsPage(skin), new ScrollingPage(skin));
        pageBar = MenuBar.of(uiRoot, skin.getMenuBarStyle("tallMenuBar"));
        layers = new LayerStack();
        content = null;
        surface.addTo(layers, tallGlyphs, squareGlyphs);
        uiRoot.getRoot().addChild(new Backdrop(columns, rows, skin.getRgba("black")));
        createPageBar();
        showPage(pageIndex);
        if (oldLayers != null) {
            oldLayers.dispose();
        }
    }

    private void createPageBar() {
        pageBar.setSize(columns, 0);
        pageBar.setPosition(0, overlayRows);
        Menu pagesMenu = pageBar.addMenu("Pages", 'P');
        for (int i = 0; i < pages.size(); i++) {
            int pageToShow = i;
            String title = pages.get(i).title();
            pagesMenu.addItem((i + 1) + " " + title, title.charAt(0),
                    () -> showPage(pageToShow));
        }
        uiRoot.getRoot().addChild(pageBar);
    }

    private void showPage(int index) {
        pageIndex = index;
        closeOpenMenu();
        if (content != null) {
            uiRoot.getRoot().removeChild(content);
        }
        int contentTop = overlayRows + RESERVED_ROWS;
        content = pages.get(pageIndex).createContent(columns, rows - contentTop);
        content.setPosition(0, contentTop);
        uiRoot.getRoot().addChild(content);
        if (pages.get(pageIndex) == buttonsPage) {
            buttonsPage.focusFirstButton();
        }
        if (pages.get(pageIndex) == textFieldsPage) {
            textFieldsPage.focusFirstField();
        }
        uiRoot.draw();
    }

    // Escape reaches the open menu through the keyboard focus. An item action runs after its menu
    // has closed, and input methods must not be called then, so only press Escape if one is open.
    private void closeOpenMenu() {
        if (isAnyMenuOpen()) {
            uiRoot.keyDown(Input.Keys.ESCAPE);
        }
    }

    private boolean isAnyMenuOpen() {
        return pageBar.isOpen() || menusPage.isMenuOpen();
    }

    private void moveBelowOverlay() {
        pageBar.setPosition(0, overlayRows);
        content.setPosition(0, overlayRows + RESERVED_ROWS);
        uiRoot.draw();
    }

    private int contentTop() {
        return overlayRows + RESERVED_ROWS;
    }

    private int pageIndexOf(String pageId) {
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).id().equals(pageId)) {
                return i;
            }
        }
        return -1;
    }

    private void clickCell(int column, int row) {
        uiRoot.pointerDown(column, row, 0, 0);
        uiRoot.pointerUp(column, row, 0, 0);
    }

    private void pickCell(int screenX, int screenY, GridPoint2 cellOut) {
        worldPoint.set(screenX, screenY);
        viewport.unproject(worldPoint);
        cellOut.x = (int) Math.floor(worldPoint.x / squareGlyphs.getCellWidth());
        cellOut.y = (int) Math.floor(
                (viewport.getWorldHeight() - worldPoint.y) / squareGlyphs.getCellHeight());
    }

    /** Black fill under everything; lets no pointer hit through it. */
    private static final class Backdrop extends Widget {
        private final int columns;
        private final int rows;
        private final int fillRgba;

        Backdrop(int columns, int rows, int fillRgba) {
            this.columns = columns;
            this.rows = rows;
            this.fillRgba = fillRgba;
            setSize(columns, rows);
        }

        @Override
        public Widget hit(int column, int row) {
            return null;
        }

        @Override
        public int getPreferredWidth() {
            return columns;
        }

        @Override
        public int getPreferredHeight() {
            return rows;
        }

        @Override
        protected void drawContent(UiSurface target, int left, int top) {
            target.fillBackground(left, top, columns, rows, fillRgba);
        }
    }

    @Override
    public String id() {
        return "ui-widgets";
    }

    @Override
    public String title() {
        return "UI: widgets";
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
        return "Page " + (pageIndex + 1) + ": " + pages.get(pageIndex).title();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (uiInput.keyDown(keycode)) {
            return true;
        }
        if (isAnyMenuOpen() || uiRoot.getKeyboardFocus() instanceof TextField) {
            return false;
        }
        int index = keycode - Input.Keys.NUM_1;
        if (index < 0 || index >= MAX_KEY_PAGES || index >= pages.size()) {
            return false;
        }
        showPage(index);
        return true;
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
        List<String> ids = new ArrayList<>(pages.size() + 2);
        for (int i = 0; i < pages.size(); i++) {
            ids.add(pages.get(i).id());
        }
        ids.add(OPEN_PAGES_MENU_VARIANT);
        ids.add(OPEN_SHORT_MENU_VARIANT);
        return ids;
    }

    @Override
    public void showCaptureVariant(String variant) {
        if (variant.equals(OPEN_PAGES_MENU_VARIANT)) {
            showPage(0);
            clickCell(1, overlayRows);
            return;
        }
        if (variant.equals(OPEN_SHORT_MENU_VARIANT)) {
            showPage(pageIndexOf(MENUS_PAGE_ID));
            clickCell(MenusPage.BAR_X + 1, contentTop() + MenusPage.BAR_Y);
            return;
        }
        int index = pageIndexOf(variant);
        if (index >= 0) {
            showPage(index);
            if (pages.get(index) == hintsPage) {
                showHintWithoutMouse();
            }
            if (pages.get(index) == buttonsPage) {
                buttonsPage.focusFirstButtonVisibly();
                uiRoot.draw();
            }
            if (pages.get(index) == textFieldsPage) {
                textFieldsPage.focusFirstFieldVisibly();
                uiRoot.draw();
            }
            if (pages.get(index) == barsPage) {
                barsPage.focusFirstSliderVisibly();
                uiRoot.draw();
            }
        }
    }

    private void showHintWithoutMouse() {
        uiRoot.pointerMoved(content.getX() + hintsPage.captureColumn(),
                content.getY() + hintsPage.captureRow());
        uiRoot.update(Tooltip.DEFAULT_DELAY_SECONDS);
        uiRoot.draw();
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

    // UiInput also ends the hover after a lift while no mouse has been seen (touch-only devices).
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return uiInput.touchUp(screenX, screenY, pointer, button);
    }

    @Override
    public void render(SpriteBatch batch, float delta) {
        uiRoot.update(delta);
        uiRoot.draw();
        SceneDrawing.clearViewportArea(viewport);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        layers.draw(batch, 0, viewport.getWorldHeight());
        batch.end();
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
