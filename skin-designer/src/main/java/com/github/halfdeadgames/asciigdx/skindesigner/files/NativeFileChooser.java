package com.github.halfdeadgames.asciigdx.skindesigner.files;

import java.io.File;
import java.util.Locale;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

/**
 * Operating system open and save dialogs through LWJGL tinyfd, filtered to {@code *.json}. The
 * dialogs block the render loop while they are open. Call them on the render thread.
 */
public final class NativeFileChooser implements FileChooser {
    private static final String OPEN_TITLE = "Open skin file";
    private static final String SAVE_TITLE = "Save skin file";
    private static final String FILTER_PATTERN = "*.json";
    private static final String FILTER_DESCRIPTION = "Skin files (*.json)";
    private static final String JSON_EXTENSION = ".json";

    @Override
    public File chooseFileToOpen(File startFolder) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            String chosenPath = TinyFileDialogs.tinyfd_openFileDialog(OPEN_TITLE,
                    folderPathOf(startFolder), createFilter(stack), FILTER_DESCRIPTION, false);
            return chosenPath == null ? null : new File(chosenPath);
        }
    }

    @Override
    public File chooseFileToSave(File suggestedFile) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            String chosenPath = TinyFileDialogs.tinyfd_saveFileDialog(SAVE_TITLE,
                    suggestedFile.getAbsolutePath(), createFilter(stack), FILTER_DESCRIPTION);
            return chosenPath == null ? null : new File(withJsonExtension(chosenPath));
        }
    }

    private static PointerBuffer createFilter(MemoryStack stack) {
        PointerBuffer patterns = stack.mallocPointer(1);
        patterns.put(stack.UTF8(FILTER_PATTERN)).flip();
        return patterns;
    }

    // A trailing separator makes the dialog open inside the folder instead of selecting it.
    private static String folderPathOf(File folder) {
        return folder.getAbsolutePath() + File.separator;
    }

    private static String withJsonExtension(String path) {
        if (path.toLowerCase(Locale.ROOT).endsWith(JSON_EXTENSION)) {
            return path;
        }
        return path + JSON_EXTENSION;
    }
}
