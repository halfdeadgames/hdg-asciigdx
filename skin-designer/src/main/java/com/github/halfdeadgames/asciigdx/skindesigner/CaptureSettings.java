package com.github.halfdeadgames.asciigdx.skindesigner;

import java.util.function.Function;

/** Settings of an unattended capture run, parsed from a key lookup. */
record CaptureSettings(String outputDirectory, int samples, float sampleSeconds,
        float warmUpSeconds) {
    static final String KEY_CAPTURE = "capture";
    static final String KEY_SAMPLES = "samples";
    static final String KEY_SAMPLE_SECONDS = "sampleSeconds";
    static final String KEY_WARM_UP_SECONDS = "warmUpSeconds";
    private static final int DEFAULT_SAMPLES = 5;
    private static final float DEFAULT_SAMPLE_SECONDS = 0.5f;
    private static final float DEFAULT_WARM_UP_SECONDS = 0f;

    /**
     * Returns null when capture mode is off (no "capture" value).
     *
     * @throws IllegalArgumentException when "samples", "sampleSeconds", or "warmUpSeconds" holds
     *         an invalid value
     */
    static CaptureSettings fromLookup(Function<String, String> lookup) {
        String outputDirectory = lookup.apply(KEY_CAPTURE);
        if (outputDirectory == null || outputDirectory.isEmpty()) {
            return null;
        }
        return new CaptureSettings(outputDirectory, parseSamples(lookup.apply(KEY_SAMPLES)),
                parseSampleSeconds(lookup.apply(KEY_SAMPLE_SECONDS)),
                parseWarmUpSeconds(lookup.apply(KEY_WARM_UP_SECONDS)));
    }

    private static int parseSamples(String value) {
        if (isBlank(value)) {
            return DEFAULT_SAMPLES;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value for '" + KEY_SAMPLES + "': '" + value
                    + "' is not a whole number", e);
        }
        if (parsed <= 0) {
            throw new IllegalArgumentException(
                    "Invalid value for '" + KEY_SAMPLES + "': '" + value + "' must be positive");
        }
        return parsed;
    }

    private static float parseSampleSeconds(String value) {
        float parsed = parseFiniteSeconds(value, KEY_SAMPLE_SECONDS, DEFAULT_SAMPLE_SECONDS);
        if (!(parsed > 0f)) {
            throw new IllegalArgumentException("Invalid value for '" + KEY_SAMPLE_SECONDS + "': '"
                    + value + "' must be positive");
        }
        return parsed;
    }

    private static float parseWarmUpSeconds(String value) {
        float parsed = parseFiniteSeconds(value, KEY_WARM_UP_SECONDS, DEFAULT_WARM_UP_SECONDS);
        if (parsed < 0f) {
            throw new IllegalArgumentException("Invalid value for '" + KEY_WARM_UP_SECONDS + "': '"
                    + value + "' must not be negative");
        }
        return parsed;
    }

    private static float parseFiniteSeconds(String value, String key, float defaultSeconds) {
        if (isBlank(value)) {
            return defaultSeconds;
        }
        float parsed;
        try {
            parsed = Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid value for '" + key + "': '" + value + "' is not a number", e);
        }
        if (Float.isNaN(parsed) || Float.isInfinite(parsed)) {
            throw new IllegalArgumentException(
                    "Invalid value for '" + key + "': '" + value + "' must be a finite number");
        }
        return parsed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
