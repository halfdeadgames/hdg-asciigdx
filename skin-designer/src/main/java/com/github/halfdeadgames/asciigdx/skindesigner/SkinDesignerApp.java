package com.github.halfdeadgames.asciigdx.skindesigner;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.glyph.SaiksisttSize;
import com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource;
import java.util.function.Function;

/** Desktop app for tuning the skin file: the widget showcase plus an optional capture run. */
public class SkinDesignerApp extends ApplicationAdapter {
    private static final long CAPTURE_RANDOM_SEED = 1234L;
    private static final String SCREENSHOT_FOLDER = "captures";
    private static final String SCRIPT_KEY = "script";

    private final WindowFitter windowFitter = new WindowFitter();
    private final Function<String, String> settings;
    private SpriteBatch batch;
    private GlyphSource squareGlyphs;
    private GlyphSource textGlyphs;
    private UiShowcaseScene showcase;
    private PerfOverlay perfOverlay;
    private ScreenshotWriter screenshots;
    private CaptureRun captureRun;

    /**
     * @param settings key to value lookup returning null when missing; see
     *        {@link CaptureSettings}
     */
    public SkinDesignerApp(Function<String, String> settings) {
        this.settings = settings;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        squareGlyphs = TextureGlyphSource.loadSaiksistt();
        textGlyphs = TextureGlyphSource.loadSaiksistt(SaiksisttSize.TALL_8X16);
        showcase = new UiShowcaseScene(squareGlyphs, textGlyphs);
        perfOverlay = new PerfOverlay(squareGlyphs);
        perfOverlay.startMeasuring(showcase.cellCount());
        perfOverlay.setStatus(showcase.statusText());
        createScreenshotsAndCaptureRun();
        Gdx.input.setInputProcessor(new ShowcaseInput());
        fitWindowUnlessScripted();
    }

    private void createScreenshotsAndCaptureRun() {
        CaptureSettings captureSettings = CaptureSettings.fromLookup(settings);
        if (captureSettings == null) {
            screenshots = new ScreenshotWriter(Gdx.files.local(SCREENSHOT_FOLDER));
            return;
        }
        MathUtils.random.setSeed(CAPTURE_RANDOM_SEED);
        screenshots = new ScreenshotWriter(Gdx.files.absolute(captureSettings.outputDirectory()));
        captureRun = new CaptureRun(showcase, screenshots, captureSettings, this::refreshStatus,
                windowFitter::isSettled);
    }

    // A script keeps the requested size: its coordinates are window pixels, and a desktop resize
    // renders a frame inside the resize call, which would advance the script mid-step.
    private void fitWindowUnlessScripted() {
        if (settings.apply(SCRIPT_KEY) != null) {
            return;
        }
        windowFitter.resizeTo(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void refreshStatus() {
        perfOverlay.setStatus(showcase.statusText());
    }

    @Override
    public void resize(int width, int height) {
        showcase.resize(width, height);
        perfOverlay.resize(width, height);
        perfOverlay.setCellCount(showcase.cellCount());
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        long renderStartNanos = System.nanoTime();
        showcase.render(batch, delta);
        long submittedNanos = System.nanoTime();
        long renderNanos = 0L;
        long gpuWaitNanos = 0L;
        if (captureRun != null) {
            Gdx.gl.glFinish();
            long finishedNanos = System.nanoTime();
            renderNanos = finishedNanos - renderStartNanos;
            gpuWaitNanos = finishedNanos - submittedNanos;
        }
        perfOverlay.update(delta);
        perfOverlay.render(batch);
        screenshots.onFrameRendered();
        if (captureRun != null) {
            captureRun.onFrameRendered(renderNanos, gpuWaitNanos);
        }
    }

    /**
     * Decides whether the window may close now. With unsaved changes it asks the user instead,
     * and quits only after Save or Discard.
     *
     * @return {@code true} when the window may close at once
     */
    public boolean isWindowCloseAllowed() {
        if (showcase == null) {
            return true;
        }
        showcase.commitPanelEdits();
        if (!showcase.hasUnsavedChanges()) {
            return true;
        }
        showcase.requestQuit(Gdx.app::exit);
        return false;
    }

    @Override
    public void dispose() {
        showcase.dispose();
        perfOverlay.dispose();
        batch.dispose();
        squareGlyphs.dispose();
        textGlyphs.dispose();
    }

    /** Forwards input to the showcase and keeps the overlay's status line current. */
    private final class ShowcaseInput extends InputAdapter {
        @Override
        public boolean keyDown(int keycode) {
            boolean consumed = showcase.keyDown(keycode);
            refreshStatus();
            if (consumed) {
                return true;
            }
            if (keycode == Input.Keys.ESCAPE) {
                showcase.requestQuit(Gdx.app::exit);
                return true;
            }
            if (keycode == Input.Keys.F12) {
                screenshots.requestTimestampedScreenshot();
                return true;
            }
            return false;
        }

        @Override
        public boolean keyUp(int keycode) {
            return showcase.keyUp(keycode);
        }

        @Override
        public boolean keyTyped(char character) {
            return showcase.keyTyped(character);
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            boolean consumed = showcase.touchDown(screenX, screenY, pointer, button);
            refreshStatus();
            return consumed;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            return showcase.touchDragged(screenX, screenY, pointer);
        }

        @Override
        public boolean mouseMoved(int screenX, int screenY) {
            return showcase.mouseMoved(screenX, screenY);
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            boolean consumed = showcase.touchUp(screenX, screenY, pointer, button);
            refreshStatus();
            return consumed;
        }

        @Override
        public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
            return showcase.touchCancelled(screenX, screenY, pointer, button);
        }

        @Override
        public boolean scrolled(float amountX, float amountY) {
            return showcase.scrolled(amountX, amountY);
        }
    }
}
