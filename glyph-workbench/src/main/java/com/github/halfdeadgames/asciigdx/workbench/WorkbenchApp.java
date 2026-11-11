package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.github.halfdeadgames.asciigdx.workbench.FontDiscovery.FontChoice;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataException;
import com.github.halfdeadgames.asciigdx.workbench.inputscript.ScriptSession;
import com.github.halfdeadgames.asciigdx.workbench.newitems.FirstGlyphPlan;
import com.github.halfdeadgames.asciigdx.workbench.newitems.FirstGlyphScreen;
import com.github.halfdeadgames.asciigdx.workbench.newitems.NewFontScreen;
import com.github.halfdeadgames.asciigdx.workbench.newitems.NewSizePlan;
import com.github.halfdeadgames.asciigdx.workbench.newitems.NewSizeScreen;
import com.github.halfdeadgames.asciigdx.workbench.paint.PaintScreen;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Hosts one {@link WorkbenchScreen} at a time. The flow is font list, then the actions of one font,
 * then a tool; Esc goes back one step and quits from the font list. With mode {@code paint} (a dev
 * shortcut for captures) the paint screen opens directly and Esc quits. A screen switch asked for
 * during a frame happens after that frame, so a screen is never disposed mid-render.
 */
final class WorkbenchApp extends ApplicationAdapter {

    private static final String TAG = "Workbench";
    private static final CellSize DEFAULT_FONT_SIZE = new CellSize(16, 16);

    private final String mode;
    private WorkbenchScreen active;
    private Supplier<WorkbenchScreen> pending;
    private ScriptSession scriptSession;

    /** @param mode {@code paint} or anything else (including null) for the menus */
    WorkbenchApp(String mode) {
        this.mode = mode;
    }

    @Override
    public void create() {
        String scriptPath = System.getProperty(ScriptSession.SCRIPT_PROPERTY);
        if (scriptPath != null) {
            scriptSession = new ScriptSession(scriptPath);
        }
        if ("paint".equals(mode)) {
            switchTo(paintScreen(Gdx.app::exit, System.getProperty("workbench.fontDir"), launchFontSize()));
        } else if (DevCapture.newFontRequest() != null || DevCapture.newFontSizeRequest() != null) {
            switchTo(newFontScreen());
        } else if (DevCapture.newSizeRequest() >= 0) {
            switchTo(newSizeScreen(Path.of(System.getProperty("workbench.fontDir"))));
        } else {
            switchTo(fontListScreen(""));
        }
    }

    // The property is always width x height, like 16x16.
    private static CellSize launchFontSize() {
        String text = System.getProperty("workbench.fontSize");
        return text == null ? DEFAULT_FONT_SIZE : CellSize.parse(text.trim());
    }

    private String fontsRoot() {
        return System.getProperty("workbench.fontsRoot");
    }

    // Read on every menu open, so size files made since the last visit show up.
    private List<FontChoice> fonts() {
        String root = fontsRoot();
        return root == null ? List.of() : FontDiscovery.discover(Path.of(root));
    }

    private WorkbenchScreen fontListScreen(String notice) {
        List<FontChoice> all = fonts();
        Map<String, MenuScreen.Detail> cache = new HashMap<>();
        Function<MenuModel.Item, MenuScreen.Detail> detail = item -> item == null ? null
            : item.kind() == MenuModel.Kind.NEW_FONT ? newFontDetail()
            : cache.computeIfAbsent(item.folderName(), folder -> {
                List<String> actions = new ArrayList<>();
                for (MenuModel.Line line : MenuModel.fontActions(folder, all).lines()) {
                    actions.add(line.text().strip());
                }
                return new MenuScreen.Detail(folder.toUpperCase(Locale.ROOT), actions);
            });
        MenuScreen.Text text = MenuScreen.Text.opening("FONTS", List.of("GLYPH WORKBENCH"), "QUIT", notice);
        return new MenuScreen(MenuModel.fontList(all), text, detail, this::chooseFont, Gdx.app::exit);
    }

    private static MenuScreen.Detail newFontDetail() {
        return new MenuScreen.Detail("NEW FONT", List.of("MAKES A FONT FOLDER", "WITH ITS FIRST SIZE FILE"));
    }

    private void chooseFont(MenuModel.Item item) {
        if (item.kind() == MenuModel.Kind.NEW_FONT) {
            pending = this::newFontScreen;
        } else {
            pending = () -> fontMenuScreen(item.folderName());
        }
    }

    // Esc on the name page returns to the font list, which is read again so it shows the font just made.
    private WorkbenchScreen newFontScreen() {
        String root = fontsRoot();
        return new NewFontScreen(root == null ? null : Path.of(root), () -> pending = () -> fontListScreen(""),
            (folder, size) -> pending = () -> paintCreatedFont(folder, size));
    }

    // Esc from the new font's first paint screen goes to that font's own menu, then to the (refreshed) font list.
    private WorkbenchScreen paintCreatedFont(Path folder, CellSize size) {
        String folderName = folder.getFileName().toString();
        Runnable back = fontsRoot() == null ? Gdx.app::exit : () -> pending = () -> fontMenuScreen(folderName);
        return paintScreen(back, folder.toString(), size);
    }

