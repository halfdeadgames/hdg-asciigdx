package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.github.halfdeadgames.asciigdx.workbench.FontDiscovery.FontChoice;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataException;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontRules;
import com.github.halfdeadgames.asciigdx.workbench.inputscript.ScriptSession;
import com.github.halfdeadgames.asciigdx.workbench.newitems.NewFontScreen;
import com.github.halfdeadgames.asciigdx.workbench.newitems.NewSizePlan;
import com.github.halfdeadgames.asciigdx.workbench.paint.PaintScreen;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
public final class WorkbenchHost extends ApplicationAdapter {

    private static final String TAG = "Workbench";
    private static final String DEFAULT_FONT_SIZE = "8";

    private final String mode;
    private WorkbenchScreen active;
    private Supplier<WorkbenchScreen> pending;
    private ScriptSession scriptSession;

    /** @param mode {@code paint} or anything else (including null) for the menus */
    public WorkbenchHost(String mode) {
        this.mode = mode;
    }

    @Override
    public void create() {
        String scriptPath = System.getProperty(ScriptSession.SCRIPT_PROPERTY);
        if (scriptPath != null) {
            scriptSession = new ScriptSession(scriptPath);
        }
        if ("paint".equals(mode)) {
            switchTo(paintScreen(Gdx.app::exit, System.getProperty("workbench.fontDir"),
                System.getProperty("workbench.fontSize", DEFAULT_FONT_SIZE)));
        } else if (DevCapture.newFontRequest() != null || DevCapture.newFontSizeRequest() != null) {
            switchTo(newFontScreen());
        } else {
            switchTo(fontListScreen(""));
        }
    }

    private String fontsRoot() {
        return System.getProperty("workbench.fontsRoot");
    }

    // Read on every menu open, so size files made since the last visit show up.
    private List<FontChoice> fonts() {
        String root = fontsRoot();
        return root == null ? List.of() : FontDiscovery.discover(Path.of(root));
    }

    // First paragraph of the font's rules.md, or empty. Cached per folder by the caller.
    private String mottoOf(String folderName) {
        Path file = Path.of(fontsRoot(), folderName, FontRules.FILE_NAME);
        try {
            return Files.exists(file) ? FontRules.parse(Files.readString(file, StandardCharsets.UTF_8)).motto() : "";
        } catch (IOException | RuntimeException e) {
            return "";
        }
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
                return new MenuScreen.Detail(folder.toUpperCase(Locale.ROOT), mottoOf(folder), actions);
            });
        MenuScreen.Text text = MenuScreen.Text.opening("FONTS", List.of("GLYPH WORKBENCH"), "QUIT", notice);
        return new MenuScreen(MenuModel.fontList(all), text, detail, this::chooseFont, Gdx.app::exit);
    }

    private static MenuScreen.Detail newFontDetail() {
        return new MenuScreen.Detail("NEW FONT", "", List.of("MAKES A FONT FOLDER", "WITH ITS FIRST SIZE FILE"));
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
            (folder, height) -> pending = () -> paintCreatedFont(folder, height));
    }

    // Esc from the new font's first paint screen goes to that font's own menu, then to the (refreshed) font list.
    private WorkbenchScreen paintCreatedFont(Path folder, int height) {
        String folderName = folder.getFileName().toString();
        Runnable back = fontsRoot() == null ? Gdx.app::exit : () -> pending = () -> fontMenuScreen(folderName);
        return paintScreen(back, folder.toString(), String.valueOf(height));
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
        String motto = mottoOf(folderName);
        Function<MenuModel.Item, MenuScreen.Detail> detail = item -> new MenuScreen.Detail(name, motto, List.of());
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
            pending = () -> newSizeScreen(folderName);
        } else {
            pending = () -> paintScreen(backToFontMenu, fontDir, String.valueOf(item.size()));
        }
    }

    // A paint screen that can switch to a size file or a font it creates; Esc there goes back as given.
    private PaintScreen paintScreen(Runnable back, String fontDir, String size) {
        return new PaintScreen(back, fontDir, size,
            height -> pending = () -> paintScreen(back, fontDir, String.valueOf(height)),
            (folder, height) -> pending = () -> paintCreatedFont(folder, height));
    }

    // The size list is a menu of its own; choosing a free size creates the file and paints it.
    private WorkbenchScreen newSizeScreen(String folderName) {
        Path folder = Path.of(fontsRoot(), folderName);
        List<NewSizePlan.SizeOption> options;
        try {
            options = NewSizePlan.options(folder);
        } catch (IOException | IllegalStateException e) {
            return fontMenuScreen(folderName, limited("CANNOT LIST SIZES: " + e.getMessage()), MenuModel.Kind.NEW_SIZE);
        }
        String name = folderName.toUpperCase(Locale.ROOT);
        Runnable backToNewSize = () -> pending = () -> fontMenuScreen(folderName, "", MenuModel.Kind.NEW_SIZE);
        Function<MenuModel.Item, MenuScreen.Detail> detail = item -> item == null ? null
            : new MenuScreen.Detail(name, "", List.of("CREATES " + item.size() + ".TXT"));
        MenuScreen.Text text = new MenuScreen.Text("NEW SIZE", List.of("GLYPH WORKBENCH", name, "NEW SIZE"),
            "CREATE", "BACK", "");
        return new MenuScreen(MenuModel.newSizes(folderName, options), text, detail,
            item -> createSize(folder, item.size()), backToNewSize);
    }

    private void createSize(Path folder, int height) {
        String folderName = folder.getFileName().toString();
        try {
            NewSizePlan.create(folder, height);
        } catch (IOException | RuntimeException e) {
            Gdx.app.error(TAG, "Cannot create size " + height + ": " + e.getMessage());
            pending = () -> fontMenuScreen(folderName, limited("CANNOT CREATE " + height + ": " + e.getMessage()),
                MenuModel.Kind.NEW_SIZE);
            return;
        }
        Runnable backToFontMenu = () -> pending = () -> fontMenuScreen(folderName);
        pending = () -> paintScreen(backToFontMenu, folder.toString(), String.valueOf(height));
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
