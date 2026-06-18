package com.mysticdrew.yourkitmcp.session;

import com.mysticdrew.yourkitmcp.ProfilerException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    @Test
    void connectReturnsIdAndValidates() {
        FakeControllerServiceFactory f = new FakeControllerServiceFactory();
        SessionManager mgr = new SessionManager(f);
        String id = mgr.connect("localhost", 10001);
        assertTrue(id.startsWith("session-"));
        assertEquals("localhost", f.lastHost);
        assertEquals(10001, f.lastPort);
        assertTrue(f.service.calls.contains("status")); // validated on connect
        assertEquals(1, mgr.count());
    }

    @Test
    void getUnknownThrows() {
        SessionManager mgr = new SessionManager(new FakeControllerServiceFactory());
        assertThrows(ProfilerException.class, () -> mgr.get("session-999"));
    }

    @Test
    void disconnectClosesAndRemoves() {
        FakeControllerServiceFactory f = new FakeControllerServiceFactory();
        SessionManager mgr = new SessionManager(f);
        String id = mgr.connect("h", 1);
        mgr.disconnect(id);
        assertTrue(f.service.closed);
        assertEquals(0, mgr.count());
    }
}
