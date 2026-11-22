package com.github.halfdeadgames.asciigdx.skindesigner.files;

import com.github.halfdeadgames.asciigdx.skindesigner.files.ChoiceDialogs.ChoiceButton;
import com.github.halfdeadgames.asciigdx.ui.Dialog;
import com.github.halfdeadgames.asciigdx.ui.DialogChoice;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.UiRoot;
import java.util.function.Supplier;

/**
 * Runs an action that would lose unsaved changes (load, quit) only once the user agreed: Save,
 * Discard, or Cancel.
 */
public final class UnsavedChangesGuard {
    private static final String DIALOG_STYLE = "dialog";
    private static final String TITLE = "Unsaved changes";
    private static final String MESSAGE = "The skin has changes that are not saved.\n"
            + "Save writes them to the file first, Discard throws them away,\n"
            + "and Cancel goes back to editing.";

    private final SkinSession session;
    private final FileCommands commands;
    private final Supplier<UiRoot> uiRoot;
    private final Supplier<Skin> skin;
    private Dialog shownDialog;
    private UiRoot askedRoot;

    /**
     * Creates the guard. The suppliers are asked again for every dialog, because the screen is
     * rebuilt now and then.
     */
    public UnsavedChangesGuard(SkinSession session, FileCommands commands,
            Supplier<UiRoot> uiRoot, Supplier<Skin> skin) {
        this.session = session;
        this.commands = commands;
        this.uiRoot = uiRoot;
        this.skin = skin;
    }

    /**
     * Runs the action right away when nothing is unsaved. Otherwise asks first, and runs it after
     * a successful Save or a Discard. A canceled save dialog or a failed save keeps everything as
     * it is and the action does not run.
     */
    public void runWhenSafe(Runnable action) {
        if (!session.getDocument().hasUnsavedChanges()) {
            action.run();
            return;
        }
        UiRoot currentRoot = uiRoot.get();
        boolean alreadyAsking = shownDialog != null && shownDialog.isShowing()
                && askedRoot == currentRoot;
        if (alreadyAsking) {
            return;
        }
        askedRoot = currentRoot;
        shownDialog = ChoiceDialogs.create(skin.get().getDialogStyle(DIALOG_STYLE), TITLE, MESSAGE,
                DialogChoice.CANCEL, (dialog, choice) -> answered(choice, action),
                new ChoiceButton("Save", DialogChoice.YES),
                new ChoiceButton("Discard", DialogChoice.NO),
                new ChoiceButton("Cancel", DialogChoice.CANCEL));
        shownDialog.show(uiRoot.get());
    }

    private void answered(DialogChoice choice, Runnable action) {
        if (choice == DialogChoice.NO) {
            action.run();
            return;
        }
        if (choice == DialogChoice.YES) {
            commands.save();
            if (!session.getDocument().hasUnsavedChanges()) {
                action.run();
            }
        }
    }
}
