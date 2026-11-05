package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource;
import com.github.halfdeadgames.asciigdx.workbench.ui.CellRect;
import com.github.halfdeadgames.asciigdx.workbench.ui.Panel;
import com.github.halfdeadgames.asciigdx.workbench.ui.ScrollText;
import com.github.halfdeadgames.asciigdx.workbench.ui.SelectList;
import com.github.halfdeadgames.asciigdx.workbench.ui.StatusBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.TopBar;
import com.github.halfdeadgames.asciigdx.workbench.ui.TopBarStatus;
import com.github.halfdeadgames.asciigdx.workbench.ui.UiPalette;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** A menu in the UI kit style: a focused list on the left, details of the selected entry on the right. */
final class MenuScreen implements WorkbenchScreen {

    /** The right panel: its title, a short description in dim text, and extra lines such as the actions. */
    record Detail(String title, String motto, List<String> lines) {
    }

    /** The words on screen: panel title, breadcrumb, one-word Enter and Esc labels, and a notice (empty for none). */
    record Text(String listTitle, List<String> crumbs, String enterLabel, String escapeLabel, String notice) {

        private static final String DEFAULT_ENTER_LABEL = "OPEN";

        /** Text whose Enter label is {@code OPEN}. */
        static Text opening(String listTitle, List<String> crumbs, String escapeLabel, String notice) {
            return new Text(listTitle, crumbs, DEFAULT_ENTER_LABEL, escapeLabel, notice);
        }
    }

    private static final int CELL = Painter.TEXT_SIZE;
    private static final int PANEL_TOP = 3;
    private static final int LIST_X = 2;
    private static final int LIST_WIDTH = 30;
    private static final int DETAIL_X = 34;
    private static final int DETAIL_MAX_WIDTH = 42;
    private static final int LIST_TOP = PANEL_TOP + 2;
    private static final int MIN_PANEL_HEIGHT = 14;

    private final MenuModel model;
    private final String listTitle;
    private final List<String> crumbs;
    private final List<StatusBar.Key> keys;
    private final String notice;
    private final Function<MenuModel.Item, Detail> detailFor;
    private final Consumer<MenuModel.Item> onChoose;
    private final Runnable onBack;
    private final ScreenViewport viewport = new ScreenViewport();
    private final Vector2 pointer = new Vector2();
    private final DevCapture devCapture = new DevCapture();

    private SpriteBatch batch;
    private Texture whitePixel;
    private TextureGlyphSource source;
    private Painter painter;
    private int lastMouseX = -1;
    private int lastMouseY = -1;

    /**
     * @param text the words on screen
     * @param detailFor the right panel for the selected item (null when the menu has no items)
     * @param onChoose called with the chosen item
     * @param onBack called when the user presses Esc
     */
    MenuScreen(MenuModel model, Text text, Function<MenuModel.Item, Detail> detailFor,
        Consumer<MenuModel.Item> onChoose, Runnable onBack) {
        this.model = model;
        this.listTitle = text.listTitle();
        this.crumbs = text.crumbs();
        this.keys = List.of(
            new StatusBar.Key("↑↓", "CHOOSE"), new StatusBar.Key("ENTER", text.enterLabel()),
            new StatusBar.Key("1-9", "QUICK PICK"), new StatusBar.Key("ESC", text.escapeLabel()));
        this.notice = text.notice();
        this.detailFor = detailFor;
        this.onChoose = onChoose;
        this.onBack = onBack;
    }

