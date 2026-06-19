package com.mysticdrew.yourkitmcp.snapshot;

import com.mysticdrew.yourkitmcp.ProfilerException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SnapshotExporterTest {

    @Test
    void buildsExportCommand() {
        SnapshotExporter ex = new SnapshotExporter(Path.of("C:/yk/jre64/bin/java.exe"), Path.of("C:/yk/lib/yourkit.jar"));
        List<String> cmd = ex.buildCommand(Path.of("C:/snaps/a.snapshot"), Path.of("C:/out"));
        assertEquals("C:\\yk\\jre64\\bin\\java.exe".replace('\\','/'), cmd.get(0).replace('\\','/'));
        assertTrue(cmd.get(1).startsWith("-Duser.home="));
        assertEquals("-jar", cmd.get(2));
        assertEquals("C:\\yk\\lib\\yourkit.jar".replace('\\','/'), cmd.get(3).replace('\\','/'));
        assertEquals("-accept-eula", cmd.get(4));
        assertEquals("-export", cmd.get(5));
        assertTrue(cmd.get(6).endsWith("a.snapshot"));
        assertTrue(cmd.get(7).endsWith("out"));
    }

    @Test
    void exportRejectsMissingSnapshot(@org.junit.jupiter.api.io.TempDir Path tmp) {
        SnapshotExporter ex = new SnapshotExporter(Path.of("C:/yk/jre64/bin/java.exe"), Path.of("C:/yk/lib/yourkit.jar"));
        assertThrows(ProfilerException.class,
            () -> ex.export(tmp.resolve("nope.snapshot"), tmp.resolve("out")));
    }
}
