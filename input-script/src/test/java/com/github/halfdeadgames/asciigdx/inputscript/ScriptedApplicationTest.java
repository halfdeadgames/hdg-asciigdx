package com.github.halfdeadgames.asciigdx.inputscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScriptedApplicationTest {

    @TempDir
    Path folder;

    private Input previousInput;
    private Graphics previousGraphics;

    @BeforeEach
    void rememberGdxDevices() {
        previousInput = Gdx.input;
        previousGraphics = Gdx.graphics;
    }

    @AfterEach
    void restoreGdxDevices() {
        Gdx.input = previousInput;
        Gdx.graphics = previousGraphics;
    }

    @Test
    void installsScriptedInputBeforeTheDelegateCreatesAndFixesFrameTime() throws IOException {
        Path script = folder.resolve("run.txt");
        Files.writeString(script, "wait 100f\nquit\n");
        List<String> calls = new ArrayList<>();
        Input[] inputSeenByDelegate = new Input[1];
        ApplicationAdapter delegate = new ApplicationAdapter() {
            @Override
            public void create() {
                inputSeenByDelegate[0] = Gdx.input;
                calls.add("create");
            }

            @Override
            public void render() {
                calls.add("render");
            }
        };
        Gdx.input = null;
        Gdx.graphics = null;
        ScriptedApplication application =
                new ScriptedApplication(delegate, script, folder.resolve("captures"));

        application.create();
        application.render();

        assertEquals(List.of("create", "render"), calls);
        assertSame(Gdx.input, inputSeenByDelegate[0]);
        assertEquals(ScriptedInput.class, Gdx.input.getClass());
    }
}
