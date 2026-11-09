package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontHeader;
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

/**
 * The "New size" rules for one font folder: which sizes can be offered, and creating the size file.
 * The folder's shape is taken from its existing {@code <width>x<height>.txt} files; {@code .guides.txt}
 * files are not size files.
 */
public final class NewSizePlan {

    /** One entry of the size pick list. Taken sizes are listed so they can be shown greyed. */
    public record SizeOption(int height, int cellWidth, boolean taken) {
    }

    /** Cell proportions: square is W x W, tall is W x 2W. */
    enum Shape { SQUARE, TALL }

    private static final int STEP = 8;
    private static final int MAX_HEIGHT = 64;

    private NewSizePlan() {
    }

    /**
     * Checks a cell height for the chosen shape.
     *
     * @param height the height the font file is named after
     * @param shape the cell proportions
     * @return null when the size is fine, otherwise a user-facing message
     */
    static String sizeError(int height, Shape shape) {
        if (shape == Shape.SQUARE) {
            boolean valid = height >= STEP && height <= MAX_HEIGHT && height % STEP == 0;
            return valid ? null : "Square heights are 8, 16, 24, up to 64";
        }
        boolean valid = height >= 2 * STEP && height <= MAX_HEIGHT && height % (2 * STEP) == 0;
        return valid ? null : "Tall heights are 16, 32, 48, or 64";
    }

    /** Returns the cell width: the height for square cells, half of it for tall cells. */
    static int cellWidth(int height, Shape shape) {
        return shape == Shape.SQUARE ? height : height / 2;
    }

    /** Returns SQUARE when the cell is as wide as it is high, otherwise TALL. */
    static Shape shapeOf(int cellWidth, int cellHeight) {
        return cellWidth == cellHeight ? Shape.SQUARE : Shape.TALL;
    }

    /**
     * Lists every size the folder's shape allows, in ascending height order.
     *
     * @param fontFolder a font folder holding {@code <width>x<height>.txt} files
     * @return the options, or an empty list when the folder has no readable size file to take the shape from
     * @throws IllegalStateException if the size files disagree about the shape
     * @throws IOException if the folder cannot be read
     */
    public static List<SizeOption> options(Path fontFolder) throws IOException {
        Shape shape = shapeOfFolder(fontFolder);
        List<SizeOption> options = new ArrayList<>();
        if (shape == null) {
            return options;
        }
        Set<CellSize> takenSizes = takenSizes(fontFolder);
        for (int height = STEP; height <= MAX_HEIGHT; height += STEP) {
            if (sizeError(height, shape) == null) {
                int cellWidth = cellWidth(height, shape);
                options.add(new SizeOption(height, cellWidth, takenSizes.contains(new CellSize(cellWidth, height))));
            }
        }
        return options;
    }

    /**
     * Creates {@code <width>x<height>.txt} containing {@code cell WxH}. An existing file is never touched.
     *
     * @param fontFolder a font folder holding at least one readable size file
     * @param height the new size's height, which with the folder's shape gives the file name
     * @throws IllegalArgumentException if the height does not fit the folder's shape
     * @throws IllegalStateException if the folder has no readable size file, or the files disagree about the shape
     * @throws java.nio.file.FileAlreadyExistsException if the size is already taken
     * @throws IOException if the file cannot be written
     */
    public static void create(Path fontFolder, int height) throws IOException {
        Shape shape = shapeOfFolder(fontFolder);
        if (shape == null) {
            throw new IllegalStateException("The folder has no size file to take the shape from");
        }
        String problem = sizeError(height, shape);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }
        CellSize size = new CellSize(cellWidth(height, shape), height);
        Path target = fontFolder.resolve(size.dataFileName());
        String content = "cell " + size + "\n";
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

    private static Set<CellSize> takenSizes(Path fontFolder) throws IOException {
        Set<CellSize> sizes = new HashSet<>();
        for (Path file : sizeFiles(fontFolder)) {
            sizes.add(CellSize.ofDataFileName(file.getFileName().toString()));
        }
        return sizes;
    }

    private static List<Path> sizeFiles(Path fontFolder) throws IOException {
        List<Path> sizeFiles = new ArrayList<>();
        try (DirectoryStream<Path> children = Files.newDirectoryStream(fontFolder)) {
            for (Path child : children) {
                if (Files.isRegularFile(child) && CellSize.ofDataFileName(child.getFileName().toString()) != null) {
                    sizeFiles.add(child);
                }
            }
        }
        return sizeFiles;
    }

    // Null when the file has no valid cell line.
    private static Shape shapeOfFile(Path file) throws IOException {
        CellSize cell = FontHeader.read(file);
        return cell == null ? null : shapeOf(cell.width(), cell.height());
    }
}
