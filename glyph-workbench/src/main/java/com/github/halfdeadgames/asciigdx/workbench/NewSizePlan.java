package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.NewFontInput.Shape;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The "New size" rules for one font folder: which sizes can be offered, and creating the size file.
 * The folder's shape is taken from its existing {@code <height>.txt} files; {@code .guides.txt}
 * files are not size files.
 */
final class NewSizePlan {

    /** One entry of the size pick list. Taken sizes are listed so they can be shown greyed. */
    record SizeOption(int height, int cellWidth, boolean taken) {
    }

    private static final Pattern SIZE_FILE = Pattern.compile("(\\d{1,4})\\.txt");
    private static final Pattern CELL = Pattern.compile("cell (\\d+)x(\\d+)");
    private static final int BYTE_ORDER_MARK = 0xFEFF;
    private static final int STEP = 8;
    private static final int MAX_HEIGHT = 64;

    private NewSizePlan() {
    }

    /**
     * Lists every size the folder's shape allows, in ascending height order.
     *
     * @param fontFolder a font folder holding {@code <height>.txt} files
     * @return the options, or an empty list when the folder has no readable size file to take the shape from
     * @throws IllegalStateException if the size files disagree about the shape
     * @throws IOException if the folder cannot be read
     */
    static List<SizeOption> options(Path fontFolder) throws IOException {
        Shape shape = shapeOfFolder(fontFolder);
        List<SizeOption> options = new ArrayList<>();
        if (shape == null) {
            return options;
        }
        Set<Integer> takenHeights = takenHeights(fontFolder);
        for (int height = STEP; height <= MAX_HEIGHT; height += STEP) {
            if (NewFontInput.sizeError(height, shape) == null) {
                options.add(new SizeOption(height, NewFontInput.cellWidth(height, shape), takenHeights.contains(height)));
            }
        }
        return options;
    }

    /**
     * Creates {@code <height>.txt} containing {@code cell WxH}. An existing file is never touched.
     *
     * @param fontFolder a font folder holding at least one readable size file
     * @param height the new size, which names the file
     * @throws IllegalArgumentException if the height does not fit the folder's shape
     * @throws IllegalStateException if the folder has no readable size file, or the files disagree about the shape
     * @throws java.nio.file.FileAlreadyExistsException if the size is already taken
     * @throws IOException if the file cannot be written
     */
    static void create(Path fontFolder, int height) throws IOException {
        Shape shape = shapeOfFolder(fontFolder);
        if (shape == null) {
            throw new IllegalStateException("The folder has no size file to take the shape from");
        }
        String problem = NewFontInput.sizeError(height, shape);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        Path target = fontFolder.resolve(height + ".txt");
        String content = "cell " + NewFontInput.cellWidth(height, shape) + "x" + height + "\n";
        Files.writeString(target, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }

    // Null when no size file has a readable cell line.
    private static Shape shapeOfFolder(Path fontFolder) throws IOException {
        Shape found = null;
        for (Path file : sizeFiles(fontFolder)) {
            Shape shape = shapeOfFile(file);
            if (shape == null) {
                continue;
            }
            if (found != null && found != shape) {
                throw new IllegalStateException("The folder mixes square and tall sizes");
            }
            found = shape;
        }
        return found;
    }

    private static Set<Integer> takenHeights(Path fontFolder) throws IOException {
        Set<Integer> heights = new HashSet<>();
        for (Path file : sizeFiles(fontFolder)) {
            Matcher matcher = SIZE_FILE.matcher(file.getFileName().toString());
            if (matcher.matches()) {
                heights.add(Integer.parseInt(matcher.group(1)));
            }
        }
        return heights;
    }

    private static List<Path> sizeFiles(Path fontFolder) throws IOException {
        List<Path> sizeFiles = new ArrayList<>();
        try (DirectoryStream<Path> children = Files.newDirectoryStream(fontFolder)) {
            for (Path child : children) {
                if (Files.isRegularFile(child) && SIZE_FILE.matcher(child.getFileName().toString()).matches()) {
                    sizeFiles.add(child);
                }
            }
        }
        return sizeFiles;
    }

    // Null when the file has no valid cell line.
    private static Shape shapeOfFile(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            while (line != null && isSkippable(line)) {
                line = reader.readLine();
            }
            if (line == null) {
                return null;
            }
            if (!line.isEmpty() && line.charAt(0) == BYTE_ORDER_MARK) {
                line = line.substring(1);
            }
            Matcher matcher = CELL.matcher(line.strip());
            if (!matcher.matches()) {
                return null;
            }
            return NewFontInput.shapeOf(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
        }
    }

    private static boolean isSkippable(String line) {
        String content = line.isEmpty() || line.charAt(0) != BYTE_ORDER_MARK ? line : line.substring(1);
        return content.isBlank() || content.equals("#") || content.startsWith("# ");
    }
}
