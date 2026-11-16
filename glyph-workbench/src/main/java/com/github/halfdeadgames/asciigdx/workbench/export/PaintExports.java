package com.github.halfdeadgames.asciigdx.workbench.export;

import com.github.halfdeadgames.asciigdx.workbench.DevCapture;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.inputscript.ScriptSession;
import java.io.File;

/** Builds the paint screen's export command with the right folder picker for this run. */
public final class PaintExports {

    private PaintExports() {
    }

    /**
     * Call after the application has started, because the folder memory uses libGDX preferences.
     */
    public static PaintExport create(File fontDirectory, CellSize fontSize,
            PaintExport.Messages messages) {
        File defaultFolder = new File(repoRoot(), "build/exports");
        if (isAutomated()) {
            ExportFolderMemory memory =
                    new ExportFolderMemory(new RunOnlyExportFolderStore(), defaultFolder);
            return new PaintExport(new FixedFolderPicker(defaultFolder), memory, fontDirectory,
                    fontSize, messages);
        }
        ExportFolderMemory memory =
                new ExportFolderMemory(new PreferencesExportFolderStore(), defaultFolder);
        return new PaintExport(new NativeFolderPicker(), memory, fontDirectory, fontSize, messages);
    }

    private static boolean isAutomated() {
        return System.getProperty(ScriptSession.SCRIPT_PROPERTY) != null
                || System.getProperty(DevCapture.CAPTURE_PROPERTY) != null;
    }

    private static File repoRoot() {
        String configured = System.getProperty(ScriptSession.REPO_ROOT_PROPERTY);
        return new File(configured != null ? configured : System.getProperty("user.dir"));
    }
}
