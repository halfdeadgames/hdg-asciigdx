package com.github.halfdeadgames.asciigdx.sandbox;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.halfdeadgames.asciigdx.glyph.GlyphSource;
import com.github.halfdeadgames.asciigdx.glyph.SaiksisttSize;
import com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource;
import java.util.function.Function;

/** Sandbox application: a menu of test scenes for visually checking rendering features. */
public class SandboxApp extends ApplicationAdapter {
    private static final long CAPTURE_RANDOM_SEED = 1234L;

    private final Array<SandboxScene> scenes = new Array<>();
    private final Function<String, String> settings;
    private final Runnable orientationToggle;
    private GpuTimer gpuTimer = GpuTimer.UNAVAILABLE;
    private WindowResizer windowResizer = WindowResizer.NONE;
    private boolean windowResizedForScene;

    private SpriteBatch batch;
    private GlyphSource glyphs;
    private GlyphSource smallGlyphs;
    private GlyphSource tallGlyphs;
    private GlyphSource narrowTallGlyphs;
    private MenuScene menu;
    private PerfOverlay perfOverlay;
    private SandboxScene activeScene;

    private ScreenCapture screenCapture;
    private CaptureScript captureScript;

    /**
     * @param settings key to value lookup returning null when missing; see {@link CaptureConfig}
     */
    public SandboxApp(Function<String, String> settings) {
        this(settings, () -> {
        });
    }

    /**
     * @param orientationToggle called on the GL thread when the F9 dev key is pressed
     */
    public SandboxApp(Function<String, String> settings, Runnable orientationToggle) {
        this.settings = settings;
        this.orientationToggle = orientationToggle;
    }

    /** Sets the GPU timer that capture mode uses; call before the app starts. */
    public void setGpuTimer(GpuTimer gpuTimer) {
        this.gpuTimer = gpuTimer;
    }

    /** Sets what resizes the window for scenes that ask for a size; call before the app starts. */
    public void setWindowResizer(WindowResizer windowResizer) {
        this.windowResizer = windowResizer;
    }

