package com.mysticdrew.yourkitmcp;

public class ProfilerException extends RuntimeException {
    public ProfilerException(String message) { super(message); }
    public ProfilerException(String message, Throwable cause) { super(message, cause); }
}
