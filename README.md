# YourKit MCP Server

An [MCP](https://modelcontextprotocol.io) server that exposes the
[YourKit Java Profiler](https://www.yourkit.com/) to an LLM client (Claude Code,
Claude Desktop, etc.).

It does two things:

1. **Live control** of a running, profiled JVM — start/stop CPU and allocation
   profiling, capture snapshots, force GC, read telemetry.
2. **Snapshot analysis** — turn a captured `.snapshot` into compact, structured,
   token-bounded results (top hot spots, biggest memory classes, ...).

> **Status:** in development. Design is finalized — see
> [`docs/superpowers/specs/2026-06-17-yourkit-mcp-design.md`](docs/superpowers/specs/2026-06-17-yourkit-mcp-design.md).
> The build and tool implementations are being written; commands below describe the
> intended usage.

## How it works

Single Java process speaking MCP over **stdio**. Live control goes through the
YourKit **v3 Controller API** (`com.yourkit.api.controller.v3`) over HTTP to the
agent loaded in your target JVM. Snapshot analysis shells out to YourKit's headless
exporter (`profiler.bat -export`) and parses the resulting CSVs.

```
Claude ──stdio──▶ YourKit MCP Server (Java)
                    ├─ SessionManager ── Controller(host,port) ──HTTP──▶ agent in target JVM
                    └─ SnapshotExporter ── profiler.bat -export ──▶ CSVs ──▶ ExportParser ──▶ top-N JSON
```

The server **connects to** JVMs that are already running with the YourKit agent —
it does not start the agent for you.

## Prerequisites

- **JDK 17+** to run the server (built/tested with Adoptium JDK 25).
- **YourKit Java Profiler** installed. Set `YOURKIT_HOME` to the install dir, e.g.
  `C:\Program Files\YourKit Java Profiler 2026.3.164`. The server uses:
  - `lib/yjp-controller-api-redist.jar` — Controller API (compile + runtime).
  - `bin/profiler.bat` (Windows) / `bin/profiler.sh` — headless snapshot export.
- A **target JVM started with the YourKit agent**, listening on a known port.

### Starting your target JVM with the agent

Add the agent to the JVM you want to profile and pin a port:

```
# Windows
-agentpath:"C:\Program Files\YourKit Java Profiler 2026.3.164\bin\windows-x86-64\yjpagent.dll"=port=10001,listen=all

# Linux
-agentpath:/path/to/YourKit/bin/linux-x86-64/libyjpagent.so=port=10001,listen=all
```

The agent logs the port it bound at startup. That port is what you pass to
`yourkit_connect`.

## Build

```
./gradlew build        # Linux/macOS
gradlew.bat build      # Windows
```

Produces a runnable jar under `build/libs/`.

## Configure in Claude Code / Desktop

Add to your MCP client config (adjust paths):

```json
{
  "mcpServers": {
    "yourkit": {
      "command": "java",
      "args": ["-jar", "G:\\yourkit_mcp\\build\\libs\\yourkit-mcp.jar"],
      "env": {
        "YOURKIT_HOME": "C:\\Program Files\\YourKit Java Profiler 2026.3.164"
      }
    }
  }
}
```

## Tools

| Tool | Purpose |
|---|---|
| `yourkit_connect(host?, port)` | Connect to an agent; returns a `sessionId` + pid/appName/status |
| `yourkit_disconnect(sessionId)` | Drop a session |
| `yourkit_status(sessionId)` | What's currently profiling |
| `yourkit_start_cpu_profiling(sessionId, mode, options?)` | Start CPU — `sampling` / `tracing` / `call_counting` |
| `yourkit_stop_cpu_profiling(sessionId)` | Stop CPU profiling |
| `yourkit_start_alloc_profiling(sessionId, mode?, options?)` | Start allocation recording |
| `yourkit_stop_alloc_profiling(sessionId)` | Stop allocation recording |
| `yourkit_force_gc(sessionId)` | Force a GC |
| `yourkit_advance_generation(sessionId, name?)` | Mark a memory generation (leak hunting) |
| `yourkit_get_telemetry(sessionId)` | Heap size + total created objects |
| `yourkit_capture_snapshot(sessionId, type)` | Capture `performance` / `memory` / `hprof` / `jfr`; returns the path |
| `yourkit_analyze_snapshot(path, views?, topN?)` | Export + parse a snapshot → top-N JSON + raw export dir |
| `yourkit_capture_and_analyze(sessionId, type, topN?)` | Convenience: capture then analyze in one call |

## Limitations (v1)

- Snapshot **analysis** assumes the snapshot file is on the **local filesystem**, so
  the primary supported case is profiling a JVM on `localhost`. Analyzing snapshots
  captured on a remote agent host is out of scope for v1.
- Thread / monitor / exception / JFR start-stop are not exposed as tools in v1
  (the architecture leaves room to add them).

## License

TBD.

## Thanks

<img src="https://www.yourkit.com/images/yklogo.png">

Thanks to YourKit for providing licenses to open source projects.
YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>,
<a href="https://www.yourkit.com/dotnet-profiler/">YourKit .NET Profiler</a>,
and <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a>.
