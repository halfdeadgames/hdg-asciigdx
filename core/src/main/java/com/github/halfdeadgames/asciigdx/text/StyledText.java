package com.github.halfdeadgames.asciigdx.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A text with inline color and effect tags, parsed once into plain visible text plus a foreground
 * and background color per letter and a list of effect runs. Immutable and thread-safe. Parse
 * when the text changes, then draw it every frame with the {@link TextDrawing} methods that take a
 * {@code StyledText}; drawing allocates nothing.
 *
 * <p>Tags:
 * <ul>
 *   <li>{@code [color:NAME]}, {@code [color:#rrggbb]}, and {@code [color:#rrggbbaa]} set the
 *       foreground color of the letters that follow. Hex digits are case-insensitive; six digits
 *       are fully opaque. {@code NAME} is looked up exactly (case-sensitive) in the
 *       {@link ColorNames} given to {@link #parse}.</li>
 *   <li>{@code [bg:...]} does the same for the background color.</li>
 *   <li>{@code [color:default]} and {@code [bg:default]} mean the color of the
 *       {@link TextPaint} used for drawing. A letter whose style is the default has no style
 *       of its own: {@link #hasForegroundAt} or {@link #hasBackgroundAt} returns
 *       {@code false} for it.</li>
 *   <li>Every opening tag pushes a value on the stack of its channel, {@code default} included.
 *       {@code [/color]} and {@code [/bg]} pop it and restore the outer value, so tags nest and
 *       you can switch back either by name or with a closer. A closer with nothing to pop is
 *       plain visible text; it is never hidden silently.</li>
 *   <li>{@code [effect:NAME]} and {@code [effect:NAME key=value key=value]} start an effect for
 *       the letters that follow; {@code [/effect]} ends the most recently opened one. Settings
 *       are separated by single spaces, values hold no spaces, and nothing is quoted. Names and
 *       keys are case-sensitive. They are resolved by the {@link EffectNames} given to
 *       {@link #parse(CharSequence, ColorNames, EffectNames)}; the result is read with
 *       {@link #getEffectRuns()}. Effect tags are only read by that overload. An effect still
 *       open at the end of the text applies to the end, like an open color.</li>
 *   <li>{@code [[} writes one literal {@code [}. Use {@link #escape} for text you do not
 *       trust.</li>
 * </ul>
 *
 * <p>Anything that is not a complete, valid tag stays in the visible text exactly as written: a
 * lone {@code [}, an unknown keyword, an unknown color name, bad hex, an unknown effect, an
 * unknown or bad effect setting, a malformed effect tag, {@code [/effect]} with no open effect,
 * or a missing {@code ]}. Parsing never throws on bad text. Tags take no width and no letter
 * index, so wrapping, ellipsis, and hyphenation count visible letters only.
 *
 * <p>A letter is one code point, which takes one cell. Letter indexes in this class are code
 * point indexes into {@link #getVisibleText()}, from 0 to {@link #getVisibleLength()} - 1. To keep
 * that true, an unpaired surrogate char in the source is replaced by U+FFFD (the replacement
 * character) in the visible text; otherwise a tag between a lone high and a lone low surrogate
 * would join them into one code point.
 *
 * <p>The class is final and immutable. Each style channel is a pair of methods: {@code hasXAt}
 * says whether a letter has that style, and {@code getXAt} reads it and throws when it has none,
 * so ask first. The overloads that take a default value ({@link #getForegroundRgbaAt(int, int)},
 * {@link #getBackgroundRgbaAt(int, int)}) do both steps in one call. Effects are a list of ranges,
 * {@link #getEffectRuns()}.
 *
 * <p>Colors are RGBA8888 ints packed as {@code 0xRRGGBBAA}.
 *
 * <p>Example:
 * <pre>{@code
 * StyledText text = StyledText.parse("The [color:red]quick[color:default] brown fox", names);
 * TextDrawing.drawText(grid, 2, 1, text, TextPaint.of(0xFFFFFFFF, 0x000000FF));
 *
 * StyledText wavy = StyledText.parse("A [effect:wave amplitude=0.5]wavy[/effect] word", names,
 *         EffectRegistry.withBuiltIns());
 * }</pre>
 */
public final class StyledText {

    static final StyledText UNSTYLED_EMPTY = new StyledText("", new boolean[0], new int[0],
            new boolean[0], new int[0], Collections.emptyList());

    private static final char TAG_OPEN = '[';
    private static final String ESCAPED_TAG_OPEN = "[[";

    private final String visibleText;
    private final boolean[] hasForeground;
    private final int[] foregroundRgba;
    private final boolean[] hasBackground;
    private final int[] backgroundRgba;
    private final List<EffectRun> effectRuns;

    StyledText(String visibleText, boolean[] hasForeground, int[] foregroundRgba,
            boolean[] hasBackground, int[] backgroundRgba, List<EffectRun> effectRuns) {
        this.visibleText = visibleText;
        this.hasForeground = hasForeground;
        this.foregroundRgba = foregroundRgba;
        this.hasBackground = hasBackground;
        this.backgroundRgba = backgroundRgba;
        this.effectRuns = Collections.unmodifiableList(new ArrayList<>(effectRuns));
    }

    /**
     * Reads the tags of a text. Call it when the text or the names change, not every frame.
     * Effect tags are not read: they stay visible as written. Use
     * {@link #parse(CharSequence, ColorNames, EffectNames)} to read them.
     *
     * @param source text with tags; must not be {@code null}; may be empty
     * @param names colors for {@code [color:NAME]} and {@code [bg:NAME]}; must not be
     *         {@code null}; use {@link ColorNames#NONE} for hex colors only. It is called during
     *         this method only and is not kept
     * @return the parsed text, never {@code null}
     * @throws NullPointerException if {@code source} or {@code names} is {@code null}, or if
     *         {@code names} returns {@code null}
     */
    public static StyledText parse(CharSequence source, ColorNames names) {
        return parse(source, names, EffectNames.NONE);
    }

    /**
     * Reads the color tags and the effect tags of a text. Call it when the text or the names
     * change, not every frame. Effects are built during this call, so the result holds them and
     * {@code effectNames} is not kept.
     *
     * @param source text with tags; must not be {@code null}; may be empty
     * @param names colors for {@code [color:NAME]} and {@code [bg:NAME]}; must not be
     *         {@code null}; use {@link ColorNames#NONE} for hex colors only
     * @param effectNames effects for {@code [effect:NAME ...]}; must not be {@code null}; use
     *         {@link EffectNames#NONE} to leave effect tags visible
     * @return the parsed text, never {@code null}
     * @throws NullPointerException if an argument is {@code null}, or if {@code names} or
     *         {@code effectNames} returns {@code null}, or if an effect factory behind
     *         {@code effectNames} returns {@code null}. Only an
     *         {@link IllegalArgumentException} from {@code effectNames} counts as bad text and
     *         leaves the tag visible. Any other exception from it, and a {@code null} result, is
     *         a bug in the game and propagates out of this method
     */
    public static StyledText parse(CharSequence source, ColorNames names,
            EffectNames effectNames) {
        return new StyledTextParser(source, names, effectNames).parse();
    }

    /**
     * Makes text safe to embed in a tagged text: each {@code [} becomes {@code [[}, so the text
     * cannot start a tag. Only {@code [} is replaced; {@code ]} and every other character stay as
     * they are, so the result is longer than the input by the number of {@code [} in it. Parsing
     * the result gives back the original text as its visible text (except that a lone surrogate
     * becomes U+FFFD, see {@link StyledText}).
     *
     * <p>Call it once, on the untrusted part only, before joining it with your own tags. Never
     * call it on text that already holds your own tags (they would stop working) or that was
     * already escaped: a second call doubles every {@code [[} to {@code [[[[}.
     *
     * <pre>{@code
     * String line = "[color:red]" + StyledText.escape(playerName) + "[/color] joined";
     * }</pre>
     *
     * @param untrustedText text to protect, such as a player name; must not be {@code null}
     * @return the escaped text, never {@code null}
     * @throws NullPointerException if {@code untrustedText} is {@code null}
     */
    public static String escape(CharSequence untrustedText) {
        Objects.requireNonNull(untrustedText, "untrustedText");
        return untrustedText.toString().replace(String.valueOf(TAG_OPEN), ESCAPED_TAG_OPEN);
    }

    /**
     * Returns the text without tags, with each {@code [[} turned into {@code [}. This is the text
     * to lay out and measure.
     *
     * @return the visible text, never {@code null}
     */
    public String getVisibleText() {
        return visibleText;
    }

    /**
     * Returns the number of letters in the visible text.
     *
     * @return the length in code points, which is the number of cells the text takes; 0 or more
     */
    public int getVisibleLength() {
        return foregroundRgba.length;
    }

    /**
     * Returns the effects of the text as ranges of letters. The runs are ordered, do not overlap,
     * and are never empty; letters outside every run have no effect. Where effect tags nest, a run
     * holds the combination of all open effects, outermost first, and a new run starts whenever
     * the set of open effects changes. An effect still open at the end of the text runs to the
     * last letter. The list is empty when the text has no effects, including when it was parsed
     * without an {@link EffectNames}.
     *
     * @return an unmodifiable list, never {@code null}
     */
    public List<EffectRun> getEffectRuns() {
        return effectRuns;
    }

    /**
     * Returns whether a letter has a foreground color of its own. A letter without one uses the
     * color of the paint.
     *
     * @param letterIndex code point index into the visible text
     * @return {@code true} if a {@code [color:...]} tag other than {@code default} is open at this
     *         letter
     * @throws IndexOutOfBoundsException if {@code letterIndex} is negative or not less than
     *         {@link #getVisibleLength()}
     */
    public boolean hasForegroundAt(int letterIndex) {
        requireLetter(letterIndex);
        return hasForeground[letterIndex];
    }

    /**
     * Returns the foreground color of a letter.
     *
     * @param letterIndex code point index into the visible text
     * @return the color as RGBA8888
     * @throws IndexOutOfBoundsException if {@code letterIndex} is negative or not less than
     *         {@link #getVisibleLength()}
     * @throws IllegalStateException if the letter has no foreground color of its own; check
     *         {@link #hasForegroundAt(int)} first
     */
    public int getForegroundRgbaAt(int letterIndex) {
        if (!hasForegroundAt(letterIndex)) {
            throw new IllegalStateException("Letter " + letterIndex + " has no foreground color");
        }
        return foregroundRgba[letterIndex];
    }

    /**
     * Returns whether a letter has a background color of its own. A letter without one uses the
     * background of the paint.
     *
     * @param letterIndex code point index into the visible text
     * @return {@code true} if a {@code [bg:...]} tag other than {@code default} is open at this
     *         letter
     * @throws IndexOutOfBoundsException if {@code letterIndex} is negative or not less than
     *         {@link #getVisibleLength()}
     */
    public boolean hasBackgroundAt(int letterIndex) {
        requireLetter(letterIndex);
        return hasBackground[letterIndex];
    }

    /**
     * Returns the background color of a letter.
     *
     * @param letterIndex code point index into the visible text
     * @return the color as RGBA8888
     * @throws IndexOutOfBoundsException if {@code letterIndex} is negative or not less than
     *         {@link #getVisibleLength()}
     * @throws IllegalStateException if the letter has no background color of its own; check
     *         {@link #hasBackgroundAt(int)} first
     */
    public int getBackgroundRgbaAt(int letterIndex) {
        if (!hasBackgroundAt(letterIndex)) {
            throw new IllegalStateException("Letter " + letterIndex + " has no background color");
        }
        return backgroundRgba[letterIndex];
    }

    /**
     * Returns the foreground color of a letter, or a default when the letter has none of its own.
     * Same as {@link #hasForegroundAt(int)} followed by {@link #getForegroundRgbaAt(int)}.
     *
     * @param letterIndex code point index into the visible text
     * @param defaultRgba color to return for a letter without a foreground color, as RGBA8888,
     *         usually the paint's foreground
     * @return the letter's color as RGBA8888, or {@code defaultRgba}
     * @throws IndexOutOfBoundsException if {@code letterIndex} is negative or not less than
     *         {@link #getVisibleLength()}
     */
    public int getForegroundRgbaAt(int letterIndex, int defaultRgba) {
        requireLetter(letterIndex);
        return foregroundOr(letterIndex, defaultRgba);
    }

    /**
     * Returns the background color of a letter, or a default when the letter has none of its own.
     * Same as {@link #hasBackgroundAt(int)} followed by {@link #getBackgroundRgbaAt(int)}.
     *
     * @param letterIndex code point index into the visible text
     * @param defaultRgba color to return for a letter without a background color, as RGBA8888,
     *         usually the paint's background
     * @return the letter's color as RGBA8888, or {@code defaultRgba}
     * @throws IndexOutOfBoundsException if {@code letterIndex} is negative or not less than
     *         {@link #getVisibleLength()}
     */
    public int getBackgroundRgbaAt(int letterIndex, int defaultRgba) {
        requireLetter(letterIndex);
        return backgroundOr(letterIndex, defaultRgba);
    }

    // Drawing helpers: a letter outside the text, such as one of a plain text, has no style.
    int foregroundOr(int letterIndex, int fallbackRgba) {
        if (letterIndex < 0 || letterIndex >= hasForeground.length || !hasForeground[letterIndex]) {
            return fallbackRgba;
        }
        return foregroundRgba[letterIndex];
    }

    int backgroundOr(int letterIndex, int fallbackRgba) {
        if (letterIndex < 0 || letterIndex >= hasBackground.length || !hasBackground[letterIndex]) {
            return fallbackRgba;
        }
        return backgroundRgba[letterIndex];
    }

    private void requireLetter(int letterIndex) {
        if (letterIndex < 0 || letterIndex >= foregroundRgba.length) {
            throw new IndexOutOfBoundsException("letterIndex " + letterIndex
                    + " is outside 0.." + (foregroundRgba.length - 1));
        }
    }
}
