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
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.UiFont;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import com.github.halfdeadgames.asciigdx.ui.UiSurface;
import com.github.halfdeadgames.asciigdx.ui.Widget;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Pages of UI widget checks, switched with the number keys 1 to 9, under a strip of page names. */
class UiWidgetsScene implements SandboxScene {
    private static final int COLUMNS = 120;
    private static final int ROWS = 67;
    private static final int RESERVED_ROWS = 2;
    private static final int MAX_KEY_PAGES = 9;
    private static final int STRIP_GAP = 3;
    private static final int BLACK = 0x000000FF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GOLD = 0xFFD700FF;
    private static final LabelStyle STRIP_NORMAL = LabelStyle.of(UiFont.SQUARE, WHITE, BLACK);
    private static final LabelStyle STRIP_CURRENT = LabelStyle.of(UiFont.SQUARE, BLACK, GOLD);

    private final List<UiPage> pages = List.of(
            new LabelsPage(),
            new TablePage(),
            new ButtonsPage()
    );
    private final UiSurface surface = UiSurface.ofSize(COLUMNS, ROWS);
    private final UiRoot uiRoot = UiRoot.of(surface);
    private final List<Label> stripEntries = new ArrayList<>();
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
        createStrip();
        showPage(0);
    }

    private void createStrip() {
        int column = 0;
        for (int i = 0; i < pages.size(); i++) {
            String text = (i + 1) + " " + pages.get(i).title();
            Label entry = new Label(text, STRIP_NORMAL);
            entry.setPosition(column, overlayRows);
            uiRoot.getRoot().addChild(entry);
            stripEntries.add(entry);
            column += entry.getWidth() + STRIP_GAP;
        }
    }

    private void showPage(int index) {
        pageIndex = index;
        for (int i = 0; i < stripEntries.size(); i++) {
            stripEntries.get(i).setStyle(i == pageIndex ? STRIP_CURRENT : STRIP_NORMAL);
        }
        if (content != null) {
            uiRoot.getRoot().removeChild(content);
        }
        int contentTop = overlayRows + RESERVED_ROWS;
        content = pages.get(pageIndex).createContent(COLUMNS, ROWS - contentTop);
        content.setPosition(0, contentTop);
        uiRoot.getRoot().addChild(content);
        uiRoot.draw();
    }

    private void moveBelowOverlay() {
        for (int i = 0; i < stripEntries.size(); i++) {
            Label entry = stripEntries.get(i);
            entry.setPosition(entry.getX(), overlayRows);
        }
        content.setPosition(0, overlayRows + RESERVED_ROWS);
        uiRoot.draw();
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
        int index = keycode - Input.Keys.NUM_1;
        if (index < 0 || index >= MAX_KEY_PAGES || index >= pages.size()) {
            return false;
        }
        showPage(index);
        return true;
    }

    @Override
    public List<String> captureVariants() {
        List<String> ids = new ArrayList<>(pages.size());
        for (int i = 0; i < pages.size(); i++) {
            ids.add(pages.get(i).id());
        }
        return ids;
    }

    @Override
    public void showCaptureVariant(String variant) {
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).id().equals(variant)) {
                showPage(i);
                return;
            }
        }
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
