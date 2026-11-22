package com.github.halfdeadgames.asciigdx.skindesigner.document;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.github.halfdeadgames.asciigdx.ui.Skin;
import com.github.halfdeadgames.asciigdx.ui.SkinFileException;
import com.github.halfdeadgames.asciigdx.ui.SkinFileFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A skin file open for editing: the parsed tree, the skin built from it, and whole-text undo and
 * redo. Free of GL and {@code Gdx.app} calls, so it works headless.
 */
public final class SkinDocument {

    private static final int MAX_UNDO_STEPS = 100;
    private static final char BYTE_ORDER_MARK = '﻿';

    private FileHandle file;
    private boolean bundledDefault;
    private JsonValue tree;
    private Skin skin;
    private String savedText;
    private Runnable changeListener;
    private final Deque<String> undoStack = new ArrayDeque<>();
    private final Deque<String> redoStack = new ArrayDeque<>();

    private SkinDocument(FileHandle file, boolean bundledDefault, JsonValue tree, Skin skin) {
        this.file = file;
        this.bundledDefault = bundledDefault;
        this.tree = tree;
        this.skin = skin;
        this.savedText = SkinFileFormatter.format(tree);
    }

    /**
     * Opens a skin file.
     *
     * @param file the skin file, read as UTF-8; must not be {@code null}
     * @param bundledDefault whether the file is the bundled default skin, which is not saved over
     * @return the document, with nothing to undo and no unsaved changes
     * @throws SkinFileException if the file is not a valid skin file
     * @throws NullPointerException if {@code file} is {@code null}
     */
    public static SkinDocument open(FileHandle file, boolean bundledDefault) {
        Objects.requireNonNull(file, "file");
        String text = withoutByteOrderMark(file.readString("UTF-8"));
        // Validates first, so syntax and format errors come as SkinFileException.
        Skin.fromJson(text);
        JsonValue tree = parse(text);
        return new SkinDocument(file, bundledDefault, tree,
                Skin.fromJson(SkinFileFormatter.format(tree)));
    }

    /** Returns the skin built from the current tree. */
    public Skin getSkin() {
        return skin;
    }

    /** Returns the current tree. Callers read it only; changes go through {@link #apply}. */
    public JsonValue getTree() {
        return tree;
    }

    /** Returns the file the document was opened from or last saved to. */
    public FileHandle getFile() {
        return file;
    }

    /** Returns whether the file is the bundled default skin. */
    public boolean isBundledDefault() {
        return bundledDefault;
    }

    /** Returns whether the current text differs from the last saved text. */
    public boolean hasUnsavedChanges() {
        return !toJson().equals(savedText);
    }

    /**
     * Changes the tree and rebuilds the skin. Saves an undo snapshot first. If the edit, the
     * writer, or the rebuild fails, the tree and skin stay as they were, no snapshot is kept, and
     * the exception is rethrown. On success the redo stack is cleared and the change listener is
     * called.
     *
     * @param edit the change to make to the tree; must not be {@code null}
     * @throws SkinFileException if the edited tree is not a valid skin
     * @throws IllegalArgumentException if the edited tree cannot be formatted as JSON
     */
    public void apply(Consumer<JsonValue> edit) {
        Objects.requireNonNull(edit, "edit");
        String snapshot = toJson();
        Skin rebuiltSkin;
        try {
            edit.accept(tree);
            rebuiltSkin = Skin.fromJson(SkinFileFormatter.format(tree));
        } catch (RuntimeException failure) {
            tree = parse(snapshot);
            throw failure;
        }
        skin = rebuiltSkin;
        pushLimited(undoStack, snapshot);
        redoStack.clear();
        notifyChange();
    }

    /** Returns whether {@link #undo} has a step to go back to. */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /** Returns whether {@link #redo} has a step to go forward to. */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Goes back one step.
     *
     * @throws IllegalStateException if there is nothing to undo
     */
    public void undo() {
        if (undoStack.isEmpty()) {
            throw new IllegalStateException("Nothing to undo");
        }
        String current = toJson();
        restore(undoStack.peekFirst());
        undoStack.removeFirst();
        redoStack.addFirst(current);
        notifyChange();
    }

    /**
     * Goes forward one step.
     *
     * @throws IllegalStateException if there is nothing to redo
     */
    public void redo() {
        if (redoStack.isEmpty()) {
            throw new IllegalStateException("Nothing to redo");
        }
        String current = toJson();
        restore(redoStack.peekFirst());
        redoStack.removeFirst();
        pushLimited(undoStack, current);
        notifyChange();
    }

    /** Returns the exact text a save writes. */
    public String toJson() {
        return SkinFileFormatter.format(tree);
    }

    /**
     * Records that the current text was saved to a file.
     *
     * @param file the file written; must not be {@code null}
     */
    public void markSaved(FileHandle file) {
        this.file = Objects.requireNonNull(file, "file");
        bundledDefault = false;
        savedText = toJson();
        notifyChange();
    }

    /**
     * Sets the listener called after every change, undo, redo, and save.
     *
     * @param listener the listener, or {@code null} for none
     */
    public void setChangeListener(Runnable listener) {
        changeListener = listener;
    }

    private void restore(String text) {
        JsonValue restoredTree = parse(text);
        Skin restoredSkin = Skin.fromJson(SkinFileFormatter.format(restoredTree));
        tree = restoredTree;
        skin = restoredSkin;
    }

    private void notifyChange() {
        if (changeListener != null) {
            changeListener.run();
        }
    }

    private static void pushLimited(Deque<String> stack, String text) {
        stack.addFirst(text);
        if (stack.size() > MAX_UNDO_STEPS) {
            stack.removeLast();
        }
    }

    private static JsonValue parse(String text) {
        return new JsonReader().parse(text);
    }

    private static String withoutByteOrderMark(String text) {
        if (!text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK) {
            return text.substring(1);
        }
        return text;
    }
}
