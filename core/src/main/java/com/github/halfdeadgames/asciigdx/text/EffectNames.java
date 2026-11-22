package com.github.halfdeadgames.asciigdx.text;

import com.github.halfdeadgames.asciigdx.zone.CellEffect;
import java.util.Map;
import java.util.Optional;

/**
 * Looks up an effect by name for the {@code [effect:NAME key=value ...]} tags of
 * {@link StyledText}. The game supplies the names; {@code EffectRegistry} in the zone package is
 * the ready-made implementation with the built-in effects.
 *
 * <p>Implementations are called while a text is parsed, not while it is drawn, so they may
 * allocate. The name and the settings come straight from the text, which may be written by a
 * user, so an implementation must be safe to call with any text. It must not return {@code null}.
 *
 * <p>Example:
 * <pre>{@code
 * EffectNames names = EffectRegistry.withBuiltIns();
 * StyledText text = StyledText.parse("A [effect:wave amplitude=0.5]wavy[/effect] word",
 *         ColorNames.NONE, names);
 * }</pre>
 */
@FunctionalInterface
public interface EffectNames {

    /**
     * Knows no names, so every {@code [effect:NAME]} tag stays visible as written.
     */
    EffectNames NONE = (name, settings) -> Optional.empty();

    /**
     * Finds the effect with a name and builds it from the settings of the tag. The name is
     * compared exactly, so {@code "Wave"} and {@code "wave"} are different names unless the
     * implementation says otherwise.
     *
     * @param name the text between {@code effect:} and the first space or {@code ]}; never
     *         {@code null}. The parser passes one or more characters, none of them a space,
     *         {@code [}, or {@code ]}; every other character is possible, uppercase letters,
     *         {@code =}, and {@code :} included. {@code EffectRegistry#register} accepts only a
     *         subset of that: lowercase ASCII letters and digits
     * @param settings the {@code key=value} pairs of the tag, in the order written; never
     *         {@code null}, and not changed by the caller after the call; may be empty. Keys and
     *         values are not empty and contain no spaces. Do not modify it
     * @return the effect, or an empty value when the name is unknown; never {@code null}
     * @throws IllegalArgumentException if the name is known but a setting is unknown or has a bad
     *         value. The parser catches it and leaves the tag visible as text. This is the only
     *         exception that means bad user text. Any other exception, and a {@code null}
     *         result, is a bug in the game and propagates out of {@code StyledText.parse} (a
     *         {@code null} result as a {@link NullPointerException})
     */
    Optional<CellEffect> findEffect(String name, Map<String, String> settings);
}