    boolean isWindowSettled() {
        return windowResizer.isSettled();
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        glyphs = TextureGlyphSource.loadSaiksistt();
        smallGlyphs = TextureGlyphSource.loadSaiksistt(SaiksisttSize.SQUARE_8X8);
        tallGlyphs = TextureGlyphSource.loadSaiksistt(SaiksisttSize.TALL_16X32);
        narrowTallGlyphs = TextureGlyphSource.loadSaiksistt(SaiksisttSize.TALL_8X16);
        registerScenes();
        menu = new MenuScene(glyphs, scenes);
        menu.show();
        perfOverlay = new PerfOverlay(glyphs);
        createScreenCapture();
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            Gdx.input.setCatchKey(Input.Keys.BACK, true);
        }
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                return handleKey(keycode);
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (activeScene != null) {
                    boolean handled = activeScene.touchDown(screenX, screenY);
                    perfOverlay.setStatus(activeScene.statusText());
                    return handled;
                }
                int row = menu.rowAt(screenX, screenY);
                if (row < 0) {
                    return false;
                }
                menu.selectRow(row);
                activateMenuSelection();
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                return activeScene != null && activeScene.touchDragged(screenX, screenY);
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                return activeScene != null && activeScene.mouseMoved(screenX, screenY);
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (activeScene == null) {
                    return false;
                }
                boolean handled = activeScene.touchUp(screenX, screenY);
                perfOverlay.setStatus(activeScene.statusText());
                return handled;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                return activeScene != null && activeScene.scrolled(amountY);
            }
        });
        openReviewSceneIfRequested();
    }

    private void openReviewSceneIfRequested() {
        if (captureScript != null || !isSet(GlyphReviewScene.KEY_REVIEW_CHARS)
                && !isSet(GlyphReviewScene.KEY_REVIEW_SHEET)) {
            return;
        }
        for (int i = 0; i < scenes.size; i++) {
            if (scenes.get(i) instanceof GlyphReviewScene) {
                openScene(i);
                return;
            }
        }
    }

    private boolean isSet(String key) {
        String value = settings.apply(key);
        return value != null && !value.isEmpty();
    }

    /** Add one line per scene; the menu and the capture script pick them up. */
    private void registerScenes() {
        scenes.add(new RendererBasicsScene(glyphs));
        scenes.add(new BoxesAndBarsScene(glyphs));
        scenes.add(new FullRewriteStressScene(glyphs));
        scenes.add(new SparseUpdateScene(smallGlyphs));
        scenes.add(new MapScrollScene(smallGlyphs));
        scenes.add(new AnimatedCellsScene(smallGlyphs));
        scenes.add(new ManySpritesStressScene(smallGlyphs));
        if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            scenes.add(new LayeredBlendStressScene(smallGlyphs));
        }
        scenes.add(new GlyphShowcaseScene(smallGlyphs, SaiksisttSize.SQUARE_8X8));
        scenes.add(new GlyphShowcaseScene(glyphs, SaiksisttSize.SQUARE_16X16));
        scenes.add(new GlyphShowcaseScene(narrowTallGlyphs, SaiksisttSize.TALL_8X16));
        scenes.add(new GlyphShowcaseScene(tallGlyphs, SaiksisttSize.TALL_16X32));
        scenes.add(new LayersScene(glyphs));
        scenes.add(new SpritesScene(glyphs, false));
        scenes.add(new SpritesScene(glyphs, true));
        scenes.add(new GlyphFilterScene(glyphs));
        scenes.add(new LayerTransformScene(glyphs, settings.apply(LayerTransformScene.KEY_HOVER)));
        scenes.add(new ParallaxScene(glyphs));
        scenes.add(
                PondScene.autoWalk(glyphs, smallGlyphs, settings.apply(PondScene.KEY_POND_TIME)));
        scenes.add(PondScene.keyWalk(glyphs, smallGlyphs));
        scenes.add(new FlexibleGridScene(glyphs));
        scenes.add(new UiLabelsScene(glyphs, tallGlyphs));
        scenes.add(
                new ZonesScene(glyphs, narrowTallGlyphs, settings.apply(ZonesScene.KEY_ZONES_TIME),
                        settings.apply(ZonesScene.KEY_ZONES_START)));
        scenes.add(new ShowcaseScene(glyphs, tallGlyphs,
                settings.apply(ShowcaseScene.KEY_SHOWCASE_TIME)));
        scenes.add(new GlyphReviewScene(glyphs,
                settings.apply(GlyphReviewScene.KEY_REVIEW_CHARS),
                settings.apply(GlyphReviewScene.KEY_REVIEW_SHEET),
                settings.apply(GlyphReviewScene.KEY_REVIEW_CELL)));
    }

    private boolean handleKey(int keycode) {
        if (keycode == Input.Keys.BACK) {
            if (activeScene != null) {
                returnToMenu();
            } else if (!menu.isShowingGroupList()) {
                menu.showGroupList();
            } else {
                Gdx.app.exit();
            }
            return true;
        }
        if (keycode == Input.Keys.F9) {
            orientationToggle.run();
            return true;
        }
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACKSPACE) {
            if (activeScene == null) {
                if (menu.isShowingGroupList()) {
                    return false;
                }
                menu.showGroupList();
                return true;
            }
            if (activeScene.keyDown(keycode)) {
                perfOverlay.setStatus(activeScene.statusText());
                return true;
            }
            returnToMenu();
            return true;
        }
        if (activeScene == null) {
            if (keycode == Input.Keys.UP) {
                menu.moveSelection(-1);
                return true;
            }
            if (keycode == Input.Keys.DOWN) {
                menu.moveSelection(1);
                return true;
            }
            if (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER) {
                activateMenuSelection();
                return true;
            }
        }
        if (activeScene != null && activeScene.keyDown(keycode)) {
            perfOverlay.setStatus(activeScene.statusText());
            return true;
        }
        if (screenCapture != null && keycode == Input.Keys.F12) {
            screenCapture.requestTimestampedScreenshot();
            return true;
        }
        if (screenCapture != null && keycode == Input.Keys.G) {
            screenCapture.toggleManualRecording();
            return true;
        }
        return false;
    }

    private void activateMenuSelection() {
        if (menu.isShowingGroupList()) {
            menu.openSelectedGroup();
            return;
        }
        openScene(menu.selectedSceneIndex());
    }

    private void createScreenCapture() {
        CaptureConfig captureConfig = CaptureConfig.fromSettings(settings);
        if (captureConfig == null) {
            if (Gdx.app.getType() != Application.ApplicationType.Desktop) {
                return;
            }
            screenCapture = new ScreenCapture(Gdx.files.local("captures"));
            return;
        }
        MathUtils.random.setSeed(CAPTURE_RANDOM_SEED);
        screenCapture = new ScreenCapture(Gdx.files.absolute(captureConfig.getOutputDirectory()));
        captureScript = new CaptureScript(this, screenCapture, captureConfig);
    }

    int getSceneCount() {
        return scenes.size;
    }

    SandboxScene getScene(int index) {
        return scenes.get(index);
    }

    void openScene(int index) {
        returnToMenu();
        activeScene = scenes.get(index);
        menu.showSceneInGroup(index);
        activeScene.show();
        perfOverlay.begin(activeScene.cellCount());
        perfOverlay.setStatus(activeScene.statusText());
        activeScene.preferredWindowSize().ifPresent(this::resizeWindowForScene);
    }

    void refreshStatus() {
        if (activeScene != null) {
            perfOverlay.setStatus(activeScene.statusText());
        }
    }

    private void returnToMenu() {
        if (activeScene == null) {
            return;
        }
        activeScene.hide();
        activeScene = null;
        if (windowResizedForScene) {
            windowResizer.restorePreviousSize();
            windowResizedForScene = false;
        }
        menu.show();
    }

    /** Desktop libGDX renders a frame inside the resize callback, so state must be set first. */
    private void resizeWindowForScene(WindowSize size) {
        windowResizedForScene = true;
        windowResizer.resizeTo(size);
    }

    @Override
    public void resize(int width, int height) {
        menu.resize(width, height);
        perfOverlay.resize(width, height);
        if (activeScene != null) {
            activeScene.resize(width, height);
            perfOverlay.setCellCount(activeScene.cellCount());
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        long renderNanos = 0L;
        long gpuWaitNanos = 0L;
        long gpuDrawNanos = CaptureScript.GPU_DRAW_UNMEASURED;
        if (activeScene == null) {
            menu.render(batch, delta);
        } else {
            boolean timingGpu = captureScript != null && gpuTimer.isAvailable();
            long renderStartNanos = System.nanoTime();
            if (timingGpu) {
                gpuTimer.begin();
            }
            activeScene.render(batch, delta);
            if (timingGpu) {
                gpuTimer.end();
            }
            if (captureScript != null) {
                long submittedNanos = System.nanoTime();
                Gdx.gl.glFinish();
                long finishedNanos = System.nanoTime();
                renderNanos = finishedNanos - renderStartNanos;
                gpuWaitNanos = finishedNanos - submittedNanos;
            }
            if (timingGpu) {
                gpuDrawNanos = gpuTimer.readElapsedNanos();
            }
            if (activeScene.hasLiveStatus()) {
                refreshStatus();
            }
            perfOverlay.update(delta);
            perfOverlay.render(batch);
        }
        if (screenCapture != null) {
            screenCapture.onFrameRendered();
        }
        if (captureScript != null) {
            captureScript.onFrameRendered(renderNanos, gpuWaitNanos, gpuDrawNanos);
        }
    }

    @Override
    public void dispose() {
        if (screenCapture != null) {
            screenCapture.dispose();
        }
        for (int i = 0; i < scenes.size; i++) {
            scenes.get(i).dispose();
        }
        menu.dispose();
        perfOverlay.dispose();
        batch.dispose();
        glyphs.dispose();
        smallGlyphs.dispose();
        tallGlyphs.dispose();
        narrowTallGlyphs.dispose();
    }
}
