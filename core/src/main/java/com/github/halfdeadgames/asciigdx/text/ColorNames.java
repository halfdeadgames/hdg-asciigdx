package com.github.halfdeadgames.asciigdx.text;

import java.util.OptionalInt;

/**
 * Looks up a color by name for the {@code [color:NAME]} and {@code [bg:NAME]} tags of
 * {@link StyledText}. The game supplies the names, so the library has no palette of its own.
 *
 * <p>Colors are RGBA8888 ints packed as {@code 0xRRGGBBAA}, with red in the highest byte, the same
 * format as every color in {@link TextPaint} and the grids.
 *
 * <p>Implementations are called while a text is parsed, not while it is drawn, so they may
 * allocate. They must not return {@code null}.
 *
 * <p>Example:
 * <pre>{@code
 * ColorNames names = name -> name.equals("red") ? OptionalInt.of(0xFF0000FF) : OptionalInt.empty();
 * StyledText text = StyledText.parse("The [color:red]quick[/color] fox", names);
 * }</pre>
 */
@FunctionalInterface
public interface ColorNames {

    /**
     * Knows no names, so every {@code [color:NAME]} tag that is not hex or {@code default} stays
     * visible as written.
     */
    ColorNames NONE = name -> OptionalInt.empty();

    /**
     * Finds the color with a name. The name is compared exactly, so {@code "Red"} and
     * {@code "red"} are different names unless the implementation says otherwise.
     *
     * @param name the text between {@code color:} and {@code ]} in the tag; never {@code null};
     *         may be empty
     * @return the color as RGBA8888 ({@code 0xRRGGBBAA}), or an empty value when the name is
     *         unknown; never {@code null}
     */
    OptionalInt findRgba(String name);
}
