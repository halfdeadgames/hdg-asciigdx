package com.github.halfdeadgames.asciigdx.glyph;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Measures one-time startup work that players feel as a slow start: decoding and parsing a
 * bundled glyph sheet, converting it to white glyphs with alpha, and baking a TTF into a glyph
 * sheet. Only the CPU parts that need no GL context are measured; texture upload is not.
 *
 * <p>Each invocation is one cold run ({@link Mode#SingleShotTime}), so the numbers include
 * the work a first call pays for. Run with
 * {@code .\gradlew.bat :core:jmh -PjmhIncludes=GlyphLoading}.
 *
 * <p>The sheets come from the main resources. The TTF comes from the test resources, found on
 * the classpath or, failing that, under {@code src/test/resources}.
 */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
public class GlyphLoadingBenchmark {

    private static final String TEST_FONT_FOLDER =
            "com/github/halfdeadgames/asciigdx/fonts/dungeon-mode/";
    private static final String TTF_NAME = "dungeon-mode.ttf";
    private static final String TEST_RESOURCES_ROOT = "src/test/resources/";
    private static final String TEST_RESOURCES_ROOT_FROM_REPOSITORY = "core/" + TEST_RESOURCES_ROOT;
    private static final int FALLBACK_CODE_POINT = '?';
    private static final int UNUSED_KEY_RGBA8888 = 0;

    /** A bundled sheet copied out of the benchmark jar, which a file handle cannot read. */
    @State(Scope.Benchmark)
    public static class SheetInput {

        @Param({"SQUARE_8X8", "SQUARE_16X16", "TALL_8X16", "TALL_16X32"})
        public SaiksisttSize sheetSize;

        private Path copyFolder;
        private FileHandle sheetImage;
        private FileHandle characterList;

        @Setup
        public void copySheetOutOfJar() throws IOException {
            loadNatives();
            copyFolder = Files.createTempDirectory("glyph-loading-benchmark");
            sheetImage = copyResource(sheetSize.getSheetPath(), copyFolder);
            characterList = copyResource(sheetSize.getCharacterListPath(), copyFolder);
        }

        @TearDown
        public void deleteCopies() throws IOException {
            deleteFolder(copyFolder);
        }
    }

    /** The test TTF, either copied out of the benchmark jar or read from the test resources. */
    @State(Scope.Benchmark)
    public static class BakeInput {

        @Param({"8", "16", "32"})
        public int bakeCellHeight;

        private Path copyFolder;
        private FileHandle ttfFile;

        @Setup
        public void locateTtf() throws IOException {
            loadNatives();
            copyFolder = Files.createTempDirectory("glyph-loading-benchmark");
            ttfFile = findTtf(copyFolder);
        }

        @TearDown
        public void deleteCopies() throws IOException {
            deleteFolder(copyFolder);
        }
    }

    /** Decodes the PNG and parses the character list into an order and layout. */
    @Benchmark
    public int readSheet(SheetInput input) {
        GlyphSheetLoader.LoadedSheet sheet = GlyphSheetLoader.readSheet(
                input.sheetImage, input.characterList, input.sheetSize.getCellWidth(),
                input.sheetSize.getCellHeight(), FALLBACK_CODE_POINT);
        try {
            return sheet.fallbackSlot();
        } finally {
            sheet.pixmap().dispose();
        }
    }

    /** Reads the sheet, then converts every pixel to white with alpha. */
    @Benchmark
    public int readSheetAndConvertToWhiteWithAlpha(SheetInput input) {
        GlyphSheetLoader.LoadedSheet sheet = GlyphSheetLoader.readSheet(
                input.sheetImage, input.characterList, input.sheetSize.getCellWidth(),
                input.sheetSize.getCellHeight(), FALLBACK_CODE_POINT);
        Pixmap converted = GlyphSheetLoader.applyTransparencyMode(
                sheet.pixmap(), TransparencyMode.ALWAYS, UNUSED_KEY_RGBA8888);
        try {
            return converted.getWidth();
        } finally {
            converted.dispose();
        }
    }

    /** Bakes the CP437 glyphs of a TTF into a glyph sheet pixmap. */
    @Benchmark
    public int bakeTtfToSheet(BakeInput input) {
        FreeTypeGlyphBaker.BakedSheet baked =
                FreeTypeGlyphBaker.bake(input.ttfFile, input.bakeCellHeight, GlyphOrder.CP437);
        try {
            return baked.getPixmap().getWidth();
        } finally {
            baked.getPixmap().dispose();
        }
    }

    private static void loadNatives() {
        new SharedLibraryLoader().load("gdx");
        new SharedLibraryLoader().load("gdx-freetype");
    }

    private static FileHandle copyResource(String resourcePath, Path folder) throws IOException {
        try (InputStream resource =
                GlyphLoadingBenchmark.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (resource == null) {
                throw new IllegalStateException("Resource not on the classpath: " + resourcePath);
            }
            Path copy = folder.resolve(Path.of(resourcePath).getFileName());
            Files.copy(resource, copy, StandardCopyOption.REPLACE_EXISTING);
            return new FileHandle(copy.toFile());
        }
    }

    private static FileHandle findTtf(Path folder) throws IOException {
        String resourcePath = TEST_FONT_FOLDER + TTF_NAME;
        if (GlyphLoadingBenchmark.class.getClassLoader().getResource(resourcePath) != null) {
            return copyResource(resourcePath, folder);
        }
        for (String root : new String[] {TEST_RESOURCES_ROOT, TEST_RESOURCES_ROOT_FROM_REPOSITORY}) {
            File candidate = new File(root + resourcePath);
            if (candidate.isFile()) {
                return new FileHandle(candidate);
            }
        }
        throw new IllegalStateException(
                "Cannot find " + TTF_NAME + " on the classpath or under " + TEST_RESOURCES_ROOT);
    }

    private static void deleteFolder(Path folder) throws IOException {
        if (folder == null) {
            return;
        }
        try (var entries = Files.list(folder)) {
            for (Path entry : (Iterable<Path>) entries::iterator) {
                deleteOrDeleteOnExit(entry);
            }
        }
        deleteOrDeleteOnExit(folder);
    }

    /** FreeType keeps the TTF memory-mapped, and Windows refuses to delete a mapped file. */
    private static void deleteOrDeleteOnExit(Path path) {
        try {
            Files.delete(path);
        } catch (IOException stillInUse) {
            path.toFile().deleteOnExit();
        }
    }
}
