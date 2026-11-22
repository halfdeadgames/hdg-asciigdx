package com.github.halfdeadgames.asciigdx.skindesigner.files;

import com.github.halfdeadgames.asciigdx.skindesigner.document.SkinDocument;
import com.github.halfdeadgames.asciigdx.skindesigner.files.ChoiceDialogs.ChoiceButton;
import com.github.halfdeadgames.asciigdx.ui.CellArea;
import com.github.halfdeadgames.asciigdx.ui.DialogChoice;
import com.github.halfdeadgames.asciigdx.ui.Menu;
import com.github.halfdeadgames.asciigdx.ui.MenuBar;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The File menu of the skin designer: Load, Known skins, Save, and Save As, with the guard for
 * unsaved changes and the reaction to disk changes. The screen only adds the menu, calls
 * {@link #update}, and asks {@link #requestQuit} before it quits.
 */
public final class SkinFileMenu {
    private static final String UNSAVED_MARKER = "* unsaved";
    private static final String DIALOG_STYLE = "dialog";

    private final SkinSession session;
    private final FileCommands commands;
    private final UnsavedChangesGuard guard;
    private final DiskChangeReaction diskChangeReaction;
    private final Supplier<UiRoot> uiRoot;
    private final Supplier<CellArea> pickerArea;
    private KnownSkinsPicker knownSkinsPicker;

    /**
     * Creates the menu logic.
     *
     * @param resultReporter receives a short message after every command
     * @param documentReplaced called after a different document was loaded, so the screen can be
     *        rebuilt; read {@link #getDocument()} again then
     * @param uiRoot the root dialogs are shown on, asked again for every dialog
     * @param pickerArea the area, in UI cells, that the known skins window is centered in
     */
    public SkinFileMenu(SkinSession session, Consumer<String> resultReporter,
            Runnable documentReplaced, Supplier<UiRoot> uiRoot, Supplier<CellArea> pickerArea) {
        this.session = session;
        this.uiRoot = uiRoot;
        this.pickerArea = pickerArea;
        commands = new FileCommands(session, resultReporter, documentReplaced,
                this::askToOverwriteBundledDefault);
        guard = new UnsavedChangesGuard(session, commands, uiRoot,
                () -> session.getDocument().getSkin());
        diskChangeReaction = new DiskChangeReaction(session, commands, uiRoot,
                () -> session.getDocument().getSkin());
    }

    /** Adds the File menu to a menu bar. */
    public void addTo(MenuBar menuBar) {
        // One picker per screen build, so its open check stops a second window from stacking.
        knownSkinsPicker = new KnownSkinsPicker(uiRoot.get(), session.getDocument().getSkin(),
                file -> guard.runWhenSafe(() -> commands.loadFile(file)));
        Menu menu = menuBar.addMenu("File", 'F');
        menu.addItem("Load…", 'L', () -> guard.runWhenSafe(commands::load));
        menu.addItem("Known skins", 'K', this::openKnownSkins);
        menu.addItem("Save", 'S', commands::save);
        menu.addItem("Save As…", 'A', commands::saveAs);
    }

    /** Returns the document being edited; it is a different object after a load. */
    public SkinDocument getDocument() {
        return session.getDocument();
    }

    /** Reloads the document's file again, asking first when that would lose unsaved changes. */
    public void reloadFromDisk() {
        guard.runWhenSafe(commands::reloadFromDisk);
    }

    /** Runs the quit action at once when nothing is unsaved, otherwise after the user agreed. */
    public void requestQuit(Runnable quitAction) {
        guard.runWhenSafe(quitAction);
    }

    /** Call once per frame: watches the file on disk. */
    public void update(float deltaSeconds) {
        diskChangeReaction.update(deltaSeconds);
    }

    /** Returns the file name for the status line, with a marker while changes are unsaved. */
    public String describeFile() {
        SkinDocument document = session.getDocument();
        String name = document.getFile().name();
        return document.hasUnsavedChanges() ? name + " " + UNSAVED_MARKER : name;
    }

    private void askToOverwriteBundledDefault(Runnable confirmed, Runnable declined) {
        ChoiceDialogs.create(session.getDocument().getSkin().getDialogStyle(DIALOG_STYLE),
                "Overwrite the bundled default?",
                "Overwrite the bundled default skin?\n"
                        + "A backup of it goes into the skin-backups folder first.",
                DialogChoice.NO,
                (dialog, choice) -> {
                    if (choice == DialogChoice.YES) {
                        confirmed.run();
                    } else {
                        declined.run();
                    }
                },
                new ChoiceButton("Yes", DialogChoice.YES),
                new ChoiceButton("No", DialogChoice.NO)).show(uiRoot.get());
    }

    private void openKnownSkins() {
        knownSkinsPicker.open(session.listKnownSkins(), pickerArea.get());
    }
}
