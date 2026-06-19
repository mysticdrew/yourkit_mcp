package com.mysticdrew.yourkitmcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysticdrew.yourkitmcp.session.FakeControllerServiceFactory;
import com.mysticdrew.yourkitmcp.session.SessionManager;
import com.mysticdrew.yourkitmcp.snapshot.ExportParser;
import com.mysticdrew.yourkitmcp.snapshot.SnapshotExporter;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SnapshotToolsTest {

    @Test
    void producesThreeSpecs() {
        SessionManager mgr = new SessionManager(new FakeControllerServiceFactory());
        List<SyncToolSpecification> specs = SnapshotTools.specs(
            mgr, new SnapshotExporter(Path.of("C:/yk/jre64/bin/java.exe"), Path.of("C:/yk/lib/yourkit.jar")), new ExportParser(), new ObjectMapper());
        assertEquals(3, specs.size());
    }
}
