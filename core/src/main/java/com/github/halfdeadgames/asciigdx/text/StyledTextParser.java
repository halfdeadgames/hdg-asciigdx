package com.github.halfdeadgames.asciigdx.text;

import com.badlogic.gdx.utils.BooleanArray;
import com.badlogic.gdx.utils.IntArray;
import com.github.halfdeadgames.asciigdx.zone.CellEffect;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Reads the tags of one source text and builds a {@link StyledText}. One parser is used once and
 * never throws on bad input: anything that is not a complete, valid tag stays in the visible text.
 */
final class StyledTextParser {

    private static final char TAG_OPEN = '[';
    private static final char TAG_CLOSE = ']';
    private static final char HEX_PREFIX = '#';
    private static final String FOREGROUND_OPENER = "color:";
    private static final String BACKGROUND_OPENER = "bg:";
    private static final String FOREGROUND_CLOSER = "/color";
    private static final String BACKGROUND_CLOSER = "/bg";
    private static final String EFFECT_OPENER = "effect:";
    private static final String EFFECT_CLOSER = "/effect";
    private static final String WAIT_OPENER = "wait:";
    private static final String DEFAULT_VALUE = "default";
    private static final int REPLACEMENT_CODE_POINT = 0xFFFD;
    private static final int HEX_RADIX = 16;
    private static final int HEX_DIGITS_BEFORE_ALPHA = 6;
    private static final int HEX_DIGITS_WITH_ALPHA = 8;
    private static final int BITS_PER_HEX_DIGIT = 4;
    private static final int BITS_PER_CHANNEL = 8;
    private static final int OPAQUE_ALPHA = 0xFF;

    private final CharSequence source;
    private final ColorNames names;
    private final EffectNames effectNames;
    private final EffectRunBuilder effectRuns = new EffectRunBuilder();
    private final List<RevealPause> pauses = new ArrayList<>();
    private final StringBuilder visibleText = new StringBuilder();
    private final ColorStack foregroundStack = new ColorStack();
    private final ColorStack backgroundStack = new ColorStack();
    private final BooleanArray hasForeground = new BooleanArray();
    private final IntArray foregroundRgba = new IntArray();
    private final BooleanArray hasBackground = new BooleanArray();
    private final IntArray backgroundRgba = new IntArray();

    StyledTextParser(CharSequence source, ColorNames names, EffectNames effectNames) {
        this.source = Objects.requireNonNull(source, "source");
        this.names = Objects.requireNonNull(names, "names");
        this.effectNames = Objects.requireNonNull(effectNames, "effectNames");
    }

    StyledText parse() {
        int index = 0;
        while (index < source.length()) {
            index = readFrom(index);
        }
        effectRuns.closeOpenRun(foregroundRgba.size);
        return new StyledText(visibleText.toString(), hasForeground.toArray(),
                foregroundRgba.toArray(), hasBackground.toArray(), backgroundRgba.toArray(),
                effectRuns.getRuns(), pauses);
    }

    private int readFrom(int index) {
        if (source.charAt(index) != TAG_OPEN) {
            return readLetterAt(index);
        }
        if (isEscapedOpen(index)) {
            appendLetter(TAG_OPEN);
            return index + 2;
        }
        int closeIndex = findTagClose(index);
        if (closeIndex >= 0 && applyTag(source.subSequence(index + 1, closeIndex).toString())) {
            return closeIndex + 1;
        }
        appendLetter(TAG_OPEN);
        return index + 1;
    }

    private int readLetterAt(int index) {
        int codePoint = Character.codePointAt(source, index);
        appendLetter(codePoint);
        return index + Character.charCount(codePoint);
    }

    private boolean isEscapedOpen(int index) {
        return index + 1 < source.length() && source.charAt(index + 1) == TAG_OPEN;
    }

