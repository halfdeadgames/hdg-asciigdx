package com.github.halfdeadgames.asciigdx.sandbox.pond;

import java.util.Arrays;

/** The few lilies that are away from home, each with its position in the lily layer's map. */
public final class MovingLilies {

    private static final int START_CAPACITY = 64;

    private Lily[] lilies = new Lily[START_CAPACITY];
    private int[] columns = new int[START_CAPACITY];
    private int[] rows = new int[START_CAPACITY];
    private int size;

    /** Adds a lily at its lily-layer map position; a lily already in the set is ignored. */
    public void add(Lily lily, int lilyColumn, int lilyRow) {
        if (contains(lily)) {
            return;
        }
        growIfFull();
        lilies[size] = lily;
        columns[size] = lilyColumn;
        rows[size] = lilyRow;
        size++;
    }

    /** Advances every lily, then drops those that are home. */
    public void advance(float deltaSeconds) {
        for (int i = 0; i < size; i++) {
            lilies[i].advance(deltaSeconds);
        }
        // Backward, so the entry swapped into a removed slot was already checked.
        for (int i = size - 1; i >= 0; i--) {
            if (lilies[i].isHome()) {
                removeBySwappingLast(i);
            }
        }
    }

    /** Sends every lily home and empties the set. */
    public void resetAll() {
        for (int i = 0; i < size; i++) {
            lilies[i].reset();
            lilies[i] = null;
        }
        size = 0;
    }

    public int size() {
        return size;
    }

    public Lily lilyAt(int index) {
        return lilies[index];
    }

    public int columnAt(int index) {
        return columns[index];
    }

    public int rowAt(int index) {
        return rows[index];
    }

    private boolean contains(Lily lily) {
        for (int i = 0; i < size; i++) {
            if (lilies[i] == lily) {
                return true;
            }
        }
        return false;
    }

    private void removeBySwappingLast(int index) {
        int last = size - 1;
        lilies[index] = lilies[last];
        columns[index] = columns[last];
        rows[index] = rows[last];
        lilies[last] = null;
        size = last;
    }

    private void growIfFull() {
        if (size < lilies.length) {
            return;
        }
        int grownCapacity = lilies.length * 2;
        lilies = Arrays.copyOf(lilies, grownCapacity);
        columns = Arrays.copyOf(columns, grownCapacity);
        rows = Arrays.copyOf(rows, grownCapacity);
    }
}
