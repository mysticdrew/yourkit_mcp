package com.mysticdrew.yourkitmcp.snapshot;

import com.mysticdrew.yourkitmcp.ProfilerException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class SnapshotExporter {
    private final Path launcher;

    public SnapshotExporter(Path launcher) { this.launcher = launcher; }

    List<String> buildCommand(Path snapshot, Path targetDir) {
        return List.of(launcher.toString(), "-export",
            snapshot.toAbsolutePath().toString(), targetDir.toAbsolutePath().toString());
    }

    public Path export(Path snapshot, Path targetDir) {
        if (!Files.exists(snapshot)) {
            throw new ProfilerException("Snapshot not found: " + snapshot);
        }
        try {
            Files.createDirectories(targetDir);
            Process p = new ProcessBuilder(buildCommand(snapshot, targetDir))
                .redirectErrorStream(true).start();
            String output = new String(p.getInputStream().readAllBytes());
            if (!p.waitFor(120, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new ProfilerException("Export timed out after 120s for " + snapshot);
            }
            if (p.exitValue() != 0) {
                throw new ProfilerException("Export failed (exit " + p.exitValue() + "): " + output.trim());
            }
            return targetDir;
        } catch (IOException e) {
            throw new ProfilerException("Failed to run export: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProfilerException("Export interrupted", e);
        }
    }
}
