package com.github.halfdeadgames.asciigdx.workbench.unicodenames;

import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;

/**
 * Names, General_Category types, and blocks of code points, the same on every JDK. The JDK answers first
 * for names and types; the bundled table fills in what the JDK does not know. Blocks always come from the
 * table, so the sheet grouping does not depend on the JDK.
 */
public final class UnicodeFacts {

    private final IntUnaryOperator jdkTypes;
    private final IntFunction<String> jdkNames;
    private final UnicodeNameTable table;

    private UnicodeFacts(IntUnaryOperator jdkTypes, IntFunction<String> jdkNames, UnicodeNameTable table) {
        this.jdkTypes = jdkTypes;
        this.jdkNames = jdkNames;
        this.table = table;
    }

    /** Loads the bundled table on first use only. */
    private static final class Bundled {
        static final UnicodeFacts INSTANCE = new UnicodeFacts(Character::getType, Character::getName,
            UnicodeNameTable.loadBundled());
    }

    public static UnicodeFacts bundled() {
        return Bundled.INSTANCE;
    }

    /**
     * Builds facts from table text and stand-ins for the JDK, so a caller can describe a JDK that is
     * older than the table.
     *
     * @param jdkTypes gives a {@link Character} type constant
     * @param jdkNames gives a name, or null
     * @throws IllegalArgumentException if the table text is malformed
     */
    public static UnicodeFacts fromTableText(String tableText, IntUnaryOperator jdkTypes,
                                             IntFunction<String> jdkNames) {
        return new UnicodeFacts(jdkTypes, jdkNames, UnicodeNameTable.parse(tableText));
    }

    /** Returns the JDK's name, else the table's, else null. */
    public String name(int codePoint) {
        String name = jdkNames.apply(codePoint);
        return name != null ? name : table.nameOf(codePoint);
    }

    /** Returns a {@link Character} type constant: the JDK's if it knows the code point, else the table's. */
    public byte type(int codePoint) {
        int jdkType = jdkTypes.applyAsInt(codePoint);
        if (jdkType != Character.UNASSIGNED) {
            return (byte) jdkType;
        }
        return table.typeOf(codePoint);
    }

    /** Returns the name of the Unicode block holding the code point, or null if it is in none. */
    public String blockOf(int codePoint) {
        return table.blockOf(codePoint);
    }
}
