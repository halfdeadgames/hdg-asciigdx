package com.github.halfdeadgames.asciigdx.workbench.export;

import java.io.File;
import java.util.Optional;

/** Asks for a folder to write into. */
public interface FolderPicker {

    /** Returns the chosen folder, or empty when the user cancels. */
    Optional<File> pickFolder(File startFolder);
}
