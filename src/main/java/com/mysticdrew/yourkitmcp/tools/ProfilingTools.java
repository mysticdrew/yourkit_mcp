package com.mysticdrew.yourkitmcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysticdrew.yourkitmcp.mcp.Tools;
import com.mysticdrew.yourkitmcp.session.AllocMode;
import com.mysticdrew.yourkitmcp.session.CpuMode;
import com.mysticdrew.yourkitmcp.session.SessionManager;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;

import java.util.List;
import java.util.Locale;

import static com.mysticdrew.yourkitmcp.tools.ConnectionTools.requireSession;
import static com.mysticdrew.yourkitmcp.tools.ConnectionTools.write;

public final class ProfilingTools {
    private ProfilingTools() {}

    public static List<SyncToolSpecification> specs(SessionManager mgr, ObjectMapper json) {
        return List.of(
            Tools.sync("yourkit_start_cpu_profiling",
                "Start CPU profiling. mode: sampling (default), tracing, or call_counting.",
                "{\"type\":\"object\",\"properties\":{"
                  + "\"sessionId\":{\"type\":\"string\"},"
                  + "\"mode\":{\"type\":\"string\",\"enum\":[\"sampling\",\"tracing\",\"call_counting\"],\"default\":\"sampling\"}"
                  + "},\"required\":[\"sessionId\"],\"additionalProperties\":false}",
                args -> {
                    CpuMode mode = CpuMode.valueOf(
                        ((String) args.getOrDefault("mode", "sampling")).toUpperCase(Locale.ROOT));
                    mgr.get((String) args.get("sessionId")).startCpuProfiling(mode);
                    return "{\"ok\":true}";
                }),
            Tools.sync("yourkit_stop_cpu_profiling", "Stop CPU profiling.", requireSession(),
                args -> { mgr.get((String) args.get("sessionId")).stopCpuProfiling(); return "{\"ok\":true}"; }),
            Tools.sync("yourkit_start_alloc_profiling",
                "Start allocation profiling. mode: heap_sampling (default, low overhead), bci, or counting.",
                "{\"type\":\"object\",\"properties\":{"
                  + "\"sessionId\":{\"type\":\"string\"},"
                  + "\"mode\":{\"type\":\"string\",\"enum\":[\"heap_sampling\",\"bci\",\"counting\"],\"default\":\"heap_sampling\"}"
                  + "},\"required\":[\"sessionId\"],\"additionalProperties\":false}",
                args -> {
                    AllocMode mode = AllocMode.valueOf(
                        ((String) args.getOrDefault("mode", "heap_sampling")).toUpperCase(Locale.ROOT));
                    mgr.get((String) args.get("sessionId")).startAllocProfiling(mode);
                    return "{\"ok\":true}";
                }),
            Tools.sync("yourkit_stop_alloc_profiling", "Stop allocation profiling.", requireSession(),
                args -> { mgr.get((String) args.get("sessionId")).stopAllocProfiling(); return "{\"ok\":true}"; }),
            Tools.sync("yourkit_force_gc", "Force a full garbage collection in the target JVM.", requireSession(),
                args -> { mgr.get((String) args.get("sessionId")).forceGc(); return "{\"ok\":true}"; }),
            Tools.sync("yourkit_advance_generation",
                "Advance the object generation marker (use between steps to find leaks).",
                "{\"type\":\"object\",\"properties\":{"
                  + "\"sessionId\":{\"type\":\"string\"},"
                  + "\"name\":{\"type\":\"string\",\"description\":\"Optional generation label\"}"
                  + "},\"required\":[\"sessionId\"],\"additionalProperties\":false}",
                args -> {
                    mgr.get((String) args.get("sessionId")).advanceGeneration((String) args.getOrDefault("name", ""));
                    return "{\"ok\":true}";
                }),
            Tools.sync("yourkit_get_telemetry",
                "Get JVM telemetry: total heap size and total created objects (count + bytes).",
                requireSession(),
                args -> write(json, mgr.get((String) args.get("sessionId")).telemetry()))
        );
    }
}
