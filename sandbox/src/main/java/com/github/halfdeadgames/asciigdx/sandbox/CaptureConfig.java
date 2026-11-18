package com.github.halfdeadgames.asciigdx.sandbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/** Settings for an unattended capture run, parsed from a platform-neutral key lookup. */
public final class CaptureConfig {
    public static final String KEY_CAPTURE = "capture";
    public static final String KEY_SCENES = "scenes";
    public static final String KEY_SAMPLES = "samples";
    public static final String KEY_SAMPLE_SECONDS = "sampleSeconds";
    public static final String KEY_WARM_UP_SECONDS = "warmUpSeconds";
    public static final String KEY_DIRTY = "dirty";
    public static final String KEY_LAYERS = "layers";
    static final int MAX_LAYER_COUNT = 8;
    private static final int DEFAULT_SAMPLES = 5;
    private static final float DEFAULT_SAMPLE_SECONDS = 0.5f;
    private static final float DEFAULT_WARM_UP_SECONDS = 0f;
    private static final int MAX_DIRTY_PERCENT = 100;

    private final String outputDirectory;
    private final List<String> sceneIds;
    private final int samples;
    private final float sampleSeconds;
    private final float warmUpSeconds;
    private final List<Integer> dirtyPercents;
    private final List<Integer> layerCounts;

    private CaptureConfig(String outputDirectory, List<String> sceneIds, int samples,
            float sampleSeconds, float warmUpSeconds,
            List<Integer> dirtyPercents, List<Integer> layerCounts) {
        this.layerCounts = Collections.unmodifiableList(new ArrayList<>(layerCounts));
        this.outputDirectory = outputDirectory;
        this.sceneIds = Collections.unmodifiableList(new ArrayList<>(sceneIds));
        this.samples = samples;
        this.sampleSeconds = sampleSeconds;
        this.warmUpSeconds = warmUpSeconds;
        this.dirtyPercents = Collections.unmodifiableList(new ArrayList<>(dirtyPercents));
    }

    /**
     * Returns null when capture mode is off (no "capture" value).
     *
     * @param lookup returns the value for a key, or null when missing
     * @throws IllegalArgumentException when "scenes", "samples", "sampleSeconds",
     *        "warmUpSeconds", "dirty", or "layers" holds an invalid value
     */
    public static CaptureConfig fromSettings(Function<String, String> lookup) {
        String outputDirectory = lookup.apply(KEY_CAPTURE);
        if (outputDirectory == null || outputDirectory.isEmpty()) {
            return null;
        }
        return new CaptureConfig(outputDirectory, parseSceneIds(lookup.apply(KEY_SCENES)),
                parseSamples(lookup.apply(KEY_SAMPLES)),
                parseSampleSeconds(lookup.apply(KEY_SAMPLE_SECONDS)),
                parseWarmUpSeconds(lookup.apply(KEY_WARM_UP_SECONDS)),
                parseDirtyPercents(lookup.apply(KEY_DIRTY)),
                parseLayerCounts(lookup.apply(KEY_LAYERS)));
    }

    private static List<Integer> parseLayerCounts(String value) {
        return parseBoundedWholeNumbers(value, KEY_LAYERS, 1, MAX_LAYER_COUNT);
    }

    private static List<Integer> parseDirtyPercents(String value) {
        return parseBoundedWholeNumbers(value, KEY_DIRTY, 0, MAX_DIRTY_PERCENT);
    }

    private static List<Integer> parseBoundedWholeNumbers(String value, String key, int min,
            int max) {
        List<Integer> numbers = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return numbers;
        }
        for (String part : value.split(",", -1)) {
            String text = part.trim();
            int number;
            try {
                number = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid value for '" + key + "': '" + text + "' is not a whole number", e);
            }
            if (number < min || number > max) {
                throw new IllegalArgumentException(
                        "Invalid value for '" + key + "': " + number + " must be between " + min
                                + " and " + max);
            }
            numbers.add(number);
        }
        return numbers;
    }

    private static List<String> parseSceneIds(String value) {
        List<String> ids = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return ids;
        }
        for (String part : value.split(",", -1)) {
            String id = part.trim();
            if (id.isEmpty()) {
                throw new IllegalArgumentException(
                        "Invalid value for '" + KEY_SCENES + "': '" + value
                                + "' contains an empty scene id");
            }
            ids.add(id);
        }
        return ids;
    }

    private static int parseSamples(String value) {
        if (value == null || value.trim().isEmpty()) {
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
            throw new IllegalArgumentException(
                    "Invalid value for '" + KEY_SAMPLE_SECONDS + "': '" + value
                            + "' must be positive");
        }
        return parsed;
    }

    private static float parseWarmUpSeconds(String value) {
        float parsed = parseFiniteSeconds(value, KEY_WARM_UP_SECONDS, DEFAULT_WARM_UP_SECONDS);
        if (parsed < 0f) {
            throw new IllegalArgumentException(
                    "Invalid value for '" + KEY_WARM_UP_SECONDS + "': '" + value
                            + "' must not be negative");
        }
        return parsed;
    }

    private static float parseFiniteSeconds(String value, String key, float defaultSeconds) {
        if (value == null || value.trim().isEmpty()) {
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

    String getOutputDirectory() {
        return outputDirectory;
    }

    /** Scene ids to visit in order; empty means every scene. */
    List<String> getSceneIds() {
        return sceneIds;
    }

    /** Dirty percentages to sweep in order; empty means no sweep. */
    List<Integer> getDirtyPercents() {
        return dirtyPercents;
    }

    /** Layer counts to sweep in order; empty means no sweep. */
    List<Integer> getLayerCounts() {
        return layerCounts;
    }

    /** Number of timing windows collected per scene after warm-up. */
    int getSamples() {
        return samples;
    }

    /**
     * Minimum wall-clock seconds of warm-up after opening a scene; 0 by default. Warm-up ends
     * only when both this time and the fixed warm-up frame count have passed.
     */
    float getWarmUpSeconds() {
        return warmUpSeconds;
    }

    /** Minimum wall-clock length of each timing window. */
    float getSampleSeconds() {
        return sampleSeconds;
    }
}
