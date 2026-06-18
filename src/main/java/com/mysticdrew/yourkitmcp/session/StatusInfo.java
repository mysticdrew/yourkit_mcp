package com.mysticdrew.yourkitmcp.session;

public record StatusInfo(int pid, String appName, boolean cpuProfiling, boolean allocProfiling,
                         boolean threadProfiling, boolean monitorProfiling, boolean exceptionProfiling,
                         boolean jfr, boolean telemetry) {}
