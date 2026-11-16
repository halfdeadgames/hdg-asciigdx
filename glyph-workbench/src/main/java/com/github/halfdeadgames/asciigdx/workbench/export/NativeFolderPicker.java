package com.github.halfdeadgames.asciigdx.workbench.export;

import java.io.File;
import java.util.Optional;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.nfd.NativeFileDialog;

/** Operating system folder dialog through LWJGL NFD. Call it on the render thread. */
public final class NativeFolderPicker implements FolderPicker {

    @Override
    public Optional<File> pickFolder(File startFolder) {
        throwIfFailed(NativeFileDialog.NFD_Init());
        try {
            return showDialog(startFolder);
        } finally {
            NativeFileDialog.NFD_Quit();
        }
    }

    private Optional<File> showDialog(File startFolder) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer chosenPath = stack.mallocPointer(1);
            int result = NativeFileDialog.NFD_PickFolder(chosenPath, defaultPathOf(startFolder));
            if (result == NativeFileDialog.NFD_CANCEL) {
                return Optional.empty();
            }
            throwIfFailed(result);
            return Optional.of(readAndFree(chosenPath));
        }
    }

    private String defaultPathOf(File startFolder) {
        if (startFolder == null || !startFolder.isDirectory()) {
            return null;
        }
        return startFolder.getAbsolutePath();
    }

    private File readAndFree(PointerBuffer chosenPath) {
        String path = chosenPath.getStringUTF8(0);
        NativeFileDialog.NFD_FreePath(chosenPath.get(0));
        return new File(path);
    }

    private void throwIfFailed(int result) {
        if (result == NativeFileDialog.NFD_ERROR) {
            throw new IllegalStateException(
                    "Folder dialog failed: " + NativeFileDialog.NFD_GetError());
        }
    }
}
