package com.github.halfdeadgames.asciigdx.workbench.ui;

/** How large the UI is drawn: a whole-number factor on the 8x8 font. Sizes derived from the cell stay whole pixels. */
public record UiScale(int factor) {

    public static final int MIN_FACTOR = 1;
    public static final int MAX_FACTOR = 4;
    public static final int DEFAULT_FACTOR = 3;
    public static final UiScale DEFAULT = new UiScale(DEFAULT_FACTOR);

    private static final int FONT_PIXELS = 8;

    public UiScale {
        if (factor < MIN_FACTOR || factor > MAX_FACTOR) {
            throw new IllegalArgumentException("UI scale factor must be " + MIN_FACTOR + ".." + MAX_FACTOR + ": " + factor);
        }
    }

    /** Pixels of one text cell. */
    public int cell() {
        return FONT_PIXELS * factor;
    }

    public int half() {
        return cell() / 2;
    }

    /** A length given in pixels of the unscaled font. */
    public int px(int pixelsAt1x) {
        return pixelsAt1x * factor;
    }

    /** A length tuned at the default factor, rounded half up and exact at the default. Positive lengths stay at least 1; zero and negative ones are returned unchanged. */
    public int scaled(int pixelsAtDefault) {
        if (pixelsAtDefault <= 0) {
            return pixelsAtDefault;
        }
        return Math.max(1, (2 * pixelsAtDefault * factor + DEFAULT_FACTOR) / (2 * DEFAULT_FACTOR));
    }
}
