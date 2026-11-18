package com.sakan.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** A time slot collides with an existing slot on the same resource. Maps to HTTP 409. */
@ResponseStatus(HttpStatus.CONFLICT)
public class SlotConflictException extends RuntimeException {

    public SlotConflictException(String message) {
        super(message);
    }
}
