package com.sakan.common.exception;

/** One fixed message for every login failure, so responses reveal nothing about which part was wrong. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}