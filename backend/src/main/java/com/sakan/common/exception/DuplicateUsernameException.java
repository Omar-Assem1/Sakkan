package com.sakan.common.exception;

public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException() {
        super("Username is already taken");
    }
}