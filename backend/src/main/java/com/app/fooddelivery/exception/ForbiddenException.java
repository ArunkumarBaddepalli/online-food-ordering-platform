package com.app.fooddelivery.exception;

/** The caller is known but is not allowed to touch this resource. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