    @Override
    public void show() {
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        whitePixel = new Texture(pixel);
        pixel.dispose();
        source = TextureGlyphSource.loadDungeonMode();
        batch = new SpriteBatch();
        painter = new Painter(batch, whitePixel, source, viewport);
        Gdx.input.setInputProcessor(new InputAdapter());
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

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            onBack.run();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            model.move(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            model.move(1);
        }
        if ((Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER))
            && model.selectedItem() != null) {
            onChoose.accept(model.selectedItem());
            return;
        }
        for (int number = 1; number <= 9; number++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0 + number)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0 + number)) {
                int index = model.itemForNumber(number);
                if (index >= 0) {
                    model.select(index);
                    onChoose.accept(model.item(index));
                    return;
                }
            }
        }
        handleMouse();
    }

    private void handleMouse() {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.input.getY();
        boolean moved = mouseX != lastMouseX || mouseY != lastMouseY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        boolean clicked = Gdx.input.justTouched() && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        if (!moved && !clicked) {
            return;
        }
        pointer.set(mouseX, mouseY);
        viewport.unproject(pointer);
        int cellX = Math.floorDiv((int) Math.floor(pointer.x), CELL);
        int cellY = Math.floorDiv((int) Math.floor(viewport.getWorldHeight() - pointer.y), CELL);
        if (cellX < LIST_X || cellX >= LIST_X + LIST_WIDTH) {
            return;
        }
        int row = SelectList.rowAt(cellY, LIST_TOP, model.lines().size());
        if (row < 0) {
            return;
        }
        int index = model.lines().get(row).itemIndex();
        if (index < 0) {
            return;
        }
        model.select(index);
        if (clicked) {
            onChoose.accept(model.item(index));
        }
    }

    private List<SelectList.Row> rows() {
        List<SelectList.Row> rows = new ArrayList<>();
        for (MenuModel.Line line : model.lines()) {
            rows.add(new SelectList.Row(line.text().strip(), line.itemIndex() >= 0, line.dimmed(), line.indent()));
        }
        return rows;
    }

    private int selectedRow() {
        List<MenuModel.Line> lines = model.lines();
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).itemIndex() >= 0 && lines.get(i).itemIndex() == model.selectedIndex()) {
                return i;
            }
        }
        return -1;
    }

    private void draw() {
        ScreenUtils.clear(UiPalette.BG);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        int columns = Math.round(viewport.getWorldWidth()) / CELL;
        int rowCount = Math.round(viewport.getWorldHeight()) / CELL;
        TopBar.draw(painter, columns, 0, crumbs, TopBarStatus.of("", UiPalette.TEXT));

        int panelHeight = Math.max(MIN_PANEL_HEIGHT, Math.min(rowCount - PANEL_TOP - 3, model.lines().size() * SelectList.PITCH + 5));
        Panel.draw(painter, new CellRect(LIST_X, PANEL_TOP, LIST_WIDTH, panelHeight), listTitle, true);
        List<SelectList.Row> listRows = rows();
        SelectList.draw(painter, SelectList.areaOf(LIST_X + 1, LIST_TOP, LIST_WIDTH - 2, listRows.size()),
            listRows, selectedRow());

        Detail detail = detailFor.apply(model.selectedItem());
        int detailWidth = Math.max(10, Math.min(DETAIL_MAX_WIDTH, columns - DETAIL_X - 2));
        if (detail != null) {
            drawDetail(detail, detailWidth, panelHeight);
        }
        if (!notice.isEmpty()) {
            painter.text(notice, LIST_X * CELL, (rowCount - 3) * CELL, UiPalette.WARN);
        }
        StatusBar.draw(painter, columns, rowCount - 1, keys);
        batch.end();
    }

    private void drawDetail(Detail detail, int width, int height) {
        Panel.draw(painter, new CellRect(DETAIL_X, PANEL_TOP, width, height), detail.title(), false);
        int y = LIST_TOP;
        int bottom = PANEL_TOP + height - 1;
        if (!detail.motto().isEmpty()) {
            for (String line : ScrollText.wrap(detail.motto(), width - 4)) {
                if (y >= bottom) {
                    break;
                }
                painter.text(line, (DETAIL_X + 2) * CELL, y * CELL, UiPalette.DIM);
                y++;
            }
            y++;
        }
        for (String line : detail.lines()) {
            if (y >= bottom) {
                break;
            }
            painter.text(line.length() > width - 4 ? line.substring(0, width - 4) : line,
                (DETAIL_X + 2) * CELL, y * CELL, UiPalette.TEXT);
            y += SelectList.PITCH;
        }
    }

    @Override
    public void hide() {
        batch.dispose();
        whitePixel.dispose();
        source.dispose();
    }
}
