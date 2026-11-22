package com.github.halfdeadgames.asciigdx.ui;

import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns a skin file tree into strict JSON text that {@link SkinFileReader} reads back to an
 * identical skin. Built-in sections come first, in {@link SkinSchema} order, then custom sections
 * in the order they have in the tree. Keys inside an entry keep their tree order.
 *
 * <p>The layout follows {@code assets/default-skin.json}: two spaces per level, sections always
 * spread over several lines, and entries made only of plain values on one line when that line
 * fits in 100 columns.
 */
final class SkinFileWriter {

    private static final String INDENT = "  ";
    private static final int MAX_LINE_WIDTH = 100;
    private static final int FIRST_INLINE_DEPTH = 2;
    private static final int FIRST_PRINTABLE_CHARACTER = 0x20;

    private SkinFileWriter() {
    }

    /**
     * Returns the JSON text for a skin file tree, ending with one newline.
     *
     * @param root the root object; its children are the sections
     * @throws IllegalArgumentException if the root is not an object, or the tree holds something
     *     JSON cannot hold (an unnamed object member, or a number that is not finite)
     */
    static String write(JsonValue root) {
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("The skin file root must be an object, was "
                    + (root == null ? "null" : root.type()));
        }
        return formatContainer(root, orderSections(root), 0, 0) + "\n";
    }

    private static List<JsonValue> orderSections(JsonValue root) {
        Map<String, JsonValue> sectionsByName = collectSections(root);
        List<JsonValue> ordered = new ArrayList<>();
        for (String sectionName : SkinSchema.getSectionNames()) {
            JsonValue section = sectionsByName.get(sectionName);
            if (section != null) {
                ordered.add(section);
            }
        }
        for (JsonValue section : sectionsByName.values()) {
            if (!SkinFileReader.isBuiltInSection(section.name)) {
                ordered.add(section);
            }
        }
        return ordered;
    }

    // JsonValue.get matches names ignoring case, so names that differ only by case are rejected.
    private static Map<String, JsonValue> collectSections(JsonValue root) {
        Map<String, JsonValue> sectionsByName = new LinkedHashMap<>();
        Map<String, String> nameByLowerCase = new HashMap<>();
        for (JsonValue section : getChildren(root)) {
            String name = section.name;
            if (name == null) {
                throw new IllegalArgumentException("A section has no name");
            }
            String earlierName = nameByLowerCase.put(name.toLowerCase(Locale.ROOT), name);
            if (earlierName != null && earlierName.equals(name)) {
                throw new IllegalArgumentException("The section \"" + name + "\" appears twice");
            }
            if (earlierName != null) {
                throw new IllegalArgumentException("The sections \"" + earlierName + "\" and \""
                        + name + "\" differ only by case");
            }
            rejectCaseClashWithBuiltIn(name);
            sectionsByName.put(name, section);
        }
        return sectionsByName;
    }

    private static void rejectCaseClashWithBuiltIn(String name) {
        for (String builtInName : SkinSchema.getSectionNames()) {
            if (builtInName.equalsIgnoreCase(name) && !builtInName.equals(name)) {
                throw new IllegalArgumentException("The section \"" + name
                        + "\" differs from the built-in section \"" + builtInName
                        + "\" only by case");
            }
        }
    }

    private static List<JsonValue> getChildren(JsonValue container) {
        List<JsonValue> children = new ArrayList<>();
        for (JsonValue child = container.child; child != null; child = child.next) {
            children.add(child);
        }
        return children;
    }

    private static String formatValue(JsonValue value, int depth, int usedWidth) {
        return switch (value.type()) {
            case object, array -> formatContainer(value, getChildren(value), depth, usedWidth);
            case stringValue -> quote(value.asString());
            case doubleValue -> formatDecimal(value.asDouble());
            case longValue -> Long.toString(value.asLong());
            case booleanValue -> Boolean.toString(value.asBoolean());
            case nullValue -> "null";
        };
    }

    private static String formatDecimal(double number) {
        if (Double.isNaN(number) || Double.isInfinite(number)) {
            throw new IllegalArgumentException("JSON cannot hold the number " + number);
        }
        // Double.toString always keeps a '.', so the reader sees a decimal and not a whole number.
        return Double.toString(number);
    }

    private static String formatContainer(JsonValue container, List<JsonValue> members, int depth,
            int usedWidth) {
        String open = container.isObject() ? "{" : "[";
        String close = container.isObject() ? "}" : "]";
        if (members.isEmpty()) {
            return open + close;
        }
        if (depth >= FIRST_INLINE_DEPTH && hasOnlyPlainValues(members)) {
            String inline = formatInline(container, members, open, close);
            if (usedWidth + inline.length() <= MAX_LINE_WIDTH) {
                return inline;
            }
        }
        return formatExpanded(container, members, depth, open, close);
    }

    private static boolean hasOnlyPlainValues(List<JsonValue> members) {
        for (JsonValue member : members) {
            if (member.isObject() || member.isArray()) {
                return false;
            }
        }
        return true;
    }

    private static String formatInline(JsonValue container, List<JsonValue> members, String open,
            String close) {
        List<String> parts = new ArrayList<>();
        for (JsonValue member : members) {
            parts.add(formatMemberKey(container, member) + formatValue(member, 0, 0));
        }
        if (container.isObject()) {
            return open + " " + String.join(", ", parts) + " " + close;
        }
        return open + String.join(", ", parts) + close;
    }

    private static String formatExpanded(JsonValue container, List<JsonValue> members, int depth,
            String open, String close) {
        String memberIndent = INDENT.repeat(depth + 1);
        StringBuilder text = new StringBuilder(open).append('\n');
        for (int i = 0; i < members.size(); i++) {
            JsonValue member = members.get(i);
            String key = formatMemberKey(container, member);
            boolean isLast = i == members.size() - 1;
            int trailingCommaWidth = isLast ? 0 : 1;
            text.append(memberIndent).append(key).append(formatValue(member, depth + 1,
                    memberIndent.length() + key.length() + trailingCommaWidth));
            text.append(isLast ? "\n" : ",\n");
        }
        return text.append(INDENT.repeat(depth)).append(close).toString();
    }

    private static String formatMemberKey(JsonValue container, JsonValue member) {
        if (container == null || !container.isObject()) {
            return "";
        }
        if (member.name == null) {
            throw new IllegalArgumentException("An object member has no name");
        }
        return quote(member.name) + ": ";
    }

    private static String quote(String text) {
        StringBuilder quoted = new StringBuilder(text.length() + 2).append('"');
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            boolean startsPair = Character.isHighSurrogate(character) && i + 1 < text.length()
                    && Character.isLowSurrogate(text.charAt(i + 1));
            if (startsPair) {
                quoted.append(character).append(text.charAt(++i));
            } else {
                appendEscaped(quoted, character);
            }
        }
        return quoted.append('"').toString();
    }

    private static void appendEscaped(StringBuilder target, char character) {
        switch (character) {
            case '"' -> target.append("\\\"");
            case '\\' -> target.append("\\\\");
            case '\b' -> target.append("\\b");
            case '\f' -> target.append("\\f");
            case '\n' -> target.append("\\n");
            case '\r' -> target.append("\\r");
            case '\t' -> target.append("\\t");
            default -> {
                // A surrogate that is not half of a pair would not survive UTF-8 encoding.
                if (character < FIRST_PRINTABLE_CHARACTER || Character.isSurrogate(character)) {
                    target.append(String.format("\\u%04x", (int) character));
                } else {
                    target.append(character);
                }
            }
        }
    }
}
