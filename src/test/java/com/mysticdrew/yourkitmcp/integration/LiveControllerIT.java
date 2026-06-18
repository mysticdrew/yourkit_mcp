package com.mysticdrew.yourkitmcp.integration;

import com.mysticdrew.yourkitmcp.session.CpuMode;
import com.mysticdrew.yourkitmcp.session.SnapshotKind;
import com.mysticdrew.yourkitmcp.session.StatusInfo;
import com.mysticdrew.yourkitmcp.session.YourKitControllerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

// Run only when YKMCP_TEST_PORT points at a live agent:
//   set YKMCP_TEST_PORT=10001 && gradlew.bat test --tests "*LiveControllerIT"
@EnabledIfEnvironmentVariable(named = "YKMCP_TEST_PORT", matches = "\\d+")
class LiveControllerIT {

    private YourKitControllerService connect() {
        String host = System.getenv().getOrDefault("YKMCP_TEST_HOST", "localhost");
        int port = Integer.parseInt(System.getenv("YKMCP_TEST_PORT"));
        return new YourKitControllerService(host, port);
    }

    @Test
    void statusAndCpuRoundTrip() {
        YourKitControllerService svc = connect();
        StatusInfo before = svc.status();
        assertTrue(before.pid() > 0);
        svc.startCpuProfiling(CpuMode.SAMPLING);
        assertTrue(svc.status().cpuProfiling());
        svc.stopCpuProfiling();
        String snap = svc.captureSnapshot(SnapshotKind.PERFORMANCE);
        assertTrue(snap.endsWith(".snapshot"));
    }
}
