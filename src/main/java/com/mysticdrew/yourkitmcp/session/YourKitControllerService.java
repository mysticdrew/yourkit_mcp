package com.mysticdrew.yourkitmcp.session;

import com.mysticdrew.yourkitmcp.ProfilerException;
import com.yourkit.api.controller.v3.AllocationProfilingMode;
import com.yourkit.api.controller.v3.AllocationProfilingOptions;
import com.yourkit.api.controller.v3.CaptureSnapshotOptions;
import com.yourkit.api.controller.v3.Controller;
import com.yourkit.api.controller.v3.CpuProfilingMode;
import com.yourkit.api.controller.v3.CpuProfilingOptions;
import com.yourkit.api.controller.v3.SnapshotType;
import com.yourkit.api.controller.v3.Status;
import com.yourkit.api.controller.v3.TotalCreatedObjects;

import java.io.IOException;

public final class YourKitControllerService implements ControllerService {
    private final String host;
    private final int port;
    private final Controller controller;

    public YourKitControllerService(String host, int port) {
        this.host = host;
        this.port = port;
        try {
            this.controller = Controller.newBuilder().host(host).port(port).build();
        } catch (Exception e) {
            throw new ProfilerException("Failed to build controller for " + host + ":" + port
                + " — " + e.getMessage(), e);
        }
    }

    @Override public StatusInfo status() {
        try {
            Status s = controller.getStatus();
            return new StatusInfo(s.getPid(), s.getAppName(), s.isCpuProfiling(), s.isAllocationProfiling(),
                s.isThreadProfiling(), s.isMonitorProfiling(), s.isExceptionProfiling(), s.isJfr(), s.isTelemetry());
        } catch (IOException e) { throw connError(e); }
    }

    @Override public void startCpuProfiling(CpuMode mode) {
        CpuProfilingMode m = switch (mode) {
            case SAMPLING -> CpuProfilingMode.SAMPLING;
            case TRACING -> CpuProfilingMode.TRACING;
            case CALL_COUNTING -> CpuProfilingMode.CALL_COUNTING;
        };
        try { controller.startCpuProfiling(new CpuProfilingOptions().setMode(m)); }
        catch (IOException e) { throw connError(e); }
    }

    @Override public void stopCpuProfiling() {
        try { controller.stopCpuProfiling(); } catch (IOException e) { throw connError(e); }
    }

    @Override public void startAllocProfiling(AllocMode mode) {
        AllocationProfilingMode m = switch (mode) {
            case BCI -> AllocationProfilingMode.BCI;
            case COUNTING -> AllocationProfilingMode.COUNTING;
            case HEAP_SAMPLING -> AllocationProfilingMode.HEAP_SAMPLING;
        };
        try { controller.startAllocationProfiling(new AllocationProfilingOptions().setMode(m)); }
        catch (IOException e) { throw connError(e); }
    }

    @Override public void stopAllocProfiling() {
        try { controller.stopAllocationProfiling(); } catch (IOException e) { throw connError(e); }
    }

    @Override public void forceGc() {
        try { controller.forceGc(); } catch (IOException e) { throw connError(e); }
    }

    @Override public void advanceGeneration(String name) {
        try { controller.advanceGeneration(name == null ? "" : name); } catch (IOException e) { throw connError(e); }
    }

    @Override public TelemetryInfo telemetry() {
        try {
            long heap = controller.getJvmTelemetry().getTotalHeapSize();
            TotalCreatedObjects t = controller.getTotalCreatedObjects();
            return new TelemetryInfo(heap, t.getCount(), t.getSize());
        } catch (IOException e) { throw connError(e); }
    }

    @Override public String captureSnapshot(SnapshotKind kind) {
        SnapshotType type = switch (kind) {
            case PERFORMANCE -> SnapshotType.PERFORMANCE;
            case MEMORY -> SnapshotType.MEMORY;
            case HPROF -> SnapshotType.HPROF;
            case JFR -> SnapshotType.JFR;
        };
        try { return controller.captureSnapshot(new CaptureSnapshotOptions().setType(type)); }
        catch (IOException e) { throw connError(e); }
    }

    @Override public void close() { /* v3 Controller holds no persistent socket; nothing to release */ }

    private ProfilerException connError(IOException e) {
        return new ProfilerException("YourKit agent communication failed at " + host + ":" + port + ": "
            + e.getMessage() + " (is the agent listening there?)", e);
    }
}
