package com.github.halfdeadgames.asciigdx.tween;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds tracks to property setters and writes their values for a given time.
 *
 * <p>Every binding applies at any time: tracks hold their first value before their span and their
 * last value after it, so seeking to any time gives the correct state. Give each property one
 * track. To chain steps, join them with {@link TweenSequence} instead of binding two tracks to the
 * same property, because the later binding would always win.
 *
 * <p>Binding the same setter instance twice throws {@link IllegalArgumentException}, across all
 * binding kinds. The check is by identity, so it only catches reuse of the same stored setter
 * reference: each {@code layer::setZoom} expression creates a new object.
 *
 * <p>Each binding remembers its last written value and calls its setter only when the new value
 * differs. The first apply after binding always writes. If other code changes the same property,
 * the binding will not write it back until its own value changes. Call
 * {@link #forceNextApply()} then.
 *
 * <p>A timeline made with {@link #writingEveryApply()} skips nothing: each apply forces every
 * binding, so every setter is called on every apply, even when the value did not change. Use it for
 * values written to a frame handle ({@code Layer.obtainFrameCellHandle} or
 * {@code MapWindow.obtainFrameCellHandle}). {@code LayerStack.beginCellEffects} resets frame
 * handles every frame, so a skipping timeline's effect would vanish after one frame. Such a
 * timeline must be applied every frame, after {@code LayerStack.beginCellEffects}. The cost is one
 * setter call per binding per apply.
 *
 * <p>A staggered binding writes one value per index and skips unchanged values per index. A child
 * timeline added with {@link #add(Timeline, float)} runs on local time and is forced along with
 * its parent: a child under a write-every parent is written on every apply, and a write-every
 * child forces itself under any parent. The duplicate-setter check does not look inside children.
 *
 * <p>{@link #apply(float)} does not allocate.
 */
public final class Timeline {

    private final List<Binding> bindings = new ArrayList<>();
    private final boolean writesEveryApply;
    private boolean hasParent;

    /**
     * Creates an empty timeline that skips setter calls for unchanged values. Not thread-safe;
     * use it from one thread, normally the game loop.
     */
    public Timeline() {
        this(false);
    }

    private Timeline(boolean writesEveryApply) {
        this.writesEveryApply = writesEveryApply;
    }

    /**
     * Creates an empty timeline that calls every setter on every {@link #apply(float)}, even when
     * the value is unchanged. Use it for values written to a frame handle, which is reset every
     * frame; apply it every frame after {@code LayerStack.beginCellEffects}. Not thread-safe.
     *
     * <p>Bind a setter that requests the frame handle on every write, as in the example below,
     * instead of a setter that holds one handle. A frame handle whose values stay at their
     * defaults counts as unused and is released, and a scroll or grid shrink detaches it; a held
     * handle then throws on the next write. Requesting an existing frame handle does not allocate.
     *
     * <pre>{@code
     * Timeline timeline = Timeline.writingEveryApply();
     * timeline.bind(track, degrees -> layer.obtainFrameCellHandle(column, row).setRotation(degrees));
     * // each frame, after LayerStack.beginCellEffects
     * timeline.apply(elapsedSeconds);
     * }</pre>
     *
     * @return a new, empty timeline in write-every-apply mode
     */
    public static Timeline writingEveryApply() {
        return new Timeline(true);
    }

    /**
     * Writes the track value to the setter on every apply.
     *
     * @throws IllegalArgumentException if an argument is null or the setter is already bound
     */
    public void bind(FloatTrack track, FloatSetter setter) {
        requireNonNull("track", track);
        requireNonNull("setter", setter);
        addBinding(new FloatBinding(track, setter));
    }

    /**
     * Writes the track color (RGBA8888) to the setter on every apply.
     *
     * @throws IllegalArgumentException if an argument is null or the setter is already bound
     */
    public void bind(ColorTrack track, ColorSetter setter) {
        requireNonNull("track", track);
        requireNonNull("setter", setter);
        addBinding(new ColorBinding(track, setter));
    }

    /**
     * Writes {@code track.valueAt(index, time)} to the setter for every index from 0 to
     * {@code count - 1}. Skipping unchanged values is tracked per index.
     *
     * @throws IllegalArgumentException if an argument is null, count is below 1, or the setter is
     *     already bound
     */
    public void bind(StaggeredTrack track, int count, IndexedFloatSetter setter) {
        requireNonNull("track", track);
        requireNonNull("setter", setter);
        if (count < 1) {
            throw new IllegalArgumentException("count must be >= 1: " + count);
        }
        addBinding(new StaggeredBinding(track, count, setter));
    }

    /**
     * Writes true while {@code startSeconds <= time < endSeconds}, false otherwise. Equal start
     * and end means never visible.
     *
     * @throws IllegalArgumentException if a time is not finite, end is before start, the setter is
     *     null, or the setter is already bound
     */
    public void visibleDuring(float startSeconds, float endSeconds, BooleanSetter setter) {
        TweenChecks.requireFinite("startSeconds", startSeconds);
        TweenChecks.requireFinite("endSeconds", endSeconds);
        if (endSeconds < startSeconds) {
            throw new IllegalArgumentException(
                    "endSeconds must not be before startSeconds: " + endSeconds + " < "
                            + startSeconds);
        }
        requireNonNull("setter", setter);
        addBinding(new VisibilityBinding(startSeconds, endSeconds, setter));
    }

    /**
     * Adds a child timeline that is applied with local time {@code time - startSeconds}, in the
     * order added among the bindings. The child applies at every time, so before its start its
     * tracks hold their first value. The duplicate-setter check does not look inside child
     * timelines.
     *
     * <p>A timeline can have only one parent and be added once. Two parents would share its
     * memory of last written values and overwrite each other.
     *
     * @throws IllegalArgumentException if the child is null, this timeline, already has a parent,
     *     or already contains this timeline (a cycle), or if startSeconds is not finite
     */
    public void add(Timeline child, float startSeconds) {
        requireNonNull("child", child);
        TweenChecks.requireFinite("startSeconds", startSeconds);
        if (child.hasParent) {
            throw new IllegalArgumentException("child already has a parent");
        }
        if (child == this || child.contains(this)) {
            throw new IllegalArgumentException("child must not contain this timeline");
        }
        addBinding(new ChildTimelineBinding(child, startSeconds));
        child.hasParent = true;
    }

    /**
     * Applies every binding, in the order added. A timeline made with {@link #writingEveryApply()}
     * writes every binding; any other writes only changed values.
     *
     * @throws IllegalArgumentException if the time is not finite
     */
    public void apply(float timeSeconds) {
        TweenChecks.requireFinite("timeSeconds", timeSeconds);
        if (writesEveryApply) {
            forceNextApply();
        }
        for (int i = 0; i < bindings.size(); i++) {
            bindings.get(i).applyAt(timeSeconds);
        }
    }

    /** Makes the next {@link #apply(float)} write every binding even if unchanged. */
    public void forceNextApply() {
        for (int i = 0; i < bindings.size(); i++) {
            bindings.get(i).forceNext();
        }
    }

    /** Returns the number of bindings; a child timeline counts as one. */
    public int bindingCount() {
        return bindings.size();
    }

    private boolean contains(Timeline target) {
        for (int i = 0; i < bindings.size(); i++) {
            if (bindings.get(i) instanceof ChildTimelineBinding childBinding) {
                Timeline child = childBinding.child();
                if (child == target || child.contains(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addBinding(Binding binding) {
        if (isSetterBound(binding.setter())) {
            throw new IllegalArgumentException("setter is already bound: " + binding.setter());
        }
        bindings.add(binding);
    }

    private boolean isSetterBound(Object setter) {
        for (int i = 0; i < bindings.size(); i++) {
            if (bindings.get(i).setter() == setter) {
                return true;
            }
        }
        return false;
    }

    private static void requireNonNull(String name, Object value) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }
}
