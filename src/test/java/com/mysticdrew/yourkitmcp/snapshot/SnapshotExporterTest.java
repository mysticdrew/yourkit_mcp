package com.mysticdrew.yourkitmcp.snapshot;

import com.mysticdrew.yourkitmcp.ProfilerException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SnapshotExporterTest {

    @Test
    void buildsExportCommand() {
        SnapshotExporter ex = new SnapshotExporter(Path.of("C:/yk/bin/profiler.bat"));
        List<String> cmd = ex.buildCommand(Path.of("C:/snaps/a.snapshot"), Path.of("C:/out"));
        assertEquals("C:\\yk\\bin\\profiler.bat".replace('\\','/'), cmd.get(0).replace('\\','/'));
        assertEquals("-export", cmd.get(1));
        assertTrue(cmd.get(2).endsWith("a.snapshot"));
        assertTrue(cmd.get(3).endsWith("out"));
    }

    @Test
    void exportRejectsMissingSnapshot(@org.junit.jupiter.api.io.TempDir Path tmp) {
        SnapshotExporter ex = new SnapshotExporter(Path.of("C:/yk/bin/profiler.bat"));
        assertThrows(ProfilerException.class,
            () -> ex.export(tmp.resolve("nope.snapshot"), tmp.resolve("out")));
    }
}
