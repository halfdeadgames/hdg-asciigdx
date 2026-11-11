package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The "New size" rules for one font folder: which sizes are suggested, and creating the size file.
 * Sizes must be square or twice as tall, within the cell limits. Suggestions are the half (when both sides are even) and the
 * double of each existing {@code <width>x<height>.txt} size; {@code .guides.txt} and other sidecar files
 * are not size files. Files of any other shape (neither square nor twice as tall) are ignored.
 */
public final class NewSizePlan {

    /**
     * One entry of the size pick list. Taken sizes are listed so they can be shown greyed.
     *
     * @param size the cell size
     * @param taken whether the font already has a file for this size
     */
    public record SizeOption(CellSize size, boolean taken) {
    }

    private static final Comparator<CellSize> BY_HEIGHT_THEN_WIDTH =
        Comparator.comparingInt(CellSize::height).thenComparingInt(CellSize::width);

    private NewSizePlan() {
    }

    /**
     * Lists the folder's existing sizes and the sizes suggested from them, ascending by height then width.
     *
     * @param fontFolder a font folder holding {@code <width>x<height>.txt} files
     * @return the options, or an empty list when the folder has no size file
     * @throws IOException if the folder cannot be read
     */
    public static List<SizeOption> options(Path fontFolder) throws IOException {
        Set<CellSize> existing = existingSizes(fontFolder);
        Set<CellSize> suggested = new LinkedHashSet<>();
        for (CellSize size : existing) {
            suggested.addAll(suggestionsFor(size));
        }
        suggested.removeAll(existing);

        List<SizeOption> options = new ArrayList<>();
        existing.forEach(size -> options.add(new SizeOption(size, true)));
        suggested.forEach(size -> options.add(new SizeOption(size, false)));
        options.sort(Comparator.comparing(SizeOption::size, BY_HEIGHT_THEN_WIDTH));
        return options;
    }

    /**
     * Creates {@code <width>x<height>.txt} containing {@code cell WxH}. An existing file is never touched.
     *
     * @param fontFolder the font folder
     * @param size the new cell size
     * @throws IllegalArgumentException if the size is outside the allowed range
     * @throws java.nio.file.FileAlreadyExistsException if the size is already taken
     * @throws IOException if the file cannot be written
     */
    public static void create(Path fontFolder, CellSize size) throws IOException {
        String problem = NewFontInput.sizeError(size);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        Path target = fontFolder.resolve(size.dataFileName());
        NewSizeFile.writeNew(target, size);
    }

    private static List<CellSize> suggestionsFor(CellSize size) {
        List<CellSize> suggestions = new ArrayList<>();
        if (size.width() % 2 == 0 && size.height() % 2 == 0) {
            suggestions.add(new CellSize(size.width() / 2, size.height() / 2));
        }
        suggestions.add(new CellSize(size.width() * 2, size.height() * 2));
        suggestions.removeIf(suggestion -> NewFontInput.sizeError(suggestion) != null);
        return suggestions;
    }

    private static Set<CellSize> existingSizes(Path fontFolder) throws IOException {
        Set<CellSize> sizes = new LinkedHashSet<>();
        try (DirectoryStream<Path> children = Files.newDirectoryStream(fontFolder)) {
            for (Path child : children) {
                CellSize size = CellSize.ofDataFileName(child.getFileName().toString());
                if (size == null || !size.isSquareOrTall()) {
                    continue;
                }
                if (Files.isRegularFile(child)) {
                    sizes.add(size);
                }
            }
        }
        return sizes;
    }
}
