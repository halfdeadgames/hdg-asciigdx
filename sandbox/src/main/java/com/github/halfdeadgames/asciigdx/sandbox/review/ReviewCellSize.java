package com.github.halfdeadgames.asciigdx.sandbox.review;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pixel size of one cell in a candidate sheet. */
public record ReviewCellSize(int width, int height) {
    private static final Pattern FORMAT = Pattern.compile("(\\d{1,4})(?:x(\\d{1,4}))?");

    /**
     * Reads {@code WxH} (for example {@code 16x12}) or a single number for a square cell.
     *
     * @throws IllegalArgumentException when the text is missing, malformed, or not positive
     */
    public static ReviewCellSize parse(String text) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("reviewCell is required when reviewSheet is set");
        }
        Matcher matcher = FORMAT.matcher(text);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("reviewCell must look like 16x16 or 16, was '" + text + "'");
        }
        int width = Integer.parseInt(matcher.group(1));
        int height = matcher.group(2) == null ? width : Integer.parseInt(matcher.group(2));
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("reviewCell must be positive, was '" + text + "'");
        }
        return new ReviewCellSize(width, height);
    }
}
