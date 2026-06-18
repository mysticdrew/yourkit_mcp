package com.mysticdrew.yourkitmcp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

public final class Config {
    private final Path controllerJar;
    private final Path profilerLauncher;

    private Config(Path controllerJar, Path profilerLauncher) {
        this.controllerJar = controllerJar;
        this.profilerLauncher = profilerLauncher;
    }

    public Path controllerJar() { return controllerJar; }
    public Path profilerLauncher() { return profilerLauncher; }

    static Config fromEnv(Function<String, String> getenv) {
        String home = getenv.apply("YOURKIT_HOME");
        if (home == null || home.isBlank()) {
            throw new ProfilerException(
                "YOURKIT_HOME is not set. Point it at your YourKit install, e.g. "
                + "C:\\Program Files\\YourKit Java Profiler 2026.3.164");
        }
        return fromHome(Path.of(home));
    }

    public static Config fromEnv() {
        return fromEnv(System::getenv);
    }

    public static Config fromHome(Path home) {
        Path jar = home.resolve("lib").resolve("yjp-controller-api-redist.jar");
        boolean win = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("win");
        Path launcher = home.resolve("bin").resolve(win ? "profiler.bat" : "profiler.sh");
        if (!Files.exists(jar)) {
            throw new ProfilerException("YourKit controller jar not found: " + jar);
        }
        if (!Files.exists(launcher)) {
            throw new ProfilerException("YourKit launcher not found: " + launcher);
        }
        return new Config(jar, launcher);
    }
}
