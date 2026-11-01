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
    public static final String KEY_DIRTY = "dirty";
    public static final int DEFAULT_SAMPLES = 5;
    public static final float DEFAULT_SAMPLE_SECONDS = 0.5f;
    private static final int MAX_DIRTY_PERCENT = 100;

    private final String outputDirectory;
    private final List<String> sceneIds;
    private final int samples;
    private final float sampleSeconds;
    private final List<Integer> dirtyPercents;

    public CaptureConfig(String outputDirectory, List<String> sceneIds, int samples, float sampleSeconds,
                         List<Integer> dirtyPercents) {
        this.outputDirectory = outputDirectory;
        this.sceneIds = Collections.unmodifiableList(new ArrayList<>(sceneIds));
        this.samples = samples;
        this.sampleSeconds = sampleSeconds;
        this.dirtyPercents = Collections.unmodifiableList(new ArrayList<>(dirtyPercents));
    }

    /**
     * Returns null when capture mode is off (no "capture" value).
     *
     * @param lookup returns the value for a key, or null when missing
     * @throws IllegalArgumentException when "scenes", "samples", "sampleSeconds" or "dirty" holds an invalid value
     */
    public static CaptureConfig fromSettings(Function<String, String> lookup) {
        String outputDirectory = lookup.apply(KEY_CAPTURE);
        if (outputDirectory == null || outputDirectory.isEmpty()) {
            return null;
        }
        return new CaptureConfig(outputDirectory, parseSceneIds(lookup.apply(KEY_SCENES)),
            parseSamples(lookup.apply(KEY_SAMPLES)), parseSampleSeconds(lookup.apply(KEY_SAMPLE_SECONDS)),
            parseDirtyPercents(lookup.apply(KEY_DIRTY)));
    }

    private static List<Integer> parseDirtyPercents(String value) {
        List<Integer> percents = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return percents;
        }
        for (String part : value.split(",", -1)) {
            String text = part.trim();
            int percent;
            try {
                percent = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid value for '" + KEY_DIRTY + "': '" + text + "' is not a whole number", e);
            }
            if (percent < 0 || percent > MAX_DIRTY_PERCENT) {
                throw new IllegalArgumentException("Invalid value for '" + KEY_DIRTY + "': " + percent + " must be between 0 and " + MAX_DIRTY_PERCENT);
            }
            percents.add(percent);
        }
        return percents;
    }

    private static List<String> parseSceneIds(String value) {
        List<String> ids = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return ids;
        }
        for (String part : value.split(",", -1)) {
            String id = part.trim();
            if (id.isEmpty()) {
                throw new IllegalArgumentException("Invalid value for '" + KEY_SCENES + "': '" + value + "' contains an empty scene id");
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
            throw new IllegalArgumentException("Invalid value for '" + KEY_SAMPLES + "': '" + value + "' is not a whole number", e);
        }
        if (parsed <= 0) {
            throw new IllegalArgumentException("Invalid value for '" + KEY_SAMPLES + "': '" + value + "' must be positive");
        }
        return parsed;
    }

    private static float parseSampleSeconds(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_SAMPLE_SECONDS;
        }
        float parsed;
        try {
            parsed = Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value for '" + KEY_SAMPLE_SECONDS + "': '" + value + "' is not a number", e);
        }
        if (!(parsed > 0f) || Float.isInfinite(parsed)) {
            throw new IllegalArgumentException("Invalid value for '" + KEY_SAMPLE_SECONDS + "': '" + value + "' must be positive");
        }
        return parsed;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    /** Scene ids to visit in order; empty means every scene. */
    public List<String> getSceneIds() {
        return sceneIds;
    }

    /** Dirty percentages to sweep in order; empty means no sweep. */
    public List<Integer> getDirtyPercents() {
        return dirtyPercents;
    }

    /** Number of timing windows collected per scene after warm-up. */
    public int getSamples() {
        return samples;
    }

    /** Minimum wall-clock length of each timing window. */
    public float getSampleSeconds() {
        return sampleSeconds;
    }
}
