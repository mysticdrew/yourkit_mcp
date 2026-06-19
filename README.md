# YourKit MCP Server

An [MCP](https://modelcontextprotocol.io) server that exposes the
[YourKit Java Profiler](https://www.yourkit.com/) to an LLM client (Claude Code,
Claude Desktop, etc.).

It does two things:

1. **Live control** of a running, profiled JVM — start/stop CPU and allocation
   profiling, capture snapshots, force GC, read telemetry.
2. **Snapshot analysis** — turn a captured `.snapshot` into compact, structured,
   token-bounded results (top hot spots, biggest memory classes, ...).

> **Status:** v1 implemented — 14 MCP tools, builds to a fat jar, and passes a stdio tool-list smoke test. Live control maps the verified YourKit v3 Controller API; end-to-end validation against a live agent + a real exported snapshot is the remaining manual step.

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

- **JDK 17+** to run the server (built with Adoptium JDK 21 + Gradle 8.10, cross-compiled to Java 17 bytecode).
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

The build requires YourKit to be installed; pass its root via `-PyourkitHome`:

```
# Windows
gradlew.bat -PyourkitHome="C:\Program Files\YourKit Java Profiler 2026.3.164" build

# Linux / macOS
./gradlew -PyourkitHome="/opt/yourkit" build
```

Produces a runnable fat-jar at `build/libs/yourkit-mcp.jar`.

> **Note:** `YOURKIT_HOME` must also be set in the MCP client `env` block at runtime
> (the server calls `Config.fromEnv()` at startup and exits immediately if it is absent
> or points to a missing directory — see the Configure section below).

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
| `yourkit_start_cpu_profiling(sessionId, mode)` | Start CPU — `sampling` / `tracing` / `call_counting` |
| `yourkit_stop_cpu_profiling(sessionId)` | Stop CPU profiling |
| `yourkit_start_alloc_profiling(sessionId, mode?)` | Start allocation recording |
| `yourkit_stop_alloc_profiling(sessionId)` | Stop allocation recording |
| `yourkit_force_gc(sessionId)` | Force a GC |
| `yourkit_advance_generation(sessionId, name?)` | Mark a memory generation (leak hunting) |
| `yourkit_get_telemetry(sessionId)` | Heap size + total created objects |
| `yourkit_capture_snapshot(sessionId, type)` | Capture `performance` / `memory` / `hprof` / `jfr`; returns the path |
| `yourkit_analyze_snapshot(path, topN?)` | Export + parse a snapshot → top-N JSON + raw export dir |
| `yourkit_capture_and_analyze(sessionId, type, topN?)` | Convenience: capture then analyze in one call |

## Limitations (v1)

- Snapshot **analysis** assumes the snapshot file is on the **local filesystem**, so
  the primary supported case is profiling a JVM on `localhost`. Analyzing snapshots
  captured on a remote agent host is out of scope for v1.
- Thread / monitor / exception / JFR start-stop are not exposed as tools in v1
  (the architecture leaves room to add them).
- `yourkit_analyze_snapshot` and `yourkit_capture_and_analyze` write each snapshot's
  exported CSVs into a fresh temp directory (path returned in the result as
  `exportDir`) that is **intentionally not deleted** so callers can drill into raw
  data. These directories accumulate over a long-running session and can be cleaned
  manually (e.g. delete all `ykexport-*` dirs from the system temp folder).

## Known issues

### Snapshot export fails with "Cannot accept EULA" under some MCP hosts

`yourkit_analyze_snapshot` / `yourkit_capture_and_analyze` shell out to the YourKit
exporter (`java -jar yourkit.jar -accept-eula -export ...`). Under some MCP hosts the
exporter subprocess exits early with:

```
Export failed (exit 1): Cannot accept EULA.
```

This is **not** a bug in the command, the EULA flag, or `user.home`. The exporter is
invoked correctly: it launches the UI jar directly (not `bin/profiler.bat`, which would
pin YourKit's settings dir to the read-only install dir) and pins `-Duser.home` to the
user profile that holds the license and prior EULA acceptance. Verified equal to a known
-good manual run: same command, same environment (`USERPROFILE`/`HOMEDRIVE`/`APPDATA`),
same resolved `user.home`, and the spawned child can write `~/.yjp`. The **identical
command succeeds when run from a shell** and fails **only** when the YourKit subprocess
is spawned by the host-launched MCP server — a host-imposed process restriction
(restricted token / job object / sandbox) that YourKit's early license + EULA check trips
on. It cannot be worked around from inside this server.

**Unaffected:** all live-control tools (`connect`, `start/stop` CPU & allocation,
`capture_snapshot`, `status`, `telemetry`, `force_gc`). Only the snapshot **export/analyze**
step is affected.

**Workaround** — capture via MCP, analyze manually:

1. `yourkit_capture_snapshot(sessionId, "performance")` → returns the `.snapshot` path.
2. Export it yourself from a normal shell (this always works):

   ```
   "%YOURKIT_HOME%\jre64\bin\java.exe" -jar "%YOURKIT_HOME%\lib\yourkit.jar" \
       -accept-eula -export "<snapshot>.snapshot" "<out-dir>"
   ```

   Then read the CSVs in `<out-dir>` (`Method-list-CPU.csv`, `Table-File-Write.csv`, etc.).

If your host can launch MCP servers without a sandbox / with a full token, that also
resolves it; configure that at the host level (e.g. the MCP client config).

## License

[MIT](LICENSE).

## Thanks

<img src="https://www.yourkit.com/images/yklogo.png">

Thanks to YourKit for providing licenses to open source projects.
YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>,
<a href="https://www.yourkit.com/dotnet-profiler/">YourKit .NET Profiler</a>,
and <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a>.
