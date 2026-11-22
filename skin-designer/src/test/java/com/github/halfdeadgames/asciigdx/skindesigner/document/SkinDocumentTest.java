package com.github.halfdeadgames.asciigdx.skindesigner.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonValue;
import com.github.halfdeadgames.asciigdx.ui.SkinFileException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SkinDocumentTest {

    private static final File DEFAULT_SKIN = new File("../assets/default-skin.json");
    private static final int GOLD_AS_RGBA = 0xFFD700FF;
    private static final int CHANGED_AS_RGBA = 0x112233FF;

    @TempDir
    Path temporaryFolder;

    private SkinDocument document;

    @BeforeEach
    void openDefaultSkin() {
        document = SkinDocument.open(new FileHandle(DEFAULT_SKIN), true);
    }

    private static void setGold(JsonValue tree, String value) {
        tree.get("colors").get("gold").set(value);
    }

    @Test
    void openBuildsSkinAndStartsClean() {
        assertEquals(GOLD_AS_RGBA, document.getSkin().getRgba("gold"));
        assertTrue(document.isBundledDefault());
        assertFalse(document.hasUnsavedChanges());
        assertFalse(document.canUndo());
        assertFalse(document.canRedo());
    }

    @Test
    void openRejectsInvalidText() throws IOException {
        Path broken = temporaryFolder.resolve("broken.json");
        Files.writeString(broken, "{ \"colors\": ", StandardCharsets.UTF_8);

        assertThrows(SkinFileException.class,
                () -> SkinDocument.open(new FileHandle(broken.toFile()), false));
    }

    @Test
    void applyChangesTreeAndSkin() {
        document.apply(tree -> setGold(tree, "#112233"));

        assertEquals(CHANGED_AS_RGBA, document.getSkin().getRgba("gold"));
        assertTrue(document.toJson().contains("#112233"));
        assertTrue(document.hasUnsavedChanges());
        assertTrue(document.canUndo());
    }

    @Test
    void rejectedEditRethrowsAndChangesNothing() {
        String textBefore = document.toJson();
        var skinBefore = document.getSkin();

        assertThrows(SkinFileException.class,
                () -> document.apply(tree -> setGold(tree, "notAColor")));

        assertEquals(textBefore, document.toJson());
        assertSame(skinBefore, document.getSkin());
        assertEquals(GOLD_AS_RGBA, document.getSkin().getRgba("gold"));
        assertFalse(document.canUndo());
        assertFalse(document.hasUnsavedChanges());
    }

    @Test
    void undoAndRedoRoundTrip() {
        String original = document.toJson();
        document.apply(tree -> setGold(tree, "#112233"));
        String edited = document.toJson();

        document.undo();
        assertEquals(original, document.toJson());
        assertEquals(GOLD_AS_RGBA, document.getSkin().getRgba("gold"));
        assertFalse(document.canUndo());
        assertTrue(document.canRedo());

        document.redo();
        assertEquals(edited, document.toJson());
        assertEquals(CHANGED_AS_RGBA, document.getSkin().getRgba("gold"));
        assertTrue(document.canUndo());
        assertFalse(document.canRedo());
    }

    @Test
    void applyClearsRedoStack() {
        document.apply(tree -> setGold(tree, "#112233"));
        document.undo();
        document.apply(tree -> setGold(tree, "#445566"));

        assertFalse(document.canRedo());
    }

    @Test
    void undoAndRedoWithNothingToDoThrow() {
        assertThrows(IllegalStateException.class, document::undo);
        assertThrows(IllegalStateException.class, document::redo);
    }

    @Test
    void unsavedFlagFollowsSavedText() {
        document.apply(tree -> setGold(tree, "#112233"));
        assertTrue(document.hasUnsavedChanges());

        document.undo();
        assertFalse(document.hasUnsavedChanges());

        document.redo();
        assertTrue(document.hasUnsavedChanges());
    }

    @Test
    void markSavedClearsFlagAndUpdatesFile() {
        document.apply(tree -> setGold(tree, "#112233"));
        var savedFile = new FileHandle(temporaryFolder.resolve("saved.json").toFile());

        document.markSaved(savedFile);

        assertFalse(document.hasUnsavedChanges());
        assertFalse(document.isBundledDefault());
        assertSame(savedFile, document.getFile());

        document.undo();
        assertTrue(document.hasUnsavedChanges());
    }

    @Test
    void undoStackKeepsAtMostOneHundredSteps() {
        for (int step = 0; step < 105; step++) {
            String color = String.format("#%06X", step + 1);
            document.apply(tree -> setGold(tree, color));
        }

        int undoCount = 0;
        while (document.canUndo()) {
            document.undo();
            undoCount++;
        }

        assertEquals(100, undoCount);
    }

    @Test
    void oldestSnapshotsAreTheOnesDropped() {
        var textsAfterApply = new ArrayList<String>();
        for (int step = 0; step < 105; step++) {
            String color = String.format("#%06X", step + 1);
            document.apply(tree -> setGold(tree, color));
            textsAfterApply.add(document.toJson());
        }

        while (document.canUndo()) {
            document.undo();
        }

        assertEquals(textsAfterApply.get(4), document.toJson());
        assertEquals(0x000005FF, document.getSkin().getRgba("gold"));
    }

    @Test
    void openSkipsLeadingByteOrderMark() throws IOException {
        String defaultText = Files.readString(DEFAULT_SKIN.toPath(), StandardCharsets.UTF_8);
        Path withMark = temporaryFolder.resolve("bom.json");
        Files.writeString(withMark, "﻿" + defaultText, StandardCharsets.UTF_8);

        var opened = SkinDocument.open(new FileHandle(withMark.toFile()), false);

        assertEquals(GOLD_AS_RGBA, opened.getSkin().getRgba("gold"));
        assertFalse(opened.hasUnsavedChanges());
        assertFalse(opened.canUndo());
    }

    @Test
    void editThatChangesTreeThenThrowsIsRolledBack() {
        document.apply(tree -> setGold(tree, "#112233"));
        document.undo();
        String textBefore = document.toJson();
        var skinBefore = document.getSkin();

        assertThrows(IllegalStateException.class, () -> document.apply(tree -> {
            setGold(tree, "#445566");
            throw new IllegalStateException("edit failed");
        }));

        assertEquals(textBefore, document.toJson());
        assertSame(skinBefore, document.getSkin());
        assertFalse(document.canUndo());
        assertTrue(document.canRedo());
        assertFalse(document.hasUnsavedChanges());
    }

    @Test
    void listenerRunsOnApplyUndoRedoAndSave() {
        var calls = new AtomicInteger();
        document.setChangeListener(calls::incrementAndGet);

        document.apply(tree -> setGold(tree, "#112233"));
        assertEquals(1, calls.get());
        document.undo();
        assertEquals(2, calls.get());
        document.redo();
        assertEquals(3, calls.get());
        document.markSaved(new FileHandle(temporaryFolder.resolve("saved.json").toFile()));
        assertEquals(4, calls.get());

        assertThrows(SkinFileException.class,
                () -> document.apply(tree -> setGold(tree, "notAColor")));
        assertEquals(4, calls.get());

        document.setChangeListener(null);
        document.undo();
        assertEquals(4, calls.get());
    }
}
