package com.github.halfdeadgames.asciigdx.layer;

/** Shared argument and ownership checks; messages are built only when a check fails. */
final class Guards {

    private Guards() {
    }

    /** Throws {@link IllegalArgumentException} when {@code value} is null. */
    static void requireNotNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }

    /**
     * Checks that an item's current owner is the expected one.
     *
     * @param currentOwner the owner the item reports, or null when it was removed
     * @param typeName the item's type as shown at the start of the message
     * @param ownerName the owner's kind as shown at the end of the message
     * @throws IllegalStateException if the item has no owner
     * @throws IllegalArgumentException if the item has a different owner
     */
    static void requireOwnedBy(Object currentOwner, Object expectedOwner, String typeName,
            String ownerName) {
        if (currentOwner == null) {
            throw new IllegalStateException(typeName + " was removed from its " + ownerName);
        }
        if (currentOwner != expectedOwner) {
            throw new IllegalArgumentException(typeName + " belongs to another " + ownerName);
        }
    }

    /** Throws {@link IndexOutOfBoundsException} unless {@code 0 <= index < size}. */
    static void requireIndexInRange(int index, int size) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index " + index + ", size " + size);
        }
    }

    /** Returns whether a cell lies inside a grid of the given size. */
    static boolean isCellInside(int column, int row, int columns, int rows) {
        return column >= 0 && column < columns && row >= 0 && row < rows;
    }

    /**
     * Throws {@link IndexOutOfBoundsException} unless the cell lies inside a grid of the given
     * size.
     */
    static void requireCellInside(int column, int row, int columns, int rows) {
        if (!isCellInside(column, row, columns, rows)) {
            throw new IndexOutOfBoundsException("Cell (" + column + ", " + row + ") outside "
                    + columns + "x" + rows);
        }
    }
}
