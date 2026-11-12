package com.github.halfdeadgames.asciigdx.workbench.export;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontExporter;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

/** The paint screen's export command: asks for a folder, exports the open font size there, and remembers the folder. */
public final class PaintExport {

    /** Where the outcome of an export is reported. */
    public interface Messages {

        void startAction();

        void ok(String text);

        void warn(String text);
    }

    private final FolderPicker picker;
    private final ExportFolderMemory memory;
    private final File fontDirectory;
    private final CellSize fontSize;
    private final Messages messages;

    public PaintExport(FolderPicker picker, ExportFolderMemory memory, File fontDirectory, CellSize fontSize,
                       Messages messages) {
        this.picker = picker;
        this.memory = memory;
        this.fontDirectory = fontDirectory;
        this.fontSize = fontSize;
        this.messages = messages;
    }

    /** Must run on the render thread: the native picker opens a dialog. */
    public void run() {
        messages.startAction();
        Optional<File> chosen;
        try {
            chosen = picker.pickFolder(memory.startFolder());
        } catch (IllegalStateException e) {
            messages.warn("Cannot pick a folder: " + e.getMessage());
            return;
        }
        if (chosen.isEmpty()) {
            messages.ok("Export canceled");
            return;
        }
        exportInto(chosen.get());
    }

    private void exportInto(File folder) {
        try {
            FontExporter.export(fontDirectory, fontSize, folder);
        } catch (IOException | RuntimeException e) {
            messages.warn("Export failed: " + e.getMessage());
            return;
        }
        messages.ok("Exported " + FontExporter.pngFile(fontDirectory, fontSize, folder).getName() + " and "
            + FontExporter.charsFile(fontDirectory, fontSize, folder).getName() + " to:");
        messages.ok(folder.getAbsolutePath());
        rememberFolder(folder);
    }

    private void rememberFolder(File folder) {
        try {
            memory.remember(folder);
        } catch (RuntimeException e) {
            messages.warn("Could not remember the export folder: " + e.getMessage());
        }
    }
}
