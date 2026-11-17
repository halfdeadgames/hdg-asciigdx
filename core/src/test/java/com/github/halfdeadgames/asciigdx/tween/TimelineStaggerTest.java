package com.github.halfdeadgames.asciigdx.tween;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimelineStaggerTest {

    private static final float EPSILON = 1e-5f;

    /** Linear ramp from 0 at t=0 to 10 at t=10, holding both ends. */
    private static final class RampTrack implements FloatTrack {

        @Override
        public float getValueAt(float timeSeconds) {
            return Math.max(0f, Math.min(10f, timeSeconds));
        }

        @Override
        public float getStartSeconds() {
            return 0f;
        }

        @Override
        public float getEndSeconds() {
            return 10f;
        }
    }

    private static final class IndexedRecorder implements IndexedFloatSetter {

        final List<Integer> indices = new ArrayList<>();
        final List<Float> values = new ArrayList<>();

        @Override
        public void set(int index, float value) {
            indices.add(index);
            values.add(value);
        }
    }

    private static StaggeredTrack staggeredTrack() {
        return new StaggeredTrack(new RampTrack(), 1f);
    }

    @Test
    void writesValuePerIndexAtSeveralTimes() {
        Timeline timeline = new Timeline();
        IndexedRecorder recorder = new IndexedRecorder();
        timeline.bind(staggeredTrack(), 3, recorder);

        timeline.apply(0.5f);
        assertEquals(List.of(0, 1, 2), recorder.indices);
        assertEquals(0.5f, recorder.values.get(0), EPSILON);
        assertEquals(0f, recorder.values.get(1), EPSILON);
        assertEquals(0f, recorder.values.get(2), EPSILON);

        recorder.indices.clear();
        recorder.values.clear();
        timeline.apply(2.5f);
        assertEquals(List.of(0, 1, 2), recorder.indices);
        assertEquals(2.5f, recorder.values.get(0), EPSILON);
        assertEquals(1.5f, recorder.values.get(1), EPSILON);
        assertEquals(0.5f, recorder.values.get(2), EPSILON);
    }

    @Test
    void skipsUnchangedIndicesIndividually() {
        Timeline timeline = new Timeline();
        IndexedRecorder recorder = new IndexedRecorder();
        timeline.bind(staggeredTrack(), 3, recorder);
        timeline.apply(0.5f);
        recorder.indices.clear();
        recorder.values.clear();

        // Indices 1 and 2 are still held at 0; only index 0 changes.
        timeline.apply(0.75f);
        assertEquals(List.of(0), recorder.indices);

        // Index 1 starts moving; index 2 stays held.
        recorder.indices.clear();
        timeline.apply(1.5f);
        assertEquals(List.of(0, 1), recorder.indices);
    }

    @Test
    void forceNextApplyRewritesAllIndicesOnce() {
        Timeline timeline = new Timeline();
        IndexedRecorder recorder = new IndexedRecorder();
        timeline.bind(staggeredTrack(), 3, recorder);
        timeline.apply(50f);
        assertEquals(3, recorder.indices.size());

        timeline.forceNextApply();
        timeline.apply(50f);
        assertEquals(6, recorder.indices.size());

        timeline.apply(50f);
        assertEquals(6, recorder.indices.size());
    }

    @Test
    void writingEveryApplyRewritesAllIndicesEveryApply() {
        Timeline timeline = Timeline.writingEveryApply();
        IndexedRecorder recorder = new IndexedRecorder();
        timeline.bind(staggeredTrack(), 3, recorder);
        timeline.apply(50f);
        timeline.apply(50f);
        assertEquals(6, recorder.indices.size());
    }

    @Test
    void countBelowOneThrows() {
        Timeline timeline = new Timeline();
        assertThrows(IllegalArgumentException.class,
                () -> timeline.bind(staggeredTrack(), 0, new IndexedRecorder()));
        assertThrows(IllegalArgumentException.class,
                () -> timeline.bind(staggeredTrack(), -1, new IndexedRecorder()));
        assertEquals(0, timeline.getBindingCount());
    }

    @Test
    void nullArgumentsThrow() {
        Timeline timeline = new Timeline();
        assertThrows(NullPointerException.class,
                () -> timeline.bind((StaggeredTrack) null, 2, new IndexedRecorder()));
        assertThrows(NullPointerException.class,
                () -> timeline.bind(staggeredTrack(), 2, (IndexedFloatSetter) null));
    }

    @Test
    void duplicateSetterThrows() {
        Timeline timeline = new Timeline();
        IndexedRecorder recorder = new IndexedRecorder();
        timeline.bind(staggeredTrack(), 2, recorder);
        assertThrows(IllegalArgumentException.class,
                () -> timeline.bind(staggeredTrack(), 2, recorder));
        assertEquals(1, timeline.getBindingCount());
    }
}
