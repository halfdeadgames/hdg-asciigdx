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
import com.github.halfdeadgames.asciigdx.glyph.TextureGlyphSource;

/** Sandbox application: a menu of test scenes for visually checking rendering features. */
public class SandboxApp extends ApplicationAdapter {
    private static final String CAPTURE_PROPERTY = "asciigdx.capture";
    private static final long CAPTURE_RANDOM_SEED = 1234L;

    private final Array<SandboxScene> scenes = new Array<>();

    private SpriteBatch batch;
    private GlyphSource glyphs;
    private MenuScene menu;
    private PerfOverlay perfOverlay;
    private SandboxScene activeScene;

    private ScreenCapture screenCapture;
    private CaptureScript captureScript;

    @Override
    public void create() {
        batch = new SpriteBatch();
        glyphs = TextureGlyphSource.loadDungeonMode();
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
                    return false;
                }
                int index = menu.indexAt(screenX, screenY);
                if (index < 0) {
                    return false;
                }
                openScene(index);
                return true;
            }
        });
    }

    /** Add one line per scene; the menu and the capture script pick them up. */
    private void registerScenes() {
        scenes.add(new RendererBasicsScene(glyphs));
        scenes.add(new FullRewriteStressScene(glyphs));
    }

    private boolean handleKey(int keycode) {
        if (keycode == Input.Keys.BACK) {
            if (activeScene != null) {
                returnToMenu();
            } else {
                Gdx.app.exit();
            }
            return true;
        }
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACKSPACE) {
            if (activeScene == null) {
                return false;
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
                openScene(menu.selectedIndex());
                return true;
            }
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

    private void createScreenCapture() {
        if (Gdx.app.getType() != Application.ApplicationType.Desktop) {
            return;
        }
        String captureDirectory = System.getProperty(CAPTURE_PROPERTY);
        if (captureDirectory == null || captureDirectory.isEmpty()) {
            screenCapture = new ScreenCapture(Gdx.files.local("captures"));
            return;
        }
        MathUtils.random.setSeed(CAPTURE_RANDOM_SEED);
        screenCapture = new ScreenCapture(Gdx.files.absolute(captureDirectory));
        captureScript = new CaptureScript(this, screenCapture);
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
        activeScene.show();
        perfOverlay.begin(activeScene.cellCount());
    }

    private void returnToMenu() {
        if (activeScene == null) {
            return;
        }
        activeScene.hide();
        activeScene = null;
        menu.show();
    }

    @Override
    public void resize(int width, int height) {
        menu.resize(width, height);
        perfOverlay.resize(width, height);
        if (activeScene != null) {
            activeScene.resize(width, height);
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        if (activeScene == null) {
            menu.render(batch, delta);
        } else {
            activeScene.render(batch, delta);
            perfOverlay.update(delta);
            perfOverlay.render(batch);
        }
        if (screenCapture != null) {
            screenCapture.onFrameRendered();
        }
        if (captureScript != null) {
            captureScript.onFrameRendered();
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
    }
}
