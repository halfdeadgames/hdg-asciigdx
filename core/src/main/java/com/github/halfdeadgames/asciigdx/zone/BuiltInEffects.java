package com.github.halfdeadgames.asciigdx.zone;

/** The tag settings and defaults of the {@link CellEffects} factories. */
final class BuiltInEffects {

    private static final String PERIOD = "period";
    private static final String PHASE = "phase";
    private static final String AMPLITUDE = "amplitude";
    private static final String AMOUNT = "amount";
    private static final String DEFAULT_GLYPHS = "|/-\\";
    private static final int DEFAULT_FLASH_RGBA = 0xFFFFFFFF;

    private BuiltInEffects() {
    }

    static void registerAll(EffectRegistry registry) {
        registry.register("wave", settings -> CellEffects.wave(
                settings.getFloat(AMPLITUDE, 0.25f), settings.getFloat(PERIOD, 1f),
                phase(settings)));
        registry.register("bounce", settings -> CellEffects.bounce(
                settings.getFloat("height", 0.5f), settings.getFloat(PERIOD, 0.8f),
                phase(settings)));
        registry.register("shake", BuiltInEffects::createShake);
        registry.register("pulse", settings -> CellEffects.pulse(
                settings.getFloat(AMOUNT, 0.2f), settings.getFloat(PERIOD, 1f),
                phase(settings)));
        registry.register("spin", settings -> CellEffects.spin(
                settings.getFloat("degrees", 90f), phase(settings)));
        registry.register("flash", BuiltInEffects::createFlash);
        registry.register("rainbow", settings -> CellEffects.rainbow(
                settings.getFloat(AMOUNT, 1f), settings.getFloat(PERIOD, 2f),
                phase(settings)));
        registry.register("glyphcycle", BuiltInEffects::createGlyphCycle);
    }

    // Each setting is read into a local first, so all of them are read even if one is bad.
    private static CellEffect createShake(EffectSettings settings) {
        float amplitude = settings.getFloat(AMPLITUDE, 0.1f);
        float stepsPerSecond = settings.getFloat("steps", 12f);
        int seed = settings.getInt("seed", 0);
        return CellEffects.shake(amplitude, stepsPerSecond, seed);
    }

    private static CellEffect createFlash(EffectSettings settings) {
        int rgba = settings.getRgba("color", DEFAULT_FLASH_RGBA);
        float amount = settings.getFloat(AMOUNT, 1f);
        float periodSeconds = settings.getFloat(PERIOD, 1f);
        return CellEffects.flash(rgba, amount, periodSeconds, phase(settings));
    }

    private static CellEffect createGlyphCycle(EffectSettings settings) {
        float periodSeconds = settings.getFloat(PERIOD, 1f);
        Phase phase = phase(settings);
        int[] codePoints = settings.getString("glyphs", DEFAULT_GLYPHS).codePoints().toArray();
        return CellEffects.glyphCycle(periodSeconds, phase, codePoints);
    }

    private static Phase phase(EffectSettings settings) {
        return settings.getPhase(PHASE, Phase.sync());
    }
}
