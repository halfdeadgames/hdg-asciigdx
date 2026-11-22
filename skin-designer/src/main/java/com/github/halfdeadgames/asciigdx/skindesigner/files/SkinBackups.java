package com.github.halfdeadgames.asciigdx.skindesigner.files;

import com.badlogic.gdx.files.FileHandle;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Copies a skin file into a {@code skin-backups} folder next to it before it is overwritten. Every
 * backup is kept. Names look like {@code default-skin-2026-11-22-153012.json}.
 */
public final class SkinBackups {
    static final String FOLDER_NAME = "skin-backups";
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss", Locale.ROOT);

    private final Clock clock;

    /** Creates backups that are named after the time of the given clock. */
    public SkinBackups(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Returns the file a backup of the given skin file would be written to now. When a backup with
     * that name exists already (two saves within one second), a counter is added to the name, so
     * no backup is overwritten.
     */
    public FileHandle backupFileFor(FileHandle skinFile) {
        FileHandle folder = skinFile.parent().child(FOLDER_NAME);
        String baseName = skinFile.nameWithoutExtension() + "-"
                + TIMESTAMP_FORMAT.format(LocalDateTime.now(clock));
        String extension = skinFile.extension().isEmpty() ? "" : "." + skinFile.extension();
        FileHandle candidate = folder.child(baseName + extension);
        int counter = 2;
        while (candidate.exists()) {
            candidate = folder.child(baseName + "-" + counter + extension);
            counter++;
        }
        return candidate;
    }

    /** Copies the file into its backup folder; does nothing when the file does not exist. */
    public void copyToBackup(FileHandle skinFile) {
        if (!skinFile.exists()) {
            return;
        }
        skinFile.copyTo(backupFileFor(skinFile));
    }
}
