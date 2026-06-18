package com.mysticdrew.yourkitmcp.session;

public class FakeControllerServiceFactory implements ControllerServiceFactory {
    public final FakeControllerService service = new FakeControllerService();
    public String lastHost; public int lastPort;
    @Override public ControllerService connect(String host, int port) {
        this.lastHost = host; this.lastPort = port; return service;
    }
}
