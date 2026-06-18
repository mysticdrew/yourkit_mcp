package com.mysticdrew.yourkitmcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysticdrew.yourkitmcp.session.FakeControllerServiceFactory;
import com.mysticdrew.yourkitmcp.session.SessionManager;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProfilingToolsTest {

    @Test
    void producesSevenSpecs() {
        SessionManager mgr = new SessionManager(new FakeControllerServiceFactory());
        List<SyncToolSpecification> specs = ProfilingTools.specs(mgr, new ObjectMapper());
        assertEquals(7, specs.size());
    }
}
