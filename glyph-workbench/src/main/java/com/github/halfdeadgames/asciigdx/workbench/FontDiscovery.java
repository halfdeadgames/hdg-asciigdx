package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Finds the paintable fonts: every {@code <fontsRoot>/<folder>/<width>x<height>.txt} whose size is square or
 * twice as tall.
 */
public final class FontDiscovery {

    /** One font data file; the name is the folder name and the size is the file name without {@code .txt}. */
    public record FontChoice(String folderName, CellSize size) {

        /** Menu text such as {@code SAIKSISTT 8X8}. */
        String label() {
            return folderName.toUpperCase(Locale.ROOT) + " " + size.toString().toUpperCase(Locale.ROOT);
        }
    }

    private FontDiscovery() {
    }

    /** Fonts sorted by folder name, then cell height, then cell width. A missing or unreadable root gives an empty list. */
    public static List<FontChoice> discover(Path fontsRoot) {
        List<FontChoice> found = new ArrayList<>();
        if (!Files.isDirectory(fontsRoot)) {
            return found;
        }
        try (DirectoryStream<Path> folders = Files.newDirectoryStream(fontsRoot, Files::isDirectory)) {
            for (Path folder : folders) {
                addSizes(found, folder);
            }
        } catch (IOException e) {
            return new ArrayList<>();
        }
        found.sort(Comparator.comparing((FontChoice choice) -> choice.folderName().toLowerCase(Locale.ROOT))
            .thenComparingInt((FontChoice choice) -> choice.size().height())
            .thenComparingInt((FontChoice choice) -> choice.size().width()));
        return found;
    }

    private static void addSizes(List<FontChoice> found, Path folder) throws IOException {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, Files::isRegularFile)) {
            for (Path file : files) {
                CellSize size = CellSize.ofDataFileName(file.getFileName().toString());
                if (size != null && size.isSquareOrTall()) {
                    found.add(new FontChoice(folder.getFileName().toString(), size));
                }
            }
        }
    }
}
