package com.mysticdrew.yourkitmcp.mcp;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapperSupplier;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import java.util.Map;
import java.util.function.Function;

/**
 * DRY wrapper isolating the MCP SDK tool API (2.0.0).
 * If the pinned SDK version's types differ, this is the ONLY file to adapt.
 *
 * <p>Key 2.0.0 differences vs 0.x:
 * <ul>
 *   <li>{@code Tool} must be built via {@code Tool.builder()}, no 3-arg constructor.</li>
 *   <li>{@code SyncToolSpecification} handler is {@code BiFunction<McpSyncServerExchange,
 *       CallToolRequest, CallToolResult>}; arguments come from {@code request.arguments()}.</li>
 *   <li>{@code CallToolResult} built via {@code CallToolResult.builder()}, not a 2-arg ctor.</li>
 * </ul>
 */
public final class Tools {
    private Tools() {}

    /** Shared mapper — constructed once via the Jackson 3 ServiceLoader supplier. */
    private static final McpJsonMapper MAPPER = new JacksonMcpJsonMapperSupplier().get();

    /**
     * Creates a synchronous tool specification.
     *
     * @param name           MCP tool name (e.g. {@code "yourkit_ping"})
     * @param description    Human-readable description
     * @param inputSchemaJson JSON Schema string for the tool's input
     * @param handler        Receives the tool's arguments map; returns a JSON string result.
     *                       Any thrown {@link RuntimeException} is converted to an isError result.
     */
    public static SyncToolSpecification sync(
            String name,
            String description,
            String inputSchemaJson,
            Function<Map<String, Object>, String> handler) {

        // Use the non-deprecated factory: builder(name, mapper, jsonSchemaString)
        Tool tool = Tool.builder(name, MAPPER, inputSchemaJson)
                .description(description)
                .build();

        return new SyncToolSpecification(tool, (exchange, request) -> {
            Map<String, Object> args = request.arguments();
            try {
                String json = handler.apply(args == null ? Map.of() : args);
                return CallToolResult.builder()
                        .addTextContent(json)
                        .isError(false)
                        .build();
            } catch (RuntimeException e) {
                String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                return CallToolResult.builder()
                        .addTextContent("ERROR: " + msg)
                        .isError(true)
                        .build();
            }
        });
    }
}
