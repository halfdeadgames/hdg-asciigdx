/**
 * Color helpers for the RGBA8888 ints used across the engine. A color is one {@code int} laid out
 * as {@code 0xRRGGBBAA}: red in the highest byte, then green, then blue, with alpha in the lowest
 * byte (0 is transparent, 255 is opaque). {@link com.github.halfdeadgames.asciigdx.color.CellColor}
 * packs, adjusts, inverts, and mixes such colors, and
 * {@link com.github.halfdeadgames.asciigdx.color.HueColor} builds saturated colors from a hue.
 *
 * <p>Pitfall: a 6-digit hex literal such as {@code 0xFF4040} is an int with value
 * {@code 0x00FF4040}, which reads as red 0, green 255, blue 64, and alpha 64. Write 8 digits
 * ({@code 0xFF4040FF}) or wrap the literal with
 * {@link com.github.halfdeadgames.asciigdx.color.CellColor#rgb}.
 *
 * <p>All methods in this package are pure, allocation-free, and safe to call from any thread.
 *
 * <pre>{@code
 * int red = CellColor.rgb(0xFF4040);
 * int translucentRed = CellColor.withAlpha(red, 128);
 * int blended = CellColor.mixOklab(red, HueColor.fromHue(2f / 3f), 0.5f);
 * }</pre>
 */
package com.github.halfdeadgames.asciigdx.color;
