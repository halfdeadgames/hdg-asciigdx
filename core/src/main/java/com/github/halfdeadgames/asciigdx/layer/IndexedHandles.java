package com.github.halfdeadgames.asciigdx.layer;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;

/**
 * An ordered list of cell handles with an index by cell position, so a handle is found without a
 * scan. The index is kept in step by {@link #add} and {@link #removeAt}; after handles are moved or
 * dropped in bulk, {@link #rebuildIndex()} restores it.
 */
final class IndexedHandles {

    private final Array<CellHandle> handles = new Array<>();
    private final LongMap<CellHandle> byPosition = new LongMap<>();

    /** The live list, in creation order. Not a copy. */
    Array<CellHandle> list() {
        return handles;
    }

    int size() {
        return handles.size;
    }

    CellHandle get(int index) {
        return handles.get(index);
    }

    /** The handle at a cell, or null if there is none. */
    CellHandle at(int column, int row) {
        return byPosition.get(keyOf(column, row));
    }

    void add(CellHandle handle) {
        handles.add(handle);
        byPosition.put(keyOf(handle.getColumn(), handle.getRow()), handle);
    }

    void removeAt(int index) {
        CellHandle removed = handles.removeIndex(index);
        byPosition.remove(keyOf(removed.getColumn(), removed.getRow()));
    }

    void remove(CellHandle handle) {
        removeAt(handles.indexOf(handle, true));
    }

    /** Puts {@code replacement}, which must sit at the same cell, where {@code old} was in the list. */
    void replace(CellHandle old, CellHandle replacement) {
        handles.set(handles.indexOf(old, true), replacement);
        byPosition.put(keyOf(replacement.getColumn(), replacement.getRow()), replacement);
    }

    /** Drops and detaches handles outside a grid of the given size; call {@link #rebuildIndex()} afterward. */
    void dropOutside(int columns, int rows) {
        for (int i = handles.size - 1; i >= 0; i--) {
            CellHandle handle = handles.get(i);
            if (isInside(handle, columns, rows)) {
                continue;
            }
            handles.removeIndex(i);
            handle.detach();
        }
    }

    void rebuildIndex() {
        byPosition.clear();
        for (int i = 0; i < handles.size; i++) {
            CellHandle handle = handles.get(i);
            byPosition.put(keyOf(handle.getColumn(), handle.getRow()), handle);
        }
    }

    /** Detaches every handle and empties the list. */
    void detachAll() {
        for (int i = handles.size - 1; i >= 0; i--) {
            handles.get(i).detach();
        }
        handles.clear();
        byPosition.clear();
    }

    private static boolean isInside(CellHandle handle, int columns, int rows) {
        return Guards.isCellInside(handle.getColumn(), handle.getRow(), columns, rows);
    }

    private static long keyOf(int column, int row) {
        return ((long) row << Integer.SIZE) | (column & 0xFFFFFFFFL);
    }
}
