package com.mysticdrew.yourkitmcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysticdrew.yourkitmcp.mcp.Tools;
import com.mysticdrew.yourkitmcp.session.SessionManager;
import com.mysticdrew.yourkitmcp.session.YourKitControllerServiceFactory;
import com.mysticdrew.yourkitmcp.snapshot.ExportParser;
import com.mysticdrew.yourkitmcp.snapshot.SnapshotExporter;
import com.mysticdrew.yourkitmcp.tools.ConnectionTools;
import com.mysticdrew.yourkitmcp.tools.ProfilingTools;
import com.mysticdrew.yourkitmcp.tools.SnapshotTools;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapperSupplier;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for the YourKit MCP stdio server.
 *
 * <p>stdout is reserved exclusively for JSON-RPC (MCP protocol). All logging
 * goes to stderr via slf4j-simple (configured in simplelogger.properties).
 * NEVER write to System.out here.
 */
public final class Main {
    public static void main(String[] args) {
        // Production dependencies
        Config config = Config.fromEnv();
        var json = new ObjectMapper();
        SessionManager mgr = new SessionManager(new YourKitControllerServiceFactory());
        SnapshotExporter exporter = new SnapshotExporter(config.profilerLauncher());
        ExportParser parser = new ExportParser();

        // Aggregate all tool specs: ping + connection + profiling + snapshot
        List<SyncToolSpecification> tools = new ArrayList<>();
        tools.add(Tools.sync(
                "yourkit_ping",
                "Health check. Returns pong.",
                "{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}",
                a -> "{\"pong\":true}"));
        tools.addAll(ConnectionTools.specs(mgr, json));
        tools.addAll(ProfilingTools.specs(mgr, json));
        tools.addAll(SnapshotTools.specs(mgr, exporter, parser, json));

        // StdioServerTransportProvider has no no-arg constructor in 2.0.0;
        // supply the Jackson 3 mapper (auto-discovered via JacksonMcpJsonMapperSupplier).
        McpJsonMapper mapper = new JacksonMcpJsonMapperSupplier().get();
        var transport = new StdioServerTransportProvider(mapper);

        McpSyncServer server = McpServer.sync(transport)
                .serverInfo("yourkit-mcp", "0.1.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .tools(tools.toArray(new SyncToolSpecification[0]))
                .build();

        // Block forever; the transport drives the server on its own threads.
        // The JVM exits when stdin closes (transport thread completes).
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            server.close();
        }
    }
}