    private WorkbenchScreen fontMenuScreen(String folderName) {
        return fontMenuScreen(folderName, "");
    }

    private WorkbenchScreen fontMenuScreen(String folderName, String notice) {
        return fontMenuScreen(folderName, notice, null);
    }

    // The cursor starts on the first item of selectKind, or on the first item when that is null or absent.
    private WorkbenchScreen fontMenuScreen(String folderName, String notice, MenuModel.Kind selectKind) {
        String name = folderName.toUpperCase(Locale.ROOT);
        Function<MenuModel.Item, MenuScreen.Detail> detail = item -> new MenuScreen.Detail(name, List.of());
        MenuModel actions = MenuModel.fontActions(folderName, fonts());
        if (selectKind != null) {
            actions.select(actions.indexOfKind(selectKind));
        }
        MenuScreen.Text text = MenuScreen.Text.opening("ACTIONS", List.of("GLYPH WORKBENCH", name), "BACK", notice);
        return new MenuScreen(actions, text, detail, item -> chooseAction(folderName, item),
            () -> pending = () -> fontListScreen(""));
    }

    private void chooseAction(String folderName, MenuModel.Item item) {
        String fontDir = Path.of(fontsRoot(), folderName).toString();
        Runnable backToFontMenu = () -> pending = () -> fontMenuScreen(folderName);
        if (item.kind() == MenuModel.Kind.NEW_SIZE) {
            pending = () -> newSizeScreen(Path.of(fontsRoot(), folderName));
        } else {
            pending = () -> paintScreen(backToFontMenu, fontDir, item.size());
        }
    }

    // Returns the paint screen, or the first-glyph page for a font with no glyph slots. That page opens the paint
    // screen once a glyph is listed. The paint screen can switch to a size file or a font it creates; Esc goes back
    // as given.
    private WorkbenchScreen paintScreen(Runnable back, String fontDir, CellSize size) {
        if (FirstGlyphPlan.isNeeded(Path.of(fontDir), size)) {
            return new FirstGlyphScreen(Path.of(fontDir), size, back,
                () -> pending = () -> paintScreen(back, fontDir, size));
        }
        return new PaintScreen(back, fontDir, size,
            newSize -> pending = () -> paintScreen(back, fontDir, newSize),
            (folder, firstSize) -> pending = () -> paintCreatedFont(folder, firstSize));
    }

    // Choosing a size creates the file and paints it; closing the dialog or failing to list sizes returns to the font menu.
    private WorkbenchScreen newSizeScreen(Path folder) {
        String folderName = folder.getFileName().toString();
        return new NewSizeScreen(folder,
            () -> pending = () -> fontMenuScreen(folderName, "", MenuModel.Kind.NEW_SIZE),
            size -> createSize(folder, size),
            reason -> pending = () -> fontMenuScreen(folderName, limited("CANNOT LIST SIZES: " + reason),
                MenuModel.Kind.NEW_SIZE));
    }

    private void createSize(Path folder, CellSize size) {
        String folderName = folder.getFileName().toString();
        try {
            NewSizePlan.create(folder, size);
        } catch (IOException | RuntimeException e) {
            Gdx.app.error(TAG, "Cannot create size " + size + ": " + e.getMessage());
            pending = () -> fontMenuScreen(folderName, limited("CANNOT CREATE " + size + ": " + e.getMessage()),
                MenuModel.Kind.NEW_SIZE);
            return;
        }
        Runnable backToFontMenu = () -> pending = () -> fontMenuScreen(folderName);
        pending = () -> paintScreen(backToFontMenu, folder.toString(), size);
    }

    private static String limited(String notice) {
        return notice.length() > 100 ? notice.substring(0, 100) : notice;
    }

    private void switchTo(WorkbenchScreen next) {
        WorkbenchScreen previous = active;
        active = null;
        if (previous != null) {
            previous.hide();
        }
        next.show();
        active = next;
        next.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    // A tool that fails to open (for example a font file with an error) sends the user to the font list with the reason.
    private void applyPending() {
        Supplier<WorkbenchScreen> request = pending;
        pending = null;
        try {
            switchTo(request.get());
        } catch (FontDataException | GdxRuntimeException e) {
            Gdx.app.error(TAG, "Cannot open screen: " + e.getMessage());
            switchTo(fontListScreen(limited("CANNOT OPEN: " + e.getMessage())));
        }
    }

    @Override
    public void resize(int width, int height) {
        if (active != null && width > 0 && height > 0) {
            active.resize(width, height);
        }
    }

    // A minimized window reports a 0x0 size, and the backend still calls render from its resize callback.
    @Override
    public void render() {
        if (Gdx.graphics.getWidth() <= 0 || Gdx.graphics.getHeight() <= 0) {
            return;
        }
        if (scriptSession != null) {
            scriptSession.beforeFrame(Gdx.graphics.getDeltaTime());
        }
        active.render();
        if (scriptSession != null) {
            scriptSession.afterFrame();
        }
        if (pending != null) {
            applyPending();
        }
    }

    @Override
    public void dispose() {
        if (active != null) {
            active.hide();
            active = null;
        }
    }
}
