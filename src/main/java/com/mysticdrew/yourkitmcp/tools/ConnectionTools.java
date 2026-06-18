package com.mysticdrew.yourkitmcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysticdrew.yourkitmcp.mcp.Tools;
import com.mysticdrew.yourkitmcp.session.SessionManager;
import com.mysticdrew.yourkitmcp.session.StatusInfo;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;

import java.util.List;
import java.util.Map;

public final class ConnectionTools {
    private ConnectionTools() {}

    public static List<SyncToolSpecification> specs(SessionManager mgr, ObjectMapper json) {
        return List.of(
            Tools.sync("yourkit_connect",
                "Connect to a YourKit agent in a running JVM. Returns a sessionId used by other tools.",
                "{\"type\":\"object\",\"properties\":{"
                  + "\"host\":{\"type\":\"string\",\"description\":\"Agent host\",\"default\":\"localhost\"},"
                  + "\"port\":{\"type\":\"integer\",\"description\":\"Agent port (printed by the agent at startup)\"}"
                  + "},\"required\":[\"port\"],\"additionalProperties\":false}",
                args -> {
                    String host = (String) args.getOrDefault("host", "localhost");
                    int port = ((Number) args.get("port")).intValue();
                    String id = mgr.connect(host, port);
                    StatusInfo s = mgr.get(id).status();
                    return write(json, Map.of("sessionId", id, "pid", s.pid(), "appName", s.appName()));
                }),
            Tools.sync("yourkit_disconnect",
                "Disconnect a YourKit session.",
                requireSession(),
                args -> { mgr.disconnect((String) args.get("sessionId")); return "{\"ok\":true}"; }),
            Tools.sync("yourkit_status",
                "Report which profiling modes are currently active for a session.",
                requireSession(),
                args -> write(json, mgr.get((String) args.get("sessionId")).status()))
        );
    }

    static String requireSession() {
        return "{\"type\":\"object\",\"properties\":{"
             + "\"sessionId\":{\"type\":\"string\"}},\"required\":[\"sessionId\"],\"additionalProperties\":false}";
    }

    static String write(ObjectMapper json, Object value) {
        try { return json.writeValueAsString(value); }
        catch (Exception e) { throw new RuntimeException("JSON serialization failed: " + e.getMessage(), e); }
    }
}
