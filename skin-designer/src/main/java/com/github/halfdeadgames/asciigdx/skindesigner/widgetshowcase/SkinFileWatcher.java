package com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase;

import com.badlogic.gdx.files.FileHandle;

/**
 * Notices when a skin file on disk changes. Only a real file is watched, so on Android nothing is
 * detected and the skin is only reloaded on request. The watcher follows whichever file it was
 * last pointed at.
 */
public final class SkinFileWatcher {
    private static final float CHECK_INTERVAL_SECONDS = 0.5f;

    private final boolean enabled;
    private FileHandle skinFile;
    private boolean watching;
    private long lastSeenModified;
    private float secondsSinceCheck;
    private boolean changeDetected;

    /**
     * Creates a watcher that already follows the given file.
     *
     * @param enabled whether files can be watched at all; false on platforms without real files
     */
    public SkinFileWatcher(FileHandle skinFile, boolean enabled) {
        this.enabled = enabled;
        follow(skinFile);
    }

    /**
     * Starts watching another file, or the same file again. The file's current timestamp counts as
     * seen, and a detected change is forgotten. Call it after loading or saving the file, so the
     * app's own writes are not reported.
     */
    public void follow(FileHandle file) {
        skinFile = file;
        // A missing file (an editor's atomic rename) reads as timestamp 0, so it is still watched.
        watching = enabled;
        lastSeenModified = watching ? file.lastModified() : 0L;
        secondsSinceCheck = 0f;
        changeDetected = false;
    }

    /** Counts the file's current timestamp as seen and forgets a detected change. */
    public void acknowledgeChange() {
        follow(skinFile);
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

    /** Returns whether {@link #update} saw a newer file since the last follow or acknowledge. */
    public boolean hasChangeBeenDetected() {
        return changeDetected;
    }
}
