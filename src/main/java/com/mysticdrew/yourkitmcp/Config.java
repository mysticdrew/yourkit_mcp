package com.mysticdrew.yourkitmcp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

public final class Config {
    private final Path controllerJar;
    private final Path exporterJar;
    private final Path javaExecutable;

    private Config(Path controllerJar, Path exporterJar, Path javaExecutable) {
        this.controllerJar = controllerJar;
        this.exporterJar = exporterJar;
        this.javaExecutable = javaExecutable;
    }

    public Path controllerJar() { return controllerJar; }
    public Path exporterJar() { return exporterJar; }
    public Path javaExecutable() { return javaExecutable; }

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
        // Snapshot export launches the YourKit UI jar directly via the bundled JRE rather than
        // bin/profiler.bat. The launcher script pins YourKit's settings/log dir to the install dir,
        // which is typically read-only (e.g. C:\Program Files); it then cannot persist EULA
        // acceptance and headless export fails with "Cannot accept EULA". Running the jar directly
        // resolves the settings dir under the user home (~/.yjp) instead.
        Path exporterJar = home.resolve("lib").resolve("yourkit.jar");
        Path bundledJava = home.resolve("jre64").resolve("bin").resolve(win ? "java.exe" : "java");
        Path javaExecutable = Files.exists(bundledJava) ? bundledJava : Path.of(win ? "java.exe" : "java");
        if (!Files.exists(jar)) {
            throw new ProfilerException("YourKit controller jar not found: " + jar);
        }
        if (!Files.exists(exporterJar)) {
            throw new ProfilerException("YourKit exporter jar not found: " + exporterJar);
        }
        return new Config(jar, exporterJar, javaExecutable);
    }
}
