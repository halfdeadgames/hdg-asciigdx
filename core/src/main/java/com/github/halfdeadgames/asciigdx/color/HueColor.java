package com.github.halfdeadgames.asciigdx.color;

/**
 * Builds fully saturated, full-brightness colors from a hue, as opaque RGBA8888 ints
 * ({@code 0xRRGGBBAA}). The methods are pure, allocation-free, and safe to call from any thread.
 *
 * <p>Example: {@code int green = HueColor.fromHue(1f / 3f);} gives {@code 0x00FF00FF}.
 */
public final class HueColor {

    private static final int SECTORS = 6;
    private static final float MAX_CHANNEL = 255f;
    private static final int OPAQUE = 255;

    private HueColor() {
    }

    /**
     * Returns the color at {@code hueCycles} around the color wheel: 0 is red, 1/3 is green, and
     * 2/3 is blue, with full saturation and full brightness. Any finite value is accepted and
     * wraps around, so 1.25 and -0.75 both give the same color as 0.25.
     *
     * @param hueCycles hue in cycles around the wheel, where 1 is one full turn; must be finite
     * @return the opaque color as RGBA8888 ({@code 0xRRGGBBFF})
     * @throws IllegalArgumentException if {@code hueCycles} is NaN or infinite
     */
    public static int fromHue(float hueCycles) {
        if (Float.isNaN(hueCycles) || Float.isInfinite(hueCycles)) {
            throw new IllegalArgumentException("hueCycles must be finite: " + hueCycles);
        }
        float wrapped = hueCycles - (float) Math.floor(hueCycles);
        float scaled = wrapped * SECTORS;
        int sector = Math.min((int) scaled, SECTORS - 1);
        float rising = scaled - sector;
        float falling = 1f - rising;
        return switch (sector) {
            case 0 -> pack(1f, rising, 0f);
            case 1 -> pack(falling, 1f, 0f);
            case 2 -> pack(0f, 1f, rising);
            case 3 -> pack(0f, falling, 1f);
            case 4 -> pack(rising, 0f, 1f);
            default -> pack(1f, 0f, falling);
        };
    }

    private static int pack(float red, float green, float blue) {
        return CellColor.rgba(toChannel(red), toChannel(green), toChannel(blue), OPAQUE);
    }

    private static int toChannel(float fraction) {
        return Math.round(fraction * MAX_CHANNEL);
    }
}
