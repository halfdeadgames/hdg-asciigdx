package com.github.halfdeadgames.asciigdx.zone;

/**
 * Builds a {@link CellEffect} from the settings of one effect tag. Register it with
 * {@link EffectRegistry#register}.
 *
 * <p>It is called while a text is parsed, not every frame. It must read every setting it supports
 * from the {@link EffectSettings}, because the registry rejects any setting it never read.
 *
 * <p>Example:
 * <pre>{@code
 * EffectFactory slowWave = settings -> CellEffects.wave(settings.getFloat("amplitude", 0.3f),
 *         4f, settings.getPhase("phase", Phase.sync()));
 * }</pre>
 */
@FunctionalInterface
public interface EffectFactory {

    /**
     * Builds the effect.
     *
     * @param settings the settings of the tag; not {@code null}; invalid once this method returns,
     *         so do not keep it
     * @return the effect; must not be {@code null}
     * @throws IllegalArgumentException if a setting has a bad value; the tag then stays visible.
     *         This is the only exception that means bad user text. Returning {@code null} or
     *         throwing anything else is a bug in the game and propagates out of
     *         {@code StyledText.parse} (a {@code null} result as a {@link NullPointerException})
     */
    CellEffect create(EffectSettings settings);
}
