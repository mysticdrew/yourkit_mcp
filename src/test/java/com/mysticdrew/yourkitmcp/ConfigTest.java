package com.mysticdrew.yourkitmcp;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    private Path fakeHome(Path tmp) throws IOException {
        Files.createDirectories(tmp.resolve("lib"));
        Files.createDirectories(tmp.resolve("bin"));
        Files.writeString(tmp.resolve("lib/yjp-controller-api-redist.jar"), "x");
        boolean win = System.getProperty("os.name").toLowerCase().contains("win");
        Files.writeString(tmp.resolve(win ? "bin/profiler.bat" : "bin/profiler.sh"), "x");
        return tmp;
    }

    @Test
    void resolvesJarAndLauncher(@org.junit.jupiter.api.io.TempDir Path tmp) throws IOException {
        Config cfg = Config.fromHome(fakeHome(tmp));
        assertTrue(cfg.controllerJar().toString().endsWith("yjp-controller-api-redist.jar"));
        assertTrue(Files.exists(cfg.profilerLauncher()));
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
