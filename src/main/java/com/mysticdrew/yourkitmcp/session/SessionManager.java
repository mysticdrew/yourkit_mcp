package com.mysticdrew.yourkitmcp.session;

import com.mysticdrew.yourkitmcp.ProfilerException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class SessionManager {
    private final ControllerServiceFactory factory;
    private final Map<String, ControllerService> sessions = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong();

    public SessionManager(ControllerServiceFactory factory) { this.factory = factory; }

    public String connect(String host, int port) {
        ControllerService svc = factory.connect(host, port);
        svc.status(); // fail fast if unreachable
        String id = "session-" + counter.incrementAndGet();
        sessions.put(id, svc);
        return id;
    }

    public ControllerService get(String id) {
        ControllerService svc = sessions.get(id);
        if (svc == null) throw new ProfilerException("Unknown session: " + id + ". Call yourkit_connect first.");
        return svc;
    }

    public void disconnect(String id) {
        ControllerService svc = sessions.remove(id);
        if (svc != null) svc.close();
    }

    public int count() { return sessions.size(); }
}
