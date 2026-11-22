/**
 * Text layout and drawing that handles text longer than its area.
 *
 * <p>Truncation and scrolling: {@link com.github.halfdeadgames.asciigdx.text.TextDrawing} fits
 * one line of text into a fixed width, either cut short with an ellipsis or scrolling over time.
 * {@link com.github.halfdeadgames.asciigdx.text.TextScroll} describes the scrolling motion.
 *
 * <p>Wrapping: {@link com.github.halfdeadgames.asciigdx.text.WrapRules} holds the columns, the
 * line limit, the ellipsis, and the {@link com.github.halfdeadgames.asciigdx.text.BreakMode}.
 * {@link com.github.halfdeadgames.asciigdx.text.WrapLayout} turns a text and rules into lines
 * once, and {@code TextDrawing.drawWrapped} draws them every frame. The break mode
 * {@link com.github.halfdeadgames.asciigdx.text.BreakMode#HYPHENATE} splits long words with a
 * hyphen.
 *
 * <p>Paint: {@link com.github.halfdeadgames.asciigdx.text.TextPaint} bundles the colors with a
 * kept background, a foreground gradient, and a visible letter count for a typewriter effect.
 * The game owns one paint per text and passes it to the {@code TextDrawing} methods that take a
 * paint. {@link com.github.halfdeadgames.asciigdx.text.GradientSpan} picks the gradient range.
 *
 * <p>Inline color tags: {@link com.github.halfdeadgames.asciigdx.text.StyledText} parses
 * {@code [color:red]...[/color]} and {@code [bg:#203070]...[/bg]} tags once into visible text with
 * a color per letter, and the {@code TextDrawing} methods that take a {@code StyledText} draw it.
 * {@link com.github.halfdeadgames.asciigdx.text.ColorNames} supplies the color names; the game
 * owns them. Tags take no width, so layout runs on the visible text.
 *
 * <p>Inline effect tags: {@code [effect:wave amplitude=0.5]...[/effect]} tags are read by
 * {@link com.github.halfdeadgames.asciigdx.text.StyledText#parse(CharSequence,
 * com.github.halfdeadgames.asciigdx.text.ColorNames,
 * com.github.halfdeadgames.asciigdx.text.EffectNames)} when the game passes an
 * {@link com.github.halfdeadgames.asciigdx.text.EffectNames}, such as the registry in the zone
 * package. The result is a list of {@link com.github.halfdeadgames.asciigdx.text.EffectRun}s
 * over the visible letters. Without an {@code EffectNames}, effect tags stay visible.
 *
 * <p>Typing reveal: {@code [wait:0.5]} tags in a {@code StyledText} are read as
 * {@link com.github.halfdeadgames.asciigdx.text.RevealPause}s, see
 * {@link com.github.halfdeadgames.asciigdx.text.StyledText#getPauses()}.
 * {@link com.github.halfdeadgames.asciigdx.text.TextReveal} turns the seconds since the reveal
 * started into a count of shown letters, which the game passes to the drawing, for example
 * {@code Label.setShownLetterCount} in the UI module. For a label, build the reveal with
 * {@code Label.createReveal(lettersPerSecond)} instead of {@code TextReveal.of}.
 *
 * <p>Hyphenation:{@link com.github.halfdeadgames.asciigdx.text.Hyphenator} finds the points
 * where a word may be split, using the bundled English (US) patterns or patterns the game
 * supplies.
 *
 * <p>Conventions:
 * <ul>
 *   <li>Positions and sizes are in cells. Columns grow to the right and rows grow down.</li>
 *   <li>Text length is counted in code points, one cell each.</li>
 *   <li>Time is in seconds, passed in by the game. Nothing here reads a clock or counts
 *       frames.</li>
 *   <li>Drawing methods are called on the render thread and do not allocate.</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * // Cut short with an ellipsis.
 * TextDrawing.drawTruncated(grid, 2, 1, 12, title, TextDrawing.DEFAULT_ELLIPSIS, fg, bg);
 *
 * // Scroll back and forth at 6 cells per second, pausing 1 second at each end.
 * TextScroll scroll = TextScroll.backAndForth(6f, 1f);
 * TextDrawing.drawScrolling(grid, 2, 2, 12, title, scroll, secondsSinceStart,
 *         TextDrawing.DEFAULT_ELLIPSIS, fg, bg);
 *
 * // Wrap into columns 2 through 31, at most 4 lines, splitting long words with a hyphen.
 * WrapRules rules = WrapRules.between(2, 31).withMaxLines(4).withBreakMode(BreakMode.HYPHENATE);
 * WrapLayout layout = new WrapLayout();
 * layout.layOut(message, rules);
 * TextDrawing.drawWrapped(grid, 4, layout, fg, bg);
 *
 * // A fading gradient with a typewriter effect, keeping the background under the text.
 * TextPaint paint = TextPaint.of(fg, bg);
 * paint.setKeepBackground(true);
 * paint.setGradient(fadeColor, GradientSpan.WHOLE_TEXT);
 * paint.setVisibleLetterCount((int) (secondsSinceStart * 20f));
 * TextDrawing.drawText(grid, 2, 8, message, paint);
 *
 * // Inline colors, parsed once and drawn every frame.
 * StyledText tagged = StyledText.parse("The [color:red]quick[/color] fox", names);
 * TextDrawing.drawText(grid, 2, 9, tagged, TextPaint.of(fg, bg));
 *
 * // Inline effects, resolved at parse time into runs of letters.
 * StyledText wavy = StyledText.parse("A [effect:wave]wavy[/effect] word", names,
 *         EffectRegistry.withBuiltIns());
 *
 * // Typing reveal of grid text at 20 letters per second with a half second pause.
 * StyledText speech = StyledText.parse("Well.[wait:0.5] Here we go.", names);
 * TextReveal reveal = TextReveal.of(speech, 20f);
 * paint.setVisibleLetterCount(reveal.getShownLetterCount(secondsSinceStart)); // each frame
 * TextDrawing.drawText(grid, 2, 10, speech, paint);
 * }</pre>
 */
package com.github.halfdeadgames.asciigdx.text;