    // A tag cannot contain another '[', so a later tag still works after a broken opener.
    private int findTagClose(int openIndex) {
        for (int index = openIndex + 1; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == TAG_CLOSE) {
                return index;
            }
            if (character == TAG_OPEN) {
                return -1;
            }
        }
        return -1;
    }

    private boolean applyTag(String content) {
        if (content.equals(EFFECT_CLOSER)) {
            return closeEffectIfOpen();
        }
        if (content.startsWith(WAIT_OPENER)) {
            return addPause(content.substring(WAIT_OPENER.length()));
        }
        if (content.startsWith(EFFECT_OPENER)) {
            return openEffect(content.substring(EFFECT_OPENER.length()));
        }
        if (content.equals(FOREGROUND_CLOSER)) {
            return popIfOpen(foregroundStack);
        }
        if (content.equals(BACKGROUND_CLOSER)) {
            return popIfOpen(backgroundStack);
        }
        if (content.startsWith(FOREGROUND_OPENER)) {
            return pushValue(foregroundStack, content.substring(FOREGROUND_OPENER.length()));
        }
        if (content.startsWith(BACKGROUND_OPENER)) {
            return pushValue(backgroundStack, content.substring(BACKGROUND_OPENER.length()));
        }
        return false;
    }

    private boolean addPause(String secondsText) {
        if (!isPlainDecimal(secondsText)) {
            return false;
        }
        float seconds = Float.parseFloat(secondsText);
        if (!Float.isFinite(seconds)) {
            return false;
        }
        pauses.add(RevealPause.of(foregroundRgba.size, seconds));
        return true;
    }

    // Digits with an optional fraction; Float.parseFloat alone would also accept signs, exponents,
    // NaN, and type suffixes.
    private static boolean isPlainDecimal(String value) {
        int dotIndex = value.indexOf('.');
        if (dotIndex < 0) {
            return isAsciiDigits(value);
        }
        return isAsciiDigits(value.substring(0, dotIndex))
                && isAsciiDigits(value.substring(dotIndex + 1));
    }

    private static boolean isAsciiDigits(String value) {
        if (value.isEmpty()) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < '0' || character > '9') {
                return false;
            }
        }
        return true;
    }

    private boolean closeEffectIfOpen() {
        if (!effectRuns.hasOpenEffect()) {
            return false;
        }
        effectRuns.closeLast(foregroundRgba.size);
        return true;
    }

    private boolean openEffect(String body) {
        Optional<EffectTag> tag = EffectTag.read(body);
        if (tag.isEmpty()) {
            return false;
        }
        Optional<CellEffect> effect = resolveEffect(tag.get());
        if (effect.isEmpty()) {
            return false;
        }
        effectRuns.open(effect.get(), foregroundRgba.size);
        return true;
    }

    private Optional<CellEffect> resolveEffect(EffectTag tag) {
        try {
            return Objects.requireNonNull(
                    effectNames.findEffect(tag.name(), tag.settings()), "EffectNames result");
        } catch (IllegalArgumentException badSetting) {
            return Optional.empty();
        }
    }

    private static boolean popIfOpen(ColorStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        stack.pop();
        return true;
    }

    private boolean pushValue(ColorStack stack, String value) {
        if (value.equals(DEFAULT_VALUE)) {
            stack.pushDefault();
            return true;
        }
        OptionalInt rgba = resolveColor(value);
        if (rgba.isEmpty()) {
            return false;
        }
        stack.pushColor(rgba.getAsInt());
        return true;
    }

    private OptionalInt resolveColor(String value) {
        if (!value.isEmpty() && value.charAt(0) == HEX_PREFIX) {
            return parseHex(value);
        }
        return Objects.requireNonNull(names.findRgba(value), "ColorNames result");
    }

    private static OptionalInt parseHex(String value) {
        int digitCount = value.length() - 1;
        if (digitCount != HEX_DIGITS_BEFORE_ALPHA && digitCount != HEX_DIGITS_WITH_ALPHA) {
            return OptionalInt.empty();
        }
        int rgba = 0;
        for (int index = 1; index < value.length(); index++) {
            int digit = hexDigitValue(value.charAt(index));
            if (digit < 0) {
                return OptionalInt.empty();
            }
            rgba = (rgba << BITS_PER_HEX_DIGIT) | digit;
        }
        if (digitCount == HEX_DIGITS_BEFORE_ALPHA) {
            rgba = (rgba << BITS_PER_CHANNEL) | OPAQUE_ALPHA;
        }
        return OptionalInt.of(rgba);
    }

    // Character.digit also accepts non-ASCII digits, which a color must not contain.
    private static int hexDigitValue(char character) {
        if (character > 'f') {
            return -1;
        }
        return Character.digit(character, HEX_RADIX);
    }

    // An unpaired surrogate is replaced, because a removed tag between a lone high and a lone low
    // surrogate would make them one code point and shift every later letter index.
    private void appendLetter(int codePoint) {
        boolean isLoneSurrogate =
                codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE;
        visibleText.appendCodePoint(isLoneSurrogate ? REPLACEMENT_CODE_POINT : codePoint);
        hasForeground.add(foregroundStack.hasColor());
        foregroundRgba.add(foregroundStack.hasColor() ? foregroundStack.getRgba() : 0);
        hasBackground.add(backgroundStack.hasColor());
        backgroundRgba.add(backgroundStack.hasColor() ? backgroundStack.getRgba() : 0);
    }
}
