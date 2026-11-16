package com.github.halfdeadgames.asciigdx.tween;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimelineTest {

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

    private static final class FloatRecorder implements FloatSetter {

        final List<Float> values = new ArrayList<>();

        @Override
        public void set(float value) {
            values.add(value);
        }
    }

    private static final class ColorRecorder implements ColorSetter {

        final List<Integer> values = new ArrayList<>();

        @Override
        public void set(int rgba) {
            values.add(rgba);
        }
    }

    private static final class BooleanRecorder implements BooleanSetter {

        final List<Boolean> values = new ArrayList<>();

        @Override
        public void set(boolean value) {
            values.add(value);
        }
    }

    private static final class DualSetter implements FloatSetter, BooleanSetter {

        @Override
        public void set(float value) {
        }

        @Override
        public void set(boolean value) {
        }
    }

    private static ColorTrack colorTrack() {
        return new ColorTrack(new RampTrack(), 0xFF0000FF, 0x00FF00FF);
    }

    @Test
    void floatBindingWritesTrackValue() {
        Timeline timeline = new Timeline();
        FloatRecorder recorder = new FloatRecorder();
        timeline.bind(new RampTrack(), recorder);
        timeline.apply(4f);
        assertEquals(1, recorder.values.size());
        assertEquals(4f, recorder.values.get(0), EPSILON);
    }

    @Test
    void colorBindingWritesColorAt() {
        Timeline timeline = new Timeline();
        ColorRecorder recorder = new ColorRecorder();
        ColorTrack track = colorTrack();
        timeline.bind(track, recorder);
        timeline.apply(5f);
        assertEquals(1, recorder.values.size());
        assertEquals(track.getColorAt(5f), recorder.values.get(0).intValue());
    }

    @Test
    void visibilityFollowsHalfOpenInterval() {
        Timeline timeline = new Timeline();
        BooleanRecorder recorder = new BooleanRecorder();
        timeline.bindVisibility(2f, 4f, recorder);
        timeline.apply(1f);
        timeline.apply(2f);
        timeline.apply(3f);
        timeline.apply(4f);
        timeline.apply(5f);
        assertEquals(List.of(false, true, false), recorder.values);
    }

    @Test
    void visibilityIsNeverTrueWhenStartEqualsEnd() {
        Timeline timeline = new Timeline();
        BooleanRecorder recorder = new BooleanRecorder();
        timeline.bindVisibility(2f, 2f, recorder);
        timeline.apply(1f);
        timeline.apply(2f);
        timeline.apply(3f);
        assertEquals(List.of(false), recorder.values);
    }

    @Test
    void firstApplyWritesEvenWhenValueEqualsDefault() {
        Timeline timeline = new Timeline();
        FloatRecorder recorder = new FloatRecorder();
        timeline.bind(new RampTrack(), recorder);
        timeline.apply(0f);
        assertEquals(1, recorder.values.size());
        assertEquals(0f, recorder.values.get(0), EPSILON);
    }

    @Test
    void unchangedValueSkipsSetter() {
        Timeline timeline = new Timeline();
        FloatRecorder recorder = new FloatRecorder();
        timeline.bind(new RampTrack(), recorder);
        timeline.apply(12f);
        timeline.apply(15f);
        timeline.apply(20f);
        assertEquals(1, recorder.values.size());
    }

    @Test
    void changedValueWrites() {
        Timeline timeline = new Timeline();
        FloatRecorder recorder = new FloatRecorder();
        timeline.bind(new RampTrack(), recorder);
        timeline.apply(1f);
        timeline.apply(2f);
        assertEquals(2, recorder.values.size());
        assertEquals(2f, recorder.values.get(1), EPSILON);
    }

    @Test
    void forceNextApplyRewritesOnceThenSkippingResumes() {
        Timeline timeline = new Timeline();
        FloatRecorder floats = new FloatRecorder();
        ColorRecorder colors = new ColorRecorder();
        BooleanRecorder booleans = new BooleanRecorder();
        timeline.bind(new RampTrack(), floats);
        timeline.bind(colorTrack(), colors);
        timeline.bindVisibility(0f, 5f, booleans);
        timeline.apply(3f);
        timeline.forceNextApply();
        timeline.apply(3f);
        timeline.apply(3f);
        assertEquals(2, floats.values.size());
        assertEquals(2, colors.values.size());
        assertEquals(2, booleans.values.size());
    }

    @Test
    void writingEveryApplyCallsSetterEvenWhenUnchanged() {
        Timeline timeline = Timeline.writingEveryApply();
        FloatRecorder recorder = new FloatRecorder();
        timeline.bind(new RampTrack(), recorder);
        timeline.apply(3f);
        timeline.apply(3f);
        timeline.apply(3f);
        assertEquals(3, recorder.values.size());
    }

    @Test
    void childUnderWriteEveryParentIsWrittenEveryApply() {
        Timeline parent = Timeline.writingEveryApply();
        Timeline child = new Timeline();
        FloatRecorder recorder = new FloatRecorder();
        child.bind(new RampTrack(), recorder);
        parent.addChild(child, 0f);
        parent.apply(3f);
        parent.apply(3f);
        assertEquals(2, recorder.values.size());
    }

    @Test
    void grandchildUnderWriteEveryRootIsWrittenEveryApply() {
        Timeline root = Timeline.writingEveryApply();
        Timeline child = new Timeline();
        Timeline grandchild = new Timeline();
        FloatRecorder recorder = new FloatRecorder();
        grandchild.bind(new RampTrack(), recorder);
        child.addChild(grandchild, 0f);
        root.addChild(child, 0f);
        root.apply(3f);
        root.apply(3f);
        assertEquals(2, recorder.values.size());
    }

    @Test
    void writeEveryChildUnderDefaultParentIsWrittenEveryApply() {
        Timeline parent = new Timeline();
        Timeline child = Timeline.writingEveryApply();
        FloatRecorder recorder = new FloatRecorder();
        child.bind(new RampTrack(), recorder);
        parent.addChild(child, 0f);
        parent.apply(3f);
        parent.apply(3f);
        assertEquals(2, recorder.values.size());
    }

    @Test
    void seekingBackwardsAndFarForwardHoldsEndValues() {
        Timeline timeline = new Timeline();
        FloatRecorder recorder = new FloatRecorder();
        timeline.bind(new RampTrack(), recorder);
        timeline.apply(6f);
        timeline.apply(-100f);
        timeline.apply(1000f);
        assertEquals(6f, recorder.values.get(0), EPSILON);
        assertEquals(0f, recorder.values.get(1), EPSILON);
        assertEquals(10f, recorder.values.get(2), EPSILON);
    }

    @Test
    void appliesInOrderAdded() {
        Timeline timeline = new Timeline();
        List<String> order = new ArrayList<>();
        timeline.bind(new RampTrack(), value -> order.add("float"));
        timeline.bindVisibility(0f, 1f, value -> order.add("visible"));
        timeline.bind(colorTrack(), rgba -> order.add("color"));
        timeline.apply(0.5f);
        assertEquals(List.of("float", "visible", "color"), order);
        assertEquals(3, timeline.getBindingCount());
    }

    @Test
    void duplicateSetterThrows() {
        Timeline timeline = new Timeline();
        FloatRecorder recorder = new FloatRecorder();
        timeline.bind(new RampTrack(), recorder);
        assertThrows(IllegalArgumentException.class,
                () -> timeline.bind(new RampTrack(), recorder));
        assertEquals(1, timeline.getBindingCount());
    }

    @Test
    void duplicateSetterAcrossKindsThrows() {
        Timeline timeline = new Timeline();
        DualSetter dual = new DualSetter();
        timeline.bind(new RampTrack(), (FloatSetter) dual);
        assertThrows(IllegalArgumentException.class,
                () -> timeline.bindVisibility(0f, 1f, (BooleanSetter) dual));
    }

    @Test
    void nullArgumentsThrow() {
        Timeline timeline = new Timeline();
        assertThrows(IllegalArgumentException.class,
                () -> timeline.bind((FloatTrack) null, new FloatRecorder()));
        assertThrows(IllegalArgumentException.class,
                () -> timeline.bind(new RampTrack(), (FloatSetter) null));
        assertThrows(IllegalArgumentException.class,
                () -> timeline.bind((ColorTrack) null, new ColorRecorder()));
        assertThrows(IllegalArgumentException.class,
                () -> timeline.bind(colorTrack(), (ColorSetter) null));
        assertThrows(IllegalArgumentException.class,
                () -> timeline.bindVisibility(0f, 1f, null));
    }

    @Test
    void nonFiniteTimeThrows() {
        Timeline timeline = new Timeline();
        assertThrows(IllegalArgumentException.class, () -> timeline.apply(Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> timeline.apply(Float.POSITIVE_INFINITY));
    }

    @Test
    void setterThatThrowsIsRetriedOnNextApply() {
        Timeline timeline = new Timeline();
        List<Float> written = new ArrayList<>();
        boolean[] failNext = {true};
        timeline.bind(new RampTrack(), value -> {
            if (failNext[0]) {
                failNext[0] = false;
                throw new IllegalStateException("setter failed");
            }
            written.add(value);
        });
        assertThrows(IllegalStateException.class, () -> timeline.apply(3f));
        timeline.apply(3f);
        assertEquals(List.of(3f), written);
    }

    @Test
    void visibleDuringRejectsEndBeforeStart() {
        Timeline timeline = new Timeline();
        assertThrows(IllegalArgumentException.class,
                () -> timeline.bindVisibility(2f, 1f, new BooleanRecorder()));
    }
}
