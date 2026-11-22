package com.github.halfdeadgames.asciigdx.skindesigner.files;

import com.github.halfdeadgames.asciigdx.skindesigner.files.ChoiceDialogs.ChoiceButton;
import com.github.halfdeadgames.asciigdx.ui.Dialog;
import com.github.halfdeadgames.asciigdx.ui.DialogChoice;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import java.util.function.Supplier;

/**
 * Reacts when the skin file changes on disk: a clean document reloads silently, one with unsaved
 * changes asks whether to reload or keep the user's changes.
 */
public final class DiskChangeReaction {
    private static final String DIALOG_STYLE = "dialog";
    private static final String TITLE = "Skin file changed on disk";
    private static final String MESSAGE = "The skin file changed on disk, and you have unsaved"
            + " changes.\nReloading loads the file and loses your changes.\n"
            + "Keeping mine ignores the change on disk.";

    private final SkinSession session;
    private final FileCommands commands;
    private final Supplier<UiRoot> uiRoot;
    private final Supplier<Skin> skin;
    private final Runnable beforeUnsavedCheck;
    private Dialog shownDialog;
    private UiRoot askedRoot;

    /**
     * Creates the reaction. The suppliers are asked again for every dialog, because the screen is
     * rebuilt now and then. {@code beforeUnsavedCheck} runs before the unsaved check, so pending
     * edits count as unsaved changes.
     */
    public DiskChangeReaction(SkinSession session, FileCommands commands,
            Supplier<UiRoot> uiRoot, Supplier<Skin> skin, Runnable beforeUnsavedCheck) {
        this.session = session;
        this.commands = commands;
        this.uiRoot = uiRoot;
        this.skin = skin;
        this.beforeUnsavedCheck = beforeUnsavedCheck;
    }

    /** Call once per frame: watches the file and reacts to a change. */
    public void update(float deltaSeconds) {
        session.updateWatcher(deltaSeconds);
        if (!session.hasFileChangedOnDisk() || isAsking()) {
            return;
        }
        beforeUnsavedCheck.run();
        if (!session.getDocument().hasUnsavedChanges()) {
            commands.reloadFromDisk();
            return;
        }
        ask();
    }

    // A dialog on a root that was replaced by a screen rebuild is gone from the user's view.
    private boolean isAsking() {
        return shownDialog != null && shownDialog.isShowing() && askedRoot == uiRoot.get();
    }

    private void ask() {
        askedRoot = uiRoot.get();
        shownDialog = ChoiceDialogs.create(skin.get().getDialogStyle(DIALOG_STYLE), TITLE, MESSAGE,
                DialogChoice.NO, (dialog, choice) -> answered(choice),
                new ChoiceButton("Reload and lose my changes", DialogChoice.YES),
                new ChoiceButton("Keep mine", DialogChoice.NO));
        shownDialog.show(uiRoot.get());
    }

    private void answered(DialogChoice choice) {
        if (choice == DialogChoice.YES) {
            commands.reloadFromDisk();
            return;
        }
        session.acknowledgeDiskChange();
    }
}
