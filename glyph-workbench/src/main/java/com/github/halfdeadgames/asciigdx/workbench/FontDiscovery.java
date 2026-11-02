package com.github.halfdeadgames.asciigdx.workbench;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Finds the paintable fonts: every {@code <fontsRoot>/<folder>/<size>.txt}. */
final class FontDiscovery {

    /** One font data file; the name is the folder name and the size is the file name without {@code .txt}. */
    record FontChoice(String folderName, int size) {

        /** Menu text such as {@code SAIKSISTT 8}. */
        String label() {
            return folderName.toUpperCase(Locale.ROOT) + " " + size;
        }
    }

    private static final Pattern SIZE_FILE = Pattern.compile("(\\d{1,4})\\.txt");

    private FontDiscovery() {
    }

    /** Fonts sorted by folder name, then size. A missing or unreadable root gives an empty list. */
    static List<FontChoice> discover(Path fontsRoot) {
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
            .thenComparingInt(FontChoice::size));
        return found;
    }

    private static void addSizes(List<FontChoice> found, Path folder) throws IOException {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, Files::isRegularFile)) {
            for (Path file : files) {
                Matcher matcher = SIZE_FILE.matcher(file.getFileName().toString());
                if (matcher.matches()) {
                    found.add(new FontChoice(folder.getFileName().toString(), Integer.parseInt(matcher.group(1))));
                }
            }
        }
    }
}
