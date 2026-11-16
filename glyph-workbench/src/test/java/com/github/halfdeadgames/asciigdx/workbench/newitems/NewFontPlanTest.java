package com.github.halfdeadgames.asciigdx.workbench.newitems;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.FontDiscovery;
import com.github.halfdeadgames.asciigdx.workbench.newitems.NewSizePlan.SizeOption;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.CellSize;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataParser;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.GlyphListFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NewFontPlanTest {

    private static final NewFontGlyphs CP437 = NewFontGlyphs.CP437;

    @TempDir
    Path root;

    @Test
    void squareFontGetsFolderAndSizeFile() throws IOException {
        Path folder = NewFontPlan.create(root, "demo", new CellSize(16, 16), CP437);
        assertEquals(root.resolve("demo"), folder);
        assertTrue(Files.isDirectory(folder));
        assertEquals("cell 16x16\n", read(folder.resolve("16x16.txt")));
    }

    @Test
    void tallFontGetsHalfWidthCell() throws IOException {
        Path folder = NewFontPlan.create(root, "demo", new CellSize(16, 32), CP437);
        assertEquals("cell 16x32\n", read(folder.resolve("16x32.txt")));
    }

    @Test
    void onlyTheSizeFileAndTheGlyphListAreCreated() throws IOException {
        Path folder = NewFontPlan.create(root, "demo", new CellSize(8, 8), CP437);
        assertFalse(Files.exists(folder.resolve("notes.md")));
        String[] children = folder.toFile().list();
        Arrays.sort(children);
        assertArrayEquals(new String[]{"8x8.txt", GlyphListFile.FILE_NAME}, children);
    }

    @Test
    void theNewFontListsTheCp437Preset() throws IOException {
        Path folder = NewFontPlan.create(root, "demo", new CellSize(8, 8), CP437);
        assertEquals("cp437\n", read(folder.resolve(GlyphListFile.FILE_NAME)));
        assertTrue(GlyphListFile.load(folder.toFile()).usesCp437());
    }

    @Test
    void anEmptyFontWritesNoGlyphList() throws IOException {
        Path folder = NewFontPlan.create(root, "demo", new CellSize(8, 8), NewFontGlyphs.EMPTY);
        assertFalse(Files.exists(folder.resolve(GlyphListFile.FILE_NAME)));
        assertEquals("cell 8x8\n", read(folder.resolve("8x8.txt")));
        assertArrayEquals(new String[]{"8x8.txt"}, folder.toFile().list());
    }

    @Test
    void aRefusedCreateLeavesNoFolderForEitherChoice() throws IOException {
        for (NewFontGlyphs glyphs : NewFontGlyphs.values()) {
            assertFalse(
                    NewFontPlan.tryCreate(root, "Bad Name", new CellSize(8, 8), glyphs).created());
            assertFalse(
                    NewFontPlan.tryCreate(root, "wide", new CellSize(129, 16), glyphs).created());
            assertFalse(
                    NewFontPlan.tryCreate(root.resolve("no-such-root"), "demo", new CellSize(8, 8),
                            glyphs).created());
        }
        try (Stream<Path> children = Files.list(root)) {
            assertEquals(0, children.count());
        }
    }

    @Test
    void createdFolderIsReadableBySizePlan() throws IOException {
        Path folder = NewFontPlan.create(root, "demo", new CellSize(16, 32), CP437);
        List<SizeOption> options = NewSizePlan.options(folder);
        assertEquals(List.of(new CellSize(8, 16), new CellSize(16, 32), new CellSize(32, 64)),
                options.stream().map(SizeOption::size).toList());
        assertFalse(options.get(0).taken());
        assertTrue(options.get(1).taken());
    }

    @Test
    void anEmptyFontIsReadableBySizePlan() throws IOException {
        Path folder = NewFontPlan.create(root, "demo", new CellSize(16, 32), NewFontGlyphs.EMPTY);
        assertTrue(NewSizePlan.options(folder).stream().anyMatch(option -> option.taken()
                && option.size().equals(new CellSize(16, 32))));
    }

    @Test
    void badNameIsRefusedAndNothingIsCreated() throws IOException {
        assertThrows(IllegalArgumentException.class,
                () -> NewFontPlan.create(root, "Bad Name", new CellSize(8, 8), CP437));
        assertThrows(IllegalArgumentException.class,
                () -> NewFontPlan.create(root, "", new CellSize(8, 8), CP437));
        assertThrows(IllegalArgumentException.class,
                () -> NewFontPlan.create(root, "con", new CellSize(8, 8), CP437));
        try (Stream<Path> children = Files.list(root)) {
            assertEquals(0, children.count());
        }
    }

    @Test
    void duplicateNameIsRefusedAndKeepsTheFolder() throws IOException {
        NewFontPlan.create(root, "demo", new CellSize(8, 8), CP437);
        assertThrows(IllegalArgumentException.class,
                () -> NewFontPlan.create(root, "demo", new CellSize(8, 16), CP437));
        assertEquals("cell 8x8\n", read(root.resolve("demo").resolve("8x8.txt")));
        assertFalse(Files.exists(root.resolve("demo").resolve("8x16.txt")));
    }

    @Test
    void existingFolderDifferingOnlyInCaseCountsAsDuplicate() throws IOException {
        Files.createDirectory(root.resolve("Foo"));
        assertThrows(IllegalArgumentException.class,
                () -> NewFontPlan.create(root, "foo", new CellSize(8, 8), CP437));
        assertFalse(Files.exists(root.resolve("Foo").resolve("8x8.txt")));
    }

    @Test
    void existingPlainFileWithThatNameIsLeftUntouched() throws IOException {
        Path blocker = root.resolve("demo");
        write(blocker, "keep me");
        assertThrows(IllegalArgumentException.class,
                () -> NewFontPlan.create(root, "demo", new CellSize(8, 8), CP437));
        assertTrue(Files.isRegularFile(blocker));
        assertEquals("keep me", read(blocker));
    }

    @Test
    void invalidSizeIsRefusedAndNoFolderIsCreated() throws IOException {
        assertThrows(IllegalArgumentException.class,
                () -> NewFontPlan.create(root, "wide", new CellSize(129, 16), CP437));
        assertThrows(IllegalArgumentException.class,
                () -> NewFontPlan.create(root, "high", new CellSize(16, 129), CP437));
        assertFalse(Files.exists(root.resolve("wide")));
        assertFalse(Files.exists(root.resolve("high")));
    }

    @Test
    void anyTypedSizeGetsItsOwnFileName() throws IOException {
        Path folder = NewFontPlan.create(root, "odd", new CellSize(7, 14), CP437);
        assertEquals("cell 7x14\n", read(folder.resolve("7x14.txt")));
    }

    @Test
    void missingFontsRootIsNotCreated() {
        Path missing = root.resolve("no-such-root");
        assertThrows(NoSuchFileException.class,
                () -> NewFontPlan.create(missing, "demo", new CellSize(8, 8), CP437));
        assertFalse(Files.exists(missing));
    }

    @Test
    void tryCreateReportsTheFolderOnSuccess() throws IOException {
        NewFontPlan.Outcome outcome =
                NewFontPlan.tryCreate(root, "demo", new CellSize(16, 16), CP437);
        assertTrue(outcome.created());
        assertEquals(root.resolve("demo"), outcome.folder());
        assertEquals("", outcome.problem());
        assertEquals("cell 16x16\n", read(root.resolve("demo").resolve("16x16.txt")));
    }

    @Test
    void tryCreateSendsADuplicateBackToTheNamePage() throws IOException {
        Files.createDirectory(root.resolve("demo"));
        NewFontPlan.Outcome outcome =
                NewFontPlan.tryCreate(root, "demo", new CellSize(8, 8), CP437);
        assertFalse(outcome.created());
        assertTrue(outcome.nameProblem());
        assertFalse(outcome.problem().isEmpty());
    }

    @Test
    void tryCreateSendsAPlainFileWithThatNameBackToTheNamePage() throws IOException {
        write(root.resolve("demo"), "keep me");
        NewFontPlan.Outcome outcome =
                NewFontPlan.tryCreate(root, "demo", new CellSize(8, 8), CP437);
        assertTrue(outcome.nameProblem());
        assertEquals("A font with that name already exists", outcome.problem());
        assertEquals("keep me", read(root.resolve("demo")));
    }

    @Test
    void tryCreateSendsABadNameBackToTheNamePage() {
        NewFontPlan.Outcome outcome =
                NewFontPlan.tryCreate(root, "Bad Name", new CellSize(8, 8), CP437);
        assertFalse(outcome.created());
        assertTrue(outcome.nameProblem());
    }

    @Test
    void tryCreateKeepsOtherFailuresOnTheGlyphPage() {
        NewFontPlan.Outcome missingRoot =
                NewFontPlan.tryCreate(
                        root.resolve("no-such-root"), "demo", new CellSize(8, 8), CP437);
        assertFalse(missingRoot.created());
        assertFalse(missingRoot.nameProblem());
        assertTrue(missingRoot.problem().startsWith("Cannot create font"));
        NewFontPlan.Outcome noRoot = NewFontPlan.tryCreate(null, "demo", new CellSize(8, 8), CP437);
        assertFalse(noRoot.created());
        assertFalse(noRoot.nameProblem());
        assertEquals("No fonts folder is set", noRoot.problem());
    }

    @Test
    void folderNamesCountsEveryChildAsTaken() throws IOException {
        Files.createDirectory(root.resolve("one"));
        write(root.resolve("notes.txt"), "x");
        assertEquals(List.of("notes.txt", "one"),
                NewFontPlan.folderNames(root).stream().sorted().toList());
        assertEquals(List.of("notes.txt", "one"),
                NewFontDialogs.foldersIn(root).stream().sorted().toList());
        assertEquals(List.of(), NewFontDialogs.foldersIn(null));
    }

    @Test
    void aNewFontIsListedByFontDiscoveryWithoutRulesOrGuides() throws IOException {
        NewFontPlan.create(root, "demo", new CellSize(8, 16), CP437);
        assertEquals(List.of(new FontDiscovery.FontChoice("demo", new CellSize(8, 16))),
                FontDiscovery.discover(root));
    }

    @Test
    void theCreatedSizeFileOpensAsAnEmptyFont() throws IOException {
        Path folder = NewFontPlan.create(root, "demo", new CellSize(8, 16), CP437);
        CellSize size = new CellSize(8, 16);
        assertDoesNotThrow(
                () -> FontDataParser.parseSizeFile(size, read(folder.resolve("8x16.txt"))));
    }

    private static void write(Path file, String content) throws IOException {
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
