package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.github.halfdeadgames.asciigdx.fit.IntegerScaling;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.layer.LayerStack;
import com.github.halfdeadgames.asciigdx.ui.Label;
import com.github.halfdeadgames.asciigdx.ui.LabelStyle;
import com.github.halfdeadgames.asciigdx.ui.UiFont;
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
            new TablePage()
    );
    private final UiSurface surface = UiSurface.ofSize(COLUMNS, ROWS);
    private final LayerStack layers = new LayerStack();
    private final ScalingViewport viewport;
    private final GlyphSource squareGlyphs;
    private int pageIndex;
    private int overlayRows = RESERVED_ROWS;

    UiWidgetsScene(GlyphSource squareGlyphs, GlyphSource tallGlyphs) {
        this.squareGlyphs = squareGlyphs;
        surface.addTo(layers, tallGlyphs, squareGlyphs);
        viewport = new ScalingViewport(IntegerScaling.INSTANCE,
                COLUMNS * squareGlyphs.getCellWidth(), ROWS * squareGlyphs.getCellHeight());
        redrawSurface();
    }

    private void redrawSurface() {
        surface.eraseGlyphs(0, 0, COLUMNS, ROWS);
        surface.fillBackground(0, 0, COLUMNS, ROWS, BLACK);
        drawStrip();
        int contentTop = overlayRows + RESERVED_ROWS;
        Widget content = pages.get(pageIndex).createContent(COLUMNS, ROWS - contentTop);
        content.setPosition(0, contentTop);
        content.validate();
        content.draw(surface);
    }

    private void drawStrip() {
        int column = 0;
        for (int i = 0; i < pages.size(); i++) {
            String text = (i + 1) + " " + pages.get(i).title();
            Label entry = new Label(text, i == pageIndex ? STRIP_CURRENT : STRIP_NORMAL);
            entry.setPosition(column, overlayRows);
            entry.draw(surface);
            column += entry.getWidth() + STRIP_GAP;
        }
    }

    private void showPage(int index) {
        pageIndex = index;
        redrawSurface();
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
        redrawSurface();
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
    public void render(SpriteBatch batch, float delta) {
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
