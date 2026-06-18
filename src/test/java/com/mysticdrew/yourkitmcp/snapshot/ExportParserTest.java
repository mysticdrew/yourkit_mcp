package com.mysticdrew.yourkitmcp.snapshot;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ExportParserTest {

    private Path fixturesDir() throws Exception {
        // Copy classpath fixtures into a temp dir so the parser sees a directory of CSVs.
        Path dir = Files.createTempDirectory("ykexport");
        for (String f : new String[]{"Hot-spots.csv", "Memory-class-list.csv"}) {
            try (var in = getClass().getResourceAsStream("/fixtures/" + f)) {
                Files.write(dir.resolve(f), in.readAllBytes());
            }
        }
        return dir;
    }

    @Test
    void parsesTopHotSpots() throws Exception {
        AnalysisResult r = new ExportParser().parse(fixturesDir(), 2);
        assertEquals(2, r.hotSpots().size());
        assertEquals("com.example.App.compute()", r.hotSpots().get(0).name());
        assertEquals(1234.5, r.hotSpots().get(0).timeMs(), 0.001);
        assertEquals(45, r.hotSpots().get(0).count());
    }

    @Test
    void parsesTopMemoryClasses() throws Exception {
        AnalysisResult r = new ExportParser().parse(fixturesDir(), 2);
        assertEquals(2, r.topClasses().size());
        assertEquals("byte[]", r.topClasses().get(0).className());
        assertEquals(12000, r.topClasses().get(0).objects());
        assertEquals(4096000, r.topClasses().get(0).shallowSize());
    }

    @Test
    void missingFilesYieldEmptyLists() throws Exception {
        Path empty = Files.createTempDirectory("ykempty");
        AnalysisResult r = new ExportParser().parse(empty, 5);
        assertTrue(r.hotSpots().isEmpty());
        assertTrue(r.topClasses().isEmpty());
    }
}
