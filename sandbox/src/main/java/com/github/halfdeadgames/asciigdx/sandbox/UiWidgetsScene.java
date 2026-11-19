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
import com.github.halfdeadgames.asciigdx.ui.ButtonStyle;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.Tooltip;
import com.github.halfdeadgames.asciigdx.ui.Menu;
import com.github.halfdeadgames.asciigdx.ui.MenuBar;
import com.github.halfdeadgames.asciigdx.ui.MenuBarStyle;
import com.github.halfdeadgames.asciigdx.ui.UiFont;
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
    private static final int COLUMNS = 120;
    private static final int ROWS = 67;
    private static final int RESERVED_ROWS = 2;
    private static final int MAX_KEY_PAGES = 9;
    private static final int BLACK = 0x000000FF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GOLD = 0xFFD700FF;
    private static final int BAR_BLUE = 0x203070FF;
    private static final int OVER_TEAL = 0x00A0A0FF;
    private static final int TRIGGER_PINK = 0xFF40C0FF;
    private static final String OPEN_PAGES_MENU_VARIANT = "pages-menu-open";
    private static final String OPEN_SHORT_MENU_VARIANT = "menus-open";
    private static final String MENUS_PAGE_ID = "menus";
    private static final ButtonStyle BAR_LOOK = ButtonStyle.of(
            LabelStyle.of(UiFont.TALL, WHITE, BAR_BLUE),
            LabelStyle.of(UiFont.TALL, BLACK, OVER_TEAL),
            LabelStyle.of(UiFont.TALL, BLACK, GOLD),
            LabelStyle.of(UiFont.TALL, WHITE, BAR_BLUE));

    private final UiSurface surface = UiSurface.ofSize(COLUMNS, ROWS);
    private final UiRoot uiRoot = UiRoot.of(surface);
    private final HintsPage hintsPage = new HintsPage(uiRoot);
    private final MenusPage menusPage = new MenusPage(uiRoot);
    private final List<UiPage> pages = List.of(
            new LabelsPage(),
            new TablePage(),
            new ButtonsPage(),
            hintsPage,
            menusPage
    );
    private final MenuBar pageBar = MenuBar.of(uiRoot, MenuBarStyle.of(BAR_LOOK, BAR_LOOK)
            .withTriggerRgba(TRIGGER_PINK));
    private final LayerStack layers = new LayerStack();
    private final ScalingViewport viewport;
    private final GlyphSource squareGlyphs;
    private final Vector2 worldPoint = new Vector2();
    private final GridPoint2 pickedCell = new GridPoint2();
    private boolean mouseSeen;
    private Widget content;
    private int pageIndex;
    private int overlayRows = RESERVED_ROWS;

    UiWidgetsScene(GlyphSource squareGlyphs, GlyphSource tallGlyphs) {
        this.squareGlyphs = squareGlyphs;
        surface.addTo(layers, tallGlyphs, squareGlyphs);
        viewport = new ScalingViewport(IntegerScaling.INSTANCE,
                COLUMNS * squareGlyphs.getCellWidth(), ROWS * squareGlyphs.getCellHeight());
        uiRoot.getRoot().addChild(new Backdrop());
        createPageBar();
        showPage(0);
    }

    private void createPageBar() {
        pageBar.setSize(COLUMNS, 0);
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
        content = pages.get(pageIndex).createContent(COLUMNS, ROWS - contentTop);
        content.setPosition(0, contentTop);
        uiRoot.getRoot().addChild(content);
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
        Backdrop() {
            setSize(COLUMNS, ROWS);
        }

        @Override
        public Widget hit(int column, int row) {
            return null;
        }

        @Override
        public int getPreferredWidth() {
            return COLUMNS;
        }

        @Override
        public int getPreferredHeight() {
            return ROWS;
        }

        @Override
        protected void drawContent(UiSurface target, int left, int top) {
            target.fillBackground(left, top, COLUMNS, ROWS, BLACK);
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
        if (uiRoot.keyDown(keycode)) {
            return true;
        }
        if (isAnyMenuOpen()) {
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
        viewport.update(width, height, true);
        int rowsCovered = rowsCoveredByPerfOverlay();
        if (rowsCovered == overlayRows) {
            return;
        }
        overlayRows = rowsCovered;
        moveBelowOverlay();
    }

    private int rowsCoveredByPerfOverlay() {
        int integerScale =
                Math.max(1, Math.round(viewport.getScreenHeight() / viewport.getWorldHeight()));
        int cellHeightInPixels = squareGlyphs.getCellHeight() * integerScale;
        int overlayHeightInPixels = PerfOverlay.heightInScreenPixels(squareGlyphs);
        int rowsCovered = (overlayHeightInPixels + cellHeightInPixels - 1) / cellHeightInPixels;
        return rowsCovered + rowsCovered % 2;
    }

    @Override
    public boolean touchDown(int screenX, int screenY) {
        pickCell(screenX, screenY, pickedCell);
        return uiRoot.pointerDown(pickedCell.x, pickedCell.y, 0, 0);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY) {
        pickCell(screenX, screenY, pickedCell);
        return uiRoot.pointerDragged(pickedCell.x, pickedCell.y, 0);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        mouseSeen = true;
        pickCell(screenX, screenY, pickedCell);
        return uiRoot.pointerMoved(pickedCell.x, pickedCell.y);
    }

    @Override
    public boolean touchUp(int screenX, int screenY) {
        pickCell(screenX, screenY, pickedCell);
        boolean handled = uiRoot.pointerUp(pickedCell.x, pickedCell.y, 0, 0);
        if (!mouseSeen) {
            // Touch-only so far: end the hover once the finger lifts.
            uiRoot.pointerMoved(-1, -1);
        }
        return handled;
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

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        layers.dispose();
    }

    @Override
    public int cellCount() {
        return COLUMNS * ROWS;
    }
}
