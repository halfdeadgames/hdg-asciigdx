package com.github.halfdeadgames.asciigdx.skindesigner.files;

import com.badlogic.gdx.utils.GdxRuntimeException;
import com.github.halfdeadgames.asciigdx.ui.SkinFileException;
import java.io.File;
import java.util.function.Consumer;

/**
 * The File menu commands without any screen code: load, save, save as, and reload. Problems are
 * reported as a message and never thrown, so a broken file keeps the current document.
 */
public final class FileCommands {
    private static final String ERROR_PREFIX = "Skin error: ";

    private final SkinSession session;
    private final Consumer<String> resultReporter;
    private final Runnable documentReplaced;
    private final BundledOverwriteConfirmation overwriteConfirmation;

    /**
     * Creates the commands.
     *
     * @param resultReporter receives a short message after every command
     * @param documentReplaced called after the session got a different document, so the screen
     *        can be rebuilt
     * @param overwriteConfirmation asked before the bundled default skin's file is overwritten
     */
    public FileCommands(SkinSession session, Consumer<String> resultReporter,
            Runnable documentReplaced, BundledOverwriteConfirmation overwriteConfirmation) {
        this.session = session;
        this.resultReporter = resultReporter;
        this.documentReplaced = documentReplaced;
        this.overwriteConfirmation = overwriteConfirmation;
    }

    /** Asks for a file and loads it; does nothing when the user cancels. */
    public void load() {
        File file = session.chooseFileToOpen();
        if (file == null) {
            return;
        }
        loadFile(file);
    }

    /** Loads the given file, replacing the document. */
    public void loadFile(File file) {
        try {
            session.open(file);
        } catch (SkinFileException | GdxRuntimeException failure) {
            resultReporter.accept(ERROR_PREFIX + failure.getMessage());
            return;
        }
        resultReporter.accept("Loaded " + file.getName());
        documentReplaced.run();
    }

    /** Saves to the document's own file. The bundled default is never overwritten: it asks. */
    public void save() {
        if (session.isDocumentFileBundledDefault()) {
            saveAs();
            return;
        }
        saveTo(session.getDocument().getFile().file());
    }

    /**
     * Asks for a file and saves to it; does nothing when the user cancels. Choosing the bundled
     * default's file asks for confirmation first, and the answer may come later.
     */
    public void saveAs() {
        File file = session.chooseFileToSave();
        if (file == null) {
            return;
        }
        if (session.isBundledDefaultFile(file)) {
            overwriteConfirmation.ask(() -> saveTo(file),
                    () -> resultReporter.accept("Not saved: the bundled default was kept"));
            return;
        }
        saveTo(file);
    }

    /** Reads the document's file again and drops unsaved changes. */
    public void reloadFromDisk() {
        try {
            session.reloadFromDisk();
        } catch (SkinFileException | GdxRuntimeException failure) {
            // Without this the same change would be reported again on every check.
            session.acknowledgeDiskChange();
            resultReporter.accept(ERROR_PREFIX + failure.getMessage());
            return;
        }
        resultReporter.accept("Skin reloaded");
        documentReplaced.run();
    }

    private void saveTo(File file) {
        try {
            session.save(file);
        } catch (GdxRuntimeException failure) {
            resultReporter.accept("Save failed: " + failure.getMessage());
            return;
        }
        resultReporter.accept("Saved " + file.getName());
    }
}
