package com.sakan.common.exception;

public class RoleNotAllowedException extends RuntimeException {
    public RoleNotAllowedException() {
        super("This role cannot be chosen at registration");
    }
}