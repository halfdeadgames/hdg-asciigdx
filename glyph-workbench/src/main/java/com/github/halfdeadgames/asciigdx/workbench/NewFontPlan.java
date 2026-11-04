package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.NewFontInput.Shape;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/** The "New font" rules: creating a font folder holding its first size file. */
final class NewFontPlan {

    /**
     * What an attempt to create a font came to. {@code folder} is set on success. Otherwise {@code problem} says
     * why, and {@code nameProblem} tells the dialog to go back to the name page instead of staying on the size page.
     */
    record Outcome(Path folder, String problem, boolean nameProblem) {

        boolean created() {
            return folder != null;
        }
    }

    private NewFontPlan() {
    }

    /** Like {@link #create}, but every failure becomes an {@link Outcome} the dialog can show. */
    static Outcome tryCreate(Path fontsRoot, String name, Shape shape, int height) {
        if (fontsRoot == null) {
            return new Outcome(null, "No fonts folder is set", false);
        }
        try {
            return new Outcome(create(fontsRoot, name, shape, height), "", false);
        } catch (FileAlreadyExistsException e) {
            return new Outcome(null, "A font named " + name + " already exists", true);
        } catch (IllegalArgumentException e) {
            return new Outcome(null, e.getMessage(), true);
        } catch (IOException | RuntimeException e) {
            return new Outcome(null, "Cannot create font: " + e.getMessage(), false);
        }
    }

    /**
     * Creates {@code <fontsRoot>/<name>/<height>.txt} containing {@code cell WxH}. Nothing existing is
     * overwritten, and a failed write leaves no new folder behind.
     *
     * @param fontsRoot the existing folder that holds all font folders
     * @param name the font folder name
     * @param shape the cell proportions
     * @param height the first size, which names the file
     * @return the created font folder
     * @throws IllegalArgumentException if the name or the size is not acceptable
     * @throws java.nio.file.NoSuchFileException if the fonts root does not exist
     * @throws java.nio.file.FileAlreadyExistsException if something already has that name
     * @throws IOException if the folder or file cannot be written
     */
    static Path create(Path fontsRoot, String name, Shape shape, int height) throws IOException {
        String nameProblem = NewFontInput.nameError(name, folderNames(fontsRoot));
        if (nameProblem != null) {
            throw new IllegalArgumentException(nameProblem);
        }
        String sizeProblem = NewFontInput.sizeError(height, shape);
        if (sizeProblem != null) {
            throw new IllegalArgumentException(sizeProblem);
        }
        Path folder = Files.createDirectory(fontsRoot.resolve(name));
        Path sizeFile = folder.resolve(height + ".txt");
        String content = "cell " + NewFontInput.cellWidth(height, shape) + "x" + height + "\n";
        try {
            Files.writeString(sizeFile, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException | RuntimeException failure) {
            try {
                Files.deleteIfExists(sizeFile);
                Files.delete(folder);
            } catch (IOException | RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
        return folder;
    }

    /** The names of everything directly inside {@code fontsRoot}; a plain file blocks a name as much as a folder. */
    static List<String> folderNames(Path fontsRoot) throws IOException {
        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> children = Files.newDirectoryStream(fontsRoot)) {
            for (Path child : children) {
                names.add(child.getFileName().toString());
            }
        }
        return names;
    }
}
