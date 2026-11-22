package com.github.halfdeadgames.asciigdx.zone;

import com.github.halfdeadgames.asciigdx.text.EffectNames;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The effects that the tags of a {@link com.github.halfdeadgames.asciigdx.text.StyledText} can
 * use, by name. {@link #withBuiltIns()} knows the effects of {@link CellEffects}; a game adds its
 * own with {@link #register}. Build the registry once, during setup. It is not thread-safe while
 * names are being added; afterwards, reading it from several threads is safe as long as nobody
 * adds names.
 *
 * <p>Built-in effects and their settings (a missing setting uses the default shown; {@code phase}
 * is {@code sync}, {@code columns:N}, {@code distance:N}, or {@code scattered:SEED}, and defaults
 * to {@code sync}):
 * <ul>
 *   <li>{@code wave}: {@code amplitude} 0.25 cells, {@code period} 1 s, {@code phase}</li>
 *   <li>{@code bounce}: {@code height} 0.5 cells, {@code period} 0.8 s, {@code phase}</li>
 *   <li>{@code shake}: {@code amplitude} 0.1 cells, {@code steps} 12 per second, {@code seed}
 *       (whole number) 0</li>
 *   <li>{@code pulse}: {@code amount} 0.2, {@code period} 1 s, {@code phase}</li>
 *   <li>{@code spin}: {@code degrees} 90 per second, {@code phase}</li>
 *   <li>{@code flash}: {@code color} {@code FFFFFF} (RRGGBB or RRGGBBAA hex, no {@code #}),
 *       {@code amount} 1, {@code period} 1 s, {@code phase}</li>
 *   <li>{@code rainbow}: {@code amount} 1, {@code period} 2 s, {@code phase}</li>
 *   <li>{@code glyphcycle}: {@code period} 1 s for the whole loop, {@code phase}, {@code glyphs}
 *       {@code |/-\} (each code point is one frame; no spaces)</li>
 * </ul>
 * The meaning and range of each number is the one of the matching {@link CellEffects} factory.
 *
 * <p>Example:
 * <pre>{@code
 * EffectRegistry registry = EffectRegistry.withBuiltIns();
 * registry.register("tilt", settings -> CellEffects.spin(settings.getFloat("degrees", 10f),
 *         settings.getPhase("phase", Phase.sync())));
 * StyledText text = StyledText.parse("[effect:tilt degrees=30]slant[/effect]", names, registry);
 * }</pre>
 */
public final class EffectRegistry implements EffectNames {

    private static final String VALID_NAME_RULE = "lowercase letters and digits";

    private final Map<String, EffectFactory> factories = new HashMap<>();

    private EffectRegistry() {
    }

    /**
     * Creates a registry with the built-in effects {@code wave}, {@code bounce}, {@code shake},
     * {@code pulse}, {@code spin}, {@code flash}, {@code rainbow}, and {@code glyphcycle}. Their
     * settings and defaults are listed in {@link EffectRegistry}.
     *
     * @return a new registry; each call returns a separate one
     */
    public static EffectRegistry withBuiltIns() {
        EffectRegistry registry = new EffectRegistry();
        BuiltInEffects.registerAll(registry);
        return registry;
    }

    /**
     * Creates a registry that knows no effects. Use it to reuse a built-in name for your own
     * effect: register every effect you want, your own version of the name included.
     * {@link #withBuiltIns()} followed by {@link #register} cannot override a built-in, because
     * the name is already taken.
     *
     * @return a new, empty registry
     */
    public static EffectRegistry empty() {
        return new EffectRegistry();
    }

    /**
     * Adds an effect under a name, so {@code [effect:NAME]} uses it. A registration never changes
     * texts that were already parsed, and the order of {@code register} calls does not matter,
     * because names are unique.
     *
     * @param name the tag name: one or more lowercase ASCII letters or digits; not {@code null}
     * @param factory builds the effect from the settings of a tag; not {@code null}
     * @throws IllegalArgumentException if the name is empty, holds anything but lowercase ASCII
     *         letters and digits, or is already registered, built-in names included
     * @throws NullPointerException if an argument is {@code null}
     */
    public void register(String name, EffectFactory factory) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(factory, "factory");
        if (!isValidName(name)) {
            throw new IllegalArgumentException(
                    "Effect name must be " + VALID_NAME_RULE + ": '" + name + "'");
        }
        if (factories.putIfAbsent(name, factory) != null) {
            throw new IllegalArgumentException("Effect name is already registered: " + name);
        }
    }

    /**
     * Builds the effect with a name from the settings of a tag.
     *
     * @param name the effect name; not {@code null}
     * @param settings the setting texts; not {@code null}
     * @return the effect, or empty when the name is not registered
     * @throws IllegalArgumentException if a setting is unknown ("unknown setting") or has a bad
     *         value, or if the effect rejects a number
     * @throws NullPointerException if an argument is {@code null}, or if the factory returns
     *         {@code null}. That, and any other exception from a factory, is a game bug and is
     *         not turned into an empty result
     */
    @Override
    public Optional<CellEffect> findEffect(String name, Map<String, String> settings) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(settings, "settings");
        EffectFactory factory = factories.get(name);
        if (factory == null) {
            return Optional.empty();
        }
        EffectSettings effectSettings = EffectSettings.of(settings);
        CellEffect effect = Objects.requireNonNull(factory.create(effectSettings),
                "The factory of effect '" + name + "' returned null");
        String unreadKey = effectSettings.findUnreadKey();
        if (unreadKey != null) {
            throw new IllegalArgumentException(
                    "Effect '" + name + "' has an unknown setting: " + unreadKey);
        }
        return Optional.of(effect);
    }

    private static boolean isValidName(String name) {
        if (name.isEmpty()) {
            return false;
        }
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            boolean isLowercaseLetter = character >= 'a' && character <= 'z';
            boolean isDigit = character >= '0' && character <= '9';
            if (!isLowercaseLetter && !isDigit) {
                return false;
            }
        }
        return true;
    }
}
