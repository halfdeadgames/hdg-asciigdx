package com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SkinFileWatcherTest {

    private static final float PAST_CHECK_INTERVAL = 1f;

    @TempDir
    File folder;

    @Test
    void missingFileIsStillWatchedAndAReappearingFileIsDetected() {
        FileHandle skin = new FileHandle(new File(folder, "skin.json"));
        SkinFileWatcher watcher = new SkinFileWatcher(skin, true);

        watcher.update(PAST_CHECK_INTERVAL);
        assertFalse(watcher.hasChangeBeenDetected());

        skin.writeString("{}", false, "UTF-8");
        watcher.update(PAST_CHECK_INTERVAL);
        assertTrue(watcher.hasChangeBeenDetected());
    }

    @Test
    void followingAFileCountsItsCurrentTimestampAsSeen() {
        FileHandle skin = new FileHandle(new File(folder, "skin.json"));
        skin.writeString("{}", false, "UTF-8");
        SkinFileWatcher watcher = new SkinFileWatcher(skin, true);

        watcher.follow(skin);
        watcher.update(PAST_CHECK_INTERVAL);

        assertFalse(watcher.hasChangeBeenDetected());
    }

    @Test
    void disabledWatcherNeverDetects() {
        FileHandle skin = new FileHandle(new File(folder, "skin.json"));
        SkinFileWatcher watcher = new SkinFileWatcher(skin, false);

        skin.writeString("{}", false, "UTF-8");
        watcher.update(PAST_CHECK_INTERVAL);

        assertFalse(watcher.hasChangeBeenDetected());
    }
}
