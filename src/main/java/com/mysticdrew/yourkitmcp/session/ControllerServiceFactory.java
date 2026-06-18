package com.mysticdrew.yourkitmcp.session;

public interface ControllerServiceFactory {
    ControllerService connect(String host, int port);
}
