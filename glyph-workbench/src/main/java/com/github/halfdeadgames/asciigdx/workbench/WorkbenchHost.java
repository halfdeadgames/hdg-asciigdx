package com.github.halfdeadgames.asciigdx.workbench;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataException;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Hosts one {@link WorkbenchScreen} at a time. With mode {@code review} or {@code paint} that tool
 * opens directly and Esc quits; otherwise the menu opens and Esc in a tool returns to it. A screen
 * switch asked for during a frame happens after that frame, so a screen is never disposed mid-render.
 */
public final class WorkbenchHost extends ApplicationAdapter {

    private static final String TAG = "Workbench";
    private static final String DEFAULT_FONT_SIZE = "8";

    private final String mode;
    private WorkbenchScreen active;
    private Supplier<WorkbenchScreen> pending;

    /** @param mode {@code review}, {@code paint}, or anything else (including null) for the menu */
    public WorkbenchHost(String mode) {
        this.mode = mode;
    }

    @Override
    public void create() {
        if ("review".equals(mode)) {
            switchTo(reviewScreen(Gdx.app::exit));
        } else if ("paint".equals(mode)) {
            switchTo(new PaintApp(Gdx.app::exit, System.getProperty("workbench.fontDir"),
                System.getProperty("workbench.fontSize", DEFAULT_FONT_SIZE)));
        } else {
            switchTo(menuScreen(""));
        }
    }

    private WorkbenchScreen reviewScreen(Runnable onExit) {
        return new WorkbenchApp(onExit);
    }

    private WorkbenchScreen menuScreen(String notice) {
        String fontsRoot = System.getProperty("workbench.fontsRoot");
        MenuModel model = new MenuModel(fontsRoot == null
            ? java.util.List.of()
            : FontDiscovery.discover(Path.of(fontsRoot)));
        return new MenuScreen(model, notice, this::choose, Gdx.app::exit);
    }

    private void choose(MenuModel.Item item) {
        if (item.kind() == MenuModel.Kind.REVIEW) {
            pending = () -> reviewScreen(this::requestMenu);
            return;
        }
        String fontsRoot = System.getProperty("workbench.fontsRoot");
        String fontDir = Path.of(fontsRoot, item.font().folderName()).toString();
        String size = String.valueOf(item.font().size());
        pending = () -> new PaintApp(this::requestMenu, fontDir, size);
    }

    private void requestMenu() {
        pending = () -> menuScreen("");
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

    // A tool that fails to open (for example a font file with an error) sends the user back to the menu with the reason.
    private void applyPending() {
        Supplier<WorkbenchScreen> request = pending;
        pending = null;
        try {
            switchTo(request.get());
        } catch (FontDataException | GdxRuntimeException e) {
            Gdx.app.error(TAG, "Cannot open screen: " + e.getMessage());
            String notice = "CANNOT OPEN: " + e.getMessage();
            switchTo(menuScreen(notice.length() > 100 ? notice.substring(0, 100) : notice));
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
