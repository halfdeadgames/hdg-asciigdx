package com.github.halfdeadgames.asciigdx.workbench.upscale;

import com.github.halfdeadgames.asciigdx.workbench.FontDiscovery;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontHeader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads the cell size of every font data file under the fonts root, for listing upscale sources. */
public final class UpscaleSourceFonts {

    private UpscaleSourceFonts() {
    }

    /**
     * Fonts in {@link FontDiscovery} order; files that cannot be read, have no cell line, or declare a cell
     * other than their name says are left out.
     */
    public static List<UpscaleSources.Font> discover(Path fontsRoot) {
        List<UpscaleSources.Font> fonts = new ArrayList<>();
        for (FontDiscovery.FontChoice choice : FontDiscovery.discover(fontsRoot)) {
            UpscaleSources.Font font = readFont(fontsRoot, choice);
            if (font != null) {
                fonts.add(font);
            }
        }
        return fonts;
    }

    // Null when the file cannot be read, has no cell line, or declares a cell other than its name says.
    private static UpscaleSources.Font readFont(Path fontsRoot, FontDiscovery.FontChoice choice) {
        Path file = fontsRoot.resolve(choice.folderName()).resolve(choice.size().dataFileName());
        try {
            CellSize cell = FontHeader.read(file);
            if (cell == null || !cell.equals(choice.size())) {
                return null;
            }
            return new UpscaleSources.Font(choice.folderName(), cell.width(), cell.height());
        } catch (IOException e) {
            return null;
        }
    }
}
