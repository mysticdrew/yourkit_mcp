package com.mysticdrew.yourkitmcp;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    private Path fakeHome(Path tmp) throws IOException {
        Files.createDirectories(tmp.resolve("lib"));
        Files.writeString(tmp.resolve("lib/yjp-controller-api-redist.jar"), "x");
        Files.writeString(tmp.resolve("lib/yourkit.jar"), "x");
        return tmp;
    }

    @Test
    void resolvesJars(@org.junit.jupiter.api.io.TempDir Path tmp) throws IOException {
        Config cfg = Config.fromHome(fakeHome(tmp));
        assertTrue(cfg.controllerJar().toString().endsWith("yjp-controller-api-redist.jar"));
        assertTrue(cfg.exporterJar().toString().endsWith("yourkit.jar"));
        // No bundled jre64 in the fake home, so it falls back to "java" on PATH.
        assertNotNull(cfg.javaExecutable());
    }

    @Test
    void missingHomeThrows(@org.junit.jupiter.api.io.TempDir Path tmp) {
        ProfilerException ex = assertThrows(ProfilerException.class,
                () -> Config.fromHome(tmp.resolve("nope")));
        assertTrue(ex.getMessage().toLowerCase().contains("yourkit"));
    }

    @Test
    void fromEnvUnsetThrows() {
        ProfilerException ex = assertThrows(ProfilerException.class,
                () -> Config.fromEnv(k -> null));
        assertTrue(ex.getMessage().contains("YOURKIT_HOME"));
    }

    @Test
    void fromEnvBlankThrows() {
        assertThrows(ProfilerException.class,
                () -> Config.fromEnv(k -> "   "));
    }

    @Test
    void fromEnvValidDelegates(@org.junit.jupiter.api.io.TempDir Path tmp) throws IOException {
        String homePath = fakeHome(tmp).toString();
        Config cfg = Config.fromEnv(k -> homePath);
        assertTrue(cfg.controllerJar().toString().endsWith("yjp-controller-api-redist.jar"));
    }
}
