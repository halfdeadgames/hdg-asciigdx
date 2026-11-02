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
import java.util.List;
import java.util.function.Consumer;

/** The launch menu: pick the review tool or a font to paint. */
final class MenuScreen implements WorkbenchScreen {

    private static final int MARGIN = 12;
    private static final int TEXT_LINE = Painter.TEXT_SIZE;
    private static final int LINE_PITCH = TEXT_LINE + 12;
    private static final int INDENT_CELLS = 2;
    private static final int TOP_LINES = 3;
    private static final String TITLE = "GLYPH WORKBENCH";
    private static final String HINT = "ARROWS AND ENTER, NUMBER KEYS, OR CLICK.  ESC QUITS";

    private static final Color BACKGROUND = new Color(0.06f, 0.06f, 0.08f, 1f);
    private static final Color TEXT_COLOR = new Color(0.9f, 0.9f, 0.9f, 1f);
    private static final Color TITLE_COLOR = new Color(0.8f, 0.6f, 1f, 1f);
    private static final Color SELECTED_COLOR = Color.YELLOW;
    private static final Color SELECTED_FILL = new Color(0.2f, 0.22f, 0.32f, 1f);
    private static final Color WARNING_COLOR = new Color(1f, 0.35f, 0.3f, 1f);

    private final MenuModel model;
    private final String notice;
    private final Consumer<MenuModel.Item> onChoose;
    private final Runnable onQuit;
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
     * @param notice shown under the menu in a warning colour, or empty
     * @param onChoose called with the chosen item
     * @param onQuit called when the user presses Esc
     */
    MenuScreen(MenuModel model, String notice, Consumer<MenuModel.Item> onChoose, Runnable onQuit) {
        this.model = model;
        this.notice = notice;
        this.onChoose = onChoose;
        this.onQuit = onQuit;
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

    private int lineTop(int lineIndex) {
        return MARGIN + TOP_LINES * TEXT_LINE + lineIndex * LINE_PITCH;
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            onQuit.run();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            model.move(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            model.move(1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
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
        int pointerY = (int) Math.floor(viewport.getWorldHeight() - pointer.y);
        int pointerX = (int) Math.floor(pointer.x);
        int index = itemAt(pointerX, pointerY);
        if (index < 0) {
            return;
        }
        model.select(index);
        if (clicked) {
            onChoose.accept(model.item(index));
        }
    }

    // A line is hit across its whole row, from the left margin to the right edge.
    private int itemAt(int pixelX, int pixelY) {
        if (pixelX < MARGIN) {
            return -1;
        }
        List<MenuModel.Line> lines = model.lines();
        for (int i = 0; i < lines.size(); i++) {
            int top = lineTop(i) - (LINE_PITCH - TEXT_LINE) / 2;
            if (pixelY >= top && pixelY < top + LINE_PITCH) {
                return lines.get(i).itemIndex();
            }
        }
        return -1;
    }

    private void draw() {
        ScreenUtils.clear(BACKGROUND);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        painter.text(TITLE, MARGIN, MARGIN, TITLE_COLOR);
        List<MenuModel.Line> lines = model.lines();
        for (int i = 0; i < lines.size(); i++) {
            MenuModel.Line line = lines.get(i);
            int x = MARGIN + line.indent() * INDENT_CELLS * TEXT_LINE;
            boolean isSelected = line.itemIndex() >= 0 && line.itemIndex() == model.selectedIndex();
            if (isSelected) {
                int width = (line.text().length() + 1) * TEXT_LINE;
                painter.fill(SELECTED_FILL, x - MARGIN / 2, lineTop(i) - (LINE_PITCH - TEXT_LINE) / 2, width, LINE_PITCH);
            }
            Color color = isSelected ? SELECTED_COLOR : line.itemIndex() >= 0 ? TEXT_COLOR : TITLE_COLOR;
            painter.text(line.text(), x, lineTop(i), color);
        }
        int bottom = Math.round(viewport.getWorldHeight()) - MARGIN - TEXT_LINE;
        if (!notice.isEmpty()) {
            painter.text(notice, MARGIN, bottom - TEXT_LINE, WARNING_COLOR);
        }
        painter.text(HINT, MARGIN, bottom, TEXT_COLOR);
        batch.end();
    }

    @Override
    public void hide() {
        batch.dispose();
        whitePixel.dispose();
        source.dispose();
    }
}
