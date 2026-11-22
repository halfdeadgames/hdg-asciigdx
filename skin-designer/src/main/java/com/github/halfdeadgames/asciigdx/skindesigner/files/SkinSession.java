package com.github.halfdeadgames.asciigdx.skindesigner.files;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.github.halfdeadgames.asciigdx.skindesigner.document.SkinDocument;
import com.github.halfdeadgames.asciigdx.skindesigner.widgetshowcase.SkinFileWatcher;
import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

/**
 * The skin document being edited, and everything that ties it to disk: choosing, loading, saving
 * with a backup, the recent list, and watching the file for outside changes. Opening a file
 * replaces the document, so callers read {@link #getDocument()} again afterward.
 */
public final class SkinSession {
    private static final String SUGGESTED_FILE_NAME = "my-skin.json";
    private static final String ENCODING = "UTF-8";
    private static final String PREFERENCES_NAME = "asciigdx-skin-designer";

    private final FileChooser fileChooser;
    private final KnownSkins knownSkins;
    private final SkinBackups backups;
    private final SkinFileWatcher watcher;
    private final File bundledDefaultFile;
    private SkinDocument document;

    /** Creates a session around an already opened document that the watcher already follows. */
    public SkinSession(SkinDocument document, FileChooser fileChooser, KnownSkins knownSkins,
            SkinBackups backups, SkinFileWatcher watcher) {
        this.document = Objects.requireNonNull(document, "document");
        this.fileChooser = Objects.requireNonNull(fileChooser, "fileChooser");
        this.knownSkins = Objects.requireNonNull(knownSkins, "knownSkins");
        this.backups = Objects.requireNonNull(backups, "backups");
        this.watcher = Objects.requireNonNull(watcher, "watcher");
        bundledDefaultFile = document.isBundledDefault()
                ? canonicalOf(document.getFile().file()) : null;
    }

    /**
     * Creates the session the running app uses: native dialogs, recent files in the app's
     * preferences, skins detected in the working folder, and a file watcher on desktop. Needs
     * {@code Gdx.app}.
     */
    public static SkinSession createForRunningApp(SkinDocument document) {
        RecentSkins recentSkins = new RecentSkins(
                new PreferencesRecentSkinsStore(Gdx.app.getPreferences(PREFERENCES_NAME)),
                RecentSkins.DEFAULT_CAPACITY);
        KnownSkins knownSkins = new KnownSkins(recentSkins, new SkinDetector(),
                new File("").getAbsoluteFile());
        boolean watchFiles = Gdx.app.getType() == Application.ApplicationType.Desktop;
        return new SkinSession(document, new NativeFileChooser(), knownSkins,
                new SkinBackups(Clock.systemDefaultZone()),
                new SkinFileWatcher(document.getFile(), watchFiles));
    }

    /** Returns the current document; it is a different object after {@link #open}. */
    public SkinDocument getDocument() {
        return document;
    }

    /** Returns the recent files, then the detected ones. */
    public List<File> listKnownSkins() {
        return knownSkins.listAll();
    }

    /** Asks the user for a file to open; returns {@code null} when canceled. */
    public File chooseFileToOpen() {
        return fileChooser.chooseFileToOpen(folderForDialogs());
    }

    /** Asks the user for a file to save to; returns {@code null} when canceled. */
    public File chooseFileToSave() {
        File suggestedFile = document.isBundledDefault()
                ? new File(folderForDialogs(), SUGGESTED_FILE_NAME)
                : document.getFile().file().getAbsoluteFile();
        return fileChooser.chooseFileToSave(suggestedFile);
    }

    /**
     * Replaces the document with the contents of a file, remembers the file as recent, and starts
     * watching it.
     *
     * @throws com.github.halfdeadgames.asciigdx.ui.SkinFileException if the file is not a valid
     *         skin; the current document stays
     */
    public void open(File file) {
        boolean bundled = isBundledDefaultFile(file);
        document = SkinDocument.open(new FileHandle(file), bundled);
        if (!bundled) {
            knownSkins.remember(file);
        }
        watcher.follow(document.getFile());
    }

    /** Returns whether the file is the bundled default skin's file on disk. */
    public boolean isBundledDefaultFile(File file) {
        return bundledDefaultFile != null && bundledDefaultFile.equals(canonicalOf(file));
    }

    /**
     * Returns whether the document's own file is the bundled default skin's file, which a plain
     * Save must not overwrite. Stays true after a confirmed overwrite of that file.
     */
    public boolean isDocumentFileBundledDefault() {
        return document.isBundledDefault()
                || isBundledDefaultFile(document.getFile().file());
    }

    private static File canonicalOf(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException unresolvable) {
            return file.getAbsoluteFile();
        }
    }

    /**
     * Reads the current file again, dropping unsaved changes and undo history.
     *
     * @throws com.github.halfdeadgames.asciigdx.ui.SkinFileException if the file is no longer a
     *         valid skin; the current document stays
     */
    public void reloadFromDisk() {
        FileHandle file = document.getFile();
        document = SkinDocument.open(file, document.isBundledDefault());
        watcher.follow(file);
    }

    /**
     * Writes the document to a file. An existing file is copied into the backup folder first.
     * Afterward the document counts as saved to that file, and the watcher follows it.
     */
    public void save(File file) {
        FileHandle target = new FileHandle(file);
        backups.copyToBackup(target);
        target.writeString(document.toJson(), false, ENCODING);
        document.markSaved(target);
        knownSkins.remember(file);
        watcher.follow(target);
    }

    /** Adds frame time to the file watcher. */
    public void updateWatcher(float deltaSeconds) {
        watcher.update(deltaSeconds);
    }

    /** Returns whether the file on disk changed since it was loaded or saved here. */
    public boolean hasFileChangedOnDisk() {
        return watcher.hasChangeBeenDetected();
    }

    /** Stops reporting the current disk change, keeping the document as it is. */
    public void acknowledgeDiskChange() {
        watcher.acknowledgeChange();
    }

    private File folderForDialogs() {
        File parent = document.getFile().file().getAbsoluteFile().getParentFile();
        return parent != null ? parent : new File("").getAbsoluteFile();
    }
}
