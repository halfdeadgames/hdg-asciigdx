package com.github.halfdeadgames.asciigdx.workbench.rules;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CodePoints;
import java.util.ArrayList;
import java.util.List;

/** An immutable set of code points, stored as inclusive ranges. */
final class GlyphSet {

    private static final int MAX_CODE_POINT = Character.MAX_CODE_POINT;

    private final int[] firstCodePoints;
    private final int[] lastCodePoints;

    private GlyphSet(int[] firstCodePoints, int[] lastCodePoints) {
        this.firstCodePoints = firstCodePoints;
        this.lastCodePoints = lastCodePoints;
    }

    /**
     * Builds a set from tokens: a single character ("A"), a code ("U+0041"), or a range ("A-Z",
     * "U+2500-U+259F"). Tokens are not trimmed.
     *
     * @param tokens the tokens to parse
     * @return the set containing every code point the tokens name
     * @throws IllegalArgumentException if a token is empty or not a valid glyph, code, or range
     */
    public static GlyphSet parse(List<String> tokens) {
        var firsts = new ArrayList<Integer>();
        var lasts = new ArrayList<Integer>();
        for (String token : tokens) {
            int[] range = parseToken(token);
            firsts.add(range[0]);
            lasts.add(range[1]);
        }
        return new GlyphSet(toArray(firsts), toArray(lasts));
    }

    /**
     * Returns a set matching every code point.
     *
     * @return the all-matching set
     */
    public static GlyphSet all() {
        return new GlyphSet(new int[] {0}, new int[] {MAX_CODE_POINT});
    }

    /**
     * Joins sets into one set holding every code point of any of them.
     *
     * @param sets the sets to join
     * @return the union, empty if no sets are given
     */
    public static GlyphSet union(List<GlyphSet> sets) {
        var firsts = new ArrayList<Integer>();
        var lasts = new ArrayList<Integer>();
        for (GlyphSet set : sets) {
            for (int i = 0; i < set.firstCodePoints.length; i++) {
                firsts.add(set.firstCodePoints[i]);
                lasts.add(set.lastCodePoints[i]);
            }
        }
        return new GlyphSet(toArray(firsts), toArray(lasts));
    }

    /**
     * Tells whether the code point is in this set.
     *
     * @param codePoint the code point to look up
     * @return true if the set contains it
     */
    public boolean contains(int codePoint) {
        for (int i = 0; i < firstCodePoints.length; i++) {
            if (codePoint >= firstCodePoints[i] && codePoint <= lastCodePoints[i]) {
                return true;
            }
        }
        return false;
    }

    private static int[] parseToken(String token) {
        if (token.isEmpty()) {
            throw new IllegalArgumentException("An empty token is not a glyph: " + allowedForms());
        }
        if (token.codePointCount(0, token.length()) == 1) {
            int codePoint = token.codePointAt(0);
            return new int[] {codePoint, codePoint};
        }
        int code = CodePoints.readUPlus(token);
        if (code != CodePoints.NOT_A_CODE) {
            int codePoint = requireInUnicode(code, token);
            return new int[] {codePoint, codePoint};
        }
        return parseRange(token);
    }

    private static int[] parseRange(String token) {
        for (int dash = token.indexOf('-'); dash >= 0; dash = token.indexOf('-', dash + 1)) {
            int first = parseEndpoint(token.substring(0, dash));
            int last = parseEndpoint(token.substring(dash + 1));
            if (first < 0 || last < 0) {
                continue;
            }
            if (first > last) {
                throw new IllegalArgumentException(
                        "\"" + token + "\" is not a glyph range: the range runs backwards, so put the lower one first");
            }
            return new int[] {first, last};
        }
        throw new IllegalArgumentException("\"" + token + "\" is not a glyph: " + allowedForms());
    }

    /** Returns the code point of a one-character or U+ endpoint, or -1 if the text is neither. */
    private static int parseEndpoint(String text) {
        if (text.isEmpty()) {
            return -1;
        }
        if (text.codePointCount(0, text.length()) == 1) {
            return text.codePointAt(0);
        }
        int code = CodePoints.readUPlus(text);
        if (code == CodePoints.NOT_A_CODE) {
            return -1;
        }
        return requireInUnicode(code, text);
    }

    private static int requireInUnicode(int codePoint, String code) {
        if (codePoint > MAX_CODE_POINT) {
            throw new IllegalArgumentException(
                    "\"" + code + "\" is out of Unicode: the highest code is U+10FFFF");
        }
        return codePoint;
    }

    private static String allowedForms() {
        return "use one character like \"A\", a code like \"U+0041\", or a range like \"A-Z\"";
    }

    private static int[] toArray(List<Integer> values) {
        int[] array = new int[values.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = values.get(i);
        }
        return array;
    }
}
