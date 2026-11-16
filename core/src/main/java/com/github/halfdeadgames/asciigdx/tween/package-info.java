/**
 * Time-based value animation for floats and colors. A {@link Tween} changes one value between two
 * numbers with an easing curve, and a {@link TweenSequence} chains several changes and holds into
 * one {@link FloatTrack}. A {@link ColorTrack} mixes two colors, and a {@link StaggeredTrack}
 * plays one track with a delay per item. Tracks are pure functions of time, so any time can be
 * queried in any order. A {@link Timeline} binds tracks to setters ({@link FloatSetter},
 * {@link ColorSetter}, {@link IndexedFloatSetter}, and {@link BooleanSetter}) and writes their
 * values for the time passed to {@link Timeline#apply}. Time is in seconds, passed in by the game;
 * nothing reads a global clock and nothing counts frames. A {@link TweenClock} is an optional
 * clock the game advances by the frame delta, with a delta cap and an optional loop length.
 *
 * <p>Example:
 * <pre>{@code
 * TweenClock clock = TweenClock.looping(8f, 0.1f);
 * Timeline timeline = new Timeline();
 * timeline.bind(Tween.eased(1f, 0.5f, 0f, 1f, Interpolation.pow2Out), layer::setZoom);
 * // each frame:
 * clock.update(deltaSeconds);
 * timeline.apply(clock.getTimeSeconds());
 * }</pre>
 */
package com.github.halfdeadgames.asciigdx.tween;
