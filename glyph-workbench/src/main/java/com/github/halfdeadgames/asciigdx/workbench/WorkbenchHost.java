package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.github.halfdeadgames.asciigdx.workbench.FontDiscovery.FontChoice;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataException;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontRules;
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
import java.util.stream.Collectors;

/**
 * Hosts one {@link WorkbenchScreen} at a time. The flow is font list, then the actions of one font,
 * then a tool; Esc goes back one step and quits from the font list. With mode {@code paint} or
 * {@code upscale} (dev shortcuts for captures) that tool opens directly and Esc quits. A screen
 * switch asked for during a frame happens after that frame, so a screen is never disposed mid-render.
 */
public final class WorkbenchHost extends ApplicationAdapter {

    private static final String TAG = "Workbench";
    private static final String DEFAULT_FONT_SIZE = "8";

    private final String mode;
    private WorkbenchScreen active;
    private Supplier<WorkbenchScreen> pending;

    /** @param mode {@code paint}, {@code upscale}, or anything else (including null) for the menus */
    public WorkbenchHost(String mode) {
        this.mode = mode;
    }

    @Override
    public void create() {
        if ("paint".equals(mode)) {
            switchTo(new PaintApp(Gdx.app::exit, System.getProperty("workbench.fontDir"),
                System.getProperty("workbench.fontSize", DEFAULT_FONT_SIZE)));
        } else if ("upscale".equals(mode)) {
            switchTo(directUpscaleScreen());
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
            : cache.computeIfAbsent(item.folderName(), folder -> {
                List<String> actions = new ArrayList<>();
                for (MenuModel.Line line : MenuModel.fontActions(folder, all).lines()) {
                    actions.add(line.text().strip());
                }
                return new MenuScreen.Detail(folder.toUpperCase(Locale.ROOT), mottoOf(folder), actions);
            });
        return new MenuScreen(MenuModel.fontList(all), "FONTS", List.of("GLYPH WORKBENCH"), "QUIT", notice,
            detail, item -> pending = () -> fontMenuScreen(item.folderName()), Gdx.app::exit);
    }

    private WorkbenchScreen fontMenuScreen(String folderName) {
        String name = folderName.toUpperCase(Locale.ROOT);
        String motto = mottoOf(folderName);
        Function<MenuModel.Item, MenuScreen.Detail> detail = item -> {
            List<String> lines = new ArrayList<>();
            if (item != null && item.upscale() != null) {
                lines.add(item.upscale().target() + " FROM " + item.upscale().sources().stream()
                    .map(size -> size + " AT " + item.upscale().factorFrom(size) + "X")
                    .collect(Collectors.joining(", ")));
            }
            return new MenuScreen.Detail(name, motto, lines);
        };
        return new MenuScreen(MenuModel.fontActions(folderName, fonts()), "ACTIONS",
            List.of("GLYPH WORKBENCH", name), "BACK", "",
            detail, item -> chooseAction(folderName, item), () -> pending = () -> fontListScreen(""));
    }

    private void chooseAction(String folderName, MenuModel.Item item) {
        String fontDir = Path.of(fontsRoot(), folderName).toString();
        Runnable backToFontMenu = () -> pending = () -> fontMenuScreen(folderName);
        if (item.kind() == MenuModel.Kind.UPSCALE) {
            pending = () -> new UpscaleReviewScreen(backToFontMenu, fontDir, item.upscale());
        } else {
            pending = () -> new PaintApp(backToFontMenu, fontDir, String.valueOf(item.size()));
        }
    }

    // Dev shortcut for captures: -Pmode=upscale reviews the next upscale of the -PfontDir font.
    private WorkbenchScreen directUpscaleScreen() {
        String fontDir = System.getProperty("workbench.fontDir");
        if (fontDir == null) {
            throw new GdxRuntimeException("workbench.fontDir is not set");
        }
        Path folder = Path.of(fontDir);
        for (UpscalePlan.Choice choice : UpscalePlan.plan(FontDiscovery.discover(folder.getParent()))) {
            if (choice.folderName().equals(folder.getFileName().toString()) && choice.hasSource()) {
                return new UpscaleReviewScreen(Gdx.app::exit, fontDir, choice);
            }
        }
        throw new GdxRuntimeException("No even upscale source for " + fontDir);
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
            String notice = "CANNOT OPEN: " + e.getMessage();
            switchTo(fontListScreen(notice.length() > 100 ? notice.substring(0, 100) : notice));
        }
    }

    @Override
    public void resize(int width, int height) {
        if (active != null) {
            active.resize(width, height);
        }
    }

    @Override
    public void render() {
        active.render();
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
