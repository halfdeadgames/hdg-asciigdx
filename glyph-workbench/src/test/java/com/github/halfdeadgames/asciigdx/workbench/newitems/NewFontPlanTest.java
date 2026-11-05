package com.github.halfdeadgames.asciigdx.workbench.newitems;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.halfdeadgames.asciigdx.workbench.FontDiscovery;
import com.github.halfdeadgames.asciigdx.workbench.newitems.NewFontInput.Shape;
import com.github.halfdeadgames.asciigdx.workbench.newitems.NewSizePlan.SizeOption;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.FontDataParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NewFontPlanTest {

    @TempDir
    Path root;

    @Test
    void squareFontGetsFolderAndSizeFile() throws IOException {
        Path folder = NewFontPlan.create(root, "demo", Shape.SQUARE, 16);
        assertEquals(root.resolve("demo"), folder);
        assertTrue(Files.isDirectory(folder));
        assertEquals("cell 16x16\n", read(folder.resolve("16.txt")));
    }

    @Test
    void tallFontGetsHalfWidthCell() throws IOException {
        Path folder = NewFontPlan.create(root, "demo", Shape.TALL, 32);
        assertEquals("cell 16x32\n", read(folder.resolve("32.txt")));
    }

    @Test
    void onlyTheSizeFileIsCreated() throws IOException {
        Path folder = NewFontPlan.create(root, "demo", Shape.SQUARE, 8);
        assertFalse(Files.exists(folder.resolve("rules.md")));
        try (Stream<Path> children = Files.list(folder)) {
            assertEquals(List.of(folder.resolve("8.txt")), children.toList());
        }
    }

    @Test
    void createdFolderIsReadableBySizePlan() throws IOException {
        Path folder = NewFontPlan.create(root, "demo", Shape.TALL, 32);
        List<SizeOption> options = NewSizePlan.options(folder);
        assertEquals(List.of(16, 32, 48, 64), options.stream().map(SizeOption::height).toList());
        assertFalse(options.get(0).taken());
        assertTrue(options.get(1).taken());
    }

    @Test
    void badNameIsRefusedAndNothingIsCreated() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> NewFontPlan.create(root, "Bad Name", Shape.SQUARE, 8));
        assertThrows(IllegalArgumentException.class, () -> NewFontPlan.create(root, "", Shape.SQUARE, 8));
        assertThrows(IllegalArgumentException.class, () -> NewFontPlan.create(root, "con", Shape.SQUARE, 8));
        try (Stream<Path> children = Files.list(root)) {
            assertEquals(0, children.count());
        }
    }

    @Test
    void duplicateNameIsRefusedAndKeepsTheFolder() throws IOException {
        NewFontPlan.create(root, "demo", Shape.SQUARE, 8);
        assertThrows(IllegalArgumentException.class, () -> NewFontPlan.create(root, "demo", Shape.TALL, 16));
        assertEquals("cell 8x8\n", read(root.resolve("demo").resolve("8.txt")));
        assertFalse(Files.exists(root.resolve("demo").resolve("16.txt")));
    }

    @Test
    void existingFolderDifferingOnlyInCaseCountsAsDuplicate() throws IOException {
        Files.createDirectory(root.resolve("Foo"));
        assertThrows(IllegalArgumentException.class, () -> NewFontPlan.create(root, "foo", Shape.SQUARE, 8));
        assertFalse(Files.exists(root.resolve("Foo").resolve("8.txt")));
    }

    @Test
    void existingPlainFileWithThatNameIsLeftUntouched() throws IOException {
        Path blocker = root.resolve("demo");
        write(blocker, "keep me");
        assertThrows(IllegalArgumentException.class, () -> NewFontPlan.create(root, "demo", Shape.SQUARE, 8));
        assertTrue(Files.isRegularFile(blocker));
        assertEquals("keep me", read(blocker));
    }

    @Test
    void invalidSizeIsRefusedAndNoFolderIsCreated() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> NewFontPlan.create(root, "tall", Shape.TALL, 24));
        assertThrows(IllegalArgumentException.class, () -> NewFontPlan.create(root, "square", Shape.SQUARE, 12));
        assertFalse(Files.exists(root.resolve("tall")));
        assertFalse(Files.exists(root.resolve("square")));
    }

    @Test
    void missingFontsRootIsNotCreated() {
        Path missing = root.resolve("no-such-root");
        assertThrows(NoSuchFileException.class, () -> NewFontPlan.create(missing, "demo", Shape.SQUARE, 8));
        assertFalse(Files.exists(missing));
    }

    @Test
    void tryCreateReportsTheFolderOnSuccess() throws IOException {
        NewFontPlan.Outcome outcome = NewFontPlan.tryCreate(root, "demo", Shape.SQUARE, 16);
        assertTrue(outcome.created());
        assertEquals(root.resolve("demo"), outcome.folder());
        assertEquals("", outcome.problem());
        assertEquals("cell 16x16\n", read(root.resolve("demo").resolve("16.txt")));
    }

    @Test
    void tryCreateSendsADuplicateBackToTheNamePage() throws IOException {
        Files.createDirectory(root.resolve("demo"));
        NewFontPlan.Outcome outcome = NewFontPlan.tryCreate(root, "demo", Shape.SQUARE, 8);
        assertFalse(outcome.created());
        assertTrue(outcome.nameProblem());
        assertFalse(outcome.problem().isEmpty());
    }

    @Test
    void tryCreateSendsAPlainFileWithThatNameBackToTheNamePage() throws IOException {
        write(root.resolve("demo"), "keep me");
        NewFontPlan.Outcome outcome = NewFontPlan.tryCreate(root, "demo", Shape.SQUARE, 8);
        assertTrue(outcome.nameProblem());
        assertEquals("A font with that name already exists", outcome.problem());
        assertEquals("keep me", read(root.resolve("demo")));
    }

    @Test
    void tryCreateSendsABadNameBackToTheNamePage() {
        NewFontPlan.Outcome outcome = NewFontPlan.tryCreate(root, "Bad Name", Shape.SQUARE, 8);
        assertFalse(outcome.created());
        assertTrue(outcome.nameProblem());
    }

    @Test
    void tryCreateKeepsOtherFailuresOnTheSizePage() {
        NewFontPlan.Outcome missingRoot = NewFontPlan.tryCreate(root.resolve("no-such-root"), "demo", Shape.SQUARE, 8);
        assertFalse(missingRoot.created());
        assertFalse(missingRoot.nameProblem());
        assertTrue(missingRoot.problem().startsWith("Cannot create font"));
        NewFontPlan.Outcome noRoot = NewFontPlan.tryCreate(null, "demo", Shape.SQUARE, 8);
        assertFalse(noRoot.created());
        assertFalse(noRoot.nameProblem());
        assertEquals("No fonts folder is set", noRoot.problem());
    }

    @Test
    void folderNamesCountsEveryChildAsTaken() throws IOException {
        Files.createDirectory(root.resolve("one"));
        write(root.resolve("notes.txt"), "x");
        assertEquals(List.of("notes.txt", "one"), NewFontPlan.folderNames(root).stream().sorted().toList());
        assertEquals(List.of("notes.txt", "one"), NewFontDialogs.foldersIn(root).stream().sorted().toList());
        assertEquals(List.of(), NewFontDialogs.foldersIn(null));
    }

    @Test
    void aNewFontIsListedByFontDiscoveryWithoutRulesOrGuides() throws IOException {
        NewFontPlan.create(root, "demo", Shape.TALL, 16);
        assertEquals(List.of(new FontDiscovery.FontChoice("demo", 16)), FontDiscovery.discover(root));
    }

    @Test
    void theCreatedSizeFileOpensAsAnEmptyFont() throws IOException {
        Path folder = NewFontPlan.create(root, "demo", Shape.TALL, 16);
        assertDoesNotThrow(() -> FontDataParser.parse(read(folder.resolve("16.txt"))));
    }

    private static void write(Path file, String content) throws IOException {
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
