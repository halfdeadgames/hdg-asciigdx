package com.github.halfdeadgames.asciigdx.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EffectRegistryTest {

    private static final String[] BUILT_IN_NAMES = {"wave", "bounce", "shake", "pulse", "spin",
            "flash", "rainbow", "glyphcycle"};

    @Test
    void everyBuiltInResolvesWithDefaults() {
        EffectRegistry registry = EffectRegistry.withBuiltIns();
        for (String name : BUILT_IN_NAMES) {
            assertTrue(registry.findEffect(name, Map.of()).isPresent(), name);
        }
    }

    @Test
    void everyBuiltInRejectsUnknownSetting() {
        EffectRegistry registry = EffectRegistry.withBuiltIns();
        for (String name : BUILT_IN_NAMES) {
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> registry.findEffect(name, Map.of("bogus", "1")), name);
            assertTrue(failure.getMessage().contains("unknown setting"), failure.getMessage());
        }
    }

    @Test
    void builtInsAcceptTheirDocumentedSettings() {
        EffectRegistry registry = EffectRegistry.withBuiltIns();
        Map<String, Map<String, String>> settingsByName = Map.of(
                "wave", Map.of("amplitude", "1", "period", "2", "phase", "distance:4"),
                "bounce", Map.of("height", "1", "period", "2", "phase", "sync"),
                "shake", Map.of("amplitude", "1", "steps", "5", "seed", "7"),
                "pulse", Map.of("amount", "0.5", "period", "2", "phase", "scattered:3"),
                "spin", Map.of("degrees", "-45", "phase", "columns:2"),
                "flash", Map.of("color", "ff0000", "amount", "0.5", "period", "2"),
                "rainbow", Map.of("amount", "0.5", "period", "3", "phase", "columns:5"),
                "glyphcycle", Map.of("period", "2", "glyphs", "ab😀"));
        for (Map.Entry<String, Map<String, String>> entry : settingsByName.entrySet()) {
            assertTrue(registry.findEffect(entry.getKey(), entry.getValue()).isPresent(),
                    entry.getKey());
        }
    }

    @Test
    void builtInsRejectBadNumbers() {
        EffectRegistry registry = EffectRegistry.withBuiltIns();
        assertThrows(IllegalArgumentException.class,
                () -> registry.findEffect("wave", Map.of("period", "0")));
        assertThrows(IllegalArgumentException.class,
                () -> registry.findEffect("flash", Map.of("amount", "2")));
        assertThrows(IllegalArgumentException.class,
                () -> registry.findEffect("flash", Map.of("color", "red")));
        assertThrows(IllegalArgumentException.class,
                () -> registry.findEffect("shake", Map.of("seed", "1.5")));
    }

    @Test
    void unknownNameIsEmpty() {
        assertEquals(Optional.empty(), EffectRegistry.withBuiltIns().findEffect("nope", Map.of()));
        assertEquals(Optional.empty(), EffectRegistry.withBuiltIns().findEffect("Wave", Map.of()));
    }

    @Test
    void emptyRegistryKnowsNothing() {
        assertTrue(EffectRegistry.empty().findEffect("wave", Map.of()).isEmpty());
    }

    @Test
    void registeredFactoryIsUsed() {
        EffectRegistry registry = EffectRegistry.empty();
        CellEffect custom = (cell, where, time) -> { };
        registry.register("custom1", settings -> {
            settings.getInt("n", 0);
            return custom;
        });
        assertSame(custom, registry.findEffect("custom1", Map.of("n", "3")).orElseThrow());
    }

    @Test
    void unreadKeyIsRejected() {
        EffectRegistry registry = EffectRegistry.empty();
        registry.register("lazy", settings -> (cell, where, time) -> { });
        assertThrows(IllegalArgumentException.class,
                () -> registry.findEffect("lazy", Map.of("extra", "1")));
    }

    @Test
    void registerRejectsDuplicatesAndBadNames() {
        EffectRegistry registry = EffectRegistry.withBuiltIns();
        EffectFactory factory = settings -> (cell, where, time) -> { };
        assertThrows(IllegalArgumentException.class, () -> registry.register("wave", factory));
        registry.register("mine", factory);
        assertThrows(IllegalArgumentException.class, () -> registry.register("mine", factory));
        for (String bad : new String[] {"", "Mine", "my-effect", "my effect", "a:b", "café"}) {
            assertThrows(IllegalArgumentException.class, () -> registry.register(bad, factory),
                    bad);
        }
        assertThrows(NullPointerException.class, () -> registry.register("fine", null));
    }

    @Test
    void registriesAreIndependent() {
        EffectRegistry first = EffectRegistry.withBuiltIns();
        first.register("mine", settings -> (cell, where, time) -> { });
        assertTrue(EffectRegistry.withBuiltIns().findEffect("mine", Map.of()).isEmpty());
    }

    @Test
    void settingsReadValuesAndDefaults() {
        EffectSettings settings = EffectSettings.of(Map.of("f", "1.5", "i", "-4", "s", "xyz"));
        assertEquals(1.5f, settings.getFloat("f", 0f));
        assertEquals(9f, settings.getFloat("missing", 9f));
        assertEquals(-4, settings.getInt("i", 0));
        assertEquals(8, settings.getInt("missing", 8));
        assertEquals("xyz", settings.getString("s", "d"));
        assertEquals("d", settings.getString("missing", "d"));
        assertNull(settings.getString("missing", null));
        assertNull(settings.findUnreadKey());
    }

    @Test
    void settingsReadPhases() {
        EffectSettings settings = EffectSettings.of(Map.of("a", "sync", "b", "columns:4",
                "c", "distance:2.5", "d", "scattered:9"));
        assertSame(Phase.sync(), settings.getPhase("a", null));
        assertNotNull(settings.getPhase("b", null));
        assertNotNull(settings.getPhase("c", null));
        assertNotNull(settings.getPhase("d", null));
        assertSame(Phase.sync(), settings.getPhase("missing", Phase.sync()));
    }

    @Test
    void settingsRejectBadValues() {
        EffectSettings settings = EffectSettings.of(Map.of("n", "abc", "p", "columns:x",
                "q", "columns", "r", "spiral:3", "z", "distance:-1", "c", "12345"));
        assertThrows(IllegalArgumentException.class, () -> settings.getFloat("n", 0f));
        assertThrows(IllegalArgumentException.class, () -> settings.getInt("n", 0));
        assertThrows(IllegalArgumentException.class, () -> settings.getPhase("n", null));
        assertThrows(IllegalArgumentException.class, () -> settings.getPhase("p", null));
        assertThrows(IllegalArgumentException.class, () -> settings.getPhase("q", null));
        assertThrows(IllegalArgumentException.class, () -> settings.getPhase("r", null));
        assertThrows(IllegalArgumentException.class, () -> settings.getPhase("z", null));
        assertThrows(IllegalArgumentException.class, () -> settings.getRgba("c", 0));
    }

    @Test
    void settingsParseColors() {
        EffectSettings settings = EffectSettings.of(Map.of("a", "ff8000", "b", "FF800040"));
        assertEquals(0xFF8000FF, settings.getRgba("a", 0));
        assertEquals(0xFF800040, settings.getRgba("b", 0));
        assertEquals(7, settings.getRgba("missing", 7));
    }

    @Test
    void floatsRejectEverythingButFiniteDecimals() {
        String[] rejected = {"NaN", "Infinity", "-Infinity", "1e999", "-1e999", "1f", "1F", "1d",
                "1D", "0x1p3", "+1", ".5", "1.", "", "-", "1e", "1,5", " 1", "1 ", "١",
                "1.5.2"};
        for (String text : rejected) {
            EffectSettings settings = EffectSettings.of(Map.of("n", text));
            assertThrows(IllegalArgumentException.class, () -> settings.getFloat("n", 0f), text);
        }
        EffectSettings accepted = EffectSettings.of(Map.of("a", "-2.5", "b", "1e3", "c", "4E-1"));
        assertEquals(-2.5f, accepted.getFloat("a", 0f));
        assertEquals(1000f, accepted.getFloat("b", 0f));
        assertEquals(0.4f, accepted.getFloat("c", 0f));
    }

    @Test
    void wholeNumbersRejectEverythingButPlainDigits() {
        String[] rejected = {"+1", "1.0", "1e3", "0x10", "١", "", "-", "2147483648", "1 "};
        for (String text : rejected) {
            EffectSettings settings = EffectSettings.of(Map.of("n", text));
            assertThrows(IllegalArgumentException.class, () -> settings.getInt("n", 0), text);
        }
        assertEquals(-12, EffectSettings.of(Map.of("n", "-12")).getInt("n", 0));
    }

    @Test
    void phaseNumbersAreStrict() {
        String[] rejected = {"columns:+1", "columns:NaN", "columns:1f", "distance:1e999",
                "distance:0x1p1", "scattered:+1", "scattered:1.5", "scattered:١",
                "scattered:"};
        for (String text : rejected) {
            EffectSettings settings = EffectSettings.of(Map.of("p", text));
            assertThrows(IllegalArgumentException.class, () -> settings.getPhase("p", null), text);
        }
        EffectSettings accepted = EffectSettings.of(Map.of("p", "scattered:-3"));
        assertNotNull(accepted.getPhase("p", null));
    }

    @Test
    void hasDoesNotMarkKeyAsRead() {
        EffectSettings settings = EffectSettings.of(Map.of("color", "ff0000"));
        assertTrue(settings.has("color"));
        assertEquals(false, settings.has("other"));
        assertEquals("color", settings.findUnreadKey());
    }

    @Test
    void factoryCanBranchOnHas() {
        EffectRegistry registry = EffectRegistry.empty();
        CellEffect plain = (cell, where, time) -> { };
        CellEffect tinted = (cell, where, time) -> { };
        registry.register("branch", settings -> {
            if (settings.has("color")) {
                settings.getRgba("color", 0);
                return tinted;
            }
            return plain;
        });
        assertSame(plain, registry.findEffect("branch", Map.of()).orElseThrow());
        assertSame(tinted, registry.findEffect("branch", Map.of("color", "ff0000")).orElseThrow());
    }

    @Test
    void factoryThatOnlyAsksHasLeavesKeyUnread() {
        EffectRegistry registry = EffectRegistry.empty();
        registry.register("asker", settings -> {
            settings.has("color");
            return (cell, where, time) -> { };
        });
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> registry.findEffect("asker", Map.of("color", "ff0000")));
        assertTrue(failure.getMessage().contains("unknown setting"));
    }

    @Test
    void factoryBugsPropagate() {
        EffectRegistry registry = EffectRegistry.empty();
        registry.register("nullish", settings -> null);
        registry.register("broken", settings -> {
            throw new IllegalStateException("bug");
        });
        assertThrows(NullPointerException.class, () -> registry.findEffect("nullish", Map.of()));
        assertThrows(IllegalStateException.class, () -> registry.findEffect("broken", Map.of()));
    }

    @Test
    void unreadKeysAreReported() {
        EffectSettings settings = EffectSettings.of(Map.of("used", "1", "unused", "2"));
        settings.getInt("used", 0);
        assertEquals("unused", settings.findUnreadKey());
        settings.getInt("unused", 0);
        assertNull(settings.findUnreadKey());
    }
}
