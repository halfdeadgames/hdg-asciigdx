package com.github.halfdeadgames.asciigdx.tween;

import com.badlogic.gdx.math.Interpolation;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * Times one frame's CPU work for many tweens and timelines ticking at once. Each benchmark call is
 * one frame: the clock advances by a fixed step, then every tween is queried or every timeline is
 * applied. Results are microseconds per frame for all tweens together. The clock loops, so
 * tweens never all finish and the work stays steady.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class TweenTickBenchmark {

    private static final float FRAME_STEP_SECONDS = 1f / 60f;
    private static final float LOOP_SECONDS = 10f;
    private static final float MAX_DELTA_SECONDS = 0.25f;
    private static final float TWEEN_DURATION_SECONDS = 4f;
    private static final float START_SPREAD_SECONDS = 5f;
    private static final float PIECE_DURATION_SECONDS = 1.5f;
    private static final float FROM_VALUE = 0f;
    private static final float TO_VALUE = 100f;
    private static final int BINDINGS_PER_CHILD = 10;

    /** Number of tweens (or sequences) ticking per frame. */
    @Param({"100", "1000", "10000"})
    public int tweenCount;

    private TweenClock clock;
    private Tween[] tweens;
    private float[] sink;
    private Timeline flatTimeline;
    private Timeline nestedTimeline;

    @Setup
    public void createTweensAndTimelines() {
        clock = TweenClock.looping(LOOP_SECONDS, MAX_DELTA_SECONDS);
        sink = new float[tweenCount];
        tweens = createTweens();
        flatTimeline = createFlatTimeline();
        nestedTimeline = createNestedTimeline();
    }

    /** Queries every independent tween once per frame, as a game reading values by hand does. */
    @Benchmark
    public float queryIndependentTweens() {
        clock.update(FRAME_STEP_SECONDS);
        float time = clock.getTimeSeconds();
        float total = 0f;
        for (int i = 0; i < tweens.length; i++) {
            total += tweens[i].getValueAt(time);
        }
        return total;
    }

    /** Applies one timeline with a tween bound to each setter; every value changes per frame. */
    @Benchmark
    public float applyFlatTimeline() {
        clock.update(FRAME_STEP_SECONDS);
        flatTimeline.apply(clock.getTimeSeconds());
        return sink[0];
    }

    /**
     * Applies a parent timeline whose children each hold several multi-piece sequences, so the
     * parent forwards local time to every child.
     */
    @Benchmark
    public float applyNestedTimelinesWithSequences() {
        clock.update(FRAME_STEP_SECONDS);
        nestedTimeline.apply(clock.getTimeSeconds());
        return sink[0];
    }

    private Tween[] createTweens() {
        Tween[] created = new Tween[tweenCount];
        for (int i = 0; i < tweenCount; i++) {
            created[i] = Tween.eased(startFor(i), TWEEN_DURATION_SECONDS, FROM_VALUE, TO_VALUE,
                    Interpolation.smooth);
        }
        return created;
    }

    private Timeline createFlatTimeline() {
        Timeline timeline = new Timeline();
        for (int i = 0; i < tweenCount; i++) {
            timeline.bind(tweens[i], sinkSetterFor(i));
        }
        return timeline;
    }

    private Timeline createNestedTimeline() {
        Timeline parent = new Timeline();
        int childCount = Math.max(1, tweenCount / BINDINGS_PER_CHILD);
        int sinkIndex = 0;
        for (int child = 0; child < childCount; child++) {
            Timeline childTimeline = new Timeline();
            for (int binding = 0; binding < BINDINGS_PER_CHILD && sinkIndex < tweenCount;
                    binding++) {
                childTimeline.bind(createSequence(), sinkSetterFor(sinkIndex));
                sinkIndex++;
            }
            parent.addChild(childTimeline, startFor(child));
        }
        return parent;
    }

    private TweenSequence createSequence() {
        return TweenSequence.startingAt(0f, FROM_VALUE)
                .to(TO_VALUE, PIECE_DURATION_SECONDS)
                .hold(PIECE_DURATION_SECONDS)
                .to(FROM_VALUE, PIECE_DURATION_SECONDS, Interpolation.smooth)
                .build();
    }

    /** Returns a distinct setter instance, because a timeline rejects a setter bound twice. */
    private FloatSetter sinkSetterFor(int index) {
        return value -> sink[index] = value;
    }

    private static float startFor(int index) {
        return (index % 50) * (START_SPREAD_SECONDS / 50f);
    }
}
