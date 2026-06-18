package com.mysticdrew.yourkitmcp.session;

public interface ControllerService extends AutoCloseable {
    StatusInfo status();
    void startCpuProfiling(CpuMode mode);
    void stopCpuProfiling();
    void startAllocProfiling(AllocMode mode);
    void stopAllocProfiling();
    void forceGc();
    void advanceGeneration(String name);
    TelemetryInfo telemetry();
    String captureSnapshot(SnapshotKind kind);
    @Override void close();
}
