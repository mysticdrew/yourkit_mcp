package com.mysticdrew.yourkitmcp.session;

import java.util.ArrayList;
import java.util.List;

public class FakeControllerService implements ControllerService {
    public final List<String> calls = new ArrayList<>();
    public StatusInfo status = new StatusInfo(4242, "demo.App", false, false, false, false, false, false, false);
    public TelemetryInfo telemetry = new TelemetryInfo(1_000_000L, 50L, 2048L);
    public String snapshotPath = "C:\\snaps\\demo.snapshot";
    public boolean closed = false;

    @Override public StatusInfo status() { calls.add("status"); return status; }
    @Override public void startCpuProfiling(CpuMode m) { calls.add("startCpu:" + m); }
    @Override public void stopCpuProfiling() { calls.add("stopCpu"); }
    @Override public void startAllocProfiling(AllocMode m) { calls.add("startAlloc:" + m); }
    @Override public void stopAllocProfiling() { calls.add("stopAlloc"); }
    @Override public void forceGc() { calls.add("forceGc"); }
    @Override public void advanceGeneration(String name) { calls.add("advanceGen:" + name); }
    @Override public TelemetryInfo telemetry() { calls.add("telemetry"); return telemetry; }
    @Override public String captureSnapshot(SnapshotKind k) { calls.add("capture:" + k); return snapshotPath; }
    @Override public void close() { closed = true; }
}
