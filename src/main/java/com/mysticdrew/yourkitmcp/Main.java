package com.mysticdrew.yourkitmcp;

import com.mysticdrew.yourkitmcp.mcp.Tools;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapperSupplier;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Entry point for the YourKit MCP stdio server.
 *
 * <p>stdout is reserved exclusively for JSON-RPC (MCP protocol). All logging
 * goes to stderr via slf4j-simple (configured in simplelogger.properties).
 * NEVER write to System.out here.
 */
public final class Main {
    public static void main(String[] args) {
        // StdioServerTransportProvider has no no-arg constructor in 2.0.0;
        // supply the Jackson 3 mapper (auto-discovered via JacksonMcpJsonMapperSupplier).
        McpJsonMapper mapper = new JacksonMcpJsonMapperSupplier().get();
        var transport = new StdioServerTransportProvider(mapper);

        McpSyncServer server = McpServer.sync(transport)
                .serverInfo("yourkit-mcp", "0.1.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .tools(Tools.sync(
                        "yourkit_ping",
                        "Health check. Returns pong.",
                        "{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}",
                        a -> "{\"pong\":true}"))
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
