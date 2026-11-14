package com.github.halfdeadgames.asciigdx.color;

/**
 * Full-precision Oklab blending of 8-bit sRGB colors. Oklab by Björn Ottosson, matrices from
 * 2021-01-25.
 */
final class OklabMix {

    private static final double SRGB_DECODE_THRESHOLD = 0.04045;
    private static final double SRGB_ENCODE_THRESHOLD = 0.0031308;

    private OklabMix() {
    }

    /**
     * Blends the RGB parts of two RGBA8888 colors in Oklab.
     *
     * @param fromRgba color at amount 0
     * @param toRgba color at amount 1
     * @param amount mix factor
     * @return blended color as 0xRRGGBB00 (alpha byte 0)
     */
    static int mixRgb(int fromRgba, int toRgba, float amount) {
        double fromRed = toLinear(fromRgba >>> 24);
        double fromGreen = toLinear((fromRgba >>> 16) & 0xFF);
        double fromBlue = toLinear((fromRgba >>> 8) & 0xFF);
        double toRed = toLinear(toRgba >>> 24);
        double toGreen = toLinear((toRgba >>> 16) & 0xFF);
        double toBlue = toLinear((toRgba >>> 8) & 0xFF);

        double fromL = cbrtL(fromRed, fromGreen, fromBlue);
        double fromM = cbrtM(fromRed, fromGreen, fromBlue);
        double fromS = cbrtS(fromRed, fromGreen, fromBlue);
        double toL = cbrtL(toRed, toGreen, toBlue);
        double toM = cbrtM(toRed, toGreen, toBlue);
        double toS = cbrtS(toRed, toGreen, toBlue);

        double fromLightness = oklabL(fromL, fromM, fromS);
        double fromA = oklabA(fromL, fromM, fromS);
        double fromB = oklabB(fromL, fromM, fromS);
        double toLightness = oklabL(toL, toM, toS);
        double toA = oklabA(toL, toM, toS);
        double toB = oklabB(toL, toM, toS);

        double lightness = fromLightness + (toLightness - fromLightness) * amount;
        double a = fromA + (toA - fromA) * amount;
        double b = fromB + (toB - fromB) * amount;

        double lCubed = cube(lightness + 0.3963377774 * a + 0.2158037573 * b);
        double mCubed = cube(lightness - 0.1055613458 * a - 0.0638541728 * b);
        double sCubed = cube(lightness - 0.0894841775 * a - 1.2914855480 * b);

        double red = 4.0767416621 * lCubed - 3.3077115913 * mCubed + 0.2309699292 * sCubed;
        double green = -1.2684380046 * lCubed + 2.6097574011 * mCubed - 0.3413193965 * sCubed;
        double blue = -0.0041960863 * lCubed - 0.7034186147 * mCubed + 1.7076147010 * sCubed;

        return (toChannel(red) << 24) | (toChannel(green) << 16) | (toChannel(blue) << 8);
    }

    private static double cbrtL(double red, double green, double blue) {
        return Math.cbrt(0.4122214708 * red + 0.5363325363 * green + 0.0514459929 * blue);
    }

    private static double cbrtM(double red, double green, double blue) {
        return Math.cbrt(0.2119034982 * red + 0.6806995451 * green + 0.1073969566 * blue);
    }

    private static double cbrtS(double red, double green, double blue) {
        return Math.cbrt(0.0883024619 * red + 0.2817188376 * green + 0.6299787005 * blue);
    }

    private static double oklabL(double l, double m, double s) {
        return 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s;
    }

    private static double oklabA(double l, double m, double s) {
        return 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s;
    }

    private static double oklabB(double l, double m, double s) {
        return 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s;
    }

    private static double cube(double value) {
        return value * value * value;
    }

    private static double toLinear(int channel) {
        double normalized = channel / 255.0;
        if (normalized <= SRGB_DECODE_THRESHOLD) {
            return normalized / 12.92;
        }
        return Math.pow((normalized + 0.055) / 1.055, 2.4);
    }

    private static int toChannel(double linear) {
        double encoded = linear <= SRGB_ENCODE_THRESHOLD
            ? 12.92 * linear
            : 1.055 * Math.pow(linear, 1.0 / 2.4) - 0.055;
        double clamped = Math.max(0.0, Math.min(1.0, encoded));
        return (int) Math.round(clamped * 255.0);
    }
}
