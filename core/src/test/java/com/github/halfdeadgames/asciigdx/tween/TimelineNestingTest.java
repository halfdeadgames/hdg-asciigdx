package com.github.halfdeadgames.asciigdx.tween;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimelineNestingTest {

    private static final float EPSILON = 1e-5f;

    /** Linear ramp from 0 at t=0 to 10 at t=10, holding both ends. */
    private static final class RampTrack implements FloatTrack {

        @Override
        public float valueAt(float timeSeconds) {
            return Math.max(0f, Math.min(10f, timeSeconds));
        }

        @Override
        public float start() {
            return 0f;
        }

        @Override
        public float end() {
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

    private static Timeline rampTimeline(FloatRecorder recorder) {
        Timeline timeline = new Timeline();
        timeline.bind(new RampTrack(), recorder);
        return timeline;
    }

    @Test
    void childReceivesLocalTime() {
        FloatRecorder recorder = new FloatRecorder();
        Timeline parent = new Timeline();
        parent.add(rampTimeline(recorder), 2f);
        parent.apply(5f);
        assertEquals(3f, recorder.values.get(0), EPSILON);
    }

    @Test
    void childHoldsFirstValueBeforeStart() {
        FloatRecorder recorder = new FloatRecorder();
        Timeline parent = new Timeline();
        parent.add(rampTimeline(recorder), 4f);
        parent.apply(1f);
        assertEquals(1, recorder.values.size());
        assertEquals(0f, recorder.values.get(0), EPSILON);
    }

    @Test
    void negativeStartShiftsLocalTimeForward() {
        FloatRecorder recorder = new FloatRecorder();
        Timeline parent = new Timeline();
        parent.add(rampTimeline(recorder), -3f);
        parent.apply(1f);
        assertEquals(4f, recorder.values.get(0), EPSILON);
    }

    @Test
    void nonFiniteStartThrows() {
        Timeline parent = new Timeline();
        assertThrows(IllegalArgumentException.class,
                () -> parent.add(new Timeline(), Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> parent.add(new Timeline(), Float.POSITIVE_INFINITY));
    }

    @Test
    void nestedTwoLevelsAddsOffsets() {
        FloatRecorder recorder = new FloatRecorder();
        Timeline inner = rampTimeline(recorder);
        Timeline middle = new Timeline();
        middle.add(inner, 1f);
        Timeline outer = new Timeline();
        outer.add(middle, 2f);
        outer.apply(6f);
        assertEquals(3f, recorder.values.get(0), EPSILON);
    }

    @Test
    void forceNextApplyPropagatesToChildren() {
        FloatRecorder recorder = new FloatRecorder();
        Timeline parent = new Timeline();
        parent.add(rampTimeline(recorder), 0f);
        parent.apply(50f);
        parent.apply(50f);
        assertEquals(1, recorder.values.size());

        parent.forceNextApply();
        parent.apply(50f);
        assertEquals(2, recorder.values.size());
    }

    @Test
    void childCountsAsOneBinding() {
        Timeline child = new Timeline();
        child.bind(new RampTrack(), new FloatRecorder());
        child.bind(new RampTrack(), new FloatRecorder());
        Timeline parent = new Timeline();
        parent.add(child, 0f);
        assertEquals(1, parent.bindingCount());
    }

    @Test
    void nullChildThrows() {
        Timeline parent = new Timeline();
        assertThrows(IllegalArgumentException.class, () -> parent.add(null, 0f));
    }

    @Test
    void addingSelfThrows() {
        Timeline timeline = new Timeline();
        assertThrows(IllegalArgumentException.class, () -> timeline.add(timeline, 0f));
        assertEquals(0, timeline.bindingCount());
    }

    @Test
    void addingSameChildTwiceThrows() {
        Timeline parent = new Timeline();
        Timeline child = new Timeline();
        parent.add(child, 0f);
        assertThrows(IllegalArgumentException.class, () -> parent.add(child, 1f));
        assertEquals(1, parent.bindingCount());
    }

    @Test
    void addingChildToSecondParentThrows() {
        Timeline child = new Timeline();
        new Timeline().add(child, 0f);
        Timeline secondParent = new Timeline();
        assertThrows(IllegalArgumentException.class, () -> secondParent.add(child, 0f));
        assertEquals(0, secondParent.bindingCount());
    }

    @Test
    void rejectedAddLeavesChildFreeForAnotherParent() {
        Timeline child = new Timeline();
        assertThrows(IllegalArgumentException.class, () -> child.add(child, 0f));
        Timeline parent = new Timeline();
        parent.add(child, 0f);
        assertEquals(1, parent.bindingCount());
    }

    @Test
    void directCycleThrows() {
        Timeline first = new Timeline();
        Timeline second = new Timeline();
        first.add(second, 0f);
        assertThrows(IllegalArgumentException.class, () -> second.add(first, 0f));
        assertEquals(0, second.bindingCount());
    }

    @Test
    void deepCycleThrows() {
        Timeline top = new Timeline();
        Timeline middle = new Timeline();
        Timeline bottom = new Timeline();
        top.add(middle, 0f);
        middle.add(bottom, 0f);
        assertThrows(IllegalArgumentException.class, () -> bottom.add(top, 0f));
        assertThrows(IllegalArgumentException.class, () -> bottom.add(middle, 0f));
    }

    @Test
    void appliesBindingsAndChildrenInOrderAdded() {
        List<String> order = new ArrayList<>();
        Timeline first = new Timeline();
        first.bind(new RampTrack(), value -> order.add("first-child"));
        Timeline second = new Timeline();
        second.bind(new RampTrack(), value -> order.add("second-child"));

        Timeline parent = new Timeline();
        parent.bind(new RampTrack(), value -> order.add("before"));
        parent.add(first, 0f);
        parent.visibleDuring(0f, 1f, value -> order.add("between"));
        parent.add(second, 0f);
        parent.bind(new RampTrack(), value -> order.add("after"));
        parent.apply(0.5f);

        assertEquals(
                List.of("before", "first-child", "between", "second-child", "after"), order);
        assertEquals(5, parent.bindingCount());
    }
}
