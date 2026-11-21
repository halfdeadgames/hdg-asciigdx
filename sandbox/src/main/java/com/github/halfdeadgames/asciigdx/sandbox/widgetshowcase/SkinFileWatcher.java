package com.github.halfdeadgames.asciigdx.sandbox.widgetshowcase;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.github.halfdeadgames.asciigdx.ui.Skin;

/**
 * Loads the skin file and, on desktop, notices when the file on disk changes. Only a real file
 * is watched, so on Android the skin is only reloaded on request.
 */
public final class SkinFileWatcher {
    private static final float CHECK_INTERVAL_SECONDS = 0.5f;

    private final FileHandle skinFile;
    private final boolean watching;
    private long lastSeenModified;
    private float secondsSinceCheck;
    private boolean changeDetected;

    /** Creates a watcher for the given skin file; it remembers the file's current timestamp. */
    public SkinFileWatcher(FileHandle skinFile) {
        this.skinFile = skinFile;
        watching = Gdx.app.getType() == Application.ApplicationType.Desktop
                && skinFile.file().isFile();
        lastSeenModified = watching ? skinFile.lastModified() : 0L;
    }

    /**
     * Loads the skin from the file. The current timestamp counts as seen even when loading fails,
     * so a broken file is reported once rather than every check.
     *
     * @throws com.github.halfdeadgames.asciigdx.ui.SkinFileException if the file is not a valid
     *         skin
     */
    public Skin load() {
        changeDetected = false;
        if (watching) {
            lastSeenModified = skinFile.lastModified();
        }
        return Skin.fromFile(skinFile);
    }

    /** Adds frame time, and checks the file's timestamp once every half second. */
    public void update(float deltaSeconds) {
        if (!watching) {
            return;
        }
        secondsSinceCheck += deltaSeconds;
        if (secondsSinceCheck < CHECK_INTERVAL_SECONDS) {
            return;
        }
        secondsSinceCheck = 0f;
        if (skinFile.lastModified() != lastSeenModified) {
            changeDetected = true;
        }
    }

    /** Returns whether {@link #update} saw a newer file since the last {@link #load()}. */
    public boolean hasChangeBeenDetected() {
        return changeDetected;
    }
}
