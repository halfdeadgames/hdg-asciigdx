package com.github.halfdeadgames.asciigdx.inputscript;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import java.nio.file.Path;

/** Runs an input script against another application listener, which stays unaware of it. */
public final class ScriptedApplication implements ApplicationListener {

    private final ApplicationListener delegate;
    private final Path scriptFile;
    private final Path captureDirectory;
    private ScriptSession session;

    public ScriptedApplication(
            ApplicationListener delegate, Path scriptFile, Path captureDirectory) {
        this.delegate = delegate;
        this.scriptFile = scriptFile;
        this.captureDirectory = captureDirectory;
    }

    @Override
    public void create() {
        // The session swaps in the scripted input first, so the delegate registers on it.
        session = new ScriptSession(scriptFile, captureDirectory);
        delegate.create();
    }

    @Override
    public void resize(int width, int height) {
        delegate.resize(width, height);
    }

    @Override
    public void render() {
        session.beforeFrame(Gdx.graphics.getDeltaTime());
        delegate.render();
        session.afterFrame();
    }

    @Override
    public void pause() {
        delegate.pause();
    }

    @Override
    public void resume() {
        delegate.resume();
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }
}
