package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphListFile;
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
     * What an attempt to create a font came to. {@code folder} is set on success. Otherwise
     * {@code problem} says why, and {@code nameProblem} tells the dialog to go back to the name
     * page instead of staying on the glyph page.
     */
    record Outcome(Path folder, String problem, boolean nameProblem) {

        boolean created() {
            return folder != null;
        }
    }

    /** The glyph list of a font that starts with the full Code Page 437 slot layout. */
    private static final String CP437_GLYPH_LIST = "cp437\n";

    private NewFontPlan() {
    }

    /** Like {@link #create}, but every failure becomes an {@link Outcome} the dialog can show. */
    static Outcome tryCreate(Path fontsRoot, String name, CellSize size, NewFontGlyphs glyphs) {
        if (fontsRoot == null) {
            return new Outcome(null, "No fonts folder is set", false);
        }
        try {
            return new Outcome(create(fontsRoot, name, size, glyphs), "", false);
        } catch (FileAlreadyExistsException e) {
            return new Outcome(null, "A font named " + name + " already exists", true);
        } catch (IllegalArgumentException e) {
            return new Outcome(null, e.getMessage(), true);
        } catch (IOException | RuntimeException e) {
            return new Outcome(null, "Cannot create font: " + e.getMessage(), false);
        }
    }

    /**
     * Creates {@code <fontsRoot>/<name>/<width>x<height>.txt} containing {@code cell WxH}, plus a
     * {@code glyphs.txt} listing {@code cp437} when {@code glyphs} is {@link NewFontGlyphs#CP437};
     * for {@link NewFontGlyphs#EMPTY} no {@code glyphs.txt} is written. Nothing existing is
     * overwritten, and a failed write leaves no new folder behind.
     *
     * @param fontsRoot the existing folder that holds all font folders
     * @param name the font folder name
     * @param size the first size's cell, which gives the file name
     * @param glyphs which glyph slots the font starts with
     * @return the created font folder
     * @throws IllegalArgumentException if the name or the size is not acceptable
     * @throws java.nio.file.NoSuchFileException if the fonts root does not exist
     * @throws java.nio.file.FileAlreadyExistsException if something already has that name
     * @throws IOException if the folder or file cannot be written
     */
    static Path create(Path fontsRoot, String name, CellSize size, NewFontGlyphs glyphs) throws
            IOException {
        String nameProblem = NewFontInput.nameError(name, folderNames(fontsRoot));
        if (nameProblem != null) {
            throw new IllegalArgumentException(nameProblem);
        }
        String sizeProblem = NewFontInput.sizeError(size);
        if (sizeProblem != null) {
            throw new IllegalArgumentException(sizeProblem);
        }
        Path folder = Files.createDirectory(fontsRoot.resolve(name));
        Path sizeFile = folder.resolve(size.dataFileName());
        Path glyphListFile = folder.resolve(GlyphListFile.FILE_NAME);
        try {
            NewSizeFile.writeNew(sizeFile, size);
            if (glyphs == NewFontGlyphs.CP437) {
                writeNewFile(glyphListFile, CP437_GLYPH_LIST);
            }
        } catch (IOException | RuntimeException failure) {
            removeFolder(folder, List.of(sizeFile, glyphListFile), failure);
            throw failure;
        }
        return folder;
    }

    private static void writeNewFile(Path file, String text) throws IOException {
        Files.writeString(file, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }

    /**
     * Best-effort cleanup after a failed create; anything that cannot be removed is attached to the
     * failure.
     */
    private static void removeFolder(Path folder, List<Path> files, Exception failure) {
        try {
            for (Path file : files) {
                Files.deleteIfExists(file);
            }
            Files.delete(folder);
        } catch (IOException | RuntimeException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }

    /**
     * The names of everything directly inside {@code fontsRoot}; a plain file blocks a name as much
     * as a folder.
     */
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
