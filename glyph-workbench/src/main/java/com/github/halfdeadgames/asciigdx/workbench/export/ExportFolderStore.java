package com.github.halfdeadgames.asciigdx.workbench.export;

import java.util.Optional;

/** Where the last export folder path is kept between runs. */
public interface ExportFolderStore {

    Optional<String> storedPath();

    void storePath(String path);

    void flush();
}
