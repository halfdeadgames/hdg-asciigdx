package com.github.halfdeadgames.asciigdx.sandbox.zones;

import com.github.halfdeadgames.asciigdx.zone.CellEffect;
import com.github.halfdeadgames.asciigdx.zone.CellEffects;
import com.github.halfdeadgames.asciigdx.zone.Phase;

/**
 * The effects the area spell can show, one at a time. Every period divides {@link #LOOP_SECONDS}, so a
 * looping clock of that length shows no jump when it wraps.
 */
public final class SpellEffectCycle {
    /** Length of the shortest clock loop that fits every effect here. */
    public static final float LOOP_SECONDS = 12f;

    private static final float RING_CELLS_PER_CYCLE = 4f;
    private static final float SLOW_RING_CELLS_PER_CYCLE = 8f;
    private static final int SHAKE_SEED = 99;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int WARM_GOLD = 0xFFA824FF;

    private final String[] names = {"combined", "wave", "bounce", "shake", "pulse", "spin", "flash", "rainbow"};
    private final CellEffect[] effects;
    private int index;

    public SpellEffectCycle() {
        Phase rings = Phase.byDistance(RING_CELLS_PER_CYCLE);
        Phase slowRings = Phase.byDistance(SLOW_RING_CELLS_PER_CYCLE);
        effects = new CellEffect[] {
            CellEffects.combine(
                CellEffects.spin(-90f, Phase.scattered(1)),
                CellEffects.pulse(0.15f, 1.2f, Phase.scattered(2)),
                CellEffects.bounce(0.3f, 1f, Phase.scattered(3)),
                CellEffects.flash(WARM_GOLD, 0.6f, 1.2f, Phase.scattered(4))),
            CellEffects.wave(0.3f, 1.2f, rings),
            CellEffects.bounce(0.4f, 1f, rings),
            CellEffects.shake(0.15f, 12f, SHAKE_SEED),
            CellEffects.pulse(0.4f, 1.2f, rings),
            CellEffects.spin(-120f, slowRings),
            CellEffects.flash(WHITE, 0.8f, 1.2f, rings),
            CellEffects.rainbow(0.7f, 2f, slowRings)
        };
    }

    public CellEffect current() {
        return effects[index];
    }

    public String currentName() {
        return names[index];
    }

    /** Shows the effect with this name, ignoring case; an unknown name changes nothing. */
    public void select(String name) {
        for (int i = 0; i < names.length; i++) {
            if (names[i].equalsIgnoreCase(name)) {
                index = i;
                return;
            }
        }
    }

    public void advance() {
        index = (index + 1) % effects.length;
    }
}
