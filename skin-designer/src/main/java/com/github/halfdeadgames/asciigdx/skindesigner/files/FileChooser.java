package com.github.halfdeadgames.asciigdx.skindesigner.files;

import java.io.File;

/** Asks the user to pick a skin file. Both methods return {@code null} when the user cancels. */
public interface FileChooser {

    /** Asks for an existing skin file, starting in the given folder. */
    File chooseFileToOpen(File startFolder);

    /** Asks for the file to save to, proposing the given file. */
    File chooseFileToSave(File suggestedFile);
}
