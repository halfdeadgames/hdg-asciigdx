package com.github.halfdeadgames.asciigdx.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CaptureConfigTest {
    private static CaptureConfig parse(String... keysAndValues) {
        Map<String, String> settings = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            settings.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return CaptureConfig.fromSettings(settings::get);
    }

    @Test
    void missingCaptureReturnsNull() {
        assertNull(parse(CaptureConfig.KEY_SCENES, "a"));
    }

    @Test
    void emptyCaptureReturnsNull() {
        assertNull(parse(CaptureConfig.KEY_CAPTURE, ""));
    }

    @Test
    void defaultsToAllScenesAndDefaultSampling() {
        CaptureConfig config = parse(CaptureConfig.KEY_CAPTURE, "out");

        assertEquals("out", config.getOutputDirectory());
        assertTrue(config.getSceneIds().isEmpty());
        assertEquals(5, config.getSamples());
        assertEquals(0.5f, config.getSampleSeconds());
    }

    @Test
    void scenesAreTrimmedAndKeepOrder() {
        CaptureConfig config = parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_SCENES, " b , a,c ");

        assertEquals(List.of("b", "a", "c"), config.getSceneIds());
    }

    @Test
    void emptySceneEntryThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_SCENES, "a,,b"));
    }

    @Test
    void samplesAreParsed() {
        CaptureConfig config = parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_SAMPLES, " 9 ");

        assertEquals(9, config.getSamples());
    }

    @Test
    void nonNumericSamplesThrow() {
        assertThrows(IllegalArgumentException.class,
            () -> parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_SAMPLES, "many"));
    }

    @Test
    void zeroSamplesThrow() {
        assertThrows(IllegalArgumentException.class,
            () -> parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_SAMPLES, "0"));
    }

    @Test
    void negativeSamplesThrow() {
        assertThrows(IllegalArgumentException.class,
            () -> parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_SAMPLES, "-5"));
    }

    @Test
    void sampleSecondsAreParsed() {
        CaptureConfig config = parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_SAMPLE_SECONDS, " 1.25 ");

        assertEquals(1.25f, config.getSampleSeconds());
    }

    @Test
    void nonNumericSampleSecondsThrow() {
        assertThrows(IllegalArgumentException.class,
            () -> parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_SAMPLE_SECONDS, "long"));
    }

    @Test
    void zeroSampleSecondsThrow() {
        assertThrows(IllegalArgumentException.class,
            () -> parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_SAMPLE_SECONDS, "0"));
    }

    @Test
    void negativeSampleSecondsThrow() {
        assertThrows(IllegalArgumentException.class,
            () -> parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_SAMPLE_SECONDS, "-0.5"));
    }

    @Test
    void notANumberSampleSecondsThrow() {
        assertThrows(IllegalArgumentException.class,
            () -> parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_SAMPLE_SECONDS, "NaN"));
    }

    @Test
    void sceneListIsUnmodifiable() {
        CaptureConfig config = parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_SCENES, "a");

        assertThrows(UnsupportedOperationException.class, () -> config.getSceneIds().add("b"));
    }

    @Test
    void dirtyDefaultsToNoSweep() {
        assertTrue(parse(CaptureConfig.KEY_CAPTURE, "out").getDirtyPercents().isEmpty());
    }

    @Test
    void dirtyPercentsAreTrimmedAndKeepOrder() {
        CaptureConfig config = parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_DIRTY, " 50, 0,100 ,1");

        assertEquals(List.of(50, 0, 100, 1), config.getDirtyPercents());
    }

    @Test
    void nonNumericDirtyThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_DIRTY, "10,abc"));
    }

    @Test
    void emptyDirtyEntryThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_DIRTY, "10,,20"));
    }

    @Test
    void negativeDirtyThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_DIRTY, "-1"));
    }

    @Test
    void dirtyAboveHundredThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_DIRTY, "101"));
    }

    @Test
    void layersDefaultToNoSweep() {
        assertTrue(parse(CaptureConfig.KEY_CAPTURE, "out").getLayerCounts().isEmpty());
    }

    @Test
    void layerCountsAreTrimmedAndKeepOrder() {
        CaptureConfig config = parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_LAYERS, " 4, 1,8 ,2");

        assertEquals(List.of(4, 1, 8, 2), config.getLayerCounts());
    }

    @Test
    void nonNumericLayersThrow() {
        assertThrows(IllegalArgumentException.class,
            () -> parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_LAYERS, "1,abc"));
    }

    @Test
    void emptyLayersEntryThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_LAYERS, "1,,2"));
    }

    @Test
    void zeroLayersThrow() {
        assertThrows(IllegalArgumentException.class,
            () -> parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_LAYERS, "0"));
    }

    @Test
    void negativeLayersThrow() {
        assertThrows(IllegalArgumentException.class,
            () -> parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_LAYERS, "-1"));
    }

    @Test
    void layersAboveMaximumThrowWithClearMessage() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_LAYERS, "9"));

        assertTrue(error.getMessage().contains("'layers'") && error.getMessage().contains("between 1 and 8"));
    }

    @Test
    void layerListIsUnmodifiable() {
        CaptureConfig config = parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_LAYERS, "2");

        assertThrows(UnsupportedOperationException.class, () -> config.getLayerCounts().add(4));
    }

    @Test
    void dirtyListIsUnmodifiable() {
        CaptureConfig config = parse(CaptureConfig.KEY_CAPTURE, "out", CaptureConfig.KEY_DIRTY, "10");

        assertThrows(UnsupportedOperationException.class, () -> config.getDirtyPercents().add(20));
    }
}
