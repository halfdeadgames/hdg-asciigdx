package com.github.halfdeadgames.asciigdx.skindesigner.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SkinBackupsTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-11-22T15:30:12Z"), ZoneOffset.UTC);

    @TempDir
    File folder;

    private final SkinBackups backups = new SkinBackups(FIXED_CLOCK);

    private FileHandle writeSkin(String text) {
        FileHandle skin = new FileHandle(new File(folder, "default-skin.json"));
        skin.writeString(text, false, "UTF-8");
        return skin;
    }

    @Test
    void backupIsNamedWithTimestampInBackupFolderNextToSkin() {
        FileHandle backup = backups.backupFileFor(writeSkin("{}"));

        assertEquals(new File(folder, "skin-backups"), backup.parent().file());
        assertEquals("default-skin-2026-11-22-153012.json", backup.name());
    }

    @Test
    void copyKeepsTheContentsOfTheOriginal() {
        FileHandle skin = writeSkin("{\"colors\":{}}");

        backups.copyToBackup(skin);

        FileHandle backup = new FileHandle(
                new File(folder, "skin-backups/default-skin-2026-11-22-153012.json"));
        assertEquals("{\"colors\":{}}", backup.readString("UTF-8"));
    }

    @Test
    void secondBackupInTheSameSecondGetsACounterAndKeepsTheFirst() {
        FileHandle skin = writeSkin("first");
        backups.copyToBackup(skin);
        skin.writeString("second", false, "UTF-8");

        backups.copyToBackup(skin);

        File backupFolder = new File(folder, "skin-backups");
        assertEquals("first", new FileHandle(
                new File(backupFolder, "default-skin-2026-11-22-153012.json")).readString());
        assertEquals("second", new FileHandle(
                new File(backupFolder, "default-skin-2026-11-22-153012-2.json")).readString());
    }

    @Test
    void missingFileIsNotBackedUp() {
        FileHandle missing = new FileHandle(new File(folder, "none.json"));

        backups.copyToBackup(missing);

        assertFalse(new File(folder, "skin-backups").exists());
        assertTrue(folder.isDirectory());
    }
}
