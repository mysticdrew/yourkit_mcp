package com.mysticdrew.yourkitmcp.session;

public final class YourKitControllerServiceFactory implements ControllerServiceFactory {
    @Override public ControllerService connect(String host, int port) {
        return new YourKitControllerService(host, port);
    }
}
