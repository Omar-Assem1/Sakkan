package com.sakan.common.exception;

public class IllegalStatusTransitionException extends RuntimeException {

    public IllegalStatusTransitionException(String message) {
        super(message);
    }

    public IllegalStatusTransitionException(Object from, Object to) {
        super("Illegal status transition from " + from + " to " + to);
    }
}
