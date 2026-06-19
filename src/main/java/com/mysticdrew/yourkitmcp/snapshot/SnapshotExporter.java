package com.mysticdrew.yourkitmcp.snapshot;

import com.mysticdrew.yourkitmcp.ProfilerException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class SnapshotExporter {
    private final Path javaExecutable;
    private final Path exporterJar;

    public SnapshotExporter(Path javaExecutable, Path exporterJar) {
        this.javaExecutable = javaExecutable;
        this.exporterJar = exporterJar;
    }

    List<String> buildCommand(Path snapshot, Path targetDir) {
        // Launch the YourKit UI jar directly (java -jar yourkit.jar) rather than bin/profiler.bat:
        // the launcher script pins YourKit's settings/log dir to the read-only install dir, so it
        // cannot persist EULA acceptance and export fails with "Cannot accept EULA".
        //
        // YourKit resolves its settings/license/EULA dir (~/.yjp) from the JVM's user.home. Some MCP
        // host launch contexts give the server a user.home that points at a non-writable or
        // license-less location, which also fails with "Cannot accept EULA". Pin user.home to the
        // OS user-profile dir (stable across launch contexts) so the export reuses the real ~/.yjp
        // that holds the license and prior EULA acceptance. The property must precede -jar.
        //
        // -accept-eula is required on a machine where the EULA has not been accepted yet (fresh
        // install / CI / new user); without it the launcher blocks waiting for acceptance and the
        // export hangs until timeout. The flag is idempotent and must precede -export.
        return List.of(javaExecutable.toString(),
            "-Duser.home=" + resolveUserHome(),
            "-jar", exporterJar.toString(),
            "-accept-eula", "-export",
            snapshot.toAbsolutePath().toString(), targetDir.toAbsolutePath().toString());
    }

    /**
     * Resolves the directory where YourKit keeps its {@code .yjp} settings, license, and EULA
     * acceptance, used as {@code -Duser.home} for the export JVM.
     *
     * <p>Prefers the {@code user.home} system property, which the operator can pin reliably via
     * the MCP server's own {@code -Duser.home} launch arg (MCP hosts pass JVM args through but may
     * drop environment variables). Falls back to the {@code USERPROFILE} (Windows) / {@code HOME}
     * (Unix) environment variable when the property is unset.
     */
    private static String resolveUserHome() {
        String home = System.getProperty("user.home");
        if (home != null && !home.isBlank()) {
            return home;
        }
        boolean win = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        return System.getenv(win ? "USERPROFILE" : "HOME");
    }

    public Path export(Path snapshot, Path targetDir) {
        if (!Files.exists(snapshot)) {
            throw new ProfilerException("Snapshot not found: " + snapshot);
        }
        Path logFile;
        try {
            Files.createDirectories(targetDir);
            logFile = Files.createTempFile("yk-export-", ".log");
        } catch (IOException e) {
            throw new ProfilerException("Failed to prepare export: " + e.getMessage(), e);
        }
        List<String> command = buildCommand(snapshot, targetDir);
        try {
            Process p = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(logFile.toFile())
                .start();
            if (!p.waitFor(120, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new ProfilerException("Export timed out after 120s for " + snapshot);
            }
            String output = Files.exists(logFile) ? Files.readString(logFile).trim() : "";
            if (p.exitValue() != 0) {
                throw new ProfilerException("Export failed (exit " + p.exitValue() + "): " + output
                    + " | cmd=" + command);
            }
            return targetDir;
        } catch (IOException e) {
            throw new ProfilerException("Failed to run export: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProfilerException("Export interrupted", e);
        } finally {
            try { Files.deleteIfExists(logFile); } catch (IOException ignored) {}
        }
    }
}
