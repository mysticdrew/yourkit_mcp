package com.mysticdrew.yourkitmcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysticdrew.yourkitmcp.ProfilerException;
import com.mysticdrew.yourkitmcp.mcp.Tools;
import com.mysticdrew.yourkitmcp.session.SessionManager;
import com.mysticdrew.yourkitmcp.session.SnapshotKind;
import com.mysticdrew.yourkitmcp.snapshot.AnalysisResult;
import com.mysticdrew.yourkitmcp.snapshot.ExportParser;
import com.mysticdrew.yourkitmcp.snapshot.SnapshotExporter;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.mysticdrew.yourkitmcp.tools.ConnectionTools.write;

public final class SnapshotTools {
    private SnapshotTools() {}

    private static final String CAPTURE_SCHEMA =
        "{\"type\":\"object\",\"properties\":{"
      + "\"sessionId\":{\"type\":\"string\"},"
      + "\"type\":{\"type\":\"string\",\"enum\":[\"performance\",\"memory\",\"hprof\",\"jfr\"],\"default\":\"performance\"}"
      + "},\"required\":[\"sessionId\"],\"additionalProperties\":false}";

    public static List<SyncToolSpecification> specs(SessionManager mgr, SnapshotExporter exporter,
                                                    ExportParser parser, ObjectMapper json) {
        return List.of(
            Tools.sync("yourkit_capture_snapshot",
                "Capture a snapshot from a running session. type: performance (default), memory, hprof, jfr. Returns the file path.",
                CAPTURE_SCHEMA,
                args -> {
                    SnapshotKind kind = kind(args.getOrDefault("type", "performance"));
                    String path = mgr.get((String) args.get("sessionId")).captureSnapshot(kind);
                    return write(json, Map.of("snapshotPath", path));
                }),
            Tools.sync("yourkit_analyze_snapshot",
                "Export and parse a snapshot file into top-N hot spots and memory classes. Returns structured results plus the raw export dir.",
                "{\"type\":\"object\",\"properties\":{"
                  + "\"path\":{\"type\":\"string\",\"description\":\"Path to a .snapshot file\"},"
                  + "\"topN\":{\"type\":\"integer\",\"default\":20}"
                  + "},\"required\":[\"path\"],\"additionalProperties\":false}",
                args -> {
                    int topN = ((Number) args.getOrDefault("topN", 20)).intValue();
                    AnalysisResult r = analyze(exporter, parser, Path.of((String) args.get("path")), topN);
                    return write(json, r);
                }),
            Tools.sync("yourkit_capture_and_analyze",
                "Capture a snapshot from a session and immediately analyze it. Returns structured top-N results.",
                "{\"type\":\"object\",\"properties\":{"
                  + "\"sessionId\":{\"type\":\"string\"},"
                  + "\"type\":{\"type\":\"string\",\"enum\":[\"performance\",\"memory\"],\"default\":\"performance\"},"
                  + "\"topN\":{\"type\":\"integer\",\"default\":20}"
                  + "},\"required\":[\"sessionId\"],\"additionalProperties\":false}",
                args -> {
                    SnapshotKind kind = kind(args.getOrDefault("type", "performance"));
                    int topN = ((Number) args.getOrDefault("topN", 20)).intValue();
                    String path = mgr.get((String) args.get("sessionId")).captureSnapshot(kind);
                    AnalysisResult r = analyze(exporter, parser, Path.of(path), topN);
                    return write(json, r);
                })
        );
    }

    private static SnapshotKind kind(Object raw) {
        return SnapshotKind.valueOf(((String) raw).toUpperCase(Locale.ROOT));
    }

    private static AnalysisResult analyze(SnapshotExporter exporter, ExportParser parser, Path snapshot, int topN) {
        try {
            Path out = Files.createTempDirectory("ykexport-");
            exporter.export(snapshot, out);
            return parser.parse(out, topN);
        } catch (IOException e) {
            throw new ProfilerException("Could not create export dir: " + e.getMessage(), e);
        }
    }
}
